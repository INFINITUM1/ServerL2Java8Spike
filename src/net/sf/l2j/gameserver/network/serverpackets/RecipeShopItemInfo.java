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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * ddddd
 *
 * @version $Revision: 1.1.2.3.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class RecipeShopItemInfo extends L2GameServerPacket {

    private int _recipeId, _shopId, curMp, maxMp;
    private int _success = 0xFFFFFFFF;
    private boolean can_writeImpl = false;

    public RecipeShopItemInfo(L2PcInstance player, int shopId, int recipeId) {
        L2PcInstance manufacturer = L2World.getInstance().getPlayer(shopId);
        if (manufacturer == null) {
            return;
        }

        _shopId = shopId;
        _recipeId = recipeId;
        curMp = (int) manufacturer.getCurrentMp();
        maxMp = (int) manufacturer.getMaxMp();
        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        writeC(0xda);
        writeD(_shopId);
        writeD(_recipeId);
        writeD(curMp);
        writeD(maxMp);
        writeD(_success);
    }
}
