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
package net.sf.l2j.gameserver.ai;

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStart;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStop;
import net.sf.l2j.gameserver.network.serverpackets.CharMoveToLocation;
import net.sf.l2j.gameserver.network.serverpackets.Die;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocationInVehicle;
import net.sf.l2j.gameserver.network.serverpackets.MoveToPawn;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.StopRotation;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * Mother class of all objects AI in the world.<BR><BR>
 *
 * AbastractAI :<BR><BR>
 * <li>L2CharacterAI</li><BR><BR>
 *
 */
abstract class AbstractAI implements Ctrl {

    protected static final Logger _log = AbstractLogger.getLogger(AbstractAI.class.getName());

    class FollowTask implements Runnable {

        protected int _range = 60;

        public FollowTask() {
        }

        public FollowTask(int range) {
            _range = range;
        }

        public void run() {
            /* try
            {*/
            if (_followTask == null) {
                return;
            }

            if (getFollowTarget() == null) {
                stopFollow();
                return;
            }
            if (!_actor.isInsideRadius(getFollowTarget(), _range, true, false)) {
                moveToPawn(getFollowTarget(), _range);
            }
            /*}
            catch (Throwable t)
            {
            _log.log(Level.WARNING, "", t);
            }*/
        }
    }
    /** The character that this AI manages */
    protected final L2Character _actor;
    /** An accessor for private methods of the actor */
    protected final L2Character.AIAccessor _accessor;
    /** Current long-term intention */
    protected CtrlIntention _intention = AI_INTENTION_IDLE;
    /** Current long-term intention parameter */
    protected Object _intentionArg0 = null;
    /** Current long-term intention parameter */
    protected Object _intentionArg1 = null;
    /** Flags about client's state, in order to know which messages to send */
    protected boolean _clientMoving;
    /** Flags about client's state, in order to know which messages to send */
    protected boolean _clientAutoAttacking;
    /** Flags about client's state, in order to know which messages to send */
    protected int _clientMovingToPawnOffset;
    /** Different targets this AI maintains */
    private WeakReference<L2Object> _target;
    private WeakReference<L2Character> _castTarget;
    protected WeakReference<L2Character> _attackTarget;
    protected WeakReference<L2Character> _followTarget;
    /** The skill we are curently casting by INTENTION_CAST */
    L2Skill _skill;
    /** Diferent internal state flags */
    private int _moveToPawnTimeout;
    protected Future<?> _followTask = null;
    private static final int FOLLOW_INTERVAL = 1000;
    private static final int ATTACK_FOLLOW_INTERVAL = 500;

    /**
     * Constructor of AbstractAI.<BR><BR>
     *
     * @param accessor The AI accessor of the L2Character
     *
     */
    protected AbstractAI(L2Character.AIAccessor accessor) {
        _accessor = accessor;

        // Get the L2Character managed by this Accessor AI
        _actor = accessor.getActor();
    }

    /**
     * Return the L2Character managed by this Accessor AI.<BR><BR>
     */
    public L2Character getActor() {
        return _actor;
    }

    /**
     * Return the current Intention.<BR><BR>
     */
    public CtrlIntention getIntention() {
        return _intention;
    }

    protected void setCastTarget(L2Character target) {
        _castTarget = target == null ? null : new WeakReference<L2Character>(target);
    }

    /**
     * Return the current cast target.<BR><BR>
     */
    public L2Character getCastTarget() {
        if (_castTarget == null) {
            return null;
        }

        final L2Character t = _castTarget.get();
        if (t == null) {
            _castTarget = null;
        }
        return t;
    }

    protected void setAttackTarget(L2Character target) {
        _attackTarget = target == null ? null : new WeakReference<L2Character>(target);
    }

    /**
     * Return current attack target.<BR><BR>
     */
    @Override
    public L2Character getAttackTarget() {
        if (_attackTarget == null) {
            return null;
        }

        final L2Character t = _attackTarget.get();
        if (t == null) {
            _attackTarget = null;
        }
        return t;
    }

