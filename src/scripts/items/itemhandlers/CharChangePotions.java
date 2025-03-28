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
package scripts.items.itemhandlers;

import scripts.items.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

/**
 * Itemhhandler for Character Appearance Change Potions
 *
 * @author Tempy
  */
public class CharChangePotions implements IItemHandler {
	private static final int[] ITEM_IDS = {
											5235, 5236, 5237,							// Face
											5238, 5239, 5240, 5241, 					// Hair Color
											5242, 5243, 5244, 5245, 5246, 5247, 5248 	// Hair Style
										  };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
	{
		L2PcInstance player = null;
		if (playable.isPlayer())
			player = (L2PcInstance)playable;
		else if (playable.isPet())
			player = ((L2PetInstance)playable).getOwner();
		
		if (player == null)
			return;

		if (player.isAllSkillsDisabled())
		{
            player.sendActionFailed();
			return;
		}

		switch (item.getItemId()) 
		{
			case 5235:
				player.getAppearance().setFace(0);
				break;
			case 5236:
				player.getAppearance().setFace(1);
				break;
			case 5237:
				player.getAppearance().setFace(2);
				break;
			case 5238:
				player.getAppearance().setHairColor(0);
				break;
			case 5239:
				player.getAppearance().setHairColor(1);
				break;
			case 5240:
				player.getAppearance().setHairColor(2);
				break;
			case 5241:
				player.getAppearance().setHairColor(3);
				break;
			case 5242:
				player.getAppearance().setHairStyle(0);
				break;
			case 5243:
				player.getAppearance().setHairStyle(1);
				break;
			case 5244:
				player.getAppearance().setHairStyle(2);
				break;
			case 5245:
				player.getAppearance().setHairStyle(3);
				break;
			case 5246:
				player.getAppearance().setHairStyle(4);
				break;
			case 5247:
				player.getAppearance().setHairStyle(5);
				break;
			case 5248:
				player.getAppearance().setHairStyle(6);
				break;
		}

        // Create a summon effect!
        player.broadcastPacket(new MagicSkillUser(playable, player, 2003, 1, 1, 0));

		// Update the changed stat for the character in the DB.
		player.store();

        // Remove the item from inventory.
		player.destroyItem("Consume", item.getObjectId(), 1, null, false);

		// Broadcast the changes to the char and all those nearby.
		player.broadcastPacket(new UserInfo(player));
	}

	public int[] getItemIds()
	{
       return ITEM_IDS;
	}
}