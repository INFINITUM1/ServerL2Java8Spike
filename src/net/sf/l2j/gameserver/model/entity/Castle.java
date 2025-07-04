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
package net.sf.l2j.gameserver.model.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.CastleUpdater;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.CropProcure;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.SeedProduction;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.util.GiveItem;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;
import scripts.zone.L2ZoneType;
import scripts.zone.type.L2CastleZone;
import scripts.zone.type.L2SiegeWaitZone;

public class Castle {

    protected static final Logger _log = AbstractLogger.getLogger(Castle.class.getName());
    // =========================================================
    // Data Field
    private FastList<CropProcure> _procure = new FastList<CropProcure>();
    private FastList<SeedProduction> _production = new FastList<SeedProduction>();
    private FastList<CropProcure> _procureNext = new FastList<CropProcure>();
    private FastList<SeedProduction> _productionNext = new FastList<SeedProduction>();
    private boolean _isNextPeriodApproved = false;
    private static final String CASTLE_MANOR_DELETE_PRODUCTION =
            "DELETE FROM castle_manor_production WHERE castle_id=?;";
    private static final String CASTLE_MANOR_DELETE_PRODUCTION_PERIOD =
            "DELETE FROM castle_manor_production WHERE castle_id=? AND period=?;";
    private static final String CASTLE_MANOR_DELETE_PROCURE =
            "DELETE FROM castle_manor_procure WHERE castle_id=?;";
    private static final String CASTLE_MANOR_DELETE_PROCURE_PERIOD =
            "DELETE FROM castle_manor_procure WHERE castle_id=? AND period=?;";
    private static final String CASTLE_UPDATE_CROP =
            "UPDATE castle_manor_procure SET can_buy=? WHERE crop_id=? AND castle_id=? AND period=?";
    private static final String CASTLE_UPDATE_SEED =
            "UPDATE castle_manor_production SET can_produce=? WHERE seed_id=? AND castle_id=? AND period=?";
    // =========================================================
    // Data Field
    private int _castleId = 0;
    private List<L2DoorInstance> _doors = new FastList<L2DoorInstance>();
    private List<String> _doorDefault = new FastList<String>();
    private String _name = "";
    private int _ownerId = 0;
    private int _ownerIdLast = 0;
    private Siege _siege = null;
    private Calendar _siegeDate;
    private int _siegeDayOfWeek = 7; // Default to saturday
    private int _siegeHourOfDay = 20; // Default to 8 pm server time
    private int _taxPercent = 0;
    private double _taxRate = 0;
    private int _treasury = 0;
    private L2CastleZone _zone;
    private L2SiegeWaitZone _zoneWait;
    private L2Clan _formerOwner = null;
    private L2Clan _ownerClan = null;
    private int _nbArtifact = 1;
    private Map<Integer, Integer> _engrave = new FastMap<Integer, Integer>();

    // =========================================================
    // Constructor
    public Castle(int castleId) {
        _castleId = castleId;
        if (_castleId == 7 || castleId == 9) // Goddard and Schuttgart
        {
            _nbArtifact = 2;
        }
        load();
        loadDoor();
    }

    // =========================================================
    // Method - Public
    public void Engrave(L2Clan clan, int objId) {
        _engrave.put(objId, clan.getClanId());
        if (_engrave.size() == _nbArtifact) {
            boolean rst = true;
            for (int id : _engrave.values()) {
                if (id != clan.getClanId()) {
                    rst = false;
                }
            }
            if (rst) {
                _engrave.clear();
                setOwner(clan);
            } else {
                getSiege().announceToPlayer("Clan " + clan.getName() + " has finished to engrave one of the rulers.", true);
            }
        } else {
            getSiege().announceToPlayer("Clan " + clan.getName() + " has finished to engrave one of the rulers.", true);
        }
    }

