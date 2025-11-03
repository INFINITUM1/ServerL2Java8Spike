package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.4.2.9 $ $Date: 2005/04/11 10:05:54 $
 */
public final class NpcInfo extends L2GameServerPacket {

    private boolean can_writeImpl = false;
    private L2Character _activeChar;
    private int _x, _y, _z, _heading;
    private int _idTemplate;
    private boolean _isAttackable, _isSummoned;
    private int _mAtkSpd, _pAtkSpd, _showSpawnAnimation = 0;
    private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
    private int _rhand, _lhand, _chest, _val, _pvpFlag;
    private int _collisionHeight, _collisionRadius;
    private String _name = "";
    private L2Summon _summon;
    private String _title = "";
    private int _form = 0;
    private boolean _isChampion = false;
    private boolean _isChampion1 = false;
    private int _team = 0;
    private int _weaponEnhcant = 0;
    private int _clanId = 0;
    private int _clanCrest = 0;

    /**
     * @param _characters
     */
    public NpcInfo(L2NpcInstance cha, L2Character attacker) {
        _activeChar = cha;
        _idTemplate = cha.getTemplate().idTemplate;
        _isAttackable = cha.isAutoAttackable(attacker);
        _rhand = cha.getRightHandItem();
        _lhand = cha.getLeftHandItem();
        _isSummoned = cha.isVis();
        _isChampion = cha.isChampion();
        _isChampion1 = cha.isChampion1();
        _collisionHeight = cha.getCollisionHeight();
        _collisionRadius = cha.getCollisionRadius();
        // Имя NPC (если задано с сервера)
        if (cha.getTemplate().serverSideName) {
            _name = cha.getTemplate().name;
        }

        boolean championTitleSet = false;

        // Установка титула чемпиона
        if (Config.L2JMOD_CHAMPION_ENABLE && _isChampion) {
            _title = "Заточки SCROLLS";
            championTitleSet = true;
        } else if (Config.L2JMOD_CHAMPION_ENABLE_BLUE && _isChampion1) {
            _title = "BONUS";
            championTitleSet = true;
        }

        // Если титул чемпиона НЕ установлен — продолжаем обычную логику
        if (!championTitleSet) {
            if (cha.getTemplate().serverSideTitle) {
                _title = cha.getTemplate().title;
            } else {
                _title = cha.getTitle();
            }

            if (!Config.SHOW_NPC_LVL && _activeChar.isL2Monster()) {
                _title = " ";
            }

            _team = cha.getTeamAura();
            if (_team > 0) {
                _title = cha.getTitle();
            }
        } else {
            // Всё равно передаём команду, даже если чемпион (на всякий случай)
            _team = cha.getTeamAura();
        }

        // if (cha.getTemplate().serverSideName) {
        // _name = cha.getTemplate().name;
        // }

        // if (Config.L2JMOD_CHAMPION_ENABLE && _isChampion) {
        // _title = ("Champion");
        // }
        // if (Config.L2JMOD_CHAMPION_ENABLE_BLUE && _isChampion1) {
        // _title = ("Champion");
        // } else if (cha.getTemplate().serverSideTitle) {
        // _title = cha.getTemplate().title;
        // } else {
        // _title = cha.getTitle();
        // }
        // if (!Config.SHOW_NPC_LVL && _activeChar.isL2Monster() && !_isChampion) {
        // _title = " ";
        // }
        // if (!Config.SHOW_NPC_LVL && _activeChar.isL2Monster() && !_isChampion1) {
        // _title = " ";
        // }

        // _team = cha.getTeamAura();
        // if (_team > 0 && !_isChampion && !_isChampion1) {
        // _title = cha.getTitle();
        // }

        _x = _activeChar.getX();
        _y = _activeChar.getY();
        _z = _activeChar.getZ();
        _heading = _activeChar.getHeading();
        _mAtkSpd = _activeChar.getMAtkSpd();
        _pAtkSpd = _activeChar.getPAtkSpd();
        _runSpd = _activeChar.getRunSpeed();
        _walkSpd = _activeChar.getWalkSpeed();
        _swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
        _swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
        _showSpawnAnimation = cha.isShowSpawnAnimation();
        _weaponEnhcant = cha.getWeaponEnchant();
        _clanId = 0;
        _clanCrest = 0;

        can_writeImpl = true;
    }

    public NpcInfo(L2Summon cha, L2Character attacker, int val) {
        _activeChar = cha;
        _summon = cha;
        _idTemplate = cha.getTemplate().idTemplate;
        _isAttackable = cha.isAutoAttackable(attacker); // (cha.getKarma() > 0);
        _rhand = cha.getWeapon();
        _lhand = 0;
        _chest = cha.getArmor();
        _val = val;
        _collisionHeight = _activeChar.getTemplate().collisionHeight;
        _collisionRadius = _activeChar.getTemplate().collisionRadius;
        _name = cha.getName();
        _title = cha.getOwner() != null ? (cha.getOwner().isOnline() == 0 ? "" : cha.getOwner().getName()) : ""; // when
                                                                                                                 // owner
                                                                                                                 // online,
                                                                                                                 // summon
                                                                                                                 // will
                                                                                                                 // show
                                                                                                                 // in
                                                                                                                 // title
                                                                                                                 // owner
                                                                                                                 // name

        if (cha.isAgathion()) {
            _name = " ";
            _title = " ";
        }

        _x = _activeChar.getX();
        _y = _activeChar.getY();
        _z = _activeChar.getZ();
        _heading = _activeChar.getHeading();
        _mAtkSpd = _activeChar.getMAtkSpd();
        _pAtkSpd = _activeChar.getPAtkSpd();
        _runSpd = _summon.getPetSpeed();
        _walkSpd = _summon.isMountable() ? 45 : 30;
        _swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
        _swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
        _pvpFlag = Config.FREE_PVP ? 0 : _summon.getOwner().getPvpFlag();
        _team = _summon.getOwner().getTeam();
        // _showSpawnAnimation = cha.isShowSpawnAnimation();
        can_writeImpl = true;
    }

