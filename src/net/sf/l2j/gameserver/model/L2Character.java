package net.sf.l2j.gameserver.model;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import net.sf.l2j.gameserver.ai.*;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.SkillDat;
import net.sf.l2j.gameserver.model.actor.knownlist.CharKnownList;
import net.sf.l2j.gameserver.model.actor.knownlist.ObjectKnownList.KnownListAsynchronousUpdateTask;
import net.sf.l2j.gameserver.model.actor.stat.CharStat;
import net.sf.l2j.gameserver.model.actor.status.CharStatus;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc;
import net.sf.l2j.gameserver.pathfinding.PathFinding;
import net.sf.l2j.gameserver.skills.Calculator;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.taskmanager.EffectTaskManager;
import net.sf.l2j.gameserver.templates.*;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.PeaceZone;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import scripts.skills.ISkillHandler;
import scripts.skills.SkillHandler;

/**
 * Mother class of all character objects of the world (PC, NPC...)<BR><BR>
 *
 * L2Character :<BR><BR> <li>L2CastleGuardInstance</li> <li>L2DoorInstance</li>
 * <li>L2NpcInstance</li> <li>L2PlayableInstance </li><BR><BR>
 *
 *
 * <B><U> Concept of L2CharTemplate</U> :</B><BR><BR> Each L2Character owns
 * generic and static properties (ex : all Keltir have the same number of
 * HP...). All of those properties are stored in a different template for each
 * type of L2Character. Each template is loaded once in the server cache memory
 * (reduce memory use). When a new instance of L2Character is spawned, server
 * just create a link between the instance and the template. This link is stored
 * in <B>_template</B><BR><BR>
 *
 *
 * @version $Revision: 1.53.2.45.2.34 $ $Date: 2005/04/11 10:06:08 $
 */
public abstract class L2Character extends L2Object {

    protected static final Logger _log = Logger.getLogger(L2Character.class.getName());
    // =========================================================
    // Data Field
    private List<L2Character> _attackByList;
    // private L2Character _attackingChar;
    private L2Skill _lastSkillCast;
    private boolean _isAfraid = false; // Flee in a random direction
    private boolean _isConfused = false; // Attack anyone randomly
    private boolean _isFakeDeath = false; // Fake death
    private boolean _isFlying = false; //Is flying Wyvern?
    private boolean _isMuted = false; // Cannot use magic
    private boolean _isPsychicalMuted = false; // Cannot use psychical skills
    private boolean _isKilledAlready = false;
    private boolean _isImobilised = false;
    private boolean _isOverloaded = false; // the char is carrying too much
    private boolean _isParalyzed = false;
    private boolean _isRiding = false; //Is Riding strider?
    private boolean _isPendingRevive = false;
    private boolean _isRooted = false; // Cannot move until root timed out
    private boolean _isRunning = false;
    private boolean _isImmobileUntilAttacked = false; // Is in immobile until attacked.
    private boolean _isSleeping = false; // Cannot move/attack until sleep timed out or monster is attacked
    private boolean _isStunned = false; // Cannot move/attack until stun timed out
    private boolean _isBetrayed = false; // Betrayed by own summon
    protected boolean _showSummonAnimation = false;
    protected boolean _isTeleporting = false;
    private L2Character _lastBuffer = null;
    protected boolean _isInvul = false;
    private int _lastHealAmount = 0;
    private CharStat _stat;
    private CharStatus _status;
    private L2CharTemplate _template;                       // The link on the L2CharTemplate object containing generic and static properties of this L2Character type (ex : Max HP, Speed...)
    private String _title = "";
    private String _aiClass = "default";
    private double _hpUpdateIncCheck = .0;
    private double _hpUpdateDecCheck = .0;
    private double _hpUpdateInterval = .0;
    private boolean _champion = false;
    private boolean _champion1 = false;
    /**
     * Table of Calculators containing all used calculator
     */
    private Calculator[] _calculators;
    /**
     * FastMap(Integer, L2Skill) containing all skills of the L2Character
     */
    protected Map<Integer, L2Skill> _skills;
    /**
     * FastMap containing the active chance skills on this character
     */
    protected ChanceSkillList _chanceSkills;
    /**
     * Zone system
     */
    public static final int ZONE_PVP = 1;
    public static final int ZONE_PEACE = 2;
    public static final int ZONE_SIEGE = 4;
    public static final int ZONE_MOTHERTREE = 8;
    public static final int ZONE_CLANHALL = 16;
    public static final int ZONE_UNUSED = 32;
    public static final int ZONE_NOLANDING = 64;
    public static final int ZONE_WATER = 128;
    public static final int ZONE_JAIL = 256;
    public static final int ZONE_MONSTERTRACK = 512;
    public static final int ZONE_BOSS = 1024;
    private int _currentZones = 0;

    public boolean isInsideZone(int zone) {
        return ((_currentZones & zone) != 0);
    }

    public void setInsideZone(int zone, boolean state) {
        if (state) {
            _currentZones |= zone;
        } else if (isInsideZone(zone)) // zone overlap possible
        {
            _currentZones ^= zone;
        }
    }

