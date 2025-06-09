package net.sf.l2j.gameserver.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

/**
 * @author Advi
 *
 */
public abstract class ItemContainer {

    protected static final Logger _log = Logger.getLogger(ItemContainer.class.getName());
    protected final ConcurrentLinkedQueue<L2ItemInstance> _items;

    protected ItemContainer() {
        _items = new ConcurrentLinkedQueue<L2ItemInstance>();
    }

    protected abstract L2Character getOwner();

    protected abstract ItemLocation getBaseLocation();

    /**
     * Returns the ownerID of the inventory
     *
     * @return int
     */
    public int getOwnerId() {
        return getOwner() == null ? 0 : getOwner().getObjectId();
    }

    /**
     * Returns the quantity of items in the inventory
     *
     * @return int
     */
    public int getSize() {
        return _items.size();
    }

    /**
     * Returns the list of items in inventory
     *
     * @return L2ItemInstance : items in inventory
     */
    public L2ItemInstance[] getItems() {
        return _items.toArray(new L2ItemInstance[_items.size()]);
    }

    // Коллекция
    public ConcurrentLinkedQueue<L2ItemInstance> getAllItems() {
        return _items;
    }
    // Шмотки, которые возможно заточить

    public FastTable<L2ItemInstance> getAllItemsEnch() {
        if (getSize() == 0) {
            return null;
        }

        FastTable<L2ItemInstance> enchs = new FastTable<L2ItemInstance>();
        for (L2ItemInstance item : getAllItems()) {
            if (item == null) {
                continue;
            }

            if (!(item.canBeEnchanted())) {
                continue;
            }

            if (item.getEnchantLevel() > 1) {
                enchs.add(item);
            }
        }
        return enchs;
    }

