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
package scripts.items.itemhandlers;

import scripts.items.IItemHandler;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.serverpackets.ChooseInventoryItem;

public class EnchantScrolls implements IItemHandler {

    private static final int[] ITEM_IDS = {
        729, 730, 731, 732, 6569, 6570, // a grade
        947, 948, 949, 950, 6571, 6572, // b grade
        951, 952, 953, 954, 6573, 6574, // c grade
        955, 956, 957, 958, 6575, 6576, // d grade
        959, 960, 961, 962, 6577, 6578, // s grade
        15000, 15001, 15002, 15003, 15004, 15005, 15006, 15007, 15008, 15009, //alt
        15010, 15011, 15012, 15013, 15014, 15015, 15016, 15017, 15018, 15019, //alt2
    };

    public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl) {
        if (!(playable.isPlayer())) {
            return;
        }

        L2PcInstance player = (L2PcInstance) playable;

        player.sendActionFailed();

        if (player.isCastingNow()) {
            return;
        }

        if (player.isOutOfControl() || player.isInOlympiadMode()) {
            return;
        }

        if (Config.ENCH_ANTI_CLICK) {
            if (player.getEnchClicks() >= Config.ENCH_ANTI_CLICK_STEP) {
                player.showAntiClickPWD();
                return;
            }
            player.updateEnchClicks();
        }

        /*if(item.isCrystallEnchantScroll())
        player.sendMessage("Crystal Scroll:");*/

        player.setActiveEnchantItem(item);
        player.sendPacket(Static.SELECT_ITEM_TO_ENCHANT);
        //player.sendPacket(new ChooseInventoryItem(item.getItemId()));
        player.sendPacket(new ChooseInventoryItem(item.getItem().getCrystalItemId()));
        return;
    }

    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
