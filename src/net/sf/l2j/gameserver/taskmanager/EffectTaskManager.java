package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.taskmanager.utils.RunnableImpl;
import net.sf.l2j.gameserver.taskmanager.utils.SteppingRunnableQueueManager;
import net.sf.l2j.util.Rnd;

/**
 * Менеджер задач для работы с эффектами, шаг выполенния задач 250 мс.
 *
 * @author G1ta0
 */
public class EffectTaskManager extends SteppingRunnableQueueManager {

    private final static long TICK = 250L;

    private static int _randomizer;

    private final static EffectTaskManager[] _instances = new EffectTaskManager[2];

    static {
        for (int i = 0; i < _instances.length; i++) {
            _instances[i] = new EffectTaskManager();
        }
    }

    public final static EffectTaskManager getInstance() {
        return _instances[_randomizer++ & (_instances.length - 1)];
    }

    private EffectTaskManager() {
        super(TICK);
        ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, Rnd.get(TICK), TICK);
        //Очистка каждые 30 секунд со сдвигом
        ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new RunnableImpl() {
            @Override
            public void runImpl() throws Exception {
                EffectTaskManager.this.purge();
            }

        }, 30000L + 1000L * _randomizer++, 30000L);
    }

    public CharSequence getStats(int num) {
        return _instances[num].getStats();
    }
}