    // Шмотки, которые возможно аугментировать
    public FastTable<L2ItemInstance> getAllItemsAug() {
        if (getSize() == 0) {
            return null;
        }

        FastTable<L2ItemInstance> enchs = new FastTable<L2ItemInstance>();
        for (L2ItemInstance item : getAllItems()) {
            if (item == null) {
                continue;
            }

            if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                enchs.add(item);
            }
        }
        return enchs;
    }
    // куда пихаем аугмент

    public FastTable<L2ItemInstance> getAllItemsNext(int exc, int service) {
        if (getSize() == 0) {
            return null;
        }

        int itemType = 0;
        FastTable<L2ItemInstance> enchs = new FastTable<L2ItemInstance>();
        for (L2ItemInstance item : getAllItems()) {
            if (item == null) {
                continue;
            }

            if (!(item.canBeEnchanted())) {
                continue;
            }

            if (exc == item.getObjectId()) {
                continue;
            }

            itemType = item.getItem().getType2();
            switch (service) {
                case 1:
                    if (itemType == L2Item.TYPE2_WEAPON && !item.isAugmented() && !item.isWear() && !item.isEquipped() && !item.isHeroItem() && item.isDestroyable()) {
                        enchs.add(item);
                    }
                    break;
                case 2:
                    if (item.getItem().getCrystalType() > 2 && (itemType == L2Item.TYPE2_WEAPON || itemType == L2Item.TYPE2_SHIELD_ARMOR || itemType == L2Item.TYPE2_ACCESSORY)) {
                        enchs.add(item);
                    }
                    break;
            }
        }
        return enchs;
    }

    /**
     * Returns the item from inventory by using its <B>itemId</B><BR><BR>
     *
     * @param itemId : int designating the ID of the item
     * @return L2ItemInstance designating the item or null if not found in
     * inventory
     */
    public L2ItemInstance getItemByItemId(int itemId) {
        for (L2ItemInstance item : _items) {
            if (item != null && item.getItemId() == itemId) {
                return item;
            }
        }

        return null;
    }

    /**
     * Returns the item from inventory by using its <B>itemId</B><BR><BR>
     *
     * @param itemId : int designating the ID of the item
     * @param itemToIgnore : used during a loop, to avoid returning the same
     * item
     * @return L2ItemInstance designating the item or null if not found in
     * inventory
     */
    public L2ItemInstance getItemByItemId(int itemId, L2ItemInstance itemToIgnore) {
        for (L2ItemInstance item : _items) {
            if (item != null && item.getItemId() == itemId
                    && !item.equals(itemToIgnore)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Returns item from inventory by using its <B>objectId</B>
     *
     * @param objectId : int designating the ID of the object
     * @return L2ItemInstance designating the item or null if not found in
     * inventory
     */
    public L2ItemInstance getItemByObjectId(int objectId) {
        for (L2ItemInstance item : _items) {
            if (item.getObjectId() == objectId) {
                return item;
            }
        }

        return null;
    }

    /**
     * Gets count of item in the inventory
     *
     * @param itemId : Item to look for
     * @param enchantLevel : enchant level to match on, or -1 for ANY enchant
     * level
     * @return int corresponding to the number of items matching the above
     * conditions.
     */
    public int getInventoryItemCount(int itemId, int enchantLevel) {
        int count = 0;

        for (L2ItemInstance item : _items) {
            if (item.getItemId() == itemId && ((item.getEnchantLevel() == enchantLevel) || (enchantLevel < 0))) //if (item.isAvailable((L2PcInstance)getOwner(), true) || item.getItem().getType2() == 3)//available or quest item
            {
                if (item.isStackable()) {
                    count = item.getCount();
                } else {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Adds item to inventory
     *
     * @param process : String Identifier of process triggering this action
     * @param item : L2ItemInstance to be added
     * @param actor : L2PcInstance Player requesting the item add
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item
     * in inventory
     */
    public L2ItemInstance addItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference) {
        L2ItemInstance olditem = getItemByItemId(item.getItemId());

        // If stackable item is found in inventory just add to current quantity
        if (olditem != null && olditem.isStackable()) {
            int count = item.getCount();
            olditem.changeCount(process, count, actor, reference);
            olditem.setLastChange(L2ItemInstance.MODIFIED);

            // And destroys the item
            ItemTable.getInstance().destroyItem(process, item, actor, reference);
            item.updateDatabase();
            item = olditem;

            // Updates database
            if (item.getItemId() == 57 && count < 10000 * Config.RATE_DROP_ADENA) {
                // Small adena changes won't be saved to database all the time
                if (GameTimeController.getGameTicks() % 5 == 0) {
                    item.updateDatabase();
                }
            } else {
                item.updateDatabase();
            }
        } // If item hasn't be found in inventory, create new one
        else {
            item.setOwnerId(process, getOwnerId(), actor, reference);
            item.setLocation(getBaseLocation());
            item.setLastChange((L2ItemInstance.ADDED));

            // Add item in inventory
            addItem(item);

            // Updates database
            item.updateDatabase();
        }

        refreshWeight();
        return item;
    }

    /**
     * Adds item to inventory
     *
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item Identifier of the item to be added
     * @param count : int Quantity of items to be added
     * @param actor : L2PcInstance Player requesting the item add
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item
     * in inventory
     */
    public L2ItemInstance addItem(String process, int itemId, int count, L2PcInstance actor, L2Object reference) {
        L2ItemInstance item = getItemByItemId(itemId);

        // If stackable item is found in inventory just add to current quantity
        if (item != null && item.isStackable()) {
            item.changeCount(process, count, actor, reference);
            item.setLastChange(L2ItemInstance.MODIFIED);
            // Updates database
            if (itemId == 57 && count < 10000 * Config.RATE_DROP_ADENA) {
                // Small adena changes won't be saved to database all the time
                if (GameTimeController.getGameTicks() % 5 == 0) {
                    item.updateDatabase();
                }
            } else {
                item.updateDatabase();
            }
        } // If item hasn't be found in inventory, create new one
        else {
            for (int i = 0; i < count; i++) {
                L2Item template = ItemTable.getInstance().getTemplate(itemId);
                if (template == null) {
                    _log.warning((actor != null ? "[" + actor.getName() + "] " : "") + " Invalid ItemId requested: " + itemId + "" + (reference != null ? "; NPC: " + reference.getNpcId() + "] " : ""));
                    return null;
                }

                item = ItemTable.getInstance().createItem(process, itemId, template.isStackable() ? count : 1, actor, reference);
                item.setOwnerId(getOwnerId());
                item.setLocation(getBaseLocation());
                item.setLastChange(L2ItemInstance.ADDED);

                // Add item in inventory
                addItem(item);
                // Updates database
                item.updateDatabase();

                // If stackable, end loop as entire count is included in 1 instance of item
                if (template.isStackable() || !Config.MULTIPLE_ITEM_DROP) {
                    break;
                }
            }
        }

        refreshWeight();
        return item;
    }

    /**
     * Adds Wear/Try On item to inventory<BR><BR>
     *
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item Identifier of the item to be added
     * @param actor : L2PcInstance Player requesting the item add
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new weared item
     */
    public L2ItemInstance addWearItem(String process, int itemId, L2PcInstance actor, L2Object reference) {
        // Surch the item in the inventory of the player
        L2ItemInstance item = getItemByItemId(itemId);

        // There is such item already in inventory
        if (item != null) {
            return item;
        }

        // Create and Init the L2ItemInstance corresponding to the Item Identifier and quantity
        // Add the L2ItemInstance object to _allObjects of L2world
        item = ItemTable.getInstance().createItem(process, itemId, 1, actor, reference);

        // Set Item Properties
        item.setWear(true); // "Try On" Item -> Don't save it in database
        item.setOwnerId(getOwnerId());
        item.setLocation(getBaseLocation());
        item.setLastChange((L2ItemInstance.ADDED));

        // Add item in inventory and equip it if necessary (item location defined)
        addItem(item);

        // Calculate the weight loaded by player
        refreshWeight();

        return item;
    }

    /**
     * Transfers item to another inventory
     *
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item Identifier of the item to be transfered
     * @param count : int Quantity of items to be transfered
     * @param actor : L2PcInstance Player requesting the item transfer
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item
     * in inventory
     */
    public L2ItemInstance transferItem(String process, int objectId, int count, ItemContainer target, L2PcInstance actor, L2Object reference) {
        if (target == null) {
            return null;
        }
        if (count <= 0) {
            return null;
        }

        L2ItemInstance sourceitem = getItemByObjectId(objectId);
        if (sourceitem == null) {
            return null;
        }

        L2ItemInstance targetitem = sourceitem.isStackable() ? target.getItemByItemId(sourceitem.getItemId()) : null;

        synchronized (sourceitem) {
            // check if this item still present in this container
            if (getItemByObjectId(objectId) != sourceitem) {
                return null;
            }

            // Check if requested quantity is available
            if (count > sourceitem.getCount()) {
                count = sourceitem.getCount();
            }

            // If possible, move entire item object
            if (sourceitem.getCount() == count && targetitem == null) {
                removeItem(sourceitem);
                target.addItem(process, sourceitem, actor, reference);
                targetitem = sourceitem;
            } else {
                if (sourceitem.getCount() > count) // If possible, only update counts
                {
                    sourceitem.changeCount(process, -count, actor, reference);
                } else // Otherwise destroy old item
                {
                    removeItem(sourceitem);
                    ItemTable.getInstance().destroyItem(process, sourceitem, actor, reference);
                }

                if (targetitem != null) // If possible, only update counts
                {
                    targetitem.changeCount(process, count, actor, reference);
                } else // Otherwise add new item
                {
                    targetitem = target.addItem(process, sourceitem.getItemId(), count, actor, reference);
                }
            }

            // Updates database
            sourceitem.updateDatabase(true);
            if (targetitem != sourceitem && targetitem != null) {
                targetitem.updateDatabase(true);
            }
            if (sourceitem.isAugmented()) {
                sourceitem.getAugmentation().removeBoni(actor);
            }
            refreshWeight();
        }
        return targetitem;
    }

    /**
     * Destroy item from inventory and updates database
     *
     * @param process : String Identifier of process triggering this action
     * @param item : L2ItemInstance to be destroyed
     * @param actor : L2PcInstance Player requesting the item destroy
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the destroyed item or the updated
     * item in inventory
     */
    public L2ItemInstance destroyItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference) {
        synchronized (item) {
            // check if item is present in this container
            if (!_items.contains(item)) {
                return null;
            }

            removeItem(item);
            ItemTable.getInstance().destroyItem(process, item, actor, reference);

            item.updateDatabase();
            refreshWeight();
        }
        return item;
    }

    /**
     * Destroy item from inventory by using its <B>objectID</B> and updates
     * database
     *
     * @param process : String Identifier of process triggering this action
     * @param objectId : int Item Instance identifier of the item to be
     * destroyed
     * @param count : int Quantity of items to be destroyed
     * @param actor : L2PcInstance Player requesting the item destroy
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the destroyed item or the updated
     * item in inventory
     */
    public L2ItemInstance destroyItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference) {
        if (count <= 0) {
            return null;
        }
        L2ItemInstance item = getItemByObjectId(objectId);
        if (item == null) {
            return null;
        }

        // Adjust item quantity
        if (item.getCount() > count) {
            synchronized (item) {
                item.changeCount(process, -count, actor, reference);
                item.setLastChange(L2ItemInstance.MODIFIED);

                item.updateDatabase();
                refreshWeight();
            }
            return item;
        } // Directly drop entire item
        else {
            return destroyItem(process, item, actor, reference);
        }
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and updates
     * database
     *
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item identifier of the item to be destroyed
     * @param count : int Quantity of items to be destroyed
     * @param actor : L2PcInstance Player requesting the item destroy
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the destroyed item or the updated
     * item in inventory
     */
    public L2ItemInstance destroyItemByItemId(String process, int itemId, int count, L2PcInstance actor, L2Object reference) {
        if (count <= 0) {
            return null;
        }
        L2ItemInstance item = getItemByItemId(itemId);
        if (item == null) {
            return null;
        }

        synchronized (item) {
            // Adjust item quantity
            if (item.getCount() > count) {
                item.changeCount(process, -count, actor, reference);
                item.setLastChange(L2ItemInstance.MODIFIED);
            } // Directly drop entire item
            else {
                return destroyItem(process, item, actor, reference);
            }

            item.updateDatabase();
            refreshWeight();
        }
        return item;
    }

    /**
     * Destroy all items from inventory and updates database
     *
     * @param process : String Identifier of process triggering this action
     * @param actor : L2PcInstance Player requesting the item destroy
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     */
    public synchronized void destroyAllItems(String process, L2PcInstance actor, L2Object reference) {
        for (L2ItemInstance item : _items) {
            destroyItem(process, item, actor, reference);
        }
    }

    /**
     * Get warehouse adena
     */
    public int getAdena() {
        /*
         * for (L2ItemInstance item : _items) if (item.getItemId() == 57) {
         * count = item.getCount(); return count;
         }
         */
        L2ItemInstance adena = findItemId(57, 2);
        if (adena == null) {
            return 0;
        }

        return adena.getCount();
    }

    public int getItemCount(int itemId) {
        L2ItemInstance adena = findItemId(itemId, 2);
        if (adena == null) {
            return 0;
        }

        return adena.getCount();
    }

    /**
     * Adds item to inventory for further adjustments.
     *
     * @param item : L2ItemInstance to be added from inventory
     */
    protected void addItem(L2ItemInstance item) {
        _items.add(item);
    }

    /**
     * Removes item from inventory for further adjustments.
     *
     * @param item : L2ItemInstance to be removed from inventory
     */
    protected void removeItem(L2ItemInstance item) {
        item.updateDatabase(true);
        //item.deleteMe();
        _items.remove(item);
    }

    public void updateDatabase(boolean commit) {
        updateDatabase(_items, commit);
    }

    /**
     * Update database with item
     *
     * @param items : ArrayList &lt;L2ItemInstance&gt; pointing out the list of
     * items
     */
    private void updateDatabase(ConcurrentLinkedQueue<L2ItemInstance> items, boolean commit) {
        if (getOwner() != null) {
            for (L2ItemInstance inst : items) {
                inst.updateDatabase(commit);

            }
        }
    }

    /**
     * Refresh the weight of equipment loaded
     */
    protected void refreshWeight() {
    }

    /**
     * Delete item object from world
     */
    public void deleteMe() {
        /*
         * try { updateDatabase(); } catch (Throwable t) {_log.log(Level.SEVERE,
         * "deletedMe()", t); } List<L2Object> items = new
         * FastList<L2Object>(_items); _items.clear();
         *
         * L2World.getInstance().removeObjects(items);
         */
        for (L2ItemInstance inst : getAllItems()) {
            inst.updateDatabase(true);
            inst.removeFromWorld();
        }
        getAllItems().clear();
    }

    /**
     * Update database with items in inventory
     */
    public void updateDatabase() {
        if (getOwner() != null) {
            for (L2ItemInstance item : _items) {
                if (item != null) {
                    item.updateDatabase();
                }
            }
        }
    }

    /**
     * Get back items in container from database
     */
    public void restore() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet inv = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT object_id FROM items WHERE owner_id=? AND (loc=?) ORDER BY object_id DESC");
            statement.setInt(1, getOwnerId());
            statement.setString(2, getBaseLocation().name());
            inv = statement.executeQuery();

            L2ItemInstance item;
            while (inv.next()) {
                int objectId = inv.getInt(1);
                item = L2ItemInstance.restoreFromDb(objectId);
                if (item == null) {
                    continue;
                }

                L2World.getInstance().storeObject(item);

                // If stackable item is found in inventory just add to current quantity
                if (item.isStackable() && getItemByItemId(item.getItemId()) != null) {
                    addItem("Restore", item, null, getOwner());
                } else {
                    addItem(item);
                }
            }
            refreshWeight();
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not restore container:", e);
        } finally {
            Close.CSR(con, statement, inv);
        }
    }

    public boolean validateCapacity(int slots) {
        return true;
    }

    public boolean validateWeight(int weight) {
        return true;
    }

    public FastTable<L2ItemInstance> listItems(int type) {
        final FastTable<L2ItemInstance> items = new FastTable<L2ItemInstance>();
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;

        try {
            String loc = "";
            switch (type) {
                case 1:
                    loc = "WAREHOUSE";
                    break;
                case 2:
                case 3:
                    loc = "CLANWH";
                    break;
                case 4:
                    loc = "FREIGHT";
                    break;
            }

            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT object_id FROM items WHERE owner_id=? AND (loc=?) ORDER BY object_id DESC");
            statement.setInt(1, getOwnerId());
            statement.setString(2, loc);
            rset = statement.executeQuery();

            L2ItemInstance item;
            while (rset.next()) {
                final int objectId = rset.getInt(1);
                item = L2ItemInstance.restoreFromDb(objectId);
                if (item == null) {
                    continue;
                }
                items.add(item);
            }
        } catch (final Exception e) {
            _log.log(Level.SEVERE, "could not restore warehouse:", e);
        } finally {
            Close.CSR(con, statement, rset);
        }
        return items;
    }

    public synchronized void addItem(L2ItemInstance newItem, int whType) {
        L2ItemInstance oldItem = findItemId(newItem.getItemId(), whType);

        // non-stackable items are simply added to DB
        if (!newItem.isStackable() || oldItem == null) {
            newItem.setOwnerId(getOwnerId());
            if (whType == 1) {
                newItem.setLocation(ItemLocation.WAREHOUSE);
            } else if (whType == 4) {
                newItem.setLocation(ItemLocation.FREIGHT);
            } else {
                newItem.setLocation(ItemLocation.CLANWH);
            }
            newItem.updateDatabase();
            return;
        }
        int newCount = oldItem.getCount() + newItem.getCount();
        long cur = (long) oldItem.getCount() + (long) newItem.getCount();
        if (oldItem.getItemId() == 57 && cur >= Integer.MAX_VALUE) {
            newCount = Integer.MAX_VALUE;
        }
        oldItem.setCount(newCount);
        oldItem.updateDatabase(true);
        newItem.deleteMe();
    }

    public synchronized void addAdena(int id, int count) {
        L2ItemInstance oldItem = findItemId(id, 2);

        // non-stackable items are simply added to DB
        if (oldItem == null) {
            L2ItemInstance newItem = ItemTable.getInstance().createItem("addAdena", id, count, null, null);
            newItem.setOwnerId(getOwnerId());
            newItem.setLocation(ItemLocation.CLANWH);
            newItem.updateDatabase();
            return;
        }
        int newCount = oldItem.getCount() + count;
        long cur = (long) oldItem.getCount() + (long) count;
        if (oldItem.getItemId() == 57 && cur >= Integer.MAX_VALUE) {
            newCount = Integer.MAX_VALUE;
        }
        oldItem.setCount(newCount);
        oldItem.updateDatabase();
        //newItem.deleteMe();
    }

    public L2ItemInstance reduceAdena(int itemId, int count, L2PcInstance actor, L2Object reference) {
        L2ItemInstance item = findItemId(itemId, 2);
        if (item == null) {
            return null;
        }

        synchronized (item) {
            // Adjust item quantity
            if (item.getCount() > count) {
                item.changeCount("reduceAdena", -count, actor, reference);
                item.setLastChange(L2ItemInstance.MODIFIED);
            } // Directly drop entire item
            else {
                return destroyItem("reduceAdena", item, actor, reference);
            }

            item.updateDatabase(true);
            refreshWeight();
        }
        return item;
    }

    public L2ItemInstance findItemId(final int itemId, final int whType) {
        L2ItemInstance foundItem = null;
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try {
            String loc = "";
            switch (whType) {
                case 1:
                    loc = "WAREHOUSE";
                    break;
                case 2:
                case 3:
                    loc = "CLANWH";
                    break;
                case 4:
                    loc = "FREIGHT";
                    break;
            }
            //String loc = whType == 1 ? "WAREHOUSE" : "CLANWH";

            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT object_id FROM items WHERE owner_id=? AND loc=? AND item_id=?");
            statement.setInt(1, getOwnerId());
            statement.setString(2, loc);
            statement.setInt(3, itemId);
            rset = statement.executeQuery();

            if (rset.next()) {
                foundItem = L2ItemInstance.restoreFromDb(rset.getInt(1));
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not list warehouse: ", e);
        } finally {
            Close.CSR(con, statement, rset);
        }
        return foundItem;
    }

    public synchronized L2ItemInstance getItemByObj(int objectId, int count, L2PcInstance player) {
        //System.out.println("getItemByObj");
		/*
         * L2ItemInstance item = (L2ItemInstance)
         * L2World.getInstance().findObject(objectId); if(item == null) {
         * System.out.println("restore"); item =
         * L2ItemInstance.restoreFromDb(objectId);
         }
         */

        L2ItemInstance item = L2ItemInstance.restoreFromDb(objectId);
        if (item == null) {
            _log.fine("Warehouse.destroyItem: can't destroy objectId: " + objectId + ", count: " + count);
            return null;
        }

        if (item.getLocation() != ItemLocation.CLANWH && item.getLocation() != ItemLocation.WAREHOUSE && item.getLocation() != ItemLocation.FREIGHT) {
            _log.warning("WARNING get item not in WAREHOUSE via WAREHOUSE: item objid=" + item.getObjectId() + " ownerid=" + item.getOwnerId());
            return null;
        }

        if (!item.isStackable()) {
            item.setWhFlag(true);
        }

        //System.out.println("wThink: " + item.getCount() + "wvs: " + count);
        if (item.getCount() <= 0 || count <= 0) {
            return null;
        }

        if (item.getCount() <= count) {
            item.setLocation(ItemLocation.VOID);
            item.updateDatabase(true);
            return item;
        }

        item.setCount(item.getCount() - count);
        item.updateDatabase(true);

        L2ItemInstance Newitem = ItemTable.getInstance().createItem("withdrawwh", item.getItem().getItemId(), count, player, player.getLastFolkNPC());
        Newitem.setWhFlag(true);

        return Newitem;
    }

    public FastTable<L2ItemInstance> getItemsShuffle() {
        L2ItemInstance[] arr = _items.toArray(new L2ItemInstance[_items.size()]);
        FastTable<L2ItemInstance> items = new FastTable<L2ItemInstance>();
        for (L2ItemInstance item : arr) {
            if (item == null) {
                continue;
            }
            items.add(item);
        }
        Collections.shuffle(items);
        return items;
    }

    public FastTable<L2ItemInstance> getItemsShufflePkDrop(L2PcInstance player) {
        L2ItemInstance[] arr = _items.toArray(new L2ItemInstance[_items.size()]);
        FastTable<L2ItemInstance> items = new FastTable<L2ItemInstance>();
        for (L2ItemInstance item : arr) {
            if (item == null) {
                continue;
            }

            // Don't drop
            if ((item.isAugmented() && !Config.CAN_DROP_AUGMENTS)
                    || item.isShadowItem()
                    || item.getItemId() == 57
                    || item.getItem().getType2() == L2Item.TYPE2_QUEST
                    || Config.KARMA_LIST_NONDROPPABLE_ITEMS.contains(item.getItemId())
                    || (player.getPet() != null && player.getPet().getControlItemId() == item.getItemId())) {
                continue;
            }
            items.add(item);
        }
        Collections.shuffle(items);
        return items;
    }

    public boolean isFreight() {
        return false;
    }

    public int getWarehouseType() {
        return 0;
    }

    private int _whId = 0;

    public void setWhId(int id) {
        _whId = id;
    }

    public int getWhId() {
        return _whId;
    }
}
