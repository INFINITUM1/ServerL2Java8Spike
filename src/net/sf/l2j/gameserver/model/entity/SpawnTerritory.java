package net.sf.l2j.gameserver.model.entity;

import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;

import javolution.util.FastList;
import javolution.util.FastMap;

public class SpawnTerritory {

    private int _id = 0;
    private boolean _autospawn = true;
    private boolean _bossLair = false;
    //private static int _maxNpc = 0;
    private int _minX = 0;
    private int _maxX = 0;
    private int _minY = 0;
    private int _maxY = 0;
    private int _minZ = 0;
    private int _maxZ = 0;
    private int _minRespawn = 60000;
    //private int WAYPOINTS = 0;
    private static int MOVE_DELAY = 10000;
    private Polygon _p = null;
    private Rectangle2D _r = null;
    private ScheduledFuture<?> _spawnTask = null;
    //private ScheduledFuture<?> _moveTask = null;
    private FastMap<L2Spawn, Integer> _spawns = new FastMap<L2Spawn, Integer>().shared("SpawnTerritory._spawns");
    private FastList<Location> _waypoints = new FastList<Location>();

    private class RespawnTask implements Runnable {

        public RespawnTask() {
        }

        public void run() {
            checkSpawns();
        }
    }

    public void checkSpawns() {
        boolean death = false;
        for (FastMap.Entry<L2Spawn, Integer> e = _spawns.head(), end = _spawns.tail(); (e = e.getNext()) != end;) {
            L2Spawn spawn = e.getKey();
            int respawn = e.getValue();
            if (spawn == null) {
                continue;
            }

            if (spawn.getLastKill() > 0 && (System.currentTimeMillis() - spawn.getLastKill()) >= respawn) {
                if (spawn.isFree()) {
                    int[] xy = getRandomPoint();
                    spawn.setLocx(xy[0]);
                    spawn.setLocy(xy[1]);
                    spawn.setLocz(Rnd.get(_minZ, _maxZ));
                    spawn.setHeading(Rnd.get(65535));
                    spawn.spawnOne();
                } else {
                    spawn.spawnOne();
                }

                spawn.setLastKill(0);
            } else {
                death = true;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
            }
        }
        if (death) {
            _spawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new RespawnTask(), _minRespawn);
        } else {
            _spawnTask = null;
        }
    }

    public SpawnTerritory(int id) {
        _id = id;
        //_maxNpc = maxNpc;

        //территория
        _p = new Polygon();
    }

    public void addPoint(int x, int y) {
        _p.addPoint(x, y);
    }

    public void setZ(int min, int max) {
        _minZ = min;
        _maxZ = max;
    }

    public int getMinZ() {
        return _minZ;
    }

    public int getMaxZ() {
        return _maxZ;
    }

    public int getId() {
        return _id;
    }

    public boolean isIdle() {
        //return (_spawnTask == null);
        return !_waypoints.isEmpty();
    }

    /*public static int getMaxNpc()
    {
    return _maxNpc;
    }*/
    public void close() {
        _r = _p.getBounds2D();

        _minX = (int) _r.getMinX();
        _maxX = (int) _r.getMaxX();
        _minY = (int) _r.getMinY();
        _maxY = (int) _r.getMaxY();

        /*if (isAutoSpawn())
        {
        _spawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new RespawnTask(), Config.NPC_SPAWN_DELAY);
        
        if(!_waypoints.isEmpty())
        {
        WAYPOINTS = _waypoints.size() - 1;
        ThreadPoolManager.getInstance().scheduleMove(new WaypointTask(), (Config.NPC_SPAWN_DELAY + 20000));
        }
        }*/
    }

    public void addSpawn(L2Spawn npc, int respawn) {
        if ((respawn < _minRespawn) || (respawn > 120000)) {
            _minRespawn = respawn;
        }

        _spawns.put(npc, respawn);
        //npc.spawnOne();
    }

    public void addWayPoint(int x, int y, int z) {
        _waypoints.add(new Location(x, y, z));
    }

    public void setWayPointDelay(int delay) {
        MOVE_DELAY = delay;
    }

    public void setManualSpawn() {
        _autospawn = false;
    }

    public void setBossSpawn() {
        _bossLair = true;
    }

    public boolean isAutoSpawn() {
        if (_bossLair) {
            return false;
        }

        return _autospawn;
    }

    public void spawn(int delay) {
        Lock shed = new ReentrantLock();
        shed.lock();
        try {
            if (_spawnTask != null) {
                return;
            }

            _spawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new RespawnTask(), delay);
        } finally {
            shed.unlock();
        }
    }

    public void notifyDeath() {
        Lock shed = new ReentrantLock();
        shed.lock();
        try {
            //if (_moveTask == null && _waypoints.isEmpty())
            //	_moveTask = ThreadPoolManager.getInstance().scheduleMove(new MoveTask(), MOVE_DELAY);

            if (_spawnTask != null || _bossLair) {
                return;
            }

            //System.out.println("##-> 1");
            _spawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new RespawnTask(), _minRespawn);
        } finally {
            shed.unlock();
        }
    }

    // случайная точка спауна внутри территории; getRandomPoint(locX, locY) для _moveTask?
    public int[] getRandomPoint() {
        //System.out.println("getRandomPoint##->STEP1->ID->" + _id);
        //System.out.println("getRandomPoint##->STEP1->MinX-> " + _minX);
        //System.out.println("getRandomPoint##->STEP1->MaxX-> " + _maxX);
        //System.out.println("getRandomPoint##->STEP1->MinY-> " + _minY);
        //System.out.println("getRandomPoint##->STEP1->MaxY-> " + _maxY);
        int x = 0;
        int y = 0;
        //int tempX = x;
        //int tempY = y;
        //System.out.println("getRandomPoint##->STEP2");
        do {
            x = Rnd.get(_minX, _maxX);
            y = Rnd.get(_minY, _maxY);
            //System.out.println("getRandomPoint##->STEP2->X-> " + tempX);
            //System.out.println("getRandomPoint##->STEP2->Y-> " + tempX);
        } while (!_p.contains(x, y));
        //System.out.println("getRandomPoint##->STEP3");

        int[] rndPoint = new int[2];
        //System.out.println("getRandomPoint##->STEP4");

        rndPoint[0] = (int) x;
        rndPoint[1] = (int) y;
        //System.out.println("getRandomPoint##->STEP4->X-> " + rndPoint[0]);
        //System.out.println("getRandomPoint##->STEP4->Y-> " + rndPoint[1]);
        //System.out.println("getRandomPoint##->STEP5");
        return rndPoint;
    }
}
