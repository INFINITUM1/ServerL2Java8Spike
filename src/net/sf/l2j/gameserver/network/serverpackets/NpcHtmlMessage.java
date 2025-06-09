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

import java.util.logging.Logger;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.clientpackets.RequestBypassToServer;

public class NpcHtmlMessage extends L2GameServerPacket {
    // d S
    // d is usually 0, S is the html text starting with <html> and ending with </html>
    //

    private static final Logger _log = Logger.getLogger(RequestBypassToServer.class.getName());
    private int _npcObjId;
    private String _html;

    /**
     * @param _characters
     */
    public static NpcHtmlMessage id(int npcObjId, String text) {
        return new NpcHtmlMessage(npcObjId, text);
    }

    public NpcHtmlMessage(int npcObjId, String text) {
        _npcObjId = npcObjId;
        setHtml(text);
    }

    public static NpcHtmlMessage id(int npcObjId) {
        return new NpcHtmlMessage(npcObjId);
    }

    public NpcHtmlMessage(int npcObjId) {
        _npcObjId = npcObjId;
    }

    /*@Override
     public void runImpl() {
     //System.out.println("##runImpl#1#" + (getClient() == null));
     if (Config.BYPASS_VALIDATION) {
     buildBypassCache(getClient().getActiveChar());
     }
     }*/
    public void setHtml(String text) {
        if (text.length() > 8192) {
            _log.warning("Html is too long! this will crash the client!");
            _html = "<html><body>Html was too long</body></html>";
            return;
        }
        _html = text; // html code must not exceed 8192 bytes
    }

    public boolean setFile(String path) {
        String content = HtmCache.getInstance().getHtm(path);

        if (content == null) {
            setHtml("<html><body>My Text is missing:<br>" + path + "</body></html>");
            _log.warning("missing html page " + path);
            return false;
        }

        setHtml(content);
        return true;
    }

    public void replace(String pattern, String value) {
        _html = _html.replaceAll(pattern, value);
    }

    public void replace(String pattern, int value) {
        _html = _html.replaceAll(pattern, String.valueOf(value));
    }

    public NpcHtmlMessage replaceAndGet(String pattern, String value) {
        _html = _html.replaceAll(pattern, value);
        return this;
    }

    private void buildBypassCache(L2PcInstance activeChar) {
        if (activeChar == null) {
            return;
        }

        activeChar.clearBypass();
        int len = _html.length();
        for (int i = 0; i < len; i++) {
            int start = _html.indexOf("bypass -h", i);
            int finish = _html.indexOf("\"", start);

            if (start < 0 || finish < 0) {
                break;
            }

            start += 10;
            i = start;
            int finish2 = _html.indexOf("$", start);
            if (finish2 < finish && finish2 > 0) {
                activeChar.addBypass2(_html.substring(start, finish2));
            } else {
                activeChar.addBypass(_html.substring(start, finish));
            }
            //System.err.println("["+_html.substring(start, finish)+"]");
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0x0f);

        //System.out.println(_npcObjId);
        writeD(_npcObjId);
        //writeS(_html);
       /* L2PcInstance player = getClient().getActiveChar();
         if (player == null) {
         return;
         }*/
        writeProtectedHTM(getClient().getActiveChar());
        //System.out.println("##3#");
        //Log.add(_html, "HTMLtest");

        //player.cleanBypasses(false);
        //String html = player.encodeBypasses(_html, false);
        //writeProtectedHTM(player.encodeBypasses(_html, false));
        //Log.add(html, "HTMLtest");
        //writeS(html);
        writeD(0x00);
    }

    private void writeProtectedHTM(L2PcInstance player) {
        if (player == null) {
            return;
        }
        player.cleanBypasses(false);
        //String html = player.encodeBypasses(_html, false);
        writeS(player.encodeBypasses(_html, false));
    }
}
