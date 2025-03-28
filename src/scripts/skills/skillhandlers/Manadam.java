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

import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import javolution.util.FastList;

/**
 * Class handling the Mana damage skill
 *
 * @author slyce
 */
public class Manadam implements ISkillHandler {

    private static final SkillType[] SKILL_IDS = {SkillType.MANADAM};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        L2Character target = null;

        if (activeChar.isAlikeDead()) {
            return;
        }

        boolean ss = false;
        boolean bss = false;
        switch (rechargeWeapon(activeChar.getActiveWeaponInstance())) {
            case 1:
                bss = true;
                break;
            case 2:
                ss = true;
                break;
        }

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            target = (L2Character) n.getValue();

            boolean acted = Formulas.calcMagicAffected(activeChar, target, skill);
            if (target.isInvul() || !acted) {
                activeChar.sendPacket(Static.MISSED_TARGET);
            } else {
                double damage = Formulas.calcManaDam(activeChar, target, skill, ss, bss);
                if (damage > target.getCurrentMp()) {
                    damage = target.getCurrentMp();
                }

                target.reduceCurrentMp(damage);

                if (damage > 0 && target.isSleeping()) {
                    target.stopSleeping(null);
                }

                target.sendPacket(SystemMessage.id(SystemMessageId.MP_WAS_REDUCED_BY_S1).addNumber((int) damage));
                if (activeChar.isPlayer()) {
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1).addNumber((int) damage));
                }
            }
        }
        //targets.clear();
    }

    private int rechargeWeapon(L2ItemInstance weaponInst) {
        if (weaponInst == null) {
            return 0;
        }
        if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) {
            weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            return 1;
        } else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT) {
            weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            return 2;
        }
        return 0;
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
