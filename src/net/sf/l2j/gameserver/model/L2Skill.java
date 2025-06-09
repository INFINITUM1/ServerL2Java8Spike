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
package net.sf.l2j.gameserver.model;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.skills.l2skills.*;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummonNpc;
import net.sf.l2j.gameserver.skills.targets.*;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.util.log.AbstractLogger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;

/**
 * This class...
 *
 * @version $Revision: 1.3.2.8.2.22 $ $Date: 2005/04/06 16:13:42 $
 */
public abstract class L2Skill {

    protected static final Logger _log = AbstractLogger.getLogger(L2Skill.class.getName());
    public static final int SKILL_CUBIC_MASTERY = 143;
    public static final int SKILL_LUCKY = 194;
    public static final int SKILL_CREATE_COMMON = 1320;
    public static final int SKILL_CREATE_DWARVEN = 172;
    public static final int SKILL_CRYSTALLIZE = 248;
    public static final int SKILL_FAKE_INT = 9001;
    public static final int SKILL_FAKE_WIT = 9002;
    public static final int SKILL_FAKE_MEN = 9003;
    public static final int SKILL_FAKE_CON = 9004;
    public static final int SKILL_FAKE_DEX = 9005;
    public static final int SKILL_FAKE_STR = 9006;

    public static enum SkillOpType {

        OP_PASSIVE, OP_ACTIVE, OP_TOGGLE, OP_CHANCE
    }

    /**
     * Target types of skills : SELF, PARTY, CLAN, PET...
     */
    public static enum SkillTargetType {

        TARGET_NONE,
        TARGET_SELF,
        TARGET_ONE,
        TARGET_PARTY,
        TARGET_ALLY,
        TARGET_CLAN,
        TARGET_PET,
        TARGET_AREA,
        TARGET_AURA,
        TARGET_CORPSE,
        TARGET_UNDEAD,
        TARGET_AREA_UNDEAD,
        TARGET_AREA_ANGEL,
        TARGET_MULTIFACE,
        TARGET_CORPSE_ALLY,
        TARGET_CORPSE_CLAN,
        TARGET_CORPSE_PLAYER,
        TARGET_CORPSE_PET,
        TARGET_ITEM,
        TARGET_MOB,
        TARGET_AREA_CORPSE_MOB,
        TARGET_CORPSE_MOB,
        TARGET_UNLOCKABLE,
        TARGET_HOLY,
        TARGET_PARTY_MEMBER,
        TARGET_PARTY_OTHER,
        TARGET_ENEMY_SUMMON,
        TARGET_OWNER_PET,
        TARGET_SIGNET_GROUND,
        TARGET_SIGNET,
        TARGET_GROUND,
        TARGET_TYRANNOSAURUS
    }

    public static enum SkillType {
        // Damage

        PDAM,
        MDAM,
        CPDAM,
        MANADAM,
        DOT,
        MDOT,
        DRAIN_SOUL(L2SkillDrainSoul.class),
        DRAIN(L2SkillDrain.class),
        DEATHLINK,
        BLOW,
        // Disablers
        BLEED,
        POISON,
        STUN,
        ROOT,
        CONFUSION,
        FEAR,
        SLEEP,
        CONFUSE_MOB_ONLY,
        MUTE,
        PARALYZE,
        WEAKNESS,
        ERASE,
        // hp, mp, cp
        HEAL,
        HOT,
        BALANCE_LIFE,
        HEAL_PERCENT,
        HEAL_STATIC,
        COMBATPOINTHEAL,
        CPHOT,
        MANAHEAL,
        MANA_BY_LEVEL,
        MANAHEAL_PERCENT,
        MANARECHARGE,
        MPHOT,
        // Aggro
        AGGDAMAGE,
        AGGREDUCE,
        AGGREMOVE,
        AGGREDUCE_CHAR,
        AGGDEBUFF,
        // Fishing
        FISHING,
        PUMPING,
        REELING,
        // MISC
        UNLOCK,
        ENCHANT_ARMOR,
        ENCHANT_WEAPON,
        SOULSHOT,
        SPIRITSHOT,
        SIEGEFLAG,
        TAKECASTLE,
        WEAPON_SA,
        DELUXE_KEY_UNLOCK,
        SOW,
        HARVEST,
        GET_PLAYER,
        // Creation
        COMMON_CRAFT,
        DWARVEN_CRAFT,
        CREATE_ITEM(L2SkillCreateItem.class),
        SUMMON_TREASURE_KEY,
        // Summons
        SUMMON(L2SkillSummon.class),
        FEED_PET,
        DEATHLINK_PET,
        STRSIEGEASSAULT,
        BLUFF,
        BETRAY,
        // Cancel
        CANCEL,
        MAGE_BANE,
        WARRIOR_BANE,
        NEGATE,
        BUFF,
        DEBUFF,
        PASSIVE,
        CONT,
        RESURRECT,
        CHARGE(L2SkillCharge.class),
        CHARGE_EFFECT(L2SkillChargeEffect.class),
        CHARGEDAM(L2SkillChargeDmg.class),
        MHOT,
        DETECT_WEAKNESS,
        LUCK,
        RECALL,
        WEDDINGTP,
        SUMMON_FRIEND,
        CLAN_GATE,
        GATE_CHANT,
        ZAKENTPPLAYER,
        ZAKENTPSELF,
        CUSTOM_TELEPORT,
        REFLECT,
        SPOIL,
        SWEEP,
        FAKE_DEATH,
        UNBLEED,
        UNPOISON,
        UNDEAD_DEFENSE,
        SEED(L2SkillSeed.class),
        BEAST_FEED,
        FORCE_BUFF,
        SUMMON_NPC(L2SkillSummonNpc.class),
        HOLD_UNDEAD,
        TURN_UNDEAD,
        SUMMON_PET(L2SkillSummonPet.class),
        MOUNT_PET(L2SkillMountPet.class),
        UNSUMMON_PET(L2SkillUnsummonPet.class),
        // unimplemented
        NOTDONE;
        private final Class<? extends L2Skill> _class;

