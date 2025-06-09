package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;

public class TargetEnemySummon extends TargetList {

    @Override
    public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target == null || target.isDead() || !target.isL2Summon()) {
            return targets;
        }

        if (activeChar.isPlayer() && !target.equals(activeChar.getPet()) 
                && ((target.getOwner().getPvpFlag() != 0 || target.getOwner().getKarma() > 0)
                || (target.getOwner().isInsidePvpZone() && activeChar.isInsidePvpZone())
                || (activeChar.isInOlympiadMode() && target.getOwner().isInOlympiadMode())))
            targets.add(target);

        /*if (activeChar.isPlayer() && activeChar.getPet() != null && target.equals(activeChar.getPet())) {
            return targets;
        }

        if (target.getOwner().getPvpFlag() == 0 || target.getOwner().getKarma() == 0) {
            return targets;
        }

        if ((activeChar.isInsidePvpZone() && target.getOwner().isInsidePvpZone())
                || (activeChar.isInOlympiadMode() && target.getOwner().isInOlympiadMode())) {
            targets.add(target);
        }*/

        return targets;
    }
}
