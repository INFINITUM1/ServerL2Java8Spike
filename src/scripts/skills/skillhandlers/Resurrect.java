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
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.5.2.4 $ $Date: 2005/04/03 15:55:03 $
 */
public class Resurrect implements ISkillHandler {
    //private static Logger _log = Logger.getLogger(Resurrect.class.getName());

    private static final SkillType[] SKILL_IDS = {SkillType.RESURRECT};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        L2PcInstance player = null;
        if (activeChar.isPlayer()) {
            player = (L2PcInstance) activeChar;
        }

        L2Character target = null;
        L2PcInstance targetPlayer;
        FastList<L2Character> targetToRes = new FastList<L2Character>();

        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            target = (L2Character) n.getValue();

            if (target.isPlayer()) {
                targetPlayer = (L2PcInstance) target;

                // Check for same party or for same clan, if target is for clan.
                if (skill.getTargetType() == SkillTargetType.TARGET_CORPSE_CLAN) {
                    if (player.getClanId() != targetPlayer.getClanId()) {
                        continue;
                    }
                }

                if (Config.PVP_ZONE_REWARDS && ZoneManager.getInstance().isResDisabled(player)) {
                    continue;
                }
            }
            if (target.isVisible()) {
                targetToRes.add(target);
            }
        }

        if (targetToRes.size() == 0) {
            activeChar.abortCast();
        }

        for (FastList.Node<L2Character> n = targetToRes.head(), end = targetToRes.tail(); (n = n.getNext()) != end;) {
            L2Character cha = (L2Character) n.getValue();
            if (activeChar.isPlayer()) {
                if (cha.isPlayer()) {
                    ((L2PcInstance) cha).reviveRequest((L2PcInstance) activeChar, skill, false);
                } else if (cha.isPet()) {
                    if (((L2PetInstance) cha).getOwner() == activeChar) {
                        cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
                    } else {
                        ((L2PetInstance) cha).getOwner().reviveRequest((L2PcInstance) activeChar, skill, true);
                    }
                } else {
                    cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
                }
            } else {
                DecayTaskManager.getInstance().cancelDecayTask(cha);
                cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
            }
        }
        //targets.clear();
        targetToRes.clear();
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
