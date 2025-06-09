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

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2Item;

/**
 * This class ...
 *
 * @version $Revision: 1.8.2.3.2.7 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestUnEquipItem extends L2GameClientPacket {

    // cd
    private int _slot;

    /**
     * packet type id 0x11
     * format:		cd
     * @param decrypt
     */
    @Override
    protected void readImpl() {
        _slot = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();

        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPL() < 100) {
            return;
        }

        player.sCPL();

        L2ItemInstance item = player.getInventory().getPaperdollItemByL2ItemId(_slot);
        if (item == null) {
            return;
        }

        // Prevent player from unequipping items in special conditions
        if (player.isStunned() || player.isSleeping() || player.isParalyzed() || player.isAlikeDead()) {
            return;
        }
        if (item.isWear()) {
            return;
        }

        switch (item.getItem().getBodyPart()) {
            case L2Item.SLOT_LR_HAND:
            case L2Item.SLOT_L_HAND:
            case L2Item.SLOT_R_HAND:
                // Don't allow weapon/shield equipment if a cursed weapon is equiped
                if (player.isCursedWeaponEquiped() || item.getItemId() == 6408) // Don't allow to put formal wear
                {
                    player.sendActionFailed();
                    return;
                }
                player.equipWeapon(item);
                break;
            default:
                player.useEquippableItem(item, true);
        }
    }
}