    // This method add to the treasury
    /** Add amount to castle instance's treasury (warehouse). */
    public void addToTreasury(int amount) {
        if (getOwnerId() <= 0) {
            return;
        }

        if (_name.equalsIgnoreCase("Schuttgart") || _name.equalsIgnoreCase("Goddard")) {
            Castle rune = CastleManager.getInstance().getCastle("rune");
            if (rune != null) {
                int runeTax = (int) (amount * rune.getTaxRate());
                if (rune.getOwnerId() > 0) {
                    rune.addToTreasury(runeTax);
                }
                amount -= runeTax;
            }
        }
        if (!_name.equalsIgnoreCase("aden") && !_name.equalsIgnoreCase("Rune") && !_name.equalsIgnoreCase("Schuttgart") && !_name.equalsIgnoreCase("Goddard")) // If current castle instance is not Aden, Rune, Goddard or Schuttgart.
        {
            Castle aden = CastleManager.getInstance().getCastle("aden");
            if (aden != null) {
                int adenTax = (int) (amount * aden.getTaxRate());        // Find out what Aden gets from the current castle instance's income
                if (aden.getOwnerId() > 0) {
                    aden.addToTreasury(adenTax); // Only bother to really add the tax to the treasury if not npc owned
                }
                amount -= adenTax; // Subtract Aden's income from current castle instance's income
            }
        }

        addToTreasuryNoTax(amount);
    }

    /** Add amount to castle instance's treasury (warehouse), no tax paying. */
    public boolean addToTreasuryNoTax(int amount) {
        if (getOwnerId() <= 0) {
            return false;
        }

        long cur = (long) _treasury + (long) amount;
        if (cur < 0) {
            _treasury = 0;
        } else if (cur >= Integer.MAX_VALUE) {
            _treasury = Integer.MAX_VALUE;
        } else {
            _treasury = (int) cur;
        }

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE castle SET treasury = ? WHERE id = ?");
            statement.setInt(1, _treasury);
            statement.setInt(2, _castleId);
            statement.execute();
        } catch (Exception e) {
            System.out.println("Exception: addToTreasuryNoTax: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
        return true;
    }

    /**
     * Move non clan members off castle area and to nearest town.<BR><BR>
     */
    public void banishForeigners() {
        getZone().banishForeigners(getOwnerId());
    }

    /**
     * Return true if object is inside the zone
     */
    public boolean checkIfInZone(int x, int y, int z) {
        return getZone().isInsideZone(x, y, z);
    }

    /**
     * Sets this castles zone
     * @param zone
     */
    public void setZone(L2CastleZone zone) {
        _zone = zone;
    }

    public L2CastleZone getZone() {
        if (_zone == null) {
            for (L2ZoneType zone : ZoneManager.getInstance().getAllZones()) {
                if (zone instanceof L2CastleZone && ((L2CastleZone) zone).getCastleId() == getCastleId()) {
                    _zone = (L2CastleZone) zone;
                    break;
                }
            }
        }
        return _zone;
    }

    public void setWaitZone(L2SiegeWaitZone zone) {
        _zoneWait = zone;
    }

    public L2SiegeWaitZone getWaitZone() {
        if (_zoneWait == null) {
            for (L2ZoneType zone : ZoneManager.getInstance().getAllZones()) {
                if (zone instanceof L2SiegeWaitZone && ((L2SiegeWaitZone) zone).getCastleId() == getCastleId()) {
                    _zoneWait = (L2SiegeWaitZone) zone;
                    break;
                }
            }
        }
        return _zoneWait;
    }

    /**
     * Get the objects distance to this castle
     * @param obj
     * @return
     */
    public double getDistance(L2Object obj) {
        return getZone().getDistanceToZone(obj);
    }

    public void closeDoor(L2PcInstance activeChar, int doorId) {
        openCloseDoor(activeChar, doorId, false);
    }

    public void openDoor(L2PcInstance activeChar, int doorId) {
        openCloseDoor(activeChar, doorId, true);
    }

    public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open) {
        if (activeChar.getClanId() != getOwnerId()) {
            return;
        }

        L2DoorInstance door = getDoor(doorId);
        if (door != null) {
            if (open) {
                door.openMe();
            } else {
                door.closeMe();
            }
        }
    }

    // This method is used to begin removing all castle upgrades
    public void removeUpgrade() {
        removeDoorUpgrade();
    }

    // This method updates the castle tax rate
    public void setOwner(L2Clan clan) {
        // Remove old owner
        if (getOwnerId() > 0 && (clan == null || clan.getClanId() != getOwnerId())) {
            L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId());			// Try to find clan instance
            if (oldOwner != null) {
                if (_formerOwner == null) {
                    _formerOwner = oldOwner;
                    if (Config.REMOVE_CASTLE_CIRCLETS) {
                        CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
                    }
                }
                oldOwner.setHasCastle(0); // Unset has castle flag for old owner
                oldOwner.removeBonusEffects();
                new Announcements().announceToAll(oldOwner.getName() + " has lost " + getName() + " castle!");
            }
        }

