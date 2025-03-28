/* This program is free software; you can redistribute it and/or modify
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
import scripts.items.IItemHandler;
import scripts.items.ItemHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;

public final class RequestPetUseItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPetUseItem.class.getName());

	private int _objectId;


	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if (player == null)
		    return;

		if(System.currentTimeMillis() - player.gCPX() < 200)
			return;

		player.sCPX();

		L2PetInstance pet = (L2PetInstance)player.getPet();

		if (pet == null)
			return;

		L2ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);

        if (item == null)
            return;

        if (item.isWear())
            return;

		int itemId = item.getItemId();

		if (player.isAlikeDead() || pet.isDead())
        {
			player.sendPacket(SystemMessage.id(SystemMessageId.S1_CANNOT_BE_USED).addItemName(item.getItemId()));
			return;
		}

		///if (Config.DEBUG)
        //    _log.finest(player.getObjectId()+": pet use item " + _objectId);

		//check if the item matches the pet
		if (item.isEquipable())
		{
			if (L2PetDataTable.isWolf(pet.getNpcId()) && // wolf
                    item.getItem().isForWolf())
			{
				useItem(pet, item, player);
				return;
			}
			else if (L2PetDataTable.isHatchling(pet.getNpcId()) && // hatchlings
                        item.getItem().isForHatchling())
			{
				useItem(pet, item, player);
				return;
			}
            else if (L2PetDataTable.isStrider(pet.getNpcId()) && // striders
                    item.getItem().isForStrider())
            {
                useItem(pet, item, player);
                return;
            }
            else if (L2PetDataTable.isBaby(pet.getNpcId()) && // baby pets (buffalo, cougar, kookaboora)
                    item.getItem().isForBabyPet())
            {
                useItem(pet, item, player);
                return;
            }
			else
			{
				player.sendPacket(Static.ITEM_NOT_FOR_PETS);
                return;
			}
		}
		else if (L2PetDataTable.isPetFood(itemId))
		{
			if (L2PetDataTable.isWolf(pet.getNpcId()) && L2PetDataTable.isWolfFood(itemId))
			{
				feed(player, pet, item);
				return;
			}
			if (L2PetDataTable.isSinEater(pet.getNpcId()) && L2PetDataTable.isSinEaterFood(itemId))
			{
				feed(player, pet, item);
				return;
			}
			else if (L2PetDataTable.isHatchling(pet.getNpcId()) && L2PetDataTable.isHatchlingFood(itemId))
			{
				feed(player, pet, item);
				return;
			}
			else if (L2PetDataTable.isStrider(pet.getNpcId()) && L2PetDataTable.isStriderFood(itemId))
			{
				feed(player, pet, item);
				return;
			}
			else if (L2PetDataTable.isWyvern(pet.getNpcId()) && L2PetDataTable.isWyvernFood(itemId))
			{
				feed(player, pet, item);
				return;
			}
			else if (L2PetDataTable.isBaby(pet.getNpcId()) && L2PetDataTable.isBabyFood(itemId))
			{
				feed(player, pet, item);
			}
		}

	    IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());
	    if (handler != null)
		{
			useItem(pet, item, player);
		}
		else
			player.sendPacket(Static.ITEM_NOT_FOR_PETS);

		return;
	}

	private synchronized void useItem(L2PetInstance pet, L2ItemInstance item, L2PcInstance player)
	{
		if (item.isEquipable())
		{
			if (item.isEquipped())
			{
				pet.getInventory().unEquipItemInSlot(item.getLocationSlot());
				switch (item.getItem().getBodyPart())
				{
					case L2Item.SLOT_R_HAND:
						pet.setWeapon(0);
						break;
					case L2Item.SLOT_CHEST:
						pet.setArmor(0);
						break;
					case L2Item.SLOT_NECK:
						pet.setJewel(0);
						break;
				}
			}
			else
			{
				pet.getInventory().equipItem(item);
				switch (item.getItem().getBodyPart())
				{
					case L2Item.SLOT_R_HAND:
						pet.setWeapon(item.getItemId());
						break;
					case L2Item.SLOT_CHEST:
						pet.setArmor(item.getItemId());
						break;
					case L2Item.SLOT_NECK:
						pet.setJewel(item.getItemId());
						break;
				}
				
			}

			PetItemList pil = new PetItemList(pet);
			player.sendPacket(pil);

			pet.updateAndBroadcastStatus(1);
		}
		else
		{
			//_log.finest("item not equipable id:"+ item.getItemId());
		    IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());

		    if (handler == null)
		        _log.warning("no itemhandler registered for itemId:" + item.getItemId());
		    else
		    {
		        handler.useItem(pet, item, false);
		        pet.updateAndBroadcastStatus(1);
		    }
		}
	}

	/**
	 * When fed by owner double click on food from pet inventory. <BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : 1 food = 100 points of currentFed</B></FONT><BR><BR>
	 */
	private void feed(L2PcInstance player, L2PetInstance pet, L2ItemInstance item)
	{
		// if pet has food in inventory
		if (pet.destroyItem("Feed", item.getObjectId(), 1, pet, false))
            pet.setCurrentFed(pet.getCurrentFed() + 100);
			
		pet.broadcastPacket(new MagicSkillUser(pet, pet, 2101, 1, 1, 0));
		pet.broadcastStatusUpdate();
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] PetUseItem";
	}
}
