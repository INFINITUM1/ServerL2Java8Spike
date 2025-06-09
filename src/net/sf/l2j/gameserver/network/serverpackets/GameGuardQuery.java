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
 * @author zabbix
 * Lets drink to code!
 */
public class GameGuardQuery extends L2GameServerPacket {

    public GameGuardQuery() {
        if (!Config.CATS_GUARD) {
            // Lets make user as gg-unauthorized
            // We will set him as ggOK after reply fromclient
            // or kick
            getClient().setGameGuardOk(false);
        }
    }

    @Override
    public void writeImpl() {
        if (Config.CATS_GUARD) {
            writeC(0xF9);
            return;
        }
        writeC(0xf9);
    }
}
