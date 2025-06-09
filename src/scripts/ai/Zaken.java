package scripts.ai;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.bosses.ZakenManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class Zaken extends L2GrandBossInstance {

    private static final int s_zaken_trans_night2day = 4223;
    private static final int s_zaken_trans_day2night = 4224;
    private static final int s_zaken_regen1 = 4227;
    private static final int s_zaken_regen2 = 4242;
    private static final int s_zaken_self_tel = 4222;
    private static final int s_zaken_tel_pc = 4216;
    private static final int s_zaken_range_tel_pc = 4217;
    private static final int s_zaken_hold = 4219;
    private static final int s_zaken_drain = 4218;
    private static final int s_zaken_range_dual_attack = 4221;
    private static final int s_zaken_dual_attack = 4220;
    private static int i_ai0 = 0;
    private static int i_az = 0;
    private static int i1 = 0;
    private static GrandBossManager _gb = GrandBossManager.getInstance();

    public Zaken(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        if (ZakenManager.getInstance().spawned()) {
            return;
        }

        i_ai0 = 0;
        i_az = 1;
        ThreadPoolManager.getInstance().scheduleAi(new Task(1, this), 1000, false);
        ThreadPoolManager.getInstance().scheduleAi(new Task(3, this), 1700, false);
        ZakenManager.getInstance().setSpawned();
    }

    @Override
    public boolean doDie(L2Character killer) {
        super.doDie(killer);
        ZakenManager.getInstance().notifyDie();
        return true;
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        /*if (getCurrentHour() < 5)
        {
        if(i_ai0 == 0 && damage < 10 && Rnd.get(300) < 2)
        {
        i_ai0 = 1;
        ThreadPoolManager.getInstance().scheduleAi(new Task(2, this), 300, false);
        }
        }
        else
        {
        if((getCurrentHp() < (getMaxHp() / 4)) && Rnd.get(100) < 5)
        {
        setTarget(this);
        addUseSkillDesire(s_zaken_self_tel, 1);
        }
        }*/
        if (Rnd.get(100) < 5) {
            int i0 = Rnd.get(100);
            setTarget(attacker);
            /*if( i0 < 1 )
            addUseSkillDesire(s_zaken_tel_pc, 1);
            else if( i0 < 2 )
            addUseSkillDesire(s_zaken_range_tel_pc, 1);
            else */            
            if (i0 < 4) {
                addUseSkillDesire(s_zaken_hold, 1);
            } else if (i0 < 8) {
                addUseSkillDesire(s_zaken_drain, 1);
            } else if (i0 < 15) {
                addUseSkillDesire(s_zaken_range_dual_attack, 1);
            }
        }
        super.reduceCurrentHp(damage, attacker, awake);
    }

    @Override
    public void setTeleported(boolean f) {
        clearAggroList();
        getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        setCanReturnToSpawnPoint(false);
        super.setTeleported(f);
    }

    private class Task implements Runnable {

        int id;
        Zaken zaken;

        Task(int id, Zaken zaken) {
            this.id = id;
            this.zaken = zaken;
        }

        public void run() {
            switch (id) {
                case 1:
                    if (getCurrentHour() < 5) {
                        if (getFirstEffect(s_zaken_trans_night2day) != null) {
                            setTarget(zaken);
                            addUseSkillDesire(s_zaken_trans_day2night, 1);
                        }
                        if (getFirstEffect(s_zaken_regen1) == null) {
                            setTarget(zaken);
                            addUseSkillDesire(s_zaken_regen1, 1);
                        }
                        if (getKnownList().getKnownPlayersInRadius(1200).isEmpty()) {
                            addUseSkillDesire(s_zaken_self_tel, 1);
                        }
                    } else if (getFirstEffect(s_zaken_trans_night2day) == null) {
                        setTarget(zaken);
                        addUseSkillDesire(s_zaken_trans_night2day, 1);
                    }
                    if (getFirstEffect(s_zaken_regen1) != null) {
                        setTarget(zaken);
                        addUseSkillDesire(s_zaken_regen2, 1);
                    }
                    if (Rnd.get(40) < 2) {
                        addUseSkillDesire(s_zaken_self_tel, 1);
                    }

                    ThreadPoolManager.getInstance().scheduleAi(new Task(1, zaken), 30000, false);
                    break;
                case 2:
                    //setTarget(zaken);
                    //addUseSkillDesire(s_zaken_self_tel, 1);
                    i_ai0 = 0;
                    break;
                case 3:
                    switch (i_az) {
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                            for (int i = 1; i <= 15; i++) {
                                int x = 0;
                                int y = 0;
                                int z = 0;
                                switch (i) {
                                    case 1:
                                        x = 53950;
                                        y = 219860;
                                        z = -3488;
                                        break;
                                    case 2:
                                        x = 55980;
                                        y = 219820;
                                        z = -3488;
                                        break;
                                    case 3:
                                        x = 54950;
                                        y = 218790;
                                        z = -3488;
                                        break;
                                    case 4:
                                        x = 55970;
                                        y = 217770;
                                        z = -3488;
                                        break;
                                    case 5:
                                        x = 53930;
                                        y = 217760;
                                        z = -3488;
                                        break;
                                    case 6:
                                        x = 55970;
                                        y = 217770;
                                        z = -3216;
                                        break;
                                    case 7:
                                        x = 55980;
                                        y = 219920;
                                        z = -3216;
                                        break;
                                    case 8:
                                        x = 54960;
                                        y = 218790;
                                        z = -3216;
                                        break;
                                    case 9:
                                        x = 53950;
                                        y = 219860;
                                        z = -3216;
                                        break;
                                    case 10:
                                        x = 53930;
                                        y = 217760;
                                        z = -3216;
                                        break;
                                    case 11:
                                        x = 55970;
                                        y = 217770;
                                        z = -2944;
                                        break;
                                    case 12:
                                        x = 55980;
                                        y = 219920;
                                        z = -2944;
                                        break;
                                    case 13:
                                        x = 54960;
                                        y = 218790;
                                        z = -2944;
                                        break;
                                    case 14:
                                        x = 53950;
                                        y = 219860;
                                        z = -2944;
                                        break;
                                    case 15:
                                        x = 53930;
                                        y = 217760;
                                        z = -2944;
                                        break;
                                }
                                int mob = 29026;
                                switch (i_az) {
                                    case 2:
                                        mob = 29023;
                                        break;
                                    case 3:
                                        mob = 29024;
                                        break;
                                    case 4:
                                        mob = 29027;
                                        break;
                                }
                                _gb.createOnePrivateEx(mob, (x + Rnd.get(600)), (y + Rnd.get(600)), z, 0, 0);
                            }
                            i_az++;
                            ThreadPoolManager.getInstance().scheduleAi(new Task(3, zaken), 1700, false);
                            break;
                        case 5:
                            _gb.createOnePrivateEx(29023, 52675, 219371, -3290, 0, 0);
                            _gb.createOnePrivateEx(29023, 52687, 219596, -3368, 0, 0);
                            _gb.createOnePrivateEx(29023, 52672, 219740, -3418, 0, 0);
                            _gb.createOnePrivateEx(29027, 52857, 219992, -3488, 0, 0);
                            _gb.createOnePrivateEx(29026, 52959, 219997, -3488, 0, 0);
                            _gb.createOnePrivateEx(29024, 53381, 220151, -3488, 0, 0);
                            _gb.createOnePrivateEx(29026, 54236, 220948, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 54885, 220144, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 55264, 219860, -3488, 0, 0);
                            _gb.createOnePrivateEx(29026, 55399, 220263, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 55679, 220129, -3488, 0, 0);
                            _gb.createOnePrivateEx(29024, 56276, 220783, -3488, 0, 0);
                            _gb.createOnePrivateEx(29024, 57173, 220234, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 56267, 218826, -3488, 0, 0);
                            _gb.createOnePrivateEx(29023, 56294, 219482, -3488, 0, 0);
                            _gb.createOnePrivateEx(29026, 56094, 219113, -3488, 0, 0);
                            _gb.createOnePrivateEx(29023, 56364, 218967, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 57113, 218079, -3488, 0, 0);
                            _gb.createOnePrivateEx(29023, 56186, 217153, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 55440, 218081, -3488, 0, 0);
                            _gb.createOnePrivateEx(29026, 55202, 217940, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 55225, 218236, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 54973, 218075, -3488, 0, 0);
                            _gb.createOnePrivateEx(29026, 53412, 218077, -3488, 0, 0);
                            _gb.createOnePrivateEx(29024, 54226, 218797, -3488, 0, 0);
                            _gb.createOnePrivateEx(29024, 54394, 219067, -3488, 0, 0);
                            _gb.createOnePrivateEx(29027, 54139, 219253, -3488, 0, 0);
                            _gb.createOnePrivateEx(29023, 54262, 219480, -3488, 0, 0);
                            i_az = 6;
                            ThreadPoolManager.getInstance().scheduleAi(new Task(3, zaken), 1700, false);
                            break;
                        case 6:
                            _gb.createOnePrivateEx(29027, 53412, 218077, -3488, 0, 0);
                            _gb.createOnePrivateEx(29024, 54413, 217132, -3488, 0, 0);
                            _gb.createOnePrivateEx(29023, 54841, 217132, -3488, 0, 0);
                            _gb.createOnePrivateEx(29023, 55372, 217128, -3343, 0, 0);
                            _gb.createOnePrivateEx(29023, 55893, 217122, -3488, 0, 0);
                            _gb.createOnePrivateEx(29026, 56282, 217237, -3216, 0, 0);
                            _gb.createOnePrivateEx(29024, 56963, 218080, -3216, 0, 0);
                            _gb.createOnePrivateEx(29027, 56267, 218826, -3216, 0, 0);
                            _gb.createOnePrivateEx(29023, 56294, 219482, -3216, 0, 0);
                            _gb.createOnePrivateEx(29026, 56094, 219113, -3216, 0, 0);
                            _gb.createOnePrivateEx(29023, 56364, 218967, -3216, 0, 0);
                            _gb.createOnePrivateEx(29024, 56276, 220783, -3216, 0, 0);
                            _gb.createOnePrivateEx(29024, 57173, 220234, -3216, 0, 0);
                            _gb.createOnePrivateEx(29027, 54885, 220144, -3216, 0, 0);
                            _gb.createOnePrivateEx(29027, 55264, 219860, -3216, 0, 0);
                            _gb.createOnePrivateEx(29026, 55399, 220263, -3216, 0, 0);
                            _gb.createOnePrivateEx(29027, 55679, 220129, -3216, 0, 0);
                            _gb.createOnePrivateEx(29026, 54236, 220948, -3216, 0, 0);
                            _gb.createOnePrivateEx(29026, 54464, 219095, -3216, 0, 0);
                            _gb.createOnePrivateEx(29024, 54226, 218797, -3216, 0, 0);
                            _gb.createOnePrivateEx(29024, 54394, 219067, -3216, 0, 0);
                            _gb.createOnePrivateEx(29027, 54139, 219253, -3216, 0, 0);
                            _gb.createOnePrivateEx(29023, 54262, 219480, -3216, 0, 0);
                            _gb.createOnePrivateEx(29026, 53412, 218077, -3216, 0, 0);
                            _gb.createOnePrivateEx(29027, 55440, 218081, -3216, 0, 0);
                            _gb.createOnePrivateEx(29026, 55202, 217940, -3216, 0, 0);
                            _gb.createOnePrivateEx(29027, 55225, 218236, -3216, 0, 0);
                            _gb.createOnePrivateEx(29027, 54973, 218075, -3216, 0, 0);
                            i_az = 7;
                            ThreadPoolManager.getInstance().scheduleAi(new Task(3, zaken), 1700, false);
                            break;
                        case 7:
                            _gb.createOnePrivateEx(29027, 54228, 217504, -3216, 0, 0);
                            _gb.createOnePrivateEx(29024, 54181, 217168, -3216, 0, 0);
                            _gb.createOnePrivateEx(29023, 54714, 217123, -3168, 0, 0);
                            _gb.createOnePrivateEx(29023, 55298, 217127, -3073, 0, 0);
                            _gb.createOnePrivateEx(29023, 55787, 217130, -2993, 0, 0);
                            _gb.createOnePrivateEx(29026, 56284, 217216, -2944, 0, 0);
                            _gb.createOnePrivateEx(29024, 56963, 218080, -2944, 0, 0);
                            _gb.createOnePrivateEx(29027, 56267, 218826, -2944, 0, 0);
                            _gb.createOnePrivateEx(29023, 56294, 219482, -2944, 0, 0);
                            _gb.createOnePrivateEx(29026, 56094, 219113, -2944, 0, 0);
                            _gb.createOnePrivateEx(29023, 56364, 218967, -2944, 0, 0);
                            _gb.createOnePrivateEx(29024, 56276, 220783, -2944, 0, 0);
                            _gb.createOnePrivateEx(29024, 57173, 220234, -2944, 0, 0);
                            _gb.createOnePrivateEx(29027, 54885, 220144, -2944, 0, 0);
                            _gb.createOnePrivateEx(29027, 55264, 219860, -2944, 0, 0);
                            _gb.createOnePrivateEx(29026, 55399, 220263, -2944, 0, 0);
                            _gb.createOnePrivateEx(29027, 55679, 220129, -2944, 0, 0);
                            _gb.createOnePrivateEx(29026, 54236, 220948, -2944, 0, 0);
                            _gb.createOnePrivateEx(29026, 54464, 219095, -2944, 0, 0);
                            _gb.createOnePrivateEx(29024, 54226, 218797, -2944, 0, 0);
                            _gb.createOnePrivateEx(29024, 54394, 219067, -2944, 0, 0);
                            _gb.createOnePrivateEx(29027, 54139, 219253, -2944, 0, 0);
                            _gb.createOnePrivateEx(29023, 54262, 219480, -2944, 0, 0);
                            _gb.createOnePrivateEx(29026, 53412, 218077, -2944, 0, 0);
                            _gb.createOnePrivateEx(29026, 54280, 217200, -2944, 0, 0);
                            _gb.createOnePrivateEx(29027, 55440, 218081, -2944, 0, 0);
                            _gb.createOnePrivateEx(29026, 55202, 217940, -2944, 0, 0);
                            _gb.createOnePrivateEx(29027, 55225, 218236, -2944, 0, 0);
                            _gb.createOnePrivateEx(29027, 54973, 218075, -2944, 0, 0);
                            i_az = 8;
                    }
                    break;
            }
        }
    }

    private int getCurrentHour() {
        int t = GameTimeController.getInstance().getGameTime();
        return ((t / 60) % 24);
    }

    @Override
    public boolean checkRange() {
        return false;
    }

    /* @Override
    public void onAction(L2PcInstance player)
    {
    if (!player.canSeeTarget(this))
    return;
    
    super.onAction(player);
    }*/
}
