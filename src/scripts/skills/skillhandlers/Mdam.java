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
import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.util.Rnd;
import javolution.util.FastList;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.8.2.9 $ $Date: 2005/04/05 19:41:23 $
 */
public class Mdam implements ISkillHandler {
    //private static Logger _log = Logger.getLogger(Mdam.class.getName());

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    private static final SkillType[] SKILL_IDS = {SkillType.MDAM, SkillType.DEATHLINK};

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    @Override
    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (activeChar.isAlikeDead()) {
            return;
        }

        boolean ss = false;
        boolean bss = false;

        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

        /* if (activeChar.isPlayer())
        {
        if (weaponInst == null)
        {
        SystemMessage sm2 = SystemMessage.id(SystemMessage.S1_S2);
        sm2.addString("You must equip a weapon before casting a spell.");
        activeChar.sendPacket(sm2);
        return;
        }
        } */

        if (weaponInst != null) {
            if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) {
                bss = true;
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            } else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT) {
                ss = true;
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }
        } // If there is no weapon equipped, check for an active summon.
        else if (activeChar instanceof L2Summon) {
            L2Summon activeSummon = (L2Summon) activeChar;

            if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) {
                bss = true;
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            } else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT) {
                ss = true;
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }
        }

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            L2Character target = (L2Character) n.getValue();

            if (target == null) {
                continue;
            }

            if (activeChar.isPlayer() && target.isPlayer() && target.isAlikeDead() && target.isFakeDeath()) {
                target.stopFakeDeath(null);
            } else if (target.isAlikeDead()) {
                continue;
            }

            //			if (skill != null)
            //			if (skill.isOffensive())
            //			{

            //				boolean acted;
            //				if (skill.getSkillType() == L2Skill.SkillType.DOT || skill.getSkillType() == L2Skill.SkillType.MDOT)
            //				    acted = Formulas.calcSkillSuccess(
            //						activeChar, target, skill);
            //				else
            //				    acted = Formulas.calcMagicAffected(
            //						activeChar, target, skill);
            //				if (!acted) {
            //					activeChar.sendPacket(SystemMessage.id(SystemMessage.MISSED_TARGET));
            //					continue;
            //				}
            //
            //			}

            boolean mcrit = false;
            if (skill.getId() == 1265 && Rnd.get(100) < 3) {
                mcrit = true;
            } else {
                mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, skill));
            }

            int damage = (int) Formulas.calcMagicDam(activeChar, target, skill, ss, bss, mcrit);

            /* if (damage > 5000 && activeChar.isPlayer())
            {
            String name = "";
            if (target instanceof L2RaidBossInstance) name = "RaidBoss ";
            if (target instanceof L2NpcInstance)
            name += target.getName() + "(" + ((L2NpcInstance) target).getTemplate().npcId + ")";
            if (target.isPlayer())
            name = target.getName() + "(" + target.getObjectId() + ") ";
            name += target.getLevel() + " lvl";
            Log.add(activeChar.getName() + "(" + activeChar.getObjectId() + ") "
            + activeChar.getLevel() + " lvl did damage " + damage + " with skill "
            + skill.getName() + "(" + skill.getId() + ") to " + name, "damage_mdam");
            }*/

            // Why are we trying to reduce the current target HP here?
            // Why not inside the below "if" condition, after the effects processing as it should be?
            // It doesn't seem to make sense for me. I'm moving this line inside the "if" condition, right after the effects processing...
            // [changed by nexus - 2006-08-15]
            //target.reduceCurrentHp(damage, activeChar);

            if (target.isRaid()
                    && Config.FIXED_DAMAGE_TO_BOSS >= 1) {
                damage = (int) Config.FIXED_DAMAGE_TO_BOSS * 2;
            }

            if (target.getNpcId() == 13016) {
                damage = 30;
            }

            if (damage > 0) {
                // Manage attack or cast break of the target (calculating rate, sending message...)
                if (!target.isRaid() && Formulas.calcAtkBreak(target, damage)) {
                    target.breakAttack();
                    target.breakCast();
                }

                activeChar.sendDamageMessage(target, damage, mcrit, false, false);

                if (skill.hasEffects()) {
                    if (target.reflectSkill(skill)) {
                        activeChar.stopSkillEffects(skill.getId());
                        skill.getEffects(null, activeChar);
                        activeChar.sendPacket(SystemMessage.id(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill.getId()));
                    } else {
                        // activate attacked effects, if any
                        L2Effect scuko = target.getFirstEffect(skill.getId());
                        //target.stopSkillEffects(skill.getId());
                        if (scuko == null && Formulas.calcSkillSuccess(activeChar, target, skill, false, ss, bss)) {
                            skill.getEffects(activeChar, target);
                        } else {
                            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
                        }
                    }
                }

                if (skill.getId() == 1417) {
                    target.removeTarget();
                }

                target.reduceCurrentHp(damage, activeChar);
            }
        }
        //targets.clear();
        // self Effect :]
        L2Effect effect = activeChar.getFirstEffect(skill.getId());
        if (effect != null && effect.isSelfEffect()) {
            //Replace old effect with new one.
            effect.exit();
        }
        skill.getEffectsSelf(activeChar);

        if (skill.isSuicideAttack()) {
            activeChar.doDie(null);
            activeChar.setCurrentHp(0);
        }
    }

    @Override
    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
