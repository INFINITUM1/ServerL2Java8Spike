package scripts.ai;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.bosses.FrintezzaManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class Halisha extends L2GrandBossInstance {

    private static int _formStep;

    public Halisha(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        setRunning();
        if (getNpcId() == 29046) {
            ThreadPoolManager.getInstance().scheduleGeneral(new ChkHp(this), 5000);
        }
        ThreadPoolManager.getInstance().scheduleGeneral(new ChkSkl(this), 18000);
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        super.reduceCurrentHp(damage, attacker, awake);
    }

    @Override
    public boolean doDie(L2Character killer) {
        super.doDie(killer);
        if (getNpcId() == 29047) {
            FrintezzaManager.getInstance().updateRespawn();
        }
        return true;
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }

    public void setFormStep(int formStep) {
        _formStep = formStep;
    }

    public int getFormStep() {
        return _formStep;
    }

    private static class ChkHp implements Runnable {

        private Halisha self;

        ChkHp(Halisha self) {
            this.self = self;
        }

        public void run() {
            boolean next = true;
            if (self.getCurrentHp() < (self.getMaxHp() * 0.6)) {
                if (_formStep == 1) {
                    self.setTarget(self);
                    self.addUseSkillDesire(5017, 1);
                } else {
                    self.broadcastPacket(new SocialAction(self.getObjectId(), 2));
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                }

                switch (_formStep) {
                    case 1:
                        next = false;
                        self.transeForm();
                        break;
                    case 2:
                        next = false;
                        self.finalForm();
                        break;
                }
            }

            if (next) {
                ThreadPoolManager.getInstance().scheduleGeneral(new ChkHp(self), 5000);
            }
        }
    }

    private void transeForm() {
        if (_formStep == 2) {
            return;
        }

        decayMe();

        Halisha halisha = (Halisha) GrandBossManager.getInstance().createOnePrivateEx(29046, getX(), getY(), getZ(), 0);
        halisha.setFormStep(2);
        halisha.setRHandId(7903);
        halisha.doTele();
        FrintezzaManager.getInstance().setHalisha(halisha);

        deleteMe();
    }

    private void finalForm() {
        if (_formStep == 3) {
            return;
        }

        decayMe();

        Halisha halisha = (Halisha) GrandBossManager.getInstance().createOnePrivateEx(29047, getX(), getY(), getZ(), 0);
        halisha.setFormStep(3);
        halisha.setRHandId(8204);
        FrintezzaManager.getInstance().setHalisha(halisha);

        deleteMe();
    }

    private static class ChkSkl implements Runnable {

        private Halisha self;

        ChkSkl(Halisha self) {
            this.self = self;
        }

        public void run() {
            try {
                if (self.getNpcId() == 29046 && self.getFormStep() == 3) {
                    return;
                }

                int chanse = Rnd.get(100);
                if (chanse < 55) {
                    int lvl = FrintezzaManager.getInstance().getGhostsSize();
                    if (lvl <= 0) {
                        lvl = 1;
                    } else if (lvl > 2) {
                        lvl = 2;
                    }

                    if (self.getFormStep() == 2 && chanse < 25) {
                        self.addUseSkillDesire(5016, 1);
                    } else if (chanse < 35) {
                        self.addUseSkillDesire(5018, lvl);
                    } else if (chanse < 45) {
                        if (self.getNpcId() == 29047) {
                            self.addUseSkillDesire(5019, 1);
                        } else {
                            self.addUseSkillDesire(5014, lvl);
                        }
                    }
                } else if (self.getFormStep() == 2 || self.getNpcId() == 29047) {
                    if (self.getTarget() != null && !Util.checkIfInRange(350, self, self.getTarget(), true)) {
                        self.doTele();
                    }
                }
            } catch (Throwable e) {
            }
            ThreadPoolManager.getInstance().scheduleGeneral(new ChkSkl(self), 18000);
        }
    }
}
