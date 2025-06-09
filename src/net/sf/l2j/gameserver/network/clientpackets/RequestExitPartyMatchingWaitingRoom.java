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

import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager;
import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager.WaitingRoom;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
/**
 * Format: (ch)
 * this is just a trigger : no data Выход
 * @author  -Wooden-
 *
 */
public final class RequestExitPartyMatchingWaitingRoom extends L2GameClientPacket
{
    @Override
	protected void readImpl()
    {
    	//trigger
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
	protected void runImpl()
    {
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;
		
		WaitingRoom room = player.getPartyRoom();
		if (room != null)
			PartyWaitingRoomManager.getInstance().exitRoom(player, room);
		
		PartyWaitingRoomManager.getInstance().delPlayer(player);
		player.setLFP(false);
		player.broadcastUserInfo();
    }
}