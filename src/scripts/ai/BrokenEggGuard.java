package scripts.ai;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import scripts.autoevents.brokeneggs.BrokenEggs;

public class BrokenEggGuard extends L2GrandBossInstance {

    public BrokenEggGuard(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        ThreadPoolManager.getInstance().scheduleAi(new Radar(this), 1000, false);
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        BrokenEggs.getEvent().notifyGuardDie();
        return true;
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }

    private static class Radar implements Runnable {

        private BrokenEggGuard self;

        Radar(BrokenEggGuard self) {
            this.self = self;
        }

        @Override
        public void run() {

            if (self.getAI().getAttackTarget() != null) {
                ThreadPoolManager.getInstance().scheduleAi(new Radar(self), 500, false);
                return;
            }

            L2PcInstance pc = null;
            FastList<L2PcInstance> players = self.getKnownList().getKnownPlayersInRadius(2100);
            for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                pc = n.getValue();
                if (pc == null) {
                    continue;
                }

                self.setTarget(pc);
                self.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, pc);
                break;
            }
            pc = null;
            ThreadPoolManager.getInstance().scheduleAi(new Radar(self), 500, false);
        }
    }
}
