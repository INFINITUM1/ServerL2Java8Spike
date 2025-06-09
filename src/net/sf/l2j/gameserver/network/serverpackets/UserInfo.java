/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.network.serverpackets;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class UserInfo extends L2GameServerPacket {

    private boolean can_writeImpl = false;
    private L2PcInstance _cha;
    private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd, _relation;
    private float move_speed, attack_speed, col_radius, col_height;
    private Inventory _inv;
    //private Location _loc, _fishLoc;
    private int obj_id, _race, sex, base_class, level, curCp, maxCp, _enchant;
    private long _exp;
    private int curHp, maxHp, curMp, maxMp, curLoad, maxLoad, rec_left, rec_have;
    private int _str, _con, _dex, _int, _wit, _men, _sp, ClanPrivs, InventoryLimit;
    private int _patk, _patkspd, _pdef, evasion, accuracy, crit, _matk, _matkspd;
    private int _mdef, pvp_flag, karma, hair_style, hair_color, face, gm_commands;
    private int clan_id, clan_crest_id, ally_id, ally_crest_id, large_clan_crest_id;
    private int private_store, can_crystalize, pk_kills, pvp_kills, class_id;
    private int team, AbnormalEffect, noble, hero, fishing, cw_level;
    private int name_color, running, pledge_class, pledge_type, title_color, _lfp;
    //private int DefenceFire, DefenceWater, DefenceWind, DefenceEarth, DefenceHoly, DefenceUnholy;
    private byte mount_type;
    private String _name, title;
    private FastList<Integer> _cubics;
    //private int[] attackElement;

    /**
     * @param _characters
     */
    public UserInfo(L2PcInstance cha) {
        _cha = cha;
        _name = _cha.getName();
        clan_crest_id = _cha.getClanCrestId();
        ally_crest_id = _cha.getAllyCrestId();
        large_clan_crest_id = _cha.getClanCrestLargeId();

        if (_cha.isCursedWeaponEquiped()) {
            cw_level = CursedWeaponsManager.getInstance().getLevel(_cha.getCursedWeaponEquipedId());
        } else {
            cw_level = 0;
        }

        if (_cha.isMounted()) {
            _enchant = 0;
            //mount_id = _cha.getMountNpcId() + 1000000;
            mount_type = (byte) _cha.getMountType();
        } else {
            _enchant = (byte) _cha.getEnchantEffect();
            //mount_id = 0;
            mount_type = 0;
        }

        move_speed = _cha.getMovementSpeedMultiplier();
        _runSpd = (int) (_cha.getRunSpeed() / move_speed);
        _walkSpd = (int) (_cha.getWalkSpeed() / move_speed);
        _swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
        _swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
        _inv = _cha.getInventory();
        _relation = _cha.isClanLeader() ? 0x40 : 0;
        if (_cha.getSiegeState() == 1) {
            _relation |= 0x180;
        } else if (_cha.getSiegeState() == 2) {
            _relation |= 0x80;
        }

        //_loc = _cha.getLoc();
        obj_id = _cha.getObjectId();
        _race = _cha.getRace().ordinal();
        sex = _cha.getAppearance().getSex() ? 1 : 0;
        base_class = _cha.getBaseClass();
        level = _cha.getLevel();
        _exp = _cha.getExp();
        _str = _cha.getSTR();
        _dex = _cha.getDEX();
        _con = _cha.getCON();
        _int = _cha.getINT();
        _wit = _cha.getWIT();
        _men = _cha.getMEN();
        curHp = (int) _cha.getCurrentHp();
        maxHp = _cha.getMaxHp();
        curMp = (int) _cha.getCurrentMp();
        maxMp = _cha.getMaxMp();
        curLoad = _cha.getCurrentLoad();
        maxLoad = _cha.getMaxLoad();
        _sp = _cha.getSp();
        _patk = _cha.getPAtk(null);
        _patkspd = _cha.getPAtkSpd();
        _pdef = _cha.getPDef(null);
        evasion = _cha.getEvasionRate(null);
        accuracy = _cha.getAccuracy();
        crit = _cha.getCriticalHit(null, null);
        _matk = _cha.getMAtk(null, null);
        _matkspd = _cha.getMAtkSpd();
        _mdef = _cha.getMDef(null, null);
        pvp_flag = Config.FREE_PVP ? 0 : _cha.getPvpFlag(); // 0=white, 1=purple, 2=purpleblink
        karma = _cha.getKarma();
        attack_speed = _cha.getAttackSpeedMultiplier();
        col_radius = _cha.getColRadius();
        col_height = _cha.getColHeight();
        hair_style = _cha.getAppearance().getHairStyle();
        hair_color = _cha.getAppearance().getHairColor();
        face = _cha.getAppearance().getFace();
        // builder level активирует в клиенте админские команды
        gm_commands = _cha.isGM() ? 1 : 0;

        title = _cha.getTitle();
        if (_cha.isInvisible()) {
            title = "*" + title;//"Invisible";
        } else if (_cha.getPoly().isMorphed()) {
            title = "*,..,*";
        }
        if (_cha.getTvtKills() > 0) {
            title = "Kills: " + _cha.getTvtKills();
        }

        name_color = _cha.getAppearance().getNameColor();
        title_color = _cha.getAppearance().getTitleColor();

        clan_id = _cha.getClanId();
        ally_id = _cha.getAllyId();
        private_store = _cha.getPrivateStoreType();
        can_crystalize = _cha.hasDwarvenCraft() ? 1 : 0;
        pk_kills = _cha.getPkKills();
        pvp_kills = _cha.getPvpKills();
        _cubics = new FastList<Integer>();
        if (_cha.getCubics() != null) {
            for (int id : _cha.getCubics().keySet()) {
                _cubics.add(id);
            }
        }
        _lfp = _cha.isLFP() ? 1 : 0; // поиск пати
        AbnormalEffect = _cha.getAbnormalEffect();
        ClanPrivs = _cha.getClanPrivileges();
        rec_left = _cha.getRecomLeft(); //c2 recommendations remaining
        rec_have = _cha.getRecomHave(); //c2 recommendations received
        InventoryLimit = _cha.getInventoryLimit();
        class_id = _cha.getClassId().getId();
        if (class_id > 118) {
            class_id += 313;
        }
        maxCp = _cha.getMaxCp();
        curCp = (int) _cha.getCurrentCp();
        team = _cha.getTeam(); //team circle around feet 1= Blue, 2 = red
        noble = _cha.isNoble() ? 1 : 0; //0x01: symbol on char menu ctrl+I
        hero = _cha.isHero() || _cha.isGM() && Config.GM_HERO_AURA ? 1 : 0; //0x01: Hero Aura and symbol
        fishing = _cha.isFishing() ? 1 : 0; // Fishing Mode
        //_fishLoc = _cha.getFishLoc();
        running = _cha.isRunning() ? 0x01 : 0x00; //changes the Speed display on Status Window
        pledge_class = _cha.getPledgeClass();
        pledge_type = _cha.getPledgeType();
        /*transformation = _cha.getTransformation();
         attackElement = _cha.getAttackElement();
         DefenceFire = _cha.getDefenceFire();
         DefenceWater = _cha.getDefenceWater();
         DefenceWind = _cha.getDefenceWind();
         DefenceEarth = _cha.getDefenceEarth();
         DefenceHoly = _cha.getDefenceHoly();
         DefenceUnholy = _cha.getDefenceUnholy();
         agathion = _cha.getAgathion() != null ? _cha.getAgathion().getId() : 0; //агнишен*/

        _cha.refreshSavedStats();

        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        writeC(0x04);

        writeD(_cha.getX());
        writeD(_cha.getY());
        writeD(_cha.getZ());
        writeD(_cha.getHeading());
        writeD(obj_id);
        writeS(_name);
        writeD(_race);
        writeD(sex);
        writeD(base_class);
        writeD(level);
        writeQ(_exp);
        writeD(_str);
        writeD(_dex);
        writeD(_con);
        writeD(_int);
        writeD(_wit);
        writeD(_men);
        writeD(maxHp);
        writeD(curHp);
        writeD(maxMp);
        writeD(curMp);
        writeD(_sp);
        writeD(curLoad);
        writeD(maxLoad);
        writeD(0x28); // unknown

        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_DHAIR));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
        writeD(_inv.getPaperdollObjectId(Inventory.PAPERDOLL_FACE));

        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_REAR));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_NECK));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
        writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
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

        // c6 new h's
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
        // end of c6 new h's

        writeD(_patk);
        writeD(_patkspd);
        writeD(_pdef);
        writeD(evasion);
        writeD(accuracy);
        writeD(crit);
        writeD(_matk);

        writeD(_matkspd);
        writeD(_patkspd);

        writeD(_mdef);

        writeD(pvp_flag);
        writeD(karma);

        writeD(_runSpd);
        writeD(_walkSpd);
        writeD(_swimRunSpd); // swimspeed
        writeD(_swimWalkSpd); // swimspeed
        writeD(_flRunSpd);
        writeD(_flWalkSpd);
        writeD(_flyRunSpd);
        writeD(_flyWalkSpd);
        writeF(move_speed);
        writeF(attack_speed);

        writeF(col_radius);
        writeF(col_height);

        writeD(hair_style);
        writeD(hair_color);
        writeD(face);
        writeD(gm_commands);

        writeS(title);
        writeD(clan_id);
        writeD(clan_crest_id);
        writeD(ally_id);
        writeD(ally_crest_id);
        // 0x40 leader rights
        // siege flags: attacker - 0x180 sword over name, defender - 0x80 shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
        writeD(_relation);
        writeC(mount_type); // mount type
        writeC(private_store);
        writeC(can_crystalize);
        writeD(pk_kills);
        writeD(pvp_kills);
        writeH(_cubics.size());
        while (_cubics.size() > 0) {
            writeH(_cubics.removeFirst());
        }

        //_cubics.clear();
        //_cubics = null;
        writeC(_lfp); //1-find party members

        writeD(AbnormalEffect);
        writeC(0x11);
        writeD(ClanPrivs);
        writeH(rec_left);
        writeH(rec_have);
        writeD(0x00);//writeD(mount_id);
        writeH(InventoryLimit);
        writeD(class_id);
        writeD(0x01000000); // special effects? circles around player...
        writeD(maxCp);
        writeD(curCp);
        writeC(_enchant);
        writeC(team);
        writeD(large_clan_crest_id);
        writeC(noble);
        writeC(hero);
        writeC(fishing);
        writeD(_cha.GetFishx()); //fishing x
        writeD(_cha.GetFishy()); //fishing y
        writeD(_cha.GetFishz()); //fishing z
        writeD(name_color);

        writeC(running);
        writeD(pledge_class);
        writeD(pledge_type);
        writeD(title_color);
        writeD(cw_level);
    }
}
