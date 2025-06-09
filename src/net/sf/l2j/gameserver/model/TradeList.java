/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model;

import java.util.List;
import java.util.logging.Logger;

import javolution.text.TextBuilder;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2EtcItemType;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * @author Advi
 *
 */
public class TradeList {

    public static class TradeItem {

        private int _objectId;
        private L2Item _item;
        private int _enchant;
        private int _count;
        private int _price;

        public TradeItem(L2ItemInstance item, int count, int price) {
            _objectId = item.getObjectId();
            _item = item.getItem();
            _enchant = item.getEnchantLevel();
            _count = count;
            _price = price;
        }

        public TradeItem(L2Item item, int count, int price, int enchLvl) {
            _objectId = 0;
            _item = item;
            _enchant = enchLvl;
            _count = count;
            _price = price;
        }

        public TradeItem(TradeItem item, int count, int price) {
            _objectId = item.getObjectId();
            _item = item.getItem();
            _enchant = item.getEnchant();
            _count = count;
            _price = price;
        }

        public void setObjectId(int objectId) {
            _objectId = objectId;
        }

        public int getObjectId() {
            return _objectId;
        }

        public L2Item getItem() {
            return _item;
        }

        public void setEnchant(int enchant) {
            _enchant = enchant;
        }

        public int getEnchant() {
            return _enchant;
        }

        public void setCount(int count) {
            _count = count;
        }

        public int getCount() {
            return _count;
        }

        public void setPrice(int price) {
            _price = price;
        }

        public int getPrice() {
            return _price;
        }
    }
    private static Logger _log = AbstractLogger.getLogger(TradeList.class.getName());
    private L2PcInstance _owner;
    private L2PcInstance _partner;
    private FastList<TradeItem> _items;
    private String _title;
    private boolean _packaged;
    private boolean _confirmed = false;
    private boolean _locked = false;

    public TradeList(L2PcInstance owner) {
        _items = new FastList<TradeItem>();
        _owner = owner;
    }

    public L2PcInstance getOwner() {
        return _owner;
    }

    public void setPartner(L2PcInstance partner) {
        _partner = partner;
    }

    public L2PcInstance getPartner() {
        return _partner;
    }

    public void setTitle(String title) {
        title = ltrim(title);
        title = rtrim(title);
        title = itrim(title);
        title = lrtrim(title);

        if (title.isEmpty()) {
            title = "^_^";
        }

        if (title.length() > 16) {
            title = title.substring(0, 16);
        }
        _title = title;
    }

    public String getTitle() {
        return _title;
    }

    public boolean isLocked() {
        return _locked;
    }

    public boolean isConfirmed() {
        return _confirmed;
    }

    public boolean isPackaged() {
        return _packaged;
    }

    public void setPackaged(boolean value) {
        _packaged = value;
    }

    /**
     * Retrieves items from TradeList
     */
    public FastList<TradeItem> getItems() {
        return _items;
    }

    /**
     * Returns the list of items in inventory available for transaction
     * @return L2ItemInstance : items in inventory
     */
    public FastList<TradeList.TradeItem> getAvailableItems(PcInventory inventory) {
        FastList<TradeList.TradeItem> list = new FastList<TradeList.TradeItem>();
        for (FastList.Node<TradeList.TradeItem> n = _items.head(), end = _items.tail(); (n = n.getNext()) != end;) {
            TradeList.TradeItem item = n.getValue();

            item = new TradeItem(item, item.getCount(), item.getPrice());
            inventory.adjustAvailableItem(item);
            list.add(item);
        }
        return list;
    }

    /**
     * Returns Item List size
     */
    public int getItemCount() {
        return _items.size();
    }

    /**
     * Adjust available item from Inventory by the one in this list
     * @param item : L2ItemInstance to be adjusted
     * @return TradeItem representing adjusted item
     */
    public TradeItem adjustAvailableItem(L2ItemInstance item) {
        if (item.isStackable()) {
            for (TradeItem exclItem : _items) {
                if (exclItem.getItem().getItemId() == item.getItemId()) {
                    if (item.getCount() <= exclItem.getCount()) {
                        return null;
                    } else {
                        return new TradeItem(item, item.getCount() - exclItem.getCount(), item.getReferencePrice());
                    }
                }
            }
        }
        return new TradeItem(item, item.getCount(), item.getReferencePrice());
    }

