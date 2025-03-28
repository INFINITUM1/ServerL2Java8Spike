package scripts.ai;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public final class BarakielNoblesse extends L2RaidBossInstance {

    public BarakielNoblesse(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public boolean isRaid() {
        return true;
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

        L2PcInstance player = null;
        if (killer.isPlayer()) {
            player = (L2PcInstance) killer;
        } else if (killer.isL2Summon()) {
            player = killer.getOwner();
        }

        if (player != null) {
            broadcastPacket(Static.RAID_WAS_SUCCESSFUL);
            if (Config.BARAKIEL_NOBLESS) {
                if (player.getParty() == null) {
                    rewardNoble(player);
                } else {
                    for (L2PcInstance member : player.getParty().getPartyMembers()) {
                        if (member == null) {
                            continue;
                        }

                        rewardNoble(member);
                    }
                }
            }
        }
        return true;
    }

    private void rewardNoble(L2PcInstance player) {
        if (player.isNoble()) {
            return;
        }

        player.setNoble(true);
        player.addItem("rewardNoble", 7694, 1, this, true);
        player.sendUserPacket(new PlaySound("ItemSound.quest_finish"));

        if (!Config.ACADEMY_CLASSIC) {
            player.rewardAcademy(0);
        }
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }
}
