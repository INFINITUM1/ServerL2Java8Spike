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
import java.sql.SQLException;
import java.util.logging.Logger;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

public class ClanTable {

    private static final Logger _log = AbstractLogger.getLogger(ClanTable.class.getName());
    private static ClanTable _instance;
    private FastMap<Integer, L2Clan> _clans = new FastMap<Integer, L2Clan>().shared("ClanTable._clans");

    public static ClanTable getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new ClanTable();
    }

    public FastTable<L2Clan> getClans() {
        FastTable<L2Clan> clans = new FastTable<L2Clan>();
        /*
         * L2Clan cn = null; for (FastMap.Entry<Integer, L2Clan> e =
         * _clans.head(), end = _clans.tail(); (e = e.getNext()) != end;) { cn =
         * e.getValue(); // No typecast necessary. all.add(cn);
        }
         */
        clans.addAll(_clans.values());
        return clans;
    }

    public int getCount() {
        return _clans.size();
    }

    private ClanTable() {
        FastTable<Integer> clans = new FastTable<Integer>();
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT clan_id FROM clan_data");
            rs = st.executeQuery();
            rs.setFetchSize(50);
            while (rs.next()) {
                clans.add(rs.getInt("clan_id"));
            }
        } catch (SQLException e) {
            _log.warning("ClanTable [ERROR]: can't restore clans " + e);
            e.printStackTrace();
        } finally {
            Close.CSR(con, st, rs);
        }

        L2Clan clan = null;
        for (int i = 0, n = clans.size(); i < n; i++) {
            int clanId = clans.get(i);
            if (clanId == 0) {
                continue;
            }

            clan = new L2Clan(clanId);
            if (clan == null) {
                _log.warning("ClanTable [ERROR]: cant restore clanId: " + clanId);
                continue;
            }

            if (clan.getMembersCount() <= 0) {
                _log.warning("ClanTable [ERROR]: no members in clanId: " + clanId);
                continue;
            }

            if (clan.getLeader() == null) {
                _log.warning("ClanTable [ERROR]: no leader in clanId: " + clanId);
                continue;
            }

            _clans.put(clan.getClanId(), clan);
            if (clan.getDissolvingExpiryTime() != 0) {
                if (clan.getDissolvingExpiryTime() < System.currentTimeMillis()) {
                    destroyClan(clan.getClanId());
                } else {
                    scheduleRemoveClan(clan.getClanId());
                }
            }
        }
        restorewars();
        _log.config("Loading ClanTable... total " + _clans.size() + " clans.");
    }

    /**
     * @param clanId
     * @return
     */
    public L2Clan getClan(int clanId) {
        L2Clan clan = _clans.get(Integer.valueOf(clanId));
        return clan;
    }

    public L2Clan getClanByName(String clanName) {
        for (L2Clan clan : getClans()) {
            if (clan.getName().equalsIgnoreCase(clanName)) {
                return clan;
            }
        }
        return null;
    }

    /**
     * Creates a new clan and store clan info to database
     *
     * @param player
     * @return NULL if clan with same name already exists
     */
    public L2Clan createClan(L2PcInstance player, String clanName) {
        if (null == player) {
            return null;
        }

        //if (Config.DEBUG)
        // _log.fine(player.getObjectId() + "(" + player.getName() + ") requested a clan creation.");

        if (10 > player.getLevel()) {
            player.sendPacket(Static.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN);
            return null;
        }
        if (0 != player.getClanId()) {
            player.sendPacket(Static.FAILED_TO_CREATE_CLAN);
            return null;
        }
        if (System.currentTimeMillis() < player.getClanCreateExpiryTime()) {
            player.sendPacket(Static.YOU_MUST_WAIT_XX_DAYS_BEFORE_CREATING_A_NEW_CLAN);
            return null;
        }
        if (!Util.isAlphaNumeric(clanName) || 2 > clanName.length()) {
            player.sendPacket(Static.CLAN_NAME_INCORRECT);
            return null;
        }
        if (16 < clanName.length()) {
            player.sendPacket(Static.CLAN_NAME_TOO_LONG);
            return null;
        }

        if (null != getClanByName(clanName)) {
            // clan name is already taken
            player.sendPacket(SystemMessage.id(SystemMessageId.S1_ALREADY_EXISTS).addString(clanName));
            return null;
        }

        L2Clan clan = new L2Clan(IdFactory.getInstance().getNextId(), clanName);
        L2ClanMember leader = new L2ClanMember(clan, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle());
        clan.setLeader(leader);
        clan.setLevel(Config.ALT_CLAN_CREATE_LEVEL);
        leader.setPlayerInstance(player);
        clan.store();
        player.setClan(clan);
        player.setPledgeClass(leader.calculatePledgeClass(player));
        player.setClanPrivileges(L2Clan.CP_ALL);

        //if (Config.DEBUG)
        //_log.fine("New clan created: "+clan.getClanId() + " " +clan.getName());

        _clans.put(Integer.valueOf(clan.getClanId()), clan);

        //should be update packet only
        player.sendPacket(new PledgeShowInfoUpdate(clan));
        player.sendPacket(new PledgeShowMemberListAll(clan, player));
        player.sendPacket(new UserInfo(player));
        player.sendPacket(new PledgeShowMemberListUpdate(player));
        player.sendPacket(Static.CLAN_CREATED);
        return clan;
    }

    public synchronized void destroyClan(int clanId) {
        //System.out.println("#####" + clanId);
        L2Clan clan = getClan(clanId);
        if (clan == null) {
            return;
        }

        clan.broadcastToOnlineMembers(Static.CLAN_HAS_DISPERSED);
        int castleId = clan.getHasCastle();
        if (castleId == 0) {
            try {
                for (Siege siege : SiegeManager.getInstance().getSieges()) {
                    siege.removeSiegeClan(clanId);
                }
            } catch (Exception e) {
            }
        }

        L2ClanMember leaderMember = clan.getLeader();
        if (leaderMember == null) {
            clan.getWarehouse().destroyAllItems("ClanRemove", null, null);
        } else {
            clan.getWarehouse().destroyAllItems("ClanRemove", clan.getLeader().getPlayerInstance(), null);
        }

        for (L2ClanMember member : clan.getMembers()) {
            clan.removeClanMember(member.getName(), 0);
        }

        /*
         * CrestCache.getInstance().removePledgeCrest(clan.getCrestId());
         * CrestCache.getInstance().removePledgeCrestLarge(clan.getCrestId());
         * if (clan.getClanId() == clan.getAllyId()) {
         * CrestCache.getInstance().removeAllyCrest(clan.getAllyCrestId());
        }
         */

        _clans.remove(clanId);
        IdFactory.getInstance().releaseId(clanId);

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?");
            st.setInt(1, clanId);
            st.execute();
            Close.S(st);

            st = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?");
            st.setInt(1, clanId);
            st.execute();
            Close.S(st);

            st = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?");
            st.setInt(1, clanId);
            st.execute();
            Close.S(st);

            st = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?");
            st.setInt(1, clanId);
            st.execute();
            Close.S(st);

            st = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?");
            st.setInt(1, clanId);
            st.setInt(2, clanId);
            st.execute();
            Close.S(st);

            if (castleId != 0) {
                st = con.prepareStatement("UPDATE castle SET taxPercent = 0 WHERE id = ?");
                st.setInt(2, castleId);
                st.execute();
                Close.S(st);
            }
            // if (Config.DEBUG) _log.fine("clan removed in db: "+clanId);
        } catch (Exception e) {
            _log.warning("error while removing clan in db " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    public void scheduleRemoveClan(final int clanId) {
        ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {

            public void run() {
                if (getClan(clanId) == null) {
                    return;
                }
                if (getClan(clanId).getDissolvingExpiryTime() != 0) {
                    destroyClan(clanId);
                }
            }
        }, getClan(clanId).getDissolvingExpiryTime() - System.currentTimeMillis());
    }

    public boolean isAllyExists(String allyName) {
        for (L2Clan clan : getClans()) {
            if (clan.getAllyName() != null && clan.getAllyName().equalsIgnoreCase(allyName)) {
                return true;
            }
        }
        return false;
    }

    public void storeclanswars(int clanId1, int clanId2) {
        L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
        L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
        clan1.setEnemyClan(clan2);
        clan2.setAttackerClan(clan1);
        clan1.broadcastClanStatus();
        clan2.broadcastClanStatus();
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2, wantspeace1, wantspeace2) VALUES(?,?,?,?)");
            st.setInt(1, clanId1);
            st.setInt(2, clanId2);
            st.setInt(3, 0);
            st.setInt(4, 0);
            st.execute();
        } catch (Exception e) {
            _log.warning("could not store clans wars data:" + e);
        } finally {
            Close.CS(con, st);
        }

        clan2.broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.CLAN_S1_DECLARED_WAR).addString(clan1.getName()));
        clan1.broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.CLAN_WAR_DECLARED_AGAINST_S1_IF_KILLED_LOSE_LOW_EXP).addString(clan2.getName()));
    }

    public void deleteclanswars(int clanId1, int clanId2) {
        L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
        L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
        clan1.deleteEnemyClan(clan2);
        clan2.deleteAttackerClan(clan1);
        clan1.broadcastClanStatus();
        clan2.broadcastClanStatus();
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
            st.setInt(1, clanId1);
            st.setInt(2, clanId2);
            st.execute();
        } catch (Exception e) {
            _log.warning("could not delete clans wars data:" + e);
        } finally {
            Close.CS(con, st);
        }

        clan1.broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED).addString(clan2.getName()));
        clan2.broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.CLAN_S1_HAS_DECIDED_TO_STOP).addString(clan1.getName()));
    }

    public void checkSurrender(L2Clan clan1, L2Clan clan2) {
        int count = 0;
        for (L2ClanMember player : clan1.getMembers()) {
            if (player != null && player.getPlayerInstance().getWantsPeace() == 1) {
                count++;
            }
        }
        if (count == clan1.getMembers().length - 1) {
            clan1.deleteEnemyClan(clan2);
            clan2.deleteEnemyClan(clan1);
            deleteclanswars(clan1.getClanId(), clan2.getClanId());
        }
    }

    private void restorewars() {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            L2Clan clan1 = null;
            L2Clan clan2 = null;

            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT clan1, clan2, wantspeace1, wantspeace2 FROM clan_wars");
            rset = st.executeQuery();
            rset.setFetchSize(50);
            while (rset.next()) {
                clan1 = getClan(rset.getInt("clan1"));
                clan2 = getClan(rset.getInt("clan2"));
                if (clan1 == null || clan2 == null) {
                    continue;
                }
                clan1.setEnemyClan(rset.getInt("clan2"));
                clan2.setAttackerClan(rset.getInt("clan1"));
            }
        } catch (Exception e) {
            _log.warning("ClanTable [ERROR]: could not restore clan wars:" + e);
        } finally {
            Close.CSR(con, st, rset);
        }
    }

    public L2Clan getClanByCharId(int objId) {
        int clanId = 0;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT clanid FROM `characters` WHERE `obj_Id` = ? LIMIT 0,1");
            st.setInt(1, objId);
            rset = st.executeQuery();
            if (rset.next()) {
                clanId = rset.getInt("clanid");
            }
        } catch (final Exception e) {
            _log.warning("[ERROR] ClanTable, getClanByCharId() error: " + e);
        } finally {
            Close.CSR(con, st, rset);
        }
        return getClan(clanId);
    }
}
