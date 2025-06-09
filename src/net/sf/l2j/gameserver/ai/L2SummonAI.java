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

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Character.AIAccessor;
import net.sf.l2j.util.Rnd;

public class L2SummonAI extends L2CharacterAI {

    private boolean _thinking; // to prevent recursive thinking
    private boolean _startFollow = _actor.getFollowStatus();
    private RunOnAttacked _runOnAttacked;
    private ScheduledFuture<?> _runOnAttackedTask;

    public L2SummonAI(AIAccessor accessor) {
        super(accessor);
    }

    @Override
    protected void onIntentionIdle() {
        stopFollow();
        _startFollow = false;
        onIntentionActive();
    }

    @Override
    protected void onIntentionActive() {
        L2Summon summon = _actor.getL2Summon();
        if (_startFollow) {
            setIntention(AI_INTENTION_FOLLOW, summon.getOwner());
        } else {
            super.onIntentionActive();
        }
    }

    private void thinkAttack() {
        if (checkTargetLostOrDead(getAttackTarget())) {
            setAttackTarget(null);
            return;
        }
        if (maybeMoveToPawn(getAttackTarget(), _actor.getPhysicalAttackRange())) {
            return;
        }
        clientStopMoving(null);

        if (_actor.getOwner() != null) {
            _actor.getOwner().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getAttackTarget());
        }

        _accessor.doAttack(getAttackTarget());
        return;
    }

    private void thinkCast() {
        L2Summon summon = _actor.getL2Summon();
        if (checkTargetLost(getCastTarget())) {
            setCastTarget(null);
            return;
        }
        boolean val = _startFollow;
        if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill))) {
            return;
        }
        clientStopMoving(null);
        summon.setFollowStatus(false);
        setIntention(AI_INTENTION_IDLE);
        _startFollow = val;
        _accessor.doCast(_skill);
    }

    private void thinkPickUp() {
        if (checkTargetLost(getTarget())) {
            return;
        }
        if (maybeMoveToPawn(getTarget(), 36)) {
            return;
        }
        setIntention(AI_INTENTION_IDLE);
        ((L2Summon.AIAccessor) _accessor).doPickupItem(getTarget());
        return;
    }

    private void thinkInteract() {
        if (_actor.isAllSkillsDisabled()) {
            return;
        }
        if (checkTargetLost(getTarget())) {
            return;
        }
        if (maybeMoveToPawn(getTarget(), 36)) {
            return;
        }
        setIntention(AI_INTENTION_IDLE);
    }

    @Override
    protected void onEvtThink() {
        if (_thinking || _actor.isAllSkillsDisabled()) {
            return;
        }
        _thinking = true;
        try {
            switch (getIntention()) {
                case AI_INTENTION_ATTACK:
                    thinkAttack();
                    break;
                case AI_INTENTION_CAST:
                    thinkCast();
                    break;
                case AI_INTENTION_PICK_UP:
                    thinkPickUp();
                    break;
                case AI_INTENTION_INTERACT:
                    thinkInteract();
                    break;
            }
        } finally {
            _thinking = false;
        }
    }

    @Override
    protected void onEvtFinishCasting() {
        if (_actor.getAI().getIntention() != AI_INTENTION_ATTACK) {
            _actor.setFollowStatus(_startFollow);
        }
    }

    public void notifyFollowStatusChange() {
        _startFollow = !_startFollow;
        switch (getIntention()) {
            case AI_INTENTION_ACTIVE:
            case AI_INTENTION_FOLLOW:
            case AI_INTENTION_IDLE:
                _actor.setFollowStatus(_startFollow);
        }
    }

    public void setStartFollowController(boolean val) {
        _startFollow = val;
    }

    @Override
    protected void onEvtAttacked(L2Character attacker) {
        L2Summon actor = _actor.getL2Summon();
        if (actor == null) {
            return;
        }

        if (_runOnAttacked != null) {
            _runOnAttacked.setAttacker(attacker);
        }

        if (actor.getOwner() != null) {
            actor.getOwner().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getAttackTarget());
        }

        if (_runOnAttacked == null && (_intention == AI_INTENTION_FOLLOW || _intention == AI_INTENTION_IDLE || _intention == AI_INTENTION_ACTIVE) && !_clientMoving/* && attacker != actor.getOwner()*/) {
            if (_runOnAttacked == null) {
                _runOnAttacked = new RunOnAttacked();
                _runOnAttacked.setAttacker(attacker);
            }

            if (_runOnAttackedTask == null) {
                _runOnAttackedTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(_runOnAttacked, 0, 500);
            }
        }

        super.onEvtAttacked(attacker);
    }

    private class RunOnAttacked implements Runnable {

        private L2Character _attacker;
        private long _lastAttack;

        public void run() {
            L2Summon actor = _actor.getL2Summon();
            if (actor == null) {
                return;
            }

            if (_attacker != null && actor.getOwner() != null && _lastAttack + 20000 > System.currentTimeMillis() && (_intention == AI_INTENTION_FOLLOW || _intention == AI_INTENTION_IDLE || _intention == AI_INTENTION_ACTIVE)) {
                if (!_clientMoving) {
                    int posX = actor.getOwner().getX();
                    int posY = actor.getOwner().getY();
                    int posZ = actor.getOwner().getZ();

                    int side = Rnd.get(1, 6);
                    switch (side) {
                        case 1:
                            posX += 30;
                            posY += 140;
                            break;
                        case 2:
                            posX += 150;
                            posY += 50;
                            break;
                        case 3:
                            posX += 70;
                            posY -= 100;
                            break;
                        case 4:
                            posX += 5;
                            posY -= 100;
                            break;
                        case 5:
                            posX -= 150;
                            posY -= 20;
                            break;
                        case 6:
                            posX -= 100;
                            posY += 50;
                            break;
                    }
                    actor.setRunning();
                    actor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, actor.calcHeading(posX, posY)));
                }
            } else {
                _attacker = null;
                if (_runOnAttackedTask != null) {
                    _runOnAttackedTask.cancel(true);
                }

                _runOnAttackedTask = null;
                _runOnAttacked = null;
            }
        }

        public void setAttacker(L2Character attacker) {
            _attacker = attacker;
            _lastAttack = System.currentTimeMillis();
        }
    }
}
