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

import java.util.concurrent.Future;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import static net.sf.l2j.gameserver.ai.CtrlIntention.*;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

/**
 * This class manages AI of L2Attackable.<BR><BR>
 *
 */
public class L2AttackableAI extends L2CharacterAI implements Runnable {

    //protected static final Logger _log = Logger.getLogger(L2AttackableAI.class.getName());
    private static final int RANDOM_WALK_RATE = 30; // confirmed
    // private static final int MAX_DRIFT_RANGE = 300;
    private static final int MAX_ATTACK_TIMEOUT = 300; // int ticks, i.e. 30 seconds
    /**
     * The L2Attackable AI task executed every 1s (call onEvtThink method)
     */
    private Future<?> _aiTask;
    /**
     * The delay after wich the attacked is stopped
     */
    private int _attackTimeout;
    /**
     * The L2Attackable aggro counter
     */
    private int _globalAggro;
    /**
     * The flag used to indicate that a thinking action is in progress
     */
    private boolean _thinking; // to prevent recursive thinking

    /**
     * Constructor of L2AttackableAI.<BR><BR>
     *
     * @param accessor The AI accessor of the L2Character
     *
     */
    public L2AttackableAI(L2Character.AIAccessor accessor) {
        super(accessor);

        _attackTimeout = Integer.MAX_VALUE;
        _globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
    }

    public void run() {
        // Launch actions corresponding to the Event Think
        onEvtThink();

    }

    /**
     * Return True if the target is autoattackable (depends on the actor
     * type).<BR><BR>
     *
     * <B><U> Actor is a L2GuardInstance</U> :</B><BR><BR> <li>The target isn't
     * a Folk or a Door</li> <li>The target isn't dead, isn't invulnerable,
     * isn't in silent moving mode AND too far (>100)</li> <li>The target is in
     * the actor Aggro range and is at the same height</li> <li>The L2PcInstance
     * target has karma (=PK)</li> <li>The L2MonsterInstance target is
     * aggressive</li><BR><BR>
     *
     * <B><U> Actor is a L2SiegeGuardInstance</U> :</B><BR><BR> <li>The target
     * isn't a Folk or a Door</li> <li>The target isn't dead, isn't
     * invulnerable, isn't in silent moving mode AND too far (>100)</li> <li>The
     * target is in the actor Aggro range and is at the same height</li> <li>A
     * siege is in progress</li> <li>The L2PcInstance target isn't a
     * Defender</li><BR><BR>
     *
     * <B><U> Actor is a L2FriendlyMobInstance</U> :</B><BR><BR> <li>The target
     * isn't a Folk, a Door or another L2NpcInstance</li> <li>The target isn't
     * dead, isn't invulnerable, isn't in silent moving mode AND too far
     * (>100)</li> <li>The target is in the actor Aggro range and is at the same
     * height</li> <li>The L2PcInstance target has karma (=PK)</li><BR><BR>
     *
     * <B><U> Actor is a L2MonsterInstance</U> :</B><BR><BR> <li>The target
     * isn't a Folk, a Door or another L2NpcInstance</li> <li>The target isn't
     * dead, isn't invulnerable, isn't in silent moving mode AND too far
     * (>100)</li> <li>The target is in the actor Aggro range and is at the same
     * height</li> <li>The actor is Aggressive</li><BR><BR>
     *
     * @param target The targeted L2Object
     *
     */
    private boolean autoAttackCondition(L2Character target) {
        /*
         * if (target == null || !(_actor instanceof L2Attackable)) return
         * false;
         */
        if (target == null) {
            return false;
        }

        if (!(_actor.isL2Attackable())) {
            return false;
        }

        L2Attackable me = (L2Attackable) _actor;

        if (me.isAlikeDead() || target.isAlikeDead()
                || !me.isInsideRadius(target, me.getAggroRange(), false, false)
                || Math.abs(_actor.getZ() - target.getZ()) > 300) {
            return false;
        }

        return (target.isEnemyForMob(me) && me.canSeeTarget(target));
    }

