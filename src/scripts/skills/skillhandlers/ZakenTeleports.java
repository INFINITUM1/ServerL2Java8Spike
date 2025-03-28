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
package scripts.skills.skillhandlers;

import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import scripts.ai.Zaken;

import javolution.util.FastList;

public class ZakenTeleports implements ISkillHandler
{
    private static final SkillType[] SKILL_IDS = {SkillType.ZAKENTPSELF, SkillType.ZAKENTPPLAYER};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
    {
		Location loc = null;
        SkillType type = skill.getSkillType();
        switch (type)
        {
			case ZAKENTPSELF:
				loc = getRandomLoc();
				activeChar.teleToLocation(loc.x + Rnd.get(300), loc.y + Rnd.get(300), loc.z, false);
				((Zaken) activeChar).setTeleported(true);
				break;	
			case ZAKENTPPLAYER:
				for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
				{
					L2Object obj = n.getValue();
					// Get a target
					if (obj == null || !(obj.isPlayer()))
						continue;
						
					L2PcInstance player = (L2PcInstance) obj;
					if (player.isDead())
						continue;
						
					loc = getRandomLoc();
					player.teleToLocation(loc.x + Rnd.get(300), loc.y + Rnd.get(300), loc.z, false);
				}
				break;
		}		
    }
	
	private Location getRandomLoc()
	{
		return CustomServerData.getInstance().getZakenPoint();
	}

    public SkillType[] getSkillIds()
    {
        return SKILL_IDS;
    }
}
