package net.sf.l2j.gameserver.network.clientpackets;

import java.util.HashMap;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastTable;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.ClanWarehouse;
import net.sf.l2j.gameserver.model.ItemContainer;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.PcInventory;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.PcFreight;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.util.Log;

/**
 * This class ...
 *
 * 31 SendWareHouseDepositList cd (dd)
 *
 * @version $Revision: 1.3.4.5 $ $Date: 2005/04/11 10:06:09 $
 */
public final class SendWareHouseDepositList extends L2GameClientPacket {

    private static Logger _log = Logger.getLogger(SendWareHouseDepositList.class.getName());
    private HashMap<Integer, Integer> _items;
    private FastTable<L2ItemInstance> _itemsOnWarehouse;

    @Override
    protected void readImpl() {
        if (!Config.ALLOW_WAREHOUSE) {
            _items = null;
            return;
        }
        int itemsCount = readD();
        if (itemsCount * 8 > _buf.remaining() || itemsCount > Config.MAX_ITEM_IN_PACKET || itemsCount <= 0) {
            _items = null;
            return;
        }

        _items = new HashMap<Integer, Integer>(itemsCount + 1, 0.999f);
        for (int i = 0; i < itemsCount; i++) {
            int obj_id = readD();
            int itemQuantity = readD();
            if (itemQuantity <= 0) {
                _items = null;
                return;
            }
            _items.put(obj_id, itemQuantity);
        }
    }

    @Override
    protected void runImpl() {
        if (!Config.ALLOW_WAREHOUSE
                || _items == null) {
            return;
        }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPAY() < 100) {
            player.sendActionFailed();
            return;
        }

        player.sCPAY();

        //////////////
        if (player.isTransactionInProgress()) {
            player.sendActionFailed();
            return;
        }

        if (!player.tradeLeft()) {
            player.sendActionFailed();
            return;
        }

        if (player.getActiveTradeList() != null) {
            player.cancelActiveTrade();
            player.sendActionFailed();
            return;
        }

        if (player.getPrivateStoreType() != 0) {
            player.sendActionFailed();
            return;
        }

        if (player.getActiveEnchantItem() != null) {
            //Util.handleIllegalPlayerAction(player,"Player "+player.getName()+" Tried To Use Enchant Exploit , And Got Banned!", IllegalPlayerAction.PUNISH_KICKBAN);
            player.setActiveEnchantItem(null);
            player.sendPacket(new EnchantResult(0, true));
            player.sendActionFailed();
            return;
        }

