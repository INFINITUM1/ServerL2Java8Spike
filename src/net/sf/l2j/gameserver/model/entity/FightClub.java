package net.sf.l2j.gameserver.model.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExMailArrived;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.util.GiveItem;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

import javolution.util.FastTable;
import javolution.util.FastList;
import javolution.text.TextBuilder;

public class FightClub {

    private static final Logger _log = AbstractLogger.getLogger(FightClub.class.getName());
    private static Map<Integer, Fighter> _fcPlayers = new ConcurrentHashMap<Integer, Fighter>();
    private static Map<Integer, Contest> _fcFights = new ConcurrentHashMap<Integer, Contest>();
    //private static ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private static FastTable<FightClubArena> _arenas = new FastTable<FightClubArena>();
    private static Location _tpLoc = new Location(116530, 76141, -2730); // 116530, 76141, -2730
    private static long _maxTime = (5 * 60000);

    public static class Fighter {

        public int obj_id;
        public FcItem item;
        public String pass;
        public int active;

        public Fighter(int obj_id, FcItem item, String pass, int shadow) {
            this.obj_id = obj_id;
            this.item = item;
            this.pass = pass;
            this.active = shadow;
        }
    }

    public static class FcItem {

        public int id;
        public int count;
        public int enchant;
        public int aug_hex;
        public int aug_skillId;
        public int aug_lvl;
        public String name;
        public String icon;

        public FcItem(int id, int count, int enchant, int aug_hex, int aug_skillId, int aug_lvl, String name, String icon) {
            this.id = id;
            this.count = count;
            this.enchant = enchant;
            this.aug_hex = aug_hex;
            this.aug_skillId = aug_skillId;
            this.aug_lvl = aug_lvl;
            this.name = name;
            this.icon = icon;
        }
    }

    public static class Contest {

        public Fighter fighter1;
        public Fighter fighter2;
        public FightClubArena stadium;

        public Contest(Fighter fighter1, Fighter fighter2, FightClubArena stadium) {
            this.fighter1 = fighter1;
            this.fighter2 = fighter2;
            this.stadium = stadium;
        }
    }

    public static void showFighters(L2PcInstance player, int npcObj) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(npcObj);
        TextBuilder tb = new TextBuilder("<html><body>Бойцовский клуб.<br>Ожидающие боя:");
        if (_fcPlayers.isEmpty()) {
            tb.append("Нет боев.");
            tb.append("</body></html>");
            reply.setHtml(tb.toString());
            player.sendPacket(reply);
            tb.clear();
            tb = null;
            return;
        }

        tb.append("<br><br><table width=300>");
        for (Integer id : _fcPlayers.keySet()) {
            Fighter temp = _fcPlayers.get(id);
            L2PcInstance enemy = L2World.getInstance().getPlayer(id);
            FcItem item = temp.item;
            if (item == null) {
                continue;
            }

            if (enemy == null) {
                continue;
            }

            String item_name = item.name + "(" + item.count + ")(+" + item.enchant + ")";
            String augm = "";
            if (item.aug_skillId > 0) {
                augm = "<br1>" + getAugmentSkill(item.aug_skillId, item.aug_lvl);
            }

            tb.append("<tr><td><img src=\"" + item.icon + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + npcObj + "_fc_enemy " + id + "\">Игрок: " + enemy.getName() + "<br1>" + item_name + ")</a> " + augm + "</td></tr>");
        }

