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
package net.sf.l2j.gameserver.model.actor.instance;

import java.text.DateFormat;
import java.util.List;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.*;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.TownManager;
import net.sf.l2j.gameserver.instancemanager.games.Lottery;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.knownlist.NpcKnownList;
import net.sf.l2j.gameserver.model.actor.stat.NpcStat;
import net.sf.l2j.gameserver.model.actor.status.NpcStatus;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2HelperBuff;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.Rnd;
import scripts.zone.type.L2TownZone;

/**
 * This class represents a Non-Player-Character in the world. It can be a
 * monster or a friendly character. It also uses a template to fetch some static
 * values. The templates are hardcoded in the client, so we can rely on
 * them.<BR><BR>
 *
 * L2Character :<BR><BR> <li>L2Attackable</li> <li>L2BoxInstance</li>
 * <li>L2FolkInstance</li>
 *
 * @version $Revision: 1.32.2.7.2.24 $ $Date: 2005/04/11 10:06:09 $
 */
public class L2NpcInstance extends L2Character {
    //private static Logger _log = Logger.getLogger(L2NpcInstance.class.getName());

    /**
     * The interaction distance of the L2NpcInstance(is used as offset in
     * MovetoLocation method)
     */
    public static final int INTERACTION_DISTANCE = 150;
    /**
     * The L2Spawn object that manage this L2NpcInstance
     */
    private L2Spawn _spawn;
    /**
     * The flag to specify if this L2NpcInstance is busy
     */
    private boolean _isBusy = false;
    /**
     * The busy message for this L2NpcInstance
     */
    private String _busyMessage = "";
    /**
     * True if endDecayTask has already been called
     */
    volatile boolean _isDecayed = false;
    /**
     * True if a Dwarf has used Spoil on this L2NpcInstance
     */
    private boolean _isSpoil = false;
    /**
     * The castle index in the array of L2Castle this L2NpcInstance belongs to
     */
    private int _castleIndex = -2;
    public boolean isEventMob = false;
    private boolean _isInTown = false;
    private int _isSpoiledBy = 0;
    protected RandomAnimationTask _rAniTask = null;
    private int _currentLHandId;  // normally this shouldn't change from the template, but there exist exceptions
    private int _currentRHandId;  // normally this shouldn't change from the template, but there exist exceptions
    private int _currentCollisionHeight; // used for npc grow effect skills
    private int _currentCollisionRadius; // used for npc grow effect skills
    private int _weaponEnch = 0;
    private static final FastList<EventReward> _raidRewards = Config.NPC_RAID_REWARDS;
    //private static final FastList<EventReward> _epicRewards = Config.NPC_EPIC_REWARDS;

    /**
     * Task launching the function onRandomAnimation()
     */
    protected class RandomAnimationTask implements Runnable {

        public void run() {
            try {
                if (this != _rAniTask) {
                    return; // Shouldn't happen, but who knows... just to make sure every active npc has only one timer.
                }
                if (isMob()) {
                    // Cancel further animation timers until intention is changed to ACTIVE again.
                    if (getAI().getIntention() != AI_INTENTION_ACTIVE) {
                        return;
                    }
                } else {
                    if (!isInActiveRegion()) // NPCs in inactive region don't run this task
                    {
                        return;
                    }
                    // update knownlist to remove playable which aren't in range any more
                    getKnownList().updateKnownObjects();
                }

                if (!(isDead() || isStunned() || isSleeping() || isParalyzed())) {
                    onRandomAnimation();
                }

                startRandomAnimationTimer();
            } catch (Throwable t) {
            }
        }
    }

    /**
     * Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of
     * the L2NpcInstance and create a new RandomAnimation Task.<BR><BR>
     */
    public void onRandomAnimation() {
        // Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
        broadcastPacket(new SocialAction(getObjectId(), Rnd.get(2, 3)));
    }

    /**
     * Create a RandomAnimation Task that will be launched after the calculated
     * delay.<BR><BR>
     */
    public void startRandomAnimationTimer() {
        /*
         * if (!hasRandomAnimation() || isRaid()) return;
         *
         * int minWait = isMob() ? Config.MIN_MONSTER_ANIMATION :
         * Config.MIN_NPC_ANIMATION; int maxWait = isMob() ?
         * Config.MAX_MONSTER_ANIMATION : Config.MAX_NPC_ANIMATION;
         *
         * // Calculate the delay before the next animation int interval =
         * Rnd.get(minWait, maxWait) * 1000;
         */
        // Create a RandomAnimation Task that will be launched after the calculated delay
        //_rAniTask = new RandomAnimationTask();
        //ThreadPoolManager.getInstance().scheduleGeneral(_rAniTask, interval);
    }

    /**
     * Check if the server allows Random Animation.<BR><BR>
     */
    public boolean hasRandomAnimation() {
        return (Config.MAX_NPC_ANIMATION > 0);
    }

    public static class DestroyTemporalNPC implements Runnable {

        private L2Spawn _oldSpawn;

        public DestroyTemporalNPC(L2Spawn spawn) {
            _oldSpawn = spawn;
        }

