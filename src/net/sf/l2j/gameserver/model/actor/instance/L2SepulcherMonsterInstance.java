package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.Future;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.FourSepulchersManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.taskmanager.EffectTaskManager;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;

/**
 *
 * @author sandman
 */
public class L2SepulcherMonsterInstance extends L2MonsterInstance {

    public int mysteriousBoxId = 0;
    protected Future<?> _victimSpawnKeyBoxTask = null;
    protected Future<?> _victimShout = null;
    protected Future<?> _changeImmortalTask = null;
    protected Future<?> _onDeadEventTask = null;

    public L2SepulcherMonsterInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
        setShowSummonAnimation(true);
    }

    @Override
    public void onSpawn() {
        setShowSummonAnimation(false);
        switch (getNpcId()) {
            case 18150:
            case 18151:
            case 18152:
            case 18153:
            case 18154:
            case 18155:
            case 18156:
            case 18157:
                if (_victimSpawnKeyBoxTask != null) {
                    _victimSpawnKeyBoxTask.cancel(true);
                }
                _victimSpawnKeyBoxTask = EffectTaskManager.getInstance().schedule(new VictimSpawnKeyBox(this), 300000);
                if (_victimShout != null) {
                    _victimShout.cancel(true);
                }
                _victimShout = EffectTaskManager.getInstance().schedule(new VictimShout(this), 5000);
                break;
            case 18196:
            case 18197:
            case 18198:
            case 18199:
            case 18200:
            case 18201:
            case 18202:
            case 18203:
            case 18204:
            case 18205:
            case 18206:
            case 18207:
            case 18208:
            case 18209:
            case 18210:
            case 18211:
                break;
            case 18231:
            case 18232:
            case 18233:
            case 18234:
            case 18235:
            case 18236:
            case 18237:
            case 18238:
            case 18239:
            case 18240:
            case 18241:
            case 18242:
            case 18243:
                if (_changeImmortalTask != null) {
                    _changeImmortalTask.cancel(true);
                }
                _changeImmortalTask = EffectTaskManager.getInstance().schedule(new ChangeImmortal(this), 1600);
                break;
            case 18256:
                break;
        }
        super.onSpawn();
    }

    @Override
    public boolean isRaid() {
        switch (getNpcId()) {
            case 25339:
            case 25342:
            case 25346:
            case 25349:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        switch (getNpcId()) {
            case 18120:
            case 18121:
            case 18122:
            case 18123:
            case 18124:
            case 18125:
            case 18126:
            case 18127:
            case 18128:
            case 18129:
            case 18130:
            case 18131:
            case 18149:
            case 18158:
            case 18159:
            case 18160:
            case 18161:
            case 18162:
            case 18163:
            case 18164:
            case 18165:
            case 18183:
            case 18184:
            case 18212:
            case 18213:
            case 18214:
            case 18215:
            case 18216:
            case 18217:
            case 18218:
            case 18219:
                if (_onDeadEventTask != null) {
                    _onDeadEventTask.cancel(true);
                }
                _onDeadEventTask = EffectTaskManager.getInstance().schedule(new OnDeadEvent(this), 3500);
                break;
            case 18150:
            case 18151:
            case 18152:
            case 18153:
            case 18154:
            case 18155:
            case 18156:
            case 18157:
                if (_victimSpawnKeyBoxTask != null) {
                    _victimSpawnKeyBoxTask.cancel(true);
                    _victimSpawnKeyBoxTask = null;
                }
                if (_victimShout != null) {
                    _victimShout.cancel(true);
                    _victimShout = null;
                }
                if (_onDeadEventTask != null) {
                    _onDeadEventTask.cancel(true);
                }
                _onDeadEventTask = EffectTaskManager.getInstance().schedule(new OnDeadEvent(this), 3500);
                break;
            case 18141:
            case 18142:
            case 18143:
            case 18144:
            case 18145:
            case 18146:
            case 18147:
            case 18148:
                if (FourSepulchersManager.getInstance().isViscountMobsAnnihilated(mysteriousBoxId)) {
                    if (_onDeadEventTask != null) {
                        _onDeadEventTask.cancel(true);
                    }
                    _onDeadEventTask = EffectTaskManager.getInstance().schedule(new OnDeadEvent(this), 3500);
                }
                break;
            case 18220:
            case 18221:
            case 18222:
            case 18223:
            case 18224:
            case 18225:
            case 18226:
            case 18227:
            case 18228:
            case 18229:
            case 18230:
            case 18231:
            case 18232:
            case 18233:
            case 18234:
            case 18235:
            case 18236:
            case 18237:
            case 18238:
            case 18239:
            case 18240:
                if (FourSepulchersManager.getInstance().isDukeMobsAnnihilated(mysteriousBoxId)) {
                    if (_onDeadEventTask != null) {
                        _onDeadEventTask.cancel(true);
                    }
                    _onDeadEventTask = EffectTaskManager.getInstance().schedule(new OnDeadEvent(this), 3500);
                }
                break;
            case 25339:
            case 25342:
            case 25346:
            case 25349:
                giveCup(killer.getPlayer());
                if (_onDeadEventTask != null) {
                    _onDeadEventTask.cancel(true);
                }
                _onDeadEventTask = EffectTaskManager.getInstance().schedule(new OnDeadEvent(this), 8500);
                break;
        }
        return true;
    }

    @Override
    public void deleteMe() {
        if (_victimSpawnKeyBoxTask != null) {
            _victimSpawnKeyBoxTask.cancel(true);
            _victimSpawnKeyBoxTask = null;
        }
        if (_onDeadEventTask != null) {
            _onDeadEventTask.cancel(true);
            _onDeadEventTask = null;
        }

        super.deleteMe();
    }

    private void giveCup(L2PcInstance player) {
        if (player == null) {
            return;
        }

        if (player.isInParty()) {
            int cupId = getCupId();
            for (L2PcInstance member : player.getParty().getPartyMembers()) {
                if (member == null) {
                    continue;
                }

                if (isValidCupState(member, member.getQuestState("q620_FourGoblets"), player)) {
                    member.addItem("Quest", cupId, 1, member, true);
                }
            }
        } else {
            if (isValidCupState(player, player.getQuestState("q620_FourGoblets"), player)) {
                player.addItem("Quest", getCupId(), 1, player, true);
            }
        }
    }

    private int getCupId() {
        switch (getNpcId()) {
            case 25339:
                return 7256;
            case 25342:
                return 7257;
            case 25346:
                return 7258;
            case 25349:
                return 7259;
        }
        return 0;
    }

    private boolean isValidCupState(L2PcInstance player, QuestState qs, L2PcInstance leader) {
        if (qs == null) {
            return false;
        }
        if (player.getInventory().getItemByItemId(7262) != null) {
            return false;
        }

        if (Util.calculateDistance(player.getX(), player.getY(), player.getZ(), leader.getX(), leader.getY(), leader.getZ(), true) > Config.FS_PARTY_RANGE) {
            return false;
        }
        if (qs.isStarted() || qs.isCompleted()) {
            return true;
        }

        return false;
    }

    private class VictimShout implements Runnable {

        private L2SepulcherMonsterInstance _activeChar;

        public VictimShout(L2SepulcherMonsterInstance activeChar) {
            _activeChar = activeChar;
        }

        public void run() {
            if (_activeChar.isDead()) {
                return;
            }

            if (!_activeChar.isVisible()) {
                return;
            }

            sayString("forgive me!!");
        }
    }

    private class VictimSpawnKeyBox implements Runnable {

        private L2SepulcherMonsterInstance _activeChar;

        public VictimSpawnKeyBox(L2SepulcherMonsterInstance activeChar) {
            _activeChar = activeChar;
        }

        public void run() {
            if (_activeChar.isDead()) {
                return;
            }

            if (!_activeChar.isVisible()) {
                return;
            }

            FourSepulchersManager.getInstance().spawnKeyBox(_activeChar);
            sayString("Many thanks for rescue me.");

            if (_victimShout != null) {
                _victimShout.cancel(true);
            }

        }
    }

    private class OnDeadEvent implements Runnable {

        L2SepulcherMonsterInstance _activeChar;

        public OnDeadEvent(L2SepulcherMonsterInstance activeChar) {
            _activeChar = activeChar;
        }

        public void run() {
            switch (_activeChar.getNpcId()) {
                case 18120:
                case 18121:
                case 18122:
                case 18123:
                case 18124:
                case 18125:
                case 18126:
                case 18127:
                case 18128:
                case 18129:
                case 18130:
                case 18131:
                case 18149:
                case 18158:
                case 18159:
                case 18160:
                case 18161:
                case 18162:
                case 18163:
                case 18164:
                case 18165:
                case 18183:
                case 18184:
                case 18212:
                case 18213:
                case 18214:
                case 18215:
                case 18216:
                case 18217:
                case 18218:
                case 18219:
                    FourSepulchersManager.getInstance().spawnKeyBox(_activeChar);
                    break;
                case 18150:
                case 18151:
                case 18152:
                case 18153:
                case 18154:
                case 18155:
                case 18156:
                case 18157:
                    FourSepulchersManager.getInstance().spawnExecutionerOfHalisha(_activeChar);
                    break;
                case 18141:
                case 18142:
                case 18143:
                case 18144:
                case 18145:
                case 18146:
                case 18147:
                case 18148:
                    FourSepulchersManager.getInstance().spawnMonster(_activeChar.mysteriousBoxId);
                    break;
                case 18220:
                case 18221:
                case 18222:
                case 18223:
                case 18224:
                case 18225:
                case 18226:
                case 18227:
                case 18228:
                case 18229:
                case 18230:
                case 18231:
                case 18232:
                case 18233:
                case 18234:
                case 18235:
                case 18236:
                case 18237:
                case 18238:
                case 18239:
                case 18240:
                    FourSepulchersManager.getInstance().spawnArchonOfHalisha(_activeChar.mysteriousBoxId);
                    break;
                case 25339:
                case 25342:
                case 25346:
                case 25349:
                    FourSepulchersManager.getInstance().spawnEmperorsGraveNpc(_activeChar.mysteriousBoxId);
                    break;
            }
        }
    }

    private class ChangeImmortal implements Runnable {

        L2SepulcherMonsterInstance activeChar;

        public ChangeImmortal(L2SepulcherMonsterInstance mob) {
            activeChar = mob;
        }

        public void run() {
            activeChar.getEffect(4616, 1); // Invulnerable	by petrification
        }
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return true;
    }
}