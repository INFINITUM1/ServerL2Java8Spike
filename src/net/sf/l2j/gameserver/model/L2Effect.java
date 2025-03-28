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

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import net.sf.l2j.gameserver.network.serverpackets.MagicEffectIcons;
import net.sf.l2j.gameserver.network.serverpackets.PartySpelled;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.skills.funcs.Lambda;
import net.sf.l2j.gameserver.taskmanager.EffectTaskManager;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.12 $ $Date: 2005/04/11 10:06:07 $
 */
public abstract class L2Effect {

    static final Logger _log = AbstractLogger.getLogger(L2Effect.class.getName());

    public static enum EffectState {

        CREATED, ACTING, FINISHING
    }

    public static enum EffectType {

        SIGNET_EFFECT,
        SIGNET_GROUND,
        BUFF,
        CHARGE,
        DMG_OVER_TIME,
        HEAL_OVER_TIME,
        COMBAT_POINT_HEAL_OVER_TIME,
        MANA_DMG_OVER_TIME,
        MANA_HEAL_OVER_TIME,
        RELAXING, STUN, ROOT,
        SLEEP,
        HATE,
        FAKE_DEATH,
        CONFUSION,
        CONFUSE_MOB_ONLY,
        MUTE,
        IMMOBILEUNTILATTACKED,
        FEAR,
        SILENT_MOVE,
        SEED,
        PARALYZE,
        STUN_SELF,
        PSYCHICAL_MUTE,
        REMOVE_TARGET,
        TARGET_ME,
        SILENCE_MAGIC_PHYSICAL,
        BETRAY,
        NOBLESSE_BLESSING,
        PHOENIX_BLESSING,
        PETRIFICATION,
        BLUFF,
        CHARM_OF_LUCK,
        INVINCIBLE,
        PROTECTION_BLESSING,
        CHANCE_SKILL
    }
    private static final Func[] _emptyFunctionSet = new Func[0];
    //member _effector is the instance of L2Character that cast/used the spell/skill that is
    //causing this effect.  Do not confuse with the instance of L2Character that
    //is being affected by this effect.
    private final L2Character _effector;
    //member _effected is the instance of L2Character that was affected
    //by this effect.  Do not confuse with the instance of L2Character that
    //catsed/used this effect.
    private final L2Character _effected;
    //the skill that was used.
    private final L2Skill _skill;
    //or the items that was used.
    //private final L2Item _item;
    // the value of an update
    private final Lambda _lambda;
    // the current state
    private EffectState _state;
    // period, seconds
    private int _period;
    private int _periodStartTicks;
    private int _periodfirsttime;
    // function templates
    private final FuncTemplate[] _funcTemplates;
    //initial count
    private final int _totalCount;
    // counter
    private int _count;
    // abnormal effect mask
    private final int _abnormalEffect;
    public boolean preventExitUpdate;

    public final class EffectTask implements Runnable {

        protected final int _delay;
        protected final int _rate;

        EffectTask(int pDelay, int pRate) {
            _delay = pDelay;
            _rate = pRate;
        }

        @Override
        public void run() {
            try {
                if (getPeriodfirsttime() == 0) {
                    setPeriodStartTicks(GameTimeController.getGameTicks());
                } else {
                    setPeriodfirsttime(0);
                }
                scheduleEffect();
            } catch (Throwable e) {
                _log.log(Level.SEVERE, "##EffectTask##id" + _skill.getId() + "##" + (_effector == null) + "#" + (_effected == null) + "#" + (_state == null) + "#", e);
            }
        }
    }
    private ScheduledFuture<?> _currentFuture;
    /**
     * The Identifier of the stack group
     */
    private final String _stackType;
    /**
     * The position of the effect in the stack group
     */
    private final float _stackOrder;
    private boolean _inUse = false;
    private final boolean _emptyStack;
    private FastList<String> _stackTypes = new FastList<>();

