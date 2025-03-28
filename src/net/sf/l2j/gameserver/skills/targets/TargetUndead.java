package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;

public class TargetUndead extends TargetList
{
	public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill)
	{
        if (target != null
				&& !target.isDead()
				&& (target.isL2Npc() || target.isSummon())
				&& target.isUndead())
            targets.add(target);
        else
			activeChar.sendPacket(Static.TARGET_IS_INCORRECT);

		return targets;
	}
}
