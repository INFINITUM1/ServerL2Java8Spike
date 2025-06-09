package scripts.autoevents.schuttgart;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

import javolution.util.FastList;
import javolution.util.FastMap;

public class Schuttgart {

    private static EventManager _event = EventManager.getInstance();
    //настройки времени
    private static final int BOSSLIFE = Config.SCH_TIMEBOSS; //время жизни босса
    //<
    private static final int TIME1 = Config.SCH_TIME1; // Время до начала спауна монстров, после обьявления о начале евента
    private static final int TIME2 = Config.SCH_TIME2; // 2 волна мобов
    private static final int TIME3 = Config.SCH_TIME3; // 3 волна мобов
    private static final int TIME4 = Config.SCH_TIME4; // 4 волна мобов
    private static final int TIME5 = Config.SCH_TIME5; // 5 волна мобов
    private static final int TIME6 = Config.SCH_TIME6; // 6 волна мобов
    private static int LIMIT = 0; //евент закончится после прохода спауна всех волн и босса.
    private static long NEXTTIME = 0; //повтороное проведение евента через
    private static final int RESTART = (Config.SCH_RESTART * 60000); //время запуска после перезагрузки скрипта, если удалить значения в бд
    private static final int WAVE1 = Config.SCH_MOB1; //мобы первой волны
    private static final int WAVE2 = Config.SCH_MOB2; //мобы первой волны
    private static final int WAVE3 = Config.SCH_MOB3; //мобы первой волны
    private static final int WAVE4 = Config.SCH_MOB4; //мобы первой волны
    private static final int WAVE5 = Config.SCH_MOB5; //мобы первой волны
    private static final int WAVE6 = Config.SCH_MOB6; //мобы первой волны
    private static final int BOSS = Config.SCH_BOSS; //Boss
    private static L2RaidBossInstance bossSpawn = null;
    private static final int NPCSHOP = Config.SCH_SHOP; //нпц которого спавним после победы над монстрами
    private static int NPCLIFE = 0; // время спауна нпц, 20 минут
    private static L2Spawn SHOP = null;
    private boolean _active = false;
    private ScheduledFuture<?> _cycleTask = null;
    private ScheduledFuture<?> _finishTask = null;
    private static final FastMap<Integer, FastList<L2Spawn>> spawns = new FastMap<Integer, FastList<L2Spawn>>();
    private static Schuttgart _instance;

    public static Schuttgart getEvent() {
        return _instance;
    }

    public static void init() {
        _instance = new Schuttgart();
        _instance.load();
    }

