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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2Weapon;
import scripts.items.IItemHandler;

/**
 * This class ...
 *
 * @version $Revision: 1.2.4.4 $ $Date: 2005/03/27 15:30:07 $
 */
public class SoulShots implements IItemHandler {
    // All the item IDs that this handler knows.

    private static final int[] ITEM_IDS = {5789, 1835, 1463, 1464, 1465, 1466, 1467};
    private static final int[] SKILL_IDS = {2039, 2150, 2151, 2152, 2153, 2154};

    private boolean incorrectGrade(int weaponGrade, int itemId) {
        if ((weaponGrade == L2Item.CRYSTAL_NONE && itemId != 5789 && itemId != 1835)
                || (weaponGrade == L2Item.CRYSTAL_D && itemId != 1463)
                || (weaponGrade == L2Item.CRYSTAL_C && itemId != 1464)
                || (weaponGrade == L2Item.CRYSTAL_B && itemId != 1465)
                || (weaponGrade == L2Item.CRYSTAL_A && itemId != 1466)
                || (weaponGrade == L2Item.CRYSTAL_S && itemId != 1467)) {
            //activeChar.sendPacket(SystemMessage.id(SystemMessageId.SOULSHOTS_GRADE_MISMATCH));
            return true;
        }
        return false;
    }

    @Override
    public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl) {
        if (!(playable.isPlayer())) {
            return;
        }

        L2PcInstance activeChar = playable.getPlayer();
        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
        L2Weapon weaponItem = activeChar.getActiveWeaponItem();
        int itemId = item.getItemId();

        // Check if Soulshot can be used
        if (weaponInst == null || weaponItem.getSoulShotCount() == 0) {
            //activeChar.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_SOULSHOTS));
            return;
        }

        if (weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE) {
            return;
        }

        // Check for correct grade
        if (incorrectGrade(weaponItem.getCrystalType(), itemId)) {
            return;
        }

        /*ReentrantLock ss = activeChar.soulShotLock;//.lock();
        ss.lock();
        try
        {*/
        // Check if Soulshot is already active

        // Consume Soulshots if player has enough of them
        	/*int saSSCount = (int)activeChar.getStat().calcStat(Stats.SOULSHOT_COUNT, 0, null, null);
        int SSCount = saSSCount == 0 ? weaponItem.getSoulShotCount() : saSSCount;
        
        if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), SSCount, null, false))
        {
        if(activeChar.getAutoSoulShot().containsKey(itemId))
        {
        activeChar.removeAutoSoulShot(itemId);
        activeChar.sendPacket(new ExAutoSoulShot(itemId, 0));
        
        SystemMessage sm = SystemMessage.id(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
        sm.addString(item.getItem().getName());
        activeChar.sendPacket(sm);
        }
        else activeChar.sendPacket(SystemMessage.id(SystemMessageId.NOT_ENOUGH_SOULSHOTS));
        return;
        }*/

        int saSSCount = (int) activeChar.getStat().calcStat(Stats.SOULSHOT_COUNT, 0, null, null);
        int SSCount = saSSCount == 0 ? weaponItem.getSoulShotCount() : saSSCount;
        if (Config.USE_SOULSHOTS && !activeChar.destroyItemByItemId("Consume", itemId, SSCount, activeChar, false)) {
            activeChar.removeAutoSoulShot(itemId);
            activeChar.sendPacket(new ExAutoSoulShot(itemId, 0));
            activeChar.sendPacket(Static.NOT_ENOUGH_SOULSHOTS);
            return;
        }

        if (activeChar.showSoulShotsAnim()) {
            activeChar.sendPacket(Static.ENABLED_SOULSHOT);
        }

        // Charge soulshot
        weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT);
        /*}
        finally
        {
        ss.unlock();
        }*/

        activeChar.broadcastSoulShotsPacket(new MagicSkillUser(activeChar, activeChar, SKILL_IDS[weaponItem.getCrystalType()], 1, 0, 0));
    }

    @Override
    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
