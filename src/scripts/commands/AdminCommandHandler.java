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
package scripts.commands;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import scripts.commands.admincommandhandlers.*;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.1.4.5 $ $Date: 2005/03/27 15:30:09 $
 */
public final class AdminCommandHandler {

    private static final Logger _log = AbstractLogger.getLogger(AdminCommandHandler.class.getName());
    private static AdminCommandHandler _instance;
    private Map<String, IAdminCommandHandler> _datatable;
    //Alt privileges setting
    private static final Logger _priviLog = Logger.getLogger("AltPrivilegesAdmin");
    private static FastMap<String, Integer> _privileges;

    public static AdminCommandHandler getInstance() {
        if (_instance == null) {
            _instance = new AdminCommandHandler();
        }
        return _instance;
    }

    private AdminCommandHandler() {
        _datatable = new FastMap<String, IAdminCommandHandler>();
        registerAdminCommandHandler(new AdminAdmin());
        registerAdminCommandHandler(new AdminInvul());
        registerAdminCommandHandler(new AdminDelete());
        registerAdminCommandHandler(new AdminKill());
        registerAdminCommandHandler(new AdminTarget());
        registerAdminCommandHandler(new AdminShop());
        registerAdminCommandHandler(new AdminAnnouncements());
        registerAdminCommandHandler(new AdminCreateItem());
        registerAdminCommandHandler(new AdminHeal());
        registerAdminCommandHandler(new AdminHelpPage());
        registerAdminCommandHandler(new AdminShutdown());
        registerAdminCommandHandler(new AdminSpawn());
        registerAdminCommandHandler(new AdminSkill());
        registerAdminCommandHandler(new AdminExpSp());
        registerAdminCommandHandler(new AdminEventEngine());
        registerAdminCommandHandler(new AdminGmChat());
        registerAdminCommandHandler(new AdminEditChar());
        registerAdminCommandHandler(new AdminGm());
        registerAdminCommandHandler(new AdminTeleport());
        registerAdminCommandHandler(new AdminRepairChar());
        registerAdminCommandHandler(new AdminChangeAccessLevel());
        registerAdminCommandHandler(new AdminBan());
        registerAdminCommandHandler(new AdminPolymorph());
        registerAdminCommandHandler(new AdminBanChat());
        registerAdminCommandHandler(new AdminKick());
        registerAdminCommandHandler(new AdminMonsterRace());
        registerAdminCommandHandler(new AdminEditNpc());
        registerAdminCommandHandler(new AdminFightCalculator());
        registerAdminCommandHandler(new AdminMenu());
        registerAdminCommandHandler(new AdminSiege());
        registerAdminCommandHandler(new AdminPathNode());
        registerAdminCommandHandler(new AdminPetition());
        registerAdminCommandHandler(new AdminPForge());
        registerAdminCommandHandler(new AdminBBS());
        registerAdminCommandHandler(new AdminEffects());
        registerAdminCommandHandler(new AdminDoorControl());
        registerAdminCommandHandler(new AdminTest());
        registerAdminCommandHandler(new AdminEnchant());
        registerAdminCommandHandler(new AdminMobGroup());
        registerAdminCommandHandler(new AdminRes());
        registerAdminCommandHandler(new AdminMammon());
        registerAdminCommandHandler(new AdminUnblockIp());
        registerAdminCommandHandler(new AdminPledge());
        registerAdminCommandHandler(new AdminRideWyvern());
        registerAdminCommandHandler(new AdminLoc());
        registerAdminCommandHandler(new AdminLogin());
        registerAdminCommandHandler(new AdminCache());
        registerAdminCommandHandler(new AdminLevel());
        registerAdminCommandHandler(new AdminQuest());
        registerAdminCommandHandler(new AdminZone());
        registerAdminCommandHandler(new AdminCursedWeapons());
        registerAdminCommandHandler(new AdminGeodata());
        registerAdminCommandHandler(new AdminGeoEditor());
        registerAdminCommandHandler(new AdminManor());
        registerAdminCommandHandler(new AdminTvTEvent());
        registerAdminCommandHandler(new AdminReload());
        registerAdminCommandHandler(new AdminDonator());
        registerAdminCommandHandler(new AdminHero());
        registerAdminCommandHandler(new AdminNoble());
        registerAdminCommandHandler(new AdminCountdown());
        registerAdminCommandHandler(new AdminEvent());
        _log.config("AdminCommandHandler: Loaded " + _datatable.size() + " handlers.");

    }

    public void registerAdminCommandHandler(IAdminCommandHandler handler) {
        String[] ids = handler.getAdminCommandList();
        for (int i = 0; i < ids.length; i++) {
            //if (Config.DEBUG) _log.fine("Adding handler for command "+ids[i]);
            _datatable.put(ids[i], handler);
        }
    }

