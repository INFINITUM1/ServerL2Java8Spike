package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class TargetMultiface extends TargetList {

    public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target == null || (!(target.isL2Attackable()) && !(target.isPlayer()))) {
            activeChar.sendPacket(Static.TARGET_IS_INCORRECT);
            return targets;
        }

        if (onlyFirst) {
            targets.add(activeChar);
            return targets;
        }

        targets.add(activeChar);

        FastList<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius());
        if (objs == null || objs.isEmpty()) {
            return targets;
        }

        L2Character obj = null;
        for (FastList.Node<L2Character> n = objs.head(), end = objs.tail(); (n = n.getNext()) != end;) {
            obj = n.getValue();
            if (obj == null) {
                continue;
            }

            if (obj.equals(target) || obj.isDead() || obj.isAgathion()) {
                continue;
            }

            if (!(obj.isL2Attackable())) {
                continue;
            }

            if (skill.isSkillTypeOffensive() && (activeChar.isMonster() && obj.isMonster())) {
                continue;
            }

            targets.add(obj);
        }

        if (targets.size() == 0) {
            activeChar.sendPacket(Static.TARGET_CANT_FOUND);
        }

        return targets;
    }
}