    /**
     * Adjust ItemRequest by corresponding item in this list using its <b>ObjectId</b>
     * @param item : ItemRequest to be adjusted
     */
    public void adjustItemRequest(ItemRequest item) {
        for (TradeItem filtItem : _items) {
            if (filtItem.getObjectId() == item.getObjectId()) {
                if (filtItem.getCount() < item.getCount()) {
                    item.setCount(filtItem.getCount());
                }
                return;
            }
        }
        item.setCount(0);
    }

    /**
     * Adjust ItemRequest by corresponding item in this list using its <b>ItemId</b>
     * @param item : ItemRequest to be adjusted
     */
    public void adjustItemRequestByItemId(ItemRequest item) {
        for (TradeItem filtItem : _items) {
            if (filtItem.getItem().getItemId() == item.getItemId()) {
                if (filtItem.getCount() < item.getCount()) {
                    item.setCount(filtItem.getCount());
                }
                return;
            }
        }
        item.setCount(0);
    }

    /**
     * Add simplified item to TradeList
     * @param objectId : int
     * @param count : int
     * @return
     */
    public synchronized TradeItem addItem(int objectId, int count) {
        return addItem(objectId, count, 0);
    }

    /**
     * Add item to TradeList
     * @param objectId : int
     * @param count : int
     * @param price : int
     * @return
     */
    public synchronized TradeItem addItem(int objectId, int count, int price) {
        if (isLocked()) {
            _log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
            _owner.kick();
            return null;
        }
        L2Object o = L2World.getInstance().findObject(objectId);

        if (o == null || !(o.isL2Item())) {
            _log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
            _owner.kick();
            return null;
        }

        L2ItemInstance item = (L2ItemInstance) o;

        if (!item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST) {
            return null;
        }

        if (count > item.getCount()) {
            return null;
        }

        if (!item.isStackable() && count > 1) {
            _log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
            _owner.kick();
            return null;
        }
        for (TradeItem checkitem : _items) {
            if (checkitem.getObjectId() == objectId) {
                return null;
            }
        }
        TradeItem titem = new TradeItem(item, count, price);
        _items.add(titem);

        // If Player has already confirmed this trade, invalidate the confirmation
        invalidateConfirmation();
        return titem;
    }

    /**
     * Add item to TradeList
     * @param objectId : int
     * @param count : int
     * @param price : int
     * @return
     */
    public synchronized TradeItem addItemByItemId(int itemId, int count, int price, int enchLvl) {
        if (isLocked()) {
            _log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
            _owner.kick();
            return null;
        }

        L2Item item = ItemTable.getInstance().getTemplate(itemId);
        if (item == null) {
            _log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
            _owner.kick();
            return null;
        }

        if (!item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST) {
            return null;
        }

        if (!item.isStackable() && count > 1) {
            _log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
            _owner.kick();
            return null;
        }

        TradeItem titem = new TradeItem(item, count, price, enchLvl);
        _items.add(titem);

        // If Player has already confirmed this trade, invalidate the confirmation
        invalidateConfirmation();
        return titem;
    }

    /**
     * Remove item from TradeList
     * @param objectId : int
     * @param count : int
     * @return
     */
    public synchronized TradeItem removeItem(int objectId, int itemId, int count) {
        if (isLocked()) {
            _log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
            _owner.kick();
            return null;
        }

        for (TradeItem titem : _items) {
            if (titem.getObjectId() == objectId || titem.getItem().getItemId() == itemId) {
                // If Partner has already confirmed this trade, invalidate the confirmation
                if (_partner != null) {
                    TradeList partnerList = _partner.getActiveTradeList();
                    if (partnerList == null) {
                        _log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
                        return null;
                    }
                    partnerList.invalidateConfirmation();
                }

                // Reduce item count or complete item
                if (count != -1 && titem.getCount() > count) {
                    titem.setCount(titem.getCount() - count);
                } else {
                    _items.remove(titem);
                }

                return titem;
            }
        }
        return null;
    }

