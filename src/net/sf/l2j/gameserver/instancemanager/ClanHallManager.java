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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;
import scripts.zone.type.L2ClanHallZone;

/**
 *
 * @author  Steuf
 */
public class ClanHallManager {

    private static final Logger _log = AbstractLogger.getLogger(ClanHallManager.class.getName());
    private static ClanHallManager _instance;
    private Map<Integer, ClanHall> _clanHall;
    private Map<Integer, ClanHall> _freeClanHall;
    private boolean _loaded = false;

    public static ClanHallManager getInstance() {
        return _instance;
    }

    public static void init() {
        //System.out.println("Initializing ClanHallManager");
        _instance = new ClanHallManager();
        _instance.load();
    }

    public boolean loaded() {
        return _loaded;
    }

    private ClanHallManager() {
    }

    /** Reload All Clan Hall */
    /*	public final void reload() Cant reload atm - would loose zone info
    {
    _clanHall.clear();
    _freeClanHall.clear();
    load();
    }
     */
    /** Load All Clan Hall */
    private final void load() {
        _clanHall = new FastMap<Integer, ClanHall>();
        _freeClanHall = new FastMap<Integer, ClanHall>();

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            int id;
            L2Clan clan = null;
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT * FROM clanhall ORDER BY id");
            rs = statement.executeQuery();
            rs.setFetchSize(50);
            while (rs.next()) {
                id = rs.getInt("id");
                if (rs.getInt("ownerId") == 0) {
                    _freeClanHall.put(id, new ClanHall(id, rs.getString("name"), rs.getInt("ownerId"), rs.getInt("lease"), rs.getString("desc"), rs.getString("location"), 0, rs.getInt("Grade"), rs.getBoolean("paid")));
                } else {
                    clan = ClanTable.getInstance().getClan(rs.getInt("ownerId"));
                    if (clan != null && clan.isActive()) {
                        _clanHall.put(id, new ClanHall(id, rs.getString("name"), rs.getInt("ownerId"), rs.getInt("lease"), rs.getString("desc"), rs.getString("location"), rs.getLong("paidUntil"), rs.getInt("Grade"), rs.getBoolean("paid")));
                        clan.setHasHideout(id);
                    } else {
                        _freeClanHall.put(id, new ClanHall(id, rs.getString("name"), rs.getInt("ownerId"), rs.getInt("lease"), rs.getString("desc"), rs.getString("location"), rs.getLong("paidUntil"), rs.getInt("Grade"), rs.getBoolean("paid")));
                        _freeClanHall.get(id).free();
                        AuctionManager.getInstance().initNPC(id);
                    }

                }
            }
            _loaded = true;
        } catch (Exception e) {
            _log.warning("Exception: ClanHallManager.load(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CSR(con, statement, rs);
        }
        _log.info("ClanHallManager: Loaded " + getClanHalls().size() + " owned and " + getFreeClanHalls().size() + " free clanhalls.");
    }

    /** Get Map with all FreeClanHalls */
    public final Map<Integer, ClanHall> getFreeClanHalls() {
        return _freeClanHall;
    }

    /** Get Map with all ClanHalls */
    public final Map<Integer, ClanHall> getClanHalls() {
        return _clanHall;
    }

    /** Check is free ClanHall */
    public final boolean isFree(int chId) {
        if (_freeClanHall.containsKey(chId)) {
            return true;
        }
        return false;
    }

    /** Free a ClanHall */
    public final synchronized void setFree(int chId) {
        _freeClanHall.put(chId, _clanHall.get(chId));
        ClanTable.getInstance().getClan(_freeClanHall.get(chId).getOwnerId()).setHasHideout(0);
        _freeClanHall.get(chId).free();
        _clanHall.remove(chId);
    }

    /** Set ClanHallOwner */
    public final synchronized void setOwner(int chId, L2Clan clan) {
        if (!_clanHall.containsKey(chId)) {
            _clanHall.put(chId, _freeClanHall.get(chId));
            _freeClanHall.remove(chId);
        } else {
            _clanHall.get(chId).free();
        }
        ClanTable.getInstance().getClan(clan.getClanId()).setHasHideout(chId);
        _clanHall.get(chId).setOwner(clan);
    }

    /** Get Clan Hall by Id */
    public final ClanHall getClanHallById(int clanHallId) {
        if (_clanHall.containsKey(clanHallId)) {
            return _clanHall.get(clanHallId);
        }
        if (_freeClanHall.containsKey(clanHallId)) {
            return _freeClanHall.get(clanHallId);
        }
        return null;
    }

    /** Get Clan Hall by x,y,z *//*
    public final ClanHall getClanHall(int x, int y, int z)
    {
    for (Map.Entry<Integer, ClanHall> ch : _clanHall.entrySet())
    if (ch.getValue().getZone().isInsideZone(x, y, z)) return ch.getValue();
    
    for (Map.Entry<Integer, ClanHall> ch : _freeClanHall.entrySet())
    if (ch.getValue().getZone().isInsideZone(x, y, z)) return ch.getValue();
    
    return null;
    }*/

    public final ClanHall getNearbyClanHall(int x, int y, int maxDist) {
        L2ClanHallZone zone = null;

        for (Map.Entry<Integer, ClanHall> ch : _clanHall.entrySet()) {
            if (ch == null || ch.getValue() == null) {
                continue;
            }
            zone = ch.getValue().getZone();
            if (zone != null && zone.getDistanceToZone(x, y) < maxDist) {
                return ch.getValue();
            }
        }
        for (Map.Entry<Integer, ClanHall> ch : _freeClanHall.entrySet()) {
            if (ch == null || ch.getValue() == null) {
                continue;
            }
            zone = ch.getValue().getZone();
            if (zone != null && zone.getDistanceToZone(x, y) < maxDist) {
                return ch.getValue();
            }
        }
        return null;
    }

    /** Get Clan Hall by Owner */
    public final ClanHall getClanHallByOwner(L2Clan clan) {
        for (Map.Entry<Integer, ClanHall> ch : _clanHall.entrySet()) {
            if (clan.getClanId() == ch.getValue().getOwnerId()) {
                return ch.getValue();
            }
        }
        return null;
    }
}