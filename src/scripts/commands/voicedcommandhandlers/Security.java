package scripts.commands.voicedcommandhandlers;

import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.Util;
import scripts.commands.IVoicedCommandHandler;

public class Security implements IVoicedCommandHandler {

    private static final String[] VOICED_COMMANDS = {"security", "security_"};

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#useVoicedCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
     */
    @Override
    public boolean useVoicedCommand(String command, L2PcInstance player, String target) {
        if (command.equalsIgnoreCase("security")) {
            showWelcome(player);
        } else if (command.startsWith("security_")) {
            String choise = command.substring(9).trim();
            if (choise.startsWith("hwid")) {
                int flag = Integer.parseInt(choise.substring(4).trim());
                player.saveHWID((flag == 1));
                showWelcome(player);
                return true;
            } else if (choise.startsWith("pwd")) {
                String pass = "";
                String passr = "";
                String error = "Произошла ошибка при установке пароля.";
                boolean ok = false;
                try {
                    String[] pwd = choise.substring(4).split(" ");
                    pass = pwd[0];
                    passr = pwd[1];
                    if ((pass.length() < 5 || pass.length() > 15) || (passr.length() < 5 || passr.length() > 15)) {
                        error = "Пароль должен быть более 5 и менее 15 символов.";
                    } else if (!Util.isAlphaNumeric(pass) || !Util.isAlphaNumeric(passr)) {
                        error = "Пароль содержит запрещенные символы.";
                    } else if (!pass.equalsIgnoreCase(passr)) {
                        error = "Пароли не совпадают.";
                    } else {
                        ok = true;
                    }
                } catch (Exception e) {
                    ok = false;
                }
                if (ok && player.updatePassword(pass)) {
                    player.sendHtmlMessage("Новый пароль установлен.");
                } else {
                    player.sendHtmlMessage(error);
                }

                return true;
            } else if (choise.startsWith("email")) {
                String pass = "";
                String passr = "";
                String error = "Произошла ошибка при установке имейла.";
                boolean ok = false;
                try {
                    String[] pwd = choise.substring(6).split(" ");
                    pass = pwd[0];
                    passr = pwd[1];
                    if ((pass.length() < 5 || pass.length() > 35) || (passr.length() < 5 || passr.length() > 35)) {
                        error = "Имеил должен быть более 5 и менее 35 символов.";
                    } else if (!Util.isValidEmail(pass) || !Util.isValidEmail(passr)) {
                        error = "Имеил содержит запрещенные символы.";
                    } else if (!pass.equalsIgnoreCase(passr)) {
                        error = "Имейлы не совпадают.";
                    } else {
                        ok = true;
                    }
                } catch (Exception e) {
                    ok = false;
                }
                if (ok && player.updateEmail(pass)) {
                    player.getClient().setHasEmail(true);
                    player.sendHtmlMessage("Новый имеил успешно установлен.<center><br>==========<br> <font color=\"33CC00\">" + pass + "</font><br>==========</center>");
                } else {
                    //player.sendHtmlMessage(error);
                    player.sendAdmResultMessage(error);
                    player.sendUserPacket(Static.CHANGE_EMAIL);
                }
                return true;
            }
            return false;
        }
        return true;
    }

    private void showWelcome(L2PcInstance player) {
        if (player.isParalyzed() || player.getUserKey().on == 1) {
            player.sendActionFailed();
            return;
        }

        NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
        TextBuilder build = new TextBuilder("<html><body>");
        build.append("<font color=\"LEVEL\">Параметры безопасности.</font><br>");
        build.append("<table width=290>");
        if (Config.VS_HWID) {
            build.append("<tr><td width=180><font color=66CC00>Привязка чара к компьютеру:</font></td>");
            if (player.getMyHWID().length() > 5) {
                build.append("<td>[Вкл.]</td>");
                build.append("<td><button value=\"Выкл.\" action=\"bypass -h security_hwid 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            } else {
                build.append("<td><button value=\"Вкл.\" action=\"bypass -h security_hwid 1\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                build.append("<td>[Выкл.]</td></tr>");
            }
        }
        if (Config.VS_PWD) {
            build.append("<tr><td width=180><font color=66CC00>Смена пароля:</font></td>");
            build.append("<td></td><td></td></tr><tr><td width=180>Введите новый пароль:</td><td> </td><td></td></tr>");
            build.append("<tr><td><edit var=\"pwd\" width=120 length=\"16\"></td>");
            build.append("<td></td><td></td></tr><tr><td width=180>Повторите новый пароль:</td><td> </td><td></td></tr>");
            build.append("<tr><td><edit var=\"pwdr\" width=120 length=\"16\"></td>");
            build.append("<td><button value=\"Вкл.\" action=\"bypass -h security_pwd $pwd $pwdr\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td> </td></tr>");
        }
        build.append("</table></body></html>");
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