        public L2Skill makeSkill(StatsSet set) {
            try {
                Constructor<? extends L2Skill> c = _class.getConstructor(StatsSet.class);

                return c.newInstance(set);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private SkillType() {
            _class = L2SkillDefault.class;
        }

        private SkillType(Class<? extends L2Skill> classType) {
            _class = classType;
        }
    }
    //elements
    public final static int ELEMENT_WIND = 1;
    public final static int ELEMENT_FIRE = 2;
    public final static int ELEMENT_WATER = 3;
    public final static int ELEMENT_EARTH = 4;
    public final static int ELEMENT_HOLY = 5;
    public final static int ELEMENT_DARK = 6;
    //save vs
    public final static int SAVEVS_INT = 1;
    public final static int SAVEVS_WIT = 2;
    public final static int SAVEVS_MEN = 3;
    public final static int SAVEVS_CON = 4;
    public final static int SAVEVS_DEX = 5;
    public final static int SAVEVS_STR = 6;
    //stat effected
    public final static int STAT_PATK = 301; // pAtk
    public final static int STAT_PDEF = 302; // pDef
    public final static int STAT_MATK = 303; // mAtk
    public final static int STAT_MDEF = 304; // mDef
    public final static int STAT_MAXHP = 305; // maxHp
    public final static int STAT_MAXMP = 306; // maxMp
    public final static int STAT_CURHP = 307;
    public final static int STAT_CURMP = 308;
    public final static int STAT_HPREGEN = 309; // regHp
    public final static int STAT_MPREGEN = 310; // regMp
    public final static int STAT_CASTINGSPEED = 311; // sCast
    public final static int STAT_ATKSPD = 312; // sAtk
    public final static int STAT_CRITDAM = 313; // critDmg
    public final static int STAT_CRITRATE = 314; // critRate
    public final static int STAT_FIRERES = 315; // fireRes
    public final static int STAT_WINDRES = 316; // windRes
    public final static int STAT_WATERRES = 317; // waterRes
    public final static int STAT_EARTHRES = 318; // earthRes
    public final static int STAT_HOLYRES = 336; // holyRes
    public final static int STAT_DARKRES = 337; // darkRes
    public final static int STAT_ROOTRES = 319; // rootRes
    public final static int STAT_SLEEPRES = 320; // sleepRes
    public final static int STAT_CONFUSIONRES = 321; // confusRes
    public final static int STAT_BREATH = 322; // breath
    public final static int STAT_AGGRESSION = 323; // aggr
    public final static int STAT_BLEED = 324; // bleed
    public final static int STAT_POISON = 325; // poison
    public final static int STAT_STUN = 326; // stun
    public final static int STAT_ROOT = 327; // root
    public final static int STAT_MOVEMENT = 328; // move
    public final static int STAT_EVASION = 329; // evas
    public final static int STAT_ACCURACY = 330; // accu
    public final static int STAT_COMBAT_STRENGTH = 331;
    public final static int STAT_COMBAT_WEAKNESS = 332;
    public final static int STAT_ATTACK_RANGE = 333; // rAtk
    public final static int STAT_NOAGG = 334; // noagg
    public final static int STAT_SHIELDDEF = 335; // sDef
    public final static int STAT_MP_CONSUME_RATE = 336; // Rate of mp consume per skill use
    public final static int STAT_HP_CONSUME_RATE = 337; // Rate of hp consume per skill use
    public final static int STAT_MCRITRATE = 338; // Magic Crit Rate
    //COMBAT DAMAGE MODIFIER SKILLS...DETECT WEAKNESS AND WEAKNESS/STRENGTH
    public final static int COMBAT_MOD_ANIMAL = 200;
    public final static int COMBAT_MOD_BEAST = 201;
    public final static int COMBAT_MOD_BUG = 202;
    public final static int COMBAT_MOD_DRAGON = 203;
    public final static int COMBAT_MOD_MONSTER = 204;
    public final static int COMBAT_MOD_PLANT = 205;
    public final static int COMBAT_MOD_HOLY = 206;
    public final static int COMBAT_MOD_UNHOLY = 207;
    public final static int COMBAT_MOD_BOW = 208;
    public final static int COMBAT_MOD_BLUNT = 209;
    public final static int COMBAT_MOD_DAGGER = 210;
    public final static int COMBAT_MOD_FIST = 211;
    public final static int COMBAT_MOD_DUAL = 212;
    public final static int COMBAT_MOD_SWORD = 213;
    public final static int COMBAT_MOD_POISON = 214;
    public final static int COMBAT_MOD_BLEED = 215;
    public final static int COMBAT_MOD_FIRE = 216;
    public final static int COMBAT_MOD_WATER = 217;
    public final static int COMBAT_MOD_EARTH = 218;
    public final static int COMBAT_MOD_WIND = 219;
    public final static int COMBAT_MOD_ROOT = 220;
    public final static int COMBAT_MOD_STUN = 221;
    public final static int COMBAT_MOD_CONFUSION = 222;
    public final static int COMBAT_MOD_DARK = 223;
    //conditional values
    public final static int COND_RUNNING = 0x0001;
    public final static int COND_WALKING = 0x0002;
    public final static int COND_SIT = 0x0004;
    public final static int COND_BEHIND = 0x0008;
    public final static int COND_CRIT = 0x0010;
    public final static int COND_LOWHP = 0x0020;
    public final static int COND_ROBES = 0x0040;
    public final static int COND_CHARGES = 0x0080;
    public final static int COND_SHIELD = 0x0100;
    public final static int COND_GRADEA = 0x010000;
    public final static int COND_GRADEB = 0x020000;
    public final static int COND_GRADEC = 0x040000;
    public final static int COND_GRADED = 0x080000;
    public final static int COND_GRADES = 0x100000;
    private static final Func[] _emptyFunctionSet = new Func[0];
    private static final L2Effect[] _emptyEffectSet = new L2Effect[0];
    // these two build the primary key
    private final int _id;
    private final int _level;
    /**
     * Identifier for a skill that client can't display
     */
    private int _displayId;
    // not needed, just for easier debug
    private final String _name;
    private final SkillOpType _operateType;
    private final boolean _magic;
    private final int _mpConsume;
    private final int _mpInitialConsume;
    private final int _hpConsume;
    private final int _itemConsume;
    private final int _itemConsumeId;
    // item consume count over time
    private final int _itemConsumeOT;
    // item consume id over time
    private final int _itemConsumeIdOT;
    // how many times to consume an item
    private final int _itemConsumeSteps;
    // for summon spells:
    // a) What is the total lifetime of summons (in millisecs)
    private final int _summonTotalLifeTime;
    // b) how much lifetime is lost per second of idleness (non-fighting)
    private final int _summonTimeLostIdle;
    // c) how much time is lost per second of activity (fighting)
    private final int _summonTimeLostActive;
    private final boolean _isCubic;
    // item consume time in milliseconds
    private final int _itemConsumeTime;
    private final int _castRange;
    private final int _effectRange;
    // all times in milliseconds
    private final int _hitTime;
    //private final int _skillInterruptTime;
    private final int _coolTime;
    private final int _reuseDelay;
    private final int _buffDuration;
    /**
     * Target type of the skill : SELF, PARTY, CLAN, PET...
     */
    private final SkillTargetType _targetType;
    private final double _power;
    private final int _effectPoints;
    private final int _magicLevel;
    private final String[] _negateStats;
    private final float _negatePower;
    private final int _negateId;
    private final int _levelDepend;
    // Effecting area of the skill, in radius.
    // The radius center varies according to the _targetType:
    // "caster" if targetType = AURA/PARTY/CLAN or "target" if targetType = AREA
    private final int _skillRadius;
    private final SkillType _skillType;
    private final SkillType _effectType;
    private final int _effectPower;
    private final int _effectId;
    private final int _effectLvl;
    private final boolean _ispotion;
    private final int _element;
    private final int _savevs;
    private final int _initialEffectDelay;
    private final boolean _isSuicideAttack;
    private final Stats _stat;
    private final int _condition;
    private final int _conditionValue;
    private final boolean _overhit;
    private final int _weaponsAllowed;
    private final int _armorsAllowed;
    private final int _addCrossLearn; // -1 disable, otherwice SP price for others classes, default 1000
    private final float _mulCrossLearn; // multiplay for others classes, default 2
    private final float _mulCrossLearnRace; // multiplay for others races, default 2
    private final float _mulCrossLearnProf; // multiplay for fighter/mage missmatch, default 3
    private final List<ClassId> _canLearn; // which classes can learn
    private final List<Integer> _teachers; // which NPC teaches
    private final int _minPledgeClass;
    private final boolean _isOffensive;
    private final int _numCharges;
    private final int _forceId;
    private final boolean _isHeroSkill; // If true the skill is a Hero Skill
    private final int _baseCritRate;  // percent of success for skill critical hit (especially for PDAM & BLOW - they're not affected by rCrit values or buffs). Default loads -1 for all other skills but 0 to PDAM & BLOW
    private final int _lethalEffect1;     // percent of success for lethal 1st effect (hit cp to 1 or if mob hp to 50%) (only for PDAM skills)
    private final int _lethalEffect2;     // percent of success for lethal 2nd effect (hit cp,hp to 1 or if mob hp to 1) (only for PDAM skills)
    private final boolean _directHpDmg;  // If true then dmg is being make directly
    private final boolean _isDance;      // If true then casting more dances will cost more MP
    private final int _nextDanceCost;
    private final float _sSBoost;	//If true skill will have SoulShot boost (power*2)
    private final int _aggroPoints;
    protected Condition _preCondition;
    protected Condition _itemPreCondition;
    protected FuncTemplate[] _funcTemplates;
    protected EffectTemplate[] _effectTemplates;
    protected EffectTemplate[] _effectTemplatesSelf;
    protected ChanceCondition _chanceCondition = null;
    private final int _chanceTriggeredId;
    private final int _chanceTriggeredLevel;
    //cache
    private final boolean _fixedReuse;
    private final boolean _fixedCast;
    private final boolean _cancelProtected;
    private boolean _isHeroDebuff = false;
    private boolean _isForbiddenProfileSkill = false;
    private boolean _isProtected = false;
    private boolean _isNotAura = true;
    private boolean _isBattleForceSkill = false;
    private boolean _isSpellForceSkill = false;
    private boolean _isAugment = false;
    private boolean _isAOEpvp = false;
    private boolean _isBuff = false;
    private boolean _isSSBuff = false;
    private boolean _isMalariaBuff = false;
    private boolean _isNoShot = false;
    private boolean _isSkillTypeOffensive = false;
    private boolean _isPvpSkill = false;
    private boolean _useSoulShot = false;
    private boolean _useFishShot = false;
    private final boolean _isActive;
    private final boolean _isPassive;
    private final boolean _isToggle;
    private final boolean _isChance;
    private final boolean _isDeathLink;
    private boolean _isDebuff = false;
    private boolean _isMagicalSlow = false;
    private boolean _isPhysicalSlow = false;
    private boolean _isMiscDebuff = false;
    private boolean _isMiscDebuffPhys = false;
    private boolean _isNobleSkill = false;
    private boolean _isHerosSkill = false;
    private boolean _isClanSkill = false;
    private boolean _isSiegeSkill = false;
    private boolean _isFishingSkill = false;
    private boolean _isDwarvenSkill = false;
    private boolean _isMiscSkill = false;
    private boolean _canTargetSelf = false;
    private boolean _continueAttack = false;
    private boolean _unlockSkill = false;
    private boolean _useAltFormula = false;
    private boolean _isSupportBuff = false;
    private boolean _isSupportTarget = false;
    private boolean _isSignedTarget = false;
    private boolean _isAuraSignedTarget = false;
    private boolean _isUseSppritShot = false;
    private boolean _isNotForCursed = false;
    private boolean _sendSkillUseInfo = true;
    private boolean _isAoeOffensive = false;
    private boolean _isPetTarget = false;
    private boolean _isFreeCastOnMob = false;
    private boolean _isSkillTypeSummon = false;
    private boolean _isChargeSkill = false;
    private boolean _isFishingSkillType = false;
    private boolean _isSiegeFlagSkill = false;
    private boolean _isSiegeSummonSkill = false;
    private boolean _isCheckPvpSkill = true;
    private boolean _canBeReflected = true;
    private boolean _checkFirstBuff = false;
    private boolean _checkPosSkip = false;
    private boolean _checkRaidSkip = false;
    private boolean _isCommonOrDwaven = false;
    private boolean _isCancelType = false;
    private boolean _canCastSelf = false;
    private boolean _isAuraTarget = false;
    private boolean _isForbOly = false;
    private boolean _isForbEvent = false;
    private boolean _isLikeDebuff = false;
    private double _baseLandRate;
    private SkillType _baseSkillType;
    private TargetList _targetList = null;

    protected L2Skill(StatsSet set) {
        _id = set.getInteger("skill_id");
        _level = set.getInteger("level");

        _displayId = set.getInteger("displayId", _id);
        _name = set.getString("name");
        _operateType = set.getEnum("operateType", SkillOpType.class);
        _magic = set.getBool("isMagic", false);
        _ispotion = set.getBool("isPotion", false);
        _mpConsume = set.getInteger("mpConsume", 0);
        _mpInitialConsume = set.getInteger("mpInitialConsume", 0);
        _hpConsume = set.getInteger("hpConsume", 0);
        _itemConsume = set.getInteger("itemConsumeCount", 0);
        _itemConsumeId = set.getInteger("itemConsumeId", 0);
        _itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
        _itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
        _itemConsumeTime = set.getInteger("itemConsumeTime", 0);
        _itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
        _summonTotalLifeTime = set.getInteger("summonTotalLifeTime", 1200000);  // 20 minutes default
        _summonTimeLostIdle = set.getInteger("summonTimeLostIdle", 0);
        _summonTimeLostActive = set.getInteger("summonTimeLostActive", 0);

        _isCubic = set.getBool("isCubic", false);

        _castRange = set.getInteger("castRange", 0);
        _effectRange = set.getInteger("effectRange", -1);

        _hitTime = set.getInteger("hitTime", 0);
        _coolTime = set.getInteger("coolTime", 0);
        _initialEffectDelay = set.getInteger("initialEffectDelay", 0);
        //_skillInterruptTime = set.getInteger("hitTime", _hitTime / 2);
        _reuseDelay = set.getInteger("reuseDelay", 0);
        _buffDuration = set.getInteger("buffDuration", 0);

        _skillRadius = set.getInteger("skillRadius", 80);

        _targetType = set.getEnum("target", SkillTargetType.class);
        _power = set.getFloat("power", 0.f);
        _effectPoints = set.getInteger("effectPoints", 0);
        _negateStats = set.getString("negateStats", "").split(" ");
        _negatePower = set.getFloat("negatePower", 0.f);
        _negateId = set.getInteger("negateId", 0);
        _magicLevel = set.getInteger("magicLvl", SkillTreeTable.getInstance().getMinSkillLevel(_id, _level));
        _levelDepend = set.getInteger("lvlDepend", 0);
        _stat = set.getEnum("stat", Stats.class, null);

        _skillType = set.getEnum("skillType", SkillType.class);
        _effectType = set.getEnum("effectType", SkillType.class, null);
        _effectPower = set.getInteger("effectPower", 0);
        _effectId = set.getInteger("effectId", 0);
        _effectLvl = set.getInteger("effectLevel", 0);

        _element = set.getInteger("element", 0);
        _savevs = set.getInteger("save", 0);

        _condition = set.getInteger("condition", 0);
        _conditionValue = set.getInteger("conditionValue", 0);
        _overhit = set.getBool("overHit", false);
        _isSuicideAttack = set.getBool("isSuicideAttack", false);
        _weaponsAllowed = set.getInteger("weaponsAllowed", 0);
        _armorsAllowed = set.getInteger("armorsAllowed", 0);

        _addCrossLearn = set.getInteger("addCrossLearn", 1000);
        _mulCrossLearn = set.getFloat("mulCrossLearn", 2.f);
        _mulCrossLearnRace = set.getFloat("mulCrossLearnRace", 2.f);
        _mulCrossLearnProf = set.getFloat("mulCrossLearnProf", 3.f);
        _minPledgeClass = set.getInteger("minPledgeClass", 0);
        _numCharges = set.getInteger("num_charges", getLevel());
        _forceId = set.getInteger("forceId", 0);

        if (_operateType == SkillOpType.OP_CHANCE) {
            _chanceCondition = ChanceCondition.parse(set);
        }

        _chanceTriggeredId = set.getInteger("chanceTriggeredId", 0);
        _chanceTriggeredLevel = set.getInteger("chanceTriggeredLevel", 0);

        _isHeroSkill = HeroSkillTable.isHeroSkill(_id);

        _baseCritRate = set.getInteger("baseCritRate", (_skillType == SkillType.PDAM || _skillType == SkillType.BLOW) ? 0 : -1);
        _lethalEffect1 = set.getInteger("lethal1", 0);
        _lethalEffect2 = set.getInteger("lethal2", 0);

        _directHpDmg = set.getBool("dmgDirectlyToHp", false);
        _isDance = set.getBool("isDance", false);
        _nextDanceCost = set.getInteger("nextDanceCost", 0);
        _sSBoost = set.getFloat("SSBoost", 0.f);
        _aggroPoints = set.getInteger("aggroPoints", 0);

        String canLearn = set.getString("canLearn", null);
        if (canLearn == null) {
            _canLearn = null;
        } else {
            _canLearn = new FastList<ClassId>();
            StringTokenizer st = new StringTokenizer(canLearn, " \r\n\t,;");
            while (st.hasMoreTokens()) {
                String cls = st.nextToken();
                try {
                    _canLearn.add(ClassId.valueOf(cls));
                } catch (Throwable t) {
                    _log.log(Level.SEVERE, "Bad class " + cls + " to learn skill", t);
                }
            }
        }

        String teachers = set.getString("teachers", null);
        if (teachers == null) {
            _teachers = null;
        } else {
            _teachers = new FastList<Integer>();
            StringTokenizer st = new StringTokenizer(teachers, " \r\n\t,;");
            while (st.hasMoreTokens()) {
                String npcid = st.nextToken();
                try {
                    _teachers.add(Integer.parseInt(npcid));
                } catch (Throwable t) {
                    _log.log(Level.SEVERE, "Bad teacher id " + npcid + " to teach skill", t);
                }
            }
        }

        //cache
        _fixedReuse = Config.ALT_FIXED_REUSES.contains(_id);
        _fixedCast = Config.ALT_FIXED_CASTS.contains(_id);
        _cancelProtected = Config.PROTECTED_BUFFS.contains(_id);

        // _isDeathLink
        _isDeathLink = (_skillType == SkillType.DEATHLINK);

        // type
        _isActive = (_operateType == SkillOpType.OP_ACTIVE);
        _isPassive = (_operateType == SkillOpType.OP_PASSIVE);
        _isToggle = (_operateType == SkillOpType.OP_TOGGLE);
        _isChance = (_operateType == SkillOpType.OP_CHANCE);

        //_useFishShot
        switch (_skillType) {
            case PUMPING:
            case REELING:
                _useFishShot = true;
                break;
        }

        //_useSoulShot
        switch (_skillType) {
            case PDAM:
            case STUN:
            case CHARGEDAM:
            case BLOW:
                _useSoulShot = true;
                break;
        }

        //isPvpSkill
        switch (_skillType) {
            case DOT:
            case BLEED:
            case CONFUSION:
            case POISON:
            case DEBUFF:
            case AGGDEBUFF:
            case STUN:
            case ROOT:
            case FEAR:
            case SLEEP:
            case MANADAM:
            case MDOT:
            case MUTE:
            case WEAKNESS:
            case PARALYZE:
            case ERASE:
            case CANCEL:
            case MAGE_BANE:
            case WARRIOR_BANE:
            case BETRAY:
            case BLUFF:
            case AGGDAMAGE:
            case AGGREDUCE_CHAR:
                _isPvpSkill = true;
                break;
        }

        //_isSkillTypeOffensive
        switch (_skillType) {
            case PDAM:
            case MDAM:
            case CPDAM:
            case DOT:
            case BLEED:
            case POISON:
            case AGGDAMAGE:
            case DEBUFF:
            case AGGDEBUFF:
            case STUN:
            case ROOT:
            case CONFUSION:
            case BLOW:
            case FEAR:
            case DRAIN:
            case SLEEP:
            case ERASE:
            case CHARGEDAM:
            case CONFUSE_MOB_ONLY:
            case DEATHLINK:
            case DETECT_WEAKNESS:
            case MANADAM:
            case MDOT:
            case MUTE:
            case SOULSHOT:
            case SPIRITSHOT:
            case SPOIL:
            case WEAKNESS:
            case MANA_BY_LEVEL:
            case SWEEP:
            case PARALYZE:
            case DRAIN_SOUL:
            case AGGREDUCE:
            case AGGREMOVE:
            case AGGREDUCE_CHAR:
            case BETRAY:
            case DELUXE_KEY_UNLOCK:
            case SOW:
            case HARVEST:
            case MAGE_BANE:
            case WARRIOR_BANE:
            case CANCEL:
            case TURN_UNDEAD:
                _isSkillTypeOffensive = true;
                break;
        }

        //хиро-дебаффы
        switch (_id) {
            case 1375:
            case 1376:
                _isHeroDebuff = true;
                break;
        }

        //для профилей баффера
        if (Config.F_PROFILE_BUFFS.contains(_id)
                || (_id >= 5123 && _id <= 5129)) // эпик бафф
        {
            _isForbiddenProfileSkill = true;
        }

        //для миража
        switch (_skillType) {
            case PDAM:
            case MDAM:
            case CPDAM:
            case DOT:
                _isProtected = true;
                break;
        }

        //аурки
        switch (_id) {
            case 1231:
            case 1275:
            case 1417:
                _isNotAura = false;
                break;
        }

        //эпические скиллы
        switch (_id) {
            case 454:
            case 455:
            case 456:
            case 457:
            case 458:
            case 459:
            case 460:
                _isBattleForceSkill = true;
                break;
        }

        switch (_id) {
            case 1419:
            case 1420:
            case 1421:
            case 1422:
            case 1423:
            case 1424:
            case 1425:
            case 1426:
            case 1427:
            case 1428:
                _isSpellForceSkill = true;
                break;
        }

        //аугмент
        if (_id >= 3100 && _id <= 3299) {
            _isAugment = true;
        }

        if (Config.CUSTOM_AUG_SKILLS.contains(_id)) {
            _isAugment = true;
        }

        //ss buff
        if (_id > 4360 && _id < 4367) {
            _isSSBuff = true;
        }

        //malaria
        if (_id >= 4552 && _id <= 4554) {
            _isMalariaBuff = true;
        }

        //no ss
        if ((_skillType == SkillType.FAKE_DEATH) || (_id == 11 || _id == 12)) {
            _isNoShot = true;
        }

        //aoe
        switch (_id) {
            case 1417:
            case 48:
            case 36:
            case 452:
            case 320:
            case 361:
                _isAOEpvp = true;
                break;
        }

        // buff
        switch (_skillType) {
            case BUFF:
            case DEBUFF:
            case REFLECT:
            case HEAL_PERCENT:
            case MANAHEAL_PERCENT:
                _isBuff = true;
                break;
        }

        // debuff
        switch (_skillType) {
            case AGGDEBUFF:
            case CANCEL:
            case CONFUSION:
            case DEBUFF:
            case FEAR:
            case MAGE_BANE:
            case MUTE:
            case NEGATE:
            case PARALYZE:
            case PDAM:
            case MDAM:
            case ROOT:
            case SLEEP:
            case STUN:
            case WARRIOR_BANE:
                _isDebuff = true;
                break;
        }

        //_canTargetSelf 
        switch (_skillType) {
            case BALANCE_LIFE:
            case BETRAY:
            case BUFF:
            case COMBATPOINTHEAL:
            case FORCE_BUFF:
            case HEAL:
            case HEAL_PERCENT:
            case HOT:
            case MAGE_BANE:
            case MANAHEAL:
            case MANARECHARGE:
            case NEGATE:
            case REFLECT:
            case SEED:
            case UNBLEED:
            case UNPOISON:
            case WARRIOR_BANE:
                _canTargetSelf = true;
                break;
        }

        //_continueAttack 
        switch (_skillType) {
            case BLOW:
            case PDAM:
            //case DRAIN_SOUL:
            case SOW:
            case SPOIL:
            case CHARGEDAM:
                _continueAttack = true;
                break;
        }

        //_unlockSkill 
        switch (_skillType) {
            case UNLOCK:
            case DELUXE_KEY_UNLOCK:
                _unlockSkill = true;
                break;
        }

        //_isMagicalSlow
        switch (_id) {
            case 102:
            case 105:
            case 127:
            case 1099:
            case 1160:
            case 1184:
            case 1236:
            case 1298:
                _isMagicalSlow = true;
                break;
        }

        //_isPhysicalSlow
        switch (_id) {
            case 95:
            case 354:
                _isPhysicalSlow = true;
                break;
        }

        //_isMiscDebuff
        switch (_id) {
            case 65: // fear ?
            case 115:
            case 122:
            case 279:
            case 405: // fear ?
            case 450: // fear ?
            case 1042:
            case 1049:
            case 1056: // cancel
            case 1064:
            case 1092: // fear ?
            case 1096:
            case 1104:
            case 1164:
            case 1169: // fear ?
            case 1170:
            case 1206:
            case 1222:
            case 1246:
            case 1247:
            case 1248:
            case 1269:
            case 1263: // Curse Gloom
            case 1272: // fear ?
            case 1336:
            case 1337:
            case 1338:
            case 1339: // fire vortex
            case 1340: // ice vortex
            case 1342: // Light Vortex
            case 1343: //dark vortex
            case 1358:
            case 1359:
            case 1360:
            case 1361:
            case 1366:
            case 1367:
            case 1376: // fear ?	
            case 1381: // fear ?
            case 1382:
            case 1386:
            case 1396:
            case 3194: // fear ?
            case 4108: // fear ?
            case 4689: // fear ?
            case 5092: // fear ?
            case 5220: // fear ?
                _isMiscDebuff = true;
                break;
        }
        //_isMiscDebuffPhys
        switch (_id) {
            case 97:
            case 106:
            case 116:
            case 342:
            case 353:
            case 367:
            case 400:
            case 407:
            case 408:
            case 412:
                _isMiscDebuffPhys = true;
                break;
        }

        //_isNobleSkill
        if ((_id >= 325 && _id <= 327) || (_id >= 1323 && _id <= 1327)) {
            _isNobleSkill = true;
        }

        //_isHerosSkill
        if ((_id >= 1374 && _id <= 1376) || (_id >= 395 && _id <= 396)) {
            _isHerosSkill = true;
        }

        //isClanSkill
        if (_id >= 370 && _id <= 391) {
            _isClanSkill = true;
        }

        //_isSiegeSkill
        if (_id >= 246 && _id <= 247) {
            _isSiegeSkill = true;
        }

        //_isFishingSkill
        if (_id >= 1312 && _id <= 1322) {
            _isFishingSkill = true;
        }

        //_isDwarvenSkill
        if (_id >= 1368 && _id <= 1373) {
            _isDwarvenSkill = true;
        }

        //_isMiscSkill
        if (_id >= 3000 && _id < 10000) {
            _isMiscSkill = true;
        }
        if (Config.WHITE_SKILLS.contains(_id)) {
            _isMiscSkill = true;
        }

        //_isOffensive
        _isOffensive = set.getBool("offensive", isSkillTypeOffensive());

        //_targetList
        _targetList = TargetList.create(_targetType);

        //_baseLandRate, _baseSkillType
        _baseLandRate = _power;
        _baseSkillType = _skillType;
        switch (_skillType) {
            case MDAM:
            case PDAM:
                _baseSkillType = getEffectType();
                if (_baseSkillType != null) {
                    _baseLandRate = getEffectPower();
                } else {
                    switch (_id) {
                        case 279:
                            _baseLandRate = 20;
                            break;
                        case 352:
                            _baseLandRate = 80;
                            break;
                        case 400:
                            _baseLandRate = 60;
                            break;
                        default:
                            _baseLandRate = 60;
                            break;
                    }
                    _baseSkillType = SkillType.ROOT;
                    if (_baseSkillType == SkillType.PDAM) {
                        _baseSkillType = SkillType.STUN;
                    }
                }
                break;
        }

        if (_isMiscDebuff) {
            _baseSkillType = SkillType.ROOT;
        } else if (_isMiscDebuffPhys) {
            if (_id == 279) {
                _baseLandRate = 40;
            }

            _baseSkillType = SkillType.DEBUFF;
        }

        //_useAltFormula
        switch (_id) {
            case 115:
            case 122:
            case 403: // Root
            case 1056:
            case 1071: // Surrender To Water
            case 1074: // Surrender To Wind
            case 1083: // Surrender To Fire
            case 1223: // Surrender To Earth
            case 1224: // Surrender To Poison
            case 1263: // Curse Gloom
            case 1339: // Fire Vortex
            case 1340: // Ice Vortex
            case 1341: // Wind Vortex
            case 1342: // Light Vortex
            case 1383: // Mass Surrender to Fire
            case 1384: // Mass Surrender to Water
            case 1385: // Mass Surrender to Wind
            case 1358: // Block Shield
            case 1359: // Block Wind Walk
            case 1360: // Mass Block Shield
            case 1361: // Mass Block Wind Walk
                _useAltFormula = true;
                break;
        }

        //_useAltFormula
        switch (_id) {
            case 48: // Thunder Storm
                _baseLandRate += 20;
                break;
            case 84: // Poison Blade Dance
                _baseLandRate += 20;
                break;
            case 367: // Dance of Medusa
                _baseLandRate += 20;
                break;
            case 452: // Shock Stomp
                _baseLandRate += 20;
                break;
            case 403: // Shackle
                _baseLandRate += 20;
                break;
            case 1263: // Curse Gloom
                _baseLandRate += 20;
                break;
            case 1339: // Fire Vortex
                _baseLandRate -= 20;
                break;
            case 1340: // Ice Vortex
                _baseLandRate -= 20;
                break;
            case 1341: // Wind Vortex
                _baseLandRate -= 20;
                break;
            case 1342: // Light Vortex
                _baseLandRate -= 20;
                break;
            case 1343: // Dark Vortex
                _baseLandRate += 40;
                break;
        }

        //_isSupportBuff
        switch (_skillType) {
            case BALANCE_LIFE:
            case BUFF:
            case HOT:
            case HEAL:
            case HEAL_PERCENT:
            case COMBATPOINTHEAL:
            case MANAHEAL:
            case MANAHEAL_PERCENT:
            case REFLECT:
            case SEED:
                _isSupportBuff = true;
                break;
        }

        //_isUseSppritShot
        switch (_skillType) {
            case BUFF:
            case MANAHEAL:
            case RESURRECT:
            case RECALL:
            case DOT:
                _isUseSppritShot = true;
                break;
        }

        //_isSupportTarget
        switch (_targetType) {
            case TARGET_SELF:
            case TARGET_PET:
            case TARGET_PARTY:
            case TARGET_CLAN:
            case TARGET_ALLY:
                _isSupportTarget = true;
                break;
        }

        //_isSignedTarget
        switch (_targetType) {
            case TARGET_SIGNET_GROUND:
            case TARGET_SIGNET:
                _isSignedTarget = true;
                break;
        }

        //_isAuraSignedTarget
        switch (_targetType) {
            case TARGET_SELF:
            case TARGET_AURA:
            case TARGET_PARTY:
            case TARGET_CLAN:
            case TARGET_ALLY:
            case TARGET_CORPSE_ALLY:
            case TARGET_SIGNET_GROUND:
            case TARGET_SIGNET:
                _isAuraSignedTarget = true;
                break;
        }

        if (Config.FORB_CURSED_SKILLS.contains(_id)) {
            _isNotForCursed = true;
        }

        //_sendSkillUseInfo
        switch (_id) {
            case 1312:
            case 2046:
            case 5099:
                _sendSkillUseInfo = false;
                break;
        }

        //_isAoeOffensive
        if (_isSkillTypeOffensive && _targetType == SkillTargetType.TARGET_AURA) {
            _isAoeOffensive = true;
        }
        if (_targetType == SkillTargetType.TARGET_CORPSE_ALLY) {
            _isAoeOffensive = true;
        }

        //_isPetTarget
        if (_targetType == SkillTargetType.TARGET_PET) {
            _isPetTarget = true;
        }

        //_isSkillTypeSummon
        if (_skillType == SkillType.SUMMON) {
            _isSkillTypeSummon = true;
        }

        //_isChargeSkill
        if (_skillType == SkillType.CHARGE) {
            _isChargeSkill = true;
        }

        //_isSiegeFlagSkill
        if (_skillType == SkillType.SIEGEFLAG) {
            _isSiegeFlagSkill = true;
        }

        //_isSiegeSummonSkill
        switch (_id) {
            case 13:
            case 299:
            case 448:
                _isSiegeSummonSkill = true;
        }

        //_isFreeCastOnMob
        switch (_targetType) {
            case TARGET_PET:
            case TARGET_AURA:
            case TARGET_CLAN:
            case TARGET_SELF:
            case TARGET_PARTY:
            case TARGET_ALLY:
            case TARGET_SIGNET_GROUND:
            case TARGET_SIGNET:
                break;
            case TARGET_CORPSE_MOB:
            case TARGET_AREA_CORPSE_MOB:
                _isFreeCastOnMob = true;
                break;
            default:
                switch (_skillType) {
                    case BEAST_FEED:
                    case DELUXE_KEY_UNLOCK:
                    case UNLOCK:
                        _isFreeCastOnMob = true;
                        break;
                }
        }

        //_isFishingSkillType
        switch (_skillType) {
            case PUMPING:
            case REELING:
            case FISHING:
                _isFishingSkillType = true;
                break;
        }

        //_canBeReflected
        switch (_skillType) {
            case BUFF:
            case HOT:
            case CPHOT:
            case MPHOT:
            case UNDEAD_DEFENSE:
            case AGGDEBUFF:
            case CONT:
                _canBeReflected = false;
                break;
        }

        //_isCheckPvpSkill
        switch (_targetType) {
            case TARGET_PARTY:
            case TARGET_ALLY:
            case TARGET_CLAN:
            case TARGET_SIGNET_GROUND:
            case TARGET_SIGNET:
            case TARGET_SELF:
                _isCheckPvpSkill = false;
                break;
        }

        //_checkFirstBuff
        switch (_skillType) {
            case BUFF:
            case REFLECT:
                if (!_isMalariaBuff && !_isSSBuff) {
                    _checkFirstBuff = true;
                }
                break;
        }

        //_checkPosSkip
        if (!isToggle() && !isMalariaBuff() && !isSSBuff()) {
            _checkPosSkip = true;
        }

        //_checkRaidSkip
        switch (_skillType) {
            case CONFUSION:
            case MUTE:
            case PARALYZE:
            case ROOT:
                _checkRaidSkip = true;
                break;
        }

        //_isCommonOrDwaven
        if (_id >= 1320 && _id <= 1322) {
            _isCommonOrDwaven = true;
        }

        //_isCancelType
        switch (_id) {
            case 1056:
            case 4094:
            case 4177:
            case 5118:
            case 4618:
                _isCancelType = true;
                break;
        }

        //_canCastSelf
        switch (_targetType) {
            case TARGET_AURA:    // AURA, SELF should be cast even if no target has been found
            case TARGET_SELF:
            case TARGET_SIGNET:
            case TARGET_CORPSE_ALLY:
            case TARGET_SIGNET_GROUND:
                _canCastSelf = true;
                break;
        }

        //_isAuraTarget
        switch (_targetType) {
            case TARGET_AREA:
            case TARGET_AURA:
                _isAuraTarget = true;
                break;
        }

        //_isForbOly
        switch (_id) {
            case 1321:
            case 1322:
                _isForbOly = true;
                break;
            default:
                if (_isFishingSkillType) {
                    _isForbOly = true;
                } else {
                    _isForbOly = Config.FORB_OLY_SKILLS.contains(_id);
                }
                break;
        }
        _isForbEvent = Config.FORB_EVENT_SKILLS.contains(_id);

        //_isLikeDebuff
        _isLikeDebuff = (_isDebuff || _isSSBuff || _isMalariaBuff);
        if (_isLikeDebuff) {
            _checkPosSkip = false;
        }
    }

    public boolean isLikeDebuff() {
        return _isLikeDebuff;
    }

    public boolean isAuraSkill() {
        return _isAuraTarget;
    }

    public boolean canCastSelf() {
        return _canCastSelf;
    }

    public boolean isCancelType() {
        return _isCancelType;
    }

    public boolean isAoeOffensive() {
        return _isAoeOffensive;
    }

    public boolean isCommonOrDwaven() {
        return _isCommonOrDwaven;
    }

    public boolean checkPosSkip() {
        return _checkPosSkip;
    }

    public boolean checkRaidSkip() {
        return _checkRaidSkip;
    }

    public boolean checkFirstBuff() {
        return _checkFirstBuff;
    }

    public boolean canBeReflected() {
        return _canBeReflected;
    }

    public boolean isFishingSkillType() {
        return _isFishingSkillType;
    }

    public boolean isCheckPvpSkill() {
        return _isCheckPvpSkill;
    }

    public boolean isSiegeSummonSkill() {
        return _isSiegeSummonSkill;
    }

    public boolean isSiegeFlagSkill() {
        return _isSiegeFlagSkill;
    }

    public boolean isChargeSkill() {
        return _isChargeSkill;
    }

    public boolean isSkillTypeSummon() {
        return _isSkillTypeSummon;
    }

    public boolean isFreeCastOnMob() {
        return _isFreeCastOnMob;
    }

    public boolean isSupportSkill() {
        return _isSupportBuff;
    }

    public boolean isSupportTargetType() {
        return _isSupportTarget;
    }

    public boolean isPetTargetType() {
        return _isPetTarget;
    }

    public boolean isSignedTargetType() {
        return _isSignedTarget;
    }

    public boolean isAuraSignedTargetType() {
        return _isAuraSignedTarget;
    }

    public boolean isUseSppritShot() {
        return _isUseSppritShot;
    }

    public abstract void useSkill(L2Character caster, FastList<L2Object> targets);

    public final boolean isPotion() {
        return _ispotion;
    }

    public final int getArmorsAllowed() {
        return _armorsAllowed;
    }

    public final int getConditionValue() {
        return _conditionValue;
    }

    public final SkillType getSkillType() {
        return _skillType;
    }

    public final int getSavevs() {
        return _savevs;
    }

    public final int getElement() {
        return _element;
    }

    /**
     * Return the target type of the skill : SELF, PARTY, CLAN, PET...<BR><BR>
     *
     */
    public final SkillTargetType getTargetType() {
        return _targetType;
    }

    public final int getCondition() {
        return _condition;
    }

    public final boolean isOverhit() {
        return _overhit;
    }

    public final boolean isSuicideAttack() {
        return _isSuicideAttack;
    }

    /**
     * Return the power of the skill.<BR><BR>
     */
    public final double getPower(L2Character activeChar) {
        if (_isDeathLink && activeChar != null) {
            return _power * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 2) * 0.577;
        }

        return _power;
    }

