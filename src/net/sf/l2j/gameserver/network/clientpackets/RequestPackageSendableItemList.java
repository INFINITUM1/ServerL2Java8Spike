/* This program is free software; you can redistribute it and/or modify
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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.ItemContainer;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.WareHouseDepositList;

public final class RequestPackageSendableItemList extends L2GameClientPacket {

    private int _objectID;

    @Override
    protected void readImpl() {
        _objectID = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    public void runImpl() {
        if (!Config.ALLOW_FREIGHT) {
            return;
        }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        // Alt game - Karma punishment
        if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE
                && player.getKarma() > 0) {
            sendPacket(Static.NO_KARRMA_TELE);
            return;
        }

        if (player.getFreightTarget() != 0
                || player.getObjectId() == _objectID
                || !player.getAccountChars().containsKey(_objectID)) {
            return;
        }

        if (L2World.getInstance().getPlayer(_objectID) != null) {
            return;
        }

        ItemContainer warehouse = player.getActiveWarehouse();
        if (warehouse != null) {
            player.cancelActiveWarehouse();
        }

        player.tempInvetoryDisable();
        player.setFreightTarget(_objectID);
        player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.FREIGHT));
    }
}
