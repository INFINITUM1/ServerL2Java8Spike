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

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.7.4.4 $ $Date: 2005/03/27 18:46:19 $
 */
public final class Action extends L2GameClientPacket {
    // cddddc

    private int _objectId;
    /*@SuppressWarnings("unused")
    private int _originX;
    @SuppressWarnings("unused")
    private int _originY;
    @SuppressWarnings("unused")
    private int _originZ;*/
    private int _actionId;

    @Override
    protected void readImpl() {
        _objectId = readD();   // Target object Identifier
        //_originX = readD();
        //_originY = readD();
        //_originZ = readD();
        readD();
        readD();
        readD();
        _actionId = readC();   // Action identifier : 0-Simple click, 1-Shift click
    }

    @Override
    protected void runImpl() {
        //System.out.println("##runImpl#1#" + _actionId);
        // Get the current L2PcInstance of the player
        L2PcInstance player = getClient().getActiveChar();

        if (player == null) {
            return;
        }

        /*if(System.currentTimeMillis() - player.getLastPacket() < 100)
        {
        player.sendActionFailed();
        return;
        }
        player.setLastPacket();*/
        //System.out.println("##runImpl#2#");

        if (player.isOutOfControl() || player.isParalyzed()) {
            player.sendActionFailed();
            return;
        }
        //System.out.println("##runImpl#3#");

        if (player.inObserverMode()) {
            player.sendActionFailed();
            return;
        }

        if (player.getPrivateStoreType() != 0) {
            player.sendActionFailed();
            return;
        }

        if (L2World.getInstance().getPlayer(player.getObjectId()) == null) {
            player.kick();
            return;
        }
        //System.out.println("##runImpl#4#");

        L2Object obj;
        if (player.getTargetId() == _objectId) {
            obj = player.getTarget();
        } else {
            obj = L2World.getInstance().findObject(_objectId);
            if (obj == null) {
                obj = L2World.getInstance().getPlayer(_objectId);
            }
        }
        //System.out.println("##runImpl#5#");

        // If object requested does not exist, add warn msg into logs
        if (obj == null || !player.canTarget(obj)) {
            player.sendActionFailed();
            return;
        }
        //System.out.println("##runImpl#6#");

        player.clearNextLoc();
        switch (_actionId) {
            case 0:
                obj.onAction(player);
                break;
            case 1:
                /*if ((!player.isGM()) && obj.isL2Door())
                {
                player.logout();
                return;
                }*/

                if (obj.isPlayer()) {
                    obj.getPlayer().onActionShift(player);
                } else if (obj.isL2Character() && obj.isAlikeDead()) {
                    obj.onAction(player);
                } else {
                    obj.onActionShift(getClient());
                }
                break;
            default:
                player.sendActionFailed();
                break;
        }
    }
}
