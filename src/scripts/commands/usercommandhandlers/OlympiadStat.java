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
package scripts.commands.usercommandhandlers;

import scripts.commands.IUserCommandHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Support for /olympiadstat command Added by kamy
 */
public class OlympiadStat implements IUserCommandHandler {

    private static final int[] COMMAND_IDS = {109};

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#useUserCommand(int, net.sf.l2j.gameserver.model.L2PcInstance)
     */
    @Override
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        if (id != COMMAND_IDS[0]) {
            return false;
        }

        /*if (!activeChar.isNoble()) {
         activeChar.sendPacket(Static.THIS_COMMAND_CAN_ONLY_BE_USED_BY_A_NOBLESSE);
         } else {
         activeChar.sendPacket(SystemMessage.id(SystemMessageId.THE_PRESENT_RECORD_DURING_THE_CURRENT_OLYMPIAD_SESSION_IS_S1_WINS_S2_DEFEATS_YOU_HAVE_EARNED_S3_OLYMPIAD_POINTS).addNumber(Olympiad.getCompetitionWin(activeChar.getObjectId())).addNumber(Olympiad.getCompetitionLoose(activeChar.getObjectId())).addNumber(Olympiad.getNoblePoints(activeChar.getObjectId())));
         }*/
        if (activeChar.isNoble()) {
            activeChar.sendPacket(Static.THIS_COMMAND_CAN_ONLY_BE_USED_BY_A_NOBLESSE);
            return true;
        }

        SystemMessage sm = new SystemMessage(SystemMessageId.THE_PRESENT_RECORD_DURING_THE_CURRENT_OLYMPIAD_SESSION_IS_S1_WINS_S2_DEFEATS_YOU_HAVE_EARNED_S3_OLYMPIAD_POINTS);
        sm.addNumber(Olympiad.getCompetitionDone(activeChar.getObjectId()));
        sm.addNumber(Olympiad.getCompetitionWin(activeChar.getObjectId()));
        sm.addNumber(Olympiad.getCompetitionLoose(activeChar.getObjectId()));
        sm.addNumber(Olympiad.getNoblePoints(activeChar.getObjectId()));

        activeChar.sendPacket(sm);

        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}