    /**
     * Update items in TradeList according their quantity in owner inventory
     */
    public synchronized void updateItems() {
        for (TradeItem titem : _items) {
            L2ItemInstance item = _owner.getInventory().getItemByObjectId(titem.getObjectId());
            if (item == null || titem.getCount() < 1) {
                removeItem(titem.getObjectId(), -1, -1);
            } else if (item.getCount() < titem.getCount()) {
                titem.setCount(item.getCount());
            }
        }
    }

    /**
     * Lockes TradeList, no further changes are allowed
     */
    public void lock() {
        _locked = true;
    }

    /**
     * Clears item list
     */
    public void clear() {
        _items.clear();
        _locked = false;
    }

    /**
     * Confirms TradeList
     * @return : boolean
     */
    public boolean confirm() {
        if (_confirmed) {
            return true; // Already confirmed
        }
        // If Partner has already confirmed this trade, proceed exchange
        if (_partner != null) {
            TradeList partnerList = _partner.getActiveTradeList();
            if (partnerList == null) {
                _log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
                return false;
            }

            // Synchronization order to avoid deadlock
            TradeList sync1, sync2;
            if (getOwner().getObjectId() > partnerList.getOwner().getObjectId()) {
                sync1 = partnerList;
                sync2 = this;
            } else {
                sync1 = this;
                sync2 = partnerList;
            }

            synchronized (sync1) {
                synchronized (sync2) {
                    _confirmed = true;
                    if (partnerList.isConfirmed()) {
                        partnerList.lock();
                        lock();
                        if (!partnerList.validate()) {
                            return false;
                        }
                        if (!validate()) {
                            return false;
                        }

                        doExchange(partnerList);
                    } else {
                        _partner.onTradeConfirm(_owner);
                    }
                }
            }
        } else {
            _confirmed = true;
        }

        return _confirmed;
    }

    /**
     * Cancels TradeList confirmation
     */
    public void invalidateConfirmation() {
        _confirmed = false;
    }

    /**
     * Validates TradeList with owner inventory
     */
    private boolean validate() {
        // Check for Owner validity
        if (_owner == null || L2World.getInstance().findObject(_owner.getObjectId()) == null) {
            _log.warning("Invalid owner of TradeList");
            return false;
        }

        // Check for Item validity
        for (TradeItem titem : _items) {
            L2ItemInstance item = _owner.checkItemManipulation(titem.getObjectId(), titem.getCount(), "transfer");
            if (item == null || titem.getCount() <= 0) {
                _log.warning(_owner.getName() + ": Invalid Item in TradeList");
                _owner.logout();
                return false;
            }
        }

        return true;
    }

    /**
     * Transfers all TradeItems from inventory to partner
     */
    private boolean TransferItems(L2PcInstance partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU) {
        String date = "";
        TextBuilder tb = null;
        if (Config.LOG_ITEMS) {
            date = Log.getTime();
            tb = new TextBuilder();
        }
        for (TradeItem titem : _items) {
            if (titem == null) {
                continue;
            }
            /*L2ItemInstance oldItem = _owner.getInventory().getItemByObjectId(titem.getObjectId());
            if (oldItem == null) {
            return false;
            }
            L2ItemInstance newItem = _owner.getInventory().transferItem("Trade", titem.getObjectId(), titem.getCount(), partner.getInventory(), _owner, _partner);
            if (newItem == null) {
            return false;
            }*/
            L2ItemInstance newItem = _owner.getInventory().dropItem("Trade", titem.getObjectId(), titem.getCount(), _owner, _partner, true);
            if (newItem == null) {
                return false;
            }
            if (newItem.getCount() < titem.getCount()) {
                return false;
            }
            partner.getInventory().addItem("Trade", newItem, _partner, _owner);

            // Add changes to inventory update packets
            /*if (ownerIU != null) {
            if (oldItem.getCount() > 0 && oldItem != newItem) {
            ownerIU.addModifiedItem(oldItem);
            } else {
            ownerIU.addRemovedItem(oldItem);
            }
            }
            
            if (partnerIU != null) {
            if (newItem.getCount() > titem.getCount()) {
            partnerIU.addModifiedItem(newItem);
            } else {
            partnerIU.addNewItem(newItem);
            }
            }*/
            if (Config.LOG_ITEMS && newItem != null) {
                String act = "TRADE " + newItem.getItemName() + "(" + titem.getCount() + ")(+" + newItem.getEnchantLevel() + ")(" + newItem.getObjectId() + ") #(player " + _owner.getName() + ", account: " + _owner.getAccountName() + ", ip: " + _owner.getIP() + ", hwid: " + _owner.getHWID() + ")->(player " + partner.getName() + ", account: " + partner.getAccountName() + ", ip: " + partner.getIP() + ", hwid: " + partner.getHWID() + ")";
                tb.append(date + act + "\n");
            }
        }
        if (Config.LOG_ITEMS && tb != null) {
            Log.item(tb.toString(), Log.TRADE);
            tb.clear();
            tb = null;
        }
        return true;
    }

