/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.instancemanager;

import cib.util.geo.Geo2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
//import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.WorldRegionTable;
import net.sf.l2j.gameserver.datatables.WorldRegionTable.Region;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Log;
import scripts.zone.L2ZoneType;
import scripts.zone.form.*;
import scripts.zone.type.*;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

//import javolution.util.FastTable;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.text.TextBuilder;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.model.entity.EventTerritory;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.log.AbstractLogger;

public class ZoneManager {

    private static final Logger _log = AbstractLogger.getLogger(ZoneManager.class.getName());
    // =========================================================
    private static ZoneManager _instance;
    //private final ArrayList<L2ZoneType> _zones = new ArrayList<L2ZoneType>();
    //private final ArrayList<L2ZoneType> _safeZones = new ArrayList<L2ZoneType>();
    private static final FastList<L2ZoneType> _zones = new FastList<>();
    private static final FastList<L2ZoneType> _safeZones = new FastList<>();
    private static final FastList<L2WaterZone> _waterZones = new FastList<>();
    private static final FastList<L2ZoneType> _fishZones = new FastList<>();
    private static final FastList<L2ArenaZone> _arenaZones = new FastList<>();
    private static final FastList<L2PvpFarmZone> _rewardPvpZones = new FastList<>();
    private int _update = 0;

    public static ZoneManager getInstance() {
        return _instance;
    }

    public static void init() {
        //_log.info("Loading zones...");
        _instance = new ZoneManager();
        _instance.load();
    }

    // =========================================================
    // Data Field
    // =========================================================
    // Constructor
    public ZoneManager() {
        //load();
    }

    public void reload() {
        // int zoneCount = 0;

        // Get the world regions
        int count = 0;
        L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
        for (int x = 0; x < worldRegions.length; x++) {
            for (int y = 0; y < worldRegions[x].length; y++) {
                worldRegions[x][y].getZones().clear();
                count++;
            }
        }
        GrandBossManager.getInstance().getZones().clear();
        _log.info("ZoneManager: Removed zones in " + count + " regions.");
        // Load the zones
        load();
    }

    // =========================================================
    // Method - Private
    private void load() {
        long start = System.currentTimeMillis();

        int zoneCount = 0;
        _zones.clear();
        _safeZones.clear();

        // Get the world regions
        L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
        FastMap<Integer, FastList<Region>> _regions = WorldRegionTable.getInstance().getRegions();

        // Load the zone xml
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            // Get a sql connection here
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);

            File file = new File(Config.DATAPACK_ROOT + "/data/zones/zone.xml");
            if (!file.exists()) {
                _log.info("ZoneManager [ERROR]: /data/zones/zone.xml file is missing.");
                return;
            }

            Document doc = factory.newDocumentBuilder().parse(file);

            outer:
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("zone".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            int zoneId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            int minZ = Integer.parseInt(attrs.getNamedItem("minZ").getNodeValue());
                            int maxZ = Integer.parseInt(attrs.getNamedItem("maxZ").getNodeValue());
                            String zoneType = attrs.getNamedItem("type").getNodeValue();
                            String zoneShape = attrs.getNamedItem("shape").getNodeValue();

                            if (zoneType.equals("Water") || zoneType.equals("Fishing")) {
                                minZ -= 20;
                                //maxZ += 10;
                            }

                            // Create the zone
                            L2ZoneType temp = getZoneByType(zoneType, zoneId);
                            // Check for unknown type
                            if (temp == null) {
                                _log.warning("ZoneManager [ERROR]: No such zone type: " + zoneType);
                                continue;
                            }

                            // Get the zone shape from sql
                            try {
                                // Set the correct query
                                st = con.prepareStatement("SELECT x,y FROM zone_vertices WHERE id=? ORDER BY 'order' ASC ", ResultSet.FETCH_FORWARD, ResultSet.CONCUR_READ_ONLY);
                                st.setInt(1, zoneId);
                                st.setFetchSize(30);
                                rs = st.executeQuery();
                                rs.setFetchSize(50);
                                // Create this zone. Parsing for cuboids is a
                                // bit different than for other polygons
                                // cuboids need exactly 2 points to be defined.
                                // Other polygons need at least 3 (one per
                                // vertex)
                                if (zoneShape.equalsIgnoreCase("Cuboid")) {
                                    int[] x = {0, 0};
                                    int[] y = {0, 0};
                                    boolean successfulLoad = true;

                                    for (int i = 0; i < 2; i++) {
                                        if (rs.next()) {
                                            x[i] = rs.getInt("x");
                                            y[i] = rs.getInt("y");
                                        } else {
                                            _log.warning("ZoneManager: Missing cuboid vertex in sql data for zone: " + zoneId);
                                            Close.SR(st, rs);
                                            successfulLoad = false;
                                            break;
                                        }
                                    }

                                    if (successfulLoad) {
                                        temp.setZone(new ZoneCuboid(x[0], x[1], y[0], y[1], minZ, maxZ));
                                    } else {
                                        continue;
                                    }
                                } else if (zoneShape.equalsIgnoreCase("NPoly")) {
                                    FastList<Integer> fl_x = new FastList<Integer>(), fl_y = new FastList<Integer>();
                                    // Load the rest
                                    while (rs.next()) {
                                        fl_x.add(rs.getInt("x"));
                                        fl_y.add(rs.getInt("y"));
                                    }

                                    // An nPoly needs to have at least 3
                                    // vertices
                                    if ((fl_x.size() == fl_y.size()) && (fl_x.size() > 2)) {
                                        // Create arrays
                                        int[] aX = new int[fl_x.size()];
                                        int[] aY = new int[fl_y.size()];

                                        // This runs only at server startup so
                                        // dont complain :>
                                        for (int i = 0; i < fl_x.size(); i++) {
                                            aX[i] = fl_x.get(i);
                                            aY[i] = fl_y.get(i);
                                        }

                                        // Create the zone
                                        temp.setZone(new ZoneNPoly(aX, aY, minZ, maxZ));
                                    } else {
                                        _log.warning("ZoneManager [ERROR]: Bad sql data for zone: " + zoneId);
                                        Close.SR(st, rs);
                                        continue;
                                    }
                                } else if (zoneShape.equalsIgnoreCase("Cylinder")) {
                                    // A Cylinder zone requires a centre point
                                    // at x,y and a radius
                                    int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
                                    if (rs.next() && zoneRad > 0) {
                                        int zoneX = rs.getInt("x");
                                        int zoneY = rs.getInt("y");

                                        // create the zone
                                        temp.setZone(new ZoneCylinder(zoneX, zoneY, minZ, maxZ, zoneRad));
                                    } else {
                                        _log.warning("ZoneManager [ERROR]: Bad sql data for zone: " + zoneId);
                                        Close.SR(st, rs);
                                        continue;
                                    }
                                } else {
                                    _log.warning("ZoneManager [ERROR]: Unknown shape: " + zoneShape);
                                    Close.SR(st, rs);
                                    continue;
                                }

                                Close.SR(st, rs);
                            } catch (Exception e) {
                                _log.warning("ZoneManager [ERROR]: Failed to load zone coordinates: " + e);
                            } finally {
                                Close.SR(st, rs);
                            }

                            // Check for aditional parameters
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("stat".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String name = attrs.getNamedItem("name").getNodeValue();
                                    String val = attrs.getNamedItem("val").getNodeValue();

                                    temp.setParameter(name, val);
                                }
                            }

