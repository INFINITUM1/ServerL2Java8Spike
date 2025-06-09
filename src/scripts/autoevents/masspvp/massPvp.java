package scripts.autoevents.masspvp;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.lasthero.LastHero;

import javolution.util.FastList;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;

public class massPvp {

    private static EventManager _event = EventManager.getInstance();
    private ScheduledFuture<?> _autoStart = null;
    private ScheduledFuture<?> _cycleStart = null;
    private static long _arRestart = (Config.MPVP_RTIME * 60000);
    private static long _regTime = (Config.MPVP_REG * 60000);
    private static long _anTime = (Config.MPVP_ANC * 60000);
    private static long _tpTime = (Config.MPVP_TP * 60000);
    private static long _prTime = (Config.MPVP_PR * 1000);
    //private static long _wtTime = (Config.MPVP_WT * 60000);
    private static long _maxTime = (Config.MPVP_MAX * 60000);
    private static long _nextTime = (Config.MPVP_NEXT * 24 * 60000);
    private int _curCycle = -1;
    private static Location _npcLoc = Config.MPVP_NPCLOC; // 116530, 76141, -2730
    //private static Location _tpLoc = Config.MPVP_TPLOC; // 116530, 76141, -2730
    //private static Location _clLoc = Config.MPVP_CLOC; // 
    //private static Location _winLoc = Config.MPVP_WLOC; //
    private boolean _active = false;
    private boolean _safe = true;
    private boolean _reg = false;
    private FastList<L2PcInstance> _curent = new FastList<L2PcInstance>();
    private FastList<L2PcInstance> _next = new FastList<L2PcInstance>();
    private FastList<L2PcInstance> _winners = new FastList<L2PcInstance>();
    private static ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<String>();
    private String _winName = "d";
    private static massPvp _instance;

    public static massPvp getEvent() {
        if (_instance == null) {
            _instance = new massPvp();
        }
        return _instance;
    }

    public void load() {
        checkTimer();
        //System.out.println("MassPvp: loaded.");
    }

