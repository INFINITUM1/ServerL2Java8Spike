package scripts.zone.type;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import scripts.zone.L2ZoneType;



public class L2ColiseumZone extends L2ZoneType {

    private boolean _arena = true;

    public L2ColiseumZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("pvpType")) {
            if (Integer.parseInt(value) == 32) {
                _arena = false;
            }
        } else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        character.setInColiseum(true);
        if (_arena) {
            character.setInsideZone(L2Character.ZONE_PVP, true);
            character.sendPacket(SystemMessage.id(SystemMessageId.ENTERED_COMBAT_ZONE));
        }
    }

    @Override
    protected void onExit(L2Character character) {
        character.setInColiseum(false);
        if (_arena) {
            character.setInsideZone(L2Character.ZONE_PVP, false);
            character.sendPacket(SystemMessage.id(SystemMessageId.LEFT_COMBAT_ZONE));
        }
    }

    @Override
    protected void onDieInside(L2Character character) {
    }

    @Override
    protected void onReviveInside(L2Character character) {
    }
}
