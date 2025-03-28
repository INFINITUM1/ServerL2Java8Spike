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

import net.sf.l2j.Config;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public final class KeyPacket extends L2GameServerPacket {

    private byte[] _key;
    @SuppressWarnings("unused")
    private int _id;

    public KeyPacket(byte[] key) {
        _key = key;
    }

    public KeyPacket(byte[] key, int id) {
        _key = key;
        _id = id;
    }

    @Override
    public void writeImpl() {
        if (Config.CATS_GUARD) {
            writeC(0x00);
            writeC(0x01); //0 - wrong protocol, 1 - protocol ok
            writeB(_key);
            writeD(0x01); // server id
            writeC(0x01);
            return;
        }

        writeC(0x00);
        writeC(0x01);
        writeB(_key);
        writeD(0x01);
        writeD(0x01);
    }
}
