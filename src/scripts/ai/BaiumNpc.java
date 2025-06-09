package scripts.ai;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.bosses.BaiumManager;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class BaiumNpc extends L2NpcInstance {

    private static L2PcInstance _talker = null;
    private static Baium _baium = null;
    private Lock shed = new ReentrantLock();
    private ScheduledFuture<?> _spawnTask = null;
    private static final int BOSS = 29020;

    public BaiumNpc(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        _spawnTask = null;
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (!canTarget(player, true)) {
            return;
        }

        if (GrandBossManager.getInstance().getZone(116040, 17455, 10078).isPlayerAllowed(player)) {
            spawnMe(player);
        }

        player.sendActionFailed();
    }

    private void spawnMe(L2PcInstance player) {
        shed.lock();
        try {
            if (_spawnTask != null) {
                return;
            }

            _spawnTask = ThreadPoolManager.getInstance().scheduleAi(new SpawnBaium(player), 1000, false);
        } finally {
            shed.unlock();
        }
    }

    class SpawnBaium implements Runnable {

        private L2PcInstance player;

        SpawnBaium(L2PcInstance player) {
            this.player = player;
        }

        public void run() {
            decayMe();
            _talker = player;
            _baium = (Baium) GrandBossManager.getInstance().createOnePrivateEx(BOSS, 116033, 17447, 10107, -25348);
            _baium.setRunning();
            _baium.broadcastPacket(new SocialAction(_baium.getObjectId(), 2));
            BaiumManager.getInstance().setBaium(_baium);
            ThreadPoolManager.getInstance().scheduleGeneral(new Come(), 9000);
            ThreadPoolManager.getInstance().scheduleGeneral(new WakeUp(), 11000);
            _spawnTask = null;
        }
    }

    // порт разбудившего к баюму
    class Come implements Runnable {

        Come() {
        }

        public void run() {
            if (_baium == null) {
                return;
            }

            if (_talker != null) {
                GrandBossManager.getInstance().getZone(116040, 17455, 10078).allowPlayerEntry(_talker, 9000000);
                _talker.teleToLocation(115922, 17342, 10051);
            }
        }
    }

    class WakeUp implements Runnable {

        WakeUp() {
        }

        public void run() {
            if (_baium == null) {
                return;
            }

            _baium.broadcastPacket(new CreatureSay(_baium.getObjectId(), 0, "Baium", "Кто посмел потревожить мой сон!?"));
            _baium.broadcastPacket(new SocialAction(_baium.getObjectId(), 1));
            _baium.broadcastPacket(new Earthquake(_baium.getX(), _baium.getY(), _baium.getZ(), 40, 5));
            if (_talker != null && Rnd.get(100) < 70) {
                _baium.setTarget(_talker);
                _baium.addUseSkillDesire(4136, 1);
            }
            BaiumNpc.this.deleteMe();
        }
    }

    /*
     * @Override public void onAction(L2PcInstance player) { if
     * (!player.canSeeTarget(this)) return;
     *
     * super.onAction(player);
     }
     */
}