    /**
     * Count items slots
     */
    public int countItemsSlots(L2PcInstance partner) {
        int slots = 0;

        for (TradeItem item : _items) {
            if (item == null) {
                continue;
            }
            L2Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
            if (template == null) {
                continue;
            }
            if (!template.isStackable()) {
                slots += item.getCount();
            } else if (partner.getInventory().getItemByItemId(item.getItem().getItemId()) == null) {
                slots++;
            }
        }

        return slots;
    }

    /**
     * Calc weight of items in tradeList
     */
    public int calcItemsWeight() {
        int weight = 0;

        for (TradeItem item : _items) {
            if (item == null) {
                continue;
            }
            L2Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
            if (template == null) {
                continue;
            }
            weight += item.getCount() * template.getWeight();
        }

        return weight;
    }

    /**
     * Proceeds with trade
     */
    private void doExchange(TradeList partnerList) {
        boolean success = false;
        // check weight and slots
        if ((!getOwner().getInventory().validateWeight(partnerList.calcItemsWeight()))
                || !(partnerList.getOwner().getInventory().validateWeight(calcItemsWeight()))) {
            partnerList.getOwner().sendPacket(Static.WEIGHT_LIMIT_EXCEEDED);
            getOwner().sendPacket(Static.WEIGHT_LIMIT_EXCEEDED);
        } else if ((!getOwner().getInventory().validateCapacity(partnerList.countItemsSlots(getOwner())))
                || (!partnerList.getOwner().getInventory().validateCapacity(countItemsSlots(partnerList.getOwner())))) {
            partnerList.getOwner().sendPacket(Static.SLOTS_FULL);
            getOwner().sendPacket(Static.SLOTS_FULL);
        } else {
            /*if (partnerList.getAugmentCount() >= 1)
            {
            int BlueEva = partnerList.getAugmentCount() * 2;
            if (getOwner().getItemCount(4355) >= BlueEva)
            {
            getOwner().sendMessage("Оплата налога: "+BlueEva+" Blue Eva");
            
            L2ItemInstance item = getOwner().getInventory().getItemByItemId(4355);
            
            if (item == null)
            {
            getOwner().cancelActiveTrade();
            getOwner().sendMessage("Не хватает Blue Eva; Налог: "+BlueEva+" Blue Eva");
            success = false;
            return;
            }
            
            getOwner().destroyItemByItemId("TradeList", 4355, BlueEva, getOwner(), false);
            getOwner().sendMessage(BlueEva+" Blue Eva исчезло");
            }
            else
            {
            getOwner().cancelActiveTrade();
            partnerList.getOwner().cancelActiveTrade();
            getOwner().sendMessage("Не хватает Blue Eva; Налог: "+BlueEva+" Blue Eva");
            success = false;
            return;
            }
            }*/

            // Prepare inventory update packet
            /*InventoryUpdate ownerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
            InventoryUpdate partnerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
            
            // Transfer items
            partnerList.TransferItems(getOwner(), partnerIU, ownerIU);
            TransferItems(partnerList.getOwner(), ownerIU, partnerIU);
            
            // Send inventory update packet
            if (ownerIU != null) _owner.sendPacket(ownerIU);
            else _owner.sendItems(false);
            
            if (partnerIU != null) _partner.sendPacket(partnerIU);
            else _partner.sendItems(false);*/

            // Transfer items
            partnerList.TransferItems(getOwner(), null, null);
            TransferItems(partnerList.getOwner(), null, null);

            _owner.sendItems(false);
            _partner.sendItems(false);

            // Update current load as well
            /*StatusUpdate playerSU = new StatusUpdate(_owner.getObjectId());
            playerSU.addAttribute(StatusUpdate.CUR_LOAD, _owner.getCurrentLoad());
            _owner.sendPacket(playerSU);
            playerSU = new StatusUpdate(_partner.getObjectId());
            playerSU.addAttribute(StatusUpdate.CUR_LOAD, _partner.getCurrentLoad());
            _partner.sendPacket(playerSU);*/
            _owner.sendChanges();
            _partner.sendChanges();

            success = true;
        }
        // Finish the trade
        partnerList.getOwner().onTradeFinish(success);
        getOwner().onTradeFinish(success);
        partnerList.getOwner().setTransactionType(TransactionType.NONE);
        getOwner().setTransactionType(TransactionType.NONE);
        partnerList.getOwner().setTransactionRequester(null);
        getOwner().setTransactionRequester(null);
    }

