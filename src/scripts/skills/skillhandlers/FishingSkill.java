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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Fishing;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Weapon;
import javolution.util.FastList;

public class FishingSkill implements ISkillHandler {
    //private static Logger _log = Logger.getLogger(SiegeFlag.class.getName());

    private static final SkillType[] SKILL_IDS = {SkillType.PUMPING, SkillType.REELING};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (activeChar == null || !(activeChar.isPlayer())) {
            return;
        }

        L2PcInstance player = (L2PcInstance) activeChar;

        L2Fishing fish = player.GetFishCombat();
        if (fish == null) {
            if (skill.getSkillType() == SkillType.PUMPING) {
                //Pumping skill is available only while fishing
                player.sendPacket(Static.CAN_USE_PUMPING_ONLY_WHILE_FISHING);
            } else if (skill.getSkillType() == SkillType.REELING) {
                //Reeling skill is available only while fishing
                player.sendPacket(Static.CAN_USE_REELING_ONLY_WHILE_FISHING);
            }
            player.sendPacket(new ActionFailed());
            return;
        }
        L2Weapon weaponItem = player.getActiveWeaponItem();
        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
        if (weaponInst == null || weaponItem == null) {
            return;
        }
        int SS = 1;
        int pen = 0;
        if (weaponInst != null && weaponInst.getChargedFishshot()) {
            SS = 2;
        }
        double gradebonus = 1 + weaponItem.getCrystalType() * 0.1;
        int dmg = (int) (skill.getPower() * gradebonus * SS);
        if (player.getSkillLevel(1315) <= skill.getLevel() - 2) //1315 - Fish Expertise
        {//Penalty
            player.sendPacket(Static.REELING_PUMPING_3_LEVELS_HIGHER_THAN_FISHING_PENALTY);
            pen = 50;
            int penatlydmg = dmg - pen;
            if (player.isGM()) {
                player.sendMessage("Dmg w/o penalty = " + dmg);
            }
            dmg = penatlydmg;
        }
        if (SS > 1) {
            weaponInst.setChargedFishshot(false);
        }
        if (skill.getSkillType() == SkillType.REELING)//Realing
        {
            fish.useRealing(dmg, pen);
        } else//Pumping
        {
            fish.usePomping(dmg, pen);
        }
        //targets.clear();
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