    public void startAITask() {
        //L2Attackable me = (L2Attackable) _actor;
        //if (me.getAggroRange() == 0)
        //	return;

        // If not idle - create an AI task (schedule onEvtThink repeatedly)
        if (_aiTask == null) {
            _aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 500, 1000);
        }
    }

    public void stopAITask() {
        if (_aiTask != null) {
            _aiTask.cancel(false);
            _aiTask = null;
        }
    }

    @Override
    protected void onEvtDead() {
        stopAITask();
        super.onEvtDead();
    }

    /**
     * Set the Intention of this L2CharacterAI and create an AI Task executed
     * every 1s (call onEvtThink method) for this L2Attackable.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : If actor _knowPlayer isn't
     * EMPTY, AI_INTENTION_IDLE will be change in
     * AI_INTENTION_ACTIVE</B></FONT><BR><BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0 The first parameter of the Intention
     * @param arg1 The second parameter of the Intention
     *
     */
    @Override
    synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1) {
        if (intention == AI_INTENTION_IDLE || intention == AI_INTENTION_ACTIVE) {
            // Check if actor is not dead
            if (!_actor.isAlikeDead()) {
                L2Attackable npc = (L2Attackable) _actor;

                // If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
                if (npc.getKnownList().getKnownPlayers().size() > 0) {
                    intention = AI_INTENTION_ACTIVE;
                }
            }

            if (intention == AI_INTENTION_IDLE) {
                // Set the Intention of this L2AttackableAI to AI_INTENTION_IDLE
                super.changeIntention(AI_INTENTION_IDLE, null, null);

                // Stop AI task and detach AI from NPC
                if (_aiTask != null) {
                    _aiTask.cancel(true);
                    _aiTask = null;
                }

                // Cancel the AI
                _accessor.detachAI();

                return;
            }
        }

        // Set the Intention of this L2AttackableAI to intention
        super.changeIntention(intention, arg0, arg1);

        // If not idle - create an AI task (schedule onEvtThink repeatedly)
        startAITask();
    }

    /**
     * Manage the Attack Intention : Stop current Attack (if necessary),
     * Calculate attack timeout, Start a new Attack and Launch Think
     * Event.<BR><BR>
     *
     * @param target The L2Character to attack
     *
     */
    @Override
    protected void onIntentionAttack(L2Character target) {
        // Calculate the attack timeout
        _attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

        // Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
        super.onIntentionAttack(target);
    }

    /**
     * Manage AI standard thinks of a L2Attackable (called by
     * onEvtThink).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Update every 1s the _globalAggro
     * counter to come close to 0</li> <li>If the actor is Aggressive and can
     * attack, add all autoAttackable L2Character in its Aggro Range to its
     * _aggroList, chose a target and order to attack it</li> <li>If the actor
     * is a L2GuardInstance that can't attack, order to it to return to its home
     * location</li> <li>If the actor is a L2MonsterInstance that can't attack,
     * order to it to random walk (1/100)</li><BR><BR>
     *
     */
    private void thinkActive() {
        L2Attackable npc = (L2Attackable) _actor;

        // Update every 1s the _globalAggro counter to come close to 0
        if (_globalAggro != 0) {
            if (_globalAggro < 0) {
                _globalAggro++;
            } else {
                _globalAggro--;
            }
        }

        // Add all autoAttackable L2Character in L2Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
        // A L2Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
        if (_globalAggro >= 0) {
            // Get all visible objects inside its Aggro Range
            //L2Object[] objects = L2World.getInstance().getVisibleObjects(_actor, ((L2NpcInstance)_actor).getAggroRange());
            // Go through visible objects
            //for (L2Object obj : npc.getKnownList().getKnownObjects().values())
            for (L2Character target : npc.getKnownList().getKnownCharactersInRadius(npc.getAggroRange())) {
                if (target == null) {
                    continue;
                }
                /*
                 * Check to see if this is a festival mob spawn. If it is, then
                 * check to see if the aggro trigger is a festival
                 * participant...if so, move to attack it.
                 */
                if (_actor.isL2FestivalMonster() && target.isPlayer() && target.isFestivalParticipant()) {
                    continue;
                }

                // For each L2Character check if the target is autoattackable
                if (autoAttackCondition(target)) // check aggression
                {
                    // Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
                    int hating = npc.getHating(target);
                    // Add the attacker to the L2Attackable _aggroList with 0 damage and 1 hate
                    if (hating == 0) {
                        npc.addDamageHate(target, 0, 1);
                    }
                }
            }

            // Chose a target from its aggroList
            L2Character hated;
            if (_actor.isConfused()) {
                hated = getAttackTarget(); // Force mobs to attak anybody if confused
            } else {
                hated = npc.getMostHated();
            }

            // Order to the L2Attackable to attack the target
            if (hated != null) {
                // Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
                int aggro = npc.getHating(hated);
                if (aggro + _globalAggro > 0) {
                    // Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
                    if (!_actor.isRunning()) {
                        _actor.setRunning();
                    }

                    // Set the AI Intention to AI_INTENTION_ATTACK
                    setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);
                }
                return;
            }
        }

        // Check if the actor is a L2GuardInstance
        if (_actor.isL2Guard()) {
            // Order to the L2GuardInstance to return to its home location because there's no target to attack
            ((L2GuardInstance) _actor).returnHome();
            return;
        }

        // If this is a festival monster, then it remains in the same location.
        if (_actor.isL2FestivalMonster()) {
            return;
        }

        // Check if the mob should not return to spawn point
        if (!npc.canReturnToSpawnPoint()) {
            return;
        }

        if (npc.getNpcId() != 29014 && npc.getSpawn() != null) {
            int x1 = npc.getSpawn().getLocx() + Rnd.nextInt(Config.MAX_DRIFT_RANGE * 2) - Config.MAX_DRIFT_RANGE;
            int y1 = npc.getSpawn().getLocy() + Rnd.nextInt(Config.MAX_DRIFT_RANGE * 2) - Config.MAX_DRIFT_RANGE;
            moveTo(x1, y1, npc.getSpawn().getLocz());
        }

        // Minions following leader
        if (_actor.isMinion() && _actor.getLeader() != null) {
            int offset = 200;
            if (_actor.isRaid()) {
                offset = 500; // for Raids - need correction
            }
            if (_actor.getLeader().isRunning()) {
                _actor.setRunning();
            } else {
                _actor.setWalking();
            }

            if (_actor.getPlanDistanceSq(_actor.getLeader()) > offset * offset) {
                int x1 = _actor.getLeader().getX() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
                int y1 = _actor.getLeader().getY() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
                moveTo(x1, y1, _actor.getLeader().getZ());
            }
        }
    }
    /**
     * Manage AI attack thinks of a L2Attackable (called by onEvtThink).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Update the attack timeout if actor
     * is running</li> <li>If target is dead or timeout is expired, stop this
     * attack and set the Intention to AI_INTENTION_ACTIVE</li> <li>Call all
     * L2Object of its Faction inside the Faction Range</li> <li>Chose a target
     * and order to attack it with magic skill or physical attack</li><BR><BR>
     *
     * TODO: Manage casting rules to healer mobs (like Ant Nurses)
     *
     */
    private long _lastChk = 0;

    private void thinkAttack() {
        if (_attackTimeout < GameTimeController.getGameTicks()) {
            // Check if the actor is running
            if (_actor.isRunning()) {
                // Set the actor movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance
                _actor.setWalking();

                // Calculate a new attack timeout
                _attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
            }
        }

        L2Character attackTarget = getAttackTarget();
        // Check if target is dead or if timeout is expired to stop this attack
        if (attackTarget == null || attackTarget.isAlikeDead() || attackTarget.isTeleporting() || _attackTimeout < GameTimeController.getGameTicks() || attackTarget.getFirstEffect(4515) != null) {
            // Stop hating this target after the attack timeout or if target is dead
            if (attackTarget != null) {
                _actor.stopHating(attackTarget);
            }

            // Set the AI Intention to AI_INTENTION_ACTIVE
            setIntention(AI_INTENTION_ACTIVE);

            _actor.setWalking();
            return;
        }

        if (outOfChaseRange(_actor.getSpawn(), attackTarget)) {
            return;
        }

        // Call all L2Object of its Faction inside the Faction Range
        if (_actor.getFactionId() != null) {
            // Go through all L2Object that belong to its faction
            for (L2Object obj : _actor.getKnownList().getKnownObjects().values()) {
                if (obj == null || !(obj.isL2Npc())) {
                    continue;
                }

                L2NpcInstance npc = (L2NpcInstance) obj;
                if (npc == null) {
                    continue;
                }

                Integer fr = npc.getFactionRange();
                if (fr == null || fr == 0 || !(_actor.equalsFactionId(npc.getFactionId()))) {
                    continue;
                }

                // Check if the L2Object is inside the Faction Range of the actor
                if (_actor.isInsideRadius(npc, fr, true, false)
                        && Math.abs(attackTarget.getZ() - npc.getZ()) < 600
                        && npc.getAI() != null
                        && _actor.getAttackByList().contains(attackTarget)
                        && (npc.getAI()._intention == CtrlIntention.AI_INTENTION_IDLE || npc.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE)) {
                    if (attackTarget.isPlayer() && attackTarget.isInParty() && attackTarget.getParty().isInDimensionalRift()) {
                        byte riftType = attackTarget.getParty().getDimensionalRift().getType();
                        byte riftRoom = attackTarget.getParty().getDimensionalRift().getCurrentRoom();

                        if (_actor.isL2RiftInvader() && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ())) {
                            continue;
                        }
                    }

                    if (_actor.canSeeTarget(npc)) {
                        npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attackTarget, 1); // Notify the L2Object AI with EVT_AGGRESSION
                    }
                }
            }
        }

        if (_actor.isAttackingDisabled()) {
            return;
        }

        // Get all information needed to chose between physical or magical attack
        L2Skill[] skills = null;
        double dist2 = 0;
        int range = 0;

        try {
            _actor.setTarget(attackTarget);
            skills = _actor.getAllSkills();
            dist2 = _actor.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY());
            range = _actor.getPhysicalAttackRange() + _actor.getTemplate().collisionRadius + attackTarget.getTemplate().collisionRadius;
        } catch (NullPointerException e) {
            //_log.warning("AttackableAI: Attack target is NULL.");
            //e.printStackTrace();
            setIntention(AI_INTENTION_ACTIVE);
            return;
        }

        L2Weapon weapon = _actor.getActiveWeaponItem();
        if (weapon != null && weapon.getItemType() == L2WeaponType.BOW) {
            // Micht: kepping this one otherwise we should do 2 sqrt
            double distance2 = _actor.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY());
            if (distance2 <= 10000) {
                int chance = 5;
                if (chance >= Rnd.get(100)) {
                    int posX = _actor.getX();
                    int posY = _actor.getY();
                    int posZ = _actor.getZ();
                    double distance = Math.sqrt(distance2); // This way, we only do the sqrt if we need it

                    int signx = -1;
                    int signy = -1;
                    if (_actor.getX() > attackTarget.getX()) {
                        signx = 1;
                    }
                    if (_actor.getY() > attackTarget.getY()) {
                        signy = 1;
                    }
                    posX += Math.round((float) ((signx * ((range / 2) + (Rnd.get(range)))) - distance));
                    posY += Math.round((float) ((signy * ((range / 2) + (Rnd.get(range)))) - distance));
                    setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, _actor.calcHeading(posX, posY)));
                    return;
                }
            }
        }

        // Force mobs to attack anybody if confused
        L2Character hated;
        if (_actor.isConfused()) {
            hated = attackTarget;
        } else {
            hated = ((L2Attackable) _actor).getMostHated();
        }

        if (hated == null) {
            setIntention(AI_INTENTION_ACTIVE);
            return;
        }

        if (hated != attackTarget) {
            setAttackTarget(hated);
        }

        // We should calculate new distance cuz mob can have changed the target
        dist2 = _actor.getPlanDistanceSq(hated.getX(), hated.getY());

        if (hated.isMoving()) {
            range += 50;
        }
        // Check if the actor isn't far from target
        if (dist2 > range * range) {
            // check for long ranged skills and heal/buff skills
            if (!_actor.isMuted() && (_actor.isL2Monster() && Rnd.nextInt(100) <= 5)) {
                for (L2Skill sk : skills) {
                    if (sk.isPassive()) {
                        continue;
                    }

                    int castRange = sk.getCastRange();
                    if (((sk.getSkillType() == L2Skill.SkillType.BUFF
                            || sk.getSkillType() == L2Skill.SkillType.HEAL)
                            || (dist2 >= castRange * castRange / 9.0)
                            && (dist2 <= castRange * castRange) && (castRange > 70))
                            && !_actor.isSkillDisabled(sk.getId())
                            && _actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)) {
                        L2Object OldTarget = _actor.getTarget();
                        if (sk.getSkillType() == L2Skill.SkillType.BUFF || sk.getSkillType() == L2Skill.SkillType.HEAL) {
                            boolean useSkillSelf = true;
                            if (sk.getSkillType() == L2Skill.SkillType.HEAL && _actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)) {
                                useSkillSelf = false;
                                break;
                            }
                            if (sk.getSkillType() == L2Skill.SkillType.BUFF) {
                                L2Effect effect = _actor.getFirstEffect(sk.getId());
                                if (effect != null) {
                                    useSkillSelf = false;
                                }
                            }
                            if (useSkillSelf) {
                                _actor.setTarget(_actor);
                            }
                        }
                        clientStopMoving(null);
                        _accessor.doCast(sk);
                        _actor.setTarget(OldTarget);
                        return;
                    }
                }
            }

            // Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
            if (hated.isMoving()) {
                range -= 100;
            }
            if (range < 5) {
                range = 5;
            }
            moveToPawn(attackTarget, range);
        } // Else, if this is close enough to attack
        else {
            _attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

            // check for close combat skills && heal/buff skills
            if (!_actor.isMuted() /*
                     * && _rnd.nextInt(100) <= 5
                     */) {
                boolean useSkillSelf = true;;
                for (L2Skill sk : skills) {
                    if (sk.isPassive()) {
                        continue;
                    }
                    if (_actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk)
                            && !_actor.isSkillDisabled(sk.getId())
                            && (Rnd.nextInt(100) <= 8 || (_actor.isL2Penalty() && Rnd.nextInt(100) <= 20))) {
                        L2Object OldTarget = _actor.getTarget();
                        if (sk.getSkillType() == L2Skill.SkillType.BUFF || sk.getSkillType() == L2Skill.SkillType.HEAL) {
                            useSkillSelf = true;
                            if (sk.getSkillType() == L2Skill.SkillType.HEAL && _actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5)) {
                                useSkillSelf = false;
                                break;
                            }
                            if (sk.getSkillType() == L2Skill.SkillType.BUFF) {
                                L2Effect effect = _actor.getFirstEffect(sk.getId());
                                if (effect != null) {
                                    useSkillSelf = false;
                                }
                            }
                            if (useSkillSelf) {
                                _actor.setTarget(_actor);
                            }
                        }
                        // GeoData Los Check here
                        if (!useSkillSelf && !_actor.canSeeTarget(OldTarget)) {
                            return;
                        }
                        clientStopMoving(null);
                        _accessor.doCast(sk);
                        _actor.setTarget(OldTarget);
                        return;
                    }
                }
            }

            // Finally, physical attacks
            clientStopMoving(null);
            _accessor.doAttack(hated);
        }
    }

    /**
     * Manage AI thinking actions of a L2Attackable.<BR><BR>
     */
    @Override
    protected void onEvtThink() {
        // Check if the actor can't use skills and if a thinking action isn't already in progress
        if (_thinking || _actor.isAllSkillsDisabled()) {
            return;
        }

        // Start thinking action
        _thinking = true;

        try {
            // Manage AI thinks of a L2Attackable
            if (getIntention() == AI_INTENTION_ACTIVE) {
                thinkActive();
            } else if (getIntention() == AI_INTENTION_ATTACK) {
                thinkAttack();
            }
        } finally {
            // Stop thinking action
            _thinking = false;
        }
    }

    /**
     * Launch actions corresponding to the Event Attacked.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Init the attack : Calculate the
     * attack timeout, Set the _globalAggro to 0, Add the attacker to the actor
     * _aggroList</li> <li>Set the L2Character movement type to run and send
     * Server->Client packet ChangeMoveType to all others L2PcInstance</li>
     * <li>Set the Intention to AI_INTENTION_ATTACK</li><BR><BR>
     *
     * @param attacker The L2Character that attacks the actor
     *
     */
    @Override
    protected void onEvtAttacked(L2Character attacker) {
        //if (_actor instanceof L2ChestInstance && !((L2ChestInstance)_actor).isInteracted())
        //{
        //((L2ChestInstance)_actor).deleteMe();
        //((L2ChestInstance)_actor).getSpawn().startRespawn();
        //return;
        //}

        // Calculate the attack timeout
        _attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

        // Set the _globalAggro to 0 to permit attack even just after spawn
        if (_globalAggro < 0) {
            _globalAggro = 0;
        }

        // Add the attacker to the _aggroList of the actor
        ((L2Attackable) _actor).addDamageHate(attacker, 0, 1);

        // Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
        if (!_actor.isRunning()) {
            _actor.setRunning();
        }

        // Set the Intention to AI_INTENTION_ATTACK
        if (getIntention() != AI_INTENTION_ATTACK) {
            setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
        } else if (((L2Attackable) _actor).getMostHated() != getAttackTarget()) {
            setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
        }

        super.onEvtAttacked(attacker);
    }

    /**
     * Launch actions corresponding to the Event Aggression.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Add the target to the actor
     * _aggroList or update hate if already present </li> <li>Set the actor
     * Intention to AI_INTENTION_ATTACK (if actor is L2GuardInstance check if it
     * isn't too far from its home location)</li><BR><BR>
     *
     * @param attacker The L2Character that attacks
     * @param aggro The value of hate to add to the actor against the target
     *
     */
    @Override
    protected void onEvtAggression(L2Character target, int aggro) {
        L2Attackable me = (L2Attackable) _actor;

        if (target != null) {
            // Add the target to the actor _aggroList or update hate if already present
            me.addDamageHate(target, 0, aggro);

            // Set the actor AI Intention to AI_INTENTION_ATTACK
            if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK) {
                // Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
                if (!_actor.isRunning()) {
                    _actor.setRunning();
                }

                setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
            }
        }
    }

    @Override
    protected void onIntentionActive() {
        // Cancel attack timeout
        _attackTimeout = Integer.MAX_VALUE;
        super.onIntentionActive();
    }

    public void setGlobalAggro(int value) {
        _globalAggro = value;
    }

    private boolean outOfChaseRange(L2Spawn spawn, L2Character target) {
        if (spawn == null) {
            return false;
        }

        if (!_actor.checkRange() || _lastChk > System.currentTimeMillis()) {
            return false;
        }

        _lastChk = System.currentTimeMillis() + 3000;
        if (Util.calculateDistance(target.getX(), target.getY(), target.getZ(), spawn.getLocx(), spawn.getLocy(), spawn.getLocz(), true) > _actor.getChaseRange()
                || Util.calculateDistance(spawn.getLocx(), spawn.getLocy(), spawn.getLocz(), _actor.getX(), _actor.getY(), _actor.getZ(), true) > _actor.getChaseRange()) {
            teleToHome(spawn.getLocx(), spawn.getLocy(), spawn.getLocz(), target);
            return true;
        }

        return false;
    }

    private void teleToHome(int x1, int y1, int z1, L2Character target) {
        _actor.stopHating(target);
        setIntention(AI_INTENTION_IDLE);
        _actor.setWalking();
        _actor.teleToLocation(x1, y1, z1, false);
    }
}
