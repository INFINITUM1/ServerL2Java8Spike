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
 * @version $Revision: 1.7.2.1.2.2 $ $Date: 2005/03/27 15:29:30 $
 */
@SuppressWarnings("unused")
public final class AttackRequest extends L2GameClientPacket {
    // cddddc

    private int _objectId;
	private int _originX;
    private int _originY;
    private int _originZ;
    private int _attackId;

    @Override
    protected void readImpl() {
        _objectId = readD();
        _originX = readD();
        _originY = readD();
        _originZ = readD();
        _attackId = readC(); 	 // 0 for simple click   1 for shift-click
    }

    @Override
    protected void runImpl() {
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

        if (player.isOutOfControl() || player.isParalyzed()) {
            player.sendActionFailed();
            return;
        }

        if (player.isDead() || player.isAlikeDead() || player.isFakeDeath()) {
            player.sendActionFailed();
            return;
        }
        // avoid using expensive operations if not needed
		/*L2Object target = null;
        if (player.getTargetId() == _objectId)
        target = player.getTarget();
        if (target == null)
        {
        target = L2World.getInstance().findObject(_objectId);
        if(target == null || !player.canTarget(obj))// || !(target instanceof L2ItemInstance))
        {
        player.sendActionFailed();
        return;
        }
        }*/
        L2Object target;
        if (player.getTargetId() == _objectId) {
            target = player.getTarget();
        } else {
            target = L2World.getInstance().findObject(_objectId);
            if (target == null) {
                target = L2World.getInstance().getPlayer(_objectId);
            }
        }

        // If object requested does not exist, add warn msg into logs
        if (target == null || !player.canTarget(target)) {
            player.sendActionFailed();
            return;
        }

        if (player.getTarget() != target) {
            target.onAction(player);
            return;
        }

        if (target.isInZonePeace()) {
            player.sendActionFailed();
            return;
        }

        if ((target.getObjectId() != player.getObjectId()) && player.getPrivateStoreType() == 0 && player.getTransactionRequester() == null)// && player.getActiveRequester() ==null)
        {
            target.onForcedAttack(player);
        } else {
            player.sendActionFailed();
        }
    }
}
