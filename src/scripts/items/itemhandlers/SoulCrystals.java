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

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.SkillTable;
import scripts.items.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.2.4 $ $Date: 2005/08/14 21:31:07 $
 */
public class SoulCrystals implements IItemHandler {
    // First line is for Red Soul Crystals, second is Green and third is Blue Soul Crystals,
    // ordered by ascending level, from 0 to 13...

    private static final int[] ITEM_IDS = {4629, 4630, 4631, 4632, 4633, 4634, 4635, 4636, 4637, 4638, 4639, 5577, 5580, 5908,
        4640, 4641, 4642, 4643, 4644, 4645, 4646, 4647, 4648, 4649, 4650, 5578, 5581, 5911,
        4651, 4652, 4653, 4654, 4655, 4656, 4657, 4658, 4659, 4660, 4661, 5579, 5582, 5914};

    // Our main method, where everything goes on
    @Override
    public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl) {
        if (!(playable.isPlayer())) {
            return;
        }

        L2Object target = playable.getTarget();
        if (target == null) {
            playable.sendActionFailed();
            return;
        }

        if (!(target.isL2Attackable())) {
            playable.sendPacket(Static.INCORRECT_TARGET);
            playable.sendActionFailed();
            return;
        }

        // u can use soul crystal only when target hp goes below 50%
        if (target.getCurrentHp() > target.getMaxHp() / 2.0) {
            playable.sendActionFailed();
            return;
        }

        L2PcInstance activeChar = playable.getPlayer();
        L2Skill skill = SkillTable.getInstance().getInfo(2096, 1);
        if (skill.checkCondition(activeChar, activeChar, false)) {
            activeChar.setCoulCryId(item.getItemId());
            activeChar.useMagic(skill, false, false);
        } else {
            activeChar.sendActionFailed();
        }
    }

    @Override
    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
