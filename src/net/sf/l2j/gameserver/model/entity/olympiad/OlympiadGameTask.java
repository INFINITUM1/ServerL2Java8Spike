package net.sf.l2j.gameserver.model.entity.olympiad;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.Log;

public class OlympiadGameTask implements Runnable {

    private OlympiadGame _game;
    private BattleStatus _status;
    private int _count;
    private long _time;
    private boolean _terminated = false;

    public boolean isTerminated() {
        return _terminated;
    }

    public BattleStatus getStatus() {
        return _status;
    }

    public int getCount() {
        return _count;
    }

    public OlympiadGame getGame() {
        return _game;
    }

    public long getTime() {
        return _count;
    }

    public ScheduledFuture<?> shedule() {
        return ThreadPoolManager.getInstance().scheduleGeneral(this, _time);
    }

    public OlympiadGameTask(OlympiadGame game, BattleStatus status, int count, long time) {
        _game = game;
        _status = status;
        _count = count;
        _time = time;
    }

    @Override
    public void run() {
        if (_game == null || _terminated) {
            return;
        }

        OlympiadGameTask task = null;

        int gameId = _game.getId();

        try {
            if (!Olympiad.inCompPeriod()) {
                return;
            }

            // Прерываем игру, если один из игроков не онлайн, и игра еще не прервана
            if (!_game.checkPlayersOnline() && _status != BattleStatus.ValidateWinner && _status != BattleStatus.Ending) {
                Log.add("Player is offline for game " + gameId + ", status: " + _status, "olympiad");
                _game.endGame(1000, true);
                return;
            }

            switch (_status) {
                case Begining: {
                    _game.broadcastPacket(SystemMessage.id(SystemMessageId.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S).addNumber(45), true, false);
                    task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, 15, 45000);
                    break;
                }
                case Begin_Countdown: {
                    _game.broadcastPacket(SystemMessage.id(SystemMessageId.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S).addNumber(_count), true, false);
                    if (_count == 30) {
                        task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, 15, 15000);
                    } else if (_count == 15) {
                        task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, 5, 10000);
                    } else if (_count < 6 && _count > 1) {
                        task = new OlympiadGameTask(_game, BattleStatus.Begin_Countdown, _count - 1, 1000);
                    } else if (_count == 1) {
                        task = new OlympiadGameTask(_game, BattleStatus.PortPlayers, 0, 1000);
                    }
                    break;
                }
                case PortPlayers: {
                    _game.portPlayersToArena();
                    task = new OlympiadGameTask(_game, BattleStatus.Started, 60, 1000);
                    break;
                }
                case Started: {
                    if (_count == 60) {
                        _game.setState(1);
                        _game.preparePlayers();
                    }

                    _game.broadcastPacket(SystemMessage.id(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S).addNumber(_count), true, true);
                    _count -= 10;

                    if (_count > 0) {
                        task = new OlympiadGameTask(_game, BattleStatus.Started, _count, 10000);
                        break;
                    }

                    task = new OlympiadGameTask(_game, BattleStatus.CountDown, 5, 5000);
                    break;
                }
                case CountDown: {
                    _game.broadcastPacket(SystemMessage.id(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S).addNumber(_count), true, true);
                    if (_count == 5) {
                        _game.preFightRestore();
                    }
                    _count--;
                    if (_count <= 0) {
                        task = new OlympiadGameTask(_game, BattleStatus.StartComp, 36, 1000);
                    } else {
                        task = new OlympiadGameTask(_game, BattleStatus.CountDown, _count, 1000);
                    }
                    break;
                }
                case StartComp: {
                    if (_count == 36) {
                        _game.setState(2);
                        _game.setPvpArena(true);
                        _game.broadcastPacket(Static.STARTS_THE_GAME, true, true);
                        _game.broadcastInfo(null, null, false);
                    }
                    // Wait 3 mins (Battle)
                    _count--;
                    if (_count == 0) {
                        task = new OlympiadGameTask(_game, BattleStatus.ValidateWinner, 0, 10000);
                    } else {
                        task = new OlympiadGameTask(_game, BattleStatus.StartComp, _count, 10000);
                    }
                    break;
                }
                case ValidateWinner: {
                    try {
                        _game.validateWinner(_count > 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    task = new OlympiadGameTask(_game, BattleStatus.Ending, 0, 15000);
                    break;
                }
                case Ending: {
                    _game.setPvpArena(false);
                    _game.portPlayersBack();
                    _game.clearSpectators();
                    _terminated = true;
                    if (Olympiad._manager != null) {
                        Olympiad._manager.freeOlympiadInstance(_game.getId());
                    }
                    return;
                }
            }

            if (task == null) {
                Log.add("task == null for game " + gameId, "olympiad");
                Thread.dumpStack();
                _game.endGame(1000, true);
                return;
            }

            _game.sheduleTask(task);
        } catch (Exception e) {
            Log.add("Error for game " + gameId + " :" + e.getMessage(), "olympiad");
            e.printStackTrace();
            _game.endGame(1000, true);
        }
    }
}