        public void run() {
            try {
                _oldSpawn.getLastSpawn().deleteMe();
                _oldSpawn.stopRespawn();
                SpawnTable.getInstance().deleteSpawn(_oldSpawn, false);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static class DestroyTemporalSummon implements Runnable {

        L2Summon _summon;
        L2PcInstance _player;

        public DestroyTemporalSummon(L2Summon summon, L2PcInstance player) {
            _summon = summon;
            _player = player;
        }

        public void run() {
            _summon.unSummon(_player);
        }
    }
    /**
     * Constructor of L2NpcInstance (use L2Character constructor).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Call the L2Character constructor to
     * set the _template of the L2Character (copy skills from template to object
     * and link _calculators to NPC_STD_CALCULATOR) </li> <li>Set the name of
     * the L2Character</li> <li>Create a RandomAnimation Task that will be
     * launched after the calculated delay if the server allow it </li><BR><BR>
     *
     * @param objectId Identifier of the object to initialized
     * @param template The L2NpcTemplate to apply to the NPC
     *
     */
    private L2NpcTemplate _template;

    public L2NpcInstance(int objectId, L2NpcTemplate template) {
        // Call the L2Character constructor to set the _template of the L2Character, copy skills from template to object
        // and link _calculators to NPC_STD_CALCULATOR
        super(objectId, template);
        getKnownList();   // init knownlist
        getStat();                        // init stats
        getStatus();              // init status
        initCharStatusUpdateValues();

        // initialize the "current" equipment
        _currentLHandId = template.lhand;
        _currentRHandId = template.rhand;
        // initialize the "current" collisions
        _currentCollisionHeight = template.collisionHeight;
        _currentCollisionRadius = template.collisionRadius;

        if (template == null) {
            _log.severe("No template for Npc. Please check your datapack is setup correctly.");
            return;
        }

        // Set the name of the L2Character
        setName(template.name);

        if (Config.ENCH_NPC_CAHNCE > 0 && Rnd.get(100) < Config.ENCH_NPC_CAHNCE) {
            _weaponEnch = Rnd.get(Config.ENCH_NPC_MINMAX.nick, Config.ENCH_NPC_MINMAX.title);
        }
    }

    @Override
    public NpcKnownList getKnownList() {
        if (super.getKnownList() == null || !(super.getKnownList() instanceof NpcKnownList)) {
            setKnownList(new NpcKnownList(this));
        }
        return (NpcKnownList) super.getKnownList();
    }

    @Override
    public NpcStat getStat() {
        if (super.getStat() == null || !(super.getStat() instanceof NpcStat)) {
            setStat(new NpcStat(this));
        }
        return (NpcStat) super.getStat();
    }

    @Override
    public NpcStatus getStatus() {
        if (super.getStatus() == null || !(super.getStatus() instanceof NpcStatus)) {
            setStatus(new NpcStatus(this));
        }
        return (NpcStatus) super.getStatus();
    }

    /**
     * Return the L2NpcTemplate of the L2NpcInstance.
     */
    @Override
    public final L2NpcTemplate getTemplate() {
        //return (L2NpcTemplate)super.getTemplate();
        if (_template == null) {
            _template = (L2NpcTemplate) super.getTemplate();
        }
        return _template;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    /**
     * Return the faction Identifier of this L2NpcInstance contained in the
     * L2NpcTemplate.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> If a NPC belows to a Faction, other NPC
     * of the faction inside the Faction range will help it if it's
     * attacked<BR><BR>
     *
     */
    @Override
    public final String getFactionId() {
        return getTemplate().factionId;
    }

    @Override
    public boolean equalsFactionId(String fact) {
        return fact.equalsIgnoreCase(getFactionId());
    }

    /**
     * Return the Level of this L2NpcInstance contained in the
     * L2NpcTemplate.<BR><BR>
     */
    @Override
    public final int getLevel() {
        return getTemplate().level;
    }

    /**
     * Return True if the L2NpcInstance is agressive (ex : L2MonsterInstance in
     * function of aggroRange).<BR><BR>
     */
    public boolean isAggressive() {
        return false;
    }

    /**
     * Return the Aggro Range of this L2NpcInstance contained in the
     * L2NpcTemplate.<BR><BR>
     */
    public int getAggroRange() {
        if (fromMonastry()) {
            return 500;
        }

        return getTemplate().aggroRange;
    }

    /**
     * Return the Faction Range of this L2NpcInstance contained in the
     * L2NpcTemplate.<BR><BR>
     */
    public int getFactionRange() {
        return getTemplate().factionRange;
    }

    /**
     * Return True if this L2NpcInstance is undead in function of the
     * L2NpcTemplate.<BR><BR>
     */
    @Override
    public boolean isUndead() {
        return getTemplate().isUndead;
    }

    /**
     * Send a packet NpcInfo with state of abnormal effect to all L2PcInstance
     * in the _KnownPlayers of the L2NpcInstance.<BR><BR>
     */
    @Override
    public void updateAbnormalEffect() {
        //NpcInfo info = new NpcInfo(this);
        //broadcastPacket(info);
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

    /**
     * Return the distance under which the object must be add to _knownObject in
     * function of the object type.<BR><BR>
     *
     * <B><U> Values </U> :</B><BR><BR> <li> object is a L2FolkInstance : 0
     * (don't remember it) </li> <li> object is a L2Character : 0 (don't
     * remember it) </li> <li> object is a L2PlayableInstance : 1500 </li> <li>
     * others : 500 </li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2Attackable</li><BR><BR>
     *
     * @param object The Object to add to _knownObject
     *
     */
    public int getDistanceToWatchObject(L2Object object) {
        if (object instanceof L2FestivalGuideInstance) {
            return 10000;
        }

        if (object.isL2Folk() || !(object.isL2Character())) {
            return 0;
        }

        if (object.isL2Playable()) {
            return 1500;
        }

        return 500;
    }

    /**
     * Return the distance after which the object must be remove from
     * _knownObject in function of the object type.<BR><BR>
     *
     * <B><U> Values </U> :</B><BR><BR> <li> object is not a L2Character : 0
     * (don't remember it) </li> <li> object is a L2FolkInstance : 0 (don't
     * remember it)</li> <li> object is a L2PlayableInstance : 3000 </li> <li>
     * others : 1000 </li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2Attackable</li><BR><BR>
     *
     * @param object The Object to remove from _knownObject
     *
     */
    public int getDistanceToForgetObject(L2Object object) {
        return 2 * getDistanceToWatchObject(object);
    }

    /**
     * Return False.<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2MonsterInstance : Check if
     * the attacker is not another L2MonsterInstance</li> <li>
     * L2PcInstance</li><BR><BR>
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return false;
    }

    /**
     * Return the Identifier of the item in the left hand of this L2NpcInstance
     * contained in the L2NpcTemplate.<BR><BR>
     */
    public int getLeftHandItem() {
        return _currentLHandId;
    }

    /**
     * Return the Identifier of the item in the right hand of this L2NpcInstance
     * contained in the L2NpcTemplate.<BR><BR>
     */
    public int getRightHandItem() {
        return _currentRHandId;
    }

    /**
     * Return True if this L2NpcInstance has drops that can be sweeped.<BR><BR>
     */
    @Override
    public boolean isSpoil() {
        return _isSpoil;
    }

    /**
     * Set the spoil state of this L2NpcInstance.<BR><BR>
     */
    public void setSpoil(boolean isSpoil) {
        _isSpoil = isSpoil;
    }

    @Override
    public final int getIsSpoiledBy() {
        return _isSpoiledBy;
    }

    public final void setIsSpoiledBy(int value) {
        _isSpoiledBy = value;
    }

    /**
     * Return the busy status of this L2NpcInstance.<BR><BR>
     */
    public final boolean isBusy() {
        return _isBusy;
    }

    /**
     * Set the busy status of this L2NpcInstance.<BR><BR>
     */
    public void setBusy(boolean isBusy) {
        _isBusy = isBusy;
    }

    /**
     * Return the busy message of this L2NpcInstance.<BR><BR>
     */
    public final String getBusyMessage() {
        return _busyMessage;
    }

    /**
     * Set the busy message of this L2NpcInstance.<BR><BR>
     */
    public void setBusyMessage(String message) {
        _busyMessage = message;
    }

    protected boolean canTarget(L2PcInstance player) {
        return canTarget(player, false);
    }

    protected boolean canTarget(L2PcInstance player, boolean checkZ) {
        if (player.isOutOfControl()) {
            player.sendActionFailed();
            return false;
        }

        if (checkZ && Math.abs(player.getZ() - getZ()) > 600) {
            player.sendActionFailed();
            return false;
        }
        /*
         * if (Math.abs(player.getZ() - getZ()) > 1200) {
         * player.sendActionFailed(); return false; }
         *
         * if (CustomServerData.getInstance().intersectEventZone(getX(), getY(),
         * getZ(), player.getX(), player.getY(), player.getZ())) {
         * player.sendActionFailed(); return false; }
         */

 /*
         * if (!player.canTarget(this)) { player.sendActionFailed(); return
         * false; }
         */

 /*
         * if (!player.canSeeTarget(this)) { player.sendActionFailed(); return
         * false; }
         */
        if (player.isDead() || player.isAlikeDead() || player.isFakeDeath()) {
            player.sendActionFailed();
            return false;
        }

        if (player.isSitting()) {
            player.sendActionFailed();
            return false;
        }

        if (getTemplate().npcId == 80008) {
            player.sendActionFailed();
            return false;
        }
        // TODO: More checks...
        return true;
    }

    protected boolean canInteract(L2PcInstance player) {
        // TODO: NPC busy check etc...

        if (!canTarget(player, true)) {
            return false;
        }

        return isInsideRadius(player, INTERACTION_DISTANCE, false, false);
    }

    /**
     * Manage actions when a player click on the L2NpcInstance.<BR><BR>
     *
     * <B><U> Actions on first click on the L2NpcInstance (Select it)</U>
     * :</B><BR><BR> <li>Set the L2NpcInstance as target of the L2PcInstance
     * player (if necessary)</li> <li>Send a Server->Client packet
     * MyTargetSelected to the L2PcInstance player (display the select
     * window)</li> <li>If L2NpcInstance is autoAttackable, send a
     * Server->Client packet StatusUpdate to the L2PcInstance in order to update
     * L2NpcInstance HP bar </li> <li>Send a Server->Client packet
     * ValidateLocation to correct the L2NpcInstance position and heading on the
     * client </li><BR><BR>
     *
     * <B><U> Actions on second click on the L2NpcInstance (Attack it/Intercat
     * with it)</U> :</B><BR><BR> <li>Send a Server->Client packet
     * MyTargetSelected to the L2PcInstance player (display the select
     * window)</li> <li>If L2NpcInstance is autoAttackable, notify the
     * L2PcInstance AI with AI_INTENTION_ATTACK (after a height
     * verification)</li> <li>If L2NpcInstance is NOT autoAttackable, notify the
     * L2PcInstance AI with AI_INTENTION_INTERACT (after a distance
     * verification) and show message</li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client
     * packet must be terminated by a ActionFailed packet in order to avoid that
     * client wait an other packet</B></FONT><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Client packet : Action,
     * AttackRequest</li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2ArtefactInstance : Manage
     * only fisrt click to select Artefact</li><BR><BR> <li> L2GuardInstance :
     * </li><BR><BR>
     *
     * @param player The L2PcInstance that start an action on the L2NpcInstance
     *
     */
    @Override
    public void onAction(L2PcInstance player) {

        // Check if the L2PcInstance already target the L2NpcInstance
        if (this != player.getTarget()) {
            //if (Config.DEBUG) _log.fine("new target selected:"+getObjectId());
            if (!canTarget(player)) {
                return;
            }

            // Set the target of the L2PcInstance player
            player.setTarget(this);

            // Check if the player is attackable (without a forced attack)
            if (isAutoAttackable(player)) {
                // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
                // The player.getLevel() - getLevel() permit to display the correct color in the select window
                player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

                // Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
                StatusUpdate su = new StatusUpdate(getObjectId());
                su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
                su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
                player.sendPacket(su);
            } else {
                // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
                player.sendPacket(new MyTargetSelected(getObjectId(), 0));
            }

            // Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
            if (getTemplate().getEventQuests(Quest.QuestEventType.ONFOCUS) != null) {
                for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ONFOCUS)) {
                    quest.notifyFocus(this, player);
                }
            }
        } else {
            if (!canTarget(player, true)) {
                return;
            }
            //player.sendPacket(new ValidateLocation(this));
            // Check if the player is attackable (without a forced attack) and isn't dead
            if (isAutoAttackable(player) && !isAlikeDead()) {
                // Check the height difference
                if (Math.abs(player.getZ() - getZ()) < 200) // this max heigth difference might need some tweaking
                {
                    // Set the L2PcInstance Intention to AI_INTENTION_ATTACK
                    player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
                    // player.startAttack(this);
                } else {
                    // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
                    player.sendActionFailed();
                }
            } else if (!isAutoAttackable(player)) {
                // Calculate the distance between the L2PcInstance and the L2NpcInstance
                if (!canInteract(player)) {
                    // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                    player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
                } else {
                    // Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance
                    // to display a social action of the L2NpcInstance on their client
                    if (getTemplate().showSocial) {
                        broadcastPacket(new SocialAction(getObjectId(), Rnd.get(8)));
                    }

                    // Open a chat window on client with the text of the L2NpcInstance
                    if (isEventMob) {
                        L2Event.showEventHtml(player, String.valueOf(getObjectId()));
                    } else {
                        Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.NPC_FIRST_TALK);
                        if ((qlst != null) && qlst.length == 1) {
                            qlst[0].notifyFirstTalk(this, player);
                        } else {
                            showChatWindow(player, 0);
                        }
                    }

                    if (Config.TALK_MOVE_DELAY) {
                        player.disableMove(1000);
                    }
                    player.sendActionFailed();
                }
            } else {
                player.sendActionFailed();
            }
        }
    }

    /**
     * Manage and Display the GM console to modify the L2NpcInstance (GM
     * only).<BR><BR>
     *
     * <B><U> Actions (If the L2PcInstance is a GM only)</U> :</B><BR><BR>
     * <li>Set the L2NpcInstance as target of the L2PcInstance player (if
     * necessary)</li> <li>Send a Server->Client packet MyTargetSelected to the
     * L2PcInstance player (display the select window)</li> <li>If L2NpcInstance
     * is autoAttackable, send a Server->Client packet StatusUpdate to the
     * L2PcInstance in order to update L2NpcInstance HP bar </li> <li>Send a
     * Server->Client NpcHtmlMessage() containing the GM console about this
     * L2NpcInstance </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client
     * packet must be terminated by a ActionFailed packet in order to avoid that
     * client wait an other packet</B></FONT><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Client packet :
     * Action</li><BR><BR>
     *
     * @param client The thread that manage the player that pessed Shift and
     * click on the L2NpcInstance
     *
     */
    @Override
    public void onActionShift(L2GameClient client) {
        // Get the L2PcInstance corresponding to the thread
        L2PcInstance player = client.getActiveChar();
        if (player == null) {
            return;
        }
        if (!canTarget(player, true)) {
            return;
        }

        // Check if the L2PcInstance is a GM
        if (player.getAccessLevel() >= Config.GM_ACCESSLEVEL) {
            // Set the target of the L2PcInstance player
            player.setTarget(this);
            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            // The player.getLevel() - getLevel() permit to display the correct color in the select window
            player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

            // Check if the player is attackable (without a forced attack)
            if (isAutoAttackable(player)) {
                // Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
                StatusUpdate su = new StatusUpdate(getObjectId());
                su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
                su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
                player.sendPacket(su);
            }

            // Send a Server->Client NpcHtmlMessage() containing the GM console about this L2NpcInstance
            NpcHtmlMessage html = NpcHtmlMessage.id(0);
            TextBuilder html1 = new TextBuilder("<html><body><center><font color=\"LEVEL\">NPC Information</font></center>");
            String className = "";//getName();
            try {
                className = getClass().getName().substring(43);
            } catch (Exception ignored) {
                try {
                    className = getClass().getName().substring(11);
                } catch (Exception ignored2) {
                    className = getName();
                }
            }
            html1.append("<br>");

            html1.append("Instance Type: " + className + "<br1>Faction: " + getFactionId() + "<br1>Location ID: " + (getSpawn() != null ? getSpawn().getLocation() : 0) + "<br1>");

            if (this instanceof L2ControllableMobInstance) {
                html1.append("Mob Group: " + MobGroupTable.getInstance().getGroupForMob((L2ControllableMobInstance) this).getGroupId() + "<br>");
            } else {
                html1.append("Respawn Time: " + (getSpawn() != null ? (getSpawn().getRespawnDelay() / 1000) + "  Seconds<br>" : "?  Seconds<br>"));
            }

            html1.append("<table border=\"0\" width=\"100%\">");
            html1.append("<tr><td>Object ID</td><td>" + getObjectId() + "</td><td>NPC ID</td><td>" + getTemplate().npcId + "</td></tr>");
            html1.append("<tr><td>Castle</td><td>" + getCastle().getCastleId() + "</td><td>Coords</td><td>" + getX() + "," + getY() + "," + getZ() + "</td></tr>");
            html1.append("<tr><td>Level</td><td>" + getLevel() + "</td><td>Aggro</td><td>" + ((this.isL2Attackable()) ? ((L2Attackable) this).getAggroRange() : 0) + "</td></tr>");
            html1.append("</table><br>");

            html1.append("<font color=\"LEVEL\">Combat</font>");
            html1.append("<table border=\"0\" width=\"100%\">");
            html1.append("<tr><td>Current HP</td><td>" + getCurrentHp() + "</td><td>Current MP</td><td>" + getCurrentMp() + "</td></tr>");
            html1.append("<tr><td>Max.HP</td><td>" + (int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null)) + "*" + getStat().calcStat(Stats.MAX_HP, 1, this, null) + "</td><td>Max.MP</td><td>" + getMaxMp() + "</td></tr>");
            html1.append("<tr><td>P.Atk.</td><td>" + getPAtk(null) + "</td><td>M.Atk.</td><td>" + getMAtk(null, null) + "</td></tr>");
            html1.append("<tr><td>P.Def.</td><td>" + getPDef(null) + "</td><td>M.Def.</td><td>" + getMDef(null, null) + "</td></tr>");
            html1.append("<tr><td>Accuracy</td><td>" + getAccuracy() + "</td><td>Evasion</td><td>" + getEvasionRate(null) + "</td></tr>");
            html1.append("<tr><td>Critical</td><td>" + getCriticalHit(null, null) + "</td><td>Speed</td><td>" + getRunSpeed() + "</td></tr>");
            html1.append("<tr><td>Atk.Speed</td><td>" + getPAtkSpd() + "</td><td>Cast.Speed</td><td>" + getMAtkSpd() + "</td></tr>");
            html1.append("</table><br>");

            html1.append("<font color=\"LEVEL\">Basic Stats</font>");
            html1.append("<table border=\"0\" width=\"100%\">");
            html1.append("<tr><td>STR</td><td>" + getSTR() + "</td><td>DEX</td><td>" + getDEX() + "</td><td>CON</td><td>" + getCON() + "</td></tr>");
            html1.append("<tr><td>INT</td><td>" + getINT() + "</td><td>WIT</td><td>" + getWIT() + "</td><td>MEN</td><td>" + getMEN() + "</td></tr>");
            html1.append("</table>");

            html1.append("<br><center><table><tr><td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc " + getTemplate().npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br1></td>");
            html1.append("<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><br1></tr>");
            html1.append("<tr><td><button value=\"Show DropList\" action=\"bypass -h admin_show_droplist " + getTemplate().npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            html1.append("<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
            html1.append("</table></center><br><br><button value=\"TeleTo\" action=\"bypass -h admin_tptoNpc " + getObjectId() + "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
            html1.append("</body></html>");

            html.setHtml(html1.toString());
            player.sendPacket(html);
        } else if (Config.ALT_GAME_VIEWNPC) {
            // Set the target of the L2PcInstance player
            player.setTarget(this);

            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            // The player.getLevel() - getLevel() permit to display the correct color in the select window
            player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

            // Check if the player is attackable (without a forced attack)
            if (isAutoAttackable(player)) {
                // Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
                StatusUpdate su = new StatusUpdate(getObjectId());
                su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
                su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
                player.sendPacket(su);
            }

            NpcHtmlMessage html = NpcHtmlMessage.id(0);
            TextBuilder html1 = new TextBuilder("<html><body>");

            html1.append("<br><center><font color=\"LEVEL\">[Combat Stats]</font></center>");
            html1.append("<table border=0 width=\"100%\">");
            html1.append("<tr><td>Max.HP</td><td>" + (int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null)) + "*" + (int) getStat().calcStat(Stats.MAX_HP, 1, this, null) + "</td><td>Max.MP</td><td>" + getMaxMp() + "</td></tr>");
            html1.append("<tr><td>P.Atk.</td><td>" + getPAtk(null) + "</td><td>M.Atk.</td><td>" + getMAtk(null, null) + "</td></tr>");
            html1.append("<tr><td>P.Def.</td><td>" + getPDef(null) + "</td><td>M.Def.</td><td>" + getMDef(null, null) + "</td></tr>");
            html1.append("<tr><td>Accuracy</td><td>" + getAccuracy() + "</td><td>Evasion</td><td>" + getEvasionRate(null) + "</td></tr>");
            html1.append("<tr><td>Critical</td><td>" + getCriticalHit(null, null) + "</td><td>Speed</td><td>" + getRunSpeed() + "</td></tr>");
            html1.append("<tr><td>Atk.Speed</td><td>" + getPAtkSpd() + "</td><td>Cast.Speed</td><td>" + getMAtkSpd() + "</td></tr>");
            html1.append("<tr><td>Race</td><td>" + getTemplate().race + "</td><td></td><td></td></tr>");
            html1.append("</table>");

            html1.append("<br><center><font color=\"LEVEL\">[Basic Stats]</font></center>");
            html1.append("<table border=0 width=\"100%\">");
            html1.append("<tr><td>STR</td><td>" + getSTR() + "</td><td>DEX</td><td>" + getDEX() + "</td><td>CON</td><td>" + getCON() + "</td></tr>");
            html1.append("<tr><td>INT</td><td>" + getINT() + "</td><td>WIT</td><td>" + getWIT() + "</td><td>MEN</td><td>" + getMEN() + "</td></tr>");
            html1.append("</table>");

            html1.append("<br><center><font color=\"LEVEL\">[Drop Info]</font></center>");
            html1.append("Rates legend: <font color=\"ff0000\">50%+</font> <font color=\"00ff00\">30%+</font> <font color=\"0000ff\">less than 30%</font>");
            html1.append("<table border=0 width=\"100%\">");

            for (L2DropCategory cat : getTemplate().getDropData()) {
                for (L2DropData drop : cat.getAllDrops()) {
                    String name = ItemTable.getInstance().getTemplate(drop.getItemId()).getName();

                    if (drop.getChance() >= 600000) {
                        html1.append("<tr><td><font color=\"ff0000\">" + name + "</font></td><td>" + (drop.isQuestDrop() ? "Quest" : (cat.isSweep() ? "Sweep" : "Drop")) + "</td></tr>");
                    } else if (drop.getChance() >= 300000) {
                        html1.append("<tr><td><font color=\"00ff00\">" + name + "</font></td><td>" + (drop.isQuestDrop() ? "Quest" : (cat.isSweep() ? "Sweep" : "Drop")) + "</td></tr>");
                    } else {
                        html1.append("<tr><td><font color=\"0000ff\">" + name + "</font></td><td>" + (drop.isQuestDrop() ? "Quest" : (cat.isSweep() ? "Sweep" : "Drop")) + "</td></tr>");
                    }
                }
            }

            html1.append("</table>");
            html1.append("</body></html>");

            html.setHtml(html1.toString());
            player.sendPacket(html);
        }

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendActionFailed();
    }

    /**
     * Return the L2Castle this L2NpcInstance belongs to.
     */
    public final Castle getCastle() {
        // Get castle this NPC belongs to (excluding L2Attackable)
        if (_castleIndex < 0) {
            L2TownZone town = TownManager.getInstance().getTown(getX(), getY(), getZ());

            if (town != null) {
                _castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());
            }

            if (_castleIndex < 0) {
                _castleIndex = CastleManager.getInstance().findNearestCastleIndex(this);
            } else {
                _isInTown = true; // Npc was spawned in town
            }
        }

        if (_castleIndex < 0) {
            return null;
        }

        return CastleManager.getInstance().getCastles().get(_castleIndex);
    }

    public final boolean getIsInTown() {
        if (_castleIndex < 0) {
            getCastle();
        }
        return _isInTown;
    }

    /**
     * Open a quest or chat window on client with the text of the L2NpcInstance
     * in function of the command.<BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Client packet :
     * RequestBypassToServer</li><BR><BR>
     *
     * @param command The command string received from client
     *
     */
    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (!canTarget(player, true)) {
            return;
        }

        //if (canInteract(player))
        {
            if (isBusy() && getBusyMessage().length() > 0) {
                player.sendActionFailed();

                NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
                html.setFile("data/html/npcbusy.htm");
                html.replace("%busymessage%", getBusyMessage());
                html.replace("%npcname%", getName());
                html.replace("%playername%", player.getName());
                player.sendPacket(html);
            } else if (command.equalsIgnoreCase("TerritoryStatus")) {
                NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
                {
                    if (getCastle().getOwnerId() > 0) {
                        html.setFile("data/html/territorystatus.htm");
                        L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
                        if (clan == null) {
                            player.sendHtmlMessage("Ошибка", "Сообщите администрации; код: " + getCastle().getOwnerId());
                            player.sendActionFailed();
                            return;
                        }
                        html.replace("%clanname%", Util.htmlSpecialChars(clan.getName()));
                        html.replace("%clanleadername%", Util.htmlSpecialChars(clan.getLeaderName()));
                    } else {
                        html.setFile("data/html/territorynoclan.htm");
                    }
                }
                html.replace("%castlename%", getCastle().getName());
                html.replace("%taxpercent%", "" + getCastle().getTaxPercent());
                html.replace("%objectId%", String.valueOf(getObjectId()));
                {
                    if (getCastle().getCastleId() > 6) {
                        html.replace("%territory%", "The Kingdom of Elmore");
                    } else {
                        html.replace("%territory%", "The Kingdom of Aden");
                    }
                }
                player.sendPacket(html);
            } else if (command.startsWith("Quest")) {
                String quest = "";
                try {
                    quest = command.substring(5).trim();
                } catch (IndexOutOfBoundsException ioobe) {
                }
                if (quest.length() == 0) {
                    showQuestWindow(player);
                } else {
                    showQuestWindow(player, quest);
                }
            } else if (command.startsWith("Chat")) {
                int val = 0;
                try {
                    val = Integer.parseInt(command.substring(5));
                } catch (IndexOutOfBoundsException ioobe) {
                } catch (NumberFormatException nfe) {
                }
                showChatWindow(player, val);
            } else if (command.startsWith("Link")) {
                String path = command.substring(5).trim();
                if (path.indexOf("..") != -1) {
                    return;
                }
                String filename = "data/html/" + path;
                NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
                html.setFile(filename);
                html.replace("%objectId%", String.valueOf(getObjectId()));
                player.sendPacket(html);
            } else if (command.startsWith("NobleTeleport")) {
                if (!player.isNoble()) {
                    String filename = "data/html/teleporter/nobleteleporter-no.htm";
                    NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
                    html.setFile(filename);
                    html.replace("%objectId%", String.valueOf(getObjectId()));
                    html.replace("%npcname%", getName());
                    player.sendPacket(html);
                    return;
                }
                int val = 0;
                try {
                    val = Integer.parseInt(command.substring(5));
                } catch (IndexOutOfBoundsException ioobe) {
                } catch (NumberFormatException nfe) {
                }
                showChatWindow(player, val);
            } else if (command.startsWith("Loto")) {
                int val = 0;
                try {
                    val = Integer.parseInt(command.substring(5));
                } catch (IndexOutOfBoundsException ioobe) {
                } catch (NumberFormatException nfe) {
                }
                if (val == 0) {
                    // new loto ticket
                    for (int i = 0; i < 5; i++) {
                        player.setLoto(i, 0);
                    }
                }
                showLotoWindow(player, val);
            } else if (command.startsWith("CPRecovery")) {
                makeCPRecovery(player);
            } else if (command.startsWith("SupportMagic")) {
                makeSupportMagic(player);
            } else if (command.startsWith("GiveBlessing")) {
                giveBlessingSupport(player);
            } else if (command.startsWith("multisell")) {
                L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(command.substring(9).trim()), player, false, getCastle().getTaxRate());
            } else if (command.startsWith("exc_multisell")) {
                L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(command.substring(13).trim()), player, true, getCastle().getTaxRate());
            } else if (command.startsWith("Augment")) {
                int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
                switch (cmdChoice) {
                    case 1:
                        player.sendPacket(Static.SELECT_THE_ITEM_TO_BE_AUGMENTED);
                        player.sendPacket(Static.ExShowVariationMakeWindow);
                        player.setAugFlag(true);
                        break;
                    case 2:
                        player.sendPacket(Static.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION);
                        player.sendPacket(Static.ExShowVariationCancelWindow);
                        break;
                }
            } else if (command.startsWith("npcfind_byid")) {
                try {
                    L2Spawn spawn = SpawnTable.getInstance().getTemplate(Integer.parseInt(command.substring(12).trim()));

                    if (spawn != null) {
                        player.sendPacket(new RadarControl(0, 1, spawn.getLocx(), spawn.getLocy(), spawn.getLocz()));
                    }
                } catch (NumberFormatException nfe) {
                    player.sendMessage("Wrong command parameters");
                }
            } else if (command.startsWith("EnterRift")) {
                try {
                    Byte b1 = Byte.parseByte(command.substring(10)); // Selected Area: Recruit, Soldier etc
                    DimensionalRiftManager.getInstance().start(player, b1, this);
                } catch (Exception e) {
                }
            } else if (command.startsWith("ChangeRiftRoom")) {
                if (player.isInParty() && player.getParty().isInDimensionalRift()) {
                    player.getParty().getDimensionalRift().manualTeleport(player, this);
                } else {
                    DimensionalRiftManager.getInstance().handleCheat(player, this);
                }
            } else if (command.startsWith("ExitRift")) {
                if (player.isInParty() && player.getParty().isInDimensionalRift()) {
                    player.getParty().getDimensionalRift().manualExitRift(player, this);
                } else {
                    DimensionalRiftManager.getInstance().handleCheat(player, this);
                }
            } else if (command.startsWith("Buff")) {
                if (getTemplate().npcId != Config.BUFFER_ID || player.ignoreBuffer()) {
                    return;
                }

                String[] opaopa = command.split(" ");
                int buff_type = Integer.parseInt(opaopa[1]);
                int buff_id = Integer.parseInt(opaopa[2]);
                int buff_level = Integer.parseInt(opaopa[3]);

                if (opaopa.length == 7 && !player.isPremium()) {
                    int coin_id = Integer.parseInt(opaopa[4]);
                    int coin_cnt = Math.max(Integer.parseInt(opaopa[5]), 1);
                    if (player.getItemCount(coin_id) < coin_cnt) {
                        player.sendHtmlMessage("Стоимость баффа: " + coin_cnt + " " + opaopa[6]);
                        return;
                    }
                    if (Integer.parseInt(opaopa[5]) >= 1) {
                        player.destroyItemByItemId("Buffer", coin_id, coin_cnt, this, true);
                    }
                }

                //player.stopSkillEffects(buff_id);
                //SkillTable.getInstance().getInfo(buff_id,buff_level).getEffects(player,player);
                addBuff(player.getBuffTarget(), buff_id, buff_level);
                if (buff_type == 0) {
                    showBufferWindow(player, "data/html/default/" + Config.BUFFER_ID + ".htm");
                } else {
                    showBufferWindow(player, "data/html/default/" + Config.BUFFER_ID + "-" + buff_type + ".htm");
                }
            } else if (command.startsWith("bDop")) {
                if (getTemplate().npcId != Config.BUFFER_ID || player.ignoreBuffer()) {
                    return;
                }

                player.setBuffing(true);
                int intdex = Integer.parseInt(command.substring(4).trim());
                switch (intdex) {
                    case 1:
                        player.getBuffTarget().stopAllEffectsB();
                        break;
                    case 2:
                        player.getBuffTarget().fullRestore();
                        break;
                    case 3:
                        player.getBuffTarget().doRebuff();
                        break;
                    case 4:
                        player.getBuffTarget().doFullBuff(1);
                        break;
                    case 5:
                        player.getBuffTarget().doFullBuff(2);
                        break;
                }
                player.setBuffing(false);
                player.updateEffectIcons();
                showBufferWindow(player, "data/html/default/" + Config.BUFFER_ID + ".htm");
            } else if (command.startsWith("profileBuff")) {
                if (getTemplate().npcId != Config.BUFFER_ID || player.ignoreBuffer()) {
                    return;
                }

                //int intdex = Integer.parseInt(command.substring(11).trim());
                player.setBuffing(true);
                player.doBuffProfile(Integer.parseInt(command.substring(11).trim()));
                player.setBuffing(false);
                player.updateEffectIcons();

                showBufferWindow(player, "data/html/default/" + Config.BUFFER_ID + ".htm");
            } else if (command.startsWith("sprofileBuff")) {
                if (getTemplate().npcId != Config.BUFFER_ID || player.ignoreBuffer()) {
                    return;
                }

                //int intdex = Integer.parseInt(command.substring(12).trim());
                player.saveBuffProfile(Integer.parseInt(command.substring(12).trim()));

                showBufferWindow(player, "data/html/default/" + Config.BUFFER_ID + "-4.htm");
            } else if (command.equalsIgnoreCase("changeBuffTarget")) {
                if (getTemplate().npcId != Config.BUFFER_ID || player.ignoreBuffer()) {
                    return;
                }

                if (player.getPet() == null || player.getBuffTarget() == player.getPet()) {
                    player.setBuffTarget(player);
                } else {
                    player.setBuffTarget(player.getPet());
                }

                showBufferWindow(player, "data/html/default/" + Config.BUFFER_ID + ".htm");
            } else if (command.equals("DrawSymbol")) {
                player.sendPacket(new HennaEquipList(player));
            } else if (command.equals("RemoveListSymbol")) {
                showRemoveChat(player);
            } else if (command.startsWith("RemoveSymbol ")) {
                player.removeHenna(Integer.parseInt(command.substring(13)));
            } else if (command.startsWith("premBuff")) {
                if (getTemplate().npcId != Config.BUFFER_ID || player.ignoreBuffer()) {
                    return;
                }

                String[] opaopa = command.split(" ");
                int buff_type = Integer.parseInt(opaopa[1]);
                int buff_id = Integer.parseInt(opaopa[2]);
                int buff_level = Integer.parseInt(opaopa[3]);

                if (!player.isPremium()) {
                    player.sendHtmlMessage("Доступно только для премиумов!");
                    return;
                }

                //player.stopSkillEffects(buff_id);
                //SkillTable.getInstance().getInfo(buff_id,buff_level).getEffects(player,player);
                addBuff(player.getBuffTarget(), buff_id, buff_level);
                if (buff_type == 0) {
                    showBufferWindow(player, "data/html/default/" + Config.BUFFER_ID + ".htm");
                } else {
                    showBufferWindow(player, "data/html/default/" + Config.BUFFER_ID + "-" + buff_type + ".htm");
                }
            }
        }
    }

