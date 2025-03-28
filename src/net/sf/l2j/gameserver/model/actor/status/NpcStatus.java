/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;

public class NpcStatus extends CharStatus {
    // =========================================================
    // Data Field

    private L2NpcInstance _activeChar;

    // =========================================================
    // Constructor
    public NpcStatus(L2NpcInstance activeChar) {
        super(activeChar);
        _activeChar = activeChar;
    }

    // =========================================================
    // Method - Public
    @Override
    public final void reduceHp(double value, L2Character attacker) {
        reduceHp(value, attacker, true);
    }

    @Override
    public final void reduceHp(double value, L2Character attacker, boolean awake) {
        if (_activeChar.isDead()) {
            return;
        }

        if (attacker != null) {
            if (!_activeChar.isMonster()) {
                if (Config.KILL_NPC_ATTACKER
                        && !Config.NPC_HIT_PROTECTET.contains(_activeChar.getNpcId())) {
                    if (!attacker.teleToLocation(Config.NPC_HIT_LOCATION)) {
                        attacker.reduceCurrentHp(999999, _activeChar);
                    }
                }
                return;
            }
            //
            if (Config.PROTECT_MOBS_ITEMS
                    && _activeChar.isMonster() && attacker.hasItems(_activeChar.getPenaltyItems())) {
                attacker.teleToLocation(_activeChar.getPenaltyLoc());
            }
            //
            if (Config.MOB_PVP_FLAG
                    && Config.MOB_PVP_FLAG_LIST.contains(_activeChar.getNpcId())) {
                attacker.updatePvPStatus();
            }
            //
            if (Config.PREMIUM_MOBS
                    && !attacker.isPremium()
                    && Config.PREMIUM_MOBS_LIST.contains(_activeChar.getNpcId())) {
                return;
            }

            if (Config.MOB_FIXED_DAMAGE
                    && Config.MOB_FIXED_DAMAGE_LIST.containsKey(_activeChar.getNpcId())) {
                value = Config.MOB_FIXED_DAMAGE_LIST.get(_activeChar.getNpcId());
            }

            // Add attackers to npc's attacker list
            _activeChar.addAttackerToAttackByList(attacker);
        }

        super.reduceHp(value, attacker, awake);
    }

    @Override
    public final void reduceNpcHp(double value, L2Character attacker, boolean awake) {
        if (_activeChar.isDead()) {
            return;
        }

        super.reduceHp(value, attacker, awake);
    }

    // =========================================================
    // Method - Private
    // =========================================================
    // Property - Public
    @Override
    public L2NpcInstance getActiveChar() {
        return _activeChar;
    }
}
