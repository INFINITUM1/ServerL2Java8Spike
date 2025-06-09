/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.instancemanager;

import java.net.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.Properties;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.bosses.*;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.TimeLogger;
import scripts.ai.QueenAnt;
import scripts.zone.type.L2BossZone;

/**
 *
 * @author DaRkRaGe Revised by Emperorc
 */
public class GrandBossManager {

    protected static final Logger _log = AbstractLogger.getLogger(GrandBossManager.class.getName());
    /*
     * ========================================================= This class
     * handles all Grand Bosses: <ul> <li>22215-22217 Tyrannosaurus</li>
     * <li>25333-25338 Anakazel</li> <li>29001 Queen Ant</li> <li>29006
     * Core</li> <li>29014 Orfen</li> <li>29019 Antharas</li> <li>29020
     * Baium</li> <li>29022 Zaken</li> <li>29028 Valakas</li> <li>29045
     * Frintezza</li> <li>29046-29047 Scarlet van Halisha</li> </ul>
     *
     * It handles the saving of hp, mp, location, and status of all Grand
     * Bosses. It also manages the zones associated with the Grand Bosses. NOTE:
     * The current version does NOT spawn the Grand Bosses, it just stores and
     * retrieves the values on reboot/startup, for AI scripts to utilize as
     * needed.
     */
    /**
     * DELETE FROM grandboss_list
     */
    private static final String DELETE_GRAND_BOSS_LIST = "DELETE FROM grandboss_list";
    /**
     * INSERT INTO grandboss_list (player_id,zone) VALUES (?,?)
     */
    private static final String INSERT_GRAND_BOSS_LIST = "INSERT INTO grandboss_list (player_id,zone) VALUES (?,?)";
    private static GrandBossManager _instance;
    protected static FastMap<Integer, BossInfo> _bosses = new FastMap<Integer, BossInfo>();
    private FastList<L2BossZone> _zones = new FastList<L2BossZone>();

    public static class BossInfo {

        public int status;
        public long respawn;

        public BossInfo(int status, long respawn) {
            this.status = status;
            this.respawn = respawn;
        }
    }

    public static GrandBossManager getInstance() {
        return _instance;
    }

    public GrandBossManager() {
    }

    public static void init() {
        _instance = new GrandBossManager();
        _instance.load();
    }

    private void load() {
        _zones = new FastList<L2BossZone>();
        loadBosses();
    }

