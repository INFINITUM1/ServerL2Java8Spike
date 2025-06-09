package net.sf.l2j.gameserver.model.entity.olympiad;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import javolution.text.TextBuilder;
import javolution.util.FastList;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.log.AbstractLogger;

public class OlympiadGame {

    private static final Logger _log = AbstractLogger.getLogger(OlympiadGame.class.getName());
    public static final int MAX_POINTS_LOOSE = 10;
    public boolean validated = false;
    private int _winner = 0;
    private int _state = 0;
    private int _id;
    private CompType _type;
    private OlympiadTeam _team1;
    private OlympiadTeam _team2;
    private FastList<L2PcInstance> _spectators = new FastList<L2PcInstance>();

    public OlympiadGame(int id, CompType type, FastList<Integer> opponents) {
        _type = type;
        _id = id;

        _team1 = new OlympiadTeam(this, 1);
        _team2 = new OlympiadTeam(this, 2);

        for (int i = 0; i < opponents.size() / 2; i++) {
            _team1.addMember(opponents.get(i));
        }

        for (int i = opponents.size() / 2; i < opponents.size(); i++) {
            _team2.addMember(opponents.get(i));
        }

        Log.add("Olympiad System: Game - " + id + ": " + _team1.getName() + " Vs " + _team2.getName(), "olympiad");
    }

    public void clearArena() {
        /*L2Zone zone = ZoneManager.getInstance().getZoneById(L2Zone.ZoneType.OlympiadStadia, 3001 + getId(), false);
         if(zone != null)
         for(L2PcInstance player : zone.getInsidePlayers())
         player.teleToClosestTown();*/
    }

    public void portPlayersToArena() {
        _team1.portPlayersToArena();
        _team2.portPlayersToArena();
    }

    public void preparePlayers() {
        _team1.preparePlayers();
        _team2.preparePlayers();
    }

    public void portPlayersBack() {
        _team1.portPlayersBack();
        _team2.portPlayersBack();
    }

    public void setPvpArena(boolean f) {
        _team1.setPvpArena(f);
        _team2.setPvpArena(f);
    }

    public void preFightRestore() {
        _team1.preFightRestore();
        _team2.preFightRestore();
    }

