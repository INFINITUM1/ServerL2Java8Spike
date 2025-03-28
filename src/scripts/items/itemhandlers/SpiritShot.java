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
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2Weapon;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.5 $ $Date: 2005/03/27 15:30:07 $
 */
public class SpiritShot implements IItemHandler {
    // All the item IDs that this handler knows.

    private static final int[] ITEM_IDS = {5790, 2509, 2510, 2511, 2512, 2513, 2514};
    private static final int[] SKILL_IDS = {2061, 2155, 2156, 2157, 2158, 2159};

    private boolean incorrectGrade(int weaponGrade, int itemId) {
        if ((weaponGrade == L2Item.CRYSTAL_NONE && itemId != 5790 && itemId != 2509)
                || (weaponGrade == L2Item.CRYSTAL_D && itemId != 2510)
                || (weaponGrade == L2Item.CRYSTAL_C && itemId != 2511)
                || (weaponGrade == L2Item.CRYSTAL_B && itemId != 2512)
                || (weaponGrade == L2Item.CRYSTAL_A && itemId != 2513)
                || (weaponGrade == L2Item.CRYSTAL_S && itemId != 2514)) {
            //activeChar.sendPacket(SystemMessage.id(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH));
            return true;
        }
        return false;
    }

    @Override
    public synchronized void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl) {
        if (!(playable.isPlayer())) {
            return;
        }

        L2PcInstance activeChar = playable.getPlayer();
        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
        L2Weapon weaponItem = activeChar.getActiveWeaponItem();
        int itemId = item.getItemId();

        // Check if Spiritshot can be used
        if (weaponInst == null || weaponItem.getSpiritShotCount() == 0) {
            //activeChar.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_SPIRITSHOTS));
            return;
        }

        // Check if Spiritshot is already active
        if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE) {
            return;
        }

        // Check for correct grade
        if (incorrectGrade(weaponItem.getCrystalType(), itemId)) {
            return;
        }

        if (Config.USE_SOULSHOTS && !activeChar.destroyItemByItemId("Consume", itemId, weaponItem.getSpiritShotCount(), activeChar, false)) {
            activeChar.removeAutoSoulShot(itemId);
            activeChar.sendPacket(new ExAutoSoulShot(itemId, 0));
            activeChar.sendPacket(Static.NOT_ENOUGH_SPIRITSHOTS);
            return;
        }

        if (activeChar.showSoulShotsAnim()) {
            activeChar.sendPacket(Static.ENABLED_SPIRITSHOT);
        }

        // Charge Spiritshot
        weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);

        activeChar.broadcastSoulShotsPacket(new MagicSkillUser(activeChar, activeChar, SKILL_IDS[weaponItem.getCrystalType()], 1, 0, 0));

    }

    @Override
    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
