package scripts.zone.type;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;
import scripts.zone.L2ZoneType;

public class L2AqZone extends L2ZoneType {

    private String _zoneName;

    public L2AqZone(int id) {
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
        if (character.isPlayer()) {
            if (character.getLevel() > Config.AQ_PLAYER_MAX_LVL) {
                if (Rnd.get(100) < 33) {
                    character.teleToLocation(-19480, 187344, -5600);
                } else if (Rnd.get(100) < 50) {
                    character.teleToLocation(-17928, 180912, -5520);
                } else {
                    character.teleToLocation(-23808, 182368, -5600);
                }
                return;
            }

            character.setInAqZone(true);
            character.setInsideSilenceZone(true);
            //player.setChannel(45);

            if (Config.ALLOW_RAID_PVP) {
                character.setInHotZone(true);
            }

            if (character.isGM()) {
                character.sendAdmResultMessage("You entered " + _zoneName);
                return;
            }
        }
        if (Config.ALLOW_RAID_PVP) {
            character.setInsideZone(L2Character.ZONE_PVP, true);
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (Config.ALLOW_RAID_PVP) {
            character.setInsideZone(L2Character.ZONE_PVP, false);
        }

        if (character.isPlayer()) {
            character.setInAqZone(false);
            character.setInsideSilenceZone(false);
            //player.setChannel(1);

            if (Config.ALLOW_RAID_PVP) {
                character.setInHotZone(false);
            }

            if (character.isGM()) {
                character.sendAdmResultMessage("You left " + _zoneName);
                return;
            }
        }

        /*if (character instanceof QueenAnt || character instanceof QueenAntNurse)
        {
        if (!(character.isDead() || character.isAlikeDead()))
        {
        ((L2Attackable) character).clearAggroList();
        character.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        character.teleToLocation(-21556, 181536, -5716, false);
        //aq.deleteMe();
        //GrandBossManager.getInstance().manageAQ(1, 1);
        }
        }*/
    }

    public void oustAllPlayers() {
        for (L2Character temp : _characterList.values()) {
            if (!(temp.isPlayer())) {
                continue;
            }

            temp.teleToLocation(MapRegionTable.TeleportWhereType.Town);
        }
    }

    public void movePlayersTo(int x, int y, int z) {
        if (_characterList == null) {
            return;
        }
        if (_characterList.isEmpty()) {
            return;
        }
        for (L2Character character : _characterList.values()) {
            if (character == null) {
                continue;
            }
            if (character.isPlayer()) {
                if (character.isOnline() == 1) {
                    character.teleToLocation(x, y, z);
                }
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