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
package net.sf.l2j.gameserver.model;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.entity.SpawnTerritory;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class manages the spawn and respawn of a group of L2NpcInstance that are in the same are and have the same type.
 *
 * <B><U> Concept</U> :</B><BR><BR>
 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
 *
 * @author Nightmare
 * @version $Revision: 1.9.2.3.2.8 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2Spawn {

    protected static final Logger _log = AbstractLogger.getLogger(L2Spawn.class.getName());
    /** The link on the L2NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
    private L2NpcTemplate _template;
    /** The Identifier of this spawn in the spawn table */
    private int _id;
    // private String _location = DEFAULT_LOCATION;
    /** The identifier of the location area where L2NpcInstance can be spwaned */
    private int _location;
    /** The maximum number of L2NpcInstance that can manage this L2Spawn */
    private int _maximumCount;
    /** The current number of L2NpcInstance managed by this L2Spawn */
    private int _currentCount;
    /** The current number of SpawnTask in progress or stand by of this L2Spawn */
    protected int _scheduledCount;
    /** The X position of the spwan point */
    private int _locX;
    /** The Y position of the spwan point */
    private int _locY;
    /** The Z position of the spwan point */
    private int _locZ;
    /** The heading of L2NpcInstance when they are spawned */
    private int _heading;
    /** The delay between a L2NpcInstance remove and its re-spawn */
    private int _respawnDelay;
    /** Minimum delay RaidBoss */
    private int _respawnMinDelay;
    /** Maximum delay RaidBoss */
    private int _respawnMaxDelay;
    /** The generic constructor of L2NpcInstance managed by this L2Spawn */
    private Constructor<?> _constructor;
    /** If True a L2NpcInstance is respawned each time that another is killed */
    private boolean _doRespawn;
    private L2NpcInstance _lastSpawn;
    private static ConcurrentLinkedQueue<SpawnListener> _spawnListeners = new ConcurrentLinkedQueue<SpawnListener>();

    /** The task launching the function doSpawn() */
    class SpawnTask implements Runnable {
        //L2NpcInstance _instance;
        //int _objId;

        private L2NpcInstance npc;

        public SpawnTask(/*int objid*/L2NpcInstance npc) {
            //_objId= objid;
            this.npc = npc;
        }

        @Override
        public void run() {
            try {
                //doSpawn();
                respawnNpc(npc);
            } catch (Exception e) {
                _log.log(Level.WARNING, "", e);
            }

            _scheduledCount--;
        }
    }

    /**
     * Constructor of L2Spawn.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...).
     * All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn.
     * Each template is loaded once in the server cache memory (reduce memory use).
     * When a new instance of L2Spawn is created, server just create a link between the instance and the template.
     * This link is stored in <B>_template</B><BR><BR>
     *
     * Each L2NpcInstance is linked to a L2Spawn that manages its spawn and respawn (delay, location...).
     * This link is stored in <B>_spawn</B> of the L2NpcInstance<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Set the _template of the L2Spawn </li>
     * <li>Calculate the implementationName used to generate the generic constructor of L2NpcInstance managed by this L2Spawn</li>
     * <li>Create the generic constructor of L2NpcInstance managed by this L2Spawn</li><BR><BR>
     *
     * @param mobTemplate The L2NpcTemplate to link to this L2Spawn
     *
     */
    public L2Spawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException {
        // Set the _template of the L2Spawn
        _template = mobTemplate;

        if (_template == null) {
            return;
        }

        // The Name of the L2NpcInstance type managed by this L2Spawn
        String implementationName = _template.type; // implementing class name

        if (mobTemplate.npcId == 30995) {
            implementationName = "L2RaceManager";
        }

        // if (mobTemplate.npcId == 8050)

        if ((mobTemplate.npcId >= 31046) && (mobTemplate.npcId <= 31053)) {
            implementationName = "L2SymbolMaker";
        }

        // Create the generic constructor of L2NpcInstance managed by this L2Spawn
        try {
            Class[] parameters = {int.class, Class.forName("net.sf.l2j.gameserver.templates.L2NpcTemplate")};
            _constructor = Class.forName("net.sf.l2j.gameserver.model.actor.instance." + implementationName + "Instance").getConstructor(parameters);
        } catch (ClassNotFoundException ex) {
            try {
                Class[] parameters = {int.class, Class.forName("net.sf.l2j.gameserver.templates.L2NpcTemplate")};
                _constructor = Class.forName("scripts.ai." + implementationName).getConstructor(parameters);
            } catch (ClassNotFoundException ex2) {
                _log.log(Level.WARNING, "Class not found", ex);
                _log.log(Level.WARNING, "Class not found", ex2);
            }
        }
    }

    /**
     * Return the maximum number of L2NpcInstance that this L2Spawn can manage.<BR><BR>
     */
    public int getAmount() {
        return _maximumCount;
    }

    /**
     * Return the Identifier of this L2Spwan (used as key in the SpawnTable).<BR><BR>
     */
    public int getId() {
        return _id;
    }

    /**
     * Return the Identifier of the location area where L2NpcInstance can be spwaned.<BR><BR>
     */
    public int getLocation() {
        return _location;
    }

    /**
     * Return the X position of the spwan point.<BR><BR>
     */
    public int getLocx() {
        return _locX;
    }

    /**
     * Return the Y position of the spwan point.<BR><BR>
     */
    public int getLocy() {
        return _locY;
    }

    /**
     * Return the Z position of the spwan point.<BR><BR>
     */
    public int getLocz() {
        return _locZ;
    }

    /**
     * Return the Itdentifier of the L2NpcInstance manage by this L2Spwan contained in the L2NpcTemplate.<BR><BR>
     */
    public int getNpcid() {
        return _template.npcId;
    }

    /**
     * Return the heading of L2NpcInstance when they are spawned.<BR><BR>
     */
    public int getHeading() {
        return _heading;
    }

    /**
     * Return the delay between a L2NpcInstance remove and its re-spawn.<BR><BR>
     */
    public int getRespawnDelay() {
        return _respawnDelay;
    }

    /**
     * Return Min RaidBoss Spawn delay.<BR><BR>
     */
    public int getRespawnMinDelay() {
        return _respawnMinDelay;
    }

    /**
     * Return Max RaidBoss Spawn delay.<BR><BR>
     */
    public int getRespawnMaxDelay() {
        return _respawnMaxDelay;
    }

    /**
     * Set the maximum number of L2NpcInstance that this L2Spawn can manage.<BR><BR>
     */
    public void setAmount(int amount) {
        _maximumCount = amount;
    }

    /**
     * Set the Identifier of this L2Spwan (used as key in the SpawnTable).<BR><BR>
     */
    public void setId(int id) {
        _id = id;
    }

    /**
     * Set the Identifier of the location area where L2NpcInstance can be spwaned.<BR><BR>
     */
    public void setLocation(int location) {
        _location = location;
    }

    /**
     * Set Minimum Respawn Delay.<BR><BR>
     */
    public void setRespawnMinDelay(int date) {
        _respawnMinDelay = date;
    }

    /**
     * Set Maximum Respawn Delay.<BR><BR>
     */
    public void setRespawnMaxDelay(int date) {
        _respawnMaxDelay = date;
    }

    /**
     * Set the X position of the spwan point.<BR><BR>
     */
    public void setLocx(int locx) {
        _locX = locx;
    }

    /**
     * Set the Y position of the spwan point.<BR><BR>
     */
    public void setLocy(int locy) {
        _locY = locy;
    }

    /**
     * Set the Z position of the spwan point.<BR><BR>
     */
    public void setLocz(int locz) {
        _locZ = locz;
    }

    /**
     * Set the heading of L2NpcInstance when they are spawned.<BR><BR>
     */
    public void setHeading(int heading) {
        _heading = heading;
    }

    /**
     * Decrease the current number of L2NpcInstance of this L2Spawn and if necessary create a SpawnTask to launch after the respawn Delay.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Decrease the current number of L2NpcInstance of this L2Spawn </li>
     * <li>Check if respawn is possible to prevent multiple respawning caused by lag </li>
     * <li>Update the current number of SpawnTask in progress or stand by of this L2Spawn </li>
     * <li>Create a new SpawnTask to launch after the respawn Delay </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</B></FONT><BR><BR>
     *
     */
    public void decreaseCount(/*int npcId*/L2NpcInstance oldNpc) {
        // Decrease the current number of L2NpcInstance of this L2Spawn
        _currentCount--;

        // Check if respawn is possible to prevent multiple respawning caused by lag
        if (_doRespawn && _scheduledCount + _currentCount < _maximumCount) {
            // Update the current number of SpawnTask in progress or stand by of this L2Spawn
            _scheduledCount++;

            // Create a new SpawnTask to launch after the respawn Delay
            //ClientScheduler.getInstance().scheduleLow(new SpawnTask(npcId), _respawnDelay);
            ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), _respawnDelay);
        }
    }

    /**
     * Create the initial spawning and set _doRespawn to True.<BR><BR>
     *
     * @return The number of L2NpcInstance that were spawned
     */
    public int init() {
        while (_currentCount < _maximumCount) {
            doSpawn();
        }
        _doRespawn = true;

        return _currentCount;
    }

    /**
     * Create a L2NpcInstance in this L2Spawn.<BR><BR>
     */
    public L2NpcInstance spawnOne() {
        return doSpawn();
    }

    /**
     * Set _doRespawn to False to stop respawn in thios L2Spawn.<BR><BR>
     */
    public void stopRespawn() {
        _doRespawn = false;
    }

    /**
     * Set _doRespawn to True to start or restart respawn in this L2Spawn.<BR><BR>
     */
    public void startRespawn() {
        _doRespawn = true;
    }

    /**
     * Create the L2NpcInstance, add it to the world and lauch its OnSpawn action.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
     * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
     *
     * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR><BR>
     * <li>Get L2NpcInstance Init parameters and its generate an Identifier </li>
     * <li>Call the constructor of the L2NpcInstance </li>
     * <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn </li>
     * <li>Set the position of the L2NpcInstance </li>
     * <li>Set the HP and MP of the L2NpcInstance to the max </li>
     * <li>Set the heading of the L2NpcInstance (random heading if not defined : value=-1) </li>
     * <li>Link the L2NpcInstance to this L2Spawn </li>
     * <li>Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world </li>
     * <li>Lauch the action OnSpawn fo the L2NpcInstance </li><BR><BR>
     * <li>Increase the current number of L2NpcInstance managed by this L2Spawn  </li><BR><BR>
     *
     */
    public L2NpcInstance doSpawn() {
        L2NpcInstance mob = null;
        try {
            // Check if the L2Spawn is not a L2Pet or L2Minion spawn
            if (_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion")) {
                _currentCount++;

                return mob;
            }

            // Get L2NpcInstance Init parameters and its generate an Identifier
            Object[] parameters = {IdFactory.getInstance().getNextId(), _template};

            // Call the constructor of the L2NpcInstance
            // (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance, L2BoxInstance,
            // L2FeedableBeastInstance, L2TamedBeastInstance, L2FolkInstance or L2TvTEventNpcInstance)
            Object tmp = _constructor.newInstance(parameters);

            // Check if the Instance is a L2NpcInstance
            if (!(tmp instanceof L2NpcInstance)) {
                return mob;
            }
            mob = (L2NpcInstance) tmp;
            return intializeNpcInstance(mob);
        } catch (Exception e) {
            _log.log(Level.WARNING, "NPC " + _template.npcId + " class not found", e);
        }
        return mob;
    }

    /**
     * @param mob
     * @return
     */
    private L2NpcInstance intializeNpcInstance(L2NpcInstance mob) {

        mob.stopAllEffects();

        // Set the HP and MP of the L2NpcInstance to the max
        mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());

        // Set the heading of the L2NpcInstance (random heading if not defined)
        mob.setHeading(getHeading());

        // Reset decay info
        mob.setDecayed(false);

        // Link the L2NpcInstance to this L2Spawn
        mob.setSpawn(this);

        // Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
        //mob.spawnMe(getLocx(), getLocy(), GeoData.getInstance().getSpawnHeight(getLocx(), getLocy(), getLocz(), getLocz()));
        //mob.spawnMe(getLocx(), getLocy(), mob.isL2Npc() ? getLocz() : GeoData.getInstance().getHeight(getLocx(), getLocy(), getLocz()));
        mob.spawnMe(getLocx(), getLocy(), GeoData.getInstance().getSpawnHeight(getLocx(), getLocy(), getLocz(), getLocz(), this));
        //mob.spawnMe(findSpawnLoc());

        //L2Spawn.notifyNpcSpawned(mob);

        _lastSpawn = mob;

        //if (Config.DEBUG)
        //   _log.finest("spawned Mob ID: "+_template.npcId+" ,at: "+mob.getX()+" x, "+mob.getY()+" y, "+mob.getZ()+" z");

        // Increase the current number of L2NpcInstance managed by this L2Spawn
        _currentCount++;
        return mob;
    }

    public Location findSpawnLoc() {
        int geo_x = _locX;
        int geo_y = _locY;
        int geo_z = GeoData.getInstance().getSpawnHeight(_locX, _locY, _locZ, _locZ, this);

        int good = 1;
        for (int i = 5; i >= -1; i--) {
            int posX = geo_x;
            int posY = geo_y;
            switch (Rnd.get(1, 6)) {
                case 1:
                    posX += 200;
                    posY += 340;
                    break;
                case 2:
                    posX += 310;
                    posY += 210;
                    break;
                case 3:
                    posX += 229;
                    posY -= 260;
                    break;
                case 4:
                    posX += 170;
                    posY -= 260;
                    break;
                case 5:
                    posX -= 310;
                    posY -= 180;
                    break;
                case 6:
                    posX -= 260;
                    posY += 220;
                    break;
            }
            int posZ = GeoData.getInstance().getSpawnHeight(posX, posY, geo_z, geo_z, this);
            if (GeoData.getInstance().canSeeTarget(geo_x, geo_y, geo_z, posX, posY, posZ)) {
                good = 2;
                break;
            }
            geo_x = posX;
            geo_y = posY;
            geo_z = posZ;
        }

        switch (good) {
            case 1:
                return findSpawnLoc();
            case 2:
                return new Location(_locX, _locY, GeoData.getInstance().getSpawnHeight(_locX, _locY, _locZ, _locZ, this));
        }
        /*if (good == 2) {
        return new Location(_locX, _locY, GeoData.getInstance().getSpawnHeight(_locX, _locY, _locZ, _locZ, this));
        }*/
        return new Location(geo_x, geo_y, geo_z);
    }

    public static void addSpawnListener(SpawnListener listener) {
        _spawnListeners.add(listener);
    }

    public static void removeSpawnListener(SpawnListener listener) {
        _spawnListeners.remove(listener);
    }

    public static void notifyNpcSpawned(L2NpcInstance npc) {
        for (SpawnListener listener : _spawnListeners) {
            listener.npcSpawned(npc);
        }
    }

    /**
     * @param i delay in seconds
     */
    public void setRespawnDelay(int i) {
        if (i < 0) {
            _log.warning("respawn delay is negative for spawnId:" + _id);
        }

        if (i < 10) {
            i = 10;
        }

        _respawnDelay = i * 1000;
    }

    public L2NpcInstance getLastSpawn() {
        return _lastSpawn;
    }

    /**
     * @param oldNpc
     */
    public void respawnNpc(L2NpcInstance oldNpc) {
        oldNpc.refreshID();
        L2NpcInstance instance = intializeNpcInstance(oldNpc);
        instance.setRunning();
    }

    public L2NpcTemplate getTemplate() {
        return _template;
    }
    /**
     * Респ по птс
     **/
    private long _lastKill = 0;
    private boolean _isFree = false;
    private SpawnTerritory _spawnTerr = null;

    public void setLastKill(long last) {
        _lastKill = last;
    }

    public long getLastKill() {
        return _lastKill;
    }

    public void setTerritory(SpawnTerritory terr) {
        _spawnTerr = terr;
    }

    public SpawnTerritory getTerritory() {
        return _spawnTerr;
    }

    public void setFree() {
        _isFree = true;
    }

    public boolean isFree() {
        return _isFree;
    }
}
