package scripts.ai;

import net.sf.l2j.gameserver.instancemanager.bosses.FrintezzaManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class HallKeeperCaptain extends L2MonsterInstance {

    public HallKeeperCaptain(int objectId, L2NpcTemplate template) {
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

        FrintezzaManager.getInstance().respawnMob(this);
        return true;
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }
}
