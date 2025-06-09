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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListSell;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgSell;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.1.2.5 $ $Date: 2005/03/27 15:29:30 $
 */
public class SetPrivateStoreListSell extends L2GameClientPacket {

    private int _count;
    private boolean _packageSale;
    private int[] _items; // count * 3

    @Override
    protected void readImpl() {
        _packageSale = (readD() == 1);
        _count = readD();
        if (_count <= 0 || _count * 12 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET) {
            _count = 0;
            _items = null;
            return;
        }
        _items = new int[_count * 3];
        for (int x = 0; x < _count; x++) {
            int objectId = readD();
            _items[x * 3 + 0] = objectId;
            long cnt = readD();
            if (cnt > Integer.MAX_VALUE || cnt < 0) {
                _count = 0;
                _items = null;
                return;
            }
            _items[x * 3 + 1] = (int) cnt;
            int price = readD();
            _items[x * 3 + 2] = price;
        }
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        TradeList tradeList = player.getSellList();
        tradeList.clear();
        tradeList.setPackaged(_packageSale);

        for (int i = 0; i < _count; i++) {
            int objectId = _items[i * 3 + 0];
            int count = _items[i * 3 + 1];
            int price = _items[i * 3 + 2];

            if (price <= 0) {
                //String msgErr = "[SetPrivateStoreListSell] player "+getClient().getActiveChar().getName()+" tried an overflow exploit (use PHX), ban this player!";
                //Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
                _count = 0;
                _items = null;
                return;
            }

            tradeList.addItem(objectId, count, price);
        }

        if (_count <= 0 || player.getChannel() > 1) {
            player.setPrivateStoreType(L2PcInstance.PS_NONE);
            player.broadcastUserInfo();
            return;
        }

        if (player.isCastingNow() || player.isFakeDeath()) {
            player.sendPacket(new PrivateStoreManageListSell(player));
            player.sendPacket(Static.A_PRIVATE_STORE_MAY_NOT_BE_OPENED_WHILE_USING_A_SKILL);
            return;
        }

        // Check maximum number of allowed slots for pvt shops
        if (_count > player.getPrivateSellStoreLimit()) {
            player.sendPacket(new PrivateStoreManageListSell(player));
            player.sendPacket(Static.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
            player.sendActionFailed();
            return;
        }

        if (!player.canTrade()) {
            player.sendActionFailed();
            return;
        }

        if (player.isSitting()) {
            player.setPrivateStoreType(L2PcInstance.PS_SELL + 1);
            player.sendPacket(new PrivateStoreManageListSell(player));
            player.sendPacket(Static.PLEASE_WAIT);
            return;
        }

        player.sitDown();
        if (_packageSale) {
            player.setPrivateStoreType(L2PcInstance.PS_PACKAGE_SELL);
        } else {
            player.setPrivateStoreType(L2PcInstance.PS_SELL);
        }
        player.saveTradeList();
        player.setTarget(null);
        //player.setChannel(9, true);
        player.broadcastPacket(new PrivateStoreMsgSell(player));
        player.broadcastUserInfo();
    }
}