    public void validateWinner(boolean aborted) throws Exception {
        int state = _state;
        _state = 0;

        if (validated) {
            Log.add("Olympiad Result: " + _team1.getName() + " vs " + _team2.getName() + " ... double validate check!!!", "olympiad");
            return;
        }
        validated = true;

        // Если игра закончилась до телепортации на стадион, то забираем очки у вышедших из игры, не засчитывая никому победу
        if (state < 1 && aborted) {
            _team1.takePointsForCrash();
            _team2.takePointsForCrash();
            broadcastPacket(Static.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_ENDS_THE_GAME, true, false);
            return;
        }

        boolean teamOneCheck = _team1.checkPlayers();
        boolean teamTwoCheck = _team2.checkPlayers();

        if (_winner <= 0) {
            if (!teamOneCheck && !teamTwoCheck) {
                _winner = 0;
            } else if (!teamTwoCheck) {
                _winner = 1; // Выиграла первая команда
            } else if (!teamOneCheck) {
                _winner = 2; // Выиграла вторая команда
            } else if (_team1.getDamage() < _team2.getDamage()) // Вторая команда нанесла вреда меньше, чем первая
            {
                _winner = 1; // Выиграла первая команда
            } else if (_team1.getDamage() > _team2.getDamage()) // Вторая команда нанесла вреда больше, чем первая
            {
                _winner = 2; // Выиграла вторая команда
            }
        }
        try {
            if (_winner == 1) // Выиграла первая команда
            {
                _team1.winGame(_team2);
            } else if (_winner == 2) // Выиграла вторая команда
            {
                _team2.winGame(_team1);
            } else {
                _team1.tie(_team2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        _team1.saveNobleData();
        _team2.saveNobleData();

        broadcastPacket(SystemMessage.id(SystemMessageId.YOU_WILL_GO_BACK_TO_THE_VILLAGE_IN_S1_SECOND_S).addNumber(15), true, true);
    }

    public int getId() {
        return _id;
    }

    public String getTitle() {
        return _team1.getName() + " vs " + _team2.getName();
    }

    public String getTeam1Title() {
        String title = "";
        TextBuilder tb = new TextBuilder();
        for (TeamMember member : _team1.getMembers()) {
            tb.append((tb.length() == 0 ? "" : ", ")).append(member.getName());
        }

        title = tb.toString();
        tb.clear();
        tb = null;
        return "<font color=\"blue\">" + title + "</font>";
    }

    public String getTeam2Title() {
        String title = "";
        TextBuilder tb = new TextBuilder();
        for (TeamMember member : _team2.getMembers()) {
            tb.append((tb.length() == 0 ? "" : ", ")).append(member.getName());
        }

        title = tb.toString();
        tb.clear();
        tb = null;
        return "<font color=\"red\">" + title + "</font>";
    }

    public boolean isRegistered(int objId) {
        return _team1.contains(objId) || _team2.contains(objId);
    }

    public FastList<L2PcInstance> getSpectators() {
        return _spectators;
    }

    public void addSpectator(L2PcInstance spec) {
        _spectators.add(spec);
    }

    public void removeSpectator(L2PcInstance spec) {
        _spectators.remove(spec);
    }

    public void clearSpectators() {
        for (L2PcInstance pc : _spectators) {
            if (pc != null && pc.inObserverMode()) {
                pc.leaveOlympiadObserverMode();
            }
        }
        _spectators.clear();
    }

    public void broadcastInfo(L2PcInstance sender, L2PcInstance receiver, boolean onlyToSpectators) {
        // TODO заюзать пакеты:
        // ExEventMatchCreate
        // ExEventMatchFirecracker
        // ExEventMatchManage
        // ExEventMatchMessage
        // ExEventMatchObserver
        // ExEventMatchScore
        // ExEventMatchTeamInfo
        // ExEventMatchTeamUnlocked
        // ExEventMatchUserInfo

        if (sender != null) {
            if (receiver != null) {
                receiver.sendPacket(new ExOlympiadUserInfo(sender));
            } else {
                broadcastPacket(new ExOlympiadUserInfo(sender), !onlyToSpectators, true);
            }
        } else {
            // Рассылаем информацию о первой команде
            for (L2PcInstance player : _team1.getPlayers()) {
                if (receiver != null) {
                    receiver.sendPacket(new ExOlympiadUserInfo(player));
                } else {
                    broadcastPacket(new ExOlympiadUserInfo(player), !onlyToSpectators, true);
                }
            }

            // Рассылаем информацию о второй команде
            for (L2PcInstance player : _team2.getPlayers()) {
                if (receiver != null) {
                    receiver.sendPacket(new ExOlympiadUserInfo(player));
                } else {
                    broadcastPacket(new ExOlympiadUserInfo(player), !onlyToSpectators, true);
                }
            }
        }
    }

    public void broadcastPacket(L2GameServerPacket packet, boolean toTeams, boolean toSpectators) {
        if (toTeams) {
            _team1.broadcast(packet);
            _team2.broadcast(packet);
        }

        if (toSpectators && _spectators != null) {
            for (L2PcInstance spec : _spectators) {
                if (spec != null) {
                    spec.sendPacket(packet);
                }
            }
        }
    }

    public void setWinner(int val) {
        _winner = val;
    }

    public void setState(int val) {
        _state = val;
    }

    public int getState() {
        return _state;
    }

    public FastList<L2PcInstance> getTeamMembers(L2PcInstance player) {
        return player.getOlympiadSide() == 1 ? _team1.getPlayers() : _team2.getPlayers();
    }

    public void addDamage(L2PcInstance player, double damage) {
        if (player.getOlympiadSide() == 1) {
            _team1.addDamage(damage);
        } else {
            _team2.addDamage(damage);
        }
    }

    public boolean doDie(L2PcInstance player) {
        return player.getOlympiadSide() == 1 ? _team1.doDie(player) : _team2.doDie(player);
    }

    public boolean checkPlayersOnline() {
        return _team1.checkPlayers() && _team2.checkPlayers();
    }

    public boolean logoutPlayer(L2PcInstance player) {
        if (player == null) {
            return false;
        }
        return player.getOlympiadSide() == 1 ? _team1.logout(player) : _team2.logout(player);
    }
    OlympiadGameTask _task;
    ScheduledFuture<?> _shedule;

    public synchronized void sheduleTask(OlympiadGameTask task) {
        if (_shedule != null) {
            _shedule.cancel(false);
        }
        _task = task;
        _shedule = task.shedule();
    }

    public OlympiadGameTask getTask() {
        return _task;
    }

    public BattleStatus getStatus() {
        if (_task != null) {
            return _task.getStatus();
        }
        return BattleStatus.Begining;
    }

    public void endGame(long time, boolean aborted) {
        try {
            validateWinner(aborted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sheduleTask(new OlympiadGameTask(this, BattleStatus.Ending, 0, time));
    }

    public CompType getType() {
        return _type;
    }
}
