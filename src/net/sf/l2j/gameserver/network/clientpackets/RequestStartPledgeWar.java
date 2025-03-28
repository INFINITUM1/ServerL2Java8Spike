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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestStartPledgeWar extends L2GameClientPacket
{
    private String _pledgeName;
    private L2Clan _clan;
    private L2PcInstance player;

    @Override
	protected void readImpl()
    {
        _pledgeName = readS();
    }

    @Override
	protected void runImpl()
    {
        player = getClient().getActiveChar();
        if (player == null) return;

        _clan = getClient().getActiveChar().getClan();
        if (_clan == null) return;

        if (_clan.getLevel() < 3 || _clan.getMembersCount() < Config.ALT_CLAN_MEMBERS_FOR_WAR)
        {
            player.sendPacket(Static.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
            player.sendActionFailed();
            return;
        }
        else if (!player.isClanLeader())
        {
            player.sendPacket(Static.WAR_NOT_LEADER);
            player.sendActionFailed();
            return;
        }

        L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
        if (clan == null)
        {
            player.sendPacket(Static.CLAN_WAR_CANNOT_DECLARED_CLAN_NOT_EXIST);
            player.sendActionFailed();
            return;
        }
        else if (_clan.getAllyId() == clan.getAllyId() && _clan.getAllyId() != 0)
        {
            player.sendPacket(Static.CLAN_WAR_AGAINST_A_ALLIED_CLAN_NOT_WORK);
            player.sendActionFailed();
            return;
        }
        //else if(clan.getLevel() < 3)
        else if (clan.getLevel() < 3 || clan.getMembersCount() < Config.ALT_CLAN_MEMBERS_FOR_WAR)
        {
            player.sendPacket(Static.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
            player.sendActionFailed();
            return;
        }
        else if (_clan.isAtWarWith(clan.getClanId()))
        {
            player.sendActionFailed();
            player.sendPacket(SystemMessage.id(SystemMessageId.ALREADY_AT_WAR_WITH_S1_WAIT_5_DAYS).addString(clan.getName()));
            return;
        }

        //_log.warning("RequestStartPledgeWar, leader: " + clan.getLeaderName() + " clan: "+ _clan.getName());

        //        L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName());

        //        if(leader == null)
        //            return;

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

        //        if (leader.isTransactionInProgress())
        //        {
        //            SystemMessage sm = SystemMessage.id(SystemMessage.S1_IS_BUSY_TRY_LATER);
        //            sm.addString(leader.getName());
        //            player.sendPacket(sm);
        //            return;
        //        }

        //        leader.setTransactionRequester(player);
        //        player.setTransactionRequester(leader);
        //        leader.sendPacket(new StartPledgeWar(_clan.getName(),player.getName()));

        ClanTable.getInstance().storeclanswars(player.getClanId(), clan.getClanId());
        for (L2PcInstance cha : L2World.getInstance().getAllPlayers()) {
        	if (cha.getClan() == player.getClan() || cha.getClan() == clan)
        		cha.broadcastUserInfo();
        }
    }

    @Override
	public String getType()
    {
        return "C.StartPledgewar";
    }
}
