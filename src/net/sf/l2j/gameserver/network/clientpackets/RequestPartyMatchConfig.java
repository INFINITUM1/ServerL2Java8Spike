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

import java.nio.BufferUnderflowException;
import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager;
import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager.WaitingRoom;
import net.sf.l2j.gameserver.instancemanager.TownManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ... После создания комнаты
 *
 * @version $Revision: 1.1.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPartyMatchConfig extends L2GameClientPacket {

    private int _unk1;
    private int _maxPlayers;
    private int _minLvl;
    private int _maxLvl;
    private int _unk5;
    private String title;
    private boolean error = false;

    @Override
    protected void readImpl() {
        try
        {
            _unk1 = readD();
            _maxPlayers = readD();
            _minLvl = readD();
            _maxLvl = readD();
            _unk5 = readD();
            title = readS();
        } catch (BufferUnderflowException e) {
            error = true;
        }
    }

    @Override
    protected void runImpl() {
        if (error) {
            return;
        }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        WaitingRoom room = player.getPartyRoom();
        if (room == null) {
            PartyWaitingRoomManager.getInstance().registerRoom(player, title, _maxPlayers, _minLvl, _maxLvl, TownManager.getInstance().getClosestLocation(player));
        } else {
            room.maxPlayers = _maxPlayers;
            room.minLvl = _minLvl;
            room.maxLvl = _maxLvl;
            room.title = title;
            room.location = TownManager.getInstance().getClosestLocation(player);
            PartyWaitingRoomManager.getInstance().refreshRoom(room);
        }

        if (player.isLFP()) {
            player.setLFP(false);
            player.broadcastUserInfo();
        }
    }
}
