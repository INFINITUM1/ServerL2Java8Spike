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
public class BlessedSpiritShot implements IItemHandler {
    // all the items ids that this handler knowns

    private static final int[] ITEM_IDS = {3947, 3948, 3949, 3950, 3951, 3952};
    private static final int[] SKILL_IDS = {2061, 2160, 2161, 2162, 2163, 2164};

    private boolean incorrectGrade(int weaponGrade, int itemId) {
        if ((weaponGrade == L2Item.CRYSTAL_NONE && itemId != 3947)
                || (weaponGrade == L2Item.CRYSTAL_D && itemId != 3948)
                || (weaponGrade == L2Item.CRYSTAL_C && itemId != 3949)
                || (weaponGrade == L2Item.CRYSTAL_B && itemId != 3950)
                || (weaponGrade == L2Item.CRYSTAL_A && itemId != 3951)
                || (weaponGrade == L2Item.CRYSTAL_S && itemId != 3952)) {
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
        if (activeChar.isInOlympiadMode()) {
            activeChar.sendPacket(Static.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
            return;
        }

        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
        L2Weapon weaponItem = activeChar.getActiveWeaponItem();

        // Check if Blessed Spiritshot can be used
        if (weaponInst == null || weaponItem.getSpiritShotCount() == 0) {
            //activeChar.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_SPIRITSHOTS));
            return;
        }
        int itemId = item.getItemId();

        // Check if Blessed Spiritshot is already active (it can be charged over Spiritshot)
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

        // Charge Blessed Spiritshot
        weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);

        activeChar.broadcastSoulShotsPacket(new MagicSkillUser(activeChar, activeChar, SKILL_IDS[weaponItem.getCrystalType()], 1, 0, 0));
    }

    @Override
    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