    /**
     * Buy items from this PrivateStore list
     * @return : boolean true if success
     */
    public synchronized boolean PrivateStoreBuy(L2PcInstance player, ItemRequest[] items, int price) {
        if (_locked) {
            return false;
        }
        if (items == null) {
            return false;
        }
        if (!validate()) {
            lock();
            return false;
        }

        int slots = 0;
        int weight = 0;

        for (ItemRequest item : items) {
            if (item == null) {
                continue;
            }
            L2Item template = ItemTable.getInstance().getTemplate(item.getItemId());
            if (template == null) {
                continue;
            }
            weight += item.getCount() * template.getWeight();
            if (!template.isStackable()) {
                slots += item.getCount();
            } else if (player.getInventory().getItemByItemId(item.getItemId()) == null) {
                slots++;
            }
        }

        if (!player.getInventory().validateWeight(weight)) {
            player.sendPacket(Static.WEIGHT_LIMIT_EXCEEDED);
            return false;
        }

        if (!player.getInventory().validateCapacity(slots)) {
            player.sendPacket(Static.SLOTS_FULL);
            return false;
        }

        PcInventory ownerInventory = _owner.getInventory();
        PcInventory playerInventory = player.getInventory();

        // Prepare inventory update packets
        InventoryUpdate ownerIU = new InventoryUpdate();
        InventoryUpdate playerIU = new InventoryUpdate();

        // Transfer adena
        if (price > playerInventory.getAdena()) {
            lock();
            return false;
        }
        //;
        L2ItemInstance adenaItem = playerInventory.getAdenaInstance();
        playerInventory.reduceAdena("PrivateStore", price, player, _owner);
        playerIU.addItem(adenaItem);
        ownerInventory.addAdena("PrivateStore", price, _owner, player);
        ownerIU.addItem(ownerInventory.getAdenaInstance());

        String date = "";
        TextBuilder tb = null;
        if (Config.LOG_ITEMS) {
            date = Log.getTime();
            tb = new TextBuilder();
        }

        // Transfer items
        for (ItemRequest item : items) {
            if (item == null) {
                continue;
            }

            //Check if requested item is sill on the list and adjust its count
            adjustItemRequest(item);
            if (item.getCount() == 0) {
                continue;
            }

            // Check if requested item is available for manipulation
            L2ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
            if (oldItem == null) {
                lock();
                return false;
            }

            // Proceed with item transfer
            L2ItemInstance newItem = ownerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), playerInventory, _owner, player);
            if (newItem == null) {
                return false;
            }
            removeItem(item.getObjectId(), -1, item.getCount());

            // Add changes to inventory update packets
            if (oldItem.getCount() > 0 && oldItem != newItem) {
                ownerIU.addModifiedItem(oldItem);
            } else {
                ownerIU.addRemovedItem(oldItem);
            }

