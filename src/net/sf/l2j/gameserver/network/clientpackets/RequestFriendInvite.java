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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinFriend;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestFriendInvite extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestFriendInvite.class.getName());

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
			
		if(player.isTransactionInProgress())
		{
			player.sendPacket(Static.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
			
		if(System.currentTimeMillis() - player.gCPBB() < 100)
			return;
		player.sCPBB();

        L2PcInstance friend = L2World.getInstance().getPlayer(_name);
        //_name = Util.capitalizeFirst(_name); //FIXME: is it right to capitalize a nickname?

    	if (friend == null)
        {
			if (player.getTargetId() != -1)
				friend = L2World.getInstance().getPlayer(player.getTargetId());
					
			if (friend == null)
			{
				//Target is not found in the game.
				player.sendPacket(Static.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
				return;
			}
    	}
		_name = Util.capitalizeFirst(friend.getName());
		
        if (friend == player)
        {
    	    //You cannot add yourself to your own friend list.
    	    player.sendPacket(Static.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST);
    	    return;
    	}
		
		if (friend.isAlone() || (friend.isGM() && friend.getMessageRefusal()))
		{
			player.sendMessage("Игрок просил его не беспокоить");
			player.sendActionFailed();
			return;
		}
		
    	if (player.haveFriend(friend.getObjectId()))
		{
    	    player.sendPacket(SystemMessage.id(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST).addString(_name));
    	    return;
		}

		SystemMessage sm;
		if (!friend.isTransactionInProgress())
		{
			friend.setTransactionRequester(player, System.currentTimeMillis() + 10000);
			friend.setTransactionType(TransactionType.FRIEND);
			player.setTransactionRequester(friend, System.currentTimeMillis() + 10000);
			player.setTransactionType(TransactionType.FRIEND);

			friend.sendPacket(new AskJoinFriend(player.getName()));
    		sm = SystemMessage.id(SystemMessageId.S1_REQUESTED_TO_BECOME_FRIENDS).addString(player.getName());
    	}
        else
			sm = SystemMessage.id(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(friend.getName());

		friend.sendPacket(sm);
		sm = null;
	}
}