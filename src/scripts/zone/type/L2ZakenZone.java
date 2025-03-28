package scripts.zone.type;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;

public class L2ZakenZone extends L2ZoneType {

    private String _zoneName;

    public L2ZakenZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("name")) {
            _zoneName = value;
        } else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        if (Config.ALLOW_RAID_PVP) {
            character.setInsideZone(L2Character.ZONE_PVP, true);
        }

        if (character.isPlayer()) {
            L2PcInstance player = (L2PcInstance) character;

            player.setInZakenZone(true);
            player.setInsideSilenceZone(true);
            player.setInDismountZone(true);

            if (Config.ALLOW_RAID_PVP) {
                player.setInHotZone(true);
            }

            if (player.isGM()) {
                player.sendAdmResultMessage("You entered " + _zoneName);
                return;
            }
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (Config.ALLOW_RAID_PVP) {
            character.setInsideZone(L2Character.ZONE_PVP, false);
        }

        if (character.isPlayer()) {
            L2PcInstance player = (L2PcInstance) character;

            player.setInZakenZone(false);
            player.setInsideSilenceZone(false);
            player.setInDismountZone(false);

            if (Config.ALLOW_RAID_PVP) {
                player.setInHotZone(false);
            }

            if (player.isGM()) {
                player.sendAdmResultMessage("You left " + _zoneName);
                return;
            }
        }
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }
}