            if (newItem.getCount() > item.getCount()) {
                playerIU.addModifiedItem(newItem);
            } else {
                playerIU.addNewItem(newItem);
            }

            // Send messages about the transaction to both players
            if (newItem.isStackable()) {
                _owner.sendPacket(SystemMessage.id(SystemMessageId.S1_PURCHASED_S3_S2_S).addString(player.getName()).addItemName(newItem.getItemId()).addNumber(item.getCount()));
                player.sendPacket(SystemMessage.id(SystemMessageId.PURCHASED_S3_S2_S_FROM_S1).addString(_owner.getName()).addItemName(newItem.getItemId()).addNumber(item.getCount()));
            } else {
                _owner.sendPacket(SystemMessage.id(SystemMessageId.S1_PURCHASED_S2).addString(player.getName()).addItemName(newItem.getItemId()));
                player.sendPacket(SystemMessage.id(SystemMessageId.PURCHASED_S2_FROM_S1).addString(_owner.getName()).addItemName(newItem.getItemId()));
            }
            if (Config.LOG_ITEMS) {
                String act = "PrivateStoreBuy " + newItem.getItemName() + "(" + item.getCount() + ")(+" + newItem.getEnchantLevel() + ")(" + newItem.getObjectId() + ")(store player " + _owner.getName() + ", account: " + _owner.getAccountName() + ", ip: " + _owner.getIP() + ", hwid: " + player.getHWID() + ") #(player " + player.getName() + ", account: " + player.getAccountName() + ", ip: " + player.getIP() + ", hwid: " + player.getHWID() + ")";
                tb.append(date + act + "\n");
            }
        }
        if (Config.LOG_ITEMS && tb != null) {
            Log.item(tb.toString(), Log.PRIVATE_STORE);
            tb.clear();
            tb = null;
        }

        // Send inventory update packet
        _owner.sendPacket(ownerIU);
        player.sendPacket(playerIU);
        return true;
    }

    /**
     * Sell items to this PrivateStore list
     * @return : boolean true if success
     */
    public synchronized boolean PrivateStoreSell(L2PcInstance player, ItemRequest[] items, int price) {
        if (_locked) {
            return false;
        }

        if (items == null) {
            return false;
        }

        PcInventory ownerInventory = _owner.getInventory();
        if (price > ownerInventory.getAdena()) {
            return false;
        }
        PcInventory playerInventory = player.getInventory();

        //we must check item are available before begining transaction, TODO: should we remove that check when transfering items as it's done here? (there might be synchro problems if player clicks fast if we remove it)
        // also check if augmented items are traded. If so, cancel it...
        for (ItemRequest item : items) {
            if (item == null) {
                continue;
            }
            // Check if requested item is available for manipulation
            L2ItemInstance oldItem = player.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
            if (oldItem == null) {
                return false;
            }
            if (oldItem.getAugmentation() != null) {
                String msg = "Transaction failed. Augmented items may not be exchanged.";
                _owner.sendMessage(msg);
                player.sendMessage(msg);
                return false;
            }
        }

        // Prepare inventory update packet
        InventoryUpdate ownerIU = new InventoryUpdate();
        InventoryUpdate playerIU = new InventoryUpdate();

        String date = "";
        TextBuilder tb = null;
        if (Config.LOG_ITEMS) {
            date = Log.getTime();
            tb = new TextBuilder();
        }

        // Transfer items
        for (ItemRequest item : items) {
            if (item == null) {
                continue;
            }

            // Check if requested item is sill on the list and adjust its count
            adjustItemRequestByItemId(item);
            if (item.getCount() == 0) {
                continue;
            }

            // Check if requested item is available for manipulation
            L2ItemInstance oldItem = player.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
            if (oldItem == null) {
                return false;
            }

            // Proceed with item transfer
            L2ItemInstance newItem = playerInventory.transferItem("PrivateStore", item.getObjectId(), item.getCount(), ownerInventory, player, _owner);
            if (newItem == null) {
                return false;
            }
            if (oldItem.getEnchantLevel() != newItem.getEnchantLevel()) {
                return false;
            }
            removeItem(-1, item.getItemId(), item.getCount());

            // Add changes to inventory update packets
            if (oldItem.getCount() > 0 && oldItem != newItem) {
                playerIU.addModifiedItem(oldItem);
            } else {
                playerIU.addRemovedItem(oldItem);
            }
            if (newItem.getCount() > item.getCount()) {
                ownerIU.addModifiedItem(newItem);
            } else {
                ownerIU.addNewItem(newItem);
            }

            // Send messages about the transaction to both players
            if (newItem.isStackable()) {
                _owner.sendPacket(SystemMessage.id(SystemMessageId.PURCHASED_S3_S2_S_FROM_S1).addString(player.getName()).addItemName(newItem.getItemId()).addNumber(item.getCount()));
                player.sendPacket(SystemMessage.id(SystemMessageId.S1_PURCHASED_S3_S2_S).addString(_owner.getName()).addItemName(newItem.getItemId()).addNumber(item.getCount()));
            } else {
                if (newItem.getEnchantLevel() > 0) {
                    _owner.sendPacket(SystemMessage.id(SystemMessageId.PURCHASED_S2_S3_FROM_S1).addString(player.getName()).addNumber(newItem.getEnchantLevel()).addItemName(newItem.getItemId()));
                    player.sendPacket(SystemMessage.id(SystemMessageId.S1_PURCHASED_S2_S3).addString(_owner.getName()).addNumber(newItem.getEnchantLevel()).addItemName(newItem.getItemId()));
                } else {
                    _owner.sendPacket(SystemMessage.id(SystemMessageId.PURCHASED_S2_FROM_S1).addString(player.getName()).addItemName(newItem.getItemId()));
                    player.sendPacket(SystemMessage.id(SystemMessageId.S1_PURCHASED_S2).addString(_owner.getName()).addItemName(newItem.getItemId()));
                }
            }
            if (Config.LOG_ITEMS) {
                String act = "PrivateStoreSell " + newItem.getItemName() + "(" + item.getCount() + ")(+" + newItem.getEnchantLevel() + ")(" + newItem.getObjectId() + ")(store player " + _owner.getName() + ", account: " + _owner.getAccountName() + ", ip: " + _owner.getIP() + ", hwid: " + player.getHWID() + ") #(player " + player.getName() + ", account: " + player.getAccountName() + ", ip: " + player.getIP() + ", hwid: " + player.getHWID() + ")";
                tb.append(date + act + "\n");
            }
        }
        if (Config.LOG_ITEMS && tb != null) {
            Log.item(tb.toString(), Log.PRIVATE_STORE);
            tb.clear();
            tb = null;
        }

        // Transfer adena
        if (price > ownerInventory.getAdena()) {
            return false;
        }
        L2ItemInstance adenaItem = ownerInventory.getAdenaInstance();
        ownerInventory.reduceAdena("PrivateStore", price, _owner, player);
        ownerIU.addItem(adenaItem);
        playerInventory.addAdena("PrivateStore", price, player, _owner);
        playerIU.addItem(playerInventory.getAdenaInstance());

        // Send inventory update packet
        _owner.sendPacket(ownerIU);
        player.sendPacket(playerIU);
        return true;
    }

    /**
     * @param objectId
     * @return
     */
    public TradeItem getItem(int objectId) {
        for (TradeItem item : _items) {
            if (item.getObjectId() == objectId) {
                return item;
            }
        }
        return null;
    }
    //передача ауг. пушек
    private int _augCount = 0;

    public void updateAugmentCount() {
        _augCount += 1;
    }

    public void clearAugmentCount() {
        _augCount = 0;
    }

    public int getAugmentCount() {
        return _augCount;
    }

    //
    public static String ltrim(String source) {
        return source.replaceAll("^\\s+", "");
    }

    public static String rtrim(String source) {
        return source.replaceAll("\\s+$", "");
    }

    public static String itrim(String source) {
        return source.replaceAll("\\b\\s{2,}\\b", " ");
    }

    public static String trim(String source) {
        return itrim(ltrim(rtrim(source)));
    }

    public static String lrtrim(String source) {
        return ltrim(rtrim(source));
    }
}
