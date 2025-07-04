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
package net.sf.l2j.gameserver.templates;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;

/**
 * This class contains all informations concerning the item (weapon, armor,
 * etc).<BR> Mother class of : <LI>L2Armor</LI> <LI>L2EtcItem</LI>
 * <LI>L2Weapon</LI>
 *
 * @version $Revision: 1.7.2.2.2.5 $ $Date: 2005/04/06 18:25:18 $
 */
public abstract class L2Item {

    public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
    public static final int TYPE1_SHIELD_ARMOR = 1;
    public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;
    public static final int TYPE2_WEAPON = 0;
    public static final int TYPE2_SHIELD_ARMOR = 1;
    public static final int TYPE2_ACCESSORY = 2;
    public static final int TYPE2_QUEST = 3;
    public static final int TYPE2_MONEY = 4;
    public static final int TYPE2_OTHER = 5;
    public static final int TYPE2_PET_WOLF = 6;
    public static final int TYPE2_PET_HATCHLING = 7;
    public static final int TYPE2_PET_STRIDER = 8;
    public static final int TYPE2_PET_BABY = 9;
    public static final int SLOT_NONE = 0x0000;
    public static final int SLOT_UNDERWEAR = 0x0001;
    public static final int SLOT_R_EAR = 0x0002;
    public static final int SLOT_L_EAR = 0x0004;
    public static final int SLOT_NECK = 0x0008;
    public static final int SLOT_R_FINGER = 0x0010;
    public static final int SLOT_L_FINGER = 0x0020;
    public static final int SLOT_HEAD = 0x0040;
    public static final int SLOT_R_HAND = 0x0080;
    public static final int SLOT_L_HAND = 0x0100;
    public static final int SLOT_GLOVES = 0x0200;
    public static final int SLOT_CHEST = 0x0400;
    public static final int SLOT_LEGS = 0x0800;
    public static final int SLOT_FEET = 0x1000;
    public static final int SLOT_BACK = 0x2000;
    public static final int SLOT_LR_HAND = 0x4000;
    public static final int SLOT_FULL_ARMOR = 0x8000;
    public static final int SLOT_HAIR = 0x010000;
    public static final int SLOT_WOLF = 0x020000;
    public static final int SLOT_HATCHLING = 0x100000;
    public static final int SLOT_STRIDER = 0x200000;
    public static final int SLOT_BABYPET = 0x400000;
    public static final int SLOT_FACE = 0x040000;
    public static final int SLOT_DHAIR = 0x080000;
    public static final int MATERIAL_STEEL = 0x00; // ??
    public static final int MATERIAL_FINE_STEEL = 0x01; // ??
    public static final int MATERIAL_BLOOD_STEEL = 0x02; // ??
    public static final int MATERIAL_BRONZE = 0x03; // ??
    public static final int MATERIAL_SILVER = 0x04; // ??
    public static final int MATERIAL_GOLD = 0x05; // ??
    public static final int MATERIAL_MITHRIL = 0x06; // ??
    public static final int MATERIAL_ORIHARUKON = 0x07; // ??
    public static final int MATERIAL_PAPER = 0x08; // ??
    public static final int MATERIAL_WOOD = 0x09; // ??
    public static final int MATERIAL_CLOTH = 0x0a; // ??
    public static final int MATERIAL_LEATHER = 0x0b; // ??
    public static final int MATERIAL_BONE = 0x0c; // ??
    public static final int MATERIAL_HORN = 0x0d; // ??
    public static final int MATERIAL_DAMASCUS = 0x0e; // ??
    public static final int MATERIAL_ADAMANTAITE = 0x0f; // ??
    public static final int MATERIAL_CHRYSOLITE = 0x10; // ??
    public static final int MATERIAL_CRYSTAL = 0x11; // ??
    public static final int MATERIAL_LIQUID = 0x12; // ??
    public static final int MATERIAL_SCALE_OF_DRAGON = 0x13; // ??
    public static final int MATERIAL_DYESTUFF = 0x14; // ??
    public static final int MATERIAL_COBWEB = 0x15; // ??
    public static final int MATERIAL_SEED = 0x15; // ??
    public static final int CRYSTAL_NONE = 0x00; // ??
    public static final int CRYSTAL_D = 0x01; // ??
    public static final int CRYSTAL_C = 0x02; // ??
    public static final int CRYSTAL_B = 0x03; // ??
    public static final int CRYSTAL_A = 0x04; // ??
    public static final int CRYSTAL_S = 0x05; // ??
    private static final int[] crystalItemId = {0, 1458, 1459, 1460, 1461, 1462};
    private static final int[] crystalEnchantBonusArmor = {0, 11, 6, 11, 19, 25};
    private static final int[] crystalEnchantBonusWeapon = {0, 90, 45, 67, 144, 250};
    private final int _itemId;
    private String _name;
    private final int _type1;	// needed for item list (inventory)
    private final int _type2;	// different lists for armor, weapon, etc
    private final int _weight;
    private final boolean _crystallizable;
    private final boolean _stackable;
    private final int _materialType;
    private final int _crystalType; // default to none-grade
    private final int _duration;
    private final int _bodyPart;
    private final int _referencePrice;
    private final int _crystalCount;
    private final boolean _sellable;
    private final boolean _dropable;
    private final boolean _destroyable;
    private final boolean _tradeable;
    private final String _icon;
    private boolean _oly;
    private boolean _boss = false;
    private long _expire = 0;
    @SuppressWarnings("unchecked")
    protected final Enum _type;
    protected FuncTemplate[] _funcTemplates;
    protected EffectTemplate[] _effectTemplates;
    protected L2Skill[] _skills;
    private static final Func[] _emptyFunctionSet = new Func[0];
    protected static final L2Effect[] _emptyEffectSet = new L2Effect[0];
    private int _maxEnchant;
    private boolean _hippy = false;
    private boolean _isBow = false;
    private boolean _isDual = false;
    private boolean _isFist = false;
    private L2Skill _customSkill = null;

