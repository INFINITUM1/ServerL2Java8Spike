package net.sf.l2j.gameserver.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MultiSellList;
import net.sf.l2j.gameserver.templates.L2Armor;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.util.log.AbstractLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Multisell list manager
 *
 */
public class L2Multisell {

    private static final Logger _log = AbstractLogger.getLogger(L2Multisell.class.getName());
    private Map<Integer, MultiSellListContainer> _entries = new ConcurrentHashMap<Integer, MultiSellListContainer>();
    private static L2Multisell _instance = new L2Multisell();

    public MultiSellListContainer getList(int id) {
        return _entries.get(id);
    }

    public L2Multisell() {
        parseData();
    }

    public void reload() {
        parseData();
    }

    public static L2Multisell getInstance() {
        return _instance;
    }

    private void parseData() {
        _entries.clear();
        parse();
    }

    /**
     * This will generate the multisell list for the items. There exist various
     * parameters in multisells that affect the way they will appear: 1)
     * inventory only: * if true, only show items of the multisell for which the
     * "primary" ingredients are already in the player's inventory. By "primary"
     * ingredients we mean weapon and armor. * if false, show the entire list.
     * 2) maintain enchantment: presumably, only lists with "inventory only" set
     * to true should sometimes have this as true. This makes no sense
     * otherwise... * If true, then the product will match the enchantment level
     * of the ingredient. if the player has multiple items that match the
     * ingredient list but the enchantment levels differ, then the entries need
     * to be duplicated to show the products and ingredients for each
     * enchantment level. For example: If the player has a crystal staff +1 and
     * a crystal staff +3 and goes to exchange it at the mammon, the list should
     * have all exchange possibilities for the +1 staff, followed by all
     * possibilities for the +3 staff. * If false, then any level ingredient
     * will be considered equal and product will always be at +0 3) apply taxes:
     * Uses the "taxIngredient" entry in order to add a certain amount of adena
     * to the ingredients
     *
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#runImpl()
     */
    private MultiSellListContainer generateMultiSell(int listId, boolean inventoryOnly, L2PcInstance player, double taxRate) {
        if (player == null) {
            return null;
        }

        MultiSellListContainer listTemplate = L2Multisell.getInstance().getList(listId);
        MultiSellListContainer list = new MultiSellListContainer();
        if (listTemplate == null) {
            return list;
        }
        //list = L2Multisell.getInstance().new MultiSellListContainer();
        list.setListId(listId);

        /*
         * if (Config.MULTISSELL_PROTECT && !listTemplate.containsNpc(1013)) {
         * L2Object object = player.getTarget(); if (object == null ||
         * !object.isL2Npc()) return list;
         *
         * L2NpcInstance npc = (L2NpcInstance)object; if
         * (!listTemplate.containsNpc(npc.getNpcId())) { //player.logout();
         * _log.warning("Player "+player.getName()+" tryed to cheat with
         * multisell list "+listId+" (NpcId: "+npc.getNpcId()+")"); return list;
         * }
         }
         */
        if (inventoryOnly) {
            L2ItemInstance[] items;
            if (listTemplate.saveEnchantment()) {
                items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
            } else {
                items = player.getInventory().getUniqueItems(false, false, false);
            }

            int enchantLevel;
            for (L2ItemInstance item : items) {
                // only do the matchup on equipable items that are not currently equipped
                // so for each appropriate item, produce a set of entries for the multisell list.
                if (!item.isWear() && ((item.getItem() instanceof L2Armor) || (item.getItem() instanceof L2Weapon))) {
                    enchantLevel = (listTemplate.saveEnchantment() ? item.getEnchantLevel() : 0);
                    // loop through the entries to see which ones we wish to include
                    for (MultiSellEntry ent : listTemplate.getEntries()) {
                        boolean doInclude = false;

                        // check ingredients of this entry to see if it's an entry we'd like to include.
                        for (MultiSellIngredient ing : ent.getIngredients()) {
                            if (item.getItemId() == ing.getItemId()) {
                                doInclude = true;
                                break;
                            }
                        }

                        // manipulate the ingredients of the template entry for this particular instance shown
                        // i.e: Assign enchant levels and/or apply taxes as needed.
                        if (doInclude) {
                            list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), listTemplate.saveEnchantment(), enchantLevel, taxRate));
                        }
                    }
                }
            } // end for each inventory item.
        } // end if "inventory-only"
        else // this is a list-all type
        {
            // if no taxes are applied, no modifications are needed
            for (MultiSellEntry ent : listTemplate.getEntries()) {
                list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), false, 0, taxRate));
            }
        }

        return list;
    }

    // Regarding taxation, the following is the case:
    // a) The taxes come out purely from the adena TaxIngredient
    // b) If the entry has no adena ingredients other than the taxIngredient, the resulting
    //    amount of adena is appended to the entry
    // c) If the entry already has adena as an entry, the taxIngredient is used in order to increase
    //	  the count for the existing adena ingredient
    private MultiSellEntry prepareEntry(MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel, double taxRate) {
        MultiSellEntry newEntry = new MultiSellEntry();
        newEntry.setEntryId(templateEntry.getEntryId() * 100000 + enchantLevel);
        int adenaAmount = 0;

        ItemTable it = ItemTable.getInstance();
        for (MultiSellIngredient ing : templateEntry.getIngredients()) {
            // load the ingredient from the template
            MultiSellIngredient newIngredient = new MultiSellIngredient(ing);

            // if taxes are to be applied, modify/add the adena count based on the template adena/ancient adena count
            if (ing.getItemId() == 57 && ing.isTaxIngredient()) {
                if (applyTaxes) {
                    adenaAmount += (int) Math.round(ing.getItemCount() * taxRate);
                }
                continue;	// do not adena yet, as non-taxIngredient adena entries might occur next (order not guaranteed)
            } else if (ing.getItemId() == 57) // && !ing.isTaxIngredient()
            {
                adenaAmount += ing.getItemCount();
                continue;	// do not adena yet, as taxIngredient adena entries might occur next (order not guaranteed)
            } // if it is an armor/weapon, modify the enchantment level appropriately, if necessary
            // not used for clan reputation and fame 
            else if (maintainEnchantment && newIngredient.getItemId() > 0) {
                L2Item tempItem = it.createDummyItem(ing.getItemId()).getItem();
                if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon)) {
                    newIngredient.setEnchantmentLevel(enchantLevel);
                }
            }

            // finally, add this ingredient to the entry
            newEntry.addIngredient(newIngredient);
        }
        // now add the adena, if any.
        if (adenaAmount > 0) {
            newEntry.addIngredient(new MultiSellIngredient(57, adenaAmount, 0, false, false));
        }

        // Now modify the enchantment level of products, if necessary
        for (MultiSellIngredient ing : templateEntry.getProducts()) {
            // load the ingredient from the template
            MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
            if (maintainEnchantment) {
                // if it is an armor/weapon, modify the enchantment level appropriately
                // (note, if maintain enchantment is "false" this modification will result to a +0)
                L2Item tempItem = it.createDummyItem(ing.getItemId()).getItem();
                if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon)) {
                    newIngredient.setEnchantmentLevel(enchantLevel);
                }
            }
            newEntry.addProduct(newIngredient);
        }
        return newEntry;
    }

    public void SeparateAndSend(int listId, L2PcInstance player, boolean inventoryOnly, double taxRate) {
        MultiSellListContainer list = generateMultiSell(listId, inventoryOnly, player, taxRate);
        MultiSellListContainer temp = new MultiSellListContainer();
        int page = 1;
        temp.setListId(list.getListId());
        for (MultiSellEntry e : list.getEntries()) {
            if (temp.getEntries().size() == 40) {
                player.setMultListId(list.getListId());
                player.sendPacket(new MultiSellList(temp, page, 0, true));
                page++;
                temp = new MultiSellListContainer();
                temp.setListId(list.getListId());
            }
            temp.addEntry(e);
        }
        player.setMultListId(list.getListId());
        player.sendPacket(new MultiSellList(temp, page, 1, true));
    }

    public static class MultiSellEntry {

        private int _entryId;
        private List<MultiSellIngredient> _products = new FastList<MultiSellIngredient>();
        private List<MultiSellIngredient> _ingredients = new FastList<MultiSellIngredient>();

        /**
         * @param entryId The entryId to set.
         */
        public void setEntryId(int entryId) {
            _entryId = entryId;
        }

        /**
         * @return Returns the entryId.
         */
        public int getEntryId() {
            return _entryId;
        }

        /**
         * @param product The product to add.
         */
        public void addProduct(MultiSellIngredient product) {
            _products.add(product);
        }

        /**
         * @return Returns the products.
         */
        public List<MultiSellIngredient> getProducts() {
            return _products;
        }

        /**
         * @param ingredients The ingredients to set.
         */
        public void addIngredient(MultiSellIngredient ingredient) {
            _ingredients.add(ingredient);
        }

        /**
         * @return Returns the ingredients.
         */
        public List<MultiSellIngredient> getIngredients() {
            return _ingredients;
        }
    }

    public static class MultiSellIngredient {

        private int _itemId, _itemCount, _enchantmentLevel;
        private boolean _isTaxIngredient, _mantainIngredient;

        public MultiSellIngredient(int itemId, int itemCount, boolean isTaxIngredient, boolean mantainIngredient) {
            this(itemId, itemCount, 0, isTaxIngredient, mantainIngredient);
        }

        public MultiSellIngredient(int itemId, int itemCount, int enchantmentLevel, boolean isTaxIngredient, boolean mantainIngredient) {
            setItemId(itemId);
            setItemCount(itemCount);
            setEnchantmentLevel(enchantmentLevel);
            setIsTaxIngredient(isTaxIngredient);
            setMantainIngredient(mantainIngredient);
        }

        public MultiSellIngredient(MultiSellIngredient e) {
            _itemId = e.getItemId();
            _itemCount = e.getItemCount();
            _enchantmentLevel = e.getEnchantmentLevel();
            _isTaxIngredient = e.isTaxIngredient();
            _mantainIngredient = e.getMantainIngredient();
        }

        /**
         * @param itemId The itemId to set.
         */
        public void setItemId(int itemId) {
            _itemId = itemId;
        }

        /**
         * @return Returns the itemId.
         */
        public int getItemId() {
            return _itemId;
        }

        /**
         * @param itemCount The itemCount to set.
         */
        public void setItemCount(int itemCount) {
            _itemCount = itemCount;
        }

        /**
         * @return Returns the itemCount.
         */
        public int getItemCount() {
            return _itemCount;
        }

        /**
         * @param itemCount The itemCount to set.
         */
        public void setEnchantmentLevel(int enchantmentLevel) {
            _enchantmentLevel = enchantmentLevel;
        }

        /**
         * @return Returns the itemCount.
         */
        public int getEnchantmentLevel() {
            return _enchantmentLevel;
        }

        public void setIsTaxIngredient(boolean isTaxIngredient) {
            _isTaxIngredient = isTaxIngredient;
        }

        public boolean isTaxIngredient() {
            return _isTaxIngredient;
        }

        public void setMantainIngredient(boolean mantainIngredient) {
            _mantainIngredient = mantainIngredient;
        }

        public boolean getMantainIngredient() {
            return _mantainIngredient;
        }
    }

    public static class MultiSellListContainer {

        private int _listId;
        private boolean _applyTaxes = false;
        private boolean _maintainEnchantment = false;
        private boolean _saveEnchantment = false;
        private boolean _saveAugment = false;
        private boolean _upgrade = false;
        private int _enchLvl = Config.MULT_ENCH;
        private int _ticket = 0;
        private int _ticketCount = 0;
        private boolean _premium = false;
        private ConcurrentLinkedQueue<Integer> _npcList = new ConcurrentLinkedQueue<Integer>();
        private Map<Integer, MultiSellEntry> _entriesC = new FastMap<>();

        public MultiSellListContainer() {
        }

        /**
         * @param listId The listId to set.
         */
        public void setListId(int listId) {
            _listId = listId;
        }

        public void setApplyTaxes(boolean applyTaxes) {
            _applyTaxes = applyTaxes;
        }

        public void setMaintainEnchantment(boolean maintainEnchantment) {
            _maintainEnchantment = maintainEnchantment;
        }

        public void setEnchant(int ench) {
            _enchLvl = ench;
        }

        public void setPremium(boolean f) {
            _premium = f;
        }

        /**
         * @return Returns the listId.
         */
        public int getListId() {
            return _listId;
        }

        public boolean getApplyTaxes() {
            return _applyTaxes;
        }

        public boolean getMaintainEnchantment() {
            return _maintainEnchantment;
        }

        public int getEnchant() {
            return _enchLvl;
        }

        public void addEntry(MultiSellEntry e) {
            _entriesC.put(e.getEntryId(), e);
        }

        public Collection<MultiSellEntry> getEntries() {
            return _entriesC.values();
        }

        public void setTicketId(int ticket) {
            _ticket = ticket;
        }

        public int getTicketId() {
            return _ticket;
        }

        public void setTicketCount(int count) {
            _ticketCount = count;
        }

        public int getTicketCount() {
            return _ticketCount;
        }

        //npc
        public void addNpc(int npcId) {
            _npcList.add(npcId);
        }

        public boolean containsNpc(int npcId) {
            return (_npcList.contains(npcId));
        }

        //
        public void setSaveEnchantment(boolean f) {
            _saveEnchantment = f;
        }

        public boolean saveEnchantment() {
            return _saveEnchantment;
        }

        public void setSaveAugment(boolean f) {
            _saveAugment = f;
        }

        public boolean saveAugment() {
            return _saveAugment;
        }

        public void setUpgrade(boolean f) {
            _upgrade = f;
        }

        public boolean isUpgradeList() {
            return _upgrade;
        }

        public boolean isPremiumShop() {
            return _premium;
        }

        public boolean containsEntry(int _entryId) {
            return _entriesC.containsKey(_entryId);
        }

        public MultiSellEntry getEntry(int _entryId) {
            return _entriesC.get(_entryId);
        }
    }

    private void hashFiles(String dirname, List<File> hash) {
        File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);
        if (!dir.exists()) {
            _log.config("Dir " + dir.getAbsolutePath() + " not exists");
            return;
        }
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".xml")) {
                hash.add(f);
            }
        }
    }

    private void parse() {
        Document doc = null;
        int id = 0;
        List<File> files = new FastList<File>();
        hashFiles("multisell", files);

        for (File f : files) {
            id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
            try {

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(false);
                factory.setIgnoringComments(true);
                doc = factory.newDocumentBuilder().parse(f);
            } catch (Exception e) {
                _log.log(Level.SEVERE, "Error loading file " + f, e);
            }
            try {
                MultiSellListContainer list = parseDocument(doc, id);
                list.setListId(id);
                _entries.put(id, list);
            } catch (Exception e) {
                _log.log(Level.SEVERE, "Error in file " + f, e);
            }
        }
    }

    protected MultiSellListContainer parseDocument(Document doc, int id) {
        MultiSellListContainer list = new MultiSellListContainer();

        for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
            if ("list".equalsIgnoreCase(n.getNodeName())) {
                Node attribute;

                attribute = n.getAttributes().getNamedItem("applyTaxes");
                if (attribute == null) {
                    list.setApplyTaxes(false);
                } else {
                    list.setApplyTaxes(Boolean.parseBoolean(attribute.getNodeValue()));
                }

                attribute = n.getAttributes().getNamedItem("maintainEnchantment");
                if (attribute == null) {
                    list.setMaintainEnchantment(false);
                } else {
                    list.setMaintainEnchantment(Boolean.parseBoolean(attribute.getNodeValue()));
                }

                attribute = n.getAttributes().getNamedItem("saveEnchantment");
                if (attribute == null) {
                    list.setSaveEnchantment(false);
                } else {
                    list.setSaveEnchantment(Boolean.parseBoolean(attribute.getNodeValue()));
                }

                attribute = n.getAttributes().getNamedItem("saveAugment");
                if (attribute == null) {
                    list.setSaveAugment(false);
                } else {
                    list.setSaveAugment(Boolean.parseBoolean(attribute.getNodeValue()));
                }

                attribute = n.getAttributes().getNamedItem("upgradeList");
                if (attribute == null) {
                    list.setUpgrade(false);
                } else {
                    list.setUpgrade(Boolean.parseBoolean(attribute.getNodeValue()));
                }

                attribute = n.getAttributes().getNamedItem("premiumShop");
                if (attribute == null) {
                    list.setPremium(false);
                } else {
                    list.setPremium(Boolean.parseBoolean(attribute.getNodeValue()));
                }

                attribute = n.getAttributes().getNamedItem("npcId");
                if (attribute == null) {
                    list.addNpc(0);
                } else {
                    String[] npcList = attribute.getNodeValue().split(",");
                    for (String npcId : npcList) {
                        if (npcId.equals("")) {
                            continue;
                        }

                        list.addNpc(Integer.parseInt(npcId));
                    }
                }

                for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                    if ("item".equalsIgnoreCase(d.getNodeName())) {
                        MultiSellEntry e = parseEntry(d, id);
                        if (e != null) {
                            list.addEntry(e);
                        }
                    }
                }
            } else if ("item".equalsIgnoreCase(n.getNodeName())) {
                MultiSellEntry e = parseEntry(n, id);
                if (e != null) {
                    list.addEntry(e);
                }
            }
        }

        //заточка
        Integer ench = Config.MULT_ENCHS.get(id);
        if (ench != null) {
            list.setEnchant(ench);
        }

        //пропуск
        EventReward ticket = Config.MULTVIP_CARDS.get(id);
        if (ticket != null) {
            list.setTicketId(ticket.id);
            list.setTicketCount(ticket.count);
        }

        return list;
    }

    protected MultiSellEntry parseEntry(Node n, int listId) {
        int entryId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());

        Node attribute;
        Node first = n.getFirstChild();
        MultiSellEntry entry = new MultiSellEntry();
        ItemTable itemTable = ItemTable.getInstance();

        for (n = first; n != null; n = n.getNextSibling()) {
            if ("ingredient".equalsIgnoreCase(n.getNodeName())) {
                int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
                if (id < 65300 && itemTable.getTemplate(id) == null) {
                    _log.warning("L2Multisell [WARNING], list " + listId + ": ingredient itemID " + id + " not known.");
                    return null;
                }

                int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
                boolean isTaxIngredient = false, mantainIngredient = false;

                attribute = n.getAttributes().getNamedItem("isTaxIngredient");

                if (attribute != null) {
                    isTaxIngredient = Boolean.parseBoolean(attribute.getNodeValue());
                }

                attribute = n.getAttributes().getNamedItem("mantainIngredient");

                if (attribute != null) {
                    mantainIngredient = Boolean.parseBoolean(attribute.getNodeValue());
                }

                entry.addIngredient(new MultiSellIngredient(id, count, isTaxIngredient, mantainIngredient));
            } else if ("production".equalsIgnoreCase(n.getNodeName())) {
                int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
                if (id < 65300 && itemTable.getTemplate(id) == null) {
                    _log.warning("L2Multisell [WARNING], list " + listId + ": production itemID " + id + " not known.");
                    return null;
                }
                int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
                entry.addProduct(new MultiSellIngredient(id, count, false, false));
            }
        }

        entry.setEntryId(entryId);

        return entry;
    }
}
