/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package scripts.skills.skillhandlers;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javolution.util.FastList;
import javolution.util.FastTable;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.instance.*;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.util.Rnd;
import scripts.skills.ISkillHandler;
import scripts.skills.SkillHandler;

/**
 * This Handles Disabler skills
 *
 * @author _drunk_
 */
public class Disablers implements ISkillHandler {

    private static final SkillType[] SKILL_IDS = {
        L2Skill.SkillType.STUN,
        L2Skill.SkillType.ROOT,
        L2Skill.SkillType.SLEEP,
        L2Skill.SkillType.FEAR,
        L2Skill.SkillType.CONFUSION,
        L2Skill.SkillType.AGGDAMAGE,
        L2Skill.SkillType.AGGREDUCE,
        L2Skill.SkillType.AGGREDUCE_CHAR,
        L2Skill.SkillType.AGGREMOVE,
        L2Skill.SkillType.UNBLEED,
        L2Skill.SkillType.UNPOISON,
        L2Skill.SkillType.MUTE,
        L2Skill.SkillType.FAKE_DEATH,
        L2Skill.SkillType.CONFUSE_MOB_ONLY,
        L2Skill.SkillType.NEGATE,
        L2Skill.SkillType.CANCEL,
        L2Skill.SkillType.PARALYZE,
        L2Skill.SkillType.ERASE,
        L2Skill.SkillType.MAGE_BANE,
        L2Skill.SkillType.WARRIOR_BANE,
        L2Skill.SkillType.BETRAY,
        L2Skill.SkillType.HOLD_UNDEAD,
        L2Skill.SkillType.TURN_UNDEAD
    };
    protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());
    private String[] _negateStats = null;
    private float _negatePower = 0.f;
    private int _negateId = 0;

    @Override
    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        SkillType type = skill.getSkillType();
        int SkillId = skill.getId();

        boolean ss = false;
        boolean sps = false;
        boolean bss = false;

        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

        /*
         * if (activeChar.isPlayer()) { if (weaponInst == null &&
         * skill.isOffensive()) { SystemMessage sm2 =
         * SystemMessage.id(SystemMessageId.S1_S2); sm2.addString("Наденьте
         * пушку"); activeChar.sendPacket(sm2); return; }
        }
         */
        if (weaponInst != null) {
            if (skill.isMagic()) {
                if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) {
                    bss = true;
                    if (skill.getId() != 1020) // vitalize
                    {
                        weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
                    }
                } else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT) {
                    sps = true;
                    if (skill.getId() != 1020) // vitalize
                    {
                        weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
                    }
                }
            } else if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT) {
                ss = true;
                if (skill.getId() != 1020) // vitalize
                {
                    weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
                }
            }
        } // If there is no weapon equipped, check for an active summon.
        else if (activeChar instanceof L2Summon) {
            L2Summon activeSummon = (L2Summon) activeChar;

            if (skill.isMagic()) {
                if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) {
                    bss = true;
                    activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
                } else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT) {
                    sps = true;
                    activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
                }
            } else if (activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT) {
                ss = true;
                activeSummon.setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
            }
        }

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            L2Object obj = n.getValue();
            // Get a target
            if (obj == null || !(obj.isL2Character())) {
                continue;
            }

            L2Character target = (L2Character) obj;

            if (target == null || target.isDead()) //bypass if target is null or dead
            {
                continue;
            }

            if (target.isRaid()) //bypass if target is null or dead
            {
                continue;
            }

            switch (type) {
                case BETRAY: {
                    if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        skill.getEffects(activeChar, target);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case FAKE_DEATH: {
                    // stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
                    skill.getEffects(activeChar, target);
                    break;
                }
                case ROOT: {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }

                    if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        skill.getEffects(activeChar, target);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case STUN: {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }

                    if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        skill.getEffects(activeChar, target);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case SLEEP: {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }

                    if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        skill.getEffects(activeChar, target);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case FEAR: {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }
                    /*
                     * int chance = 0; switch(skill.getId()) { case 1376: chance
                     * = 12; break; default: chance = 4; break;
                    }
                     */
                    boolean wrong = false;
                    // Fear skills cannot be used l2pcinstance to l2pcinstance. Heroic Dread, Curse: Fear, Fear and Horror are the exceptions.
                    //if(target.isPlayer() && target.isPlayer() && getSkill().getId() != 1376 && getSkill().getId() != 1169 && getSkill().getId() != 65 && getSkill().getId() != 1092) 
                    //	wrong = true;
                    if (target.isL2Folk()) {
                        wrong = true;
                    }
                    if (target.isL2SiegeGuard()) {
                        wrong = true;
                    }
                    // Fear skills cannot be used on Headquarters Flag.
                    if (target instanceof L2SiegeFlagInstance) {
                        wrong = true;
                    }
                    if (target instanceof L2SiegeSummonInstance) {
                        wrong = true;
                    }
                    if (target.isL2Npc() && !target.isMonster()) {
                        wrong = true;
                    }

                    //if (Rnd.get(100) < chance)
                    if (!wrong && Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        skill.getEffects(activeChar, target);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case PARALYZE: //use same as root for now
                {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }

                    if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        skill.getEffects(activeChar, target);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case CONFUSION:
                case MUTE: {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }

                    if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        skill.getEffects(activeChar, target);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case CONFUSE_MOB_ONLY: {
                    if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        L2Effect[] effects = target.getAllEffects();
                        for (L2Effect e : effects) {
                            if (e.getSkill().getSkillType() == type) {
                                e.exit();
                            }
                        }
                        skill.getEffects(activeChar, target);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case AGGDAMAGE: {
                    if (/*
                             * target.isL2Attackable() &&
                             */Rnd.get(100) < 60) {
                        if (target.getTarget() != activeChar) {
                            target.setTarget(activeChar);
                            target.sendPacket(new MyTargetSelected(activeChar.getObjectId(), 0));
                            target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
                        } else {
                            target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
                        }

                        //target.setTarget(activeChar);
                        //target.setTarget(activeChar);
                        //target.sendPacket(new MyTargetSelected(activeChar.getObjectId(), 0));
                        //target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    //skill.getEffects(activeChar, target);
                    break;
                }
                case AGGREDUCE: {
                    // these skills needs to be rechecked
                    if (target.isL2Attackable()) {
                        skill.getEffects(activeChar, target);

                        double aggdiff = ((L2Attackable) target).getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((L2Attackable) target).getHating(activeChar), target, skill);

                        if (skill.getPower() > 0) {
                            ((L2Attackable) target).reduceHate(null, (int) skill.getPower());
                        } else if (aggdiff > 0) {
                            ((L2Attackable) target).reduceHate(null, (int) aggdiff);
                        }
                    }
                    break;
                }
                case AGGREDUCE_CHAR: {
                    switch (SkillId) {
                        case 11: {
                            if (target.isPlayer() || target.isL2Monster() || target instanceof L2SummonInstance) {
                                if (Rnd.get(100) < 80) {
                                    target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                                    target.setTarget(null);
                                    target.abortAttack();
                                    target.abortCast();
                                } else {
                                    unAffected(activeChar, target.getName(), skill.getId());
                                }
                            }
                            break;
                        }
                        case 5144: {
                            if (target.isPlayer() || target.isL2Monster()) {
                                target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                                target.setTarget(null);
                                target.abortAttack();
                                target.abortCast();
                            }
                            break;
                        }
                        case 12: {
                            if (target.isPlayer()) {
                                if (Rnd.get(100) < 80) {
                                    L2PcInstance xzkaknazvat = (L2PcInstance) target;

                                    target.abortCast();
                                    target.abortAttack();

                                    if (target.getPet() != null) {
                                        if (target.getPet() instanceof L2SummonInstance) {
                                            L2SummonInstance switchtarg = (L2SummonInstance) target.getPet();
                                            xzkaknazvat.setTarget(switchtarg);
                                        } else if (target.getPet().isPet()) {
                                            L2PetInstance switchtarg = (L2PetInstance) target.getPet();
                                            xzkaknazvat.setTarget(switchtarg);
                                        }
                                        return;
                                    } else if (activeChar.getPet() != null) {
                                        if (activeChar.getPet() instanceof L2SummonInstance) {
                                            L2SummonInstance switchtarg = (L2SummonInstance) activeChar.getPet();
                                            xzkaknazvat.setTarget(switchtarg);
                                        } else if (activeChar.getPet().isPet()) {
                                            L2PetInstance switchtarg = (L2PetInstance) activeChar.getPet();
                                            xzkaknazvat.setTarget(switchtarg);
                                        }
                                        return;
                                    }

                                    Collection<L2Character> randomtargets = xzkaknazvat.getKnownList().getKnownCharactersInRadius(300);
                                    if (randomtargets == null || randomtargets.isEmpty()) {
                                        return;
                                    }

                                    int switched = 0;
                                    for (L2Character toswitch : randomtargets) {
                                        if (toswitch == activeChar) {
                                            continue;
                                        }

                                        if (toswitch.isDead()) {
                                            continue;
                                        }

                                        if (toswitch.isPlayer()) {
                                            L2PcInstance switchtarg = (L2PcInstance) toswitch;
                                            xzkaknazvat.setTarget(switchtarg);
                                            switched++;
                                        } else if (toswitch.isL2Monster()) {
                                            L2MonsterInstance switchtarg = (L2MonsterInstance) toswitch;
                                            xzkaknazvat.setTarget(switchtarg);
                                            switched++;
                                        } else if (toswitch instanceof L2SummonInstance) {
                                            L2SummonInstance switchtarg = (L2SummonInstance) toswitch;
                                            xzkaknazvat.setTarget(switchtarg);
                                            switched++;
                                        } else if (toswitch.isPet()) {
                                            L2PetInstance switchtarg = (L2PetInstance) toswitch;
                                            xzkaknazvat.setTarget(switchtarg);
                                            switched++;
                                        } else if (toswitch instanceof L2BabyPetInstance) {
                                            L2BabyPetInstance switchtarg = (L2BabyPetInstance) toswitch;
                                            xzkaknazvat.setTarget(switchtarg);
                                            switched++;
                                        } else if (toswitch instanceof L2TamedBeastInstance) {
                                            L2TamedBeastInstance switchtarg = (L2TamedBeastInstance) toswitch;
                                            xzkaknazvat.setTarget(switchtarg);
                                            switched++;
                                        } else if (toswitch instanceof L2ChestInstance) {
                                            L2ChestInstance switchtarg = (L2ChestInstance) toswitch;
                                            xzkaknazvat.setTarget(switchtarg);
                                            switched++;
                                        } else if (toswitch instanceof L2GuardInstance) {
                                            L2GuardInstance switchtarg = (L2GuardInstance) toswitch;
                                            xzkaknazvat.setTarget(switchtarg);
                                            //if (xzkaknazvat.isAttackingNow()) comment 4tob ne zabit'
                                            //xzkaknazvat.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK,switchtarg);
                                            switched++;
                                        }
                                        if (switched == 1) {
                                            break;
                                        }
                                    }
                                    skill.getEffects(activeChar, target);
                                } else {
                                    unAffected(activeChar, target.getName(), skill.getId());
                                    target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
                                }
                            } else if (target.isL2Monster()) {
                                if (Rnd.get(100) < 80) {
                                    target.setTarget(null);
                                    target.abortCast();
                                    target.abortAttack();
                                    target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                                } else {
                                    unAffected(activeChar, target.getName(), skill.getId());
                                    target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
                case AGGREMOVE: {
                    if (target.isL2Attackable() && !target.isRaid()) {
                        if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                            if (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_UNDEAD) {
                                if (target.isUndead()) {
                                    ((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
                                }
                            } else {
                                ((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
                            }
                        } else {
                            unAffected(activeChar, target.getName(), skill.getId());
                        }
                    }
                    break;
                }
                case HOLD_UNDEAD: {
                    FastList<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius());
                    if (objs == null || objs.isEmpty()) {
                        break;
                    }
                    L2Character undead = null;
                    for (FastList.Node<L2Character> k = objs.head(), kend = objs.tail(); (k = k.getNext()) != kend;) {
                        undead = k.getValue();
                        if (undead == null) {
                            continue;
                        }

                        if (undead == activeChar) {
                            continue;
                        }

                        if (undead.isAlikeDead()) {
                            continue;
                        }

                        if (undead.isRaid()) {
                            continue;
                        }

                        if (!undead.isUndead()) {
                            continue;
                        }

                        //if (!activeChar.canSeeTarget(target))
                        //	continue;
                        //target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                        //target.setTarget(null);
                        //target.abortAttack();
                        //target.abortCast();
                        if (undead.isL2Attackable()) {
                            ((L2Attackable) undead).reduceHate(null, ((L2Attackable) undead).getHating(((L2Attackable) undead).getMostHated()));
                        }
                        //if (skill.getId() == 1049)
                        //	SkillTable.getInstance().getInfo(1049, skill.getLevel()).getEffects(undead, undead);
                    }
                    break;
                }
                case TURN_UNDEAD: {
                    if (target.isAlikeDead()) {
                        continue;
                    }

                    if (target.isRaid()) {
                        continue;
                    }

                    if (!target.isUndead()) {
                        continue;
                    }

                    if (Rnd.get(100) < 70) {
                        SkillTable.getInstance().getInfo(1092, 19).getEffects(target, target);
                        if (Rnd.get(100) < 50) {
                            target.setCurrentHp(1);
                            activeChar.sendPacket(Static.LETHAL_STRIKE);
                        }
                    } else {
                        unAffected(activeChar, target.getName(), skill.getId());
                    }
                    break;
                }
                case UNBLEED: {
                    negateEffect(target, SkillType.BLEED, skill.getPower());
                    break;
                }
                case UNPOISON: {
                    negateEffect(target, SkillType.POISON, skill.getPower());
                    break;
                }
                case ERASE: {
                    if (target instanceof L2SummonInstance || target.isPet()) {
                        if (Rnd.get(100) < 43) {
                            L2PcInstance summonOwner = null;
                            L2Summon summonPet = null;
                            summonOwner = ((L2Summon) target).getOwner();
                            summonPet = summonOwner.getPet();
                            summonPet.unSummon(summonOwner);
                            activeChar.sendPacket(Static.LETHAL_STRIKE);
                        } else {
                            unAffected(activeChar, target.getName(), skill.getId());
                        }
                    }
                    break;
                }
                case MAGE_BANE: {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }

                    if (!Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        unAffected(activeChar, target.getName(), skill.getId());
                        continue;
                    }

                    FastTable<L2Effect> effects = target.getAllEffectsTable();
                    for (int a = 0, z = effects.size(); a < z; a++) {
                        L2Effect e = effects.get(a);
                        if (e == null) {
                            continue;
                        }

                        switch (e.getSkill().getId()) {
                            case 1085:
                            case 1059:
                            case 2053:
                            case 2056:
                            case 1004:
                            case 1002:
                                e.exit();
                                break;
                        }
                    }
                    break;
                }
                case WARRIOR_BANE: {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }

                    if (!Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                        unAffected(activeChar, target.getName(), skill.getId());
                        continue;
                    }

                    FastTable<L2Effect> effects = target.getAllEffectsTable();
                    for (int s = 0, x = effects.size(); s < x; s++) {
                        L2Effect e = effects.get(s);
                        if (e == null) {
                            continue;
                        }

                        switch (e.getSkill().getId()) {
                            case 1204:
                            case 1086:
                            case 2054:
                            case 2058:
                            case 1251:
                            case 1282:
                                e.exit();
                                break;
                        }
                    }
                    break;
                }
                case CANCEL:
                case NEGATE: {
                    if (target.reflectSkill(skill)) {
                        target = activeChar;
                    }

                    if (skill.isCancelType()) {
                        if (Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss)) {
                            int max = Rnd.get(4, 6);
                            int canceled = 0;
                            int count = 0;
                            int finish = target.getBuffCount();
                            FastTable<L2Effect> effects = target.getAllEffectsTable();
                            for (int d = 0, c = effects.size(); d < c; d++) {
                                L2Effect e = effects.get(d);
                                if (e == null) {
                                    continue;
                                }

                                if (e.getSkill().isCancelProtected()) {
                                    continue;
                                }

                                switch (e.getEffectType()) {
                                    case SIGNET_GROUND:
                                    case SIGNET_EFFECT:
                                        continue;
                                }

                                if (e.getSkill().isBuff()) {
                                    if (Rnd.get(100) < finish) {
                                        e.exit();
                                        canceled++;
                                    }
                                    count++;
                                    if (canceled >= max || count >= finish) {
                                        break;
                                    }
                                }
                            }
                        } else {
                            unAffected(activeChar, target.getName(), skill.getId());
                        }
                        break;
                    } // fishing potion
                    else if (skill.getId() == 2275) {
                        _negatePower = skill.getNegatePower();
                        _negateId = skill.getNegateId();

                        negateEffect(target, SkillType.BUFF, _negatePower, _negateId);
                    } // all others negate type skills
                    else {
                        _negateStats = skill.getNegateStats();
                        _negatePower = skill.getNegatePower();

                        for (String stat : _negateStats) {
                            //stat = stat.toLowerCase();//.intern();
                            if ("buff".equalsIgnoreCase(stat)) {
                                int lvlmodifier = 52 + skill.getMagicLevel() * 2;
                                if (skill.getMagicLevel() == 12) {
                                    lvlmodifier = (Experience.MAX_LEVEL - 1);
                                }
                                int landrate = 60;
                                if ((target.getLevel() - lvlmodifier) > 0) {
                                    landrate = 60 - 4 * (target.getLevel() - lvlmodifier);
                                }

                                landrate = (int) activeChar.calcStat(Stats.CANCEL_VULN, landrate, target, null);

                                if (Rnd.get(100) < landrate) {
                                    negateEffect(target, SkillType.BUFF, -1);
                                }
                            } else if ("debuff".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.DEBUFF, -1);
                            } else if ("weakness".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.WEAKNESS, -1);
                            } else if ("stun".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.STUN, -1);
                            } else if ("sleep".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.SLEEP, -1);
                            } else if ("confusion".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.CONFUSION, -1);
                            } else if ("mute".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.MUTE, -1);
                            } else if ("fear".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.FEAR, -1);
                            } else if ("poison".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.POISON, _negatePower);
                            } else if ("bleed".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.BLEED, _negatePower);
                            } else if ("paralyze".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.PARALYZE, -1);
                            } else if ("root".equalsIgnoreCase(stat)) {
                                negateEffect(target, SkillType.ROOT, -1);
                            } else if ("heal".equalsIgnoreCase(stat)) {
                                ISkillHandler Healhandler = SkillHandler.getInstance().getSkillHandler(SkillType.HEAL);
                                if (Healhandler == null) {
                                    _log.severe("Couldn't find skill handler for HEAL.");
                                    continue;
                                }
                                FastList<L2Object> tgts = new FastList<L2Object>();
                                tgts.add(target);
                                try {
                                    Healhandler.useSkill(activeChar, skill, tgts);
                                } catch (IOException e) {
                                    _log.log(Level.WARNING, "", e);
                                }
                                //tgts.clear();
                            } else if ("herodebuff".equalsIgnoreCase(stat)) {
                                negateHeroEffect(target);
                            } else if ("malaria".equalsIgnoreCase(stat)) {
                                target.stopSkillEffects(4554);
                            } else if ("cholera".equalsIgnoreCase(stat)) {
                                target.stopSkillEffects(4552);
                            } else if ("flu".equalsIgnoreCase(stat)) {
                                target.stopSkillEffects(4553);
                            } else if ("rheumatism".equalsIgnoreCase(stat)) {
                                target.stopSkillEffects(4551);
                            }
                        }//end for
                    }//end else
                }// end case
            }//end switch
        }//end for

        // self Effect :]
        L2Effect effect = activeChar.getFirstEffect(skill.getId());
        if (effect != null && effect.isSelfEffect()) {
            //Replace old effect with new one.
            effect.exit();
        }
        skill.getEffectsSelf(activeChar);
        //targets.clear();

    } //end void

    private void negateHeroEffect(L2Character target) {
        FastTable<L2Effect> effects = target.getAllEffectsTable();
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect e = effects.get(i);
            if (e == null) {
                continue;
            }

            switch (e.getSkill().getId()) {
                case 3579:
                case 3584:
                case 3586:
                case 3588:
                case 3590:
                case 3594:
                    e.exit();
                    break;
            }
        }
    }

    private void negateEffect(L2Character target, SkillType type, double power) {
        negateEffect(target, type, power, 0);
    }

    private void negateEffect(L2Character target, SkillType type, double power, int skillId) {
        FastTable<L2Effect> effects = target.getAllEffectsTable();
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().getId() == 4515 || e.getSkill().getId() == 4215) {
                continue;
            }

            if (power == -1) // if power is -1 the effect is always removed without power/lvl check ^^
            {
                if (e.getSkill().getSkillType() == type || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type)) {
                    if (skillId != 0) {
                        if (skillId == e.getSkill().getId()) {
                            e.exit();
                        }
                    } else {
                        e.exit();
                    }
                }
            } else if ((e.getSkill().getSkillType() == type && e.getSkill().getPower() <= power) || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type && e.getSkill().getEffectLvl() <= power)) {
                if (skillId != 0) {
                    if (skillId == e.getSkill().getId()) {
                        e.exit();
                    }
                } else {
                    e.exit();
                }
            }
        }
    }

    private void unAffected(L2Character activeChar, String targetName, int skillId) {
        if (activeChar.isPlayer()) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(targetName).addSkillName(skillId));
        }
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
