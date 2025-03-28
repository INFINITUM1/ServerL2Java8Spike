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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.ItemRequest;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPrivateStoreSell extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(RequestPrivateStoreSell.class.getName());
    private int _storePlayerId;
    private int _count;
    private int _price;
    private ItemRequest[] _items;

    @Override
    protected void readImpl() {
        _storePlayerId = readD();
        _count = readD();
//		 count*20 is the size of a for iteration of each item
        if (_count < 0 || _count * 20 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET) {
            _count = 0;
        }
        _items = new ItemRequest[_count];

        long priceTotal = 0;
        for (int i = 0; i < _count; i++) {
            int objectId = readD();
            int itemId = readD();
            readH(); //TODO analyse this
            readH(); //TODO analyse this
            long count = readD();
            int price = readD();

            if (count > Integer.MAX_VALUE || count < 0) {
                //String msgErr = "[RequestPrivateStoreSell] player "+getClient().getActiveChar().getName()+" tried an overflow exploit, ban this player!";
                //Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
                _count = 0;
                _items = null;
                return;
            }
            _items[i] = new ItemRequest(objectId, itemId, (int) count, price);
            priceTotal += price * count;
        }

        if (priceTotal < 0 || priceTotal > Integer.MAX_VALUE) {
            //String msgErr = "[RequestPrivateStoreSell] player "+getClient().getActiveChar().getName()+" tried an overflow exploit, ban this player!";
            //Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
            _count = 0;
            _items = null;
            return;
        }

        _price = (int) priceTotal;
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (player.isParalyzed()) {
            return;
        }

        L2PcInstance storePlayer = L2World.getInstance().getPlayer(_storePlayerId);
        if (storePlayer == null) {
            return;
        }
        if (storePlayer.getPrivateStoreType() != L2PcInstance.PS_BUY) {
            return;
        }
        TradeList storeList = storePlayer.getBuyList();
        if (storeList == null) {
            return;
        }

        if (!player.isInsideRadius(storePlayer, 120, false, false)) {
            return;
        }

        if (storePlayer.getAdena() < _price) {
            storePlayer.sendActionFailed();
            storePlayer.sendMessage("You have not enough adena, canceling PrivateBuy.");
            storePlayer.setPrivateStoreType(L2PcInstance.PS_NONE);
            storePlayer.broadcastUserInfo();
            return;
        }

        if (!storeList.PrivateStoreSell(player, _items, _price)) {
            //player.sendActionFailed();
            _log.warning("PrivateStore sell has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
            player.kick();
            return;
        }

        storePlayer.saveTradeList();
        if (storeList.getItemCount() == 0) {
            if (storePlayer.isInOfflineMode()) {
                storePlayer.kick();
                return;
            }
            storePlayer.setPrivateStoreType(L2PcInstance.PS_NONE);
            storePlayer.broadcastUserInfo();
        }
    }
}
