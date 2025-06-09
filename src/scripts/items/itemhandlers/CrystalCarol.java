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
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;

/**
 * This class ...
 *
 * @version $Revision: 1.2.4.4 $ $Date: 2005/03/27 15:30:07 $
 */

public class CrystalCarol implements IItemHandler
{
	private static final int[] ITEM_IDS = { 5562, 5563, 5564, 5565, 5566, 5583, 5584, 5585, 5586, 5587,
									 4411, 4412, 4413, 4414, 4415, 4416, 4417, 5010, 6903, 7061, 7062, 8555};

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
	{
		if (!(playable.isPlayer()))
			return;
			
		int songId = 0;
		L2PcInstance activeChar = (L2PcInstance)playable;
	    switch(item.getItemId())
		{
			case 5562: //crystal_carol_01
				songId = 2140;
				break;
			case 5563: //crystal_carol_02
				songId = 2141;
				break;
			case 5564: //crystal_carol_03
				songId = 2142;
				break;
			case 5565: //crystal_carol_04
				songId = 2143;
				break;
			case 5566: //crystal_carol_05
				songId = 2144;
				break;
			case 5583: //crystal_carol_06
				songId = 2145;
				break;
			case 5584: //crystal_carol_07
				songId = 2146;
				break;
			case 5585: //crystal_carol_08
				songId = 2147;
				break;
			case 5586: //crystal_carol_09
				songId = 2148;
				break;
			case 5587: //crystal_carol_10
				songId = 2149;
				break;
			case 4411: //crystal_journey
				songId = 2069;
				break;
			case 4412: //crystal_battle
				songId = 2068;
				break;
			case 4413: //crystal_love
				songId = 2070;
				break;
			case 4414: //crystal_solitude
				songId = 2072;
				break;
			case 4415: //crystal_festival
				songId = 2071;
				break;
			case 4416: //crystal_celebration
				songId = 2073;
				break;
			case 4417: //crystal_comedy
				songId = 2067;
				break;
			case 5010: //crystal_victory
				songId = 2066;
				break;
			case 6903: //music_box_m
				songId = 2187;
				break;
			case 7061: //crystal_birthday
				songId = 2073;
				break;
			case 7062: //crystal_wedding
				songId = 2230;
				break;
			case 8555: //VVKorea
				songId = 2272;
				break;
		}
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
		activeChar.broadcastPacket(new MagicSkillUser(playable, activeChar, songId, 1, 1, 0));
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
