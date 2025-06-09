package net.sf.l2j.gameserver.network.serverpackets;

import java.util.logging.Logger;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class CharInfo extends L2GameServerPacket {

    private static final Logger _log = Logger.getLogger(CharInfo.class.getName());
    private L2PcInstance _plr;
    private Inventory _inv;
    private int _x, _y, _z, _heading;
    private int _mAtkSpd, _pAtkSpd;
    private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
    private float _moveMultiplier, _attackSpeedMultiplier;
    private int _maxCp, _curCp;
    private String _name, _title;
    private int _objId, _race, _sex, base_class, pvp_flag, karma, rec_have, rec_left;
    private float speed_move, speed_atack, col_radius, col_height;
    private int hair_style, hair_color, face, abnormal_effect;
    private int clan_id, clan_crest_id, large_clan_crest_id, ally_id, ally_crest_id, class_id;
    private int _sit, _run, _combat, _dead, _invis, private_store, _enchant;
    private int _team, _noble, _hero, _fishing, mount_type, _lfp;
    private int plg_class, pledge_type, clan_rep_score, cw_level, mount_id;
    private int _nameColor, title_color;
    private FastList<L2CubicInstance> _cubics;
    private boolean can_writeImpl = false;

    /**
     * @param _characters
     */
    public CharInfo(L2PcInstance cha) {
        /*if(cha == null)
         return;
         _plr = cha;*/
        if ((_plr = cha) == null || _plr.isInvisible() || _plr.isDeleting()) {
            return;
        }

        _name = _plr.getName();
        _title = _plr.getTitle();

        if (_plr.isInOfflineMode()) {
            if (_plr.getPrivateStoreType() == 1) {
                _title = "OFF: Продаю";
            } else if (_plr.getPrivateStoreType() == 3) {
                _title = "OFF: Скупаю";
            }

            title_color = Integer.decode("0x1b7ccf");
            _nameColor = Integer.decode("0x1b7ccf");
        } else {
            if (_plr.isInvisible()) {
                _title = "*" + _title;//"Invisible";
            }
            title_color = _plr.getAppearance().getTitleColor();
            _nameColor = _plr.getAppearance().getNameColor();
        }

        if (_plr.getTvtKills() > 0) {
            _title = "Kills: " + _plr.getTvtKills();
        }

        clan_id = _plr.getClanId();
        if (clan_id > 0 && _plr.getClan() != null) {
            clan_rep_score = _plr.getClan().getReputationScore();
            clan_crest_id = _plr.getClanCrestId();
            ally_id = _plr.getAllyId();
            ally_crest_id = _plr.getAllyCrestId();
            large_clan_crest_id = _plr.getClanCrestLargeId();
        } else {
            clan_rep_score = 0;
            clan_crest_id = 0;
            ally_id = 0;
            ally_crest_id = 0;
            large_clan_crest_id = 0;
        }

        if (_plr.isCursedWeaponEquiped()) {
            cw_level = CursedWeaponsManager.getInstance().getLevel(_plr.getCursedWeaponEquipedId());
        } else {
            cw_level = 0;
        }

        if (_plr.isMounted()) {
            _enchant = 0;
            //mount_id = _plr.getMountNpcId() + 1000000;
            mount_type = (byte) _plr.getMountType();
        } else {
            _enchant = (byte) _plr.getEnchantEffect();
            //mount_id = 0;
            mount_type = 0;
        }

        _inv = _plr.getInventory();
        _mAtkSpd = _plr.getMAtkSpd();
        _pAtkSpd = _plr.getPAtkSpd();
        _moveMultiplier = _plr.getMovementSpeedMultiplier();
        _runSpd = (int) (_plr.getRunSpeed() / _moveMultiplier);
        _walkSpd = (int) (_plr.getWalkSpeed() / _moveMultiplier);
        _flRunSpd = _runSpd;
        _flWalkSpd = _walkSpd;
        _swimRunSpd = _runSpd;
        _swimWalkSpd = _walkSpd;
        _objId = _plr.getObjectId();
        _race = _plr.getRace().ordinal();
        _sex = _plr.getAppearance().getSex() ? 1 : 0;

        if (_plr.getClassIndex() == 0) {
            base_class = _plr.getClassId().getId();
        } else {
            base_class = _plr.getBaseClass();
        }

        pvp_flag = Config.FREE_PVP ? 0 : _plr.getPvpFlag(); // 0=white, 1=purple, 2=purpleblink
        karma = _plr.getKarma();
        speed_move = _plr.getMovementSpeedMultiplier();
        speed_atack = _plr.getAttackSpeedMultiplier();
        col_radius = _plr.getColRadius();
        col_height = _plr.getColHeight();
        hair_style = _plr.getAppearance().getHairStyle();
        hair_color = _plr.getAppearance().getHairColor();
        face = _plr.getAppearance().getFace();
        _sit = _plr.isSitting() ? 0 : 1; // standing = 1 sitting = 0
        _run = _plr.isRunning() ? 1 : 0; // running = 1 walking = 0
        _combat = _plr.isInCombat() ? 1 : 0;
        _dead = _plr.isAlikeDead() ? 1 : 0;

        //_invis = _plr.getAppearance().getInvisible() ? 1 : 0; // invisible = 1 visible = 0
        //if (_plr.inObserverMode())
        //	_invis = 1;
        _invis = _plr.inObserverMode() ? 1 : 0; // invisible = 1 visible = 0

        private_store = _plr.getPrivateStoreType(); // 1 - sellshop

        _cubics = new FastList<L2CubicInstance>();
        _cubics.addAll(_plr.getCubics().values());
        //for(int id : _plr.getCubics().keySet())
        //	_cubics.add(id);

        _lfp = _plr.isLFP() ? 1 : 0; // поиск пати
        abnormal_effect = _plr.getAbnormalEffect();
        if (_plr.isInvisible()) {
            abnormal_effect = L2Character.ABNORMAL_EFFECT_STEALTH;
        }

        rec_left = 0;//_plr.getRecomLeft();
        rec_have = _plr.getRecomHave();
        class_id = _plr.getClassId().getId();
        _team = _plr.getTeam(); // team circle around feet 1 = Blue, 2 = red

        _noble = _plr.isNoble() ? 1 : 0; // 0x01: symbol on char menu ctrl+I
        _hero = (_plr.isHero() || (_plr.isGM() && Config.GM_HERO_AURA)) ? 1 : 0; // 0x01: Hero Aura
        _fishing = _plr.isFishing() ? 1 : 0; // Fishing Mode
        plg_class = _plr.getPledgeClass();
        pledge_type = _plr.getPledgeType();
        _maxCp = _plr.getMaxCp();
        _curCp = (int) _plr.getCurrentCp();
        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) {
            return;
        }

        if (activeChar.equals(_plr)) {
            _log.severe("You cant send CharInfo about his character to active user!!!");
            return;
        }

        /*f(_plr.isInvisibleFor(activeChar))
         return;*/
        if (_plr.getPoly().isMorphed() && !_plr.isInOfflineMode()) {
            activeChar.sendPacket(new NpcInfoPoly(_plr, activeChar));
            return;
        }
        if (Config.ALLOW_GUILD_AURA && _team == 0 && _plr.isInGuild()) {
            if (_plr.isGuildEnemyFor(activeChar) == 1) {
                _team = 2;
            }
        } else if (_team == 0 && _plr.getOsTeam() > 0) {
            if (activeChar.getOsTeam() > 0 && activeChar.getOsTeam() != _plr.getOsTeam()) {
                _team = 2;
            }
        }

        writeC(0x03);
        writeD(_plr.getX());
        writeD(_plr.getY());
        writeD(_plr.getZ());
        writeD(_plr.getHeading()); //?
        writeD(_objId);
        writeS(_name);
        writeD(_race);
        writeD(_sex);
        writeD(base_class);

        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FACE));

        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);

        writeD(pvp_flag);
        writeD(karma);

        writeD(_mAtkSpd);
        writeD(_pAtkSpd);

        writeD(pvp_flag);
        writeD(karma);

        writeD(_runSpd);
        writeD(_walkSpd);
        writeD(_swimRunSpd/*0x32*/);  // swimspeed
        writeD(_swimWalkSpd/*0x32*/);  // swimspeed
        writeD(_flRunSpd);
        writeD(_flWalkSpd);
        writeD(_flyRunSpd);
        writeD(_flyWalkSpd);
        writeF(speed_move); // _plr.getProperMultiplier()
        writeF(speed_atack); // _plr.getAttackSpeedMultiplier()

        writeF(col_radius);
        writeF(col_height);

        writeD(hair_style);
        writeD(hair_color);
        writeD(face);
        writeS(_title);
        writeD(clan_id);
        writeD(clan_crest_id);
        writeD(ally_id);
        writeD(ally_crest_id);

        writeD(0);

        writeC(_sit);
        writeC(_run);
        writeC(_combat);
        writeC(_dead);
        writeC(_invis);
        writeC(mount_type); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
        writeC(private_store);

        writeH(_cubics.size());
        //while(_cubics.size() > 0)
        //	writeH(_cubics.removeFirst());
        for (L2CubicInstance cubic : _cubics) {
            writeH(cubic == null ? 0 : cubic.getId());
        }

        //_cubics.clear();
        writeC(_lfp); // find party members
        writeD(abnormal_effect);
        writeC(rec_left);
        writeH(rec_have);
        writeD(class_id);
        writeD(_maxCp);
        writeD(_curCp);
        writeC(_enchant);

        writeC(_team);

        writeD(large_clan_crest_id);

        writeC(_noble);
        writeC(_hero);

        writeC(_fishing);
        writeD(_plr.GetFishx());
        writeD(_plr.GetFishy());
        writeD(_plr.GetFishz());

        writeD(_nameColor);

        writeD(_plr.getHeading()); // isRunning() as in UserInfo?

        writeD(plg_class);
        writeD(pledge_type);

        writeD(title_color);
        writeD(cw_level);
        writeD(clan_rep_score);
    }

    @Override
    public boolean isCharInfo() {
        return true;
    }
}
