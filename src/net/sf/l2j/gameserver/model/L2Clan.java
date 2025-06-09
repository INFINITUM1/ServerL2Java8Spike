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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CrownManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import scripts.communitybbs.BB.Forum;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.4.2.7 $ $Date: 2005/04/06 16:13:41 $
 */
public class L2Clan {

    private static final Logger _log = Logger.getLogger(L2Clan.class.getName());
    private String _name;
    private int _clanId;
    private L2ClanMember _leader;
    private Map<Integer, L2ClanMember> _members = new ConcurrentHashMap<Integer, L2ClanMember>();
    private String _allyName;
    private int _allyId;
    private int _level;
    private int _hasCastle;
    private int _hasHideout;
    private boolean _hasCrest;
    private int _hiredGuards;
    private int _crestId;
    private int _crestLargeId;
    private int _allyCrestId;
    private int _auctionBiddedAt = 0;
    private long _allyPenaltyExpiryTime;
    private int _allyPenaltyType;
    private long _charPenaltyExpiryTime;
    private long _dissolvingExpiryTime;
    // Ally Penalty Types
    /**
     * Clan leaved ally
     */
    public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
    /**
     * Clan was dismissed from ally
     */
    public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
    /**
     * Leader clan dismiss clan from ally
     */
    public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
    /**
     * Leader clan dissolve ally
     */
    public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;
    private ItemContainer _warehouse = new ClanWarehouse(this);
    private ConcurrentLinkedQueue<Integer> _atWarWith = new ConcurrentLinkedQueue<Integer>();
    private ConcurrentLinkedQueue<Integer> _atWarAttackers = new ConcurrentLinkedQueue<Integer>();
    private boolean _hasCrestLarge;
    private Forum _forum;
    private List<L2Skill> _skillList = new FastList<L2Skill>();
    //  Clan Privileges
    /**
     * No privilege to manage any clan activity
     */
    public static final int CP_NOTHING = 0;
    /**
     * Privilege to join clan
     */
    public static final int CP_CL_JOIN_CLAN = 2;
    /**
     * Privilege to give a title
     */
    public static final int CP_CL_GIVE_TITLE = 4;
    /**
     * Privilege to view warehouse content
     */
    public static final int CP_CL_VIEW_WAREHOUSE = 8;
    /**
     * Privilege to manage clan ranks
     */
    public static final int CP_CL_MANAGE_RANKS = 16;
    public static final int CP_CL_PLEDGE_WAR = 32;
    public static final int CP_CL_DISMISS = 64;
    /**
     * Privilege to register clan crest
     */
    public static final int CP_CL_REGISTER_CREST = 128;
    public static final int CP_CL_MASTER_RIGHTS = 256;
    public static final int CP_CL_MANAGE_LEVELS = 512;
    /**
     * Privilege to open a door
     */
    public static final int CP_CH_OPEN_DOOR = 1024;
    public static final int CP_CH_OTHER_RIGHTS = 2048;
    public static final int CP_CH_AUCTION = 4096;
    public static final int CP_CH_DISMISS = 8192;
    public static final int CP_CH_SET_FUNCTIONS = 16384;
    public static final int CP_CS_OPEN_DOOR = 32768;
    public static final int CP_CS_MANOR_ADMIN = 65536;
    public static final int CP_CS_MANAGE_SIEGE = 131072;
    public static final int CP_CS_USE_FUNCTIONS = 262144;
    public static final int CP_CS_DISMISS = 524288;
    public static final int CP_CS_TAXES = 1048576;
    public static final int CP_CS_MERCENARIES = 2097152;
    public static final int CP_CS_SET_FUNCTIONS = 4194304;
    /**
     * Privilege to manage all clan activity
     */
    public static final int CP_ALL = 8388606;
    // Sub-unit types
    /**
     * Clan subunit type of Academy
     */
    public static final int SUBUNIT_ACADEMY = -1;
    /**
     * Clan subunit type of Royal Guard A
     */
    public static final int SUBUNIT_ROYAL1 = 100;
    /**
     * Clan subunit type of Royal Guard B
     */
    public static final int SUBUNIT_ROYAL2 = 200;
    /**
     * Clan subunit type of Order of Knights A-1
     */
    public static final int SUBUNIT_KNIGHT1 = 1001;
    /**
     * Clan subunit type of Order of Knights A-2
     */
    public static final int SUBUNIT_KNIGHT2 = 1002;
    /**
     * Clan subunit type of Order of Knights B-1
     */
    public static final int SUBUNIT_KNIGHT3 = 2001;
    /**
     * Clan subunit type of Order of Knights B-2
     */
    public static final int SUBUNIT_KNIGHT4 = 2002;
    /**
     * FastMap(Integer, L2Skill) containing all skills of the L2Clan
     */
    protected final Map<Integer, L2Skill> _skills = new FastMap<Integer, L2Skill>();
    protected final Map<Integer, RankPrivs> _privs = new FastMap<Integer, RankPrivs>();
    protected final Map<Integer, SubPledge> _subPledges = new FastMap<Integer, SubPledge>();
    private int _reputationScore = 0;
    private int _rank = 0;
    //
    private boolean _disableBonus = false;
    private L2Skill _siegeBonus = null;

    /**
     * Called if a clan is referenced only by id. In this case all other data
     * needs to be fetched from db
     *
     * @param clanId A valid clan Id to create and restore
     */
    public L2Clan(int clanId) {
        _clanId = clanId;
        initializePrivs();
        restore();
        getWarehouse().restore();
    }

    /**
     * Called only if a new clan is created
     *
     * @param clanId A valid clan Id to create
     * @param clanName A valid clan name
     */
    public L2Clan(int clanId, String clanName) {
        _clanId = clanId;
        _name = clanName;
        initializePrivs();
    }

    /**
     * @return Returns the clanId.
     */
    public int getClanId() {
        return _clanId;
    }

    /**
     * @param clanId The clanId to set.
     */
    public void setClanId(int clanId) {
        _clanId = clanId;
    }

    /**
     * @return Returns the leaderId.
     */
    public int getLeaderId() {
        return (_leader != null ? _leader.getObjectId() : 0);
    }

    /**
     * @return L2ClanMember of clan leader.
     */
    public L2ClanMember getLeader() {
        return _leader;
    }

    /**
     * @param leaderId The leaderId to set.
     */
    public void setLeader(L2ClanMember leader) {
        _leader = leader;
        _members.put(leader.getObjectId(), leader);
    }

