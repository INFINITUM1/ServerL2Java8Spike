package scripts.zone.type;

import net.sf.l2j.gameserver.model.L2Character;
import scripts.zone.L2ZoneType;

public class L2AntiLogoutZone extends L2ZoneType {

    public L2AntiLogoutZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
        character.setInNoLogoutArea(true);
    }

    @Override
    protected void onExit(L2Character character) {
        character.setInNoLogoutArea(false);
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }
}
