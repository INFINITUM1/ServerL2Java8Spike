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
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.TownManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.util.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;
import scripts.zone.type.L2ArenaZone;
import scripts.zone.type.L2ClanHallZone;

public class MapRegionTable {

    private static Logger _log = AbstractLogger.getLogger(MapRegionTable.class.getName());
    private static MapRegionTable _instance;
    private static final int JAIL = 27;
    private static final int COLISEUM = 77;
    private static final int ELF_TREE = 78;
    private static final int DERBY_TRACK = 79;
    private static final int DERBY_TRACKPVP = 792;
    private static final int PRIMIVAL_ISLE = 80;
    private final int[][] _regions = new int[19][21];
    private FastMap<Integer, FastList<Location>> _restartPoints = new FastMap<Integer, FastList<Location>>();

    public static enum TeleportWhereType {

        Castle,
        ClanHall,
        SiegeFlag,
        Town
    }

    public static MapRegionTable getInstance() {
        if (_instance == null) {
            _instance = new MapRegionTable();
        }
        return _instance;
    }

    private MapRegionTable() {
        int count2 = 0;

        //LineNumberReader lnr = null;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT region, sec0, sec1, sec2, sec3, sec4, sec5, sec6, sec7, sec8, sec9 FROM mapregion");
            rs = st.executeQuery();
            rs.setFetchSize(50);
            int region;
            while (rs.next()) {
                region = rs.getInt(1);

                for (int j = 0; j < 10; j++) {
                    _regions[j][region] = rs.getInt(j + 2);
                    count2++;
                    //_log.fine(j+","+region+" -> "+rs.getInt(j+2));
                }
            }
            //if (Config.DEBUG) _log.fine(count2 +" mapregion loaded");
        } catch (Exception e) {
            _log.warning("error while creating map region data: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        _restartPoints.clear();
        /*
        private static final int JAIL = 27;
        private static final int COLISEUM = 77;
        private static final int ELF_TREE = 78;
        private static final int DERBY_TRACK = 79;
        private static final int DERBY_TRACKPVP = 792;
        private static final int PRIMIVAL_ISLE = 80;
         */

        FastList<Location> points = new FastList<Location>();
        points.add(new Location(-114598, -249431, -2984));
        points.add(new Location(-114527, -249452, -2984));
        points.add(new Location(-114567, -249517, -2984));
        points.add(new Location(-114634, -249516, -2984));
        points.add(new Location(-114665, -249440, -2984));
        points.add(new Location(-114626, -249397, -2984));
        points.add(new Location(-114567, -249384, -2984));
        points.add(new Location(-114566, -249470, -2984));
        points.add(new Location(-114601, -249489, -2984));
        points.add(new Location(-114630, -249465, -2984));
        points.add(new Location(-114501, -249297, -2984));
        points.add(new Location(-114475, -249273, -2984));
        points.add(new Location(-114441, -249293, -2984));
        _restartPoints.put(JAIL, points);

        points = new FastList<Location>();
        points.add(new Location(147420, 46378, -3350));
        points.add(new Location(147696, 46416, -3350));
        points.add(new Location(147248, 47048, -3350));
        points.add(new Location(147449, 47051, -3350));
        points.add(new Location(147680, 47040, -3350));
        points.add(new Location(147232, 46352, -3350));
        points.add(new Location(147696, 46928, -3350));
        points.add(new Location(147456, 46928, -3350));
        points.add(new Location(147232, 46928, -3350));
        points.add(new Location(147232, 46464, -3350));
        points.add(new Location(147424, 46464, -3350));
        points.add(new Location(147696, 46512, -3350));
        points.add(new Location(151294, 46378, -3350));
        points.add(new Location(151535, 46353, -3350));
        points.add(new Location(151712, 46368, -3350));
        points.add(new Location(151753, 46500, -3405));
        points.add(new Location(151760, 46928, -3350));
        points.add(new Location(151760, 47104, -3350));
        points.add(new Location(151536, 46960, -3350));
        points.add(new Location(151536, 47104, -3350));
        points.add(new Location(151264, 47136, -3350));
        points.add(new Location(151264, 47008, -3350));
        points.add(new Location(151536, 46482, -3350));
        points.add(new Location(151296, 46480, -3350));
        _restartPoints.put(COLISEUM, points);

        points = new FastList<Location>();
        points.add(new Location(44907, 40587, -3508));
        points.add(new Location(45419, 41228, -3508));
        points.add(new Location(46240, 41492, -3508));
        points.add(new Location(46923, 41172, -3508));
        points.add(new Location(47291, 40496, -3515));
        _restartPoints.put(ELF_TREE, points);

        points = new FastList<Location>();
        points.add(new Location(11892, 181700, -3560));
        points.add(new Location(14028, 181695, -3560));
        points.add(new Location(13111, 181695, -3560));
        points.add(new Location(12587, 181702, -3560));
        points.add(new Location(13588, 181695, -3560));
        points.add(new Location(14077, 182490, -3560));
        points.add(new Location(13631, 182490, -3560));
        points.add(new Location(13157, 182490, -3560));
        points.add(new Location(12675, 182490, -3560));
        points.add(new Location(11926, 182503, -3560));
        points.add(new Location(12288, 182497, -3560));
        points.add(new Location(12292, 181707, -3560));
        points.add(new Location(11825, 181822, -3560));
        points.add(new Location(11827, 182334, -3560));
        points.add(new Location(11814, 181984, -3560));
        points.add(new Location(11824, 182172, -3560));
        _restartPoints.put(DERBY_TRACK, points);

        points = new FastList<Location>();
        points.add(new Location(11817, 182897, -3560));
        points.add(new Location(12438, 182887, -3560));
        points.add(new Location(13047, 182902, -3560));
        points.add(new Location(13061, 183518, -3560));
        points.add(new Location(13059, 184123, -3560));
        points.add(new Location(12430, 184133, -3560));
        points.add(new Location(11822, 184143, -3560));
        points.add(new Location(11803, 183473, -3560));
        points.add(new Location(12115, 182896, -3560));
        points.add(new Location(12740, 182901, -3560));
        points.add(new Location(13059, 183199, -3560));
        points.add(new Location(13049, 182796, -3560));
        points.add(new Location(12740, 184138, -3560));
        points.add(new Location(12172, 184133, -3560));
        points.add(new Location(11820, 183806, -3560));
        points.add(new Location(11811, 183209, -3560));
        _restartPoints.put(DERBY_TRACKPVP, points);

        points = new FastList<Location>();
        points.add(new Location(-16554, 109382, -1799));
        points.add(new Location(-16869, 109375, -1799));
        points.add(new Location(-16659, 109261, -1799));
        points.add(new Location(-16618, 109485, -1799));
        _restartPoints.put(101, points);

        points = new FastList<Location>();
        points.add(new Location(20514, 160368, -1993));
        points.add(new Location(20829, 160375, -1993));
        points.add(new Location(20619, 160489, -1993));
        points.add(new Location(20578, 160265, -1993));
        _restartPoints.put(102, points);

        points = new FastList<Location>();
        points.add(new Location(116540, 146655, -1866));
        points.add(new Location(116547, 146340, -1866));
        points.add(new Location(116661, 146550, -1866));
        points.add(new Location(116437, 146591, -1866));
        _restartPoints.put(103, points);

        points = new FastList<Location>();
        points.add(new Location(82616, 38750, -1593));
        points.add(new Location(82623, 38435, -1593));
        points.add(new Location(82737, 38645, -1593));
        points.add(new Location(82513, 38686, -1593));
        _restartPoints.put(104, points);

        points = new FastList<Location>();
        points.add(new Location(147700, 4608, -2784));
        points.add(new Location(147705, 4865, -2784));
        points.add(new Location(147200, 4865, -2784));
        points.add(new Location(147705, 4350, -2784));
        points.add(new Location(147705, 4350, -2784));
        _restartPoints.put(105, points);

        points = new FastList<Location>();
        points.add(new Location(114465, 249147, -89));
        points.add(new Location(114780, 249154, -89));
        points.add(new Location(114570, 249268, -89));
        points.add(new Location(114529, 249044, -89));
        _restartPoints.put(106, points);

        points = new FastList<Location>();
        points.add(new Location(147408, -46448, -963));
        points.add(new Location(147520, -46432, -963));
        points.add(new Location(147408, -46896, -963));
        points.add(new Location(147536, -46896, -963));
        _restartPoints.put(107, points);

        points = new FastList<Location>();
        points.add(new Location(11224, -49176, 3864));
        points.add(new Location(11048, -49256, 3864));
        points.add(new Location(11048, -49080, 3864));
        _restartPoints.put(108, points);

        points = new FastList<Location>();
        points.add(new Location(77641, -150494, 776));
        points.add(new Location(77460, -150505, 776));
        points.add(new Location(77467, -150854, 776));
        points.add(new Location(77640, -150869, 776));
        _restartPoints.put(109, points);
    }

    public final int getMapRegion(int posX, int posY) {
        return _regions[getMapRegionX(posX)][getMapRegionY(posY)];
    }

    public final int getMapRegionX(int posX) {
        return (posX >> 15) + 4;// + centerTileX;
    }

    public final int getMapRegionY(int posY) {
        return (posY >> 15) + 10;// + centerTileX;
    }

    public int getAreaCastle(L2Character activeChar) {
        int area = getClosestTownNumber(activeChar);
        int castle;
        switch (area) {
            case 0:
                castle = 1;
                break;//Talking Island Village
            case 1:
                castle = 4;
                break; //Elven Village
            case 2:
                castle = 4;
                break; //Dark Elven Village
            case 3:
                castle = 9;
                break; //Orc Village
            case 4:
                castle = 9;
                break; //Dwarven Village
            case 5:
                castle = 1;
                break; //Town of Gludio
            case 6:
                castle = 1;
                break; //Gludin Village
            case 7:
                castle = 2;
                break; //Town of Dion
            case 8:
                castle = 3;
                break; //Town of Giran
            case 9:
                castle = 4;
                break; //Town of Oren
            case 10:
                castle = 5;
                break; //Town of Aden
            case 11:
                castle = 5;
                break; //Hunters Village
            case 12:
                castle = 3;
                break; //Giran Harbor
            case 13:
                castle = 6;
                break; //Heine
            case 14:
                castle = 8;
                break; //Rune Township
            case 15:
                castle = 7;
                break; //Town of Goddard
            case 16:
                castle = 9;
                break; //Town of Shuttgart
            case 17:
                castle = 4;
                break; //Ivory Tower
            case 18:
                castle = 8;
                break; //Primeval Isle Wharf
            default:
                castle = 5;
                break; //Town of Aden
        }
        return castle;
    }

    public int getClosestTownNumber(L2Character activeChar) {
        return getMapRegion(activeChar.getX(), activeChar.getY());
    }

    public String getClosestTownName(L2Character activeChar) {
        int nearestTownId = getMapRegion(activeChar.getX(), activeChar.getY());
        String nearestTown;

        switch (nearestTownId) {
            case 0:
                nearestTown = "Talking Island Village";
                break;
            case 1:
                nearestTown = "Elven Village";
                break;
            case 2:
                nearestTown = "Dark Elven Village";
                break;
            case 3:
                nearestTown = "Orc Village";
                break;
            case 4:
                nearestTown = "Dwarven Village";
                break;
            case 5:
                nearestTown = "Town of Gludio";
                break;
            case 6:
                nearestTown = "Gludin Village";
                break;
            case 7:
                nearestTown = "Town of Dion";
                break;
            case 8:
                nearestTown = "Town of Giran";
                break;
            case 9:
                nearestTown = "Town of Oren";
                break;
            case 10:
                nearestTown = "Town of Aden";
                break;
            case 11:
                nearestTown = "Hunters Village";
                break;
            case 12:
                nearestTown = "Giran Harbor";
                break;
            case 13:
                nearestTown = "Heine";
                break;
            case 14:
                nearestTown = "Rune Township";
                break;
            case 15:
                nearestTown = "Town of Goddard";
                break;
            case 16:
                nearestTown = "Town of Shuttgart";
                break;  ////TODO@ (Check mapregion table)[Luno]
            case 18:
                nearestTown = "Primeval Isle";
                break;
            default:
                nearestTown = "Town of Aden";
                break;
        }
        return nearestTown;
    }

    public Location getTeleToLocation(L2Character activeChar, TeleportWhereType teleportWhere) {
        int[] coord;
        if (activeChar.isPlayer()) {
            L2PcInstance player = activeChar.getPlayer();

            if (player.isInJail()) {
                return getRestartPoint(JAIL);
            }

            if (player.getClan() != null) {
                Castle castle = null;
                ClanHall clanhall = null;
                // If teleport to clan hall
                if (teleportWhere == TeleportWhereType.ClanHall) {

                    clanhall = ClanHallManager.getInstance().getClanHallByOwner(player.getClan());
                    if (clanhall != null) {
                        L2ClanHallZone zone = clanhall.getZone();
                        if (zone != null) {
                            return zone.getSpawn();
                        }
                    }
                }

                // If teleport to castle
                if (teleportWhere == TeleportWhereType.Castle) {
                    castle = CastleManager.getInstance().getCastleByOwner(player.getClan());
                }

                // Check if player is on castle ground
                if (castle == null) {
                    castle = CastleManager.getInstance().getCastle(player);
                }

                if (castle != null && castle.getCastleId() > 0) {
                    if (castle.getSiege().getIsInProgress()) {
                        if (castle.getSiege().checkIsAttacker(player.getClan()) && castle.getSiege().getFlag(player.getClan()) != null) {
                            L2NpcInstance flag = castle.getSiege().getFlag(player.getClan());
                            return new Location(flag.getX(), flag.getY(), flag.getZ());
                        }
                        if (castle.getSiege().checkIsDefender(player.getClan())) {
                            switch (castle.getCastleId()) {
                                case 1: //gludio
                                    return getRestartPoint(101);
                                case 2: //dion
                                    return getRestartPoint(102);
                                case 3: //giran
                                    return getRestartPoint(103);
                                case 4: //oren
                                    return getRestartPoint(104);
                                case 5: //aden
                                    return getRestartPoint(105);
                                case 6: //inadrill
                                    return getRestartPoint(106);
                                case 7: //godard
                                    return getRestartPoint(107);
                                case 8: //rune
                                    return getRestartPoint(108);
                                case 9: //shutgart
                                    return getRestartPoint(109);
                            }
                        }
                        if (player.getKarma() > 1) {
                            if (getMapRegion(activeChar.getX(), activeChar.getY()) >= 0) {
                                return TownManager.getInstance().getClosestTown(activeChar).getSpawnLocPk();
                            }

                            return TownManager.getTown(16).getSpawnLocPk();
                        }
                        return TownManager.getInstance().getClosestTown(activeChar).getSpawnLoc();
                    }
                    // If Teleporting to castle or
                    // If is on caslte with siege and player's clan is defender
                    if (teleportWhere == TeleportWhereType.Castle || (teleportWhere == TeleportWhereType.Castle && castle.getSiege().getIsInProgress() && castle.getSiege().getDefenderClan(player.getClan()) != null)) {
                        coord = castle.getZone().getSpawn();
                        return new Location(coord[0], coord[1], coord[2]);
                    }

                    /*if (teleportWhere == TeleportWhereType.SiegeFlag && castle.getSiege().getIsInProgress())
                    {
                    // Check if player's clan is attacker
                    L2NpcInstance flag = castle.getSiege().getFlag(player.getClan());
                    if (flag != null )
                    return new Location(flag.getX(), flag.getY(), flag.getZ());
                    }*/
                }
            }

            if (player.isInColiseum()) {
                return getRestartPoint(COLISEUM);
            }

            if (player.isInElfTree()) {
                return getRestartPoint(ELF_TREE);
            }

            if (player.isInDerbyTrack()) {
                if (player.isInsidePvpZone()) {
                    return getRestartPoint(DERBY_TRACKPVP);
                }
                return getRestartPoint(DERBY_TRACK);
            }

            //Karma player land out of city
            if (player.getKarma() > 1) {
                int closest = getMapRegion(activeChar.getX(), activeChar.getY());
                if (closest >= 0) {
                    return TownManager.getInstance().getClosestTown(activeChar).getSpawnLocPk();
                }

                return TownManager.getTown(16).getSpawnLocPk();
            }

            if (player.isInDino()) {
                return TownManager.getTown(18).getSpawnLoc();
            }

            // Checking if in arena
            L2ArenaZone arena = ZoneManager.getInstance().getArena(player);
            if (arena != null) {
                coord = arena.getSpawnLoc();
                return new Location(coord[0], coord[1], coord[2]);
            }
        }
        return TownManager.getInstance().getClosestTown(activeChar).getSpawnLoc();
    }

    public Location getRestartPoint(int id) {
        FastList<Location> points = _restartPoints.get(id);
        if (points != null) {
            return points.get(Rnd.get(points.size() - 1));
        }

        return TownManager.getTown(16).getSpawnLocPk();
    }
}