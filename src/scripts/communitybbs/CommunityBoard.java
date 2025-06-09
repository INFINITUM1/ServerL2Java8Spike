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
package scripts.communitybbs;

import net.sf.l2j.Config;
import scripts.communitybbs.Manager.*;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.PeaceZone;

public class CommunityBoard {

    private static CommunityBoard _instance;

    public CommunityBoard() {
    }

    public static CommunityBoard getInstance() {
        if (_instance == null) {
            _instance = new CommunityBoard();
        }

        return _instance;
    }

    private boolean cantBss(L2PcInstance activeChar) {
        if (activeChar.isDead() || activeChar.isAlikeDead() || activeChar.isInJail() || activeChar.underAttack() || activeChar.getChannel() > 1) {
            return true;
        }

        if (activeChar.isMovementDisabled() || activeChar.isInCombat() || activeChar.isInsideSilenceZone() || activeChar.isInOlympiadMode()) {
            return true;
        }

        if (Config.BBS_ONLY_PEACE && !PeaceZone.getInstance().inPeace(activeChar)) {
            activeChar.sendPacket(Static.BBS_PEACE);
            return true;
        }

        if (activeChar.inEvent() || activeChar.isParalyzed() || activeChar.getPvpFlag() != 0 || (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(activeChar.getName()))) {
            return true;
        }

        return false;
    }

    public void handleCommands(L2GameClient client, String command) {
        L2PcInstance activeChar = client.getActiveChar();
        if (activeChar == null) {
            return;
        }

        if (cantBss(activeChar)) {
            return;
        }

        //System.out.println("##->" + command + "<-##");
        if (Config.COMMUNITY_TYPE.equals("pw")) {
            if (command.startsWith("_bbsmail")) {
                MailBBSManager.getInstance().parsecmd(command, activeChar);
            } else if (command.startsWith("_bbsauc")) {
                AuctionBBSManager.getInstance().parsecmd(command, activeChar);
            } else if (command.startsWith("_bbsmenu")) {
                MenuBBSManager.getInstance().parsecmd(command, activeChar);
            } else {
                CustomBBSManager.getInstance().parsecmd(command, activeChar);
            }
        } else if (Config.COMMUNITY_TYPE.equals("full")) {
            if (command.startsWith("_bbsclan")) {
                ClanBBSManager.getInstance().parsecmd(command, activeChar);
            } else if (command.startsWith("_bbsmemo")) {
                TopicBBSManager.getInstance().parsecmd(command, activeChar);
            } else if (command.startsWith("_bbstopics")) {
                TopicBBSManager.getInstance().parsecmd(command, activeChar);
            } else if (command.startsWith("_bbsposts")) {
                PostBBSManager.getInstance().parsecmd(command, activeChar);
            } else if (command.startsWith("_bbstop")) {
                TopBBSManager.getInstance().parsecmd(command, activeChar);
            } else if (command.startsWith("_bbshome")) {
                TopBBSManager.getInstance().parsecmd(command, activeChar);
            } else if (command.startsWith("_bbsloc")) {
                RegionBBSManager.getInstance().parsecmd(command, activeChar);
            } else {
                activeChar.sendPacket(new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101"));
                activeChar.sendPacket(new ShowBoard(null, "102"));
                activeChar.sendPacket(new ShowBoard(null, "103"));
            }
        } else if (Config.COMMUNITY_TYPE.equals("old")) {
            RegionBBSManager.getInstance().parsecmd(command, activeChar);
        } else {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.CB_OFFLINE));
        }
    }

    public boolean findBypass(String command) {

        //System.out.println("##->" + command + "<-##");
        if (Config.COMMUNITY_TYPE.equals("pw")) {
            if (command.startsWith("_bbsmail")) {
                return true;
            } else if (command.startsWith("_bbsauc")) {
                return true;
            } else if (command.startsWith("_bbsmenu")) {
                return true;
            } else {
                return true;
            }
        } else if (Config.COMMUNITY_TYPE.equals("full")) {
            if (command.startsWith("_bbsclan")) {
                return true;
            } else if (command.startsWith("_bbsmemo")) {
                return true;
            } else if (command.startsWith("_bbstopics")) {
                return true;
            } else if (command.startsWith("_bbsposts")) {
                return true;
            } else if (command.startsWith("_bbstop")) {
                return true;
            } else if (command.startsWith("_bbshome")) {
                return true;
            } else if (command.startsWith("_bbsloc")) {
                return true;
            } else {
                return true;
            }
        } else if (Config.COMMUNITY_TYPE.equals("old")) {
            return true;
        }
        return false;
    }

    /**
     * @param client
     * @param url
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    public void handleWriteCommands(L2GameClient client, String url, String arg1, String arg2, String arg3, String arg4, String arg5) {
        L2PcInstance activeChar = client.getActiveChar();
        if (activeChar == null) {
            return;
        }

        if (Config.COMMUNITY_TYPE.equals("full")) {
            if (url.equals("Topic")) {
                TopicBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
            } else if (url.equals("Post")) {
                PostBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
            } else if (url.equals("Region")) {
                RegionBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
            } else {
                activeChar.sendPacket(new ShowBoard("<html><body><br><br><center>the command: " + url + " is not implemented yet</center><br><br></body></html>", "101"));
                activeChar.sendPacket(new ShowBoard(null, "102"));
                activeChar.sendPacket(new ShowBoard(null, "103"));
            }
        } else if (Config.COMMUNITY_TYPE.equals("old")) {
            RegionBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
        } else {
            activeChar.sendPacket(new ShowBoard("<html><body><br><br><center>The Community board is currently disable</center><br><br></body></html>", "101"));
            activeChar.sendPacket(new ShowBoard(null, "102"));
            activeChar.sendPacket(new ShowBoard(null, "103"));
        }
    }
}
