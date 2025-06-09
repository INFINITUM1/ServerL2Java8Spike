package net.sf.l2j.gameserver.skills;

import java.util.logging.Logger;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerState;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerState.CheckPlayerState;
import net.sf.l2j.gameserver.skills.conditions.ConditionUsingItemType;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.templates.*;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * Global calculations, can be modified by server admins
 */
public final class Formulas {

    /**
     * Regen Task period
     */
    protected static final Logger _log = AbstractLogger.getLogger(L2Character.class.getName());
    public static final int MAX_STAT_VALUE = 100;
    private static final double[] STRCompute = new double[]{1.036, 34.845}; //{1.016, 28.515}; for C1
    private static final double[] INTCompute = new double[]{1.020, 31.375}; //{1.020, 31.375}; for C1
    private static final double[] DEXCompute = new double[]{1.009, 19.360}; //{1.009, 19.360}; for C1
    private static final double[] WITCompute = new double[]{1.050, 20.000}; //{1.050, 20.000}; for C1
    private static final double[] CONCompute = new double[]{1.030, 27.632}; //{1.015, 12.488}; for C1
    private static final double[] MENCompute = new double[]{1.010, -0.060}; //{1.010, -0.060}; for C1
    protected static final double[] WITbonus = new double[MAX_STAT_VALUE];
    protected static final double[] MENbonus = new double[MAX_STAT_VALUE];
    protected static final double[] INTbonus = new double[MAX_STAT_VALUE];
    protected static final double[] STRbonus = new double[MAX_STAT_VALUE];
    protected static final double[] DEXbonus = new double[MAX_STAT_VALUE];
    protected static final double[] CONbonus = new double[MAX_STAT_VALUE];

