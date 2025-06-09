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
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

/**
 *  sample
 *  5F
 *  01 00 00 00
 *
 *  format  cdd
 *
 *
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestAnswerFriendInvite extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestAnswerFriendInvite.class.getName());

	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
    	if(player != null)
        {
			if(System.currentTimeMillis() - player.gCPBC() < 100)
				return;
			player.sCPBC();
			
    		L2PcInstance requestor = player.getTransactionRequester();
			
			player.setTransactionRequester(null);
			
    		if (requestor == null)
    		    return;
				
			requestor.setTransactionRequester(null);
			
			if(player.getTransactionType() != TransactionType.FRIEND || player.getTransactionType() != requestor.getTransactionType())
				return;

    		if (_response == 1)
            {
				Connect con = null;
				PreparedStatement statement = null;
        		try
        		{
        		    con = L2DatabaseFactory.get();
        		    statement = con.prepareStatement("INSERT INTO character_friends (char_id, friend_id, friend_name) VALUES (?, ?, ?), (?, ?, ?)");
                    statement.setInt(1, requestor.getObjectId());
                    statement.setInt(2, player.getObjectId());
        		    statement.setString(3, player.getName());
                    statement.setInt(4, player.getObjectId());
                    statement.setInt(5, requestor.getObjectId());
                    statement.setString(6, requestor.getName());
        		    statement.execute();
        		    Close.S(statement);
        			requestor.sendPacket(Static.YOU_HAVE_SUCCEEDED_INVITING_FRIEND);
        			//Player added to your friendlist
            		requestor.sendPacket(SystemMessage.id(SystemMessageId.S1_ADDED_TO_FRIENDS).addString(player.getName()));

        			//has joined as friend.
            		player.sendPacket(SystemMessage.id(SystemMessageId.S1_JOINED_AS_FRIEND).addString(requestor.getName()));
            		
            		//Send notificacions for both player in order to show them online
   				    player.sendPacket(new FriendList(player));
   				    requestor.sendPacket(new FriendList(requestor));
            		
					//
					player.storeFriend(requestor.getObjectId(), requestor.getName());
					requestor.storeFriend(player.getObjectId(), player.getName());
        		}
        		catch (Exception e)
        		{
        		    _log.warning("could not add friend objectid: "+ e);
        		}
        		finally
        		{
        		    Close.CS(con, statement);
        		}
    		} 
			else
    			requestor.sendPacket(Static.FAILED_TO_INVITE_A_FRIEND);
			
			requestor.setTransactionType(TransactionType.NONE);
			player.setTransactionType(TransactionType.NONE);
        }
	}
}
