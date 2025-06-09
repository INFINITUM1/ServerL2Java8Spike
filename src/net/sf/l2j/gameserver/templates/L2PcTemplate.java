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
package net.sf.l2j.gameserver.templates;

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;

/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class L2PcTemplate extends L2CharTemplate {

    /**
     * The Class object of the L2PcInstance
     */
    public final ClassId classId;
    public final Race race;
    public final String className;
    public final int classBaseLevel;
    public final int parentClassId;
    public final float lvlHpAdd;
    public final float lvlHpMod;
    public final float lvlCpAdd;
    public final float lvlCpMod;
    public final float lvlMpAdd;
    public final float lvlMpMod;
    private FastList<Integer> _items = new FastList<Integer>();
    private FastList<Location> _spawnPoints = new FastList<Location>();

    public L2PcTemplate(StatsSet set) {
        super(set);
        classId = ClassId.values()[set.getInteger("classId")];
        race = Race.values()[set.getInteger("raceId")];
        className = set.getString("className");

        classBaseLevel = set.getInteger("classBaseLevel");
        parentClassId = set.getInteger("parentClassId");
        lvlHpAdd = set.getFloat("lvlHpAdd");
        lvlHpMod = set.getFloat("lvlHpMod");
        lvlCpAdd = set.getFloat("lvlCpAdd");
        lvlCpMod = set.getFloat("lvlCpMod");
        lvlMpAdd = set.getFloat("lvlMpAdd");
        lvlMpMod = set.getFloat("lvlMpMod");
    }

    /**
     * add starter equipment
     *
     * @param i
     */
    public void addItem(int itemId) {
        L2Item item = ItemTable.getInstance().getTemplate(itemId);
        if (item != null) {
            _items.add(itemId);
        }
    }

    public void addSpawnPoint(Location loc) {
        _spawnPoints.add(loc);
    }

    /**
     *
     * @return itemIds of all the starter equipment
     */
    public FastList<Integer> getItems() {
        return _items;
    }

    public Location getRandomSpawnPoint() {
        return _spawnPoints.get(Rnd.get(_spawnPoints.size() - 1));
    }
}
