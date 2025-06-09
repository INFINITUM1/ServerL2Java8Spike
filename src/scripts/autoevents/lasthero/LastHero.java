package scripts.autoevents.lasthero;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.masspvp.massPvp;

public class LastHero {

    protected static final Logger _log = Logger.getLogger(LastHero.class.getName());
    private static final long _arRestart = (Config.ELH_ARTIME * 60000);
    private static final long _regTime = (Config.ELH_REGTIME * 60000);
    private static final long _anTime = (Config.ELH_ANNDELAY * 60000);
    private static final long _tpDelay = (Config.ELH_TPDELAY * 60000);
    private static final long _nextTime = (Config.ELH_NEXT * 60000);
    private static final int _minPlayers = Config.ELH_MINP;
    private static final int _maxPlayers = Config.ELH_MAXP;
    private static final Location _tpLoc = Config.ELH_TPLOC;
    private static final int _ticketId = Config.ELH_TICKETID;
    private static final int _ticketCount = Config.ELH_TICKETCOUNT;
    private static final ConcurrentLinkedQueue<Integer> _players = new ConcurrentLinkedQueue<>();
    //private static final FastList<EventReward> _rewards = Config.ELH_REWARDS;
    private static final ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<>();
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
    private static LastHero _event;

    public static void init() {
        _event = new LastHero();
        _event.load();
    }

    public static LastHero getEvent() {
        return _event;
    }

    public void load() {
        checkTimer();
        if (Config.LH_CUSTOM_ITEMS) {
            loadItemSettings();
        }
    }

