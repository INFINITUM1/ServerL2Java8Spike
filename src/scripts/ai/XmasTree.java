package scripts.ai;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class XmasTree extends L2NpcInstance {

    private L2Skill skl = null;

    public XmasTree(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        if (getSpawn() != null) {
            return;
        }

        ThreadPoolManager.getInstance().scheduleAi(new Task(1), Config.XM_TREE_LIFE, false);
        if (getNpcId() == 13007) {
            skl = SkillTable.getInstance().getInfo(2139, 1);
            ThreadPoolManager.getInstance().scheduleAi(new Task(2), 10000, false);
        }
    }

    @Override
    public void onAction(L2PcInstance player) {
        player.sendActionFailed();
    }

    @Override
    public void onActionShift(L2GameClient client) {
        L2PcInstance player = client.getActiveChar();
        if (player == null) {
            return;
        }

        if (player.isGM()) {
            super.onActionShift(client);
        } else {
            player.sendActionFailed();
        }
    }

    private class Task implements Runnable {

        private int id;

        Task(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            switch (id) {
                case 1:
                    skl = null;
                    decayMe();
                    deleteMe();
                    break;
                case 2:
                    L2PcInstance pc = null;
                    FastList<L2PcInstance> players = getKnownList().getKnownPlayersInRadius(500);
                    for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                        pc = n.getValue();
                        if (pc == null) {
                            continue;
                        }

                        pc.stopSkillEffects(2139);
                        skl.getEffects(pc, pc);
                    }
                    //players.clear();
                    ThreadPoolManager.getInstance().scheduleAi(new Task(2), 10000, false);
                    break;
            }
        }
    }
}
