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
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.templates.StatsSet;
import javolution.util.FastList;
import net.sf.l2j.Config;

public class L2SkillChargeDmg extends L2Skill {

    final int chargeSkillId;

    public L2SkillChargeDmg(StatsSet set) {
        super(set);
        chargeSkillId = set.getInteger("charge_skill_id");
    }

    @Override
    public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon) {
        if (activeChar.isPlayer()) {
            if (activeChar.getCharges() < getNumCharges()) {
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(getId()));
                return false;
            }
        }
        return super.checkCondition(activeChar, target, itemOrWeapon);
    }

    @Override
    public void useSkill(L2Character caster, FastList<L2Object> targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        // get the effect
        int charges = caster.getCharges();
        if (charges < getNumCharges()) {
            caster.sendPacket(SystemMessage.id(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(getId()));
            return;
        }
        double modifier = 0;
        modifier = 1.1 + 0.201 * charges; // thanks Diego Vargas of L2Guru: 70*((0.8+0.201*No.Charges) * (PATK+POWER)) / PDEF
        if (getTargetType() != SkillTargetType.TARGET_AREA && getTargetType() != SkillTargetType.TARGET_MULTIFACE) {
            caster.decreaseCharges(getNumCharges());
        }

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            L2Character target = (L2Character) n.getValue();
            if (target == null || target.isAlikeDead()) {
                continue;
            }

            L2ItemInstance weapon = caster.getActiveWeaponInstance();

            // TODO: should we use dual or not?
            // because if so, damage are lowered but we dont do anything special with dual then
            // like in doAttackHitByDual which in fact does the calcPhysDam call twice
            //boolean dual  = caster.isUsingDualWeapon();
            boolean shld = Formulas.calcShldUse(caster, target);
            boolean crit = Formulas.calcCrit(caster.getCriticalHit(target, this));
            boolean soul = (weapon != null
                    && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT
                    && weapon.getItemType() != L2WeaponType.DAGGER);

            // damage calculation
            int damage = (int) Formulas.calcPhysDam(caster, target, this, shld, false, false, soul);

            if (crit) {
                damage = (int) Formulas.calcViciousDam(caster, damage, true);
            }

            if (damage > 0) {
                double finalDamage = damage * modifier;

                if (target.isRaid()
                        && Config.FIXED_DAMAGE_TO_BOSS >= 1) {
                    finalDamage = Config.FIXED_DAMAGE_TO_BOSS;
                }

                if (target.getNpcId() == 13016) {
                    finalDamage = 30;
                }

                target.reduceCurrentHp(finalDamage, caster);

                caster.sendDamageMessage(target, (int) finalDamage, false, crit, false);

                if (soul && weapon != null) {
                    weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
                }
            } else {
                caster.sendDamageMessage(target, 0, false, false, true);
            }
        }
        // effect self :]
        L2Effect seffect = caster.getFirstEffect(getId());
        if (seffect != null && seffect.isSelfEffect()) {
            //Replace old effect with new one.
            seffect.exit();
        }
        // cast self effect if any
        getEffectsSelf(caster);
    }

}
