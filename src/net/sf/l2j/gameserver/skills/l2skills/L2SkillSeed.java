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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.effects.EffectSeed;
import net.sf.l2j.gameserver.templates.StatsSet;
import javolution.util.FastList;
import javolution.util.FastList.Node;

public class L2SkillSeed extends L2Skill 
{

	public L2SkillSeed(StatsSet set) {
		super(set);
	}

	@Override
	public void useSkill(L2Character caster, FastList<L2Object> targets) 
	{
		if (caster.isAlikeDead())
			return;

		// Update Seeds Effects
		for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = (L2Character) n.getValue();
			
			if (target.isAlikeDead() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
				continue;

            EffectSeed oldEffect = (EffectSeed) target.getFirstEffect(getId());
            if (oldEffect == null)
                getEffects(caster, target);
            else oldEffect.increasePower();

            L2Effect[] effects = target.getAllEffects();
            for (int j = 0; j < effects.length; j++)
                if (effects[j].getEffectType() == L2Effect.EffectType.SEED)
                    effects[j].rescheduleEffect();
/*
			for (int j=0;j<effects.length;j++){
				if (effects[j].getEffectType()==L2Effect.EffectType.SEED){
					EffectSeed e = (EffectSeed)effects[j];
					if (e.getInUse() || e.getSkill().getId()==this.getId()){
						e.rescheduleEffect();
					}
				}
			}
*/
		}
	}
}
