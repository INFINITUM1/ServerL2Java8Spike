package scripts.ai;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public final class SiegeRaidGuard extends L2RaidBossInstance {

    public SiegeRaidGuard(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public boolean isRaid() {
        return true;
    }

    @Override
    public boolean isSiegeRaidGuard() {
        return false;
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
         return false;
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        super.reduceCurrentHp(damage, attacker, awake);
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }
        return true;
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }
}
