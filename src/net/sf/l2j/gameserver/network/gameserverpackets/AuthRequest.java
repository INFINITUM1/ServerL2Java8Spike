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
package net.sf.l2j.gameserver.network.gameserverpackets;

import java.io.IOException;

public class AuthRequest extends GameServerBasePacket {

    /**
     * Format: cccSddb c desired ID c accept alternative ID c reserve Host s
     * ExternalHostName s InetranlHostName d max players d hexid size b hexid
     *
     * @param id
     * @param acceptAlternate
     * @param hexid
     * @param externalHost
     * @param internalHost
     * @param reserveHost
     * @param maxplayer
     */
    public AuthRequest(int id, boolean acceptAlternate, byte[] hexid, String externalHost, String internalHost, int[] ports, boolean reserveHost, int maxplayer, String key, boolean catsguard) {
        writeC(0xaf);
        writeC(id);
        writeC(acceptAlternate ? 0x01 : 0x00);
        writeC(reserveHost ? 0x01 : 0x00);
        writeH(1);
        writeD(maxplayer);
        writeS(key);
        writeD(hexid.length);
        writeB(hexid);
        writeS(externalHost);
        writeS(internalHost);
        writeC(catsguard ? 1 : 0);
        writeH(ports.length);
        for (int PORT_GAME : ports) {
            writeH(PORT_GAME);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.gameserverpackets.GameServerBasePacket#getContent()
     */
    @Override
    public byte[] getContent() throws IOException {
        return getBytes();
    }
}
