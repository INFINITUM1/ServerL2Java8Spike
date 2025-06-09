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

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.Rnd;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.lasthero.LastHero;
import scripts.autoevents.masspvp.massPvp;

public final class DlgAnswer extends L2GameClientPacket {

    private int _messageId, _response, _unk;

    @Override
    protected void readImpl() {
        _messageId = readD();
        _response = readD();
        _unk = readD();
    }

    @Override
    public void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null)
            return;

        switch (_messageId) 
        {
            case 614: // свадьба
                if (_unk > 0) {
                    if (_response != 1) {
                        player.sendPacket(Static.EVENT_PASS);
                        return;
                    }
                    switch (_unk) {
                        case 104:
                            BaseCapture.getEvent().regPlayer(player);
                            break;
                        case 106:
                            LastHero.getEvent().regPlayer(player);
                            break;
                        case 107:
                            massPvp.getEvent().regPlayer(player);
                            break;
                        case 108:
                            TvTEvent.onBypass("tvt_event_participation", player);
                            break;
                    }
                    return;
                }
                player.engageAnswer(_response);
                break;
            case 1510: // рес
                player.reviveAnswer(_response);
                break;
            case 1842: // самон кота
                player.sfAnswer(_response);
                break;
            case 100:

                L2PcInstance partner = player.getTransactionRequester();

                if (partner == null || partner.getTransactionRequester() == null) {
                    // Trade partner not found, cancel trade
                    if (_response != 0) {
                        player.cancelActiveTrade();
                        player.sendPacket(Static.TARGET_IS_NOT_FOUND_IN_THE_GAME);
                    }
                    player.setTradePartner(-1, 0);
                    player.setTransactionRequester(null);
                    player.setTransactionType(TransactionType.NONE);
                    return;
                }

                if (!player.isInsideRadius(partner, 320, false, false)) {
                    player.sendPacket(Static.TARGET_TOO_FAR);
                    player.sendActionFailed();
                    return;
                }

                if (player.getTransactionType() != TransactionType.TRADE || player.getTransactionType() != partner.getTransactionType()) {
                    clearTrade(player, partner);
                    return;
                }

                if (_response != 1 || player.getPrivateStoreType() != 0) {
                    partner.sendPacket(SystemMessage.id(SystemMessageId.S1_DENIED_TRADE_REQUEST).addString(player.getName()));
                    clearTrade(player, partner);
                    if (player.getPrivateStoreType() != 0) {
                        player.sendPacket(Static.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
                    }
                    return;
                }

                if (player.getTradePartner() == -1) {
                    player.sendPacket(Static.ANSWER_TIMEOUT);
                    clearTrade(player, partner);
                    return;
                }

                if (partner.getTradePartner() == -1) {
                    player.sendPacket(Static.ANSWER_TIMEOUT);
                    clearTrade(player, partner);
                    return;
                }

                if (player.tradeLeft() || partner.tradeLeft()) {
                    player.sendPacket(Static.ANSWER_TIMEOUT);
                    clearTrade(player, partner);
                    return;
                }

                if (_response == 1 && player.getTradeStart() == partner.getTradeStart()) {
                    player.startTrade(partner);
                    player.setTransactionType(TransactionType.TRADED);
                    partner.setTransactionType(TransactionType.TRADED);
                } else {
                    partner.sendPacket(SystemMessage.id(SystemMessageId.S1_DENIED_TRADE_REQUEST).addString(player.getName()));
                    clearTrade(player, partner);
                }

                player.setTransactionRequester(partner);
                partner.setTransactionRequester(player);
                break;
            case 2010:
                if (_unk > 0) {
                    if (_response != 1) {
                        player.sendPacket(Static.TELETO_RB);
                        return;
                    }
                    switch (_unk) {
                        case 1:
                            player.teleToLocation(player._raidloc.getX() + Rnd.get(50, 100), player._raidloc.getY() + Rnd.get(50, 100), player._raidloc.getZ());
                            break;
                    }
                    return;
                }
                break;
        }
    }

    private void clearTrade(L2PcInstance player, L2PcInstance partner) {
        player.setTransactionRequester(null);
        partner.setTransactionRequester(null);
        partner.setTransactionType(TransactionType.NONE);
        player.setTransactionType(TransactionType.NONE);

        player.setTradePartner(-1, 0);
        partner.setTradePartner(-1, 0);
        player.sendActionFailed();
    }
}