    /**
     * Set the Intention of this AbstractAI.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is USED by AI classes</B></FONT><BR><BR>
     *
     * <B><U> Overriden in </U> : </B><BR>
     * <B>L2AttackableAI</B> : Create an AI Task executed every 1s (if necessary)<BR>
     * <B>L2PlayerAI</B> : Stores the current AI intention parameters to later restore it if necessary<BR><BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0 The first parameter of the Intention
     * @param arg1 The second parameter of the Intention
     *
     */
    synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1) {
        /*
        if (Config.DEBUG)
        _log.warning("AbstractAI: changeIntention -> " + intention + " " + arg0 + " " + arg1);
         */

        _intention = intention;
        _intentionArg0 = arg0;
        _intentionArg1 = arg1;
    }

    /**
     * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR><BR>
     *
     * @param intention The new Intention to set to the AI
     *
     */
    public final void setIntention(CtrlIntention intention) {
        setIntention(intention, null, null);
    }

    /**
     * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR><BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0 The first parameter of the Intention (optional target)
     *
     */
    public final void setIntention(CtrlIntention intention, Object arg0) {
        setIntention(intention, arg0, null);
    }

    /**
     * Launch the L2CharacterAI onIntention method corresponding to the new Intention.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Stop the FOLLOW mode if necessary</B></FONT><BR><BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0 The first parameter of the Intention (optional target)
     * @param arg1 The second parameter of the Intention (optional target)
     *
     */
    public final void setIntention(CtrlIntention intention, Object arg0, Object arg1) {
        if (!_actor.isVisible() || !_actor.hasAI()) {
            return;
        }

        /*
        if (Config.DEBUG)
        _log.warning("AbstractAI: setIntention -> " + intention + " " + arg0 + " " + arg1);
         */

        // Stop the follow mode if necessary
        if (intention != AI_INTENTION_FOLLOW && intention != AI_INTENTION_ATTACK) {
            stopFollow();
        }

        // Launch the onIntention method of the L2CharacterAI corresponding to the new Intention
        switch (intention) {
            case AI_INTENTION_IDLE:
                onIntentionIdle();
                break;
            case AI_INTENTION_ACTIVE:
                onIntentionActive();
                break;
            case AI_INTENTION_REST:
                onIntentionRest();
                break;
            case AI_INTENTION_ATTACK:
                onIntentionAttack((L2Character) arg0);
                break;
            case AI_INTENTION_CAST:
                onIntentionCast((L2Skill) arg0, (L2Object) arg1);
                break;
            case AI_INTENTION_MOVE_TO:
                onIntentionMoveTo((L2CharPosition) arg0);
                break;
            case AI_INTENTION_MOVE_TO_IN_A_BOAT:
                onIntentionMoveToInABoat((L2CharPosition) arg0, (L2CharPosition) arg1);
                break;
            case AI_INTENTION_FOLLOW:
                onIntentionFollow((L2Character) arg0);
                break;
            case AI_INTENTION_PICK_UP:
                onIntentionPickUp((L2Object) arg0);
                break;
            case AI_INTENTION_INTERACT:
                onIntentionInteract((L2Object) arg0);
                break;
        }
    }

    /**
     * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change
     * (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR><BR>
     *
     * @param evt The event whose the AI must be notified
     *
     */
    public final void notifyEvent(CtrlEvent evt) {
        notifyEvent(evt, null, null);
    }

    /**
     * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change
     * (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR><BR>
     *
     * @param evt The event whose the AI must be notified
     * @param arg0 The first parameter of the Event (optional target)
     *
     */
    public final void notifyEvent(CtrlEvent evt, Object arg0) {
        notifyEvent(evt, arg0, null);
    }

    /**
     * Launch the L2CharacterAI onEvt method corresponding to the Event.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current general intention won't be change
     * (ex : If the character attack and is stunned, he will attack again after the stunned periode)</B></FONT><BR><BR>
     *
     * @param evt The event whose the AI must be notified
     * @param arg0 The first parameter of the Event (optional target)
     * @param arg1 The second parameter of the Event (optional target)
     *
     */
    public final void notifyEvent(CtrlEvent evt, Object arg0, Object arg1) {
        if (!_actor.isVisible() || !_actor.hasAI()) {
            return;
        }

        /*
        if (Config.DEBUG)
        _log.warning("AbstractAI: notifyEvent -> " + evt + " " + arg0 + " " + arg1);
         */

        switch (evt) {
            case EVT_THINK:
                onEvtThink();
                break;
            case EVT_ATTACKED:
                onEvtAttacked((L2Character) arg0);
                break;
            case EVT_AGGRESSION:
                onEvtAggression((L2Character) arg0, ((Number) arg1).intValue());
                break;
            case EVT_STUNNED:
                onEvtStunned((L2Character) arg0);
                break;
            case EVT_SLEEPING:
                onEvtSleeping((L2Character) arg0);
                break;
            case EVT_ROOTED:
                onEvtRooted((L2Character) arg0);
                break;
            case EVT_CONFUSED:
                onEvtConfused((L2Character) arg0);
                break;
            case EVT_MUTED:
                onEvtMuted((L2Character) arg0);
                break;
            case EVT_READY_TO_ACT:
                onEvtReadyToAct();
                break;
            case EVT_USER_CMD:
                onEvtUserCmd(arg0, arg1);
                break;
            case EVT_ARRIVED:
                onEvtArrived();
                break;
            case EVT_ARRIVED_REVALIDATE:
                onEvtArrivedRevalidate();
                break;
            case EVT_ARRIVED_BLOCKED:
                onEvtArrivedBlocked((L2CharPosition) arg0);
                break;
            case EVT_FORGET_OBJECT:
                onEvtForgetObject((L2Object) arg0);
                break;
            case EVT_CANCEL:
                onEvtCancel();
                break;
            case EVT_DEAD:
                onEvtDead();
                break;
            case EVT_FAKE_DEATH:
                onEvtFakeDeath();
                break;
            case EVT_FINISH_CASTING:
                onEvtFinishCasting();
                break;
        }
    }

    protected abstract void onIntentionIdle();

    protected abstract void onIntentionActive();

    protected abstract void onIntentionRest();

    protected abstract void onIntentionAttack(L2Character target);

    protected abstract void onIntentionCast(L2Skill skill, L2Object target);

    protected abstract void onIntentionMoveTo(L2CharPosition destination);

    protected abstract void onIntentionMoveToInABoat(L2CharPosition destination, L2CharPosition origin);

    protected abstract void onIntentionFollow(L2Character target);

    protected abstract void onIntentionPickUp(L2Object item);

    protected abstract void onIntentionInteract(L2Object object);

    protected abstract void onEvtThink();

    protected abstract void onEvtAttacked(L2Character attacker);

    protected abstract void onEvtAggression(L2Character target, int aggro);

    protected abstract void onEvtStunned(L2Character attacker);

    protected abstract void onEvtSleeping(L2Character attacker);

    protected abstract void onEvtRooted(L2Character attacker);

    protected abstract void onEvtConfused(L2Character attacker);

    protected abstract void onEvtMuted(L2Character attacker);

    protected abstract void onEvtReadyToAct();

    protected abstract void onEvtUserCmd(Object arg0, Object arg1);

    protected abstract void onEvtArrived();

    protected abstract void onEvtArrivedRevalidate();

    protected abstract void onEvtArrivedBlocked(L2CharPosition blocked_at_pos);

    protected abstract void onEvtForgetObject(L2Object object);

    protected abstract void onEvtCancel();

    protected abstract void onEvtDead();

    protected abstract void onEvtFakeDeath();

    protected abstract void onEvtFinishCasting();

    /**
     * Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void clientActionFailed() {
        if (_actor.isPlayer()) {
            _actor.sendActionFailed();
        }
    }

    /**
     * Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void moveToPawn(L2Object pawn, int offset) {
        // Chek if actor can move
        if (!_actor.isMovementDisabled()) {
            if (offset < 10) {
                offset = 10;
            }

            // prevent possible extra calls to this function (there is none?), 
            // also don't send movetopawn packets too often
            boolean sendPacket = true;
            if (_clientMoving && getTarget() == pawn) {
                if (_clientMovingToPawnOffset == offset) {
                    if (GameTimeController.getGameTicks() < _moveToPawnTimeout) {
                        return;
                    }
                    sendPacket = false;
                } else if (_actor.isOnGeodataPath()) {
                    // minimum time to calculate new route is 2 seconds
                    if (GameTimeController.getGameTicks() < (_moveToPawnTimeout + 10)) {
                        return;
                    }
                }
            }

            // Set AI movement data
            _clientMoving = true;
            _clientMovingToPawnOffset = offset;
            setTarget(pawn);
            _moveToPawnTimeout = GameTimeController.getGameTicks();
            _moveToPawnTimeout += 1000 / GameTimeController.MILLIS_IN_TICK;

            if (pawn == null || _accessor == null) {
                return;
            }

            // Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
            _accessor.moveTo(pawn.getX(), pawn.getY(), pawn.getZ(), offset);

            if (!_actor.isMoving()) {
                _actor.sendActionFailed();
                return;
            }

            // Send a Server->Client packet MoveToPawn/CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
            if (pawn.isL2Character()) {
                if (_actor.isOnGeodataPath()) {
                    _actor.broadcastPacket(new CharMoveToLocation(_actor));
                    _clientMovingToPawnOffset = 0;
                } else if (sendPacket) // don't repeat unnecessarily
                {
                    _actor.broadcastPacket(new MoveToPawn(_actor, (L2Character) pawn, offset));
                }
            } else {
                _actor.broadcastPacket(new CharMoveToLocation(_actor));
            }
        } else {
            _actor.sendActionFailed();
        }
    }

    /**
     * Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    public void moveXYZ(int x, int y, int z) {
        moveTo(x, y, z);
    }

    protected void moveTo(int x, int y, int z) {
        // Chek if actor can move
        if (!_actor.isMovementDisabled()) {
            // Set AI movement data
            _clientMoving = true;
            _clientMovingToPawnOffset = 0;

            /*if (_actor.canMoveFromToTarget(_actor.getX(), _actor.getY(), _actor.getZ(), x, y, z))
            {*/
            // Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
            _accessor.moveTo(x, y, z);
            // Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
            _actor.broadcastPacket(new CharMoveToLocation(_actor));
            /*}
            else
            {
            setIntention(AI_INTENTION_IDLE);
            _actor.setWalking();
            _actor.teleToLocation(x, y, z, false);
            }*/

            //if (_actor instanceof L2MinionInstance && ((L2MinionInstance)_actor).getLeader() != null)
			/*if (_actor.isL2Monster() && ((L2MonsterInstance)_actor).hasMinions())
            {
            //((L2MonsterInstance)_actor).callMinions();
            int offset = 200;
            if (_actor.isRaid()) 
            offset = 500; // for Raids - need correction
            
            L2MonsterInstance leader = (L2MonsterInstance) _actor;
            for (L2MinionInstance minion : leader.getSpawnedMinions())
            {
            if (minion == null || minion.isDead())
            continue;
            
            if(leader.isRunning())	
            minion.setRunning();
            else 
            minion.setWalking();
            
            int x1 = x + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
            int y1 = y + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
            
            minion.getAI().moveXYZ(x1, y1, z);
            
            /*if (minion.getPlanDistanceSq(leader) > offset*offset)
            {
            int x1 = x + Rnd.nextInt( (offset - 30) * 2 ) - ( offset - 30 );
            int y1 = y + Rnd.nextInt( (offset - 30) * 2 ) - ( offset - 30 );
            // Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
            moveTo(x1, y1, z);
            return;
            }*/
            /*}
            }*/
        } else {
            _actor.sendActionFailed();
        }
    }

    protected void moveToInABoat(L2CharPosition destination, L2CharPosition origin) {
        // Chek if actor can move
        if (!_actor.isMovementDisabled()) {
            /*	// Set AI movement data
            _client_moving = true;
            _client_moving_to_pawn_offset = 0;
            
            // Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
            _accessor.moveTo(((L2PcInstance)_actor).getBoat().getX() - destination.x,((L2PcInstance)_actor).getBoat().getY()- destination.y,((L2PcInstance)_actor).getBoat().getZ() - destination.z);
             */
            // Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
            //CharMoveToLocation msg = new CharMoveToLocation(_actor);
            if (_actor.getPlayer().getBoat() != null) {
                _actor.broadcastPacket(new MoveToLocationInVehicle(_actor, destination, origin));
            }
        } else {
            _actor.sendActionFailed();
        }
    }

    /**
     * Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void clientStopMoving(L2CharPosition pos) {
        /*
        if (Config.DEBUG)
        _log.warning("clientStopMoving();");
         */

        // Stop movement of the L2Character
        if (_actor.isMoving()) {
            _accessor.stopMove(pos);
        }

        _clientMovingToPawnOffset = 0;

        if (_clientMoving || pos != null) {
            _clientMoving = false;

            // Send a Server->Client packet StopMove to the actor and all L2PcInstance in its _knownPlayers
            _actor.broadcastPacket(new StopMove(_actor));

            if (pos != null) {
                // Send a Server->Client packet StopRotation to the actor and all L2PcInstance in its _knownPlayers
                _actor.broadcastPacket(new StopRotation(_actor, pos.heading, 0));
            }
        }
    }

    // Client has already arrived to target, no need to force StopMove packet
    protected void clientStoppedMoving() {
        if (_clientMovingToPawnOffset > 0) // movetoPawn needs to be stopped
        {
            _clientMovingToPawnOffset = 0;
            _actor.broadcastPacket(new StopMove(_actor));
        }
        _clientMoving = false;
    }

    public boolean isAutoAttacking() {
        return _clientAutoAttacking;
    }

    public void setAutoAttacking(boolean isAutoAttacking) {
        _clientAutoAttacking = isAutoAttacking;
    }

    /**
     * Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    public void clientStartAutoAttack() {
        if (!isAutoAttacking()) {
            // Send a Server->Client packet AutoAttackStart to the actor and all L2PcInstance in its _knownPlayers
            _actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
            setAutoAttacking(true);
        }
        AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
    }

    /**
     * Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    public void clientStopAutoAttack() {
        if (_actor.isPlayer()) {
            if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor) && isAutoAttacking()) {
                AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
            }
        } else if (isAutoAttacking()) {
            _actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
        }

        setAutoAttacking(false);
    }

    /**
     * Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die <I>(broadcast)</I>.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     */
    protected void clientNotifyDead() {
        // Send a Server->Client packet Die to the actor and all L2PcInstance in its _knownPlayers
        _actor.broadcastPacket(new Die(_actor));

        // Init AI
        _intention = AI_INTENTION_IDLE;
        setTarget(null);
        setCastTarget(null);
        setAttackTarget(null);

        // Cancel the follow task if necessary
        stopFollow();
    }

    /**
     * Update the state of this actor client side by sending Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance player.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR><BR>
     *
     * @param player The L2PcIstance to notify with state of this L2Character
     *
     */
    public void describeStateToPlayer(L2PcInstance player) {
        if (_clientMoving) {
            if (_clientMovingToPawnOffset != 0 && getFollowTarget() != null) {
                // Send a Server->Client packet MoveToPawn to the actor and all L2PcInstance in its _knownPlayers
                player.sendPacket(new MoveToPawn(_actor, getFollowTarget(), _clientMovingToPawnOffset));
            } else {
                // Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
                player.sendPacket(new CharMoveToLocation(_actor));
            }
        }
    }
    /**
     * Create and Launch an AI Follow Task to execute every 1s.<BR><BR>
     *
     * @param target The L2Character to follow
     *
     */
    private final Lock startFollow = new ReentrantLock();

    public void startFollow(L2Character target) {
        //System.out.println("####?");
        startFollow.lock();
        try {
            if (_followTask != null) {
                _followTask.cancel(false);
                //_followTask = null;
            }

            // Create and Launch an AI Follow Task to execute every 1s
            setFollowTarget(target);
            _followTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(), 5, FOLLOW_INTERVAL);
        } finally {
            startFollow.unlock();
        }
    }

    /**
     * Create and Launch an AI Follow Task to execute every 0.5s, following at specified range.<BR><BR>
     *
     * @param target The L2Character to follow
     *
     */
    public void startFollow(L2Character target, int range) {
        //System.out.println("####??");
        startFollow.lock();
        try {
            if (_followTask != null) {
                _followTask.cancel(false);
                _followTask = null;
            }

            setFollowTarget(target);
            _followTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(range), 5, ATTACK_FOLLOW_INTERVAL);
        } finally {
            startFollow.unlock();
        }
    }
    /**
     * Stop an AI Follow Task.<BR><BR>
     */
    private final Lock stopFollow = new ReentrantLock();

    public void stopFollow() {
        //System.out.println("####???");
        stopFollow.lock();
        try {
            if (_followTask != null) {
                // Stop the Follow Task
                _followTask.cancel(false);
                _followTask = null;
            }
            setFollowTarget(null);
        } finally {
            stopFollow.unlock();
        }
    }

    protected L2Character getFollowTarget() {
        if (_followTarget == null) {
            return null;
        }

        final L2Character t = _followTarget.get();
        if (t == null) {
            _followTarget = null;
        }
        return t;
    }

    protected void setFollowTarget(L2Character target) {
        _followTarget = target == null ? null : new WeakReference<L2Character>(target);
    }

    protected L2Object getTarget() {
        if (_target == null) {
            return null;
        }

        final L2Object t = _target.get();
        if (t == null) {
            _target = null;
        }
        return t;
    }

    protected void setTarget(L2Object target) {
        _target = target == null ? null : new WeakReference<L2Object>(target);
    }
}
