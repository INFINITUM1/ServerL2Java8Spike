package net.sf.l2j.gameserver.datatables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.util.Online;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;

public class FakePlayersTablePlus {

    private static final Logger _log = AbstractLogger.getLogger(FakePlayersTablePlus.class.getName());
    // полный список всех obj_Id ботов
    private static int _fakesCount = 0;
    private static int _fakesLimit = 0;
    private static String _fakeAcc = "fh4f#67$kl";
    private static FastMap<Integer, L2Fantome> _fakes = new FastMap<Integer, L2Fantome>().shared("FakePlayersTablePlus._fakes");
    /** √ородские*/
    private volatile int _fakesTownTotal = 0;
    // координаты
    private static int _locsCount = 0;
    private static FastList<Location> _fakesTownLoc = new FastList<Location>();
    // сто€чие в городах: волна_спауна,список_заспавненных
    private static Map<Integer, ConcurrentLinkedQueue<L2PcInstance>> _fakesTown = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<L2PcInstance>>();
    // сид€чие кучки кланов в городах: клан,список_заспавненных
    private static Map<Integer, ConcurrentLinkedQueue<L2PcInstance>> _fakesTownClan = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<L2PcInstance>>();
    private static Map<Integer, ConcurrentLinkedQueue<Integer>> _fakesTownClanList = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Integer>>();
    // сеты
    private static int _setsCount = 0;
    private static FastList<L2Set> _sets = new FastList<L2Set>();
    private static int _setsArcherCount = 0;
    private static FastList<L2Set> _setsArcher = new FastList<L2Set>();
    // рек, если любишь маму
    private static ConcurrentLinkedQueue<L2PcInstance> _fakesTownRec = new ConcurrentLinkedQueue<L2PcInstance>();
    /*** ќлимп**/
    // сто€чие возле олимп столбов
    private static ConcurrentLinkedQueue<L2PcInstance> _fakesOly = new ConcurrentLinkedQueue<L2PcInstance>();
    // сеты
    private static int _setsOlyCount = 0;
    private static FastList<L2Set> _setsOly = new FastList<L2Set>();
    /** дополнительно**/
    //цвета
    private static int _nameColCount = 0;
    private static int _titleColCount = 0;
    private static FastList<Integer> _nameColors = new FastList<Integer>();
    private static FastList<Integer> _titleColors = new FastList<Integer>();
    private static FakePlayersTablePlus _instance;
    //фразы
    private static int _fakesEnchPhsCount = 0;
    private static FastList<String> _fakesEnchPhrases = new FastList<String>();
    private static int _fakesLastPhsCount = 0;
    private static FastList<String> _fakesLastPhrases = new FastList<String>();

