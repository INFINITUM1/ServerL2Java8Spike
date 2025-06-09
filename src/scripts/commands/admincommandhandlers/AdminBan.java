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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Log;
import net.sf.l2j.gameserver.util.protection.catsguard.CatsGuard;
import scripts.communitybbs.Manager.RegionBBSManager;
import scripts.commands.IAdminCommandHandler;

/**
 * This class handles following admin commands:
 * - ban account_name = changes account access level to -100 and logs him off. If no account is specified, target's account is used.
 * - unban account_name = changes account access level to 0.
 * - jail charname [penalty_time] = jails character. Time specified in minutes. For ever if no time is specified.
 * - unjail charname = Unjails player, teleport him to Floran.
 *
 * @version $Revision: 1.1.6.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminBan implements IAdminCommandHandler {

    private static final String[] ADMIN_COMMANDS = {"admin_ban", "admin_unban", "admin_jail", "admin_unjail", "admin_hwidban"};
    private static final int REQUIRED_LEVEL = Config.GM_BAN;

    public boolean useAdminCommand(String command, L2PcInstance adm) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!(checkLevel(adm.getAccessLevel()))) {
                return false;
            }
        }

        StringTokenizer st = new StringTokenizer(command);
        st.nextToken();
        String account_name = "";
        String player = "";
        L2PcInstance plyr = null;
        if (command.startsWith("admin_ban")) {
            try {
                player = st.nextToken();
                plyr = L2World.getInstance().getPlayer(player);
            } catch (Exception e) {
                L2Object target = adm.getTarget();
                if (target != null && target.isPlayer()) {
                    plyr = (L2PcInstance) target;
                } else {
                    adm.sendAdmResultMessage("Usage: //ban [account_name] (if none, target char's account gets banned)");
                }
            }
            if (plyr != null && plyr.equals(adm)) {
                plyr.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_ON_YOURSELF));
            } else if (plyr == null) {
                account_name = player;
                LoginServerThread.getInstance().sendAccessLevel(account_name, 0);
                adm.sendAdmResultMessage("Ban request sent for account " + account_name + ". If you need a playername based commmand, see //ban_menu");
            } else {
                Olympiad.clearPoints(plyr.getObjectId());
                plyr.setAccountAccesslevel(-100);
                account_name = plyr.getAccountName();
                //RegionBBSManager.getInstance().changeCommunityBoard();
                plyr.logout();
                adm.sendAdmResultMessage("Account " + account_name + " banned.");
            }
        } else if (command.startsWith("admin_unban")) {
            try {
                account_name = st.nextToken();
                LoginServerThread.getInstance().sendAccessLevel(account_name, 0);
                adm.sendAdmResultMessage("Unban request sent for account " + account_name + ". If you need a playername based commmand, see //unban_menu");
            } catch (Exception e) {
                adm.sendAdmResultMessage("Usage: //unban <account_name>");
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        } else if (command.startsWith("admin_jail")) {
            try {
                player = st.nextToken();
                int delay = 0;
                try {
                    delay = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException nfe) {
                    adm.sendAdmResultMessage("Usage: //jail <charname> [penalty_minutes]");
                } catch (NoSuchElementException nsee) {
                }
                L2PcInstance playerObj = L2World.getInstance().getPlayer(player);
                if (playerObj != null) {
                    playerObj.setInJail(true, delay);
                    adm.sendAdmResultMessage("Character " + player + " jailed for " + (delay > 0 ? delay + " minutes." : "ever!"));
                } else {
                    jailOfflinePlayer(adm, player, delay);
                }
            } catch (NoSuchElementException nsee) {
                adm.sendAdmResultMessage("Usage: //jail <charname> [penalty_minutes]");
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        } else if (command.startsWith("admin_unjail")) {
            try {
                player = st.nextToken();
                L2PcInstance playerObj = L2World.getInstance().getPlayer(player);

                if (playerObj != null) {
                    playerObj.setInJail(false, 0);
                    adm.sendAdmResultMessage("Character " + player + " removed from jail");
                } else {
                    unjailOfflinePlayer(adm, player);
                }
            } catch (NoSuchElementException nsee) {
                adm.sendAdmResultMessage("Specify a character name.");
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        } else if (command.equalsIgnoreCase("admin_hwidban")) {
            L2Object target = adm.getTarget();
            if (target != null && target.isPlayer()) {
                plyr = (L2PcInstance) target;
            }

            if (plyr != null && plyr.equals(adm)) {
                plyr.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_ON_YOURSELF));
            } else {
                if (Config.CATS_GUARD) {
                    CatsGuard.getInstance().ban(plyr);
                } else {
                    Log.banHWID(plyr.getHWID(), plyr.getIP(), plyr.getAccountName());
                }
                Olympiad.clearPoints(plyr.getObjectId());
                plyr.logout();
                adm.sendAdmResultMessage("HWID " + plyr.getHWID() + " banned.");
            }
        }
        GMAudit.auditGMAction(adm.getName(), command, player, "");
        return true;
    }

    private void jailOfflinePlayer(L2PcInstance adm, String name, int delay) {
        Connect con = null;
        try {
            con = L2DatabaseFactory.get();

            PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, in_jail=?, jail_timer=? WHERE char_name=?");
            statement.setInt(1, -114356);
            statement.setInt(2, -249645);
            statement.setInt(3, -2984);
            statement.setInt(4, 1);
            statement.setLong(5, delay * 60000L);
            statement.setString(6, name);

            statement.execute();
            int count = statement.getUpdateCount();
            statement.close();

            if (count == 0) {
                adm.sendAdmResultMessage("Character not found!");
            } else {
                adm.sendAdmResultMessage("Character " + name + " jailed for " + (delay > 0 ? delay + " minutes." : "ever!"));
            }
        } catch (SQLException se) {
            adm.sendAdmResultMessage("SQLException while jailing player");
            if (Config.DEBUG) {
                se.printStackTrace();
            }
        } finally {
            try {
                con.close();
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void unjailOfflinePlayer(L2PcInstance adm, String name) {
        Connect con = null;
        try {
            con = L2DatabaseFactory.get();
            PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, in_jail=?, jail_timer=? WHERE char_name=?");
            statement.setInt(1, 17836);
            statement.setInt(2, 170178);
            statement.setInt(3, -3507);
            statement.setInt(4, 0);
            statement.setLong(5, 0);
            statement.setString(6, name);
            statement.execute();
            int count = statement.getUpdateCount();
            statement.close();
            if (count == 0) {
                adm.sendAdmResultMessage("Character not found!");
            } else {
                adm.sendAdmResultMessage("Character " + name + " removed from jail");
            }
        } catch (SQLException se) {
            adm.sendAdmResultMessage("SQLException while jailing player");
            if (Config.DEBUG) {
                se.printStackTrace();
            }
        } finally {
            try {
                con.close();
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }
}