    // These values are 100% matching retail tables, no need to change and no need add
    // calculation into the stat bonus when accessing (not efficient),
    // better to have everything precalculated and use values directly (saves CPU)
    public static void init() {
        for (int i = 0; i < STRbonus.length; i++) {
            STRbonus[i] = Math.floor(Math.pow(STRCompute[0], i - STRCompute[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < INTbonus.length; i++) {
            INTbonus[i] = Math.floor(Math.pow(INTCompute[0], i - INTCompute[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < DEXbonus.length; i++) {
            DEXbonus[i] = Math.floor(Math.pow(DEXCompute[0], i - DEXCompute[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < WITbonus.length; i++) {
            WITbonus[i] = Math.floor(Math.pow(WITCompute[0], i - WITCompute[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < CONbonus.length; i++) {
            CONbonus[i] = Math.floor(Math.pow(CONCompute[0], i - CONCompute[1]) * 100 + .5d) / 100;
        }
        for (int i = 0; i < MENbonus.length; i++) {
            MENbonus[i] = Math.floor(Math.pow(MENCompute[0], i - MENCompute[1]) * 100 + .5d) / 100;
        }
    }

    static class FuncAddLevel3 extends Func {

        static final FuncAddLevel3[] _instancies = new FuncAddLevel3[Stats.NUM_STATS];

        static Func getInstance(Stats stat) {
            int pos = stat.ordinal();
            if (_instancies[pos] == null) {
                _instancies[pos] = new FuncAddLevel3(stat);
            }
            return _instancies[pos];
        }

        private FuncAddLevel3(Stats pStat) {
            super(pStat, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += env.cha.getLevel() / 3.0;
        }
    }

    static class FuncMultLevelMod extends Func {

        static final FuncMultLevelMod[] _instancies = new FuncMultLevelMod[Stats.NUM_STATS];

        static Func getInstance(Stats stat) {
            int pos = stat.ordinal();
            if (_instancies[pos] == null) {
                _instancies[pos] = new FuncMultLevelMod(stat);
            }
            return _instancies[pos];
        }

        private FuncMultLevelMod(Stats pStat) {
            super(pStat, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            env.value *= env.cha.getLevelMod();
        }
    }

    static class FuncMultRegenResting extends Func {

        static final FuncMultRegenResting[] _instancies = new FuncMultRegenResting[Stats.NUM_STATS];

        /**
         * Return the Func object corresponding to the state concerned.<BR><BR>
         */
        static Func getInstance(Stats stat) {
            int pos = stat.ordinal();

            if (_instancies[pos] == null) {
                _instancies[pos] = new FuncMultRegenResting(stat);
            }

            return _instancies[pos];
        }

        /**
         * Constructor of the FuncMultRegenResting.<BR><BR>
         */
        private FuncMultRegenResting(Stats pStat) {
            super(pStat, 0x20, null);
            setCondition(new ConditionPlayerState(CheckPlayerState.RESTING, true));
        }

        /**
         * Calculate the modifier of the state concerned.<BR><BR>
         */
        @Override
        public void calc(Env env) {
            if (!cond.test(env)) {
                return;
            }

            env.value *= 1.45;
        }
    }

    static class FuncPAtkMod extends Func {

        static final FuncPAtkMod _fpa_instance = new FuncPAtkMod();

        static Func getInstance() {
            return _fpa_instance;
        }

        private FuncPAtkMod() {
            super(Stats.POWER_ATTACK, 0x30, null);
        }

        @Override
        public void calc(Env env) {
            env.value *= STRbonus[env.cha.getSTR()] * env.cha.getLevelMod();
        }
    }

    static class FuncMAtkMod extends Func {

        static final FuncMAtkMod _fma_instance = new FuncMAtkMod();

        static Func getInstance() {
            return _fma_instance;
        }

        private FuncMAtkMod() {
            super(Stats.MAGIC_ATTACK, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            double intb = INTbonus[env.cha.getINT()];
            double lvlb = env.cha.getLevelMod();
            env.value *= (lvlb * lvlb) * (intb * intb);
            if (env.value > 36000) {
                env.value = 36000;
            }
        }
    }

    static class FuncMDefMod extends Func {

        static final FuncMDefMod _fmm_instance = new FuncMDefMod();

        static Func getInstance() {
            return _fmm_instance;
        }

        private FuncMDefMod() {
            super(Stats.MAGIC_DEFENCE, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            env.value = env.cha.calcMDefMod(env.value);
            env.value *= MENbonus[env.cha.getMEN()] * env.cha.getLevelMod();
        }
    }

    static class FuncPDefMod extends Func {

        static final FuncPDefMod _fmm_instance = new FuncPDefMod();

        static Func getInstance() {
            return _fmm_instance;
        }

        private FuncPDefMod() {
            super(Stats.POWER_DEFENCE, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            env.value = env.cha.calcPDefMod(env.value);
            env.value *= env.cha.getLevelMod();
        }
    }

    static class FuncBowAtkRange extends Func {

        private static final FuncBowAtkRange _fbar_instance = new FuncBowAtkRange();

        static Func getInstance() {
            return _fbar_instance;
        }

        private FuncBowAtkRange() {
            super(Stats.POWER_ATTACK_RANGE, 0x10, null);
            setCondition(new ConditionUsingItemType(L2WeaponType.BOW.mask()));
        }

        @Override
        public void calc(Env env) {
            if (!cond.test(env)) {
                return;
            }
            env.value += 450;
        }
    }

    static class FuncAtkAccuracy extends Func {

        static final FuncAtkAccuracy _faa_instance = new FuncAtkAccuracy();

        static Func getInstance() {
            return _faa_instance;
        }

        private FuncAtkAccuracy() {
            super(Stats.ACCURACY_COMBAT, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += Math.sqrt(env.cha.getDEX()) * 6;
            env.value += env.cha.getLevel();
            env.value = env.cha.calcAtkAccuracy(env.value);
        }
    }

    static class FuncAtkEvasion extends Func {

        static final FuncAtkEvasion _fae_instance = new FuncAtkEvasion();

        static Func getInstance() {
            return _fae_instance;
        }

        private FuncAtkEvasion() {
            super(Stats.EVASION_RATE, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += Math.sqrt(env.cha.getDEX()) * 6;
            env.value += env.cha.getLevel();
        }
    }

    static class FuncAtkCritical extends Func {

        static final FuncAtkCritical _fac_instance = new FuncAtkCritical();

        static Func getInstance() {
            return _fac_instance;
        }

        private FuncAtkCritical() {
            super(Stats.CRITICAL_RATE, 0x30, null);
        }

        @Override
        public void calc(Env env) {
            env.value = env.cha.calcAtkCritical(env.value, DEXbonus[env.cha.getDEX()]);
            env.value = Math.min(env.value, Config.MAX_PCRIT_RATE);
        }
    }

    static class FuncMAtkCritical extends Func {

        static final FuncMAtkCritical _fac_instance = new FuncMAtkCritical();

        static Func getInstance() {
            return _fac_instance;
        }

        private FuncMAtkCritical() {
            super(Stats.MCRITICAL_RATE, 0x30, null);
        }

        @Override
        public void calc(Env env) {
            env.value = env.cha.calcMAtkCritical(env.value, DEXbonus[env.cha.getWIT()]);
        }
    }

    static class FuncMoveSpeed extends Func {

        static final FuncMoveSpeed _fms_instance = new FuncMoveSpeed();

        static Func getInstance() {
            return _fms_instance;
        }

        private FuncMoveSpeed() {
            super(Stats.RUN_SPEED, 0x30, null);
        }

        @Override
        public void calc(Env env) {
            env.value *= DEXbonus[env.cha.getDEX()];
        }
    }

    static class FuncPAtkSpeed extends Func {

        static final FuncPAtkSpeed _fas_instance = new FuncPAtkSpeed();

        static Func getInstance() {
            return _fas_instance;
        }

        private FuncPAtkSpeed() {
            super(Stats.POWER_ATTACK_SPEED, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            env.value *= DEXbonus[env.cha.getDEX()];
        }
    }

    static class FuncMAtkSpeed extends Func {

        static final FuncMAtkSpeed _fas_instance = new FuncMAtkSpeed();

        static Func getInstance() {
            return _fas_instance;
        }

        private FuncMAtkSpeed() {
            super(Stats.MAGIC_ATTACK_SPEED, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            env.value *= WITbonus[env.cha.getWIT()];
        }
    }

    static class FuncHennaSTR extends Func {

        static final FuncHennaSTR _fh_instance = new FuncHennaSTR();

        static Func getInstance() {
            return _fh_instance;
        }

        private FuncHennaSTR() {
            super(Stats.STAT_STR, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += env.cha.getHennaStatSTR();
        }
    }

    static class FuncHennaDEX extends Func {

        static final FuncHennaDEX _fh_instance = new FuncHennaDEX();

        static Func getInstance() {
            return _fh_instance;
        }

        private FuncHennaDEX() {
            super(Stats.STAT_DEX, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += env.cha.getHennaStatDEX();
        }
    }

    static class FuncHennaINT extends Func {

        static final FuncHennaINT _fh_instance = new FuncHennaINT();

        static Func getInstance() {
            return _fh_instance;
        }

        private FuncHennaINT() {
            super(Stats.STAT_INT, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += env.cha.getHennaStatINT();
        }
    }

    static class FuncHennaMEN extends Func {

        static final FuncHennaMEN _fh_instance = new FuncHennaMEN();

        static Func getInstance() {
            return _fh_instance;
        }

        private FuncHennaMEN() {
            super(Stats.STAT_MEN, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += env.cha.getHennaStatMEN();
        }
    }

    static class FuncHennaCON extends Func {

        static final FuncHennaCON _fh_instance = new FuncHennaCON();

        static Func getInstance() {
            return _fh_instance;
        }

        private FuncHennaCON() {
            super(Stats.STAT_CON, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += env.cha.getHennaStatCON();
        }
    }

    static class FuncHennaWIT extends Func {

        static final FuncHennaWIT _fh_instance = new FuncHennaWIT();

        static Func getInstance() {
            return _fh_instance;
        }

        private FuncHennaWIT() {
            super(Stats.STAT_WIT, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            env.value += env.cha.getHennaStatWIT();
        }
    }

    static class FuncMaxHpAdd extends Func {

        static final FuncMaxHpAdd _fmha_instance = new FuncMaxHpAdd();

        static Func getInstance() {
            return _fmha_instance;
        }

        private FuncMaxHpAdd() {
            super(Stats.MAX_HP, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            L2PcTemplate t = (L2PcTemplate) env.cha.getTemplate();
            //int lvl = env.cha.getLevel() - t.classBaseLevel;
            int lvl = Math.max(0, env.cha.getLevel() - t.classBaseLevel);

            double hpmod = t.lvlHpMod * lvl;
            double hpmax = (t.lvlHpAdd + hpmod) * lvl;
            double hpmin = (t.lvlHpAdd * lvl) + hpmod;
            env.value += (hpmax + hpmin) / 2;
        }
    }

    static class FuncMaxHpMul extends Func {

        static final FuncMaxHpMul _fmhm_instance = new FuncMaxHpMul();

        static Func getInstance() {
            return _fmhm_instance;
        }

        private FuncMaxHpMul() {
            super(Stats.MAX_HP, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            env.value *= CONbonus[env.cha.getCON()];
        }
    }

    static class FuncMaxCpAdd extends Func {

        static final FuncMaxCpAdd _fmca_instance = new FuncMaxCpAdd();

        static Func getInstance() {
            return _fmca_instance;
        }

        private FuncMaxCpAdd() {
            super(Stats.MAX_CP, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            L2PcTemplate t = (L2PcTemplate) env.cha.getTemplate();
            //int lvl = env.cha.getLevel() - t.classBaseLevel;
            int lvl = Math.max(0, env.cha.getLevel() - t.classBaseLevel);

            double cpmod = t.lvlCpMod * lvl;
            double cpmax = (t.lvlCpAdd + cpmod) * lvl;
            double cpmin = (t.lvlCpAdd * lvl) + cpmod;
            env.value += (cpmax + cpmin) / 2;
        }
    }

    static class FuncMaxCpMul extends Func {

        static final FuncMaxCpMul _fmcm_instance = new FuncMaxCpMul();

        static Func getInstance() {
            return _fmcm_instance;
        }

        private FuncMaxCpMul() {
            super(Stats.MAX_CP, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            env.value *= CONbonus[env.cha.getCON()];
        }
    }

    static class FuncMaxMpAdd extends Func {

        static final FuncMaxMpAdd _fmma_instance = new FuncMaxMpAdd();

        static Func getInstance() {
            return _fmma_instance;
        }

        private FuncMaxMpAdd() {
            super(Stats.MAX_MP, 0x10, null);
        }

        @Override
        public void calc(Env env) {
            L2PcTemplate t = (L2PcTemplate) env.cha.getTemplate();
            //int lvl = env.cha.getLevel() - t.classBaseLevel;
            int lvl = Math.max(0, env.cha.getLevel() - t.classBaseLevel);

            double mpmod = t.lvlMpMod * lvl;
            double mpmax = (t.lvlMpAdd + mpmod) * lvl;
            double mpmin = (t.lvlMpAdd * lvl) + mpmod;
            env.value += (mpmax + mpmin) / 2;
        }
    }

    static class FuncMaxMpMul extends Func {

        static final FuncMaxMpMul _fmmm_instance = new FuncMaxMpMul();

        static Func getInstance() {
            return _fmmm_instance;
        }

        private FuncMaxMpMul() {
            super(Stats.MAX_MP, 0x20, null);
        }

        @Override
        public void calc(Env env) {
            env.value *= MENbonus[env.cha.getMEN()];
        }
    }

    /**
     * Return the standard NPC Calculator set containing ACCURACY_COMBAT and
     * EVASION_RATE.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> A calculator is created to manage and
     * dynamically calculate the effect of a character property (ex : MAX_HP,
     * REGENERATE_HP_RATE...). In fact, each calculator is a table of Func
     * object in which each Func represents a mathematic function : <BR><BR>
     *
     * FuncAtkAccuracy ->
     * Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
     *
     * To reduce cache memory use, L2NPCInstances who don't have skills share
     * the same Calculator set called <B>NPC_STD_CALCULATOR</B>.<BR><BR>
     *
     */
    public static Calculator[] getStdNPCCalculators() {
        Calculator[] std = new Calculator[Stats.NUM_STATS];

        // Add the FuncAtkAccuracy to the Standard Calculator of ACCURACY_COMBAT
        std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
        std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());

        // Add the FuncAtkEvasion to the Standard Calculator of EVASION_RATE
        std[Stats.EVASION_RATE.ordinal()] = new Calculator();
        std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
        return std;
    }

    /**
     * Add basics Func objects to L2PcInstance and L2Summon.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> A calculator is created to manage and
     * dynamically calculate the effect of a character property (ex : MAX_HP,
     * REGENERATE_HP_RATE...). In fact, each calculator is a table of Func
     * object in which each Func represents a mathematic function : <BR><BR>
     *
     * FuncAtkAccuracy ->
     * Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
     *
     * @param cha L2PcInstance or L2Summon that must obtain basic Func objects
     */
    public static void addFuncsToNewCharacter(L2Character cha) {
        if (cha.isPlayer()) {
            cha.addStatFunc(FuncMaxHpAdd.getInstance());
            cha.addStatFunc(FuncMaxHpMul.getInstance());
            cha.addStatFunc(FuncMaxCpAdd.getInstance());
            cha.addStatFunc(FuncMaxCpMul.getInstance());
            cha.addStatFunc(FuncMaxMpAdd.getInstance());
            cha.addStatFunc(FuncMaxMpMul.getInstance());
            //cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_HP_RATE));
            //cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_CP_RATE));
            //cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_MP_RATE));
            cha.addStatFunc(FuncBowAtkRange.getInstance());
            //cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.POWER_ATTACK));
            //cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.POWER_DEFENCE));
            //cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.MAGIC_DEFENCE));
            cha.addStatFunc(FuncPAtkMod.getInstance());
            cha.addStatFunc(FuncMAtkMod.getInstance());
            cha.addStatFunc(FuncPDefMod.getInstance());
            cha.addStatFunc(FuncMDefMod.getInstance());
            cha.addStatFunc(FuncAtkCritical.getInstance());
            cha.addStatFunc(FuncMAtkCritical.getInstance());
            cha.addStatFunc(FuncAtkAccuracy.getInstance());
            cha.addStatFunc(FuncAtkEvasion.getInstance());
            cha.addStatFunc(FuncPAtkSpeed.getInstance());
            cha.addStatFunc(FuncMAtkSpeed.getInstance());
            cha.addStatFunc(FuncMoveSpeed.getInstance());

            cha.addStatFunc(FuncHennaSTR.getInstance());
            cha.addStatFunc(FuncHennaDEX.getInstance());
            cha.addStatFunc(FuncHennaINT.getInstance());
            cha.addStatFunc(FuncHennaMEN.getInstance());
            cha.addStatFunc(FuncHennaCON.getInstance());
            cha.addStatFunc(FuncHennaWIT.getInstance());
        } else if (cha.isPet()) {
            cha.addStatFunc(FuncPAtkMod.getInstance());
            cha.addStatFunc(FuncMAtkMod.getInstance());
            cha.addStatFunc(FuncPDefMod.getInstance());
            cha.addStatFunc(FuncMDefMod.getInstance());
        } else if (cha.isL2Summon()) {
            //cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_HP_RATE));
            //cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_MP_RATE));
            cha.addStatFunc(FuncAtkCritical.getInstance());
            cha.addStatFunc(FuncMAtkCritical.getInstance());
            cha.addStatFunc(FuncAtkAccuracy.getInstance());
            cha.addStatFunc(FuncAtkEvasion.getInstance());
        }
    }

    /**
     * Calculate the HP regen rate (base + modifiers).<BR><BR>
     */
    public static double calcHpRegen(L2Character cha) {
        double value = cha.getTemplate().baseHpReg;
        double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
        double hpRegenBonus = 0;

        if (Config.L2JMOD_CHAMPION_ENABLE && cha.isChampion()) {
            hpRegenMultiplier *= Config.L2JMOD_CHAMPION_HP_REGEN;
        }
        if (Config.L2JMOD_CHAMPION_ENABLE_BLUE && cha.isChampion1()) {
            hpRegenMultiplier *= Config.L2JMOD_CHAMPION_HP_REGEN_BLUE;
        }

        if (cha.isPlayer()) {
            L2PcInstance player = cha.getPlayer();
            if (player.isPcNpc()) {
                return 1;
            }

            // Calculate correct baseHpReg value for certain level of PC
            value += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;

            // SevenSigns Festival modifier
            if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant()) {
                hpRegenMultiplier *= calcFestivalRegenModifier(player);
            } else {
                double siegeModifier = calcSiegeRegenModifer(player);
                if (siegeModifier > 0) {
                    hpRegenMultiplier *= siegeModifier;
                }
            }

            if (player.isInsideZone(L2Character.ZONE_CLANHALL) && player.getClan() != null) {
                int clanHallIndex = player.getClan().getHasHideout();
                if (clanHallIndex > 0) {
                    ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
                    if (clansHall != null) {
                        if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null) {
                            hpRegenMultiplier *= 1 + (double) clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
                        }
                    }
                }
            }

            // Mother Tree effect is calculated at last
            if (player.isInsideZone(L2Character.ZONE_MOTHERTREE)) {
                hpRegenBonus += 2;
            }

            // Calculate Movement bonus
            if (player.isSitting()) {
                hpRegenMultiplier *= 1.5;      // Sitting
            } else if (!player.isMoving()) {
                hpRegenMultiplier *= 1.1; // Staying
            } else if (player.isRunning()) {
                hpRegenMultiplier *= 0.7; // Running
            }
            // Add CON bonus
            value *= cha.getLevelMod() * CONbonus[cha.getCON()];
        }

        return cha.calcStat(Stats.REGENERATE_HP_RATE, Math.max(value, 1), null, null) * hpRegenMultiplier + hpRegenBonus;
    }

    /**
     * Calculate the MP regen rate (base + modifiers).<BR><BR>
     */
    public static double calcMpRegen(L2Character cha) {
        double value = cha.getTemplate().baseMpReg;
        double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
        double mpRegenBonus = 0;

        if (cha.isPlayer()) {
            L2PcInstance player = cha.getPlayer();

            // Calculate correct baseMpReg value for certain level of PC
            value += 0.3 * ((player.getLevel() - 1) / 10.0);

            // SevenSigns Festival modifier
            if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant()) {
                mpRegenMultiplier *= calcFestivalRegenModifier(player);
            }

            // Mother Tree effect is calculated at last
            if (player.isInsideZone(L2Character.ZONE_MOTHERTREE)) {
                mpRegenBonus += 1;
            }

            if (player.isInsideZone(L2Character.ZONE_CLANHALL) && player.getClan() != null) {
                int clanHallIndex = player.getClan().getHasHideout();
                if (clanHallIndex > 0) {
                    ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
                    if (clansHall != null) {
                        if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null) {
                            mpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
                        }
                    }
                }
            }

            // Calculate Movement bonus
            if (player.isSitting()) {
                mpRegenMultiplier *= 1.5;      // Sitting
            } else if (!player.isMoving()) {
                mpRegenMultiplier *= 1.1; // Staying
            } else if (player.isRunning()) {
                mpRegenMultiplier *= 0.7; // Running
            }
            // Add MEN bonus
            value *= cha.getLevelMod() * MENbonus[cha.getMEN()];
        }

        return cha.calcStat(Stats.REGENERATE_MP_RATE, Math.max(value, 1), null, null) * mpRegenMultiplier + mpRegenBonus;
    }

    /**
     * Calculate the CP regen rate (base + modifiers).<BR><BR>
     */
    public static double calcCpRegen(L2Character cha) {
        double value = cha.getTemplate().baseHpReg;
        double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
        double cpRegenBonus = 0;

        if (cha.isPlayer()) {
            L2PcInstance player = cha.getPlayer();

            // Calculate correct baseHpReg value for certain level of PC
            value += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;

            // Calculate Movement bonus
            if (player.isSitting()) {
                cpRegenMultiplier *= 1.5;      // Sitting
            } else if (!player.isMoving()) {
                cpRegenMultiplier *= 1.1; // Staying
            } else if (player.isRunning()) {
                cpRegenMultiplier *= 0.7; // Running
            }
        } else // Calculate Movement bonus
            if (!cha.isMoving()) {
                cpRegenMultiplier *= 1.1; // Staying
            } else if (cha.isRunning()) {
                cpRegenMultiplier *= 0.7; // Running
            }

        // Apply CON bonus
        value *= cha.getLevelMod() * CONbonus[cha.getCON()];

        return cha.calcStat(Stats.REGENERATE_CP_RATE, Math.max(value, 1), null, null) * cpRegenMultiplier + cpRegenBonus;
    }

    @SuppressWarnings("deprecation")
    public static double calcFestivalRegenModifier(L2PcInstance activeChar) {
        final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
        final int oracle = festivalInfo[0];
        final int festivalId = festivalInfo[1];
        int[] festivalCenter;

        // If the player isn't found in the festival, leave the regen rate as it is.
        if (festivalId < 0) {
            return 0;
        }

        // Retrieve the X and Y coords for the center of the festival arena the player is in.
        if (oracle == SevenSigns.CABAL_DAWN) {
            festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
        } else {
            festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
        }

        // Check the distance between the player and the player spawn point, in the center of the arena.
        double distToCenter = activeChar.getDistance(festivalCenter[0], festivalCenter[1]);

        //if (Config.DEBUG)
        //	_log.info("Distance: " + distToCenter + ", RegenMulti: " + (distToCenter * 2.5) / 50);
        return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
    }

    public static double calcSiegeRegenModifer(L2PcInstance activeChar) {
        if (activeChar == null || activeChar.getClan() == null) {
            return 0;
        }

        Siege siege = SiegeManager.getInstance().getSiege(activeChar.getPosition().getX(), activeChar.getPosition().getY(), activeChar.getPosition().getZ());
        if (siege == null || !siege.getIsInProgress()) {
            return 0;
        }

        L2SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getClanId());
        if (siegeClan == null || siegeClan.getNumFlags() == 0 || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag(), true)) {
            return 0;
        }

        return 1.5; // If all is true, then modifer will be 50% more
    }

    /**
     * Calculate blow damage based on cAtk
     */
    public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, boolean shld, boolean ss) {
        if (target.getFirstEffect(446) != null) {
            attacker.sendMessage("Игрок увернулся от вашей атаки.");
            return 0;
        }

        if (target.isRaid()
                && Config.FIXED_DAMAGE_TO_BOSS >= 1) {
            return Config.FIXED_DAMAGE_TO_BOSS;
        }

        if (target.getNpcId() == 13016) {
            return 15;
        }

        double power = skill.getPower();
        if (ss && skill.getSSBoost() > 0) {
            power *= skill.getSSBoost();
        }
        //double damage = 1;//attacker.getPAtk(target);
        double defence = target.getPDef(attacker);
        if (shld) {
            defence += target.getShldDef();
        }

        //Multiplier should be removed, it's false ??
        double damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, power, target, skill);

        if (ss) {
            damage *= 2.;
        }
        //damage *= (double)attacker.getLevel()/target.getLevel();
        if (calcPrWindBonus(attacker)) {
            damage *= 1.2;
        }

        // get the natural vulnerability for the template
        if (target.isL2Npc()) {
            damage *= ((L2NpcInstance) target).getTemplate().getVulnerability(Stats.DAGGER_WPN_VULN);
        }

        // get the vulnerability for the instance due to skills (buffs, passives, toggles, etc)
        damage = target.calcStat(Stats.DAGGER_WPN_VULN, damage, target, null);
        damage *= 70. / defence;
        damage += Rnd.get() * attacker.getRandomDamage(target);
        // Sami: Must be removed, after armor resistances are checked.
        // These values are a quick fix to balance dagger gameplay and give
        // armor resistances vs dagger. daggerWpnRes could also be used if a skill
        // was given to all classes. The values here try to be a compromise.
        // They were originally added in a late C4 rev (2289).

        damage *= target.calcBlowDamageMul();
        if (damage > 0) {
            target.stopSleeping(null);
        }

        /*if (Config.MOB_FIXED_DAMAGE
         && target.isMonster()) {
         damage = checkFixedDamage(damage, Config.MOB_FIXED_DAMAGE_LIST.get(target.getNpcId()));
         }*/
        return Math.max(damage, 1);
    }

    /*private static double checkFixedDamage(double damage, Double dmg) {
     if (dmg == null) {
     return damage;
     }
     return dmg;
     }*/
    /**
     * Calculated damage caused by ATTACK of attacker on target, called
     * separatly for each weapon, if dual-weapon is used.
     *
     * @param attacker player or NPC that makes ATTACK
     * @param target player or NPC, target of ATTACK
     * @param miss one of ATTACK_XXX constants
     * @param crit if the ATTACK have critical success
     * @param dual if dual weapon is used
     * @param ss if weapon item was charged by soulshot
     * @return damage points
     */
    public static double calcPhysDam(L2Character attacker, L2Character target, L2Skill skill, boolean shld, boolean crit, boolean dual, boolean ss) {
        if (target.getFirstEffect(446) != null) {
            attacker.sendMessage("Игрок увернулся от вашей атаки.");
            return 0;
        }

        if (target.isRaid()
                && Config.FIXED_DAMAGE_TO_BOSS >= 1) {
            return Config.FIXED_DAMAGE_TO_BOSS;
        }

        if (target.getNpcId() == 13016) {
            return 15;
        }

        if (shld && Rnd.get(100) < 5) {
            target.sendPacket(Static.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
            return 1;
        }

        double damage = attacker.getPAtk(target);
        double defence = target.getPDef(attacker);

        if (skill != null) {
            /*
             * if (target.getFirstEffect(447) != null) return 0;
             */

            double skillpower = skill.getPower();
            float ssboost = skill.getSSBoost();
            if (ssboost <= 0) {
                damage += skillpower;
            } else if (ssboost > 0) {
                if (ss) {
                    skillpower *= ssboost;
                    damage += skillpower;
                } else {
                    damage += skillpower;
                }
            }
        } else //if(dual)
        //	damage /= 2.;
         if (ss) {
                damage *= 2.;
            }

        boolean heavy = false;
        boolean heavym = false;
        boolean light = false;
        boolean magic = false;
        if (attacker.isPlayer()) {
            switch (attacker.getClassId().getId()) {
                /*
                 * case 5: case 6: case 20: case 33: case 90: case 91: case 99:
                 * case 106: damage *= 0.6;
                 break;
                 */
                case 48:
                case 114:
                    if (skill != null) {
                        damage *= 1.5;
                    }
                    break;
            }

            L2Armor armor = attacker.getActiveChestArmorItem();
            if (armor != null) {
                if (attacker.isWearingHeavyArmor()) {
                    heavym = true;
                }
            }

            if (target.isPlayer()) {
                L2Armor armort = target.getActiveChestArmorItem();
                if (armort != null) {
                    if (target.isWearingHeavyArmor()) {
                        heavy = true;
                    }
                    if (target.isWearingLightArmor()) {
                        light = true;
                    }
                    if (target.isWearingMagicArmor()) {
                        magic = true;
                    }
                }
            }
        }

        // In C5 summons make 10 % less dmg in PvP.
        if (attacker.isL2Summon() && target.isPlayer()) {
            damage *= 0.9;
        }

        if ((target.isL2Npc()) && (attacker.isPlayer())) {
            damage *= attacker.calcStat(Stats.PVE_DMG, 1.0, null, null);
        }

        // defence modifier depending of the attacker weapon
        L2Weapon weapon = attacker.getActiveWeaponItem();
        Stats stat = null;
        if (weapon != null) {
            switch (weapon.getItemType()) {
                case BOW:
                    stat = Stats.BOW_WPN_VULN;
                    if (crit && skill == null) {
                        if (heavy) {
                            damage *= 0.8;
                        } else if (magic) {
                            damage *= Config.MAGIC_PDEF_EXP;
                        }
                    }
                    break;
                case BLUNT:
                case BIGBLUNT:
                    stat = Stats.BLUNT_WPN_VULN;
                    break;
                case DAGGER:
                    stat = Stats.DAGGER_WPN_VULN;
                    if (skill != null) {
                        damage *= 0.55;
                    }
                    if (heavym) {
                        damage *= 0.6;
                    }
                    break;
                case DUAL:
                    stat = Stats.DUAL_WPN_VULN;
                    if (skill != null) {
                        damage *= 1.05;
                    }
                    break;
                case DUALFIST:
                    stat = Stats.DUALFIST_WPN_VULN;
                    break;
                case ETC:
                    stat = Stats.ETC_WPN_VULN;
                    break;
                case FIST:
                    stat = Stats.FIST_WPN_VULN;
                    break;
                case POLE:
                    stat = Stats.POLE_WPN_VULN;
                    break;
                case SWORD:
                    stat = Stats.SWORD_WPN_VULN;
                    break;
                case BIGSWORD: //TODO: have a proper resistance/vulnerability for Big swords
                    stat = Stats.SWORD_WPN_VULN;
                    break;
            }
        }

        if (crit) {
            damage += attacker.getCriticalDmg(target, damage);
            if (attacker.getFirstEffect(357) != null) {
                damage *= 0.7;
            }
        }
        if (shld && !Config.ALT_GAME_SHIELD_BLOCKS) {
            defence += target.getShldDef();
        }

        damage = 70 * damage / defence;

        if (stat != null) {
            // get the vulnerability due to skills (buffs, passives, toggles, etc)
            damage = target.calcStat(stat, damage, target, null);
            if (target.isL2Npc()) {
                // get the natural vulnerability for the template
                damage *= ((L2NpcInstance) target).getTemplate().getVulnerability(stat);
            }
        }

        if (calcPrWindBonus(attacker)) {
            damage *= 1.2;
        }

        if (attacker.getFirstEffect(423) != null) {
            switch (attacker.getFirstEffect(423).getLevel()) {
                case 1:
                    damage *= 1.15;
                    break;
                case 2:
                    damage *= 1.23;
                    break;
                case 3:
                    damage *= 1.3;
                    break;
            }
            //double fire = target.calcStat(Stats.FIRE_VULN, target.getTemplate().baseFireVuln, target, null);
            damage *= target.calcStat(Stats.FIRE_VULN, target.getTemplate().baseFireVuln, target, null);
        }

        damage += Rnd.nextDouble() * damage / 10;

        if (shld && Config.ALT_GAME_SHIELD_BLOCKS) {
            damage -= target.getShldDef();
            damage = Math.max(damage, 0);
        }

        if (attacker.isL2Npc()) {
            //Skill Race : Undead
            if (((L2NpcInstance) attacker).getTemplate().getRace() == L2NpcTemplate.Race.UNDEAD) {
                damage /= attacker.getPDefUndead(target);
            }
            if (((L2NpcInstance) attacker).getTemplate().npcId == 29028) {
                damage /= target.getPDefValakas(attacker);
            }
        }

        if (target.isL2Npc()) {
            switch (((L2NpcInstance) target).getTemplate().getRace()) {
                case UNDEAD:
                    damage *= attacker.getPAtkUndead(target);
                    break;
                case BEAST:
                    damage *= attacker.getPAtkMonsters(target);
                    break;
                case ANIMAL:
                    damage *= attacker.getPAtkAnimals(target);
                    break;
                case PLANT:
                    damage *= attacker.getPAtkPlants(target);
                    break;
                case DRAGON:
                    damage *= attacker.getPAtkDragons(target);
                    break;
                case BUG:
                    damage *= attacker.getPAtkInsects(target);
                    break;
            }
            if (((L2NpcInstance) target).getTemplate().npcId == 29028) {
                damage *= attacker.getPAtkValakas(target);
            }
        }

        // Dmg bonusses in PvP fight
        if ((attacker.isPlayer() || attacker.isL2Summon()) && (target.isPlayer() || target.isL2Summon())) {
            if (skill == null) {
                damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
                damage /= target.calcStat(Stats.PVP_PHYSICAL_DEF, 1, null, null);
            } else {
                damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
            }
        }

        /*if (Config.MOB_FIXED_DAMAGE
         && target.isMonster()) {
         damage = checkFixedDamage(damage, Config.MOB_FIXED_DAMAGE_LIST.get(target.getNpcId()));
         }*/
        return Math.max(damage, 1);
    }

    public static double calcViciousDam(L2Character attacker, double damage, boolean skill) {
        if (skill) {
            damage *= 2;
        }

        return damage;
    }

    public static double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss, boolean mcrit) {
        if (target.isRaid()
                && Config.FIXED_DAMAGE_TO_BOSS >= 1) {
            return Config.FIXED_DAMAGE_TO_BOSS;
        }

        if (target.getNpcId() == 13016) {
            return 30;
        }

        double mAtk = attacker.getMAtk(target, skill);
        double mDef = target.getMDef(attacker, skill);
        if (bss) {
            mAtk *= 4;
        } else if (ss) {
            mAtk *= 2;
        }

        double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker) * calcSkillVulnerability(target, skill);

        // In C5 summons make 10 % less dmg in PvP.
        if (attacker.isL2Summon() && target.isPlayer()) {
            damage *= 0.9;
        }

        if (!attacker.isInOlympiadMode()) {
            damage *= Config.MAGIC_DAM_EXP;
        }

        if ((target.isL2Npc()) && (attacker.isPlayer())) {
            damage *= attacker.calcStat(Stats.PVE_DMG, 1.0, null, null);
        }

        //		if(attacker.isPlayer() && target.isPlayer()) damage *= 0.9; // PvP modifier (-10%)
        //if (skill.getSkillType() == SkillType.DRAIN)
        //	damage *= 0.95;
        // Failure calculation
        if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill)) {
            if (attacker.isPlayer()) {
                if (calcMagicSuccess(attacker, target, skill) && (target.getLevel() - attacker.getLevel()) <= 9) {
                    if (skill.getSkillType() == SkillType.DRAIN) {
                        attacker.sendPacket(Static.DRAIN_HALF_SUCCESFUL);
                    } else {
                        attacker.sendPacket(Static.ATTACK_FAILED);
                    }

                    damage /= 2;
                } else {
                    damage = 1;
                    attacker.sendPacket(SystemMessage.id(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getId()));
                }
            }

            if (target.isPlayer()) {
                if (skill.getSkillType() == SkillType.DRAIN) {
                    target.sendPacket(SystemMessage.id(SystemMessageId.RESISTED_S1_DRAIN).addString(attacker.getName()));
                } else {
                    target.sendPacket(SystemMessage.id(SystemMessageId.RESISTED_S1_MAGIC).addString(attacker.getName()));
                }
            }
        } else if (mcrit) {
            if (attacker.isInOlympiadMode()) {
                damage *= Config.MAGIC_CRIT_EXP_OLY;
            } else {
                damage *= Config.MAGIC_CRIT_EXP;
            }
            damage *= attacker.calcStat(Stats.MCRITICAL_POWER, 1, null, null);
        }

        // Pvp bonusses for dmg
        if ((attacker.isPlayer() || attacker.isL2Summon()) && (target.isPlayer() || target.isL2Summon())) {
            if (skill.isMagic()) {
                damage *= attacker.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
                damage /= target.calcStat(Stats.PVP_MAGICAL_DEF, 1, null, null);
            } else {
                damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
                damage /= target.calcStat(Stats.PVP_PHYS_KILL_DEF, 1, null, null);
            }
        }

        switch (skill.getId()) {
            case 1231: //case 1234:
            case 1245:
                damage *= 0.7;
                break;
        }

        /*if (Config.MOB_FIXED_DAMAGE
         && target.isMonster()) {
         damage = checkFixedDamage(damage, Config.MOB_FIXED_DAMAGE_LIST.get(target.getNpcId()));
         }*/
        return Math.max(damage, 1);
    }

    /**
     * Returns true in case of critical hit
     */
    public static boolean calcCrit(double rate) {
        return rate > Rnd.get(1000);
    }

    public static boolean calcCrit(L2Character activeChar, double rate) {
        if (activeChar.isBehindTarget()) {
            rate *= 1.6;
            if (activeChar.getFirstEffect(1357) != null) {
                rate *= 1.2;
            }
        } else if (!activeChar.isFrontTarget()) {
            rate *= 1.3;
        } else if (activeChar.getFirstEffect(356) != null) {
            rate *= 0.7;
        }
        return (Rnd.get(4000) < rate);
    }

    public static boolean calcPrWindBonus(L2Character activeChar) {
        return (activeChar.isBehindTarget() && activeChar.getFirstEffect(1357) != null);
    }

    /**
     * Calcul value of blow success
     */
    public static boolean calcBlow(L2Character activeChar, L2Character target, L2Skill skill, int chance) {
        /*switch (skill.getId()) {
         case 30:
         if (activeChar.isFrontTarget()) {
         return false;
         }
         break;
         case 344:
         case 263:
         if (Rnd.get(100) < 25) {
         return true;
         }
         break;
         }*/

 /*int blowChance = (int) activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 20) / 100), target, null);
         activeChar.sendMessage("##" + blowChance + "%#");
         return Rnd.get(290) < blowChance;*/
        double rate = activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 20) / 100), target, null);

        rate = Math.max(rate, Math.min(skill.getBaseLandRate(), Config.SKILLS_CHANCE_MIN));
        rate = Math.min(rate, Config.SKILLS_CHANCE_MAX);

        double rnd = Rnd.poker(rate);

        if (activeChar.getShowSkillChances()) {
            activeChar.sendMessage(skill.getName() + ", шанс: " + Math.round(rate) + "%; выпало: " + Math.round(rnd));
        }
        return (rnd < rate);
    }

    /**
     * Calcul value of lethal chance
     */
    public static double calcLethal(L2Character activeChar, L2Character target, int baseLethal) {
        return activeChar.calcStat(Stats.LETHAL_RATE, (baseLethal * ((double) activeChar.getLevel() / target.getLevel())), target, null);
    }

    public static boolean calcMCrit(double mRate) {
        return (Rnd.poker(mRate) < mRate);
    }

    /**
     * Returns true in case when ATTACK is canceled due to hit
     */
    public static boolean calcAtkBreak(L2Character target, double dmg) {
        if (target.getForceBuff() != null) {
            return true;
        }

        double init = 0;

        if (Config.ALT_GAME_CANCEL_CAST && target.isCastingNow()) {
            init = 15;
        }

        if (Config.ALT_GAME_CANCEL_BOW && target.isAttackingNow()) {
            L2Weapon wpn = target.getActiveWeaponItem();
            if (wpn != null && wpn.getItemType() == L2WeaponType.BOW) {
                init = 15;
            }
        }

        if (init <= 0) {
            return false; // No attack break
        }
        // Chance of break is higher with higher dmg
        init += Math.sqrt(13 * dmg);

        // Chance is affected by target MEN
        init -= (MENbonus[target.getMEN()] * 100 - 100);

        // Calculate all modifiers for ATTACK_CANCEL
        double rate = target.calcStat(Stats.ATTACK_CANCEL, init, null, null);

        // Adjust the rate to be between 1 and 99
        rate = Math.max(rate, 1);
        rate = Math.min(rate, 99);
        return Rnd.get(100) < rate;
    }

    /**
     * Calculate delay (in milliseconds) before next ATTACK
     */
    public static int calcPAtkSpd(L2Character attacker, L2Character target, double rate) {
        //return (int) (470000 / rate);
        return (int) (500000 / rate);
    }

    /**
     * Calculate delay (in milliseconds) for skills cast
     */
    public static int calcMAtkSpd(L2Character attacker, L2Character target, L2Skill skill, double skillTime) {
        if (skill.isMagic()) {
            return (int) (skillTime * 333 / attacker.getMAtkSpd());
        }
        return (int) (skillTime * 333 / attacker.getPAtkSpd());
    }

    /**
     * Calculate delay (in milliseconds) for skills cast
     */
    public static int calcMAtkSpd(L2Character attacker, L2Skill skill, double skillTime) {
        if (skill.isFixedCast()) {
            return (int) skillTime;
        }
        if (skill.isMagic()) {
            return (int) (skillTime * 333 / attacker.getMAtkSpd());
        }
        return (int) (skillTime * 333 / attacker.getPAtkSpd());
    }

    /**
     * Returns true if hit missed (taget evaded)
     */
    public static boolean calcHitMiss(L2Character attacker, L2Character target) {
        // accuracy+dexterity => probability to hit in percents
        int d = 85 + attacker.getAccuracy() - target.getEvasionRate(attacker);
        if (attacker.isBehindTarget()) {
            d *= 1.2;
        } else if (!attacker.isFrontTarget()) {
            d *= 1.1;
        }

        return d < Rnd.get(100);
    }

    /**
     * Returns true if shield defence successfull
     */
    public static boolean calcShldUse(L2Character attacker, L2Character target) {
        /*
         * double shldRate = 0; try
         {
         */
        double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, null) * DEXbonus[target.getDEX()];
        /*
         * }
         * catch(Exception e) { _log.warning("Formulas, calcShldUse [ERROR]:
         * target " + target.getName() + "; DEX: " + target.getDEX() + "
         * //rusxxx"); return false;
         }
         */

        if (shldRate == 0) {
            return false;
        }

        // Check for passive skill Aegis (316) or Aegis Stance (318)
        if ((target.getKnownSkill(316) == null || target.getFirstEffect(318) == null) && !target.isFront(attacker)) {
            return false;
        }

        // if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
        L2Weapon at_weapon = attacker.getActiveWeaponItem();
        if (at_weapon != null && at_weapon.getItemType() == L2WeaponType.BOW) {
            shldRate *= 1.3;
        }

        return shldRate > Rnd.get(100);
    }

    public static boolean calcMagicAffected(L2Character actor, L2Character target, L2Skill skill) {
        if (protectBoss(skill, target)) {
            return false;
        }

        double defence = 0;
        // TODO: CHECK/FIX THIS FORMULA UP!!
        if (skill.isActive() && skill.isOffensive()) {
            defence = target.getMDef(actor, skill);
        }
        double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(target, skill);
        double d = attack - defence;
        d /= attack + defence;
        d += 0.5 * Rnd.nextGaussian();
        return d > 0;
    }

    private static boolean protectBoss(L2Skill skill, L2Character target) {
        switch (skill.getSkillType()) {
            case CONFUSION:
            case MUTE:
            case PARALYZE:
            case ROOT:
            case FEAR:
            case SLEEP:
            case STUN:
            case DEBUFF:
            case AGGDEBUFF:
                if (target.isRaid()) {
                    return true;
                }
                break;
        }
        return false;
    }

    public static double calcSkillVulnerability(L2Character target, L2Skill skill) {
        double multiplier = 1;	// initialize...
        // Get the skill type to calculate its effect in function of base stats
        // of the L2Character target
        if (skill != null) {
            // first, get the natural template vulnerability values for the target
            multiplier = calcTemplateVuln(skill.getStat(), multiplier, target);

            // Next, calculate the elemental vulnerabilities
            multiplier = calcElementVuln(skill, target, multiplier);

            // Finally, calculate skilltype vulnerabilities
            multiplier = calcSkillTypeVuln(skill.getSkillType(), skill, target, multiplier);
        }
        return multiplier;
    }

    private static double calcTemplateVuln(Stats stat, double multiplier, L2Character target) {
        if (stat != null) {
            switch (stat) {
                case AGGRESSION:
                    multiplier *= target.getTemplate().baseAggressionVuln;
                    break;
                case BLEED:
                    multiplier *= target.getTemplate().baseBleedVuln;
                    break;
                case POISON:
                    multiplier *= target.getTemplate().basePoisonVuln;
                    break;
                case STUN:
                    multiplier *= target.getTemplate().baseStunVuln;
                    break;
                case ROOT:
                    multiplier *= target.getTemplate().baseRootVuln;
                    break;
                case MOVEMENT:
                    multiplier *= target.getTemplate().baseMovementVuln;
                    break;
                case CONFUSION:
                    multiplier *= target.getTemplate().baseConfusionVuln;
                    break;
                case SLEEP:
                    multiplier *= target.getTemplate().baseSleepVuln;
                    break;
                case FIRE:
                    multiplier *= target.getTemplate().baseFireVuln;
                    break;
                case WIND:
                    multiplier *= target.getTemplate().baseWindVuln;
                    break;
                case WATER:
                    multiplier *= target.getTemplate().baseWaterVuln;
                    break;
                case EARTH:
                    multiplier *= target.getTemplate().baseEarthVuln;
                    break;
                case HOLY:
                    multiplier *= target.getTemplate().baseHolyVuln;
                    break;
                case DARK:
                    multiplier *= target.getTemplate().baseDarkVuln;
                    break;
            }
        }
        return multiplier;
    }

    private static double calcSkillTypeVuln(SkillType type, L2Skill skill, L2Character target, double multiplier) {
        // For additional effects on PDAM and MDAM skills (like STUN, SHOCK, PARALYZE...)
        if (type != null && (type == SkillType.PDAM || type == SkillType.MDAM)) {
            type = skill.getEffectType();
        }
        if (type != null) {
            switch (type) {
                case BLEED:
                    multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
                    break;
                case POISON:
                    multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
                    break;
                case STUN:
                    multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
                    break;
                case PARALYZE:
                    multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
                    break;
                case ROOT:
                    multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
                    break;
                case SLEEP:
                    multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
                    break;
                case MUTE:
                case FEAR:
                case BETRAY:
                case AGGREDUCE_CHAR:
                    multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
                    break;
                case CONFUSION:
                    multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, target, null);
                    break;
                case DEBUFF:
                case WEAKNESS:
                    multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
                    break;
                default:
                    ;
            }
        }
        return multiplier;
    }

    private static double calcElementVuln(L2Skill skill, L2Character target, double multiplier) {
        switch (skill.getElement()) {
            case L2Skill.ELEMENT_EARTH:
                multiplier = target.calcStat(Stats.EARTH_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_FIRE:
                multiplier = target.calcStat(Stats.FIRE_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_WATER:
                multiplier = target.calcStat(Stats.WATER_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_WIND:
                multiplier = target.calcStat(Stats.WIND_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_HOLY:
                multiplier = target.calcStat(Stats.HOLY_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_DARK:
                multiplier = target.calcStat(Stats.DARK_VULN, multiplier, target, skill);
                break;
        }
        return multiplier;
    }

    public static double calcSkillResistans(L2Character target, L2Skill skill, SkillType type, SkillType deftype) {
        double multiplier = 1;	// initialize...
        // Get the skill type to calculate its effect in function of base stats
        // of the L2Character target
        // first, get the natural template vulnerability values for the target
        Stats stat = skill.getStat();
        if (stat != null) {
            switch (stat) {
                case AGGRESSION:
                    multiplier *= target.getTemplate().baseAggressionVuln;
                    break;
                case BLEED:
                    multiplier *= target.getTemplate().baseBleedVuln;
                    break;
                case POISON:
                    multiplier *= target.getTemplate().basePoisonVuln;
                    break;
                case STUN:
                    multiplier *= target.getTemplate().baseStunVuln;
                    break;
                case ROOT:
                    multiplier *= target.getTemplate().baseRootVuln;
                    break;
                case MOVEMENT:
                    multiplier *= target.getTemplate().baseMovementVuln;
                    break;
                case CONFUSION:
                    multiplier *= target.getTemplate().baseConfusionVuln;
                    break;
                case SLEEP:
                    multiplier *= target.getTemplate().baseSleepVuln;
                    break;
                case FIRE:
                    multiplier *= target.getTemplate().baseFireVuln;
                    break;
                case WIND:
                    multiplier *= target.getTemplate().baseWindVuln;
                    break;
                case WATER:
                    multiplier *= target.getTemplate().baseWaterVuln;
                    break;
                case EARTH:
                    multiplier *= target.getTemplate().baseEarthVuln;
                    break;
                case HOLY:
                    multiplier *= target.getTemplate().baseHolyVuln;
                    break;
                case DARK:
                    multiplier *= target.getTemplate().baseDarkVuln;
                    break;
            }
        }

        // Next, calculate the elemental vulnerabilities
        switch (skill.getElement()) {
            case L2Skill.ELEMENT_EARTH:
                multiplier = target.calcStat(Stats.EARTH_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_FIRE:
                multiplier = target.calcStat(Stats.FIRE_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_WATER:
                multiplier = target.calcStat(Stats.WATER_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_WIND:
                multiplier = target.calcStat(Stats.WIND_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_HOLY:
                multiplier = target.calcStat(Stats.HOLY_VULN, multiplier, target, skill);
                break;
            case L2Skill.ELEMENT_DARK:
                multiplier = target.calcStat(Stats.DARK_VULN, multiplier, target, skill);
                break;
        }

        switch (type) {
            case BLEED:
                multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
                break;
            case POISON:
                multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
                break;
            case STUN:
                multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
                break;
            case PARALYZE:
                multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
                break;
            case ROOT:
                switch (deftype) {
                    case MUTE:
                    case FEAR:
                    case CANCEL:
                        return (target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null));
                }
                multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
                break;
            case SLEEP:
                multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
                break;
            case MUTE:
            case FEAR:
            case BETRAY:
            case AGGREDUCE_CHAR:
                multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
                break;
            case CONFUSION:
                multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, target, null);
                break;
            case DEBUFF:
            case WEAKNESS:
                multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
                break;
        }
        return multiplier;
    }

    /*
     * public double calcStatModifier(SkillType type, L2Character target) {
     * double multiplier = 1; if (type == null) return multiplier;
     *
     * switch (type) { case STUN: case BLEED: multiplier = 2 -
     * Math.sqrt(CONbonus[target.getCON()]); break; case POISON: case SLEEP:
     * case DEBUFF: case WEAKNESS: case ERASE: case ROOT: case MUTE: case FEAR:
     * case BETRAY: case CONFUSION: case AGGREDUCE_CHAR: case PARALYZE:
     * multiplier = 2 - Math.sqrt(MENbonus[target.getMEN()]); break; default:
     * return multiplier; }
     *
     * return Math.max(0, multiplier);
     }
     */
    public static double calcCONModifier(L2Character target) {
        double multiplier = 2 - Math.sqrt(CONbonus[target.getCON()]);
        return Math.max(0, multiplier);
    }

    public static double calcMENModifier(L2Character target) {
        double multiplier = 2 - Math.sqrt(MENbonus[target.getMEN()]);
        return Math.max(0, multiplier);
    }

    public static boolean calcSkillSuccess(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean sps, boolean bss) {
        if (target.isRaid() && (skill.getId() >= 1339 && skill.getId() <= 1343)) {
            return (Rnd.get(100) <= Config.RAID_VORTEX_CHANCE);
        }

        if (target.isDebuffImmun(skill)) {
            return false;
        }

        /*
         * // фиксированный шанс Integer custom_rate =
         * Config.ALT_SKILL_CHANSE.get(skill.getId()); if (custom_rate != null)
         * return Rnd.get(100) < custom_rate;
         */
        //double rate = skill.calcActivateRate(attacker.getMAtk(), target.getMDef(), target, skill.useAltFormula(attacker));
        double rate = calcActivateRate(attacker, target, skill, Config.ALT_DEBUFF_CHANCES, Config.ALT_DEBUFF_CHANCES_TABLE.get(skill.getId()));

        rate = Math.max(rate, Math.min(skill.getBaseLandRate(), Config.SKILLS_CHANCE_MIN));
        rate = Math.min(rate, Config.SKILLS_CHANCE_MAX);

        double rnd = Rnd.poker(rate);

        if (attacker.getShowSkillChances()) {
            attacker.sendMessage(skill.getName() + ", шанс: " + Math.round(rate) + "%; выпало: " + Math.round(rnd));
        }

        return (rnd < rate);
    }

    private static double calcActivateRate(L2Character attacker, L2Character target, L2Skill skill, boolean fixed, Double fixed_rate) {
        if (fixed && fixed_rate != null) {
            return fixed_rate;
        }
        return skill.calcActivateRate(attacker.getMAtk(), target.getMDef(), target, skill.useAltFormula(attacker));
    }

    public static boolean calcMagicSuccess(L2Character attacker, L2Character target, L2Skill skill) {
        /*
         * double lvlDifference = (target.getLevel() - (skill.getMagicLevel() >
         * 0 ? skill.getMagicLevel() : attacker.getLevel())); int rate =
         * Math.round((float)(Math.pow(1.3, lvlDifference) * 100));
         *
         * return (Rnd.get(10000) > rate);
         */
        if (skill.isAugment()) {
            return true;
        }

        if (target.getLevel() - skill.getMagicLevel() < 18) {
            return true;
        }

        return false;
    }

    public static boolean calculateUnlockChance(L2Skill skill) {
        int level = skill.getLevel();
        int chance = 0;
        switch (level) {
            case 1:
                chance = 30;
                break;
            case 2:
                chance = 50;
                break;
            case 3:
                chance = 75;
                break;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
                chance = 100;
                break;
        }
        if (Rnd.get(120) > chance) {
            return false;
        }
        return true;
    }

    public static double calcManaDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss) {
        //Mana Burnt = (SQR(M.Atk)*Power*(Target Max MP/97))/M.Def
        double mAtk = attacker.getMAtk(target, skill);
        if (bss) {
            mAtk *= 9;
        } else {
            mAtk *= 4;
        }

        double damage = (Math.sqrt(mAtk) * skill.getPower(attacker) * (target.getMaxMp() / 97)) / target.getMDef(attacker, skill);
        //damage *= calcSkillVulnerability(target, skill);		
        damage *= calcSkillVulnerability(target, skill);
        return damage;
    }

    public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, int casterWIT) {
        double restorePercent = baseRestorePercent;
        if (restorePercent != 100 && restorePercent != 0) {

            restorePercent = baseRestorePercent * WITbonus[casterWIT];

            if (restorePercent - baseRestorePercent > 20.0) {
                restorePercent = baseRestorePercent + 20.0;
            }
        }

        restorePercent = Math.min(restorePercent, 100);
        restorePercent = Math.max(restorePercent, baseRestorePercent);
        return restorePercent;
    }

    public static double getSTRBonus(L2Character activeChar) {
        return STRbonus[activeChar.getSTR()];
    }

    public static double getINTBonus(L2Character activeChar) {
        return INTbonus[activeChar.getINT()];
    }

    public static boolean calcSkillMastery(L2Character actor) {
        if (actor == null || actor.isInOlympiadMode()) {
            return false;
        }
        return Rnd.get(200) < 2;//actor.getStat().getSkillMastery();
    }
}
