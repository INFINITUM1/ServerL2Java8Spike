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
package net.sf.l2j.gameserver.instancemanager;

import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: $ $Date: $
 * @author  godson
 */

public class DayNightSpawnManager {

    private static Logger _log = AbstractLogger.getLogger(DayNightSpawnManager.class.getName());

    private static DayNightSpawnManager _instance;
    private static FastMap<L2Spawn, L2NpcInstance> _dayCreatures;// = new FastMap<L2Spawn, L2NpcInstance>().shared("DayNightSpawnManager._dayCreatures");
    private static FastMap<L2Spawn, L2NpcInstance> _nightCreatures;// = new FastMap<L2Spawn, L2NpcInstance>().shared("DayNightSpawnManager._nightCreatures");
    private static FastMap<L2Spawn, L2RaidBossInstance> _bosses;// = new FastMap<L2Spawn, L2RaidBossInstance>().shared("DayNightSpawnManager._bosses");

    private DayNightSpawnManager()
    {
    }
	
    public static DayNightSpawnManager getInstance()
    {
        return _instance;
    }
	
	public static void init()
	{
		_instance = new DayNightSpawnManager();
		_instance.load(false);
	}

    public void load(boolean reload)
    {
        /*_dayCreatures = new FastMap<L2Spawn, L2NpcInstance>().shared("DayNightSpawnManager._dayCreatures");
        _nightCreatures = new FastMap<L2Spawn, L2NpcInstance>().shared("DayNightSpawnManager._nightCreatures");
        _bosses = new FastMap<L2Spawn, L2RaidBossInstance>().shared("DayNightSpawnManager._bosses");*/
		if (reload)
		{
			_dayCreatures.clear();
			_nightCreatures.clear();
			_bosses.clear();
		}
		else
		{
			_dayCreatures = new FastMap<L2Spawn, L2NpcInstance>().shared("DayNightSpawnManager._dayCreatures");
			_nightCreatures = new FastMap<L2Spawn, L2NpcInstance>().shared("DayNightSpawnManager._nightCreatures");
			_bosses = new FastMap<L2Spawn, L2RaidBossInstance>().shared("DayNightSpawnManager._bosses");
		}
        //_log.info("DayNightSpawnManager: Day/Night handler initialised");
    }

    public void addDayCreature(L2Spawn spawnDat)
    {
        if (!_dayCreatures.containsKey(spawnDat))
            _dayCreatures.put(spawnDat, null);
    }

    public void addNightCreature(L2Spawn spawnDat)
    {
        if (!_nightCreatures.containsKey(spawnDat))
            _nightCreatures.put(spawnDat, null);
    }
    /*
     * Spawn Day Creatures, and Unspawn Night Creatures
     */
    public void spawnDayCreatures()
	{
    	spawnCreatures(_nightCreatures, _dayCreatures, "night", "day");
	}
    /*
     * Spawn Night Creatures, and Unspawn Day Creatures
     */
    public void spawnNightCreatures()
	{
    	spawnCreatures(_dayCreatures, _nightCreatures, "day", "night");
	}
    /*
     * Manage Spawn/Respawn
     * Arg 1 : Map with L2NpcInstance must be unspawned
     * Arg 2 : Map with L2NpcInstance must be spawned
     * Arg 3 : String for log info for unspawned L2NpcInstance
     * Arg 4 : String for log info for spawned L2NpcInstance
     */
    private void spawnCreatures(final FastMap<L2Spawn, L2NpcInstance> toDelete, final FastMap<L2Spawn, L2NpcInstance> toSpawn, String UnspawnLogInfo, String SpawnLogInfo)
	{
		new Thread(new Runnable(){
			public void run()
			{
				for (FastMap.Entry<L2Spawn, L2NpcInstance> e = toDelete.head(), end = toDelete.tail(); (e = e.getNext()) != end;) 
				{
					L2Spawn key = e.getKey(); // No typecast necessary.
					L2NpcInstance value = e.getValue(); // No typecast necessary.
					if (key == null || value == null)
						continue;
					
                    value.getSpawn().stopRespawn();
                    value.decayMe();
                    value.deleteMe();
				}
				
				L2NpcInstance creature = null;
				for (FastMap.Entry<L2Spawn, L2NpcInstance> e = toSpawn.head(), end = toSpawn.tail(); (e = e.getNext()) != end;) 
				{
					L2Spawn key = e.getKey(); // No typecast necessary.
					L2NpcInstance value = e.getValue(); // No typecast necessary.
					if (key == null)
						continue;
					
					if (value == null)
					{
						value = key.doSpawn();
						if (value == null) 
							continue;

						toSpawn.put(key, value);
						value.setCurrentHp(value.getMaxHp());
						value.setCurrentMp(value.getMaxMp());
						value.getSpawn().startRespawn();
						if (value.isDecayed())
							value.setDecayed(false);
						if (value.isDead())
							value.doRevive();
					}
					else
					{
						value.getSpawn().startRespawn();
						if (value.isDecayed())
							value.setDecayed(false);
						if (value.isDead())
							value.doRevive();
						value.setCurrentHp(value.getMaxHp());
						value.setCurrentMp(value.getMaxMp());
						value.spawnMe();
					}
				}
				
			}
		}).start();
    }
	
    private void changeMode(int mode)
    {
        if (_nightCreatures.isEmpty() && _dayCreatures.isEmpty())
            return;

        switch(mode) 
		{
            case 0:
                spawnDayCreatures();
                specialNightBoss(0);
                break;
            case 1:
                spawnNightCreatures();
                specialNightBoss(1);
                break;
        }
    }

    public void notifyChangeMode()
    {
        try
		{
            if (GameTimeController.getInstance().isNowNight())
                changeMode(1);
            else
                changeMode(0);
        }
		catch(Exception e)
		{
			e.printStackTrace();
		}
    }

    public void cleanUp()
    {
        _nightCreatures.clear();
        _dayCreatures.clear();
        _bosses.clear();
    }

    private void specialNightBoss(int mode)
    {
		try
		{
			L2RaidBossInstance boss = null;
			for (L2Spawn spawn : _bosses.keySet())
			{
				if (spawn == null)
					continue;
					
				boss = _bosses.get(spawn);

				if (boss == null && mode == 1)
				{
					boss = (L2RaidBossInstance)spawn.doSpawn();
					RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
					_bosses.remove(spawn);
					_bosses.put(spawn, boss);
					continue;
				}

				if (boss == null && mode == 0)
					continue;
					
				if (boss == null)
					continue;

				if(boss.getNpcId() == 25328 && boss.getRaidStatus().equals(RaidBossSpawnManager.StatusEnum.ALIVE))
					handleHellmans(boss, mode);
				return;
			}
        }
		catch(Exception e)
		{
			e.printStackTrace();
		}
    }

    private void handleHellmans(L2RaidBossInstance boss, int mode)
    {
		switch(mode)
        {
            case 0:
                boss.deleteMe();
                _log.info("DayNightSpawnManager: Deleting Hellman raidboss");
                break;
            case 1:
                boss.spawnMe();
                _log.info("DayNightSpawnManager: Spawning Hellman raidboss");
                break;
        }
    }

    public L2RaidBossInstance handleBoss(L2Spawn spawnDat)
    {
        if(_bosses.containsKey(spawnDat)) 
			return _bosses.get(spawnDat);

        if (GameTimeController.getInstance().isNowNight())
        {
            L2RaidBossInstance raidboss = (L2RaidBossInstance)spawnDat.doSpawn();
            _bosses.put(spawnDat, raidboss);

            return raidboss;
        }
        else
            _bosses.put(spawnDat, null);
       return null;
    }
}
