package net.sf.l2j.gameserver.instancemanager.bosses;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;
import scripts.ai.Zaken;

public class ZakenManager extends GrandBossManager {

    private static final Logger _log = AbstractLogger.getLogger(ZakenManager.class.getName());
    private static final int BOSS = 29022;
    private Zaken self = null;
    private boolean _enter = false;
    private static ZakenManager _instance;

    public static final ZakenManager getInstance() {
        return _instance;
    }

    static class Status {

        public int status;
        public long respawn;
        public boolean wait = true;
        public boolean spawned = false;

        public Status(int status, long respawn) {
            this.status = status;
            this.respawn = respawn;
        }
    }
    private static Status _status;

    class ManageBoss implements Runnable {

        ManageBoss() {
        }

        public void run() {
            long delay = 0;
            if (_status.respawn > 0) {
                delay = _status.respawn - System.currentTimeMillis();
            }

            if (delay <= 0) {
                spawnBoss();
            } else {
                ThreadPoolManager.getInstance().scheduleGeneral(new SpawnBoss(), delay);
            }
            _status.wait = false;
        }
    }

    class SpawnBoss implements Runnable {

        SpawnBoss() {
        }

        public void run() {
            spawnBoss();
        }
    }

    public static void init() {
        _instance = new ZakenManager();
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

                if (status > 1) {
                    status = 1;
                }

                if (respawn > 0) {
                    status = 0;
                }

                _status = new Status(status, respawn);
            }
        } catch (SQLException e) {
            _log.warning("ZakenManager, failed to load: " + e);
            e.getMessage();
        } finally {
            Close.CSR(con, st, rs);
        }

        switch (_status.status) {
            case 0:
                String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(_status.respawn));
                _log.info("Zaken: dead; spawn date: " + date);
                break;
            case 1:
                _log.info("Zaken: live; farm delay: " + (Config.ZAKEN_RESTART_DELAY / 60000) + "min.");
                break;
        }
        ThreadPoolManager.getInstance().scheduleGeneral(new ManageBoss(), Config.ZAKEN_RESTART_DELAY);
    }

    public void spawnBoss() {
        setState(1, 0);
        self = (Zaken) createOnePrivateEx(BOSS, Config.ZAKEN_SPAWN_LOC.x, Config.ZAKEN_SPAWN_LOC.y, Config.ZAKEN_SPAWN_LOC.z, 30000);
        self.setRunning();
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
            _log.warning("ZakenManager, could not set Zaken status" + e);
            e.getMessage();
        } finally {
            Close.CS(con, statement);
        }

        switch (status) {
            case 0:
                ThreadPoolManager.getInstance().scheduleGeneral(new SpawnBoss(), (respawn - System.currentTimeMillis()));
                String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(respawn));
                _log.info("ZakenManager, Zaken status: 0; killed, respawn date: " + date);
                break;
            case 1:
                _log.info("ZakenManager, Zaken status: 1; spawned.");
                if (Config.ANNOUNCE_EPIC_STATES) {
                    EventManager.getInstance().announce(Static.ZAKEN_SPAWNED);
                }
                break;
        }
    }

    public int getStatus() {
        if (_status.wait) {
            return 0;
        }

        return _status.status;
    }

    public boolean spawned() {
        return _status.spawned;
    }

    public void setSpawned() {
        _status.spawned = true;
        GrandBossManager.getInstance().getZone(55242, 219131, -3251).oustAllPlayers();
    }

    public void notifyDie() {
        if (self == null) {
            return;
        }

        self = null;
        _status.spawned = false;

        long offset = (Config.ZAKEN_MIN_RESPAWN + Config.ZAKEN_MAX_RESPAWN) / 2;
        setState(0, (System.currentTimeMillis() + offset));

        if (Config.ANNOUNCE_EPIC_STATES) {
            EventManager.getInstance().announce(Static.ZAKEN_DIED);
        }
    }
}