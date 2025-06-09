package net.sf.l2j.gameserver;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadDatabase;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.gameserverpackets.ServerStatus;
import net.sf.l2j.gameserver.network.serverpackets.RestartResponse;
import net.sf.l2j.gameserver.network.serverpackets.ServerClose;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.log.AbstractLogger;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.sf.l2j.Config;
import net.sf.l2j.util.TimeLogger;
import org.mmocore.network.nio.impl.SelectorThread;

public class AutoRestart {

    private static Logger _log = AbstractLogger.getLogger(AutoRestart.class.getName());
    private static AutoRestart _instance;
    private static long _restartTime = 0;

    public static AutoRestart getInstance() {
        return _instance;
    }

    public AutoRestart() {
    }

    public static void init() {
        _instance = new AutoRestart();
        _instance.prepare();
    }

    private void prepare() {
        ThreadPoolManager.getInstance().scheduleGeneral(new Timer(56), getNextRestart());
        _log.info(TimeLogger.getLogTime() + "Auto Restart: scheduled at " + Config.RESTART_HOUR + " hour. (" + (getNextRestart() / 60000) + " minutes remaining.)");
    }

    private static long getNextRestart() {
        Calendar tomorrow = new GregorianCalendar();
        tomorrow.add(Calendar.DATE, 1);
        Calendar result = new GregorianCalendar(
                tomorrow.get(Calendar.YEAR),
                tomorrow.get(Calendar.MONTH),
                tomorrow.get(Calendar.DATE),
                Config.RESTART_HOUR,
                0);
        _restartTime = result.getTimeInMillis();
        return (_restartTime - System.currentTimeMillis());
    }

    public long remain() {
        return (_restartTime - System.currentTimeMillis());
    }

    static class Timer implements Runnable {

        private int step;

        Timer(int step) {
            this.step = step;
        }

        public void run() {
            Announcements _an = Announcements.getInstance();
            switch (step) {
                case 56:
                case 46:
                case 36:
                case 26:
                    String count = String.valueOf(step).substring(0, 1).trim();
                    _an.announceToAll("Уважаемый игрок!");
                    _an.announceToAll("Автоматический рестарт сервера через " + count + " минут!");
                    ThreadPoolManager.getInstance().scheduleGeneral(new Timer((step - 10)), 60000);
                    _log.info(TimeLogger.getLogTime() + "Auto Restart: " + count + " minutes remaining.");
                    break;
                case 16:
                    _an.announceToAll("Уважаемый игрок!");
                    _an.announceToAll("Автоматический рестарт сервера через 1 минуту!");
                    _log.info(TimeLogger.getLogTime() + "Auto Restart: 1 minute remaining.");
                    ThreadPoolManager.getInstance().scheduleGeneral(new Timer(3), 30000);
                    break;
                case 3:
                case 2:
                case 1:
                    _an.announceToAll("Уважаемый игрок!");
                    _an.announceToAll("Автоматический рестарт сервера через " + (step * 10) + " секунд!");
                    //if (step == 10)
                    //	disconnectAllCharacters();
                    _log.info(TimeLogger.getLogTime() + "Auto Restart: " + (step * 10) + " seconds remaining.");
                    Broadcast.toAllOnlinePlayers(SystemMessage.id(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS).addNumber((step * 10)));
                    ThreadPoolManager.getInstance().scheduleGeneral(new Timer((step - 1)), 10000);
                    break;
                case 0:
                    System.err.println(" ");
                    System.err.println(" ");
                    System.err.println("#########################################################################");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    System.err.println("#Auto Restart: All players disconnected.");
                    //startRestart();
                    Shutdown.getInstance().startRestart();
                    break;
            }
        }
    }

    protected static void startRestart() {
        // ensure all services are stopped
        try {
            GameTimeController.getInstance().stopTimer();
        } catch (Throwable t) {
            // ignore
        }

        // stop all threadpolls
        try {
            ThreadPoolManager.getInstance().shutdown();
        } catch (Throwable t) {
            // ignore
        }

        // last byebye, save all data and quit this server
        // logging doesnt work here :(
        saveData();

        try {
            LoginServerThread.getInstance().interrupt();
        } catch (Throwable t) {
            // ignore
        }

        // saveData sends messages to exit players, so sgutdown selector after it
        try {
            if (GameServer.getInstance() != null) {
                for (SelectorThread<L2GameClient> st : GameServer.getInstance().getSelectorThreads()) {
                    try {
                        st.shutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }

        // commit data, last chance
        try {
            L2DatabaseFactory.shutdown();
        } catch (Throwable t) {
        }

        // server will quit, when this function ends.
        Runtime.getRuntime().halt(2);
    }

    private static void saveData() {
        // Seven Signs data is now saved along with Festival data.
        if (!SevenSigns.getInstance().isSealValidationPeriod()) {
            SevenSignsFestival.getInstance().saveFestivalData(false);
            System.err.println("#Auto Restart: Seven Signs Festival, saved.");
        }

        // Save Seven Signs data before closing. :)
        SevenSigns.getInstance().saveSevenSignsData(null, true);
        System.err.println("#Auto Restart: Seven Sings, saved.");

        // Save all raidboss status ^_^
        RaidBossSpawnManager.getInstance().cleanUp();
        System.err.println("#Auto Restart: Raid Boss Spawn, saved.");
        GrandBossManager.getInstance().cleanUp();
        System.err.println("#Auto Restart: Grand Boss Spawn, saved.");
        TradeController.getInstance().dataCountStore();
        System.err.println("#Auto Restart: Trade Controller, saved.");

        try {
            OlympiadDatabase.save();
            System.err.println("#Auto Restart: Olympiad, saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Save Cursed Weapons data before closing.
        // Save Cursed Weapons data before closing.
        CursedWeaponsManager.getInstance().saveData();
        System.err.println("#Auto Restart: Cursed Weapons, saved.");

        // Save all manor data
        CastleManorManager.getInstance().save();
        System.err.println("#Auto Restart: Castle Manor Manager, saved.");

        // Save all global (non-player specific) Quest data that needs to persist after reboot
        QuestManager.getInstance().save();

        System.err.println("#Auto Restart: Data saved. Starting Up Server...");
        System.err.println("#########################################################################");
        System.err.println(" ");
        System.err.println(" ");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            //never happens :p
        }
    }

    /**
     * this disconnects all clients from the server
     *
     */
    private static void disconnectAllCharacters() {
        for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
            //Logout Character
            try {
                L2GameClient.saveCharToDisk(player);
                if (player.isInOfflineMode()) {
                    player.logout();
                } else {
                    player.sendPacket(new RestartResponse());
                    player.sendPacket(new ServerClose());
                }
            } catch (Throwable t) {
            }
        }

        try {
            Thread.sleep(1000);
        } catch (Throwable t) {
            _log.log(Level.INFO, "", t);
        }


        /*for (L2PcInstance player : L2World.getInstance().getAllPlayers())
         {
         try 
         {
         player.closeNetConnection();
         } 
         catch (Throwable t)	
         {
         // just to make sure we try to kill the connection 
         }				
         }*/
    }
}
