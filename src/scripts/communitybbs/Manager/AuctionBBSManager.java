package scripts.communitybbs.Manager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javolution.text.TextBuilder;
import javolution.util.FastMap;

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
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Rnd;

public class AuctionBBSManager extends BaseBBSManager {

    private static AuctionBBSManager _instance;

    public static void init() {
        _instance = new AuctionBBSManager();
        _instance.cacheMoneys();
        _instance.cacheMenu();
        _instance.returnExpiredLots();
    }

    public static AuctionBBSManager getInstance() {
        return _instance;
    }
    private static final int PAGE_LIMIT = 20;
    private static final int SORT_LIMIT = 8;
    private static final String CENCH = "CCCC33";
    private static final String CPRICE = "669966";
    private static final String CITEM = "993366";
    private static final String CAUG1 = "333366";
    private static final String CAUG2 = "006699";
    private static String MONEY_VARS = "Adena;";
    private static String AUC_MENU = "";
    private static final FastMap<Integer, String> moneys = Config.BBS_AUC_MONEYS;

    @Override
    public void parsecmd(String command, L2PcInstance player) {
        if (command.equalsIgnoreCase("_bbsauc")) {
            showIndex(player); // index
        } else if (command.equalsIgnoreCase("_bbsauc_office")) {
            showOffice(player); // office
        } else if (command.startsWith("_bbsauc_fsearch")) {
            showSearch(player);
        } else if (command.equalsIgnoreCase("_bbsauc_add")) {
            showAdd(player); // add
        } else if (command.startsWith("_bbsauc_show")) {
            showItem(player, Integer.parseInt(command.substring(12).trim()));
        } else if (command.startsWith("_bbsauc_enchanted")) {
            showAddEnch(player, Integer.parseInt(command.substring(17).trim()));
        } else if (command.startsWith("_bbsauc_augment")) {
            showAddAug(player, Integer.parseInt(command.substring(15).trim()));
        } else if (command.startsWith("_bbsauc_custom")) {
            showAddCustom(player, Integer.parseInt(command.substring(14).trim()));
        } else if (command.startsWith("_bbsauc_bue")) // _bbsauc_bue 1 2
        {
            String[] opaopa = command.substring(11).split(" ");
            int id = Integer.parseInt(opaopa[2]);
            String pass;
            try {
                pass = opaopa[3];
            } catch (Exception e) {
                pass = "no";
            }
            switch (Integer.parseInt(opaopa[1])) {
                case 0:
                case 1:
                case 2:
                case 3:
                    getItemFrom(player, id, pass);
                    break;
                case 4:
                    getAugFrom(player, id, pass);
                    break;
                case 5:
                    getSkillFrom(player, id, pass);
                    break;
                case 6:
                    getHeroFrom(player, id, pass);
                    break;
            }
        } else if (command.startsWith("_bbsauc_menu")) {
            String[] opaopa = command.substring(13).split("_");
            switch (Integer.parseInt(opaopa[0])) {
                case 5:
                    addAugTo(player, Integer.parseInt(opaopa[1]), opaopa[2]);
                    break;
            }
        } else if (command.startsWith("_bbsauc_step2_")) // _bbsauc_step2_123_1_ $price _ $type _ $paytype _$pass
        {
            String[] opaopa = command.substring(14).split("_");
            try {
                Integer item_obj = Integer.parseInt(opaopa[0]);
                Integer item_type = Integer.parseInt(opaopa[1]);
                Integer item_price = Integer.parseInt(opaopa[2].trim());
                String price_type = opaopa[3].trim();
                String pay_type = opaopa[4].trim();
                String pwd = opaopa[5].trim();
                String pwduse = opaopa[6].trim();
                //System.out.println("1#" + item_obj + ";2#" + item_type + ";3#" + item_price + ";4#" + price_type + ";5#" + pay_type + ";");
                if (item_obj == null || item_type == null || item_price == null || price_type.length() < 2) {
                    TextBuilder tb = new TextBuilder("");
                    tb.append(getMenu());
                    tb.append("&nbsp;&nbsp;Произошла ошибка, step2<br></body></html>");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                addToAuc(player, item_obj, item_type, item_price, price_type, (pay_type.equals("YES") ? 0 : 1), pwd, (pwduse.equals("YES") ? 1 : 0));
            } catch (Exception e) {
                TextBuilder tb = new TextBuilder("");
                tb.append(getMenu());
                tb.append("&nbsp;&nbsp;Произошла ошибка, step2.2<br></body></html>");
                separateAndSend(tb.toString(), player);
            }
        } else if (command.startsWith("_bbsauc_pageshow")) // bypass _bbsauc_pageshow_1_1_0_0_-1
        {
            player.setBriefItem(0);
            String[] opaopa = command.substring(17).split("_");
            TextBuilder tb = new TextBuilder("");
            try {
                Integer page = Integer.parseInt(opaopa[0]);
                Integer self = Integer.parseInt(opaopa[1]);
                Integer item_id = Integer.parseInt(opaopa[2]);
                Integer aug_id = Integer.parseInt(opaopa[3]);
                Integer type = Integer.parseInt(opaopa[4]);
                tb.append(getMenu());
                /****/
                tb.append(showSellItems(player, page, self, 0, item_id, aug_id, type));
                /****/
                tb.append("<br></body></html>");
            } catch (Exception e) {
                tb.clear();
                tb.append(getMenu());
                /****/
                tb.append("&nbsp;&nbsp;Произошла ошибка, pageshow<br></body></html>");
            }
            separateAndSend(tb.toString(), player);
        } else if (command.startsWith("_bbsauc_search")) // _bbsauc_search type_itemId/skillId_augId
        {
            TextBuilder tb = new TextBuilder("");
            String[] opaopa = command.substring(15).split("_");
            tb.append(getMenu());
            /****/
            tb.append(showSellItems(player, 1, 0, 0, Integer.parseInt(opaopa[1]), Integer.parseInt(opaopa[2]), Integer.parseInt(opaopa[0])));
            /****/
            tb.append("<br></body></html>");
            separateAndSend(tb.toString(), player);
        }
    }

    private void showIndex(L2PcInstance player) {
        player.setBriefItem(0);
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        tb.append(showSellItems(player, 1, 0, 0, 0, 0, -1));
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private String showSellItems(L2PcInstance player, int page, int me, int last, int itemId, int augment, int type2) {
        TextBuilder text = new TextBuilder("&nbsp;&nbsp;Страница " + page + ":<br>");
        text.append("<table width=650 border=0>");
        int limit1 = (page - 1) * PAGE_LIMIT;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            if (type2 >= 0) {
                st = con.prepareStatement("SELECT id, itemId, itemName, enchant, augment, augLvl, price, money, type, ownerId, shadow, pwd FROM `z_bbs_auction` WHERE `type` = ? ORDER BY `id` DESC LIMIT ?, ?");
                st.setInt(1, type2);
                st.setInt(2, limit1);
                st.setInt(3, PAGE_LIMIT);
            } else if (itemId > 0) {
                st = con.prepareStatement("SELECT id, itemId, itemName, enchant, augment, augLvl, price, money, type, ownerId, shadow, pwd FROM `z_bbs_auction` WHERE `itemId` = ? ORDER BY `id` DESC LIMIT ?, ?");
                st.setInt(1, itemId);
                st.setInt(2, limit1);
                st.setInt(3, PAGE_LIMIT);
            } else if (augment > 0) {
                st = con.prepareStatement("SELECT id, itemId, itemName, enchant, augment, augLvl, price, money, type, ownerId, shadow, pwd FROM `z_bbs_auction` WHERE `augment` = ? ORDER BY `id` DESC LIMIT ?, ?");
                st.setInt(1, augment);
                st.setInt(2, limit1);
                st.setInt(3, PAGE_LIMIT);
            } else if (me == 1) {
                st = con.prepareStatement("SELECT id, itemId, itemName, enchant, augment, augLvl, price, money, type, ownerId, shadow, pwd FROM `z_bbs_auction` WHERE `ownerId` = ? ORDER BY `id` DESC LIMIT ?, ?");
                st.setInt(1, player.getObjectId());
                st.setInt(2, limit1);
                st.setInt(3, PAGE_LIMIT);
            } else {
                st = con.prepareStatement("SELECT id, itemId, itemName, enchant, augment, augLvl, price, money, type, ownerId, shadow, pwd FROM `z_bbs_auction` ORDER BY `id` DESC LIMIT ?, ?");
                st.setInt(1, limit1);
                st.setInt(2, PAGE_LIMIT);
            }
            int i = 0;
            rs = st.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                int itmId = rs.getInt("itemId");
                String name = rs.getString("itemName");
                int ownerId = rs.getInt("ownerId");
                int augId = rs.getInt("augment");
                int augLvl = rs.getInt("augLvl");
                int enchant = rs.getInt("enchant");
                int type = rs.getInt("type");
                String pwd = rs.getString("pwd");
                /*try
                {
                pwd = rs.getString("pwd");
                }
                catch(Exception e)
                {
                pwd = "a";
                }*/
                String priceB = "<font color=" + CPRICE + ">" + Util.formatAdena(rs.getInt("price")) + " " + getMoneyCall(rs.getInt("money")) + "; \"" + getSellerName(con, ownerId) + "</font>";
                //if (player.getObjectId() == ownerId)
                //	priceB = "<table width=240><tr><td width=160><font color=666699>" + Util.formatAdena(rs.getInt("price")) + " " + getMoneyCall(rs.getInt("money")) + ";</font></td><td align=right><button value=\"X\" action=\"bypass _bbsauc_StockBuyItem_" + sId + "\" width=25 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table><br1>";

                String icon = "";
                String ench = "";
                String augm = "";
                switch (type) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        L2Item item = ItemTable.getInstance().getTemplate(itmId);
                        if (item == null) {
                            continue;
                        }

                        name = item.getName();
                        icon = item.getIcon();
                        ench = "+" + enchant + "";
                        if (augId > 0) {
                            augm = " " + getAugmentSkill(augId, augLvl) + "<br1>";
                        }
                        break;
                    case 4:
                        name = getAugmentSkill(augId, augLvl);
                        icon = "Icon.skill3123";
                        break;
                    case 5:
                        name = "Скилл: " + name;
                        icon = "Icon.etc_spell_books_element_i00";
                        ench = enchant + "lvl";
                        break;
                    case 6:
                        name = name.replace("Hero: ", "Геройство: ");
                        icon = "Icon.skill1374";
                        break;
                }

                if (i == 0) {
                    text.append("<tr><td><table width=300><tr><td><img src=\"" + icon + "\" width=32 height=32></td>");
                    text.append("<td width=270><a action=\"bypass _bbsauc_show " + id + "\">" + name + "</a> <font color=993366> " + ench + "</font><br1> " + augm + "");
                    text.append("<font color=CC3366> " + (pwd.equals(" ") ? "" : "***") + "</font> " + priceB + "</td></tr></table></td>");
                    i = 1;
                } else {
                    text.append("<td><table width=300><tr><td><img src=\"" + icon + "\" width=32 height=32></td>");
                    text.append("<td width=270><a action=\"bypass _bbsauc_show " + id + "\">" + name + "</a> <font color=993366> " + ench + "</font><br1> " + augm + "");
                    text.append("<font color=CC3366> " + (pwd.equals(" ") ? "" : "***") + "</font> " + priceB + "</td></tr></table></td></tr>");
                    i = 0;
                }
            }
            text.append("</table><br>");
            if (last == 1) {
                text.append("<br>");
            } else {
                int pages = getPageCount(con, me, itemId, augment, type2, player.getObjectId());
                if (pages >= 2) {
                    text.append(sortPages(page, pages, me, itemId, augment, type2));
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, showSellItems() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        return (text.toString());
    }

    private void showItem(L2PcInstance player, int id) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_auction` WHERE `id`=? LIMIT 1");
            st.setInt(1, id);
            rs = st.executeQuery();
            if (rs.next()) {
                int item_id = rs.getInt("itemId");
                String name = rs.getString("itemName");
                int aug_id = rs.getInt("augment");
                int aug_lvl = rs.getInt("augLvl");
                int item_ench = rs.getInt("enchant");
                int owner = rs.getInt("ownerId");
                int price = rs.getInt("price");
                int money = rs.getInt("money");
                int item_type = rs.getInt("type");
                int pay_type = rs.getInt("pay");
                String pwd = rs.getString("pwd");

                if (pay_type == 1) {
                    int coin_id = 0;
                    int coin_price = 0;
                    String coin_name = "";
                    switch (item_type) {
                        case 0:
                        case 1:
                        case 2:
                            coin_id = Config.BBS_AUC_ITEM_COIN;
                            coin_price = Config.BBS_AUC_ITEM_PRICE;
                            coin_name = Config.BBS_AUC_ITEM_NAME;
                            break;
                        case 3:
                        case 4:
                            coin_id = Config.BBS_AUC_AUG_COIN;
                            coin_price = Config.BBS_AUC_AUG_PRICE;
                            coin_name = Config.BBS_AUC_AUG_NAME;
                            break;
                        case 5:
                            coin_id = Config.BBS_AUC_SKILL_COIN;
                            coin_price = Config.BBS_AUC_SKILL_PRICE;
                            coin_name = Config.BBS_AUC_SKILL_NAME;
                            break;
                        case 6:
                            coin_id = Config.BBS_AUC_HERO_COIN;
                            coin_price = Config.BBS_AUC_HERO_PRICE;
                            coin_name = Config.BBS_AUC_HERO_NAME;
                            break;
                    }
                    if (coin_id > 0 && player.getObjectId() != owner) {
                        tb.append("<br><br>&nbsp;&nbsp;<font color=FF9933>Необходимо оплатить налог аукциона: " + coin_price + " " + coin_name + ".</font>");
                    }
                }

                String icon = "";
                String ench = "";
                String augm = "";
                switch (item_type) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        L2Item item = ItemTable.getInstance().getTemplate(item_id);
                        if (item == null) {
                            tb.append("&nbsp;&nbsp;Ошибка.");
                            separateAndSend(tb.toString(), player);
                            return;
                        }

                        name = item.getName();
                        icon = item.getIcon();
                        ench = "+" + item_ench + "";
                        if (aug_id > 0) {
                            augm = " " + getAugmentSkill(aug_id, aug_lvl) + "<br1>";
                        }
                        break;
                    case 4:
                        name = getAugmentSkill(aug_id, aug_lvl);
                        icon = "Icon.skill3123";
                        break;
                    case 5:
                        icon = "Icon.etc_spell_books_element_i00";
                        ench = item_ench + "lvl";
                        break;
                    case 6:
                        icon = "Icon.skill1374";
                        break;
                }

                tb.append("<br><table width=400 border=0><tr><td width=32><img src=\"" + icon + "\" width=32 height=32></td>");
                tb.append("<td width=342 align=left><font color=LEVEL>" + name + " " + ench + " </font> " + augm + "</td></tr>");
                tb.append("<tr><td width=32></td><td width=342 align=left><br><br><img src=\"sek.cbui355\" width=300 height=1><br></td></tr></table>");
                String priceB = "<br>&nbsp;&nbsp;<font color=" + CPRICE + ">Стоимость: " + Util.formatAdena(price) + " " + getMoneyCall(money) + "; <br1> &nbsp;&nbsp;Продавец: " + getSellerName(con, owner) + "</font><br>";
                if (player.getObjectId() == owner) {
                    priceB = "<br>&nbsp;&nbsp;<button value=\"Забрать\" action=\"bypass _bbsauc_bue " + item_type + " " + id + " no\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>";
                }
                tb.append(priceB);
                if (player.getObjectId() != owner) {
                    L2ItemInstance coin = player.getInventory().getItemByItemId(money);
                    if (coin == null || coin.getCount() < price) {
                        tb.append("&nbsp;&nbsp;<font color=999999>[Купить]</font>");
                    } else {
                        if (pwd.length() > 2) {
                            tb.append("&nbsp;&nbsp;Лот защищен паролем, введите пароль:<br>");
                            tb.append("&nbsp;&nbsp;<edit var=\"pass\" width=70 length=\"16\"><br>");
                            tb.append("&nbsp;&nbsp;<button value=\"Купить\" action=\"bypass _bbsauc_bue " + item_type + " " + id + " $pass\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                        } else {
                            tb.append("&nbsp;&nbsp;<button value=\"Купить\" action=\"bypass _bbsauc_bue " + item_type + " " + id + " no\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                        }
                    }
                }
            } else {
                tb.append("&nbsp;&nbsp;Нe найдена или уже купили.");
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, showItem() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void showOffice(L2PcInstance player) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        tb.append("<table><tr><td width=260>Личный кабинет " + player.getName() + "</td></tr></table><br1>");
        //tb.append("Привет, " + player.getName() + ".<br>");
        tb.append("<button value=\"Мои лоты\" action=\"bypass _bbsauc_pageshow_1_1_0_0_-1\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        tb.append("</body></html>");
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void showSearch(L2PcInstance player) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        tb.append("<table width=500><tr><td width=5>&nbsp;&nbsp;Поиск: </td><td><font color=LEVEL>Что ищем?</font><br1>");
        tb.append("Оружие:<br1>");
        tb.append("<table width=240><tr><td><button value=\"Заточенное\" action=\"bypass _bbsauc_search 0_0_0\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        tb.append("<td><button value=\"Аугментированное\" action=\"bypass _bbsauc_search 3_0_0\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
        tb.append("</tr></table><br>Заточенная броня:<br1>");
        tb.append("<button value=\"Одежда\" action=\"bypass _bbsauc_search 1_0_0\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        tb.append("<button value=\"Бижутерия\" action=\"bypass _bbsauc_search 2_0_0\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        tb.append("Прочее:<br1>");
        tb.append("<button value=\"Аугмент\" action=\"bypass _bbsauc_search 4_0_0\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        tb.append("<button value=\"Скилл\" action=\"bypass _bbsauc_search 5_0_0\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        tb.append("<button value=\"Геройство\" action=\"bypass _bbsauc_search 6_0_0\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        /****/
        tb.append("</td></tr></table><br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void showAdd(L2PcInstance player) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        String content = getPwHtm("menu-auction");
        if (content == null) {
            content = "<html><body><br><br><center>404 :File Not found: '" + PWHTML + "menu-auction.htm' </center></body></html>";
        }
        tb.append(content);
        //tb.append("<button value=\"Геройство\" action=\"bypass _bbsauc_custom 6\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void showAddEnch(L2PcInstance player, int type) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        int pwd = Rnd.get(1100, 9999);
        tb.append("<br><br>&nbsp;&nbsp;Стоимость услуги: " + Config.BBS_AUC_ITEM_PRICE + " " + Config.BBS_AUC_ITEM_NAME + ".<br><br>");
        tb.append("&nbsp;&nbsp;Шаг 1. Введите желаемую стоимость: <br><table width=300><tr><td><edit var=\"price\" width=70 length=\"16\"></td><td><combobox width=100 var=type list=\"" + MONEY_VARS + "\"></td></tr></table><br>");
        tb.append("&nbsp;&nbsp;Шаг 2. Оплачиваете налог аукциона? (Да: YES; Оплатит покупатель: NO)) <br><table width=300><tr><td></td><td><combobox width=100 var=payer list=\"YES;NO\"></td></tr></table><br>");
        tb.append("&nbsp;&nbsp;Шаг 3. Использовать пароль? Ваш пароль: <font color=LEVEL>" + pwd + "</font> <br><table width=300><tr><td></td><td><combobox width=100 var=pass list=\"NO;YES\"></td></tr></table><br>");
        tb.append("<br>&nbsp;&nbsp;&nbsp;Шаг 4. Выберите предмет, который хотите выставить:<br1><table width=650>");

        int i = 0;
        String augment = "";
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (item.getItem().getType2() == type && item.getEnchantLevel() > 0 && item.canBeEnchanted() && !item.isEquipped() && item.isDestroyable()) {
                if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                    augment = "<br1>" + getAugmentSkill(item.getAugmentation().getAugmentSkill().getId(), item.getAugmentation().getAugmentSkill().getLevel());
                }

                if (i == 0) {
                    tb.append("<tr><td><table width=300><tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td>");
                    tb.append("<td width=270><a action=\"bypass _bbsauc_step2_1_2_3_" + item.getObjectId() + "_0_ $price _ $type _ $payer _ " + pwd + " _ $pass\">" + item.getItem().getName() + "</a> <font color=993366> +" + item.getEnchantLevel() + "</font> " + augment + "");
                    tb.append("</td></tr></table></td>");
                    i = 1;
                } else {
                    tb.append("<td><table width=300><tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td>");
                    tb.append("<td width=270><a action=\"bypass _bbsauc_step2_1_2_3_" + item.getObjectId() + "_0_ $price _ $type _ $payer _ " + pwd + " _ $pass\">" + item.getItem().getName() + "</a> <font color=993366> +" + item.getEnchantLevel() + "</font> " + augment + "");
                    tb.append("</td></tr></table></td></tr>");
                    i = 0;
                }
                augment = "";
            }
        }
        /****/
        tb.append("</table><br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void showAddAug(L2PcInstance player, int type) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        int pwd = Rnd.get(1100, 9999);
        tb.append("<br><br>&nbsp;&nbsp;Стоимость услуги: " + Config.BBS_AUC_AUG_PRICE + " " + Config.BBS_AUC_AUG_NAME + ".<br><br>");
        tb.append("&nbsp;&nbsp;Шаг 1. Введите желаемую стоимость: <br><table width=300><tr><td><edit var=\"price\" width=70 length=\"16\"></td><td><combobox width=100 var=type list=\"" + MONEY_VARS + "\"></td></tr></table><br>");
        tb.append("&nbsp;&nbsp;Шаг 2. Оплачиваете налог аукциона? (Да: YES; Оплатит покупатель: NO)) <br><table width=300><tr><td></td><td><combobox width=100 var=payer list=\"YES;NO\"></td></tr></table><br>");
        tb.append("&nbsp;&nbsp;Шаг 3. Использовать пароль? Ваш пароль: <font color=LEVEL>" + pwd + "</font> <br><table width=300><tr><td></td><td><combobox width=100 var=pass list=\"NO;YES\"></td></tr></table><br>");
        tb.append("<br>&nbsp;&nbsp;&nbsp;Шаг 4. Выберите предмет, который хотите выставить:<br1><table width=650>");

        int i = 0;
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null && item.getItem().getType2() == L2Item.TYPE2_WEAPON && item.canBeEnchanted() && !item.isEquipped() && item.isDestroyable()) {
                String name = item.getItem().getName();
                String icon = item.getItem().getIcon();
                String ench = " +" + item.getEnchantLevel() + "";
                String augment = getAugmentSkill(item.getAugmentation().getAugmentSkill().getId(), item.getAugmentation().getAugmentSkill().getLevel());
                if (type == 4) {
                    name = augment;
                    icon = "Icon.skill3123";
                    ench = "";
                    augment = "";
                } else {
                    augment = "<br1>" + augment;
                }

                if (i == 0) {
                    tb.append("<tr><td><table width=300><tr><td><img src=\"" + icon + "\" width=32 height=32></td>");
                    tb.append("<td width=270><a action=\"bypass _bbsauc_step2_1_2_3_" + item.getObjectId() + "_" + type + "_ $price _ $type _ $payer _ " + pwd + " _ $pass\">" + name + "</a> <font color=993366> " + ench + "</font> " + augment + "");
                    tb.append("</td></tr></table></td>");
                    i = 1;
                } else {
                    tb.append("<td><table width=300><tr><td><img src=\"" + icon + "\" width=32 height=32></td>");
                    tb.append("<td width=270><a action=\"bypass _bbsauc_step2_1_2_3_" + item.getObjectId() + "_" + type + "_ $price _ $type _ $payer _ " + pwd + " _ $pass\">" + name + "</a> <font color=993366> " + ench + "</font> " + augment + "");
                    tb.append("</td></tr></table></td></tr>");
                    i = 0;
                }
            }
        }
        /****/
        tb.append("</table><br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void showAddCustom(L2PcInstance player, int type) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        int pwd = Rnd.get(1100, 9999);
        tb.append("&nbsp;&nbsp;Шаг 1. Введите желаемую стоимость: <br><table width=300><tr><td><edit var=\"price\" width=70 length=\"16\"></td><td><combobox width=100 var=type list=\"" + MONEY_VARS + "\"></td></tr></table><br>");
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        if (type == 5) {
            tb.append("<br><br>&nbsp;&nbsp;Стоимость услуги: " + Config.BBS_AUC_SKILL_PRICE + " " + Config.BBS_AUC_SKILL_NAME + ".<br><br>");
            tb.append("&nbsp;&nbsp;Шаг 2. Оплачиваете налог аукциона? (Да: YES; Оплатит покупатель: NO)) <br><table width=300><tr><td></td><td><combobox width=100 var=payer list=\"YES;NO\"></td></tr></table><br>");
            tb.append("&nbsp;&nbsp;Шаг 3. Использовать пароль? Ваш пароль: <font color=LEVEL>" + pwd + "</font> <br><table width=300><tr><td></td><td><combobox width=100 var=pass list=\"NO;YES\"></td></tr></table><br>");
            tb.append("<br>&nbsp;&nbsp;&nbsp;<table width=280><tr><td>Шаг 4. Выберите скилл, который хотите выставить: <br></td></tr>");
            SkillTable sst = SkillTable.getInstance();
            try {
                con = L2DatabaseFactory.get();
                st = con.prepareStatement("SELECT skill_id, skill_lvl FROM z_donate_skills WHERE char_id=?");
                st.setInt(1, player.getObjectId());
                rs = st.executeQuery();
				while (rs.next()) {
                    int id = rs.getInt("skill_id");
                    int lvl = rs.getInt("skill_lvl");
                    L2Skill skill = sst.getInfo(id, lvl);
                    tb.append("<tr><td><a action=\"bypass _bbsauc_step2_0_1_2_3_" + id + "_" + type + "_ $price _ $type _ $payer _ " + pwd + " _ $pass\"><font color=bef574>" + skill.getName() + " (" + lvl + " уровень)</font></a><br></td></tr>");
                }
            } catch (SQLException e) {
                System.out.println("[ERROR] AuctionBBSManager, showAddCustom1 " + e);
            } finally {
                Close.CSR(con, st, rs);
            }
            tb.append("</table><br></body></html>");
        } else {
            tb.append("<br><br>&nbsp;&nbsp;Стоимость услуги: " + Config.BBS_AUC_HERO_PRICE + " " + Config.BBS_AUC_HERO_NAME + ".<br><br>");
            tb.append("&nbsp;&nbsp;Шаг 2. Оплачиваете налог аукциона? (Да: YES; Оплатит покупатель: NO)) <br><table width=300><tr><td></td><td><combobox width=100 var=payer list=\"YES;NO\"></td></tr></table><br>");
            tb.append("&nbsp;&nbsp;Шаг 3. Использовать пароль? Ваш пароль: <font color=LEVEL>" + pwd + "</font> <br><table width=300><tr><td></td><td><combobox width=100 var=pass list=\"NO;YES\"></td></tr></table><br>");
            tb.append("<br>&nbsp;&nbsp;&nbsp;Шаг 4. Статус Героя: <br>");
            try {
                con = L2DatabaseFactory.get();
                st = con.prepareStatement("SELECT hero FROM characters WHERE obj_Id=? LIMIT 1");
                st.setInt(1, player.getObjectId());
                rs = st.executeQuery();
                if (rs.next()) {
                    long expire = rs.getLong("hero");
                    if (expire == 1) {
                        tb.append("&nbsp;&nbsp;<a action=\"bypass _bbsauc_step2_0_1_2_3_" + type + "_ $price _ $type _ $payer _ " + pwd + " _ $pass\">Бесконечный</font></a><br>");
                    } else if (System.currentTimeMillis() - expire < 0) {
                        String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(expire));
                        tb.append("&nbsp;&nbsp;<a action=\"bypass _bbsauc_step2_1_2_3_" + type + "_ $price _ $type _ $payer _ " + pwd + " _ $pass\"> Истекает " + date + "</font></a><br>");
                    } else {
                        tb.append("<br><br>&nbsp;Вы не герой.");
                    }
                } else {
                    tb.append("<br><br>&nbsp;Вы не герой.");
                }
            } catch (SQLException e) {
                System.out.println("[ERROR] AuctionBBSManager, showAddCustom2 " + e);
            } finally {
                Close.CSR(con, st, rs);
            }
        }
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void getItemFrom(L2PcInstance player, int id, String tpwd) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_auction` WHERE `id`=? LIMIT 1");
            st.setInt(1, id);
            rs = st.executeQuery();
            if (rs.next()) {
                int item_id = rs.getInt("itemId");
                String name = rs.getString("itemName");
                int aug_id = rs.getInt("augment");
                int aug_lvl = rs.getInt("augLvl");
                int aug_hex = rs.getInt("augAttr");
                int item_ench = rs.getInt("enchant");
                int owner = rs.getInt("ownerId");
                int price = rs.getInt("price");
                int money = rs.getInt("money");
                int type = rs.getInt("type");
                int pay = rs.getInt("pay");
                String pwd = rs.getString("pwd");
                if (owner != player.getObjectId() && !pwd.equals(" ")) {
                    if (!tpwd.equals(pwd)) {
                        tb.append("&nbsp;&nbsp;Неверный пароль.");
                        separateAndSend(tb.toString(), player);
                        return;
                    }
                }

                if (item_id > 0) {
                    L2Item item = ItemTable.getInstance().getTemplate(item_id);
                    if (item == null) {
                        tb.append("&nbsp;&nbsp;Предмет не найден.");
                        separateAndSend(tb.toString(), player);
                        return;
                    } else {
                        if (!transferPay(con, owner, name, item_ench, aug_id, aug_lvl, price, money, player, pay, type)) {
                            tb.append("&nbsp;&nbsp;Проверьте стоимость.");
                            separateAndSend(tb.toString(), player);
                            return;
                        }

                        if (pay == 0 && owner == player.getObjectId()) {
                            int coin_id = Config.BBS_AUC_ITEM_COIN;
                            int coin_price = Config.BBS_AUC_ITEM_PRICE;
                            if (type == 3) {
                                coin_id = Config.BBS_AUC_AUG_COIN;
                                coin_price = Config.BBS_AUC_AUG_PRICE;
                            }
                            if (coin_id > 0) {
                                player.addItem("auc.return", coin_id, coin_price, player, true);
                            }
                        }

                        L2ItemInstance reward = player.getInventory().addItem("auc1", item_id, 1, player, player.getTarget());
                        if (reward == null) {
                            tb.append("&nbsp;&nbsp;не найдено.");
                            separateAndSend(tb.toString(), player);
                            return;
                        }
                        if (item_ench > 0) {
                            reward.setEnchantLevel(item_ench);
                        }
                        if (aug_id > 0) {
                            reward.setAugmentation(new L2Augmentation(reward, aug_hex, aug_id, aug_lvl, true));
                        }

                        SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_ITEM);
                        smsg.addItemName(item_id);
                        player.sendPacket(smsg);
                        player.sendItems(true);
                    }
                } else {
                    tb.append("&nbsp;&nbsp;Произошла ошибка.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                Close.SR(st, rs);
                st = con.prepareStatement("DELETE FROM `z_bbs_auction` WHERE `id`=?");
                st.setInt(1, id);
                st.executeUpdate();
                if (owner == player.getObjectId()) {
                    tb.append("&nbsp;&nbsp;Забрали.");
                } else {
                    tb.append("&nbsp;&nbsp;Предмет куплен.");
                }
            } else {
                tb.append("&nbsp;&nbsp;не найдено.");
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, getItemFrom() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void getAugFrom(L2PcInstance player, int id, String tpwd) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        tb.append("&nbsp;&nbsp;&nbsp;Выберите оружие, в которое хотите вставить аугмент:<br1><table width=370>");
        int i = 0;
        player.setBriefItem(id);
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            int itemType = item.getItem().getType2();
            if (item.canBeEnchanted() && !item.isAugmented() && !item.isEquipped() && item.isDestroyable() && itemType == L2Item.TYPE2_WEAPON) {
                if (i == 0) {
                    tb.append("<tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><a action=\"bypass _bbsauc_menu 5_" + item.getObjectId() + "_" + tpwd + "\"> " + item.getItem().getName() + " + " + item.getEnchantLevel() + "</a></td><td width=30></td>");
                    i = 1;
                } else {
                    tb.append("<td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><a action=\"bypass _bbsauc_menu 5_" + item.getObjectId() + "_" + tpwd + "\"> " + item.getItem().getName() + " + " + item.getEnchantLevel() + "</a></td></tr>");
                    i = 0;
                }
            }
        }
        /****/
        tb.append("</table><br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void getSkillFrom(L2PcInstance player, int id, String tpwd) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_auction` WHERE `id`=? LIMIT 1");
            st.setInt(1, id);
            rs = st.executeQuery();
            if (rs.next()) {
                int item_id = rs.getInt("itemId");
                String name = rs.getString("itemName");
                int item_ench = rs.getInt("enchant");
                int owner = rs.getInt("ownerId");
                int price = rs.getInt("price");
                int money = rs.getInt("money");
                int type = rs.getInt("type");
                int pay = rs.getInt("pay");
                String pwd = rs.getString("pwd");
                if (owner != player.getObjectId() && !pwd.equals(" ")) {
                    if (!tpwd.equals(pwd)) {
                        tb.append("&nbsp;&nbsp;Неверный пароль.");
                        separateAndSend(tb.toString(), player);
                        return;
                    }
                }
                // при добавлении итема бет ошибка 1. nullnull
                L2Skill skill = null;
                if (item_id > 0) {
                    skill = SkillTable.getInstance().getInfo(item_id, item_ench);
                    if (skill == null) {
                        tb.append("&nbsp;&nbsp;Произошла ошибка 1.");
                        separateAndSend(tb.toString(), player);
                        return;
                    } else {
                        if (!transferPay(con, owner, name, item_ench, 0, 0, price, money, player, pay, type)) {
                            tb.append("&nbsp;&nbsp;Проверьте стоимость.");
                            separateAndSend(tb.toString(), player);
                            return;
                        }

                        if (pay == 0 && owner == player.getObjectId()) {
                            if (Config.BBS_AUC_SKILL_COIN > 0) {
                                player.addItem("auc.return", Config.BBS_AUC_SKILL_COIN, Config.BBS_AUC_SKILL_PRICE, player, true);
                            }
                        }
                        Close.SR(st, rs);
                        player.addSkill(skill, false);
                        player.sendSkillList();
                        player.addDonateSkill(0, item_id, item_ench, -1);
                    }
                } else {
                    tb.append("&nbsp;&nbsp;Произошла ошибка 2.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                st = con.prepareStatement("DELETE FROM `z_bbs_auction` WHERE `id`=?");
                st.setInt(1, id);
                st.executeUpdate();
                if (owner == player.getObjectId()) {
                    tb.append("&nbsp;&nbsp;Забрали скилл: <font color=bef574>" + skill.getName() + " (" + item_ench + " уровень)</font>");
                } else {
                    tb.append("&nbsp;&nbsp;Куплен скилл: <font color=bef574>" + skill.getName() + " (" + item_ench + " уровень)</font>.");
                }
            } else {
                tb.append("&nbsp;&nbsp;не найдено.");
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, getItemFrom() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void getHeroFrom(L2PcInstance player, int id, String tpwd) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_auction` WHERE `id`=? LIMIT 1");
            st.setInt(1, id);
            rs = st.executeQuery();
            if (rs.next()) {
                String name = rs.getString("itemName");
                int owner = rs.getInt("ownerId");
                int price = rs.getInt("price");
                int money = rs.getInt("money");
                long hero_expire = rs.getLong("shadow");
                int type = rs.getInt("type");
                int pay = rs.getInt("pay");
                String pwd = rs.getString("pwd");
                if (owner != player.getObjectId() && !pwd.equals(" ")) {
                    if (!tpwd.equals(pwd)) {
                        tb.append("&nbsp;&nbsp;Неверный пароль.");
                        separateAndSend(tb.toString(), player);
                        return;
                    }
                }

                if (hero_expire > 0 && hero_expire != 3) {
                    if (!transferPay(con, owner, name, 0, 0, 0, price, money, player, pay, type)) {
                        tb.append("&nbsp;&nbsp;Проверьте стоимость.");
                        separateAndSend(tb.toString(), player);
                        return;
                    }

                    if (pay == 0 && owner == player.getObjectId()) {
                        if (Config.BBS_AUC_HERO_COIN > 0) {
                            player.addItem("auc.return", Config.BBS_AUC_HERO_COIN, Config.BBS_AUC_HERO_PRICE, player, true);
                        }
                    }
                    Close.SR(st, rs);

                    player.setHero(true);
                    player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
                    player.broadcastUserInfo();

                    st = con.prepareStatement("UPDATE `characters` SET `hero`=? WHERE `obj_Id`=?");
                    st.setLong(1, hero_expire);
                    st.setInt(2, player.getObjectId());
                    st.execute();
                    Close.S(st);
                } else {
                    tb.append("&nbsp;&nbsp;Произошла ошибка.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                st = con.prepareStatement("DELETE FROM `z_bbs_auction` WHERE `id`=?");
                st.setInt(1, id);
                st.executeUpdate();
                if (owner == player.getObjectId()) {
                    tb.append("&nbsp;&nbsp;Вы снова герой, " + name + "</font>");
                } else {
                    tb.append("&nbsp;&nbsp;Куплено геройство: <font color=bef574>" + name + "</font>.");
                }
            } else {
                tb.append("&nbsp;&nbsp;не найдено.");
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, getItemFrom() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void addAugTo(L2PcInstance player, int item_id, String tpwd) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        int id = player.getBriefItem();
        if (id == 0 || id > 1000000) {
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
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_bbs_auction` WHERE `id`=? LIMIT 1");
            st.setInt(1, id);
            rs = st.executeQuery();
            if (rs.next()) {
                String name = rs.getString("itemName");
                int aug_id = rs.getInt("augment");
                int aug_lvl = rs.getInt("augLvl");
                int aug_hex = rs.getInt("augAttr");
                int owner = rs.getInt("ownerId");
                int price = rs.getInt("price");
                int money = rs.getInt("money");
                int type = rs.getInt("type");
                int pay = rs.getInt("pay");
                String pwd = rs.getString("pwd");
                if (owner != player.getObjectId() && !pwd.equals(" ")) {
                    if (!tpwd.equals(pwd)) {
                        tb.append("&nbsp;&nbsp;Неверный пароль.");
                        separateAndSend(tb.toString(), player);
                        return;
                    }
                }

                if (aug_id > 0) {
                    L2Skill aug = SkillTable.getInstance().getInfo(aug_id, aug_lvl);
                    if (aug == null) {
                        tb.append("&nbsp;&nbsp;Предмет не найден.");
                        separateAndSend(tb.toString(), player);
                        return;
                    } else {
                        if (!transferPay(con, owner, name, 0, aug_id, aug_lvl, price, money, player, pay, type)) {
                            tb.append("&nbsp;&nbsp;Проверьте стоимость.");
                            separateAndSend(tb.toString(), player);
                            return;
                        }

                        if (pay == 0 && owner == player.getObjectId()) {
                            if (Config.BBS_AUC_AUG_COIN > 0) {
                                player.addItem("auc.return", Config.BBS_AUC_AUG_COIN, Config.BBS_AUC_AUG_PRICE, player, true);
                            }
                        }
                        item.setAugmentation(new L2Augmentation(item, aug_hex, aug_id, aug_lvl, true));
                        player.sendItems(true);
                        player.sendPacket(new SystemMessage(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
                    }
                } else {
                    tb.append("&nbsp;&nbsp;Предмет не найден.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                Close.SR(st, rs);
                st = con.prepareStatement("DELETE FROM `z_bbs_auction` WHERE `id`=?");
                st.setInt(1, id);
                st.executeUpdate();
                if (owner == player.getObjectId()) {
                    tb.append("&nbsp;&nbsp;Забрали.");
                } else {
                    tb.append("&nbsp;&nbsp;Аугмент куплен.");
                }
            } else {
                tb.append("&nbsp;&nbsp;Письмо не найдено.");
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, addAugTo() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private void addToAuc(L2PcInstance player, int item_obj, int item_type, int item_price, String price_type, int pay_type, String pwd, int use_pwd) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getMenu());
        /****/
        if (use_pwd == 1 && pwd.length() < 3) {
            tb.append("&nbsp;&nbsp;Пароль должен быть более 3 символов.");
            separateAndSend(tb.toString(), player);
            return;
        }
        if (item_price <= 0) {
            tb.append("&nbsp;&nbsp;Ошибка запроса.");
            separateAndSend(tb.toString(), player);
            return;
        }

        int coin_id = 0;
        int coin_price = 0;
        String coin_name = "";
        if (pay_type == 0) {
            switch (item_type) {
                case 0:
                case 1:
                case 2:
                    coin_id = Config.BBS_AUC_ITEM_COIN;
                    coin_price = Config.BBS_AUC_ITEM_PRICE;
                    coin_name = Config.BBS_AUC_ITEM_NAME;
                    break;
                case 3:
                case 4:
                    coin_id = Config.BBS_AUC_AUG_COIN;
                    coin_price = Config.BBS_AUC_AUG_PRICE;
                    coin_name = Config.BBS_AUC_AUG_NAME;
                    break;
                case 5:
                    coin_id = Config.BBS_AUC_SKILL_COIN;
                    coin_price = Config.BBS_AUC_SKILL_PRICE;
                    coin_name = Config.BBS_AUC_SKILL_NAME;
                    break;
                case 6:
                    coin_id = Config.BBS_AUC_HERO_COIN;
                    coin_price = Config.BBS_AUC_HERO_PRICE;
                    coin_name = Config.BBS_AUC_HERO_NAME;
                    break;
            }
            if (coin_id > 0) {
                L2ItemInstance coin = player.getInventory().getItemByItemId(coin_id);
                if (coin == null || coin.getCount() < coin_price) {
                    tb.append("<br><br>&nbsp;&nbsp;Стоимость услуги: " + coin_price + " " + coin_name + ".");
                    separateAndSend(tb.toString(), player);
                    return;
                }
            }
        }

        L2ItemInstance item = null;
        if (item_type >= 0 && item_type <= 4) {
            item = player.getInventory().getItemByObjectId(item_obj);
            if (item == null || item.isEquipped()) {
                tb.append("&nbsp;&nbsp;Произошла ошибка 1.");
                separateAndSend(tb.toString(), player);
                return;
            }
            if (item.getOwnerId() != player.getObjectId()) {
                tb.append("&nbsp;&nbsp;Произошла ошибка 6.");
                separateAndSend(tb.toString(), player);
                return;
            }
        }

        int money = getMoneyId(price_type);
        if (money == 0) {
            tb.append("&nbsp;&nbsp;Произошла ошибка 2.");
            separateAndSend(tb.toString(), player);
            return;
        }

        int item_id = 0;
        int item_ench = 0;
        int item_type2 = 0;
        String item_name = "";

        int aug_id = 0;
        int aug_lvl = 0;
        int aug_hex = 0;

        long hero_expire = 0;

        switch (item_type) {
            case 0:
            case 1:
            case 2:
                item_ench = item.getEnchantLevel();
                if (item_ench == 0) {
                    tb.append("&nbsp;&nbsp;Произошла ошибка 3.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                item_id = item.getItemId();
                item_name = item.getItem().getName();
                if (item.isAugmented()) {
                    aug_hex = item.getAugmentation().getAugmentationId();
                    if (item.getAugmentation().getAugmentSkill() != null) {
                        L2Skill augment = item.getAugmentation().getAugmentSkill();
                        aug_id = augment.getId();
                        aug_lvl = augment.getLevel();
                    }
                }
                break;
            case 3:
            case 4:
                if (!item.isAugmented() || item.getAugmentation().getAugmentSkill() == null) {
                    tb.append("&nbsp;&nbsp;Произошла ошибка 4.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                L2Skill augment = item.getAugmentation().getAugmentSkill();
                aug_id = augment.getId();
                aug_lvl = augment.getLevel();
                aug_hex = item.getAugmentation().getAugmentationId();
                item_id = 0;
                item_ench = 0;
                if (item_type == 4) {
                    item_name = augment.getName();
                } else {
                    item_id = item.getItemId();
                    item_ench = item.getEnchantLevel();
                    item_name = item.getItem().getName();
                }
                break;
            case 5:
                if (player.getKnownSkill(item_obj) == null) {
                    tb.append("&nbsp;&nbsp;Произошла ошибка 7.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                Connect con = null;
                PreparedStatement st = null;
                ResultSet rs = null;
                try {
                    con = L2DatabaseFactory.get();
                    st = con.prepareStatement("SELECT skill_lvl FROM z_donate_skills WHERE char_id=? AND skill_id=?");
                    st.setInt(1, player.getObjectId());
                    st.setInt(2, item_obj);
                    rs = st.executeQuery();
                    if (rs.next()) {
                        item_id = item_obj;
                        item_ench = rs.getInt("skill_lvl");
                        L2Skill skill = SkillTable.getInstance().getInfo(item_id, item_ench);
                        if (skill == null) {
                            tb.append("&nbsp;&nbsp;Произошла ошибка 10.");
                            separateAndSend(tb.toString(), player);
                            return;
                        }
                        item_name = skill.getName();
                    } else {
                        tb.append("&nbsp;&nbsp;Произошла ошибка 8.");
                        separateAndSend(tb.toString(), player);
                        return;
                    }
                } catch (SQLException e) {
                    System.out.println("[ERROR] AuctionBBSManager, addToAuc() error: " + e);
                } finally {
                    Close.CSR(con, st, rs);
                }
                break;
            case 6:
                if (!(player.isHero())) {
                    tb.append("&nbsp;&nbsp;Произошла ошибка 8.");
                    separateAndSend(tb.toString(), player);
                    return;
                }
                Connect con2 = null;
                PreparedStatement st2 = null;
                ResultSet rs2 = null;
                try {
                    con2 = L2DatabaseFactory.get();
                    st2 = con2.prepareStatement("SELECT hero FROM characters WHERE obj_Id=? LIMIT 1");
                    st2.setInt(1, player.getObjectId());
                    rs2 = st2.executeQuery();
                    if (rs2.next()) {
                        hero_expire = rs2.getLong("hero");
                        if (hero_expire == 1) {
                            item_name = "Hero: Вечное";
                        } else {
                            item_name = "Hero: " + (new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(hero_expire))) + "";
                        }
                    } else {
                        tb.append("&nbsp;&nbsp;Произошла ошибка 9.");
                        separateAndSend(tb.toString(), player);
                        return;
                    }
                } catch (SQLException e) {
                    System.out.println("[ERROR] AuctionBBSManager, addToAuc() error: " + e);
                } finally {
                    Close.CSR(con2, st2, rs2);
                }
                break;
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            switch (item_type) {
                case 5:
                    st = con.prepareStatement("DELETE FROM z_donate_skills WHERE char_id=? AND skill_id=?");
                    st.setInt(1, player.getObjectId());
                    st.setInt(2, item_obj);
                    st.execute();
                    Close.S(st);
                    break;
                case 6:
                    st = con.prepareStatement("UPDATE `characters` SET `hero`=? WHERE `obj_Id`=?");
                    st.setInt(1, 0);
                    st.setInt(2, player.getObjectId());
                    st.execute();
                    Close.S(st);
                    break;
            }
            st = con.prepareStatement("INSERT INTO `z_bbs_auction` (`id`,`itemId`,`itemName`,`enchant`,`augment`,`augAttr`,`augLvl`,`price`,`money`,`type`,`ownerId`,`shadow`,`pay`,`pwd`,`expire`) VALUES (NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
            st.setInt(1, item_id);
            st.setString(2, item_name);
            st.setInt(3, item_ench);
            st.setInt(4, aug_id);
            st.setInt(5, aug_hex);
            st.setInt(6, aug_lvl);
            st.setInt(7, item_price);
            st.setInt(8, money);
            st.setInt(9, item_type);
            st.setInt(10, player.getObjectId());
            st.setLong(11, hero_expire);
            st.setInt(12, pay_type);
            st.setString(13, (use_pwd == 0 ? " " : pwd));
            st.setLong(14, (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(Config.BBS_AUC_EXPIRE_DAYS)));
            st.executeUpdate();
        } catch (final SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, addToAuc() error: " + e);
            tb.append("&nbsp;&nbsp;Произошла ошибка 5.");
            separateAndSend(tb.toString(), player);
            return;
        } finally {
            Close.CS(con, st);
        }

        if (pay_type == 0) {
            player.destroyItemByItemId("auc", coin_id, coin_price, player, true);
        }

        switch (item_type) {
            case 0:
            case 1:
            case 2:
            case 3:
                player.destroyItem("addToAuc", item, player, true);
                break;
            case 4:
                item.removeAugmentation();
                player.sendItems(false);
                break;
            case 5:
                player.removeSkill(player.getKnownSkill(item_obj));
                player.sendSkillList();
                break;
            case 6:
                player.setHero(false);
                player.setHeroExpire(0);
                player.broadcastUserInfo();
                break;
        }
        tb.append("<br>&nbsp;&nbsp;Лот: " + item_name + " " + (item_ench == 0 ? "" : "+" + item_ench + "") + " " + (item_id != 0 ? getAugmentSkill(aug_id, aug_lvl) : "") + " выставлен на аукцион.");
        if (use_pwd == 1) {
            tb.append("<br>&nbsp;&nbsp;Ваш пароль: <font color=LEVEL>" + pwd + "</font>.");
        }
        tb.append("<br>&nbsp;&nbsp;<font color=" + CPRICE + ">Стоимость: " + Util.formatAdena(item_price) + " " + getMoneyCall(money) + "</font><br>");
        /****/
        tb.append("<br></body></html>");
        separateAndSend(tb.toString(), player);
    }

    private boolean transferPay(Connect con, int charId, String name, int enchant, int augId, int augLvl, int price, int money, L2PcInstance player, int pay, int type) {
        if (charId == player.getObjectId()) {
            return true;
        }
        
        if (price <= 0)
        {
            return false;
        }

        if (pay == 1) {
            int coin_id = 0;
            int coin_price = 0;
            switch (type) {
                case 0:
                case 1:
                case 2:
                    coin_id = Config.BBS_AUC_ITEM_COIN;
                    coin_price = Config.BBS_AUC_ITEM_PRICE;
                    break;
                case 3:
                case 4:
                    coin_id = Config.BBS_AUC_AUG_COIN;
                    coin_price = Config.BBS_AUC_AUG_PRICE;
                    break;
                case 5:
                    coin_id = Config.BBS_AUC_SKILL_COIN;
                    coin_price = Config.BBS_AUC_SKILL_PRICE;
                    break;
                case 6:
                    coin_id = Config.BBS_AUC_HERO_COIN;
                    coin_price = Config.BBS_AUC_HERO_PRICE;
                    break;
            }
            if (coin_id > 0) {
                if (coin_id == money) {
                    coin_price += price;

                    L2ItemInstance coin = player.getInventory().getItemByItemId(coin_id);
                    if (coin == null || coin.getCount() < coin_price) {
                        return false;
                    }
                    player.destroyItemByItemId("auc1", coin_id, coin_price, player, true);
                } else {
                    L2ItemInstance coin = player.getInventory().getItemByItemId(coin_id);
                    if (coin == null || coin.getCount() < coin_price) {
                        return false;
                    }

                    L2ItemInstance coin2 = player.getInventory().getItemByItemId(money);
                    if (coin2 == null || coin2.getCount() < price) {
                        return false;
                    }

                    player.destroyItemByItemId("auc1", money, price, player, true);
                    player.destroyItemByItemId("auc1", coin_id, coin_price, player, true);
                }
            } else {
                L2ItemInstance coin = player.getInventory().getItemByItemId(money);
                if (coin == null || coin.getCount() < price) {
                    return false;
                }
                player.destroyItemByItemId("auc1", money, price, player, true);
            }
        } else {
            L2ItemInstance coin = player.getInventory().getItemByItemId(money);
            if (coin == null || coin.getCount() < price) {
                return false;
            }
            player.destroyItemByItemId("auc1", money, price, player, true);
        }

        TextBuilder text = new TextBuilder();
        text.append("" + name + "<br1> был успешно продан.<br>");
        text.append("Благодарим за сотрудничество.");
        PreparedStatement st = null;
        try {
            st = con.prepareStatement("INSERT INTO `z_bbs_mail` (`id`,`from`,`to`,`tema`,`text`,`datetime`,`read`,`item_id`,`item_count`,`item_ench`,`aug_hex`,`aug_id`,`aug_lvl`) VALUES (NULL,?,?,?,?,?,?,?,?,?,?,?,?)");
            st.setInt(1, 777);
            st.setInt(2, charId);
            st.setString(3, "Ваш лот продан");
            st.setString(4, text.toString());
            st.setLong(5, System.currentTimeMillis());
            st.setInt(6, 0);
            st.setInt(7, money);
            st.setInt(8, price);
            st.setInt(9, 0);
            st.setInt(10, 0);
            st.setInt(11, 0);
            st.setInt(12, 0);
            st.execute();
            L2PcInstance trg = L2World.getInstance().getPlayer(getSellerName(con, charId));
            if (trg != null) {
                trg.sendPacket(new ExMailArrived());
                trg.sendMessage("Уведомление с аукциона: проверьте почту.");
            }
            return true;
        } catch (final SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, transferPay() error: " + e);
        } finally {
            text.clear();
            Close.S(st);
        }
        return false;
    }

    private String getMoneyCall(int money) {
        return moneys.get(money);
    }

    private int getMoneyId(String price_type) {
        for (FastMap.Entry<Integer, String> e = moneys.head(), end = moneys.tail(); (e = e.getNext()) != end;) {
            Integer key = e.getKey();
            String value = e.getValue();
            if (key == null) {
                continue;
            }

            if (value.trim().equals(price_type)) {
                return key;
            }
        }
        return 0;
    }

    private void cacheMoneys() {
        TextBuilder text = new TextBuilder();
        for (FastMap.Entry<Integer, String> e = moneys.head(), end = moneys.tail(); (e = e.getNext()) != end;) {
            String value = e.getValue();
            text.append(value + ";");
        }
        MONEY_VARS = text.toString();
        text.clear();
    }

    private void cacheMenu() {
        TextBuilder tb = new TextBuilder();
        tb.append(getPwHtm("menu"));
        tb.append("<html><body><table width=280><tr><td align=right><button value=\"Главная\" action=\"bypass _bbsauc\" width=70 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td align=right><button value=\"Личный кабинет\" action=\"bypass _bbsauc_office\" width=86 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td align=right><button value=\"Добавить\" action=\"bypass _bbsauc_add\" width=70 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td align=right><button value=\"Поиск\" action=\"bypass _bbsauc_fsearch\" width=40 height=17 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table><br>");
        AUC_MENU = tb.toString();
        tb.clear();
    }

    private int getPageCount(Connect con, int me, int itemId, int augment, int type2, int charId) {
        int rowCount = 0;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            if (type2 >= 0) {
                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_bbs_auction` WHERE `type` = ?");
                st.setInt(1, type2);
            } else if (itemId > 0) {
                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_bbs_auction` WHERE `itemId` = ?");
                st.setInt(1, itemId);
            } else if (augment > 0) {
                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_bbs_auction` WHERE `augment` = ?");
                st.setInt(1, augment);
            } else if (me == 1) {
                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_bbs_auction` WHERE `ownerId` = ?");
                st.setInt(1, charId);
            } else {
                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_bbs_auction` WHERE `id` > 0");
            }

            rs = st.executeQuery();
            if (rs.next()) {
                rowCount = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, getPageCount() error: " + e);
        } finally {
            Close.SR(st, rs);
        }
        if (rowCount == 0) {
            return 0;
        }

        return (rowCount / PAGE_LIMIT) + 1;
    }

    private String sortPages(int page, int pages, int me, int itemId, int augment, int type2) {
        TextBuilder text = new TextBuilder("<br>Страницы:<br1><table width=300><tr>");
        int step = 1;
        int s = page - 3;
        int f = page + 3;
        if (page < SORT_LIMIT && s < SORT_LIMIT) {
            s = 1;
        }
        if (page >= SORT_LIMIT) {
            text.append("<td><a action=\"bypass _bbsauc_pageshow_" + s + "_" + me + "_" + itemId + "_" + augment + "_" + type2 + "\"> ... </a></td>");
        }
        for (int i = s; i <= pages; i++) {
            int al = i + 1;
            if (i == page) {
                text.append("<td>" + i + "</td>");
            } else {
                if (al <= pages) {
                    text.append("<td><a action=\"bypass _bbsauc_pageshow_" + i + "_" + me + "_" + itemId + "_" + augment + "_" + type2 + "\">" + i + "</a></td>");
                }
            }
            if (step == SORT_LIMIT && f < pages) {
                if (al < pages) {
                    text.append("<td><a action=\"bypass _bbsauc_pageshow_" + al + "_" + me + "_" + itemId + "_" + augment + "_" + type2 + "\"> ... </a></td>");
                }
                break;
            }
            step++;
        }
        text.append("</tr></table><br>");
        return text.toString();
    }

    private String getAugmentSkill(int skillId, int skillLvl) {
        L2Skill augment = SkillTable.getInstance().getInfo(skillId, 1);
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

        return "<font color=336699>Аугмент:</font> <font color=bef574>" + augName + " (" + type + ":" + skillLvl + "lvl)</font>";
    }

    private String getSellerName(Connect con, int objId) {
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            st = con.prepareStatement("SELECT char_name FROM `characters` WHERE `obj_Id` = ? LIMIT 0,1");
            st.setInt(1, objId);
            rset = st.executeQuery();
            if (rset.next()) {
                return rset.getString("char_name");
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] AuctionBBSManager, getCharName() error: " + e);
        } finally {
            Close.SR(st, rset);
        }
        return "???";
    }

    private static String getMenu() {
        return AUC_MENU;
    }

    private void returnExpiredLots() {
        Connect con = null;
        PreparedStatement st = null;
        PreparedStatement st2 = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("SELECT id, itemId, itemName, enchant, augment, augAttr, augLvl, price, money, type, ownerId, shadow, pay, pwd, expire FROM `z_bbs_auction` WHERE `expire` < ?");
            st.setLong(1, System.currentTimeMillis());
            rs = st.executeQuery();
            while (rs.next()) {
                int type = rs.getInt("type");
                if (type > 3) {
                    continue;
                }
                int id = rs.getInt("id");
                int itmId = rs.getInt("itemId");
                String name = rs.getString("itemName");
                int ownerId = rs.getInt("ownerId");
                int augAttr = rs.getInt("augAttr");
                int augId = rs.getInt("augment");
                int augLvl = rs.getInt("augLvl");
                int enchant = rs.getInt("enchant");
                int pay = rs.getInt("pay");

                st2 = con.prepareStatement("DELETE FROM `z_bbs_auction` WHERE `id`=?");
                st2.setInt(1, id);
                st2.execute();
                Close.S(st2);

                st2 = con.prepareStatement("INSERT INTO `z_bbs_mail` (`from`, `to`, `tema`, `text`, `datetime`, `read`, `item_id`, `item_count`, `item_ench`, `aug_hex`, `aug_id`, `aug_lvl`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                st2.setInt(1, 777);
                st2.setInt(2, ownerId);
                st2.setString(3, "Возврат");
                st2.setString(4, "Итем возвращен.");
                st2.setLong(5, System.currentTimeMillis());
                st2.setInt(6, 0);
                st2.setInt(7, itmId);
                st2.setInt(8, 1);
                st2.setInt(9, enchant);
                st2.setInt(10, augAttr);
                st2.setInt(11, augId);
                st2.setInt(12, augLvl);
                st2.execute();
                Close.S(st2);

                if (pay == 0) {
                    int coin_id = Config.BBS_AUC_ITEM_COIN;
                    int coin_price = Config.BBS_AUC_ITEM_PRICE;
                    if (type == 3) {
                        coin_id = Config.BBS_AUC_AUG_COIN;
                        coin_price = Config.BBS_AUC_AUG_PRICE;
                    }
                    if (coin_id > 0) {
                        st2 = con.prepareStatement("INSERT INTO `z_bbs_mail` (`from`, `to`, `tema`, `text`, `datetime`, `read`, `item_id`, `item_count`, `item_ench`, `aug_hex`, `aug_id`, `aug_lvl`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        st2.setInt(1, 777);
                        st2.setInt(2, ownerId);
                        st2.setString(3, "Возврат");
                        st2.setString(4, "Итем возвращен.");
                        st2.setLong(5, System.currentTimeMillis());
                        st2.setInt(6, 0);
                        st2.setInt(7, coin_id);
                        st2.setInt(8, coin_price);
                        st2.setInt(9, 0);
                        st2.setInt(10, 0);
                        st2.setInt(11, 0);
                        st2.setInt(12, 0);
                        st2.execute();
                        Close.S(st2);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] AuctionBBSManager, showSellItems() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
    }

    @Override
    public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance player) {
        // TODO Auto-generated method stub
    }

    /*
    CREATE TABLE `z_bbs_auction` (
    `id` bigint(9) NOT NULL AUTO_INCREMENT,
    `itemId` smallint(5) unsigned NOT NULL DEFAULT '0',
    `itemName` varchar(128) NOT NULL DEFAULT ' ',
    `enchant` smallint(5) unsigned NOT NULL DEFAULT '0',
    `augment` int(11) DEFAULT '0',
    `augAttr` int(11) DEFAULT '0',
    `augLvl` int(11) DEFAULT '0',
    `price` int(11) DEFAULT '0',
    `money` int(11) DEFAULT '0',
    `type` tinyint(1) DEFAULT '0',
    `ownerId` int(10) unsigned NOT NULL DEFAULT '0',
    `shadow` bigint(20) NOT NULL DEFAULT '0',
    `pay` smallint(2) NOT NULL DEFAULT '0',
    `pwd` varchar(16) DEFAULT NULL,
    `expire` bigint(20) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`)
    ) ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
     */
}