    public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage) {
        // Default: NPCs consume virtual items for their skills
        // TODO: should be logged if even happens.. should be false
        return true;
    }

    // =========================================================
    // Constructor
    /**
     * Constructor of L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> Each L2Character owns generic and static
     * properties (ex : all Keltir have the same number of HP...). All of those
     * properties are stored in a different template for each type of
     * L2Character. Each template is loaded once in the server cache memory
     * (reduce memory use). When a new instance of L2Character is spawned,
     * server just create a link between the instance and the template This link
     * is stored in <B>_template</B><BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Set the _template of the L2Character
     * </li> <li>Set _overloaded to false (the charcater can take more
     * items)</li><BR><BR>
     *
     * <li>If L2Character is a L2NPCInstance, copy skills from template to
     * object</li> <li>If L2Character is a L2NPCInstance, link _calculators to
     * NPC_STD_CALCULATOR</li><BR><BR>
     *
     * <li>If L2Character is NOT a L2NPCInstance, create an empty _skills
     * slot</li> <li>If L2Character is a L2PcInstance or L2Summon, copy basic
     * Calculator set to object</li><BR><BR>
     *
     * @param objectId Identifier of the object to initialized
     * @param template The L2CharTemplate to apply to the object
     */
    public L2Character(int objectId, L2CharTemplate template) {
        super(objectId);
        init(template);
    }

    private void init(L2CharTemplate template) {
        getKnownList();

        // Set its template to the new L2Character
        _template = template;

        if (template != null && _template instanceof L2NpcTemplate && isL2Npc()) {
            // Copy the Standard Calcultors of the L2NPCInstance in _calculators
            _calculators = NPC_STD_CALCULATOR;

            // Copy the skills of the L2NPCInstance from its template to the L2Character Instance
            // The skills list can be affected by spell effects so it's necessary to make a copy
            // to avoid that a spell affecting a L2NPCInstance, affects others L2NPCInstance of the same type too.
            _skills = template.getSkills();
            if (_skills != null) {
                for (Map.Entry<Integer, L2Skill> skill : _skills.entrySet()) {
                    addStatFuncs(skill.getValue().getStatFuncs(null, this));
                }
            }
        } else {
            // Initialize the FastMap _skills to null
            //_skills = new FastMap<Integer,L2Skill>().shared("L2Character._skills");
            _skills = new ConcurrentHashMap<Integer, L2Skill>();

            // If L2Character is a L2PcInstance or a L2Summon, create the basic calculator set
            _calculators = new Calculator[Stats.NUM_STATS];
            Formulas.addFuncsToNewCharacter(this);
        }
    }

    protected void initCharStatusUpdateValues() {
        _hpUpdateInterval = getMaxHp() / 352.0; // MAX_HP div MAX_HP_BAR_PX
        _hpUpdateIncCheck = getMaxHp();
        _hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
    }

    // =========================================================
    // Event - Public
    /**
     * Remove the L2Character from the world when the decay task is
     * launched.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the
     * object from _allObjects of L2World </B></FONT><BR> <FONT
     * COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND
     * Server->Client packets to players</B></FONT><BR><BR>
     *
     */
    public void onDecay() {
        L2WorldRegion reg = getWorldRegion();
        if (reg != null) {
            reg.removeFromZones(this);
        }
        decayMe();
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        this.revalidateZone();
    }

    public void onTeleported() {
        if (!isTeleporting()) {
            return;
        }

        spawnMe(getPosition().getX(), getPosition().getY(), getPosition().getZ());

        setIsTeleporting(false);

        if (_isPendingRevive) {
            doRevive();
        }

        // Modify the position of the pet if necessary
        if (getPet() != null) {
            //getPet().setFollowStatus(false);
            //getPet().teleToLocation(getPosition().getX() + Rnd.get(-100,100), getPosition().getY() + Rnd.get(-100,100), getPosition().getZ(), false);
            ((L2SummonAI) getPet().getAI()).setStartFollowController(true);
            getPet().setFollowStatus(true);
            getPet().updateAndBroadcastStatus(0);
        }
        sendActionFailed();
    }

    // =========================================================
    // Method - Public
    /**
     * Add L2Character instance that is attacking to the attacker list.<BR><BR>
     *
     * @param player The L2Character that attcks this one
     */
    public void addAttackerToAttackByList(L2Character player) {
        if (player == null || player == this || getAttackByList() == null || getAttackByList().contains(player)) {
            return;
        }
        getAttackByList().add(player);
    }

    /**
     * Send a packet to the L2Character AND to all L2PcInstance in the
     * _KnownPlayers of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> L2PcInstance in the detection area of
     * the L2Character are identified in <B>_knownPlayers</B>. In order to
     * inform other players of state modification on the L2Character, server
     * just need to go through _knownPlayers to send Server->Client
     * Packet<BR><BR>
     *
     */
    public void broadcastPacket(final L2GameServerPacket packet) {
        if (!(packet.isCharInfo())) {
            sendPacket(packet);
        }

        Broadcast.toKnownPlayers(this, packet);
    }

    public void broadcastSoulShotsPacket(final L2GameServerPacket mov) {
        if (showSoulShotsAnim()) {
            sendPacket(mov);
        }

        Broadcast.broadcastSoulShotsPacket(mov, getKnownList().getListKnownPlayers(), null);
    }

    /**
     * Send a packet to the L2Character AND to all L2PcInstance in the radius
     * (max knownlist radius) from the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> L2PcInstance in the detection area of
     * the L2Character are identified in <B>_knownPlayers</B>. In order to
     * inform other players of state modification on the L2Character, server
     * just need to go through _knownPlayers to send Server->Client
     * Packet<BR><BR>
     *
     */
    public final void broadcastPacket(L2GameServerPacket packet, int radius) {
        if (!(packet.isCharInfo())) {
            sendPacket(packet);
        }
        Broadcast.toKnownPlayersInRadius(this, packet, radius, true);
    }

    /**
     * Returns true if hp update should be done, false if not
     *
     * @return boolean
     */
    protected boolean needHpUpdate(int barPixels) {
        double currentHp = getCurrentHp();

        if (currentHp <= 1.0 || getMaxHp() < barPixels) {
            return true;
        }

        if (currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck) {
            if (currentHp == getMaxHp()) {
                _hpUpdateIncCheck = currentHp + 1;
                _hpUpdateDecCheck = currentHp - _hpUpdateInterval;
            } else {
                double doubleMulti = currentHp / _hpUpdateInterval;
                int intMulti = (int) doubleMulti;

                _hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
                _hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
            }
            return true;
        }
        return false;
    }

    /**
     * Send the Server->Client packet StatusUpdate with current HP and MP to all
     * other L2PcInstance to inform.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Create the Server->Client packet
     * StatusUpdate with current HP and MP </li> <li>Send the Server->Client
     * packet StatusUpdate with current HP and MP to all L2Character called
     * _statusListener that must be informed of HP/MP updates of this
     * L2Character </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND CP
     * information</B></FONT><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance : Send current
     * HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all
     * other L2PcInstance of the Party</li><BR><BR>
     *
     */
    public void broadcastStatusUpdate() {
        final CopyOnWriteArraySet<L2Character> list = getStatus().getStatusListener();
        if (list == null || list.isEmpty()) {
            return;
        }

        if (!needHpUpdate(352)) {
            return;
        }

        final StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
        su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
        su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());

        L2Object target = null;
        for (final L2Character temp : list) {
            if (temp == null) {
                continue;
            }

            target = temp.getTarget();
            if (target == null) {
                continue;
            }

            if (target.equals(this)) {
                temp.sendPacket(su);
            }
        }
        target = null;
    }

    /**
     * Not Implemented.<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     */
    /*
     * @Override public void sendPacket(L2GameServerPacket mov) { // default
     * implementation }
     */
    /**
     * Teleport a L2Character and its pet if necessary.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Stop the movement of the
     * L2Character</li> <li>Set the x,y,z position of the L2Object and if
     * necessary modify its _worldRegion</li> <li>Send a Server->Client packet
     * TeleportToLocationt to the L2Character AND to all L2PcInstance in its
     * _KnownPlayers</li> <li>Modify the position of the pet if
     * necessary</li><BR><BR>
     *
     */
    public void teleToLocation(int x, int y, int z, boolean allowRandomOffset) {
        if (this != null) {
            if (isInJail()) {
                return;
            }

            // Stop movement
            stopMove(null, false);
            abortAttack();
            abortCast();

            setIsTeleporting(true);
            setTarget(null);

            // Modify the position of the pet if necessary
            if (getPet() != null) {
                getPet().setFollowStatus(false);
                getPet().teleToLocation(x, y, z, false);
            }

            // Remove from world regions zones
            if (getWorldRegion() != null) {
                getWorldRegion().removeFromZones(this);
            }

            getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

            /*
             * if (Config.RESPAWN_RANDOM_ENABLED && allowRandomOffset) { x +=
             * Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET,
             * Config.RESPAWN_RANDOM_MAX_OFFSET); y +=
             * Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET,
             * Config.RESPAWN_RANDOM_MAX_OFFSET); }
             */
            z += 5;

            //if (Config.DEBUG)
            //	_log.fine("Teleporting to: " + x + ", " + y + ", " + z);
            // Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
            broadcastPacket(new TeleportToLocation(this, x, y, z, true));

            // Set the x,y,z position of the L2Object and if necessary modify its _worldRegion
            getPosition().setXYZ(x, y, z);

            decayMe();

            if (!(this.isPlayer())) {
                onTeleported();
            }
        }
    }

    public void teleToLocation(int x, int y, int z) {
        teleToLocation(x, y, z, false);
    }

    public void teleToLocation(Location loc, boolean allowRandomOffset) {
        if (this != null) {
            int x = loc.getX();
            int y = loc.getY();
            int z = loc.getZ();

            if (isPlayer() && DimensionalRiftManager.getInstance().checkIfInRiftZone(getX(), getY(), getZ(), true)) // true -> ignore waiting room :)
            {
                sendUserPacket(Static.SENT_TO_WAITING_ROOM);
                if (isInParty() && getParty().isInDimensionalRift()) {
                    getParty().getDimensionalRift().usedTeleport(getPlayer());
                }

                int[] newCoords = DimensionalRiftManager.getInstance().getRoom((byte) 0, (byte) 0).getTeleportCoords();
                x = newCoords[0];
                y = newCoords[1];
                z = newCoords[2];
            }
            teleToLocation(x, y, z, allowRandomOffset);
        }
    }

    public void teleToLocation(TeleportWhereType teleportWhere) {
        if (this != null) {
            teleToLocation(MapRegionTable.getInstance().getTeleToLocation(this, teleportWhere), true);
        }
    }

    //Jail
    public void teleToJail() {
        if (this != null) {
            // Stop movement
            stopMove(null, false);
            abortAttack();
            abortCast();

            setIsTeleporting(true);
            setTarget(null);

            // Modify the position of the pet if necessary
            if (getPet() != null) {
                getPet().setFollowStatus(false);
                getPet().unSummon(getPlayer());
            }

            // Remove from world regions zones
            if (getWorldRegion() != null) {
                getWorldRegion().removeFromZones(this);
            }

            getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

            // Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
            broadcastPacket(new TeleportToLocation(this, -114356, -249645, -2984, true));

            // Set the x,y,z position of the L2Object and if necessary modify its _worldRegion
            getPosition().setXYZ(-114356, -249645, -2989);

            decayMe();

            if (!(this.isPlayer())) {
                onTeleported();
            }
        }
    }
    // =========================================================
    // Method - Private
    /**
     * Launch a physical attack against a target (Simple, Bow, Pole or
     * Dual).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Get the active weapon (always
     * equiped in the right hand) </li><BR><BR> <li>If weapon is a bow, check
     * for arrows, MP and bow re-use delay (if necessary, equip the L2PcInstance
     * with arrows in left hand)</li> <li>If weapon is a bow, consume MP and set
     * the new period of bow non re-use </li><BR><BR> <li>Get the Attack Speed
     * of the L2Character (delay (in milliseconds) before next attack) </li>
     * <li>Select the type of attack to start (Simple, Bow, Pole or Dual) and
     * verify if SoulShot are charged then start calculation</li> <li>If the
     * Server->Client packet Attack contains at least 1 hit, send the
     * Server->Client packet Attack to the L2Character AND to all L2PcInstance
     * in the _KnownPlayers of the L2Character</li> <li>Notify AI with
     * EVT_READY_TO_ACT</li><BR><BR>
     *
     * @param target The L2Character targeted
     *
     */
    private L2CharPosition _nextLoc;

    public void setNextLoc(int x, int y, int z) {
        _nextLoc = new L2CharPosition(x, y, z, 0);
    }

    public void clearNextLoc() {
        _nextLoc = null;
    }

    public boolean checkNextLoc() {
        if (_nextLoc == null) {
            return false;
        }

        getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, _nextLoc);
        return true;
    }

    protected void doAttack(L2Character target) {
        if (isAttackingNow()) {
            sendActionFailed();
            return;
        }

        if (this.isL2Attackable() && isAttackingDisabled()) {
            sendActionFailed();
            return;
        }

        if (isAlikeDead() || target == null || (this.isL2Npc() && target.isAlikeDead())
                || (this.isPlayer() && target.isDead() && !target.isFakeDeath())
                || !getKnownList().knowsObject(target)
                || (this.isPlayer() && isDead())
                || (target.isPlayer() && target.getDuel() != null && (target.getDuel().getDuelState(target.getPlayer()) == DuelState.Dead))) {
            // If L2PcInstance is dead or the target is dead, the action is stoped
            getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
            sendActionFailed();
            return;
        }

        if (target.isL2Door() && !target.isAutoAttackable(this)) {
            getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
            sendActionFailed();
            return;
        }

        if (isForbidWeapon(getActiveWeaponInstance())) {
            sendActionFailed();
            return;
        }

        updateLastTeleport(false);

        if (isPlayer()) {
            if (inObserverMode()) {
                sendUserPacket(Static.OBSERVERS_CANNOT_PARTICIPATE);
                sendActionFailed();
                return;
            }

            // Checking if target has moved to peace zone
            if (isHippy() || PeaceZone.getInstance().inPeace(this, target)) {
                getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
                sendActionFailed();
                return;
            }

            if (target.isPlayer()) {
                if (target.isCursedWeaponEquiped() && getLevel() <= 20) {
                    sendUserPacket(Static.CW_21);
                    sendActionFailed();
                    return;
                }

                if (isCursedWeaponEquiped() && target.getLevel() <= 20) {
                    sendUserPacket(Static.CW_20);
                    sendActionFailed();
                    return;
                }

                if (getObjectId() == target.getObjectId()) {
                    sendActionFailed();
                    return;
                }
            }
        }

        // Get the active weapon instance (always equiped in the right hand)
        L2ItemInstance weaponInst = getActiveWeaponInstance();

        // Get the active weapon item corresponding to the active weapon instance (always equiped in the right hand)
        L2Weapon weaponItem = getActiveWeaponItem();
        if (weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD) {
            //	You can't make an attack with a fishing pole.
            sendUserPacket(Static.CANNOT_ATTACK_WITH_FISHING_POLE);
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

            sendActionFailed();
            return;
        }

        // GeoData Los Check here (or dz > 1000)
        if (!canSeeTarget(target)) {
            sendUserPacket(Static.CANT_SEE_TARGET);
            getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
            sendActionFailed();
            return;
        }

        int ssGrade = 0;
        int timeAtk = Config.MIN_ATKSPD_DELAY > 0 ? Math.max(calculateTimeBetweenAttacks(target, weaponItem), Config.MIN_ATKSPD_DELAY) : calculateTimeBetweenAttacks(target, weaponItem);
        // Check for a bow
        if (weaponItem != null) {
            if (isPlayer() && weaponItem.getItemType() == L2WeaponType.BOW) {
                //Check for arrows and MP
                if (!target.isAttackable()) {
                    sendActionFailed();
                    //System.out.println("!!!2");
                    return;
                }

                int saMpConsume = (int) getStat().calcStat(Stats.MP_CONSUME, 0, null, null);
                int mpConsume = saMpConsume == 0 ? weaponItem.getMpConsume() : saMpConsume;
                if (getCurrentMp() < mpConsume) {
                    sendUserPacket(Static.NOT_ENOUGH_MP);
                    sendActionFailed();
                    return;
                }
                getStatus().reduceMp(mpConsume);

                if (!checkAndEquipArrows()) {
                    getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

                    sendActionFailed();
                    sendUserPacket(Static.NOT_ENOUGH_ARROWS);
                    return;
                }

                int reuse = (int) (weaponItem.getAttackReuseDelay() * getStat().getWeaponReuseModifier(target) * 666 * getTemplate().basePAtkSpd / 293. / getPAtkSpd());
                if (reuse > 0) {
                    sendUserPacket(new SetupGauge(SetupGauge.RED, reuse));
                    _attackReuseEndTime = reuse + System.currentTimeMillis() - 75;
                    if (reuse > timeAtk) {
                        ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), reuse, isPlayer() || isPet() || isSummon());
                    }
                }
            }
            ssGrade = weaponItem.getCrystalType();
        }

        // Add the L2PcInstance to _knownObjects and _knownPlayer of the target
        target.getKnownList().addKnownObject(this);

        // Verify if soulshots are charged.
        boolean wasSSCharged;

        if (this.isL2Summon() && !(this.isPet())) {
            wasSSCharged = (getChargedSoulShot() != L2ItemInstance.CHARGED_NONE);
        } else {
            wasSSCharged = (weaponInst != null && weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE);
        }

        _attackEndTime = timeAtk + System.currentTimeMillis() - 60;

        if (isFantome()) {
            wasSSCharged = true;
            ssGrade = 5;
        }

        // Create a Server->Client packet Attack
        Attack attack = new Attack(this, wasSSCharged, ssGrade);

        boolean hitted;

        // Set the Attacking Body part to CHEST
        setAttackingBodypart();

        // Get the Attack Reuse Delay of the L2Weapon
        //int reuse = calculateReuseTime(target, weaponItem);
        setHeading(Util.calculateHeadingFrom(this, target));

        // Select the type of attack to start
        if (weaponItem == null || isMounted()) {
            stopMove(null);
            hitted = doAttackHitSimple(attack, target, timeAtk);
        } else {
            switch (weaponItem.getItemType()) {
                case BOW:
                    hitted = doAttackHitByBow(attack, target, timeAtk);
                    break;
                case POLE:
                    hitted = doAttackHitByPole(attack, timeAtk);
                    break;
                case DUAL:
                case DUALFIST:
                    hitted = doAttackHitByDual(attack, target, timeAtk);
                    break;
                default:
                    hitted = doAttackHitSimple(attack, target, timeAtk);
                    break;
            }
        }

        // Flag the attacker if it's a L2PcInstance outside a PvP area
        if (target.isPlayer() || target.isL2Summon() || target.isPet()) {
            if (getPet() != target) {
                updatePvPStatus(target);
            }
        }

        // Check if hit isn't missed
        if (!hitted) // Abort the attack of the L2Character and send Server->Client ActionFailed packet
        {
            abortAttack(this.isL2Attackable(), timeAtk);
        } else {
            /*
             * ADDED BY nexus - 2006-08-17
             *
             * As soon as we know that our hit landed, we must discharge any
             * active soulshots. This must be done so to avoid unwanted soulshot
             * consumption.
             */

            // If we didn't miss the hit, discharge the shoulshots, if any
            if (this.isL2Summon() && !(this.isPet())) {
                setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
            } else if (weaponInst != null) {
                weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
            }

            if (isCursedWeaponEquiped()) {
                // If hitted by a cursed weapon, Cp is reduced to 0
                if (!target.isInvul()) {
                    target.setCurrentCp(0);
                }
            } else if (isHero()) {
                if (target.isPlayer() && target.isCursedWeaponEquiped()) // If a cursed weapon is hitted by a Hero, Cp is reduced to 0
                {
                    target.setCurrentCp(0);
                }
            }
        }

        // If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
        // to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
        if (attack.hasHits()) {
            broadcastPacket(attack);
        }

        setLastHit();
        // Notify AI with EVT_READY_TO_ACT
        //ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), timeAtk/* + reuse*/, isPlayer() || isPet() || isSummon());
    }

    public boolean isForbidWeapon(L2ItemInstance weapon) {
        if (weapon == null) {
            return false;
        }
        if (weapon.isBowWeapon()) {
            if (isInOlympiadMode()) {
                if (Config.FORBIDDEN_BOW_CLASSES_OLY.contains(getClassId().getId())) {
                    sendUserPacket(Static.FORBIDDEN_BOW_CLASS_OLY);
                    return true;
                }
                return false;
            }
            if (Config.FORBIDDEN_BOW_CLASSES.contains(getClassId().getId())) {
                sendUserPacket(Static.FORBIDDEN_BOW_CLASS);
                return true;
            }
        }
        if (weapon.isFistWeapon() && Config.FORBIDDEN_FIST_CLASSES.contains(getClassId().getId())) {
            sendUserPacket(Static.FORBIDDEN_FIST_CLASS);
            return true;
        }
        if (weapon.isDualWeapon() && Config.FORBIDDEN_DUAL_CLASSES.contains(getClassId().getId())) {
            sendUserPacket(Static.FORBIDDEN_DUAL_CLASS);
            return true;
        }
        return false;
    }

    // запрет на атаку луком конкретным профам
    public boolean isForbidBow(L2ItemInstance weapon) {
        if (weapon == null || !weapon.isBowWeapon()) {
            return false;
        }
        if (isInOlympiadMode()) {
            if (Config.FORBIDDEN_BOW_CLASSES_OLY.contains(getClassId().getId())) {
                sendUserPacket(Static.FORBIDDEN_BOW_CLASS_OLY);
                return true;
            }
            return false;
        }

        if (Config.FORBIDDEN_BOW_CLASSES.contains(getClassId().getId())) {
            return true;
        }

        return false;
    }

    // запрет на атаку костетами конкретным профам
    public boolean isForbidFist(L2ItemInstance weapon) {
        if (weapon == null || !weapon.isFistWeapon()) {
            return false;
        }
        if (Config.FORBIDDEN_FIST_CLASSES.contains(getClassId().getId())) {
            return true;
        }
        return false;
    }

    // запрет на атаку дуалами конкретным профам
    public boolean isForbidDual(L2ItemInstance weapon) {
        if (weapon == null || !weapon.isDualWeapon()) {
            return false;
        }
        if (Config.FORBIDDEN_DUAL_CLASSES.contains(getClassId().getId())) {
            return true;
        }
        return false;
    }

    /**
     * Return the Attack Speed of the L2Character (delay (in milliseconds)
     * before next attack).<BR><BR>
     */
    public int calculateTimeBetweenAttacks(L2Character target, L2Weapon weapon) {
        return Formulas.calcPAtkSpd(this, target, getPAtkSpd());
    }

    public int calculateReuseTime(L2Character target, L2Weapon weapon) {
        if (weapon == null) {
            return 0;
        }

        int reuse = weapon.getAttackReuseDelay();
        // only bows should continue for now
        if (reuse == 0) {
            return 0;
        }
        // else if (reuse < 10) reuse = 1500;

        reuse *= getStat().getWeaponReuseModifier(target);
        double atkSpd = getPAtkSpd();
        switch (weapon.getItemType()) {
            case BOW:
                return (int) (reuse * 345 / atkSpd);
            default:
                return (int) (reuse * 312 / atkSpd);
        }
    }

    /**
     * Launch a Bow attack.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Calculate if hit is missed or not
     * </li> <li>Consumme arrows </li> <li>If hit isn't missed, calculate if
     * shield defense is efficient </li> <li>If hit isn't missed, calculate if
     * hit is critical </li> <li>If hit isn't missed, calculate physical damages
     * </li> <li>If the L2Character is a L2PcInstance, Send a Server->Client
     * packet SetupGauge </li> <li>Create a new hit task with Medium
     * priority</li> <li>Calculate and set the disable delay of the bow in
     * function of the Attack Speed</li> <li>Add this hit to the Server-Client
     * packet Attack </li><BR><BR>
     *
     * @param attack Server->Client packet Attack in which the hit will be added
     * @param target The L2Character targeted
     * @param sAtk The Attack Speed of the attacker
     *
     * @return True if the hit isn't missed
     *
     */
    private boolean doAttackHitByBow(Attack attack, L2Character target, int sAtk) {
        /*
         * if (getFirstEffect(413) != null && !Util.checkIfInRange(600, this,
         * target, false)) return false;
         */

        int damage1 = 0;
        boolean shld1 = false;
        boolean crit1 = false;

        // Calculate if hit is missed or not
        boolean miss1 = Formulas.calcHitMiss(this, target);

        // Consumme arrows
        reduceArrowCount();

        _move = null;

        // Check if hit isn't missed
        if (!miss1) {
            // Calculate if shield defense is efficient
            shld1 = Formulas.calcShldUse(this, target);

            // Calculate if hit is critical
            crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

            // Calculate physical damages
            damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);
        }

        // Check if the L2Character is a L2PcInstance
        //if (this.isPlayer())
        //{
        // Send a system message
        sendUserPacket(Static.GETTING_READY_TO_SHOOT_AN_ARROW);

        // Send a Server->Client packet SetupGauge
        //}
        // Create a new hit task with Medium priority
        ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk, isPlayer() || isPet() || isSummon());

        // Calculate and set the disable delay of the bow in function of the Attack Speed
        //_disableBowAttackEndTime = (sAtk) / GameTimeController.MILLIS_IN_TICK + GameTimeController.getGameTicks();
        // Add this hit to the Server-Client packet Attack
        attack.addHit(target, damage1, miss1, crit1, shld1);

        // Return true if hit isn't missed
        return !miss1;
    }
    private long _attackReuseEndTime;

    /**
     * Launch a Dual attack.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Calculate if hits are missed or not
     * </li> <li>If hits aren't missed, calculate if shield defense is efficient
     * </li> <li>If hits aren't missed, calculate if hit is critical </li>
     * <li>If hits aren't missed, calculate physical damages </li> <li>Create 2
     * new hit tasks with Medium priority</li> <li>Add those hits to the
     * Server-Client packet Attack </li><BR><BR>
     *
     * @param attack Server->Client packet Attack in which the hit will be added
     * @param target The L2Character targeted
     *
     * @return True if hit 1 or hit 2 isn't missed
     *
     */
    private boolean doAttackHitByDual(Attack attack, L2Character target, int sAtk) {
        int damage1 = 0;
        int damage2 = 0;
        boolean shld1 = false;
        boolean shld2 = false;
        boolean crit1 = false;
        boolean crit2 = false;

        // Calculate if hits are missed or not
        boolean miss1 = Formulas.calcHitMiss(this, target);
        boolean miss2 = Formulas.calcHitMiss(this, target);

        // Check if hit 1 isn't missed
        if (!miss1) {
            // Calculate if shield defense is efficient against hit 1
            shld1 = Formulas.calcShldUse(this, target);

            // Calculate if hit 1 is critical
            crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

            // Calculate physical damages of hit 1
            damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, true, attack.soulshot);
            damage1 /= 2;
        }

        // Check if hit 2 isn't missed
        if (!miss2) {
            // Calculate if shield defense is efficient against hit 2
            shld2 = Formulas.calcShldUse(this, target);

            // Calculate if hit 2 is critical
            crit2 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

            // Calculate physical damages of hit 2
            damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, true, attack.soulshot);
            damage2 /= 2;
        }

        // Create a new hit task with Medium priority for hit 1
        ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk / 2, isPlayer() || isPet() || isSummon());

        // Create a new hit task with Medium priority for hit 2 with a higher delay
        ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, attack.soulshot, shld2), sAtk, isPlayer() || isPet() || isSummon());

        // Add those hits to the Server-Client packet Attack
        attack.addHit(target, damage1, miss1, crit1, shld1);
        attack.addHit(target, damage2, miss2, crit2, shld2);

        // Return true if hit 1 or hit 2 isn't missed
        return (!miss1 || !miss2);
    }

    /**
     * Launch a Pole attack.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Get all visible objects in a spheric
     * area near the L2Character to obtain possible targets </li> <li>If
     * possible target is the L2Character targeted, launch a simple attack
     * against it </li> <li>If possible target isn't the L2Character targeted
     * but is attakable, launch a simple attack against it </li><BR><BR>
     *
     * @param attack Server->Client packet Attack in which the hit will be added
     *
     * @return True if one hit isn't missed
     *
     */
    private boolean doAttackHitByPole(Attack attack, int sAtk) {
        if (getTarget() == null) {
            return false;
        }

        boolean hitted = false;

        double angleChar, angleTarget;
        int maxRadius = (int) getStat().calcStat(Stats.POWER_ATTACK_RANGE, 66, null, null);
        int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);

        //if (Config.DEBUG)
        //{
        //    _log.info("doAttackHitByPole: Max radius = " + maxRadius);
        //    _log.info("doAttackHitByPole: Max angle = " + maxAngleDiff);
        //}
        // o1 x: 83420 y: 148158 (Giran)
        // o2 x: 83379 y: 148081 (Giran)
        // dx = -41
        // dy = -77
        // distance between o1 and o2 = 87.24
        // arctan2 = -120 (240) degree (excel arctan2(dx, dy); java arctan2(dy, dx))
        //
        // o2
        //
        //          o1 ----- (heading)
        // In the diagram above:
        // o1 has a heading of 0/360 degree from horizontal (facing East)
        // Degree of o2 in respect to o1 = -120 (240) degree
        //
        // o2          / (heading)
        //            /
        //          o1
        // In the diagram above
        // o1 has a heading of -80 (280) degree from horizontal (facing north east)
        // Degree of o2 in respect to 01 = -40 (320) degree
        // ===========================================================
        // Make sure that char is facing selected target
        angleTarget = Util.calculateAngleFrom(this, getTarget());
        setHeading((int) ((angleTarget / 9.0) * 1610.0)); // = this.setHeading((int)((angleTarget / 360.0) * 64400.0));

        // Update char's heading degree
        angleChar = Util.convertHeadingToDegree(getHeading());
        double attackpercent = 85;
        int attackcountmax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 3, null, null);
        int attackcount = 0;

        if (angleChar <= 0) {
            angleChar += 360;
        }
        // ===========================================================

        L2Character target;
        FastList<L2Character> objs = getKnownList().getKnownCharactersInRadius(maxRadius);
        for (FastList.Node<L2Character> n = objs.head(), end = objs.tail(); (n = n.getNext()) != end;) {
            target = n.getValue();
            if (target == null) {
                continue;
            }

            if (target.isAlikeDead()) {
                continue;
            }

            //otherwise hit too high/low. 650 because mob z coord sometimes wrong on hills
            if (Math.abs(target.getZ() - getZ()) > 650) {
                continue;
            }

            if (PeaceZone.getInstance().inPeace(this, target)) {
                continue;
            }

            if (target.isPet() && this.isPlayer() && target.getOwner() == getPlayer()) {
                continue;
            }

            angleTarget = Util.calculateAngleFrom(this, target);
            if (Math.abs(angleChar - angleTarget) > maxAngleDiff
                    && Math.abs((angleChar + 360) - angleTarget) > maxAngleDiff
                    && // Example: char is at 1 degree and target is at 359 degree
                    Math.abs(angleChar - (angleTarget + 360)) > maxAngleDiff) // Example: target is at 1 degree and char is at 359 degree
            {
                continue;
            }

            if (isL2Guard() && target.isFantome()) {
                //if (target.rndWalk(this, true)) {
                continue;
                //}
            }

            attackcount += 1;
            if (attackcount <= attackcountmax) {
                if (target == getAI().getAttackTarget() || target.isAutoAttackable(this)) {

                    hitted |= doAttackHitSimple(attack, target, attackpercent, sAtk);
                    attackpercent /= 1.15;
                }
            }
        }
        objs.clear();
        objs = null;
        target = null;

        // Return true if one hit isn't missed
        return hitted;
    }

    /**
     * Launch a simple attack.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Calculate if hit is missed or not
     * </li> <li>If hit isn't missed, calculate if shield defense is efficient
     * </li> <li>If hit isn't missed, calculate if hit is critical </li> <li>If
     * hit isn't missed, calculate physical damages </li> <li>Create a new hit
     * task with Medium priority</li> <li>Add this hit to the Server-Client
     * packet Attack </li><BR><BR>
     *
     * @param attack Server->Client packet Attack in which the hit will be added
     * @param target The L2Character targeted
     *
     * @return True if the hit isn't missed
     *
     */
    private boolean doAttackHitSimple(Attack attack, L2Character target, int sAtk) {
        return doAttackHitSimple(attack, target, 100, sAtk);
    }

    private boolean doAttackHitSimple(Attack attack, L2Character target, double attackpercent, int sAtk) {
        int damage1 = 0;
        boolean shld1 = false;
        boolean crit1 = false;

        // Calculate if hit is missed or not
        boolean miss1 = Formulas.calcHitMiss(this, target);

        // Check if hit isn't missed
        if (!miss1) {
            // Calculate if shield defense is efficient
            shld1 = Formulas.calcShldUse(this, target);

            // Calculate if hit is critical
            crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

            // Calculate physical damages
            damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);

            if (attackpercent != 100) {
                damage1 = (int) (damage1 * attackpercent / 100);
            }
        }

        // Create a new hit task with Medium priority
        ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk, isPlayer() || isPet() || isSummon());

        // Add this hit to the Server-Client packet Attack
        attack.addHit(target, damage1, miss1, crit1, shld1);

        // Return true if hit isn't missed
        return !miss1;
    }

    /**
     * Manage the casting task (casting and interrupt time, re-use delay...) and
     * display the casting bar and animation on client.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Verify the possibilty of the the
     * cast : skill is a spell, caster isn't muted... </li> <li>Get the list of
     * all targets (ex : area effects) and define the L2Charcater targeted (its
     * stats will be used in calculation)</li> <li>Calculate the casting time
     * (base + modifier of MAtkSpd), interrupt time and re-use delay</li>
     * <li>Send a Server->Client packet MagicSkillUser (to diplay casting
     * animation), a packet SetupGauge (to display casting bar) and a system
     * message </li> <li>Disable all skills during the casting time (create a
     * task EnableAllSkills)</li> <li>Disable the skill during the re-use delay
     * (create a task EnableSkill)</li> <li>Create a task MagicUseTask (that
     * will call method onMagicUseTimer) to launch the Magic Skill at the end of
     * the casting time</li><BR><BR>
     *
     * @param skill The L2Skill to use
     *
     */
    public void doCast(L2Skill skill) {
        //long start = System.currentTimeMillis();
        //System.out.println("######1");
        if (isHippy() || skill == null) {
            getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
            return;
        }

        /*
         * if (isSkillDisabled(skill.getId())) {
         * sendUserPacket(SystemMessage.id(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill.getId(),
         * skill.getLevel())); return; }
         */
        //System.out.println("1 / "+(System.currentTimeMillis()-start));
        //System.out.println("2 / "+(System.currentTimeMillis()-start));
        // Check if the skill is a magic spell and if the L2Character is not muted
        if (skill.isMagic() && isMuted() && !skill.isPotion()) {
            getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
            return;
        }
        // Check if the skill is psychical and if the L2Character is not psychical_muted
        if (!skill.isMagic() && isPsychicalMuted() && !skill.isPotion()) {
            getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
            return;
        }

        //System.out.println("4 / "+(System.currentTimeMillis()-start));
        // Can't use Hero and resurrect skills during Olympiad
        //System.out.println("5 / "+(System.currentTimeMillis()-start));
        //System.out.println("6 / "+(System.currentTimeMillis()-start));
        //System.out.println("??");
        // Get all possible targets of the skill in a table in function of the skill target type
        FastList<L2Object> targets = new FastList<L2Object>();
        // targets could be NULL or 0 here couse its not given that objects are in the pointed area
        if (!skill.isSignedTargetType()) {
            targets = skill.getTargetList(this);
            if (targets == null || targets.isEmpty()) {
                if (skill.isAoeOffensive()) {
                    targets.add(this);
                } else {
                    getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
                    return;
                }
            }
        }
        //System.out.println("!!");

        //System.out.println("7 / "+(System.currentTimeMillis()-start));
        // Set the target of the skill in function of Skill Type and Target Type
        L2Character target = null;
        if (skill.isSupportSkill() && skill.isSupportTargetType()) {
            target = (L2Character) targets.getFirst();
        }

        // AURA and SIGNET skills should always be using caster as target
        if (skill.isAuraSignedTargetType()) {
            target = this;
        }

        //System.out.println("8 / "+(System.currentTimeMillis()-start));
        if (target == null && getTarget() != null) {
            target = (L2Character) getTarget();
        }

        //System.out.println("9 / "+(System.currentTimeMillis()-start));
        if (target == null)// || this.getKnownSkill(skill.getId()) == null)	
        {
            getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
            return;
        }

        if (skill.getCastRange() > 0 && !canSeeTarget(target)) {
            sendUserPacket(Static.CANT_SEE_TARGET);
            sendActionFailed();
            return;
        }

        //System.out.println("!!!");
        //System.out.println("10"+(System.currentTimeMillis()-start));
        setLastSkillCast(skill);

        // Get the Identifier of the skill
        int magicId = skill.getId();
        // Get the casting time of the skill (base)
        int hitTime = skill.getHitTime();
        int coolTime = skill.getCoolTime();

        boolean hasEffectDelay = skill.getInitialEffectDelay() > 0;

        // Calculate the casting time of the skill (base + modifier of MAtkSpd)
        // Don't modify the skill time for FORCE_BUFF skills. The skill time for those skills represent the buff time.
        if (!hasEffectDelay) {
            hitTime = Formulas.calcMAtkSpd(this, skill, hitTime);
            if (coolTime > 0) {
                coolTime = Formulas.calcMAtkSpd(this, skill, coolTime);
            }
        }

        //System.out.println("11 / "+(System.currentTimeMillis()-start));
        // Calculate altered Cast Speed due to BSpS/SpS
        L2ItemInstance weaponInst = getActiveWeaponInstance();

        boolean wasSSCharged;
        if (this.isL2Summon()) {
            wasSSCharged = getChargedSpiritShot() != L2ItemInstance.CHARGED_NONE;
        } else {
            wasSSCharged = (weaponInst != null && weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE);
        }

        //System.out.println("12 / "+(System.currentTimeMillis()-start));
        if (skill.isMagic() && skill.getTargetType() != SkillTargetType.TARGET_SELF) {
            if (wasSSCharged) {
                //Only takes 70% of the time to cast a BSpS/SpS cast
                //if (skill.isNotAura())
                //{
                hitTime = (int) (0.80 * hitTime);
                coolTime = (int) (0.80 * coolTime);
                //}

                //Because the following are magic skills that do not actively 'eat' BSpS/SpS,
                //I must 'eat' them here so players don't take advantage of infinite speed increase
                if (skill.isUseSppritShot() && !skill.isPotion()) {
                    if (this.isSummon()) {
                        setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
                    } else if (weaponInst != null) {
                        weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
                    }
                }
            }
        }

        //System.out.println("13 / "+(System.currentTimeMillis()-start));
        // Set the _castEndTime and _castInterruptTim. +10 ticks for lag situations, will be reseted in onMagicFinalizer
        _castEndTime = 10 + GameTimeController.getGameTicks() + (coolTime + hitTime) / GameTimeController.MILLIS_IN_TICK;
        _castInterruptTime = GameTimeController.getGameTicks() + hitTime / GameTimeController.MILLIS_IN_TICK;

        //System.out.println("14 / "+(System.currentTimeMillis()-start));
        // Send a system message USE_S1 to the L2Character
        if (skill.sendSkillUseInfo()) {
            sendUserPacket(SystemMessage.id(SystemMessageId.USE_S1).addSkillName(magicId, skill.getLevel()));
        }

        // Init the reuse time of the skill
        int reuseDelay = 0;
        // Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
        if (Formulas.calcSkillMastery(this)) {
            sendUserPacket(Static.SKILL_READY_TO_USE_AGAIN);
        } else {
            reuseDelay = Math.max(10, calcReuseDelay(skill, reuseDelay));
            disableSkill(skill.getId(), reuseDelay);
        }

        //System.out.println("15 / "+(System.currentTimeMillis()-start));
        //System.out.println("16 / "+(System.currentTimeMillis()-start));
        // Send a Server->Client packet MagicSkillUser with target, displayId, level, skillTime, reuseDelay
        // to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
        //if (!skill.isBuff())
        broadcastPacket(new MagicSkillUser(this, target, skill.getDisplayId(), Math.max(1, skill.getLevel()), hitTime + skill.getInitialEffectDelay(), reuseDelay));

        //System.out.println("17 / "+(System.currentTimeMillis()-start));
        //System.out.println("18 / "+(System.currentTimeMillis()-start));
        //System.out.println("19 / "+(System.currentTimeMillis()-start));
        // Check if this skill consume mp on start casting
        int initmpcons = getStat().getMpInitialConsume(skill);
        if (initmpcons > 0) {
            //StatusUpdate su = new StatusUpdate(getObjectId());
            //getStatus().reduceMp(calcStat(Stats.MP_CONSUME_RATE,initmpcons,null,null));
            //su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
            //sendPacket(su);
            reduceCurrentMp(calcStat(Stats.MP_CONSUME_RATE, initmpcons, null, null));
        }

        //System.out.println("20 / "+(System.currentTimeMillis()-start))
        //System.out.println("22 / "+(System.currentTimeMillis()-start));	
        int finalTime = hitTime + skill.getInitialEffectDelay();
        //System.out.println("23 / "+(System.currentTimeMillis()-start));

        // launch the magic in hitTime milliseconds
        if (finalTime > Config.MIN_HIT_TIME) {
            // Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
            int initialEffectDelay = skill.getInitialEffectDelay();
            //if (this.isPlayer())
            //{
            if (hasEffectDelay) {
                sendUserPacket(new SetupGauge(SetupGauge.BLUE, initialEffectDelay));
            } else {
                sendUserPacket(new SetupGauge(SetupGauge.BLUE, finalTime));
            }
            //}

            // Disable all skills during the casting
            disableAllSkills();

            if (_skillCast != null) {
                _skillCast.cancel(true);
                _skillCast = null;
            }

            // Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
            // For client animation reasons (party buffs especially) 200 ms before! 
            if (hasEffectDelay) {
                _skillCast = EffectTaskManager.getInstance().schedule(new MagicUseTask(targets, skill, coolTime, 2, finalTime), initialEffectDelay);
            } else {
                _skillCast = EffectTaskManager.getInstance().schedule(new MagicUseTask(targets, skill, coolTime, 1, 0), finalTime - 200);
            }
        } else {
            onMagicLaunchedTimer(targets, skill, coolTime, true);
        }

        setLastHit();
        //System.out.println("######4" + targets.toString());
        //System.out.println("finish / "+(System.currentTimeMillis()-start));
    }

    private int calcReuseDelay(L2Skill skill, int reuseDelay) {
        if (skill.isFixedReuse()) {
            reuseDelay = skill.getReuseDelay();
        } else {
            if (skill.isMagic()) {
                reuseDelay = (int) (skill.getReuseDelay() * getStat().getMReuseRate(skill));
            } else {
                reuseDelay = (int) (skill.getReuseDelay() * getStat().getPReuseRate(skill));
            }
            reuseDelay *= 333.0 / calcAtkSpd(skill.isMagic());//(skill.isMagic() ? getMAtkSpd() : getPAtkSpd());
        }
        return reuseDelay;
    }

    public void setChargedSpiritShot(int shotType) {
        //
    }

    private int calcAtkSpd(boolean magic) {
        return magic ? Math.min(getMAtkSpd(), Config.MAX_MATKSPD_DELAY) : Math.min(getPAtkSpd(), Config.MAX_PATKSPD_DELAY);
    }

    /**
     * Reduce the current MP of the L2Character.<BR><BR>
     *
     * @param i The MP decrease value
     * @param attacker L2Character
     */
    public void reduceCurrentMp(double i, L2Character attacker) {
        if (attacker != null && attacker != this) {
            if (isSleeping()) {
                stopEffects(L2Effect.EffectType.SLEEP);
            }
        }

        i = getCurrentMp() - i;

        if (i < 0) {
            i = 0;
        }

        setCurrentMp(i);
    }

    /**
     * @return
     */
    private L2Character getActingPlayer() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Index according to skill id the current timestamp of use.<br><br>
     *
     * @param skill id
     * @param reuse delay <BR><B>Overriden in :</B> (L2PcInstance)
     */
    public void addTimeStamp(int s, int r) {
        /**
         *
         */
    }

    /**
     * Index according to skill id the current timestamp of use.<br><br>
     *
     * @param skill id <BR><B>Overriden in :</B> (L2PcInstance)
     */
    public void removeTimeStamp(int s) {
        /**
         *
         */
    }

    /**
     * Starts a force buff on target.<br><br>
     *
     * @param caster
     * @param force type <BR><B>Overriden in :</B> (L2PcInstance)
     */
    public void startForceBuff(L2Character caster, L2Skill skill) {
        /**
         *
         */
    }

    /**
     * Kill the L2Character.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Set target to null and cancel Attack
     * or Cast </li> <li>Stop movement </li> <li>Stop HP/MP/CP Regeneration task
     * </li> <li>Stop all active skills effects in progress on the L2Character
     * </li> <li>Send the Server->Client packet StatusUpdate with current HP and
     * MP to all other L2PcInstance to inform </li> <li>Notify L2Character AI
     * </li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2NpcInstance : Create a
     * DecayTask to remove the corpse of the L2NpcInstance after 7 seconds </li>
     * <li> L2Attackable : Distribute rewards (EXP, SP, Drops...) and notify
     * Quest Engine </li> <li> L2PcInstance : Apply Death Penalty, Manage
     * gain/loss Karma and Item Drop </li><BR><BR>
     *
     * @param killer The L2Character who killed it
     *
     */
    public boolean doDie(L2Character killer) {
        // killing is only possible one time
        synchronized (this) {
            if (isKilledAlready()) {
                return false;
            }
            setIsKilledAlready(true);
        }
        // Set target to null and cancel Attack or Cast
        setTarget(null);

        // Stop movement
        stopMove(null);

        // Stop HP/MP/CP Regeneration task
        getStatus().stopHpMpRegeneration();

        // Stop all active skills effects in progress on the L2Character,
        // if the Character isn't a Noblesse Blessed L2PlayableInstance
        if (isNoblesseBlessed()) {
            stopNoblesseBlessing(null);
            if (getCharmOfLuck()) //remove Lucky Charm if player have Nobless blessing buff 
            {
                stopCharmOfLuck(null);
            }
        } //Same thing if the Character is affected by Soul of The Phoenix or Salvation
        else if (this.isL2Playable() && isPhoenixBlessed()) {
            if (getCharmOfLuck()) //remove Lucky Charm if player has SoulOfThePhoenix/Salvation buff
            {
                stopCharmOfLuck(null);
            }
        } else if (Config.CLEAR_BUFF_ONDEATH) {
            stopAllEffects();
        }

        calculateRewards(killer);

        // Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        broadcastStatusUpdate();

        // Notify L2Character AI
        getAI().notifyEvent(CtrlEvent.EVT_DEAD, null);

        if (getWorldRegion() != null) {
            getWorldRegion().onDeath(this);
        }

        // Notify Quest of character's death
        for (QuestState qs : getNotifyQuestOfDeath()) {
            qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);
        }
        getNotifyQuestOfDeath().clear();

        getAttackByList().clear();

        //If character is PhoenixBlessed a resurrection popup will show up
        /*
         * if (this.isL2Player() && ((L2PcInstance)this).isPhoenixBlessed())
         * ((L2PcInstance)this).reviveRequest(((L2PcInstance)this),null,false);
         */
        return true;
    }

    protected void calculateRewards(L2Character killer) {
    }

    /**
     * Sets HP, MP and CP and revives the L2Character.
     */
    public void doRevive() {
        if (!isTeleporting()) {
            setIsPendingRevive(false);

            _status.setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_CP);
            _status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
            //_Status.setCurrentMp(getMaxMp() * Config.RESPAWN_RESTORE_MP);

            // Start broadcast status
            /*
             * broadcastPacket(new Revive(this)); if (getWorldRegion() != null)
             * getWorldRegion().onRevive(this);
             */
        }

        // Start broadcast status
        broadcastPacket(new Revive(this));
        if (getWorldRegion() != null) {
            getWorldRegion().onRevive(this);
        } else {
            setIsPendingRevive(true);
        }
    }

    /**
     * Revives the L2Character using skill.
     */
    public void doRevive(double revivePower) {
        doRevive();
    }

    /**
     * Check if the active L2Skill can be casted.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Check if the L2Character can cast
     * (ex : not sleeping...) </li> <li>Check if the target is correct </li>
     * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR><BR>
     *
     * @param skill The L2Skill to use
     *
     */
    protected void useMagic(L2Skill skill) {
        if (skill == null || isDead()) {
            return;
        }

        // Notify the AI with AI_INTENTION_CAST and target
        getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, findSkillTarget(skill));
    }

    private L2Object findSkillTarget(L2Skill skill) {
        if (skill.canCastSelf()) {
            return this;
        }
        return skill.getFirstOfTargetList(this);
    }

    // =========================================================
    // Property - Public
    /**
     * Return the L2CharacterAI of the L2Character and if its null create a new
     * one.
     */
    public L2CharacterAI getAI() {
        //synchronized(this)
        //{
        if (_ai == null) {
            _ai = new L2CharacterAI(new AIAccessor());
        }
        //}

        return _ai;
    }

    public void setAI(L2CharacterAI newAI) {
        L2CharacterAI oldAI = getAI();
        if (oldAI != null && oldAI != newAI && oldAI instanceof L2AttackableAI) {
            ((L2AttackableAI) oldAI).stopAITask();
        }
        _ai = newAI;
    }

    /**
     * Return True if the L2Character has a L2CharacterAI.
     */
    public boolean hasAI() {
        return _ai != null;
    }

    /**
     * Return True if the L2Character is RaidBoss or his minion.
     */
    public boolean isRaid() {
        return false;
    }

    public boolean isGrandRaid() {
        return false;
    }

    public boolean checkRange() {
        if (!Config.NPC_CHECK_RANGE || isMinion()) {
            return false;
        }

        return (isMonster() || isRaid());
    }

    public int getChaseRange() {
        return Config.NPC_CHECK_MAX_RANGE;
    }

    /**
     * Дебаффы на флаги, гвардов...
     */
    public boolean isDebuffProtected() {
        return false;
    }

    /**
     * Return a list of L2Character that attacked.
     */
    public final List<L2Character> getAttackByList() {
        if (_attackByList == null) {
            _attackByList = new FastList<L2Character>();
        }
        return _attackByList;
    }

    public final L2Skill getLastSkillCast() {
        return _lastSkillCast;
    }

    public void setLastSkillCast(L2Skill skill) {
        _lastSkillCast = skill;
    }

    public final boolean isAfraid() {
        return _isAfraid;
    }

    public final void setIsAfraid(boolean value) {
        _isAfraid = value;
    }

    /**
     * Return True if the L2Character is dead or use fake death.
     */
    @Override
    public boolean isAlikeDead() {
        return isFakeDeath() || (getCurrentHp() < 0.5);
    }

    /**
     * Return True if the L2Character can't use its skills (ex : stun,
     * sleep...).
     */
    public final boolean isAllSkillsDisabled() {
        return _allSkillsDisabled || isImmobileUntilAttacked() || isStunned() || isSleeping() || isParalyzed();
    }

    /**
     * Return True if the L2Character can't attack (stun, sleep, attackEndTime,
     * fakeDeath, paralyse).
     */
    public boolean isAttackingDisabled() {
        return _attackReuseEndTime > System.currentTimeMillis() || isImmobileUntilAttacked() || isStunned() || isSleeping() || isFakeDeath() || isParalyzed();
    }

    public final Calculator[] getCalculators() {
        return _calculators;
    }

    public final boolean isConfused() {
        return _isConfused;
    }

    public final void setIsConfused(boolean value) {
        _isConfused = value;
    }

    /**
     * Return True if the L2Character is dead.
     */
    //public final boolean isDead() { return !(isFakeDeath()) && !(getCurrentHp() > 0.5); }
    @Override
    public final boolean isDead() {
        return (getCurrentHp() < 0.5);
    }

    public final boolean isFakeDeath() {
        return _isFakeDeath;
    }

    public final void setIsFakeDeath(boolean value) {
        _isFakeDeath = value;
    }

    /**
     * Return True if the L2Character is flying.
     */
    public final boolean isFlying() {
        return _isFlying;
    }

    /**
     * Set the L2Character flying mode to True.
     */
    public final void setIsFlying(boolean mode) {
        _isFlying = mode;
    }

    public boolean isImobilised() {
        /*
         * if (this.isPlayer()) { if (isUltimate()) return true; }
         */
        return _isImobilised;
    }

    public void setIsImobilised(boolean value) {
        _isImobilised = value;
    }

    public final boolean isKilledAlready() {
        return _isKilledAlready;
    }

    public final void setIsKilledAlready(boolean value) {
        _isKilledAlready = value;
    }

    public final boolean isMuted() {
        return _isMuted;
    }

    public final void setIsMuted(boolean value) {
        _isMuted = value;
    }

    public final boolean isPsychicalMuted() {
        return _isPsychicalMuted;
    }

    public final void setIsPsychicalMuted(boolean value) {
        _isPsychicalMuted = value;
    }

    /**
     * Return True if the L2Character can't move (stun, root, sleep, overload,
     * paralyzed).
     */
    //public boolean isMovementDisabled() { return isStunned() || isRooted() || isSleeping() || isOverloaded() || isParalyzed() || isImobilised() || isFakeDeath(); }
    public boolean isMovementDisabled() {
        return isTeleporting() || isSitting() || isStunned() || isRooted() || isSleeping() || isParalyzed() || isImobilised() || isAlikeDead() || isAttackingNow() || isCastingNow() || isOverloaded() || isFishing() || getRunSpeed() == 0;
    }

    public boolean isFishing() {
        return false;
    }

    /**
     * Return True if the L2Character can be controlled by the player (confused,
     * afraid).
     */
    public final boolean isOutOfControl() {
        return isConfused() || isAfraid();
    }

    public final boolean isOverloaded() {
        return _isOverloaded;
    }

    /**
     * Set the overloaded status of the L2Character is overloaded (if True, the
     * L2PcInstance can't take more item).
     */
    public final void setIsOverloaded(boolean value) {
        _isOverloaded = value;
    }

    public boolean isParalyzed() {
        return _isParalyzed;
    }

    public final void setIsParalyzed(boolean value) {
        _isParalyzed = value;
    }

    public final boolean isPendingRevive() {
        return isDead() && _isPendingRevive;
    }

    public final void setIsPendingRevive(boolean value) {
        _isPendingRevive = value;
    }

    /**
     * Return the L2Summon of the L2Character.<BR><BR> <B><U> Overriden in </U>
     * :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     */
    public L2Summon getPet() {
        return null;
    }

    /**
     * Return True if the L2Character is ridding.
     */
    public final boolean isRiding() {
        return _isRiding;
    }

    /**
     * Set the L2Character riding mode to True.
     */
    public final void setIsRiding(boolean mode) {
        _isRiding = mode;
    }

    public final boolean isRooted() {
        return _isRooted;
    }

    public final void setIsRooted(boolean value) {
        _isRooted = value;
    }

    /**
     * Return True if the L2Character is running.
     */
    public final boolean isRunning() {
        return _isRunning;
    }

    public final void setIsRunning(boolean value) {
        if (_isRunning == value) {
            return;
        }
        _isRunning = value;
        if (getRunSpeed() != 0) {
            broadcastPacket(new ChangeMoveType(this));
        }
        if (this.isPlayer()) {
            broadcastUserInfo();
        } else if (this.isL2Summon()) {
            broadcastStatusUpdate();
        } else if (this.isL2Npc()) {
            FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
            L2PcInstance pc = null;
            for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                pc = n.getValue();
                if (pc == null) {
                    continue;
                }

                if (getRunSpeed() == 0) {
                    pc.sendPacket(new ServerObjectInfo((L2NpcInstance) this, pc));
                } else {
                    pc.sendPacket(new NpcInfo((L2NpcInstance) this, pc));
                }
            }
            players.clear();
            players = null;
            pc = null;
        }
    }

    /**
     * Set the L2Character movement type to run and send Server->Client packet
     * ChangeMoveType to all others L2PcInstance.
     */
    public final void setRunning() {
        if (!isRunning()) {
            setIsRunning(true);
        }
    }

    public final boolean isImmobileUntilAttacked() {
        return _isImmobileUntilAttacked;
    }

    public final void setIsImmobileUntilAttacked(boolean value) {
        _isImmobileUntilAttacked = value;
    }

    public final boolean isSleeping() {
        return _isSleeping;
    }

    public final void setIsSleeping(boolean value) {
        _isSleeping = value;
    }

    public final boolean isStunned() {
        return _isStunned;
    }

    public final void setIsStunned(boolean value) {
        _isStunned = value;
    }

    public final boolean isBetrayed() {
        return _isBetrayed;
    }

    public final void setIsBetrayed(boolean value) {
        _isBetrayed = value;
    }

    public final boolean isTeleporting() {
        return _isTeleporting;
    }

    public final void setIsTeleporting(boolean value) {
        _isTeleporting = value;
    }

    public void setIsInvul(boolean b) {
        _isInvul = b;
    }

    public boolean isInvul() {
        return _isInvul || _isTeleporting;
    }

    public boolean isUndead() {
        return _template.isUndead;
    }

    @Override
    public CharKnownList getKnownList() {
        if (super.getKnownList() == null || !(super.getKnownList() instanceof CharKnownList)) {
            setKnownList(new CharKnownList(this));
        }
        return ((CharKnownList) super.getKnownList());
    }

    public CharStat getStat() {
        if (_stat == null) {
            _stat = new CharStat(this);
        }
        return _stat;
    }

    public final void setStat(CharStat value) {
        _stat = value;
    }

    public CharStatus getStatus() {
        if (_status == null) {
            _status = new CharStatus(this);
        }
        return _status;
    }

    public final void setStatus(CharStatus value) {
        _status = value;
    }

    public L2CharTemplate getTemplate() {
        return _template;
    }

    /**
     * Set the template of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> Each L2Character owns generic and static
     * properties (ex : all Keltir have the same number of HP...). All of those
     * properties are stored in a different template for each type of
     * L2Character. Each template is loaded once in the server cache memory
     * (reduce memory use). When a new instance of L2Character is spawned,
     * server just create a link between the instance and the template This link
     * is stored in <B>_template</B><BR><BR>
     *
     * <B><U> Assert </U> :</B><BR><BR> <li> this instanceof
     * L2Character</li><BR><BR
     */
    protected final void setTemplate(L2CharTemplate template) {
        _template = template;
    }

    /**
     * Return the Title of the L2Character.
     */
    public String getTitle() {
        return _title;
    }

    /**
     * Set the Title of the L2Character.
     */
    public void setTitle(String value) {
        _title = value;
    }

    /**
     * Set the L2Character movement type to walk and send Server->Client packet
     * ChangeMoveType to all others L2PcInstance.
     */
    public final void setWalking() {
        if (isRunning()) {
            setIsRunning(false);
        }
    }

    /**
     * Task lauching the function onHitTimer().<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>If the attacker/target is dead or
     * use fake death, notify the AI with EVT_CANCEL and send a Server->Client
     * packet ActionFailed (if attacker is a L2PcInstance)</li> <li>If attack
     * isn't aborted, send a message system (critical hit, missed...) to
     * attacker/target if they are L2PcInstance </li> <li>If attack isn't
     * aborted and hit isn't missed, reduce HP of the target and calculate
     * reflection damage to reduce HP of attacker if necessary </li> <li>if
     * attack isn't aborted and hit isn't missed, manage attack or cast break of
     * the target (calculating rate, sending message...) </li><BR><BR>
     *
     */
    class HitTask implements Runnable {

        L2Character _hitTarget;
        int _damage;
        boolean _crit;
        boolean _miss;
        boolean _shld;
        boolean _soulshot;

        public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld) {
            _hitTarget = target;
            _damage = damage;
            _crit = crit;
            _shld = shld;
            _miss = miss;
            _soulshot = soulshot;
        }

        public void run() {
            try {
                onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld);
                getAI().notifyEvent(CtrlEvent.EVT_READY_TO_ACT);
            } catch (Throwable e) {
                _log.severe(e.toString());
            }
        }
    }

    /**
     * Task lauching the magic skill phases
     */
    class MagicUseTask implements Runnable {

        FastList<L2Object> _targets = new FastList<L2Object>();
        L2Skill _skill;
        int _hitTime;
        int _coolTime;
        int _phase;

        public MagicUseTask(FastList<L2Object> targets, L2Skill skill, int coolTime, int phase, int hitTime) {
            _hitTime = hitTime;
            _targets.addAll(targets);
            _skill = skill;
            _coolTime = coolTime;
            _phase = phase;
        }

        public void run() {
            try {
                switch (_phase) {
                    case 1:
                        onMagicLaunchedTimer(_targets, _skill, _coolTime, false);
                        break;
                    case 2:
                        onMagicHitTimer(_targets, _skill, _coolTime, false, _hitTime);
                        ;
                        break;
                    case 3:
                        onMagicFinalizer(_skill, _targets.getFirst());
                        break;
                }
            } catch (Throwable e) {
                _log.log(Level.SEVERE, "", e);
                enableAllSkills();
            }
        }
    }

    /**
     * Task lauching the function useMagic()
     */
    static class QueuedMagicUseTask implements Runnable {

        L2PcInstance _currPlayer;
        L2Skill _queuedSkill;
        boolean _isCtrlPressed;
        boolean _isShiftPressed;

        public QueuedMagicUseTask(L2PcInstance currPlayer, L2Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed) {
            _currPlayer = currPlayer;
            _queuedSkill = queuedSkill;
            _isCtrlPressed = isCtrlPressed;
            _isShiftPressed = isShiftPressed;
        }

        public void run() {
            /*
             * try {
             */
            _currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
            /*
             * }
             * catch (Throwable e) { _log.log(Level.SEVERE, "", e); }
             */
        }
    }

    /**
     * Task of AI notification
     */
    public class NotifyAITask implements Runnable {

        private final CtrlEvent _evt;

        public NotifyAITask(CtrlEvent evt) {
            _evt = evt;
        }

        public void run() {
            try {
                getAI().notifyEvent(_evt, null);
            } catch (Throwable t) {
                _log.log(Level.WARNING, "", t);
            }
        }
    }

    /**
     * Task lauching the function stopPvPFlag()
     */
    class PvPFlag implements Runnable {

        public PvPFlag() {
        }

        public void run() {
            try {
                if (System.currentTimeMillis() > getPvpFlagLasts()) {
                    stopPvPFlag();
                } else if (System.currentTimeMillis() > (getPvpFlagLasts() - 20000)) {
                    updatePvPFlag(2);
                } else {
                    updatePvPFlag(1);
                    // Start a new PvP timer check
                    //checkPvPFlag();
                }
            } catch (Exception e) {
                _log.log(Level.WARNING, "error in pvp flag task:", e);
            }
        }
    }
    // =========================================================
    // =========================================================
    // Abnormal Effect - NEED TO REMOVE ONCE L2CHARABNORMALEFFECT IS COMPLETE
    // Data Field
    /**
     * Map 32 bits (0x0000) containing all abnormal effect in progress
     */
    private int _AbnormalEffects;
    /**
     * FastTable containing all active skills effects in progress of a
     * L2Character.
     */
    //private L2EffectList _effectList = new L2EffectList(this);
    public static final int ABNORMAL_EFFECT_BLEEDING = 0x000001;
    public static final int ABNORMAL_EFFECT_POISON = 0x000002;
    public static final int ABNORMAL_EFFECT_UNKNOWN_3 = 0x000004;
    public static final int ABNORMAL_EFFECT_UNKNOWN_4 = 0x000008;
    public static final int ABNORMAL_EFFECT_UNKNOWN_5 = 0x000010;
    public static final int ABNORMAL_EFFECT_UNKNOWN_6 = 0x000020; // mozno dlja bolota
    public static final int ABNORMAL_EFFECT_STUN = 0x000040;
    public static final int ABNORMAL_EFFECT_SLEEP = 0x000080;
    public static final int ABNORMAL_EFFECT_MUTED = 0x000100;
    public static final int ABNORMAL_EFFECT_ROOT = 0x000200;
    public static final int ABNORMAL_EFFECT_HOLD_1 = 0x000400;
    public static final int ABNORMAL_EFFECT_HOLD_2 = 0x000800;
    public static final int ABNORMAL_EFFECT_UNKNOWN_13 = 0x001000;
    public static final int ABNORMAL_EFFECT_BIG_HEAD = 0x002000;
    public static final int ABNORMAL_EFFECT_FLAME = 0x004000;
    public static final int ABNORMAL_EFFECT_UNKNOWN_16 = 0x008000;
    public static final int ABNORMAL_EFFECT_GROW = 0x010000;
    public static final int ABNORMAL_EFFECT_FLOATING_ROOT = 0x020000; // nevesomost' o_O
    public static final int ABNORMAL_EFFECT_DANCE_STUNNED = 0x040000;
    public static final int ABNORMAL_EFFECT_FIREROOT_STUN = 0x080000; // prikol'niy stun
    public static final int ABNORMAL_EFFECT_STEALTH = 0x100000;
    public static final int ABNORMAL_EFFECT_IMPRISIONING_1 = 0x200000; // mozet eto lovuwka tiranozavra?
    public static final int ABNORMAL_EFFECT_IMPRISIONING_2 = 0x400000; // mozet eto lovuwka tiranozavra?
    public static final int ABNORMAL_EFFECT_MAGIC_CIRCLE = 0x800000; // Clan Gate
    // XXX TEMP HACKS (get the proper mask for these effects)
    public static final int ABNORMAL_EFFECT_CONFUSED = 0x0020;
    public static final int ABNORMAL_EFFECT_AFRAID = 0x0010;
    private final FastList<L2Effect> _effects = new FastList<L2Effect>();
    protected Map<String, List<L2Effect>> _stackedEffects = new FastMap<String, List<L2Effect>>();
    private static final L2Effect[] EMPTY_EFFECTS = new L2Effect[0];
    private static final FastTable<L2Effect> EMPTY_EFFECTS_TABLE = new FastTable<L2Effect>();

    // Method - Public
    /**
     * Launch and add L2Effect (including Stack Group management) to L2Character
     * and update client magic icone.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All active skills effects in progress on
     * the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
     * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier
     * that has created the L2Effect.<BR><BR>
     *
     * Several same effect can't be used on a L2Character at the same time.
     * Indeed, effects are not stackable and the last cast will replace the
     * previous in progress. More, some effects belong to the same Stack Group
     * (ex WindWald and Haste Potion). If 2 effects of a same group are used at
     * the same time on a L2Character, only the more efficient (identified by
     * its priority order) will be preserve.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Add the L2Effect to the L2Character
     * _effects</li> <li>If this effect doesn't belong to a Stack Group, add its
     * Funcs to the Calculator set of the L2Character (remove the old one if
     * necessary)</li> <li>If this effect has higher priority in its Stack
     * Group, add its Funcs to the Calculator set of the L2Character (remove
     * previous stacked effect Funcs if necessary)</li> <li>If this effect has
     * NOT higher priority in its Stack Group, set the effect to Not In Use</li>
     * <li>Update active skills in progress icones on player client</li><BR>
     *
     */
    public boolean updateSkillEffects(int skillId, int level) {
        // Get all skills effects on the L2Character
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return true;
        }

        L2Effect e = null;
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().getId() == skillId) {
                if (e.getSkill().getLevel() > level) {
                    return false;
                }
                e.exit();
                return true;
            }
        }
        return true;
    }

    private boolean checkForStacks(String stackType) {
        if (stackType.equals("none")) {
            return true;
        }

        for (int i = 0; i < _effects.size(); i++) {
            if (_effects.get(i).getStackType() == null || _effects.get(i).hasEmptyStack()) {
                continue;
            }

            if (_effects.get(i).hasMoreStacks() && _effects.get(i).containsStack(stackType)) {
                return false;
            }
        }
        return true;
    }

    private void updateMoreStacks(L2Effect newEffect) {
        if (newEffect.hasMoreStacks()) {
            FastTable<L2Effect> effects = getAllEffectsTable();
            //System.out.println("###2#");
            for (int i = 0; i < effects.size(); i++) {
                if (effects.get(i).isSuperStack() && newEffect.isSuperStack()) {
                    removeEffect(effects.get(i));
                    break;
                }
                //System.out.println("###3#" + effects.get(i).getStackType() + "###");
                if (newEffect.containsStack(effects.get(i).getStackType())) {
                    //System.out.println("###3.1#" + effects.get(i).getStackType() + "###");
                    removeEffect(effects.get(i));
                }
            }
        }
    }
    private boolean _cccsss = false;
    private final Lock lock = new ReentrantLock();

    public final void addEffect(L2Effect newEffect) {
        if (newEffect == null) {
            return;
        }

        lock.lock();

        try {
            L2Effect tempEffect = null;

            if (_cccsss && !checkForStacks(newEffect.getStackType())) {
                return;
            }

            if (!updateSkillEffects(newEffect.getId(), newEffect.getLevel())) {
                return;
            }
            // Remove first Buff if number of buffs > 19
            L2Skill tempskill = newEffect.getSkill();
            if (tempskill.checkFirstBuff() && !doesStack(tempskill) && replaceFirstBuff()) {
                removeFirstBuff(tempskill.getId());
            }

            if (tempskill.isAugment() && isMounted()) {
                return;
            }

            //System.out.println("###1#" + newEffect.getStackType() + "#" + newEffect.getStacks().size() + "#" + _effects.size() + "#");
            if (_cccsss) {
                updateMoreStacks(newEffect);
            }
            //System.out.println("###99#");
            // Add the L2Effect to all effect in progress on the L2Character
            if (newEffect.getSkill().isToggle() || newEffect.getSkill().isDebuff()) {
                //stopMove(); http://www.youtube.com/watch?v=7ld6E0iZiBs
                _effects.addLast(newEffect);
            } else {
                int pos = 0;
                for (int i = 0; i < _effects.size(); i++) {
                    if (_effects.get(i) != null) {
                        if (_effects.get(i).getSkill().checkPosSkip()) {
                            pos++;
                        }
                    } else {
                        break;
                    }
                }
                _effects.add(pos, newEffect);
            }

            // Check if a stack group is defined for this effect
            if (newEffect.hasEmptyStack()) {
                // Set this L2Effect to In Use
                newEffect.setInUse(true);

                // Add Funcs of this effect to the Calculator set of the L2Character
                addStatFuncs(newEffect.getStatFuncs());

                // Update active skills in progress icones on player client
                updateEffectIcons();
                return;
            }

            // Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
            List<L2Effect> stackQueue = _stackedEffects.get(newEffect.getStackType());

            if (stackQueue == null) {
                stackQueue = new FastList<L2Effect>();
            }

            if (stackQueue.size() > 0) {
                // Get the first stacked effect of the Stack group selected
                tempEffect = null;
                for (int i = 0; i < _effects.size(); i++) {
                    if (_effects.get(i) == stackQueue.get(0)) {
                        tempEffect = _effects.get(i);
                        break;
                    }
                }

                if (tempEffect != null) {
                    // Remove all Func objects corresponding to this stacked effect from the Calculator set of the L2Character
                    removeStatsOwner(tempEffect);
                    // Set the L2Effect to Not In Use
                    tempEffect.setInUse(false);
                }
            }

            // Add the new effect to the stack group selected at its position
            stackQueue = effectQueueInsert(newEffect, stackQueue);

            if (stackQueue == null) {
                return;
            }

            // Update the Stack Group table _stackedEffects of the L2Character
            _stackedEffects.put(newEffect.getStackType(), stackQueue);

            // Get the first stacked effect of the Stack group selected
            tempEffect = null;
            for (int i = 0; i < _effects.size(); i++) {
                if (_effects.get(i) == stackQueue.get(0)) {
                    tempEffect = _effects.get(i);
                    break;
                }
            }

            if (tempEffect != null) {
                tempEffect.setInUse(true); // Set this L2Effect to In Use
                addStatFuncs(tempEffect.getStatFuncs()); // Add all Func objects corresponding to this stacked effect to the Calculator set of the L2Character
            }
        } finally {
            lock.unlock();
        }
        // Update active skills in progress (In Use and Not In Use because stacked) icones on client
        updateEffectIcons();
    }

    public final void removeEffect(L2Effect effect) {
        //_effectList.removeEffect(effect);
        if (effect == null || _effects.isEmpty()) {
            return;
        }

        lock.lock();
        try {
            if ("none".equals(effect.getStackType())) {
                // Remove Func added by this effect from the L2Character Calculator
                removeStatsOwner(effect);
            } else {
                if (_stackedEffects.isEmpty()) {
                    return;
                }

                // Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
                List<L2Effect> stackQueue = _stackedEffects.get(effect.getStackType());

                if (stackQueue == null || stackQueue.size() < 1) {
                    return;
                }

                // Get the Identifier of the first stacked effect of the Stack group selected
                L2Effect frontEffect = stackQueue.get(0);

                // Remove the effect from the Stack Group
                boolean removed = stackQueue.remove(effect);

                if (removed) {
                    // Check if the first stacked effect was the effect to remove
                    if (frontEffect == effect) {
                        // Remove all its Func objects from the L2Character calculator set
                        removeStatsOwner(effect);

                        // Check if there's another effect in the Stack Group
                        if (stackQueue.size() > 0) {
                            // Add its list of Funcs to the Calculator set of the L2Character
                            for (int i = 0; i < _effects.size(); i++) {
                                if (_effects.get(i) == stackQueue.get(0)) {
                                    // Add its list of Funcs to the Calculator set of the L2Character
                                    addStatFuncs(_effects.get(i).getStatFuncs());
                                    // Set the effect to In Use
                                    _effects.get(i).setInUse(true);
                                    break;
                                }
                            }
                        }
                    }
                    if (stackQueue.isEmpty()) {
                        _stackedEffects.remove(effect.getStackType());
                    } else // Update the Stack Group table _stackedEffects of the L2Character
                    {
                        _stackedEffects.put(effect.getStackType(), stackQueue);
                    }
                }
            }

            // Remove the active skill L2effect from _effects of the L2Character
            // The Integer key of _effects is the L2Skill Identifier that has created the effect
            for (int i = 0; i < _effects.size(); i++) {
                if (_effects.get(i) == effect) {
                    _effects.remove(i);
                    break;
                }
            }

        } finally {
            lock.unlock();
        }
        // Update active skills in progress (In Use and Not In Use because stacked) icones on client
        updateEffectIcons();
    }

    /*public final void addEffect(L2Effect newEffect) {
     if (newEffect == null) {
     return;
     }
    
     synchronized (_effects) {
     L2Effect tempEffect = null;
    
     if (_cccsss && !checkForStacks(newEffect.getStackType())) {
     return;
     }
    
     if (!updateSkillEffects(newEffect.getId(), newEffect.getLevel())) {
     return;
     }
     // Remove first Buff if number of buffs > 19
     L2Skill tempskill = newEffect.getSkill();
     if (tempskill.checkFirstBuff() && !doesStack(tempskill) && replaceFirstBuff()) {
     removeFirstBuff(tempskill.getId());
     }
    
     if (tempskill.isAugment() && isMounted()) {
     return;
     }
    
     //System.out.println("###1#" + newEffect.getStackType() + "#" + newEffect.getStacks().size() + "#" + _effects.size() + "#");
     if (_cccsss) {
     updateMoreStacks(newEffect);
     }
     //System.out.println("###99#");
     // Add the L2Effect to all effect in progress on the L2Character
     if (newEffect.getSkill().isToggle() || newEffect.getSkill().isDebuff()) {
     //stopMove(); http://www.youtube.com/watch?v=7ld6E0iZiBs
     _effects.addLast(newEffect);
     } else {
     int pos = 0;
     for (int i = 0; i < _effects.size(); i++) {
     if (_effects.get(i) != null) {
     if (_effects.get(i).getSkill().checkPosSkip()) {
     pos++;
     }
     } else {
     break;
     }
     }
     _effects.add(pos, newEffect);
     }
    
     // Check if a stack group is defined for this effect
     if (newEffect.hasEmptyStack()) {
     // Set this L2Effect to In Use
     newEffect.setInUse(true);
    
     // Add Funcs of this effect to the Calculator set of the L2Character
     addStatFuncs(newEffect.getStatFuncs());
    
     // Update active skills in progress icones on player client
     updateEffectIcons();
     return;
     }
    
     // Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
     List<L2Effect> stackQueue = _stackedEffects.get(newEffect.getStackType());
    
     if (stackQueue == null) {
     stackQueue = new FastList<L2Effect>();
     }
    
     if (stackQueue.size() > 0) {
     // Get the first stacked effect of the Stack group selected
     tempEffect = null;
     for (int i = 0; i < _effects.size(); i++) {
     if (_effects.get(i) == stackQueue.get(0)) {
     tempEffect = _effects.get(i);
     break;
     }
     }
    
     if (tempEffect != null) {
     // Remove all Func objects corresponding to this stacked effect from the Calculator set of the L2Character
     removeStatsOwner(tempEffect);
     // Set the L2Effect to Not In Use
     tempEffect.setInUse(false);
     }
     }
    
     // Add the new effect to the stack group selected at its position
     stackQueue = effectQueueInsert(newEffect, stackQueue);
    
     if (stackQueue == null) {
     return;
     }
    
     // Update the Stack Group table _stackedEffects of the L2Character
     _stackedEffects.put(newEffect.getStackType(), stackQueue);
    
     // Get the first stacked effect of the Stack group selected
     tempEffect = null;
     for (int i = 0; i < _effects.size(); i++) {
     if (_effects.get(i) == stackQueue.get(0)) {
     tempEffect = _effects.get(i);
     break;
     }
     }
    
     if (tempEffect != null) {
     tempEffect.setInUse(true); // Set this L2Effect to In Use
     addStatFuncs(tempEffect.getStatFuncs()); // Add all Func objects corresponding to this stacked effect to the Calculator set of the L2Character
     }
     }
     // Update active skills in progress (In Use and Not In Use because stacked) icones on client
     updateEffectIcons();
     }
    
     public final void removeEffect(L2Effect effect) {
     //_effectList.removeEffect(effect);
     if (effect == null || _effects.isEmpty()) {
     return;
     }
    
     synchronized (_effects) {
    
     if ("none".equals(effect.getStackType())) {
     // Remove Func added by this effect from the L2Character Calculator
     removeStatsOwner(effect);
     } else {
     if (_stackedEffects.isEmpty()) {
     return;
     }
    
     // Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
     List<L2Effect> stackQueue = _stackedEffects.get(effect.getStackType());
    
     if (stackQueue == null || stackQueue.size() < 1) {
     return;
     }
    
     // Get the Identifier of the first stacked effect of the Stack group selected
     L2Effect frontEffect = stackQueue.get(0);
    
     // Remove the effect from the Stack Group
     boolean removed = stackQueue.remove(effect);
    
     if (removed) {
     // Check if the first stacked effect was the effect to remove
     if (frontEffect == effect) {
     // Remove all its Func objects from the L2Character calculator set
     removeStatsOwner(effect);
    
     // Check if there's another effect in the Stack Group
     if (stackQueue.size() > 0) {
     // Add its list of Funcs to the Calculator set of the L2Character
     for (int i = 0; i < _effects.size(); i++) {
     if (_effects.get(i) == stackQueue.get(0)) {
     // Add its list of Funcs to the Calculator set of the L2Character
     addStatFuncs(_effects.get(i).getStatFuncs());
     // Set the effect to In Use
     _effects.get(i).setInUse(true);
     break;
     }
     }
     }
     }
     if (stackQueue.isEmpty()) {
     _stackedEffects.remove(effect.getStackType());
     } else // Update the Stack Group table _stackedEffects of the L2Character
     {
     _stackedEffects.put(effect.getStackType(), stackQueue);
     }
     }
     }
    
    
     // Remove the active skill L2effect from _effects of the L2Character
     // The Integer key of _effects is the L2Skill Identifier that has created the effect
     for (int i = 0; i < _effects.size(); i++) {
     if (_effects.get(i) == effect) {
     _effects.remove(i);
     break;
     }
     }
    
     }
     // Update active skills in progress (In Use and Not In Use because stacked) icones on client
     updateEffectIcons();
     }*/
    public boolean replaceFirstBuff() {
        return (getBuffCount() >= Config.BUFFS_MAX_AMOUNT);
    }

    /**
     * Active abnormal effects flags in the binary mask and send Server->Client
     * UserInfo/CharInfo packet.<BR><BR>
     */
    public final void startAbnormalEffect(int mask) {
        if (isRaid()) {
            return;
        }

        _AbnormalEffects |= mask;
        updateAbnormalEffect();
    }

    /**
     * immobile start
     */
    public final void startImmobileUntilAttacked() {
        setIsImmobileUntilAttacked(true);
        abortAttack();
        abortCast();
        getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
        updateAbnormalEffect();
    }

    /**
     * Active the abnormal effect Confused flag, notify the L2Character AI and
     * send Server->Client UserInfo/CharInfo packet.<BR><BR>
     */
    public final void startConfused() {
        if (isRaid()) {
            return;
        }

        setIsConfused(true);
        getAI().notifyEvent(CtrlEvent.EVT_CONFUSED);
        updateAbnormalEffect();
    }

    /**
     * Active the abnormal effect Fake Death flag, notify the L2Character AI and
     * send Server->Client UserInfo/CharInfo packet.<BR><BR>
     */
    public final void startFakeDeath() {
        setIsFakeDeath(true);
        /*
         * Aborts any attacks/casts if fake dead
         */
        abortAttack();
        abortCast();
        getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH, null);
        broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
    }

    /**
     * Active the abnormal effect Fear flag, notify the L2Character AI and send
     * Server->Client UserInfo/CharInfo packet.<BR><BR>
     */
    public final void startFear() {
        if (isRaid()) {
            return;
        }

        setIsAfraid(true);
        getAI().notifyEvent(CtrlEvent.EVT_AFFRAID);
        updateAbnormalEffect();
    }

    /**
     * Active the abnormal effect Muted flag, notify the L2Character AI and send
     * Server->Client UserInfo/CharInfo packet.<BR><BR>
     */
    public final void startMuted() {
        if (isRaid()) {
            return;
        }

        setIsMuted(true);
        /*
         * Aborts any casts if muted
         */
        abortCast();
        getAI().notifyEvent(CtrlEvent.EVT_MUTED);
        updateAbnormalEffect();
    }

    /**
     * Active the abnormal effect Psychical_Muted flag, notify the L2Character
     * AI and send Server->Client UserInfo/CharInfo packet.<BR><BR>
     */
    public final void startPsychicalMuted() {
        if (isRaid()) {
            return;
        }

        setIsPsychicalMuted(true);
        getAI().notifyEvent(CtrlEvent.EVT_MUTED);
        updateAbnormalEffect();
    }

    /**
     * Active the abnormal effect Root flag, notify the L2Character AI and send
     * Server->Client UserInfo/CharInfo packet.<BR><BR>
     */
    public final void startRooted() {
        if (isRaid()) {
            return;
        }

        setIsRooted(true);
        getAI().notifyEvent(CtrlEvent.EVT_ROOTED, null);
        updateAbnormalEffect();
    }

    /**
     * Active the abnormal effect Sleep flag, notify the L2Character AI and send
     * Server->Client UserInfo/CharInfo packet.<BR><BR>
     */
    public final void startSleeping() {
        if (isRaid()) {
            return;
        }

        setIsSleeping(true);
        /*
         * Aborts any attacks/casts if sleeped
         */
        abortAttack();
        abortCast();
        getAI().notifyEvent(CtrlEvent.EVT_SLEEPING, null);
        updateAbnormalEffect();
    }

    /**
     * Launch a Stun Abnormal Effect on the L2Character.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Calculate the success rate of the
     * Stun Abnormal Effect on this L2Character</li> <li>If Stun succeed, active
     * the abnormal effect Stun flag, notify the L2Character AI and send
     * Server->Client UserInfo/CharInfo packet</li> <li>If Stun NOT succeed,
     * send a system message Failed to the L2PcInstance attacker</li><BR><BR>
     *
     */
    public final void startStunning() {
        if (isRaid()) {
            return;
        }

        setIsStunned(true);
        /*
         * Aborts any attacks/casts if stunned
         */
        abortAttack();
        abortCast();
        getAI().notifyEvent(CtrlEvent.EVT_STUNNED, null);
        updateAbnormalEffect();
    }

    public final void startBetray() {
        if (isRaid()) {
            return;
        }

        setIsBetrayed(true);
        getAI().notifyEvent(CtrlEvent.EVT_BETRAYED, null);
        updateAbnormalEffect();
    }

    public final void stopBetray() {
        stopEffects(L2Effect.EffectType.BETRAY);
        setIsBetrayed(false);
        updateAbnormalEffect();
    }

    /**
     * Modify the abnormal effect map according to the mask.<BR><BR>
     */
    public final void stopAbnormalEffect(int mask) {
        _AbnormalEffects &= ~mask;
        updateAbnormalEffect();
    }

    /**
     * Stop all active skills effects in progress on the L2Character.<BR><BR>
     */
    public final void stopAllEffects() {
        // Get all active skills effects in progress on the L2Character
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return;
        }

        // Go through all active skills effects
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect e = effects.get(i);
            if (e == null) {
                continue;
            }

            e.exit(true);
        }
        updateAndBroadcastStatus(2);
    }

    public final void stopAllDebuffs() {
        // Get all active skills effects in progress on the L2Character
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return;
        }

        // Go through all active skills effects
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().isDebuff()) {
                e.exit(true);
            }
        }
        updateAndBroadcastStatus(2);
    }

    /**
     * Stop immobilization until attacked abnormal L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete a specified/all (if
     * effect=null) immobilization until attacked abnormal L2Effect from
     * L2Character and update client magic icon </li> <li>Set the abnormal
     * effect flag _muted to False </li> <li>Notify the L2Character AI</li>
     * <li>Send Server->Client UserInfo/CharInfo packet</li><BR><BR>
     *
     */
    public final void stopImmobileUntilAttacked(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.IMMOBILEUNTILATTACKED);
        } else {
            removeEffect(effect);
            stopSkillEffects(effect.getSkill().getNegateId());
        }

        setIsImmobileUntilAttacked(false);
        getAI().notifyEvent(CtrlEvent.EVT_THINK);
        updateAbnormalEffect();
    }

    /**
     * Stop a specified/all Confused abnormal L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete a specified/all (if
     * effect=null) Confused abnormal L2Effect from L2Character and update
     * client magic icone </li> <li>Set the abnormal effect flag _confused to
     * False </li> <li>Notify the L2Character AI</li> <li>Send Server->Client
     * UserInfo/CharInfo packet</li><BR><BR>
     *
     */
    public final void stopConfused(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.CONFUSION);
        } else {
            removeEffect(effect);
        }

        setIsConfused(false);
        getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
        updateAbnormalEffect();
    }

    public void stopSlowEffects() {
        // Get all skills effects on the L2Character
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return;
        }

        L2Effect e = null;
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }

            switch (e.getSkill().getId()) {
                case 95:
                case 102:
                case 105:
                case 127:
                case 354:
                case 1099:
                case 1160:
                case 1236:
                case 1298:
                    e.exit();
                    break;
            }
        }
    }

    /**
     * Stop and remove the L2Effects corresponding to the L2Skill Identifier and
     * update client magic icone.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All active skills effects in progress on
     * the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
     * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier
     * that has created the L2Effect.<BR><BR>
     *
     * @param effectId The L2Skill Identifier of the L2Effect to remove from
     * _effects
     *
     */
    public final void stopSkillEffects(int skillId) {
        if (skillId == 0) {
            return;
        }
        // Get all skills effects on the L2Character
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return;
        }

        L2Effect e = null;
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().getId() == skillId) {
                e.exit();
            }
        }
    }

    /**
     * Stop and remove all L2Effect of the selected type (ex : BUFF,
     * DMG_OVER_TIME...) from the L2Character and update client magic
     * icone.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All active skills effects in progress on
     * the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
     * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier
     * that has created the L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Remove Func added by this effect
     * from the L2Character Calculator (Stop L2Effect)</li> <li>Remove the
     * L2Effect from _effects of the L2Character</li> <li>Update active skills
     * in progress icones on player client</li><BR><BR>
     *
     * @param type The type of effect to stop ((ex : BUFF, DMG_OVER_TIME...)
     *
     */
    public final void stopEffects(L2Effect.EffectType type) {
        // Get all active skills effects in progress on the L2Character
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return;
        }

        L2Effect e = null;
        // Go through all active skills effects
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }

            // Stop active skills effects of the selected type
            if (e.getEffectType() == type) {
                e.exit();
            }
        }
    }

    /**
     * Stop a specified/all Fake Death abnormal L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete a specified/all (if
     * effect=null) Fake Death abnormal L2Effect from L2Character and update
     * client magic icone </li> <li>Set the abnormal effect flag _fake_death to
     * False </li> <li>Notify the L2Character AI</li><BR><BR>
     *
     */
    public final void stopFakeDeath(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.FAKE_DEATH);
        } else {
            removeEffect(effect);
        }

        setIsFakeDeath(false);
        // if this is a player instance, start the grace period for this character (grace from mobs only)!
        setRecentFakeDeath(true);

        ChangeWaitType revive = new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH);
        broadcastPacket(revive);
        //TODO: Temp hack: players see FD on ppl that are moving: Teleport to someone who uses FD - if he gets up he will fall down again for that client - 
        // even tho he is actually standing... Probably bad info in CharInfo packet? 
        broadcastPacket(new Revive(this));
        getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
    }

    /**
     * Stop a specified/all Fear abnormal L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete a specified/all (if
     * effect=null) Fear abnormal L2Effect from L2Character and update client
     * magic icone </li> <li>Set the abnormal effect flag _affraid to False
     * </li> <li>Notify the L2Character AI</li> <li>Send Server->Client
     * UserInfo/CharInfo packet</li><BR><BR>
     *
     */
    public final void stopFear(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.FEAR);
        } else {
            removeEffect(effect);
        }

        setIsAfraid(false);
        updateAbnormalEffect();
    }

    /**
     * Stop a specified/all Muted abnormal L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete a specified/all (if
     * effect=null) Muted abnormal L2Effect from L2Character and update client
     * magic icone </li> <li>Set the abnormal effect flag _muted to False </li>
     * <li>Notify the L2Character AI</li> <li>Send Server->Client
     * UserInfo/CharInfo packet</li><BR><BR>
     *
     */
    public final void stopMuted(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.MUTE);
        } else {
            removeEffect(effect);
        }

        setIsMuted(false);
        updateAbnormalEffect();
    }

    public final void stopPsychicalMuted(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.PSYCHICAL_MUTE);
        } else {
            removeEffect(effect);
        }

        setIsPsychicalMuted(false);
        updateAbnormalEffect();
    }

    /**
     * Stop a specified/all Root abnormal L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete a specified/all (if
     * effect=null) Root abnormal L2Effect from L2Character and update client
     * magic icone </li> <li>Set the abnormal effect flag _rooted to False </li>
     * <li>Notify the L2Character AI</li> <li>Send Server->Client
     * UserInfo/CharInfo packet</li><BR><BR>
     *
     */
    public final void stopRooting(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.ROOT);
        } else {
            removeEffect(effect);
        }

        setIsRooted(false);
        getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
        updateAbnormalEffect();
    }

    /**
     * Stop a specified/all Sleep abnormal L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete a specified/all (if
     * effect=null) Sleep abnormal L2Effect from L2Character and update client
     * magic icone </li> <li>Set the abnormal effect flag _sleeping to False
     * </li> <li>Notify the L2Character AI</li> <li>Send Server->Client
     * UserInfo/CharInfo packet</li><BR><BR>
     *
     */
    public final void stopSleeping(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.SLEEP);
        } else {
            removeEffect(effect);
        }

        setIsSleeping(false);
        getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
        updateAbnormalEffect();
    }

    /**
     * Stop a specified/all Stun abnormal L2Effect.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete a specified/all (if
     * effect=null) Stun abnormal L2Effect from L2Character and update client
     * magic icone </li> <li>Set the abnormal effect flag _stuned to False </li>
     * <li>Notify the L2Character AI</li> <li>Send Server->Client
     * UserInfo/CharInfo packet</li><BR><BR>
     *
     */
    public final void stopStunning(L2Effect effect) {
        if (effect == null) {
            stopEffects(L2Effect.EffectType.STUN);
        } else {
            removeEffect(effect);
        }

        setIsStunned(false);
        getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
        updateAbnormalEffect();
    }

    /**
     * Not Implemented.<BR><BR>
     *
     * <B><U> Overridden in</U> :</B><BR><BR> <li>L2NPCInstance</li>
     * <li>L2PcInstance</li> <li>L2Summon</li> <li>L2DoorInstance</li><BR><BR>
     *
     */
    public abstract void updateAbnormalEffect();

    /**
     * Update active skills in progress (In Use and Not In Use because stacked)
     * icones on client.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All active skills effects in progress
     * (In Use and Not In Use because stacked) are represented by an icone on
     * the client.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method ONLY UPDATE the
     * client of the player and not clients of all players in the
     * party.</B></FONT><BR><BR>
     *
     */
    public final void updateEffectIcons() {
        updateEffectIcons(false);
    }

    public void updateEffectIcons(boolean partyOnly) {
        // overridden
    }

    // Property - Public
    /**
     * Return a map of 16 bits (0x0000) containing all abnormal effect in
     * progress for this L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> In Server->Client packet, each effect is
     * represented by 1 bit of the map (ex : BLEEDING = 0x0001 (bit 1), SLEEP =
     * 0x0080 (bit 8)...). The map is calculated by applying a BINARY OR
     * operation on each effect.<BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Server Packet : CharInfo,
     * NpcInfo, NpcInfoPoly, UserInfo...</li><BR><BR>
     */
    public int getAbnormalEffect() {
        int ae = _AbnormalEffects;
        if (isStunned()) {
            ae |= ABNORMAL_EFFECT_STUN;
        }
        if (isRooted()) {
            ae |= ABNORMAL_EFFECT_ROOT;
        }
        if (isSleeping()) {
            ae |= ABNORMAL_EFFECT_SLEEP;
        }
        if (isConfused()) {
            ae |= ABNORMAL_EFFECT_CONFUSED;
        }
        if (isMuted()) {
            ae |= ABNORMAL_EFFECT_MUTED;
        }
        if (isAfraid()) {
            ae |= ABNORMAL_EFFECT_AFRAID;
        }
        if (isPsychicalMuted()) {
            ae |= ABNORMAL_EFFECT_MUTED;
        }
        return ae;
    }

    /**
     * Return all active skills effects in progress on the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All active skills effects in progress on
     * the L2Character are identified in <B>_effects</B>. The Integer key of
     * _effects is the L2Skill Identifier that has created the effect.<BR><BR>
     *
     * @return A table containing all active skills effect in progress on the
     * L2Character
     *
     */
    public final L2Effect[] getAllEffects() {
        //return _effectList.getAllEffects();
        // Create a copy of the effects set
        FastList<L2Effect> effects = _effects;

        // If no effect found, return EMPTY_EFFECTS
        if (effects == null || effects.isEmpty()) {
            return EMPTY_EFFECTS;
        }

        // Return all effects in progress in a table
        int ArraySize = effects.size();
        L2Effect[] effectArray = new L2Effect[ArraySize];
        for (int i = 0; i < ArraySize; i++) {
            if (i >= effects.size() || effects.get(i) == null) {
                break;
            }
            effectArray[i] = effects.get(i);
        }
        return effectArray;
    }

    public final FastTable<L2Effect> getAllEffectsTable() {
        //return _effectList.getAllEffectsTable();
        if (_effects.isEmpty()) {
            return EMPTY_EFFECTS_TABLE;
        }

        FastTable<L2Effect> effects = new FastTable<L2Effect>();
        effects.addAll(_effects);

        return effects;
    }

    /**
     * Return L2Effect in progress on the L2Character corresponding to the
     * L2Skill Identifier.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All active skills effects in progress on
     * the L2Character are identified in <B>_effects</B>.
     *
     * @param index The L2Skill Identifier of the L2Effect to return from the
     * _effects
     *
     * @return The L2Effect corresponding to the L2Skill Identifier
     *
     */
    public final L2Effect getFirstEffect(int index) {
        //FastTable<L2Effect> effects = _effectList.getAllEffectsTable();
        FastList<L2Effect> effects = _effects;
        if (effects.isEmpty()) {
            return null;
        }

        L2Effect e = null;
        L2Effect eventNotInUse = null;
        for (int i = 0; i < effects.size(); i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }
            if (e.getSkill().getId() == index) {
                if (e.getInUse()) {
                    return e;
                } else {
                    eventNotInUse = e;
                }
            }
        }
        return eventNotInUse;
    }

    /**
     * Return the first L2Effect in progress on the L2Character created by the
     * L2Skill.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All active skills effects in progress on
     * the L2Character are identified in <B>_effects</B>.
     *
     * @param skill The L2Skill whose effect must be returned
     *
     * @return The first L2Effect created by the L2Skill
     *
     */
    public final L2Effect getFirstEffect(L2Skill skill) {
        //FastTable<L2Effect> effects = _effectList.getAllEffectsTable();
        FastList<L2Effect> effects = _effects;
        if (effects.isEmpty()) {
            return null;
        }

        L2Effect e = null;
        L2Effect eventNotInUse = null;
        for (int i = 0; i < effects.size(); i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }
            if (e.getSkill() == skill) {
                if (e.getInUse()) {
                    return e;
                } else {
                    eventNotInUse = e;
                }
            }
        }
        return eventNotInUse;
    }

    /**
     * Return the first L2Effect in progress on the L2Character corresponding to
     * the Effect Type (ex : BUFF, STUN, ROOT...).<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All active skills effects in progress on
     * the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
     * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier
     * that has created the L2Effect.<BR><BR>
     *
     * @param tp The Effect Type of skills whose effect must be returned
     *
     * @return The first L2Effect corresponding to the Effect Type
     *
     */
    public final L2Effect getFirstEffect(L2Effect.EffectType tp) {
        //FastTable<L2Effect> effects = _effectList.getAllEffectsTable();
        FastList<L2Effect> effects = _effects;
        if (effects.isEmpty()) {
            return null;
        }

        L2Effect e = null;
        L2Effect eventNotInUse = null;
        for (int i = 0; i < effects.size(); i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }
            if (e.getEffectType() == tp) {
                if (e.getInUse()) {
                    return e;
                } else {
                    eventNotInUse = e;
                }
            }
        }
        return eventNotInUse;
    }

    public final L2Effect getFirstEffect(L2Skill.SkillType stp) {
        //FastTable<L2Effect> effects = _effectList.getAllEffectsTable();
        FastList<L2Effect> effects = _effects;
        if (effects.isEmpty()) {
            return null;
        }

        L2Effect e = null;
        L2Effect eventNotInUse = null;
        for (int i = 0; i < effects.size(); i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }
            if (e.getSkill().getSkillType() == stp) {
                if (e.getInUse()) {
                    return e;
                } else {
                    eventNotInUse = e;
                }
            }
        }
        return eventNotInUse;
    }
    // =========================================================

    // =========================================================
    // NEED TO ORGANIZE AND MOVE TO PROPER PLACE
    /**
     * This class permit to the L2Character AI to obtain informations and uses
     * L2Character method
     */
    public class AIAccessor {

        public AIAccessor() {
        }

        /**
         * Return the L2Character managed by this Accessor AI.<BR><BR>
         */
        public L2Character getActor() {
            return L2Character.this;
        }

        /**
         * Accessor to L2Character moveToLocation() method with an interaction
         * area.<BR><BR>
         */
        public void moveTo(int x, int y, int z, int offset) {
            L2Character.this.moveToLocation(x, y, z, offset);
        }

        /**
         * Accessor to L2Character moveToLocation() method without interaction
         * area.<BR><BR>
         */
        public void moveTo(int x, int y, int z) {
            L2Character.this.moveToLocation(x, y, z, 0);
        }

        /**
         * Accessor to L2Character stopMove() method.<BR><BR>
         */
        public void stopMove(L2CharPosition pos) {
            L2Character.this.stopMove(pos);
        }

        /**
         * Accessor to L2Character doAttack() method.<BR><BR>
         */
        public void doAttack(L2Character target) {
            L2Character.this.doAttack(target);
        }

        /**
         * Accessor to L2Character doCast() method.<BR><BR>
         */
        public void doCast(L2Skill skill) {
            L2Character.this.doCast(skill);
        }

        /**
         * Create a NotifyAITask.<BR><BR>
         */
        public NotifyAITask newNotifyTask(CtrlEvent evt) {
            return new NotifyAITask(evt);
        }

        /**
         * Cancel the AI.<BR><BR>
         */
        public void detachAI() {
            _ai = null;
        }
    }

    /**
     * This class group all mouvement data.<BR><BR>
     *
     * <B><U> Data</U> :</B><BR><BR> <li>_moveTimestamp : Last time position
     * update</li> <li>_xDestination, _yDestination, _zDestination : Position of
     * the destination</li> <li>_xMoveFrom, _yMoveFrom, _zMoveFrom : Position of
     * the origin</li> <li>_moveStartTime : Start time of the movement</li>
     * <li>_ticksToMove : Nb of ticks between the start and the destination</li>
     * <li>_xSpeedTicks, _ySpeedTicks : Speed in unit/ticks</li><BR><BR>
     *
     *
     */
    public static class MoveData {
        // when we retrieve x/y/z we use GameTimeControl.getGameTicks()
        // if we are moving, but move timestamp==gameticks, we don't need
        // to recalculate position

        public int _moveTimestamp;
        public int _xDestination;
        public int _yDestination;
        public int _zDestination;
        public int _xMoveFrom;
        public int _yMoveFrom;
        public int _zMoveFrom;
        public double _xAccurate; // otherwise there would be rounding errors
        public double _yAccurate;
        public double _zAccurate;
        public int _heading;
        public int _moveStartTime;
        public int _ticksToMove;
        public float _xSpeedTicks;
        public float _ySpeedTicks;
        public int onGeodataPathIndex;
        public List<AbstractNodeLoc> geoPath;
        public int geoPathAccurateTx;
        public int geoPathAccurateTy;
        public int geoPathGtx;
        public int geoPathGty;
    }
    /**
     * Table containing all skillId that are disabled
     */
    public Map<Integer, DisabledSkill> _disabledSkills = new ConcurrentHashMap<Integer, DisabledSkill>();

    public static class DisabledSkill {

        public int delay;
        public long expire;

        public DisabledSkill(int delay) {
            this.delay = delay;
            this.expire = System.currentTimeMillis() + delay;
        }

        public DisabledSkill(int delay, int remain) {
            this.delay = delay;
            this.expire = System.currentTimeMillis() + remain;
        }

        public boolean isInReuse() {
            if (System.currentTimeMillis() < expire) {
                return true;
            }
            delay = 0;
            expire = 0;
            return false;
        }

        public boolean isAugmentInReuse() {
            if (expire - System.currentTimeMillis() > 500) {
                return true;
            }
            delay = 0;
            expire = 0;
            return false;
        }
    }

    public void removeDisabledSkill(int id) {
        _disabledSkills.remove(id);
    }
    private boolean _allSkillsDisabled;
