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

import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;

/**
 * @author Maktakien
 *
 */
public class MoveToLocationInVehicle extends L2GameServerPacket {

    private int char_obj_id;
    private int boat_obj_id;
    private L2CharPosition _destination;
    private L2CharPosition _origin;
    private boolean can_writeimpl = false;

    /**
     * @param actor
     * @param destination
     * @param origin
     */
    public MoveToLocationInVehicle(L2Character actor, L2CharPosition destination, L2CharPosition origin) {
        if (actor == null) {
            return;
        }

        if (!(actor.isPlayer())) {
            return;
        }

        L2PcInstance _char = actor.getPlayer();
        L2BoatInstance _boat = _char.getBoat();
        if (_boat == null) {
            return;
        }

        char_obj_id = _char.getObjectId();
        boat_obj_id = _boat.getObjectId();
        _destination = destination;
        _origin = origin;
        can_writeimpl = true;
    }

    @Override
    protected void writeImpl() {
        if (!can_writeimpl) {
            return;
        }

        writeC(0x71);
        writeD(char_obj_id);
        writeD(boat_obj_id);
        writeD(_destination.x);
        writeD(_destination.y);
        writeD(_destination.z);
        writeD(_origin.x);
        writeD(_origin.y);
        writeD(_origin.z);
    }
}
