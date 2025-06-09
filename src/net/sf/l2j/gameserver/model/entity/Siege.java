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

import java.util.logging.Logger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;

import javolution.util.FastList;
import javolution.util.FastTable;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.instancemanager.SiegeGuardManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager.SiegeSpawn;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2SiegeClan.SiegeClanType;
import net.sf.l2j.gameserver.model.actor.instance.L2ArtefactInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2ControlTowerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadDiary;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.SiegeInfo;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import scripts.clanhalls.BanditStronghold;
import net.sf.l2j.util.log.AbstractLogger;

public class Siege {

    private static final Logger _log = AbstractLogger.getLogger(ClanHallManager.class.getName());
    // ==========================================================================================
    // Message to add/check
    //  id=17  msg=[Castle siege has begun.] c3_attr1=[SystemMsg_k.17]
    //  id=18  msg=[Castle siege is over.]   c3_attr1=[SystemMsg_k.18]
    //  id=288 msg=[The castle gate has been broken down.]
    //  id=291 msg=[Clan $s1 is victorious over $s2's castle siege!]
    //  id=292 msg=[$s1 has announced the castle siege time.]
    //  - id=293 msg=[The registration term for $s1 has ended.]
    //  - id=358 msg=[$s1 hour(s) until castle siege conclusion.]
    //  - id=359 msg=[$s1 minute(s) until castle siege conclusion.]
    //  - id=360 msg=[Castle siege $s1 second(s) left!]
    //  id=640 msg=[You have failed to refuse castle defense aid.]
    //  id=641 msg=[You have failed to approve castle defense aid.]
    //  id=644 msg=[You are not yet registered for the castle siege.]
    //  - id=645 msg=[Only clans with Level 4 and higher may register for a castle siege.]
    //  id=646 msg=[You do not have the authority to modify the castle defender list.]
    //  - id=688 msg=[The clan that owns the castle is automatically registered on the defending side.]
    //  id=689 msg=[A clan that owns a castle cannot participate in another siege.]
    //  id=690 msg=[You cannot register on the attacking side because you are part of an alliance with the clan that owns the castle.]
    //  id=718 msg=[The castle gates cannot be opened and closed during a siege.]
    //  - id=295 msg=[$s1's siege was canceled because there were no clans that participated.]
    //  id=659 msg=[This is not the time for siege registration and so registrations cannot be accepted or rejected.]
    //  - id=660 msg=[This is not the time for siege registration and so registration and cancellation cannot be done.]
    //  id=663 msg=[The siege time has been declared for $s. It is not possible to change the time after a siege time has been declared. Do you want to continue?]
    //  id=667 msg=[You are registering on the attacking side of the $s1 siege. Do you want to continue?]
    //  id=668 msg=[You are registering on the defending side of the $s1 siege. Do you want to continue?]
    //  id=669 msg=[You are canceling your application to participate in the $s1 siege battle. Do you want to continue?]
    //  id=707 msg=[You cannot teleport to a village that is in a siege.]
    //  - id=711 msg=[The siege of $s1 has started.]
    //  - id=712 msg=[The siege of $s1 has finished.]
    //  id=844 msg=[The siege to conquer $s1 has begun.]
    //  - id=845 msg=[The deadline to register for the siege of $s1 has passed.]
    //  - id=846 msg=[The siege of $s1 has been canceled due to lack of interest.]
    //  - id=856 msg=[The siege of $s1 has ended in a draw.]
    //  id=285 msg=[Clan $s1 has succeeded in engraving the ruler!]
    //  - id=287 msg=[The opponent clan has begun to engrave the ruler.]

    public static enum TeleportWhoType {

        All, Attacker, DefenderNotOwner, Owner, Spectator
    }

    // ===============================================================
    // Schedule task
    public class ScheduleEndSiegeTask implements Runnable {

        private Castle _castleInst;

        public ScheduleEndSiegeTask(Castle pCastle) {
            _castleInst = pCastle;
        }