    public void showRemoveChat(L2PcInstance player) {
        TextBuilder html1 = new TextBuilder("<html><body>");
        html1.append("Select symbol you would like to remove:<br><br>");
        boolean hasHennas = false;
        for (int i = 1; i <= 3; i++) {
            if (player.getHenna(i) == null) {
                continue;
            }
            hasHennas = true;
            html1.append("<a action=\"bypass -h npc_" + getObjectId() + "_RemoveSymbol " + i + "\">" + player.getHenna(i).getName() + "</a><br>");
        }

        if (!hasHennas) {
            html1.append("You don't have any symbol to remove!");
        }

        html1.append("</body></html>");
        insertObjectIdAndShowChatWindow(player, html1.toString());
    }

    private void showBufferWindow(L2PcInstance player, String htm) {
        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        reply.setFile(htm);
        reply.replace("%objectId%", String.valueOf(getObjectId()));

        if (player.getPet() == null) {
            reply.replace("%change_target%", "[Игрок]");
        } else if (player.getBuffTarget() == player.getPet()) {
            reply.replace("%change_target%", "[Пет] <button value=\"Игрок\" action=\"bypass -h npc_" + getObjectId() + "_changeBuffTarget\" width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
        } else {
            reply.replace("%change_target%", "<button value=\"Пет\" action=\"bypass -h npc_" + getObjectId() + "_changeBuffTarget\" width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> [Игрок]");
        }

        player.sendUserPacket(reply);
        player.sendActionFailed();
        reply = null;
    }

    /**
     * Return null (regular NPCs don't have weapons instancies).<BR><BR>
     */
    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        // regular NPCs dont have weapons instancies
        return null;
    }

