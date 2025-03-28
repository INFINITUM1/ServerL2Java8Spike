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
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import javolution.util.FastList;
/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.1 $ $Date: 2005/03/02 15:38:36 $
 */

public class ManaHeal implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS = {SkillType.MANAHEAL, SkillType.MANARECHARGE, SkillType.MANAHEAL_PERCENT};

	public void useSkill(L2Character caster, L2Skill skill, FastList<L2Object> targets)
	{
		SystemMessage sm;
        L2Character target = null;
		for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			target = (L2Character) n.getValue();
            double mp = target.getCurrentMp() == target.getMaxMp() ? 0 : skill.getPower();
			
			if (mp > 0)
			{
				switch(skill.getSkillType())
				{
					case MANAHEAL_PERCENT:
						mp = target.getMaxMp() * mp / 100.0;
						break;	
					case MANARECHARGE:
						mp = target.calcStat(Stats.RECHARGE_MP_RATE,mp, null, null);
						if (caster.equals(target))
							mp = 0;
						break;	
				}
				target.setLastHealAmount((int)mp);
				target.setCurrentMp(target.getCurrentMp() + mp);
			}
			
            if (caster.isPlayer() && !caster.equals(target))
                sm = SystemMessage.id(SystemMessageId.S2_MP_RESTORED_BY_S1).addString(caster.getName());
            else
                sm = SystemMessage.id(SystemMessageId.S1_MP_RESTORED);
            sm.addNumber((int)mp);
            target.sendPacket(sm);
		}
		sm = null;
		target = null;
	}

	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
