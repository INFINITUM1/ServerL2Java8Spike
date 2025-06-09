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

import net.sf.l2j.gameserver.model.L2CommandChannel;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;

/**
 * @author -Wooden-
 *
 */
public final class RequestExAcceptJoinMPCC extends L2GameClientPacket
{
	private int _response;

	/**
	 * @param buf
	 * @param client
	 */
	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
        if(player != null)
        {
	    	L2PcInstance requestor = player.getTransactionRequester();
			
			player.setTransactionRequester(null);
			
			if (requestor == null)
			    return;
				
			requestor.setTransactionRequester(null);
			
			if(player.getTransactionType() != TransactionType.CHANNEL || player.getTransactionType() != requestor.getTransactionType())
				return;
				
			if(!requestor.isInParty() || !player.isInParty() || (requestor.getParty().isInCommandChannel() && !requestor.getParty().getCommandChannel().getChannelLeader().equals(requestor)))// || !player.getParty().isInCommandChannel())
			{
				requestor.sendMessage("Никакой пользователь не был приглашен в канал команды");
				player.sendMessage("Вы не можете присоединиться к Командному Каналу");
				return;
			}
			
			if(_response == 1)
			{
				if(player.isTeleporting())
				{
					player.sendMessage("Вы не можете присоединиться к Командному Каналу телепортируясь");
					requestor.sendMessage("Никакой пользователь не был приглашен в канал команды");
					return;
				}
			
				if(!requestor.getParty().isInCommandChannel())
				{
					new L2CommandChannel(requestor); // Create new CC
				}
				requestor.getParty().getCommandChannel().addParty(player.getParty());	
			}
			else
				requestor.sendMessage(player.getName()+" отклонил приглашение в канал.");
        }
	}
}
