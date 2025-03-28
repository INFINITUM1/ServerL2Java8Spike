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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestSetPledgeCrest extends L2GameClientPacket {

    static final Logger _log = Logger.getLogger(RequestSetPledgeCrest.class.getName());
    private int _length;
    private byte[] _data;

    @Override
    protected void readImpl() {
        _length = readD();
        if (_length < 0 || _length > 256) {
            return;
        }

        _data = new byte[_length];
        readB(_data);
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();

        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPAA() < 1000) {
            return;
        }

        player.sCPAA();

        L2Clan clan = player.getClan();
        if (clan == null) {
            return;
        }

        if (clan.getDissolvingExpiryTime() > System.currentTimeMillis()) {
            player.sendPacket(Static.CANNOT_SET_CREST_WHILE_DISSOLUTION_IN_PROGRESS);
            return;
        }

        if (_length < 0) {
            player.sendPacket(Static.FILE_TRANSFER_ERROR);
            return;
        }
        if (_length > 256) {
            player.sendPacket(Static.CLAN_CREST_256);
            return;
        }
        if (_length == 0 || _data.length == 0) {
            CrestCache.getInstance().removePledgeCrest(clan.getCrestId());

            clan.setHasCrest(false);
            player.sendPacket(Static.CLAN_CREST_HAS_BEEN_DELETED);

            for (L2PcInstance member : clan.getOnlineMembers("")) {
                member.broadcastUserInfo();
            }

            return;
        }


        if ((player.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST) {
            if (clan.getLevel() < 3) {
                player.sendPacket(Static.CLAN_LVL_3_NEEDED_TO_SET_CREST);
                return;
            }

            CrestCache crestCache = CrestCache.getInstance();

            int newId = IdFactory.getInstance().getNextId();

            if (clan.hasCrest()) {
                crestCache.removePledgeCrest(newId);
            }

            if (!crestCache.savePledgeCrest(newId, _data)) {
                _log.log(Level.INFO, "Error loading crest of clan:" + clan.getName());
                return;
            }

            Connect con = null;
            PreparedStatement statement = null;
            try {
                con = L2DatabaseFactory.get();
                statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
                statement.setInt(1, newId);
                statement.setInt(2, clan.getClanId());
                statement.executeUpdate();
            } catch (SQLException e) {
                _log.warning("could not update the crest id:" + e.getMessage());
            } finally {
                Close.CS(con, statement);
            }

            clan.setCrestId(newId);
            clan.setHasCrest(true);

            for (L2PcInstance member : clan.getOnlineMembers("")) {
                member.broadcastUserInfo();
            }
        }
    }
}
