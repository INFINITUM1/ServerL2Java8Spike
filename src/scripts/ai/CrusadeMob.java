/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripts.ai;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 *
 * @author Администратор
 */
public class CrusadeMob extends L2MonsterInstance {

   /* private int _killerId;
    private FastList<EventReward> _rewards;*/

    public CrusadeMob(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        /*if (_rewards == null || _rewards.isEmpty()) {
            return true;
        }

        L2PcInstance winner = L2World.getInstance().getPlayer(_killerId);
        if (winner == null) {
            return true;
        }

        for (FastList.Node<EventReward> k = _rewards.head(), endk = _rewards.tail(); (k = k.getNext()) != endk;) {
            EventReward reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                winner.addItem("CrusadeMob", reward.id, reward.count, winner, true);
            }
        }*/
        return true;
    }

   /* public void setKiller(int id) {
        _killerId = id;
    }

    public void setRewards(FastList<EventReward> rewards) {
        _rewards = rewards;
    }

    public void setTimeout(int time) {
        ThreadPoolManager.getInstance().scheduleAi(new DeSpawn(), TimeUnit.SECONDS.toMillis(time), false);
    }*/

    @Override
    public void deleteMe() {
        super.deleteMe();
    }

    /*private class DeSpawn implements Runnable {

        public DeSpawn() {
        }

        @Override
        public void run() {
            deleteMe();
        }
    }*/
}