    /**
     * Return the weapon item equiped in the right hand of the L2NpcInstance or
     * null.<BR><BR>
     */
    @Override
    public L2Weapon getActiveWeaponItem() {
        // Get the weapon identifier equiped in the right hand of the L2NpcInstance
        int weaponId = getTemplate().rhand;

        if (weaponId < 1) {
            return null;
        }

        // Get the weapon item equiped in the right hand of the L2NpcInstance
        L2Item item = ItemTable.getInstance().getTemplate(weaponId);
        if (item == null) {
            return null;
        }

        if (!(item instanceof L2Weapon)) {
            return null;
        }

        return (L2Weapon) item;
    }

    public void giveBlessingSupport(L2PcInstance player) {
        if (player == null) {
            return;
        }

        // Blessing of protection - author kerberos_20. Used codes from Rayan - L2Emu project.
        // Prevent a cursed weapon weilder of being buffed - I think no need of that becouse karma check > 0
        // if (player.isCursedWeaponEquiped()) 
        //   return; 
        int player_level = player.getLevel();
        // Select the player 
        setTarget(player);
        // If the player is too high level, display a message and return 
        if (player_level > 39 || player.getClassId().level() >= 2) {
            String content = "<html><body>Newbie Guide:<br>I'm sorry, but you are not eligible to receive the protection blessing.<br1>It can only be bestowed on <font color=\"LEVEL\">characters below level 39 who have not made a seccond transfer.</font></body></html>";
            insertObjectIdAndShowChatWindow(player, content);
            return;
        }
        L2Skill skill = SkillTable.getInstance().getInfo(5182, 1);
        doCast(skill);
    }

