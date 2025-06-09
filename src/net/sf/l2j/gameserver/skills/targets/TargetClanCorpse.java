package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.Util;

public class TargetClanCorpse extends TargetList {

    public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (!activeChar.isPlayer()) {
            return targets;
        }

        /*if (activeChar.isInOlympiadMode())
        {
        targets.add(activeChar);
        return targets;
        }*/

        L2Clan clan = activeChar.getClan();
        if (clan == null) {
            return targets;
        }

        L2PcInstance player = activeChar.getPlayer();
        for (L2ClanMember member : clan.getMembers()) {
            if (member == null) {
                continue;
            }

            L2PcInstance newTarget = member.getPlayerInstance();
            if (newTarget == null) {
                continue;
            }

            if (!newTarget.isDead()) {
                continue;
            }

            if (skill.getSkillType() == SkillType.RESURRECT && newTarget.isInsideZone(L2Character.ZONE_SIEGE)) {
                continue;
            }

            if (player.isInDuel() && (player.getDuel() != newTarget.getDuel() || (player.getParty() != null && !player.getParty().getPartyMembers().contains(newTarget)))) {
                continue;
            }

            // Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
            if (!player.checkPvpSkill(newTarget, skill)) {
                continue;
            }

            if (!Util.checkIfInRange(skill.getSkillRadius(), activeChar, newTarget, true)) {
                continue;
            }

            if (!player.canSeeTarget(newTarget)) {
                continue;
            }

            if (onlyFirst) {
                targets.add(newTarget);
                return targets;
            }
            targets.add(newTarget);
        }
        return targets;
    }
}
