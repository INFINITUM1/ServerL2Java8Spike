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
package scripts.communitybbs.Manager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExMailArrived;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.TimeLogger;
import net.sf.l2j.util.Log;

public class MailBBSManager extends BaseBBSManager {

    private static MailBBSManager _instance;

    public static void init() {
        _instance = new MailBBSManager();
    }

    public static MailBBSManager getInstance() {
        return _instance;
    }
    private static final int PAGE_LIMIT = 30;
    private static Date _date = new Date();
    private static SimpleDateFormat _sdf = new SimpleDateFormat("yyyy.MM.dd");
    private static SimpleDateFormat _sdfTime = new SimpleDateFormat("HH:mm");
    private static SimpleDateFormat _sdfDate = new SimpleDateFormat("d MMM.");

    @Override
    public void parsecmd(String command, L2PcInstance player) {
        if (command.equalsIgnoreCase("_bbsmail")) {
            showIndex(player, 1, 1); // входящие
        } else if (command.startsWith("_bbsmail_menu")) {
            String[] opaopa = command.substring(14).split("_");
            //System.out.println("##2->" + act + "<-##");
            switch (Integer.parseInt(opaopa[0])) {
                case 1:
                    int itemObj = Integer.parseInt(opaopa[1]);
                    if (itemObj > 0 && player.getBriefItem() == 0) {
                        player.setBriefItem(itemObj);
                    }

                    //System.out.println("##3->" + act + "<-##");
                    showNewBrief(player);
                    break;
                case 2:
                    showIndex(player, 1, Integer.parseInt(opaopa[2]));
                    break;
                case 3:
                    showIndex(player, 2, Integer.parseInt(opaopa[2]));
                    break;
                case 4:
                    //showAddressBook(player);
                    break;
                case 5:
                    addAugTo(player, Integer.parseInt(opaopa[1]));
                    break;
            }
        } else if (command.startsWith("_bbsmail_show")) {
            showBrief(player, Integer.parseInt(command.substring(13).trim()));
        } else if (command.startsWith("_bbsmail_send")) {
            Integer act = Integer.parseInt(command.substring(13, 15).trim());
            switch (act) {
                case 1:
                    try {
                        String[] data = command.substring(16).split(" _ "); //$target _ $tema _ $text
                        String target = data[0];
                        String tema = data[1];
                        String text = data[2];
                        sendBrief(player, target, tema, text);
                    } catch (Exception e) {
                        TextBuilder tb = new TextBuilder("<html><body><br>Произошла ошибка.<br></body></html>");
                        separateAndSend(tb.toString(), player);
                        tb.clear();
                        tb = null;
                    }
                    break;
                case 2:
                    showAddItem(player);
                    break;
            }
        } else if (command.startsWith("_bbsmail_act")) {
            String[] data = command.split("_"); //_bbsmail_act_3_x
            int act = Integer.parseInt(data[3]);
            int id = Integer.parseInt(data[4]);
            switch (act) {
                case 1:
                    showNewBrief(player);
                    break;
                case 2:
                    deleteBrief(player, id, false);
                    break;
                case 3:
                    getItemFrom(player, id);
                    break;
                case 4:
                    deleteBrief(player, id, true);
                    break;
                case 5:
                    getAugFrom(player, id);
                    break;
            }
        } else if (command.startsWith("_bbsmail_search")) {
            try {
                String[] data = command.substring(16).split(" _ "); //$search _ $keyword
                String type = data[0];
                String word = data[1];
                showSearchResult(player, word, type);
            } catch (Exception e) {
                TextBuilder tb = new TextBuilder("<html><body><br>Ошибка при обработке запроса.<br></body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
            }
        }
    }

    private void showNewBrief(L2PcInstance player) {
        String ansSend = player.getBriefSender();
        String ansThem = player.getMailTheme();
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>");
        /****/
        tb.append("<font color=00CC99>Совет: Если вы собираетесь отправить предмет, то сначала прикрепите его.<br>");
        tb.append("Отправка письма: " + Config.EXPOSTB_PRICE + " " + Config.EXPOSTB_NAME + ".<br1>");
        tb.append("Отправка предмета: " + Config.EXPOSTA_PRICE + " " + Config.EXPOSTA_NAME + ".</font><br>");
        tb.append("<table width=370><tr><td width=10> </td><td>Кому: " + (ansSend.equals("n.a") ? "" : "(" + ansSend + ")") + "</td><td><edit var=\"target\" width=150 length=\"22\"><br></td></tr>");
        tb.append("<tr><td width=10> </td><td>Тема: " + (ansThem.equals("n.a") ? "" : "(Re: " + ansThem + ")") + "</td><td><edit var=\"tema\" width=150 length=\"22\"><br></td></tr>");
        if (player.getBriefItem() == 0) {
            tb.append("<tr><td width=10> </td><td></td><td><a action=\"bypass _bbsmail_send 2\">$Прикрепить предмет</a><br></td></tr></table>");
        } else {
            L2ItemInstance item = player.getInventory().getItemByObjectId(player.getBriefItem());
            if (item == null) {
                tb.append("<tr><td width=10> </td><td></td><td><a action=\"bypass _bbsmail_send 2\">$Прикрепить предмет</a><br></td></tr></table>");
            } else {
                tb.append("</table>Предмет:<br><table width=200><tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><font color=LEVEL>" + item.getItem().getName() + " +" + item.getEnchantLevel() + "</font><br></td></tr></table><br>");
                if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                    tb.append("Аугмент: " + getAugmentSkill(item.getAugmentation().getAugmentSkill()) + "</font><br>");
                }
            }
        }
        tb.append("<table width=400><tr><td>Текст:<multiedit var=\"text\" width=280 height=70><br></td></tr>");
        if (ansThem.equals("n.a")) {
            tb.append("<tr><td><button value=\"Отправить\" action=\"bypass _bbsmail_send 1 $target _ $tema _ $text\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
        } else {
            tb.append("<tr><td><button value=\"Отправить\" action=\"bypass _bbsmail_send 1 " + ansSend + " _ " + ansThem + " _ $text\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
        }
        /****/
        tb.append("</td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void showIndex(L2PcInstance player, int type, int page) {
        player.setBriefItem(0);
        player.setBriefSender("n.a");
        player.setMailTheme("n.a");
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250 border=0><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        if (type == 1) {
            tb.append("</tr><tr><td>&nbsp;Входящие:</td></tr>");
            tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        } else {
            tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
            tb.append("<tr><td>&nbsp;Исходящие:<br></td></tr>");
        }
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480><font color=00CC99>Отправка письма: " + Config.EXPOSTB_PRICE + " " + Config.EXPOSTB_NAME + ".<br1>");
        tb.append("Отправка предмета: " + Config.EXPOSTA_PRICE + " " + Config.EXPOSTA_NAME + ".</font><br>");
        tb.append("<table width=500><tr><td></td><td></td><td></td><td></td></tr>");
        /** Список писем **/
        int limit1 = (page - 1) * PAGE_LIMIT;
        int limit2 = PAGE_LIMIT;
        int pageCount = 0;
        Date date = null;
        String dateNow = getNow();
        String briefDate = "";
        String loc = "to";

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            //con.setTransactionIsolation(1);
            if (type == 2) {
                st = con.prepareStatement("SELECT * FROM `z_bbs_mail` WHERE `from` = ? ORDER BY `datetime` DESC LIMIT ?, ?");
            } else {
                st = con.prepareStatement("SELECT * FROM `z_bbs_mail` WHERE `to` = ? ORDER BY `datetime` DESC LIMIT ?, ?");
                loc = "from";
            }
            st.setInt(1, player.getObjectId());
            st.setInt(2, limit1);
            st.setInt(3, limit2);
            rset = st.executeQuery();
            while (rset.next()) {
                int id = rset.getInt("id");
                String tema = rset.getString("tema");
                long time = rset.getLong("datetime");
                int read = rset.getInt("read");
                String name = getCharName(con, rset.getInt(loc));
                int item_id = rset.getInt("item_id");

                date = new Date(time);
                briefDate = _sdf.format(date);
                if (briefDate.equals(dateNow)) {
                    briefDate = _sdfTime.format(date);
                } else {
                    briefDate = _sdfDate.format(date);
                }

                tb.append("<tr><td width=16><img src=\"Icon.etc_letter_envelope_i00\" width=16 height=16></td><td align=left><font color=" + (read == 0 ? "CC00FF" : "6699CC") + "> " + name + " </td>");
                tb.append("<td align=left><a action=\"bypass _bbsmail_show " + id + "\"> " + (item_id > 1 ? "$" : "") + tema + " </a></td><td align=right> " + briefDate + " </font></td></tr>");
            }
            pageCount = getPageCount(con, player.getObjectId(), type);
        } catch (Exception e) {
            System.out.println("[ERROR] MailBBSManager, showIndex(" + player.getName() + ", " + type + ") " + e);
            e.printStackTrace();
        } finally {
            Close.CSR(con, st, rset);
        }
        /****/
        tb.append("</table><br>");
        if (pageCount > PAGE_LIMIT) {
            tb.append("&nbsp;&nbsp;Страницы: <br1>");
            int pages = (pageCount / PAGE_LIMIT) + 1;
            for (int i = 0; i < pages; i++) {
                int cur = i + 1;
                if (page == cur) {
                    tb.append("&nbsp;&nbsp;" + cur + "&nbsp;&nbsp;");
                } else {
                    tb.append("&nbsp;&nbsp;<a action=\"bypass _bbsmail_menu 2_0_" + cur + "\">" + cur + "</a>&nbsp;&nbsp;");
                }
            }
        }
        tb.append("</td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private int getPageCount(Connect con, int charId, int type) {
        int rowCount = 0;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            if (type == 2) {
                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_bbs_mail` WHERE `from` = ?");
            } else {
                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_bbs_mail` WHERE `to` = ?");
            }
            st.setInt(1, charId);
            rs = st.executeQuery();
            if (rs.next()) {
                rowCount = rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] MailBBSManager, getPageCount(con, " + charId + ", " + type + ") " + e);
            e.printStackTrace();
        } finally {
            Close.SR(st, rs);
        }
        if (rowCount == 0) {
            return 0;
        }

        return ((rowCount / PAGE_LIMIT) + 1);
    }

    private void showAddItem(L2PcInstance player) {
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>");
        /****/
        //tb.append("&nbsp;&nbsp;<a action=\"bypass _bbsmail_send 1\"><-Вернуться к письму</a><br>");
        tb.append("&nbsp;&nbsp;&nbsp;Выберите предмет, который хотите прикрепить:<br1><table width=370>");

        int i = 0;
        String augment = "";
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            int itemType = item.getItem().getType2();
            if (item.canBeEnchanted() && !item.isEquipped() && item.isDestroyable() && (itemType == L2Item.TYPE2_WEAPON || itemType == L2Item.TYPE2_SHIELD_ARMOR || itemType == L2Item.TYPE2_ACCESSORY)) {
                if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                    augment = "<br1>" + getAugmentSkill(item.getAugmentation().getAugmentSkill());
                }

                if (i == 0) {
                    tb.append("<tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><a action=\"bypass _bbsmail_menu 1_" + item.getObjectId() + "\"> " + item.getItem().getName() + " + " + item.getEnchantLevel() + "</a>" + augment + "</td><td width=30></td>");
                    i = 1;
                } else {
                    tb.append("<td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><a action=\"bypass _bbsmail_menu 1_" + item.getObjectId() + "\"> " + item.getItem().getName() + " + " + item.getEnchantLevel() + "</a>" + augment + "</td></tr>");
                    i = 0;
                }
                augment = "";
            }
        }
        /****/
        tb.append("</table></td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void sendBrief(L2PcInstance player, String target, String tema, String text) {
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>");
        /****/
        int item_id = 0;
        int item_count = 0;
        int item_ench = 0;
        int aug_id = 0;
        int aug_lvl = 0;
        int aug_hex = 0;

        int item_obj = 0;

        if (player.getBriefItem() != 0) {
            L2ItemInstance item = player.getInventory().getItemByObjectId(player.getBriefItem());
            if (item == null) {
                tb.append("&nbsp;&nbsp;Предмет не найден.");
                separateAndSend(tb.toString(), player);
                return;
            }
            item_id = item.getItemId();
            item_count = item.getCount();
            item_ench = item.getEnchantLevel();
            if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                L2Skill aug = item.getAugmentation().getAugmentSkill();
                aug_id = aug.getId();
                aug_lvl = aug.getLevel();
                aug_hex = item.getAugmentation().getAugmentationId();
            }
            item_obj = item.getObjectId();
        }

        int targetId = 0;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            //con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT obj_Id FROM `characters` WHERE `char_name` = ? LIMIT 0,1");
            st.setString(1, target);
            rset = st.executeQuery();
            if (rset.next()) {
                targetId = rset.getInt("obj_Id");
                Close.SR(st, rset);
            } else {
                tb.append("&nbsp;&nbsp;Персонаж с ником <font color=LEVEL>" + target + "</font> не найден.");
                separateAndSend(tb.toString(), player);
                return;
            }
            if (player.getBriefItem() > 0) {
                L2ItemInstance coin = player.getInventory().getItemByItemId(Config.EXPOSTA_COIN);
                if (coin == null || coin.getCount() < Config.EXPOSTA_PRICE) {
                    tb.append("<br><br>Отправка предмета: " + Config.EXPOSTA_PRICE + " " + Config.EXPOSTA_NAME + ".");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                player.destroyItemByItemId("MailBBSManager", Config.EXPOSTA_COIN, Config.EXPOSTA_PRICE, player, true);
                player.destroyItem("MailBBSManager", player.getBriefItem(), item_count, player, true);
            } else {
                L2ItemInstance coin = player.getInventory().getItemByItemId(Config.EXPOSTB_COIN);
                if (coin == null || coin.getCount() < Config.EXPOSTB_PRICE) {
                    tb.append("<br><br>Отправка письма: " + Config.EXPOSTB_PRICE + " " + Config.EXPOSTB_NAME + ".");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                player.destroyItemByItemId("MailBBSManager", Config.EXPOSTB_COIN, Config.EXPOSTB_PRICE, player, true);
            }

            st = con.prepareStatement("INSERT INTO `z_bbs_mail` (`from`, `to`, `tema`, `text`, `datetime`, `read`, `item_id`, `item_count`, `item_ench`, `aug_hex`, `aug_id`, `aug_lvl`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            st.setInt(1, player.getObjectId());
            st.setInt(2, targetId);
            st.setString(3, tema);
            st.setString(4, text);
            st.setLong(5, System.currentTimeMillis());
            st.setInt(6, 0);
            st.setInt(7, item_id);
            st.setInt(8, item_count);
            st.setInt(9, item_ench);
            st.setInt(10, aug_hex);
            st.setInt(11, aug_id);
            st.setInt(12, aug_lvl);
            st.execute();
            tb.append("&nbsp;&nbsp;Ваше письмо (" + tema + ") для <font color=LEVEL>" + target + "</font> отправлено.");
            L2PcInstance trg = L2World.getInstance().getPlayer(target);
            if (trg != null) {
                trg.sendPacket(new ExMailArrived());
            }

            Log.add(TimeLogger.getTime() + "MAIL " + item_id + "(" + item_count + ")(+" + item_ench + ")(" + item_obj + ")(aug: " + aug_hex + ";skill: " + aug_id + ";lvl: " + aug_lvl + ";) #(player " + player.getName() + ", account: " + player.getAccountName() + ", ip: " + player.getIP() + ", hwid: " + player.getHWID() + ")->" + (trg != null ? "(player " + trg.getName() + ", account: " + trg.getAccountName() + ", ip: " + trg.getIP() + ", hwid: " + player.getHWID() + ")" : "(targetId: " + targetId + ")"), "bbs_mail");
        } catch (final Exception e) {
            System.out.println("[ERROR] MailBBSManager, sendBrief() error: " + e);
        } finally {
            Close.CSR(con, st, rset);
        }
        /****/
        tb.append("</td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void showBrief(L2PcInstance player, int id) {
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>");
        /****/
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            //con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_mail` WHERE `id` = ? AND `to` = ? LIMIT 0, 1");
            st.setInt(1, id);
            st.setInt(2, player.getObjectId());
            rset = st.executeQuery();
            if (rset.next()) {
                String tema = rset.getString("tema");
                String text = rset.getString("text");
                long time = rset.getLong("datetime");
                int item_id = rset.getInt("item_id");
                int item_count = rset.getInt("item_count");
                int item_ench = rset.getInt("item_ench");
                int aug_id = rset.getInt("aug_id");
                int aug_lvl = rset.getInt("aug_lvl");
                //int aug_hex = rset.getInt("aug_hex");
                String sender = getCharName(con, rset.getInt("from"));

                Date date = new Date(time);
                String briefDate = _sdf.format(date);
                if (briefDate.equals(getNow())) {
                    briefDate = "Время отправки " + _sdfTime.format(date);
                } else {
                    briefDate = "Дата отправки " + _sdfDate.format(date);
                }

                text = strip(text);
                tema = strip(tema);

                player.setBriefSender(sender);
                player.setMailTheme(tema);

                tb.append("<table width=500><tr><td></td><td><img src=\"Icon.etc_letter_envelope_i00\" width=16 height=16></td>");
                tb.append("<td width=340>" + tema + " &nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\"><-Назад|x</a></td>");
                tb.append("<td width=80><button value=\"Ответить\" action=\"bypass _bbsmail_act_1_" + id + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
                tb.append("<td width=80><button value=\"Удалить\" action=\"bypass _bbsmail_act_2_" + id + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");

                tb.append("</tr></table><br><img src=\"sek.cbui355\" width=500 height=1><br><table width=600><tr>");
                tb.append("<td align=left width=380>От <font color=FFCC33> " + sender + " </font></td>");
                tb.append("<td align=left width=220> " + briefDate + "</td></tr></table><br>");
                tb.append("" + text + "<br><br><br>");
                tb.append("<img src=\"sek.cbui355\" width=500 height=1><br>");

                if (item_id > 0) {
                    L2Item item = ItemTable.getInstance().getTemplate(item_id);
                    if (item == null) {
                        tb.append("");
                    } else {
                        String augment = "";
                        if (aug_id > 0) {
                            L2Skill aug = SkillTable.getInstance().getInfo(aug_id, aug_lvl);
                            if (aug != null) {
                                augment = "<br1>" + getAugmentSkill(aug);
                            }
                        }
                        tb.append("Прикрепленные предметы:<br><table width=400 border=0><tr><td width=32><img src=\"" + item.getIcon() + "\" width=32 height=32></td>");
                        tb.append("<td width=342 align=left><font color=LEVEL>" + item.getName() + "(" + item_count + ") +" + item_ench + " </font>" + augment + "</td></tr>");
                        tb.append("<tr><td width=32></td><td width=342 align=left><br><br><img src=\"sek.cbui355\" width=300 height=1><br>");
                        tb.append("<button value=\"Забрать\" action=\"bypass _bbsmail_act_3_" + id + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
                    }
                } else if (aug_id > 0) {
                    String augment = "";
                    L2Skill aug = SkillTable.getInstance().getInfo(aug_id, aug_lvl);
                    if (aug != null) {
                        augment = "<br1>" + getAugmentSkill(aug);
                    }

                    tb.append("Прикрепленные аугменты:<br>");
                    tb.append("" + augment + "");
                    tb.append("<br><br><img src=\"sek.cbui355\" width=300 height=1><br>");
                    tb.append("<button value=\"Забрать\" action=\"bypass _bbsmail_act_5_" + id + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
                }
                Close.SR(st, rset);
                st = con.prepareStatement("UPDATE `z_bbs_mail` SET `read`= ? WHERE `id`= ?");
                st.setInt(1, 1);
                st.setInt(2, id);
                st.executeUpdate();
            } else {
                tb.append("&nbsp;&nbsp;Письмо не найдено.");
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] MailBBSManager, showBrief() error: " + e);
        } finally {
            Close.CSR(con, st, rset);
        }
        /****/
        tb.append("</td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void getItemFrom(L2PcInstance player, int id) {
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>");
        /****/
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            //con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_mail` WHERE `id` = ? AND `to` = ? LIMIT 0, 1");
            st.setInt(1, id);
            st.setInt(2, player.getObjectId());
            rset = st.executeQuery();
            if (rset.next()) {
                //String tema = rset.getString("tema");
                //String text = rset.getString("text");
                //long time = rset.getLong("datetime");
                int item_id = rset.getInt("item_id");
                int item_count = rset.getInt("item_count");
                int item_ench = rset.getInt("item_ench");
                int aug_id = rset.getInt("aug_id");
                int aug_lvl = rset.getInt("aug_lvl");
                int aug_hex = rset.getInt("aug_hex");
                //String sender = getCharName(con, rset.getInt("from"));
                if (item_id > 0) {
                    L2Item item = ItemTable.getInstance().getTemplate(item_id);
                    if (item == null) {
                        tb.append("");
                    } else {
                        L2ItemInstance reward = player.getInventory().addItem("MailBBSManager", item_id, item_count, player, player.getTarget());
                        if (reward == null) {
                            tb.append("&nbsp;&nbsp;Письмо не найдено.");
                            separateAndSend(tb.toString(), player);
                            return;
                        }
                        if (item_ench > 0) {
                            reward.setEnchantLevel(item_ench);
                        }
                        if (aug_id > 0) {
                            reward.setAugmentation(new L2Augmentation(reward, aug_hex, aug_id, aug_lvl, true));
                        }

                        if (item_count > 1) {
                            player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(item_id).addNumber(item_count));
                        } else {
                            player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(item_id));
                        }
                        player.sendItems(true);
                    }
                }
                Close.SR(st, rset);
                st = con.prepareStatement("UPDATE `z_bbs_mail` SET `item_id`=?, `item_count`=?, `item_ench`=?, `aug_hex`=?, `aug_id`=?, `aug_lvl`=? WHERE `id`= ?");
                st.setInt(1, 0);
                st.setInt(2, 0);
                st.setInt(3, 0);
                st.setInt(4, 0);
                st.setInt(5, 0);
                st.setInt(6, 0);
                st.setInt(7, id);
                st.executeUpdate();
                tb.append("&nbsp;&nbsp;Забрали.");
            } else {
                tb.append("&nbsp;&nbsp;Письмо не найдено.");
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] MailBBSManager, showBrief() error: " + e);
        } finally {
            Close.CSR(con, st, rset);
        }
        /****/
        tb.append("</td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void getAugFrom(L2PcInstance player, int id) {
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>");
        /****/
        player.setBriefItem(id);
        tb.append("&nbsp;&nbsp;&nbsp;Выберите предмет, в который хотите вставить аугмент:<br1><table width=370>");

        int i = 0;
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            int itemType = item.getItem().getType2();
            if (item.canBeEnchanted() && !item.isAugmented() && !item.isEquipped() && item.isDestroyable() && itemType == L2Item.TYPE2_WEAPON) {
                if (i == 0) {
                    tb.append("<tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><a action=\"bypass _bbsmail_menu 5_" + item.getObjectId() + "\"> " + item.getItem().getName() + " + " + item.getEnchantLevel() + "</a></td><td width=30></td>");
                    i = 1;
                } else {
                    tb.append("<td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><a action=\"bypass _bbsmail_menu 5_" + item.getObjectId() + "\"> " + item.getItem().getName() + " + " + item.getEnchantLevel() + "</a></td></tr>");
                    i = 0;
                }
            }
        }
        /****/
        tb.append("</table></td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void addAugTo(L2PcInstance player, int item_id) {
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>");
        /****/
        int briefId = player.getBriefItem();
        if (briefId == 0 || briefId > 1000000) {
            tb.append("&nbsp;&nbsp;Предмет не найден.");
            separateAndSend(tb.toString(), player);
            return;
        }

        L2ItemInstance item = player.getInventory().getItemByObjectId(item_id);
        if (item == null) {
            tb.append("&nbsp;&nbsp;Предмет не найден.");
            separateAndSend(tb.toString(), player);
            return;
        }

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            //con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_mail` WHERE `id` = ? AND `to` = ? LIMIT 0, 1");
            st.setInt(1, briefId);
            st.setInt(2, player.getObjectId());
            rset = st.executeQuery();
            if (rset.next()) {
                int aug_id = rset.getInt("aug_id");
                int aug_lvl = rset.getInt("aug_lvl");
                int aug_hex = rset.getInt("aug_hex");
                if (aug_id > 0) {
                    L2Skill aug = SkillTable.getInstance().getInfo(aug_id, aug_lvl);
                    if (aug == null) {
                        tb.append("&nbsp;&nbsp;Предмет не найден.");
                        separateAndSend(tb.toString(), player);
                        return;
                    } else {
                        item.setAugmentation(new L2Augmentation(item, aug_hex, aug_id, aug_lvl, true));
                        player.sendItems(true);
                        player.sendPacket(SystemMessage.id(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
                    }
                } else {
                    tb.append("&nbsp;&nbsp;Предмет не найден.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                Close.SR(st, rset);
                st = con.prepareStatement("UPDATE `z_bbs_mail` SET `item_id`=?, `item_count`=?, `item_ench`=?, `aug_hex`=?, `aug_id`=?, `aug_lvl`=? WHERE `id`= ?");
                st.setInt(1, 0);
                st.setInt(2, 0);
                st.setInt(3, 0);
                st.setInt(4, 0);
                st.setInt(5, 0);
                st.setInt(6, 0);
                st.setInt(7, briefId);
                st.executeUpdate();
                tb.append("&nbsp;&nbsp;Забрали.");
            } else {
                tb.append("&nbsp;&nbsp;Письмо не найдено.");
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] MailBBSManager, showBrief() error: " + e);
        } finally {
            Close.CSR(con, st, rset);
        }
        /****/
        tb.append("</td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void deleteBrief(L2PcInstance player, int id, boolean force) {
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>");
        /****/
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            //con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_mail` WHERE `id` = ? AND `to` = ? LIMIT 0, 1");
            st.setInt(1, id);
            st.setInt(2, player.getObjectId());
            rset = st.executeQuery();
            if (rset.next()) {
                String tema = rset.getString("tema");
                int item_id = rset.getInt("item_id");
                int item_ench = rset.getInt("item_ench");
                int aug_id = rset.getInt("aug_id");
                int aug_lvl = rset.getInt("aug_lvl");
                if (item_id > 0) {
                    L2Item item = ItemTable.getInstance().getTemplate(item_id);
                    if (item == null) {
                        tb.append("");
                    } else {
                        String augment = "";
                        if (aug_id > 0) {
                            L2Skill aug = SkillTable.getInstance().getInfo(aug_id, aug_lvl);
                            if (aug != null) {
                                augment = "<br1>" + getAugmentSkill(aug);
                            }
                        }
                        tb.append("<font color=FF9966>!Письмо содержит прикрепленный предмет:</font><br><table width=400 border=0><tr><td width=32><img src=\"" + item.getIcon() + "\" width=32 height=32></td>");
                        tb.append("<td width=342 align=left><font color=LEVEL>" + item.getName() + " +" + item_ench + " </font>" + augment + "</td></tr>");
                        tb.append("<tr><td width=32></td><td width=342 align=left><br><br><img src=\"sek.cbui355\" width=300 height=1><br>");
                        tb.append("<button value=\"Удалить\" action=\"bypass _bbsmail_act_2_4\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
                    }
                } else if (item_id == 0 || force) {
                    Close.SR(st, rset);
                    st = con.prepareStatement("DELETE FROM `z_bbs_mail` WHERE `id`= ?");
                    st.setInt(1, id);
                    st.executeUpdate();
                    tb.append("&nbsp;&nbsp;Письмо (" + tema + ") удалено.");
                }
            } else {
                tb.append("&nbsp;&nbsp;Письмо не найдено.");
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] MailBBSManager, deleteBrief() error: " + e);
        } finally {
            Close.CSR(con, st, rset);
        }
        /****/
        tb.append("</td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void showSearchResult(L2PcInstance player, String type, String word) {
        player.setBriefItem(0);
        player.setBriefSender("n.a");
        player.setMailTheme("n.a");
        TextBuilder tb = new TextBuilder("");
        tb.append("<html><body><br><br><center><table width=650 border=0><tr><td><img src=\"Icon.etc_letter_envelope_i00\" width=32 height=32><font color=CC9933>@</font><font color=CC00FF>" + player.getName() + "</font></td>");
        tb.append("<td width=480><table width=250 border=0><tr><td><edit var=\"search\" width=150 length=\"22\"></td><td><combobox width=51 var=keyword list=\"Тема;Автор\"></td>");
        tb.append("<td><button value=\"Найти\" action=\"bypass _bbsmail_search $search _ $keyword\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr>");
        tb.append("<tr><td><br><br><table width=100><tr><td><button value=\"Новое письмо\" action=\"bypass _bbsmail_menu 1_0\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br></td>");
        tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 3_0_1\">Исходящие</a><br></td></tr>");
        tb.append("</tr><tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 2_0_1\">Входящие</a></td></tr>");
        //tb.append("<tr><td>&nbsp;<a action=\"bypass _bbsmail_menu 4\">Контакты</a></td></tr></table></td>");
        tb.append("</table></td>");
        tb.append("<td width=480>Поиск по: " + type + "+" + word + "<table width=500><tr><td></td><td></td><td></td><td></td></tr>");
        /** Список писем **/
        Date date = null;
        String dateNow = getNow();
        String briefDate = "";

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            //con.setTransactionIsolation(1);
            if (type.equals("Тема")) {
                st = con.prepareStatement("SELECT * FROM `z_bbs_mail` WHERE `to` = ? AND `tema` LIKE '%?%' ORDER BY `datetime` DESC LIMIT 0, 20");
            } else {
                st = con.prepareStatement("SELECT * FROM `z_bbs_mail` WHERE `to` = ? AND `from` LIKE '%?%' ORDER BY `datetime` DESC LIMIT 0, 20");
            }

            st.setInt(1, player.getObjectId());
            rset = st.executeQuery();
            while (rset.next()) {
                int id = rset.getInt("id");
                String tema = rset.getString("tema");
                long time = rset.getLong("datetime");
                int read = rset.getInt("read");
                String name = getCharName(con, rset.getInt("from"));
                int item_id = rset.getInt("item_id");

                date = new Date(time);
                briefDate = _sdf.format(date);
                if (briefDate.equals(dateNow)) {
                    briefDate = _sdfTime.format(date);
                } else {
                    briefDate = _sdfDate.format(date);
                }

                tb.append("<tr><td width=16><img src=\"Icon.etc_letter_envelope_i00\" width=16 height=16></td><td align=left><font color=" + (read == 0 ? "CC00FF" : "6699CC") + "> " + name + " </td>");
                tb.append("<td align=left><a action=\"bypass _bbsmail_show " + id + "\"> " + (item_id > 1 ? "$" : "") + tema + " </a></td><td align=right> " + briefDate + " </font></td></tr>");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] MailBBSManager, showIndex(" + player.getName() + ", " + type + ") " + e);
            e.printStackTrace();
        } finally {
            Close.CSR(con, st, rset);
        }
        /****/
        tb.append("</table></td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private String getNow() {
        return _sdf.format(_date);
    }

    private String strip(String text) {
        text = text.replaceAll("<a>", "");
        text = text.replaceAll("</a>", "");
        text = text.replaceAll("<font>", "");
        text = text.replaceAll("</font>", "");
        text = text.replaceAll("<table>", "");
        text = text.replaceAll("<tr>", "");
        text = text.replaceAll("<td>", "");
        text = text.replaceAll("</table>", "");
        text = text.replaceAll("</tr>", "");
        text = text.replaceAll("</td>", "");
        text = text.replaceAll("<br>", "");
        text = text.replaceAll("<br1>", "");
        text = text.replaceAll("<button", "");
        return text;
    }

    private String getAugmentSkill(L2Skill augment) {
        if (augment == null) {
            return "";
        }

        String augName = augment.getName();
        String type = "";
        if (augment.isActive()) {
            type = "Актив";
        } else if (augment.isPassive()) {
            type = "Пассив";
        } else {
            type = "Шанс";
        }

        augName = augName.replace("Item Skill: ", "");

        return "<font color=336699>Аугмент:</font> <font color=bef574>" + augName + " (" + type + ":" + augment.getLevel() + "lvl)</font>";
    }

    private String getCharName(Connect con, int objId) {
        if (objId == 555) {
            return Config.POST_NPCNAME;
        }
        if (objId == 777) {
            return "=Аукцион=";
        }
        if (objId == 334) {
            return "~Fight Club.";
        }

        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            //con = L2DatabaseFactory.get();
            //con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT char_name FROM `characters` WHERE `obj_Id` = ? LIMIT 0,1");
            st.setInt(1, objId);
            rset = st.executeQuery();
            if (rset.next()) {
                return rset.getString("char_name");
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] MailBBSManager, getCharName() error: " + e);
        } finally {
            Close.SR(st, rset);
        }
        return "???";
    }

    @Override
    public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance player) {
        // TODO Auto-generated method stub
    }
}