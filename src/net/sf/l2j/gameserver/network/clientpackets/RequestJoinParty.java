package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinParty;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 *  sample
 *  29
 *  42 00 00 10
 *  01 00 00 00
 *
 *  format  cdd
 *
 *
 * @version $Revision: 1.7.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestJoinParty extends L2GameClientPacket {

    private String _name;
    private int _itemDistribution;

    @Override
    protected void readImpl() {
        _name = readS();
        _itemDistribution = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance requestor = getClient().getActiveChar();

        if (requestor == null) {
            return;
        }

        if (System.currentTimeMillis() - requestor.gCPR() < 500) {
            return;
        }

        requestor.sCPR();

        L2PcInstance target = L2World.getInstance().getPlayer(_name);
        if (target == null || target.equals(requestor)) {
            requestor.sendPacket(Static.TARGET_IS_INCORRECT);
            return;
        }

        if (target.isAlone() || target.isInEncounterEvent()) {
            requestor.sendPacket(Static.LEAVE_ALONR);
            requestor.sendActionFailed();
            return;
        }

        if (target.isInParty()) {
            if (requestor.isInParty() && requestor.getParty().equals(target.getParty())) {
                return;
            }

            requestor.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_ALREADY_IN_PARTY).addString(target.getName()));
            return;
        }

        if (target.equals(requestor)) {
            requestor.sendPacket(Static.INCORRECT_TARGET);
            return;
        }

        if (target.isCursedWeaponEquiped() || requestor.isCursedWeaponEquiped()) {
            requestor.sendPacket(Static.INCORRECT_TARGET);
            return;
        }

        if (target.isGM() && target.getMessageRefusal()) {
            requestor.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_ALREADY_IN_PARTY).addString(target.getName()));
            return;
        }

        if (target.isInJail() || requestor.isInJail()) {
            requestor.sendPacket(Static.INCORRECT_TARGET);
            return;
        }

        if (target.isInOlympiadMode() || requestor.isInOlympiadMode()) {
            return;
        }

        if (target.getChannel() == 6 || requestor.getChannel() == 6) {
            return;
        }

        if (target.isInDuel() || requestor.isInDuel()) {
            return;
        }

        if (requestor.isTransactionInProgress()) {
            requestor.sendPacket(Static.WAITING_FOR_ANOTHER_REPLY);
            return;
        }

        if (!requestor.isInParty()) //Asker has no party
        {
            createNewParty(target, requestor);
        } else //Asker is in party
        {
            if (requestor.getParty().isInDimensionalRift()) {
                requestor.sendMessage("You can't invite a player when in Dimensional Rift.");
            } else {
                addTargetToParty(target, requestor);
            }
        }
    }

    /**
     * @param client
     * @param itemDistribution
     * @param target
     * @param requestor
     */
    private void addTargetToParty(L2PcInstance target, L2PcInstance requestor) {
        SystemMessage msg;

        // summary of ppl already in party and ppl that get invitation
        if (requestor.getParty().getMemberCount() >= 9) {
            requestor.sendPacket(SystemMessage.id(SystemMessageId.PARTY_FULL));
            return;
        }

        if (!requestor.getParty().isLeader(requestor)) {
            requestor.sendPacket(SystemMessage.id(SystemMessageId.ONLY_LEADER_CAN_INVITE));
            return;
        }

        if (!target.isTransactionInProgress()) {
            target.setTransactionRequester(requestor, System.currentTimeMillis() + 10000);
            target.setTransactionType(TransactionType.PARTY);
            requestor.setTransactionRequester(target, System.currentTimeMillis() + 10000);
            requestor.setTransactionType(TransactionType.PARTY);

            target.sendPacket(new AskJoinParty(requestor.getName(), _itemDistribution));
            msg = SystemMessage.id(SystemMessageId.YOU_INVITED_S1_TO_PARTY).addString(target.getName());
        } else {
            msg = SystemMessage.id(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(target.getName());
        }

        requestor.sendPacket(msg);
        msg = null;
    }

    /**
     * @param client
     * @param itemDistribution
     * @param target
     * @param requestor
     */
    private void createNewParty(L2PcInstance target, L2PcInstance requestor) {
        SystemMessage msg;
        if (!target.isTransactionInProgress()) {
            requestor.setParty(new L2Party(requestor, _itemDistribution));
            target.setTransactionRequester(requestor, System.currentTimeMillis() + 10000);
            target.setTransactionType(TransactionType.PARTY);
            requestor.setTransactionRequester(target, System.currentTimeMillis() + 10000);
            requestor.setTransactionType(TransactionType.PARTY);

            target.sendPacket(new AskJoinParty(requestor.getName(), _itemDistribution));
            msg = SystemMessage.id(SystemMessageId.YOU_INVITED_S1_TO_PARTY).addString(target.getName());
        } else {
            msg = SystemMessage.id(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(target.getName());
        }

        requestor.sendPacket(msg);
        msg = null;
    }
}
