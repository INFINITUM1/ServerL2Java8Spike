package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2ArtefactInstance;

public class TargetHoly extends TargetList
{
	public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill)
	{
		if (activeChar.getTarget() == null)
			return targets;

		L2Object arfkt = activeChar.getTarget();
        if (arfkt.isL2Artefact())
			targets.add((L2ArtefactInstance) arfkt);

		return targets;
	}
}
