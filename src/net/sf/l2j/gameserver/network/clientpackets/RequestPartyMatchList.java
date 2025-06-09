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
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchList;

/**
 * Packetformat  Rev650  cdddddS
 *
 * @version $Revision: 1.1.4.4 $ $Date: 2005/03/27 15:29:30 $ Главное окно
 */

public class RequestPartyMatchList extends L2GameClientPacket
{
	private int _unk1; // page?
	private int _unk4;
	private String _unk5;

	private int _territoryId;
	private int _levelType;

	@Override
	protected void readImpl()
	{
        _unk1 = readD();
        _territoryId = readD();
        _levelType = readD();
		if(_buf.remaining() > 0)
		{
			_unk4 = readD();
			_unk5 = readS();
		}
		else
		{
			_unk4 = 0;
			_unk5 = "none?";
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		player.sendPacket(new PartyMatchList(player, _unk1, _territoryId, _levelType, _unk4, _unk5));
		PartyWaitingRoomManager.getInstance().registerPlayer(player);
		player.setLFP(true);
		player.broadcastUserInfo();
	}
}