    public IAdminCommandHandler getAdminCommandHandler(String adminCommand) {
        String command = adminCommand;
        if (adminCommand.indexOf(" ") != -1) {
            command = adminCommand.substring(0, adminCommand.indexOf(" "));
        }
        //if (Config.DEBUG)
        //_log.fine("getting handler for command: "+command+" -> "+(_datatable.get(command) != null));
        return _datatable.get(command);
    }

    public IAdminCommandHandler getEGMCommandHandler(L2PcInstance player, String adminCommand) {
        System.out.println("###" + adminCommand);
        String command = adminCommand;
        if (adminCommand.indexOf(" ") != -1) {
            command = adminCommand.substring(0, adminCommand.indexOf(" "));
        }

        System.out.println("#####" + command);

        if (command.startsWith("admin_announce") || command.startsWith("admin_banchat") || command.startsWith("admin_jail") || command.startsWith("admin_kick")
                || command.startsWith("admin_admin_heal") || command.startsWith("admin_admin_kill") || command.startsWith("admin_admin_res") || command.startsWith("admin_silence")
                || command.startsWith("admin_unbanchat") || command.startsWith("admin_unjail") || command.startsWith("admin_open") || command.startsWith("admin_close")
                || command.startsWith("admin_set_level") || command.startsWith("admin_rec") || command.startsWith("admin_vis") || command.startsWith("admin_invis")
                || command.startsWith("admin_recall") || command.startsWith("admin_recall_char_menu") || command.startsWith("admin_teleportto") || command.startsWith("admin_move_to")
                || command.startsWith("admin_teleport_character") || command.startsWith("admin_tele") || command.startsWith("admin_teleto") || command.startsWith("admin_setnoble")) {
            return _datatable.get(command);
        }

        return null;
    }

    /**
     * @return
     */
    public int size() {
        return _datatable.size();
    }

    public final boolean checkPrivileges(L2PcInstance player, String adminCommand) {
        //Only a GM can execute a admin command
        if (!player.isGM()) {
            return false;
        }

        String command = adminCommand;
        if (adminCommand.indexOf(" ") != -1) {
            command = adminCommand.substring(0, adminCommand.indexOf(" "));
        }
        if (player.getAccessLevel() == 75) {
            if (command.startsWith("announce") || command.startsWith("banchat") || command.startsWith("jail") || command.startsWith("kick")
                    || command.startsWith("heal") || command.startsWith("kill") || command.startsWith("res") || command.startsWith("silence")
                    || command.startsWith("unbanchat") || command.startsWith("unjail") || command.startsWith("open") || command.startsWith("close")
                    || command.startsWith("set_level") || command.startsWith("rec") || command.startsWith("vis") || command.startsWith("invis")
                    || command.startsWith("recall") || command.startsWith("recall_char_menu") || command.startsWith("teleportto") || command.startsWith("move_to")
                    || command.startsWith("teleport_character") || command.startsWith("tele") || command.startsWith("teleto") || command.startsWith("setnoble")) {
                return true;
            }

            return false;
        }

        //Skip special privileges handler?
        if (!Config.ALT_PRIVILEGES_ADMIN || Config.EVERYBODY_HAS_ADMIN_RIGHTS) {
            return true;
        }

        if (_privileges == null) {
            _privileges = new FastMap<String, Integer>();
        }

        //The command not exists
        if (!_datatable.containsKey(command)) {
            return false;
        }

        int requireLevel = 0;

        if (!_privileges.containsKey(command)) {
            //Try to loaded the command config
            boolean isLoaded = false;

            InputStream is = null;
            try {
                Properties Settings = new Properties();
                is = new FileInputStream(Config.COMMAND_PRIVILEGES_FILE);
                Settings.load(is);

                String stringLevel = Settings.getProperty(command);

                if (stringLevel != null) {
                    isLoaded = true;
                    requireLevel = Integer.parseInt(stringLevel);
                }
            } catch (Exception e) {
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (Exception ignored) {
                    //
                }
            }

            //Secure level?
            if (!isLoaded) {
                if (Config.ALT_PRIVILEGES_SECURE_CHECK) {
                    _priviLog.info("The command '" + command + "' haven't got a entry in the configuration file. The command cannot be executed!!");
                    return false;
                }

                requireLevel = Config.ALT_PRIVILEGES_DEFAULT_LEVEL;
            }

            _privileges.put(command, requireLevel);
        } else {
            requireLevel = _privileges.get(command);
        }

        if (player.getAccessLevel() < requireLevel) {
            _priviLog.warning("<GM>" + player.getName() + ": have not access level to execute the command '" + command + "'");
            return false;
        }

        return true;
    }
}
