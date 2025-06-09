package net.sf.l2j.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.io.InputStream;
import java.net.Socket;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.EventDroplist;
import net.sf.l2j.gameserver.datatables.ExtractableItemsData;
import net.sf.l2j.gameserver.datatables.FakePlayersTablePlus;
import net.sf.l2j.gameserver.datatables.FishTable;
import net.sf.l2j.gameserver.datatables.HelperBuffTable;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.LevelUpData;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NobleSkillTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.datatables.Runes;
import net.sf.l2j.gameserver.datatables.SkillSpellbookTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.datatables.StaticObjects;
import net.sf.l2j.gameserver.datatables.SummonItemsData;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.datatables.WorldRegionTable;
import net.sf.l2j.gameserver.geoeditorcon.GeoEditorListener;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.*;
import net.sf.l2j.gameserver.model.AutoChatHandler;
import net.sf.l2j.gameserver.model.AutoSpawnHandler;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.entity.FightClub;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.TvTManager;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GamePacketHandler;
import net.sf.l2j.gameserver.pathfinding.PathFinding;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.util.AntiFarm;
import net.sf.l2j.gameserver.util.MemoryAgent;
import net.sf.l2j.gameserver.util.Online;
import net.sf.l2j.gameserver.util.vote.L2TopRU;
import net.sf.l2j.gameserver.util.vote.MmotopRU;
import net.sf.l2j.gameserver.util.PcCafe;
import net.sf.l2j.gameserver.util.QueuedItems;
import net.sf.l2j.gameserver.util.WebStat;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.status.Status;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.TimeLogger;

import net.sf.l2j.gameserver.util.protection.GameGuard;

import scripts.clanhalls.BanditStronghold;
import scripts.commands.AdminCommandHandler;
import scripts.commands.UserCommandHandler;
import scripts.commands.VoicedCommandHandler;
import scripts.communitybbs.Manager.AuctionBBSManager;
import scripts.communitybbs.Manager.AugmentBBSManager;
import scripts.communitybbs.Manager.CustomBBSManager;
import scripts.communitybbs.Manager.MailBBSManager;
import scripts.communitybbs.Manager.MenuBBSManager;
import scripts.items.ItemHandler;
import scripts.script.faenor.FaenorScriptEngine;
import scripts.scripting.CompiledScriptCache;
import scripts.scripting.L2ScriptEngineManager;
import scripts.skills.SkillHandler;

import net.sf.l2j.gameserver.util.protection.catsguard.CatsGuard;
import org.mmocore.network.nio.impl.SelectorConfig;
import org.mmocore.network.nio.impl.SelectorStats;
import org.mmocore.network.nio.impl.SelectorThread;

/**
 * This class ...
 *
 * @version $Revision: 1.29.2.15.2.19 $ $Date: 2005/04/05 19:41:23 $
 */
public class GameServer {

    private static Logger _log;// = AbstractLogger.getLogger(GameServer.class.getName());
    private final SkillTable _st;
    private final ItemTable _it;
    private final NpcTable _nt;
    private final HennaTable _ht;
    private final IdFactory _if;
    public static GameServer gameServer;
    private static ClanHallManager _cm;
    private final Shutdown _sh;
    private final DoorTable _dt;
    private final SevenSigns _ss;
    private final AutoChatHandler _ach;
    private final AutoSpawnHandler _ash;
    private LoginServerThread _loginThread;
    private final HelperBuffTable _hbt;
    private static Status _statusServer;
    private long _serverStartTimeMillis;
    @SuppressWarnings("unused")
    private final ThreadPoolManager _threadpools;
    public static final Calendar dateTimeServerStarted = Calendar.getInstance();

