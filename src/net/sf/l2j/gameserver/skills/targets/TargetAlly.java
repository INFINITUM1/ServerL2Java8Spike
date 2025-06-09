package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class TargetAlly extends TargetList {

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

        FastList<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius());
        if (objs == null || objs.isEmpty()) {
            return targets;
        }

        L2Character cha = null;
        for (FastList.Node<L2Character> n = objs.head(), end = objs.tail(); (n = n.getNext()) != end;) {
            cha = n.getValue();
            if (cha == null) {
                continue;
            }

            if (!cha.isPlayer() || cha.isDead()) {
                continue;
            }

            L2PcInstance allyTarget = cha.getPlayer();

            if ((allyTarget.getAllyId() == 0 || allyTarget.getAllyId() != player.getAllyId()) && (allyTarget.getClan() == null || allyTarget.getClanId() != player.getClanId())) {
                continue;
            }

            if (player.isInDuel() && (player.getDuel() != allyTarget.getDuel() || (player.getParty() != null && !player.getParty().getPartyMembers().contains(cha)))) {
                continue;
            }

            // Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
            if (!player.checkPvpSkill(cha, skill)) {
                continue;
            }

            if (!activeChar.canSeeTarget(cha)) {
                continue;
            }

            /*
             * if (onlyFirst) { targets.add(cha); return targets; }
             */
            targets.add(cha);
        }
        return targets;
    }
}
