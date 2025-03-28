package net.sf.l2j.gameserver;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastSet;
import net.sf.l2j.Config;
import net.sf.l2j.util.log.AbstractLogger;

public final class DeadlockDetector extends Thread {

    private static final Logger _log = AbstractLogger.getLogger(DeadlockDetector.class.getName());
    private final ThreadMXBean _mbean = ManagementFactory.getThreadMXBean();
    private final Set<Long> _logged = new FastSet<Long>();
    private static DeadlockDetector _instance;

    private DeadlockDetector() {
    }

    public static DeadlockDetector getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new DeadlockDetector();
        _instance.setDaemon(true);
        _instance.setPriority(MIN_PRIORITY);
        _instance.start();
    }

    @Override
    public void run() {
        for (;;) {
            try {
                new Thread() {

                    @Override
                    public void run() {
                        try {
                            checkForDeadlocks();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.run();

                try {
                    Thread.sleep(Config.DEADLOCKCHECK_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkForDeadlocks() {
        long[] ids = findDeadlockedThreadIDs();
        if (ids == null) {
            return;
        }

        List<Thread> deadlocked = new FastList<Thread>();

        for (long id : ids) {
            if (_logged.add(id)) {
                deadlocked.add(findThreadById(id));
            }
        }

        if (!deadlocked.isEmpty()) {
            System.out.println("Deadlocked Thread(s)");
            for (Thread thread : deadlocked) {
                _log.warning("ERROR:" + thread);
                for (StackTraceElement trace : thread.getStackTrace()) {
                    _log.warning("\tat " + trace);
                }

            }
            System.out.println("Kill deadlocked Thread(s)...");
            for (Thread thread : deadlocked) {
                thread.interrupt();
            }
            System.out.println("Done.");
        }
    }

    private long[] findDeadlockedThreadIDs() {
        if (_mbean.isSynchronizerUsageSupported()) {
            return _mbean.findDeadlockedThreads();
        }
        return _mbean.findMonitorDeadlockedThreads();
    }

    private Thread findThreadById(long id) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getId() == id) {
                return thread;
            }
        }
        throw new IllegalStateException("Deadlocked Thread not found!");
    }
}