    public final double getPower() {
        return _power;
    }

    public final int getEffectPoints() {
        return _effectPoints;
    }

    public final String[] getNegateStats() {
        return _negateStats;
    }

    public final float getNegatePower() {
        return _negatePower;
    }

    public final int getNegateId() {
        return _negateId;
    }

    public final int getMagicLevel() {
        return _magicLevel;
    }

    public final int getLevelDepend() {
        return _levelDepend;
    }

    /**
     * Return the additional effect power or base probability.<BR><BR>
     */
    public final int getEffectPower() {
        return _effectPower;
    }

    public final int getEffectId() {
        return _effectId;
    }

    /**
     * Return the additional effect level.<BR><BR>
     */
    public final int getEffectLvl() {
        return _effectLvl;
    }

    /**
     * Return the additional effect skill type (ex : STUN,
     * PARALYZE,...).<BR><BR>
     */
    public final SkillType getEffectType() {
        return _effectType;
    }

    /**
     * @return Returns the buffDuration.
     */
    public final int getBuffDuration() {
        return _buffDuration;
    }

    /**
     * @return Returns the castRange.
     */
    public final int getCastRange() {
        return _castRange;
    }

    /**
     * @return Returns the effectRange.
     */
    public final int getEffectRange() {
        return _effectRange;
    }

