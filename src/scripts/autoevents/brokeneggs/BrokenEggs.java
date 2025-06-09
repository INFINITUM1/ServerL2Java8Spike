package scripts.autoevents.brokeneggs;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.util.Location;

public class BrokenEggs {

    private boolean _active = false;
    private ScheduledFuture<?> _cycleTask = null;
    private ScheduledFuture<?> _finishTask = null;
    private static final FastMap<Integer, FastList<L2Spawn>> spawns = new FastMap<Integer, FastList<L2Spawn>>();
    private static final FastMap<Integer, Integer> guards = Config.BE_GUARDS;
    private static BrokenEggs _instance;
    private static L2Spawn EGG = null;
    private static Location eggLoc = Config.BE_EGG_LOC;
    private static FastList<L2Spawn> privates = new FastList<L2Spawn>();
    private static volatile int guardsCount = 0;

    public static BrokenEggs getEvent() {
        return _instance;
    }

    public static void init() {
        _instance = new BrokenEggs();
        _instance.load();
    }

    public void load() {
        try {
            L2NpcTemplate npc = NpcTable.getInstance().getTemplate(Config.BE_EGG_ID);
            if (npc != null) {
                EGG = getSpawn(npc, Config.BE_EGG_ID, eggLoc.x, eggLoc.y, eggLoc.z);
                privates.add(EGG);
            }

            for (FastMap.Entry<Integer, Integer> e = guards.head(), end = guards.tail(); (e = e.getNext()) != end;) {
                Integer id = e.getKey(); // No typecast necessary.
                Integer count = e.getValue(); // No typecast necessary.
                if (id == null || count == null) {
                    continue;
                }
                npc = NpcTable.getInstance().getTemplate(id);
                if (npc == null) {
                    continue;
                }

                for (int i = count; i > -1; i--) {

                    int posX = eggLoc.x;
                    int posY = eggLoc.y;
                    switch (Rnd.get(1, 4)) {
                        case 1:
                            posX += Rnd.get(30, 50);
                            posY += Rnd.get(160, 200);
                            break;
                        case 2:
                            posX += Rnd.get(60, 80);
                            posY -= Rnd.get(95, 105);
                            break;
                        case 3:
                            posX -= Rnd.get(140, 160);
                            posY -= Rnd.get(10, 30);
                            break;
                        case 4:
                            posX -= Rnd.get(85, 115);
                            posY += Rnd.get(40, 80);
                            break;
                    }
                    privates.add(getSpawn(npc, id, posX, posY, eggLoc.z));
                }
            }
        } catch (Exception e) {
            System.out.println("BrokenEggs: error: " + e);
        }
        checkTimer();
    }

    private L2Spawn getSpawn(L2NpcTemplate npc, int id, int x, int y, int z) throws SecurityException, ClassNotFoundException, NoSuchMethodException {
        L2Spawn spawn = new L2Spawn(npc);
        spawn.setId(id);
        spawn.setAmount(1);
        spawn.setLocx(x);
        spawn.setLocy(y);
        spawn.setLocz(z);
        spawn.setHeading(0);
        spawn.stopRespawn();
        return spawn;
    }

    public void checkTimer() {
        _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(0), Config.BE_RESTART);
        System.out.println("EventManager: BrokenEggs, start after " + (Config.BE_RESTART / 60000) + " min.");
    }

    public class CycleTask implements Runnable {

        private int cycle;

        public CycleTask(int cycle) {
            this.cycle = cycle;
        }

        @Override
        public void run() {
            switch (cycle) {
                case 0:
                    if (_active) {
                        return;
                    }

                    _active = true;
                    announce(Static.BE_STARTED);
                    spawnPrivates();
                    _finishTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(99), Config.BE_LIMIT);
                    break;
                case 99:
                    if (!_active) {
                        return;
                    }

                    _active = false;
                    _finishTask = null;
                    announce(Static.BE_TIME_LEFT);
                    deleteSpawns();
                    manageNextTime();
                    break;
            }
        }
    }

    private void spawnPrivates() {
        guardsCount = 0;
        for (FastList.Node<L2Spawn> n = privates.head(), end = privates.tail(); (n = n.getNext()) != end;) {
            L2Spawn mob = n.getValue();
            if (mob == null) {
                continue;
            }

            guardsCount++;
            mob.spawnOne();
            try // для разгрузки цпу
            {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
            }
        }
    }

    private void manageNextTime() {
        _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(0), Config.BE_NEXTTIME);
        _active = false;
    }

    public void notifyEggDie(L2PcInstance player) {
        if (!_active) {
            return;
        }

        announce(Static.BE_FINISHED);

        _active = false;
        if (_finishTask != null) {
            _finishTask.cancel(true);
        }

        _finishTask = null;
        _cycleTask = null;

        manageNextTime();
        rewardPlayer(player);
    }

    private void rewardPlayer(L2PcInstance player) {
        if (player == null) {
            return;
        }
        for (FastList.Node<EventReward> n = Config.BE_REWARDS.head(), end = Config.BE_REWARDS.tail(); (n = n.getNext()) != end;) {
            EventReward rew = n.getValue();
            if (rew == null) {
                continue;
            }
            if (Rnd.get(100) > rew.chance) {
                continue;
            }

            player.addItem("StoriumScrolls", rew.id, rew.count, player, true);
        }
    }

    public void startScript(L2PcInstance player) {
        if (_active) {
            player.sendHtmlMessage("BrokenEggs", "Уже запущен.");
            return;
        }

        if (_finishTask != null) {
            _finishTask.cancel(true);
        }
        if (_cycleTask != null) {
            _cycleTask.cancel(true);
        }

        _finishTask = null;
        _cycleTask = null;

        _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(0), 1000);
    }

    public void stopScript(L2PcInstance player) {
        if (!_active) {
            player.sendHtmlMessage("BrokenEggs", "Не запущен.");
            return;
        }
        if (_finishTask != null) {
            _finishTask.cancel(true);
        }

        if (_cycleTask != null) {
            _cycleTask.cancel(true);
        }

        announce(Static.SCH_ADM_CANCEL);
        deleteSpawns();
    }

    private void deleteSpawns() {
        new Thread(new Runnable() {

            public void run() {
                for (FastMap.Entry<Integer, FastList<L2Spawn>> e = spawns.head(), end = spawns.tail(); (e = e.getNext()) != end;) {
                    Integer wave = e.getKey();
                    FastList mobs = e.getValue();
                    if (wave == null || mobs == null) {
                        continue;
                    }

                    for (FastList.Node<L2Spawn> n = mobs.head(), mend = mobs.tail(); (n = n.getNext()) != mend;) {
                        L2Spawn mob = n.getValue();
                        if (mob == null) {
                            continue;
                        }

                        L2NpcInstance npc = mob.getLastSpawn();
                        if (npc == null || npc.isDead()) {
                            continue;
                        }

                        npc.deleteMe();
                        try // для разгрузки цпу
                        {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                        }
                    }

                    try // для разгрузки цпу
                    {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }).start();
    }

    private void announce(String text) {
        Announcements.getInstance().announceToAll(text);
    }

    public void notifyGuardDie() {
        guardsCount--;
    }

    public boolean canAttackEgg() {
        return guardsCount <= 1;
    }
}
