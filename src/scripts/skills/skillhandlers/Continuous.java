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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.util.Rnd;
import scripts.skills.ISkillHandler;
import javolution.util.FastList;
import javolution.util.FastTable;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.9 $ $Date: 2005/04/03 15:55:04 $
 */
public class Continuous implements ISkillHandler {
    //private static Logger _log = Logger.getLogger(Continuous.class.getName());

    private static final SkillType[] SKILL_IDS = {
        L2Skill.SkillType.BUFF,
        L2Skill.SkillType.DEBUFF,
        L2Skill.SkillType.DOT,
        L2Skill.SkillType.MDOT,
        L2Skill.SkillType.POISON,
        L2Skill.SkillType.BLEED,
        L2Skill.SkillType.HOT,
        L2Skill.SkillType.CPHOT,
        L2Skill.SkillType.MPHOT,
        //L2Skill.SkillType.MANAHEAL,
        //L2Skill.SkillType.MANA_BY_LEVEL,
        L2Skill.SkillType.CONT,
        L2Skill.SkillType.WEAKNESS,
        L2Skill.SkillType.REFLECT,
        L2Skill.SkillType.UNDEAD_DEFENSE,
        L2Skill.SkillType.AGGDEBUFF,
        L2Skill.SkillType.FORCE_BUFF
    };

    /*
     * (non-Javadoc) @see
     * net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance,
     * net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (skill == null) {
            return;
        }

        L2Character target = null;
        L2PcInstance player = null;
        if (activeChar.isPlayer()) {
            player = activeChar.getPlayer();
        }

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            target = (L2Character) n.getValue();

            if (target == null || target.isDead()) //bypass if target is null or dead
            {
                continue;
            }
            if (skill.canBeReflected() && target.reflectSkill(skill)) {
                target = activeChar;
            }

            // Walls and Door should not be buffed
            if (target.isL2Door() || target instanceof L2SiegeFlagInstance && (skill.getSkillType() == L2Skill.SkillType.BUFF || skill.getSkillType() == L2Skill.SkillType.HOT)) {
                continue;
            }

            // Player holding a cursed weapon can't be buffed and can't buff
            if (skill.getSkillType() == L2Skill.SkillType.BUFF) {
                if (target != activeChar) {
                    if (target.isBlockingBuffs()) {
                        continue;
                    }

                    if (player != null && player.isCursedWeaponEquiped()) {
                        continue;
                    }
                }
            }

            if (skill.isOffensive()) {

                boolean ss = false;
                boolean sps = false;
                boolean bss = false;
                if (player != null) {
                    L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
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
                    }
                } else if (activeChar.isL2Summon()) {
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

                boolean acted = Formulas.calcSkillSuccess(activeChar, target, skill, ss, sps, bss);
                if (!acted) {
                    activeChar.sendPacket(Static.ATTACK_FAILED);
                    continue;
                }

            }
            /*boolean stopped = false;
            FastTable<L2Effect> effects = target.getAllEffectsTable();
            if (skill != null) {
            for (int k = 0, m = effects.size(); k < m; k++) {
            L2Effect e = effects.get(k);
            if (e == null) {
            continue;
            }
            
            if (e.getSkill().getId() == skill.getId()) {
            e.exit(true);
            stopped = true;
            break;
            }
            }
            }
            if (skill.isToggle() && stopped) {
            return;
            }*/

            // if this is a debuff let the duel manager know about it
            // so the debuff can be removed after the duel
            // (player & target must be in the same duel)
            if (player != null && player.getDuel() != null && target.isPlayer() && target.getPlayer().getDuel() != null
                    && (skill.getSkillType() == L2Skill.SkillType.DEBUFF || skill.getSkillType() == L2Skill.SkillType.BUFF)
                    && player.getDuel() == target.getPlayer().getDuel()) {
                for (L2Effect buff : skill.getEffects(activeChar, target)) {
                    if (buff != null) {
                        player.getDuel().onBuff(target.getPlayer(), buff);
                    }
                }
            } else {
                skill.getEffects(activeChar, target, target.getCurrentHp(), target.getCurrentMp(), target.getCurrentCp());
            }

            if (skill.getSkillType() == L2Skill.SkillType.AGGDEBUFF) {
                if (target.isL2Attackable()) {
                    target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) skill.getPower());
                } else if (target instanceof L2PlayableInstance) {
                    if (target.getTarget() == activeChar) {
                        target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
                    } else {
                        target.setTarget(activeChar);
                    }
                }
            }

            FastTable<L2Effect> effects = target.getAllEffectsTable();
            switch (skill.getId()) {
                case 342:
                    //L2Effect[] effectsc = target.getAllEffects();
                    int max = Rnd.get(2, 6);
                    int canceled = 0;
                    int count = 0;
                    int finish = target.getBuffCount();

                    for (int h = 0, j = effects.size(); h < j; h++) {
                        L2Effect e = effects.get(h);
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
                        /*
                         * if (e.getSkill().getId() != 4082 &&
                         * e.getSkill().getId() != 4215 && e.getSkill().getId()
                         * != 4515 && e.getSkill().getId() != 5182 &&
                         * e.getSkill().getId() != 110 && e.getSkill().getId()
                         * != 111 && e.getSkill().getId() != 1323 &&
                         * e.getSkill().getId() != 1325 && e.getSkill().getId()
                         * != 4146 && e.getSkill().getId() != 4148 &&
                         * e.getSkill().getId() != 4625 && e.getSkill().getId()
                         * != 4147 && e.getSkill().getId() != 4150 &&
                         * e.getSkill().getId() != 4559){ if
                         * (e.getSkill().isBuff()) { if (Rnd.get(100) < finish)
                         * { e.exit(); canceled++; } count++; if (canceled >=
                         * max || count >= finish) break; } }
                         */
                    }
                    break;
                case 1358:
                case 1360:
                    //L2Effect[] effects = target.getAllEffects();
                    for (int a = 0, z = effects.size(); a < z; a++) {
                        L2Effect e = effects.get(a);
                        if (e == null) {
                            continue;
                        }
                        switch (e.getSkill().getId()) {
                            case 1005:
                            case 1009:
                            case 1010:
                            case 1040:
                                e.exit();
                                break;
                        }
                    }
                    break;
                case 1359:
                case 1361:
                    //L2Effect[] effects = target.getAllEffects();
                    for (int s = 0, x = effects.size(); s < x; s++) {
                        L2Effect e = effects.get(s);
                        if (e == null) {
                            continue;
                        }
                        switch (e.getSkill().getId()) {
                            case 230:
                            case 1204:
                            case 1282:
                            case 2011:
                                e.exit();
                                break;
                        }
                    }
                    break;
                case 1363:
                    target.setCurrentHp(target.getCurrentHp() * 1.2);
                    //target.setCurrentHp(target.getMaxHp());
                    break;
                case 1416:
                    if (skill.getPower() == 800) {
                        target.setCurrentCp(target.getMaxCp());
                    } else {
                        target.setCurrentCp(target.getCurrentCp() + skill.getPower());
                    }
                    break;
            }
        }
        // self Effect :]
        L2Effect effect = activeChar.getFirstEffect(skill.getId());
        if (effect != null && effect.isSelfEffect()) {
            //Replace old effect with new one.
            effect.exit();
        }
        skill.getEffectsSelf(activeChar);

        if (skill.getId() == 3206) {
            activeChar.broadcastPacket(new MagicSkillUser(activeChar, activeChar, Rnd.get(4411, 4417), 1, 1, 0));
        }
        //targets.clear();
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