        public void run() {
            if (!getIsInProgress()) {
                return;
            }

            try {
                long timeRemaining = _siegeEndDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
                String CastleName = getCastleName(getCastle().getCastleId());

                if (timeRemaining > 3600000) {
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 3600000); // Prepare task for 1 hr left.
                } else if ((timeRemaining <= 3600000) && (timeRemaining > 600000)) {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " минут до окончани€ осады " + CastleName, true);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
                } else if ((timeRemaining <= 600000) && (timeRemaining > 300000)) {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " минут до окончани€ осады " + CastleName, true);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
                } else if ((timeRemaining <= 300000) && (timeRemaining > 10000)) {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " минут до окончани€ осады " + CastleName, true);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down
                } else if ((timeRemaining <= 10000) && (timeRemaining > 0)) {
                    for (int i = 10; i > 0; i--) {
                        announceToPlayerSiegeEnd(i);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    _castleInst.getSiege().endSiege();
                }
            } catch (Throwable t) {
            }
        }
    }

    public class ScheduleStartSiegeTask implements Runnable {

        private Castle _castleInst;

        public ScheduleStartSiegeTask(Castle pCastle) {
            _castleInst = pCastle;
        }

        public void run() {
            if (getIsInProgress()) {
                return;
            }

            try {
                long timeRemaining = getSiegeDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
                String CastleName = getCastleName(getCastle().getCastleId());

                if (timeRemaining > 86400000) {
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 86400000); // Prepare task for 24 before siege start to end registration
                } else if ((timeRemaining <= 86400000) && (timeRemaining > 13600000)) {
                    announceToPlayer("«акончена регистраци€ на осаду " + CastleName, false);
                    _isRegistrationOver = true;
                    clearSiegeWaitingClan();
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 13600000); // Prepare task for 1 hr left before siege start.
                } else if ((timeRemaining <= 13600000) && (timeRemaining > 600000)) {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " минут до начала осады " + CastleName, false);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
                } else if ((timeRemaining <= 600000) && (timeRemaining > 300000)) {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " минут до начала осады " + CastleName, false);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
                } else if ((timeRemaining <= 300000) && (timeRemaining > 10000)) {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " минут до начала осады " + CastleName, false);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down
                } else if ((timeRemaining <= 10000) && (timeRemaining > 0)) {
                    announceToPlayer(Math.round(timeRemaining / 1000) + " секунд до начала осады " + CastleName + "!", false);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining); // Prepare task for second count down
                } else {
                    _castleInst.getSiege().startSiege();
                }
            } catch (Throwable t) {
            }
        }
    }

    public class ScheduleWaitersTeleportTask implements Runnable {

        private Castle _castleInst;

        public ScheduleWaitersTeleportTask(Castle pCastle) {
            _castleInst = pCastle;
        }

        public void run() {
            if (!getIsInProgress()) {
                return;
            }

            try {
                getCastle().getWaitZone().oustDefenders();
                ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleWaitersTeleportTask(_castleInst), getDefenderRespawnDelay());
                //System.out.println("Castle.oust");
            } catch (Throwable t) {
            }
        }
    }
    // =========================================================
    // Data Field
    // Attacker and Defender
    private List<L2SiegeClan> _attackerClans = new FastList<L2SiegeClan>(); // L2SiegeClan
    private List<L2SiegeClan> _defenderClans = new FastList<L2SiegeClan>(); // L2SiegeClan
    private List<L2SiegeClan> _defenderWaitingClans = new FastList<L2SiegeClan>(); // L2SiegeClan
    private int _defenderRespawnDelayPenalty;
    // Castle setting
    private List<L2ArtefactInstance> _artifacts = new FastList<L2ArtefactInstance>();
    private List<L2ControlTowerInstance> _controlTowers = new FastList<L2ControlTowerInstance>();
    private Castle[] _castle;
    private boolean _isInProgress = false;
    private boolean _isNormalSide = true; // true = Atk is Atk, false = Atk is Def
    protected boolean _isRegistrationOver = false;
    protected Calendar _siegeEndDate;
    private SiegeGuardManager _siegeGuardManager;
    protected Calendar _siegeRegistrationEndDate;

    // =========================================================
    // Constructor
    public Siege(Castle[] castle) {
        _castle = castle;
        _siegeGuardManager = new SiegeGuardManager(getCastle());

        startAutoTask();
    }

    // =========================================================
    // Siege phases
    /**
     * When siege ends<BR><BR>
     */
    public void endSiege() {
        if (getIsInProgress()) {
            announceToPlayer("Ѕитва за " + getCastleName(getCastle().getCastleId()) + " окончена!", false);

            removeFlags(); // Removes all flags. Note: Remove flag before teleporting players
            teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the second closest town
            teleportPlayer(Siege.TeleportWhoType.DefenderNotOwner, MapRegionTable.TeleportWhereType.Town); // Teleport to the second closest town
            teleportPlayer(Siege.TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.Town); // Teleport to the second closest town
            _isInProgress = false; // Flag so that siege instance can be started
            updatePlayerSiegeStateFlags(true);
            saveCastleSiege(); // Save castle specific data
            clearSiegeClan(); // Clear siege clan from db
            removeArtifact(); // Remove artifact from this castle
            removeControlTower(); // Remove all control tower from this castle
            if (Config.SIEGE_GUARDS_SPAWN) {
                _siegeGuardManager.unspawnSiegeGuard(); // Remove all spawned siege guard from this castle
            }
            getCastle().spawnDoor(); // Respawn door to castle
            getCastle().getZone().updateZoneStatusForCharactersInside();

            if (getCastle().getOwnerId() > 0) {
                if (Config.SIEGE_GUARDS_SPAWN) {
                    _siegeGuardManager.removeMercs();
                }

                if (Config.CASTLE_SIEGE_SKILLS_DELETE) {
                    getCastle().disableOwnerBonus(false);
                }

                getCastle().giveOwnerBonus();
                getCastle().giveClanBonus();
            }
        }
    }

    private void removeDefender(L2SiegeClan sc) {
        if (sc != null) {
            getDefenderClans().remove(sc);
        }
    }

    private void removeAttacker(L2SiegeClan sc) {
        if (sc != null) {
            getAttackerClans().remove(sc);
        }
    }

    private void addDefender(L2SiegeClan sc, SiegeClanType type) {
        if (sc == null) {
            return;
        }
        sc.setType(type);
        getDefenderClans().add(sc);
    }

    private void addAttacker(L2SiegeClan sc) {
        if (sc == null) {
            return;
        }
        sc.setType(SiegeClanType.ATTACKER);
        getAttackerClans().add(sc);
    }

    /**
     * When control of castle changed during siege<BR><BR>
     */
    public void midVictory() {
        if (getIsInProgress()) // Siege still in progress
        {
            if (getCastle().getOwnerId() > 0) {
                if (Config.SIEGE_GUARDS_SPAWN) {
                    _siegeGuardManager.removeMercs(); // Remove all merc entry from db
                }
            }
            if (getDefenderClans().isEmpty() && // If defender doesn't exist (Pc vs Npc)
                    getAttackerClans().size() == 1 // Only 1 attacker
                    ) {
                L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
                removeAttacker(sc_newowner);
                addDefender(sc_newowner, SiegeClanType.OWNER);
                endSiege();
                return;
            }
            if (getCastle().getOwnerId() > 0) {

                int allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
                if (getDefenderClans().isEmpty()) // If defender doesn't exist (Pc vs Npc)
                // and only an alliance attacks
                {
                    // The player's clan is in an alliance
                    if (allyId != 0) {
                        boolean allinsamealliance = true;
                        for (L2SiegeClan sc : getAttackerClans()) {
                            if (sc != null) {
                                if (ClanTable.getInstance().getClan(sc.getClanId()).getAllyId() != allyId) {
                                    allinsamealliance = false;
                                }
                            }
                        }
                        if (allinsamealliance) {
                            L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
                            removeAttacker(sc_newowner);
                            addDefender(sc_newowner, SiegeClanType.OWNER);
                            endSiege();
                            return;
                        }
                    }
                }

                for (L2SiegeClan sc : getDefenderClans()) {
                    if (sc != null) {
                        removeDefender(sc);
                        addAttacker(sc);
                    }
                }

                L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
                removeAttacker(sc_newowner);
                addDefender(sc_newowner, SiegeClanType.OWNER);

                // The player's clan is in an alliance
                if (allyId != 0) {
                    FastTable<L2Clan> cn = new FastTable<L2Clan>();
                    cn.addAll(ClanTable.getInstance().getClans());
                    for (L2Clan clan : cn) {
                        if (clan.getAllyId() == allyId) {
                            L2SiegeClan sc = getAttackerClan(clan.getClanId());
                            if (sc != null) {
                                removeAttacker(sc);
                                addDefender(sc, SiegeClanType.DEFENDER);
                            }
                        }
                    }
                }
                teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.SiegeFlag); // Teleport to the second closest town
                teleportPlayer(Siege.TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.Town);     // Teleport to the second closest town

                removeDefenderFlags(); 		 // Removes defenders' flags
                getCastle().removeUpgrade(); // Remove all castle upgrade
                getCastle().spawnDoor(true); // Respawn door to castle but make them weaker (50% hp)
                updatePlayerSiegeStateFlags(false);
            }
        }
    }

    /**
     * When siege starts<BR><BR>
     */
    public void startSiege() {
        if (!getIsInProgress()) {
            int id = getCastle().getCastleId();
            String CastleName = getCastleName(id);

            if (getAttackerClans().size() <= 0 && id != 21 && id != 35) {
                SystemMessage sm;
                if (getCastle().getOwnerId() <= 0) {
                    sm = SystemMessage.id(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
                } else {
                    sm = SystemMessage.id(SystemMessageId.S1_SIEGE_WAS_CANCELED_BECAUSE_NO_CLANS_PARTICIPATED);
                }
                sm.addString(CastleName);
                Announcements.getInstance().announceToAll(sm);
                sm = null;
                return;
            }

            if (id == 35) {
                if (BanditStronghold.getCH().getAttackers().size() <= 1) {
                    BanditStronghold.getCH().cancel();
                    Announcements.getInstance().announceToAll(SystemMessage.id(SystemMessageId.S1_SIEGE_WAS_CANCELED_BECAUSE_NO_CLANS_PARTICIPATED).addString(CastleName));
                    return;
                }
                BanditStronghold.getCH().startSiege();
            }

            _isNormalSide = true; // Atk is now atk
            _isInProgress = true; // Flag so that same siege instance cannot be started again

            loadSiegeClan(); // Load siege clan from db

            if (id != 21 && id != 35) {
                updatePlayerSiegeStateFlags(false);
            }

            if (Config.CASTLE_SIEGE_SKILLS_DELETE && getCastle().getOwnerId() > 0) {
                getCastle().disableOwnerBonus(true);
            }

            teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the closest town
            //teleportPlayer(Siege.TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.Town);      // Teleport to the second closest town
            spawnArtifact(id); // Spawn artifact
            spawnControlTower(id); // Spawn control tower
            getCastle().spawnDoor(); // Spawn door
            if (Config.SIEGE_GUARDS_SPAWN) {
                spawnSiegeGuard(ClanTable.getInstance().getClan(getCastle().getOwnerId())); // Spawn siege guard
            }
            MercTicketManager.getInstance().deleteTickets(id); // remove the tickets from the ground
            _defenderRespawnDelayPenalty = 0; // Reset respawn delay

            getCastle().getZone().updateZoneStatusForCharactersInside();

            // Schedule a task to prepare auto siege end
            _siegeEndDate = Calendar.getInstance();

            if (id == 34 || id == 64) {
                _siegeEndDate.add(Calendar.MINUTE, 60);
            } else {
                _siegeEndDate.add(Calendar.MINUTE, SiegeManager.getInstance().getSiegeLength());
            }

            ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(getCastle()), 1000); // Prepare auto end task
            //
            if (!getCastle().isClanhall()) {
                ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleWaitersTeleportTask(getCastle()), getDefenderRespawnDelay());
            } else if (!ClanHallManager.getInstance().isFree(id)) {
                ClanHallManager.getInstance().setFree(id);
            }

            announceToPlayer("Ќачалась битва за " + CastleName + "", false);
            _log.info("Siege of " + CastleName + " started.");
        }
    }

    // =========================================================
    // Method - Public
    /**
     * Announce to player.<BR><BR>
     * @param message The String of the message to send to player
     * @param inAreaOnly The boolean flag to show message to players in area only.
     */
    public void announceToPlayer(final String message, boolean inAreaOnly) {
        //getCastle().getZone().announceToPlayers(message);

        if (inAreaOnly) {
            getCastle().getZone().announceToPlayers(message);
            return;
        }

        // Get all players
        for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
            player.sendMessage(message);
        }
    }

    public void announceToPlayerSiegeEnd(int countdown) {
        /*SystemMessage sm = SystemMessage.id(SystemMessageId.SIEGE_END_FOR_S1_SECONDS);
        sm.addNumber(countdown);*/

        getCastle().getZone().announceSmToPlayers(SystemMessage.id(SystemMessageId.SIEGE_END_FOR_S1_SECONDS).addNumber(countdown));
        //sm = null;
    }

    public void updatePlayerSiegeStateFlags(boolean clear) {
        L2Clan clan;
        for (L2SiegeClan siegeclan : getAttackerClans()) {
            if (siegeclan == null) {
                continue;
            }

            clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
            if (clan == null) {
                continue;
            }

            for (L2PcInstance member : clan.getOnlineMembers("")) {
                if (member == null) {
                    continue;
                }

                if (clear) {
                    member.setSiegeState((byte) 0);
                } else {
                    member.setSiegeState((byte) 1);
                }
                member.sendPacket(new UserInfo(member));

                FastList<L2PcInstance> players = member.getKnownList().getListKnownPlayers();
                L2PcInstance pc = null;
                for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                    pc = n.getValue();
                    if (pc == null) {
                        continue;
                    }

                    pc.sendPacket(new RelationChanged(member, member.getRelation(pc), member.isAutoAttackable(pc)));
                }
                players.clear();
                players = null;
                pc = null;
            }
        }
        for (L2SiegeClan siegeclan : getDefenderClans()) {
            if (siegeclan == null) {
                continue;
            }

            clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
            if (clan == null) {
                continue;
            }

            for (L2PcInstance member : clan.getOnlineMembers("")) {
                if (member == null) {
                    continue;
                }

                if (clear) {
                    member.setSiegeState((byte) 0);
                } else {
                    member.setSiegeState((byte) 2);
                }
                member.sendPacket(new UserInfo(member));

                FastList<L2PcInstance> players = member.getKnownList().getListKnownPlayers();
                L2PcInstance pc = null;
                for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                    pc = n.getValue();
                    if (pc == null) {
                        continue;
                    }

                    pc.sendPacket(new RelationChanged(member, member.getRelation(pc), member.isAutoAttackable(pc)));
                }
                players.clear();
                players = null;
                pc = null;
            }
        }
    }

    /**
     * Approve clan as defender for siege<BR><BR>
     * @param clanId The int of player's clan id
     */
    public void approveSiegeDefenderClan(int clanId) {
        if (clanId <= 0) {
            return;
        }
        saveSiegeClan(ClanTable.getInstance().getClan(clanId), 0, true);
        loadSiegeClan();
    }

    /** Return true if object is inside the zone */
    public boolean checkIfInZone(L2Object object) {
        return checkIfInZone(object.getX(), object.getY(), object.getZ());
    }

    /** Return true if object is inside the zone */
    public boolean checkIfInZone(int x, int y, int z) {
        return (getIsInProgress() && (getCastle().checkIfInZone(x, y, z))); // Castle zone during siege
    }

    /**
     * Return true if clan is attacker<BR><BR>
     * @param clan The L2Clan of the player
     */
    public boolean checkIsAttacker(L2Clan clan) {
        return (getAttackerClan(clan) != null);
    }

    /**
     * Return true if clan is defender<BR><BR>
     * @param clan The L2Clan of the player
     */
    public boolean checkIsDefender(L2Clan clan) {
        return (getDefenderClan(clan) != null);
    }

    /**
     * Return true if clan is defender waiting approval<BR><BR>
     * @param clan The L2Clan of the player
     */
    public boolean checkIsDefenderWaiting(L2Clan clan) {
        return (getDefenderWaitingClan(clan) != null);
    }

    /** Clear all registered siege clans from database for castle */
    public void clearSiegeClan() {
        Connect con = null;
        PreparedStatement statement = null;
        PreparedStatement statement2 = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=?");
            statement.setInt(1, getCastle().getCastleId());
            statement.execute();
            Close.S(statement);

            if (getCastle().getOwnerId() > 0) {
                statement2 = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?");
                statement2.setInt(1, getCastle().getOwnerId());
                statement2.execute();
                Close.S(statement2);
            }
            getAttackerClans().clear();
            getDefenderClans().clear();
            getDefenderWaitingClans().clear();
        } catch (Exception e) {
            _log.warning("Exception: clearSiegeClan(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.S(statement2);
            Close.CS(con, statement);
        }
    }

    /** Clear all siege clans waiting for approval from database for castle */
    public void clearSiegeWaitingClan() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and type = 2");
            statement.setInt(1, getCastle().getCastleId());
            statement.execute();

            getDefenderWaitingClans().clear();
        } catch (Exception e) {
            _log.warning("Exception: clearSiegeWaitingClan(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    /** Return list of L2PcInstance registered as attacker in the zone. */
    public List<L2PcInstance> getAttackersInZone() {
        List<L2PcInstance> players = new FastList<L2PcInstance>();
        L2Clan clan;
        for (L2SiegeClan siegeclan : getAttackerClans()) {
            if (siegeclan == null) {
                continue;
            }
            clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
            if (clan == null) {
                continue;
            }
            for (L2PcInstance player : clan.getOnlineMembers("")) {
                if (checkIfInZone(player.getX(), player.getY(), player.getZ())) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    /** Return list of L2PcInstance registered as defender but not owner in the zone. */
    public List<L2PcInstance> getDefendersButNotOwnersInZone() {
        List<L2PcInstance> players = new FastList<L2PcInstance>();
        L2Clan clan;
        for (L2SiegeClan siegeclan : getDefenderClans()) {
            if (siegeclan == null) {
                continue;
            }
            clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
            if (clan == null) {
                continue;
            }
            if (clan.getClanId() == getCastle().getOwnerId()) {
                continue;
            }
            for (L2PcInstance player : clan.getOnlineMembers("")) {
                if (checkIfInZone(player.getX(), player.getY(), player.getZ())) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    /** Return list of L2PcInstance in the zone. */
    public List<L2PcInstance> getPlayersInZone() {
        return getCastle().getZone().getAllPlayers();
    }

    /** Return list of L2PcInstance owning the castle in the zone. */
    public List<L2PcInstance> getOwnersInZone() {
        List<L2PcInstance> players = new FastList<L2PcInstance>();
        L2Clan clan;
        for (L2SiegeClan siegeclan : getDefenderClans()) {
            if (siegeclan == null) {
                continue;
            }
            clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
            if (clan == null) {
                continue;
            }
            if (clan.getClanId() != getCastle().getOwnerId()) {
                continue;
            }
            for (L2PcInstance player : clan.getOnlineMembers("")) {
                if (checkIfInZone(player.getX(), player.getY(), player.getZ())) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    /** Return list of L2PcInstance not registered as attacker or defender in the zone. */
    public List<L2PcInstance> getSpectatorsInZone() {
        List<L2PcInstance> players = new FastList<L2PcInstance>();

        for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
            if (player == null) {
                continue;
            }
            // quick check from player states, which don't include siege number however
            if (!player.isInsideZone(L2Character.ZONE_SIEGE) || player.getSiegeState() != 0) {
                continue;
            }
            if (checkIfInZone(player.getX(), player.getY(), player.getZ())) {
                players.add(player);
            }
        }

        return players;
    }

    /** Control Tower was skilled */
    public void killedCT(L2NpcInstance ct) {
        _defenderRespawnDelayPenalty += SiegeManager.getInstance().getControlTowerLosePenalty(); // Add respawn penalty to defenders for each control tower lose
    }

    /** Display list of registered clans */
    public void listRegisterClan(L2PcInstance player) {
        player.sendPacket(new SiegeInfo(getCastle()));
    }

    /**
     * Register clan as attacker<BR><BR>
     * @param player The L2PcInstance of the player trying to register
     */
    public void registerAttacker(L2PcInstance player) {
        registerAttacker(player, false);
    }

    public void registerAttacker(L2PcInstance player, boolean force) {

        if (player.getClan() == null) {
            return;
        }
        int allyId = 0;
        if (getCastle().getOwnerId() != 0) {
            try {
                allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
            } catch (Exception e) {
                allyId = 0;
            }
        }
        if (allyId != 0) {
            if (player.getClan().getAllyId() == allyId && !force) {
                player.sendMessage("¬ы не можете зарегистрироватьс€ на атаку клана вашего аль€нса");
                return;
            }
        }
        if (force || checkIfCanRegister(player)) {
            saveSiegeClan(player.getClan(), 1, false); // Save to database
        }
    }

    /**
     * Register clan as defender<BR><BR>
     * @param player The L2PcInstance of the player trying to register
     */
    public void registerDefender(L2PcInstance player) {
        registerDefender(player, false);
    }

    public void registerDefender(L2PcInstance player, boolean force) {
        if (getCastle().getOwnerId() <= 0) {
            player.sendMessage("Ќельз€ зарегистрироватьс€ на защиту");
        } else if (getCastle().getCastleId() == 34 || getCastle().getCastleId() == 64 || getCastle().getCastleId() == 21) {
            player.sendMessage("Ќельз€ зарегистрироватьс€ на защиту");
        } else if (force || checkIfCanRegister(player)) {
            saveSiegeClan(player.getClan(), 2, false); // Save to database
        }
    }

    /**
     * Remove clan from siege<BR><BR>
     * @param clanId The int of player's clan id
     */
    public void removeSiegeClan(int clanId) {
        if (clanId <= 0) {
            return;
        }

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and clan_id=?");
            statement.setInt(1, getCastle().getCastleId());
            statement.setInt(2, clanId);
            statement.execute();

            loadSiegeClan();
        } catch (Exception e) {
        } finally {
            Close.CS(con, statement);
        }
    }

    /**
     * Remove clan from siege<BR><BR>
     * @param player The L2PcInstance of player/clan being removed
     */
    public void removeSiegeClan(L2Clan clan) {
        if (clan == null || clan.getHasCastle() == getCastle().getCastleId()
                || !SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getCastleId())) {
            return;
        }
        removeSiegeClan(clan.getClanId());
    }

    /**
     * Remove clan from siege<BR><BR>
     * @param player The L2PcInstance of player/clan being removed
     */
    public void removeSiegeClan(L2PcInstance player) {
        removeSiegeClan(player.getClan());
    }

    /**
     * Start the auto tasks<BR><BR>
     */
    public void startAutoTask() {
        correctSiegeDateTime();

        _log.info("Siege of " + getCastleName(getCastle().getCastleId()) + ": " + getCastle().getSiegeDate().getTime());

        loadSiegeClan();

        // Schedule registration end
        _siegeRegistrationEndDate = Calendar.getInstance();
        _siegeRegistrationEndDate.setTimeInMillis(getCastle().getSiegeDate().getTimeInMillis());
        _siegeRegistrationEndDate.add(Calendar.DAY_OF_MONTH, -1);

        // Schedule siege auto start
        ThreadPoolManager.getInstance().scheduleGeneral(new Siege.ScheduleStartSiegeTask(getCastle()), 1000);
    }

    /**
     * Teleport players
     */
    public void teleportPlayer(TeleportWhoType teleportWho, MapRegionTable.TeleportWhereType teleportWhere) {
        List<L2PcInstance> players;
        switch (teleportWho) {
            case Owner:
                players = getOwnersInZone();
                break;
            case Attacker:
                players = getAttackersInZone();
                break;
            case DefenderNotOwner:
                players = getDefendersButNotOwnersInZone();
                break;
            case Spectator:
                players = getSpectatorsInZone();
                break;
            default:
                players = getPlayersInZone();
        }
        ;

        for (L2PcInstance player : players) {
            if (player == null) {
                continue;
            }

            if (player.isGM() || player.isInJail()) {
                continue;
            }
            player.teleToLocation(teleportWhere);

            if (player.isHero() && player.getClan() != null && player.getClan().getClanId() == getCastle().getOwnerId()) {
                OlympiadDiary.addRecord(player, "ѕобеда в битве за замок" + getCastleName(getCastle().getCastleId()) + ".");
            }
        }
    }

    // =========================================================
    // Method - Private
    /**
     * Add clan as attacker<BR><BR>
     * @param clanId The int of clan's id
     */
    private void addAttacker(int clanId) {
        getAttackerClans().add(new L2SiegeClan(clanId, SiegeClanType.ATTACKER)); // Add registered attacker to attacker list
    }

    /**
     * Add clan as defender<BR><BR>
     * @param clanId The int of clan's id
     */
    private void addDefender(int clanId) {
        getDefenderClans().add(new L2SiegeClan(clanId, SiegeClanType.DEFENDER)); // Add registered defender to defender list
    }

    /**
     * <p>Add clan as defender with the specified type</p>
     * @param clanId The int of clan's id
     * @param type the type of the clan
     */
    private void addDefender(int clanId, SiegeClanType type) {
        getDefenderClans().add(new L2SiegeClan(clanId, type));
    }

    /**
     * Add clan as defender waiting approval<BR><BR>
     * @param clanId The int of clan's id
     */
    private void addDefenderWaiting(int clanId) {
        getDefenderWaitingClans().add(new L2SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING)); // Add registered defender to defender list
    }

    /**
     * Return true if the player can register.<BR><BR>
     * @param player The L2PcInstance of the player trying to register
     */
    private boolean checkIfCanRegister(L2PcInstance player) {
        if (getIsRegistrationOver()) {
            player.sendMessage("¬рем€ регистрации на битву за " + getCastle().getName() + " истекло");
        } else if (getIsInProgress()) {
            player.sendPacket(Static.SIEGE_NO_REG);
        } else if (player.getClan() == null
                || player.getClan().getLevel() < SiegeManager.getInstance().getSiegeClanMinLevel()) {
            player.sendMessage("“олько кланы выше " + SiegeManager.getInstance().getSiegeClanMinLevel() + " уровн€ могут регистрироватьс€");
        } else if (player.getClan().getHasCastle() > 0) {
            player.sendPacket(Static.SIEGE_HAVE_CASTLE);
        } else if (player.getClan().getClanId() == getCastle().getOwnerId()) {
            player.sendPacket(Static.CLAN_THAT_OWNS_CASTLE_IS_AUTOMATICALLY_REGISTERED_DEFENDING);
        } else if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), getCastle().getCastleId())) {
            player.sendMessage("¬ы уже зарегистрированы");
        } //else if (player.getClanId() == ClanHallManager.getInstance().getClanHallById(34).getOwnerId() 
        //	|| player.getClanId() == ClanHallManager.getInstance().getClanHallById(64).getOwnerId())
        //		 player.sendMessage("¬ладельцам элитных кланхоллов нельз€ регистрироватьс€ на другие осады");
        //else if (ClanHallManager.getInstance().getClanHallByOwner(player.getClan()) != null) 
        //player.sendMessage(" лан, который имеет  лан ’олл может не участвовать в осаде.");
        else {
            return true;
        }

        return false;
    }

    /**
     * Return the correct siege date as Calendar.<BR><BR>
     * @param siegeDate The Calendar siege date and time
     */
    private void correctSiegeDateTime() {
        boolean corrected = false;

        if (getCastle().getSiegeDate().getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
            // Since siege has past reschedule it to the next one (14 days)
            // This is usually caused by server being down
            corrected = true;
            setNextSiegeDate();
        }

        if (getCastle().getSiegeDate().get(Calendar.DAY_OF_WEEK) != getCastle().getSiegeDayOfWeek()) {
            corrected = true;
            getCastle().getSiegeDate().set(Calendar.DAY_OF_WEEK, getCastle().getSiegeDayOfWeek());
        }

        if (getCastle().getSiegeDate().get(Calendar.HOUR_OF_DAY) != getCastle().getSiegeHourOfDay()) {
            corrected = true;
            getCastle().getSiegeDate().set(Calendar.HOUR_OF_DAY, getCastle().getSiegeHourOfDay());
        }

        getCastle().getSiegeDate().set(Calendar.MINUTE, 0);

        if (corrected) {
            saveSiegeDate();
        }
    }

    /** Load siege clans. */
    private void loadSiegeClan() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            getAttackerClans().clear();
            getDefenderClans().clear();
            getDefenderWaitingClans().clear();

            // Add castle owner as defender (add owner first so that they are on the top of the defender list)
            if (getCastle().getOwnerId() > 0 && (getCastle().getCastleId() != 34 || getCastle().getCastleId() != 64)) {
                addDefender(getCastle().getOwnerId(), SiegeClanType.OWNER);
            }
            //else if (getCastle().getCastleId() == 34 && ClanHallManager.getInstance().getClanHallById(34).getOwnerId() > 0)
            //    addAttacker(ClanHallManager.getInstance().getClanHallById(34).getOwnerId());
            //else if (getCastle().getCastleId() == 64 && ClanHallManager.getInstance().getClanHallById(64).getOwnerId() > 0)
            //    addAttacker(ClanHallManager.getInstance().getClanHallById(64).getOwnerId());

            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            statement = con.prepareStatement("SELECT clan_id,type FROM siege_clans where castle_id=?");
            statement.setInt(1, getCastle().getCastleId());
            rs = statement.executeQuery();

            int typeId;
            while (rs.next()) {
                switch (rs.getInt("type")) {
                    case 0:
                        addDefender(rs.getInt("clan_id"));
                        break;
                    case 1:
                        addAttacker(rs.getInt("clan_id"));
                        break;
                    case 2:
                        addDefenderWaiting(rs.getInt("clan_id"));
                        break;
                }
            }
        } catch (Exception e) {
            _log.warning("Exception: loadSiegeClan(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CSR(con, statement, rs);
        }
    }

    /** Remove artifacts spawned. */
    private void removeArtifact() {
        if (_artifacts != null) {
            // Remove all instance of artifact for this castle
            for (L2ArtefactInstance art : _artifacts) {
                if (art != null) {
                    art.decayMe();
                }
            }
            _artifacts = null;
        }
    }

    /** Remove all control tower spawned. */
    private void removeControlTower() {
        if (_controlTowers != null) {
            // Remove all instance of control tower for this castle
            for (L2ControlTowerInstance ct : _controlTowers) {
                if (ct != null) {
                    ct.decayMe();
                }
            }

            _controlTowers = null;
        }
    }

    /** Remove all flags. */
    private void removeFlags() {
        for (L2SiegeClan sc : getAttackerClans()) {
            if (sc != null) {
                sc.removeFlag();
            }
        }
        for (L2SiegeClan sc : getDefenderClans()) {
            if (sc != null) {
                sc.removeFlag();
            }
        }
    }

    /** Remove flags from defenders. */
    private void removeDefenderFlags() {
        for (L2SiegeClan sc : getDefenderClans()) {
            if (sc != null) {
                sc.removeFlag();
            }
        }
    }

    /** Save castle siege related to database. */
    private void saveCastleSiege() {
        setNextSiegeDate(); // Set the next set date for 2 weeks from now
        saveSiegeDate(); // Save the new date
        startAutoTask(); // Prepare auto start siege and end registration
    }

    /** Save siege date to database. */
    private void saveSiegeDate() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("Update castle set siegeDate = ? where id = ?");
            statement.setLong(1, getSiegeDate().getTimeInMillis());
            statement.setInt(2, getCastle().getCastleId());
            statement.execute();
        } catch (Exception e) {
            _log.warning("Exception: saveSiegeDate(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    /**
     * Save registration to database.<BR><BR>
     * @param clan The L2Clan of player
     * @param typeId -1 = owner 0 = defender, 1 = attacker, 2 = defender waiting
     */
    private void saveSiegeClan(L2Clan clan, int typeId, boolean isUpdateRegistration) {
        if (clan.getHasCastle() > 0) {
            return;
        }

        if (getAttackerClan(clan) != null || getDefenderClan(clan) != null) {
            return;
        }

        Connect con = null;
        PreparedStatement statement = null;
        try {
            if (typeId == 0 || typeId == 2 || typeId == -1) {
                if (getDefenderClans().size() + getDefenderWaitingClans().size() >= SiegeManager.getInstance().getDefenderMaxClans()) {
                    return;
                }
            } else {
                if (getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans()) {
                    return;
                }
            }

            con = L2DatabaseFactory.get();
            if (!isUpdateRegistration) {
                statement = con.prepareStatement("REPLACE INTO siege_clans (clan_id,castle_id,type,castle_owner) values (?,?,?,0)");
                statement.setInt(1, clan.getClanId());
                statement.setInt(2, getCastle().getCastleId());
                statement.setInt(3, typeId);
                statement.execute();
                Close.S(statement);
            } else {
                statement = con.prepareStatement("Update siege_clans set type = ? where castle_id = ? and clan_id = ?");
                statement.setInt(1, typeId);
                statement.setInt(2, getCastle().getCastleId());
                statement.setInt(3, clan.getClanId());
                statement.execute();
                Close.S(statement);
            }

            String CastleName = getCastleName(getCastle().getCastleId());

            if (typeId == 0 || typeId == -1) {
                addDefender(clan.getClanId());
                announceToPlayer(clan.getName() + " зарегистрировалс€ на защиту " + CastleName, false);
            } else if (typeId == 1) {
                addAttacker(clan.getClanId());
                announceToPlayer(clan.getName() + " зарегистрировалс€ на атаку " + CastleName, false);
            } else if (typeId == 2) {
                addDefenderWaiting(clan.getClanId());
                announceToPlayer(clan.getName() + " сделал запрос на защиту " + CastleName, false);
            }
        } catch (Exception e) {
            _log.warning("Exception: saveSiegeClan(L2Clan clan, int typeId, boolean isUpdateRegistration): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    /** Set the date for the next siege. */
    private void setNextSiegeDate() {
        while (getCastle().getSiegeDate().getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
            // Set next siege date if siege has passed
            if (getCastle().isClanhall()) {
                getCastle().getSiegeDate().add(Calendar.DAY_OF_MONTH, 7);
            } else {
                getCastle().getSiegeDate().add(Calendar.DAY_OF_MONTH, Config.ALT_SIEGE_INTERVAL); // Schedule to happen in 14 days
            }
        }
        _isRegistrationOver = false; // Allow registration for next siege
    }

    /** Spawn artifact. */
    private void spawnArtifact(int Id) {
        //Set artefact array size if one does not exist
        if (_artifacts == null) {
            _artifacts = new FastList<L2ArtefactInstance>();
        }

        for (SiegeSpawn _sp : SiegeManager.getInstance().getArtefactSpawnList(Id)) {
            if (_sp == null) {
                continue;
            }

            L2ArtefactInstance art = new L2ArtefactInstance(IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(_sp.getNpcId()));
            art.setCurrentHpMp(art.getMaxHp(), art.getMaxMp());
            art.setHeading(_sp.getLocation().getHeading());
            art.spawnMe(_sp.getLocation().getX(), _sp.getLocation().getY(), _sp.getLocation().getZ() + 50);
            _artifacts.add(art);
        }
    }

    /** Spawn control tower. */
    private void spawnControlTower(int Id) {
        //Set control tower array size if one does not exist
        if (_controlTowers == null) {
            _controlTowers = new FastList<L2ControlTowerInstance>();
        }

        for (SiegeSpawn _sp : SiegeManager.getInstance().getControlTowerSpawnList(Id)) {
            if (_sp == null) {
                continue;
            }

            L2NpcTemplate template = NpcTable.getInstance().getTemplate(_sp.getNpcId());
            template.getStatsSet().set("baseHpMax", _sp.getHp());
            // TODO: Check/confirm if control towers have any special weapon resistances/vulnerabilities
            // template.addVulnerability(Stats.BOW_WPN_VULN,0);
            // template.addVulnerability(Stats.BLUNT_WPN_VULN,0);
            // template.addVulnerability(Stats.DAGGER_WPN_VULN,0);

            L2ControlTowerInstance ct = new L2ControlTowerInstance(IdFactory.getInstance().getNextId(), template);
            ct.setCurrentHpMp(ct.getMaxHp(), ct.getMaxMp());
            ct.spawnMe(_sp.getLocation().getX(), _sp.getLocation().getY(), _sp.getLocation().getZ() + 20);

            _controlTowers.add(ct);
        }
    }

    /**
     * Spawn siege guard.<BR><BR>
     */
    private void spawnSiegeGuard(L2Clan owner) {
        getSiegeGuardManager().spawnSiegeGuard(owner);

        // Register guard to the closest Control Tower
        // When CT dies, so do all the guards that it controls
        if (getSiegeGuardManager().getSiegeGuardSpawn().size() > 0 && _controlTowers.size() > 0) {
            L2ControlTowerInstance closestCt;
            double distance, x, y, z;
            double distanceClosest = 0;
            for (L2Spawn spawn : getSiegeGuardManager().getSiegeGuardSpawn()) {
                if (spawn == null) {
                    continue;
                }
                closestCt = null;
                distanceClosest = 0;
                for (L2ControlTowerInstance ct : _controlTowers) {
                    if (ct == null) {
                        continue;
                    }
                    x = (spawn.getLocx() - ct.getX());
                    y = (spawn.getLocy() - ct.getY());
                    z = (spawn.getLocz() - ct.getZ());

                    distance = (x * x) + (y * y) + (z * z);

                    if (closestCt == null || distance < distanceClosest) {
                        closestCt = ct;
                        distanceClosest = distance;
                    }
                }

                if (closestCt != null) {
                    closestCt.registerGuard(spawn);
                }
            }
        }
    }

    public final L2SiegeClan getAttackerClan(L2Clan clan) {
        if (clan == null) {
            return null;
        }
        return getAttackerClan(clan.getClanId());
    }

    public final L2SiegeClan getAttackerClan(int clanId) {
        for (L2SiegeClan sc : getAttackerClans()) {
            if (sc != null && sc.getClanId() == clanId) {
                return sc;
            }
        }
        return null;
    }

    public final List<L2SiegeClan> getAttackerClans() {
        if (_isNormalSide) {
            return _attackerClans;
        }
        return _defenderClans;
    }

    public final int getAttackerRespawnDelay() {
        return (SiegeManager.getInstance().getAttackerRespawnDelay());
    }

    public final Castle getCastle() {
        if (_castle == null || _castle.length <= 0) {
            return null;
        }
        return _castle[0];
    }

    public final L2SiegeClan getDefenderClan(L2Clan clan) {
        if (clan == null) {
            return null;
        }
        return getDefenderClan(clan.getClanId());
    }

    public final L2SiegeClan getDefenderClan(int clanId) {
        for (L2SiegeClan sc : getDefenderClans()) {
            if (sc != null && sc.getClanId() == clanId) {
                return sc;
            }
        }
        return null;
    }

    public final List<L2SiegeClan> getDefenderClans() {
        if (_isNormalSide) {
            return _defenderClans;
        }
        return _attackerClans;
    }

    public final L2SiegeClan getDefenderWaitingClan(L2Clan clan) {
        if (clan == null) {
            return null;
        }
        return getDefenderWaitingClan(clan.getClanId());
    }

    public final L2SiegeClan getDefenderWaitingClan(int clanId) {
        for (L2SiegeClan sc : getDefenderWaitingClans()) {
            if (sc != null && sc.getClanId() == clanId) {
                return sc;
            }
        }
        return null;
    }

    public final List<L2SiegeClan> getDefenderWaitingClans() {
        return _defenderWaitingClans;
    }

    public final int getDefenderRespawnDelay() {
        return (SiegeManager.getInstance().getDefenderRespawnDelay() + _defenderRespawnDelayPenalty);
    }

    public final boolean getIsInProgress() {
        return _isInProgress;
    }

    public final boolean getIsRegistrationOver() {
        return _isRegistrationOver;
    }

    public final Calendar getSiegeDate() {
        return getCastle().getSiegeDate();
    }

    public void addFlag(L2Clan clan, L2NpcInstance flag) {
        if (clan != null) {
            L2SiegeClan sc = getAttackerClan(clan);
            if (sc != null) {
                sc.addFlag(flag);
            }
        }
    }

    public L2NpcInstance getFlag(L2Clan clan) {
        L2NpcInstance flag = null;
        if (clan != null) {
            L2SiegeClan sc = getAttackerClan(clan);
            if (sc != null) {
                flag = sc.getFlag();
            }
        }
        return flag;
    }

    public final SiegeGuardManager getSiegeGuardManager() {
        if (_siegeGuardManager == null) {
            _siegeGuardManager = new SiegeGuardManager(getCastle());
        }
        return _siegeGuardManager;
    }

    private String getCastleName(int castleId) {
        String castleName = "";
        switch (castleId) {
            case 1:
                castleName = "Gludio";
                break;
            case 2:
                castleName = "Dion";
                break;
            case 3:
                castleName = "Giran";
                break;
            case 4:
                castleName = "Oren";
                break;
            case 5:
                castleName = "Aden";
                break;
            case 6:
                castleName = "Innadril";
                break;
            case 7:
                castleName = "Goddard";
                break;
            case 8:
                castleName = "Rune";
                break;
            case 9:
                castleName = "Schuttgart";
                break;
            case 21:
                castleName = "Fortress of Resistance";
                break;
            case 34:
                castleName = "Devastated Castle";
                break;
            case 35:
                castleName = "Bandit Stronghold";
                break;
            case 64:
                castleName = "Fortress of the Dead";
                break;
        }
        return castleName;
    }
}