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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.1.4.8 $ $Date: 2005/04/06 16:13:24 $
 */
public class GameTimeController {

    private static final Logger _log = AbstractLogger.getLogger(GameTimeController.class.getName());
    public static final int TICKS_PER_SECOND = 10;
    public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;
    private static GameTimeController _ins;// = new GameTimeController();
    private static int _gt;
    private static long _gst;
    private static boolean _in = false;
    private static ConcurrentLinkedQueue<L2Character> _movingObjects = new ConcurrentLinkedQueue<L2Character>();
    private static Thread _timer;
    //private ScheduledFuture<?> _tw;

    /**
     * one ingame day is 240 real minutes
     */
    public static GameTimeController getInstance() {
        return _ins;
    }
    private boolean _interruptRequest = false;

    private GameTimeController() {}
    
    public static void init()
    {
        _ins = new GameTimeController();
        _ins.load();
    }
    
    private void load()
    {
        _gst = System.currentTimeMillis() - 3600000; // offset so that the server starts a day begin
        _gt = 3600000 / MILLIS_IN_TICK; // offset so that the server starts a day begin

        _timer = new Thread(new TimerThread());
        _timer.setName("GameTimeController");
        _timer.setDaemon(true);
        _timer.setPriority(Thread.MAX_PRIORITY);
        _timer.start();

        //_tw = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TimerWatcher(), 0, 1000);
        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new BroadcastSunState(), 0, TimeUnit.MINUTES.toMillis(10));

        ThreadPoolManager.getInstance().scheduleGeneral(new ZakenDoor(1), TimeUnit.HOURS.toMillis(3));
    }

    public boolean isNowNight() {
        return _in;
    }

    public int getGameTime() {
        return (_gt / (TICKS_PER_SECOND * 10));
    }

    public static int getGameTicks() {
        return _gt;
    }

    /**
     * Add a L2Character to movingObjects of GameTimeController.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController.<BR><BR>
     *
     * @param cha The L2Character to add to movingObjects of GameTimeController
     *
     */
    public void registerMovingObject(L2Character cha) {
        if (cha == null) {
            return;
        }
        if (_movingObjects.contains(cha)) {
            return;
        }

        _movingObjects.add(cha);
    }

    /**
     * Move all L2Characters contained in movingObjects of GameTimeController.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Update the position of each L2Character </li>
     * <li>If movement is finished, the L2Character is removed from movingObjects </li>
     * <li>Create a task to update the _knownObject and _knowPlayers of each L2Character that finished its movement and of their already known L2Object then notify AI with EVT_ARRIVED </li><BR><BR>
     *
     */
    protected void moveObjects() {
        // Create an ArrayList to contain all L2Character that are arrived to destination
        ConcurrentLinkedQueue<L2Character> ended = new ConcurrentLinkedQueue<L2Character>();

        // Go throw the table containing L2Character in movement
        for (L2Character cha : _movingObjects) {

            // If movement is finished, the L2Character is removed from movingObjects and added to the ArrayList ended
            if (cha.updatePosition(_gt)) {
                _movingObjects.remove(cha);
                ended.add(cha);
            }
        }

        // Create a task to update the _knownObject and _knowPlayers of each L2Character that finished its movement and of their already known L2Object
        // then notify AI with EVT_ARRIVED
        // TODO: maybe a general TP is needed for that kinda stuff (all knownlist updates should be done in a TP anyway).
        if (!ended.isEmpty()) {
            ThreadPoolManager.getInstance().scheduleMove(new MovingObjectArrived(ended), 10);
        }

    }

    public void stopTimer() {
        //_tw.cancel(true);
        _interruptRequest = true;
        _timer.interrupt();
    }

    class TimerThread implements Runnable {

        public TimerThread() {
        }

        @Override
        public void run() {
            int oldTicks;
            long runtime;
            int sleepTime;
            for (;;) {

                try {
                    oldTicks = _gt; // save old ticks value to avoid moving objects 2x in same tick
                    runtime = System.currentTimeMillis() - _gst; // from server boot to now

                    _gt = (int) (runtime / MILLIS_IN_TICK); // new ticks value (ticks now)

                    if (oldTicks != _gt) {
                        moveObjects(); // XXX: if this makes objects go slower, remove it
                    }					// but I think it can't make that effect. is it better to call moveObjects() twice in same
                    // tick to make-up for missed tick ?   or is it better to ignore missed tick ?
                    // (will happen very rarely but it will happen ... on garbage collection definitely)

                    runtime = (System.currentTimeMillis() - _gst) - runtime;

                    // calculate sleep time... time needed to next tick minus time it takes to call moveObjects()
                    sleepTime = 1 + MILLIS_IN_TICK - ((int) runtime) % MILLIS_IN_TICK;

                    //_log.finest("TICK: "+_gt);

                    //sleep(sleepTime); // hope other threads will have much more cpu time available now
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }

                    // SelectorThread most of all
                } catch (InterruptedException ie) {
                    if (_interruptRequest) {
                        return;
                    }

                    _log.warning("" + ie);
                    ie.printStackTrace();
                } catch (Exception e) {
                    _log.warning("" + e);
                    e.printStackTrace();
                }

            }
        }
    }

    /*class TimerWatcher implements Runnable {
    
    @Override
    public void run() {
    if (!_timer.isAlive()) {
    String time = (new SimpleDateFormat("HH:mm:ss")).format(new Date());
    _log.warning(time + " TimerThread stop with following error. restart it.");
    if (_timer._error != null) {
    _timer._error.printStackTrace();
    }
    
    _timer = new TimerThread();
    _timer.start();
    }
    }
    }*/
    /**
     * Update the _knownObject and _knowPlayers of each L2Character that finished its movement and of their already known L2Object then notify AI with EVT_ARRIVED.<BR><BR>
     */
    static class MovingObjectArrived implements Runnable {

        private final ConcurrentLinkedQueue<L2Character> ended;

        MovingObjectArrived(ConcurrentLinkedQueue<L2Character> ended) {
            this.ended = ended;
        }

        @Override
        public void run() {
            for (L2Character cha : ended) {
                try {
                    cha.getKnownList().updateKnownObjects();
                    cha.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param rise
     */
    class BroadcastSunState implements Runnable {

        @Override
        public void run() {
            int h = (getGameTime() / 60) % 24; // Time in hour
            boolean tempIsNight = (h < 6);

            if (tempIsNight != _in) { // If diff day/night state
                _in = tempIsNight; // Set current day/night varible to value of temp varible

                DayNightSpawnManager.getInstance().notifyChangeMode();
            }
        }
    }

    /* 
     * Zaken_Door class opens the door at 0:00
     * Zaken_Door class closes the door at 0:25
     */
    private class ZakenDoor implements Runnable {

        private int act;

        public ZakenDoor(int act) {
            this.act = act;
        }

        @Override
        public void run() {
            switch (act) {
                case 1:
                    DoorTable.getInstance().getDoor(21240006).openMe();
                    ThreadPoolManager.getInstance().scheduleGeneral(new ZakenDoor(2), 120000);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ZakenDoor(1), TimeUnit.HOURS.toMillis(4));
                    break;
                case 2:
                    DoorTable.getInstance().getDoor(21240006).closeMe();
                    DoorTable.getInstance().checkDoorsBetween();
                    break;
            }
        }
    }
}
