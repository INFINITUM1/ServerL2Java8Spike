package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import javolution.util.FastTable;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.datatables.CustomServerData.ChinaItem;
import net.sf.l2j.gameserver.datatables.CustomServerData.DonateItem;
import net.sf.l2j.gameserver.datatables.CustomServerData.DonateSkill;
import net.sf.l2j.gameserver.datatables.CustomServerData.StatCastle;
import net.sf.l2j.gameserver.datatables.CustomServerData.StatClan;
import net.sf.l2j.gameserver.datatables.CustomServerData.StatPlayer;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.SystemMessageId;
//import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExMailArrived;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;

public class L2DonateInstance extends L2NpcInstance {

    public static final Logger _log = AbstractLogger.getLogger(L2DonateInstance.class.getName());
    private static final int STOCK_SERTIFY = Config.STOCK_SERTIFY; // ID итема - сертификат, позволяющий барыжить на бирже
    private static final int SERTIFY_PRICE = Config.SERTIFY_PRICE; // стоимость сертификата
    private static final int FIRST_BALANCE = Config.FIRST_BALANCE; // счет на бирже; при покупке сертификата и создании аккаунта
    private static final int DONATE_COIN = Config.DONATE_COIN; // ID итема - донат монетки, пополняющие счет биржи
    private static final String DONATE_COIN_NEMA = Config.DONATE_COIN_NEMA; // Название донат монетки
    private static final int DONATE_RATE = Config.DONATE_RATE; // 1 DONATE_COIN = (X) DONATE_RATE на баланс биржи
    private static final int NALOG_NPS = (100 - Config.NALOG_NPS); // налог с продажи шмотки
    private static final String VAL_NAME = Config.VAL_NAME;
    private static final int PAGE_LIMIT = Config.PAGE_LIMIT; // показ шмоток с биржи, лимит на страницу, ?Html file too long
    // перенос заточки и аугментации 
    private static final int AUGMENT_COIN = Config.AUGMENT_COIN; // ID итема, за перенос аугментации
    private static final int ENCHANT_COIN = Config.ENCHANT_COIN; // ID итема, за перенос заточки	
    private static final String AUGMENT_COIN_NAME = Config.AUGMENT_COIN_NAME; // название итема, за перенос аугментации
    private static final String ENCHANT_COIN_NAME = Config.ENCHANT_COIN_NAME; // название итема, за перенос заточки
    private static final int AUGMENT_PRICE = Config.AUGMENT_PRICE; // плата за перенос аугментации
    private static final int ENCHANT_PRICE = Config.ENCHANT_PRICE;//  плата за перенос заточки
    //клан
    private static final int CLAN_COIN = Config.CLAN_COIN;
    private static final String CLAN_COIN_NAME = Config.CLAN_COIN_NAME; // название итема, за перенос аугментации
    private static final int CLAN_LVL6 = Config.CLAN_LVL6;
    private static final int CLAN_LVL7 = Config.CLAN_LVL7;
    private static final int CLAN_LVL8 = Config.CLAN_LVL8;
    private static final int CLAN_POINTS = Config.CLAN_POINTS;
    private static final int CLAN_POINTS_PRICE = Config.CLAN_POINTS_PRICE;
    //аугментация
    private static final int AUGSALE_COIN = Config.AUGSALE_COIN; // ID итема
    private static final int AUGSALE_PRICE = Config.AUGSALE_PRICE; // стоимость
    private static final String AUGSALE_COIN_NAME = Config.AUGSALE_COIN_NAME; // название итема
    private static final FastMap<Integer, Integer> AUGSALE_TABLE = Config.AUGSALE_TABLE;
    private static final FastMap<Integer, Integer> PREMIUM_DAY_PRICES = Config.PREMIUM_DAY_PRICES;
    //skill of balance
    private static final int SOB_COIN = Config.SOB_COIN; // ID итема
    private static final int SOB_PRICE_ONE = Config.SOB_PRICE_ONE; // стоимость
    private static final int SOB_PRICE_TWO = Config.SOB_PRICE_TWO; // стоимость
    private static final String SOB_COIN_NAME = Config.SOB_COIN_NAME; // название итема
    // донат шоп
    //private static final int DSHOP_COIN = Config.DSHOP_COIN; // ID итема - донат монетки
    //private static final String DSHOP_COIN_NEMA = Config.DSHOP_COIN_NEMA; // Название донат монетки
    //private static final FastTable<DonateItem> DONATE_ITEMS = CustomServerData.DONATE_ITEMS;
    //кеш донат скиллов
    public static FastMap<Integer, FastTable<DonateSkill>> _donateSkills = CustomServerData._donateSkills;

    public L2DonateInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        //System.out.println("String: " + command);

