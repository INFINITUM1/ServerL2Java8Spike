package net.sf.l2j.gameserver.model.entity.olympiad;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.ServerVariables;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.MultiValueIntegerMap;
import net.sf.l2j.util.log.AbstractLogger;

public class Olympiad {

    public static final Logger _log = AbstractLogger.getLogger(Olympiad.class.getName());
    public static FastMap<Integer, StatsSet> _nobles;
    public static FastMap<Integer, Integer> _noblesRank;
    public static FastList<StatsSet> _heroesToBe;
    public static FastList<Integer> _nonClassBasedRegisters = new FastList<Integer>();
    public static MultiValueIntegerMap _classBasedRegisters = new MultiValueIntegerMap();
    public static ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<String>();
    public static ConcurrentLinkedQueue<String> _hwids = new ConcurrentLinkedQueue<String>();
    public static final int DEFAULT_POINTS = 18;
    private static final int WEEKLY_POINTS = 3;
    private static final String OLYMPIAD_DATA_FILE = "config/olympiad.cfg";
    public static final String OLYMPIAD_HTML_PATH = "data/html/olympiad/";
    public static final String OLYMPIAD_LOAD_NOBLES = "SELECT * FROM `olympiad_nobles`";
    public static final String OLYMPIAD_SAVE_NOBLES = "REPLACE INTO `olympiad_nobles` (`char_id`, `class_id`, `char_name`, `olympiad_points`, `olympiad_points_past`, `olympiad_points_past_static`, `competitions_done`, `competitions_win`, `competitions_loose`) VALUES (?,?,?,?,?,?,?,?,?)";
    public static final String OLYMPIAD_GET_HEROS = "SELECT `char_id`, `char_name` FROM `olympiad_nobles` WHERE `class_id` = ? AND `competitions_done` >= 5 AND `competitions_win` > 0 ORDER BY `olympiad_points` DESC, `competitions_win` DESC, `competitions_done` DESC";
    public static final String GET_ALL_CLASSIFIED_NOBLESS = "SELECT `char_id` FROM `olympiad_nobles` ORDER BY olympiad_points_past_static DESC";
    public static final String GET_EACH_CLASS_LEADER = "SELECT `char_name` FROM `olympiad_nobles` WHERE `class_id` = ? AND `olympiad_points` != 0 ORDER BY `olympiad_points` DESC LIMIT 10";
    public static final String OLYMPIAD_CALCULATE_LAST_PERIOD = "UPDATE `olympiad_nobles` SET `olympiad_points_past` = `olympiad_points`, `olympiad_points_past_static` = `olympiad_points` WHERE `competitions_done` >= 5";
    public static final String OLYMPIAD_CLEANUP_NOBLES = "UPDATE `olympiad_nobles` SET `olympiad_points` = " + DEFAULT_POINTS + ", `competitions_done` = 0, `competitions_win` = 0, `competitions_loose` = 0";
    public static final String CHAR_ID = "char_id";
    public static final String CLASS_ID = "class_id";
    public static final String CHAR_NAME = "char_name";
    public static final String POINTS = "olympiad_points";
    public static final String POINTS_PAST = "olympiad_points_past";
    public static final String POINTS_PAST_STATIC = "olympiad_points_past_static";
    public static final String COMP_DONE = "competitions_done";
    public static final String COMP_WIN = "competitions_win";
    public static final String COMP_LOOSE = "competitions_loose";
    public static long _olympiadEnd;
    public static long _validationEnd;
    public static int _period;
    public static long _nextWeeklyChange;
    public static int _currentCycle;
    private static long _compEnd;
    private static Calendar _compStart;
    public static boolean _inCompPeriod;
    public static boolean _isOlympiadEnd;
    private static ScheduledFuture<?> _scheduledOlympiadEnd;
    public static ScheduledFuture<?> _scheduledManagerTask;
    public static ScheduledFuture<?> _scheduledWeeklyTask;
    public static ScheduledFuture<?> _scheduledValdationTask;
    public static final Stadia[] STADIUMS = new Stadia[22];
    public static OlympiadManager _manager;
    private static FastList<L2OlympiadManagerInstance> _npcs = new FastList<L2OlympiadManagerInstance>();