    /**
     * @return Returns the hpConsume.
     */
    public final int getHpConsume() {
        return _hpConsume;
    }

    /**
     * @return Returns the id.
     */
    public final int getId() {
        return _id;
    }

    public int getDisplayId() {
        return _displayId;
    }

    public void setDisplayId(int id) {
        _displayId = id;
    }

    public int getForceId() {
        return _forceId;
    }

    /**
     * Return the additional Skill end time. <BR><BR>
     */
    public final int getInitialEffectDelay() {
        return _initialEffectDelay;
    }

    /**
     * Return the skill type (ex : BLEED, SLEEP, WATER...).<BR><BR>
     */
    public final Stats getStat() {
        return _stat;
    }

    /**
     * @return Returns the itemConsume.
     */
    public final int getItemConsume() {
        return _itemConsume;
    }

    /**
     * @return Returns the itemConsumeId.
     */
    public final int getItemConsumeId() {
        return _itemConsumeId;
    }

    /**
     * @return Returns the itemConsume count over time.
     */
    public final int getItemConsumeOT() {
        return _itemConsumeOT;
    }

    /**
     * @return Returns the itemConsumeId over time.
     */
    public final int getItemConsumeIdOT() {
        return _itemConsumeIdOT;
    }

    /**
     * @return Returns the itemConsume count over time.
     */
    public final int getItemConsumeSteps() {
        return _itemConsumeSteps;
    }

