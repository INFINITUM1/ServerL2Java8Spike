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

import scripts.skills.ISkillHandler;
import scripts.skills.SkillHandler;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import javolution.util.FastList;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.4 $ $Date: 2005/04/06 16:13:48 $
 */
public class Heal implements ISkillHandler {

    private static final SkillType[] SKILL_IDS = {
        SkillType.HEAL,
        SkillType.HEAL_PERCENT,
        SkillType.HEAL_STATIC
    };

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (activeChar == null || activeChar.isAlikeDead()) {
            return;
        }

        try {
            ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(SkillType.BUFF);

            if (handler != null) {
                handler.useSkill(activeChar, skill, targets);
            }
        } catch (Exception e) {
        }

        L2Character target = null;
        L2PcInstance player = null;
        L2Summon activeSummon = null;

        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

        if (activeChar.isPlayer()) {
            player = (L2PcInstance) activeChar;
        } else if (activeChar.isL2Summon()) {
            activeSummon = (L2Summon) activeChar;
        }

        double hp_mul = 1;
        boolean clearSpiritShot = false;
        if (skill.getSkillType() != SkillType.HEAL_PERCENT && skill.getId() != 4051) {
            //Added effect of SpS and Bsps
            if (weaponInst != null && weaponInst.getChargedSpiritshot() > 0) {
                switch (weaponInst.getChargedSpiritshot()) {
                    case L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT:
                        hp_mul = 1.5;
                        break;
                    case L2ItemInstance.CHARGED_SPIRITSHOT:
                        hp_mul = 1.3;
                        break;
                }
                clearSpiritShot = true;
            } // If there is no weapon equipped, check for an active summon.
            else if (activeSummon != null && activeSummon.getChargedSpiritShot() > 0) {
                switch (activeSummon.getChargedSpiritShot()) {
                    case L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT:
                        hp_mul = 1.5;
                        break;
                    case L2ItemInstance.CHARGED_SPIRITSHOT:
                        hp_mul = 1.3;
                        break;
                }
                clearSpiritShot = true;
            }
        }

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            target = (L2Character) n.getValue();

            // We should not heal if char is dead
            if (target == null || target.isDead() || target.isL2Door() || target.isSiegeRaidGuard()) {
                continue;
            }

            /*if (target.isRaid()) {
                System.out.println("##00#");
                continue;
            }*/

            if (target.isRaid()
                    && Config.FORBID_RAID_BOSS_HEAL
                    /*&& !activeChar.isL2Npc()  && player != null*/) {
                //System.out.println("##11#");
                continue;
            }

            // Player holding a cursed weapon can't be healed and can't heal
            if (target != activeChar) {
                if (target.isPlayer() && target.isCursedWeaponEquiped()) {
                    continue;
                } else if (player != null && player.isCursedWeaponEquiped()) {
                    continue;
                }
            }

            double hp = target.getCurrentHp() == target.getMaxHp() ? 0 : skill.getPower();
            if (hp > 0) {
                if (skill.getSkillType() == SkillType.HEAL_PERCENT) {
                    hp = target.getMaxHp() * hp / 100.0;
                } else {
                    hp *= hp_mul;
                }

                if (skill.getSkillType() == SkillType.HEAL_STATIC) {
                    hp = skill.getPower();
                } else if (skill.getSkillType() != SkillType.HEAL_PERCENT) {
                    hp *= target.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;
                }
                hp *= activeChar.calcStat(Stats.HEAL_BONUS, 1, null, null);

                target.setCurrentHp(target.getCurrentHp() + hp);
                target.setLastHealAmount((int) hp);
            }

            if (target.isPlayer()) {
                if (skill.getId() == 4051) {
                    target.sendPacket(Static.REJUVENATING_HP);
                } else {
                    SystemMessage sm;
                    if (activeChar.isPlayer() && activeChar != target) {
                        sm = SystemMessage.id(SystemMessageId.S2_HP_RESTORED_BY_S1).addString(activeChar.getName());
                    } else {
                        sm = SystemMessage.id(SystemMessageId.S1_HP_RESTORED);
                    }

                    sm.addNumber((int) hp);
                    target.sendPacket(sm);
                    sm = null;
                }
            }
        }

        if (clearSpiritShot) {
            if (activeSummon != null) {
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            } else if (weaponInst != null) {
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }
        }

        player = null;
        target = null;
        activeSummon = null;
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
