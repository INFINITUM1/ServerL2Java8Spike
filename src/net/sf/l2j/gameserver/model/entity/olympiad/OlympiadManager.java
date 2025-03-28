package net.sf.l2j.gameserver.model.entity.olympiad;

import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

public class OlympiadManager implements Runnable {

    private FastMap<Integer, OlympiadGame> _olympiadInstances = new FastMap<Integer, OlympiadGame>().setShared(true);

    public void wait2(long time) {
        try {
            wait(time);
        } catch (InterruptedException ex) {
        }
    }

    @Override
    public synchronized void run() {
        if (Olympiad.isOlympiadEnd()) {
            return;
        }

        while (Olympiad.inCompPeriod()) {
            if (Olympiad._nobles.isEmpty()) {
                wait2(60000);
                continue;
            }

            while (Olympiad.inCompPeriod()) {
                // Подготовка и запуск внеклассовых боев
                if (Olympiad._nonClassBasedRegisters.size() >= Config.ALT_OLY_MINNONCLASS) {
                    prepareBattles(CompType.NON_CLASSED, Olympiad._nonClassBasedRegisters);
                }

                // Подготовка и запуск классовых боев
                for (Map.Entry<Integer, FastList<Integer>> entry : Olympiad._classBasedRegisters.entrySet()) {
                    if (entry.getValue().size() >= Config.ALT_OLY_MINCLASS) {
                        prepareBattles(CompType.CLASSED, entry.getValue());
                    }
                }

                wait2(30000);
            }

            wait2(30000);
        }

        Olympiad._classBasedRegisters.clear();
        Olympiad._nonClassBasedRegisters.clear();

        Olympiad._ips.clear();
        Olympiad._hwids.clear();

        // when comp time finish wait for all games terminated before execute the cleanup code
        boolean allGamesTerminated = false;

        // wait for all games terminated
        while (!allGamesTerminated) {
            wait2(30000);

            if (_olympiadInstances.isEmpty()) {
                break;
            }

            allGamesTerminated = true;
            for (OlympiadGame game : _olympiadInstances.values()) {
                if (game.getTask() != null && !game.getTask().isTerminated()) {
                    allGamesTerminated = false;
                }
            }
        }

        _olympiadInstances.clear();
    }

    private void prepareBattles(CompType type, FastList<Integer> list) {
        for (int i = 0; i < Olympiad.STADIUMS.length; i++) {
            try {
                if (!Olympiad.STADIUMS[i].isFreeToUse()) {
                    continue;
                }
                if (list.size() < type.getMinSize()) {
                    break;
                }

                OlympiadGame game = new OlympiadGame(i, type, nextOpponents(list, type));
                game.sheduleTask(new OlympiadGameTask(game, BattleStatus.Begining, 0, 1));

                _olympiadInstances.put(i, game);

                Olympiad.STADIUMS[i].setStadiaBusy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void freeOlympiadInstance(int index) {
        _olympiadInstances.remove(index);
        Olympiad.STADIUMS[index].setStadiaFree();
    }

    public OlympiadGame getOlympiadInstance(int index) {
        return _olympiadInstances.get(index);
    }

    public FastMap<Integer, OlympiadGame> getOlympiadGames() {
        return _olympiadInstances;
    }

    private FastList<Integer> nextOpponents(FastList<Integer> list, CompType type) {
        FastList<Integer> opponents = new FastList<Integer>();

        Integer noble;

        for (int i = 0; i < type.getMinSize(); i++) {
            noble = list.remove(Rnd.get(list.size()));
            opponents.add(noble);
            removeOpponent(noble);
        }

        return opponents;
    }

    private void removeOpponent(Integer noble) {
        Olympiad._classBasedRegisters.removeValue(noble);
        Olympiad._nonClassBasedRegisters.remove(noble);
        L2PcInstance player = L2World.getInstance().getPlayer(noble);
        if (player != null) {
            player.olympiadClear();
        }
    }
}
