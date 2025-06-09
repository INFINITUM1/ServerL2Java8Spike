
package net.sf.l2j.gameserver.taskmanager.tasks;

import net.sf.l2j.gameserver.taskmanager.Task;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;
import net.sf.l2j.gameserver.taskmanager.TaskTypes;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.mysql.Connect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.L2World;

import java.util.logging.Logger;  // Добавляем логгер

public class TaskGiveDonations extends Task {

    private static final String NAME = "GiveDonations";

    // Инициализация логгера
    private static final Logger _log = Logger.getLogger(TaskGiveDonations.class.getName());

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void onTimeElapsed(ExecutedTask task) {
       // _log.info("GiveDonations task triggered!");
       // System.out.println("GiveDonations task triggered!");
        int rewardId;

        Connect con = null;
        PreparedStatement stSelect = null;
        PreparedStatement stUpdate = null;
        ResultSet rs = null;

        try {
            con = L2DatabaseFactory.get();

            String querySelect = "SELECT id, char_name, reward_id, reward_count FROM donations WHERE status = 0";
            stSelect = con.prepareStatement(querySelect);
            rs = stSelect.executeQuery();

            String queryUpdate = "UPDATE donations SET status = 1 WHERE id = ?";
            stUpdate = con.prepareStatement(queryUpdate);

            while (rs.next()) {
                int id = rs.getInt("id");
                String charName = rs.getString("char_name");
                rewardId = rs.getInt("reward_id");
                int rewardCount = rs.getInt("reward_count");
                
                L2PcInstance player = L2World.getInstance().getPlayer(charName);

                if (player != null) {
                    if (player.getInventory().validateCapacityByItemId(rewardId)) {
                        player.addItem("sweep", rewardId, rewardCount, null, true);
                        stUpdate.setInt(1, id);
                        stUpdate.executeUpdate();
                        _log.info("Given " + rewardCount + " itemId = " + rewardId +" to " + charName);  // Логирование
                    } else {
                        player.sendMessage("Освободите место в инвентаре для получения награды за донат");
                        _log.warning("Player " + charName + " does not have slots in inventory");  // Логирование
                    }
                } else {
                 //   _log.warning("Player " + charName + " is offline");  // Логирование
                }
            }
        } catch (Exception e) {
            _log.severe("Error occurred: " + e.getMessage());  // Логирование ошибки
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception e) {
                _log.severe("Error closing ResultSet: " + e.getMessage());  // Логирование ошибки
                e.printStackTrace();
            }

            try {
                if (stSelect != null) stSelect.close();
            } catch (Exception e) {
                _log.severe("Error closing PreparedStatement (stSelect): " + e.getMessage());  // Логирование ошибки
                e.printStackTrace();
            }

            try {
                if (stUpdate != null) stUpdate.close();
            } catch (Exception e) {
                _log.severe("Error closing PreparedStatement (stUpdate): " + e.getMessage());  // Логирование ошибки
                e.printStackTrace();
            }

            try {
                if (con != null) con.close();
            } catch (Exception e) {
                _log.severe("Error closing connection: " + e.getMessage());  // Логирование ошибки
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initializate() {
        super.initializate();
        // Устанавливаем интервал 30 секунд (30000 миллисекунд)
        TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "180000", "30000", "");
    }
}
