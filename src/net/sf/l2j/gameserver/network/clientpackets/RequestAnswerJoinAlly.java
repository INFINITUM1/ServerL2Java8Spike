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

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

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
public final class RequestAnswerJoinAlly extends L2GameClientPacket
{
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
			if(System.currentTimeMillis() - player.gCPAS() < 400)
				return;
			player.sCPAS();

			L2PcInstance requestor = player.getTransactionRequester();
			
			player.setTransactionRequester(null);
			
			if (requestor == null)
				return;
				
			requestor.setTransactionRequester(null);

			if(player.getTransactionType() != TransactionType.ALLY || player.getTransactionType() != requestor.getTransactionType())
				return;
			
			if(_response == 1)
			{
				L2Clan clan = requestor.getClan();
				// we must double check this cause of hack
				if (clan.checkAllyJoinCondition(requestor, player))
				{
					player.sendPacket(Static.FAILED_TO_INVITE_CLAN_IN_ALLIANCE);

					player.getClan().setAllyId(clan.getAllyId());
					player.getClan().setAllyName(clan.getAllyName());
					player.getClan().setAllyPenaltyExpiryTime(0, 0);
					player.getClan().updateClanInDB();
				}
			}
			else
				requestor.sendPacket(Static.FAILED_TO_INVITE_CLAN_IN_ALLIANCE);
				
			requestor.setTransactionType(TransactionType.NONE);
			player.setTransactionType(TransactionType.NONE);
		}
	}
}
