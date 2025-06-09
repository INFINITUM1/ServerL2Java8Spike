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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.TradeOtherAdd;
import net.sf.l2j.gameserver.network.serverpackets.TradeOwnAdd;
import net.sf.l2j.gameserver.network.serverpackets.TradeUpdate;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.2.2.5 $ $Date: 2005/03/27 15:29:29 $
 */
public final class AddTradeItem extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(AddTradeItem.class.getName());
    private int _tradeId;
    private int _objectId;
    private int _count;

    public AddTradeItem() {
    }

    @Override
    protected void readImpl() {
        _tradeId = readD();
        _objectId = readD();
        _count = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null || _count < 1) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPN() < 300) {
            return;
        }

        player.sCPN();

        TradeList trade = player.getActiveTradeList();
        if (trade == null) {
            _log.warning("Character: " + player.getName() + " requested item:" + _objectId + " add without active tradelist:" + _tradeId);
            return;
        }

        if (trade.getPartner() == null || L2World.getInstance().getPlayer(trade.getPartner().getObjectId()) == null) {
            // Trade partner not found, cancel trade
            if (trade.getPartner() != null) {
                _log.warning("Character:" + player.getName() + " requested invalid trade object: " + _objectId);
            }
            player.sendPacket(Static.TARGET_IS_NOT_FOUND_IN_THE_GAME);
            player.cancelActiveTrade();
            return;
        }

        L2ItemInstance InvItem = player.getInventory().getItemByObjectId(_objectId);

        if (!player.validateItemManipulation(_objectId, "trade")) {
            player.sendPacket(Static.NOTHING_HAPPENED);
            return;
        }

        TradeList.TradeItem item = trade.addItem(_objectId, _count);
        if (item != null) {
            player.sendPacket(new TradeOwnAdd(item));
            player.sendPacket(new TradeUpdate(InvItem));
            trade.getPartner().sendPacket(new TradeOtherAdd(item));
        }
    }
}
