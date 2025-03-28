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
package net.sf.l2j.gameserver.instancemanager;

import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.entity.Castle;
import scripts.zone.L2ZoneType;
import scripts.zone.type.L2TownZone;
import net.sf.l2j.util.log.AbstractLogger;

public class TownManager {

    private static final Logger _log = AbstractLogger.getLogger(TownManager.class.getName());
    // =========================================================
    private static TownManager _instance;

    public static final TownManager getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new TownManager();
        _log.info("TownManager: Loaded.");
    }
    // =========================================================
    // =========================================================
    // Data Field
    private FastList<L2TownZone> _towns;

    // =========================================================
    // Constructor
    public TownManager() {
    }

    // =========================================================
    // Property - Public

    /*public void addTown(L2TownZone arena)
    {
    if (_towns == null)
    _towns = new FastList<L2TownZone>();
    
    _towns.add(arena);
    }*/
    public final L2TownZone getClosestTown(L2Object activeObject) {
        switch (MapRegionTable.getInstance().getMapRegion(activeObject.getPosition().getX(), activeObject.getPosition().getY())) {
            case 0:
                return getTown(2); // TI
            case 1:
                return getTown(3); // Elven
            case 2:
                return getTown(1); // DE
            case 3:
                return getTown(4); // Orc
            case 4:
                return getTown(6); // Dwarven
            case 5:
                return getTown(7); // Gludio
            case 6:
                return getTown(5); // Gludin
            case 7:
                return getTown(8); // Dion
            case 8:
                return getTown(9); // Giran
            case 9:
                return getTown(10); // Oren
            case 10:
                return getTown(12); // Aden
            case 11:
                return getTown(11); // HV
            case 12:
                return getTown(9); // Giran Harbour
            case 13:
                return getTown(15); // Heine
            case 14:
                return getTown(14); // Rune
            case 15:
                return getTown(13); // Goddard
            case 16:
                return getTown(17); // Schuttgart
            case 17:
                return getTown(16); // Floran
            case 18:
                return getTown(18); //Primeval Isle
        }

        return getTown(16); // Default to floran
    }

    public final int getClosestLocation(L2Object activeObject) {
        switch (MapRegionTable.getInstance().getMapRegion(activeObject.getPosition().getX(), activeObject.getPosition().getY())) {
            case 0:
                return 1; // TI
            case 1:
                return 4; // Elven
            case 2:
                return 3; // DE
            case 3:
                return 9; // Orc
            case 4:
                return 9; // Dwarven
            case 5:
                return 2; // Gludio
            case 6:
                return 2; // Gludin
            case 7:
                return 5; // Dion
            case 8:
                return 6; // Giran
            case 9:
                return 10; // Oren
            case 10:
                return 13; // Aden
            case 11:
                return 11; // HV
            case 12:
                return 6; // Giran Harbour
            case 13:
                return 12; // Heine
            case 14:
                return 14; // Rune
            case 15:
                return 15; // Goddard
            case 16:
                return 9; // Schuttgart
        }
        return -1;
    }

    public final boolean townHasCastleInSiege(int townId) {
        //int[] castleidarray = {0,0,0,0,0,0,0,1,2,3,4,0,5,0,0,6,0};
        int[] castleidarray = {0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 0, 5, 7, 8, 6, 0, 9};
        int castleIndex = castleidarray[townId];

        if (castleIndex > 0) {
            Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
            if (castle != null) {
                return castle.getSiege().getIsInProgress();
            }
        }
        return false;
    }

    public final boolean townHasCastleInSiege(int x, int y) {
        int curtown = (MapRegionTable.getInstance().getMapRegion(x, y));
        //int[] castleidarray = {0,0,0,0,0,1,0,2,3,4,5,0,0,6,0,0,0,0};
        int[] castleidarray = {0, 0, 0, 0, 0, 1, 0, 2, 3, 4, 5, 0, 0, 6, 8, 7, 9, 0};
        //find an instance of the castle for this town.
        int castleIndex = castleidarray[curtown];
        if (castleIndex > 0) {
            Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
            if (castle != null) {
                return castle.getSiege().getIsInProgress();
            }
        }
        return false;
    }

    public static L2TownZone getTown(int townId) {
        for (L2ZoneType temp : ZoneManager.getInstance().getSafeZones()) {
            if (temp == null || !temp.isTownZone()) {
                continue;
            }
            if (temp.getTownId() == townId) {
                return temp.getTownZone();
            }
        }
        return null;
    }

    /**
     * Returns the town at that position (if any)
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static L2TownZone getTown(int x, int y, int z) {
        for (L2ZoneType temp : ZoneManager.getInstance().getSafeZones()) {
            if (temp == null || !temp.isTownZone()) {
                continue;
            }
            if (temp.isInsideZone(x, y, z)) {
                return temp.getTownZone();
            }
        }
        return null;
    }
}