//	private int _flyingRunSpeed;
//	private int _floatingWalkSpeed;
//	private int _flyingWalkSpeed;
//	private int _floatingRunSpeed;
    /**
     * Movement data of this L2Character
     */
    protected MoveData _move;
    /**
     * Orientation of the L2Character
     */
    private int _heading;
    /**
     * L2Charcater targeted by the L2Character
     */
    private WeakReference<L2Object> _target;
    // set by the start of casting, in game ticks
    private int _castEndTime;
    private int _castInterruptTime;
    // set by the start of attack, in game ticks
    private long _attackEndTime;
    private int _attacking;
    private int _disableBowAttackEndTime;
    /**
     * Table of calculators containing all standard NPC calculator (ex :
     * ACCURACY_COMBAT, EVASION_RATE
     */
    private static final Calculator[] NPC_STD_CALCULATOR;

    static {
        NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
    }
    protected L2CharacterAI _ai;
    /**
     * Future Skill Cast
     */
    protected Future<?> _skillCast;
    /**
     * Char Coords from Client
     */
    private int _clientX;
    private int _clientY;
    private int _clientZ;
    private int _clientHeading;
    /**
     * List of all QuestState instance that needs to be notified of this
     * character's death
     */
    private List<QuestState> _NotifyQuestOfDeathList = new FastList<QuestState>();

    /**
     * Add QuestState instance that is to be notified of character's
     * death.<BR><BR>
     *
     * @param qs The QuestState that subscribe to this event
     *
     */
    public void addNotifyQuestOfDeath(QuestState qs) {
        if (qs == null || _NotifyQuestOfDeathList.contains(qs)) {
            return;
        }

        _NotifyQuestOfDeathList.add(qs);
    }

    /**
     * Return a list of L2Character that attacked.<BR><BR>
     */
    public final List<QuestState> getNotifyQuestOfDeath() {
        if (_NotifyQuestOfDeathList == null) {
            _NotifyQuestOfDeathList = new FastList<QuestState>();
        }

        return _NotifyQuestOfDeathList;
    }

    /**
     * Add a Func to the Calculator set of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> A L2Character owns a table of
     * Calculators called <B>_calculators</B>. Each Calculator (a calculator per
     * state) own a table of Func object. A Func object is a mathematic function
     * that permit to calculate the modifier of a state (ex :
     * REGENERATE_HP_RATE...). To reduce cache memory use, L2NPCInstances who
     * don't have skills share the same Calculator set called
     * <B>NPC_STD_CALCULATOR</B>.<BR><BR>
     *
     * That's why, if a L2NPCInstance is under a skill/spell effect that modify
     * one of its state, a copy of the NPC_STD_CALCULATOR must be create in its
     * _calculators before addind new Func object.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>If _calculators is linked to
     * NPC_STD_CALCULATOR, create a copy of NPC_STD_CALCULATOR in
     * _calculators</li> <li>Add the Func object to _calculators</li><BR><BR>
     *
     * @param f The Func object to add to the Calculator corresponding to the
     * state affected
     */
    public final synchronized void addStatFunc(Func f) {
        if (f == null) {
            return;
        }

        // Check if Calculator set is linked to the standard Calculator set of NPC
        if (_calculators == NPC_STD_CALCULATOR) {
            // Create a copy of the standard NPC Calculator set
            _calculators = new Calculator[Stats.NUM_STATS];

            for (int i = 0; i < Stats.NUM_STATS; i++) {
                if (NPC_STD_CALCULATOR[i] != null) {
                    _calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
                }
            }
        }

        // Select the Calculator of the affected state in the Calculator set
        int stat = f.stat.ordinal();

        if (_calculators[stat] == null) {
            _calculators[stat] = new Calculator();
        }

        // Add the Func to the calculator corresponding to the state
        _calculators[stat].addFunc(f);

    }

    /**
     * Add a list of Funcs to the Calculator set of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> A L2Character owns a table of
     * Calculators called <B>_calculators</B>. Each Calculator (a calculator per
     * state) own a table of Func object. A Func object is a mathematic function
     * that permit to calculate the modifier of a state (ex :
     * REGENERATE_HP_RATE...). <BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is ONLY for
     * L2PcInstance</B></FONT><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Equip an item from
     * inventory</li> <li> Learn a new passive skill</li> <li> Use an active
     * skill</li><BR><BR>
     *
     * @param funcs The list of Func objects to add to the Calculator
     * corresponding to the state affected
     */
    public final synchronized void addStatFuncs(Func[] funcs) {

        FastList<Stats> modifiedStats = new FastList<Stats>();

        for (Func f : funcs) {
            if (f == null) {
                continue;
            }
            modifiedStats.add(f.stat);
            addStatFunc(f);
        }
        broadcastModifiedStats(modifiedStats);
    }

    /**
     * Remove a Func from the Calculator set of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> A L2Character owns a table of
     * Calculators called <B>_calculators</B>. Each Calculator (a calculator per
     * state) own a table of Func object. A Func object is a mathematic function
     * that permit to calculate the modifier of a state (ex :
     * REGENERATE_HP_RATE...). To reduce cache memory use, L2NPCInstances who
     * don't have skills share the same Calculator set called
     * <B>NPC_STD_CALCULATOR</B>.<BR><BR>
     *
     * That's why, if a L2NPCInstance is under a skill/spell effect that modify
     * one of its state, a copy of the NPC_STD_CALCULATOR must be create in its
     * _calculators before addind new Func object.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Remove the Func object from
     * _calculators</li><BR><BR> <li>If L2Character is a L2NPCInstance and
     * _calculators is equal to NPC_STD_CALCULATOR, free cache memory and just
     * create a link on NPC_STD_CALCULATOR in _calculators</li><BR><BR>
     *
     * @param f The Func object to remove from the Calculator corresponding to
     * the state affected
     */
    public final synchronized void removeStatFunc(Func f) {
        if (f == null) {
            return;
        }

        // Select the Calculator of the affected state in the Calculator set
        int stat = f.stat.ordinal();

        if (_calculators[stat] == null) {
            return;
        }

        // Remove the Func object from the Calculator
        _calculators[stat].removeFunc(f);

        if (_calculators[stat].size() == 0) {
            _calculators[stat] = null;
        }

        // If possible, free the memory and just create a link on NPC_STD_CALCULATOR
        if (this.isL2Npc()) {
            int i = 0;
            for (; i < Stats.NUM_STATS; i++) {
                if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i])) {
                    break;
                }
            }

            if (i >= Stats.NUM_STATS) {
                _calculators = NPC_STD_CALCULATOR;
            }
        }
    }

    /**
     * Remove a list of Funcs from the Calculator set of the
     * L2PcInstance.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> A L2Character owns a table of
     * Calculators called <B>_calculators</B>. Each Calculator (a calculator per
     * state) own a table of Func object. A Func object is a mathematic function
     * that permit to calculate the modifier of a state (ex :
     * REGENERATE_HP_RATE...). <BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is ONLY for
     * L2PcInstance</B></FONT><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Unequip an item from
     * inventory</li> <li> Stop an active skill</li><BR><BR>
     *
     * @param funcs The list of Func objects to add to the Calculator
     * corresponding to the state affected
     */
    public final synchronized void removeStatFuncs(Func[] funcs) {

        FastList<Stats> modifiedStats = new FastList<Stats>();

        for (Func f : funcs) {
            if (f == null) {
                continue;
            }
            modifiedStats.add(f.stat);
            removeStatFunc(f);
        }

        broadcastModifiedStats(modifiedStats);

    }

    /**
     * Remove all Func objects with the selected owner from the Calculator set
     * of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> A L2Character owns a table of
     * Calculators called <B>_calculators</B>. Each Calculator (a calculator per
     * state) own a table of Func object. A Func object is a mathematic function
     * that permit to calculate the modifier of a state (ex :
     * REGENERATE_HP_RATE...). To reduce cache memory use, L2NPCInstances who
     * don't have skills share the same Calculator set called
     * <B>NPC_STD_CALCULATOR</B>.<BR><BR>
     *
     * That's why, if a L2NPCInstance is under a skill/spell effect that modify
     * one of its state, a copy of the NPC_STD_CALCULATOR must be create in its
     * _calculators before addind new Func object.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Remove all Func objects of the
     * selected owner from _calculators</li><BR><BR> <li>If L2Character is a
     * L2NPCInstance and _calculators is equal to NPC_STD_CALCULATOR, free cache
     * memory and just create a link on NPC_STD_CALCULATOR in
     * _calculators</li><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Unequip an item from
     * inventory</li> <li> Stop an active skill</li><BR><BR>
     *
     * @param owner The Object(Skill, Item...) that has created the effect
     */
    public final synchronized void removeStatsOwner(Object owner) {

        FastList<Stats> modifiedStats = null;
        // Go through the Calculator set
        for (int i = 0; i < _calculators.length; i++) {
            if (_calculators[i] != null) {
                // Delete all Func objects of the selected owner
                if (modifiedStats != null) {
                    modifiedStats.addAll(_calculators[i].removeOwner(owner));
                } else {
                    modifiedStats = _calculators[i].removeOwner(owner);
                }

                if (_calculators[i].size() == 0) {
                    _calculators[i] = null;
                }
            }
        }

        // If possible, free the memory and just create a link on NPC_STD_CALCULATOR
        if (this.isL2Npc()) {
            int i = 0;
            for (; i < Stats.NUM_STATS; i++) {
                if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i])) {
                    break;
                }
            }

            if (i >= Stats.NUM_STATS) {
                _calculators = NPC_STD_CALCULATOR;
            }
        }

        if (owner instanceof L2Effect && !((L2Effect) owner).preventExitUpdate) {
            broadcastModifiedStats(modifiedStats);
        }

    }

    private void broadcastModifiedStats(FastList<Stats> stats) {
        if (stats == null || stats.isEmpty()) {
            return;
        }

        boolean broadcastFull = false;
        boolean otherStats = false;
        StatusUpdate su = null;

        for (Stats stat : stats) {
            if (isL2Summon()) {
                updateAndBroadcastStatus(1);
                break;
            } else if (stat == Stats.POWER_ATTACK_SPEED) {
                if (su == null) {
                    su = new StatusUpdate(getObjectId());
                }
                su.addAttribute(StatusUpdate.ATK_SPD, getPAtkSpd());
            } else if (stat == Stats.MAGIC_ATTACK_SPEED) {
                if (su == null) {
                    su = new StatusUpdate(getObjectId());
                }
                su.addAttribute(StatusUpdate.CAST_SPD, getMAtkSpd());
            } //else if (stat==Stats.MAX_HP) // TODO: self only and add more stats...
            //{
            //	if (su == null) su = new StatusUpdate(getObjectId());
            //	su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
            //}
            else if (stat == Stats.MAX_CP) {
                if (this.isPlayer()) {
                    if (su == null) {
                        su = new StatusUpdate(getObjectId());
                    }
                    su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
                }
            } //else if (stat==Stats.MAX_MP) 
            //{
            //	if (su == null) su = new StatusUpdate(getObjectId());
            //	su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
            //}
            else if (stat == Stats.RUN_SPEED) {
                broadcastFull = true;
            } else {
                otherStats = true;
            }
        }

        if (this.isPlayer()) {
            if (broadcastFull) {
                updateAndBroadcastStatus(2);
            } else if (otherStats) {
                updateAndBroadcastStatus(1);
                if (su != null) {
                    FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
                    L2PcInstance pc = null;
                    for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                        pc = n.getValue();
                        if (pc == null) {
                            continue;
                        }

                        pc.sendPacket(su);
                    }
                    players.clear();
                    players = null;
                    pc = null;
                }
            } else if (su != null) {
                broadcastPacket(su);
            }
        } else if (this.isL2Npc()) {
            if (broadcastFull) {
                FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
                L2PcInstance pc = null;
                for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                    pc = n.getValue();
                    if (pc == null) {
                        continue;
                    }

                    if (getRunSpeed() == 0) {
                        pc.sendPacket(new ServerObjectInfo((L2NpcInstance) this, pc));
                    } else {
                        pc.sendPacket(new NpcInfo((L2NpcInstance) this, pc));
                    }
                }
                players.clear();
                players = null;
                pc = null;
            } else if (su != null) {
                broadcastPacket(su);
            }
        } else if (su != null) {
            broadcastPacket(su);
        }
        su = null;
    }

    /**
     * Return the orientation of the L2Character.<BR><BR>
     */
    @Override
    public final int getHeading() {
        if (isCastingNow()) {
            correctHeadingWhenCast();
        }
        return _heading;
    }

    private void correctHeadingWhenCast() {
        L2Character castingTarget = getAI().getCastTarget();
        if (castingTarget != null && !castingTarget.equals(this)) {
            setHeading(castingTarget, true);
        }
    }

    /**
     * Set the orientation of the L2Character.<BR><BR>
     */
    public final void setHeading(int heading) {
        _heading = heading;
    }

    public final void setHeading(final L2Object target, final boolean toChar) {
        if (target == null || target.equals(this)) {
            return;
        }

        _heading = (int) (Math.atan2(getY() - target.getY(), getX() - target.getX()) * 32768. / Math.PI) + (toChar ? 32768 : 0);
        if (_heading < 0) {
            _heading += 65536;
        }
    }

    /**
     * Return the X destination of the L2Character or the X position if not in
     * movement.<BR><BR>
     */
    public final int getClientX() {
        return _clientX;
    }

    public final int getClientY() {
        return _clientY;
    }

    public final int getClientZ() {
        return _clientZ;
    }

    public final int getClientHeading() {
        return _clientHeading;
    }

    public final void setClientX(int val) {
        _clientX = val;
    }

    public final void setClientY(int val) {
        _clientY = val;
    }

    public final void setClientZ(int val) {
        _clientZ = val;
    }

    public final void setClientHeading(int val) {
        _clientHeading = val;
    }

    public final int getXdestination() {
        MoveData m = _move;

        if (m != null) {
            return m._xDestination;
        }

        return getX();
    }

    /**
     * Return the Y destination of the L2Character or the Y position if not in
     * movement.<BR><BR>
     */
    public final int getYdestination() {
        MoveData m = _move;

        if (m != null) {
            return m._yDestination;
        }

        return getY();
    }

    /**
     * Return the Z destination of the L2Character or the Z position if not in
     * movement.<BR><BR>
     */
    public final int getZdestination() {
        MoveData m = _move;

        if (m != null) {
            return m._zDestination;
        }

        return getZ();
    }

    /**
     * Return True if the L2Character is in combat.<BR><BR>
     */
    public final boolean isInCombat() {
        return (getAI().getAttackTarget() != null);
    }

    /**
     * Return True if the L2Character is moving.<BR><BR>
     */
    public final boolean isMoving() {
        return _move != null;
    }

    /**
     * Return True if the L2Character is travelling a calculated path.<BR><BR>
     */
    public final boolean isOnGeodataPath() {
        MoveData m = _move;
        if (m == null) {
            return false;
        }
        if (m.geoPath == null) {
            return false;
        }
        if (m.onGeodataPathIndex == -1) {
            return false;
        }
        if (m.onGeodataPathIndex == m.geoPath.size() - 1) {
            return false;
        }
        return true;
    }

    /**
     * Return True if the L2Character is casting.<BR><BR>
     */
    public final boolean isCastingNow() {
        return _castEndTime > GameTimeController.getGameTicks();
    }

    /**
     * Return True if the cast of the L2Character can be aborted.<BR><BR>
     */
    public final boolean canAbortCast() {
        return _castInterruptTime > GameTimeController.getGameTicks();
    }

    /**
     * Return True if the L2Character is attacking.<BR><BR>
     */
    public final boolean isAttackingNow() {
        //return _attackEndTime > GameTimeController.getGameTicks();
        return _attackEndTime > System.currentTimeMillis();
    }

    /**
     * Return True if the L2Character has aborted its attack.<BR><BR>
     */
    public final boolean isAttackAborted() {
        return _attacking <= 0;
    }

    /**
     * Abort the attack of the L2Character and send Server->Client ActionFailed
     * packet.<BR><BR>
     */
    public final void abortAttack() {
        if (isAttackingNow()) {
            _attacking = 0;
            _attackEndTime = 0;
            sendActionFailed();
        }
    }

    public final void abortAttack(boolean mob, int atkTime) {
        if (mob) {
            _attacking = 0;
            _attackEndTime = System.currentTimeMillis() + atkTime;
            _attackReuseEndTime = System.currentTimeMillis() + atkTime;
            return;
        }

        if (isAttackingNow()) {
            _attacking = 0;
            _attackEndTime = 0;
            sendActionFailed();
        }
    }

    /**
     * Returns body part (paperdoll slot) we are targeting right now
     */
    public final int getAttackingBodyPart() {
        return _attacking;
    }

    /**
     * Abort the cast of the L2Character and send Server->Client
     * MagicSkillCanceld/ActionFailed packet.<BR><BR>
     */
    public final void abortCast() {
        if (isCastingNow()) {
            _castEndTime = 0;
            _castInterruptTime = 0;
            if (_skillCast != null) {
                _skillCast.cancel(true);
                _skillCast = null;
            }
            setCurrentSkill(null, false, false);

            if (getForceBuff() != null) {
                getForceBuff().delete();
            }

            L2Effect mog = getFirstEffect(L2Effect.EffectType.SIGNET_GROUND);
            if (mog != null) {
                mog.exit();
            }

            // cancels the skill hit scheduled task
            correctHeadingWhenCast();
            enableAllSkills();                                      // re-enables the skills
            if (this.isPlayer()) {
                getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING); // setting back previous intention
            }
            broadcastPacket(new MagicSkillCanceld(getObjectId()));  // broadcast packet to stop animations client-side
            sendActionFailed();                         // send an "action failed" packet to the caster
        }
    }

    /**
     * Update the position of the L2Character during a movement and return True
     * if the movement is finished.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> At the beginning of the move action, all
     * properties of the movement are stored in the MoveData object called
     * <B>_move</B> of the L2Character. The position of the start point and of
     * the destination permit to estimated in function of the movement speed the
     * time to achieve the destination.<BR><BR>
     *
     * When the movement is started (ex : by MovetoLocation), this method will
     * be called each 0.1 sec to estimate and update the L2Character position on
     * the server. Note, that the current server position can differe from the
     * current client position even if each movement is straight foward. That's
     * why, client send regularly a Client->Server ValidatePosition packet to
     * eventually correct the gap on the server. But, it's always the server
     * position that is used in range calculation.<BR><BR>
     *
     * At the end of the estimated movement time, the L2Character position is
     * automatically set to the destination position even if the movement is not
     * finished.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current Z position is
     * obtained FROM THE CLIENT by the Client->Server ValidatePosition Packet.
     * But x and y positions must be calculated to avoid that players try to
     * modify their movement speed.</B></FONT><BR><BR>
     *
     * @param gameTicks Nb of ticks since the server start
     * @return True if the movement is finished
     */
    public boolean updatePosition(int gameTicks) {
        MoveData m = _move;

        if (m == null) {
            return true;
        }

        if (!isVisible()) {
            _move = null;
            return true;
        }

        // Check if this is the first update
        if (m._moveTimestamp == 0) {
            m._moveTimestamp = m._moveStartTime;
            m._xAccurate = getX();
            m._yAccurate = getY();
        }

        // Check if the position has already been calculated
        if (m._moveTimestamp == gameTicks) {
            return false;
        }

        int xPrev = getX();
        int yPrev = getY();
        int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations

        double dx, dy, dz;
        if (Config.COORD_SYNCHRONIZE == 1) // the only method that can modify x,y while moving (otherwise _move would/should be set null)
        {
            dx = m._xDestination - xPrev;
            dy = m._yDestination - yPrev;
        } else // otherwise we need saved temporary values to avoid rounding errors
        {
            dx = m._xDestination - m._xAccurate;
            dy = m._yDestination - m._yAccurate;
        }

        final boolean isFloating = isFlying() || isInWater();
        dz = m._zDestination - zPrev;

        double delta = dx * dx + dy * dy;
        if (delta < 10000
                && (dz * dz > 2500) // close enough, allows error between client and server geodata if it cannot be avoided
                && !isFloating) // should not be applied on vertical movements in water or during flight
        {
            delta = Math.sqrt(delta);
        } else {
            delta = Math.sqrt(delta + dz * dz);
        }

        double distFraction = Double.MAX_VALUE;
        if (delta > 1) {
            final double distPassed = getStat().getMoveSpeed() * (gameTicks - m._moveTimestamp) / GameTimeController.TICKS_PER_SECOND;
            distFraction = distPassed / delta;
        }

        // if (Config.DEVELOPER) _log.warning("Move Ticks:" + (gameTicks - m._moveTimestamp) + ", distPassed:" + distPassed + ", distFraction:" + distFraction);
        if (distFraction > 1) // already there
        // Set the position of the L2Character to the destination
        {
            super.getPosition().setXYZ(m._xDestination, m._yDestination, m._zDestination);
        } else {
            m._xAccurate += dx * distFraction;
            m._yAccurate += dy * distFraction;

            // Set the position of the L2Character to estimated after parcial move
            super.getPosition().setXYZ((int) (m._xAccurate), (int) (m._yAccurate), zPrev + (int) (dz * distFraction + 0.5));
        }
        revalidateZone();

        // Set the timer of last position update to now
        m._moveTimestamp = gameTicks;

        return (distFraction > 1);
    }

    public void revalidateZone() {
        if (getWorldRegion() == null) {
            return;
        }

        getWorldRegion().revalidateZones(this);
    }

    /**
     * Stop movement of the L2Character (Called by AI Accessor only).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Delete movement data of the
     * L2Character </li> <li>Set the current position (x,y,z), its current
     * L2WorldRegion if necessary and its heading </li> <li>Remove the L2Object
     * object from _gmList** of GmListTable </li> <li>Remove object from
     * _knownObjects and _knownPlayer* of all surrounding L2WorldRegion
     * L2Characters </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send
     * Server->Client packet StopMove/StopRotation </B></FONT><BR><BR>
     *
     */
    public void stopMove(L2CharPosition pos) {
        stopMove(pos, true);
    }

    public void stopMove(L2CharPosition pos, boolean updateKnownObjects) {
        // Delete movement data of the L2Character
        _move = null;

        //if (getAI() != null)
        //  getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        // Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading
        // All data are contained in a L2CharPosition object
        if (pos != null) {
            getPosition().setXYZ(pos.x, pos.y, pos.z);
            setHeading(pos.heading);
            revalidateZone(true);
        }
        broadcastPacket(new StopMove(this));
        if (updateKnownObjects) {
            ThreadPoolManager.getInstance().executeAi(new KnownListAsynchronousUpdateTask(this), (isPlayer() || isPet() || isSummon()));
        }
    }

    /**
     * @return Returns the showSummonAnimation.
     */
    public boolean isShowSummonAnimation() {
        return _showSummonAnimation;
    }

    /**
     * @param showSummonAnimation The showSummonAnimation to set.
     */
    public void setShowSummonAnimation(boolean showSummonAnimation) {
        _showSummonAnimation = showSummonAnimation;
    }

    /**
     * Target a L2Object (add the target to the L2Character _target,
     * _knownObject and L2Character to _KnownObject of the L2Object).<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> The L2Object (including L2Character)
     * targeted is identified in <B>_target</B> of the L2Character<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Set the _target of L2Character to
     * L2Object </li> <li>If necessary, add L2Object to _knownObject of the
     * L2Character </li> <li>If necessary, add L2Character to _KnownObject of
     * the L2Object </li> <li>If object==null, cancel Attak or Cast
     * </li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance : Remove the
     * L2PcInstance from the old target _statusListener and add it to the new
     * target if it was a L2Character</li><BR><BR>
     *
     * @param object L2object to target
     *
     */
    public void setTarget(L2Object object) {
        if (object != null && !object.isVisible()) {
            object = null;
        }

        if (object != null && object != getTarget()) {
            getKnownList().addKnownObject(object);
            object.getKnownList().addKnownObject(this);
        }

        // If object==null, Cancel Attak or Cast
        if (object == null) {
            if (getTarget() != null) {
                broadcastPacket(new TargetUnselected(this));
            }

            if (isAttackingNow() && getAI().getAttackTarget() == getTarget()) {
                abortAttack();
                getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
            }

            if (isCastingNow() && canAbortCast() && getAI().getCastTarget() == getTarget()) {
                abortCast();
                getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
            }
        }
        _target = object == null ? null : new WeakReference<L2Object>(object);
    }

    /**
     * Return the identifier of the L2Object targeted or -1.<BR><BR>
     */
    public final int getTargetId() {
        /*
         * if(getTarget() != null) // _target = null; срабатывал return
         * getTarget().getObjectId(); return -1;
         */
        L2Object target = getTarget();
        if (target == null) {
            return -1;
        }

        return target.getObjectId();
    }

    /**
     * Return the L2Object targeted or null.<BR><BR>
     */
    public final L2Object getTarget() {
        if (_target == null) {
            return null;
        }

        final L2Object t = _target.get();
        if (t == null) {
            _target = null;
        }
        return t;
    }

    // called from AIAccessor only
    /**
     * Calculate movement data for a move to location action and add the
     * L2Character to movingObjects of GameTimeController (only called by AI
     * Accessor).<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> At the beginning of the move action, all
     * properties of the movement are stored in the MoveData object called
     * <B>_move</B> of the L2Character. The position of the start point and of
     * the destination permit to estimated in function of the movement speed the
     * time to achieve the destination.<BR><BR> All L2Character in movement are
     * identified in <B>movingObjects</B> of GameTimeController that will call
     * the updatePosition method of those L2Character each 0.1s.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Get current position of the
     * L2Character </li> <li>Calculate distance (dx,dy) between current position
     * and destination including offset </li> <li>Create and Init a MoveData
     * object </li> <li>Set the L2Character _move object to MoveData object
     * </li> <li>Add the L2Character to movingObjects of the GameTimeController
     * </li> <li>Create a task to notify the AI that L2Character arrives at a
     * check point of the movement </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send
     * Server->Client packet MoveToPawn/CharMoveToLocation </B></FONT><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> AI :
     * onIntentionMoveTo(L2CharPosition), onIntentionPickUp(L2Object),
     * onIntentionInteract(L2Object) </li> <li> FollowTask </li><BR><BR>
     *
     * @param x The X position of the destination
     * @param y The Y position of the destination
     * @param z The Y position of the destination
     * @param offset The size of the interaction area of the L2Character
     * targeted
     *
     */
    public void moveToLocationm(int x, int y, int z, int offset) {
        moveToLocation(x, y, z, offset);
    }

    protected void moveToLocation(int x, int y, int z, int offset) {
        if (getStat().getMoveSpeed() <= 0 || isMovementDisabled()) {
            sendActionFailed();
            return;
        }
        clearNextLoc();

        /*
         * if (!isInWater() && !isFlying() && Math.abs(getZ() - z) > 3000)
         * return;
         */
        // Get current position of the L2Character
        final int curX = super.getX();
        final int curY = super.getY();
        final int curZ = super.getZ();
        /*
         * if (isPlayer()) { ((L2PcInstance)
         * this).sendAdmResultMessage("##moveToLocation#1####x:"+curX+"
         * y:"+curY+" z:"+curZ); ((L2PcInstance)
         * this).sendAdmResultMessage("##moveToLocation#1#>>#x:"+x+" y:"+y+"
         * z:"+z); }
         */

        // проверка на двери (Line2D-/-Line2D) тут, что-бы не вызывать далее кучу математических просчетов
        if (DoorTable.getInstance().checkIfDoorsBetween(this, curX, curY, curZ, x, y, z)) {
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            return;
        }

        // Calculate distance (dx,dy) between current position and destination
        // TODO: improve Z axis move/follow support when dx,dy are small compared to dz
        double dx = (x - curX);
        double dy = (y - curY);
        double dz = (z - curZ);
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (isInWater() && distance > 700) {
            double divider = 700 / distance;
            x = curX + (int) (divider * dx);
            y = curY + (int) (divider * dy);
            z = curZ + (int) (divider * dz);
            dx = (x - curX);
            dy = (y - curY);
            dz = (z - curZ);
            distance = Math.sqrt(dx * dx + dy * dy);
        }

        //if (Config.DEBUG) _log.fine("distance to target:" + distance);
        // Define movement angles needed
        // ^
        // |     X (x,y)
        // |   /
        // |  /distance
        // | /
        // |/ angle
        // X ---------->
        // (curx,cury)
        double cos;
        double sin;

        // Check if a movement offset is defined or no distance to go through
        if (offset > 0 || distance < 1) {
            // approximation for moving closer when z coordinates are different
            // TODO: handle Z axis movement better
            offset -= Math.abs(dz);
            if (offset < 5) {
                offset = 5;
            }

            // If no distance to go through, the movement is canceled
            if (distance < 1 || distance - offset <= 0) {
                /*
                 * sin = 0; cos = 1; distance = 0; x = curX; y = curY;
                 */

                //if (Config.DEBUG) _log.fine("already in range, no movement needed.");
                // Notify the AI that the L2Character is arrived at destination
                getAI().notifyEvent(CtrlEvent.EVT_ARRIVED, null);

                return;
            }
            // Calculate movement angles needed
            sin = dy / distance;
            cos = dx / distance;

            distance -= (offset - 5); // due to rounding error, we have to move a bit closer to be in range

            // Calculate the new destination with offset included
            x = curX + (int) (distance * cos);
            y = curY + (int) (distance * sin);

        } else {
            // Calculate movement angles needed
            sin = dy / distance;
            cos = dx / distance;
        }

        // Create and Init a MoveData object
        MoveData m = new MoveData();
        // GEODATA MOVEMENT CHECKS AND PATHFINDING
        m.onGeodataPathIndex = -1; // Initialize not on geodata path

        if (checkMove()) {
            double originalDistance = distance;
            int originalX = x;
            int originalY = y;
            int originalZ = z;
            int gtx = (originalX - L2World.MAP_MIN_X) >> 4;
            int gty = (originalY - L2World.MAP_MIN_Y) >> 4;
            // Movement checks:
            // when geodata == 2, for all characters except mobs returning home (could be changed later to teleport if pathfinding fails)
            // when geodata == 1, for l2playableinstance and l2riftinstance only
            if (checkMoveDestiny()) {
                if (isOnGeodataPath()) {
                    if (gtx == _move.geoPathGtx && gty == _move.geoPathGty) {
                        return;
                    } else {
                        _move.onGeodataPathIndex = -1; // Set not on geodata path
                    }
                }

                if (curX < L2World.MAP_MIN_X || curX > L2World.MAP_MAX_X || curY < L2World.MAP_MIN_Y || curY > L2World.MAP_MAX_Y) {
                    // Temporary fix for character outside world region errors
                    //_log.warning("Character "+this.getName()+" outside world area, in coordinates x:"+curX+" y:"+curY);
                    getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                    if (isPlayer()) {
                        logout();
                    } else if (isL2Summon()) {
                        return; // preventation when summon get out of world coords, player will not loose him, unsummon handled from pcinstance			
                    } else {
                        this.onDecay();
                    }
                    return;
                }
                Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z, getInstanceId());
                // location different if destination wasn't reached (or just z coord is different)
                /*
                 * x = destiny.getX(); y = destiny.getY(); if
                 * (!isInsideZone(ZONE_WATER)) // check: perhaps should be
                 * inside moveCheck z = destiny.getZ(); distance = Math.sqrt((x
                 * - curX)*(x - curX) + (y - curY)*(y - curY));
                 */
                x = destiny.getX();
                y = destiny.getY();
                z = destiny.getZ();
                dx = x - curX;
                dy = y - curY;
                dz = z - curZ;
                distance = Math.sqrt(dx * dx + dy * dy);
            }
            // Pathfinding checks. Only when geodata setting is 2, the LoS check gives shorter result
            // than the original movement was and the LoS gives a shorter distance than 2000
            // This way of detecting need for pathfinding could be changed.
            if (geoPathfind() && originalDistance - distance > 30 && distance < 2000) {
                // Path calculation
                // Overrides previous movement check
                if (this.isL2Playable() || this.isInCombat()) {
                    //int gx = (curX - L2World.MAP_MIN_X) >> 4;
                    //int gy = (curY - L2World.MAP_MIN_Y) >> 4;

                    //m.geoPath = GeoPathFinding.getInstance().findPath(gx, gy, (short)curZ, gtx, gty, (short)originalZ);
                    //long start = System.currentTimeMillis();
                    //System.out.println("##start##");
                    m.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, getInstanceId(), isPlayer());
                    //System.out.println("##finish##" + Util.formatAdena((int) (System.currentTimeMillis() - start)) + " ms. // size: " + m.geoPath.size());
                    if (m.geoPath == null || m.geoPath.size() < 2) // No path found
                    {
                        //System.out.println("##m.geoPath == null######!!!!!!!!!!###");
                        // Even though there's no path found (remember geonodes aren't perfect), 
                        // the mob is attacking and right now we set it so that the mob will go
                        // after target anyway, is dz is small enough. Summons will follow their masters no matter what.
                        if (this.isPlayer()
                                || (!(this.isL2Playable()) && Math.abs(z - curZ) > 140)
                                || (this.isL2Summon() && !((L2Summon) this).getFollowStatus())) {
                            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                            return;
                        } else {
                            x = originalX;
                            y = originalY;
                            z = originalZ;
                            distance = originalDistance;
                        }
                    } else {
                        m.onGeodataPathIndex = 0; // on first segment
                        m.geoPathGtx = gtx;
                        m.geoPathGty = gty;
                        m.geoPathAccurateTx = originalX;
                        m.geoPathAccurateTy = originalY;

                        x = m.geoPath.get(m.onGeodataPathIndex).getX();
                        y = m.geoPath.get(m.onGeodataPathIndex).getY();
                        z = m.geoPath.get(m.onGeodataPathIndex).getZ();

                        /*
                         * if (isPlayer()) { ((L2PcInstance)
                         * this).sendAdmResultMessage("##moveToLocation#2####x:"+curX+"
                         * y:"+curY+" z:"+curZ); ((L2PcInstance)
                         * this).sendAdmResultMessage("##moveToLocation#2#>>#x:"+x+"
                         * y:"+y+" z:"+z); }
                         */
                        // финальная проверка на двери
                        if (DoorTable.getInstance().checkIfDoorsBetween(this, curX, curY, curZ, x, y, z)) {
                            m.geoPath.clear();
                            m.geoPath = null;
                            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                            return;
                        }

                        /*
                         * // check for doors in the route for (int i = 0; i <
                         * m.geoPath.size()-1; i++) { if
                         * (DoorTable.getInstance().checkIfDoorsBetween(m.geoPath.get(i),m.geoPath.get(i+1)))
                         * { m.geoPath = null;
                         * getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                         * return; } }
                         */
                        // not in use: final check if we can indeed reach first path node (path nodes sometimes aren't accurate enough)
                        // but if the node is very far, then a shorter check (like 3 blocks) would be enough
                        // something similar might be needed for end
                        /*
                         * Location destiny =
                         * GeoData.getInstance().moveCheck(curX, curY, curZ, x,
                         * y, z); if (destiny.getX() != x || destiny.getY() !=
                         * y) { m.geoPath = null; getAI().stopFollow();
                         * getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                         * return; }
                         */
                        dx = x - curX;
                        dy = y - curY;
                        dz = z - curZ;
                        distance = Math.sqrt(dx * dx + dy * dy);
                        sin = dy / distance;
                        cos = dx / distance;
                    }
                }
            }
            // If no distance to go through, the movement is canceled
            if (distance < 1
                    && (geoPathfind() || this.isL2Playable() || this.isAfraid() || this.isL2RiftInvader())) {
                /*
                 * sin = 0; cos = 1; distance = 0; x = curX; y = curY;
                 */

                setFollowStatus(false);
                //getAI().notifyEvent(CtrlEvent.EVT_ARRIVED, null);
                getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE); //needed?
                return;
            }
        }

        // Get the Move Speed of the L2Charcater
        float speed = getStat().getMoveSpeed();

        if (isFlying() || isInWater()) {
            distance = Math.sqrt(distance * distance + dz * dz);
        }

        // Caclulate the Nb of ticks between the current position and the destination
        // One tick added for rounding reasons
        m._ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);

        // Calculate the xspeed and yspeed in unit/ticks in function of the movement speed
        m._xSpeedTicks = (float) (cos * speed / GameTimeController.TICKS_PER_SECOND);
        m._ySpeedTicks = (float) (sin * speed / GameTimeController.TICKS_PER_SECOND);

        // Calculate and set the heading of the L2Character
        int heading = calcHeading(x, y);
        setHeading(heading);
        m._xDestination = x;
        m._yDestination = y;
        m._zDestination = z; // this is what was requested from client
        m._heading = heading;

        m._moveStartTime = GameTimeController.getGameTicks();
        m._xMoveFrom = curX;
        m._yMoveFrom = curY;
        m._zMoveFrom = curZ;

        //if (Config.DEBUG) _log.fine("time to target:" + m._ticksToMove);
        // Set the L2Character _move object to MoveData object
        _move = m;

        // Add the L2Character to movingObjects of the GameTimeController
        // The GameTimeController manage objects movement
        GameTimeController.getInstance().registerMovingObject(this);

        //int tm = m._ticksToMove*GameTimeController.MILLIS_IN_TICK;
        // Create a task to notify the AI that L2Character arrives at a check point of the movement
        if (m._ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000) {
            ThreadPoolManager.getInstance().scheduleMove(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
        }

        // the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
        // to destination by GameTimeController
    }

    private boolean checkMove() {
        if (isFlying() || isInWater()) // || isL2NpcWalker())
        {
            return false;
        }

        return true;
    }

    private boolean checkMoveDestiny() {
        if (geoPathfind() && !(isReturningToSpawnPoint())) {
            return true;
        }

        return (this.isPlayer() || (this.isL2Summon() && !(this.getAI().getIntention() == AI_INTENTION_FOLLOW)) || this.isAfraid() || this.isL2RiftInvader());
    }

    public boolean moveToNextRoutePoint() {
        if (!isOnGeodataPath()) {
            // Cancel the move action
            _move = null;
            return false;
        }

        // Get the Move Speed of the L2Charcater
        if (getStat().getMoveSpeed() <= 0 || isMovementDisabled()) {
            // Cancel the move action
            _move = null;
            return false;
        }

        MoveData md = _move;
        if (md == null) {
            return false;
        }

        // Create and Init a MoveData object
        MoveData m = new MoveData();

        // Update MoveData object
        m.onGeodataPathIndex = md.onGeodataPathIndex + 1; // next segment
        m.geoPath = md.geoPath;
        m.geoPathGtx = md.geoPathGtx;
        m.geoPathGty = md.geoPathGty;
        m.geoPathAccurateTx = md.geoPathAccurateTx;
        m.geoPathAccurateTy = md.geoPathAccurateTy;

        //System.out.println("##moveToNextRoutePoint##" + m.onGeodataPathIndex);
        if (md.onGeodataPathIndex == md.geoPath.size() - 2) {
            m._xDestination = md.geoPathAccurateTx;
            m._yDestination = md.geoPathAccurateTy;
            m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
        } else {
            m._xDestination = md.geoPath.get(m.onGeodataPathIndex).getX();
            m._yDestination = md.geoPath.get(m.onGeodataPathIndex).getY();
            m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
        }

        if (DoorTable.getInstance().checkIfDoorsBetween(this, getX(), getY(), getZ(), m._xDestination, m._yDestination, m._zDestination)) {
            _move = null;
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            return false;
        }

        float speed = getStat().getMoveSpeed();
        double dx = (m._xDestination - super.getX());
        double dy = (m._yDestination - super.getY());
        double distance = Math.sqrt(dx * dx + dy * dy);
        // Calculate and set the heading of the L2Character
        if (distance != 0) {
            setHeading(calcHeading(m._xDestination, m._yDestination));
        }

        // Caclulate the Nb of ticks between the current position and the destination
        // One tick added for rounding reasons
        int ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);

        m._heading = 0; // initial value for coordinate sync

        m._moveStartTime = GameTimeController.getGameTicks();

        // Set the L2Character _move object to MoveData object
        _move = m;

        // Add the L2Character to movingObjects of the GameTimeController
        // The GameTimeController manage objects movement
        GameTimeController.getInstance().registerMovingObject(this);

        // Create a task to notify the AI that L2Character arrives at a check point of the movement
        if (ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000) {
            ThreadPoolManager.getInstance().scheduleMove(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
        }

        // the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
        // to destination by GameTimeController
        // Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
        broadcastPacket(new CharMoveToLocation(this));
        return true;
    }

    public boolean validateMovementHeading(int heading) {
        MoveData m = _move;

        if (m == null) {
            return true;
        }

        boolean result = true;
        if (m._heading != heading) {
            result = (m._heading == 0); // initial value or false
            m._heading = heading;
        }

        return result;
    }

    /**
     * Return the distance between the current position of the L2Character and
     * the target (x,y).<BR><BR>
     *
     * @param x X position of the target
     * @param y Y position of the target
     * @return the plan distance
     *
     * @deprecated use getPlanDistanceSq(int x, int y, int z)
     */
    @Deprecated
    public final double getDistance(int x, int y) {
        double dx = x - getX();
        double dy = y - getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Return the distance between the current position of the L2Character and
     * the target (x,y).<BR><BR>
     *
     * @param x X position of the target
     * @param y Y position of the target
     * @return the plan distance
     *
     * @deprecated use getPlanDistanceSq(int x, int y, int z)
     */
    @Deprecated
    public final double getDistance(int x, int y, int z) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Return the squared distance between the current position of the
     * L2Character and the given object.<BR><BR>
     *
     * @param object L2Object
     * @return the squared distance
     */
    public final double getDistanceSq(L2Object object) {
        return getDistanceSq(object.getX(), object.getY(), object.getZ());
    }

    /**
     * Return the squared distance between the current position of the
     * L2Character and the given x, y, z.<BR><BR>
     *
     * @param x X position of the target
     * @param y Y position of the target
     * @param z Z position of the target
     * @return the squared distance
     */
    public final double getDistanceSq(int x, int y, int z) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();

        return (dx * dx + dy * dy + dz * dz);
    }

    /**
     * Return the squared plan distance between the current position of the
     * L2Character and the given object.<BR> (check only x and y, not z)<BR><BR>
     *
     * @param object L2Object
     * @return the squared plan distance
     */
    public final double getPlanDistanceSq(L2Object object) {
        return getPlanDistanceSq(object.getX(), object.getY());
    }

    /**
     * Return the squared plan distance between the current position of the
     * L2Character and the given x, y, z.<BR> (check only x and y, not
     * z)<BR><BR>
     *
     * @param x X position of the target
     * @param y Y position of the target
     * @return the squared plan distance
     */
    public final double getPlanDistanceSq(int x, int y) {
        double dx = x - getX();
        double dy = y - getY();

        return (dx * dx + dy * dy);
    }

    /**
     * Check if this object is inside the given radius around the given object.
     * Warning: doesn't cover collision radius!<BR><BR>
     *
     * @param object the target
     * @param radius the radius around the target
     * @param checkZ should we check Z axis also
     * @param strictCheck true if (distance < radius), false if (distance <=
     * radius) @return t rue is the L2Character is inside the radius.
     *
     * @see net.sf.l2j.gameserver.model.L2Character.isInsideRadius(int x, int y,
     * int z, int radius, boolean checkZ, boolean strictCheck)
     */
    public final boolean isInsideRadius(L2Object object, int radius, boolean checkZ, boolean strictCheck) {
        if (object == null) {
            return false;
        }
        return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
    }

    /**
     * Check if this object is inside the given plan radius around the given
     * point. Warning: doesn't cover collision radius!<BR><BR>
     *
     * @param x X position of the target
     * @param y Y position of the target
     * @param radius the radius around the target
     * @param strictCheck true if (distance < radius), false if (distance <=
     * radius) @return t rue is the L2Character is inside the radius.
     */
    public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck) {
        return isInsideRadius(x, y, 0, radius, false, strictCheck);
    }

    /**
     * Check if this object is inside the given radius around the given
     * point.<BR><BR>
     *
     * @param x X position of the target
     * @param y Y position of the target
     * @param z Z position of the target
     * @param radius the radius around the target
     * @param checkZ should we check Z axis also
     * @param strictCheck true if (distance < radius), false if (distance <=
     * radius) @return t rue is the L2Character is inside the radius.
     */
    public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();

        if (strictCheck) {
            if (checkZ) {
                return (dx * dx + dy * dy + dz * dz) < radius * radius;
            } else {
                return (dx * dx + dy * dy) < radius * radius;
            }
        } else if (checkZ) {
            return (dx * dx + dy * dy + dz * dz) <= radius * radius;
        } else {
            return (dx * dx + dy * dy) <= radius * radius;
        }
    }

