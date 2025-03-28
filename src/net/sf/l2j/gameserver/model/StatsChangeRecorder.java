package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NicknameChanged;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.Broadcast;

public class StatsChangeRecorder {

    private L2PcInstance _activeChar;
    private int _accuracy;
    private int _attackSpeed;
    private int _castSpeed;
    private int _criticalHit;
    private int _evasion;
    private int _magicAttack;
    private int _magicDefence;
    private int _maxCp;
    private int _maxHp;
    private int _maxLoad;
    private int _curLoad;
    private int _maxMp;
    private int _physicAttack;
    private int _physicDefence;
    private int _level;
    private long _exp;
    private int _sp;
    private int _karma;
    private int _pk;
    private int _pvp;
    private int _runSpeed;
    private int _abnormalEffects;
    private String _title;

    public StatsChangeRecorder(L2PcInstance activeChar) {
        _activeChar = activeChar;
        refreshSaves();
    }

    public void refreshSaves() {
        if (_activeChar == null) {
            return;
        }

        _accuracy = _activeChar.getAccuracy();
        _attackSpeed = _activeChar.getPAtkSpd();
        _castSpeed = _activeChar.getMAtkSpd();
        _criticalHit = _activeChar.getCriticalHit(null, null);
        _evasion = _activeChar.getEvasionRate(null);
        _magicAttack = _activeChar.getMAtk(null, null);
        _magicDefence = _activeChar.getMDef(null, null);
        _maxCp = _activeChar.getMaxCp();
        _maxHp = _activeChar.getMaxHp();
        _maxLoad = _activeChar.getMaxLoad();
        _curLoad = _activeChar.getCurrentLoad();
        _maxMp = _activeChar.getMaxMp();
        _physicAttack = _activeChar.getPAtk(null);
        _physicDefence = _activeChar.getPDef(null);
        _level = _activeChar.getLevel();
        _exp = _activeChar.getExp();
        _sp = _activeChar.getSp();
        _karma = _activeChar.getKarma();

        _runSpeed = _activeChar.getRunSpeed();
        _pk = _activeChar.getPkKills();
        _pvp = _activeChar.getPvpKills();
        _abnormalEffects = _activeChar.getAbnormalEffect(); //TODO: почему-то мне кажется что его можно отослать отдельным пакетом
        _title = _activeChar.getTitle();
    }

    public void sendChanges() {
        if (_activeChar == null) {
            return;
        }

        // Броадкаст UserInfo и charInfo();
        if (needsUserInfoBroadcast()) {
            _activeChar.broadcastUserInfo();
            return;
        }

        sendGlobalInfo();
        sendPartyInfo();
        sendSelfInfo();

        refreshSaves();
    }

    /**
     * Отправляет броадкастом инфу всем игрокам
     */
    private void sendGlobalInfo() {
        StatusUpdate globalUpdate = new StatusUpdate(_activeChar.getObjectId());
        /**if(_attackSpeed != _activeChar.getPAtkSpd())
        globalUpdate.addAttribute(StatusUpdate.ATK_SPD, _activeChar.getPAtkSpd());
        
        if(_castSpeed != _activeChar.getMAtkSpd())
        globalUpdate.addAttribute(StatusUpdate.CAST_SPD, _activeChar.getMAtkSpd());
         */
        if (_karma != _activeChar.getKarma()) {
            globalUpdate.addAttribute(StatusUpdate.KARMA, _activeChar.getKarma());
        }

        if (globalUpdate.hasAttributes()) {
            _activeChar.broadcastPacket(globalUpdate);
        }

        // Проверка тайтла
        if (_title == null && _activeChar.getTitle() != null || _title != null && !_title.equals(_activeChar.getTitle())) {
            Broadcast.toSelfAndKnownPlayers(_activeChar, new NicknameChanged(_activeChar));
        }
    }

    /**
     * Отправляет инфу парти игрока.
     * Если парти нет, то отправляет лично игроку
     */
    private void sendPartyInfo() {
        // Эти статы нужно рассылать только для партии игрока
        StatusUpdate partyUpdate = new StatusUpdate(_activeChar.getObjectId());
        if (_maxCp != _activeChar.getMaxCp()) {
            partyUpdate.addAttribute(StatusUpdate.MAX_CP, _activeChar.getMaxCp());
        }

        if (_maxHp != _activeChar.getMaxHp()) {
            partyUpdate.addAttribute(StatusUpdate.MAX_HP, _activeChar.getMaxHp());
        }

        if (_maxMp != _activeChar.getMaxMp()) {
            partyUpdate.addAttribute(StatusUpdate.MAX_MP, _activeChar.getMaxMp());
        }

        L2Party party = _activeChar.getParty();
        if (partyUpdate.hasAttributes()) {
            if (party != null) {
                party.broadcastToPartyMembers(partyUpdate);
            } else {
                _activeChar.sendPacket(partyUpdate);
            }
        }
    }

    /**
     * Отправляет инфу только игроку
     */
    private void sendSelfInfo() {
        // Количество exp, sp, pk и левел - характеристики о которых другие игроки не обязаны знать
        if (_pk != _activeChar.getPkKills() || _pvp != _activeChar.getPvpKills() || _exp != _activeChar.getExp() || _sp != _activeChar.getSp()) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        // Проверка тайтла
        if (_title == null && _activeChar.getTitle() != null) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        } else if (_title != null && !_title.equals(_activeChar.getTitle())) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_accuracy != _activeChar.getAccuracy()) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_criticalHit != _activeChar.getCriticalHit(null, null)) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_evasion != _activeChar.getEvasionRate(null)) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_magicAttack != _activeChar.getMAtk(null, null)) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_magicDefence != _activeChar.getMDef(null, null)) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_maxLoad != _activeChar.getMaxLoad()) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_curLoad != _activeChar.getCurrentLoad()) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_physicAttack != _activeChar.getPAtk(null)) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_physicDefence != _activeChar.getPDef(null)) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
            return;
        }

        if (_level != _activeChar.getLevel()) {
            _activeChar.sendPacket(new UserInfo(_activeChar));
        }
    }

    /**
     * Проверяет нужно ли делать UserInfo broadcast. Дорогостоящая операция.
     * @return true если нужно.
     */
    private boolean needsUserInfoBroadcast() {
        return _runSpeed != _activeChar.getRunSpeed() || _abnormalEffects != _activeChar.getAbnormalEffect() || _attackSpeed != _activeChar.getPAtkSpd() || _castSpeed != _activeChar.getMAtkSpd();
    }
}