    /**
     * Constructor of the L2Item that fill class variables.<BR><BR>
     * <U><I>Variables filled :</I></U><BR> <LI>type</LI> <LI>_itemId</LI>
     * <LI>_name</LI> <LI>_type1 & _type2</LI> <LI>_weight</LI>
     * <LI>_crystallizable</LI> <LI>_stackable</LI> <LI>_materialType &
     * _crystalType & _crystlaCount</LI> <LI>_duration</LI> <LI>_bodypart</LI>
     * <LI>_referencePrice</LI> <LI>_sellable</LI>
     *
     * @param type : Enum designating the type of the item
     * @param set : StatsSet corresponding to a set of couples (key,value) for
     * description of the item
     */
    @SuppressWarnings("unchecked")
    protected L2Item(Enum type, StatsSet set) {
        _type = type;
        _itemId = set.getInteger("item_id");
        _name = set.getString("name");
        _type1 = set.getInteger("type1");	// needed for item list (inventory)
        _type2 = set.getInteger("type2");	// different lists for armor, weapon, etc
        _weight = set.getInteger("weight");
        _crystallizable = set.getBool("crystallizable");
        _stackable = set.getBool("stackable", false);
        _materialType = set.getInteger("material");
        _duration = set.getInteger("duration");
        _bodyPart = set.getInteger("bodypart");
        _referencePrice = set.getInteger("price");
        _crystalCount = set.getInteger("crystal_count", 0);
        _sellable = set.getBool("sellable", true);
        _dropable = set.getBool("dropable", true);
        _destroyable = set.getBool("destroyable", true);
        _tradeable = set.getBool("tradeable", true);
        _icon = set.getString("icon");
        if (!_name.startsWith("Shadow") && _duration > 0) {
            _expire = TimeUnit.MINUTES.toMillis(_duration);
            //String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(item.getExpire()));
            _name = _name + "(" + (_duration / 60) + " часa)";
        }

        Integer maxEnchant = Config.ENCHANT_LIMITS.get(_itemId);
        if (maxEnchant != null) {
            _maxEnchant = maxEnchant;
        } else {
            switch (_type2) {
                case TYPE2_WEAPON:
                    _maxEnchant = Config.ENCHANT_MAX_WEAPON;
                    break;
                case TYPE2_ACCESSORY:
                    _maxEnchant = Config.ENCHANT_MAX_JEWELRY;
                    break;
                case TYPE2_SHIELD_ARMOR:
                    _maxEnchant = Config.ENCHANT_MAX_ARMOR;
                    break;
            }
        }

        if (Config.HIPPY_ITEMS.contains(_itemId)) {
            _hippy = true;
        }

        if (Config.BOSS_ITEMS.contains(_itemId)) {
            _boss = true;
        }

        if (Config.F_OLY_ITEMS.contains(_itemId)) {
            _oly = true;
        }

        if (_type == L2WeaponType.BOW) {
            _isBow = true;
        }

        if (_type == L2WeaponType.DUAL
                || (_type == L2WeaponType.SWORD && _bodyPart == 16384)) {
            _isDual = true;
        }

        if (_type == L2WeaponType.DUALFIST) {
            _isFist = true;
        }

        switch (_itemId) {
            case 729:
            case 6569:
            case 731:
            case 730:
            case 6570:
            case 732:
            case 15003:
            case 15008:
            case 15018:
            case 15013:
                _crystalType = CRYSTAL_A;
                break;
            case 947:
            case 6571:
            case 949:
            case 948:
            case 6572:
            case 950:
            case 15002:
            case 15007:
            case 15012:
            case 15017:
                _crystalType = CRYSTAL_B;
                break;
            case 951:
            case 6573:
            case 953:
            case 952:
            case 6574:
            case 954:
            case 15001:
            case 15006:
            case 15011:
            case 15016:
                _crystalType = CRYSTAL_C;
                break;
            case 955:
            case 6575:
            case 957:
            case 956:
            case 6576:
            case 958:
            case 15000:
            case 15005:
            case 15010:
            case 15015:
                _crystalType = CRYSTAL_D;
                break;
            case 959:
            case 6577:
            case 961:
            case 960:
            case 6578:
            case 962:
            case 15004:
            case 15009:
            case 15014:
            case 15019:
            case 4355:
                _crystalType = CRYSTAL_S;
                break;
            default:
                _crystalType = set.getInteger("crystal_type", CRYSTAL_NONE); // default to none-grade
        }

        checkCustomSkill(Config.TATOO_SKILLS_LIST.get(_itemId));
    }

