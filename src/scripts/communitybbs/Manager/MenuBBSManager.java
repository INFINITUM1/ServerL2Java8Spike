package scripts.communitybbs.Manager;

import java.util.regex.Pattern;
import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.AutoRestart;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.UserKey;
import net.sf.l2j.gameserver.util.Util;

public class MenuBBSManager extends BaseBBSManager {

    private static MenuBBSManager _instance;

    public static void init() {
        _instance = new MenuBBSManager();
    }

    public static MenuBBSManager getInstance() {
        return _instance;
    }

    @Override
    public void parsecmd(String command, L2PcInstance player) {
        if (command.equalsIgnoreCase("_bbsmenu")) {
            showWelcome(player);
        } else if (command.startsWith("_bbsmenu_")) {
            String choise = command.substring(9).trim();
            if (choise.startsWith("exp")) {
                int flag = Integer.parseInt(choise.substring(3).trim());
                player.setNoExp(flag == 1 ? true : false);
                showWelcome(player);
            } else if (choise.startsWith("alone")) {
                int flag = Integer.parseInt(choise.substring(5).trim());

                player.setAlone(flag == 1 ? true : false);
                showWelcome(player);
            } else if (choise.startsWith("autoloot")) {
                int flag = Integer.parseInt(choise.substring(8).trim());
                player.setAutoLoot(flag == 1 ? true : false);

                showWelcome(player);
                player.sendAdmResultMessage("##1#");
            } else if (choise.startsWith("pathfind")) {
                int flag = Integer.parseInt(choise.substring(8).trim());

                player.setGeoPathfind(flag == 1 ? true : false);
                showWelcome(player);
            } else if (choise.startsWith("skillchance")) {
                int flag = Integer.parseInt(choise.substring(11).trim());

                player.setShowSkillChances(flag == 1 ? true : false);
                showWelcome(player);
            } else if (choise.startsWith("showshots")) {
                int flag = Integer.parseInt(choise.substring(9).trim());

                player.setSoulShotsAnim(flag == 1 ? true : false);
                showWelcome(player);
            } else if (choise.startsWith("blockchat")) {
                int flag = 0;
                try {
                    flag = Integer.parseInt(choise.substring(9).trim());
                } catch (Exception ignored) {
                    //
                }
                player.setChatIgnore(flag);

                showWelcome(player);
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
            } else if (choise.equalsIgnoreCase("offvote")) {
                player.delVoteRef();
                showWelcome(player);
            } else if (choise.startsWith("key")) {
                if (!player.getUserKey().key.equalsIgnoreCase("")) {
                    TextBuilder tb = new TextBuilder("");
                    tb.append(getPwHtm("menu"));
                    tb.append("&nbsp;&nbsp;Ключ установлен.<br></body></html>");
                    separateAndSend(tb.toString(), player);
                }

                try {
                    String key = choise.substring(3).trim();
                    if (isValidKey(key)) {
                        player.setUserKey(key);
                    } else {
                        TextBuilder tb = new TextBuilder("");
                        tb.append(getPwHtm("menu"));
                        tb.append("&nbsp;&nbsp;" + Static.SET_KEY_ERROR4_16 + ".<br></body></html>");
                        separateAndSend(tb.toString(), player);
                    }
                } catch (Exception ignored) {
                    //
                }
            } else if (choise.startsWith("chkkey")) {
                try {
                    String key = choise.substring(6).trim();
                    if (!player.getUserKey().key.equalsIgnoreCase(key)) {
                        if (player.getUserKey().checkLeft()) {
                            player.kick();
                        }
                        TextBuilder tb = new TextBuilder("");
                        tb.append(getPwHtm("menu"));
                        tb.append("&nbsp;&nbsp;Неверный пароль..<br></body></html>");
                        separateAndSend(tb.toString(), player);
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
            } else if (choise.equalsIgnoreCase("offkey")) {
                player.delUserKey();
                showWelcome(player);
            }
        }
        player.sendAdmResultMessage("#######1#");
        showWelcome(player);
    }

    private void showWelcome(L2PcInstance player) {
        TextBuilder build = new TextBuilder("");
        build.append(getPwHtm("menu"));
        if (Config.VS_ONLINE) {
            build.append("&nbsp;&nbsp;Игроков онлайн: <font color=33CC00>" + L2World.getInstance().getAllPlayersCount() + "</font><br1><img src=\"L2UI.SquareWhite\" width=150 height=1><br>");
        }
        if (Config.VS_AUTORESTAT && Config.RESTART_HOUR > 0) {
            build.append("&nbsp;&nbsp;Время до рестарта: <font color=33CC00>" + (AutoRestart.getInstance().remain() / 60000) + "</font> минут.<br1><img src=\"L2UI.SquareWhite\" width=150 height=1><br>");
        }
        build.append("<font color=\"LEVEL\">&nbsp;&nbsp;Параметры игрока </font>");
        build.append("<table width=290><br>");
        int all = 0;
        if (Config.VS_NOEXP) {
            build.append("<tr><td width=180><font color=66CC00>Отказ от опыта:</font></td>");
            if (player.isNoExp()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_exp 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_exp 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_NOREQ) {
            build.append("<tr><td width=180><font color=66CC00>Отказ от трейда/пати и т.д.:</font></td>");
            if (player.isAlone()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_alone 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_alone 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_AUTOLOOT) {
            build.append("<tr><td width=180><font color=66CC00>Автолут:</font></td>");
            if (player.getAutoLoot()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_autoloot 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_autoloot 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_PATHFIND && Config.GEODATA == 2) {
            build.append("<tr><td width=180><font color=66CC00>Огибание препятствий:</font></td>");
            if (player.geoPathfind()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_pathfind 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_pathfind 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_SKILL_CHANCES) {
            build.append("<tr><td width=180><font color=66CC00>Шансы прохождения скиллов:</font></td>");
            if (player.getShowSkillChances()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_skillchance 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_skillchance 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
            all += 1;
        }

        if (Config.VS_ANIM_SHOTS) {
            build.append("<tr><td width=180><font color=66CC00>Анимация сулшотов:</font></td>");
            if (player.showSoulShotsAnim()) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_showshots 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_showshots 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
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
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_offvote\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td></td><td></td></tr><tr><td width=180>Введите ник:</td><td> </td><td></td></tr>");
                build.append("<tr><td><edit var=\"name\" width=120 length=\"16\"></td>");
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_vote $name\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
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
                    build.append("<td><button value=\"Ок\" action=\"bypass -h _bbsmenu_chkkey $key\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
                } else {
                    build.append("<td></td><td></td></tr><tr><td width=180>Ключ от чара</td><td> </td><td></td></tr>");
                    build.append("<tr><td><font color=3399CC>*******</font></td>");
                    build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_offkey\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
                }
            } else {
                build.append("<td></td><td></td></tr><tr><td width=180>Ключ:</td><td> </td><td></td></tr>");
                build.append("<tr><td><edit var=\"key\" width=120 length=\"16\"></td>");
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_key $key\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
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
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h _bbsmenu_blockchat 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
            } else {
                build.append("<td><edit var=\"lvl\" width=20 length=\"2\"></td><td width=20>ур.</td>");
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h _bbsmenu_blockchat $lvl\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
            }
            build.append("</tr></table><br>");
        }

        if (Config.VS_VREF) {
            build.append("<font color=FFCC33>&nbsp;&nbsp;* при голосовании в Л2ТОП награда будет идти на указанныый ник.</font><br><br>");
        }
        if (Config.VS_CKEY) {
            build.append("<font color=FFCC33>&nbsp;&nbsp;* с ключем нельзя будет торговать, ходить, использовать инвентарь.</font>");
        }
        build.append("</body></html>");
        separateAndSend(build.toString(), player);
        build.clear();
        build = null;
    }
    private static final Pattern keyPattern = Pattern.compile("[\\w\\u005F\\u002E]+", Pattern.UNICODE_CASE);

    private boolean isValidKey(String key) {
        if (!Util.isAlphaNumeric(key)) {
            return false;
        }

        if (key.length() < 3 || key.length() > 16) {
            return false;
        }

        return keyPattern.matcher(key).matches();
    }

    @Override
    public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance player) {
        // TODO Auto-generated method stub
    }
}
