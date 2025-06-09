/* This program is free software; you can redistribute it and/or modify
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

//import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.util.protection.catsguard.CatsGuard;

/**
 * @author zabbix
 * Lets drink to code!
 *
 * Unknown Packet:ca
 * 0000: 45 00 01 00 1e 37 a2 f5 00 00 00 00 00 00 00 00    E....7..........
 */
public class GameGuardReply extends L2GameClientPacket {

    private int[] _reply = new int[4];

    @Override
    protected void readImpl() {
        //System.out.println("###gg1#");
        if (Config.CATS_GUARD) {
            //System.out.println("###gg2#");
            if ((CatsGuard.getInstance().isEnabled()) && (getClient().getHWid() == null || getClient().getHWid().equalsIgnoreCase("none"))) {
                //System.out.println("###gg3#");
                this._reply[0] = readD();
                this._reply[1] = readD();
                this._reply[2] = readD();
                this._reply[3] = readD();
            } else {
                //System.out.println("###gg4#");
                byte[] b = new byte[this._buf.remaining()];
                readB(b);
            }
            //System.out.println("###gg5#");
        } else {
            _reply[0] = readD();
            _reply[1] = readD();
            _reply[2] = readD();
            _reply[3] = readD();
        }
    }

    @Override
    protected void runImpl() {
        //System.out.println("###gg6#");
        if (Config.CATS_GUARD) {
            //System.out.println("###gg7#");
            if (CatsGuard.getInstance().isEnabled()) {
                //System.out.println("###gg8#");
                CatsGuard.getInstance().initSession(getClient(), this._reply);
            }
            //System.out.println("###gg9#");
            return;
        }
        //System.out.println("###gg10#");
        //L2PcInstance player = getClient().getActiveChar();
        if (getClient() == null) {
            return;
        }

        if (!getClient().checkGameGuardReply(_reply)) {
            return;
        }

        getClient().setGameGuardOk(true);
    }
}
