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
package scripts.skills.skillhandlers;

import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.util.Rnd;
import javolution.util.FastList;
import javolution.util.FastList.Node;

/*
 * Mobs can teleport players to them
 */

public class GetPlayer implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS = {SkillType.GET_PLAYER};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
    {
        if (activeChar.isAlikeDead()) 
			return;
		for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Object target = n.getValue();
        	if (target.isPlayer())
        	{
        		L2PcInstance trg = (L2PcInstance)target;
        		if (trg.isAlikeDead()) 
					continue;
        		trg.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ(), true);
        		//trg.setXYZ(activeChar.getX()+Rnd.get(-10,10), activeChar.getY()+Rnd.get(-10,10), activeChar.getZ());
        		//trg.sendPacket(new ValidateLocation(trg));
        	}
        }
		//targets.clear();
    }

    public SkillType[] getSkillIds()
    {
        return SKILL_IDS;
    }
}
