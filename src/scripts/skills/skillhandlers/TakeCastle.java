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
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2ArtefactInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import javolution.util.FastList;

/**
 * @author _drunk_
 *
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class TakeCastle implements ISkillHandler {
    //private static Logger _log = Logger.getLogger(TakeCastle.class.getName());

    private static final SkillType[] SKILL_IDS = {SkillType.TAKECASTLE};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (activeChar == null || !(activeChar.isPlayer())) {
            return;
        }

        L2PcInstance player = activeChar.getPlayer();
        if (player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId()) {
            return;
        }

        Castle castle = CastleManager.getInstance().getCastle(player);
        if (castle == null || !checkIfOkToCastSealOfRule(player, castle, false)) {
            return;
        }

        try {
            if (player.getTarget().isL2Artefact()) {
                castle.Engrave(player.getClan(), player.getTarget().getObjectId());
            }
        } catch (Exception e) {
        }
        //targets.clear();
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }

    /**
     * Return true if character clan place a flag<BR><BR>
     *
     * @param activeChar The L2Character of the character placing the flag
     *
     */
    public static boolean checkIfOkToCastSealOfRule(L2Character activeChar, boolean isCheckOnly) {
        return checkIfOkToCastSealOfRule(activeChar, CastleManager.getInstance().getCastle(activeChar), isCheckOnly);
    }

    public static boolean checkIfOkToCastSealOfRule(L2Character activeChar, Castle castle, boolean isCheckOnly) {
        if (activeChar == null || !(activeChar.isPlayer())) {
            return false;
        }

        SystemMessage sm = SystemMessage.id(SystemMessageId.S1_S2);
        L2PcInstance player = activeChar.getPlayer();

        if (castle == null || castle.getCastleId() <= 0) {
            sm.addString("Не подходящее место для чтения печати/");
        } else if (player.getTarget() == null || !(player.getTarget().isL2Artefact())) {
            sm.addString("You can only use this skill on an artifact");
        } else if (!castle.getSiege().getIsInProgress()) {
            sm.addString("You can only use this skill during a siege.");
        } else if (castle.getSiege().getAttackerClan(player.getClan()) == null) {
            sm.addString("You must be an attacker to use this skill");
        } else if (castle.getRaidGuard() >= 1) {
            sm.addString("Вам необходимо убить Босса-защитника замка.");
        } else {
            if (!isCheckOnly) {
                castle.getSiege().announceToPlayer("Клан " + player.getClan().getName() + " начал чтение печати.", true);
            }
            return true;
        }

        if (!isCheckOnly) {
            player.sendPacket(sm);
        }
        return false;
    }
}
