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

import net.sf.l2j.gameserver.instancemanager.BoatManager;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.GetOnVehicle;
import net.sf.l2j.util.Point3D;

/**
 * This class ...
 *
 * @version $Revision: 1.1.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestGetOnVehicle extends L2GameClientPacket
{
    private int _id, _x, _y, _z;

    @Override
	protected void readImpl()
    {
        _id = readD();
        _x = readD();
        _y = readD();
        _z = readD();
    }

    @Override
	protected void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) 
			return;

		if(System.currentTimeMillis() - player.gCPAL() < 500)
			return;

		player.sCPAL();

        L2BoatInstance boat = BoatManager.getInstance().GetBoat(_id);
        if (boat == null) 
			return;

        GetOnVehicle Gon = new GetOnVehicle(player,boat,_x,_y,_z);
        player.setInBoatPosition(new Point3D(_x,_y,_z));
        player.getPosition().setXYZ(boat.getPosition().getX(),boat.getPosition().getY(),boat.getPosition().getZ());
        player.broadcastPacket(Gon);
        player.revalidateZone(true);
    }
}
