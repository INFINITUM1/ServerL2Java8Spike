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
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListBuy;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPrivateStoreManageBuy extends L2GameClientPacket {

    @Override
    protected void readImpl() {
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        // Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
        if (player.isAlikeDead()) {
            player.sendActionFailed();
            return;
        }

        if (!player.canTrade()) {
            player.sendActionFailed();
            return;
        }

        if (player.isInOlympiadMode() || player.getMountType() != 0) {
            player.sendActionFailed();
            return;
        }

        if (player.getActiveTradeList() != null) {
            player.cancelActiveTrade();
            player.sendActionFailed();
            return;
        }

        if (player.getPrivateStoreType() == L2PcInstance.PS_BUY || player.getPrivateStoreType() == L2PcInstance.PS_BUY + 1) {
            player.setPrivateStoreType(L2PcInstance.PS_NONE);
        }

        if (player.getPrivateStoreType() == L2PcInstance.PS_NONE) {
            if (player.isSitting()) {
                player.standUp();
            }
            player.setPrivateStoreType(L2PcInstance.PS_BUY + 1);
            player.sendPacket(new PrivateStoreManageListBuy(player));
            //player.setChannel(1, true);
        }
    }
}