    protected L2Effect(Env env, EffectTemplate template) {
        this(env, template, false);
    }

    protected L2Effect(Env env, EffectTemplate template, boolean enter) {
        //if (isImunneBuff())
        //return;
        int time = template.period;
        _state = EffectState.CREATED;
        _skill = env.skill;
        //_item = env._item == null ? null : env._item.getItem();
        _effected = env.target;
        _effector = env.cha;
        _lambda = template.lambda;
        _funcTemplates = template.funcTemplates;
        _count = template.counter;
        _totalCount = _count;

        if (_skill.getSkillType() == L2Skill.SkillType.BUFF && env.skillMastery) {
            time *= 2;
        }

        if (_skill.getSkillType() == L2Skill.SkillType.STUN) {
            time /= 2;
        }

        _period = time;
        _abnormalEffect = template.abnormalEffect;
        _stackType = template.stackType;
        _stackOrder = template.stackOrder;
        _periodStartTicks = GameTimeController.getGameTicks();
        _periodfirsttime = 0;
        //
        _emptyStack = template._emptyStack;
        _stackTypes = template._stackTypes;
        //
        //scheduleEffect();
    }

    public boolean hasEmptyStack() {
        return _emptyStack;
    }

    public boolean hasMoreStacks() {
        return _stackTypes.size() > 1;
    }

    public boolean containsStack(String stack) {
        return _stackTypes.contains(stack);
    }

    public boolean isSuperStack() {
        return (_stackOrder >= 56 && _stackOrder <= 65);
    }

    public FastList<String> getStacks() {
        return _stackTypes;
    }

    public int getCount() {
        return _count;
    }

    public int getTotalCount() {
        return _totalCount;
    }

    public void setCount(int newcount) {
        _count = newcount;
    }

    public void setFirstTime(int newfirsttime) {
        _period -= newfirsttime;
        _periodStartTicks = GameTimeController.getGameTicks() - newfirsttime * GameTimeController.TICKS_PER_SECOND;
    }

    private final Lock lock = new ReentrantLock();

    private void startEffectTask(int duration) {
        lock.lock();
        try {
            if (_period > 0) {
                stopEffectTask();
                _currentFuture = EffectTaskManager.getInstance().schedule(new EffectTask(duration, -1), duration);
            }
            if (_state == EffectState.ACTING) {
                _effected.addEffect(this);
            }
        } finally {
            lock.unlock();
        }
    }

    private void startEffectTaskAtFixedRate(int delay, int rate) {
        lock.lock();
        try {
            if (_period > 0) {
                stopEffectTask();
                _currentFuture = EffectTaskManager.getInstance().scheduleAtFixedRate(new EffectTask(delay, rate), delay, rate);
            }
            if (_state == EffectState.ACTING) {
                _effected.addEffect(this);
            }
        } finally {
            lock.unlock();
        }
    }

    public void stopEffectTask() {
        lock.lock();
        try {
            if (_currentFuture == null) {
                return;
            }
            // Cancel the task
            _currentFuture.cancel(false);
            _currentFuture = null;

            _effected.removeEffect(this);
        } finally {
            lock.unlock();
        }
    }
    /*
     private synchronized void startEffectTask(int duration) {
     if (_period > 0) {
     stopEffectTask();
     _currentFuture = EffectTaskManager.getInstance().schedule(new EffectTask(duration, -1), duration);
     }
     if (_state == EffectState.ACTING) {
     _effected.addEffect(this);
     }
     }

     private synchronized void startEffectTaskAtFixedRate(int delay, int rate) {
     if (_period > 0) {
     stopEffectTask();
     _currentFuture = EffectTaskManager.getInstance().scheduleAtFixedRate(new EffectTask(delay, rate), delay, rate);
     }
     if (_state == EffectState.ACTING) {
     _effected.addEffect(this);
     }
     }

     public synchronized void stopEffectTask() {
     if (_currentFuture == null) {
     return;
     }
     // Cancel the task
     _currentFuture.cancel(false);
     _currentFuture = null;

     _effected.removeEffect(this);
     }*/

