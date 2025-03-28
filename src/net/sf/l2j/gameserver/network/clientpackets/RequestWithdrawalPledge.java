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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestWithdrawalPledge extends L2GameClientPacket {

    @Override
    protected void readImpl() {
        // trigger
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (player.getClan() == null) {
            player.sendPacket(Static.YOU_ARE_NOT_A_CLAN_MEMBER);
            return;
        }

        if (player.isClanLeader()) {
            player.sendPacket(Static.CLAN_LEADER_CANNOT_WITHDRAW);
            return;
        }

        if (player.isInCombat() || player.underAttack() || player.getPvpFlag() > 0) {
            player.sendPacket(Static.YOU_CANNOT_LEAVE_DURING_COMBAT);
            return;
        }

        Castle castle = CastleManager.getInstance().getCastle(player);
        if (castle != null && castle.getSiege().getIsInProgress()) {
            player.sendPacket(Static.YOU_CANNOT_LEAVE_DURING_COMBAT);
            return;
        }

        L2Clan clan = player.getClan();

        clan.removeClanMember(player.getName(), System.currentTimeMillis() + Config.ALT_CLAN_JOIN_DAYS * 86400000L); //24*60*60*1000 = 86400000

        clan.broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.S1_HAS_WITHDRAWN_FROM_THE_CLAN).addString(player.getName()));

        // Remove the Player From the Member list
        clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(player.getName()));

        player.sendPacket(Static.YOU_HAVE_WITHDRAWN_FROM_CLAN);
        player.sendPacket(Static.YOU_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
    }
}
