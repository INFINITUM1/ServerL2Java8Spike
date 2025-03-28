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

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExConfirmVariationItem;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;

/**
 * Format:(ch) d
 * @author  -Wooden-
 */
public final class RequestConfirmTargetItem extends L2GameClientPacket
{
	private int _itemObjId;

	/**
	 * @param buf
	 * @param client
	 */
	@Override
	protected void readImpl()
	{
		_itemObjId = readD();
	}

	/**
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();		
		
		if (player == null)
			return;
		
		if(System.currentTimeMillis() - player.gCPBD() < 200)
			return;

		player.sCPBD();
		
		L2ItemInstance item = L2World.getInstance().getItem(_itemObjId);
		if (item == null) 
			return;

		if (player.getLevel() < 46)
		{
			player.sendPacket(Static.WRONG_LVL_46);
			return;
		}

		if (item.isAugmented())
		{
			player.sendPacket(Static.ONCE_AN_ITEM_IS_AUGMENTED_IT_CANNOT_BE_AUGMENTED_AGAIN);
			return;
		}
		
		// check if the item is augmentable
		if (!item.canBeAugmented())
		{
			player.sendPacket(Static.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		// check if the player can augment
		if (player.getPrivateStoreType() != L2PcInstance.PS_NONE)
		{
			player.sendPacket(Static.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION);
			return;
		}
		if (player.isDead())
		{
			player.sendPacket(Static.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD);
			return;
		}
		if (player.isParalyzed())
		{
			player.sendPacket(Static.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED);
			return;
		}
		if (player.isFishing())
		{
			player.sendPacket(Static.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING);
			return;
		}
		if (player.isSitting())
		{
			player.sendPacket(Static.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN);
			return;
		}

		player.sendPacket(new ExConfirmVariationItem(_itemObjId));
		player.sendPacket(Static.SELECT_THE_CATALYST_FOR_AUGMENTATION);
	}
}
