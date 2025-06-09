package net.sf.l2j.gameserver.instancemanager.bosses;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Party;
import scripts.ai.EvilSpirit;
import scripts.ai.Frintezza;
import scripts.ai.Halisha;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Rnd;

public class FrintezzaManager extends GrandBossManager {

    protected static final Logger _logF = Logger.getLogger(BaiumManager.class.getName());
    private static final int BOSS = 29045;
    private Frintezza _self = null;
    private Halisha _halisa = null;
    private boolean _enter = false;
    private static FrintezzaManager _instance;
    private static final int _hall_alarm_device = 18328;
    private static final int _hall_keeper_captain = 18329;
    private static final int _dark_choir_player = 18339;
    private static final int _dark_choir_captain = 18334;
    private static final int _undeadband_member_leader = 18335;
    private static Map<Integer, EvilSpirit> _pghosts = new ConcurrentHashMap<Integer, EvilSpirit>();
    private static ConcurrentLinkedQueue<L2MonsterInstance> _privates = new ConcurrentLinkedQueue<L2MonsterInstance>();
    private ConcurrentLinkedQueue<L2Party> _partyes = new ConcurrentLinkedQueue<L2Party>();
    private static int _killCount = 0;

    public static FrintezzaManager getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new FrintezzaManager();
        _instance.load();
    }

    public void load() {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT spawn_date, status FROM grandboss_data WHERE boss_id=?");
            st.setInt(1, BOSS);
            rs = st.executeQuery();
            if (rs.next()) {
                int status = rs.getInt("status");
                long respawn = rs.getLong("spawn_date");

                if (status > 1 || respawn == 0) {
                    status = 1;
                }

                if (respawn > 0) {
                    status = 0;
                }

                _status = new Status(status, respawn);
            }
        } catch (SQLException e) {
            _logF.warning("FrintezzaManager, failed to load: " + e);
            e.getMessage();
        } finally {
            Close.CSR(con, st, rs);
        }

        switch (_status.status) {
            case 0:
                String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(_status.respawn));
                _logF.info("Frintezza: dead; spawn date: " + date);
                break;
            case 1:
                _logF.info("Frintezza: live; farm delay: " + (Config.FRINTA_RESTART_DELAY / 60000) + "min.");
                break;
        }
        ThreadPoolManager.getInstance().scheduleGeneral(new ManageBoss(), Config.FRINTA_RESTART_DELAY);
    }

    static class ManageBoss implements Runnable {

        ManageBoss() {
        }

        public void run() {
            long delay = 0;
            if (_status.respawn > 0) {
                delay = _status.respawn - System.currentTimeMillis();
            }

            if (delay <= 0) {
                _instance.prepareBoss();
            } else {
                ThreadPoolManager.getInstance().scheduleGeneral(new PrepareBoss(), delay);
            }
            _status.wait = false;
        }
    }

    static class PrepareBoss implements Runnable {

        PrepareBoss() {
        }

        public void run() {
            _instance.prepareBoss();
        }
    }

    static class Status {

        public int status;
        public long respawn;
        public boolean wait = true;

        public Status(int status, long respawn) {
            this.status = status;
            this.respawn = respawn;
        }
    }
    private static Status _status;

    public int getStatus() {
        if (_status.wait) {
            return 0;
        }

        return _status.status;
    }

    public int getPartyCount() {
        return _partyes.size();
    }

    public void addParty(L2Party party) {
        _partyes.add(party);
    }

    public void respawnMob(L2MonsterInstance privat) {
        L2MonsterInstance privat_new = null;
        switch (privat.getNpcId()) {
            case _hall_keeper_captain:
                if (_killCount == 0) {
                    privat_new = (L2MonsterInstance) createOnePrivateEx(privat.getNpcId(), privat.getX(), privat.getY(), privat.getZ(), 0);
                }
                break;
            case _undeadband_member_leader:
                if (_killCount < 14) {
                    privat_new = (L2MonsterInstance) createOnePrivateEx(privat.getNpcId(), privat.getX(), privat.getY(), privat.getZ(), 0);
                }
                break;
        }
        if (privat_new != null) {
            privat_new.setRunning();
        }
    }

    public void doEvent(int event) {
        L2MonsterInstance privat = null;
        DoorTable dt = DoorTable.getInstance();
        switch (event) {
            case 1:
                privat = (L2MonsterInstance) createOnePrivateEx(_hall_alarm_device, 175100, -76870, -5104, 0);
                privat.setIsImobilised(true);
                _privates.add(privat);
                privat = (L2MonsterInstance) createOnePrivateEx(_hall_alarm_device, 175400, -76000, -5104, 0);
                privat.setIsImobilised(true);
                _privates.add(privat);
                privat = (L2MonsterInstance) createOnePrivateEx(_hall_alarm_device, 175030, -75175, -5104, 0);
                privat.setIsImobilised(true);
                _privates.add(privat);
                privat = (L2MonsterInstance) createOnePrivateEx(_hall_alarm_device, 174190, -74800, -5104, 0);
                privat.setIsImobilised(true);
                _privates.add(privat);
                privat = (L2MonsterInstance) createOnePrivateEx(_hall_alarm_device, 172870, -76000, -5104, 0);
                privat.setIsImobilised(true);
                _privates.add(privat);
                for (int i = 0; i <= 5; i++) {
                    privat = (L2MonsterInstance) createOnePrivateEx(_hall_keeper_captain, 173990 + Rnd.get(200), -75920 + Rnd.get(200), -5104, 0);
                    _privates.add(privat);
                }
                ThreadPoolManager.getInstance().scheduleGeneral(new TimerTask(2002), Config.FRINTA_UPDATE_LAIR);
                break;
            case 2:
                _killCount += 1;
                switch (_killCount) {
                    case 5:
                        dt.getDoor(25150051).openMe();
                        dt.getDoor(25150052).openMe();
                        dt.getDoor(25150053).openMe();
                        dt.getDoor(25150054).openMe();
                        dt.getDoor(25150055).openMe();
                        dt.getDoor(25150056).openMe();
                        dt.getDoor(25150057).openMe();
                        dt.getDoor(25150058).openMe();
                        dt.getDoor(25150042).openMe();
                        dt.getDoor(25150043).openMe();
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_player, 173940, -81600, -5120, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_player, 174040, -81700, -5120, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_player, 174140, -81800, -5120, 0);
                        _privates.add(privat);
                        break;
                    case 8:
                        dt.getDoor(25150061).openMe();
                        dt.getDoor(25150062).openMe();
                        dt.getDoor(25150063).openMe();
                        dt.getDoor(25150064).openMe();
                        dt.getDoor(25150065).openMe();
                        dt.getDoor(25150066).openMe();
                        dt.getDoor(25150067).openMe();
                        dt.getDoor(25150068).openMe();
                        dt.getDoor(25150069).openMe();
                        dt.getDoor(25150070).openMe();
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_captain, 173400, -81100, -5072, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_captain, 173260, -81700, -5072, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_captain, 173360, -82400, -5072, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_captain, 174720, -82530, -5072, 32768);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_captain, 174900, -81800, -5072, 32768);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_dark_choir_captain, 174750, -81150, -5072, 32768);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_undeadband_member_leader, 174000, -81830, -5120, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_undeadband_member_leader, 174000, -81830, -5120, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_undeadband_member_leader, 174000, -81830, -5120, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_undeadband_member_leader, 174000, -81830, -5120, 0);
                        _privates.add(privat);
                        privat = (L2MonsterInstance) createOnePrivateEx(_undeadband_member_leader, 174000, -81830, -5120, 0);
                        _privates.add(privat);
                        break;
                    case 14:
                        dt.getDoor(25150045).openMe();
                        dt.getDoor(25150046).openMe();
                        ThreadPoolManager.getInstance().scheduleGeneral(new TimerTask(1), Config.FRINTA_SPAWN_DELAY);
                        break;
                }
                break;
        }
    }

    private static class TimerTask implements Runnable {

        private int _taskId = 0;

        public TimerTask(final int taskId) {
            _taskId = taskId;
        }

        public void run() {
            switch (_taskId) {
                case 1:
                    FrintezzaManager.getInstance().spawnBoss();
                    break;
                case 2002:
                    FrintezzaManager.getInstance().deSpawnBoss();
                    break;
            }
        }
    }

    public void spawnBoss() {
        _self = (Frintezza) createOnePrivateEx(BOSS, 174239, -89805, -5020, 16000);
        _self.setRunning();
    }

    public void prepareBoss() {
        setState(1, 0);
    }

    private void deSpawnBoss() {
        if (_self == null) {
            if (getStatus() == 2) {
                _enter = false;
                setState(1, 0);
                getZone(174239, -89805, -5020).oustAllPlayers();
            }
            return;
        }

        if (_self.isDead()) {
            return;
        }

        if (getStatus() == 1 || getStatus() == 0) {
            return;
        }

        _self.sayString("Time is out! Raid failed and finished!", 1);
        getZone(174239, -89805, -5020).oustAllPlayers();

        setState(1, 0);
        _self.decayMe();
        _partyes.clear();
        if (_halisa != null) {
            _halisa.deleteMe();
        }
        _self.deleteMe();
        _self = null;
        closeAllDoors();
        deletePrivates();
    }

    public void setState(int status, long respawn) {
        _status.status = status;
        _status.respawn = respawn;

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE `grandboss_data` SET `status`=?, `spawn_date`=? WHERE `boss_id`=?");
            statement.setInt(1, status);
            statement.setLong(2, respawn);
            statement.setInt(3, BOSS);
            statement.executeUpdate();
        } catch (SQLException e) {
            _logF.warning("FrintezzaManager, could not set Frintezza status" + e);
            e.getMessage();
        } finally {
            Close.CS(con, statement);
        }

        switch (status) {
            case 0:
                ThreadPoolManager.getInstance().scheduleGeneral(new PrepareBoss(), (respawn - System.currentTimeMillis()));
                String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(respawn));
                _logF.info("FrintezzaManager, Frintezza status: 0; killed, respawn date: " + date);
                break;
            case 1:
                _enter = false;
                _logF.info("FrintezzaManager, Frintezza status: 1; ready for farm.");
                break;
            case 2:
                _logF.info("FrintezzaManager, Frintezza status: 2; under attack.");
                break;
        }
    }

    public void closeAllDoors() {
        DoorTable dt = DoorTable.getInstance();
        dt.getDoor(25150051).closeMe();
        dt.getDoor(25150052).closeMe();
        dt.getDoor(25150053).closeMe();
        dt.getDoor(25150054).closeMe();
        dt.getDoor(25150055).closeMe();
        dt.getDoor(25150056).closeMe();
        dt.getDoor(25150057).closeMe();
        dt.getDoor(25150058).closeMe();
        dt.getDoor(25150042).closeMe();
        dt.getDoor(25150043).closeMe();
        dt.getDoor(25150061).closeMe();
        dt.getDoor(25150062).closeMe();
        dt.getDoor(25150063).closeMe();
        dt.getDoor(25150064).closeMe();
        dt.getDoor(25150065).closeMe();
        dt.getDoor(25150066).closeMe();
        dt.getDoor(25150067).closeMe();
        dt.getDoor(25150068).closeMe();
        dt.getDoor(25150069).closeMe();
        dt.getDoor(25150070).closeMe();
        dt.getDoor(25150045).closeMe();
        dt.getDoor(25150046).closeMe();
    }

    private static void deletePrivates() {
        for (L2MonsterInstance mob : _privates) {
            if (mob == null) {
                continue;
            }
            mob.decayMe();
            mob.deleteMe();
        }
        _privates.clear();

        for (L2MonsterInstance mob : _pghosts.values()) {
            if (mob == null) {
                continue;
            }
            mob.decayMe();
            mob.deleteMe();
        }
        _pghosts.clear();
        _killCount = 0;
    }

    public void notifyDie() {
        if (_self == null) {
            return;
        }

        _self.decayMe();
        _self.deleteMe();
        _self = null;

        deletePrivates();
        createOnePrivateEx(29055, 174235, -88023, -5108, 0);
    }

    public void updateRespawn() {
        _self.deathAnim();
        int offset = Rnd.get((int) Config.FRINTA_MIN_RESPAWN, (int) Config.FRINTA_MAX_RESPAWN);
        //long offset = (Config.FRINTA_MIN_RESPAWN + Config.FRINTA_MAX_RESPAWN) / 2;
        setState(0, (System.currentTimeMillis() + offset));
    }

    public void setFrintezza(Frintezza frinta) {
        if (frinta == null) {
            return;
        }

        _self = frinta;
    }

    public void setHalisha(Halisha halisha) {
        if (halisha == null) {
            return;
        }

        _halisa = halisha;
    }

    public Halisha getHalisha() {
        return _halisa;
    }

    public void putGhost(EvilSpirit ghost) {
        _pghosts.put(ghost.getObjectId(), ghost);
    }

    public void removeGhost(EvilSpirit ghost) {
        _pghosts.remove(ghost.getObjectId());
    }

    public int getGhostsSize() {
        return _pghosts.size();
    }

    public void addEffectPlayers(int id, int level, int rnd) {
        for (L2Party party : _partyes) {
            if (party == null) {
                continue;
            }

            party.addEffect(id, level, rnd);
        }
    }
}