    public static FakePlayersTablePlus getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new FakePlayersTablePlus();
        _instance.load();
    }

    public FakePlayersTablePlus() {
    }

    public void load() {
        parceArmors();
        parceArcherArmors();
        parceOlyArmors();
        parceColors();
        cacheLastPhrases();
        if (Config.ALLOW_FAKE_PLAYERS_PLUS) {
            parceTownLocs();
            parceTownClans();
            parceTownRecs();
            cacheFantoms();
            cacheEnchantPhrases();

            _fakesLimit = Config.FAKE_PLAYERS_PLUS_COUNT_FIRST + Config.FAKE_PLAYERS_PLUS_COUNT_NEXT + 10;
            _fakesTown.put(1, new ConcurrentLinkedQueue<L2PcInstance>());
            _fakesTown.put(2, new ConcurrentLinkedQueue<L2PcInstance>());
        }
    }

    private void cacheFantoms() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                String name = "";
                String new_name = "";
                Connect con = null;
                PreparedStatement st = null;
                //PreparedStatement st2 = null;
                //PreparedStatement st3 = null;
                ResultSet rs = null;
                //ResultSet rs2 = null;
                L2PcInstance fantom = null;
                try {
                    con = L2DatabaseFactory.get();
                    con.setTransactionIsolation(1);
                    st = con.prepareStatement("SELECT obj_Id,char_name,title,x,y,z,clanid FROM characters WHERE account_name = ?");
                    st.setString(1, _fakeAcc);
                    rs = st.executeQuery();
                    rs.setFetchSize(250);
                    while (rs.next()) {
                        //System.out.println("##1# " + name);
                        name = rs.getString("char_name");
//
						/*st2 = con.prepareStatement("SELECT char_name FROM `characters` WHERE `char_name` = ? AND `account_name` <> ?");
                        st2.setString(1, name);
                        st2.setString(2, _fakeAcc);
                        rs2 = st2.executeQuery();
                        if (rs2.next())
                        {
                        System.out.println("##2#!!!!!# " + name);
                        st3 = con.prepareStatement("UPDATE characters SET char_name=? WHERE `char_name` = ? AND `account_name` = ?");
                        switch(Rnd.get(8))
                        {
                        case 0:
                        new_name = "zx" + name + "xz";
                        break;
                        case 1:
                        new_name = "Oo" + name + "oO";
                        break;
                        case 2:
                        new_name = "XX" + name + "XX";
                        break;
                        case 3:
                        new_name = "jj" + name + "jj";
                        break;
                        case 4:
                        new_name = "oOo" + name + "oOo";
                        break;
                        case 5:
                        new_name = "xXx" + name + "xXx";
                        break;
                        case 6:
                        new_name = "zZz" + name + "zZz";
                        break;
                        case 7:
                        new_name = "zX" + name + "Xz";
                        break;
                        case 8:
                        new_name = "xlx" + name + "xlx";
                        break;
                        }
                        st3.setString(1, new_name);
                        st3.setString(2, name);
                        st3.setString(3, _fakeAcc);
                        st3.execute();
                        Close.S(st3);
                        }
                        Close.SR(st2, rs2);*/
//
                        _fakes.put(rs.getInt("obj_Id"), new L2Fantome(name, rs.getString("title"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("clanid")));
                    }
                } catch (Exception e) {
                    _log.warning("FakePlayersTablePlus: could not load chars from DB: " + e);
                } finally {
                    Close.CSR(con, st, rs);
                }
                _log.info("FakePlayersTablePlus: Cached " + _fakes.size() + " players.");
                if (!_fakes.isEmpty()) {
                    _fakesCount = _fakes.size() - 1;
                    ThreadPoolManager.getInstance().scheduleGeneral(new FantomTask(1), Config.FAKE_PLAYERS_PLUS_DELAY_FIRST); //360000
                }
            }
        }).start();
    }

    public class FantomTask implements Runnable {

        public int task;

        /**
        1 - спаун первой волны
        2 - спаун 2 волны
         **/
        public FantomTask(int task) {
            this.task = task;
        }

        public void run() {
            switch (task) {
                case 1:
                    _log.info("FakePlayersTablePlus: 1st wave, spawn started.");
                    int count = 0;
                    int fakeObjId = 0;
                    L2PcInstance fantom = null;
                    while (count < Config.FAKE_PLAYERS_PLUS_COUNT_FIRST) {
                        //_log.info("FakePlayersTablePlus: #1### " + count + " player.");
                        fakeObjId = getRandomFake();
                        //_log.info("FakePlayersTablePlus: #2### " + fakeObjId + " player.");
                        if (_fakesTown.get(1).contains(fakeObjId)) {
                            continue;
                        }
                        //_log.info("FakePlayersTablePlus: #3### " + count + " player.");

                        fantom = restoreFake(fakeObjId);
                        //_log.info("FakePlayersTablePlus: #4### " + count + " player.");
                        wearFantom(fantom);

                        if (Config.SOULSHOT_ANIM && Rnd.get(100) < 45) {
                            try {
                                Thread.sleep(900);
                            } catch (InterruptedException e) {
                            }

                            if (Rnd.get(100) < 3) {
                                fantom.sitDown();
                            }

                            fantom.broadcastPacket(new MagicSkillUser(fantom, fantom, 2154, 1, 0, 0));
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                            }
                            fantom.broadcastPacket(new MagicSkillUser(fantom, fantom, 2164, 1, 0, 0));
                        }

                        //_log.info("FakePlayersTablePlus: #5### " + count + " player.");
                        _fakesTown.get(1).add(fantom);
                        //_log.info("FakePlayersTablePlus: #6### " + count + " player.");
                        try {
                            Thread.sleep(Config.FAKE_PLAYERS_PLUS_DELAY_SPAWN_FIRST);
                        } catch (InterruptedException e) {
                        }
                        count++;
                        _fakesTownTotal++;
                        //_log.info("FakePlayersTablePlus: 1 wave, spawned " + count + " player.");
                    }
                    _log.info("FakePlayersTablePlus: 1st wave, spawned " + count + " players.");
                    Online.getInstance().checkMaxOnline();
                    ThreadPoolManager.getInstance().scheduleGeneral(new FantomTaskDespawn(1), Config.FAKE_PLAYERS_PLUS_DESPAWN_FIRST);
                    ThreadPoolManager.getInstance().scheduleGeneral(new FantomTask(2), Config.FAKE_PLAYERS_PLUS_DELAY_NEXT);
                    ThreadPoolManager.getInstance().scheduleGeneral(new Social(), 12000);
                    ThreadPoolManager.getInstance().scheduleGeneral(new CheckCount(), 300000);
                    break;
                case 2:
                    _log.info("FakePlayersTablePlus: 2nd wave, spawn started.");
                    int count2 = 0;
                    int fakeObjId2 = 0;
                    L2PcInstance fantom2 = null;
                    while (count2 < Config.FAKE_PLAYERS_PLUS_COUNT_NEXT) {
                        fakeObjId2 = getRandomFake();
                        if (_fakesTown.get(2).contains(fakeObjId2)) {
                            continue;
                        }

                        fantom2 = restoreFake(fakeObjId2);
                        wearFantom(fantom2);

                        if (Config.SOULSHOT_ANIM && Rnd.get(100) < 45) {
                            try {
                                Thread.sleep(900);
                            } catch (InterruptedException e) {
                            }

                            if (Rnd.get(100) < 3) {
                                fantom2.sitDown();
                            }

                            fantom2.broadcastPacket(new MagicSkillUser(fantom2, fantom2, 2154, 1, 0, 0));
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                            }
                            fantom2.broadcastPacket(new MagicSkillUser(fantom2, fantom2, 2164, 1, 0, 0));
                        }
                        _fakesTown.get(2).add(fantom2);
                        try {
                            Thread.sleep(Config.FAKE_PLAYERS_PLUS_DELAY_SPAWN_NEXT);
                        } catch (InterruptedException e) {
                        }
                        count2++;
                        _fakesTownTotal++;
                    }
                    _log.info("FakePlayersTablePlus: 2nd wave, spawned " + count2 + " players.");
                    Online.getInstance().checkMaxOnline();
                    ThreadPoolManager.getInstance().scheduleGeneral(new FantomTaskDespawn(2), Config.FAKE_PLAYERS_PLUS_DESPAWN_NEXT);
                    break;
            }
        }
    }

    public class FantomTaskDespawn implements Runnable {

        public int task;

        /**
        1 - Despawn первой волны
        1 - Despawn первой волны
         **/
        public FantomTaskDespawn(int task) {
            this.task = task;
        }

        public void run() {
            Location loc = null;
            L2PcInstance next = null;
            ConcurrentLinkedQueue<L2PcInstance> players = _fakesTown.get(task);
            for (L2PcInstance fantom : players) {
                if (fantom == null) {
                    continue;
                }

                loc = fantom.getFakeLoc();
                fantom.kick();
                fantom.setOnlineStatus(false);
                _fakesTown.get(task).remove(fantom);
                _fakesTownTotal--;

                try {
                    Thread.sleep((task == 1 ? Config.FAKE_PLAYERS_PLUS_DELAY_DESPAWN_FIRST : Config.FAKE_PLAYERS_PLUS_DELAY_DESPAWN_NEXT));
                } catch (InterruptedException e) {
                }

                if (_fakesTownTotal > _fakesLimit) {
                    continue;
                }

                next = restoreFake(getRandomFakeNext());
                next.setFakeLoc(loc.x, loc.y, loc.z);
                next.setXYZInvisible(loc.x + Rnd.get(60), loc.y + Rnd.get(60), loc.z);

                wearFantom(next);

                if (Config.SOULSHOT_ANIM && Rnd.get(100) < 45) {
                    try {
                        Thread.sleep(900);
                    } catch (InterruptedException e) {
                    }

                    if (Rnd.get(100) < 3) {
                        next.sitDown();
                    }

                    next.broadcastPacket(new MagicSkillUser(next, next, 2154, 1, 0, 0));

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                    }
                    next.broadcastPacket(new MagicSkillUser(next, next, 2164, 1, 0, 0));
                }

                _fakesTown.get(task).add(next);
                _fakesTownTotal++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            loc = null;
            next = null;
            ThreadPoolManager.getInstance().scheduleGeneral(new FantomTaskDespawn(1), (task == 1 ? Config.FAKE_PLAYERS_PLUS_DESPAWN_FIRST : Config.FAKE_PLAYERS_PLUS_DESPAWN_NEXT));
        }
    }

    public class Social implements Runnable {

        public Social() {
        }

        public void run() {
            TextBuilder tb = new TextBuilder();
            for (Map.Entry<Integer, ConcurrentLinkedQueue<L2PcInstance>> entry : _fakesTown.entrySet()) {
                Integer wave = entry.getKey();
                ConcurrentLinkedQueue<L2PcInstance> players = entry.getValue();
                if (wave == null || players == null) {
                    continue;
                }

                if (players.isEmpty()) {
                    continue;
                }

                int count = 0;
                for (L2PcInstance player : players) {
                    if (Rnd.get(100) < 65) {
                        switch (Rnd.get(2)) {
                            case 0:
                            case 1:
                                L2ItemInstance wpn = player.getActiveWeaponInstance();
                                int enhchant = wpn.getEnchantLevel();
                                int nextench = enhchant + 1;
                                if (Rnd.get(100) < Config.ENCHANT_ALT_MAGICCAHNCE && enhchant <= Config.ENCHANT_MAX_WEAPON) {
                                    wpn.setEnchantLevel(nextench);
                                } else if (Rnd.get(100) < 70) {
                                    wpn.setEnchantLevel(3);
                                    if (nextench > 13 && Rnd.get(100) < 2) {
                                        tb.append("!");
                                        for (int i = Rnd.get(2, 13); i > 0; i--) {
                                            tb.append("!");
                                        }
                                        player.sayString(getRandomEnchantPhrase() + tb.toString(), 1);
                                        tb.clear();
                                    }
                                }
                                player.sendItems(true);
                                player.broadcastUserInfo();
                                break;
                            case 2:
                                if (Rnd.get(100) < 5) {
                                    player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(player.getX() + Rnd.get(30), player.getY() + Rnd.get(30), player.getZ(), 0));
                                }
                                break;
                        }
                        try {
                            Thread.sleep(Rnd.get(500, 1500));
                        } catch (InterruptedException e) {
                        }
                        count++;
                    }
                    if (count > 55) {
                        break;
                    }
                }
            }
            tb.clear();
            tb = null;
            ThreadPoolManager.getInstance().scheduleGeneral(new Social(), 12000);
        }
    }

    private String getRandomEnchantPhrase() {
        return _fakesEnchPhrases.get(Rnd.get(_fakesEnchPhsCount));
    }

    public String getRandomLastPhrase() {
        return _fakesLastPhrases.get(Rnd.get(_fakesLastPhsCount));
    }

    public class CheckCount implements Runnable {

        public CheckCount() {
        }

        public void run() {
            for (Map.Entry<Integer, ConcurrentLinkedQueue<L2PcInstance>> entry : _fakesTown.entrySet()) {
                Integer wave = entry.getKey();
                ConcurrentLinkedQueue<L2PcInstance> players = entry.getValue();
                if (wave == null || players == null) {
                    continue;
                }

                if (players.isEmpty()) {
                    continue;
                }

                int limit = wave == 1 ? Config.FAKE_PLAYERS_PLUS_COUNT_FIRST : Config.FAKE_PLAYERS_PLUS_COUNT_NEXT;
                int overflow = players.size() - limit;
                if (overflow < 1) {
                    continue;
                }

                for (L2PcInstance fantom : players) {
                    fantom.kick();
                    fantom.setOnlineStatus(false);
                    _fakesTown.get(wave).remove(fantom);
                    _fakesTownTotal--;

                    overflow--;
                    if (overflow == 0) {
                        break;
                    }
                }
            }
            ThreadPoolManager.getInstance().scheduleGeneral(new CheckCount(), 300000);
        }
    }

    public void wearFantom(L2PcInstance fantom) {
        //System.out.println("##wearFantom###1#");
        L2Set set = getRandomSet();
        //System.out.println("##wearFantom###2#");
        L2ItemInstance body = ItemTable.getInstance().createDummyItem(set.body);
        L2ItemInstance gaiters = ItemTable.getInstance().createDummyItem(set.gaiters);
        L2ItemInstance gloves = ItemTable.getInstance().createDummyItem(set.gloves);
        L2ItemInstance boots = ItemTable.getInstance().createDummyItem(set.boots);
        L2ItemInstance weapon = ItemTable.getInstance().createDummyItem(set.weapon);
        ///System.out.println("##wearFantom###3#");
        fantom.getInventory().equipItemAndRecord(body);
        fantom.getInventory().equipItemAndRecord(gaiters);
        fantom.getInventory().equipItemAndRecord(gloves);
        fantom.getInventory().equipItemAndRecord(boots);

        if (set.custom > 0) {
            L2ItemInstance custom = ItemTable.getInstance().createDummyItem(set.custom);
            fantom.getInventory().equipItemAndRecord(custom);
        }

        //System.out.println("##wearFantom###4#");
        //weapon.setEnchantLevel((Rnd.get(6) + Rnd.get(6))); // 2 кубика надежнее
        weapon.setEnchantLevel(Rnd.get(Config.FAKE_PLAYERS_ENCHANT.nick, Config.FAKE_PLAYERS_ENCHANT.title)); // 2 кубика надежнее
        if (Rnd.get(100) < 30) {
            weapon.setAugmentation(new L2Augmentation(weapon, 1067847165, 3250, 1, false));
        }
        fantom.getInventory().equipItemAndRecord(weapon);
        //System.out.println("##wearFantom###5#");

        fantom.spawnMe();
        fantom.setOnlineStatus(true);
        fantom.setAlone(true);
        //System.out.println("##wearFantom###6#");
        //System.out.println("##wearFantom###7#");
    }

    static class L2Set {

        public int body;
        public int gaiters;
        public int gloves;
        public int boots;
        public int weapon;
        public int custom;

        L2Set(int bod, int gaiter, int glove, int boot, int weapon, int custom) {
            this.body = bod;
            this.gaiters = gaiter;
            this.gloves = glove;
            this.boots = boot;
            this.weapon = weapon;
            this.custom = custom;
        }
    }

    static class L2Fantome {

        public String name;
        public String title;
        public int x;
        public int y;
        public int z;
        public int clanId;

        L2Fantome(String name, String title, int x, int y, int z, int clanId) {
            this.name = name;
            this.title = title;

            this.x = x;
            this.y = y;
            this.z = z;

            this.clanId = clanId;
        }
    }

    private void parceArmors() {
        if (!_sets.isEmpty()) {
            _sets.clear();
        }

        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./config/fake/town_sets.txt");
            if (!Data.exists()) {
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            //#верх,низ,перчатки,ботинки,оружие,аксессуар
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                String[] items = line.split(",");

                int custom = 0;
                try {
                    custom = Integer.parseInt(items[5]);
                } catch (Exception e) {
                    custom = 0;
                }
                _sets.add(new L2Set(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2]), Integer.parseInt(items[3]), Integer.parseInt(items[4]), custom));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        _setsCount = _sets.size() - 1;
    }

    private void parceArcherArmors() {
        if (!_setsArcher.isEmpty()) {
            _setsArcher.clear();
        }

        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./config/fake/archer_sets.txt");
            if (!Data.exists()) {
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            //#верх,низ,перчатки,ботинки,оружие,аксессуар
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                String[] items = line.split(",");

                int custom = 0;
                try {
                    custom = Integer.parseInt(items[5]);
                } catch (Exception e) {
                    custom = 0;
                }
                _setsArcher.add(new L2Set(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2]), Integer.parseInt(items[3]), Integer.parseInt(items[4]), custom));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        _setsArcherCount = _setsArcher.size() - 1;
    }

    private void parceOlyArmors() {
        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./config/fake/oly_sets.txt");
            if (!Data.exists()) {
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            //#верх,низ,перчатки,ботинки,оружие,аксессуар
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }
                String[] items = line.split(",");

                int custom = 0;
                try {
                    custom = Integer.parseInt(items[5]);
                } catch (Exception e) {
                    custom = 0;
                }
                _setsOly.add(new L2Set(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2]), Integer.parseInt(items[3]), Integer.parseInt(items[4]), custom));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        _setsOlyCount = _setsOly.size() - 1;
    }

    private void parceTownClans() {
        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./config/fake/town_clans.txt");
            if (!Data.exists()) {
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            int clanId = 0;
            //#ид_клана:ид_игрокa,ид_игрокa,ид_игрокa,ид_игрокa
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                String[] items = line.split(":");
                clanId = Integer.parseInt(items[0]);

                String[] pls = items[1].split(",");
                ConcurrentLinkedQueue<Integer> players = new ConcurrentLinkedQueue<Integer>();
                for (String plid : pls) {
                    players.add(Integer.parseInt(plid));
                }
                _fakesTownClanList.put(clanId, players);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    }

    private void parceTownRecs() {
        /*LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try
        {
        File Data = new File("./config/fake/town_rec.txt");
        if (!Data.exists())
        return;
        
        fr = new FileReader(Data);
        br = new BufferedReader(fr);
        lnr = new LineNumberReader(br);
        int clanId = 0;
        //#x,y,x:название_магазина,итем_ид,кол-во,цена,продажа0/покупка1,оффлайн1да/0нет
        String line;
        while((line = lnr.readLine()) != null)
        {
        if(line.trim().length() == 0 || line.startsWith("#"))
        continue;
        
        String[] items = line.split(":");
        clanId = Integer.parseInt(items[0]);
        
        String[] pls = items[1].split(",");
        ConcurrentLinkedQueue<Integer> players = new ConcurrentLinkedQueue<Integer>();
        for (String plid : pls)
        {
        players.add(Integer.parseInt(plid));
        }
        _fakesTownClanList.put(clanId, players);
        }
        }
        catch(Exception e)
        {
        e.printStackTrace();
        }
        finally
        {
        try
        {
        if (fr != null)
        fr.close();
        if (br != null)
        br.close();
        if (lnr != null)
        lnr.close();
        }
        catch(Exception e1)
        {}
        }*/
    }

    private void parceTownLocs() {
        _fakesTownLoc.clear();

        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./config/fake/town_locs.txt");
            if (!Data.exists()) {
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                String[] items = line.split(",");
                _fakesTownLoc.add(new Location(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2])));
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        _locsCount = _fakesTownLoc.size() - 1;
    }

    private void cacheEnchantPhrases() {
        _fakesEnchPhrases.clear();

        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./config/fake/phrases_enchant.txt");
            if (!Data.exists()) {
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                _fakesEnchPhrases.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        _fakesEnchPhsCount = _fakesEnchPhrases.size() - 1;
    }

    private void cacheLastPhrases() {
        _fakesLastPhrases.clear();

        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./config/fake/phrases_last.txt");
            if (!Data.exists()) {
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                _fakesLastPhrases.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        _fakesLastPhsCount = _fakesLastPhrases.size() - 1;
    }

    private L2Set getRandomSet() {
        return _sets.get(Rnd.get(_setsCount));
    }

    private L2Set getRandomArcherSet() {
        return _setsArcher.get(Rnd.get(_setsArcherCount));
    }

    private int getRandomFake() {
        return Rnd.get(511151115, 511157114);
    }

    private int getRandomFakeNext() {
        int obj = 0;
        for (int i = 6; i > 0; i--) {
            obj = Rnd.get(511151115, 511157114);

            if (_fakesTown.get(1).contains(obj) || _fakesTown.get(2).contains(obj)) {
                continue;
            }

            return obj;
        }
        return getRandomFakeNext();
    }

    private int getRandomClan() {
        return Rnd.get(511158000, 511158008);
    }

    private Location getRandomLoc() {
        /*int id = Rnd.get(_locsCount);
        Location loc = _fakesTownLoc.remove(id);
        return loc;*/
        Location loc = null;
        try {
            loc = _fakesTownLoc.get(_fakesTownTotal);
        } catch (Exception e) {
            //loc = _fakesTownLoc.get(Rnd.get(_locsCount));
        }
        if (loc == null) {
            loc = _fakesTownLoc.get(Rnd.get(_locsCount));
        }

        return loc;
    }

    private L2PcInstance restoreFake(int objId) {
        L2Fantome fake = _fakes.get(objId);
        if (fake == null) {
            return null;
        }

        L2PcInstance fantom = L2PcInstance.restoreFake(objId);
        fantom.setName(fake.name);
        fantom.setTitle(fake.title);

        fantom.getAppearance().setNameColor(getNameColor());
        fantom.getAppearance().setTitleColor(getTitleColor());

        //if (fake.clanId > 0)
        //	fantom.setClan(ClanTable.getInstance().getClan(fake.clanId));
        if (Rnd.get(100) < 40) {
            fantom.setClan(ClanTable.getInstance().getClan(getRandomClan()));
        }

        Location loc = getRandomLoc();
        fantom.setFakeLoc(loc.x, loc.y, loc.z);
        fantom.setXYZInvisible(loc.x + Rnd.get(60), loc.y + Rnd.get(60), loc.z);
        return fantom;
    }

    //
    private void parceColors() {
        _nameColors = Config.FAKE_PLAYERS_NAME_CLOLORS;
        _titleColors = Config.FAKE_PLAYERS_TITLE_CLOLORS;
        _nameColCount = _nameColors.size() - 1;
        _titleColCount = _titleColors.size() - 1;
    }

    private int getNameColor() {
        return _nameColors.get(Rnd.get(_nameColCount));
    }

    private int getTitleColor() {
        return _titleColors.get(Rnd.get(_titleColCount));
    }

    //
    public void wearArcher(L2PcInstance fantom) {
        //System.out.println("##wearFantom###1#");
        L2Set set = getRandomArcherSet();
        //System.out.println("##wearFantom###2#");
        L2ItemInstance body = ItemTable.getInstance().createDummyItem(set.body);
        L2ItemInstance gaiters = ItemTable.getInstance().createDummyItem(set.gaiters);
        L2ItemInstance gloves = ItemTable.getInstance().createDummyItem(set.gloves);
        L2ItemInstance boots = ItemTable.getInstance().createDummyItem(set.boots);
        L2ItemInstance weapon = ItemTable.getInstance().createDummyItem(set.weapon);
        ///System.out.println("##wearFantom###3#");
        fantom.getInventory().equipItemAndRecord(body);
        fantom.getInventory().equipItemAndRecord(gaiters);
        fantom.getInventory().equipItemAndRecord(gloves);
        fantom.getInventory().equipItemAndRecord(boots);

        if (set.custom > 0) {
            L2ItemInstance custom = ItemTable.getInstance().createDummyItem(set.custom);
            fantom.getInventory().equipItemAndRecord(custom);
        }

        //System.out.println("##wearFantom###4#");
        //weapon.setEnchantLevel((Rnd.get(6) + Rnd.get(6))); // 2 кубика надежнее
        weapon.setEnchantLevel(Rnd.get(Config.FAKE_PLAYERS_ENCHANT.nick, Config.FAKE_PLAYERS_ENCHANT.title)); // 2 кубика надежнее
        if (Rnd.get(100) < 30) {
            weapon.setAugmentation(new L2Augmentation(weapon, 1067847165, 3240, 10, false));
        }
        fantom.getInventory().equipItemAndRecord(weapon);
        //System.out.println("##wearFantom###5#");

        fantom.spawnMe();
        fantom.setOnlineStatus(true);
        if (Rnd.get(100) < 23) {
            fantom.setAlone(true);
        }
        //System.out.println("##wearFantom###6#");
        //System.out.println("##wearFantom###7#");
    }
}
