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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.util.Broadcast;
import scripts.commands.IUserCommandHandler;

/**
 *
 *
 */
public class Escape implements IUserCommandHandler {

    private static final int[] COMMAND_IDS = {52};

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#useUserCommand(int, net.sf.l2j.gameserver.model.L2PcInstance)
     */
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        // Thanks nbd
        if (!TvTEvent.onEscapeUse(activeChar.getName())) {
            activeChar.sendActionFailed();
            return false;
        }

        if (activeChar.isCastingNow()
                || activeChar.isMovementDisabled()
                || activeChar.isMuted()
                || activeChar.isAlikeDead()
                || activeChar.isInOlympiadMode()
                || activeChar.isEventWait()
                || activeChar.getChannel() > 1) {
            activeChar.sendActionFailed();
            return false;
        }

        // Check to see if the player is in a festival.
        if (activeChar.isFestivalParticipant()) {
            activeChar.sendMessage("You may not use an escape command in a festival.");
            return false;
        }

        // Check to see if player is in jail
        if (activeChar.isInJail()) {
            activeChar.sendMessage("You can not escape from jail.");
            return false;
        }

        L2Skill skill = SkillTable.getInstance().getInfo(2099, 1);

        if (skill.checkCondition(activeChar, activeChar, false)) {
            activeChar.useMagic(skill, false, false);
        } else {
            activeChar.sendActionFailed();
        }

        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}
