/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.l2j.gameserver.taskmanager;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.taskmanager.utils.RunnableImpl;
import net.sf.l2j.gameserver.taskmanager.utils.SteppingRunnableQueueManager;

/**
 *
 * @author Администратор
 */
public class LazyPrecisionTaskManager extends SteppingRunnableQueueManager {

    private static final LazyPrecisionTaskManager _instance = new LazyPrecisionTaskManager();

    public static final LazyPrecisionTaskManager getInstance() {
        return _instance;
    }

    private LazyPrecisionTaskManager() {
        super(1000L);
        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, 1000L, 1000L);
        //Очистка каждые 60 секунд
        ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new RunnableImpl() {
            @Override
            public void runImpl() throws Exception {
                LazyPrecisionTaskManager.this.purge();
            }

        }, 60000L, 60000L);
    }

}
