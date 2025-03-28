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
package net.sf.l2j.gameserver.network.loginserverpackets;

/**
 * @author -Wooden-
 *
 */
public class PlayerAuthResponse extends LoginServerBasePacket {

    private String _account;
    private boolean _authed;
    private String _hwid;
    private boolean _email;
    private int _serverId;

    /**
     * @param decrypt
     */
    public PlayerAuthResponse(byte[] decrypt) {
        super(decrypt);

        _account = readS();
        _authed = (readC() == 0 ? false : true);
        _hwid = readS();
        _email = (readC() == 0 ? false : true);
        _serverId = readC();
    }

    /**
     * @return Returns the account.
     */
    public String getAccount() {
        return _account;
    }

    /**
     * @return Returns the authed state.
     */
    public boolean isAuthed() {
        return _authed;
    }

    public String getHWID() {
        return _hwid;
    }

    public boolean hasEmail() {
        return _email;
    }

    public int getServerId() {
        return _serverId;
    }
}