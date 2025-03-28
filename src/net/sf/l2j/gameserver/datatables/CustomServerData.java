package net.sf.l2j.gameserver.datatables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ItemsAutoDestroy;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcNpcInstance;
import net.sf.l2j.gameserver.model.entity.EventTerritory;
import net.sf.l2j.gameserver.model.entity.EventTerritoryRound;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.log.AbstractLogger;
import org.mmocore.network.nio.impl.MMOConnection;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


public class CustomServerData {

    private static final Logger _log = AbstractLogger.getLogger(CustomServerData.class.getName());
    private static CustomServerData _instance;

    public static CustomServerData getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new CustomServerData();
        _instance.load();
    }

    public CustomServerData() {
    }

    private void load() {
        if (Config.ALLOW_DSHOP) {
            loadDonateShop();
        }

        if (Config.ALLOW_DSKILLS) {
            loadDonateSkills();
        }

        if (Config.ALLOW_CSHOP) {
            cacheChinaShop();
        }

        if (Config.REWARD_SHADOW) {
            cacheShadowRewards();
        }

        if (Config.ALLOW_NPC_CHAT) {
            cacheNpcChat();
        }

        if (Config.PROTECT_MOBS_ITEMS) {
            cacheNpcPenaltyItems();
        }

        if (Config.CUSTOM_CHEST_DROP) {
            cacheChestDrop();
        }

        if (Config.CASTLE_BONUS_SKILLS) {
            cacheCastleSkills();
        }

        cacheSoldSkills();
        cacheZakenPoints();
        cacheWhiteBuffs();
        parceCustomMessages();
        cacheRiddles();
        cacheEventZones();
        cacheCustomZones();
        cacheClanSkills();
        cacheChestsDrop();
        cacheSpecialDrop();
        loadPcNpcs();
    }
    //кеш донат шопа
    public static FastTable<DonateItem> _donateItems = new FastTable<DonateItem>();
    //кеш проданных скиллов
    public static FastMap<Integer, FastTable<DonateSkill>> _cachedSkills = new FastMap<Integer, FastTable<DonateSkill>>().shared("CustomServerData._cachedSkills");
    //кеш шадоу шмоток
    public static FastMap<Integer, int[]> _shdSets = new FastMap<Integer, int[]>().shared("CustomServerData._shdSets");
    //кеш китайского шопа
    public static FastMap<Integer, ChinaItem> _chinaItems = new FastMap<Integer, ChinaItem>().shared("CustomServerData._chinaItems");
    //точки тп закена
    public static FastTable<Location> _zakenPoints = new FastTable<Location>();
    //разрешенные баффы
    public static FastTable<Integer> _whiteBuffs = new FastTable<Integer>();
    //кеш кастом сообщений
    public static FastMap<Integer, String> _customMessages = new FastMap<Integer, String>().shared("CustomServerData._customMessages");
    //кеш загадок
    public static FastMap<Integer, Riddle> _riddles = new FastMap<Integer, Riddle>().shared("CustomServerData._riddles");
    //кеш донат скиллов
    public static FastMap<Integer, FastTable<DonateSkill>> _donateSkills = new FastMap<Integer, FastTable<DonateSkill>>().shared("CustomServerData._donateSkills");
    //ивент зоны
    public static FastTable<EventTerritory> _eventZones = new FastTable<EventTerritory>();
    //кеш клан скиллов
    public static FastTable<L2Skill> _clanSkills = new FastTable<L2Skill>();
    //кеш нпц чата
    public static FastMap<Integer, NpcChat> _cachedNpcChat = new FastMap<Integer, NpcChat>().shared("CustomServerData._cachedNpcChat");
    /**
     * Статистика
     */
    public static FastTable<StatPlayer> _statPvp = new FastTable<StatPlayer>();
    public static FastTable<StatPlayer> _statPk = new FastTable<StatPlayer>();
    public static FastTable<StatClan> _statClans = new FastTable<StatClan>();
    public static FastTable<StatCastle> _statCastles = new FastTable<StatCastle>();

    public static class Riddle {

        public String answer;
        public String question;

        public Riddle(String answer, String question) {
            this.answer = answer;
            this.question = question;
        }
    }

    public static class DonateSkill {

        public int cls;
        public int id;
        public int lvl;
        public long expire;
        public int priceId;
        public int priceCount;
        public String priceName;
        public String icon;
        public String info;

        public DonateSkill(int cls, int id, int lvl, long expire) {
            this.cls = cls;
            this.id = id;
            this.lvl = lvl;
            this.expire = expire;
        }

        public DonateSkill(int cls, int id, int lvl, long expire, int priceId, int priceCount, String priceName, String icon, String info) {
            this.cls = cls;
            this.id = id;
            this.lvl = lvl;
            this.expire = expire;
            this.priceId = priceId;
            this.priceCount = priceCount;
            this.priceName = priceName;
            this.icon = icon;
            this.info = info;
        }
    }

    public static class DonateItem {

        public int itemId;
        public int itemCount;
        public String itemInfoRu;
        public String itemInfoDesc;
        public int priceId;
        public int priceCount;
        public String priceName;

        public DonateItem(int itemId, int itemCount, String itemInfoRu, String itemInfoDesc, int priceId, int priceCount, String priceName) {
            this.itemId = itemId;
            this.itemCount = itemCount;
            this.itemInfoRu = itemInfoRu;
            this.itemInfoDesc = itemInfoDesc;

            this.priceId = priceId;
            this.priceCount = priceCount;
            this.priceName = priceName;
        }
    }

    public static class ChinaItem {

        public int coin;
        public int price;
        public int days;
        public String name;
        public String info;

        public ChinaItem(int coin, int price, int days, String name, String info) {
            this.coin = coin;
            this.price = price;
            this.days = days;
            this.name = name;
            this.info = info;
        }
    }

    private void cacheSoldSkills() {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            // очищаем истекшие
            st = con.prepareStatement("DELETE FROM z_donate_skills WHERE expire > ? AND expire < ?");
            st.setInt(1, 1);
            st.setLong(2, System.currentTimeMillis());
            st.execute();
            Close.S(st);

            // кешируем
            FastTable<DonateSkill> dst = new FastTable<DonateSkill>();
            st = con.prepareStatement("SELECT char_id, class_id, skill_id, skill_lvl, expire FROM `z_donate_skills`");
            rs = st.executeQuery();
            while (rs.next()) {
                int charId = rs.getInt("char_id");
                int classId = rs.getInt("class_id");
                int skillId = rs.getInt("skill_id");
                int skillLvl = rs.getInt("skill_lvl");
                long expire = rs.getLong("expire");
                DonateSkill ds = new DonateSkill(classId, skillId, skillLvl, expire);
                if (_cachedSkills.get(charId) == null) {
                    dst.clear();
                    dst.add(ds);
                    _cachedSkills.put(charId, dst);
                } else {
                    _cachedSkills.get(charId).add(ds);
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData: cacheDonateSkills() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        /*
         * CREATE TABLE `z_donate_skills` ( `char_id` int(10) NOT NULL DEFAULT
         * '0', `class_id` smallint(3) NOT NULL DEFAULT '0', `skill_id`
         * varchar(5) NOT NULL DEFAULT '0', `skill_lvl` varchar(5) NOT NULL
         * DEFAULT '1', `expire` bigint(20) NOT NULL DEFAULT '0', PRIMARY KEY
         * (`char_id`,`class_id`) ) ENGINE=MyISAM DEFAULT CHARSET=utf8
         */
    }

    public FastTable<DonateSkill> getDonateSkills(int charId) {
        return _cachedSkills.get(charId);
    }

    public void addDonateSkill(int charId, int cls, int id, int lvl, long expire) {
        DonateSkill ds = new DonateSkill(cls, id, lvl, expire);
        if (_cachedSkills.get(charId) == null) {
            FastTable<DonateSkill> dst = new FastTable<DonateSkill>();
            dst.add(ds);
            _cachedSkills.put(charId, dst);
        } else {
            _cachedSkills.get(charId).add(ds);
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("REPLACE INTO `z_donate_skills` (`char_id`, `class_id`, `skill_id`, `skill_lvl`, `expire`) VALUES (?, ?, ?, ?, ?)");
            st.setInt(1, charId);
            st.setInt(2, cls);
            st.setInt(3, id);
            st.setInt(4, lvl);
            st.setLong(5, expire);
            st.execute();
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: addDonateSkill() ->" + e);
        } finally {
            Close.CS(con, st);
        }
    }

    public FastTable<DonateItem> getDonateShop() {
        return _donateItems;
    }

    public DonateItem getDonateItem(int saleId) {
        return _donateItems.get(saleId);
    }

    private void loadDonateShop() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/donate_shop.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/donate_shop.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            //FastList<String> strings = new FastList<String>();
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    int itemId = 0;
                    int itemCount = 0;
                    String itemInfoRu = "";
                    String itemInfoDesc = "";

                    int priceId = 0;
                    int priceCount = 0;
                    String priceName = "";
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("sale".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            int saleId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("item".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    itemId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                                    itemCount = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
                                }
                                if ("info".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    itemInfoRu = attrs.getNamedItem("ru").getNodeValue();
                                    itemInfoDesc = attrs.getNamedItem("description").getNodeValue();
                                }
                                if ("price".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    priceId = Integer.parseInt(attrs.getNamedItem("coin").getNodeValue());
                                    priceCount = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
                                    priceName = attrs.getNamedItem("name").getNodeValue();
                                }
                            }
                            _donateItems.add(new DonateItem(itemId, itemCount, itemInfoRu, itemInfoDesc, priceId, priceCount, priceName));
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: " + e.toString());
        }
        _log.config("CustomServerData: Donate Shop, loaded " + _donateItems.size() + " items.");
    }

    private void loadDonateSkills() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/donate_skills.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/donate_skills.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            //FastList<String> strings = new FastList<String>();
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    int cls = 0;
                    int id = 0;
                    int lvl = 0;
                    long expire = 0;

                    int priceId = 0;
                    int priceCount = 0;
                    String priceName = "";
                    String icon = "";
                    String info = "";
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("class".equalsIgnoreCase(d.getNodeName())) {
                            FastTable<DonateSkill> skills = new FastTable<DonateSkill>();
                            NamedNodeMap attrs = d.getAttributes();
                            cls = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("skill".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String[] skill = attrs.getNamedItem("id").getNodeValue().split(",");
                                    id = Integer.parseInt(skill[0]);
                                    lvl = Integer.parseInt(skill[1]);

                                    String[] price = attrs.getNamedItem("price").getNodeValue().split(",");
                                    priceId = Integer.parseInt(price[0]);
                                    priceCount = Integer.parseInt(price[1]);
                                    priceName = price[2];

                                    expire = Integer.parseInt(attrs.getNamedItem("period").getNodeValue());
                                    icon = attrs.getNamedItem("icon").getNodeValue();
                                    info = attrs.getNamedItem("info").getNodeValue();
                                    skills.add(new DonateSkill(cls, id, lvl, expire, priceId, priceCount, priceName, icon, info));
                                }
                            }
                            _donateSkills.put(cls, skills);
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: " + e.toString());
        }
        _log.config("CustomServerData: Donate Skills Shop, loaded " + _donateSkills.size() + " classes.");
    }

    private void cacheShadowRewards() {
        _shdSets.put(1, new int[]{2415, 2406, 5716, 5732});
        _shdSets.put(2, new int[]{2417, 2392, 5723, 5739});
        _shdSets.put(3, new int[]{2417, 2381, 5722, 5738});
        _shdSets.put(4, new int[]{358, 2380, 2416, 5718, 5734});
        //
        _shdSets.put(5, new int[]{10673, 10680, 10681, 10682, 10683, 920, 889, 889, 858, 858});
        _shdSets.put(6, new int[]{10673, 10677, 10678, 10679, 920, 889, 889, 858, 858});
        _shdSets.put(7, new int[]{10673, 10674, 10675, 10676, 920, 889, 889, 858, 858});
    }

    private void cacheWhiteBuffs() {
        int[] buffs = {1068, 1388, 1086, 1077, 1242, 1240, 4352, 1085, 1059, 1303, 1043, 1356, 1355, 1357, 1040, 1389, 1036, 1035, 1243, 1304, 1078, 1087,
            1006, 1009, 1007, 1002, 1252, 1253, 1309, 1251, 1308, 1391, 1310, 1390, 1362, 1413, 1363, 1003, 1005, 1008, 1260, 1004, 1250, 1261, 1249, 1282, 1364, 1365, 1414,
            267, 270, 268, 269, 265, 264, 266, 306, 304, 308, 363, 349, 364, 274, 277, 272, 273, 276, 271, 275, 311, 307, 310, 365, 1073, 4342, 1044, 4347, 4348, 1257, 1397, 1268, 4554,
            1032, 1392, 1393, 1259, 1354, 1353, 1352, 1191, 1182, 1189, 1033, 4700, 4702, 4703, 4699};

        int i = 0;
        for (int id : buffs) {
            _whiteBuffs.add(id);
        }

        _whiteBuffs.addAll(Config.C_ALLOWED_BUFFS);
    }

    public boolean isWhiteBuff(int id) {
        if (id >= 3100 && id <= 3299) {
            return false;
        }
        if (Config.F_BUFF.containsKey(id) || Config.M_BUFF.containsKey(id)) {
            return true;
        }
        if (_whiteBuffs.contains(id)) {
            return true;
        }
        if (Config.F_PROFILE_BUFFS.contains(id)) {
            return false;
        }

        return false;
    }

    public int[] getShadeItems(int set) {
        return _shdSets.get(set);
    }

    private void cacheChinaShop() {
        _chinaItems.put(14000, new ChinaItem(4037, 5000, 36, "Белые крылья", "pDef/mDef +200; MP +3000; HP +500."));
        _chinaItems.put(26116, new ChinaItem(4037, 5000, 36, "Черные крылья", "pDef/mDef +200; MP +3000; HP +500."));
        _chinaItems.put(50000, new ChinaItem(4037, 6000, 36, "Флаг", "pDef + 300; mDef +200; MP +3000; HP +500."));
    }

    public FastMap<Integer, ChinaItem> getChinaShop() {
        return _chinaItems;
    }

    public ChinaItem getChinaItem(int id) {
        return _chinaItems.get(id);
    }

    //
    private void cacheZakenPoints() {
        _zakenPoints.add(new Location(53950, 219860, -3488));
        _zakenPoints.add(new Location(55980, 219820, -3488));
        _zakenPoints.add(new Location(54950, 218790, -3488));
        _zakenPoints.add(new Location(55970, 217770, -3488));
        _zakenPoints.add(new Location(53930, 217760, -3488));
        _zakenPoints.add(new Location(55970, 217770, -3216));
        _zakenPoints.add(new Location(55980, 219920, -3216));
        _zakenPoints.add(new Location(54960, 218790, -3216));
        _zakenPoints.add(new Location(53950, 219860, -3216));
        _zakenPoints.add(new Location(53930, 217760, -3216));
        _zakenPoints.add(new Location(55970, 217770, -2944));
        _zakenPoints.add(new Location(55980, 219920, -2944));
        _zakenPoints.add(new Location(54960, 218790, -2944));
        _zakenPoints.add(new Location(53950, 219860, -2944));
        _zakenPoints.add(new Location(53930, 217760, -2944));
    }

    public Location getZakenPoint() {
        return _zakenPoints.get(Rnd.get(_zakenPoints.size() - 1));
    }

    //
    public String getCharName(int objId) {
        return getCharName(objId, null);
    }

    public String getCharName(int objId, Connect excon) {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            if (excon == null) {
                con = L2DatabaseFactory.get();
                con.setTransactionIsolation(1);
            } else {
                con = excon;
            }

            st = con.prepareStatement("SELECT char_name FROM `characters` WHERE `obj_Id` = ? LIMIT 0,1");
            st.setInt(1, objId);
            rs = st.executeQuery();
            if (rs.next()) {
                return rs.getString("char_name");
            }
        } catch (final Exception e) {
            _log.warning("[ERROR] CustomServerData, getCharName() error: " + e);
        } finally {
            if (excon == null) {
                Close.CSR(con, st, rs);
            } else {
                Close.SR(st, rs);
            }
        }
        return "n?f";
    }

    private void parceCustomMessages() {
        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./data/static_messages.txt");
            if (!Data.exists()) {
                _log.warning("[ERROR] CustomServerData, parceCustomMessages() '/data/static_messages.txt' not founded. ");
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            //#номер_сообщения,текст
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                String[] msgs = line.split("=");
                _customMessages.put(Integer.parseInt(msgs[0]), msgs[1]);
            }
        } catch (final Exception e) {
            _log.warning("[ERROR] CustomServerData, parceCustomMessages() error: " + e);
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
                if (br != null) {
                    br.close();
                }
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception e1) {
            }
        }
        //System.out.println("###################parceCustomMessages() error: " + _customMessages.size());
    }

    public String getMessage(int msg) {
        return _customMessages.get(msg);
    }

    private void cacheRiddles() {
        _riddles.put(99900, new Riddle("солнце", "Жаркий шар на небе светит.<br1>Этот шар любой заметит.<br1>Утром смотрит к нам в оконце,<br1>Радостно сияя, ...<br>"));
        _riddles.put(99901, new Riddle("река", "Мчится по холмам змея,<br1>Влагу деревцам неся.<br1>Омывая берега,<br1>По полям течет ...<br>"));
        _riddles.put(99902, new Riddle("облака", "Они легкие, как вата,<br1>По небу плывут куда-то.<br1>Держат путь издалека<br1>Каравеллы-...<br>"));
        _riddles.put(99903, new Riddle("роса", "Вот брильянты на листочках,<br1>Вдоль дорожек и на кочках -<br1>Это что за чудеса?<br1>По утру блестит ...<br>"));
        _riddles.put(99904, new Riddle("гроза", "Заслонили тучи солнце,<br1>Гром раскатисто смеется.<br1>В небе молний полоса -<br1>Значит, началась ...<br>"));
        _riddles.put(99905, new Riddle("град", "Сыплется из туч горох,<br1>Прыгает к нам на порог.<br1>С крыши катится он в сад.<br1>Что такое? Это - ...<br>"));
        _riddles.put(99906, new Riddle("пух", "А в июне белый снег<br1>Вновь порадовал нас всех. -<br1>Будто рой ленивых мух,<br1>С тополей слетает ...<br>"));
        _riddles.put(99907, new Riddle("дождик", "Он поплачет над садами  -<br1>Сад наполнится плодами.<br1>Даже пыльный подорожник<br1>Рад умыться в летний ...<br>"));
        _riddles.put(99908, new Riddle("коровы", "Кто пасется на лугу?<br>Далеко, далеко<br1>На лугу пасутся ко:<br1>Кони? Нет, не кони!<br1>Далеко, далеко<br1>На лугу пасутся ко:<br1>Козы? Нет, не козы!<br>Далеко, далеко<br1>На лугу пасутся ко:<br>"));
        _riddles.put(99909, new Riddle("картошка", "Под землей живёт семья: <br1>Папа, мама, деток тьма. <br1>Лишь копни её немножко - <br1>Вмиг появится ...<br>"));
        _riddles.put(99910, new Riddle("баклажан", "Наш лиловый господин <br1>Среди овощей один. <br1>Он французский граф Де Жан <br1>А по-русски - ...<br>"));
        _riddles.put(99911, new Riddle("кабачок", "Кто разлёгся среди грядки, <br1>Кто играть не любит в прятки? <br1>Вот Емеля-простачок, <br1>Белобокий ...<br>"));
        _riddles.put(99912, new Riddle("лимон", "Желтый цитрусовый плод<br1>В странах солнечных растёт.<br1>Но на вкус кислейший он,<br11>А зовут его ...<br1>"));
        _riddles.put(99913, new Riddle("груша", "Все о ней боксеры знают  <br1>С ней удар свой развивают.<br1>Хоть она и неуклюжа,<br1>Но на фрукт похожа ...<br1>"));
        _riddles.put(99914, new Riddle("банан", "Знают этот фрукт детишки,<br1>Любят есть его мартышки.<br1>Родом он из жарких стран<br1>В тропиках растет ...<br1>"));
        _riddles.put(99915, new Riddle("арбуз", "Он тяжелый и пузатый,<br1>Носит фрак свой полосатый.<br1>На макушке хвостик-ус,<br1>Спелый изнутри ...<br1>"));
        _riddles.put(99916, new Riddle("картошка", "Ты все лето зеленеешь,<br1>Спрятав ягоды в земле,<br1>Ближе к осени созреешь -<br1>Сразу праздник на столе!<br1>Суп и щи, пюре, окрошка,<br1>Нам везде нужна...<br1>"));
        _riddles.put(99917, new Riddle("арбуз", "С виду он зеленый мячик,<br1>Но зовут его иначе,<br1>На нем бархатный картуз.<br1>Это сладкий наш...<br1>"));
        _riddles.put(99918, new Riddle("свекла", "Ты кругла, вкусна, красива!<br1>Ты сочна, ну просто диво!<br1>Борщ, свекольник, винегрет...<br1>Без тебя уж не обед!<br1>Ты во всем нам помогла,<br1>А зовут тебя...<br1>"));
        _riddles.put(99919, new Riddle("чеснок", "От простуды нас избавил,<br1>Витаминов нам добавил<br1>И от гриппа он помог,<br1>Горький доктор наш...<br1>"));
        _riddles.put(99920, new Riddle("глобус", "Круглый мяч на тонкой ножке<br1>Мы вращаем у окошка.<br1>На мяче мы видим страны,<br1>Города и океаны.<br1>"));
        _riddles.put(99921, new Riddle("айболит", "Лечит он мышей и крыс,<br1>Крокодилов, зайцев, лис,<br1>Перевязывает  ранки<br1>Африканской обезьянке. <br1>И любой нам подтвердит: <br1>Это - доктор ...<br1>"));
    }

    public Riddle getRiddle(int i) {
        return _riddles.get(i);
    }

    /**
     * * офф. терйдеры**
     */
    public void restoreOfflineTraders() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Connect con = null;
                PreparedStatement st = null;
                ResultSet rs = null;
                try {
                    con = L2DatabaseFactory.get();

                    st = con.prepareStatement("DELETE FROM character_offline WHERE `name` = ? AND `value` < ?");
                    st.setString(1, "offline");
                    st.setLong(2, System.currentTimeMillis());
                    st.execute();
                    Close.S(st);

                    con.setTransactionIsolation(1);
                    // восстанавливаем
                    int count = 0;
                    st = con.prepareStatement("SELECT obj_id FROM `character_offline` WHERE `name` = ?");
                    st.setString(1, "offline");
                    rs = st.executeQuery();
                    while (rs.next()) {
                        int objid = rs.getInt("obj_id");
                        if (objid == 0) {
                            continue;
                        }

                        L2GameClient client = new L2GameClient(new MMOConnection<L2GameClient>(null, null, null), true);
                        client.setCharSelection(objid);
                        L2PcInstance p = client.loadCharFromDisk(0);
                        if (p == null || p.isDead()) {
                            continue;
                        }
                        client.setAccountName(p.getAccountName());
                        //
                        p.spawnMe();
                        p.revalidateZone(true);
                        p.setOnlineStatus(true);
                        p.setOfflineMode(true, false);
                        p.setConnected(false);
                        p.broadcastUserInfo();
                        count++;
                    }
                    _log.info("Restored " + count + " offline traders");
                } catch (Exception e) {
                    _log.severe("GameServer [ERROR]: Failed to restore offline traders. Reason: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    Close.CSR(con, st, rs);
                }
            }
        }).start();
    }

    public void cacheEventZones() {
        if (Config.TVT_EVENT_ENABLED && Config.TVT_POLY != null) {
            _eventZones.add(Config.TVT_POLY);
        }

        if (Config.ELH_ENABLE && Config.LASTHERO_POLY != null) {
            _eventZones.add(Config.LASTHERO_POLY);
        }

        if (Config.MASS_PVP && Config.MASSPVP_POLY != null) {
            _eventZones.add(Config.MASSPVP_POLY);
        }

        if (Config.EBC_ENABLE && Config.BASECAPTURE_POLY != null) {
            _eventZones.add(Config.BASECAPTURE_POLY);
        }
    }
    private EventTerritoryRound ttw = new EventTerritoryRound();

    public void cacheCustomZones() {
        EventTerritoryRound baiumLair = new EventTerritoryRound();
        baiumLair.addPoint(113144, 14072);
        baiumLair.addPoint(113288, 14008);
        baiumLair.addPoint(118328, 11464);
        baiumLair.addPoint(119000, 20824);
        baiumLair.addPoint(112600, 17430);
        baiumLair.addPoint(112232, 16536);
        baiumLair.addPoint(112248, 15576);
        baiumLair.addPoint(112632, 14584);
        baiumLair.setZ(9960, 16301);
        _eventZones.add(baiumLair);

        if (Config.FS_WALL_DOORS) {
            EventTerritoryRound sepDoor1 = new EventTerritoryRound();
            baiumLair.addPoint(175429, -82761);
            baiumLair.addPoint(175788, -82759);
            baiumLair.addPoint(175790, -82691);
            baiumLair.addPoint(175429, -82693);
            baiumLair.setZ(-7630, -6621);
            _eventZones.add(sepDoor1);
            EventTerritoryRound sepDoor2 = new EventTerritoryRound();
            baiumLair.addPoint(173033, -86673);
            baiumLair.addPoint(173397, -86673);
            baiumLair.addPoint(173400, -86604);
            baiumLair.addPoint(173036, -86604);
            baiumLair.setZ(-7630, -6621);
            _eventZones.add(sepDoor2);
            EventTerritoryRound sepDoor3 = new EventTerritoryRound();
            baiumLair.addPoint(179505, -89153);
            baiumLair.addPoint(179566, -89155);
            baiumLair.addPoint(179567, -88806);
            baiumLair.addPoint(179506, -88805);
            baiumLair.setZ(-7630, -6621);
            _eventZones.add(sepDoor3);
            EventTerritoryRound sepDoor4 = new EventTerritoryRound();
            baiumLair.addPoint(181185, -85775);
            baiumLair.addPoint(181256, -85772);
            baiumLair.addPoint(181254, -85412);
            baiumLair.addPoint(181184, -85414);
            baiumLair.setZ(-7630, -6621);
            _eventZones.add(sepDoor4);
        }
    }

    public boolean intersectEventZone(int x, int y, int z, int tx, int ty, int tz) {
        if (_eventZones.isEmpty()) {
            return false;
        }

        EventTerritory zone = null;
        for (int i = 0, n = _eventZones.size(); i < n; i++) {
            zone = _eventZones.get(i);
            if (zone == null) {
                continue;
            }

            if (zone.contains(x, y, z) && zone.contains(tx, ty, tz)) {
                continue;
            }

            if (zone.intersectsLine(x, y, z, tx, ty, tz)) {
                return true;
            }
            /*
             * boolean in = zone.contains(x, y, z); boolean out =
             * zone.contains(tx, ty, tz); if (in == out) continue;
             *
             * if (in != out) return true;
             */
        }
        zone = null;
        return false;
    }

    public void cacheStat() {
        _statPvp.clear();
        _statPk.clear();
        _statClans.clear();
        _statCastles.clear();

        L2Clan clan = null;
        String clan_name = "";
        String owner = "";
        int clan_id = -1;
        String siegeDate = "";
        CastleManager cm = CastleManager.getInstance();
        ClanTable ct = ClanTable.getInstance();

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT obj_Id,char_name,pvpkills,clanid,online FROM `characters` WHERE `pvpkills` >= ? AND `accesslevel` = ? ORDER BY `pvpkills` DESC LIMIT 0, 100");
            st.setInt(1, 1);
            st.setInt(2, 0);
            rs = st.executeQuery();
            rs.setFetchSize(20);
            while (rs.next()) {
                clan = ct.getClan(rs.getInt("clanid"));
                if (clan != null) {
                    clan_name = clan.getName();
                }

                _statPvp.add(new StatPlayer(rs.getInt("obj_Id"), Util.htmlSpecialChars(rs.getString("char_name")), Util.htmlSpecialChars(clan_name), rs.getInt("online"), rs.getInt("pvpkills")));
            }
            Close.SR(st, rs);

            st = con.prepareStatement("SELECT obj_Id,char_name,pkkills,clanid,online FROM `characters` WHERE `pkkills` >= ? AND `accesslevel` = ? ORDER BY `pkkills` DESC LIMIT 0, 100");
            st.setInt(1, 1);
            st.setInt(2, 0);
            rs = st.executeQuery();
            rs.setFetchSize(20);
            while (rs.next()) {
                clan = ct.getClan(rs.getInt("clanid"));
                if (clan != null) {
                    clan_name = clan.getName();
                }

                _statPk.add(new StatPlayer(rs.getInt("obj_Id"), Util.htmlSpecialChars(rs.getString("char_name")), Util.htmlSpecialChars(clan_name), rs.getInt("online"), rs.getInt("pkkills")));
            }
            Close.SR(st, rs);

            st = con.prepareStatement("SELECT clan_id,clan_name,clan_level,reputation_score,ally_name,leader_id FROM `clan_data` ORDER BY `clan_level` DESC, `reputation_score` DESC LIMIT 0, 100");
            rs = st.executeQuery();
            rs.setFetchSize(20);
            while (rs.next()) {
                clan = ct.getClan(rs.getInt("clan_id"));
                if (clan == null) {
                    continue;
                }

                _statClans.add(/*
                         * rs.getInt("clan_id"),
                         */new StatClan(Util.htmlSpecialChars(rs.getString("clan_name")), clan.getLeaderName(), rs.getInt("clan_level"), rs.getInt("reputation_score"), clan.getMembersCount(), rs.getString("ally_name")));
            }
            Close.SR(st, rs);

            st = con.prepareStatement("SELECT id,name,siegeDate FROM `castle` WHERE `id` <= ?");
            st.setInt(1, 9);
            rs = st.executeQuery();
            rs.setFetchSize(20);
            while (rs.next()) {
                clan_id = rs.getInt("id");
                siegeDate = String.valueOf(cm.getCastleById(clan_id).getSiegeDate().getTime());
                owner = "NPC";
                clan = ct.getClan(cm.getCastleById(clan_id).getOwnerId());
                if (clan != null) {
                    owner = clan.getName();
                }
                _statCastles.add(/*
                         * clan_id,
                         */new StatCastle(rs.getString("name"), owner, siegeDate));
            }
            Close.SR(st, rs);
        } catch (final SQLException e) {
            _log.warning("[ERROR] CustomServerData, cacheStat() error: " + e);
        } finally {
            clan = null;
            Close.CSR(con, st, rs);
        }
        cacheStatHome();
    }

    public static class StatPlayer {

        public int id;
        public String name;
        public String clan;
        public int online;
        public int kills;

        public StatPlayer(int id, String name, String clan, int online, int kills) {
            this.id = id;
            this.name = name;
            this.clan = clan;
            this.online = online;
            this.kills = kills;
        }
    }

    public static class StatClan {

        public String name;
        public String owner;
        public int level;
        public int rep;
        public int count;
        public String ally;

        public StatClan(String name, String owner, int level, int rep, int count, String ally) {
            this.name = name;
            this.owner = owner;
            this.level = level;
            this.rep = rep;
            this.count = count;
            this.ally = ally;
        }
    }

    public static class StatCastle {

        public String name;
        public String owner;
        public String siege;

        public StatCastle(String name, String owner, String siege) {
            this.name = name;
            this.owner = owner;
            this.siege = siege;
        }
    }
    /**
     * Статистика public static Map<Integer, StatPlayer> _statPvp = new
     * ConcurrentHashMap<Integer, StatPlayer>(); public static Map<Integer,
     * StatPlayer> _statPk = new ConcurrentHashMap<Integer, StatPlayer>();
     * public static Map<Integer, StatClan> _statClans = new
     * ConcurrentHashMap<Integer, StatClan>(); public static Map<Integer,
     * StatCastle> _statCastles = new ConcurrentHashMap<Integer, StatCastle>();
     */
    private static String statHome;

    private void cacheStatHome() {
        TextBuilder htm = new TextBuilder();

        int count = 0;
        StatPlayer pc = null;
        for (int i = 0, n = _statPvp.size(); i < n; i++) {
            pc = _statPvp.get(i);
            if (pc == null) {
                continue;
            }

            htm.append("<font color=CCCC33>" + pc.name + "</font> <font color=999966>" + pc.kills + "</font><br1>");
            count++;
            if (count > 10) {
                break;
            }
        }
        htm.append("</td><td valign=top>");

        count = 0;
        for (int i = 0, n = _statPk.size(); i < n; i++) {
            pc = _statPk.get(i);
            if (pc == null) {
                continue;
            }

            htm.append("<font color=CCCC33>" + pc.name + "</font> <font color=999966>" + pc.kills + "</font><br1>");
            count++;
            if (count > 10) {
                break;
            }
        }
        pc = null;

        statHome = htm.toString();
        htm.clear();
        htm = null;
    }

    public String getStatHome() {
        return statHome;
    }

    public FastTable<StatPlayer> getStatPvp() {
        return _statPvp;
    }

    public FastTable<StatPlayer> getStatPk() {
        return _statPk;
    }

    public FastTable<StatClan> getStatClans() {
        return _statClans;
    }

    public FastTable<StatCastle> getStatCastles() {
        return _statCastles;
    }

    // клан скиллы
    private void cacheClanSkills() {
        SkillTable st = SkillTable.getInstance();
        _clanSkills.add(st.getInfo(370, 3));
        _clanSkills.add(st.getInfo(371, 3));
        _clanSkills.add(st.getInfo(372, 3));
        _clanSkills.add(st.getInfo(373, 3));
        _clanSkills.add(st.getInfo(374, 3));
        _clanSkills.add(st.getInfo(375, 3));
        _clanSkills.add(st.getInfo(376, 3));
        _clanSkills.add(st.getInfo(377, 3));
        _clanSkills.add(st.getInfo(378, 3));
        _clanSkills.add(st.getInfo(379, 3));
        _clanSkills.add(st.getInfo(380, 3));
        _clanSkills.add(st.getInfo(381, 3));
        _clanSkills.add(st.getInfo(382, 3));
        _clanSkills.add(st.getInfo(383, 3));
        _clanSkills.add(st.getInfo(384, 3));
        _clanSkills.add(st.getInfo(385, 3));
        _clanSkills.add(st.getInfo(386, 3));
        _clanSkills.add(st.getInfo(387, 3));
        _clanSkills.add(st.getInfo(388, 3));
        _clanSkills.add(st.getInfo(389, 3));
        _clanSkills.add(st.getInfo(390, 3));
        _clanSkills.add(st.getInfo(391, 1));
    }

    public void addClanSkills(L2PcInstance player, L2Clan clan) {
        if (clan == null) {
            return;
        }

        L2Skill skill = null;
        for (int i = 0, n = _clanSkills.size(); i < n; i++) {
            skill = _clanSkills.get(i);;
            if (skill == null) {
                continue;
            }

            clan.addNewSkill(skill);
            player.sendPacket(SystemMessage.id(SystemMessageId.CLAN_SKILL_S1_ADDED).addSkillName(skill.getId()));
        }
        skill = null;

        clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
        for (L2PcInstance member : clan.getOnlineMembers("")) {
            if (member == null) {
                continue;
            }

            member.sendSkillList();
        }
    }
    // нпц фат
    public final String EMPTY = "";

    public static class NpcChat {

        public FastTable<String> onSpawn = new FastTable<String>();
        public FastTable<String> onAttack = new FastTable<String>();
        public FastTable<String> onDeath = new FastTable<String>();
        public FastTable<String> onKill = new FastTable<String>();
        private int onSpawnSize;
        private int onAttackSize;
        private int onDeathSize;
        private int onKillSize;
        private int type;

        public NpcChat(final FastTable<String> onSpawn, final FastTable<String> onAttack, final FastTable<String> onDeath, final FastTable<String> onKill, final int type) {
            this.onSpawn = onSpawn;
            this.onAttack = onAttack;
            this.onDeath = onDeath;
            this.onKill = onKill;

            this.onSpawnSize = onSpawn.size() - 1;
            this.onAttackSize = onAttack.size() - 1;
            this.onDeathSize = onDeath.size() - 1;
            this.onKillSize = onKill.size() - 1;

            this.type = type;
        }

        public void chatSpawn(L2NpcInstance npc) {
            if (onSpawnSize < 0) {
                return;
            }

            String txt = onSpawn.get(Rnd.get(onSpawnSize));
            txt = txt.replaceAll("%npc%", npc.getName());
            sayString(npc, txt);
        }

        public void chatAttack(L2NpcInstance npc) {
            if (onAttackSize < 0) {
                return;
            }

            npc.sayString(onAttack.get(Rnd.get(onAttackSize)));
        }

        public void chatDeath(L2NpcInstance npc, String name) {
            if (onDeathSize < 0) {
                return;
            }

            String txt = onDeath.get(Rnd.get(onDeathSize));
            txt = txt.replaceAll("%npc%", npc.getName());
            txt = txt.replaceAll("%player%", name);
            sayString(npc, txt);
        }

        public void chatKill(L2NpcInstance npc, String name) {
            if (onKillSize < 0) {
                return;
            }

            String txt = onKill.get(Rnd.get(onKillSize));
            txt = txt.replaceAll("%npc%", npc.getName());
            txt = txt.replaceAll("%player%", name);
            sayString(npc, txt);
        }

        public void sayString(L2NpcInstance npc, String txt) {
            npc.sayString(txt, type);
        }
    }

    private void cacheNpcChat() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/npc_chat.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/npc_chat.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    int type = 0;
                    int npcId = 0;
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("npc".equalsIgnoreCase(d.getNodeName())) {
                            FastTable<String> spawn = new FastTable<String>();
                            FastTable<String> attack = new FastTable<String>();
                            FastTable<String> death = new FastTable<String>();
                            FastTable<String> kill = new FastTable<String>();

                            NamedNodeMap attrs = d.getAttributes();
                            npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            type = Integer.parseInt(attrs.getNamedItem("type").getNodeValue());
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("onSpawn".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String[] text = attrs.getNamedItem("chat").getNodeValue().split(";");
                                    for (String phrase : text) {
                                        if (phrase.length() == 0) {
                                            continue;
                                        }

                                        spawn.add(phrase);
                                    }
                                }
                                if ("onAttack".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String[] text = attrs.getNamedItem("chat").getNodeValue().split(";");
                                    for (String phrase : text) {
                                        if (phrase.length() == 0) {
                                            continue;
                                        }

                                        attack.add(phrase);
                                    }
                                }
                                if ("onDeath".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String[] text = attrs.getNamedItem("chat").getNodeValue().split(";");
                                    for (String phrase : text) {
                                        if (phrase.length() == 0) {
                                            continue;
                                        }

                                        death.add(phrase);
                                    }
                                }
                                if ("onKill".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String[] text = attrs.getNamedItem("chat").getNodeValue().split(";");
                                    for (String phrase : text) {
                                        if (phrase.length() == 0) {
                                            continue;
                                        }

                                        kill.add(phrase);
                                    }
                                }
                            }
                            _cachedNpcChat.put(npcId, new NpcChat(spawn, attack, death, kill, type));
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: " + e.toString());
        }
        _log.config("CustomServerData: Npc Chat, cached " + _cachedNpcChat.size() + " npcs.");
    }

    public NpcChat getNpcChat(int id) {
        return _cachedNpcChat.get(id);
    }
    // чест дроп
    private static FastMap<Integer, FastList<EventReward>> _chestDrop = new FastMap<Integer, FastList<EventReward>>().shared("CustomServerData._chestDrop");

    public void cacheChestsDrop() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/chests_drop.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/chests_drop.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    int type = 0;
                    int item_id = 0;
                    int item_count = 0;
                    int item_chance = 0;
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("chest".equalsIgnoreCase(d.getNodeName())) {
                            FastList<EventReward> reward = new FastList<EventReward>();
                            NamedNodeMap attrs = d.getAttributes();
                            type = Integer.parseInt(attrs.getNamedItem("type").getNodeValue());
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("item".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    item_id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                                    item_count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
                                    item_chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
                                    reward.add(new EventReward(item_id, item_count, item_chance));
                                }
                            }
                            _chestDrop.put(type, reward);
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: cacheChestsDrop " + e.toString());
        }
    }

    public FastList<EventReward> getChestDrop(int cat) {
        return _chestDrop.get(cat);
    }

    // специальный дроп
    public static class SpecialDrop {

        String name;
        CreatureSay welcome;
        long begin;
        long end;

        public SpecialDrop(String name, String welcome, long begin, long end) {
            this.name = name;
            this.welcome = new CreatureSay(0, 18, "", welcome);
            this.begin = begin;
            this.end = end;
        }
    }

    public static class SpecialDropReward {

        int id = 0;
        int count = 0;
        int chance = 0;
        int autoloot = 0;
        int announce = 0;
        String text = "";

        public SpecialDropReward(int id, int count, int chance, int autoloot, int announce, String text) {
            this.id = id;
            this.count = count;
            this.chance = chance;
            this.autoloot = autoloot;
            this.announce = announce;
            this.text = text;
        }

        public int getCount() {
            if (count > 1) {
                return Rnd.get(1, count);
            }

            return 1;
        }
    }
    private static FastMap<SpecialDrop, FastList<SpecialDropReward>> _specialDrop = new FastMap<SpecialDrop, FastList<SpecialDropReward>>().shared("CustomServerData._specialDrop");

    private void cacheSpecialDrop() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/special_drop.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/special_drop.xml doesn't exist");
                return;
            }

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    String name = "";
                    String time = "";
                    String welcome = "";

                    long begin;
                    long end;
                    Date start = null;
                    Date finish = null;

                    int item_id = 0;
                    int item_count = 0;
                    int item_chance = 0;
                    int autoloot = 0;
                    int announce = 0;
                    String announce_text = "";

                    SpecialDrop sDrop = null;
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("drop".equalsIgnoreCase(d.getNodeName())) {
                            FastList<SpecialDropReward> reward = new FastList<SpecialDropReward>();
                            NamedNodeMap attrs = d.getAttributes();
                            name = attrs.getNamedItem("name").getNodeValue();
                            time = attrs.getNamedItem("time").getNodeValue();
                            welcome = attrs.getNamedItem("welcome").getNodeValue();
                            try {
                                String[] period = time.split("~");
                                start = formatter.parse(period[0]);
                                finish = formatter.parse(period[1]);
                            } catch (final ParseException e) {
                                _log.warning("CustomServerData [ERROR]: cacheSpecialDrop: time " + time);
                                _log.warning("TRACE: " + e.toString());
                                continue;
                            }
                            begin = start.getTime();
                            end = finish.getTime();

                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("item".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    item_id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                                    item_count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
                                    item_chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
                                    try {
                                        autoloot = Integer.parseInt(attrs.getNamedItem("autoloot").getNodeValue());
                                    } catch (Exception e) {
                                        autoloot = 0;
                                    }
                                    try {
                                        announce_text = attrs.getNamedItem("text").getNodeValue();
                                        if (announce_text != null) {
                                            announce = 1;
                                        }
                                    } catch (Exception e) {
                                        announce = 0;
                                    }
                                    reward.add(new SpecialDropReward(item_id, item_count, item_chance, autoloot, announce, announce_text));
                                }
                            }
                            if (!reward.isEmpty()) {
                                sDrop = new SpecialDrop(name, welcome, begin, end);
                            }

                            if (sDrop != null) {
                                _specialDrop.put(sDrop, reward);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: cacheSpecialDrop " + e.toString());
        }
        _log.config("CustomServerData: Special Drop, cached " + _specialDrop.size() + " event.");
    }

    public void showSpecialDropWelcome(L2PcInstance player) {
        SpecialDrop key = null;
        FastList<SpecialDropReward> value = null;
        long now = System.currentTimeMillis();
        for (FastMap.Entry<SpecialDrop, FastList<SpecialDropReward>> e = _specialDrop.head(), end = _specialDrop.tail(); (e = e.getNext()) != end;) {
            key = e.getKey(); // No typecast necessary.
            value = e.getValue(); // No typecast necessary.
            if (key == null || value == null) {
                continue;
            }

            if (now < key.begin || now > key.end) {
                continue;
            }

            player.sendUserPacket(key.welcome);
        }
        key = null;
        value = null;
    }

    public void manageSpecialDrop(L2PcInstance player, L2Attackable mob) {
        SpecialDrop key = null;
        L2ItemInstance item = null;
        FastList<SpecialDropReward> value = null;
        long now = System.currentTimeMillis();
        for (FastMap.Entry<SpecialDrop, FastList<SpecialDropReward>> e = _specialDrop.head(), end = _specialDrop.tail(); (e = e.getNext()) != end;) {
            key = e.getKey(); // No typecast necessary.
            value = e.getValue(); // No typecast necessary.
            if (key == null || value == null) {
                continue;
            }

            if (now < key.begin || now > key.end) {
                continue;
            }

            SpecialDropReward reward = null;
            for (FastList.Node<SpecialDropReward> n = value.head(), endv = value.tail(); (n = n.getNext()) != endv;) {
                reward = n.getValue(); // No typecast necessary.
                if (reward == null) {
                    continue;
                }

                if (Rnd.get(100) < reward.chance) {
                    if (reward.autoloot > 0) {
                        player.addItem("SpecialDrop", reward.id, reward.getCount(), player, true);
                    } else {
                        int x = mob.getX() + Rnd.get(30);
                        int y = mob.getY() + Rnd.get(30);
                        int z = GeoData.getInstance().getSpawnHeight(x, y, mob.getZ(), mob.getZ(), null);

                        item = ItemTable.getInstance().createItem("Loot", reward.id, reward.getCount(), player, mob);
                        item.setProtected(false);
                        item.setPickuper(player);
                        item.dropMe(mob, x, y, z);

                        // Add drop to auto destroy item task
                        if (Config.AUTODESTROY_ITEM_AFTER > 0 && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId())) {
                            ItemsAutoDestroy.getInstance().addItem(item);
                        }
                    }

                    if (reward.announce > 0) {
                        Announcements.getInstance().announceToAll(reward.text.replace("%player%", player.getName()));
                    }
                }
            }
            reward = null;
        }
        key = null;
        item = null;
        value = null;
    }

    public static class NpcPenalty {

        public Location loc = null;
        public FastList<Integer> item_list = null;

        public NpcPenalty(Location loc, FastList<Integer> item_list) {
            this.loc = loc;
            this.item_list = item_list;
        }
    }
    private static FastMap<FastList<Integer>, NpcPenalty> _npcPenaltyItems = new FastMap<FastList<Integer>, NpcPenalty>().shared("CustomServerData._npcPenaltyItems");

    private void cacheNpcPenaltyItems() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/npc_penalty_items.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/npc_penalty_items.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    String name = "";
                    String coord = "";
                    String[] coords = null;

                    String npc = "";
                    String item = "";
                    FastList<Integer> npc_list = null;
                    FastList<Integer> item_list = null;

                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("penalty".equalsIgnoreCase(d.getNodeName())) {
                            FastList<SpecialDropReward> reward = new FastList<SpecialDropReward>();
                            NamedNodeMap attrs = d.getAttributes();
                            name = attrs.getNamedItem("name").getNodeValue();

                            Location loc = null;
                            coord = attrs.getNamedItem("loc").getNodeValue();
                            coords = coord.split(",");
                            loc = new Location(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));

                            npc_list = new FastList<Integer>();
                            item_list = new FastList<Integer>();
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("npc".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    npc = attrs.getNamedItem("list").getNodeValue();
                                    if (npc.isEmpty()) {
                                        continue;
                                    }
                                    String[] npcs = npc.split(",");
                                    {
                                        for (String npcId : npcs) {
                                            if (npcId.isEmpty()) {
                                                continue;
                                            }
                                            npc_list.add(Integer.parseInt(npcId));
                                        }
                                    }
                                }
                                if ("item".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    item = attrs.getNamedItem("list").getNodeValue();
                                    if (item.isEmpty()) {
                                        continue;
                                    }
                                    String[] items = item.split(",");
                                    {
                                        for (String itemId : items) {
                                            if (itemId.isEmpty()) {
                                                continue;
                                            }
                                            item_list.add(Integer.parseInt(itemId));
                                        }
                                    }
                                }
                            }

                            if (!npc_list.isEmpty() && !item_list.isEmpty()) {
                                _npcPenaltyItems.put(npc_list, new NpcPenalty(loc, item_list));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: cacheNpcPenaltyItems " + e.toString());
        }
        _log.config("CustomServerData: Npc Penalty Items, cached " + _npcPenaltyItems.size() + " penaltyes.");
    }

    public void setPenaltyItems(int id, L2NpcTemplate template) {
        NpcPenalty penalty = null;
        FastList<Integer> list = null;
        for (FastMap.Entry<FastList<Integer>, NpcPenalty> e = _npcPenaltyItems.head(), end = _npcPenaltyItems.tail(); (e = e.getNext()) != end;) {
            list = e.getKey(); // No typecast necessary.
            penalty = e.getValue(); // No typecast necessary.
            if (list == null || penalty == null) {
                continue;
            }

            if (!list.contains(id)) {
                continue;
            }

            template.setPenaltyLoc(penalty.loc);
            template.setPenaltyItems(penalty.item_list);
        }
        penalty = null;
        list = null;
    }

    //
    public static class PcNpc {

        public int id;
        public int npcId;
        public int hero;
        public int male;
        public FastList<Integer> items = new FastList<Integer>();

        public PcNpc(int id, int npcId, int hero, int male) {
            this.id = id;
            this.npcId = npcId;
            this.hero = hero;
            this.male = male;
        }

        public void addItems(FastList<Integer> item_list) {
            this.items = item_list;
        }
    }
    private static FastMap<Integer, PcNpc> _pcNpcs = new FastMap<Integer, PcNpc>().shared("CustomServerData._pcNpcs");

    public FastList<Integer> getPcNpcIds() {
        FastList<Integer> ids = new FastList<Integer>();
        for (FastMap.Entry<Integer, PcNpc> e = _pcNpcs.head(), end = _pcNpcs.tail(); (e = e.getNext()) != end;) {
            Integer id = e.getKey(); // No typecast necessary.
            if (id == null || e.getValue() == null) {
                continue;
            }
            ids.add(id);
        }

        return ids;
    }

    private void loadPcNpcs() {
        if (!Config.ALLOW_PC_NPC) {
            return;
        }
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/pc_npc.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/pc_npc.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("npc".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            //int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            int npc_id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            int class_id = Integer.parseInt(attrs.getNamedItem("classId").getNodeValue());
                            int hero = Integer.parseInt(attrs.getNamedItem("hero").getNodeValue());
                            int sex = Integer.parseInt(attrs.getNamedItem("sex").getNodeValue());
                            PcNpc pn = new PcNpc(class_id, npc_id, hero, sex);
                            FastList<Integer> equip = new FastList<Integer>();
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("equipment".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String[] items = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String item : items) {
                                        if (item.equalsIgnoreCase("")) {
                                            continue;
                                        }

                                        equip.add(Integer.parseInt(item));
                                    }
                                }
                            }
                            pn.addItems(equip);
                            _pcNpcs.put(npc_id, pn);
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: loadPcNpcs() " + e.toString());
        }
        _log.config("CustomServerData: Pc Npcs, loaded " + _pcNpcs.size() + " npc's.");
    }

    public void spawnPcNpc(int id, int x, int y, int z, int heading, L2NpcTemplate template) {
        PcNpc pn = _pcNpcs.get(id);
        L2PcNpcInstance fantom = L2PcNpcInstance.restorePcNpc(IdFactory.getInstance().getNextId(), pn.id, pn.hero == 1, pn.male == 0, template, pn.npcId);
        fantom.setName(template.name);
        fantom.setTitle(template.title);

        fantom.setHeading(heading);
        fantom.setXYZInvisible(x + Rnd.get(60), y, z);
        wearPcNpc(fantom, pn.items);
    }

    private void wearPcNpc(L2PcInstance fantom, FastList<Integer> items) {

        Integer item_id = null;
        for (FastList.Node<Integer> n = items.head(), endv = items.tail(); (n = n.getNext()) != endv;) {
            item_id = n.getValue(); // No typecast necessary.
            if (item_id == null) {
                continue;
            }
            fantom.getInventory().equipItemAndRecord(ItemTable.getInstance().createDummyItem(item_id));
        }

        fantom.spawnMe();
        fantom.setOnlineStatus(false);
    }
    //
    private static FastMap<Integer, FastList<Integer>> _specilaTeleports = new FastMap<Integer, FastList<Integer>>().shared("CustomServerData._specilaTeleports");

    public void addTeleItem(int teleIt, FastList<Integer> itemId) {
        _specilaTeleports.put(teleIt, itemId);
    }

    public boolean isSpecialTeleDenied(L2PcInstance player, int teleIt) {
        if (!_specilaTeleports.containsKey(teleIt)) {
            return false;
        }

        return findForbiddenItems(player, _specilaTeleports.get(teleIt));
    }

    public boolean isSpecialForrbid(int teleIt, int itemId) {
        if (teleIt == 0 || !_specilaTeleports.containsKey(teleIt)) {
            return false;
        }

        return findForbiddenItems2(itemId, _specilaTeleports.get(teleIt));
    }

    private boolean findForbiddenItems2(int itemId, FastList<Integer> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (FastList.Node<Integer> k = list.head(), endk = list.tail(); (k = k.getNext()) != endk;) {
            Integer item = k.getValue();
            if (item == null) {
                continue;
            }

            if (itemId == item) {
                return true;
            }
        }
        return false;
    }

    private boolean findForbiddenItems(L2PcInstance player, FastList<Integer> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (FastList.Node<Integer> k = list.head(), endk = list.tail(); (k = k.getNext()) != endk;) {
            Integer item = k.getValue();
            if (item == null) {
                continue;
            }

            if (hasForbiddenItem(player.getInventory().getItemByItemId(item))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasForbiddenItem(L2ItemInstance item) {
        if (item == null) {
            return false;
        }

        return item.isEquipped();
    }
    //спецдроп в пвп зоне
    private static FastMap<Integer, FastList<EventReward>> _pvpRewardZones = new FastMap<Integer, FastList<EventReward>>().shared("CustomServerData._pvpRewardZones");

    public void addPvpReward(int id, FastList<EventReward> rewards) {
        _pvpRewardZones.put(id, rewards);
    }

    public void checkPvpReward(L2PcInstance player) {
        if (player == null) {
            return;
        }

        if (_pvpRewardZones.containsKey(player.getPpvRewardZone())) {
            checkPvpReward2(player, _pvpRewardZones.get(player.getPpvRewardZone()));
        }
    }

    private void checkPvpReward2(L2PcInstance player, FastList<EventReward> rewards) {
        if (player == null || rewards == null) {
            return;
        }
        EventReward reward = null;
        for (FastList.Node<EventReward> k = rewards.head(), endk = rewards.tail(); (k = k.getNext()) != endk;) {
            reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                player.addItem("pvp_bonus", reward.id, reward.count, player, true);
            }
        }
        reward = null;
    }
    //спецдроп с сундуков
    public static FastList<Integer> _allSpecChests = new FastList<Integer>();
    private static FastMap<String, FastList<EventReward>> _chestSpecialDrop = new FastMap<String, FastList<EventReward>>().shared("CustomServerData._chestSpecialDrop");

    private void cacheChestDrop() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/chests_drop_mod.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/chests_drop_mod.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("chest".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            int chest_id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            int skillId = Integer.parseInt(attrs.getNamedItem("skill").getNodeValue());
                            int skillLvl = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
                            String id_lvl = chest_id + "" + skillId + "" + skillLvl;

                            FastList<EventReward> rewards = new FastList<EventReward>();
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("item".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                                    int count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
                                    int chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
                                    rewards.add(new EventReward(id, count, chance));
                                }
                            }

                            _allSpecChests.add(chest_id);
                            _chestSpecialDrop.put(id_lvl, rewards);
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: cacheChestDrop() " + e.toString());
        }
        _log.config("CustomServerData: Chest Drop Mod, loaded " + _chestSpecialDrop.size() + " drop categories.");
    }

    public boolean isSpecChest(int id) {
        return _allSpecChests.contains(id);
    }

    public void checkChestReward(L2PcInstance player, String id_lvl) {
        if (player == null) {
            return;
        }

        checkChestReward2(player, _chestSpecialDrop.get(id_lvl));
    }

    private void checkChestReward2(L2PcInstance player, FastList<EventReward> rewards) {
        if (player == null || rewards == null) {
            return;
        }
        EventReward reward = null;
        for (FastList.Node<EventReward> k = rewards.head(), endk = rewards.tail(); (k = k.getNext()) != endk;) {
            reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                player.addItem("pvp_bonus", reward.id, reward.count, player, true);
            }
        }
        reward = null;
    }
    //
    private static FastMap<Integer, FastList<L2Skill>> _castleSkills = new FastMap<Integer, FastList<L2Skill>>().shared("CustomServerData._castleSkills");
    private static FastMap<Integer, FastList<L2Skill>> _castleSkillsL = new FastMap<Integer, FastList<L2Skill>>().shared("CustomServerData._castleSkills");

    private void cacheCastleSkills() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/castle_skills.xml");
            if (!file.exists()) {
                _log.config("CustomServerData [ERROR]: data/castle_skills.xml doesn't exist");
                return;
            }

            /*
             <list>
             <castle id='1'>
             <leader skills='4552,4;4554,4'/>
             <members skills='4551,4;4553,4'/>
             </castle>
             </list>
             */
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("castle".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            int castleId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());

                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("leader".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    FastList<L2Skill> skills = new FastList<L2Skill>();
                                    String[] idS = attrs.getNamedItem("skills").getNodeValue().split(";");
                                    for (String data : idS) {
                                        if (data.equalsIgnoreCase("")) {
                                            continue;
                                        }

                                        String[] idlvl = data.split(",");
                                        L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(idlvl[0]), Integer.parseInt(idlvl[1]));
                                        skills.add(skill);
                                    }
                                    _castleSkillsL.put(castleId, skills);
                                }
                                if ("member".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    FastList<L2Skill> skills = new FastList<L2Skill>();
                                    String[] idS = attrs.getNamedItem("skills").getNodeValue().split(";");
                                    for (String data : idS) {
                                        if (data.equalsIgnoreCase("")) {
                                            continue;
                                        }

                                        String[] idlvl = data.split(",");
                                        L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(idlvl[0]), Integer.parseInt(idlvl[1]));
                                        skills.add(skill);
                                    }
                                    _castleSkills.put(castleId, skills);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CustomServerData [ERROR]: cacheCastleSkills() " + e.toString());
        }
        _log.config("CustomServerData: Castle Skills, loaded " + _castleSkills.size() + " member and " + _castleSkillsL.size() + " leader castles.");
    }

    public FastList<L2Skill> getCastleMembersSkills(int catleId) {
        return _castleSkills.get(catleId);
    }

    public FastList<L2Skill> getCastleLeaderSkills(int catleId) {
        return _castleSkillsL.get(catleId);
    }

    public void giveCastleBonusSkill(L2PcInstance player, int castleId, boolean add) {
        if (player == null) {
            return;
        }

        if (player.isClanLeader()) {
            giveCastleSkill(player, add, _castleSkillsL.get(castleId));
        } else {
            giveCastleSkill(player, add, _castleSkills.get(castleId));
        }
    }

    private void giveCastleSkill(L2PcInstance player, boolean add, FastList<L2Skill> skills) {
        if (skills == null
                || skills.isEmpty()) {
            return;
        }

        for (L2Skill skill : skills) {
            if (skill == null) {
                continue;
            }

            if (add) {
                player.addSkill(skill, false);
            } else {
                player.removeSkill(skill);
            }
        }
    }
}