    public int getPeriod() {
        return _period;
    }

    public int getTime() {
        return (GameTimeController.getGameTicks() - _periodStartTicks)
                / GameTimeController.TICKS_PER_SECOND;
    }

    /**
     * Returns the elapsed time of the task.
     *
     * @return Time in seconds.
     */
    public int getTaskTime() {
        if (_count == _totalCount) {
            return 0;
        }
        return (Math.abs(_count - _totalCount + 1) * _period) + getTime() + 1;
    }

    public boolean getInUse() {
        return _inUse;
    }

    public void setInUse(boolean inUse) {
        _inUse = inUse;
    }

    public String getStackType() {
        return _stackType;
    }

    public float getStackOrder() {
        return _stackOrder;
    }

    public final L2Skill getSkill() {
        return _skill;
    }

    public final L2Character getEffector() {
        return _effector;
    }

    public final L2Character getEffected() {
        return _effected;
    }

    public boolean isSelfEffect() {
        return _skill._effectTemplatesSelf != null;
    }

    public boolean isHerbEffect() {
        return getSkill().getName().contains("Herb");
    }

    public final double calc() {
        Env env = new Env();
        env.cha = _effector;
        env.target = _effected;
        env.skill = _skill;
        return _lambda.calc(env);
    }

    /**
     * Stop the L2Effect task and send Server->Client update packet.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Cancel the effect in the the
     * abnormal effect map of the L2Character </li> <li>Stop the task of the
     * L2Effect, remove it and update client magic icone </li><BR><BR>
     *
     */
    public final void exit() {
        this.exit(false);
    }

    public final void exit(boolean preventUpdate) {
        preventExitUpdate = preventUpdate;
        _state = EffectState.FINISHING;
        scheduleEffect();
    }

    /**
     * returns effect type
     */
    public abstract EffectType getEffectType();

    /**
     * Notify started
     */
    public void onStart() {
        if (_abnormalEffect != 0) {
            getEffected().startAbnormalEffect(_abnormalEffect);
        }
    }

    /**
     * Cancel the effect in the the abnormal effect map of the effected
     * L2Character.<BR><BR>
     */
    public void onExit() {
        if (_abnormalEffect != 0) {
            getEffected().stopAbnormalEffect(_abnormalEffect);
        }
    }

    /**
     * Return true for continueation of this effect
     */
    public abstract boolean onActionTime();

    public final void rescheduleEffect() {
        if (_state != EffectState.ACTING) {
            scheduleEffect();
        } else {
            if (_count > 1) {
                startEffectTaskAtFixedRate(5, _period * 1000);
                return;
            }
            if (_period > 0) {
                startEffectTask(_period * 1000);
            }
        }
    }

