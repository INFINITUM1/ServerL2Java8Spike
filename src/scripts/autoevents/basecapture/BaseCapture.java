package scripts.autoevents.basecapture;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import scripts.ai.CaptureBase;
import scripts.autoevents.lasthero.LastHero;
import scripts.autoevents.masspvp.massPvp;

import javax.xml.parsers.DocumentBuilderFactory;

public class BaseCapture {

    protected static final Logger _log = Logger.getLogger(BaseCapture.class.getName());
    private static final long _arRestart = (Config.EBC_ARTIME * 60000);
    private static final long _regTime = (Config.EBC_REGTIME * 60000);
    private static final long _anTime = (Config.EBC_ANNDELAY * 60000);
    private static final long _tpDelay = Config.EBC_TPDELAY;
    private static final long _deathDelay = (Config.EBC_DEATHLAY * 1000);
    private static final long _nextTime = (Config.EBC_NEXT * 60000);
    private static final int _minLvl = Config.EBC_MINLVL;
    private static CaptureBase _base1 = null;
    private static CaptureBase _base2 = null;
    private static final FastList<Location> _tpLoc1 = Config.EBC_TPLOC1;
    private static final FastList<Location> _tpLoc2 = Config.EBC_TPLOC2;
    private int _returnCount = 0;
    private final FastList<Location> _returnLocs = Config.EBC_RETURN_COORDINATES;
    //private FastList<L2PcInstance> _team1 = new FastList<L2PcInstance>();
    //private FastList<L2PcInstance> _team2 = new FastList<L2PcInstance>();
    private static final FastMap<Integer, ConcurrentLinkedQueue<Integer>> _teams = new FastMap<>();
    private static final FastList<EventReward> _rewards = Config.EBC_REWARDS;
    //private static final FastList<Location> _locs = new FastList<>();
    private static final FastList<String> _teamNames = new FastList<>();
    private static final ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> _hwids = new ConcurrentLinkedQueue<>();
    //

    private static final Map<Integer, FastList<Integer>> _tvtItems = new ConcurrentHashMap<>();
    private static final Map<Integer, FastList<Integer>> _storedItems = new ConcurrentHashMap<>();
    private static final FastList<EventReward> _times = new FastList<>();

    static enum EventState {

        WAIT,
        REG,
        BATTLE
    }
    private static EventState _state = EventState.WAIT;
    private static BaseCapture _event;

    public static void init() {
        _event = new BaseCapture();
        _event.load();
    }

    public static BaseCapture getEvent() {
        return _event;
    }

    public void load() {

        _teamNames.add(Config.EBC_BASE1NAME);
        _teamNames.add(Config.EBC_BASE2NAME);

        _ips.clear();
        _hwids.clear();
        _teams.clear();
        _teams.put(0, new ConcurrentLinkedQueue<>());
        _teams.put(1, new ConcurrentLinkedQueue<>());

        _returnCount = _returnLocs.size() - 1;

        checkTimer();
        if (Config.EBC_CUSTOM_ITEMS) {
            loadItemSettings();
        }
    }

