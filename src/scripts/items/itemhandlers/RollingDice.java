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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.Dice;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Rnd;


/**
 * This class ...
 *
 * @version $Revision: 1.1.4.2 $ $Date: 2005/03/27 15:30:07 $
 */

public class RollingDice implements IItemHandler
{
	private static final int[] ITEM_IDS = { 4625, 4626, 4627, 4628 };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
	{
		if (!(playable.isPlayer()))
			return;

		L2PcInstance activeChar = (L2PcInstance)playable;
	    int itemId = item.getItemId();

	    if (activeChar.isInOlympiadMode())
        {
            activeChar.sendPacket(Static.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
            return;
        }

		if (itemId == 4625 || itemId == 4626 || itemId == 4627 || itemId == 4628)
		{
			int number = rollDice(activeChar);
			if (number == 0)
			{
				activeChar.sendPacket(Static.YOU_MAY_NOT_THROW_THE_DICE_AT_THIS_TIME_TRY_AGAIN_LATER);
				return;
			}

			Broadcast.toSelfAndKnownPlayers(activeChar, new Dice (activeChar.getObjectId(),item.getItemId(),number,activeChar.getX()-30,activeChar.getY()-30,activeChar.getZ()));
			SystemMessage sm = SystemMessage.id(SystemMessageId.S1_ROLLED_S2).addString(activeChar.getName()).addNumber(number);
			activeChar.sendPacket(sm);
            if (activeChar.isInsideZone(L2Character.ZONE_PEACE))
			    Broadcast.toKnownPlayers(activeChar, sm);
			else if (activeChar.isInParty())
			    activeChar.getParty().broadcastToPartyMembers(activeChar,sm);
		}
	}

	private int rollDice(L2PcInstance player)
	{
		return Rnd.get(1, 6);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