    public final void scheduleEffect() {
        //if (isImunneBuff())
        //return;

        if (_state == EffectState.CREATED) {
            _state = EffectState.ACTING;
            onStart();

            if (_skill.isPvpSkill()) {
                getEffected().sendPacket(SystemMessage.id(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(_skill.getDisplayId()));
            }

            if (_count > 1) {
                if (_skill.getId() == 337) {
                    startEffectTaskAtFixedRate(1511, _period * 1000);
                } else {
                    startEffectTaskAtFixedRate(5, _period * 1000);
                }
                return;
            }
            if (_period > 0) {
                startEffectTask(_period * 1000);
                return;
            }
        }

        if (_state == EffectState.ACTING) {
            if (_count-- > 0) {
                if (getInUse()) { // effect has to be in use
                    if (onActionTime()) {
                        return; // false causes effect to finish right away
                    }
                } else if (_count > 0) { // do not finish it yet, in case reactivated
                    return;
                }
            }
            _state = EffectState.FINISHING;
        }

        if (_state == EffectState.FINISHING) {
            // Cancel the effect in the the abnormal effect map of the L2Character
            onExit();

            //If the time left is equal to zero, send the message
            if (_count == 0) {
                getEffected().sendPacket(SystemMessage.id(SystemMessageId.S1_HAS_WORN_OFF).addString(_skill.getName()));
            }
            // Stop the task of the L2Effect, remove it and update client magic icone
            stopEffectTask();
        }
    }

    public Func[] getStatFuncs() {
        if (_funcTemplates == null) {
            return _emptyFunctionSet;
        }
        List<Func> funcs = new FastList<>();
        for (FuncTemplate t : _funcTemplates) {
            Env env = new Env();
            env.cha = getEffector();
            env.target = getEffected();
            env.skill = getSkill();
            Func f = t.getFunc(env, this); // effect is owner
            if (f != null) {
                funcs.add(f);
            }
        }
        if (funcs.isEmpty()) {
            return _emptyFunctionSet;
        }
        return funcs.toArray(new Func[funcs.size()]);
    }

    /*
     * public final void addIcon(AbnormalStatusUpdate mi) { EffectTask task =
     * _currentTask; ScheduledFuture<?> future = _currentFuture; if (task ==
     * null || future == null) return; if (_state == EffectState.FINISHING ||
     * _state == EffectState.CREATED) return; L2Skill sk = getSkill(); if
     * (task._rate > 0) { if (sk.isPotion()) mi.addEffect(sk.getId(),
     * getLevel(), sk.getBuffDuration() - (getTaskTime() * 1000)); else
     * mi.addEffect(sk.getId(), getLevel(), -1); } else mi.addEffect(sk.getId(),
     * getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
     }
     */
    public final void addIcon(MagicEffectIcons mi) {
        if (_state != EffectState.ACTING) {
            return;
        }

        ScheduledFuture<?> future = _currentFuture;
        if (future == null) {
            return;
        }

        if (_totalCount <= 1) {
            mi.addEffect(getSkill().getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
            return;
        }

        L2Skill sk = getSkill();
        mi.addEffect(sk.getId(), getLevel(), sk.isPotion() ? (sk.getBuffDuration() - (getTaskTime() * 1000)) : -1);
    }

    public final void addPartySpelledIcon(PartySpelled ps) {
        if (_state != EffectState.ACTING) {
            return;
        }

        ScheduledFuture<?> future = _currentFuture;
        if (future == null) {
            return;
        }

        ps.addPartySpelledEffect(getSkill().getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
    }

    public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os) {
        if (_state != EffectState.ACTING) {
            return;
        }

        ScheduledFuture<?> future = _currentFuture;
        if (future == null) {
            return;
        }

        os.addEffect(getSkill().getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
    }

    public int getLevel() {
        return getSkill().getLevel();
    }

    public int getId() {
        return getSkill().getId();
    }

    public int getPeriodfirsttime() {
        return _periodfirsttime;
    }

    public void setPeriodfirsttime(int periodfirsttime) {
        _periodfirsttime = periodfirsttime;
    }

    public int getPeriodStartTicks() {
        return _periodStartTicks;
    }

    public void setPeriodStartTicks(int periodStartTicks) {
        _periodStartTicks = periodStartTicks;
    }

    private boolean isImunneBuff() {
        /*
         * if (getSkill().getId() == 2276) return false;
         */

        if (_effected.getTarget() == _effected) {
            return false;
        }

        if (/*
                 * (getSkill().getSkillType() == L2Skill.SkillType.BUFF &&
                 * _effected.getFirstEffect(2276) != null) ||
                 */_effected.getFirstEffect(1411) != null) {
            return true;
        }

        return false;
    }

    public boolean isStoreable() {
        return !(_skill.isToggle()
                || getEffectType() == EffectType.HEAL_OVER_TIME
                || getEffectType() == EffectType.COMBAT_POINT_HEAL_OVER_TIME
                || getEffectType() == EffectType.MANA_HEAL_OVER_TIME);
    }
}
