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

import java.nio.BufferUnderflowException;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.PetNameTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.4 $ $Date: 2005/04/06 16:13:48 $
 */
public final class RequestChangePetName extends L2GameClientPacket
{
	private String _name;

	@Override
	protected void readImpl()
	{
		try
		{
			_name = readS();
		}
		catch (BufferUnderflowException e)
		{
			_name = "no";
		}
	}

	@Override
	protected void runImpl()
	{
		if (_name.equalsIgnoreCase("no"))
			return;
		
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		final L2Summon pet = player.getPet();
		if (pet == null)
			return;

		if (pet.getName() != null)
		{
			player.sendPacket(Static.NAMING_YOU_CANNOT_SET_NAME_OF_THE_PET);
			return;
		}
		else if (PetNameTable.getInstance().doesPetNameExist(_name, pet.getTemplate().npcId))
		{
			player.sendPacket(Static.NAMING_ALREADY_IN_USE_BY_ANOTHER_PET);
			return;
		}
        else if ((_name.length() < 3) || (_name.length() > 16))
		{
			// SystemMessage sm = SystemMessage.id(SystemMessage.NAMING_PETNAME_UP_TO_8CHARS);
        	player.sendMessage("Не более 16 символов");
			return;
		}
        else if (!PetNameTable.getInstance().isValidPetName(_name))
		{
        	player.sendPacket(Static.NAMING_PETNAME_CONTAINS_INVALID_CHARS);
			return;
		}

		pet.setName(_name);
		pet.broadcastPacket(new NpcInfo(pet, player,1));
		pet.updateAndBroadcastStatus(1);
		// The PetInfo packet wipes the PartySpelled (list of active spells' icons).  Re-add them
		pet.updateEffectIcons(true);

		// set the flag on the control item to say that the pet has a name
		if (pet.isPet())
		{
			L2ItemInstance controlItem = pet.getOwner().getInventory().getItemByObjectId(pet.getControlItemId());
			if (controlItem != null)
			{
				controlItem.setCustomType2(1);
				controlItem.updateDatabase();
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(controlItem);
				player.sendPacket(iu);
			}
		}
	}
}
