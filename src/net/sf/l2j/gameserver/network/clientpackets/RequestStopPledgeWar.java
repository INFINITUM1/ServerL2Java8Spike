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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

public final class RequestStopPledgeWar extends L2GameClientPacket {

    private String _pledgeName;

    @Override
    protected void readImpl() {
        _pledgeName = readS();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }
        L2Clan playerClan = player.getClan();
        if (playerClan == null) {
            return;
        }

        L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);

        if (clan == null) {
            player.sendPacket(Static.CLAN_NOT_FOUND);
            player.sendActionFailed();
            return;
        }

        if (!playerClan.isAtWarWith(clan.getClanId())) {
            player.sendPacket(Static.NO_WAR);
            player.sendActionFailed();
            return;
        }

        // Check if player who does the request has the correct rights to do it
        if ((player.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) != L2Clan.CP_CL_PLEDGE_WAR) {
            player.sendPacket(Static.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
        }

        if (!player.isInZonePeace()) {
            player.sendMessage("Действие доступно в безопасной зоне или в городе.");
            return;
        }

        //_log.info("RequestStopPledgeWar: By leader or authorized player: " + playerClan.getLeaderName() + " of clan: "
        //	+ playerClan.getName() + " to clan: " + _pledgeName);
        //        L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName());
        //        if(leader != null && leader.isOnline() == 0)
        //        {
        //            player.sendMessage("Clan leader isn't online.");
        //            player.sendActionFailed();
        //            return;
        //        }
        //        if (leader.isProcessingRequest())
        //        {
        //            SystemMessage sm = SystemMessage.id(SystemMessage.S1_IS_BUSY_TRY_LATER);
        //            sm.addString(leader.getName());
        //            player.sendPacket(sm);
        //            return;
        //        }
        for (L2ClanMember member : playerClan.getMembers()) {
            if (member == null || member.getPlayerInstance() == null) {
                continue;
            }

            if (AttackStanceTaskManager.getInstance().getAttackStanceTask(member.getPlayerInstance())) {
                player.sendMessage("Нельзя отменить войну, когда один из членов клана " + playerClan.getName() + " находится в бою.");
                return;
            }
        }

        for (L2ClanMember member : clan.getMembers()) {
            if (member == null || member.getPlayerInstance() == null) {
                continue;
            }

            if (AttackStanceTaskManager.getInstance().getAttackStanceTask(member.getPlayerInstance())) {
                player.sendMessage("Нельзя отменить войну, когда один из членов клана " + clan.getName() + " находится в бою.");
                return;
            }
        }

        ClanTable.getInstance().deleteclanswars(playerClan.getClanId(), clan.getClanId());
        for (L2PcInstance cha : L2World.getInstance().getAllPlayers()) {
            if (cha.getClan() == player.getClan() || cha.getClan() == clan) {
                cha.broadcastUserInfo();
            }
        }
    }

    @Override
    public String getType() {
        return "C.StopPledgeWar";
    }
}