    private static void loadItemSettings() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/last_hero_items.xml");
            if (!file.exists()) {
                _log.config("TvTEvent [ERROR]: data/last_hero_items.xml doesn't exist");
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
            _log.warning("LastHero [ERROR]: loadItemSettings() " + e.toString());
        }
    }

    public void checkTimer() {
        ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _arRestart);
        System.out.println("EventManager: Last Hero, start after " + (_arRestart / 60000) + " min.");
    }

    public class StartTask implements Runnable {

        public void run() {
            if (_state == EventState.WAIT) {
                startEvent();
            }
        }
    }

    private void startEvent() {
        _state = EventState.REG;

        announce(Static.LH_STARTED);
        announce(Static.LH_REG_FOR_S1.replace("%a%", String.valueOf(((_regTime / 60000) + 1))));
        System.out.println("EventManager: Last Hero, registration opened.");

        if (Config.EVENT_REG_POPUP) {
            for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                if (player == null || player.getLevel() < Config.EVENT_REG_POPUPLVL) {
                    continue;
                }


                    player.sendPacket(new ConfirmDlg(614, "Принять участие в ивенте -Последний герой-?", 106));

            }
        }

        _ips.clear();
        _players.clear();
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
                    announce(Static.LH_REG_IN);
                    announce(Static.LH_REG_LOST_S1.replace("%a%", String.valueOf(((regMin / 60000) + 1))));
                    if (_players.isEmpty()) {
                        announce(Static.LH_NO_PLAYESR_YET);
                    } else {
                        announce(Static.LH_REGD_PLAYESR.replace("%a%", String.valueOf(_players.size())));
                    }
                    Thread.sleep(_anTime);
                } catch (InterruptedException e) {
                }
            }
            announce(Static.LH_REG_CLOSED);
            _state = EventState.BATTLE;

            if (_players.size() < _minPlayers) {
                _state = EventState.WAIT;
                announce(Static.LH_CANC_NO_PLAYERS);
                announce(Static.LH_NEXT_TIME.replace("%a%", String.valueOf((_nextTime / 60000))));
                System.out.println("EventManager: Last Hero, canceled: no players.");
                System.out.println("EventManager: Last Hero, next start after " + (_nextTime / 60000) + " min.");
                ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
                return;
            }
            announce(Static.LH_BATTLE_AFTER.replace("%a%", String.valueOf((_tpDelay / 60000))));
            System.out.println("EventManager: Last Hero, battle start after " + (_tpDelay / 60000) + " min.");
            ThreadPoolManager.getInstance().scheduleGeneral(new StartBattle(), _tpDelay);
        }
    }

    public class StartBattle implements Runnable {

        public StartBattle() {
        }

        @Override
        public void run() {
            L2PcInstance player = null;
            for (Integer player_id : _players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    _players.remove(player_id);
                    continue;
                }

                /*if (player.isDead()) {
                 notifyDeath(player);
                 }*/
                if (player.isDead()) {
                    player.restoreExp(100.0);
                    player.doRevive();
                }
                player.setEventChannel(6);
            }

            for (Integer player_id : _players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    _players.remove(player_id);
                    continue;
                }

                if (Config.LH_CUSTOM_ITEMS) {
                    equipPlayer(player, _tvtItems.get(player.getClassId().getId()));
                } else if (Config.FORBIDDEN_EVENT_ITMES) {
                    // снятие переточеных вещей
                    for (L2ItemInstance item : player.getInventory().getItems()) {
                        if (item == null) {
                            continue;
                        }

                        if (item.notForOly()) {
                            player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
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

                if (player.getTrainedBeast() != null) {
                    player.getTrainedBeast().doDespawn();
                }

                if (player.getParty() != null) {
                    player.getParty().oustPartyMember(player);
                }

                player.teleToLocationEvent(_tpLoc.x + Rnd.get(300), _tpLoc.y + Rnd.get(300), _tpLoc.z);
                player.stopAllEffects();
                player.doEventBuff();
                player.setCurrentCp(player.getMaxCp());
                player.setCurrentHp(player.getMaxHp());
                player.setCurrentMp(player.getMaxMp());
                player.setPVPArena(true);
                player.setTeam(1);
            }
            player = null;
            ThreadPoolManager.getInstance().scheduleGeneral(new WinTask(), 10000);
            System.out.println("EventManager: Last Hero, battle started.");
        }
    }

    public class WinTask implements Runnable {

        public WinTask() {
        }

        @Override
        public void run() {
            if (_players.size() == 1) {
                announceWinner(_players.peek());
            } else {
                ThreadPoolManager.getInstance().scheduleGeneral(new WinTask(), 10000);
            }
        }
    }

    public void announceWinner(Integer player_id) {
        if (player_id == null) {
            return;
        }

        L2PcInstance player = L2World.getInstance().getPlayer(player_id);
        if (player == null) {
            return;
        }

        player.setEventChannel(1);
        _state = EventState.WAIT;
        announce(Static.LH_DONE);
        announce(Static.LH_WINNER.replace("%a%", player.getName()));
        announce(Static.LH_NEXT_AFTER.replace("%a%", String.valueOf((_nextTime / 60000))));
        System.out.println("EventManager: Last Hero, finished; palyer " + player.getName() + " win.");
        System.out.println("EventManager: Last Hero, next start after " + (_nextTime / 60000) + " min.");
        if (!player.isHero()) {
            player.setHero(Config.ELH_HERO_DAYS);
        }
        player.setPVPArena(false);

        FastList<EventReward> rewards = Config.ELH_REWARDS;
        for (FastList.Node<EventReward> k = rewards.head(), endk = rewards.tail(); (k = k.getNext()) != endk;) {
            EventReward reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                L2ItemInstance rewItem = ItemTable.getInstance().createItem("LastHero", reward.id, reward.count, player, null);
                player.getInventory().addItem("LastHero", rewItem, player, null);
            }
        }

        player.restoreEventSkills();
        if (Config.LH_CUSTOM_ITEMS) {
            restoreItems(player);
        }
        player.teleToLocation(82737, 148571, -3470);

        ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
    }

    private boolean foundIp(String ip) {
        return (_ips.contains(ip));
    }

    public void regPlayer(L2PcInstance player) {
        if (_state != EventState.REG) {
            player.sendHtmlMessage("-Последний герой-", "Регистрация еще не обьявлялась<br1> Приходите позже ;).");
            return;
        }

        if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(player.getName())) {
            player.sendHtmlMessage("-Последний герой-", "Вы уже зарегистрированы на TvT.");
            return;
        }

        if (player.getKarma() > 0 || player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("У вас плохая карма.");
            return;
        }

        if (Config.MASS_PVP && massPvp.getEvent().isReg(player)) {
            player.sendHtmlMessage("-Последний герой-", "Удачи на евенте -Масс ПВП-");
            return;
        }
        if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player)) {
            player.sendHtmlMessage("-Последний герой-", "Удачи на евенте -Захват базы-");
            return;
        }

        if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode()) {
            player.sendHtmlMessage("-Последний герой-", "Вы уже зарегистрированы на олимпиаде.");
            return;
        }

        if (player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("-Последний герой-", "С проклятым оружием нельзя.");
            return;
        }

        if (_players.size() >= _maxPlayers) {
            player.sendHtmlMessage("-Последний герой-", "Достигнут предел игроков.");
            return;
        }

        if (_players.contains(player.getObjectId())) {
            player.sendHtmlMessage("-Последний герой-", "Вы уже зарегистрированы.");
            return;
        }

        if (!Config.EVENTS_SAME_IP && foundIp(player.getIP())) {
            player.sendHtmlMessage("-Последний герой-", "С вашего IP уже есть игрок.");
            return;
        }

        if (_ticketId > 0) {
            L2ItemInstance coin = player.getInventory().getItemByItemId(_ticketId);
            if (coin == null || coin.getCount() < _ticketCount) {
                player.sendHtmlMessage("-Последний герой-", "Участив в ивенте платное.");
                return;
            }
            player.destroyItemByItemId("lasthero", _ticketId, _ticketCount, player, true);
        }

        _players.add(player.getObjectId());
        if (!Config.EVENTS_SAME_IP) {
            _ips.add(player.getIP());
        }
        //player.sendHtmlMessage("-Последний герой-", "Регистрация завершена.");
        NpcHtmlMessage npcHtmlMessage = NpcHtmlMessage.id(0);
        npcHtmlMessage.setFile("data/html/events/lasthero_registration_done.htm");
        player.sendPacket(npcHtmlMessage);
    }

    public void delPlayer(L2PcInstance player) {
        if (!(_players.contains(player.getObjectId()))) {
            player.sendHtmlMessage("-Последний герой-", "Вы не зарегистрированы.");
            return;
        }

        _players.remove(player.getObjectId());
        if (!Config.EVENTS_SAME_IP) {
            _ips.remove(player.getIP());
        }
        player.sendHtmlMessage("-Последний герой-", "Регистрация отменена.");
    }

    public void notifyFail(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return;
        }

        if (_players.contains(player.getObjectId())) {
            _players.remove(player.getObjectId());
            if (!Config.EVENTS_SAME_IP) {
                _ips.remove(player.getIP());
            }
            player.setEventChannel(1);
            player.setXYZ(82737, 148571, -3470);
            player.setPVPArena(false);
            if (Config.LH_CUSTOM_ITEMS) {
                restoreItems(player);
            }
        }
    }

    public void notifyDeath(L2PcInstance player) {
        if (_state == EventState.WAIT || _state == EventState.REG) {
            return;
        }

        if (_players.contains(player.getObjectId())) {
            _players.remove(player.getObjectId());
            if (!Config.EVENTS_SAME_IP) {
                _ips.remove(player.getIP());
            }

            player.sendCritMessage("Вы проиграли...");
            try {
                player.teleToLocationEvent(82737, 148571, -3470);
            } catch (Exception e) {
            }
            player.setEventChannel(1);
            player.doRevive();
            player.doEventBuff();
            player.setCurrentHp(player.getMaxHp());
            player.setCurrentMp(player.getMaxMp());
            player.setCurrentCp(player.getMaxCp());
            player.setPVPArena(false);
            player.setTeam(0);
            if (Config.LH_CUSTOM_ITEMS) {
                restoreItems(player);
            }
            player.restoreEventSkills();
        }
    }

    public boolean isRegged(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return false;
        }

        return _players.contains(player.getObjectId());
    }

    private void announce(String text) {
        Announcements.getInstance().announceToAll(text);
    }

    public boolean isInBattle() {
        return (_state == EventState.BATTLE);
    }

    public boolean forbPotion(int itemId) {
        return Config.ELH_FORB_POTIONS.contains(itemId);
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
