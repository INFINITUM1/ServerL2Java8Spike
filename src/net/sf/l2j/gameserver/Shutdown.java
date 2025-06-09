/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.instancemanager.*;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadDatabase;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.gameserverpackets.ServerStatus;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.log.AbstractLogger;
import org.mmocore.network.nio.impl.SelectorThread;

/**
 *
 * This class provides the functions for shutting down and restarting the server
 * It closes all open clientconnections and saves all data.
 *
 * @version $Revision: 1.2.4.5 $ $Date: 2005/03/27 15:29:09 $
 */
public class Shutdown extends Thread {

    private static final Logger _log = AbstractLogger.getLogger(Shutdown.class.getName());
    private static Shutdown _instance;
    private static Shutdown _counterInstance = null;
    private int _secondsShut;
    private int _shutdownMode;
    public static final int SIGTERM = 0;
    public static final int GM_SHUTDOWN = 1;
    public static final int GM_RESTART = 2;
    public static final int ABORT = 3;
    public static final int AUTO_RESTART = 4;
    private static final String[] MODE_TEXT = {"Профилактическое выключение сервера!", "Выключение", "Рестарт", "Отмена", "Автоматический рестарт!"};
    private boolean _AbortShutdown = false;

    /**
     * This function starts a shutdown countdown from Telnet (Copied from
     * Function startShutdown())
     *
     * @param ip IP Which Issued shutdown command
     * @param seconds seconds untill shutdown
     * @param restart true if the server will restart after shutdown
     */
    public void startTelnetShutdown(String IP, int seconds, boolean restart) {
        Announcements _an = Announcements.getInstance();
        _log.warning("IP: " + IP + " issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");
        //_an.announceToAll("Server is " + _modeText[shutdownMode] + " in "+seconds+ " seconds!");

        if (restart) {
            _shutdownMode = GM_RESTART;
        } else {
            _shutdownMode = GM_SHUTDOWN;
        }

        if (_shutdownMode > 0) {
            _an.announceToAll("Внимание игроки!");
            _an.announceToAll(MODE_TEXT[_shutdownMode] + " сервера через " + seconds + " секунд!");
            if (_shutdownMode == 1 || _shutdownMode == 2) {
                _an.announceToAll("Пожалуйста, выйдите из игры.");
            }
        }

        if (_counterInstance != null) {
            _counterInstance._abort();
        }
        _counterInstance = new Shutdown(seconds, restart);
        _counterInstance.start();
    }

    /**
     * This function aborts a running countdown
     *
     * @param IP IP Which Issued shutdown command
     */
    public void telnetAbort(String IP) {
        Announcements _an = Announcements.getInstance();
        _log.warning("IP: " + IP + " issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");
        _an.announceToAll(MODE_TEXT[_shutdownMode] + ", сервер продолжает свою работу!");

        if (_counterInstance != null) {
            _counterInstance._abort();
        }
    }

    public void startRestart() {
        _shutdownMode = GM_RESTART;

        if (_counterInstance != null) {
            _counterInstance._abort();
        }

        _counterInstance = new Shutdown(45, true);
        _counterInstance.start();
    }

    /**
     * Default constucter is only used internal to create the shutdown-hook
     * instance
     *
     */
    public Shutdown() {
        _secondsShut = -1;
        _shutdownMode = SIGTERM;
    }

    /**
     * This creates a countdown instance of Shutdown.
     *
     * @param seconds	how many seconds until shutdown
     * @param restart	true is the server shall restart after shutdown
     *
     */
    public Shutdown(int seconds, boolean restart) {
        if (seconds < 0) {
            seconds = 0;
        }

        _secondsShut = seconds;

        if (restart) {
            _shutdownMode = GM_RESTART;
        } else {
            _shutdownMode = GM_SHUTDOWN;
        }
    }

    /**
     * get the shutdown-hook instance the shutdown-hook instance is created by
     * the first call of this function, but it has to be registrered externaly.
     *
     * @return	instance of Shutdown, to be used as shutdown hook
     */
    public static Shutdown getInstance() {
        if (_instance == null) {
            _instance = new Shutdown();
        }
        return _instance;
    }