//	/**
//	* event that is called when the destination coordinates are reached
//	*/
//	public void onTargetReached()
//	{
//	L2Character pawn = getPawnTarget();
//
//	if (pawn != null)
//	{
//	int x = pawn.getX(), y=pawn.getY(),z = pawn.getZ();
//
//	double distance = getDistance(x,y);
//	if (getCurrentState() == STATE_FOLLOW)
//	{
//	calculateMovement(x,y,z,distance);
//	return;
//	}
//
//	//          takes care of moving away but distance is 0 so i won't follow problem
//
//
//	if (((distance > getAttackRange()) && (getCurrentState() == STATE_ATTACKING)) || (pawn.isMoving() && getCurrentState() != STATE_ATTACKING))
//	{
//	calculateMovement(x,y,z,distance);
//	return;
//	}
//
//	}
//	//       update x,y,z with the current calculated position
//	stopMove();
//
//	if (Config.DEBUG)
//	_log.fine(this.getName() +":: target reached at: x "+getX()+" y "+getY()+ " z:" + getZ());
//
//	if (getPawnTarget() != null)
//	{
//
//	setPawnTarget(null);
//	setMovingToPawn(false);
//	}
//	}
//
//	public void setTo(int x, int y, int z, int heading)
//	{
//	setX(x);
//	setY(y);
//	setZ(z);
//	setHeading(heading);
//	updateCurrentWorldRegion(); //TODO: maybe not needed here
//	if (isMoving())
//	{
//	setCurrentState(STATE_IDLE);
//	StopMove setto = new StopMove(this);
//	broadcastPacket(setto);
//	}
//	else
//	{
//	ValidateLocation setto = new ValidateLocation(this);
//	broadcastPacket(setto);
//	}
//
//	FinishRotation fr = new FinishRotation(this);
//	broadcastPacket(fr);
//	}
//	protected void startCombat()
//	{
//	if (_currentAttackTask == null )//&& !isInCombat())
//	{
//	_currentAttackTask = ThreadPoolManager.getInstance().scheduleMed(new AttackTask(), 0);
//	}
//	else
//	{
//	_log.info("multiple attacks want to start in parallel. prevented.");
//	}
//	}
//
    /**
     * Return the Weapon Expertise Penalty of the L2Character.<BR><BR>
     */
    public float getWeaponExpertisePenalty() {
        return 1.f;
    }

    /**
     * Return the Armour Expertise Penalty of the L2Character.<BR><BR>
     */
    public float getArmourExpertisePenalty() {
        return 1.f;
    }

    /**
     * Set _attacking corresponding to Attacking Body part to CHEST.<BR><BR>
     */
    public void setAttackingBodypart() {
        _attacking = Inventory.PAPERDOLL_CHEST;
    }

    /**
     * Retun True if arrows are available.<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     *
     */
    protected boolean checkAndEquipArrows() {
        return true;
    }

    /**
     * Add Exp and Sp to the L2Character.<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li> <li>
     * L2PetInstance</li><BR><BR>
     *
     */
    public void addExpAndSp(long addToExp, int addToSp) {
        // Dummy method (overridden by players and pets)
    }

    /**
     * Return the active weapon instance (always equiped in the right
     * hand).<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     *
     */
    public abstract L2ItemInstance getActiveWeaponInstance();

    /**
     * Return the active weapon item (always equiped in the right hand).<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     *
     */
    public abstract L2Weapon getActiveWeaponItem();

    /**
     * Return the secondary weapon instance (always equiped in the left
     * hand).<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     *
     */
    public abstract L2ItemInstance getSecondaryWeaponInstance();

    /**
     * Return the secondary weapon item (always equiped in the left
     * hand).<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     *
     */
    public abstract L2Weapon getSecondaryWeaponItem();

    /**
     * Manage hit process (called by Hit Task).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>If the attacker/target is dead or
     * use fake death, notify the AI with EVT_CANCEL and send a Server->Client
     * packet ActionFailed (if attacker is a L2PcInstance)</li> <li>If attack
     * isn't aborted, send a message system (critical hit, missed...) to
     * attacker/target if they are L2PcInstance </li> <li>If attack isn't
     * aborted and hit isn't missed, reduce HP of the target and calculate
     * reflection damage to reduce HP of attacker if necessary </li> <li>if
     * attack isn't aborted and hit isn't missed, manage attack or cast break of
     * the target (calculating rate, sending message...) </li><BR><BR>
     *
     * @param target The L2Character targeted
     * @param damage Nb of HP to reduce
     * @param crit True if hit is critical
     * @param miss True if hit is missed
     * @param soulshot True if SoulShot are charged
     * @param shld True if shield is efficient
     *
     */
    protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld) {
        // If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL
        // and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)
        if (target == null || isAlikeDead() || isEventMob()) {
            getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
            return;
        }

        if (target.isInvul()) {
            sendUserPacket(Static.TARGET_IS_INVUL);
            damage = 0;
        }

        if ((isL2Npc() && target.isAlikeDead()) || target.isDead() || (!getKnownList().knowsObject(target) && !(isL2Door()))) {
            //getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
            getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
            sendActionFailed();
            return;
        }

        // Check Raidboss attack and
        // check buffing chars who attack raidboss. Results in mute.
        if (Config.ALLOW_RAID_BOSS_PUT && target.isRaid() && getLevel() > target.getLevel() + 8) {
            SkillTable.getInstance().getInfo(4515, 1).getEffects(this, this);
            return;
        }

        if (miss) {
            sendUserPacket(Static.MISSED_TARGET);
            if (target.isPlayer()) {
                SystemMessage sm = SystemMessage.id(SystemMessageId.AVOIDED_S1S_ATTACK);
                if (isL2Summon()) {
                    sm.addNpcName(getNpcId());
                } else {
                    sm.addString(getName());
                }
                target.sendUserPacket(sm);
                sm = null;
            }
            //Recharge AutoShots
            rechargeAutoSoulShot(true, false, isL2Summon());

            if (!isCastingNow()) {
                getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
            }
            return;
        }

        // If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance
        if (isAttackAborted()) {
            rechargeAutoSoulShot(true, false, isL2Summon());
            return;
        }

        sendDamageMessage(target, damage, false, crit, miss);

        // If L2Character target is a L2PcInstance, send a system message
        if (target.isPlayer()) {
            //L2PcInstance enemy = (L2PcInstance)target;

            // Check if shield is efficient
            if (shld) {
                target.sendUserPacket(Static.SHIELD_DEFENCE_SUCCESSFULL);
            }
            //else if (!miss && damage < 1)
            //enemy.sendMessage("You hit the target's armor.");
        } else if (target.isL2Summon()) {
            target.getOwner().sendUserPacket(SystemMessage.id(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_S1).addString(getName()).addNumber(damage));
        }

        if (!miss && damage > 0) {
            int reflectedDamage = 0;
            int repostDamage = 0;
            if (!isBowEquppied(getActiveWeaponItem())) // Do not reflect or absorb if weapon is of type bow
            {
                // Absorb HP from the damage inflicted
                if (getCurrentHp() > 0) {
                    if (target.getFirstEffect(340) != null) {
                        repostDamage = (int) (damage * 0.3);
                        repostDamage = Math.min(repostDamage, (int) getMaxHp());
                        repostDamage = Math.max(repostDamage, 0);
                    }
                    // Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
                    double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
                    if (reflectPercent > 0) {
                        reflectedDamage = (int) (reflectPercent / 100. * damage);
                        damage -= reflectedDamage;
                    }
                    reflectedDamage += repostDamage;
                    reflectedDamage = Math.min(reflectedDamage, (int) getMaxHp());
                    reflectedDamage = Math.max(reflectedDamage, 0);

                    double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);
                    if (absorbPercent > 0) {
                        int absorbDamage = (int) (absorbPercent / 100. * damage);
                        absorbDamage = Math.min(absorbDamage, (int) (getMaxHp() - getCurrentHp()));// Can't absord more than max hp
                        if (absorbDamage > 0) {
                            setCurrentHp(getCurrentHp() + absorbDamage);
                        }
                    }
                }
            }
            if (isRaid() || isGrandRaid() || (Config.DISABLE_REFLECT_ON_MOBS && isL2Monster())) {
                reflectedDamage = 0;
            }

            target.reduceCurrentHp(damage, this);
            if (reflectedDamage > 0 && getCurrentHp() > reflectedDamage) {
                getStatus().reduceHp(reflectedDamage, target, true);
            }
            /*if (repostDamage > 0 && getCurrentHp() > repostDamage) {
             getStatus().reduceHp(repostDamage, target, true);
             }*/

            // Notify AI with EVT_ATTACKED
            target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
            getAI().clientStartAutoAttack();

            // Manage attack or cast break of the target (calculating rate, sending message...)
            if (!target.isRaid() && Formulas.calcAtkBreak(target, damage)) {
                target.breakAttack();
                target.breakCast();
            }

            // Maybe launch chance skills on us
            if (getChanceSkills() != null) {
                if (getSkillLevel(3207) > 0) {
                    if (Rnd.get(100) < 5) {
                        setCurrentHp(getCurrentHp() + 522);
                        broadcastPacket(new MagicSkillUser(this, this, 5123, 1, 0, 0));
                    }
                } else if (Rnd.get(100) < 15) {
                    getChanceSkills().onHit(target, false, crit);
                }
            }
            // Maybe launch chance skills on target
            if (target.getChanceSkills() != null) {
                if (getSkillLevel(3213) > 0) {
                    if (Rnd.get(100) < 5) {
                        target.setCurrentCp(target.getCurrentCp() + 473);
                        target.broadcastPacket(new MagicSkillUser(this, this, 5123, 1, 0, 0));
                    }
                } else if (Rnd.get(100) < 15) {
                    target.getChanceSkills().onHit(this, true, crit);
                }
            }

            //invocation
            if (target.isImmobileUntilAttacked()) {
                target.stopImmobileUntilAttacked(null);
            }

            if (isPlayer()) {
                /*
                 * if (PcPlayer.isInsideAqZone()) { if (PcPlayer.getLevel() >
                 * 46) {
                 * this.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                 * this.abortAttack(); this.abortCast();
                 *
                 * L2Skill tempSkill = SkillTable.getInstance().getInfo(4515,
                 * 1); if(tempSkill != null) tempSkill.getEffects(this, this);
                 * else _log.warning("Skill 4515 at level 1 is missing in DP.");
                 * } }
                 */

                if (target.isPlayer()) {
                    if (!isInParty() || (isInParty() && getParty().getPartyMembers().contains(target))) {
                        boolean haveBuff = false;
                        //mirage	 
                        if (target.getFirstEffect(445) != null) {
                            if (Rnd.get(100) < Config.MIRAGE_CHANCE) {
                                getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                                setTarget(null);
                                abortAttack();
                                abortCast();
                                haveBuff = true;
                            }
                        }

                        if (Config.ALLOW_APELLA_BONUSES) {
                            //apella - root
                            if (target.getSkillLevel(3608) > 0) {
                                if ((target.getFirstEffect(4202) == null) && (Rnd.get(100) < 15)) {
                                    SkillTable.getInstance().getInfo(4202, 12).getEffects(this, this);
                                    haveBuff = true;
                                }
                            }
                            //apella - RunSpd
                            if (target.getSkillLevel(3609) > 0) {
                                if ((target.getFirstEffect(4200) == null) && (Rnd.get(100) < 15)) {
                                    SkillTable.getInstance().getInfo(4200, 12).getEffects(this, this);
                                    haveBuff = true;
                                }
                            }
                            //apella - AtkSpd
                            if (target.getSkillLevel(3610) > 0) {
                                if ((target.getFirstEffect(4203) == null) && (Rnd.get(100) < 15)) {
                                    SkillTable.getInstance().getInfo(4203, 12).getEffects(this, this);
                                    haveBuff = true;
                                }
                            }
                        }

                        if (haveBuff) {
                            this.broadcastPacket(new MagicSkillUser(this, this, 5144, 1, 0, 0));
                        }
                    }
                }
            }
        }

        // Launch weapon Special ability effect if available
        launchSaOnTarget(getActiveWeaponItem(), target, crit);

        //Recharge AutoShots
        rechargeAutoSoulShot(true, false, isL2Summon());
    }

    private boolean isBowEquppied(L2Weapon wpn) {
        if (wpn == null) {
            return false;
        }

        return wpn.isBowWeapon();
    }

    private void launchSaOnTarget(L2Weapon wpn, L2Character target, boolean crit) {
        if (wpn == null) {
            return;
        }
        wpn.getSkillEffects(this, target, crit);
    }

    /**
     * Break an attack and send Server->Client ActionFailed packet and a System
     * Message to the L2Character.<BR><BR>
     */
    public void breakAttack() {
        if (isAttackingNow()) {
            // Abort the attack of the L2Character and send Server->Client ActionFailed packet
            abortAttack();

            //if (this.isPlayer())
            //{
            //TODO Remove sendPacket because it's always done in abortAttack
            //sendActionFailed();
            // Send a system message
            sendUserPacket(Static.ATTACK_FAILED);
            //}
        }
    }

    /**
     * Break a cast and send Server->Client ActionFailed packet and a System
     * Message to the L2Character.<BR><BR>
     */
    public void breakCast() {
        // damage can only cancel magical skills
        if (isCastingNow() && canAbortCast() && getLastSkillCast() != null && getLastSkillCast().isMagic()) {
            // Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.
            abortCast();

            //if (this.isPlayer())
            //{
            // Send a system message
            sendUserPacket(Static.CASTING_INTERRUPTED);
            //}
        }
    }

    /**
     * Reduce the arrow number of the L2Character.<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     *
     */
    protected void reduceArrowCount() {
        // default is to do nothin
    }

    /**
     * Manage Forced attack (shift + select target).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>If L2Character or target is in a
     * town area, send a system message TARGET_IN_PEACEZONE a Server->Client
     * packet ActionFailed </li> <li>If target is confused, send a
     * Server->Client packet ActionFailed </li> <li>If L2Character is a
     * L2ArtefactInstance, send a Server->Client packet ActionFailed </li>
     * <li>Send a Server->Client packet MyTargetSelected to start attack and
     * Notify AI with AI_INTENTION_ATTACK </li><BR><BR>
     *
     * @param player The L2PcInstance to attack
     *
     */
    @Override
    public void onForcedAttack(L2PcInstance player) {
        if (player.isConfused()) {
            player.sendActionFailed();
            return;
        }

        if (this.isL2Artefact()) {
            // If L2Character is a L2ArtefactInstance, send a Server->Client packet ActionFailed
            player.sendActionFailed();
            return;
        }

        if (player.isSitting()) {
            player.sendActionFailed();
            return;
        }

        player.clearNextLoc();
        player.sendUserPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
        player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
    }

    /**
     * return true if this character is inside an active grid.
     */
    public Boolean isInActiveRegion() {
        L2WorldRegion region = getWorldRegion();
        return ((region != null) && (region.isActive()));
    }

    /**
     * Return True if the L2Character has a Party in progress.<BR><BR>
     */
    public boolean isInParty() {
        return false;
    }

    /**
     * Return the L2Party object of the L2Character.<BR><BR>
     */
    public L2Party getParty() {
        return null;
    }

    /**
     * Return True if the L2Character use a dual weapon.<BR><BR>
     */
    public boolean isUsingDualWeapon() {
        return false;
    }

    /**
     * Add a skill to the L2Character _skills and its Func objects to the
     * calculator set of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All skills own by a L2Character are
     * identified in <B>_skills</B><BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Replace oldSkill by newSkill or Add
     * the newSkill </li> <li>If an old skill has been replaced, remove all its
     * Func objects of L2Character calculator set</li> <li>Add Func objects of
     * newSkill to the calculator set of the L2Character </li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance : Save update in
     * the character_skills table of the database</li><BR><BR>
     *
     * @param newSkill The L2Skill to add to the L2Character
     *
     * @return The L2Skill replaced or null if just added a new L2Skill
     *
     */
    public L2Skill addSkill(L2Skill newSkill) {
        L2Skill oldSkill = null;

        if (newSkill != null) {
            // Replace oldSkill by newSkill or Add the newSkill
            oldSkill = _skills.put(newSkill.getId(), newSkill);

            // If an old skill has been replaced, remove all its Func objects
            if (oldSkill != null) {
                removeStatsOwner(oldSkill);
            }
            //stopSkillEffects(oldSkill.getId());

            // Add Func objects of newSkill to the calculator set of the L2Character
            addStatFuncs(newSkill.getStatFuncs(null, this));

            if (oldSkill != null && oldSkill.isChance() && _chanceSkills != null) {
                _chanceSkills.remove(oldSkill);
            }
            if (newSkill.isChance()) {
                addChanceSkill(newSkill);
            }
        }

        return oldSkill;
    }

    /**
     * Remove a skill from the L2Character and its Func objects from calculator
     * set of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All skills own by a L2Character are
     * identified in <B>_skills</B><BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Remove the skill from the
     * L2Character _skills </li> <li>Remove all its Func objects from the
     * L2Character calculator set</li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance : Save update in
     * the character_skills table of the database</li><BR><BR>
     *
     * @param skill The L2Skill to remove from the L2Character
     *
     * @return The L2Skill removed
     *
     */
    public L2Skill removeSkill(L2Skill skill) {
        if (skill == null) {
            return null;
        }

        // Remove the skill from the L2Character _skills
        L2Skill oldSkill = _skills.remove(skill.getId());

        // Remove all its Func objects from the L2Character calculator set
        if (oldSkill != null) {
            if (oldSkill.isChance() && _chanceSkills != null) {
                this.removeChanceSkill(oldSkill);
            } else {
                removeStatsOwner(oldSkill);
            }
        }
        return oldSkill;
    }

    /**
     * Return all skills own by the L2Character in a table of L2Skill.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All skills own by a L2Character are
     * identified in <B>_skills</B> the L2Character <BR><BR>
     *
     */
    public final L2Skill[] getAllSkills() {
        if (_skills == null) {
            return new L2Skill[0];
        }

        return _skills.values().toArray(new L2Skill[_skills.values().size()]);
    }
    // перезарядка шанс скиллов 60 секунд
    private long _lastTrigger = 0;

    public void setLastTrigger() {
        _lastTrigger = System.currentTimeMillis();
    }

    public ChanceSkillList getChanceSkills() {
        if (System.currentTimeMillis() - _lastTrigger < 60000) {
            return null;
        }

        return _chanceSkills;
    }

    /**
     * Return the level of a skill owned by the L2Character.<BR><BR>
     *
     * @param skillId The identifier of the L2Skill whose level must be returned
     *
     * @return The level of the L2Skill identified by skillId
     *
     */
    public int getSkillLevel(int skillId) {
        if (_skills == null) {
            return -1;
        }

        L2Skill skill = _skills.get(skillId);
        if (skill == null) {
            return -1;
        }

        return skill.getLevel();
    }

    /**
     * Return True if the skill is known by the L2Character.<BR><BR>
     *
     * @param skillId The identifier of the L2Skill to check the knowledge
     *
     */
    public final L2Skill getKnownSkill(int skillId) {
        if (_skills == null) {
            return null;
        }

        return _skills.get(skillId);
    }

    /**
     * Return the number of skills of type(Buff, Debuff, HEAL_PERCENT,
     * MANAHEAL_PERCENT) affecting this L2Character.<BR><BR>
     *
     * @return The number of Buffs affecting this L2Character
     */
    public int getBuffCount() {
        //return _effectList.getBuffCount();
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return 0;
        }

        int numBuffs = 0;
        L2Effect e = null;
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().isBuff() && !(e.getSkill().isSSBuff()) && !(e.getSkill().isLikeDebuff())) {
                numBuffs++;
            }
        }
        return numBuffs;
    }

    /**
     * Removes the first Buff of this L2Character.<BR><BR>
     *
     * @param preferSkill If != 0 the given skill Id will be removed instead of
     * first
     */
    public void removeFirstBuff(int preferSkill) {
        //_effectList.removeFirstBuff(preferSkill);
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return;
        }

        L2Effect e = null;
        L2Effect removeMe = null;
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().isBuff() && !e.getSkill().isSSBuff() && !e.getSkill().isMalariaBuff()) {
                if (preferSkill == 0) {
                    removeMe = e;
                    break;
                } else if (e.getSkill().getId() == preferSkill) {
                    removeMe = e;
                    break;
                } else if (removeMe == null) {
                    removeMe = e;
                }
            }
        }
        if (removeMe != null) {
            removeMe.exit();
        }
    }

    public int getDanceCount() {
        //return _effectList.getDanceCount();
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return 0;
        }

        L2Effect e = null;
        int danceCount = 0;
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }
            if (e.getSkill().isDance() && e.getInUse()) {
                danceCount++;
            }
        }
        return danceCount;
    }

    public int getAugmentCount() {
        //return _effectList.getAugmentCount();
        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            return 0;
        }

        L2Effect e = null;
        int augmentCount = 0;
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }
            if (e.getSkill().isAugment() && e.getInUse()) {
                augmentCount++;
            }
        }
        return augmentCount;
    }

    public boolean doesStack(L2Skill checkSkill) {
        if (_effects == null || _effects.isEmpty()
                || checkSkill._effectTemplates == null
                || checkSkill._effectTemplates.length < 1
                || checkSkill._effectTemplates[0].stackType == null) {
            return false;
        }

        String stackType = checkSkill._effectTemplates[0].stackType;
        if (stackType.equals("none")) {
            return false;
        }

        for (int i = 0; i < _effects.size(); i++) {
            if (_effects.get(i).getStackType() != null && _effects.get(i).getStackType().equals(stackType)) {
                return true;
            }
        }
        return false;
    }

    private List<L2Effect> effectQueueInsert(L2Effect newStackedEffect, List<L2Effect> stackQueue) {
        // Get the L2Effect corresponding to the Effect Identifier from the L2Character _effects
        if (_effects.isEmpty()) {
            return null;
        }

        // Create an Iterator to go through the list of stacked effects in progress on the L2Character
        Iterator<L2Effect> queueIterator = stackQueue.iterator();

        int i = 0;
        while (queueIterator.hasNext()) {
            L2Effect cur = queueIterator.next();
            if (newStackedEffect.getStackOrder() < cur.getStackOrder()) {
                i++;
            } else {
                break;
            }
        }

        // Add the new effect to the Stack list in function of its position in the Stack group
        stackQueue.add(i, newStackedEffect);

        // skill.exit() could be used, if the users don't wish to see "effect
        // removed" always when a timer goes off, even if the buff isn't active
        // any more (has been replaced). but then check e.g. npc hold and raid petrify.
        if (Config.EFFECT_CANCELING && !newStackedEffect.isHerbEffect() && stackQueue.size() > 1) {
            // only keep the current effect, cancel other effects
            for (int n = 0; n < _effects.size(); n++) {
                if (_effects.get(n) == stackQueue.get(1)) {
                    _effects.remove(n);
                    break;
                }
            }
            stackQueue.remove(1);
        }
        return stackQueue;
    }

    /**
     * Manage the magic skill launching task (MP, HP, Item consummation...) and
     * display the magic skill animation on client.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Send a Server->Client packet
     * MagicSkillLaunched (to display magic skill animation) to all L2PcInstance
     * of L2Charcater _knownPlayers</li> <li>Consumme MP, HP and Item if
     * necessary</li> <li>Send a Server->Client packet StatusUpdate with MP
     * modification to the L2PcInstance</li> <li>Launch the magic skill in order
     * to calculate its effects</li> <li>If the skill type is PDAM, notify the
     * AI of the target with AI_INTENTION_ATTACK</li> <li>Notify the AI of the
     * L2Character with EVT_FINISH_CASTING</li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : A magic skill casting MUST BE in
     * progress</B></FONT><BR><BR>
     *
     * @param skill The L2Skill to use
     *
     */
    public void onMagicLaunchedTimer(FastList<L2Object> targets, L2Skill skill, int coolTime, boolean instant) {
        //System.out.println("######5 "+targets.toString());
        if (skill == null || targets == null || targets.isEmpty()) {
            _skillCast = null;
            enableAllSkills();
            getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
            return;
        }

        // Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
        int escapeRange = 0;
        if (skill.getEffectRange() > escapeRange) {
            escapeRange = skill.getEffectRange();
        } else if (skill.getCastRange() < 0 && skill.getSkillRadius() > 80) {
            escapeRange = skill.getSkillRadius();
        }

        if (escapeRange > 0 && skill.getTargetType() != SkillTargetType.TARGET_SIGNET) {
            L2Object target = null;
            PeaceZone _peace = PeaceZone.getInstance();
            FastList<L2Character> targetList = new FastList<L2Character>();
            for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
                target = n.getValue();
                if (target != null && target.isL2Character()) {
                    if (!Util.checkIfInRange(escapeRange, this, target, true)) {
                        continue;
                    }

                    if (skill.isOffensive() && !(target.isMonster() || (target.isL2Npc()))) {
                        //System.out.println("######6");
                        if (_peace.inPeace(this, target)) {
                            //((L2PcInstance)this).sendAdmResultMessage("##Peace zone. chk2");
                            continue;
                        }
                    }
                    targetList.add((L2Character) target);
                    //System.out.println("######7");
                }
            }
            if (targetList.isEmpty()) {
                //System.out.println("######?");
                if (skill.getId() != 347) {
                    abortCast();
                    return;
                }
                targets.clear();
                targets.add(this);
            } else {
                targets.clear();
                targets.addAll(targetList);
            }
        }

        //System.out.println("######8");
        // Ensure that a cast is in progress
        // Check if player is using fake death.
        // Potions can be used while faking death.
        if (!isCastingNow() || (isAlikeDead() && !skill.isPotion())) {
            _skillCast = null;
            enableAllSkills();

            getAI().notifyEvent(CtrlEvent.EVT_CANCEL);

            _castEndTime = 0;
            _castInterruptTime = 0;
            return;
        }

        // Get the display identifier of the skill
        int magicId = skill.getDisplayId();

        // Get the level of the skill
        int level = getSkillLevel(skill.getId());
        if (level < 1) {
            level = 1;
        }

        // Send a Server->Client packet MagicSkillLaunched to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
        if (!skill.isPotion()) {
            /*
             * MagicSkillLaunched msl = new MagicSkillLaunched(this, magicId,
             * level, targets); broadcastPacket(msl); msl.clear();
             */
            broadcastPacket(new MagicSkillLaunched(this, magicId, level, targets));
            //MagicSkillLaunched msl = new MagicSkillLaunched(this, magicId, level, targets);
            //broadcastPacket(msl);
        }

        if (instant) {
            onMagicHitTimer(targets, skill, coolTime, true, 0);
        } else {
            _skillCast = EffectTaskManager.getInstance().schedule(new MagicUseTask(targets, skill, coolTime, 2, 0), 200);
        }
        //System.out.println("######8"+targets.toString());
    }

    /*
     * Runs in the end of skill casting
     */
    public void onMagicHitTimer(FastList<L2Object> targets, L2Skill skill, int coolTime, boolean instant, int hitTime) {
        //System.out.println("######9"+targets.toString() + " dd" + skill);
        if (skill == null || targets == null || targets.isEmpty()) {
            abortCast();
            return;
        }
        //System.out.println("######11");

        switch (skill.getTargetType()) {
            case TARGET_SIGNET_GROUND:
            case TARGET_SIGNET:
                break;
            default: {
                if (targets == null || targets.isEmpty()) {
                    _skillCast = null;
                    enableAllSkills();
                    getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
                    return;
                }
            }
        }
        boolean forceBuff = skill.getSkillType() == SkillType.FORCE_BUFF;
        // For force buff skills, start the effect as long as the player is casting.
        if (forceBuff) {
            startForceBuff((L2Character) targets.getFirst(), skill);
        }
        //System.out.println("######12");
        /*
         * try {
         */
        // Go through targets table
        L2Object tgt = null;
        for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
            tgt = n.getValue();
            if (tgt == null) {
                continue;
            }

            if (tgt.isL2Playable()) {
                L2Character target = (L2Character) tgt;
                if (skill.getSkillType() != SkillType.BUFF
                        || skill.getSkillType() != SkillType.MANAHEAL
                        || skill.getSkillType() != SkillType.RESURRECT
                        || skill.getSkillType() != SkillType.RECALL
                        || skill.getSkillType() != SkillType.DOT
                        || (isInParty() && getParty().getPartyMembers().contains(target))
                        || (getClan() == target.getClan())) {
                    continue;
                }

                if (skill.getSkillType() == L2Skill.SkillType.BUFF) {
                    target.sendUserPacket(SystemMessage.id(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill.getDisplayId()));
                }

                if (isPlayer() && target.isL2Summon()) {
                    target.updateAndBroadcastStatus(1);
                }
                if (isPlayer() && target.isPartner()) {
                    target.updateAndBroadcastPartnerStatus(1);
                }
            }

            if (isPlayer() && tgt.isL2Monster() && skill.getSkillType() != L2Skill.SkillType.SUMMON && skill.getSkillType() != L2Skill.SkillType.BEAST_FEED && !skill.isOffensive()) {
                updatePvPStatus();
            }
        }

        //StatusUpdate su = new StatusUpdate(getObjectId());
        //boolean isSendStatus = false;
        // Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        if (getStat().getMpConsume(skill) > 0) {
            getStatus().reduceMp(calcStat(Stats.MP_CONSUME_RATE, getStat().getMpConsume(skill), null, null));
            // su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
            //isSendStatus = true;
        }

        // Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        if (skill.getHpConsume() > 0) {
            setCurrentHp(getCurrentHp() - skill.getHpConsume());
            //double consumeHp;

            // consumeHp = calcStat(Stats.HP_CONSUME_RATE,skill.getHpConsume(),null,null);
            // if(consumeHp+1 >= getCurrentHp())
            //	consumeHp = getCurrentHp()-1.0;
            //getStatus().reduceHp(consumeHp, this);
            //su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
            //isSendStatus = true;
        }

        // Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance
        //if (isSendStatus) sendPacket(su);
        // Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
        if (skill.getItemConsume() > 0) {
            if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, false)) {
                sendUserPacket(Static.NOT_ENOUGH_ITEMS);
                abortCast();
                return;
            }
        }

        // Launch the magic skill in order to calculate its effects
        if (!forceBuff) {
            callSkill(skill, targets);
        }
        /*
         * }
         * catch (NullPointerException e) { e.printStackTrace(); }
         */

        //System.out.println("######13");
        if (skill.getInitialEffectDelay() > 0) {
            _skillCast = EffectTaskManager.getInstance().schedule(new MagicUseTask(targets, skill, coolTime, 3, 0), hitTime);
        } else if (instant || coolTime == 0) {
            /*
             * try {
             */
            onMagicFinalizer(skill, targets.getFirst());
            /*
             * }
             * catch (Exception e) { // }
             */
        } else {
            _skillCast = EffectTaskManager.getInstance().schedule(new MagicUseTask(targets, skill, coolTime, 3, 0), coolTime);
        }

        if (skill.isNoShot()) {
            return;
        }

        //Recharge AutoShots
        if (skill.useSpiritShot()) {
            rechargeAutoSoulShot(false, true, isL2Summon());
        } else {
            rechargeAutoSoulShot(true, false, isL2Summon());
        }

        /*
         * if (this.isPlayer()) if (((L2PcInstance)this).isFantome())
         * ((L2PcInstance)this).broadcastPacket(new MagicSkillUser(this, this,
         * 2163, 1, 1, 0));
         */
    }

    /*
     * Runs after skill hitTime+coolTime
     */
    public void onMagicFinalizer(L2Skill skill, L2Object target) {
        _skillCast = null;
        _castEndTime = 0;
        _castInterruptTime = 0;
        enableAllSkills();

        if (getForceBuff() != null) {
            getForceBuff().delete();
        }

        L2Effect mog = getFirstEffect(L2Effect.EffectType.SIGNET_GROUND);
        if (mog != null) {
            mog.exit();
        }

        correctHeadingWhenCast();

        if (Config.AUTO_SKILL_ATTACK) {
            if (continueAttack(skill, getTarget(), target)) {
                getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
            }

            if (skill.isOffensive() && skill.isNotUnlock())// && !(type == SkillType.BLOW))
            {
                getAI().clientStartAutoAttack();

                if (getPartner() != null) {
                    getPartner().getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
                }
            }
        }

        // Notify the AI of the L2Character with EVT_FINISH_CASTING
        getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);

        sheduleQueuedSkill(getQueuedSkill());
    }

    private void sheduleQueuedSkill(SkillDat queuedSkill) {
        if (!isPlayer()) {
            return;
        }

        setCurrentSkill(null, false, false);
        if (queuedSkill == null) {
            return;
        }

        setQueuedSkill(null, false, false);
        ThreadPoolManager.getInstance().scheduleAi(new QueuedMagicUseTask(getPlayer(), queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()), 1, true);
    }

    private boolean continueAttack(L2Skill skill, L2Object target, L2Object current) {
        if (!skill.isContinueAttack()) {
            return false;
        }

        if (target == null) {
            return false;
        }

        if (!target.isL2Character() || target.equals(this)) {
            return false;
        }

        return current.equals(target);
    }

    /**
     * Reduce the item number of the L2Character.<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance</li><BR><BR>
     *
     */
    public void consumeItem(int itemConsumeId, int itemCount) {
    }

    /**
     * Enable a skill (remove it from _disabledSkills of the
     * L2Character).<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All skills disabled are identified by
     * their skillId in <B>_disabledSkills</B> of the L2Character <BR><BR>
     *
     * @param skillId The identifier of the L2Skill to enable
     *
     */
    public void enableSkill(int skillId) {
        if (_disabledSkills.isEmpty()) {
            return;
        }

        _disabledSkills.remove(skillId);
        //removeTimeStamp(skillId);
    }

    /**
     * Disable this skill id for the duration of the delay in milliseconds.
     *
     * @param skillId
     * @param delay (seconds * 1000)
     */
    public void disableSkill(int id, int delay) {
        //addTimeStamp(id, delay);
        _disabledSkills.put(id, new DisabledSkill(delay));
        //sendSkillCoolTime(true);
    }

    public void disableSkill(int id, int delay, int remain) {
        //addTimeStamp(id, delay);
        _disabledSkills.put(id, new DisabledSkill(delay, remain));
        //sendSkillCoolTime(true);
    }

    /**
     * Check if a skill is disabled.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> All skills disabled are identified by
     * their skillId in <B>_disabledSkills</B> of the L2Character <BR><BR>
     *
     * @param skillId The identifier of the L2Skill to disable
     *
     */
    public final boolean isSkillDisabled(int id) {
        if (isAllSkillsDisabled()) {
            return true;
        }

        return isSkillReusing(_disabledSkills.get(id));
    }

    private boolean isSkillReusing(DisabledSkill ds) {
        if (ds == null) {
            return false;
        }

        return ds.isInReuse();
    }

    public final boolean isSkillDisabled(int id, boolean augment) {
        if (isAllSkillsDisabled()) {
            return true;
        }

        if (augment) {
            return isAugmentReusing(_disabledSkills.get(id));
        }

        return isSkillReusing(_disabledSkills.get(id));
    }

    private boolean isAugmentReusing(DisabledSkill ds) {
        if (ds == null) {
            return false;
        }

        return ds.isAugmentInReuse();
    }

    public Map<Integer, DisabledSkill> getDisabledSkills() {
        return _disabledSkills;
    }

    public void clearDisabledSkills() {
        _disabledSkills.clear();
    }

    /**
     * Disable all skills (set _allSkillsDisabled to True).<BR><BR>
     */
    public void disableAllSkills() {
        //if (Config.DEBUG) _log.fine("all skills disabled");
        _allSkillsDisabled = true;
    }

    /**
     * Enable all skills (set _allSkillsDisabled to False).<BR><BR>
     */
    public void enableAllSkills() {
        //if (Config.DEBUG) _log.fine("all skills enabled");
        _allSkillsDisabled = false;
    }

    /**
     * Launch the magic skill and calculate its effects on each target contained
     * in the targets table.<BR><BR>
     *
     * @param skill The L2Skill to use
     * @param targets The table of L2Object targets
     *
     */
    public void callSkill(L2Skill skill, FastList<L2Object> targets) {
        if (skill == null || targets == null) {
            return;
        }

        if (skill.isAoeOffensive() && skill.getId() != 347) {
            targets.remove(this);
        }

        //System.out.println("###?");
        try {
            //System.out.println("###?2");
            // Get the skill handler corresponding to the skill type (PDAM, MDAM, SWEEP...) started in gameserver
            ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
            L2Weapon activeWeapon = getActiveWeaponItem();
            // Check if the toggle skill effects are already in progress on the L2Character
            /*
             * if(skill.isToggle() && getFirstEffect(skill.getId()) != null)
             * return;
             */

            //System.out.println("###3");
            // Initial checks
            L2Object trg = null;
            for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
                //System.out.println("###4");
                trg = n.getValue();
                if (trg == null) {
                    continue;
                }
                //System.out.println("###5");

                if (trg.isL2Character()) {
                    // Set some values inside target's instance for later use
                    L2Character target = (L2Character) trg;
                    if (target.isInvul() && skill.isSkillTypeOffensive()) {
                        sendMessage("Цель не воспреимчива к повреждениям");
                        continue;
                    }

                    // Check Raidboss attack and
                    // check buffing chars who attack raidboss. Results in mute.
                    //L2Character targetsAttackTarget = target.getAI().getAttackTarget();
                    //L2Character targetsCastTarget = target.getAI().getCastTarget();
                    if (Config.ALLOW_RAID_BOSS_PUT && target.isRaid() && skill.isSkillTypeOffensive() && (getLevel() > target.getLevel() + 8)) {
                        if (skill.isMagic()) {
                            SkillTable.getInstance().getInfo(4215, 1).getEffects(this, this);
                        } else {
                            SkillTable.getInstance().getInfo(4515, 1).getEffects(this, this);
                        }
                        continue;
                    }

                    // Check if over-hit is possible
                    if (skill.isOverhit()) {
                        if (target.isL2Attackable()) {
                            ((L2Attackable) target).overhitEnabled(true);
                        }
                    }

                    // Launch weapon Special ability skill effect if available
                    if (activeWeapon != null && !target.isDead()) {
                        if (activeWeapon.getSkillEffects(this, target, skill).length > 0 && this.isPlayer()) {
                            sendUserPacket(Static.SA_BUFFED_OK);
                        }
                    }

                    // Maybe launch chance skills on us
                    if (getChanceSkills() != null) {
                        if (Rnd.get(100) < 15) {
                            getChanceSkills().onSkillHit(target, false, skill.isMagic(), skill.isOffensive());
                        }
                    }

                    // Maybe launch chance skills on target
                    if (target.getChanceSkills() != null) {
                        if (Rnd.get(100) < 15) {
                            target.getChanceSkills().onSkillHit(this, true, skill.isMagic(), skill.isOffensive());
                        }
                    }

                    if (target.isPlayer() && skill.isSkillTypeOffensive()) {
                        boolean haveBuff = false;

                        //mirage	 
                        if (target.getFirstEffect(445) != null) {
                            if (Rnd.get(100) < Config.MIRAGE_CHANCE) {
                                getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                                setTarget(null);
                                abortAttack();
                                abortCast();
                                haveBuff = true;
                            }
                        }

                        //invocation
                        if (target.isImmobileUntilAttacked()) {
                            target.stopImmobileUntilAttacked(null);
                        }

                        if (Config.ALLOW_APELLA_BONUSES) {
                            //apella - root
                            if (target.getSkillLevel(3608) > 0) {
                                if ((target.getFirstEffect(4202) == null) && (Rnd.get(100) < 15)) {
                                    SkillTable.getInstance().getInfo(4202, 12).getEffects(this, this);
                                    haveBuff = true;
                                }
                            }
                            //apella - RunSpd
                            if (target.getSkillLevel(3609) > 0) {
                                if ((target.getFirstEffect(4200) == null) && (Rnd.get(100) < 15)) {
                                    SkillTable.getInstance().getInfo(4200, 12).getEffects(this, this);
                                    haveBuff = true;
                                }
                            }
                            //apella - AtkSpd
                            if (target.getSkillLevel(3610) > 0) {
                                if ((target.getFirstEffect(4203) == null) && (Rnd.get(100) < 15)) {
                                    SkillTable.getInstance().getInfo(4203, 12).getEffects(this, this);
                                    haveBuff = true;
                                }
                            }
                        }
                        if (haveBuff) {
                            this.broadcastPacket(new MagicSkillUser(this, this, 5144, 1, 0, 0));
                        }
                    }
                }
            }
            //System.out.println("###6");

            // Launch the magic skill and calculate its effects
            if (handler != null) {
                handler.useSkill(this, skill, targets);
            } else {
                skill.useSkill(this, targets);
            }
            //System.out.println("###7");

            if ((this.isPlayer()) || (this.isL2Summon())) {
                L2PcInstance player = getPlayer();

                /*
                 * if (player.isInsideAqZone()) { if (player.getLevel() > 46) {
                 * this.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                 * this.abortAttack(); this.abortCast();
                 *
                 * if (skill.isMagic()) { L2Skill tempSkill =
                 * SkillTable.getInstance().getInfo(4215, 1); if(tempSkill !=
                 * null) tempSkill.getEffects(this, this); else
                 * _log.warning("Skill 4215 at level 1 is missing in DP."); }
                 * else { L2Skill tempSkill =
                 * SkillTable.getInstance().getInfo(4515, 1); if(tempSkill !=
                 * null) tempSkill.getEffects(this, this); else
                 * _log.warning("Skill 4515 at level 1 is missing in DP."); } }
                 * }
                 */
                L2Object target = null;
                for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
                    target = n.getValue();
                    if (target == null) {
                        continue;
                    }

                    if (target.isL2Character()) {
                        if (skill.isOffensive()) {
                            if (target.isPlayer() || target.isL2Summon()) {
                                if (target == player) {
                                    continue;
                                }

                                if (skill.getSkillType() != SkillType.AGGREDUCE && skill.getSkillType() != SkillType.AGGREDUCE_CHAR && skill.getSkillType() != SkillType.AGGREMOVE) {
                                    ((L2Character) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player);
                                }

                                if (target.isPlayer()) {
                                    target.getPlayer().getAI().clientStartAutoAttack();
                                } else if (target.isL2Summon()) {
                                    if (target.getOwner() != null) {
                                        target.getOwner().getAI().clientStartAutoAttack();
                                    }
                                }
                                if (!(target.isL2Summon()) || player.getPet() != target) {
                                    player.updatePvPStatus((L2Character) target);
                                }
                            } else if (target.isL2Attackable()) {
                                if (skill.getSkillType() != SkillType.AGGREDUCE && skill.getSkillType() != SkillType.AGGREDUCE_CHAR && skill.getSkillType() != SkillType.AGGREMOVE) {
                                    ((L2Character) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player);
                                }
                            }
                        } else if (target.isPlayer()) {
                            // Casting non offensive skill on player with pvp flag set or with karma
                            if (!target.equals(this) && (target.getPvpFlag() > 0 || target.getKarma() > 0)) {
                                player.updatePvPStatus();
                            }
                        } else if (target.isL2Attackable() && !(skill.getSkillType() == SkillType.SUMMON) && !(skill.getSkillType() == SkillType.BEAST_FEED)
                                && !(skill.getSkillType() == SkillType.UNLOCK) && !(skill.getSkillType() == SkillType.DELUXE_KEY_UNLOCK) && (!(target.isL2Summon()) || player.getPet() != target)) {
                            player.updatePvPStatus((L2Character) target);
                        }
                        target.notifySkillUse(player, skill);
                    } else if (target.isL2Npc()) {
                        L2NpcInstance npc = (L2NpcInstance) target;
                        if (npc.getTemplate().getEventQuests(Quest.QuestEventType.MOB_TARGETED_BY_SKILL) != null) {
                            for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.MOB_TARGETED_BY_SKILL)) {
                                quest.notifySkillUse(npc, player, skill);
                            }
                        }
                    }
                }
                if (skill.getAggroPoints() > 0) {
                    for (L2Object spMob : player.getKnownList().getKnownObjects().values()) {
                        if (spMob.isL2Npc()) {
                            L2NpcInstance npcMob = (L2NpcInstance) spMob;
                            if (npcMob.isInsideRadius(player, 1000, true, true) && npcMob.hasAI()
                                    && npcMob.getAI().getIntention() == AI_INTENTION_ATTACK) {
                                L2Object npcTarget = npcMob.getTarget();
                                for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
                                    target = n.getValue();
                                    if (target == null) {
                                        continue;
                                    }
                                    if (npcTarget == target || npcMob == target) {
                                        npcMob.seeSpell(player, target, skill);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (skill.isAoeOffensive() && skill.getId() != 347) {
                targets.add(this);
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "L2Character [ERROR] can't cast skillId: " + skill.getId(), e);
            e.printStackTrace();
        }
    }

    public void seeSpell(L2PcInstance caster, L2Object target, L2Skill skill) {
        //if (this.isL2Attackable())
        //	((L2Attackable)this).addDamageHate(caster, 0, -skill.getAggroPoints());

        addDamageHate(caster, 0, -skill.getAggroPoints());
    }

    public void addDamage(L2Character attacker, int damage) {
        //
    }

    public void addDamageHate(L2Character attacker, int damage, int aggro) {
        //
    }

    /**
     * Return True if the L2Character is behind the target and can't be
     * seen.<BR><BR>
     */
    public boolean isBehind(L2Object target) {
        double angleChar, angleTarget, angleDiff, maxAngleDiff = 45;

        if (target == null) {
            return false;
        }

        if (target.isL2Character()) {
            L2Character target1 = (L2Character) target;
            angleChar = Util.calculateAngleFrom(target1, this);
            angleTarget = Util.convertHeadingToDegree(target1.getHeading());
            angleDiff = angleChar - angleTarget;
            if (angleDiff <= -360 + maxAngleDiff) {
                angleDiff += 360;
            }
            if (angleDiff >= 360 - maxAngleDiff) {
                angleDiff -= 360;
            }
            if (Math.abs(angleDiff) <= maxAngleDiff) {
                return true;
            }
        }
        return false;
    }

    public boolean isBehindTarget() {
        return isBehind(getTarget());
    }

    /**
     * Return True if the L2Character is behind the target and can't be
     * seen.<BR><BR>
     */
    public boolean isFront(L2Object target) {
        double angleChar, angleTarget, angleDiff, maxAngleDiff = 45;

        if (target == null) {
            return false;
        }

        if (target.isL2Character()) {
            L2Character target1 = (L2Character) target;
            angleChar = Util.calculateAngleFrom(target1, this);
            angleTarget = Util.convertHeadingToDegree(target1.getHeading());
            angleDiff = angleChar - angleTarget;
            if (angleDiff <= -180 + maxAngleDiff) {
                angleDiff += 180;
            }
            if (angleDiff >= 180 - maxAngleDiff) {
                angleDiff -= 180;
            }
            if (Math.abs(angleDiff) <= maxAngleDiff) {
                return true;
            }
        }
        return false;
    }

    public boolean isFrontTarget() {
        return isFront(getTarget());
    }

    /**
     * Return 1.<BR><BR>
     */
    public double getLevelMod() {
        return 1;
    }

    public final void setSkillCast(Future<?> newSkillCast) {
        _skillCast = newSkillCast;
    }

    public final void setSkillCastEndTime(int newSkillCastEndTime) {
        _castEndTime = newSkillCastEndTime;
        // for interrupt -12 ticks; first removing the extra second and then -200 ms
        _castInterruptTime = newSkillCastEndTime - 12;
    }
    private Future<?> _PvPRegTask;
    private long _pvpFlagLasts;

    public void setPvpFlagLasts(long time) {
        _pvpFlagLasts = time;
    }

    public long getPvpFlagLasts() {
        return _pvpFlagLasts;
    }

    public void startPvPFlag() {
        updatePvPFlag(1);

        _PvPRegTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PvPFlag(), 1000, 1000);
    }

    public void stopPvpRegTask() {
        if (_PvPRegTask != null) {
            _PvPRegTask.cancel(true);
        }
    }

    public void stopPvPFlag() {
        stopPvpRegTask();

        updatePvPFlag(0);

        _PvPRegTask = null;
    }

    public void updatePvPFlag(int value) {
        if (!(this.isPlayer())) {
            return;
        }
        L2PcInstance player = getPlayer();
        /*
         * if (player.getPvpFlag() == value) return;
         */

        player.setPvpFlag(value);
        player.sendUserPacket(new UserInfo(player));

        FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
        L2PcInstance pc = null;
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }

            pc.sendPacket(new RelationChanged(player, player.getRelation(player), player.isAutoAttackable(pc)));
        }
        players.clear();
        players = null;
        pc = null;
    }

