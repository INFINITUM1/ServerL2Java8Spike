package net.sf.l2j.gameserver.model.actor.instance;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadDiary;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

/**
 * This class manages all Grand Bosses.
 */
public class L2GrandBossInstance extends L2MonsterInstance {

    private static final int BOSS_MAINTENANCE_INTERVAL = 10000;
    //затычка Орфена
    private boolean _teleportedToNest;
    //
    private boolean _social;

    public L2GrandBossInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    protected int getMaintenanceInterval() {
        return BOSS_MAINTENANCE_INTERVAL;
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    /**
     * Used by Orfen to set 'teleported' flag, when hp goes to <50%
     *
     * @param flag
     */
    public void setTeleported(boolean flag) {
        _teleportedToNest = flag;
    }

    public boolean getTeleported() {
        return _teleportedToNest;
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        super.reduceCurrentHp(damage, attacker, awake);

        int npcId = getTemplate().npcId;

        L2PcInstance player = attacker.getPlayer();
        if (player != null) {
            switch (npcId) {
                case 29014: // Орфен
                    //lastHit = System.currentTimeMillis();

                    if (getTeleported()) {
                        return;
                    }

                    if (this.getCurrentHp() < (this.getMaxHp() * 0.5)) {
                        clearAggroList();
                        getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                        teleToLocation(43577, 15985, -4396, false);
                        setTeleported(true);
                        setCanReturnToSpawnPoint(false);
                    }
                    break;
            }
        }
    }

    @Override
    public boolean isRaid() {
        return true;
    }

    @Override
    public final boolean isGrandRaid() {
        return true;
    }

    //
    public void setIsInSocialAction(boolean flag) {
        _social = flag;
    }

    //
    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        if (isTyranosurus()) {
            return true;
        }

        L2PcInstance player = killer.getPlayer();
        if (player != null) {
            player.doEpicLoot(this, getTemplate().npcId);

            if (Config.RAID_CUSTOM_DROP) {
                dropRaidCustom(player);
            }

            if (Config.EPIC_CLANPOINTS_REWARD > 0) {
                if (player.getClan() != null) {
                    player.getClan().addPoints(Config.EPIC_CLANPOINTS_REWARD);
                }
            }

            broadcastPacket(Static.RAID_WAS_SUCCESSFUL);
            if (player.getParty() != null) {
                for (L2PcInstance member : player.getParty().getPartyMembers()) {
                    if (member == null) {
                        continue;
                    }

                    RaidBossPointsManager.addPoints(member, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
                    if (member.isHero()) {
                        OlympiadDiary.addRecord(member, "Победа в битве с " + getTemplate().name + ".");
                    }
                }
            } else {
                RaidBossPointsManager.addPoints(player, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));

                if (player.isHero()) {
                    OlympiadDiary.addRecord(player, "Победа в битве с " + getTemplate().name + ".");
                }
            }
        }
        return true;
    }
    private long _lastDrop = 0;

    public long getLastDrop() {
        return _lastDrop;
    }

    public void setLastDrop() {
        _lastDrop = System.currentTimeMillis();
    }

    public void doTele() {
        FastList<L2PcInstance> trgs = new FastList<L2PcInstance>();
        trgs.addAll(getKnownList().getKnownPlayersInRadius(1500));
        if (!trgs.isEmpty()) {
            L2PcInstance trg = trgs.get(Rnd.get(trgs.size() - 1));
            if (trg != null) {
                setTarget(trg);
                broadcastPacket(new MagicSkillUser(this, trg, 5015, 1, 1500, 0));
                try {
                    Thread.sleep(340);
                } catch (InterruptedException e) {
                }
                setVis(false);
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                }
                getPosition().setXYZ(trg.getX(), trg.getY(), trg.getZ());
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }
                setVis(true);
                attackFirst();
            }
        }
    }

    public void attackFirst() {
        FastList<L2PcInstance> trgs = new FastList<L2PcInstance>();
        trgs.addAll(getKnownList().getKnownPlayersInRadius(1200));
        if (!trgs.isEmpty()) {
            L2PcInstance trg = trgs.get(Rnd.get(trgs.size() - 1));
            if (trg != null) {
                setTarget(trg);
                addDamageHate(trg, 0, 999);
                getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, trg);
            }
        }
    }

    public boolean inLair() {
        return false;
    }

    @Override
    public boolean checkRange() {
        if (getTemplate().npcId == 29021) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isTyranosurus() {
        switch (getTemplate().npcId) {
            case 22215:
            case 22216:
            case 22217:
                return true;
        }
        return false;
    }

    /*
     * @Override public void onAction(L2PcInstance player) { if
     * (!player.canSeeTarget(this)) return;
     *
     * super.onAction(player);
    }
     */
}