    public void load() {
        NEXTTIME = Config.SCH_NEXT * 3600000;
        LIMIT = TIME1 + TIME2 + TIME3 + TIME4 + TIME5 + TIME6 + BOSSLIFE;
        //кешируем мобов, что-бы во время евента не нагружать цпу созданием и спавном новых монстров
        try {
            FastList<L2Spawn> mobs = new FastList<L2Spawn>();
            L2NpcTemplate npc = NpcTable.getInstance().getTemplate(WAVE1);
            if (npc != null) {
                for (int i = 5; i > -1; i--) {
                    mobs.add(getSpawn(npc, WAVE1, (87271 + Rnd.get(100)), (-137217 + Rnd.get(100)), -2280));
                }
                for (int i = 5; i > -1; i--) {
                    mobs.add(getSpawn(npc, WAVE1, (91904 + Rnd.get(100)), (-139434 + Rnd.get(100)), -2280));
                }
                for (int i = 5; i > -1; i--) {
                    mobs.add(getSpawn(npc, WAVE1, (82648 + Rnd.get(100)), (-139434 + Rnd.get(100)), -2280));
                }
                spawns.put(1, mobs);
            }

            //mobs.clear();
            FastList<L2Spawn> mobs2 = new FastList<L2Spawn>();
            npc = NpcTable.getInstance().getTemplate(WAVE2);
            if (npc != null) {
                mobs2.add(getSpawn(npc, WAVE2, 87370, -140183, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 87586, -140366, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 87124, -140399, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 87345, -140634, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 85309, -141943, -1495));
                mobs2.add(getSpawn(npc, WAVE2, 85066, -141654, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 84979, -141423, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 84951, -141875, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 89619, -141752, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 89398, -141956, -1487));
                mobs2.add(getSpawn(npc, WAVE2, 89677, -141866, -1541));
                mobs2.add(getSpawn(npc, WAVE2, 89712, -141388, -1541));
                spawns.put(2, mobs2);
            }

            //mobs.clear();
            FastList<L2Spawn> mobs3 = new FastList<L2Spawn>();
            npc = NpcTable.getInstance().getTemplate(WAVE3);
            if (npc != null) {
                mobs3.add(getSpawn(npc, WAVE3, 88887, -142259, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 88780, -142220, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 88710, -142575, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 88503, -142547, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 87168, -141752, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 87313, -141630, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 87434, -141917, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 87204, -142156, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 86277, -142634, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 86180, -142421, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 85908, -142485, -1340));
                mobs3.add(getSpawn(npc, WAVE3, 85943, -142266, -1340));
                spawns.put(3, mobs3);
            }

            //mobs.clear();
            FastList<L2Spawn> mobs4 = new FastList<L2Spawn>();
            npc = NpcTable.getInstance().getTemplate(WAVE4);
            if (npc != null) {
                mobs4.add(getSpawn(npc, WAVE3, 87168, -141752, -1340));
                mobs4.add(getSpawn(npc, WAVE3, 87313, -141630, -1340));
                mobs4.add(getSpawn(npc, WAVE3, 87434, -141917, -1340));
                mobs4.add(getSpawn(npc, WAVE3, 87204, -142156, -1340));
                mobs4.add(getSpawn(npc, WAVE4, 87955, -142804, -1340));
                mobs4.add(getSpawn(npc, WAVE4, 87956, -142608, -1340));
                mobs4.add(getSpawn(npc, WAVE4, 87642, -142589, -1340));
                mobs4.add(getSpawn(npc, WAVE4, 87402, -142651, -1340));
                mobs4.add(getSpawn(npc, WAVE4, 87261, -142558, -1340));
                mobs4.add(getSpawn(npc, WAVE4, 87010, -142625, -1340));
                mobs4.add(getSpawn(npc, WAVE4, 86771, -142818, -1340));
                spawns.put(4, mobs4);
            }

            //mobs.clear();
            FastList<L2Spawn> mobs5 = new FastList<L2Spawn>();
            npc = NpcTable.getInstance().getTemplate(WAVE5);
            if (npc != null) {
                mobs5.add(getSpawn(npc, WAVE5, 87505, -143049, -1292));
                mobs5.add(getSpawn(npc, WAVE5, 87236, -142939, -1292));
                mobs5.add(getSpawn(npc, WAVE5, 87202, -143257, -1292));
                mobs5.add(getSpawn(npc, WAVE5, 87466, -143269, -1292));
                mobs5.add(getSpawn(npc, WAVE5, 87426, -143537, -1292));
                mobs5.add(getSpawn(npc, WAVE5, 87313, -143461, -1292));
                mobs5.add(getSpawn(npc, WAVE5, 87358, -143878, -1292));
                mobs5.add(getSpawn(npc, WAVE5, 87353, -144076, -1292));
                mobs5.add(getSpawn(npc, WAVE5, 87350, -144355, -1292));
                mobs5.add(getSpawn(npc, WAVE4, 87955, -142804, -1340));
                mobs5.add(getSpawn(npc, WAVE4, 87956, -142608, -1340));
                mobs5.add(getSpawn(npc, WAVE4, 87642, -142589, -1340));
                mobs5.add(getSpawn(npc, WAVE4, 87402, -142651, -1340));
                spawns.put(5, mobs5);
            }

            //mobs.clear();
            FastList<L2Spawn> mobs6 = new FastList<L2Spawn>();
            npc = NpcTable.getInstance().getTemplate(WAVE6);
            if (npc != null) {
                mobs6.add(getSpawn(npc, WAVE5, 87466, -143269, -1292));
                mobs6.add(getSpawn(npc, WAVE5, 87426, -143537, -1292));
                mobs6.add(getSpawn(npc, WAVE5, 87313, -143461, -1292));
                mobs6.add(getSpawn(npc, WAVE5, 87358, -143878, -1292));
                mobs6.add(getSpawn(npc, WAVE5, 87353, -144076, -1292));
                mobs6.add(getSpawn(npc, WAVE5, 87350, -144355, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87394, -144725, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87329, -144734, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87361, -144651, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87511, -144964, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87390, -144697, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87276, -145006, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87114, -145285, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87378, -145255, -1292));
                mobs6.add(getSpawn(npc, WAVE6, 87575, -145295, -1292));
                //mobs6.add(getSpawn(npc, BOSS, 87362, -145640, -1292));
                spawns.put(6, mobs6);
            }
            //mobs.clear();
            if (Config.SCH_ALLOW_SHOP) {
                NPCLIFE = Config.SCH_SHOPTIME * 60000;
                npc = NpcTable.getInstance().getTemplate(NPCSHOP);
                if (npc != null) {
                    SHOP = getSpawn(npc, NPCSHOP, 87508, -143595, -1292);
                }
            }
        } catch (Exception e) {
            System.out.println("Schuttgart: error: " + e);
        }
        checkTimer();
        //System.out.println("Schuttgart: loaded.");
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
        long nextStart = _event.GetDBValue("Schuttgart", "nextStart") - System.currentTimeMillis();
        if (nextStart < RESTART) {
            nextStart = RESTART;
        }

        _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(0), nextStart);
        System.out.println("EventManager: Schuttgart, start after " + (nextStart / 60000) + " min.");
    }

    public class CycleTask implements Runnable {

        private int cycle;

        public CycleTask(int cycle) {
            this.cycle = cycle;
        }

        public void run() {
            switch (cycle) {
                case 0:
                    if (_active) {
                        return;
                    }

                    _active = true;
                    announce(Static.SCH_STARTED);
                    _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(1), TIME1);
                    _event.SetDBValue("Schuttgart", "nextStart", "" + (System.currentTimeMillis() + NEXTTIME));
                    break;
                case 1:
                    announce(Static.SCH_STEP1);
                    _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(2), TIME2);
                    _finishTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(99), LIMIT);
                    break;
                case 2:
                    _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(3), TIME3);
                    break;
                case 3:
                    announce(Static.SCH_STEP2);
                    _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(4), TIME4);
                    break;
                case 4:
                    announce(Static.SCH_STEP3);
                    _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(5), TIME5);
                    break;
                case 5:
                    announce(Static.SCH_STEP4);
                    _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(6), TIME6);
                    break;
                case 6:
                    announce(Static.SCH_STEP5);
                    bossSpawn = (L2RaidBossInstance) GrandBossManager.getInstance().createOnePrivateEx(BOSS, 87362, -145640, -1292, 0);
                    _cycleTask = null;
                    break;
                case 99:
                    if (!_active) {
                        return;
                    }

                    _active = false;
                    _finishTask = null;
                    announce(Static.SCH_FAIL);
                    deleteSpawns();
                    manageNextTime();
                    break;
            }

            if (cycle >= 1 && cycle <= 6) {
                FastList<L2Spawn> mobs = spawns.get(cycle);
                for (FastList.Node<L2Spawn> n = mobs.head(), end = mobs.tail(); (n = n.getNext()) != end;) {
                    L2Spawn mob = n.getValue();
                    if (mob == null) {
                        continue;
                    }

                    mob.spawnOne();
                    try // для разгрузки цпу
                    {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
    }

    private void manageNextTime() {
        _cycleTask = ThreadPoolManager.getInstance().scheduleGeneral(new CycleTask(0), NEXTTIME);
        _active = false;
    }

    public void notifyBossDie(L2PcInstance player) {
        if (!_active) {
            return;
        }

        announce(Static.SCH_WIN);

        _active = false;
        if (_finishTask != null) {
            _finishTask.cancel(true);
        }

        _finishTask = null;
        _cycleTask = null;

        if (SHOP != null) {
            SHOP.spawnOne();
            ThreadPoolManager.getInstance().scheduleGeneral(new UnspawnShop(), NPCLIFE);
        }

        manageNextTime();
    }

    public static class UnspawnShop implements Runnable {

        public UnspawnShop() {
        }

        public void run() {
            if (SHOP != null) {
                SHOP.getLastSpawn().deleteMe();
            }
        }
    }

    public void startScript(L2PcInstance player) {
        if (_active) {
            player.sendHtmlMessage("Schuttgart", "Уже запущен.");
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
            player.sendHtmlMessage("Schuttgart", "Не запущен.");
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
        if (bossSpawn != null) {
            bossSpawn.decayMe();
            bossSpawn.deleteMe();
        }
        bossSpawn = null;

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
                            Thread.sleep(40);
                        } catch (InterruptedException ex) {
                        }
                    }

                    try // для разгрузки цпу
                    {
                        Thread.sleep(40);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }).start();
    }

    private void announce(String text) {
        Announcements.getInstance().announceToAll(text);
    }
}
