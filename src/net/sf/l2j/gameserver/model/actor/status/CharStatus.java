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
package net.sf.l2j.gameserver.model.actor.status;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.stat.CharStat;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.taskmanager.EffectTaskManager;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;

public class CharStatus {

    protected static final Logger _log = AbstractLogger.getLogger(CharStatus.class.getName());
    // =========================================================
    // Data Field
    private L2Character _activeChar;
    private volatile double _currentCp = 0; //Current CP of the L2Character
    private volatile double _currentHp = 0; //Current HP of the L2Character
    private volatile double _currentMp = 0; //Current MP of the L2Character
    /** Array containing all clients that need to be notified about hp/mp updates of the L2Character */
    private CopyOnWriteArraySet<L2Character> _statusListener;
    private final Object statusListenerLock = new Object();
    private Future<?> _regTask;
    private byte _flagsRegenActive = 0;
    private static final byte REGEN_FLAG_CP = 4;
    private static final byte REGEN_FLAG_HP = 1;
    private static final byte REGEN_FLAG_MP = 2;

    // =========================================================
    // Constructor
    public CharStatus(L2Character activeChar) {
        _activeChar = activeChar;
    }

    /**
     * Add the object to the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.
     * Players who must be informed are players that target this L2Character.
     * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR>
     * <li> Target a PC or NPC</li><BR><BR>
     *
     * @param object L2Character to add to the listener
     *
     */
    public final void addStatusListener(L2Character object) {
        if (object.equals(_activeChar)) {
            return;
        }

        CopyOnWriteArraySet<L2Character> listeners;
        synchronized (statusListenerLock) {
            if (_statusListener == null) {
                _statusListener = new CopyOnWriteArraySet<L2Character>();
            }

            listeners = _statusListener;
        }
        listeners.add(object);
    }

    public final void removeStatusListener(L2Character object) {
        synchronized (statusListenerLock) {
            if (getStatusListener() == null) {
                return;
            }

            getStatusListener().remove(object);
            if (getStatusListener() != null && getStatusListener().isEmpty()) {
                setStatusListener(null);
            }
        }
    }

    private void setStatusListener(final CopyOnWriteArraySet<L2Character> value) {
        _statusListener = value;
    }

    public final void reduceCp(int value) {
        if (getCurrentCp() > value) {
            setCurrentCp(getCurrentCp() - value);
        } else {
            setCurrentCp(0);
        }
    }

    /**
     * Reduce the current HP of the L2Character and launch the doDie Task if necessary.<BR><BR>
     *
     * <B><U> Overriden in </U> :</B><BR><BR>
     * <li> L2Attackable : Update the attacker AggroInfo of the L2Attackable _aggroList</li><BR><BR>
     *
     * @param i The HP decrease value
     * @param attacker The L2Character who attacks
     * @param awake The awake state (If True : stop sleeping)
     *
     */
    public void reduceHp(double value, L2Character attacker) {
        reduceHp(value, attacker, true);
    }

