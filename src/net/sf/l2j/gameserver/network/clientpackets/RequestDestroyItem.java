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
package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.4.2.6 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestDestroyItem extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(RequestDestroyItem.class.getName());
    private int _objectId;
    private int _count;

    @Override
    protected void readImpl() {
        _objectId = readD();
        _count = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPK() < 400) {
            return;
        }

        if (player.isAlikeDead() || player.isAllSkillsDisabled() || player.isOutOfControl() || player.isParalyzed()) {
            return;
        }

        player.sCPK();

        if (_count <= 0) {
            //if (_count < 0) 
            //Util.handleIllegalPlayerAction(player,"[RequestDestroyItem] count < 0! ban! oid: "+_objectId+" owner: "+player.getName(),Config.DEFAULT_PUNISH);
            return;
        }

        int count = _count;

        if (player.getPrivateStoreType() != 0) {
            player.sendPacket(Static.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
            return;
        }

        L2ItemInstance itemToRemove = player.getInventory().getItemByObjectId(_objectId);
        // if we cant find requested item, its actualy a cheat!
        if (itemToRemove == null) {
            return;
        }

        // Cannot discard item that the skill is consumming
        if (player.isCastingNow()) {
            if (player.getCurrentSkill() != null && player.getCurrentSkill().getSkill().getItemConsumeId() == itemToRemove.getItemId()) {
                player.sendPacket(Static.CANNOT_DISCARD_THIS_ITEM);
                return;
            }
        }

        int itemId = itemToRemove.getItemId();
        if (itemToRemove == null || itemToRemove.isWear() || !itemToRemove.isDestroyable() || CursedWeaponsManager.getInstance().isCursed(itemId)) {
            player.sendPacket(Static.CANNOT_DISCARD_THIS_ITEM);
            return;
        }

        if (!itemToRemove.isStackable() && count > 1) {
            //Util.handleIllegalPlayerAction(player,"[RequestDestroyItem] count > 1 but item is not stackable! oid: "+_objectId+" owner: "+player.getName(),Config.DEFAULT_PUNISH);
            return;
        }

        if (_count > itemToRemove.getCount()) {
            count = itemToRemove.getCount();
        }


        if (itemToRemove.isEquipped()) {
            L2ItemInstance[] unequiped =
                    player.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getEquipSlot());
            InventoryUpdate iu = new InventoryUpdate();
            for (int i = 0; i < unequiped.length; i++) {
                iu.addModifiedItem(unequiped[i]);
            }
            player.sendPacket(iu);
            //player.broadcastUserInfo();
        }

        if (L2PetDataTable.isPetItem(itemId)) {
            Connect con = null;
            PreparedStatement statement = null;
            try {
                if (player.getPet() != null && player.getPet().getControlItemId() == _objectId) {
                    player.getPet().unSummon(player);
                }

                // if it's a pet control item, delete the pet
                con = L2DatabaseFactory.get();
                statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
                statement.setInt(1, _objectId);
                statement.execute();
            } catch (Exception e) {
                _log.log(Level.WARNING, "could not delete pet objectid: ", e);
            } finally {
                Close.CS(con, statement);
            }
        }

        if (itemToRemove.isAugmented()) {
            itemToRemove.removeAugmentation();
        }

        L2ItemInstance removedItem = player.getInventory().destroyItem("Destroy", _objectId, count, player, null);

        if (removedItem == null) {
            return;
        }

        if (!Config.FORCE_INVENTORY_UPDATE) {
            InventoryUpdate iu = new InventoryUpdate();
            if (removedItem.getCount() == 0) {
                iu.addRemovedItem(removedItem);
            } else {
                iu.addModifiedItem(removedItem);
            }

            //client.get().sendPacket(iu);
            player.sendPacket(iu);
        } else {
            player.sendItems(true);
        }

        player.sendChanges();
        player.broadcastUserInfo();

        //StatusUpdate su = new StatusUpdate(player.getObjectId());
        //su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
        //player.sendPacket(su);

        //L2World.getInstance().removeObject(removedItem);
    }
}
