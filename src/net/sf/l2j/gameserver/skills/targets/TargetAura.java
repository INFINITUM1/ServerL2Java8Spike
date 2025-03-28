package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class TargetAura extends TargetList {

    @Override
    public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        // Go through the L2Character _knownList
        FastList<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius());
        if (objs == null || objs.isEmpty()) {
            if (skill.getId() == 347) {
                targets.add(activeChar);
            }

            return targets;
        }

        boolean srcInArena = (activeChar.isInsidePvpZone() && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));
        L2PcInstance player = activeChar.getPlayer();

        L2Character cha = null;
        L2PcInstance trg = null;
        for (FastList.Node<L2Character> n = objs.head(), end = objs.tail(); (n = n.getNext()) != end;) {
            cha = n.getValue();
            if (cha == null) {
                continue;
            }

            if (!(cha.isL2Attackable() || cha.isL2Playable())) {
                continue;
            }

            if (cha.isL2Guard() || cha.isAgathion()) {
                continue;
            }

            if (cha.isDead() || cha.equals(activeChar) || cha.equals(player)) {
                continue;
            }

            if (skill.isSkillTypeOffensive() && (activeChar.isMonster() && cha.isMonster())) {
                continue;
            }

            if (player != null && (cha.isPlayer() || cha.isL2Summon())) {
                trg = cha.getPlayer();
                if (player.equals(trg)) {
                    continue;
                }

                if (!player.checkPvpSkill(trg, skill)) {
                    continue;
                }

                if (trg.isInZonePeace()) {
                    continue;
                }

                if (player.getParty() != null && player.getParty().getPartyMembers().contains(trg)) {
                    continue;
                }

                if (!srcInArena && !(trg.isInsidePvpZone() && !trg.isInsideZone(L2Character.ZONE_SIEGE))) {
                    if (player.getClanId() != 0 && player.getClanId() == trg.getClanId()) {
                        continue;
                    }

                    if ((skill.isSkillTypeOffensive() || skill.isPvpSkill() || skill.isHeroSkill() || skill.isAOEpvp()) && (player.getAllyId() != 0 && player.getAllyId() == trg.getAllyId())) {
                        continue;
                    }
                }
            }

            if (!activeChar.canSeeTarget(cha)) {
                continue;
            }

            if (onlyFirst) {
                targets.add(cha);
                return targets;
            }
            targets.add(cha);
        }

        if (targets.isEmpty() && skill.getId() == 347) {
            targets.add(activeChar);
        }

        return targets;
    }
}
