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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class RecipeShopMsg extends L2GameServerPacket {

    private int _chaObjectId;
    private String _chaStoreName;

    public RecipeShopMsg(L2PcInstance player) {
        if (player.getCreateList() == null || player.getCreateList().getStoreName() == null) {
            return;
        }
        _chaObjectId = player.getObjectId();
        _chaStoreName = player.getCreateList().getStoreName();
    }

    @Override
    protected final void writeImpl() {
        writeC(0xdb);
        writeD(_chaObjectId);
        writeS(_chaStoreName);
    }
}
