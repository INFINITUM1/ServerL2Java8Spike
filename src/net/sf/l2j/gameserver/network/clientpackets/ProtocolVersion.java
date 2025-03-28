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
package net.sf.l2j.gameserver.network.clientpackets;

//import java.util.logging.Logger;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.serverpackets.KeyPacket;
import net.sf.l2j.gameserver.network.serverpackets.SendStatus;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.8.2.8 $ $Date: 2005/04/02 10:43:04 $
 */
public final class ProtocolVersion extends L2GameClientPacket {

    private int _version;

    @Override
    protected void readImpl() {
        _version = readH();
    }

    @Override
    protected void runImpl() {
        //System.out.println("###" + (_client == null));
        // this packet is never encrypted
        if (_version == -2) {
            //  if (Config.DEBUG) _log.info("Ping received");
            // this is just a ping attempt from the new C2 client
            getClient().close(new SendStatus());
            return;
        }

        if (_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION) {
            // _log.info("Client: "+getClient().toString()+" -> Protocol Revision: " + _version + " is invalid. Minimum is "+Config.MIN_PROTOCOL_REVISION+" and Maximum is "+Config.MAX_PROTOCOL_REVISION+" are supported. Closing connection.");
            // _log.warning("Wrong Protocol Version "+_version);
            //if (Config.SHOW_PROTOCOL_VERSIONS)
            //System.out.println("#####L2TOP? "+_version+" FROM"+getClient().toString());
            //System.out.println("##### " + _version + " FROM" + getClient().toString());

            getClient().close(new SendStatus());
            return;
            //getClient().closeNow();
            //return;
        }

        if (Config.CATS_GUARD) {
            sendPacket(new KeyPacket(getClient().enableCrypt(), 1));
            return;
        }
        //System.out.println("##### " + _version + " FROM" + getClient().toString());
        getClient().sendPacket(new KeyPacket(getClient().enableCrypt()));
    }
}
