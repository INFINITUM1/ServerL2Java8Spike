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
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchDetail;

/**
 * This class ...
 *
 * @version $Revision: 1.1.4.3 $ $Date: 2005/03/27 15:29:30 $
 */

public final class RequestPartyMatchDetail extends L2GameClientPacket
{
	private int _roomId; // 0 - кнопка автоприем, id комнаты - двойной клик
	private int _unk1; // -1 - кнопка автоприем, 0 - двойной клик
	private int _unk2; // 1 - кнопка автоприем, 0 - двойной клик
	private int _unk3; // 1 - кнопка автоприем, 0 - двойной клик

	@Override
	protected void readImpl()
	{
		_roomId = readD();
        _unk1 = readD();
        _unk2 = readD();
        _unk3 = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		PartyWaitingRoomManager.getInstance().joinRoom(player, _roomId);
	}
}
