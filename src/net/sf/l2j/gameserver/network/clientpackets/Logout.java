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
import net.sf.l2j.gameserver.SevenSignsFestival;
import scripts.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * This class ...
 *
 * @version $Revision: 1.9.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class Logout extends L2GameClientPacket {

    @Override
    protected void readImpl() {
    }

    @Override
    protected void runImpl() {
        // Dont allow leaving if player is fighting
        L2PcInstance player = getClient().getActiveChar();

        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPAV() < 100) {
            return;
        }

        player.sCPAV();

        player.getInventory().updateDatabase();

        if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player)) {
            //if (Config.DEBUG) _log.fine("Player " + player.getName() + " tried to logout while fighting");

            player.sendPacket(Static.CANT_LOGOUT_WHILE_FIGHTING);
            player.sendActionFailed();
            return;
        }

        if (player.getActiveTradeList() != null) {
            //player.getTransactionRequester().onTradeCancel(player);
            //player.onTradeCancel(player.getTransactionRequester());
            player.cancelActiveTrade();
            if (player.getTransactionRequester() != null) {
                player.getTransactionRequester().setTransactionRequester(null);
            }
            player.setTransactionRequester(null);
        }

        if (player.atEvent) {
            player.sendMessage("A superior power doesn't allow you to leave the event");
            return;
        }

        /* if (player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
        {
        player.sendMessage("Нельзя выйти из игры на олимпиаде");
        return;
        }*/

        if (player.isInOfflineMode()) {
            player.closeNetConnection();
            return;
        }

        // Prevent player from logging out if they are a festival participant
        // and it is in progress, otherwise notify party members that the player
        // is not longer a participant.
        if (player.isFestivalParticipant()) {
            if (SevenSignsFestival.getInstance().isFestivalInitialized()) {
                player.sendMessage("You cannot log out while you are a participant in a festival.");
                return;
            }
            L2Party playerParty = player.getParty();

            if (playerParty != null) {
                player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming festival."));
            }
        }
        if (player.isFlying()) {
            player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
        }

        TvTEvent.onLogout(player);
        RegionBBSManager.getInstance().changeCommunityBoard();

        player.deleteMe();
    }
}