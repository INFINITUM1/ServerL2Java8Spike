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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package scripts.commands.admincommandhandlers;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import scripts.commands.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.Manager;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.TimeLogger;

/**
 * @author KidZor
 */
public class AdminReload implements IAdminCommandHandler {

    private static final String[] ADMIN_COMMANDS = {"admin_reload", "admin_reload_home", "admin_config_reload"};

    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (command.startsWith("admin_reload_home")) {
            showWelcome(activeChar);
        } else if (command.startsWith("admin_reload")) {
            StringTokenizer st = new StringTokenizer(command);
            st.nextToken();
            try {
                String type = st.nextToken();
                if (type.equals("multisell")) {
                    L2Multisell.getInstance().reload();
                    activeChar.sendAdmResultMessage("MULTISELL: reloaded");
                } else if (type.startsWith("teleport")) {
                    TeleportLocationTable.getInstance().reloadAll();
                    activeChar.sendAdmResultMessage("TELEPORTS: reloaded");
                } else if (type.startsWith("skill")) {
                    SkillTable.getInstance().reload();
                    activeChar.sendAdmResultMessage("SKILLS: reloaded");
                } else if (type.equals("npc")) {
                    NpcTable.getInstance().reloadAllNpc();
                    activeChar.sendAdmResultMessage("NPC: reloaded");
                } else if (type.startsWith("htm")) {
                    HtmCache.getInstance().reload();
                    Static.updateHtm();
                    //activeChar.sendAdmResultMessage("Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage()  + " megabytes on " + HtmCache.getInstance().getLoadedFiles() + " files loaded");
                    activeChar.sendAdmResultMessage("HTML: reloaded");
                } else if (type.startsWith("item")) {
                    ItemTable.getInstance().reload();
                    activeChar.sendAdmResultMessage("ITEMS: reloaded");
                } else if (type.startsWith("instancemanager")) {
                    Manager.reloadAll();
                    activeChar.sendAdmResultMessage("All instance manager has been reloaded");
                } else if (type.startsWith("npcwalkers")) {
                    NpcWalkerRoutesTable.getInstance().load();
                    activeChar.sendAdmResultMessage("All NPC walker routes have been reloaded");
                } else if (type.equals("configs")) {
                    showConfigWindow(activeChar);
                    return true;
                } else if (type.equals("tradelist")) {
                    TradeController.reload();
                    activeChar.sendAdmResultMessage("TRADE LIST: reloaded.");
                } else if (type.equals("bosses")) {
                    GrandBossManager.getInstance().loadBosses();
                    activeChar.sendAdmResultMessage("GrandBoss Table reloaded.");
                }
            } catch (Exception e) {
                activeChar.sendAdmResultMessage("Usage:  //reload <type>");
            }
            showWelcome(activeChar);
        } else if (command.startsWith("admin_config_reload")) {
            String cfg = "-все конфиги-";
            switch (Integer.parseInt(command.substring(19).trim())) {
                case 0:
                    Config.load(true);
                    break;
                case 1:
                    cfg = "altsettings";
                    Config.loadAltSettingCfg();
                    break;
                case 2:
                    cfg = "commands";
                    Config.loadCommandsCfg();
                    break;
                case 3:
                    cfg = "enchants";
                    Config.loadEnchantCfg();
                    break;
                case 4:
                    cfg = "events";
                    Config.loadEventsCfg();
                    break;
                case 5:
                    cfg = "fakeplayers";
                    Config.loadFakeCfg();
                    break;
                case 6:
                    cfg = "geodata";
                    Config.loadGeoDataCfg();
                    break;
                case 7:
                    cfg = "l2custom";
                    Config.loadCustomCfg();
                    break;
                case 8:
                    cfg = "npc";
                    Config.loadNpcCfg();
                    break;
                case 9:
                    cfg = "options";
                    Config.loadOptionsCfg();
                    break;
                case 10:
                    cfg = "other";
                    Config.loadOtherCfg();
                    break;
                case 11:
                    cfg = "pvp";
                    Config.loadPvpCfg();
                    break;
                case 12:
                    cfg = "rates";
                    Config.loadRatesCfg();
                    break;
                case 13:
                    cfg = "server";
                    Config.loadServerCfg();
                    break;
                case 14:
                    cfg = "services";
                    Config.loadServicesCfg();
                    break;
            }
            showConfigWindow(activeChar);
            activeChar.sendAdmResultMessage("Конфиг " + cfg + ".cfg перезагружен.");
            System.out.println(TimeLogger.getLogTime() + "Config [RELOAD], " + cfg + ".cfg reloaded.");
        }
        return true;
    }

    /**
     * send reload page
     * @param admin
     */
    private void showWelcome(L2PcInstance player) {
        String warning = "msg=\"Данным действием, вы подтверждаете, что можете вызвать утечку памяти и падение сервера.\"";
        NpcHtmlMessage menu = NpcHtmlMessage.id(5);
        TextBuilder replyMSG = new TextBuilder("<html><body><font color=LEVEL>Перезагрузка данных сервера<br></font><br1>");
        replyMSG.append("<a action=\"bypass -h admin_reload configs\">Configs...</a> <font color=\"777777\">&nbsp;//Конфиги</font><br1>");
        replyMSG.append("<a action=\"bypass -h admin_reload htm\" " + warning + ">Html</a> <font color=\"777777\">&nbsp;//Диалоги</font><br1>");
        replyMSG.append("<a action=\"bypass -h admin_reload item\" " + warning + ">Item Tabels</a> <font color=\"777777\">&nbsp;//Шмотки</font><br1>");
        replyMSG.append("<a action=\"bypass -h admin_reload multisell\" " + warning + ">Multisell</a> <font color=\"777777\">&nbsp;//Мультиселлы</font><br1>");
        replyMSG.append("<a action=\"bypass -h admin_reload npc\" " + warning + ">Npcs</a> <font color=\"777777\">&nbsp;//Нпц</font><br1>");
        replyMSG.append("<a action=\"bypass -h admin_reload skill\" " + warning + ">Skills</a> <font color=\"777777\">&nbsp;//Скиллы</font><br1>");
        replyMSG.append("<a action=\"bypass -h admin_reload teleport\" " + warning + ">Teleports</a> <font color=\"777777\">&nbsp;//Телепорты</font><br1>");
        replyMSG.append("<a action=\"bypass -h admin_reload tradelist\" " + warning + ">Trade Lists</a> <font color=\"777777\">&nbsp;//Обычные магазины</font><br1>");
        replyMSG.append("</body></html>");
        menu.setHtml(replyMSG.toString());
        player.sendPacket(menu);
        replyMSG.clear();
        replyMSG = null;
        menu = null;
    }

    private void showConfigWindow(L2PcInstance player) {
        String warning = "msg=\"Данным действием, вы подтверждаете, что можете вызвать утечку памяти и падение сервера.\"";
        NpcHtmlMessage menu = NpcHtmlMessage.id(5);
        TextBuilder replyMSG = new TextBuilder("<html><body><font color=LEVEL>Перезагрузка конфигов (<a action=\"bypass -h admin_config_reload 0\" " + warning + "><font color=\"FF9933\">Перезагрузить все</font></a>)</font><br>");
        replyMSG.append("<font color=\"FF9933\">На живом сервере перезагружайте конфиг, который изменяли.</font><br>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 1\" " + warning + ">altsettings.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 2\" " + warning + ">commands.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 3\" " + warning + ">enchants.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 4\" " + warning + ">events.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 5\" " + warning + ">fakeplayers.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 6\" " + warning + ">geodata.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 7\" " + warning + ">custom.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 8\" " + warning + ">npc.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 9\" " + warning + ">options.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 10\" " + warning + ">other.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 11\" " + warning + ">pvp.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 12\" " + warning + ">rates.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 13\" " + warning + ">server.cfg</a><br1>");
        replyMSG.append("<a action=\"bypass -h admin_config_reload 14\" " + warning + ">services.cfg</a><br1>");
        replyMSG.append("</body></html>");
        menu.setHtml(replyMSG.toString());
        player.sendPacket(menu);
        replyMSG.clear();
        replyMSG = null;
        menu = null;
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}