    public long getUsedMemoryMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576; // 1024 * 1024 = 1048576;
    }

    private final SelectorThread<L2GameClient> _selectorThreads[];
    private final SelectorStats _selectorStats = new SelectorStats();

    public SelectorThread<L2GameClient>[] getSelectorThreads() {
        return _selectorThreads;
    }

    public SelectorStats getSelectorStats() {
        return _selectorStats;
    }

    public ClanHallManager getCHManager() {
        return _cm;
    }

    public static GameServer getInstance() {
        return gameServer;
    }

    public long getServerStartTime() {
        return _serverStartTimeMillis;
    }

    public GameServer() throws Exception {
        gameServer = this;
        _serverStartTimeMillis = System.currentTimeMillis();
        //_log.finest("GameServer: used mem:" + getUsedMemoryMB()+"MB" );
        ThreadPoolManager.init();
        _threadpools = ThreadPoolManager.getInstance();

        AbstractLogger.startRefresTask();

        _if = IdFactory.getInstance();
        if (!_if.isInitialized()) {
            _log.severe("GameServer [ERROR]: Could not read object IDs from DB. Please Check Your Data.");
            throw new Exception("GameServer [ERROR]: Could not initialize the ID factory");
        }
        Formulas.init();
        ItemTable.init();
        //CrestCache.init();

        if (Config.DEADLOCKCHECK_INTERVAL > 0) {
            DeadlockDetector.init();
        }
        new File(Config.DATAPACK_ROOT, "data/clans").mkdirs();

        new File(Config.DATAPACK_ROOT, "data/crests").mkdirs();
        //new File("pathnode").mkdirs();

        // load script engines
        L2ScriptEngineManager.getInstance();

        // start game time control early
        GameTimeController.init();

        // keep the references of Singletons to prevent garbage collection
        CharNameTable.getInstance();

        ClanTable.init();
        _it = ItemTable.getInstance();
        if (!_it.isInitialized()) {
            _log.severe("GameServer [ERROR]: Could not find the extraced files. Please Check Your Data.");
            throw new Exception("GameServer [ERROR]: Could not initialize the item table");
        }

        ExtractableItemsData.getInstance();
        CustomServerData.init();
        SummonItemsData.getInstance();
        WorldRegionTable.init();
        // Load clan hall data before zone data
        GrandBossManager.init();
        ClanHallManager.init();
        _cm = ClanHallManager.getInstance();
        CastleManager.init();
        ZoneManager.init();
        SiegeManager.init();

        TradeController.init();
        _st = SkillTable.getInstance();
        if (!_st.isInitialized()) {
            _log.severe("GameServer [ERROR]: Could not find the extraced files. Please Check Your Data.");
            throw new Exception("GameServer [ERROR]: Could not initialize the skill table");
        }

        //if(Config.ALLOW_NPC_WALKERS)
        NpcWalkerRoutesTable.getInstance().load();
        //L2EMU_ADD by Rayan. L2J - BigBro
        GrandBossManager.getInstance().loadManagers();

        RecipeController.init();
        NobleSkillTable.init();
        HeroSkillTable.init();

        SkillTreeTable.getInstance();
        ArmorSetsTable.getInstance();
        FishTable.getInstance();
        SkillSpellbookTable.getInstance();
        CharTemplateTable.getInstance();

        //Call to load caches
        HtmCache.getInstance();
        CrestCache.init();
        Static.init();
        _nt = NpcTable.getInstance();

        if (!_nt.isInitialized()) {
            _log.severe("GameServer [ERROR]: Could not find the extraced files. Please Check Your Data.");
            throw new Exception("GameServer [ERROR]: Could not initialize the npc table");
        }

        _ht = HennaTable.getInstance();

        if (!_ht.isInitialized()) {
            throw new Exception("GameServer [ERROR]: Could not initialize the Henna Table");
        }

        HennaTreeTable.getInstance();

        if (!_ht.isInitialized()) {
            throw new Exception("GameServer [ERROR]: Could not initialize the Henna Tree Table");
        }

        _hbt = HelperBuffTable.getInstance();

        if (!_hbt.isInitialized()) {
            throw new Exception("GameServer [ERROR]: Could not initialize the Helper Buff Table");
        }

        GeoData.getInstance();
        if (Config.GEODATA == 2) {
            PathFinding.getInstance();
        }

        AttackStanceTaskManager.init();
        DecayTaskManager.init();

        TeleportLocationTable.init();
        LevelUpData.getInstance();
        L2World.getInstance();
        //ZoneData.getInstance();
        SpawnTable.getInstance();
        RaidBossSpawnManager.init();
        //FrintezzaManager.getInstance().onLoad();
        RaidBossPointsManager.init();
        //DayNightSpawnManager.getInstance().notifyChangeMode();
        DimensionalRiftManager.getInstance();
        Announcements.init();
        MapRegionTable.getInstance();
        EventDroplist.getInstance();

        /**
         * Load Manor data
         */
        L2Manor.init();
        L2Multisell.getInstance();

        /**
         * Load Manager
         */
        AuctionManager.getInstance();
        BoatManager.init();
        CastleManorManager.pinit();
        MercTicketManager.init();
        //PartyCommandManager.getInstance();
        PetitionManager.getInstance();
        QuestManager.init();
        EventManager.init();
        BanditStronghold.init();

        AugmentationData.getInstance();
        if (Config.SAVE_DROPPED_ITEM) {
            ItemsOnGroundManager.init();
        }

        if (Config.AUTODESTROY_ITEM_AFTER > 0 || Config.HERB_AUTO_DESTROY_TIME > 0) {
            ItemsAutoDestroy.init();
        }

        MonsterRace.getInstance();
        // Handlers
        ItemHandler.getInstance();
        SkillHandler.getInstance();
        UserCommandHandler.getInstance();
        VoicedCommandHandler.getInstance();

        _dt = DoorTable.getInstance();
        _dt.parseData();
        StaticObjects.getInstance();

        try {
            _log.info("GameServer: Loading Server Scripts");
            File scripts = new File(Config.DATAPACK_ROOT + "/data/scripts.cfg");
            L2ScriptEngineManager.getInstance().executeScriptList(scripts);
        } catch (IOException ioe) {
            _log.severe("GameServer [ERROR]: Failed loading scripts.cfg, no script going to be loaded");
        }
        try {
            CompiledScriptCache compiledScriptCache = L2ScriptEngineManager.getInstance().getCompiledScriptCache();
            if (compiledScriptCache == null) {
                _log.info("GameServer: Compiled Scripts Cache is disabled.");
            } else {
                compiledScriptCache.purge();

                if (compiledScriptCache.isModified()) {
                    compiledScriptCache.save();
                    _log.info("GameServer: Compiled Scripts Cache was saved.");
                } else {
                    _log.info("GameServer: Compiled Scripts Cache is up-to-date.");
                }
            }

        } catch (IOException e) {
            _log.log(Level.SEVERE, "GameServer [ERROR]: Failed to store Compiled Scripts Cache.", e);
        }

        SevenSigns.init();
        _ss = SevenSigns.getInstance();
        SevenSignsFestival.init();
        FourSepulchersManager.init();
        AutoChatHandler.init();
        AutoSpawnHandler.init();
        _ash = AutoSpawnHandler.getInstance();
        _ach = AutoChatHandler.getInstance();

        // Spawn the Orators/Preachers if in the Seal Validation period.
        _ss.spawnSevenSignsNPC();
        AdminCommandHandler.getInstance();

        //CustomServerData.init();
        Olympiad.load();
        //OlympiadDiary.load();
        Hero.getInstance();
        //Anarchy.getInstance().init();
        //
        FaenorScriptEngine.getInstance();
        PartyWaitingRoomManager.init();
        // Init of a cursed weapon manager
        CursedWeaponsManager.init();
        CrownManager.init();
        TownManager.init();

        _log.config("AutoChatHandler: Loaded " + _ach.size() + " handlers in total.");
        _log.config("AutoSpawnHandler: Loaded " + _ash.size() + " handlers in total.");

        if (Config.L2JMOD_ALLOW_WEDDING) {
            CoupleManager.init();
        }

        TaskManager.getInstance();

        GmListTable.getInstance();

        // read pet stats from db
        L2PetDataTable.getInstance().loadPetsData();

        // Universe.getInstance();
        GeoEditorListener.init();

        _sh = Shutdown.getInstance();
        Runtime.getRuntime().addShutdownHook(_sh);

        try {
            _dt.getDoor(24190001).openMe();
            _dt.getDoor(24190002).openMe();
            _dt.getDoor(24190003).openMe();
            _dt.getDoor(24190004).openMe();
            _dt.getDoor(23180001).openMe();
            _dt.getDoor(23180002).openMe();
            _dt.getDoor(23180003).openMe();
            _dt.getDoor(23180004).openMe();
            _dt.getDoor(23180005).openMe();
            _dt.getDoor(23180006).openMe();
            _dt.getDoor(19160001).openMe();
            _dt.getDoor(19160010).openMe();
            _dt.getDoor(19160011).openMe();
            _dt.getDoor(23150003).openMe();
            _dt.getDoor(23150004).openMe();
            // _dt.getDoor(25150043).openMe();
            // _dt.getDoor(25150045).openMe();
            // _dt.getDoor().openMe();
            // _dt.getDoor(19160011).openMe();

            _dt.checkAutoOpen();
        } catch (NullPointerException e) {
            e.printStackTrace();
            _log.warning("GameServer [ERROR]: There is errors in your Door.csv file. Update door.csv");
            // if (Config.DEBUG)
            //e.printStackTrace();
        }

        if (Config.COMMUNITY_TYPE.equals("pw")) {
            AugmentBBSManager.init();
            AuctionBBSManager.init();
            CustomBBSManager.init();
            MailBBSManager.init();
            MenuBBSManager.init();
        }
        //ForumsBBSManager.getInstance();
        
        Runes.load();
        _log.config("IdFactory: Free ObjectID's remaining: " + IdFactory.getInstance().size());

        TvTManager.getInstance();
        FightClub.init();

        Runtime r = Runtime.getRuntime();
        /*
         * //for (int i = 0, n = 4; i < n; i++) //{ r.runFinalization();
         * r.gc(); //System.gc(); //}
         */
        // maxMemory is the upper limit the jvm can use, totalMemory the size of the current allocation pool, freeMemory the unused memory in the allocation pool
        long freeMem = (r.maxMemory() - r.totalMemory() + r.freeMemory()) / 1048576; // 1024 * 1024 = 1048576;
        long totalMem = r.maxMemory() / 1048576;
        _log.info("GameServer: Started, free memory " + freeMem + " Mb of " + totalMem + " Mb");

        LoginServerThread.init();
        _loginThread = LoginServerThread.getInstance();
        _loginThread.start();

        /////////////////
        L2GamePacketHandler gph = new L2GamePacketHandler();
        InetAddress serverAddr = Config.GAMESERVER_HOSTNAME.equalsIgnoreCase("*") ? null : InetAddress.getByName(Config.GAMESERVER_HOSTNAME);

        final SelectorConfig sc = new SelectorConfig();
        _selectorThreads = new SelectorThread[Config.PORTS_GAME.length];
        for (int i = 0; i < Config.PORTS_GAME.length; i++) {
            _selectorThreads[i] = new SelectorThread<>(sc, _selectorStats, gph, gph, gph, null);
            _selectorThreads[i].openServerSocket(serverAddr, Config.PORTS_GAME[i]);
            _selectorThreads[i].start();
        }

        ////////////////
        if (Config.IS_TELNET_ENABLED) {
            _statusServer = new Status(Server.serverMode);
            _statusServer.start();
        } else {
            _log.info("GameServer: Telnet server is currently disabled.");
        }

        _log.config("GameServer: Maximum Numbers of Connected Players: " + Config.MAXIMUM_ONLINE_USERS);

        Online.getInstance().loadMaxOnline();
        MemoryAgent.init();
        //FakePlayersTable.init();
        FakePlayersTablePlus.init();

        if (Config.CACHED_SERVER_STAT) {
            CustomServerData.getInstance().cacheStat();
        }

        if (Config.RESTART_HOUR > 0) {
            AutoRestart.init();
        } else {
            _log.info("Auto Restart: disabled.");
        }

        Util.setVoteRefMethod();
        if (Config.L2TOP_ENABLE) {
            L2TopRU.init();
        }

        if (Config.MMOTOP_ENABLE) {
            MmotopRU.init();
        }

        if (Config.PC_CAFE_ENABLED) {
            PcCafe.init();
        }

        if (Config.QUED_ITEMS_ENABLE) {
            QueuedItems.init();
        }

        if (Config.WEBSTAT_ENABLE) {
            WebStat.init();
        }

        if (Config.ALT_RESTORE_OFFLINE_TRADE) {
            CustomServerData.getInstance().restoreOfflineTraders();
        }

        GameGuard.getInstance().startCheckTask();
        if (Config.CATS_GUARD) {
            CatsGuard.init();
        }

        AbstractLogger.setLoaded();
    }

    public static void main(String[] args) throws Exception {

        Server.serverMode = Server.MODE_GAMESERVER;
        // Initialize config
        try {
            Config.load(false);
        } catch (Exception e) {
        }

        if (isPortBusy()) {
            System.out.println("[ERROR] Server is already running.");
            System.exit(0);
        }

        if (isMysqlDown()) {
            System.out.println("[ERROR] MySQL Server is not running.");
            System.exit(0);
        }

        AbstractLogger.init();
        _log = AbstractLogger.getLogger(GameServer.class.getName());
        _log.info(TimeLogger.getLogTime() + "Welcome to pwServer.");

        /*long time = System.currentTimeMillis();
         System.out.println("####" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(time)));
         System.out.println("####" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(time)));
         System.out.println("####" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(time)));
         System.out.println("####" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(time)));
         System.out.println("####" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(time)));
         System.out.println("####" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(time)));*/
        GameGuard.init();

        // Create log folder
        new File(Config.DATAPACK_ROOT, "log").mkdir();
        new File(Config.DATAPACK_ROOT, "log/cheats").mkdir();
        new File(Config.DATAPACK_ROOT, "log/donate").mkdir();
        new File(Config.DATAPACK_ROOT, "log/item").mkdirs();
        new File(Config.DATAPACK_ROOT, "log/item/auction").mkdirs();
        new File(Config.DATAPACK_ROOT, "log/item/post").mkdirs();

        // Create input stream for log file -- or store file data into memory
        InputStream is = new FileInputStream(new File("./config/log.cfg"));
        LogManager.getLogManager().readConfiguration(is);
        is.close();

        Log.init();
        AntiFarm.init();

        L2DatabaseFactory.init();
        gameServer = new GameServer();
    }

    private static boolean isPortBusy() {
        Socket sock = null;
        try {
            sock = new Socket(Config.GAMESERVER_HOSTNAME, Config.PORTS_GAME[0]);
            if (sock.isConnected()) {
                return true;
            }
        } catch (Exception e) {
        } finally {
            try {
                if (sock != null) {
                    sock.close();
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    //
    private static boolean isMysqlDown() {
        //jdbc:mysql://localhost/pw?useUnicode=true&characterEncoding=UTF-8
        String[] a = Config.DATABASE_URL.split("/");
        String host = a[2];
        String port = "3306";

        a = host.split(":");
        host = a[0];
        try {
            port = a[1];
        } catch (Exception e) {
        }

        Socket sock = null;
        try {
            sock = new Socket(host, Integer.valueOf(port));
            if (sock.isConnected()) {
                return false;
            }
        } catch (Exception e) {
        } finally {
            try {
                if (sock != null) {
                    sock.close();
                }
            } catch (Exception e) {
            }
        }
        return true;
    }
}