        tb.append("</table><br><br>");
        tb.append("</body></html>");
        reply.setHtml(tb.toString());
        player.sendPacket(reply);
        tb.clear();
        tb = null;
    }

    public static void viewFights(L2PcInstance player, int npcObj) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(npcObj);
        TextBuilder tb = new TextBuilder("<html><body>Бойцовский клуб:");
        if (_fcPlayers.isEmpty()) {
            tb.append("Нет боев.");
            tb.append("</body></html>");
            reply.setHtml(tb.toString());
            player.sendPacket(reply);
            tb.clear();
            tb = null;
            return;
        }
        tb.append("<br1> Просмотр боев<br><table width=300>");

        for (Integer id : _fcFights.keySet()) {
            Contest temp = _fcFights.get(id);
            //int enemyId = temp.obj_id2;
            //FightClubArena arena = temp.stadium;
            L2PcInstance fighter = L2World.getInstance().getPlayer(temp.fighter1.obj_id);
            L2PcInstance enemy = L2World.getInstance().getPlayer(temp.fighter2.obj_id);
            if (fighter == null || enemy == null) {
                continue;
            }

            FcItem item1 = temp.fighter1.item;
            String item_name1 = item1.name + "(" + item1.count + ")(+" + item1.enchant + ")";
            String augm1 = "";
            if (item1.aug_skillId > 0) {
                augm1 = "<br1>" + getAugmentSkill(item1.aug_skillId, item1.aug_lvl);
            }

            FcItem item2 = temp.fighter2.item;
            String item_name2 = item2.name + "(" + item2.count + ")(+" + item2.enchant + ")";
            String augm2 = "";
            if (item2.aug_skillId > 0) {
                augm2 = "<br1>" + getAugmentSkill(item2.aug_skillId, item2.aug_lvl);
            }

            tb.append("<tr><td></td><td><a action=\"bypass -h npc_" + npcObj + "_fc_arview " + id + "\">Бой между: " + fighter.getName() + " и " + enemy.getName() + "</a>На кону:<br> </td></tr>");
            tb.append("<tr><td><img src=\"" + item1.icon + "\" width=32 height=32></td><td>" + item_name1 + " " + augm1 + "</td></tr>");
            tb.append("<tr><td><img src=\"" + item2.icon + "\" width=32 height=32></td><td>" + item_name2 + " " + augm2 + "</td></tr>");
        }
        tb.append("</table><br><br><a action=\"bypass -h npc_" + npcObj + "_FightClub 1\">Вернуться</a><br>");
        tb.append("</body></html>");
        reply.setHtml(tb.toString());
        player.sendPacket(reply);
        tb.clear();
        tb = null;
    }

    public static void viewArena(L2PcInstance player, int id, int npcObj) {
        Contest ftemp = _fcFights.get(id);
        //FightClubArena arena = ftemp.stadium;
        ftemp.stadium.addSpectator(1, player, true);
    }

    public static void showInventoryItems(L2PcInstance player, int type, int npcObj) {
        if (player != null) {
            player.sendHtmlMessage("Ивент отключен.");
            return;
        }
        type = 0; // удалить после теста

        NpcHtmlMessage reply = NpcHtmlMessage.id(npcObj);
        TextBuilder tb = new TextBuilder("<html><body>");
        tb.append("Выбор шмотки:<br>На что сыграем?<br><br><table width=300>");

        int objectId = 0;
        String itemName = "";
        int enchantLevel = 0;
        String itemIcon = "";
        String augm = "";
        int itemType = 0;
        int itemId = 0;
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            itemId = item.getItemId();
            if (!(Config.FC_ALLOWITEMS.contains(itemId))) {
                continue;
            }

            objectId = item.getObjectId();
            itemName = item.getItem().getName();
            itemIcon = item.getItem().getIcon();

            if (type == 0) {
                //if (isCol(itemId))
                tb.append("<tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + npcObj + "_fc_item_0_" + objectId + "\">" + itemName + " (" + item.getCount() + " штук)</a></td></tr>");
            } else {
                enchantLevel = item.getEnchantLevel();
                itemType = item.getItem().getType2();
                if (item.canBeEnchanted() && !item.isEquipped() && item.isDestroyable() && (itemType == L2Item.TYPE2_WEAPON || itemType == L2Item.TYPE2_SHIELD_ARMOR || itemType == L2Item.TYPE2_ACCESSORY || item.isAugmented())) {
                    if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                        L2Skill augment = item.getAugmentation().getAugmentSkill();
                        augm = "<br1>" + getAugmentSkill(augment.getId(), augment.getLevel());
                    }
                    tb.append("<tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><a action=\"bypass -h npc_" + npcObj + "_fc_item_1_" + objectId + "\">" + itemName + " (+" + enchantLevel + ")</a> " + augm + "</td></tr>");
                }
            }
        }

        tb.append("</table><br><br><a action=\"bypass -h npc_" + npcObj + "_FightClub 1\">Вернуться</a><br>");
        tb.append("</body></html>");
        reply.setHtml(tb.toString());
        player.sendPacket(reply);
        tb.clear();
        tb = null;
    }

    //подробная инфа о шмотке
    public static void showItemFull(L2PcInstance player, int objectId, int type, int npcObj) {
        type = 0; // удалить после теста

        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item == null) {
            showError(player, "Шмотка не найдена");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(npcObj);
        TextBuilder tb = new TextBuilder("<html><body>");

        String itemName = item.getItem().getName();
        String enchantLevel = "";
        int count = item.getCount();
        String augm = "";
        int augId = -1;
        int encLvl = 0;

        tb.append("Выбор ставки:<br>Подтверждаете?<br>");
        tb.append("<table width=300><tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + " (" + count + ")(+" + item.getEnchantLevel() + ")</font><br></td></tr></table><br><br>");
        if (type == 1) {
            tb.append("<br>Заточка: <font color=bef574>+" + enchantLevel + "</font><br>");
            if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                L2Skill augment = item.getAugmentation().getAugmentSkill();
                augm = "<br1>" + getAugmentSkill(augment.getId(), augment.getLevel());
                augId = item.getAugmentation().getAugmentationId();
            }
            //tb.append("<br>Пароль на бой*:<br><edit var=\"pass\" width=200 length=\"16\"><br>");
            tb.append("<button value=\"Выставить\" action=\"bypass -h npc_" + npcObj + "_fc_add " + objectId + " -1 $pass\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
            count = 1;
            encLvl = 0;
        } else {
            //tb.append("<br>Пароль на бой*:<br><edit var=\"pass\" width=200 length=\"16\"><br>");
            tb.append("Сколько ставим? (max. " + count + "):<br1><edit var=\"count\" width=200 length=\"16\">");
            tb.append("<button value=\"Выставить\" action=\"bypass -h npc_" + npcObj + "_fc_add " + objectId + " $count $pass\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
            encLvl = item.getEnchantLevel();
        }
        player.setFCItem(objectId, encLvl, augId, count);

        //tb.append("<br>* для боя с определенным игроком<br>");
        tb.append("<br><br><a action=\"bypass -h npc_" + npcObj + "_FightClub 1\">Вернуться</a><br>");
        tb.append("</body></html>");
        reply.setHtml(tb.toString());
        player.sendPacket(reply);
        tb.clear();
        tb = null;
    }

    // finish ставка
    public static void finishItemFull(L2PcInstance player, int objectId, int count, String passw, int npcObj) {
        if (_fcPlayers.containsKey(player.getObjectId())) {
            showError(player, "Вы уже ждете бой");
            return;
        }

        if (count == 0) {
            showError(player, "Шмотка не найдена. 4.1");
            return;
        }

        if (player.getFcObj() != objectId) {
            showError(player, "Шмотка не найдена. 4.2");
            return;
        }

        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item != null) {
            int enchantLevel = item.getEnchantLevel();
            if (player.getFcEnch() != enchantLevel) {
                showError(player, "Шмотка не найдена. 5");
                return;
            }
            int itemCount = item.getCount();
            if (itemCount == 0 || player.getFcCount() == 0 || itemCount < player.getFcCount()) {
                showError(player, "Шмотка не найдена. 6");
                return;
            }

            NpcHtmlMessage reply = NpcHtmlMessage.id(npcObj);
            TextBuilder tb = new TextBuilder("<html><body>");

            int itemId = item.getItemId();
            int augmentId = 0;
            int augAttr = 0;
            int augLvl = 0;
            int shadow = 0;
            int encLvl = item.getEnchantLevel();
            String itemName = item.getItem().getName();
            String itemIcon = item.getItem().getIcon();

            tb.append("<br>Выставлена шмотка<br>");
            tb.append("<table width=300><tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><font color=LEVEL>" + itemName + "(" + count + ")(+" + encLvl + ")</font><br></td></tr></table><br><br>");

            if (item.isAugmented() && item.getAugmentation().getAugmentSkill() != null) {
                augAttr = item.getAugmentation().getAugmentationId();
                if (player.getFcAugm() != augAttr) {
                    showError(player, "Ошибка запроса.");
                    return;
                }

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
                augLvl = augment.getLevel();

                tb.append("<br>Аугмент: <font color=bef574>" + augName + " (" + type + ")</font><br>");
            }

            if (player.getItemCount(itemId) < count) {
                tb.clear();
                tb = null;
                showError(player, "Шмотка не найдена. 7");
                return;
            }
            /*if (!player.destroyItemByItemId("FC add item", itemId, count, player, true))
            {
            showError(player, "Шмотка не найдена");
            return;
            }*/

            int plObj = player.getObjectId();
            if (addToFC(player, itemId, enchantLevel, augmentId, augAttr, augLvl, count, passw, shadow)) {
                //L2ItemInstance itemDropped = player.getInventory().dropItem("FC cols", objectId, count, player, player.getLastFolkNPC(), true);

                player.destroyItem("FightClub", objectId, count, null, true);
                putFighter(plObj, new FcItem(itemId, count, enchantLevel, augAttr, augmentId, augLvl, itemName, itemIcon), passw, shadow);

                player.setFClub(true);
                player.sendItems(false);
                player.sendChanges();
                tb.append("Выставлена!<br><br>");
            } else {
                tb.append("Произошла ошибка!<br><br>");
            }

            player.setFCItem(0, 0, 0, 0);

            tb.append("<br><a action=\"bypass -h npc_" + npcObj + "_FightClub 1\">Вернуться.</a>");
            tb.append("</body></html>");
            reply.setHtml(tb.toString());
            player.sendPacket(reply);
            tb.clear();
            tb = null;
            reply = null;
        } else {
            showError(player, "Шмотка не найдена. 8");
            return;
        }
    }

    public static void putFighter(int obj, FcItem item, String pass, int shadow) {
        /*Lock putf = rwlock.writeLock();
        putf.lock();
        try 
        {*/
        _fcPlayers.put(obj, new Fighter(obj, item, pass, shadow));
        /*}
        finally 
        {
        putf.unlock();
        }*/
    }

    // выставление шмотки на бой
    private static boolean addToFC(L2PcInstance player, int itemId, int enchantLevel, int augmentId, int augAttr, int augLvl, int count, String pass, int shadow) {
        if (count == 0) {
            return false;
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("REPLACE INTO z_fc_players (id, name, itemId, enchant, augment, augAttr, augLvl, count, password, shadow) VALUES (?,?,?,?,?,?,?,?,?,?)");
            st.setInt(1, player.getObjectId());
            st.setString(2, player.getName());
            st.setInt(3, itemId);
            st.setInt(4, enchantLevel);
            st.setInt(5, augmentId);
            st.setInt(6, augAttr);
            st.setInt(7, augLvl);
            st.setInt(8, count);
            st.setString(9, pass);
            st.setInt(10, shadow);
            st.execute();
            return true;
        } catch (final SQLException e) {
            _log.warning("FC: addToFC() error: " + e);
            e.printStackTrace();
        } finally {
            Close.CS(con, st);
        }
        return false;
    }

    public static void showEnemyDetails(L2PcInstance player, int id, int npcObj) {
        if (!_fcPlayers.containsKey(id)) {
            showError(player, "Боец ни найден");
            return;
        }

        NpcHtmlMessage reply = NpcHtmlMessage.id(npcObj);
        TextBuilder tb = new TextBuilder("<html><body>");

        Fighter temp = _fcPlayers.get(id);
        L2PcInstance enemy = L2World.getInstance().getPlayer(id);
        FcItem item = temp.item;
        if (enemy == null) {
            showError(player, "Игрок не в игре");
            sendLetter(id, item, false);
            _fcPlayers.remove(id);
            return;
        }
        //String pass = temp.pass;			
        //int shadow = temp.active;	

        String item_name = item.name + "(" + item.count + ")(+" + item.enchant + ")";

        tb.append("Бойцовский клуб:<br>Игрок: " + enemy.getName() + "<br>");
        tb.append("Класс: " + CharTemplateTable.getClassNameById(enemy.getActiveClass()) + "<br>");
        tb.append("Уровень: " + enemy.getLevel() + "<br>");
        if (enemy.isHero()) {
            tb.append("Статус: Герой<br>");
        }

        tb.append("<table width=300><tr><td><img src=\"" + item.icon + "\" width=32 height=32></td><td><font color=LEVEL>" + item_name + ")</font><br></td></tr></table><br><br>");

        if (item.aug_skillId > 0) {
            tb.append(getAugmentSkill(item.aug_skillId, item.aug_lvl) + "<br><br>");
        }

        //tb.append("<br>Пароль на бой*:<br><edit var=\"pass\" width=200 length=\"16\"><br>"); потом мб
        //tb.append("Сколько ставим? (max. " + iCount + "):<br><edit var=\"count\" width=200 length=\"16\">"); потом мб
        L2ItemInstance coins = player.getInventory().getItemByItemId(item.id);
        if (coins != null && coins.getCount() >= item.count) {
            tb.append("<button value=\"Принять\" action=\"bypass -h npc_" + npcObj + "_fc_accept " + id + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
        } else {
            tb.append("<font color=999999>[Принять бой]</font><br>");
        }

        tb.append("<br><br><a action=\"bypass -h npc_" + npcObj + "_FightClub 1\">Вернуться</a><br>");
        tb.append("</body></html>");
        reply.setHtml(tb.toString());
        player.sendPacket(reply);
        tb.clear();
        tb = null;
    }

    public static void startFight(L2PcInstance player, int id, int npcObj) {
        if (!_fcPlayers.containsKey(id)) {
            showError(player, "Боец ни найден");
            return;
        }

        Fighter temp = _fcPlayers.get(id);
        L2PcInstance enemy = L2World.getInstance().getPlayer(id);
        FcItem item = temp.item;
        if (enemy == null) {
            showError(player, "Игрок не в игре");
            sendLetter(id, item, false);
            _fcPlayers.remove(id);
            return;
        }
        String pass = temp.pass;
        int shadow = temp.active;

        FightClubArena arenaf = null;
        for (int i = (_arenas.size() - 1); i > -1; i--) {
            FightClubArena arena = _arenas.get(i);
            if (arena == null) {
                continue;
            }

            if (arena.isFreeToUse()) {
                arena.setStadiaBusy();
                arenaf = arena;
                break;
            }
        }
        if (arenaf == null) {
            showError(player, "Нет свободных арен");
            return;
        }

        L2ItemInstance coins = player.getInventory().getItemByItemId(item.id);
        if (coins == null || coins.getCount() < item.count) {
            showError(player, "Неправильная ставка");
            return;
        }

        if (!player.destroyItemByItemId("FC accept", item.id, item.count, player, true)) {
            showError(player, "Неправильная ставка");
            return;
        }

        _fcPlayers.remove(id);
        //_fcFights.put(id, new Contest(id, player, item, stadium));
        //putBattle(temp, player.getObjectId(), arenaf);
        putBattle(temp, new Fighter(player.getObjectId(), item, pass, shadow), arenaf);

        player.sendCritMessage("Через 30 секунд телепорт на арену.");
        enemy.sendCritMessage("Через 30 секунд телепорт на арену.");
        player.setFClub(true);
        enemy.setFClub(true);
        ThreadPoolManager.getInstance().scheduleGeneral(new Teleport(id), 30000);
    }

    public static void putBattle(Fighter fighter1, Fighter fighter2, FightClubArena stadium) {
        _fcFights.put(fighter1.obj_id, new Contest(fighter1, fighter2, stadium));
    }

    public static class Teleport implements Runnable {

        private int _id;

        public Teleport(int id) {
            _id = id;
        }

        public void run() {
            if (!_fcFights.containsKey(_id)) {
                return;
            }

            Contest temp = _fcFights.get(_id);
            L2PcInstance fighter = L2World.getInstance().getPlayer(_id);
            int eid = temp.fighter2.obj_id;
            L2PcInstance enemy = L2World.getInstance().getPlayer(eid);
            if (fighter == null || enemy == null) {
                if (fighter != null) {
                    showError(fighter, "Игрок не в игре");
                    fighter.sendCritMessage("Бойцовский клуб: возврат ставки, проверь почту");
                    fighter.sendPacket(new ExMailArrived());
                }
                if (enemy != null) {
                    showError(enemy, "Игрок не в игре");
                    enemy.sendCritMessage("Бойцовский клуб: возврат ставки, проверь почту");
                    enemy.sendPacket(new ExMailArrived());
                }
                sendLetter(_id, temp.fighter1.item, false);
                sendLetter(eid, temp.fighter2.item, false);
                _fcFights.remove(_id);
                return;
            }
            FightClubArena arena = temp.stadium;
            int[] coords = arena.getCoordinates();

            FastList<L2PcInstance> _fighters = new FastList<L2PcInstance>();
            _fighters.add(fighter);
            _fighters.add(enemy);

            ThreadPoolManager.getInstance().scheduleGeneral(new StartFight(_id), 60000);

            for (FastList.Node<L2PcInstance> n = _fighters.head(), end = _fighters.tail(); (n = n.getNext()) != end;) {
                L2PcInstance plyr = n.getValue();

                plyr.setEventWait(true);
                plyr.setCurrentCp(plyr.getMaxCp());
                plyr.setCurrentHp(plyr.getMaxHp());
                plyr.setCurrentMp(plyr.getMaxMp());
                plyr.teleToLocation(coords[0] + Rnd.get(300), coords[1] + Rnd.get(300), coords[2]);

                plyr.sendCritMessage("Битва начнется через минуту, баффайся. Если противник вылетел, дождись начала боя.");
                if (plyr.getClan() != null) {
                    for (L2Skill skill : plyr.getClan().getAllSkills()) {
                        plyr.removeSkill(skill, false);
                    }
                }

                // Abort casting if player casting
                if (plyr.isCastingNow()) {
                    plyr.abortCast();
                }

                // Force the character to be visible
                plyr.setChannel(6);

                // Remove Hero Skills
                if (plyr.isHero()) {
                    for (L2Skill skill : HeroSkillTable.getHeroSkills()) {
                        plyr.removeSkill(skill, false);
                    }
                }

                // Remove Summon's Buffs
                if (plyr.getPet() != null) {
                    L2Summon summon = plyr.getPet();
                    summon.stopAllEffects();

                    if (summon.isPet()) {
                        summon.unSummon(plyr);
                    }
                }

                if (plyr.getCubics() != null) {
                    for (L2CubicInstance cubic : plyr.getCubics().values()) {
                        cubic.stopAction();
                        plyr.delCubic(cubic.getId());
                    }
                    plyr.getCubics().clear();
                }

                // Remove player from his party
                if (plyr.getParty() != null) {
                    plyr.getParty().removePartyMember(plyr);
                }

                plyr.sendSkillList();

                plyr.setCurrentCp(plyr.getMaxCp());
                plyr.setCurrentHp(plyr.getMaxHp());
                plyr.setCurrentMp(plyr.getMaxMp());
                SkillTable.getInstance().getInfo(1204, 2).getEffects(plyr, plyr);
                if (!plyr.isMageClass()) {
                    SkillTable.getInstance().getInfo(1086, 2).getEffects(plyr, plyr);
                } else {
                    SkillTable.getInstance().getInfo(1085, 3).getEffects(plyr, plyr);
                }
                plyr.broadcastUserInfo();
            }
        }
    }

    public static class StartFight implements Runnable {

        private int _id;

        public StartFight(int id) {
            _id = id;
        }

        public void run() {
            if (!_fcFights.containsKey(_id)) {
                return;
            }

            Contest temp = _fcFights.get(_id);
            L2PcInstance fighter = L2World.getInstance().getPlayer(_id);
            int eid = temp.fighter2.obj_id;
            FightClubArena arena = temp.stadium;
            L2PcInstance enemy = L2World.getInstance().getPlayer(eid);
            if (fighter == null || enemy == null) {
                if (fighter != null) {
                    showError(fighter, "Игрок не в игре");
                    fighter.sendCritMessage("Бойцовский клуб: возврат ставки, проверь почту");
                    fighter.sendPacket(new ExMailArrived());
                    fighter.teleToLocation(_tpLoc.x + Rnd.get(100), _tpLoc.y + Rnd.get(100), _tpLoc.x);
                }
                if (enemy != null) {
                    showError(enemy, "Игрок не в игре");
                    enemy.sendCritMessage("Бойцовский клуб: возврат ставки, проверь почту");
                    enemy.sendPacket(new ExMailArrived());
                    enemy.teleToLocation(_tpLoc.x + Rnd.get(100), _tpLoc.y + Rnd.get(100), _tpLoc.x);
                }

                sendLetter(_id, temp.fighter1.item, false);
                sendLetter(eid, temp.fighter2.item, false);
                arena.setStadiaFree();
                _fcFights.remove(_id);
                return;
            }

            SystemMessage sm = null;
            for (int i = 5; i > 0; i--) {
                sm = SystemMessage.id(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS).addNumber(i);
                fighter.sendPacket(sm);
                enemy.sendPacket(sm);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }

            FastList<L2PcInstance> _fighters = new FastList<L2PcInstance>();
            _fighters.add(fighter);
            _fighters.add(enemy);

            sm = SystemMessage.id(SystemMessageId.LET_THE_DUEL_BEGIN);
            for (FastList.Node<L2PcInstance> n = _fighters.head(), end = _fighters.tail(); (n = n.getNext()) != end;) {
                L2PcInstance plyr = n.getValue();

                plyr.setFightClub(true);
                plyr.setCurrentCp(plyr.getMaxCp());
                plyr.setCurrentHp(plyr.getMaxHp());
                plyr.setCurrentMp(plyr.getMaxMp());
                plyr.setTeam(2);
                plyr.setEventWait(false);
                //player.setInOlumpiadStadium(false);				
                plyr.setInsideZone(L2Character.ZONE_PVP, true);
                plyr.sendPacket(Static.ENTERED_COMBAT_ZONE);
                plyr.sendPacket(sm);
            }
            sm = null;

            boolean victory = false;
            for (int i = 0; i < _maxTime; i += 10000) {
                try {
                    Thread.sleep(10000);
                    if (haveWinner(_id)) {
                        victory = true;
                        break;
                    }
                } catch (InterruptedException e) {
                }
            }

            for (FastList.Node<L2PcInstance> n = _fighters.head(), end = _fighters.tail(); (n = n.getNext()) != end;) {
                L2PcInstance plyr = n.getValue();
                if (plyr == null) {
                    continue;
                }

                if (!victory) {
                    plyr.sendMessage("Бойцовский клуб: проверь почту");
                    plyr.sendPacket(new ExMailArrived());
                    if (plyr.getObjectId() == temp.fighter1.obj_id) {
                        sendLetter(plyr.getObjectId(), temp.fighter1.item, false);
                    } else {
                        sendLetter(plyr.getObjectId(), temp.fighter2.item, false);
                    }
                }

                try {
                    plyr.teleToLocation(116530 + Rnd.get(100), 76141 + Rnd.get(100), -2730);
                } catch (Exception e) {
                }

                if (plyr.isDead()) {
                    plyr.doRevive();
                }

                plyr.setCurrentCp(plyr.getMaxCp());
                plyr.setCurrentHp(plyr.getMaxHp());
                plyr.setCurrentMp(plyr.getMaxMp());
                plyr.setChannel(1);
                plyr.setFClub(false);
                plyr.setFightClub(false);
                plyr.setEventWait(false);
                plyr.setInsideZone(L2Character.ZONE_PVP, false);
                plyr.sendPacket(Static.LEFT_COMBAT_ZONE);
                plyr.setTeam(0);
            }
            _fighters.clear();
            _fcFights.remove(_id);
            arena.setStadiaFree();
        }
    }

    private static boolean haveWinner(int id) {
        Contest temp = _fcFights.get(id);
        L2PcInstance fighter = L2World.getInstance().getPlayer(id);
        int eid = temp.fighter2.obj_id;
        L2PcInstance enemy = L2World.getInstance().getPlayer(eid);
        if ((fighter == null || enemy == null) || (!fighter.inFightClub() || !enemy.inFightClub())) {
            if (fighter != null && fighter.inFightClub()) {
                fighter.sendCritMessage("Бойцовский клуб: Победа! Проверь почту.");
                fighter.sendPacket(new ExMailArrived());
                sendLetter(id, temp.fighter1.item, true);
                sendLetter(id, temp.fighter2.item, true);
                return true;
            }
            if (enemy != null && enemy.inFightClub()) {
                enemy.sendCritMessage("Бойцовский клуб: Победа! Проверь почту.");
                enemy.sendPacket(new ExMailArrived());
                sendLetter(eid, temp.fighter1.item, true);
                sendLetter(eid, temp.fighter2.item, true);
                return true;
            }

            sendLetter(id, temp.fighter1.item, false);
            sendLetter(id, temp.fighter2.item, false);
            return true;
        }
        return false;
    }

    public static void unReg(int obj, boolean battle) {
        if (_fcFights.containsKey(obj)) {
            return;
        }

        if (_fcPlayers.containsKey(obj)) {
            Fighter temp = _fcPlayers.get(obj);
            if (!battle) {
                sendLetter(obj, temp.item, false);
            }

            _fcPlayers.remove(obj);
        }
    }

    public static boolean isRegged(int obj) {
        if (_fcPlayers.containsKey(obj)) {
            return true;
        }

        if (_fcFights.containsKey(obj)) {
            return true;
        }

        return false;
    }

    private static void sendLetter(int char_id, FcItem item, boolean victory) {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet result = null;
        TextBuilder text = new TextBuilder();
        try {
            con = L2DatabaseFactory.get();

            /*statement = con.prepareStatement("SELECT COUNT(id) FROM z_fc_players WHERE id = ?");
            statement.setInt(1, char_id);
            result = statement.executeQuery();
            result.next();
            if (result.getInt(1) == 0)
            return;
            Close.SR(statement, result);*/

            /*String name = getNameById(char_id);
            if (name == "")
            return;*/

            st = con.prepareStatement("DELETE FROM `z_fc_players` WHERE `id`=?");
            st.setInt(1, char_id);
            st.execute();
            Close.S(st);

            if (Config.FC_INSERT_INVENTORY) {
                L2PcInstance player = L2World.getInstance().getPlayer(char_id);
                if (player == null) {
                    GiveItem.insertOffline(con, char_id, item.id, item.count, item.enchant, 0, 0, "INVENTORY");
                } else {
                    L2ItemInstance reward = player.getInventory().addItem("auc1", item.id, item.count, player, player.getTarget());
                    if (reward == null) {
                        return;
                    }

                    if (item.enchant > 0 && item.count == 1) {
                        reward.setEnchantLevel(item.enchant);
                    }

                    player.sendItems(true);
                    player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(item.id));
                }
                return;
            }

            String item_name = item.name + "(" + item.count + ")(+" + item.enchant + ")";
            String augm = "";
            if (item.aug_skillId > 0) {
                augm = "<br1>" + getAugmentSkill(item.aug_skillId, item.aug_lvl);
            }

            String theme = "Возврат";
            if (victory) {
                theme = "Победа!";
            }

            text.append("Итем: <font color=FF3399>" + item_name + " <br>" + augm + "</font>.<br1>");
            text.append("Благодарим за сотрудничество.");

            Date date = new Date();
            SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timef = new SimpleDateFormat("HH:mm:ss");

            /*st = con.prepareStatement("INSERT INTO `z_post_pos` (`id`,`tema`,`text`,`from`,`to`,`type`,`date`,`time`,`itemName`,`itemId`,`itemCount`,`itemEnch`,`augData`,`augSkill`,`augLvl`) VALUES (NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            st.setString(1, theme);
            st.setString(2, text.toString());
            st.setString(3, "~Fight Club.");
            st.setString(4, name);
            st.setInt(5, 0);
            st.setString(6, datef.format(date).toString());
            st.setString(7, timef.format(date).toString());
            st.setString(8, item.getItem().getName());
            st.setInt(9, item.getItemId());
            st.setInt(10, item.getCount());
            st.setInt(11, item.getEnchantLevel());
            st.setInt(12, augAttr);
            st.setInt(13, augmentId);
            st.setInt(14, augLvl);
            st.execute();*/
            st = con.prepareStatement("INSERT INTO `z_bbs_mail` (`from`, `to`, `tema`, `text`, `datetime`, `read`, `item_id`, `item_count`, `item_ench`, `aug_hex`, `aug_id`, `aug_lvl`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            st.setInt(1, 334);
            st.setInt(2, char_id);
            st.setString(3, theme);
            st.setString(4, text.toString());
            st.setLong(5, System.currentTimeMillis());
            st.setInt(6, 0);
            st.setInt(7, item.id);
            st.setInt(8, item.count);
            st.setInt(9, item.enchant);
            st.setInt(10, item.aug_hex);
            st.setInt(11, item.aug_skillId);
            st.setInt(12, item.aug_lvl);
            st.execute();
        } catch (final Exception e) {
            _log.warning("FightClub: sendLetter() error: " + e);
        } finally {
            Close.CSR(con, st, result);
            text.clear();
            text = null;
        }
    }

    // информации об аугментации
    private static String getAugmentSkill(int skillId, int skillLvl) {
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

    private static void showError(L2PcInstance player, String errorText) {
        player.setFCItem(0, 0, 0, 0);
        player.sendHtmlMessage("Ошибка", errorText);
        player.sendActionFailed();
    }

    public static void init() {
        _arenas.add(new FightClubArena(-20814, -21189, -3030));
        _arenas.add(new FightClubArena(-120324, -225077, -3331));
        _arenas.add(new FightClubArena(-102495, -209023, -3331));
        _arenas.add(new FightClubArena(-120156, -207378, -3331));
        _arenas.add(new FightClubArena(-87628, -225021, -3331));
        _arenas.add(new FightClubArena(-81705, -213209, -3331));
        _arenas.add(new FightClubArena(-87593, -207339, -3331));
        _arenas.add(new FightClubArena(-93709, -218304, -3331));
        _arenas.add(new FightClubArena(-77157, -218608, -3331));
        _arenas.add(new FightClubArena(-69682, -209027, -3331));
        _arenas.add(new FightClubArena(-76887, -201256, -3331));
        _arenas.add(new FightClubArena(-109985, -218701, -3331));
        _arenas.add(new FightClubArena(-126367, -218228, -3331));
        _arenas.add(new FightClubArena(-109629, -201292, -3331));
        _arenas.add(new FightClubArena(-87523, -240169, -3331));
        _arenas.add(new FightClubArena(-81748, -245950, -3331));
        _arenas.add(new FightClubArena(-77123, -251473, -3331));
        _arenas.add(new FightClubArena(-69778, -241801, -3331));
        _arenas.add(new FightClubArena(-76754, -234014, -3331));
        _arenas.add(new FightClubArena(-93742, -251032, -3331));
        _arenas.add(new FightClubArena(-87466, -257752, -3331));
        _arenas.add(new FightClubArena(-114413, -213241, -3331));

        _log.info("Fight Club - loaded " + _arenas.size() + "arenas.");
    }
}