    /**
     * @return Returns the itemConsume count over time.
     */
    public final int getTotalLifeTime() {
        return _summonTotalLifeTime;
    }

    /**
     * @return Returns the itemConsume count over time.
     */
    public final int getTimeLostIdle() {
        return _summonTimeLostIdle;
    }

    /**
     * @return Returns the itemConsumeId over time.
     */
    public final int getTimeLostActive() {
        return _summonTimeLostActive;
    }

    public final boolean isCubic() {
        return _isCubic;
    }

    /**
     * @return Returns the itemConsume time in milliseconds.
     */
    public final int getItemConsumeTime() {
        return _itemConsumeTime;
    }

    /**
     * @return Returns the level.
     */
    public final int getLevel() {
        return _level;
    }

    /**
     * @return Returns the magic.
     */
    public final boolean isMagic() {
        return _magic;
    }

    /**
     * @return Returns the mpConsume.
     */
    public final int getMpConsume() {
        return _mpConsume;
    }

    /**
     * @return Returns the mpInitialConsume.
     */
    public final int getMpInitialConsume() {
        return _mpInitialConsume;
    }

    /**
     * @return Returns the name.
     */
    public final String getName() {
        return _name;
    }

    /**
     * @return Returns the reuseDelay.
     */
    public final int getReuseDelay() {
        return _reuseDelay;
    }

