package scripts.zone;

import java.util.List;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.type.L2ArenaZone;
import scripts.zone.type.L2TownZone;

/**
 * Abstract base class for any zone type
 * Handles basic operations
 *
 * @author  durgus
 */
public abstract class L2ZoneType {

    private final int _id;
    protected List<L2ZoneForm> _zone;
    protected FastMap<Integer, L2Character> _characterList;
    protected FastMap<Integer, Integer> _zones;
    /** Parameters to affect specific characters */
    private boolean _checkAffected;
    private int _minLvl;
    private int _maxLvl;
    private int[] _race;
    private int[] _class;
    private char _classType;

    protected L2ZoneType(int id) {
        _id = id;
        _characterList = new FastMap<Integer, L2Character>().shared();
        _zones = new FastMap<Integer, Integer>().shared();

        _checkAffected = false;

        _minLvl = 0;
        _maxLvl = 0xFF;

        _classType = 0;

        _race = null;
        _class = null;
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        return _id;
    }

    /**
     * Setup new parameters for this zone
     * @param type
     * @param value
     */
    public void setParameter(String name, String value) {
        _checkAffected = true;

        // Minimum level
        if (name.equals("affectedLvlMin")) {
            _minLvl = Integer.parseInt(value);
        } // Maximum level
        else if (name.equals("affectedLvlMax")) {
            _maxLvl = Integer.parseInt(value);
        } // Affected Races
        else if (name.equals("affectedRace")) {
            // Create a new array holding the affected race
            if (_race == null) {
                _race = new int[1];
                _race[0] = Integer.parseInt(value);
            } else {
                int[] temp = new int[_race.length + 1];

                int i = 0;
                for (; i < _race.length; i++) {
                    temp[i] = _race[i];
                }

                temp[i] = Integer.parseInt(value);

                _race = temp;
            }
        } // Affected classes
        else if (name.equals("affectedClassId")) {
            // Create a new array holding the affected classIds
            if (_class == null) {
                _class = new int[1];
                _class[0] = Integer.parseInt(value);
            } else {
                int[] temp = new int[_class.length + 1];

                int i = 0;
                for (; i < _class.length; i++) {
                    temp[i] = _class[i];
                }

                temp[i] = Integer.parseInt(value);

                _class = temp;
            }
        } // Affected class type
        else if (name.equals("affectedClassType")) {
            if (value.equals("Fighter")) {
                _classType = 1;
            } else {
                _classType = 2;
            }
        }
    }

    /**
     * Checks if the given character is affected by this zone
     * @param character
     * @return
     */
    private boolean isAffected(L2Character character) {
        // Check lvl
        if (character.getLevel() < _minLvl || character.getLevel() > _maxLvl) {
            return false;
        }

        if (character.isPlayer()) {
            // Check class type
            if (_classType != 0) {
                if (((L2PcInstance) character).isMageClass()) {
                    if (_classType == 1) {
                        return false;
                    }
                } else if (_classType == 2) {
                    return false;
                }
            }

            // Check race
            if (_race != null) {
                boolean ok = false;

                for (int i = 0; i < _race.length; i++) {
                    if (((L2PcInstance) character).getRace().ordinal() == _race[i]) {
                        ok = true;
                        break;
                    }
                }

                if (!ok) {
                    return false;
                }
            }

            // Check class
            if (_class != null) {
                boolean ok = false;

                for (int i = 0; i < _class.length; i++) {
                    if (((L2PcInstance) character).getClassId().ordinal() == _class[i]) {
                        ok = true;
                        break;
                    }
                }

                if (!ok) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Set the zone for this L2ZoneType Instance
     * @param zone
     */
    public void setZone(L2ZoneForm zone) {
        getZones().add(zone);
    }

    /**
     * Returns this zones zone form
     * @param zone
     * @return
     */
    public L2ZoneForm getZone() {
        for (L2ZoneForm zone : getZones()) {
            return zone;
        }
        return null;
    }

    public final List<L2ZoneForm> getZones() {
        if (_zone == null) {
            _zone = new FastList<L2ZoneForm>();
        }
        return _zone;
    }

    /**
     * Checks if the given coordinates are within zone's plane
     * @param x
     * @param y
     */
    public boolean isInsideZone(int x, int y) {
        for (L2ZoneForm zone : getZones()) {
            if (zone.isInsideZone(x, y, zone.getHighZ())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given coordinates are within the zone
     * @param x
     * @param y
     * @param z
     */
    public boolean isInsideZone(int x, int y, int z) {
        for (L2ZoneForm zone : getZones()) {
            if (zone.isInsideZone(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given object is inside the zone.
     *
     * @param object
     */
    public boolean isInsideZone(L2Object object) {
        return isInsideZone(object.getX(), object.getY(), object.getZ());
    }

    public double getDistanceToZone(int x, int y) {
        return getZone().getDistanceToZone(x, y);
    }

    public double getDistanceToZone(L2Object object) {
        return getZone().getDistanceToZone(object.getX(), object.getY());
    }

    public void revalidateInZone(L2Character character) {
        // If the character can't be affected by this zone return
        if (_checkAffected) {
            if (!isAffected(character)) {
                return;
            }
        }

        // If the object is inside the zone...
        if (isInsideZone(character.getX(), character.getY(), character.getZ())) {
            // Was the character not yet inside this zone?
            if (!_characterList.containsKey(character.getObjectId())) {
                _characterList.put(character.getObjectId(), character);
                onEnter(character);
            }
        } else {
            // Was the character inside this zone?
            if (_characterList.containsKey(character.getObjectId())) {
                _characterList.remove(character.getObjectId());
                onExit(character);
            }
        }
    }

    /**
     * Force fully removes a character from the zone
     * Should use during teleport / logoff
     * @param character
     */
    public void removeCharacter(L2Character character) {
        if (_characterList.containsKey(character.getObjectId())) {
            _characterList.remove(character.getObjectId());
            onExit(character);
        }
    }

    /**
     * Will scan the zones char list for the character
     * @param character
     * @return
     */
    public boolean isCharacterInZone(L2Character character) {
        return _characterList.containsKey(character.getObjectId());
    }

    protected abstract void onEnter(L2Character character);

    protected abstract void onExit(L2Character character);

    protected abstract void onDieInside(L2Character character);

    protected abstract void onReviveInside(L2Character character);

    public FastMap<Integer, L2Character> getCharactersInside() {
        return _characterList;
    }

    // уменьшаем затраты на каст ((L2ZoneType)zone).
    public boolean isPvP(final int x, final int y) {
        return false;
    }

    public boolean isInsideTradeZone(final int x, final int y) {
        return true;
    }

    public boolean isArena() {
        return false;
    }

    public boolean isTownZone() {
        return false;
    }

    public boolean isArenaZone() {
        return false;
    }

    public int getTownId() {
        return -1;
    }

    public L2TownZone getTownZone() {
        return null;
    }

    public L2ArenaZone getArenaZone() {
        return null;
    }

    public int getWaterZ() {
        return 0;
    }

    public void givePvpRewards(L2PcInstance player) {
    }
}
