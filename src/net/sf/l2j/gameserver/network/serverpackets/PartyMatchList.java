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
package net.sf.l2j.gameserver.network.serverpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager;
import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager.WaitingRoom;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 *
 * sample
 * af
 * 02 00 00 00   count
 *
 * 71 b3 70 4b  object id
 * 44 00 79 00 66 00 65 00 72 00 00 00   name
 * 14 00 00 00  level
 * 0f 00 00 00  class id
 * 00 00 00 00  sex ??
 * 00 00 00 00  clan id
 * 02 00 00 00  ??
 * 6f 5f 00 00  x
 * af a9 00 00  y
 * f7 f1 ff ff  z
 *
 *
 * c1 9c c0 4b object id
 * 43 00 6a 00 6a 00 6a 00 6a 00 6f 00 6e 00 00 00
 * 0b 00 00 00  level
 * 12 00 00 00  class id
 * 00 00 00 00  sex ??
 * b1 01 00 00  clan id
 * 00 00 00 00
 * 13 af 00 00
 * 38 b8 00 00
 * 4d f4 ff ff
 * *
 * format   d (dSdddddddd)
 *
 * @version $Revision: 1.1.2.1.2.4 $ $Date: 2005/03/27 15:29:57 $
 */
public class PartyMatchList extends L2GameServerPacket
{
	private L2PcInstance _player;
	private ConcurrentLinkedQueue<WaitingRoom> _rooms;
	private int _size;
	
	public PartyMatchList(L2PcInstance player, int unk1, int territoryId, int levelType, int unk4, String unk5)
	{
		_player = player;
		if (levelType == 0)
			levelType = player.getLevel();
		else
			levelType = -1;
		_rooms = PartyWaitingRoomManager.getInstance().getRooms(levelType, territoryId, new ConcurrentLinkedQueue<WaitingRoom>());
		_size = _rooms.size();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x96);
		writeD(_size > 0 ? 1 : 0);
		writeD(_size);
		
		for(WaitingRoom room : _rooms)
		{
			if (room == null)
				continue;
			
			writeD(room.id);
			writeS(room.title);
			writeD(room.location);
			writeD(room.minLvl);
			writeD(room.maxLvl);
			writeD(room.players.size());
			writeD(room.maxPlayers);
			writeS(room.leaderName);
		}
	}
}
