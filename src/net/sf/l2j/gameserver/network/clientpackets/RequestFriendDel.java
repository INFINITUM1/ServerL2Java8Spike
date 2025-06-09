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
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestFriendDel extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestFriendDel.class.getName());

	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
        if (player == null)
            return;
			
		int charId = player.getObjectId();

		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
		    L2PcInstance friend = L2World.getInstance().getPlayer(_name);
			int objectId = 0;
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
				
		    if (friend != null)
			{	
    			if (player.haveFriend(friend.getObjectId()))
				{
					objectId = friend.getObjectId();
				}
			}
			else
            {
    			statement = con.prepareStatement("SELECT friend_id FROM character_friends, characters WHERE char_id=? AND friend_id=obj_Id AND char_name=?");
    			statement.setInt(1, charId);
    			statement.setString(2, _name);
    			rset = statement.executeQuery();
    			if (rset.next())
                {
    				objectId = rset.getInt("friend_id");
    			}
				Close.SR(statement, rset);
		    }
			
    		if (objectId == 0)
            {
    			// Player is not in your friendlist
    			player.sendPacket(SystemMessage.id(SystemMessageId.S1_NOT_ON_YOUR_FRIENDS_LIST).addString(_name));
    			return;
    		}
			con.setAutoCommit(false);
			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? AND friend_id=?");
			statement.setInt(1, charId);
			statement.setInt(2, objectId);
			statement.addBatch();
			//udalaemsa sami
			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? AND friend_id=?");
			statement.setInt(1, objectId);
			statement.setInt(2, charId);
			statement.addBatch();
			statement.executeBatch();
			con.commit();
			
			// Player deleted from your friendlist
			player.sendPacket(SystemMessage.id(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST).addString(_name));
			
			player.deleteFriend(objectId);
			player.sendPacket(new FriendList(player));
			if (friend != null)
			{
				friend.deleteFriend(charId);
				friend.sendPacket(new FriendList(friend));
			}
		}
		catch (Exception e)
		{
		    _log.log(Level.WARNING, "could not del friend objectid: ", e);
		}
		finally
		{
			Close.CSR(con, statement, rset);
		}

	}
}

