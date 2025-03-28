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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2EtcItemType;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/02 21:25:21 $
 */
public final class RequestDropItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestDropItem.class.getName());

	private int _objectId;
	private int _count;
	private int _x;
	private int _y;
	private int _z;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count    = readD();
		_x        = readD();
		_y        = readD();
		_z        = readD();
	}

	@Override
	protected void runImpl()
	{
        L2PcInstance player = getClient().getActiveChar();
    	if (player == null) 
			return;
        player.sendPacket(Static.CANNOT_DISCARD_THIS_ITEM);
		return;
        /*L2PcInstance player = getClient().getActiveChar();
    	if (player == null) return;
		
		if(System.currentTimeMillis() - player.getCPB() < 400)
			return;

		player.setCPB();
		
        L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);

        if (item == null
        		|| _count == 0
        		|| !player.validateItemManipulation(_objectId, "drop")
        		|| (!Config.ALLOW_DISCARDITEM && !player.isGM())
        		|| !item.isDropable())
        {
        	player.sendPacket(Static.CANNOT_DISCARD_THIS_ITEM);
        	return;
        }
        if(item.getItemType() == L2EtcItemType.QUEST || player.isParalyzed())
        {
        	return;
        }
        int itemId = item.getItemId();

        // Cursed Weapons cannot be dropped
        if (CursedWeaponsManager.getInstance().isCursed(itemId))
        	return;

        if(_count > item.getCount())
        {
			player.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
        }

        if (Config.PLAYER_SPAWN_PROTECTION > 0 && player.isInvul() && !player.isGM())
        {
        	player.sendPacket(Static.CANNOT_DISCARD_THIS_ITEM);
        	return;
        }
        
        	
        if(_count <= 0)
        {
        	//Util.handleIllegalPlayerAction(player,"[RequestDropItem] count <= 0! ban! oid: "+_objectId+" owner: "+player.getName(),IllegalPlayerAction.PUNISH_KICK);
        	return;
        }

        if(!item.isStackable() && _count > 1)
        {
        	//Util.handleIllegalPlayerAction(player,"[RequestDropItem] count > 1 but item is not stackable! ban! oid: "+_objectId+" owner: "+player.getName(),IllegalPlayerAction.PUNISH_KICK);
        	return;
        }

		if (player.isTransactionInProgress() || player.getPrivateStoreType() != 0)
        {
            player.sendPacket(Static.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
            return;
        }
		if (player.isFishing())
        {
			//You can't mount, dismount, break and drop items while fishing
            player.sendPacket(Static.CANNOT_DO_WHILE_FISHING_2);
            return;
        }

		// Cannot discard item that the skill is consumming
		if (player.isCastingNow())
		{
			if (player.getCurrentSkill() != null && player.getCurrentSkill().getSkill().getItemConsumeId() == item.getItemId())
			{
	            player.sendPacket(Static.CANNOT_DISCARD_THIS_ITEM);
	            return;
			}
		}

		if (L2Item.TYPE2_QUEST == item.getItem().getType2() && !player.isGM())
		{
			//if (Config.DEBUG) _log.finest(player.getObjectId()+":player tried to drop quest item");
            player.sendPacket(Static.CANNOT_DISCARD_EXCHANGE_ITEM);
            return;
		}

		if (!player.isInsideRadius(_x, _y, 150, false) || Math.abs(_z - player.getZ()) > 50)
		{
			//if (Config.DEBUG) _log.finest(player.getObjectId()+": trying to drop too far away");
            player.sendPacket(Static.CANNOT_DISCARD_DISTANCE_TOO_FAR);
		    return;
		}
		
		if (player.getActiveEnchantItem() != null)
		{
			//Util.handleIllegalPlayerAction(player,"Player "+player.getName()+" Tried To Use Enchant Exploit , And Got Banned!", IllegalPlayerAction.PUNISH_KICKBAN);
			player.setActiveEnchantItem(null); 
			player.sendPacket(new EnchantResult(0, true));
			player.sendActionFailed();
			return;
		} 
		
		if (player.getActiveTradeList() != null) 
		{
			player.cancelActiveTrade(); 
			player.sendActionFailed();
			return;
		} 

		//if (Config.DEBUG) _log.fine("requested drop item " + _objectId + "("+ item.getCount()+") at "+_x+"/"+_y+"/"+_z);

		if (item.isEquipped())
		{
			L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (int i = 0; i < unequiped.length; i++)
			{
				iu.addModifiedItem(unequiped[i]);
			}
			player.sendPacket(iu);
			player.broadcastUserInfo();

			player.sendItems(true);
		}

		L2ItemInstance dropedItem = player.dropItem("Drop", _objectId, _count, _x, _y, _z, null, false);
		
		if(dropedItem == null)
			return;
			
		//if (Config.DEBUG) _log.fine("dropping " + _objectId + " item("+_count+") at: " + _x + " " + _y + " " + _z);

		// player.broadcastUserInfo();
		player.sendItems(true);
		player.sendChanges();
		//player.sendPacket(SystemMessage.id(SystemMessage.YOU_HAVE_DROPPED_S1).addItemName(dropedItem.getItemId()));
		player.broadcastUserInfo();
		//player.sendPacket(new UserInfo(player));
		
		player.refreshExpertisePenalty();
		player.refreshOverloaded();*/
		/*if (player.isGM())
		{
			String target = (player.getTarget() != null?player.getTarget().getName():"no-target");
			GMAudit.auditGMAction(player.getName(), "drop", target, dropedItem.getItemId() + " - " +dropedItem.getName());
		}

        if (dropedItem != null && dropedItem.getItemId() == 57 && dropedItem.getCount() >= 1000000)
        {
            String msg = "Character ("+player.getName()+") has dropped ("+dropedItem.getCount()+")adena at ("+_x+","+_y+","+_z+")";
            _log.warning(msg);
            GmListTable.broadcastMessageToGMs(msg);
        }*/
	}
}
