package scripts.ai;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.bosses.FrintezzaManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class EvilSpirit extends L2GrandBossInstance {

    private static L2MonsterInstance g1 = null;
    private static L2MonsterInstance g2 = null;
    private static L2MonsterInstance g3 = null;
    private static L2MonsterInstance g4 = null;

    public EvilSpirit(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        ThreadPoolManager.getInstance().scheduleGeneral(new Ghosts(this), 3000);
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        FrintezzaManager.getInstance().removeGhost(this);
        return true;
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }

    static class Ghosts implements Runnable {

        private EvilSpirit self;

        Ghosts(EvilSpirit npc) {
            self = npc;
        }

        public void run() {
            try {
                int x = self.getX();
                FrintezzaManager gb = FrintezzaManager.getInstance();
                switch (x) {
                    case 175882:
                        g1 = (L2MonsterInstance) gb.createOnePrivateEx(29050, 175799, -88751, -5108, 4);
                        g1.broadcastPacket(new SocialAction(g1.getObjectId(), 1));
                        g2 = (L2MonsterInstance) gb.createOnePrivateEx(29050, 175801, -88593, -5108, 4);
                        g3 = (L2MonsterInstance) gb.createOnePrivateEx(29051, 175729, -88678, -5108, 4);
                        g3.broadcastPacket(new SocialAction(g1.getObjectId(), 2));
                        g4 = (L2MonsterInstance) gb.createOnePrivateEx(29051, 175635, -88747, -5108, 4);
                        break;
                    case 175820:
                        g1 = (L2MonsterInstance) gb.createOnePrivateEx(29050, 175695, -87108, -5108, 4);
                        g1.broadcastPacket(new SocialAction(g1.getObjectId(), 1));
                        g2 = (L2MonsterInstance) gb.createOnePrivateEx(29050, 175815, -87312, -5108, 4);
                        g3 = (L2MonsterInstance) gb.createOnePrivateEx(29051, 175623, -87206, -5108, 4);
                        g3.broadcastPacket(new SocialAction(g1.getObjectId(), 2));
                        g4 = (L2MonsterInstance) gb.createOnePrivateEx(29051, 175697, -87325, -5108, 4);
                        break;
                    case 172629:
                        g1 = (L2MonsterInstance) gb.createOnePrivateEx(29050, 172658, -87381, -5108, 4);
                        g1.broadcastPacket(new SocialAction(g1.getObjectId(), 1));
                        g2 = (L2MonsterInstance) gb.createOnePrivateEx(29050, 172835, -87308, -5108, 4);
                        g3 = (L2MonsterInstance) gb.createOnePrivateEx(29051, 172914, -87207, -5108, 4);
                        g3.broadcastPacket(new SocialAction(g1.getObjectId(), 2));
                        g4 = (L2MonsterInstance) gb.createOnePrivateEx(29051, 172840, -86995, -5108, 4);
                        break;
                    case 172596:
                        g1 = (L2MonsterInstance) gb.createOnePrivateEx(29050, 172613, -88539, -5108, 4);
                        g1.broadcastPacket(new SocialAction(g1.getObjectId(), 1));
                        g2 = (L2MonsterInstance) gb.createOnePrivateEx(29050, 172692, -88610, -5108, 4);
                        g3 = (L2MonsterInstance) gb.createOnePrivateEx(29051, 172765, -88739, -5108, 4);
                        g3.broadcastPacket(new SocialAction(g1.getObjectId(), 2));
                        g4 = (L2MonsterInstance) gb.createOnePrivateEx(29051, 172699, -88811, -5108, 4);
                        break;
                }

                try {
                    Thread.sleep(1900);
                } catch (InterruptedException e) {
                }

                if (gb.getHalisha() != null) {
                    if (Rnd.get(100) < 10) {
                        FastList<L2PcInstance> trgs = new FastList<L2PcInstance>();
                        trgs.addAll(gb.getHalisha().getKnownList().getKnownPlayersInRadius(1200));
                        if (trgs.isEmpty()) {
                            return;
                        }

                        L2PcInstance trg = trgs.get(Rnd.get(trgs.size() - 1));
                        if (trg == null) {
                            return;
                        }

                        g3.setTarget(trg);
                        g3.addUseSkillDesire(5015, 1);
                    }
                }
            } catch (Throwable e) {
            }
            ThreadPoolManager.getInstance().scheduleGeneral(new MobsCheck(), 60000);
        }
    }

    static class MobsCheck implements Runnable {

        MobsCheck() {
        }

        public void run() {
            try {
                Halisha halisha = FrintezzaManager.getInstance().getHalisha();
                if (halisha == null || halisha.isDead()) {
                    ThreadPoolManager.getInstance().scheduleGeneral(new MobsCheck(), 60000);
                    return;
                }
                FastList<L2PcInstance> trgs = new FastList<L2PcInstance>();
                trgs.addAll(halisha.getKnownList().getKnownPlayersInRadius(1200));
                if (!trgs.isEmpty()) {
                    L2PcInstance trg = null;
                    FrintezzaManager gb = FrintezzaManager.getInstance();
                    if (g1 == null) {
                        g1 = (L2MonsterInstance) gb.createOnePrivateEx(29050, trg.getX(), trg.getY(), trg.getZ(), trg.getHeading());
                        trg = trgs.get(Rnd.get(trgs.size() - 1));
                        if (trg != null) {
                            g1.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, trg);
                        }
                    }
                    if (g2 == null) {
                        g2 = (L2MonsterInstance) gb.createOnePrivateEx(29050, trg.getX(), trg.getY(), trg.getZ(), trg.getHeading());
                        trg = trgs.get(Rnd.get(trgs.size() - 1));
                        if (trg != null) {
                            g2.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, trg);
                        }
                    }
                    if (g3 == null) {
                        g3 = (L2MonsterInstance) gb.createOnePrivateEx(29051, trg.getX(), trg.getY(), trg.getZ(), trg.getHeading());
                        trg = trgs.get(Rnd.get(trgs.size() - 1));
                        if (trg != null) {
                            g3.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, trg);
                        }
                    }
                    if (g4 == null) {
                        g4 = (L2MonsterInstance) gb.createOnePrivateEx(29051, trg.getX(), trg.getY(), trg.getZ(), trg.getHeading());
                        trg = trgs.get(Rnd.get(trgs.size() - 1));
                        if (trg != null) {
                            g4.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, trg);
                        }
                    }

                    try {
                        Thread.sleep(1900);
                    } catch (InterruptedException e) {
                    }

                    if (Rnd.get(100) < 10) {
                        trg = trgs.get(Rnd.get(trgs.size() - 1));
                        if (trg != null) {
                            g3.setTarget(trg);
                            g3.addUseSkillDesire(5015, 1);
                        }
                    }
                }

            } catch (Throwable e) {
            }
            ThreadPoolManager.getInstance().scheduleGeneral(new MobsCheck(), 60000);
        }
    }
}
