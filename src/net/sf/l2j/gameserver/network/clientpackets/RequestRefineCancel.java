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

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExVariationCancelResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;

/**
 * Format(ch) d
 * @author  -Wooden-
 */
public final class RequestRefineCancel extends L2GameClientPacket {

    private int _targetItemObjId;

    @Override
    protected void readImpl() {
        _targetItemObjId = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_targetItemObjId);

        if (player == null) {
            return;
        }
        if (targetItem == null) {
            player.sendPacket(new ExVariationCancelResult(0));
            return;
        }

        if (!targetItem.canBeEnchanted()) {
            player.sendPacket(new ExVariationCancelResult(0));
            return;
        }

        // cannot remove augmentation from a not augmented item
        if (!targetItem.isAugmented()) {
            player.sendPacket(Static.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
            player.sendPacket(new ExVariationCancelResult(0));
            return;
        }

        // get the price
        int price = 0;
        switch (targetItem.getItem().getItemGrade()) {
            case L2Item.CRYSTAL_C:
                if (targetItem.getCrystalCount() < 1720) {
                    price = 95000;
                } else if (targetItem.getCrystalCount() < 2452) {
                    price = 150000;
                } else {
                    price = 210000;
                }
                break;
            case L2Item.CRYSTAL_B:
                if (targetItem.getCrystalCount() < 1746) {
                    price = 240000;
                } else {
                    price = 270000;
                }
                break;
            case L2Item.CRYSTAL_A:
                if (targetItem.getCrystalCount() < 2160) {
                    price = 330000;
                } else if (targetItem.getCrystalCount() < 2824) {
                    price = 390000;
                } else {
                    price = 420000;
                }
                break;
            case L2Item.CRYSTAL_S:
                price = 480000;
                break;
            // any other item type is not augmentable
            default:
                player.sendPacket(new ExVariationCancelResult(0));
                return;
        }

        // try to reduce the players adena
        if (!player.reduceAdena("RequestRefineCancel", price, null, true)) {
            return;
        }

        // unequip item
        if (targetItem.isEquipped()) {
            player.disarmWeapons();
        }

        // remove the augmentation
        targetItem.removeAugmentation();

        // send ExVariationCancelResult
        player.sendPacket(new ExVariationCancelResult(1));

        // send inventory update
        InventoryUpdate iu = new InventoryUpdate();
        iu.addModifiedItem(targetItem);
        player.sendPacket(iu);

        // send system message
        player.sendPacket(SystemMessage.id(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1).addString(targetItem.getItemName()));
    }
}