    private void checkCustomSkill(Integer skill) {
        if (!Config.TATOO_SKILLS || skill == null) {
            return;
        }

        _customSkill = SkillTable.getInstance().getInfo(skill, 1);
    }

    public L2Skill getCustomSkill() {
        return _customSkill;
    }

    public boolean isBowWeapon() {
        return _isBow;
    }

    public boolean isDualWeapon() {
        return _isDual;
    }

    public boolean isFistWeapon() {
        return _isFist;
    }

    /**
     * Returns the itemType.
     *
     * @return Enum
     */
    @SuppressWarnings("unchecked")
    public Enum getItemType() {
        return _type;
    }

    /**
     * Returns the duration of the item
     *
     * @return int
     */
    public final int getDuration() {
        return _duration;
    }

    public final long getExpire() {
        return _expire;
    }

    /**
     * Returns the ID of the iden
     *
     * @return int
     */
    public final int getItemId() {
        return _itemId;
    }

    public abstract int getItemMask();

    /**
     * Return the type of material of the item
     *
     * @return int
     */
    public final int getMaterialType() {
        return _materialType;
    }

    /**
     * Returns the type 2 of the item
     *
     * @return int
     */
    public final int getType2() {
        return _type2;
    }

    /**
     * Returns the weight of the item
     *
     * @return int
     */
    public final int getWeight() {
        return _weight;
    }

    /**
     * Returns if the item is crystallizable
     *
     * @return boolean
     */
    public final boolean isCrystallizable() {
        return _crystallizable;
    }

    /**
     * Return the type of crystal if item is crystallizable
     *
     * @return int
     */
    public final int getCrystalType() {
        return _crystalType;
    }

    /**
     * Return the type of crystal if item is crystallizable
     *
     * @return int
     */
    public final int getCrystalItemId() {
        return crystalItemId[_crystalType];
    }

    /**
     * Returns the grade of the item.<BR><BR> <U><I>Concept :</I></U><BR> In
     * fact, this fucntion returns the type of crystal of the item.
     *
     * @return int
     */
    public final int getItemGrade() {
        return getCrystalType();
    }

    /**
     * Returns the quantity of crystals for crystallization
     *
     * @return int
     */
    public final int getCrystalCount() {
        return _crystalCount;
    }