    public NpcInfo(L2Summon cha, L2Character attacker) {
        _activeChar = cha;
        _summon = cha;
        _idTemplate = cha.getTemplate().idTemplate;
        _isAttackable = cha.isAutoAttackable(attacker); // (cha.getKarma() > 0);
        _rhand = 0;
        _lhand = 0;
        _isSummoned = cha.isShowSummonAnimation();
        _collisionHeight = _activeChar.getTemplate().collisionHeight;
        _collisionRadius = _activeChar.getTemplate().collisionRadius;
        _name = cha.getName();
        _title = cha.getOwner() != null ? (cha.getOwner().isOnline() == 0 ? "" : cha.getOwner().getName()) : ""; // when
                                                                                                                 // owner
                                                                                                                 // online,
                                                                                                                 // summon
                                                                                                                 // will
                                                                                                                 // show
                                                                                                                 // in
                                                                                                                 // title
                                                                                                                 // owner
                                                                                                                 // name

        if (cha.isAgathion()) {
            _name = " ";
            _title = " ";
        }

        _x = _activeChar.getX();
        _y = _activeChar.getY();
        _z = _activeChar.getZ();
        _heading = _activeChar.getHeading();
        _mAtkSpd = _activeChar.getMAtkSpd();
        _pAtkSpd = _activeChar.getPAtkSpd();
        _runSpd = _activeChar.getRunSpeed();
        _walkSpd = _activeChar.getWalkSpeed();
        _swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
        _swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
        _pvpFlag = Config.FREE_PVP ? 0 : _summon.getOwner().getPvpFlag();
        _team = _summon.getOwner().getTeam();
        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        if (_activeChar.isL2Summon()) {
            if (_activeChar.getOwner() != null && _activeChar.getOwner().isInvisible()) {
                return;
            }
        }

        writeC(0x16);
        writeD(_activeChar.getObjectId());
        writeD(_idTemplate + 1000000); // npctype id
        writeD(_isAttackable ? 1 : 0);
        writeD(_x);
        writeD(_y);
        writeD(_z);
        writeD(_heading);
        writeD(0x00);
        writeD(_mAtkSpd);
        writeD(_pAtkSpd);
        writeD(_runSpd);
        writeD(_walkSpd);
        writeD(_swimRunSpd/*
                           * 0x32
                           */); // swimspeed
        writeD(_swimWalkSpd/*
                            * 0x32
                            */); // swimspeed
        writeD(_flRunSpd);
        writeD(_flWalkSpd);
        writeD(_flyRunSpd);
        writeD(_flyWalkSpd);
        writeF(1.1/*
                   * _activeChar.getProperMultiplier()
                   */);
        // writeF(1/*_activeChar.getAttackSpeedMultiplier()*/);
        writeF(_pAtkSpd / 277.478340719);
        writeF(_collisionRadius);
        writeF(_collisionHeight);
        writeD(_rhand); // right hand weapon
        writeD(0);
        writeD(_lhand); // left hand weapon
        writeC(1); // name above char 1=true ... ??
        writeC(_activeChar.isRunning() ? 1 : 0);
        writeC(_activeChar.isInCombat() ? 1 : 0);
        writeC(_activeChar.isAlikeDead() ? 1 : 0);
        writeC(_showSpawnAnimation);// writeC(_isSummoned ? 2 : 0); // invisible ?? 0=false 1=true 2=summoned (only
                                    // works if model has a summon animation)
        writeS(_name);
        writeS(_title);

        if (_activeChar.isL2Summon()) {
            writeD(0x01);// Title color 0=client default
        } else {
            writeD(_isChampion || _isChampion1 ? 0x01 : 0x00);
        }

        writeD(0x00); // pvp_flag

        // hmm karma ??
        if (_activeChar.isL2Summon()) {
            writeD(_pvpFlag);
        } else {
            writeD(_isChampion || _isChampion1 ? 0x03 : 0x00);
        }

        writeD(_activeChar.getAbnormalEffect()); // C2
        /*
         * if (_activeChar.isL2Summon()) {
         * writeD(0x01);
         * } else {
         * writeD(0x00);
         * }
         */

        // writeD(0000); // C2
        // writeD(0000); // C2
        writeD(_clanId);
        writeD(_clanCrest);
        writeD(0000); // C2
        writeD(0000); // C2
        writeC(0000); // C2

        writeC(_team);// аура

        writeF(_collisionRadius);
        writeF(_collisionHeight);
        writeD(_weaponEnhcant); // C4
        // writeD(0x00); // C4
        writeD(0x00); // C6
    }
}
