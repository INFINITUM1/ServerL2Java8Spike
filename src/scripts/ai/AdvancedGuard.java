/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripts.ai;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 *
 * @author �������������
 */
public class AdvancedGuard extends L2GuardInstance {

    //private boolean _attack = false;

    public AdvancedGuard(int objectId, L2NpcTemplate template) {
        super(objectId, template);

    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        if (getSpawn() != null) {
            return;
        }

        ThreadPoolManager.getInstance().scheduleAi(new Radar(), 1000, false);
    }

    private class Radar implements Runnable {

        Radar() {
        }

        @Override
        public void run() {

            L2PcInstance pc = null;
            FastList<L2PcInstance> players = getKnownList().getKnownPlayersInRadius(2100);
            for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                pc = n.getValue();
                if (pc == null || pc.getKarma() == 0) {
                    continue;
                }

                //setTarget(pc);
                //getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, pc);
                pc.reduceCurrentHp(999999, AdvancedGuard.this, true, true);
                //_attack = true;
            }

            ThreadPoolManager.getInstance().scheduleAi(new Radar(), 10, false);

            /*if (_attack) {
                ThreadPoolManager.getInstance().scheduleAi(new Radar(), 1400, false);
                _attack = false;
            } else if (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE) {
                ThreadPoolManager.getInstance().scheduleAi(new Radar(), 400, false);
            }*/
        }
    }
}
