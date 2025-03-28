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

import net.sf.l2j.gameserver.datatables.SkillTable;
import scripts.items.IItemHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class ChestKey implements IItemHandler
{
	public static final int INTERACTION_DISTANCE = 100;

	private static final int[] ITEM_IDS = {
											6665, 6666, 6667, 6668, 6669, 6670, 6671, 6672  //deluxe key
										  };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
	{
		if (!(playable.isPlayer())) return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2Skill skill = SkillTable.getInstance().getInfo(2229, item.getItemId()-6664);//box key skill
		L2Object target = activeChar.getTarget();

		if (target == null || !(target instanceof L2ChestInstance))
		{
			activeChar.sendPacket(Static.INCORRECT_TARGET);
			activeChar.sendActionFailed();
		}
		else
		{
			L2ChestInstance chest = (L2ChestInstance) target;
			if (chest.isDead() || chest.isInteracted())
			{
				activeChar.sendMessage("The chest Is empty.");
				activeChar.sendActionFailed();
				return;
			}
			activeChar.useMagic(skill,false,false);
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}
