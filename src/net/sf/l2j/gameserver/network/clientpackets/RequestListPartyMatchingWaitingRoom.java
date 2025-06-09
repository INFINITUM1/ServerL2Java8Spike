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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExListPartyMatchingWaitingRoom;
/**
 * Format: (ch)
 * this is just a trigger : no data Очередь
 * @author  -Wooden-
 * 
 */
public class RequestListPartyMatchingWaitingRoom extends L2GameClientPacket
{
	private int _unk1; // page?
	private int _minLvl;
	private int _maxLevel;
	private int _unk4;
	
    @Override
	protected void readImpl()
    {
        _unk1 = readD();
        _minLvl = readD();
        _maxLevel = readD();
        _unk4 = readD();
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
		
		player.sendPacket(new ExListPartyMatchingWaitingRoom(player, _unk1, _minLvl, _maxLevel));
    }
}