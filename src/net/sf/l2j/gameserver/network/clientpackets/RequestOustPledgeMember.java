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
package net.sf.l2j.gameserver.network.clientpackets;

//import java.util.logging.Logger;
import java.nio.BufferUnderflowException;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestOustPledgeMember extends L2GameClientPacket {

    private String _target;

    @Override
    protected void readImpl() {
        try {
            _target = readS();
        } catch (BufferUnderflowException e) {
            _target = "n-no";
        }
    }

    @Override
    protected void runImpl() {
        if (_target.equalsIgnoreCase("n-no")) {
            return;
        }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.getCPD() < 300) {
            return;
        }

        if (player.isOutOfControl()) {
            return;
        }

        player.setCPD();

        if (player.getClan() == null) {
            player.sendPacket(Static.YOU_ARE_NOT_A_CLAN_MEMBER);
            return;
        }
        if ((player.getClanPrivileges() & L2Clan.CP_CL_DISMISS) != L2Clan.CP_CL_DISMISS) {
            player.sendPacket(Static.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
        }
        if (player.getName().equalsIgnoreCase(_target)) {
            player.sendPacket(Static.YOU_CANNOT_DISMISS_YOURSELF);
            return;
        }

        L2Clan clan = player.getClan();

        L2ClanMember member = clan.getClanMember(_target);
        if (member == null) {
            //_log.warning("Target ("+_target+") is not member of the clan");
            return;
        }
        if (member.isOnline() && member.getPlayerInstance().isInCombat()) {
            player.sendPacket(Static.CLAN_MEMBER_CANNOT_BE_DISMISSED_DURING_COMBAT);
            return;
        }

        // this also updates the database
        clan.removeClanMember(_target, System.currentTimeMillis() + Config.ALT_CLAN_JOIN_DAYS * 86400000L); //24*60*60*1000 = 86400000
        clan.setCharPenaltyExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_JOIN_DAYS * 86400000L); //24*60*60*1000 = 86400000
        clan.updateClanInDB();

        clan.broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.CLAN_MEMBER_S1_EXPELLED).addString(member.getName()));
        player.sendPacket(Static.YOU_HAVE_SUCCEEDED_IN_EXPELLING_CLAN_MEMBER);
        player.sendPacket(Static.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER);

        // Remove the Player From the Member list
        clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(_target));

        if (member.isOnline()) {
            member.getPlayerInstance().sendPacket(Static.CLAN_MEMBERSHIP_TERMINATED);
        }
    }
}