//	public void checkPvPFlag()
//	{
//	if (Config.DEBUG) _log.fine("Checking PvpFlag");
//	_PvPRegTask = ThreadPoolManager.getInstance().scheduleLowAtFixedRate(
//	new PvPFlag(), 1000, 5000);
//	_PvPRegActive = true;
//	//  _log.fine("PvP recheck");
//	}
//
    /**
     * Return a Random Damage in function of the weapon.<BR><BR>
     */
    public final int getRandomDamage(L2Character target) {
        L2Weapon weaponItem = getActiveWeaponItem();

        if (weaponItem == null) {
            return 5 + (int) Math.sqrt(getLevel());
        }

        return weaponItem.getRandomDamage();
    }

    @Override
    public String toString() {
        return "mob " + getObjectId();
    }

    public long getAttackEndTime() {
        return _attackEndTime;
    }

    /**
     * Not Implemented.<BR><BR>
     */
    // =========================================================
    // =========================================================
    // Stat - NEED TO REMOVE ONCE L2CHARSTAT IS COMPLETE
    // Property - Public
    public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill) {
        return getStat().calcStat(stat, init, target, skill);
    }

    public final double calcStat(Stats stat, double init) {
        return getStat().calcStat(stat, init, null, null);
    }

    // Property - Public
    public int getAccuracy() {
        return getStat().getAccuracy();
    }

    public final float getAttackSpeedMultiplier() {
        return getStat().getAttackSpeedMultiplier();
    }

    public int getCON() {
        return getStat().getCON();
    }

    public int getDEX() {
        return getStat().getDEX();
    }

    public final double getCriticalDmg(L2Character target, double init) {
        return getStat().getCriticalDmg(target, init);
    }

    public int getCriticalHit(L2Character target, L2Skill skill) {
        return getStat().getCriticalHit(target, skill);
    }

    public int getEvasionRate(L2Character target) {
        return getStat().getEvasionRate(target);
    }

    public int getINT() {
        return getStat().getINT();
    }

    public final int getMagicalAttackRange(L2Skill skill) {
        return getStat().getMagicalAttackRange(skill);
    }

    public final int getMaxCp() {
        return getStat().getMaxCp();
    }

    public int getMAtk(L2Character target, L2Skill skill) {
        return getStat().getMAtk(target, skill);
    }

    public int getMAtkSpd() {
        /*
         * int mSpd = getStat().getMAtkSpd(); if(mSpd > Config.MAX_MATK_SPEED)
         * mSpd = Config.MAX_MATK_SPEED; return mSpd;
         */
        return Math.min(getStat().getMAtkSpd(), Config.MAX_MATK_SPEED);
    }

    public int getMaxMp() {
        return getStat().getMaxMp();
    }

    @Override
    public int getMaxHp() {
        return getStat().getMaxHp();
    }

    public final int getMCriticalHit(L2Character target, L2Skill skill) {
        return getStat().getMCriticalHit(target, skill);
    }

    public int getMDef(L2Character target, L2Skill skill) {
        return getStat().getMDef(target, skill);
    }

    public int getMEN() {
        return getStat().getMEN();
    }

    public double getMReuseRate(L2Skill skill) {
        return getStat().getMReuseRate(skill);
    }

    public float getMovementSpeedMultiplier() {
        return getStat().getMovementSpeedMultiplier();
    }

    public int getPAtk(L2Character target) {
        return getStat().getPAtk(target);
    }

    public double getPAtkAnimals(L2Character target) {
        return getStat().getPAtkAnimals(target);
    }

    public double getPAtkDragons(L2Character target) {
        return getStat().getPAtkDragons(target);
    }

    public double getPAtkInsects(L2Character target) {
        return getStat().getPAtkInsects(target);
    }

    public double getPAtkMonsters(L2Character target) {
        return getStat().getPAtkMonsters(target);
    }

    public double getPAtkPlants(L2Character target) {
        return getStat().getPAtkPlants(target);
    }

    public int getPAtkSpd() {
        return Math.min(getStat().getPAtkSpd(), Config.MAX_PATK_SPEED);
    }

    public double getPAtkUndead(L2Character target) {
        return getStat().getPAtkUndead(target);
    }

    public double getPDefUndead(L2Character target) {
        return getStat().getPDefUndead(target);
    }

    public double getPAtkValakas(L2Character target) {
        return getStat().getPAtkValakas(target);
    }

    public double getPDefValakas(L2Character target) {
        return getStat().getPDefValakas(target);
    }

    public int getPDef(L2Character target) {
        return getStat().getPDef(target);
    }

    public final int getPhysicalAttackRange() {
        return getStat().getPhysicalAttackRange();
    }

    public int getRunSpeed() {
        return getStat().getRunSpeed();
    }

    public final int getShldDef() {
        return getStat().getShldDef();
    }

    public int getSTR() {
        return getStat().getSTR();
    }

    public final int getWalkSpeed() {
        return getStat().getWalkSpeed();
    }

    public int getWIT() {
        return getStat().getWIT();
    }

    public double getMAtk() {
        return ((double) getStat().getMAtk(null, null));
    }

    public double getMDef() {
        return ((double) getStat().getMDef(null, null));
    }
    // =========================================================

    // =========================================================
    // Status - NEED TO REMOVE ONCE L2CHARTATUS IS COMPLETE
    // Method - Public
    public void reduceCurrentHp(double i, L2Character attacker) {
        reduceCurrentHp(i, attacker, true);
    }

    public void reduceNpcHp(double i, L2Character attacker) {
        getStatus().reduceNpcHp(i, attacker, true);
    }

    public void reduceCurrentHp(double i, L2Character attacker, boolean awake) {
        if (Config.L2JMOD_CHAMPION_ENABLE && isChampion() && Config.L2JMOD_CHAMPION_HP != 0) {
            getStatus().reduceHp(i / Config.L2JMOD_CHAMPION_HP, attacker, awake);
        } else {
            getStatus().reduceHp(i, attacker, awake);
        }
        if (Config.L2JMOD_CHAMPION_ENABLE_BLUE && isChampion1() && Config.L2JMOD_CHAMPION_HP_BLUE != 0) {
            getStatus().reduceHp(i / Config.L2JMOD_CHAMPION_HP_BLUE, attacker, awake);
        } else {
            getStatus().reduceHp(i, attacker, awake);
        }
    }

    public void reduceCurrentMp(double i) {
        getStatus().reduceMp(i);
    }

    @Override
    public void addStatusListener(L2Character object) {
        getStatus().addStatusListener(object);
    }

    @Override
    public void removeStatusListener(L2Character object) {
        getStatus().removeStatusListener(object);
    }

    protected void stopHpMpRegeneration() {
        getStatus().stopHpMpRegeneration();
    }

    // Property - Public
    public final double getCurrentCp() {
        return getStatus().getCurrentCp();
    }

    public final void setCurrentCp(Double newCp) {
        setCurrentCp((double) newCp);
    }

    public final void setCurrentCp(double newCp) {
        getStatus().setCurrentCp(newCp);
    }

    @Override
    public final double getCurrentHp() {
        return getStatus().getCurrentHp();
    }

    public final void setCurrentHp(double newHp) {
        getStatus().setCurrentHp(newHp);
    }

    public final void setCurrentHpMp(double newHp, double newMp) {
        getStatus().setCurrentHpMp(newHp, newMp);
    }

    public final double getCurrentMp() {
        return getStatus().getCurrentMp();
    }

    public final void setCurrentMp(Double newMp) {
        setCurrentMp((double) newMp);
    }

    public final void setCurrentMp(double newMp) {
        getStatus().setCurrentMp(newMp);
    }
    // =========================================================

    public void setAiClass(String aiClass) {
        _aiClass = aiClass;
    }

    public String getAiClass() {
        return _aiClass;
    }

    public void setChampion(boolean champ) {
        _champion = champ;
    }

    public boolean isChampion() {
        return _champion;
    }

    public void setChampion1(boolean champ) {
        _champion1 = champ;
    }

    public boolean isChampion1() {

        return _champion1;
    }

    public int getLastHealAmount() {
        return _lastHealAmount;
    }

    public void setLastHealAmount(int hp) {
        _lastHealAmount = hp;
    }

    /**
     * Check if character reflected skill
     *
     * @param skill
     * @return
     */
    public boolean reflectSkill(L2Skill skill) {
        double reflect = calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, null);
        return (Rnd.get(100) < reflect);
    }

    /**
     * Send system message about damage.<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2PcInstance <li>
     * L2SummonInstance <li> L2PetInstance</li><BR><BR>
     *
     */
    public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
    }

    public ForceBuff getForceBuff() {
        return null;
    }

    public final void addChanceSkill(L2Skill skill) {
        if (_chanceSkills == null) {
            _chanceSkills = new ChanceSkillList(this);
        }
        _chanceSkills.put(skill, skill.getChanceCondition());
    }

    public final void removeChanceSkill(L2Skill skill) {
        _chanceSkills.remove(skill);
        if (_chanceSkills.size() == 0) {
            _chanceSkills = null;
        }
    }
    private boolean _vis = true;

    public final void setVis(boolean f) {
        _vis = f;
        L2PcInstance pc = null;
        FastList<L2PcInstance> players = getKnownList().getKnownPlayersInRadius(1200);
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.sendPacket(new NpcInfo((L2NpcInstance) this, pc));
        }
        players.clear();
        players = null;
        pc = null;
    }

    public boolean isVis() {
        return _vis;
    }
    // зарядки
    private int _numCharges = 0;

    public void increaseCharges() {
        _numCharges++;

        if (isPlayer()) {
            sendEtcStatusUpdate();
            if (_numCharges == 7) {
                sendUserPacket(Static.FORCE_MAXIMUM);
            } else {
                sendUserPacket(SystemMessage.id(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(_numCharges));
            }
        }
    }

    public void decreaseCharges(int decrease) {
        _numCharges -= decrease;
        sendEtcStatusUpdate();
    }

    public void clearCharges() {
        _numCharges = 0;
        sendEtcStatusUpdate();
    }

    public int getCharges() {
        return _numCharges;
    }

    // дебафф инвул
    public boolean isDebuffImmun() {
        return (isRaid() || isInvul() || getFirstEffect(1411) != null || isCursedWeaponEquiped());
    }

    public boolean isDebuffImmun(L2Skill skill) {
        // уже висит
        if (getFirstEffect(skill.getId()) != null) {
            return true;
        }

        // висит Mystic Immunity
        if (getFirstEffect(1411) != null && skill.hasEffects()) {
            return true;
        }

        // защищенные от дебаффов
        if (skill.isDebuff() && isDebuffImmun()) {
            return true;
        }

        return false;
    }

    //статы чара
    public double calcMENModifier() {
        return Formulas.calcMENModifier(this);
    }

    public double calcCONModifier() {
        return Formulas.calcCONModifier(this);
    }

    public double calcSkillResistans(L2Skill skill, SkillType type, SkillType deftype) {
        return Formulas.calcSkillResistans(this, skill, type, deftype);
    }

    public double calcSkillVulnerability(L2Skill skill) {
        return Formulas.calcSkillVulnerability(this, skill);
    }

    /**
     * уменьшаем затраты на сравнение классов instanceof
     */
    //враг для моба?
    public boolean isEnemyForMob(L2Attackable mob) {
        return false;
    }

    public boolean isInWater() {
        return isInsideZone(ZONE_WATER);
    }

    public boolean isInDerbyTrack() {
        return isInsideZone(ZONE_MONSTERTRACK);
    }

    @Override
    public boolean isInsidePvpZone() {
        return isInsideZone(ZONE_PVP);
    }

    public boolean isInsideNoLandZone() {
        return isInsideZone(ZONE_NOLANDING);
    }
    public static final double HEADINGS_IN_PI = 10430.378350470452724949566316381;

    public int calcHeading(final int x_dest, final int y_dest) {
        return (int) (Math.atan2(getY() - y_dest, getX() - x_dest) * HEADINGS_IN_PI) + 32768;
    }

    public boolean isSitting() {
        return false;
    }

    public boolean isUnlockable() {
        return false;
    }

    public boolean isTyranosurus() {
        return false;
    }

    public boolean isAngel() {
        return false;
    }
    private long _lastRestore = 0;

    public void fullRestore() {
        fullRestore(true);
    }

    public void fullRestore(boolean self) {
        if (self) {
            if (System.currentTimeMillis() - _lastRestore < 5000) {
                sNotReady();
                return;
            }
            _lastRestore = System.currentTimeMillis();
        }

        broadcastPacket(new MagicSkillUser(this, this, 2241, 1, 1000, 0));
        setCurrentHpMp(getMaxHp(), getMaxMp());
        setCurrentCp(getMaxCp());

        //sendMessage("Полное восстановление");
        sendUserPacket(Static.FULL_RESTORE);
    }
    private long _lastStop = 0;

    public void stopAllEffectsB() {
        stopAllEffectsB(true);
    }

    public void stopAllEffectsB(boolean self) {
        if (self) {
            if (System.currentTimeMillis() - _lastStop < 5000) {
                sNotReady();
                return;
            }
            _lastStop = System.currentTimeMillis();
        }
        broadcastPacket(new MagicSkillUser(this, this, 2243, 1, 1000, 0));
        stopAllEffects();

        //sendMessage("Снятие баффов");
        sendUserPacket(Static.BUFFS_CANCEL);
    }
    private long _lastRebuff = 0;

    public void doRebuff() {
        doRebuff(true);
    }

    public void doRebuff(boolean self) {
        if (self) {
            if (System.currentTimeMillis() - _lastRebuff < 5000) {
                sNotReady();
                return;
            }
            _lastRebuff = System.currentTimeMillis();
        }

        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            sendUserPacket(Static.OOPS_ERROR);
            return;
        }
        stopAllEffects();

        SkillTable _st = SkillTable.getInstance();
        broadcastPacket(new MagicSkillUser(this, this, 2242, 1, 1000, 0));

        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().isForbiddenProfileSkill() || e.getSkill().getSkillType() != SkillType.BUFF || e.getSkill().isChance()) {
                continue;
            }

            //stopSkillEffects(e.getSkill().getId());
            _st.getInfo(e.getSkill().getId(), e.getSkill().getLevel()).getEffects(this, this);
        }
        //sendMessage("Обновление баффов");
        sendUserPacket(Static.BUFFS_UPDATE);
    }
    private long _fullRebuff = 0;

    public void doFullBuff(int type) {
        if (System.currentTimeMillis() - _fullRebuff < 5000) {
            sNotReady();
            return;
        }
        _fullRebuff = System.currentTimeMillis();

        FastMap<Integer, Integer> buffs = null;
        switch (type) {
            case 1:
                if (Config.F_BUFF.isEmpty()) {
                    return;
                }
                buffs = Config.F_BUFF;
                break;
            case 2:
                if (Config.M_BUFF.isEmpty()) {
                    return;
                }
                buffs = Config.M_BUFF;
                break;
        }

        if (Config.BUFF_CANCEL) {
            stopAllEffects();
        }

        SkillTable _st = SkillTable.getInstance();
        for (FastMap.Entry<Integer, Integer> e = buffs.head(), end = buffs.tail(); (e = e.getNext()) != end;) {
            Integer id = e.getKey(); // No typecast necessary.
            Integer lvl = e.getValue(); // No typecast necessary.
            if (id == null || lvl == null) {
                continue;
            }

            _st.getInfo(id, lvl).getEffects(this, this);
        }
        //broadcastPacket(new MagicSkillUser(this, this, 1013, 1, 1, 0));
    }

    public void sNotReady() {
        sendUserPacket(Static.PLEASE_WAIT);//sendMessage("Еще не готово. Раз в 5 секунд.");
        sendActionFailed();
    }

    public boolean hasClanWarWith(L2Character cha) {
        if (getClan() == null || cha.getClan() == null) {
            return false;
        }

        if (isAcademyMember() || cha.isAcademyMember()) {
            return false;
        }

        return (getClan().isAtWarWith(cha.getClan()) && cha.getClan().isAtWarWith(getClan()));
    }

    public boolean canExp() {
        return !isDead();
    }

    /**
     **	уменьшаем затраты для каста ((L2PcInstance) L2Character.cha)
     *
     */
    public L2Armor getActiveChestArmorItem() {
        return null;
    }

    public boolean isWearingHeavyArmor() {
        return false;
    }

    public boolean isWearingLightArmor() {
        return false;
    }

    public boolean isWearingMagicArmor() {
        return false;
    }

    public boolean isCursedWeaponEquiped() {
        return false;
    }

    public void setPVPArena(boolean f) {
    }

    public void startWaterTask(int waterZone) {
        //
    }

    public void stopWaterTask(int waterZone) {
        //
    }

    public void rechargeAutoSoulShot(boolean a, boolean b, boolean c) {
        //
    }

    public boolean geoPathfind() {
        if (isAfraid()) {
            return false;
        }

        return (Config.GEODATA == 2);
    }

    public boolean getShowSkillChances() {
        return false;
    }

    public void sendMessage(String txt) {
        //
    }

    public void setInCastleZone(boolean f) {
        //
    }

    public int getClanId() {
        return 0;
    }

    public int getClanCrestId() {
        return 0;
    }

    public void setInSiegeFlagArea(boolean f) {
        //
    }

    public void setInSiegeRuleArea(boolean f) {
        //
    }

    public void setInNoLogoutArea(boolean f) {
        //
    }

    public int getRelation(L2PcInstance target) {
        return 0;
    }

    public boolean isFestivalParticipant() {
        return false;
    }

    public boolean isMounted() {
        return false;
    }

    public ClassId getClassId() {
        return null;
    }

    public void setEventWait(boolean f) {
        //
    }

    public void doNpcChat(int type, String name) {
        //
    }

    public void setInDino(boolean f) {
        //
    }

    public void sendAdmResultMessage(String txt) {
        //
    }

    public void setInPvpFarmZone(boolean f) {
        //
    }

    public void setInPvpRewardZone(int id) {
        //
    }

    public void sendEtcStatusUpdate() {
        //
    }

    public void updateAndBroadcastStatus(int broadcastType) {
        //
    }

    public void updatePvPStatus() {
        //
    }

    public void updatePvPStatus(L2Character target) {
        //
    }

    public boolean inObserverMode() {
        return false;
    }

    public void setChargedSoulShot(int shotType) {
        //
    }

    public int getChargedSoulShot() {
        return 0;
    }

    public int getChargedSpiritShot() {
        return 0;
    }

    public void broadcastUserInfo() {
        //
    }

    @Override
    public Duel getDuel() {
        return null;
    }

    public void setRecentFakeDeath(boolean f) {
        //
    }

    public void revalidateZone(boolean f) {
        //
    }

    public void logout() {
        //
    }

    public L2Clan getClan() {
        return null;
    }

    public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed) {
        //
    }

    public SkillDat getQueuedSkill() {
        return null;
    }

    public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed) {
        //
    }

    public boolean isEventMob() {
        return false;
    }

    public void setChannel(int channel) {
        //
    }

    public boolean isMageClass() {
        return false;
    }

    public boolean isReviveRequested() {
        return false;
    }

    public boolean isRevivingPet() {
        return false;
    }

    public boolean isGM() {
        return false;
    }

    public boolean isReturningToSpawnPoint() {
        return false;
    }

    public boolean isAcademyMember() {
        return false;
    }

    public void setFollowStatus(boolean state) {
        //
    }

    /**
     * formulas
     */
    public double calcMDefMod(double value) {
        return value;
    }

    public double calcPDefMod(double value) {
        return value;
    }

    public double calcAtkAccuracy(double value) {
        return value;
    }

    public double calcAtkCritical(double value, double dex) {
        value *= dex * 10;
        return value;
    }

    public double calcMAtkCritical(double value, double wit) {
        return (value * wit);
    }

    public double calcBlowDamageMul() {
        return 1;
    }
    //Return the period between 2 regenerations task (3s for L2Character, 5 min for L2DoorInstance).<BR><BR>

    public int getRegeneratePeriod() {
        return 3000; // 3s
    }
    //Return the Henna modifier of this L2PcInstance.<BR><BR>

    public int getHennaStatINT() {
        return 0;
    }

    public int getHennaStatSTR() {
        return 0;
    }

    public int getHennaStatCON() {
        return 0;
    }

    public int getHennaStatMEN() {
        return 0;
    }

    public int getHennaStatWIT() {
        return 0;
    }

    public int getHennaStatDEX() {
        return 0;
    }

    public boolean isOverlord() // как же вы заебали, у овера активейтрейт 40, какой нахуй шанс выше может быть?
    {
        return false;
    }

    public boolean isSilentMoving() {
        return false;
    }

    @Override
    public L2PcInstance getPlayer() {
        return null;
    }

    @Override
    public boolean isL2Character() {
        return true;
    }

    @Override
    public L2Character getL2Character() {
        return this;
    }

    public void onKillUpdatePvPKarma(L2Character target) {
        //
    }

    public void updateLastTeleport(boolean f) {
        //
    }

    public boolean isNoblesseBlessed() {
        return false;
    }

    public void stopNoblesseBlessing(L2Effect effect) {
        //
    }

    public boolean getCharmOfLuck() {
        return false;
    }

    public void stopCharmOfLuck(L2Effect effect) {
        //
    }

    public boolean isPhoenixBlessed() {
        return false;
    }

    public boolean isFantome() {
        return false;
    }

    public void rndWalk() {
        int posX = getX();
        int posY = getY();
        int posZ = getZ();
        switch (Rnd.get(1, 6)) {
            case 1:
                posX += 40;
                posY += 180;
                break;
            case 2:
                posX += 150;
                posY += 50;
                break;
            case 3:
                posX += 69;
                posY -= 100;
                break;
            case 4:
                posX += 10;
                posY -= 100;
                break;
            case 5:
                posX -= 150;
                posY -= 20;
                break;
            case 6:
                posX -= 100;
                posY += 60;
                break;
        }

        setRunning();
        getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, this.calcHeading(posX, posY)));
    }

    public boolean rndWalk(L2Character target, boolean fake) {
        return false;
    }

    public void clearRndWalk() {
        //
    }

    public void teleToClosestTown() {
        //
    }

    public void sayString(String text, int type) {
        broadcastPacket(new CreatureSay(getObjectId(), type, getName(), text));
    }

    public Location getFakeLoc() {
        return null;
    }

    public boolean getFollowStatus() {
        return false;
    }

    public L2Summon getL2Summon() {
        return null;
    }

    public L2PcInstance getPartner() {
        return null;
    }

    public boolean isPartner() {
        return false;
    }

    public void updateAndBroadcastPartnerStatus(int val) {
        //
    }

    public int getPartnerClass() {
        return 0;
    }

    public boolean teleToLocation(Location loc) {
        return false;
    }

    public void setInAqZone(boolean f) {
        //
    }

    public void setInsideSilenceZone(boolean f) {
        //
    }

    public void setInHotZone(boolean f) {
        //
    }

    public int isOnline() {
        return 0;
    }

    public void checkHpMessages(double curHp, double newHp) {
        //
    }

    public boolean isInDuel() {
        return false;
    }

    public void refreshSavedStats() {
        //
    }

    public void sendChanges() {
        //
    }

    public void setHippy(boolean hippy) {
        //
    }

    public boolean isHippy() {
        return false;
    }

    public boolean isHero() {
        return false;
    }

    public boolean hasItems(FastList<Integer> items) {
        return false;
    }

    public PcInventory getPcInventory() {
        return null;
    }

    public void setFreePvp(boolean f) {
        //
    }

    public L2Attackable getL2Attackable() {
        return null;
    }

    public void stopHating(L2Character target) {
        //
    }

    public L2Spawn getSpawn() {
        return null;
    }

    public String getFactionId() {
        return null;
    }

    public boolean equalsFactionId(String fact) {
        return false;
    }

    public boolean isMinion() {
        return false;
    }

    public L2MonsterInstance getLeader() {
        return null;
    }

    public void setInColiseum(boolean b) {
        //
    }

    public boolean isRealPlayer() {
        return false;
    }

    public boolean isBlockingBuffs() {
        return false;
    }

    public void sendSkillCoolTime() {
        //
    }

    public void sendSkillCoolTime(boolean force) {
        //
    }

    public void setFreeArena(boolean force) {
        //
    }

    public boolean isInFreeArena() {
        return false;
    }

    public void setInFishZone(boolean free) {
        //
    }

    public boolean isInFishZone() {
        return false;
    }

    public boolean hasElement(int element) {
        return false;
    }

    public int getElement() {
        return 0;
    }

    public boolean isSiegeRaidGuard() {
        return false;
    }

    public int getWantsPeace() {
        return 0;
    }

    public void setBuffing(boolean f) {
        //
    }

    public boolean isL2Teleporter() {
        return false;
    }

    public void setSaveBuff(boolean f) {
        //
    }

    public void stopPhoenixBlessing(L2Effect effect) {
        //
    }

    public boolean checkForbiddenItems() {
        return false;
    }

    public boolean hasFarmPenalty() {
        return false;
    }

    public L2GuardInstance getGuard() {
        return null;
    }

    public long getLastPvpPk() {
        return 0;
    }

    public void removeTarget() {
        setTarget(null);
        abortAttack();
        abortCast();
    }

    public boolean isInEventChannel() {
        return false;
    }

    public void increasePvpKills(int count) {
    }

    public boolean isPremium() {
        return false;
    }

    public void setSummonPenalty(boolean f) {
        //
    }

    public void setFordItems(FastList<Integer> items) {
        //
    }

    public void setFreePk(boolean force) {
        //
    }

    public boolean isInFreePk() {
        return false;
    }

    public void enableTvtReward() {

    }

    public void useAutoItem(int itemId) {
    }

    public boolean hasAutoMp() {
        return false;
    }

    public boolean hasAutoHp() {
        return false;
    }

    public boolean hasAutoCp() {
        return false;
    }

    public void setLastHit() {
        //
    }
}
