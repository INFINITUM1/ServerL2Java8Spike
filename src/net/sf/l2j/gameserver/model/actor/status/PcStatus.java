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

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadGame;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;

public class PcStatus extends PlayableStatus {
    // =========================================================
    // Data Field

    private L2PcInstance _activeChar;

    // =========================================================
    // Constructor
    public PcStatus(L2PcInstance activeChar) {
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
        reduceHp(value, attacker, awake, false);
    }

    public final void reduceHp(double value, L2Character attacker, boolean awake, boolean hp) {
        if (attacker == null) {
            return;
        }

        if (_activeChar.isInvul()) {
            return;
        }

        //if (getActiveChar().isFantome()) return;

        if (attacker.isPlayer()) {
            if (_activeChar.isInDuel()) {
                // the duel is finishing - players do not recive damage
                if (_activeChar.getDuel().getDuelState(_activeChar) == DuelState.Dead) {
                    return;
                } else if (_activeChar.getDuel().getDuelState(_activeChar) == DuelState.Winner) {
                    return;
                }

                // cancel duel if player got hit by another player, that is not part of the duel
                if (attacker.getDuel() != _activeChar.getDuel()) {
                    _activeChar.getDuel().setDuelState(_activeChar, DuelState.Interrupted);
                }
            }

            if (_activeChar.isDead() && !_activeChar.isFakeDeath()) {
                return;
            }
        } else {
            // if attacked by a non L2PcInstance & non L2SummonInstance the duel gets canceled
            if (_activeChar.isInDuel() && !(attacker.isSummon())) {
                _activeChar.getDuel().setDuelState(_activeChar, DuelState.Interrupted);
            }
            if (_activeChar.isDead()) {
                return;
            }
        }

        if (_activeChar.isInOlympiadMode()) {
            OlympiadGame olymp_game = Olympiad.getOlympiadGame(_activeChar.getOlympiadGameId());
            if (olymp_game != null) {
                if (olymp_game.getState() <= 0) {
                    //attacker.getPlayer().sendPacket(Msg.INVALID_TARGET);
                    return;
                }

                if (!_activeChar.equals(attacker)) {
                    olymp_game.addDamage(_activeChar, Math.min(_activeChar.getCurrentHp(), value));
                }

                if (value >= (_activeChar.getCurrentHp() + _activeChar.getCurrentCp())) {
                    olymp_game.setWinner(_activeChar.getOlympiadSide() == 1 ? 2 : 1);
                    olymp_game.endGame(20000, false);
                    _activeChar.setCurrentHp(1);
                    attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
                    attacker.sendActionFailed();
                    return;
                }
            }
            /*else
            {
            _log.warning("OlympiadGame id = " + getActiveChar().getOlympiadGameId() + " is null");
            Thread.dumpStack();
            }*/
        }

        int petDmg = 0;
        int fullValue = (int) value;
        if (attacker != _activeChar) {
            // Check and calculate transfered damage
            L2Summon summon = _activeChar.getPet();
            //TODO correct range
            if (summon != null && summon.isSummon() && Util.checkIfInRange(900, _activeChar, summon, true)) {
                petDmg = (int) value * (int) _activeChar.getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) / 100;
                if (summon.getCurrentHp() < petDmg) {
                    _activeChar.stopSkillEffects(1262);
                }
                // Only transfer dmg up to current HP, it should not be killed
                petDmg = Math.min((int) summon.getCurrentHp() - 1, petDmg);
                if (petDmg > 0) {
                    summon.reduceCurrentHp(petDmg, attacker);
                    value -= petDmg;
                    fullValue = (int) value; // reduce the announced value here as player will get a message about summon damage
                }
                /*if (petDmg > 0)
                {
                // Only transfer dmg up to current HP, it should not be killed
                //if (summon.getCurrentHp() < petDmg) petDmg = (int)summon.getCurrentHp() - 1;
                if (summon.getCurrentHp() > petDmg)
                fullValue = (int) value; // reduce the annouced value here as player will get a message about summon dammage
                else
                {
                _activeChar.stopSkillEffects(1262);
                petDmg = (int) summon.getCurrentHp() - 1;
                value -= petDmg;
                }
                value -= petDmg;
                summon.reduceCurrentHp(petDmg, attacker);
                }*/
            }

            if (!hp && (attacker.isL2Playable() || attacker.isL2SiegeGuard())) {
                if (getCurrentCp() >= value) {
                    setCurrentCp(getCurrentCp() - value);   // Set Cp to diff of Cp vs value
                    value = 0;                              // No need to subtract anything from Hp
                } else {
                    value -= getCurrentCp();                // Get diff from value vs Cp; will apply diff to Hp
                    setCurrentCp(0);                        // Set Cp to 0
                }
            }
        }

        super.reduceHp(value, attacker, awake);

        if (!_activeChar.isDead() && _activeChar.isSitting()) {
            _activeChar.standUp();
        }

        if (_activeChar.isFakeDeath()) {
            _activeChar.stopFakeDeath(null);
        }

        if (attacker != null && attacker != _activeChar && fullValue > 0) {
            // Send a System Message to the L2PcInstance
            SystemMessage smsg = SystemMessage.id(SystemMessageId.S1_GAVE_YOU_S2_DMG);
            if (attacker.isL2Npc()) {
                int mobId = ((L2NpcInstance) attacker).getTemplate().idTemplate;
                smsg.addNpcName(mobId);
            } else if (attacker.isL2Summon()) {
                int mobId = ((L2Summon) attacker).getTemplate().idTemplate;
                smsg.addNpcName(mobId);
            } else {
                smsg.addString(attacker.getName());
            }

            smsg.addNumber(fullValue);
            _activeChar.sendPacket(smsg);
            smsg = null;

            if (petDmg > 0) {
                attacker.sendMessage("Вы нанесли " + fullValue + " повреждений вашей цели и " + petDmg + " повреждений слуге");
            } else if (fullValue > 0) {
                attacker.sendUserPacket(SystemMessage.id(SystemMessageId.YOU_DID_S1_DMG).addNumber(fullValue));
            }
        }
    }

    // =========================================================
    // Method - Private
    // =========================================================
    // Property - Public
    @Override
    public L2PcInstance getActiveChar() {
        return _activeChar;
    }
}
