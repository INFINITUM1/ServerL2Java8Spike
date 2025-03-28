/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class manages all Grand Bosses.
 *
 * @version $Revision: 1.0.0.0 $ $Date: 2006/06/16 $
 */
public final class L2ClanHallBossInstance extends L2MonsterInstance {

    private static final int BOSS_MAINTENANCE_INTERVAL = 10000;

    /**
     * Constructor for L2ClanHallBossInstance. This represent all grandbosses.
     * 
     * @param objectId ID of the instance
     * @param template L2NpcTemplate of the instance
     */
    public L2ClanHallBossInstance(int objectId, L2NpcTemplate template) {
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
     * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if necessary.<BR><BR>
     *
     */
    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        super.reduceCurrentHp(damage, attacker, awake);
    }

    @Override
    public boolean isRaid() {
        return true;
    }

    /**
     * 
     * @see net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance#doDie(net.sf.l2j.gameserver.model.L2Character)
     */
    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        if (!getCastle().getSiege().getIsInProgress()) {
            return false;
        }

        L2PcInstance player = killer.getPlayer();
        if (player != null) {
            int npcId = getTemplate().npcId;
            int ClanHallId = 0;

            switch (npcId) {
                case 35410: //Devastated Castle - Gustav
                    ClanHallId = 34;
                    break;
                case 35629: //Fortress of Dead
                    ClanHallId = 64;
                    break;
                case 35368: //Fortress of Resisstans
                    ClanHallId = 21;
                    break;
            }

            if (ClanHallId == 21) {
                if (player.getClan() != null && player.getClan().getHasHideout() == 0) {
                    if (!ClanHallManager.getInstance().isFree(ClanHallId)) {
                        ClanHallManager.getInstance().setFree(ClanHallId);
                    }

                    ClanHallManager.getInstance().setOwner(ClanHallId, player.getClan());
                }
                getCastle().getSiege().endSiege();
                return true;
            }

            if (player.getClan() != null && getCastle().getSiege().getAttackerClan(player.getClan()) != null && player.getClan().getHasHideout() == 0) {
                if (!ClanHallManager.getInstance().isFree(ClanHallId)) {
                    ClanHallManager.getInstance().setFree(ClanHallId);
                }

                ClanHallManager.getInstance().setOwner(ClanHallId, player.getClan());
            }
            getCastle().getSiege().endSiege();
        }
        return true;
    }
}
