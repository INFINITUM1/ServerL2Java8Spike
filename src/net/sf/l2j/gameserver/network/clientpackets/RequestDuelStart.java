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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelAskStart;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Format:(ch) Sd
 * @author  -Wooden-
 */
public final class RequestDuelStart extends L2GameClientPacket {

    private static Logger _log = Logger.getLogger(RequestDuelStart.class.getName());
    private String _player;
    private int _partyDuel;

    @Override
    protected void readImpl() {
        _player = readS();
        _partyDuel = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        L2PcInstance targetChar = L2World.getInstance().getPlayer(_player);
        if (player == null) {
            return;
        }
        if (targetChar == null || player == targetChar) {
            player.sendPacket(Static.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
            return;
        }

        if (targetChar.isAlone()) {
            player.sendMessage("Игрок просил его не беспокоить");
            player.sendActionFailed();
            return;
        }

        if (!Duel.checkIfCanDuel(player, player, true) || !Duel.checkIfCanDuel(player, targetChar, true)) {
            return;
        }

        // Duel is a party duel
        if (_partyDuel == 1) {
            player.sendMessage("Извините, но пати дуэль отключен.");
            return;
            // Player must be in a party & the party leader
			/*if (!player.isInParty() || !(player.isInParty() && player.getParty().isLeader(player)))
            {
            player.sendMessage("You have to be the leader of a party in order to request a party duel.");
            return;
            }
            // Target must be in a party
            else if (!targetChar.isInParty())
            {
            player.sendPacket(Static.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY);
            return;
            }
            // Target may not be of the same party
            else if (player.getParty().getPartyMembers().contains(targetChar))
            {
            player.sendMessage("This player is a member of your own party.");
            return;
            }
            
            // Check if every player is ready for a duel
            for (L2PcInstance temp : player.getParty().getPartyMembers())
            {
            if (!temp.canDuel())
            {
            player.sendMessage("Not all the members of your party are ready for a duel.");
            return;
            }
            }
            L2PcInstance partyLeader = null; // snatch party leader of targetChar's party
            for (L2PcInstance temp : targetChar.getParty().getPartyMembers())
            {
            if (partyLeader == null) partyLeader = temp;
            if (!temp.canDuel())
            {
            player.sendPacket(Static.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
            return;
            }
            }
            
            // Send request to targetChar's party leader
            if (!partyLeader.isTransactionInProgress())
            {
            player.onTransactionRequest(partyLeader);
            partyLeader.sendPacket(new ExDuelAskStart(player.getName(), _partyDuel));
            
            //if (Config.DEBUG)
            //   _log.fine(player.getName() + " requested a duel with " + partyLeader.getName());
            
            SystemMessage msg = SystemMessage.id(SystemMessageId.S1S_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL);
            msg.addString(partyLeader.getName());
            player.sendPacket(msg);
            
            msg = SystemMessage.id(SystemMessageId.S1S_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL);
            msg.addString(player.getName());
            targetChar.sendPacket(msg);
            }
            else
            {
            SystemMessage msg = SystemMessage.id(SystemMessageId.S1_IS_BUSY_TRY_LATER);
            msg.addString(partyLeader.getName());
            player.sendPacket(msg);
            }*/
        } else // 1vs1 duel
        {
            if (!targetChar.isTransactionInProgress()) {
                player.onTransactionRequest(targetChar);
                targetChar.sendPacket(new ExDuelAskStart(player.getName(), _partyDuel));

                //if (Config.DEBUG)
                //_log.fine(player.getName() + " requested a duel with " + targetChar.getName());
                player.sendPacket(SystemMessage.id(SystemMessageId.S1_HAS_BEEN_CHALLENGED_TO_A_DUEL).addString(targetChar.getName()));
                targetChar.sendPacket(SystemMessage.id(SystemMessageId.S1_HAS_CHALLENGED_YOU_TO_A_DUEL).addString(player.getName()));
            } else {
                player.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(targetChar.getName()));
            }
        }
    }
}
