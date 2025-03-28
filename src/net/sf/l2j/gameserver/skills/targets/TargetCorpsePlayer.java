package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;

public class TargetCorpsePlayer extends TargetList
{
	public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill)
	{
        if (!activeChar.isPlayer())
			return targets;

        if (target == null || !target.isDead())
		{
			activeChar.sendPacket(Static.TARGET_IS_INCORRECT);
			return targets;
		}

        if (target.isInsideZone(L2Character.ZONE_SIEGE))
        {
            activeChar.sendPacket(Static.CANNOT_BE_RESURRECTED_DURING_SIEGE);
			return targets;
        }

		if (target.isPlayer())
		{
            if (target.isReviveRequested())
            {
                if (target.isRevivingPet())
                    activeChar.sendPacket(Static.MASTER_CANNOT_RES); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
                else
                    activeChar.sendPacket(Static.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.

				return targets;
            }
			targets.add(target);
		}
		else if (target.isPet())
		{                                
			if (!activeChar.equals(target.getOwner()))
            {
                activeChar.sendPacket(Static.NOT_PET_OWNER);
				return targets;
            }
			targets.add(target);
		}
		return targets;
	}
}
