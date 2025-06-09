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
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.effects.EffectCharge;
import net.sf.l2j.gameserver.templates.StatsSet;
import javolution.util.FastList;
import javolution.util.FastList.Node;

public class L2SkillChargeEffect extends L2Skill
{
	final int chargeSkillId;

	public L2SkillChargeEffect(StatsSet set)
	{
		super(set);
		chargeSkillId = set.getInteger("charge_skill_id");
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
	{
		if (activeChar.isPlayer())
		{
			if (activeChar.getCharges() < getNumCharges())
			{
				activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(getId()));
				return false;
			}
		}
		return super.checkCondition(activeChar, target, itemOrWeapon);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Object> targets)
	{
		if (activeChar.isAlikeDead()) 
			return;

		// get the effect
		if (activeChar.getCharges() < getNumCharges())
		{
			activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(getId()));
			return;
		}

		// decrease?
		activeChar.decreaseCharges(getNumCharges());

		// apply effects
		if (hasEffects())
		{
			for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
			{
				L2Character target = (L2Character) n.getValue();
				if (target == null)
					continue;

				getEffects(activeChar, target);
			}
		}

		if (getId() == 461)
		{
			activeChar.stopEffects(L2Effect.EffectType.ROOT);
			activeChar.stopSlowEffects();
		}
		activeChar.sendEtcStatusUpdate();
	}
}