    @Deprecated
    public final int getSkillTime() {
        return _hitTime;
    }

    public final int getHitTime() {
        return _hitTime;
    }

    /**
     * @return Returns the coolTime.
     */
    public final int getCoolTime() {
        return _coolTime;
    }

    public final int getSkillRadius() {
        return _skillRadius;
    }

    public final boolean isActive() {
        return _isActive;
    }

    public final boolean isPassive() {
        return _isPassive;
    }

    public final boolean isToggle() {
        return _isToggle;
    }

    public final boolean isChance() {
        return _isChance;
    }

    public ChanceCondition getChanceCondition() {
        return _chanceCondition;
    }

    public final int getChanceTriggeredId() {
        return _chanceTriggeredId;
    }

    public final int getChanceTriggeredLevel() {
        return _chanceTriggeredLevel;
    }

    public final boolean isDance() {
        return _isDance;
    }

    public final int getNextDanceMpCost() {
        return _nextDanceCost;
    }

    public final float getSSBoost() {
        return _sSBoost;
    }

    public final int getAggroPoints() {
        return _aggroPoints;
    }

    public final boolean useSoulShot() {
        return _useSoulShot;
    }

    public final boolean useSpiritShot() {
        return isMagic();
    }

    public final boolean useFishShot() {
        return _useFishShot;
    }

    public final int getWeaponsAllowed() {
        return _weaponsAllowed;
    }

    public final int getCrossLearnAdd() {
        return _addCrossLearn;
    }

    public final float getCrossLearnMul() {
        return _mulCrossLearn;
    }

    public final float getCrossLearnRace() {
        return _mulCrossLearnRace;
    }

    public final float getCrossLearnProf() {
        return _mulCrossLearnProf;
    }

    public final boolean getCanLearn(ClassId cls) {
        return _canLearn == null || _canLearn.contains(cls);
    }

    public final boolean canTeachBy(int npcId) {
        return _teachers == null || _teachers.contains(npcId);
    }

    public int getMinPledgeClass() {
        return _minPledgeClass;
    }

    public final boolean isPvpSkill() {
        return _isPvpSkill;
    }

    public final boolean isOffensive() {
        return _isOffensive;
    }

    public final boolean isHeroSkill() {
        return _isHeroSkill;
    }

    public final int getNumCharges() {
        return _numCharges;
    }

    public final int getBaseCritRate() {
        return _baseCritRate;
    }

    public final int getLethalChance1() {
        return _lethalEffect1;
    }

    public final int getLethalChance2() {
        return _lethalEffect2;
    }

    public final boolean getDmgDirectlyToHP() {
        return _directHpDmg;
    }

    public final boolean isSkillTypeOffensive() {
        return _isSkillTypeOffensive;
    }

