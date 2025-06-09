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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.Shutdown;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import scripts.commands.IAdminCommandHandler;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.gameserverpackets.ServerStatus;
import net.sf.l2j.gameserver.network.serverpackets.ServerClose;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * This class handles following admin commands: - server_shutdown [sec] = shows
 * menu or shuts down server in sec seconds
 *
 * @version $Revision: 1.5.2.1.2.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminShutdown implements IAdminCommandHandler {
    //private static Logger _log = Logger.getLogger(AdminShutdown.class.getName());

    private static final String[] ADMIN_COMMANDS = {"admin_server_shutdown", "admin_server_restart", "admin_server_abort", "admin_server_gc"};
    private static final int REQUIRED_LEVEL = Config.GM_RESTART;

    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) {
                return false;
            }
        }

        if (command.startsWith("admin_server_shutdown")) {
            try {
                int val = Integer.parseInt(command.substring(22));
                serverShutdown(activeChar, val, false);
            } catch (StringIndexOutOfBoundsException e) {
                sendHtmlForm(activeChar);
            }
        } else if (command.startsWith("admin_server_restart")) {
            try {
                int val = Integer.parseInt(command.substring(21));
                serverShutdown(activeChar, val, true);
            } catch (StringIndexOutOfBoundsException e) {
                sendHtmlForm(activeChar);
            }
        } else if (command.startsWith("admin_server_abort")) {
            serverAbort(activeChar);
        } else if (command.startsWith("admin_server_gc")) {
            System.out.println("Starting clearing memory... current: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024) + "MB...");
            System.gc();
            System.out.println("...done; now: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024) + "MB");
        }

        return true;
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }

    private void sendHtmlForm(L2PcInstance activeChar) {
        NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
        int t = GameTimeController.getInstance().getGameTime();
        int h = t / 60;
        int m = t % 60;
        SimpleDateFormat format = new SimpleDateFormat("h:mm a");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        adminReply.setFile("data/html/admin/shutdown.htm");
        adminReply.replace("%count%", String.valueOf(L2World.getInstance().getAllPlayersCount()));
        adminReply.replace("%count_hwid%", String.valueOf(L2World.getInstance().getAllPlayersCountHwid()));
        adminReply.replace("%used%", String.valueOf(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        adminReply.replace("%xp%", String.valueOf(Config.RATE_XP));
        adminReply.replace("%sp%", String.valueOf(Config.RATE_SP));
        adminReply.replace("%adena%", String.valueOf(Config.RATE_DROP_ADENA));
        adminReply.replace("%drop%", String.valueOf(Config.RATE_DROP_ITEMS));
        adminReply.replace("%time%", String.valueOf(format.format(cal.getTime())));
        activeChar.sendPacket(adminReply);
    }

    private void serverShutdown(L2PcInstance activeChar, int seconds, boolean restart) {
        Announcements.getInstance().announceToAll("Внимание! Сервер будет выключен!");
        Announcements.getInstance().announceToAll("Выйдите из игры, что-бы не потерять достигнутых результатов.");
        Announcements.getInstance().announceToAll("Приносим извинения за возможные неудобства.");
        Broadcast.toAllOnlinePlayers(SystemMessage.id(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS).addNumber(seconds));
        Shutdown.getInstance().startShutdown(activeChar, seconds, restart);
    }

    private void serverAbort(L2PcInstance activeChar) {
        Shutdown.getInstance().abort(activeChar);
    }
}