    public void setNewLeader(L2ClanMember member) {
        if (!getLeader().isOnline()) {
            return;
        }
        if (member == null) {
            return;
        }
        if (!member.isOnline()) {
            return;
        }

        L2PcInstance exLeader = getLeader().getPlayerInstance();
        SiegeManager.getInstance().removeSiegeSkills(exLeader);
        exLeader.setClan(this);
        exLeader.setClanPrivileges(L2Clan.CP_NOTHING);
        exLeader.broadcastUserInfo();

        setLeader(member);
        updateClanInDB();

        exLeader.setPledgeClass(exLeader.getClan().getClanMember(exLeader.getObjectId()).calculatePledgeClass(exLeader));
        exLeader.broadcastUserInfo();

        L2PcInstance newLeader = member.getPlayerInstance();
        newLeader.setClan(this);
        newLeader.setPledgeClass(member.calculatePledgeClass(newLeader));
        newLeader.setClanPrivileges(L2Clan.CP_ALL);
        if (getLevel() >= 4) {
            SiegeManager.getInstance().addSiegeSkills(newLeader);
        }
        newLeader.broadcastUserInfo();

        broadcastClanStatus();
        broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_S1).addString(newLeader.getName()));

        CrownManager.getInstance().checkCrowns(exLeader);
        CrownManager.getInstance().checkCrowns(newLeader);
    }

    /**
     * @return Returns the leaderName.
     */
    public String getLeaderName() {
        return (_leader != null ? _leader.getName() : "");
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return _name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        _name = name;
    }

    private void addClanMember(L2ClanMember member) {
        _members.put(member.getObjectId(), member);
    }

    public void addClanMember(L2PcInstance player) {
        L2ClanMember member = new L2ClanMember(this, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle());
        // store in memory
        addClanMember(member);
        member.setPlayerInstance(player);
        player.setClan(this);
        player.setPledgeClass(member.calculatePledgeClass(player));
        player.sendPacket(new PledgeShowMemberListUpdate(player));
        player.sendPacket(new UserInfo(player));

        addBonusEffects(player, true);
    }

    public void updateClanMember(L2PcInstance player) {
        L2ClanMember member = new L2ClanMember(player);

        addClanMember(member);
    }

    public void updateClanMember(L2PcInstance player, boolean changeName) {
        L2ClanMember member = new L2ClanMember(player);
        if (getLeaderId() == player.getObjectId()) {
            L2PcInstance exLeader = getLeader().getPlayerInstance();
            SiegeManager.getInstance().removeSiegeSkills(exLeader);
            exLeader.setClan(this);
            exLeader.setClanPrivileges(L2Clan.CP_NOTHING);

            setLeader(member);
            //updateClanInDB();

            //exLeader.setPledgeClass(exLeader.getClan().getClanMember(exLeader.getObjectId()).calculatePledgeClass(exLeader));
            //exLeader.broadcastUserInfo();
            L2PcInstance newLeader = member.getPlayerInstance();
            //newLeader.setClan(this);
            newLeader.setPledgeClass(member.calculatePledgeClass(newLeader));
            newLeader.setClanPrivileges(L2Clan.CP_ALL);
            if (getLevel() >= 4) {
                SiegeManager.getInstance().addSiegeSkills(newLeader);
            }
            //newLeader.broadcastUserInfo();
            player.sendPacket(new PledgeShowMemberListUpdate(player));
            broadcastClanStatus();
            return;
        }
        addClanMember(member);
        // store in memory
        member.setPlayerInstance(player);
        player.setClan(this);
        player.setPledgeClass(member.calculatePledgeClass(player));
        player.sendPacket(new PledgeShowMemberListUpdate(player));
        broadcastClanStatus();
    }

    public L2ClanMember getClanMember(String name) {
        for (L2ClanMember temp : _members.values()) {
            if (temp == null) {
                continue;
            }

            if (name.equalsIgnoreCase(temp.getName())) {
                return temp;
            }
        }
        return null;
    }

    public L2ClanMember getClanMember(int objectID) {
        return _members.get(objectID);
    }

    public void removeClanMember(String name, long clanJoinExpiryTime) {
        L2ClanMember exMember = getClanMember(name);
        if (exMember == null) {
            _log.warning("Member " + name + " not found in clan while trying to remove");
            return;
        }
        _members.remove(exMember.getObjectId());

        int leadssubpledge = getLeaderSubPledge(name);
        if (leadssubpledge != 0) {
            // Sub-unit leader withdraws, position becomes vacant and leader
            // should appoint new via NPC
            getSubPledge(leadssubpledge).setLeaderName("");
            updateSubPledgeInDB(leadssubpledge);
        }

        if (exMember.getApprentice() != 0) {
            L2ClanMember apprentice = getClanMember(exMember.getApprentice());
            if (apprentice != null) {
                if (apprentice.getPlayerInstance() != null) {
                    apprentice.getPlayerInstance().setSponsor(0);
                } else {
                    apprentice.initApprenticeAndSponsor(0, 0);
                }

                apprentice.saveApprenticeAndSponsor(0, 0);
            }
        }
        if (exMember.getSponsor() != 0) {
            L2ClanMember sponsor = getClanMember(exMember.getSponsor());
            if (sponsor != null) {
                if (sponsor.getPlayerInstance() != null) {
                    sponsor.getPlayerInstance().setApprentice(0);
                } else {
                    sponsor.initApprenticeAndSponsor(0, 0);
                }

                sponsor.saveApprenticeAndSponsor(0, 0);
            }
        }
        exMember.saveApprenticeAndSponsor(0, 0);
        if (Config.REMOVE_CASTLE_CIRCLETS) {
            try {
                CastleManager.getInstance().removeCirclet(exMember, getHasCastle());
            } catch (Exception e) {
            }
        }
        if (exMember.isOnline()) {
            L2PcInstance player = exMember.getPlayerInstance();
            player.setApprentice(0);
            player.setSponsor(0);

            if (player.isClanLeader()) {
                SiegeManager.getInstance().removeSiegeSkills(player);
                player.setClanCreateExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L); //24*60*60*1000 = 86400000
            }
            for (L2Skill skill : getAllSkills()) {
                player.removeSkill(skill, false);
            }

            player.setClan(null);
            player.setClanJoinExpiryTime(clanJoinExpiryTime);
            player.setPledgeClass(exMember.calculatePledgeClass(player));
            player.sendSkillList();
            player.broadcastUserInfo();
            // disable clan tab
            player.sendPacket(new PledgeShowMemberListDeleteAll());
        } else {
            removeMemberInDatabase(exMember, clanJoinExpiryTime, getLeaderName().equalsIgnoreCase(name) ? System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L : 0);
        }
    }

    public L2ClanMember[] getMembers() {
        return _members.values().toArray(new L2ClanMember[_members.size()]);
    }

    public int getMembersCount() {
        return _members.size();
    }

    public int getSubPledgeMembersCount(int subpl) {
        int result = 0;
        for (L2ClanMember temp : _members.values()) {
            if (temp == null) {
                continue;
            }
            if (temp.getPledgeType() == subpl) {
                result++;
            }
        }
        return result;
    }

    public int getMaxNrOfMembers(int pledgetype) {
        int limit = 0;

        switch (pledgetype) {
            case 0:
                switch (getLevel()) {
                    case 4:
                        limit = 40;
                        break;
                    case 3:
                        limit = 30;
                        break;
                    case 2:
                        limit = 20;
                        break;
                    case 1:
                        limit = 15;
                        break;
                    case 0:
                        limit = 10;
                        break;
                    default:
                        limit = 40;
                        break;
                }
                break;
            case -1:
            case 100:
            case 200:
                limit = 20;
                break;
            case 1001:
            case 1002:
            case 2001:
            case 2002:
                limit = 10;
                break;
            default:
                break;
        }

        return limit;
    }

    public L2PcInstance[] getOnlineMembers(String exclude) {
        List<L2PcInstance> result = new FastList<L2PcInstance>();
        for (L2ClanMember temp : _members.values()) {
            if (temp == null) {
                continue;
            }
            if (temp.isOnline() && !temp.getName().equals(exclude)) {
                result.add(temp.getPlayerInstance());
            }
        }

        return result.toArray(new L2PcInstance[result.size()]);

    }

    /**
     * @return
     */
    public int getAllyId() {
        return _allyId;
    }

    /**
     * @return
     */
    public String getAllyName() {
        return _allyName;
    }

    public void setAllyCrestId(int allyCrestId) {
        _allyCrestId = allyCrestId;
    }

    /**
     * @return
     */
    public int getAllyCrestId() {
        return _allyCrestId;
    }

    /**
     * @return
     */
    public int getLevel() {
        return _level;
    }

    /**
     * @return
     */
    public int getHasCastle() {
        return _hasCastle;
    }

    /**
     * @return
     */
    public int getHasHideout() {
        return _hasHideout;
    }

    /**
     * @param crestId The id of pledge crest.
     */
    public void setCrestId(int id) {
        _crestId = id;
    }

    /**
     * @return Returns the clanCrestId.
     */
    public int getCrestId() {
        return _crestId;
    }

    /**
     * @param crestLargeId The id of pledge LargeCrest.
     */
    public void setCrestLargeId(int id) {
        _crestLargeId = id;
    }

    /**
     * @return Returns the clan CrestLargeId
     */
    public int getCrestLargeId() {
        return _crestLargeId;
    }

    /**
     * @param allyId The allyId to set.
     */
    public void setAllyId(int id) {
        _allyId = id;
    }

    /**
     * @param allyName The allyName to set.
     */
    public void setAllyName(String allyName) {
        _allyName = allyName;
    }

    /**
     * @param hasCastle The hasCastle to set.
     */
    public void setHasCastle(int id) {
        _hasCastle = id;
    }

    /**
     * @param hasHideout The hasHideout to set.
     */
    public void setHasHideout(int id) {
        _hasHideout = id;

        if (isEliteCH(id)) {
            addPoints(300);
            broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.CLAN_ACQUIRED_CONTESTED_CLAN_HALL_AND_S1_REPUTATION_POINTS).addNumber(300));
        }
    }

    private boolean isEliteCH(int id) {
        switch (id) {
            case 21:
            case 34:
            case 35:
            case 64:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param level The level to set.
     */
    public void setLevel(int level) {
        _level = level;
        /*
         * if(_forum == null) { if(_level >= 2) { _forum =
         * ForumsBBSManager.getInstance().getForumByName("ClanRoot").getChildByName(_name);
         * if(_forum == null) { _forum =
         * ForumsBBSManager.getInstance().createNewForum(_name,ForumsBBSManager.getInstance().getForumByName("ClanRoot"),Forum.CLAN,Forum.CLANMEMBERONLY,getClanId());
         * } } }
         */
    }

    /**
     * @param player name
     * @return
     */
    public boolean isMember(String name) {
        //return (name == null ? false : _members.containsKey(name));
        return getClanMember(name) != null;
    }

    public boolean isMember(int objId) {
        //return (name == null ? false : _members.containsKey(name));
        return _members.containsKey(objId);
    }

    public void updateClanInDB() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=? WHERE clan_id=?");
            statement.setInt(1, getLeaderId());
            statement.setInt(2, getAllyId());
            statement.setString(3, getAllyName());
            statement.setInt(4, getReputationScore());
            statement.setLong(5, getAllyPenaltyExpiryTime());
            statement.setInt(6, getAllyPenaltyType());
            statement.setLong(7, getCharPenaltyExpiryTime());
            statement.setLong(8, getDissolvingExpiryTime());
            statement.setInt(9, getClanId());
            statement.execute();
        } catch (Exception e) {
            _log.warning("error while saving new clan leader to db " + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    public void store() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id) values (?,?,?,?,?,?,?,?,?,?)");
            statement.setInt(1, getClanId());
            statement.setString(2, getName());
            statement.setInt(3, getLevel());
            statement.setInt(4, getHasCastle());
            statement.setInt(5, getAllyId());
            statement.setString(6, getAllyName());
            statement.setInt(7, getLeaderId());
            statement.setInt(8, getCrestId());
            statement.setInt(9, getCrestLargeId());
            statement.setInt(10, getAllyCrestId());
            statement.execute();
        } catch (Exception e) {
            _log.warning("error while saving new clan to db " + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    private void removeMemberInDatabase(L2ClanMember member, long clanJoinExpiryTime, long clanCreateExpiryTime) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE obj_Id=?");
            statement.setString(1, "");
            statement.setLong(2, clanJoinExpiryTime);
            statement.setLong(3, clanCreateExpiryTime);
            statement.setInt(4, member.getObjectId());
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
            statement.setInt(1, member.getObjectId());
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
            statement.setInt(1, member.getObjectId());
            statement.execute();
            Close.S(statement);
        } catch (Exception e) {
            _log.warning("error while removing clan member in db " + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    @SuppressWarnings("unused")
    private void updateWarsInDB() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE clan_wars SET wantspeace1=? WHERE clan1=?");
            statement.setInt(1, 0);
            statement.setInt(2, 0);
        } catch (Exception e) {
            _log.warning("could not update clans wars data:" + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    private void restore() {
        //restorewars();
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet clanData = null;
        ResultSet clanMembers = null;
        ResultSet rs3 = null;
        PreparedStatement statement2 = null;
        PreparedStatement st3 = null;
        try {
            L2ClanMember member;

            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,reputation_score,auction_bid_at,ally_penalty_expiry_time,ally_penalty_type,char_penalty_expiry_time,dissolving_expiry_time FROM clan_data where clan_id=?");
            statement.setInt(1, getClanId());
            clanData = statement.executeQuery();

            if (clanData.next()) {
                setName(clanData.getString("clan_name"));
                setLevel(clanData.getInt("clan_level"));
                setHasCastle(clanData.getInt("hasCastle"));
                setAllyId(clanData.getInt("ally_id"));
                setAllyName(clanData.getString("ally_name"));
                setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));
                if (getAllyPenaltyExpiryTime() < System.currentTimeMillis()) {
                    setAllyPenaltyExpiryTime(0, 0);
                }
                setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));
                if (getCharPenaltyExpiryTime() + Config.ALT_CLAN_JOIN_DAYS * 86400000L < System.currentTimeMillis()) //24*60*60*1000 = 86400000
                {
                    setCharPenaltyExpiryTime(0);
                }
                setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));

                setCrestId(clanData.getInt("crest_id"));
                if (getCrestId() != 0) {
                    setHasCrest(true);
                }

                setCrestLargeId(clanData.getInt("crest_large_id"));
                if (getCrestLargeId() != 0) {
                    setHasCrestLarge(true);
                }

                setAllyCrestId(clanData.getInt("ally_crest_id"));
                setReputationScore(clanData.getInt("reputation_score"), false);
                setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);

                int leaderId = (clanData.getInt("leader_id"));

                //statement2 = con.prepareStatement("SELECT char_name,level,classid,obj_Id,title,power_grade,subpledge,apprentice,sponsor,premium FROM characters WHERE clanid=?");
                statement2 = con.prepareStatement("SELECT char_name,level,classid,obj_Id,title,power_grade,subpledge,apprentice,sponsor,premium FROM characters WHERE clanid=?");
                statement2.setInt(1, getClanId());
                clanMembers = statement2.executeQuery();

                while (clanMembers.next()) {
                    // гилд мод / Storium
                    int side = 0;
                    if (Config.ALLOW_GUILD_MOD) {
                        try {
                            st3 = con.prepareStatement("SELECT side FROM z_guild_mod WHERE char_id=? LIMIT 1");
                            st3.setInt(1, clanMembers.getInt("obj_id"));
                            rs3 = st3.executeQuery();
                            if (rs3.next()) {
                                side = rs3.getInt("side");
                            }
                        } catch (SQLException e) {
                            _log.severe("L2Clan Oops3" + e);
                        } finally {
                            Close.SR(st3, rs3);
                        }
                    }

                    // { прибить, как перепишу хранение на obj_id
                    String name = clanMembers.getString("char_name");
                    if (Config.ALLOW_GUILD_MOD && side > 0) {
                        name = Config.GUILD_MOD_NAMES.get(side) + name;
                    }
                    if (clanMembers.getLong("premium") > 0) {
                        name = name + Config.PREMIUM_NAME_PREFIX;
                    }
                    // прибить, как перепишу хранение на obj_id }

                    member = new L2ClanMember(this, name, clanMembers.getInt("level"), clanMembers.getInt("classid"), clanMembers.getInt("obj_id"), clanMembers.getInt("subpledge"), clanMembers.getInt("power_grade"), clanMembers.getString("title"));
                    if (member.getObjectId() == leaderId) {
                        setLeader(member);
                    } else {
                        addClanMember(member);
                    }
                    member.initApprenticeAndSponsor(clanMembers.getInt("apprentice"), clanMembers.getInt("sponsor"));
                }
                Close.SR(statement2, clanMembers);
            }

            restoreSubPledges();
            restoreRankPrivs();
            restoreSkills();
            setBonusSkill();
        } catch (SQLException e) {
            _log.warning("error while restoring clan " + e);
        } finally {
            Close.SR(st3, rs3);
            Close.SR(statement2, clanMembers);
            Close.CSR(con, statement, clanData);
        }
    }

    private void restoreSkills() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try {
            // Retrieve all skills of this L2PcInstance from the database
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT skill_id,skill_level FROM clan_skills WHERE clan_id=?");
            statement.setInt(1, getClanId());

            rset = statement.executeQuery();

            // Go though the recordset of this SQL query
            while (rset.next()) {
                int id = rset.getInt("skill_id");
                int level = rset.getInt("skill_level");
                // Create a L2Skill object for each record
                L2Skill skill = SkillTable.getInstance().getInfo(id, level);
                // Add the L2Skill object to the L2Clan _skills
                _skills.put(skill.getId(), skill);
            }
        } catch (Exception e) {
            _log.warning("Could not restore clan skills: " + e);
        } finally {
            Close.CSR(con, statement, rset);
        }
    }

    /**
     * used to retrieve all skills
     */
    public final L2Skill[] getAllSkills() {
        if (_skills == null) {
            return new L2Skill[0];
        }

        return _skills.values().toArray(new L2Skill[_skills.values().size()]);
    }

    /**
     * used to add a skill to skill list of this L2Clan
     */
    public L2Skill addSkill(L2Skill newSkill) {
        L2Skill oldSkill = null;

        if (newSkill != null) {
            // Replace oldSkill by newSkill or Add the newSkill
            oldSkill = _skills.put(newSkill.getId(), newSkill);
        }

        return oldSkill;
    }

    /**
     * used to add a new skill to the list, send a packet to all online clan
     * members, update their stats and store it in db
     */
    public L2Skill addNewSkill(L2Skill newSkill) {
        L2Skill oldSkill = null;

        if (newSkill != null) {

            // Replace oldSkill by newSkill or Add the newSkill
            oldSkill = _skills.put(newSkill.getId(), newSkill);

            Connect con = null;
            PreparedStatement statement = null;
            try {
                con = L2DatabaseFactory.get();

                if (oldSkill != null) {
                    statement = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?");
                    statement.setInt(1, newSkill.getLevel());
                    statement.setInt(2, oldSkill.getId());
                    statement.setInt(3, getClanId());
                    statement.execute();
                    Close.S(statement);
                } else {
                    statement = con.prepareStatement("INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name) VALUES (?,?,?,?)");
                    statement.setInt(1, getClanId());
                    statement.setInt(2, newSkill.getId());
                    statement.setInt(3, newSkill.getLevel());
                    statement.setString(4, newSkill.getName());
                    statement.execute();
                    Close.S(statement);
                }
            } catch (Exception e) {
                _log.warning("Error could not store char skills: " + e);
            } finally {
                Close.CS(con, statement);
            }

            for (L2ClanMember temp : _members.values()) {
                if (temp == null) {
                    continue;
                }
                if (temp.isOnline()) {
                    if (newSkill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass()) {
                        temp.getPlayerInstance().addSkill(newSkill, false); // Skill is not saved to player DB
                        temp.getPlayerInstance().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
                    }
                }
            }
        }

        return oldSkill;
    }

    public void addSkillEffects() {
        for (L2Skill skill : _skills.values()) {
            if (skill == null) {
                continue;
            }
            for (L2ClanMember temp : _members.values()) {
                if (temp == null) {
                    continue;
                }
                if (temp.isOnline()) {
                    if (skill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass()) {
                        temp.getPlayerInstance().addSkill(skill, false); // Skill is not saved to player DB
                    }
                }
            }
        }
    }

    public void addSkillEffects(L2PcInstance cm) {
        if (cm == null) {
            return;
        }

        for (L2Skill skill : _skills.values()) {
            if (skill == null) {
                continue;
            }
            //TODO add skills according to members class( in ex. don't add Clan Agillity skill's effect to lower class then Baron)
            if (skill.getMinPledgeClass() <= cm.getPledgeClass()) {
                cm.addSkill(skill, false); // Skill is not saved to player DB
            }
        }
    }

    public void broadcastToOnlineAllyMembers(L2GameServerPacket packet) {
        if (getAllyId() == 0) {
            return;
        }

        FastTable<L2Clan> cn = new FastTable<L2Clan>();
        cn.addAll(ClanTable.getInstance().getClans());
        for (L2Clan clan : cn) {
            if (clan == null) {
                continue;
            }
            if (clan.getAllyId() == getAllyId()) {
                clan.broadcastToOnlineMembers(packet);
            }
        }
    }

    public void broadcastMessageToOnlineMembers(String message) {
        for (L2ClanMember member : _members.values()) {
            if (member == null) {
                continue;
            }
            if (member.isOnline()) {
                member.getPlayerInstance().sendMessage(message);
            }
        }
    }

    public void broadcastToOnlineMembers(L2GameServerPacket packet) {
        for (L2ClanMember member : _members.values()) {
            if (member == null) {
                continue;
            }
            if (member.isOnline()) {
                member.getPlayerInstance().sendPacket(packet);
            }
        }
    }

    public void broadcastToOtherOnlineMembers(L2GameServerPacket packet, L2PcInstance player) {
        for (L2ClanMember member : _members.values()) {
            if (member == null) {
                continue;
            }
            if (member.isOnline() && member.getPlayerInstance() != player) {
                member.getPlayerInstance().sendPacket(packet);
            }
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * @return
     */
    public boolean hasCrest() {
        return _hasCrest;
    }

    public boolean hasCrestLarge() {
        return _hasCrestLarge;
    }

    public void setHasCrest(boolean flag) {
        _hasCrest = flag;
    }

    public void setHasCrestLarge(boolean flag) {
        _hasCrestLarge = flag;
    }

    public ItemContainer getWarehouse() {
        return _warehouse;
    }

    public boolean isAtWarWith(int id) {
        return _atWarWith.contains(id);
    }

    public boolean isAtWarWith(L2Clan clan) {
        return isAtWarWith(clan.getClanId());
    }

    public boolean isAtWarAttacker(int id) {
        return _atWarAttackers.contains(id);
    }

    public void setEnemyClan(L2Clan clan) {
        setEnemyClan(clan.getClanId());
    }

    public void setEnemyClan(int id) {
        _atWarWith.add(id);
    }

    public void setAttackerClan(L2Clan clan) {
        setAttackerClan(clan.getClanId());
    }

    public void setAttackerClan(int id) {
        _atWarAttackers.add(id);
    }

    public void deleteEnemyClan(L2Clan clan) {
        _atWarWith.remove(clan.getClanId());
    }

    public void deleteAttackerClan(L2Clan clan) {
        _atWarAttackers.remove(clan.getClanId());
    }

    public int getHiredGuards() {
        return _hiredGuards;
    }

    public void incrementHiredGuards() {
        _hiredGuards++;
    }

    public int isAtWar() {
        if (_atWarWith.isEmpty()) {
            return 0;
        }

        return 1;
    }

    public ConcurrentLinkedQueue<Integer> getWarList() {
        return _atWarWith;
    }

    public ConcurrentLinkedQueue<Integer> getAttackerList() {
        return _atWarAttackers;
    }

    public void broadcastClanStatus() {
        for (L2PcInstance member : getOnlineMembers("")) {
            member.sendPacket(new PledgeShowMemberListDeleteAll());
            member.sendPacket(new PledgeShowMemberListAll(this, member));
        }
    }

    public void removeSkill(int id) {
        L2Skill deleteSkill = null;
        for (L2Skill sk : _skillList) {
            if (sk == null) {
                continue;
            }
            if (sk.getId() == id) {
                deleteSkill = sk;
                break;
            }
        }
        _skillList.remove(deleteSkill);
    }

    public void removeSkill(L2Skill deleteSkill) {
        _skillList.remove(deleteSkill);
    }

    /**
     * @return
     */
    public List<L2Skill> getSkills() {
        return _skillList;
    }

    public static class SubPledge {

        private int _id;
        private String _subPledgeName;
        private String _leaderName;

        public SubPledge(int id, String name, String leaderName) {
            _id = id;
            _subPledgeName = name;
            _leaderName = leaderName;
        }

        public int getId() {
            return _id;
        }

        public String getName() {
            return _subPledgeName;
        }

        public String getLeaderName() {
            return _leaderName;
        }

        public void setLeaderName(String leaderName) {
            _leaderName = leaderName;
        }
    }

    public static class RankPrivs {

        private int _rankId;
        private int _party;// TODO find out what this stuff means and implement it
        private int _rankPrivs;

        public RankPrivs(int rank, int party, int privs) {
            _rankId = rank;
            _party = party;
            _rankPrivs = privs;
        }

        public int getRank() {
            return _rankId;
        }

        public int getParty() {
            return _party;
        }

        public int getPrivs() {
            return _rankPrivs;
        }

        public void setPrivs(int privs) {
            _rankPrivs = privs;
        }
    }

    private void restoreSubPledges() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try {
            // Retrieve all subpledges of this clan from the database
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT sub_pledge_id,name,leader_name FROM clan_subpledges WHERE clan_id=?");
            statement.setInt(1, getClanId());
            rset = statement.executeQuery();

            while (rset.next()) {
                int id = rset.getInt("sub_pledge_id");
                String name = rset.getString("name");
                String leaderName = rset.getString("leader_name");
                // Create a SubPledge object for each record
                SubPledge pledge = new SubPledge(id, name, leaderName);
                _subPledges.put(id, pledge);
            }
        } catch (Exception e) {
            _log.warning("Could not restore clan sub-units: " + e);
        } finally {
            Close.CSR(con, statement, rset);
        }
    }

    /**
     * used to retrieve subPledge by type
     */
    public final SubPledge getSubPledge(int pledgeType) {
        if (_subPledges == null) {
            return null;
        }

        return _subPledges.get(pledgeType);
    }

    /**
     * used to retrieve subPledge by type
     */
    public final SubPledge getSubPledge(String pledgeName) {
        if (_subPledges == null) {
            return null;
        }

        for (SubPledge sp : _subPledges.values()) {
            if (sp.getName().equalsIgnoreCase(pledgeName)) {
                return sp;
            }
        }
        return null;
    }

    /**
     * used to retrieve all subPledges
     */
    public final SubPledge[] getAllSubPledges() {
        if (_subPledges == null) {
            return new SubPledge[0];
        }

        return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
    }

    public SubPledge createSubPledge(L2PcInstance player, int pledgeType, String leaderName, String subPledgeName) {
        SubPledge subPledge = null;
        pledgeType = getAvailablePledgeTypes(pledgeType);
        if (pledgeType == 0) {
            if (pledgeType == L2Clan.SUBUNIT_ACADEMY) {
                player.sendPacket(Static.CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
            } else {
                player.sendPacket(Static.FULL_PLEDGE);
            }
            return null;
        }
        if (_leader.getName().equals(leaderName)) {
            player.sendPacket(Static.KEADER_NOT_CORRECT);
            return null;
        }

        // Royal Guard 5000 points per each
        // Order of Knights 10000 points per each
        if (pledgeType != -1
                && ((getReputationScore() < 5000 && pledgeType < L2Clan.SUBUNIT_KNIGHT1)
                || (getReputationScore() < 10000 && pledgeType > L2Clan.SUBUNIT_ROYAL2))) {
            player.sendPacket(Static.CLAN_REPUTATION_SCORE_IS_TOO_LOW);
            return null;
        } else {
            Connect con = null;
            PreparedStatement statement = null;
            try {
                con = L2DatabaseFactory.get();
                statement = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_name) values (?,?,?,?)");
                statement.setInt(1, getClanId());
                statement.setInt(2, pledgeType);
                statement.setString(3, subPledgeName);
                if (pledgeType != -1) {
                    statement.setString(4, leaderName);
                } else {
                    statement.setString(4, "");
                }
                statement.execute();

                subPledge = new SubPledge(pledgeType, subPledgeName, leaderName);
                _subPledges.put(pledgeType, subPledge);

                if (pledgeType != -1) {
                    setReputationScore(getReputationScore() - 2500, true);
                }
            } catch (Exception e) {
                _log.warning("error while saving new sub_clan to db " + e);
            } finally {
                Close.CS(con, statement);
            }
        }
        broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()));
        broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge));
        return subPledge;
    }

    public int getAvailablePledgeTypes(int pledgeType) {
        if (_subPledges.get(pledgeType) != null) {
            //_log.warning("found sub-unit with id: "+pledgeType);
            switch (pledgeType) {
                case SUBUNIT_ACADEMY:
                    return 0;
                case SUBUNIT_ROYAL1:
                    pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
                    break;
                case SUBUNIT_ROYAL2:
                    return 0;
                case SUBUNIT_KNIGHT1:
                    pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
                    break;
                case SUBUNIT_KNIGHT2:
                    pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
                    break;
                case SUBUNIT_KNIGHT3:
                    pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
                    break;
                case SUBUNIT_KNIGHT4:
                    return 0;
            }
        }
        return pledgeType;
    }

    public void updateSubPledgeInDB(int pledgeType) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE clan_subpledges SET leader_name=? WHERE clan_id=? AND sub_pledge_id=?");
            statement.setString(1, getSubPledge(pledgeType).getLeaderName());
            statement.setInt(2, getClanId());
            statement.setInt(3, pledgeType);
            statement.execute();
        } catch (Exception e) {
            _log.warning("error while saving new clan leader to db " + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    private void restoreRankPrivs() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try {
            // Retrieve all skills of this L2PcInstance from the database
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT privs,rank,party FROM clan_privs WHERE clan_id=?");
            statement.setInt(1, getClanId());
            //_log.warning("clanPrivs restore for ClanId : "+getClanId());
            rset = statement.executeQuery();

            // Go though the recordset of this SQL query
            while (rset.next()) {
                int rank = rset.getInt("rank");
                //int party = rset.getInt("party");
                int privileges = rset.getInt("privs");
                // Create a SubPledge object for each record
                //RankPrivs privs = new RankPrivs(rank, party, privileges);
                //_Privs.put(rank, privs);
                _privs.get(rank).setPrivs(privileges);
            }
        } catch (Exception e) {
            _log.warning("Could not restore clan privs by rank: " + e);
        } finally {
            Close.CSR(con, statement, rset);
        }
    }

    public void initializePrivs() {
        RankPrivs privs;
        for (int i = 1; i < 10; i++) {
            privs = new RankPrivs(i, 0, CP_NOTHING);
            _privs.put(i, privs);
        }

    }

    public int getRankPrivs(int rank) {
        if (_privs.get(rank) != null) {
            return _privs.get(rank).getPrivs();
        } else {
            return CP_NOTHING;
        }
    }

    public void setRankPrivs(int rank, int privs) {
        if (_privs.get(rank) != null) {
            _privs.get(rank).setPrivs(privs);

            Connect con = null;
            PreparedStatement statement = null;
            try {
                //_log.warning("requested store clan privs in db for rank: "+rank+", privs: "+privs);
                // Retrieve all skills of this L2PcInstance from the database
                con = L2DatabaseFactory.get();
                statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privs) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE privs = ?");
                statement.setInt(1, getClanId());
                statement.setInt(2, rank);
                statement.setInt(3, 0);
                statement.setInt(4, privs);
                statement.setInt(5, privs);
                statement.execute();
            } catch (Exception e) {
                _log.warning("Could not store clan privs for rank: " + e);
            } finally {
                Close.CS(con, statement);
            }

            for (L2ClanMember cm : getMembers()) {
                if (cm == null) {
                    continue;
                }
                if (cm.isOnline()) {
                    if (cm.getPowerGrade() == rank) {
                        if (cm.getPlayerInstance() != null) {
                            cm.getPlayerInstance().setClanPrivileges(privs);
                            cm.getPlayerInstance().sendPacket(new UserInfo(cm.getPlayerInstance()));
                        }
                    }
                }
            }
            broadcastClanStatus();
        } else {
            _privs.put(rank, new RankPrivs(rank, 0, privs));

            Connect con = null;
            PreparedStatement statement = null;
            try {
                //_log.warning("requested store clan new privs in db for rank: "+rank);
                // Retrieve all skills of this L2PcInstance from the database
                con = L2DatabaseFactory.get();
                statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privs) VALUES (?,?,?,?)");
                statement.setInt(1, getClanId());
                statement.setInt(2, rank);
                statement.setInt(3, 0);
                statement.setInt(4, privs);
                statement.execute();
            } catch (Exception e) {
                _log.warning("Could not create new rank and store clan privs for rank: " + e);
            } finally {
                Close.CS(con, statement);
            }
        }
    }

    /**
     * used to retrieve all RankPrivs
     */
    public final RankPrivs[] getAllRankPrivs() {
        if (_privs == null) {
            return new RankPrivs[0];
        }

        return _privs.values().toArray(new RankPrivs[_privs.values().size()]);
    }

    public int getLeaderSubPledge(String name) {
        int id = 0;
        for (SubPledge sp : _subPledges.values()) {
            if (sp == null) {
                continue;
            }
            if (sp.getLeaderName() == null) {
                continue;
            }
            if (sp.getLeaderName().equals(name)) {
                id = sp.getId();
            }
        }
        return id;
    }

    public void setReputationScore(int value, boolean save) {
        if (_reputationScore >= 0 && value < 0) {
            broadcastToOnlineMembers(Static.REPUTATION_POINTS_0_OR_LOWER_CLAN_SKILLS_DEACTIVATED);
            L2Skill[] skills = getAllSkills();
            for (L2ClanMember member : _members.values()) {
                if (member == null) {
                    continue;
                }
                if (member.isOnline() && member.getPlayerInstance() != null) {
                    for (L2Skill sk : skills) {
                        member.getPlayerInstance().removeSkill(sk, false);
                    }
                }
            }
        } else if (_reputationScore < 0 && value >= 0) {
            broadcastToOnlineMembers(Static.CLAN_SKILLS_WILL_BE_ACTIVATED_SINCE_REPUTATION_IS_0_OR_HIGHER);
            L2Skill[] skills = getAllSkills();
            for (L2ClanMember member : _members.values()) {
                if (member == null) {
                    continue;
                }
                if (member.isOnline() && member.getPlayerInstance() != null) {
                    for (L2Skill sk : skills) {
                        if (sk.getMinPledgeClass() <= member.getPlayerInstance().getPledgeClass()) {
                            member.getPlayerInstance().addSkill(sk, false);
                        }
                    }
                }
            }
        }

        _reputationScore = value;
        _reputationScore = Math.min(_reputationScore, 100000000);
        _reputationScore = Math.max(_reputationScore, -100000000);
        if (save) {
            updateClanInDB();
        }
    }

    public int getReputationScore() {
        return _reputationScore;
    }

    public void addPoints(int points) {
        if (Config.ALT_CLAN_REP_MUL > 1) {
            points *= Config.ALT_CLAN_REP_MUL;
        }

        setReputationScore((_reputationScore + points), true);
    }

    public void incWarPoints(int points) {
        setReputationScore((_reputationScore + points), true);
    }

    public void decWarPoints(int points) {
        setReputationScore((_reputationScore - points), true);
    }

    public void setRank(int rank) {
        _rank = rank;
    }

    public int getRank() {
        return _rank;
    }

    public int getAuctionBiddedAt() {
        return _auctionBiddedAt;
    }

    public void setAuctionBiddedAt(int id, boolean storeInDb) {
        _auctionBiddedAt = id;

        if (storeInDb) {
            Connect con = null;
            PreparedStatement statement = null;
            try {
                con = L2DatabaseFactory.get();
                statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
                statement.setInt(1, id);
                statement.setInt(2, getClanId());
                statement.execute();
            } catch (Exception e) {
                _log.warning("Could not store auction for clan: " + e);
            } finally {
                Close.CS(con, statement);
            }
        }
    }

    /**
     * Checks if activeChar and target meet various conditions to join a clan
     *
     * @param activeChar
     * @param target
     * @param pledgeType
     * @return
     */
    public boolean checkClanJoinCondition(L2PcInstance activeChar, L2PcInstance target, int pledgeType) {
        if (activeChar == null) {
            return false;
        }
        if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_JOIN_CLAN) != L2Clan.CP_CL_JOIN_CLAN) {
            activeChar.sendPacket(Static.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return false;
        }
        if (target == null) {
            activeChar.sendPacket(Static.YOU_HAVE_INVITED_THE_WRONG_TARGET);
            return false;
        }
        if (activeChar.getObjectId() == target.getObjectId()) {
            activeChar.sendPacket(Static.CANNOT_INVITE_YOURSELF);
            return false;
        }
        if (getCharPenaltyExpiryTime() > System.currentTimeMillis()) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER).addString(target.getName()));
            return false;
        }
        if (target.getClanId() != 0) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN).addString(target.getName()));
            return false;
        }
        if (target.getClanId() != 0) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN).addString(target.getName()));
            return false;
        }
        if (target.getClanJoinExpiryTime() > System.currentTimeMillis()) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN).addString(target.getName()));
            return false;
        }
        if (Config.ACADEMY_CLASSIC && ((target.getLevel() > 40 || target.getClassId().level() >= 2) && pledgeType == -1)) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_DOESNOT_MEET_REQUIREMENTS_TO_JOIN_ACADEMY).addString(target.getName()));
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.ACADEMY_REQUIREMENTS));
            return false;
        }
        if (getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType)) {
            if (pledgeType == 0) {
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_CLAN_IS_FULL).addString(getName()));
            } else {
                activeChar.sendPacket(Static.SUBCLAN_IS_FULL);
            }
            return false;
        }
        return true;
    }

    /**
     * Checks if activeChar and target meet various conditions to join a clan
     *
     * @param activeChar
     * @param target
     * @return
     */
    public boolean checkAllyJoinCondition(L2PcInstance activeChar, L2PcInstance target) {
        if (activeChar == null) {
            return false;
        }
        if (activeChar.getAllyId() == 0 || !activeChar.isClanLeader() || activeChar.getClanId() != activeChar.getAllyId()) {
            activeChar.sendPacket(Static.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
            return false;
        }
        L2Clan leaderClan = activeChar.getClan();
        if (leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis()) {
            if (leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN) {
                activeChar.sendPacket(Static.CANT_INVITE_CLAN_WITHIN_1_DAY);
                return false;
            }
        }
        if (target == null) {
            activeChar.sendPacket(Static.YOU_HAVE_INVITED_THE_WRONG_TARGET);
            return false;
        }
        if (activeChar.getObjectId() == target.getObjectId()) {
            activeChar.sendPacket(Static.CANNOT_INVITE_YOURSELF);
            return false;
        }
        if (target.getClan() == null) {
            activeChar.sendPacket(Static.TARGET_MUST_BE_IN_CLAN);
            return false;
        }
        if (!target.isClanLeader()) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(target.getName()));
            return false;
        }
        L2Clan targetClan = target.getClan();
        if (target.getAllyId() != 0) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE).addString(targetClan.getName()).addString(targetClan.getAllyName()));
            return false;
        }
        if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis()) {
            if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_LEAVED) {
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_CANT_ENTER_ALLIANCE_WITHIN_1_DAY).addString(target.getClan().getName()).addString(target.getClan().getAllyName()));
                return false;
            }
            if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_DISMISSED) {
                activeChar.sendPacket(Static.CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
                return false;
            }
        }
        if (activeChar.isInsideZone(L2Character.ZONE_SIEGE) && target.isInsideZone(L2Character.ZONE_SIEGE)) {
            activeChar.sendPacket(Static.OPPOSING_CLAN_IS_PARTICIPATING_IN_SIEGE);
            return false;
        }
        if (leaderClan.isAtWarWith(targetClan.getClanId())) {
            activeChar.sendPacket(Static.MAY_NOT_ALLY_CLAN_BATTLE);
            return false;
        }

        int numOfClansInAlly = 0;
        FastTable<L2Clan> cn = new FastTable<L2Clan>();
        cn.addAll(ClanTable.getInstance().getClans());
        for (L2Clan clan : cn) {
            if (clan == null) {
                continue;
            }
            if (clan.getAllyId() == activeChar.getAllyId()) {
                ++numOfClansInAlly;
            }
        }
        if (numOfClansInAlly >= Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY) {
            activeChar.sendPacket(Static.YOU_HAVE_EXCEEDED_THE_LIMIT);
            return false;
        }

        return true;
    }

    public long getAllyPenaltyExpiryTime() {
        return _allyPenaltyExpiryTime;
    }

    public int getAllyPenaltyType() {
        return _allyPenaltyType;
    }

    public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType) {
        _allyPenaltyExpiryTime = expiryTime;
        _allyPenaltyType = penaltyType;
    }

    public long getCharPenaltyExpiryTime() {
        return _charPenaltyExpiryTime;
    }

    public void setCharPenaltyExpiryTime(long time) {
        _charPenaltyExpiryTime = time;
    }

    public long getDissolvingExpiryTime() {
        return _dissolvingExpiryTime;
    }

    public void setDissolvingExpiryTime(long time) {
        _dissolvingExpiryTime = time;
    }

    public void createAlly(L2PcInstance player, String allyName) {
        if (null == player) {
            return;
        }

        //if (Config.DEBUG)
        //   _log.fine(player.getObjectId() + "(" + player.getName() + ") requested ally creation from ");
        if (!player.isClanLeader()) {
            player.sendPacket(Static.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
            return;
        }
        if (getAllyId() != 0) {
            player.sendPacket(Static.ALREADY_JOINED_ALLIANCE);
            return;
        }
        if (getLevel() < 5) {
            player.sendPacket(Static.TO_CREATE_AN_ALLY_YOU_CLAN_MUST_BE_LEVEL_5_OR_HIGHER);
            return;
        }
        if (getAllyPenaltyExpiryTime() > System.currentTimeMillis()) {
            if (getAllyPenaltyType() == L2Clan.PENALTY_TYPE_DISSOLVE_ALLY) {
                player.sendPacket(Static.CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION);
                return;
            }
        }
        if (getDissolvingExpiryTime() > System.currentTimeMillis()) {
            player.sendPacket(Static.YOU_MAY_NOT_CREATE_ALLY_WHILE_DISSOLVING);
            return;
        }
        if (!Util.isAlphaNumeric(allyName)) {
            player.sendPacket(Static.INCORRECT_ALLIANCE_NAME);
            return;
        }
        if (allyName.length() > 16 || allyName.length() < 2) {
            player.sendPacket(Static.INCORRECT_ALLIANCE_NAME_LENGTH);
            return;
        }
        if (ClanTable.getInstance().isAllyExists(allyName)) {
            player.sendPacket(Static.ALLIANCE_ALREADY_EXISTS);
            return;
        }

        setAllyId(getClanId());
        setAllyName(allyName.trim());
        setAllyPenaltyExpiryTime(0, 0);
        updateClanInDB();

        player.sendPacket(new UserInfo(player));

        //TODO: Need correct message id
        player.sendMessage("Alliance " + allyName + " has been created.");
    }

    public void dissolveAlly(L2PcInstance player) {
        if (getAllyId() == 0) {
            player.sendPacket(Static.NO_CURRENT_ALLIANCES);
            return;
        }
        if (!player.isClanLeader() || getClanId() != getAllyId()) {
            player.sendPacket(Static.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
            return;
        }
        if (player.isInsideZone(L2Character.ZONE_SIEGE)) {
            player.sendPacket(Static.CANNOT_DISSOLVE_ALLY_WHILE_IN_SIEGE);
            return;
        }

        broadcastToOnlineAllyMembers(Static.ALLIANCE_DISOLVED);

        long currentTime = System.currentTimeMillis();
        FastTable<L2Clan> cn = new FastTable<L2Clan>();
        cn.addAll(ClanTable.getInstance().getClans());
        for (L2Clan clan : cn) {
            if (clan == null) {
                continue;
            }
            if (clan.getAllyId() == getAllyId() && clan.getClanId() != getClanId()) {
                clan.setAllyId(0);
                clan.setAllyName(null);
                clan.setAllyPenaltyExpiryTime(0, 0);
                clan.updateClanInDB();
            }
        }

        setAllyId(0);
        setAllyName(null);
        setAllyPenaltyExpiryTime(
                currentTime + Config.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000L,
                L2Clan.PENALTY_TYPE_DISSOLVE_ALLY); //24*60*60*1000 = 86400000
        updateClanInDB();

        // The clan leader should take the XP penalty of a full death.
        player.deathPenalty(false);
    }

    private boolean canBeIncreased(L2PcInstance player, int sp, int item, int count) {
        if (Config.DISABLE_CLAN_REQUREMENTS) {
            return true;
        }

        if (player.getSp() >= sp && player.getInventory().getItemByItemId(item) != null) {
            if (player.destroyItemByItemId("ClanLvl", item, count, player.getTarget(), false)) {
                player.setSp(player.getSp() - sp);
                player.sendPacket(SystemMessage.id(SystemMessageId.SP_DECREASED_S1).addNumber(3500000));
                player.sendPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addItemName(item).addNumber(count));
                return true;
            }
        }
        return false;
    }

    public void levelUpClan(L2PcInstance player) {
        if (!player.isClanLeader()) {
            player.sendPacket(Static.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
        }
        if (System.currentTimeMillis() < getDissolvingExpiryTime()) {
            player.sendPacket(Static.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS);
            return;
        }

        boolean increaseClanLevel = false;

        switch (getLevel()) {
            case 0: {
                // upgrade to 1
                increaseClanLevel = canBeIncreased(player, 30000, 57, 650000);
                break;
            }
            case 1: {
                // upgrade to 2
                increaseClanLevel = canBeIncreased(player, 150000, 57, 2500000);
                break;
            }
            case 2: {
                // upgrade to 3
                increaseClanLevel = canBeIncreased(player, 500000, 1419, 1);
                break;
            }
            case 3: {
                // upgrade to 4
                increaseClanLevel = canBeIncreased(player, 1400000, 3874, 1);
                break;
            }
            case 4: {
                // upgrade to 5
                increaseClanLevel = canBeIncreased(player, 3500000, 3870, 1);
                break;
            }
            case 5:
                if (getReputationScore() >= Config.CLAN_REP_LEVEL6 && getMembersCount() >= Config.CLAN_CHAR_LEVEL6) {
                    setReputationScore(getReputationScore() - Config.CLAN_REP_LEVEL6, true);
                    player.sendPacket(SystemMessage.id(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(Config.CLAN_REP_LEVEL6));
                    increaseClanLevel = true;
                }
                break;
            case 6:
                if (getReputationScore() >= Config.CLAN_REP_LEVEL7 && getMembersCount() >= Config.CLAN_CHAR_LEVEL7) {
                    setReputationScore(getReputationScore() - Config.CLAN_REP_LEVEL7, true);
                    player.sendPacket(SystemMessage.id(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(Config.CLAN_REP_LEVEL7));
                    increaseClanLevel = true;
                }
                break;
            case 7:
                if (getReputationScore() >= Config.CLAN_REP_LEVEL8 && getMembersCount() >= Config.CLAN_CHAR_LEVEL8) {
                    setReputationScore(getReputationScore() - Config.CLAN_REP_LEVEL8, true);
                    player.sendPacket(SystemMessage.id(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(Config.CLAN_REP_LEVEL8));
                    increaseClanLevel = true;
                }
                break;
        }

        if (!increaseClanLevel) {
            player.sendPacket(Static.FAILED_TO_INCREASE_CLAN_LEVEL);
            return;
        }

        // the player should know that he has less sp now :p
        StatusUpdate su = new StatusUpdate(player.getObjectId());
        su.addAttribute(StatusUpdate.SP, player.getSp());
        player.sendPacket(su);

        player.sendItems(false);

        changeLevel(getLevel() + 1);
    }

    public void changeLevel(int level) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
            statement.setInt(1, level);
            statement.setInt(2, getClanId());
            statement.execute();
        } catch (Exception e) {
            _log.warning("could not increase clan level:" + e);
        } finally {
            Close.CS(con, statement);
        }

        setLevel(level);

        if (getLeader().isOnline()) {
            L2PcInstance leader = getLeader().getPlayerInstance();
            if (3 < level) {
                SiegeManager.getInstance().addSiegeSkills(leader);
            } else if (4 > level) {
                SiegeManager.getInstance().removeSiegeSkills(leader);
            }
            if (4 < level) {
                leader.sendPacket(Static.CLAN_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS);
            }
        }

        // notify all the members about it
        broadcastToOnlineMembers(Static.CLAN_LEVEL_INCREASED);
        broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
        /*
         * Micht : - use PledgeShowInfoUpdate instead of PledgeStatusChanged to
         * update clan level ingame - remove broadcastClanStatus() to avoid
         * members duplication
         */
        //clan.broadcastToOnlineMembers(new PledgeStatusChanged(clan));
        //clan.broadcastClanStatus();
    }

    public boolean isActive() {
        long max = 0;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT lastAccess FROM characters WHERE clanid=?");
            st.setInt(1, _clanId);
            rs = st.executeQuery();
            rs.setFetchSize(50);
            while (rs.next()) {
                long last = rs.getLong("lastAccess");
                if (max < last) {
                    max = last;
                }
            }
        } catch (Exception e) {
            _log.warning("L2Clan: isActive() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
        return (System.currentTimeMillis() - max < Config.CLAN_CH_CLEAN);
    }

    public void addBonusEffects(L2PcInstance player, boolean add) {
        addBonusEffects(player, add, true);
    }

    public void addBonusEffects(L2PcInstance player, boolean add, boolean send) {
        if (player == null || _hasCastle == 0) {
            return;
        }

        giveClanBonusSkill(player, add);
        giveSiegeBonusSkill(player, add);
        giveCastleBonusSkill(player, add);

        if (send) {
            player.sendSkillList();
        }
    }

    private void giveClanBonusSkill(L2PcInstance player, boolean add) {
        if (_clanSkill != null) {
            if (add) {
                player.addSkill(_clanSkill, false);
            } else {
                player.removeSkill(_clanSkill);
            }
        }
    }

    public void giveSiegeBonusSkill(L2PcInstance player, boolean add) {
        if (_disableBonus || Config.CASTLE_SIEGE_SKILLS.isEmpty()) {
            return;
        }

        if (_siegeBonus == null) {
            if (Config.CASTLE_SIEGE_SKILLS.get(_hasCastle) == null) {
                return;
            }
            EventReward bonus = Config.CASTLE_SIEGE_SKILLS.get(_hasCastle);
            _siegeBonus = SkillTable.getInstance().getInfo(bonus.id, bonus.count);
        }
        //player.sendAdmResultMessage("####3");

        if (_siegeBonus == null) {
            return;
        }
        //player.sendAdmResultMessage("####4###" + _siegeBonus.getName());

        if (add) {
            player.addSkill(_siegeBonus, false);
        } else {
            player.removeSkill(_siegeBonus);
        }
    }

    public void disableSiegeBonus(boolean f) {
        _disableBonus = f;
        if (f && _hasCastle > 0) {
            EventReward bonus = Config.CASTLE_SIEGE_SKILLS.get(_hasCastle);
            if (bonus == null) {
                return;
            }
            _siegeBonus = SkillTable.getInstance().getInfo(bonus.id, bonus.count);
            if (_siegeBonus == null) {
                return;
            }

            L2PcInstance member = null;
            for (L2ClanMember temp : _members.values()) {
                if (temp == null) {
                    continue;
                }
                if (temp.isOnline()) {
                    member = temp.getPlayerInstance();
                    if (member != null) {
                        member.removeSkill(_siegeBonus);
                        member.sendSkillList();
                    }
                }
            }
        }
    }

    private void giveCastleBonusSkill(L2PcInstance player, boolean add) {
        if (_hasCastle > 0) {
            CustomServerData.getInstance().giveCastleBonusSkill(player, _hasCastle, add);
        }
    }

    public void removeBonusEffects() {
        for (L2ClanMember temp : _members.values()) {
            if (temp == null) {
                continue;
            }
            if (temp.isOnline()) {
                addBonusEffects(temp.getPlayerInstance(), false);
            }
        }
        _siegeBonus = null;
    }

    public void setBonusSkill() {

        if (_hasCastle > 0) {
            EventReward bonus = Config.CASTLE_SIEGE_SKILLS.get(_hasCastle);
            if (bonus == null) {
                return;
            }
            _siegeBonus = SkillTable.getInstance().getInfo(bonus.id, bonus.count);
            if (_siegeBonus == null) {
                return;
            }

            L2PcInstance member = null;
            for (L2ClanMember temp : _members.values()) {
                if (temp == null) {
                    continue;
                }
                if (temp.isOnline()) {
                    addBonusEffects(temp.getPlayerInstance(), true);

                    member = temp.getPlayerInstance();
                    if (member != null) {
                        member.sendSkillList();
                    }
                }
            }
        }
    }

    public String getCastleName() {
        return CastleManager.getCastleName(_hasCastle);
    }
    private L2Skill _clanSkill = null;

    public void setClanSkill(L2Skill cs) {
        _clanSkill = cs;
    }

    public L2Skill getClanSkill() {
        return _clanSkill;
    }
}
