package scripts.zone.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;

/**
 * @author DaRkRaGe
 */
public class L2BossZone extends L2ZoneType {

    private String _zoneName;
    private int _timeInvade;
    private boolean _enabled = true; // default value, unless overridden by xml...
    private Map<Integer, Long> _raiders;
    private int[] _oustLoc = {
        0, 0, 0
    };

    public L2BossZone(int id) {
        super(id);
        _raiders = new ConcurrentHashMap<Integer, Long>();
        _oustLoc = new int[3];
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("name")) {
            _zoneName = value;
        } else if (name.equals("InvadeTime")) {
            _timeInvade = Integer.parseInt(value);
        } else if (name.equals("EnabledByDefault")) {
            _enabled = Boolean.parseBoolean(value);
        } else if (name.equals("oustX")) {
            _oustLoc[0] = Integer.parseInt(value);
        } else if (name.equals("oustY")) {
            _oustLoc[1] = Integer.parseInt(value);
        } else if (name.equals("oustZ")) {
            _oustLoc[2] = Integer.parseInt(value);
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
            character.setInsideSilenceZone(true);

            if (Config.ALLOW_RAID_PVP) {
                character.setInHotZone(true);
            }

            if (character.isGM()) {
                character.sendAdmResultMessage("You entered " + _zoneName);
                return;
            } else {
                character.setChannel(67);
            }

            if (_zoneName.equalsIgnoreCase("Lair of Frintezza")) {
                return;
            }

            if (!(Config.NOEPIC_QUESTS)) {
                return;
            }

            if (character.isMounted()) {
                character.teleToLocation(MapRegionTable.TeleportWhereType.Town);
                return;
            }

            // if player has been (previously) cleared by npc/ai for entry and the zone is 
            // set to receive players (aka not waiting for boss to respawn)
            //Long until = _raiders.get(character.getObjectId());
            //if (until == null || until < System.currentTimeMillis()) {
            if (sendAway(character.getObjectId(), _raiders.get(character.getObjectId()))) {
                //teleport out all players who attempt "illegal" (re-)entry
                character.teleToLocation(82737, 148571, -3470);
                _raiders.remove(character.getObjectId());
            }
        }
    }

    private boolean sendAway(int charId, Long until) {
        if (until == null) {
            return true;
        }

        if (!Config.BOSS_ZONE_LOGOUT || _zoneName.startsWith("Four Sepulcher")) {
            _raiders.remove(charId);
            return false;
        }

        if (until < System.currentTimeMillis()) {
            return true;
        }

        return false;
    }

    private void unEquipForbItems(L2Character character) {
        // снятие переточеных вещей
        for (L2ItemInstance item : character.getPcInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (item.notForBossZone()) {
                character.getPcInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
            }
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (Config.ALLOW_RAID_PVP) {
            character.setInsideZone(L2Character.ZONE_PVP, false);
        }

        if (_enabled) {
            if (character.isPlayer()) {
                //Thread.dumpStack();
                character.setInsideSilenceZone(false);

                if (Config.ALLOW_RAID_PVP) {
                    character.setInHotZone(false);
                }

                if (_zoneName.equals("Lair of Frintezza")) {
                    sheckSpecialItems(character.getPlayer());
                }

                if (character.isGM()) {
                    character.sendAdmResultMessage("You left " + _zoneName);
                    return;
                } else {
                    character.setChannel(1);
                }

                // if the player just got disconnected/logged out, store the dc
                // time so that
                // decisions can be made later about allowing or not the player
                // to log into the zone
                Long until = _raiders.get(character.getObjectId());
                if (until != null && until < System.currentTimeMillis()) {
                    _raiders.remove(character.getObjectId());
                }
            }
        }
    }

    private void sheckSpecialItems(L2PcInstance player) {
        L2ItemInstance coin = player.getInventory().getItemByItemId(8192);
        if (coin == null || coin.getCount() <= 0) {
            return;
        }

        player.destroyItemByItemId("ClearSpecialItems", 8192, coin.getCount(), player, true);
    }

    public void setZoneEnabled(boolean flag) {
        if (_enabled != flag) {
            oustAllPlayers();
        }

        _enabled = flag;
    }

    public String getZoneName() {
        return _zoneName;
    }

    public int getTimeInvade() {
        return _timeInvade;
    }

    /*
     * public Collection<Integer> getAllowedPlayers() { return _raiders; }
     */
    public int getPlayersCount() {
        return _raiders.size();
    }

    public boolean isPlayerAllowed(L2PcInstance player) {
        if (player.isGM()) {
            return true;
        } else if (_raiders.containsKey(player.getObjectId())) {
            return true;
        }

        player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
        return false;
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
                    character.teleToLocation(MapRegionTable.TeleportWhereType.Town);
                }
            }
        }
    }

    public void oustAllPlayers() {
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
                    character.teleToLocation(MapRegionTable.TeleportWhereType.Town);
                }
            }
        }
        _raiders.clear();
    }

    public void allowPlayerEntry(L2PcInstance player, int minutes) {
        //if (!player.isGM())
        //{
        /*
         * if (_raiders.containsKey(player.getObjectId())) return;
         */
        _raiders.put(player.getObjectId(), (System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes)));
        //}
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }
}