    private static void loadItemSettings() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/base_capture_items.xml");
            if (!file.exists()) {
                _log.config("TvTEvent [ERROR]: data/base_capture_items.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            //FastList<String> strings = new FastList<String>();
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("class".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            FastList<Integer> list = new FastList<Integer>();
                            int classId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("equipment".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    String[] items = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String item : items) {
                                        if (item.equalsIgnoreCase("")) {
                                            continue;
                                        }

                                        list.add(Integer.parseInt(item));
                                    }
                                }
                            }
                            _tvtItems.put(classId, list);
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("BaseCapture [ERROR]: loadItemSettings() " + e.toString());
        }
    }

    public void checkTimer() {
        ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _arRestart);
        System.out.println("EventManager: Base Capture, start after " + (_arRestart / 60000) + " min.");
    }

    public class StartTask implements Runnable {

        @Override
        public void run() {
            if (_state == EventState.WAIT) {
                startEvent();
            }
        }
    }

    public void startEvent() {
        _ips.clear();
        _hwids.clear();
        _teams.clear();
        _teams.put(0, new ConcurrentLinkedQueue<>());
        _teams.put(1, new ConcurrentLinkedQueue<>());

        _state = EventState.REG;

        announce(Static.EBC_STARTED);
        announce(Static.EBC_REG_FOR_S1.replace("%a%", String.valueOf(((_regTime / 60000) + 1))));
        System.out.println("EventManager: Base Capture, registration opened.");

        if (Config.EVENT_REG_POPUP) {
            for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                if (player == null || player.getLevel() < Config.EVENT_REG_POPUPLVL) {
                    continue;
                }

                player.sendPacket(new ConfirmDlg(614, "Принять участие в ивенте -Захват базы-?", 104));
            }
        }
        _storedItems.clear();
        ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceTask(), _anTime);
    }

    public class AnnounceTask implements Runnable {

        @Override
        public void run() {
            if (_state != EventState.REG) {
                return;
            }

            long regMin = _regTime;
            for (int i = 0; i < _regTime; i += _anTime) {
                try {
                    regMin -= _anTime;
                    announce(Static.EBC_STARTED);
                    announce(Static.EBC_REG_LOST_S1.replace("%a%", String.valueOf(((regMin / 60000) + 1))));
                    if (_teams.get(0).isEmpty() || _teams.get(1).isEmpty()) {
                        announce(Static.EBC_NO_PLAYESR_YET);
                    } else {
                        String announs = Static.EBC_PLAYER_TEAMS.replace("%a%", String.valueOf(_teams.get(0).size()));
                        announs = announs.replace("%b%", Config.EBC_BASE1NAME);
                        announs = announs.replace("%c%", String.valueOf(_teams.get(1).size()));
                        announs = announs.replace("%d%", Config.EBC_BASE2NAME);
                        announce(announs);
                    }
                    Thread.sleep(_anTime);
                } catch (InterruptedException e) {
                }
            }
            announce(Static.EBC_REG_CLOSED);
            _state = EventState.BATTLE;

            if (_teams.get(0).size() < Config.EBC_MINP || _teams.get(1).size() < Config.EBC_MINP) {
                announce(Static.EBC_NO_PLAYERS);
                System.out.println("EventManager: Base Capture, canceled: no players.");
                _state = EventState.WAIT;
                ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
                announce(Static.EBC_NEXT_AFTER.replace("%a%", String.valueOf((_nextTime / 60000))));
                System.out.println("EventManager: Base Capture, next start after " + (_nextTime / 60000) + " min.");

                _state = EventState.WAIT;
                _ips.clear();
                _hwids.clear();
                _teams.clear();
                _teams.put(0, new ConcurrentLinkedQueue<>());
                _teams.put(1, new ConcurrentLinkedQueue<>());
                return;
            }
            //announce(Static.EBC_BATTLE_STRTED_AFTER.replace("%a%", String.valueOf(_tpDelay)));

            L2PcInstance player = null;
            for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
                ConcurrentLinkedQueue<Integer> players = e.getValue();
                for (Integer player_id : players) {
                    if (player_id == null) {
                        continue;
                    }

                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    player.sendMessage(Static.EBC_BATTLE_STRTED_AFTER.replace("%a%", String.valueOf(_tpDelay)));
                }
            }
            System.out.println("EventManager: Base Capture, battle start after " + _tpDelay + " sec.");
            ThreadPoolManager.getInstance().scheduleGeneral(new StartBattle(), _tpDelay * 1000);
        }
    }

    public class StartBattle implements Runnable {

        public StartBattle() {
        }

        @Override
        public void run() {
            _base1 = (CaptureBase) EventManager.getInstance().doSpawn(Config.EBC_BASE1ID, Config.EBC_BASE1_LOC, 0);
            _base1.setTitle(Config.EBC_BASE1TITLE);
            _base1.setName(Config.EBC_BASE1NAME);
            _base2 = (CaptureBase) EventManager.getInstance().doSpawn(Config.EBC_BASE2ID, Config.EBC_BASE2_LOC, 0);
            _base2.setTitle(Config.EBC_BASE2TITLE);
            _base2.setName(Config.EBC_BASE2NAME);

            L2PcInstance player = null;
            for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
                ConcurrentLinkedQueue<Integer> players = e.getValue();

                for (Integer player_id : players) {
                    if (player_id == null) {
                        continue;
                    }

                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    if (player.isDead()) {
                        player.restoreExp(100.0);
                        player.doRevive();
                    }
                }
            }

            for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
                Integer teamId = e.getKey();
                ConcurrentLinkedQueue<Integer> players = e.getValue();
                teamId += 1;

                for (Integer player_id : players) {
                    if (player_id == null) {
                        continue;
                    }

                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    if (Config.EBC_CUSTOM_ITEMS) {
                        equipPlayer(player, _tvtItems.get(player.getClassId().getId()));
                    } else if (Config.FORBIDDEN_EVENT_ITMES) {
                        // снятие переточеных вещей
                        for (L2ItemInstance item : player.getInventory().getItems()) {
                            if (item == null) {
                                continue;
                            }

                            if (item.notForOly()) {
                                player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
                            }
                        }
                    }

                    for (L2Skill s : player.getAllSkills()) {
                        if (s == null) {
                            continue;
                        }
                        if (s.isForbidEvent()) {
                            player.removeStatsOwner(s);
                        }
                    }

                    // Remove Summon's Buffs
                    if (player.getPet() != null) {
                        L2Summon summon = player.getPet();
                        summon.stopAllEffects();

                        if (summon.isPet()) {
                            summon.unSummon(player);
                        }
                    }

                    player.setChannel(4);

                    player.teleToLocationEvent(getEventLoc(teamId - 1));

                    player.stopAllEffects();
                    player.doEventBuff();

                    player.setCurrentCp(player.getMaxCp());
                    player.setCurrentHp(player.getMaxHp());
                    player.setCurrentMp(player.getMaxMp());
                    player.setTeam(teamId);
                    player.setPVPArena(true);
                }
            }

            System.out.println("EventManager: Base Capture, battle started.");
            ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(), _deathDelay);
        }
    }

    public class DeathTask implements Runnable {

        public DeathTask() {
        }

        public void run() {
            if (_state != EventState.BATTLE) {
                return;
            }
            L2PcInstance player = null;
            for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
                Integer teamId = e.getKey();
                ConcurrentLinkedQueue<Integer> players = e.getValue();
                teamId += 1;

                for (Integer player_id : players) {
                    if (player_id == null) {
                        continue;
                    }

                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    if (!player.isDead()) {
                        continue;
                    }

                    player.doRevive();
                    player.stopAllEffects();
                    player.doEventBuff();

                    player.setCurrentCp(player.getMaxCp());
                    player.setCurrentHp(player.getMaxHp());
                    player.setCurrentMp(player.getMaxMp());

                    player.broadcastStatusUpdate();
                    player.setTeam(teamId);
                    player.setChannel(4);

                    player.teleToLocationEvent(getEventLoc(teamId - 1));
                    player.setPVPArena(true);
                }
            }
            ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(), _deathDelay);
        }
    }

    public void notifyBaseDestroy(int baseId) {
        if (_state != EventState.BATTLE) {
            return;
        }

        int winTeam = 0;
        if (baseId == Config.EBC_BASE1ID) {
            winTeam = 1;
        }

        if (_base1 != null) {
            _base1.deleteMe();
        }

        if (_base2 != null) {
            _base2.deleteMe();
        }

        _base1 = null;
        _base2 = null;

        _state = EventState.WAIT;
        announce(Static.EBC_FINISHED);
        announce(Static.EBC_TEAM_S1_WIN.replace("%a%", _teamNames.get(winTeam)));

        System.out.println("EventManager: Base Capture, finished; team " + (winTeam + 1) + " win.");

        ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
        announce(Static.EBC_NEXT_AFTER.replace("%a%", String.valueOf((_nextTime / 60000))));
        System.out.println("EventManager: Base Capture, next start after " + (_nextTime / 60000) + " min.");

        try {
            validateWinners(winTeam);
        } catch (Exception e) {
            //
        }

        prepareNextEvent();
    }

    private void validateWinners(int team) {
        L2PcInstance player = null;
        ConcurrentLinkedQueue<Integer> players = _teams.get(team);
        for (Integer player_id : players) {
            if (player_id == null) {
                continue;
            }

            player = L2World.getInstance().getPlayer(player_id);
            if (player == null) {
                continue;
            }

            for (FastList.Node<EventReward> k = _rewards.head(), endk = _rewards.tail(); (k = k.getNext()) != endk;) {
                EventReward reward = k.getValue();
                if (reward == null) {
                    continue;
                }

                if (Rnd.get(100) < reward.chance) {
                    player.addItem("Npc.giveItem", reward.id, reward.count, player, true);
                }
            }
        }

        if (Config.LH_CUSTOM_ITEMS) {
            restoreItems(player);
        }

    }

    private void prepareNextEvent() {
        L2PcInstance player = null;
        for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
            //Integer teamId = e.getKey();
            ConcurrentLinkedQueue<Integer> players = e.getValue();

            for (Integer player_id : players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    continue;
                }

                if (player.isDead()) {
                    player.restoreExp(100.0);
                    player.doRevive();
                }
            }
        }

        for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
            //Integer teamId = e.getKey();
            ConcurrentLinkedQueue<Integer> players = e.getValue();

            for (Integer player_id : players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    continue;
                }

                player.setCurrentCp(player.getMaxCp());
                player.setCurrentHp(player.getMaxHp());
                player.setCurrentMp(player.getMaxMp());
                player.setChannel(1);
                player.broadcastStatusUpdate();
                player.setTeam(0);
                player.setPVPArena(false);
                player.teleToLocationEvent(getRandomReturnLoc());
                if (Config.EBC_CUSTOM_ITEMS) {
                    restoreItems(player);
                }
            }
        }

        _ips.clear();
        _hwids.clear();
        _teams.clear();
        _teams.put(0, new ConcurrentLinkedQueue<>());
        _teams.put(1, new ConcurrentLinkedQueue<>());
    }

    private Location getRandomReturnLoc() {
        return _returnLocs.get(Rnd.get(0, _returnCount));
    }

    private boolean foundIp(String ip) {
        return (_ips.contains(ip));
    }

    private boolean foundHWID(String id) {
        return (_hwids.contains(id));
    }

    public void regPlayer(L2PcInstance player) {
        if (_state != EventState.REG) {
            player.sendHtmlMessage("-Захват базы-", "Регистрация на эвент закрыта.");
            return;
        }
        if (_state == EventState.BATTLE) {
            player.sendHtmlMessage("-Захват базы-", "Битва уже обьявлена!");
            return;
        }

        if (player.getKarma() > 0 || player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("У вас плохая карма.");
            return;
        }

        if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(player.getName())) {
            player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы на TvT.");
            return;
        }

        if (Config.MASS_PVP && massPvp.getEvent().isReg(player)) {
            player.sendMessage("Удачи на евенте -Масс ПВП-");
            return;
        }

        if (Config.ELH_ENABLE && LastHero.getEvent().isRegged(player)) {
            player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы в -Последний герой- эвенте.");
            return;
        }
        if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player)) {
            player.sendHtmlMessage("-Захват базы-", "Удачи на евенте -Захват базы-");
            return;
        }

        if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode()) {
            player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы на олимпиаде.");
            return;
        }

        if (player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("-Захват базы-", "С проклятым оружием нельзя.");
            return;
        }

        if (_teams.get(0).size() >= Config.EBC_MAXP && _teams.get(1).size() >= Config.EBC_MAXP) {
            player.sendHtmlMessage("-Захват базы-", "Достигнут предел игроков.");
            return;
        }

        if (_teams.get(0).contains(player.getObjectId()) || _teams.get(1).contains(player.getObjectId())) {
            player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы.");
            return;
        }

        if (!Config.EVENTS_SAME_IP && foundIp(player.getIP())) {
            player.sendHtmlMessage("-Захват базы-", "С вашего IP уже есть игрок.");
            return;
        }

        if (Config.EBC_TICKETID > 0) {
            L2ItemInstance coin = player.getInventory().getItemByItemId(Config.EBC_TICKETID);
            if (coin == null || coin.getCount() < Config.EBC_TICKETCOUNT) {
                player.sendHtmlMessage("-Захват базы-", "Участив в ивенте платное.");
                return;
            }
        }

        int team = 0;
        if (_teams.get(0).size() == _teams.get(1).size()) {
            team = Rnd.get(0, 1);
        } else if (_teams.get(0).size() > _teams.get(1).size()) {
            team = 1;
        }

        _teams.get(team).add(player.getObjectId());
        player.sendHtmlMessage("-Захват базы-", "Регистрация завершена, ваша команда: <br> " + _teamNames.get(team) + ".");
        if (!Config.EVENTS_SAME_IP) {
            _ips.add(player.getIP());
        }
    }

    public void delPlayer(L2PcInstance player) {
        if (_state != EventState.REG) {
            player.sendHtmlMessage("-Захват базы-", "Сейчас не регистрационный период.");
            return;
        }

        if (!(_teams.get(0).contains(player.getObjectId())) && !(_teams.get(1).contains(player.getObjectId()))) {
            player.sendHtmlMessage("-Захват базы-", "Вы не зарегистрированы.");
            return;
        }

        if (_teams.get(0).contains(player.getObjectId())) {
            _teams.get(0).remove(player.getObjectId());
        } else if (_teams.get(1).contains(player.getObjectId())) {
            _teams.get(1).remove(player.getObjectId());
        }

        if (!Config.EVENTS_SAME_IP) {
            _ips.remove(player.getIP());
        }

        player.sendHtmlMessage("-Захват базы-", "Регистрация отменена.");
    }

    public static void onLogout(L2PcInstance player) {
        /*if (!isRegBattle(player)) {
         return;
         }*/

 /*Location loc = null;
         if (_teams.get(0).contains(player.getObjectId())) {
         loc = _locs.get(0);
         } else if (_teams.get(1).contains(player.getObjectId())) {
         loc = _locs.get(1);
         }

         if (loc == null) {
         return;
         }

         player.teleToLocation(loc);*/
    }

    public static void onLogin(L2PcInstance player) {
        if (!isRegBattle(player)) {
            return;
        }

        int teamId = 0;
        Location loc = null;
        if (_teams.get(0).contains(player.getObjectId())) {
            loc = getEventLoc(0);
            teamId = 1;
        } else if (_teams.get(1).contains(player.getObjectId())) {
            loc = getEventLoc(1);
            teamId = 2;
        }

        if (loc == null) {
            return;
        }

        if (player.isDead()) {
            player.restoreExp(100.0);
            player.doRevive();
        }

        if (Config.FORBIDDEN_EVENT_ITMES) {
            // снятие переточеных вещей
            for (L2ItemInstance item : player.getInventory().getItems()) {
                if (item == null) {
                    continue;
                }

                if (item.notForOly()) {
                    player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
                }
            }
        }

        for (L2Skill s : player.getAllSkills()) {
            if (s == null) {
                continue;
            }
            if (s.isForbidEvent()) {
                player.removeStatsOwner(s);
            }
        }

        player.setChannel(4);
        player.teleToLocationEvent(loc);

        player.stopAllEffects();
        player.doEventBuff();

        player.setCurrentCp(player.getMaxCp());
        player.setCurrentHp(player.getMaxHp());
        player.setCurrentMp(player.getMaxMp());
        player.setTeam(teamId);
        player.setPVPArena(true);
        if (Config.EBC_CUSTOM_ITEMS) {
            restoreItems(player);
        }
    }

    public boolean isRegged(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return false;
        }

        return (_teams.get(0).contains(player.getObjectId()) || _teams.get(1).contains(player.getObjectId()));
    }

    public boolean isInBattle(L2PcInstance player) {
        if (player == null) {
            return false;
        }

        if (_state != EventState.BATTLE) {
            return false;
        }

        return (_teams.get(0).contains(player.getObjectId()) || _teams.get(1).contains(player.getObjectId()));
    }

    public static boolean isRegBattle(L2PcInstance player) {
        if (player == null) {
            return false;
        }

        if (_state != EventState.BATTLE) {
            return false;
        }

        return (_teams.get(0).contains(player.getObjectId()) || _teams.get(1).contains(player.getObjectId()));
    }

    public boolean isInSameTeam(L2PcInstance player1, L2PcInstance player2) {
        if (_state != EventState.BATTLE) {
            return false;
        }

        if (player1 == null || player2 == null) {
            return false;
        }

        return (_teams.get(0).contains(player1.getObjectId()) && _teams.get(0).contains(player2.getObjectId()))
                || (_teams.get(1).contains(player1.getObjectId()) && _teams.get(1).contains(player2.getObjectId()));
    }

    public boolean isInTeam1(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return false;
        }

        return _teams.get(0).contains(player.getObjectId());
    }

    public boolean isInTeam2(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return false;
        }

        return _teams.get(1).contains(player.getObjectId());
    }

    private void announce(String text) {
        Announcements.getInstance().announceToAll(text);
    }

    private static Location getEventLoc(int teamId) {
        switch (teamId) {
            case 0:
                //int rnd1 = (Rnd.get(0, _tpLoc1.size() - 1));
                //System.out.println("##getEventLoc##team: " + teamId + "##random: " + rnd1 + "##coord: " + _tpLoc1.get(rnd1) + "##");
                return _tpLoc1.get((Rnd.get(0, _tpLoc1.size() - 1)));
            case 1:
                //int rnd2 = (Rnd.get(0, _tpLoc2.size() - 1));
                //System.out.println("##getEventLoc##team: " + teamId + "##random: " + rnd2 + "##coord: " + _tpLoc2.get(rnd2) + "##");
                return _tpLoc2.get((Rnd.get(0, _tpLoc2.size() - 1)));
        }
        return null;
    }

    private static void equipPlayer(L2PcInstance player, FastList<Integer> items) {
        if (player == null
                || items == null
                || items.isEmpty()) {
            return;
        }
        //6379,512,6380,6381,7577
        //int[] items = {6379, 512, 6380, 6381, 7577, 858, 858, 889, 889, 920};

        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (item.isEquipped()) {
                storeItem(player.getObjectId(), item.getObjectId());
                player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
            }
        }

        L2ItemInstance item = null;
        for (int itemId : items) {
            item = player.getInventory().addItem("TVT", itemId, 1, player, null);
            if (item == null) {
                continue;
            }

            item.setTimeOfUse(1);
            player.getInventory().equipItemAndRecord(item);
        }

        //
        player.sendItems(false);
        player.broadcastUserInfo();
        //
    }

    private static void storeItem(int charId, int itemObjId) {
        putStoredItem(charId, itemObjId, _storedItems.get(charId));
    }

    private static void putStoredItem(int charId, int itemObjId, FastList<Integer> items) {
        if (items == null) {
            items = new FastList<>();
        }
        items.add(itemObjId);
        _storedItems.put(charId, items);
    }

    private static void restoreItems(L2PcInstance player) {
        equipStoredItems(player, _storedItems.get(player.getObjectId()));
    }

    private static void equipStoredItems(L2PcInstance player, FastList<Integer> items) {
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (item.isEquipped()) {
                player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
            }

            if (item.getTimeOfUse() > 0) {
                player.getInventory().destroyItem("TVT", item, player, player);
            }
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        L2ItemInstance item = null;
        for (int itemId : items) {
            item = player.getInventory().getItemByObjectId(itemId);
            if (item == null) {
                continue;
            }

            if (item.getOwnerId() != player.getObjectId()) {
                continue;
            }

            if (item.getLocation() != L2ItemInstance.ItemLocation.INVENTORY || item.getLocation() == L2ItemInstance.ItemLocation.PAPERDOLL) {
                continue;
            }

            if (!item.checkForEquipped(player)) {
                continue;
            }

            player.getInventory().equipItemAndRecord(item);
        }

        //
        player.sendItems(false);
        player.broadcastUserInfo();
        //
    }

}
