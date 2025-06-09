package scripts.ai;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.bosses.FrintezzaManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class Frintezza extends L2GrandBossInstance {

    private static L2NpcInstance camera = null;
    private static L2MonsterInstance camera2 = null;
    private static Halisha _halisa = null;

    public Frintezza(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        FrintezzaManager.getInstance().setFrintezza(this);

        FrintezzaManager.getInstance().closeAllDoors();
        startAnim();
        FrintezzaManager.getInstance().setState(2, 0);
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
    }

    @Override
    public boolean doDie(L2Character killer) {
        super.doDie(killer);
        return true;
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }

    private void startAnim() {
        setTarget(this);
        FrintezzaManager gb = FrintezzaManager.getInstance();
        camera = (L2NpcInstance) gb.createOnePrivateEx(80003, 174235, -88023, -4820, 40240);
        FastList<L2PcInstance> _players = camera.getKnownList().getKnownPlayersInRadius(2100);
        L2PcInstance pc = null;
        //  UPDATE `npc` SET `type`='L2Monster' WHERE (`id`='80003');
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(camera, 500, 160, -45, 4000, 4400);
        }

        try {
            Thread.sleep(3800);
        } catch (InterruptedException e) {
        }
        if (camera != null) {
            camera.deleteMe();
        }
        camera = null;

        // 
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(this, 1600, 95, 7, 2000, 2000);
        }

        try {
            Thread.sleep(2200);
        } catch (InterruptedException e) {
        }

        // 
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(this, 200, 90, 0, 3000, 5000);
        }

        try {
            Thread.sleep(4900);
        } catch (InterruptedException e) {
        }

        // 
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(this, 10, 70, -10, 20000, 10000);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        //  motor5
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.sendPacket(new SocialAction(getObjectId(), 1));
        }
        try {
            Thread.sleep(8900);
        } catch (InterruptedException e) {
        }
        //  motor6
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(this, 340, 182, 30, 12000, 25000);
        }
        try {
            Thread.sleep(4900);
        } catch (InterruptedException e) {
        }
        //  motor7
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.sendPacket(new SocialAction(getObjectId(), 3));
            pc.specialCamera(this, 1200, 90, 20, 12000, 3000);
        }

        try {
            Thread.sleep(3900);
        } catch (InterruptedException e) {
        }
        //  motor8
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.sendPacket(new MagicSkillUser(this, this, 5007, 1, 15800, 0));
            pc.specialCamera(this, 1200, 90, 20, 12000, 3000);
        }
        try {
            Thread.sleep(2900);
        } catch (InterruptedException e) {
        }
        //  motor9
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(this, 700, 140, 40, 15000, 8000);
        }

        camera = (L2NpcInstance) gb.createOnePrivateEx(29053, 175882, -88703, -5134, 40240);
        try {
            Thread.sleep(5400);
        } catch (InterruptedException e) {
        }
        //  motor10
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(camera, 650, 0, 0, 11500, 15000);
        }
        EvilSpirit p1 = (EvilSpirit) gb.createOnePrivateEx(29048, 175882, -88703, -5134, 0);
        EvilSpirit p2 = (EvilSpirit) gb.createOnePrivateEx(29048, 175820, -87184, -5108, 0);
        EvilSpirit p3 = (EvilSpirit) gb.createOnePrivateEx(29048, 172629, -87175, -5108, 0);
        EvilSpirit p4 = (EvilSpirit) gb.createOnePrivateEx(29048, 172596, -88706, -5134, 0);
        gb.putGhost(p1);
        gb.putGhost(p2);
        gb.putGhost(p3);
        gb.putGhost(p4);
        try {
            Thread.sleep(5900);
        } catch (InterruptedException e) {
        }
        //  motor11
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(p1, 400, 0, 40, 5000, 12000);
        }
        try {
            Thread.sleep(8900);
        } catch (InterruptedException e) {
        }
        //  motor12
        camera = (L2NpcInstance) gb.createOnePrivateEx(80003, 174235, -88023, -4820, 40240);
        try {
            Thread.sleep(1900);
        } catch (InterruptedException e) {
        }
        //  motor13
        camera2 = (L2MonsterInstance) gb.createOnePrivateEx(29053, 174235, -88023, -4820, 40240);
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(camera, 500, 160, -45, 4000, 6400);
        }
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
        }
        //  anim
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            camera2.broadcastPacket(new MagicSkillUser(camera2, camera2, 5004, 1, 5800, 0));
            //pc.specialCamera(this, 1200, 90, 20, 12000, 3000);
        }
        try {
            Thread.sleep(1400);
        } catch (InterruptedException e) {
        }
        //  motor14
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(camera, 500, 120, -35, 14000, 6400);
        }
        _halisa = (Halisha) gb.createOnePrivateEx(29046, 174235, -88023, -5108, 0);
        _halisa.setFormStep(1);
        FrintezzaManager.getInstance().setHalisha(_halisa);
        try {
            Thread.sleep(900);
        } catch (InterruptedException e) {
        }
        //  motor16
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            pc.specialCamera(_halisa, 500, 160, 45, 14000, 14400);
            _halisa.broadcastPacket(new SocialAction(_halisa.getObjectId(), 1));
        }
        try {
            Thread.sleep(5300);
        } catch (InterruptedException e) {
        }
        //  motor17
        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            _halisa.broadcastPacket(new MagicSkillUser(_halisa, _halisa, 5004, 1, 5800, 0));
        }
        if (camera != null) {
            camera.deleteMe();
        }
        camera = null;
        if (camera2 != null) {
            camera2.deleteMe();
        }
        camera2 = null;

        for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }

            pc.setTarget(null);
            pc.leaveMovieMode();
        }
        ThreadPoolManager.getInstance().scheduleGeneral(new PlaySong(this), 16000);
    }

    public void deathAnim() {
        FastList<L2PcInstance> _players = getKnownList().getKnownPlayersInRadius(1200);
        _players.addAll(FrintezzaManager.getInstance().getHalisha().getKnownList().getKnownPlayersInRadius(1200));
        if (!_players.isEmpty()) {
            L2PcInstance pc = null;
            for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
                pc = n.getValue();
                if (pc == null) {
                    continue;
                }
                pc.specialCamera(this, 10, 70, -10, 20000, 10000);
            }

            try {
                Thread.sleep(1900);
            } catch (InterruptedException e) {
            }

            for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
                pc = n.getValue();
                if (pc == null) {
                    continue;
                }
                pc.sendPacket(new SocialAction(getObjectId(), 4));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;) {
                pc = n.getValue();
                if (pc == null) {
                    continue;
                }
                pc.specialCamera(this, 340, 182, 30, 12000, 25000);
            }
            try {
                Thread.sleep(2900);
            } catch (InterruptedException e) {
            }
        }
        FrintezzaManager.getInstance().notifyDie();
    }

    private static class PlaySong implements Runnable {

        private Frintezza self;

        public PlaySong(Frintezza self) {
            this.self = self;
        }

        public void run() {
            int i0 = Rnd.get(4);
            i0 = (i0 + 1);
            switch (i0) {
                case 1:
                    self.playSong(i0);
                    self.sayString("Frintezza's Healing Rhapsody", 1);
                    ThreadPoolManager.getInstance().scheduleGeneral(new PlaySong(self), 36000);
                    break;
                case 2:
                    self.playSong(i0);
                    self.sayString("Frintezza's Rampaging Opus", 1);
                    ThreadPoolManager.getInstance().scheduleGeneral(new PlaySong(self), 36000);
                    break;
                case 3:
                    self.playSong(i0);
                    self.sayString("Frintezza's Power Concerto", 1);
                    ThreadPoolManager.getInstance().scheduleGeneral(new PlaySong(self), 36000);
                    break;
                case 4:
                    self.playSong(i0);
                    self.sayString("Frintezza's Plagued Concerto", 1);
                    ThreadPoolManager.getInstance().scheduleGeneral(new PlaySong(self), 36000);
                    break;
                case 5:
                    self.playSong(i0);
                    self.sayString("Frintezza's Psycho Symphony", 1);
                    ThreadPoolManager.getInstance().scheduleGeneral(new PlaySong(self), 36000);
                    break;
            }
        }
    }

    private void playSong(int lvl) {
        broadcastPacket(new MagicSkillUser(this, this, 5007, lvl, 35800, 0));
        if (Rnd.get(100) < 50) {
            SkillTable.getInstance().getInfo(5007, Rnd.get(1, 4)).getEffects(_halisa, _halisa);
            return;
        }

        FrintezzaManager.getInstance().addEffectPlayers(5008, 1, 55);
    }
}
