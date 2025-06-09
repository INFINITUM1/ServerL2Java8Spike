package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class TargetArea extends TargetList {

    @Override
    public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target == null || target.isAlikeDead()) {
            activeChar.sendPacket(Static.TARGET_IS_INCORRECT);
            return targets;
        }

        if ((skill.getCastRange() >= 0 && (target.equals(activeChar)))
                || (!(target.isL2Attackable() || target.isL2Playable()))) //target is null or self or dead/faking
        {
            activeChar.sendPacket(Static.TARGET_IS_INCORRECT);
            return targets;
        }

        L2Character cha;
        if (skill.getCastRange() >= 0) {
            cha = target;
            if (onlyFirst) {
                targets.add(cha);
                return targets;
            }
            //targets.add(cha); // Add target to target list
        } else {
            cha = activeChar;
        }

        FastList<L2Character> objs = activeChar.getKnownList().getKnownCharacters();
        L2PcInstance src = activeChar.getPlayer();
        boolean srcInArena = (cha.isInsidePvpZone() && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));
        //FastList<L2Character> objs = cha.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius());
        if (objs == null || objs.isEmpty()) {
            return targets;
        }

        L2Character obj = null;
        L2PcInstance trg = null;
        for (FastList.Node<L2Character> n = objs.head(), end = objs.tail(); (n = n.getNext()) != end;) {
            obj = n.getValue();
            if (obj == null) {
                continue;
            }

            if (obj.equals(activeChar) || obj.isAlikeDead()) {
                continue;
            }

            if (!(obj.isL2Attackable() || obj.isL2Playable())) {
                continue;
            }

            if (obj.isL2Guard() || cha.isAgathion()) {
                continue;
            }

            if (!target.isInsideRadius(obj, skill.getSkillRadius(), false, false)) {
                continue;
            }

            if (skill.isSkillTypeOffensive() && (activeChar.isMonster() && obj.isMonster())) {
                continue;
            }

            if (src == null) // Skill user is not L2PlayableInstance
            {
                if (cha.isL2Playable()
                        && // If effect starts at L2PlayableInstance and
                        !(obj.isL2Playable())) // Object is not L2PlayableInstance
                {
                    continue;
                }
            } else if (obj.isPlayer() || obj.isL2Summon()) {
                trg = obj.getPlayer();
                if (trg.equals(src)) {
                    continue;
                }

                if (!src.checkPvpSkill(trg, skill)) {
                    continue;
                }

                if (trg.isInZonePeace()) {
                    continue;
                }

                if ((src.getParty() != null && src.getParty().getPartyMembers().contains(trg))) {
                    continue;
                }

                if (!srcInArena && !(trg.isInsidePvpZone() && !trg.isInsideZone(L2Character.ZONE_SIEGE))) {
                    //if(src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
                    //continue;

                    if ((skill.isSkillTypeOffensive() || (skill.isPvpSkill() && skill.getId() != 452) || skill.isHeroSkill() || skill.isAOEpvp()) && (src.getAllyId() != 0 && src.getAllyId() == trg.getAllyId())) {
                        continue;
                    }

                    if (src.getClanId() != 0 && src.getClanId() == trg.getClanId()) {
                        continue;
                    }
                }
            }

            if (!activeChar.canSeeTarget(obj)) {
                continue;
            }

            targets.add(obj);
        }
        return targets;
    }
}