    public void loadBosses() {
        _bosses.clear();
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT boss_id, spawn_date, status FROM grandboss_data ORDER BY boss_id");
            rs = st.executeQuery();
            rs.setFetchSize(5);
            while (rs.next()) {
                if (isEpic(rs.getInt("boss_id"))) {
                    continue;
                }

                _bosses.put(rs.getInt("boss_id"), new BossInfo(0, 0));
                manageBoss(rs.getInt("boss_id"), rs.getInt("status"), rs.getLong("spawn_date"));
            }
        } catch (SQLException e) {
            _log.warning("GrandBossManager: Could not load grandboss_data table");
            e.getMessage();
        } finally {
            Close.CSR(con, st, rs);
        }
        _log.info("GrandBossManager: loaded " + _bosses.size() + " Grand Bosses");
        /*
         * try { updatePlayersInZones(); } catch(Exception ignored) { // }
         */
    }

    private boolean isEpic(int id) {
        switch (id) {
            case 29001:
            case 29019:
            case 29028:
            case 29020:
            case 29045:
            case 29022:
                return true;
            default:
                return false;
        }
    }

    private void manageBoss(int boss, int status, long respawn) {
        long new_respawn = 0;
        boolean spawn = false;

        long temp = respawn - System.currentTimeMillis();
        if (temp > 0) {
            new_respawn = respawn;
        } else {
            spawn = true;
            new_respawn = 0;
        }

        if (spawn) {
            switch (boss) {
                case 29045:
                    temp = 90000;//Config.FRINTEZZA_RESTART_DELAY;
                    break;
            }
        }
        setStatus(boss, 0);
        _bosses.get(boss).respawn = new_respawn;
        ThreadPoolManager.getInstance().scheduleGeneral(new RespawnBoss(boss), temp);
        //System.out.println("##BOSS " + boss + " ##RESPAWN AFTER(ms): " + temp);
    }

    class RespawnBoss implements Runnable {

        public int boss;

        RespawnBoss(int boss) {
            this.boss = boss;
        }

        public void run() {
            long temp = _bosses.get(boss).respawn - System.currentTimeMillis();
            if (temp > 0) {
                manageBoss(boss, 0, _bosses.get(boss).respawn);
                return;
            }
            setStatus(boss, 1);
            setRespawn(boss, 0);
            switch (boss) {
                case 29045:
                    _log.info(TimeLogger.getTime() + "GrandBossManager: Frintezza, ready for farm.");
                    if (Config.ANNOUNCE_EPIC_STATES) {
                        EventManager.getInstance().announce(Static.FRINTEZZA_SPAWNED);
                    }
                    break;
            }
        }
    }

    /*
     * Zone Functions
     */
    public void initZones() {
        FastMap<Integer, FastList<Integer>> zones = new FastMap<Integer, FastList<Integer>>();
        if (_zones == null) {
            _log.warning("GrandBossManager: Could not read Grand Boss zone data");
            return;
        }

        for (L2BossZone zone : _zones) {
            if (zone == null) {
                continue;
            }
            zones.put(zone.getId(), new FastList<Integer>());
        }

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT * from grandboss_list ORDER BY player_id");
            rset = statement.executeQuery();
            rset.setFetchSize(50);
            while (rset.next()) {
                int id = rset.getInt("player_id");
                int zone_id = rset.getInt("zone");
                zones.get(zone_id).add(id);
            }
        } catch (SQLException e) {
            _log.warning("GrandBossManager: Could not load grandboss_list table");
            e.getMessage();
        } finally {
            Close.CSR(con, statement, rset);
        }
        _log.info("GrandBossManager: Loaded " + _zones.size() + " Grand Boss Zones");

        /*
         * for (L2BossZone zone : _zones) { if (zone == null) continue;
         * zone.setAllowedPlayers(zones.get(zone.getId())); }
         */
        zones.clear();
    }

    public void addZone(L2BossZone zone) {
        if (_zones != null) {
            _zones.add(zone);
        }
    }

    public final L2BossZone getZone(L2Character character) {
        if (_zones != null) {
            for (L2BossZone temp : _zones) {
                if (temp.isCharacterInZone(character)) {
                    return temp;
                }
            }
        }
        return null;
    }

    public final L2BossZone getZone(int x, int y, int z) {
        if (_zones != null) {
            for (L2BossZone temp : _zones) {
                if (temp.isInsideZone(x, y, z)) {
                    return temp;
                }
            }
        }
        return null;
    }

    public boolean checkIfInZone(String zoneType, L2Object obj) {
        L2BossZone temp = getZone(obj.getX(), obj.getY(), obj.getZ());
        if (temp == null) {
            return false;
        }
        return temp.getZoneName().equalsIgnoreCase(zoneType);
    }

    public boolean checkIfInZone(L2PcInstance player) {
        if (player == null) {
            return false;
        }
        L2BossZone temp = getZone(player.getX(), player.getY(), player.getZ());
        if (temp == null) {
            return false;
        }
        return true;
    }
    /*
     * The rest
     */

    public BossInfo get(int bossId) {
        return _bosses.get(bossId);
    }

    public void setStatus(int bossId, int status) {
        _bosses.get(bossId).status = status;

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE `grandboss_data` SET `status`=? WHERE `boss_id`=?");
            statement.setInt(1, status);
            statement.setInt(2, bossId);
            statement.executeUpdate();
        } catch (Exception e) {
            _log.warning("L2GrandBossInstance: could not set " + bossId + " status" + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    public void setRespawn(int bossId, long nextTime) {
        _bosses.get(bossId).respawn = nextTime;

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE `grandboss_data` SET `spawn_date`=? WHERE `boss_id`=?");
            statement.setLong(1, nextTime);
            statement.setInt(2, bossId);
            statement.executeUpdate();
        } catch (Exception e) {
            _log.warning("L2GrandBossInstance: could not set " + bossId + " status" + e);
        } finally {
            Close.CS(con, statement);
        }
        if (nextTime > 0) {
            setStatus(bossId, 0);
            manageBoss(bossId, 0, nextTime);

            if (Config.ANNOUNCE_EPIC_STATES) {
                switch (bossId) {
                    case 29045:
                        EventManager.getInstance().announce(Static.FRINTEZZA_DIED);
                        break;
                }
            }
        }
    }

    /*
     * Adds a L2GrandBossInstance to the list of bosses.
     */
    private void storeToDb() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement(DELETE_GRAND_BOSS_LIST);
            statement.executeUpdate();
            Close.S(statement);

            con.setAutoCommit(false);
            /*
             * statement = con.prepareStatement(INSERT_GRAND_BOSS_LIST); for
             * (L2BossZone zone : _zones) { if (zone == null) continue; Integer
             * id = zone.getId(); FastList<Integer> list =
             * zone.getAllowedPlayers(); if (list == null || list.isEmpty())
             * continue; for (Integer player : list) { statement.setInt(1,
             * player); statement.setInt(2, id); statement.executeUpdate();
             * statement.addBatch(); } } statement.executeBatch();
             * Close.S(statement);
             */

            statement = con.prepareStatement("UPDATE `grandboss_data` SET `spawn_date`=?,`status`=? WHERE `boss_id`=?");
            for (FastMap.Entry<Integer, BossInfo> e = _bosses.head(), end = _bosses.tail(); (e = e.getNext()) != end;) {
                int boss = e.getKey();
                BossInfo bi = e.getValue();
                statement.setLong(1, bi.respawn);
                if (bi.respawn > 0) {
                    statement.setInt(2, 0);
                } else {
                    statement.setInt(2, 1);
                }
                statement.setInt(3, boss);
                //statement.executeUpdate();
                statement.addBatch();
                //Close.S(statement);
            }
            statement.executeBatch();
            con.commit();
        } catch (SQLException e) {
            _log.warning("GrandBossManager: Couldn't store grandbosses to database:" + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    /**
     * Saves all Grand Boss info and then clears all info from memory, including
     * all schedules.
     */
    public void cleanUp() {
        storeToDb();

        _bosses.clear();
        _zones.clear();
    }

    public FastList<L2BossZone> getZones() {
        return _zones;
    }

    /**
     * спавн
     */
    public int getDBValue(String name, String var) {
        int result = 0;
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
            statement.setString(1, name);
            statement.setString(2, var);
            rset = statement.executeQuery();
            if (rset.first()) {
                result = rset.getInt(1);
            }
        } catch (Exception e) {
            _log.warning("L2GrandBossInstance: could not load " + name + "; info" + e);
        } finally {
            Close.CSR(con, statement, rset);
        }
        return result;
    }

    public L2NpcInstance createOnePrivateEx(int npcId, int x, int y, int z, int heading) {
        return createOnePrivateEx(npcId, x, y, z, heading, false);
    }

    public L2NpcInstance createOnePrivateEx(int npcId, int x, int y, int z, int heading, boolean respawn) {
        L2NpcInstance result = null;
        try {
            L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
            L2Spawn spawn = new L2Spawn(template);
            spawn.setHeading(heading);
            spawn.setLocx(x);
            spawn.setLocy(y);
            spawn.setLocz(z + 20);
            if (respawn) {
                spawn.startRespawn();
            } else {
                spawn.stopRespawn();
            }
            result = spawn.spawnOne();
            return result;
        } catch (Exception e1) {
            _log.warning("L2GrandBossInstance: Could not spawn Npc " + npcId);
        }

        return null;
    }

    public L2Spawn createOneSpawnEx(int npcId, int x, int y, int z, int heading, boolean respawn) {
        try {
            L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
            L2Spawn spawn = new L2Spawn(template);
            spawn.setHeading(heading);
            spawn.setLocx(x);
            spawn.setLocy(y);
            spawn.setLocz(z + 20);
            if (respawn) {
                spawn.startRespawn();
            } else {
                spawn.stopRespawn();
            }
            return spawn;
        } catch (Exception e1) {
            _log.warning("L2GrandBossInstance: Could not spawn Npc " + npcId);
        }
        return null;
    }

    public L2NpcInstance createOnePrivateEx(int npcId, int x, int y, int z, int heading, int respawnTime) {
        L2NpcInstance result = null;
        try {
            L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
            L2Spawn spawn = new L2Spawn(template);
            spawn.setLocx(x);
            spawn.setLocy(y);
            spawn.setLocz(z + 20);
            spawn.setRespawnDelay(respawnTime);
            spawn.setHeading(heading);
            result = spawn.spawnOne();
            return result;
        } catch (Exception e1) {
            _log.warning("L2GrandBossInstance: Could not spawn Npc " + npcId);
        }

        return null;
    }
    //
    L2GrandBossInstance _frinta = null;
    L2GrandBossInstance _halisha = null;

    public void setFrintezza(L2GrandBossInstance boss) {
        if (boss != null) {
            _frinta = boss;
        }
    }

    public L2GrandBossInstance getFrintezza() {
        return _frinta;
    }

    public void setHalisha(L2GrandBossInstance boss) {
        if (boss != null) {
            _halisha = boss;
        }
    }

    public L2GrandBossInstance getHalisha() {
        return _halisha;
    }
    //aq
    QueenAnt _aq = null;

    public void setAQ(QueenAnt boss) {
        if (boss != null) {
            _aq = boss;
        }
    }

    public QueenAnt getAQ() {
        return _aq;
    }
    L2MonsterInstance _larva = null;

    public void setAQLarva(L2MonsterInstance larva) {
        if (larva != null) {
            _larva = larva;
        }
    }

    public L2MonsterInstance getAQLarva() {
        return _larva;
    }

    //
    public boolean getItem(L2PcInstance player, int itemId) {
        if (!(Config.NOEPIC_QUESTS)) {
            return true;
        }

        L2ItemInstance coin = player.getInventory().getItemByItemId(itemId);
        if (coin == null || coin.getCount() < 1) {
            return false;
        }

        if (!player.destroyItemByItemId("RaidBossTele", itemId, 1, player, true)) {
            return false;
        }

        return true;
    }

    public void loadManagers() {
        AntharasManager.init();
        BaiumManager.init();
        FrintezzaManager.init();
        QueenAntManager.init();
        ValakasManager.init();
        ZakenManager.init();
    }

    public static NpcHtmlMessage getHtmlRespawns(NpcHtmlMessage html) {
        if (!Config.SHOW_BOSS_RESPAWNS) {
            return html;
        }
        /*
         %status_antqueen%
         %status_zaken%
         %status_baium%
         %status_antharas%
         %status_valakas%
         %status_flyrb%
         */

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();

            String status = getGlobalData(con, "antqueen", "status");
            if (!status.isEmpty()) {
                html.replace("%status_antqueen%", parseRespawn(con, Integer.parseInt(status), "antqueen"));
            }

            status = getGlobalData(con, "zaken", "status");
            if (!status.isEmpty()) {
                html.replace("%status_zaken%", parseRespawn(con, Integer.parseInt(status), "zaken"));
            }

            status = getGlobalData(con, "baium", "status");
            if (!status.isEmpty()) {
                html.replace("%status_baium%", parseRespawn(con, Integer.parseInt(status), "baium"));
            }

            status = getGlobalData(con, "anthatas", "status");
            if (!status.isEmpty()) {
                html.replace("%status_antharas%", parseRespawn(con, Integer.parseInt(status), "anthatas"));
            }

            status = getGlobalData(con, "valakas", "status");
            if (!status.isEmpty()) {
                html.replace("%status_valakas%", parseRespawn(con, Integer.parseInt(status), "valakas"));
            }

            status = getGlobalData(con, "flyrb", "status");
            if (!status.isEmpty()) {
                html.replace("%status_flyrb%", parseRespawn(con, Integer.parseInt(status), "flyrb"));
            }
        } catch (final Exception e) {
            _log.warning("getHtmlRespawns() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }

        return html;
    }

    private static String getGlobalData(Connect con, String name, String var) {
        String result = "";
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
            st.setString(1, name);
            st.setString(2, var);
            rs = st.executeQuery();
            if (rs.first()) {
                result = rs.getString(1);
            }
        } catch (Exception e) {
            _log.warning("getGlobalData() error: " + e);
        } finally {
            Close.SR(st, rs);
        }
        return result;
    }

    private static String parseRespawn(Connect con, Integer status, String name) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0:
                String respawn = getGlobalData(con, name, "respawn");
                if (!respawn.isEmpty()) {
                    return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(Long.parseLong(respawn)));
                }
                break;
            case 1:
                return "Жив";
            default:
                return "Атакуют";
        }
        return "Жив";
    }
}
