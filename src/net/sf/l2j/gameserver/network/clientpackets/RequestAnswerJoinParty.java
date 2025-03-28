package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.serverpackets.JoinParty;

/**
 *  sample
 *  2a
 *  01 00 00 00
 *
 *  format  cdd
 *
 *
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestAnswerJoinParty extends L2GameClientPacket {

    private int _response;

    @Override
    protected void readImpl() {
        _response = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player != null) {
            if (System.currentTimeMillis() - player.gCPS() < 100) {
                return;
            }
            player.sCPS();

            L2PcInstance requestor = player.getTransactionRequester();

            player.setTransactionRequester(null);

            if (requestor == null) {
                return;
            }

            requestor.setTransactionRequester(null);

            if (requestor.getParty() == null) {
                return;
            }

            if (player.isInOlympiadMode() || requestor.isInOlympiadMode()) {
                return;
            }

            if (player.getChannel() == 6 || requestor.getChannel() == 6) {
                return;
            }

            if (player.getTransactionType() != TransactionType.PARTY || player.getTransactionType() != requestor.getTransactionType()) {
                return;
            }

            requestor.sendPacket(new JoinParty(_response));
            if (_response == 1) {
                if (requestor.getParty().getMemberCount() >= 9) {
                    player.sendPacket(Static.PARTY_FULL);
                    requestor.sendPacket(Static.PARTY_FULL);
                    return;
                }

                player.joinParty(requestor.getParty());
            } else {
                requestor.sendPacket(Static.PLAYER_DECLINED);

                //activate garbage collection if there are no other members in party (happens when we were creating new one)
                if (requestor.getParty() != null && requestor.getParty().getMemberCount() == 1) {
                    requestor.setParty(null);
                }
            }

            requestor.setTransactionType(TransactionType.NONE);
            player.setTransactionType(TransactionType.NONE);
        }
    }
}