    public void checkTimer() {
        long nextStart = _event.GetDBValue("massPvp", "nextStart") - System.currentTimeMillis();
        if (nextStart < _arRestart) {
            nextStart = _arRestart;
        }

        _autoStart = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), nextStart);
        System.out.println("EventManager: MassPvP, start after " + (nextStart / 60000) + " min.");
    }

    public class StartTask implements Runnable {

        public void run() {
            if (!_active) {
                startEvent();
            }
        }
    }

    public class AnnounceTask implements Runnable {

        public void run() {
            if (!_reg) {
                return;
            }

            long regMin = _regTime;

            for (int i = 0; i < _regTime; i += _anTime) {
                try {
                    if (!_reg) {
                        break;
                    }

                    regMin -= _anTime;
                    announce(Static.MPVP_STARTED);
                    announce(Static.MPVP_REG_FOR.replaceAll("%m%", String.valueOf((regMin / 60000))));
                    Thread.sleep(_anTime);
                } catch (InterruptedException e) {
                }
            }

            //ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceTask(), _anTime);
        }
    }

    public class CycleTask implements Runnable {

        private int _cycle;

        public CycleTask(int cycle) {
            _cycle = cycle;
        }

        public void run() {
            _reg = false;
            _curCycle = _cycle;
            if (_cycle == 1) {
                if (_curent.size() < 2) {
                    _curent.clear();
                    _ips.clear();
                    _autoStart.cancel(true);
                    _autoStart = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _arRestart);
                    announce(Static.MPVP_CANCELED_NO_PLAYERS);
                    return;
                }
                announce(Static.MPVP_REG_CLOSED);
                announce(Static.MPVP_TELE_ARENA_FOR.replaceAll("%m%", String.valueOf((_tpTime / 60000))));
                //announce("Через 30 секунд после прилета будет дано время на подготовку.");
                ThreadPoolManager.getInstance().scheduleGeneral(new StartTeleport(_cycle), _tpTime);
                return;
            }

            if (_cycle < Config.MPVP_MAXC) {
                announce(Static.MPVP_BEGIN_ROUND.replaceAll("%r%", String.valueOf(_cycle)));
            } else {
                _curent.clear();
                _curent.addAll(_winners);
                announce(Static.MPVP_FINAL_ROUND);
            }
            announce(Static.MPVP_TELE_ARENA_FOR.replaceAll("%m%", String.valueOf((_tpTime / 60000))));
            ThreadPoolManager.getInstance().scheduleGeneral(new StartTeleport(_cycle), _tpTime);
        }
    }

    public class StartTeleport implements Runnable {

        private int _cycle;

        public StartTeleport(int cycle) {
            _cycle = cycle;
        }

        public void run() {
            _safe = true;
            /*
             * for (int i = 0, n = _curent.size(); i < n; i++) { L2PcInstance
             * player = _curent.get(i);
             */
            for (FastList.Node<L2PcInstance> n = _curent.head(), end = _curent.tail(); (n = n.getNext()) != end;) {
                L2PcInstance player = n.getValue();
                if (player == null) {
                    continue;
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

                player.setChannel(7);
                Location _clLoc = getRandomClLoc();
                player.teleToLocationEvent(_clLoc.x + Rnd.get(300), _clLoc.y + Rnd.get(300), _clLoc.z);
                player.setCurrentCp(player.getMaxCp());
                player.setCurrentHp(player.getMaxHp());
                player.setCurrentMp(player.getMaxMp());
                player.setEventWait(true);
                //player.stopAllEffects();
            }
            //announce("Подождите 30 секунд, пока все прогрузятся!");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            announce(Static.MPVP_BATTLA_PREPARE_FOR.replaceAll("%s%", String.valueOf((_prTime / 1000))));

            /*
             * for (int i = 0, n = _curent.size(); i < n; i++) { L2PcInstance
             * player = _curent.get(i);
             */
            for (FastList.Node<L2PcInstance> n = _curent.head(), end = _curent.tail(); (n = n.getNext()) != end;) {
                L2PcInstance player = n.getValue();
                if (player == null) {
                    continue;
                }

                //player.stopAllEffects();

                // Remove Clan Skills
                if (player.getClan() != null) {
                    for (L2Skill skill : player.getClan().getAllSkills()) {
                        player.removeSkill(skill, false);
                    }
                }

                // Abort casting if player casting
                if (player.isCastingNow()) {
                    player.abortCast();
                }

                // Remove Hero Skills
                if (player.isHero()) {
                    for (L2Skill skill : HeroSkillTable.getHeroSkills()) {
                        player.removeSkill(skill, false);
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

                if (player.getCubics() != null) {
                    for (L2CubicInstance cubic : player.getCubics().values()) {
                        cubic.stopAction();
                        player.delCubic(cubic.getId());
                    }
                    player.getCubics().clear();
                }

                // Remove player from his party
                if (player.getParty() != null) {
                    player.getParty().removePartyMember(player);
                }

                player.sendSkillList();

                player.setCurrentCp(player.getMaxCp());
                player.setCurrentHp(player.getMaxHp());
                player.setCurrentMp(player.getMaxMp());
                SkillTable.getInstance().getInfo(1204, 2).getEffects(player, player);
                if (!player.isMageClass()) {
                    SkillTable.getInstance().getInfo(1086, 2).getEffects(player, player);
                } else {
                    SkillTable.getInstance().getInfo(1085, 3).getEffects(player, player);
                }
                player.broadcastUserInfo();
            }
            ThreadPoolManager.getInstance().scheduleGeneral(new StartFight(_cycle), _prTime);
        }
    }

    public class StartFight implements Runnable {

        private int _cycle;

        public StartFight(int cycle) {
            _cycle = cycle;
        }

        public void run() {
            for (int i = 5; i > 0; i--) {
                announce(Static.MPVP_BATTLE_BEGIN_FOR.replaceAll("%s%", String.valueOf(i)));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
            _safe = false;
            //for (int i = 0, n = _curent.size(); i < n; i++)
            for (FastList.Node<L2PcInstance> n = _curent.head(), end = _curent.tail(); (n = n.getNext()) != end;) {
                L2PcInstance player = n.getValue();
                if (player == null) {
                    _curent.remove(player);
                    continue;
                }
                player.setChannel(7);
                player.setCurrentCp(player.getMaxCp());
                player.setCurrentHp(player.getMaxHp());
                player.setCurrentMp(player.getMaxMp());
                player.setTeam(2);
                player.setEventWait(false);
                //player.setInOlumpiadStadium(false);				
                player.setPVPArena(true);
                player.sendPacket(SystemMessage.id(SystemMessageId.ENTERED_COMBAT_ZONE));
                player.broadcastUserInfo();
            }
            if (_cycle < Config.MPVP_MAXC) {
                announce(Static.MPVP_FIGHT);
            } else {
                announce(Static.MPVP_FINAL_FIGHT);
            }

            _cycleStart = ThreadPoolManager.getInstance().scheduleGeneral(new StopFight(_cycle, false), _maxTime);

            for (int i = 0; i < _maxTime; i += 10000) {
                try {
                    Thread.sleep(10000);
                    if (haveWinner()) {
                        break;
                    }
                } catch (InterruptedException e) {
                }
            }

            if (_active) {
                _cycleStart.cancel(true);
                ThreadPoolManager.getInstance().scheduleGeneral(new StopFight(_cycle, true), 4000);
            }
        }
    }

    protected boolean haveWinner() {
        if (_curent.size() == 1) {
            return true;
        }

        return false;
    }

    public class StopFight implements Runnable {

        private int _cycle;
        private boolean _last;

        public StopFight(int cycle, boolean last) {
            _cycle = cycle;
            _last = last;
        }

        public void run() {
            if (!_last) {
                for (int i = 5; i > 0; i--) {
                    String round_end_for = Static.MPVP_ROUND_END_FOR.replaceAll("%s%", String.valueOf(i));
                    announce(round_end_for.replaceAll("%r%", String.valueOf(_cycle)));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
            _safe = true;
            boolean round = true;
            if (_cycle < Config.MPVP_MAXC) {
                announce(Static.MPVP_ROUND_ENDED.replaceAll("%r%", String.valueOf(_cycle)));
            } else {
                round = false;
                announce(Static.MPVP_FINAL_ENDED);
            }

            if (_curent.size() == 1) {
                L2PcInstance player = _curent.getFirst();
                if (player != null) {
                    anWinner(player, round);
                }
            } else {
                L2PcInstance wplayer = _curent.get(Rnd.get((_curent.size() - 1)));
                if (wplayer != null) {
                    _curent.remove(wplayer);
                    anWinner(wplayer, round);
                }
                /*
                 * for (int i = 0, n = _curent.size(); i < n; i++) {
                 * L2PcInstance player = _curent.get(i);
                 */
                for (FastList.Node<L2PcInstance> n = _curent.head(), end = _curent.tail(); (n = n.getNext()) != end;) {
                    L2PcInstance player = n.getValue();
                    if (player == null) {
                        _curent.remove(player);
                        continue;
                    }
                    _curent.remove(player);
                    _next.add(player);

                    player.setChannel(1);
                    player.setTeam(0);
                    player.setCurrentCp(player.getMaxCp());
                    player.setCurrentHp(player.getMaxHp());
                    player.setCurrentMp(player.getMaxMp());
                    player.setPVPArena(false);
                    player.sendPacket(SystemMessage.id(SystemMessageId.LEFT_COMBAT_ZONE));
                    Location _tpLoc = getRandomTpLoc();
                    player.teleToLocationEvent(_tpLoc.x + Rnd.get(100), _tpLoc.y + Rnd.get(100), _tpLoc.z);
                }
            }

            _curent.clear();

            if (round) {
                _curent.addAll(_next);
                _next.clear();
                _cycle += 1;
                if (_curent.size() <= 1) {
                    ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(Config.MPVP_MAXC), 60000);
                    L2PcInstance player = _curent.get(0);
                    if (player != null) {
                        player.setChannel(1);
                        player.sendCritMessage("Не хватает участников для следующего раунда.");
                        player.sendCritMessage("Попытайте судьбу в следующем турнире.");
                    }
                    announce(Static.MPVP_GO_FINAL);
                } else {
                    ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(_cycle), 60000);
                }
            } else {
                _autoStart = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);

                _event.SetDBValue("massPvp", "nextStart", "" + (System.currentTimeMillis() + _nextTime));
                _active = false;
                _reg = false;
                _safe = true;
                _curent.clear();
                //_winners.clear();
                _next.clear();
                _curCycle = -1;
            }
        }
    }

    private void startEvent() {
        _active = true;
        _reg = true;

        announce(Static.MPVP_STARTED);
        announce(Static.MPVP_REG_FOR.replaceAll("%m%", String.valueOf((_regTime / 60000))));
        ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceTask(), _anTime);
        //_event.doSpawn(Config.MPVP_NPC, _npcLoc, 0);

        _curent = new FastList<L2PcInstance>();
        _next = new FastList<L2PcInstance>();
        _winners = new FastList<L2PcInstance>();
        _winners.clear();
        _ips.clear();

        if (Config.EVENT_REG_POPUP) {
            for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                if (player == null || player.getLevel() < Config.EVENT_REG_POPUPLVL) {
                    continue;
                }

                player.sendPacket(new ConfirmDlg(614, "Принять участие в ивенте -Масс ПВП-?", 107));
            }
        }

        _cycleStart = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(1), _regTime);
    }

    public void startScript() {
        if (_autoStart != null) {
            _autoStart.cancel(true);
        }

        startEvent();
    }

    public void stopScript(L2PcInstance player) {
        if (!_active || _autoStart != null) {
            player.sendHtmlMessage("MassPvP", "Не запущен.");
            return;
        }

        announce(Static.MPVP_ADMIN_CANCEL);

        for (FastList.Node<L2PcInstance> n = _curent.head(), end = _curent.tail(); (n = n.getNext()) != end;) {
            L2PcInstance gamer = n.getValue();
            if (gamer == null) {
                _curent.remove(gamer);
                continue;
            }

            if (gamer.isDead()) {
                gamer.doRevive();
            }

            gamer.setChannel(1);
            gamer.setTeam(0);
            gamer.setEventWait(false);
            gamer.setCurrentCp(gamer.getMaxCp());
            gamer.setCurrentHp(gamer.getMaxHp());
            gamer.setCurrentMp(gamer.getMaxMp());
            gamer.setInsideZone(L2Character.ZONE_PVP, false);
            gamer.sendPacket(SystemMessage.id(SystemMessageId.LEFT_COMBAT_ZONE));
            Location _tpLoc = getRandomTpLoc();
            gamer.teleToLocationEvent(_tpLoc.x + Rnd.get(300), _tpLoc.y + Rnd.get(300), _tpLoc.z);
            onExit(gamer);
        }

        _autoStart = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);

        _event.SetDBValue("massPvp", "nextStart", "" + (System.currentTimeMillis() + _nextTime));
        _active = false;
        _reg = false;
        _safe = true;
        _curent.clear();
        _winners.clear();
        _next.clear();
        _ips.clear();
        _curCycle = -1;
    }

    private void announce(String text) {
        Announcements.getInstance().announceToAll(text);
    }
    private long _lastt = 0;

    private synchronized void anWinner(L2PcInstance player, boolean cycle) {
        if (!_active) {
            return;
        }

        if (System.currentTimeMillis() - _lastt < 11000) {
            if (cycle && _curent.contains(player)) {
                _curent.remove(player);
                _next.add(player);
                return;
            }

            if (!cycle && _winners.contains(player)) {
                _winners.remove(player);
                return;
            }
            return;
        }
        _lastt = System.currentTimeMillis();

        if (cycle) {
            _winners.add(player);
            announce(Static.MPVP_ROUND_WINNER.replaceAll("%player%", player.getName()));
            player.giveItem(Config.MPVP_CREW, Config.MPVP_CREWC);
            player.sendCritMessage("Оставайтесь в игре и ждите завершения всех раундов.");
            player.sendCritMessage("Вас ждет финальная битва!");
        } else {
            announce(Static.MPVP_ENDED);
            announce(Static.MPVP_WINNER_IS.replaceAll("%player%", player.getName()));
            announce(Static.MPVP_GRATS_WINNER);
            player.giveItem(Config.MPVP_EREW, Config.MPVP_EREWC);
            _winName = player.getName();
            _active = false;
            _curCycle = -1;
        }

        player.setChannel(1);
        player.setTeam(0);
        player.setCurrentCp(player.getMaxCp());
        player.setCurrentHp(player.getMaxHp());
        player.setCurrentMp(player.getMaxMp());
        player.setPVPArena(false);
        player.sendPacket(SystemMessage.id(SystemMessageId.LEFT_COMBAT_ZONE));
        Location _tpLoc = getRandomTpLoc();
        player.teleToLocationEvent(_tpLoc.x + Rnd.get(100), _tpLoc.y + Rnd.get(100), _tpLoc.z);
    }

    private boolean foundIp(String ip) {
        return (_ips.contains(ip));
    }

    public void regPlayer(L2PcInstance player) {
        if (!_active) {
            player.sendHtmlMessage("Масс ПВП", "Не запущен!");
            return;
        }
        if (isReg(player)) {
            player.sendHtmlMessage("Масс ПВП", "Вы уже зарегистрированы!");
            return;
        }
        if (!_reg) {
            player.sendHtmlMessage("Масс ПВП", "Регистрация окончена!");
            return;
        }
        if (_curent.size() > Config.MPVP_MAXP) {
            player.sendHtmlMessage("Масс ПВП", "Достигнут предел участников: " + Config.MPVP_MAXP);
            return;
        }
        if (Config.MPVP_NOBL && !player.isNoble()) {
            player.sendHtmlMessage("Масс ПВП", "Только для ноблессов");
            return;
        }
        if (player.getKarma() > 0 || player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("У вас плохая карма.");
            return;
        }
        if (player.getLevel() < Config.MPVP_LVL) {
            player.sendHtmlMessage("Масс ПВП", "Минимальный уровень для участия: " + Config.MPVP_LVL);
            return;
        }
        if (Config.ELH_ENABLE && LastHero.getEvent().isRegged(player)) {
            player.sendHtmlMessage("Масс ПВП", "Вы уже зарегистрированы в евенте -Последний герой-");
            return;
        }
        if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player)) {
            player.sendHtmlMessage("Масс ПВП", "Удачи на евенте -Захват базы-");
            return;
        }
        if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode()) {
            player.sendHtmlMessage("Масс ПВП", "Удачи на олимпе");
            return;
        } else if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(player.getName())) {
            player.sendHtmlMessage("Масс ПВП", "Удачи на твт");
            return;
        }
        if (!Config.EVENTS_SAME_IP && foundIp(player.getIP())) {
            player.sendHtmlMessage("-Масс ПВП-", "С вашего IP уже есть игрок.");
            return;
        }
        player.sendHtmlMessage("Масс ПВП", "Вы зарегистрировались на -Масс ПВП-");
        _curent.add(player);
        if (!Config.EVENTS_SAME_IP) {
            _ips.add(player.getIP());
        }
    }

    public boolean isReg(L2PcInstance player) {
        if (!_active) {
            return false;
        }

        if (_curent.contains(player) || _next.contains(player) || _winners.contains(player)) {
            return true;
        }

        return false;
    }

    public boolean isRegAndBattle(L2PcInstance player) {
        if (!_active) {
            return false;
        }

        if (!_safe && _curent.contains(player)) {
            return true;
        }

        return false;
    }

    public boolean isOpenReg() {
        if (!_active) {
            return false;
        }

        return _reg;
    }

    public boolean isActive() {
        return _active;
    }

    public void doDie(L2PcInstance player, L2Character killer) {
        //if (!_active)
        //return; 

        //if (player == null)// || killer == null || (!(killer.isPlayer()) && !(killer instanceof L2PetInstance) && !(killer instanceof L2SummonInstance)) || !_active)
        //	return;

        if (_reg) {
            return;
        }

        if (Config.MASSPVP_KILL_REWARD) {
            EventManager.getInstance().giveEventKillReward(killer.getPlayer(), null, Config.MASSPVP_KILLSITEMS);
        }

        if (_curCycle == Config.MPVP_MAXC) {
            player.sendCritMessage("Вы проиграли...");
        } else {
            player.sendCritMessage("Попытайте судьбу в следующем раунде!");
        }

        if (_curent.contains(player)) {
            _curent.remove(player);
            _next.add(player);
        }

        Location _tpLoc = getRandomTpLoc();
        try {
            player.teleToLocationEvent(_tpLoc.x + Rnd.get(100), _tpLoc.y + Rnd.get(100), _tpLoc.z);
        } catch (Exception e) {
        }

        player.setChannel(1);
        player.doRevive();
        player.setTeam(0);
        player.setCurrentHp(player.getMaxHp());
        player.setCurrentMp(player.getMaxMp());
        player.setCurrentCp(player.getMaxCp());

        //player.broadcastStatusUpdate();
        //player.broadcastUserInfo();
    }

    public void onExit(L2PcInstance player) {
        if (!_active) {
            return;
        }

        if (_curent.contains(player)) {
            _curent.remove(player);
        }

        if (_next.contains(player)) {
            _next.remove(player);
        }

        if (!Config.EVENTS_SAME_IP) {
            _ips.remove(player.getIP());
        }

        player.setChannel(1);
    }

    public int getRound() {
        return _curCycle;
    }

    public FastList<L2PcInstance> getWinners() {
        return _winners;
    }

    public String getWinner() {
        return _winName;
    }

    private Location getRandomTpLoc() {
        return Config.MPVP_TPLOC.get(Rnd.get(Config.MPVP_TPLOC.size() - 1));
    }

    private Location getRandomClLoc() {
        return Config.MPVP_CLOC.get(Rnd.get(Config.MPVP_CLOC.size() - 1));
    }

    private Location getRandomWinLoc() {
        return Config.MPVP_CLOC.get(Rnd.get(Config.MPVP_CLOC.size() - 1));
    }
}
