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

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExRegenMax;
import net.sf.l2j.gameserver.skills.Env;


class EffectHealOverTime extends L2Effect
{
	public EffectHealOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.HEAL_OVER_TIME;
	}

	@Override
	public void onStart()
	{
		if(getEffected().isPlayer())
			getEffected().sendPacket(new ExRegenMax(calc(), (int) (getCount() * getPeriod() / 1000), (int) (getPeriod() / 1000)));
			
		/**
		//getEffected().sendPacket(new ExRegenMax(calc(), (int) (getCount() * getPeriod() / 1000), Math.round(getPeriod() / 1000)));
			switch(getSkill().getId().intValue())
			{
				case 2031: // Lesser Healing Potion
					getEffected().sendPacket(new ExRegenMax(ExRegenMax.POTION_HEALING_LESSER));
					break;
				case 2032: // Healing Potion
					getEffected().sendPacket(new ExRegenMax(ExRegenMax.POTION_HEALING_MEDIUM));
					break;
				case 2037: // Greater Healing Potion
					getEffected().sendPacket(new ExRegenMax(ExRegenMax.POTION_HEALING_GREATER));
					break;
			}
		*/
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected().isDead())
			return false;

		if(getEffected().isL2Door())
			return false;

		/*double hp = getEffected().getCurrentHp();
		double maxhp = getEffected().getMaxHp();
		hp += calc();
		if(hp > maxhp)
		{
			hp = maxhp;
		}*/
		getEffected().setCurrentHp(getEffected().getCurrentHp() + calc());
		/*StatusUpdate suhp = new StatusUpdate(getEffected().getObjectId());
		suhp.addAttribute(StatusUpdate.CUR_HP, (int)hp);
		getEffected().sendPacket(suhp);*/
		return true;
	}
}
