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
package scripts.communitybbs.Manager;

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public abstract class BaseBBSManager {

    public static final String PWHTML = "data/html/CommunityBoard/pw/";
    public static HtmCache _hc = HtmCache.getInstance();

    public abstract void parsecmd(String command, L2PcInstance activeChar);

    public abstract void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar);

    protected void separateAndSend(String html, L2PcInstance pl) {
        if (html == null) {
            return;
        }
        if (html.length() < 4090) {
            pl.sendPacket(new ShowBoard(html, "101"));
            pl.sendPacket(new ShowBoard(null, "102"));
            pl.sendPacket(new ShowBoard(null, "103"));

        } else if (html.length() < 8180) {
            pl.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
            pl.sendPacket(new ShowBoard(html.substring(4090, html.length()), "102"));
            pl.sendPacket(new ShowBoard(null, "103"));

        } else if (html.length() < 12270) {
            pl.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
            pl.sendPacket(new ShowBoard(html.substring(4090, 8180), "102"));
            pl.sendPacket(new ShowBoard(html.substring(8180, html.length()), "103"));
        }
    }

    /**
     * @param html
     */
    protected void send1001(String html, L2PcInstance player) {
        if (html.length() < 8180) {
            player.sendPacket(new ShowBoard(html, "1001"));
        }
    }

    /**
     * @param i
     */
    protected void send1002(L2PcInstance player) {
        send1002(player, " ", " ", "0");
    }

    /**
     * @param activeChar
     * @param string
     * @param string2
     */
    protected void send1002(L2PcInstance player, String string, String string2, String string3) {
        List<String> _arg = new FastList<String>();
        _arg.add("0");
        _arg.add("0");
        _arg.add("0");
        _arg.add("0");
        _arg.add("0");
        _arg.add("0");
        _arg.add(player.getName());
        _arg.add(Integer.toString(player.getObjectId()));
        _arg.add(player.getAccountName());
        _arg.add("9");
        _arg.add(string2);
        _arg.add(string2);
        _arg.add(string);
        _arg.add(string3);
        _arg.add(string3);
        _arg.add("0");
        _arg.add("0");
        player.sendPacket(new ShowBoard(_arg));
    }

    public static String getPwHtm(String page) {
        return _hc.getHtm(PWHTML + page + ".htm");
    }
}
