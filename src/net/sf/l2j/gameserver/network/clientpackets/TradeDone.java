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
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
//import net.sf.l2j.gameserver.network.serverpackets.TradePressOtherOk;
//import net.sf.l2j.gameserver.network.serverpackets.TradePressOwnOk;

/**
 * This class ...
 *
 * @version $Revision: 1.6.2.2.2.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class TradeDone extends L2GameClientPacket {

    private static Logger _log = Logger.getLogger(TradeDone.class.getName());
    private int _response;

    @Override
    protected void readImpl() {
        _response = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPO() < 500) {
            return;
        }

        player.sCPO();
        //System.out.println("#####1");

        L2PcInstance requestor = player.getTransactionRequester();
        if (requestor == null) {
            clearTrade(player, null);
            return;
        }

        if (requestor == player || player == requestor) {
            clearTrade(player, requestor);
            return;
        }
        //System.out.println("#####2");

        if (player.getTradePartner() != requestor.getObjectId() || player.getTradeStart() != requestor.getTradeStart()) {
            //System.out.println("#####2 1: " + player.getTradePartner() + " 2: " + requestor.getObjectId() + "3: " + player.getTradeStart() + "4: " + requestor.getTradeStart());
            clearTrade(player, requestor);
            return;
        }
        //System.out.println("#####3");

        TradeList trade = player.getActiveTradeList();
        if (trade == null || (trade.getItemCount() == 0 && (requestor.getActiveTradeList() != null && requestor.getActiveTradeList().getItemCount() == 0))) {
            clearTrade(player, requestor);
            //_log.warning("player.getTradeList == null in "+getType()+" for player "+player.getName());
            return;
        }

        if (trade.isLocked()) {
            return;
        }

        //System.out.println("#####4");
        if (_response == 1) {
            //System.out.println("#####5 " + player.getName());
            if (player.getTransactionRequester() == null || trade.getPartner() == null || L2World.getInstance().findObject(trade.getPartner().getObjectId()) == null) {
                // Trade partner not found, cancel trade
                player.cancelActiveTrade();
                player.setTransactionRequester(null);
                player.setTransactionType(TransactionType.NONE);
                player.setTradePartner(-1, 0);
                player.sendPacket(Static.TARGET_IS_NOT_FOUND_IN_THE_GAME);
                return;
            }

            /*if (trade.getAugmentCount() >= 1)
            {
            int BlueEva = trade.getAugmentCount() * 2;
            if (player.getItemCount(4355) >= BlueEva)
            player.sendMessage("После подтверждения, будет оплачен налог: "+BlueEva+" Blue Eva");
            else
            {
            player.cancelActiveTrade();
            player.sendMessage("Не хватает Blue Eva; Налог: "+BlueEva+" Blue Eva");
            
            player.setTransactionRequester(null);
            trade.getPartner().setTransactionRequester(null);
            return;
            }
            }*/
            if (trade.isConfirmed()) {
                requestor.onTradeConfirm(player);
            }

            trade.confirm();
            return;
        } else {
            //System.out.println("#####6");
            player.cancelActiveTrade();
            player.setTransactionRequester(null);
            requestor.setTransactionRequester(null);
        }

        player.setTradePartner(-1, 0);
        requestor.setTradePartner(-1, 0);

        requestor.setTransactionType(TransactionType.NONE);
        player.setTransactionType(TransactionType.NONE);
    }

    private void clearTrade(L2PcInstance player, L2PcInstance partner) {
        player.cancelActiveTrade();
        player.setTransactionRequester(null);
        player.setTransactionType(TransactionType.NONE);
        player.setTradePartner(-1, 0);
        player.sendActionFailed();

        if (partner != null) {
            partner.cancelActiveTrade();
            partner.setTransactionRequester(null);
            partner.setTransactionType(TransactionType.NONE);
            partner.setTradePartner(-1, 0);
            partner.sendActionFailed();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
    public String getType() {
        return "[C] TradeDone";
    }
}