        updateOwnerInDB(clan);															// Update in database

        if (getSiege().getIsInProgress()) // If siege in progress
        {
            getSiege().midVictory();													// Mid victory phase of siege
        }
        updateClansReputation();
    }

    public void removeOwner(L2Clan clan) {
        if (clan != null) {
            _formerOwner = clan;
            if (Config.REMOVE_CASTLE_CIRCLETS) {
                CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
            }
            clan.setHasCastle(0);
            new Announcements().announceToAll(clan.getName() + " has lost " + getName() + " castle");
            clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
        }

        updateOwnerInDB(null);
        if (getSiege().getIsInProgress()) {
            getSiege().midVictory();
        }

        updateClansReputation();
    }

    // This method updates the castle tax rate
    public void setTaxPercent(L2PcInstance activeChar, int taxPercent) {
        int maxTax;
        switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE)) {
            case SevenSigns.CABAL_DAWN:
                maxTax = 25;
                break;
            case SevenSigns.CABAL_DUSK:
                maxTax = 5;
                break;
            default: // no owner
                maxTax = 15;
        }

        if (taxPercent < 0 || taxPercent > maxTax) {
            activeChar.sendMessage("Tax value must be between 0 and " + maxTax + ".");
            return;
        }

        setTaxPercent(taxPercent);
        activeChar.sendMessage(getName() + " castle tax changed to " + taxPercent + "%.");
    }

    public void setTaxPercent(int taxPercent) {
        _taxPercent = taxPercent;
        _taxRate = _taxPercent / 100.0;

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            ArrayList breakable = new ArrayList();
            Iterator castles;
            statement = con.prepareStatement("UPDATE castle SET taxPercent = ? WHERE id = ?");
            castles = breakable.iterator();
            while (castles.hasNext()) {
                statement.setInt(1, taxPercent);
                statement.setInt(2, getCastleId());
                statement.addBatch();
            }
            statement.executeBatch();
            statement.clearBatch();
        } catch (Exception e) {
        } finally {
            Close.CS(con, statement);
        }
    }

    /**
     * Respawn all doors on castle grounds<BR><BR>
     */
    public void spawnDoor() {
        spawnDoor(false);
    }

    /**
     * Respawn all doors on castle grounds<BR><BR>
     */
    public void spawnDoor(boolean isDoorWeak) {
        for (int i = 0; i < getDoors().size(); i++) {
            L2DoorInstance door = getDoors().get(i);
            if (door.getCurrentHp() <= 0) {
                door.decayMe();	// Kill current if not killed already
                door = DoorTable.parseList(_doorDefault.get(i));
                if (isDoorWeak) {
                    door.setCurrentHp(door.getMaxHp() / 2);
                }
                door.spawnMe(door.getX(), door.getY(), door.getZ());
                getDoors().set(i, door);
            } else if (door.getOpen()) {
                door.closeMe();
            }
        }
        loadDoorUpgrade(); // Check for any upgrade the doors may have
    }

    // This method upgrade door
    public void upgradeDoor(int doorId, int hp, int pDef, int mDef) {
        L2DoorInstance door = getDoor(doorId);
        if (door == null) {
            return;
        }

        if (door != null && door.getDoorId() == doorId) {
            door.setCurrentHp(door.getMaxHp() + hp);

            saveDoorUpgrade(doorId, hp, pDef, mDef);
            return;
        }
    }

    // =========================================================
    // Method - Private
    // This method loads castle
    private void load() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            statement = con.prepareStatement("Select * from castle where id = ?");
            statement.setInt(1, getCastleId());
            rs = statement.executeQuery();

            while (rs.next()) {
                _name = rs.getString("name");
                //_OwnerId = rs.getInt("ownerId");

                _siegeDate = Calendar.getInstance();
                _siegeDate.setTimeInMillis(rs.getLong("siegeDate"));

                _siegeDayOfWeek = rs.getInt("siegeDayOfWeek");
                if (_siegeDayOfWeek < 1 || _siegeDayOfWeek > 7) {
                    _siegeDayOfWeek = 7;
                }

                _siegeHourOfDay = rs.getInt("siegeHourOfDay");
                if (_siegeHourOfDay < 0 || _siegeHourOfDay > 23) {
                    _siegeHourOfDay = 20;
                }

                _taxPercent = rs.getInt("taxPercent");
                _treasury = rs.getInt("treasury");
            }

            Close.SR(statement, rs);

            _taxRate = _taxPercent / 100.0;

            statement = con.prepareStatement("Select clan_id from clan_data where hasCastle = ?");
            statement.setInt(1, getCastleId());
            rs = statement.executeQuery();

            while (rs.next()) {
                _ownerId = rs.getInt("clan_id");
            }

            if (getOwnerId() > 0) {
                _ownerIdLast = _ownerId;
                _ownerClan = ClanTable.getInstance().getClan(getOwnerId());                        // Try to find clan instance
                ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(_ownerClan, 1), 3600000);     // Schedule owner tasks to start running
            }

            Close.S(statement);
        } catch (Exception e) {
            System.out.println("Exception: loadCastleData(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CSR(con, statement, rs);
        }
        getSiege();
    }

    // This method loads castle door data from database
    private void loadDoor() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("Select * from castle_door where castleId = ?");
            statement.setInt(1, getCastleId());
            rs = statement.executeQuery();

            while (rs.next()) {
                // Create list of the door default for use when respawning dead doors
                _doorDefault.add(rs.getString("name")
                        + ";" + rs.getInt("id")
                        + ";" + rs.getInt("x")
                        + ";" + rs.getInt("y")
                        + ";" + rs.getInt("z")
                        + ";" + rs.getInt("range_xmin")
                        + ";" + rs.getInt("range_ymin")
                        + ";" + rs.getInt("range_zmin")
                        + ";" + rs.getInt("range_xmax")
                        + ";" + rs.getInt("range_ymax")
                        + ";" + rs.getInt("range_zmax")
                        + ";" + rs.getInt("hp")
                        + ";" + rs.getInt("pDef")
                        + ";" + rs.getInt("mDef"));

                L2DoorInstance door = DoorTable.parseList(_doorDefault.get(_doorDefault.size() - 1));
                door.spawnMe(door.getX(), door.getY(), door.getZ());
                _doors.add(door);
                DoorTable.getInstance().putDoor(door);
            }
        } catch (Exception e) {
            System.out.println("Exception: loadCastleDoor(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CSR(con, statement, rs);
        }
    }

    // This method loads castle door upgrade data from database
    private void loadDoorUpgrade() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("Select * from castle_doorupgrade where doorId in (Select Id from castle_door where castleId = ?)");
            statement.setInt(1, getCastleId());
            rs = statement.executeQuery();
            while (rs.next()) {
                upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
            }
        } catch (Exception e) {
            System.out.println("Exception: loadCastleDoorUpgrade(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CSR(con, statement, rs);
        }
    }

    private void removeDoorUpgrade() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("delete from castle_doorupgrade where doorId in (select id from castle_door where castleId=?)");
            statement.setInt(1, getCastleId());
            statement.execute();
        } catch (Exception e) {
            System.out.println("Exception: removeDoorUpgrade(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("INSERT INTO castle_doorupgrade (doorId, hp, pDef, mDef) values (?,?,?,?)");
            statement.setInt(1, doorId);
            statement.setInt(2, hp);
            statement.setInt(3, pDef);
            statement.setInt(4, mDef);
            statement.execute();
        } catch (Exception e) {
            System.out.println("Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    private void updateOwnerInDB(L2Clan clan) {
        if (clan != null) {
            _ownerId = clan.getClanId();	// Update owner id property
            _ownerClan = clan;
        } else {
            _ownerId = 0;					// Remove owner
        }
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            // ============================================================================
            // NEED TO REMOVE HAS CASTLE FLAG FROM CLAN_DATA
            // SHOULD BE CHECKED FROM CASTLE TABLE
            statement = con.prepareStatement("UPDATE clan_data SET hasCastle=0 WHERE hasCastle=?");
            statement.setInt(1, getCastleId());
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("UPDATE clan_data SET hasCastle=? WHERE clan_id=?");
            statement.setInt(1, getCastleId());
            statement.setInt(2, getOwnerId());
            statement.execute();
            Close.S(statement);
            // ============================================================================

            // Announce to clan memebers
            if (clan != null) {
                clan.setHasCastle(getCastleId()); // Set has castle flag for new owner
                new Announcements().announceToAll("Клан " + clan.getName() + " захватил замок " + getName() + "!");
                clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));

                ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000);	// Schedule owner tasks to start running
            }
        } catch (Exception e) {
            System.out.println("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    // =========================================================
    // Property
    public final int getCastleId() {
        return _castleId;
    }

    public final L2DoorInstance getDoor(int doorId) {
        if (doorId <= 0) {
            return null;
        }

        for (int i = 0; i < getDoors().size(); i++) {
            L2DoorInstance door = getDoors().get(i);
            if (door.getDoorId() == doorId) {
                return door;
            }
        }
        return null;
    }

    public final List<L2DoorInstance> getDoors() {
        return _doors;
    }

    public final String getName() {
        return _name;
    }

    public final int getOwnerId() {
        return _ownerId;
    }

    public final Siege getSiege() {
        if (_siege == null) {
            _siege = new Siege(new Castle[]{this});
        }
        return _siege;
    }

    public final Calendar getSiegeDate() {
        return _siegeDate;
    }

    public final int getSiegeDayOfWeek() {
        return _siegeDayOfWeek;
    }

    public final int getSiegeHourOfDay() {
        return _siegeHourOfDay;
    }

    public final int getTaxPercent() {
        return _taxPercent;
    }

    public final double getTaxRate() {
        return _taxRate;
    }

    public final int getTreasury() {
        return _treasury;
    }

    public FastList<SeedProduction> getSeedProduction(int period) {
        return (period == CastleManorManager.PERIOD_CURRENT ? _production : _productionNext);
    }

    public FastList<CropProcure> getCropProcure(int period) {
        return (period == CastleManorManager.PERIOD_CURRENT ? _procure : _procureNext);
    }

    public void setSeedProduction(FastList<SeedProduction> seed, int period) {
        if (period == CastleManorManager.PERIOD_CURRENT) {
            _production = seed;
        } else {
            _productionNext = seed;
        }
    }

    public void setCropProcure(FastList<CropProcure> crop, int period) {
        if (period == CastleManorManager.PERIOD_CURRENT) {
            _procure = crop;
        } else {
            _procureNext = crop;
        }
    }

    public synchronized SeedProduction getSeed(int seedId, int period) {
        for (SeedProduction seed : getSeedProduction(period)) {
            if (seed.getId() == seedId) {
                return seed;
            }
        }
        return null;
    }

    public synchronized CropProcure getCrop(int cropId, int period) {
        for (CropProcure crop : getCropProcure(period)) {
            if (crop.getId() == cropId) {
                return crop;
            }
        }
        return null;
    }

    public int getManorCost(int period) {
        FastList<CropProcure> procure;
        FastList<SeedProduction> production;

        if (period == CastleManorManager.PERIOD_CURRENT) {
            procure = _procure;
            production = _production;
        } else {
            procure = _procureNext;
            production = _productionNext;
        }

        int total = 0;
        if (production != null) {
            for (SeedProduction seed : production) {
                total += L2Manor.getInstance().getSeedBuyPrice(seed.getId()) * seed.getStartProduce();
            }
        }
        if (procure != null) {
            for (CropProcure crop : procure) {
                total += crop.getPrice() * crop.getStartAmount();
            }
        }
        return total;
    }

    //save manor production data
    public void saveSeedData() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION);
            statement.setInt(1, getCastleId());
            statement.execute();

            Close.S(statement);

            TextBuilder query = new TextBuilder("INSERT INTO castle_manor_production VALUES ");
            if (_production != null) {
                int count = 0;
                String values[] = new String[_production.size()];
                for (SeedProduction s : _production) {
                    values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_CURRENT + ")";
                    count++;
                }
                if (values.length > 0) {
                    query.append(values[0]);
                    for (int i = 1; i < values.length; i++) {
                        query.append("," + values[i]);
                    }
                    statement = con.prepareStatement(query.toString());
                    statement.execute();
                    Close.S(statement);
                }
            }
            query.clear();
            query.append("INSERT INTO castle_manor_production VALUES ");
            if (_productionNext != null) {
                int count = 0;
                String values[] = new String[_productionNext.size()];
                for (SeedProduction s : _productionNext) {
                    values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_NEXT + ")";
                    count++;
                }
                if (values.length > 0) {
                    query.append(values[0]);
                    for (int i = 1; i < values.length; i++) {
                        query.append("," + values[i]);
                    }
                    statement = con.prepareStatement(query.toString());
                    statement.execute();
                    Close.S(statement);
                }
            }
        } catch (Exception e) {
            _log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
        } finally {
            Close.CS(con, statement);
        }
    }

    //save manor production data for specified period
    public void saveSeedData(int period) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION_PERIOD);
            statement.setInt(1, getCastleId());
            statement.setInt(2, period);
            statement.execute();
            Close.S(statement);

            FastList<SeedProduction> prod = null;
            prod = getSeedProduction(period);

            if (prod != null) {
                int count = 0;
                TextBuilder query = new TextBuilder("INSERT INTO castle_manor_production VALUES ");
                String values[] = new String[prod.size()];
                for (SeedProduction s : prod) {
                    values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + period + ")";
                    count++;
                }
                if (values.length > 0) {
                    query.append(values[0]);
                    for (int i = 1; i < values.length; i++) {
                        query.append("," + values[i]);
                    }
                    statement = con.prepareStatement(query.toString());
                    statement.execute();
                    Close.S(statement);
                }
            }
        } catch (Exception e) {
            _log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
        } finally {
            Close.CS(con, statement);
        }
    }

    //save crop procure data
    public void saveCropData() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE);
            statement.setInt(1, getCastleId());
            statement.execute();
            Close.S(statement);
            TextBuilder query = new TextBuilder("INSERT INTO castle_manor_procure VALUES ");
            if (_procure != null) {
                int count = 0;
                String values[] = new String[_procure.size()];
                for (CropProcure cp : _procure) {
                    values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_CURRENT + ")";
                    count++;
                }
                if (values.length > 0) {
                    query.append(values[0]);
                    for (int i = 1; i < values.length; i++) {
                        query.append("," + values[i]);
                    }
                    statement = con.prepareStatement(query.toString());
                    statement.execute();
                    Close.S(statement);
                }
            }
            query.clear();
            query.append("INSERT INTO castle_manor_procure VALUES ");
            if (_procureNext != null) {
                int count = 0;
                String values[] = new String[_procureNext.size()];
                for (CropProcure cp : _procureNext) {
                    values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_NEXT + ")";
                    count++;
                }
                if (values.length > 0) {
                    query.append(values[0]);
                    for (int i = 1; i < values.length; i++) {
                        query.append("," + values[i]);
                    }
                    statement = con.prepareStatement(query.toString());
                    statement.execute();
                    Close.S(statement);
                }
            }
            query.clear();
        } catch (Exception e) {
            _log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
        } finally {
            Close.CS(con, statement);
        }
    }

    //	save crop procure data for specified period
    public void saveCropData(int period) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE_PERIOD);
            statement.setInt(1, getCastleId());
            statement.setInt(2, period);
            statement.execute();
            Close.S(statement);

            FastList<CropProcure> proc = null;
            proc = getCropProcure(period);

            if (proc != null) {
                int count = 0;
                TextBuilder query = new TextBuilder("INSERT INTO castle_manor_procure VALUES ");
                String values[] = new String[proc.size()];
                for (CropProcure cp : proc) {
                    values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + period + ")";
                    count++;
                }
                if (values.length > 0) {
                    query.append(values[0]);
                    for (int i = 1; i < values.length; i++) {
                        query.append("," + values[i]);
                    }
                    statement = con.prepareStatement(query.toString());
                    statement.execute();
                    Close.S(statement);
                }
            }
        } catch (Exception e) {
            _log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
        } finally {
            Close.CS(con, statement);
        }
    }

    public void updateCrop(int cropId, int amount, int period) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement(CASTLE_UPDATE_CROP);
            statement.setInt(1, amount);
            statement.setInt(2, cropId);
            statement.setInt(3, getCastleId());
            statement.setInt(4, period);
            statement.execute();
        } catch (Exception e) {
            _log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
        } finally {
            Close.CS(con, statement);
        }
    }

    public void updateSeed(int seedId, int amount, int period) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement(CASTLE_UPDATE_SEED);
            statement.setInt(1, amount);
            statement.setInt(2, seedId);
            statement.setInt(3, getCastleId());
            statement.setInt(4, period);
            statement.execute();
        } catch (SQLException e) {
            _log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
        } finally {
            Close.CS(con, statement);
        }
    }

    public boolean isNextPeriodApproved() {
        return _isNextPeriodApproved;
    }

    public void setNextPeriodApproved(boolean val) {
        _isNextPeriodApproved = val;
    }

    public void updateClansReputation() {
        if (_formerOwner != null) {
            if (_formerOwner != ClanTable.getInstance().getClan(getOwnerId())) {
                int maxreward = Math.max(0, _formerOwner.getReputationScore());
                _formerOwner.setReputationScore(_formerOwner.getReputationScore() - 1000, true);
                L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
                if (owner != null) {
                    owner.addPoints(Math.min(1000, maxreward));
                    owner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(owner));
                }
            } else {
                _formerOwner.addPoints(500);
            }

            _formerOwner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_formerOwner));
        } else {
            L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
            if (owner != null) {
                owner.addPoints(1000);
                owner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(owner));
            }
        }
    }

    public boolean isClanhall() {
        switch (_castleId) {
            case 21:
            case 34:
            case 35:
            case 64:
                return true;
            default:
                return false;
        }
    }

    public void giveOwnerBonus() {
        L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
        if (clan == null) {
            return;
        }

        FastList<EventReward> rewards = Config.CASTLE_SIEGE_REWARDS.get(getCastleId());
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        if (Config.SIEGE_REWARD_CLAN) {
            if (_ownerIdLast == _ownerId) {
                rewards = Config.CASTLE_RPOTECT_REWARDS.get(getCastleId());
            }
            Connect con = null;
            PreparedStatement st = null;
            try {
                con = L2DatabaseFactory.get();
                for (L2ClanMember member : clan.getMembers()) {
                    if (member == null) {
                        continue;
                    }

                    L2PcInstance pl = member.getPlayerInstance();
                    for (FastList.Node<EventReward> k = rewards.head(), endk = rewards.tail(); (k = k.getNext()) != endk;) {
                        EventReward reward = k.getValue();
                        if (reward == null) {
                            continue;
                        }

                        if (Rnd.get(100) < reward.chance) {
                            if (pl == null) {
                                GiveItem.insertOffline(con, member.getObjectId(), reward.id, reward.count, 0, 0, 0, "INVENTORY");
                            } else {
                                pl.addItem("SiegeReward", reward.id, reward.count, pl, true);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                _log.info("Castle [ERROR] giveOwnerBonus() for  " + clan.getLeaderId() + ": " + e.getMessage());
            } finally {

                Close.CS(con, st);
            }
            return;
        }

        L2PcInstance owner = L2World.getInstance().getPlayer(clan.getLeaderId());
        if (owner == null) {
            Connect con = null;
            PreparedStatement st = null;
            try {
                con = L2DatabaseFactory.get();
                for (FastList.Node<EventReward> k = rewards.head(), endk = rewards.tail(); (k = k.getNext()) != endk;) {
                    EventReward reward = k.getValue();
                    if (reward == null) {
                        continue;
                    }

                    if (Rnd.get(100) < reward.chance) {
                        GiveItem.insertOffline(con, clan.getLeaderId(), reward.id, (Config.CASTLE_SIEGE_REWARD_STATIC ? reward.count : Rnd.get(1, reward.count)), 0, 0, 0, "INVENTORY");
                        //GiveItem.insertOffline(con, clan.getLeaderId(), reward.id, reward.count, 0, 0, 0, "INVENTORY");
                    }
                }
            } catch (SQLException e) {
                _log.info("Castle [ERROR] giveOwnerBonus() for  " + clan.getLeaderId() + ": " + e.getMessage());
            } finally {

                Close.CS(con, st);
            }
            return;
        }

        for (FastList.Node<EventReward> k = rewards.head(), endk = rewards.tail(); (k = k.getNext()) != endk;) {
            EventReward reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                owner.addItem("SiegeReward", reward.id, (Config.CASTLE_SIEGE_REWARD_STATIC ? reward.count : Rnd.get(1, reward.count)), owner, true);
                //owner.addItem("SiegeReward", reward.id, reward.count, owner, true);
            }
        }
    }

    public void giveClanBonus() {
        L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
        if (clan == null) {
            return;
        }

        clan.setBonusSkill();
    }

    public void disableOwnerBonus(boolean f) {
        L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
        if (clan == null) {
            return;
        }

        clan.disableSiegeBonus(f);
    }

    private int _raidGuard = 0;

    public void setRaidGuard(int lvl) {
        _raidGuard = lvl;
    }

    public int getRaidGuard() {
        return _raidGuard;
    }

    public L2Clan getOwnerClan() {
        return _ownerClan;
    }
}