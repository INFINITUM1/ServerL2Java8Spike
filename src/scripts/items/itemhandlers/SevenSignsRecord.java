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
import net.sf.l2j.gameserver.network.serverpackets.SSQStatus;

/**
 * Item Handler for Seven Signs Record
 *
 * @author Tempy
 */
public class SevenSignsRecord implements IItemHandler {
private static final int[] ITEM_IDS = {5707};

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
	{
		L2PcInstance activeChar;

		if (playable.isPlayer())
			activeChar = (L2PcInstance)playable;
		else if (playable.isPet())
			activeChar = ((L2PetInstance)playable).getOwner();
		else
			return;

		activeChar.sendPacket(new SSQStatus(activeChar, 1));
	}

	public int[] getItemIds()
	{
       return ITEM_IDS;
	}
}