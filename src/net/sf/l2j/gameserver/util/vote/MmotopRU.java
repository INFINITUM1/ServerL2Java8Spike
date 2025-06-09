package net.sf.l2j.gameserver.util.vote;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.GiveItem;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.log.AbstractLogger;

public class MmotopRU {

    private static final Logger _log = AbstractLogger.getLogger(MmotopRU.class.getName());
    private static FastList<EventReward> _rewards = Config.MMOTOP_ONLINE_REWARDS;
    private static FastList<String> _waiters = new FastList<String>();
    private static String _last = "";
    private static MmotopRU _instance;

    public static MmotopRU getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new MmotopRU();
        _instance.load();
    }

    public MmotopRU() {
    }

    private void load() {
        if (Config.MMOTOP_STAT_LINK.equals("")) {
            _log.warning("[ERROR] MmotopRU: load() MmotopStatLink is empty; in services.cfg");
            return;
        }
        _last = getLast();
        updateVoters();
    }

    class UpdateTask implements Runnable {

        UpdateTask() {
        }

        public void run() {
            updateVoters();
            //
            CustomServerData.getInstance().cacheStat();
        }
    }

    private void updateVoters() {
        new Thread(new Runnable() {

            public void run() {
                long start = System.currentTimeMillis();
                String pre_last = "";
                String f_last = "";
                //_log.info("MmotopRU: now#" + now);
                BufferedReader br = null;
                InputStreamReader isr = null;
                try {
                    URL l2top = new URL(Config.MMOTOP_STAT_LINK);
                    isr = new InputStreamReader(l2top.openStream());
                    br = new BufferedReader(isr);
                    int i = 0;
                    String line;
                    while ((line = br.readLine()) != null) {
                        i++;
                        String[] tmp = line.split("	");
                        pre_last = tmp[1];
                        if (pre_last.equals(_last)) {
                            break;
                        }

                        if (i == 1) {
                            f_last = pre_last;
                        }

                        String name = tmp[3];

                        if (name.isEmpty()) {
                            continue;
                        }

                        name = Util.checkServerVotePrefix(name);
                        if (name.isEmpty()) {
                            continue;
                        }

                        _waiters.add(name);
                        //System.out.println("##" + name + "##");
                    }
                } catch (Exception e) {
                    _log.warning("[ERROR] MmotopRU: updateVoters() error: " + e);
                } finally {
                    try {
                        if (isr != null) {
                            isr.close();
                            isr = null;
                        }
                        if (br != null) {
                            br.close();
                            br = null;
                        }
                    } catch (Exception e) {
                        _log.warning("[ERROR] MmotopRU: updateVoters()close() error: " + e);
                    }
                }
                if (f_last.length() > 0) {
                    _last = f_last;
                }
                setLast();

                if (Config.MMOTOP_LOGTYPE > 0) {
                    long time = (System.currentTimeMillis() - start) / 1000;
                    String result = getDate() + ", mmotop.ru: finished, +" + _waiters.size() + "; time: " + time + "s.";
                    switch (Config.MMOTOP_LOGTYPE) {
                        case 1:
                            _log.info(result);
                            break;
                        case 2:
                            Log.add(result, "vote_mmotop");
                            break;
                    }
                }
                giveRewards();
            }
        }).start();
    }

    private void giveRewards() {
        new Thread(new Runnable() {

            public void run() {
                String now = getDate();
                Connect con = null;
                PreparedStatement st = null;
                ResultSet rs = null;
                try {
                    L2PcInstance voter = null;
                    EventReward reward = null;
                    con = L2DatabaseFactory.get();
                    con.setTransactionIsolation(1);
                    for (FastList.Node<String> n = _waiters.head(), end = _waiters.tail(); (n = n.getNext()) != end;) {
                        String name = n.getValue();
                        if (name == null) {
                            continue;
                        }

                        int char_id = getCharId(con, name);
                        if (char_id == 0) {
                            continue;
                        }

                        voter = L2World.getInstance().getPlayer(char_id);
                        if (voter == null) // && Config.MMOTOP_OFFLINE
                        {
                            if (GiveItem.insertOffline(con, char_id, Config.MMOTOP_OFFLINE_ITEM, Config.MMOTOP_OFFLINE_COUNT, 0, 0, 0, Config.MMOTOP_OFFLINE_LOC)) {
                                logVote(con, name, now, char_id);
                            }
                        } else {
                            reward = null;
                            for (FastList.Node<EventReward> k = _rewards.head(), endk = _rewards.tail(); (k = k.getNext()) != endk;) {
                                reward = k.getValue();
                                if (reward == null) {
                                    continue;
                                }

                                if (Rnd.get(100) < reward.chance) {
                                    voter.addItem("MmotopRU.giveItem", reward.id, reward.count, voter, true);
                                }
                            }
                            reward = null;
                            logVote(con, name, now, char_id);
                        }

                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                        }
                    }
                    voter = null;
                    reward = null;
                } catch (final SQLException e) {
                    _log.warning("[ERROR] MmotopRU: giveRewards() error: " + e);
                } finally {
                    _waiters.clear();
                    Close.CSR(con, st, rs);
                }
                ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask(), Config.MMOTOP_UPDATE_DELAY);
            }
        }).start();
    }

    private void logVote(Connect con, String name, String date, int char_id) {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = con.prepareStatement("INSERT INTO `z_vote_logs` (`date`, `name`, `obj_Id`) VALUES (?, ?, ?)");
            st.setString(1, date);
            st.setString(2, name);
            st.setInt(3, char_id);
            st.execute();
        } catch (final SQLException e) {
            System.out.println("[ERROR] MmotopRU, logVote() error: " + e);
        } finally {
            Close.SR(st, rs);
        }
    }

    private int getCharId(Connect con, String name) {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            if (Config.VS_VREF) {
                st = con.prepareStatement("SELECT * FROM `z_vote_names` WHERE `from` = ? LIMIT 1");
                st.setString(1, name);
                rs = st.executeQuery();
                if (rs.next()) {
                    name = rs.getString("to");
                }

                Close.SR(st, rs);
            } else if (Config.VS_VREF_NAME) {
                String refName = Util.findRefName(name);
                if (!name.equalsIgnoreCase(refName)) {
                    st = con.prepareStatement("SELECT obj_Id FROM `characters` WHERE `char_name` = ? LIMIT 0,1");
                    st.setString(1, refName);
                    rs = st.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("obj_Id");
                    }
                    Close.SR(st, rs);
                }
            }

            st = con.prepareStatement("SELECT obj_Id FROM `characters` WHERE `char_name` = ? LIMIT 0,1");
            st.setString(1, name);
            rs = st.executeQuery();
            if (rs.next()) {
                return rs.getInt("obj_Id");
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] MmotopRU, getCharId() error: " + e);
        } finally {
            Close.SR(st, rs);
        }
        return 0;
    }

    private String getLast() {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            st = con.prepareStatement("SELECT date FROM `z_vote_logs` WHERE `id` = ?");
            st.setInt(1, -2);
            rs = st.executeQuery();
            if (rs.next()) {
                return rs.getString("date");
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] MmotopRU, getLast() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        return "";
    }

    private void setLast() {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("REPLACE INTO `z_vote_logs` (`id`, `date`, `name`) VALUES (?, ?, ?)");
            st.setInt(1, -2);
            st.setString(2, _last);
            st.setString(3, "l#a#s#t#m#m#o#t#o#p");
            st.execute();
        } catch (SQLException e) {
            _log.warning("setLast() error: " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    private static String getDate() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        return sdf.format(date);
    }
}
