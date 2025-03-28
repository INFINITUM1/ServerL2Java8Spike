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
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchList;

public final class RequestOustFromPartyRoom extends L2GameClientPacket
{
	private int _charId;

	@Override
	protected void readImpl()
	{
		_charId = readD();
	}

	@Override
	protected void runImpl()
	{
        L2PcInstance player = getClient().getActiveChar();

		if (player == null)
		    return;
			
        L2PcInstance target = L2World.getInstance().getPlayer(_charId);
        if (target == null || target.equals(player))
        {
			player.sendPacket(Static.TARGET_IS_INCORRECT);
			return;
		}
		
		WaitingRoom rRoom = player.getPartyRoom();
		if (rRoom == null)
			return;
		
		if (!rRoom.owner.equals(player))
			return;
		
		WaitingRoom tRoom = target.getPartyRoom();
		if (tRoom == null)
			return;
		
		if (tRoom.id != rRoom.id)
			return;
		
		PartyWaitingRoomManager.getInstance().exitRoom(target, rRoom);
		target.sendPacket(new PartyMatchList(player, 0, -1, 0, 0, ""));
		PartyWaitingRoomManager.getInstance().registerPlayer(target);
		target.setLFP(true);
		target.broadcastUserInfo();
	}
}
