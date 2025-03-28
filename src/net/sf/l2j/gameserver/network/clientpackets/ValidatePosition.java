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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;

/**
 * This class ...
 *
 * @version $Revision: 1.13.4.7 $ $Date: 2005/03/27 15:29:30 $
 */
public class ValidatePosition extends L2GameClientPacket {

    /**
     * urgent messages, execute immediatly
     */
    //task// public TaskPriority getPriority() { return TaskPriority.PR_HIGH; }
    private int _x;
    private int _y;
    private int _z;
    private int _heading;
    @SuppressWarnings("unused")
    private int _data;

    @Override
    protected void readImpl() {
        _x = readD();
        _y = readD();
        _z = readD();
        _heading = readD();
        _data = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (player.isTeleporting() || player.inObserverMode()) {
            return;
        }

        if (Config.ALLOW_FALL && player.isFalling(_z)) {
            return; // disable validations during fall to avoid "jumping"
        }
        final int realX = player.getX();
        final int realY = player.getY();
        int realZ = player.getZ();

        int dx, dy, dz;
        double diffSq;

        dx = _x - realX;
        dy = _y - realY;
        dz = _z - realZ;
        diffSq = (dx * dx + dy * dy);

        if (player.isFlying() || player.isInWater()) {
            //player.setXYZ(realX, realY, _z);
            //player.sendAdmResultMessage("w.x: " + _x + " y:" + _y + " z: " + _z);
            realZ = _z;
            player.setXYZ(realX, realY, _z);
            if (diffSq > 90000) // validate packet, may also cause z bounce if close to land
            {
                player.sendPacket(new ValidateLocation(player));
            }

            //player.sendAdmResultMessage("w.realX: " + realX + " y:" + realY + " z: " + realZ);
        } else if (diffSq < 360000) // if too large, messes observation
        {
            if (Config.COORD_SYNCHRONIZE == -1) // Only Z coordinate synched to server,
            // mainly used when no geodata but can be used also with geodata
            {
                player.setXYZ(realX, realY, _z);
                return;
            }
            if (Config.COORD_SYNCHRONIZE == 1) // Trusting also client x,y coordinates (should not be used with geodata)
            {
                if (!player.isMoving()
                        || !player.validateMovementHeading(_heading)) // Heading changed on client = possible obstacle
                {
                    // character is not moving, take coordinates from client
                    if (diffSq < 2500) // 50*50 - attack won't work fluently if even small differences are corrected
                    {
                        player.setXYZ(realX, realY, _z);
                    } else {
                        player.setXYZ(_x, _y, _z);
                    }
                } else {
                    player.setXYZ(realX, realY, _z);
                }

                player.setHeading(_heading);
                return;
            }
            // Sync 2 (or other),
            // intended for geodata. Sends a validation packet to client
            // when too far from server calculated true coordinate.
            // Due to geodata/zone errors, some Z axis checks are made. (maybe a temporary solution)
            // Important: this code part must work together with L2Character.updatePosition
            if (Config.GEODATA > 0 && (diffSq > 250000 || Math.abs(dz) > 200)) {
                //if ((_z - player.getClientZ()) < 200 && Math.abs(player.getLastServerPosition().getZ()-realZ) > 70)

                if (Math.abs(dz) > 200
                        && Math.abs(dz) < 1500
                        && Math.abs(_z - player.getClientZ()) < 800) {
                    player.setXYZ(realX, realY, _z);
                    realZ = _z;
                } else {
                    player.sendPacket(new ValidateLocation(player));
                }
            }
        }

        /*if (checkDiffZ(player)
                && getDiffZ(realX, realY, realZ, _z) >= 50) {

            player.getPosition().setXYZ(_x, _y, GeoData.getInstance().getHeight(_x, _y, _z));
            player.sendPacket(new ValidateLocation(player));
        }*/

        player.setClientX(_x);
        player.setClientY(_y);
        player.setClientZ(_z);
        player.setClientHeading(_heading); // No real need to validate heading.
        player.setLastServerPosition(realX, realY, realZ);
    }

    private boolean checkDiffZ(L2PcInstance activeChar) {
        return !activeChar.isFlying() && !activeChar.isInWater();
    }

    private int getDiffZ(int x, int y, int z, int checkZ) {
        return Math.abs(GeoData.getInstance().getHeight(x, y, z) - checkZ);
    }
}
