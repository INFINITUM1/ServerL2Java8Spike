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
package net.sf.l2j.gameserver.network.serverpackets;

import javolution.util.FastTable;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 */
public class AllyInfo extends L2GameServerPacket {

    //private static Logger _log = Logger.getLogger(AllyInfo.class.getName());
    private L2PcInstance _cha;

    public AllyInfo(L2PcInstance cha) {
        _cha = cha;
    }

    @Override
    protected final void writeImpl() {
        //======<AllyInfo>======
        _cha.sendPacket(Static.ALLIANCE_INFO_HEAD);
        //======<Ally Name>======
        _cha.sendPacket(SystemMessage.id(SystemMessageId.ALLIANCE_NAME_S1).addString(_cha.getClan().getAllyName()));
        int online = 0;
        int count = 0;
        int clancount = 0;

        FastTable<L2Clan> cn = new FastTable<L2Clan>();
        for (L2Clan clan : ClanTable.getInstance().getClans()) {
            if (clan.getAllyId() == _cha.getAllyId()) {
                clancount++;
                online += clan.getOnlineMembers("").length;
                count += clan.getMembers().length;
                cn.add(clan);
            }
        }
        //Connection
        _cha.sendPacket(SystemMessage.id(SystemMessageId.CONNECTION_S1_TOTAL_S2).addString("" + online).addString("" + count));
        L2Clan leaderclan = ClanTable.getInstance().getClan(_cha.getAllyId());
        _cha.sendPacket(SystemMessage.id(SystemMessageId.ALLIANCE_LEADER_S2_OF_S1).addString(leaderclan.getName()).addString(leaderclan.getLeaderName()));
        //clan count
        _cha.sendPacket(SystemMessage.id(SystemMessageId.ALLIANCE_CLAN_TOTAL_S1).addString("" + clancount));
        //clan information
        _cha.sendPacket(Static.CLAN_INFO_HEAD);

        for (L2Clan clan : cn) {
            if (clan == null) {
                continue;
            }

            //clan name
            _cha.sendPacket(SystemMessage.id(SystemMessageId.CLAN_INFO_NAME).addString(clan.getName()));
            //clan leader name
            _cha.sendPacket(SystemMessage.id(SystemMessageId.CLAN_INFO_LEADER).addString(clan.getLeaderName()));
            //clan level
            _cha.sendPacket(SystemMessage.id(SystemMessageId.CLAN_INFO_LEVEL).addNumber(clan.getLevel()));
            //online
            _cha.sendPacket(SystemMessage.id(SystemMessageId.CONNECTION_S1_TOTAL_S2).addString("" + clan.getOnlineMembers("").length).addString("" + clan.getMembers().length));
            //---------
            _cha.sendPacket(SystemMessage.id(SystemMessageId.CLAN_INFO_SEPARATOR));
        }
        //=========================
        _cha.sendPacket(Static.CLAN_INFO_FOOT);

        //
        cn.clear();
        cn = null;
    }
}