        if (command.startsWith("StockExchange")) {
            int val = Integer.parseInt(command.substring(13).trim());

            switch (val) {
                case 1: // приветствие
                    L2ItemInstance setify = player.getInventory().getItemByItemId(STOCK_SERTIFY);
                    if (setify != null && setify.getCount() >= 1) {
                        showWelcome(player, 1);
                    } else {
                        showWelcome(player, 0);
                    }
                    break;
                case 2: // список товаров на бирже
                    showStockSellList(player, 1, 0);
                    break;
                case 3: // выставление на биржу своих шмоток
                    showInventoryItems(player);
                    break;
                case 4: // личный кабинет
                    showPrivateInfo(player);
                    break;
                case 5: // покупка сертификата
                    showSertifyInfo(player);
                    break;
                default:
                    showError(player, "Ошибка запроса.");
                    break;
            }
        } else if (command.startsWith("StockShowItem")) // покупка на бирже, подробная инфа о шмотке на бирже
        {
            int sellId = Integer.parseInt(command.substring(13).trim());
            if (sellId == 0) {
                showError(player, "Ошибка запроса.");
                return;
            }

            showStockItem(player, sellId);
        } else if (command.startsWith("StockInventoryItem")) // выставление на биржу, подробная инфа о шмотке в инвентаре
        {
            int objectId = Integer.parseInt(command.substring(18).trim());
            if (objectId == 0) {
                showError(player, "Ошибка запроса.");
                return;
            }

            showInventoryItem(player, objectId);
        } else if (command.startsWith("StockBuyItem")) // finish покупка с биржи
        {
            int sellId = Integer.parseInt(command.substring(12).trim());
            if (sellId == 0) {
                showError(player, "Ошибка запроса.");
                return;
            }
            buyStockItem(player, sellId);
        } else if (command.startsWith("StockAddItem")) // finish добавление шмотки на биржу
        {
            try {
                String[] opaopa = command.split(" ");
                int objectId = Integer.parseInt(opaopa[1]);
                int price = Integer.parseInt(opaopa[2]);
                if (objectId == 0 || price == 0) {
                    showError(player, "Ошибка запроса.");
                    return;
                }
                addStockItem(player, objectId, price);
            } catch (Exception e) //catch (ArrayIndexOutOfBoundsException e)
            {
                showError(player, "Ошибка запроса.");
                return;
            }
        } else if (command.startsWith("showPrivateInfo")) // обработчик личного кабинета
        {
            int val = Integer.parseInt(command.substring(15).trim());

            switch (val) {
                case 1: // Обменять донат монеты в валюту биржи
                    incBalance(player);
                    break;
                case 2: // список своих шмоток на бирже, с возможностью забрать
                    showStockSellList(player, 1, 1);
                    break;
                case 14:
                    //showLogs(player);
                    break;
                default:
                    showError(player, "Ошибка запроса.");
                    break;
            }
        } else if (command.startsWith("StockAddBalance")) // обмен монет в валюту биржи
        {
            try {
                int val = Integer.parseInt(command.substring(15).trim());
                StockAddBalance(player, val);
            } catch (Exception e) //catch (NumberFormatException e)
            {
                showError(player, "Ошибка запроса.");
                return;
            }
        } else if (command.startsWith("StockShowPage")) // постраничность
        {
            try {
                String[] opaopa = command.split(" ");
                int page = Integer.parseInt(opaopa[1]);
                int self = Integer.parseInt(opaopa[2]);
                if (page == 0) {
                    showError(player, "Ошибка запроса.");
                    return;
                }
                showStockSellList(player, page, self);
            } catch (Exception e) //catch (ArrayIndexOutOfBoundsException e)
            {
                showError(player, "Ошибка запроса.");
                return;
            }
        } else if (command.equalsIgnoreCase("StockSertifyBuy")) {
            StockSertifyBuy(player);
        } else if (command.equalsIgnoreCase("voteHome")) {
            showChatWindow(player, "data/html/default/80007.htm");
        } else if (command.startsWith("voteService")) {
            String[] opaopa = command.split(" ");
            int service = Integer.parseInt(opaopa[1]);
            int page = Integer.parseInt(opaopa[2]);
            if (service > 2) {
                return;
            }

            showVoteServiceWindow(player, service, page);
        } else if (command.startsWith("voteItemShow_")) {
            String[] opaopa = command.split("_");
            int service = Integer.parseInt(opaopa[1]);
            int objectId = Integer.parseInt(opaopa[2]);

            if (service > 2) {
                return;
            }

            showVote1Item(player, service, objectId);
        } else if (command.startsWith("voteItem2Show_")) {
            String[] opaopa = command.split("_");
            int service = Integer.parseInt(opaopa[1]);
            int objectId = Integer.parseInt(opaopa[2]);

            if (service > 2) {
                return;
            }

            showVote2Item(player, service, objectId);
        } else if (command.startsWith("voteStep2")) {
            String[] opaopa = command.split(" ");
            int service = Integer.parseInt(opaopa[1]);
            int page = Integer.parseInt(opaopa[2]);
            if (service > 2) {
                return;
            }

            showVoteNextItems(player, service, page);
        } else if (command.startsWith("voteStep3_")) {
            int service = Integer.parseInt(command.substring(10).trim());

            if (service > 2) {
                return;
            }

            showVoteAgree(player, service);
        } else if (command.startsWith("voteComplete_")) {
            int service = Integer.parseInt(command.substring(13).trim());

            if (service > 2) {
                return;
            }

            showDoVoteFinish(player, service);
        } else if (command.startsWith("clanService_")) {
            int service = Integer.parseInt(command.substring(12).trim());

            if (service > 2) {
                return;
            }

            if (!player.isClanLeader()) {
                player.sendPacket(Static.WAR_NOT_LEADER);
                return;
            }

            if (player.getClan().getLevel() < 5) {
                player.sendPacket(Static.CLAN_5LVL_HIGHER);
                return;
            }

            clanWelcome(player);
        } else if (command.startsWith("clanLevel_")) {
            int level = Integer.parseInt(command.substring(10).trim());

            clanSetLevel(player, level);
        } else if (command.equalsIgnoreCase("clanPoints")) {
            clanPoints(player);
        } else if (command.equalsIgnoreCase("clanSkills")) {
            clanSkills(player);
        } else if (command.equalsIgnoreCase("Augsale")) {
            AugSaleWelcome(player);
        } else if (command.equalsIgnoreCase("augSaleItems")) {
            AugSaleItems(player);
        } else if (command.equalsIgnoreCase("AugsaleFinish")) {
            AugsaleFinish(player);
        } else if (command.startsWith("augsaleShow")) {
            int augId = Integer.parseInt(command.substring(11).trim());

            augSaleShow(player, augId);
        } else if (command.startsWith("augsItem")) {
            int objectId = Integer.parseInt(command.substring(8).trim());
            if (objectId == 0) {
                showError(player, "Ошибка запроса.");
                return;
            }

            AugsItem(player, objectId);
        } else if (command.equalsIgnoreCase("chinaItems")) {
            itemsChina(player);
        } else if (command.startsWith("chinaShow")) {
            int itId = Integer.parseInt(command.substring(9).trim());
            if (itId == 0) {
                showError(player, "Ошибка запроса.");
                return;
            }

            chinaShow(player, itId);
        } else if (command.startsWith("bueChina")) {
            int itId = Integer.parseInt(command.substring(8).trim());
            if (itId == 0) {
                showError(player, "Ошибка запроса.");
                return;
            }

            bueChina(player, itId);
        } else if (command.startsWith("bueSOB")) {
            if (Config.SOB_ID == 0) {
                showError(player, "Ошибка запроса.");
                return;
            }
            int itId = Integer.parseInt(command.substring(6).trim());
            if (itId == 0) {
                showError(player, "Ошибка запроса.");
                return;
            }

            bueSOB(player, itId);
        } else if (command.equalsIgnoreCase("donateShop")) {
            if (!Config.ALLOW_DSHOP) {
                showError(player, "Магазин отключен.");
                return;
            }

            donateShop(player);
        } else if (command.startsWith("dShopShow")) {
            if (!Config.ALLOW_DSHOP) {
                showError(player, "Магазин отключен.");
                return;
            }

            donateShopShow(player, Integer.parseInt(command.substring(9).trim()));
        } else if (command.startsWith("dShopBue")) {
            if (!Config.ALLOW_DSHOP) {
                showError(player, "Магазин отключен.");
                return;
            }

            donateShopBue(player, Integer.parseInt(command.substring(8).trim()));
        } else if (command.startsWith("addPremium")) {
            if (!Config.PREMIUM_ENABLE) {
                showError(player, "Премиум отключен.");
                return;
            }

            addPremium(player, Integer.parseInt(command.substring(10).trim()));
        } else if (command.equalsIgnoreCase("addNoble")) {
            if (!Config.NOBLES_ENABLE) {
                showError(player, "Сервис выдачи ноблесса отключен.");
                return;
            }

            addNoble(player);
        } else if (command.equalsIgnoreCase("donateSkillsShop")) {
            if (!Config.ALLOW_DSKILLS) {
                showError(player, "Магазин отключен.");
                return;
            }
            donateSkillShop(player);
        } else if (command.startsWith("dsShopShow")) {
            if (!Config.ALLOW_DSKILLS) {
                showError(player, "Магазин отключен.");
                return;
            }

            donateSkillShopShow(player, Integer.parseInt(command.substring(10).trim()));
        } else if (command.startsWith("dsShopBue")) {
            if (!Config.ALLOW_DSKILLS) {
                showError(player, "Магазин отключен.");
                return;
            }

            donateSkillShopBue(player, Integer.parseInt(command.substring(9).trim()));
        } else if (command.equalsIgnoreCase("statHome")) {
            if (!Config.CACHED_SERVER_STAT) {
                showError(player, "Статистика отключена.");
                return;
            }

            statHome(player);
        } else if (command.startsWith("bueUniqSkill")) {
            if (!Config.ALLOW_UNIQ_SKILLS) {
                showError(player, "Магазин отключен.");
                return;
            }

            uniqSkillShopBue(player, Integer.parseInt(command.substring(12).trim()));
        } else if (command.startsWith("statPvp")) {
            statShowPvp(player, Integer.parseInt(command.substring(7).trim()));
        } else if (command.startsWith("statPk")) {
            statShowPk(player, Integer.parseInt(command.substring(6).trim()));
        } else if (command.startsWith("statClans")) {
            statClans(player, Integer.parseInt(command.substring(9).trim()));
        } else if (command.equalsIgnoreCase("statCastles")) {
            statCastles(player);
        } else if (command.equalsIgnoreCase("changeSex")) {
            if (!Config.CHGSEX_ENABLE) {
                showError(player, "Магазин отключен.");
                return;
            }
            changeSex(player);
        } else if (command.startsWith("changeClass")) {
            if (!Config.CHGCLASS_ENABLE) {
                showError(player, "Магазин отключен.");
                return;
            }
            changeClass(player, Integer.parseInt(command.substring(11).trim()));
        } else {
            super.onBypassFeedback(player, command);
        }
    }

    // окно приветствия
    private void showWelcome(L2PcInstance player, int hasSertify) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        replyMSG.append("<table width=280><tr><td>" + player.getName() + "</td><td align=right><font color=336699>Баланс:</font> <font color=33CCFF>" + getStockBalance(player) + " " + VAL_NAME + "</font></td></tr>");
        replyMSG.append("<tr><td> </td><td align=right> <a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 4\">Личный кабинет</a></td></tr></table><br><br>");
        replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 2\">Просмотр биржи</a><br>");
        if (hasSertify == 1) {
            replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 3\">Выставить предмет</a><br>");
        } else {
            replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 5\">?Получить сертификат</a><br>");
        }
        replyMSG.append("</body></html>");

        player.setStockItem(-1, 0, 0, 0, 0);
        player.setStockInventoryItem(0, 0);
        player.setStockSelf(0);

        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    // окно личного кабинета
    private void showPrivateInfo(L2PcInstance player) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body> Личный кабинет:");
        replyMSG.append("<table width=280><tr><td>" + player.getName() + "</td><td align=right><font color=336699>Баланс:</font> <font color=33CCFF>" + getStockBalance(player) + " " + VAL_NAME + "</font></td></tr>");
        replyMSG.append("<tr><td> </td><td align=right> <a action=\"bypass -h npc_" + getObjectId() + "_showPrivateInfo 1\">Пополнить счет</a></td></tr></table><br><br>");
        replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_showPrivateInfo 2\">Мои товары</a><br>");
        //replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_showPrivateInfo 3\">Просмотреть последние действия</a> //еще не делал<br>");
        replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 1\">Вернуться</a><br>");
        replyMSG.append("</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    // окно информации о сертификате
    private void showSertifyInfo(L2PcInstance player) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body> <font color=99CC66>Торговый сертификат.<br>");
        replyMSG.append("Сертификат позволяет торговать вещами на бирже:<br>");
        replyMSG.append("Заточенным и аугментированным оружием;<br1>");
        replyMSG.append("Заточенной броней;<br1>");
        replyMSG.append("Эпик бижутерией;<br>");
        replyMSG.append("Стоимость сертификата составляет:</font><br1>");
        replyMSG.append("<table border=1 width=290><tr><td>" + SERTIFY_PRICE + " " + DONATE_COIN_NEMA + "</td></tr></table><br>");
        L2ItemInstance coins = player.getInventory().getItemByItemId(DONATE_COIN);
        if (coins != null && coins.getCount() >= SERTIFY_PRICE) {
            replyMSG.append("<button value=\"Купить\" action=\"bypass -h npc_" + getObjectId() + "_StockSertifyBuy\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        } else {
            replyMSG.append("<font color=999999>[Приобрести сертификат]</font><br>");
        }
        replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 1\">Вернуться</a><br>");
        replyMSG.append("</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    // запрос баланса на бирже
    private int getStockBalance(L2PcInstance player) {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT balance FROM `z_stock_accounts` WHERE `charId`=? LIMIT 1");
            statement.setInt(1, player.getObjectId());
            result = statement.executeQuery();

            if (result.next()) {
                return result.getInt("balance");
            }
            Close.SR(statement, result);
        } catch (Exception e) {
            _log.warning("StockExchange: getStockBalance() error: " + e);
        } finally {
            Close.CSR(con, statement, result);
        }

        return 0;
    }

    // показ шмоток на бирже
    private void showStockSellList(L2PcInstance player, int page, int self) {
        if (System.currentTimeMillis() - player.getStockLastAction() < 2000) {
            showError(player, "Раз в 2 секунды!"); //lacosta
            //showError(player, "Раз в 2 секунды.");
            return;
        }

        player.setStockLastAction(System.currentTimeMillis());

        int limit1 = 0;

        if (page == 1) {
            limit1 = 0;
        } else if (page == 2) {
            limit1 = PAGE_LIMIT;
        } else {
            limit1 = page * PAGE_LIMIT;
        }

        int limit2 = limit1 + PAGE_LIMIT;

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        replyMSG.append("<table width=300><tr><td width=36></td><td width=264>Страница " + page + ":</td></tr>");

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            if (self == 0) {
                statement = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, ownerId, shadow FROM `z_stock_items` WHERE `id` > '0' ORDER BY `id` DESC LIMIT ?, ?");
                statement.setInt(1, limit1);
                statement.setInt(2, limit2);
            } else {
                statement = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, ownerId, shadow FROM `z_stock_items` WHERE `ownerId` = ? ORDER BY `id` DESC LIMIT ?, ?");
                statement.setInt(1, player.getObjectId());
                statement.setInt(2, limit1);
                statement.setInt(3, limit2);
                player.setStockSelf(self);
            }
            rs = statement.executeQuery();
            while (rs.next()) {
                L2Item brokeItem = ItemTable.getInstance().getTemplate(rs.getInt("itemId"));
                if (brokeItem != null) {
                    replyMSG.append("<tr><td><img src=\"Icon." + brokeItem.getIcon() + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_StockShowItem " + rs.getInt("id") + "\"> " + brokeItem.getName() + "</a>+" + rs.getInt("enchant") + " <br1><font color=336699>Цена: " + rs.getInt("price") + " " + VAL_NAME + "; Продавец: " + getSellerName(rs.getInt("ownerId")) + "</font><br1>" + getAugmentSkill(rs.getInt("augment"), rs.getInt("augLvl")) + "</td></tr>");
                }
            }
            Close.SR(statement, rs);
        } catch (SQLException e) {
            _log.warning("StockExchange: showStockSellList() error: " + e);
        } finally {
            Close.CSR(con, statement, rs);
        }
        replyMSG.append("</table><br>");
        // постраничность
        int pages = getPageCount();
        if (pages >= 2) {
            replyMSG.append("Страницы:<br1><table width=300><tr>");
            int step = 0;
            for (int i = 1; i <= pages; i++) {
                if (i == page) {
                    replyMSG.append("<td>" + i + "</td>");
                } else {
                    replyMSG.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_StockShowPage " + i + " " + self + "\">" + i + "</a></td>");
                }
                if (step == 10) {
                    replyMSG.append("</tr><tr>");
                    step = 0;
                }
                step++;
            }
            replyMSG.append("</tr></table><br>");
        }
        //
        replyMSG.append("<br><br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 1\">Вернуться</a><br>");
        replyMSG.append("</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    private int getPageCount() {
        int rowCount = 0;
        int pages = 0;

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT COUNT(id) FROM z_stock_items WHERE id > ?");
            statement.setInt(1, 0);
            result = statement.executeQuery();

            result.next();
            rowCount = result.getInt(1);
            Close.SR(statement, result);
        } catch (Exception e) {
            _log.warning("StockExchange: getPageCount() error: " + e);
        } finally {
            Close.CSR(con, statement, result);
        }

        if (rowCount == 0) {
            return 0;
        }

        pages = (rowCount / PAGE_LIMIT) + 1;

        return pages;
    }

    // выставление на биржу, показ шмоток в инвентаре 
    private void showInventoryItems(L2PcInstance player) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        replyMSG.append("Выбор шмотки:<br>Что выставляем на биржу?<br><br><table width=300>");

        int objectId = 0;
        String itemName = "";
        int enchantLevel = 0;
        String itemIcon = "";
        int itemType = 0;
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            objectId = item.getObjectId();
            itemName = item.getItem().getName();
            enchantLevel = item.getEnchantLevel();
            itemIcon = item.getItem().getIcon();
            itemType = item.getItem().getType2();

            if (item.canBeEnchanted() && !item.isEquipped() && item.isDestroyable() && (itemType == L2Item.TYPE2_WEAPON || itemType == L2Item.TYPE2_SHIELD_ARMOR || itemType == L2Item.TYPE2_ACCESSORY || item.isAugmented())) {
                replyMSG.append("<tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_StockInventoryItem " + objectId + "\">" + itemName + " (+" + enchantLevel + ")</a></td></tr>");
            }
        }

        replyMSG.append("</table><br><br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 1\">Вернуться</a><br>");
        replyMSG.append("</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    // выставление на биржу, подробная инфа о шмотке
    private void showInventoryItem(L2PcInstance player, int objectId) {
        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item != null) {
            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");

            String itemName = item.getItem().getName();
            int enchantLevel = item.getEnchantLevel();
            String itemIcon = item.getItem().getIcon();

            replyMSG.append("Выставление на биржу:<br>Подтверждаете шмотку?<br>");

            replyMSG.append("<table width=300><tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + " (+" + enchantLevel + ")</font>g<br></td></tr></table><br><br>");
            replyMSG.append("<br>Заточка: <font color=bef574>+" + enchantLevel + "</font><br>");

            if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                L2Skill augment = item.getAugmentation().getAugmentSkill();
                String augName = augment.getName();
                String type = "";
                if (augment.isActive()) {
                    type = "Актив";
                } else if (augment.isPassive()) {
                    type = "Пассив";
                } else {
                    type = "Шанс";
                }

                replyMSG.append("<br>Аугмент: <font color=bef574>" + augName + " (" + type + ")</font><br>");
            }

            replyMSG.append("Введите желаемую цену:<br><edit var=\"price\" width=200 length=\"16\">");
            replyMSG.append("<button value=\"Выставить\" action=\"bypass -h npc_" + getObjectId() + "_StockAddItem " + objectId + " $price\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
            player.setStockInventoryItem(objectId, enchantLevel);

            replyMSG.append("<br><br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 1\">Вернуться</a><br>");
            replyMSG.append("</body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } else {
            showError(player, "Ошибка запроса.");
            return;
        }
    }

    // подробная инфа о шмотке на бирже
    private void showStockItem(L2PcInstance player, int sellId) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            statement = con.prepareStatement("SELECT itemId, enchant, augment, augLvl, price, ownerId, shadow FROM `z_stock_items` WHERE `id` = ? LIMIT 1");
            statement.setInt(1, sellId);
            result = statement.executeQuery();

            if (result.next()) {
                int itemId = result.getInt("itemId");
                L2Item brokeItem = ItemTable.getInstance().getTemplate(itemId);
                if (brokeItem == null) {
                    showError(player, "Ошибка запроса.");
                    return;
                }
                int price = result.getInt("price");
                int augment = result.getInt("augment");
                int auhLevel = result.getInt("augLvl");
                int enchant = result.getInt("enchant");
                int self = player.getStockSelf();
                int charId = result.getInt("ownerId");

                replyMSG.append("<table width=300><tr><td><img src=\"" + brokeItem.getIcon() + "\" width=32 height=32></td><td><font color=LEVEL>" + brokeItem.getName() + " +" + enchant + "</font><br></td></tr></table><br><br>");
                replyMSG.append("Продавец: " + getSellerName(charId) + "<br><br>");
                replyMSG.append(getAugmentSkill(augment, auhLevel) + "<br>");

                if (self == 1) {
                    if (player.getObjectId() != charId) {
                        showError(player, "Ошибка запроса.");
                        return;
                    }
                    replyMSG.append("<font color=6699CC>Стоимость: " + price + " P.</font><br>");
                    replyMSG.append("<button value=\"Забрать\" action=\"bypass -h npc_" + getObjectId() + "_StockBuyItem " + sellId + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                    player.setStockItem(sellId, itemId, enchant, augment, auhLevel);
                } else {
                    if (getStockBalance(player) >= price) {
                        replyMSG.append("<font color=33CC00>Стоимость: " + price + " P.</font><br>");
                        replyMSG.append("<button value=\"Купить\" action=\"bypass -h npc_" + getObjectId() + "_StockBuyItem " + sellId + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                        player.setStockItem(sellId, itemId, enchant, augment, auhLevel);
                    } else {
                        replyMSG.append("<font color=FF6666>Стоимость: " + price + " P.</font><br>");
                        replyMSG.append("<font color=999999>[Купить]</font>");
                    }
                }
            } else {
                replyMSG.append("Не найдена или уже купили.");
            }
            Close.SR(statement, result);
        } catch (Exception e) {
            _log.warning("StockExchange: showStockItem() error: " + e);
        } finally {
            Close.CSR(con, statement, result);
        }

        replyMSG.append("<br><br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 1\">Вернуться</a><br>");
        replyMSG.append("</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    // finish выставление на биржу
    private void addStockItem(L2PcInstance player, int objectId, int price) {
        if (player.getObjectIdStockI() != objectId) {
            showError(player, "Ошибка запроса.");
            return;
        }

        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item != null) {
            int enchantLevel = item.getEnchantLevel();
            if (player.getEnchantStockI() != enchantLevel) {
                showError(player, "Ошибка запроса.");
                return;
            }

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");

            int itemId = item.getItemId();
            int augmentId = 0;
            int augAttr = 0;
            int augLvl = 0;
            int shadow = 0;
            String itemName = item.getItem().getName();
            String itemIcon = item.getItem().getIcon();

            replyMSG.append("Выставление на биржу:<br>Выставлена шмотка<br>");
            replyMSG.append("<table width=300><tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + " (+" + enchantLevel + ")</font>g<br></td></tr></table><br><br>");
            replyMSG.append("<br>Заточка: <font color=bef574>+" + enchantLevel + "</font><br>");

            if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                L2Skill augment = item.getAugmentation().getAugmentSkill();
                String augName = augment.getName();
                String type = "";
                if (augment.isActive()) {
                    type = "Актив";
                } else if (augment.isPassive()) {
                    type = "Пассив";
                } else {
                    type = "Шанс";
                }

                augmentId = augment.getId();
                augAttr = item.getAugmentation().getAugmentationId();
                augLvl = augment.getLevel();

                replyMSG.append("<br>Аугмент: <font color=bef574>" + augName + " (" + type + ")</font><br>");
            }

            if (!player.destroyItemByItemId("DS addStockItem", itemId, 1, player, true)) {
                showError(player, "Ошибка запроса.");
                return;
            }

            if (addToStockList(itemId, enchantLevel, augmentId, augAttr, augLvl, price, player.getObjectId(), shadow)) {
                //player.getInventory().destroyItem(item, 1, true);		

                replyMSG.append("Выставлена!<br><br>");
            } else {
                replyMSG.append("Произошла ошибка!<br><br>");
            }

            player.setStockInventoryItem(0, 0);

            replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 1\">Вернуться.</a>");
            replyMSG.append("</body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } else {
            showError(player, "Ошибка запроса.");
            return;
        }
    }

    // покупка с биржи, задержка 1-2 секунд
    private void buyStockItem(L2PcInstance player, int sellId) {
        if (player.getSellIdStock() != sellId) {
            showError(player, "Ошибка запроса.");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>Подождите...</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);

        try {
            Thread.sleep(Rnd.get(1000, 2000));
        } catch (InterruptedException e) {
        }

        buyStockItemFinish(player, sellId);
    }

    // finish покупка с биржи
    private void buyStockItemFinish(L2PcInstance player, int sellId) {
        if (player.getSellIdStock() != sellId) {
            showError(player, "Ошибка запроса.");
            return;
        }

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            statement = con.prepareStatement("SELECT itemId, enchant, augment, augAttr, augLvl, price, ownerId, shadow FROM `z_stock_items` WHERE `id` = ? LIMIT 1");
            statement.setInt(1, sellId);
            result = statement.executeQuery();

            if (result.next()) {
                int itemId = result.getInt("itemId");
                if (player.getItemIdStock() != itemId) {
                    showError(player, "Ошибка запроса.");
                    return;
                }
                L2Item brokeItem = ItemTable.getInstance().getTemplate(itemId);
                if (brokeItem == null) {
                    showError(player, "Ошибка запроса.");
                    return;
                }
                int price = result.getInt("price");
                int augment = result.getInt("augment");
                int auhLevel = result.getInt("augLvl");
                int enchant = result.getInt("enchant");
                int augAttr = result.getInt("augAttr");
                int ownerId = result.getInt("ownerId");
                int self = player.getStockSelf();

                if (self == 1 && player.getObjectId() != ownerId) {
                    showError(player, "Ошибка запроса.");
                    return;
                }

                if (player.getEnchantStock() != enchant || player.getAugmentStock() != augment || player.getAuhLeveStock() != auhLevel) {
                    showError(player, "Ошибка запроса.");
                    return;
                }

                if (self == 0) {
                    if (!updateBalance(player, ownerId, price)) {
                        showError(player, "Ошибка баланса.");
                        return;
                    }
                }

                if (!deleteFromList(player, sellId, itemId, enchant, augment, auhLevel, price, ownerId, self)) {
                    showError(player, "Ошибка запроса.");
                    return;
                }

                player.setStockItem(-1, 0, 0, 0, 0);
                player.setStockSelf(0);

                /*L2ItemInstance buyItem = ItemTable.getInstance().createItem(itemId)createItem(itemId);
                 L2ItemInstance buyItem = ItemTable.getInstance().createItem("DS buyStockItemFinish", itemId, 1, player);
                 if (enchant > 0)
                 buyItem.setEnchantLevel(enchant);
                 if (augment > 0)
                 buyItem.setAugmentation(new L2Augmentation(buyItem, augAttr, augment, auhLevel, true));
                 */
                L2ItemInstance buyItem = player.getInventory().addItem("DS buyStockItemFinish", itemId, 1, player, player.getTarget());
                if (buyItem == null) {
                    showError(player, "Ошибка запроса.");
                    return;
                }

                if (enchant > 0) {
                    buyItem.setEnchantLevel(enchant);
                }
                if (augment > 0) {
                    buyItem.setAugmentation(new L2Augmentation(buyItem, augAttr, augment, auhLevel, true));
                }

                NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
                TextBuilder replyMSG = new TextBuilder("<html><body>");
                if (self == 0) {
                    L2PcInstance owner = L2World.getInstance().getPlayer(ownerId);
                    if (owner != null) {
                        owner.sendMessage("Уведомление с биржи: проверь почту");
                        owner.sendPacket(new ExMailArrived());
                        sendLetter(getSellerName(ownerId), itemId, enchant, augment, auhLevel, price);
                    }
                    replyMSG.append("Сделка проведена, с вашего счета списано: " + price + " " + VAL_NAME + "");
                } else {
                    replyMSG.append("<br>Предмет успешно снят с биржи");
                }
                replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 1\">Вернуться.</a></body></html>");
                reply.setHtml(replyMSG.toString());
                player.sendPacket(reply);

                player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(itemId));
                //player.getInventory().addItem(buyItem);
                //player.getInventory().addItem("DS buyStockItemFinish", buyItem, player, player.getTarget());

                player.sendItems(false);
                player.sendChanges();
            } else {
                showError(player, "Ошибка запроса.");
                return;
            }
            Close.SR(statement, result);
        } catch (Exception e) {
            _log.warning("StockExchange: buyStockItemFinish() error: " + e);
        } finally {
            Close.CSR(con, statement, result);
        }
    }

    private void sendLetter(String sName, int itemId, int enchant, int augment, int auhLevel, int price) {
        L2Item brokeItem = ItemTable.getInstance().getTemplate(itemId);
        if (brokeItem == null) {
            return;
        }

        Date date = new Date();
        SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timef = new SimpleDateFormat("HH:mm:ss");

        int plus = (price / 100) * NALOG_NPS;

        StringBuilder text = new StringBuilder();
        text.append("Итем: <font color=FF3399>" + brokeItem.getName() + " +" + enchant + " " + getAugmentSkill(augment, auhLevel) + "</font><br1> был успешно продан.<br1>");
        text.append("За вычетом налога " + NALOG_NPS + "%, ваш баланс пополнен на " + plus + " P.<br1>");
        text.append("Благодарим за сотрудничество.");

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            //statement = con.prepareStatement("INSERT INTO `z_stock_accounts` (`charId`,`balance`,`ban`) VALUES (?,?,?)");
            statement = con.prepareStatement("INSERT INTO `z_post_in` (`id`,`tema`,`text`,`from`,`to`,`type`,`date`,`time`) VALUES (NULL,?,?,?,?,?,?,?)");
            statement.setString(1, "Шмотка продана");
            statement.setString(2, text.toString());
            statement.setString(3, "~Биржа.");
            statement.setString(4, sName);
            statement.setInt(5, 0);
            statement.setString(6, datef.format(date).toString());
            statement.setString(7, timef.format(date).toString());

            statement.execute();
            Close.S(statement);
        } catch (final Exception e) {
            _log.warning("StockExchange: sendLetter() error: " + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    // покупка сертификата
    private void StockSertifyBuy(L2PcInstance player) {
        L2ItemInstance coins = player.getInventory().getItemByItemId(DONATE_COIN);
        if (coins == null || coins.getCount() < SERTIFY_PRICE) {
            showError(player, "Ошибка запроса.");
            return;
        }

        //player.getInventory().destroyItem(coins, 10, true);
        player.destroyItemByItemId("DS StockSertifyBuy", DONATE_COIN, SERTIFY_PRICE, player, true);

        //L2ItemInstance buyItem = ItemTable.getInstance().createItem(STOCK_SERTIFY);
        //L2ItemInstance buyItem = ItemTable.getInstance().createItem("DS buyStockItemFinish", STOCK_SERTIFY, 1, player);
        L2ItemInstance buyItem = player.getInventory().addItem("DS buyStockItemFinish", STOCK_SERTIFY, 1, player, player.getTarget());
        if (buyItem == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(buyItem.getItemId()));
        player.sendItems(false);
        player.sendChanges();

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("INSERT INTO `z_stock_accounts` (`charId`,`balance`,`ban`) VALUES (?,?,?)");
            statement.setInt(1, player.getObjectId());
            statement.setInt(2, FIRST_BALANCE);
            statement.setInt(3, 0);
            statement.execute();
            Close.S(statement);
        } catch (final Exception e) {
            _log.warning("StockExchange: StockSertifyBuy() error: " + e);
        } finally {
            Close.CS(con, statement);
        }

        showWelcome(player, 1);
    }

    // при покупке с биржи, удаляем шмотку с базы
    private boolean deleteFromList(L2PcInstance player, int sellId, int itemId, int enchant, int augment, int auhLevel, int price, int ownerId, int self) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("DELETE FROM z_stock_items WHERE id = ? LIMIT 1");
            statement.setInt(1, sellId);
            statement.execute();
            Close.S(statement);

            if (self == 0) {
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("dd mm (HH:mm:ss)");

                statement = con.prepareStatement("INSERT INTO z_stock_logs (date, charId, itemId, enchant, augment, augLvl, price, ownerId) VALUES (?,?,?,?,?,?,?,?)");
                statement.setString(1, sdf.format(date) + ":");
                statement.setInt(2, player.getObjectId());
                statement.setInt(3, itemId);
                statement.setInt(4, enchant);
                statement.setInt(5, augment);
                statement.setInt(6, auhLevel);
                statement.setInt(7, price);
                statement.setInt(8, ownerId);
                statement.execute();
                Close.S(statement);
            }
            return true;
        } catch (final Exception e) {
            _log.warning("StockExchange: deleteFromList() error: " + e);
        } finally {
            Close.CS(con, statement);
        }
        return false;
    }

    // выставление шмотки на биржу ,пишем в базу
    private boolean addToStockList(int itemId, int enchantLevel, int augmentId, int augAttr, int augLvl, int price, int ownerId, int shadow) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("INSERT INTO z_stock_items (id,itemId, enchant, augment, augAttr, augLvl, price, ownerId, shadow) VALUES (NULL,?,?,?,?,?,?,?,?)");
            statement.setInt(1, itemId);
            statement.setInt(2, enchantLevel);
            statement.setInt(3, augmentId);
            statement.setInt(4, augAttr);
            statement.setInt(5, augLvl);
            statement.setInt(6, price);
            statement.setInt(7, ownerId);
            statement.setInt(8, shadow);
            statement.execute();
            Close.S(statement);
            return true;
        } catch (final SQLException e) {
            _log.warning("StockExchange: addToStockList() error: " + e);
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
        return false;
    }

    // обновление баланса, снятие денег и пополнение счета продавца за вычетом налога NALOG_NPS
    private boolean updateBalance(L2PcInstance player, int ownerId, int price) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE z_stock_accounts SET `balance` = `balance`-? WHERE charId=? LIMIT 1");
            statement.setInt(1, price);
            statement.setInt(2, player.getObjectId());
            statement.executeUpdate();
            Close.S(statement);

            int plus = (price / 100) * NALOG_NPS;

            statement = con.prepareStatement("UPDATE z_stock_accounts SET `balance` = `balance`+? WHERE charId=? LIMIT 1");
            statement.setInt(1, plus);
            statement.setInt(2, ownerId);
            statement.executeUpdate();
            Close.S(statement);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
        return false;
    }

    // информации об аугментации
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

    // информация о продавце
    private String getSellerName(int charId) {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            con = L2DatabaseFactory.get();
            /*statement = con.createStatement();
             result = statement.executeQuery("SELECT char_name FROM `characters` WHERE `Obj_Id`='" + charId + "' LIMIT 1");*/

            statement = con.prepareStatement("SELECT char_name FROM `characters` WHERE `Obj_Id`=? LIMIT 1");
            statement.setInt(1, charId);
            result = statement.executeQuery();

            if (result.next()) {
                return result.getString("char_name");
            }
            Close.SR(statement, result);
        } catch (Exception e) {
            _log.warning("StockExchange: getSellerName() error: " + e);
        } finally {
            Close.CSR(con, statement, result);
        }

        return "";
    }

    // личный кабинет - окно пополнения баланса
    private void incBalance(L2PcInstance player) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>Перевод " + DONATE_COIN_NEMA + " в валюту биржи:<br>");

        L2ItemInstance coin = player.getInventory().getItemByItemId(DONATE_COIN);
        if (coin != null && coin.getCount() >= 1) {
            long dnCount = coin.getCount();
            long stCount = dnCount * DONATE_RATE;
            replyMSG.append("<table border=1 width=290><tr><td>Курс: 1" + DONATE_COIN_NEMA + " за " + DONATE_RATE + " P.</td></tr></table><br>");
            replyMSG.append("<font color=99CC99>У вас есть " + dnCount + " " + DONATE_COIN_NEMA + ";</font><br>");
            replyMSG.append("Вы можете пополнить счет биржи на " + stCount + " P. <button value=\"На все!\" action=\"bypass -h npc_" + getObjectId() + "_StockAddBalance 0\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
            replyMSG.append("Или введите свое число..<br>");
            replyMSG.append("Так сколько?<br><edit var=\"coins\" width=200 length=\"16\">");
            replyMSG.append("<button value=\"Пополнить\" action=\"bypass -h npc_" + getObjectId() + "_StockAddBalance $coins\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        } else {
            replyMSG.append("<font color=CC0000>К обмену принимаются только " + DONATE_COIN_NEMA + "</font>");
        }

        replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 4\">Вернуться.</a></body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    // личный кабинет - finish перегон монет
    private void StockAddBalance(L2PcInstance player, int coins) {
        int plus = 0;
        int cnCount = 0;
        L2ItemInstance coin = player.getInventory().getItemByItemId(DONATE_COIN);
        if (coin != null) {
            cnCount = coin.getCount();
            if (coins == 0 && cnCount >= 1) {
                plus = cnCount * DONATE_RATE;
            } else if (cnCount >= coins) {
                plus = coins * DONATE_RATE;
            } else {
                showError(player, "Ошибка запроса.");
                return;
            }
        }

        if (coins == 0) {
            coins = cnCount;
        }

        if (!player.destroyItemByItemId("DS StockAddBalance", DONATE_COIN, coins, player, true)) {
            showError(player, "Ошибка запроса.");
            return;
        }

        Connect con = null;
        PreparedStatement statement = null;
        try {
            //player.getInventory().destroyItemByItemId(DONATE_COIN, coins, true);
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE z_stock_accounts SET `balance` = `balance`+? WHERE charId=? LIMIT 1");
            statement.setInt(1, (int) plus);
            statement.setInt(2, player.getObjectId());
            statement.executeUpdate();
            Close.S(statement);

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>Перевод " + DONATE_COIN_NEMA + " в валюту биржи:<br>");
            replyMSG.append("<font color=99CC99>Баланс пополнен на " + plus + " " + VAL_NAME + "</font>");
            replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_StockExchange 4\">Вернуться.</a></body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    /**
     * Далее перенос заточки и аугментации
     *
     */
    private void showVoteServiceWindow(L2PcInstance player, int service, int page) {
        FastTable<L2ItemInstance> items = null;
        if (service == 1) {
            items = player.getInventory().getAllItemsAug();
        } else {
            items = player.getInventory().getAllItemsEnch();
        }

        if (items == null || items.isEmpty()) {
            player.sendHtmlMessage("Голосование", "Ваш инвентарь пуст.");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        if (service == 1) {
            replyMSG.append("Перенос ЛС:<br>Откуда переносим?<br><br><table width=300>");
        } else {
            replyMSG.append("Перенос заточки:<br>Откуда переносим?<br><br><table width=300>");
        }

        int objectId = 0;
        String itemName = "";
        int enchantLevel = 0;
        String itemIcon = "";
        int itemType = 0;
        //for(L2ItemInstance item : player.getInventory().getItems())
        int begin = page == 1 ? 0 : page == 2 ? PAGE_LIMIT : page * PAGE_LIMIT;
        int end = begin + PAGE_LIMIT;
        if (end > items.size()) {
            end = items.size();
        }
        L2ItemInstance item = null;
        for (int i = begin, n = end; i < n; i++) {
            item = items.get(i);
            if (item == null) {
                continue;
            }

            objectId = item.getObjectId();
            itemName = item.getItem().getName();
            enchantLevel = item.getEnchantLevel();
            itemIcon = item.getItem().getIcon();
            itemType = item.getItem().getType2();

            if (service == 1) {
                if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                    L2Skill aug = item.getAugmentation().getAugmentSkill();
                    replyMSG.append("<tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_voteItemShow_1_" + objectId + "\">" + itemName + " (+" + enchantLevel + ")</a><br1> " + getAugmentSkill(aug.getId(), aug.getLevel()) + " </td></tr>");
                }
            } else if (service == 2) {
                if (itemType == L2Item.TYPE2_WEAPON || itemType == L2Item.TYPE2_SHIELD_ARMOR || itemType == L2Item.TYPE2_ACCESSORY) {
                    replyMSG.append("<tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_voteItemShow_2_" + objectId + "\">" + itemName + " (+" + enchantLevel + ")</a></td></tr>");
                }
            }
        }

        player.setVote1Item(0);
        player.setVote2Item(0);
        player.setVoteEnchant(0);
        player.setVoteAugment(null);

        replyMSG.append("</table><br><br>");
        if (items.size() > PAGE_LIMIT) {
            replyMSG.append("Страницы: <br1>");
            int pages = (items.size() / PAGE_LIMIT) + 1;
            for (int i = 0; i < pages; i++) {
                int cur = i + 1;
                if (page == cur) {
                    replyMSG.append("&nbsp;&nbsp;" + cur + "&nbsp;&nbsp;");
                } else {
                    replyMSG.append("&nbsp;&nbsp;<a action=\"bypass -h npc_" + getObjectId() + "_voteService " + service + " " + cur + "\">" + cur + "</a>&nbsp;&nbsp;");
                }
            }
        }
        replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_voteHome\">Вернуться.</a></body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
        items.clear();
        items = null;
    }

    private void showVoteNextItems(L2PcInstance player, int service, int page) {
        FastTable<L2ItemInstance> items = player.getInventory().getAllItemsNext(player.getVote1Item(), service);
        if (items == null || items.isEmpty()) {
            player.sendHtmlMessage("Голосование", "Ваш инвентарь пуст.");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        if (service == 1) {
            replyMSG.append("Перенос ЛС:<br>Куда переносим?<br><br><table width=300>");
        } else {
            replyMSG.append("Перенос заточки:<br>Куда переносим?<br><br><table width=300>");
        }

        //
        int begin = page == 1 ? 0 : page == 2 ? PAGE_LIMIT : page * PAGE_LIMIT;
        int end = begin + PAGE_LIMIT;
        if (end > items.size()) {
            end = items.size();
        }
        L2ItemInstance item = null;
        for (int i = begin, n = end; i < n; i++) {
            item = items.get(i);
            if (item == null) {
                continue;
            }

            if (service == 1) {
                replyMSG.append("<tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_voteItem2Show_1_" + item.getObjectId() + "\">" + item.getItem().getName() + " (+" + item.getEnchantLevel() + ")</a></td></tr>");
            } else {
                replyMSG.append("<tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_voteItem2Show_2_" + item.getObjectId() + "\">" + item.getItem().getName() + " (+" + item.getEnchantLevel() + ")</a></td></tr>");
            }
        }

        replyMSG.append("</table><br><br>");
        if (items.size() > PAGE_LIMIT) {
            replyMSG.append("Страницы: <br1>");
            int pages = (items.size() / PAGE_LIMIT) + 1;
            for (int i = 0; i < pages; i++) {
                int cur = i + 1;
                if (page == cur) {
                    replyMSG.append("&nbsp;&nbsp;" + cur + "&nbsp;&nbsp;");
                } else {
                    replyMSG.append("&nbsp;&nbsp;<a action=\"bypass -h npc_" + getObjectId() + "_voteStep2 " + service + " " + cur + "\">" + cur + "</a>&nbsp;&nbsp;");
                }
            }
        }
        replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_voteHome\">Вернуться.</a></body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    private void showVote1Item(L2PcInstance player, int service, int objectId) {
        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item != null) {
            if (!item.canBeEnchanted()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            if (service == 1 && !item.isAugmented()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");

            int coinId = 0;
            String itemName = item.getItem().getName();
            int enchantLevel = item.getEnchantLevel();
            String itemIcon = item.getItem().getIcon();

            if (service == 1) {
                replyMSG.append("Перенос ЛС:<br>Из этой шмотки переносим?<br>");
                coinId = AUGMENT_COIN;
            } else {
                replyMSG.append("Перенос заточки:<br>Из этой шмотки переносим?<br>");
                coinId = ENCHANT_COIN;
            }

            L2ItemInstance coin = player.getInventory().getItemByItemId(coinId);

            if (service == 1) {
                replyMSG.append("<table width=300><tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + " (+" + enchantLevel + ")</font><br></td></tr></table><br><br>");
                L2Skill augment = item.getAugmentation().getAugmentSkill();
                if (augment == null) {
                    showVoteErr0r(player, service, 1);
                    return;
                }

                String augName = augment.getName();
                String type = "";
                if (augment.isActive()) {
                    type = "(Активный)";
                } else if (augment.isPassive()) {
                    type = "(Пассивный)";
                } else {
                    type = "(Шансовый)";
                }

                replyMSG.append("<br>Аугмент: <font color=bef574>" + augName + "" + type + "</font><br>");
                if (coin != null && coin.getCount() >= AUGMENT_PRICE) {
                    replyMSG.append("<font color=33CC00>Стоимость: " + AUGMENT_PRICE + " " + AUGMENT_COIN_NAME + "</font><br>");
                    replyMSG.append("<button value=\"Продолжить\" action=\"bypass -h npc_" + getObjectId() + "_voteStep2 1 1\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                    player.setVote1Item(objectId);
                    player.setVoteAugment(augment);
                } else {
                    replyMSG.append("<font color=FF6666>Стоимость: " + AUGMENT_PRICE + " " + AUGMENT_COIN_NAME + "</font><br>");
                    replyMSG.append("<font color=999999>[Продолжить]</font>");
                }
            } else {
                int vePrice = ENCHANT_PRICE * enchantLevel;
                replyMSG.append("<table width=300><tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + " (+" + enchantLevel + ")</font><br></td></tr></table><br><br>");
                replyMSG.append("<br>Заточка: <font color=bef574>+" + enchantLevel + "</font><br>");
                if (coin != null && coin.getCount() >= vePrice) {
                    replyMSG.append("<font color=33CC00>Стоимость: " + vePrice + " " + ENCHANT_COIN_NAME + "</font><br>");
                    replyMSG.append("<button value=\"Продолжить\" action=\"bypass -h npc_" + getObjectId() + "_voteStep2 2 1\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                    player.setVote1Item(objectId);
                    player.setVoteEnchant(enchantLevel);
                } else {
                    replyMSG.append("<font color=FF6666>Стоимость: " + vePrice + " " + ENCHANT_COIN_NAME + "</font><br>");
                    replyMSG.append("<font color=999999>[Продолжить]</font>");
                }
            }
            replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_voteHome\">Вернуться.</a>");
            replyMSG.append("</body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } else {
            showVoteErr0r(player, service, 2);
            return;
        }
    }

    private void showVote2Item(L2PcInstance player, int service, int objectId) {
        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item != null) {
            if (!item.canBeEnchanted()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            if (service == 1 && item.isAugmented()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");

            int coinId = 0;
            String itemName = item.getItem().getName();
            int enchantLevel = item.getEnchantLevel();
            String itemIcon = item.getItem().getIcon();

            if (service == 1) {
                replyMSG.append("Перенос ЛС:<br>В эту шмотку переносим?<br>");
                coinId = AUGMENT_COIN;
            } else {
                replyMSG.append("Перенос заточки:<br>В эту шмотку переносим?<br>");
                coinId = ENCHANT_COIN;
            }

            L2ItemInstance coin = player.getInventory().getItemByItemId(coinId);

            if (service == 1) {
                replyMSG.append("<table width=300><tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + " (+" + enchantLevel + ")</font><br></td></tr></table><br><br>");
                if (coin != null && coin.getCount() >= AUGMENT_PRICE) {
                    replyMSG.append("<font color=33CC00>Стоимость: " + AUGMENT_PRICE + " " + AUGMENT_COIN_NAME + "</font><br>");
                    replyMSG.append("<button value=\"Продолжить\" action=\"bypass -h npc_" + getObjectId() + "_voteStep3_1\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                    player.setVote2Item(objectId);
                } else {
                    replyMSG.append("<font color=FF6666>Стоимость: " + AUGMENT_PRICE + " Ble Eva</font><br>");
                    replyMSG.append("<font color=999999>[Продолжить]</font>");
                }
            } else {
                int enchPrice = ENCHANT_PRICE * player.getVoteEnchant();
                replyMSG.append("<table width=300><tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + " (+" + enchantLevel + ")</font><br></td></tr></table><br><br>");
                replyMSG.append("<br>Заточка: <font color=bef574>+" + enchantLevel + "</font><br>");
                if (coin != null && coin.getCount() >= enchPrice) {
                    if (enchantLevel > 0) {
                        replyMSG.append("<font color=CC6633>Аккуратно! Пушка уже заточена и при переносе точка на ней пропадет!</font><br><br>");
                    }
                    replyMSG.append("<font color=33CC00>Стоимость: " + enchPrice + " " + ENCHANT_COIN_NAME + "</font><br>");
                    replyMSG.append("<button value=\"Продолжить\" action=\"bypass -h npc_" + getObjectId() + "_voteStep3_2\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                    player.setVote2Item(objectId);
                } else {
                    replyMSG.append("<font color=FF6666>Стоимость: " + enchPrice + " " + ENCHANT_COIN_NAME + "</font><br>");
                    replyMSG.append("<font color=999999>[Продолжить]</font>");
                }
            }
            replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_voteHome\">Вернуться.</a>");
            replyMSG.append("</body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } else {
            showVoteErr0r(player, service, 2);
            return;
        }
    }

    private void showVoteAgree(L2PcInstance player, int service) {
        L2ItemInstance item1 = player.getInventory().getItemByObjectId(player.getVote1Item());
        L2ItemInstance item2 = player.getInventory().getItemByObjectId(player.getVote2Item());
        if (item1 != null && item2 != null) {
            if (!item1.canBeEnchanted() || !item2.canBeEnchanted()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            if (service == 1 && !item1.isAugmented()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            if (service == 1 && item2.isAugmented()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");

            int coinId = 0;
            String itemName1 = item1.getItem().getName();
            int enchantLevel1 = item1.getEnchantLevel();
            String itemName2 = item2.getItem().getName();
            int enchantLevel2 = item2.getEnchantLevel();
            String itemIcon1 = item1.getItem().getIcon();
            String itemIcon2 = item2.getItem().getIcon();

            if (service == 1) {
                replyMSG.append("Перенос ЛС:<br>Подтверждаете?<br>");
                coinId = AUGMENT_COIN;
            } else {
                replyMSG.append("Перенос заточки:<br>Подтверждаете?<br>");
                coinId = ENCHANT_COIN;
            }

            L2ItemInstance coin = player.getInventory().getItemByItemId(coinId);

            if (service == 1) {
                L2Skill augment = item1.getAugmentation().getAugmentSkill();

                if (augment == null || player.getVoteAugment() != augment) {
                    showVoteErr0r(player, service, 1);
                    return;
                }

                String augName = augment.getName();
                String type = "";
                if (augment.isActive()) {
                    type = "(Активный)";
                } else if (augment.isPassive()) {
                    type = "(Пассивный)";
                } else {
                    type = "(Шансовый)";
                }

                replyMSG.append("<br>Аугмент: <font color=bef574>" + augName + "" + type + "</font><br>");
                replyMSG.append("<table width=300><tr><td><img src=\"" + itemIcon1 + "\" width=32 height=32></td><td> >>>>>> </td><td><img src=\"" + itemIcon2 + "\" width=32 height=32></td></tr></table><br><br>");
                replyMSG.append("Из: <font color=LEVEL>" + itemName1 + " (+" + enchantLevel1 + ")</font><br>");
                replyMSG.append("В: <font color=LEVEL>" + itemName2 + " (+" + enchantLevel2 + ")</font><br>");

                if (coin != null && coin.getCount() >= AUGMENT_PRICE) {
                    replyMSG.append("<font color=33CC00>Стоимость: " + AUGMENT_PRICE + " " + AUGMENT_COIN_NAME + "</font><br>");
                    replyMSG.append("<button value=\"Перенести\" action=\"bypass -h npc_" + getObjectId() + "_voteComplete_1\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                } else {
                    replyMSG.append("<font color=FF6666>Стоимость: " + AUGMENT_PRICE + " Ble Eva</font><br>");
                    replyMSG.append("<font color=999999>[Продолжить]</font>");
                }
            } else {
                if (player.getVoteEnchant() != enchantLevel1 || enchantLevel1 == 0) {
                    showVoteErr0r(player, service, 1);
                    return;
                }

                int enchPrice = ENCHANT_PRICE * enchantLevel1;

                replyMSG.append("<br>Заточка: <font color=bef574>+" + enchantLevel1 + "</font><br>");
                replyMSG.append("<table width=220><tr><td><img src=\"" + itemIcon1 + "\" width=32 height=32></td><td> >>>>>> </td><td><img src=\"" + itemIcon2 + "\" width=32 height=32></td></tr></table><br><br>");
                replyMSG.append("Из: <font color=LEVEL>" + itemName1 + " (+" + enchantLevel1 + ")</font><br>");
                replyMSG.append("В: <font color=LEVEL>" + itemName2 + " (+" + enchantLevel2 + ")</font><br>");

                if (coin != null && coin.getCount() >= enchPrice) {
                    replyMSG.append("<font color=33CC00>Стоимость: " + enchPrice + " " + ENCHANT_COIN_NAME + "</font><br>");
                    replyMSG.append("<button value=\"Перенести\" action=\"bypass -h npc_" + getObjectId() + "_voteComplete_2\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
                } else {
                    replyMSG.append("<font color=FF6666>Стоимость: " + enchPrice + " " + ENCHANT_COIN_NAME + "</font><br>");
                    replyMSG.append("<font color=999999>[Продолжить]</font>");
                }
            }
            replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_voteHome\">Вернуться.</a>");
            replyMSG.append("</body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } else {
            showVoteErr0r(player, service, 2);
            return;
        }
    }

    private void showDoVoteFinish(L2PcInstance player, int service) {
        L2ItemInstance item1 = player.getInventory().getItemByObjectId(player.getVote1Item());
        L2ItemInstance item2 = player.getInventory().getItemByObjectId(player.getVote2Item());
        if (item1 != null && item2 != null) {
            if (!item1.canBeEnchanted() || !item2.canBeEnchanted()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            if (service == 1 && !item1.isAugmented()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            if (service == 1 && item2.isAugmented()) {
                showVoteErr0r(player, service, 1);
                return;
            }

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");

            int coinId = 5962;
            int price = 99999;
            int enchantLevel1 = item1.getEnchantLevel();

            if (service == 1) {
                coinId = AUGMENT_COIN;
                price = AUGMENT_PRICE;
            } else {
                coinId = ENCHANT_COIN;
                price = ENCHANT_PRICE * enchantLevel1;
            }

            L2ItemInstance coin = player.getInventory().getItemByItemId(coinId);

            if (coin == null || coin.getCount() < price) {
                showVoteErr0r(player, service, 3);
                return;
            }

            //player.getInventory().destroyItemByItemId(coinId, price, true);				
            if (!player.destroyItemByItemId("DS showDoVoteFinish", coinId, price, player, true)) {
                showVoteErr0r(player, service, 3);
                return;
            }

            if (service == 1) {
                L2Skill augment = item1.getAugmentation().getAugmentSkill();

                if (augment == null || player.getVoteAugment() != augment) {
                    showVoteErr0r(player, service, 1);
                    return;
                }

                int augId = augment.getId();
                int augLevel = augment.getLevel();
                int augEffId = item1.getAugmentation().getAugmentationId();

                String augName = augment.getName();
                String type = "";
                if (augment.isActive()) {
                    type = "(Активный)";
                } else if (augment.isPassive()) {
                    type = "(Пассивный)";
                } else {
                    type = "(Шансовый)";
                }

                item1.getAugmentation().removeBoni(player);
                item1.removeAugmentation();

                //item2.setAugmentation(new L2Augmentation(augEffId, augId, augLevel));
                item2.setAugmentation(new L2Augmentation(item2, augEffId, augId, augLevel, true));

                replyMSG.append("<br>Аугмент: <font color=bef574>" + augName + "" + type + "</font>...<br>");
                replyMSG.append("<font color=33CC00>...перенесен!<br>");
            } else {
                if (player.getVoteEnchant() != enchantLevel1) {
                    showVoteErr0r(player, service, 1);
                    return;
                }

                item1.setEnchantLevel(0);
                item1.updateDatabase();

                item2.setEnchantLevel(enchantLevel1);
                item2.updateDatabase();

                replyMSG.append("<br>Заточка: <font color=bef574>+" + enchantLevel1 + "</font><br>");
                replyMSG.append("<font color=33CC00>...перенесена!<br>");
            }

            //player.sendPacket(new InventoryUpdate().addModifiedItem(item1));
            //player.sendPacket(new InventoryUpdate().addModifiedItem(item2));
            player.sendItems(false);
            player.broadcastUserInfo();

            player.setVote1Item(0);
            player.setVote2Item(0);
            player.setVoteEnchant(0);
            player.setVoteAugment(null);

            //player.sendChanges();
            replyMSG.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_voteHome\">Вернуться.</a>");
            replyMSG.append("</body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } else {
            showVoteErr0r(player, service, 0);
            return;
        }
    }

    private void showVoteErr0r(L2PcInstance player, int serviceId, int errorId) {
        String Service;
        if (serviceId == 1) {
            Service = "Перенос аугментации:";
        } else {
            Service = "Перенос заточки:";
        }

        String Error = "Ошибка!";

        switch (errorId) {
            case 0:
                Error = "пушки не найдены";
                break;
            case 1:
                Error = "пушка не соответствует параметрам";
                break;
            case 2:
                Error = "пушка не найдена";
                break;
            case 3:
                Error = "проверьте стоимость";
                break;
        }

        player.setVote1Item(0);
        player.setVote2Item(0);
        player.setVoteEnchant(0);
        player.setVoteAugment(null);

        //player.sendMessage(Service + " " + Error);
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body> " + Service + " " + Error + "</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
        player.sendActionFailed();
    }

    /*
     * Сервис для кланов
     */
    private void clanWelcome(L2PcInstance player) {
        L2Clan clan = player.getClan();
        int level = clan.getLevel();
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        replyMSG.append("<table width=280><tr><td><font color=336699>Клан:</font> <font color=33CCFF>" + clan.getName() + " (" + level + " ур.)</font></td><td align=right><font color=336699>Лидер:</font> <font color=33CCFF>" + player.getName() + "</font></td></tr></table><br><br>");
        replyMSG.append("Повышение уровня клана:<br1>");
        if (level < 8) {
            replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanLevel_8\" msg=\"Покупка 8 уровень кланa. Уверены?\">8 уровень.</a> (" + CLAN_LVL8 + " " + CLAN_COIN_NAME + ")<br>");
            /*switch (level)
             {
             case 5:
             replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanLevel_6\">6 уровень</a> (" + CLAN_LVL6 + " " + CLAN_COIN_NAME + ")<br>");
             replyMSG.append("<font color=999999>[7 уровень]</font> (" + CLAN_LVL7 + " " + CLAN_COIN_NAME + ")<br>");
             replyMSG.append("<font color=999999>[8 уровень]</font> (" + CLAN_LVL8 + " " + CLAN_COIN_NAME + ")<br>");
             break;
             case 6:
             replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanLevel_7\">7 уровень.</a> (" + CLAN_LVL7 + " " + CLAN_COIN_NAME + ")<br>");
             replyMSG.append("<font color=999999>[8 уровень]</font> (" + CLAN_LVL8 + " " + CLAN_COIN_NAME + ")<br>");
             break;
             case 7:
             replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanLevel_8\">8 уровень.</a> (" + CLAN_LVL8 + " " + CLAN_COIN_NAME + ")<br>");
             break;
             }*/
        } else {
            replyMSG.append("<font color=66CC00>Уже максимальный!</font><br>");
        }

        replyMSG.append("Дополнительно:<br1>");
        if (level >= 5) {
            replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanPoints\" msg=\"Покупка " + CLAN_POINTS + " клан очков. Уверены?\">" + CLAN_POINTS + " клан очков. </a> (" + CLAN_POINTS_PRICE + " " + CLAN_COIN_NAME + ")<br>");
            replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanSkills\" msg=\"Покупка фулл клан скиллов. Уверены?\">Фулл клан скиллы. </a> (" + Config.CLAN_SKILLS_PRICE + " " + CLAN_COIN_NAME + ")<br>");
        } else {
            replyMSG.append("<font color=999999>[" + CLAN_POINTS + " клан очков]</font> (" + CLAN_POINTS_PRICE + " " + CLAN_COIN_NAME + ") Для кланов выше 5 ур.<br>");
            replyMSG.append("<font color=999999>[Фулл клан скиллы]</font> (" + Config.CLAN_SKILLS_PRICE + " " + CLAN_COIN_NAME + ") Для кланов выше 5 ур.<br>");
        }

        replyMSG.append("</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    private void clanSetLevel(L2PcInstance player, int level) {
        int price = 99999;
        switch (level) {
            case 6:
                price = CLAN_LVL6;
                break;
            case 7:
                price = CLAN_LVL7;
                break;
            case 8:
                price = CLAN_LVL8;
                break;
        }

        L2ItemInstance coin = player.getInventory().getItemByItemId(CLAN_COIN);
        if (coin == null || coin.getCount() < price) {
            player.sendMessage("Проверьте стоимость");
            return;
        }

        if (!player.destroyItemByItemId("DS clanSetLevel", CLAN_COIN, price, player, true)) {
            player.sendMessage("Проверьте стоимость");
            return;
        }

        player.getClan().changeLevel(level);
        player.sendMessage("Уровень клана увеличен до " + level);
        Log.addDonate(player, "Clan Level: " + level, CLAN_POINTS_PRICE);
    }

    private void clanPoints(L2PcInstance player) {
        L2Clan clan = player.getClan();
        if (clan == null || clan.getLevel() < 5) {
            player.sendMessage("Только для кланов выше 5 уровня");
            return;
        }

        L2ItemInstance coin = player.getInventory().getItemByItemId(CLAN_COIN);
        if (coin == null || coin.getCount() < CLAN_POINTS_PRICE) {
            player.sendMessage("Проверьте стоимость.");
            return;
        }

        if (!player.destroyItemByItemId("DS clanSetLevel", CLAN_COIN, CLAN_POINTS_PRICE, player, true)) {
            player.sendMessage("Проверьте стоимость");
            return;
        }

        clan.addPoints(CLAN_POINTS);
        //clan.setReputationScore(clan.getReputationScore() + CLAN_POINTS, true);
        player.sendMessage("Добавлено " + CLAN_POINTS + " очков, перезайдите, что-бы увидеть изменения.");
        Log.addDonate(player, CLAN_POINTS + " Clan Points", CLAN_POINTS_PRICE);
    }

    private void clanSkills(L2PcInstance player) {
        L2Clan clan = player.getClan();
        if (clan == null || clan.getLevel() < 5) {
            player.sendMessage("Только для кланов выше 5 уровня");
            return;
        }

        if (clan.getSkills().size() == 22) { //license
            //player.sendMessage("Все скиллы куплены!"); // CeT
            //player.sendMessage("Все скиллы куплены!!"); //  joe`jo
            //player.sendMessage("Все скиллы куплены!!!"); //  mat
            //player.sendMessage("Все скиллы куплены!!!!"); //  djimbo
            //player.sendMessage("Все скиллы уже куплены!"); //  CruSade
            //player.sendMessage("Все скиллы уже куплены!!"); //  BooGiMaN
            //player.sendMessage("Все скиллы уже куплены!!!"); //  Gektor
            //player.sendMessage("Все скиллы уже куплены!!!!"); //  b13
            //player.sendMessage("Нет доступных скиллов!"); //  mcwa
            //player.sendMessage("Нет доступных скиллов!!"); //  f1recat
            player.sendMessage("Нет доступных скиллов!!!"); //  DOty
            return;
        }

        L2ItemInstance coin = player.getInventory().getItemByItemId(CLAN_COIN);
        if (coin == null || coin.getCount() < Config.CLAN_SKILLS_PRICE) {
            player.sendMessage("Проверьте стоимость.");
            return;
        }

        if (!player.destroyItemByItemId("DS clanSetLevel", CLAN_COIN, Config.CLAN_SKILLS_PRICE, player, true)) {
            player.sendMessage("Проверьте стоимость");
            return;
        }

        CustomServerData.getInstance().addClanSkills(player, clan);

        player.sendMessage("Добавлены фулл клан скиллы, перезайдите, что-бы увидеть изменения.");
        Log.addDonate(player, "Clan " + clan.getName() + ": Full Skills", Config.CLAN_SKILLS_PRICE);
    }

    /**
     * Продажа аугментации
     *
     */
    // окно приветствия
    private void AugSaleWelcome(L2PcInstance player) {
        if (AUGSALE_TABLE.isEmpty()) {
            player.sendMessage("Сервис не активен");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        replyMSG.append(player.getName() + ", выбери аугментацию:<br>");
        replyMSG.append("<table width=280><tr><td>Аугмент<br></td></tr>");

        for (FastMap.Entry<Integer, Integer> e = AUGSALE_TABLE.head(), end = AUGSALE_TABLE.tail(); (e = e.getNext()) != end;) {
            Integer id = e.getKey(); // No typecast necessary.
            Integer lvl = e.getValue(); // No typecast necessary.
            if (id == null || lvl == null) {
                continue;
            }

            L2Skill augment = SkillTable.getInstance().getInfo(id, 1);
            if (augment == null) {
                continue;
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

            replyMSG.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_augsaleShow " + id + "\"><font color=bef574>" + augName + " (" + type + ":" + lvl + "lvl)</font></a><br></td></tr>");
        }

        replyMSG.append("</table><br>* стоимость любого аугмента:<br1>" + AUGSALE_PRICE + " " + AUGSALE_COIN_NAME + "</body></html>");

        player.setAugSale(0, 0);
        player.setAugSaleItem(0);

        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    //показ аугмента
    private void augSaleShow(L2PcInstance player, int augId) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>");
        replyMSG.append(player.getName() + ", подтверждаешь?:<br>");

        int lvl = AUGSALE_TABLE.get(augId);
        replyMSG.append("<table width=280><tr><td><img src=\"Icon.skill0375\" width=32 height=32></td><td>" + getAugmentSkill(augId, lvl) + "</td></tr></table><br>");

        L2ItemInstance coin = player.getInventory().getItemByItemId(AUGSALE_COIN);
        if (coin != null && coin.getCount() >= AUGSALE_PRICE) {
            player.setAugSale(augId, lvl);
            replyMSG.append("<font color=33CC00>Стоимость: " + AUGSALE_PRICE + " " + AUGSALE_COIN_NAME + "</font><br>");
            replyMSG.append("<button value=\"Продолжить\" action=\"bypass -h npc_" + getObjectId() + "_augSaleItems\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
        } else {
            replyMSG.append("<font color=FF6666>Стоимость: " + AUGSALE_PRICE + " " + AUGSALE_COIN_NAME + "</font><br>");
            replyMSG.append("<font color=999999>[Продолжить]</font>");
        }
        replyMSG.append("<br><br><a action=\"bypass -h npc_" + getObjectId() + "_Augsale\">Вернуться</a><br></body></html>");

        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    // шмотки в инвентаре
    private void AugSaleItems(L2PcInstance player) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>" + getAugmentSkill(player.getAugSaleId(), player.getAugSaleLvl()) + "<br>");
        replyMSG.append("Выбор шмотки:<br>Куда втыкаем?<br><br><table width=300>");

        int objectId = 0;
        String itemName = "";
        int enchantLevel = 0;
        String itemIcon = "";
        int itemType = 0;
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (!item.canBeEnchanted()) {
                continue;
            }

            objectId = item.getObjectId();
            itemName = item.getItem().getName();
            enchantLevel = item.getEnchantLevel();
            itemIcon = item.getItem().getIcon();
            itemType = item.getItem().getType2();

            if (item.canBeAugmented() && !item.isAugmented() && !item.isWear()) {
                replyMSG.append("<tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_augsItem " + objectId + "\">" + itemName + " (+" + enchantLevel + ")</a></td></tr>");
            }
        }

        replyMSG.append("</table>");
        replyMSG.append("<br><br><a action=\"bypass -h npc_" + getObjectId() + "_Augsale\">Вернуться</a><br></body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    // подробная инфа о шмотке
    private void AugsItem(L2PcInstance player, int objectId) {
        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item != null) {
            if (!item.canBeEnchanted()) {
                showError(player, "Ошибка запроса.");
                return;
            }

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>" + getAugmentSkill(player.getAugSaleId(), player.getAugSaleLvl()) + "<br>");

            String itemName = item.getItem().getName();
            int enchantLevel = item.getEnchantLevel();
            String itemIcon = item.getItem().getIcon();

            replyMSG.append("Покупка аугментации:<br>Подтверждаете шмотку?<br>");

            replyMSG.append("<table width=300><tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + " (+" + enchantLevel + ")</font>g<br></td></tr></table><br><br>");
            replyMSG.append("<br>Заточка: <font color=bef574>+" + enchantLevel + "</font><br>");

            replyMSG.append("<button value=\"Продолжить\" action=\"bypass -h npc_" + getObjectId() + "_AugsaleFinish\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
            player.setAugSaleItem(objectId);

            replyMSG.append("<br><br><a action=\"bypass -h npc_" + getObjectId() + "_Augsale\">Вернуться</a><br></body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } else {
            showError(player, "Ошибка запроса.");
            return;
        }
    }

    // финишь. забираем мани и втыкаем лс
    private void AugsaleFinish(L2PcInstance player) {
        L2ItemInstance targetItem = player.getInventory().getItemByObjectId(player.getAugSaleItem());
        if (targetItem != null) {
            if (targetItem.isAugmented()) {
                showVoteErr0r(player, 1, 1);
                return;
            }

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");

            L2ItemInstance coin = player.getInventory().getItemByItemId(AUGSALE_COIN);
            if (coin == null || coin.getCount() < AUGSALE_PRICE) {
                showVoteErr0r(player, 1, 3);
                return;
            }

            //player.getInventory().destroyItemByItemId(coinId, price, true);				
            if (!player.destroyItemByItemId("Ls bue", AUGSALE_COIN, AUGSALE_PRICE, player, true)) {
                showVoteErr0r(player, 1, 3);
                return;
            }
            int augId = player.getAugSaleId();
            int augLevel = player.getAugSaleLvl();

            L2Skill augment = SkillTable.getInstance().getInfo(augId, 1);
            if (augment == null) {
                showVoteErr0r(player, 1, 0);
                return;
            }

            int type = 0;
            if (augment.isActive()) {
                type = 2;
            } else if (augment.isPassive()) {
                type = 3;
            } else {
                type = 1;
            }

            //item2.setAugmentation(new L2Augmentation(item2, augEffId, augId, augLevel, true));
            targetItem.setAugmentation(AugmentationData.getInstance().generateAugmentation(targetItem, augId, augLevel, type));

            replyMSG.append("" + getAugmentSkill(augId, augLevel) + "<br>");
            replyMSG.append("<font color=33CC00>...куплен!<br>");

            //player.sendPacket(new InventoryUpdate().addModifiedItem(item1));
            //player.sendPacket(new InventoryUpdate().addModifiedItem(item2));
            player.sendItems(false);
            player.broadcastUserInfo();

            player.setAugSale(0, 0);
            player.setAugSaleItem(0);

            //player.sendChanges();
            replyMSG.append("</body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);

            Log.addDonate(player, "LS " + augId + ":" + augLevel, AUGSALE_PRICE);
        } else {
            showVoteErr0r(player, 1, 0);
            return;
        }
    }

    /**
     * Шадоу шмотки
     */
    private void itemsChina(L2PcInstance player) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>Китайский магазин<br>");
        replyMSG.append("Товары:<br>");
        replyMSG.append("<table width=260><tr><td></td><td></td></tr>");

        ChinaItem ds = null;
        FastMap<Integer, ChinaItem> chinaShop = CustomServerData.getInstance().getChinaShop();
        for (FastMap.Entry<Integer, ChinaItem> e = chinaShop.head(), end = chinaShop.tail(); (e = e.getNext()) != end;) {
            Integer id = e.getKey(); // No typecast necessary.
            ds = e.getValue(); // No typecast necessary.

            if (ds == null) {
                continue;
            }

            L2Item china = ItemTable.getInstance().getTemplate(id);
            if (china == null) {
                continue;
            }

            replyMSG.append("<tr><td><img src=\"" + china.getIcon() + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_chinaShow " + id + "\"><font color=99FF66>" + ds.name + "</font></a><br1><font color=336633>Стоимость: " + ds.price + " CoL</font><br></td></tr>");
        }
        //
        //
        replyMSG.append("</table><br><br>");

        replyMSG.append("<br><br></body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    private void chinaShow(L2PcInstance player, int itemId) {
        L2Item china = ItemTable.getInstance().getTemplate(itemId);
        if (china == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        ChinaItem ds = CustomServerData.getInstance().getChinaItem(itemId);
        if (ds == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body>KuTаuсkuu Mаrа3uH*<br>");

        replyMSG.append("<table width=250><tr><td align=right><img src=\"" + china.getIcon() + "\" width=32 height=32></td><td><font color=33FFFF>" + ds.name + "</font></td></tr></table><br><br>");
        replyMSG.append("<font color=336699>Инфо: </font><font color=3399CC> " + ds.info + "</font><br>");
        replyMSG.append("<font color=336699>Длительность: </font><font color=3399CC> " + ds.days + " часов</font><br>");
        L2ItemInstance coin = player.getInventory().getItemByItemId(ds.coin);
        if (coin != null && coin.getCount() >= ds.price) {
            replyMSG.append("<font color=33CC00>Стоимость:<br1> " + ds.price + " Coin Of Luck</font><br>");
            replyMSG.append("<button value=\"Купить\" action=\"bypass -h npc_" + getObjectId() + "_bueChina " + itemId + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
        } else {
            replyMSG.append("<font color=FF6666>Стоимость:<br1> " + ds.price + " Coin Of Luck</font><br>");
            replyMSG.append("<font color=999999>[Купить]</font>");
        }
        replyMSG.append("<br><font color=CC33CC>*Через " + ds.days + " часов шмотка сломается!<br1>Отсчет начинается при надевании шмотки.<br1>Просто так валяясь в инвентаре ничего не сломается.<br1>Made in China.</font><br></body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);
    }

    private void bueChina(L2PcInstance player, int itemId) {
        L2Item china = ItemTable.getInstance().getTemplate(itemId);
        if (china == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        ChinaItem ds = CustomServerData.getInstance().getChinaItem(itemId);
        if (ds == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        L2ItemInstance coins = player.getInventory().getItemByItemId(ds.coin);
        if (coins == null || coins.getCount() < ds.price) {
            showError(player, "Ошибка запроса.");
            return;
        }
        player.destroyItemByItemId("China", ds.coin, ds.price, player, true);

        L2ItemInstance item = ItemTable.getInstance().createItem("China", itemId, 1, player, null);
        item.setMana((int) TimeUnit.HOURS.toMinutes(ds.days));
        player.getInventory().addItem("Enchantt", item, player, null);

        player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(item.getItemId()));
        player.sendItems(true);
        player.sendChanges();
        player.broadcastUserInfo();
    }

    /**
     * SOB
     *
     */
    private void bueSOB(L2PcInstance player, int period) {
        int price = 99999;
        switch (period) {
            case 1:
                price = SOB_PRICE_ONE;
                break;
            case 2:
                price = SOB_PRICE_TWO;
                break;
        }

        L2ItemInstance coins = player.getInventory().getItemByItemId(SOB_COIN);
        if (coins == null || coins.getCount() < price) {
            showError(player, "Не достаточно " + SOB_COIN_NAME + ".");
            return;
        }

        int skillId = Config.SOB_ID;
        if (skillId == 0 || player.getClassId().getId() < 88) {
            showError(player, "Только для 3й профы.");
            return;
        }

        if (skillId == 1) {
            switch (player.getClassId().getId()) {
                case 88:
                case 89:
                case 92:
                case 93:
                case 101:
                case 102:
                case 113:
                case 114:
                case 108:
                case 109:
                    skillId = 7077; // файтер
                    break;
                case 94:
                case 95:
                case 103:
                case 110:
                    skillId = 7078; // маг
                    break;
                case 96:
                case 97:
                case 98:
                case 104:
                case 105:
                case 100:
                case 107:
                case 111:
                case 112:
                case 115:
                case 116:
                    skillId = 7079; // суппорт
                    break;
                case 90:
                case 91:
                case 99:
                case 106:
                case 117:
                case 118:
                    skillId = 7080; // танк
                    break;
            }
        }

        if (player.getKnownSkill(skillId) != null) {
            showError(player, "У вас уже есть Skill OF Balance.");
            return;
        }

        player.destroyItemByItemId("SOB", SOB_COIN, price, player, true);

        /*player.addSkill(SkillTable.getInstance().getInfo(Config.SOB_ID, period), false);
         if (period == 2 && Config.SOB_NPC > 0)
         player.addCubic(Config.SOB_NPC, 80);
        
         player.sendSkillList();
         player.sendChanges();
         player.broadcastUserInfo();*/
        Log.addDonate(player, "SoB [" + period + "]", price);

        long expire = TimeUnit.DAYS.toMillis(15); //(14 * 24 * 60 * 60000);//TimeUnit.DAYS.toMillis(14);
        if (period == 2) {
            expire *= 2;
        }

        player.addDonateSkill(player.getClassId().getId(), skillId, period, (System.currentTimeMillis() + expire));
        /*Connect con = null;
         PreparedStatement statement = null;
         try
         {
         con = L2DatabaseFactory.get();
         statement = con.prepareStatement("REPLACE INTO `z_balance_skill` (`char_id`,`class_id`,`skill_id`,`lvl`,`expire`) VALUES (?,?,?,?,?)");
         statement.setInt(1, player.getObjectId());
         statement.setInt(2, player.getClassId().getId());
         statement.setInt(3, skillId);
         statement.setInt(4, period);
         statement.setLong(5, System.currentTimeMillis() + expire);
         statement.execute();
         }
         catch(final Exception e)
         {
         _log.warning("Donate: Log.addDonate() error: " + e);
         }
         finally
         {
         Close.CS(con, statement);
         }*/
        player.sendMessage("Куплен Skill Of Balance, перезайдите!");
    }

    /**
     * Донейт шоп
     *
     */
    private void donateShop(L2PcInstance player) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder htm = new TextBuilder();
        htm.append("<html><body><table width=260><tr><td><font color=LEVEL>Уникальный магазин:</font></td></tr></table><br>");
        htm.append("<table width=260><tr><td></td><td></td></tr>");

        L2Item item = null;
        String count = "";
        DonateItem di = null;
        ItemTable it = ItemTable.getInstance();
        FastTable<DonateItem> donShop = CustomServerData.getInstance().getDonateShop();
        for (int i = 0, n = donShop.size(); i < n; i++) {
            di = donShop.get(i);
            if (di == null) {
                continue;
            }

            item = it.getTemplate(di.itemId);
            if (item == null) {
                continue;
            }

            if (di.itemCount > 1) {
                count = "(" + di.itemCount + ")";
            }

            htm.append("<tr><td><img src=" + item.getIcon() + " width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_dShopShow " + i + "\"><font color=99FF66>" + item.getName() + "" + count + "</font></a><br1><font color=336633>Стоимость: " + di.priceCount + " " + di.priceName + "</font></td></tr>");
            htm.append("<tr><td><br></td><td></td></tr>");
            count = "";
        }
        htm.append("</table><br><br>");
        htm.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></body></html>");
        reply.setHtml(htm.toString());
        player.sendPacket(reply);
    }

    private void donateShopShow(L2PcInstance player, int saleId) {
        DonateItem di = CustomServerData.getInstance().getDonateItem(saleId);
        if (di == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        L2Item item = ItemTable.getInstance().getTemplate(di.itemId);
        if (item == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        String count = "";
        if (di.itemCount > 1) {
            count = "(" + di.itemCount + ")";
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder htm = new TextBuilder();

        htm.append("<html><body><font color=LEVEL>Уникальный магазин:<br>Просмотр товара:</font><br1>");
        htm.append("<table width=250><tr><td align=right><img src=" + item.getIcon() + " width=32 height=32></td><td><font color=33FFFF>" + item.getName() + "" + count + "</font></td></tr></table><br><br>");
        htm.append("<font color=336699>Дополнительная информация:</font><br1><font color=3399CC>" + di.itemInfoRu + "</font><br>");
        htm.append("<font color=336699>Описание:</font><br1>");
        htm.append("<font color=3399CC>" + di.itemInfoDesc + "</font><br>");
        htm.append("<font color=336699>Стоимость:</font><br1>");
        htm.append("<font color=3399CC>" + di.priceCount + " " + di.priceName + "</font><br><br>");

        L2ItemInstance coins = player.getInventory().getItemByItemId(di.priceId);
        if (coins == null || coins.getCount() < di.priceCount) {
            htm.append("<font color=666699>[Купить]</font><br><br>");
        } else {
            htm.append("<button value=\"Купить\" action=\"bypass -h npc_" + getObjectId() + "_dShopBue " + saleId + "\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\" msg=\"Покупаем " + item.getName() + "" + count + " за " + di.priceCount + " " + di.priceName + "?\"><br><br>");
        }

        htm.append("<a action=\"bypass -h npc_" + getObjectId() + "_donateShop\">Вернуться.</a></body></html>");
        reply.setHtml(htm.toString());
        player.sendPacket(reply);
    }

    private void donateShopBue(L2PcInstance player, int saleId) {
        DonateItem di = CustomServerData.getInstance().getDonateItem(saleId);
        if (di == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        L2Item item = ItemTable.getInstance().getTemplate(di.itemId);
        if (item == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        L2ItemInstance coins = player.getInventory().getItemByItemId(di.priceId);
        if (coins == null || coins.getCount() < di.priceCount) {
            showError(player, "Ошибка запроса.");
            return;
        }
        player.destroyItemByItemId("Donate Shop", di.priceId, di.priceCount, player, true);
        String count = "";
        if (di.itemCount > 1) {
            count = "(" + di.itemCount + ")";
        }
        Log.addDonate(player, "Donate Shop: " + item.getName() + "" + count + "", di.priceCount);

        player.addItem("DonateShop", di.itemId, di.itemCount, player, true);
        /*L2ItemInstance newItem = ItemTable.getInstance().createItem("Donate Shop", di.itemId, di.itemCount, player, null);
         player.getInventory().addItem("Donate Shop", newItem, player, null);
        
         SystemMessage smsg = SystemMessage.id(SystemMessageId.EARNED_ITEM);
         smsg.addItemName(newItem.getItemId());
         player.sendPacket(smsg);*/

        //player.sendItems(true);
        //player.sendChanges();
        //player.broadcastUserInfo();
    }

    /**
     * *
     * Продажа скиллов data/donate_skills.xml
     *
     */
    private void donateSkillShop(L2PcInstance player) {
        int store = -1;
        if (Config.DS_ALL_SUBS) {
            store = 0;
        } else {
            store = player.getClassId().getId();
        }

        if (_donateSkills.get(store) == null) {
            showError(player, "Для вашего класса нет доступных скиллов.");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder htm = new TextBuilder();
        htm.append("<html><body><table width=260><tr><td><font color=LEVEL>Уникальный магазин скиллов:</font></td></tr></table><br>");
        htm.append("<table width=260><tr><td></td><td></td></tr>");

        DonateSkill di = null;
        SkillTable st = SkillTable.getInstance();
        for (FastMap.Entry<Integer, FastTable<DonateSkill>> e = _donateSkills.head(), end = _donateSkills.tail(); (e = e.getNext()) != end;) {
            Integer classId = e.getKey(); // No typecast necessary.
            FastTable<DonateSkill> skills = e.getValue(); // No typecast necessary.
            if (classId == null || skills == null) {
                continue;
            }

            if (skills.isEmpty()) {
                continue;
            }

            for (int i = 0, n = skills.size(); i < n; i++) {
                di = skills.get(i);
                if (di == null) {
                    continue;
                }

                L2Skill skill = st.getInfo(di.id, di.lvl);
                if (skill == null) {
                    continue;
                }

                if (player.getKnownSkill(di.id) != null
                        && player.getKnownSkill(di.id).getLevel() >= di.lvl) {
                    continue;
                }

                htm.append("<tr><td><img src=" + di.icon + " width=32 height=32></td><td><a action=\"bypass -h npc_" + getObjectId() + "_dsShopShow " + i + "\"><font color=99FF66>" + skill.getName() + " (" + di.lvl + " ур.)</font></a><br1><font color=336633>Стоимость: " + di.priceCount + " " + di.priceName + "</font></td></tr>");
                htm.append("<tr><td><br></td><td></td></tr>");
            }
        }

        htm.append("</table><br><br>");
        htm.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></body></html>");
        reply.setHtml(htm.toString());
        player.sendPacket(reply);
    }

    private void donateSkillShopShow(L2PcInstance player, int saleId) {
        int store = -1;
        if (Config.DS_ALL_SUBS) {
            store = 0;
        } else {
            store = player.getClassId().getId();
        }

        if (_donateSkills.get(store) == null) {
            showError(player, "Для вашего класса нет доступных скиллов.");
            return;
        }

        DonateSkill di = _donateSkills.get(store).get(saleId);
        if (di == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        L2Skill skill = SkillTable.getInstance().getInfo(di.id, di.lvl);
        if (skill == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder htm = new TextBuilder();

        htm.append("<html><body><font color=LEVEL>Уникальный магазин скиллов:<br>Просмотр скилла:</font><br1>");
        htm.append("<table width=250><tr><td align=right><img src=" + di.icon + " width=32 height=32></td><td><font color=33FFFF>" + skill.getName() + " (" + di.lvl + " ур.)</font></td></tr></table><br><br>");
        htm.append("<font color=336699>Описание:</font><br1>");
        htm.append("<font color=3399CC>" + di.info + "</font><br>");

        htm.append("<font color=336699>Длительность:</font><br1>");
        if (di.expire < 0) {
            htm.append("<font color=3399CC>Бесконечный.</font><br>");
        } else if (di.expire == 0) {
            htm.append("<font color=3399CC>До конца месяца.</font><br>");
        } else {
            htm.append("<font color=3399CC>" + di.expire + " дней.</font><br>");
        }

        htm.append("<font color=336699>Стоимость:</font><br1>");
        htm.append("<font color=3399CC>" + di.priceCount + " " + di.priceName + "</font><br><br>");

        L2ItemInstance coins = player.getInventory().getItemByItemId(di.priceId);
        if (coins == null || coins.getCount() < di.priceCount) {
            htm.append("<font color=666699>[Купить]</font><br><br>");
        } else {
            htm.append("<button value=\"Купить\" action=\"bypass -h npc_" + getObjectId() + "_dsShopBue " + saleId + "\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\" msg=\"Покупаем " + skill.getName() + " (" + di.lvl + " ур.) за " + di.priceCount + " " + di.priceName + "?\"><br><br>");
        }

        htm.append("<a action=\"bypass -h npc_" + getObjectId() + "_donateSkillsShop\">Вернуться.</a></body></html>");
        reply.setHtml(htm.toString());
        player.sendPacket(reply);
    }

    private void donateSkillShopBue(L2PcInstance player, int saleId) {
        int store = -1;
        if (Config.DS_ALL_SUBS) {
            store = 0;
        } else {
            store = player.getClassId().getId();
        }
        if (_donateSkills.get(store) == null) {
            showError(player, "Для вашего класса нет доступных скиллов.");
            return;
        }

        DonateSkill di = _donateSkills.get(store).get(saleId);
        if (di == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        L2Skill skill = SkillTable.getInstance().getInfo(di.id, di.lvl);
        if (skill == null) {
            showError(player, "Ошибка запроса.");
            return;
        }

        L2ItemInstance coins = player.getInventory().getItemByItemId(di.priceId);
        if (coins == null || coins.getCount() < di.priceCount) {
            showError(player, "Ошибка запроса.");
            return;
        }
        player.destroyItemByItemId("Donate Skill Shop", di.priceId, di.priceCount, player, true);
        Log.addDonate(player, "Donate Skill Shop: " + skill.getName() + "(" + di.lvl + " lvl)", di.priceCount);

        long expire = 0;
        if (di.expire < 0) {
            expire = -1;
        } else if (di.expire == 0) {
            Calendar calendar = Calendar.getInstance();
            int lastDate = calendar.getActualMaximum(Calendar.DATE);
            calendar.set(Calendar.DATE, lastDate);
            //int lastDay = calendar.get(Calendar.DAY_OF_WEEK);
            expire = calendar.getTimeInMillis();
        } else {
            expire = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(di.expire); //(14 * 24 * 60 * 60000);//TimeUnit.DAYS.toMillis(14);
        }
        player.addSkill(skill, false);
        player.addDonateSkill(store, di.id, di.lvl, expire);
        player.sendHtmlMessage("Уникальный магазин скиллов:", "Вы приобрели скилл: <br> <font color=33FFFF>" + skill.getName() + " (" + di.lvl + " ур.)</font>");
    }

    /*
     Премиум
     */
    private void addPremium(L2PcInstance player, int days) {
        if (player.isPremium()) { //license
            //showError(player, "Вы уже премиум."); // CeT
            //showError(player, "Вы уже премиум!"); //  joe`jo
            //showError(player, "Вы уже премиум!!"); //  mat
            //showError(player, "Вы уже премиум!!!"); //  djimbo
            //showError(player, "Вы и так премиум!"); //  CruSade
            //showError(player, "Вы и так премиум!!"); //  BooGiMaN
            //showError(player, "Вы и так премиум!!!"); //  Gektor
            //showError(player, "Вы и так премиум!!!!"); //  b13
            //showError(player, "Премиум уже куплен!"); //  mcwa
            //showError(player, "Премиум уже куплен!!"); //  f1recat
            //showError(player, "Премиум уже куплен!!!"); //  TruOverLike
            showError(player, "Премиум уже куплен!!!!"); //  DOty
            return;
        }

        if (days <= 0) {
            return;
        }
        Integer price = PREMIUM_DAY_PRICES.get(days);
        if (price == null) {
            price = Config.PREMIUM_PRICE * days;
        }

        L2ItemInstance coins = player.getInventory().getItemByItemId(Config.PREMIUM_COIN);
        if (coins == null || coins.getCount() < price) {
            showError(player, "Стоимость премиум " + price + " " + Config.PREMIUM_COINNAME + ".");
            return;
        }
        player.destroyItemByItemId("Donate Shop", Config.PREMIUM_COIN, price, player, true);

        player.storePremium(days);
        Log.addDonate(player, "Premium, " + days + " days.", Config.PREMIUM_PRICE);
    }

    private void addNoble(L2PcInstance player) {
        if (player.isNoble()) { //license
            //showError(player, "Вы уже ноблесс."); // CeT
            //showError(player, "Вы уже ноблесс!"); //  joe`jo
            //showError(player, "Вы уже ноблесс!!"); //  mat
            //showError(player, "Вы уже ноблесс!!!"); //  djimbo
            //showError(player, "Вы и так ноблесс!"); //  CruSade
            //showError(player, "Вы и так ноблесс!!"); //  BooGiMaN
            //showError(player, "Вы и так ноблесс!!!"); //  Gektor
            //showError(player, "Вы и так ноблесс!!!!"); //  b13
            //showError(player, "Ноблесс уже куплен!"); //  mcwa
            //showError(player, "Ноблесс уже куплен!!"); //  f1recat
            //showError(player, "Ноблесс уже куплен!!!"); //  TruOverLike
            showError(player, "Ноблесс уже куплен!!!!"); //  DOty
            return;
        }

        L2ItemInstance coins = player.getInventory().getItemByItemId(Config.SNOBLE_COIN);
        if (coins == null || coins.getCount() < Config.SNOBLE_PRICE) {
            showError(player, "Стоимость ноблесса " + Config.SNOBLE_PRICE + " " + Config.SNOBLE_COIN_NAME + ".");
            return;
        }
        player.destroyItemByItemId("Donate Shop", Config.SNOBLE_COIN, Config.SNOBLE_PRICE, player, true);

        player.setNoble(true);
        player.addItem("rewardNoble", 7694, 1, this, true);
        player.sendUserPacket(new PlaySound("ItemSound.quest_finish"));

        if (!Config.ACADEMY_CLASSIC) {
            player.rewardAcademy(0);
        }
        Log.addDonate(player, "Noblesse.", Config.SNOBLE_PRICE);
    }

    private void uniqSkillShopBue(L2PcInstance player, int id) {
        Integer level = Config.UNIQ_SKILLS.get(id);
        if (level == null) {
            showError(player, "Данный скилл нельзя получить.");
            return;
        }

        if (player.getLevel() < level) {
            showError(player, "Данный скилл доступен на " + level + " уровне.");
            return;
        }

        for (int lid : Config.UNIQ_SKILLS.keySet()) {
            if (player.getKnownSkill(lid) != null) {
                showError(player, "Вы уже выучили уникальный скилл; попробуйте на саб-класс.");
                return;
            }
        }

        L2Skill skill = SkillTable.getInstance().getInfo(id, 1);
        if (skill == null) {
            showError(player, "Данный скилл отсутствует.");
            return;
        }

        player.addSkill(skill, true);
        player.sendHtmlMessage("Опа", "Вы изучили скилл: <br> <font color=33FFFF>" + skill.getName() + "</font>");
    }

    /**
     * *
     ** Статистика *
     */
    private void statHome(L2PcInstance player) {
        TextBuilder htm = new TextBuilder();
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());

        htm.append("<html><body><center><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br1>");
        htm.append("<table width=290> <tr><td><font color=0099CC>Лидеры сервера</font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statPvp 1\">Top PvP</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statPk 1\">Top Pk</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statClans 1\">Кланы</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statCastles\">Замки</a></font></td>");
        htm.append("</tr></table><br><img src=\"sek.cbui175\" width=150 height=3><br><table width=310><tr>");
        htm.append("<td align=center><font color=0099CC>Top 10 PvP</font></td><td align=center><font color=0099CC>Top 10 Pk</font></td>");
        htm.append("</tr><tr><td valign=top>");
        htm.append(CustomServerData.getInstance().getStatHome());
        htm.append("</td></tr></table><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><a action=\"bypass -h npc_%objectId%_Chat 0\"><font color=666633>Назад</font></a></body></html>");

        reply.setHtml(htm.toString());
        player.sendPacket(reply);

        htm.clear();
        htm = null;
    }

    private void statShowPvp(L2PcInstance player, int page) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder htm = new TextBuilder();

        htm.append("<html><body><center><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br1>");
        htm.append("<table width=290> <tr><td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statHome\">Лидеры сервера</a></font></td>");
        htm.append("<td><font color=0099CC>Top Pvp</font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statPk 1\">Top Pk</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statClans 1\">Кланы</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statCastles\">Замки</a></font></td>");
        htm.append("</tr></table><br><img src=\"sek.cbui175\" width=150 height=3><br>");
        htm.append("<font color=999966><table width=310><tr><td>#</td><td>Ник</td><td>Клан</td><td>Онлайн</td><td>Pvp</td></tr>");

        FastTable<StatPlayer> pvp = CustomServerData.getInstance().getStatPvp();
        L2World world = L2World.getInstance();

        int count = 0;
        String online = "<font color=006600>Online</font>";
        int start = (page - 1) * _statLimit;
        int stop = start + _statLimit;
        if (stop > pvp.size()) {
            stop = pvp.size() - 1;
        }
        int pages = (pvp.size() / _statLimit) + 1;
        for (int i = start, n = stop; i < n; i++) {
            StatPlayer pc = pvp.get(i);;
            if (pc == null) {
                continue;
            }

            if (world.getPlayer(pc.id) == null) {
                online = "<font color=330033>Offline</font>";
            }

            htm.append("<tr><td>" + (i + 1) + "</td><td><font color=CCCC33>" + pc.name + "</td><td>" + pc.clan + "</font></td><td><font color=006600>" + online + "</font></td><td><font color=CCCC33>" + pc.kills + "</font></td></tr>");
            count++;
        }

        htm.append("</table></font>");
        if (pages > 2) {
            htm.append(sortPvp(page, pages));
        }

        htm.append("</center></body></html>");

        reply.setHtml(htm.toString());
        player.sendPacket(reply);
        htm.clear();
        htm = null;
    }

    private String sortPvp(int page, int pages) {
        TextBuilder text = new TextBuilder("<br>Страницы:<br1><table width=300><tr>");
        int step = 1;
        int s = page - 3;
        int f = page + 3;
        if (page < _statSortLimit && s < _statSortLimit) {
            s = 1;
        }
        if (page >= _statSortLimit) {
            text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statPvp " + s + "\"> ... </a></td>");
        }

        for (int i = s; i < (pages + 1); i++) {
            int al = i + 1;
            if (i == page) {
                text.append("<td>" + i + "</td>");
            } else {
                if (al <= pages) {
                    text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statPvp " + i + "\">" + i + "</a></td>");
                }
            }
            if (step == _statSortLimit && f < pages) {
                if (al < pages) {
                    text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statPvp " + al + "\"> ... </a></td>");
                }
                break;
            }
            step++;
        }
        text.append("</tr></table><br>");
        String htmltext = text.toString();
        text.clear();
        text = null;
        return htmltext;
    }
    private static int _statLimit = 11;
    private static int _statSortLimit = 9;

    private void statShowPk(L2PcInstance player, int page) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder htm = new TextBuilder();

        htm.append("<html><body><center><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br1>");
        htm.append("<table width=290> <tr><td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statHome\">Лидеры сервера</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statPvp 1\">Top Pvp</a></font></td>");
        htm.append("<td><font color=0099CC>Top Pk</font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statClans 1\">Кланы</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statCastles\">Замки</a></font></td>");
        htm.append("</tr></table><br><img src=\"sek.cbui175\" width=150 height=3><br>");
        htm.append("<font color=999966><table width=310><tr><td>#</td><td>Ник</td><td>Клан</td><td>Онлайн</td><td>Pvp</td></tr>");

        FastTable<StatPlayer> pk = CustomServerData.getInstance().getStatPk();
        L2World world = L2World.getInstance();

        int count = 0;
        String online = "<font color=006600>Online</font>";
        int start = (page - 1) * _statLimit;
        int stop = start + _statLimit;
        if (stop > pk.size()) {
            stop = pk.size() - 1;
        }
        int pages = (pk.size() / _statLimit) + 1;
        for (int i = start, n = stop; i < n; i++) {
            StatPlayer pc = pk.get(i);
            if (pc == null) {
                continue;
            }

            if (world.getPlayer(pc.id) == null) {
                online = "<font color=330033>Offline</font>";
            }

            htm.append("<tr><td>" + (i + 1) + "</td><td><font color=CCCC33>" + pc.name + "</td><td>" + pc.clan + "</font></td><td><font color=006600>" + online + "</font></td><td><font color=CCCC33>" + pc.kills + "</font></td></tr>");
            count++;
        }

        htm.append("</table></font>");
        if (pages > 2) {
            htm.append(sortPk(page, pages));
        }

        htm.append("</body></html>");

        reply.setHtml(htm.toString());
        player.sendPacket(reply);
        htm.clear();
        htm = null;
    }

    private String sortPk(int page, int pages) {
        TextBuilder text = new TextBuilder("<br>Страницы:<br1><table width=300><tr>");
        int step = 1;
        int s = page - 3;
        int f = page + 3;
        if (page < _statSortLimit && s < _statSortLimit) {
            s = 1;
        }
        if (page >= _statSortLimit) {
            text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statPk " + s + "\"> ... </a></td>");
        }

        for (int i = s; i < (pages + 1); i++) {
            int al = i + 1;
            if (i == page) {
                text.append("<td>" + i + "</td>");
            } else {
                if (al <= pages) {
                    text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statPk " + i + "\">" + i + "</a></td>");
                }
            }
            if (step == _statSortLimit && f < pages) {
                if (al < pages) {
                    text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statPk " + al + "\"> ... </a></td>");
                }
                break;
            }
            step++;
        }
        text.append("</tr></table><br>");
        String htmltext = text.toString();
        text.clear();
        text = null;
        return htmltext;
    }

    private void statClans(L2PcInstance player, int page) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder htm = new TextBuilder();

        htm.append("<html><body><center><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br1>");
        htm.append("<table width=290> <tr><td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statHome\">Лидеры сервера</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statPvp 1\">Top Pvp</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statPk 1\">Top Pk</a></font></td>");
        htm.append("<td><font color=0099CC>Кланы</font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statCastles\">Замки</a></font></td>");
        htm.append("</tr></table><br><img src=\"sek.cbui175\" width=150 height=3><br>");
        htm.append("<font color=999966><table width=310><tr><td>#</td><td>Название</td><td>Ур.</td><td>Лидер</td><td>Очки</td><td>Людей</td></tr>");

        FastTable<StatClan> clans = CustomServerData.getInstance().getStatClans();

        int count = 0;
        int start = (page - 1) * _statLimit;
        int stop = start + _statLimit;
        if (stop > clans.size()) {
            stop = clans.size() - 1;
        }
        int pages = (clans.size() / _statLimit) + 1;
        for (int i = start, n = stop; i < n; i++) {
            StatClan clan = clans.get(i);
            if (clan == null) {
                continue;
            }

            htm.append("<tr><td>" + (i + 1) + "</td><td><font color=CCCC33>" + clan.name + "</td><td>" + clan.level + "</td><td>" + clan.owner + "</td><td>" + clan.rep + "</td><td>" + clan.count + "</td></tr>");
            count++;
        }

        htm.append("</table></font>");
        if (pages > 2) {
            htm.append(sortClans(page, pages));
        }

        htm.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\"><font color=666633>Назад</font></a></body></html>");

        reply.setHtml(htm.toString());
        player.sendPacket(reply);
        htm.clear();
        htm = null;
    }

    private String sortClans(int page, int pages) {
        TextBuilder text = new TextBuilder("<br>Страницы:<br1><table width=300><tr>");
        int step = 1;
        int s = page - 3;
        int f = page + 3;
        if (page < _statSortLimit && s < _statSortLimit) {
            s = 1;
        }
        if (page >= _statSortLimit) {
            text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statClans " + s + "\"> ... </a></td>");
        }

        for (int i = s; i < (pages + 1); i++) {
            int al = i + 1;
            if (i == page) {
                text.append("<td>" + i + "</td>");
            } else {
                if (al <= pages) {
                    text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statClans " + i + "\">" + i + "</a></td>");
                }
            }
            if (step == _statSortLimit && f < pages) {
                if (al < pages) {
                    text.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_statClans " + al + "\"> ... </a></td>");
                }
                break;
            }
            step++;
        }
        text.append("</tr></table><br>");
        String htmltext = text.toString();
        text.clear();
        text = null;
        return htmltext;
    }

    private void statCastles(L2PcInstance player) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder htm = new TextBuilder();

        htm.append("<html><body><center><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br1>");
        htm.append("<table width=290> <tr><td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statHome\">Лидеры сервера</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statPvp 1\">Top Pvp</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statPk 1\">Top Pk</a></font></td>");
        htm.append("<td><font color=993366><a action=\"bypass -h npc_" + getObjectId() + "_statClans 1\">Кланы</a></font></td>");
        htm.append("<td><font color=0099CC>Замки</font></td>");
        htm.append("</tr></table><br><img src=\"sek.cbui175\" width=150 height=3><br>");
        htm.append("<font color=999966><table width=310><tr><td>Замок</td><td>Владелец</td><td>Дата осады</td></tr>");

        for (StatCastle castle : CustomServerData.getInstance().getStatCastles()) {
            if (castle == null) {
                continue;
            }

            htm.append("<tr><td><font color=CCCC33>" + castle.name + "</td><td>" + castle.owner + "</td><td>" + castle.siege + "</td></tr>");
        }

        htm.append("</table></font>");

        htm.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\"><font color=666633>Назад</font></a></body></html>");

        reply.setHtml(htm.toString());
        player.sendPacket(reply);
        htm.clear();
        htm = null;
    }

    /*
     * Дампы таблиц и хтмл, чтоб не потерять
     *
     CREATE TABLE `z_stock_items` (
     `id` bigint(9) NOT NULL auto_increment,
     `itemId` smallint(5) unsigned NOT NULL default '0',
     `enchant` smallint(5) unsigned NOT NULL default '0',
     `augment` int(11) default '0',
     `augAttr` int(11) default '0',
     `augLvl` int(11) default '0',
     `price` int(11) default '0',
     `ownerId` int(10) unsigned NOT NULL default '0',
     `shadow` tinyint(1) default 0,
     PRIMARY KEY  (`id`)
     ) ENGINE=MyISAM DEFAULT CHARSET=utf8;
    
     CREATE TABLE `z_stock_accounts` (
     `charId` int(10) NOT NULL default '0',
     `balance` bigint(20) unsigned NOT NULL default '0',
     `ban` tinyint(1) default 0,
     PRIMARY KEY  (`charId`)
     ) ENGINE=MyISAM DEFAULT CHARSET=utf8;
    
     CREATE TABLE `z_stock_logs` (
     `date` varchar(50) default '1970',
     `charId` int(10) unsigned NOT NULL default '0',
     `itemId` smallint(5) unsigned NOT NULL default '0',
     `enchant` smallint(5) unsigned NOT NULL default '0',
     `augment` int(11) default '0',
     `augLvl` int(11) default '0',
     `price` int(11) default '0',
     `ownerId` int(10) unsigned NOT NULL default '0',
     `shadow` tinyint(1) default 0
     ) ENGINE=MyISAM DEFAULT CHARSET=utf8;
    
     CREATE TABLE `z_post_in` (
     `id` bigint(9) NOT NULL auto_increment,
     `tema` varchar(255) NOT NULL default 'No theme',
     `text` varchar(11255) NOT NULL default '',
     `from` varchar(255) NOT NULL default '',
     `to` varchar(255) NOT NULL default '',
     `type` varchar(255) NOT NULL default '',
     `date` date NOT NULL,
     `time` time NOT NULL,
     PRIMARY KEY  (`id`)
     ) ENGINE=MyISAM DEFAULT CHARSET=utf8;
    
     CREATE TABLE `z_post_out` (
     `id` bigint(9) NOT NULL auto_increment,
     `tema` varchar(255) NOT NULL default 'No theme',
     `text` varchar(11255) NOT NULL default '',
     `from` varchar(255) NOT NULL default '',
     `to` varchar(255) NOT NULL default '',
     `type` varchar(255) NOT NULL default '',
     `date` date NOT NULL,
     `time` time NOT NULL,
     PRIMARY KEY  (`id`)
     ) ENGINE=MyISAM DEFAULT CHARSET=utf8;
    
     UPDATE `npc` SET `type`='L2Donate' WHERE (`id`='80007');
     UPDATE `npc` SET `type`='L2Donate' WHERE (`id`='99999');
    
    
     <html><title>Донат.</title>
     <body>
     <a action="bypass -h npc_" + getObjectId() + "_StockExchange 1">Биржа предметов</a><br>
     </body>
     </html>
     */
}
