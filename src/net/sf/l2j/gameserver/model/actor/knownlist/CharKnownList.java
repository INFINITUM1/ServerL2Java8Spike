/* This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.model.actor.knownlist;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.Util;

public class CharKnownList extends ObjectKnownList {
    // =========================================================
    // Data Field

    private L2Character activeChar;
    private Map<Integer, Integer> _knownRelations = new ConcurrentHashMap<Integer, Integer>();
    private Map<Integer, L2PcInstance> _knownPlayers = new ConcurrentHashMap<Integer, L2PcInstance>();

    // =========================================================
    // Constructor
    public CharKnownList(L2Character activeChar) {
        super(activeChar);
        this.activeChar = activeChar;
    }

    // =========================================================
    // Method - Public
    @Override
    public boolean addKnownObject(L2Object object) {
        return addKnownObject(object, null);
    }

    @Override
    public boolean addKnownObject(L2Object object, L2Character dropper) {
        if (!super.addKnownObject(object, dropper)) {
            return false;
        }

        if (object.isPlayer()) {
            _knownPlayers.put(object.getObjectId(), object.getPlayer());
            _knownRelations.put(object.getObjectId(), -1);
        }
        return true;
    }

    /**
     * Return True if the L2PcInstance is in _knownPlayer of the L2Character.<BR><BR>
     * @param player The L2PcInstance to search in _knownPlayer
     */
    public final boolean knowsThePlayer(L2PcInstance player) {
        return activeChar.equals(player) || _knownPlayers.containsKey(player.getObjectId());
    }

    /** Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI. */
    @Override
    public final void removeAllKnownObjects() {
        super.removeAllKnownObjects();
        _knownPlayers.clear();
        _knownRelations.clear();

        // Set _target of the L2Character to null
        // Cancel Attack or Cast
        activeChar.setTarget(null);

        // Cancel AI Task
        if (activeChar.hasAI()) {
            activeChar.setAI(null);
        }
    }

    @Override
    public boolean removeKnownObject(L2Object object) {
        if (!super.removeKnownObject(object)) {
            return false;
        }
        if (object.isPlayer()) {
            _knownPlayers.remove(object.getObjectId());
            _knownRelations.remove(object.getObjectId());
        }
        // If object is targeted by the L2Character, cancel Attack or Cast
        if (object.equals(activeChar.getTarget())) {
            activeChar.setTarget(null);
        }

        return true;
    }

    // =========================================================
    // Method - Private
    // =========================================================
    // Property - Public
    public L2Character getActiveChar() {
        return activeChar;
    }

    @Override
    public int getDistanceToForgetObject(L2Object object) {
        return 0;
    }

    @Override
    public int getDistanceToWatchObject(L2Object object) {
        return 0;
    }

    public FastList<L2Character> getKnownCharacters() {
        return getCharactersList(new FastList<L2Character>());
    }

    public FastList<L2Character> getKnownCharactersInRadius(int radius) {
        return getCharactersListInRadius(new FastList<L2Character>(), radius);
    }

    private FastList<L2Character> getCharactersList(FastList<L2Character> result) {
        for (L2Object obj : getKnownObjects().values()) {
            if (obj != null && obj.isL2Character()) {
                result.add(obj.getL2Character());
            }
        }
        return result;
    }

    private FastList<L2Character> getCharactersListInRadius(FastList<L2Character> result, int radius) {
        for (L2Object obj : getKnownObjects().values()) {
            if (obj != null && obj.isL2Character()) {
                if (Util.checkIfInRange(radius, activeChar, obj, true)) {
                    result.add(obj.getL2Character());
                }
            }
        }
        return result;
    }

    public final Map<Integer, L2PcInstance> getKnownPlayers() {
        //if (_knownPlayers == null) _knownPlayers = new ConcurrentHashMap<Integer, L2PcInstance>();
        //if (_knownPlayers == null) _knownPlayers = new FastMap<Integer, L2PcInstance>().shared("CharKnownList._knownPlayers");
        return _knownPlayers;
    }

    public final Map<Integer, Integer> getKnownRelations() {
        //if (_knownRelations == null) _knownRelations = new ConcurrentHashMap<Integer, Integer>();
        //if (_knownRelations == null) _knownRelations = new FastMap<Integer, Integer>().shared("CharKnownList._knownRelations");
        return _knownRelations;
    }

    public final FastList<L2PcInstance> getListKnownPlayers() {
        return getPlayersList(new FastList<L2PcInstance>());
    }

    public final FastList<L2PcInstance> getKnownPlayersInRadius(int radius) {
        return getPlayersListInRadius(new FastList<L2PcInstance>(), radius);
    }

    private FastList<L2PcInstance> getPlayersList(FastList<L2PcInstance> result) {
        for (L2PcInstance player : _knownPlayers.values()) {
            if (player.isInOfflineMode()/* || player.isFantome()*/) {
                continue;
            }
            result.add(player);
        }
        return result;
    }

    private FastList<L2PcInstance> getPlayersListInRadius(FastList<L2PcInstance> result, int radius) {
        for (L2PcInstance player : _knownPlayers.values()) {
            if (player.isInOfflineMode()/* || player.isFantome()*/) {
                continue;
            }
            if (Util.checkIfInRange(radius, activeChar, player, true)) {
                result.add(player);
            }
        }
        return result;
    }

    public boolean existsDoorsInRadius(int radius) {
        for (L2Object obj : getKnownObjects().values()) {
            if (obj.isL2Door()) {
                //System.out.println("!@#!@#");
                if (Util.checkIfInRange(radius, activeChar, obj, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean updateRelationsFor(int objId, int idx) {
        /*if (_knownRelations == null)
        {
        _knownRelations = new ConcurrentHashMap<Integer, Integer>();
        return false;
        }*/

        Integer relation = _knownRelations.get(objId);
        if (relation == null) {
            return false;
        }

        return (relation != idx);
    }

    public void gc() {
        _knownPlayers.clear();
        _knownRelations.clear();
    }
}