    /**
     * Returns the quantity of crystals for crystallization on specific enchant
     * level
     *
     * @return int
     */
    public final int getCrystalCount(int enchantLevel) {
        if (enchantLevel > 3) {
            switch (_type2) {
                case TYPE2_SHIELD_ARMOR:
                case TYPE2_ACCESSORY:
                    return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * (3 * enchantLevel - 6);
                case TYPE2_WEAPON:
                    return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * (2 * enchantLevel - 3);
                default:
                    return _crystalCount;
            }
        } else if (enchantLevel > 0) {
            switch (_type2) {
                case TYPE2_SHIELD_ARMOR:
                case TYPE2_ACCESSORY:
                    return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * enchantLevel;
                case TYPE2_WEAPON:
                    return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * enchantLevel;
                default:
                    return _crystalCount;
            }
        } else {
            return _crystalCount;
        }
    }

    /**
     * Returns the name of the item
     *
     * @return String
     */
    public final String getName() {
        return _name;
    }

    /**
     * Return the part of the body used with the item.
     *
     * @return int
     */
    public final int getBodyPart() {
        return _bodyPart;
    }

    /**
     * Returns the type 1 of the item
     *
     * @return int
     */
    public final int getType1() {
        return _type1;
    }

    /**
     * Returns if the item is stackable
     *
     * @return boolean
     */
    public final boolean isStackable() {
        return _stackable;
    }

    /**
     * Returns if the item is consumable
     *
     * @return boolean
     */
    public boolean isConsumable() {
        return false;
    }

    /**
     * Returns the price of reference of the item
     *
     * @return int
     */
    public final int getReferencePrice() {
        return (isConsumable() ? (int) (_referencePrice * Config.RATE_CONSUMABLE_COST) : _referencePrice);
    }

    /**
     * Returns if the item can be sold
     *
     * @return boolean
     */
    public final boolean isSellable() {
        return _sellable;
    }

    /**
     * Returns if the item can dropped
     *
     * @return boolean
     */
    public final boolean isDropable() {
        return _dropable;
    }

    /**
     * Returns if the item can destroy
     *
     * @return boolean
     */
    public final boolean isDestroyable() {
        return _destroyable;
    }

    /**
     * Returns if the item can add to trade
     *
     * @return boolean
     */
    public final boolean isTradeable() {
        return _tradeable;
    }

    /**
     * Returns if item is for hatchling
     *
     * @return boolean
     */
    public boolean isForHatchling() {
        return (_type2 == TYPE2_PET_HATCHLING);
    }

    /**
     * Returns if item is for strider
     *
     * @return boolean
     */
    public boolean isForStrider() {
        return (_type2 == TYPE2_PET_STRIDER);
    }

    /**
     * Returns if item is for wolf
     *
     * @return boolean
     */
    public boolean isForWolf() {
        return (_type2 == TYPE2_PET_WOLF);
    }

    /**
     * Returns if item is for wolf
     *
     * @return boolean
     */
    public boolean isForBabyPet() {
        return (_type2 == TYPE2_PET_BABY);
    }

    /**
     * Returns array of Func objects containing the list of functions used by
     * the item
     *
     * @param instance : L2ItemInstance pointing out the item
     * @param player : L2Character pointing out the player
     * @return Func[] : array of functions
     */
    public Func[] getStatFuncs(L2ItemInstance instance, L2Character player) {
        if (player.getChannel() == 8 && _oly) {
            return _emptyFunctionSet;
        }

        if (_funcTemplates == null) {
            return _emptyFunctionSet;
        }

        List<Func> funcs = new FastList<Func>();
        for (FuncTemplate t : _funcTemplates) {
            Env env = new Env();
            env.cha = player;
            env.target = player;
            env.item = instance;
            Func f = t.getFunc(env, this); // skill is owner
            if (f != null) {
                funcs.add(f);
            }
        }
        if (funcs.isEmpty()) {
            return _emptyFunctionSet;
        }
        return funcs.toArray(new Func[funcs.size()]);
    }

