package net.sf.l2j.gameserver.model.entity;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class FightClubArena {

    private boolean _freeToUse = true;
    private int[] _coords = new int[3];
    private FastList<L2PcInstance> _spectators;

    public boolean isFreeToUse() {
        return _freeToUse;
    }

    public void setStadiaBusy() {
        _freeToUse = false;
    }

    public void setStadiaFree() {
        _freeToUse = true;
    }

    public int[] getCoordinates() {
        return _coords;
    }

    public FightClubArena(int x, int y, int z) {
        _coords[0] = x;
        _coords[1] = y;
        _coords[2] = z;
        _spectators = new FastList<L2PcInstance>();
    }

    protected void addSpectator(int id, L2PcInstance spec, boolean storeCoords) {
        spec.enterOlympiadObserverMode(getCoordinates()[0], getCoordinates()[1], getCoordinates()[2], id, storeCoords);
        _spectators.add(spec);
    }

    protected FastList<L2PcInstance> getSpectators() {
        return _spectators;
    }

    protected void removeSpectator(L2PcInstance spec) {
        if (_spectators != null && _spectators.contains(spec)) {
            _spectators.remove(spec);
        }
    }
}
