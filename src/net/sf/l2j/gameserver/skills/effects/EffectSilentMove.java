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
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;

final class EffectSilentMove extends L2Effect
{
	public EffectSilentMove(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	/** Notify started */
	@Override
	public void onStart()
	{
		super.onStart();
		if (getEffected().isPlayer())
		{
			if (getSkill().getId() == 296)
			{
				getEffected().getPlayer().setRelax(true);
				getEffected().getPlayer().sitDown();
			}
			
			getEffected().getPlayer().setSilentMoving(true);
		}
	}

	/** Notify exited */
	@Override
	public void onExit()
	{
		super.onExit();
		if (getEffected().isPlayer())
		{
			if (getSkill().getId() == 296)
			{
				getEffected().getPlayer().setRelax(false);
				getEffected().getPlayer().sitDown();
			}
			
			getEffected().getPlayer().setSilentMoving(false);
		}
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.SILENT_MOVE;
	}

	@Override
	public boolean onActionTime()
	{
		 // Only cont skills shouldn't end
		if(getSkill().getSkillType() != SkillType.CONT)
			return false;

		if(getEffected().isDead())
			return false;

		double manaDam = calc();

		if(manaDam > getEffected().getCurrentMp())
		{
			getEffected().sendPacket(Static.SKILL_REMOVED_DUE_LACK_MP);
			return false;
		}
		
		if (getSkill().getId() == 296 && !getEffected().isSitting())
			return false;

		getEffected().reduceCurrentMp(manaDam);
		return true;
	}
}
