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
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_REST;

import java.util.EmptyStackException;
import java.util.Stack;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Character.AIAccessor;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.ObjectKnownList.KnownListAsynchronousUpdateTask;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.util.Rnd;

public class L2PlayerFakeAI extends L2CharacterAI {

    private boolean _thinking; // to prevent recursive thinking


    static class IntentionCommand {

        protected CtrlIntention _crtlIntention;
        protected Object _arg0, _arg1;

        protected IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1) {
            _crtlIntention = pIntention;
            _arg0 = pArg0;
            _arg1 = pArg1;
        }
    }
    private Stack<IntentionCommand> _interuptedIntentions = new Stack<IntentionCommand>();

    public L2PlayerFakeAI(AIAccessor accessor) {
        super(accessor);
    }

    /**
     * Saves the current Intention for this L2PlayerAI if necessary and calls changeIntention in AbstractAI.<BR><BR>
     *
     * @param intention The new Intention to set to the AI
     * @param arg0 The first parameter of the Intention
     * @param arg1 The second parameter of the Intention
     *
     */
    @Override
    synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1) {
        /*
        if (Config.DEBUG)
        _log.warning("L2PlayerAI: changeIntention -> " + intention + " " + arg0 + " " + arg1);
         */

        // nothing to do if it does not CAST intention
        if (intention != AI_INTENTION_CAST) {
            super.changeIntention(intention, arg0, arg1);
            return;
        }

        // do nothing if next intention is same as current one.
        if (intention == _intention && arg0 == _intentionArg0 && arg1 == _intentionArg1) {
            super.changeIntention(intention, arg0, arg1);
            return;
        }

        /*
        if (Config.DEBUG)
        _log.warning("L2PlayerAI: changeIntention -> Saving current intention: " + _intention + " " + _intention_arg0 + " " + _intention_arg1);
         */

        // push current intention to stack
        _interuptedIntentions.push(new IntentionCommand(_intention, _intentionArg0, _intentionArg1));
        super.changeIntention(intention, arg0, arg1);
    }

    /**
     * Finalize the casting of a skill. This method overrides L2CharacterAI method.<BR><BR>
     *
     * <B>What it does:</B>
     * Check if actual intention is set to CAST and, if so, retrieves latest intention
     * before the actual CAST and set it as the current intention for the player
     */
    @Override
    protected void onEvtFinishCasting() {
        // forget interupted actions after offensive skill
        if (_skill != null && _skill.isOffensive()) {
            _interuptedIntentions.clear();
        }

        if (getIntention() == AI_INTENTION_CAST) {
            // run interupted intention if it remain.
            if (!_interuptedIntentions.isEmpty()) {
                IntentionCommand cmd = null;
                try {
                    cmd = _interuptedIntentions.pop();
                } catch (EmptyStackException ese) {
                }

                /*
                if (Config.DEBUG)
                _log.warning("L2PlayerAI: onEvtFinishCasting -> " + cmd._intention + " " + cmd._arg0 + " " + cmd._arg1);
                 */

                if (cmd != null && cmd._crtlIntention != AI_INTENTION_CAST) // previous state shouldn't be casting
                {
                    setIntention(cmd._crtlIntention, cmd._arg0, cmd._arg1);
                } else {
                    setIntention(AI_INTENTION_IDLE);
                }
            } else {
                /*
                if (Config.DEBUG)
                _log.warning("L2PlayerAI: no previous intention set... Setting it to IDLE");
                 */
                // set intention to idle if skill doesn't change intention.
                setIntention(AI_INTENTION_IDLE);
            }
        }
    }

    @Override
    protected void onIntentionRest() {
        if (getIntention() != AI_INTENTION_REST) {
            changeIntention(AI_INTENTION_REST, null, null);
            setTarget(null);
            if (getAttackTarget() != null) {
                setAttackTarget(null);
            }
            clientStopMoving(null);
        }
    }

    @Override
    protected void onIntentionActive() {
        setIntention(AI_INTENTION_IDLE);
        _actor.clearRndWalk();
    }

    private void thinkCast() {

        L2Character target = getCastTarget();
        //if (Config.DEBUG) _log.warning("L2PlayerAI: thinkCast -> Start");

        if (_skill.getTargetType() == SkillTargetType.TARGET_SIGNET_GROUND && _actor.isPlayer()) {
            if (maybeMoveToPosition(_actor.getPlayer().getCurrentSkillWorldPosition(), _actor.getMagicalAttackRange(_skill))) {
                return;
            }
        } else {
            if (checkTargetLost(target)) {
                if (_skill.isOffensive() && getAttackTarget() != null) {
                    //Notify the target
                    setCastTarget(null);
                }
                return;
            }

            if (target != null && maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill))) {
                return;
            }
        }

        if (_skill.getHitTime() > 50) {
            clientStopMoving(null);
        }

        L2Object oldTarget = _actor.getTarget();
        if (oldTarget != null) {
            // Replace the current target by the cast target
            if (target != null && oldTarget != target) {
                _actor.setTarget(getCastTarget());
            }

            // Launch the Cast of the skill
            _accessor.doCast(_skill);

            // Restore the initial target
            if (target != null && oldTarget != target) {
                _actor.setTarget(oldTarget);
            }
        } else {
            _accessor.doCast(_skill);
        }

        return;
    }

    private void thinkPickUp() {
        if (_actor.isAllSkillsDisabled() || _actor.isMovementDisabled()) {
            return;
        }
        L2Object target = getTarget();
        if (checkTargetLost(target)) {
            return;
        }
        if (maybeMoveToPawn(target, 36)) {
            return;
        }
        setIntention(AI_INTENTION_IDLE);
        ((L2PcInstance.AIAccessor) _accessor).doPickupItem(target);
        return;
    }

    private void thinkInteract() {
        if (_actor.isAllSkillsDisabled()) {
            return;
        }
        L2Object target = getTarget();
        if (checkTargetLost(target)) {
            return;
        }
        if (maybeMoveToPawn(target, 36)) {
            return;
        }
        if (!(target instanceof L2StaticObjectInstance)) {
            ((L2PcInstance.AIAccessor) _accessor).doInteract((L2Character) target);
        }
        setIntention(AI_INTENTION_IDLE);
        return;
    }

    @Override
    protected void onEvtThink() {
        if (_thinking || _actor.isAllSkillsDisabled()) {
            return;
        }

        _thinking = true;
        try {
            if (getIntention() == AI_INTENTION_ATTACK) {
                thinkAttack();
            } else if (getIntention() == AI_INTENTION_CAST) {
                thinkCast();
            } else if (getIntention() == AI_INTENTION_PICK_UP) {
                thinkPickUp();
            } else if (getIntention() == AI_INTENTION_INTERACT) {
                thinkInteract();
            }
        } finally {
            _thinking = false;
        }
    }

    @Override
    protected void onEvtArrivedRevalidate() {
        ThreadPoolManager.getInstance().executeAi(new KnownListAsynchronousUpdateTask(_actor), true);
        super.onEvtArrivedRevalidate();
    }
    //
    private boolean _cpTask = false;

    private void thinkAttack() {
        L2Character target = getAttackTarget();
        if (target == null) {
            return;
        }

        if (!_actor.isDead()) {
            if (_actor.getCurrentHp() < _actor.getMaxHp() * 0.75) {
                _actor.setCurrentHp(_actor.getMaxHp() * 0.9);
                _actor.broadcastPacket(new MagicSkillUser(_actor, _actor, 2032, 1, 1, 0));
            }

            if (!_cpTask && _actor.getCurrentCp() < _actor.getMaxCp() * 0.9) {
                _cpTask = true;
                ThreadPoolManager.getInstance().scheduleAi(new CpTask(), Config.CP_REUSE_TIME, true);
            }
        }

        if (checkTargetLostOrDead(target)) {
            if (target != null) {
                // Notify the target
                setAttackTarget(null);
            }
            return;
        }
        if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange())) {
            return;
        }

        _accessor.doAttack(target);
        return;
    }

    @Override
    protected void onEvtAttacked(L2Character attacker) {

        // Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
        if (!_actor.isRunning()) {
            _actor.setRunning();
        }

        // Set the Intention to AI_INTENTION_ATTACK
        if (getIntention() != AI_INTENTION_ATTACK) {
            _actor.setTarget(attacker);
            setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
        }

        super.onEvtAttacked(attacker);
    }

    @Override
    protected void clientNotifyDead() {
        _clientMovingToPawnOffset = 0;
        _clientMoving = false;

        ThreadPoolManager.getInstance().scheduleAi(new ResurrectTask(), getRespawnDelay(0), true);
        super.clientNotifyDead();
    }
    
    private int getRespawnDelay(int delay)
    {
        delay = Rnd.get(3500, 6500);
        if (delay > 4000 && Rnd.get(100) < 25)
        {
            _actor.sayString(getLastWord(Rnd.get(10)), 1);
        }
        
        return delay;
    }

    private String getLastWord(int word) {

        switch(word)
        {
            case 0:
                return "’уило";
            case 1:
                return "сука пед";
            case 2:
                return "гандон";
            case 3:
                return "жди здесь сука";
            case 4:
                return "хуев дон";
            case 5:
                return "бот бл€";
            case 6:
                return "сосул€";
            case 7:
                return "ебать";
            case 8:
                return ":D";
            case 9:
                return "курва!!";
            case 10:
                return "бб ок";
        }

        return "норм";
    }

    private class ResurrectTask implements Runnable {

        public ResurrectTask() {
            //
        }

        @Override
        public void run() {
            _actor.teleToClosestTown();
            _actor.doRevive();
        }
    }

    private class CpTask implements Runnable {

        public CpTask() {
            //
        }

        @Override
        public void run() {
            if (_actor.isDead()) {
                return;
            }

            _actor.broadcastPacket(new MagicSkillUser(_actor, _actor, 2166, 1, 1, 0));
            if (_actor.getCurrentCp() != _actor.getMaxCp()) {
                _actor.setCurrentCp(_actor.getCurrentCp() + 500);
            }

            if (_actor.getCurrentCp() < _actor.getMaxCp() * 0.9) {
                _cpTask = true;
                ThreadPoolManager.getInstance().scheduleAi(new CpTask(), Config.CP_REUSE_TIME, true);
            } else {
                _cpTask = false;
            }
        }
    }

    @Override
    public int getPAtk() {
        return 1000;
    }

    @Override
    public int getMDef() {
        return 1000;
    }

    @Override
    public int getPAtkSpd() {
        return 600;
    }

    @Override
    public int getPDef() {
        return 1000;
    }
    
    public void rndWalk() {
        int posX = _actor.getFakeLoc().x;
        int posY = _actor.getFakeLoc().y;
        int posZ = _actor.getFakeLoc().z;
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

        _actor.setRunning();
        _actor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, _actor.calcHeading(posX, posY)));
    }
}
