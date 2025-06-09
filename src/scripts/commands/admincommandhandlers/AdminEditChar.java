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

import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.Util;
import scripts.commands.IAdminCommandHandler;

/**
 * This class handles following admin commands:
 * - edit_character
 * - current_player
 * - character_list
 * - show_characters
 * - find_character
 * - find_ip
 * - find_account
 * - rec
 * - nokarma
 * - setkarma
 * - settitle
 * - setname
 * - setsex
 * - setclass
 * - fullfood
 * - save_modifications
 *
 * @version $Revision: 1.3.2.1.2.10 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminEditChar implements IAdminCommandHandler {

    private static final Logger _log = Logger.getLogger(AdminEditChar.class.getName());
    private static final String[] ADMIN_COMMANDS = {
        "admin_edit_character",
        "admin_current_player",
        "admin_nokarma", // this is to remove karma from selected char...
        "admin_setkarma", // sets karma of target char to any amount. //setkarma <karma>
        "admin_character_list", //same as character_info, kept for compatibility purposes
        "admin_character_info", //given a player name, displays an information window
        "admin_character_obj", //given a player name, displays an information window
        "admin_show_characters",//list of characters
        "admin_find_character", //find a player by his name or a part of it (case-insensitive)
        "admin_find_ip", // find all the player connections from a given IPv4 number
        "admin_find_account", //list all the characters from an account (useful for GMs w/o DB access)
        "admin_save_modifications", //consider it deprecated...
        "admin_rec", // gives recommendation points
        "admin_settitle", // changes char title
        "admin_setname", // changes char name
        "admin_setsex", // changes characters' sex
        "admin_sethero", // gives Hero Status
        "admin_setclass", // changes chars' classId
        "admin_fullfood", // fulfills a pet's food bar
        "admin_namecolor",
        "admin_titlecolor",
        "admin_negate",
        "admin_cleanse",
        "admin_spy",
        "admin_clearoly"
    };
    private static final int REQUIRED_LEVEL = Config.GM_CHAR_EDIT;
    private static final int REQUIRED_LEVEL2 = Config.GM_CHAR_EDIT_OTHER;
    private static final int REQUIRED_LEVEL_VIEW = Config.GM_CHAR_VIEW;

    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!((checkLevel(activeChar.getAccessLevel()) || checkLevel2(activeChar.getAccessLevel())) && activeChar.isGM())) {
                return false;
            }
        }

        GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target", "");

        if (command.equals("admin_current_player")) {
            showCharacterInfo(activeChar, null);
        } else if ((command.startsWith("admin_character_list")) || (command.startsWith("admin_character_info"))) {
            try {
                String val = command.substring(21);
                val = val.replaceAll("&lt;", "<");
                val = val.replaceAll("&gt;", ">");
                L2PcInstance target = L2World.getInstance().getPlayer(val);
                if (target != null) {
                    showCharacterInfo(activeChar, target);
                } else {
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
                }
            } catch (StringIndexOutOfBoundsException e) {
                activeChar.sendAdmResultMessage("Usage: //character_info <player_name>");
            }
        } else if (command.startsWith("admin_character_obj")) {
            try {
                Integer obj = Integer.parseInt(command.substring(20));
                L2PcInstance target = L2World.getInstance().getPlayer(obj);
                if (target != null) {
                    showCharacterInfo(activeChar, target);
                } else {
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
                }
            } catch (StringIndexOutOfBoundsException e) {
                activeChar.sendAdmResultMessage("Usage: //character_info <player_name>");
            }
        } else if (command.startsWith("admin_show_characters")) {
            try {
                String val = command.substring(22);
                int page = Integer.parseInt(val);
                listCharacters(activeChar, page);
            } catch (StringIndexOutOfBoundsException e) {
                //Case of empty page number
                activeChar.sendAdmResultMessage("Usage: //show_characters <page_number>");
            }
        } else if (command.startsWith("admin_find_character")) {
            try {
                String val = command.substring(21);
                findCharacter(activeChar, val);
            } catch (StringIndexOutOfBoundsException e) {	//Case of empty character name
                activeChar.sendAdmResultMessage("Usage: //find_character <character_name>");
                listCharacters(activeChar, 0);
            }
        } else if (command.startsWith("admin_find_ip")) {
            try {
                String val = command.substring(14);
                findCharactersPerIp(activeChar, val);
            } catch (Exception e) {	//Case of empty or malformed IP number
                activeChar.sendAdmResultMessage("Usage: //find_ip <www.xxx.yyy.zzz>");
                listCharacters(activeChar, 0);
            }
        } // [L2J_JP ADD START]
        else if (command.startsWith("admin_sethero")) {
            L2Object target = activeChar.getTarget();
            L2PcInstance player = null;
            if (activeChar != target && activeChar.getAccessLevel() < REQUIRED_LEVEL2) {
                return false;
            }
            if (target.isPlayer()) {
                player = (L2PcInstance) target;
            } else {
                return false;
            }
            player.setHero(player.isHero() ? false : true);
            if (player.isHero()) {
                player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
            }
            player.sendAdmResultMessage("Admin changed your hero status");
            player.broadcastUserInfo();
        } else if (command.startsWith("admin_find_account")) {
            try {
                String val = command.substring(19);
                findCharactersPerAccount(activeChar, val);
            } catch (Exception e) {	//Case of empty or malformed player name
                activeChar.sendAdmResultMessage("Usage: //find_account <player_name>");
                listCharacters(activeChar, 0);
            }
        } else if (command.equals("admin_edit_character")) {
            editCharacter(activeChar);
        } // Karma control commands
        else if (command.equals("admin_nokarma")) {
            setTargetKarma(activeChar, 0);
        } else if (command.startsWith("admin_setkarma")) {
            try {
                String val = command.substring(15);
                int karma = Integer.parseInt(val);
                if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel() >= REQUIRED_LEVEL2) {
                    GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");
                }
                setTargetKarma(activeChar, karma);
            } catch (StringIndexOutOfBoundsException e) {
                if (Config.DEVELOPER) {
                    System.out.println("Set karma error: " + e);
                }
                activeChar.sendAdmResultMessage("Usage: //setkarma <new_karma_value>");
            }
        } else if (command.startsWith("admin_save_modifications")) {
            try {
                String val = command.substring(24);
                if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel() >= REQUIRED_LEVEL2) {
                    GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");
                }
                adminModifyCharacter(activeChar, val);
            } catch (StringIndexOutOfBoundsException e) {	//Case of empty character name
                activeChar.sendAdmResultMessage("Error while modifying character.");
                listCharacters(activeChar, 0);
            }
        } else if (command.startsWith("admin_rec")) {
            try {
                String val = command.substring(10);
                int recVal = Integer.parseInt(val);
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;
                if (activeChar != target && activeChar.getAccessLevel() < REQUIRED_LEVEL2) {
                    return false;
                }
                if (target.isPlayer()) {
                    player = (L2PcInstance) target;
                } else {
                    return false;
                }
                player.setRecomHave(recVal);
                player.sendAdmResultMessage("You have been recommended by a GM");
                player.broadcastUserInfo();
            } catch (Exception e) {
                activeChar.sendAdmResultMessage("Usage: //rec number");
            }
        } else if (command.startsWith("admin_setclass")) {
            try {
                String val = command.substring(15);
                int classidval = Integer.parseInt(val);
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;
                if (activeChar != target && activeChar.getAccessLevel() < REQUIRED_LEVEL2) {
                    return false;
                }
                if (target.isPlayer()) {
                    player = (L2PcInstance) target;
                } else {
                    return false;
                }
                boolean valid = false;
                for (ClassId classid : ClassId.values()) {
                    if (classidval == classid.getId()) {
                        valid = true;
                    }
                }
                if (valid && (player.getClassId().getId() != classidval)) {
                    player.setClassId(classidval);
                    if (!player.isSubClassActive()) {
                        player.setBaseClass(classidval);
                    }
                    String newclass = player.getTemplate().className;
                    player.store();
                    player.sendAdmResultMessage("A GM changed your class to " + newclass);
                    player.broadcastUserInfo();
                    activeChar.sendMessage(player.getName() + " is a " + newclass);
                }
                activeChar.sendAdmResultMessage("Usage: //setclass <valid_new_classid>");
            } catch (StringIndexOutOfBoundsException e) {
                AdminHelpPage.showHelpPage(activeChar, "charclasses.htm");
            }
        } else if (command.startsWith("admin_settitle")) {
            try {
                String val = command.substring(15);
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;
                if (activeChar != target && activeChar.getAccessLevel() < REQUIRED_LEVEL2) {
                    return false;
                }
                if (target.isPlayer()) {
                    player = (L2PcInstance) target;
                } else {
                    return false;
                }
                player.setTitle(val);
                player.sendAdmResultMessage("Your title has been changed by a GM");
                player.broadcastTitleInfo();
            } catch (StringIndexOutOfBoundsException e) {   //Case of empty character title
                activeChar.sendAdmResultMessage("You need to specify the new title.");
            }
        } else if (command.startsWith("admin_setname")) {
            try {
                String val = command.substring(14);
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;
                if (activeChar != target && activeChar.getAccessLevel() < REQUIRED_LEVEL2) {
                    return false;
                }
                if (target.isPlayer()) {
                    player = (L2PcInstance) target;
                } else {
                    return false;
                }
                player.changeName(val);
                activeChar.sendAdmResultMessage("Сменили игроку ник на " + val);
            } catch (StringIndexOutOfBoundsException e) {   //Case of empty character name
                activeChar.sendAdmResultMessage("Usage: //setname new_name_for_target");
            }
        } else if (command.startsWith("admin_setsex")) {
            L2Object target = activeChar.getTarget();
            L2PcInstance player = null;
            if (activeChar != target && activeChar.getAccessLevel() < REQUIRED_LEVEL2) {
                return false;
            }
            if (target.isPlayer()) {
                player = (L2PcInstance) target;
            } else {
                return false;
            }
            player.setSex((player.getAppearance().getSex() ? false : true));
            player.sendAdmResultMessage("Your gender has been changed by a GM");
            player.store();
            player.broadcastUserInfo();
        } else if (command.startsWith("admin_namecolor")) //3399cc / 9966ff / cc99ff / 333366 - oo / 3ca2dc / 338edc / 1b7ccf
        {
            try {
                String val = command.substring(16);
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;

                if (target.isPlayer()) {
                    player = (L2PcInstance) target;
                } else {
                    return false;
                }

                player.getAppearance().setNameColor(Integer.decode("0x" + convertColor(val)));
                player.sendAdmResultMessage("Цвет ника изменен");
                player.broadcastUserInfo();
            } catch (StringIndexOutOfBoundsException e) {   //Case of empty color
                activeChar.sendAdmResultMessage("You need to specify the new color.");
            }
        } else if (command.startsWith("admin_titlecolor")) {
            try {
                String val = command.substring(17);
                L2Object target = activeChar.getTarget();
                L2PcInstance player = null;
                if (target.isPlayer()) {
                    player = (L2PcInstance) target;
                } else {
                    return false;
                }

                player.getAppearance().setTitleColor(Integer.decode("0x" + convertColor(val)));
                player.sendAdmResultMessage("Цвет титула изменен");
                player.broadcastUserInfo();
            } catch (StringIndexOutOfBoundsException e) {   //Case of empty color
                activeChar.sendAdmResultMessage("You need to specify the new color.");
            }
        } else if (command.startsWith("admin_fullfood")) {
            L2Object target = activeChar.getTarget();
            if (target.isPet()) {
                L2PetInstance targetPet = (L2PetInstance) target;
                targetPet.setCurrentFed(targetPet.getMaxFed());
            } else {
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
            }
        } else if (command.startsWith("admin_negate")) {
            L2Object target = activeChar.getTarget();
            L2Character player = null;
            if (target.isL2Character()) {
                player = (L2Character) target;
                player.stopAllEffects();
                player.broadcastPacket(new MagicSkillUser(player, player, 2243, 1, 1, 0));
                activeChar.sendAdmResultMessage("Снятие баффов игроку " + player.getName());
            } else {
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
            }
        } else if (command.startsWith("admin_cleanse")) {
            L2Object target = activeChar.getTarget();
            L2Character player = null;
            if (target.isL2Character()) {
                player = (L2Character) target;
                player.stopAllDebuffs();
                player.broadcastPacket(new MagicSkillUser(player, player, 2242, 1, 1, 0));
                activeChar.sendAdmResultMessage("Снятие дебаффов игроку " + player.getName());
            } else {
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
            }
        } else if (command.startsWith("admin_spy")) {
            L2Object target = activeChar.getTarget();
            L2PcInstance player = null;
            if (target.isPlayer()) {
                player = (L2PcInstance) target;
                player.setSpy(true);
            }
        } else if (command.equalsIgnoreCase("admin_clearoly")) {
            L2Object target = activeChar.getTarget();
            if (target != null) {
                target.olympiadClear();
            }
        }

        return true;
    }

    private String convertColor(String color) {
        //TextBuilder tb = new TextBuilder(color);
        return new TextBuilder(color).reverse().toString();
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }

    private boolean checkLevel2(int level) {
        return (level >= REQUIRED_LEVEL_VIEW);
    }

    private void listCharacters(L2PcInstance activeChar, int page) {
        Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
        L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

        int MaxCharactersPerPage = 20;
        int MaxPages = players.length / MaxCharactersPerPage;

        if (players.length > MaxCharactersPerPage * MaxPages) {
            MaxPages++;
        }

        //Check if number of users changed
        if (page > MaxPages) {
            page = MaxPages;
        }

        int CharactersStart = MaxCharactersPerPage * page;
        int CharactersEnd = players.length;
        if (CharactersEnd - CharactersStart > MaxCharactersPerPage) {
            CharactersEnd = CharactersStart + MaxCharactersPerPage;
        }

        NpcHtmlMessage htm = NpcHtmlMessage.id(5);
        htm.setFile("data/html/admin/charlist.htm");
        TextBuilder tb = new TextBuilder();
        for (int x = 0; x < MaxPages; x++) {
            int pagenr = x + 1;
            tb.append("<center><a action=\"bypass -h admin_show_characters " + x + "\">Page " + pagenr + "</a></center>");
        }
        htm.replace("%pages%", tb.toString());
        tb.clear();

        tb.append("<tr><td width=80><a action=\"bypass -h admin_character_obj " + activeChar.getObjectId() + "\">" + activeChar.getName() + "</a></td><td width=110>" + activeChar.getTemplate().className + "</td><td width=40>" + activeChar.getLevel() + "</td></tr>");
        tb.append("<tr><td width=80>#</td><td width=110>#</td><td width=40>#</td></tr>");
        String name = "";
        for (int i = CharactersStart; i < CharactersEnd; i++) {
            if (players[i] == null) {
                continue;
            }

            //Add player info into new Table row
            name = Util.htmlSpecialChars(players[i].getName());
            tb.append("<tr><td width=80><a action=\"bypass -h admin_character_obj " + players[i].getObjectId() + "\">" + name + "</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
        }
        htm.replace("%players%", tb.toString());
        activeChar.sendUserPacket(htm);

        tb.clear();
        tb = null;
        htm = null;
    }

    private void showCharacterInfo(L2PcInstance activeChar, L2PcInstance player) {
        if (player == null) {
            L2Object target = activeChar.getTarget();
            if (target.isPlayer()) {
                player = (L2PcInstance) target;
            } else {
                return;
            }
        } else {
            activeChar.setTarget(player);
        }
        gatherCharacterInfo(activeChar, player, "charinfo.htm");
    }

    /**
     * @param activeChar
     * @param player
     */
    private void gatherCharacterInfo(L2PcInstance activeChar, L2PcInstance player, String filename) {
        String ip = "N/A";
        String account = "N/A";
        try {
            StringTokenizer clientinfo = new StringTokenizer(player.getClient().toString(), " ]:-[");
            clientinfo.nextToken();
            clientinfo.nextToken();
            clientinfo.nextToken();
            account = clientinfo.nextToken();
            clientinfo.nextToken();
            ip = clientinfo.nextToken();
        } catch (Exception e) {
        }

        String name = Util.htmlSpecialChars(player.getName());
        NpcHtmlMessage htm = NpcHtmlMessage.id(5);
        htm.setFile("data/html/admin/" + filename);
        htm.replace("%name%", name);
        htm.replace("%level%", String.valueOf(player.getLevel()));
        htm.replace("%clan%", String.valueOf(ClanTable.getInstance().getClan(player.getClanId())));
        htm.replace("%xp%", String.valueOf(player.getExp()));
        htm.replace("%sp%", String.valueOf(player.getSp()));
        htm.replace("%class%", player.getTemplate().className);
        htm.replace("%ordinal%", String.valueOf(player.getClassId().ordinal()));
        htm.replace("%classid%", String.valueOf(player.getClassId()));
        htm.replace("%x%", String.valueOf(player.getX()));
        htm.replace("%y%", String.valueOf(player.getY()));
        htm.replace("%z%", String.valueOf(player.getZ()));
        htm.replace("%currenthp%", String.valueOf((int) player.getCurrentHp()));
        htm.replace("%maxhp%", String.valueOf(player.getMaxHp()));
        htm.replace("%karma%", String.valueOf(player.getKarma()));
        htm.replace("%currentmp%", String.valueOf((int) player.getCurrentMp()));
        htm.replace("%maxmp%", String.valueOf(player.getMaxMp()));
        htm.replace("%pvpflag%", String.valueOf(player.getPvpFlag()));
        htm.replace("%currentcp%", String.valueOf((int) player.getCurrentCp()));
        htm.replace("%maxcp%", String.valueOf(player.getMaxCp()));
        htm.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
        htm.replace("%pkkills%", String.valueOf(player.getPkKills()));
        htm.replace("%currentload%", String.valueOf(player.getCurrentLoad()));
        htm.replace("%maxload%", String.valueOf(player.getMaxLoad()));
        htm.replace("%percent%", String.valueOf(Util.roundTo(((float) player.getCurrentLoad() / (float) player.getMaxLoad()) * 100, 2)));
        htm.replace("%patk%", String.valueOf(player.getPAtk(null)));
        htm.replace("%matk%", String.valueOf(player.getMAtk(null, null)));
        htm.replace("%pdef%", String.valueOf(player.getPDef(null)));
        htm.replace("%mdef%", String.valueOf(player.getMDef(null, null)));
        htm.replace("%accuracy%", String.valueOf(player.getAccuracy()));
        htm.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
        htm.replace("%critical%", String.valueOf(player.getCriticalHit(null, null)));
        htm.replace("%runspeed%", String.valueOf(player.getRunSpeed()));
        htm.replace("%patkspd%", String.valueOf(player.getPAtkSpd()));
        htm.replace("%matkspd%", String.valueOf(player.getMAtkSpd()));
        htm.replace("%access%", String.valueOf(player.getAccessLevel()));
        htm.replace("%account%", account);
        htm.replace("%ip%", ip);
        activeChar.sendPacket(htm);
        htm = null;
    }

    private void setTargetKarma(L2PcInstance activeChar, int newKarma) {
        // function to change karma of selected char
        L2Object target = activeChar.getTarget();
        L2PcInstance player = null;
        if (target.isPlayer()) {
            player = (L2PcInstance) target;
        } else {
            return;
        }

        if (newKarma >= 0) {
            // for display
            int oldKarma = player.getKarma();
            // update karma
            player.setKarma(newKarma);
            //Common character information
            player.sendPacket(SystemMessage.id(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO).addString(String.valueOf(newKarma)));
            //Admin information
            activeChar.sendAdmResultMessage("Successfully Changed karma for " + player.getName() + " from (" + oldKarma + ") to (" + newKarma + ").");
            if (Config.DEBUG) {
                _log.fine("[SET KARMA] [GM]" + activeChar.getName() + " Changed karma for " + player.getName() + " from (" + oldKarma + ") to (" + newKarma + ").");
            }
        } else {
            // tell admin of mistake
            activeChar.sendAdmResultMessage("You must enter a value for karma greater than or equal to 0.");
            if (Config.DEBUG) {
                _log.fine("[SET KARMA] ERROR: [GM]" + activeChar.getName() + " entered an incorrect value for new karma: " + newKarma + " for " + player.getName() + ".");
            }
        }
    }

    private void adminModifyCharacter(L2PcInstance activeChar, String modifications) {
        L2Object target = activeChar.getTarget();

        if (!(target.isPlayer())) {
            return;
        }

        L2PcInstance player = (L2PcInstance) target;
        StringTokenizer st = new StringTokenizer(modifications);

        if (st.countTokens() != 6) {
            editCharacter(player);
            return;
        }

        String hp = st.nextToken();
        String mp = st.nextToken();
        String cp = st.nextToken();
        String pvpflag = st.nextToken();
        String pvpkills = st.nextToken();
        String pkkills = st.nextToken();

        int hpval = Integer.parseInt(hp);
        int mpval = Integer.parseInt(mp);
        int cpval = Integer.parseInt(cp);
        int pvpflagval = Integer.parseInt(pvpflag);
        int pvpkillsval = Integer.parseInt(pvpkills);
        int pkkillsval = Integer.parseInt(pkkills);

        //Common character information
        player.sendAdmResultMessage("Admin has changed your stats."
                + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval
                + "  PvP Flag: " + pvpflagval + " PvP/PK " + pvpkillsval + "/" + pkkillsval);
        player.setCurrentHp(hpval);
        player.setCurrentMp(mpval);
        player.setCurrentCp(cpval);
        player.setPvpFlag(pvpflagval);
        player.setPvpKills(pvpkillsval);
        player.setPkKills(pkkillsval);

        // Save the changed parameters to the database.
        player.store();

        StatusUpdate su = new StatusUpdate(player.getObjectId());
        su.addAttribute(StatusUpdate.CUR_HP, hpval);
        su.addAttribute(StatusUpdate.MAX_HP, player.getMaxHp());
        su.addAttribute(StatusUpdate.CUR_MP, mpval);
        su.addAttribute(StatusUpdate.MAX_MP, player.getMaxMp());
        su.addAttribute(StatusUpdate.CUR_CP, cpval);
        su.addAttribute(StatusUpdate.MAX_CP, player.getMaxCp());
        player.sendPacket(su);

        //Admin information
        player.sendAdmResultMessage("Changed stats of " + player.getName() + "."
                + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval
                + "  PvP: " + pvpflagval + " / " + pvpkillsval);

        if (Config.DEBUG) {
            _log.fine("[GM]" + activeChar.getName() + " changed stats of " + player.getName() + ". "
                    + " HP: " + hpval + " MP: " + mpval + " CP: " + cpval
                    + " PvP: " + pvpflagval + " / " + pvpkillsval);
        }

        showCharacterInfo(activeChar, null); //Back to start

        player.broadcastPacket(new CharInfo(player));
        player.sendPacket(new UserInfo(player));
        player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        player.decayMe();
        player.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
    }

    private void editCharacter(L2PcInstance activeChar) {
        L2Object target = activeChar.getTarget();
        if (target == null)
        {
            activeChar.sendAdmResultMessage("Персонаж не найден.");
            return;
        }
        if (!(target.isPlayer())) {
            return;
        }
        L2PcInstance player = (L2PcInstance) target;
        gatherCharacterInfo(activeChar, player, "charedit.htm");
    }

    /**
     * @param activeChar
     * @param CharacterToFind
     */
    private void findCharacter(L2PcInstance activeChar, String charToFind) {
        /*int CharactersFound = 0;
        String name;
        Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
        L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
        NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
        adminReply.setFile("data/html/admin/charfind.htm");
        TextBuilder replyMSG = new TextBuilder();
        replyMSG.append("<tr><td width=80># <a action=\"bypass -h admin_character_list "+activeChar.getName()+"\">"+activeChar.getName()+"</a></td><td width=110>" + activeChar.getTemplate().className + "</td><td width=40>"+activeChar.getLevel()+"</td></tr>");
        for (int i = 0; i < players.length; i++)
        {	//Add player info into new Table row
        name = players[i].getName();
        if (name.toLowerCase().contains(CharacterToFind.toLowerCase()))
        {
        CharactersFound = CharactersFound+1;
        replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list "+name+"\">"+name+"</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>"+players[i].getLevel()+"</td></tr>");
        }
        if (CharactersFound > 20)
        break;
        }
        adminReply.replace("%results%", replyMSG.toString());
        replyMSG.clear();
        if (CharactersFound==0)
        replyMSG.append("s. Please try again.");
        else if (CharactersFound > 20)
        {
        adminReply.replace("%number%", " more than 20");
        replyMSG.append("s.<br>Please refine your search to see all of the results.");
        }
        else if (CharactersFound==1)
        replyMSG.append(".");
        else
        replyMSG.append("s.");
        adminReply.replace("%number%", String.valueOf(CharactersFound));
        adminReply.replace("%end%", replyMSG.toString());
        activeChar.sendPacket(adminReply);*/
        //
        TextBuilder tb = new TextBuilder();
        NpcHtmlMessage htm = NpcHtmlMessage.id(5);
        htm.setFile("data/html/admin/charfind.htm");

        int count = 0;
        String name;
        String search = Util.htmlSpecialChars(charToFind.toLowerCase());
        for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
            if (player == null) {
                continue;
            }

            name = Util.htmlSpecialChars(player.getName());
            if (name.toLowerCase().contains(search)) {
                count += 1;
                tb.append("<tr><td width=80><a action=\"bypass -h admin_character_list " + name + "\">" + name + "</a></td><td width=110>" + player.getTemplate().className + "</td><td width=40>" + player.getLevel() + "</td></tr>");
            }
            if (count > 20) {
                break;
            }
        }
        htm.replace("%results%", tb.toString());
        tb.clear();

        if (count == 0) {
            tb.append("s. Please try again.");
        } else if (count > 20) {
            htm.replace("%number%", " more than 20");
            tb.append("s.<br>Please refine your search to see all of the results.");
        } else if (count == 1) {
            tb.append(".");
        } else {
            tb.append("s.");
        }

        htm.replace("%number%", String.valueOf(count));
        htm.replace("%end%", tb.toString());
        activeChar.sendUserPacket(htm);

        tb.clear();
        tb = null;
        htm = null;
    }

    /**
     * @param activeChar
     * @param IpAdress
     * @throws IllegalArgumentException
     */
    private void findCharactersPerIp(L2PcInstance activeChar, String IpAdress) throws IllegalArgumentException {
        /*if (!IpAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
        throw  new IllegalArgumentException("Malformed IPv4 number");
        Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
        L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
        int CharactersFound = 0;
        String name,ip="0.0.0.0";
        TextBuilder replyMSG = new TextBuilder();
        NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
        adminReply.setFile("data/html/admin/ipfind.htm");
        for (int i = 0; i < players.length; i++)
        {
        ip=players[i].getClient().getConnection().getSocket().getInetAddress();
        if (ip.equals(IpAdress))
        {
        name = players[i].getName();
        CharactersFound = CharactersFound+1;
        replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list "+name+"\">"+name+"</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>"+players[i].getLevel()+"</td></tr>");
        }
        if (CharactersFound > 20)
        break;
        }
        adminReply.replace("%results%", replyMSG.toString());
        replyMSG.clear();
        if (CharactersFound==0)
        replyMSG.append("s. Maybe they got d/c? :)");
        else if (CharactersFound > 20)
        {
        adminReply.replace("%number%", " more than "+String.valueOf(CharactersFound));
        replyMSG.append("s.<br>In order to avoid you a client crash I won't <br1>display results beyond the 20th character.");
        }
        else if (CharactersFound==1)
        replyMSG.append(".");
        else
        replyMSG.append("s.");
        adminReply.replace("%ip%", ip);
        adminReply.replace("%number%", String.valueOf(CharactersFound));
        adminReply.replace("%end%", replyMSG.toString());
        activeChar.sendPacket(adminReply);*/
    }

    /**
     * @param activeChar
     * @param characterName
     * @throws IllegalArgumentException
     */
    private void findCharactersPerAccount(L2PcInstance activeChar, String characterName) throws IllegalArgumentException {
        if (characterName.matches(Config.CNAME_TEMPLATE)) {
            String account = null;
            Map<Integer, String> chars;
            L2PcInstance player = L2World.getInstance().getPlayer(characterName);
            if (player == null) {
                throw new IllegalArgumentException("Player doesn't exist");
            }
            chars = player.getAccountChars();
            account = player.getAccountName();
            TextBuilder replyMSG = new TextBuilder();
            NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
            adminReply.setFile("data/html/admin/accountinfo.htm");
            for (String charname : chars.values()) {
                replyMSG.append(charname + "<br1>");
            }
            adminReply.replace("%characters%", replyMSG.toString());
            adminReply.replace("%account%", account);
            adminReply.replace("%player%", characterName);
            activeChar.sendPacket(adminReply);
        } else {
            throw new IllegalArgumentException("Malformed character name");
        }
    }
}
