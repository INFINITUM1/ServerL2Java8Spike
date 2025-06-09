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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.PcInventory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.3.2.5 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestCrystallizeItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestCrystallizeItem.class.getName());

	private int _objectId;
	private int _count;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if (player == null)
			return;

		if(System.currentTimeMillis() - player.gCPW() < 100)
		{
			player.sendActionFailed();
			return;
		}

		player.sCPW();

		if (_count <= 0 || player.isParalyzed())
		{
			//Util.handleIllegalPlayerAction(player,"[RequestCrystallizeItem] count <= 0! ban! oid: "+ _objectId + " owner: " + player.getName(),IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		if (player.getPrivateStoreType() != 0 || player.isInCrystallize())
		{
			player.sendPacket(Static.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}

		int skillLevel = player.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE);
		if (skillLevel <= 0)
		{
			player.sendPacket(Static.CRYSTALLIZE_LEVEL_TOO_LOW);
			player.sendActionFailed();
			return;
		}

		PcInventory inventory = player.getInventory();
		if (inventory != null)
		{
			L2ItemInstance item = inventory.getItemByObjectId(_objectId);
			if (item == null || item.isWear())
			{
				player.sendActionFailed();
				return;
			}

			int itemId = item.getItemId();
			if ((itemId >= 6611 && itemId <= 6621) || itemId == 6842)
				return;

			if (_count > item.getCount())
			{
				_count = player.getInventory().getItemByObjectId(_objectId)
						.getCount();
			}
		}

		L2ItemInstance itemToRemove = player.getInventory().getItemByObjectId(_objectId);
		if (itemToRemove == null || itemToRemove.isWear())
		{
			return;
		}
		if (!itemToRemove.getItem().isCrystallizable()
				|| (itemToRemove.getItem().getCrystalCount() <= 0)
				|| (itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_NONE))
		{
			_log.warning("" + player.getObjectId() + " tried to crystallize " + itemToRemove.getItem().getItemId());
			return;
		}

		// Check if the char can crystallize C items and return if false;
		if (itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_C && skillLevel <= 1)
		{
			player.sendPacket(Static.CRYSTALLIZE_LEVEL_TOO_LOW);
			player.sendActionFailed();
			return;
		}

		// Check if the user can crystallize B items and return if false;
		if (itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_B && skillLevel <= 2)
		{
			player.sendPacket(Static.CRYSTALLIZE_LEVEL_TOO_LOW);
			player.sendActionFailed();
			return;
		}

		// Check if the user can crystallize A items and return if false;
		if (itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_A && skillLevel <= 3)
		{
			player.sendPacket(Static.CRYSTALLIZE_LEVEL_TOO_LOW);
			player.sendActionFailed();
			return;
		}

		// Check if the user can crystallize S items and return if false;
		if (itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_S && skillLevel <= 4)
		{
			player.sendPacket(Static.CRYSTALLIZE_LEVEL_TOO_LOW);
			player.sendActionFailed();
			return;
		}

		player.setInCrystallize(true);

		// unequip if needed
		if (itemToRemove.isEquipped())
		{
			L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getEquipSlot());
			InventoryUpdate iu = new InventoryUpdate();
			for (int i = 0; i < unequiped.length; i++)
			{
				iu.addModifiedItem(unequiped[i]);
			}
			player.sendPacket(iu);
			// player.updatePDef();
			// player.updatePAtk();
			// player.updateMDef();
			// player.updateMAtk();
			// player.updateAccuracy();
			// player.updateCriticalChance();
		}

		// remove from inventory
		L2ItemInstance removedItem = player.getInventory().destroyItem("Crystalize", _objectId, _count, player, null);

		// add crystals
		int crystalId = itemToRemove.getItem().getCrystalItemId();
		int crystalAmount = itemToRemove.getCrystalCount();
		L2ItemInstance createditem = player.getInventory().addItem("Crystalize", crystalId, crystalAmount, player, itemToRemove);

		player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(crystalId).addNumber(crystalAmount));

		// send inventory update
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate iu = new InventoryUpdate();
			if (removedItem.getCount() == 0)
				iu.addRemovedItem(removedItem);
			else
				iu.addModifiedItem(removedItem);

			if (createditem.getCount() != crystalAmount)
				iu.addModifiedItem(createditem);
			else
				iu.addNewItem(createditem);

			player.sendPacket(iu);
		} 
		else
			player.sendItems(false);

		// status & user info
		//StatusUpdate su = new StatusUpdate(player.getObjectId());
		//su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		//player.sendPacket(su);
		
		player.sendChanges();
		player.broadcastUserInfo();

		L2World world = L2World.getInstance();
		world.removeObject(removedItem);

		player.setInCrystallize(false);
	}
}
