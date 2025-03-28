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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Character.AIAccessor;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

public class L2PartnerAI extends L2CharacterAI {

    private boolean _thinking; // to prevent recursive thinking
    private boolean _startFollow = _actor.getFollowStatus();
    private RunOnAttacked _runOnAttacked;
    private ScheduledFuture<?> _runOnAttackedTask;

    public L2PartnerAI(AIAccessor accessor) {
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
        //L2PcInstance summon = _actor.getPartner();
        if (_startFollow) {
            setIntention(AI_INTENTION_FOLLOW, _actor.getOwner());
        } else {
            super.onIntentionActive();
        }
    }

    private void thinkAttack() {
        L2Character target = getAttackTarget();
        if (target == null) {
            return;
        }

        if (checkTargetLostOrDead(target)) {
            setAttackTarget(null);
            return;
        }

        if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange())) {
            return;
        }
        clientStopMoving(null);

        if (_actor.getOwner() != null) {
            _actor.getOwner().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, target);
        }

        switch (_actor.getPartnerClass()) {
            case 1:
                archerAtatck(target);
                _accessor.doAttack(target);
                break;
            default:
                _accessor.doAttack(target);
                break;
        }

        return;
    }

    private class AttackTask implements Runnable {

        public AttackTask() {
        }

        @Override
        public void run() {
            thinkAttack();
        }
    }

    private void archerAtatck(L2Character target) {
        if (Rnd.get(100) < 45) {
            switch (Rnd.get(10)) {
                case 1:
                    _accessor.doCast(SkillTable.getInstance().getInfo(101, 40));
                    break;
                case 2:
                    _accessor.doCast(SkillTable.getInstance().getInfo(19, 37));
                    break;
                case 3:
                    _accessor.doCast(SkillTable.getInstance().getInfo(354, 1));
                    break;
            }
            _actor.setTarget(target);
            ThreadPoolManager.getInstance().scheduleAi(new AttackTask(), 900, true);
            return;
        }
    }

    private void thinkCast() {
        //L2PcInstance summon = _actor.getPartner();
        if (checkTargetLost(getCastTarget())) {
            setCastTarget(null);
            return;
        }
        boolean val = _startFollow;
        if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill))) {
            return;
        }
        clientStopMoving(null);
        _actor.setFollowStatus(false);
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
    public void onOwnerGotAttacked(L2Character attacker) {
        L2PcInstance owner = _actor.getOwner();
        // check if the owner is no longer around...if so, despawn
        if ((owner == null) || (owner.isOnline() == 0)) {
            _actor.logout();
            return;
        }
        // if the owner is too far away, stop anything else and immediately run towards the owner.
        if (!owner.isInsideRadius(_actor, 1200, true, true)) {
            startFollow(owner);
            return;
        }
        // if the owner is dead, do nothing...
        if (owner.isDead()) {
            return;
        }

        // if the tamed beast is currently in the middle of casting, let it complete its skill...
        if (_actor.isCastingNow()) {
            return;
        }

        if (getIntention() != AI_INTENTION_ATTACK) {
            _actor.setTarget(attacker);
            setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
        }
    }

    @Override
    protected void onEvtAttacked(L2Character attacker) {
        //L2PcInstance actor = _actor.getPartner();
        if (_actor == null) {
            return;
        }

        if (_runOnAttacked != null) {
            _runOnAttacked.setAttacker(attacker);
        }

        if (_actor.getOwner() != null) {
            _actor.getOwner().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getAttackTarget());
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
            //L2PcInstance actor = _actor.getPartner();
            if (_actor == null) {
                return;
            }

            if (_attacker != null && _actor.getOwner() != null && _lastAttack + 20000 > System.currentTimeMillis() && (_intention == AI_INTENTION_FOLLOW || _intention == AI_INTENTION_IDLE || _intention == AI_INTENTION_ACTIVE)) {
                if (!_clientMoving) {
                    int posX = _actor.getOwner().getX();
                    int posY = _actor.getOwner().getY();
                    int posZ = _actor.getOwner().getZ();

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
                    _actor.setRunning();
                    _actor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, _actor.calcHeading(posX, posY)));
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

    @Override
    public int getPAtk() {
        switch (_actor.getPartnerClass()) {
            case 1:
                return Config.FAKE_MAX_PATK_BOW;
            case 3:
                return Config.FAKE_MAX_PATK_MAG;
            case 4:
                return Config.FAKE_MAX_PATK_HEAL;
            default:
                return 2000;
        }
    }

    @Override
    public int getMDef() {
        switch (_actor.getPartnerClass()) {
            case 1:
                return Config.FAKE_MAX_MDEF_BOW;
            case 3:
                return Config.FAKE_MAX_MDEF_MAG;
            case 4:
                return Config.FAKE_MAX_MDEF_HEAL;
            default:
                return 2300;
        }
    }

    @Override
    public int getPAtkSpd() {
        switch (_actor.getPartnerClass()) {
            case 1:
                return Config.FAKE_MAX_PSPD_BOW;
            case 3:
                return Config.FAKE_MAX_PSPD_MAG;
            case 4:
                return Config.FAKE_MAX_PSPD_HEAL;
            default:
                return 400;
        }
    }

    @Override
    public int getPDef() {
        switch (_actor.getPartnerClass()) {
            case 1:
                return Config.FAKE_MAX_PDEF_BOW;
            case 3:
                return Config.FAKE_MAX_PDEF_MAG;
            case 4:
                return Config.FAKE_MAX_PDEF_HEAL;
            default:
                return 2600;
        }
    }

    @Override
    public int getMAtk() {
        switch (_actor.getPartnerClass()) {
            case 1:
                return Config.FAKE_MAX_MATK_BOW;
            case 3:
                return Config.FAKE_MAX_MATK_MAG;
            case 4:
                return Config.FAKE_MAX_MATK_HEAL;
            default:
                return 5600;
        }
    }
    
    @Override
    public int getMAtkSpd() {
        switch (_actor.getPartnerClass()) {
            case 1:
                return Config.FAKE_MAX_MSPD_BOW;
            case 3:
                return Config.FAKE_MAX_MSPD_MAG;
            case 4:
                return Config.FAKE_MAX_MSPD_HEAL;
            default:
                return 400;
        }
    }

    @Override
    public int getMaxHp() {
        switch (_actor.getPartnerClass()) {
            case 1:
                return Config.FAKE_MAX_HP_BOW;
            case 3:
                return Config.FAKE_MAX_HP_MAG;
            case 4:
                return Config.FAKE_MAX_HP_HEAL;
            default:
                return 4000;
        }
    }
}