    /**
     * this function is called, when a new thread starts
     *
     * if this thread is the thread of getInstance, then this is the shutdown
     * hook and we save all data and disconnect all clients.
     *
     * after this thread ends, the server will completely exit
     *
     * if this is not the thread of getInstance, then this is a countdown
     * thread. we start the countdown, and when we finished it, and it was not
     * aborted, we tell the shutdown-hook why we call exit, and then call exit
     *
     * when the exit status of the server is 1, startServer.sh / startServer.bat
     * will restart the server.
     *
     */
    @Override
    public void run() {
        // disallow new logins
        try {
            //Doesnt actually do anything
            //Server.gameServer.getLoginController().setMaxAllowedOnlinePlayers(0);
        } catch (Throwable t) {
            // ignore
        }

        if (this == _instance) {
            Announcements _an = Announcements.getInstance();
            switch (_shutdownMode) {
                case SIGTERM:
                    System.err.println("SIGTERM received. Shutting down after 60 sec!");
                    //Olympiad.getInstance().saveOlympiadStatus();
                    try {
                        L2World.getInstance().deleteVisibleNpcSpawns();
                        _an.announceToAll("Выключение сервера на профилактику через минуту!");
                        _an.announceToAll("Выйдите из игры, что-бы не потерять достигнутых результатов.");
                        _an.announceToAll("Приносим извинения за возможные неудобства.");
                        Broadcast.toAllOnlinePlayers(SystemMessage.id(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS).addNumber(60));
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                    }

                    try {
                        System.err.println("SIGTERM received. Shutting down after 30 sec!");
                        _an.announceToAll("Выключение сервера на профилактику через 30 секунд!");
                        _an.announceToAll("Выйдите из игры, что-бы не потерять достигнутых результатов.");
                        _an.announceToAll("Приносим извинения за возможные неудобства.");
                        Broadcast.toAllOnlinePlayers(SystemMessage.id(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS).addNumber(20));
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                    }

                    for (int i = 10; i > 0; i--) {
                        try {
                            System.err.println("SIGTERM received. Shutting down after " + i + " sec!");
                            _an.announceToAll("Выключение сервера на профилактику через " + i + " секунд.");
                            _an.announceToAll("Пожалуйста, выйдите из игры.");
                            if (i == 3) {
                                disconnectAllCharacters();
                            }
                            Broadcast.toAllOnlinePlayers(SystemMessage.id(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS).addNumber(i));
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    System.err.println("SIGTERM received. Shutting down now...");
                    break;
            }
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
                /*GameServer.gameServer.getSelectorThread().shutdown();
                 GameServer.gameServer.getSelectorThread().setDaemon(true);*/
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
            if (_instance._shutdownMode == GM_RESTART || _instance._shutdownMode == AUTO_RESTART) {
                Runtime.getRuntime().halt(2);
            } else {
                Runtime.getRuntime().halt(0);
            }
        } else {
            // gm shutdown: send warnings and then call exit to start shutdown sequence
            countdown();
            // last point where logging is operational :(
            _log.warning("GM shutdown countdown is over. " + MODE_TEXT[_shutdownMode] + " NOW!");
            switch (_shutdownMode) {
                case GM_SHUTDOWN:
                    _instance.setMode(GM_SHUTDOWN);
                    System.exit(0);
                    break;
                case GM_RESTART:
                    _instance.setMode(GM_RESTART);
                    System.exit(2);
                case AUTO_RESTART:
                    _instance.setMode(AUTO_RESTART);
                    System.exit(2);
                    break;
            }
        }
    }

    /**
     * This functions starts a shutdown countdown
     *
     * @param activeChar	GM who issued the shutdown command
     * @param seconds	seconds until shutdown
     * @param restart	true if the server will restart after shutdown
     */
    public void startShutdown(L2PcInstance activeChar, int seconds, boolean restart) {
        Announcements _an = Announcements.getInstance();
        _log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");

        if (restart) {
            _shutdownMode = GM_RESTART;
        } else {
            _shutdownMode = GM_SHUTDOWN;
        }

        /*
         * if(_shutdownMode > 0) { _an.announceToAll("Внимание игроки!");
         * _an.announceToAll(MODE_TEXT[_shutdownMode] + " сервера через
         * "+seconds+ " секунд!"); if(_shutdownMode == 1 || _shutdownMode == 2)
         * { _an.announceToAll("Пожалуйста, выйдите из игры."); }
         }
         */
        if (_counterInstance != null) {
            _counterInstance._abort();
        }

//		 the main instance should only run for shutdown hook, so we start a new instance
        _counterInstance = new Shutdown(seconds, restart);
        _counterInstance.start();
    }

    /**
     * This function aborts a running countdown
     *
     * @param activeChar	GM who issued the abort command
     */
    public void abort(L2PcInstance activeChar) {
        Announcements _an = Announcements.getInstance();
        _log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");
        _an.announceToAll(MODE_TEXT[_shutdownMode] + ", сервер продолжает свою работу.");

        _AbortShutdown = true;

        if (_counterInstance != null) {
            _counterInstance._abort();
        }
    }

    /**
     * set the shutdown mode
     *
     * @param mode	what mode shall be set
     */
    private void setMode(int mode) {
        _shutdownMode = mode;
    }

    /**
     * set shutdown mode to ABORT
     *
     */
    private void _abort() {
        _shutdownMode = ABORT;
    }

    /**
     * this counts the countdown and reports it to all players countdown is
     * aborted if mode changes to ABORT
     */
    private void countdown() {
        Announcements _an = Announcements.getInstance();
        try {
            while (_secondsShut > 0) {

                switch (_secondsShut) {
                    case 540:
                    case 480:
                    case 420:
                    case 360:
                    case 300:
                    case 240:
                    case 180:
                    case 120:
                    case 60:
                        LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN); //avoids new players from logging in
                        SendCountdownMMessage(MODE_TEXT[_shutdownMode], _secondsShut);
                        L2World.getInstance().deleteVisibleNpcSpawns();
                        break;
                    case 30:
                        for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                            if (player == null) {
                                continue;
                            }
                            try {
                                player.sendHtmlMessage("Выйдите из игры!");
                                player.setIsParalyzed(true);
                            } catch (Throwable t) {
                                // just to make sure we try to kill the connection 
                            }
                        }
                        _an.announceToAll(MODE_TEXT[_shutdownMode] + " через " + _secondsShut + " секунд!");
                        break;
                    case 5:
                        disconnectAllCharacters();
                        _an.announceToAll(MODE_TEXT[_shutdownMode] + " через " + _secondsShut + " секунд!");
                        _an.announceToAll("Пожалуйста, выйдите из игры.");
                        Broadcast.toAllOnlinePlayers(SystemMessage.id(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS).addNumber(_secondsShut));
                        break;
                }

                _secondsShut--;

                int delay = 1000; //milliseconds
                Thread.sleep(delay);

                if (_shutdownMode == ABORT) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            //this will never happen
        }
    }

    private void SendCountdownMMessage(String Mode, int secs) {
        Announcements _an = Announcements.getInstance();
        _an.announceToAll(Mode + " сервера через " + secs / 60 + " минут.");
        _an.announceToAll("Пожалуйста, выйдите из игры.");

        Broadcast.toAllOnlinePlayers(SystemMessage.id(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS).addNumber(secs));
    }

    /**
     * this sends a last byebye, disconnects all players and saves data
     *
     */
    private void saveData() {
        Announcements _an = Announcements.getInstance();
        try {
            _an.announceToAll(MODE_TEXT[_shutdownMode] + " сервера!");
        } catch (Throwable t) {
            _log.log(Level.INFO, "", t);
        }

        // Seven Signs data is now saved along with Festival data.
        if (!SevenSigns.getInstance().isSealValidationPeriod()) {
            SevenSignsFestival.getInstance().saveFestivalData(false);
        }
        System.err.println("Seven Signs Festival Data       ... Saved");

        // Save Seven Signs data before closing. :)
        SevenSigns.getInstance().saveSevenSignsData(null, true);
        System.err.println("Seven Sings Data                ... Saved");

        // Save all raidboss status ^_^
        RaidBossSpawnManager.getInstance().cleanUp();
        System.err.println("Raid Boss Spawn Data            ... Saved");
        GrandBossManager.getInstance().cleanUp();
        System.err.println("Grand Boss Spawn Data           ... Saved");
        TradeController.getInstance().dataCountStore();
        System.err.println("Trade Controller Data           ... Saved");

        try {
            OlympiadDatabase.save();
            System.err.println("Olympiad Data                   ... Saved");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Save Cursed Weapons data before closing.
        // Save Cursed Weapons data before closing.
        CursedWeaponsManager.getInstance().saveData();
        System.err.println("Cursed Weapons Data             ... Saved");

        // Save all manor data
        CastleManorManager.getInstance().save();
        System.err.println("Castle Manor Manager Data       ... Saved");

        // Save all global (non-player specific) Quest data that needs to persist after reboot
        QuestManager.getInstance().save();
        System.err.println("Data saved. All players disconnected, shutting down. Wait 5 seconds...");

        try {
            int delay = 5000;
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            //never happens :p
        }
    }

    /**
     * this disconnects all clients from the server
     *
     */
    private void disconnectAllCharacters() {
        //SystemMessage sysm = Static.YOU_HAVE_BEEN_DISCONNECTED;
        for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
            if (player == null) {
                continue;
            }
            //Logout Character
            try {
                /*
                 * if (player.isInOfflineMode()) { player.logout(); } else {
                 * player.sendPacket(sysm); player.sendPacket(new
                 * ServerClose());
                 }
                 */
                if (player.isInOfflineMode()) {
                    player.logout();
                } else {
                    player.kick();
                }
                //		player.logout();
                //L2GameClient.saveCharToDisk(player);
            } catch (Throwable t) {
            }
        }

        try {
            Thread.sleep(1000);
        } catch (Throwable t) {
            _log.log(Level.INFO, "", t);
        }

        for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
            if (player == null) {
                continue;
            }
            try {
                player.closeNetConnection();
            } catch (Throwable t) {
                // just to make sure we try to kill the connection 
            }
        }
    }
}
