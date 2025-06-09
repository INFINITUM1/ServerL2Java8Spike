package scripts.commands.voicedcommandhandlers;

import java.util.regex.Pattern;
import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.AutoRestart;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.UserKey;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.Util;
import scripts.commands.IVoicedCommandHandler;

public class Menu implements IVoicedCommandHandler {

    private static final String[] VOICED_COMMANDS = {"menu", "menu_"};
    private static final Pattern keyPattern = Pattern.compile("[\\w\\u005F\\u002E]+", Pattern.UNICODE_CASE);

    @Override
    public boolean useVoicedCommand(String command, L2PcInstance player, String target) {
        if (player.isAlikeDead()) {
            showWelcome(player);
            return true;
        }
        if (command.equalsIgnoreCase("menu")) {
            showWelcome(player);
        } else if (command.startsWith("menu_")) {
            String choise = command.substring(5).trim();
            if (choise.startsWith("exp")) {
                int flag = Integer.parseInt(choise.substring(3).trim());
                /*if (flag == 0)
                player.setNoExp(false);
                else
                player.setNoExp(true);*/

                player.setNoExp(flag == 1 ? true : false);
                showWelcome(player);
                return true;
            } else if (choise.startsWith("alone")) {
                int flag = 0;
                try {
                    flag = Integer.parseInt(choise.substring(5).trim());
                } catch (Exception e) {
                    flag = 0;
                }

                player.setAlone(flag == 1 ? true : false);
                showWelcome(player);
                return true;
            } else if (choise.startsWith("autoloot")) {
                int flag = Integer.parseInt(choise.substring(8).trim());

                player.setAutoLoot(flag == 1 ? true : false);
                showWelcome(player);
                return true;
            } else if (choise.startsWith("pathfind")) {
                int flag = Integer.parseInt(choise.substring(8).trim());

                player.setGeoPathfind(flag == 1 ? true : false);
                showWelcome(player);
                return true;
            } else if (choise.startsWith("skillchance")) {
                int flag = Integer.parseInt(choise.substring(11).trim());

                player.setShowSkillChances(flag == 1 ? true : false);
                showWelcome(player);
                return true;
            } else if (choise.startsWith("showshots")) {
                int flag = Integer.parseInt(choise.substring(9).trim());

                player.setSoulShotsAnim(flag == 1 ? true : false);
                showWelcome(player);
                return true;
            } else if (choise.startsWith("blockchat")) {
                int flag = 0;
                try {
                    flag = Integer.parseInt(choise.substring(9).trim());
                } catch (Exception ignored) {
                    //
                }
                player.setChatIgnore(flag);

                showWelcome(player);
                return true;
            } else if (choise.startsWith("vote")) {
                try {
                    String nick = choise.substring(4);
                    if ((nick.length() >= 3 && nick.length() <= 16) && !nick.equals(player.getName())) {
                        player.setVoteRf(nick);
                    }

                    showWelcome(player);
                } catch (Exception ignored) {
                    //
                }

                showWelcome(player);
                return true;
            } else if (choise.equalsIgnoreCase("offvote")) {
                player.delVoteRef();
                showWelcome(player);
                return true;
            } else if (choise.startsWith("key")) {
                if (!player.getUserKey().key.equalsIgnoreCase("")) {
                    return true;
                }

                try {
                    String key = choise.substring(3).trim();
                    if (isValidKey(key)) {
                        player.setUserKey(key);
                    } else {
                        //player.sendHtmlMessage(Static.SET_KEY_ERROR4_16);
                        player.sendPacket(Static.SET_KEY_ERROR4_16);
                        return true;
                    }
                    //showWelcome(player);
                } catch (Exception ignored) {
                    //
                }
                //showWelcome(player);
                return true;
            } else if (choise.startsWith("chkkey")) {
                try {
                    String key = choise.substring(6).trim();
                    if (!player.getUserKey().key.equalsIgnoreCase(key)) {
                        if (player.getUserKey().checkLeft()) {
                            player.kick();
                            return false;
                        }
                        //player.sendHtmlMessage("Неверный пароль.");
                        //player.sendPacket(Static.MENU_WRONG_KEY);
                        NpcHtmlMessage SET_KEY_ERROR4_16 = new NpcHtmlMessage(0);
                        SET_KEY_ERROR4_16.setFile("data/html/menu/unblock_form_error.htm");
                        SET_KEY_ERROR4_16.replace("%CHK_TRYES%", String.valueOf(player.getUserKey().error));
                        player.sendPacket(SET_KEY_ERROR4_16);
                        SET_KEY_ERROR4_16 = null;
                        return true;
                    }
                } catch (Exception ignored) {
                    //
                }
                player.unsetUserKey();
                if (Config.VS_EMAIL && !player.hasEmail()) {
                    player.sendUserPacket(Static.CHANGE_EMAIL);
                } else if (Config.SERVER_NEWS) {
                    player.sendUserPacket(Static.SERVER_WELCOME);
                } else {
                    showWelcome(player);
                }
                return true;
            } else if (choise.equalsIgnoreCase("offkey")) {
                player.delUserKey();
                showWelcome(player);
                return true;
            }
            /*else if (choise.startsWith("traders"))
            {
            int flag = Integer.parseInt(choise.substring(7).trim());
            
            player.setTradersIgnore(flag == 1 ? true : false);
            player.setChannel(1, true);
            showWelcome(player);
            return true;
            }*/
            return false;
        }
        return true;
    }

    private boolean isValidKey(String key) {
        if (!Util.isAlphaNumeric(key)) {
            return false;
        }

        if (key.length() < 3 || key.length() > 16) {
            return false;
        }

        return keyPattern.matcher(key).matches();
    }

