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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/29 23:15:33 $
 */
public final class RequestGiveItemToPet extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(RequestGetItemFromPet.class.getName());
    private int _objectId;
    private int _amount;

    @Override
    protected void readImpl() {
        _objectId = readD();
        _amount = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null || player.getPet() == null || !(player.getPet().isPet())) {
            return;
        }

        // Alt game - Karma punishment
        //if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && player.getKarma() > 0) 
        //return;

        if (System.currentTimeMillis() - player.gCPT() < 400) {
            player.sendActionFailed();
            return;
        }

        player.sCPT();

        if (_amount < 0) {
            return;
        }

        if (!Config.GIVE_ITEM_PET) {
            player.sendMessage("Отключено.");
            player.sendActionFailed();
            return;
        }

        if (player.getActiveEnchantItem() != null) {
            player.setActiveEnchantItem(null);
            player.sendPacket(new EnchantResult(0, true));
            player.sendActionFailed();
            return;
        }

        if (player.getActiveTradeList() != null) {
            player.cancelActiveTrade();
            player.sendActionFailed();
            return;
        }

        if (player.getActiveWarehouse() != null) {
            player.cancelActiveWarehouse();
            player.sendActionFailed();
            return;
        }

        if (player.getPrivateStoreType() != 0) {
            player.sendMessage("Cannot exchange items while trading");
            return;
        }

        L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
        if (item == null) {
            player.sendActionFailed();
            return;
        }

        if (item.isAugmented()) {
            return;
        }

        if (!item.isDropable() || !item.isDestroyable() || !item.isTradeable()) {
            player.sendPacket(Static.ITEM_NOT_FOR_PETS);
            return;
        }

        L2PetInstance pet = (L2PetInstance) player.getPet();
        if (pet.isDead()) {
            player.sendPacket(Static.CANNOT_GIVE_ITEMS_TO_DEAD_PET);
            return;
        }

        if (item.getItem().isForWolf() || item.getItem().isForHatchling() || item.getItem().isForStrider() || item.getItem().isForBabyPet()) {
            if (player.transferItem("Transfer", _objectId, _amount, pet.getInventory(), pet) == null) {
                _log.warning("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
            }
        } else {
            player.sendPacket(Static.ITEM_NOT_FOR_PETS);
            return;
        }
    }
}
