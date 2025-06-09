/* This program is free software; you can redistribute it and/or modify
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
import net.sf.l2j.gameserver.cache.Static;
//import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import scripts.skills.ISkillHandler;
import javolution.util.FastList;

/**
 *
 * @author Steuf
 */
public class Blow implements ISkillHandler {

    private static final SkillType[] SKILL_IDS = {SkillType.BLOW};
    private int _successChance;
    public final static int FRONT = Config.BLOW_CHANCE_FRONT;
    public final static int SIDE = Config.BLOW_CHANCE_SIDE;
    public final static int BEHIND = Config.BLOW_CHANCE_BEHIND;

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (activeChar == null || activeChar.isAlikeDead()) {
            return;
        }

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            L2Character target = (L2Character) n.getValue();
            if (target == null || target.isAlikeDead()) {
                continue;
            }

            if (activeChar.isBehindTarget()) {
                _successChance = BEHIND;
            } else if (activeChar.isFrontTarget()) {
                _successChance = FRONT;
            } else {
                _successChance = SIDE;
            }
            //If skill requires Crit or skill requires behind,
            //calculate chance based on DEX, Position and on self BUFF
            if (/*((skill.getCondition() & L2Skill.COND_BEHIND) != 0) && _successChance == BEHIND || *//*((skill.getCondition() & L2Skill.COND_CRIT) != 0) && */Formulas.calcBlow(activeChar, target, skill, _successChance)) {
                if (skill.getId() == 321) {
                    if (target.isPlayer()) {

                        int posX = target.getX();
                        int posY = target.getY();
                        int posZ = target.getZ();
                        int signx = -1;
                        int signy = -1;
                        if (posX > activeChar.getX()) {
                            signx = 1;
                        }
                        if (posY > activeChar.getY()) {
                            signy = 1;
                        }

                        posX += signx * 14;
                        posY += signy * 14;
                        target.setRunning();

                        L2CharPosition cp = new L2CharPosition(posX, posY, posZ, target.calcHeading(posX, posY));
                        target.stopMove(cp);
                        target.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, cp);
                        target.setHeading(activeChar.getHeading());
                        target.sendPacket(new ValidateLocation(target));
                        target.sendPacket(new StopMove(target));
                    }
                }

                if (skill.hasEffects()) {
                    if (target.reflectSkill(skill)) {
                        L2Effect scuko = activeChar.getFirstEffect(skill.getId());
                        if (scuko == null) {
                            skill.getEffects(activeChar, target);
                            target.sendPacket(SystemMessage.id(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill.getId()));
                        } else {
                            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
                        }
                    }
                }
                L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
                boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() == L2WeaponType.DAGGER);
                boolean shld = Formulas.calcShldUse(activeChar, target);

                // Crit rate base crit rate for skill, modified with STR bonus
                boolean crit = false;
                if (Formulas.calcCrit(activeChar, skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(activeChar))) {
                    crit = true;
                }
                double damage = (int) Formulas.calcBlowDamage(activeChar, target, skill, shld, soul);
                if (crit) {
                    activeChar.sendPacket(Static.CRITICAL_HIT);
                    damage *= 1.6;
                    // Vicious Stance is special after C5, and only for BLOW skills
                    // Adds directly to damage
                    L2Effect vicious = activeChar.getFirstEffect(312);
                    if (vicious != null && damage > 1) {
                        damage += (vicious.getLevel() * 30);
                        /*for(Func func: vicious.getStatFuncs())
                         {
                         Env env = new Env();
                         env.cha = activeChar;
                         env.target = target;
                         env.skill = skill;
                         env.value = damage;
                         func.calc(env);
                         damage = (int)env.value;
                         }*/
                    }
                    if (activeChar.getFirstEffect(355) != null) {
                        if (activeChar.isBehindTarget()) {
                            damage *= 1.9;
                        } else if (activeChar.isFrontTarget()) {
                            damage *= 0.7;
                        }
                    }
                }

                if (soul && weapon != null) {
                    weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
                }
                if (skill.getDmgDirectlyToHP() && target.isPlayer()) {
                    L2PcInstance player = (L2PcInstance) target;
                    if (!player.isInvul()) {
                        // Check and calculate transfered damage
                        L2Summon summon = player.getPet();
                        if (summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, player, summon, true)) {
                            int tDmg = (int) damage * (int) player.getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) / 100;

                            // Only transfer dmg up to current HP, it should not be killed
                            if (summon.getCurrentHp() < tDmg) {
                                tDmg = (int) summon.getCurrentHp() - 1;
                            }
                            if (tDmg > 0) {
                                summon.reduceCurrentHp(tDmg, activeChar);
                                damage -= tDmg;
                            }
                        }

                        /*if (damage >= player.getCurrentHp())
                         {
                         if(player.isInDuel()) player.setCurrentHp(1);
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
                    /*SystemMessage smsg = SystemMessage.id(SystemMessageId.S1_GAVE_YOU_S2_DMG);
                     smsg.addString(activeChar.getName());
                     smsg.addNumber((int)damage);
                     player.sendPacket(smsg);*/
                } else {
                    if (target.isRaid()
                            && Config.FIXED_DAMAGE_TO_BOSS >= 1) {
                        damage = Config.FIXED_DAMAGE_TO_BOSS;
                    }
                    if (target.getNpcId() == 13016) {
                        damage = 15;
                    }
                    target.reduceCurrentHp(damage, activeChar);
                }

                if (activeChar.isPlayer()
                        && target.isMonster()) {
                    SystemMessage sm = SystemMessage.id(SystemMessageId.YOU_DID_S1_DMG);
                    sm.addNumber((int) damage);
                    activeChar.sendPacket(sm);
                }

                if (target.isSleeping()) {
                    target.stopSleeping(null);
                }

                if (target.getFirstEffect(447) != null) {
                    activeChar.reduceCurrentHp(damage / 2.7, target);
                }

                //Possibility of a lethal strike
                if (!target.isRaid()
                        && !(target.isL2Door())
                        && !(target.isL2Npc() && ((L2NpcInstance) target).getNpcId() == 35062)) {
                    int chance = Rnd.get(100);
                    //2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1)
                    if (skill.getLethalChance2() > 0 && chance < 2) {
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
                        activeChar.sendPacket(Static.LETHAL_STRIKE);
                    } else if (skill.getLethalChance1() > 0 && chance < 1) {
                        if (target.isPlayer()) {
                            L2PcInstance player = (L2PcInstance) target;
                            if (!player.isInvul()) {
                                player.setCurrentCp(1); // Set CP to 1
                            }
                        } else if (target.isL2Npc()) // If is a monster remove first damage and after 50% of current hp
                        {
                            target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar);
                        }
                        activeChar.sendPacket(Static.LETHAL_STRIKE);
                    }
                }
            }
            L2Effect effect = activeChar.getFirstEffect(skill.getId());
            //Self Effect
            if (effect != null && effect.isSelfEffect()) {
                effect.exit();
            }
            skill.getEffectsSelf(activeChar);
        }
        //targets.clear();
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
