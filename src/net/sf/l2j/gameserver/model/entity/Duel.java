/*
 * This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.model.entity;

import java.util.Calendar;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelEnd;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelReady;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelStart;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.log.AbstractLogger;

public class Duel {

    protected static final Logger _log = AbstractLogger.getLogger(Duel.class.getName());
    // =========================================================
    // Data Field
    private boolean _isPartyDuel;
    private Calendar _DuelEndTime;
    private int _surrenderRequest = 0;
    private int _countdown = 4;
    private boolean _finished = false;
    FastList<L2PcInstance> _team1 = new FastList<L2PcInstance>();
    FastList<L2PcInstance> _team2 = new FastList<L2PcInstance>();
    private FastMap<L2PcInstance, PlayerCondition> _playerConditions = new FastMap<L2PcInstance, PlayerCondition>();

    public static enum DuelResultEnum {

        Continue,
        Team1Win,
        Team2Win,
        Team1Surrender,
        Team2Surrender,
        Canceled,
        Timeout
    }

    public static enum DuelState {

        Winner,
        Looser,
        Fighting,
        Dead,
        Interrupted
    }

    // =========================================================
    // Constructor
    public Duel(L2PcInstance playerA, L2PcInstance playerB, boolean partyDuel) {
        _isPartyDuel = partyDuel;

        _team1.add(playerA);
        _team2.add(playerB);

        _DuelEndTime = Calendar.getInstance();
        if (_isPartyDuel) {
            _DuelEndTime.add(Calendar.SECOND, 300);
        } else {
            _DuelEndTime.add(Calendar.SECOND, 120);
        }

        if (_isPartyDuel) {
            /*//Добавить игроков в списки дуэлянтов
            for(L2PcInstance p : playerA.getParty().getPartyMembers())
            if(p != playerA)
            _team1.add(p);
            
            for(L2PcInstance p : playerB.getParty().getPartyMembers())
            if(p != playerB)
            _team1.add(p);
            
            // increase countdown so that start task can teleport players
            _countdown++;
            // inform players that they will be portet shortly
            SystemMessage sm = SystemMessage.id(SystemMessage.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
            broadcastToTeam1(sm);
            broadcastToTeam2(sm);*/
        }

        // Save player Conditions
        savePlayerConditions();

        // Schedule duel start
        ThreadPoolManager.getInstance().scheduleAi(new ScheduleStartDuelTask(this), 3000, true);
    }

    // ===============================================================
    // Nested Class
    public static class PlayerCondition {

        private L2PcInstance _player;
        private double _hp, _mp, _cp;
        private boolean _paDuel;
        private int _x, _y, _z;
        private DuelState _duelState;
        private FastList<L2Effect> _debuffs;

        public PlayerCondition(L2PcInstance player, boolean partyDuel) {
            if (player == null) {
                return;
            }
            _player = player;
            _hp = _player.getCurrentHp();
            _mp = _player.getCurrentMp();
            _cp = _player.getCurrentCp();
            _paDuel = partyDuel;

            if (_paDuel) {
                _x = _player.getX();
                _y = _player.getY();
                _z = _player.getZ();
            }
        }

        public void registerDebuff(L2Effect debuff) {
            if (_debuffs == null) {
                _debuffs = new FastList<L2Effect>();
            }

            _debuffs.add(debuff);
        }

        public void RestoreCondition(boolean abnormalEnd) {
            if (_player == null) {
                return;
            }
            _player.updateLastTeleport(true);

            if (_debuffs != null) {
                for (L2Effect e : _debuffs) {
                    if (e != null) {
                        e.exit();
                    }
                }
            }

            for (L2Effect e : _player.getAllEffects()) {
                if (e == null) {
                    continue;
                }

                if (e.getSkill().isOffensive()) {
                    e.exit();
                }
            }

            // if it is an abnormal DuelEnd do not restore hp, mp, cp
            if (!abnormalEnd && !_player.isDead()) {
                _player.setCurrentHp(_hp);
                _player.setCurrentMp(_mp);
                _player.setCurrentCp(_cp);
            }

            if (_paDuel) {
                TeleportBack();
            }
        }

        public void TeleportBack() {
            if (_paDuel) {
                _player.teleToLocation(_x, _y, _z);
            }
        }

        public L2PcInstance getPlayer() {
            return _player;
        }

        public void setDuelState(DuelState d) {
            _duelState = d;
        }

        public DuelState getDuelState() {
            return _duelState;
        }
    }

    // ===============================================================
    // Schedule task
    public class ScheduleDuelTask implements Runnable {

        private Duel _duel;

        public ScheduleDuelTask(Duel duel) {
            _duel = duel;
        }

        public void run() {
            try {
                DuelResultEnum status = _duel.checkEndDuelCondition();

                //_log.info("DuelCheck done, result: "+status.toString());

                switch (status) {
                    case Continue:
                        ThreadPoolManager.getInstance().scheduleAi(this, 1000, true);
                        break;
                    case Team1Win:
                    case Team2Win:
                    case Team1Surrender:
                    case Team2Surrender:
                        setFinished(true);
                        playKneelAnimation();
                    //break;
                    case Canceled:
                    case Timeout:
                        setFinished(true); //На всякий пожарный, если верхнее не выполнилось.

                        //Колечка должны сниматся сразу
                        for (L2PcInstance p : _team1) {
                            p.setTeam(0);
                        }
                        for (L2PcInstance p : _team2) {
                            p.setTeam(0);
                        }

                        ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndDuelTask(_duel, status), 5000);
                        stopFighting();
                        //TODO: hide hp display of opponents (after adding it.. :p )
                        break;
                    default:
                        _log.info("Error with duel end.");
                }
            } catch (Throwable t) {
                _log.warning("Can't continue duel" + t);
                t.printStackTrace(System.out);
            }
        }
    }

    public class ScheduleStartDuelTask implements Runnable {

        private Duel _duel;

        public ScheduleStartDuelTask(Duel duel) {
            _duel = duel;
        }

        public void run() {
            try {
                // start/continue countdown
                int count = _duel.Countdown();

                if (count == 4) {
                    // players need to be teleportet first
                    //TODO: stadia manager needs a function to return an unused stadium for duels
                    // currently only teleports to the same stadium
                    _duel.teleportPlayers(-102495, -209023, -3326);

                    // give players 20 seconds to complete teleport and get ready (its ought to be 30 on offical..)
                    ThreadPoolManager.getInstance().scheduleAi(this, 20000, true);
                } else if (count > 0) {
                    ThreadPoolManager.getInstance().scheduleAi(this, 1000, true);
                } else {
                    _duel.startDuel();
                }
            } catch (Throwable t) {
            }
        }
    }

    public class ScheduleEndDuelTask implements Runnable {

        private Duel _duel;
        private DuelResultEnum _result;

        public ScheduleEndDuelTask(Duel duel, DuelResultEnum result) {
            _duel = duel;
            _result = result;
        }

        public void run() {
            try {
                _duel.endDuel(_result);
            } catch (Throwable t) {
                _log.warning("Duel: Can't end duel " + t);
            }
        }
    }

    // ========================================================
    // Method - Private
    /**
     * Stops all players from attacking.
     * Used for duel timeout.
     */
    void stopFighting() {
        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            stopFighting(temp);
        }

        for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            stopFighting(temp);
        }
    }

    /**
     * Прекращает атаку определенного игрока
     * @param player you wish to stop the attack
     */
    public void stopFighting(L2PcInstance player) {
        if (player.isDead() == true) {
            player.setIsKilledAlready(true);
        }
        player.abortCast();
        player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
        player.setTarget(null);
        if (player.getPet() != null) {
            player.getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
        }
        player.sendActionFailed();
    }

    // ========================================================
    // Method - Public
    /**
     * Check if a player engaged in pvp combat (only for 1on1 duels)
     * @param sendMessage if we need to send message
     * @return returns true if a duelist is engaged in Pvp combat
     */
    public boolean isDuelistInPvp(boolean sendMessage) {
        if (_isPartyDuel) // Party duels take place in arenas - should be no other players there
        {
            return false;
        } else if (_team1.get(0).getPvpFlag() != 0 || _team2.get(0).getPvpFlag() != 0) {
            if (sendMessage) {
                String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
                _team1.get(0).sendMessage(engagedInPvP);
                _team1.get(0).sendMessage(engagedInPvP);
            }
            return true;
        }
        return false;
    }

    /**
     * Starts the duel
     */
    public void startDuel() {
        // Начало проверки на наличие дуэли
        String name = null;

        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            if (temp.getDuel() != null) {
                name = temp.getName();
                break;
            }
        }

        if (name == null) {
            for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) {
                L2PcInstance temp = n.getValue();
                if (temp == null) {
                    continue;
                }
                if (temp.getDuel() != null) {
                    name = temp.getName();
                    break;
                }
            }
        }

        if (name != null) {
            SystemMessage sm = SystemMessage.id(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD).addString(name);
            for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) {
                L2PcInstance temp = n.getValue();
                if (temp == null) {
                    continue;
                }
                temp.sendPacket(sm);
            }
            for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) {
                L2PcInstance temp = n.getValue();
                if (temp == null) {
                    continue;
                }
                temp.sendPacket(sm);
            }
            sm = null;
            return;
        }
        // Конец проверки на наличие дуэли

        // set isInDuel() state
        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            temp.cancelActiveTrade();
            temp.setDuel(this);
            getPlayerCondition(temp).setDuelState(DuelState.Fighting);
            temp.setTeam(1);
            temp.broadcastStatusUpdate();
            broadcastToOppositTeam(temp, new ExDuelUpdateUserInfo(temp));
        }
        for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            temp.cancelActiveTrade();
            temp.setDuel(this);
            temp.setTeam(2);
            getPlayerCondition(temp).setDuelState(DuelState.Fighting);
            temp.broadcastStatusUpdate();
            broadcastToOppositTeam(temp, new ExDuelUpdateUserInfo(temp));
        }

        // Send duel Start packets
        // TODO: verify: is this done correctly?
        ExDuelReady ready = new ExDuelReady(_isPartyDuel ? 1 : 0);
        ExDuelStart start = new ExDuelStart(_isPartyDuel ? 1 : 0);

        broadcastToTeam1(ready);
        broadcastToTeam2(ready);
        broadcastToTeam1(start);
        broadcastToTeam2(start);

        // play duel music
        PlaySound ps = new PlaySound("B04_S01");
        broadcastToTeam1(ps);
        broadcastToTeam2(ps);

        // start duelling task
        ThreadPoolManager.getInstance().scheduleAi(new ScheduleDuelTask(this), 1000, true);
    }

    /**
     * Save the current player condition: hp, mp, cp, location
     *
     */
    public void savePlayerConditions() {
        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            _playerConditions.put(temp, new PlayerCondition(temp, _isPartyDuel));
        }
        for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            _playerConditions.put(temp, new PlayerCondition(temp, _isPartyDuel));
        }
    }

    /**
     * Restore player conditions
     * @param abnormalDuelEnd was the duel canceled?
     */
    public void restorePlayerConditions(boolean abnormalDuelEnd) {
        // restore player conditions
        for (FastMap.Entry<L2PcInstance, PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;) {
            PlayerCondition pc = e.getValue();
            if (pc == null) {
                continue;
            }
            pc.RestoreCondition(abnormalDuelEnd);
        }

        // update isInDuel() state for all players
        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            temp.setDuel(null);
            temp.setTeam(0);
        }
        for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            temp.setDuel(null);
            temp.setTeam(0);
        }
    }

    /**
     * Returns the remaining time
     * @return remaining time
     */
    public int getRemainingTime() {
        return (int) (_DuelEndTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
    }

    /**
     * Get the player that requestet the duel
     * @return duel requester
     */
    public L2PcInstance getPlayerA() {
        return _team1.get(0);
    }

    /**
     * Get the player that was challenged
     * @return challenged player
     */
    public L2PcInstance getPlayerB() {
        return _team2.get(0);
    }

    /**
     * Returns whether this is a party duel or not
     * @return is party duel
     */
    public boolean isPartyDuel() {
        return _isPartyDuel;
    }

    public void setFinished(boolean mode) {
        _finished = mode;
    }

    public boolean isFinished() {
        return _finished;
    }

    /**
     * teleport all players to the given coordinates
     * @param x coord
     * @param y coord
     * @param z coord
     */
    public void teleportPlayers(int x, int y, int z) {
        //TODO: adjust the values if needed... or implement something better (especially using more then 1 arena)
        if (!_isPartyDuel) {
            return;
        }
        int offset = 0;

        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            temp.teleToLocation(x + offset - 180, y - 150, z);
            offset += 40;
        }
        offset = 0;
        for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            temp.teleToLocation(x + offset - 180, y + 150, z);
            offset += 40;
        }
    }

    /**
     * Broadcast a packet to the challanger team
     * @param packet what you wish to send
     */
    public void broadcastToTeam1(L2GameServerPacket packet) {
        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            temp.sendPacket(packet);
        }
    }

    /**
     * Broadcast a packet to the challenged team
     * @param packet what you wish to send
     */
    public void broadcastToTeam2(L2GameServerPacket packet) {
        for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) {
            L2PcInstance temp = n.getValue();
            if (temp == null) {
                continue;
            }
            temp.sendPacket(packet);
        }
    }

    /**
     * Get the duel winner
     * @return winner
     */
    public L2PcInstance getWinner() {
        if (!isFinished() || _team1.size() == 0 || _team2.size() == 0) {
            return null;
        }
        if (_playerConditions.get(_team1.get(0)).getDuelState() == DuelState.Winner) {
            return _team1.get(0);
        }
        if (_playerConditions.get(_team2.get(0)).getDuelState() == DuelState.Winner) {
            return _team2.get(0);
        }
        return null;
    }

    /**
     * Get the duel looser
     * @return looser
     */
    public FastList<L2PcInstance> getLoosers() {
        if (!isFinished() || _team1.get(0) == null || _team2.get(0) == null) {
            return null;
        }
        if (_playerConditions.get(_team1.get(0)).getDuelState() == DuelState.Winner) {
            return _team2;
        }
        if (_playerConditions.get(_team2.get(0)).getDuelState() == DuelState.Winner) {
            return _team1;
        }
        return null;
    }

    /**
     * Playback the bow animation for all loosers
     */
    public void playKneelAnimation() {
        FastList<L2PcInstance> loosers = getLoosers();
        if (loosers == null || loosers.size() == 0) {
            return;
        }

        for (FastList.Node<L2PcInstance> n = loosers.head(), end = loosers.tail(); (n = n.getNext()) != end;) {
            L2PcInstance looser = n.getValue();
            if (looser == null) {
                continue;
            }
            looser.broadcastPacket(new SocialAction(looser.getObjectId(), 7));
        }
    }

    /**
     * Do the countdown and send message to players if necessary
     * @return current count
     */
    public int Countdown() {
        _countdown--;

        if (_countdown > 3) {
            return _countdown;
        }

        // Broadcast countdown to duelists
        SystemMessage sm = null;
        if (_countdown > 0) {
            sm = SystemMessage.id(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS).addNumber(_countdown);
        } else {
            sm = Static.LET_THE_DUEL_BEGIN;
        }

        broadcastToTeam1(sm);
        broadcastToTeam2(sm);
        sm = null;
        return _countdown;
    }

    /**
     * The duel has reached a state in which it can no longer continue
     * @param result of duel
     */
    public void endDuel(DuelResultEnum result) {
        //_log.info("Executing duel End task.");
        if (_team1.get(0) == null || _team2.get(0) == null) {
            //clean up
            _log.warning("Duel: Duel end with null players.");
            _playerConditions.clear();
            _playerConditions = null;
            return;
        }

        // inform players of the result
        SystemMessage sm = null;
        switch (result) {
            case Team1Win:
                restorePlayerConditions(false);
                // send SystemMessage
                if (_isPartyDuel) {
                    sm = SystemMessage.id(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL);
                } else {
                    sm = SystemMessage.id(SystemMessageId.S1_HAS_WON_THE_DUEL);
                }
                sm.addString(_team1.get(0).getName());

                broadcastToTeam1(sm);
                broadcastToTeam2(sm);
                break;
            case Team2Win:
                restorePlayerConditions(false);
                // send SystemMessage
                if (_isPartyDuel) {
                    sm = SystemMessage.id(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL);
                } else {
                    sm = SystemMessage.id(SystemMessageId.S1_HAS_WON_THE_DUEL);
                }
                sm.addString(_team2.get(0).getName());

                broadcastToTeam1(sm);
                broadcastToTeam2(sm);
                break;
            case Team1Surrender:
                restorePlayerConditions(false);
                // send SystemMessage
                if (_isPartyDuel) {
                    sm = SystemMessage.id(SystemMessageId.SINCE_S1S_PARTY_WITHDREW_FROM_THE_DUEL_S1S_PARTY_HAS_WON);
                } else {
                    sm = SystemMessage.id(SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON);
                }
                sm.addString(_team2.get(0).getName()).addString(_team1.get(0).getName());

                broadcastToTeam1(sm);
                broadcastToTeam2(sm);
                break;
            case Team2Surrender:
                restorePlayerConditions(false);
                // send SystemMessage
                if (_isPartyDuel) {
                    sm = SystemMessage.id(SystemMessageId.SINCE_S1S_PARTY_WITHDREW_FROM_THE_DUEL_S1S_PARTY_HAS_WON);
                } else {
                    sm = SystemMessage.id(SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON);
                }
                sm.addString(_team2.get(0).getName()).addString(_team1.get(0).getName());

                broadcastToTeam1(sm);
                broadcastToTeam2(sm);
                break;
            case Canceled:
                restorePlayerConditions(true);
                // send SystemMessage
                sm = Static.THE_DUEL_HAS_ENDED_IN_A_TIE;

                broadcastToTeam1(sm);
                broadcastToTeam2(sm);
                break;
            case Timeout:
                stopFighting();
                // hp,mp,cp seem to be restored in a timeout too...
                restorePlayerConditions(false);
                // send SystemMessage
                sm = Static.THE_DUEL_HAS_ENDED_IN_A_TIE;

                broadcastToTeam1(sm);
                broadcastToTeam2(sm);
                break;
        }
        sm = null;

        // Send end duel packet
        //TODO: verify: is this done correctly?
        ExDuelEnd duelEnd = new ExDuelEnd(_isPartyDuel ? 1 : 0);

        broadcastToTeam1(duelEnd);
        broadcastToTeam2(duelEnd);

        //clean up
        _playerConditions.clear();
        _playerConditions = null;
    }

    /**
     * Did a situation occur in which the duel has to be ended?
     * @return DuelResultEnum duel status
     */
    public DuelResultEnum checkEndDuelCondition() {
        // one of the players might leave during duel
        if (_team1.get(0) == null || _team2.get(0) == null) {
            return DuelResultEnum.Canceled;
        }

        // got a duel surrender request?
        if (_surrenderRequest != 0) {
            if (_surrenderRequest == 1) {
                return DuelResultEnum.Team1Surrender;
            }
            return DuelResultEnum.Team2Surrender;
        } // duel timed out
        else if (getRemainingTime() <= 0) {
            return DuelResultEnum.Timeout;
        }
        // Has a player been declared winner yet?
        if (_playerConditions.get(_team1.get(0)).getDuelState() == DuelState.Winner) {
            return DuelResultEnum.Team1Win;
        }
        if (_playerConditions.get(_team2.get(0)).getDuelState() == DuelState.Winner) {
            return DuelResultEnum.Team2Win;
        } // More end duel conditions for 1on1 duels
        else if (!_isPartyDuel) {
            // Duel was interrupted e.g.: player was attacked by mobs / other players
            if (_playerConditions.get(_team1.get(0)).getDuelState() == DuelState.Interrupted || _playerConditions.get(_team2.get(0)).getDuelState() == DuelState.Interrupted) {
                return DuelResultEnum.Canceled;
            }

            // Are the players too far apart?
            if (!_team1.get(0).isInsideRadius(_team2.get(0), 1600, false, false)) {
                return DuelResultEnum.Canceled;
            }

            // Did one of the players engage in PvP combat?
            if (isDuelistInPvp(true)) {
                return DuelResultEnum.Canceled;
            }

            // is one of the players in a Siege, Peace or PvP zone?
			/*if(_team1.get(0).isInZonePeace() || _team2.get(0).isInZonePeace() 
            || _team1.get(0).isOnSiegeField() || _team2.get(0).isOnSiegeField() 
            || _team1.get(0).isInCombatZone() || _team2.get(0).isInCombatZone() 
            || _team1.get(0).isInWater() || _team2.get(0).isInWater() //плавать не дано
            || _team1.get(0).isFishing() || _team2.get(0).isFishing()) // и рыбку ловить тоже
            return DuelResultEnum.Canceled;*/
            if (_team1.get(0).isInZonePeace() || _team2.get(0).isInZonePeace()) {
                return DuelResultEnum.Canceled;
            }
        }

        return DuelResultEnum.Continue;
    }

    /**
     * Register a surrender request
     * @param player that had surrender
     */
    public void doSurrender(L2PcInstance player) {
        // already recived a surrender request
        if (_surrenderRequest != 0) {
            return;
        }

        if (getTeamForPlayer(player) == null) {
            _log.warning("Error handling duel surrender request by " + player.getName());
            return;
        }

        if (_team1.contains(player)) {
            _surrenderRequest = 1;
            for (L2PcInstance temp : _team1) {
                setDuelState(temp, DuelState.Dead);
            }

            for (L2PcInstance temp : _team2) {
                setDuelState(temp, DuelState.Winner);
            }
        } else if (_team2.contains(player)) {
            _surrenderRequest = 2;
            for (L2PcInstance temp : _team2) {
                setDuelState(temp, DuelState.Dead);
            }

            for (L2PcInstance temp : _team1) {
                setDuelState(temp, DuelState.Winner);
            }
        }

        /*if (player.getParty() != null && player.getParty().getLeader() == player)
        {
        if(_team1.contains(player))
        {
        _surrenderRequest = 1;
        
        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) 
        {
        L2PcInstance temp = n.getValue();
        if (temp == null)
        continue;
        setDuelState(temp, DuelState.Dead);
        }
        
        for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) 
        {
        L2PcInstance temp = n.getValue();
        if (temp == null)
        continue;
        setDuelState(temp, DuelState.Winner);
        }
        }
        else if(_team2.contains(player))
        {
        _surrenderRequest = 2;
        for (FastList.Node<L2PcInstance> n = _team2.head(), end = _team2.tail(); (n = n.getNext()) != end;) 
        {
        L2PcInstance temp = n.getValue();
        if (temp == null)
        continue;
        setDuelState(temp, DuelState.Dead);
        }
        
        for (FastList.Node<L2PcInstance> n = _team1.head(), end = _team1.tail(); (n = n.getNext()) != end;) 
        {
        L2PcInstance temp = n.getValue();
        if (temp == null)
        continue;
        setDuelState(temp, DuelState.Winner);
        }
        }
        }*/
    }

    /**
     * This function is called whenever a player was defeated in a duel
     * @param player tat loose the duel
     */
    public void onPlayerDefeat(L2PcInstance player) {
        // Set player as defeated
        setDuelState(player, DuelState.Dead);

        if (_isPartyDuel) {
            boolean teamdefeated = true;
            FastList<L2PcInstance> team = getTeamForPlayer(player);
            for (L2PcInstance temp : getTeamForPlayer(player)) {
                if (getDuelState(temp) == DuelState.Fighting) {
                    teamdefeated = false;
                    break;
                }
            }

            if (teamdefeated) {
                //Установить поьедителем противоположеную команду
                team = team == _team1 ? _team2 : _team1;
                for (L2PcInstance temp : team) {
                    setDuelState(temp, DuelState.Winner);
                }
            }
        } else {
            if (player != _team1.get(0) && player != _team2.get(0)) {
                _log.warning("Error in onPlayerDefeat(): player is not part of this 1vs1 duel");
            }

            if (_team1.get(0) == player) {
                setDuelState(_team2.get(0), DuelState.Winner);
            } else {
                setDuelState(_team1.get(0), DuelState.Winner);
            }
        }
    }

    /**
     * This function is called whenever a player leaves a party
     * @param player leaving player
     */
    public void onRemoveFromParty(L2PcInstance player) {
        // if it isnt a party duel ignore this
        if (!_isPartyDuel) {
            return;
        }

        // this player is leaving his party during party duel
        // if hes either playerA or playerB cancel the duel and port the players back
        if (player == _team1.get(0) || player == _team2.get(0)) {
            for (FastMap.Entry<L2PcInstance, PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;) {
                PlayerCondition pc = e.getValue();
                if (pc == null) {
                    continue;
                }
                pc.TeleportBack();
                pc.getPlayer().setDuel(null);
            }
        } else // teleport the player back & delete his PlayerCondition record
        {
            PlayerCondition pc = _playerConditions.get(player);

            if (pc == null) {
                _log.warning("Duel: Error, can't get player condition from list.");
                return;
            }

            pc.TeleportBack();
            _playerConditions.remove(player);

            //Удалить игрока со списков учасников
            if (_team1.contains(player)) {
                _team1.remove(player);
            } else if (_team2.contains(player)) {
                _team2.remove(player);
            }

            player.setDuel(null);
        }
    }

    /**
     * Получаем playerCondition
     * @param player у которого мы хотим получить
     * @return либо playerCondition либо null
     */
    public PlayerCondition getPlayerCondition(L2PcInstance player) {
        return _playerConditions.get(player);
    }

    /**
     * Получить состояние дуэли
     * @param player игрок состояние кого пытаемся получить.
     * @return состояние дуели
     */
    public DuelState getDuelState(L2PcInstance player) {
        return _playerConditions.get(player).getDuelState();
    }

    /**
     * Устанавливает состояние дуели
     * @param player кому устанавливать
     * @param state что устанавливать
     */
    public void setDuelState(L2PcInstance player, DuelState state) {
        _playerConditions.get(player).setDuelState(state);
    }

    /**
     * Broadcasts a packet to the team opposing the given player.
     * @param player to whos opponents you wish to send packet
     * @param packet what you wish to send
     */
    public void broadcastToOppositTeam(L2PcInstance player, L2GameServerPacket packet) {

        if (_team1.contains(player)) {
            broadcastToTeam2(packet);
        } else if (_team2.contains(player)) {
            broadcastToTeam1(packet);
        } else {
            _log.warning("Duel: Broadcast by player who is not in duel");
        }
    }

    public FastList<L2PcInstance> getTeamForPlayer(L2PcInstance p) {
        if (_team1.contains(p)) {
            return _team1;
        } else if (_team2.contains(p)) {
            return _team2;
        } else {
            _log.warning("Duel: got request for player team who is not duel participant");
            return null;
        }
    }

    /**
     * Посколько мы снесли к чертям мэнеджер дуэлей (Нафига оно надо???)
     * мы должны запускать дуэли как-то по другому.
     * Статический метод вполне устроит, т.к. все нужное находится в инстансе.
     * @param playerA бросающий вызов
     * @param playerB кто бросает вызов
     * @param partyDuel партийная или нет
     */
    public static void createDuel(L2PcInstance playerA, L2PcInstance playerB, int partyDuel) {
        if (playerA == null || playerB == null || playerA.getDuel() != null || playerB.getDuel() != null) {
            return;
        }

        // return if a player has PvPFlag
        String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
        if (partyDuel == 1) {
            boolean playerInPvP = false;
            for (L2PcInstance temp : playerA.getParty().getPartyMembers()) {
                if (temp.getPvpFlag() != 0) {
                    playerInPvP = true;
                    break;
                }
            }
            if (!playerInPvP) {
                for (L2PcInstance temp : playerB.getParty().getPartyMembers()) {
                    if (temp.getPvpFlag() != 0) {
                        playerInPvP = true;
                        break;
                    }
                }
            }
            // A player has PvP flag
            if (playerInPvP) {
                for (L2PcInstance temp : playerA.getParty().getPartyMembers()) {
                    temp.sendMessage(engagedInPvP);
                }
                for (L2PcInstance temp : playerB.getParty().getPartyMembers()) {
                    temp.sendMessage(engagedInPvP);
                }
                return;
            }
        } else if (playerA.getPvpFlag() != 0 || playerB.getPvpFlag() != 0) {
            playerA.sendMessage(engagedInPvP);
            playerB.sendMessage(engagedInPvP);
            return;
        }

        //запуск дуэли происходит в ее конструкторе
        new Duel(playerA, playerB, partyDuel == 1);
    }

    public static boolean checkIfCanDuel(L2PcInstance requestor, L2PcInstance target, boolean sendMessage) {
        SystemMessage _noDuelReason = null;
        if (target.isInCombat() || target.isInJail()) //TODO: jail check?
        {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
        } else if (target.isDead() || target.isAlikeDead())// || target.getCurrentHpPercents() < 50 || target.getCurrentMpPercents() < 50 || target.getCurrentCpPercents() < 50)
        {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1S_HP_OR_MP_IS_BELOW_50_PERCENT;
        } else if (target.isInDuel()) {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL;
        } else if (target.isInOlympiadMode()) {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
        } else if (target.isCursedWeaponEquiped() || target.getKarma() > 0 || target.getPvpFlag() > 0) {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE;
        } else if (target.getPrivateStoreType() != 0) {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
        } else if (target.isMounted() || target.isInBoat()) {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER;
        } else if (target.isFishing()) {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING;
        } else if (target.isInsideZone(L2Character.ZONE_PVP) || target.isInsideZone(L2Character.ZONE_PEACE) || target.isInsideZone(L2Character.ZONE_SIEGE)) {
            _noDuelReason = Static.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
        } else if (!requestor.isInsideRadius(target, 250, false, false)) {
            _noDuelReason = Static.S1_CANNOT_RECEIVE_A_DUEL_CHALLENGE_BECAUSE_S1_IS_TOO_FAR_AWAY;
        } else if (!EventManager.getInstance().checkPlayer(requestor) || !EventManager.getInstance().checkPlayer(target)) {
            _noDuelReason = Static.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL;
        } else if (requestor.isInZonePeace() || target.isInZonePeace()) {
            _noDuelReason = Static.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME;
        }

        if (sendMessage && _noDuelReason != null) {
            if (requestor != target) {
                requestor.sendPacket(_noDuelReason);
            } else {
                requestor.sendPacket(Static.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
            }
        }
        return _noDuelReason == null;
    }

    public void onBuff(L2PcInstance player, L2Effect debuff) {
        PlayerCondition pcon = _playerConditions.get(player);

        if (pcon != null) {
            pcon.registerDebuff(debuff);
        }
    }
}