        // Alt game - Karma punishment
        if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE
                && player.getKarma() > 0) {
            sendPacket(Static.NO_KARRMA_TELE);
            return;
        }
        ///////////
        ItemContainer warehouse = player.getActiveWarehouse();
        if (warehouse == null
                && player.getFreightTarget() == 0) {
            return;
        }

        if (player.getFreightTarget() != 0) {
            if (!Config.ALLOW_FREIGHT) {
                return;
            }

            int freaighTargetId = player.getFreightTarget();
            if (player.getObjectId() == freaighTargetId
                    || !player.getAccountChars().containsKey(freaighTargetId)) {
                return;
            }

            if (L2World.getInstance().getPlayer(freaighTargetId) != null) {
                return;
            }

            L2PcInstance target = L2PcInstance.load(freaighTargetId);
            if (target == null) {
                player.cancelActiveWarehouse();
                return;
            }

            PcFreight freight = target.getFreight();
            if (freight == null) {
                player.cancelActiveWarehouse();
                return;
            }

            L2FolkInstance manager = findFolkTarget(player.getTarget());
            if (manager == null) {
                player.cancelActiveWarehouse();
                return;
            }

            player.setActiveWarehouse(freight, manager.getNpcId());
            warehouse = player.getActiveWarehouse();
            target.deleteMe();
        }

        if (warehouse == null) {
            return;
        }

        if (warehouse.isFreight() && player.getFreightTarget() == 0
                || !warehouse.isFreight() && player.getFreightTarget() != 0) {
            player.cancelActiveWarehouse();
            return;
        }

        //System.out.println("####" + warehouse.isFreight());
        L2FolkInstance manager = findFolkTarget(player.getTarget());
        if (manager == null
                || manager.getNpcId() != warehouse.getWhId()
                || !player.isInsideRadius(manager, L2NpcInstance.INTERACTION_DISTANCE, false, false)) {
            sendPacket(player.getFreightTarget() == 0 ? Static.WAREHOUSE_TOO_FAR : Static.PACKAGE_SEND_ERROR_TOO_FAR);
            return;
        }

        int whType = 0;
        int slotsleft = 0;
        int adenaDeposit = 0;
        // Список предметов, уже находящихся на складе
        _itemsOnWarehouse = new FastTable<>();
        switch (warehouse.getWarehouseType()) {
            case 1: // private
                _itemsOnWarehouse.addAll(warehouse.listItems(1));
                whType = 1;
                slotsleft = player.getWareHouseLimit() - _itemsOnWarehouse.size();
                break;
            case 2: // clan
                _itemsOnWarehouse.addAll(warehouse.listItems(2));
                whType = 2;
                slotsleft = 211 - _itemsOnWarehouse.size();
                break;
            case 4: // freight
                _itemsOnWarehouse.addAll(warehouse.listItems(4));
                whType = 4;
                slotsleft = player.getWareHouseLimit() - _itemsOnWarehouse.size();
                break;
        }

        if (whType == 0) {
            return;
        }

        // Список стекуемых предметов, уже находящихся на складе
        FastTable<Integer> stackableList = new FastTable<>();
        for (int i = 0, n = _itemsOnWarehouse.size(); i < n; i++) {
            L2ItemInstance itm = _itemsOnWarehouse.get(i);
            if (itm.isStackable()) {
                stackableList.add(itm.getItemId());
            }
        }

        // Создаем новый список передаваемых предметов, на основе полученных данных
        PcInventory inventory = player.getInventory();
        FastTable<L2ItemInstance> itemsToStoreList = new FastTable<>();
        for (Integer itemObjectId : _items.keySet()) {
            L2ItemInstance item = inventory.getItemByObjectId(itemObjectId);
            if (item == null || item.isEquipped()) {
                continue;
            }
            if (warehouse instanceof ClanWarehouse && !item.isDropable()) // а его вообще положить можно?
            {
                continue;
            }
            if (!item.isStackable() || !stackableList.contains(item.getItemId())) // вещь требует слота
            {
                if (slotsleft == 0) // если слоты кончились нестекуемые вещи и отсутствующие стекуемые пропускаем
                {
                    continue;
                }
                slotsleft--; // если слот есть то его уже нет
            }
            if (item.getItemId() == 57) {
                adenaDeposit = _items.get(itemObjectId);
            }
            itemsToStoreList.add(item);
        }
        stackableList.clear();
        stackableList = null;

        // Проверяем, хватит ли у нас денег на уплату налога
        int fee = itemsToStoreList.size() * 30;
        if (fee + adenaDeposit > player.getAdena()) {
            sendPacket(Static.YOU_NOT_ENOUGH_ADENA);
            return;
        }

        // Сообщаем о том, что слоты кончились
        if (slotsleft == 0) {
            sendPacket(Static.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
            return;
        }
        // Перекидываем
        String date = "";
        TextBuilder tb = null;
        if (Config.LOG_ITEMS) {
            date = Log.getTime();
            tb = new TextBuilder();
        }
        for (int i = 0, n = itemsToStoreList.size(); i < n; i++) {
            L2ItemInstance itemToStore = itemsToStoreList.get(i);
            L2ItemInstance itemDropped = inventory.dropItem("depositwh", itemToStore.getObjectId(), _items.get(itemToStore.getObjectId()), player, player.getLastFolkNPC(), true);
            warehouse.addItem(itemDropped, whType);
            if (Config.LOG_ITEMS && itemDropped != null) {
                String act = "DEPOSIT " + (player.getFreightTarget() != 0 ? "FREIGHT" : "") + "" + itemDropped.getItemName() + "(" + itemDropped.getCount() + ")(+" + itemDropped.getEnchantLevel() + ")(" + itemDropped.getObjectId() + ")(npc:" + manager.getTemplate().npcId + ") #(player " + player.getName() + ", account: " + player.getAccountName() + ", ip: " + player.getIP() + ", hwid: " + player.getHWID() + ")";
                tb.append(date + act + "\n");
            }
        }
        if (Config.LOG_ITEMS && tb != null) {
            Log.item(tb.toString(), Log.WAREHOUSE);
            tb.clear();
            tb = null;
        }
        player.sendItems(true);

        // Update current load status on player
        player.sendChanges();
        _items.clear();
        _items = null;
        itemsToStoreList.clear();
        itemsToStoreList = null;

        if (player.getFreightTarget() != 0) {
            player.setFreightTarget(0);
            sendPacket(Static.PACKAGE_SEND_DONE);
        }

        /*// Freight price from config or normal price per item slot (30)
         int fee = _count * 30;
         int currentAdena = player.getAdena();
         int slots = 0;
        
         for (int i = 0; i < _count; i++)
         {
         int objectId = _items[i * 2 + 0];
         int count = _items[i * 2 + 1];
        
         // Check validity of requested item
         L2ItemInstance item = player.checkItemManipulation(objectId, count, "deposit");
         if (item == null)
         {
         _log.warning("Error depositing a warehouse object for char "+player.getName()+" (validity check)");
         _items[i * 2 + 0] = 0;
         _items[i * 2 + 1] = 0;
         continue;
         }
        
         if ((warehouse instanceof ClanWarehouse) && !item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST) return;
         // Calculate needed adena and slots
         if (item.getItemId() == 57) currentAdena -= count;
         if (!item.isStackable()) slots += count;
         else if (warehouse.getItemByItemId(item.getItemId()) == null) slots++;
         }
        
         // Item Max Limit Check
         if (!warehouse.validateCapacity(slots))
         {
         sendPacket(SystemMessage.id(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
         return;
         }
        
         // Check if enough adena and charge the fee
         if (currentAdena < fee || !player.reduceAdena("Warehouse", fee, player.getLastFolkNPC(), false))
         {
         sendPacket(Static.YOU_NOT_ENOUGH_ADENA);
         return;
         }
        
         // Proceed to the transfer
         //InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
         for (int i = 0; i < _count; i++)
         {
         int objectId = _items[i * 2 + 0];
         int count = _items[i * 2 + 1];
        
         // check for an invalid item
         if (objectId == 0 && count == 0) continue;
        
         L2ItemInstance oldItem = player.getInventory().getItemByObjectId(objectId);
         if (oldItem == null)
         {
         _log.warning("Error depositing a warehouse object for char "+player.getName()+" (olditem == null)");
         continue;
         }
        
         int itemId = oldItem.getItemId();
        
         if ((itemId >= 6611 && itemId <= 6621) || itemId == 6842)
         continue;
        
         L2ItemInstance newItem = player.getInventory().transferItem("Warehouse", objectId, count, warehouse, player, player.getLastFolkNPC());
         if (newItem == null)
         {
         _log.warning("Error depositing a warehouse object for char "+player.getName()+" (newitem == null)");
         continue;
         }
        
         //if (playerIU != null)
         //{
         // 	if (oldItem.getCount() > 0 && oldItem != newItem) playerIU.addModifiedItem(oldItem);
         // 	else playerIU.addRemovedItem(oldItem);
         // }
         }
        
         // Send updated item list to the player
         //if (playerIU != null) player.sendPacket(playerIU);
         //else 
         player.sendPacket(new ItemList(player, true));
        
         // Update current load status on player
         player.sendChanges();*/
    }

    private L2FolkInstance findFolkTarget(L2Object target) {
        if (target == null
                || !target.isL2Folk()) {
            return null;
        }

        return (L2FolkInstance) target;
    }
}
