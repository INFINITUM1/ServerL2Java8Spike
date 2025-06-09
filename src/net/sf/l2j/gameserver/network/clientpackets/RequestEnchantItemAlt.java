package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.util.WebStat;
import net.sf.l2j.util.Rnd;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.lasthero.LastHero;

public final class RequestEnchantItemAlt extends RequestEnchantItem {

    private int _objectId;

    @Override
    protected void readImpl() {
        _objectId = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null || _objectId == 0) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPJ() < 700) {
            cancelActiveEnchant(player);
            return;
        }

        player.sCPJ();

        if (player.isOutOfControl()) {
            cancelActiveEnchant(player);
            return;
        }
        if (player.isDead() || player.isAlikeDead() || player.isFakeDeath()) {
            cancelActiveEnchant(player);
            return;
        }
        if (player.isInOlympiadMode()) {
            cancelActiveEnchant(player);
            return;
        }

        if (Config.ENCH_ANTI_CLICK) {
            if (player.getEnchClicks() >= Config.ENCH_ANTI_CLICK_STEP) {
                cancelActiveEnchant(player);
                player.showAntiClickPWD();
                return;
            }
            player.updateEnchClicks();
        }

        if (Config.TVT_CUSTOM_ITEMS
                && (player.getChannel() == 8 || TvTEvent.isRegisteredPlayer(player.getObjectId()))) {
            cancelActiveEnchant(player);
            return;
        }

        if (Config.ELH_ENABLE
                && Config.LH_CUSTOM_ITEMS
                && (player.getChannel() == 6 || LastHero.getEvent().isRegged(player))) {
            cancelActiveEnchant(player);
            return;
        }

        if (Config.EBC_CUSTOM_ITEMS
                && (player.getChannel() == 6 || BaseCapture.getEvent().isRegged(player))) {
            cancelActiveEnchant(player);
            return;
        }

        Inventory inventory = player.getInventory();
        L2ItemInstance item = inventory.getItemByObjectId(_objectId);
        L2ItemInstance scroll = player.getActiveEnchantItem();
        player.setActiveEnchantItem(null);

        if (item == null || scroll == null) {
            cancelActiveEnchant(player);
            return;
        }

        if (player.isTransactionInProgress()) {
            cancelActiveEnchant(player);
            return;
        }

        if (!player.tradeLeft()) {
            cancelActiveEnchant(player);
            return;
        }

        if (player.isOnline() == 0) {
            cancelActiveEnchant(player);
            return;
        }

        if (player.getActiveWarehouse() != null) {
            //player.cancelActiveWarehouse();
            cancelActiveEnchant(player);
            return;
        }

        if (player.getActiveTradeList() != null) {
            //player.cancelActiveTrade();
            cancelActiveEnchant(player);
            return;
        }

        if (player.getPrivateStoreType() != 0) {
            cancelActiveEnchant(player);
            return;
        }

        // can't enchant rods, hero weapons and shadow items
        if (!(item.canBeEnchanted())) {
            cancelActiveEnchant(player);
            return;
        }

        if (item.getLocation() != L2ItemInstance.ItemLocation.INVENTORY && item.getLocation() != L2ItemInstance.ItemLocation.PAPERDOLL) {
            cancelActiveEnchant(player);
            return;
        }

        if (item.isWear()) {
            cancelActiveEnchant(player);
            //Util.handleIllegalPlayerAction(player,"Player "+player.getName()+" tried to enchant a weared Item", IllegalPlayerAction.PUNISH_KICK);
            return;
        }

        if (item.getOwnerId() != player.getObjectId()) {
            cancelActiveEnchant(player);
            return;
        }

        int crystalId = item.getEnchantCrystalId(scroll);

        if (crystalId == 0 || item.getItem().getCrystalItemId() != crystalId) {
            player.sendPacket(new EnchantResult(item.getEnchantLevel(), true));
            player.sendPacket(Static.INAPPROPRIATE_ENCHANT_CONDITION);
            player.sendActionFailed();
            return;
        }

        int maxEnchant = item.getMaxEnchant();//getMaxEnchant(item);
        //player.sendAdmResultMessage("MAX: +" + maxEnchant);
        if (item.getEnchantLevel() >= maxEnchant) {
            player.sendPacket(new EnchantResult(item.getEnchantLevel(), true));
            player.sendMessage("Достигнут предел заточки +" + maxEnchant);
            player.sendActionFailed();
            return;
        }

        //L2ItemInstance removedScroll;
        if (Config.EAT_ENCH_SCROLLS) {

            synchronized (inventory) {
                //removedScroll = inventory.destroyItem("Enchant", scroll, player, null);		
                //UPDATE `etcitem` SET `item_type`='scroll',`consume_type`='stackable',`material`='steel',`oldtype`='none' WHERE `name` LIKE '%Scroll: Enchant%';
                if (!player.destroyItemByItemId("Enchant", scroll.getItemId(), 1, player, false)) {
                    cancelActiveEnchant(player);
                    return;
                }
            }
        }

        SystemMessage sm;
        int safeEnchantLevel = item.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR ? Config.ENCHANT_SAFE_MAX_FULL : Config.ENCHANT_SAFE_MAX;
        int chance = item.getEnchantLevel() < safeEnchantLevel ? 100 : calculateChance(item, item.getEnchantLevel(), scroll, (Config.BLESS_BONUSES.contains(scroll.getItemId()) || Config.BLESS_BONUSES2.contains(scroll.getItemId())));

        if (Config.ENCH_SHOW_CHANCE) {
            player.sendMessage("Шанс заточки: " + chance + "%");
        }
        if (chance > 0 && Rnd.calcEnchant(chance, (Config.PREMIUM_ENABLE && player.isPremium()))) {
            notifyEnchant(player.getName(), item.getItemName(), item.getEnchantLevel(), 1);
            if (item.getEnchantLevel() == 0) {
                sm = SystemMessage.id(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED).addItemName(item.getItemId());
            } else {
                sm = SystemMessage.id(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId());
            }

            player.sendUserPacket(sm);
            item.setEnchantLevel(calcNewEnchant(item.getEnchantLevel(), maxEnchant));
            item.updateDatabase();
        } else {
            notifyEnchant(player.getName(), item.getItemName(), item.getEnchantLevel(), 0);
            if (scroll.isBlessedEnchantScroll() || scroll.isCrystallEnchantScroll()) {
                int fail = calculateFail(item, item.getEnchantLevel(), scroll.isCrystallEnchantScroll(), (Config.PREMIUM_ENABLE && player.isPremium()), scroll);
                item.setEnchantLevel(fail);
                //player.sendPacket(new InventoryUpdate().addModifiedItem(item));

                //InventoryUpdate iu = new InventoryUpdate();
                //iu.addModifiedItem(item);
                //player.sendPacket(iu);
                //player.sendUserPacket(Static.BLESSED_ENCHANT_FAILED);
                player.sendMessage("Супер-улучшение неудачно. Заточка сброшена на +" + fail);
            } else {
                if (item.isEquipped()) {
                    inventory.unEquipItemInSlot(item.getEquipSlot());
                }

                if (item.getEnchantLevel() > 0) {
                    sm = SystemMessage.id(SystemMessageId.ENCHANTMENT_FAILED_S1_S2_EVAPORATED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId());
                } else {
                    sm = SystemMessage.id(SystemMessageId.ENCHANTMENT_FAILED_S1_EVAPORATED).addItemName(item.getItemId());
                }

                player.sendUserPacket(sm);

                L2ItemInstance destroyedItem = inventory.destroyItem("Enchant", item, player, null);
                if (destroyedItem == null) {
                    //_log.warning("failed to destroy " + item.getObjectId() + " after unsuccessful enchant attempt by char " + player.getName());
                    player.setActiveEnchantItem(null);
                    player.sendActionFailed();
                    return;
                }

                int count = (int) (item.getItem().getCrystalCount() * 0.87);
                if (destroyedItem.getEnchantLevel() > 3) {
                    count += item.getItem().getCrystalCount() * 0.25 * (destroyedItem.getEnchantLevel() - 3);
                }
                if (count < 1) {
                    count = 1;
                }

                L2ItemInstance crystals = inventory.addItem("Enchant", crystalId, count, player, destroyedItem);

                sm = SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(crystals.getItemId()).addNumber(count);
                player.sendUserPacket(sm);
                player.refreshExpertisePenalty();
                player.refreshOverloaded();
            }
            sm = null;
            //player.setActiveEnchantItem(null); 
        }

        player.setInEnch(false);
        player.sendItems(true);
        //player.sendChanges();
        //player.sendPacket(new EnchantResult(item.getEnchantLevel(), true));
        player.sendUserPacket(new EnchantResult(65535, true));
        /*
         * if (scroll.getCount() > 0) player.sendMessage("Осталось " +
         * scroll.getCount() + " " + scroll.getItem().getName()); else
         * player.sendMessage("Закончились " + scroll.getItem().getName());
         */
        player.broadcastUserInfo();
    }

    private int calculateChance(L2ItemInstance item, int next, L2ItemInstance scroll, boolean bonus) {
        Integer chance = 0;
        switch (item.getItem().getType2()) {
            case L2Item.TYPE2_WEAPON:
                if (scroll.isCrystallEnchantScroll() && Config.ALLOW_CRYSTAL_SCROLLS) {
                    return Config.ENCHANT_CHANCE_WEAPON_CRYSTAL;
                }

                if (item.isMagicWeapon()) {
                    chance = Config.ENCHANT_ALT_MAGICSTEPS.get(next);
                    if (chance == null || bonus) {
                        if (scroll.isBlessedEnchantScroll() || scroll.isCrystallEnchantScroll()) {
                            chance = scroll.isCrystallEnchantScroll() ? Config.ENCHANT_CHANCE_WEAPON_CRYSTAL : Config.ENCHANT_ALT_MAGICCAHNCE_BLESS;
                            if (Config.BLESS_BONUSES.contains(scroll.getItemId())) {
                                chance += Config.BLESS_BONUS_ENCH1;
                            } else if (Config.BLESS_BONUSES2.contains(scroll.getItemId())) {
                                return Config.BLESS_BONUS_ENCH2;
                            }
                        } else {
                            chance = Config.ENCHANT_ALT_MAGICCAHNCE;
                        }
                    }
                } else {
                    chance = Config.ENCHANT_ALT_WEAPONSTEPS.get(next);
                    if (chance == null || bonus) {
                        if (scroll.isBlessedEnchantScroll() || scroll.isCrystallEnchantScroll()) {
                            chance = scroll.isCrystallEnchantScroll() ? Config.ENCHANT_CHANCE_WEAPON_CRYSTAL : Config.ENCHANT_ALT_WEAPONCAHNCE_BLESS;
                            if (Config.BLESS_BONUSES.contains(scroll.getItemId())) {
                                chance += Config.BLESS_BONUS_ENCH1;
                            } else if (Config.BLESS_BONUSES2.contains(scroll.getItemId())) {
                                return Config.BLESS_BONUS_ENCH2;
                            }
                        } else {
                            chance = Config.ENCHANT_ALT_WEAPONCAHNCE;
                        }
                    }
                }
                break;
            case L2Item.TYPE2_SHIELD_ARMOR:
                if (scroll.isCrystallEnchantScroll() && Config.ALLOW_CRYSTAL_SCROLLS) {
                    return Config.ENCHANT_CHANCE_ARMOR_CRYSTAL;
                }

                chance = Config.ENCHANT_ALT_ARMORSTEPS.get(next);
                if (chance == null || bonus) {
                    if (scroll.isBlessedEnchantScroll() || scroll.isCrystallEnchantScroll()) {
                        chance = scroll.isCrystallEnchantScroll() ? Config.ENCHANT_CHANCE_ARMOR_CRYSTAL : Config.ENCHANT_ALT_ARMORCAHNCE_BLESS;
                        if (Config.BLESS_BONUSES.contains(scroll.getItemId())) {
                            chance += Config.BLESS_BONUS_ENCH1;
                        } else if (Config.BLESS_BONUSES2.contains(scroll.getItemId())) {
                            return Config.BLESS_BONUS_ENCH2;
                        }
                    } else {
                        chance = Config.ENCHANT_ALT_ARMORCAHNCE;
                    }
                }
                break;
            case L2Item.TYPE2_ACCESSORY:
                if (scroll.isCrystallEnchantScroll() && Config.ALLOW_CRYSTAL_SCROLLS) {
                    return Config.ENCHANT_CHANCE_JEWELRY_CRYSTAL;
                }

                chance = Config.ENCHANT_ALT_JEWERLYSTEPS.get(next);
                if (chance == null || bonus) {
                    if (scroll.isBlessedEnchantScroll() || scroll.isCrystallEnchantScroll()) {
                        chance = scroll.isCrystallEnchantScroll() ? Config.ENCHANT_CHANCE_JEWELRY_CRYSTAL : Config.ENCHANT_ALT_JEWERLYCAHNCE_BLESS;
                        if (Config.BLESS_BONUSES.contains(scroll.getItemId())) {
                            chance += Config.BLESS_BONUS_ENCH1;
                        } else if (Config.BLESS_BONUSES2.contains(scroll.getItemId())) {
                            return Config.BLESS_BONUS_ENCH2;
                        }
                    } else {
                        chance = Config.ENCHANT_ALT_JEWERLYCAHNCE;
                    }
                }
                break;
        }
        return chance;
    }

    private int calculateFail(L2ItemInstance item, int ench, boolean crystall, boolean premium, L2ItemInstance scroll) {
        if (Config.BLESS_BONUSES2.contains(scroll.getItemId())) {
            return ench;
        }

        if (premium && Config.PREMIUM_ENCHANT_FAIL) {
            ench -= 3;
            ench = Math.max(ench, 0);
            return ench;
        }

        int fail = 0;
        switch (item.getItem().getType2()) {
            case L2Item.TYPE2_WEAPON:
                if (ench<10)
                fail = Config.ENCHANT_ALT_WEAPONFAILBLESS;
                else if (ench < 20)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS1;
                else if (ench<30)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS2;
                else if (ench<40)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS3;
                else if (ench<50)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS4;
                else if (ench<60)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS5;
                else if (ench<70)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS6;
                else if (ench<80)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS7;
                else if (ench<90)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS8;
                else if (ench<100)
                    fail = Config.ENCHANT_ALT_WEAPONFAILBLESS9;
                if (crystall) {
                    fail = Config.ENCHANT_ALT_WEAPONFAILCRYST;
                }
                break;
            case L2Item.TYPE2_SHIELD_ARMOR:
                if (ench<10)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS;
                else if (ench < 20)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS1;
                else if (ench<30)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS2;
                else if (ench<40)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS3;
                else if (ench<50)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS4;
                else if (ench<60)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS5;
                else if (ench<70)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS6;
                else if (ench<80)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS7;
                else if (ench<90)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS8;
                else if (ench<100)
                    fail = Config.ENCHANT_ALT_ARMORFAILBLESS9;
                if (crystall) {
                    fail = Config.ENCHANT_ALT_ARMORFAILCRYST;
                }
                break;
            case L2Item.TYPE2_ACCESSORY:
                if (ench<10)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS;
                else if (ench < 20)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS1;
                else if (ench<30)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS2;
                else if (ench<40)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS3;
                else if (ench<50)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS4;
                else if (ench<60)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS5;
                else if (ench<70)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS6;
                else if (ench<80)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS7;
                else if (ench<90)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS8;
                else if (ench<100)
                    fail = Config.ENCHANT_ALT_JEWERLYFAILBLESS9;
                if (crystall) {
                    fail = Config.ENCHANT_ALT_JEWERLYFAILCRYST;
                }
                break;
        }
        if (fail < 0) {
            fail = ench + fail;
        }

        return Math.max(fail, 0);
    }

    /*
     * private int getMaxEnchant(L2ItemInstance item) { Integer maxEnchant =
     * Config.ENCHANT_LIMITS.get(item.getItemId()); if (maxEnchant != null) {
     * return maxEnchant; }
     *
     * switch (item.getItem().getType2()) { case L2Item.TYPE2_WEAPON: maxEnchant
     * = Config.ENCHANT_MAX_WEAPON; break; case L2Item.TYPE2_ACCESSORY:
     * maxEnchant = Config.ENCHANT_MAX_JEWELRY; break; case
     * L2Item.TYPE2_SHIELD_ARMOR: maxEnchant = Config.ENCHANT_MAX_ARMOR; break;
     * } return maxEnchant; }
     */
    private void notifyEnchant(String name, String item, int ench, int sucess) {
        if (Config.WEBSTAT_ENABLE && ench >= Config.WEBSTAT_ENCHANT) {
            WebStat.getInstance().addEnchant(name, item, ench, sucess);
        }
    }

    private int calcNewEnchant(int enchantLevel, int max) {
        enchantLevel += Config.ENCHANT_ALT_STEP;
        return Math.min(enchantLevel, max);
    }
}