    public static void load() {
        _nobles = new FastMap<Integer, StatsSet>().shared("Olympiad._nobles");
        _currentCycle = ServerVariables.getInt("Olympiad_CurrentCycle", -1);
        _period = ServerVariables.getInt("Olympiad_Period", -1);
        _olympiadEnd = ServerVariables.getLong("Olympiad_End", -1);
        _validationEnd = ServerVariables.getLong("Olympiad_ValdationEnd", -1);
        _nextWeeklyChange = ServerVariables.getLong("Olympiad_NextWeeklyChange", -1);

        Properties OlympiadProperties = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(new File("./" + OLYMPIAD_DATA_FILE));
            OlympiadProperties.load(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ignored) {
                //
            }
        }

        if (_currentCycle == -1) {
            _currentCycle = Integer.parseInt(OlympiadProperties.getProperty("CurrentCycle", "1"));
        }
        if (_period == -1) {
            _period = Integer.parseInt(OlympiadProperties.getProperty("Period", "0"));
        }
        if (_olympiadEnd == -1) {
            _olympiadEnd = Long.parseLong(OlympiadProperties.getProperty("OlympiadEnd", "0"));
        }
        if (_validationEnd == -1) {
            _validationEnd = Long.parseLong(OlympiadProperties.getProperty("ValdationEnd", "0"));
        }
        if (_nextWeeklyChange == -1) {
            _nextWeeklyChange = Long.parseLong(OlympiadProperties.getProperty("NextWeeklyChange", "0"));
        }

        initStadiums();

        OlympiadDatabase.loadNobles();
        OlympiadDatabase.loadNoblesRank();

        switch (_period) {
            case 0:
                if (_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis()) {
                    OlympiadDatabase.setNewOlympiadEnd();
                } else {
                    _isOlympiadEnd = false;
                }
                break;
            case 1:
                _isOlympiadEnd = true;
                _scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationTask(), getMillisToValidationEnd());
                break;
            default:
                _log.warning("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
                return;
        }

        _log.info("Olympiad System: Loading Olympiad System....");
        if (_period == 0) {
            _log.info("Olympiad System: Currently in Olympiad Period");
        } else {
            _log.info("Olympiad System: Currently in Validation Period");
        }

        _log.info("Olympiad System: Period Ends....");

        long milliToEnd;
        if (_period == 0) {
            milliToEnd = getMillisToOlympiadEnd();
        } else {
            milliToEnd = getMillisToValidationEnd();
        }

        double numSecs = milliToEnd / 1000 % 60;
        double countDown = (milliToEnd / 1000 - numSecs) / 60;
        int numMins = (int) Math.floor(countDown % 60);
        countDown = (countDown - numMins) / 60;
        int numHours = (int) Math.floor(countDown % 24);
        int numDays = (int) Math.floor((countDown - numHours) / 24);

        _log.info("Olympiad System: In " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");

        if (_period == 0) {
            _log.info("Olympiad System: Next Weekly Change is in....");

            milliToEnd = getMillisToWeekChange();

            double numSecs2 = milliToEnd / 1000 % 60;
            double countDown2 = (milliToEnd / 1000 - numSecs2) / 60;
            int numMins2 = (int) Math.floor(countDown2 % 60);
            countDown2 = (countDown2 - numMins2) / 60;
            int numHours2 = (int) Math.floor(countDown2 % 24);
            int numDays2 = (int) Math.floor((countDown2 - numHours2) / 24);

            _log.info("Olympiad System: In " + numDays2 + " days, " + numHours2 + " hours and " + numMins2 + " mins.");
        }

        _log.info("Olympiad System: Loaded " + _nobles.size() + " Noblesses");

        if (_period == 0) {
            init();
        }
    }