    /**
     * Returns the effects associated with the item.
     *
     * @param instance : L2ItemInstance pointing out the item
     * @param player : L2Character pointing out the player
     * @return L2Effect[] : array of effects generated by the item
     */
    public L2Effect[] getEffects(L2ItemInstance instance, L2Character player) {
        if (_effectTemplates == null) {
            return _emptyEffectSet;
        }
        List<L2Effect> effects = new FastList<L2Effect>();
        for (EffectTemplate et : _effectTemplates) {
            Env env = new Env();
            env.cha = player;
            env.target = player;
            env.item = instance;
            L2Effect e = et.getEffect(env);
            if (e != null) {
                e.scheduleEffect();
                effects.add(e);
            }
        }
        if (effects.isEmpty()) {
            return _emptyEffectSet;
        }
        return effects.toArray(new L2Effect[effects.size()]);
    }

    /**
     * Returns effects of skills associated with the item.
     *
     * @param caster : L2Character pointing out the caster
     * @param target : L2Character pointing out the target
     * @return L2Effect[] : array of effects generated by the skill
     */
    public L2Effect[] getSkillEffects(L2Character caster, L2Character target) {
        if (_skills == null) {
            return _emptyEffectSet;
        }
        List<L2Effect> effects = new FastList<L2Effect>();

        for (L2Skill skill : _skills) {
            if (!skill.checkCondition(caster, target, true)) {
                continue; // Skill condition not met
            }
            if (target.getFirstEffect(skill.getId()) != null) {
                target.removeEffect(target.getFirstEffect(skill.getId()));
            }
            for (L2Effect e : skill.getEffects(caster, target)) {
                effects.add(e);
            }
        }
        if (effects.isEmpty()) {
            return _emptyEffectSet;
        }
        return effects.toArray(new L2Effect[effects.size()]);
    }

    /**
     * Add the FuncTemplate f to the list of functions used with the item
     *
     * @param f : FuncTemplate to add
     */
    public void attach(FuncTemplate f) {
        // If _functTemplates is empty, create it and add the FuncTemplate f in it
        if (_funcTemplates == null) {
            _funcTemplates = new FuncTemplate[]{f};
        } else {
            int len = _funcTemplates.length;
            FuncTemplate[] tmp = new FuncTemplate[len + 1];
            // Definition : arraycopy(array source, begins copy at this position of source, array destination, begins copy at this position in dest,
            //						  number of components to be copied)
            System.arraycopy(_funcTemplates, 0, tmp, 0, len);
            tmp[len] = f;
            _funcTemplates = tmp;
        }
    }

    /**
     * Add the EffectTemplate effect to the list of effects generated by the
     * item
     *
     * @param effect : EffectTemplate
     */
    public void attach(EffectTemplate effect) {
        if (_effectTemplates == null) {
            _effectTemplates = new EffectTemplate[]{effect};
        } else {
            int len = _effectTemplates.length;
            EffectTemplate[] tmp = new EffectTemplate[len + 1];
            // Definition : arraycopy(array source, begins copy at this position of source, array destination, begins copy at this position in dest,
            //						  number of components to be copied)
            System.arraycopy(_effectTemplates, 0, tmp, 0, len);
            tmp[len] = effect;
            _effectTemplates = tmp;
        }
    }

    /**
     * Add the L2Skill skill to the list of skills generated by the item
     *
     * @param skill : L2Skill
     */
    public void attach(L2Skill skill) {
        if (_skills == null) {
            _skills = new L2Skill[]{skill};
        } else {
            int len = _skills.length;
            L2Skill[] tmp = new L2Skill[len + 1];
            // Definition : arraycopy(array source, begins copy at this position of source, array destination, begins copy at this position in dest,
            //                        number of components to be copied)
            System.arraycopy(_skills, 0, tmp, 0, len);
            tmp[len] = skill;
            _skills = tmp;
        }
    }

    /**
     * Returns the name of the item
     *
     * @return String
     */
    @Override
    public String toString() {
        return _name;
    }

    //иконка
    public String getIcon() {
        return _icon;
    }

    public boolean notForOly() {
        return _oly;
    }

    public int maxOlyEnch(int ench) {
        return ench;
    }

    public int getMaxEnchant() {
        return _maxEnchant;
    }

    public boolean isHippy() {
        return _hippy;
    }

    public boolean isNotForBossZone() {
        return _boss;
    }

    public boolean isWeapon() {
        return false;
    }

    public boolean isShield() {
        return false;
    }
}