    public void reduceHp(double value, L2Character attacker, boolean awake) {
        if (_activeChar.isInvul() || _activeChar.isDead()) {
            return;
        }

        if (_activeChar.isPlayer()) {
            if (_activeChar.getDuel() != null) {
                // the duel is finishing - players do not recive damage
                if (_activeChar.getDuel().getDuelState(_activeChar.getPlayer()) == DuelState.Dead) {
                    return;
                } else if (_activeChar.getDuel().getDuelState(_activeChar.getPlayer()) == DuelState.Winner) {
                    return;
                }

                // cancel duel if player got hit by another player, that is not part of the duel or a monster
                if (!(attacker.isSummon()) && !(attacker.isPlayer() && attacker.getDuel() == _activeChar.getDuel())) {
                    _activeChar.getDuel().setDuelState(_activeChar.getPlayer(), DuelState.Interrupted);
                }
            }
        } else {
            if (attacker.isPlayer() && attacker.isInDuel()
                    && !(_activeChar.isSummon()
                    && _activeChar.getOwner().getDuel() == attacker.getDuel())) // Duelling player attacks mob
            {
                attacker.getDuel().setDuelState(attacker.getPlayer(), DuelState.Interrupted);
            }
        }

        if (awake && _activeChar.isSleeping()) {
            _activeChar.stopSleeping(null);
        }

        if (_activeChar.isStunned() && Rnd.get(10) == 0) {
            _activeChar.stopStunning(null);
        }

        // Add attackers to npc's attacker list
        if (_activeChar.isL2Npc()) {
            _activeChar.addAttackerToAttackByList(attacker);
        }

        if (value > 0) // Reduce Hp if any
        {
            // If we're dealing with an L2Attackable Instance and the attacker hit it with an over-hit enabled skill, set the over-hit values.
            // Anything else, clear the over-hit flag
            if (_activeChar.isL2Attackable()) {
                if (((L2Attackable) _activeChar).isOverhit()) {
                    ((L2Attackable) _activeChar).setOverhitValues(attacker, value);
                } else {
                    ((L2Attackable) _activeChar).overhitEnabled(false);
                }
            }
            value = getCurrentHp() - value;             // Get diff of Hp vs value
            if (value <= 0) {
                // is the dieing one a duelist? if so change his duel state to dead
                if (_activeChar.isPlayer()) {
                    boolean pvp = false;
                    L2PcInstance player = _activeChar.getPlayer();
                    if (player.isInDuel()) {
                        player.getDuel().onPlayerDefeat(player);
                        pvp = true;
                    }

                    if (pvp) {
                        stopHpMpRegeneration();
                        _activeChar.setIsKilledAlready(true);
                        _activeChar.setIsPendingRevive(true);
                        value = 1;
                    } else {
                        value = 0;
                    }
                } else {
                    value = 0;                         // Set value to 0 if Hp < 0
                }
            }
            setCurrentHp(value);                        // Set Hp
        } else {
            // If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
            if (_activeChar.isL2Attackable()) {
                ((L2Attackable) _activeChar).overhitEnabled(false);
            }
        }

        if (_activeChar.isDead()) {
            _activeChar.abortAttack();
            _activeChar.abortCast();

            if (_activeChar.isPlayer()) {
                if (_activeChar.isInOlympiadMode()) {
                    stopHpMpRegeneration();
                    _activeChar.setIsKilledAlready(true);
                    _activeChar.setIsPendingRevive(true);
                    if (_activeChar.getPet() != null) {
                        _activeChar.getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
                    }
                    return;
                }
            }

            // first die (and calculate rewards), if currentHp < 0,
            // then overhit may be calculated
            //if (Config.DEBUG) _log.fine("char is dead.");

            // Start the doDie process
            _activeChar.doDie(attacker);

            // now reset currentHp to zero
            setCurrentHp(0);
        } else {
            // If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
            if (_activeChar.isL2Attackable()) {
                ((L2Attackable) _activeChar).overhitEnabled(false);
            }
        }
    }

    public final void reduceMp(double value) {
        value = getCurrentMp() - value;
        if (value < 0) {
            value = 0;
        }
        setCurrentMp(value);
    }

    /**
     * Start the HP/MP/CP Regeneration task.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Calculate the regen task period </li>
     * <li>Launch the HP/MP/CP Regeneration task with Medium priority </li><BR><BR>
     *
     */
    public synchronized final void startHpMpRegeneration() {
        if (_activeChar.isPcNpc()) {
            return;
        }
        if (_regTask == null && !getActiveChar().isDead()) {
            // Create the HP/MP/CP Regeneration task
            _regTask = EffectTaskManager.getInstance().scheduleAtFixedRate(new RegenTask(), getActiveChar().getRegeneratePeriod(), getActiveChar().getRegeneratePeriod());
        }
    }

    /**
     * Stop the HP/MP/CP Regeneration task.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Set the RegenActive flag to False </li>
     * <li>Stop the HP/MP/CP Regeneration task </li><BR><BR>
     *
     */
    public synchronized final void stopHpMpRegeneration() {
        if (_regTask != null) {
            //if (Config.DEBUG) _log.fine("HP/MP/CP regen stop");

            // Stop the HP/MP/CP Regeneration task
            _regTask.cancel(false);
            _regTask = null;

            // Set the RegenActive flag to false
            _flagsRegenActive = 0;
        }
    }

    // =========================================================
    // Method - Private
    // =========================================================
    // Property - Public
    public L2Character getActiveChar() {
        return _activeChar;
    }

    public final double getCurrentCp() {
        return _currentCp;
    }

    public final void setCurrentCp(double newCp) {
        setCurrentCp(newCp, true);
    }

