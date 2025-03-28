package net.sf.l2j.gameserver.model.entity;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent.StoredBuff;
import net.sf.l2j.gameserver.templates.L2PcTemplate;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;

public class TvTEventTeleporter implements Runnable {

    /**
     * The instance of the player to teleport
     */
    private L2PcInstance _player;
    /**
     * Coordinates of the spot to teleport to
     */
    private Location _coordinates;
    /**
     * Admin removed this player from event
     */
    private boolean _adminRemove;

    /**
     * Initialize the teleporter and start the delayed task
     *
     * @param playerInstance
     * @param coordinates
     * @param reAdd
     */
    public TvTEventTeleporter(L2PcInstance playerInstance, Location coordinates, boolean fastSchedule, boolean adminRemove) {
        createTask(playerInstance, coordinates, fastSchedule, adminRemove, null);
    }

    public TvTEventTeleporter(L2PcInstance playerInstance, Location coordinates, boolean fastSchedule, boolean adminRemove, FastList<StoredBuff> sb) {
        createTask(playerInstance, coordinates, fastSchedule, adminRemove, sb);
    }

    private void createTask(L2PcInstance playerInstance, Location coordinates, boolean fastSchedule, boolean adminRemove, FastList<StoredBuff> sb) {
        _player = playerInstance;
        _coordinates = coordinates;
        _adminRemove = adminRemove;

        // in config as seconds
        long delay = (TvTEvent.isStarted() ? Config.TVT_EVENT_RESPAWN_TELEPORT_DELAY : Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000;

        if (fastSchedule) {
            delay = 0;
        }

        ThreadPoolManager.getInstance().scheduleGeneral(this, delay);
    }

    /**
     * The task method to teleport the player<br> 1. Unsummon pet if there is
     * one 2. Remove all effects 3. Revive and full heal the player 4. Teleport
     * the player 5. Broadcast status and user info
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if (_player == null) {
            return;
        }

        if (TvTEvent.isStarted() && !_adminRemove) {
            if (!TvTEvent.isRegisteredPlayer2(_player.getObjectId())) {
                return;
            }
        }

        L2Summon summon = _player.getPet();

        if (summon != null) {
            summon.unSummon(_player);
        }

        _player.stopAllEffects();
        _player.doEventBuff();
        _player.doRevive();

        if (TvTEvent.isStarted() && !_adminRemove) {
            _player.setEventChannel(8);
            _player.setTeam(TvTEvent.getParticipantTeamId(_player.getName()) + 1);
        } else {
            _player.setEventChannel(1);
            _player.setTeam(0);
            _player.restoreEventSkills();
        }

        _player.setCurrentCp(_player.getMaxCp());
        _player.setCurrentHp(_player.getMaxHp());
        _player.setCurrentMp(_player.getMaxMp());

        //if (TvTEvent.isStarted() && !_adminRemove)
        //	_player.teleToLocationEvent(_coordinates[0]+Rnd.get(100), _coordinates[1]+Rnd.get(100), _coordinates[2], false);
        //else
        _player.setEventWait(false);
        _player.teleToLocationEvent(_coordinates.x + Rnd.get(300), _coordinates.y + Rnd.get(300), _coordinates.z, false);

        _player.broadcastStatusUpdate();

        if (Config.TVT_BUFF) {
            FastMap<Integer, Integer> buffs = null;
            if (_player.isMageClass()) {
                buffs = Config.ON_ENTER_M_BUFFS;
            } else {
                buffs = Config.ON_ENTER_F_BUFFS;
            }


            Integer id = null;
            Integer lvl = null;
            SkillTable _st = SkillTable.getInstance();
            for (FastMap.Entry<Integer, Integer> e = buffs.head(), end = buffs.tail(); (e = e.getNext()) != end; ) {
                id = e.getKey(); // No typecast necessary.
                lvl = e.getValue(); // No typecast necessary.
                if (id == null || lvl == null) {
                    continue;
                }

                _st.getInfo(id, lvl).getEffects(_player, _player);
            }
            _player.setCurrentCp(_player.getMaxCp());
            _player.setCurrentHp(_player.getMaxHp());
            _player.setCurrentMp(_player.getMaxMp());
        }
    }
}