    private void showWelcome(L2PcInstance player) {
        NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
        TextBuilder build = new TextBuilder("<html><body>");
        if (Config.VS_ONLINE) {
            build.append("Игроков онлайн: <font color=33CC00>" + L2World.getInstance().getAllPlayersCount() + "</font><br1><img src=\"L2UI.SquareWhite\" width=150 height=1><br>");
        }
        if (Config.VS_AUTORESTAT && Config.RESTART_HOUR > 0) {
            build.append("Время до рестарта: <font color=33CC00>" + (AutoRestart.getInstance().remain() / 60000) + "</font> минут.<br1><img src=\"L2UI.SquareWhite\" width=150 height=1><br>");
        }
        build.append("<font color=\"LEVEL\">Параметры игрока </font>");
        build.append("<table width=290><br>");
        int all = 0;
        if (Config.VS_NOEXP) {
            build.append("<tr><td width=180><font color=66CC00>Отказ от опыта:</font></td>");
            if (player.isNoExp()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_exp 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_exp 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_NOREQ) {
            build.append("<tr><td width=180><font color=66CC00>Отказ от трейда/пати и т.д.:</font></td>");
            if (player.isAlone()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_alone 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_alone 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_AUTOLOOT) {
            build.append("<tr><td width=180><font color=66CC00>Автолут:</font></td>");
            if (player.getAutoLoot()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_autoloot 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_autoloot 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        /*if (Config.VS_TRADERSIGNORE)
        {
        build.append("<tr><td width=180><font color=66CC00>Видимость торговцев:</font></td>");
        if (player.getTradersIgnore())
        {
        build.append("<td>[Вкл.]</td>");
        build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_traders 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
        }
        else
        {
        build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_traders 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        build.append("<td>[Выкл.]</td></tr>");
        }
        all+=1;
        }*/

        if (Config.VS_PATHFIND && Config.GEODATA == 2) {
            build.append("<tr><td width=180><font color=66CC00>Огибание препятствий:</font></td>");
            if (player.geoPathfind()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_pathfind 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_pathfind 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_SKILL_CHANCES) {
            build.append("<tr><td width=180><font color=66CC00>Шансы прохождения скиллов:</font></td>");
            if (player.getShowSkillChances()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_skillchance 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_skillchance 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_ANIM_SHOTS) {
            build.append("<tr><td width=180><font color=66CC00>Анимация сулшотов:</font></td>");
            if (player.showSoulShotsAnim()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_showshots 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_showshots 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_VREF) {
            build.append("<tr><td width=180><br><font color=66CC00>Голосование на ник<font color=FFCC33>*</font>:</font></td>");
            if (!player.voteRef().equalsIgnoreCase("")) {
                build.append("<td></td><td></td></tr>");
                build.append("<tr><td width=180><font color=3399CC>" + player.voteRef() + "</font></td>");
                build.append("<td> </td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_offvote\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td></td><td></td></tr><tr><td width=180>Введите ник:</td><td> </td><td></td></tr>");
                build.append("<tr><td><edit var=\"name\" width=120 length=\"16\"></td>");
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_vote $name\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
            }
            all += 1;
        }

        if (Config.VS_CKEY) {
            UserKey uk = player.getUserKey();
            build.append("<tr><td width=180><br><font color=66CC00>Ключ от чара*<font color=FFCC33></font>:</font></td>");
            if (!uk.key.equalsIgnoreCase("")) {
                if (uk.on == 1) {
                    build.append("<td></td><td></td></tr><tr><td width=180>Какой ключ?</td><td> </td><td></td></tr>");
                    build.append("<tr><td><edit var=\"key\" width=120 length=\"16\"></td>");
                    build.append("<td><button value=\"Ок\" action=\"bypass -h menu_chkkey $key\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
                } else {
                    build.append("<td></td><td></td></tr><tr><td width=180>Ключ от чара</td><td> </td><td></td></tr>");
                    build.append("<tr><td><font color=3399CC>*******</font></td>");
                    build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_offkey\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
                }
            } else {
                build.append("<td></td><td></td></tr><tr><td width=180>Ключ:</td><td> </td><td></td></tr>");
                build.append("<tr><td><edit var=\"key\" width=120 length=\"16\"></td>");
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_key $key\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
            }
            all += 1;
        }
        if (all == 0) {
            build.append("<tr><td>Все сервисы отключены.</td></tr>");
        }
        build.append("</table><br>");

        if (Config.VS_CHATIGNORE) {
            int ignore = player.getChatIgnore();
            build.append("<font color=\"LEVEL\">Параметры чата </font><table width=290><tr><td width=180><font color=66CC00>Игнорировать игроков ниже:</font></td>");
            if (ignore > 0) {
                build.append("<td align=right width=20>" + ignore + "</td><td width=20>ур.</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h menu_blockchat 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
            } else {
                build.append("<td><edit var=\"lvl\" width=20 length=\"2\"></td><td width=20>ур.</td>");
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h menu_blockchat $lvl\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
            }
            build.append("</tr></table><br>");
        }

        if (Config.VS_VREF) {
            build.append("<font color=FFCC33>* при голосовании в Л2ТОП награда будет идти на указанныый ник.</font><br><br>");
        }
        if (Config.VS_CKEY) {
            build.append("<font color=FFCC33>* с ключем нельзя будет торговать, ходить, использовать инвентарь.</font>");
        }
        build.append("</body></html>");
        nhm.setHtml(build.toString());
        player.sendPacket(nhm);
        build.clear();
        build = null;
    }

    @Override
    public String[] getVoicedCommandList() {
        return VOICED_COMMANDS;
    }
}