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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.effects.EffectCharge;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.util.Rnd;
import javolution.util.FastList;
import javolution.util.FastList.Node;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.7.2.16 $ $Date: 2005/04/06 16:13:49 $
 */
public class Pdam implements ISkillHandler {
    // all the items ids that this handler knowns

    private static final Logger _log = Logger.getLogger(Pdam.class.getName());

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    private static final SkillType[] SKILL_IDS = {SkillType.PDAM, /*SkillType.CHARGEDAM*/};

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (activeChar.isAlikeDead()) {
            return;
        }

        int damage = 0;

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            L2Character target = (L2Character) n.getValue();

            if (target == null || target.isDead()) {
                continue;
            }

            if (target == activeChar) {
                continue;
            }

            L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
            if (activeChar.isPlayer() && target.isPlayer() && target.isAlikeDead() && target.isFakeDeath()) {
                target.stopFakeDeath(null);
            } else if (target.isAlikeDead()) {
                continue;
            }

            boolean dual = activeChar.isUsingDualWeapon();
            boolean shld = Formulas.calcShldUse(activeChar, target);
            // PDAM critical chance not affected by buffs, only by STR. Only some skills are meant to crit.
            boolean crit = false;

            if (skill.getBaseCritRate() > 0) {
                crit = Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(activeChar));
            }

            boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);

            if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0) {
                damage = 0;
            } else {
                damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, false, dual, soul);
            }

            if (crit) {
                damage = (int) Formulas.calcViciousDam(activeChar, damage, true);
            }

            if (skill.getId() == 314) {
                double hp = activeChar.getMaxHp();
                double curhp = activeChar.getCurrentHp();
                double xz = (curhp / hp) * 100;
                long rounded = Math.round(xz);
                int intper = (int) rounded;
                int prepower = (int) skill.getPower(activeChar);
                int lol = 100 - intper;
                int powper = (prepower / 240) * lol;
                damage += powper;
            }

            if (soul && weapon != null) {
                weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
            }

            if (target.isRaid()
                    && Config.FIXED_DAMAGE_TO_BOSS >= 1) {
                damage = (int) Config.FIXED_DAMAGE_TO_BOSS;
            }

            if (target.getNpcId() == 13016) {
                damage = 15;
            }

            if (damage > 0) {
                activeChar.sendDamageMessage(target, damage, false, crit, false);

                if (skill.hasEffects()) {
                    if (target.reflectSkill(skill)) {
                        activeChar.stopSkillEffects(skill.getId());
                        skill.getEffects(null, activeChar);
                        activeChar.sendPacket(SystemMessage.id(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill.getId()));
                    } else {
                        // activate attacked effects, if any
                        //target.stopSkillEffects(skill.getId());
                        if (Formulas.calcSkillSuccess(activeChar, target, skill, false, false, false)) {
                            skill.getEffects(activeChar, target);
                            target.sendPacket(SystemMessage.id(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill.getId()));
                        } else {
                            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
                        }
                    }
                }

                // Success of lethal effect
                int chance = Rnd.get(100);
                if (!target.isRaid()
                        && chance < skill.getLethalChance1()
                        && !(target.isL2Door())
                        && !(target.isL2Npc() && ((L2NpcInstance) target).getNpcId() == 35062)) {
                    // 1st lethal effect activate (cp to 1 or if target is npc then hp to 50%)
                    if (skill.getLethalChance2() > 0 && chance >= skill.getLethalChance2()) {
                        if (target.isPlayer()) {
                            L2PcInstance player = (L2PcInstance) target;
                            if (!player.isInvul()) {
                                player.setCurrentCp(1); // Set CP to 1
                                player.reduceCurrentHp(damage, activeChar);
                            }
                        } else if (target.isL2Monster()) // If is a monster remove first damage and after 50% of current hp
                        {
                            target.reduceCurrentHp(damage, activeChar);
                            target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar);
                        }
                    } else //2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1)
                    {
                        // If is a monster damage is (CurrentHp - 1) so HP = 1
                        if (target.isL2Npc()) {
                            target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar);
                        } else if (target.isPlayer()) // If is a active player set his HP and CP to 1
                        {
                            L2PcInstance player = (L2PcInstance) target;
                            if (!player.isInvul()) {
                                player.setCurrentHp(1);
                                player.setCurrentCp(1);
                            }
                        }
                    }
                    // Lethal Strike was succefful!
                    activeChar.sendPacket(Static.LETHAL_STRIKE_SUCCESSFUL);
                } else {
                    // Make damage directly to HP
                    if (skill.getDmgDirectlyToHP()) {
                        if (target.isPlayer()) {
                            L2PcInstance player = (L2PcInstance) target;
                            if (!player.isInvul()) {
                                /*if (damage >= player.getCurrentHp())
                                 {
                                 if (player.isInDuel())
                                 player.setCurrentHp(1);
                                 else
                                 {
                                 player.setCurrentHp(0);
                                 if (player.isInOlympiadMode()) 
                                 {
                                 player.abortAttack();
                                 player.abortCast();
                                 player.getStatus().stopHpMpRegeneration();
                                 player.setIsKilledAlready(true);
                                 player.setIsPendingRevive(true);
                                 if (player.getPet() != null)
                                 player.getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
                                 } 
                                 else
                                 player.doDie(activeChar);
                                 }
                                 }
                                 else
                                 player.setCurrentHp(player.getCurrentHp() - damage);*/
                                player.reduceCurrentHp(damage, activeChar, true, true);
                            }

                            player.sendPacket(SystemMessage.id(SystemMessageId.S1_GAVE_YOU_S2_DMG).addString(activeChar.getName()).addNumber(damage));

                        } else {
                            target.reduceCurrentHp(damage, activeChar);
                        }
                    } else {
                        target.reduceCurrentHp(damage, activeChar);
                    }
                }
                if (target.getFirstEffect(447) != null) {
                    activeChar.reduceCurrentHp(damage / 2.7, target);
                }
            } else // No - damage
            {
                activeChar.sendPacket(Static.ATTACK_FAILED);
            }

            if (skill.getId() == 345 || skill.getId() == 346) // Sonic Rage or Raging Force
            {
                if (activeChar.getCharges() < 7) {
                    activeChar.increaseCharges();
                }
                //else спамит сильно
                //	activeChar.sendPacket(Static.FORCE_MAXLEVEL_REACHED);
            }
            //self Effect :]
            L2Effect effect = activeChar.getFirstEffect(skill.getId());
            if (effect != null && effect.isSelfEffect()) {
                //Replace old effect with new one.
                effect.exit();
            }
            skill.getEffectsSelf(activeChar);
        }
        //targets.clear();

        if (activeChar.isPlayer()) {
            activeChar.rechargeAutoSoulShot(true, false, false);
        }

        if (skill.isSuicideAttack()) {
            activeChar.doDie(null);
            activeChar.setCurrentHp(0);
        }
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
