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
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.4 $ $Date: 2005/04/06 16:13:51 $
 */

public class Remedy implements IItemHandler
{
	private static int[] ITEM_IDS = { 1831, 1832, 1833, 1834, 3889 };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
	{
		L2PcInstance activeChar;
		if (playable.isPlayer())
			activeChar = (L2PcInstance)playable;
		else if (playable.isPet())
			activeChar = ((L2PetInstance)playable).getOwner();
		else
			return;

		if (activeChar.isInOlympiadMode())
        {
            activeChar.sendPacket(Static.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
            return;
        }
		
		int remId = 0;
		L2Effect e = null;
	    switch (item.getItemId())
		{
			case 1831: // antidote
				e = activeChar.getFirstEffect(L2Skill.SkillType.POISON);
				if (e != null && e.getLevel() <= 3)
					e.exit();
				remId = 2042;
				break;
			case 1832: // advanced antidote
				e = activeChar.getFirstEffect(L2Skill.SkillType.POISON);
				if (e != null && e.getLevel() <= 7)
					e.exit();
				remId = 2043;
				break;
			case 1833: // bandage
				e = activeChar.getFirstEffect(L2Skill.SkillType.BLEED);
				if (e != null && e.getLevel() <= 3)
					e.exit();
				remId = 34;
				break;
			case 1834: // emergency dressing
				e = activeChar.getFirstEffect(L2Skill.SkillType.BLEED);
				if (e != null && e.getLevel() <= 7)
					e.exit();
				remId = 2045;
				break;
			case 3889: // potion of recovery
				e = activeChar.getFirstEffect(4082);
				if (e != null)
					e.exit();
				activeChar.setIsImobilised(false);
				if (activeChar.getFirstEffect(L2Effect.EffectType.ROOT) == null) 
					activeChar.stopRooting(null);
				remId = 2042;
				break;
		}
		
        activeChar.broadcastPacket(new MagicSkillUser(playable, playable, remId, 1, 0, 0));
		playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
	}
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
