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

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExConfirmCancelItem;
import net.sf.l2j.gameserver.templates.L2Item;

/**
 * Format(ch) d
 * @author  -Wooden-
 */
public final class RequestConfirmCancelItem extends L2GameClientPacket {

    private int _itemId;

    /**
     * @param buf
     * @param client
     */
    @Override
    protected void readImpl() {
        _itemId = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        L2ItemInstance item = (L2ItemInstance) L2World.getInstance().findObject(_itemId);

        if (player == null || item == null) {
            return;
        }
        if (!item.isAugmented()) {
            player.sendPacket(Static.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
            return;
        }

        int price = 0;
        switch (item.getItem().getItemGrade()) {
            case L2Item.CRYSTAL_C:
                if (item.getCrystalCount() < 1720) {
                    price = 95000;
                } else if (item.getCrystalCount() < 2452) {
                    price = 150000;
                } else {
                    price = 210000;
                }
                break;
            case L2Item.CRYSTAL_B:
                if (item.getCrystalCount() < 1746) {
                    price = 240000;
                } else {
                    price = 270000;
                }
                break;
            case L2Item.CRYSTAL_A:
                if (item.getCrystalCount() < 2160) {
                    price = 330000;
                } else if (item.getCrystalCount() < 2824) {
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
                return;
        }

        player.sendPacket(new ExConfirmCancelItem(_itemId, price));
    }
}
