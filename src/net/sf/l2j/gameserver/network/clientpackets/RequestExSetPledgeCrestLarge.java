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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

/**
 * Format : chdb
 * c (id) 0xD0
 * h (subid) 0x11
 * d data size
 * b raw data (picture i think ;) )
 * @author -Wooden-
 *
 */
public final class RequestExSetPledgeCrestLarge extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(RequestExSetPledgeCrestLarge.class.getName());
	private int _size;
	private byte[] _data;

	@Override
	protected void readImpl()
	{
		_size = readD();
		if(_size > 2176)
			return;
		if(_size > 0) // client CAN send a RequestExSetPledgeCrestLarge with the size set to 0 then format is just chd
		{
			_data = new byte[_size];
			readB(_data);
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected
	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null) return;

		L2Clan clan = player.getClan();
		if (clan == null) return;

		if (_data == null)
		{
			CrestCache.getInstance().removePledgeCrestLarge(clan.getCrestId());

            clan.setHasCrestLarge(false);
            player.sendMessage("The insignia has been removed.");

            for (L2PcInstance member : clan.getOnlineMembers(""))
                member.broadcastUserInfo();

            return;
		}

		if (_size > 2176)
        {
        	player.sendMessage("The insignia file size is greater than 2176 bytes.");
        	return;
        }

		if ((player.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{
			if(clan.getHasCastle() == 0 && clan.getHasHideout() == 0)
			{
				player.sendMessage("Only a clan that owns a clan hall or a castle can get their emblem displayed on clan related items"); //there is a system message for that but didnt found the id
				return;
			}

			CrestCache crestCache = CrestCache.getInstance();

			int newId = IdFactory.getInstance().getNextId();

            if (!crestCache.savePledgeCrestLarge(newId,_data))
            {
                _log.log(Level.INFO, "Error loading large crest of clan:" + clan.getName());
                return;
            }

            if (clan.hasCrestLarge())
            {
                crestCache.removePledgeCrestLarge(clan.getCrestLargeId());
            }

            Connect con = null;
			PreparedStatement statement = null;
            try
            {
                con = L2DatabaseFactory.get();
                statement = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
                statement.setInt(1, newId);
                statement.setInt(2, clan.getClanId());
                statement.executeUpdate();
            }
            catch (SQLException e)
            {
                _log.warning("could not update the large crest id:"+e.getMessage());
            }
            finally
            {
                Close.CS(con,statement);
            }

            clan.setCrestLargeId(newId);
            clan.setHasCrestLarge(true);

            player.sendPacket(Static.CLAN_EMBLEM_WAS_SUCCESSFULLY_REGISTERED);

            for (L2PcInstance member : clan.getOnlineMembers(""))
                member.broadcastUserInfo();

		}
	}
}