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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.StatsSet;
import javolution.util.FastList;

public class L2SkillDrain extends L2Skill {

    private float _absorbPart;
    private int _absorbAbs;

    public L2SkillDrain(StatsSet set) {
        super(set);

        _absorbPart = set.getFloat("absorbPart", 0.f);
        _absorbAbs = set.getInteger("absorbAbs", 0);
    }

    @Override
    public void useSkill(L2Character activeChar, FastList<L2Object> targets) {
        if (activeChar.isAlikeDead()) {
            return;
        }

        boolean ss = false;
        boolean bss = false;

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            L2Character target = (L2Character) n.getValue();

            if ((target.isAlikeDead() || (target.isL2Npc() && !target.isMonster())) && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB) {
                continue;
            }

            if (activeChar != target && target.isInvul()) {
                continue; // No effect on invulnerable chars unless they cast it themselves.
            }
            L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

            if (weaponInst != null) {
                if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) {
                    bss = true;
                    weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
                } else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT) {
                    ss = true;
                    weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
                }
            } // If there is no weapon equipped, check for an active summon.
            else if (activeChar.isL2Summon()) {
                L2Summon activeSummon = (L2Summon) activeChar;

                if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) {
                    bss = true;
                    activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
                } else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT) {
                    ss = true;
                    activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
                }
            }

            double trgCP = target.getCurrentCp();
            boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this));
            int damage = (int) Formulas.calcMagicDam(activeChar, target, this, ss, bss, mcrit);
            double hpAdd = _absorbAbs + _absorbPart * damage;
            if (getId() == 1245 && target.isPlayer()) {
                if (damage - trgCP > 0) {
                    hpAdd = _absorbAbs + _absorbPart * (damage - trgCP);
                } else {
                    hpAdd = 1;
                }
            }
            double hp = ((activeChar.getCurrentHp() + hpAdd) > activeChar.getMaxHp() ? activeChar.getMaxHp() : (activeChar.getCurrentHp() + hpAdd));
            activeChar.setCurrentHp(hp);

            /*StatusUpdate suhp = new StatusUpdate(activeChar.getObjectId());
            suhp.addAttribute(StatusUpdate.CUR_HP, (int)hp);
            activeChar.sendPacket(suhp);*/

            // Check to see if we should damage the target
            if (damage > 0 && (!target.isDead() || getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)) {
                // Manage attack or cast break of the target (calculating rate, sending message...)
                if (!target.isRaid() && Formulas.calcAtkBreak(target, damage)) {
                    target.breakAttack();
                    target.breakCast();
                }

                activeChar.sendDamageMessage(target, damage, mcrit, false, false);

                if (hasEffects() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB) {
                    if (target.reflectSkill(this)) {
                        activeChar.stopSkillEffects(getId());
                        getEffects(null, activeChar);
                        activeChar.sendPacket(SystemMessage.id(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(getId()));
                    } else {
                        // activate attacked effects, if any
                        target.stopSkillEffects(getId());
                        if (Formulas.calcSkillSuccess(activeChar, target, this, false, ss, bss)) {
                            getEffects(activeChar, target);
                        } else {
                            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(getDisplayId()));
                        }
                    }
                }

                target.reduceCurrentHp(damage, activeChar);
            }

            // Check to see if we should do the decay right after the cast
            if (target.isDead() && getTargetType() == SkillTargetType.TARGET_CORPSE_MOB && target.isL2Npc()) {
                ((L2NpcInstance) target).endDecayTask();
            }
        }
        //effect self :]
        L2Effect effect = activeChar.getFirstEffect(getId());
        if (effect != null && effect.isSelfEffect()) {
            //Replace old effect with new one.
            effect.exit();
        }
        // cast self effect if any
        getEffectsSelf(activeChar);
    }
}