    public final void setCurrentCp(double newCp, boolean broadcastPacket) {
        synchronized (this) {
            //if (getActiveChar().isDead())
            //return;

            // Get the Max CP of the L2Character
            int maxCp = _activeChar.getStat().getMaxCp();

            if (newCp < 0) {
                newCp = 0;
            }

            if (newCp >= maxCp) {
                // Set the RegenActive flag to false
                _currentCp = maxCp;
                /*if (getActiveChar().isPlayer())
                {
                if (_currentCp > 25000)
                _currentCp = 25000;
                }*/
                _flagsRegenActive &= ~REGEN_FLAG_CP;

                // Stop the HP/MP/CP Regeneration task
                if (_flagsRegenActive == 0) {
                    stopHpMpRegeneration();
                }
            } else {
                // Set the RegenActive flag to true
                _currentCp = newCp;
                /*if (getActiveChar().isPlayer())
                {
                if (_currentCp > 25000)
                _currentCp = 25000;
                }*/
                _flagsRegenActive |= REGEN_FLAG_CP;

                // Start the HP/MP/CP Regeneration task with Medium priority
                startHpMpRegeneration();
            }
        }

        // Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        if (broadcastPacket) {
            _activeChar.broadcastStatusUpdate();
            //_activeChar.sendChanges();
        }
    }

    public final double getCurrentHp() {
        return _currentHp;
    }

    public final void setCurrentHp(double newHp) {
        setCurrentHp(newHp, true);
    }

    public final void setCurrentHp(double newHp, boolean broadcastPacket) {
        newHp = Math.min(_activeChar.getMaxHp(), Math.max(0, newHp));

        if (_currentHp == newHp) {
            return;
        }

        double startHp = _currentHp;
        synchronized (this) {
            //if (getActiveChar().isDead())
            //return;

            // Get the Max HP of the L2Character
            double maxHp = _activeChar.getMaxHp();

            if (newHp >= maxHp) {
                // Set the RegenActive flag to false
                _currentHp = maxHp;

                _flagsRegenActive &= ~REGEN_FLAG_HP;
                _activeChar.setIsKilledAlready(false);

                // Stop the HP/MP/CP Regeneration task
                if (_flagsRegenActive == 0) {
                    stopHpMpRegeneration();
                }
            } else {
                // Set the RegenActive flag to true
                _currentHp = newHp;

                _flagsRegenActive |= REGEN_FLAG_HP;
                if (!_activeChar.isDead()) {
                    _activeChar.setIsKilledAlready(false);
                }

                // Start the HP/MP/CP Regeneration task with Medium priority
                startHpMpRegeneration();
            }
        }

        _activeChar.checkHpMessages(startHp, newHp);

        // Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        //if (broadcastPacket)
        //getActiveChar().broadcastStatusUpdate();
        _activeChar.broadcastStatusUpdate();
        //_activeChar.sendChanges();
    }

    public final void setCurrentHpMp(double newHp, double newMp) {
        setCurrentHp(newHp, false);
        setCurrentMp(newMp, true); //send the StatusUpdate only once
    }

    public final double getCurrentMp() {
        return _currentMp;
    }

    public final void setCurrentMp(double newMp) {
        setCurrentMp(newMp, true);
    }

    public final void setCurrentMp(double newMp, boolean broadcastPacket) {
        synchronized (this) {
            //if (getActiveChar().isDead())
            //return;

            // Get the Max MP of the L2Character
            int maxMp = _activeChar.getStat().getMaxMp();

            if (newMp >= maxMp) {
                // Set the RegenActive flag to false
                _currentMp = maxMp;
                _flagsRegenActive &= ~REGEN_FLAG_MP;

                // Stop the HP/MP/CP Regeneration task
                if (_flagsRegenActive == 0) {
                    stopHpMpRegeneration();
                }
            } else {
                // Set the RegenActive flag to true
                _currentMp = newMp;
                _flagsRegenActive |= REGEN_FLAG_MP;

                // Start the HP/MP/CP Regeneration task with Medium priority
                startHpMpRegeneration();
            }
        }

        // Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        if (broadcastPacket) {
            _activeChar.broadcastStatusUpdate();
            //_activeChar.sendChanges();
        }
    }

    /**
     * Return the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.
     * Players who must be informed are players that target this L2Character.
     * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<BR><BR>
     *
     * @return The list of L2Character to inform or null if empty
     *
     */
    public final CopyOnWriteArraySet<L2Character> getStatusListener() {
        if (_statusListener == null) {
            _statusListener = new CopyOnWriteArraySet<L2Character>();
        }
        return _statusListener;
    }

    // =========================================================
    // Runnable
    /** Task of HP/MP/CP regeneration */
    class RegenTask implements Runnable {

        public void run() {
            try {
                CharStat charstat = _activeChar.getStat();

                // Modify the current CP of the L2Character and broadcast Server->Client packet StatusUpdate
                if (getCurrentCp() < charstat.getMaxCp()) {
                    setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(_activeChar), false);
                }

                // Modify the current HP of the L2Character and broadcast Server->Client packet StatusUpdate
                if (getCurrentHp() < charstat.getMaxHp()) {
                    setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(_activeChar), false);
                }

                // Modify the current MP of the L2Character and broadcast Server->Client packet StatusUpdate
                if (getCurrentMp() < charstat.getMaxMp()) {
                    setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(_activeChar), false);
                }

                if (!_activeChar.isInActiveRegion()) {
                    // no broadcast necessary for characters that are in inactive regions.
                    // stop regeneration for characters who are filled up and in an inactive region.
                    if ((getCurrentCp() == charstat.getMaxCp()) && (getCurrentHp() == charstat.getMaxHp()) && (getCurrentMp() == charstat.getMaxMp())) {
                        stopHpMpRegeneration();
                    }
                } else {
                    _activeChar.broadcastStatusUpdate(); //send the StatusUpdate packet
                    _activeChar.sendChanges();
                }
            } catch (Throwable e) {
                _log.log(Level.SEVERE, "class RegenTask implements Runnable", e);
                e.printStackTrace();
            }
        }
    }
    
    public void reduceNpcHp(double value, L2Character attacker, boolean awake) {
    }
}