                            //
                            if (temp instanceof L2TownZone || temp instanceof L2PeaceZone || temp instanceof L2SafeDismountZone) {
                                addSafe(temp);
                            }
                            if (temp instanceof L2WaterZone) {
                                addWater((L2WaterZone) temp);
                            }
                            if (temp instanceof L2FishingZone) {
                                addFishing(temp);
                            }
                            if (temp instanceof L2ArenaZone) {
                                addArena((L2ArenaZone) temp);
                            }
                            if (temp instanceof L2PvpFarmZone) {
                                addRewardPvp((L2PvpFarmZone) temp);
                            }

                            addZone(temp);

                            // Skip checks for fishing zones
                            /*if (temp instanceof L2FishingZone) {
                             zoneCount++;
                             continue;
                             }*/
                            // Register the zone into any world region it
                            if (_regions.containsKey(zoneId)) // Сначала првоеряем, кеширован ли регион для зоны
                            {
                                //System.out.println("1###!!!!!");
                                FastList<Region> rgs = _regions.get(zoneId);
                                //System.out.println("1###!!!!!^^^^" + rgs.size());
                                for (FastList.Node<Region> k = rgs.head(), end = rgs.tail(); (k = k.getNext()) != end;) {
                                    //System.out.println("2###!!!!!");
                                    Region value = k.getValue();
                                    if (value == null) {
                                        //System.out.println("3###!!!!!");
                                        continue;
                                    }
                                    worldRegions[value.x][value.y].addZone(temp);
                                }
                            } else //если зона ни найдена в кеше, то ищем регион
                            {
                                if (_update == 2) {
                                    File wr = new File("./data/world_regions.txt");
                                    wr.createNewFile();
                                    // currently 11136 test for each zone :>
                                    int ax, ay, bx, by;
                                    TextBuilder tb = new TextBuilder();
                                    for (int x = 0; x < worldRegions.length; x++) {
                                        for (int y = 0; y < worldRegions[x].length; y++) {
                                            ax = (x - L2World.OFFSET_X) << L2World.SHIFT_BY;
                                            bx = ((x + 1) - L2World.OFFSET_X) << L2World.SHIFT_BY;
                                            ay = (y - L2World.OFFSET_Y) << L2World.SHIFT_BY;
                                            by = ((y + 1) - L2World.OFFSET_Y) << L2World.SHIFT_BY;

                                            if (temp.getZone().intersectsRectangle(ax, bx, ay, by)) {
                                                worldRegions[x][y].addZone(temp);
                                                tb.append(x + "," + y + ";");
                                            }
                                        }
                                    }
                                    //System.out.println("ZoneManager [INFO]: ####Please update data/world_regions file: see logs/world_region_error.txt");
                                    Log.addToPath(wr, zoneId + "=" + tb.toString());
                                    tb.clear();
                                    tb = null;
                                } else {
                                    _update = 1;
                                    break outer;
                                }

                            }
                            // Special managers for arenas, towns...
                            if (temp instanceof L2BossZone) {
                                GrandBossManager.getInstance().addZone((L2BossZone) temp);
                            }

