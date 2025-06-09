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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket;
import net.sf.l2j.util.log.AbstractLogger;

public class ThreadPoolManager {

    public static class PriorityThreadFactory implements ThreadFactory {

        private int _prio;
        private String _name;
        private AtomicInteger _threadNumber = new AtomicInteger(1);
        private ThreadGroup _group;

        public PriorityThreadFactory(String name, int prio) {
            _prio = prio;
            _name = name;
            _group = new ThreadGroup(_name);
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(_group, r);
            t.setName(_name + "-" + _threadNumber.getAndIncrement());
            t.setPriority(_prio);
            return t;
        }

        public ThreadGroup getGroup() {
            return _group;
        }
    }

    private static final Logger _log = AbstractLogger.getLogger(ThreadPoolManager.class.getName());
    private static ThreadPoolManager _instance;
    private boolean _shutdown;

    private ScheduledThreadPoolExecutor _scheduledExecutor;
    private ThreadPoolExecutor _executor;

    private static final long _minDelay = 0L;

    public static ThreadPoolManager getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new ThreadPoolManager();
        _instance.load();
    }

    private ThreadPoolManager() {
    }

    private void load() {
        _scheduledExecutor = new ScheduledThreadPoolExecutor(Config.SCHEDULED_THREAD_POOL_SIZE, new PriorityThreadFactory("ScheduledThreadPool", Thread.NORM_PRIORITY), new ThreadPoolExecutor.CallerRunsPolicy());
        _executor = new ThreadPoolExecutor(Config.EXECUTOR_THREAD_POOL_SIZE, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("ThreadPoolExecutor", Thread.NORM_PRIORITY), new ThreadPoolExecutor.CallerRunsPolicy());

        scheduleGeneralAtFixedRate(new Runnable() {
            @Override
            public void run() {
                _scheduledExecutor.purge();
                _executor.purge();
            }
        }, 300000L, 300000L);
    }

    @Deprecated
    public ScheduledFuture<?> scheduleEffect(Runnable r, long delay) {
        return _scheduledExecutor.schedule(r, Math.max(delay, _minDelay), TimeUnit.MILLISECONDS);
    }

    @Deprecated
    public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable r, long initial, long delay) {
        return _scheduledExecutor.scheduleAtFixedRate(r, Math.max(initial, _minDelay), Math.max(delay, _minDelay), TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> scheduleGeneral(Runnable r, long delay) {

        return _scheduledExecutor.schedule(r, Math.max(delay, _minDelay), TimeUnit.MILLISECONDS);

    }

    public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable r, long initial, long delay) {

        return _scheduledExecutor.scheduleAtFixedRate(r, Math.max(initial, _minDelay), Math.max(delay, _minDelay), TimeUnit.MILLISECONDS);

    }

    public <T extends Runnable> ScheduledFuture<T> scheduleAi(T r, long delay, boolean isPlayer) {

        return (ScheduledFuture<T>) _scheduledExecutor.schedule(r, Math.max(delay, _minDelay), TimeUnit.MILLISECONDS);

    }

    public <T extends Runnable> ScheduledFuture<T> scheduleAiAtFixedRate(T r, long initial, long delay) {
        return (ScheduledFuture<T>) _scheduledExecutor.scheduleAtFixedRate(r, Math.max(initial, _minDelay), Math.max(delay, _minDelay), TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> scheduleMove(Runnable r, long delay) {
        return _scheduledExecutor.schedule(r, Math.max(delay, _minDelay), TimeUnit.MILLISECONDS);

    }

    public void executePacket(Runnable pkt) {
        //_generalPacketsThreadPool.execute(pkt);
        _executor.execute(pkt);
    }

    public void executeIOPacket(Runnable pkt) {
        //_ioPacketsThreadPool.execute(pkt);
        _executor.execute(pkt);
    }

    public void executeGeneral(Runnable r) {
        _executor.execute(r);
    }

    public void executeAi(Runnable r, boolean isPlayer) {
        _executor.execute(r);
        /*if (isPlayer) {
         _playerAiScheduledThreadPool.execute(r);
         } else {
         _npcAiScheduledThreadPool.execute(r);
         }*/
    }

    public void executePathfind(Runnable r) {
        //_pathfindThreadPool.execute(r);
    }

    public String[] getStats() {
        return new String[]{
            "TP:",
            " + AI:",
            " |- Not Done"
        };
    }

    public void shutdown() {
        _shutdown = true;
        _scheduledExecutor.shutdown();
        _executor.shutdown();
    }

    public boolean isShutdown() {
        return _shutdown;
    }

    /**
     *
     */
    public void purge() {
        _scheduledExecutor.purge();
        _executor.purge();
    }
}
