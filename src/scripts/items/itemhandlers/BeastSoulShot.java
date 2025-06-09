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
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * Beast SoulShot Handler
 *
 * @author Tempy
 */
public class BeastSoulShot implements IItemHandler {
    // All the item IDs that this handler knows.

    private static final int[] ITEM_IDS = {6645};

    public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl) {
        if (playable == null) {
            return;
        }

        L2PcInstance activeOwner = null;
        if (playable instanceof L2Summon) {
            activeOwner = ((L2Summon) playable).getOwner();
            activeOwner.sendPacket(Static.PET_CANNOT_USE_ITEM);
            return;
        } else if (playable.isPlayer()) {
            activeOwner = (L2PcInstance) playable;
        }

        if (activeOwner == null) {
            return;
        }
        L2Summon activePet = activeOwner.getPet();

        if (activePet == null) {
            activeOwner.sendPacket(Static.PETS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
            return;
        }

        if (activePet.isDead()) {
            activeOwner.sendPacket(Static.SOULSHOTS_AND_SPIRITSHOTS_ARE_NOT_AVAILABLE_FOR_A_DEAD_PET);
            return;
        }

        //int itemId = 6645;
        //int shotConsumption = 1;

        if (activePet.getChargedSoulShot() != L2ItemInstance.CHARGED_NONE) {
            return;
        }

        activePet.setChargedSoulShot(L2ItemInstance.CHARGED_SOULSHOT);

        // Pet uses the power of spirit.
        activeOwner.sendPacket(Static.PET_USE_THE_POWER_OF_SPIRIT);

        Broadcast.toSelfAndKnownPlayersInRadius(activeOwner, new MagicSkillUser(activePet, activePet, 2033, 1, 0, 0), 360000/*600*/);
    }

    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
