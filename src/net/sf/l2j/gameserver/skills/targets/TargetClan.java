package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.Util;

public class TargetClan extends TargetList {

    @Override
    public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (!activeChar.isPlayer()) {
            return targets;
        }

        if (activeChar.isInOlympiadMode()) {
            targets.add(activeChar);
            return targets;
        }

        L2PcInstance player = activeChar.getPlayer();
        if (onlyFirst) {
            targets.add(player);
            return targets;
        }
        targets.add(player);

        if (player.getClanId() == 0) {
            return targets;
        }

        L2PcInstance newTarget = null;
        for (L2ClanMember member : player.getClan().getMembers()) {
            if (member == null) {
                continue;
            }

            newTarget = member.getPlayerInstance();
            if (newTarget == null) {
                continue;
            }

            if (newTarget.isDead()) {
                continue;
            }

            // Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
            if (!player.checkPvpSkill(newTarget, skill)) {
                continue;
            }

            if (skill.getSkillRadius() > 0) {
                if (!Util.checkIfInRange(skill.getSkillRadius(), activeChar, newTarget, true)) {
                    continue;
                }

                if (!player.canSeeTarget(newTarget)) {
                    continue;
                }
            }

            targets.add(newTarget);
        }
        return targets;
    }
}