    private static void initStadiums() {
        // Начальные данные стадионов
        Stadia st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-20814, -21189, -3030));
        st.setTele2(new Location(-20814, -21189, -3030));
        STADIUMS[0] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-120324, -225077, -3331));
        st.setTele2(new Location(-120324, -225077, -3331));
        STADIUMS[1] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-102495, -209023, -3331));
        st.setTele2(new Location(-102495, -209023, -3331));
        STADIUMS[2] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-120156, -207378, -3331));
        st.setTele2(new Location(-120156, -207378, -3331));
        STADIUMS[3] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-87628, -225021, -3331));
        st.setTele2(new Location(-87628, -225021, -3331));
        STADIUMS[4] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-81705, -213209, -3331));
        st.setTele2(new Location(-81705, -213209, -3331));
        STADIUMS[5] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-87593, -207339, -3331));
        st.setTele2(new Location(-87593, -207339, -3331));
        STADIUMS[6] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-93709, -218304, -3331));
        st.setTele2(new Location(-93709, -218304, -3331));
        STADIUMS[7] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-77157, -218608, -3331));
        st.setTele2(new Location(-77157, -218608, -3331));
        STADIUMS[8] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-69682, -209027, -3331));
        st.setTele2(new Location(-69682, -209027, -3331));
        STADIUMS[9] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-76887, -201256, -3331));
        st.setTele2(new Location(-76887, -201256, -3331));
        STADIUMS[10] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-109985, -218701, -3331));
        st.setTele2(new Location(-109985, -218701, -3331));
        STADIUMS[11] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-126367, -218228, -3331));
        st.setTele2(new Location(-126367, -218228, -3331));
        STADIUMS[12] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-109629, -201292, -3331));
        st.setTele2(new Location(-109629, -201292, -3331));
        STADIUMS[13] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-87523, -240169, -3331));
        st.setTele2(new Location(-87523, -240169, -3331));
        STADIUMS[14] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-81748, -245950, -3331));
        st.setTele2(new Location(-81748, -245950, -3331));
        STADIUMS[15] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-77123, -251473, -3331));
        st.setTele2(new Location(-77123, -251473, -3331));
        STADIUMS[16] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-69778, -241801, -3331));
        st.setTele2(new Location(-69778, -241801, -3331));
        STADIUMS[17] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-76754, -234014, -3331));
        st.setTele2(new Location(-76754, -234014, -3331));
        STADIUMS[18] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-93742, -251032, -3331));
        st.setTele2(new Location(-93742, -251032, -3331));
        STADIUMS[19] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-87466, -257752, -3331));
        st.setTele2(new Location(-87466, -257752, -3331));
        STADIUMS[20] = st;

        st = new Stadia();
        st.setStadiaFree();
        st.setTele1(new Location(-114413, -213241, -3331));
        st.setTele2(new Location(-114413, -213241, -3331));
        STADIUMS[21] = st;
    }

    public static void init() {
        if (_period == 1) {
            return;
        }

        _compStart = Calendar.getInstance();
        _compStart.set(Calendar.HOUR_OF_DAY, Config.ALT_OLY_START_TIME);
        _compStart.set(Calendar.MINUTE, Config.ALT_OLY_MIN);
        _compEnd = _compStart.getTimeInMillis() + Config.ALT_OLY_CPERIOD;

        if (_scheduledOlympiadEnd != null) {
            _scheduledOlympiadEnd.cancel(true);
        }
        _scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new OlympiadEndTask(), getMillisToOlympiadEnd());

        updateCompStatus();

        if (_scheduledWeeklyTask != null) {
            _scheduledWeeklyTask.cancel(true);
        }
        _scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WeeklyTask(), getMillisToWeekChange(), Config.ALT_OLY_WPERIOD);
    }

    public static synchronized boolean registerNoble(L2PcInstance noble, CompType type) {
        if (!_inCompPeriod || _isOlympiadEnd) {
            noble.sendPacket(Static.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
            return false;
        }

        if (getMillisToOlympiadEnd() <= 600 * 1000) {
            noble.sendPacket(Static.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
            return false;
        }

        if (getMillisToCompEnd() <= 600 * 1000) {
            noble.sendPacket(Static.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
            return false;
        }

        if (!Config.ALT_OLY_SAME_HWID && _hwids.contains(noble.getHWID())) {
            noble.sendHtmlMessage("Олимпиада", "С вашего компьютера уже кто-то учавствует.");
            return false;
        }

        if (!Config.ALT_OLY_SAME_IP && _ips.contains(noble.getIP())) {
            noble.sendHtmlMessage("Олимпиада", "С вашего ip-адресса уже кто-то учавствует.");
            return false;
        }

        if (noble.isInEvent()) {
            noble.sendHtmlMessage("Олимпиада", "Вы уже зарегистрированы в другом ивенте.");
            return false;
        }

        if (noble.getKarma() > 0 || noble.isCursedWeaponEquiped()) {
            noble.sendPacket(Static.OLYMPIAD_GAME_KARMA);
            return false;
        }

        if (Config.OLY_ALT_REWARD && !noble.destroyItemByItemId("OlyReward", Config.OLY_ALT_REWARD_ITEM.id, Config.OLY_ALT_REWARD_ITEM.count, noble, true)) {
            noble.sendHtmlMessage("Олимпиада", "Стоимость участия: " + Config.OLY_ALT_REWARD_ITEM.count + " " + Config.OLY_ALT_REWARD_ITEM_NAME + ".");
            return false;
        }

        addNoble(noble);

        StatsSet nobleInfo = _nobles.get(noble.getObjectId());

        if (nobleInfo == null || !noble.isNoble()) {
            noble.sendPacket(Static.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
            return false;
        }

        if (noble.getBaseClass() != noble.getClassId().getId()) {
            noble.sendPacket(Static.YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER);
            return false;
        }

        if (getNoblePoints(noble.getObjectId()) < 3) {
            noble.sendPacket(Static.OLYMPIAD_GAME_LESS_POINTS);
            return false;
        }

        if (noble.getOlympiadGameId() > -1) {
            noble.sendPacket(Static.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME);
            return false;
        }

        int classId = nobleInfo.getInteger(CLASS_ID);

        switch (type) {
            case CLASSED: {
                if (_classBasedRegisters.containsValue(noble.getObjectId())) {
                    noble.sendPacket(Static.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_TO_PARTICIPATE_IN_THE_GAME_FOR_YOUR_CLASS);
                    return false;
                }

                _classBasedRegisters.put(classId, noble.getObjectId());
                noble.sendPacket(Static.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
                break;
            }
            case NON_CLASSED: {
                if (_nonClassBasedRegisters.contains(noble.getObjectId())) {
                    noble.sendPacket(Static.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME);
                    return false;
                }

                _nonClassBasedRegisters.add(noble.getObjectId());
                noble.sendPacket(Static.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
                break;
            }
        }

        _ips.add(noble.getIP());
        _hwids.add(noble.getHWID());
        return true;
    }

    public static synchronized void logoutPlayer(L2PcInstance player) {
        _classBasedRegisters.removeValue(player.getObjectId());
        _nonClassBasedRegisters.remove(Integer.valueOf(player.getObjectId()));
        removeNobleIp(player, true);

        OlympiadGame game = getOlympiadGame(player.getOlympiadGameId());
        if (game != null) {
            try {
                if (!game.logoutPlayer(player) && !game.validated) {
                    game.endGame(20000, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized boolean unRegisterNoble(L2PcInstance noble) {
        if (!_inCompPeriod || _isOlympiadEnd) {
            noble.sendPacket(Static.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
            return false;
        }

        if (!noble.isNoble()) {
            noble.sendPacket(Static.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
            return false;
        }

        OlympiadGame game = getOlympiadGame(noble.getOlympiadGameId());
        if (game != null) {
            try {
                if (!game.logoutPlayer(noble) && !game.validated) {
                    game.endGame(20000, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!isRegistered(noble)) {
            noble.sendPacket(Static.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
            return false;
        }

        _classBasedRegisters.removeValue(noble.getObjectId());
        _nonClassBasedRegisters.remove(Integer.valueOf(noble.getObjectId()));
        removeNobleIp(noble, true);

        noble.sendPacket(Static.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);

        return true;
    }

    private static synchronized void updateCompStatus() {
        long milliToStart = getMillisToCompBegin();
        double numSecs = milliToStart / 1000 % 60;
        double countDown = (milliToStart / 1000 - numSecs) / 60;
        int numMins = (int) Math.floor(countDown % 60);
        countDown = (countDown - numMins) / 60;
        int numHours = (int) Math.floor(countDown % 24);
        int numDays = (int) Math.floor((countDown - numHours) / 24);

        _log.info("Olympiad System: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
        _log.info("Olympiad System: Event starts/started: " + _compStart.getTime());

        ThreadPoolManager.getInstance().scheduleGeneral(new CompStartTask(), getMillisToCompBegin());
    }

    private static long getMillisToOlympiadEnd() {
        return _olympiadEnd - System.currentTimeMillis();
    }

    static long getMillisToValidationEnd() {
        if (_validationEnd > System.currentTimeMillis()) {
            return _validationEnd - System.currentTimeMillis();
        }
        return 10L;
    }

    public static boolean isOlympiadEnd() {
        return _isOlympiadEnd;
    }

    public static boolean inCompPeriod() {
        return _inCompPeriod;
    }

    private static long getMillisToCompBegin() {
        if (_compStart.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && _compEnd > Calendar.getInstance().getTimeInMillis()) {
            return 10L;
        }
        if (_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
            return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        }
        return setNewCompBegin();
    }

    private static long setNewCompBegin() {
        _compStart = Calendar.getInstance();
        _compStart.set(Calendar.HOUR_OF_DAY, Config.ALT_OLY_START_TIME);
        _compStart.set(Calendar.MINUTE, Config.ALT_OLY_MIN);
        _compStart.add(Calendar.HOUR_OF_DAY, 24);
        _compEnd = _compStart.getTimeInMillis() + Config.ALT_OLY_CPERIOD;

        _log.info("Olympiad System: New Schedule @ " + _compStart.getTime());

        return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
    }

    public static long getMillisToCompEnd() {
        return _compEnd - Calendar.getInstance().getTimeInMillis();
    }

    private static long getMillisToWeekChange() {
        if (_nextWeeklyChange > Calendar.getInstance().getTimeInMillis()) {
            return _nextWeeklyChange - Calendar.getInstance().getTimeInMillis();
        }
        return 10L;
    }

    public static synchronized void addWeeklyPoints() {
        if (_period == 1) {
            return;
        }
        for (Integer nobleId : _nobles.keySet()) {
            StatsSet nobleInfo = _nobles.get(nobleId);
            if (nobleInfo != null) {
                nobleInfo.set(POINTS, nobleInfo.getInteger(POINTS) + WEEKLY_POINTS);
            }
        }
    }

    public static int getCurrentCycle() {
        return _currentCycle;
    }

    public static synchronized void addSpectator(int id, L2PcInstance spectator) {
        if (spectator.getOlympiadGameId() != -1 || Olympiad.isRegisteredInComp(spectator)) {
            spectator.sendPacket(Static.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
            return;
        }

        if (_manager == null || _manager.getOlympiadInstance(id) == null || _manager.getOlympiadInstance(id).getStatus() == BattleStatus.Begining || _manager.getOlympiadInstance(id).getStatus() == BattleStatus.Begin_Countdown) {
            spectator.sendPacket(Static.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
            return;
        }

        Location tele = STADIUMS[id].getTele1();
        spectator.enterOlympiadObserverMode(tele.x, tele.y, tele.z, id, true);

        _manager.getOlympiadInstance(id).addSpectator(spectator);
    }

    public static synchronized void removeSpectator(int id, L2PcInstance spectator) {
        if (_manager == null || _manager.getOlympiadInstance(id) == null) {
            return;
        }

        _manager.getOlympiadInstance(id).removeSpectator(spectator);
    }

    public static FastList<L2PcInstance> getSpectators(int id) {
        if (_manager == null || _manager.getOlympiadInstance(id) == null) {
            return null;
        }
        return _manager.getOlympiadInstance(id).getSpectators();
    }

    public static OlympiadGame getOlympiadGame(int gameId) {
        if (_manager == null || gameId < 0) {
            return null;
        }
        return _manager.getOlympiadGames().get(gameId);
    }

    public static synchronized int[] getWaitingList() {
        if (!inCompPeriod()) {
            return null;
        }

        int[] array = new int[4];
        array[0] = _classBasedRegisters.totalSize();
        array[1] = _nonClassBasedRegisters.size();

        return array;
    }

    public static synchronized int getNoblessePasses(L2PcInstance player) {
        int objId = player.getObjectId();

        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return 0;
        }

        int points = noble.getInteger(POINTS_PAST);
        if (points == 0) // Уже получил бонус
        {
            return 0;
        }

        int rank = _noblesRank.get(objId);
        switch (rank) {
            case 1:
                points = Config.ALT_OLY_RANK1_POINTS;
                break;
            case 2:
                points = Config.ALT_OLY_RANK2_POINTS;
                break;
            case 3:
                points = Config.ALT_OLY_RANK3_POINTS;
                break;
            case 4:
                points = Config.ALT_OLY_RANK4_POINTS;
                break;
            default:
                points = Config.ALT_OLY_RANK5_POINTS;
        }

        if (player.isHero() || Hero.getInstance().isInactiveHero(player.getObjectId())) {
            points += Config.ALT_OLY_HERO_POINTS;
        }

        noble.set(POINTS_PAST, 0);
        OlympiadDatabase.saveNobleData(objId);

        return points * Config.ALT_OLY_GP_PER_POINT;
    }

    public static synchronized boolean isRegistered(L2PcInstance noble) {
        if (_classBasedRegisters.containsValue(noble.getObjectId())) {
            return true;
        }
        if (_nonClassBasedRegisters.contains(noble.getObjectId())) {
            return true;
        }
        return false;
    }

    public static synchronized boolean isRegisteredInComp(L2PcInstance player) {
        if (isRegistered(player)) {
            return true;
        }
        if (_manager == null || _manager.getOlympiadGames() == null) {
            return false;
        }
        for (OlympiadGame g : _manager.getOlympiadGames().values()) {
            if (g != null && g.isRegistered(player.getObjectId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает олимпийские очки за текущий период
     *
     * @param objId
     * @return
     */
    public static synchronized int getNoblePoints(int objId) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return 0;
        }
        return noble.getInteger(POINTS);
    }

    /**
     * Возвращает олимпийские очки за прошлый период
     *
     * @param objId
     * @return
     */
    public static synchronized int getNoblePointsPast(int objId) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return 0;
        }
        return noble.getInteger(POINTS_PAST);
    }

    public static synchronized int getCompetitionDone(int objId) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return 0;
        }
        return noble.getInteger(COMP_DONE);
    }

    public static synchronized int getCompetitionWin(int objId) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return 0;
        }
        return noble.getInteger(COMP_WIN);
    }

    public static synchronized int getCompetitionLoose(int objId) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return 0;
        }
        return noble.getInteger(COMP_LOOSE);
    }

    public static Stadia[] getStadiums() {
        return STADIUMS;
    }

    public static FastList<L2OlympiadManagerInstance> getNpcs() {
        return _npcs;
    }

    public static void addOlympiadNpc(L2OlympiadManagerInstance npc) {
        _npcs.add(npc);
    }

    public static void changeNobleName(int objId, String newName) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return;
        }
        noble.set(CHAR_NAME, newName);
        OlympiadDatabase.saveNobleData(objId);
    }

    public static String getNobleName(int objId) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return null;
        }
        return noble.getString(CHAR_NAME);
    }

    public static int getNobleClass(int objId) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return 0;
        }
        return noble.getInteger(CLASS_ID);
    }

    public static void manualSetNoblePoints(int objId, int points) {
        StatsSet noble = _nobles.get(objId);
        if (noble == null) {
            return;
        }
        noble.set(POINTS, points);
        OlympiadDatabase.saveNobleData(objId);
    }

    public static synchronized boolean isNoble(int objId) {
        return _nobles.get(objId) != null;
    }

    public static synchronized void addNoble(L2PcInstance noble) {
        if (!_nobles.containsKey(noble.getObjectId())) {
            int classId = noble.getBaseClass();
            if (classId < 88) {  // Если это не 3-я профа, то исправляем со 2-й на 3-ю.
                for (ClassId id : ClassId.values()) {
                    if (id == null) {
                        continue;
                    }
                    if (id.level() == 3
                            && id.getParent().getId() == classId) {
                        classId = id.getId();
                        break;
                    }
                }
            }

            StatsSet statDat = new StatsSet();
            statDat.set(CLASS_ID, classId);
            statDat.set(CHAR_NAME, noble.getName());
            statDat.set(POINTS, DEFAULT_POINTS);
            statDat.set(POINTS_PAST, 0);
            statDat.set(POINTS_PAST_STATIC, 0);
            statDat.set(COMP_DONE, 0);
            statDat.set(COMP_WIN, 0);
            statDat.set(COMP_LOOSE, 0);
            _nobles.put(noble.getObjectId(), statDat);
            OlympiadDatabase.saveNobleData();
        }
    }

    public static void removeNobleIp(L2PcInstance player, boolean error) {
        //if (Config.ALT_OLY_SAME_IP)
        //	return;

        _ips.remove(player.getIP());
        _hwids.remove(player.getHWID());

        if (!error && Config.OLY_ALT_REWARD) {
            player.addItem("OlyRewardReturn", Config.OLY_ALT_REWARD_ITEM.id, (int) (Config.OLY_ALT_REWARD_ITEM.count / 2), player, true);
        }
    }

    public static synchronized void clearPoints(int nobleId) {
        if (!Config.CLEAR_OLY_BAN) {
            return;
        }

        StatsSet nobleInfo = _nobles.get(nobleId);
        if (nobleInfo != null) {
            nobleInfo.set(POINTS, 0);
        }
    }
}
