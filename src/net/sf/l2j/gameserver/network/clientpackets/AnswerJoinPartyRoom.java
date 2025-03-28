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

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager;
import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager.WaitingRoom;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
/**
 * Format: (ch) d
 * @author  -Wooden-
 *
 */
public final class AnswerJoinPartyRoom extends L2GameClientPacket
{
	private int _response;

    @Override
	protected void readImpl()
    {
		_response = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    protected void runImpl()
    {
		L2PcInstance player = getClient().getActiveChar();
        if (player == null) 
			return;

        L2PcInstance partner = player.getTransactionRequester();
		
        if (partner == null || partner.getTransactionRequester() == null)
        {
            // Trade partner not found, cancel trade
			if(_response != 0)
				player.sendPacket(Static.TARGET_IS_NOT_FOUND_IN_THE_GAME);

			player.setTransactionRequester(null);
			player.setTransactionType(TransactionType.NONE);
			return;
        }
		
		WaitingRoom rRoom = partner.getPartyRoom();
		if (rRoom == null)
			return;
		
		if (!rRoom.owner.equals(partner))
			return;

		if(player.getTransactionType() != TransactionType.ROOM || player.getTransactionType() != partner.getTransactionType())
			return;
		
		if (_response == 1) 
	        PartyWaitingRoomManager.getInstance().joinRoom(player, rRoom.id);
		else
			partner.sendPacket(Static.PLAYER_DECLINED);
		
		partner.setTransactionRequester(null);
		player.setTransactionRequester(null);
		partner.setTransactionType(TransactionType.NONE);
		player.setTransactionType(TransactionType.NONE);
    }
}