    /**
     * Return null (regular NPCs don't have weapons instancies).<BR><BR>
     */
    @Override
    public L2ItemInstance getSecondaryWeaponInstance() {
        // regular NPCs dont have weapons instancies
        return null;
    }

    /**
     * Return the weapon item equiped in the left hand of the L2NpcInstance or
     * null.<BR><BR>
     */
    @Override
    public L2Weapon getSecondaryWeaponItem() {
        // Get the weapon identifier equiped in the right hand of the L2NpcInstance
        int weaponId = getTemplate().lhand;

        if (weaponId < 1) {
            return null;
        }

        // Get the weapon item equiped in the right hand of the L2NpcInstance
        L2Item item = ItemTable.getInstance().getTemplate(getTemplate().lhand);

        if (!(item instanceof L2Weapon)) {
            return null;
        }

        return (L2Weapon) item;
    }

    /**
     * Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order
     * to display the message of the L2NpcInstance.<BR><BR>
     *
     * @param player The L2PcInstance who talks with the L2NpcInstance
     * @param content The text of the L2NpcMessage
     *
     */
    public void insertObjectIdAndShowChatWindow(L2PcInstance player, String content) {
        // Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
        content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));
        NpcHtmlMessage npcReply = NpcHtmlMessage.id(getObjectId());
        npcReply.setHtml(content);
        player.sendPacket(npcReply);
    }

    /**
     * Return the pathfile of the selected HTML file in function of the npcId
     * and of the page number.<BR><BR>
     *
     * <B><U> Format of the pathfile </U> :</B><BR><BR> <li> if the file exists
     * on the server (page number = 0) : <B>data/html/default/12006.htm</B>
     * (npcId-page number)</li> <li> if the file exists on the server (page
     * number > 0) : <B>data/html/default/12006-1.htm</B> (npcId-page
     * number)</li> <li> if the file doesn't exist on the server :
     * <B>data/html/npcdefault.htm</B> (message : "I have nothing to say to
     * you")</li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2GuardInstance : Set the
     * pathfile to data/html/guard/12006-1.htm (npcId-page number)</li><BR><BR>
     *
     * @param npcId The Identifier of the L2NpcInstance whose text must be
     * display
     * @param val The number of the page to display
     *
     */
    public String getHtmlPath(int npcId, int val) {
        String pom = "";

        if (val == 0) {
            pom = "" + npcId;
        } else {
            pom = npcId + "-" + val;
        }

        String temp = "data/html/default/" + pom + ".htm";

        if (!Config.LAZY_CACHE) {
            // If not running lazy cache the file must be in the cache or it doesnt exist
            if (HtmCache.getInstance().contains(temp)) {
                return temp;
            }
        } else if (HtmCache.getInstance().isLoadable(temp)) {
            return temp;
        }

        // If the file is not found, the standard message "I have nothing to say to you" is returned
        return "data/html/npcdefault.htm";
    }

    /**
     * Open a choose quest window on client with all quests available of the
     * L2NpcInstance.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Send a Server->Client NpcHtmlMessage
     * containing the text of the L2NpcInstance to the L2PcInstance
     * </li><BR><BR>
     *
     * @param player The L2PcInstance that talk with the L2NpcInstance
     * @param quests The table containing quests of the L2NpcInstance
     *
     */
    public void showQuestChooseWindow(L2PcInstance player, Quest[] quests) {
        TextBuilder sb = new TextBuilder();

        sb.append("<html><body><title>Talk about:</title><br>");

        for (Quest q : quests) {
            sb.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Quest ").append(q.getName()).append("\">").append(q.getDescr()).append("</a><br>");
        }

        sb.append("</body></html>");

        // Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
        insertObjectIdAndShowChatWindow(player, sb.toString());
    }

    /**
     * Open a quest window on client with the text of the L2NpcInstance.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Get the text of the quest state in
     * the folder data/jscript/quests/questId/stateId.htm </li> <li>Send a
     * Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to
     * the L2PcInstance </li> <li>Send a Server->Client ActionFailed to the
     * L2PcInstance in order to avoid that the client wait another packet
     * </li><BR><BR>
     *
     * @param player The L2PcInstance that talk with the L2NpcInstance
     * @param questId The Identifier of the quest to display the message
     *
     */
    public void showQuestWindow(L2PcInstance player, String questId) {
        String content;

        Quest q = QuestManager.getInstance().getQuest(questId);

        if (player.getWeightPenalty() >= 3 && q.getQuestIntId() >= 1 && q.getQuestIntId() < 1000) {
            player.sendPacket(Static.INVENTORY_LESS_THAN_80_PERCENT);
            return;
        }

        //FileInputStream fis = null;
        // Get the state of the selected quest
        QuestState qs = player.getQuestState(questId);

        if (qs != null) {
            // If the quest is alreday started, no need to show a window
            if (!qs.getQuest().notifyTalk(this, qs)) {
                return;
            }
        } else if (q != null) {
            // check for start point
            Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

            if (qlst != null && qlst.length > 0) {
                for (int i = 0; i < qlst.length; i++) {
                    if (qlst[i] == q) {
                        qs = q.newQuestState(player);
                        //disabled by mr. becouse quest dialog only show on second click.
                        //if(qs.getState().getName().equalsIgnoreCase("completed"))
                        //{
                        if (!qs.getQuest().notifyTalk(this, qs)) {
                            return; // no need to show a window
                        }                            //}
                        break;
                    }
                }
            }
        }

        if (qs == null) {
            // no quests found
            content = "<html><body>Для вас на данный момент у меня ничего нет.</body></html>";
        } else {
            questId = qs.getQuest().getName();
            String stateId = qs.getStateId();
            content = HtmCache.getInstance().getHtm("data/jscript/quests/" + questId + "/" + stateId + ".htm"); //TODO path for quests html
            if (content == null) {
                content = HtmCache.getInstance().getHtm("data/scripts/quests/" + questId + "/" + stateId + ".htm"); //TODO path for quests html
            }
        }

        // Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
        if (content != null) {
            insertObjectIdAndShowChatWindow(player, content);
        }

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendActionFailed();
    }

    /**
     * Collect awaiting quests/start points and display a QuestChooseWindow (if
     * several available) or QuestWindow.<BR><BR>
     *
     * @param player The L2PcInstance that talk with the L2NpcInstance
     *
     */
    public void showQuestWindow(L2PcInstance player) {
        // collect awaiting quests and start points
        List<Quest> options = new FastList<Quest>();

        QuestState[] awaits = player.getQuestsForTalk(getTemplate().npcId);
        Quest[] starts = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

        // Quests are limited between 1 and 999 because those are the quests that are supported by the client.  
        // By limitting them there, we are allowed to create custom quests at higher IDs without interfering  
        if (awaits != null) {
            for (QuestState x : awaits) {
                if (!options.contains(x)) {
                    if ((x.getQuest().getQuestIntId() > 0) && (x.getQuest().getQuestIntId() < 1000)) {
                        options.add(x.getQuest());
                    }
                }
            }
        }

        if (starts != null) {
            for (Quest x : starts) {
                if (!options.contains(x)) {
                    if ((x.getQuestIntId() > 0) && (x.getQuestIntId() < 1000)) {
                        options.add(x);
                    }
                }
            }
        }

        // Display a QuestChooseWindow (if several quests are available) or QuestWindow
        if (options.size() > 1) {
            showQuestChooseWindow(player, options.toArray(new Quest[options.size()]));
        } else if (options.size() == 1) {
            showQuestWindow(player, options.get(0).getName());
        } else {
            showQuestWindow(player, "");
        }
    }

    /**
     * Open a Loto window on client with the text of the L2NpcInstance.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Get the text of the selected HTML
     * file in function of the npcId and of the page number </li> <li>Send a
     * Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to
     * the L2PcInstance </li> <li>Send a Server->Client ActionFailed to the
     * L2PcInstance in order to avoid that the client wait another packet
     * </li><BR>
     *
     * @param player The L2PcInstance that talk with the L2NpcInstance
     * @param val The number of the page of the L2NpcInstance to display
     *
     */
    // 0 - first buy lottery ticket window
    // 1-20 - buttons
    // 21 - second buy lottery ticket window
    // 22 - selected ticket with 5 numbers
    // 23 - current lottery jackpot
    // 24 - Previous winning numbers/Prize claim
    // >24 - check lottery ticket by item object id
    public void showLotoWindow(L2PcInstance player, int val) {
        int npcId = getTemplate().npcId;
        String filename;
        NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());

        if (val == 0) // 0 - first buy lottery ticket window
        {
            filename = (getHtmlPath(npcId, 1));
            html.setFile(filename);
        } else if (val >= 1 && val <= 21) // 1-20 - buttons, 21 - second buy lottery ticket window
        {
            if (!Lottery.getInstance().isStarted()) {
                //tickets can't be sold
                player.sendPacket(Static.NO_LOTTERY_TICKETS_CURRENT_SOLD);
                return;
            }
            if (!Lottery.getInstance().isSellableTickets()) {
                //tickets can't be sold
                player.sendPacket(Static.NO_LOTTERY_TICKETS_AVAILABLE);
                return;
            }

            filename = (getHtmlPath(npcId, 5));
            html.setFile(filename);

            int count = 0;
            int found = 0;
            // counting buttons and unsetting button if found
            for (int i = 0; i < 5; i++) {
                if (player.getLoto(i) == val) {
                    //unsetting button
                    player.setLoto(i, 0);
                    found = 1;
                } else if (player.getLoto(i) > 0) {
                    count++;
                }
            }

            //if not rearched limit 5 and not unseted value
            if (count < 5 && found == 0 && val <= 20) {
                for (int i = 0; i < 5; i++) {
                    if (player.getLoto(i) == 0) {
                        player.setLoto(i, val);
                        break;
                    }
                }
            }

            //setting pusshed buttons
            count = 0;
            for (int i = 0; i < 5; i++) {
                if (player.getLoto(i) > 0) {
                    count++;
                    String button = String.valueOf(player.getLoto(i));
                    if (player.getLoto(i) < 10) {
                        button = "0" + button;
                    }
                    String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
                    String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
                    html.replace(search, replace);
                }
            }

            if (count == 5) {
                String search = "0\">Return";
                String replace = "22\">The winner selected the numbers above.";
                html.replace(search, replace);
            }
        } else if (val == 22) //22 - selected ticket with 5 numbers
        {
            if (!Lottery.getInstance().isStarted()) {
                //tickets can't be sold
                player.sendPacket(Static.NO_LOTTERY_TICKETS_CURRENT_SOLD);
                return;
            }
            if (!Lottery.getInstance().isSellableTickets()) {
                //tickets can't be sold
                player.sendPacket(Static.NO_LOTTERY_TICKETS_AVAILABLE);
                return;
            }

            int price = Config.ALT_LOTTERY_TICKET_PRICE;
            int lotonumber = Lottery.getInstance().getId();
            int enchant = 0;
            int type2 = 0;

            for (int i = 0; i < 5; i++) {
                if (player.getLoto(i) == 0) {
                    return;
                }

                if (player.getLoto(i) < 17) {
                    enchant += Math.pow(2, player.getLoto(i) - 1);
                } else {
                    type2 += Math.pow(2, player.getLoto(i) - 17);
                }
            }
            if (player.getAdena() < price) {
                player.sendPacket(Static.YOU_NOT_ENOUGH_ADENA);
                return;
            }
            if (!player.reduceAdena("Loto", price, this, true)) {
                return;
            }
            Lottery.getInstance().increasePrize(price);

            player.sendPacket(SystemMessage.id(SystemMessageId.ACQUIRED).addNumber(lotonumber).addItemName(4442));

            L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), 4442);
            item.setCount(1);
            item.setCustomType1(lotonumber);
            item.setEnchantLevel(enchant);
            item.setCustomType2(type2);
            player.getInventory().addItem("Loto", item, player, this);

            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(item);
            L2ItemInstance adenaupdate = player.getInventory().getItemByItemId(57);
            iu.addModifiedItem(adenaupdate);
            player.sendPacket(iu);

            filename = (getHtmlPath(npcId, 3));
            html.setFile(filename);
        } else if (val == 23) //23 - current lottery jackpot
        {
            filename = (getHtmlPath(npcId, 3));
            html.setFile(filename);
        } else if (val == 24) // 24 - Previous winning numbers/Prize claim
        {
            filename = (getHtmlPath(npcId, 4));
            html.setFile(filename);

            int lotonumber = Lottery.getInstance().getId();
            TextBuilder message = new TextBuilder();
            for (L2ItemInstance item : player.getInventory().getItems()) {
                if (item == null) {
                    continue;
                }
                if (item.getItemId() == 4442 && item.getCustomType1() < lotonumber) {
                    message.append("<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ");
                    int[] numbers = Lottery.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
                    for (int i = 0; i < 5; i++) {
                        message.append(numbers[i] + " ");
                    }
                    int[] check = Lottery.getInstance().checkTicket(item);
                    if (check[0] > 0) {
                        switch (check[0]) {
                            case 1:
                                message.append("- 1st Prize");
                                break;
                            case 2:
                                message.append("- 2nd Prize");
                                break;
                            case 3:
                                message.append("- 3th Prize");
                                break;
                            case 4:
                                message.append("- 4th Prize");
                                break;
                        }
                        message.append(" " + check[1] + "a.");
                    }
                    message.append("</a><br>");
                }
            }
            if (message.toString().equals("")) {
                message.append("There is no winning lottery ticket...<br>");
            }
            html.replace("%result%", message.toString());
        } else if (val > 24) // >24 - check lottery ticket by item object id
        {
            int lotonumber = Lottery.getInstance().getId();
            L2ItemInstance item = player.getInventory().getItemByObjectId(val);
            if (item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber) {
                return;
            }
            int[] check = Lottery.getInstance().checkTicket(item);

            player.sendPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addItemName(4442));
            int adena = check[1];
            if (adena > 0) {
                player.addAdena("Loto", adena, this, true);
            }
            player.destroyItem("Loto", item, this, false);
            return;
        }
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%race%", "" + Lottery.getInstance().getId());
        html.replace("%adena%", "" + Lottery.getInstance().getPrize());
        html.replace("%ticket_price%", "" + Config.ALT_LOTTERY_TICKET_PRICE);
        html.replace("%prize5%", "" + (Config.ALT_LOTTERY_5_NUMBER_RATE * 100));
        html.replace("%prize4%", "" + (Config.ALT_LOTTERY_4_NUMBER_RATE * 100));
        html.replace("%prize3%", "" + (Config.ALT_LOTTERY_3_NUMBER_RATE * 100));
        html.replace("%prize2%", "" + Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE);
        html.replace("%enddate%", "" + DateFormat.getDateInstance().format(Lottery.getInstance().getEndDate()));
        player.sendPacket(html);

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendActionFailed();
    }

    public void makeCPRecovery(L2PcInstance player) {
        if (getNpcId() != 31225 && getNpcId() != 31226) {
            return;
        }

        if (player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("Go away, you're not welcome here.");
            return;
        }

        int neededmoney = 100;

        if (!player.reduceAdena("RestoreCP", neededmoney, player.getLastFolkNPC(), true)) {
            return;
        }

        player.setCurrentCp(player.getMaxCp());
        //cp restored
        player.sendPacket(SystemMessage.id(SystemMessageId.S1_CP_WILL_BE_RESTORED).addString(player.getName()));
    }

    /**
     * Add Newbie helper buffs to L2Player according to its level.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Get the range level in wich player
     * must be to obtain buff </li> <li>If player level is out of range, display
     * a message and return </li> <li>According to player level cast buff
     * </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> Newbie Helper Buff list is define in sql table
     * helper_buff_list</B></FONT><BR><BR>
     *
     * @param player The L2PcInstance that talk with the L2NpcInstance
     *
     */
    public void makeSupportMagic(L2PcInstance player) {
        if (player == null) {
            return;
        }

        // Prevent a cursed weapon weilder of being buffed
        if (player.isCursedWeaponEquiped()) {
            return;
        }

        int player_level = player.getLevel();
        int lowestLevel = 0;
        int higestLevel = 0;

        // Select the player
        setTarget(player);

        // Calculate the min and max level between wich the player must be to obtain buff
        if (player.isMageClass()) {
            lowestLevel = HelperBuffTable.getInstance().getMagicClassLowestLevel();
            higestLevel = HelperBuffTable.getInstance().getMagicClassHighestLevel();
        } else {
            lowestLevel = HelperBuffTable.getInstance().getPhysicClassLowestLevel();
            higestLevel = HelperBuffTable.getInstance().getPhysicClassHighestLevel();
        }

        // If the player is too high level, display a message and return
        if (player_level > higestLevel || !player.isNewbie()) {
            String content = "<html><body>Newbie Guide:<br>Only a <font color=\"LEVEL\">novice character of level " + higestLevel + " or less</font> can receive my support magic.<br>Your novice character is the first one that you created and raised in this world.</body></html>";
            insertObjectIdAndShowChatWindow(player, content);
            return;
        }

        // If the player is too low level, display a message and return
        if (player_level < lowestLevel) {
            String content = "<html><body>Come back here when you have reached level " + lowestLevel + ". I will give you support magic then.</body></html>";
            insertObjectIdAndShowChatWindow(player, content);
            return;
        }

        L2Skill skill = null;
        // Go through the Helper Buff list define in sql table helper_buff_list and cast skill
        for (L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable()) {
            if (helperBuffItem.isMagicClassBuff() == player.isMageClass()) {
                if (player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel()) {
                    skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
                    if (skill.getSkillType() == SkillType.SUMMON) {
                        player.doCast(skill);
                    } else {
                        doCast(skill);
                    }
                }
            }
        }

    }

    public void showChatWindow(L2PcInstance player) {
        showChatWindow(player, 0);
    }

    /**
     * Returns true if html exists
     *
     * @param player
     * @param type
     * @return boolean
     */
    private boolean showPkDenyChatWindow(L2PcInstance player, String type) {
        String html = HtmCache.getInstance().getHtm("data/html/" + type + "/" + getNpcId() + "-pk.htm");

        if (html != null) {
            NpcHtmlMessage pkDenyMsg = NpcHtmlMessage.id(getObjectId());
            pkDenyMsg.setHtml(html);
            player.sendPacket(pkDenyMsg);
            player.sendActionFailed();
            return true;
        }

        return false;
    }

    /**
     * Open a chat window on client with the text of the L2NpcInstance.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Get the text of the selected HTML
     * file in function of the npcId and of the page number </li> <li>Send a
     * Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to
     * the L2PcInstance </li> <li>Send a Server->Client ActionFailed to the
     * L2PcInstance in order to avoid that the client wait another packet
     * </li><BR>
     *
     * @param player The L2PcInstance that talk with the L2NpcInstance
     * @param val The number of the page of the L2NpcInstance to display
     *
     */
    public void showChatWindow(L2PcInstance player, int val) {
        if (player.getKarma() > 0) {
            if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2MerchantInstance) {
                if (showPkDenyChatWindow(player, "merchant")) {
                    return;
                }
            } else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && this instanceof L2TeleporterInstance) {
                if (showPkDenyChatWindow(player, "teleporter")) {
                    return;
                }
            } else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && this instanceof L2WarehouseInstance) {
                if (showPkDenyChatWindow(player, "warehouse")) {
                    return;
                }
            } else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2FishermanInstance) {
                if (showPkDenyChatWindow(player, "fisherman")) {
                    return;
                }
            }
        }

        if ("L2Auctioneer".equals(getTemplate().type) && val == 0) {
            return;
        }

        int npcId = getTemplate().npcId;
        if (npcId == Config.BUFFER_ID && player.ignoreBuffer()) {
            return;
        }

        /*
         * For use with Seven Signs implementation
         */
        String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
        int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
        int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
        int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
        boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
        int compWinner = SevenSigns.getInstance().getCabalHighestScore();

        switch (npcId) {
            case 31078:
            case 31079:
            case 31080:
            case 31081:
            case 31082: // Dawn Priests
            case 31083:
            case 31084:
            case 31168:
            case 31692:
            case 31694:
            case 31997:
                switch (playerCabal) {
                    case SevenSigns.CABAL_DAWN:
                        if (isSealValidationPeriod) {
                            if (compWinner == SevenSigns.CABAL_DAWN) {
                                if (compWinner != sealGnosisOwner) {
                                    filename += "dawn_priest_2c.htm";
                                } else {
                                    filename += "dawn_priest_2a.htm";
                                }
                            } else {
                                filename += "dawn_priest_2b.htm";
                            }
                        } else {
                            filename += "dawn_priest_1b.htm";
                        }
                        break;
                    case SevenSigns.CABAL_DUSK:
                        if (isSealValidationPeriod) {
                            filename += "dawn_priest_3b.htm";
                        } else {
                            filename += "dawn_priest_3a.htm";
                        }
                        break;
                    default:
                        if (isSealValidationPeriod) {
                            if (compWinner == SevenSigns.CABAL_DAWN) {
                                filename += "dawn_priest_4.htm";
                            } else {
                                filename += "dawn_priest_2b.htm";
                            }
                        } else {
                            filename += "dawn_priest_1a.htm";
                        }
                        break;
                }
                break;
            case 31085:
            case 31086:
            case 31087:
            case 31088: // Dusk Priest
            case 31089:
            case 31090:
            case 31091:
            case 31169:
            case 31693:
            case 31695:
            case 31998:
                switch (playerCabal) {
                    case SevenSigns.CABAL_DUSK:
                        if (isSealValidationPeriod) {
                            if (compWinner == SevenSigns.CABAL_DUSK) {
                                if (compWinner != sealGnosisOwner) {
                                    filename += "dusk_priest_2c.htm";
                                } else {
                                    filename += "dusk_priest_2a.htm";
                                }
                            } else {
                                filename += "dusk_priest_2b.htm";
                            }
                        } else {
                            filename += "dusk_priest_1b.htm";
                        }
                        break;
                    case SevenSigns.CABAL_DAWN:
                        if (isSealValidationPeriod) {
                            filename += "dusk_priest_3b.htm";
                        } else {
                            filename += "dusk_priest_3a.htm";
                        }
                        break;
                    default:
                        if (isSealValidationPeriod) {
                            if (compWinner == SevenSigns.CABAL_DUSK) {
                                filename += "dusk_priest_4.htm";
                            } else {
                                filename += "dusk_priest_2b.htm";
                            }
                        } else {
                            filename += "dusk_priest_1a.htm";
                        }
                        break;
                }
                break;
            case 31095: //
            case 31096: //
            case 31097: //
            case 31098: // Enter Necropolises
            case 31099: //
            case 31100: //
            case 31101: // 
            case 31102: //
                if (isSealValidationPeriod) {
                    if (playerCabal != compWinner || sealAvariceOwner != compWinner) {
                        switch (compWinner) {
                            case SevenSigns.CABAL_DAWN:
                                player.sendPacket(Static.CAN_BE_USED_BY_DAWN);
                                filename += "necro_no.htm";
                                break;
                            case SevenSigns.CABAL_DUSK:
                                player.sendPacket(Static.CAN_BE_USED_BY_DUSK);
                                filename += "necro_no.htm";
                                break;
                            case SevenSigns.CABAL_NULL:
                                filename = (getHtmlPath(npcId, val)); // do the default!
                                break;
                        }
                    } else {
                        filename = (getHtmlPath(npcId, val)); // do the default!
                    }
                } else if (playerCabal == SevenSigns.CABAL_NULL) {
                    filename += "necro_no.htm";
                } else {
                    filename = (getHtmlPath(npcId, val)); // do the default!    
                }
                break;
            case 31114: //
            case 31115: //
            case 31116: // Enter Catacombs
            case 31117: //
            case 31118: //
            case 31119: //
                if (isSealValidationPeriod) {
                    if (playerCabal != compWinner || sealGnosisOwner != compWinner) {
                        switch (compWinner) {
                            case SevenSigns.CABAL_DAWN:
                                player.sendPacket(Static.CAN_BE_USED_BY_DAWN);
                                filename += "cata_no.htm";
                                break;
                            case SevenSigns.CABAL_DUSK:
                                player.sendPacket(Static.CAN_BE_USED_BY_DUSK);
                                filename += "cata_no.htm";
                                break;
                            case SevenSigns.CABAL_NULL:
                                filename = (getHtmlPath(npcId, val)); // do the default!
                                break;
                        }
                    } else {
                        filename = (getHtmlPath(npcId, val)); // do the default!
                    }
                } else if (playerCabal == SevenSigns.CABAL_NULL) {
                    filename += "cata_no.htm";
                } else {
                    filename = (getHtmlPath(npcId, val)); // do the default!    
                }
                break;
            case 31111: // Gatekeeper Spirit (Disciples)
                if (playerCabal == sealAvariceOwner && playerCabal == compWinner) {
                    switch (sealAvariceOwner) {
                        case SevenSigns.CABAL_DAWN:
                            filename += "spirit_dawn.htm";
                            break;
                        case SevenSigns.CABAL_DUSK:
                            filename += "spirit_dusk.htm";
                            break;
                        case SevenSigns.CABAL_NULL:
                            filename += "spirit_null.htm";
                            break;
                    }
                } else {
                    filename += "spirit_null.htm";
                }
                break;
            case 31112: // Gatekeeper Spirit (Disciples)
                filename += "spirit_exit.htm";
                break;
            case 31127: //
            case 31128: //
            case 31129: // Dawn Festival Guides
            case 31130: //
            case 31131: //
                filename += "festival/dawn_guide.htm";
                break;
            case 31137: //
            case 31138: //
            case 31139: // Dusk Festival Guides
            case 31140: //
            case 31141: //
                filename += "festival/dusk_guide.htm";
                break;
            case 31092: // Black Marketeer of Mammon
                filename += "blkmrkt_1.htm";
                break;
            case 31113: // Merchant of Mammon
                /*
                 * switch (compWinner) { case SevenSigns.CABAL_DAWN: if
                 * (playerCabal != compWinner || playerCabal !=
                 * sealAvariceOwner) {
                 * player.sendPacket(Static.CAN_BE_USED_BY_DAWN); return; }
                 * break; case SevenSigns.CABAL_DUSK: if (playerCabal !=
                 * compWinner || playerCabal != sealAvariceOwner) {
                 * player.sendPacket(Static.CAN_BE_USED_BY_DUSK); return; }
                 * break; }
                 */
                filename += "mammmerch_1.htm";
                break;
            case 31126: // Blacksmith of Mammon
                /*
                 * switch (compWinner) { case SevenSigns.CABAL_DAWN: if
                 * (playerCabal != compWinner || playerCabal != sealGnosisOwner)
                 * { player.sendPacket(Static.CAN_BE_USED_BY_DAWN); return; }
                 * break; case SevenSigns.CABAL_DUSK: if (playerCabal !=
                 * compWinner || playerCabal != sealGnosisOwner) {
                 * player.sendPacket(Static.CAN_BE_USED_BY_DUSK); return; }
                 * break; }
                 */
                filename += "mammblack_1.htm";
                break;
            case 31132:
            case 31133:
            case 31134:
            case 31135:
            case 31136:  // Festival Witches
            case 31142:
            case 31143:
            case 31144:
            case 31145:
            case 31146:
                filename += "festival/festival_witch.htm";
                break;
            case 31688:
                if (player.isNoble()) {
                    filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
                } else {
                    filename = (getHtmlPath(npcId, val));
                }
                break;
            case 31690:
            case 31769:
            case 31770:
            case 31771:
            case 31772:
                if (player.isNoble() || player.isHero()) {
                    filename = Olympiad.OLYMPIAD_HTML_PATH + "obelisk001.htm";
                } else {
                    filename = Olympiad.OLYMPIAD_HTML_PATH + "obelisk001a.htm";
                }
                break;
            case 40001:
                showBufferWindow(player, getHtmlPath(npcId, val));
                return;
            default:
                if (npcId >= 31865 && npcId <= 31918) {
                    filename += "rift/GuardianOfBorder.htm";
                    break;
                }
                if ((npcId >= 31093 && npcId <= 31094) || (npcId >= 31172 && npcId <= 31201) || (npcId >= 31239 && npcId <= 31254)) {
                    return;
                }
                // Get the text of the selected HTML file in function of the npcId and of the page number
                filename = (getHtmlPath(npcId, val));
                break;
        }

        // Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance 
        NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
        html.setFile(filename);

        //String word = "npc-"+npcId+(val>0 ? "-"+val : "" )+"-dialog-append";
        if (this instanceof L2MerchantInstance) {
            if (Config.LIST_PET_RENT_NPC.contains(npcId)) {
                html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");
            }
        }

        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());

        if (isL2Teleporter()) {
            html = GrandBossManager.getHtmlRespawns(html);
        }

        player.sendPacket(html);

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendActionFailed();
    }

    /**
     * Open a chat window on client with the text specified by the given file
     * name and path,<BR> relative to the datapack root. <BR><BR> Added by Tempy
     *
     * @param player The L2PcInstance that talk with the L2NpcInstance
     * @param filename The filename that contains the text to send
     *
     */
    public void showChatWindow(L2PcInstance player, String filename) {
        if (getTemplate().npcId == Config.BUFFER_ID && player.ignoreBuffer()) {
            return;
        }
        // Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance 
        NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(html);

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendActionFailed();
    }

    /**
     * Return the Exp Reward of this L2NpcInstance contained in the
     * L2NpcTemplate (modified by RATE_XP).<BR><BR>
     */
    public int getExpReward() {
        //double rateXp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
        //return (int) (getTemplate().rewardExp * rateXp * Config.RATE_XP);
        //return (int) (getTemplate().rewardExp * Config.RATE_XP);
        double rateXp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
        return (int) (getTemplate().rewardExp * rateXp * Config.RATE_XP);
    }

    /**
     * Return the SP Reward of this L2NpcInstance contained in the L2NpcTemplate
     * (modified by RATE_SP).<BR><BR>
     */
    public int getSpReward() {
        //double rateSp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
        //return (int) (getTemplate().rewardSp * rateSp * Config.RATE_SP);
        return (int) (getTemplate().rewardSp * Config.RATE_SP);
    }

    /**
     * Kill the L2NpcInstance (the corpse disappeared after 7 seconds).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Create a DecayTask to remove the
     * corpse of the L2NpcInstance after 7 seconds </li> <li>Set target to null
     * and cancel Attack or Cast </li> <li>Stop movement </li> <li>Stop HP/MP/CP
     * Regeneration task </li> <li>Stop all active skills effects in progress on
     * the L2Character </li> <li>Send the Server->Client packet StatusUpdate
     * with current HP and MP to all other L2PcInstance to inform </li>
     * <li>Notify L2Character AI </li><BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR> <li> L2Attackable </li><BR><BR>
     *
     * @param killer The L2Character who killed it
     *
     */
    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        // normally this wouldn't really be needed, but for those few exceptions, 
        // we do need to reset the weapons back to the initial templated weapon.
        _currentLHandId = getTemplate().lhand;
        _currentRHandId = getTemplate().rhand;
        _currentCollisionHeight = getTemplate().collisionHeight;
        _currentCollisionRadius = getTemplate().collisionRadius;
        DecayTaskManager.getInstance().addDecayTask(this);
        return true;
    }

    /**
     * Set the spawn of the L2NpcInstance.<BR><BR>
     *
     * @param spawn The L2Spawn that manage the L2NpcInstance
     *
     */
    public void setSpawn(L2Spawn spawn) {
        _spawn = spawn;
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    /**
     * Remove the L2NpcInstance from the world and update its spawn object (for
     * a complete removal use the deleteMe method).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Remove the L2NpcInstance from the
     * world when the decay task is launched </li> <li>Decrease its spawn
     * counter </li> <li>Manage Siege task (killFlag, killCT) </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the
     * object from _allObjects of L2World </B></FONT><BR> <FONT
     * COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND
     * Server->Client packets to players</B></FONT><BR><BR>
     *
     */
    @Override
    public void onDecay() {
        if (isDecayed()) {
            return;
        }

        setDecayed(true);

        // Manage Life Control Tower
        if (this instanceof L2ControlTowerInstance) {
            ((L2ControlTowerInstance) this).onDeath();
        }

        // Decrease its spawn counter
        if (_spawn != null) {
            _spawn.decreaseCount(this);
            if (_spawn.getTerritory() != null) {
                _spawn.getTerritory().notifyDeath();
                _spawn.setLastKill(System.currentTimeMillis());
            }
        }

        // Remove the L2NpcInstance from the world when the decay task is launched
        super.onDecay();
    }

    /**
     * Remove PROPERLY the L2NpcInstance from the world.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Remove the L2NpcInstance from the
     * world and update its spawn object </li> <li>Remove all L2Object from
     * _knownObjects and _knownPlayer of the L2NpcInstance then cancel Attak or
     * Cast and notify AI </li> <li>Remove L2Object object from _allObjects of
     * L2World </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND
     * Server->Client packets to players</B></FONT><BR><BR>
     *
     */
    public void deleteMe() {
        if (getWorldRegion() != null) {
            getWorldRegion().removeFromZones(this);
        }
        //FIXME this is just a temp hack, we should find a better solution

        try {
            decayMe();
        } catch (Throwable t) {
            _log.severe("deletedMe(): " + t);
        }

        // Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
        try {
            getKnownList().removeAllKnownObjects();
        } catch (Throwable t) {
            _log.severe("deletedMe(): " + t);
        }

        // Remove L2Object object from _allObjects of L2World
        L2World.getInstance().removeObject(this);
    }

    /**
     * Return the L2Spawn object that manage this L2NpcInstance.<BR><BR>
     */
    @Override
    public L2Spawn getSpawn() {
        return _spawn;
    }

    @Override
    public String toString() {
        return getTemplate().name;
    }

    public boolean isDecayed() {
        return _isDecayed;
    }

    public void setDecayed(boolean decayed) {
        _isDecayed = decayed;
    }

    public void endDecayTask() {
        if (!isDecayed()) {
            DecayTaskManager.getInstance().cancelDecayTask(this);
            onDecay();
        }
    }

    public boolean isMob() // rather delete this check
    {
        return false; // This means we use MAX_NPC_ANIMATION instead of MAX_MONSTER_ANIMATION
    }

    // Two functions to change the appearance of the equipped weapons on the NPC
    // This is only useful for a few NPCs and is most likely going to be called from AI
    public void setLHandId(int newWeaponId) {
        _currentLHandId = newWeaponId;
    }

    public void setRHandId(int newWeaponId) {
        _currentRHandId = newWeaponId;
    }

    public void setCollisionHeight(int height) {
        _currentCollisionHeight = height;
    }

    public void setCollisionRadius(int radius) {
        _currentCollisionRadius = radius;
    }

    public int getCollisionHeight() {
        return _currentCollisionHeight;
    }

    public int getCollisionRadius() {
        return _currentCollisionRadius;
    }

    //каст скилла
    public void addUseSkillDesire(int skillId, int skillLvl) {
        doCast(SkillTable.getInstance().getInfo(skillId, skillLvl));
    }

    public void dropItem(int item, int count, L2PcInstance pickuper) {
        L2ItemInstance ditem = ItemTable.getInstance().createItem("MonsterDrop", item, count, null, this);
        ditem.dropMe(this, getX() + Rnd.get(40), getY() + Rnd.get(40), getZ());

        if (pickuper != null) {
            ditem.setPickuper(pickuper);
        }
    }

    public void dropItem(int item, int count) {
        dropItem(item, count, null);
    }

    public void sayString(String text, int type) {
        broadcastPacket(new CreatureSay(getObjectId(), type, getName(), text));
    }

    public void sayString(String text) {
        sayString(text, 0);
    }

    //
    public boolean fromMonastry() {
        return getTemplate().fromMonastry();
    }

    @Override
    public boolean isEnemyForMob(L2Attackable mob) {
        return false;
    }

    // кастом рейд дроп
    public void dropRaidCustom(L2PcInstance killer) {
        if (_raidRewards.isEmpty()) {
            return;
        }

        for (FastList.Node<EventReward> k = _raidRewards.head(), endk = _raidRewards.tail(); (k = k.getNext()) != endk;) {
            EventReward reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                killer.addItem("RaidDrop", reward.id, reward.count, killer, true);
            }
        }
    }

    /*
     * public void dropEpicCustom(L2PcInstance killer) { if
     * (_epicRewards.isEmpty()) return;
     *
     * for (FastList.Node<EventReward> k = _epicRewards.head(), endk =
     * _epicRewards.tail(); (k = k.getNext()) != endk;) { EventReward reward =
     * k.getValue(); if (reward == null) continue;
     *
     * if (Rnd.get(100) < reward.chance) { L2ItemInstance item =
     * ItemTable.getInstance().createItem("EpicDrop", reward.id, 1, killer,
     * null); item.setMana(reward.count);
     * killer.getInventory().addItem("EpicDrop", item, killer, null);
     *
     * SystemMessage smsg = SystemMessage.id(SystemMessageId.EARNED_ITEM);
     * smsg.addItemName(item.getItemId()); killer.sendPacket(smsg); } }
     * killer.sendItems(false); killer.sendChanges(); }
     */
    public int getItemCount(L2PcInstance player, int itemId) {
        return player.getItemCount(itemId);
    }

    public void giveItem(L2PcInstance player, int itemId, int count) {
        player.addItem("Npc.giveItem", itemId, count, player, true);
    }

    public void deleteItem(L2PcInstance player, int itemId, int count) {
        player.destroyItemByItemId("Npc.deleteItem", itemId, count, player, true);
    }

    public void addBuff(L2Character target, int id, int lvl) {
        if (CustomServerData.getInstance().isWhiteBuff(id)) {
            target.stopSkillEffects(id);
            SkillTable.getInstance().getInfo(id, lvl).getEffects(target, target);
        }
    }

    public void soundEffect(L2PcInstance player, String sound) {
        player.sendPacket(new PlaySound(sound));
    }
    /**
     * Возвращает режим NPC: свежезаспавненный или нормальное состояние
     *
     * @return true, если NPC свежезаспавненный
     */
    private int _showSpawnAnimation = 2;

    public int isShowSpawnAnimation() {
        return _showSpawnAnimation;
    }

    @Override
    public void setShowSpawnAnimation(int value) {
        _showSpawnAnimation = value;
    }

    // заточка пухи
    public int getWeaponEnchant() {
        return _weaponEnch;
    }

    public void setWeaponEnchant(int ench) {
        _weaponEnch = ench;
    }

    // нпц чат
    @Override
    public void doNpcChat(int type, String name) {
        _template.doNpcChat(this, type, name);
    }

    @Override
    public boolean isEventMob() {
        return isEventMob;
    }

    @Override
    public int getNpcId() {
        return getTemplate().npcId;
    }

    @Override
    public boolean isL2Npc() {
        return true;
    }

    @Override
    public boolean isPcNpc() {
        return true;
    }

    @Override
    public boolean isShop() {
        return true;
    }

    @Override
    public L2NpcInstance getNpcShop() {
        return this;
    }

    public FastList<Integer> getPenaltyItems() {
        return _template.getPenaltyItems();
    }

    public Location getPenaltyLoc() {
        return _template.getPenaltyLoc();
    }

    public L2PcInstance getMostDamager() {
        return null;
    }
    private int _teamAura = 0;

    public void setTeamAura(int teamAura) {
        _teamAura = teamAura;
    }

    public int getTeamAura() {
        if (isChampion() && Config.L2JMOD_CHAMPION_AURA) {
            return 2;
        }
        if (isChampion1() && Config.L2JMOD_CHAMPION_AURA_BLUE) {
            return 1;
        }
        return _teamAura;
    }

    @Override
    public int getChaseRange() {
        return _template.chaseRange;
    }

    // вывод ошибок, TODO перегнать в HTML и текст под конкретные ситуации
    public void showError(L2PcInstance player, String errorText) {
        player.setStockItem(-1, 0, 0, 0, 0);
        player.setStockInventoryItem(0, 0);
        player.setStockSelf(0);
        //player.sendMessage("Биржа: " + errorText);

        NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
        TextBuilder replyMSG = new TextBuilder("<html><body> Ой!<br> " + errorText + "</body></html>");
        reply.setHtml(replyMSG.toString());
        player.sendPacket(reply);

        player.sendActionFailed();
    }

    public void changeSex(L2PcInstance player) {
        L2ItemInstance coins = player.getInventory().getItemByItemId(Config.CHGSEX_COIN);
        if (coins == null || coins.getCount() < Config.CHGSEX_PRICE) {
            showError(player, "Стоимость смены пола " + Config.CHGSEX_PRICE + " " + Config.CHGSEX_COIN_NAME + ".");
            return;
        }
        player.destroyItemByItemId("Donate Shop", Config.CHGSEX_COIN, Config.CHGSEX_PRICE, player, true);

        player.setSex((player.getAppearance().getSex() ? false : true));
        player.store();
        player.sendHtmlMessage("Ваш пол изменен.");
        player.teleToLocation(player.getX(), player.getY(), player.getZ());
        player.kick();
        //Log.addDonate(player, "changeSex.", Config.CHGSEX_PRICE);
    }

    public void changeClass(L2PcInstance player, int classId) {
        if (player.isSubClassActive()) {
            player.sendHtmlMessage("Переключитесь на основной класс!");
            return;
        }
        L2ItemInstance coins = player.getInventory().getItemByItemId(Config.CHGCLASS_COIN);
        if (coins == null || coins.getCount() < Config.CHGCLASS_PRICE) {
            showError(player, "Стоимость смены класса " + Config.CHGCLASS_PRICE + " " + Config.CHGCLASS_COIN_NAME + ".");
            return;
        }
        player.destroyItemByItemId("Donate Shop", Config.CHGCLASS_COIN, Config.CHGCLASS_PRICE, player, true);

        boolean valid = false;
        for (ClassId classid : ClassId.values()) {
            if (classId == classid.getId()) {
                valid = true;
            }
        }

        if (valid && (player.getClassId().getId() != classId)) {
            player.setClassId(classId);
            if (!player.isSubClassActive()) {
                player.setBaseClass(classId);
            }
            String newclass = player.getTemplate().className;
            player.store();
            player.sendHtmlMessage("Ваш класс изменен на " + newclass + ".");
            player.teleToLocation(player.getX(), player.getY(), player.getZ());
            Log.addDonate(player, "changeClass.", Config.CHGCLASS_PRICE);
            return;
        }
        player.sendHtmlMessage("Произошла ошибка при поытки изменить класс!");
    }
}
