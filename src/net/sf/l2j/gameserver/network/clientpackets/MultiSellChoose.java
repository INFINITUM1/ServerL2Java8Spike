package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellEntry;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellIngredient;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellListContainer;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Armor;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.util.Log;

public class MultiSellChoose extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(MultiSellChoose.class.getName());
    private int _listId;
    private int _entryId;
    private int _amount;
    private int _enchantment;
    private int _transactionTax;	// local handling of taxation
    private int _sevaEnch = 0;
    private Augment _sevaAug;

    public static class Augment {

        public int id;
        public int skill_id = 0;
        public int skill_lvl = 0;

        public Augment(int id, L2Skill skill) {
            this.id = id;
            if (skill != null) {
                this.skill_id = skill.getId();
                this.skill_lvl = skill.getLevel();
            }
        }
    }

    @Override
    protected void readImpl() {
        _listId = readD();
        _entryId = readD();
        _amount = readD();
        // _enchantment = readH();  // <---commented this line because it did NOT work!
        _enchantment = _entryId % 100000;
        _entryId = _entryId / 100000;
        _transactionTax = 0;	// initialize tax amount to 0...
    }

    @Override
    public void runImpl() {
        if (_amount < 1 || _amount > 5000) {
            return;
        }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPAU() < 300) {
            return;
        }

        player.sCPAU();

        if (player.getMultListId() != _listId) {
            player.setMultListId(0);
            Log.add("Wrong list: (" + player.getMultListId() + " != " + _listId + ") / Player: " + player.getName(), "cheats/multisell");
            return;
        }

        MultiSellListContainer list = L2Multisell.getInstance().getList(_listId);
        if (list == null) {
            _log.warning("[L2Multisell] can't find list with id: " + _listId);
            player.kick();
            return;
        }

        if (player.isParalyzed()) {
            return;
        }

        int npcId = 1013;
        if (Config.MULTISSELL_PROTECT) {
            if (!list.containsNpc(npcId)) {
                L2Object object = player.getTarget();
                if (object == null || !object.isL2Npc()) {
                    return;
                }

                L2NpcInstance npc = (L2NpcInstance) object;
                if (!player.isInsideRadius(npc, 120, false, false)) {
                    return;
                }

                npcId = npc.getNpcId();
                if (!list.containsNpc(npcId)) {
                    //player.logout();
                    _log.warning("Player " + player.getName() + " tryed to cheat with multisell list " + _listId + " (NpcId: " + npcId + ")");
                    return;
                }
            }
        }

        if (list.getTicketId() > 0 && player.getItemCount(list.getTicketId()) == 0) {
            player.sendHtmlMessage("Магазин", "Для покупки нужен особый пропуск.");
            return;
        }

        if (Config.PREMIUM_ENABLE && list.isPremiumShop() && !player.isPremium()) {
            player.sendHtmlMessage("Магазин", Static.SHOP_FOR_PREMIUM);
            return;
        }

        if (!list.containsEntry(_entryId)) {
            player.setMultListId(0);
            return;
        }
        //System.out.println("!!! " + list.getEnchant());
        doExchange(player, list.getEntry(_entryId), list.getApplyTaxes(), list.getMaintainEnchantment(), _enchantment, list.getEnchant(), npcId, list.saveEnchantment(), list.saveAugment(), list.isUpgradeList());
    }

    private void doExchange(L2PcInstance player, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantment, int listEnch, int npcId, boolean saveEnch, boolean saveAug, boolean upgrade) {
        // given the template entry and information about maintaining enchantment and applying taxes
        // re-create the instance of the entry that will be used for this exchange
        // i.e. change the enchantment level of select ingredient/products and adena amount appropriately.
        L2NpcInstance merchant = null;
        if (npcId != 1013) {
            merchant = (player.getTarget().isL2Npc()) ? (L2NpcInstance) player.getTarget() : null;
            if (merchant == null) {
                return;
            }
        }

        if (upgrade) {
            saveEnch = true;
            saveAug = true;
        }

        PcInventory inv = player.getInventory();
        boolean maintainItemFound = false;

        ItemTable itemTable = ItemTable.getInstance();
        MultiSellEntry entry = prepareEntry(merchant, templateEntry, applyTaxes, saveEnch, enchantment);
        int slots = 0;
        for (MultiSellIngredient e : entry.getProducts()) {
            L2Item template = itemTable.getTemplate(e.getItemId());
            if (template == null) {
                continue;
            }

            if (!template.isStackable()) {
                slots += (e.getItemCount() * _amount);
            } else if (player.getInventory().getItemByItemId(e.getItemId()) == null) {
                slots++;
            }
        }

        if (slots > Integer.MAX_VALUE || slots < 0 || !player.getInventory().validateCapacity(slots)) {
            sendPacket(Static.SLOTS_FULL);
            return;
        }

        // Generate a list of distinct ingredients and counts in order to check if the correct item-counts
        // are possessed by the player
        FastList<MultiSellIngredient> _ingredientsList = new FastList<MultiSellIngredient>();
        boolean newIng = true;
        for (MultiSellIngredient e : entry.getIngredients()) {
            newIng = true;

            // at this point, the template has already been modified so that enchantments are properly included
            // whenever they need to be applied.  Uniqueness of items is thus judged by item id AND enchantment level
            for (MultiSellIngredient ex : _ingredientsList) {
                // if the item was already added in the list, merely increment the count
                // this happens if 1 list entry has the same ingredient twice (example 2 swords = 1 dual)
                if ((ex.getItemId() == e.getItemId()) && (ex.getEnchantmentLevel() == e.getEnchantmentLevel())) {
                    if ((double) ex.getItemCount() + e.getItemCount() > Integer.MAX_VALUE) {
                        player.sendPacket(Static.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
                        _ingredientsList.clear();
                        _ingredientsList = null;
                        return;
                    }
                    ex.setItemCount(ex.getItemCount() + e.getItemCount());
                    newIng = false;
                }
            }
            if (newIng) {
                // If there is a maintainIngredient, then we do not need to check the enchantment parameter
                //  as the enchant level will be checked elsewhere
                if (saveEnch && e.getMantainIngredient()) {
                    maintainItemFound = true;
                }
                // if it's a new ingredient, just store its info directly (item id, count, enchantment)
                _ingredientsList.add(new MultiSellIngredient(e));
            }
        }

        // If there is no maintainIngredient, then we must make sure that the 
        //  enchantment is not kept from the client packet, as it may have been forged
        if (!maintainItemFound) {
            for (MultiSellIngredient product : entry.getProducts()) {
                product.setEnchantmentLevel(0);
            }
        }

        // now check if the player has sufficient items in the inventory to cover the ingredients' expences
        for (MultiSellIngredient e : _ingredientsList) {
            if ((double) e.getItemCount() * _amount > Integer.MAX_VALUE) {
                player.sendPacket(Static.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
                _ingredientsList.clear();
                _ingredientsList = null;
                return;
            }

            switch (e.getItemId()) {
                case 65336:
                    if (player.getClan() == null) {
                        player.sendPacket(Static.YOU_ARE_NOT_A_CLAN_MEMBER);
                        return;
                    }
                    if (!player.isClanLeader()) {
                        player.sendPacket(Static.ONLY_THE_CLAN_LEADER_IS_ENABLED);
                        return;
                    }
                    if (player.getClan().getReputationScore() < (e.getItemCount() * _amount)) {
                        player.sendPacket(Static.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
                        return;
                    }
                    break;
                case 65436:
                    if (player.getPcPoints() < (e.getItemCount() * _amount)) {
                        player.sendPacket(Static.NOT_ENOUGH_PCPOINTS);
                        return;
                    }
                    break;
                default:
                    if (inv.getInventoryItemCount(e.getItemId(), saveEnch ? upgrade ? -1 : e.getEnchantmentLevel() : -1) < ((Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient()) ? (e.getItemCount() * _amount) : e.getItemCount())) {
                        player.sendPacket(Static.NOT_ENOUGH_ITEMS);
                        //player.sendAdmResultMessage("###default###+" + e.getEnchantmentLevel() + "##" + inv.getInventoryItemCount(e.getItemId(), saveEnch ? e.getEnchantmentLevel() : -1) + "##" + ((Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient()) ? (e.getItemCount() * _amount) : e.getItemCount()));
                        _ingredientsList.clear();
                        _ingredientsList = null;
                        return;
                    }
                    break;
            }
        }

        _ingredientsList.clear();
        _ingredientsList = null;
        //FastList<L2Augmentation> augmentation = new FastList<L2Augmentation>();
        /**
         * All ok, remove items and add final product
         */
        for (MultiSellIngredient e : entry.getIngredients()) {
            int totalCount = e.getItemCount() * _amount;
            switch (e.getItemId()) {
                case 65336:
                    int repCost = player.getClan().getReputationScore() - totalCount;
                    player.getClan().setReputationScore(repCost, true);
                    player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
                    player.sendPacket(SystemMessage.id(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(totalCount));
                    break;
                case 65436:
                    player.updatePcPoints(-totalCount, 1, false);
                    break;
                default:
                    L2ItemInstance itemToTake = inv.getItemByItemId(e.getItemId());		// initialize and initial guess for the item to take.
                    if (itemToTake == null) { //this is a cheat, transaction will be aborted and if any items already tanken will not be returned back to inventory!
                        _log.severe("Character: " + player.getName() + " is trying to cheat in multisell, merchatnt id:" + (merchant == null ? npcId : merchant.getNpcId()));
                        return;
                    }

                    /*
                     * if (saveEnch) _sevaEnch = Math.max(_sevaEnch,
                     * itemToTake.getEnchantLevel());
                     */
                    if (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient()) {
                        // if it's a stackable item, just reduce the amount from the first (only) instance that is found in the inventory
                        if (itemToTake.isStackable()) {
                            if (!player.destroyItem("Multisell", itemToTake.getObjectId(), totalCount, player.getTarget(), true)) {
                                return;
                            }
                        } else {
                            // for non-stackable items, one of two scenaria are possible:
                            // a) list maintains enchantment: get the instances that exactly match the requested enchantment level
                            // b) list does not maintain enchantment: get the instances with the LOWEST enchantment level

                            // a) if enchantment is maintained, then get a list of items that exactly match this enchantment
                            if (saveEnch || saveAug) {
                                // loop through this list and remove (one by one) each item until the required amount is taken.
                                //L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantmentLevel());
                                L2ItemInstance llop = null;
                                L2ItemInstance[] inventoryContents;
                                if (upgrade) {
                                    inventoryContents = inv.getAllItemsByItemId(e.getItemId());
                                } else {
                                    inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantmentLevel());
                                }
                                for (int i = 0; i < totalCount; i++) {
                                    //if (inventoryContents[i].isAugmented())
                                    //	augmentation.add(inventoryContents[i].getAugmentation());

                                    /*
                                     * _sevaEnch =
                                     * inventoryContents[i].getEnchantLevel();
                                     */
                                    llop = inventoryContents[i];
                                    if (llop == null) {
                                        continue;
                                    }

                                    if (llop.getItem() instanceof L2Armor || llop.getItem() instanceof L2Weapon) {
                                        if (saveEnch && llop.getEnchantLevel() > 0) {
                                            _sevaEnch = Math.max(_sevaEnch, llop.getEnchantLevel());
                                        }

                                        if (saveAug && _sevaAug == null && llop.getAugmentation() != null) {
                                            _sevaAug = new Augment(llop.getAugmentation().getAugmentationId(), llop.getAugmentation().getSkill());
                                        }
                                    }

                                    if (!player.destroyItem("Multisell", llop.getObjectId(), 1, player.getTarget(), true)) {
                                        return;
                                    }
                                }
                            } else // b) enchantment is not maintained.  Get the instances with the LOWEST enchantment level
                            {
                                /*
                                 * NOTE: There are 2 ways to achieve the above
                                 * goal. 1) Get all items that have the correct
                                 * itemId, loop through them until the lowest
                                 * enchantment level is found. Repeat all this
                                 * for the next item until proper count of items
                                 * is reached. 2) Get all items that have the
                                 * correct itemId, sort them once based on
                                 * enchantment level, and get the range of items
                                 * that is necessary. Method 1 is faster for a
                                 * small number of items to be exchanged. Method
                                 * 2 is faster for large amounts.
                                 *
                                 * EXPLANATION: Worst case scenario for
                                 * algorithm 1 will make it run in a number of
                                 * cycles given by: m*(2n-m+1)/2 where m is the
                                 * number of items to be exchanged and n is the
                                 * total number of inventory items that have a
                                 * matching id. With algorithm 2 (sort), sorting
                                 * takes n*log(n) time and the choice is done in
                                 * a single cycle for case b (just grab the m
                                 * first items) or in linear time for case a
                                 * (find the beginning of items with correct
                                 * enchantment, index x, and take all items from
                                 * x to x+m). Basically, whenever m > log(n) we
                                 * have: m*(2n-m+1)/2 = (2nm-m*m+m)/2 >
                                 * (2nlogn-logn*logn+logn)/2 = nlog(n) -
                                 * log(n*n) + log(n) = nlog(n) + log(n/n*n) =
                                 * nlog(n) + log(1/n) = nlog(n) - log(n) =
                                 * (n-1)log(n) So for m < log(n) then
                                 * m*(2n-m+1)/2 > (n-1)log(n) and m*(2n-m+1)/2 >
                                 * nlog(n)
                                 *
                                 * IDEALLY: In order to best optimize the
                                 * performance, choose which algorithm to run,
                                 * based on whether 2^m > n if (
                                 * (2<<(e.getItemCount() * _amount)) <
                                 * inventoryContents.length ) // do Algorithm 1,
                                 * no sorting else // do Algorithm 2, sorting
                                 *
                                 * CURRENT IMPLEMENTATION: In general, it is
                                 * going to be very rare for a person to do a
                                 * massive exchange of non-stackable items For
                                 * this reason, we assume that algorithm 1 will
                                 * always suffice and we keep things simple. If,
                                 * in the future, it becomes necessary that we
                                 * optimize, the above discussion should make it
                                 * clear what optimization exactly is necessary
                                 * (based on the comments under "IDEALLY").
                                 */

                                // choice 1.  Small number of items exchanged.  No sorting.
                                for (int i = 1; i <= totalCount; i++) {
                                    L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId());

                                    itemToTake = inventoryContents[0];
                                    // get item with the LOWEST enchantment level  from the inventory...
                                    // +0 is lowest by default...
                                    if (itemToTake.getEnchantLevel() > 0) {
                                        for (int j = 0; j < inventoryContents.length; j++) {
                                            if (inventoryContents[j].getEnchantLevel() < itemToTake.getEnchantLevel()) {
                                                itemToTake = inventoryContents[j];
                                                // nothing will have enchantment less than 0. If a zero-enchanted
                                                // item is found, just take it
                                                if (itemToTake.getEnchantLevel() == 0) {
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    //if (saveEnch)
                                    //	_sevaEnch = Math.max(_sevaEnch, itemToTake.getEnchantLevel());
                                    if (!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player.getTarget(), true)) {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
        }
        // Generate the appropriate items
        String date = "";
        SystemMessage sm;
        TextBuilder tb = null;
        boolean logItems = Config.LOG_MULTISELL_ID.contains(_listId);
        if (logItems) {
            date = Log.getTime();
            tb = new TextBuilder();
        }
        for (MultiSellIngredient e : entry.getProducts()) {
            int ench = 0;
            int count = e.getItemCount() * _amount;
            L2ItemInstance product = null;

            if (itemTable.createDummyItem(e.getItemId()).isStackable()) {
                product = inv.addItem("Multisell", e.getItemId(), count, player, player.getTarget());
                if (logItems && product != null) {
                    String act = "MULTISELL " + product.getItemName() + "(" + count + ")(+0)(" + product.getObjectId() + ")(npc:" + (merchant == null ? "null" : merchant.getTemplate().npcId) + ") #(player " + player.getName() + ", account: " + player.getAccountName() + ", ip: " + player.getIP() + ")";
                    tb.append(date + act + "\n");
                }
            } else {
                for (int i = 0; i < count; i++) {
                    product = inv.addItem("Multisell", e.getItemId(), 1, player, player.getTarget());
                    if (product == null) {
                        continue;
                    }
                    /*
                     * надо какнить глянуть, хотя в донат шопе продается
                     * аугментация if (maintainEnchantment) { if (i <
                     * augmentation.size()) product.setAugmentation(new
                     * L2Augmentation(product,
                     * augmentation.get(i).getAugmentationId(),
                     * augmentation.get(i).getSkill(), true));
                     *
                     * product.setEnchantLevel(e.getEnchantmentLevel()); }
                     */
                    int itemType = product.getItem().getType2();
                    if (product.canBeEnchanted() && product.isDestroyable() && (itemType == L2Item.TYPE2_WEAPON || itemType == L2Item.TYPE2_SHIELD_ARMOR || itemType == L2Item.TYPE2_ACCESSORY)) {
                        ench = _sevaEnch > 0 ? _sevaEnch : listEnch;
                        product.setEnchantLevel(ench);
                    }

                    if (_sevaAug != null && itemType == L2Item.TYPE2_WEAPON) {
                        product.setAugmentation(new L2Augmentation(product, _sevaAug.id, _sevaAug.skill_id, _sevaAug.skill_lvl, true));
                    }

                    if (logItems) {
                        String act = "MULTISELL " + product.getItemName() + "(1)(+" + ench + ")(" + product.getObjectId() + ")(npc:" + (merchant == null ? "null" : merchant.getTemplate().npcId) + ") #(player " + player.getName() + ", account: " + player.getAccountName() + ", ip: " + player.getIP() + ", hwid: " + player.getHWID() + ")";
                        tb.append(date + act + "\n");
                    }
                }
            }
            // msg part
            if (count > 1) {
                sm = SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(e.getItemId()).addNumber(count);
            } else {
                //if (maintainEnchantment && e.getEnchantmentLevel() > 0)
                if (ench > 0) {
                    sm = SystemMessage.id(SystemMessageId.ACQUIRED).addNumber(ench).addItemName(e.getItemId());
                } else {
                    sm = SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(e.getItemId());
                }
            }
            player.sendPacket(sm);
        }
        if (logItems && tb != null) {
            Log.item(tb.toString(), Log.MULTISELL);
            tb.clear();
            tb = null;
        }
        sm = null;

        /*
         * StatusUpdate su = new StatusUpdate(player.getObjectId());
         * su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
         * player.sendPacket(su); su = null;
         */
        player.sendItems(false);
        player.sendChanges();

        if (saveEnch) {
            //L2Multisell.getInstance().SeparateAndSend(_listId, player, true, (merchant == null ? null : merchant.getCastle().getTaxRate()));
            L2Multisell.getInstance().SeparateAndSend(_listId, player, true, 0);
        }

        // finally, give the tax to the castle...
        if (merchant != null && merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0) {
            merchant.getCastle().addToTreasury(_transactionTax * _amount);
        }
        //augmentation = null;
    }

    // Regarding taxation, the following appears to be the case:
    // a) The count of aa remains unchanged (taxes do not affect aa directly).
    // b) 5/6 of the amount of aa is taxed by the normal tax rate.
    // c) the resulting taxes are added as normal adena value.
    // d) normal adena are taxed fully.
    // e) Items other than adena and ancient adena are not taxed even when the list is taxable.
    // example: If the template has an item worth 120aa, and the tax is 10%,
    // then from 120aa, take 5/6 so that is 100aa, apply the 10% tax in adena (10a)
    // so the final price will be 120aa and 10a!
    private MultiSellEntry prepareEntry(L2NpcInstance merchant, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel) {
        MultiSellEntry newEntry = new MultiSellEntry();
        newEntry.setEntryId(templateEntry.getEntryId());
        int totalAdenaCount = 0;
        boolean hasIngredient = false;

        for (MultiSellIngredient ing : templateEntry.getIngredients()) {
            // load the ingredient from the template
            MultiSellIngredient newIngredient = new MultiSellIngredient(ing);

            if (newIngredient.getItemId() == 57 && newIngredient.isTaxIngredient()) {
                double taxRate = 0.0;
                if (applyTaxes) {
                    if (merchant != null && merchant.getIsInTown()) {
                        taxRate = merchant.getCastle().getTaxRate();
                    }
                }

                _transactionTax = (int) Math.round(newIngredient.getItemCount() * taxRate);
                totalAdenaCount += _transactionTax;
                continue;	// do not yet add this adena amount to the list as non-taxIngredient adena might be entered later (order not guaranteed)
            } else if (ing.getItemId() == 57) // && !ing.isTaxIngredient()
            {
                totalAdenaCount += newIngredient.getItemCount();
                continue;	// do not yet add this adena amount to the list as taxIngredient adena might be entered later (order not guaranteed)
            } // if it is an armor/weapon, modify the enchantment level appropriately, if necessary
            // not used for clan reputation and fame 
            else if (maintainEnchantment && newIngredient.getItemId() > 0) {
                L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
                if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon)) {
                    newIngredient.setEnchantmentLevel(enchantLevel);
                    hasIngredient = true;
                }
            }

            // finally, add this ingredient to the entry
            newEntry.addIngredient(newIngredient);
        }
        // Next add the adena amount, if any
        if (totalAdenaCount > 0) {
            newEntry.addIngredient(new MultiSellIngredient(57, totalAdenaCount, false, false));
        }

        // Now modify the enchantment level of products, if necessary
        for (MultiSellIngredient ing : templateEntry.getProducts()) {
            // load the ingredient from the template
            MultiSellIngredient newIngredient = new MultiSellIngredient(ing);

            if (maintainEnchantment && hasIngredient) {
                // if it is an armor/weapon, modify the enchantment level appropriately
                // (note, if maintain enchantment is "false" this modification will result to a +0)
                L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
                if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon)) {
                    newIngredient.setEnchantmentLevel(enchantLevel);
                }
            }
            newEntry.addProduct(newIngredient);
        }
        return newEntry;
    }
}
