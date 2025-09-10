package net.sf.l2j.gameserver.model.actor.instance;

import java.lang.ref.WeakReference;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import static net.sf.l2j.Config.ALT_HENNA_SUB;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.Config.PvpColor;
import net.sf.l2j.Config.PvpTitleBonus;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.*;
import net.sf.l2j.gameserver.ai.*;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.cache.WarehouseCacheManager;
import net.sf.l2j.gameserver.datatables.CustomServerData.DonateSkill;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.*;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager.WaitingRoom;
import net.sf.l2j.gameserver.instancemanager.*;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.BypassManager.BypassType;
import net.sf.l2j.gameserver.model.BypassManager.DecodedBypass;
import net.sf.l2j.gameserver.model.TradeList.TradeItem;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.knownlist.PcKnownList;
import net.sf.l2j.gameserver.model.actor.stat.CharStat;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.actor.status.PcStatus;
import net.sf.l2j.gameserver.model.base.*;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.entity.*;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadGame;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.EffectTaskManager;
import net.sf.l2j.gameserver.templates.*;
import net.sf.l2j.gameserver.util.AntiFarm;
import net.sf.l2j.gameserver.util.AntiFarm.FarmDelay;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.Moderator;
import net.sf.l2j.gameserver.util.PeaceZone;
import net.sf.l2j.gameserver.util.WebStat;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.*;
import scripts.autoevents.encounter.Encounter;
import scripts.communitybbs.BB.Forum;
import scripts.communitybbs.CommunityBoard;
import scripts.communitybbs.Manager.ForumsBBSManager;
import scripts.events.custom.CustomPlayerEventList;
import scripts.items.IItemHandler;
import scripts.items.ItemHandler;

/**
 * This class represents all player characters in the world. There is always a
 * client-thread connected to this (except if a player-store is activated upon
 * logout).<BR>
 * <BR>
 *
 * @version $Revision: 1.66.2.41.2.33 $ $Date: 2005/04/11 10:06:09 $
 */
public class L2PcInstance extends L2PlayableInstance {

    public Location _raidloc;

    public static final int PS_NONE = 0;
    public static final int PS_SELL = 1;
    public static final int PS_BUY = 3;
    public static final int PS_MANUFACTURE = 5;
    public static final int PS_PACKAGE_SELL = 8;
    // multilang
    private String _lang = Config.MULTILANG_DEFAULT;
    private String _htmlPrefix = "";

    /**
     * The table containing all minimum level needed for each Expertise (None,
     * D, C, B, A, S)
     */
    private static final int[] GRADES = {
            SkillTreeTable.getInstance().getExpertiseLevel(0), // NONE
            SkillTreeTable.getInstance().getExpertiseLevel(1), // D
            SkillTreeTable.getInstance().getExpertiseLevel(2), // C
            SkillTreeTable.getInstance().getExpertiseLevel(3), // B
            SkillTreeTable.getInstance().getExpertiseLevel(4), // A
            SkillTreeTable.getInstance().getExpertiseLevel(5), // S
    };
    private static final int[] CC_LEVELS = // CC_LEVELS
            {
                    5, 20, 28, 36, 43, 49, 55, 62
            };

    // private static Logger _log = Logger.getLogger(L2PcInstance.class.getName());
    public String getLang() {
        return _lang;
    }

    // В L2PcInstance
    public boolean setLang(String lang) {
        boolean result = false;
        if (Config.MULTILANG_ENABLE) {
            if (Config.MULTILANG_ALLOWED.contains(lang)) {
                _lang = lang;
                result = true;
            } else {
                _lang = Config.MULTILANG_DEFAULT;
            }

            _htmlPrefix = _lang.equals("ru") ? "" : "html-" + _lang + "/";

            // Сохраняем в базу
            storeLangToDB();
        } else {
            _lang = Config.MULTILANG_DEFAULT;
            _htmlPrefix = "";
        }
        return result;
    }

    // Метод для сохранения языка в базе
    public void storeLangToDB() {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE characters SET lang=? WHERE obj_id=?");
            st.setString(1, _lang);
            st.setInt(2, getObjectId());
            st.execute();
        } catch (SQLException e) {
            _log.warning("Could not store language for player " + getName() + ": " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    // public boolean setLang(String lang) {
    // if (Config.MULTILANG_ALLOWED.contains(lang)) {
    // _lang = lang;
    // _htmlPrefix = _lang.equals("en") ? "" : "data/lang/" + _lang + "/";
    // return true;
    // }
    // return false;
    // }
    public String getHtmlPrefix() {
        if (!Config.MULTILANG_ENABLE || _lang == null || _lang.isEmpty() || _lang.equals(Config.MULTILANG_DEFAULT)) {
            // дефолтный язык → без изменений
            return "";
        }

        // если язык не дефолтный → формируем суффикс для замены
        return "-" + _lang;
    }

    // public String getHtmlPrefix() {
    // if (!Config.MULTILANG_ENABLE || _lang == null ||
    // _lang.equals(Config.MULTILANG_DEFAULT)) {
    // return "data/html/"; // дефолтная папка
    // }
    // return "data/html-" + _lang + "/"; // для en, ua и т.д.
    // }

    // public String getHtmlPrefix() {
    // return Config.MULTILANG_ENABLE ? _htmlPrefix : "";
    // }

    public class AIAccessor extends L2Character.AIAccessor {

        protected AIAccessor() {
        }

        public L2PcInstance getPlayer() {
            return L2PcInstance.this;
        }

        public void doPickupItem(L2Object object) {
            L2PcInstance.this.doPickupItem(object);
        }

        public void doInteract(L2Character target) {
            L2PcInstance.this.doInteract(target);
        }

        @Override
        public void doAttack(L2Character target) {
            super.doAttack(target);

            // cancel the recent fake-death protection instantly if the player attacks or
            // casts spells
            getPlayer().setRecentFakeDeath(false);
            for (L2CubicInstance cubic : getCubics().values()) {
                if (cubic.getId() != L2CubicInstance.LIFE_CUBIC) {
                    cubic.doAction(target);
                }
            }
        }

        @Override
        public void doCast(L2Skill skill) {
            super.doCast(skill);

            // cancel the recent fake-death protection instantly if the player attacks or
            // casts spells
            getPlayer().setRecentFakeDeath(false);
            if (skill == null) {
                return;
            }
            if (!skill.isOffensive()) {
                return;
            }
            switch (skill.getTargetType()) {
                case TARGET_SIGNET_GROUND:
                case TARGET_SIGNET:
                    return;

                default: {
                    L2Object mainTarget = skill.getFirstOfTargetList(L2PcInstance.this);
                    if (mainTarget == null || !(mainTarget.isL2Character())) {
                        return;
                    }
                    for (L2CubicInstance cubic : getCubics().values()) {
                        if (cubic.getId() != L2CubicInstance.LIFE_CUBIC) {
                            cubic.doAction((L2Character) mainTarget);
                        }
                    }
                }
                    break;
            }
        }

        /*
         * public L2Summon getSummon() {
         * return getSummon();
         * }
         */
        public boolean isAutoFollow() {
            return getFollowStatus();
        }
    }

    /**
     * Starts battle force / spell force on target.<br>
     * <br>
     *
     * @param caster
     * @param force  type
     */
    @Override
    public void startForceBuff(L2Character target, L2Skill skill) {
        if (!(target.isPlayer())) {
            return;
        }

        if (skill.getSkillType() != SkillType.FORCE_BUFF) {
            return;
        }

        int forceId = 0;
        SystemMessage sm = null;

        if (skill.getId() == 426) {
            forceId = 5104;
        } else {
            forceId = 5105;
        }

        L2Effect force = target.getFirstEffect(forceId);
        if (force != null) {
            int forceLvl = force.getLevel();
            if (forceLvl < 3) {
                int newForceLvl = forceLvl + 1;
                force.exit();
                SkillTable.getInstance().getInfo(forceId, newForceLvl).getEffects(target, target);
                sm = SystemMessage.id(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(newForceLvl);
            } else {
                target.sendUserPacket(Static.YOUR_FORCE_IS_MAX);
            }
        } else {
            SkillTable.getInstance().getInfo(forceId, 1).getEffects(target, target);
            sm = SystemMessage.id(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(1);
        }

        target.sendUserPacket(sm);
        sm = null;
        // if(_forceBuff == null)
        // _forceBuff = new ForceBuff(this, (L2PcInstance)target, skill);
    }

    private L2GameClient _client;
    private boolean _isConnected = true;
    private String _accountName;
    private long _deleteTimer;
    private boolean _isOnline = false;
    private long _onlineTime;
    private long _onlineBeginTime;
    private long _lastAccess;
    private long _uptime;
    protected int _baseClass;
    protected int _activeClass;
    protected int _classIndex = 0;
    /**
     * The list of sub-classes this character has.
     */
    private Map<Integer, SubClass> _subClasses;
    private PcAppearance _appearance;
    /**
     * The Identifier of the L2PcInstance
     */
    private int _charId = 0x00030b7a;
    /**
     * The Experience of the L2PcInstance before the last Death Penalty
     */
    private long _expBeforeDeath;
    /**
     * The Karma of the L2PcInstance (if higher than 0, the name of the
     * L2PcInstance appears in red)
     */
    private int _karma;
    /**
     * The number of player killed during a PvP (the player killed was PvP
     * Flagged)
     */
    private int _pvpKills;
    private int _deaths;
    /**
     * The PK counter of the L2PcInstance (= Number of non PvP Flagged player
     * killed)
     */
    private int _pkKills;
    /**
     * The PvP Flag state of the L2PcInstance (0=White, 1=Purple)
     */
    private byte _pvpFlag;
    /**
     * The Siege state of the L2PcInstance
     */
    private byte _siegeState = 0;
    private int _curWeightPenalty = 0;
    private int _lastCompassZone; // the last compass zone update send to the client
    private byte _zoneValidateCounter = 4;
    private boolean _isIn7sDungeon = false;
    /**
     * Зоны *
     */
    private boolean _isInDangerArea = false;
    private boolean _isInSiegeFlagArea = false;
    private boolean _isInSiegeRuleArea = false;
    private boolean _isInOlumpiadStadium = false;
    private boolean _isInsideCastleWaitZone = false;
    private boolean _isInsideCastleZone = false;
    private boolean _isInsideHotZone = false;
    private boolean _isInsideDismountZone = false;
    private boolean _isInColiseumZone = false;
    private boolean _isInMotherElfZone = false;
    private boolean _isInBlockZone = false;
    private boolean _isInsideAqZone = false;
    private boolean _isInsideSilenceZone = false;
    private boolean _isInZakenZone = false;
    private boolean _InvullBuffs = false;
    private boolean _inJail = false;
    private long _jailTimer = 0;
    private ScheduledFuture<?> _jailTask;
    /**
     * Olympiad
     */
    private boolean _inOlympiadMode = false;
    private boolean _OlympiadStart = false;
    private int _olympiadGameId = -1;
    private int _olympiadSide = -1;
    /**
     * Boat
     */
    private boolean _inBoat;
    private L2BoatInstance _boat;
    private Point3D _inBoatPosition;
    private int _mountType;
    /**
     * Store object used to summon the strider you are mounting *
     */
    private int _mountObjectID = 0;
    public int _telemode = 0;
    public boolean _exploring = false;
    private boolean _isSilentMoving = false;
    private boolean _inCrystallize;
    private boolean _inCraftMode;
    /**
     * The table containing all L2RecipeList of the L2PcInstance
     */
    private Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<Integer, L2RecipeList>();
    private Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<Integer, L2RecipeList>();
    /**
     * True if the L2PcInstance is sitting
     */
    private boolean _waitTypeSitting;
    /**
     * True if the L2PcInstance is using the relax skill
     */
    private boolean _relax;
    /**
     * Location before entering Observer Mode
     */
    private int _obsX;
    private int _obsY;
    private int _obsZ;
    private int _observerMode = 0;
    /**
     * The number of recommandation obtained by the L2PcInstance
     */
    private int _recomHave; // how much I was recommended by others
    /**
     * The number of recommandation that the L2PcInstance can give
     */
    private int _recomLeft; // how many recomendations I can give to others
    /**
     * Date when recom points were updated last time
     */
    private long _lastRecomUpdate;
    /**
     * List with the recomendations that I've give
     */
    private List<Integer> _recomChars = new FastList<Integer>();
    /**
     * The random number of the L2PcInstance
     */
    // private static final Random _rnd = new Random();
    private PcInventory _inventory = new PcInventory(this);
    private PcWarehouse _warehouse;
    private PcFreight _freight = new PcFreight(this);
    /**
     * The Private Store type of the L2PcInstance (PS_NONE=0, PS_SELL=1,
     * sellmanage=2, PS_BUY=3, buymanage=4, PS_MANUFACTURE=5)
     */
    private int _privatestore;
    private TradeList _activeTradeList;
    private ItemContainer _activeWarehouse;
    private L2ManufactureList _createList;
    private TradeList _sellList;
    private TradeList _buyList;
    /**
     * True if the L2PcInstance is newbie
     */
    private boolean _newbie;
    private boolean _noble = false;
    private boolean _hero = false;
    /**
     * The L2FolkInstance corresponding to the last Folk wich one the player
     * talked.
     */
    private L2FolkInstance _lastFolkNpc = null;
    /**
     * Last NPC Id talked on a quest
     */
    private int _questNpcObject = 0;
    /**
     * The table containing all Quests began by the L2PcInstance
     */
    private HashMap<String, QuestState> _quests = new HashMap<String, QuestState>();
    /**
     * The list containing all shortCuts of this L2PcInstance
     */
    private ShortCuts _shortCuts = new ShortCuts(this);
    /**
     * The list containing all macroses of this L2PcInstance
     */
    private MacroList _macroses = new MacroList(this);
    private List<L2PcInstance> _snoopListener = new FastList<L2PcInstance>();
    private List<L2PcInstance> _snoopedPlayer = new FastList<L2PcInstance>();
    private ClassId _skillLearningClassId;
    // hennas
    private final L2HennaInstance[] _henna = new L2HennaInstance[3];
    private int _hennaSTR;
    private int _hennaINT;
    private int _hennaDEX;
    private int _hennaMEN;
    private int _hennaWIT;
    private int _hennaCON;
    /**
     * The L2Summon of the L2PcInstance
     */
    private L2Summon _summon = null;
    // apparently, a L2PcInstance CAN have both a summon AND a tamed beast at the
    // same time!!
    private L2TamedBeastInstance _tamedBeast = null;
    // client radar
    // TODO: This needs to be better intergrated and saved/loaded
    private L2Radar _radar;
    private final StatsChangeRecorder _statsChangeRecorder = new StatsChangeRecorder(this);
    // these values are only stored temporarily
    private boolean _partyMatchingAutomaticRegistration;
    private boolean _partyMatchingShowLevel;
    private boolean _partyMatchingShowClass;
    private String _partyMatchingMemo;
    // Clan related attributes
    /**
     * The Clan Identifier of the L2PcInstance
     */
    private int _clanId;
    /**
     * The Clan object of the L2PcInstance
     */
    private L2Clan _clan;
    /**
     * Apprentice and IDs
     */
    private int _apprentice = 0;
    private int _sponsor = 0;
    private long _clanJoinExpiryTime;
    private long _clanCreateExpiryTime;
    private int _powerGrade = 0;
    private int _clanPrivileges = 0;
    /**
     * L2PcInstance's pledge class (knight, Baron, etc.)
     */
    private int _pledgeClass = 0;
    private int _pledgeType = 0;
    /**
     * Level at which the player joined the clan as an academy member
     */
    private int _lvlJoinedAcademy = 0;
    private int _wantsPeace = 0;
    // Death Penalty Buff Level
    private int _deathPenaltyBuffLevel = 0;
    // WorldPosition used by TARGET_SIGNET_GROUND
    private Point3D _currentSkillWorldPosition;
    // GM related variables
    private boolean _isGm;
    private int _accessLevel;
    // Chat ban
    private boolean _chatBanned = false; // Chat Banned
    private long _banchat_timer = 0;
    private ScheduledFuture _BanChatTask;
    private boolean _messageRefusal = false; // message refusal mode
    private boolean _dietMode = false; // ignore weight penalty
    private boolean _tradeRefusal = false; // Trade refusal
    private boolean _exchangeRefusal = false; // Exchange refusal
    private L2Party _party;
    // this is needed to find the inviting player for Party response
    // there can only be one active party request at once
    private L2PcInstance _activeRequester;
    private long _requestExpireTime = 0;
    private L2ItemInstance _arrowItem;
    //
    private L2PcInstance _currentTransactionRequester;
    public long _currentTransactionTimeout;
    // protects a char from agro mobs when getting up from fake death
    private long _recentFakeDeathEndTime = 0;
    /**
     * The fists L2Weapon of the L2PcInstance (used when no weapon is equiped)
     */
    private L2Weapon _fistsWeaponItem;
    private final HashMap<Integer, String> _chars = new HashMap<Integer, String>();
    // private byte _updateKnownCounter = 0;
    /**
     * The current higher Expertise of the L2PcInstance (None=0, D=1, C=2, B=3,
     * A=4, S=5)
     */
    private int _expertiseIndex; // index in GRADES
    private int _expertisePenalty = 0;
    private L2ItemInstance _activeEnchantItem = null;
    protected boolean _inventoryDisable = false;
    protected Map<Integer, L2CubicInstance> _cubics = new FastMap<Integer, L2CubicInstance>();
    /**
     * Active shots. A FastSet variable would actually suffice but this was
     * changed to fix threading stability...
     */
    // protected Map<Integer, Integer> _activeSoulShots = new FastMap<Integer,
    // Integer>().shared("L2PcInstance._activeSoulShots");
    protected Map<Integer, Integer> _activeSoulShots = new ConcurrentHashMap<Integer, Integer>();
    public final ReentrantLock soulShotLock = new ReentrantLock();
    /**
     * Event parameters
     */
    public int eventX;
    public int eventY;
    public int eventZ;
    public int eventkarma;
    public int eventpvpkills;
    public int eventpkkills;
    public String eventTitle;
    public LinkedList<String> kills = new LinkedList<String>();
    public boolean eventSitForced = false;
    public boolean atEvent = false;
    /**
     * new loto ticket *
     */
    private int _loto[] = new int[5];
    // public static int _loto_nums[] = {0,1,2,3,4,5,6,7,8,9,};
    /**
     * new race ticket *
     */
    private int _race[] = new int[2];
    private final BlockList _blockList = new BlockList();
    private int _team = 0;
    /**
     * lvl of alliance with ketra orcs or varka silenos, used in quests and
     * aggro checks [-5,-1] varka, 0 neutral, [1,5] ketra
     *
     */
    private int _alliedVarkaKetra = 0;
    private L2Fishing _fishCombat;
    private boolean _fishing = false;
    private int _fishx = 0;
    private int _fishy = 0;
    private int _fishz = 0;
    private ScheduledFuture<?> _taskRentPet;
    private ScheduledFuture<?> _taskWater;
    /**
     * Bypass validations
     */
    private List<String> _validBypass = new FastList<String>();
    private List<String> _validBypass2 = new FastList<String>();
    private Forum _forumMail;
    private Forum _forumMemo;
    /**
     * Current skill in use
     */
    private SkillDat _currentSkill;
    /**
     * Skills queued because a skill is already in progress
     */
    private SkillDat _queuedSkill;

    /*
     * Flag to disable equipment/skills while wearing formal wear *
     */
    private boolean _IsWearingFormalWear = false;
    private int _cursedWeaponEquipedId = 0;
    private int _reviveRequested = 0;
    private double _revivePower = 0;
    private boolean _revivePet = false;
    private double _cpUpdateIncCheck = .0;
    private double _cpUpdateDecCheck = .0;
    private double _cpUpdateInterval = .0;
    private double _mpUpdateIncCheck = .0;
    private double _mpUpdateDecCheck = .0;
    private double _mpUpdateInterval = .0;
    private double _enterWorldCp = 0;
    private double _enterWorldHp = 0;
    private double _enterWorldMp = 0;
    /**
     * Herbs Task Time *
     */
    private int _herbstask = 0;

    /**
     * Task for Herbs
     */
    public class HerbTask implements Runnable {

        private String _process;
        private int _itemId;
        private int _count;
        private L2Object _reference;
        private boolean _sendMessage;

        HerbTask(String process, int itemId, int count, L2Object reference, boolean sendMessage) {
            _process = process;
            _itemId = itemId;
            _count = count;
            _reference = reference;
            _sendMessage = sendMessage;
        }

        public void run() {
            try {
                addItem(_process, _itemId, _count, _reference, _sendMessage);
            } catch (Throwable t) {
                _log.log(Level.WARNING, "", t);
            }
        }
    }

    // L2JMOD Wedding
    private boolean _married = false;
    private int _partnerId = 0;
    private int _coupleId = 0;
    private boolean _engagerequest = false;
    private int _engageid = 0;
    private boolean _marryrequest = false;
    private boolean _marryaccepted = false;
    // Current force buff this caster is casting to a target
    protected ForceBuff _forceBuff;

    /**
     * Skill casting information (used to queue when several skills are cast in
     * a short time) *
     */
    public static class SkillDat {

        private L2Skill _skill;
        private boolean _ctrlPressed;
        private boolean _shiftPressed;

        protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed) {
            _skill = skill;
            _ctrlPressed = ctrlPressed;
            _shiftPressed = shiftPressed;
        }

        public boolean isCtrlPressed() {
            return _ctrlPressed;
        }

        public boolean isShiftPressed() {
            return _shiftPressed;
        }

        public L2Skill getSkill() {
            return _skill;
        }

        public int getSkillId() {
            return (getSkill() != null) ? getSkill().getId() : -1;
        }
    }

    /**
     * Create a new L2PcInstance and add it in the characters table of the
     * database.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Create a new L2PcInstance with an
     * account name</li>
     * <li>Set the name, the Hair Style, the Hair Color and
     * the Face type of the L2PcInstance</li>
     * <li>Add the player in the
     * characters table of the database</li><BR>
     * <BR>
     *
     * @param objectId    Identifier of the object to initialized
     * @param template    The L2PcTemplate to apply to the L2PcInstance
     * @param accountName The name of the L2PcInstance
     * @param name        The name of the L2PcInstance
     * @param hairStyle   The hair style Identifier of the L2PcInstance
     * @param hairColor   The hair color Identifier of the L2PcInstance
     * @param face        The face type Identifier of the L2PcInstance
     *
     * @return The L2PcInstance added to the database or null
     *
     */
    public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName, String name,
            byte hairStyle, byte hairColor, byte face, boolean sex) {
        // Create a new L2PcInstance with an account name
        PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
        L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);

        // Set the name of the L2PcInstance
        player.setName(name);

        // Set the base class ID to that of the actual class ID.
        player.setBaseClass(player.getClassId());

        if (Config.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE) {
            player.setNewbie(true);
        }

        if (Config.ALT_GAME_NEW_CHAR_ALWAYS_IS_NOBLE) {
            player.setNoble(true);
        }

        // Add the player in the characters table of the database
        boolean ok = player.createDb();

        // уровень при старте
        player.addExpAndSp(Experience.LEVEL[Config.ALT_START_LEVEL] - player.getExp(), Config.ALT_START_SP);

        if (!Config.CUSTOM_STRT_ITEMS.isEmpty()) {
            for (FastMap.Entry<Integer, Integer> e = Config.CUSTOM_STRT_ITEMS.head(),
                    end = Config.CUSTOM_STRT_ITEMS.tail(); (e = e.getNext()) != end;) {
                Integer item_id = e.getKey();
                Integer item_count = e.getValue();
                if (item_id == null || item_count == null) {
                    continue;
                }

                player.getInventory().addItem("start_items", item_id, item_count, player, null);
            }
        }

        if (!ok) {
            return null;
        }

        return player;
    }

    public static L2PcInstance createDummyPlayer(int objectId, String name) {
        // Create a new L2PcInstance with an account name
        L2PcInstance player = new L2PcInstance(objectId);
        player.setName(name);

        return player;
    }

    private String _maskName = "";
    private boolean _showMaskName = false;

    @Override
    public String getName() {
        if (_showMaskName) {
            return _maskName;
        }

        return getName(super.getName());
    }

    private String getName(String realName) {
        if (isInGuild()) {
            realName = Config.GUILD_MOD_NAMES.get(getGuildSide()) + realName;
        }
        if (isPremium()) {
            realName += Config.PREMIUM_NAME_PREFIX;
        }

        return realName;
    }

    public String getRealName() {
        return super.getName();
    }

    public String getAccountName() {
        if (getClient() == null) {
            return "N/A";
        }

        return getClient().getAccountName();
    }

    public HashMap<Integer, String> getAccountChars() {
        return _chars;
    }

    @Override
    public int getRelation(L2PcInstance target) {
        int result = 0;

        // karma and pvp may not be required
        if (getPvpFlag() != 0) {
            result |= RelationChanged.RELATION_PVP_FLAG;
        }
        if (getKarma() > 0) {
            result |= RelationChanged.RELATION_HAS_KARMA;
        }

        if (isClanLeader()) {
            result |= RelationChanged.RELATION_LEADER;
        }

        if (getSiegeState() != 0) {
            result |= RelationChanged.RELATION_INSIEGE;
            if (getSiegeState() != target.getSiegeState()) {
                result |= RelationChanged.RELATION_ENEMY;
            } else {
                result |= RelationChanged.RELATION_ALLY;
            }
            if (getSiegeState() == 1) {
                result |= RelationChanged.RELATION_ATTACKER;
            }
        }

        if (getClan() != null && target.getClan() != null) {
            if (target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY
                    && target.getClan().isAtWarWith(getClan().getClanId())) {
                result |= RelationChanged.RELATION_1SIDED_WAR;
                if (getClan().isAtWarWith(target.getClan().getClanId())) {
                    result |= RelationChanged.RELATION_MUTUAL_WAR;
                }
            }
        }
        return result;
    }

    /**
     * Retrieve a L2PcInstance from the characters table of the database and add
     * it in _allObjects of the L2world (call restore method).<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Retrieve the L2PcInstance from the
     * characters table of the database</li>
     * <li>Add the L2PcInstance object in
     * _allObjects</li>
     * <li>Set the x,y,z position of the L2PcInstance and make
     * it invisible</li>
     * <li>Update the overloaded status of the
     * L2PcInstance</li><BR>
     * <BR>
     *
     * @param objectId Identifier of the object to initialized
     *
     * @return The L2PcInstance loaded from the database
     *
     */
    public static L2PcInstance load(int objectId) {
        return restore(objectId);
    }

    private void initPcStatusUpdateValues() {
        _cpUpdateInterval = getMaxCp() / 352.0;
        _cpUpdateIncCheck = getMaxCp();
        _cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
        _mpUpdateInterval = getMaxMp() / 352.0;
        _mpUpdateIncCheck = getMaxMp();
        _mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
    }

    /**
     * Constructor of L2PcInstance (use L2Character constructor).<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Call the L2Character constructor to
     * create an empty _skills slot and copy basic Calculator set to this
     * L2PcInstance</li>
     * <li>Set the name of the L2PcInstance</li><BR>
     * <BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the
     * L2PcInstance to 1</B></FONT><BR>
     * <BR>
     *
     * @param objectId    Identifier of the object to initialized
     * @param template    The L2PcTemplate to apply to the L2PcInstance
     * @param accountName The name of the account including this L2PcInstance
     *
     */
    public L2PcInstance(int objectId, L2PcTemplate template) {
        super(objectId, template);
    }

    private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app) {
        super(objectId, template);
        getKnownList(); // init knownlist
        getStat(); // init stats
        getStatus(); // init status
        super.initCharStatusUpdateValues();
        initPcStatusUpdateValues();

        _accountName = accountName;
        _appearance = app;

        // Create an AI
        _ai = new L2PlayerAI(new L2PcInstance.AIAccessor());

        // Create a L2Radar object
        _radar = new L2Radar(this);

        // Retrieve from the database all skills of this L2PcInstance and add them to
        // _skills
        // Retrieve from the database all items of this L2PcInstance and add them to
        // _inventory
        getInventory().restore();
        if (!Config.WAREHOUSE_CACHE) {
            getWarehouse();
        }
        getFreight().restore();
    }

    private L2PcInstance(int objectId) {
        super(objectId, null);
        getKnownList(); // init knownlist
        getStat(); // init stats
        getStatus(); // init status
        super.initCharStatusUpdateValues();
        initPcStatusUpdateValues();
    }

    @Override
    public final PcKnownList getKnownList() {
        if (super.getKnownList() == null || !(super.getKnownList() instanceof PcKnownList)) {
            setKnownList(new PcKnownList(this));
        }
        return (PcKnownList) super.getKnownList();
    }

    @Override
    public final PcStat getStat() {
        if (super.getStat() == null || !(super.getStat() instanceof PcStat)) {
            setStat(new PcStat(this));
        }
        return (PcStat) super.getStat();
    }

    @Override
    public final PcStatus getStatus() {
        if (super.getStatus() == null || !(super.getStatus() instanceof PcStatus)) {
            setStatus(new PcStatus(this));
        }
        return (PcStatus) super.getStatus();
    }

    public void setPcAppearance(PcAppearance app) {
        _appearance = app;
    }

    public final PcAppearance getAppearance() {
        return _appearance;
    }

    /**
     * Return the base L2PcTemplate link to the L2PcInstance.<BR>
     * <BR>
     */
    public final L2PcTemplate getBaseTemplate() {
        return CharTemplateTable.getInstance().getTemplate(_baseClass);
    }

    /**
     * Return the L2PcTemplate link to the L2PcInstance.
     */
    @Override
    public final L2PcTemplate getTemplate() {
        return (L2PcTemplate) super.getTemplate();
    }

    public void setTemplate(ClassId newclass) {
        super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
    }

    /**
     * Return the AI of the L2PcInstance (create it if necessary).<BR>
     * <BR>
     */
    @Override
    public L2CharacterAI getAI() {
        // synchronized(this)
        // {
        if (_ai == null) {
            // _ai = new L2PlayerAI(new L2PcInstance.AIAccessor());

            if (_fantome) {
                if (_isPartner) {
                    _ai = new L2PartnerAI(new L2PcInstance.AIAccessor());
                    return _ai;
                }
                switch (getClassId().getId()) {
                    case 92:
                    case 102:
                    case 109:
                        _ai = new L2PlayerFakeArcherAI(new L2PcInstance.AIAccessor());
                        break;
                    default:
                        _ai = new L2PlayerFakeAI(new L2PcInstance.AIAccessor());
                        break;
                }
            } else {
                _ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
            }
        }
        // }

        return _ai;
    }

    /**
     * Calculate a destination to explore the area and set the AI Intension to
     * AI_INTENTION_MOVE_TO.<BR>
     * <BR>
     */
    public void explore() {
        if (!_exploring) {
            return;
        }

        if (getMountType() == 2) {
            return;
        }

        // Calculate the destination point (random)
        int x = getX() + Rnd.nextInt(6000) - 3000;
        int y = getY() + Rnd.nextInt(6000) - 3000;

        /*
         * if (x > Universe.MAX_X) x = Universe.MAX_X; if (x < Universe.MIN_X) x
         * = Universe.MIN_X; if (y > Universe.MAX_Y) y = Universe.MAX_Y; if (y <
         * Universe.MIN_Y) y = Universe.MIN_Y;
         */
        // Set the AI Intention to AI_INTENTION_MOVE_TO
        getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(x, y, getZ(), calcHeading(x, y)));

    }

    /**
     * Return the Level of the L2PcInstance.
     */
    @Override
    public final int getLevel() {
        return getStat().getLevel();
    }

    /**
     * Return the _newbie state of the L2PcInstance.<BR>
     * <BR>
     */
    public boolean isNewbie() {
        return _newbie;
    }

    /**
     * Set the _newbie state of the L2PcInstance.<BR>
     * <BR>
     *
     * @param isNewbie The Identifier of the _newbie state<BR>
     *                 <BR>
     *
     */
    public void setNewbie(boolean f) {
        _newbie = f;
    }

    public void setBaseClass(int baseClass) {
        _baseClass = baseClass;
    }

    public void setBaseClass(ClassId classId) {
        _baseClass = classId.ordinal();
    }

    public boolean isInStoreMode() {
        return (getPrivateStoreType() > 0);
    }
    // public boolean isInCraftMode() { return (getPrivateStoreType() ==
    // PS_MANUFACTURE); }

    public boolean isInCraftMode() {
        return _inCraftMode;
    }

    public void isInCraftMode(boolean b) {
        _inCraftMode = b;
    }

    /**
     * Manage Logout Task.<BR>
     * <BR>
     */
    @Override
    public void logout() {
        closeNetConnection();
    }

    @Override
    public void kick() {
        kick(false);
    }

    public void kick(boolean force) {
        if (!force && _isDeleting) {
            return;
        }

        // if (_offline)
        setOfflineMode(false);

        sendUserPacket(Static.ServerClose);
        deleteMe();
        _client = null;
        setConnected(false);
        broadcastUserInfo();
    }

    /**
     * Return a table containing all Common L2RecipeList of the
     * L2PcInstance.<BR>
     * <BR>
     */
    public L2RecipeList[] getCommonRecipeBook() {
        if (_commonRecipeBook == null) {
            return new L2RecipeList[0];
        }

        return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
    }

    /**
     * Return a table containing all Dwarf L2RecipeList of the
     * L2PcInstance.<BR>
     * <BR>
     */
    public L2RecipeList[] getDwarvenRecipeBook() {
        return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
    }

    /**
     * Add a new L2RecipList to the table _commonrecipebook containing all
     * L2RecipeList of the L2PcInstance <BR>
     * <BR>
     *
     * @param recipe The L2RecipeList to add to the _recipebook
     *
     */
    public void registerCommonRecipeList(L2RecipeList recipe) {
        _commonRecipeBook.put(recipe.getId(), recipe);
    }

    /**
     * Add a new L2RecipList to the table _recipebook containing all
     * L2RecipeList of the L2PcInstance <BR>
     * <BR>
     *
     * @param recipe The L2RecipeList to add to the _recipebook
     *
     */
    public void registerDwarvenRecipeList(L2RecipeList recipe) {
        _dwarvenRecipeBook.put(recipe.getId(), recipe);
    }

    /**
     * @param RecipeID The Identifier of the L2RecipeList to check in the
     *                 player's recipe books
     *
     * @return <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe
     *         book else returns <b>FALSE</b>
     */
    public boolean hasRecipeList(int recipeId) {
        if (_dwarvenRecipeBook.containsKey(recipeId)) {
            return true;
        } else if (_commonRecipeBook.containsKey(recipeId)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from
     * table _CommonRecipeBook, those table contain all L2RecipeList of the
     * L2PcInstance <BR>
     * <BR>
     *
     * @param RecipeID The Identifier of the L2RecipeList to remove from the
     *                 _recipebook
     *
     */
    public void unregisterRecipeList(int recipeId) {
        if (_dwarvenRecipeBook.containsKey(recipeId)) {
            _dwarvenRecipeBook.remove(recipeId);
        } else if (_commonRecipeBook.containsKey(recipeId)) {
            _commonRecipeBook.remove(recipeId);
        } else {
            _log.warning("Attempted to remove unknown RecipeList: " + recipeId);
        }

        /*
         * FastTable <L2ShortCut> allShortCuts = new FastTable<L2ShortCut>();
         * allShortCuts.addAll(getAllShortCuts());
         *
         * for (int i = 0, n = allShortCuts.size(); i < n; i++) { L2ShortCut sc
         * = allShortCuts.get(i); if (sc == null) continue;
         *
         * if (sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE)
         * deleteShortCut(sc.getSlot(), sc.getPage()); } allShortCuts.clear();
         * allShortCuts = null;
         */
        for (L2ShortCut sc : getAllShortCuts()) {
            if (sc == null) {
                continue;
            }

            if (sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE) {
                deleteShortCut(sc.getSlot(), sc.getPage());
            }
        }
    }

    /**
     * Returns the Id for the last talked quest NPC.<BR>
     * <BR>
     */
    public int getLastQuestNpcObject() {
        return _questNpcObject;
    }

    public void setLastQuestNpcObject(int npcId) {
        _questNpcObject = npcId;
    }

    /**
     * Return the QuestState object corresponding to the quest name.<BR>
     * <BR>
     *
     * @param quest The name of the quest
     *
     */
    public QuestState getQuestState(String quest) {
        return _quests.get(quest);
    }

    /**
     * Add a QuestState to the table _quest containing all quests began by the
     * L2PcInstance.<BR>
     * <BR>
     *
     * @param qs The QuestState to add to _quest
     *
     */
    public void setQuestState(QuestState qs) {
        _quests.put(qs.getQuestName(), qs);
    }

    /**
     * Remove a QuestState from the table _quest containing all quests began by
     * the L2PcInstance.<BR>
     * <BR>
     *
     * @param quest The name of the quest
     *
     */
    public void delQuestState(String quest) {
        _quests.remove(quest);
    }

    private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state) {
        int len = questStateArray.length;
        QuestState[] tmp = new QuestState[len + 1];
        for (int i = 0; i < len; i++) {
            tmp[i] = questStateArray[i];
        }
        tmp[len] = state;
        return tmp;
    }

    /**
     * Return a table containing all Quest in progress from the table
     * _quests.<BR>
     * <BR>
     */
    public Quest[] getAllActiveQuests() {
        ArrayList<Quest> quests = new ArrayList<Quest>();

        for (QuestState qs : _quests.values()) {
            if (qs.getQuest().getQuestIntId() >= 1999) {
                continue;
            }

            if (qs.isCompleted() && !Config.DEVELOPER) {
                continue;
            }

            if (!qs.isStarted() && !Config.DEVELOPER) {
                continue;
            }

            quests.add(qs.getQuest());
        }

        return quests.toArray(new Quest[quests.size()]);
    }

    /**
     * Return a table containing all QuestState to modify after a L2Attackable
     * killing.<BR>
     * <BR>
     *
     * @param npcId The Identifier of the L2Attackable attacked
     *
     */
    public QuestState[] getQuestsForAttacks(L2NpcInstance npc) {
        // Create a QuestState table that will contain all QuestState to modify
        QuestState[] states = null;

        // Go through the QuestState of the L2PcInstance quests
        for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.MOBGOTATTACKED)) {
            // Check if the Identifier of the L2Attackable attck is needed for the current
            // quest
            if (getQuestState(quest.getName()) != null) {
                // Copy the current L2PcInstance QuestState in the QuestState table
                if (states == null) {
                    states = new QuestState[] { getQuestState(quest.getName()) };
                } else {
                    states = addToQuestStateArray(states, getQuestState(quest.getName()));
                }
            }
        }

        // Return a table containing all QuestState to modify
        return states;
    }

    /**
     * Return a table containing all QuestState to modify after a L2Attackable
     * killing.<BR>
     * <BR>
     *
     * @param npcId The Identifier of the L2Attackable killed
     *
     */
    public QuestState[] getQuestsForKills(L2NpcInstance npc) {
        // Create a QuestState table that will contain all QuestState to modify
        QuestState[] states = null;

        // Go through the QuestState of the L2PcInstance quests
        for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.MOBKILLED)) {
            // Check if the Identifier of the L2Attackable killed is needed for the current
            // quest
            if (getQuestState(quest.getName()) != null) {
                // Copy the current L2PcInstance QuestState in the QuestState table
                if (states == null) {
                    states = new QuestState[] { getQuestState(quest.getName()) };
                } else {
                    states = addToQuestStateArray(states, getQuestState(quest.getName()));
                }
            }
        }

        // Return a table containing all QuestState to modify
        return states;
    }

    /**
     * Return a table containing all QuestState from the table _quests in which
     * the L2PcInstance must talk to the NPC.<BR>
     * <BR>
     *
     * @param npcId The Identifier of the NPC
     *
     */
    public QuestState[] getQuestsForTalk(int npcId) {
        // Create a QuestState table that will contain all QuestState to modify
        QuestState[] states = null;

        // Go through the QuestState of the L2PcInstance quests
        Quest[] quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(Quest.QuestEventType.QUEST_TALK);
        if (quests != null) {
            for (Quest quest : quests) {
                if (quest != null) {
                    // Copy the current L2PcInstance QuestState in the QuestState table
                    if (getQuestState(quest.getName()) != null) {
                        if (states == null) {
                            states = new QuestState[] { getQuestState(quest.getName()) };
                        } else {
                            states = addToQuestStateArray(states, getQuestState(quest.getName()));
                        }
                    }
                }
            }
        }

        // Return a table containing all QuestState to modify
        return states;
    }

    public QuestState processQuestEvent(String quest, String event) {
        QuestState retval = null;
        if (event == null) {
            event = "";
        }
        if (!_quests.containsKey(quest)) {
            return retval;
        }
        QuestState qs = getQuestState(quest);
        if (qs == null && event.length() == 0) {
            return retval;
        }
        if (qs == null) {
            Quest q = QuestManager.getInstance().getQuest(quest);
            if (q == null) {
                return retval;
            }
            qs = q.newQuestState(this);
        }
        if (qs != null) {
            if (getLastQuestNpcObject() > 0) {
                L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
                if (object == null) {
                    return retval;
                }

                if (object.isL2Npc() && isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false)) {
                    L2NpcInstance npc = (L2NpcInstance) object; // TODO: прибить
                    QuestState[] states = getQuestsForTalk(npc.getNpcId());

                    if (states != null) {
                        for (QuestState state : states) {
                            if ((state.getQuest().getQuestIntId() == qs.getQuest().getQuestIntId())
                                    && !qs.isCompleted()) {
                                if (qs.getQuest().notifyEvent(event, npc, this)) {
                                    showQuestWindow(quest, qs.getStateId());
                                }

                                retval = qs;
                            }
                        }
                        sendUserPacket(new QuestList(this));
                    }
                }
            }
        }

        return retval;
    }

    private void showQuestWindow(String questId, String stateId) {
        String content = HtmCache.getInstance().getHtm("data/jscript/quests/" + questId + "/" + stateId + ".htm"); // TODO
                                                                                                                   // path
                                                                                                                   // for
                                                                                                                   // quests
                                                                                                                   // html
        if (content == null) {
            content = HtmCache.getInstance().getHtm("data/scripts/quests/" + questId + "/" + stateId + ".htm"); // TODO
                                                                                                                // path
                                                                                                                // for
                                                                                                                // quests
                                                                                                                // html
        }
        if (content != null) {
            NpcHtmlMessage npcReply = NpcHtmlMessage.id(5);
            npcReply.setHtml(content);
            sendUserPacket(npcReply);
        }

        sendActionFailed();
    }

    /**
     * Return a table containing all L2ShortCut of the L2PcInstance.<BR>
     * <BR>
     */
    public ShortCuts getShortCuts() {
        return _shortCuts;
    }

    public FastTable<L2ShortCut> getAllShortCuts() {
        return _shortCuts.getAllShortCuts();
    }

    /**
     * Return the L2ShortCut of the L2PcInstance corresponding to the position
     * (page-slot).<BR>
     * <BR>
     *
     * @param slot The slot in wich the shortCuts is equiped
     * @param page The page of shortCuts containing the slot
     *
     */
    public L2ShortCut getShortCut(int slot, int page) {
        return _shortCuts.getShortCut(slot, page);
    }

    /**
     * Add a L2shortCut to the L2PcInstance _shortCuts<BR>
     * <BR>
     */
    public void registerShortCut(L2ShortCut shortcut) {
        _shortCuts.registerShortCut(shortcut);
    }

    /**
     * Delete the L2ShortCut corresponding to the position (page-slot) from the
     * L2PcInstance _shortCuts.<BR>
     * <BR>
     */
    public void deleteShortCut(int slot, int page) {
        _shortCuts.deleteShortCut(slot, page);
    }

    /**
     * Add a L2Macro to the L2PcInstance _macroses<BR>
     * <BR>
     */
    public void registerMacro(L2Macro macro) {
        _macroses.registerMacro(macro);
    }

    /**
     * Delete the L2Macro corresponding to the Identifier from the L2PcInstance
     * _macroses.<BR>
     * <BR>
     */
    public void deleteMacro(int id) {
        _macroses.deleteMacro(id);
    }

    /**
     * Return all L2Macro of the L2PcInstance.<BR>
     * <BR>
     */
    public MacroList getMacroses() {
        return _macroses;
    }

    /**
     * Set the siege state of the L2PcInstance.<BR>
     * <BR>
     * 1 = attacker, 2 =
     * defender, 0 = not involved
     */
    public void setSiegeState(byte siegeState) {
        _siegeState = siegeState;
    }

    /**
     * Get the siege state of the L2PcInstance.<BR>
     * <BR>
     * 1 = attacker, 2 =
     * defender, 0 = not involved
     */
    public byte getSiegeState() {
        return _siegeState;
    }

    /**
     * Set the PvP Flag of the L2PcInstance.<BR>
     * <BR>
     */
    public void setPvpFlag(int pvpFlag) {
        _pvpFlag = (byte) pvpFlag;
    }

    @Override
    public byte getPvpFlag() {
        if (Config.FREE_PVP) {
            return 1;
        }
        return _pvpFlag;
    }

    @Override
    public void updatePvPFlag(int value) {
        if (Config.FREE_PVP || getPvpFlag() == value) {
            return;
        }
        setPvpFlag(value);

        sendUserPacket(new UserInfo(this));
        // sendPacket(new ExBrExtraUserInfo(this));

        // If this player has a pet update the pets pvp flag as well
        if (getPet() != null) {
            sendUserPacket(new RelationChanged(getPet(), getRelation(this), false));
        }

        FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
        L2PcInstance pc = null;
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }

            pc.sendPacket(new RelationChanged(this, getRelation(this), isAutoAttackable(pc)));
            if (getPet() != null) {
                pc.sendPacket(new RelationChanged(getPet(), getRelation(this), isAutoAttackable(pc)));
            }
        }
        players.clear();
        players = null;
        pc = null;
    }

    private Duel _duel;

    public void setDuel(final Duel duel) {
        _duel = duel;
        broadcastPacket(new RelationChanged(this, getRelation(this), false));
    }

    @Override
    public Duel getDuel() {
        return _duel;
    }

    @Override
    public boolean isInDuel() {
        return _duel != null;
    }

    @Override
    public void revalidateZone(boolean f) {
        // Cannot validate if not in a world region (happens during teleport)
        if (getWorldRegion() == null) {
            return;
        }

        // This function is called very often from movement code
        if (f) {
            _zoneValidateCounter = 4;
        } else {
            _zoneValidateCounter--;
            if (_zoneValidateCounter < 0) {
                _zoneValidateCounter = 4;
            } else {
                return;
            }
        }

        getWorldRegion().revalidateZones(this);

        if (isInsideZone(ZONE_SIEGE)) {
            if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) {
                return;
            }
            _lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
            sendUserPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2));
        } else if (isInsidePvpZone()) {
            if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE) {
                return;
            }
            _lastCompassZone = ExSetCompassZoneCode.PVPZONE;
            sendUserPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE));
        } else if (isIn7sDungeon()) {
            if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE) {
                return;
            }
            _lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
            sendUserPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE));
        } else if (isInZonePeace()) {
            if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE) {
                return;
            }
            _lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
            sendUserPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE));
        } else {
            if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE) {
                return;
            }
            if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) {
                updatePvPStatus();
            }
            _lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
            sendUserPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE));
        }
    }

    /**
     * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR>
     * <BR>
     */
    public boolean hasDwarvenCraft() {
        return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
    }

    public int getDwarvenCraft() {
        return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
    }

    /**
     * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR>
     * <BR>
     */
    public boolean hasCommonCraft() {
        return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
    }

    public int getCommonCraft() {
        return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
    }

    /**
     * Return the PK counter of the L2PcInstance.<BR>
     * <BR>
     */
    public int getPkKills() {
        return _pkKills;
    }

    /**
     * Set the PK counter of the L2PcInstance.<BR>
     * <BR>
     */
    public void setPkKills(int pkKills) {
        _pkKills = pkKills;
    }

    /**
     * Return the _deleteTimer of the L2PcInstance.<BR>
     * <BR>
     */
    public long getDeleteTimer() {
        return _deleteTimer;
    }

    /**
     * Set the _deleteTimer of the L2PcInstance.<BR>
     * <BR>
     */
    public void setDeleteTimer(long deleteTimer) {
        _deleteTimer = deleteTimer;
    }

    /**
     * Return the current weight of the L2PcInstance.<BR>
     * <BR>
     */
    public int getCurrentLoad() {
        return _inventory.getTotalWeight();
    }

    /**
     * Return date of las update of recomPoints
     */
    public long getLastRecomUpdate() {
        return _lastRecomUpdate;
    }

    public void setLastRecomUpdate(long date) {
        _lastRecomUpdate = date;
    }

    /**
     * Return the number of recommandation obtained by the L2PcInstance.<BR>
     * <BR>
     */
    public int getRecomHave() {
        return _recomHave;
    }

    /**
     * Increment the number of recommandation obtained by the L2PcInstance (Max
     * : 255).<BR>
     * <BR>
     */
    protected void incRecomHave() {
        if (_recomHave < 255) {
            _recomHave++;
        }
    }

    /**
     * Set the number of recommandation obtained by the L2PcInstance (Max :
     * 255).<BR>
     * <BR>
     */
    public void setRecomHave(int value) {
        if (value > 255) {
            _recomHave = 255;
        } else if (value < 0) {
            _recomHave = 0;
        } else {
            _recomHave = value;
        }
    }

    /**
     * Return the number of recommandation that the L2PcInstance can
     * give.<BR>
     * <BR>
     */
    public int getRecomLeft() {
        return _recomLeft;
    }

    /**
     * Increment the number of recommandation that the L2PcInstance can
     * give.<BR>
     * <BR>
     */
    protected void decRecomLeft() {
        if (_recomLeft > 0) {
            _recomLeft--;
        }
    }

    public void giveRecom(L2PcInstance target) {
        if (Config.ALT_RECOMMEND) {
            Connect con = null;
            PreparedStatement st = null;
            try {
                con = L2DatabaseFactory.get();
                st = con.prepareStatement("INSERT INTO character_recommends (char_id,target_id) VALUES (?,?)");
                st.setInt(1, getObjectId());
                st.setInt(2, target.getObjectId());
                st.execute();
            } catch (Exception e) {
                _log.warning("could not update char recommendations:" + e);
            } finally {
                Close.CS(con, st);
            }
        }
        target.incRecomHave();
        decRecomLeft();
        _recomChars.add(target.getObjectId());
    }

    public boolean canRecom(L2PcInstance target) {
        return !_recomChars.contains(target.getObjectId());
    }

    /**
     * Set the exp of the L2PcInstance before a death
     *
     * @param exp
     */
    public void setExpBeforeDeath(long exp) {
        _expBeforeDeath = exp;
    }

    public long getExpBeforeDeath() {
        return _expBeforeDeath;
    }

    /**
     * Return the Karma of the L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public int getKarma() {
        if (Config.FREE_PVP) {
            return 0;
        }
        return _karma;
    }

    /**
     * Set the Karma of the L2PcInstance and send a Server->Client packet
     * StatusUpdate (broadcast).<BR>
     * <BR>
     */
    public void setKarma(int karma) {
        if (_karma == karma) {
            return;
        }

        if (_partner != null) {
            _partner.setKarma(karma);
        }
        if (_isPartner && _owner != null) {
            _owner.setKarma(karma);
        }

        _karma = karma;

        if (_karma > 0) {
            setKarmaFlag(1);
            /*
             * for (L2Object object : getKnownList().getKnownObjects().values()) {
             * if (object == null || !(object.isL2Guard())) {
             * continue;
             * }
             * 
             * if (((L2GuardInstance) object).getAI().getIntention() ==
             * CtrlIntention.AI_INTENTION_IDLE) {
             * ((L2GuardInstance)
             * object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
             * }
             * }
             */
        } else {
            _karma = 0;
            // Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the
            // L2PcInstance and all L2PcInstance to inform (broadcast)
            if (getPvpFlag() != 0) {
                setPvpFlag(0);
            }
            setKarmaFlag(0);
        }

        // broadcastKarma();
        sendChanges();
        sendUserPacket(
                SystemMessage.id(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO).addString(String.valueOf(_karma)));

        if (getPet() != null) {
            sendUserPacket(new RelationChanged(getPet(), getRelation(this), isAutoAttackable(this)));
        }
    }

    /**
     * Return the max weight that the L2PcInstance can load.<BR>
     * <BR>
     */
    public int getMaxLoad() {
        // Weight Limit = (CON Modifier*69000)*Skills
        // Source http://l2p.bravehost.com/weightlimit.html (May 2007)
        // Fitted exponential curve to the data
        int con = getCON();
        if (con < 1) {
            return 31000;
        }
        if (con > 59) {
            return 176000;
        }
        double baseLoad = Math.pow(1.029993928, con) * 30495.627366;
        return (int) calcStat(Stats.MAX_LOAD, baseLoad * Config.ALT_WEIGHT_LIMIT, this, null);
    }

    public int getExpertisePenalty() {
        return _expertisePenalty;
    }

    public int getWeightPenalty() {
        if (_dietMode) {
            return 0;
        }
        return _curWeightPenalty;
    }

    /**
     * Update the overloaded status of the L2PcInstance.<BR>
     * <BR>
     */
    public void refreshOverloaded() {
        int maxLoad = getMaxLoad();
        if (maxLoad > 0) {
            setIsOverloaded(getCurrentLoad() > maxLoad);
            int weightproc = getCurrentLoad() * 1000 / maxLoad;
            int newWeightPenalty;
            if (weightproc < 500 || _dietMode) {
                newWeightPenalty = 0;
            } else if (weightproc < 666) {
                newWeightPenalty = 1;
            } else if (weightproc < 800) {
                newWeightPenalty = 2;
            } else if (weightproc < 1000) {
                newWeightPenalty = 3;
            } else {
                newWeightPenalty = 4;
            }

            if (_curWeightPenalty != newWeightPenalty) {
                _curWeightPenalty = newWeightPenalty;
                if (newWeightPenalty > 0 && !_dietMode) {
                    super.addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
                } else {
                    super.removeSkill(getKnownSkill(4270));
                }

                sendEtcStatusUpdate();
                // Broadcast.toKnownPlayers(this, new CharInfo(this));
            }
        }
        _farmDelay = AntiFarm.create(this);
    }

    private boolean _classUpdate = false;

    public void setClassUpdate(boolean flag) {
        _classUpdate = flag;
    }

    public void refreshExpertisePenalty() {
        if (_classUpdate) {
            return;
        }

        int newPenalty = 0;

        for (L2ItemInstance item : getInventory().getItems()) {
            if (item != null && item.isEquipped()) {
                int crystaltype = item.getItem().getCrystalType();

                if (crystaltype > newPenalty) {
                    newPenalty = crystaltype;
                }
            }
        }

        newPenalty = newPenalty - getExpertiseIndex();

        if (newPenalty <= 0) {
            newPenalty = 0;
        }

        if (getExpertisePenalty() != newPenalty) {
            _expertisePenalty = newPenalty;

            if (newPenalty > 0) {
                super.addSkill(SkillTable.getInstance().getInfo(4267, 1)); // level used to be newPenalty
            } else {
                super.removeSkill(getKnownSkill(4267));
            }

            sendEtcStatusUpdate();
        }
    }

    public void checkIfWeaponIsAllowed() {
        // Override for Gamemasters
        if (isGM()) {
            return;
        }

        // Iterate through all effects currently on the character.
        FastTable<L2Effect> effects = getAllEffectsTable();
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect effect = effects.get(i);
            if (effect == null) {
                continue;
            }

            L2Skill effectSkill = effect.getSkill();
            // Ignore all buff skills that are party related (ie. songs, dances) while still
            // remaining weapon dependant on cast though.
            if (!effectSkill.isOffensive() && !(effectSkill.getTargetType() == SkillTargetType.TARGET_PARTY
                    && effectSkill.getSkillType() == SkillType.BUFF)) {
                // Check to rest to assure current effect meets weapon requirements.
                if (!effectSkill.getWeaponDependancy(this)) {
                    // sendPacket(effectSkill.getName() + " cannot be used with this weapon.");
                    sendUserPacket(Static.WRONG_WEAPON);

                    // if (Config.DEBUG)
                    // _log.info(" | Skill "+effectSkill.getName()+" has been disabled for
                    // ("+getName()+"); Reason: Incompatible Weapon Type.");
                    effect.exit();
                }
            }
            // continue;
        }
    }

    public void useEquippableItem(L2ItemInstance item, boolean abortAttack) {
        if (isCastingNow()) {
            return;
        }

        if (item.getItem().isShield()) {
            if (isCursedWeaponEquiped()) {
                return;
            }
            stopSkillEffects(350);
        }

        if (CustomServerData.getInstance().isSpecialForrbid(_forbItem, item.getItemId())) {
            return;
        }

        // Equip or unEquip
        SystemMessage sm = null;
        L2ItemInstance[] items = null;

        if (item.isEquipped()) {
            if (item.getEnchantLevel() > 0) {
                sm = SystemMessage.id(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(item.getEnchantLevel())
                        .addItemName(item.getItemId());
            } else {
                sm = SystemMessage.id(SystemMessageId.S1_DISARMED).addItemName(item.getItemId());
            }
            sendUserPacket(sm);
            // we cant unequip talisman by body slot
            items = getInventory().unEquipItemInBodySlotAndRecord(getInventory().getSlotFromItem(item));
        } else {
            if (!canEquipItemInSlot(getInventory().getPaperdollItemByL2ItemId(item.getItem().getBodyPart()),
                    item.getItem().getBodyPart())) {
                return;
            }

            if (item.getEnchantLevel() > 0) {
                sm = SystemMessage.id(SystemMessageId.S1_S2_EQUIPPED).addNumber(item.getEnchantLevel())
                        .addItemName(item.getItemId());
            } else {
                sm = SystemMessage.id(SystemMessageId.S1_EQUIPPED).addItemName(item.getItemId());
            }
            sendUserPacket(sm);

            items = getInventory().equipItemAndRecord(item);
            // Consume mana - will start a task if required; returns if item is not a shadow
            // item
            if (item.isShadowItem()) {
                sendCritMessage(item.getItemName() + ": осталось " + item.getMana() + " минут.");
            }
            if (item.getExpire() > 0) {
                String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                        .format(new java.util.Date(item.getExpire()));
                sendCritMessage(item.getItemName() + ": истекает " + date + ".");
            }
            item.decreaseMana(true);
        }
        sm = null;

        refreshExpertisePenalty();

        // InventoryUpdate iu = new InventoryUpdate();
        // iu.addItems(Arrays.asList(items));
        // sendPacket(iu);
        InventoryUpdate iu = new InventoryUpdate();
        iu.addItems(Arrays.asList(items));
        sendUserPacket(iu);
        // sendPacket(new ItemList(this, false));

        if (abortAttack) {
            abortCast();
            abortAttack();
        }

        broadcastUserInfo();
    }

    private boolean canEquipItemInSlot(L2ItemInstance tempItem, int tempBodyPart) {
        // check if the item replaces a wear-item
        if (tempItem != null && tempItem.isWear()) {
            return false;// dont allow an item to replace a wear-item
        } else if (tempBodyPart == 0x4000) // left+right hand equipment
        {
            // this may not remove left OR right hand equipment
            tempItem = getInventory().getPaperdollItem(7);
            if (tempItem != null && tempItem.isWear()) {
                return false;
            }

            tempItem = getInventory().getPaperdollItem(8);
            if (tempItem != null && tempItem.isWear()) {
                return false;
            }
        } else if (tempBodyPart == 0x8000) // fullbody armor
        {
            // this may not remove chest or leggins
            tempItem = getInventory().getPaperdollItem(10);
            if (tempItem != null && tempItem.isWear()) {
                return false;
            }

            tempItem = getInventory().getPaperdollItem(11);
            if (tempItem != null && tempItem.isWear()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the the PvP Kills of the L2PcInstance (Number of player killed
     * during a PvP).<BR>
     * <BR>
     */
    public int getPvpKills() {
        return _pvpKills;
    }

    public int getDeaths() {
        return _deaths;
    }

    public void setInvullBuffs(boolean f) {
        _InvullBuffs = f;
    }

    public boolean isInvullBuffs() {
        return _InvullBuffs;
    }

    /**
     * Set the the PvP Kills of the L2PcInstance (Number of player killed during
     * a PvP).<BR>
     * <BR>
     */
    public void setPvpKills(int pvpKills) {
        _pvpKills = pvpKills;
    }

    public void setDeaths(int deaths) {
        _deaths = deaths;
    }

    /**
     * Return the ClassId object of the L2PcInstance contained in
     * L2PcTemplate.<BR>
     * <BR>
     */
    @Override
    public ClassId getClassId() {
        return getTemplate().classId;
    }

    /**
     * Set the template of the L2PcInstance.<BR>
     * <BR>
     *
     * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
     *
     */
    public void setClassId(int Id) {
        if (Config.ACADEMY_CLASSIC) {
            rewardAcademy(Id);
        }

        if (isSubClassActive()) {
            getSubClasses().get(_classIndex).setClassId(Id);
        }

        // Социалка при получении профы
        broadcastPacket(new MagicSkillUser(this, this, 5103, 1, 1000, 0));
        // broadcastPacket(new SocialAction(getObjectId(), 16));
        sendUserPacket(new PlaySound("ItemSound.quest_fanfare_2"));

        setClassTemplate(Id);
        refreshExpertisePenalty();
        // checkAllowedSkills();
    }

    private int _lastAcademyCheck = 0;

    public void rewardAcademy(int Id) {
        if (_clan == null || _lastAcademyCheck == 1) {
            return;
        }

        if (getLvlJoinedAcademy() != 0 && !isSubClassActive()
                && (!Config.ACADEMY_CLASSIC || PlayerClass.values()[Id].getLevel() == ClassLevel.Third)) {
            _lastAcademyCheck = 1;

            setLvlJoinedAcademy(0);
            _clan.broadcastToOnlineMembers(
                    SystemMessage.id(SystemMessageId.CLAN_MEMBER_S1_EXPELLED).addString(getName()));
            if (Config.ACADEMY_BONUS_ON) {
                if (Config.ACADEMY_CLASSIC) {
                    if (getLvlJoinedAcademy() <= 16) {
                        _clan.addPoints(Config.ACADEMY_POINTS);
                    } else if (getLvlJoinedAcademy() >= 39) {
                        _clan.addPoints((int) (Config.ACADEMY_POINTS * 0.4));
                    } else {
                        _clan.addPoints((Config.ACADEMY_POINTS - (getLvlJoinedAcademy() - 16) * 10));
                    }
                } else {
                    _clan.addPoints(Config.ACADEMY_POINTS);
                }
                _clan.broadcastToOnlineMembers(
                        SystemMessage.id(SystemMessageId.CLAN_ACQUIRED_CONTESTED_CLAN_HALL_AND_S1_REPUTATION_POINTS)
                                .addNumber(Config.ACADEMY_POINTS));
            }

            // oust pledge member from the academy, cuz he has finished his 2nd class
            // transfer
            _clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
            _clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
            _clan.removeClanMember(getName(), 0);
            sendUserPacket(Static.ACADEMY_MEMBERSHIP_TERMINATED);
            // receive graduation gift
            // getInventory().addItem("Gift",8181,1,this,null); // give academy circlet
            // getInventory().updateDatabase(); // update database
            addItem("Gift", 8181, 1, this, true);
        }
    }

    /**
     * Return the Experience of the L2PcInstance.
     */
    public long getExp() {
        return getStat().getExp();
    }

    public void setActiveEnchantItem(L2ItemInstance scroll) {
        _activeEnchantItem = scroll;
    }

    public L2ItemInstance getActiveEnchantItem() {
        return _activeEnchantItem;
    }

    /**
     * Set the fists weapon of the L2PcInstance (used when no weapon is
     * equiped).<BR>
     * <BR>
     *
     * @param weaponItem The fists L2Weapon to set to the L2PcInstance
     *
     */
    public void setFistsWeaponItem(L2Weapon weaponItem) {
        _fistsWeaponItem = weaponItem;
    }

    /**
     * Return the fists weapon of the L2PcInstance (used when no weapon is
     * equiped).<BR>
     * <BR>
     */
    public L2Weapon getFistsWeaponItem() {
        return _fistsWeaponItem;
    }

    /**
     * Return the fists weapon of the L2PcInstance Class (used when no weapon is
     * equiped).<BR>
     * <BR>
     */
    public L2Weapon findFistsWeaponItem(int classId) {
        L2Weapon weaponItem = null;
        if ((classId >= 0x00) && (classId <= 0x09)) {
            // human fighter fists
            L2Item temp = ItemTable.getInstance().getTemplate(246);
            weaponItem = (L2Weapon) temp;
        } else if ((classId >= 0x0a) && (classId <= 0x11)) {
            // human mage fists
            L2Item temp = ItemTable.getInstance().getTemplate(251);
            weaponItem = (L2Weapon) temp;
        } else if ((classId >= 0x12) && (classId <= 0x18)) {
            // elven fighter fists
            L2Item temp = ItemTable.getInstance().getTemplate(244);
            weaponItem = (L2Weapon) temp;
        } else if ((classId >= 0x19) && (classId <= 0x1e)) {
            // elven mage fists
            L2Item temp = ItemTable.getInstance().getTemplate(249);
            weaponItem = (L2Weapon) temp;
        } else if ((classId >= 0x1f) && (classId <= 0x25)) {
            // dark elven fighter fists
            L2Item temp = ItemTable.getInstance().getTemplate(245);
            weaponItem = (L2Weapon) temp;
        } else if ((classId >= 0x26) && (classId <= 0x2b)) {
            // dark elven mage fists
            L2Item temp = ItemTable.getInstance().getTemplate(250);
            weaponItem = (L2Weapon) temp;
        } else if ((classId >= 0x2c) && (classId <= 0x30)) {
            // orc fighter fists
            L2Item temp = ItemTable.getInstance().getTemplate(248);
            weaponItem = (L2Weapon) temp;
        } else if ((classId >= 0x31) && (classId <= 0x34)) {
            // orc mage fists
            L2Item temp = ItemTable.getInstance().getTemplate(252);
            weaponItem = (L2Weapon) temp;
        } else if ((classId >= 0x35) && (classId <= 0x39)) {
            // dwarven fists
            L2Item temp = ItemTable.getInstance().getTemplate(247);
            weaponItem = (L2Weapon) temp;
        }

        return weaponItem;
    }

    /**
     * Give Expertise skill of this level and remove beginner Lucky
     * skill.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Get the Level of the L2PcInstance
     * </li>
     * <li>If L2PcInstance Level is 5, remove beginner Lucky skill</li>
     * <li>Add the Expertise skill corresponding to its Expertise level</li>
     * <li>Update the overloaded status of the L2PcInstance</li><BR>
     * <BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other
     * free skills (SP needed = 0)</B></FONT><BR>
     * <BR>
     *
     */
    public void rewardSkills() {
        if (_fantome) {
            return;
        }

        SkillTable st = SkillTable.getInstance();
        // Get the Level of the L2PcInstance
        int lvl = getLevel();
        // Remove beginner Lucky skill
        if (lvl == 10) {
            removeSkill(st.getInfo(194, 1));
        }

        if (Config.DISABLE_GRADE_PENALTY || _fourSide == 5) {
            setExpertiseIndex(5);
            addSkill(st.getInfo(239, 5), true);
        } else {
            // Calculate the current higher Expertise of the L2PcInstance
            for (int i = 0; i < GRADES.length; i++) {
                if (lvl >= GRADES[i]) {
                    setExpertiseIndex(i);
                }
            }
            // Add the Expertise skill corresponding to its Expertise level
            if (getExpertiseIndex() > 0) {
                addSkill(st.getInfo(239, getExpertiseIndex()), true);
            }
        }

        // Active skill dwarven craft
        if (getSkillLevel(1321) < 1 && getRace() == Race.dwarf) {
            addSkill(st.getInfo(1321, 1), true);
        }

        // Active skill common craft
        if (getSkillLevel(1322) < 1) {
            addSkill(st.getInfo(1322, 1), true);
        }

        for (int i = 0; i < CC_LEVELS.length; i++) {
            if (lvl >= CC_LEVELS[i] && getSkillLevel(1320) < (i + 1)) {
                addSkill(st.getInfo(1320, (i + 1)), true);
            }
        }

        // Auto-Learn skills if activated
        if (Config.AUTO_LEARN_SKILLS) {
            giveAvailableSkills();
        }

        sendSkillList();
        // This function gets called on login, so not such a bad place to check weight
        refreshOverloaded(); // Update the overloaded status of the L2PcInstance
        refreshExpertisePenalty(); // Update the expertise status of the L2PcInstance
    }

    /**
     * Regive all skills which aren't saved to database, like Noble, Hero, Clan
     * Skills<BR>
     * <BR>
     *
     */
    private void regiveTemporarySkills() {
        // Do not call this on enterworld or char load

        // Add noble skills if noble
        if (isNoble()) {
            setNoble(true);
        }

        // Add Hero skills if hero
        if (isHero()) {
            setHero(true);
        }

        // Add clan skills
        if (getClan() != null) {
            if (getClan().getReputationScore() >= 0) {
                L2Skill[] skills = getClan().getAllSkills();
                for (L2Skill sk : skills) {
                    if (sk.getMinPledgeClass() <= getPledgeClass()) {
                        addSkill(sk, false);
                    }
                }
            }

            if (getClan().getLevel() > 3 && isClanLeader()) {
                SiegeManager.getInstance().addSiegeSkills(this);
            }
        }
        checkDonateSkills();

        // Add Augmentation Skill Bonus if any.
        L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
        if (wpn == null) {
            wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
        }
        if (wpn != null) {
            if (wpn.isAugmented()) {
                wpn.getAugmentation().applyBoni(this);
            }
        }

        // Reload passive skills from armors / jewels / weapons
        getInventory().reloadEquippedItems();

    }

    /**
     * Give all available skills to the player.<br>
     * <br>
     *
     */
    private void giveAvailableSkills() {
        int unLearnable = 0;
        int skillCounter = 0;

        // Get available skills
        L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
        while (skills.length > unLearnable) {
            for (int i = 0; i < skills.length; i++) {
                L2SkillLearn s = skills[i];
                L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
                if (sk == null || !sk.getCanLearn(getClassId())) {
                    unLearnable++;
                    continue;
                }

                if (getSkillLevel(sk.getId()) == -1) {
                    skillCounter++;
                }

                addSkill(sk, true);
            }

            // Get new available skills
            skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
        }

        sendMessage("Получено " + skillCounter + " новых скиллов.");
    }

    /**
     * Set the Experience value of the L2PcInstance.
     */
    public void setExp(long exp) {
        getStat().setExp(exp);
    }

    /**
     * Return the Race object of the L2PcInstance.<BR>
     * <BR>
     */
    public Race getRace() {
        if (!isSubClassActive()) {
            return getTemplate().race;
        }

        L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
        return charTemp.race;
    }

    public L2Radar getRadar() {
        return _radar;
    }

    /**
     * Return the SP amount of the L2PcInstance.
     */
    public int getSp() {
        return getStat().getSp();
    }

    /**
     * Set the SP amount of the L2PcInstance.
     */
    public void setSp(int sp) {
        super.getStat().setSp(sp);
    }

    /**
     * Return true if this L2PcInstance is a clan leader in ownership of the
     * passed castle
     */
    public boolean isCastleLord(int castleId) {
        L2Clan clan = getClan();

        // player has clan and is the clan leader, check the castle info
        if ((clan != null) && (clan.getLeader().getPlayerInstance() == this)) {
            // if the clan has a castle and it is actually the queried castle, return true
            Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
            if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the Clan Identifier of the L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public int getClanId() {
        if (_showMaskName) {
            return 0;
        }

        return _clanId;
    }

    /**
     * Return the Clan Crest Identifier of the L2PcInstance or 0.<BR>
     * <BR>
     */
    @Override
    public int getClanCrestId() {
        if (_clan != null && _clan.hasCrest()) {
            return _clan.getCrestId();
        }

        return 0;
    }

    /**
     * @return The Clan CrestLarge Identifier or 0
     */
    public int getClanCrestLargeId() {
        if (_clan != null && _clan.hasCrestLarge()) {
            return _clan.getCrestLargeId();
        }

        return 0;
    }

    public long getClanJoinExpiryTime() {
        return _clanJoinExpiryTime;
    }

    public void setClanJoinExpiryTime(long time) {
        _clanJoinExpiryTime = time;
    }

    public long getClanCreateExpiryTime() {
        return _clanCreateExpiryTime;
    }

    public void setClanCreateExpiryTime(long time) {
        _clanCreateExpiryTime = time;
    }

    public void setOnlineTime(long time) {
        _onlineTime = time;
        _onlineBeginTime = System.currentTimeMillis();
    }

    /**
     * Return the PcInventory Inventory of the L2PcInstance contained in
     * _inventory.<BR>
     * <BR>
     */
    public PcInventory getInventory() {
        return _inventory;
    }

    @Override
    public PcInventory getPcInventory() {
        return _inventory;
    }

    /**
     * Delete a ShortCut of the L2PcInstance _shortCuts.<BR>
     * <BR>
     */
    public void removeItemFromShortCut(int objectId) {
        _shortCuts.deleteShortCutByObjectId(objectId);
    }

    /**
     * Return True if the L2PcInstance is sitting.<BR>
     * <BR>
     */
    @Override
    public boolean isSitting() {
        return _waitTypeSitting;
    }

    /**
     * Set _waitTypeSitting to given value
     */
    public void setIsSitting(boolean f) {
        _waitTypeSitting = f;
    }

    /**
     * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and
     * send a Server->Client ChangeWaitType packet (broadcast)<BR>
     * <BR>
     */
    public void sitDown() {
        if (isCastingNow() && !_relax) {
            sendUserPacket(Static.CANT_SET_WHILE_CAST);
            return;
        }

        if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImobilised()) {
            breakAttack();
            setIsSitting(true);
            broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
            // Schedule a sit down task to wait for the animation to finish
            ThreadPoolManager.getInstance().scheduleAi(new SitDownTask(this), 2500, true);
            setIsParalyzed(true);
        }
    }

    /**
     * Sit down Task
     */
    static class SitDownTask implements Runnable {

        L2PcInstance _player;

        SitDownTask(L2PcInstance player) {
            _player = player;
        }

        public void run() {
            _player.setIsParalyzed(false);
            _player.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
        }
    }

    /**
     * Stand up Task
     */
    static class StandUpTask implements Runnable {

        L2PcInstance _player;

        StandUpTask(L2PcInstance player) {
            _player = player;
        }

        public void run() {
            _player.setIsSitting(false);
            _player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        }
    }

    /**
     * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and
     * send a Server->Client ChangeWaitType packet (broadcast)<BR>
     * <BR>
     */
    public void standUp() {
        if (L2Event.active && eventSitForced) {
            sendMessage(
                    "A dark force beyond your mortal understanding makes your knees to shake when you try to stand up ...");
        } else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead()) {
            if (_relax) {
                setRelax(false);
                stopEffects(L2Effect.EffectType.RELAXING);
            }

            broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
            // Schedule a stand up task to wait for the animation to finish
            ThreadPoolManager.getInstance().scheduleAi(new StandUpTask(this), 2500, true);
        }
    }

    /**
     * Set the value of the _relax value. Must be True if using skill Relax and
     * False if not.
     */
    public void setRelax(boolean f) {
        _relax = f;
    }

    /**
     * Return the PcWarehouse object of the L2PcInstance.<BR>
     * <BR>
     */
    public PcWarehouse getWarehouse() {
        if (_warehouse == null) {
            _warehouse = new PcWarehouse(this);
            _warehouse.restore();
        }
        if (Config.WAREHOUSE_CACHE) {
            WarehouseCacheManager.getInstance().addCacheTask(this);
        }
        return _warehouse;
    }

    /**
     * Free memory used by Warehouse
     */
    public void clearWarehouse() {
        if (_warehouse != null) {
            _warehouse.deleteMe();
        }
        _warehouse = null;
    }

    /**
     * Return the PcFreight object of the L2PcInstance.<BR>
     * <BR>
     */
    public PcFreight getFreight() {
        return _freight;
    }

    /**
     * Return the Identifier of the L2PcInstance.<BR>
     * <BR>
     */
    public int getCharId() {
        return _charId;
    }

    /**
     * Set the Identifier of the L2PcInstance.<BR>
     * <BR>
     */
    public void setCharId(int charId) {
        _charId = charId;
    }

    /**
     * Return the Adena amount of the L2PcInstance.<BR>
     * <BR>
     */
    public int getAdena() {
        return _inventory.getAdena();
    }

    /**
     * Return the Ancient Adena amount of the L2PcInstance.<BR>
     * <BR>
     */
    public int getAncientAdena() {
        return _inventory.getAncientAdena();
    }

    /**
     * Add adena to Inventory of the L2PcInstance and send a Server->Client
     * InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param count       : int Quantity of adena to be added
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     */
    public void addAdena(String process, int count, L2Object reference, boolean sendMessage) {
        if (count <= 0) {
            return;
        }
        if (sendMessage) {
            sendUserPacket(SystemMessage.id(SystemMessageId.EARNED_ADENA).addNumber(count));
        }

        if (count > 0) {
            _inventory.addAdena(process, count, this, reference);

            // Send update packet
            if (!Config.FORCE_INVENTORY_UPDATE) {
                InventoryUpdate iu = new InventoryUpdate();
                iu.addItem(_inventory.getAdenaInstance());
                sendUserPacket(iu);
            } else {
                sendItems(false);
            }
        }
    }

    /**
     * Reduce adena in Inventory of the L2PcInstance and send a Server->Client
     * InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param count       : int Quantity of adena to be reduced
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return boolean informing if the action was successfull
     */
    public boolean reduceAdena(String process, int count, L2Object reference, boolean sendMessage) {
        if (count <= 0) {
            return false;
        }
        if (count > getAdena()) {
            if (sendMessage) {
                sendUserPacket(Static.YOU_NOT_ENOUGH_ADENA);
            }
            return false;
        }

        if (count > 0) {
            L2ItemInstance adenaItem = _inventory.getAdenaInstance();
            _inventory.reduceAdena(process, count, this, reference);

            // Send update packet
            if (!Config.FORCE_INVENTORY_UPDATE) {
                InventoryUpdate iu = new InventoryUpdate();
                iu.addItem(adenaItem);
                sendUserPacket(iu);
            } else {
                sendItems(false);
            }

            if (sendMessage) {
                sendUserPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ADENA).addNumber(count));
            }
        }

        return true;
    }

    /**
     * Add ancient adena to Inventory of the L2PcInstance and send a
     * Server->Client InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param count       : int Quantity of ancient adena to be added
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     */
    public void addAncientAdena(String process, int count, L2Object reference, boolean sendMessage) {
        if (sendMessage) {
            sendUserPacket(SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(PcInventory.ANCIENT_ADENA_ID)
                    .addNumber(count));
        }

        if (count > 0) {
            _inventory.addAncientAdena(process, count, this, reference);

            if (!Config.FORCE_INVENTORY_UPDATE) {
                InventoryUpdate iu = new InventoryUpdate();
                iu.addItem(_inventory.getAncientAdenaInstance());
                sendUserPacket(iu);
            } else {
                sendItems(false);
            }
        }
    }

    /**
     * Reduce ancient adena in Inventory of the L2PcInstance and send a
     * Server->Client InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param count       : int Quantity of ancient adena to be reduced
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return boolean informing if the action was successfull
     */
    public boolean reduceAncientAdena(String process, int count, L2Object reference, boolean sendMessage) {
        if (count <= 0) {
            return false;
        }
        if (count > getAncientAdena()) {
            if (sendMessage) {
                sendUserPacket(Static.YOU_NOT_ENOUGH_ADENA);
            }

            return false;
        }

        if (count > 0) {
            L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
            _inventory.reduceAncientAdena(process, count, this, reference);

            if (!Config.FORCE_INVENTORY_UPDATE) {
                InventoryUpdate iu = new InventoryUpdate();
                iu.addItem(ancientAdenaItem);
                sendUserPacket(iu);
            } else {
                sendItems(false);
            }

            if (sendMessage) {
                sendUserPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addNumber(count)
                        .addItemName(PcInventory.ANCIENT_ADENA_ID));
            }
        }

        return true;
    }

    /**
     * Adds item to inventory and send a Server->Client InventoryUpdate packet
     * to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param item        : L2ItemInstance to be added
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     */
    public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage) {
        L2Item template = ItemTable.getInstance().getTemplate(item.getItemId());
        if (template == null) {
            return;
        }

        if (item.getCount() > 0) {
            // Sends message to client if requested
            if (sendMessage) {
                SystemMessage sm = null;
                if (item.getCount() > 1) {
                    sm = SystemMessage.id(SystemMessageId.YOU_PICKED_UP_S1_S2).addItemName(item.getItemId())
                            .addNumber(item.getCount());
                } else if (item.getEnchantLevel() > 0) {
                    sm = SystemMessage.id(SystemMessageId.YOU_PICKED_UP_A_S1_S2).addNumber(item.getEnchantLevel())
                            .addItemName(item.getItemId());
                } else {
                    sm = SystemMessage.id(SystemMessageId.YOU_PICKED_UP_S1).addItemName(item.getItemId());
                }

                sendUserPacket(sm);
                sm = null;
            }

            // Add the item to inventory
            L2ItemInstance newitem = _inventory.addItem(process, item, this, reference);

            // Send inventory update packet
            if (!Config.FORCE_INVENTORY_UPDATE) {
                InventoryUpdate playerIU = new InventoryUpdate();
                playerIU.addItem(newitem);
                sendUserPacket(playerIU);
            } else {
                sendItems(false);
            }

            // Update current load as well
            /*
             * StatusUpdate su = new StatusUpdate(getObjectId());
             * su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
             * sendPacket(su);
             */
            sendChanges();

            // Cursed Weapon
            if (CursedWeaponsManager.getInstance().isCursed(newitem.getItemId())) {
                CursedWeaponsManager.getInstance().activate(this, newitem);
            }

            // If over capacity, trop the item
            if (!isGM() && !_inventory.validateCapacity(0)) {
                dropItem("InvDrop", newitem, null, true);
            }
        }
    }

    /**
     * Adds item to Inventory and send a Server->Client InventoryUpdate packet
     * to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param itemId      : int Item Identifier of the item to be added
     * @param count       : int Quantity of items to be added
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     */
    public void addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage) {
        if (count <= 0) {
            return;
        }
        /*
         * L2Item template = ItemTable.getInstance().getTemplate(itemId); if
         * (template == null) return;
         */
        if (ItemTable.getInstance().getTemplate(itemId) == null) {
            return;
        }

        if (count > 0) {
            // Sends message to client if requested
            if (sendMessage) {
                SystemMessage sm = null;
                if (count > 1) {
                    if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest")) {
                        sm = SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addNumber(count);
                    } else {
                        sm = SystemMessage.id(SystemMessageId.YOU_PICKED_UP_S1_S2).addItemName(itemId).addNumber(count);
                    }
                } else if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest")) {
                    sm = SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(itemId);
                } else {
                    sm = SystemMessage.id(SystemMessageId.YOU_PICKED_UP_S1).addItemName(itemId);
                }
                sendUserPacket(sm);
                sm = null;
            }
            // Auto use herbs - autoloot
            /*
             * if (ItemTable.getInstance().createDummyItem(itemId).getItemType()
             * == L2EtcItemType.HERB) //If item is herb dont add it to iv :] {
             * if(!isCastingNow()){ L2ItemInstance herb = new
             * L2ItemInstance(_charId, itemId); IItemHandler handler =
             * ItemHandler.getInstance().getItemHandler(herb.getItemId()); if
             * (handler == null) _log.warning("No item handler registered for
             * Herb - item ID " + herb.getItemId() + "."); else{
             * handler.useItem(this, herb); if(_herbstask>=100)_herbstask -=100;
             * } }else{ _herbstask += 100;
             * ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process,
             * itemId, count, reference, sendMessage), _herbstask); } } else
             */
            // {
            // Add the item to inventory
            L2ItemInstance item = _inventory.addItem(process, itemId, count, this, reference);

            // Send inventory update packet
            if (!Config.FORCE_INVENTORY_UPDATE) {
                InventoryUpdate playerIU = new InventoryUpdate();
                playerIU.addItem(item);
                sendUserPacket(playerIU);
            } else {
                sendItems(false);
            }

            // Update current load as well
            /*
             * StatusUpdate su = new StatusUpdate(getObjectId());
             * su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
             * sendPacket(su);
             */
            sendChanges();

            // Cursed Weapon
            if (CursedWeaponsManager.getInstance().isCursed(itemId)) {
                CursedWeaponsManager.getInstance().activate(this, item);
            }

            // If over capacity, drop the item
            if (!isGM() && !_inventory.validateCapacity(0)) {
                dropItem("InvDrop", item, null, true);
            }
            // }
        }
    }

    /**
     * Destroy item from inventory and send a Server->Client InventoryUpdate
     * packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param item        : L2ItemInstance to be destroyed
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage) {
        int oldCount = item.getCount();
        item = _inventory.destroyItem(process, item, this, reference);

        if (item == null) {
            if (sendMessage) {
                sendUserPacket(Static.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        // Send inventory update packet
        if (!Config.FORCE_INVENTORY_UPDATE) {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(item);
            sendUserPacket(playerIU);
        } else {
            sendItems(false);
        }

        // Update current load as well
        // StatusUpdate su = new StatusUpdate(getObjectId());
        // su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        // sendPacket(su);
        refreshOverloaded();

        // Sends message to client if requested
        if (sendMessage) {
            sendUserPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addNumber(oldCount)
                    .addItemName(item.getItemId()));
        }

        return true;
    }

    /**
     * Destroys item from inventory and send a Server->Client InventoryUpdate
     * packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param objectId    : int Item Instance identifier of the item to be
     *                    destroyed
     * @param count       : int Quantity of items to be destroyed
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage) {
        if (count <= 0) {
            return false;
        }
        L2ItemInstance item = _inventory.getItemByObjectId(objectId);

        if (item == null || item.getCount() < count
                || _inventory.destroyItem(process, objectId, count, this, reference) == null) {
            if (sendMessage) {
                sendUserPacket(Static.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        // Send inventory update packet
        if (!Config.FORCE_INVENTORY_UPDATE) {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(item);
            sendUserPacket(playerIU);
        } else {
            sendItems(false);
        }

        // Update current load as well
        // StatusUpdate su = new StatusUpdate(getObjectId());
        // su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        // sendPacket(su);
        refreshOverloaded();

        // Sends message to client if requested
        if (sendMessage) {
            sendUserPacket(
                    SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addNumber(count).addItemName(item.getItemId()));
        }
        return true;
    }

    /**
     * Destroys shots from inventory without logging and only occasional saving
     * to database. Sends a Server->Client InventoryUpdate packet to the
     * L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param objectId    : int Item Instance identifier of the item to be
     *                    destroyed
     * @param count       : int Quantity of items to be destroyed
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItemWithoutTrace(String process, int objectId, int count, L2Object reference,
            boolean sendMessage) {
        if (count <= 0) {
            return false;
        }
        L2ItemInstance item = _inventory.getItemByObjectId(objectId);

        if (item == null || item.getCount() < count) {
            if (sendMessage) {
                sendUserPacket(Static.NOT_ENOUGH_ITEMS);
            }
            return false;
        }

        // Adjust item quantity
        if (item.getCount() > count) {
            synchronized (item) {
                item.changeCountWithoutTrace(process, -count, this, reference);
                item.setLastChange(L2ItemInstance.MODIFIED);

                // could do also without saving, but let's save approx 1 of 10
                if (GameTimeController.getGameTicks() % 10 == 0) {
                    item.updateDatabase();
                }
                _inventory.refreshWeight();
            }
        } else {
            // Destroy entire item and save to database
            _inventory.destroyItem(process, item, this, reference);
        }

        // Send inventory update packet
        if (!Config.FORCE_INVENTORY_UPDATE) {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(item);
            sendUserPacket(playerIU);
        } else {
            sendItems(false);
        }

        // Update current load as well
        /*
         * StatusUpdate su = new StatusUpdate(getObjectId());
         * su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
         * sendPacket(su);
         */
        sendChanges();

        // Sends message to client if requested
        if (sendMessage) {
            sendUserPacket(
                    SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addNumber(count).addItemName(item.getItemId()));
        }
        return true;
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and send a
     * Server->Client InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param itemId      : int Item identifier of the item to be destroyed
     * @param count       : int Quantity of items to be destroyed
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage) {
        if (count <= 0) {
            return false;
        }
        L2ItemInstance item = _inventory.getItemByItemId(itemId);

        if (item == null || item.getCount() < count
                || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null) {
            if (sendMessage) {
                sendUserPacket(Static.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        // Send inventory update packet
        if (!Config.FORCE_INVENTORY_UPDATE) {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(item);
            sendUserPacket(playerIU);
        } else {
            sendItems(false);
        }

        // Update current load as well
        /*
         * StatusUpdate su = new StatusUpdate(getObjectId());
         * su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
         * sendPacket(su);
         */
        sendChanges();

        // Sends message to client if requested
        if (sendMessage) {
            sendUserPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addNumber(count).addItemName(itemId));
        }

        return true;
    }

    /**
     * Destroy all weared items from inventory and send a Server->Client
     * InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return boolean informing if the action was successfull
     */
    public void destroyWearedItems(String process, L2Object reference, boolean sendMessage) {

        // Go through all Items of the inventory
        for (L2ItemInstance item : getInventory().getItems()) {
            // Check if the item is a Try On item in order to remove it
            if (item.isWear()) {
                if (item.isEquipped()) {
                    getInventory().unEquipItemInSlotAndRecord(item.getEquipSlot());
                }

                if (_inventory.destroyItem(process, item, this, reference) == null) {
                    _log.warning("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ "
                            + item.getObjectId() + " ]");
                    continue;
                }

                // Send an Unequipped Message in system window of the player for each Item
                sendUserPacket(SystemMessage.id(SystemMessageId.S1_DISARMED).addItemName(item.getItemId()));
            }
        }

        // Send the StatusUpdate Server->Client Packet to the player with new CUR_LOAD
        // (0x0e) information
        /*
         * StatusUpdate su = new StatusUpdate(getObjectId());
         * su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
         * sendPacket(su);
         */
        sendChanges();

        // Send the ItemList Server->Client Packet to the player in order to refresh its
        // Inventory
        sendItems(true);

        // Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to
        // all L2PcInstance in its _KnownPlayers
        broadcastUserInfo();

        // Sends message to client if requested
        sendUserPacket(Static.TRY_ON_ENDED);
    }

    /**
     * Transfers item to another ItemContainer and send a Server->Client
     * InventoryUpdate packet to the L2PcInstance.
     *
     * @param process   : String Identifier of process triggering this action
     * @param itemId    : int Item Identifier of the item to be transfered
     * @param count     : int Quantity of items to be transfered
     * @param reference : L2Object Object referencing current action like NPC
     *                  selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item
     *         in inventory
     */
    public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2Object reference) {
        L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
        if (oldItem == null) {
            return null;
        }
        L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
        if (newItem == null) {
            return null;
        }

        // Send inventory update packet
        /*
         * if (!Config.FORCE_INVENTORY_UPDATE) { InventoryUpdate playerIU = new
         * InventoryUpdate();
         *
         * if (oldItem.getCount() > 0 && oldItem != newItem)
         * playerIU.addModifiedItem(oldItem); else
         * playerIU.addRemovedItem(oldItem);
         *
         * sendPacket(playerIU); } else
         */
        sendItems(false);

        // Update current load as well
        /*
         * StatusUpdate playerSU = new StatusUpdate(getObjectId());
         * playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
         * sendPacket(playerSU);
         */
        sendChanges();

        // Send target update packet
        if (target instanceof PcInventory) {
            L2PcInstance targetPlayer = ((PcInventory) target).getOwner();

            if (!Config.FORCE_INVENTORY_UPDATE) {
                InventoryUpdate playerIU = new InventoryUpdate();

                if (newItem.getCount() > count) {
                    playerIU.addModifiedItem(newItem);
                } else {
                    playerIU.addNewItem(newItem);
                }

                targetPlayer.sendUserPacket(playerIU);
            } else {
                targetPlayer.sendItems(false);
            }

            // Update current load as well
            /*
             * playerSU = new StatusUpdate(target.getObjectId());
             * playerSU.addAttribute(StatusUpdate.CUR_LOAD,
             * target.getCurrentLoad()); target.sendPacket(playerSU);
             */
            targetPlayer.sendChanges();
        } else if (target instanceof PetInventory) {
            PetInventoryUpdate petIU = new PetInventoryUpdate();

            if (newItem.getCount() > count) {
                petIU.addModifiedItem(newItem);
            } else {
                petIU.addNewItem(newItem);
            }

            ((PetInventory) target).getOwner().getOwner().sendUserPacket(petIU);
        }

        return newItem;
    }

    /**
     * Drop item from inventory and send a Server->Client InventoryUpdate packet
     * to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param item        : L2ItemInstance to be dropped
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return boolean informing if the action was successfull
     */
    public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage) {
        item = _inventory.dropItem(process, item, this, reference);

        if (item == null) {
            if (sendMessage) {
                sendUserPacket(Static.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        // item.dropMe(this, getX() + Rnd.get(50) - 25, getY() + Rnd.get(50) - 25,
        // GeoData.getInstance().getHeight(getX(), getY(), getZ()));
        // item.dropMe(this, getClientX() + Rnd.get(50) - 25, getClientY() + Rnd.get(50)
        // - 25, getClientZ() + 20);
        item.dropMe(this, getX(), getY(), getZ());

        if (Config.AUTODESTROY_ITEM_AFTER > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM
                && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId())) {
            if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable()) {
                ItemsAutoDestroy.getInstance().addItem(item);
            }
        }

        if (Config.DESTROY_DROPPED_PLAYER_ITEM) {
            if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM)) {
                item.setProtected(false);
            } else {
                item.setProtected(true);
            }
        } else {
            item.setProtected(true);
        }

        // Send inventory update packet
        if (!Config.FORCE_INVENTORY_UPDATE) {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(item);
            sendUserPacket(playerIU);
        } else {
            sendItems(false);
        }

        // Update current load as well
        /*
         * StatusUpdate su = new StatusUpdate(getObjectId());
         * su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
         * sendPacket(su);
         */
        sendChanges();

        // Sends message to client if requested
        if (sendMessage) {
            sendUserPacket(SystemMessage.id(SystemMessageId.YOU_DROPPED_S1).addItemName(item.getItemId()));
        }
        return true;
    }

    /**
     * Drop item from inventory by using its <B>objectID</B> and send a
     * Server->Client InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     : String Identifier of process triggering this action
     * @param objectId    : int Item Instance identifier of the item to be dropped
     * @param count       : int Quantity of items to be dropped
     * @param x           : int coordinate for drop X
     * @param y           : int coordinate for drop Y
     * @param z           : int coordinate for drop Z
     * @param reference   : L2Object Object referencing current action like NPC
     *                    selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     *                    about this action
     * @return L2ItemInstance corresponding to the new item or the updated item
     *         in inventory
     */
    public L2ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, L2Object reference,
            boolean sendMessage) {
        L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
        L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);

        if (item == null) {
            if (sendMessage) {
                sendUserPacket(Static.NOT_ENOUGH_ITEMS);
            }

            return null;
        }

        item.dropMe(this, x, y, z);

        if (Config.AUTODESTROY_ITEM_AFTER > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM
                && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId())) {
            if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable()) {
                ItemsAutoDestroy.getInstance().addItem(item);
            }
        }
        if (Config.DESTROY_DROPPED_PLAYER_ITEM) {
            if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM)) {
                item.setProtected(false);
            } else {
                item.setProtected(true);
            }
        } else {
            item.setProtected(true);
        }

        // Send inventory update packet
        if (!Config.FORCE_INVENTORY_UPDATE) {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(invitem);
            sendUserPacket(playerIU);
        } else {
            sendItems(false);
        }

        // Update current load as well
        /*
         * StatusUpdate su = new StatusUpdate(getObjectId());
         * su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
         * sendPacket(su);
         */
        sendChanges();

        // Sends message to client if requested
        if (sendMessage) {
            sendUserPacket(SystemMessage.id(SystemMessageId.YOU_DROPPED_S1).addItemName(item.getItemId()));
        }

        return item;
    }

    public L2ItemInstance checkItemManipulation(int objectId, int count, String action) {
        // TODO: if we remove objects that are not visisble from the L2World, we'll have
        // to remove this check
        if (L2World.getInstance().findObject(objectId) == null) {
            _log.finest(getObjectId() + ": player tried to " + action + " item not available in L2World");
            return null;
        }

        L2ItemInstance item = getInventory().getItemByObjectId(objectId);

        if (item == null || item.getOwnerId() != getObjectId()) {
            _log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
            return null;
        }

        if (count < 0 || (count > 1 && !item.isStackable())) {
            _log.finest(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
            return null;
        }

        if (count > item.getCount()) {
            _log.finest(getObjectId() + ": player tried to " + action + " more items than he owns");
            return null;
        }

        // Pet is summoned and not the item that summoned the pet AND not the buggle
        // from strider you're mounting
        if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId) {
            // if (Config.DEBUG)
            // _log.finest(getObjectId()+": player tried to " + action + " item controling
            // pet");

            return null;
        }

        if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId) {
            // if (Config.DEBUG)
            // _log.finest(getObjectId()+":player tried to " + action + " an enchant scroll
            // he was using");

            return null;
        }

        if (item.isWear()) {
            // cannot drop/trade wear-items
            return null;
        }

        return item;
    }

    /**
     * Set protection from agro mobs when getting up from fake death, according
     * settings.
     */
    @Override
    public void setRecentFakeDeath(boolean f) {
        _recentFakeDeathEndTime = f
                ? GameTimeController.getGameTicks()
                        + Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND
                : 0;
    }

    public boolean isRecentFakeDeath() {
        return _recentFakeDeathEndTime > GameTimeController.getGameTicks();
    }

    /**
     * Get the client owner of this char.<BR>
     * <BR>
     */
    public L2GameClient getClient() {
        return _client;
    }

    public void setClient(L2GameClient client) {
        _client = client;
    }

    /**
     * Close the active connection with the client.<BR>
     * <BR>
     */
    public void closeNetConnection() {
        if (_client != null) {
            _client.close(Static.LeaveWorld);
        }
    }

    // заглушки коннекта
    @Override
    public void setConnected(boolean f) {
        _isConnected = f;
    }

    public boolean isConnected() {
        return _isConnected;
    }

    public Point3D getCurrentSkillWorldPosition() {
        return _currentSkillWorldPosition;
    }

    public void setCurrentSkillWorldPosition(Point3D worldPosition) {
        _currentSkillWorldPosition = worldPosition;
    }

    /**
     * Manage actions when a player click on this L2PcInstance.<BR>
     * <BR>
     *
     * <B><U> Actions on first click on the L2PcInstance (Select it)</U>
     * :</B><BR>
     * <BR>
     * <li>Set the target of the player</li>
     * <li>Send a
     * Server->Client packet MyTargetSelected to the player (display the select
     * window)</li><BR>
     * <BR>
     *
     * <B><U> Actions on second click on the L2PcInstance (Follow it/Attack
     * it/Intercat with it)</U> :</B><BR>
     * <BR>
     * <li>Send a Server->Client packet
     * MyTargetSelected to the player (display the select window)</li>
     * <li>If
     * this L2PcInstance has a Private Store, notify the player AI with
     * AI_INTENTION_INTERACT</li>
     * <li>If this L2PcInstance is autoAttackable,
     * notify the player AI with AI_INTENTION_ATTACK</li><BR>
     * <BR>
     * <li>If this
     * L2PcInstance is NOT autoAttackable, notify the player AI with
     * AI_INTENTION_FOLLOW</li><BR>
     * <BR>
     *
     * <B><U> Example of use </U> :</B><BR>
     * <BR>
     * <li>Client packet : Action,
     * AttackRequest</li><BR>
     * <BR>
     *
     * @param player The player that start an action on this L2PcInstance
     *
     */
    @Override
    public void onAction(L2PcInstance player) {
        // See description in TvTEvent.java
        /*
         * if (!TvTEvent.onAction(player.getName(), getName())) {
         * player.sendActionFailed(); return; }
         */
        // Check if the L2PcInstance is confused
        if (player.isOutOfControl()) {
            player.sendActionFailed();
            return;
        }

        // revalidateZone(true);
        // Check if the player already target this L2PcInstance
        if (player.getTarget() != this) {
            player.setTarget(this);
            if (_isPartner) {
                player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
                StatusUpdate su = new StatusUpdate(getObjectId());
                su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
                su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
                player.sendPacket(su);
            } else {
                player.sendUserPacket(new MyTargetSelected(getObjectId(), 0));
            }
            player.sendActionFailed();
        } else {
            player.sendUserPacket(new MyTargetSelected(getObjectId(), 0));
            // Check if this L2PcInstance has a Private Store
            if (getPrivateStoreType() != 0 || isPcNpc()) {
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
            } else {
                // Check if this L2PcInstance is autoAttackable
                if (isAutoAttackable(player)) {
                    // Player with lvl < 21 can't attack a cursed weapon holder
                    // And a cursed weapon holder can't attack players with lvl < 21
                    if ((isCursedWeaponEquiped() && player.getLevel() < 21)
                            || (player.isCursedWeaponEquiped() && getLevel() < 21)) {
                        player.sendActionFailed();
                    } else {
                        player.clearNextLoc();
                        player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
                    }
                } else if (player != this) {
                    if (canSeeTarget(player)) {
                        player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
                    }
                }
                // else
                player.sendActionFailed();
            }
        }
        // player.sendActionFailed();
    }

    public void onActionShift(L2PcInstance player) {
        /*
         * if (!TvTEvent.onAction(player.getName(), getName())) {
         * player.sendActionFailed(); return; }
         *
         * if ((isInOlympiadMode() || player.isInOlympiadMode()) &&
         * player.getOlympiadGameId() != getOlympiadGameId()) {
         * player.sendActionFailed(); return; }
         */

        // Check if the L2PcInstance is confused
        if (player.isOutOfControl()) {
            player.sendActionFailed();
            return;
        }

        /*
         * if (!player.isGM() && getChannel() != player.getChannel()) {
         * player.sendActionFailed(); return; }
         */
        // revalidateZone(true);
        // Check if the player already target this L2PcInstance
        if (player.getTarget() != this) {
            player.setTarget(this);
            player.sendUserPacket(new MyTargetSelected(getObjectId(), 0));
        }
        if (_isPartner) {
            if (player.equals(_owner)) {
                player.sendPacket(new GMViewCharacterInfo(this));
            }
        }
        player.sendActionFailed();
    }

    /**
     * Returns true if cp update should be done, false if not
     *
     * @return boolean
     */
    private boolean needCpUpdate(int barPixels) {
        double currentCp = getCurrentCp();

        if (currentCp <= 1.0 || getMaxCp() < barPixels) {
            return true;
        }

        if (currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck) {
            if (currentCp == getMaxCp()) {
                _cpUpdateIncCheck = currentCp + 1;
                _cpUpdateDecCheck = currentCp - _cpUpdateInterval;
            } else {
                double doubleMulti = currentCp / _cpUpdateInterval;
                int intMulti = (int) doubleMulti;

                _cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
                _cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
            }

            return true;
        }

        return false;
    }

    /**
     * Returns true if mp update should be done, false if not
     *
     * @return boolean
     */
    private boolean needMpUpdate(int barPixels) {
        double currentMp = getCurrentMp();

        if (currentMp <= 1.0 || getMaxMp() < barPixels) {
            return true;
        }

        if (currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck) {
            if (currentMp == getMaxMp()) {
                _mpUpdateIncCheck = currentMp + 1;
                _mpUpdateDecCheck = currentMp - _mpUpdateInterval;
            } else {
                double doubleMulti = currentMp / _mpUpdateInterval;
                int intMulti = (int) doubleMulti;

                _mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
                _mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
            }

            return true;
        }

        return false;
    }

    /**
     * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance
     * and only current HP, MP and Level to all other L2PcInstance of the
     * Party.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Send the Server->Client packet
     * StatusUpdate with current HP, MP and CP to this L2PcInstance</li><BR>
     * <li>Send the Server->Client packet PartySmallWindowUpdate with current
     * HP, MP and Level to all other L2PcInstance of the Party</li><BR>
     * <BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current
     * HP and MP to all L2PcInstance of the _statusListener</B></FONT><BR>
     * <BR>
     *
     */
    @Override
    public void broadcastStatusUpdate() {
        if (!needStatusUpdate()) {
            return;
        }

        // Send the Server->Client packet StatusUpdate with current HP, MP and CP to
        // this L2PcInstance
        StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
        su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
        su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
        su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
        sendUserPacket(su);

        // Check if a party is in progress and party window update is usefull
        if (isInParty()) {
            getParty().broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
        }

        if (getDuel() != null) {
            getDuel().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));
        }

        if (isInOlympiadMode() && isOlympiadCompStart()) {
            OlympiadGame game = Olympiad.getOlympiadGame(getOlympiadGameId());
            if (game != null) {
                game.broadcastInfo(this, null, false);
            }
        }
    }

    private L2PcInstance _olyEnemy = null;

    public void setOlyEnemy(L2PcInstance enemy) {
        _olyEnemy = enemy;
    }

    public L2PcInstance getOlyEnemy() {
        return _olyEnemy;
    }

    public boolean needStatusUpdate() {
        if (needCpUpdate(352) || super.needHpUpdate(352) || needMpUpdate(352)) {
            return true;
        }

        return false;
    }

    @Override
    public final void updateEffectIcons(boolean f) {
        if (_isBuffing) {
            return;
        }
        // Create the main packet if needed
        MagicEffectIcons mi = null;
        if (!f) {
            mi = new MagicEffectIcons();
        }

        PartySpelled ps = null;
        if (this.isInParty()) {
            ps = new PartySpelled(this);
        }

        // Go through all effects if any
        FastTable<L2Effect> effects = getAllEffectsTable();
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect effect = effects.get(i);
            if (effect == null) {
                continue;
            }

            switch (effect.getEffectType()) {
                case CHARGE: // handled by EtcStatusUpdate
                case SIGNET_GROUND:
                    continue;
            }

            if (effect.getInUse()) {
                if (mi != null) {
                    effect.addIcon(mi);
                }
                if (ps != null) {
                    effect.addPartySpelledIcon(ps);
                }
            }
        }

        // Send the packets if needed
        if (mi != null) {
            sendUserPacket(mi);
        }
        if (ps != null && isInParty()) {
            // summon info only needs to go to the owner, not to the whole party
            // player info: if in party, send to all party members except one's self.
            // if not in party, send to self.
            getParty().broadcastToPartyMembers(this, ps);
        }
    }

    /**
     * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo
     * to all L2PcInstance in its _KnownPlayers.<BR>
     * <BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * Others L2PcInstance in the detection
     * area of the L2PcInstance are identified in <B>_knownPlayers</B>. In order
     * to inform other players of this L2PcInstance state modifications, server
     * just need to go through _knownPlayers to send Server->Client
     * Packet<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Send a Server->Client packet
     * UserInfo to this L2PcInstance (Public and Private Data)</li>
     * <li>Send a
     * Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of
     * the L2PcInstance (Public data only)</li><BR>
     * <BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to
     * other players instead of CharInfo packet. Indeed, UserInfo packet
     * contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
     * <BR>
     *
     */
    @Override
    public final void broadcastUserInfo() {
        // Send a Server->Client packet UserInfo to this L2PcInstance
        sendUserPacket(new UserInfo(this));
        sendEtcStatusUpdate();
        // sendPacket(new SkillCoolTime(this));

        // if(!isVisible() && !Config.INVIS_SHOW)
        // return;
        // CharInfo charInfo = new CharInfo(this);
        // Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of
        // the L2PcInstance
        // if (Config.DEBUG)
        // _log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + "
        // packet: [S] 03 CharInfo");
        Broadcast.toKnownPlayers(this, new CharInfo(this));
    }

    public final void broadcastTitleInfo() {
        // Send a Server->Client packet UserInfo to this L2PcInstance
        sendUserPacket(new UserInfo(this));

        // Send a Server->Client packet TitleUpdate to all L2PcInstance in _KnownPlayers
        // of the L2PcInstance
        // if (Config.DEBUG)
        // _log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + "
        // packet: [S] cc TitleUpdate");
        Broadcast.toKnownPlayers(this, new TitleUpdate(this));
    }

    /**
     * Return the Alliance Identifier of the L2PcInstance.<BR>
     * <BR>
     */
    public int getAllyId() {
        if (_clan == null) {
            return 0;
        }
        return _clan.getAllyId();
    }

    public int getAllyCrestId() {
        if (getAllyId() == 0) {
            return 0;
        }
        return getClan().getAllyCrestId();
    }

    /**
     * Manage hit process (called by Hit Task of L2Character).<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>If the attacker/target is dead or
     * use fake death, notify the AI with EVT_CANCEL and send a Server->Client
     * packet ActionFailed (if attacker is a L2PcInstance)</li>
     * <li>If attack
     * isn't aborted, send a message system (critical hit, missed...) to
     * attacker/target if they are L2PcInstance</li>
     * <li>If attack isn't
     * aborted and hit isn't missed, reduce HP of the target and calculate
     * reflection damage to reduce HP of attacker if necessary</li>
     * <li>if
     * attack isn't aborted and hit isn't missed, manage attack or cast break of
     * the target (calculating rate, sending message...)</li><BR>
     * <BR>
     *
     * @param target   The L2Character targeted
     * @param damage   Nb of HP to reduce
     * @param crit     True if hit is critical
     * @param miss     True if hit is missed
     * @param soulshot True if SoulShot are charged
     * @param shld     True if shield is efficient
     *
     */
    @Override
    protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot,
            boolean shld) {
        super.onHitTimer(target, damage, crit, miss, soulshot, shld);
    }

    /**
     * Send a Server->Client packet StatusUpdate to the L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public void sendPacket(final L2GameServerPacket packet) {
        if (_fantome) {
            return;
        }

        if (_isConnected) {
            try {
                if (_client != null) {
                    _client.sendPacket(packet);
                }
            } catch (final Exception e) {
                _log.log(Level.INFO, "", e);
            }
        }
    }

    @Override
    public void sendUserPacket(L2GameServerPacket packet) {
        if (_fantome) {
            return;
        }

        if (_isConnected) {
            try {
                if (_client != null) {
                    _client.sendPacket(packet);
                }
            } catch (final Exception e) {
                _log.log(Level.INFO, "", e);
            } finally {
                // packet.gc();
                // packet.gcb();
                packet = null;
            }
        }
    }

    public void sendSayPacket(L2GameServerPacket packet, int limit) {
        if (getChatIgnore() < limit) {
            sendPacket(packet);
        }
    }

    /**
     * Manage Interact Task with another L2PcInstance.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>If the private store is a PS_SELL,
     * send a Server->Client PrivateBuyListSell packet to the L2PcInstance</li>
     * <li>If the private store is a PS_BUY, send a Server->Client
     * PrivateBuyListBuy packet to the L2PcInstance</li>
     * <li>If the private
     * store is a PS_MANUFACTURE, send a Server->Client RecipeShopSellList
     * packet to the L2PcInstance</li><BR>
     * <BR>
     *
     * @param target The L2Character targeted
     *
     */
    public void doInteract(L2Character target) {
        if (target == null) {
            sendActionFailed();
            return;
        }

        if (target.isRealPlayer()) {
            L2PcInstance temp = target.getPlayer();
            if (temp == null) {
                sendActionFailed();
                return;
            }
            sendActionFailed();

            if (temp.getPrivateStoreType() == PS_SELL || temp.getPrivateStoreType() == PS_PACKAGE_SELL) {
                sendUserPacket(new PrivateStoreListSell(this, temp));
            } else if (temp.getPrivateStoreType() == PS_BUY) {
                sendUserPacket(new PrivateStoreListBuy(this, temp));
            } else if (temp.getPrivateStoreType() == PS_MANUFACTURE) {
                sendUserPacket(new RecipeShopSellList(this, temp));
            }

        } else {
            // _interactTarget=null should never happen but one never knows ^^;
            target.onAction(this);
        }
    }

    @Override
    public boolean isRealPlayer() {
        return true;
    }

    @Override
    public boolean isPcNpc() {
        return false;
    }

    /**
     * Manage AutoLoot Task.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Send a System Message to the
     * L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
     * <li>Add
     * the Item to the L2PcInstance inventory</li>
     * <li>Send a Server->Client
     * packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot)
     * or ModifiedItem (increase amount)</li>
     * <li>Send a Server->Client packet
     * StatusUpdate to this L2PcInstance with current weight</li><BR>
     * <BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress,
     * distribute Items between party members</B></FONT><BR>
     * <BR>
     *
     * @param target The L2ItemInstance dropped
     *
     */
    public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item) {
        if (isInParty()) {
            getParty().distributeItem(this, item, false, target);
        } else if (item.getItemId() == 57) {
            addAdena("Loot", item.getCount(), target, true);
        } else {
            addItem("Loot", item.getItemId(), item.getCount(), target, true);
            if (Config.LOG_ITEMS && item.getItemId() != 9885) {
                String act = Log.getTime() + "PICKUP(AUTOLOOT) itemId: " + item.getItemId() + "(" + item.getCount()
                        + ") #(player " + getName() + ", account: " + getAccountName() + ", ip: " + getIP() + ", hwid: "
                        + getHWID() + ")" + "\n";
                Log.item(act, Log.PICKUP);
            }
        }
        sendChanges();
    }

    public void doEpicLoot(L2GrandBossInstance boss, int bossId) {
        int raid_item = 8350;
        String raid_name = "Ooops! Скрин для админа сделай.";
        Integer epicChance = -1;

        switch (bossId) {
            case 29001:
                raid_item = 6660;
                raid_name = "Кольцо Ant Queen ушло игроку ";
                break;
            case 29028:
                raid_item = 6657;
                raid_name = "Ожерелье Valakas ушло игроку ";
                break;
            case 29020:
                raid_item = 6658;
                raid_name = "Кольцо Baium ушло игроку ";
                break;
            case 29066:
            case 29067:
            case 29068:
                raid_item = 6656;
                raid_name = "Серьга Antharas ушла игроку ";
                break;
            case 29006:
                raid_item = 6662;
                raid_name = "Кольцо Core ушло игроку ";
                break;
            case 29014:
                raid_item = 6661;
                raid_name = "Серьга Orfen ушла игроку ";
                break;
            case 29022:
                raid_item = 6659;
                raid_name = "Серьга Zaken ушла игроку ";
                break;
            case 29047:
                raid_item = 8191;
                raid_name = "Ожерелье Frintezza ушло игроку ";
                break;
        }

        if (raid_item == 8350) {
            return;
        }

        epicChance = Config.EPIC_JEWERLY_CHANCES.get(raid_item);
        if (epicChance != null && Rnd.get(100) <= epicChance) {
            if (Config.ALT_EPIC_JEWERLY) {
                addItem("raidLoot", raid_item, 1, null, true);
            } else {
                boss.dropItem(raid_item, 1, this);
                return;
            }

            CreatureSay gmcs = new CreatureSay(0, 18, getName(), raid_name + " " + getName());
            sendPacket(gmcs);

            FastList<L2PcInstance> players = getKnownList().getKnownPlayersInRadius(1250);
            L2PcInstance pc = null;
            for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                pc = n.getValue();
                if (pc == null) {
                    continue;
                }
                pc.sendPacket(gmcs);
            }
            players.clear();
            players = null;
            pc = null;
            gmcs = null;
            Log.add(TimeLogger.getTime() + raid_name + " " + getName(), "epic_loot");
            sendChanges();
        }
        raid_name = null;
    }

    public void sendCritMessage(String text) {
        sendUserPacket(new CreatureSay(0, 18, "", text));
    }

    public void giveItem(int item, int count) {
        addItem("giveItem", item, count, this, true);
        sendChanges();
    }

    /**
     * Manage Pickup Task.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Send a Server->Client packet
     * StopMove to this L2PcInstance</li>
     * <li>Remove the L2ItemInstance from
     * the world and send server->client GetItem packets</li>
     * <li>Send a System
     * Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or
     * YOU_PICKED_UP_S1_S2</li>
     * <li>Add the Item to the L2PcInstance
     * inventory</li>
     * <li>Send a Server->Client packet InventoryUpdate to this
     * L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase
     * amount)</li>
     * <li>Send a Server->Client packet StatusUpdate to this
     * L2PcInstance with current weight</li><BR>
     * <BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress,
     * distribute Items between party members</B></FONT><BR>
     * <BR>
     *
     * @param object The L2ItemInstance to pick up
     *
     */
    protected void doPickupItem(L2Object object) {
        if (isAlikeDead() || isFakeDeath()) {
            return;
        }

        // Set the AI Intention to AI_INTENTION_IDLE
        getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

        // Check if the L2Object to pick up is a L2ItemInstance
        if (!(object.isL2Item())) {
            // dont try to pickup anything that is not an item :)
            _log.warning("trying to pickup wrong target." + getTarget());
            return;
        }

        L2ItemInstance target = (L2ItemInstance) object;

        // Send a Server->Client packet ActionFailed to this L2PcInstance
        sendActionFailed();

        // Send a Server->Client packet StopMove to this L2PcInstance
        sendUserPacket(new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading()));

        synchronized (target) {
            // Check if the target to pick up is visible
            if (!target.isVisible()) {
                // Send a Server->Client packet ActionFailed to this L2PcInstance
                sendActionFailed();
                return;
            }

            if (((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER) || !isInParty())
                    && !_inventory.validateCapacity(target)) {
                sendActionFailed();
                sendUserPacket(Static.SLOTS_FULL);
                return;
            }

            if (isInvul() && !isGM()) {
                sendActionFailed();
                sendUserPacket(SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
                return;
            }

            if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId()
                    && !isInLooterParty(target.getOwnerId())) {
                sendActionFailed();
                SystemMessage sm = null;
                if (target.getItemId() == 57) {
                    sm = SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA).addNumber(target.getCount());
                } else if (target.getCount() > 1) {
                    sm = SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S).addItemName(target.getItemId())
                            .addNumber(target.getCount());
                } else {
                    sm = SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId());
                }

                sendUserPacket(sm);
                sm = null;
                return;
            }
            if (target.getItemLootShedule() != null
                    && (target.getOwnerId() == getObjectId() || isInLooterParty(target.getOwnerId()))) {
                target.resetOwnerTimer();
            }

            // Remove the L2ItemInstance from the world and send server->client GetItem
            // packets
            sendChanges();
            target.pickupMe(this);
            if (Config.SAVE_DROPPED_ITEM) // item must be removed from ItemsOnGroundManager if is active
            {
                ItemsOnGroundManager.getInstance().removeObject(target);
            }
        }

        // Auto use herbs - pick up
        if (target.getItemType() == L2EtcItemType.HERB) {
            IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getItemId());
            if (handler == null) {
                _log.fine("No item handler registered for item ID " + target.getItemId() + ".");
            } else {
                handler.useItem(this, target, false);
            }
            ItemTable.getInstance().destroyItem("Consume", target, this, null);
        } // Cursed Weapons are not distributed
        else if (CursedWeaponsManager.getInstance().isCursed(target.getItemId())) {
            addItem("Pickup", target, null, true);
        } else {
            // if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention"
            // system message
            if (target.getItemType() instanceof L2ArmorType || target.getItemType() instanceof L2WeaponType) {
                SystemMessage msg = null;
                if (target.getEnchantLevel() > 0) {
                    msg = SystemMessage.id(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3).addString(getName())
                            .addNumber(target.getEnchantLevel()).addItemName(target.getItemId());
                } else {
                    msg = SystemMessage.id(SystemMessageId.ATTENTION_S1_PICKED_UP_S2).addString(getName())
                            .addItemName(target.getItemId());
                }
                broadcastPacket(msg, 1400);
                msg = null;
            }

            // Check if a Party is in progress
            if (isInParty()) {
                getParty().distributeItem(this, target);
            } else if (target.getItemId() == 57 && getInventory().getAdenaInstance() != null) // adena
            {
                addAdena("Pickup", target.getCount(), null, true);
                ItemTable.getInstance().destroyItem("Pickup", target, this, null);
            } else {
                addItem("Pickup", target, null, true); // regular item
                if (Config.LOG_ITEMS) {
                    String act = Log.getTime() + "PICKUP " + target.getItemName() + "(" + target.getCount() + ")(+"
                            + target.getEnchantLevel() + ")(" + target.getObjectId() + ") #(player " + getName()
                            + ", account: " + getAccountName() + ", ip: " + getIP() + ", hwid: " + getHWID() + ")"
                            + "\n";
                    Log.item(act, Log.PICKUP);
                }
            }
        }
    }

    /**
     * Set a target.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Remove the L2PcInstance from the
     * _statusListener of the old target if it was a L2Character</li>
     * <li>Add
     * the L2PcInstance to the _statusListener of the new target if it's a
     * L2Character</li>
     * <li>Target the new L2Object (add the target to the
     * L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of
     * the L2Object)</li><BR>
     * <BR>
     *
     * @param newTarget The L2Object to target
     *
     */
    @Override
    public void setTarget(L2Object newTarget) {
        // Check if the new target is visible
        if (newTarget != null && !newTarget.isVisible()) {
            newTarget = null;
        }

        // Prevents /target exploiting
        /*
         * if (newTarget != null && Math.abs(newTarget.getZ() - getZ()) > 1000)
         * newTarget = null;
         */
        if (newTarget != null && !isGM()) {
            // Can't target and attack festival monsters if not participant
            if (newTarget.isL2FestivalMonster() && !isFestivalParticipant()) {
                newTarget = null;
            } else if (isInParty() && getParty().isInDimensionalRift()) // Can't target and attack rift invaders if not
                                                                        // in the same room
            {
                byte riftType = getParty().getDimensionalRift().getType();
                byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();

                if (!DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(),
                        newTarget.getY(), newTarget.getZ())) {
                    newTarget = null;
                }
            }
        }

        // Get the current target
        L2Object oldTarget = getTarget();

        if (oldTarget != null) {
            if (oldTarget.equals(newTarget)) {
                return; // no target change
            }
            // Remove the L2PcInstance from the _statusListener of the old target if it was
            // a L2Character
            oldTarget.removeStatusListener(this);
        }

        // Add the L2PcInstance to the _statusListener of the new target if it's a
        // L2Character
        if (newTarget != null && newTarget.isL2Character()) {
            newTarget.addStatusListener(this);
            broadcastPacket(new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ()));
        }

        // Target the new L2Object (add the target to the L2PcInstance _target,
        // _knownObject and L2PcInstance to _KnownObject of the L2Object)
        super.setTarget(newTarget);
    }

    /**
     * Return the active weapon instance (always equiped in the right
     * hand).<BR>
     * <BR>
     */
    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
    }

    /**
     * Return the active weapon item (always equiped in the right hand).<BR>
     * <BR>
     */
    @Override
    public L2Weapon getActiveWeaponItem() {
        L2ItemInstance weapon = getActiveWeaponInstance();

        if (weapon == null) {
            return getFistsWeaponItem();
        }

        return (L2Weapon) weapon.getItem();
    }

    public L2ItemInstance getChestArmorInstance() {
        return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
    }

    @Override
    public L2Armor getActiveChestArmorItem() {
        L2ItemInstance armor = getChestArmorInstance();

        if (armor == null) {
            return null;
        }

        return (L2Armor) armor.getItem();
    }

    @Override
    public boolean isWearingHeavyArmor() {
        L2ItemInstance armor = getChestArmorInstance();

        if ((L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isWearingLightArmor() {
        L2ItemInstance armor = getChestArmorInstance();

        if ((L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isWearingMagicArmor() {
        L2ItemInstance armor = getChestArmorInstance();

        if ((L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC) {
            return true;
        }

        return false;
    }

    public boolean isWearingFormalWear() {
        return _IsWearingFormalWear;
    }

    public void setIsWearingFormalWear(boolean f) {
        _IsWearingFormalWear = f;
    }

    public boolean isMarried() {
        return _married;
    }

    public void setMarried(boolean f) {
        _married = f;
    }

    public boolean isEngageRequest() {
        return _engagerequest;
    }

    private long _engageTime = 0;

    public void setEngageRequest(boolean f, int playerid) {
        _engagerequest = f;
        _engageid = playerid;
        _engageTime = System.currentTimeMillis() + Config.WEDDING_ANSWER_TIME;
    }

    public void setMaryRequest(boolean f) {
        _marryrequest = f;
    }

    public boolean isMaryRequest() {
        return _marryrequest;
    }

    public void setMarryAccepted(boolean f) {
        _marryaccepted = f;
    }

    public boolean isMarryAccepted() {
        return _marryaccepted;
    }

    public int getEngageId() {
        return _engageid;
    }

    public int getPartnerId() {
        return _partnerId;
    }

    public void setPartnerId(int partnerid) {
        _partnerId = partnerid;
    }

    public int getCoupleId() {
        return _coupleId;
    }

    public void setCoupleId(int coupleId) {
        _coupleId = coupleId;
    }

    public void engageAnswer(int answer) {
        if (_engagerequest == false) {
            return;
        } else if (_engageid == 0) {
            return;
        } else {
            if (Config.WEDDING_ANSWER_TIME > 0 && System.currentTimeMillis() > _engageTime) {
                setEngageRequest(false, 0);
                sendUserPacket(Static.ANSWER_TIMEOUT);
                return;
            }

            if (answer == 1) {
                CoupleManager.getInstance().getWedding(_engageid).sayYes(this);
            } else {
                CoupleManager.getInstance().getWedding(_engageid).sayNo(this);
            }

            /*
             * L2PcInstance ptarget =
             * L2World.getInstance().getPlayer(_engageid); if(ptarget!=null) {
             * if (Config.WEDDING_ANSWER_TIME > 0 && System.currentTimeMillis()
             * > _engageTime) { sendPacket(Static.ANSWER_TIMEOUT);
             * ptarget.sendPacket(Static.ANSWER_TIMEOUT); return; } if (answer
             * == 1) { CoupleManager.getInstance().createCouple(ptarget,
             * L2PcInstance.this); sendPacket(Static.WEDDING_YES);
             * ptarget.sendPacket(Static.WEDDING_YES); } else
             * ptarget.sendPacket(Static.WEDDING_NO); }
             */
            setEngageRequest(false, 0);
        }
    }

    /**
     * Return the secondary weapon instance (always equiped in the left
     * hand).<BR>
     * <BR>
     */
    @Override
    public L2ItemInstance getSecondaryWeaponInstance() {
        return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
    }

    /**
     * Return the secondary weapon item (always equiped in the left hand) or the
     * fists weapon.<BR>
     * <BR>
     */
    @Override
    public L2Weapon getSecondaryWeaponItem() {
        L2ItemInstance weapon = getSecondaryWeaponInstance();

        if (weapon == null) {
            return getFistsWeaponItem();
        }

        L2Item item = weapon.getItem();

        if (item instanceof L2Weapon) {
            return (L2Weapon) item;
        }

        return null;
    }

    /**
     * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and
     * Item Drop.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Reduce the Experience of the
     * L2PcInstance in function of the calculated Death Penalty</li>
     * <li>If
     * necessary, unsummon the Pet of the killed L2PcInstance</li>
     * <li>Manage
     * Karma gain for attacker and Karam loss for the killed L2PcInstance</li>
     * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
     * <li>Kill
     * the L2PcInstance</li><BR>
     * <BR>
     *
     *
     * @param i        The HP decrease value
     * @param attacker The L2Character who attacks
     *
     */
    @Override
    public boolean doDie(L2Character killer) {
        // Kill the L2PcInstance
        if (!super.doDie(killer)) {
            return false;
        }

        if (isPhoenixBlessed()) {
            reviveRequest(this, null, false);
        }

        if (killer != null) {
            /*
             * L2PcInstance pk = null; if (killer.isPlayer()) pk =
             * (L2PcInstance) killer;
             */
            increaseDeaths();
            TvTEvent.onKill(killer, this);
            EventManager.getInstance().doDie(this, killer);

            /*
             * if (atEvent && pk != null) pk.kills.add(getName());
             */
            // Clear resurrect xp calculation
            setExpBeforeDeath(0);

            if (isCursedWeaponEquiped()) {
                CursedWeaponsManager.getInstance().drop(_cursedWeaponEquipedId, killer);
            } else {

                // System.out.println("###1#");
                onDieDropItem(killer); // Check if any item should be dropped

                if (killer.isPlayer() && !killer.isCursedWeaponEquiped()) {
                    if (!(isInsidePvpZone() && !isInsideZone(ZONE_SIEGE))) {
                        if (hasClanWarWith(killer)) {
                            if (getClan().getReputationScore() > 0) // when your reputation score is 0 or below, the
                                                                    // other clan cannot acquire any reputation points
                            {
                                killer.getClan().incWarPoints(Config.ALT_CLAN_REP_WAR);
                            }
                            if (killer.getClan().getReputationScore() > 0) // when the opposing sides reputation score
                                                                           // is 0 or below, your clans reputation score
                                                                           // does not decrease
                            {
                                _clan.decWarPoints(Config.ALT_CLAN_REP_WAR);
                            }
                        }
                        // Reduce the Experience of the L2PcInstance in function of the calculated Death
                        // Penalty
                        // NOTE: deathPenalty +- Exp will update karma
                        if (getSkillLevel(L2Skill.SKILL_LUCKY) < 0 || getStat().getLevel() > 9) {
                            deathPenalty(hasClanWarWith(killer));
                        }

                        if (Config.ALLOW_PVPPK_REWARD) {
                            managePvpPkBonus(killer.getPlayer(), true);
                        }

                        if (Config.TOWN_PVP_REWARD) {
                            ZoneManager.getInstance().checkPvpTownRewards(killer.getPlayer());
                        }

                        if (Config.WEBSTAT_ENABLE && Config.WEBSTAT_KILLS) {
                            WebStat.getInstance().addKill(killer.getName(), getName());
                        }

                        if (Config.PVP_ZONE_REWARDS && getPvpFlag() != 0 && getPvpFlag() == killer.getPvpFlag()) {
                            if (System.currentTimeMillis() > killer.getLastPvpPk()) {
                                ZoneManager.getInstance().checkSpecialRewards(killer.getPlayer());
                            }
                        }

                        if (Config.REWARD_PVP_ZONE) {
                            managePvpFarmReward(killer.getPlayer(), true);
                        }

                        killer.getPlayer().checkPvpTitle();
                    }
                }
            }

            if (killer.isL2Npc()) {
                killer.doNpcChat(3, getName());
            }

            CustomPlayerEventList.notifyPlayerDie(this, killer);
        }

        if (getPvpFlag() != 0) {
            setPvpFlag(0);
        }

        // Unsummon Cubics
        if (_cubics != null && _cubics.size() > 0) {
            for (L2CubicInstance cubic : _cubics.values()) {
                cubic.stopAction();
                cubic.cancelDisappear();
            }
            _cubics.clear();
        }

        if (_forceBuff != null) {
            _forceBuff.delete();
        }

        for (L2Character character : getKnownList().getKnownCharacters()) {
            if (character.getForceBuff() != null && character.getForceBuff().getTarget() == this) {
                character.abortCast();
            }
        }

        if (isInParty() && getParty().isInDimensionalRift()) {
            getParty().getDimensionalRift().getDeadMemberList().add(this);
        }

        if (_partner != null) {
            try {
                _partner.despawnMe();
            } catch (Exception t) {
            } // returns pet to control item
        }

        // calculate death penalty buff
        calculateDeathPenaltyBuffLevel(killer);
        if (getPrivateStoreType() != PS_NONE) {
            setPrivateStoreType(L2PcInstance.PS_NONE);
        }
        setTransactionRequester(null);

        broadcastUserInfo();

        stopRentPet(false);
        stopWaterTask(-5);
        updateEffectIcons();
        return true;
    }

    private void onDieDropItem(L2Character killer) {
        if (atEvent || killer == null) {
            return;
        }

        // System.out.println("###2#");
        if (isInsidePvpZone()
                || getChannel() > 1
                || isCursedWeaponEquiped()) {
            return;
        }

        // System.out.println("###3#" + getKarma() + "###" + _karma);
        if (getKarma() == 0) {
            return;
        }

        // System.out.println("###4#");
        if (killer.isL2Npc() && !Config.KARMA_PK_NPC_DROP) {
            return;
        }

        // System.out.println("###5#");
        if (isPremium() && Config.PREMIUM_PKDROP_OFF) {
            return;
        }

        onDieUpdateKarma();

        // System.out.println("###6#");
        if (getPkKills() < Config.KARMA_PK_LIMIT) {
            return;
        }

        // System.out.println("###7#");
        int dropEquip = 60;// Config.KARMA_RATE_DROP_EQUIP;
        int dropEquipWeapon = 60;// Config.KARMA_RATE_DROP_EQUIP_WEAPON;
        int dropItem = 60;// Config.KARMA_RATE_DROP_ITEM;
        int dropLimit = Rnd.get(1, 6);// Config.KARMA_DROP_LIMIT;
        // int dropPercent = Config.KARMA_RATE_DROP;
        /*
         * if (killer.isL2Npc() && getLevel() > 4 && !isFestivalParticipant()) {
         * //dropPercent = Config.PLAYER_RATE_DROP;
         * dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
         * dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
         * dropItem = Config.PLAYER_RATE_DROP_ITEM;
         * dropLimit = Config.PLAYER_DROP_LIMIT;
         * }
         */

        int dropCount = 0;
        String date = "";
        TextBuilder tb = null;
        if (Config.LOG_ITEMS) {
            date = Log.getTime();
            tb = new TextBuilder();
        }

        int itemDropPercent = 0;
        for (L2ItemInstance itemDrop : getInventory().getItemsShufflePkDrop(this)) {
            if (itemDrop == null) {
                continue;
            }

            if (!itemDrop.isDropablePk()) {
                continue;
            }

            if (dropCount >= dropLimit) {
                break;
            }

            itemDropPercent = dropItem; // Item in inventory
            if (itemDrop.isEquipped()) {
                // Set proper chance according to Item type of equipped Item
                itemDropPercent = itemDrop.getItem().isWeapon() ? dropEquipWeapon : dropEquip;
            }

            // NOTE: Each time an item is dropped, the chance of another item being dropped
            // gets lesser (dropCount * 2)
            if (Rnd.get(100) > itemDropPercent) {
                continue;
            }

            if (itemDrop.isEquipped()) {
                getInventory().unEquipItemInSlotAndRecord(itemDrop.getEquipSlot());
            }

            dropPkItem(itemDrop, killer);
            if (Config.LOG_ITEMS) {
                String act = "DIEDROP " + itemDrop.getItemName() + "(" + itemDrop.getCount() + ")(+"
                        + itemDrop.getEnchantLevel() + ")(" + itemDrop.getObjectId() + ") #(player " + getName()
                        + ", account: " + getAccountName() + ", ip: " + getIP() + ")"
                        + (killer.isPlayer()
                                ? "(killer " + killer.getName() + ", account: " + killer.getPlayer().getAccountName()
                                        + ", ip: " + killer.getPlayer().getIP() + ", hwid: "
                                        + killer.getPlayer().getHWID() + ")"
                                : "(killer MOB)");
                tb.append(date + act + "\n");
            }

            dropCount++;
        }

        if (dropCount > 0) {
            sendItems(false);
            sendChanges();
        }

        if (Config.LOG_ITEMS && tb != null) {
            Log.item(tb.toString(), Log.DIEDROP);
            tb.clear();
            tb = null;
        }
        // System.out.println("###99#");
    }

    private void dropPkItem(L2ItemInstance item, L2Character killer) {
        dropItem("DieDrop", item, killer, true);
        sendMessage("Вы обронили " + item.getDisplayName() + ".");
    }

    private void onDieUpdateKarma() {
        double karmaLost = Config.KARMA_LOST_BASE;
        if (karmaLost == 0) {
            karmaLost = 100;
        }
        karmaLost *= getLevel(); // multiply by char lvl
        karmaLost *= (getLevel() / 100.0); // divide by 0.charLVL
        karmaLost = Math.round(karmaLost);
        if (karmaLost < 0) {
            karmaLost = 1;
        }

        // Decrease Karma of the L2PcInstance and Send it a Server->Client StatusUpdate
        // packet with Karma and PvP Flag if necessary
        setKarma(getKarma() - (int) karmaLost);
    }

    private boolean _freePvp = false;

    @Override
    public void setFreePvp(boolean f) {
        _freePvp = f;
    }

    @Override
    public void onKillUpdatePvPKarma(L2Character target) {
        if (target == null) {
            return;
        }
        if (!(target.isL2Playable())) {
            return;
        }

        L2PcInstance targetPlayer = target.getPlayer();
        if (targetPlayer == null) {
            return; // Target player is null
        }
        if (targetPlayer.equals(this)) {
            return; // Target player is self
        }

        if (isCursedWeaponEquiped()) {
            CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquipedId);
            return;
        }

        if (targetPlayer.getKarma() > 0) {
            return;
        }

        // If in duel and you kill (only can kill l2summon), do nothing
        if (isInDuel() && targetPlayer.isInDuel()) {
            return;
        }

        if (_freePvp || _freeArena || _freePk) {
            if (Config.PVPPK_HWIDPENALTY && getHWID().equals(targetPlayer.getHWID())) {
                return;
            }
            increasePvpKills();
            if (Config.ALLOW_PVPPK_REWARD) {
                managePvpPkBonus(getPlayer(), false);
            }

            if (Config.WEBSTAT_ENABLE && Config.WEBSTAT_KILLS) {
                WebStat.getInstance().addKill(getName(), getName());
            }
            return;
        }

        // If in Arena, do nothing
        if (isInsidePvpZone()
                || targetPlayer.isInsidePvpZone()
                || targetPlayer.getChannel() > 1
                || TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getName())) {
            if (!Config.CHECK_PVP_ZONES) {
                if (Config.PVPPK_HWIDPENALTY && getHWID().equals(targetPlayer.getHWID())) {
                    return;
                }
                increasePvpKills();
                if (Config.ALLOW_PVPPK_REWARD) {
                    managePvpPkBonus(getPlayer(), false);
                }

                if (Config.WEBSTAT_ENABLE && Config.WEBSTAT_KILLS) {
                    WebStat.getInstance().addKill(getName(), getName());
                }
                return;
            }
            return;
        }

        if (isInGuild()) {
            if (isGuildEnemyFor(targetPlayer) == 1) {
                addGuildBonus();
                return;
            } else if (isGuildEnemyFor(targetPlayer) == 2) {
                incGuildPenalty();
                sendMessage("Вы убили своего, ваш штраф увеличен.");
            }
        }

        if (target.isL2Summon()) {
            if ((checkIfPvP(target) && targetPlayer.getPvpFlag() != 0)) {
                return;
            }

            if (hasClanWarWith(targetPlayer.getClan())) {
                return;
            }

            if (targetPlayer.getPvpFlag() == 0) {
                increasePkKillsAndKarma(targetPlayer.getLevel(), false);
            }
            return;
        }

        if (isPartnerFor(targetPlayer.getPartner()) || targetPlayer.isPartnerFor(getPartner())) {
            return;
        }

        // Check if it's pvp
        if ((checkIfPvP(target) && targetPlayer.getPvpFlag() != 0)) {
            if (Config.PVPPK_HWIDPENALTY && getHWID().equals(targetPlayer.getHWID())) {
                return;
            }
            increasePvpKills();
            return;
        } else // Target player doesn't have pvp flag set
        {
            // check about wars
            if (hasClanWarWith(targetPlayer.getClan())) {
                if (Config.PVPPK_HWIDPENALTY && getHWID().equals(targetPlayer.getHWID())) {
                    return;
                }
                increasePvpKills(); // 'Both way war' -> 'PvP Kill'
                return;
            }

            if (targetPlayer.getPvpFlag() == 0) // // 'No war' or 'One way war' -> 'Normal PK' - Target player doesn't
                                                // have karma
            {
                increasePkKillsAndKarma(targetPlayer.getLevel());
            }
        }
    }

    public boolean hasClanWarWith(L2Clan enemy) {
        if (enemy == null) {
            return false;
        }

        if (_clan == null) {
            return false;
        }

        if (_clan.isAtWarWith(enemy) && enemy.isAtWarWith(_clan)) {
            return true;
        }

        return false;
    }

    /**
     * Increase the pvp kills count and send the info to the player
     *
     */
    private long _lastPvpKill = 0;

    public void increasePvpKills() {
        if (Config.PVP_DELAY) {
            if (System.currentTimeMillis() < _lastPvpKill) {
                return;
            }

            _lastPvpKill = System.currentTimeMillis() + Config.CHECK_PVP_DELAY;
        }
        // Add karma to attacker and increase its PK counter
        setPvpKills(getPvpKills() + 1);

        sendChanges();
    }

    @Override
    public void increasePvpKills(int count) {
        // Add karma to attacker and increase its PK counter
        setPvpKills(getPvpKills() + count);

        sendChanges();// broadcastUserInfo();
        // Send a Server->Client UserInfo packet to attacker with its Karma and PK
        // Counter
        // sendPacket(new UserInfo(this));
    }

    public void increaseDeaths() {
        setDeaths(getDeaths() + 1);
    }

    /**
     * Increase pk count, karma and send the info to the player
     *
     * @param targLVL : level of the killed player
     */
    public void increasePkKillsAndKarma(int targLVL) {
        increasePkKillsAndKarma(targLVL, true);
    }

    public void increasePkKillsAndKarma(int targLVL, boolean inc) {
        if (Config.FREE_PVP) {
            return;
        }

        int baseKarma = Config.KARMA_MIN_KARMA;
        int newKarma = baseKarma;
        int karmaLimit = Config.KARMA_MAX_KARMA;

        int pkLVL = getLevel();
        int pkPKCount = getPkKills();

        int lvlDiffMulti = 0;
        int pkCountMulti = 0;

        // Check if the attacker has a PK counter greater than 0
        if (pkPKCount > 0) {
            pkCountMulti = pkPKCount / 2;
        } else {
            pkCountMulti = 1;
        }
        if (pkCountMulti < 1) {
            pkCountMulti = 1;
        }

        // Calculate the level difference Multiplier between attacker and killed
        // L2PcInstance
        if (pkLVL > targLVL) {
            lvlDiffMulti = pkLVL / targLVL;
        } else {
            lvlDiffMulti = 1;
        }
        if (lvlDiffMulti < 1) {
            lvlDiffMulti = 1;
        }

        // Calculate the new Karma of the attacker : newKarma =
        // baseKarma*pkCountMulti*lvlDiffMulti
        newKarma *= pkCountMulti;
        newKarma *= lvlDiffMulti;

        // Make sure newKarma is less than karmaLimit and higher than baseKarma
        if (newKarma < baseKarma) {
            newKarma = baseKarma;
        }
        if (newKarma > karmaLimit) {
            newKarma = karmaLimit;
        }

        // Fix to prevent overflow (=> karma has a max value of 2 147 483 647)
        if (getKarma() > (Integer.MAX_VALUE - newKarma)) {
            newKarma = Integer.MAX_VALUE - getKarma();
        }

        // Add karma to attacker and increase its PK counter
        if (inc) {
            setPkKills(getPkKills() + 1);
        }

        setKarma(getKarma() + newKarma);

        sendChanges();//
        // broadcastUserInfo();
        // Send a Server->Client UserInfo packet to attacker with its Karma and PK
        // Counter
        sendUserPacket(new UserInfo(this));
    }

    public int calculateKarmaLost(long exp) {
        // KARMA LOSS
        // When a PKer gets killed by another player or a L2MonsterInstance, it loses a
        // certain amount of Karma based on their level.
        // this (with defaults) results in a level 1 losing about ~2 karma per death,
        // and a lvl 70 loses about 11760 karma per death...
        // You lose karma as long as you were not in a pvp zone and you did not kill
        // urself.
        // NOTE: exp for death (if delevel is allowed) is based on the players level

        long expGained = Math.abs(exp);
        expGained /= Config.KARMA_XP_DIVIDER;

        // FIXME Micht : Maybe this code should be fixed and karma set to a long value
        int karmaLost = 0;
        if (expGained > Integer.MAX_VALUE) {
            karmaLost = Integer.MAX_VALUE;
        } else {
            karmaLost = (int) expGained;
        }

        if (karmaLost < Config.KARMA_LOST_BASE) {
            karmaLost = Config.KARMA_LOST_BASE;
        }
        if (karmaLost > getKarma()) {
            karmaLost = getKarma();
        }

        return karmaLost;
    }

    @Override
    public void updatePvPStatus() {
        if (Config.FREE_PVP) {
            return;
        }

        if (isInsidePvpZone()) {
            return;
        }

        if (isInsideHotZone()) {
            return;
        }

        if (isInOlympiadMode() && isOlympiadStart()) {
            return;
        }

        if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getName())) {
            return;
        }

        setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);

        if (getPvpFlag() == 0) {
            startPvPFlag();
        }
    }

    @Override
    public void updatePvPStatus(L2Character target) {
        if (Config.FREE_PVP) {
            return;
        }

        if (isInsideHotZone()) {
            return;
        }

        if (isInOlympiadMode() && isOlympiadStart()) {
            return;
        }

        if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getName())) {
            return;
        }

        L2PcInstance player_target = target.getPlayer();
        if (player_target == null) {
            return;
        }

        if ((isInDuel() && player_target.getDuel() == getDuel())) {
            return;
        }

        if (isInGuild() && isGuildEnemyFor(player_target) == 1) {
            return;
        }

        if (isPartnerFor(player_target.getPartner()) || player_target.isPartnerFor(getPartner())) {
            return;
        }

        if ((!isInsidePvpZone() || !player_target.isInsidePvpZone()) && player_target.getKarma() == 0) {
            // if (checkIfPvP(player_target))
            // setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
            // else
            setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
            if (_partner != null) {
                _partner.setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
            }
            if (_isPartner && _owner != null) {
                _owner.setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
            }
            if (getPvpFlag() == 0) {
                startPvPFlag();
                if (_partner != null) {
                    _partner.startPvPFlag();
                }
                if (_isPartner && _owner != null) {
                    _owner.startPvPFlag();
                }
            }
        }
    }

    public boolean isPartnerFor(L2PcInstance partner) {
        if (partner == null) {
            return false;
        }

        if (_isPartner && partner.equals(this)) {
            return true;
        }

        return false;
    }

    /**
     * Restore the specified % of experience this L2PcInstance has lost and
     * sends a Server->Client StatusUpdate packet.<BR>
     * <BR>
     */
    public void restoreExp(double restorePercent) {
        if (getExpBeforeDeath() > 0) {
            // Restore the specified % of lost experience.
            getStat().addExp((int) Math.round((getExpBeforeDeath() - getExp()) * restorePercent / 100), true);
            setExpBeforeDeath(0);
        }
    }

    /**
     * Reduce the Experience (and level if necessary) of the L2PcInstance in
     * function of the calculated Death Penalty.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Calculate the Experience loss</li>
     * <li>Set the value of _expBeforeDeath</li>
     * <li>Set the new Experience
     * value of the L2PcInstance and Decrease its level if necessary</li>
     * <li>Send a Server->Client StatusUpdate packet with its new Experience
     * </li><BR>
     * <BR>
     *
     */
    public void deathPenalty(boolean f) {
        // TODO Need Correct Penalty
        // Get the level of the L2PcInstance
        final int lvl = getLevel();

        // The death steal you some Exp
        double percentLost = 7.0;
        if (getLevel() >= 76) {
            percentLost = 2.0;
        } else if (getLevel() >= 40) {
            percentLost = 4.0;
        }

        if (getKarma() > 0) {
            percentLost *= Config.RATE_KARMA_EXP_LOST;
        }

        if (isFestivalParticipant() || f || isInsideZone(ZONE_SIEGE)) {
            percentLost /= 4.0;
        }

        // Calculate the Experience loss
        long lostExp = 0;
        if (!atEvent) {
            if (lvl < Experience.MAX_LEVEL) {
                lostExp = Math
                        .round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
            } else {
                lostExp = Math.round((getStat().getExpForLevel(Experience.MAX_LEVEL)
                        - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost / 100);
            }
        }

        // Get the Experience before applying penalty
        setExpBeforeDeath(getExp());

        if (getCharmOfCourage()) {
            if (getSiegeState() > 0 && isInsideZone(ZONE_SIEGE)) {
                lostExp = 0;
            }
        }

        setCharmOfCourage(false);

        // if (Config.DEBUG)
        // _log.fine(getName() + " died and lost " + lostExp + " experience.");
        // Set the new Experience value of the L2PcInstance
        getStat().addExp(-lostExp, true);
    }

    /**
     * @param b
     */
    public void setPartyMatchingAutomaticRegistration(boolean b) {
        _partyMatchingAutomaticRegistration = b;
    }

    /**
     * @param b
     */
    public void setPartyMatchingShowLevel(boolean b) {
        _partyMatchingShowLevel = b;
    }

    /**
     * @param b
     */
    public void setPartyMatchingShowClass(boolean b) {
        _partyMatchingShowClass = b;
    }

    /**
     * @param memo
     */
    public void setPartyMatchingMemo(String memo) {
        _partyMatchingMemo = memo;
    }

    public boolean isPartyMatchingAutomaticRegistration() {
        return _partyMatchingAutomaticRegistration;
    }

    public String getPartyMatchingMemo() {
        return _partyMatchingMemo;
    }

    public boolean isPartyMatchingShowClass() {
        return _partyMatchingShowClass;
    }

    public boolean isPartyMatchingShowLevel() {
        return _partyMatchingShowLevel;
    }

    /**
     * Manage the increase level task of a L2PcInstance (Max MP, Max MP,
     * Recommandation, Expertise and beginner skills...).<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Send a Server->Client System Message
     * to the L2PcInstance : YOU_INCREASED_YOUR_LEVEL</li>
     * <li>Send a
     * Server->Client packet StatusUpdate to the L2PcInstance with new LEVEL,
     * MAX_HP and MAX_MP</li>
     * <li>Set the current HP and MP of the
     * L2PcInstance, Launch/Stop a HP/MP/CP Regeneration Task and send
     * StatusUpdate packet to all other L2PcInstance to inform (exclusive
     * broadcast)</li>
     * <li>Recalculate the party level</li>
     * <li>Recalculate the
     * number of Recommandation that the L2PcInstance can give</li>
     * <li>Give
     * Expertise skill of this level and remove beginner Lucky
     * skill</li><BR>
     * <BR>
     *
     */
    public void increaseLevel() {
        // Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP
        // Regeneration Task and send StatusUpdate packet to all other L2PcInstance to
        // inform (exclusive broadcast)
        setCurrentHpMp(getMaxHp(), getMaxMp());
        setCurrentCp(getMaxCp());
    }

    /**
     * Stop the HP/MP/CP Regeneration task.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Set the RegenActive flag to False
     * </li>
     * <li>Stop the HP/MP/CP Regeneration task</li><BR>
     * <BR>
     *
     */
    public void stopAllTimers() {
        stopHpMpRegeneration();
        stopWarnUserTakeBreak();
        stopWaterTask(-5);
        stopRentPet();
        stopPvpRegTask();
        stopJailTask(true);
    }

    /**
     * Return the L2Summon of the L2PcInstance or null.<BR>
     * <BR>
     */
    @Override
    public L2Summon getPet() {
        return _summon;
    }

    public boolean isPetSummoned() {
        return (_summon != null);
    }

    /**
     * Set the L2Summon of the L2PcInstance.<BR>
     * <BR>
     */
    public void setPet(L2Summon summon) {
        _summon = summon;
    }

    /**
     * Фея
     */
    L2Summon fairy = null;

    public L2Summon getFairy() {
        return fairy;
    }

    public void setFairy(L2Summon fairy) {
        this.fairy = fairy;
    }

    /**
     * Return the L2Summon of the L2PcInstance or null.<BR>
     * <BR>
     */
    public L2TamedBeastInstance getTrainedBeast() {
        return _tamedBeast;
    }

    /**
     * Set the L2Summon of the L2PcInstance.<BR>
     * <BR>
     */
    public void setTrainedBeast(L2TamedBeastInstance tamedBeast) {
        _tamedBeast = tamedBeast;
    }

    /**
     * риквесты*
     */
    public void setTransactionRequester(final L2PcInstance requestor) {
        _currentTransactionRequester = requestor;
        _currentTransactionTimeout = -1;
    }

    public void setTransactionRequester(L2PcInstance requestor, long timeout) {
        _currentTransactionRequester = requestor;
        _currentTransactionTimeout = timeout;
    }

    public static enum TransactionType {

        NONE,
        PARTY,
        CLAN,
        ALLY,
        TRADE,
        FRIEND,
        CHANNEL,
        TRADED,
        ROOM
    }

    TransactionType _currentTransactionType = TransactionType.NONE;

    public void setTransactionType(TransactionType type) {
        _currentTransactionType = type;
    }

    public TransactionType getTransactionType() {
        return _currentTransactionType;
    }

    /**
     * Set the L2PcInstance requester of a transaction (ex : FriendInvite,
     * JoinAlly, JoinParty...).<BR>
     * <BR>
     */
    public synchronized void setActiveRequester(L2PcInstance requester) {
        _currentTransactionRequester = requester;
    }

    /**
     * Return the L2PcInstance requester of a transaction (ex : FriendInvite,
     * JoinAlly, JoinParty...).<BR>
     * <BR>
     */
    public L2PcInstance getTransactionRequester() {
        return _currentTransactionRequester;
    }

    /**
     * Return True if a transaction is in progress.<BR>
     * <BR>
     */
    public boolean isTransactionInProgress() {
        return (_currentTransactionTimeout < 0 || _currentTransactionTimeout > System.currentTimeMillis())
                && _currentTransactionRequester != null;
    }

    /**
     * Return True if a transaction is in progress.<BR>
     * <BR>
     */
    // public boolean isProcessingTransaction() { return _activeRequester != null ||
    // _activeTradeList != null || _requestExpireTime >
    // GameTimeController.getGameTicks(); }
    /**
     * Select the Warehouse to be used in next activity.<BR>
     * <BR>
     */
    public void onTransactionRequest(L2PcInstance partner) {
        _requestExpireTime = GameTimeController.getGameTicks() + 10 * GameTimeController.TICKS_PER_SECOND;
        partner.setTransactionRequester(this);
    }

    /**
     * Select the Warehouse to be used in next activity.<BR>
     * <BR>
     */
    public void onTransactionResponse() {
        _requestExpireTime = 0;
    }

    /**
     * Select the Warehouse to be used in next activity.<BR>
     * <BR>
     */
    public void setActiveWarehouse(ItemContainer warehouse) {
        _activeWarehouse = warehouse;
    }

    public void setActiveWarehouse(ItemContainer warehouse, int whId) {
        warehouse.setWhId(whId);
        _activeWarehouse = warehouse;
    }

    public void cancelActiveWarehouse() {
        if (_activeWarehouse == null) {
            return;
        }

        _activeWarehouse = null;

        sendUserPacket(new SendTradeDone(0));
    }

    /**
     * Return active Warehouse.<BR>
     * <BR>
     */
    public ItemContainer getActiveWarehouse() {
        return _activeWarehouse;
    }

    /**
     * Select the TradeList to be used in next activity.<BR>
     * <BR>
     */
    public void setActiveTradeList(TradeList tradeList) {
        _activeTradeList = tradeList;
    }

    /**
     * Return active TradeList.<BR>
     * <BR>
     */
    public TradeList getActiveTradeList() {
        return _activeTradeList;
    }

    public void onTradeStart(L2PcInstance partner) {
        if (getTradeStart() != partner.getTradeStart()) {
            // System.out.println("@@@@1");
            cancelActiveTrade();
            return;
        }
        _activeTradeList = new TradeList(this);
        _activeTradeList.setPartner(partner);
        // _activeTradeList.clearAugmentCount();

        sendUserPacket(SystemMessage.id(SystemMessageId.BEGIN_TRADE_WITH_S1).addString(partner.getName()));
        sendUserPacket(new TradeStart(this));
        sendUserPacket(new TradeStartOk());
    }

    public void onTradeConfirm(L2PcInstance partner) {
        if (_activeTradeList == null) {
            // System.out.println("@@@@0");
            cancelActiveTrade();
            return;
        }

        if (getTradeStart() != partner.getTradeStart()) {
            // System.out.println("@@@@2");
            cancelActiveTrade();
            return;
        }

        sendUserPacket(SystemMessage.id(SystemMessageId.S1_CONFIRMED_TRADE).addString(partner.getName()));
        sendUserPacket(Static.TradePressOtherOk);
        partner.sendUserPacket(Static.TradePressOwnOk);
    }

    public void onTradeCancel(L2PcInstance partner) {
        if (_activeTradeList == null) {
            return;
        }

        _activeTradeList.lock();
        _activeTradeList = null;

        sendUserPacket(new SendTradeDone(0));
        sendUserPacket(SystemMessage.id(SystemMessageId.S1_CANCELED_TRADE).addString(partner.getName()));

        setTransactionRequester(null);
        setTransactionType(TransactionType.NONE);
        partner.setTransactionRequester(null);
        partner.setTransactionType(TransactionType.NONE);

        setTradePartner(-1, 0);
        partner.setTradePartner(-1, 0);
    }

    public void onTradeFinish(boolean f) {
        _activeTradeList = null;
        sendUserPacket(new SendTradeDone(1));
        if (f) {
            sendUserPacket(Static.TRADE_SUCCESSFUL);
        }

        setTransactionRequester(null);
        setTransactionType(TransactionType.NONE);
        setTradePartner(-1, 0);
    }

    public void startTrade(L2PcInstance partner) {
        onTradeStart(partner);
        partner.onTradeStart(this);
    }

    public void cancelActiveTrade() {
        if (_activeTradeList == null) {
            return;
        }

        L2PcInstance partner = _activeTradeList.getPartner();
        if (partner != null) {
            partner.onTradeCancel(this);
        }

        onTradeCancel(this);
    }

    public void stopTrade() {
        setTransactionRequester(null);

        _activeTradeList = null;
        sendPacket(new SendTradeDone(0));
        setActiveRequester(null);
        sendActionFailed();
    }

    /**
     * Return the _createList object of the L2PcInstance.<BR>
     * <BR>
     */
    public L2ManufactureList getCreateList() {
        return _createList;
    }

    /**
     * Set the _createList object of the L2PcInstance.<BR>
     * <BR>
     */
    public void setCreateList(L2ManufactureList x) {
        _createList = x;
    }

    /**
     * Return the _buyList object of the L2PcInstance.<BR>
     * <BR>
     */
    public TradeList getSellList() {
        if (_sellList == null) {
            _sellList = new TradeList(this);
        }
        return _sellList;
    }

    /**
     * Return the _buyList object of the L2PcInstance.<BR>
     * <BR>
     */
    public TradeList getBuyList() {
        if (_buyList == null) {
            _buyList = new TradeList(this);
        }
        return _buyList;
    }

    /**
     * Set the Private Store type of the L2PcInstance.<BR>
     * <BR>
     *
     * <B><U> Values </U> :</B><BR>
     * <BR>
     * <li>0 : PS_NONE</li>
     * <li>1 :
     * PS_SELL</li>
     * <li>2 : sellmanage</li><BR>
     * <li>3 : PS_BUY</li><BR>
     * <li>4 :
     * buymanage</li><BR>
     * <li>5 : PS_MANUFACTURE</li><BR>
     *
     */
    public void setPrivateStoreType(int type) {
        _privatestore = type;
        if (type != 0 && !inObserverMode()) {
            setVar("storemode", String.valueOf(type), null);
        } else {
            unsetVar("storemode", null);
        }
    }

    /**
     * Return the Private Store type of the L2PcInstance.<BR>
     * <BR>
     *
     * <B><U> Values </U> :</B><BR>
     * <BR>
     * <li>0 : PS_NONE</li>
     * <li>1 :
     * PS_SELL</li>
     * <li>2 : sellmanage</li><BR>
     * <li>3 : PS_BUY</li><BR>
     * <li>4 :
     * buymanage</li><BR>
     * <li>5 : PS_MANUFACTURE</li><BR>
     *
     */
    public int getPrivateStoreType() {
        return _privatestore;
    }

    /**
     * Set the _skillLearningClassId object of the L2PcInstance.<BR>
     * <BR>
     */
    public void setSkillLearningClassId(ClassId classId) {
        _skillLearningClassId = classId;
    }

    /**
     * Return the _skillLearningClassId object of the L2PcInstance.<BR>
     * <BR>
     */
    public ClassId getSkillLearningClassId() {
        return _skillLearningClassId;
    }

    /**
     * Set the _clan object, _clanId, _clanLeader Flag and title of the
     * L2PcInstance.<BR>
     * <BR>
     */
    public void setClan(L2Clan clan) {
        _clan = clan;
        setTitle("");

        if (clan == null) {
            _clanId = 0;
            _clanPrivileges = 0;
            _pledgeType = 0;
            _powerGrade = 0;
            _lvlJoinedAcademy = 0;
            _apprentice = 0;
            _sponsor = 0;
            return;
        }

        if (!clan.isMember(getObjectId()) && !isFantome()) {
            // char has been kicked from clan
            setClan(null);
            return;
        }

        _clanId = clan.getClanId();
    }

    public void profwindow(L2PcInstance player) {
        if (player.getLevel() >= 76)
            ;
        {
            ClassMaster();
        }
    }

    public void ClassMaster() {
        NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
        html.setFile("data/html/classmaster/index.htm");
        html.replace("%objectId%", String.valueOf(getObjectId()));
        sendPacket(html);
    }

    /**
     * Return the _clan object of the L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public L2Clan getClan() {
        return _clan;
    }

    /**
     * Return True if the L2PcInstance is the leader of its clan.<BR>
     * <BR>
     */
    public boolean isClanLeader() {
        if (getClan() == null) {
            return false;
        }

        return getObjectId() == getClan().getLeaderId();
    }

    /**
     * Reduce the number of arrows owned by the L2PcInstance and send it
     * Server->Client Packet InventoryUpdate or ItemList (to unequip if the last
     * arrow was consummed).<BR>
     * <BR>
     */
    @Override
    protected void reduceArrowCount() {
        if (!Config.USE_ARROWS || isFantome()) {
            return;
        }

        L2ItemInstance arrows = getInventory().destroyItem("Consume",
                getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, this, null);
        // if (Config.DEBUG) _log.fine("arrow count:" + (arrows==null? 0 :
        // arrows.getCount()));

        if (arrows == null || arrows.getCount() == 0) {
            getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
            _arrowItem = null;

            // if (Config.DEBUG) _log.fine("removed arrows count");
            sendItems(false);
        } else if (!Config.FORCE_INVENTORY_UPDATE) {
            InventoryUpdate iu = new InventoryUpdate();
            iu.addModifiedItem(arrows);
            sendUserPacket(iu);
        } else {
            sendItems(false);
        }
    }

    /**
     * Equip arrows needed in left hand and send a Server->Client packet
     * ItemList to the L2PcINstance then return True.<BR>
     * <BR>
     */
    @Override
    protected boolean checkAndEquipArrows() {
        if (!Config.USE_ARROWS || isFantome()) {
            return true;
        }

        // Check if nothing is equiped in left hand
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null) {
            // Get the L2ItemInstance of the arrows needed for this bow
            _arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());

            if (_arrowItem != null) {
                // Equip arrows needed in left hand
                getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);

                // Send a Server->Client packet ItemList to this L2PcINstance to update left
                // hand equipement
                sendItems(false);
            }
        } else {
            // Get the L2ItemInstance of arrows equiped in left hand
            _arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
        }

        return _arrowItem != null;
    }

    /**
     * Disarm the player's weapon and shield.<BR>
     * <BR>
     */
    public boolean disarmWeapons() {
        // Don't allow disarming a cursed weapon
        if (isCursedWeaponEquiped()) {
            return false;
        }
        stopSkillEffects(_activeAug);

        Lock shed = new ReentrantLock();
        shed.lock();
        try {
            if (_euipWeapon != null) {
                return false;
            }
        } finally {
            shed.unlock();
        }

        // Unequip the weapon
        L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
        if (wpn == null) {
            wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
        }
        if (wpn != null) {
            if (wpn.isWear()) {
                return false;
            }

            L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (int i = 0; i < unequiped.length; i++) {
                iu.addModifiedItem(unequiped[i]);
            }
            sendUserPacket(iu);

            abortCast();
            abortAttack();
            broadcastUserInfo();

            // this can be 0 if the user pressed the right mousebutton twice very fast
            if (unequiped.length > 0) {
                SystemMessage sm = null;
                if (unequiped[0].getEnchantLevel() > 0) {
                    sm = SystemMessage.id(SystemMessageId.EQUIPMENT_S1_S2_REMOVED)
                            .addNumber(unequiped[0].getEnchantLevel()).addItemName(unequiped[0].getItemId());
                } else {
                    sm = SystemMessage.id(SystemMessageId.S1_DISARMED).addItemName(unequiped[0].getItemId());
                }

                sendUserPacket(sm);
                sm = null;
            }
        }

        // Unequip the shield
        L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
        if (sld != null) {
            if (sld.isWear()) {
                return false;
            }

            L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (int i = 0; i < unequiped.length; i++) {
                iu.addModifiedItem(unequiped[i]);
            }
            sendUserPacket(iu);

            abortCast();
            abortAttack();
            broadcastUserInfo();

            // this can be 0 if the user pressed the right mousebutton twice very fast
            if (unequiped.length > 0) {
                SystemMessage sm = null;
                if (unequiped[0].getEnchantLevel() > 0) {
                    sm = SystemMessage.id(SystemMessageId.EQUIPMENT_S1_S2_REMOVED)
                            .addNumber(unequiped[0].getEnchantLevel()).addItemName(unequiped[0].getItemId());
                } else {
                    sm = SystemMessage.id(SystemMessageId.S1_DISARMED).addItemName(unequiped[0].getItemId());
                }

                sendUserPacket(sm);
                sm = null;
            }
        }

        return true;
    }

    /**
     * Return True if the L2PcInstance use a dual weapon.<BR>
     * <BR>
     */
    @Override
    public boolean isUsingDualWeapon() {
        L2Weapon weaponItem = getActiveWeaponItem();
        if (weaponItem == null) {
            return false;
        }

        if (weaponItem.getItemType() == L2WeaponType.DUAL) {
            return true;
        } else if (weaponItem.getItemType() == L2WeaponType.DUALFIST) {
            return true;
        } else if (weaponItem.getItemId() == 248) // orc fighter fists
        {
            return true;
        } else if (weaponItem.getItemId() == 252) // orc mage fists
        {
            return true;
        } else {
            return false;
        }
    }

    public void setUptime(long time) {
        _uptime = time;
    }

    public long getUptime() {
        return System.currentTimeMillis() - _uptime;
    }

    /**
     * Return True if the L2PcInstance is invulnerable.<BR>
     * <BR>
     */
    @Override
    public boolean isInvul() {
        return _isInvul || _isTeleporting;
    }

    /**
     * Return True if the L2PcInstance has a Party in progress.<BR>
     * <BR>
     */
    @Override
    public boolean isInParty() {
        return _party != null;
    }

    /**
     * Set the _party object of the L2PcInstance (without joining it).<BR>
     * <BR>
     */
    public void setParty(L2Party party) {
        _party = party;
    }

    /**
     * Set the _party object of the L2PcInstance AND join it.<BR>
     * <BR>
     */
    public void joinParty(L2Party party) {
        if (party != null) {
            // First set the party otherwise this wouldn't be considered
            // as in a party into the L2Character.updateEffectIcons() call.
            _party = party;
            party.addPartyMember(this);
            if (_partyRoom != null) {
                PartyWaitingRoomManager.getInstance().refreshRoom(_partyRoom);
            }
        }
    }

    /**
     * Manage the Leave Party task of the L2PcInstance.<BR>
     * <BR>
     */
    public void leaveParty() {
        if (isInParty()) {
            _party.removePartyMember(this);
            _party = null;
            if (_partyRoom != null) {
                PartyWaitingRoomManager.getInstance().exitRoom(this, _partyRoom);
            }
        }
    }

    public void leaveOffParty() {
        if (isInParty()) {
            _party.removePartyMember(this, false);
            _party = null;
            if (_partyRoom != null) {
                PartyWaitingRoomManager.getInstance().exitRoom(this, _partyRoom);
            }
        }
    }

    /**
     * Return the _party object of the L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public L2Party getParty() {
        return _party;
    }

    /**
     * Set the _isGm Flag of the L2PcInstance.<BR>
     * <BR>
     */
    public void setIsGM(boolean f) {
        _isGm = f;
    }

    /**
     * Return True if the L2PcInstance is a GM.<BR>
     * <BR>
     */
    @Override
    public boolean isGM() {
        return _isGm;
    }

    /**
     * Manage a cancel cast task for the L2PcInstance.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Set the Intention of the AI to
     * AI_INTENTION_IDLE</li>
     * <li>Enable all skills (set _allSkillsDisabled to
     * False)</li>
     * <li>Send a Server->Client Packet MagicSkillCanceld to the
     * L2PcInstance and all L2PcInstance in the _KnownPlayers of the L2Character
     * (broadcast)</li><BR>
     * <BR>
     *
     */
    public void cancelCastMagic() {
        // Set the Intention of the AI to AI_INTENTION_IDLE
        getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

        // Enable all skills (set _allSkillsDisabled to False)
        enableAllSkills();

        // Send a Server->Client Packet MagicSkillCanceld to the L2PcInstance and all
        // L2PcInstance in the _KnownPlayers of the L2Character (broadcast)
        // MagicSkillCanceld msc = new MagicSkillCanceld(getObjectId());
        // Broadcast the packet to self and known players.
        Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillCanceld(getObjectId()), 5000);
    }

    /**
     * Set the _accessLevel of the L2PcInstance.<BR>
     * <BR>
     */
    public void setAccessLevel(int level) {
        _accessLevel = level;

        if (_accessLevel > 0 || Config.EVERYBODY_HAS_ADMIN_RIGHTS) {
            setIsGM(true);
        }
    }

    public void setAccountAccesslevel(int level) {
        LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
        if (level < 0) {
            kick();
        }
    }

    /**
     * Return the _accessLevel of the L2PcInstance.<BR>
     * <BR>
     */
    public int getAccessLevel() {
        if (Config.EVERYBODY_HAS_ADMIN_RIGHTS && _accessLevel <= 200) {
            return 200;
        }

        return _accessLevel;
    }

    @Override
    public double getLevelMod() {
        return (100.0 - 11 + getLevel()) / 100.0;
    }

    /**
     * Update Stats of the L2PcInstance client side by sending Server->Client
     * packet UserInfo/StatusUpdate to this L2PcInstance and
     * CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers
     * (broadcast).<BR>
     * <BR>
     */
    @Override
    public void updateAndBroadcastStatus(int broadcastType) {
        refreshOverloaded();
        refreshExpertisePenalty();
        // Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to
        // all L2PcInstance in its _KnownPlayers (broadcast)
        if (broadcastType == 1) {
            sendUserPacket(new UserInfo(this));
        }
        if (broadcastType == 2) {
            broadcastUserInfo();
        }
    }

    /**
     * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the
     * L2PcInstance and all L2PcInstance to inform (broadcast).<BR>
     * <BR>
     */
    public void setKarmaFlag(int flag) {
        // System.out.println("###setKarmaFlag####");
        sendUserPacket(new UserInfo(this));

        FastList<L2Character> chars = getKnownList().getKnownCharacters();
        for (FastList.Node<L2Character> n = chars.head(), end = chars.tail(); (n = n.getNext()) != end;) {
            if (n == null) {
                continue;
            }

            notifyKarmaFlag(n.getValue(), flag);
            /*
             * pc = n.getValue();
             * if (pc == null) {
             * continue;
             * }
             * 
             * pc.sendPacket(new RelationChanged(this, getRelation(pc),
             * isAutoAttackable(pc)));
             * if (getPet() != null) {
             * pc.sendPacket(new RelationChanged(getPet(), getRelation(pc),
             * isAutoAttackable(pc)));
             * }
             */
        }
        /*
         * FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
         * L2PcInstance pc = null;
         * for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n
         * = n.getNext()) != end;) {
         * pc = n.getValue();
         * if (pc == null) {
         * continue;
         * }
         * 
         * pc.sendPacket(new RelationChanged(this, getRelation(pc),
         * isAutoAttackable(pc)));
         * if (getPet() != null) {
         * pc.sendPacket(new RelationChanged(getPet(), getRelation(pc),
         * isAutoAttackable(pc)));
         * }
         * }
         * players.clear();
         * players = null;
         * pc = null;
         */
    }

    private void notifyKarmaFlag(L2Character cha, int flag) {
        if (cha == null) {
            return;
        }

        if (cha.isPlayer()) {
            sendRelationTo(cha.getPlayer());
            return;
        }

        if (flag == 1 && cha.isL2Guard()) {
            notifyGuard(cha.getGuard());
        }
    }

    private void sendRelationTo(L2PcInstance pc) {
        if (pc == null) {
            return;
        }
        pc.sendPacket(new RelationChanged(this, getRelation(pc), isAutoAttackable(pc)));
    }

    private void notifyGuard(L2GuardInstance guard) {
        guard.setTarget(this);
        guard.setRunning();
        guard.addDamageHate(this, 0, 999);
        guard.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
    }

    /**
     * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance
     * and all L2PcInstance to inform (broadcast).<BR>
     * <BR>
     */
    public void broadcastKarma() {
        /*
         * StatusUpdate su = new StatusUpdate(getObjectId());
         * su.addAttribute(StatusUpdate.KARMA, getKarma()); sendPacket(su);
         */
        sendChanges();

        FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
        L2PcInstance pc = null;
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }

            pc.sendPacket(new RelationChanged(this, getRelation(pc), isAutoAttackable(pc)));
            if (getPet() != null) {
                pc.sendPacket(new RelationChanged(getPet(), getRelation(pc), isAutoAttackable(pc)));
            }
        }
        players.clear();
        players = null;
        pc = null;
    }

    /**
     * Set the online Flag to True or False and update the characters table of
     * the database with online status and lastAccess (called when login and
     * logout).<BR>
     * <BR>
     */
    public void setOnlineStatus(boolean f) {
        if (_isOnline != f) {
            _isOnline = f;
        }

        // Update the characters table of the database with online status and lastAccess
        // (called when login and logout)
        updateOnlineStatus();
    }

    public void setIsIn7sDungeon(boolean f) {
        if (_isIn7sDungeon != f) {
            _isIn7sDungeon = f;
        }

        updateIsIn7sDungeonStatus();
    }

    /**
     * Update the characters table of the database with online status and
     * lastAccess of this L2PcInstance (called when login and logout).<BR>
     * <BR>
     */
    public void updateOnlineStatus() {
        if (isInOfflineMode()) {
            return;
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_id=?");
            st.setInt(1, isOnline());
            st.setLong(2, System.currentTimeMillis());
            st.setInt(3, getObjectId());
            st.execute();
        } catch (SQLException e) {
            _log.warning("could not set char online status:" + e);
        } finally {
            Close.CS(con, st);
        }
    }

    public void updateIsIn7sDungeonStatus() {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE characters SET isIn7sDungeon=?, lastAccess=? WHERE obj_id=?");
            st.setInt(1, isIn7sDungeon() ? 1 : 0);
            st.setLong(2, System.currentTimeMillis());
            st.setInt(3, getObjectId());
            st.execute();
        } catch (SQLException e) {
            _log.warning("could not set char isIn7sDungeon status:" + e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Create a new player in the characters table of the database.<BR>
     * <BR>
     */
    private boolean createDb() {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement(
                    "INSERT INTO characters "
                            + "(account_name,obj_Id,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,"
                            + "acc,crit,evasion,mAtk,mDef,mSpd,pAtk,pDef,pSpd,runSpd,walkSpd,"
                            + "str,con,dex,_int,men,wit,face,hairStyle,hairColor,sex,"
                            + "movement_multiplier,attack_speed_multiplier,colRad,colHeight,"
                            + "exp,sp,karma,pvpkills,pkkills,clanid,maxload,race,classid,deletetime,"
                            + "cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,"
                            + "base_class,newbie,nobless,power_grade,last_recom_date,premium) "
                            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            st.setString(1, _accountName);
            st.setInt(2, getObjectId());
            st.setString(3, super.getName());
            st.setInt(4, getLevel());
            st.setInt(5, getMaxHp());
            st.setDouble(6, getCurrentHp());
            st.setInt(7, getMaxCp());
            st.setDouble(8, getCurrentCp());
            st.setInt(9, getMaxMp());
            st.setDouble(10, getCurrentMp());
            st.setInt(11, getAccuracy());
            st.setInt(12, getCriticalHit(null, null));
            st.setInt(13, getEvasionRate(null));
            st.setInt(14, getMAtk(null, null));
            st.setInt(15, getMDef(null, null));
            st.setInt(16, getMAtkSpd());
            st.setInt(17, getPAtk(null));
            st.setInt(18, getPDef(null));
            st.setInt(19, getPAtkSpd());
            st.setInt(20, getRunSpeed());
            st.setInt(21, getWalkSpeed());
            st.setInt(22, getSTR());
            st.setInt(23, getCON());
            st.setInt(24, getDEX());
            st.setInt(25, getINT());
            st.setInt(26, getMEN());
            st.setInt(27, getWIT());
            st.setInt(28, getAppearance().getFace());
            st.setInt(29, getAppearance().getHairStyle());
            st.setInt(30, getAppearance().getHairColor());
            st.setInt(31, getAppearance().getSex() ? 1 : 0);
            st.setDouble(32, 1/*
                               * getMovementMultiplier()
                               */);
            st.setDouble(33, 1/*
                               * getAttackSpeedMultiplier()
                               */);
            st.setDouble(34, getTemplate().collisionRadius/*
                                                           * getCollisionRadius()
                                                           */);
            st.setDouble(35, getTemplate().collisionHeight/*
                                                           * getCollisionHeight()
                                                           */);
            st.setLong(36, getExp());
            st.setInt(37, getSp());
            st.setInt(38, getKarma());
            st.setInt(39, getPvpKills());
            st.setInt(40, getPkKills());
            st.setInt(41, getClanId());
            st.setInt(42, getMaxLoad());
            st.setInt(43, getRace().ordinal());
            st.setInt(44, getClassId().getId());
            st.setLong(45, getDeleteTimer());
            st.setInt(46, hasDwarvenCraft() ? 1 : 0);
            if (!Config.STARTUP_TITLE.equalsIgnoreCase("off")) {
                setTitle(Config.STARTUP_TITLE);
            }
            st.setString(47, getTitle());

            st.setInt(48, getAccessLevel());
            st.setInt(49, isOnline());
            st.setInt(50, isIn7sDungeon() ? 1 : 0);
            st.setInt(51, getClanPrivileges());
            st.setInt(52, getWantsPeace());
            st.setInt(53, getBaseClass());
            st.setInt(54, isNewbie() ? 1 : 0);
            st.setInt(55, isNoble() ? 1 : 0);
            st.setLong(56, 0);
            st.setLong(57, System.currentTimeMillis());
            if (Config.PREMIUM_ENABLE && Config.PREMIUM_START_DAYS > 0) {
                st.setLong(58, (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(Config.PREMIUM_START_DAYS)));
            } else {
                st.setLong(58, 0);
            }
            st.executeUpdate();
            Close.S(st);

            //
            setAutoLoot(Config.VS_AUTOLOOT_VAL == 1 ? true : false);
            setGeoPathfind(Config.VS_PATHFIND_VAL == 1 ? true : false);
            setShowSkillChances(Config.VS_SKILL_CHANCES_VAL == 1 ? true : false);
            setSoulShotsAnim(Config.SOULSHOT_ANIM);

            st = con.prepareStatement(
                    "INSERT INTO `character_settings` (`char_obj_id`, `autoloot`, `pathfind`, `skillchances`) VALUES (?, ?, ?, ?)");
            st.setInt(1, getObjectId());
            st.setInt(2, Config.VS_AUTOLOOT_VAL);
            st.setInt(3, Config.VS_PATHFIND_VAL);
            st.setInt(4, Config.VS_SKILL_CHANCES_VAL);
            st.execute();
            Close.S(st);

            //
            if (Config.POST_CHARBRIEF) {
                TextBuilder text = new TextBuilder();
                text.append(Config.POST_BRIEFTEXT);
                st = con.prepareStatement(
                        "INSERT INTO `z_bbs_mail` (`from`, `to`, `tema`, `text`, `datetime`, `read`, `item_id`, `item_count`, `item_ench`, `aug_hex`, `aug_id`, `aug_lvl`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                st.setInt(1, 555);
                st.setInt(2, getObjectId());
                st.setString(3, Config.POST_BRIEFTHEME);
                st.setString(4, text.toString());
                st.setLong(5, System.currentTimeMillis());
                st.setInt(6, 0);
                st.setInt(7, Config.POST_BRIEF_ITEM.id);
                st.setInt(8, Config.POST_BRIEF_ITEM.count);
                st.setInt(9, 0);
                st.setInt(10, 0);
                st.setInt(11, 0);
                st.setInt(12, 0);
                st.execute();
                text.clear();
                text = null;
                Close.S(st);
            }
        } catch (SQLException e) {
            _log.severe("Could not insert char data: " + e);
            return false;
        } finally {
            Close.CS(con, st);
        }
        return true;
    }

    /**
     * Retrieve a L2PcInstance from the characters table of the database and add
     * it in _allObjects of the L2world.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Retrieve the L2PcInstance from the
     * characters table of the database</li>
     * <li>Add the L2PcInstance object in
     * _allObjects</li>
     * <li>Set the x,y,z position of the L2PcInstance and make
     * it invisible</li>
     * <li>Update the overloaded status of the
     * L2PcInstance</li><BR>
     * <BR>
     *
     * @param objectId Identifier of the object to initialized
     *
     * @return The L2PcInstance loaded from the database
     *
     */
    private static L2PcInstance restore(int objectId) {
        L2PcInstance player = null;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        PreparedStatement st2 = null;
        ResultSet rs2 = null;
        PreparedStatement st3 = null;
        try {
            // Retrieve the L2PcInstance from the characters table of the database
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            st = con.prepareStatement(
                    "SELECT account_name, obj_Id, char_name, name_color, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, expBeforeDeath, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, title_color, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, in_jail, jail_timer, newbie, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level,hero,premium,chatban_timer,chatban_reason,chat_filter_count,deaths,lang FROM characters WHERE obj_Id=? LIMIT 1");
            st.setInt(1, objectId);
            rset = st.executeQuery();

            double currentCp = 0;
            double currentHp = 0;
            double currentMp = 0;

            if (rset.next()) {
                final int activeClassId = rset.getInt("classid");
                final boolean female = rset.getInt("sex") != 0;
                final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
                PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"),
                        rset.getByte("hairStyle"), female);

                player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
                player.setClassUpdate(true);
                // player.setName(rset.getString("char_name"));
                player.getAppearance().setNameColor(Integer.decode("0x" + rset.getString("name_color")));
                player._lastAccess = rset.getLong("lastAccess");

                player.loadOfflineTrade(con);

                player.getStat().setExp(rset.getLong("exp"));
                player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
                player.getStat().setLevel(rset.getByte("level"));
                player.getStat().setSp(rset.getInt("sp"));

                player.setWantsPeace(rset.getInt("wantspeace"));

                player.setHeading(rset.getInt("heading"));

                player.setKarma(rset.getInt("karma"));
                player.setPvpKills(rset.getInt("pvpkills"));
                player.setDeaths(rset.getInt("deaths"));
                player.setPkKills(rset.getInt("pkkills"));
                player.setOnlineTime(rset.getLong("onlinetime"));
                player.setNewbie(rset.getInt("newbie") == 1);
                player.setNoble(rset.getInt("nobless") == 1);
                // player.setFourSide(rset.getInt("lang"));

                // премиум
                long premiumExpire = rset.getLong("premium");
                if (Config.PREMIUM_ENABLE && premiumExpire > 0) {
                    if (premiumExpire > System.currentTimeMillis()) {
                        player.setPremium(true);
                        EffectTaskManager.getInstance().schedule(new StopPremium(objectId),
                                (premiumExpire - System.currentTimeMillis()));
                    } else if (premiumExpire > 1) {
                        premiumExpire = -1;
                        st3 = con.prepareStatement("UPDATE `characters` SET `premium`=? WHERE `obj_Id`=?");
                        st3.setInt(1, 0);
                        st3.setInt(2, objectId);
                        st3.execute();
                        Close.S(st3);
                        if (Config.PREMIUM_ITEMS) {
                            for (L2ItemInstance item : player.getInventory().getItems()) {
                                if (item == null) {
                                    continue;
                                }

                                if (item.isPremiumItem()) {
                                    player.getInventory().unEquipItemInBodySlotAndRecord(
                                            player.getInventory().getSlotFromItem(item));
                                }
                            }
                        }
                    }
                }
                player.setPremiumExpire(premiumExpire);
                player.setName(rset.getString("char_name"));

                player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
                if (player.getClanJoinExpiryTime() < System.currentTimeMillis()) {
                    player.setClanJoinExpiryTime(0);
                }
                player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
                if (player.getClanCreateExpiryTime() < System.currentTimeMillis()) {
                    player.setClanCreateExpiryTime(0);
                }

                int clanId = rset.getInt("clanid");
                player.setPowerGrade((int) rset.getLong("power_grade"));
                player.setPledgeType(rset.getInt("subpledge"));
                player.setLastRecomUpdate(rset.getLong("last_recom_date"));
                // player.setApprentice(rset.getInt("apprentice"));

                if (clanId > 0) {
                    player.setClan(ClanTable.getInstance().getClan(clanId));
                }

                if (player.getClan() != null) {
                    if (player.getClan().getLeaderId() != player.getObjectId()) {
                        if (player.getPowerGrade() == 0) {
                            player.setPowerGrade(5);
                        }
                        player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
                    } else {
                        player.setClanPrivileges(L2Clan.CP_ALL);
                        player.setPowerGrade(1);
                    }
                } else {
                    player.setClanPrivileges(L2Clan.CP_NOTHING);
                }

                player.setDeleteTimer(rset.getLong("deletetime"));

                player.setTitle(rset.getString("title"));
                player.getAppearance().setTitleColor(Integer.decode("0x" + rset.getString("title_color")));
                player.setAccessLevel(rset.getInt("accesslevel"));
                player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
                player.setUptime(System.currentTimeMillis());

                currentHp = rset.getDouble("curHp");
                player.setCurrentHp(rset.getDouble("curHp"));
                currentCp = rset.getDouble("curCp");
                player.setCurrentCp(rset.getDouble("curCp"));
                currentMp = rset.getDouble("curMp");
                player.setCurrentMp(rset.getDouble("curMp"));

                // Check recs
                player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));

                player._classIndex = 0;
                try {
                    player.setBaseClass(rset.getInt("base_class"));
                } catch (Exception e) {
                    player.setBaseClass(activeClassId);
                }

                // Restore Subclass Data (cannot be done earlier in function)
                if (restoreSubClassData(player)) {
                    if (activeClassId != player.getBaseClass()) {
                        for (SubClass subClass : player.getSubClasses().values()) {
                            if (subClass.getClassId() == activeClassId) {
                                player._classIndex = subClass.getClassIndex();
                            }
                        }
                    }
                }
                if (player.getClassIndex() == 0 && activeClassId != player.getBaseClass()) {
                    // Subclass in use but doesn't exist in DB -
                    // a possible restart-while-modifysubclass cheat has been attempted.
                    // Switching to use base class
                    player.setClassId(player.getBaseClass());
                    _log.warning("Player " + player.getName()
                            + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
                } else {
                    player._activeClass = activeClassId;
                }

                // оффтрейд
                if (player.getVar("storemode") != null && player.getVar("offline") != null) {
                    player.restoreTradeList();
                    player.setPrivateStoreType(Integer.parseInt(player.getVar("storemode")));
                    player.sitDown();
                } else {
                    player.unsetVar("storemode", null);
                }

                // покупное герйоство
                long heroExpire = rset.getLong("hero");
                if (Config.EVERYBODE_HERO) {
                    heroExpire = 1;
                }
                if (heroExpire > 0) {
                    if (heroExpire == 1 || (heroExpire > System.currentTimeMillis())) {
                        player.setHero(true);
                        if (heroExpire > System.currentTimeMillis()) {
                            EffectTaskManager.getInstance().schedule(new StopHero(objectId),
                                    (heroExpire - System.currentTimeMillis()));
                        }
                    } else if (heroExpire > 1) {
                        heroExpire = -1;
                        st3 = con.prepareStatement("UPDATE `characters` SET `hero`=? WHERE `obj_Id`=?");
                        st3.setInt(1, 0);
                        st3.setInt(2, objectId);
                        st3.execute();
                        Close.S(st3);
                        st3 = con.prepareStatement(
                                "DELETE FROM `items` WHERE `item_id` IN ('6611', '6612', '6613', '6614', '6615', '6616', '6617', '6618', '6619', '6620', '6621', '6842') AND `owner_id` = ?");
                        st3.setInt(1, objectId);
                        st3.execute();
                        Close.S(st3);
                    }
                    player.setHeroExpire(heroExpire);
                }

                if (Hero.getInstance().isHero(player.getObjectId())) {
                    player.setHero(true);
                }

                player.setApprentice(rset.getInt("apprentice"));
                player.setSponsor(rset.getInt("sponsor"));
                player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
                player.setIsIn7sDungeon((rset.getInt("isin7sdungeon") == 1) ? true : false);
                player.setInJail((rset.getInt("in_jail") == 1) ? true : false);
                if (player.isInJail()) {
                    player.setJailTimer(rset.getLong("jail_timer"));
                } else {
                    player.setJailTimer(0);
                }

                CursedWeaponsManager.getInstance().checkPlayer(player);

                player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));

                player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));

                player.setPcPoints(rset.getInt("chat_filter_count"));

                // Add the L2PcInstance object in _allObjects
                // L2World.getInstance().storeObject(player);
                // Set the x,y,z position of the L2PcInstance and make it invisible
                player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));

                try {
                    st2 = con.prepareStatement(
                            "SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id<>?");
                    st2.setString(1, player._accountName);
                    st2.setInt(2, objectId);
                    rs2 = st2.executeQuery();

                    while (rs2.next()) {
                        Integer charId = rs2.getInt("obj_Id");
                        String charName = rs2.getString("char_name");
                        player._chars.put(charId, charName);
                    }
                } catch (SQLException e) {
                    _log.severe("Oops" + e);
                } finally {
                    Close.SR(st2, rs2);
                }

                // настройки чара
                try {
                    st2 = con.prepareStatement(
                            "SELECT no_exp, no_requests, autoloot, chatblock, charkey, traders, pathfind, skillchances, showshots FROM character_settings WHERE char_obj_id=? LIMIT 1");
                    st2.setInt(1, objectId);
                    rs2 = st2.executeQuery();
                    if (rs2.next()) {
                        player.setNoExp(rs2.getInt("no_exp") == 1 ? true : false);
                        player.setAlone(rs2.getInt("no_requests") == 1 ? true : false);
                        player.setAutoLoot(rs2.getInt("autoloot") == 1 ? true : false);
                        player.setChatIgnore(rs2.getInt("chatblock"));
                        player.checkUserKey(rs2.getString("charkey"));
                        player.setTradersIgnore(rs2.getLong("traders"));
                        player.setGeoPathfind(rs2.getInt("pathfind") == 1 ? true : false);
                        player.setShowSkillChances(rs2.getInt("skillchances") == 1 ? true : false);
                        player.setSoulShotsAnim(rs2.getInt("showshots") == 1 ? true : false);
                    }
                } catch (SQLException e) {
                    _log.severe("Oops2" + e);
                } finally {
                    Close.SR(st2, rs2);
                }

                // гилд мод / Storium
                if (Config.ALLOW_GUILD_MOD) {
                    try {
                        st2 = con.prepareStatement("SELECT side, penalty FROM z_guild_mod WHERE char_id=? LIMIT 1");
                        st2.setInt(1, objectId);
                        rs2 = st2.executeQuery();
                        if (rs2.next()) {
                            player.setGuildSide(rs2.getInt("side"));
                            player.setGuildPenalty(rs2.getInt("penalty"));
                        }
                    } catch (SQLException e) {
                        _log.severe("Oops3" + e);
                    } finally {
                        Close.SR(st2, rs2);
                    }
                }

                // player.restoreCharData(con);
                player.restoreSkills(con); // Retrieve from the database all skills of this L2PcInstance and add them to
                                           // _skills.
                player.getMacroses().restore(con); // Retrieve from the database all macroses of this L2PcInstance and
                                                   // add them to _macroses.
                player.getShortCuts().restore(con); // Retrieve from the database all shortCuts of this L2PcInstance and
                                                    // add them to _shortCuts.
                player.restoreHenna(con); // Retrieve from the database all henna of this L2PcInstance and add them to
                                          // _henna.
                if (Config.ALT_RECOMMEND) // Retrieve from the database all recom data of this L2PcInstance and add to
                                          // _recomChars.
                {
                    player.restoreRecom(con);
                }
                if (!player.isSubClassActive()) // Retrieve from the database the recipe book of this L2PcInstance.
                {
                    player.restoreRecipeBook(con);
                }

                player.rewardSkills();
                player.refreshOverloaded();
                player.setClassUpdate(false);

                player.setEnterWorldHp(currentHp);
                player.setEnterWorldMp(currentMp);
                player.setEnterWorldCp(currentCp);

                player.restoreProfileBuffs(con);
                player.checkPvpTitle();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            _log.severe("Could not restore char data: " + e.getMessage());
        } finally {
            Close.S(st3);
            Close.SR(st2, rs2);
            Close.CSR(con, st, rset);
        }
        return player;
    }

    // заглушки
    public void setEnterWorldHp(double Hp) {
        _enterWorldHp = Hp;
    }

    public double getEnterWorldHp() {
        return _enterWorldHp;
    }

    public void setEnterWorldMp(double Mp) {
        _enterWorldMp = Mp;
    }

    public double getEnterWorldMp() {
        return _enterWorldMp;
    }

    public void setEnterWorldCp(double Cp) {
        _enterWorldCp = Cp;
    }

    public double getEnterWorldCp() {
        return _enterWorldCp;
    }

    /**
     * @return
     */
    public Forum getMail() {
        if (_forumMail == null) {
            setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));

            if (_forumMail == null) {
                ForumsBBSManager.getInstance().createNewForum(getName(),
                        ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY,
                        getObjectId());
                setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
            }
        }

        return _forumMail;
    }

    /**
     * @param forum
     */
    public void setMail(Forum forum) {
        _forumMail = forum;
    }

    /**
     * @return
     */
    public Forum getMemo() {
        if (_forumMemo == null) {
            setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));

            if (_forumMemo == null) {
                ForumsBBSManager.getInstance().createNewForum(_accountName,
                        ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY,
                        getObjectId());
                setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
            }
        }

        return _forumMemo;
    }

    /**
     * @param forum
     */
    public void setMemo(Forum forum) {
        _forumMemo = forum;
    }

    /**
     * Restores sub-class data for the L2PcInstance, used to check the current
     * class index for the character.
     */
    private static boolean restoreSubClassData(L2PcInstance player) {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement(
                    "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE char_obj_id=? ORDER BY class_index ASC");
            st.setInt(1, player.getObjectId());

            rset = st.executeQuery();

            while (rset.next()) {
                SubClass subClass = new SubClass();
                subClass.setClassId(rset.getInt("class_id"));
                subClass.setLevel(rset.getByte("level"));
                subClass.setExp(rset.getLong("exp"));
                subClass.setSp(rset.getInt("sp"));
                subClass.setClassIndex(rset.getInt("class_index"));

                // Enforce the correct indexing of _subClasses against their class indexes.
                player.getSubClasses().put(subClass.getClassIndex(), subClass);
            }
        } catch (Exception e) {
            _log.warning("Could not restore classes for " + player.getName() + ": " + e);
            e.printStackTrace();
        } finally {
            Close.CSR(con, st, rset);
        }

        return true;
    }

    /**
     * Restores secondary data for the L2PcInstance, based on the current class
     * index.
     */
    /*
     * private void restoreCharData(Connect con) { // Retrieve from the database
     * all skills of this L2PcInstance and add them to _skills.
     * restoreSkills(con);
     *
     * // Retrieve from the database all macroses of this L2PcInstance and add
     * them to _macroses. _macroses.restore(con);
     *
     * // Retrieve from the database all shortCuts of this L2PcInstance and add
     * them to _shortCuts. _shortCuts.restore(con);
     *
     * // Retrieve from the database all henna of this L2PcInstance and add
     * them to _henna. restoreHenna(con);
     *
     * // Retrieve from the database all recom data of this L2PcInstance and
     * add to _recomChars. if (Config.ALT_RECOMMEND) restoreRecom(con);
     *
     * // Retrieve from the database the recipe book of this L2PcInstance. if
     * (!isSubClassActive()) restoreRecipeBook(con); }
     */
    /**
     * Restore recipe book data for this L2PcInstance.
     */
    private void restoreRecipeBook(Connect con) {
        // Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            // con = L2DatabaseFactory.get();
            // con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT id, type FROM character_recipebook WHERE char_id=?");
            st.setInt(1, getObjectId());
            rset = st.executeQuery();

            L2RecipeList recipe = null;
            RecipeController rc = RecipeController.getInstance();
            while (rset.next()) {
                recipe = rc.getRecipeList(rset.getInt("id") - 1);
                if (recipe == null) {
                    continue;
                }

                if (rset.getInt("type") == 1) {
                    registerDwarvenRecipeList(recipe);
                } else {
                    registerCommonRecipeList(recipe);
                }
            }
        } catch (Exception e) {
            _log.warning("Could not restore recipe book data:" + e);
        } finally {
            Close.SR(st, rset);
        }
    }

    /**
     * Update L2PcInstance stats in the characters table of the
     * database.<BR>
     * <BR>
     */
    public synchronized void store() {
        if (_fantome) {
            return;
        }

        // update client coords, if these look like true
        // if (isInsideRadius(getX(), getY(), 1000, true))
        setXYZ(getX(), getY(), getZ());

        storeCharBase();
    }

    private void storeCharBase() {
        Connect con = null;
        PreparedStatement st = null;
        try {
            // Get the exp, level, and sp of base class to store in base table
            int currentClassIndex = getClassIndex();
            _classIndex = 0;
            long exp = getStat().getExp();
            int level = getStat().getLevel();
            int sp = getStat().getSp();
            _classIndex = currentClassIndex;

            con = L2DatabaseFactory.get();

            // Update base class
            st = con.prepareStatement(
                    "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,str=?,con=?,dex=?,_int=?,men=?,wit=?,face=?,hairStyle=?,hairColor=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,maxload=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,in_jail=?,jail_timer=?,newbie=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,chat_filter_count=?, name_color=?, title_color=?, sex=?, hero=?, deaths=?, lang=? WHERE obj_id=?");
            st.setInt(1, level);
            st.setInt(2, getMaxHp());
            st.setDouble(3, getCurrentHp());
            st.setInt(4, getMaxCp());
            st.setDouble(5, getCurrentCp());
            st.setInt(6, getMaxMp());
            st.setDouble(7, getCurrentMp());
            st.setInt(8, getSTR());
            st.setInt(9, getCON());
            st.setInt(10, getDEX());
            st.setInt(11, getINT());
            st.setInt(12, getMEN());
            st.setInt(13, getWIT());
            st.setInt(14, getAppearance().getFace());
            st.setInt(15, getAppearance().getHairStyle());
            st.setInt(16, getAppearance().getHairColor());
            st.setInt(17, getHeading());
            st.setInt(18, getX());
            st.setInt(19, getY());
            st.setInt(20, getZ());
            st.setLong(21, exp);
            st.setLong(22, getExpBeforeDeath());
            st.setInt(23, sp);
            st.setInt(24, getKarma());
            st.setInt(25, getPvpKills());
            st.setInt(26, getPkKills());
            st.setInt(27, getRecomHave());
            st.setInt(28, getRecomLeft());
            st.setInt(29, _clanId);
            st.setInt(30, getMaxLoad());
            st.setInt(31, getRace().ordinal());

            // if (!isSubClassActive())
            // else
            // st.setInt(30, getBaseTemplate().race.ordinal());
            st.setInt(32, getClassId().getId());
            st.setLong(33, getDeleteTimer());
            st.setString(34, getTitle());
            st.setInt(35, getAccessLevel());
            st.setInt(36, isOnline());
            st.setInt(37, isIn7sDungeon() ? 1 : 0);
            st.setInt(38, getClanPrivileges());
            st.setInt(39, getWantsPeace());
            st.setInt(40, getBaseClass());

            long totalOnlineTime = _onlineTime;

            if (_onlineBeginTime > 0) {
                totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
            }

            st.setLong(41, totalOnlineTime);
            st.setInt(42, isInJail() ? 1 : 0);
            st.setLong(43, getJailTimer());
            st.setInt(44, isNewbie() ? 1 : 0);
            st.setInt(45, isNoble() ? 1 : 0);
            st.setLong(46, getPowerGrade());
            st.setInt(47, getPledgeType());
            st.setLong(48, getLastRecomUpdate());
            st.setInt(49, getLvlJoinedAcademy());
            st.setLong(50, getApprentice());
            st.setLong(51, getSponsor());
            st.setInt(52, getAllianceWithVarkaKetra());
            st.setLong(53, getClanJoinExpiryTime());
            st.setLong(54, getClanCreateExpiryTime());
            st.setString(55, super.getName());
            st.setLong(56, getDeathPenaltyBuffLevel());
            st.setInt(57, getPcPoints());
            st.setString(58, convertColor(Integer.toHexString(getAppearance().getNameColor()).toUpperCase()));
            st.setString(59, convertColor(Integer.toHexString(getAppearance().getTitleColor()).toUpperCase()));
            st.setInt(60, getAppearance().getSex() ? 1 : 0);
            st.setLong(61, _heroExpire);
            st.setLong(62, getDeaths());
            st.setString(63, _lang);
            st.setInt(64, getObjectId());
            st.execute();

            storeCharSub(con);
            storeCharSettings(con);
            storeEffect(con);
            storeRecipeBook(con);
            _macroses.store(con);
            _shortCuts.store(con);
        } catch (SQLException e) {
            _log.warning("Could not store char base data: " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    private void storeCharSub(Connect con) {
        if (getTotalSubClasses() > 0) {
            PreparedStatement st = null;
            try {
                con.setAutoCommit(false);
                st = con.prepareStatement(
                        "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE char_obj_id=? AND class_index=?");
                for (SubClass subClass : getSubClasses().values()) {
                    st.setLong(1, subClass.getExp());
                    st.setInt(2, subClass.getSp());
                    st.setInt(3, subClass.getLevel());
                    st.setInt(4, subClass.getClassId());
                    st.setInt(5, getObjectId());
                    st.setInt(6, subClass.getClassIndex());
                    st.addBatch();
                }
                st.executeBatch();
                con.commit();
                con.setAutoCommit(true);
            } catch (SQLException e) {
                // con.rollback();
                _log.warning("Could not store sub class data for " + getName() + ": " + e);
            } finally {
                Close.S(st);
            }
        }
    }

    private void storeCharSettings(Connect con) {
        PreparedStatement st = null;
        try {
            // st = con.prepareStatement("UPDATE `character_settings` SET `no_exp`=?,
            // `no_requests`=?, `autoloot`=?, `chatblock`=?, `charkey`=?, `traders`=?,
            // `pathfind`=?, `skillchances`=? WHERE `char_obj_id`=?");
            st = con.prepareStatement(
                    "UPDATE `character_settings` SET `no_exp`=?, `no_requests`=?, `autoloot`=?, `chatblock`=?, `traders`=?, `pathfind`=?, `skillchances`=? WHERE `char_obj_id`=?");
            st.setInt(1, isNoExp() ? 1 : 0);
            st.setInt(2, isAlone() ? 1 : 0);
            st.setInt(3, getAutoLoot() ? 1 : 0);
            st.setInt(4, getChatIgnore());
            // st.setString(5, getUserKey().key);
            st.setLong(5, _tradersIgnore);
            st.setInt(6, geoPathfind() ? 1 : 0);
            st.setInt(7, getShowSkillChances() ? 1 : 0);
            st.setInt(8, getObjectId());
            st.execute();

            /*
             * if (isInGuild()) {
             * Close.S(st);
             * st =
             * con.prepareStatement("UPDATE `z_guild_mod` SET `penalty`=? WHERE `char_id`=?"
             * );
             * st.setInt(1, getGuildPenalty());
             * st.setInt(2, getObjectId());
             * st.execute();
             * }
             */
        } catch (SQLException e) {
            _log.warning("storeCharSettings() error: " + e);
        } finally {
            Close.S(st);
        }
    }

    private void storeEffect(Connect con) {
        if (!Config.STORE_SKILL_COOLTIME) {
            return;
        }

        PreparedStatement st = null;
        try {
            // Delete all current stored effects for char to avoid dupe
            st = con.prepareStatement("DELETE FROM character_buffs WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, getClassIndex());
            st.execute();
            Close.S(st);

            int buff_index = 0;
            // Store all effect data along with calulated remaining
            // reuse delays for matching skills. 'restore_type'= 0
            con.setAutoCommit(false);
            List<Integer> storedSkills = new FastList<Integer>();
            st = con.prepareStatement(
                    "INSERT INTO character_buffs (char_obj_id,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)");

            FastTable<L2Effect> effects = getAllEffectsTable();
            for (int i = 0, n = effects.size(); i < n; i++) {
                L2Effect effect = effects.get(i);
                if (effect == null) {
                    continue;
                }

                if (!effect.isStoreable()) {
                    continue;
                }

                int skillId = effect.getSkill().getId();
                if (storedSkills.contains(skillId)) {
                    continue;
                }

                if (effect.getSkill().isAugment() && _activeAug != skillId) {
                    continue;
                }

                storedSkills.add(skillId);

                if (effect.getInUse()) {
                    buff_index++;

                    st.setInt(1, getObjectId());
                    st.setInt(2, skillId);
                    st.setInt(3, effect.getSkill().getLevel());
                    st.setInt(4, effect.getCount());
                    st.setInt(5, effect.getTime());

                    DisabledSkill ds = _disabledSkills.get(skillId);
                    if (ds != null && ds.isInReuse()) {
                        st.setInt(6, ds.delay);
                        st.setLong(7, Math.max(ds.expire - System.currentTimeMillis(), 1));
                        _disabledSkills.remove(skillId);
                    } else {
                        st.setInt(6, 0);
                        st.setInt(7, 0);
                    }

                    st.setInt(8, 0);
                    st.setInt(9, getClassIndex());
                    st.setInt(10, buff_index);
                    st.addBatch();
                }
            }
            st.executeBatch();
            // Close.S(st);
            // Store the reuse delays of remaining skills which
            // lost effect but still under reuse delay. 'restore_type' 1.
            for (Map.Entry<Integer, DisabledSkill> entry : _disabledSkills.entrySet()) {
                Integer id = entry.getKey();
                DisabledSkill ds = entry.getValue();
                if (id == null || ds == null) {
                    continue;
                }

                if (!ds.isInReuse()) {
                    continue;
                }

                buff_index++;

                if (storedSkills.contains(id)) {
                    continue;
                }
                storedSkills.add(id);

                st.setInt(1, getObjectId());
                st.setInt(2, id);
                st.setInt(3, -1);
                st.setInt(4, -1);
                st.setInt(5, -1);
                st.setLong(6, ds.delay);
                st.setLong(7, Math.max(ds.expire - System.currentTimeMillis(), 1));
                st.setInt(8, 1);
                st.setInt(9, getClassIndex());
                st.setInt(10, buff_index);
                st.addBatch();
            }

            st.executeBatch();
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            // con.rollback();
            _log.warning("Could not store char effect data: " + e);
        } finally {
            Close.S(st);
        }
    }

    /**
     * Store recipe book data for this L2PcInstance, if not on an active
     * sub-class.
     */
    private void storeRecipeBook(Connect con) {
        // If the player is on a sub-class don't even attempt to store a recipe book.
        if (isSubClassActive()) {
            return;
        }
        if (getCommonRecipeBook().length == 0 && getDwarvenRecipeBook().length == 0) {
            return;
        }

        PreparedStatement st = null;
        try {
            st = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
            st.setInt(1, getObjectId());
            st.execute();
            Close.S(st);

            L2RecipeList[] recipes = getCommonRecipeBook();

            con.setAutoCommit(false);
            st = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) values(?,?,0)");
            for (int count = 0; count < recipes.length; count++) {
                st.setInt(1, getObjectId());
                st.setInt(2, recipes[count].getId());
                st.addBatch();
            }
            st.executeBatch();
            Close.S(st);

            recipes = getDwarvenRecipeBook();
            st = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) values(?,?,1)");
            for (int count = 0; count < recipes.length; count++) {
                st.setInt(1, getObjectId());
                st.setInt(2, recipes[count].getId());
                st.addBatch();
            }
            st.executeBatch();
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            // con.rollback();
            _log.warning("Could not store recipe book data: " + e);
        } finally {
            Close.S(st);
        }
    }

    /**
     * Return True if the L2PcInstance is on line.<BR>
     * <BR>
     */
    @Override
    public int isOnline() {
        return (_isOnline ? 1 : 0);
    }

    public boolean isIn7sDungeon() {
        return _isIn7sDungeon;
    }

    /**
     * Add a skill to the L2PcInstance _skills and its Func objects to the
     * calculator set of the L2PcInstance and save update in the
     * character_skills table of the database.<BR>
     * <BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All skills own by a L2PcInstance are
     * identified in <B>_skills</B><BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Replace oldSkill by newSkill or Add
     * the newSkill</li>
     * <li>If an old skill has been replaced, remove all its
     * Func objects of L2Character calculator set</li>
     * <li>Add Func objects of
     * newSkill to the calculator set of the L2Character</li><BR>
     * <BR>
     *
     * @param newSkill The L2Skill to add to the L2Character
     *
     * @return The L2Skill replaced or null if just added a new L2Skill
     *
     */
    public L2Skill addSkill(L2Skill newSkill, boolean store) {
        // Add a skill to the L2PcInstance _skills and its Func objects to the
        // calculator set of the L2PcInstance
        L2Skill oldSkill = super.addSkill(newSkill);

        // Add or update a L2PcInstance skill in the character_skills table of the
        // database
        if (store) {
            storeSkill(newSkill, oldSkill, -1);
        }
        return oldSkill;
    }

    public L2Skill removeSkill(L2Skill skill, boolean store) {
        if (store) {
            return removeSkill(skill);
        } else {
            return super.removeSkill(skill);
        }
    }

    /**
     * Remove a skill from the L2Character and its Func objects from calculator
     * set of the L2Character and save update in the character_skills table of
     * the database.<BR>
     * <BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All skills own by a L2Character are
     * identified in <B>_skills</B><BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Remove the skill from the
     * L2Character _skills</li>
     * <li>Remove all its Func objects from the
     * L2Character calculator set</li><BR>
     * <BR>
     *
     * <B><U> Overriden in </U> :</B><BR>
     * <BR>
     * <li>L2PcInstance : Save update in
     * the character_skills table of the database</li><BR>
     * <BR>
     *
     * @param skill The L2Skill to remove from the L2Character
     *
     * @return The L2Skill removed
     *
     */
    @Override
    public L2Skill removeSkill(L2Skill skill) {
        // Remove a skill from the L2Character and its Func objects from calculator set
        // of the L2Character
        L2Skill oldSkill = super.removeSkill(skill);
        if (oldSkill != null) {
            Connect con = null;
            PreparedStatement st = null;
            try {
                // Remove or update a L2PcInstance skill from the character_skills table of the
                // database
                con = L2DatabaseFactory.get();

                st = con.prepareStatement(
                        "DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?");
                st.setInt(1, oldSkill.getId());
                st.setInt(2, getObjectId());
                st.setInt(3, getClassIndex());
                st.execute();
            } catch (SQLException e) {
                _log.warning("Error could not delete skill: " + e);
            } finally {
                Close.CS(con, st);
            }
        }

        if (_shortCuts != null) {
            /*
             * FastTable <L2ShortCut> allShortCuts = new
             * FastTable<L2ShortCut>(); allShortCuts.addAll(getAllShortCuts());
             *
             * for (int i = 0, n = allShortCuts.size(); i < n; i++) { L2ShortCut
             * sc = allShortCuts.get(i); if (sc == null) continue;
             *
             * if (skill != null && sc.getId() == skill.getId() && sc.getType()
             * == L2ShortCut.TYPE_SKILL) deleteShortCut(sc.getSlot(),
             * sc.getPage()); } allShortCuts.clear();
             */
            for (L2ShortCut sc : getAllShortCuts()) {
                if (sc == null) {
                    continue;
                }

                if (skill != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL) {
                    deleteShortCut(sc.getSlot(), sc.getPage());
                }
            }
        }
        return oldSkill;
    }

    /**
     * Add or update a L2PcInstance skill in the character_skills table of the
     * database. <BR>
     * <BR>
     * If newClassIndex > -1, the skill will be stored with
     * that class index, not the current one.
     */
    private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex) {
        int classIndex = _classIndex;

        if (newClassIndex > -1) {
            classIndex = newClassIndex;
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();

            if (oldSkill != null && newSkill != null) {
                st = con.prepareStatement(
                        "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=? AND class_index=?");
                st.setInt(1, newSkill.getLevel());
                st.setInt(2, oldSkill.getId());
                st.setInt(3, getObjectId());
                st.setInt(4, classIndex);
                st.execute();
                Close.S(st);
            } else if (newSkill != null) {
                st = con.prepareStatement(
                        "REPLACE INTO character_skills (char_obj_id,skill_id,skill_level,skill_name,class_index) VALUES (?,?,?,?,?)");
                st.setInt(1, getObjectId());
                st.setInt(2, newSkill.getId());
                st.setInt(3, newSkill.getLevel());
                st.setString(4, newSkill.getName());
                st.setInt(5, classIndex);
                st.execute();
                Close.S(st);
            } else {
                _log.warning("could not store new skill. its NULL");
            }
        } catch (SQLException e) {
            _log.warning("Error could not store char skills: " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Retrieve from the database all skills of this L2PcInstance and add them
     * to _skills.<BR>
     * <BR>
     */
    private void restoreSkills(Connect con) {
        // Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            // Retrieve all skills of this L2PcInstance from the database
            // con = L2DatabaseFactory.get();
            // con.setTransactionIsolation(1);
            st = con.prepareStatement(Config.ALT_SUBCLASS_SKILLS
                    ? "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=?"
                    : "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());

            if (!Config.ALT_SUBCLASS_SKILLS) {
                st.setInt(2, getClassIndex());
            }

            rset = st.executeQuery();

            L2Skill skill = null;
            SkillTable stbl = SkillTable.getInstance();
            // Go though the recordset of this SQL query
            while (rset.next()) {
                int id = rset.getInt("skill_id");
                int level = rset.getInt("skill_level");

                if (id > 9000) {
                    continue; // fake skills for base stats
                }
                // Create a L2Skill object for each record
                skill = stbl.getInfo(id, level);
                if (skill == null) {
                    continue;
                }

                // Add the L2Skill object to the L2Character _skills and its Func objects to the
                // calculator set of the L2Character
                super.addSkill(skill);
            }
        } catch (SQLException e) {
            _log.warning("Could not restore character skills: " + e);
        } finally {
            Close.SR(st, rset);
        }
    }

    /**
     * Retrieve from the database all skill effects of this L2PcInstance and add
     * them to the player.<BR>
     * <BR>
     */
    public void restoreEffects(Connect con_ex) {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            if (con_ex == null) {
                con = L2DatabaseFactory.get();
                con.setTransactionIsolation(1);
            } else {
                con = con_ex;
            }
            /**
             * Restore Type 0 These skill were still in effect on the character
             * upon logout. Some of which were self casted and might still have
             * had a long reuse delay which also is restored.
             */
            st = con.prepareStatement(
                    "SELECT skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type FROM character_buffs WHERE char_obj_id=? AND class_index=? ORDER BY buff_index ASC");
            st.setInt(1, getObjectId());
            st.setInt(2, getClassIndex());
            rset = st.executeQuery();

            L2Skill skill = null;
            SkillTable stbl = SkillTable.getInstance();
            while (rset.next()) {
                int skillId = rset.getInt("skill_id");
                int effectCount = rset.getInt("effect_count");
                int effectCurTime = rset.getInt("effect_cur_time");
                int restore_type = rset.getInt("restore_type");

                disableSkill(skillId, rset.getInt("reuse_delay"), rset.getInt("systime"));
                // Just incase the admin minipulated this table incorrectly :x
                if (skillId == -1 || effectCount == -1 || effectCurTime == -1 || restore_type == 1) {
                    continue;
                }

                int skillLvl = rset.getInt("skill_level");
                skill = stbl.getInfo(skillId, skillLvl);
                if (skill == null) {
                    continue;
                }

                skill.getEffects(this, this, effectCount, effectCurTime);
                /*
                 * skill.getEffects(this, this);
                 * 
                 * L2Effect effect = null;
                 * FastTable<L2Effect> effects = getAllEffectsTable();
                 * for (int i = 0, n = effects.size(); i < n; i++) {
                 * effect = effects.get(i);
                 * if (effect == null) {
                 * continue;
                 * }
                 * 
                 * if (effect.getSkill().getId() == skillId) {
                 * effect.setCount(effectCount);
                 * effect.setFirstTime(effectCurTime);
                 * }
                 * }
                 */
            }
            Close.SR(st, rset);

            /**
             * Restore Type 1 The remaning skills lost effect upon logout but
             * were still under a high reuse delay.
             */
            /*
             * st = con.prepareStatement("SELECT
             * skill_id,skill_level,effect_count,effect_cur_time, reuse_delay,
             * systime FROM character_buffs WHERE char_obj_id=? AND
             * class_index=? AND restore_type=? ORDER BY buff_index ASC");
             * st.setInt(1, getObjectId()); st.setInt(2, getClassIndex());
             * st.setInt(3, 1); rset = st.executeQuery();
             *
             * while (rset.next()) { int skillId = rset.getInt("skill_id"); long
             * reuseDelay = rset.getLong("reuse_delay"); long systime =
             * rset.getLong("systime");
             *
             * long remainingTime = systime - System.currentTimeMillis(); if
             * (remainingTime < 10) { continue; }
             *
             * disableSkill(skillId, (int) remainingTime); //addTimeStamp(new
             * TimeStamp(skillId, (long) reuseDelay, (long) systime)); }
             * Close.SR(st, rset);
             */
            con.setAutoCommit(false);
            st = con.prepareStatement("DELETE FROM character_buffs WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, getClassIndex());
            st.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            // con.rollback();
            _log.warning("Could not restore active effect data: " + e);
        } finally {
            if (con_ex == null) {
                Close.CSR(con, st, rset);
            } else {
                Close.SR(st, rset);
            }
        }

        updateEffectIcons();
        if (con_ex == null) // дубликат setActiveClass(int classIndex)
        {
            broadcastUserInfo();
        }
    }

    /**
     * Retrieve from the database all Recommendation data of this L2PcInstance,
     * add to _recomChars and calculate stats of the L2PcInstance.<BR>
     * <BR>
     */
    private void restoreRecom(Connect con) {
        // Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            // con = L2DatabaseFactory.get();
            // con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT char_id,target_id FROM character_recommends WHERE char_id=? LIMIT 1");
            st.setInt(1, getObjectId());
            rset = st.executeQuery();
            while (rset.next()) {
                _recomChars.add(rset.getInt("target_id"));
            }
        } catch (SQLException e) {
            _log.warning("could not restore recommendations: " + e);
        } finally {
            Close.SR(st, rset);
        }
    }

    /**
     * Retrieve from the database all Henna of this L2PcInstance, add them to
     * _henna and calculate stats of the L2PcInstance.<BR>
     * <BR>
     */
    private void restoreHenna(Connect con) {
        // Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;

        L2Henna tpl = null;
        L2HennaInstance sym = null;
        HennaTable ht = HennaTable.getInstance();
        try {
            // con = L2DatabaseFactory.get();
            // con.setTransactionIsolation(1);
            // st = con.prepareStatement("SELECT slot,symbol_id,skill_id,skill_lvl FROM
            // character_hennas WHERE char_obj_id=? AND class_index=?");
            st = con.prepareStatement(
                    "SELECT slot,symbol_id FROM character_hennas WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, getClassIndexHenna());
            rset = st.executeQuery();
            for (int i = 0; i < 3; i++) {
                _henna[i] = null;
            }

            while (rset.next()) {
                int slot = rset.getInt("slot");

                if (slot < 1 || slot > 3) {
                    continue;
                }

                int symbol_id = rset.getInt("symbol_id");
                if (symbol_id != 0) {
                    tpl = ht.getTemplate(symbol_id);
                    if (tpl != null) {
                        sym = new L2HennaInstance(tpl);
                        /*
                         * int skill_id = rset.getInt("skill_id");
                         * if (skill_id != 0) {
                         * L2Skill skill = SkillTable.getInstance().getInfo(skill_id,
                         * rset.getInt("skill_lvl"));
                         * if (skill != null) {
                         * sym.setSkill(skill);
                         * addSkill(skill, false);
                         * }
                         * }
                         */
                        _henna[slot - 1] = sym;
                    }
                }
            }
        } catch (SQLException e) {
            _log.warning("could not restore henna: " + e);
        } finally {
            Close.SR(st, rset);
        }
        // Calculate Henna modifiers of this L2PcInstance
        recalcHennaStats();
    }

    /**
     * Return the number of Henna empty slot of the L2PcInstance.<BR>
     * <BR>
     */
    public int getHennaEmptySlots() {
        int totalSlots = 1 + getClassId().level();

        for (int i = 0; i < 3; i++) {
            if (_henna[i] != null) {
                totalSlots--;
            }
        }

        if (totalSlots <= 0) {
            return 0;
        }

        return totalSlots;
    }

    /**
     * Remove a Henna of the L2PcInstance, save update in the character_hennas
     * table of the database and send Server->Client HennaInfo/UserInfo packet
     * to this L2PcInstance.<BR>
     * <BR>
     */
    public boolean removeHenna(int slot) {
        if (slot < 1 || slot > 3) {
            return false;
        }

        slot--;

        if (_henna[slot] == null) {
            return false;
        }

        L2HennaInstance henna = _henna[slot];
        _henna[slot] = null;

        Connect con = null;
        PreparedStatement st = null;

        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=? AND slot=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, slot + 1);
            st.setInt(3, getClassIndexHenna());
            st.execute();
        } catch (SQLException e) {
            _log.warning("could not remove char henna: " + e);
        } finally {
            Close.CS(con, st);
        }

        // Calculate Henna modifiers of this L2PcInstance
        recalcHennaStats();

        // Send Server->Client HennaInfo packet to this L2PcInstance
        sendUserPacket(new HennaInfo(this));

        // Send Server->Client UserInfo packet to this L2PcInstance
        sendUserPacket(new UserInfo(this));

        // Add the recovered dyes to the player's inventory and notify them.
        getInventory().addItem("Henna", henna.getItemIdDye(), henna.getAmountDyeRequire() / 2, this, null);

        if (henna.getSkill() != null) {
            removeSkill(henna.getSkill(), false);
        }

        sendUserPacket(SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(henna.getItemIdDye())
                .addNumber(henna.getAmountDyeRequire() / 2));
        sendItems(false);
        sendChanges();
        return true;
    }

    /**
     * Add a Henna to the L2PcInstance, save update in the character_hennas
     * table of the database and send Server->Client HennaInfo/UserInfo packet
     * to this L2PcInstance.<BR>
     * <BR>
     */
    public boolean addHenna(L2HennaInstance henna) {
        return addHenna(henna, 0, 0);
    }

    public boolean addHenna(L2HennaInstance henna, int skill_id, int skill_lvl) {
        if (getHennaEmptySlots() == 0) {
            sendUserPacket(Static.MAX_3_DYES);
            return false;
        }

        // int slot = 0;
        for (int i = 0; i < 3; i++) {
            if (_henna[i] == null) {
                _henna[i] = henna;

                // Calculate Henna modifiers of this L2PcInstance
                recalcHennaStats();

                Connect con = null;
                PreparedStatement st = null;
                try {
                    con = L2DatabaseFactory.get();
                    // if (skill_id == 0) {
                    st = con.prepareStatement(
                            "INSERT INTO character_hennas (char_obj_id,symbol_id,slot,class_index) VALUES (?,?,?,?)");
                    st.setInt(1, getObjectId());
                    st.setInt(2, henna.getSymbolId());
                    st.setInt(3, i + 1);
                    st.setInt(4, getClassIndexHenna());
                    /*
                     * } else {
                     * st = con.
                     * prepareStatement("INSERT INTO character_hennas (char_obj_id,symbol_id,slot,class_index,skill_id,skill_lvl) VALUES (?,?,?,?,?,?)"
                     * );
                     * st.setInt(1, getObjectId());
                     * st.setInt(2, henna.getSymbolId());
                     * st.setInt(3, i + 1);
                     * st.setInt(4, getClassIndex());
                     * st.setInt(5, skill_id);
                     * st.setInt(6, skill_lvl);
                     * }
                     */
                    st.execute();
                } catch (SQLException e) {
                    _log.warning("could not save char henna: " + e);
                } finally {
                    Close.CS(con, st);
                }
                // Send Server->Client HennaInfo packet to this L2PcInstance
                sendUserPacket(new HennaInfo(this));
                // Send Server->Client UserInfo packet to this L2PcInstance
                sendUserPacket(new UserInfo(this));
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate Henna modifiers of this L2PcInstance.<BR>
     * <BR>
     */
    public void recalcHennaStats() {
        _hennaINT = 0;
        _hennaSTR = 0;
        _hennaCON = 0;
        _hennaMEN = 0;
        _hennaWIT = 0;
        _hennaDEX = 0;

        if (Config.ALT_HENNA_OLY_EVENT_DISABLE
                && getChannel() > 1) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (_henna[i] == null) {
                continue;
            }

            _hennaINT += _henna[i].getStatINT();
            _hennaSTR += _henna[i].getStatSTR();
            _hennaMEN += _henna[i].getStatMEM();
            _hennaCON += _henna[i].getStatCON();
            _hennaWIT += _henna[i].getStatWIT();
            _hennaDEX += _henna[i].getStatDEX();
        }

        /*
         * int max = Config.MAX_HENNA_BONUS; if (_hennaINT>max)_hennaINT = max;
         * if (_hennaSTR>5)_hennaSTR=5; if (_hennaMEN>5)_hennaMEN=5; if
         * (_hennaCON>5)_hennaCON=5; if (_hennaWIT>5)_hennaWIT=5; if
         * (_hennaDEX>5)_hennaDEX=5;
         */
        _hennaINT = Math.min(_hennaINT, Config.MAX_HENNA_BONUS);
        _hennaSTR = Math.min(_hennaSTR, Config.MAX_HENNA_BONUS);
        _hennaMEN = Math.min(_hennaMEN, Config.MAX_HENNA_BONUS);
        _hennaCON = Math.min(_hennaCON, Config.MAX_HENNA_BONUS);
        _hennaWIT = Math.min(_hennaWIT, Config.MAX_HENNA_BONUS);
        _hennaDEX = Math.min(_hennaDEX, Config.MAX_HENNA_BONUS);
    }

    /**
     * Return the Henna of this L2PcInstance corresponding to the selected
     * slot.<BR>
     * <BR>
     */
    public L2HennaInstance getHenna(int slot) {
        if (slot < 1 || slot > 3) {
            return null;
        }

        return _henna[slot - 1];
    }

    /**
     * Return the INT Henna modifier of this L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public int getHennaStatINT() {
        return _hennaINT;
    }

    /**
     * Return the STR Henna modifier of this L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public int getHennaStatSTR() {
        return _hennaSTR;
    }

    /**
     * Return the CON Henna modifier of this L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public int getHennaStatCON() {
        return _hennaCON;
    }

    /**
     * Return the MEN Henna modifier of this L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public int getHennaStatMEN() {
        return _hennaMEN;
    }

    /**
     * Return the WIT Henna modifier of this L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public int getHennaStatWIT() {
        return _hennaWIT;
    }

    /**
     * Return the DEX Henna modifier of this L2PcInstance.<BR>
     * <BR>
     */
    @Override
    public int getHennaStatDEX() {
        return _hennaDEX;
    }

    public void setChatBanned(boolean f) {
        _chatBanned = f;

        stopBanChatTask();
        if (isChatBanned()) {
            sendUserPacket(Static.CHAT_BLOCKED);
            if (_banchat_timer > 0) {
                _BanChatTask = ThreadPoolManager.getInstance().scheduleAi(new SchedChatUnban(this), _banchat_timer,
                        true);
            }
        } else {
            sendUserPacket(Static.CHAT_UNBLOCKED);
            setBanChatTimer(0);
        }
        sendUserPacket(new EtcStatusUpdate(this));
    }

    public void setChatBannedForAnnounce(boolean f) {
        _chatBanned = f;

        stopBanChatTask();
        if (isChatBanned()) {
            sendUserPacket(Static.CHAT_BLOCKED);
            _BanChatTask = ThreadPoolManager.getInstance().scheduleAi(new SchedChatUnban(this), _banchat_timer, false);
        } else {
            sendUserPacket(Static.CHAT_UNBLOCKED);
            setBanChatTimer(0);
        }
        sendEtcStatusUpdate();
    }

    public void setBanChatTimer(long timer) {
        _banchat_timer = timer;
    }

    public long getBanChatTimer() {
        if (_BanChatTask != null) {
            return _BanChatTask.getDelay(TimeUnit.MILLISECONDS);
        }
        return _banchat_timer;
    }

    public void stopBanChatTask() {
        if (_BanChatTask != null) {
            _BanChatTask.cancel(false);
            _BanChatTask = null;

        }
    }

    private static class SchedChatUnban implements Runnable {

        L2PcInstance _player;
        protected long _startedAt;

        protected SchedChatUnban(L2PcInstance player) {
            _player = player;
            _startedAt = System.currentTimeMillis();
        }

        public void run() {
            _player.setChatBanned(false);
        }
    }

    public boolean isChatBanned() {
        return _chatBanned;
    }

    /**
     * Return True if the L2PcInstance is autoAttackable.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Check if the attacker isn't the
     * L2PcInstance Pet</li>
     * <li>Check if the attacker is
     * L2MonsterInstance</li>
     * <li>If the attacker is a L2PcInstance, check if it
     * is not in the same party</li>
     * <li>Check if the L2PcInstance has Karma
     * </li>
     * <li>If the attacker is a L2PcInstance, check if it is not in the
     * same siege clan (Attacker, Defender)</li><BR>
     * <BR>
     *
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        if (attacker == null) {
            return false;
        }

        // Check if the attacker isn't the L2PcInstance Pet
        if (attacker == this || attacker == getPet()) {
            return false;
        }

        if (attacker.isL2Guard() && getKarma() > 0) {
            return true;
        }

        if (PeaceZone.getInstance().inPeace(this, attacker)) {
            return false;
        }

        // TODO: check for friendly mobs
        // Check if the attacker is a L2MonsterInstance
        if (attacker.isMonster()) {
            return true;
        }

        // Check if the attacker is not in the same party
        if (getParty() != null && getParty().getPartyMembers().contains(attacker)) {
            return false;
        }

        // Check if the attacker is in olympia and olympia start
        if (isInOlympiadMode()) {
            L2PcInstance enemy = attacker.getPlayer();
            if (!enemy.isInOlympiadMode() || enemy.getOlympiadGameId() != getOlympiadGameId()
                    || !isOlympiadCompStart()) {
                return false;
            } else {
                return true;
            }
        }

        // Check if the attacker is in TvT and TvT is started
        if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(attacker.getName())) {
            return true;
        }

        // Check if the attacker is not in the same clan
        if (getClan() != null && attacker != null && getClan().isMember(attacker.getObjectId())) {
            return false;
        }

        // Check if the L2PcInstance has Karma
        if (getKarma() > 0 || getPvpFlag() > 0) {
            return true;
        }

        // Check if the attacker is a L2PcInstance
        if (attacker.isPlayer()) {
            // is AutoAttackable if both players are in the same duel and the duel is still
            // going on
            if (getDuel() != null && attacker.getDuel() != null
                    && getDuel().getDuelState(this) == DuelState.Fighting && getDuel() == attacker.getDuel()) {
                return true;
            }
            // Check if the L2PcInstance is in an arena or a siege area
            if (isInsidePvpZone() && attacker.isInsidePvpZone()) {
                return true;
            }

            if (getClan() != null) {
                Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
                if (siege != null) {
                    // Check if a siege is in progress and if attacker and the L2PcInstance aren't
                    // in the Defender clan
                    if (siege.checkIsDefender(attacker.getClan()) && siege.checkIsDefender(getClan())) {
                        return false;
                    }

                    // Check if a siege is in progress and if attacker and the L2PcInstance aren't
                    // in the Attacker clan
                    if (siege.checkIsAttacker(attacker.getClan()) && siege.checkIsAttacker(getClan())) {
                        return false;
                    }
                }

                if (Config.FREE_PVP) {
                    return true;
                }

                // Check if clan is at war
                if (hasClanWarWith(attacker.getClan()) && !isAcademyMember()) {
                    return true;
                }
                /*
                 * if (getClan() != null && attacker.getClan() != null
                 * && (getClan().isAtWarWith(attacker.getClanId())
                 * && getWantsPeace() == 0
                 * && attacker.getWantsPeace() == 0
                 * && !isAcademyMember())) {
                 * return true;
                 * }
                 */
            }
        } else if (attacker.isL2SiegeGuard()) {
            if (getClan() != null) {
                Siege siege = SiegeManager.getInstance().getSiege(this);
                return (siege != null && siege.checkIsAttacker(getClan()));
            }
        }

        return false;
    }

    /**
     * Check if the active L2Skill can be casted.<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Check if the skill isn't toggle and
     * is offensive</li>
     * <li>Check if the target is in the skill cast range
     * </li>
     * <li>Check if the skill is Spoil type and if the target isn't
     * already spoiled</li>
     * <li>Check if the caster owns enought consummed
     * Item, enough HP and MP to cast the skill</li>
     * <li>Check if the caster
     * isn't sitting</li>
     * <li>Check if all skills are enabled and this skill is
     * enabled</li><BR>
     * <BR>
     * <li>Check if the caster own the weapon needed
     * </li><BR>
     * <BR>
     * <li>Check if the skill is active</li><BR>
     * <BR>
     * <li>Check if
     * all casting conditions are completed</li><BR>
     * <BR>
     * <li>Notify the AI with
     * AI_INTENTION_CAST and target</li><BR>
     * <BR>
     *
     * @param skill    The L2Skill to use
     * @param forceUse used to force ATTACK on players
     * @param dontMove used to prevent movement, if not in range
     *
     */
    public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove) {
        // System.out.println("#L2PcInstance##useMagic##1#");
        // Check if the caster is sitting
        if (isSitting() && !skill.isPotion()) {
            sendActionFailed();
            sendUserPacket(Static.CANT_MOVE_SITTING);
            return;
        }

        if (inObserverMode()) {
            abortCast();
            sendActionFailed();
            sendUserPacket(Static.OBSERVERS_CANNOT_PARTICIPATE);
            return;
        }

        if (isConfused() || getPrivateStoreType() != L2PcInstance.PS_NONE) {
            sendActionFailed();
            return;
        }

        if (isSkillDisabled(skill.getId(), skill.isAugment())) {
            sendActionFailed();
            sendUserPacket(SystemMessage.id(SystemMessageId.SKILL_NOT_AVAILABLE).addString(skill.getName()));
            return;
        }

        if (skill.isDisabledFor(this)) {
            sendActionFailed();
            return;
        }

        if (isAlikeDead() && !skill.isPotion() && skill.getSkillType() != L2Skill.SkillType.FAKE_DEATH) {
            sendActionFailed();
            return;
        }

        if (skill.isChargeSkill() && getCharges() >= 7) {
            sendUserPacket(Static.FORCE_MAXLEVEL_REACHED);
            sendActionFailed();
            return;
        }

        if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)) {
            sendActionFailed();
            sendUserPacket(Static.NOT_ENOUGH_MP);
            return;
        }

        if (getCurrentHp() <= skill.getHpConsume()) {
            sendActionFailed();
            sendUserPacket(Static.NOT_ENOUGH_HP);
            return;
        }

        // Check if the caster own the weapon needed
        if (!skill.getWeaponDependancy(this)) {
            sendActionFailed();
            return;
        }

        // Check if the spell consummes an Item
        if (skill.getItemConsume() > 0 && !hasItemCount(skill.getItemConsumeId(), skill.getItemConsume())) {
            // Checked: when a summon skill failed, server show required consume item count
            if (skill.isSkillTypeSummon()) {
                sendUserPacket(SystemMessage.id(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1)
                        .addItemName(skill.getItemConsumeId()).addNumber(skill.getItemConsume()));
                return;
            } else {
                // Send a System Message to the caster
                sendUserPacket(Static.NOT_ENOUGH_ITEMS);
                return;
            }
        }

        if (isCursedWeaponEquiped() && skill.isNotForCursed()) {
            sendPacket(Static.NOT_FOR_CURSED);
            sendActionFailed();
            return;
        }

        if (skill.isSiegeFlagSkill()) {
            if (!isInSiegeFlagArea()) {
                sendActionFailed();
                return;
            }

            Castle castle = CastleManager.getInstance().getCastle(this);
            if (castle == null || !castle.getSiege().getIsInProgress()) {
                sendActionFailed();
                return;
            }
        }

        // Check if it's ok to summon
        // siege golem (13), Wild Hog Cannon (299), Swoop Cannon (448)
        if (skill.isSiegeSummonSkill() && !SiegeManager.getInstance().checkIfOkToSummon(this, false)) {
            return;
        }
        if (!skill.isCubic() && skill.getSkillType() == L2Skill.SkillType.SUMMON && getPet() != null) {
            sendUserPacket(Static.YOU_ALREADY_HAVE_A_PET);
            return;
        }

        if (!skill.isBuff() && isMounted()) {
            return;
        }

        if (isInOlympiadMode() && (skill.isHeroSkill() || skill.getSkillType() == SkillType.RESURRECT)) {
            sendUserPacket(Static.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
            return;
        }

        if (isForbidWeapon(getActiveWeaponInstance())) {
            sendActionFailed();
            return;
        }

        // ************************************* Check Casting in Progress
        // *******************************************
        if (getCurrentSkill() != null && isCastingNow()) {
            // Check if new skill different from current skill in progress
            if (skill.getId() == getCurrentSkill().getSkillId()) {
                sendActionFailed();
                return;
            }

            // Create a new SkillDat object and queue it in the player _queuedSkill
            setQueuedSkill(skill, forceUse, dontMove);
            sendActionFailed();
            return;
        }
        // wiping out previous values, after casting has been aborted
        if (getQueuedSkill() != null) {
            setQueuedSkill(null, false, false);
        }

        // ************************************* Check Target
        // *******************************************
        L2Object target = findCastTarget(skill);
        if (!canCastOnTarget(skill, target, forceUse, dontMove, getTarget())) {
            sendActionFailed();
            return;
        }

        updateLastTeleport(false);

        // System.out.println("#L2PcInstance##useMagic##2#");
        if (skill.isOffensive() && (target.isMonster() || (target.isL2Npc() && Config.ALLOW_HIT_NPC && forceUse))) {
            // GeoData Los Check here
            if (skill.getCastRange() > 0 && skill.isSignedTargetType()) {
                if (!GeoData.getInstance().canSeeTarget(this, getCurrentSkillWorldPosition())) {
                    sendActionFailed();
                    sendUserPacket(Static.CANT_SEE_TARGET);
                    return;
                }
            }
            // If all conditions are checked, create a new SkillDat object and set the
            // player _currentSkill
            setCurrentSkill(skill, forceUse, dontMove);
            // Check if the active L2Skill can be casted (ex : not sleeping...), Check if
            // the target is correct and Notify the AI with AI_INTENTION_CAST and target
            super.useMagic(skill);
            return;
        }

        // Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
        if (skill.isCheckPvpSkill() && !checkPvpSkill(target, skill)) {
            sendActionFailed();
            sendUserPacket(Static.TARGET_IS_INCORRECT);
            return;
        }

        // GeoData Los Check here
        if (skill.getCastRange() > 0 && skill.isSignedTargetType()) {
            if (!GeoData.getInstance().canSeeTarget(this, getCurrentSkillWorldPosition())) {
                sendActionFailed();
                sendUserPacket(Static.CANT_SEE_TARGET);
                return;
            }
        }

        // System.out.println("#L2PcInstance##useMagic##5#");
        // If all conditions are checked, create a new SkillDat object and set the
        // player _currentSkill
        setCurrentSkill(skill, forceUse, dontMove);

        // Check if the active L2Skill can be casted (ex : not sleeping...), Check if
        // the target is correct and Notify the AI with AI_INTENTION_CAST and target
        super.useMagic(skill);
    }

    private L2Object findCastTarget(L2Skill skill) {
        if (skill.isAuraSignedTargetType()) {
            return this;
        } else if (skill.isPetTargetType()) {
            return getPet();
        }
        return getTarget();
    }

    private boolean canCastOnTarget(L2Skill skill, L2Object target, boolean forceUse, boolean dontMove,
            L2Object ctarget) {
        if (target == null) {
            sendUserPacket(Static.TARGET_CANT_FOUND);
            return false;
        }

        if (ctarget != null && ctarget.isL2VillageMaster()) {
            sendUserPacket(Static.TARGET_IS_INCORRECT);
            return false;
        }

        if (skill.isAuraSkill() && isInZonePeace() && !Config.ALLOW_DEBUFFS_IN_TOWN) {
            if (isInOlympiadMode() && isOlympiadCompStart() && skill.getId() == 347) {
                return true;
            }
            return false;
        }

        if (target.equals(this)) {
            return true;
        }

        if (isInDuel()) {
            // if (!(target.isPlayer() && target.getDuel() == getDuel())) {
            if (target.getDuel() != getDuel()) {
                sendUserPacket(Static.CANT_IN_DUEL);
                return false;
            }
        }

        if (skill.isOffensive()) {

            if (target.isMonster()) {
                return true;
            }

            if (isInOlympiadMode() && !isOlympiadCompStart() && skill.getId() != 347) {
                sendActionFailed();
                return false;
            }

            // Check if the target is attackable
            if (!target.isAttackable()) {
                return false;
            }

            // Check if a Forced ATTACK is in progress on non-attackable target
            if (!skill.isAuraSignedTargetType() && !target.isAutoAttackable(this) && !forceUse) {
                return false;
            }
            // Check if the target is in the skill cast range
            if (dontMove) {
                // Calculate the distance between the L2PcInstance and the target
                if (skill.getCastRange() > 0 && !isInsideRadius(target,
                        skill.getCastRange() + getTemplate().collisionRadius, false, false)) {
                    sendUserPacket(Static.TARGET_TOO_FAR);
                    return false;
                }
            }

            if (PeaceZone.getInstance().inPeace(this, target) && skill.getId() != 347) {
                return false;
            }
        } else {
            if (Config.ELH_FORBID_MAGIC && target.getChannel() == 6) {
                sendUserPacket(Static.TARGET_IS_INCORRECT);
                return false;
            }
            if (Config.PROTECT_GATE_PVP && PeaceZone.getInstance().outGate(this, target, skill.getId())) {
                sendUserPacket(Static.TARGET_IS_INCORRECT);
                return false;
            }
        }
        // skills can be used on Walls and Doors only durring siege

        if (target.isL2Door()
                && !target.isAttackable()) {
            return false;
        }
        // check if the target is a monster and if force attack is set.. if not then we
        // don't want to cast.

        if (target.isMonster()
                && !skill.isFreeCastOnMob() && !forceUse) {
            return false;
        }

        if (isInOlympiadMode()
                && skill.isFishingSkill()) {
            sendActionFailed();
            return false;
        }

        if (skill.getId() == 246) {
            if (!target.isL2Artefact()) {
                return false;
            }

            if (!isInSiegeRuleArea()) {
                sendPacket(Static.WRONG_PLACE_CAST_RULE);
                return false;
            }
        }

        switch (skill.getSkillType()) {
            case SPOIL: // Check if the skill is Spoil type and if the target isn't already spoiled
            case DRAIN_SOUL: // Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
                if (!target.isL2Monster()) {
                    sendUserPacket(Static.TARGET_IS_INCORRECT);
                    return false;
                }
                break;
            case SWEEP: // Check if the skill is Sweep type and if conditions not apply
                if (target.isL2Attackable()) {
                    if (target.isDead()) {
                        if (!target.isSpoil()) {
                            sendActionFailed();
                            sendUserPacket(Static.SWEEPER_FAILED_TARGET_NOT_SPOILED);
                            return false;
                        }
                        int spoilerId = target.getIsSpoiledBy();
                        if (getObjectId() != spoilerId && !isInLooterParty(spoilerId)) {
                            sendActionFailed();
                            sendUserPacket(Static.SWEEP_NOT_ALLOWED);
                            return false;
                        }
                    }
                }
                break;
            case SUMMON_NPC:
                if (!(skill.getId() == 2137 || skill.getId() == 2138)) {
                    if (PeaceZone.getInstance().inPeace(this, target)) {
                        return false;
                    }
                }
                break;
        }

        return true;
    }

    public boolean isInLooterParty(int LooterId) {
        L2PcInstance looter = L2World.getInstance().getPlayer(LooterId);
        if (looter == null) {
            return false;
        }

        // if L2PcInstance is in a CommandChannel
        if (isInParty() && getParty().isInCommandChannel()) {
            return getParty().getCommandChannel().getMembers().contains(looter);
        }

        if (isInParty()) {
            return getParty().getPartyMembers().contains(looter);
        }

        return false;
    }

    /**
     * Check if the requested casting is a Pc->Pc skill cast and if it's a valid
     * pvp condition
     *
     * @param obj   L2Object instance containing the obj
     * @param skill L2Skill instance with the skill being casted
     * @return False if the skill is a pvpSkill and obj is not a valid pvp obj
     */
    public boolean checkPvpSkill(L2Object obj, L2Skill skill) {
        if (obj == null || skill == null) {
            return true;
        }

        // check for PC->PC Pvp status
        // if (getCurrentSkill().isCtrlPressed() && skill.getId() == 452)
        // return true;
        L2PcInstance target = obj.getPlayer();
        if (target == null) {
            return true;
        }

        // if (target.equals(this))
        // return false;
        if (isInOlympiadMode()) {
            if (target.isInOlympiadMode() && target.getOlympiadGameId() == getOlympiadGameId()
                    && isOlympiadCompStart()) {
                return true;
            }
        }

        if (!target.equals(this) && !(isInDuel() && target.getDuel() == getDuel())) {
            if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getName())
                    && TvTEvent.isPlayerParticipant(target.getName())) {
                if (skill.isSkillTypeOffensive() || skill.isPvpSkill() || skill.isHeroDebuff() || skill.isAOEpvp()) // pvp
                                                                                                                    // skill
                {
                    if (TvTEvent.getParticipantTeamId(getName()) == TvTEvent.getParticipantTeamId(target.getName())) {
                        return false;
                    }
                }
            } else if (isInsideHotZone() || target.isInsideHotZone()) {
                if (skill.isPvpSkill() || skill.isHeroDebuff() || skill.isAOEpvp()) // pvp skill
                {
                    if (getParty() != null && getParty().getPartyMembers().contains(target)) {
                        return false;
                    }

                    if (getClan() != null && target.getClan() != null) {
                        if (getClanId() == target.getClanId()) {
                            return false;
                        }
                    }

                    if (getAllyId() != 0 && target.getAllyId() != 0 && getAllyId() == target.getAllyId()) {
                        return false;
                    }
                }
            } else if (!isInsidePvpZone() || !target.isInsidePvpZone()) {
                if (skill.isPvpSkill() || skill.isHeroDebuff() || skill.isAOEpvp()) // pvp skill
                {
                    if (getClan() != null && target.getClan() != null) {
                        if (hasClanWarWith(target)) {
                            return true; // in clan war player can attack whites even with sleep etc.
                        } else if (getClanId() == target.getClanId()) {
                            return false;
                        }
                    }

                    if (getParty() != null && getParty().getPartyMembers().contains(target)) {
                        return false;
                    }

                    if (getClan() != null && target.getClan() != null) {
                        if (getClanId() == target.getClanId()) {
                            return false;
                        }
                    }

                    if (getAllyId() != 0 && target.getAllyId() != 0 && getAllyId() == target.getAllyId()) {
                        return false;
                    }

                    if (target.getPvpFlag() == 0 && target.getKarma() == 0) {
                        return false;
                    }
                } else if (getCurrentSkill() != null && !getCurrentSkill().isCtrlPressed() && skill.isOffensive()) {
                    if (hasClanWarWith(target)) {
                        return true; // in clan war player can attack whites even without ctrl
                    }
                    if (getParty() != null && getParty().getPartyMembers().contains(target)) {
                        return false;
                    }

                    if (getClan() != null && target.getClan() != null) {
                        if (getClanId() == target.getClanId()) {
                            return false;
                        }
                    }

                    if (getAllyId() != 0 && target.getAllyId() != 0 && getAllyId() == target.getAllyId()) {
                        return false;
                    }

                    if (target.getPvpFlag() == 0 && target.getKarma() == 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Reduce Item quantity of the L2PcInstance Inventory and send it a
     * Server->Client packet InventoryUpdate.<BR>
     * <BR>
     */
    @Override
    public void consumeItem(int itemConsumeId, int itemCount) {
        if (itemConsumeId != 0 && itemCount != 0) {
            destroyItemByItemId("Consume", itemConsumeId, itemCount, null, false);
        }
    }

    /**
     * Return True if the L2PcInstance is a Mage.<BR>
     * <BR>
     */
    @Override
    public boolean isMageClass() {
        return getClassId().isMage();
    }

    @Override
    public boolean isMounted() {
        return _mountType > 0;
    }

    /**
     * Set the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern) and send
     * a Server->Client packet InventoryUpdate to the L2PcInstance.<BR>
     * <BR>
     */
    public boolean checkLandingState() {
        // Check if char is in a no landing zone
        if (isInsideZone(ZONE_NOLANDING)) {
            return true;
        } else // if this is a castle that is currently being sieged, and the rider is NOT a
               // castle owner
        // he cannot land.
        // castle owner is the leader of the clan that owns the castle where the pc is
        if (isInsideZone(ZONE_SIEGE)
                && !(getClan() != null && CastleManager.getInstance().getCastle(this) == CastleManager.getInstance()
                        .getCastleByOwner(getClan()) && this == getClan().getLeader().getPlayerInstance())) {
            return true;
        }

        return false;
    }

    // returns false if the change of mount type fails.
    public boolean setMountType(int mountType) {
        if (checkLandingState() && mountType == 2) {
            return false;
        } else if (isInsideCastleZone() && mountType == 1) {
            return false;
        }

        if (_taskRentPet != null) {
            sendUserPacket(new SetupGauge(3, 0));
            _taskRentPet.cancel(true);
        }

        switch (mountType) {
            case 0:
                setIsFlying(false);
                setIsRiding(false);
                break; // Dismounted
            case 1:
                setIsRiding(true);
                if (isNoble()) {
                    addSkill(SkillTable.getInstance().getInfo(325, 1), false); // not saved to DB
                }
                break;
            case 2:
                setIsFlying(true);
                addSkill(SkillTable.getInstance().getInfo(327, 1), false); // not saved to DB
                addSkill(SkillTable.getInstance().getInfo(4289, 1), false); // not saved to DB
                break; // Flying Wyvern
        }

        _mountType = mountType;

        // Send a Server->Client packet InventoryUpdate to the L2PcInstance in order to
        // update speed
        // sendUserPacket(new UserInfo(this));
        sendSkillList();
        broadcastUserInfo();
        return true;
    }

    /**
     * Return the type of Pet mounted (0 : none, 1 : Stridder, 2 :
     * Wyvern).<BR>
     * <BR>
     */
    public int getMountType() {
        return _mountType;
    }

    /**
     * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo
     * to all L2PcInstance in its _KnownPlayers.<BR>
     * <BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * Others L2PcInstance in the detection
     * area of the L2PcInstance are identified in <B>_knownPlayers</B>. In order
     * to inform other players of this L2PcInstance state modifications, server
     * just need to go through _knownPlayers to send Server->Client
     * Packet<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Send a Server->Client packet
     * UserInfo to this L2PcInstance (Public and Private Data)</li>
     * <li>Send a
     * Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of
     * the L2PcInstance (Public data only)</li><BR>
     * <BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to
     * other players instead of CharInfo packet. Indeed, UserInfo packet
     * contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
     * <BR>
     *
     */
    @Override
    public void updateAbnormalEffect() {
        sendChanges();// broadcastUserInfo();
    }

    /**
     * Disable the Inventory and create a new task to enable it after
     * 1.5s.<BR>
     * <BR>
     */
    public void tempInvetoryDisable() {
        _inventoryDisable = true;

        ThreadPoolManager.getInstance().scheduleAi(new InventoryEnable(), 1500, true);
    }

    /**
     * Return True if the Inventory is disabled.<BR>
     * <BR>
     */
    public boolean isInvetoryDisabled() {
        return _inventoryDisable;

    }

    class InventoryEnable implements Runnable {

        public void run() {
            _inventoryDisable = false;
        }
    }

    public Map<Integer, L2CubicInstance> getCubics() {
        return _cubics;
    }

    /**
     * Add a L2CubicInstance to the L2PcInstance _cubics.<BR>
     * <BR>
     */
    public void addCubic(int id, int level) {
        L2CubicInstance cubic = new L2CubicInstance(this, id, level);
        _cubics.put(id, cubic);
    }

    /**
     * Remove a L2CubicInstance from the L2PcInstance _cubics.<BR>
     * <BR>
     */
    public void delCubic(int id) {
        _cubics.remove(id);
    }

    /**
     * Return the L2CubicInstance corresponding to the Identifier of the
     * L2PcInstance _cubics.<BR>
     * <BR>
     */
    public L2CubicInstance getCubic(int id) {
        return _cubics.get(id);
    }

    @Override
    public String toString() {
        return "player " + super.getName();
    }

    /**
     * Return the modifier corresponding to the Enchant Effect of the Active
     * Weapon (Min : 127).<BR>
     * <BR>
     */
    public int getEnchantEffect() {
        L2ItemInstance wpn = getActiveWeaponInstance();

        if (wpn == null) {
            return 0;
        }

        return Math.min(127, wpn.getEnchantLevel());
    }

    /**
     * Set the _lastFolkNpc of the L2PcInstance corresponding to the last Folk
     * wich one the player talked.<BR>
     * <BR>
     */
    public void setLastFolkNPC(L2FolkInstance folkNpc) {
        _lastFolkNpc = folkNpc;
    }

    /**
     * Return the _lastFolkNpc of the L2PcInstance corresponding to the last
     * Folk wich one the player talked.<BR>
     * <BR>
     */
    public L2FolkInstance getLastFolkNPC() {
        return _lastFolkNpc;
    }

    /**
     * Set the Silent Moving mode Flag.<BR>
     * <BR>
     */
    public void setSilentMoving(boolean f) {
        _isSilentMoving = f;
    }

    /**
     * Return True if the Silent Moving mode is active.<BR>
     * <BR>
     */
    @Override
    public boolean isSilentMoving() {
        return _isSilentMoving;
    }

    /**
     * Return True if L2PcInstance is a participant in the Festival of
     * Darkness.<BR>
     * <BR>
     */
    @Override
    public boolean isFestivalParticipant() {
        return SevenSignsFestival.getInstance().isParticipant(this);
    }

    public void addAutoSoulShot(int itemId) {
        _activeSoulShots.put(itemId, itemId);
    }

    public void removeAutoSoulShot(int itemId) {
        _activeSoulShots.remove(itemId);
    }

    public Map<Integer, Integer> getAutoSoulShot() {
        return _activeSoulShots;
    }

    @Override
    public void rechargeAutoSoulShot(boolean a, boolean b, boolean c) {

        if (_fantome) {
            broadcastSoulShotsPacket(new MagicSkillUser(this, this, 2154, 1, 0, 0));
            return;
        }

        if (_activeSoulShots == null || _activeSoulShots.isEmpty()) {
            return;
        }

        L2ItemInstance item;
        IItemHandler handler;

        for (int itemId : _activeSoulShots.values()) {
            item = getInventory().getItemByItemId(itemId);

            if (item != null) {
                if (b) {
                    if (!c) {
                        if (item.isMagicShot()) {
                            handler = ItemHandler.getInstance().getItemHandler(itemId);

                            if (handler != null) {
                                handler.useItem(this, item, false);
                            }
                        }
                    } else if (itemId == 6646 || itemId == 6647) {
                        handler = ItemHandler.getInstance().getItemHandler(itemId);

                        if (handler != null) {
                            handler.useItem(this, item, false);
                        }
                    }
                }

                if (a) {
                    if (!c) {
                        if (item.isFighterShot()) {
                            handler = ItemHandler.getInstance().getItemHandler(itemId);

                            if (handler != null) {
                                handler.useItem(this, item, false);
                            }
                        }
                    } else if (itemId == 6645) {
                        handler = ItemHandler.getInstance().getItemHandler(itemId);

                        if (handler != null) {
                            handler.useItem(this, item, false);
                        }
                    }
                }
            } else {
                removeAutoSoulShot(itemId);

            }
        }
    }

    private ScheduledFuture<?> _taskWarnUserTakeBreak;

    class WarnUserTakeBreak implements Runnable {

        @Override
        public void run() {
            if (isOnline() == 1) {
                sendUserPacket(Static.PLAYING_FOR_LONG_TIME);
            } else {
                stopWarnUserTakeBreak();
            }
        }
    }

    class RentPetTask implements Runnable {

        @Override
        public void run() {
            stopRentPet();
        }
    }

    public ScheduledFuture<?> _taskforfish;

    class LookingForFishTask implements Runnable {

        boolean _isNoob, _isUpperGrade;
        int _fishType, _fishGutsCheck, _gutsCheckTime;
        long _endTaskTime;

        protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob,
                boolean isUpperGrade) {
            _fishGutsCheck = fishGutsCheck;
            _endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
            _fishType = fishType;
            _isNoob = isNoob;
            _isUpperGrade = isUpperGrade;
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() >= _endTaskTime) {
                EndFishing(false);
                return;
            }
            if (_fishType == -1) {
                return;
            }
            int check = Rnd.get(1000);
            if (_fishGutsCheck > check) {
                stopLookingForFishTask();
                StartFishCombat(_isNoob, _isUpperGrade);
            }
        }
    }

    public int getClanPrivileges() {
        return _clanPrivileges;
    }

    public void setClanPrivileges(int n) {
        _clanPrivileges = n;
    }

    // baron etc
    public void setPledgeClass(int classId) {
        _pledgeClass = classId;
    }

    public int getPledgeClass() {
        return _pledgeClass;
    }

    public void setPledgeType(int typeId) {
        _pledgeType = typeId;
    }

    public int getPledgeType() {
        return _pledgeType;
    }

    public int getApprentice() {
        return _apprentice;
    }

    public void setApprentice(int apprentice_id) {
        _apprentice = apprentice_id;
    }

    public int getSponsor() {
        return _sponsor;
    }

    public void setSponsor(int sponsor_id) {
        _sponsor = sponsor_id;
    }

    @Override
    public void sendMessage(String txt) {
        sendUserPacket(SystemMessage.sendString(txt));
    }

    @Override
    public void sendAdmResultMessage(String txt) {
        sendUserPacket(new CreatureSay(0, 0, "SYS", txt));
    }

    public void sendModerResultMessage(String txt) {
        sendUserPacket(new CreatureSay(0, 16, "ModerLog", txt));
    }

    public void sendHtmlMessage(String txt) {
        sendHtmlMessage("Уведомление.", txt);
    }

    public void sendHtmlMessage(String type, String txt) {
        NpcHtmlMessage html = NpcHtmlMessage.id(0);
        html.setHtml("<html><body> " + type + "<br>" + txt + "<br></body></html>");
        sendUserPacket(html);
        html = null;
    }

    public void enterObserverMode(int x, int y, int z) {
        _obsX = getX();
        _obsY = getY();
        _obsZ = getZ();

        setTarget(null);
        stopMove(null);
        setIsParalyzed(true);
        setIsInvul(true);
        setChannel(0);
        sendUserPacket(new ObservationMode(x, y, z));
        setXYZ(x, y, z);

        _observerMode = 1;
        // enterMovieMode();
        broadcastUserInfo();

        /*
         * if(getOlympiadObserveId() > -1) { OlympiadGame game =
         * Olympiad.getOlympiadGame(getOlympiadObserveId()); if(game != null)
         * game.broadcastInfo(null, this, true); }
         */
    }

    public void enterOlympiadObserverMode(int x, int y, int z, int id, boolean storeCoords) {
        if (getPet() != null) {
            getPet().unSummon(this);
        }

        if (getCubics().size() > 0) {
            for (L2CubicInstance cubic : getCubics().values()) {
                cubic.stopAction();
                cubic.cancelDisappear();
            }

            getCubics().clear();
        }

        if (getParty() != null) {
            getParty().removePartyMember(this);
        }

        _olympiadGameId = id;
        _olympiadObserveId = id;
        if (isSitting()) {
            standUp();
        }
        if (storeCoords) {
            _obsX = getX();
            _obsY = getY();
            _obsZ = getZ();
        }
        setTarget(null);
        setIsInvul(true);
        setChannel(0);
        teleToLocation(x, y, z, true);
        sendUserPacket(new ExOlympiadMode(3));
        _observerMode = 1;
        // enterMovieMode();
        broadcastUserInfo();
        if (getOlympiadObserveId() > -1) {
            OlympiadGame game = Olympiad.getOlympiadGame(getOlympiadObserveId());
            if (game != null) {
                game.broadcastInfo(null, this, true);
            }
        }
    }

    public void appearObserverMode() {
        _observerMode = 3;

        getKnownList().updateKnownObjects();
        // broadcastUserInfo();

        if (getOlympiadObserveId() > -1) {
            OlympiadGame game = Olympiad.getOlympiadGame(getOlympiadObserveId());
            if (game != null) {
                game.broadcastInfo(null, this, true);
            }
        }
    }

    public void returnFromObserverMode() {
        _observerMode = 0;
        _olympiadObserveId = -1;
        // broadcastUserInfo();
        // getKnownList().updateKnownObjects();
    }

    public void leaveObserverMode() {
        setTarget(null);
        setXYZ(_obsX, _obsY, _obsZ);
        setIsParalyzed(false);
        setChannel(1);
        setIsInvul(false);

        if (getAI() != null) {
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        }

        _observerMode = 2;
        _olympiadObserveId = -1;
        sendUserPacket(new ObservationReturn(this));
        // leaveMovieMode();
        broadcastUserInfo();
    }

    public void leaveOlympiadObserverMode() {
        setTarget(null);
        sendUserPacket(new ExOlympiadMode(0));
        sendUserPacket(new ExOlympiadMatchEnd());
        teleToLocation(_obsX, _obsY, _obsZ, true);
        setChannel(1);
        setIsInvul(false);

        if (getAI() != null) {
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        }

        Olympiad.removeSpectator(_olympiadObserveId, this);
        _olympiadGameId = -1;
        _olympiadObserveId = -1;
        _observerMode = 2;
        // leaveMovieMode();
        broadcastUserInfo();
    }

    public int getObserverMode() {
        return _observerMode;
    }

    public void updateNameTitleColor() {
        // Donator Color and title update
        // Note: this code can be used for GM's too
        sendChanges();// broadcastUserInfo();
    }

    public void setOlympiadSide(int i) {
        _olympiadSide = i;
    }

    public int getOlympiadSide() {
        return _olympiadSide;
    }

    public void setOlympiadGameId(int id) {
        _olympiadGameId = id;
    }

    @Override
    public int getOlympiadGameId() {
        return _olympiadGameId;
    }

    public int getObsX() {
        return _obsX;
    }

    public int getObsY() {
        return _obsY;
    }

    public int getObsZ() {
        return _obsZ;
    }

    @Override
    public boolean inObserverMode() {
        return _observerMode > 0;
    }

    public int getTeleMode() {
        return _telemode;
    }

    public void setTeleMode(int mode) {
        _telemode = mode;
    }

    public void setLoto(int i, int val) {
        _loto[i] = val;
    }

    public int getLoto(int i) {
        return _loto[i];
    }

    public void setRace(int i, int val) {
        _race[i] = val;
    }

    public int getRace(int i) {
        return _race[i];
    }

    public boolean getMessageRefusal() {
        return _messageRefusal;
    }

    public void setMessageRefusal(boolean f) {
        _messageRefusal = f;
        sendEtcStatusUpdate();
    }

    public void setDietMode(boolean f) {
        _dietMode = f;
    }

    public boolean getDietMode() {
        return _dietMode;
    }

    public void setTradeRefusal(boolean f) {
        _tradeRefusal = f;
    }

    public boolean getTradeRefusal() {
        return _tradeRefusal;
    }

    public void setExchangeRefusal(boolean f) {
        _exchangeRefusal = f;
    }

    public boolean getExchangeRefusal() {
        return _exchangeRefusal;
    }

    public BlockList getBlockList() {
        return _blockList;
    }

    public int getCount() {
        int count = 0;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT count FROM heroes WHERE char_name=?");
            st.setString(1, getName());
            rset = st.executeQuery();
            if (rset.next()) {
                count = (rset.getInt("count"));
            }
        } catch (Exception e) {
        } finally {
            Close.CSR(con, st, rset);
        }
        return count;
    }

    public void setHero(boolean f) {
        if (f && (_baseClass == _activeClass || Config.ALLOW_HERO_SUBSKILL)) {
            for (L2Skill s : HeroSkillTable.getHeroSkills()) {
                addSkill(s, false);
            }
        } else {
            for (L2Skill s : HeroSkillTable.getHeroSkills()) {
                super.removeSkill(s); // Just Remove skills from nonHero characters
            }
        }
        _hero = f;
        sendSkillList();
    }

    public void setIsInOlympiadMode(boolean b) {
        _inOlympiadMode = b;
    }

    public void setIsOlympiadStart(boolean b) {
        _OlympiadStart = b;
    }

    @Override
    public boolean isOlympiadStart() {
        return _OlympiadStart;
    }

    @Override
    public boolean isInOlympiadMode() {
        return _inOlympiadMode;
    }

    private int _olympiadObserveId = -1;

    public int getOlympiadObserveId() {
        return _olympiadObserveId;
    }

    public boolean isOlympiadGameStart() {
        int id = _olympiadGameId;
        if (id < 0) {
            return false;
        }
        OlympiadGame og = Olympiad.getOlympiadGame(id);
        return og != null && og.getState() == 1;
    }

    public boolean isOlympiadCompStart() {
        int id = _olympiadGameId;
        if (id < 0) {
            return false;
        }
        OlympiadGame og = Olympiad.getOlympiadGame(id);
        return og != null && og.getState() == 2;
    }

    @Override
    public boolean isHero() {
        return _hero;
    }

    public boolean isNoble() {
        return _noble;
    }

    public void setNoble(boolean f) {
        if (f) {
            for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills()) {
                addSkill(s, false); // Dont Save Noble skills to Sql
            }
        } else {
            for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills()) {
                super.removeSkill(s); // Just Remove skills without deleting from Sql
            }
        }
        _noble = f;

        sendSkillList();
    }

    public void setLvlJoinedAcademy(int lvl) {
        _lvlJoinedAcademy = lvl;
    }

    public int getLvlJoinedAcademy() {
        return _lvlJoinedAcademy;
    }

    @Override
    public boolean isAcademyMember() {
        return _lvlJoinedAcademy > 0;
    }

    public void setTeam(int team) {
        _team = team;
        broadcastUserInfo();
        if (getPet() != null) {
            getPet().broadcastPetInfo();
            getPet().updateAbnormalEffect();
        }
    }

    public int getTeam() {
        /*
         * L2ItemInstance flag = getInventory().getItemByItemId(50000); if (flag
         * != null && flag.isEquipped()) return 2;
         */

        return _team;
    }

    public void setWantsPeace(int wantsPeace) {
        _wantsPeace = wantsPeace;
    }

    @Override
    public int getWantsPeace() {
        return _wantsPeace;
    }

    @Override
    public boolean isFishing() {
        return _fishing;
    }

    public void setFishing(boolean f) {
        _fishing = f;
    }

    public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance) {
        // [-5,-1] varka, 0 neutral, [1,5] ketra
        _alliedVarkaKetra = sideAndLvlOfAlliance;
    }

    public int getAllianceWithVarkaKetra() {
        return _alliedVarkaKetra;
    }

    public boolean isAlliedWithVarka() {
        return (_alliedVarkaKetra < 0);
    }

    public boolean isAlliedWithKetra() {
        return (_alliedVarkaKetra > 0);
    }

    public void sendSkillList() {
        SkillList sl = new SkillList();
        for (L2Skill s : getAllSkills()) {
            if (s == null) {
                continue;
            }
            if (s.getId() > 9000) {
                continue; // Fake skills to change base stats
            }
            sl.addSkill(s.getId(), s.getLevel(), s.isPassive(), s.isDisabledFor(this));
        }
        sendUserPacket(sl);
    }

    /**
     * 1. Add the specified class ID as a subclass (up to the maximum number of
     * <b>three</b>) for this character.<BR>
     * 2. This method no longer changes
     * the active _classIndex of the player. This is only done by the calling of
     * setActiveClass() method as that should be the only way to do so.
     *
     * @param int classId
     * @param int classIndex
     * @return boolean subclassAdded
     */
    public boolean addSubClass(int classId, int classIndex) {
        if (getTotalSubClasses() == Config.MAX_SUBCLASS || classIndex == 0) {
            return false;
        }

        if (getSubClasses().containsKey(classIndex)) {
            return false;
        }

        // Note: Never change _classIndex in any method other than setActiveClass().
        SubClass newClass = new SubClass();
        newClass.setClassId(classId);
        newClass.setClassIndex(classIndex);

        Connect con = null;
        PreparedStatement st = null;
        try {
            // Store the basic info about this new sub-class.
            con = L2DatabaseFactory.get();
            st = con.prepareStatement(
                    "REPLACE INTO character_subclasses (char_obj_id,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)");
            st.setInt(1, getObjectId());
            st.setInt(2, newClass.getClassId());
            st.setLong(3, newClass.getExp());
            st.setInt(4, newClass.getSp());
            st.setInt(5, newClass.getLevel());
            st.setInt(6, newClass.getClassIndex()); // <-- Added
            st.execute();
        } catch (SQLException e) {
            _log.warning("WARNING: Could not add character sub class for " + getName() + ": " + e);
            return false;
        } finally {
            Close.CS(con, st);
        }

        // Commit after database INSERT incase exception is thrown.
        getSubClasses().put(newClass.getClassIndex(), newClass);

        // if (Config.DEBUG)
        // _log.info(getName() + " added class ID " + classId + " as a sub class at
        // index " + classIndex + ".");
        ClassId subTemplate = ClassId.values()[classId];
        Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);

        if (skillTree == null) {
            return true;
        }

        Map<Integer, L2Skill> prevSkillList = new FastMap<Integer, L2Skill>();

        for (L2SkillLearn skillInfo : skillTree) {
            if (skillInfo == null) {
                continue;
            }
            if (skillInfo.getMinLevel() <= 40) {
                L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());
                if (newSkill == null) {
                    _log.warning("WARNING: SkillInfo id " + skillInfo.getId() + ":" + skillInfo.getLevel()
                            + "level not founded.");
                    continue;
                }
                L2Skill prevSkill = prevSkillList.get(skillInfo.getId());

                if (prevSkill != null && (prevSkill.getLevel() > newSkill.getLevel())) {
                    continue;
                }

                prevSkillList.put(newSkill.getId(), newSkill);
                storeSkill(newSkill, prevSkill, classIndex);
            }
        }

        // if (Config.DEBUG)
        // _log.info(getName() + " was given " + getAllSkills().length + " skills for
        // their new sub class.");
        return true;
    }

    /**
     * 1. Completely erase all existance of the subClass linked to the
     * classIndex.<BR>
     * 2. Send over the newClassId to addSubClass()to create a
     * new instance on this classIndex.<BR>
     * 3. Upon Exception, revert the player
     * to their BaseClass to avoid further problems.<BR>
     *
     * @param int classIndex
     * @param int newClassId
     * @return boolean subclassAdded
     */
    public boolean modifySubClass(int classIndex, int newClassId) {
        // int oldClassId = getSubClasses().get(classIndex).getClassId();

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            con.setAutoCommit(false);
            // Remove all henna info stored for this sub-class.
            st = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, classIndex);
            st.execute();
            Close.S(st);

            // Remove all shortcuts info stored for this sub-class.
            st = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, classIndex);
            st.execute();
            Close.S(st);

            // Remove all effects info stored for this sub-class.
            st = con.prepareStatement("DELETE FROM character_buffs WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, classIndex);
            st.execute();
            Close.S(st);

            // Remove all skill info stored for this sub-class.
            st = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, classIndex);
            st.execute();
            Close.S(st);

            // Remove all basic info stored about this sub-class.
            st = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, getObjectId());
            st.setInt(2, classIndex);
            st.execute();
            con.commit();
        } catch (SQLException e) {
            // con.rollback();
            _log.warning("Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e);
            // This must be done in order to maintain data consistency.
            getSubClasses().remove(classIndex);
            return false;
        } finally {
            Close.CS(con, st);
        }

        getSubClasses().remove(classIndex);
        return addSubClass(newClassId, classIndex);
    }

    public boolean isSubClassActive() {
        return _classIndex > 0;
    }

    public Map<Integer, SubClass> getSubClasses() {
        if (_subClasses == null) {
            _subClasses = new FastMap<Integer, SubClass>();
        }

        return _subClasses;
    }

    public int getTotalSubClasses() {
        return getSubClasses().size();
    }

    public int getBaseClass() {
        return _baseClass;
    }

    public int getActiveClass() {
        return _activeClass;
    }

    public int getClassIndex() {
        return _classIndex;
    }

    public int getClassIndexHenna() {
        return Config.ALT_HENNA_SUB ? 0 : _classIndex;
    }

    private void setClassTemplate(int classId) {
        _activeClass = classId;

        L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);

        if (t == null) {
            _log.severe("Missing template for classId: " + classId);
            throw new Error();
        }

        // Set the template of the L2PcInstance
        setTemplate(t);
    }

    /**
     * Changes the character's class based on the given class index. <BR>
     * <BR>
     * An
     * index of zero specifies the character's original (base) class, while
     * indexes 1-3 specifies the character's sub-classes respectively.
     *
     * @param classIndex
     */
    public boolean setActiveClass(int classIndex) {
        _classUpdate = true;

        L2ItemInstance under = getInventory().getPaperdollItem(Inventory.PAPERDOLL_UNDER);
        if (under != null) {
            L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(under.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (L2ItemInstance element : unequipped) {
                iu.addModifiedItem(element);
            }
            sendUserPacket(iu);
        }

        // Remove active item skills before saving char to database
        // because next time when choosing this class, weared items can
        // be different
        for (L2ItemInstance temp : getInventory().getAugmentedItems()) {
            if (temp != null && temp.isEquipped()) {
                temp.getAugmentation().removeBoni(this);
            }
        }

        // Delete a force buff upon class change.
        if (_forceBuff != null) {
            abortCast();
        }

        if (getFairy() != null) {
            getFairy().unSummon(this);
        }

        clearProtectedSubBuffs(Config.STOP_SUB_BUFFS);

        // Stop casting for any player that may be casting a force buff on this
        // l2pcinstance.
        // for(L2Character character : getKnownList().getKnownCharacters())
        // {
        // if(character.getForceBuff() != null && character.getForceBuff().getTarget()
        // == this)
        // character.abortCast();
        // }
        /*
         * 1. Call store() before modifying _classIndex to avoid skill effects
         * rollover. 2. Register the correct _classId against applied
         * 'classIndex'.
         */
        store();
        clearDisabledSkills();

        if (classIndex == 0) {
            setClassTemplate(getBaseClass());
        } else {
            try {
                setClassTemplate(getSubClasses().get(classIndex).getClassId());
            } catch (Exception e) {
                _log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
                return false;
            }
        }
        _classIndex = classIndex;

        if (isInParty()) {
            getParty().recalculatePartyLevel();
        }

        /*
         * Update the character's change in class status.
         *
         * 1. Remove any active cubics from the player. 2. Renovate the
         * characters table in the database with the new class info, storing
         * also buff/effect data. 3. Remove all existing skills. 4. Restore all
         * the learned skills for the current class from the database. 5.
         * Restore effect/buff data for the new class. 6. Restore henna data for
         * the class, applying the new stat modifiers while removing existing
         * ones. 7. Reset HP/MP/CP stats and send Server->Client character
         * status packet to reflect changes. 8. Restore shortcut data related to
         * this class. 9. Resend a class change animation effect to broadcast to
         * all nearby players. 10.Unsummon any active servitor from the player.
         */
        // if (getPet() != null && getPet().isSummon())
        // getPet().unSummon(this);
        if (!getCubics().isEmpty()) {
            for (L2CubicInstance cubic : getCubics().values()) {
                cubic.stopAction();
                cubic.cancelDisappear();
            }

            getCubics().clear();
        }

        // Delete a force buff upon class change.
        if (_forceBuff != null) {
            _forceBuff.delete();
        }

        // Stop casting for any player that may be casting a force buff on this
        // l2pcinstance.
        // for(L2Character character : getKnownList().getKnownCharacters())
        // {
        // if(character.getForceBuff() != null && character.getForceBuff().getTarget()
        // == this)
        // character.abortCast();
        // }
        for (L2Skill oldSkill : getAllSkills()) {
            super.removeSkill(oldSkill);
        }

        // Yesod: Rebind CursedWeapon passive.
        if (isCursedWeaponEquiped()) {
            CursedWeaponsManager.getInstance().givePassive(_cursedWeaponEquipedId);
        }

        stopAllEffects();
        clearCharges();

        Connect con = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            if (isSubClassActive()) {
                _dwarvenRecipeBook.clear();
                _commonRecipeBook.clear();
            } else {
                restoreRecipeBook(con);
            }

            restoreSkills(con);

            restoreEffects(con);

            for (int i = 0; i < 3; i++) {
                _henna[i] = null;
            }

            restoreHenna(con);

            _shortCuts.restore(con);
        } catch (Exception e) {
            _log.warning("setActiveClass [ERROR]: " + e);
        } finally {
            Close.C(con);
        }

        /*
         * if (isSubClassActive()) { _dwarvenRecipeBook.clear();
         * _commonRecipeBook.clear(); } else restoreRecipeBook(con);
         */
        // Restore any Death Penalty Buff
        restoreDeathPenaltyBuffLevel();

        // restoreSkills(con);
        regiveTemporarySkills();
        if (_clan != null) {
            _clan.addBonusEffects(this, true, false);
        }
        rewardSkills();
        if (Config.RELOAD_SUB_SKILL) {
            clearDisabledSkills();
        }

        // restoreEffects(con);
        // updateEffectIcons();
        sendEtcStatusUpdate();

        // if player has quest 422: Repent Your Sins, remove it
        QuestState st = getQuestState("422_RepentYourSins");
        if (st != null) {
            st.exitQuest(true);
        }

        // for (int i = 0; i < 3; i++)
        // _henna[i] = null;
        // restoreHenna(con);
        sendUserPacket(new HennaInfo(this));

        // if (getCurrentHp() > getMaxHp())
        // setCurrentHp(getMaxHp());
        // if (getCurrentMp() > getMaxMp())
        // setCurrentMp(getMaxMp());
        // if (getCurrentCp() > getMaxCp())
        // setCurrentCp(getMaxCp());
        refreshOverloaded();

        _expertisePenalty = 0;
        _classUpdate = false;
        refreshExpertisePenalty();

        broadcastUserInfo();

        // Clear resurrect xp calculation
        setExpBeforeDeath(0);
        // _macroses.restore();
        // _macroses.sendUpdate();
        // _shortCuts.restore(con);
        // Close.C(con);
        sendUserPacket(new ShortCutInit(this));

        if (getKnownSkill(1324) != null && Config.SUMMON_CP_PROTECT) {
            L2Skill pero = getKnownSkill(1324);
            int reuseDelay = (int) (pero.getReuseDelay() * getStat().getMReuseRate(pero));
            reuseDelay *= 333.0 / getMAtkSpd();
            disableSkill(pero.getId(), reuseDelay);
        }

        clearProtectedSubBuffs(Config.STOP_SUB_BUFFS);

        broadcastPacket(new SocialAction(getObjectId(), 15));
        sendSkillCoolTime();
        sendActionFailed();
        return true;
    }

    private void clearProtectedSubBuffs(FastList<Integer> buffs) {
        if (buffs.isEmpty()) {
            return;
        }

        for (Integer id : buffs) {
            if (id == null) {
                continue;
            }
            stopSkillEffects(id);
        }
    }

    public void stopWarnUserTakeBreak() {
        if (_taskWarnUserTakeBreak != null) {
            _taskWarnUserTakeBreak.cancel(true);
            _taskWarnUserTakeBreak = null;
        }
    }

    public void startWarnUserTakeBreak() {
        if (_taskWarnUserTakeBreak == null) {
            _taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreak(),
                    7200000, 7200000);
        }
    }

    public void stopRentPet() {
        stopRentPet(true);
    }

    private void stopRentPet(boolean unmount) {
        if (_taskRentPet != null) {
            // if the rent of a wyvern expires while over a flying zone, tp to down before
            // unmounting
            if (checkLandingState() && getMountType() == 2) {
                teleToLocation(MapRegionTable.TeleportWhereType.Town);
            }

            if (unmount && setMountType(0)) // this should always be true now, since we teleported already
            {
                if (isFlying()) {
                    removeSkill(SkillTable.getInstance().getInfo(327, 1));
                    removeSkill(SkillTable.getInstance().getInfo(4289, 1));
                }

                sendUserPacket(new SetupGauge(3, 0));
                _taskRentPet.cancel(true);
                broadcastPacket(new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0));
                setMountObjectID(0);
                broadcastUserInfo();
                _taskRentPet = null;
            }
        }
    }

    public void startRentPet(int seconds) {
        if (_taskRentPet == null) {
            sendUserPacket(new SetupGauge(3, seconds * 1000));
            _taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(),
                    seconds * 1000L, seconds * 1000L);
        }
    }

    public void startUnmountPet(int seconds) {
        if (seconds == 0 || (Config.EENC_ENABLE && Encounter.getEvent().isRegged(this))) {
            return;
        }
        if (_taskRentPet == null) {
            sendUserPacket(new SetupGauge(3, seconds));
            _taskRentPet = EffectTaskManager.getInstance().schedule(new RentPetTask(), seconds);
        }
    }

    public boolean isRentedPet() {
        return (_taskRentPet != null);

    }

    private int _waterZone = -1;

    class WaterTask implements Runnable {

        public void run() {
            double reduceHp = getMaxHp() / 100.0;

            if (reduceHp < 1) {
                reduceHp = 1;
            }

            reduceCurrentHp(reduceHp, L2PcInstance.this, false);
            // reduced hp, becouse not rest
            sendUserPacket(SystemMessage.id(SystemMessageId.DROWN_DAMAGE_S1).addNumber((int) reduceHp));
        }
    }

    @Override
    public void stopWaterTask(int waterZone) {
        if (waterZone != -5 && _waterZone != waterZone) {
            return;
        }

        if (_taskWater != null) {
            _taskWater.cancel(false);
            _taskWater = null;
            sendUserPacket(new SetupGauge(2, 0));
            sendChanges();
        }
    }

    @Override
    public void startWaterTask(int waterZone) {
        _waterZone = waterZone;
        if (isDead()) {
            stopWaterTask(waterZone);
        } else if (Config.ALLOW_WATER && _taskWater == null) {
            int timeinwater = 86000;
            sendUserPacket(new SetupGauge(2, timeinwater));
            _taskWater = EffectTaskManager.getInstance().scheduleAtFixedRate(new WaterTask(), timeinwater, 1000);
            sendChanges();
        }
    }

    @Override
    public boolean isInWater() {
        return (_taskWater != null);
    }

    public void onPlayerEnter() {
        startWarnUserTakeBreak();

        if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod()) {
            if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) != SevenSigns.getInstance()
                    .getCabalHighestScore()) {
                teleToLocation(MapRegionTable.TeleportWhereType.Town);
                setIsIn7sDungeon(false);
                sendMessage(Static.SEVEN_SGINS1);
            }
        } else if (!isGM() && isIn7sDungeon()
                && SevenSigns.getInstance().getPlayerCabal(this) == SevenSigns.CABAL_NULL) {
            teleToLocation(MapRegionTable.TeleportWhereType.Town);
            setIsIn7sDungeon(false);
            sendMessage(Static.SEVEN_SGINS2);
        }

        // jail task
        updateJailState();

        if (_isInvul) {
            sendAdmResultMessage(Static.STATE_INVUL);
        }
        if (isInvisible()) {
            sendAdmResultMessage(Static.STATE_INVIS);
        }
        if (getMessageRefusal()) {
            sendAdmResultMessage(Static.STATE_PMBLOCK);
        }
    }

    public long getLastAccess() {
        return _lastAccess;
    }

    private void checkRecom(int recsHave, int recsLeft) {
        Calendar check = Calendar.getInstance();
        check.setTimeInMillis(_lastRecomUpdate);
        check.add(Calendar.DAY_OF_MONTH, 1);

        Calendar min = Calendar.getInstance();

        _recomHave = recsHave;
        _recomLeft = recsLeft;

        if (getStat().getLevel() < 10 || check.after(min)) {
            return;
        }

        restartRecom();
    }

    public void restartRecom() {
        if (Config.ALT_RECOMMEND) {
            Connect con = null;
            PreparedStatement st = null;
            try {
                con = L2DatabaseFactory.get();
                st = con.prepareStatement("DELETE FROM character_recommends WHERE char_id=?");
                st.setInt(1, getObjectId());
                st.execute();

                _recomChars.clear();
            } catch (Exception e) {
                _log.warning("could not clear char recommendations: " + e);
            } finally {
                Close.CS(con, st);
            }
        }

        if (getStat().getLevel() < 20) {
            _recomLeft = 3;
            _recomHave--;
        } else if (getStat().getLevel() < 40) {
            _recomLeft = 6;
            _recomHave -= 2;
        } else {
            _recomLeft = 9;
            _recomHave -= 3;
        }
        if (_recomHave < 0) {
            _recomHave = 0;
        }

        // If we have to update last update time, but it's now before 13, we should set
        // it to yesterday
        Calendar update = Calendar.getInstance();
        if (update.get(Calendar.HOUR_OF_DAY) < 13) {
            update.add(Calendar.DAY_OF_MONTH, -1);
        }
        update.set(Calendar.HOUR_OF_DAY, 13);
        _lastRecomUpdate = update.getTimeInMillis();
    }

    @Override
    public void doRevive() {
        super.doRevive();

        if (isPhoenixBlessed()) {
            stopPhoenixBlessing(null);
        }

        updateEffectIcons();
        sendEtcStatusUpdate();
        _reviveRequested = 0;
        _revivePower = 0;

        L2Skill pero = this.getKnownSkill(1410);
        if (pero != null && isInsidePvpZone()) {
            int reuseDelay = (int) (pero.getReuseDelay() * getStat().getMReuseRate(pero));
            reuseDelay *= 1380.0 / getMAtkSpd();
            disableSkill(pero.getId(), reuseDelay);
        }

        if (getPvpFlag() != 0) {
            setPvpFlag(0);
        }

        if (isInParty() && getParty().isInDimensionalRift()) {
            if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ())) {
                getParty().getDimensionalRift().memberRessurected(this);
            }
        }
    }

    @Override
    public void doRevive(double revivePower) {
        // Restore the player's lost experience,
        // depending on the % return of the skill used (based on its power).
        restoreExp(revivePower);
        doRevive();
    }

    private long _reviveTime = 0;

    public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet) {
        if (_reviveRequested == 1) {
            if (_revivePet == Pet) {
                Reviver.sendUserPacket(Static.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
            } else if (Pet) {
                Reviver.sendUserPacket(Static.PET_CANNOT_RES); // A pet cannot be resurrected while it's owner is in the
                                                               // process of resurrecting.
            } else {
                Reviver.sendUserPacket(Static.MASTER_CANNOT_RES); // While a pet is attempting to resurrect, it cannot
                                                                  // help in resurrecting its master.
            }
            return;
        }
        if ((Pet && getPet() != null && getPet().isDead()) || (!Pet && isDead())) {
            _reviveRequested = 1;
            if (isPhoenixBlessed()) {
                _revivePower = 100;
            } else {
                _revivePower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), Reviver.getWIT());
            }

            _reviveTime = System.currentTimeMillis() + Config.RESURECT_ANSWER_TIME;
            _revivePet = Pet;
            sendUserPacket(new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST.getId(), Reviver.getName()));
        }
    }

    public void reviveAnswer(int answer) {
        if (_reviveRequested != 1 || (!isDead() && !_revivePet)
                || (_revivePet && getPet() != null && !getPet().isDead())) {
            return;
        }

        if (Config.RESURECT_ANSWER_TIME > 0 && System.currentTimeMillis() > _reviveTime) {
            sendUserPacket(Static.ANSWER_TIMEOUT);
            return;
        }

        // If character refuse a PhoenixBlessed autoress, cancel all buffs he had
        if (answer == 0 && isPhoenixBlessed()) {
            stopPhoenixBlessing(null);
            // stopAllEffects();
        }
        if (answer == 1) {
            if (!_revivePet) {
                if (_revivePower != 0) {
                    doRevive(_revivePower);
                } else {
                    doRevive();
                }
            } else if (getPet() != null) {
                if (_revivePower != 0) {
                    getPet().doRevive(_revivePower);
                } else {
                    getPet().doRevive();
                }
            }
        }
        _reviveRequested = 0;
        _revivePower = 0;
    }

    @Override
    public boolean isReviveRequested() {
        return (_reviveRequested == 1);
    }

    @Override
    public boolean isRevivingPet() {
        return _revivePet;
    }

    public void removeReviving() {
        _reviveRequested = 0;
        _revivePower = 0;
    }

    /**
     * @param expertiseIndex The expertiseIndex to set.
     */
    public void setExpertiseIndex(int expertiseIndex) {
        _expertiseIndex = expertiseIndex;
    }

    /**
     * @return Returns the expertiseIndex.
     */
    public int getExpertiseIndex() {
        return _expertiseIndex;
    }

    @Override
    public final void onTeleported() {
        super.onTeleported();

        // Force a revalidation
        revalidateZone(true);
        updateLastTeleport(true);

        // Modify the position of the tamed beast if necessary (normal pets are handled
        // by super...though
        // L2PcInstance is the only class that actually has pets!!! )
        if (getTrainedBeast() != null) {
            getTrainedBeast().getAI().stopFollow();
            getTrainedBeast().teleToLocation(getPosition().getX() + Rnd.get(-100, 100),
                    getPosition().getY() + Rnd.get(-100, 100), getPosition().getZ(), false);
            getTrainedBeast().getAI().startFollow(this);
        }

        // Modify the position of the pet if necessary
        if (getPet() != null) {
            // getPet().setFollowStatus(false);
            // getPet().teleToLocation(getPosition().getX(), getPosition().getY(),
            // getPosition().getZ(), false);
            ((L2SummonAI) getPet().getAI()).setStartFollowController(true);
            getPet().setFollowStatus(true);
            getPet().updateAndBroadcastStatus(0);
        }

        if (getFairy() != null) {
            getFairy().getAI().stopFollow();
            getFairy().teleToLocation(getPosition().getX() + Rnd.get(-50, 50), getPosition().getY() + Rnd.get(-50, 50),
                    getPosition().getZ(), false);
            // getFairy().getAI().setStartFollowController(true);
            getFairy().getAI().startFollow(this);
            getFairy().setFollowStatus(true);
            getFairy().updateAndBroadcastStatus(0);
        }

        if (_partner != null) {
            _partner.getAI().stopFollow();
            _partner.teleToLocation(getPosition().getX() + Rnd.get(-50, 50), getPosition().getY() + Rnd.get(-50, 50),
                    getPosition().getZ(), false);
            // getFairy().getAI().setStartFollowController(true);
            _partner.getAI().startFollow(this);
            _partner.setFollowStatus(true);
            _partner.updateAndBroadcastPartnerStatus(0);
            _partner.onTeleported();
        }

        if (!_isPartner) {
            sendUserPacket(new UserInfo(this));
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
        }
    }

    /*
     * @Override public final boolean updatePosition(int gameTicks) { //
     * Disables custom movement for L2PCInstance when Old Synchronization is
     * selected if (Config.COORD_SYNCHRONIZE == -1) return
     * super.updatePosition(gameTicks);
     *
     * // Get movement data MoveData m = _move;
     *
     * if (_move == null) return true;
     *
     * if (!isVisible()) { _move = null; return true; }
     *
     * // Check if the position has alreday be calculated if (m._moveTimestamp
     * == 0) m._moveTimestamp = m._moveStartTime;
     *
     * // Check if the position has alreday be calculated if (m._moveTimestamp
     * == gameTicks) return false;
     *
     * double dx = m._xDestination - getX(); double dy = m._yDestination -
     * getY(); double dz = m._zDestination - getZ(); int distPassed =
     * (int)getStat().getMoveSpeed() * (gameTicks - m._moveTimestamp) /
     * GameTimeController.TICKS_PER_SECOND; double distFraction = (distPassed) /
     * Math.sqrt(dx*dx + dy*dy + dz*dz); // if (Config.DEVELOPER)
     * System.out.println("Move Ticks:" + (gameTicks - m._moveTimestamp) + ",
     * distPassed:" + distPassed + ", distFraction:" + distFraction);
     *
     * if (distFraction > 1) { // Set the position of the L2Character to the
     * destination super.setXYZ(m._xDestination, m._yDestination,
     * m._zDestination); } else { // Set the position of the L2Character to
     * estimated after parcial move super.setXYZ(getX() + (int)(dx *
     * distFraction + 0.5), getY() + (int)(dy * distFraction + 0.5), getZ() +
     * (int)(dz * distFraction)); }
     *
     * // Set the timer of last position update to now m._moveTimestamp =
     * gameTicks;
     *
     * revalidateZone(false);
     *
     * return (distFraction > 1); }
     */
    @Override
    public void addExpAndSp(long exp, int sp) {
        if (Config.PREMIUM_ENABLE && isPremium()) {
            sp *= Config.PREMIUM_SP;
            exp *= Config.PREMIUM_EXP;
        }
        getStat().addExpAndSp(exp, sp);
    }

    public void removeExpAndSp(long removeExp, int removeSp) {
        getStat().removeExpAndSp(removeExp, removeSp);
    }

    @Override
    public void reduceCurrentHp(double i, L2Character attacker) {
        getStatus().reduceHp(i, attacker);

        // notify the tamed beast of attacks
        if (getTrainedBeast() != null) {
            getTrainedBeast().onOwnerGotAttacked(attacker);
        }

        if (_isPartner) {
            _owner.sendMessage("Партнер получает " + ((int) i) + " урона от " + attacker.getName());
        }
        if (_partner != null) {
            _partner.getAI().onOwnerGotAttacked(attacker);
        }
    }

    @Override
    public void reduceCurrentHp(double value, L2Character attacker, boolean awake) {
        getStatus().reduceHp(value, attacker, awake);

        // notify the tamed beast of attacks
        if (getTrainedBeast() != null) {
            getTrainedBeast().onOwnerGotAttacked(attacker);
        }
        if (_isPartner) {
            _owner.sendMessage("Партнер получает " + ((int) value) + " урона от " + attacker.getName());
        }
        if (_partner != null) {
            _partner.getAI().onOwnerGotAttacked(attacker);
        }
    }

    public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean hp) {
        getStatus().reduceHp(value, attacker, awake, true);

        // notify the tamed beast of attacks
        if (getTrainedBeast() != null) {
            getTrainedBeast().onOwnerGotAttacked(attacker);
        }
        if (_isPartner) {
            _owner.sendMessage("Партнер получает " + ((int) value) + " урона от " + attacker.getName());
        }
        if (_partner != null) {
            _partner.getAI().onOwnerGotAttacked(attacker);
        }
    }

    public void broadcastSnoop(int type, String name, String _text) {
        if (_snoopListener.size() > 0) {
            Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text);
            for (L2PcInstance pci : _snoopListener) {
                if (pci != null) {
                    pci.sendPacket(sn);
                }
            }
        }
    }

    public void addSnooper(L2PcInstance pci) {
        if (!_snoopListener.contains(pci)) {
            _snoopListener.add(pci);
        }
    }

    public void removeSnooper(L2PcInstance pci) {
        _snoopListener.remove(pci);
    }

    public void addSnooped(L2PcInstance pci) {
        if (!_snoopedPlayer.contains(pci)) {
            _snoopedPlayer.add(pci);
        }
    }

    public void removeSnooped(L2PcInstance pci) {
        _snoopedPlayer.remove(pci);
    }

    public synchronized void addBypass(String bypass) {
        if (bypass == null) {
            return;
        }
        _validBypass.add(bypass);
        // _log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
    }

    public synchronized void addBypass2(String bypass) {
        if (bypass == null) {
            return;
        }
        _validBypass2.add(bypass);
        // _log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
    }

    public boolean validateItemManipulation(int objectId, String action) {
        L2ItemInstance item = getInventory().getItemByObjectId(objectId);

        if (item == null || item.getOwnerId() != getObjectId()) {
            _log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
            return false;
        }

        // Pet is summoned and not the item that summoned the pet AND not the buggle
        // from strider you're mounting
        if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId) {
            // if (Config.DEBUG)
            // _log.finest(getObjectId()+": player tried to " + action + " item controling
            // pet");

            return false;
        }

        if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId) {
            // if (Config.DEBUG)
            // _log.finest(getObjectId()+":player tried to " + action + " an enchant scroll
            // he was using");

            return false;
        }

        if (CursedWeaponsManager.getInstance().isCursed(item.getItemId())) {
            // can not trade a cursed weapon
            return false;
        }

        if (item.isWear()) {
            // cannot drop/trade wear-items
            return false;
        }

        return true;
    }

    public synchronized void clearBypass() {
        _validBypass.clear();
        _validBypass2.clear();
    }

    //
    private List<String> bypasses = null, bypasses_bbs = null;

    private List<String> getStoredBypasses(boolean bbs) {
        if (bbs) {
            if (bypasses_bbs == null) {
                bypasses_bbs = new ArrayList<String>();
            }
            return bypasses_bbs;
        }
        if (bypasses == null) {
            bypasses = new ArrayList<String>();
        }
        return bypasses;
    }

    public void cleanBypasses(boolean bbs) {
        List<String> bypassStorage = getStoredBypasses(bbs);
        synchronized (bypassStorage) {
            bypassStorage.clear();
        }
    }

    public String encodeBypasses(String htmlCode, boolean bbs) {
        List<String> bypassStorage = getStoredBypasses(bbs);
        synchronized (bypassStorage) {
            return BypassManager.encode(htmlCode, bypassStorage, bbs);
        }
    }

    public DecodedBypass decodeBypass(String bypass) {
        BypassType bpType = BypassManager.getBypassType(bypass);
        boolean bbs = bpType == BypassType.ENCODED_BBS || bpType == BypassType.SIMPLE_BBS;
        List<String> bypassStorage = getStoredBypasses(bbs);
        if (bpType == BypassType.ENCODED || bpType == BypassType.ENCODED_BBS) {
            return BypassManager.decode(bypass, bypassStorage, bbs, this);
        }
        if (bpType == BypassType.SIMPLE) {
            return new DecodedBypass(bypass, false).trim();
        }
        if (bpType == BypassType.SIMPLE_BBS && !bypass.startsWith("_bbsscripts")) {
            return new DecodedBypass(bypass, true).trim();
        }
        // BaseBBSManager handler = CommunityBoard.getInstance().getHandler(bypass);
        if (CommunityBoard.getInstance().findBypass(bypass)) {
            return new DecodedBypass(bypass, null).trim();
        }
        // _log.warn("Direct access to bypass: " + bypass + " / Player: " + getName());
        Log.add("Запрос: " + bypass + " / " + getFingerPrints(), "cheats/BypassValidate");
        return null;
    }

    public String getFingerPrints() {
        return "Player: " + getName() + "(" + getObjectId() + "), account: " + getAccountName() + ", ip: " + getIP()
                + ", hwid: " + getHWID() + "";
    }

    /**
     * @return Returns the inBoat.
     */
    @Override
    public boolean isInBoat() {
        return _inBoat;
    }

    /**
     * @param inBoat The inBoat to set.
     */
    public void setInBoat(boolean f) {
        _inBoat = f;
    }

    /**
     * @return
     */
    @Override
    public L2BoatInstance getBoat() {
        return _boat;
    }

    /**
     * @param boat
     */
    public void setBoat(L2BoatInstance boat) {
        _boat = boat;
    }

    public void setInCrystallize(boolean f) {
        _inCrystallize = f;
    }

    public boolean isInCrystallize() {
        return _inCrystallize;
    }

    /**
     * @return
     */
    public Point3D getInBoatPosition() {
        return _inBoatPosition;
    }

    public void setInBoatPosition(Point3D pt) {
        _inBoatPosition = pt;
    }

    /**
     * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save
     * its inventory in the database, Remove it from the world...).<BR>
     * <BR>
     *
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>If the L2PcInstance is in observer
     * mode, set its position to its position before entering in observer mode
     * </li>
     * <li>Set the online Flag to True or False and update the characters
     * table of the database with online status and lastAccess</li>
     * <li>Stop
     * the HP/MP/CP Regeneration task</li>
     * <li>Cancel Crafting, Attak or Cast
     * </li>
     * <li>Remove the L2PcInstance from the world</li>
     * <li>Stop Party and
     * Unsummon Pet</li>
     * <li>Update database with items in its inventory and
     * remove them from the world</li>
     * <li>Remove all L2Object from
     * _knownObjects and _knownPlayer of the L2Character then cancel Attak or
     * Cast and notify AI</li>
     * <li>Close the connection with the client
     * </li><BR>
     * <BR>
     *
     */
    public void deleteMe() {
        if (_fantome && isVisible()) {
            try {
                decayMe();
            } catch (Throwable t) {
                _log.log(Level.SEVERE, "deleteMe()", t);
            }
            return;
        }

        /*
         * if(isFlying() && isInsideNoLandZone()) teleToClosestTown();
         */
        if (hasExitPenalty()) {
            teleToClosestTown();
        }

        _isDeleting = true;
        setInGame(false);

        abortAttack();
        abortCast();

        if (getActiveTradeList() != null) {
            cancelActiveTrade();

            if (getTransactionRequester() != null) {
                getTransactionRequester().setTransactionRequester(null);
            }
            setTransactionRequester(null);
        }

        if (getTransactionRequester() != null) {
            getTransactionRequester().setTransactionRequester(null);
            setTransactionRequester(null);
        }

        // Check if the L2PcInstance is in observer mode to set its position to its
        // position before entering in observer mode
        if (inObserverMode()) {
            if (getOlympiadObserveId() == -1) {
                leaveObserverMode();
            } else {
                leaveOlympiadObserverMode();
            }
        }

        if (Olympiad.isRegisteredInComp(this) || isInOlympiadMode() || getOlympiadGameId() > -1) {
            Olympiad.logoutPlayer(this);
        }

        if (isInDuel()) {
            getDuel().onPlayerDefeat(this);
        }

        // Set the online Flag to True or False and update the characters table of the
        // database with online status and lastAccess (called when login and logout)
        try {
            setOnlineStatus(false);
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        // Евенты
        try {
            EventManager.getInstance().onExit(this);
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        // Stop the HP/MP/CP Regeneration task (scheduled tasks)
        try {
            stopAllTimers();
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        // Stop crafting, if in progress
        try {
            RecipeController.getInstance().requestMakeItemAbort(this);
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        // Cancel Attak or Cast
        try {
            setTarget(null);
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        // Remove from world regions zones
        if (getWorldRegion() != null) {
            getWorldRegion().removeFromZones(this);
        }

        try {
            // запоминаем баффы
            storeBuffProfiles();
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        /*
         * try { //запоминаем макросы _macroses.store(); } catch (Throwable t)
         * {_log.log(Level.SEVERE, "deleteMe()", t); }
         */

        /*
         * try { //запоминаем панельку _shortCuts.store(); } catch (Throwable t)
         * {_log.log(Level.SEVERE, "deleteMe()", t); }
         */
        try {
            if (_forceBuff != null) {
                _forceBuff.delete();
            }
            for (L2Character character : getKnownList().getKnownCharacters()) {
                if (character.getForceBuff() != null && character.getForceBuff().getTarget() == this) {
                    character.abortCast();
                }
            }
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        try {
            /*
             * FastTable<L2Effect> effects = getAllEffectsTable(); for (int i =
             * 0, n = effects.size(); i < n; i++) { L2Effect effect =
             * effects.get(i); if (effect == null) continue; switch
             * (effect.getEffectType()) { case SIGNET_GROUND: case
             * SIGNET_EFFECT: effect.exit(); break; } } effects.clear(); effects
             * = null;
             */
            for (L2Effect effect : getAllEffectsTable()) {
                if (effect == null) {
                    continue;
                }
                switch (effect.getEffectType()) {
                    case SIGNET_GROUND:
                    case SIGNET_EFFECT:
                        effect.exit();
                        break;
                }
            }
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        // Remove the L2PcInstance from the world
        if (isVisible()) {
            try {
                decayMe();
            } catch (Throwable t) {
                _log.log(Level.SEVERE, "deleteMe()", t);
            }
        }

        // If a Party is in progress, leave it
        if (isInParty()) {
            try {
                leaveOffParty();
            } catch (Throwable t) {
                _log.log(Level.SEVERE, "deleteMe()", t);
            }
        }

        // If the L2PcInstance has Pet, unsummon it
        if (getFairy() != null) {
            try {
                getFairy().unSummon(this);
            } catch (Throwable t) {
                _log.log(Level.SEVERE, "deleteMe()", t);
            } // returns pet to control item
        }
        if (getPet() != null) {
            try {
                getPet().unSummon(this);
            } catch (Throwable t) {
                _log.log(Level.SEVERE, "deleteMe()", t);
            } // returns pet to control item
        }
        if (_partner != null) {
            try {
                _partner.despawnMe();
            } catch (Throwable t) {
                _log.log(Level.SEVERE, "deleteMe()", t);
            } // returns pet to control item
        }
        try {
            if (_clanId > 0 && getClan() != null) {
                getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);

                L2ClanMember clanMember = getClan().getClanMember(getName());
                if (clanMember != null) {
                    clanMember.setPlayerInstance(null);
                }
            }
        } catch (final Throwable t) {
            _log.log(Level.SEVERE, "deletedMe()", t);
        }

        // If the L2PcInstance is a GM, remove it from the GM List
        if (isGM()) {
            try {
                GmListTable.getInstance().deleteGm(this);
            } catch (Throwable t) {
                _log.log(Level.SEVERE, "deleteMe()", t);
            }
        }

        // Update database with items in its inventory and remove them from the world
        try {
            getInventory().deleteMe();
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        // Update database with items in its warehouse and remove them from the world
        try {
            clearWarehouse();
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        if (Config.WAREHOUSE_CACHE) {
            WarehouseCacheManager.getInstance().remCacheTask(this);
        }

        // Update database with items in its freight and remove them from the world
        if (_freight != null) {
            try {
                _freight.deleteMe();
            } catch (Throwable t) {
                _log.log(Level.SEVERE, "deleteMe()", t);
            }
        }

        // Remove all L2Object from _knownObjects and _knownPlayer of the L2Character
        // then cancel Attak or Cast and notify AI
        try {
            getKnownList().removeAllKnownObjects();
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        // Close the connection with the client
        closeNetConnection();

        /*
         * for (L2PcInstance player : _snoopedPlayer) {
         * player.removeSnooper(this); }
         *
         * for (L2PcInstance player : _snoopListener) {
         * player.removeSnooped(this); }
         */
        if (_partyRoom != null) {
            PartyWaitingRoomManager.getInstance().exitRoom(this, _partyRoom);
        }

        // Remove L2Object object from _allObjects of L2World
        L2World.getInstance().removePlayer(this);
    }

    public void gc() {
        // _subClasses = null;
        _dwarvenRecipeBook = null;
        _commonRecipeBook = null;
        _recomChars = null;
        _warehouse = null;
        _freight = null;
        _activeTradeList = null;
        _activeWarehouse = null;
        _createList = null;
        _sellList = null;
        _buyList = null;
        _lastFolkNpc = null;
        _quests = null;
        _shortCuts = null;
        _macroses = null;
        _snoopListener = null;
        _snoopedPlayer = null;
        _skillLearningClassId = null;
        _summon = null;
        _tamedBeast = null;
        _clan = null;
        _currentSkillWorldPosition = null;
        _BanChatTask = null;
        _party = null;
        _arrowItem = null;
        _currentTransactionRequester = null;
        _fistsWeaponItem = null;
        _activeEnchantItem = null;
        _activeSoulShots = null;
        kills = null;
        _fishCombat = null;
        _taskRentPet = null;
        _taskWater = null;
        // _validBypass = null;
        // _validBypass2 = null;
        _forumMail = null;
        _forumMemo = null;
        _currentSkill = null;
        _queuedSkill = null;
        _forceBuff = null;
        _bbsMailSender = null;
        _bbsMailTheme = null;
        _euipWeapon = null;
        _userKey = null;
        voteAugm = null;
        _friends = null;
        _lastOptiClientPosition = null;
        _lastOptiServerPosition = null;
        _profiles = null;
        _cubics = null;
        clearBypass();
    }

    private FishData _fish;

    /*
     * startFishing() was stripped of any pre-fishing related checks, namely the
     * fishing zone check. Also worthy of note is the fact the code to find the
     * hook landing position was also striped. The stripped code was moved into
     * fishing.java. In my opinion it makes more sense for it to be there since
     * all other skill related checks were also there. Last but not least,
     * moving the zone check there, fixed a bug where baits would always be
     * consumed no matter if fishing actualy took place. startFishing() now
     * takes up 3 arguments, wich are acurately described as being the hook
     * landing coordinates.
     */
    public void startFishing(int _x, int _y, int _z) {
        stopMove(null);
        setIsImobilised(true);
        _fishing = true;
        _fishx = _x;
        _fishy = _y;
        _fishz = _z;
        broadcastUserInfo();
        // Starts fishing
        int lvl = getRandomFishLvl();
        int group = getRandomFishGroup();
        int type = getRandomFishType(group);
        List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
        if (fishs == null || fishs.size() == 0) {
            sendMessage("Error - Fishes are not definied");
            EndFishing(false);
            return;
        }
        int check = Rnd.get(fishs.size());
        // Use a copy constructor else the fish data may be over-written below
        _fish = new FishData(fishs.get(check));
        fishs.clear();
        fishs = null;
        sendUserPacket(Static.CAST_LINE_AND_START_FISHING);
        ExFishingStart efs = null;
        if (!GameTimeController.getInstance().isNowNight() && _lure.isNightLure()) {
            _fish.setType(-1);
        }
        // sendMessage("Hook x,y: " + _x + "," + _y + " - Water Z, Player Z:" + _z + ",
        // " + getZ()); //debug line, uncoment to show coordinates used in fishing.
        efs = new ExFishingStart(this, _fish.getType(), _x, _y, _z, _lure.isNightLure());
        broadcastPacket(efs);
        startLookingForFishTask();
    }

    public void stopLookingForFishTask() {
        if (_taskforfish != null) {
            _taskforfish.cancel(false);
            _taskforfish = null;
        }
    }

    public void startLookingForFishTask() {
        if (!isDead() && _taskforfish == null) {
            int checkDelay = 0;
            boolean isNoob = false;
            boolean isUpperGrade = false;

            if (_lure != null) {
                int lureid = _lure.getItemId();
                isNoob = _fish.getGroup() == 0;
                isUpperGrade = _fish.getGroup() == 2;
                if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508
                        || lureid == 8511) // low grade
                {
                    checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.33)));
                } else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || (lureid >= 8505 && lureid <= 8513)
                        || (lureid >= 7610 && lureid <= 7613) || (lureid >= 7807 && lureid <= 7809)
                        || (lureid >= 8484 && lureid <= 8486)) // medium grade, beginner, prize-winning & quest special
                                                               // bait
                {
                    checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.00)));
                } else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510
                        || lureid == 8513) // high grade
                {
                    checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (0.66)));
                }
            }
            _taskforfish = EffectTaskManager.getInstance()
                    .scheduleAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(),
                            _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
        }
    }

    private int getRandomFishGroup() {
        switch (_lure.getItemId()) {
            case 7807: // green for beginners
            case 7808: // purple for beginners
            case 7809: // yellow for beginners
            case 8486: // prize-winning for beginners
                return 0;
            case 8485: // prize-winning luminous
            case 8506: // green luminous
            case 8509: // purple luminous
            case 8512: // yellow luminous
                return 2;
            default:
                return 1;
        }
    }

    private int getRandomFishType(int group) {
        int check = Rnd.get(100);
        int type = 1;
        switch (group) {
            case 0: // fish for novices
                switch (_lure.getItemId()) {
                    case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
                        if (check <= 54) {
                            type = 5;
                        } else if (check <= 77) {
                            type = 4;
                        } else {
                            type = 6;
                        }
                        break;
                    case 7808: // purple lure, preferred by fat fish (type 4)
                        if (check <= 54) {
                            type = 4;
                        } else if (check <= 77) {
                            type = 6;
                        } else {
                            type = 5;
                        }
                        break;
                    case 7809: // yellow lure, preferred by ugly fish (type 6)
                        if (check <= 54) {
                            type = 6;
                        } else if (check <= 77) {
                            type = 5;
                        } else {
                            type = 4;
                        }
                        break;
                    case 8486: // prize-winning fishing lure for beginners
                        if (check <= 33) {
                            type = 4;
                        } else if (check <= 66) {
                            type = 5;
                        } else {
                            type = 6;
                        }
                        break;
                }
                break;
            case 1: // normal fish
                switch (_lure.getItemId()) {
                    case 7610:
                    case 7611:
                    case 7612:
                    case 7613:
                        type = 3;
                        break;
                    case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
                    case 8505:
                    case 6520:
                    case 6521:
                    case 8507:
                        if (check <= 54) {
                            type = 1;
                        } else if (check <= 74) {
                            type = 0;
                        } else if (check <= 94) {
                            type = 2;
                        } else {
                            type = 3;
                        }
                        break;
                    case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
                    case 8508:
                    case 6523:
                    case 6524:
                    case 8510:
                        if (check <= 54) {
                            type = 0;
                        } else if (check <= 74) {
                            type = 1;
                        } else if (check <= 94) {
                            type = 2;
                        } else {
                            type = 3;
                        }
                        break;
                    case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
                    case 8511:
                    case 6526:
                    case 6527:
                    case 8513:
                        if (check <= 55) {
                            type = 2;
                        } else if (check <= 74) {
                            type = 1;
                        } else if (check <= 94) {
                            type = 0;
                        } else {
                            type = 3;
                        }
                        break;
                    case 8484: // prize-winning fishing lure
                        if (check <= 33) {
                            type = 0;
                        } else if (check <= 66) {
                            type = 1;
                        } else {
                            type = 2;
                        }
                        break;
                }
                break;
            case 2: // upper grade fish, luminous lure
                switch (_lure.getItemId()) {
                    case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
                        if (check <= 54) {
                            type = 8;
                        } else if (check <= 77) {
                            type = 7;
                        } else {
                            type = 9;
                        }
                        break;
                    case 8509: // purple lure, preferred by fat fish (type 7)
                        if (check <= 54) {
                            type = 7;
                        } else if (check <= 77) {
                            type = 9;
                        } else {
                            type = 8;
                        }
                        break;
                    case 8512: // yellow lure, preferred by ugly fish (type 9)
                        if (check <= 54) {
                            type = 9;
                        } else if (check <= 77) {
                            type = 8;
                        } else {
                            type = 7;
                        }
                        break;
                    case 8485: // prize-winning fishing lure
                        if (check <= 33) {
                            type = 7;
                        } else if (check <= 66) {
                            type = 8;
                        } else {
                            type = 9;
                        }
                        break;
                }
        }
        return type;
    }

    private int getRandomFishLvl() {
        int skilllvl = getSkillLevel(1315);
        FastTable<L2Effect> effects = getAllEffectsTable();
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect effect = effects.get(i);
            if (effect == null) {
                continue;
            }
            if (effect.getSkill().getId() == 2274) {
                skilllvl = (int) effect.getSkill().getPower(this);
            }
        }
        if (skilllvl <= 0) {
            return 1;
        }

        int randomlvl;
        int check = Rnd.get(100);

        if (check <= 50) {
            randomlvl = skilllvl;
        } else if (check <= 85) {
            randomlvl = skilllvl - 1;
            if (randomlvl <= 0) {
                randomlvl = 1;
            }
        } else {
            randomlvl = skilllvl + 1;
            if (randomlvl > 27) {
                randomlvl = 27;
            }
        }
        return randomlvl;
    }

    public void StartFishCombat(boolean a, boolean b) {
        _fishCombat = new L2Fishing(this, _fish, a, b);
    }

    public void EndFishing(boolean f) {
        ExFishingEnd efe = new ExFishingEnd(f, this);
        broadcastPacket(efe);
        _fishing = false;
        _fishx = 0;
        _fishy = 0;
        _fishz = 0;
        broadcastUserInfo();
        if (_fishCombat == null) {
            sendUserPacket(Static.BAIT_LOST_FISH_GOT_AWAY);
        }
        _fishCombat = null;
        _lure = null;
        // Ends fishing
        sendUserPacket(Static.REEL_LINE_AND_STOP_FISHING);
        setIsImobilised(false);
        stopLookingForFishTask();
    }

    public L2Fishing GetFishCombat() {
        return _fishCombat;
    }

    public int GetFishx() {
        return _fishx;
    }

    public int GetFishy() {
        return _fishy;
    }

    public int GetFishz() {
        return _fishz;
    }

    public void SetLure(L2ItemInstance lure) {
        _lure = lure;
    }

    public L2ItemInstance GetLure() {
        return _lure;
    }

    public int getInventoryLimit() {
        int ivlim;
        if (isGM()) {
            ivlim = Config.INVENTORY_MAXIMUM_GM;
        } else if (getRace() == Race.dwarf) {
            ivlim = Config.INVENTORY_MAXIMUM_DWARF;
        } else {
            ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
        }
        ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);

        return ivlim;
    }

    public int getWareHouseLimit() {
        int whlim;
        if (getRace() == Race.dwarf) {
            whlim = Config.WAREHOUSE_SLOTS_DWARF;
        } else {
            whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
        }
        whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);

        return whlim;
    }

    public int getPrivateSellStoreLimit() {
        int pslim;
        if (getRace() == Race.dwarf) {
            pslim = Config.MAX_PVTSTORE_SLOTS_DWARF;
        } else {
            pslim = Config.MAX_PVTSTORE_SLOTS_OTHER;
        }
        pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);

        return pslim;
    }

    public int getPrivateBuyStoreLimit() {
        int pblim;
        if (getRace() == Race.dwarf) {
            pblim = Config.MAX_PVTSTORE_SLOTS_DWARF;
        } else {
            pblim = Config.MAX_PVTSTORE_SLOTS_OTHER;
        }
        pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);

        return pblim;
    }

    public int getFreightLimit() {
        return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
    }

    public int getDwarfRecipeLimit() {
        int recdlim = Config.DWARF_RECIPE_LIMIT;
        recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
        return recdlim;
    }

    public int getCommonRecipeLimit() {
        int recclim = Config.COMMON_RECIPE_LIMIT;
        recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
        return recclim;
    }

    public void setMountObjectID(int newID) {
        _mountObjectID = newID;
    }

    public int getMountObjectID() {
        return _mountObjectID;
    }

    private L2ItemInstance _lure = null;

    /**
     * Get the current skill in use or return null.<BR>
     * <BR>
     *
     */
    public SkillDat getCurrentSkill() {
        return _currentSkill;
    }

    /**
     * Create a new SkillDat object and set the player _currentSkill.<BR>
     * <BR>
     *
     */
    @Override
    public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed) {
        if (currentSkill == null) {
            _currentSkill = null;
            return;
        }

        _currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
    }

    @Override
    public SkillDat getQueuedSkill() {
        return _queuedSkill;
    }

    /**
     * Create a new SkillDat object and queue it in the player
     * _queuedSkill.<BR>
     * <BR>
     *
     */
    @Override
    public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed) {
        if (queuedSkill == null) {
            // if (Config.DEBUG)
            // _log.info("Setting queued skill: NULL for " + getName() + ".");

            _queuedSkill = null;
            return;
        }

        // if (Config.DEBUG)
        // _log.info("Setting queued skill: " + queuedSkill.getName() + " (ID: " +
        // queuedSkill.getId() + ") for " + getName() + ".");
        _queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
    }

    @Override
    public boolean isInJail() {
        return _inJail;
    }

    public void setInJail(boolean f) {
        _inJail = f;
    }

    public void setInJail(boolean f, int delayInMinutes) {
        _inJail = f;
        _jailTimer = 0;
        // Remove the task if any
        stopJailTask(false);

        if (_inJail) {
            if (delayInMinutes > 0) {
                _jailTimer = delayInMinutes * 60000L; // in millisec

                // start the countdown
                _jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
                sendMessage("Вас посадили в тюрьму на " + delayInMinutes + " минут.");
            }

            if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(getName())) {
                TvTEvent.removeParticipant(getName());
            }

            // Open a Html message to inform the player
            NpcHtmlMessage htmlMsg = NpcHtmlMessage.id(5);
            TextBuilder build = new TextBuilder("<html><body>");
            build.append("<html><body>Право заниматься самообразованием.<br>");
            build.append("Право трудиться.<br>");
            build.append("Право на медицинское обеспечение.<br>");
            build.append("Право отправлять религиозные обряды.<br>");
            build.append("Право обращаться с предложениями, заявлениями и жалобами.<br>");
            build.append("Право на получение бесплатного питания.<br>");
            build.append("Право на прогулку.<br>");
            build.append("</body></html>");
            htmlMsg.setHtml(build.toString());
            sendUserPacket(htmlMsg);

            teleToJail(); // Jail
        } else {
            // Open a Html message to inform the player
            NpcHtmlMessage htmlMsg = NpcHtmlMessage.id(0);
            htmlMsg.setHtml("<html><body>Ведите себя хорошо!</body></html>");
            sendUserPacket(htmlMsg);

            teleToLocation(17836, 170178, -3507, true); // Floran
        }

        // store in database
        storeCharBase();
    }

    public long getJailTimer() {
        return _jailTimer;
    }

    public void setJailTimer(long time) {
        _jailTimer = time;
    }

    private void updateJailState() {
        if (isInJail()) {
            // If jail time is elapsed, free the player
            if (_jailTimer > 0) {
                // restart the countdown
                _jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
                sendMessage("You are still in jail for " + Math.round(_jailTimer / 60000) + " minutes.");
            }

            // If player escaped, put him back in jail
            if (!isInsideZone(ZONE_JAIL)) {
                teleToLocation(-114356, -249645, -2984, true);
            }
        }
    }

    public void stopJailTask(boolean f) {
        if (_jailTask != null) {
            if (f) {
                long delay = _jailTask.getDelay(TimeUnit.MILLISECONDS);
                if (delay < 0) {
                    delay = 0;
                }
                setJailTimer(delay);
            }
            _jailTask.cancel(false);
            _jailTask = null;

        }
    }

    private static class JailTask implements Runnable {

        L2PcInstance _player;
        protected long _startedAt;

        protected JailTask(L2PcInstance player) {
            _player = player;
            _startedAt = System.currentTimeMillis();
        }

        @Override
        public void run() {
            _player.setInJail(false, 0);
        }
    }

    /**
     * @return
     */
    public int getPowerGrade() {
        return _powerGrade;
    }

    /**
     * @return
     */
    public void setPowerGrade(int power) {
        _powerGrade = power;
    }

    @Override
    public boolean isCursedWeaponEquiped() {
        return _cursedWeaponEquipedId != 0;
    }

    public void setCursedWeaponEquipedId(int value) {
        _cursedWeaponEquipedId = value;
    }

    public int getCursedWeaponEquipedId() {
        return _cursedWeaponEquipedId;
    }

    private boolean _charmOfCourage = false;

    public boolean getCharmOfCourage() {
        return _charmOfCourage;
    }

    public void setCharmOfCourage(boolean f) {
        _charmOfCourage = f;
        sendEtcStatusUpdate();
    }

    public int getDeathPenaltyBuffLevel() {
        return _deathPenaltyBuffLevel;
    }

    public void setDeathPenaltyBuffLevel(int level) {
        _deathPenaltyBuffLevel = level;
    }

    public void calculateDeathPenaltyBuffLevel(L2Character killer) {
        if (killer.isPlayer() || killer.isL2Summon()) {
            return;
        }

        if (killer.isRaid() && getCharmOfLuck()) {
            return;
        }

        if (Rnd.get(100) > Config.DEATH_PENALTY_CHANCE) {
            return;
        }

        increaseDeathPenaltyBuffLevel();
    }

    public void increaseDeathPenaltyBuffLevel() {
        if (getDeathPenaltyBuffLevel() >= 15) // maximum level reached
        {
            return;
        }

        if (getDeathPenaltyBuffLevel() != 0) {
            L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

            if (skill != null) {
                removeSkill(skill, true);
            }
        }

        _deathPenaltyBuffLevel++;

        addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
        sendEtcStatusUpdate();
        sendUserPacket(
                SystemMessage.id(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(getDeathPenaltyBuffLevel()));
    }

    public void reduceDeathPenaltyBuffLevel() {
        if (getDeathPenaltyBuffLevel() <= 0) {
            return;
        }

        L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

        if (skill != null) {
            removeSkill(skill, true);
        }

        _deathPenaltyBuffLevel--;

        if (getDeathPenaltyBuffLevel() > 0) {
            addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
            sendUserPacket(SystemMessage.id(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED)
                    .addNumber(getDeathPenaltyBuffLevel()));
        } else {
            sendUserPacket(Static.DEATH_PENALTY_LIFTED);
        }

        sendEtcStatusUpdate();
    }

    public void restoreDeathPenaltyBuffLevel() {
        L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

        if (skill != null) {
            removeSkill(skill, true);
        }

        if (getDeathPenaltyBuffLevel() > 0) {
            addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
            // SystemMessage sm =
            // SystemMessage.id(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
            // sm.addNumber(getDeathPenaltyBuffLevel());
            // sendPacket(sm);
        }
        // sendEtcStatusUpdate();
    }

    @Override
    public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
        if (miss) {
            sendUserPacket(Static.MISSED_TARGET);
            return;
        }

        if (isInOlympiadMode() && (target.isPlayer() || target.isL2Summon())) {
            L2PcInstance enemy = target.getPlayer();
            if (enemy == null) {
                return;
            }

            if (getOlympiadGameId() != enemy.getOlympiadGameId()) {
                return;
            }

            OlympiadGame olymp_game = Olympiad.getOlympiadGame(getOlympiadGameId());
            if (olymp_game != null) {
                if (olymp_game.getState() <= 0) {
                    return;
                }

                if (!this.equals(enemy)) {
                    olymp_game.addDamage(enemy, Math.min((getCurrentHp() + getCurrentCp()), damage));
                }
            }
        }

        // Check if hit is critical
        if (pcrit) {
            sendUserPacket(Static.CRITICAL_HIT);
        }
        if (mcrit) {
            sendUserPacket(Static.CRITICAL_HIT_MAGIC);
        }

        /*
         * if (target.getPet() != null && target.getFirstEffect(1262) != null) {
         * int tDmg = (int)damage *
         * (int)target.getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0,
         * null, null) / 100; sendMessage("Вы нанесли " + (damage-tDmg) + "
         * повреждений вашей цели и " + tDmg + " повреждений слуге"); } else if
         * (damage > 0)
         * sendUserPacket(SystemMessage.id(SystemMessageId.YOU_DID_S1_DMG).addNumber(
         * damage));
         */
        if (damage > 0) {
            if (!target.isPlayer()) {
                sendUserPacket(SystemMessage.id(SystemMessageId.YOU_DID_S1_DMG).addNumber(damage));
            }

            if (_isPartner) {
                _owner.sendMessage("Партнер наносит " + damage + " урона по " + target.getName());
            }
        }
    }

    /*
     * checkBanChat - checks is user's chat banned or not
     *
     * boolean notEnterWorld - shows that checkup called not from EnterWorld
     * packet, if we'll not use it user will see "Your chat ban has been
     * lifted." on every login into game :)
     */
    public void checkBanChat(boolean f) {
        long banLength = 0;
        String banReason = "";

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT chatban_timer FROM characters WHERE `obj_Id`=? LIMIT 1");
            st.setInt(1, getObjectId());
            rset = st.executeQuery();
            if (rset.next()) {
                banLength = rset.getLong("chatban_timer");
                // banReason = rset.getString("chatban_reason");
            }
        } catch (Exception e) {
            _log.warning("Could not select chat ban info:" + e);
        } finally {
            Close.CSR(con, st, rset);
        }

        Calendar serv_time = Calendar.getInstance();
        long nowTime = serv_time.getTimeInMillis();
        banLength = (banLength - nowTime) / 1000;

        if (banLength > 0) {
            _chatBanned = true;
            setChatBanned(true, banLength, banReason);
        } else if (_chatBanned && f) {
            _chatBanned = false;
            setChatBanned(false, 0, "");
        }
    }

    /*
     * setChatBanned - used for setting up chat ban status
     *
     * isBanned - shows chat ban status (true, false) banLength - chat ban time
     * in seconds banReason - reason of chat ban (if needed)
     */
    public void setChatBanned(boolean f, long banLength, String banReason) {
        _chatBanned = f;
        if (f) {
            long banLengthMs = TimeUnit.SECONDS.toMillis(banLength);
            ThreadPoolManager.getInstance().scheduleGeneral(new SchedChatUnban(this), banLengthMs);

            sendMessage("Чат заблокирован на (" + (banLength / 60) + ") минут");
            banLength = System.currentTimeMillis() + banLengthMs;
        } else {
            banLength = 0;
            sendUserPacket(Static.CHAT_UNBLOCKED);
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE `characters` SET `chatban_timer`=? WHERE `obj_Id`=?");
            st.setLong(1, banLength);
            st.setInt(2, getObjectId());
            st.execute();
        } catch (Exception e) {
            _log.warning("Could not save chat ban info:" + e);
        } finally {
            Close.CS(con, st);
        }
    }

    @Override
    public ForceBuff getForceBuff() {
        return _forceBuff;
    }

    public void setForceBuff(ForceBuff fb) {
        _forceBuff = fb;
    }

    /**
     * Return true if last request is expired.
     *
     * @return
     */
    public boolean isRequestExpired() {
        return !(_requestExpireTime > GameTimeController.getGameTicks());
    }

    /**
     * Зоны *
     */
    public final boolean isInDangerArea() {
        return _isInDangerArea;
    }

    public final void setInDangerArea(boolean f) {
        _isInDangerArea = f;
        sendEtcStatusUpdate();
    }

    public final boolean isInSiegeFlagArea() {
        return _isInSiegeFlagArea;
    }

    @Override
    public void setInSiegeFlagArea(boolean f) {
        _isInSiegeFlagArea = f;
    }

    public final boolean isInSiegeRuleArea() {
        return _isInSiegeRuleArea;
    }

    @Override
    public void setInSiegeRuleArea(boolean f) {
        _isInSiegeRuleArea = f;
    }

    public final boolean isInOlumpiadStadium() {
        return _isInOlumpiadStadium;
    }

    public final void setInOlumpiadStadium(boolean f) {
        _isInOlumpiadStadium = f;
    }

    @Override
    public void setInCastleWaitZone(boolean f) {
        _isInsideCastleWaitZone = f;
    }

    public final boolean isInsideCastleZone() {
        return _isInsideCastleZone;
    }

    @Override
    public void setInCastleZone(boolean f) {
        _isInsideCastleZone = f;
    }

    public final boolean isInsideHotZone() {
        if (isInPVPArena()) {
            return true;
        }

        return _isInsideHotZone;
    }

    @Override
    public void setInHotZone(boolean f) {
        _isInsideHotZone = f;
    }

    public final boolean isInsideDismountZone() {
        return _isInsideDismountZone;
    }

    public final void setInDismountZone(boolean f) {
        _isInsideDismountZone = f;
    }

    public final boolean isInColiseum() {
        return _isInColiseumZone;
    }

    @Override
    public final void setInColiseum(boolean f) {
        _isInColiseumZone = f;
    }

    public final boolean isInElfTree() {
        return _isInMotherElfZone;
    }

    public final void setInElfTree(boolean f) {
        _isInMotherElfZone = f;
    }

    public final boolean isInBlockZone() {
        return _isInBlockZone;
    }

    public final void setInBlockZone(boolean f) {
        _isInBlockZone = f;
    }

    @Override
    public boolean isInsideAqZone() {
        return _isInsideAqZone;
    }

    @Override
    public final void setInAqZone(boolean f) {
        _isInsideAqZone = f;
    }

    @Override
    public boolean isInsideSilenceZone() {
        return _isInsideSilenceZone;
    }

    @Override
    public final void setInsideSilenceZone(boolean f) {
        _isInsideSilenceZone = f;
    }

    public final boolean isInZakenZone() {
        return _isInZakenZone;
    }

    public final void setInZakenZone(boolean f) {
        _isInZakenZone = f;
    }

    private boolean _pvpArena;

    public final boolean isInPVPArena() {
        return _pvpArena;
    }

    @Override
    public void setPVPArena(boolean f) {
        // if (_pvpArena == f)
        // return;
        _pvpArena = f;
        super.setInsideZone(L2Character.ZONE_PVP, f);
    }

    @Override
    public boolean isInsidePvpZone() {
        if (_pvpArena || _freeArena || ZoneManager.getInstance().inPvpZone(this)) {
            return true;
        }

        return super.isInsidePvpZone();
    }

    private boolean _dinoIsle;

    public final boolean isInDino() {
        return _dinoIsle;
    }

    @Override
    public void setInDino(boolean f) {
        _dinoIsle = f;
    }

    /**
     * hfpyjt *
     */
    private String convertColor(String color) {
        switch (color.length()) {
            case 1:
                color = "00000" + color;
                break;
            case 2:
                color = "0000" + color;
                break;
            case 3:
                color = "000" + color;
                break;
            case 4:
                color = "00" + color;
                break;
            case 5:
                color = "0" + color;
                break;
        }
        return color;
    }

    public void checkAllowedSkills() {
        if (getLevel() == 19 || getLevel() == 39) {
            return;
        }

        if (isGM()) {
            return;
        }

        if (!(Config.CHECK_SKILLS)) {
            return;
        }

        if (Config.PREMIUM_ENABLE && isPremium() && !(Config.PREMIUM_CHKSKILLS)) {
            return;
        }

        Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(getClassId());
        // loop through all skills of player
        outer: for (L2Skill skill : getAllSkills()) {
            if (skill == null) {
                continue;
            }

            // exclude fishing skills and common skills + dwarfen craft
            if (skill.isFishingSkill()) {
                continue;
            }

            // Expand Dwarven Craft
            if (skill.isDwarvenSkill()) {
                continue;
            }

            // exclude sa / enchant bonus / penality etc. skills
            if (skill.isMiscSkill()) {
                continue;
            }

            // exclude noble skills
            if (isNoble() && skill.isNobleSkill()) {
                continue;
            }

            // exclude hero skills
            if (isHero() && skill.isHerosSkill()) {
                continue;
            }

            // exclude clan skills
            if (getClan() != null) {
                if (skill.isClanSkill()) {
                    continue;
                }

                // exclude seal of ruler / build siege hq
                if (isClanLeader() && skill.isSiegeSkill()) {
                    continue;
                }
            }

            int skillid = skill.getId();
            // exclude cursed weapon skills
            if (isCursedWeaponEquiped() && skillid == CursedWeaponsManager.getInstance()
                    .getCursedWeapon(_cursedWeaponEquipedId).getSkillId()) {
                continue;
            }

            if (skillid == 3603 && getKarma() >= 900000) {
                continue;
            }

            // loop through all skills in players skilltree
            for (L2SkillLearn temp : skillTree) {
                if (temp == null) {
                    continue;
                }
                // if the skill was found and the level is possible to obtain for his class
                // everything is ok
                if (temp.getId() == skillid) {
                    continue outer;
                }
            }

            // remove skill and do a lil log message
            removeSkill(skill);
        }
    }

    @Override
    public void checkHpMessages(double curHp, double newHp) {
        // сюда пасивные скиллы
        byte[] _hp = { 30, 30 };
        int[] skills = { 290, 291 };

        // сюда активные эффекты
        int[] _effects_skills_id = { 292, 292 };
        byte[] _effects_hp = { 30, 60 };

        double percent = getMaxHp() / (double) 100;
        double _curHpPercent = curHp / percent;
        double _newHpPercent = newHp / percent;
        boolean needsUpdate = false;

        L2Skill skill = null;
        SkillTable st = SkillTable.getInstance();
        // check for passive skills
        for (int i = 0; i < skills.length; i++) {
            int level = getSkillLevel(skills[i]);
            skill = st.getInfo(skills[i], 1);
            if (level > 0) {
                if (_curHpPercent > _hp[i] && _newHpPercent <= _hp[i]) {
                    sendMessage("Так как HP уменьшилось, вы ощущаете эффект от " + skill.getName());
                    needsUpdate = true;
                } else if (_curHpPercent <= _hp[i] && _newHpPercent > _hp[i]) {
                    sendMessage("Так как HP увеличилось, эффект от " + skill.getName() + " пропадает");
                    needsUpdate = true;
                }
            }
        }

        // check for active effects
        for (int i = 0; i < _effects_skills_id.length; i++) {
            try {
                if (getFirstEffect(_effects_skills_id[i]) != null) {
                    skill = st.getInfo(_effects_skills_id[i], 1);
                    if (_curHpPercent > _effects_hp[i] && _newHpPercent <= _effects_hp[i]) {
                        sendMessage("Так как HP уменьшилось, вы можете применить " + skill.getName());
                        needsUpdate = true;
                    } else if (_curHpPercent <= _effects_hp[i] && _newHpPercent > _effects_hp[i]) {
                        sendMessage("Так как HP увеличилось, эффект от " + skill.getName() + " пропадает");
                        // stopSkillEffects(_effects_skills_id[i]);
                        needsUpdate = true;
                    }
                }
            } catch (Exception e) {
                //
            }
        }
        if (needsUpdate) {
            sendChanges();
        }
    }

    public void checkDayNightMessages() {
        if (getSkillLevel(294) == -1) {
            return;
        }

        if (GameTimeController.getInstance().isNowNight()) {
            sendUserPacket(Static.SHADOW_SENSE_ON);
        } else {
            sendUserPacket(Static.SHADOW_SENSE_OFF);
        }

        sendChanges();
    }

    @Override
    public void refreshSavedStats() {
        _statsChangeRecorder.refreshSaves();
    }

    @Override
    public void sendChanges() {
        _statsChangeRecorder.sendChanges();
    }

    public float getColRadius() {
        L2Summon pet = getPet();
        if (isMounted() && pet != null) {
            return pet.getTemplate().collisionRadius;
        } else {
            return getBaseTemplate().collisionRadius;
        }
    }

    public float getColHeight() {
        L2Summon pet = getPet();
        if (isMounted() && pet != null) {
            return pet.getTemplate().collisionHeight;
        } else {
            return getBaseTemplate().collisionHeight;
        }
    }

    public void updateStats() {
        refreshOverloaded();
        refreshExpertisePenalty();
        sendChanges();
    }

    /**
     * профили баффа
     */
    private FastMap<Integer, FastMap<Integer, Integer>> _profiles = new FastMap<Integer, FastMap<Integer, Integer>>()
            .shared("L2PcInstance._profiles");
    private long _lastBuffProfile = 0;

    public void restoreProfileBuffs(Connect con) {
        // Connect con = null;
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            // con = L2DatabaseFactory.get();
            // con.setTransactionIsolation(1);

            st = con.prepareStatement(
                    "SELECT profile, buffs FROM `z_buff_profile` WHERE `char_id` = ? ORDER BY `profile`");
            st.setInt(1, getObjectId());
            rset = st.executeQuery();
            while (rset.next()) {
                int profile = rset.getInt("profile");
                _profiles.put(profile, new FastMap<Integer, Integer>());
                String buffs = rset.getString("buffs");
                String[] token = buffs.split(";");
                for (String bf : token) {
                    if (bf.equals("")) {
                        continue;
                    }

                    String[] buff = bf.split(",");
                    Integer id = Integer.valueOf(buff[0]);
                    Integer lvl = Integer.valueOf(buff[1]);
                    if (id == null || lvl == null) {
                        continue;
                    }

                    L2Skill skill = SkillTable.getInstance().getInfo(id, 1);
                    if (skill == null) {
                        continue;
                    }

                    if (skill.isForbiddenProfileSkill() || skill.getSkillType() != SkillType.BUFF || skill.isChance()
                            || skill.isAugment()) {
                        continue;
                    }

                    _profiles.get(profile).put(id, lvl);
                }
            }
        } catch (SQLException e) {
            _log.warning("Could not store " + getName() + "'s buff profiles: " + e);
        } finally {
            Close.SR(st, rset);
        }
    }

    public void doBuffProfile(int buffprofile) {
        if (System.currentTimeMillis() - _lastBuffProfile < 5000) {
            sNotReady();
            return;
        }
        _lastBuffProfile = System.currentTimeMillis();

        FastMap<Integer, Integer> profile = _profiles.get(buffprofile);
        if (profile == null || profile.isEmpty()) {
            sendUserPacket(Static.OOPS_ERROR);
            return;
        }

        if (Config.BUFF_CANCEL) {
            getBuffTarget().stopAllEffects();
        }

        SkillTable st = SkillTable.getInstance();
        for (FastMap.Entry<Integer, Integer> e = profile.head(), end = profile.tail(); (e = e.getNext()) != end;) {
            Integer id = e.getKey(); // No typecast necessary.
            Integer lvl = e.getValue(); // No typecast necessary.
            if (id == null || lvl == null) {
                continue;
            }

            st.getInfo(id, lvl).getEffects(getBuffTarget(), getBuffTarget());
        }
        // sendMessage("Профиль " + buffprofile + " баффнут");
        broadcastPacket(new MagicSkillUser(getBuffTarget(), getBuffTarget(), 264, 1, 1, 0));
    }

    public void saveBuffProfile(int buffprofile) {
        if (System.currentTimeMillis() - _lastBuffProfile < 5000) {
            sNotReady();
            return;
        }
        _lastBuffProfile = System.currentTimeMillis();

        FastTable<L2Effect> effects = getAllEffectsTable();
        if (effects.isEmpty()) {
            sendUserPacket(Static.OOPS_ERROR);
            return;
        }

        _profiles.remove(buffprofile);
        _profiles.put(buffprofile, new FastMap<Integer, Integer>());
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().isForbiddenProfileSkill() || e.getSkill().getSkillType() != SkillType.BUFF
                    || e.getSkill().isChance() || e.getSkill().isAugment()) {
                continue;
            }

            int id = e.getSkill().getId();
            int level = e.getSkill().getLevel();

            _profiles.get(buffprofile).put(id, level);
        }
        // sendMessage("Профиль " + buffprofile + " сохранен");
        sendUserPacket(Static.PROFILE_SAVED);
    }

    public void storeBuffProfiles() {
        if (_profiles == null || _profiles.isEmpty()) {
            return;
        }

        TextBuilder pf = new TextBuilder();
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            con.setAutoCommit(false);
            st = con.prepareStatement("REPLACE INTO z_buff_profile (char_id,profile,buffs) VALUES (?,?,?)");
            for (FastMap.Entry<Integer, FastMap<Integer, Integer>> e = _profiles.head(),
                    end = _profiles.tail(); (e = e.getNext()) != end;) {
                Integer profile = e.getKey(); // No typecast necessary.
                FastMap<Integer, Integer> data = e.getValue(); // No typecast necessary.
                if (profile == null || data == null) {
                    continue;
                }

                if (data.isEmpty()) {
                    continue;
                }
                pf.clear();

                for (FastMap.Entry<Integer, Integer> j = data.head(), endj = data.tail(); (j = j.getNext()) != endj;) {
                    Integer id = j.getKey(); // No typecast necessary.
                    Integer lvl = j.getValue(); // No typecast necessary.
                    if (id == null || lvl == null) {
                        continue;
                    }

                    pf.append(id + "," + lvl + ";");
                }
                st.setInt(1, getObjectId());
                st.setInt(2, profile);
                st.setString(3, pf.toString());
                st.addBatch();
            }
            st.executeBatch();
            con.commit();
        } catch (SQLException h) {
            _log.warning("Could not store " + getName() + "'s buff profile" + h);
        } finally {
            pf.clear();
            Close.CS(con, st);
        }
    }

    // откта скиллов
    private long _lastReload = 0;

    public void reloadSkills() {
        reloadSkills(true);
    }

    public void reloadSkills(boolean self) {
        if (self) {
            if (System.currentTimeMillis() - _lastReload < 5000) {
                sNotReady();
                return;
            }
            _lastReload = System.currentTimeMillis();
        }

        clearDisabledSkills();

        sendSkillCoolTime();
        sendSkillList();

        // sendMessage("Откат скиллов");
        sendUserPacket(Static.SKILLS_RELOAD);
    }

    public boolean underAttack() {
        return (AttackStanceTaskManager.getInstance().getAttackStanceTask(this));
    }

    // задержка на флуд атакой
    private long _lastPacket = 0;

    public long getLastPacket() {
        return _lastPacket;
    }

    public void setLastPacket() {
        _lastPacket = System.currentTimeMillis();
    }

    // чар выходит из игры
    private boolean _isDeleting = false;

    public boolean isDeleting() {
        return _isDeleting;
    }

    // Бот
    // первая проверка на бота по задержке между выбором чара и входа им в мир
    private long _EnterWorld = 0;

    public long getEnterWorld() {
        return _EnterWorld;
    }

    public void setEnterWorld() {
        _EnterWorld = System.currentTimeMillis();
    }

    // Титул
    private long _requestGiveNickName = 0;
    private int _titleChngedFail = 0;

    public long getRequestGiveNickName() {
        return _requestGiveNickName;
    }

    public void setRequestGiveNickName() {
        _requestGiveNickName = System.currentTimeMillis();
    }

    public long getTitleChngedFail() {
        return _titleChngedFail;
    }

    public void setTitleChngedFail() {
        _titleChngedFail += 1;
    }

    public void clearTitleChngedFail() {
        _titleChngedFail = 0;
    }

    // флуд пакетами
    private long _cpa = 0;
    private long _cpb = 0;
    private long _cpc = 0;
    private long _cpd = 0;
    private long _cpe = 0;
    private long _cpf = 0;
    private long _cpg = 0;
    private long _cph = 0;
    private long _cpj = 0;
    private long _cpk = 0;
    private long _cpl = 0;
    private long _cpm = 0;
    private long _cpn = 0;
    private long _cpo = 0;
    private long _cpp = 0;
    private long _cpq = 0;
    private long _cpr = 0;
    private long _cps = 0;
    private long _cpt = 0;
    private long _cpu = 0;
    private long _cpw = 0;
    private long _cpx = 0;
    private long _cpv = 0;
    private long _cpy = 0;
    private long _cpz = 0;
    private long _cpaa = 0;
    private long _cpab = 0;
    private long _cpac = 0;
    private long _cpad = 0;
    private long _cpae = 0;
    private long _cpaf = 0;
    private long _cpag = 0;
    private long _cpah = 0;
    private long _cpaj = 0;
    private long _cpak = 0;
    private long _cpal = 0;
    private long _cpam = 0;
    private long _cpan = 0;
    private long _cpao = 0;
    private long _cpap = 0;
    private long _cpaq = 0;
    private long _cpar = 0;
    private long _cpas = 0;
    private long _cpat = 0;
    private long _cpau = 0;
    private long _cpav = 0;
    private long _cpaw = 0;
    private long _cpax = 0;
    private long _cpay = 0;
    private long _cpaz = 0;
    private long _cpaaa = 0;
    private long _cpaab = 0;
    private long _cpaac = 0;
    private long _cpaad = 0;
    private long _cpaae = 0;
    private long _cpaaf = 0;
    private long _cpaag = 0;
    private long _cpaah = 0;

    public long getCPA() {
        return _cpaae;
    }

    public void setCPA() {
        _cpaae = System.currentTimeMillis();
    }

    public long getCPB() {
        return _cpaad;
    }

    public void setCPB() {
        _cpaad = System.currentTimeMillis();
    }

    public long getCPC() {
        return _cpa;
    }

    public void setCPC() {
        _cpa = System.currentTimeMillis();
    }

    public long getCPD() {
        return _cpb;
    }

    public void setCPD() {
        _cpb = System.currentTimeMillis();
    }

    public long gCPE() {
        return _cpc;
    }

    public void sCPE() {
        _cpc = System.currentTimeMillis();
    }

    public long gCPF() {
        return _cpd;
    }

    public void sCPF() {
        _cpd = System.currentTimeMillis();
    }

    public long gCPG() {
        return _cpe;
    }

    public void sCPG() {
        _cpe = System.currentTimeMillis();
    }

    public long gCPH() {
        return _cpf;
    }

    public void sCPH() {
        _cpf = System.currentTimeMillis();
    }

    public long gCPJ() {
        return _cpg;
    }

    public void sCPJ() {
        _cpg = System.currentTimeMillis();
    }

    public long gCPK() {
        return _cph;
    }

    public void sCPK() {
        _cph = System.currentTimeMillis();
    }

    public long gCPL() {
        return _cpj;
    }

    public void sCPL() {
        _cpj = System.currentTimeMillis();
    }

    public long gCPM() {
        return _cpk;
    }

    public void sCPM() {
        _cpk = System.currentTimeMillis();
    }

    public long gCPN() {
        return _cpl;
    }

    public void sCPN() {
        _cpl = System.currentTimeMillis();
    }

    public long gCPO() {
        return _cpm;
    }

    public void sCPO() {
        _cpm = System.currentTimeMillis();
    }

    public long gCPP() {
        return _cpn;
    }

    public void sCPP() {
        _cpn = System.currentTimeMillis();
    }

    public long gCPR() {
        return _cpo;
    }

    public void sCPR() {
        _cpo = System.currentTimeMillis();
    }

    public long gCPS() {
        return _cpp;
    }

    public void sCPS() {
        _cpp = System.currentTimeMillis();
    }

    public long gCPT() {
        return _cpq;
    }

    public void sCPT() {
        _cpq = System.currentTimeMillis();
    }

    public long gCPU() {
        return _cpr;
    }

    public void sCPU() {
        _cpr = System.currentTimeMillis();
    }

    public long gCPV() {
        return _cps;
    }

    public void sCPV() {
        _cps = System.currentTimeMillis();
    }

    public long gCPW() {
        return _cpt;
    }

    public void sCPW() {
        _cpt = System.currentTimeMillis();
    }

    public long gCPX() {
        return _cpu;
    }

    public void sCPX() {
        _cpu = System.currentTimeMillis();
    }

    public long gCPY() {
        return _cpw;
    }

    public void sCPY() {
        _cpw = System.currentTimeMillis();
    }

    public long gCPZ() {
        return _cpx;
    }

    public void sCPZ() {
        _cpx = System.currentTimeMillis();
    }

    public long gCPAA() {
        return _cpv;
    }

    public void sCPAA() {
        _cpv = System.currentTimeMillis();
    }

    public long gCPAB() {
        return _cpy;
    }

    public void sCPAB() {
        _cpy = System.currentTimeMillis();
    }

    public long gCPAC() {
        return _cpz;
    }

    public void sCPAC() {
        _cpz = System.currentTimeMillis();
    }

    public long gCPAD() {
        return _cpaa;
    }

    public void sCPAD() {
        _cpaa = System.currentTimeMillis();
    }

    public long gCPAE() {
        return _cpab;
    }

    public void sCPAE() {
        _cpab = System.currentTimeMillis();
    }

    public long gCPAF() {
        return _cpac;
    }

    public void sCPAF() {
        _cpac = System.currentTimeMillis();
    }

    public long gCPAG() {
        return _cpad;
    }

    public void sCPAG() {
        _cpad = System.currentTimeMillis();
    }

    public long gCPAH() {
        return _cpae;
    }

    public void sCPAH() {
        _cpae = System.currentTimeMillis();
    }

    public long gCPAJ() {
        return _cpaf;
    }

    public void sCPAJ() {
        _cpaf = System.currentTimeMillis();
    }

    public long gCPAK() {
        return _cpag;
    }

    public void sCPAK() {
        _cpag = System.currentTimeMillis();
    }

    public long gCPAL() {
        return _cpah;
    }

    public void sCPAL() {
        _cpah = System.currentTimeMillis();
    }

    public long gCPAM() {
        return _cpaj;
    }

    public void sCPAM() {
        _cpaj = System.currentTimeMillis();
    }

    public long gCPAN() {
        return _cpak;
    }

    public void sCPAN() {
        _cpak = System.currentTimeMillis();
    }

    public long gCPAO() {
        return _cpal;
    }

    public void sCPAO() {
        _cpal = System.currentTimeMillis();
    }

    public long gCPAP() {
        return _cpam;
    }

    public void sCPAP() {
        _cpam = System.currentTimeMillis();
    }

    public long gCPAQ() {
        return _cpan;
    }

    public void sCPAQ() {
        _cpan = System.currentTimeMillis();
    }

    public long gCPAR() {
        return _cpao;
    }

    public void sCPAR() {
        _cpao = System.currentTimeMillis();
    }

    public long gCPAS() {
        return _cpap;
    }

    public void sCPAS() {
        _cpap = System.currentTimeMillis();
    }

    public long gCPAT() {
        return _cpaq;
    }

    public void sCPAT() {
        _cpaq = System.currentTimeMillis();
    }

    public long gCPAU() {
        return _cpar;
    }

    public void sCPAU() {
        _cpar = System.currentTimeMillis();
    }

    public long gCPAV() {
        return _cpas;
    }

    public void sCPAV() {
        _cpas = System.currentTimeMillis();
    }

    public long gCPAW() {
        return _cpat;
    }

    public void sCPAW() {
        _cpat = System.currentTimeMillis();
    }

    public long gCPAX() {
        return _cpau;
    }

    public void sCPAX() {
        _cpau = System.currentTimeMillis();
    }

    public long gCPAY() {
        return _cpav;
    }

    public void sCPAY() {
        _cpav = System.currentTimeMillis();
    }

    public long gCPAZ() {
        return _cpaw;
    }

    public void sCPAZ() {
        _cpaw = System.currentTimeMillis();
    }

    public long gCPBA() {
        return _cpax;
    }

    public void sCPBA() {
        _cpax = System.currentTimeMillis();
    }

    public long gCPBB() {
        return _cpay;
    }

    public void sCPBB() {
        _cpay = System.currentTimeMillis();
    }

    public long gCPBC() {
        return _cpaz;
    }

    public void sCPBC() {
        _cpaz = System.currentTimeMillis();
    }

    public long gCPBD() {
        return _cpaaa;
    }

    public void sCPBD() {
        _cpaaa = System.currentTimeMillis();
    }

    public long gCPBE() {
        return _cpaab;
    }

    public void sCPBE() {
        _cpaab = System.currentTimeMillis();
    }

    public long gCPBF() {
        return _cpaac;
    }

    public void sCPBF() {
        _cpaac = System.currentTimeMillis();
    }

    public long gCPBG() {
        return _cpaaf;
    }

    public void sCPBG() {
        _cpaaf = System.currentTimeMillis();
    }

    public long gCPBH() {
        return _cpaag;
    }

    public void sCPBH() {
        _cpaag = System.currentTimeMillis();
    }

    public long gCPBJ() {
        return _cpaah;
    }

    public void sCPBJ() {
        _cpaah = System.currentTimeMillis();
    }

    public void disableMove(int delay) {
        _cpaah = System.currentTimeMillis() + delay;
    }

    // вепон еквип
    private boolean _equiptask = false;

    public void setWaitEquip(boolean f) {
        _equiptask = f;
    }

    public boolean isWaitEquip() {
        return _equiptask;
    }

    // для AI
    public int getItemCount(int itemId) {
        return checkItemCount(getInventory().getItemByItemId(itemId));
    }

    private int checkItemCount(L2ItemInstance coins) {
        if (coins == null) {
            return 0;
        }
        return coins.getCount();
    }

    public boolean hasItemCount(int id, int count) {
        return getItemCount(id) >= count;
    }

    @Override
    public boolean hasItems(FastList<Integer> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        Integer id = null;
        for (FastList.Node<Integer> n = items.head(), end = items.tail(); (n = n.getNext()) != end;) {
            id = n.getValue();
            if (id == null) {
                continue;
            }
            if (getInventory().getItemByItemId(id) != null) {
                id = null;
                return true;
            }
        }
        id = null;
        return false;
    }

    // анти-мировой чат
    private boolean _antiWorldChat = false;

    public void setWorldIgnore(boolean f) {
        _antiWorldChat = f;
    }

    public boolean isWorldIgnore() {
        return _antiWorldChat;
    }

    // модераторам
    private boolean _moder = false;
    private boolean _cmoder = true;

    public boolean isModerator() {
        if (_cmoder) {
            Connect con = null;
            PreparedStatement st = null;
            ResultSet result = null;
            try {
                con = L2DatabaseFactory.get();
                st = con.prepareStatement("SELECT * FROM `z_moderator` WHERE `moder`=? LIMIT 1");
                st.setInt(1, getObjectId());
                result = st.executeQuery();
                if (result.next()) {
                    _moder = true;
                }
            } catch (final Exception e) {
                _log.warning("isModerator() error: " + e);
            } finally {
                Close.CSR(con, st, result);
            }
            _cmoder = false;
        }
        return _moder;
    }

    public String getForumName() {
        return getName();
    }

    public int getModerRank() {
        return 3;
    }

    public void logModerAction(String Moder, String Action) {
        Moderator.getInstance().logWrite(getName(), Action);
    }

    // оптимизация положения чара
    private Location _lastOptiClientPosition;
    private Location _lastOptiServerPosition;

    public void setOptiLastClientPosition(Location position) {
        _lastOptiClientPosition = position;
    }

    public Location getOptiLastClientPosition() {
        return _lastOptiClientPosition;
    }

    public void setOptiLastServerPosition(Location position) {
        _lastOptiServerPosition = position;
    }

    public Location getOptiLastServerPosition() {
        return _lastOptiServerPosition;
    }

    // падение с высоты
    private volatile long _fallingTimestamp = 0;

    public final boolean isFalling(int z) {
        if (isDead() || isFlying() || isInWater()) {
            return false;
        }

        if (System.currentTimeMillis() < _fallingTimestamp) {
            return true;
        }

        final int deltaZ = getZ() - z;
        if (deltaZ <= 400) {
            return false;
        }

        /*
         * final int damage = (int)Formulas.calcFallDam(this, deltaZ); if
         * (damage > 0) { reduceCurrentHp(Math.min(damage, getCurrentHp() - 1),
         * null, false, true, null);
         * sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FALL_DAMAGE_S1).
         * addNumber(damage));
         * }
         */
        giveDamageFall(deltaZ);
        setFalling();
        return false;
    }

    public final void setFalling() {
        _fallingTimestamp = System.currentTimeMillis() + 4000;
    }

    public void giveDamageFall(int damage) {
        int hp = (int) getCurrentHp() - damage;
        if (hp < 1) {
            setCurrentHp(1);
        } else {
            setCurrentHp(hp);
        }
        sendMessage("Вы получили " + damage + " урона при падении с высоты.");
    }

    // френды
    private FastMap<Integer, String> _friends = new FastMap<Integer, String>().shared("L2PcInstance._friends");

    public void storeFriend(int fId, String fName) {
        _friends.put(fId, fName);
    }

    public void deleteFriend(int friendId) {
        _friends.remove(friendId);
    }

    public boolean haveFriend(int friendId) {
        return (_friends.containsKey(friendId));
    }

    public FastMap<Integer, String> getFriends() {
        return _friends;
    }

    // сигнеты
    private Location _groundSkillLoc = null;

    public void setGroundSkillLoc(Location location) {
        _groundSkillLoc = location;
    }

    public Location getGroundSkillLoc() {
        return _groundSkillLoc;
    }

    // чтоб не ходили в снайпах и уд
    public boolean isUltimate() {
        int[] imBuffs = { 313, 110, 368 };

        for (int buff : imBuffs) {
            if (getFirstEffect(buff) != null) {
                return true;
            }
        }

        return false;
    }

    public boolean canPotion() {
        int[] imBuffs = { 2002, 2031, 2032 };

        for (int buff : imBuffs) {
            if (getFirstEffect(buff) != null) {
                return true;
            }
        }

        return false;
    }

    public void clearPotions() {
        int[] imBuffs = { 2002, 2031, 2032 };

        for (int buff : imBuffs) {
            if (getFirstEffect(buff) != null) {
                stopSkillEffects(buff);
            }
        }
    }

    // stop
    public void stopMoving() {
        stopMove(null);
    }

    // откат цп
    private long _cpReuseTimeS = 0;
    private long _cpReuseTimeB = 0;

    public long getCpReuseTime(int itemId) {
        long curTime = System.currentTimeMillis();
        long delay = 0;
        if (itemId == 5592) {
            delay = curTime - _cpReuseTimeB;
        } else {
            delay = curTime - _cpReuseTimeS;
        }
        return delay;
    }

    public void setCpReuseTime(int itemId) {
        long curTime = System.currentTimeMillis();
        if (itemId == 5592) {
            _cpReuseTimeB = curTime;
        } else {
            _cpReuseTimeS = curTime;
        }
    }

    //
    @Override
    public void onForcedAttack(L2PcInstance attacker) {
        /*
         * if(PeaceZone.getInstance().inPeace(attacker, this)) {
         * attacker.sendActionFailed(); return; }
         */
        if (attacker.isInOlympiadMode()) {
            if (!isInOlympiadMode() || attacker.getOlympiadGameId() != getOlympiadGameId() || !isOlympiadCompStart()) {
                attacker.sendActionFailed();
                return;
            }
        }
        super.onForcedAttack(attacker);
    }

    // экономим байты
    @Override
    public void sendActionFailed() {
        sendUserPacket(Static.ActionFailed);
    }

    // заглушка на трейд Alt-H
    private int _tradePartner = -1;
    private long _tradeStart = 0; // также как сессия трейда, у 2-х игроков не могут быть разные сессии

    public int getTradePartner() {
        return _tradePartner;
    }

    public long getTradeStart() {
        return _tradeStart;
    }

    public boolean tradeLeft() {
        if (System.currentTimeMillis() - _tradeStart > 10000) {
            return true;
        }

        return false;
    }

    public boolean setTradePartner(int charId, long start) {
        if ((_tradePartner > 0 && charId > 0) || (_tradeStart > 0 && start > 0)) {
            _tradePartner = -1;
            _tradeStart = 0;
            cancelActiveTrade();
            // System.out.println("????");
            return false;
        }

        // System.out.println("!!!!! 1: " + charId + "2: " + start);
        _tradePartner = charId;
        _tradeStart = start;
        return true;
    }

    // bluff
    private int _destX = 0;
    private int _destY = 0;
    private int _destZ = 0;

    public void setDestination(int x, int y, int z) {
        _destX = x;
        _destY = y;
        _destZ = z;
    }

    public int getXdest() {
        return _destX;
    }

    public int getYdest() {
        return _destY;
    }

    public int getZdest() {
        return _destZ;
    }

    /*
     * L2DonateInstance - переносы и биржа
     */
    private int _vote1Item = 0;
    private int _vote2Item = 0;

    public void setVote1Item(int objId) {
        _vote1Item = objId;
    }

    public int getVote1Item() {
        return _vote1Item;
    }

    public void setVote2Item(int objId) {
        _vote2Item = objId;
    }

    public int getVote2Item() {
        return _vote2Item;
    }

    private int _voteEnch = 0;

    public void setVoteEnchant(int enchantLevel) {
        _voteEnch = enchantLevel;
    }

    public int getVoteEnchant() {
        return _voteEnch;
    }

    private L2Skill voteAugm = null;

    public void setVoteAugment(L2Skill augment) {
        voteAugm = augment;
    }

    public L2Skill getVoteAugment() {
        return voteAugm;
    }

    /*
     * Биржа шмоток
     */
    private int _sellIdStock = 0;
    private int _itemIdStock = 0;
    private int _enchantStock = 0;
    private int _augmentStock = 0;
    private int _auhLeveStock = 0;
    private int _objectIdStockI = 0;
    private int _enchantStockI = 0;

    public void setStockItem(int id, int itemId, int enchant, int augment, int auhLevel) {
        _sellIdStock = id;
        _itemIdStock = itemId;
        _enchantStock = enchant;
        _augmentStock = augment;
        _auhLeveStock = auhLevel;
    }

    public void setStockInventoryItem(int objectId, int enchantLevel) {
        _objectIdStockI = objectId;
        _enchantStockI = enchantLevel;
    }

    public int getSellIdStock() {
        return _sellIdStock;
    }

    public int getItemIdStock() {
        return _itemIdStock;
    }

    public int getEnchantStock() {
        return _enchantStock;
    }

    public int getAugmentStock() {
        return _augmentStock;
    }

    public int getAuhLeveStock() {
        return _auhLeveStock;
    }

    public int getObjectIdStockI() {
        return _objectIdStockI;
    }

    public int getEnchantStockI() {
        return _enchantStockI;
    }

    private int _stockSelf = 0;

    public void setStockSelf(int self) {
        _stockSelf = self;
    }

    public int getStockSelf() {
        return _stockSelf;
    }

    private long _stockTime = 0;

    public void setStockLastAction(long last) {
        _stockTime = last;
    }

    public long getStockLastAction() {
        return _stockTime;
    }

    // продажа лс
    private int _augSaleItem = 0;
    private int _augSaleId = 0;
    private int _augSaleLvl = 0;

    public void setAugSaleItem(int id) {
        _augSaleItem = id;
    }

    public int getAugSaleItem() {
        return _augSaleItem;
    }

    public void setAugSale(int id, int lvl) {
        _augSaleId = id;
        _augSaleLvl = lvl;
    }

    public int getAugSaleId() {
        return _augSaleId;
    }

    public int getAugSaleLvl() {
        return _augSaleLvl;
    }

    // EnterWorld fix
    private boolean _inGame = false;

    public void setInGame(boolean f) {
        if (_client == null) {
            kick();
            return;
        }

        _inGame = f;
        if (f) {
            _client.startSession();

            if (Config.VS_HWID) {
                LoginServerThread.getInstance().setLastHwid(getAccountName(), getHWID());
            }
        }
    }

    public boolean isInGame() {
        return _inGame;
    }

    /*
     * .menu
     */
    private boolean _noExp = false;
    private boolean _lAlone = false;
    private boolean _autoLoot = false;
    private int _chatIgnore = 0;
    private long _tradersIgnore = 0;
    private boolean _geoPathFind;
    private boolean _skillChances = false;

    public void setNoExp(boolean f) {
        _noExp = f;
    }

    public boolean isNoExp() {
        return _noExp;
    }

    public void setAlone(boolean f) {
        _lAlone = f;
    }

    public boolean isAlone() {
        return _reviveRequested == 1 || _lAlone;
    }

    public void setAutoLoot(boolean f) {
        _autoLoot = f;
    }

    public boolean getAutoLoot() {
        return _autoLoot;
    }

    public void setChatIgnore(int f) {
        _chatIgnore = f;
    }

    public int getChatIgnore() {
        return _chatIgnore;
    }

    public void setTradersIgnore(long expire) {
        if (expire < System.currentTimeMillis()) {
            expire = 0;
        }
        _tradersIgnore = expire;
    }

    public boolean getTradersIgnore() {
        return (_tradersIgnore > System.currentTimeMillis());
    }

    public void setGeoPathfind(boolean f) {
        _geoPathFind = (Config.GEODATA == 2) ? f : false;
    }

    @Override
    public boolean geoPathfind() {
        if (isPartner()) {
            return true;
        }

        return _geoPathFind;
    }

    public void setShowSkillChances(boolean f) {
        _skillChances = f;
    }

    @Override
    public boolean getShowSkillChances() {
        return _skillChances;
    }

    // МассПвп, апдейт статуса в хтмл-ке
    private long _mpvplast = 0;

    public void setMPVPLast() {
        _mpvplast = System.currentTimeMillis();
    }

    public long getMPVPLast() {
        return _mpvplast;
    }

    // Для дальнейших евентов
    public boolean inEvent() {
        return EventManager.getInstance().isReg(this);
    }

    /*
     * public boolean inEventBattle() { return
     * EventManager.getInstance().isRegAndBattle(this); }
     */
    private boolean _eventWait = false;

    @Override
    public void setEventWait(boolean f) {
        _eventWait = f;
    }

    public boolean isEventWait() {
        return _eventWait;
    }

    public void enterMovieMode() {
        setTarget(null);
        // setIsInvul(true);
        // setIsImobilised(true);
        sendUserPacket(new CameraMode(1));
    }

    public void enterMovieMode(L2Object target) {
        setTarget(target);
        // setIsInvul(true);
        // setIsImobilised(true);
        sendUserPacket(new CameraMode(1));
    }

    public void leaveMovieMode() {
        // setIsInvul(false);
        // setIsImobilised(false);
        sendUserPacket(new CameraMode(0));
    }

    public void specialCamera(final L2Object target, final int dist, final int yaw, final int pitch, final int time,
            final int duration) {
        if (Config.DISABLE_BOSS_INTRO) {
            return;
        }

        sendUserPacket(new SpecialCamera(target.getObjectId(), dist, yaw, pitch, time, duration));
    }

    // выдача наград за голосование на опр. ник
    private String _voteRef = "no";

    public String voteRef() {
        if (!_voteRef.equalsIgnoreCase("no")) {
            return _voteRef;
        }

        String voter = "";

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM `z_vote_names` WHERE `from`=? LIMIT 1");
            st.setString(1, getName());
            rs = st.executeQuery();
            if (rs.next()) {
                voter = rs.getString("to");
            }
        } catch (final SQLException e) {
            _log.warning("voteRef() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }

        _voteRef = voter;
        return voter;
    }

    public void setVoteRf(String name) {
        if (_voteRef.equalsIgnoreCase(name)) {
            return;
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("REPLACE INTO `z_vote_names` (`from`,`to`) VALUES (?,?)");
            st.setString(1, getName());
            st.setString(2, name);
            st.execute();
        } catch (final SQLException e) {
            _log.warning("setVoteRf() error: " + e);
        } finally {
            Close.CS(con, st);
        }
        _voteRef = name;
    }

    public void delVoteRef() {
        if (_voteRef.equalsIgnoreCase("no")) {
            return;
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM `z_vote_names` WHERE `from`=?");
            st.setString(1, getName());
            st.execute();
        } catch (final SQLException e) {
            _log.warning("delVoteRef() error: " + e);
        } finally {
            Close.CS(con, st);
        }
        _voteRef = "no";
    }

    // окно вставки лс, посылаем нафик при вставке, если не было пакета
    private boolean _augFlag = false;

    public void setAugFlag(boolean f) {
        _augFlag = f;
    }

    public boolean getAugFlag() {
        return _augFlag;
    }

    // окно заточки скиллов, если в конфиге включена мультипрофа, посылаем нафик,
    // если не было пакета
    private int _aquFlag = 0;

    public void setAquFlag(int id) {
        _aquFlag = id;
    }

    public int getAquFlag() {
        return _aquFlag;
    }

    // Дальность торговли от других
    public boolean canTrade() {
        if (isParalyzed()
                || isCastingNow()
                || isFakeDeath()) {
            sendUserPacket(Static.PLEASE_WAIT);
            return false;
        }
        if (!ZoneManager.getInstance().inTradeZone(this)) {
            sendUserPacket(Static.NOT_TRADE_ZONE);
            return false;
        }

        FastList<L2PcInstance> players = getKnownList().getKnownPlayersInRadius(20);
        L2PcInstance pc = null;
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }

            if (pc.getPrivateStoreType() != L2PcInstance.PS_NONE) {
                sendUserPacket(Static.CANT_TRADE_NEAR);
                return false;
            }
        }
        pc = null;
        return true;
    }

    //
    private boolean _spyPacket = false;

    public void setSpyPacket(boolean f) {
        _spyPacket = f;
    }

    public boolean isSpyPckt() {
        return _spyPacket;
    }

    // fake
    public Location _fakeLoc = null;

    public void setFakeLoc(int x, int y, int z) {
        _fakeLoc = new Location(x, y, z);
    }

    @Override
    public Location getFakeLoc() {
        return _fakeLoc;
    }

    private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app, boolean aggro) {
        super(objectId, template);
        _accountName = accountName;
        _appearance = app;

        initAggro(aggro);
    }

    private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app, boolean aggro,
            boolean summon) {
        super(objectId, template);
        _accountName = accountName;
        _appearance = app;

        _ai = new L2PartnerAI(new L2PcInstance.AIAccessor());
    }

    private void initAggro(boolean aggro) {
        // Create an AI
        if (aggro) {
            switch (getClassId().getId()) {
                case 92:
                case 102:
                case 109:
                    _ai = new L2PlayerFakeArcherAI(new L2PcInstance.AIAccessor());
                    stopSkillEffects(99);
                    SkillTable.getInstance().getInfo(99, 2).getEffects(this, this);
                    break;
                default:
                    _ai = new L2PlayerFakeAI(new L2PcInstance.AIAccessor());
                    break;
            }
            if (isMageClass()) {
                doFullBuff(2);
            } else {
                doFullBuff(1);
            }
        } else {
            _ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
        }
    }

    public static L2PcInstance restoreFake(int objectId) {
        return restoreFake(objectId, 0, false);
    }

    public static L2PcInstance restoreFake(int objectId, int classId, boolean summon) {
        L2PcInstance player = null;

        int activeClassId = Rnd.get(89, 112);
        if (classId > 0) {
            activeClassId = classId;
        }

        final boolean female = Rnd.get(0, 1) != 0;

        final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
        byte abc = (byte) Rnd.get(3);
        PcAppearance app = new PcAppearance(abc, abc, abc, female);

        if (summon) {
            player = new L2PcInstance(objectId, template, "fake_qwerty", app, false, true);
        } else {
            player = new L2PcInstance(objectId, template, "fake_qwerty", app, true);
        }

        player.setFantome(true);

        player._lastAccess = 0;

        // long exp = 4115985617;
        // player.getStat().setExp(exp);
        // player.setExpBeforeDeath(0);
        // player.getStat().setLevel(lvl);
        player.getStat().setSp(2147483647);

        long pXp = player.getExp();
        long tXp = Experience.LEVEL[80];
        player.addExpAndSp(tXp - pXp, 0);

        player.setWantsPeace(0);

        player.setHeading(Rnd.get(1, 65535));

        player.setKarma(0);
        player.setPvpKills(0);
        player.setPkKills(0);

        player.setOnlineTime(0);

        player.setNewbie(false);

        final boolean noble = Rnd.get(0, 1) != 0;
        player.setNoble(noble);
        player.setHero(false);

        player.setClanJoinExpiryTime(0);
        player.setClanJoinExpiryTime(0);

        player.setClanCreateExpiryTime(0);
        player.setClanCreateExpiryTime(0);

        // int clanId = 0;
        player.setPowerGrade(5);
        player.setPledgeType(0);
        player.setLastRecomUpdate(0);

        player.setDeleteTimer(0);

        player.setAccessLevel(0);
        player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
        player.setUptime(System.currentTimeMillis());

        player.setCurrentHp(player.getMaxHp());
        player.setCurrentCp(player.getMaxCp());
        player.setCurrentMp(player.getMaxMp());

        // Check recs
        player.checkRecom(5, 1);

        player._classIndex = 0;
        try {
            player.setBaseClass(2);
        } catch (Exception e) {
            player.setBaseClass(activeClassId);
        }

        // Restore Subclass Data (cannot be done earlier in function)
        if (restoreSubClassData(player)) {
            if (activeClassId != player.getBaseClass()) {
                for (SubClass subClass : player.getSubClasses().values()) {
                    if (subClass.getClassId() == activeClassId) {
                        player._classIndex = subClass.getClassIndex();
                    }
                }
            }
        }

        player._activeClass = activeClassId;

        player.setApprentice(0);
        player.setSponsor(0);
        player.setLvlJoinedAcademy(0);
        player.setIsIn7sDungeon(false);
        player.setInJail(false);
        player.setJailTimer(0);

        player.setAllianceWithVarkaKetra(0);

        player.setDeathPenaltyBuffLevel(0);

        player.updatePcPoints(0, 0, false);

        try {
            player.stopAllTimers();
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }

        return player;
    }

    /**
     * 0 - стандарт 1 - лук 2 - даггер 3 - маг 4 - бишоg
     */
    private int _partnerClass = 0;

    public void setPartnerClass(int id) {
        _partnerClass = id;
    }

    @Override
    public int getPartnerClass() {
        return _partnerClass;
    }

    private L2PcInstance _partner;

    public void setPartner(L2PcInstance partner) {
        _partner = partner;
        if (_partner == null) {
            return;
        }
        _partner.setPartner();
    }

    @Override
    public L2PcInstance getPartner() {
        return _partner;
    }

    private boolean _isPartner = false;

    public void setPartner() {
        _isPartner = true;
    }

    @Override
    public boolean isPartner() {
        return _isPartner;
    }

    private L2PcInstance _owner;

    public void setOwner(L2PcInstance partner) {
        _owner = partner;
    }

    @Override
    public L2PcInstance getOwner() {
        return _owner;
    }

    private boolean _follow = true;

    @Override
    public void setFollowStatus(boolean state) {
        // _owner.sendAdmResultMessage("##setFollowStatus#" + state);
        _follow = state;
        if (_follow) {
            getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getOwner());
        } else {
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
        }
    }

    @Override
    public boolean getFollowStatus() {
        return _follow;
    }

    public void followOwner() {
        setFollowStatus(true);
    }

    @Override
    public void updateAndBroadcastPartnerStatus(int val) {
        // getOwner().sendPacket(new PetInfo(this, val));
        _owner.sendPacket(new PetStatusUpdate(this));

        if (isVisible()) {
            broadcastUserInfo();
        }

        // updateEffectIcons(true);
    }

    // фейк игроки
    private boolean _fantome = false;

    public void setFantome(boolean f) {
        _fantome = f;
    }

    @Override
    public boolean isFantome() {
        return _fantome;
    }

    // снифф пакетов опр. игрока
    private boolean _spy = false;

    public void setSpy(boolean f) {
        _spy = f;
    }

    public boolean isSpy() {
        return _spy;
    }

    // флаг, что к себе телепортит баюм
    private boolean _bt = false;

    public void setBaiTele(boolean f) {
        _bt = f;
    }

    public boolean isBaiTele() {
        return _bt;
    }

    /**
     * Бойцовский клуб
     */
    private int _fcObj = 0;
    private int _fcEnch = 0;
    private int _fcCount = 0;
    private int _fcAugm = 0;

    public void setFCItem(int objectId, int enchantLevel, int fcAugm, int iCount) {
        _fcObj = objectId;
        _fcEnch = enchantLevel;
        _fcAugm = fcAugm;
        _fcCount = iCount;
    }

    public int getFcObj() {
        return _fcObj;
    }

    public int getFcEnch() {
        return _fcEnch;
    }

    public int getFcCount() {
        return _fcCount;
    }

    public int getFcAugm() {
        return _fcAugm;
    }

    private boolean _fcBattle = false;

    public void setFightClub(boolean f) {
        _fcBattle = f;
    }

    public boolean inFightClub() {
        return _fcBattle;
    }

    private boolean _fcWait = false;

    public void setFClub(boolean f) {
        _fcWait = f;
    }

    public boolean inFClub() {
        return _fcWait;
    }

    //
    private boolean _inEnch = false;

    public void setInEnch(boolean f) {
        _inEnch = f;
    }

    public boolean inEnch() {
        return _inEnch;
    }

    //
    public void sendItems(boolean f) {
        sendPacket(new ItemList(this, f));
    }

    @Override
    public void sendEtcStatusUpdate() {
        sendUserPacket(new EtcStatusUpdate(this));

    }

    // пароль на чара
    /*
     * CREATE TABLE `z_char_keys` ( `char_id` int(10) unsigned NOT NULL DEFAULT
     * '0', `key` varchar(255) NOT NULL DEFAULT ' ', PRIMARY KEY (`char_id`) )
     * ENGINE=MyISAM DEFAULT CHARSET=utf8;
     */
    public static class UserKey {

        public String key;
        public int on;
        public int ptr;
        public int error = 1;

        UserKey(String key, int on, int ptr) {
            this.key = key;
            this.on = on;
            this.ptr = ptr;
        }

        public boolean checkLeft() {
            error += 1;
            return (error > 3);
        }
    }

    private UserKey _userKey = new UserKey("", 0, 0);

    public void checkUserKey(String key) {
        if (key.length() > 1) {
            _userKey.key = key.trim();
            _userKey.on = 1;

            // setIsParalyzed(true);
        }
    }

    public void setUserKey(String key) {
        if (_userKey.key.equalsIgnoreCase(key)) {
            return;
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE `character_settings` SET `charkey`=? WHERE `char_obj_id`=?");
            st.setString(1, key);
            st.setInt(2, getObjectId());
            st.execute();
        } catch (final SQLException e) {
            _log.warning("setUserKey() error: " + e);
        } finally {
            Close.CS(con, st);
        }
        _userKey.key = key;
        unsetUserKey();

        /*
         * NpcHtmlMessage htm = Static.SET_KEY_OK;
         * htm.replace("%KEY%", key);
         * sendUserPacket(htm);
         */
        // sendPacket(Static.SET_KEY_OK.replaceAndGet("%KEY%", key));
        // sendHtmlMessage("Уважаемый игрок!", Static.SET_KEY_OK.replaceAll("%KEY%",
        // key));
        NpcHtmlMessage htm = NpcHtmlMessage.id(0);
        htm.setFile("data/html/menu/set_form_ok.htm");
        htm.replace("%KEY%", key);
        sendUserPacket(htm);
    }

    public UserKey getUserKey() {
        return _userKey;
    }

    public void unsetUserKey() {
        _userKey.on = 0;
        // setIsParalyzed(false);
    }

    public void delUserKey() {
        if (_userKey.key.equalsIgnoreCase("")) {
            return;
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE `character_settings` SET `charkey`=? WHERE `char_obj_id`=?");
            st.setString(1, "");
            st.setInt(2, getObjectId());
            st.execute();
        } catch (final SQLException e) {
            _log.warning("delUserKey() error: " + e);
        } finally {
            Close.CS(con, st);
        }
        _userKey.key = "";
    }

    public void setUserKeyOnLevel() {
        if (_userKey.key.length() > 1) {
            return;
        }

        // setIsParalyzed(true);
        _userKey.on = 1;
        sendUserPacket(Static.SET_CHAR_KEY);
    }

    @Override
    public boolean isParalyzed() {
        if (_userKey.on == 1 && !_userKey.key.isEmpty()) {
            NpcHtmlMessage UNBLOCK_CHAR_KEY = new NpcHtmlMessage(0);

            if (_userKey.error == 1) {
                UNBLOCK_CHAR_KEY.setFile("data/html/menu/unblock_form.htm");
            } else {
                UNBLOCK_CHAR_KEY.setFile("data/html/menu/unblock_form_error.htm");
                UNBLOCK_CHAR_KEY.replace("%CHK_TRYES%", String.valueOf(_userKey.error));
            }

            sendPacket(UNBLOCK_CHAR_KEY);
            UNBLOCK_CHAR_KEY = null;
            return true;
        }

        return super.isParalyzed();
    }

    // антисуммон филд
    private boolean _antiSummon = false;

    public void setNoSummon(boolean f) {
        _antiSummon = f;
    }

    public boolean noSummon() {
        return _antiSummon;
    }

    /**
     * Задержка на одевание пушки
     *
     */
    private Lock wpnEquip = new ReentrantLock();
    private ScheduledFuture<?> _euipWeapon;

    public void equipWeapon(L2ItemInstance item) {
        wpnEquip.lock();
        try {
            if (_euipWeapon != null) {
                sendActionFailed();
                return;
            }

            if (CustomServerData.getInstance().isSpecialForrbid(_forbItem, item.getItemId())) {
                return;
            }
        } finally {
            wpnEquip.unlock();
        }

        if (isAttackingNow() || isCastingNow()) {
            _euipWeapon = ThreadPoolManager.getInstance().scheduleAi(new WeaponEquip(item), 700, true);
            sendActionFailed();
            return;
        }
        _euipWeapon = ThreadPoolManager.getInstance().scheduleAi(new WeaponEquip(item), 200, true);

    }

    private class WeaponEquip implements Runnable {

        final WeakReference<L2ItemInstance> wItem;

        public WeaponEquip(L2ItemInstance item) {
            this.wItem = new WeakReference<L2ItemInstance>(item);
        }

        @Override
        public void run() {
            abortAttack();
            abortCast();

            if (isMounted() || _isDeleting) {
                sendActionFailed();
                _euipWeapon = null;
                return;
            }

            L2ItemInstance item = wItem.get();
            if (item == null) {
                _euipWeapon = null;
                return;
            }
            int bodyPart = item.getItem().getBodyPart();
            L2ItemInstance weapon;
            L2ItemInstance[] items = null;

            // Equip or unEquip
            boolean isEquipped = item.isEquipped();
            if (isEquipped) {
                weapon = getActiveWeaponInstance();
                items = getInventory().unEquipItemInBodySlotAndRecord(bodyPart);
            } else {
                items = getInventory().equipItemAndRecord(item);
                weapon = getActiveWeaponInstance();
            }

            if (isEquipped != item.isEquipped()) {
                SystemMessage sm;
                if (isEquipped) {
                    if (item.getElement() > 0) {
                        sm = SystemMessage.id(SystemMessageId.S1_S2).addString(item.getWeaponName() + " было снято.");
                    } else if (item.getEnchantLevel() > 0) {
                        sm = SystemMessage.id(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(item.getEnchantLevel())
                                .addItemName(item.getItemId());
                    } else {
                        sm = SystemMessage.id(SystemMessageId.S1_DISARMED).addItemName(item.getItemId());
                    }
                    sendUserPacket(sm);

                    if (weapon != null && item == weapon) {
                        item.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
                        item.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
                        // snipe
                        if (weapon.getItemType() == L2WeaponType.BOW && getFirstEffect(313) != null) {
                            stopSkillEffects(313);
                        }
                    }
                } else {
                    if (item.getElement() > 0) {
                        sm = SystemMessage.id(SystemMessageId.S1_S2).addString("Одето " + item.getWeaponName());
                    } else if (item.getEnchantLevel() > 0) {
                        sm = SystemMessage.id(SystemMessageId.S1_S2_EQUIPPED).addNumber(item.getEnchantLevel())
                                .addItemName(item.getItemId());
                    } else {
                        sm = SystemMessage.id(SystemMessageId.S1_EQUIPPED).addItemName(item.getItemId());
                    }
                    sendUserPacket(sm);

                    if (weapon != null && item == weapon) {
                        rechargeAutoSoulShot(true, false, false);
                        rechargeAutoSoulShot(false, true, false);
                        // snipe
                        if (weapon.getItemType() != L2WeaponType.BOW && getFirstEffect(313) != null) {
                            stopSkillEffects(313);
                        }
                    }

                    if (item.isShadowItem()) {
                        sendCritMessage(item.getItemName() + ": осталось " + item.getMana() + " минут.");
                    }

                    if (item.getExpire() > 0) {
                        String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                                .format(new java.util.Date(item.getExpire()));
                        sendCritMessage(item.getItemName() + ": истекает " + date + ".");
                    }
                    item.decreaseMana(true);
                }
                sm = null;
                InventoryUpdate iu = new InventoryUpdate();
                iu.addItems(Arrays.asList(items));
                sendUserPacket(iu);

                refreshExpertisePenalty();
                broadcastUserInfo();
            }
            _euipWeapon = null;
        }
    }

    /**
     * Разгрузка проверок AI-мобов на агрессивность
     *
     */
    @Override
    public boolean isEnemyForMob(L2Attackable mob) {
        if (isInvisible()) {
            return false;
        }

        if (isInvul() && isGM()) {
            return false;
        }

        if (_isTeleporting) {
            return false;
        }

        if (!canSeeTarget(mob)) {
            return false;
        }

        if (mob.isL2Guard() && getKarma() > 0) {
            return true;
        }

        if (mob.isSiegeRaidGuard()) {
            return mob.isAutoAttackable(this);
        }

        // Check if the AI isn't a Raid Boss and the target isn't in silent move mode
        if (isSilentMoving() && !(mob.isRaid())) {
            return false;
        }

        // check if the target is within the grace period for JUST getting up from fake
        // death
        if (isRecentFakeDeath()) {
            return false;
        }

        // Check if player is an ally //TODO! [Nemesiss] it should be rather boolean or
        // smth like that
        // Comparing String isnt good idea!
        if ("varka".equals(mob.getFactionId()) && isAlliedWithVarka()) {
            return false;
        }

        if ("ketra".equals(mob.getFactionId()) && isAlliedWithKetra()) {
            return false;
        }

        if (mob.fromMonastry()) {
            if (getActiveWeaponItem() == null) {
                return false;
            } else {
                mob.sayString("Brother " + getName() + ", move your weapon away!!");
                return true;
            }
        }

        if (isInParty() && getParty().isInDimensionalRift()) {
            byte riftType = getParty().getDimensionalRift().getType();
            byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();

            if (mob.isL2RiftInvader() && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom)
                    .checkIfInZone(mob.getX(), mob.getY(), mob.getZ())) {
                return false;
            }
        }

        if ((mob.isL2Guard() || mob.isL2FriendlyMob()) && getKarma() == 0) {
            return false;
        }

        return mob.isAggressive();
    }

    /**
     * Для евента Сезон охоты
     */
    private int _osTeam = 0;

    public void setOsTeam(int team) {
        _osTeam = team;
        broadcastUserInfo();
        // if(getPet() != null)
        // getPet().broadcastPetInfo();
    }

    public int getOsTeam() {
        return _osTeam;
    }

    /**
     * Продажа скиллов
     */
    private boolean _havpwcs = false;
    private int _pwskill = 0;

    public void setHaveCustomSkills() {
        _havpwcs = true;
    }

    public boolean haveCustomSkills() {
        return _havpwcs;
    }

    public void setPwSkill(int id) {
        _pwskill = id;
    }

    public int getPwSkill() {
        return _pwskill;
    }

    public void checkDonateSkills() {
        FastTable<DonateSkill> donSkills = CustomServerData.getInstance().getDonateSkills(getObjectId());
        if (donSkills == null || donSkills.isEmpty()) {
            return;
        }

        for (int i = 0, n = donSkills.size(); i < n; i++) {
            DonateSkill di = donSkills.get(i);
            if (di == null) {
                continue;
            }

            if (!Config.DS_ALL_SUBS
                    && di.cls > 0
                    && (di.cls != getClassId().getId())) {
                continue;
            }

            if (di.expire > 0
                    && di.expire < System.currentTimeMillis()) {
                continue;
            }

            addSkill(SkillTable.getInstance().getInfo(di.id, di.lvl), false);
        }
        sendSkillList();
    }

    public void addDonateSkill(int cls, int id, int lvl, long expire) {
        CustomServerData.getInstance().addDonateSkill(getObjectId(), cls, id, lvl, expire);
    }

    /**
     * Премиум
     *
     */
    private boolean _premium = false;
    private long _premiumExpire = 0;

    public void setPremium(boolean f) {
        _premium = f;
    }

    public void setPremiumExpire(long expire) {
        this._premiumExpire = expire;
    }

    public boolean isPremium() {
        if (Config.PREMIUM_ENABLE && _premiumExpire > 0) {
            if (_premiumExpire < System.currentTimeMillis()) {
                storePremium(0);
                return false;
            }
        }
        return _premium;
    }

    public void storePremium(int days) {
        long expire = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
        if (days == 0) {
            expire = 0;
        }
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE `characters` SET `premium`=? WHERE `obj_Id`=?");
            st.setLong(1, expire);
            st.setInt(2, getObjectId());
            st.execute();
        } catch (SQLException e) {
            _log.warning("addPremium(L2PcInstance player, int days) error: " + e);
        } finally {
            Close.CS(con, st);
        }

        if (days == 0) {
            _premium = false;
            _premiumExpire = 0;
            sendCritMessage("Статус Премиум истек!");
            if (Config.PREMIUM_ITEMS) {
                for (L2ItemInstance item : getInventory().getItems()) {
                    if (item == null) {
                        continue;
                    }

                    if (item.isPremiumItem()) {
                        getInventory().unEquipItemInBodySlotAndRecord(getInventory().getSlotFromItem(item));
                    }
                }
            }
            return;
        }

        setPremium(true);
        broadcastUserInfo();

        String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(expire));
        sendCritMessage("Статус Премиум: до " + date);
    }

    private static class StopPremium implements Runnable {

        int id;

        public StopPremium(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            L2PcInstance player = L2World.getInstance().getPlayer(id);

            if (player != null) {
                player.storePremium(0);
            }
        }
    }

    private static class StopHero implements Runnable {

        int id;

        public StopHero(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            L2PcInstance player = L2World.getInstance().getPlayer(id);

            if (player != null) {
                for (L2ItemInstance item : player.getInventory().getItems()) {
                    if (item == null) {
                        continue;
                    }
                    if (item.isHeroItem()) {
                        player.destroyItem("Hero", item, player, true);
                    }
                }

                player.setHero(false);
                player.broadcastUserInfo();
            }
        }
    }

    /**
     * Покупное геройство
     */
    private long _heroExpire = 0;

    public void setHero(int days) {
        setHero(true);
        // broadcastPacket(new SocialAction(getObjectId(), 16));
        broadcastUserInfo();

        if (days == 0) {
            _heroExpire = 3;
            return;
        } else {
            _heroExpire = (days == -1 ? 1 : (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)));
        }

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE `characters` SET `hero`=? WHERE `obj_Id`=?");
            st.setLong(1, _heroExpire);
            st.setInt(2, getObjectId());
            st.execute();
        } catch (SQLException e) {
            _log.warning("setHero(int days) error: " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    public void setHeroExpire(long expire) {
        _heroExpire = expire;
    }

    public void sendTempMessages() {
        revalidateZone(true);
        if (_heroExpire == 1) {
            sendCritMessage("Статус Героя: бесконечный.");
        } else if (_heroExpire == -1) {
            sendCritMessage("Статус Героя: истек.");
        } else if (_heroExpire > 1) {
            String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(_heroExpire));
            sendCritMessage("Статус Героя: до " + date);
        }

        if (_premiumExpire == -1) {
            sendCritMessage("Статус Премиум: истек.");
        } else if (_premiumExpire > 1) {
            String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                    .format(new java.util.Date(_premiumExpire));
            sendCritMessage("Статус Премиум: до " + date);
        }

        if (_tradersIgnore > 1) {
            String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                    .format(new java.util.Date(_tradersIgnore));
            sendCritMessage("Чат героя: до " + date);
        }

        if (Config.HTMPARA_WELCOME && !hasFourSide()) {
            sendUserPacket(Static.PARA_WELCOME);
            return;
        }

        if (_userKey.on == 1) {
            sendUserPacket(Static.UNBLOCK_CHAR_KEY);
        } else if (Config.VS_CKEY_CHARLEVEL && getLevel() >= 40) {
            setUserKeyOnLevel();
        } else if (Config.VS_EMAIL && !hasEmail()) {
            sendUserPacket(Static.CHANGE_EMAIL);
        } else if (Config.SERVER_NEWS) {
            sendUserPacket(Static.SERVER_WELCOME);
        }
    }

    // стихии
    private int _fourSide = 0;

    private void setFourSide(int f) {
        _fourSide = f;
        if (!Config.HTMPARA_WELCOME) {
            return;
        }
        if (f > 1) {
            _userKey.on = 0;
        } else {
            _userKey.on = 1;
        }
    }

    public void setFourSideSkill(int id) {
        if (!Config.HTMPARA_WELCOME) {
            return;
        }
        setFourSide(id);
        addSkill(SkillTable.getInstance().getInfo(id, 1), true);
        sendHtmlMessage("Выбор сделан, теперь вы можете играть.");
    }

    private boolean hasFourSide() {
        if (!Config.HTMPARA_WELCOME) {
            return false;
        }
        return _fourSide > 1;
    }

    public void setReborned() {
        _fourSide = 5;
    }

    // шадов затычка
    private boolean _shdItems = false;

    public void setShadeItems(boolean f) {
        _shdItems = f;
    }

    public boolean getShadeItems() {
        return (_shdItems == false);
    }

    // твт затычка
    private boolean _tvtPassive = true;

    public void setTvtPassive(boolean f) {
        _tvtPassive = f;
    }

    public boolean isTvtPassive() {
        return _tvtPassive;
    }

    // Почта
    private int _bbsMailItem = 0;
    private String _bbsMailSender = "n.a";
    private String _bbsMailTheme = "n.a";

    public int getBriefItem() {
        return _bbsMailItem;
    }

    public void setBriefItem(int obj) {
        _bbsMailItem = obj;
    }

    public void setBriefSender(String sender) {
        _bbsMailSender = sender;
    }

    public String getBriefSender() {
        return _bbsMailSender;
    }

    public void setMailTheme(String sender) {
        _bbsMailTheme = sender;
    }

    public String getMailTheme() {
        return _bbsMailTheme;
    }

    // Смена пола
    public void setSex(boolean f) {
        byte abc = (byte) Rnd.get(3);
        _appearance = new PcAppearance(abc, abc, abc, f);
        // getAppearance().setSex(f);
    }

    // пк/пвп бонус
    private long _lastPvPPk = 0;

    @Override
    public long getLastPvpPk() {
        return _lastPvPPk;
    }

    public void setLastPvpPk() {
        _lastPvPPk = System.currentTimeMillis();
    }

    public void setLastPvpPkBan() {
        _lastPvPPk = System.currentTimeMillis() + Config.PVPPK_STEPBAN;
    }

    public void setLastPvpPkBan(long delay) {
        _lastPvPPk = System.currentTimeMillis() + delay;
    }

    public Map<Integer, Integer> _pvppk_penalties = new ConcurrentHashMap<Integer, Integer>();

    private boolean canChangeBonusColor(int color, boolean name) {
        if (color == 0) {
            return false;
        }

        if (name) {
            if (Config.PVPBONUS_COLORS_NAME.contains(getAppearance().getNameColor())) {
                getAppearance().setNameColor(color);
                return true;
            }
        } else if (Config.PVPBONUS_COLORS_TITLE.contains(getAppearance().getTitleColor())) {
            getAppearance().setTitleColor(color);
            return true;
        }
        return false;
    }

    private void managePvpFarmReward(L2PcInstance killer, boolean b) {
        // System.out.println("###1#");
        if (System.currentTimeMillis() - killer.getLastPvpPk() < Config.PVPPK_INTERVAL) {
            return;
        }
        killer.setLastPvpPk();
        // System.out.println("###2#");

        if (!Config.REWARD_PVP_ZONE_IP
                && getIP().equals(killer.getIP())) {
            return;
        }

        if (!Config.REWARD_PVP_ZONE_HWID
                && getHWID().equals(killer.getHWID())) {
            return;
        }

        if (!Config.REWARD_PVP_ZONE_CLAN
                && (killer.getClan() != null && killer.getClanId() == getClanId())) {
            return;
        }

        if (!Config.REWARD_PVP_ZONE_PARTY
                && (killer.isInParty() && killer.getParty().getPartyMembers().contains(this))) {
            return;
        }

        // System.out.println("###3#");
        if (Config.PVPPK_STEP > 0) {
            Integer last = _pvppk_penalties.get(killer.getObjectId());
            if (last == null) {
                _pvppk_penalties.put(killer.getObjectId(), 1);
            } else {
                if (last > Config.PVPPK_STEP) {
                    killer.setLastPvpPkBan();
                    _pvppk_penalties.clear();
                    return;
                }
                _pvppk_penalties.put(killer.getObjectId(), last + 1);
            }
        }

        ZoneManager.getInstance().checkPvpRewards(killer);
    }

    private void managePvpPkBonus(L2PcInstance killer, boolean checkPvpZone) {

        if (killer.getKarma() == 0) {
            PvpColor color_bonus = Config.PVPBONUS_COLORS.get(killer.getPvpKills());
            if (color_bonus != null) {

                boolean update = killer.canChangeBonusColor(color_bonus.nick, true);
                if (!update) {
                    update = killer.canChangeBonusColor(color_bonus.title, false);
                }

                if (update) {
                    killer.broadcastUserInfo();
                    killer.store();
                }
            }
        }

        if (getChannel() > 1) {
            return;
        }

        if (checkPvpZone && (isInsidePvpZone() || killer.isInsidePvpZone())) {
            return;
        }

        if (Config.PVPPK_REWARD_ZONE && !isInsidePpvFarmZone()) {
            return;
        }

        if (System.currentTimeMillis() - killer.getLastPvpPk() < Config.PVPPK_INTERVAL) {
            return;
        }

        if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getName())) {
            return;
        }

        if (Config.PVPPK_IPPENALTY && getIP().equals(killer.getIP())) {
            return;
        }

        if (Config.PVPPK_HWIDPENALTY && getHWID().equals(killer.getHWID())) {
            return;
        }

        killer.setLastPvpPk();

        if (Config.PVPPK_STEP > 0) {
            Integer last = _pvppk_penalties.get(killer.getObjectId());
            if (last == null) {
                _pvppk_penalties.put(killer.getObjectId(), 1);
            } else {
                if (last > Config.PVPPK_STEP) {
                    killer.setLastPvpPkBan();
                    _pvppk_penalties.clear();
                    return;
                }
                _pvppk_penalties.put(killer.getObjectId(), last + 1);
            }
        }

        PvpColor expsp = null;
        if (killer.getKarma() > 0) {
            if (killer.getLevel() - getLevel() > Config.PVPPK_PENALTY.title) {
                return;
            }

            expsp = Config.PVPPK_EXP_SP.get(1);

            EventReward reward = null;
            for (FastList.Node<EventReward> k = Config.PVPPK_PKITEMS.head(),
                    endk = Config.PVPPK_PKITEMS.tail(); (k = k.getNext()) != endk;) {
                reward = k.getValue();
                if (reward == null) {
                    continue;
                }

                if (Rnd.get(100) < reward.chance) {
                    killer.addItem("pk_bonus", reward.id, reward.count, killer, true);
                }
            }
            reward = null;
            /*
             * for (FastMap.Entry<Integer, Integer> e =
             * Config.PVPPK_PKITEMS.head(), end = Config.PVPPK_PKITEMS.tail();
             * (e = e.getNext()) != end;) { Integer item_id = e.getKey();
             * Integer item_count = e.getValue(); if (item_id == null ||
             * item_count == null) continue;
             *
             * killer.addItem("pk_bonus", item_id, item_count, killer, true); }
             */
        } else {
            if (killer.getLevel() - getLevel() > Config.PVPPK_PENALTY.nick) {
                return;
            }

            expsp = Config.PVPPK_EXP_SP.get(0);

            /*
             * for (FastMap.Entry<Integer, Integer> e =
             * Config.PVPPK_PVPITEMS.head(), end = Config.PVPPK_PVPITEMS.tail();
             * (e = e.getNext()) != end;) { Integer item_id = e.getKey();
             * Integer item_count = e.getValue(); if (item_id == null ||
             * item_count == null) continue;
             *
             * killer.addItem("pvp_bonus", item_id, item_count, killer, true); }
             */
            EventReward reward = null;
            for (FastList.Node<EventReward> k = Config.PVPPK_PVPITEMS.head(),
                    endk = Config.PVPPK_PVPITEMS.tail(); (k = k.getNext()) != endk;) {
                reward = k.getValue();
                if (reward == null) {
                    continue;
                }

                if (Rnd.get(100) < reward.chance) {
                    killer.addItem("pvp_bonus", reward.id, reward.count, killer, true);
                }
            }
            reward = null;

            //
            PvpColor item_bonus = Config.PVPBONUS_ITEMS.get(killer.getPvpKills());
            if (item_bonus != null) {
                killer.addItem("pk_bonus", item_bonus.nick, item_bonus.title, killer, true);
            }
        }

        if (expsp != null && (expsp.nick > 0 || expsp.title > 0)) {
            killer.addExpAndSp((long) expsp.nick, expsp.title);
        }
    }

    private boolean _isInPpvFarm = false;
    private int _isInPpvRewZone = 0;

    public boolean isInsidePpvFarmZone() {
        return _isInPpvFarm;
    }

    @Override
    public void setInPvpFarmZone(boolean f) {
        _isInPpvFarm = f;
    }

    @Override
    public void setInPvpRewardZone(int id) {
        _isInPpvRewZone = id;
    }

    public boolean isInsidePpvRewardZone() {
        return _isInPpvRewZone != 0;
    }

    public int getPpvRewardZone() {
        return _isInPpvRewZone;
    }

    // ip
    public String getIP() {
        if (_client == null || _isDeleting || !_isConnected) {
            return "None";
        }

        // return getClient().get().getSocket().getInetAddress().getHostAddress();
        return getClient().getIpAddr();
    }

    // проверки гейт чант/клан гейт/суммон френд
    public boolean canSummon() {
        if (underAttack()) {
            sendUserPacket(Static.YOU_CANNOT_SUMMON_IN_COMBAT);
            return false;
        }
        if (Olympiad.isRegisteredInComp(this) || isInOlympiadMode()) {
            sendUserPacket(Static.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
            return false;
        }
        if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getName())) {
            sendUserPacket(Static.YOU_CANNOT_SUMMON_IN_COMBAT);
            return false;
        }
        if (isInsidePvpZone()) {
            sendUserPacket(Static.YOU_CANNOT_SUMMON_IN_COMBAT);
            return false;
        }
        if (isInsideZone(L2Character.ZONE_BOSS)) {
            sendUserPacket(Static.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
            return false;
        }
        if (hasSummonPenalty()) {
            sendUserPacket(Static.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
            return false;
        }
        if (isInsideSilenceZone()) {
            sendUserPacket(Static.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
            return false;
        }
        if (isEventWait() || isInEncounterEvent() || isInNoLogoutArea()) {
            sendUserPacket(Static.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
            return false;
        }
        if (getKnownList().existsDoorsInRadius(169)) {
            sendUserPacket(Static.YOU_MAY_NOT_SUMMON_NEAR_DOORS);
            return false;
        }

        FastList<L2Object> objects = L2World.getInstance().getVisibleObjects(this, 2000);
        if (!objects.isEmpty()) {
            for (FastList.Node<L2Object> n = objects.head(), end = objects.tail(); (n = n.getNext()) != end;) {
                L2Object object = n.getValue();
                if (object instanceof L2RaidBossInstance) {
                    sendUserPacket(Static.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canBeSummoned(L2PcInstance caster) {
        if (isAlikeDead()) {
            /*
             * SystemMessage sm =
             * SystemMessage.id(SystemMessageId.
             * S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED);
             * sm.addString(getName()); caster.sendPacket(sm);
             */
            caster.sendUserPacket(SystemMessage.id(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED)
                    .addString(getName()));
            sendUserPacket(Static.CANT_BE_SUMMONED_NOW);
            return false;
        }

        if (isInStoreMode()) {
            caster.sendUserPacket(SystemMessage
                    .id(SystemMessageId.S1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED)
                    .addString(getName()));
            sendUserPacket(Static.CANT_BE_SUMMONED_NOW);
            return false;
        }

        // Target cannot be in combat (or dead, but that's checked by TARGET_PARTY)
        if (isRooted() || underAttack()) {
            caster.sendUserPacket(SystemMessage.id(SystemMessageId.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED)
                    .addString(getName()));
            sendUserPacket(Static.CANT_BE_SUMMONED_NOW);
            return false;
        }

        // Check for the the target's festival status
        if (Olympiad.isRegisteredInComp(this) || isInOlympiadMode()) {
            caster.sendUserPacket(Static.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD);
            sendUserPacket(Static.CANT_BE_SUMMONED);
            return false;
        }

        // Check for the the target's festival status
        if (isFestivalParticipant()) {
            caster.sendUserPacket(Static.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            sendUserPacket(Static.CANT_BE_SUMMONED);
            return false;
        }

        // Check for the target's jail status, arenas and siege zones
        if (isInsidePvpZone()) {
            caster.sendUserPacket(Static.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            sendUserPacket(Static.CANT_BE_SUMMONED);
            return false;
        }

        // Check for the target's jail status, arenas and siege zones
        if (isInsideSilenceZone()) {
            caster.sendUserPacket(Static.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            sendUserPacket(Static.CANT_BE_SUMMONED);
            return false;
        }

        // Check for the the target's Inside Boss Zone
        if (isInsideZone(L2Character.ZONE_BOSS)) {
            caster.sendUserPacket(Static.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            sendUserPacket(Static.CANT_BE_SUMMONED);
            return false;
        }

        if (isEventWait() || isInEncounterEvent() || isInNoLogoutArea()) {
            caster.sendUserPacket(Static.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            sendUserPacket(Static.CANT_BE_SUMMONED);
            return false;
        }

        if (getChannel() != caster.getChannel()) {
            caster.sendUserPacket(Static.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            sendUserPacket(Static.CANT_BE_SUMMONED);
            return false;
        }

        if (hasSummonPenalty()) {
            caster.sendUserPacket(Static.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            sendUserPacket(Static.CANT_BE_SUMMONED);
            return false;
        }

        if (getItemCount(8615) == 0) {
            caster.sendMessage("Персонаж " + getName() + " не может быть призван без Summoning Crystal.");
            sendUserPacket(Static.NO_SUMMON_CRY);
            return false;
        }
        return true;
    }

    // антиклик
    private int _enchClicks = 0;
    private int _enchLesson = 0;

    public void updateEnchClicks() {
        _enchClicks += 1;
    }

    public int getEnchClicks() {
        return _enchClicks;
    }

    public int getEnchLesson() {
        return _enchLesson;
    }

    public void showAntiClickPWD() {
        // if (_enchClicks < 99900)
        _enchClicks = Rnd.get(99900, 99921);
        _enchLesson = _enchClicks;
        NpcHtmlMessage html = NpcHtmlMessage.id(0);
        switch (Config.ENCH_ANTI_CLICK_TYPE) {
            case 0:
                TextBuilder tb = new TextBuilder("<br>");
                for (int i = (Rnd.get(30)); i > 0; i--) {
                    tb.append("<br>");
                }
                html.setHtml(
                        "<html><body><font color=\"FF6600\">!Превышен лимит заточки!</font><br>Нажмите на кнопку для продолжения заточки!<br> <table width=\""
                                + Rnd.get(40, 300) + "\"><tr><td align=\"right\">" + tb.toString()
                                + "<button value=\"Продолжить\" action=\"bypass -h ench_click " + _enchClicks
                                + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></body></html>");
                // 3.0Riddle riddle = CustomServerData.getInstance().getRiddle(_enchClicks);
                // html.setHtml("<html><body>Отгадай загадку!<br>" + riddle.question +
                // "<br><edit var=\"pwd\" width=60 length=\"8\"><br><br><button value=\"Ok\"
                // action=\"bypass -h ench_click $pwd\" width=40 height=15 back=\"sek.cbui94\"
                // fore=\"sek.cbui92\"></body></html>");
                tb.clear();
                tb = null;
                break;
            case 1:
                html.setHtml("<html><body>Введите этот код,<br>=== <font color=LEVEL>" + _enchClicks
                        + "</font> ===<br> для продолжения заточки: <br><edit var=\"pwd\" width=60 length=\"5\"><br><br><button value=\"Ok\" action=\"bypass -h ench_click $pwd\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></body></html>");
                break;
            case 2:
                int a = Rnd.get(100);
                int b = Rnd.get(100);
                _enchLesson = a + b;
                html.setHtml("<html><body>Решите пример,<br>=== <font color=LEVEL> " + a + " + " + b
                        + " = ?</font>, ===<br> для продолжения заточки: <br><edit var=\"pwd\" width=60 length=\"4\"><br><br><button value=\"Ok\" action=\"bypass -h ench_click $pwd\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></body></html>");
                break;
        }
        sendUserPacket(html);
    }

    public void showAntiClickOk() {
        if (_enchClicks == 99908) {
            sendHtmlMessage("Правильно, коровы!", "Пейте, дети, молоко -<br1>Будете здоровы!");
        } else {
            sendHtmlMessage("Спасибо", "можете точить дальше.");
        }

        _enchClicks = 0;
        _enchLesson = 0;
    }

    public boolean checkEnchClicks(String answer) {
        return answer.equalsIgnoreCase(CustomServerData.getInstance().getRiddle(_enchClicks).answer);
    }

    // тп в гиран
    @Override
    public void teleToClosestTown() {
        teleToLocation(MapRegionTable.TeleportWhereType.Town);
        // teleToLocation(83461, 149018, -3431);
    }

    // евент дозор
    private boolean _isInEncounterEvent = false;

    public void setInEncounterEvent(boolean f) {
        _isInEncounterEvent = f;
    }

    public boolean isInEncounterEvent() {
        return _isInEncounterEvent;
    }

    // евент медальки
    private int _eventColNumber = 0;

    public void setEventColNumber(int rnd) {
        _eventColNumber = rnd;
    }

    public int getEventColNumber() {
        return _eventColNumber;
    }

    private long quest_last_reward_time = 0;

    public void setQuestLastReward() {
        quest_last_reward_time = System.currentTimeMillis();
    }

    public long getQuestLastReward() {
        return quest_last_reward_time;
    }

    // pc cafe
    private int _pcPoints = 0;

    public int getPcPoints() {
        return _pcPoints;
    }

    public void setPcPoints(int points) {
        _pcPoints = points;
    }

    public void updatePcPoints(int points, int type, boolean _double) {
        if (_double) {
            points *= 2;
        }

        switch (type) {
            case 1: // отнимаем
                sendUserPacket(new ExPCCafePointInfo(this, 0, false, false));
                sendMessage(Static.CONSUMED_S1_PCPOINTS.replace("%a%", String.valueOf(points)));
                break;
            case 2: // добавляем
                if (_premium) {
                    points *= Config.PREMIUM_PCCAFE_MUL;
                }

                sendUserPacket(new ExPCCafePointInfo(this, points, true, _double));
                sendMessage(Static.EARNED_S1_PCPOINTS.replace("%a%", String.valueOf(points)));
                break;
        }
        _pcPoints += points;
    }

    // scrolls
    private int _nextScroll = 0;

    public void setNextScroll(int id) {
        _nextScroll = id;
    }

    public int getNextScroll() {
        return _nextScroll;
    }

    public void useNextScroll() {
        if (_nextScroll == 0) {
            return;
        }

        L2ItemInstance item = _inventory.getItemByObjectId(_nextScroll);
        if (item != null) {
            IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());
            if (handler != null) {
                handler.useItem(this, item, false);
            }
        }
        _nextScroll = 0;
    }

    /**
     * party waiting room
     */
    // - ищу пати
    private boolean _lookingForParty = false;

    public void setLFP(boolean f) {
        _lookingForParty = f;
    }

    public boolean isLFP() {
        return _lookingForParty;
    }

    private WaitingRoom _partyRoom = null;

    public void setPartyRoom(WaitingRoom room) {
        _partyRoom = room;
    }

    public WaitingRoom getPartyRoom() {
        return _partyRoom;
    }

    public void sayToPartyRoom(CreatureSay cs) {
        if (_partyRoom == null) {
            return;
        }
        _partyRoom.sayToPartyRoom(cs);
    }

    /**
     * невидимость для остальных, концепт 0 - инвиз 1 - обычный 2 - олимп 3 -
     * просмотр 9 - торговцы //ивенты 4 - захват базы 5 - zames 6 - last hero 7
     * - mass pvp 8 - tvt
     *
     */
    private int _channel = 1;

    @Override
    public void setChannel(int channel) {
        _channel = channel;
    }

    public void setEventChannel(int channel) {
        _channel = channel;
        switch (channel) {
            case 1:
                _showMaskName = false;
                _maskName = "";
                break;
            case 6: // lh
                if (Config.ELH_HIDE_NAMES) {
                    _showMaskName = true;
                    _maskName = Config.ELH_ALT_NAME;
                }
                break;
            case 8: // tvt
                if (Config.TVT_HIDE_NAMES) {
                    _showMaskName = true;
                    _maskName = Config.TVT_ALT_NAME;
                }
                break;
        }
        if (channel == 8 && Config.FORBIDDEN_EVENT_ITMES) {
            // снятие переточеных вещей
            boolean found = false;
            for (L2ItemInstance item : getInventory().getItems()) {
                if (item == null) {
                    continue;
                }

                if (item.notForOly()) {
                    getInventory().unEquipItemInBodySlotAndRecord(getInventory().getSlotFromItem(item));
                }
            }
            if (found) {
                sendItems(false);
                // broadcastUserInfo();
            }
        }
    }

    @Override
    public int getChannel() {
        return _channel;
    }

    public boolean isInvisible() {
        return (_channel == 0);
    }

    /**
     * summon friend *
     */
    private Location _sfLoc = null;

    public void setSfLoc(Location loc) {
        _sfLoc = loc;
    }

    public Location getSfLoc() {
        return _sfLoc;
    }

    private int _sfRequest = 0;
    private long _sfTime = 0;

    public void sendSfRequest(L2PcInstance requester, ConfirmDlg dialog) {
        if (requester == null || dialog.getLoc() == null) {
            return;
        }

        _sfTime = System.currentTimeMillis() + Config.SUMMON_ANSWER_TIME;
        _sfRequest = 1;
        // setSfLoc(new Location(requester.getX()+Rnd.get(50),
        // requester.getY()+Rnd.get(50), requester.getZ()));
        setSfLoc(dialog.getLoc());
        sendUserPacket(dialog);
    }

    public void sfAnswer(int answer) {
        if (answer == 0 || _sfRequest == 0 || _sfLoc == null) {
            return;
        }

        if (Config.SUMMON_ANSWER_TIME > 0 && System.currentTimeMillis() > _sfTime) {
            sendUserPacket(Static.ANSWER_TIMEOUT);
            return;
        }

        teleToLocation(_sfLoc.x, _sfLoc.y, _sfLoc.z, false);
        _sfLoc = null;
        _sfTime = 0;
        _sfRequest = 0;
    }

    /**
     * телепортер на ивентах
     */
    @Override
    public void teleToLocation(int x, int y, int z) {
        if (_pvpArena) {
            sendUserPacket(Static.CANT_TELE_ON_EVENT);
            return;
        }

        /*
         * if (Config.FORB_EVENT_REG_TELE) {
         * EventManager.getInstance().onExit(this); }
         */
        super.teleToLocation(x, y, z, false);
    }

    @Override
    public void teleToLocation(TeleportWhereType teleportWhere) {
        super.teleToLocation(teleportWhere);
    }

    public void teleToLocationEvent(int x, int y, int z) {
        teleToLocationEvent(x, y, z, false);
    }

    public void teleToLocationEvent(Location loc) {
        teleToLocationEvent(loc.x, loc.y, loc.z, false);
    }

    public void teleToLocationEvent(int x, int y, int z, boolean f) {
        if (isMounted()) {
            if (setMountType(0)) {
                if (isFlying()) {
                    removeSkill(SkillTable.getInstance().getInfo(327, 1));
                    removeSkill(SkillTable.getInstance().getInfo(4289, 1));
                }
                broadcastPacket(new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0));
                setMountObjectID(0);
            }
        }
        super.teleToLocation(x, y, z, f);
    }

    // титул
    @Override
    public String getTitle() {
        if (_showMaskName) {
            return _maskName;
        }

        if (Config.PVP_TITLE_SKILL
                && _pvpTitle != null
                && !_pvpTitle.isEmpty()) {
            return _pvpTitle;
        }

        /*
         * if (!Config.STARTUP_TITLE.equalsIgnoreCase("off") && !isNoble() && getClan()
         * == null) {
         * return Config.STARTUP_TITLE;
         * }
         */
        return super.getTitle();
    }

    /*
     * оффтрейд DROP TABLE IF EXISTS `character_offline`; CREATE TABLE
     * `character_offline` ( `obj_id` int(11) NOT NULL DEFAULT '0', `name`
     * varchar(86) NOT NULL DEFAULT '0', `value` varchar(255) NOT NULL DEFAULT
     * '0', UNIQUE KEY `prim` (`obj_id`,`name`), KEY `obj_id` (`obj_id`), KEY
     * `name` (`name`), KEY `value` (`value`) ) ENGINE=MyISAM DEFAULT
     * CHARSET=utf8;
     */
    private boolean _offline = false;

    public void setOfflineMode(boolean f) {
        setOfflineMode(f, true);
    }

    public void setOfflineMode(boolean f, boolean start) {
        if (f) {
            _offline = true;
            if (getParty() != null) {
                getParty().oustPartyMember(this);
            }

            if (getFairy() != null) {
                getFairy().unSummon(this);
            }

            if (getPet() != null) {
                getPet().unSummon(this);
            }

            if (Olympiad.isRegisteredInComp(this) || isInOlympiadMode() || getOlympiadGameId() > -1) {
                Olympiad.logoutPlayer(this);
            }

            if (Config.ALT_RESTORE_OFFLINE_TRADE && start) {
                setVar("offline", String.valueOf(System.currentTimeMillis() + Config.ALT_OFFLINE_TRADE_LIMIT), null);
            }
            /*
             * L2Object obj = (L2Object)this; obj.getPoly().setPolyInfo("npc",
             * "31148"); // 31625 // 32033 obj.decayMe();
             * obj.spawnMe(obj.getX(),obj.getY(),obj.getZ());
             */
            store();
            sendUserPacket(Static.ServerClose);
            setConnected(false);
            // closeNetConnection();

            broadcastUserInfo();

            if (Config.ALT_OFFLINE_TRADE_ONLINE) {
                Connect con = null;
                PreparedStatement st = null;
                try {
                    con = L2DatabaseFactory.get();
                    st = con.prepareStatement("UPDATE characters SET online=? WHERE obj_id=?");
                    st.setInt(1, 2);
                    st.setInt(2, getObjectId());
                    st.execute();
                } catch (SQLException e) {
                    _log.warning("could not set char offline status:" + e);
                } finally {
                    Close.CS(con, st);
                }
            }
        } else {
            unsetVars();
            setPrivateStoreType(0);
            _offline = false;
        }
    }

    public boolean isInOfflineMode() {
        return _offline;
    }

    private Map<String, String> _offtrade_items = new ConcurrentHashMap<String, String>();

    public void saveTradeList() {
        if (!Config.ALT_ALLOW_OFFLINE_TRADE || !Config.ALT_RESTORE_OFFLINE_TRADE) {
            return;
        }

        TextBuilder tb = new TextBuilder();
        Connect con = null;
        try {
            con = L2DatabaseFactory.get();
            if (_sellList == null || _sellList.getItemCount() == 0) {
                unsetVar("selllist", con);
            } else {
                for (TradeItem i : _sellList.getItems()) {
                    tb.append(i.getObjectId() + ";" + i.getCount() + ";" + i.getPrice() + ":");
                }
                setVar("selllist", tb.toString(), con);
                if (_sellList != null && _sellList.getTitle() != null) {
                    setVar("sellstorename", _sellList.getTitle(), con);
                }
            }
            tb.clear();

            if (_buyList == null || _buyList.getItemCount() == 0) {
                unsetVar("buylist", con);
            } else {
                for (TradeItem i : _buyList.getItems()) {
                    tb.append(i.getItem().getItemId() + ";" + i.getCount() + ";" + i.getPrice() + ":");
                }
                setVar("buylist", tb.toString(), con);
                if (_buyList != null && _buyList.getTitle() != null) {
                    setVar("buystorename", _buyList.getTitle(), con);
                }
            }
            tb.clear();

            if (_createList == null || _createList.getList().isEmpty()) {
                unsetVar("createlist", con);
            } else {
                for (L2ManufactureItem i : _createList.getList()) {
                    tb.append(i.getRecipeId() + ";" + i.getCost() + ":");
                }
                setVar("createlist", tb.toString(), con);
                if (_createList.getStoreName() != null) {
                    setVar("manufacturename", _createList.getStoreName(), con);
                }
            }
        } catch (SQLException e) {
            _log.warning("could not saveTradeList(): " + e);
        } finally {
            tb.clear();
            tb = null;
            Close.C(con);
        }
    }

    public void restoreTradeList() {
        if (!Config.ALT_RESTORE_OFFLINE_TRADE) {
            return;
        }

        if (getVar("selllist") != null) {
            _sellList = new TradeList(this);
            String[] items = getVar("selllist").split(":");
            for (String item : items) {
                if (item.equals("")) {
                    continue;
                }
                String[] values = item.split(";");
                if (values.length < 3) {
                    continue;
                }

                int oId = Integer.parseInt(values[0]);
                int count = Integer.parseInt(values[1]);
                int price = Integer.parseInt(values[2]);

                L2ItemInstance itemToSell = getInventory().getItemByObjectId(oId);

                if (count < 1 || itemToSell == null) {
                    continue;
                }

                if (count > itemToSell.getCount()) {
                    count = itemToSell.getCount();
                }

                _sellList.addItem(oId, count, price);
            }

            if (getVar("sellstorename") != null) {
                _sellList.setTitle(getVar("sellstorename"));
            }
        }
        if (getVar("buylist") != null) {
            _buyList = new TradeList(this);
            String[] items = getVar("buylist").split(":");
            for (String item : items) {
                if (item.equals("")) {
                    continue;
                }
                String[] values = item.split(";");
                if (values.length < 3) {
                    continue;
                }

                _buyList.addItem(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]));
            }

            if (getVar("buystorename") != null) {
                _buyList.setTitle(getVar("buystorename"));
            }
        }
        if (getVar("createlist") != null) {
            _createList = new L2ManufactureList();
            String[] items = getVar("createlist").split(":");
            for (String item : items) {
                if (item.equals("")) {
                    continue;
                }
                String[] values = item.split(";");
                if (values.length < 2) {
                    continue;
                }
                _createList.add(new L2ManufactureItem(Integer.parseInt(values[0]), Integer.parseInt(values[1])));
            }
            if (getVar("manufacturename") != null) {
                _createList.setStoreName(getVar("manufacturename"));
            }
        }
    }

    public void setVar(String name, String value, Connect excon) {
        if (!Config.ALT_RESTORE_OFFLINE_TRADE) {
            return;
        }

        _offtrade_items.put(name, value);

        Connect con = null;
        PreparedStatement st = null;
        try {
            if (excon == null) {
                con = L2DatabaseFactory.get();
            } else {
                con = excon;
            }

            st = con.prepareStatement("REPLACE INTO character_offline (obj_id, name, value) VALUES (?,?,?)");
            st.setInt(1, getObjectId());
            st.setString(2, name);
            st.setString(3, value);
            st.execute();
        } catch (SQLException e) {
            _log.warning("could not setVar: " + e);
        } finally {
            if (excon == null) {
                Close.CS(con, st);
            } else {
                Close.S(st);
            }
        }
    }

    public void unsetVar(String name, Connect excon) {
        if (name == null) {
            return;
        }

        if (_offtrade_items.remove(name) != null) {
            Connect con = null;
            PreparedStatement st = null;
            try {
                if (excon == null) {
                    con = L2DatabaseFactory.get();
                } else {
                    con = excon;
                }

                st = con.prepareStatement("DELETE FROM `character_offline` WHERE `obj_id`=? AND `name`=? LIMIT 1");
                st.setInt(1, getObjectId());
                st.setString(2, name);
                st.execute();
            } catch (SQLException e) {
                _log.warning("could not unsetVar: " + e);
            } finally {
                if (excon == null) {
                    Close.CS(con, st);
                } else {
                    Close.S(st);
                }
            }
        }
    }

    public void unsetVars() {
        if (!Config.ALT_RESTORE_OFFLINE_TRADE) {
            return;
        }

        _offtrade_items.clear();

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();

            st = con.prepareStatement("DELETE FROM `character_offline` WHERE `obj_id`=?");
            st.setInt(1, getObjectId());
            st.execute();
        } catch (SQLException e) {
            _log.warning("could not unsetVar: " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    public String getVar(String name) {
        return _offtrade_items.get(name);
    }

    public Map<String, String> getVars() {
        return _offtrade_items;
    }

    private void loadOfflineTrade(Connect con) {
        if (!Config.ALT_RESTORE_OFFLINE_TRADE) {
            return;
        }

        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT name, value FROM character_offline WHERE obj_id = ?");
            st.setInt(1, getObjectId());
            rs = st.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String value = Util.htmlSpecialChars(rs.getString("value"));

                _offtrade_items.put(name, value);
            }
        } catch (SQLException e) {
            _log.warning("could not restore offtrade data: " + e);
        } finally {
            Close.SR(st, rs);
        }
    }

    // Группируем
    @Override
    public boolean canSeeTarget(L2Object trg) {
        /*
         * if (Math.abs(trg.getZ() - getZ()) > 1200) return false;
         *
         * if (CustomServerData.getInstance().intersectEventZone(getX(), getY(),
         * getZ(), trg.getX(), trg.getY(), trg.getZ())) return false;
         */

        if (trg.isPlayer()) {
            if (!isGM() && getChannel() == 8 && trg.isPlayer() && trg.getChannel() != 8) {
                if (TvTEvent.isRegisteredPlayer(trg.getObjectId())) {
                    TvTEvent.onLogin(trg.getPlayer());
                } /*
                   * else {
                   * teleToClosestTown();
                   * }
                   */

                return false;
            }

            if (!isGM() && getChannel() != trg.getChannel()) {
                return false;
            }

            if ((isInOlympiadMode() || trg.isInOlympiadMode()) && trg.getOlympiadGameId() != getOlympiadGameId()) {
                return false;
            }
        }
        return super.canSeeTarget(trg);
    }

    public boolean canTarget(L2Object trg) {
        /*
         * if (Math.abs(trg.getZ() - getZ()) > 1200) return false;
         *
         * if (CustomServerData.getInstance().intersectEventZone(getX(), getY(),
         * getZ(), trg.getX(), trg.getY(), trg.getZ())) return false;
         */

        if (trg.isPlayer()) {
            if (!isGM() && getChannel() == 8 && trg.isPlayer() && trg.getChannel() != 8) {
                if (TvTEvent.isRegisteredPlayer(trg.getObjectId())) {
                    TvTEvent.onLogin(trg.getPlayer());
                } /*
                   * else {
                   * trg.teleToClosestTown();
                   * }
                   */

                return false;
            }

            if (!isGM() && getChannel() != trg.getChannel()) {
                return false;
            }

            if ((isInOlympiadMode() || trg.isInOlympiadMode()) && trg.getOlympiadGameId() != getOlympiadGameId()) {
                return false;
            }
        }
        return true;
    }

    // зареган на ивенте
    public boolean isInEvent() {
        return EventManager.getInstance().onEvent(this);
    }

    // ламмгвардддд
    public void saveHWID(boolean f) {
        LoginServerThread.getInstance().setHwid(getAccountName(), (f ? getHWID() : ""));
        _client.setMyHWID(f ? getHWID() : "none");
    }
    // мой хвид

    public String getMyHWID() {
        if (_client == null) {
            return "none";
        }

        return _client.getMyHWID();
    }
    // полученный

    public String getHWID() {
        if (_client == null) {
            return "none";
        }

        return _client.getHWID();
    }

    // пароль
    public boolean updatePassword(String pass) {
        String newpass = Util.getSHA1(pass);
        if (newpass.length() < 5) {
            return false;
        }

        LoginServerThread.getInstance().setNewPassword(getAccountName(), newpass);
        return true;
    }
    // email

    public boolean updateEmail(String email) {
        LoginServerThread.getInstance().setNewEmail(getAccountName(), email);
        return true;
    }

    // антифлуд на повторные фразы
    private int _lastSayCount = 0;
    private long _lastSayTime = 0;
    private String _lastSayString = "";

    public void setLastSay(String text) {
        _lastSayString = text;
    }

    public String getLastSay() {
        return _lastSayString;
    }

    public boolean identSay(String text) {
        if (text.startsWith(".")) {
            return false;
        }

        if (text.equalsIgnoreCase(_lastSayString)) {
            if (System.currentTimeMillis() < _lastSayTime) {
                _lastSayCount += 1;
            }

            _lastSayTime = System.currentTimeMillis() + Config.PROTECT_SAY_INTERVAL;

            if (_lastSayCount >= Config.PROTECT_SAY_COUNT) {
                _lastSayTime = 0;
                _lastSayCount = 0;
                setChatBanned(true, Config.PROTECT_SAY_BAN, "");
            }
            return true;
        }
        return false;
    }

    // аугмент
    private int _activeAug = 0;

    public void setActiveAug(int aug) {
        _activeAug = aug;
    }

    public int getActiveAug() {
        return _activeAug;
    }

    // перенос аугментации альтб, отдельные методы, дабы избежать багов с нпц
    // переносом
    private int _trans1item = 0;
    private int _trans2item = 0;
    private int _transAugId = 0;

    public void setTrans1Item(int objId) {
        _trans1item = objId;
    }

    public void setTrans2Item(int objId) {
        _trans2item = objId;
    }

    public void setTransAugment(int id) {
        _transAugId = id;
    }

    public int getTrans1Item() {
        return _trans1item;
    }

    public int getTrans2Item() {
        return _trans2item;
    }

    public int getTransAugment() {
        return _transAugId;
    }

    // бафф таргет
    private L2Character _buffTarget = this;

    public void setBuffTarget(L2Character cha) {
        _buffTarget = cha;
    }

    public L2Character getBuffTarget() {
        if (_summon == null) {
            _buffTarget = this;
        }

        return _buffTarget;
    }

    // instanceof L2PcInstance
    @Override
    public boolean isPlayer() {
        return true;
    }

    /**
     * formulas
     */
    @Override
    public double calcMDefMod(double value) {
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null) {
            value -= 5;
        }
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null) {
            value -= 5;
        }
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null) {
            value -= 9;
        }
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_REAR) != null) {
            value -= 9;
        }
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_NECK) != null) {
            value -= 13;
        }

        return value;
    }

    @Override
    public double calcPDefMod(double value) {
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null) {
            value -= 12;
        }
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST) != null) {
            value -= ((getClassId().isMage()) ? 15 : 31);
        }
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null) {
            value -= ((getClassId().isMage()) ? 8 : 18);
        }
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null) {
            value -= 8;
        }
        if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_FEET) != null) {
            value -= 7;
        }

        return value;
    }

    @Override
    public double calcAtkCritical(double value, double dex) {
        if (getActiveWeaponInstance() == null) {
            return 40;
        }

        value *= dex * 10;
        return value;
    }

    @Override
    public double calcMAtkCritical(double value, double wit) {
        if (getActiveWeaponInstance() == null) {
            return 8;
        }

        return (value * wit);
    }

    @Override
    public double calcBlowDamageMul() {
        L2Armor armor = getActiveChestArmorItem();
        if (armor != null) {
            if (isWearingHeavyArmor()) {
                return Config.BLOW_DAMAGE_HEAVY;
            }
            if (isWearingLightArmor()) {
                return Config.BLOW_DAMAGE_LIGHT;
            }
            if (isWearingMagicArmor()) {
                return Config.BLOW_DAMAGE_ROBE;
            }
        }
        return 1;
    }

    //
    @Override
    public boolean isOverlord() {
        return (getClassId().getId() == 51 || getClassId().getId() == 115);
    }

    //
    @Override
    public void olympiadClear() {
        Olympiad.removeNobleIp(this, true);
    }

    //
    @Override
    public void getEffect(int id, int lvl) {
        stopSkillEffects(id);
        SkillTable.getInstance().getInfo(id, lvl).getEffects(this, this);
    }

    //
    @Override
    public boolean canExp() {
        if (getLevel() >= Config.MAX_EXP_LEVEL) {
            return false;
        }

        return super.canExp();
    }

    //
    @Override
    public L2PcInstance getPlayer() {
        return this;
    }

    //
    private boolean _showSoulshotsAnim = true;

    public void setSoulShotsAnim(boolean f) {
        _showSoulshotsAnim = f;
    }

    @Override
    public boolean showSoulShotsAnim() {
        return _showSoulshotsAnim;
    }

    //
    private long _lastTeleport = 0;

    @Override
    public void updateLastTeleport(boolean f) {
        if (Config.TELEPORT_PROTECTION == 0) {
            return;
        }

        if (f) {
            _lastTeleport = System.currentTimeMillis() + Config.TELEPORT_PROTECTION;
            return;
        }

        if (isProtected()) {
            sendMessage("Теперь на вас могут напасть.");
        }

        _lastTeleport = 0;
    }

    public boolean isProtected() {
        if (Config.TELEPORT_PROTECTION == 0) {
            return false;
        }

        if (Config.TVT_TELE_PROTECT > 0 && isTvtProtected()) {
            return true;
        }

        return System.currentTimeMillis() < _lastTeleport;
    }

    //
    private int _accKickCount = 0;

    public void incAccKickCount() {
        _accKickCount++;
    }

    public int getAccKickCount() {
        return _accKickCount;
    }

    //
    public void changeName(String name) {
        setName(name);
        store();

        sendAdmResultMessage("Ваш ник изменен на " + name);

        if (_clan != null) {
            _clan.updateClanMember(this, true);
        }

        if (_party != null) {
            _party.updateMembers();
        }

        // L2World.getInstance().updatePlayer(getObjectId(), this);
        if (!isGM()) {
            setChannel(1);
        }

        teleToLocation(getX(), getY(), getZ());
        // broadcastUserInfo();
    }

    public boolean hasEmail() {
        return getClient().hasEmail();
    }

    //
    private int _fakeProtect = 0;

    @Override
    public boolean rndWalk(L2Character target, boolean fake) {
        rndWalk();
        if (_fakeProtect > 2) {
            /*
             * if (target.getTarget() != null) {
             * target.getTarget().getPlayer().kick(); }
             */
            // _fakeProtect = 0;
            return false;
        }
        _fakeProtect++;
        return true;
    }

    @Override
    public void clearRndWalk() {
        // _fakeProtect = 0;
    }

    @Override
    public int getPAtk(L2Character target) {
        if (isFantome()) {
            return getAI().getPAtk();
        }
        return super.getPAtk(target);
    }

    @Override
    public double getMDef() {
        return getMDef(null, null);
    }

    @Override
    public int getMDef(L2Character target, L2Skill skill) {
        if (isFantome()) {
            return getAI().getMDef();
        }
        return super.getMDef(target, skill);
    }

    @Override
    public int getPAtkSpd() {
        if (isFantome()) {
            return getAI().getPAtkSpd();
        }
        return super.getPAtkSpd();
    }

    @Override
    public int getPDef(L2Character target) {
        if (isFantome()) {
            return getAI().getPDef();
        }
        return super.getPDef(target);
    }

    @Override
    public double getMAtk() {
        if (isFantome()) {
            return getAI().getMAtk();
        }
        return ((double) getStat().getMAtk(null, null));
    }

    @Override
    public int getMAtkSpd() {
        if (isFantome()) {
            return getAI().getMAtkSpd();
        }
        return super.getMAtkSpd();
    }

    @Override
    public int getMaxHp() {
        if (isFantome()) {
            return getAI().getMaxHp();
        }
        return super.getMaxHp();
    }

    @Override
    public int getRunSpeed() {
        if (_isPartner) {
            if (_owner != null) {
                return _owner.getRunSpeed();
            }
        }
        return super.getRunSpeed();
    }

    public void despawnMe() {
        if (getWorldRegion() != null) {
            getWorldRegion().removeFromZones(this);
            getWorldRegion().removeVisibleObject(this);
        }

        decayMe();
        getKnownList().removeAllKnownObjects();
        setOnlineStatus(false);
        L2World.getInstance().removeVisibleObject(this, this.getWorldRegion());
        L2World.getInstance().removePlayer(this);
        if (_owner != null) {
            _owner.setPartner(null);
        }
    }

    @Override
    public boolean teleToLocation(Location loc) {
        if (_pvpArena || loc.x == 0) {
            return false;
        }

        teleToLocation(loc.x, loc.y, loc.z, false);
        return true;
    }

    public boolean ignoreBuffer() {
        if (Config.BUFFER_ALLOW_PEACE && PeaceZone.getInstance().inPeace(this)) {
            return false;
        }
        if (underAttack()) {
            sendHtmlMessage("Баффер", "Во время боя не баффаю.");
            sendActionFailed();
            return true;
        }
        return false;
    }

    private boolean _isHippy = false;

    @Override
    public void setHippy(boolean hippy) {
        _isHippy = hippy;
    }

    @Override
    public boolean isHippy() {
        return _isHippy;
    }

    @Override
    public boolean isInEventChannel() {
        return (_channel > 3 && _channel < 60);
    }

    public boolean isFullyRestored() {
        if (getCurrentCp() < getMaxCp()) {
            return false;
        }

        if (getCurrentHp() < getMaxHp()) {
            return false;
        }

        if (getCurrentMp() < getMaxMp()) {
            return false;
        }

        return true;
    }

    public boolean canMountPet(L2Summon pet) {
        if (pet == null || !pet.isMountable()) {
            return false;
        }

        if (isMounted()/*
                        * || isMounting()
                        */ || isBetrayed()) {
            return false;
        }

        if (isInEncounterEvent() || getKnownList().existsDoorsInRadius(169)) {
            return false;
        }

        if (isOutOfControl() || isParalyzed()) {
            return false;
        }
        if (isInsideDismountZone()) {
            return false;
        }

        SystemMessage sm = null;
        if (isDead()) {
            sm = Static.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD; // A strider cannot be ridden when dead
        } else if (pet.isDead()) {
            sm = Static.DEAD_STRIDER_CANT_BE_RIDDEN; // A dead strider cannot be ridden.
        } else if (pet.isInCombat() || pet.isRooted()) {
            sm = Static.STRIDER_IN_BATLLE_CANT_BE_RIDDEN; // A strider in battle cannot be ridden
        } else if (isInCombat()) {
            sm = Static.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE; // A strider cannot be ridden while in battle
        } else if (isSitting() || isMoving()) {
            sm = Static.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING; // A strider can be ridden only when standing
        } else if (isFishing()) {
            sm = Static.CANNOT_DO_WHILE_FISHING_2; // You can't mount, dismount, break and drop items while fishing
        } else if (isCursedWeaponEquiped()) {
            sm = Static.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE; // You can't mount, dismount, break and drop items while
                                                                // weilding a cursed weapon
        }
        if (sm != null) {
            sendPacket(sm);
            sm = null;
            return false;
        }

        if (!disarmWeapons()) {
            return false;
        }

        if (!canSeeTarget(pet)) {
            return false;
        }

        return true;
    }

    public void tryMountPet(L2Summon pet) {
        // sendAdmResultMessage("##tryMountPet###1#");
        if (canMountPet(pet)) {
            abortCast();
            // sendAdmResultMessage("##tryMountPet###2#");
            useSkill(SkillTable.getInstance().getInfo(5099, 1));
            // sendAdmResultMessage("##tryMountPet###3#");
        } else if (isRentedPet()) {
            stopRentPet();
        } else if (isMounted()) {
            tryUnmounPet(pet);
        }
    }

    public void useSkill(L2Skill skill) {
        if (skill.checkCondition(this, this, false)) {
            useMagic(skill, false, false);
        } else {
            sendActionFailed();
        }
    }

    public void tryUnmounPet(L2Summon pet) {
        if (setMountType(0)) {
            if (isFlying()) {
                removeSkill(SkillTable.getInstance().getInfo(327, 1));
                removeSkill(SkillTable.getInstance().getInfo(4289, 1));
            }
            broadcastPacket(new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0));
            setMountObjectID(0);
            broadcastUserInfo();
        }
    }

    private L2ItemInstance _petSummon = null;

    public void setPetSummon(L2ItemInstance petSummon) {
        _petSummon = petSummon;
    }

    public L2ItemInstance getPetSummon() {
        return _petSummon;
    }

    @Override
    public boolean isBlockingBuffs() {
        if (getFirstEffect(1411) != null || isCursedWeaponEquiped()) {
            return true;
        }

        if (isInOlympiadMode() && !Config.OLY_ANTI_BUFF) {
            return false;
        }

        return getFirstEffect(Config.ANTIBUFF_SKILLID) != null;
    }

    @Override
    public void sendSkillCoolTime() {
        sendSkillCoolTime(false);
    }

    @Override
    public void sendSkillCoolTime(boolean force) {
        /*
         * if (!force) {
         * if (System.currentTimeMillis() - gCPAT() < 5000) {
         * return;
         * }
         * 
         * sCPAT();
         * }
         */

        sendUserPacket(new SkillCoolTime(this, getDisabledSkills()));
    }

    @Override
    public final boolean isNoblesseBlessed() {
        return getFirstEffect(1323) != null || (calcStat(Stats.NOBLE_BLESS, 0, null, null) > 0) || _saveBuff;
    }

    @Override
    public final boolean isPhoenixBlessed() {
        if (Config.PVP_ZONE_REWARDS && ZoneManager.getInstance().isResDisabled(this)) {
            sendPacket(Static.RES_DISABLED);
            return false;
        }
        return (calcStat(Stats.PHOENIX_BLESS, 0, null, null) > 0)
                || (getFirstEffect(438) != null || getFirstEffect(1410) != null);
    }

    private boolean _freeArena = false;

    @Override
    public void setFreeArena(boolean free) {
        _freeArena = free;
    }

    @Override
    public boolean isInFreeArena() {
        return _freeArena;
    }

    private boolean _saveBuff = false;

    @Override
    public void setSaveBuff(boolean f) {
        _saveBuff = true;
    }

    private boolean _fishZone = false;

    @Override
    public void setInFishZone(boolean f) {
        _fishZone = f;
    }

    @Override
    public boolean isInFishZone() {
        return _fishZone;
    }

    private int _soulCryId = 0;

    public void setCoulCryId(int id) {
        _soulCryId = id;
    }

    public int getCoulCryId() {
        return _soulCryId;
    }

    private int _guildSide = 0;

    public void setGuildSide(int id) {
        _guildSide = id;
    }

    public int getGuildSide() {
        return _guildSide;
    }

    private int _guildPenalty = 0;

    public void setGuildPenalty(int id) {
        _guildPenalty = id;
    }

    public void incGuildPenalty() {
        _guildPenalty++;
        if (_guildPenalty >= Config.GUILD_MOD_PENALTYS_MAX) {
            setGuildSide(0);
            setGuildPenalty(0);
            destroyItemByItemId("incGuildPenalty", Config.GUILD_MOD_MASKS.get(_guildSide), 1, this, true);
            Connect con = null;
            PreparedStatement st = null;
            try {
                con = L2DatabaseFactory.get();
                st = con.prepareStatement("DELETE FROM `z_guild_mod` WHERE `char_id`=?");
                st.setInt(1, getObjectId());
                st.execute();
            } catch (SQLException e) {
                _log.severe("Could not delete z_guild_mod: " + e);
            } finally {
                Close.CS(con, st);
            }
        }
    }

    public int getGuildPenalty() {
        return _guildPenalty;
    }

    public boolean isInGuild() {
        if (!Config.ALLOW_GUILD_MOD) {
            return false;
        }

        return _guildSide > 0;
    }

    public int isGuildEnemyFor(L2PcInstance target) {
        if (!target.isInGuild()) {
            return 0;
        }

        if (target.getGuildSide() == _guildSide) {
            return 2;
        }

        return 1;
    }

    private void addGuildBonus() {
        EventReward reward = null;
        for (FastList.Node<EventReward> k = Config.GUILD_MOD_REWARDS.head(),
                endk = Config.GUILD_MOD_REWARDS.tail(); (k = k.getNext()) != endk;) {
            reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                addItem("pk_bonus", reward.id, reward.count, this, true);
            }
        }
        reward = null;
    }

    public boolean changeSub(L2VillageMasterInstance master, int sub) {
        if (System.currentTimeMillis() - gCPBF() < Config.CHANGE_SUB_DELAY) {
            sendPacket(Static.PLEASE_WAIT);
            return false;
        }
        sCPBF();

        if (!canChangeSub(master)) {
            return false;
        }

        if (getFairy() != null) {
            getFairy().unSummon(this);
        }

        abortAttack();
        abortCast();
        setIsParalyzed(true);

        setActiveClass(sub);
        setCurrentCp(getMaxCp());
        setCurrentHp(getMaxHp());
        setCurrentMp(getMaxMp());
        stopSkillEffects(426);
        stopSkillEffects(427);
        stopSkillEffects(5104);
        stopSkillEffects(5105);
        setIsParalyzed(false);
        checkAllowedSkills();
        sendChanges();
        return true;
    }

    public boolean canChangeSub(L2VillageMasterInstance master) {
        if (getTarget() != master) {
            sendUserPacket(Static.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
            return false;
        }

        if (isCastingNow() || isAllSkillsDisabled() || isAttackingNow()) {
            sendUserPacket(Static.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
            return false;
        }

        if (getPet() != null) {
            sendUserPacket(Static.CANT_CHANGE_SUB_SUMMON);
            return false;
        }

        if (isDead() || isAlikeDead()) {
            sendUserPacket(Static.OOPS_ERROR);
            return false;
        }

        if (Olympiad.isRegisteredInComp(this) || isInOlympiadMode() || getOlympiadGameId() > -1) {
            sendUserPacket(Static.OOPS_ERROR);
            return false;
        }

        if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(getName())) {
            sendUserPacket(Static.OOPS_ERROR);
            return false;
        }

        if (inFClub()) {
            sendUserPacket(Static.OOPS_ERROR);
            return false;
        }

        abortAttack();
        abortCast();
        return true;
    }

    public boolean canSeeBroadcast() {
        if (isCastingNow() || isAllSkillsDisabled() || isAttackingNow()) {
            sendHtmlMessage("Произошла ошибка при попытке просмотра.");
            return false;
        }
        if (isDead() || isAlikeDead()) {
            sendHtmlMessage("Произошла ошибка при попытке просмотра.");
            return false;
        }

        if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(getName())) {
            sendHtmlMessage("Вы зарегистрирвоаны на ивенте.");
            return false;
        }

        if (inFClub()) {
            sendHtmlMessage("Вы зарегистрирвоаны на ивенте.");
            return false;
        }

        if (getPet() != null) {
            sendHtmlMessage("Отзовите суммона.");
            return false;
        }

        if (Olympiad.isRegisteredInComp(this) || isInOlympiadMode() || getOlympiadGameId() > -1) {
            sendHtmlMessage("Вы зарегистрирвоаны на олимпиаде.");
            return false;
        }

        if (isInEvent()) {
            sendHtmlMessage("Вы зарегистрирвоаны на ивенте.");
            return false;
        }

        abortAttack();
        abortCast();
        return true;
    }

    private int _clanNpcScore = 0;

    public void setClanNpcScore(int score) {
        _clanNpcScore = score;
    }

    public void updateClanNpcScore(int score) {
        _clanNpcScore += score;
        sendMessage("Теперь у вас " + _clanNpcScore + " клан очков.");
    }

    public int getClanNpcScore() {
        return _clanNpcScore;
    }

    private int _kewkeBonus = 0;

    public void setWeekBonus(int score) {
        _kewkeBonus = score;
    }

    public int getWeekBonus() {
        return _kewkeBonus;
    }

    private boolean _isBuffing = false;

    @Override
    public void setBuffing(boolean f) {
        _isBuffing = f;
    }

    private long _lastStriderSummon = 0;

    public boolean canSummonStrider() {
        if (_lastStriderSummon > System.currentTimeMillis()) {
            return false;
        }

        _lastStriderSummon = System.currentTimeMillis() + Config.SUMMON_DELAY;
        return true;
    }

    //
    private int _forbItem = 0;

    public void setForbItem(int itemId) {
        _forbItem = itemId;
    }

    public int getForbItem() {
        return _forbItem;
    }

    @Override
    public boolean checkForbiddenItems() {
        if (_forbItem == 0) {
            return false;
        }
        // return (hasForbiddenItem(getInventory().getItemByItemId(_forbItem)));
        return CustomServerData.getInstance().isSpecialTeleDenied(this, _forbItem);
    }

    private FarmDelay _farmDelay;

    public void showFarmPenalty() {
        _farmDelay.showPenalty();
    }

    public void clearFarmPenalty() {
        _farmDelay.clear();
    }

    @Override
    public boolean hasFarmPenalty() {
        return _farmDelay.hasPenalty();
    }

    public int getFarmLesson() {
        return _farmDelay.getLesson();
    }

    /*
     * private boolean hasForbiddenItem(L2ItemInstance item) {
     * if (item == null) {
     * return false;
     * }
     * 
     * return item.isEquipped();
     * }
     */
    private boolean _isInNoLogoutArea = false;
    private boolean _overB = true;

    @Override
    public void setInNoLogoutArea(boolean f) {
        _isInNoLogoutArea = f;
    }

    public boolean isInNoLogoutArea() {
        return _isInNoLogoutArea;
    }

    private String _pvpTitle = "";

    public void checkPvpTitle() {
        if (!Config.PVP_TITLE_SKILL || Config.PVPBONUS_TITLE.isEmpty()) {
            return;
        }

        String title = "";
        L2Skill skill = null;

        for (PvpTitleBonus bonus : Config.PVPBONUS_TITLE) {
            if (bonus == null) {
                continue;
            }

            if (!bonus.isInRange(getPvpKills())) {
                removeSkill(SkillTable.getInstance().getInfo(bonus.skillId, bonus.skillLvl), false);
                continue;
            }

            title = bonus.title;
            skill = SkillTable.getInstance().getInfo(bonus.skillId, bonus.skillLvl);
        }
        _pvpTitle = title;
        addSkill(skill, false);
    }

    //
    private boolean _spamer = false;

    public void markSpamer() {
        _spamer = true;
    }

    public boolean isSpamer() {
        return _spamer;
    }

    private String _lastMessPM = "none";
    private long _lastMessTimePM = 0;
    private int _lastMessCountPM = 0;

    public void setLastMessPM(String text) {
        _lastMessPM = text;
        _lastMessTimePM = System.currentTimeMillis() + Config.SAY_PM_INTERVAL;
    }

    public String getLastMessPM() {
        if (System.currentTimeMillis() > _lastMessTimePM) {
            _lastMessPM = "";
        }
        return _lastMessPM;
    }

    public long getLastMessTimePM() {
        return _lastMessTimePM;
    }

    public void updateLastMessPm() {
        _lastMessCountPM++;
        if (_lastMessCountPM > Config.SAY_PM_COUNT) {
            markSpamer();
        }
    }

    public int getTvtKills() {
        if (!Config.TVT_SHOW_KILLS) {
            return 0;
        }
        return TvTEvent.getTvtKills(getObjectId());
    }

    //
    private String _hwid;

    public String getHWid() {
        if (getClient() == null) {
            return _hwid;
        }
        _hwid = getClient().getHWid();
        return _hwid;
    }

    public String getLastHwId() {
        return this._hwid;
    }

    //
    private boolean _partyExitPenalty = false;
    private boolean _logoutPenalty = false;
    private boolean _exitPenalty = false;
    private boolean _summonPenalty = false;

    public void setPartyExitPenalty(boolean f) {
        _partyExitPenalty = f;
    }

    public boolean hasPartyExitPenalty() {
        return _partyExitPenalty;
    }

    public void setLogoutPenalty(boolean f) {
        _logoutPenalty = f;
    }

    public boolean hasLogoutPenalty() {
        return _logoutPenalty;
    }

    public void setExitPenalty(boolean f) {
        _exitPenalty = f;
    }

    public boolean hasExitPenalty() {
        return _exitPenalty;
    }

    @Override
    public void setSummonPenalty(boolean f) {
        _summonPenalty = f;
    }

    public boolean hasSummonPenalty() {
        return _summonPenalty;
    }

    public boolean isShowPopup() {
        return false;
    }

    //
    private long _tvtProtection = 0;

    public void setTvtProtection(int time) {
        _tvtProtection = System.currentTimeMillis() + time;
    }

    public boolean isTvtProtected() {
        return _tvtProtection > System.currentTimeMillis();
    }

    //
    private FastList<Integer> _forbItems = new FastList<Integer>();

    @Override
    public void setFordItems(FastList<Integer> items) {
        _forbItems = items;
    }

    public boolean isForbidItem(int id) {
        return _forbItems.contains(id);
    }

    //
    public int getLastServerId() {
        if (_client == null) {
            return -1;
        }
        return _client.getLastServerId();
    }

    // multisell
    private int _multsellId = 0;

    public void setMultListId(int id) {
        _multsellId = id;
    }

    public int getMultListId() {
        return _multsellId;
    }

    //
    private boolean _freePk = false;

    @Override
    public void setFreePk(boolean free) {
        _freePk = free;
    }

    @Override
    public boolean isInFreePk() {
        return _freePk;
    }

    //
    private boolean _tvtReward = false;

    @Override
    public void enableTvtReward() {
        _tvtReward = true;
    }

    public boolean isEnabledTvtReward() {
        return _tvtReward;
    }

    public void restoreEventSkills() {
        for (L2Skill s : getAllSkills()) {
            if (s == null) {
                continue;
            }
            if (s.isForbidEvent()) {
                removeSkill(s, false);
                addSkill(s, false);
            }
        }
    }

    // _freightTarget
    private int _freightTarget = 0;

    public void setFreightTarget(int id) {
        _freightTarget = id;
    }

    public int getFreightTarget() {
        return _freightTarget;
    }

    private Point3D _lastClientPosition = new Point3D(0, 0, 0);
    private Point3D _lastServerPosition = new Point3D(0, 0, 0);

    public void setLastClientPosition(int x, int y, int z) {
        _lastClientPosition.setXYZ(x, y, z);
    }

    public boolean checkLastClientPosition(int x, int y, int z) {
        return _lastClientPosition.equals(x, y, z);
    }

    public int getLastClientDistance(int x, int y, int z) {
        double dx = (x - _lastClientPosition.getX());
        double dy = (y - _lastClientPosition.getY());
        double dz = (z - _lastClientPosition.getZ());

        return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void setLastServerPosition(int x, int y, int z) {
        _lastServerPosition.setXYZ(x, y, z);
    }

    public boolean checkLastServerPosition(int x, int y, int z) {
        return _lastServerPosition.equals(x, y, z);
    }

    public int getLastServerDistance(int x, int y, int z) {
        double dx = (x - _lastServerPosition.getX());
        double dy = (y - _lastServerPosition.getY());
        double dz = (z - _lastServerPosition.getZ());

        return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    //
    private boolean _autoMp = false;

    public void setAutoMp(boolean f) {
        _autoMp = f;
        sendPacket(new ExAutoSoulShot(728, _autoMp ? 1 : 0));
        sendMessage("Автоматическое использование MP " + (_autoMp ? "включено." : "отключено."));
    }

    @Override
    public boolean hasAutoMp() {
        return _autoMp;
    }

    private boolean _autoHp = false;

    public void setAutoHp(boolean f) {
        _autoHp = f;
        sendPacket(new ExAutoSoulShot(1539, _autoHp ? 1 : 0));
        sendMessage("Автоматическое использование HP " + (_autoHp ? "включено." : "отключено."));
    }

    @Override
    public boolean hasAutoHp() {
        return _autoHp;
    }

    private boolean _autoCp = false;

    public void setAutoCp(boolean f) {
        _autoCp = f;
        sendPacket(new ExAutoSoulShot(5592, _autoCp ? 1 : 0));
        sendMessage("Автоматическое использование CP " + (_autoCp ? "включено." : "отключено."));
    }

    @Override
    public boolean hasAutoCp() {
        return _autoCp;
    }

    public void setACP(boolean f) {
        setAutoMp(f);
        setAutoHp(f);
        setAutoCp(f);
    }

    @Override
    public void useAutoItem(int itemId) {
        if (isInEvent()) {
            setAutoMp(false);
            setAutoHp(false);
            setAutoCp(false);
            return;
        }

        switch (itemId) {
            case 1539:
                if (isSkillDisabled(2037)) {
                    return;
                }
                L2Effect potion = getFirstEffect(2037);
                if (potion != null) {
                    if (potion.getTaskTime() > (potion.getSkill().getBuffDuration() * 67) / 100000) {
                        if (canPotion()) {
                            clearPotions();
                        }
                        potion.exit();
                    } else {
                        return;
                    }
                }
                break;
            case 5592:
                if (getCpReuseTime(itemId) < Config.CP_REUSE_TIME) {
                    return;
                }
                break;
        }

        L2ItemInstance item = getInventory().getItemByItemId(itemId);
        if (item == null) {
            switch (itemId) {
                case 728:
                    setAutoMp(false);
                    break;
                case 1539:
                    setAutoHp(false);
                    break;
                case 5592:
                    setAutoCp(false);
                    break;
            }
            return;
        }

        IItemHandler handler = ItemHandler.getInstance().getItemHandler(itemId);
        if (handler == null) {
            switch (itemId) {
                case 728:
                    setAutoMp(false);
                    break;
                case 1539:
                    setAutoHp(false);
                    break;
                case 5592:
                    setAutoCp(false);
                    break;
            }
            return;
        }

        handler.useItem(this, item, false);
    }

    private Future<?> _acpTask;

    public void notifyACP() {
        if (!Config.ACP_ENGINE) {
            return;
        }

        setAutoHp(!hasAutoHp());
        setAutoMp(!hasAutoMp());
        setAutoCp(!hasAutoCp());

        if (_acpTask == null) {
            _acpTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AcpTask(), Config.ACP_DELAY,
                    Config.ACP_DELAY);
        } else {
            _acpTask.cancel(false);
            _acpTask = null;
        }
    }

    class AcpTask implements Runnable {

        @Override
        public void run() {
            try {

                if (Config.ACP_ENGINE) {
                    if (hasAutoMp()) {
                        if (getCurrentMp() <= ((getStat().getMaxMp() / 100) * Config.ACP_MP)) {
                            useAutoItem(728);
                        }
                    }
                    if (hasAutoHp()) {
                        if (getCurrentHp() <= ((getStat().getMaxHp() / 100) * Config.ACP_HP)) {
                            useAutoItem(1539);
                        }
                    }
                    if (hasAutoCp()) {
                        if (getCurrentCp() <= ((getStat().getMaxCp() / 100) * Config.ACP_CP)) {
                            useAutoItem(5592);
                        }
                    }
                }

            } catch (Throwable e) {
                _log.log(Level.SEVERE, "class AcpTask implements Runnable", e);
                e.printStackTrace();
            }
        }
    }

    public void doEventBuff() {
        if (Config.ALT_HENNA_OLY_EVENT_DISABLE
                && getChannel() > 1) {
            recalcHennaStats();
            sendPacket(new HennaInfo(this));
        }

        if (!Config.ALT_EVENT_BUFFS) {
            return;
        }

        FastMap<Integer, Integer> buffs = null;
        if (isMageClass()) {
            buffs = Config.ALT_EVENT_BUFFS_MAGE;
        } else {
            buffs = Config.ALT_EVENT_BUFFS_FIGHTER;
        }

        stopAllEffects();
        setBuffing(true);

        Integer id = null;
        Integer lvl = null;
        SkillTable _st = SkillTable.getInstance();
        for (FastMap.Entry<Integer, Integer> e = buffs.head(), end = buffs.tail(); (e = e.getNext()) != end;) {
            id = e.getKey(); // No typecast necessary.
            lvl = e.getValue(); // No typecast necessary.
            if (id == null || lvl == null) {
                continue;
            }

            _st.getInfo(id, lvl).getEffects(this, this);
        }

        setBuffing(false);
        updateEffectIcons();
    }

    //
    private long _lastHit = 0;

    @Override
    public void setLastHit() {
        _lastHit = System.currentTimeMillis() + 30000;
    }

    public long getLastHit() {
        return _lastHit;
    }
}