    //	int weapons[] = {L2Weapon.WEAPON_TYPE_ETC, L2Weapon.WEAPON_TYPE_BOW,
    //	L2Weapon.WEAPON_TYPE_POLE, L2Weapon.WEAPON_TYPE_DUALFIST,
    //	L2Weapon.WEAPON_TYPE_DUAL, L2Weapon.WEAPON_TYPE_BLUNT,
    //	L2Weapon.WEAPON_TYPE_SWORD, L2Weapon.WEAPON_TYPE_DAGGER};
    public final boolean getWeaponDependancy(L2Character activeChar) {
        if (getWeaponDependancy(activeChar, false)) {
            return true;
        }

        activeChar.sendUserPacket(SystemMessage.id(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(this));
        return false;
    }

    public final boolean getWeaponDependancy(L2Character activeChar, boolean chance) {
        int weaponsAllowed = getWeaponsAllowed();
        //check to see if skill has a weapon dependency.
        if (weaponsAllowed == 0) {
            return true;
        }
        if (activeChar.getActiveWeaponItem() != null) {
            L2WeaponType playerWeapon = activeChar.getActiveWeaponItem().getItemType();
            int mask = playerWeapon.mask();
            if ((mask & weaponsAllowed) != 0) {
                return true;
            }
            // can be on the secondary weapon
            if (activeChar.getSecondaryWeaponItem() != null) {
                playerWeapon = activeChar.getSecondaryWeaponItem().getItemType();
                mask = playerWeapon.mask();
                if ((mask & weaponsAllowed) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon) {
        /*
         * if ((getCondition() & L2Skill.COND_SHIELD) != 0) {
         *
         * L2Armor armorPiece; L2ItemInstance dummy; dummy =
         * activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
         * armorPiece = (L2Armor) dummy.getItem();
         *
         * //TODO add checks for shield here. }
         */

        Condition preCondition = _preCondition;
        if (itemOrWeapon) {
            preCondition = _itemPreCondition;
        }
        if (preCondition == null) {
            return true;
        }

        Env env = new Env();
        env.cha = activeChar;
        if (target.isL2Character()) // TODO: object or char?
        {
            env.target = (L2Character) target;
        }
        env.skill = this;

        if (!preCondition.test(env)) {
            String msg = preCondition.getMessage();
            if (msg != null) {
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_S2).addString(msg));
            }

            return false;
        }
        return true;
    }

    public boolean checkForceCondition(L2PcInstance activeChar, int id) {
        if (Config.DISABLE_FORCES) {
            return true;
        }

        int forceId = 0;
        boolean isBattle = isBattleForceSkill();

        if (isBattle) {
            forceId = 5104;
        } else {
            forceId = 5105;
        }

        L2Effect force = activeChar.getFirstEffect(forceId);
        if (force != null) {
            /*
             * int forceLvl = force.getLevel();
             *
             * if (forceLvl >= getForceLvlFor(id)) return true;
             */

            return (force.getLevel() >= getForceLvlFor(id));
        }
        return false;
    }

    public int getForceLvlFor(int id) {
        int Level = 5;
        switch (_id) {
            case 454:
            case 455:
            case 456:
            case 457:
            case 458:
            case 459:
            case 460:
            case 1424:
            case 1425:
            case 1426:
            case 1427:
                Level = 2;
                break;
            case 1419:
            case 1420:
            case 1421:
            case 1422:
            case 1423:
            case 1428:
                Level = 3;
                break;
        }
        return Level;
    }

    public final FastList<L2Object> getTargetList(L2Character activeChar, boolean onlyFirst) {
        // Init to null the target of the skill
        L2Character target = null;

        // Get the L2Objcet targeted by the user of the skill at this moment
        L2Object objTarget = activeChar.getTarget();
        // If the L2Object targeted is a L2Character, it becomes the L2Character target
        if (objTarget != null && objTarget.isL2Character()) {
            target = (L2Character) objTarget;
        }

        return getTargetList(activeChar, onlyFirst, target);
    }

    /**
     * Return all targets of the skill in a table in function a the skill
     * type.<BR><BR>
     *
     * <B><U> Values of skill type</U> :</B><BR><BR> <li>ONE : The skill can
     * only be used on the L2PcInstance targeted, or on the caster if it's a
     * L2PcInstance and no L2PcInstance targeted</li> <li>SELF</li> <li>HOLY,
     * UNDEAD</li> <li>PET</li> <li>AURA, AURA_CLOSE</li> <li>AREA</li>
     * <li>MULTIFACE</li> <li>PARTY, CLAN</li> <li>CORPSE_PLAYER, CORPSE_MOB,
     * CORPSE_CLAN</li> <li>UNLOCKABLE</li> <li>ITEM</li><BR><BR>
     *
     * @param activeChar The L2Character who use the skill
     *
     */
    public final FastList<L2Object> getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target) {
        return _targetList.getTargetList(new FastList<L2Object>(), activeChar, onlyFirst, target, this);
    }

    public final FastList<L2Object> getTargetList(L2Character activeChar) {
        return getTargetList(activeChar, false);
    }

    public final L2Object getFirstOfTargetList(L2Character activeChar) {
        FastList<L2Object> ftargets = getTargetList(activeChar, true);
        if (ftargets == null || ftargets.isEmpty()) {
            return null;
        }

        return ftargets.getFirst();
    }

    public final Func[] getStatFuncs(L2Effect effect, L2Character player) {
        if (!(player.isPlayer()) && !(player.isL2Attackable())
                && !(player.isL2Summon())) {
            return _emptyFunctionSet;
        }

        if (_funcTemplates == null) {
            return _emptyFunctionSet;
        }

        if (player.isInOlympiadMode() && _isForbOly) {
            return _emptyFunctionSet;
        }

        if (player.getChannel() >= 4 && _isForbEvent) {
            return _emptyFunctionSet;
        }

        List<Func> funcs = new FastList<Func>();
        for (FuncTemplate t : _funcTemplates) {
            Env env = new Env();
            env.cha = player;
            env.skill = this;
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

    public boolean hasEffects() {
        return (_effectTemplates != null && _effectTemplates.length > 0);
    }

    public final L2Effect[] getEffects(L2Character effector, L2Character effected) {
        return getEffects(effector, effected, 0, 0);
    }

    public final L2Effect[] getEffects(L2Character effector, L2Character effected, int effectCount, int effectCurTime) {
        if (isPassive() || _effectTemplates == null) {
            return _emptyEffectSet;
        }

        if (isPvpSkill() && (effected.isRaid() || effected.isDebuffProtected())) {
            return _emptyEffectSet;
        }

        if ((effector != effected) && effected.isInvul()) {
            return _emptyEffectSet;
        }

        List<L2Effect> effects = new FastList<L2Effect>();

        boolean skillMastery = false;

        if (!isToggle() && Formulas.calcSkillMastery(effector)) {
            skillMastery = true;
        }

        for (EffectTemplate et : _effectTemplates) {
            Env env = new Env();
            env.cha = effector;
            env.target = effected;
            env.skill = this;
            env.skillMastery = skillMastery;
            L2Effect e = et.getEffect(env);
            if (e != null) {
                if (effectCount > 0) {
                    e.setCount(effectCount);
                    e.setFirstTime(effectCurTime);
                }
                e.scheduleEffect();
                effects.add(e);
            }
        }

        if (effects.isEmpty()) {
            return _emptyEffectSet;
        }

        return effects.toArray(new L2Effect[effects.size()]);
    }

    public final void getEffects(L2Character effector, L2Character effected, double hp, double mp, double cp) {
        getEffects(effector, effected);
        if (_isStoreStat) {
            effected.setCurrentCp(cp);
            effected.setCurrentHpMp(hp, mp);
        }
    }

    public final L2Effect[] getEffectsSelf(L2Character effector) {
        if (isPassive()) {
            return _emptyEffectSet;
        }

        if (_effectTemplatesSelf == null) {
            return _emptyEffectSet;
        }

        List<L2Effect> effects = new FastList<L2Effect>();

        for (EffectTemplate et : _effectTemplatesSelf) {
            Env env = new Env();
            env.cha = effector;
            env.target = effector;
            env.skill = this;
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

    public final void attach(FuncTemplate f) {
        if (_funcTemplates == null) {
            _funcTemplates = new FuncTemplate[]{f};
        } else {
            int len = _funcTemplates.length;
            FuncTemplate[] tmp = new FuncTemplate[len + 1];
            System.arraycopy(_funcTemplates, 0, tmp, 0, len);
            tmp[len] = f;
            _funcTemplates = tmp;
        }
    }

    public final void attach(EffectTemplate effect) {
        if (_effectTemplates == null) {
            _effectTemplates = new EffectTemplate[]{effect};
        } else {
            int len = _effectTemplates.length;
            EffectTemplate[] tmp = new EffectTemplate[len + 1];
            System.arraycopy(_effectTemplates, 0, tmp, 0, len);
            tmp[len] = effect;
            _effectTemplates = tmp;
        }

        /*if (effect.isStoreStat()) {
         setStoreStat(true);
         }*/
    }

    public final void attachSelf(EffectTemplate effect) {
        if (_effectTemplatesSelf == null) {
            _effectTemplatesSelf = new EffectTemplate[]{effect};
        } else {
            int len = _effectTemplatesSelf.length;
            EffectTemplate[] tmp = new EffectTemplate[len + 1];
            System.arraycopy(_effectTemplatesSelf, 0, tmp, 0, len);
            tmp[len] = effect;
            _effectTemplatesSelf = tmp;
        }
    }

    public final void attach(Condition c, boolean itemOrWeapon) {
        if (itemOrWeapon) {
            _itemPreCondition = c;
        } else {
            _preCondition = c;
        }
    }

    @Override
    public String toString() {
        return "" + _name + "[id=" + _id + ",lvl=" + _level + "]";
    }

    //хиро-дебаффы
    public final boolean isHeroDebuff() {
        return _isHeroDebuff;
    }

    //для профилей баффера
    public final boolean isForbiddenProfileSkill() {
        return _isForbiddenProfileSkill;
    }

    //для миража
    public final boolean isProtected() {
        return _isProtected;
    }

    //аурки
    public final boolean isNotAura() {
        return _isNotAura;
    }

    //эпические скиллы
    public final boolean isBattleForceSkill() {
        return _isBattleForceSkill;
    }

    public final boolean isSpellForceSkill() {
        return _isSpellForceSkill;
    }

    public final boolean isAugment() {
        return _isAugment;
    }

    public final boolean isAOEpvp() {
        return _isAOEpvp;
    }

    public final boolean isBuff() {
        return _isBuff;
    }

    public final boolean isSSBuff() {
        return _isSSBuff;
    }

    public final boolean isMalariaBuff() {
        return _isMalariaBuff;
    }

    public final boolean isNoShot() {
        return _isNoShot;
    }

    public final boolean isFixedReuse() {
        return _fixedReuse;
    }

    public final boolean isFixedCast() {
        return _fixedCast;
    }

    public final boolean isCancelProtected() {
        return _cancelProtected;
    }

    public final boolean isDebuff() {
        return _isDebuff;
    }

    public final boolean isMagicalSlow() {
        return _isMagicalSlow;
    }

    public final boolean isPhysicalSlow() {
        return _isPhysicalSlow;
    }

    public final boolean isMiscDebuff() {
        return _isMiscDebuff;
    }

    public final boolean isMiscDebuffPhys() {
        return _isMiscDebuffPhys;
    }

    public final boolean canTargetSelf() {
        return _canTargetSelf;
    }

    // для проверки левых скиллов
    public final boolean isNobleSkill() {
        return _isNobleSkill;
    }

    public final boolean isHerosSkill() {
        return _isHerosSkill;
    }

    public final boolean isClanSkill() {
        return _isClanSkill;
    }

    public final boolean isSiegeSkill() {
        return _isSiegeSkill;
    }

    public final boolean isFishingSkill() {
        return _isFishingSkill;
    }

    public final boolean isDwarvenSkill() {
        return _isDwarvenSkill;
    }

    public final boolean isMiscSkill() {
        return _isMiscSkill;
    }

    public final boolean isContinueAttack() {
        return _continueAttack;
    }

    public final boolean isNotUnlock() {
        return !_unlockSkill;
    }

    public boolean isNotForCursed() {
        return _isNotForCursed;
    }

    boolean sendSkillUseInfo() {
        return _sendSkillUseInfo;
    }

    public boolean useAltFormula(L2Character attacker) {
        if (_id != 1097 && attacker.isOverlord()) {
            return true;
        }

        return _useAltFormula;
    }
    private boolean _isStoreStat = false;

    public void setStoreStat(boolean f) {
        _isStoreStat = f;
    }

    public boolean isStoreStat() {
        return _isStoreStat;
    }

    //шанс прохождения
    public double getBaseLandRate() {
        return _baseLandRate;
    }

    public SkillType getBaseSkillType() {
        return _baseSkillType;
    }

    private boolean useTempHook(int _id) {

        switch (_id) {
            case 1358: // Block Shield
            case 1359: // Block Wind Walk
            case 1360: // Mass Block Shield
            case 1361: // Mass Block Wind Walk
                return true;
            default:
                return false;
        }
    }

    private double caclMatkMod(double ss, double mAtk, double mDef, double mAtkModifier) {
        if (isMagic()) {
            mAtkModifier = 14 * Math.sqrt(ss * mAtk) / mDef;
        }
        return mAtkModifier;
    }

    private double calcStatMod(double statmodifier, double mAtkModifier) {
        return (_baseLandRate / statmodifier) * mAtkModifier;
    }

    private double calcResMod(L2Character target, double rate) {
        double resMod = 1;
        double res = target.calcSkillResistans(this, _baseSkillType, _skillType);
        if (res < 0) {
            resMod = 1 / (1 - 0.075 * res);
        } else if (res >= 0) {
            resMod = 1 + 0.02 * res;
        }
        rate = rate * resMod;
        return rate;
    }

    private double calcLevelMod(double levelmod, double delta) {
        double deltamod = delta / 5 * 5.;
        if (deltamod != delta) {
            if (delta < 0) {
                levelmod = deltamod - 5.;
            } else if (delta >= 0) {
                levelmod = deltamod + 5.;
            }
        } else if (deltamod == delta) {
            levelmod = deltamod;
        }
        return levelmod;
    }

    private double calcAltVuln(L2Character target, double rate) {
        //костыли
        switch (_id) {
            case 403:
                rate *= target.calcStat(Stats.ROOT_VULN, 1, target, null);
                break;
            case 1056:
                rate = Math.min(rate, (_baseLandRate * 1.5));
                rate *= target.calcStat(Stats.CANCEL_VULN, 1, target, null);
                break;
        }
        return rate;
    }

    private double caclAttr(L2Character target) {
        switch (_id) {
            case 106: // deragment
            case 353:
                return target.calcStat(Stats.DERANGEMENT_VULN, 1, target, null);
            case 95: // earth
            case 367:
                return target.calcStat(Stats.EARTH_VULN, 1, target, this);
            case 400: // holy
                return target.calcStat(Stats.HOLY_VULN, 1, target, this);
        }
        return 1;
    }

    public double calcAltActivateRate(double mAtk, double mDef, L2Character target, double ss) {

        double mAtkModifier = caclMatkMod(ss, mAtk, mDef, 1);
        if (useTempHook(_id)) {
            int min = 60;
            if (target.getFirstEffect(1354) != null) {
                min = (int) (_baseLandRate / 2);
            }

            return (Math.min(_baseLandRate, min)) * mAtkModifier;
        }

        double rate = calcStatMod((isMagic() ? target.calcMENModifier() : target.calcCONModifier()), mAtkModifier);
        rate = calcResMod(target, rate);
        rate = calcAltVuln(target, rate);

        //levelmode
        return rate + calcLevelMod(0, (_magicLevel - (double) target.getLevel()));
    }

    public double calcActivateRate(double mAtk, double mDef, L2Character target, boolean alt) {
        mDef = Math.min(mDef, Config.MAX_MDEF_CALC);
        if (alt) {
            mAtk = Math.min(mAtk, Config.MAX_MATK_CALC);
            return calcAltActivateRate(mAtk, mDef, target, 4);//((sps || bss) ? 4 : 1));
        }
        mAtk = Math.min((mAtk / 2), Config.MAX_MATK_CALC);
        //double mAtkModifier = 14 * Math.sqrt(mAtk) / mDef;
        double mAtkModifier = mAtk / mDef;
        //double mAtkModifier = 11 * Math.pow(mAtk, 0.5) / mDef;
        //mAtkModifier = 14 * sqrt(ssmodifier * MAtk) / targetMDef
        switch (_baseSkillType) {
            case STUN: //Stun - (Base Land Rate/CON Modifier*Resistance)*(Learn Level/Target Level)
            case BLEED: //Physical Bleed - (Base Land Rate/*CON Modifier*Resistance)*(Learn Level/Target Level), can be cured by a cure bleed spell. | Magical Bleed - (Base Land Rate/Men Modifier*Resistance)*(Learn Level/Target Level)*(M.Atk./M.Def.), cannot be cured.
                if (isMagic()) {
                    return ((_baseLandRate / target.calcMENModifier() * target.calcSkillVulnerability(this)) * (_magicLevel / (double) target.getLevel()) * (mAtkModifier));
                } else {
                    return ((_baseLandRate / target.calcCONModifier() * target.calcSkillResistans(this, _baseSkillType, _skillType)) * (_magicLevel / (double) target.getLevel()));
                }
            case ROOT: //Root/Hold - (Base Land Rate*Men Modifier*Resistance)*(Learn Level/Target Level)*(M.Atk./M.Def.)
                return ((_baseLandRate * target.calcMENModifier() * target.calcSkillResistans(this, _baseSkillType, _skillType)) * (_magicLevel / (double) target.getLevel()) * (mAtkModifier));
            case SLEEP: //Sleep - (Base Land Rate/Men Modifier*Resistance)*(Learn Level/Target Level)*(M.Atk./M.Def.)
            case POISON: //Poison - (Base Land Rate/Men Modifier*Resistance)*(Learn Level/Target Level)*(M.Atk/M.Def.)
            case PARALYZE: //Paralyze (Base Land Rate/Men Modifier*Resistance)*(Learn Level/Target Level)*(M.Atk./M.Def.)
                //return ((_baseLandRate / target.calcMENModifier() * target.calcSkillResistans(this, _baseSkillType, _skillType)) * (_magicLevel / (double) target.getLevel()) * (mAtk / target.getMDef()));
                return ((_baseLandRate * target.calcMENModifier() * target.calcSkillResistans(this, _baseSkillType, _skillType)) * (_magicLevel / (double) target.getLevel()) * (mAtkModifier));
            case DEBUFF: //
                if (isMagicalSlow()) // Magical Slow - (Base Land Rate/Men Modifier)*(Learn Level/Target Level)*(M.Atk/M.Def.)
                {
                    return ((_baseLandRate / target.calcMENModifier() * (target.calcStat(Stats.EARTH_VULN, 1, target, this))) * (_magicLevel / (double) target.getLevel()) * (mAtkModifier));
                } else if (isPhysicalSlow() || isMiscDebuffPhys()) // Physical Slow (Cripple) - (Base Land Rate/Men Modifier)*(Learn Level/Target Level)
                {
                    return ((_baseLandRate / target.calcMENModifier() * caclAttr(target)) * (_magicLevel / (double) target.getLevel()));//(((double) this.getLevel() / (double) target.getLevel()));
                }
                return ((_baseLandRate / target.calcMENModifier() * target.calcSkillResistans(this, _baseSkillType, _skillType)) * (_magicLevel / (double) target.getLevel()) * (mAtkModifier));
            //case WEAKNESS: //
            default:
                return (Math.min(_baseLandRate, 60));
        }
    }

    public boolean isDisabledFor(L2PcInstance player) {
        if (player.isFishing() && !_isFishingSkillType) {
            return true;
        }

        if (player.isInOlympiadMode() && _isForbOly) {
            return true;
        }

        if (player.getChannel() >= 4 && _isForbEvent) {
            return true;
        }

        if (Config.PVP_ZONE_REWARDS && ZoneManager.getInstance().isSkillDisabled(player, _id)) {
            return true;
        }

        if (player.isMounted()) {
            if (player.isFlying()) {
                if (_id == 327 || _id == 4289) {
                    return false;
                }
                return true;
            }

            if (_isBuff || _id == 325) {
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isForbidOly() {
        return _isForbOly;
    }

    public boolean isForbidEvent() {
        return _isForbEvent;
    }

    public String getAugInfo() {
        return SkillTable.getAugmentInfo(_id);
    }
}
