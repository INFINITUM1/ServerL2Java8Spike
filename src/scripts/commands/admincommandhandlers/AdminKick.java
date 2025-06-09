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
package scripts.commands.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import scripts.communitybbs.Manager.RegionBBSManager;
import scripts.commands.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.LeaveWorld;

public class AdminKick implements IAdminCommandHandler {

    private static final String[] ADMIN_COMMANDS = {"admin_kick", "admin_kick_non_gm", "admin_sniffspy"};
    private static final int REQUIRED_LEVEL = Config.GM_KICK;

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) {
                return false;
            }
        }

        String target = (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

        if (command.startsWith("admin_kick")) {
            StringTokenizer st = new StringTokenizer(command);
            if (st.countTokens() > 1) {
                st.nextToken();
                String player = st.nextToken();
                L2PcInstance plyr = L2World.getInstance().getPlayer(player);
                if (plyr != null) {
                    if (plyr.isFantome())
                    {
                        plyr.decayMe();
                        L2World.getInstance().removePlayer(plyr);
                    }
                    else
                    {
                        plyr.kick();
                    }
                    activeChar.sendAdmResultMessage("You kicked " + plyr.getName() + " from the game.");
                    //RegionBBSManager.getInstance().changeCommunityBoard();
                }
            }
        } else if (command.startsWith("admin_kick_non_gm")) {
            int counter = 0;
            for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                if (!player.isGM()) {
                    counter++;
                    player.sendPacket(new LeaveWorld());
                    player.logout();
                    RegionBBSManager.getInstance().changeCommunityBoard();
                }
            }
            activeChar.sendAdmResultMessage("Kicked " + counter + " players");
        } else if (command.startsWith("admin_sniffspy")) {
            StringTokenizer st = new StringTokenizer(command);
            if (st.countTokens() > 1) {
                st.nextToken();
                String player = st.nextToken();
                L2PcInstance plyr = L2World.getInstance().getPlayer(player);
                if (plyr != null) {
                    if (plyr.isSpyPckt()) {
                        plyr.setSpyPacket(false);
                        activeChar.sendAdmResultMessage("Выключен лог пакетов игрока: " + plyr.getName());
                    } else {
                        plyr.setSpyPacket(true);
                        activeChar.sendAdmResultMessage("Включен лог пакетов игрока: " + plyr.getName());
                    }
                    //RegionBBSManager.getInstance().changeCommunityBoard();
                }
            }
        }
        return true;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }
}
