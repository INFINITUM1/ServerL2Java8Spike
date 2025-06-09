package net.sf.l2j.gameserver.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Log;

public class QueuedItems {

    private static final Logger _log = Logger.getLogger(QueuedItems.class.getName());
    private static QueuedItems _instance;

    public static QueuedItems getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new QueuedItems();
        _instance.load();
    }

    public QueuedItems() {
    }

    private void load() {
        ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask(), Config.QUED_ITEMS_INTERVAL);
        _log.fine("QueuedItems: task started every " + Config.QUED_ITEMS_INTERVAL + " msc.");
    }

    class UpdateTask implements Runnable {

        UpdateTask() {
        }

        public void run() {
            checkTable();
        }
    }

    private void checkTable() {
        new Thread(new Runnable() {

            public void run() {
                Connect con = null;
                PreparedStatement st = null;
                PreparedStatement st2 = null;
                ResultSet rs = null;
                try {
                    con = L2DatabaseFactory.get();
                    con.setTransactionIsolation(1);

                    int id = 0;
                    int char_id = 0;
                    int item_id = 0;
                    int item_count = 0;
                    int item_ench = 0;
                    int aug_id = 0;
                    int aug_lvl = 0;
                    String name = "";

                    L2PcInstance player = null;
                    L2ItemInstance reward = null;
                    ItemTable it = ItemTable.getInstance();

                    st = con.prepareStatement("SELECT id, char_id, name, item_id, item_count, item_ench, aug_id, aug_lvl FROM `z_queued_items` WHERE `status` = ?");
                    st.setInt(1, 0);
                    rs = st.executeQuery();
                    rs.setFetchSize(50);
                    while (rs.next()) {
                        char_id = rs.getInt("char_id");
                        item_id = rs.getInt("item_id");
                        if (char_id == 0 || item_id == 0) {
                            continue;
                        }

                        if (it.getTemplate(item_id) == null) {
                            continue;
                        }

                        id = rs.getInt("id");
                        name = rs.getString("name");
                        item_count = rs.getInt("item_count");
                        item_ench = rs.getInt("item_ench");
                        aug_id = rs.getInt("aug_id");
                        aug_lvl = rs.getInt("aug_lvl");

                        player = L2World.getInstance().getPlayer(char_id);
                        if (player == null) {
                            if (GiveItem.insertOffline(con, char_id, item_id, item_count, item_ench, aug_id, aug_lvl, "INVENTORY")) {
                                logAdd(getTime() + " player: " + name + "(" + char_id + "), item: " + item_id + "(" + item_count + ")(+" + item_ench + ")");
                            }
                        } else {
                            reward = player.getInventory().addItem("auc1", item_id, item_count, player, player.getTarget());
                            if (reward == null) {
                                continue;
                            }

                            if (item_ench > 0 && item_count == 1) {
                                reward.setEnchantLevel(item_ench);
                            }
                            //if (aug_id > 0)
                            //	reward.setAugmentation(new L2Augmentation(reward, aug_hex, aug_id, aug_lvl, true));

                            player.sendItems(true);
                            player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(item_id));
                            logAdd(getTime() + " player: " + name + "(" + char_id + "), item: " + item_id + "(" + item_count + ")(+" + item_ench + ")");
                        }

                        st2 = con.prepareStatement("UPDATE `z_queued_items` SET `status`=? WHERE `id`=?");
                        st2.setInt(1, 1);
                        st2.setInt(2, id);
                        st2.execute();
                        Close.S(st2);

                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                        }
                    }
                    player = null;
                    reward = null;
                } catch (final SQLException e) {
                    _log.warning("[ERROR] QueuedItems: checkTable() error: " + e);
                } finally {
                    Close.CSR(con, st, rs);
                }
                ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask(), Config.QUED_ITEMS_INTERVAL);
            }
        }).start();
    }

    public void logAdd(String text) {
        if (Config.QUED_ITEMS_LOGTYPE > 0) {
            switch (Config.QUED_ITEMS_LOGTYPE) {
                case 1:
                    _log.info(text);
                    break;
                case 2:
                    Log.add(text, "queued_items");
                    break;
            }
        }
    }

    public static String getTime() {
        Date date = new Date();
        SimpleDateFormat datef = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SS, ");
        return datef.format(date).toString();
    }
}