                            // Increase the counter
                            zoneCount++;
                        }
                    }
                }
            }

            //////////////////////////

            /* file = new File(Config.DATAPACK_ROOT, "data/pvp_zones.xml");
             if (!file.exists()) {
             _log.config("CustomServerData [ERROR]: data/pvp_zones.xml doesn't exist");
             return;
             }
            
             factory = DocumentBuilderFactory.newInstance();
             factory.setValidating(false);
             factory.setIgnoringComments(true);
             doc = factory.newDocumentBuilder().parse(file);
            
             int zoneId = 400500;
            
             outer:
             for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
             if ("list".equalsIgnoreCase(n.getNodeName())) {
             for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
             if ("zone".equalsIgnoreCase(d.getNodeName())) {
             NamedNodeMap attrs = d.getAttributes();
             String zone_name = attrs.getNamedItem("name").getNodeValue();
            
             L2ZoneType temp = new L2CheckZone(zoneId);
            
             for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
             if ("area".equalsIgnoreCase(cd.getNodeName())) {
             attrs = cd.getAttributes();
            
             FastList<Integer> fl_x = new FastList<Integer>(), fl_y = new FastList<Integer>();
             // Load the rest
             String[] coords = attrs.getNamedItem("coords").getNodeValue().split(";");
             for (String coord : coords) {
             if (coord.equalsIgnoreCase("")) {
             continue;
             }
            
             String[] xy = coord.split(",");
             fl_x.add(Integer.parseInt(xy[0]));
             fl_y.add(Integer.parseInt(xy[1]));
             }
            
             // An nPoly needs to have at least 3
             // vertices
             if ((fl_x.size() == fl_y.size()) && (fl_x.size() > 2)) {
             // Create arrays
             int[] aX = new int[fl_x.size()];
             int[] aY = new int[fl_y.size()];
            
             // This runs only at server startup so
             // dont complain :>
             for (int i = 0; i < fl_x.size(); i++) {
             aX[i] = fl_x.get(i);
             aY[i] = fl_y.get(i);
             }
            
             // Create the zone
             temp.setZone(new ZoneNPoly(aX, aY, Integer.parseInt(attrs.getNamedItem("minZ").getNodeValue()), Integer.parseInt(attrs.getNamedItem("maxZ").getNodeValue())));
             } else {
             _log.warning("ZoneManager [ERROR]: Bad sql data for zone: " + zoneId);
             Close.SR(st, rs);
             continue;
             }
             //
             }
             if ("penalty".equalsIgnoreCase(cd.getNodeName())) {
             attrs = cd.getAttributes();
            
            
             temp.setParameter("items", attrs.getNamedItem("items").getNodeValue());
             temp.setParameter("loc", attrs.getNamedItem("loc").getNodeValue());
            
             //penalty = new ZonePenalty(new Location(Integer.parseInt(loc[0]), Integer.parseInt(loc[1]), Integer.parseInt(loc[2])), penalty_items);
             }
             }
             zoneId++;
            
             addZone(temp);
            
             // Register the zone into any world region it
             if (_regions.containsKey(zoneId)) // Сначала првоеряем, кеширован ли регион для зоны
             {
             //System.out.println("1###!!!!!");
             FastList<Region> rgs = _regions.get(zoneId);
             //System.out.println("1###!!!!!^^^^" + rgs.size());
             for (FastList.Node<Region> k = rgs.head(), end = rgs.tail(); (k = k.getNext()) != end;) {
             //System.out.println("2###!!!!!");
             Region value = k.getValue();
             if (value == null) {
             //System.out.println("3###!!!!!");
             continue;
             }
             worldRegions[value.x][value.y].addZone(temp);
             }
             } else //если зона ни найдена в кеше, то ищем регион
             {
             if (_update == 2) {
             File wr = new File("./data/world_regions.txt");
             wr.createNewFile();
             // currently 11136 test for each zone :>
             int ax, ay, bx, by;
             TextBuilder tb = new TextBuilder();
             for (int x = 0; x < worldRegions.length; x++) {
             for (int y = 0; y < worldRegions[x].length; y++) {
             ax = (x - L2World.OFFSET_X) << L2World.SHIFT_BY;
             bx = ((x + 1) - L2World.OFFSET_X) << L2World.SHIFT_BY;
             ay = (y - L2World.OFFSET_Y) << L2World.SHIFT_BY;
             by = ((y + 1) - L2World.OFFSET_Y) << L2World.SHIFT_BY;
            
             if (temp.getZone().intersectsRectangle(ax, bx, ay, by)) {
             worldRegions[x][y].addZone(temp);
             tb.append(x + "," + y + ";");
             }
             }
             }
             //System.out.println("ZoneManager [INFO]: ####Please update data/world_regions file: see logs/world_region_error.txt");
             Log.addToPath(wr, zoneId + "=" + tb.toString());
             tb.clear();
             tb = null;
             } else {
             _update = 1;
             break outer;
             }
            
             }
             }
             }
             }
             }*/
            /////////////////////////
        } catch (Exception e) {
            _log.log(Level.SEVERE, "ZoneManager [ERROR]: loading failed: ", e);
            return;
        } finally {
            Close.CSR(con, st, rs);
        }

        if (_update > 0) {
            switch (_update) {
                case 1:
                    _update = 2;
                    System.out.println("ZoneManager [INFO]: ####Updating zone cache, please wait...");
                    File wr = new File("./data/world_regions.txt");
                    if (wr.exists()) {
                        wr.delete();
                    }
                    WorldRegionTable.getInstance().clearRegions();
                    load();
                    break;
                case 2:
                    System.out.println("ZoneManager [INFO]: ####Updating zone cache, done; restarting now...");
                    System.exit(2);
                    break;
            }
        } else {
            GrandBossManager.getInstance().initZones();

            long time = (System.currentTimeMillis() - start);
            _log.info("ZoneManager: Loaded " + zoneCount + " zones; Time: " + time + "ms.");
        }

        loadPvpZones();
    }

    private L2ZoneType getZoneByType(String zoneType, int zoneId) {
        if (zoneType.equals("Fishing")) {
            return new L2FishingZone(zoneId);
        } else if (zoneType.equals("ClanHall")) {
            return new L2ClanHallZone(zoneId);
        } else if (zoneType.equals("Peace")) {
            return new L2PeaceZone(zoneId);
        } else if (zoneType.equals("Town")) {
            return new L2TownZone(zoneId);
        } else if (zoneType.equals("OlympiadStadium")) {
            return new L2OlympiadStadiumZone(zoneId);
        } else if (zoneType.equals("Castle")) {
            return new L2CastleZone(zoneId);
        } else if (zoneType.equals("Damage")) {
            return new L2DamageZone(zoneId);
        } else if (zoneType.equals("Skill")) {
            return new L2PoisonZone(zoneId);
        } else if (zoneType.equals("Arena")) {
            return new L2ArenaZone(zoneId);
        } else if (zoneType.equals("MotherTree")) {
            return new L2MotherTreeZone(zoneId);
        } else if (zoneType.equals("Bighead")) {
            return new L2BigheadZone(zoneId);
        } else if (zoneType.equals("NoLanding")) {
            return new L2NoLandingZone(zoneId);
        } else if (zoneType.equals("Jail")) {
            return new L2JailZone(zoneId);
        } else if (zoneType.equals("DerbyTrack")) {
            return new L2DerbyTrackZone(zoneId);
        } else if (zoneType.equals("Boss")) {
            return new L2BossZone(zoneId);
        } else if (zoneType.equals("Water")) {
            return new L2WaterZone(zoneId);
        } else if (zoneType.equals("SiegeFlag")) {
            return new L2SiegeFlagZone(zoneId);
        } else if (zoneType.equals("SiegeRule")) {
            return new L2SiegeRuleZone(zoneId);
        } else if (zoneType.equals("SiegeWait")) {
            return new L2SiegeWaitZone(zoneId);
        } else if (zoneType.equals("Tvt")) {
            return new L2TvtZone(zoneId);
        } else if (zoneType.equals("Aq")) {
            return new L2AqZone(zoneId);
        } else if (zoneType.equals("S400")) {
            return new L2S400Zone(zoneId);
        } else if (zoneType.equals("Dismount")) {
            return new L2DismountZone(zoneId);
        } else if (zoneType.equals("Coliseum")) {
            return new L2ColiseumZone(zoneId);
        } else if (zoneType.equals("ElfTree")) {
            return new L2ElfTreeZone(zoneId);
        } else if (zoneType.equals("SafeDismount")) {
            return new L2SafeDismountZone(zoneId);
        } else if (zoneType.equals("NobleRb")) {
            return new L2NoblessRbZone(zoneId);
        } else if (zoneType.equals("Zaken")) {
            return new L2ZakenZone(zoneId);
        } else if (zoneType.equals("OlympTex")) {
            return new L2OlympiadTexture(zoneId);
        } else if (zoneType.equals("PvpFarm")) {
            return new L2PvpFarmZone(zoneId);
        } else if (zoneType.equals("HotSpa")) {
            return new L2HotSpaZone(zoneId);
        } else if (zoneType.equals("ZakenWelcome")) {
            return new L2ZakenWelcomeZone(zoneId);
        } else if (zoneType.equals("Special")) {
            return new L2SpecialZone(zoneId);
        } else if (zoneType.equals("PvpReward")) {
            return new L2PvpRewardZone(zoneId);
        } else if (zoneType.equals("NoLogout")) {
            return new L2AntiLogoutZone(zoneId);
        } else if (zoneType.equals("Spec")) {
            return new L2SpecZone(zoneId);
        } else if (zoneType.equals("AntiSummon")) {
            return new L2AntiSummonZone(zoneId);
        } else if (zoneType.equals("TvtCheck")) {
            return new L2TvtCheckZone(zoneId);
        }
        return null;
    }

    /**
     * Add new zone
     *
     * @param zone
     */
    public void addZone(L2ZoneType zone) {
        _zones.add(zone);
    }

    public void addSafe(L2ZoneType zone) {
        _safeZones.add(zone);
    }

    public void addWater(L2WaterZone zone) {
        _waterZones.add(zone);
    }

    public void addFishing(L2ZoneType zone) {
        _fishZones.add(zone);
    }

    public void addArena(L2ArenaZone zone) {
        _arenaZones.add(zone);
    }

    public void addRewardPvp(L2PvpFarmZone zone) {
        _rewardPvpZones.add(zone);
    }

    public void checkPvpRewards(L2PcInstance player) {
        if (player == null) {
            return;
        }
        //System.out.println("###3#");

        L2PvpFarmZone zone = null;
        for (FastList.Node<L2PvpFarmZone> n = _rewardPvpZones.head(), end = _rewardPvpZones.tail(); (n = n.getNext()) != end;) {
            zone = n.getValue(); // No typecast necessary.
            if (zone == null) {
                continue;
            }

            //System.out.println("###4#");
            if (!zone.isInsideZone(player)) {
                continue;
            }

            //System.out.println("###5#");
            zone.giveRewards(player);
        }
    }

    /**
     * Returns all zones registered with the ZoneManager. To minimise iteration
     * processing retrieve zones from L2WorldRegion for a specific location
     * instead.
     *
     * @return zones
     */
    public FastList<L2ZoneType> getAllZones() {
        return _zones;
    }

    public FastList<L2ZoneType> getSafeZones() {
        return _safeZones;
    }

    /**
     * Returns all zones from where the object is located
     *
     * @param object
     * @return zones
     */
    public FastList<L2ZoneType> getZones(L2Object object) {
        return getZones(object.getX(), object.getY(), object.getZ());
    }

    /**
     * Returns all zones from given coordinates (plane)
     *
     * @param x
     * @param y
     * @return zones
     */
    public FastList<L2ZoneType> getZones(int x, int y) {
        L2WorldRegion region = L2World.getInstance().getRegion(x, y);
        FastList<L2ZoneType> temp = new FastList<L2ZoneType>();
        for (L2ZoneType zone : region.getZones()) {
            if (zone.isInsideZone(x, y)) {
                temp.add(zone);
            }
        }
        return temp;
    }

    /**
     * Returns all zones from given coordinates
     *
     * @param x
     * @param y
     * @param z
     * @return zones
     */
    public FastList<L2ZoneType> getZones(int x, int y, int z) {
        L2WorldRegion region = L2World.getInstance().getRegion(x, y);
        FastList<L2ZoneType> temp = new FastList<L2ZoneType>();
        for (L2ZoneType zone : region.getZones()) {
            if (zone.isInsideZone(x, y, z)) {
                temp.add(zone);
            }
        }
        return temp;
    }

    public final L2ArenaZone getArena(L2Character ch) {
        return getArena(ch.getX(), ch.getY(), ch.getZ(), null);
    }

    public final L2ArenaZone getArena(int x, int y, int z, L2ArenaZone zn) {
        for (FastList.Node<L2ArenaZone> n = _arenaZones.head(), end = _arenaZones.tail(); (n = n.getNext()) != end;) {
            zn = n.getValue(); // No typecast necessary.
            if (zn == null) {
                continue;
            }
            if (zn.isInsideZone(x, y, z)) {
                return zn;
            }
        }
        return null;
    }

    public final L2WaterZone getWaterZone(int x, int y, int z, L2WaterZone zn) {
        for (FastList.Node<L2WaterZone> n = _waterZones.head(), end = _waterZones.tail(); (n = n.getNext()) != end;) {
            zn = n.getValue(); // No typecast necessary.
            if (zn == null) {
                continue;
            }
            if (zn.isInsideZone(x, y, z)) {
                return zn;
            }
        }
        return null;
    }

    public final L2FishingZone getFishingZone(int x, int y, int z) {
        L2ZoneType zn = null;
        for (FastList.Node<L2ZoneType> n = _fishZones.head(), end = _fishZones.tail(); (n = n.getNext()) != end;) {
            zn = n.getValue(); // No typecast necessary.   
            if (zn.isInsideZone(x, y, z)) {
                return ((L2FishingZone) zn);
            }
        }
        return null;
    }

    public final L2OlympiadStadiumZone getOlympiadStadium(L2Character ch) {
        L2ZoneType zn = null;
        L2OlympiadStadiumZone o = null;
        FastList<L2ZoneType> all = new FastList<L2ZoneType>();
        all.addAll(getZones(ch.getX(), ch.getY(), ch.getZ()));
        for (FastList.Node<L2ZoneType> n = all.head(), end = all.tail(); (n = n.getNext()) != end;) {
            zn = n.getValue(); // No typecast necessary.   
            if (zn instanceof L2OlympiadStadiumZone && zn.isCharacterInZone(ch)) {
                o = ((L2OlympiadStadiumZone) zn);
                break;
            }
        }
        all.clear();
        all = null;
        return o;
    }

    public final L2AqZone getAqZone(int x, int y, int z) {
        L2AqZone a = null;
        L2ZoneType zn = null;
        FastList<L2ZoneType> all = new FastList<L2ZoneType>();
        all.addAll(getZones(x, y, z));
        for (FastList.Node<L2ZoneType> n = all.head(), end = all.tail(); (n = n.getNext()) != end;) {
            zn = n.getValue(); // No typecast necessary.   
            if (zn instanceof L2AqZone) {
                a = ((L2AqZone) zn);
                break;
            }
        }
        all.clear();
        all = null;
        return a;
    }
    //

    public boolean inSafe(L2Object obj) {
        L2ZoneType temp = null;
        for (FastList.Node<L2ZoneType> n = _safeZones.head(), end = _safeZones.tail(); (n = n.getNext()) != end;) {
            temp = n.getValue(); // No typecast necessary.
            if (temp == null) {
                continue;
            }

            if (temp.isInsideZone(obj)) {
                if (temp.isPvP(obj.getX(), obj.getY())) {
                    return false;
                }

                return true;
            }
        }
        temp = null;
        return false;
    }
    //

    public boolean inTradeZone(L2PcInstance pc) {
        L2ZoneType temp = null;
        for (FastList.Node<L2ZoneType> n = _safeZones.head(), end = _safeZones.tail(); (n = n.getNext()) != end;) {
            temp = n.getValue(); // No typecast necessary.  
            if (temp == null) {
                continue;
            }

            if (!temp.isTownZone()) {
                continue;
            }

            if (temp.isInsideZone(pc)) {
                if (temp.isInsideTradeZone(pc.getX(), pc.getY())) {
                    return true;
                }

                if (Config.TRADE_ZONE_ENABLE) {
                    return false;
                }
                break;
            }
        }
        temp = null;
        return !Config.TRADE_ZONE_ENABLE;
    }
    //

    public boolean inPvpZone(L2Object obj) {
        L2ZoneType temp = null;
        for (FastList.Node<L2ZoneType> n = _safeZones.head(), end = _safeZones.tail(); (n = n.getNext()) != end;) {
            temp = n.getValue(); // No typecast necessary.  
            if (temp == null) {
                continue;
            }

            if (!temp.isTownZone()) {
                continue;
            }

            if (temp.isArena() && temp.isInsideZone(obj) && temp.isPvP(obj.getX(), obj.getY())) {
                return true;
            }
        }
        temp = null;
        return false;
    }

    public void checkPvpTownRewards(L2PcInstance player) {
        if (player == null) {
            return;
        }

        /*L2ZoneType zone = null;
         for (FastList.Node<L2ZoneType> n = _safeZones.head(), end = _safeZones.tail(); (n = n.getNext()) != end;) {
         zone = n.getValue(); // No typecast necessary.
         if (zone == null) {
         continue;
         }
         if (!zone.isTownZone() || !zone.isPvP(player.getX(), player.getY())) {
         continue;
         }
        
         if (!zone.isInsideZone(player)) {
         continue;
         }
        
         zone.givePvpRewards(player);
         }*/
    }
    //
    private static FastList<SpecialPvpZone> _pvpZones = new FastList<SpecialPvpZone>();

    private static class SpecialPvpZone {

        public String name;
        public FastList<ZoneReward> rewards = new FastList<ZoneReward>();
        public ZonePenalty penalty;
        public EventTerritory area;
        public ZoneJackPot extra;
        public int kills = 0;

        public SpecialPvpZone(String name, FastList<ZoneReward> rewards, EventTerritory area, ZonePenalty penalty, ZoneJackPot extra) {
            this.name = name;
            this.rewards = rewards;
            this.area = area;
            this.penalty = penalty;
            this.extra = extra;
        }

        private boolean containsPlayer(L2PcInstance player) {
            return area.contains(player.getX(), player.getY(), player.getZ());
        }

        public void incKills(L2PcInstance player) {
            if (extra == null) {
                return;
            }
            kills += 1;
            if (kills >= extra.kills) {
                kills = 0;
                extra.reward(player);
            }
        }

        public boolean isSkillDisabled(int id) {
            return penalty.isSkillDisabled(id);
        }

        public boolean isResDisabled() {
            return penalty.isResDisabled();
        }
    }

    private static class ZoneReward {

        public FastList<EventReward> items = new FastList<EventReward>();
        public int delay;
        public int minHour;
        public int maxHour;

        public ZoneReward(int delay, int minHour, int maxHour, FastList<EventReward> items) {
            this.delay = delay;
            this.minHour = minHour;
            this.maxHour = maxHour;
            this.items = items;
            if (minHour == 24 && maxHour == 24) {
                this.minHour = 9999;
            }
        }

        public boolean isExpired(int now) {
            if (minHour == 9999) {
                return false;
            }

            if (now < minHour || now > maxHour) {
                return true;
            }

            return false;
        }
    }

    private static class ZonePenalty {

        public Location loc;
        public int resurrect;
        FastList<Integer> items = new FastList<Integer>();
        FastList<Integer> skills = new FastList<Integer>();

        public ZonePenalty(Location loc, FastList<Integer> items, FastList<Integer> skills, int resurrect) {
            this.loc = loc;
            this.items = items;
            this.skills = skills;
            this.resurrect = resurrect;
        }

        public boolean isSkillDisabled(int id) {
            return skills.contains(id);
        }

        public boolean isResDisabled() {
            return (resurrect == 0);
        }
    }

    private static class ZoneJackPot {

        public int kills;
        public String announce;
        FastList<EventReward> items = new FastList<EventReward>();

        public ZoneJackPot(int kills, FastList<EventReward> items, String announce) {
            this.kills = kills;
            this.items = items;
            this.announce = announce;
        }

        public void reward(L2PcInstance player) {
            /*if (!announce.isEmpty()) {
             Announcements.getInstance().announceToAll(announce.replaceAll("%player%", player.getName()));
             }
             EventReward reward = null;
             for (FastList.Node<EventReward> k = items.head(), endk = items.tail(); (k = k.getNext()) != endk;) {
             reward = k.getValue();
             if (reward == null) {
             continue;
             }
            
             if (reward.chance == 100 || Rnd.get(100) < reward.chance) {
             player.addItem("pvp_bonus", reward.id, reward.count, player, true);
             }
             }
             reward = null;*/
        }
    }

    private void loadPvpZones() {
        if (!Config.PVP_ZONE_REWARDS) {
            return;
        }

        /*try {
         File file = new File(Config.DATAPACK_ROOT, "data/pvp_zones.xml");
         if (!file.exists()) {
         _log.config("CustomServerData [ERROR]: data/pvp_zones.xml doesn't exist");
         return;
         }
        
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setValidating(false);
         factory.setIgnoringComments(true);
         Document doc = factory.newDocumentBuilder().parse(file);
        
         for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
         for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
         if ("zone".equalsIgnoreCase(d.getNodeName())) {
         NamedNodeMap attrs = d.getAttributes();
         String zone_name = attrs.getNamedItem("name").getNodeValue();
        
         ZonePenalty penalty = null; //+
         ZoneJackPot jackpot = null; //+
         EventTerritory area = new EventTerritory(0); //+
         FastList<ZoneReward> rewards = new FastList<ZoneReward>(); //+
        
         for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
         if ("rewards".equalsIgnoreCase(cd.getNodeName())) {
         attrs = cd.getAttributes();
         FastList<EventReward> reward_items = new FastList<EventReward>();
        
         String[] items = attrs.getNamedItem("items").getNodeValue().split(";");
         for (String drop : items) {
         if (drop.equalsIgnoreCase("")) {
         continue;
         }
        
         String[] item = drop.split(",");
        
         reward_items.add(new EventReward(Integer.parseInt(item[0]), Integer.parseInt(item[1]), Integer.parseInt(item[2])));
         }
        
         String[] time = attrs.getNamedItem("time").getNodeValue().split("~");
         int minHour = Integer.parseInt(time[0].replaceAll(":", ""));
         int maxHour = Integer.parseInt(time[1].replaceAll(":", ""));
         if (maxHour == 0) {
         maxHour = 2400;
         }
         int delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
        
         rewards.add(new ZoneReward(delay, minHour, maxHour, reward_items));
         }
         if ("penalty".equalsIgnoreCase(cd.getNodeName())) {
         attrs = cd.getAttributes();
        
         FastList<Integer> penalty_items = new FastList<Integer>();
         String[] items = attrs.getNamedItem("items").getNodeValue().split(",");
         for (String item : items) {
         if (item.equalsIgnoreCase("")) {
         continue;
         }
        
         penalty_items.add(Integer.parseInt(item));
         }
         FastList<Integer> penalty_skills = new FastList<Integer>();
         String[] skills = attrs.getNamedItem("skills").getNodeValue().split(",");
         for (String skill : skills) {
         if (skill.equalsIgnoreCase("")) {
         continue;
         }
        
         penalty_skills.add(Integer.parseInt(skill));
         }
         String[] loc = attrs.getNamedItem("loc").getNodeValue().split(",");
         int resurrect = 1;
         try {
         resurrect = Integer.parseInt(attrs.getNamedItem("resurrect").getNodeValue());
         } catch (Exception e) {
         resurrect = 1;
         }
         penalty = new ZonePenalty(new Location(Integer.parseInt(loc[0]), Integer.parseInt(loc[1]), Integer.parseInt(loc[2])), penalty_items, penalty_skills, resurrect);
         }
         if ("area".equalsIgnoreCase(cd.getNodeName())) {
         attrs = cd.getAttributes();
        
         String[] coords = attrs.getNamedItem("coords").getNodeValue().split(";");
         for (String coord : coords) {
         if (coord.equalsIgnoreCase("")) {
         continue;
         }
        
         String[] xy = coord.split(",");
         area.addPoint(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
         }
        
         area.setZ(Integer.parseInt(attrs.getNamedItem("minZ").getNodeValue()), Integer.parseInt(attrs.getNamedItem("maxZ").getNodeValue()));
         }
         if ("extra".equalsIgnoreCase(cd.getNodeName())) {
         attrs = cd.getAttributes();
        
         FastList<EventReward> reward_items = new FastList<EventReward>();
         String[] items = attrs.getNamedItem("items").getNodeValue().split(";");
         for (String drop : items) {
         if (drop.equalsIgnoreCase("")) {
         continue;
         }
        
         String[] item = drop.split(",");
        
         reward_items.add(new EventReward(Integer.parseInt(item[0]), Integer.parseInt(item[1]), Integer.parseInt(item[2])));
         }
         int kills = Integer.parseInt(attrs.getNamedItem("kills").getNodeValue());
         String announce = attrs.getNamedItem("announce").getNodeValue();
        
         jackpot = new ZoneJackPot(kills, reward_items, announce);
         }
         }
         _pvpZones.add(new SpecialPvpZone(zone_name, rewards, area, penalty, jackpot));
         }
         }
         }
         }
         } catch (Exception e) {
         _log.warning("ZoneManager [ERROR]: loadPvpZones() " + e.toString());
         }
         _log.config("ZoneManager: Special Pvp Zones, loaded " + _pvpZones.size() + " zones.");*/
    }

    public void checkSpecialRewards(L2PcInstance player) {
        if (player == null) {
            return;
        }


        /*String current = new SimpleDateFormat("HH:mm").format(new Date()).toString();
         int now = Integer.parseInt(current.replaceAll(":", ""));
        
         SpecialPvpZone zone = null;
         for (FastList.Node<SpecialPvpZone> n = _pvpZones.head(), end = _pvpZones.tail(); (n = n.getNext()) != end;) {
         zone = n.getValue(); // No typecast necessary.
         if (zone == null) {
         continue;
         }
        
         if (!zone.containsPlayer(player)) {
         continue;
         }
        
         zone.incKills(player);
         checkRewardZone(player, zone.rewards, null, now);
         }*/
    }

    public boolean isSkillDisabled(L2PcInstance player, int id) {
        /*SpecialPvpZone zone = null;
         for (FastList.Node<SpecialPvpZone> n = _pvpZones.head(), end = _pvpZones.tail(); (n = n.getNext()) != end;) {
         zone = n.getValue(); // No typecast necessary.
         if (zone == null) {
         continue;
         }
        
         if (!zone.containsPlayer(player)) {
         continue;
         }
        
         if (zone.isSkillDisabled(id)) {
         return true;
         }
         }*/
        return false;
    }

    public boolean isResDisabled(L2PcInstance player) {
        /*SpecialPvpZone zone = null;
         for (FastList.Node<SpecialPvpZone> n = _pvpZones.head(), end = _pvpZones.tail(); (n = n.getNext()) != end;) {
         zone = n.getValue(); // No typecast necessary.
         if (zone == null) {
         continue;
         }
        
         if (!zone.containsPlayer(player)) {
         continue;
         }
        
         if (zone.isResDisabled()) {
         return true;
         }
         }*/
        return false;
    }

    private void checkRewardZone(L2PcInstance player, FastList<ZoneReward> rewards, ZoneReward reward, int now) {
        if (player == null || rewards == null) {
            return;
        }
        /*for (FastList.Node<ZoneReward> k = rewards.head(), endk = rewards.tail(); (k = k.getNext()) != endk;) {
         reward = k.getValue();
         if (reward == null) {
         continue;
         }
        
         if (reward.isExpired(now)) {
         continue;
         }
        
         player.setLastPvpPkBan(reward.delay * 1000);
         checkRewardZoneItems(player, reward.items, null);
         }
         reward = null;*/
    }

    private void checkRewardZoneItems(L2PcInstance player, FastList<EventReward> rewards, EventReward reward) {
        /*if (player == null || rewards == null) {
         return;
         }
         for (FastList.Node<EventReward> k = rewards.head(), endk = rewards.tail(); (k = k.getNext()) != endk;) {
         reward = k.getValue();
         if (reward == null) {
         continue;
         }
        
         if (reward.chance == 100 || Rnd.get(100) < reward.chance) {
         player.addItem("pvp_bonus", reward.id, reward.count, player, true);
         }
         }
         reward = null;*/
    }

    //
    public static FastList<DwZone> _dwZones = new FastList<DwZone>();

    public static class DwZone {

        public Ellipse2D.Double zone;

        public DwZone(double x, double y, double w, double h) {
            zone = new Ellipse2D.Double(x, y, w, h);
        }

        public boolean contains(double x, double y) {
            return zone.contains(x, y);
        }

        public boolean intersects(double x, double y, double w, double h) {
            return zone.intersects(x, y, w, h);
        }
    }

    public static void addDwZone(double x, double y, double w, double h) {
        _dwZones.add(new DwZone(x, y, w, h));
    }

    public static Location getCoordInDwZone(int x, int y, int z, int tx, int ty, int tz) {
        Location loc = null;
        Iterator<Point2D> it = null;
        for (FastList.Node<DwZone> n = _dwZones.head(), end = _dwZones.tail(); (n = n.getNext()) != end;) {
            loc = findIntersects(n.getValue(), x, y, z, tx, ty, tz, it);
            if (loc == null) {
                continue;
            }
            return loc;
        }
        return null;
    }

    private static Location findIntersects(DwZone zone, int x1, int y1, int z1, int x2, int y2, int z2, Iterator<Point2D> it) {
        if (zone == null) {
            return null;
        }

        if (zone.contains(x1, y1) && zone.contains(x2, y2)) {
            return new Location(x1, y1, z1);
        }

        if (zone.contains(x1, y1) && !zone.contains(x2, y2)) {
            return null;
        }

        if (!zone.contains(x1, y1) && !zone.contains(x2, y2)) {
            return null;
        }

        it = null;
        try {
            it = Geo2D.intersection(new Line2D.Double(x1, y1, x2, y2), zone.zone);
        } catch (Exception e) {
            it = null;
        }

        if (it == null || !it.hasNext()) {
            return null;
        }

        Point2D point = it.next();
        if (it.hasNext()) {
            point = it.next();
        }

        return new Location((int) point.getX(), (int) point.getY(), GeoData.getInstance().getSpawnHeight((int) point.getX(), (int) point.getY(), z1, z2, null));
    }
}
