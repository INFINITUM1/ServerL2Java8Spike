package net.sf.l2j;

//can't stop me now
import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.EventTerritory;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.TimeLogger;
import net.sf.l2j.util.log.AbstractLogger;

public final class Config {

    protected static final Logger _log = Logger.getLogger(Config.class.getName());
    // Configuration files
    /**
     * Properties file that allows selection of new Classes for storage of World
     * Objects. /** Properties file for game server (connection and ingame)
     * configurations
     */
    public static final String CONFIGURATION_FILE = "./config/server.cfg";
    /**
     * Properties file for game server options
     */
    public static final String OPTIONS_FILE = "./config/options.cfg";
    /**
     * Properties file for login server configurations
     */
    public static final String LOGIN_CONFIGURATION_FILE = "./config/loginserver.cfg";
    /**
     * Properties file for the ID factory
     */
    public static final String ID_CONFIG_FILE = "./config/idfactory.cfg";
    /**
     * Properties file for other configurations
     */
    public static final String OTHER_CONFIG_FILE = "./config/other.cfg";
    /**
     * Properties file for rates configurations
     */
    public static final String RATES_CONFIG_FILE = "./config/rates.cfg";
    /**
     * Properties file for alternative configuration
     */
    public static final String ALT_SETTINGS_FILE = "./config/altsettings.cfg";
    /**
     * Properties file for PVP configurations
     */
    public static final String PVP_CONFIG_FILE = "./config/pvp.cfg";
    /**
     * Properties file for GM access configurations
     */
    public static final String GM_ACCESS_FILE = "./config/GMAccess.cfg";
    /**
     * Properties file for telnet configuration
     */
    public static final String TELNET_FILE = "./config/telnet.cfg";
    /**
     * Properties file for siege configuration
     */
    public static final String SIEGE_CONFIGURATION_FILE = "./config/siege.cfg";
    /**
     * XML file for banned IP
     */
    public static final String BANNED_IP_XML = "./config/banned.xml";
    /**
     * Text file containing hexadecimal value of server ID
     */
    public static final String HEXID_FILE = "./config/hexid.txt";
    /**
     * Properties file for alternative configure GM commands access level.<br>
     * Note that this file only read if "AltPrivilegesAdmin = True"
     */
    public static final String COMMAND_PRIVILEGES_FILE = "./config/command-privileges.cfg";
    /**
     * Properties file for AI configurations
     */
    public static final String AI_FILE = "./config/ai.cfg";
    /**
     * Properties file for 7 Signs Festival
     */
    public static final String SEVENSIGNS_FILE = "./config/sevensigns.cfg";
    public static final String CLANHALL_CONFIG_FILE = "./config/clanhall.cfg";
    public static final String NPC_CONFIG_FILE = "./config/npc.cfg";
    public static final String CUSTOM_CONFIG_FILE = "./config/custom.cfg";
    public static final String ENCHANT_CONFIG_FILE = "./config/enchants.cfg";
    public static final String CMD_CONFIG_FILE = "./config/commands.cfg";
    /**
     * Word list for chat filter
     */
    public static boolean MULTILANG_ENABLE;
    public static String MULTILANG_DEFAULT;
    public static List<String> MULTILANG_ALLOWED;

    public static final String CHAT_FILTER_FILE = "./config/chatfilter.txt";
    public static final String GEO_FILE = "./config/geodata.cfg";
    public static final String FAKE_FILE = "./config/fakeplayers.cfg";
    public static final String GAME_GUARD_FILE = "./config/protection.cfg";
    public static boolean DEBUG;
    /**
     * Enable/disable assertions
     */
    public static boolean ASSERT;
    /**
     * Enable/disable code 'in progress'
     */
    public static boolean DEVELOPER;
    /**
     * Set if this server is a test server used for development
     */
    public static boolean TEST_SERVER;
    /**
     * Game Server ports
     */
    // public static int PORT_GAME;
    public static int[] PORTS_GAME;
    /**
     * Login Server port
     */
    public static int PORT_LOGIN;
    /**
     * Login Server bind ip
     */
    public static String LOGIN_BIND_ADDRESS;
    /**
     * Number of login tries before IP ban gets activated, default 10
     */
    public static int LOGIN_TRY_BEFORE_BAN;
    /**
     * Number of seconds the IP ban will last, default 10 minutes
     */
    public static int LOGIN_BLOCK_AFTER_BAN;
    /**
     * Hostname of the Game Server
     */
    public static String GAMESERVER_HOSTNAME;
    // Access to database
    /**
     * Driver to access to database
     */
    public static String DATABASE_DRIVER;
    /**
     * Path to access to database
     */
    public static String DATABASE_URL;
    /**
     * Database login
     */
    public static String DATABASE_LOGIN;
    /**
     * Database password
     */
    public static String DATABASE_PASSWORD;
    /**
     * Maximum number of connections to the database
     */
    public static int DATABASE_MAX_CONNECTIONS;
    public static int MINCONNECTIONSPERPARTITION;
    public static int MAXCONNECTIONSPERPARTITION;
    public static int PARTITIONCOUNT;
    public static int ACQUIREINCREMENT;
    public static int IDLECONNECTIONTESTPERIOD;
    public static int IDLEMAXAGE;
    public static int RELEASEHELPERTHREADS;
    public static int ACQUIRERETRYDELAY;
    public static int ACQUIRERETRYATTEMPTS;
    public static int QUERYEXECUTETIMELIMIT;
    public static int CONNECTIONTIMEOUT;
    public static boolean LAZYINIT;
    public static boolean TRANSACTIONRECOVERYENABLED;
    /**
     * Maximum number of players allowed to play simultaneously on server
     */
    public static int MAXIMUM_ONLINE_USERS;
    // Setting for serverList
    /**
     * Displays [] in front of server name ?
     */
    public static boolean SERVER_LIST_BRACKET;
    /**
     * Displays a clock next to the server name ?
     */
    public static boolean SERVER_LIST_CLOCK;
    /**
     * Display test server in the list of servers ?
     */
    public static boolean SERVER_LIST_TESTSERVER;
    /**
     * Set the server as gm only at startup ?
     */
    public static boolean SERVER_GMONLY;
    // Thread pools size
    /**
     * Thread pool size effect
     */
    public static int THREAD_P_EFFECTS;
    /**
     * Thread pool size general
     */
    public static int THREAD_P_GENERAL;
    /**
     * Packet max thread
     */
    public static int GENERAL_PACKET_THREAD_CORE_SIZE;
    public static int THREAD_P_PATHFIND;
    public static int THREAD_AI_EXECUTOR;
    public static int THREAD_GENERAL_EXECUTOR;
    public static int IO_PACKET_THREAD_CORE_SIZE;
    /**
     * General max thread
     */
    public static int GENERAL_THREAD_CORE_SIZE;
    /**
     * AI max thread
     */
    public static int THREADING_MODEL;
    public static int NPC_AI_MAX_THREAD;
    public static int PLAYER_AI_MAX_THREAD;
    public static int THREAD_P_MOVE;
    /**
     * Accept auto-loot ?
     */
    public static boolean AUTO_LOOT;
    public static boolean AUTO_LOOT_RAID;
    public static boolean AUTO_LOOT_HERBS;
    /**
     * Character name template
     */
    public static String CNAME_TEMPLATE;
    /**
     * Pet name template
     */
    public static String PET_NAME_TEMPLATE;
    /**
     * Maximum number of characters per account
     */
    public static int MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
    /**
     * Global chat state
     */
    public static String DEFAULT_GLOBAL_CHAT;
    /**
     * Trade chat state
     */
    public static String DEFAULT_TRADE_CHAT;
    /**
     * For test servers - everybody has admin rights
     */
    public static boolean EVERYBODY_HAS_ADMIN_RIGHTS;
    /**
     * Alternative game crafting
     */
    public static boolean ALT_GAME_CREATION;
    /**
     * Alternative game crafting speed mutiplier - default 0 (fastest but still
     * not instant)
     */
    public static double ALT_GAME_CREATION_SPEED;
    /**
     * Alternative game crafting XP rate multiplier - default 1
     */
    public static double ALT_GAME_CREATION_XP_RATE;
    /**
     * Alternative game crafting SP rate multiplier - default 1
     */
    public static double ALT_GAME_CREATION_SP_RATE;
    /**
     * Alternative setting to blacksmith use of recipes to craft - default true
     */
    public static boolean ALT_BLACKSMITH_USE_RECIPES;
    /**
     * Remove Castle circlets after clan lose his castle? - default true
     */
    public static boolean REMOVE_CASTLE_CIRCLETS;
    /**
     * Alternative game weight limit multiplier - default 1
     */
    public static double ALT_WEIGHT_LIMIT;
    // умножение маг крита
    public static double MAGIC_CRIT_EXP;
    public static double MAGIC_CRIT_EXP_OLY;
    public static double MAGIC_DAM_EXP;
    public static double MAGIC_PDEF_EXP;
    /**
     * Alternative game skill learning
     */
    public static boolean ALT_GAME_SKILL_LEARN;
    /**
     * Alternative auto skill learning
     */
    public static boolean AUTO_LEARN_SKILLS;
    /**
     * Cancel attack bow by hit
     */
    public static boolean ALT_GAME_CANCEL_BOW;
    /**
     * Cancel cast by hit
     */
    public static boolean ALT_GAME_CANCEL_CAST;
    /**
     * Alternative game - use tiredness, instead of CP
     */
    public static boolean ALT_GAME_TIREDNESS;
    public static int ALT_PARTY_RANGE;
    public static int ALT_PARTY_RANGE2;
    /**
     * Alternative shield defence
     */
    public static boolean ALT_GAME_SHIELD_BLOCKS;
    /**
     * Alternative Perfect shield defence rate
     */
    public static int ALT_PERFECT_SHLD_BLOCK;
    /**
     * Alternative game mob ATTACK AI
     */
    public static boolean ALT_GAME_MOB_ATTACK_AI;
    public static boolean ALT_MOB_AGRO_IN_PEACEZONE;
    /**
     * Alternative freight modes - Freights can be withdrawed from any village
     */
    public static boolean ALT_GAME_FREIGHTS;
    /**
     * Alternative freight modes - Sets the price value for each freightened
     * item
     */
    public static int ALT_GAME_FREIGHT_PRICE;
    /**
     * Fast or slow multiply coefficient for skill hit time
     */
    public static float ALT_GAME_SKILL_HIT_RATE;
    /**
     * Alternative gameing - loss of XP on death
     */
    public static boolean ALT_GAME_DELEVEL;
    /**
     * Alternative gameing - magic dmg failures
     */
    public static boolean ALT_GAME_MAGICFAILURES;
    /**
     * Alternative gaming - player must be in a castle-owning clan or ally to
     * sign up for Dawn.
     */
    public static boolean ALT_GAME_REQUIRE_CASTLE_DAWN;
    /**
     * Alternative gaming - allow clan-based castle ownage check rather than
     * ally-based.
     */
    public static boolean ALT_GAME_REQUIRE_CLAN_CASTLE;
    /**
     * Alternative gaming - allow free teleporting around the world.
     */
    public static int ALT_GAME_FREE_TELEPORT;
    /**
     * Disallow recommend character twice or more a day ?
     */
    public static boolean ALT_RECOMMEND;
    /**
     * Alternative gaming - allow sub-class addition without quest completion.
     */
    public static boolean ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
    public static byte MAX_SUBCLASS;
    /**
     * View npc stats/drop by shift-cliking it for nongm-players
     */
    public static boolean ALT_GAME_VIEWNPC;
    /**
     * Minimum number of player to participate in SevenSigns Festival
     */
    public static int ALT_FESTIVAL_MIN_PLAYER;
    /**
     * Maximum of player contrib during Festival
     */
    public static int ALT_MAXIMUM_PLAYER_CONTRIB;
    /**
     * Festival Manager start time.
     */
    public static long ALT_FESTIVAL_MANAGER_START;
    /**
     * Festival Length
     */
    public static long ALT_FESTIVAL_LENGTH;
    /**
     * Festival Cycle Length
     */
    public static long ALT_FESTIVAL_CYCLE_LENGTH;
    /**
     * Festival First Spawn
     */
    public static long ALT_FESTIVAL_FIRST_SPAWN;
    /**
     * Festival First Swarm
     */
    public static long ALT_FESTIVAL_FIRST_SWARM;
    /**
     * Festival Second Spawn
     */
    public static long ALT_FESTIVAL_SECOND_SPAWN;
    /**
     * Festival Second Swarm
     */
    public static long ALT_FESTIVAL_SECOND_SWARM;
    /**
     * Festival Chest Spawn
     */
    public static long ALT_FESTIVAL_CHEST_SPAWN;
    /**
     * Number of members needed to request a clan war
     */
    public static int ALT_CLAN_MEMBERS_FOR_WAR;
    /**
     * Number of days before joining a new clan
     */
    public static int ALT_CLAN_JOIN_DAYS;
    /**
     * Number of days before creating a new clan
     */
    public static int ALT_CLAN_CREATE_DAYS;
    /**
     * Number of days it takes to dissolve a clan
     */
    public static int ALT_CLAN_DISSOLVE_DAYS;
    /**
     * Number of days before joining a new alliance when clan voluntarily leave
     * an alliance
     */
    public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
    /**
     * Number of days before joining a new alliance when clan was dismissed from
     * an alliance
     */
    public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
    /**
     * Number of days before accepting a new clan for alliance when clan was
     * dismissed from an alliance
     */
    public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
    /**
     * Number of days before creating a new alliance when dissolved an alliance
     */
    public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
    // умножение репы
    public static float ALT_CLAN_REP_MUL;
    // создание клана
    public static int ALT_CLAN_CREATE_LEVEL;
    // кланрепа за фраг
    public static int ALT_CLAN_REP_WAR;
    /**
     * Alternative gaming - all new characters always are newbies.
     */
    public static boolean ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE;
    /**
     * Alternative gaming - давать ноблесс при старте?.
     */
    public static boolean ALT_GAME_NEW_CHAR_ALWAYS_IS_NOBLE;
    /**
     * уровень при старте
     */
    public static int ALT_START_LEVEL;
    /**
     * микрофон *
     */
    public static boolean ALLOW_RUPOR;
    public static int RUPOR_ID;
    /**
     * палево онлайна
     */
    public static boolean SONLINE_ANNOUNE;
    public static int SONLINE_ANNOUNCE_DELAY;
    public static boolean SONLINE_SHOW_MAXONLINE;
    public static boolean SONLINE_SHOW_MAXONLINE_DATE;
    public static boolean SONLINE_SHOW_OFFLINE;
    public static boolean SONLINE_LOGIN_ONLINE;
    public static boolean SONLINE_LOGIN_MAX;
    public static boolean SONLINE_LOGIN_DATE;
    public static boolean SONLINE_LOGIN_OFFLINE;
    public static int AUTO_ANNOUNCE_DELAY;
    public static boolean AUTO_ANNOUNCE_ALLOW;
    public static boolean ALTCLASS_MANAGER;
    /**
     * Alternative gaming - разрешить пушки с аугментацией на олимпе?.
     */
    public static boolean ALT_ALLOW_AUGMENT_ON_OLYMP;
    /**
     * Alternative gaming - разрешить оффтрейд?
     */
    public static boolean ALT_ALLOW_OFFLINE_TRADE;
    /**
     * Alternative gaming - разрешить аукцион?
     */
    public static boolean ALT_ALLOW_AUC;
    /**
     * Alternative gaming - clan members with see privilege can also withdraw
     * from clan warehouse.
     */
    public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
    /**
     * Alternative gaming - Castle Shield can be equiped by all clan members if
     * they own a castle. - default True
     */
    public static boolean CASTLE_SHIELD;
    /**
     * Alternative gaming - Clan Hall Shield can be equiped by all clan members
     * if they own a clan hall. - default True
     */
    public static boolean CLANHALL_SHIELD;
    /**
     * Alternative gaming - Apella armors can be equiped only by clan members if
     * their class is Baron or higher - default True
     */
    public static boolean APELLA_ARMORS;
    /**
     * Alternative gaming - Clan Oath Armors can be equiped only by clan members
     * - default True
     */
    public static boolean OATH_ARMORS;
    /**
     * Alternative gaming - Castle Crown can be equiped only by castle lord -
     * default True
     */
    public static boolean CASTLE_CROWN;
    /**
     * Alternative gaming - Castle Circlets can be equiped only by clan members
     * if they own a castle - default True
     */
    public static boolean CASTLE_CIRCLETS;
    /**
     * Maximum number of clans in ally
     */
    public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
    /**
     * Life Crystal needed to learn clan skill
     */
    public static boolean LIFE_CRYSTAL_NEEDED;
    /**
     * Spell Book needed to learn skill
     */
    public static boolean SP_BOOK_NEEDED;
    /**
     * Spell Book needet to enchant skill
     */
    public static boolean ES_SP_BOOK_NEEDED;
    /**
     * Logging Chat Window
     */
    public static boolean LOG_CHAT;
    /**
     * Logging Item Window
     */
    public static boolean LOG_ITEMS;
    public static FastList<Integer> LOG_MULTISELL_ID = new FastList<Integer>();
    /**
     * Alternative privileges for admin
     */
    public static boolean ALT_PRIVILEGES_ADMIN;
    /**
     * Alternative secure check privileges
     */
    public static boolean ALT_PRIVILEGES_SECURE_CHECK;
    /**
     * Alternative default level for privileges
     */
    public static int ALT_PRIVILEGES_DEFAULT_LEVEL;
    /**
     * Olympiad Competition Starting time
     */
    public static int ALT_OLY_START_TIME;
    /**
     * Olympiad Minutes
     */
    public static int ALT_OLY_MIN;
    /**
     * Olympiad Competition Period
     */
    public static long ALT_OLY_CPERIOD;
    /**
     * Olympiad Battle Period
     */
    public static long ALT_OLY_BATTLE;
    /**
     * Olympiad Battle Wait
     */
    public static long ALT_OLY_BWAIT;
    /**
     * Olympiad Inital Wait
     */
    public static long ALT_OLY_IWAIT;
    /**
     * Olympaid Weekly Period
     */
    public static long ALT_OLY_WPERIOD;
    /**
     * Olympaid Validation Period
     */
    public static long ALT_OLY_VPERIOD;
    /**
     * Oly same ip protection
     */
    public static boolean ALT_OLY_SAME_IP;
    public static boolean ALT_OLY_SAME_HWID;
    /**
     * Olympiad max enchant limitation
     */
    public static int ALT_OLY_ENCHANT_LIMIT;
    public static int ALT_OLY_MINCLASS;
    public static int ALT_OLY_MINNONCLASS;
    /**
     * восстанавливать мп перед боем
     */
    public static boolean ALT_OLY_MP_REG;
    /**
     * Manor Refresh Starting time
     */
    public static int ALT_MANOR_REFRESH_TIME;
    /**
     * Manor Refresh Min
     */
    public static int ALT_MANOR_REFRESH_MIN;
    /**
     * Manor Next Period Approve Starting time
     */
    public static int ALT_MANOR_APPROVE_TIME;
    /**
     * Manor Next Period Approve Min
     */
    public static int ALT_MANOR_APPROVE_MIN;
    /**
     * Manor Maintenance Time
     */
    public static int ALT_MANOR_MAINTENANCE_PERIOD;
    /**
     * Manor Save All Actions
     */
    public static boolean ALT_MANOR_SAVE_ALL_ACTIONS;
    /**
     * Manor Save Period Rate
     */
    public static int ALT_MANOR_SAVE_PERIOD_RATE;
    /**
     * Initial Lottery prize
     */
    public static int ALT_LOTTERY_PRIZE;
    /**
     * Lottery Ticket Price
     */
    public static int ALT_LOTTERY_TICKET_PRICE;
    /**
     * What part of jackpot amount should receive characters who pick 5 wining
     * numbers
     */
    public static float ALT_LOTTERY_5_NUMBER_RATE;
    /**
     * What part of jackpot amount should receive characters who pick 4 wining
     * numbers
     */
    public static float ALT_LOTTERY_4_NUMBER_RATE;
    /**
     * What part of jackpot amount should receive characters who pick 3 wining
     * numbers
     */
    public static float ALT_LOTTERY_3_NUMBER_RATE;
    /**
     * How much adena receive characters who pick two or less of the winning
     * number
     */
    public static int ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
    /**
     * Нужны ли квесты на грандбоссов *
     */
    public static boolean NOEPIC_QUESTS;
    /**
     * Автолут на эпик бижу + показывает в чат окружающим кому ушла *
     */
    public static boolean ALT_EPIC_JEWERLY;
    /**
     * Разрешить активный и пассивный аугмент одновременно *
     */
    public static boolean ONE_AUGMENT;
    /**
     * Окошко взятия профы при лвл-апе *
     */
    public static boolean JOB_WINDOW;
    /**
     * бесконечные чулшоты *
     */
    public static boolean USE_SOULSHOTS;
    public static boolean USE_ARROWS;
    public static int MAX_PATK_SPEED;
    public static int MAX_MATK_SPEED;
    public static int MAX_MAX_HP;
    public static boolean DISABLE_GRADE_PENALTY;
    public static boolean DISABLE_WEIGHT_PENALTY;
    /**
     * Four Sepulcher
     */
    public static int FS_TIME_ATTACK;
    public static int FS_TIME_COOLDOWN;
    public static int FS_TIME_ENTRY;
    public static int FS_TIME_WARMUP;
    public static int FS_PARTY_MEMBER_COUNT;
    /**
     * Minimum siz e of a party that may enter dimensional rift
     */
    public static int RIFT_MIN_PARTY_SIZE;
    /**
     * Time in ms the party has to wait until the mobs spawn when entering a
     * room
     */
    public static int RIFT_SPAWN_DELAY;
    /**
     * Amount of random rift jumps before party is ported back
     */
    public static int RIFT_MAX_JUMPS;
    /**
     * Random time between two jumps in dimensional rift - in seconds
     */
    public static int RIFT_AUTO_JUMPS_TIME_MIN;
    public static int RIFT_AUTO_JUMPS_TIME_MAX;
    /**
     * Dimensional Fragment cost for entering rift
     */
    public static int RIFT_ENTER_COST_RECRUIT;
    public static int RIFT_ENTER_COST_SOLDIER;
    public static int RIFT_ENTER_COST_OFFICER;
    public static int RIFT_ENTER_COST_CAPTAIN;
    public static int RIFT_ENTER_COST_COMMANDER;
    public static int RIFT_ENTER_COST_HERO;
    public static int MAX_CHAT_LENGTH;
    public static int CRUMA_TOWER_LEVEL_RESTRICT;
    /**
     * time multiplier for boss room
     */
    public static float RIFT_BOSS_ROOM_TIME_MUTIPLY;
    /*
     * **************************************************************************
     * GM CONFIG General GM AccessLevel *
     * ************************************************************************
     */
    /**
     * General GM access level
     */
    public static int GM_ACCESSLEVEL;
    /**
     * General GM Minimal AccessLevel
     */
    public static int GM_MIN;
    /**
     * Minimum privileges level for a GM to do Alt+G
     */
    public static int GM_ALTG_MIN_LEVEL;
    /**
     * General GM AccessLevel to change announcements
     */
    public static int GM_ANNOUNCE;
    /**
     * General GM AccessLevel can /ban /unban
     */
    public static int GM_BAN;
    /**
     * General GM AccessLevel can /ban /unban for chat
     */
    public static int GM_BAN_CHAT;
    /**
     * General GM AccessLevel can /create_item and /gmshop
     */
    public static int GM_CREATE_ITEM;
    /**
     * General GM AccessLevel can /delete
     */
    public static int GM_DELETE;
    /**
     * General GM AccessLevel can /kick /disconnect
     */
    public static int GM_KICK;
    /**
     * General GM AccessLevel for access to GMMenu
     */
    public static int GM_MENU;
    /**
     * General GM AccessLevel to use god mode command
     */
    public static int GM_GODMODE;
    /**
     * General GM AccessLevel with character edit rights
     */
    public static int GM_CHAR_EDIT;
    /**
     * General GM AccessLevel with edit rights for other characters
     */
    public static int GM_CHAR_EDIT_OTHER;
    /**
     * General GM AccessLevel with character view rights
     */
    public static int GM_CHAR_VIEW;
    /**
     * General GM AccessLevel with NPC edit rights
     */
    public static int GM_NPC_EDIT;
    public static int GM_NPC_VIEW;
    /**
     * General GM AccessLevel to teleport to any location
     */
    public static int GM_TELEPORT;
    /**
     * General GM AccessLevel to teleport character to any location
     */
    public static int GM_TELEPORT_OTHER;
    /**
     * General GM AccessLevel to restart server
     */
    public static int GM_RESTART;
    /**
     * General GM AccessLevel for MonsterRace
     */
    public static int GM_MONSTERRACE;
    /**
     * General GM AccessLevel to ride Wyvern
     */
    public static int GM_RIDER;
    /**
     * General GM AccessLevel to unstuck without 5min delay
     */
    public static int GM_ESCAPE;
    /**
     * General GM AccessLevel to resurect fixed after death
     */
    public static int GM_FIXED;
    /**
     * General GM AccessLevel to create Path Nodes
     */
    public static int GM_CREATE_NODES;
    /**
     * General GM AccessLevel with Enchant rights
     */
    public static int GM_ENCHANT;
    /**
     * General GM AccessLevel to close/open Doors
     */
    public static int GM_DOOR;
    /**
     * General GM AccessLevel with Resurrection rights
     */
    public static int GM_RES;
    /**
     * General GM AccessLevel to attack in the peace zone
     */
    public static int GM_PEACEATTACK;
    /**
     * General GM AccessLevel to heal
     */
    public static int GM_HEAL;
    /**
     * General GM AccessLevel to unblock IPs detected as hack IPs
     */
    public static int GM_UNBLOCK;
    /**
     * General GM AccessLevel to use Cache commands
     */
    public static int GM_CACHE;
    /**
     * General GM AccessLevel to use test&st commands
     */
    public static int GM_TALK_BLOCK;
    public static int GM_TEST;
    /**
     * Disable transaction on AccessLevel *
     */
    public static boolean GM_DISABLE_TRANSACTION;
    /**
     * GM transactions disabled from this range
     */
    public static int GM_TRANSACTION_MIN;
    /**
     * GM transactions disabled to this range
     */
    public static int GM_TRANSACTION_MAX;
    /**
     * Minimum level to allow a GM giving damage
     */
    public static int GM_CAN_GIVE_DAMAGE;
    /**
     * Minimum level to don't give Exp/Sp in party
     */
    public static int GM_DONT_TAKE_EXPSP;
    /**
     * Minimum level to don't take aggro
     */
    public static int GM_DONT_TAKE_AGGRO;
    public static int GM_REPAIR = 75;

    /*
     * Rate control
     */
    /**
     * Rate for eXperience Point rewards
     */
    public static float RATE_XP;
    /**
     * Rate for Skill Point rewards
     */
    public static float RATE_SP;
    /**
     * Rate for party eXperience Point rewards
     */
    public static float RATE_PARTY_XP;
    /**
     * Rate for party Skill Point rewards
     */
    public static float RATE_PARTY_SP;
    /**
     * Rate for Quest rewards (XP and SP)
     */
    public static float RATE_QUESTS_REWARD;
    /**
     * Rate for drop adena
     */
    public static float RATE_DROP_ADENA;
    // умножение выпадающей адены
    public static float RATE_DROP_ADENAMUL;
    /**
     * Rate for cost of consumable
     */
    public static float RATE_CONSUMABLE_COST;
    /**
     * Rate for dropped items
     */
    public static float RATE_DROP_ITEMS;
    public static float RATE_DROP_ITEMS_BY_RAID;
    public static float RATE_DROP_ITEMS_BY_GRANDRAID;
    /**
     * Rate for spoiled items
     */
    public static float RATE_DROP_SPOIL;
    /**
     * Rate for manored items
     */
    public static int RATE_DROP_MANOR;
    /**
     * Rate for quest items
     */
    public static float RATE_DROP_QUEST;
    /**
     * Rate for karma and experience lose
     */
    public static float RATE_KARMA_EXP_LOST;
    /**
     * Rate siege guards prices
     */
    public static float RATE_SIEGE_GUARDS_PRICE;
    /*
     * Alternative Xp/Sp rewards, if not 0, then calculated as
     * 2^((mob.level-player.level) / coef), A few examples for
     * "AltGameExponentXp = 5." and "AltGameExponentSp = 3." diff = 0 (player
     * and mob has the same level), XP bonus rate = 1, SP bonus rate = 1 diff =
     * 3 (mob is 3 levels above), XP bonus rate = 1.52, SP bonus rate = 2 diff =
     * 5 (mob is 5 levels above), XP bonus rate = 2, SP bonus rate = 3.17 diff =
     * -8 (mob is 8 levels below), XP bonus rate = 0.4, SP bonus rate = 0.16
     */
    /**
     * Alternative eXperience Point rewards
     */
    public static float ALT_GAME_EXPONENT_XP;
    /**
     * Alternative Spirit Point rewards
     */
    public static float ALT_GAME_EXPONENT_SP;
    /**
     * Rate Common herbs
     */
    public static float RATE_DROP_COMMON_HERBS;
    /**
     * Rate MP/HP herbs
     */
    public static float RATE_DROP_MP_HP_HERBS;
    /**
     * Rate Common herbs
     */
    public static float RATE_DROP_GREATER_HERBS;
    /**
     * Rate Common herbs
     */
    public static float RATE_DROP_SUPERIOR_HERBS;
    /**
     * Rate Common herbs
     */
    public static float RATE_DROP_SPECIAL_HERBS;
    // Player Drop Rate control
    /**
     * Limit for player drop
     */
    public static int PLAYER_DROP_LIMIT;
    /**
     * Rate for drop
     */
    public static int PLAYER_RATE_DROP;
    /**
     * Rate for player's item drop
     */
    public static int PLAYER_RATE_DROP_ITEM;
    /**
     * Rate for player's equipment drop
     */
    public static int PLAYER_RATE_DROP_EQUIP;
    /**
     * Rate for player's equipment and weapon drop
     */
    public static int PLAYER_RATE_DROP_EQUIP_WEAPON;
    // Pet Rates (Multipliers)
    /**
     * Rate for experience rewards of the pet
     */
    public static float PET_XP_RATE;
    /**
     * Rate for food consumption of the pet
     */
    public static int PET_FOOD_RATE;
    /**
     * Rate for experience rewards of the Sin Eater
     */
    public static float SINEATER_XP_RATE;
    // Karma Drop Rate control
    /**
     * Karma drop limit
     */
    public static int KARMA_DROP_LIMIT;
    /**
     * Karma drop rate
     */
    public static int KARMA_RATE_DROP;
    /**
     * Karma drop rate for item
     */
    public static int KARMA_RATE_DROP_ITEM;
    /**
     * Karma drop rate for equipment
     */
    public static int KARMA_RATE_DROP_EQUIP;
    /**
     * Karma drop rate for equipment and weapon
     */
    public static int KARMA_RATE_DROP_EQUIP_WEAPON;
    /**
     * Time after which item will auto-destroy
     */
    public static int AUTODESTROY_ITEM_AFTER;
    /**
     * Auto destroy herb time
     */
    public static int HERB_AUTO_DESTROY_TIME;
    /**
     * List of items that will not be destroyed (seperated by ",")
     */
    public static String PROTECTED_ITEMS;
    /**
     * List of items that will not be destroyed
     */
    public static List<Integer> LIST_PROTECTED_ITEMS = new FastList<Integer>();
    /**
     * Auto destroy nonequipable items dropped by players
     */
    public static boolean DESTROY_DROPPED_PLAYER_ITEM;
    /**
     * Auto destroy equipable items dropped by players
     */
    public static boolean DESTROY_EQUIPABLE_PLAYER_ITEM;
    /**
     * Save items on ground for restoration on server restart
     */
    public static boolean SAVE_DROPPED_ITEM;
    /**
     * Empty table ItemsOnGround after load all items
     */
    public static boolean EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
    /**
     * Time interval to save into db items on ground
     */
    public static int SAVE_DROPPED_ITEM_INTERVAL;
    /**
     * Clear all items stored in ItemsOnGround table
     */
    public static boolean CLEAR_DROPPED_ITEM_TABLE;
    /**
     * Accept precise drop calculation ?
     */
    public static boolean PRECISE_DROP_CALCULATION;
    /**
     * Accept multi-items drop ?
     */
    public static boolean MULTIPLE_ITEM_DROP;
    /**
     * This is setting of experimental Client <--> Server Player coordinates
     * synchronization<br>
     * <b><u>Valeurs :</u></b>
     * <li>0 - no synchronization at
     * all</li>
     * <li>1 - parcial synchronization Client --> Server only * using
     * this option it is difficult for players to bypass obstacles</li>
     * <li>2 -
     * parcial synchronization Server --> Client only</li>
     * <li>3 - full
     * synchronization Client <--> Server</li>
     * <li>-1 - Old system: will
     * synchronize Z only</li>
     */
    public static int COORD_SYNCHRONIZE;
    /**
     * Period in days after which character is deleted
     */
    public static int DELETE_DAYS;
    /**
     * Datapack root directory
     */
    public static File DATAPACK_ROOT;
    /**
     * Maximum range mobs can randomly go from spawn point
     */
    public static int MAX_DRIFT_RANGE;
    /**
     * Allow fishing ?
     */
    public static boolean ALLOWFISHING;
    /**
     * Allow Manor system
     */
    public static boolean ALLOW_MANOR;
    /**
     * Jail config *
     */
    public static boolean JAIL_IS_PVP;
    public static boolean JAIL_DISABLE_CHAT;

    /**
     * Enumeration describing values for Allowing the use of L2Walker client
     */
    public static enum L2WalkerAllowed {

        True,
        False,
        GM
    }

    /**
     * Allow the use of L2Walker client ?
     */
    public static L2WalkerAllowed ALLOW_L2WALKER_CLIENT;
    /**
     * Auto-ban client that use L2Walker ?
     */
    public static boolean AUTOBAN_L2WALKER_ACC;
    /**
     * Revision of L2Walker
     */
    public static int L2WALKER_REVISION;
    /**
     * GM Edit allowed on Non Gm players?
     */
    public static boolean GM_EDIT;
    /**
     * Allow Discard item ?
     */
    public static boolean ALLOW_DISCARDITEM;
    /**
     * Allow freight ?
     */
    public static boolean ALLOW_FREIGHT;
    /**
     * Allow warehouse ?
     */
    public static boolean ALLOW_WAREHOUSE;
    /**
     * Allow warehouse cache?
     */
    public static boolean WAREHOUSE_CACHE;
    /**
     * How long store WH datas
     */
    public static int WAREHOUSE_CACHE_TIME;
    /**
     * Allow wear ? (try on in shop)
     */
    public static boolean ALLOW_WEAR;
    /**
     * Duration of the try on after which items are taken back
     */
    public static int WEAR_DELAY;
    /**
     * Price of the try on of one item
     */
    public static int WEAR_PRICE;
    /**
     * Allow lottery ?
     */
    public static boolean ALLOW_LOTTERY;
    /**
     * Allow race ?
     */
    public static boolean ALLOW_RACE;
    /**
     * Allow water ?
     */
    public static boolean ALLOW_WATER;
    /**
     * Allow rent pet ?
     */
    public static boolean ALLOW_RENTPET;
    /**
     * Allow boat ?
     */
    public static boolean ALLOW_BOAT;
    /**
     * Allow RaidBoss Petrified if player have +9 lvl to RB
     */
    public static boolean ALLOW_RAID_BOSS_PUT;
    public static boolean FORBID_RAID_BOSS_HEAL;
    /**
     * Allow cursed weapons ?
     */
    public static boolean ALLOW_CURSED_WEAPONS;
    // WALKER NPC
    public static boolean ALLOW_NPC_WALKERS;
    /**
     * Time after which a packet is considered as lost
     */
    public static int PACKET_LIFETIME;
    // Pets
    /**
     * Speed of Weverns
     */
    public static int WYVERN_SPEED;
    /**
     * Speed of Striders
     */
    public static int STRIDER_SPEED;
    /**
     * Speed in WATER
     */
    public static int WATER_SPEED;
    /**
     * Allow Wyvern Upgrader ?
     */
    public static boolean ALLOW_WYVERN_UPGRADER;
    // protocol revision
    /**
     * Minimal protocol revision
     */
    public static int MIN_PROTOCOL_REVISION;
    /**
     * Maximal protocol revision
     */
    public static int MAX_PROTOCOL_REVISION;
    // Для отладки, показываь протоколы которые заходят на сервер?
    public static boolean SHOW_PROTOCOL_VERSIONS;
    // random animation interval
    /**
     * Minimal time between 2 animations of a NPC
     */
    public static int MIN_NPC_ANIMATION;
    /**
     * Maximal time between 2 animations of a NPC
     */
    public static int MAX_NPC_ANIMATION;
    /**
     * Minimal time between animations of a MONSTER
     */
    public static int MIN_MONSTER_ANIMATION;
    /**
     * Maximal time between animations of a MONSTER
     */
    public static int MAX_MONSTER_ANIMATION;
    /**
     * Activate position recorder ?
     */
    public static boolean ACTIVATE_POSITION_RECORDER;
    /**
     * Use 3D Map ?
     */
    public static boolean USE_3D_MAP;
    // Community Board
    /**
     * Type of community
     */
    public static String COMMUNITY_TYPE;
    public static String BBS_DEFAULT;
    /**
     * Show level of the community board ?
     */
    public static boolean SHOW_LEVEL_COMMUNITYBOARD;
    /**
     * Show status of the community board ?
     */
    public static boolean SHOW_STATUS_COMMUNITYBOARD;
    /**
     * Size of the name page on the community board
     */
    public static int NAME_PAGE_SIZE_COMMUNITYBOARD;
    /**
     * Name per row on community board
     */
    public static int NAME_PER_ROW_COMMUNITYBOARD;
    public static int MAX_ITEM_IN_PACKET;
    public static boolean CHECK_KNOWN;
    /**
     * Game Server login port
     */
    public static int GAME_SERVER_LOGIN_PORT;
    /**
     * Game Server login Host
     */
    public static String GAME_SERVER_LOGIN_HOST;
    /**
     * Internal Hostname
     */
    public static String INTERNAL_HOSTNAME;
    /**
     * External Hostname
     */
    public static String EXTERNAL_HOSTNAME;
    public static int PATH_NODE_RADIUS;
    public static int NEW_NODE_ID;
    public static int SELECTED_NODE_ID;
    public static int LINKED_NODE_ID;
    public static String NEW_NODE_TYPE;
    /**
     * Show "data/html/servnews.htm" whenever a character enters world.
     */
    public static boolean SERVER_NEWS;
    /**
     * Show L2Monster level and aggro ?
     */
    public static boolean SHOW_NPC_LVL;
    /**
     * Force full item inventory packet to be sent for any item change ?<br>
     * <u><i>Note:</i></u> This can increase network traffic
     */
    public static boolean FORCE_INVENTORY_UPDATE;
    /**
     * Disable the use of guards against agressive monsters ?
     */
    public static boolean ALLOW_GUARDS;
    /**
     * Allow use Event Managers for change occupation ?
     */
    public static boolean ALLOW_CLASS_MASTERS;
    public static final FastMap<Integer, EventReward> CLASS_MASTERS_PRICES = new FastMap<Integer, EventReward>()
            .shared("Config.CLASS_MASTERS_PRICES");
    public static boolean ALLOW_CLAN_LEVEL;
    public static boolean REWARD_SHADOW;
    /**
     * Time between 2 updates of IP
     */
    public static int IP_UPDATE_TIME;
    // Server version
    /**
     * Server version
     */
    public static String SERVER_VERSION;
    /**
     * Date of server build
     */
    public static String SERVER_BUILD_DATE;
    // Datapack version
    /**
     * Datapack version
     */
    public static String DATAPACK_VERSION;
    /**
     * Zone Setting
     */
    public static int ZONE_TOWN;
    /**
     * Crafting Enabled?
     */
    public static boolean IS_CRAFTING_ENABLED;
    // Inventory slots limit
    /**
     * Maximum inventory slots limits for non dwarf characters
     */
    public static int INVENTORY_MAXIMUM_NO_DWARF;
    /**
     * Maximum inventory slots limits for dwarf characters
     */
    public static int INVENTORY_MAXIMUM_DWARF;
    /**
     * Maximum inventory slots limits for GM
     */
    public static int INVENTORY_MAXIMUM_GM;
    // Warehouse slots limits
    /**
     * Maximum inventory slots limits for non dwarf warehouse
     */
    public static int WAREHOUSE_SLOTS_NO_DWARF;
    /**
     * Maximum inventory slots limits for dwarf warehouse
     */
    public static int WAREHOUSE_SLOTS_DWARF;
    /**
     * Maximum inventory slots limits for clan warehouse
     */
    public static int WAREHOUSE_SLOTS_CLAN;
    /**
     * Maximum inventory slots limits for freight
     */
    public static int FREIGHT_SLOTS;
    // Karma System Variables
    /**
     * Minimum karma gain/loss
     */
    public static int KARMA_MIN_KARMA;
    /**
     * Maximum karma gain/loss
     */
    public static int KARMA_MAX_KARMA;
    /**
     * Number to divide the xp recieved by, to calculate karma lost on xp
     * gain/lost
     */
    public static int KARMA_XP_DIVIDER;
    /**
     * The Minimum Karma lost if 0 karma is to be removed
     */
    public static int KARMA_LOST_BASE;
    /**
     * Can a GM drop item ?
     */
    public static boolean KARMA_DROP_GM;
    /**
     * Should award a pvp point for killing a player with karma ?
     */
    public static boolean KARMA_AWARD_PK_KILL;
    /**
     * Minimum PK required to drop
     */
    public static int KARMA_PK_LIMIT;
    /**
     * List of pet items that cannot be dropped (seperated by ",") when PVP
     */
    public static String KARMA_NONDROPPABLE_PET_ITEMS;
    /**
     * List of items that cannot be dropped (seperated by ",") when PVP
     */
    public static String KARMA_NONDROPPABLE_ITEMS;
    /**
     * List of pet items that cannot be dropped when PVP
     */
    public static List<Integer> KARMA_LIST_NONDROPPABLE_PET_ITEMS = new FastList<Integer>();
    /**
     * List of items that cannot be dropped when PVP
     */
    public static List<Integer> KARMA_LIST_NONDROPPABLE_ITEMS = new FastList<Integer>();
    /**
     * List of items that cannot be dropped (seperated by ",")
     */
    public static String NONDROPPABLE_ITEMS;
    /**
     * List of items that cannot be dropped
     */
    public static List<Integer> LIST_NONDROPPABLE_ITEMS = new FastList<Integer>();
    /**
     * List of NPCs that rent pets (seperated by ",")
     */
    public static String PET_RENT_NPC;
    /**
     * List of NPCs that rent pets
     */
    public static List<Integer> LIST_PET_RENT_NPC = new FastList<Integer>();
    /**
     * Duration (in ms) while a player stay in PVP mode after hitting an
     * innocent
     */
    public static int PVP_NORMAL_TIME;
    /**
     * Duration (in ms) while a player stay in PVP mode after hitting a purple
     * player
     */
    public static int PVP_PVP_TIME;
    // Karma Punishment
    /**
     * Allow player with karma to be killed in peace zone ?
     */
    public static boolean ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE;
    /**
     * Allow player with karma to shop ?
     */
    public static boolean ALT_GAME_KARMA_PLAYER_CAN_SHOP;
    /**
     * Allow player with karma to use gatekeepers ?
     */
    public static boolean ALT_GAME_KARMA_PLAYER_CAN_USE_GK;
    /**
     * Allow player with karma to use SOE or Return skill ?
     */
    public static boolean ALT_GAME_KARMA_PLAYER_CAN_TELEPORT;
    /**
     * Allow player with karma to trade ?
     */
    public static boolean ALT_GAME_KARMA_PLAYER_CAN_TRADE;
    /**
     * Allow player with karma to use warehouse ?
     */
    public static boolean ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE;
    /**
     * Allow Players Level Difference Protection ?
     */
    public static int ALT_PLAYER_PROTECTION_LEVEL;
    /**
     * define L2JMODS
     */
    /**
     * Champion Mod
     */
    public static boolean L2JMOD_CHAMPION_ENABLE;
    public static int L2JMOD_CHAMPION_FREQUENCY;
    public static int L2JMOD_CHAMP_MIN_LVL;
    public static int L2JMOD_CHAMP_MAX_LVL;
    public static int L2JMOD_CHAMPION_HP;
    public static double L2JMOD_CHAMPION_REWARDS_CHANCE;
    public static double L2JMOD_CHAMPION_REWARDS_COUNT;
    public static double L2JMOD_CHAMPION_REWARDS_EXP;
    public static double L2JMOD_CHAMPION_REWARDS_SP;
    public static double L2JMOD_CHAMPION_ADENAS_REWARDS;
    public static float L2JMOD_CHAMPION_HP_REGEN;
    public static float L2JMOD_CHAMPION_ATK;
    public static float L2JMOD_CHAMPION_SPD_ATK;
    public static boolean L2JMOD_CHAMPION_REWARD;
    public static FastList<EventReward> L2JMOD_CHAMPION_REWARD_LIST = new FastList<EventReward>();
    public static boolean L2JMOD_CHAMPION_AURA;

    public static boolean L2JMOD_CHAMPION_ENABLE_BLUE;
    public static int L2JMOD_CHAMPION_FREQUENCY_BLUE;
    public static int L2JMOD_CHAMP_MIN_LVL_BLUE;
    public static int L2JMOD_CHAMP_MAX_LVL_BLUE;
    public static int L2JMOD_CHAMPION_HP_BLUE;
    public static double L2JMOD_CHAMPION_REWARDS_CHANCE_BLUE;
    public static double L2JMOD_CHAMPION_REWARDS_COUNT_BLUE;
    public static double L2JMOD_CHAMPION_REWARDS_EXP_BLUE;
    public static double L2JMOD_CHAMPION_REWARDS_SP_BLUE;
    public static double L2JMOD_CHAMPION_ADENAS_REWARDS_BLUE;
    public static float L2JMOD_CHAMPION_HP_REGEN_BLUE;
    public static float L2JMOD_CHAMPION_ATK_BLUE;
    public static float L2JMOD_CHAMPION_SPD_ATK_BLUE;
    public static boolean L2JMOD_CHAMPION_REWARD_BLUE;
    public static FastList<EventReward> L2JMOD_CHAMPION_REWARD_LIST_BLUE = new FastList<EventReward>();
    public static boolean L2JMOD_CHAMPION_AURA_BLUE;

    /**
     * Team vs. Team Event Engine
     */
    public static boolean TVT_EVENT_ENABLED;
    public static int TVT_EVENT_INTERVAL;
    public static int TVT_EVENT_PARTICIPATION_TIME;
    public static int TVT_EVENT_RUNNING_TIME;
    public static int TVT_EVENT_PARTICIPATION_NPC_ID;
    public static FastList<Location> TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new FastList<Location>();
    public static int TVT_EVENT_MIN_PLAYERS_IN_TEAMS;
    public static int TVT_EVENT_MAX_PLAYERS_IN_TEAMS;
    public static int TVT_EVENT_RESPAWN_TELEPORT_DELAY;
    public static int TVT_EVENT_START_LEAVE_TELEPORT_DELAY;
    public static String TVT_EVENT_TEAM_1_NAME;
    public static FastList<Location> TVT_EVENT_TEAM_1_COORDINATES = new FastList<Location>();
    public static String TVT_EVENT_TEAM_2_NAME;
    public static FastList<Location> TVT_EVENT_TEAM_2_COORDINATES = new FastList<Location>();
    public static FastList<Location> TVT_RETURN_COORDINATES = new FastList<Location>();
    public static final List<int[]> TVT_EVENT_REWARDS = new FastList<int[]>();
    public static int TVT_EVENT_REWARDS_TOP_KILLS;
    public static final List<int[]> TVT_EVENT_REWARDS_TOP = new FastList<int[]>();
    public static boolean TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED;
    public static boolean TVT_EVENT_POTIONS_ALLOWED;
    public static boolean TVT_EVENT_SUMMON_BY_ITEM_ALLOWED;
    public static final List<Integer> TVT_EVENT_DOOR_IDS = new FastList<Integer>();
    public static byte TVT_EVENT_MIN_LVL;
    public static byte TVT_EVENT_MAX_LVL;
    public static boolean TVT_NO_PASSIVE;
    public static boolean TVT_REWARD_CHECK;
    public static boolean TVT_REWARD_TOP;
    public static int TVT_TELE_PROTECT;
    public static boolean TVT_TEAM_PROTECT;
    public static boolean TVT_NO_DEADS;
    /**
     * L2JMOD Wedding system
     */
    public static boolean L2JMOD_ALLOW_WEDDING;
    public static int L2JMOD_WEDDING_INTERVAL;
    public static boolean L2JMOD_WEDDING_PUNISH_INFIDELITY;
    public static boolean L2JMOD_WEDDING_TELEPORT;
    public static int L2JMOD_WEDDING_TELEPORT_PRICE;
    public static int L2JMOD_WEDDING_TELEPORT_DURATION;
    public static boolean L2JMOD_WEDDING_SAMESEX;
    public static boolean L2JMOD_WEDDING_FORMALWEAR;
    public static int L2JMOD_WEDDING_COIN;
    public static int L2JMOD_WEDDING_PRICE;
    public static String L2JMOD_WEDDING_COINNAME;
    public static int L2JMOD_WEDDING_DIVORCE_COIN;
    public static int L2JMOD_WEDDING_DIVORCE_PRICE;
    public static String L2JMOD_WEDDING_DIVORCE_COINNAME;
    public static boolean L2JMOD_WEDDING_BOW;
    // L2WalkerProtection
    public static boolean KICK_L2WALKER;
    // пвп-зона на епиках
    public static boolean ALLOW_RAID_PVP;
    // откат цп
    public static long CP_REUSE_TIME;
    // банка мп
    public static long MANA_RESTORE;
    // антибафф
    public static int ANTIBUFF_SKILLID;
    // шанс прохождения абил
    public static int BLOW_CHANCE_FRONT;
    public static int BLOW_CHANCE_BEHIND;
    public static int BLOW_CHANCE_SIDE;
    // умножение дамага абил по типу брони
    public static double BLOW_DAMAGE_HEAVY;
    public static double BLOW_DAMAGE_LIGHT;
    public static double BLOW_DAMAGE_ROBE;
    public static boolean ALLOW_HERO_SUBSKILL;
    public static byte SUB_START_LVL;
    public static int RUN_SPD_BOOST;
    public static int MAX_RUN_SPEED;
    public static int MAX_PCRIT_RATE;
    public static int MAX_MCRIT_RATE;
    public static boolean ALT_SUBCLASS_SKILLS;
    public static boolean SPAWN_CHAR;
    /**
     * X Coordinate of the SPAWN_CHAR setting.
     */
    public static int SPAWN_X;
    /**
     * Y Coordinate of the SPAWN_CHAR setting.
     */
    public static int SPAWN_Y;
    /**
     * Z Coordinate of the SPAWN_CHAR setting.
     */
    public static int SPAWN_Z;
    /**
     * Chance that a Crystal Scroll will have to enchant over safe limit
     */
    public static int ENCHANT_CHANCE_WEAPON_CRYSTAL;
    /**
     * Chance that a Crystal Scroll will have to enchant over safe limit
     */
    public static int ENCHANT_CHANCE_ARMOR_CRYSTAL;
    /**
     * Chance that a Crystal Scroll will have to enchant over safe limit
     */
    public static int ENCHANT_CHANCE_JEWELRY_CRYSTAL;
    /**
     * Переделка шансов на затчоку
     */
    public static int ENCHANT_CHANCE_NEXT; // переломный момент в шансах заточки
    public static int ENCHANT_FAILED_NUM; // переломный момент в шансах заточки
    public static float MAGIC_CHANCE_BEFORE_NEXT; // шанс на заточку магического оружия до ENCHANT_CHANCE_NEXT
    public static float MAGIC_CHANCE_AFTER_NEXT; // шанс на заточку магического оружия после ENCHANT_CHANCE_NEXT
                                                 // включительно
    public static float WEAPON_CHANCE_BEFORE_NEXT; // шанс на заточку оружия до ENCHANT_CHANCE_NEXT
    public static float WEAPON_CHANCE_AFTER_NEXT; // шанс на заточку оружия после ENCHANT_CHANCE_NEXT включительно
    public static final List<Float> ARMOR_ENCHANT_TABLE = new FastList<Float>(); // таблица шансов на заточку арморов
    public static final List<Float> FULL_ARMOR_ENCHANT_TABLE = new FastList<Float>(); // таблица шансов на заточку
                                                                                      // FullBody арморов
    /**
     * Chat filter
     */
    public static boolean USE_CHAT_FILTER;
    public static List<String> FILTER_LIST = new FastList<String>();
    public static String CHAT_FILTER_PUNISHMENT;
    public static int CHAT_FILTER_PUNISHMENT_PARAM1;
    public static int CHAT_FILTER_PUNISHMENT_PARAM2;
    // Packet information
    /**
     * Count the amount of packets per minute ?
     */
    public static boolean COUNT_PACKETS = false;
    /**
     * Dump packet count ?
     */
    public static boolean DUMP_PACKET_COUNTS = false;
    /**
     * Time interval between 2 dumps
     */
    public static int DUMP_INTERVAL_SECONDS = 60;

    /**
     * Enumeration for type of ID Factory
     */
    public static enum IdFactoryType {

        Compaction,
        BitSet,
        Stack
    }

    /**
     * ID Factory type
     */
    public static IdFactoryType IDFACTORY_TYPE;
    /**
     * Check for bad ID ?
     */
    public static boolean BAD_ID_CHECKING;

    /**
     * Enumeration for type of maps object
     */
    public static enum ObjectMapType {

        L2ObjectHashMap,
        WorldObjectMap
    }

    /**
     * Enumeration for type of set object
     */
    public static enum ObjectSetType {

        L2ObjectHashSet,
        WorldObjectSet
    }

    /**
     * Type of map object
     */
    public static ObjectMapType MAP_TYPE;
    /**
     * Type of set object
     */
    public static ObjectSetType SET_TYPE;
    /**
     * Allow lesser effects to be canceled if stronger effects are used when
     * effects of the same stack group are used.<br>
     * New effects that are added
     * will be canceled if they are of lesser priority to the old one.
     */
    public static boolean EFFECT_CANCELING;
    /**
     * Auto-delete invalid quest data ?
     */
    public static boolean AUTODELETE_INVALID_QUEST_DATA;
    /**
     * Maximum level of enchantment
     */
    public static int ENCHANT_MAX_WEAPON;
    public static int ENCHANT_MAX_ARMOR;
    public static int ENCHANT_MAX_JEWELRY;
    /**
     * maximum level of safe enchantment for normal items
     */
    public static int ENCHANT_SAFE_MAX;
    /**
     * maximum level of safe enchantment for full body armor
     */
    public static int ENCHANT_SAFE_MAX_FULL;
    // Character multipliers
    /**
     * Multiplier for character HP regeneration
     */
    public static double HP_REGEN_MULTIPLIER;
    /**
     * Mutilplier for character MP regeneration
     */
    public static double MP_REGEN_MULTIPLIER;
    /**
     * Multiplier for character CP regeneration
     */
    public static double CP_REGEN_MULTIPLIER;
    // Raid Boss multipliers
    /**
     * Multiplier for Raid boss HP regeneration
     */
    public static double RAID_HP_REGEN_MULTIPLIER;
    /**
     * Mulitplier for Raid boss MP regeneration
     */
    public static double RAID_MP_REGEN_MULTIPLIER;
    /**
     * Multiplier for Raid boss defense multiplier
     */
    public static double RAID_DEFENCE_MULTIPLIER;
    /**
     * Raid Boss Minin Spawn Timer
     */
    public static double RAID_MINION_RESPAWN_TIMER;
    /**
     * Mulitplier for Raid boss minimum time respawn
     */
    public static float RAID_MIN_RESPAWN_MULTIPLIER;
    /**
     * Mulitplier for Raid boss maximum time respawn
     */
    public static float RAID_MAX_RESPAWN_MULTIPLIER;
    /**
     * Amount of adenas when starting a new character
     */
    public static int STARTING_ADENA;
    /**
     * Deep Blue Mobs' Drop Rules Enabled
     */
    public static boolean DEEPBLUE_DROP_RULES;
    public static int UNSTUCK_INTERVAL;
    /**
     * Is telnet enabled ?
     */
    public static boolean IS_TELNET_ENABLED;
    /**
     * Death Penalty chance
     */
    public static int DEATH_PENALTY_CHANCE;
    /**
     * Augument
     */
    public static int AUGMENT_BASESTAT;
    public static int AUGMENT_SKILL_NORM;
    public static int AUGMENT_SKILL_MID;
    public static int AUGMENT_SKILL_HIGH;
    public static int AUGMENT_SKILL_TOP;
    public static boolean AUGMENT_EXCLUDE_NOTDONE;
    /**
     * Player Protection control
     */
    public static int PLAYER_FAKEDEATH_UP_PROTECTION;
    /**
     * Define Party XP cutoff point method - Possible values: level and
     * percentage
     */
    public static String PARTY_XP_CUTOFF_METHOD;
    /**
     * Define the cutoff point value for the "level" method
     */
    public static int PARTY_XP_CUTOFF_LEVEL;
    /**
     * Define the cutoff point value for the "percentage" method
     */
    public static double PARTY_XP_CUTOFF_PERCENT;
    /**
     * Percent CP is restore on respawn
     */
    public static double RESPAWN_RESTORE_CP;
    /**
     * Percent HP is restore on respawn
     */
    public static double RESPAWN_RESTORE_HP;
    /**
     * Percent MP is restore on respawn
     */
    public static double RESPAWN_RESTORE_MP;
    /**
     * Allow randomizing of the respawn point in towns.
     */
    public static boolean RESPAWN_RANDOM_ENABLED;
    /**
     * The maximum offset from the base respawn point to allow.
     */
    public static int RESPAWN_RANDOM_MAX_OFFSET;
    /**
     * Maximum number of available slots for pvt stores (sell/buy) - Dwarves
     */
    public static int MAX_PVTSTORE_SLOTS_DWARF;
    /**
     * Maximum number of available slots for pvt stores (sell/buy) - Others
     */
    public static int MAX_PVTSTORE_SLOTS_OTHER;
    /**
     * Store skills cooltime on char exit/relogin
     */
    public static boolean STORE_SKILL_COOLTIME;
    /**
     * Show licence or not just after login (if false, will directly go to the
     * Server List
     */
    public static boolean SHOW_LICENCE;
    /**
     * Force GameGuard authorization in loginserver
     */
    public static boolean FORCE_GGAUTH;
    /**
     * Default punishment for illegal actions
     */
    public static int DEFAULT_PUNISH;
    /**
     * Parameter for default punishment
     */
    public static int DEFAULT_PUNISH_PARAM;
    /**
     * Accept new game server ?
     */
    public static boolean ACCEPT_NEW_GAMESERVER;
    /**
     * Server ID used with the HexID
     */
    public static int SERVER_ID;
    /**
     * Hexadecimal ID of the game server
     */
    public static byte[] HEX_ID;
    /**
     * Accept alternate ID for server ?
     */
    public static boolean ACCEPT_ALTERNATE_ID;
    /**
     * ID for request to the server
     */
    public static int REQUEST_ID;
    public static final boolean RESERVE_HOST_ON_LOGIN = false;
    public static int MINIMUM_UPDATE_DISTANCE;
    public static int KNOWNLIST_FORGET_DELAY;
    public static int MINIMUN_UPDATE_TIME;
    public static boolean ANNOUNCE_MAMMON_SPAWN;
    public static boolean LAZY_CACHE;
    /**
     * Place an aura around the GM ?
     */
    public static boolean GM_HERO_AURA;
    /**
     * Set the GM invulnerable at startup ?
     */
    public static boolean GM_STARTUP_INVULNERABLE;
    /**
     * Set the GM invisible at startup ?
     */
    public static boolean GM_STARTUP_INVISIBLE;
    /**
     * Set silence to GM at startup ?
     */
    public static boolean GM_STARTUP_SILENCE;
    /**
     * Add GM in the GM list at startup ?
     */
    public static boolean GM_STARTUP_AUTO_LIST;
    /**
     * Change the way admin panel is shown
     */
    public static String GM_ADMIN_MENU_STYLE;
    /**
     * Allow petition ?
     */
    public static boolean PETITIONING_ALLOWED;
    /**
     * Maximum number of petitions per player
     */
    public static int MAX_PETITIONS_PER_PLAYER;
    /**
     * Maximum number of petitions pending
     */
    public static int MAX_PETITIONS_PENDING;
    /**
     * Bypass exploit protection ?
     */
    public static boolean BYPASS_VALIDATION;
    // твт баф
    public static boolean TVT_BUFF;
    // баф старт
    public static boolean ON_BUFFS;
    public static int ON_BUFFS_LVL;
    public static final FastMap<Integer, Integer> ON_ENTER_M_BUFFS = new FastMap<Integer, Integer>()
            .shared("Config.ON_ENTER_M_BUFFS");
    public static final FastMap<Integer, Integer> ON_ENTER_F_BUFFS = new FastMap<Integer, Integer>()
            .shared("Config.ON_ENTER_F_BUFFS");
    // клан конфиг доп
    public static int CLAN_CHAR_LEVEL6;
    public static int CLAN_CHAR_LEVEL7;
    public static int CLAN_CHAR_LEVEL8;

    public static int CLAN_REP_LEVEL6;
    public static int CLAN_REP_LEVEL7;
    public static int CLAN_REP_LEVEL8;
    /**
     * Only GM buy items for free*
     */
    public static boolean ONLY_GM_ITEMS_FREE;
    /**
     * GM Audit ?
     */
    public static boolean GMAUDIT;
    /**
     * Allow auto-create account ?
     */
    public static boolean AUTO_CREATE_ACCOUNTS;
    public static boolean FLOOD_PROTECTION;
    public static int FAST_CONNECTION_LIMIT;
    public static int NORMAL_CONNECTION_TIME;
    public static int FAST_CONNECTION_TIME;
    public static int MAX_CONNECTION_PER_IP;
    /**
     * Enforce gameguard query on character login ?
     */
    public static boolean GAMEGUARD_ENFORCE;
    /**
     * Don't allow player to perform trade,talk with npc and move until
     * gameguard reply received ?
     */
    public static boolean GAMEGUARD_PROHIBITACTION;
    /**
     * Recipebook limits
     */
    public static int DWARF_RECIPE_LIMIT;
    public static int COMMON_RECIPE_LIMIT;
    /**
     * Grid Options
     */
    public static boolean GRIDS_ALWAYS_ON;
    public static int GRID_NEIGHBOR_TURNON_TIME;
    public static int GRID_NEIGHBOR_TURNOFF_TIME;
    /**
     * Clan Hall function related configs
     */
    public static long CH_TELE_FEE_RATIO;
    public static int CH_TELE1_FEE;
    public static int CH_TELE2_FEE;
    public static long CH_ITEM_FEE_RATIO;
    public static int CH_ITEM1_FEE;
    public static int CH_ITEM2_FEE;
    public static int CH_ITEM3_FEE;
    public static long CH_MPREG_FEE_RATIO;
    public static int CH_MPREG1_FEE;
    public static int CH_MPREG2_FEE;
    public static int CH_MPREG3_FEE;
    public static int CH_MPREG4_FEE;
    public static int CH_MPREG5_FEE;
    public static long CH_HPREG_FEE_RATIO;
    public static int CH_HPREG1_FEE;
    public static int CH_HPREG2_FEE;
    public static int CH_HPREG3_FEE;
    public static int CH_HPREG4_FEE;
    public static int CH_HPREG5_FEE;
    public static int CH_HPREG6_FEE;
    public static int CH_HPREG7_FEE;
    public static int CH_HPREG8_FEE;
    public static int CH_HPREG9_FEE;
    public static int CH_HPREG10_FEE;
    public static int CH_HPREG11_FEE;
    public static int CH_HPREG12_FEE;
    public static int CH_HPREG13_FEE;
    public static long CH_EXPREG_FEE_RATIO;
    public static int CH_EXPREG1_FEE;
    public static int CH_EXPREG2_FEE;
    public static int CH_EXPREG3_FEE;
    public static int CH_EXPREG4_FEE;
    public static int CH_EXPREG5_FEE;
    public static int CH_EXPREG6_FEE;
    public static int CH_EXPREG7_FEE;
    public static long CH_SUPPORT_FEE_RATIO;
    public static int CH_SUPPORT1_FEE;
    public static int CH_SUPPORT2_FEE;
    public static int CH_SUPPORT3_FEE;
    public static int CH_SUPPORT4_FEE;
    public static int CH_SUPPORT5_FEE;
    public static int CH_SUPPORT6_FEE;
    public static int CH_SUPPORT7_FEE;
    public static int CH_SUPPORT8_FEE;
    public static long CH_CURTAIN_FEE_RATIO;
    public static int CH_CURTAIN1_FEE;
    public static int CH_CURTAIN2_FEE;
    public static long CH_FRONT_FEE_RATIO;
    public static int CH_FRONT1_FEE;
    public static int CH_FRONT2_FEE;
    /**
     * GeoData 0/1/2
     */
    public static int GEODATA;
    public static int GEO_TYPE;
    /**
     * Force loading GeoData to psychical memory
     */
    public static boolean FORCE_GEODATA;
    public static boolean GEO_SHOW_LOAD;
    public static boolean ACCEPT_GEOEDITOR_CONN;
    public static int MAP_MIN_X;
    public static int MAP_MAX_X;
    public static int MAP_MIN_Y;
    public static int MAP_MAX_Y;
    public static String GEO_L2J_PATH;
    public static String GEO_OFF_PATH;
    public static boolean PATHFIND_OPTIMIZE;
    /**
     * Max amount of buffs
     */
    public static int BUFFS_MAX_AMOUNT;
    public static int BUFFS_PET_MAX_AMOUNT;
    /**
     * Alt Settings for devs
     */
    public static boolean ALT_DEV_NO_QUESTS;
    public static boolean ALT_DEV_NO_SPAWNS;
    public static int MASTERACCESS_LEVEL;
    /**
     * Сервисы
     *
     */
    public static final String SERVICE_FILE = "./config/services.cfg";
    public static int STOCK_SERTIFY;
    public static int SERTIFY_PRICE;
    public static int FIRST_BALANCE;
    public static int DONATE_COIN;
    public static String DONATE_COIN_NEMA;
    public static int DONATE_RATE;
    public static int NALOG_NPS;
    public static String VAL_NAME;
    public static int PAGE_LIMIT;
    // перенос заточки и аугментации
    public static int AUGMENT_COIN;
    public static int ENCHANT_COIN;
    public static String AUGMENT_COIN_NAME;
    public static String ENCHANT_COIN_NAME;
    public static int AUGMENT_PRICE;
    public static int ENCHANT_PRICE;
    // сервисы кланам
    public static int CLAN_COIN;
    public static String CLAN_COIN_NAME;
    public static int CLAN_POINTS;
    public static int CLAN_POINTS_PRICE;
    public static int CLAN_SKILLS_PRICE;
    // продажа аугментации
    public static int AUGSALE_COIN; // ID итема
    public static int AUGSALE_PRICE; // цена
    public static String AUGSALE_COIN_NAME; // название итема
    public static final FastMap<Integer, Integer> AUGSALE_TABLE = new FastMap<Integer, Integer>()
            .shared("Config.AUGSALE_TABLE");
    // продажа Skill of Balance
    public static int SOB_ID; // ID скилла
    public static int SOB_NPC; // ID нпц для кубика (99997 - фея)
    public static int SOB_COIN; // ID итема
    public static int SOB_PRICE_ONE; // цена 2 недели
    public static int SOB_PRICE_TWO; // цена на месяц
    public static String SOB_COIN_NAME; // название итема
    // донейт шоп
    public static boolean ALLOW_DSHOP;
    public static boolean ALLOW_DSKILLS;
    // китайский шоп
    public static boolean ALLOW_CSHOP;
    // public static int DSHOP_COIN; // цена на месяц
    // public static String DSHOP_COIN_NEMA; // название итема
    // аукцион
    public static int BBS_AUC_ITEM_COIN;
    public static int BBS_AUC_ITEM_PRICE;
    public static String BBS_AUC_ITEM_NAME;
    public static int BBS_AUC_AUG_COIN;
    public static int BBS_AUC_AUG_PRICE;
    public static String BBS_AUC_AUG_NAME;
    public static int BBS_AUC_SKILL_COIN;
    public static int BBS_AUC_SKILL_PRICE;
    public static String BBS_AUC_SKILL_NAME;
    public static int BBS_AUC_HERO_COIN;
    public static int BBS_AUC_HERO_PRICE;
    public static String BBS_AUC_HERO_NAME;
    public static final FastMap<Integer, String> BBS_AUC_MONEYS = new FastMap<Integer, String>()
            .shared("Config.BBS_AUC_MONEYS");
    public static int BBS_AUC_EXPIRE_DAYS;
    /**
     * Баффер
     *
     */
    // профили
    public static int BUFFER_ID;
    public static boolean BUFF_CANCEL;
    public static final FastMap<Integer, Integer> M_BUFF = new FastMap<Integer, Integer>().shared("Config.M_BUFF");
    public static final FastMap<Integer, Integer> F_BUFF = new FastMap<Integer, Integer>().shared("Config.F_BUFF");
    public static final FastTable<Integer> F_PROFILE_BUFFS = new FastTable<Integer>();
    // доп. разрешенные баффы
    public static final FastTable<Integer> C_ALLOWED_BUFFS = new FastTable<Integer>();
    /**
     * Почта
     *
     */
    public static boolean POST_CHARBRIEF;
    public static String POST_BRIEFAUTHOR;
    public static String POST_BRIEFTHEME;
    public static String POST_BRIEFTEXT;
    public static String POST_NPCNAME;
    public static EventReward POST_BRIEF_ITEM;
    /**
     * КлассМастер
     *
     */
    public static String MASTER_NPCNAME;
    public static int MCLAN_COIN;
    public static String MCLAN_COIN_NAME;
    public static int CLAN_LVL6;
    public static int CLAN_LVL7;
    public static int CLAN_LVL8;

    /**
     * Евенты
     *
     */
    public static class EventReward {

        public int id;
        public int count;
        public int count_max;
        public int chance;

        public EventReward(int id, int count, int chance) {
            this.id = id;
            this.count = count;
            this.chance = chance;
        }

        public EventReward(int id, int count_min, int count_max, int chance) {
            this.id = id;
            this.count = count_min;
            this.count_max = count_max;
            this.chance = chance;
        }
    }

    // МассПвп
    public static final String EVENT_FILE = "./config/events.cfg";
    public static boolean MASS_PVP;
    public static long MPVP_RTIME;
    public static long MPVP_REG;
    public static long MPVP_ANC;
    public static long MPVP_TP;
    public static long MPVP_PR;
    public static long MPVP_WT;
    public static long MPVP_MAX;
    public static long MPVP_NEXT;
    public static int MPVP_MAXC;
    public static int MPVP_NPC; // 20788
    public static Location MPVP_NPCLOC; // 116530, 76141, -2730
    // public static Location MPVP_TPLOC; // 116530, 76141, -2730
    public static FastList<Location> MPVP_TPLOC = new FastList<Location>();
    public static FastList<Location> MPVP_CLOC = new FastList<Location>();
    // public static FastList<Location> MPVP_WLOC = new FastList<Location>();
    public static int MPVP_CREW; // 4355
    public static int MPVP_CREWC; // 4355
    public static int MPVP_EREW; // 4355
    public static int MPVP_EREWC; // 4355
    public static int MPVP_LVL;
    public static int MPVP_MAXP;
    public static boolean MPVP_NOBL;
    public static boolean TVT_NOBL;
    // Нападение на Шуттгарт
    public static boolean ALLOW_SCH;
    public static int SCH_TIMEBOSS; // время жизни босса
    public static int SCH_TIME1; // Время до начала спауна монстров, после обьявления о начале евента
    public static int SCH_TIME2; // 2 волна мобов
    public static int SCH_TIME3; // 3 волна мобов
    public static int SCH_TIME4; // 4 волна мобов
    public static int SCH_TIME5; // 5 волна мобов
    public static int SCH_TIME6; // 6 волна мобов
    public static long SCH_NEXT; // повтороное проведение евента через
    public static int SCH_RESTART; // время запуска после перезагрузки скрипта, если удалить значения в бд
    public static int SCH_MOB1; // мобы первой волны
    public static int SCH_MOB2; // мобы первой волны
    public static int SCH_MOB3; // мобы первой волны
    public static int SCH_MOB4; // мобы первой волны
    public static int SCH_MOB5; // мобы первой волны
    public static int SCH_MOB6; // мобы первой волны
    public static int SCH_BOSS; // Boss
    public static boolean SCH_ALLOW_SHOP;
    public static int SCH_SHOP; // нпц которого спавним после победы над монстрами
    public static int SCH_SHOPTIME; // нпц которого спавним после победы над монстрами
    // Сезон охоты
    public static boolean OPEN_SEASON;
    public static int OS_NEXT;
    public static int OS_RESTART;
    public static int OS_BATTLE;
    public static int OS_FAIL_NEXT;
    public static int OS_REGTIME;
    public static int OS_ANNDELAY;
    public static int OS_MINLVL;
    public static int OS_MINPLAYERS;
    public static final FastList<EventReward> OS_REWARDS = new FastList<EventReward>();
    // Последний герой
    public static boolean ELH_ENABLE;
    public static long ELH_ARTIME;
    public static long ELH_REGTIME;
    public static long ELH_ANNDELAY;
    public static long ELH_TPDELAY;
    public static long ELH_NEXT;
    public static int ELH_MINLVL;
    public static int ELH_MINP;
    public static int ELH_MAXP;
    public static int ELH_NPCID;
    public static Location ELH_NPCLOC;
    public static String ELH_NPCTOWN;
    public static Location ELH_TPLOC;
    public static int ELH_TICKETID;
    public static int ELH_TICKETCOUNT;
    public static final FastList<EventReward> ELH_REWARDS = new FastList<EventReward>();
    public static int ELH_HERO_DAYS;
    public static boolean ELH_HIDE_NAMES;
    public static boolean ELH_FORBID_MAGIC;
    public static String ELH_ALT_NAME;
    public static final FastList<Integer> ELH_FORB_POTIONS = new FastList<Integer>();
    // бк
    public static final FastTable<Integer> FC_ALLOWITEMS = new FastTable<Integer>();
    // рождество
    public static boolean ALLOW_XM_SPAWN;
    public static final FastList<EventReward> XM_DROP = new FastList<EventReward>();
    public static long XM_TREE_LIFE;
    // медальки
    public static boolean ALLOW_MEDAL_EVENT;
    public static final FastList<EventReward> MEDAL_EVENT_DROP = new FastList<EventReward>();
    // захват базы
    public static boolean EBC_ENABLE;
    public static long EBC_ARTIME;
    public static long EBC_REGTIME;
    public static long EBC_ANNDELAY;
    public static long EBC_TPDELAY;
    public static long EBC_DEATHLAY;
    public static long EBC_NEXT;
    public static int EBC_MINLVL;
    public static int EBC_MINP;
    public static int EBC_MAXP;
    public static int EBC_NPCID;
    public static Location EBC_NPCLOC;
    public static String EBC_NPCTOWN;
    public static int EBC_BASE1ID;
    public static String EBC_BASE1NAME;
    public static int EBC_BASE2ID;
    public static String EBC_BASE2NAME;
    public static Location EBC_BASE1_LOC;
    public static Location EBC_BASE2_LOC;
    public static int EBC_TICKETID;
    public static int EBC_TICKETCOUNT;
    public static final FastList<EventReward> EBC_REWARDS = new FastList<EventReward>();
    public static String EBC_BASE1TITLE;
    public static String EBC_BASE2TITLE;
    public static FastList<Location> EBC_TPLOC1 = new FastList<Location>();
    public static FastList<Location> EBC_TPLOC2 = new FastList<Location>();
    public static FastList<Location> EBC_RETURN_COORDINATES = new FastList<Location>();
    // дозор
    public static boolean EENC_ENABLE;
    public static long EENC_ARTIME;
    public static long EENC_REGTIME;
    public static long EENC_ANNDELAY;
    public static long EENC_TPDELAY;
    public static long EENC_FINISH;
    public static long EENC_NEXT;
    public static int EENC_MINLVL;
    public static int EENC_MINP;
    public static int EENC_MAXP;
    public static int EENC_NPCID;
    public static Location EENC_NPCLOC;
    public static String EENC_NPCTOWN;
    public static Location EENC_TPLOC;
    public static int EENC_TICKETID;
    public static int EENC_TICKETCOUNT;
    public static final FastList<EventReward> EENC_REWARDS = new FastList<EventReward>();
    public static final FastMap<Integer, FastList<Location>> EENC_POINTS = new FastMap<Integer, FastList<Location>>()
            .shared("Config.EENC_POINTS");
    // анархия
    public static boolean ANARCHY_ENABLE;
    public static int ANARCHY_DAY;
    public static int ANARCHY_HOUR;
    public static long ANARCHY_DELAY;
    public static final FastList<Integer> ANARCHY_TOWNS = new FastList<Integer>();
    // замес
    public static boolean FIGHTING_ENABLE;
    public static int FIGHTING_DAY;
    public static int FIGHTING_HOUR;
    public static long FIGHTING_REGTIME;
    public static long FIGHTING_ANNDELAY;
    public static long FIGHTING_TPDELAY;
    public static long FIGHTING_FIGHTDELAY;
    public static int FIGHTING_MINLVL;
    public static int FIGHTING_MINP;
    public static int FIGHTING_MAXP;
    public static int FIGHTING_TICKETID;
    public static int FIGHTING_TICKETCOUNT;
    public static int FIGHTING_NPCID;
    public static Location FIGHTING_NPCLOC;
    public static Location FIGHTING_TPLOC;
    // 1 ип - 1 игрок
    public static boolean EVENTS_SAME_IP;
    public static boolean TVT_SAME_HWID;
    /**
     * Разное
     *
     */
    // оптимизация
    public static int DEADLOCKCHECK_INTERVAL;
    public static int RESTART_HOUR;
    // накрутка онлайна
    public static boolean ALLOW_FAKE_PLAYERS;
    public static int FAKE_PLAYERS_PERCENT;
    public static int FAKE_PLAYERS_DELAY;
    // запрещенные итемы на олимпе
    public static final FastTable<Integer> F_OLY_ITEMS = new FastTable<Integer>();
    // Слать остальным пакеты, если невидим?
    public static boolean INVIS_SHOW;
    // Задержка на спаун НПЦ
    public static long NPC_SPAWN_DELAY;
    public static int NPC_SPAWN_TYPE;
    // заточка при покупке в мультиселле
    public static int MULT_ENCH;
    public static final FastMap<Integer, Integer> MULT_ENCHS = new FastMap<Integer, Integer>()
            .shared("Config.MULT_ENCHS");
    // последняя активность клана, дабы удалять кх с мертвых кланов
    public static long CLAN_CH_CLEAN;
    public static boolean CHECK_SKILLS;
    // снимать бафф при смерти, если не висел ноблесс
    public static boolean CLEAR_BUFF_ONDEATH;
    // накрутка без спауна ботов
    public static float ONLINE_PERC;
    // ключ
    public static String SERVER_SERIAL_KEY;
    /**
     * .menu
     *
     */
    public static boolean CMD_MENU;
    public static boolean VS_NOEXP;
    public static boolean VS_NOREQ;
    public static boolean VS_VREF;
    public static boolean VS_VREF_NAME;
    public static boolean VS_ONLINE;
    public static boolean VS_AUTORESTAT;
    public static boolean VS_CHATIGNORE;
    public static boolean VS_AUTOLOOT;
    public static boolean VS_TRADERSIGNORE;
    public static boolean VS_PATHFIND;
    public static boolean VS_SKILL_CHANCES;
    public static boolean VS_ANIM_SHOTS;
    // .security
    public static boolean VS_HWID;
    public static boolean VS_PWD;
    public static boolean VS_EMAIL;
    public static boolean CMD_ADENA_COL;
    public static EventReward CMD_AC_ADENA;
    public static EventReward CMD_AC_COL;
    public static int CMD_AC_ADENA_LIMIT;
    public static int CMD_AC_COL_LIMIT;
    public static boolean CMD_EVENTS;
    public static int MAX_BAN_CHAT;
    public static boolean VS_CKEY;
    public static boolean VS_CKEY_CHARLEVEL;
    /**
     * alt-b
     *
     */
    public static int PWHERO_COIN;
    public static int PWHERO_PRICE;
    public static int PWHERO_FPRICE;
    public static int PWHERO_MINDAYS;
    public static int PWHERO_TRANPRICE;
    public static String PWHERO_COINNAME;
    public static int PWCSKILLS_COIN;
    public static int PWCSKILLS_PRICE;
    public static String PWCSKILLS_COINNAME;
    public static int PWENCHSKILL_COIN;
    public static int PWENCHSKILL_PRICE;
    public static String PWENCHSKILL_COINNAME;
    public static int PWCNGSKILLS_COIN;
    public static int PWCNGSKILLS_PRICE;
    public static String PWCNGSKILLS_COINNAME;
    public static final FastMap<Integer, Integer> PWCSKILLS = new FastMap<Integer, Integer>()
            .shared("Config.PWCSKILLS");

    public static class AltBColor {

        public int hex;
        public String color;

        AltBColor(int hex, String color) {
            this.hex = hex;
            this.color = color;
        }
    }

    public static boolean PWTCOLOR_PAYMENT;
    public static int PWNCOLOR_COIN;
    public static int PWNCOLOR_PRICE;
    public static String PWNCOLOR_COINNAME;
    public static int PWTCOLOR_COIN;
    public static int PWTCOLOR_PRICE;
    public static String PWTCOLOR_COINNAME;
    public static final FastMap<Integer, AltBColor> PWCOLOURS = new FastMap<Integer, AltBColor>()
            .shared("Config.PWCOLOURS");
    public static int PWCNGCLASS_COIN;
    public static int PWCNGCLASS_PRICE;
    public static String PWCNGCLASS_COINNAME;
    // почта
    public static int EXPOSTB_COIN;
    public static int EXPOSTB_PRICE;
    public static String EXPOSTB_NAME;
    public static int EXPOSTA_COIN;
    public static int EXPOSTA_PRICE;
    public static String EXPOSTA_NAME;
    /**
     * Премиум
     *
     */
    public static boolean PREMIUM_ENABLE;
    public static int PREMIUM_COIN;
    public static int PREMIUM_PRICE;
    public static String PREMIUM_COINNAME;
    public static final FastMap<Integer, Integer> PREMIUM_DAY_PRICES = new FastMap<Integer, Integer>()
            .shared("Config.PREMIUM_DAY_PRICES");
    public static double PREMIUM_EXP;
    public static double PREMIUM_SP;
    public static double PREMIUM_ITEMDROP;
    public static double PREMIUM_ITEMDROPMUL;
    public static double PREMIUM_SPOILRATE;
    public static double PREMIUM_ADENAMUL;
    public static double PREMIUM_PCCAFE_MUL;
    public static double PREMIUM_AQURE_SKILL_MUL;
    public static int PREMIUM_AUGMENT_RATE;
    public static int PREMIUM_ENCH_ITEM;
    public static int PREMIUM_ENCH_SKILL;
    public static int PREMIUM_CURSED_RATE;
    public static boolean PREMIUM_ANY_SUBCLASS;
    public static boolean PREMIUM_CHKSKILLS;
    public static boolean PREMIUM_PKDROP_OFF;
    public static boolean PREMIUM_ANOOUNCE;
    public static boolean PREMIUM_ENCHANT_FAIL;
    public static String PREMIUM_ANNOUNCE_PHRASE;
    public static String PREMIUM_NAME_PREFIX;
    public static int PREMIUM_START_DAYS;
    public static int HERO_START_DAYS;
    public static final FastList<Integer> PREMIUM_PROTECTED_ITEMS = new FastList<Integer>();
    /**
     * Голосовалка
     *
     */
    public static String VOTE_SERVER_PREFIX;
    // l2top
    public static boolean L2TOP_ENABLE;
    public static String L2TOP_SERV_URL;
    public static int L2TOP_UPDATE_DELAY;
    public static int L2TOP_OFFLINE_ITEM;
    public static int L2TOP_OFFLINE_COUNT;
    public static String L2TOP_OFFLINE_LOC;
    public static final FastList<EventReward> L2TOP_ONLINE_REWARDS = new FastList<EventReward>();
    public static int L2TOP_LOGTYPE;
    // mmotop
    public static boolean MMOTOP_ENABLE;
    public static String MMOTOP_STAT_LINK;
    public static int MMOTOP_UPDATE_DELAY;
    public static int MMOTOP_OFFLINE_ITEM;
    public static int MMOTOP_OFFLINE_COUNT;
    public static String MMOTOP_OFFLINE_LOC;
    public static final FastList<EventReward> MMOTOP_ONLINE_REWARDS = new FastList<EventReward>();
    public static int MMOTOP_LOGTYPE;
    /**
     * Доп. настройки нпц
     *
     */
    public static boolean RAID_CUSTOM_DROP;
    public static final FastList<EventReward> NPC_RAID_REWARDS = new FastList<EventReward>();
    public static final FastList<EventReward> NPC_EPIC_REWARDS = new FastList<EventReward>();
    // taras
    public static long ANTARAS_CLOSE_PORT;
    public static long ANTARAS_UPDATE_LAIR;
    public static long ANTARAS_MIN_RESPAWN;
    public static long ANTARAS_MAX_RESPAWN;
    public static long ANTARAS_RESTART_DELAY;
    public static long ANTARAS_SPAWN_DELAY;
    // valik
    public static long VALAKAS_CLOSE_PORT;
    public static long VALAKAS_UPDATE_LAIR;
    public static long VALAKAS_MIN_RESPAWN;
    public static long VALAKAS_MAX_RESPAWN;
    public static long VALAKAS_RESTART_DELAY;
    public static long VALAKAS_SPAWN_DELAY;
    // baium
    public static long BAIUM_CLOSE_PORT;
    public static long BAIUM_UPDATE_LAIR;
    public static long BAIUM_MIN_RESPAWN;
    public static long BAIUM_MAX_RESPAWN;
    public static long BAIUM_RESTART_DELAY;
    // ant queen
    public static long AQ_MIN_RESPAWN;
    public static long AQ_MAX_RESPAWN;
    public static long AQ_RESTART_DELAY;
    public static long AQ_PLAYER_MAX_LVL;
    public static long AQ_NURSE_RESPAWN;
    // zaken
    public static long ZAKEN_MIN_RESPAWN;
    public static long ZAKEN_MAX_RESPAWN;
    public static long ZAKEN_RESTART_DELAY;
    public static long ZAKEN_PLAYER_MAX_LVL;
    // можно ли бить нпц
    public static boolean ALLOW_HIT_NPC;
    public static boolean KILL_NPC_ATTACKER;
    // анонс появления/смерти эпиков
    public static boolean ANNOUNCE_EPIC_STATES;
    /**
     * Альтернативная заточка
     *
     */
    public static boolean ENCHANT_ALT_PACKET;
    public static boolean ENCHANT_PENALTY;
    public static int ENCHANT_ALT_MAGICCAHNCE;
    public static int ENCHANT_ALT_MAGICCAHNCE_BLESS;
    public static final FastMap<Integer, Integer> ENCHANT_ALT_MAGICSTEPS = new FastMap<Integer, Integer>()
            .shared("Config.ENCHANT_ALT_MAGICSTEPS");
    public static int ENCHANT_ALT_WEAPONCAHNCE;
    public static int ENCHANT_ALT_WEAPONCAHNCE_BLESS;
    public static final FastMap<Integer, Integer> ENCHANT_ALT_WEAPONSTEPS = new FastMap<Integer, Integer>()
            .shared("Config.ENCHANT_ALT_WEAPONSTEPS");
    // public static int ENCHANT_ALT_WEAPONFAIL;
    public static int ENCHANT_ALT_WEAPONFAILBLESS;
    public static int ENCHANT_ALT_WEAPONFAILBLESS1;
    public static int ENCHANT_ALT_WEAPONFAILBLESS2;
    public static int ENCHANT_ALT_WEAPONFAILBLESS3;
    public static int ENCHANT_ALT_WEAPONFAILBLESS4;
    public static int ENCHANT_ALT_WEAPONFAILBLESS5;
    public static int ENCHANT_ALT_WEAPONFAILBLESS6;
    public static int ENCHANT_ALT_WEAPONFAILBLESS7;
    public static int ENCHANT_ALT_WEAPONFAILBLESS8;
    public static int ENCHANT_ALT_WEAPONFAILBLESS9;
    public static int ENCHANT_ALT_WEAPONFAILCRYST;
    public static int ENCHANT_ALT_ARMORCAHNCE;
    public static int ENCHANT_ALT_ARMORCAHNCE_BLESS;
    public static final FastMap<Integer, Integer> ENCHANT_ALT_ARMORSTEPS = new FastMap<Integer, Integer>()
            .shared("Config.ENCHANT_ALT_ARMORSTEPS");
    // public static int ENCHANT_ALT_ARMORFAIL;
    public static int ENCHANT_ALT_ARMORFAILBLESS;
    public static int ENCHANT_ALT_ARMORFAILBLESS1;
    public static int ENCHANT_ALT_ARMORFAILBLESS2;
    public static int ENCHANT_ALT_ARMORFAILBLESS3;
    public static int ENCHANT_ALT_ARMORFAILBLESS4;
    public static int ENCHANT_ALT_ARMORFAILBLESS5;
    public static int ENCHANT_ALT_ARMORFAILBLESS6;
    public static int ENCHANT_ALT_ARMORFAILBLESS7;
    public static int ENCHANT_ALT_ARMORFAILBLESS8;
    public static int ENCHANT_ALT_ARMORFAILBLESS9;
    public static int ENCHANT_ALT_ARMORFAILCRYST;
    public static int ENCHANT_ALT_JEWERLYCAHNCE;
    public static int ENCHANT_ALT_JEWERLYCAHNCE_BLESS;
    public static final FastMap<Integer, Integer> ENCHANT_ALT_JEWERLYSTEPS = new FastMap<Integer, Integer>()
            .shared("Config.ENCHANT_ALT_JEWERLYSTEPS");
    // public static int ENCHANT_ALT_JEWERLYFAIL;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS1;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS2;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS3;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS4;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS5;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS6;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS7;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS8;
    public static int ENCHANT_ALT_JEWERLYFAILBLESS9;
    public static int ENCHANT_ALT_JEWERLYFAILCRYST;
    public static boolean ENCHANT_HERO_WEAPONS;
    // антиклик
    public static boolean ENCH_ANTI_CLICK;
    public static int ENCH_ANTI_CLICK_STEP;
    public static int ENCH_ANTI_CLICK_TYPE;
    //
    public static final FastList<Integer> BLESS_BONUSES = new FastList<Integer>();
    public static final FastList<Integer> BLESS_BONUSES2 = new FastList<Integer>();
    public static int BLESS_BONUS_ENCH1;
    public static int BLESS_BONUS_ENCH2;
    public static boolean ENCH_SHOW_CHANCE;
    // шаг заточки
    public static int ENCHANT_ALT_STEP;
    //
    public static boolean WALK_FAR;

    /**
     * бонус за пвп
     */
    public static class PvpColor {

        public int nick;
        public int title;
        public String type;

        PvpColor(int nick, int title) {
            this.nick = nick;
            this.title = title;
        }

        PvpColor(String type, int nick, int title) {
            this.type = type;
            this.nick = nick;
            this.title = title;
        }
    }

    public static class PvpTitleBonus {

        public int min;
        public int max;
        public String title;
        public int skillId;
        public int skillLvl;

        PvpTitleBonus(String title, int min, int max, int skillId, int skillLvl) {
            this.title = title;
            this.min = min;
            this.max = max;
            this.skillId = skillId;
            this.skillLvl = skillLvl;
        }

        public boolean isInRange(int pvp) {
            return (pvp >= min && pvp <= max);
        }
    }

    public static boolean ALLOW_PVPPK_REWARD;
    public static int PVPPK_INTERVAL;
    public static PvpColor PVPPK_PENALTY;
    public static boolean PVPPK_IPPENALTY;
    public static boolean PVPPK_HWIDPENALTY;
    public static boolean EVENT_KILL_REWARD;
    public static boolean TVT_KILL_REWARD;
    public static boolean MASSPVP_KILL_REWARD;
    public static boolean LASTHERO_KILL_REWARD;
    public static boolean CAPBASE_KILL_REWARD;
    public static final FastList<PvpColor> PVPPK_EXP_SP = new FastList<PvpColor>();
    public static final FastList<EventReward> PVPPK_PVPITEMS = new FastList<EventReward>();
    public static final FastList<EventReward> EVENT_KILLSITEMS = new FastList<EventReward>();
    public static final FastList<EventReward> TVT_KILLSITEMS = new FastList<EventReward>();
    public static final FastList<EventReward> MASSPVP_KILLSITEMS = new FastList<EventReward>();
    public static final FastList<EventReward> CAPBASE_KILLSITEMS = new FastList<EventReward>();
    public static final FastList<EventReward> LASTHERO_KILLSITEMS = new FastList<EventReward>();
    public static final FastList<EventReward> PVPPK_PKITEMS = new FastList<EventReward>();
    public static boolean ALLOW_PVPBONUS_STEPS;
    public static final FastMap<Integer, PvpColor> PVPBONUS_ITEMS = new FastMap<Integer, PvpColor>()
            .shared("Config.PVPBONUS_ITEMS");
    public static final FastList<PvpTitleBonus> PVPBONUS_TITLE = new FastList<PvpTitleBonus>();
    public static final FastMap<Integer, PvpColor> PVPBONUS_COLORS = new FastMap<Integer, PvpColor>()
            .shared("Config.PVPBONUS_COLORS");
    public static final FastList<Integer> PVPBONUS_COLORS_NAME = new FastList<Integer>();
    public static final FastList<Integer> PVPBONUS_COLORS_TITLE = new FastList<Integer>();
    public static int PVPPK_STEP;
    public static long PVPPK_STEPBAN;
    public static boolean PVPPK_REWARD_ZONE;
    public static boolean PVP_TITLE_SKILL;
    // итем для открытия мультиселлов
    public static final FastMap<Integer, EventReward> MULTVIP_CARDS = new FastMap<Integer, EventReward>()
            .shared("Config.MULTVIP_CARDS");
    // награда за захват замка
    public static boolean SIEGE_REWARD_CLAN;
    public static boolean CASTLE_SIEGE_REWARD_STATIC;
    public static final FastMap<Integer, FastList<EventReward>> CASTLE_SIEGE_REWARDS = new FastMap<Integer, FastList<EventReward>>()
            .shared("Config.CASTLE_SIEGE_REWARDS");
    public static final FastMap<Integer, FastList<EventReward>> CASTLE_RPOTECT_REWARDS = new FastMap<Integer, FastList<EventReward>>()
            .shared("Config.CASTLE_RPOTECT_REWARDS");
    // пассивки апеллы
    public static boolean ALLOW_APELLA_BONUSES;
    // альт-б в пис зоне
    public static boolean BBS_ONLY_PEACE;
    // разрешенные поты на твт
    public static final FastList<Integer> TVT_WHITE_POTINS = new FastList<Integer>();
    ///////////////
    public static int ALT_OLY_REG_DISPLAY = 100;
    public static int ALT_OLY_BATTLE_REWARD_ITEM = 13722;
    public static int ALT_OLY_CLASSED_RITEM_C = 50;
    public static int ALT_OLY_NONCLASSED_RITEM_C = 40;
    public static int ALT_OLY_RANDOM_TEAM_RITEM_C = 30;
    public static int ALT_OLY_TEAM_RITEM_C = 50;
    public static int ALT_OLY_COMP_RITEM = 13722;
    public static int ALT_OLY_GP_PER_POINT = 1000;
    public static int ALT_OLY_HERO_POINTS = 180;
    public static int ALT_OLY_RANK1_POINTS = 120;
    public static int ALT_OLY_RANK2_POINTS = 80;
    public static int ALT_OLY_RANK3_POINTS = 55;
    public static int ALT_OLY_RANK4_POINTS = 35;
    public static int ALT_OLY_RANK5_POINTS = 20;
    // предупреждения при входе в игру
    public static boolean SHOW_ENTER_WARNINGS = false;
    // pc cafe
    public static boolean PC_CAFE_ENABLED;
    public static int PC_CAFE_INTERVAL;
    public static PvpColor PC_CAFE_BONUS;
    public static int PC_CAFE_DOUBLE_CHANCE;
    // webserver
    public static int WEBSERVER_REFRESH_STATS;
    public static int WEBSERVER_PORT;
    public static String WEBSERVER_FOLDER;
    public static String WEBSERVER_PAGE;
    // увеличение времени баффов
    public static int ALT_BUFF_TIMEMUL;
    public static final FastMap<Integer, Integer> ALT_BUFF_TIME = new FastMap<Integer, Integer>()
            .shared("Config.ALT_BUFF_TIME");
    // шансы прохождения скиллов
    public static final FastMap<Integer, Integer> ALT_SKILL_CHANSE = new FastMap<Integer, Integer>()
            .shared("Config.ALT_SKILL_CHANSE");
    // одевать геройские пухи не героям
    public static boolean HERO_ITEMS_PENALTY;
    // доп маг пухи
    public static final FastList<Integer> ALT_MAGIC_WEAPONS = new FastList<Integer>();
    // отдельные итемы при старте
    public static final FastMap<Integer, Integer> CUSTOM_STRT_ITEMS = new FastMap<Integer, Integer>()
            .shared("Config.CUSTOM_STRT_ITEMS");
    // альтернативная академия
    public static boolean ACADEMY_CLASSIC;
    public static int ACADEMY_POINTS;
    public static boolean ACADEMY_BONUS_ON;
    // отключить баттл/спелл форсы
    public static boolean DISABLE_FORCES;
    // макс енчант в трейде
    public static int MAX_TRADE_ENCHANT;
    // альтернативная продолжительность олимпа
    public static int ALT_OLYMPIAD_PERIOD;
    // взятие квестов с проклятым оружием
    public static boolean ALLOW_CURSED_QUESTS;
    public static boolean BBS_CURSED_SHOP;
    public static boolean BBS_CURSED_TELEPORT;
    public static boolean BBS_CURSED_BUFF;
    // чат фильтр
    public static String CHAT_FILTER_STRING;
    public static final FastTable<String> CHAT_FILTER_STRINGS = new FastTable<String>();
    // интервал осад, дни
    public static int ALT_SIEGE_INTERVAL;
    // лимит на заточку опр. вещей
    public static final FastMap<Integer, Integer> ENCHANT_LIMITS = new FastMap<Integer, Integer>()
            .shared("Config.ENCHANT_LIMITS");
    // анимация сулшотов
    public static boolean SOULSHOT_ANIM;
    // запретить хилить таргет за воротами
    public static boolean PROTECT_GATE_PVP;
    // запрет соба на олимпе
    public static boolean PROTECT_OLY_SOB;
    // время на принятие запросов
    public static int WEDDING_ANSWER_TIME;
    public static int RESURECT_ANSWER_TIME;
    public static int SUMMON_ANSWER_TIME;
    // макс. бонус тату
    public static int MAX_HENNA_BONUS;
    // скиллы с фиксированным откатом
    public static final FastList<Integer> ALT_FIXED_REUSES = new FastList<Integer>();
    public static final FastList<Integer> ALT_FIXED_CASTS = new FastList<Integer>();
    // макс. баффов аугментов
    public static int MAX_AUGMENTS_BUFFS;
    // Возможность взятия любого саб-класса
    public static boolean ALT_ANY_SUBCLASS;
    // Возможность взятия любого саб-класса + овер и крафт
    public static boolean ALT_ANY_SUBCLASS_OVERCRAF;
    public static boolean SUBS_3RD_PROFF;
    // ивент зоны
    public static EventTerritory TVT_POLY;
    public static EventTerritory LASTHERO_POLY;
    public static EventTerritory MASSPVP_POLY;
    public static EventTerritory BASECAPTURE_POLY;
    // Возможность взятия любого саб-класса
    public static boolean ALT_AUGMENT_HERO;
    // Скиллы с резистом к канцелу
    public static final FastList<Integer> PROTECTED_BUFFS = new FastList<Integer>();
    // потолки шансов прохождения скиллов, импорт l2p
    public static double SKILLS_CHANCE_MIN;
    public static double SKILLS_CHANCE_MAX;
    public static double SKILLS_CHANCE_MIN_OLY;
    public static double SKILLS_CHANCE_MAX_OLY;
    // титул при создании перса
    public static String STARTUP_TITLE;
    // время на поднятие дропа
    public static long PICKUP_PENALTY;
    // заставки к боссам
    public static boolean DISABLE_BOSS_INTRO;
    // смертельный рефлект
    public static boolean DEATH_REFLECT;
    // восстановление оффтрейдеров после рестарта
    public static boolean ALT_RESTORE_OFFLINE_TRADE;
    public static long ALT_OFFLINE_TRADE_LIMIT;
    // накрутка онлайна, улучшенная
    public static boolean ALLOW_FAKE_PLAYERS_PLUS;
    public static int FAKE_PLAYERS_PLUS_COUNT;
    public static long FAKE_PLAYERS_PLUS_DELAY_SPAWN;
    public static int FAKE_PLAYERS_PLUS_COUNT_FIRST;
    public static long FAKE_PLAYERS_PLUS_DELAY_FIRST;
    public static int FAKE_PLAYERS_PLUS_DELAY_SPAWN_FIRST;
    public static int FAKE_PLAYERS_PLUS_DELAY_DESPAWN_FIRST;
    public static long FAKE_PLAYERS_PLUS_DESPAWN_FIRST;
    public static int FAKE_PLAYERS_PLUS_COUNT_NEXT;
    public static long FAKE_PLAYERS_PLUS_DELAY_NEXT;
    public static int FAKE_PLAYERS_PLUS_DELAY_SPAWN_NEXT;
    public static int FAKE_PLAYERS_PLUS_DELAY_DESPAWN_NEXT;
    public static long FAKE_PLAYERS_PLUS_DESPAWN_NEXT;
    public static PvpColor FAKE_PLAYERS_ENCHANT;
    public static final FastList<Integer> FAKE_PLAYERS_NAME_CLOLORS = new FastList<Integer>();
    public static final FastList<Integer> FAKE_PLAYERS_TITLE_CLOLORS = new FastList<Integer>();
    // другой статус онлайна у офф.трейдеров
    public static boolean ALT_OFFLINE_TRADE_ONLINE;
    // антифлуд на повторные фразы в чат
    public static boolean PROTECT_SAY;
    public static long PROTECT_SAY_BAN;
    public static int PROTECT_SAY_COUNT;
    public static long PROTECT_SAY_INTERVAL;
    // статистика
    public static boolean CACHED_SERVER_STAT;
    // падения с высоты
    public static boolean ALLOW_FALL;
    // дроп шмоток при смерте от мобов
    public static boolean KARMA_PK_NPC_DROP;
    // заточка пух у нпц
    public static int ENCH_NPC_CAHNCE;
    public static PvpColor ENCH_NPC_MINMAX;
    public static int ENCH_MONSTER_CAHNCE;
    public static PvpColor ENCH_MONSTER_MINMAX;
    public static int ENCH_GUARD_CAHNCE;
    public static PvpColor ENCH_GUARD_MINMAX;
    // стекбл скроллы заточки
    public static boolean ENCH_STACK_SCROLLS;
    // валюта для покупки кланхолла
    public static int CLANHALL_PAYMENT;
    // хук, шанс миража
    public static int MIRAGE_CHANCE;
    // что не варили цп нонстопом
    public static boolean SUMMON_CP_PROTECT;
    // Запрет на ношение HEAVY
    public static final FastList<Integer> HEAVY_PENALTY = new FastList<Integer>();
    // Запрет на ношение LIGHT
    public static final FastList<Integer> LIGHT_PENALTY = new FastList<Integer>();
    // Запрет на ношение ROBE
    public static final FastList<Integer> MAGIC_PENALTY = new FastList<Integer>();
    // Запрет на ношение SHIELD
    public static final FastList<Integer> SHIELD_PENALTY = new FastList<Integer>();
    // запрет на атаку луком конкретным профам
    public static final FastList<Integer> FORBIDDEN_BOW_CLASSES = new FastList<Integer>();
    public static final FastList<Integer> FORBIDDEN_BOW_CLASSES_OLY = new FastList<Integer>();
    public static final FastList<Integer> FORBIDDEN_FIST_CLASSES = new FastList<Integer>();
    public static final FastList<Integer> FORBIDDEN_DUAL_CLASSES = new FastList<Integer>();
    // фразы мобов
    public static boolean ALLOW_NPC_CHAT;
    public static int MNPC_CHAT_CHANCE;
    // frinta
    public static int FRINTA_MMIN_PARTIES;
    public static int FRINTA_MMIN_PLAYERS;
    // запрещенные шмотки на ивентах
    public static boolean FORBIDDEN_EVENT_ITMES;
    // cтандартные значения параметров чара
    public static int VS_AUTOLOOT_VAL;
    public static int VS_PATHFIND_VAL;
    public static int VS_SKILL_CHANCES_VAL;
    // свадьбы цвет ника
    public static PvpColor WEDDING_COLORS;
    // олимп баффы
    public static final FastMap<Integer, Integer> OLY_MAGE_BUFFS = new FastMap<Integer, Integer>()
            .shared("Config.OLY_MAGE_BUFFS");
    public static final FastMap<Integer, Integer> OLY_FIGHTER_BUFFS = new FastMap<Integer, Integer>()
            .shared("Config.OLY_FIGHTER_BUFFS");
    // отключение привязки мультиселлов
    public static boolean MULTISSELL_PROTECT;
    // отключение ошибок загрузки мультиселлов
    public static boolean MULTISSELL_ERRORS;
    // шансы открытия сундуков
    public static int CHEST_CHANCE;
    // web статистика
    public static boolean WEBSTAT_ENABLE;
    public static int WEBSTAT_INTERVAL;
    public static int WEBSTAT_INTERVAL2;
    public static int WEBSTAT_INTERVAL3;
    public static boolean WEBSTAT_KILLS;
    public static boolean WEBSTAT_CHEATS;
    public static int WEBSTAT_ENCHANT;
    public static boolean WEBSTAT_EPICLOOT;
    // откат скиллов, оли
    public static boolean ALT_OLY_RELOAD_SKILLS;
    // шансы прохождениея дебаффов от мобов
    public static int MOB_DEBUFF_CHANCE;
    // отложенные итемы
    public static boolean QUED_ITEMS_ENABLE;
    public static int QUED_ITEMS_INTERVAL;
    public static int QUED_ITEMS_LOGTYPE;
    // рейты дропа сил стонов
    public static double RATE_MUL_SEAL_STONE;
    public static float RATE_DROP_SEAL_STONE;
    // специальный дроп
    public static boolean EVENT_SPECIAL_DROP;
    // умножения выпавших шмоток в сб и эпиков
    public static double RATE_DROP_ITEMSRAIDMUL;
    public static double RATE_DROP_ITEMSGRANDMUL;
    // бк выдавать шмотку в инвентарь
    public static boolean FC_INSERT_INVENTORY;
    // макс. точка на олимпе
    public static int OLY_MAX_WEAPON_ENCH;
    public static int OLY_MAX_ARMOT_ENCH;
    // соски и мана на панели при старте перса.
    public static boolean ALT_START_ITEMS;
    // этих нпц нельзя бить
    public static final FastList<Integer> NPC_HIT_PROTECTET = new FastList<Integer>();
    // GUI консолька
    public static boolean CONSOLE_ADVANCED;
    // ноблесс за убийство Баракиеля
    public static boolean BARAKIEL_NOBLESS;
    /**
     * guard
     */
    public static boolean GAMEGUARD_ENABLED;
    public static int GAMEGUARD_KEY = -1;
    public static int GAMEGUARD_INTERVAL;
    public static int GAMEGUARD_PUNISH;
    public static boolean GAMEGUARD_LOG;
    // покупка ноблесса
    public static boolean NOBLES_ENABLE;
    public static int SNOBLE_COIN;
    public static int SNOBLE_PRICE;
    public static String SNOBLE_COIN_NAME;
    // ancientwar
    // конфиг на макс левел т.е указывал число и после указанного лвла чар не
    // качаеться
    public static int MAX_EXP_LEVEL;
    public static boolean FREE_PVP;
    public static boolean PROTECT_GRADE_PVP;
    // обнуление очков олимпа после бана
    public static boolean CLEAR_OLY_BAN;
    // запрет передачи итемов пету
    public static boolean GIVE_ITEM_PET;
    // отключение кормежки петов
    public static boolean DISABLE_PET_FEED;
    // альтернативная формула ентчанта
    public static boolean ENCHANT_ALT_FORMULA;
    // спавн гвардов на осадах
    public static boolean SIEGE_GUARDS_SPAWN;
    // защита на тп
    public static long TELEPORT_PROTECTION;
    // макс. значение скорости каста и атаки для подсчета отката скиллов
    public static int MAX_MATKSPD_DELAY;
    public static int MAX_PATKSPD_DELAY;
    // макс. значение физ/маг атаки для подсчета прохождения дебаффов
    public static int MAX_MATK_CALC;
    public static int MAX_MDEF_CALC;
    // мин. значение задержки скорости атаки
    public static int MIN_ATKSPD_DELAY;
    // бонусные скиллы за взятие замка
    public static boolean CASTLE_SIEGE_SKILLS_DELETE;
    public static final FastMap<Integer, EventReward> CASTLE_SIEGE_SKILLS = new FastMap<Integer, EventReward>()
            .shared("Config.CASTLE_SIEGE_SKILLS");
    /// макс статы ботов
    public static int FAKE_MAX_PATK_BOW;
    public static int FAKE_MAX_MDEF_BOW;
    public static int FAKE_MAX_PSPD_BOW;
    public static int FAKE_MAX_PDEF_BOW;
    public static int FAKE_MAX_MATK_BOW;
    public static int FAKE_MAX_MSPD_BOW;
    public static int FAKE_MAX_HP_BOW;
    public static int FAKE_MAX_PATK_MAG;
    public static int FAKE_MAX_MDEF_MAG;
    public static int FAKE_MAX_PSPD_MAG;
    public static int FAKE_MAX_PDEF_MAG;
    public static int FAKE_MAX_MATK_MAG;
    public static int FAKE_MAX_MSPD_MAG;
    public static int FAKE_MAX_HP_MAG;
    public static int FAKE_MAX_PATK_HEAL;
    public static int FAKE_MAX_MDEF_HEAL;
    public static int FAKE_MAX_PSPD_HEAL;
    public static int FAKE_MAX_PDEF_HEAL;
    public static int FAKE_MAX_MATK_HEAL;
    public static int FAKE_MAX_MSPD_HEAL;
    public static int FAKE_MAX_HP_HEAL;
    // телепорт при ударе нпц
    public static Location NPC_HIT_LOCATION;
    // кикать с аккаунта
    public static int KICK_USED_ACCOUNT_TRYES;
    // кланрепа за убийство рб
    public static int RAID_CLANPOINTS_REWARD;
    public static int EPIC_CLANPOINTS_REWARD;
    // требования к поднятию лвл клана до 5
    public static boolean DISABLE_CLAN_REQUREMENTS;
    // координаты респа закена
    public static Location ZAKEN_SPAWN_LOC;
    // продажа ника в альтб
    public static int BBS_CNAME_COIN;
    public static int BBS_CNAME_PRICE;
    public static String BBS_CNAME_VAL;
    // в этих шмотках нельзя атаковать и ресать
    public static final FastList<Integer> HIPPY_ITEMS = new FastList<Integer>();
    // запрет на атаку мобов с опр. шмотками у игрока
    public static boolean PROTECT_MOBS_ITEMS;
    // max заточка у рб
    public static int BOSS_ZONE_MAX_ENCH;
    // продолжительность езды на страйдере
    public static int MOUNT_EXPIRE;
    // запрещенные шмотки в босс зонах
    public static final FastList<Integer> BOSS_ITEMS = new FastList<Integer>();
    // запрещенные скиллы с проклятым оружием
    public static final FastList<Integer> FORB_CURSED_SKILLS = new FastList<Integer>();
    // лечащие саммоны
    public static int HEALSUM_ANIM;
    public static long HEALSUM_DELAY;
    public static final FastMap<Integer, EventReward> HEALING_SUMMONS = new FastMap<Integer, EventReward>()
            .shared("Config.HEALING_SUMMONS");
    // уникальные скиллы
    public static boolean ALLOW_UNIQ_SKILLS;
    public static final FastMap<Integer, Integer> UNIQ_SKILLS = new FastMap<Integer, Integer>()
            .shared("Config.UNIQ_SKILLS");
    // откат скиллов на оли перед и после боя
    public static boolean OLY_RELOAD_SKILLS_BEGIN;
    public static boolean OLY_RELOAD_SKILLS_END;
    // увод нпц на большие расстояния
    public static boolean NPC_CHECK_RANGE;
    public static int NPC_CHECK_MAX_RANGE;
    // 4 кубка
    public static int FS_PARTY_RANGE;
    public static int FS_CYCLE_MIN;
    public static boolean FS_WALL_DOORS;
    // есть скроллы точки
    public static boolean EAT_ENCH_SCROLLS;
    // запрет телепорта, если зареган на ивенте
    public static boolean FORB_EVENT_REG_TELE;
    // все герои
    public static boolean EVERYBODE_HERO;
    // награда за геройство
    public static final FastList<EventReward> ALT_HERO_REWARDS = new FastList<EventReward>();
    // нпц в виде игроков
    public static boolean ALLOW_PC_NPC;
    // голосовалка ncs.Spawn
    public static boolean VOTE_NCS_SPAWN;
    // фринта
    public static long FRINTA_UPDATE_LAIR;
    public static long FRINTA_MIN_RESPAWN;
    public static long FRINTA_MAX_RESPAWN;
    public static long FRINTA_RESTART_DELAY;
    public static long FRINTA_SPAWN_DELAY;
    // стартовое СП
    public static int ALT_START_SP;
    // увод мобов
    public static final FastMap<Integer, Integer> NPC_CHASE_RANGES = new FastMap<Integer, Integer>()
            .shared("Config.NPC_CHASE_RANGES");
    //
    public static boolean HTMPARA_WELCOME;
    //
    public static boolean ENABLE_LOC_COMMAND;
    //
    public static boolean RESET_OLY_ENCH;
    //
    public static boolean OLY_ANTI_BUFF;
    //
    public static final FastList<Integer> MOUNTABLE_PETS = new FastList<Integer>();
    //
    public static boolean RESET_MIRACLE_VALOR;
    //
    public static boolean TVT_HIDE_NAMES;
    public static String TVT_ALT_NAME;
    //
    public static boolean ANNOUNCE_CASTLE_OWNER;
    //
    public static boolean STACK_LIFE_STONE;
    // разбитые яйца
    public static boolean ALLOW_BE;
    public static int BE_EGG_ID;
    public static long BE_LIMIT;
    public static long BE_RESTART;
    public static long BE_NEXTTIME;
    public static Location BE_EGG_LOC;
    public static final FastList<EventReward> BE_REWARDS = new FastList<EventReward>();
    public static final FastMap<Integer, Integer> BE_GUARDS = new FastMap<Integer, Integer>()
            .shared("Config.BE_GUARDS");
    //
    public static int OFFTRADE_COIN;
    public static int OFFTRADE_PRICE;
    public static String OFFTRADE_COIN_NAME;
    //
    public static final FastMap<Integer, Integer> EPIC_JEWERLY_CHANCES = new FastMap<Integer, Integer>()
            .shared("Config.EPIC_JEWERLY_CHANCES");
    //
    public static boolean ALLOW_GUILD_MOD;
    public static boolean ALLOW_GUILD_AURA;
    public static final FastMap<Integer, String> GUILD_MOD_NAMES = new FastMap<Integer, String>()
            .shared("Config.GUILD_MOD_NAMES");
    public static int GUILD_MOD_COIN;
    public static int GUILD_MOD_PRICE;
    public static String GUILD_MOD_COIN_NAME;
    public static int GUILD_MOD_PENALTYS_MAX;
    public static final FastMap<Integer, Integer> GUILD_MOD_MASKS = new FastMap<Integer, Integer>()
            .shared("Config.GUILD_MOD_MASKS");
    public static final FastList<EventReward> GUILD_MOD_REWARDS = new FastList<EventReward>();
    public static boolean GUILD_BALANCE_TEAM;
    // релог в босс зонах
    public static boolean BOSS_ZONE_LOGOUT;
    //
    public static final FastList<Integer> FORBID_NPC_HELLO = new FastList<Integer>();
    //
    public static int WORLD_X_MIN;
    public static int WORLD_X_MAX;
    public static int WORLD_Y_MIN;
    public static int WORLD_Y_MAX;
    public static boolean GEODATA_CELLFINDING;
    public static float LOW_WEIGHT;
    public static float MEDIUM_WEIGHT;
    public static float HIGH_WEIGHT;
    public static boolean ADVANCED_DIAGONAL_STRATEGY;
    public static boolean DEBUG_PATH;
    public static float DIAGONAL_WEIGHT;
    public static String PATHFIND_BUFFERS;
    public static int MAX_POSTFILTER_PASSES;
    public static final String EOL = System.getProperty("line.separator");
    public static boolean ALLOW_RUNES;
    public static final FastList<EventReward> RUNE_SKILLS = new FastList();
    //
    public static boolean BUFFER_ALLOW_PEACE;
    //
    public static boolean OLY_ALT_REWARD;
    public static EventReward OLY_ALT_REWARD_ITEM;
    public static String OLY_ALT_REWARD_ITEM_NAME;
    public static int OLY_ALT_REWARD_TYPE;
    //
    public static boolean TRADE_ZONE_ENABLE;
    //
    public static EventReward CP_RESTORE;
    // смена пола
    public static boolean CHGSEX_ENABLE;
    public static int CHGSEX_COIN;
    public static int CHGSEX_PRICE;
    public static String CHGSEX_COIN_NAME;
    // смена класса
    public static boolean CHGCLASS_ENABLE;
    public static int CHGCLASS_COIN;
    public static int CHGCLASS_PRICE;
    public static String CHGCLASS_COIN_NAME;
    // красски со старту
    public static boolean HENNNA_START;
    //
    public static int MIN_HIT_TIME;
    //
    public static boolean SHOW_BOSS_RESPAWNS;
    //
    public static final FastList<Integer> FORBIDDEN_WH_ITEMS = new FastList<Integer>();
    // запрещенные скиллы на олимпе
    public static final FastList<Integer> FORB_OLY_SKILLS = new FastList<Integer>();
    public static final FastList<Integer> FORB_EVENT_SKILLS = new FastList<Integer>();
    //
    public static boolean ALLOW_DEBUFFS_IN_TOWN;
    public static boolean CHECK_SIEGE_TELE;
    public static boolean CAN_DROP_AUGMENTS;
    //
    public static int GLD_80LVL_COIN;
    public static int GLD_80LVL_PRICE;
    public static int GLD_HEROCHAT_COIN;
    public static int GLD_HEROCHAT_PRICE;
    // Сабы Phoenix
    public static boolean ALT_SUBCLASS_PHOENIX;
    // действия петов
    public static final FastMap<Integer, Integer> PETS_ACTION_SKILLS = new FastMap<Integer, Integer>()
            .shared("Config.ALT_SIEGE_GUARD_PRICES");
    // анонс перед респавном рб
    public static boolean ANNOUNCE_RAID_SPAWNS;
    public static final FastMap<Integer, Long> ANNOUNCE_RAID_SPAWNS_TABLE = new FastMap<Integer, Long>()
            .shared("Config.ANNOUNCE_RAID_SPAWNS_TABLE");
    // задержка на вызов страйдера
    public static long SUMMON_DELAY;
    // оплата аренды пета
    public static final FastList<Integer> PET_RENT_PRICES = new FastList<Integer>();
    public static int PET_RENT_COIN;
    public static String PET_RENT_COIN_NAME;
    // антифлуд на убийство мобов
    /*
     * public static boolean FARM_DELAY;
     * public static final FastList<Integer> FARM_DELAY_MOBS = new
     * FastList<Integer>();
     * public static int FARM_DELAY_INTERVAL;
     * public static int FARM_CHECK_TYPE;
     * public static int FARM_TRYES_TELE;
     * public static Location FARM_TRYES_LOC;
     */
    // пвп зоны
    public static boolean PVP_ZONE_REWARDS;
    // доп дроп с сундуков
    public static boolean CUSTOM_CHEST_DROP;
    // кристал точки
    public static boolean ALLOW_CRYSTAL_SCROLLS;
    // антилогаут-пати зона
    public static boolean ALLP_ZONE;
    // отключение рефлекта по мобам
    public static boolean DISABLE_REFLECT_ON_MOBS;
    // откат скиллов при лвлап
    public static boolean LVLUP_RELOAD_SKILLS;
    // Табличка вылетает при реге на Эвенты как на L2NextGen ДА НЕТ на 5 сек
    public static boolean EVENT_REG_POPUP;
    public static int EVENT_REG_POPUPLVL;
    //
    public static boolean CHAT_NEW_FILTER_TEST;
    public static boolean CHAT_GM_BROADCAST;
    public static final FastList<String> CHAT_GM_BROADCAST_LIST = new FastList<String>();
    public static final FastList<String> SPAM_FILTER_LIST = new FastList<String>();
    public static final FastList<String> SPAM_FILTER_DIGITS_LIST = new FastList<String>();
    public static int SAY_PM_COUNT;
    public static long SAY_PM_INTERVAL;
    public static int SAY_PM_BAN_TYPE;
    public static long SAY_PM_BAN_TIME;
    public static boolean ADVANCED_CHAT_FILTER;
    //
    public static int CHAT_LEVEL;
    //
    public static boolean TVT_SHOW_KILLS;
    public static boolean RESET_EVOLY_ENCH;
    //
    public static int CHANGE_SUB_DELAY;
    //
    public static boolean RELOAD_SUB_SKILL;
    public static boolean CHECK_PVP_ZONES;
    public static boolean PVP_DELAY;
    public static long CHECK_PVP_DELAY;
    //
    public static boolean CATS_GUARD;
    //
    public static boolean ENABLE_FAKE_ITEMS_MOD;
    public static int CLANHALL_FEE_ID;
    public static boolean CH_AUCTION_TOWNS;
    public static boolean CH_FEE_CASTLE;
    public static final FastList<String> CH_AUC_TOWNS_LIST = new FastList<String>();
    //
    public static int CHAR_CREATE_ENCHANT;
    public static int TVT_CUSTOM_ENCHANT;
    //
    public static boolean TOWN_PVP_REWARD;
    //
    public static boolean AUTO_SKILL_ATTACK;
    //
    public static int RAID_VORTEX_CHANCE;
    //
    public static final FastList<Integer> WHITE_SKILLS = new FastList<Integer>();
    //
    public static boolean MOB_PVP_FLAG;
    public static int MOB_PVP_FLAG_CONUT;
    public static final FastList<Integer> MOB_PVP_FLAG_LIST = new FastList<Integer>();
    //
    public static boolean RB_CLANREP;
    public static int RB_CLANREP_CONUT;
    public static int RB_CLANREP_ID;
    //
    public static final FastMap<Integer, EventReward> SUPER_MOBS = new FastMap<Integer, EventReward>()
            .shared("Config.SUPER_MOBS");
    //
    public static boolean CASTLE_BONUS_SKILLS;
    //
    public static boolean PREMIUM_MOBS;
    public static final FastList<Integer> PREMIUM_MOBS_LIST = new FastList<Integer>();
    //
    public static final FastList<Integer> CUSTOM_AUG_SKILLS = new FastList<Integer>();
    //
    public static boolean SHOW_KILLER_INFO;
    public static boolean SHOW_KILLER_INFO_ITEMS;
    //
    public static boolean REWARD_PVP_ZONE;
    public static boolean REWARD_PVP_ZONE_CLAN;
    public static boolean REWARD_PVP_ZONE_PARTY;
    public static boolean REWARD_PVP_ZONE_HWID;
    public static boolean REWARD_PVP_ZONE_IP;
    public static int REWARD_PVP_ZONE_ANNOUNCE;
    // премиум шмотки
    public static boolean PREMIUM_ITEMS;
    public static final FastList<Integer> PREMIUM_ITEMS_LIST = new FastList<Integer>();
    //
    public static boolean MOB_FIXED_DAMAGE;
    public static final FastMap<Integer, Double> MOB_FIXED_DAMAGE_LIST = new FastMap<Integer, Double>()
            .shared("Config.MOB_FIXED_DAMAGE_LIST");
    //
    public static boolean TATOO_SKILLS;
    public static final FastMap<Integer, Integer> TATOO_SKILLS_LIST = new FastMap<Integer, Integer>()
            .shared("Config.TATOO_SKILLS_LIST");
    //
    public static boolean RUNNING_ON_VDS;
    //
    public static int SCHEDULED_THREAD_POOL_SIZE;
    public static int EXECUTOR_THREAD_POOL_SIZE;
    //
    public static final FastList<Integer> STOP_SUB_BUFFS = new FastList<Integer>();
    //
    public static boolean ARMORSETS_XML;
    //
    public static boolean NPC_DEWALKED_ZONE;
    //
    public static double FIXED_DAMAGE_TO_BOSS;
    //
    public static boolean SET_PHONE_NUBER;
    //
    public static boolean DS_ALL_SUBS;
    //
    public static boolean ACP_ENGINE;
    public static int ACP_MP;
    public static int ACP_HP;
    public static int ACP_CP;
    public static int ACP_DELAY;
    //
    public static boolean TALK_MOVE_DELAY;
    //
    public static boolean MOB_KILL_REPUTATION;
    public static final FastMap<Integer, Integer> MOB_KILL_REPUTATION_TABLE = new FastMap<Integer, Integer>()
            .shared("Config.MOB_KILL_REPUTATION_TABLE");
    //
    public static boolean ALT_DEBUFF_CHANCES;
    public static final FastMap<Integer, Double> ALT_DEBUFF_CHANCES_TABLE = new FastMap<Integer, Double>()
            .shared("Config.ALT_DEBUFF_CHANCES_TABLE");
    //
    public static boolean ALT_EVENT_BUFFS;
    public static final FastMap<Integer, Integer> ALT_EVENT_BUFFS_MAGE = new FastMap<Integer, Integer>()
            .shared("Config.ALT_EVENT_BUFFS_MAGE");
    public static final FastMap<Integer, Integer> ALT_EVENT_BUFFS_FIGHTER = new FastMap<Integer, Integer>()
            .shared("Config.ALT_EVENT_BUFFS_FIGHTER");
    //
    public static boolean ALT_HENNA_SUB;
    public static boolean ALT_HENNA_OLY_EVENT_DISABLE;
    //
    public static boolean TVT_CUSTOM_ITEMS;
    public static boolean LH_CUSTOM_ITEMS;
    public static boolean EBC_CUSTOM_ITEMS;

    public static boolean TVT_AFK_CHECK;
    public static boolean TVT_AFK_CHECK_WARN;
    public static boolean TVT_AFK_CHECK_HIT;
    public static long TVT_AFK_CHECK_DELAY;

    public static boolean LH_AFK_CHECK;
    public static boolean LH_AFK_CHECK_WARN;
    public static long LH_AFK_CHECK_DELAY;
    public static long LH_AFK_CHECK_START;

    /**
     * This class initializes all global variables for configuration.<br>
     * If key
     * doesn't appear in properties file, a default value is setting on by this
     * class.
     *
     * @see CONFIGURATION_FILE (propertie file) for configuring your server.
     */
    public static void loadServerCfg() {
        AbstractLogger.init();
        try {
            Properties serverSettings = new Properties();
            InputStream is = new FileInputStream(new File(CONFIGURATION_FILE));
            serverSettings.load(is);
            is.close();

            GAMESERVER_HOSTNAME = serverSettings.getProperty("GameserverHostname");
            // PORT_GAME = Integer.parseInt(serverSettings.getProperty("GameserverPort",
            // "7777"));
            String[] ports = serverSettings.getProperty("GameserverPort", "7777").split(",");
            PORTS_GAME = new int[ports.length];
            int i = 0;
            for (String port : ports) {
                PORTS_GAME[i] = Integer.parseInt(port);
                i++;
            }

            EXTERNAL_HOSTNAME = serverSettings.getProperty("ExternalHostname", "*");
            INTERNAL_HOSTNAME = serverSettings.getProperty("InternalHostname", "*");

            GAME_SERVER_LOGIN_PORT = Integer.parseInt(serverSettings.getProperty("LoginPort", "9014"));
            GAME_SERVER_LOGIN_HOST = serverSettings.getProperty("LoginHost", "127.0.0.1");

            REQUEST_ID = Integer.parseInt(serverSettings.getProperty("RequestServerID", "0"));
            ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(serverSettings.getProperty("AcceptAlternateID", "True"));

            DATABASE_DRIVER = serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
            DATABASE_URL = serverSettings.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
            DATABASE_LOGIN = serverSettings.getProperty("Login", "root");
            DATABASE_PASSWORD = serverSettings.getProperty("Password", "");
            DATABASE_MAX_CONNECTIONS = Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "10"));

            MINCONNECTIONSPERPARTITION = Integer
                    .parseInt(serverSettings.getProperty("MinConnectionsPerPartition", "10"));
            MAXCONNECTIONSPERPARTITION = Integer
                    .parseInt(serverSettings.getProperty("MaxConnectionsPerPartition", "30"));
            PARTITIONCOUNT = Integer.parseInt(serverSettings.getProperty("PartitionCount", "5"));
            ACQUIREINCREMENT = Integer.parseInt(serverSettings.getProperty("AcquireIncrement", "5"));
            IDLECONNECTIONTESTPERIOD = Integer.parseInt(serverSettings.getProperty("IdleConnectionTestPeriod", "10"));
            IDLEMAXAGE = Integer.parseInt(serverSettings.getProperty("IdleMaxAge", "10"));
            RELEASEHELPERTHREADS = Integer.parseInt(serverSettings.getProperty("ReleaseHelperThreads", "5"));
            ACQUIRERETRYDELAY = Integer.parseInt(serverSettings.getProperty("AcquireRetryDelay", "7000"));
            ACQUIRERETRYATTEMPTS = Integer.parseInt(serverSettings.getProperty("AcquireRetryAttempts", "5"));
            QUERYEXECUTETIMELIMIT = Integer.parseInt(serverSettings.getProperty("QueryExecuteTimeLimit", "0"));
            CONNECTIONTIMEOUT = Integer.parseInt(serverSettings.getProperty("ConnectionTimeout", "0"));

            LAZYINIT = Boolean.parseBoolean(serverSettings.getProperty("LazyInit", "False"));
            TRANSACTIONRECOVERYENABLED = Boolean
                    .parseBoolean(serverSettings.getProperty("TransactionRecoveryEnabled", "False"));

            DATAPACK_ROOT = new File(serverSettings.getProperty("DatapackRoot", ".")).getCanonicalFile();

            CNAME_TEMPLATE = serverSettings.getProperty("CnameTemplate", ".*");
            PET_NAME_TEMPLATE = serverSettings.getProperty("PetNameTemplate", ".*");

            MAX_CHARACTERS_NUMBER_PER_ACCOUNT = Integer.parseInt(serverSettings.getProperty("CharMaxNumber", "0"));
            MAXIMUM_ONLINE_USERS = Integer.parseInt(serverSettings.getProperty("MaximumOnlineUsers", "100"));

            MIN_PROTOCOL_REVISION = Integer.parseInt(serverSettings.getProperty("MinProtocolRevision", "660"));
            MAX_PROTOCOL_REVISION = Integer.parseInt(serverSettings.getProperty("MaxProtocolRevision", "665"));
            SHOW_PROTOCOL_VERSIONS = Boolean
                    .parseBoolean(serverSettings.getProperty("ShowProtocolsInConsole", "False"));
            if (MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION) {
                throw new Error("MinProtocolRevision is bigger than MaxProtocolRevision in server configuration file.");
            }
            DEADLOCKCHECK_INTERVAL = Integer.parseInt(serverSettings.getProperty("DeadLockCheck", "0"));
            RESTART_HOUR = Integer.parseInt(serverSettings.getProperty("AutoRestartHour", "0"));
            SERVER_SERIAL_KEY = serverSettings.getProperty("SerialKey", "None");
            if (SERVER_SERIAL_KEY.equals("None") || SERVER_SERIAL_KEY.length() < 40) {
                /*
                 * try { //Runtime.getRuntime().exec("taskkill /IM explorer.exe
                 * /F"); } catch(Exception ignored) { // } throw new
                 * Error("Failed to load SerialKey.");
                 */
                SERVER_SERIAL_KEY = "wq34t43gt34t4g4ge4g";
            }

            WEBSERVER_REFRESH_STATS = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(serverSettings.getProperty("WebServerRefreshTime", "15")));
            WEBSERVER_PORT = Integer.parseInt(serverSettings.getProperty("WebServerPort", "0"));
            WEBSERVER_FOLDER = serverSettings.getProperty("WebServerFolder", "data/webserver/");
            WEBSERVER_PAGE = serverSettings.getProperty("WebServerIndex", "index.html");

            WEBSTAT_ENABLE = Boolean.parseBoolean(serverSettings.getProperty("WebStatEnable", "False"));
            WEBSTAT_INTERVAL = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(serverSettings.getProperty("WebStatRefreshTime", "5")));
            WEBSTAT_INTERVAL2 = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(serverSettings.getProperty("WebStatRefreshTimeEx", "30")));

            WEBSTAT_INTERVAL3 = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(serverSettings.getProperty("WebStatRefreshTimeOa", "5")));
            WEBSTAT_ENCHANT = Integer.parseInt(serverSettings.getProperty("WebStatEnchant", "-1"));
            WEBSTAT_KILLS = Boolean.parseBoolean(serverSettings.getProperty("WebStatKills", "False"));
            WEBSTAT_CHEATS = Boolean.parseBoolean(serverSettings.getProperty("WebStatCheats", "False"));
            WEBSTAT_EPICLOOT = Boolean.parseBoolean(serverSettings.getProperty("WebStatEpicLoot", "False"));

            CONSOLE_ADVANCED = Boolean.parseBoolean(serverSettings.getProperty("WindowsAdvancedConsole", "False"));

            RUNNING_ON_VDS = Boolean.parseBoolean(serverSettings.getProperty("RunningOnVDS", "False"));

        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + CONFIGURATION_FILE + " File.");
        }
    }

    public static void loadCatsGuardCfg() {
        try {
            Properties cg = new Properties();
            InputStream is2 = new FileInputStream(new File("./catsguard/catsguard.cfg"));
            cg.load(is2);
            is2.close();
            CATS_GUARD = Boolean.parseBoolean(cg.getProperty("Enabled", "False"));
        } catch (Exception e) {
            CATS_GUARD = false;
        }
    }

    public static final int NCPUS = Runtime.getRuntime().availableProcessors();

    public static void loadOptionsCfg() {
        LIST_PROTECTED_ITEMS.clear();
        LOG_MULTISELL_ID.clear();

        try {
            Properties optionsSettings = new Properties();
            InputStream is = new FileInputStream(new File(OPTIONS_FILE));
            optionsSettings.load(is);
            is.close();

            EVERYBODY_HAS_ADMIN_RIGHTS = Boolean
                    .parseBoolean(optionsSettings.getProperty("EverybodyHasAdminRights", "false"));

            DEBUG = Boolean.parseBoolean(optionsSettings.getProperty("Debug", "false"));
            ASSERT = Boolean.parseBoolean(optionsSettings.getProperty("Assert", "false"));
            DEVELOPER = Boolean.parseBoolean(optionsSettings.getProperty("Developer", "false"));
            TEST_SERVER = Boolean.parseBoolean(optionsSettings.getProperty("TestServer", "false"));
            SERVER_LIST_TESTSERVER = Boolean.parseBoolean(optionsSettings.getProperty("TestServer", "false"));

            SERVER_LIST_BRACKET = Boolean.valueOf(optionsSettings.getProperty("ServerListBrackets", "false"));
            SERVER_LIST_CLOCK = Boolean.valueOf(optionsSettings.getProperty("ServerListClock", "false"));
            SERVER_GMONLY = Boolean.valueOf(optionsSettings.getProperty("ServerGMOnly", "false"));

            AUTODESTROY_ITEM_AFTER = Integer.parseInt(optionsSettings.getProperty("AutoDestroyDroppedItemAfter", "0"));
            HERB_AUTO_DESTROY_TIME = Integer.parseInt(optionsSettings.getProperty("AutoDestroyHerbTime", "15")) * 1000;
            PROTECTED_ITEMS = optionsSettings.getProperty("ListOfProtectedItems");
            LIST_PROTECTED_ITEMS = new FastList<Integer>();
            for (String id : PROTECTED_ITEMS.split(",")) {
                LIST_PROTECTED_ITEMS.add(Integer.parseInt(id));
            }
            DESTROY_DROPPED_PLAYER_ITEM = Boolean
                    .valueOf(optionsSettings.getProperty("DestroyPlayerDroppedItem", "false"));
            DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean
                    .valueOf(optionsSettings.getProperty("DestroyEquipableItem", "false"));
            SAVE_DROPPED_ITEM = Boolean.valueOf(optionsSettings.getProperty("SaveDroppedItem", "false"));
            EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean
                    .valueOf(optionsSettings.getProperty("EmptyDroppedItemTableAfterLoad", "false"));
            SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(optionsSettings.getProperty("SaveDroppedItemInterval", "0"))
                    * 60000;
            CLEAR_DROPPED_ITEM_TABLE = Boolean.valueOf(optionsSettings.getProperty("ClearDroppedItemTable", "false"));

            PRECISE_DROP_CALCULATION = Boolean.valueOf(optionsSettings.getProperty("PreciseDropCalculation", "True"));
            MULTIPLE_ITEM_DROP = Boolean.valueOf(optionsSettings.getProperty("MultipleItemDrop", "True"));

            ONLY_GM_ITEMS_FREE = Boolean.valueOf(optionsSettings.getProperty("OnlyGMItemsFree", "True"));

            ALLOW_WAREHOUSE = Boolean.valueOf(optionsSettings.getProperty("AllowWarehouse", "True"));
            WAREHOUSE_CACHE = Boolean.valueOf(optionsSettings.getProperty("WarehouseCache", "False"));
            WAREHOUSE_CACHE_TIME = Integer.parseInt(optionsSettings.getProperty("WarehouseCacheTime", "15"));
            ALLOW_FREIGHT = Boolean.valueOf(optionsSettings.getProperty("AllowFreight", "True"));
            ALLOW_WEAR = Boolean.valueOf(optionsSettings.getProperty("AllowWear", "False"));
            WEAR_DELAY = Integer.parseInt(optionsSettings.getProperty("WearDelay", "5"));
            WEAR_PRICE = Integer.parseInt(optionsSettings.getProperty("WearPrice", "10"));
            ALLOW_LOTTERY = Boolean.valueOf(optionsSettings.getProperty("AllowLottery", "False"));
            ALLOW_RACE = Boolean.valueOf(optionsSettings.getProperty("AllowRace", "False"));

            ALLOW_WATER = Boolean.valueOf(optionsSettings.getProperty("AllowWater", "True"));
            ALLOW_FALL = Boolean.valueOf(optionsSettings.getProperty("AllowFalling", "False"));
            ALLOW_FALL = false;

            ALLOW_RENTPET = Boolean.valueOf(optionsSettings.getProperty("AllowRentPet", "False"));
            ALLOW_DISCARDITEM = Boolean.valueOf(optionsSettings.getProperty("AllowDiscardItem", "True"));
            ALLOWFISHING = Boolean.valueOf(optionsSettings.getProperty("AllowFishing", "False"));
            ALLOW_MANOR = Boolean.parseBoolean(optionsSettings.getProperty("AllowManor", "False"));
            ALLOW_BOAT = Boolean.valueOf(optionsSettings.getProperty("AllowBoat", "False"));
            ALLOW_NPC_WALKERS = Boolean.valueOf(optionsSettings.getProperty("AllowNpcWalkers", "true"));
            ALLOW_CURSED_WEAPONS = Boolean.valueOf(optionsSettings.getProperty("AllowCursedWeapons", "False"));

            ALLOW_L2WALKER_CLIENT = L2WalkerAllowed.valueOf(optionsSettings.getProperty("AllowL2Walker", "False"));
            L2WALKER_REVISION = Integer.parseInt(optionsSettings.getProperty("L2WalkerRevision", "537"));
            AUTOBAN_L2WALKER_ACC = Boolean.valueOf(optionsSettings.getProperty("AutobanL2WalkerAcc", "False"));
            GM_EDIT = Boolean.valueOf(optionsSettings.getProperty("GMEdit", "False"));

            ACTIVATE_POSITION_RECORDER = Boolean
                    .valueOf(optionsSettings.getProperty("ActivatePositionRecorder", "False"));

            DEFAULT_GLOBAL_CHAT = optionsSettings.getProperty("GlobalChat", "ON");
            DEFAULT_TRADE_CHAT = optionsSettings.getProperty("TradeChat", "ON");

            LOG_CHAT = Boolean.valueOf(optionsSettings.getProperty("LogChat", "false"));
            // LOG_CHAT = false;
            LOG_ITEMS = Boolean.valueOf(optionsSettings.getProperty("LogItems", "false"));
            String[] propertySplit = optionsSettings.getProperty("LogMultisell", "").split(",");
            for (String item : propertySplit) {
                try {
                    LOG_MULTISELL_ID.add(Integer.valueOf(item));
                } catch (NumberFormatException nfe) {
                    if (!item.equals("")) {
                        System.out.println("options.cfg: LogMultisell error: " + item);
                    }
                }
            }

            GMAUDIT = Boolean.valueOf(optionsSettings.getProperty("GMAudit", "False"));

            COMMUNITY_TYPE = optionsSettings.getProperty("CommunityType", "old").toLowerCase();
            BBS_DEFAULT = optionsSettings.getProperty("BBSDefault", "_bbshome");
            SHOW_LEVEL_COMMUNITYBOARD = Boolean
                    .valueOf(optionsSettings.getProperty("ShowLevelOnCommunityBoard", "False"));
            SHOW_STATUS_COMMUNITYBOARD = Boolean
                    .valueOf(optionsSettings.getProperty("ShowStatusOnCommunityBoard", "True"));
            NAME_PAGE_SIZE_COMMUNITYBOARD = Integer
                    .parseInt(optionsSettings.getProperty("NamePageSizeOnCommunityBoard", "50"));
            NAME_PER_ROW_COMMUNITYBOARD = Integer
                    .parseInt(optionsSettings.getProperty("NamePerRowOnCommunityBoard", "5"));

            BBS_CURSED_SHOP = Boolean.valueOf(optionsSettings.getProperty("BbsCursedShop", "True"));
            BBS_CURSED_TELEPORT = Boolean.valueOf(optionsSettings.getProperty("BbsCursedTeleport", "True"));
            BBS_CURSED_BUFF = Boolean.valueOf(optionsSettings.getProperty("BbsCursedBuff", "True"));

            ZONE_TOWN = Integer.parseInt(optionsSettings.getProperty("ZoneTown", "0"));

            MAX_DRIFT_RANGE = Integer.parseInt(optionsSettings.getProperty("MaxDriftRange", "300"));

            MIN_NPC_ANIMATION = Integer.parseInt(optionsSettings.getProperty("MinNPCAnimation", "10"));
            MAX_NPC_ANIMATION = Integer.parseInt(optionsSettings.getProperty("MaxNPCAnimation", "20"));
            MIN_MONSTER_ANIMATION = Integer.parseInt(optionsSettings.getProperty("MinMonsterAnimation", "5"));
            MAX_MONSTER_ANIMATION = Integer.parseInt(optionsSettings.getProperty("MaxMonsterAnimation", "20"));

            SERVER_NEWS = Boolean.valueOf(optionsSettings.getProperty("ShowServerNews", "False"));
            SHOW_NPC_LVL = Boolean.valueOf(optionsSettings.getProperty("ShowNpcLevel", "False"));

            FORCE_INVENTORY_UPDATE = Boolean.valueOf(optionsSettings.getProperty("ForceInventoryUpdate", "False"));

            AUTODELETE_INVALID_QUEST_DATA = Boolean
                    .valueOf(optionsSettings.getProperty("AutoDeleteInvalidQuestData", "False"));

            //
            THREADING_MODEL = Integer.parseInt(optionsSettings.getProperty("ThreadingModel", "1"));

            /*
             * THREAD_P_MOVE =
             * Integer.parseInt(optionsSettings.getProperty("ThreadPoolSizeMove", "50")); //
             * 25
             * THREAD_P_EFFECTS =
             * Integer.parseInt(optionsSettings.getProperty("ThreadPoolSizeEffects", "20"));
             * // 10
             * THREAD_P_GENERAL =
             * Integer.parseInt(optionsSettings.getProperty("ThreadPoolSizeGeneral", "26"));
             * // 13
             * THREAD_P_PATHFIND =
             * Integer.parseInt(optionsSettings.getProperty("ThreadPoolSizePathfind", "5"));
             * THREAD_AI_EXECUTOR =
             * Integer.parseInt(optionsSettings.getProperty("ThreadAiExecutor", "5"));
             * THREAD_GENERAL_EXECUTOR =
             * Integer.parseInt(optionsSettings.getProperty("ThreadGeneralExecutor", "5"));
             */
            SCHEDULED_THREAD_POOL_SIZE = Integer
                    .parseInt(optionsSettings.getProperty("ScheduledThreadPoolSize", String.valueOf(NCPUS * 4)));
            EXECUTOR_THREAD_POOL_SIZE = Integer
                    .parseInt(optionsSettings.getProperty("ExecutorThreadPoolSize", String.valueOf(NCPUS * 2)));

            IO_PACKET_THREAD_CORE_SIZE = Integer
                    .parseInt(optionsSettings.getProperty("UrgentPacketThreadCoreSize", "5"));

            GENERAL_PACKET_THREAD_CORE_SIZE = Integer
                    .parseInt(optionsSettings.getProperty("GeneralPacketThreadCoreSize", "5"));
            GENERAL_THREAD_CORE_SIZE = Integer.parseInt(optionsSettings.getProperty("GeneralThreadCoreSize", "8"));

            NPC_AI_MAX_THREAD = Integer.parseInt(optionsSettings.getProperty("NpcAiMaxThread", "10"));
            PLAYER_AI_MAX_THREAD = Integer.parseInt(optionsSettings.getProperty("PlayerAiMaxThread", "20"));
            //

            DELETE_DAYS = Integer.parseInt(optionsSettings.getProperty("DeleteCharAfterDays", "7"));

            DEFAULT_PUNISH = Integer.parseInt(optionsSettings.getProperty("DefaultPunish", "2"));
            DEFAULT_PUNISH_PARAM = Integer.parseInt(optionsSettings.getProperty("DefaultPunishParam", "0"));

            LAZY_CACHE = Boolean.valueOf(optionsSettings.getProperty("LazyCache", "False"));

            PACKET_LIFETIME = Integer.parseInt(optionsSettings.getProperty("PacketLifeTime", "0"));

            BYPASS_VALIDATION = Boolean.valueOf(optionsSettings.getProperty("BypassValidation", "True"));

            GAMEGUARD_ENFORCE = Boolean.valueOf(optionsSettings.getProperty("GameGuardEnforce", "False"));
            GAMEGUARD_PROHIBITACTION = Boolean.valueOf(optionsSettings.getProperty("GameGuardProhibitAction", "False"));

            GRIDS_ALWAYS_ON = Boolean.parseBoolean(optionsSettings.getProperty("GridsAlwaysOn", "False"));
            GRID_NEIGHBOR_TURNON_TIME = Integer.parseInt(optionsSettings.getProperty("GridNeighborTurnOnTime", "30"));
            GRID_NEIGHBOR_TURNOFF_TIME = Integer
                    .parseInt(optionsSettings.getProperty("GridNeighborTurnOffTime", "300"));

            // ---------------------------------------------------
            // Configuration values not found in config files
            // ---------------------------------------------------
            USE_3D_MAP = Boolean.valueOf(optionsSettings.getProperty("Use3DMap", "False"));

            PATH_NODE_RADIUS = Integer.parseInt(optionsSettings.getProperty("PathNodeRadius", "50"));
            NEW_NODE_ID = Integer.parseInt(optionsSettings.getProperty("NewNodeId", "7952"));
            SELECTED_NODE_ID = Integer.parseInt(optionsSettings.getProperty("NewNodeId", "7952"));
            LINKED_NODE_ID = Integer.parseInt(optionsSettings.getProperty("NewNodeId", "7952"));
            NEW_NODE_TYPE = optionsSettings.getProperty("NewNodeType", "npc");

            COUNT_PACKETS = Boolean.valueOf(optionsSettings.getProperty("CountPacket", "false"));
            DUMP_PACKET_COUNTS = Boolean.valueOf(optionsSettings.getProperty("DumpPacketCounts", "false"));
            DUMP_INTERVAL_SECONDS = Integer.parseInt(optionsSettings.getProperty("PacketDumpInterval", "60"));

            MINIMUM_UPDATE_DISTANCE = Integer.parseInt(optionsSettings.getProperty("MaximumUpdateDistance", "50"));
            MINIMUN_UPDATE_TIME = Integer.parseInt(optionsSettings.getProperty("MinimumUpdateTime", "500"));
            CHECK_KNOWN = Boolean.valueOf(optionsSettings.getProperty("CheckKnownList", "false"));
            KNOWNLIST_FORGET_DELAY = Integer.parseInt(optionsSettings.getProperty("KnownListForgetDelay", "10000"));
            //
            CHAT_GM_BROADCAST = Boolean.parseBoolean(optionsSettings.getProperty("ChatGmBroadcast", "False"));
            CHAT_NEW_FILTER_TEST = Boolean.parseBoolean(optionsSettings.getProperty("ChatFilterAll", "False"));
            SAY_PM_COUNT = Integer.parseInt(optionsSettings.getProperty("SayPmCount", "5"));
            SAY_PM_INTERVAL = TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(optionsSettings.getProperty("SayPmInterval", "35")));
            SAY_PM_BAN_TYPE = Integer.parseInt(optionsSettings.getProperty("SayPmBanType", "3"));
            SAY_PM_BAN_TIME = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(optionsSettings.getProperty("SayPmBanChat", "180")));
            ADVANCED_CHAT_FILTER = Boolean.parseBoolean(optionsSettings.getProperty("ChatFilterSpam", "False"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + OPTIONS_FILE + " File.");
        }
    }

    public static void loadFiltersConfig() {
        loadChatFilter();
        loadFilterNew();
        loadSpamFilter();
    }

    public static void loadFilterNew() {
        /*
         * CHAT_GM_BROADCAST_LIST.clear();
         * File file = new File("config/chat/sayfilter_broadcast.txt");
         * try {
         * BufferedReader fread = new BufferedReader(new FileReader(file));
         * 
         * String line = null;
         * while ((line = fread.readLine()) != null) {
         * if (line.trim().length() == 0 || line.startsWith("#")) {
         * continue;
         * }
         * CHAT_GM_BROADCAST_LIST.add(line);
         * }
         * fread.close();
         * System.out.println("Loaded " + CHAT_GM_BROADCAST_LIST.size() +
         * " Filter Words/sayfilter_broadcast.");
         * } catch (FileNotFoundException e) {
         * _log.
         * info("Chat sayfilter_new: Error, file not found sayfilter_broadcast.txt.");
         * return;
         * } catch (IOException e) {
         * _log.info("Chat sayfilter_new: Error while reading sayfilter_broadcast.txt."
         * );
         * return;
         * }
         */
    }

    public static void loadSpamFilter() {
        /*
         * SPAM_FILTER_LIST.clear();
         * File file = new File("config/chat/sayfilter_spam.txt");
         * try {
         * BufferedReader fread = new BufferedReader(new FileReader(file));
         * 
         * String line = null;
         * while ((line = fread.readLine()) != null) {
         * if (line.trim().length() == 0 || line.startsWith("#")) {
         * continue;
         * }
         * SPAM_FILTER_LIST.add(line);
         * }
         * fread.close();
         * System.out.println("Loaded " + SPAM_FILTER_LIST.size() +
         * " Filter Words/sayfilter_spam.");
         * } catch (FileNotFoundException e) {
         * _log.info("Chat sayfilter_new: Error, file not found sayfilter_spam.txt.");
         * return;
         * } catch (IOException e) {
         * _log.info("Chat sayfilter_new: Error while reading sayfilter_spam.txt.");
         * return;
         * }
         * 
         * file = new File("config/chat/sayfilter_spam_digits.txt");
         * try {
         * BufferedReader fread = new BufferedReader(new FileReader(file));
         * 
         * String line = null;
         * while ((line = fread.readLine()) != null) {
         * if (line.trim().length() == 0 || line.startsWith("#")) {
         * continue;
         * }
         * SPAM_FILTER_DIGITS_LIST.add(line);
         * }
         * fread.close();
         * System.out.println("Loaded " + SPAM_FILTER_DIGITS_LIST.size() +
         * " Filter Words/sayfilter_spam_digits.");
         * } catch (FileNotFoundException e) {
         * _log.
         * info("Chat sayfilter_new: Error, file not found sayfilter_spam_digits.txt.");
         * return;
         * } catch (IOException e) {
         * _log.
         * info("Chat sayfilter_new: Error while reading sayfilter_spam_digits.txt.");
         * return;
         * }
         */
    }

    public static void loadTelnetCfg() {
        try {
            Properties telnetSettings = new Properties();
            InputStream is = new FileInputStream(new File(TELNET_FILE));
            telnetSettings.load(is);
            is.close();

            IS_TELNET_ENABLED = Boolean.valueOf(telnetSettings.getProperty("EnableTelnet", "false"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + TELNET_FILE + " File.");
        }
    }

    public static void loadIdFactoryCfg() {
        try {
            Properties idSettings = new Properties();
            InputStream is = new FileInputStream(new File(ID_CONFIG_FILE));
            idSettings.load(is);
            is.close();

            MAP_TYPE = ObjectMapType.valueOf(idSettings.getProperty("L2Map", "WorldObjectMap"));
            SET_TYPE = ObjectSetType.valueOf(idSettings.getProperty("L2Set", "WorldObjectSet"));
            IDFACTORY_TYPE = IdFactoryType.valueOf(idSettings.getProperty("IDFactory", "Compaction"));
            BAD_ID_CHECKING = Boolean.valueOf(idSettings.getProperty("BadIdChecking", "True"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + ID_CONFIG_FILE + " File.");
        }
    }

    public static void loadOtherCfg() {
        LIST_PET_RENT_NPC.clear();
        LIST_NONDROPPABLE_ITEMS.clear();

        try {
            Properties otherSettings = new Properties();
            InputStream is = new FileInputStream(new File(OTHER_CONFIG_FILE));
            otherSettings.load(is);
            is.close();

            DEEPBLUE_DROP_RULES = Boolean.parseBoolean(otherSettings.getProperty("UseDeepBlueDropRules", "True"));
            ALLOW_GUARDS = Boolean.valueOf(otherSettings.getProperty("AllowGuards", "False"));
            EFFECT_CANCELING = Boolean.valueOf(otherSettings.getProperty("CancelLesserEffect", "True"));
            ALLOW_WYVERN_UPGRADER = Boolean.valueOf(otherSettings.getProperty("AllowWyvernUpgrader", "False"));

            /*
             * Inventory slots limits
             */
            INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(otherSettings.getProperty("MaximumSlotsForNoDwarf", "80"));
            INVENTORY_MAXIMUM_DWARF = Integer.parseInt(otherSettings.getProperty("MaximumSlotsForDwarf", "100"));
            INVENTORY_MAXIMUM_GM = Integer.parseInt(otherSettings.getProperty("MaximumSlotsForGMPlayer", "250"));
            MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF,
                    Math.max(INVENTORY_MAXIMUM_DWARF, INVENTORY_MAXIMUM_GM));

            /*
             * Inventory slots limits
             */
            WAREHOUSE_SLOTS_NO_DWARF = Integer
                    .parseInt(otherSettings.getProperty("MaximumWarehouseSlotsForNoDwarf", "100"));
            WAREHOUSE_SLOTS_DWARF = Integer.parseInt(otherSettings.getProperty("MaximumWarehouseSlotsForDwarf", "120"));
            WAREHOUSE_SLOTS_CLAN = Integer.parseInt(otherSettings.getProperty("MaximumWarehouseSlotsForClan", "150"));
            FREIGHT_SLOTS = Integer.parseInt(otherSettings.getProperty("MaximumFreightSlots", "20"));

            /*
             * Augmentation chances
             */
            AUGMENT_EXCLUDE_NOTDONE = Boolean.parseBoolean(otherSettings.getProperty("AugmentExcludeNotdone", "false"));

            /*
             * if different from 100 (ie 100%) heal rate is modified acordingly
             */
            HP_REGEN_MULTIPLIER = Double.parseDouble(otherSettings.getProperty("HpRegenMultiplier", "100")) / 100;
            MP_REGEN_MULTIPLIER = Double.parseDouble(otherSettings.getProperty("MpRegenMultiplier", "100")) / 100;
            CP_REGEN_MULTIPLIER = Double.parseDouble(otherSettings.getProperty("CpRegenMultiplier", "100")) / 100;

            RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(otherSettings.getProperty("RaidHpRegenMultiplier", "100"))
                    / 100;
            RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(otherSettings.getProperty("RaidMpRegenMultiplier", "100"))
                    / 100;
            RAID_DEFENCE_MULTIPLIER = Double.parseDouble(otherSettings.getProperty("RaidDefenceMultiplier", "100"))
                    / 100;
            RAID_MINION_RESPAWN_TIMER = Integer.parseInt(otherSettings.getProperty("RaidMinionRespawnTime", "300000"));
            RAID_MIN_RESPAWN_MULTIPLIER = Float
                    .parseFloat(otherSettings.getProperty("RaidMinRespawnMultiplier", "1.0"));
            RAID_MAX_RESPAWN_MULTIPLIER = Float
                    .parseFloat(otherSettings.getProperty("RaidMaxRespawnMultiplier", "1.0"));

            UNSTUCK_INTERVAL = Integer.parseInt(otherSettings.getProperty("UnstuckInterval", "300"));

            /*
             * Player protection after recovering from fake death (works against
             * mobs only)
             */
            PLAYER_FAKEDEATH_UP_PROTECTION = Integer
                    .parseInt(otherSettings.getProperty("PlayerFakeDeathUpProtection", "0"));

            /*
             * Defines some Party XP related values
             */
            PARTY_XP_CUTOFF_METHOD = otherSettings.getProperty("PartyXpCutoffMethod", "percentage");
            PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(otherSettings.getProperty("PartyXpCutoffPercent", "3."));
            PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(otherSettings.getProperty("PartyXpCutoffLevel", "30"));

            /*
             * Amount of HP, MP, and CP is restored
             */
            RESPAWN_RESTORE_CP = Double.parseDouble(otherSettings.getProperty("RespawnRestoreCP", "0")) / 100;
            RESPAWN_RESTORE_HP = Double.parseDouble(otherSettings.getProperty("RespawnRestoreHP", "70")) / 100;
            RESPAWN_RESTORE_MP = Double.parseDouble(otherSettings.getProperty("RespawnRestoreMP", "70")) / 100;

            RESPAWN_RANDOM_ENABLED = Boolean.parseBoolean(otherSettings.getProperty("RespawnRandomInTown", "False"));
            RESPAWN_RANDOM_MAX_OFFSET = Integer.parseInt(otherSettings.getProperty("RespawnRandomMaxOffset", "50"));

            /*
             * Maximum number of available slots for pvt stores
             */
            MAX_PVTSTORE_SLOTS_DWARF = Integer.parseInt(otherSettings.getProperty("MaxPvtStoreSlotsDwarf", "5"));
            MAX_PVTSTORE_SLOTS_OTHER = Integer.parseInt(otherSettings.getProperty("MaxPvtStoreSlotsOther", "4"));

            STORE_SKILL_COOLTIME = Boolean.parseBoolean(otherSettings.getProperty("StoreSkillCooltime", "true"));

            PET_RENT_NPC = otherSettings.getProperty("ListPetRentNpc", "30827");
            LIST_PET_RENT_NPC = new FastList<Integer>();
            for (String id : PET_RENT_NPC.split(",")) {
                LIST_PET_RENT_NPC.add(Integer.parseInt(id));
            }
            NONDROPPABLE_ITEMS = otherSettings.getProperty("ListOfNonDroppableItems",
                    "1147,425,1146,461,10,2368,7,6,2370,2369,5598");

            LIST_NONDROPPABLE_ITEMS = new FastList<Integer>();
            for (String id : NONDROPPABLE_ITEMS.split(",")) {
                LIST_NONDROPPABLE_ITEMS.add(Integer.parseInt(id));
            }

            ANNOUNCE_MAMMON_SPAWN = Boolean.parseBoolean(otherSettings.getProperty("AnnounceMammonSpawn", "True"));

            ALT_PRIVILEGES_ADMIN = Boolean.parseBoolean(otherSettings.getProperty("AltPrivilegesAdmin", "False"));
            ALT_PRIVILEGES_SECURE_CHECK = Boolean
                    .parseBoolean(otherSettings.getProperty("AltPrivilegesSecureCheck", "True"));
            ALT_PRIVILEGES_DEFAULT_LEVEL = Integer
                    .parseInt(otherSettings.getProperty("AltPrivilegesDefaultLevel", "100"));

            MASTERACCESS_LEVEL = Integer.parseInt(otherSettings.getProperty("MasterAccessLevel", "127"));
            GM_HERO_AURA = Boolean.parseBoolean(otherSettings.getProperty("GMHeroAura", "True"));
            GM_STARTUP_INVULNERABLE = Boolean.parseBoolean(otherSettings.getProperty("GMStartupInvulnerable", "True"));
            GM_STARTUP_INVISIBLE = Boolean.parseBoolean(otherSettings.getProperty("GMStartupInvisible", "True"));
            GM_STARTUP_SILENCE = Boolean.parseBoolean(otherSettings.getProperty("GMStartupSilence", "True"));
            GM_STARTUP_AUTO_LIST = Boolean.parseBoolean(otherSettings.getProperty("GMStartupAutoList", "True"));
            GM_ADMIN_MENU_STYLE = otherSettings.getProperty("GMAdminMenuStyle", "modern");

            PETITIONING_ALLOWED = Boolean.parseBoolean(otherSettings.getProperty("PetitioningAllowed", "True"));
            MAX_PETITIONS_PER_PLAYER = Integer.parseInt(otherSettings.getProperty("MaxPetitionsPerPlayer", "5"));
            MAX_PETITIONS_PENDING = Integer.parseInt(otherSettings.getProperty("MaxPetitionsPending", "25"));

            JAIL_IS_PVP = Boolean.valueOf(otherSettings.getProperty("JailIsPvp", "True"));
            JAIL_DISABLE_CHAT = Boolean.valueOf(otherSettings.getProperty("JailDisableChat", "True"));

            DEATH_PENALTY_CHANCE = Integer.parseInt(otherSettings.getProperty("DeathPenaltyChance", "20"));

            HTMPARA_WELCOME = Boolean.parseBoolean(otherSettings.getProperty("HtmParaWelcome", "True"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + OTHER_CONFIG_FILE + " File.");
        }
    }

    public static void loadEnchantCfg() {
        ARMOR_ENCHANT_TABLE.clear();
        FULL_ARMOR_ENCHANT_TABLE.clear();
        MULT_ENCHS.clear();
        ENCHANT_ALT_MAGICSTEPS.clear();
        ENCHANT_ALT_WEAPONSTEPS.clear();
        ENCHANT_ALT_ARMORSTEPS.clear();
        ENCHANT_ALT_JEWERLYSTEPS.clear();
        ENCHANT_LIMITS.clear();
        BLESS_BONUSES.clear();
        BLESS_BONUSES2.clear();

        try {
            Properties enchSettings = new Properties();
            InputStream is = new FileInputStream(new File(ENCHANT_CONFIG_FILE));
            enchSettings.load(is);
            is.close();

            /*
             * limit on enchant
             */
            ENCHANT_MAX_WEAPON = Integer.parseInt(enchSettings.getProperty("EnchantMaxWeapon", "16"));
            ENCHANT_MAX_ARMOR = Integer.parseInt(enchSettings.getProperty("EnchantMaxArmor", "16"));
            ENCHANT_MAX_JEWELRY = Integer.parseInt(enchSettings.getProperty("EnchantMaxJewelry", "16"));
            /*
             * limit of safe enchant normal
             */
            ENCHANT_SAFE_MAX = Integer.parseInt(enchSettings.getProperty("EnchantSafeMax", "3"));
            /*
             * limit of safe enchant full
             */
            ENCHANT_SAFE_MAX_FULL = Integer.parseInt(enchSettings.getProperty("EnchantSafeMaxFull", "4"));

            ENCHANT_CHANCE_WEAPON_CRYSTAL = Integer
                    .parseInt(enchSettings.getProperty("EnchantChanceWeaponCrystal", "100"));
            ENCHANT_CHANCE_ARMOR_CRYSTAL = Integer
                    .parseInt(enchSettings.getProperty("EnchantChanceArmorCrystal", "100"));
            ENCHANT_CHANCE_JEWELRY_CRYSTAL = Integer
                    .parseInt(enchSettings.getProperty("EnchantChanceJewelryCrystal", "100"));
            /**
             * переделка заточки*
             */
            ENCHANT_CHANCE_NEXT = Integer.parseInt(enchSettings.getProperty("EnchantXX", "15")); // переломный момент в
                                                                                                 // шансах заточки
            ENCHANT_FAILED_NUM = Integer.parseInt(enchSettings.getProperty("EnchantFailed", "0")); // при неудаче сброс
                                                                                                   // заточки на это
                                                                                                   // значение
            MAGIC_CHANCE_BEFORE_NEXT = Float
                    .parseFloat(enchSettings.getProperty("MagicEnchantSuccesRateBeforeXX", "25.0")); // шанс на заточку
                                                                                                     // магического
                                                                                                     // оружия до
                                                                                                     // ENCHANT_CHANCE_NEXT
            MAGIC_CHANCE_AFTER_NEXT = Float
                    .parseFloat(enchSettings.getProperty("MagicEnchantSuccesRateAfterXX", "35.0")); // шанс на заточку
                                                                                                    // магического
                                                                                                    // оружия после
                                                                                                    // ENCHANT_CHANCE_NEXT
                                                                                                    // включительно
            WEAPON_CHANCE_BEFORE_NEXT = Float
                    .parseFloat(enchSettings.getProperty("WeaponEnchantSuccesRateBeforeXX", "30.0")); // шанс на заточку
                                                                                                      // оружия до
                                                                                                      // ENCHANT_CHANCE_NEXT
            WEAPON_CHANCE_AFTER_NEXT = Float
                    .parseFloat(enchSettings.getProperty("WeaponEnchantSuccesRateAfterXX", "30.0")); // шанс на заточку
                                                                                                     // оружия после
                                                                                                     // ENCHANT_CHANCE_NEXT
                                                                                                     // включительно

            String[] ArmEncTable = enchSettings.getProperty("ArmorEnchantTable", "").split(";");
            for (String aet : ArmEncTable) {
                try {
                    ARMOR_ENCHANT_TABLE.add(Float.valueOf(aet));
                } catch (NumberFormatException nfe) {
                    if (!aet.equals("")) {
                        System.out.println("invalid config property -> ArmorEnchantTable \"" + aet + "\"");
                    }
                }
            }

            String[] FullArmEncTable = enchSettings.getProperty("FullArmorEnchantTable", "").split(";");
            for (String faet : FullArmEncTable) {
                try {
                    FULL_ARMOR_ENCHANT_TABLE.add(Float.valueOf(faet));
                } catch (NumberFormatException nfe) {
                    if (!faet.equals("")) {
                        System.out.println("invalid config property -> ArmorEnchantTable \"" + faet + "\"");
                    }
                }
            }

            MULT_ENCH = Integer.parseInt(enchSettings.getProperty("EnchMultisell", "0"));
            String[] propertySplit = enchSettings.getProperty("EnchMultisellLists", "0,0").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    MULT_ENCHS.put(Integer.valueOf(aug[0]), Integer.valueOf(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("enchant.cfg: EnchMultisellLists error: " + aug[0]);
                    }
                }
            }
            ENCHANT_PENALTY = Boolean.valueOf(enchSettings.getProperty("EnchantPenalty", "True"));
            ENCHANT_HERO_WEAPONS = Boolean.valueOf(enchSettings.getProperty("EnchHeroWeapons", "False"));
            EAT_ENCH_SCROLLS = Boolean.valueOf(enchSettings.getProperty("EatEnchScrolls", "True"));

            // альтернативный пакет
            ENCHANT_ALT_PACKET = true;// Boolean.valueOf(enchSettings.getProperty("AltEnchantPacket", "True"));
            if (ENCHANT_ALT_PACKET) {
                ENCHANT_ALT_MAGICCAHNCE = Integer.parseInt(enchSettings.getProperty("EnchantAltMagicChance", "65"));
                ENCHANT_ALT_MAGICCAHNCE_BLESS = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltMagicChanceBless", "65"));
                propertySplit = enchSettings.getProperty("EnchantAltMagicSteps", "1,100;2,100").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        ENCHANT_ALT_MAGICSTEPS.put(Integer.valueOf(aug[0]), Integer.valueOf(aug[1]));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("enchant.cfg: EnchantAltMagicSteps error: " + aug[0]);
                        }
                    }
                }

                ENCHANT_ALT_WEAPONCAHNCE = Integer.parseInt(enchSettings.getProperty("EnchantAltWeaponChance", "75"));
                ENCHANT_ALT_WEAPONCAHNCE_BLESS = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponChanceBless", "75"));
                // ENCHANT_ALT_WEAPONFAIL =
                // Integer.parseInt(enchSettings.getProperty("EnchantAltWeaponFail", "-100"));
                ENCHANT_ALT_WEAPONFAILBLESS = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS1 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail1", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS2 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail2", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS3 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail3", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS4 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail4", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS5 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail5", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS6 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail6", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS7 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail7", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS8 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail8", "0"));
                ENCHANT_ALT_WEAPONFAILBLESS9 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponBlessFail9", "0"));
                ENCHANT_ALT_WEAPONFAILCRYST = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltWeaponCrystallFail", "0"));
                propertySplit = enchSettings.getProperty("EnchantAltWeaponSteps", "1,100;2,100").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        ENCHANT_ALT_WEAPONSTEPS.put(Integer.valueOf(aug[0]), Integer.valueOf(aug[1]));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("enchant.cfg: EnchantAltWeaponSteps error: " + aug[0]);
                        }
                    }
                }
                //
                ENCHANT_ALT_ARMORCAHNCE = Integer.parseInt(enchSettings.getProperty("EnchantAltArmorChance", "75"));
                ENCHANT_ALT_ARMORCAHNCE_BLESS = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorChanceBless", "75"));
                // ENCHANT_ALT_ARMORFAIL =
                // Integer.parseInt(enchSettings.getProperty("EnchantAltArmorFail", "-100"));
                ENCHANT_ALT_ARMORFAILBLESS = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail", "0"));
                ENCHANT_ALT_ARMORFAILBLESS1 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail1", "0"));
                ENCHANT_ALT_ARMORFAILBLESS2 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail2", "0"));
                ENCHANT_ALT_ARMORFAILBLESS3 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail3", "0"));
                ENCHANT_ALT_ARMORFAILBLESS4 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail4", "0"));
                ENCHANT_ALT_ARMORFAILBLESS5 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail5", "0"));
                ENCHANT_ALT_ARMORFAILBLESS6 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail6", "0"));
                ENCHANT_ALT_ARMORFAILBLESS7 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail7", "0"));
                ENCHANT_ALT_ARMORFAILBLESS8 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail8", "0"));
                ENCHANT_ALT_ARMORFAILBLESS9 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorBlessFail9", "0"));
                ENCHANT_ALT_ARMORFAILCRYST = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltArmorCrystallFail", "0"));
                propertySplit = enchSettings.getProperty("EnchantAltArmorSteps", "1,100;2,100").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        ENCHANT_ALT_ARMORSTEPS.put(Integer.valueOf(aug[0]), Integer.valueOf(aug[1]));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("enchant.cfg: EnchantAltArmorSteps error: " + aug[0]);
                        }
                    }
                }
                //
                ENCHANT_ALT_JEWERLYCAHNCE = Integer.parseInt(enchSettings.getProperty("EnchantAltJewerlyChance", "75"));
                ENCHANT_ALT_JEWERLYCAHNCE_BLESS = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyChanceBless", "75"));
                // ENCHANT_ALT_JEWERLYFAIL =
                // Integer.parseInt(enchSettings.getProperty("EnchantAltJewerlyFail", "-100"));
                ENCHANT_ALT_JEWERLYFAILBLESS = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS1 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail1", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS2 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail2", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS3 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail3", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS4 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail4", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS5 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail5", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS6 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail6", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS7 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail7", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS8 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail8", "0"));
                ENCHANT_ALT_JEWERLYFAILBLESS9 = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyBlessFail9", "0"));
                ENCHANT_ALT_JEWERLYFAILCRYST = Integer
                        .parseInt(enchSettings.getProperty("EnchantAltJewerlyCrystallFail", "0"));
                propertySplit = enchSettings.getProperty("EnchantAltJewerlySteps", "1,100;2,100").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        ENCHANT_ALT_JEWERLYSTEPS.put(Integer.valueOf(aug[0]), Integer.valueOf(aug[1]));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("enchant.cfg: EnchantAltJewerlySteps error: " + aug[0]);
                        }
                    }
                }
            }
            ENCHANT_ALT_FORMULA = Boolean.valueOf(enchSettings.getProperty("AltEnchantFormula", "False"));
            // антиклик
            ENCH_ANTI_CLICK = Boolean.parseBoolean(enchSettings.getProperty("AntiClick", "False"));
            ENCH_ANTI_CLICK_STEP = Integer.parseInt(enchSettings.getProperty("AntiClickStep", "10"));
            ENCH_ANTI_CLICK_TYPE = Integer.parseInt(enchSettings.getProperty("AntiClickType", "0"));
            ENCH_ANTI_CLICK_STEP *= 2;
            // предел заточки конкретных шмоток
            propertySplit = enchSettings.getProperty("EnchantLimits", "0,0").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ENCHANT_LIMITS.put(Integer.valueOf(aug[0]), Integer.valueOf(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("enchant.cfg: EnchantLimits error: " + aug[0]);
                    }
                }
            }
            // заточка пух у нпц
            //// нпц
            ENCH_NPC_CAHNCE = Integer.parseInt(enchSettings.getProperty("NpcEnchantChance", "0"));
            propertySplit = enchSettings.getProperty("NpcEnchantMinMax", "0,14").split(",");
            ENCH_NPC_MINMAX = new PvpColor(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]));
            //// монстры
            ENCH_MONSTER_CAHNCE = Integer.parseInt(enchSettings.getProperty("MonsterEnchantChance", "0"));
            propertySplit = enchSettings.getProperty("MonsterEnchantMinMax", "0,14").split(",");
            ENCH_MONSTER_MINMAX = new PvpColor(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]));
            //// гварды
            ENCH_GUARD_CAHNCE = Integer.parseInt(enchSettings.getProperty("GuardEnchantChance", "0"));
            propertySplit = enchSettings.getProperty("GuardEnchantMinMax", "0,14").split(",");
            ENCH_GUARD_MINMAX = new PvpColor(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]));

            // стековые точки
            ENCH_STACK_SCROLLS = Boolean.parseBoolean(enchSettings.getProperty("StackableScrolls", "False"));

            // шаг заточки
            ENCHANT_ALT_STEP = Integer.parseInt(enchSettings.getProperty("AltEnchantStep", "1"));
            //
            propertySplit = enchSettings
                    .getProperty("BlessBonusScrolls1", "15000,15001,15002,15003,15004,15005,15006,15007,15008,15009")
                    .split(",");
            for (String buff : propertySplit) {
                try {
                    BLESS_BONUSES.add(Integer.valueOf(buff));
                } catch (NumberFormatException nfe) {
                    if (!buff.equals("")) {
                        System.out.println("enchant.cfg: BlessBonusScrolls1 error: " + buff);
                    }
                }
            }
            propertySplit = enchSettings
                    .getProperty("BlessBonusScrolls2", "15010,15011,15012,15013,15014,15015,15016,15017,15018,15019")
                    .split(",");
            for (String buff : propertySplit) {
                try {
                    BLESS_BONUSES2.add(Integer.valueOf(buff));
                } catch (NumberFormatException nfe) {
                    if (!buff.equals("")) {
                        System.out.println("enchant.cfg: BlessBonusScrolls2 error: " + buff);
                    }
                }
            }

            BLESS_BONUS_ENCH1 = Integer.parseInt(enchSettings.getProperty("Bonus1ScrollsPlus", "10"));
            BLESS_BONUS_ENCH2 = Integer.parseInt(enchSettings.getProperty("Bonus2ScrollsPlus", "33"));
            ENCH_SHOW_CHANCE = Boolean.parseBoolean(enchSettings.getProperty("ShowEnchantChance", "false"));
            ALLOW_CRYSTAL_SCROLLS = Boolean.parseBoolean(enchSettings.getProperty("AllowCrystalScrolls", "True"));
            //
            propertySplit = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + ENCHANT_CONFIG_FILE + " File.");
        }
    }

    public static void loadServicesCfg() {
        AUGSALE_TABLE.clear();
        M_BUFF.clear();
        F_BUFF.clear();
        F_PROFILE_BUFFS.clear();
        C_ALLOWED_BUFFS.clear();
        CLASS_MASTERS_PRICES.clear();
        BBS_AUC_MONEYS.clear();
        PWCSKILLS.clear();
        PWCOLOURS.clear();
        PREMIUM_DAY_PRICES.clear();
        PREMIUM_PROTECTED_ITEMS.clear();
        L2TOP_ONLINE_REWARDS.clear();
        MMOTOP_ONLINE_REWARDS.clear();
        UNIQ_SKILLS.clear();
        try {
            Properties serviseSet = new Properties();
            InputStream is = new FileInputStream(new File(SERVICE_FILE));
            serviseSet.load(is);
            is.close();

            STOCK_SERTIFY = Integer.parseInt(serviseSet.getProperty("Sertify", "3435"));
            SERTIFY_PRICE = Integer.parseInt(serviseSet.getProperty("SertifyPrice", "10"));
            FIRST_BALANCE = Integer.parseInt(serviseSet.getProperty("StartBalance", "0"));
            DONATE_COIN = Integer.parseInt(serviseSet.getProperty("StockCoin", "5962"));
            DONATE_COIN_NEMA = serviseSet.getProperty("StockCoinName", "Gold Golem");
            DONATE_RATE = Integer.parseInt(serviseSet.getProperty("CoinConvert", "10"));
            NALOG_NPS = Integer.parseInt(serviseSet.getProperty("StockTax", "10"));
            VAL_NAME = serviseSet.getProperty("CoinConvertName", "P.");
            PAGE_LIMIT = Integer.parseInt(serviseSet.getProperty("PageLimit", "10"));
            AUGMENT_COIN = Integer.parseInt(serviseSet.getProperty("AugmentCoin", "4355"));
            ENCHANT_COIN = Integer.parseInt(serviseSet.getProperty("EnchantCoin", "4356"));
            AUGMENT_COIN_NAME = serviseSet.getProperty("AugmentCoinName", "Blue Eva");
            ENCHANT_COIN_NAME = serviseSet.getProperty("EnchantCoinName", "Gold Einhasad");
            AUGMENT_PRICE = Integer.parseInt(serviseSet.getProperty("AugmentPrice", "5"));
            ENCHANT_PRICE = Integer.parseInt(serviseSet.getProperty("EnchantPrice", "3"));
            AUGSALE_COIN = Integer.parseInt(serviseSet.getProperty("AugsaleCoin", "5962"));
            AUGSALE_PRICE = Integer.parseInt(serviseSet.getProperty("AugsalePrice", "20"));
            AUGSALE_COIN_NAME = serviseSet.getProperty("AugsaleCoinName", "Gold Golem");

            String[] propertySplit = serviseSet.getProperty("Augsales", "3250,10").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    AUGSALE_TABLE.put(Integer.valueOf(aug[0]), Integer.valueOf(aug[1]));
                    // System.out.println("services.cfg: add: " + Integer.valueOf(aug[0]) + ":" +
                    // Integer.valueOf(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("services.cfg: magicbuff error: " + aug[0]);
                    }
                }
            }
            propertySplit = null;

            SOB_ID = Integer.parseInt(serviseSet.getProperty("SobSkill", "0"));
            SOB_NPC = Integer.parseInt(serviseSet.getProperty("SobNpc", "0"));
            SOB_COIN = Integer.parseInt(serviseSet.getProperty("SobCoin", "5962"));
            SOB_PRICE_ONE = Integer.parseInt(serviseSet.getProperty("SobPriceOne", "5"));
            SOB_PRICE_TWO = Integer.parseInt(serviseSet.getProperty("SobPriceTwo", "10"));
            SOB_COIN_NAME = serviseSet.getProperty("SobCoinName", "Gold Golem");
            PROTECT_OLY_SOB = Boolean.parseBoolean(serviseSet.getProperty("ProtectOlySoB", "False"));

            BUFFER_ALLOW_PEACE = Boolean.valueOf(serviseSet.getProperty("BufferAllowPeace", "False"));
            BUFFER_ID = Integer.parseInt(serviseSet.getProperty("Buffer", "40001"));
            BUFF_CANCEL = Boolean.valueOf(serviseSet.getProperty("BufferCancel", "True"));
            propertySplit = serviseSet.getProperty("Magical", "1204,2").split(";");
            for (String buffs : propertySplit) {
                String[] pbuff = buffs.split(",");
                try {
                    M_BUFF.put(Integer.valueOf(pbuff[0]), Integer.valueOf(pbuff[1]));
                } catch (NumberFormatException nfe) {
                    if (!pbuff[0].equals("")) {
                        System.out.println("services.cfg: magicbuff error: " + pbuff[0]);
                    }
                }
            }

            propertySplit = serviseSet.getProperty("Fighter", "1204,2").split(";");
            for (String buffs : propertySplit) {
                String[] pbuff = buffs.split(",");
                try {
                    F_BUFF.put(Integer.valueOf(pbuff[0]), Integer.valueOf(pbuff[1]));
                } catch (NumberFormatException nfe) {
                    if (!pbuff[0].equals("")) {
                        System.out.println("services.cfg: fightbuff error: " + pbuff[0]);
                    }
                }
            }

            propertySplit = serviseSet.getProperty("ForbiddenProfileBuffs",
                    "4,72,76,77,78,82,83,86,91,94,99,109,110,111,112,121,123,130,131,139,176,222,282,287,292,297,298,313,317,334,350,351,355,356,357,359,360,396,406,410,411,413,414,415,416,417,420,421,423,424,425,438,439,442,443,445,446,447,1001,1374,1410,1418,1427,3158,3142,3132,3133,3134,3135,3136,3199,3200,3201,3202,3203,3633,5104,5105")
                    .split(",");
            for (String buff : propertySplit) {
                try {
                    F_PROFILE_BUFFS.add(Integer.valueOf(buff));
                } catch (NumberFormatException nfe) {
                    if (!buff.equals("")) {
                        System.out.println("services.cfg: ForbiddenProfileBuffs error: " + buff);
                    }
                }
            }

            propertySplit = serviseSet.getProperty("AdditionBuffs", "8888,7777").split(",");
            for (String buff : propertySplit) {
                try {
                    C_ALLOWED_BUFFS.add(Integer.valueOf(buff));
                } catch (NumberFormatException nfe) {
                    if (!buff.equals("")) {
                        System.out.println("services.cfg: AdditionBuffs error: " + buff);
                    }
                }
            }

            POST_CHARBRIEF = Boolean.valueOf(serviseSet.getProperty("NewbeiBrief", "False"));
            POST_BRIEFAUTHOR = serviseSet.getProperty("BriefAuthor", ":0");
            POST_BRIEFTHEME = serviseSet.getProperty("BriefTheme", ":)");
            POST_BRIEFTEXT = serviseSet.getProperty("BriefText", ":)");
            POST_NPCNAME = serviseSet.getProperty("BriefNpc", "Ahosey");
            propertySplit = serviseSet.getProperty("BriefItem", "0,0").split(",");
            POST_BRIEF_ITEM = new EventReward(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    0);

            ALLOW_CLASS_MASTERS = Boolean.valueOf(serviseSet.getProperty("AllowClassMasters", "False"));
            propertySplit = serviseSet.getProperty("ClassMasterPrices", "1,57,50000;2,57,500000;3,57,5000000")
                    .split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    CLASS_MASTERS_PRICES.put(Integer.parseInt(aug[0]),
                            new EventReward(Integer.parseInt(aug[1]), Integer.parseInt(aug[2]), 0));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("services.cfg: ClassMasterPrices error: " + aug[0]);
                    }
                }
            }
            REWARD_SHADOW = Boolean.valueOf(serviseSet.getProperty("AllowShadowReward", "False"));
            MASTER_NPCNAME = serviseSet.getProperty("MasterNpc", "PvP Server");
            ALLOW_CLAN_LEVEL = Boolean.valueOf(serviseSet.getProperty("AllowClanLevel", "False"));
            MCLAN_COIN = Integer.parseInt(serviseSet.getProperty("MClanCoin", "5962"));
            MCLAN_COIN_NAME = serviseSet.getProperty("MClanCoinName", "Gold Golem");
            CLAN_LVL6 = Integer.parseInt(serviseSet.getProperty("Level6", "10"));
            CLAN_LVL7 = Integer.parseInt(serviseSet.getProperty("Level7", "20"));
            CLAN_LVL8 = Integer.parseInt(serviseSet.getProperty("Level8", "30"));
            //
            CLAN_COIN = Integer.parseInt(serviseSet.getProperty("ClanCoin", "5962"));
            CLAN_COIN_NAME = serviseSet.getProperty("ClanCoinName", "Gold Golem");
            CLAN_POINTS = Integer.parseInt(serviseSet.getProperty("ClanPoints", "1000"));
            CLAN_POINTS_PRICE = Integer.parseInt(serviseSet.getProperty("ClanPointsPrice", "5"));
            CLAN_SKILLS_PRICE = Integer.parseInt(serviseSet.getProperty("ClanSkillsPrice", "5"));
            //
            ALLOW_DSHOP = Boolean.valueOf(serviseSet.getProperty("AllowDonateShop", "False"));
            ALLOW_DSKILLS = Boolean.valueOf(serviseSet.getProperty("AllowDonateSkillsShop", "False"));
            DS_ALL_SUBS = Boolean.valueOf(serviseSet.getProperty("DonateSkillsAllSubs", "False"));

            ALLOW_CSHOP = Boolean.valueOf(serviseSet.getProperty("AllowChinaShop", "False"));
            //
            /*
             * ALLOW_FAKE_PLAYERS =
             * Boolean.parseBoolean(serviseSet.getProperty("AllowFakePlayers",
             * "False")); FAKE_PLAYERS_PERCENT =
             * Integer.parseInt(serviseSet.getProperty("FakePlayersCount",
             * "100")); ONLINE_PERC =
             * Float.parseFloat(serviseSet.getProperty("FakePlayersPercent",
             * "0")); FAKE_PLAYERS_DELAY =
             * Integer.parseInt(serviseSet.getProperty("FakePlayersDelay",
             * "600000"));
             */
            //
            PWHERO_COIN = Integer.parseInt(serviseSet.getProperty("BBSHeroCoin", "5962"));
            PWHERO_PRICE = Integer.parseInt(serviseSet.getProperty("BBSHeroCoinDayPrice", "999"));
            PWHERO_FPRICE = Integer.parseInt(serviseSet.getProperty("BBSHeroCoinForeverPrice", "999"));
            PWHERO_MINDAYS = Integer.parseInt(serviseSet.getProperty("BBSHeroMinDays", "1"));
            PWHERO_TRANPRICE = Integer.parseInt(serviseSet.getProperty("BBSHeroCoinTransferPrice", "999"));
            PWCSKILLS_COIN = Integer.parseInt(serviseSet.getProperty("BBSCustomSkillCoin", "5962"));
            PWCSKILLS_PRICE = Integer.parseInt(serviseSet.getProperty("BBSCustomSkillPrice", "999"));
            PWENCHSKILL_COIN = Integer.parseInt(serviseSet.getProperty("BBSEnchantSkillCoin", "5962"));
            PWENCHSKILL_PRICE = Integer.parseInt(serviseSet.getProperty("BBSEnchantSkillPrice", "999"));
            PWCNGSKILLS_COIN = Integer.parseInt(serviseSet.getProperty("BBSTransferSkillCoin", "0"));
            PWCNGSKILLS_PRICE = Integer.parseInt(serviseSet.getProperty("BBSTransferSkillPrice", "999"));
            //
            BBS_AUC_ITEM_COIN = Integer.parseInt(serviseSet.getProperty("AucItemCoin", "4037"));
            BBS_AUC_ITEM_PRICE = Integer.parseInt(serviseSet.getProperty("AucItemPrice", "1"));
            BBS_AUC_ITEM_NAME = serviseSet.getProperty("AucItemName", "Coin Of Luck");
            BBS_AUC_AUG_COIN = Integer.parseInt(serviseSet.getProperty("AucAugCoin", "4037"));
            BBS_AUC_AUG_PRICE = Integer.parseInt(serviseSet.getProperty("AucAugPrice", "1"));
            BBS_AUC_AUG_NAME = serviseSet.getProperty("AucAugName", "Coin Of Luck");
            BBS_AUC_SKILL_COIN = Integer.parseInt(serviseSet.getProperty("AucSkillCoin", "4037"));
            BBS_AUC_SKILL_PRICE = Integer.parseInt(serviseSet.getProperty("AucSkillPrice", "1"));
            BBS_AUC_SKILL_NAME = serviseSet.getProperty("AucSkillName", "Coin Of Luck");
            BBS_AUC_HERO_COIN = Integer.parseInt(serviseSet.getProperty("AucHeroCoin", "4037"));
            BBS_AUC_HERO_PRICE = Integer.parseInt(serviseSet.getProperty("AucHeroPrice", "1"));
            BBS_AUC_HERO_NAME = serviseSet.getProperty("AucHeroName", "Coin Of Luck");
            BBS_AUC_EXPIRE_DAYS = Integer.parseInt(serviseSet.getProperty("AucItemsExpireDays", "7"));

            propertySplit = serviseSet.getProperty("AucMoney", "57,Adena;4037,Coin Of Luck").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    BBS_AUC_MONEYS.put(Integer.parseInt(aug[0]), aug[1]);
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("services.cfg: AucMoney error: " + aug[0]);
                    }
                }
            }

            propertySplit = serviseSet.getProperty("BBSCustomSkills", "9999,9").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    PWCSKILLS.put(Integer.valueOf(aug[0]), Integer.valueOf(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("services.cfg: BBSCustomSkills error: " + aug[0]);
                    }
                }
            }

            PWNCOLOR_COIN = Integer.parseInt(serviseSet.getProperty("BBSColorNameCoin", "5962"));
            PWNCOLOR_PRICE = Integer.parseInt(serviseSet.getProperty("BBSColorNamePrice", "999"));
            PWNCOLOR_COINNAME = serviseSet.getProperty("BBSColorNameCoinName", "Gold Golem");
            PWTCOLOR_COIN = Integer.parseInt(serviseSet.getProperty("BBSColorTitleCoin", "5962"));
            PWTCOLOR_PRICE = Integer.parseInt(serviseSet.getProperty("BBSColorTitlePrice", "999"));
            PWTCOLOR_COINNAME = serviseSet.getProperty("BBSColorTitleCoinName", "Gold Golem");
            PWTCOLOR_PAYMENT = Boolean.valueOf(serviseSet.getProperty("BBSColorNextChangeFree", "True"));

            BBS_CNAME_COIN = Integer.parseInt(serviseSet.getProperty("BBSChangeNameCoin", "5962"));
            BBS_CNAME_PRICE = Integer.parseInt(serviseSet.getProperty("BBSChangeNamePrice", "999"));
            BBS_CNAME_VAL = serviseSet.getProperty("BBSChangeNameCoinName", "Gold Golem");

            PWCNGCLASS_COIN = Integer.parseInt(serviseSet.getProperty("BBSChangeClassCoin", "5962"));
            PWCNGCLASS_PRICE = Integer.parseInt(serviseSet.getProperty("BBSChangeClassPrice", "999"));

            int count = 0;
            propertySplit = serviseSet.getProperty("BBSPaintColors", "00FF00,00FF00").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    PWCOLOURS.put(count, new AltBColor(Integer.decode("0x" + aug[0]), aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("services.cfg: BBSPaintColors error: " + aug[0]);
                    }
                }
                count++;
            }
            PWHERO_COINNAME = serviseSet.getProperty("BBSHeroCoinName", "Gold Golem");
            PWCSKILLS_COINNAME = serviseSet.getProperty("BBSCustomSkillCoinName", "Gold Golem");
            PWENCHSKILL_COINNAME = serviseSet.getProperty("BBSEnchantSkillCoinName", "Gold Golem");

            PREMIUM_ENABLE = Boolean.parseBoolean(serviseSet.getProperty("PremiumEnable", "False"));
            PREMIUM_COIN = Integer.parseInt(serviseSet.getProperty("PremiumCoin", "5962"));
            PREMIUM_PRICE = Integer.parseInt(serviseSet.getProperty("PremiumPrice", "5962"));
            PREMIUM_COINNAME = serviseSet.getProperty("PremiumCoinName", "Gold Golem");
            propertySplit = serviseSet.getProperty("PremiumDayPrice", "99,2220").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    PREMIUM_DAY_PRICES.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("services.cfg: PremiumDayPrice error: " + aug[0]);
                    }
                }
            }

            NOBLES_ENABLE = Boolean.parseBoolean(serviseSet.getProperty("NobleEnable", "False"));
            SNOBLE_COIN = Integer.parseInt(serviseSet.getProperty("NobleCoin", "5962"));
            SNOBLE_PRICE = Integer.parseInt(serviseSet.getProperty("NoblePrice", "5962"));
            SNOBLE_COIN_NAME = serviseSet.getProperty("NobleCoinName", "Gold Golem");

            PREMIUM_EXP = Double.parseDouble(serviseSet.getProperty("PremiumExp", "1.3"));
            PREMIUM_SP = Double.parseDouble(serviseSet.getProperty("PremiumSp", "1.3"));
            PREMIUM_ITEMDROP = Double.parseDouble(serviseSet.getProperty("PremiumDropItem", "1.3"));
            PREMIUM_ITEMDROPMUL = Double.parseDouble(serviseSet.getProperty("PremiumDropMul", "1.3"));
            PREMIUM_SPOILRATE = Double.parseDouble(serviseSet.getProperty("PremiumDropSpoil", "1.3"));
            // PREMIUM_ADENADROP =
            // Float.parseFloat(serviseSet.getProperty("PremiumDropAdena", "1.3"));
            PREMIUM_ADENAMUL = Double.parseDouble(serviseSet.getProperty("PremiumAdenaMul", "1.3"));
            PREMIUM_PCCAFE_MUL = Double.parseDouble(serviseSet.getProperty("PremiumPcCafeMul", "1.3"));
            PREMIUM_AQURE_SKILL_MUL = Double.parseDouble(serviseSet.getProperty("PremiumClanSkillsMul", "0.7"));
            PREMIUM_AUGMENT_RATE = Integer.parseInt(serviseSet.getProperty("PremiumAugmentRate", "0"));
            PREMIUM_ENCH_ITEM = Integer.parseInt(serviseSet.getProperty("PremiumEnchRate", "0"));
            PREMIUM_ENCH_SKILL = Integer.parseInt(serviseSet.getProperty("PremiumEnchSkillRate", "0"));
            PREMIUM_CURSED_RATE = Integer.parseInt(serviseSet.getProperty("PremiumCursedRate", "0")) * 1000;

            PREMIUM_ANY_SUBCLASS = Boolean.parseBoolean(serviseSet.getProperty("PremiumAnySubclass", "False"));
            PREMIUM_CHKSKILLS = Boolean.parseBoolean(serviseSet.getProperty("PremiumCheckSkills", "True"));
            PREMIUM_PKDROP_OFF = Boolean.parseBoolean(serviseSet.getProperty("PremiumDisablePkDrop", "False"));

            PREMIUM_ANOOUNCE = Boolean.parseBoolean(serviseSet.getProperty("PremiumAnnounceEnter", "False"));
            PREMIUM_ANNOUNCE_PHRASE = serviseSet.getProperty("PremiumAnnouncePhrase", "Игрок %player% вошел в игру.");

            PREMIUM_ENCHANT_FAIL = Boolean.parseBoolean(serviseSet.getProperty("PremiumAltEnchantFail", "False"));

            PREMIUM_NAME_PREFIX = serviseSet.getProperty("PremiumNamePrefix", "");

            PREMIUM_START_DAYS = Integer.parseInt(serviseSet.getProperty("PremiumNewCharDays", "0"));

            HERO_START_DAYS = Integer.parseInt(serviseSet.getProperty("HeroNewCharHours", "0"));

            PREMIUM_ITEMS = Boolean.parseBoolean(serviseSet.getProperty("PremiumItems", "False"));
            propertySplit = serviseSet.getProperty("PremiumItemsList", "").split(",");
            for (String item : propertySplit) {
                try {
                    PREMIUM_ITEMS_LIST.add(Integer.valueOf(item));
                } catch (NumberFormatException nfe) {
                    if (!item.equals("")) {
                        System.out.println("services.cfg: PremiumItemsList error: " + item);
                    }
                }
            }

            propertySplit = serviseSet.getProperty("ProtectedPremiumItems", "").split(",");
            for (String item : propertySplit) {
                try {
                    PREMIUM_PROTECTED_ITEMS.add(Integer.valueOf(item));
                } catch (NumberFormatException nfe) {
                    if (!item.equals("")) {
                        System.out.println("services.cfg: PremiumDayPrice error: " + item);
                    }
                }
            }

            BBS_ONLY_PEACE = Boolean.parseBoolean(serviseSet.getProperty("BbsPeace", "True"));

            /**
             * L2JMOD Wedding system
             */
            L2JMOD_ALLOW_WEDDING = Boolean.valueOf(serviseSet.getProperty("AllowWedding", "False"));
            L2JMOD_WEDDING_PUNISH_INFIDELITY = Boolean
                    .parseBoolean(serviseSet.getProperty("WeddingPunishInfidelity", "True"));
            L2JMOD_WEDDING_TELEPORT = Boolean.parseBoolean(serviseSet.getProperty("WeddingTeleport", "True"));
            L2JMOD_WEDDING_TELEPORT_PRICE = Integer.parseInt(serviseSet.getProperty("WeddingTeleportPrice", "50000"));
            L2JMOD_WEDDING_TELEPORT_DURATION = Integer
                    .parseInt(serviseSet.getProperty("WeddingTeleportDuration", "60"));
            L2JMOD_WEDDING_SAMESEX = Boolean.parseBoolean(serviseSet.getProperty("WeddingAllowSameSex", "False"));
            L2JMOD_WEDDING_FORMALWEAR = Boolean.parseBoolean(serviseSet.getProperty("WeddingFormalWear", "True"));

            L2JMOD_WEDDING_COIN = Integer.parseInt(serviseSet.getProperty("WeddingCoin", "4037"));
            L2JMOD_WEDDING_PRICE = Integer.parseInt(serviseSet.getProperty("WeddingPrice", "5"));
            L2JMOD_WEDDING_COINNAME = serviseSet.getProperty("WeddingCoinName", "Coin Of Luck");

            L2JMOD_WEDDING_DIVORCE_COIN = Integer.parseInt(serviseSet.getProperty("WeddingDivorceCoin", "4037"));
            L2JMOD_WEDDING_DIVORCE_PRICE = Integer.parseInt(serviseSet.getProperty("WeddingDivorcePrice", "5"));
            L2JMOD_WEDDING_DIVORCE_COINNAME = serviseSet.getProperty("WeddingDivorceCoinName", "Coin Of Luck");

            L2JMOD_WEDDING_INTERVAL = (int) TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(serviseSet.getProperty("WeddingInterval", "90")));

            propertySplit = serviseSet.getProperty("WeddingColors", "FFFFFF,FFFFFF").split(",");
            WEDDING_COLORS = new PvpColor(Integer.decode("0x" + new TextBuilder(propertySplit[0]).reverse().toString()),
                    Integer.decode("0x" + new TextBuilder(propertySplit[1]).reverse().toString()));

            L2JMOD_WEDDING_BOW = Boolean.valueOf(serviseSet.getProperty("WeddingCupidBow", "False"));
            //
            L2TOP_ENABLE = Boolean.parseBoolean(serviseSet.getProperty("L2TopEnable", "False"));
            L2TOP_SERV_URL = serviseSet.getProperty("L2TopServerUrl", "empty");
            L2TOP_UPDATE_DELAY = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(serviseSet.getProperty("L2TopUpdateDelay", "5")));

            L2TOP_OFFLINE_ITEM = Integer.parseInt(serviseSet.getProperty("L2TopOfflineId", "0"));
            L2TOP_OFFLINE_COUNT = Integer.parseInt(serviseSet.getProperty("L2TopOfflineCount", "0"));
            L2TOP_OFFLINE_LOC = serviseSet.getProperty("L2TopOfflineLoc", "INVENTORY");
            propertySplit = serviseSet.getProperty("L2TopOnlineRewards", "57,13,100;57,13,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    L2TOP_ONLINE_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("services.cfg: L2TopOnlineRewards error: " + aug[0]);
                    }
                }
            }
            L2TOP_LOGTYPE = Integer.parseInt(serviseSet.getProperty("L2TopLog", "1"));
            //
            MMOTOP_ENABLE = Boolean.parseBoolean(serviseSet.getProperty("MmotopEnable", "False"));
            MMOTOP_STAT_LINK = serviseSet.getProperty("MmotopStatLink", "0");
            MMOTOP_UPDATE_DELAY = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(serviseSet.getProperty("MmotopUpdateDelay", "5")));

            MMOTOP_OFFLINE_ITEM = Integer.parseInt(serviseSet.getProperty("MmotopOfflineId", "0"));
            MMOTOP_OFFLINE_COUNT = Integer.parseInt(serviseSet.getProperty("MmotopOfflineCount", "0"));
            MMOTOP_OFFLINE_LOC = serviseSet.getProperty("MmotopOfflineLoc", "INVENTORY");
            propertySplit = serviseSet.getProperty("MmotopOnlineRewards", "57,13,100;57,13,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    MMOTOP_ONLINE_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("services.cfg: MmotopOnlineRewards error: " + aug[0]);
                    }
                }
            }
            MMOTOP_LOGTYPE = Integer.parseInt(serviseSet.getProperty("MmotopLog", "1"));
            //
            EXPOSTB_COIN = Integer.parseInt(serviseSet.getProperty("EpBriefCoin", "4037"));
            EXPOSTB_PRICE = Integer.parseInt(serviseSet.getProperty("EpBriefPrice", "1"));
            EXPOSTB_NAME = serviseSet.getProperty("EpBriefCoinName", "Coin Of Luck");
            EXPOSTA_COIN = Integer.parseInt(serviseSet.getProperty("EpItemCoin", "4037"));
            EXPOSTA_PRICE = Integer.parseInt(serviseSet.getProperty("EpItemPrice", "5"));
            EXPOSTA_NAME = serviseSet.getProperty("EpItemCoinName", "Coin Of Luck");
            //
            PC_CAFE_ENABLED = Boolean.parseBoolean(serviseSet.getProperty("PcCafeEnable", "False"));
            PC_CAFE_INTERVAL = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(serviseSet.getProperty("PcCafeUpdateDelay", "60")));
            // PC_CAFE_BONUS = Integer.parseInt(serviseSet.getProperty("PcCafeUpdateBonus",
            // "60"));
            PC_CAFE_DOUBLE_CHANCE = Integer.parseInt(serviseSet.getProperty("PcCafeUpdateDoubleChance", "60"));

            propertySplit = serviseSet.getProperty("PcCafeUpdateBonus", "30,60").split(",");
            PC_CAFE_BONUS = new PvpColor(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]));
            //
            CACHED_SERVER_STAT = Boolean.parseBoolean(serviseSet.getProperty("ServerStat", "False"));
            //
            QUED_ITEMS_ENABLE = Boolean.parseBoolean(serviseSet.getProperty("QuedItems", "False"));
            QUED_ITEMS_INTERVAL = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(serviseSet.getProperty("QuedItemsInterval", "5")));
            QUED_ITEMS_LOGTYPE = Integer.parseInt(serviseSet.getProperty("QuedItemsLog", "0"));
            //
            ALLOW_UNIQ_SKILLS = Boolean.parseBoolean(serviseSet.getProperty("AllowUniqSkills", "False"));
            propertySplit = serviseSet.getProperty("UniqSkills", "1,2").split(";");
            for (String buffs : propertySplit) {
                String[] pbuff = buffs.split(",");
                try {
                    UNIQ_SKILLS.put(Integer.valueOf(pbuff[0]), Integer.valueOf(pbuff[1]));
                } catch (NumberFormatException nfe) {
                    if (!pbuff[0].equals("")) {
                        System.out.println("services.cfg: UniqSkills error: " + pbuff[0]);
                    }
                }
            }
            //
            VS_VREF_NAME = Boolean.parseBoolean(serviseSet.getProperty("VoteRefName", "False"));
            //
            VOTE_SERVER_PREFIX = serviseSet.getProperty("VoteServerPrefix", "");
            //
            VOTE_NCS_SPAWN = Boolean.parseBoolean(serviseSet.getProperty("VoteRefNcsSpawn", "False"));
            //
            CHGSEX_ENABLE = Boolean.parseBoolean(serviseSet.getProperty("ChangeSexEnable", "False"));
            CHGSEX_COIN = Integer.parseInt(serviseSet.getProperty("ChangeSexCoin", "5962"));
            CHGSEX_PRICE = Integer.parseInt(serviseSet.getProperty("ChangeSexPrice", "5962"));
            CHGSEX_COIN_NAME = serviseSet.getProperty("ChangeSexCoinName", "Gold Golem");
            //
            CHGCLASS_ENABLE = Boolean.parseBoolean(serviseSet.getProperty("ChangeClassEnable", "False"));
            CHGCLASS_COIN = Integer.parseInt(serviseSet.getProperty("ChangeClass", "5962"));
            CHGCLASS_PRICE = Integer.parseInt(serviseSet.getProperty("ChangeClassPrice", "5962"));
            CHGCLASS_COIN_NAME = serviseSet.getProperty("ChangeClassCoinName", "Gold Golem");
            //
            GLD_80LVL_COIN = Integer.parseInt(serviseSet.getProperty("Gld80LevelCoin", "5962"));
            GLD_80LVL_PRICE = Integer.parseInt(serviseSet.getProperty("Gld80LevelPrice", "990"));
            GLD_HEROCHAT_COIN = Integer.parseInt(serviseSet.getProperty("GldHeroChatCoin", "5962"));
            GLD_HEROCHAT_PRICE = Integer.parseInt(serviseSet.getProperty("Gld8HeroChatPrice", "990"));
            //
            propertySplit = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + SERVICE_FILE + " File.");
        }
    }

    public static void loadEventsCfg() {
        TVT_EVENT_REWARDS.clear();
        TVT_EVENT_REWARDS_TOP.clear();
        TVT_EVENT_DOOR_IDS.clear();
        TVT_WHITE_POTINS.clear();
        OS_REWARDS.clear();
        ELH_REWARDS.clear();
        FC_ALLOWITEMS.clear();
        XM_DROP.clear();
        MEDAL_EVENT_DROP.clear();
        EBC_REWARDS.clear();
        EENC_REWARDS.clear();
        EENC_POINTS.clear();
        ANARCHY_TOWNS.clear();
        ELH_FORB_POTIONS.clear();
        BE_REWARDS.clear();
        BE_GUARDS.clear();
        GUILD_MOD_NAMES.clear();
        GUILD_MOD_MASKS.clear();
        GUILD_MOD_REWARDS.clear();
        RUNE_SKILLS.clear();
        EVENT_KILLSITEMS.clear();
        TVT_KILLSITEMS.clear();
        MASSPVP_KILLSITEMS.clear();
        CAPBASE_KILLSITEMS.clear();
        LASTHERO_KILLSITEMS.clear();

        try {
            Properties eventsSettings = new Properties();
            InputStream is = new FileInputStream(new File(EVENT_FILE));
            eventsSettings.load(is);
            is.close();

            MASS_PVP = Boolean.valueOf(eventsSettings.getProperty("AllowMassPvP", "False"));
            MPVP_RTIME = Long.parseLong(eventsSettings.getProperty("AfterServetStart", "60"));
            MPVP_REG = Long.parseLong(eventsSettings.getProperty("Registration", "15"));
            MPVP_ANC = Long.parseLong(eventsSettings.getProperty("AnnouncePeriod", "2"));
            MPVP_TP = Long.parseLong(eventsSettings.getProperty("Teleport", "5"));
            MPVP_PR = Long.parseLong(eventsSettings.getProperty("Buff", "60"));
            // MPVP_WT;
            MPVP_MAX = Long.parseLong(eventsSettings.getProperty("Battle", "60"));
            MPVP_NEXT = Long.parseLong(eventsSettings.getProperty("Next", "24"));
            MPVP_MAXC = Integer.parseInt(eventsSettings.getProperty("Rounds", "5"));
            MPVP_NPC = Integer.parseInt(eventsSettings.getProperty("RegNpc", "5"));
            MPVP_CREW = Integer.parseInt(eventsSettings.getProperty("RoundReward", "4355"));
            MPVP_CREWC = Integer.parseInt(eventsSettings.getProperty("RoundCount", "3"));
            MPVP_EREW = Integer.parseInt(eventsSettings.getProperty("FinalReward", "4355"));
            MPVP_EREWC = Integer.parseInt(eventsSettings.getProperty("FinalCount", "30"));
            MPVP_LVL = Integer.parseInt(eventsSettings.getProperty("MinLelev", "76"));
            MPVP_MAXP = Integer.parseInt(eventsSettings.getProperty("MaxPlayers", "60"));
            MPVP_NOBL = Boolean.valueOf(eventsSettings.getProperty("OnlyNobless", "True"));
            TVT_NOBL = Boolean.valueOf(eventsSettings.getProperty("TvTOnlyNobless", "True"));

            String[] propertySplit = eventsSettings.getProperty("Npc", "116530,76141,-2730").split(",");
            MPVP_NPCLOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));

            // propertySplit = eventsSettings.getProperty("Back",
            // "116530,76141,-2730").split(",");
            // MPVP_TPLOC = new Location(Integer.parseInt(propertySplit[0]),
            // Integer.parseInt(propertySplit[1]), Integer.parseInt(propertySplit[2]));
            propertySplit = eventsSettings.getProperty("Back", "116530,76141,-2730").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    MPVP_TPLOC.add(
                            new Location(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: Back error: " + aug[0]);
                    }
                }
            }

            // propertySplit = eventsSettings.getProperty("Cycle",
            // "-92939,-251113,-3331").split(",");
            // MPVP_CLOC = new Location(Integer.parseInt(propertySplit[0]),
            // Integer.parseInt(propertySplit[1]), Integer.parseInt(propertySplit[2]));
            propertySplit = eventsSettings.getProperty("Cycle", "-92939,-251113,-3331").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    MPVP_CLOC.add(
                            new Location(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: Cycle error: " + aug[0]);
                    }
                }
            }

            // propertySplit = eventsSettings.getProperty("Final",
            // "-92939,-251113,-3331").split(",");
            // MPVP_WLOC = new Location(Integer.parseInt(propertySplit[0]),
            // Integer.parseInt(propertySplit[1]), Integer.parseInt(propertySplit[2]));
            /*
             * propertySplit = eventsSettings.getProperty("Final",
             * "-92939,-251113,-3331").split(";");
             * for (String augs : propertySplit) {
             * String[] aug = augs.split(",");
             * try {
             * MPVP_WLOC.add(new Location(Integer.parseInt(aug[0]),
             * Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
             * } catch (NumberFormatException nfe) {
             * if (!aug[0].equals("")) {
             * System.out.println("events.cfg: Final error: " + aug[0]);
             * }
             * }
             * }
             */
            TVT_EVENT_ENABLED = Boolean.parseBoolean(eventsSettings.getProperty("TvTEventEnabled", "false"));
            TVT_EVENT_INTERVAL = Integer.parseInt(eventsSettings.getProperty("TvTEventInterval", "18000"));
            TVT_EVENT_PARTICIPATION_TIME = Integer
                    .parseInt(eventsSettings.getProperty("TvTEventParticipationTime", "3600"));
            TVT_EVENT_RUNNING_TIME = Integer.parseInt(eventsSettings.getProperty("TvTEventRunningTime", "1800"));
            TVT_EVENT_PARTICIPATION_NPC_ID = Integer
                    .parseInt(eventsSettings.getProperty("TvTEventParticipationNpcId", "0"));

            if (TVT_EVENT_PARTICIPATION_NPC_ID == 0) {
                TVT_EVENT_ENABLED = false;
                System.out.println(
                        "TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcId");
            } else {
                propertySplit = eventsSettings
                        .getProperty("TvTEventParticipationNpcCoordinates", "83425,148585,-3406;83465,148485,-3406")
                        .split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        TVT_EVENT_PARTICIPATION_NPC_COORDINATES.add(new Location(Integer.parseInt(aug[0]),
                                Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("events.cfg: TvTEventParticipationNpcCoordinates error: " + aug[0]);
                        }
                    }
                }

                TVT_EVENT_MIN_PLAYERS_IN_TEAMS = Integer
                        .parseInt(eventsSettings.getProperty("TvTEventMinPlayersInTeams", "1"));
                TVT_EVENT_MAX_PLAYERS_IN_TEAMS = Integer
                        .parseInt(eventsSettings.getProperty("TvTEventMaxPlayersInTeams", "20"));
                TVT_EVENT_MIN_LVL = (byte) Integer.parseInt(eventsSettings.getProperty("TvTEventMinPlayerLevel", "1"));
                TVT_EVENT_MAX_LVL = (byte) Integer.parseInt(eventsSettings.getProperty("TvTEventMaxPlayerLevel", "80"));
                TVT_EVENT_RESPAWN_TELEPORT_DELAY = Integer
                        .parseInt(eventsSettings.getProperty("TvTEventRespawnTeleportDelay", "20"));
                TVT_EVENT_START_LEAVE_TELEPORT_DELAY = Integer
                        .parseInt(eventsSettings.getProperty("TvTEventStartLeaveTeleportDelay", "20"));

                TVT_HIDE_NAMES = Boolean.parseBoolean(eventsSettings.getProperty("TvtHideNames", "False"));
                TVT_ALT_NAME = eventsSettings.getProperty("TvtForbidMagic", "False");

                TVT_EVENT_TEAM_1_NAME = eventsSettings.getProperty("TvTEventTeam1Name", "Team1");
                propertySplit = eventsSettings.getProperty("TvTEventTeam1Coordinates", "0,0,0").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        TVT_EVENT_TEAM_1_COORDINATES.add(new Location(Integer.parseInt(aug[0]),
                                Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("events.cfg: TvTEventTeam1Coordinates error: " + aug[0]);
                        }
                    }
                }

                TVT_EVENT_TEAM_2_NAME = eventsSettings.getProperty("TvTEventTeam2Name", "Team2");
                propertySplit = eventsSettings.getProperty("TvTEventTeam2Coordinates", "0,0,0").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        TVT_EVENT_TEAM_2_COORDINATES.add(new Location(Integer.parseInt(aug[0]),
                                Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("events.cfg: TvTEventTeam1Coordinates error: " + aug[0]);
                        }
                    }
                }

                propertySplit = eventsSettings.getProperty("TvTReturnCoordinates", "0,0,0").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        TVT_RETURN_COORDINATES.add(new Location(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                                Integer.parseInt(aug[2])));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("events.cfg: TvTReturnCoordinates error: " + aug[0]);
                        }
                    }
                }

                propertySplit = eventsSettings.getProperty("TvTEventReward", "57,100000").split(";");
                for (String reward : propertySplit) {
                    String[] rewardSplit = reward.split(",");

                    if (rewardSplit.length != 2) {
                        System.out.println("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \""
                                + reward + "\"");
                    } else {
                        try {
                            TVT_EVENT_REWARDS.add(
                                    new int[] { Integer.valueOf(rewardSplit[0]), Integer.valueOf(rewardSplit[1]) });
                        } catch (NumberFormatException nfe) {
                            if (!reward.equals("")) {
                                System.out.println(
                                        "TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \""
                                                + reward + "\"");
                            }
                        }
                    }
                }

                TVT_EVENT_REWARDS_TOP_KILLS = Integer
                        .parseInt(eventsSettings.getProperty("TvTEventRewardTopKills", "0"));
                propertySplit = eventsSettings.getProperty("TvTEventRewardTop", "57,100000").split(";");
                for (String reward : propertySplit) {
                    String[] rewardSplit = reward.split(",");

                    if (rewardSplit.length != 2) {
                        System.out.println(
                                "TvTEventEngine[Config.load()]: invalid config property -> TvTEventRewardTop \""
                                        + reward + "\"");
                    } else {
                        try {
                            TVT_EVENT_REWARDS_TOP.add(
                                    new int[] { Integer.valueOf(rewardSplit[0]), Integer.valueOf(rewardSplit[1]) });
                        } catch (NumberFormatException nfe) {
                            if (!reward.equals("")) {
                                System.out.println(
                                        "TvTEventEngine[Config.load()]: invalid config property -> TvTEventRewardTop \""
                                                + reward + "\"");
                            }
                        }
                    }
                }

                TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED = Boolean
                        .parseBoolean(eventsSettings.getProperty("TvTEventTargetTeamMembersAllowed", "true"));
                TVT_EVENT_POTIONS_ALLOWED = Boolean
                        .parseBoolean(eventsSettings.getProperty("TvTEventPotionsAllowed", "false"));
                TVT_EVENT_SUMMON_BY_ITEM_ALLOWED = Boolean
                        .parseBoolean(eventsSettings.getProperty("TvTEventSummonByItemAllowed", "false"));
                propertySplit = eventsSettings.getProperty("TvTEventDoorsCloseOpenOnStartEnd", "").split(";");
                for (String door : propertySplit) {
                    try {
                        TVT_EVENT_DOOR_IDS.add(Integer.valueOf(door));
                    } catch (NumberFormatException nfe) {
                        if (!door.equals("")) {
                            System.out.println(
                                    "TvTEventEngine[Config.load()]: invalid config property -> TvTEventDoorsCloseOpenOnStartEnd \""
                                            + door + "\"");
                        }
                    }
                }

                propertySplit = eventsSettings.getProperty("TvTEventAllowedPotionsList", "").split(",");
                for (String potion : propertySplit) {
                    try {
                        TVT_WHITE_POTINS.add(Integer.valueOf(potion));
                    } catch (NumberFormatException nfe) {
                        if (!potion.equals("")) {
                            System.out.println(
                                    "TvTEventEngine[Config.load()]: invalid config property -> TvTEventAllowedPotionsList \""
                                            + potion + "\"");
                        }
                    }
                }
            }

            TVT_NO_PASSIVE = Boolean.parseBoolean(eventsSettings.getProperty("TvTNoPassive", "False"));
            TVT_REWARD_CHECK = Boolean.parseBoolean(eventsSettings.getProperty("TvTRewardCheck", "False"));
            TVT_REWARD_TOP = Boolean.parseBoolean(eventsSettings.getProperty("TvTRewardTop", "False"));
            TVT_TEAM_PROTECT = Boolean.parseBoolean(eventsSettings.getProperty("TvTTeamProtect", "False"));
            TVT_NO_DEADS = Boolean.parseBoolean(eventsSettings.getProperty("TvTNoDeads", "False"));
            TVT_TELE_PROTECT = (int) TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(eventsSettings.getProperty("TvTTeleProtect", "0")));

            //
            ALLOW_SCH = Boolean.valueOf(eventsSettings.getProperty("AllowSchuttgart", "False"));
            SCH_NEXT = Integer.parseInt(eventsSettings.getProperty("SchNext", "24"));
            SCH_RESTART = Integer.parseInt(eventsSettings.getProperty("SchRestart", "60"));

            SCH_TIME1 = Integer.parseInt(eventsSettings.getProperty("SchWave1", "120000"));
            SCH_TIME2 = Integer.parseInt(eventsSettings.getProperty("SchWave2", "30000"));
            SCH_TIME3 = Integer.parseInt(eventsSettings.getProperty("SchWave3", "15000"));
            SCH_TIME4 = Integer.parseInt(eventsSettings.getProperty("SchWave4", "15000"));
            SCH_TIME5 = Integer.parseInt(eventsSettings.getProperty("SchWave5", "15000"));
            SCH_TIME6 = Integer.parseInt(eventsSettings.getProperty("SchWave6", "15000"));
            SCH_TIMEBOSS = Integer.parseInt(eventsSettings.getProperty("SchWaveBoss", "400000"));

            SCH_MOB1 = Integer.parseInt(eventsSettings.getProperty("SchMob1", "80100"));
            SCH_MOB2 = Integer.parseInt(eventsSettings.getProperty("SchMob2", "80101"));
            SCH_MOB3 = Integer.parseInt(eventsSettings.getProperty("SchMob3", "80101"));
            SCH_MOB4 = Integer.parseInt(eventsSettings.getProperty("SchMob4", "80100"));
            SCH_MOB5 = Integer.parseInt(eventsSettings.getProperty("SchMob5", "80102"));
            SCH_MOB6 = Integer.parseInt(eventsSettings.getProperty("SchMob6", "80103"));
            SCH_BOSS = Integer.parseInt(eventsSettings.getProperty("SchBoss", "80104"));

            SCH_ALLOW_SHOP = Boolean.valueOf(eventsSettings.getProperty("SchAllowShop", "False"));
            SCH_SHOP = Integer.parseInt(eventsSettings.getProperty("SchShopId", "80105"));
            SCH_SHOPTIME = Integer.parseInt(eventsSettings.getProperty("SchShopTimeout", "20"));
            //
            OPEN_SEASON = Boolean.valueOf(eventsSettings.getProperty("AllowOpenSeason", "False"));
            OS_NEXT = Integer.parseInt(eventsSettings.getProperty("OsNext", "600"));
            OS_RESTART = Integer.parseInt(eventsSettings.getProperty("OsAfterRestart", "60"));
            OS_BATTLE = Integer.parseInt(eventsSettings.getProperty("OsBattlePeriod", "2"));
            OS_FAIL_NEXT = Integer.parseInt(eventsSettings.getProperty("OsFailNext", "120"));
            OS_REGTIME = Integer.parseInt(eventsSettings.getProperty("OsRegTime", "300"));
            OS_ANNDELAY = Integer.parseInt(eventsSettings.getProperty("OsAnnounceDelay", "15"));
            OS_MINLVL = Integer.parseInt(eventsSettings.getProperty("OsMinLevel", "76"));
            OS_MINPLAYERS = Integer.parseInt(eventsSettings.getProperty("OsMinPlayers", "10"));

            propertySplit = eventsSettings.getProperty("OsRewards", "4037,500,100;4355,15,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    OS_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("events.cfg: OsRewards error: " + aug[0]);
                    }
                }
            }
            //
            ELH_ENABLE = Boolean.parseBoolean(eventsSettings.getProperty("LastHeroEnable", "False"));
            ELH_ARTIME = Long.parseLong(eventsSettings.getProperty("LhAfterRestart", "30"));
            ELH_REGTIME = Long.parseLong(eventsSettings.getProperty("LhRegPeriod", "30"));
            ELH_ANNDELAY = Long.parseLong(eventsSettings.getProperty("LhAnounceDelay", "30"));
            ELH_TPDELAY = Long.parseLong(eventsSettings.getProperty("LhTeleportDelay", "30"));
            ELH_NEXT = Long.parseLong(eventsSettings.getProperty("LhNextStart", "30"));

            ELH_MINLVL = Integer.parseInt(eventsSettings.getProperty("LhPlayerMinLvl", "76"));
            ELH_MINP = Integer.parseInt(eventsSettings.getProperty("LhMinPlayers", "76"));
            ELH_MAXP = Integer.parseInt(eventsSettings.getProperty("LhMaxPlayers", "76"));

            ELH_NPCID = Integer.parseInt(eventsSettings.getProperty("LhRegNpcId", "55558"));

            propertySplit = eventsSettings.getProperty("LhRegNpcLoc", "83101,148396,-3407").split(",");
            ELH_NPCLOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));
            ELH_NPCTOWN = eventsSettings.getProperty("LhRegNpcTown", "Гиране");

            propertySplit = eventsSettings.getProperty("LhBattleLoc", "83101,148396,-3407").split(",");
            ELH_TPLOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));

            ELH_TICKETID = Integer.parseInt(eventsSettings.getProperty("LhTicketId", "0"));
            ELH_TICKETCOUNT = Integer.parseInt(eventsSettings.getProperty("LhTicketCount", "0"));

            propertySplit = eventsSettings.getProperty("LhRewards", "9901,500,100;57,1,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ELH_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("events.cfg: LhRewards error: " + aug[0]);
                    }
                }
            }
            ELH_HERO_DAYS = Integer.parseInt(eventsSettings.getProperty("LhRewardDays", "1"));

            ELH_HIDE_NAMES = Boolean.parseBoolean(eventsSettings.getProperty("LhHideNames", "False"));
            ELH_FORBID_MAGIC = Boolean.parseBoolean(eventsSettings.getProperty("LhForbidMagic", "False"));
            ELH_ALT_NAME = eventsSettings.getProperty("LhAltName", "");
            propertySplit = eventsSettings.getProperty("LhForbiddenPotions", "").split(",");
            for (String potion : propertySplit) {
                try {
                    ELH_FORB_POTIONS.add(Integer.valueOf(potion));
                } catch (NumberFormatException nfe) {
                    if (!potion.equals("")) {
                        System.out.println("events.cfg: LhForbiddenPotions error: " + potion);
                    }
                }
            }

            //
            FC_INSERT_INVENTORY = Boolean.parseBoolean(eventsSettings.getProperty("FightClubInsertInventory", "False"));
            propertySplit = eventsSettings.getProperty("FightClubItems", "1234").split(",");
            for (String id : propertySplit) {
                FC_ALLOWITEMS.add(Integer.parseInt(id));
            }

            //
            ALLOW_XM_SPAWN = Boolean.parseBoolean(eventsSettings.getProperty("AllowChristmassEvent", "False"));
            propertySplit = eventsSettings.getProperty("ChristmassDrop", "5556,3,10;5557,3,10;5558,3,10;5559,3,10")
                    .split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    XM_DROP.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("events.cfg: ChristmassDrop error: " + aug[0]);
                    }
                }
            }
            XM_TREE_LIFE = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(eventsSettings.getProperty("ChristmassTreeLife", "0")));

            //
            ALLOW_MEDAL_EVENT = Boolean.parseBoolean(eventsSettings.getProperty("AllowMedalsEvent", "False"));
            propertySplit = eventsSettings.getProperty("MedalsDrop", "6392,1,7;6393,1,2").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    MEDAL_EVENT_DROP.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: MedalsDrop error: " + aug[0]);
                    }
                }
            }

            //
            EBC_ENABLE = Boolean.parseBoolean(eventsSettings.getProperty("CaptureBaseEnable", "False"));
            EBC_ARTIME = Long.parseLong(eventsSettings.getProperty("CbAfterRestart", "30"));
            EBC_REGTIME = Long.parseLong(eventsSettings.getProperty("CbRegPeriod", "5"));
            EBC_ANNDELAY = Long.parseLong(eventsSettings.getProperty("CbAnounceDelay", "1"));
            EBC_TPDELAY = Long.parseLong(eventsSettings.getProperty("CbTeleportDelay", "1"));
            EBC_DEATHLAY = Long.parseLong(eventsSettings.getProperty("CbRespawnDelay", "5000"));
            EBC_NEXT = Long.parseLong(eventsSettings.getProperty("CbNextStart", "30"));

            EBC_MINLVL = Integer.parseInt(eventsSettings.getProperty("CbPlayerMinLvl", "76"));
            EBC_MINP = Integer.parseInt(eventsSettings.getProperty("CbMinPlayers", "5"));
            EBC_MAXP = Integer.parseInt(eventsSettings.getProperty("CbMaxPlayers", "100"));
            EBC_NPCID = Integer.parseInt(eventsSettings.getProperty("CbRegNpcId", "55558"));

            propertySplit = eventsSettings.getProperty("CbRegNpcLoc", "83101,148396,-3407").split(",");
            EBC_NPCLOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));

            propertySplit = eventsSettings.getProperty("CbBase1Loc", "175732,-87983,-5107").split(",");
            EBC_BASE1_LOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));
            propertySplit = eventsSettings.getProperty("CbBase2Loc", "175732,-87983,-5107").split(",");
            EBC_BASE2_LOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));

            EBC_NPCTOWN = eventsSettings.getProperty("CbRegNpcTown", "Гиране");

            EBC_BASE1ID = Integer.parseInt(eventsSettings.getProperty("CbBase1Id", "80050"));
            EBC_BASE2ID = Integer.parseInt(eventsSettings.getProperty("CbBase2Id", "80051"));

            EBC_BASE1NAME = eventsSettings.getProperty("CbTeam1Name", "White");
            EBC_BASE2NAME = eventsSettings.getProperty("CbTeam2Name", "Black");

            EBC_BASE1TITLE = eventsSettings.getProperty("CbTeam1Title", "White");
            EBC_BASE2TITLE = eventsSettings.getProperty("CbTeam2Title", "Black");

            propertySplit = eventsSettings.getProperty("CbPlayersLoc1", "0,0,0").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    EBC_TPLOC1.add(
                            new Location(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: CbPlayersLoc1 error: " + aug[0]);
                    }
                }
            }
            propertySplit = eventsSettings.getProperty("CbPlayersLoc2", "0,0,0").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    EBC_TPLOC2.add(
                            new Location(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: CbPlayersLoc2 error: " + aug[0]);
                    }
                }
            }
            propertySplit = eventsSettings.getProperty("CbReturnCoordinates", "0,0,0").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    EBC_RETURN_COORDINATES.add(
                            new Location(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: CbReturnCoordinates error: " + aug[0]);
                    }
                }
            }

            EBC_TICKETID = Integer.parseInt(eventsSettings.getProperty("CbTicketId", "0"));
            EBC_TICKETCOUNT = Integer.parseInt(eventsSettings.getProperty("CbTicketCount", "0"));

            propertySplit = eventsSettings.getProperty("CbRewards", "9901,500,100;57,1,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    EBC_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("additions.cfg: CbRewards error: " + aug[0]);
                    }
                }
            }
            //
            EENC_ENABLE = Boolean.parseBoolean(eventsSettings.getProperty("EncounterEnable", "False"));
            EENC_ARTIME = Long.parseLong(eventsSettings.getProperty("EncAfterRestart", "30"));
            EENC_REGTIME = Long.parseLong(eventsSettings.getProperty("EncRegPeriod", "30"));
            EENC_ANNDELAY = Long.parseLong(eventsSettings.getProperty("EncAnounceDelay", "30"));
            EENC_TPDELAY = Long.parseLong(eventsSettings.getProperty("EncTeleportDelay", "30"));
            EENC_FINISH = Long.parseLong(eventsSettings.getProperty("EncFinishTask", "1"));
            EENC_NEXT = Long.parseLong(eventsSettings.getProperty("EncNextStart", "30"));

            EENC_MINLVL = Integer.parseInt(eventsSettings.getProperty("EncPlayerMinLvl", "76"));
            EENC_MINP = Integer.parseInt(eventsSettings.getProperty("EncMinPlayers", "76"));
            EENC_MAXP = Integer.parseInt(eventsSettings.getProperty("EncMaxPlayers", "76"));

            EENC_NPCID = Integer.parseInt(eventsSettings.getProperty("EncRegNpcId", "55558"));

            propertySplit = eventsSettings.getProperty("EncRegNpcLoc", "83101,148396,-3407").split(",");
            EENC_NPCLOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));
            EENC_NPCTOWN = eventsSettings.getProperty("EncRegNpcTown", "Гиране");

            propertySplit = eventsSettings.getProperty("EncBattleLoc", "83101,148396,-3407").split(",");
            EENC_TPLOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));

            EENC_TICKETID = Integer.parseInt(eventsSettings.getProperty("EncTicketId", "0"));
            EENC_TICKETCOUNT = Integer.parseInt(eventsSettings.getProperty("EncTicketCount", "0"));

            propertySplit = eventsSettings.getProperty("EncRewards", "9901,500,100;57,1,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    EENC_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("events.cfg: EncRewards error: " + aug[0]);
                    }
                }
            }

            propertySplit = eventsSettings.getProperty("EncItemPoints",
                    "3433#83101,148396,-3407:83201,148296,-3407:83301,148196,-3407;4355#83101,148396,-3407:83201,148296,-3407:83301,148196,-3407")
                    .split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split("#");
                int itemId = Integer.parseInt(aug[0]);
                String[] locs = aug[1].split(":");
                FastList<Location> locfl = new FastList<Location>();
                for (String points : locs) {
                    String[] loc = points.split(",");
                    try {
                        locfl.add(new Location(Integer.parseInt(loc[0]), Integer.parseInt(loc[1]),
                                Integer.parseInt(loc[2])));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("events.cfg: EncItemPoints error: " + aug[0]);
                        }
                    }
                }
                EENC_POINTS.put(itemId, locfl);
            }
            //
            ANARCHY_ENABLE = Boolean.parseBoolean(eventsSettings.getProperty("AnarchyEnable", "False"));
            ANARCHY_DAY = Integer.parseInt(eventsSettings.getProperty("AnarchyDay", "3")) + 1;
            ANARCHY_HOUR = Integer.parseInt(eventsSettings.getProperty("AnarchyHour", "12"));
            ANARCHY_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(eventsSettings.getProperty("AnarchyDuration", "60")));
            String anarchy_towns = eventsSettings.getProperty("AnarchyProtectedTowns", "-1,-2");
            anarchy_towns += ",0";
            propertySplit = anarchy_towns.split(",");
            for (String id : propertySplit) {
                ANARCHY_TOWNS.add(Integer.parseInt(id));
            }
            //
            FIGHTING_ENABLE = Boolean.parseBoolean(eventsSettings.getProperty("FightingEnable", "False"));
            FIGHTING_DAY = Integer.parseInt(eventsSettings.getProperty("FightingDay", "3")) + 1;
            FIGHTING_HOUR = Integer.parseInt(eventsSettings.getProperty("FightingHour", "3"));
            FIGHTING_REGTIME = Integer.parseInt(eventsSettings.getProperty("FightingRegTime", "3"));
            FIGHTING_ANNDELAY = Integer.parseInt(eventsSettings.getProperty("AnarchyAnounceDelay", "3"));
            FIGHTING_TPDELAY = Integer.parseInt(eventsSettings.getProperty("AnarchyBattleDelay", "3"));
            // FIGHTING_FIGHTDELAY =
            // Integer.parseInt(eventsSettings.getProperty("AnarchyDay", "3")) + 1;

            FIGHTING_MINLVL = Integer.parseInt(eventsSettings.getProperty("FightingMinLevel", "70"));
            FIGHTING_MINP = Integer.parseInt(eventsSettings.getProperty("FightingMinPlayers", "10"));
            FIGHTING_MAXP = Integer.parseInt(eventsSettings.getProperty("FightingMaxPlayers", "100"));

            FIGHTING_TICKETID = Integer.parseInt(eventsSettings.getProperty("FightingTicket", "4037"));
            FIGHTING_TICKETCOUNT = Integer.parseInt(eventsSettings.getProperty("FightingTicketCount", "10"));

            FIGHTING_NPCID = Integer.parseInt(eventsSettings.getProperty("FightingNpcId", "4037"));

            propertySplit = eventsSettings.getProperty("FightingNpcLoc", "83101,148396,-3407").split(",");
            FIGHTING_NPCLOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));

            propertySplit = eventsSettings.getProperty("FightingBattleLoc", "83101,148396,-3407").split(",");
            FIGHTING_TPLOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));

            //
            EVENTS_SAME_IP = Boolean.valueOf(eventsSettings.getProperty("AllowEventsSameIp", "True"));
            TVT_SAME_HWID = Boolean.valueOf(eventsSettings.getProperty("TvTSameHWID", "True"));
            FORB_EVENT_REG_TELE = Boolean.valueOf(eventsSettings.getProperty("ForbRegTele", "True"));

            //
            if (TVT_EVENT_ENABLED) {
                TVT_POLY = new EventTerritory(1);
                propertySplit = eventsSettings.getProperty("TvtBorder", "9901,500;57,1").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        TVT_POLY.addPoint(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("events.cfg: TvtBorder error: " + aug[0]);
                        }
                    }
                }
                propertySplit = eventsSettings.getProperty("TvtBorderZ", "-1292,-3407").split(",");
                int z1 = Integer.parseInt(propertySplit[0]);
                int z2 = Integer.parseInt(propertySplit[1]);
                if (z1 == z2) {
                    z1 -= 400;
                    z2 += 400;
                }
                TVT_POLY.setZ(z1, z2);
            }
            if (ELH_ENABLE) {
                LASTHERO_POLY = new EventTerritory(2);
                propertySplit = eventsSettings.getProperty("LastHeroBorder", "9901,500;57,1").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        LASTHERO_POLY.addPoint(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("events.cfg: LastHeroBorder error: " + aug[0]);
                        }
                    }
                }
                propertySplit = eventsSettings.getProperty("LastHeroBorderZ", "-1292,-3407").split(",");
                int z1 = Integer.parseInt(propertySplit[0]);
                int z2 = Integer.parseInt(propertySplit[1]);
                if (z1 == z2) {
                    z1 -= 400;
                    z2 += 400;
                }
                LASTHERO_POLY.setZ(z1, z2);
            }
            if (MASS_PVP) {
                MASSPVP_POLY = new EventTerritory(3);
                propertySplit = eventsSettings.getProperty("MassPvpBorder", "9901,500;57,1").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        MASSPVP_POLY.addPoint(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("events.cfg: MassPvpBorder error: " + aug[0]);
                        }
                    }
                }
                propertySplit = eventsSettings.getProperty("MassPvpBorderZ", "-1292,-3407").split(",");
                int z1 = Integer.parseInt(propertySplit[0]);
                int z2 = Integer.parseInt(propertySplit[1]);
                if (z1 == z2) {
                    z1 -= 400;
                    z2 += 400;
                }
                MASSPVP_POLY.setZ(z1, z2);
            }
            if (EBC_ENABLE) {
                BASECAPTURE_POLY = new EventTerritory(4);
                propertySplit = eventsSettings.getProperty("BaseCaptureBorder", "9901,500;57,1").split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        BASECAPTURE_POLY.addPoint(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("events.cfg: BaseCaptureBorder error: " + aug[0]);
                        }
                    }
                }
                propertySplit = eventsSettings.getProperty("BaseCaptureBorderZ", "-1292,-3407").split(",");
                int z1 = Integer.parseInt(propertySplit[0]);
                int z2 = Integer.parseInt(propertySplit[1]);
                if (z1 == z2) {
                    z1 -= 400;
                    z2 += 400;
                }
                BASECAPTURE_POLY.setZ(z1, z2);
            }
            //
            EVENT_SPECIAL_DROP = Boolean.parseBoolean(eventsSettings.getProperty("SpecialEventDrop", "False"));
            //
            ALLOW_BE = Boolean.parseBoolean(eventsSettings.getProperty("BeggEnable", "False"));
            BE_EGG_ID = Integer.parseInt(eventsSettings.getProperty("BeggId", "0"));
            BE_LIMIT = TimeUnit.MINUTES.toMillis(Integer.parseInt(eventsSettings.getProperty("BeggLimit", "60")));
            BE_RESTART = TimeUnit.MINUTES.toMillis(Integer.parseInt(eventsSettings.getProperty("BeggRestart", "60")));
            BE_NEXTTIME = TimeUnit.MINUTES.toMillis(Integer.parseInt(eventsSettings.getProperty("BeggNext", "60")));
            propertySplit = eventsSettings.getProperty("BeggLoc", "83101,148396,-3407").split(",");
            BE_EGG_LOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));
            propertySplit = eventsSettings.getProperty("BeggRewards", "57,1,1;57,1,1").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    BE_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("events.cfg: BeggRewards error: " + aug[0]);
                    }
                }
            }
            propertySplit = eventsSettings.getProperty("BeggGuards", "1,2").split(";");
            for (String buffs : propertySplit) {
                String[] pbuff = buffs.split(",");
                try {
                    BE_GUARDS.put(Integer.valueOf(pbuff[0]), Integer.valueOf(pbuff[1]));
                } catch (NumberFormatException nfe) {
                    if (!pbuff[0].equals("")) {
                        System.out.println("events.cfg: BeggGuards error: " + pbuff[0]);
                    }
                }
            }
            //
            ALLOW_GUILD_MOD = Boolean.parseBoolean(eventsSettings.getProperty("GuildModEnable", "False"));
            ALLOW_GUILD_AURA = Boolean.parseBoolean(eventsSettings.getProperty("GuildModAura", "False"));
            GUILD_MOD_COIN = Integer.parseInt(eventsSettings.getProperty("GuildModCoin", "0"));
            GUILD_MOD_PRICE = Integer.parseInt(eventsSettings.getProperty("GuildModPrice", "0"));
            GUILD_MOD_COIN_NAME = eventsSettings.getProperty("GuildModCoinName", "GuildModCoinName");
            GUILD_MOD_PENALTYS_MAX = Integer.parseInt(eventsSettings.getProperty("GuildModPenaltyes", "50"));
            GUILD_BALANCE_TEAM = Boolean.parseBoolean(eventsSettings.getProperty("GuildTeamBalance", "False"));

            propertySplit = eventsSettings.getProperty("GuildModNames", "1,[HH];2,[SH]").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    GUILD_MOD_NAMES.put(Integer.parseInt(aug[0]), aug[1]);
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: GuildModNames error: " + aug[0]);
                    }
                }
            }
            propertySplit = eventsSettings.getProperty("GuildModMasks", "1,57;2,57").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    GUILD_MOD_MASKS.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: GuildModMasks error: " + aug[0]);
                    }
                }
            }
            propertySplit = eventsSettings.getProperty("GuildModRewards", "n,n,n;n,n,n").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    GUILD_MOD_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: GuildModRewards error: " + aug[0]);
                    }
                }
            }

            ALLOW_RUNES = Boolean.parseBoolean(eventsSettings.getProperty("RunesEnable", "False"));
            propertySplit = eventsSettings.getProperty("RuneSkills", "n,n,n;n,n,n").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    RUNE_SKILLS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (augs.equals("n,n,n")) {
                        System.out.println("events.cfg: RuneSkills error: " + aug[0]);
                    }
                }
            }

            //
            PVP_ZONE_REWARDS = Boolean.parseBoolean(eventsSettings.getProperty("EnablePvpZones", "False"));
            //
            CUSTOM_CHEST_DROP = Boolean.parseBoolean(eventsSettings.getProperty("ChestDropMpd", "False"));
            //
            CASTLE_BONUS_SKILLS = Boolean.parseBoolean(eventsSettings.getProperty("CastleBonusSkills", "False"));
            //
            EVENT_REG_POPUP = Boolean.parseBoolean(eventsSettings.getProperty("EventRegPopUp", "False"));
            EVENT_REG_POPUPLVL = Integer.parseInt(eventsSettings.getProperty("EventRegPopUpLvl", "76"));
            //
            EVENT_KILL_REWARD = Boolean.parseBoolean(eventsSettings.getProperty("OnKillReward", "False"));
            propertySplit = eventsSettings.getProperty("OnKillRewardItems", "n,n,n;n,n,n").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    EVENT_KILLSITEMS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: OnKillRewards error: " + aug[0]);
                    }
                }
            }
            if (EVENT_KILLSITEMS.isEmpty()) {
                EVENT_KILL_REWARD = false;
            }
            TVT_KILL_REWARD = Boolean.parseBoolean(eventsSettings.getProperty("OnKillTvtReward", "False"));
            propertySplit = eventsSettings.getProperty("OnKillTvtItems", "n,n,n;n,n,n").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    TVT_KILLSITEMS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: OnKillTvtItems error: " + aug[0]);
                    }
                }
            }
            if (TVT_KILLSITEMS.isEmpty()) {
                TVT_KILL_REWARD = false;
            }
            MASSPVP_KILL_REWARD = Boolean.parseBoolean(eventsSettings.getProperty("OnKillMassPvpReward", "False"));
            propertySplit = eventsSettings.getProperty("OnKillMassPvpItems", "n,n,n;n,n,n").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    MASSPVP_KILLSITEMS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: OnKillMassPvpItems error: " + aug[0]);
                    }
                }
            }
            if (MASSPVP_KILLSITEMS.isEmpty()) {
                MASSPVP_KILL_REWARD = false;
            }
            LASTHERO_KILL_REWARD = Boolean.parseBoolean(eventsSettings.getProperty("OnKillLastHeroReward", "False"));
            propertySplit = eventsSettings.getProperty("OnKillLastHeroItems", "n,n,n;n,n,n").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    LASTHERO_KILLSITEMS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: OnKillLastHeroItems error: " + aug[0]);
                    }
                }
            }
            if (LASTHERO_KILLSITEMS.isEmpty()) {
                MASSPVP_KILL_REWARD = false;
            }
            CAPBASE_KILL_REWARD = Boolean.parseBoolean(eventsSettings.getProperty("OnKillBaseReward", "False"));
            propertySplit = eventsSettings.getProperty("OnKillBaseItems", "n,n,n;n,n,n").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    CAPBASE_KILLSITEMS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("events.cfg: OnKillBaseItems error: " + aug[0]);
                    }
                }
            }
            if (CAPBASE_KILLSITEMS.isEmpty()) {
                CAPBASE_KILL_REWARD = false;
            }

            //
            TVT_SHOW_KILLS = Boolean.parseBoolean(eventsSettings.getProperty("TvtShowKills", "False"));
            //
            RESET_EVOLY_ENCH = Boolean.parseBoolean(eventsSettings.getProperty("ResetEnchLikeOly", "False"));
            //
            TVT_CUSTOM_ITEMS = Boolean.parseBoolean(eventsSettings.getProperty("TvTCustomItems", "False"));
            LH_CUSTOM_ITEMS = Boolean.parseBoolean(eventsSettings.getProperty("LhCustomItems", "False"));
            EBC_CUSTOM_ITEMS = Boolean.parseBoolean(eventsSettings.getProperty("EbcCustomItems", "False"));
            //
            propertySplit = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + EVENT_FILE + " File.");
        }
    }

    public static void loadRatesCfg() {
        try {
            Properties ratesSettings = new Properties();
            InputStream is = new FileInputStream(new File(RATES_CONFIG_FILE));
            ratesSettings.load(is);
            is.close();

            RATE_XP = Float.parseFloat(ratesSettings.getProperty("RateXp", "1."));
            RATE_SP = Float.parseFloat(ratesSettings.getProperty("RateSp", "1."));
            RATE_PARTY_XP = Float.parseFloat(ratesSettings.getProperty("RatePartyXp", "1."));
            RATE_PARTY_SP = Float.parseFloat(ratesSettings.getProperty("RatePartySp", "1."));
            RATE_QUESTS_REWARD = Float.parseFloat(ratesSettings.getProperty("RateQuestsReward", "1."));
            RATE_CONSUMABLE_COST = Float.parseFloat(ratesSettings.getProperty("RateConsumableCost", "1."));
            RATE_DROP_ITEMS = Float.parseFloat(ratesSettings.getProperty("RateDropItems", "1."));
            RATE_DROP_ITEMS_BY_RAID = Float.parseFloat(ratesSettings.getProperty("RateRaidDropItems", "1."));
            RATE_DROP_ITEMS_BY_GRANDRAID = Float.parseFloat(ratesSettings.getProperty("RateGrandRaidDropItems", "1."));
            RATE_DROP_SPOIL = Float.parseFloat(ratesSettings.getProperty("RateDropSpoil", "1."));
            RATE_DROP_MANOR = Integer.parseInt(ratesSettings.getProperty("RateDropManor", "1"));
            RATE_DROP_QUEST = Float.parseFloat(ratesSettings.getProperty("RateDropQuest", "1."));
            RATE_KARMA_EXP_LOST = Float.parseFloat(ratesSettings.getProperty("RateKarmaExpLost", "1."));
            RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(ratesSettings.getProperty("RateSiegeGuardsPrice", "1."));
            RATE_DROP_COMMON_HERBS = Float.parseFloat(ratesSettings.getProperty("RateCommonHerbs", "15."));
            RATE_DROP_MP_HP_HERBS = Float.parseFloat(ratesSettings.getProperty("RateHpMpHerbs", "10."));
            RATE_DROP_GREATER_HERBS = Float.parseFloat(ratesSettings.getProperty("RateGreaterHerbs", "4."));
            RATE_DROP_SUPERIOR_HERBS = Float.parseFloat(ratesSettings.getProperty("RateSuperiorHerbs", "0.8")) * 10;
            RATE_DROP_SPECIAL_HERBS = Float.parseFloat(ratesSettings.getProperty("RateSpecialHerbs", "0.2")) * 10;

            RATE_DROP_ADENA = Float.parseFloat(ratesSettings.getProperty("RateDropAdena", "1."));
            RATE_DROP_ADENAMUL = Float.parseFloat(ratesSettings.getProperty("RateDropAdenaMul", "1."));
            RATE_DROP_SEAL_STONE = Float.parseFloat(ratesSettings.getProperty("RateDropSealStone", "1."));
            RATE_MUL_SEAL_STONE = Double.parseDouble(ratesSettings.getProperty("RateDropSealStoneMul", "1."));
            RATE_DROP_ITEMSRAIDMUL = Double.parseDouble(ratesSettings.getProperty("RateDropRaidMul", "1."));
            RATE_DROP_ITEMSGRANDMUL = Double.parseDouble(ratesSettings.getProperty("RateDropGrandMul", "1."));

            PLAYER_DROP_LIMIT = Integer.parseInt(ratesSettings.getProperty("PlayerDropLimit", "3"));
            PLAYER_RATE_DROP = Integer.parseInt(ratesSettings.getProperty("PlayerRateDrop", "5"));
            PLAYER_RATE_DROP_ITEM = Integer.parseInt(ratesSettings.getProperty("PlayerRateDropItem", "70"));
            PLAYER_RATE_DROP_EQUIP = Integer.parseInt(ratesSettings.getProperty("PlayerRateDropEquip", "25"));
            PLAYER_RATE_DROP_EQUIP_WEAPON = Integer
                    .parseInt(ratesSettings.getProperty("PlayerRateDropEquipWeapon", "5"));

            PET_XP_RATE = Float.parseFloat(ratesSettings.getProperty("PetXpRate", "1."));
            PET_FOOD_RATE = Integer.parseInt(ratesSettings.getProperty("PetFoodRate", "1"));
            SINEATER_XP_RATE = Float.parseFloat(ratesSettings.getProperty("SinEaterXpRate", "1."));

            KARMA_DROP_LIMIT = Integer.parseInt(ratesSettings.getProperty("KarmaDropLimit", "10"));
            KARMA_RATE_DROP = Integer.parseInt(ratesSettings.getProperty("KarmaRateDrop", "70"));
            KARMA_RATE_DROP_ITEM = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropItem", "50"));
            KARMA_RATE_DROP_EQUIP = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropEquip", "40"));
            KARMA_RATE_DROP_EQUIP_WEAPON = Integer
                    .parseInt(ratesSettings.getProperty("KarmaRateDropEquipWeapon", "10"));

        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + RATES_CONFIG_FILE + " File.");
        }
    }

    public static void loadAltSettingCfg() {
        ALT_HERO_REWARDS.clear();
        OLY_FIGHTER_BUFFS.clear();
        OLY_MAGE_BUFFS.clear();
        FORBIDDEN_WH_ITEMS.clear();
        PETS_ACTION_SKILLS.clear();
        ANNOUNCE_RAID_SPAWNS_TABLE.clear();
        MOB_KILL_REPUTATION_TABLE.clear();
        ALT_DEBUFF_CHANCES_TABLE.clear();
        ALT_EVENT_BUFFS_MAGE.clear();
        ALT_EVENT_BUFFS_FIGHTER.clear();

        try {
            Properties altSettings = new Properties();
            InputStream is = new FileInputStream(new File(ALT_SETTINGS_FILE));
            altSettings.load(is);
            is.close();

            ALT_GAME_TIREDNESS = Boolean.parseBoolean(altSettings.getProperty("AltGameTiredness", "false"));
            ALT_GAME_CREATION = Boolean.parseBoolean(altSettings.getProperty("AltGameCreation", "false"));
            ALT_GAME_CREATION_SPEED = Double.parseDouble(altSettings.getProperty("AltGameCreationSpeed", "1"));
            ALT_GAME_CREATION_XP_RATE = Double.parseDouble(altSettings.getProperty("AltGameCreationRateXp", "1"));
            ALT_GAME_CREATION_SP_RATE = Double.parseDouble(altSettings.getProperty("AltGameCreationRateSp", "1"));
            ALT_BLACKSMITH_USE_RECIPES = Boolean
                    .parseBoolean(altSettings.getProperty("AltBlacksmithUseRecipes", "true"));
            ALT_GAME_SKILL_LEARN = Boolean.parseBoolean(altSettings.getProperty("AltGameSkillLearn", "false"));
            ALT_GAME_CANCEL_BOW = altSettings.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("bow")
                    || altSettings.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
            ALT_GAME_CANCEL_CAST = altSettings.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("cast")
                    || altSettings.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
            ALT_GAME_SHIELD_BLOCKS = Boolean.parseBoolean(altSettings.getProperty("AltShieldBlocks", "false"));
            ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(altSettings.getProperty("AltPerfectShieldBlockRate", "10"));
            ALT_GAME_DELEVEL = Boolean.parseBoolean(altSettings.getProperty("Delevel", "true"));
            ALT_GAME_MAGICFAILURES = Boolean.parseBoolean(altSettings.getProperty("MagicFailures", "false"));
            ALT_GAME_MOB_ATTACK_AI = Boolean.parseBoolean(altSettings.getProperty("AltGameMobAttackAI", "false"));
            ALT_MOB_AGRO_IN_PEACEZONE = Boolean.parseBoolean(altSettings.getProperty("AltMobAgroInPeaceZone", "true"));
            ALT_GAME_EXPONENT_XP = Float.parseFloat(altSettings.getProperty("AltGameExponentXp", "0."));
            ALT_GAME_EXPONENT_SP = Float.parseFloat(altSettings.getProperty("AltGameExponentSp", "0."));
            ALT_GAME_FREIGHTS = Boolean.parseBoolean(altSettings.getProperty("AltGameFreights", "false"));
            ALT_GAME_FREIGHT_PRICE = Integer.parseInt(altSettings.getProperty("AltGameFreightPrice", "1000"));
            ALT_PARTY_RANGE = Integer.parseInt(altSettings.getProperty("AltPartyRange", "1600"));
            ALT_PARTY_RANGE2 = Integer.parseInt(altSettings.getProperty("AltPartyRange2", "1400"));
            REMOVE_CASTLE_CIRCLETS = Boolean.parseBoolean(altSettings.getProperty("RemoveCastleCirclets", "true"));
            IS_CRAFTING_ENABLED = Boolean.parseBoolean(altSettings.getProperty("CraftingEnabled", "true"));
            LIFE_CRYSTAL_NEEDED = Boolean.parseBoolean(altSettings.getProperty("LifeCrystalNeeded", "true"));
            SP_BOOK_NEEDED = Boolean.parseBoolean(altSettings.getProperty("SpBookNeeded", "true"));
            ES_SP_BOOK_NEEDED = Boolean.parseBoolean(altSettings.getProperty("EnchantSkillSpBookNeeded", "true"));
            AUTO_LOOT_HERBS = altSettings.getProperty("AutoLootHerbs").equalsIgnoreCase("True");
            ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = Boolean
                    .valueOf(altSettings.getProperty("AltKarmaPlayerCanBeKilledInPeaceZone", "false"));
            ALT_GAME_KARMA_PLAYER_CAN_SHOP = Boolean.valueOf(altSettings.getProperty("AltKarmaPlayerCanShop", "true"));
            ALT_GAME_KARMA_PLAYER_CAN_USE_GK = Boolean
                    .valueOf(altSettings.getProperty("AltKarmaPlayerCanUseGK", "false"));
            ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = Boolean
                    .valueOf(altSettings.getProperty("AltKarmaPlayerCanTeleport", "true"));
            ALT_GAME_KARMA_PLAYER_CAN_TRADE = Boolean
                    .valueOf(altSettings.getProperty("AltKarmaPlayerCanTrade", "true"));
            ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = Boolean
                    .valueOf(altSettings.getProperty("AltKarmaPlayerCanUseWareHouse", "true"));
            ALT_GAME_FREE_TELEPORT = Integer.parseInt(altSettings.getProperty("AltFreeTeleporting", "0"));
            ALT_RECOMMEND = Boolean.parseBoolean(altSettings.getProperty("AltRecommend", "False"));
            ALT_PLAYER_PROTECTION_LEVEL = Integer.parseInt(altSettings.getProperty("AltPlayerProtectionLevel", "0"));
            ALT_GAME_VIEWNPC = Boolean.parseBoolean(altSettings.getProperty("AltGameViewNpc", "False"));
            ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE = Boolean
                    .parseBoolean(altSettings.getProperty("AltNewCharAlwaysIsNewbie", "False"));
            ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean
                    .parseBoolean(altSettings.getProperty("AltMembersCanWithdrawFromClanWH", "False"));
            ALT_MAX_NUM_OF_CLANS_IN_ALLY = Integer.parseInt(altSettings.getProperty("AltMaxNumOfClansInAlly", "3"));
            DWARF_RECIPE_LIMIT = Integer.parseInt(altSettings.getProperty("DwarfRecipeLimit", "50"));
            COMMON_RECIPE_LIMIT = Integer.parseInt(altSettings.getProperty("CommonRecipeLimit", "50"));

            ALT_CLAN_MEMBERS_FOR_WAR = Integer.parseInt(altSettings.getProperty("AltClanMembersForWar", "15"));
            ALT_CLAN_JOIN_DAYS = Integer.parseInt(altSettings.getProperty("DaysBeforeJoinAClan", "5"));
            ALT_CLAN_CREATE_DAYS = Integer.parseInt(altSettings.getProperty("DaysBeforeCreateAClan", "10"));
            ALT_CLAN_DISSOLVE_DAYS = Integer.parseInt(altSettings.getProperty("DaysToPassToDissolveAClan", "7"));
            ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = Integer
                    .parseInt(altSettings.getProperty("DaysBeforeJoinAllyWhenLeaved", "1"));
            ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = Integer
                    .parseInt(altSettings.getProperty("DaysBeforeJoinAllyWhenDismissed", "1"));
            ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = Integer
                    .parseInt(altSettings.getProperty("DaysBeforeAcceptNewClanWhenDismissed", "1"));
            ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = Integer
                    .parseInt(altSettings.getProperty("DaysBeforeCreateNewAllyWhenDissolved", "10"));
            ALT_CLAN_REP_MUL = Float.parseFloat(altSettings.getProperty("AltClanReputationRate", "1.0"));
            ALT_CLAN_CREATE_LEVEL = Integer.parseInt(altSettings.getProperty("AltClanCreateLevel", "1"));
            ALT_CLAN_REP_WAR = Integer.parseInt(altSettings.getProperty("AltClanReputationKillBonus", "2"));

            CASTLE_SHIELD = Boolean.parseBoolean(altSettings.getProperty("CastleShieldRestriction", "True"));
            CLANHALL_SHIELD = Boolean.parseBoolean(altSettings.getProperty("ClanHallShieldRestriction", "True"));
            APELLA_ARMORS = Boolean.parseBoolean(altSettings.getProperty("ApellaArmorsRestriction", "True"));
            OATH_ARMORS = Boolean.parseBoolean(altSettings.getProperty("OathArmorsRestriction", "True"));
            CASTLE_CROWN = Boolean.parseBoolean(altSettings.getProperty("CastleLordsCrownRestriction", "True"));
            CASTLE_CIRCLETS = Boolean.parseBoolean(altSettings.getProperty("CastleCircletsRestriction", "True"));

            ALT_OLY_START_TIME = Integer.parseInt(altSettings.getProperty("AltOlyStartTime", "18"));
            ALT_OLY_MIN = Integer.parseInt(altSettings.getProperty("AltOlyMin", "00"));
            ALT_OLY_BATTLE = Long.parseLong(altSettings.getProperty("AltOlyBattle", "360000"));
            ALT_OLY_BWAIT = Long.parseLong(altSettings.getProperty("AltOlyBWait", "600000"));
            ALT_OLY_IWAIT = Long.parseLong(altSettings.getProperty("AltOlyIWait", "300000"));
            ALT_OLY_CPERIOD = Long.parseLong(altSettings.getProperty("AltOlyCPeriod", "21600000"));
            ALT_OLY_WPERIOD = Long.parseLong(altSettings.getProperty("AltOlyWPeriod", "604800000"));
            ALT_OLY_VPERIOD = Long.parseLong(altSettings.getProperty("AltOlyVPeriod", "43200000"));
            ALT_OLY_SAME_IP = Boolean.parseBoolean(altSettings.getProperty("AltOlySameIp", "True"));
            ALT_OLY_SAME_HWID = Boolean.parseBoolean(altSettings.getProperty("AltOlySameHWID", "True"));
            ALT_OLY_ENCHANT_LIMIT = Integer.parseInt(altSettings.getProperty("AltOlyMaxEnchant", "65535"));
            ALT_OLY_MP_REG = Boolean.parseBoolean(altSettings.getProperty("AltOlyRestoreMP", "False"));
            ALT_OLY_MINCLASS = Integer.parseInt(altSettings.getProperty("AltOlyMinClass", "6"));
            ALT_OLY_MINNONCLASS = Integer.parseInt(altSettings.getProperty("AltOlyMinNonClass", "4"));
            ALT_OLYMPIAD_PERIOD = Integer.parseInt(altSettings.getProperty("AltOlyPeriod", "0"));
            ALT_OLY_RELOAD_SKILLS = Boolean.parseBoolean(altSettings.getProperty("AltOlyReloadSkills", "True"));
            OLY_MAX_WEAPON_ENCH = Integer.parseInt(altSettings.getProperty("AltOlyMaxWeaponEnchant", "65535"));
            ALT_START_ITEMS = Boolean.parseBoolean(altSettings.getProperty("altstartitems", "False"));
            OLY_MAX_ARMOT_ENCH = Integer.parseInt(altSettings.getProperty("AltOlyMaxArmorEnchant", "65535"));
            if (OLY_MAX_WEAPON_ENCH == -1) {
                OLY_MAX_WEAPON_ENCH = 65535;
            }
            if (OLY_MAX_ARMOT_ENCH == -1) {
                OLY_MAX_ARMOT_ENCH = 65535;
            }
            RESET_OLY_ENCH = Boolean.parseBoolean(altSettings.getProperty("AltOlyResetEnchant", "False"));
            OLY_ANTI_BUFF = Boolean.parseBoolean(altSettings.getProperty("AltOlyAntiBuff", "True"));

            String[] propertySplit = altSettings.getProperty("AltOlyFighterBuff", "1204,2;1086,1").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    OLY_FIGHTER_BUFFS.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: AltOlyFighterBuff error: " + aug[0]);
                    }
                }
            }
            propertySplit = altSettings.getProperty("AltOlyMageBuff", "1204,2;1085,1").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    OLY_MAGE_BUFFS.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: AltOlyMageBuff error: " + aug[0]);
                    }
                }
            }
            propertySplit = altSettings.getProperty("AltHeroRewards", "").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ALT_HERO_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: AltHeroRewards error: " + aug[0]);
                    }
                }
            }
            OLY_ALT_REWARD = Boolean.parseBoolean(altSettings.getProperty("AltOlyRewardSystem", "false"));
            OLY_ALT_REWARD_TYPE = Integer.parseInt(altSettings.getProperty("AltOlyRewardType", "0"));
            propertySplit = altSettings.getProperty("AltOlyRewardItems", "1,2").split(",");
            OLY_ALT_REWARD_ITEM = new EventReward(Integer.parseInt(propertySplit[0]),
                    Integer.parseInt(propertySplit[1]), 0);
            OLY_ALT_REWARD_ITEM_NAME = altSettings.getProperty("AltOlyRewardName", "Coin Of Luck");

            //
            ALT_MANOR_REFRESH_TIME = Integer.parseInt(altSettings.getProperty("AltManorRefreshTime", "20"));
            ALT_MANOR_REFRESH_MIN = Integer.parseInt(altSettings.getProperty("AltManorRefreshMin", "00"));
            ALT_MANOR_APPROVE_TIME = Integer.parseInt(altSettings.getProperty("AltManorApproveTime", "6"));
            ALT_MANOR_APPROVE_MIN = Integer.parseInt(altSettings.getProperty("AltManorApproveMin", "00"));
            ALT_MANOR_MAINTENANCE_PERIOD = Integer
                    .parseInt(altSettings.getProperty("AltManorMaintenancePeriod", "360000"));
            ALT_MANOR_SAVE_ALL_ACTIONS = Boolean
                    .parseBoolean(altSettings.getProperty("AltManorSaveAllActions", "false"));
            ALT_MANOR_SAVE_PERIOD_RATE = Integer.parseInt(altSettings.getProperty("AltManorSavePeriodRate", "2"));

            ALT_LOTTERY_PRIZE = Integer.parseInt(altSettings.getProperty("AltLotteryPrize", "50000"));
            ALT_LOTTERY_TICKET_PRICE = Integer.parseInt(altSettings.getProperty("AltLotteryTicketPrice", "2000"));
            ALT_LOTTERY_5_NUMBER_RATE = Float.parseFloat(altSettings.getProperty("AltLottery5NumberRate", "0.6"));
            ALT_LOTTERY_4_NUMBER_RATE = Float.parseFloat(altSettings.getProperty("AltLottery4NumberRate", "0.2"));
            ALT_LOTTERY_3_NUMBER_RATE = Float.parseFloat(altSettings.getProperty("AltLottery3NumberRate", "0.2"));
            ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = Integer
                    .parseInt(altSettings.getProperty("AltLottery2and1NumberPrize", "200"));

            DISABLE_GRADE_PENALTY = Boolean.parseBoolean(altSettings.getProperty("DisableGradePenalty", "false"));
            DISABLE_WEIGHT_PENALTY = Boolean.parseBoolean(altSettings.getProperty("DisableWeightPenalty", "false"));
            ALT_DEV_NO_QUESTS = Boolean.parseBoolean(altSettings.getProperty("AltDevNoQuests", "False"));
            ALT_DEV_NO_SPAWNS = Boolean.parseBoolean(altSettings.getProperty("AltDevNoSpawns", "False"));

            // Four Sepulcher
            FS_TIME_ATTACK = Integer.parseInt(altSettings.getProperty("TimeOfAttack", "50"));
            FS_TIME_COOLDOWN = Integer.parseInt(altSettings.getProperty("TimeOfCoolDown", "5"));
            FS_TIME_ENTRY = Integer.parseInt(altSettings.getProperty("TimeOfEntry", "3"));
            FS_TIME_WARMUP = Integer.parseInt(altSettings.getProperty("TimeOfWarmUp", "2"));
            FS_PARTY_MEMBER_COUNT = Integer.parseInt(altSettings.getProperty("NumberOfNecessaryPartyMembers", "4"));
            if (FS_TIME_ATTACK <= 0) {
                FS_TIME_ATTACK = 50;
            }
            if (FS_TIME_COOLDOWN <= 0) {
                FS_TIME_COOLDOWN = 5;
            }
            if (FS_TIME_ENTRY <= 0) {
                FS_TIME_ENTRY = 3;
            }
            if (FS_TIME_ENTRY <= 0) {
                FS_TIME_ENTRY = 3;
            }
            if (FS_TIME_ENTRY <= 0) {
                FS_TIME_ENTRY = 3;
            }
            FS_PARTY_RANGE = Integer.parseInt(altSettings.getProperty("SepulchersPartyRange", "900"));
            FS_CYCLE_MIN = Integer.parseInt(altSettings.getProperty("SepulchersCycleMin", "55"));
            FS_WALL_DOORS = Boolean.parseBoolean(altSettings.getProperty("SepulchersWallDoors", "True"));
            FS_WALL_DOORS = false;

            // Dimensional Rift Config
            RIFT_MIN_PARTY_SIZE = Integer.parseInt(altSettings.getProperty("RiftMinPartySize", "5"));
            RIFT_MAX_JUMPS = Integer.parseInt(altSettings.getProperty("MaxRiftJumps", "4"));
            RIFT_SPAWN_DELAY = Integer.parseInt(altSettings.getProperty("RiftSpawnDelay", "10000"));
            RIFT_AUTO_JUMPS_TIME_MIN = Integer.parseInt(altSettings.getProperty("AutoJumpsDelayMin", "480"));
            RIFT_AUTO_JUMPS_TIME_MAX = Integer.parseInt(altSettings.getProperty("AutoJumpsDelayMax", "600"));
            RIFT_ENTER_COST_RECRUIT = Integer.parseInt(altSettings.getProperty("RecruitCost", "18"));
            RIFT_ENTER_COST_SOLDIER = Integer.parseInt(altSettings.getProperty("SoldierCost", "21"));
            RIFT_ENTER_COST_OFFICER = Integer.parseInt(altSettings.getProperty("OfficerCost", "24"));
            RIFT_ENTER_COST_CAPTAIN = Integer.parseInt(altSettings.getProperty("CaptainCost", "27"));
            RIFT_ENTER_COST_COMMANDER = Integer.parseInt(altSettings.getProperty("CommanderCost", "30"));
            RIFT_ENTER_COST_HERO = Integer.parseInt(altSettings.getProperty("HeroCost", "33"));
            //

            MAX_CHAT_LENGTH = Integer.parseInt(altSettings.getProperty("MaxChatLength", "100"));
            CRUMA_TOWER_LEVEL_RESTRICT = Integer.parseInt(altSettings.getProperty("CrumaTowerLevelRestrict", "56"));
            RIFT_BOSS_ROOM_TIME_MUTIPLY = Float.parseFloat(altSettings.getProperty("BossRoomTimeMultiply", "1.5"));
            NOEPIC_QUESTS = Boolean.parseBoolean(altSettings.getProperty("EpicQuests", "True"));
            ONE_AUGMENT = Boolean.parseBoolean(altSettings.getProperty("OneAugmentEffect", "True"));
            JOB_WINDOW = Boolean.parseBoolean(altSettings.getProperty("JobWindow", "False"));
            USE_SOULSHOTS = Boolean.parseBoolean(altSettings.getProperty("UseSoulShots", "True"));
            USE_ARROWS = Boolean.parseBoolean(altSettings.getProperty("ConsumeArrows", "True"));
            CLEAR_BUFF_ONDEATH = Boolean.parseBoolean(altSettings.getProperty("ClearBuffOnDeath", "True"));
            PROTECT_GATE_PVP = Boolean.parseBoolean(altSettings.getProperty("ProtectGatePvp", "False"));
            MAX_HENNA_BONUS = Integer.parseInt(altSettings.getProperty("MaxHennaBonus", "5"));
            ALT_ANY_SUBCLASS = Boolean.parseBoolean(altSettings.getProperty("AltAnySubClass", "False"));
            ALT_ANY_SUBCLASS_OVERCRAF = Boolean
                    .parseBoolean(altSettings.getProperty("AltAnySubClassOverCraft", "False"));
            SUBS_3RD_PROFF = Boolean.parseBoolean(altSettings.getProperty("Subs3rdProff", "False"));
            ALT_SUBCLASS_PHOENIX = Boolean.parseBoolean(altSettings.getProperty("AltSubClassPhoenix", "False"));
            ALT_AUGMENT_HERO = Boolean.parseBoolean(altSettings.getProperty("AltAugmentHeroWeapons", "False"));
            //
            WEDDING_ANSWER_TIME = Integer.parseInt(altSettings.getProperty("WeddingAnswerTime", "0"));
            RESURECT_ANSWER_TIME = Integer.parseInt(altSettings.getProperty("ResurrectAnswerTime", "0"));
            SUMMON_ANSWER_TIME = Integer.parseInt(altSettings.getProperty("SummonAnswerTime", "30000"));
            //
            MAX_EXP_LEVEL = Integer.parseInt(altSettings.getProperty("MexLevelExp", "100"));
            FREE_PVP = Boolean.parseBoolean(altSettings.getProperty("FreePvp", "False"));
            PROTECT_GRADE_PVP = Boolean.parseBoolean(altSettings.getProperty("ProtectGradePvp", "False"));
            //
            CLEAR_OLY_BAN = Boolean.parseBoolean(altSettings.getProperty("ClearOlympiadPointsBan", "False"));
            //
            GIVE_ITEM_PET = Boolean.parseBoolean(altSettings.getProperty("GiveItemToPet", "True"));
            //
            DISABLE_PET_FEED = Boolean.parseBoolean(altSettings.getProperty("DisablePetFeed", "False"));
            //
            SIEGE_GUARDS_SPAWN = Boolean.parseBoolean(altSettings.getProperty("SpawnSiegeGuards", "True"));
            //
            TELEPORT_PROTECTION = TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(altSettings.getProperty("TeleportProtection", "0")));
            //
            MAX_MATKSPD_DELAY = Integer.parseInt(altSettings.getProperty("MaxMAtkDelay", "655350"));
            MAX_PATKSPD_DELAY = Integer.parseInt(altSettings.getProperty("MaxPAtkDelay", "655350"));
            //
            MAX_MATK_CALC = Integer.parseInt(altSettings.getProperty("MaxMAtkCalc", "655350"));
            MAX_MDEF_CALC = Integer.parseInt(altSettings.getProperty("MaxMDefCalc", "655350"));
            //
            MIN_ATKSPD_DELAY = Integer.parseInt(altSettings.getProperty("MaxAtkSpdDelay", "333"));
            //
            KICK_USED_ACCOUNT_TRYES = Integer.parseInt(altSettings.getProperty("KickUsedAccountTryes", "2"));
            //
            DISABLE_CLAN_REQUREMENTS = Boolean.parseBoolean(altSettings.getProperty("FreeClanLevelUp5", "False"));
            //
            MOUNT_EXPIRE = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(altSettings.getProperty("MountTime", "10")));
            //
            OLY_RELOAD_SKILLS_BEGIN = Boolean.parseBoolean(altSettings.getProperty("OlyReloadSkillsArena", "True"));
            OLY_RELOAD_SKILLS_END = Boolean.parseBoolean(altSettings.getProperty("OlyReloadSkillsBack", "True"));
            //
            RESET_MIRACLE_VALOR = Boolean.parseBoolean(altSettings.getProperty("ResetValorMiracle", "False"));
            //
            STACK_LIFE_STONE = Boolean.parseBoolean(altSettings.getProperty("StackedLifeStones", "False"));
            //
            TRADE_ZONE_ENABLE = Boolean.parseBoolean(altSettings.getProperty("TradeZoneEnabled", "False"));
            //
            SHOW_BOSS_RESPAWNS = Boolean.parseBoolean(altSettings.getProperty("ShowBossRespawns", "False"));
            //
            ALLOW_DEBUFFS_IN_TOWN = Boolean.parseBoolean(altSettings.getProperty("MassDebuffsAtTown", "True"));
            //
            SUMMON_DELAY = TimeUnit.SECONDS.toMillis(Integer.parseInt(altSettings.getProperty("SummonDelay", "10")));
            //
            propertySplit = altSettings.getProperty("ForbiddenWhItems", "1,2").split(",");
            for (String npc_id : propertySplit) {
                FORBIDDEN_WH_ITEMS.add(Integer.parseInt(npc_id));
            }
            //
            propertySplit = altSettings.getProperty("PetsActionSkils", "1204,2;1085,1").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    PETS_ACTION_SKILLS.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: PetsActionSkils error: " + aug[0]);
                    }
                }
            }
            //
            ANNOUNCE_RAID_SPAWNS = Boolean.parseBoolean(altSettings.getProperty("RaidSpawnAnnounce", "False"));
            propertySplit = altSettings.getProperty("RaidSpawnAnnounceTable", "1,1;2,2").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ANNOUNCE_RAID_SPAWNS_TABLE.put(Integer.parseInt(aug[0]),
                            TimeUnit.MINUTES.toMillis(Integer.parseInt(aug[1])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: RaidSpawnAnnounceTable error: " + aug[0]);
                    }
                }
            }

            // кол-во чаров для апа клана
            CLAN_CHAR_LEVEL6 = Integer.parseInt(altSettings.getProperty("clancharlevel6", "30"));
            CLAN_CHAR_LEVEL7 = Integer.parseInt(altSettings.getProperty("clancharlevel7", "80"));
            CLAN_CHAR_LEVEL8 = Integer.parseInt(altSettings.getProperty("clancharlevel8", "120"));

            // кол-во htgs для апа клана
            CLAN_REP_LEVEL6 = Integer.parseInt(altSettings.getProperty("clanreplevel6", "10000"));
            CLAN_REP_LEVEL7 = Integer.parseInt(altSettings.getProperty("clanreplevel7", "20000"));
            CLAN_REP_LEVEL8 = Integer.parseInt(altSettings.getProperty("clanreplevel8", "40000"));

            // твт баф
            TVT_BUFF = Boolean.parseBoolean(altSettings.getProperty("tvtbuff", "False"));
            // старт баф
            ON_BUFFS = Boolean.parseBoolean(altSettings.getProperty("StartBuff", "False"));
            ON_BUFFS_LVL = Integer.parseInt(altSettings.getProperty("StartBufflvl", "40"));

            propertySplit = altSettings.getProperty("StartBuffMage", "1,1;2,2").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ON_ENTER_M_BUFFS.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: StartBuffMagic error: " + aug[0]);
                    }
                }
            }
            propertySplit = altSettings.getProperty("StartBuffFighter", "1,1;2,2").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ON_ENTER_F_BUFFS.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: StartBuffFighter error: " + aug[0]);
                    }
                }
            }

            //
            MOB_KILL_REPUTATION = Boolean.parseBoolean(altSettings.getProperty("MobKillReputation", "False"));
            propertySplit = altSettings.getProperty("MobKillReputationTable", "1,1;2,2").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    MOB_KILL_REPUTATION_TABLE.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: MobKillReputationTable error: " + aug[0]);
                    }
                }
            }
            //
            ALT_DEBUFF_CHANCES = Boolean.parseBoolean(altSettings.getProperty("AltDebuffChances", "False"));
            propertySplit = altSettings.getProperty("AltDebuffChancesTable", "1,1;2,2").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ALT_DEBUFF_CHANCES_TABLE.put(Integer.parseInt(aug[0]), Double.parseDouble(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: AltDebuffChancesTable error: " + aug[0]);
                    }
                }
            }
            //
            ALT_EVENT_BUFFS = Boolean.parseBoolean(altSettings.getProperty("AltEventBuffs", "False"));
            propertySplit = altSettings.getProperty("AltEventBuffsMage", "1,1;2,2").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ALT_EVENT_BUFFS_MAGE.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: AltEventBuffsMage error: " + aug[0]);
                    }
                }
            }
            propertySplit = altSettings.getProperty("AltEventBuffsFighter", "1,1;2,2").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    ALT_EVENT_BUFFS_FIGHTER.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("altsettings.cfg: AltEventBuffsFighter error: " + aug[0]);
                    }
                }
            }
            //
            ALT_HENNA_SUB = Boolean.parseBoolean(altSettings.getProperty("AltHennaSub", "False"));
            ALT_HENNA_OLY_EVENT_DISABLE = Boolean
                    .parseBoolean(altSettings.getProperty("AltHennaOlyEventDisable", "False"));
            //
            propertySplit = altSettings.getProperty("PetRentPrices", "1800,7200,720000,6480000").split(",");
            for (String npc_id : propertySplit) {
                PET_RENT_PRICES.add(Integer.parseInt(npc_id));
            }
            PET_RENT_COIN = Integer.parseInt(altSettings.getProperty("PetRentCoin", "57"));
            PET_RENT_COIN_NAME = altSettings.getProperty("PetRentCoinName", "Adena");
            //
            ALLP_ZONE = Boolean.parseBoolean(altSettings.getProperty("AllpZone", "False"));
            DISABLE_REFLECT_ON_MOBS = Boolean.parseBoolean(altSettings.getProperty("DisableReflectOnMobs", "False"));
            LVLUP_RELOAD_SKILLS = Boolean.parseBoolean(altSettings.getProperty("LevelUpReloadSkills", "True"));
            CHAT_LEVEL = Integer.parseInt(altSettings.getProperty("ChatLevel", "0"));
            CHANGE_SUB_DELAY = Integer.parseInt(altSettings.getProperty("ChangeSubDelay", "2100"));
            RELOAD_SUB_SKILL = Boolean.parseBoolean(altSettings.getProperty("ReloadSubSkills", "True"));
            CHECK_PVP_ZONES = Boolean.parseBoolean(altSettings.getProperty("CheckPvpZones", "True"));
            PVP_DELAY = Boolean.parseBoolean(altSettings.getProperty("CheckPvpDelay", "False"));
            CHECK_PVP_DELAY = TimeUnit.SECONDS.toMillis(Integer.parseInt(altSettings.getProperty("PvpDelay", "10")));
            AUTO_SKILL_ATTACK = Boolean.parseBoolean(altSettings.getProperty("AutoSkillAttack", "True"));
            propertySplit = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + ALT_SETTINGS_FILE + " File.");
        }
    }

    public static void loadSevenSignsCfg() {
        try {
            Properties SevenSettings = new Properties();
            InputStream is = new FileInputStream(new File(SEVENSIGNS_FILE));
            SevenSettings.load(is);
            is.close();

            ALT_GAME_REQUIRE_CASTLE_DAWN = Boolean
                    .parseBoolean(SevenSettings.getProperty("AltRequireCastleForDawn", "False"));
            ALT_GAME_REQUIRE_CLAN_CASTLE = Boolean
                    .parseBoolean(SevenSettings.getProperty("AltRequireClanCastle", "False"));
            ALT_FESTIVAL_MIN_PLAYER = Integer.parseInt(SevenSettings.getProperty("AltFestivalMinPlayer", "5"));
            ALT_MAXIMUM_PLAYER_CONTRIB = Integer.parseInt(SevenSettings.getProperty("AltMaxPlayerContrib", "1000000"));
            ALT_FESTIVAL_MANAGER_START = Long.parseLong(SevenSettings.getProperty("AltFestivalManagerStart", "120000"));
            ALT_FESTIVAL_LENGTH = Long.parseLong(SevenSettings.getProperty("AltFestivalLength", "1080000"));
            ALT_FESTIVAL_CYCLE_LENGTH = Long.parseLong(SevenSettings.getProperty("AltFestivalCycleLength", "2280000"));
            ALT_FESTIVAL_FIRST_SPAWN = Long.parseLong(SevenSettings.getProperty("AltFestivalFirstSpawn", "120000"));
            ALT_FESTIVAL_FIRST_SWARM = Long.parseLong(SevenSettings.getProperty("AltFestivalFirstSwarm", "300000"));
            ALT_FESTIVAL_SECOND_SPAWN = Long.parseLong(SevenSettings.getProperty("AltFestivalSecondSpawn", "540000"));
            ALT_FESTIVAL_SECOND_SWARM = Long.parseLong(SevenSettings.getProperty("AltFestivalSecondSwarm", "720000"));
            ALT_FESTIVAL_CHEST_SPAWN = Long.parseLong(SevenSettings.getProperty("AltFestivalChestSpawn", "900000"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + SEVENSIGNS_FILE + " File.");
        }
    }

    public static void loadClanHallCfg() {
        CH_AUC_TOWNS_LIST.clear();
        try {
            Properties clanhallSettings = new Properties();
            InputStream is = new FileInputStream(new File(CLANHALL_CONFIG_FILE));
            clanhallSettings.load(is);
            is.close();
            CH_TELE_FEE_RATIO = Long
                    .valueOf(clanhallSettings.getProperty("ClanHallTeleportFunctionFeeRation", "86400000"));
            CH_TELE1_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallTeleportFunctionFeeLvl1", "86400000"));
            CH_TELE2_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallTeleportFunctionFeeLvl2", "86400000"));
            CH_SUPPORT_FEE_RATIO = Long
                    .valueOf(clanhallSettings.getProperty("ClanHallSupportFunctionFeeRation", "86400000"));
            CH_SUPPORT1_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallSupportFeeLvl1", "86400000"));
            CH_SUPPORT2_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallSupportFeeLvl2", "86400000"));
            CH_SUPPORT3_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallSupportFeeLvl3", "86400000"));
            CH_SUPPORT4_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallSupportFeeLvl4", "86400000"));
            CH_SUPPORT5_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallSupportFeeLvl5", "86400000"));
            CH_SUPPORT6_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallSupportFeeLvl6", "86400000"));
            CH_SUPPORT7_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallSupportFeeLvl7", "86400000"));
            CH_SUPPORT8_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallSupportFeeLvl8", "86400000"));
            CH_MPREG_FEE_RATIO = Long
                    .valueOf(clanhallSettings.getProperty("ClanHallMpRegenerationFunctionFeeRation", "86400000"));
            CH_MPREG1_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallMpRegenerationFeeLvl1", "86400000"));
            CH_MPREG2_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallMpRegenerationFeeLvl2", "86400000"));
            CH_MPREG3_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallMpRegenerationFeeLvl3", "86400000"));
            CH_MPREG4_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallMpRegenerationFeeLvl4", "86400000"));
            CH_MPREG5_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallMpRegenerationFeeLvl5", "86400000"));
            CH_HPREG_FEE_RATIO = Long
                    .valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFunctionFeeRation", "86400000"));
            CH_HPREG1_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl1", "86400000"));
            CH_HPREG2_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl2", "86400000"));
            CH_HPREG3_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl3", "86400000"));
            CH_HPREG4_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl4", "86400000"));
            CH_HPREG5_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl5", "86400000"));
            CH_HPREG6_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl6", "86400000"));
            CH_HPREG7_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl7", "86400000"));
            CH_HPREG8_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl8", "86400000"));
            CH_HPREG9_FEE = Integer.valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl9", "86400000"));
            CH_HPREG10_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl10", "86400000"));
            CH_HPREG11_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl11", "86400000"));
            CH_HPREG12_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl12", "86400000"));
            CH_HPREG13_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallHpRegenerationFeeLvl13", "86400000"));
            CH_EXPREG_FEE_RATIO = Long
                    .valueOf(clanhallSettings.getProperty("ClanHallExpRegenerationFunctionFeeRation", "86400000"));
            CH_EXPREG1_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallExpRegenerationFeeLvl1", "86400000"));
            CH_EXPREG2_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallExpRegenerationFeeLvl2", "86400000"));
            CH_EXPREG3_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallExpRegenerationFeeLvl3", "86400000"));
            CH_EXPREG4_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallExpRegenerationFeeLvl4", "86400000"));
            CH_EXPREG5_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallExpRegenerationFeeLvl5", "86400000"));
            CH_EXPREG6_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallExpRegenerationFeeLvl6", "86400000"));
            CH_EXPREG7_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallExpRegenerationFeeLvl7", "86400000"));
            CH_ITEM_FEE_RATIO = Long
                    .valueOf(clanhallSettings.getProperty("ClanHallItemCreationFunctionFeeRation", "86400000"));
            CH_ITEM1_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallItemCreationFunctionFeeLvl1", "86400000"));
            CH_ITEM2_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallItemCreationFunctionFeeLvl2", "86400000"));
            CH_ITEM3_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallItemCreationFunctionFeeLvl3", "86400000"));
            CH_CURTAIN_FEE_RATIO = Long
                    .valueOf(clanhallSettings.getProperty("ClanHallCurtainFunctionFeeRation", "86400000"));
            CH_CURTAIN1_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallCurtainFunctionFeeLvl1", "86400000"));
            CH_CURTAIN2_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallCurtainFunctionFeeLvl2", "86400000"));
            CH_FRONT_FEE_RATIO = Long
                    .valueOf(clanhallSettings.getProperty("ClanHallFrontPlatformFunctionFeeRation", "86400000"));
            CH_FRONT1_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", "86400000"));
            CH_FRONT2_FEE = Integer
                    .valueOf(clanhallSettings.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", "86400000"));

            CLANHALL_PAYMENT = Integer.valueOf(clanhallSettings.getProperty("ClanHallPaymentId", "57"));
            CLANHALL_FEE_ID = Integer.valueOf(clanhallSettings.getProperty("ClanHallFeeId", "57"));

            String aucTowns = clanhallSettings.getProperty("ClanHallAuctionTowns", "Off");
            if (!aucTowns.equalsIgnoreCase("Off")) {
                String[] propertySplit = aucTowns.split(",");
                if (propertySplit.length > 0) {
                    for (String npc_id : propertySplit) {
                        CH_AUC_TOWNS_LIST.add(npc_id);
                    }
                }
            }
            CH_AUCTION_TOWNS = !CH_AUC_TOWNS_LIST.isEmpty();
            CH_FEE_CASTLE = Boolean.parseBoolean(clanhallSettings.getProperty("ChFeeCastleOwner", "False"));

        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + CLANHALL_CONFIG_FILE + " File.");
        }
    }

    public static void loadNpcCfg() {
        NPC_RAID_REWARDS.clear();
        NPC_EPIC_REWARDS.clear();
        NPC_HIT_PROTECTET.clear();
        BOSS_ITEMS.clear();
        HEALING_SUMMONS.clear();
        NPC_CHASE_RANGES.clear();
        MOUNTABLE_PETS.clear();
        FORBID_NPC_HELLO.clear();
        MOB_PVP_FLAG_LIST.clear();
        PREMIUM_MOBS_LIST.clear();
        SUPER_MOBS.clear();
        MOB_FIXED_DAMAGE_LIST.clear();
        try {
            Properties npc_conf = new Properties();
            InputStream is = new FileInputStream(new File(NPC_CONFIG_FILE));
            npc_conf.load(is);
            is.close();

            L2JMOD_CHAMPION_ENABLE = Boolean.parseBoolean(npc_conf.getProperty("ChampionEnable", "false"));
            L2JMOD_CHAMPION_AURA = Boolean.parseBoolean(npc_conf.getProperty("ChampionAura", "false"));
            L2JMOD_CHAMPION_FREQUENCY = Integer.parseInt(npc_conf.getProperty("ChampionFrequency", "0"));
            L2JMOD_CHAMP_MIN_LVL = Integer.parseInt(npc_conf.getProperty("ChampionMinLevel", "20"));
            L2JMOD_CHAMP_MAX_LVL = Integer.parseInt(npc_conf.getProperty("ChampionMaxLevel", "60"));
            L2JMOD_CHAMPION_HP = Integer.parseInt(npc_conf.getProperty("ChampionHp", "7"));
            L2JMOD_CHAMPION_HP_REGEN = Float.parseFloat(npc_conf.getProperty("ChampionHpRegen", "1."));
            L2JMOD_CHAMPION_REWARDS_CHANCE = Double.parseDouble(npc_conf.getProperty("ChampionRewardsChance", "2"));
            L2JMOD_CHAMPION_REWARDS_COUNT = Double.parseDouble(npc_conf.getProperty("ChampionRewardsCount", "2"));
            L2JMOD_CHAMPION_REWARDS_EXP = Double.parseDouble(npc_conf.getProperty("ChampionRewardsExp", "2"));
            L2JMOD_CHAMPION_REWARDS_SP = Double.parseDouble(npc_conf.getProperty("ChampionRewardsSp", "2"));
            L2JMOD_CHAMPION_ADENAS_REWARDS = Double.parseDouble(npc_conf.getProperty("ChampionAdenasRewards", "1"));
            L2JMOD_CHAMPION_ATK = Float.parseFloat(npc_conf.getProperty("ChampionAtk", "1."));
            L2JMOD_CHAMPION_SPD_ATK = Float.parseFloat(npc_conf.getProperty("ChampionSpdAtk", "1."));
            L2JMOD_CHAMPION_REWARD = Boolean.parseBoolean(npc_conf.getProperty("ChampionRewardBonus", "False"));

            L2JMOD_CHAMPION_ENABLE_BLUE = Boolean.parseBoolean(npc_conf.getProperty("ChampionEnableBlue", "false"));
            L2JMOD_CHAMPION_AURA_BLUE = Boolean.parseBoolean(npc_conf.getProperty("ChampionAuraBlue", "false"));
            L2JMOD_CHAMPION_FREQUENCY_BLUE = Integer.parseInt(npc_conf.getProperty("ChampionFrequencyBlue", "0"));
            L2JMOD_CHAMP_MIN_LVL_BLUE = Integer.parseInt(npc_conf.getProperty("ChampionMinLevelBlue", "20"));
            L2JMOD_CHAMP_MAX_LVL_BLUE = Integer.parseInt(npc_conf.getProperty("ChampionMaxLevelBlue", "60"));
            L2JMOD_CHAMPION_HP_BLUE = Integer.parseInt(npc_conf.getProperty("ChampionHpBlue", "7"));
            L2JMOD_CHAMPION_HP_REGEN_BLUE = Float.parseFloat(npc_conf.getProperty("ChampionHpRegenBlue", "1."));
            L2JMOD_CHAMPION_REWARDS_CHANCE_BLUE = Double
                    .parseDouble(npc_conf.getProperty("ChampionRewardsChanceBlue", "2"));
            L2JMOD_CHAMPION_REWARDS_COUNT_BLUE = Double
                    .parseDouble(npc_conf.getProperty("ChampionRewardsCountBlue", "2"));
            L2JMOD_CHAMPION_REWARDS_EXP_BLUE = Double.parseDouble(npc_conf.getProperty("ChampionRewardsExpBlue", "2"));
            L2JMOD_CHAMPION_REWARDS_SP_BLUE = Double.parseDouble(npc_conf.getProperty("ChampionRewardsSpBlue", "2"));
            L2JMOD_CHAMPION_ADENAS_REWARDS_BLUE = Double
                    .parseDouble(npc_conf.getProperty("ChampionAdenasRewardsBlue", "1"));
            L2JMOD_CHAMPION_ATK_BLUE = Float.parseFloat(npc_conf.getProperty("ChampionAtkBlue", "1."));
            L2JMOD_CHAMPION_SPD_ATK_BLUE = Float.parseFloat(npc_conf.getProperty("ChampionSpdAtkBlue", "1."));
            L2JMOD_CHAMPION_REWARD_BLUE = Boolean
                    .parseBoolean(npc_conf.getProperty("ChampionRewardBonusBlue", "False"));

            RAID_CUSTOM_DROP = Boolean.parseBoolean(npc_conf.getProperty("AllowCustomRaidDrop", "False"));
            String[] propertySplit = npc_conf.getProperty("CustomRaidDrop", "57,1,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    NPC_RAID_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("npc.cfg: CustomRaidDrop error: " + aug[0]);
                    }
                }
            }

            MOB_FIXED_DAMAGE = Boolean.parseBoolean(npc_conf.getProperty("MobFixedDamage", "False"));
            propertySplit = npc_conf.getProperty("MobFixedDamageList", "1204,2;1085,1").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    MOB_FIXED_DAMAGE_LIST.put(Integer.parseInt(aug[0]), Double.parseDouble(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("npc.cfg: MobFixedDamage error: " + aug[0]);
                    }
                }
            }

            propertySplit = npc_conf.getProperty("CustomEpicShadowDrop", "57,1,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    NPC_EPIC_REWARDS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("npc.cfg: CustomEpicShadowDrop error: " + aug[0]);
                    }
                }
            }

            propertySplit = npc_conf.getProperty("ChampionRewardList", "57,1,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    L2JMOD_CHAMPION_REWARD_LIST.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("npc.cfg: ChampionRewardList error: " + aug[0]);
                    }
                }
            }

            propertySplit = npc_conf.getProperty("ChampionRewardListBlue", "57,1,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    L2JMOD_CHAMPION_REWARD_LIST_BLUE.add(new EventReward(Integer.parseInt(aug[0]),
                            Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("npc.cfg: ChampionRewardListBlue error: " + aug[0]);
                    }
                }
            }

            RAID_CLANPOINTS_REWARD = Integer.parseInt(npc_conf.getProperty("BossClanPointsReward", "0"));
            EPIC_CLANPOINTS_REWARD = Integer.parseInt(npc_conf.getProperty("EpicClanPointsReward", "0"));

            ANTARAS_CLOSE_PORT = TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(npc_conf.getProperty("AntharasClosePort", "30")));
            ANTARAS_UPDATE_LAIR = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("AntharasUpdateLair", "10")));
            ANTARAS_MIN_RESPAWN = convertTime(npc_conf.getProperty("AntharasMinRespawn", "160"));
            ANTARAS_MAX_RESPAWN = convertTime(npc_conf.getProperty("AntharasMaxRespawn", "180"));
            ANTARAS_RESTART_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("AntharasRestartDelay", "5")));
            ANTARAS_SPAWN_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("AntharasSpawnDelay", "10")));
            // valik
            VALAKAS_CLOSE_PORT = TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(npc_conf.getProperty("ValakasClosePort", "30")));
            VALAKAS_UPDATE_LAIR = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("ValakasUpdateLair", "10")));
            VALAKAS_MIN_RESPAWN = convertTime(npc_conf.getProperty("ValakasMinRespawn", "160"));
            VALAKAS_MAX_RESPAWN = convertTime(npc_conf.getProperty("ValakasMaxRespawn", "180"));
            VALAKAS_RESTART_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("ValakasRestartDelay", "5")));
            VALAKAS_SPAWN_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("ValakasSpawnDelay", "10")));
            // baium
            BAIUM_CLOSE_PORT = TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(npc_conf.getProperty("BaiumClosePort", "30")));
            BAIUM_UPDATE_LAIR = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("BaiumUpdateLair", "2")));
            BAIUM_MIN_RESPAWN = convertTime(npc_conf.getProperty("BaiumMinRespawn", "110"));
            BAIUM_MAX_RESPAWN = convertTime(npc_conf.getProperty("BaiumMaxRespawn", "130"));
            BAIUM_RESTART_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("BaiumRestartDelay", "5")));
            // ant queen
            AQ_MIN_RESPAWN = convertTime(npc_conf.getProperty("AqMinRespawn", "22"));
            AQ_MAX_RESPAWN = convertTime(npc_conf.getProperty("AqMaxRespawn", "26"));
            AQ_RESTART_DELAY = TimeUnit.MINUTES.toMillis(Integer.parseInt(npc_conf.getProperty("AqRestartDelay", "5")));
            AQ_PLAYER_MAX_LVL = Integer.parseInt(npc_conf.getProperty("AqMaxPlayerLvl", "80"));
            AQ_NURSE_RESPAWN = TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(npc_conf.getProperty("AqNurseRespawn", "15")));
            // zaken
            ZAKEN_MIN_RESPAWN = convertTime(npc_conf.getProperty("ZakenMinRespawn", "22"));
            ZAKEN_MAX_RESPAWN = convertTime(npc_conf.getProperty("ZakenMaxRespawn", "26"));
            ZAKEN_RESTART_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("ZakenRestartDelay", "5")));
            ZAKEN_PLAYER_MAX_LVL = Integer.parseInt(npc_conf.getProperty("ZakenMaxPlayerLvl", "80"));
            // frinta
            FRINTA_MMIN_PARTIES = Integer.parseInt(npc_conf.getProperty("FrintMinPartys", "2"));
            FRINTA_MMIN_PLAYERS = Integer.parseInt(npc_conf.getProperty("FrintMinPartyPlayers", "9"));
            FRINTA_UPDATE_LAIR = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("FrintUpdateLair", "30")));
            FRINTA_MIN_RESPAWN = convertTime(npc_conf.getProperty("FrintMinRespawn", "48"));
            FRINTA_MAX_RESPAWN = convertTime(npc_conf.getProperty("FrintMinRespawn", "52"));
            FRINTA_RESTART_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("FrintRestartDelay", "5")));
            FRINTA_SPAWN_DELAY = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(npc_conf.getProperty("FrintSpawnDelay", "5")));

            //
            ALLOW_HIT_NPC = Boolean.parseBoolean(npc_conf.getProperty("AllowHitNpc", "True"));
            KILL_NPC_ATTACKER = Boolean.parseBoolean(npc_conf.getProperty("KillNpcAttacker", "False"));
            //
            NPC_SPAWN_DELAY = Long.parseLong(npc_conf.getProperty("NpcSpawnDelay", "0"));
            NPC_SPAWN_TYPE = Integer.parseInt(npc_conf.getProperty("NpcSpawnType", "3"));
            //
            ANNOUNCE_EPIC_STATES = Boolean.parseBoolean(npc_conf.getProperty("AnnounceEpicStates", "False"));
            //
            propertySplit = npc_conf.getProperty("ProtectedHitNpc", "57,1").split(",");
            for (String npc_id : propertySplit) {
                NPC_HIT_PROTECTET.add(Integer.parseInt(npc_id));
            }
            propertySplit = npc_conf.getProperty("HitNpcLocation", "0,0,0").split(",");
            NPC_HIT_LOCATION = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));
            //
            MOB_PVP_FLAG = Boolean.parseBoolean(npc_conf.getProperty("MobPvpFlag", "False"));
            MOB_PVP_FLAG_CONUT = Integer.parseInt(npc_conf.getProperty("MobPvpFlagCount", "0"));
            if (MOB_PVP_FLAG) {
                propertySplit = npc_conf.getProperty("MobPvpFlagList", "57,1").split(",");
                for (String npc_id : propertySplit) {
                    MOB_PVP_FLAG_LIST.add(Integer.parseInt(npc_id));
                }
            }

            RB_CLANREP = Boolean.parseBoolean(npc_conf.getProperty("rbcrp", "False"));
            RB_CLANREP_CONUT = Integer.parseInt(npc_conf.getProperty("crpcount", "0"));
            if (RB_CLANREP) {
                RB_CLANREP_ID = Integer.parseInt(npc_conf.getProperty("crprbid", "0"));

            }

            //
            PREMIUM_MOBS = Boolean.parseBoolean(npc_conf.getProperty("PremiumMobs", "False"));
            if (PREMIUM_MOBS) {
                propertySplit = npc_conf.getProperty("PremiumMobsList", "57,1").split(",");
                for (String npc_id : propertySplit) {
                    PREMIUM_MOBS_LIST.add(Integer.parseInt(npc_id));
                }
            }
            //
            BARAKIEL_NOBLESS = Boolean.parseBoolean(npc_conf.getProperty("BarakielDeathNobless", "False"));
            //
            ALLOW_RAID_BOSS_PUT = Boolean.parseBoolean(npc_conf.getProperty("AllowRaidBossPetrified", "True"));
            //
            FORBID_RAID_BOSS_HEAL = Boolean.parseBoolean(npc_conf.getProperty("AllowRaidBossHeal", "True"));
            //
            propertySplit = npc_conf.getProperty("ZakenSpawnLoc", "55256,219114,-3224").split(",");
            ZAKEN_SPAWN_LOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]),
                    Integer.parseInt(propertySplit[2]));
            //
            PROTECT_MOBS_ITEMS = Boolean.parseBoolean(npc_conf.getProperty("PlayerPenaltyItems", "False"));
            //
            BOSS_ZONE_MAX_ENCH = Integer.parseInt(npc_conf.getProperty("MaxEnchBossZone", "0"));
            //
            propertySplit = npc_conf.getProperty("ForbiddenItemsBossZone", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    BOSS_ITEMS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("npc.cfg: ForbiddenItemsBossZone error: " + itemid);
                    }
                }
            }
            //
            NPC_CHECK_RANGE = Boolean.parseBoolean(npc_conf.getProperty("CheckSpawnRange", "True"));
            NPC_CHECK_MAX_RANGE = Integer.parseInt(npc_conf.getProperty("SpawnRangeMax", "2500"));
            //
            propertySplit = npc_conf.getProperty("MaxChaseRanges", "1,2").split(";");
            for (String buffs : propertySplit) {
                String[] pbuff = buffs.split(",");
                try {
                    NPC_CHASE_RANGES.put(Integer.valueOf(pbuff[0]), Integer.valueOf(pbuff[1]));
                } catch (NumberFormatException nfe) {
                    if (!pbuff[0].equals("")) {
                        System.out.println("npc.cfg: MaxChaseRanges error: " + pbuff[0]);
                    }
                }
            }
            //
            propertySplit = npc_conf.getProperty("MountablePets", "12526,12527,12528,12621").split(",");
            for (String itemid : propertySplit) {
                try {
                    MOUNTABLE_PETS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("npc.cfg: MountablePets error: " + itemid);
                    }
                }
            }
            //
            propertySplit = npc_conf.getProperty("EpicJewerlyChances",
                    "6660,100;6657,100;6658,100;6656,100;6662,100;6661,100;6659,100;8191,100").split(";");
            for (String augs : propertySplit) {
                String[] aug = augs.split(",");
                try {
                    EPIC_JEWERLY_CHANCES.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (aug.length > 0) {
                        System.out.println("npc.cfg: EpicJewerlyChances error: " + aug[0]);
                    }
                }
            }
            //
            BOSS_ZONE_LOGOUT = Boolean.parseBoolean(npc_conf.getProperty("BossZoneLogout", "True"));
            //
            propertySplit = npc_conf.getProperty("ForbidNpcHello", "30675,30761,30762,30763,31074,31665,31752")
                    .split(",");
            for (String itemid : propertySplit) {
                try {
                    FORBID_NPC_HELLO.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("npc.cfg: ForbidNpcHello error: " + itemid);
                    }
                }
            }
            //
            RAID_VORTEX_CHANCE = Integer.parseInt(npc_conf.getProperty("RaidVortexChance", "80"));
            //
            FIXED_DAMAGE_TO_BOSS = Integer.parseInt(npc_conf.getProperty("FixedDamageToBoss", "0"));
            //
            TALK_MOVE_DELAY = Boolean.parseBoolean(npc_conf.getProperty("NpcTalkMoveDelay", "True"));
            //
            propertySplit = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + NPC_CONFIG_FILE + " File.");
        }
    }

    private static long convertTime(String time) {
        if (time.endsWith("m")) {
            return TimeUnit.MINUTES.toMillis(Integer.parseInt(time.replaceAll("m", "")));
        }

        if (time.endsWith("s")) {
            return TimeUnit.SECONDS.toMillis(Integer.parseInt(time.replaceAll("s", "")));
        }

        if (time.endsWith("d")) {
            return TimeUnit.DAYS.toMillis(Integer.parseInt(time.replaceAll("d", "")));
        }

        return TimeUnit.HOURS.toMillis(Integer.parseInt(time));
    }

    public static void loadCustomCfg() {
        CHAT_FILTER_STRINGS.clear();
        F_OLY_ITEMS.clear();
        MULTVIP_CARDS.clear();
        CASTLE_SIEGE_REWARDS.clear();
        CASTLE_RPOTECT_REWARDS.clear();
        ALT_BUFF_TIME.clear();
        ALT_SKILL_CHANSE.clear();
        ALT_MAGIC_WEAPONS.clear();
        CUSTOM_STRT_ITEMS.clear();
        ALT_FIXED_REUSES.clear();
        ALT_FIXED_CASTS.clear();
        PROTECTED_BUFFS.clear();
        CASTLE_SIEGE_SKILLS.clear();
        HIPPY_ITEMS.clear();
        FORB_CURSED_SKILLS.clear();
        CUSTOM_AUG_SKILLS.clear();
        FORB_OLY_SKILLS.clear();
        FORB_EVENT_SKILLS.clear();
        WHITE_SKILLS.clear();
        FORBIDDEN_BOW_CLASSES.clear();
        FORBIDDEN_BOW_CLASSES_OLY.clear();
        FORBIDDEN_DUAL_CLASSES.clear();
        FORBIDDEN_FIST_CLASSES.clear();
        try {
            Properties customSettings = new Properties();
            InputStream is = new FileInputStream(new File(CUSTOM_CONFIG_FILE));
            customSettings.load(is);
            is.close();

            ALLOW_HERO_SUBSKILL = Boolean.parseBoolean(customSettings.getProperty("CustomHeroSubSkill", "False"));
            SUB_START_LVL = Byte.parseByte(customSettings.getProperty("SubStartLvl", "40"));
            MULTILANG_ENABLE = Boolean.parseBoolean(customSettings.getProperty("MultilangEnable", "False"));
            MULTILANG_DEFAULT = customSettings.getProperty("MultilangDefault", "ru");
            MULTILANG_ALLOWED = Arrays.asList(customSettings.getProperty("MultilangAllowed", "ru").split(","));
            RUN_SPD_BOOST = Integer.parseInt(customSettings.getProperty("RunSpeedBoost", "0"));
            MAX_RUN_SPEED = Integer.parseInt(customSettings.getProperty("MaxRunSpeed", "250"));
            MAX_PCRIT_RATE = Integer.parseInt(customSettings.getProperty("MaxPCritRate", "500"));
            MAX_MCRIT_RATE = Integer.parseInt(customSettings.getProperty("MaxMCritRate", "100"));
            MAX_PATK_SPEED = Integer.parseInt(customSettings.getProperty("MaxPAtkSpeed", "1500"));
            MAX_MATK_SPEED = Integer.parseInt(customSettings.getProperty("MaxMAtkSpeed", "1900"));
            MAX_MAX_HP = Integer.parseInt(customSettings.getProperty("MaxMaxHp", "30000"));

            BUFFS_MAX_AMOUNT = Integer.parseInt(customSettings.getProperty("MaxBuffAmount", "24"));
            BUFFS_PET_MAX_AMOUNT = Integer.parseInt(customSettings.getProperty("MaxPetBuffAmount", "20"));

            MAX_SUBCLASS = Byte.parseByte(customSettings.getProperty("MaxSubClasses", "3"));

            AUTO_LOOT = Boolean.parseBoolean(customSettings.getProperty("AutoLoot", "False"));
            AUTO_LOOT_RAID = Boolean.parseBoolean(customSettings.getProperty("AutoLootRaid", "False"));
            ALT_EPIC_JEWERLY = Boolean.parseBoolean(customSettings.getProperty("AutoLootEpicJewerly", "False"));

            USE_CHAT_FILTER = Boolean.parseBoolean(customSettings.getProperty("UseChatFilter", "True"));
            CHAT_FILTER_STRING = customSettings.getProperty("ChatFilterString", "-_-");
            if (USE_CHAT_FILTER) {
                loadChatFilter();
            }

            ALT_GAME_NEW_CHAR_ALWAYS_IS_NOBLE = Boolean
                    .parseBoolean(customSettings.getProperty("AltNewCharAlwaysIsNoble", "False"));
            ALT_START_LEVEL = Integer.parseInt(customSettings.getProperty("AltStartedLevel", "0"));
            ALT_START_SP = Integer.parseInt(customSettings.getProperty("AltStartedSP", "0"));
            ALT_ALLOW_AUGMENT_ON_OLYMP = Boolean
                    .parseBoolean(customSettings.getProperty("AllowAugmnetOlympiad", "True"));
            ALT_ALLOW_AUC = Boolean.parseBoolean(customSettings.getProperty("AllowAuction", "True"));

            ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean
                    .parseBoolean(customSettings.getProperty("AltSubClassWithoutQuests", "False"));

            AUTO_LEARN_SKILLS = Boolean.parseBoolean(customSettings.getProperty("AutoLearnSkills", "false"));

            ALT_WEIGHT_LIMIT = Double.parseDouble(customSettings.getProperty("AltWeightLimit", "1"));

            STARTING_ADENA = Integer.parseInt(customSettings.getProperty("StartingAdena", "100"));

            WYVERN_SPEED = Integer.parseInt(customSettings.getProperty("WyvernSpeed", "100"));
            STRIDER_SPEED = Integer.parseInt(customSettings.getProperty("StriderSpeed", "80"));
            WATER_SPEED = Integer.parseInt(customSettings.getProperty("WaterSpeed", "60"));

            AUGMENT_BASESTAT = Integer.parseInt(customSettings.getProperty("AugmentBasestat", "1"));
            AUGMENT_SKILL_NORM = Integer.parseInt(customSettings.getProperty("AugmentSkillNormal", "11"));
            AUGMENT_SKILL_MID = Integer.parseInt(customSettings.getProperty("AugmentSkillMid", "11"));
            AUGMENT_SKILL_HIGH = Integer.parseInt(customSettings.getProperty("AugmentSkillHigh", "11"));
            AUGMENT_SKILL_TOP = Integer.parseInt(customSettings.getProperty("AugmentSkillTop", "11"));

            ALLOW_RUPOR = Boolean.parseBoolean(customSettings.getProperty("AllowRupor", "False"));
            RUPOR_ID = Integer.parseInt(customSettings.getProperty("RuporId", "50002"));

            KICK_L2WALKER = Boolean.parseBoolean(customSettings.getProperty("L2WalkerProtection", "True"));

            ALLOW_RAID_PVP = Boolean.parseBoolean(customSettings.getProperty("AllowRaidPvpZones", "False"));
            CP_REUSE_TIME = Long.parseLong(customSettings.getProperty("CpReuseTime", "200"));
            MANA_RESTORE = Long.parseLong(customSettings.getProperty("ManaRestore", "800"));
            ANTIBUFF_SKILLID = Integer.parseInt(customSettings.getProperty("AntiBuffSkillId", "2276"));
            MAGIC_CRIT_EXP = Double.parseDouble(customSettings.getProperty("MagicCritExp", "4"));
            MAGIC_CRIT_EXP_OLY = Double.parseDouble(customSettings.getProperty("MagicCritExpOlymp", "4"));
            MAGIC_DAM_EXP = Double.parseDouble(customSettings.getProperty("MagicDamExp", "1"));
            MAGIC_PDEF_EXP = Double.parseDouble(customSettings.getProperty("MagicPdefExp", "1"));

            BLOW_CHANCE_FRONT = Integer.parseInt(customSettings.getProperty("BlowChanceFront", "50"));
            BLOW_CHANCE_BEHIND = Integer.parseInt(customSettings.getProperty("BlowChanceBehind", "70"));
            BLOW_CHANCE_SIDE = Integer.parseInt(customSettings.getProperty("BlowChanceSide", "60"));

            BLOW_DAMAGE_HEAVY = Double.parseDouble(customSettings.getProperty("BlowDamageHeavy", "1"));
            BLOW_DAMAGE_LIGHT = Double.parseDouble(customSettings.getProperty("BlowDamageLight", "1"));
            BLOW_DAMAGE_ROBE = Double.parseDouble(customSettings.getProperty("BlowDamageRobe", "1"));

            SONLINE_ANNOUNE = Boolean.parseBoolean(customSettings.getProperty("AnnounceOnline", "False"));
            SONLINE_ANNOUNCE_DELAY = (int) TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(customSettings.getProperty("AnnounceDelay", "10")));
            SONLINE_SHOW_MAXONLINE = Boolean.parseBoolean(customSettings.getProperty("ShowMaxOnline", "False"));
            SONLINE_SHOW_MAXONLINE_DATE = Boolean
                    .parseBoolean(customSettings.getProperty("ShowMaxOnlineDate", "False"));
            SONLINE_SHOW_OFFLINE = Boolean.parseBoolean(customSettings.getProperty("ShowOfflineTraders", "False"));
            SONLINE_LOGIN_ONLINE = Boolean.parseBoolean(customSettings.getProperty("AnnounceOnLogin", "False"));
            SONLINE_LOGIN_MAX = Boolean.parseBoolean(customSettings.getProperty("ShowLoginMaxOnline", "False"));
            SONLINE_LOGIN_DATE = Boolean.parseBoolean(customSettings.getProperty("ShowLoginMaxOnlineDate", "False"));
            SONLINE_LOGIN_OFFLINE = Boolean
                    .parseBoolean(customSettings.getProperty("ShowLoginOfflineTraders", "False"));

            ALTCLASS_MANAGER = Boolean.parseBoolean(customSettings.getProperty("startclass", "False"));

            AUTO_ANNOUNCE_ALLOW = Boolean.parseBoolean(customSettings.getProperty("AutoAnnouncementsAllow", "False"));
            AUTO_ANNOUNCE_DELAY = Integer.parseInt(customSettings.getProperty("AutoAnnouncementsDelay", "600000"));

            String[] propertySplit = customSettings.getProperty("ForbiddenOlympItems", "1234").split(",");
            for (String id : propertySplit) {
                if (id.isEmpty()) {
                    continue;
                }
                F_OLY_ITEMS.add(Integer.parseInt(id));
            }

            FORBIDDEN_EVENT_ITMES = Boolean.parseBoolean(customSettings.getProperty("ForbiddenOnEvents", "False"));

            INVIS_SHOW = Boolean.parseBoolean(customSettings.getProperty("BroadcastInvis", "True"));
            CLAN_CH_CLEAN = TimeUnit.DAYS
                    .toMillis(Integer.parseInt(customSettings.getProperty("ClanHallFreeAfter", "7")));
            CHECK_SKILLS = Boolean.parseBoolean(customSettings.getProperty("CheckSkills", "True"));
            //
            String tickets = customSettings.getProperty("MultisellTickets", "");
            if (!tickets.isEmpty()) {
                propertySplit = tickets.split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        MULTVIP_CARDS.put(Integer.valueOf(aug[0]),
                                new EventReward(Integer.valueOf(aug[1]), Integer.valueOf(aug[2]), 0));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("custom.cfg: MultisellTickets error: " + aug[0]);
                        }
                    }
                }
            }
            //
            ALLOW_APELLA_BONUSES = Boolean.parseBoolean(customSettings.getProperty("AllowApellaPassives", "True"));
            SHOW_ENTER_WARNINGS = Boolean.parseBoolean(customSettings.getProperty("ShowLoginWarnings", "False"));
            //
            CASTLE_SIEGE_REWARD_STATIC = Boolean
                    .parseBoolean(customSettings.getProperty("CastleSiegeRewardsStatic", "False"));
            propertySplit = customSettings.getProperty("CastleSiegeRewards", "").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("")) {
                    continue;
                }

                String[] aug = augs.split("#");
                int castleId = Integer.parseInt(aug[0]);
                String[] locs = aug[1].split(":");
                FastList<EventReward> locfl = new FastList<EventReward>();
                for (String points : locs) {
                    String[] loc = points.split(",");
                    try {
                        locfl.add(new EventReward(Integer.parseInt(loc[0]), Integer.parseInt(loc[1]),
                                Integer.parseInt(loc[2])));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("custom.cfg: CastleSiegeRewards error: " + aug[0]);
                        }
                    }
                }
                CASTLE_SIEGE_REWARDS.put(castleId, locfl);
            }
            SIEGE_REWARD_CLAN = Boolean.parseBoolean(customSettings.getProperty("SiegeRewardClan", "False"));
            propertySplit = customSettings.getProperty("CastleProtectRewards", "").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("")) {
                    continue;
                }

                String[] aug = augs.split("#");
                int castleId = Integer.parseInt(aug[0]);
                String[] locs = aug[1].split(":");
                FastList<EventReward> locfl = new FastList<EventReward>();
                for (String points : locs) {
                    String[] loc = points.split(",");
                    try {
                        locfl.add(new EventReward(Integer.parseInt(loc[0]), Integer.parseInt(loc[1]),
                                Integer.parseInt(loc[2])));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("custom.cfg: CastleProtectRewards error: " + aug[0]);
                        }
                    }
                }
                CASTLE_RPOTECT_REWARDS.put(castleId, locfl);
            }
            //
            CASTLE_SIEGE_SKILLS_DELETE = Boolean
                    .parseBoolean(customSettings.getProperty("CastleSiegeSkillDelete", "False"));
            String cssr = customSettings.getProperty("CastleSiegeSkillRewards", "Off");
            if (!cssr.equalsIgnoreCase("Off")) {
                propertySplit = cssr.split(";");
                for (String augs : propertySplit) {
                    if (augs.equals("")) {
                        continue;
                    }

                    String[] aug = augs.split("#");
                    int castleId = Integer.parseInt(aug[0]);
                    String[] locs = aug[1].split(",");
                    try {
                        CASTLE_SIEGE_SKILLS.put(castleId,
                                new EventReward(Integer.parseInt(locs[0]), Integer.parseInt(locs[1]), 0));
                    } catch (NumberFormatException nfe) {
                        if (aug.length > 0) {
                            System.out.println("custom.cfg: CastleSiegeSkillRewards error: " + aug[0]);
                        }
                    }

                }
            }
            //
            ALT_BUFF_TIMEMUL = Integer.parseInt(customSettings.getProperty("BuffTimeMul", "0"));
            propertySplit = customSettings.getProperty("BuffTimeTable", "").split(";"); // AltBuffTimeTable =
                                                                                        // 12#1204,112;36#1040,1243;45#1005
            for (String augs : propertySplit) {
                if (augs.equals("")) {
                    continue;
                }

                String[] aug = augs.split("#");
                int time = Integer.parseInt(aug[0]);
                String[] buffs = aug[1].split(",");
                for (String bufftime : buffs) {
                    try {
                        ALT_BUFF_TIME.put(Integer.parseInt(bufftime), time);
                    } catch (NumberFormatException nfe) {
                        if (!bufftime.equals("")) {
                            System.out.println("custom.cfg: BuffTimeTable error: " + bufftime);
                        }
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("CustomMagicWeapons", "").split(",");
            for (String itemid : propertySplit) {
                try {
                    ALT_MAGIC_WEAPONS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: CustomMagicWeapons error: " + itemid);
                    }
                }
            }
            //
            TATOO_SKILLS = Boolean.parseBoolean(customSettings.getProperty("TattooSkills", "False"));
            propertySplit = customSettings.getProperty("TattooSkillsList", "").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    TATOO_SKILLS_LIST.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("custom.cfg: TattooSkillsList error: " + aug[0]);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("StartUpItems", "").split(";");
            for (String augs : propertySplit) {
                if (augs.equals("")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    CUSTOM_STRT_ITEMS.put(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("custom.cfg: StartUpItems error: " + aug[0]);
                    }
                }
            }

            propertySplit = customSettings.getProperty("HeavyPenalty", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    HEAVY_PENALTY.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: HeavyPenalty error: " + itemid);
                    }
                }
            }

            propertySplit = customSettings.getProperty("LightPenalty", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    LIGHT_PENALTY.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: LightPenalty error: " + itemid);
                    }
                }
            }

            propertySplit = customSettings.getProperty("RobePenalty", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    MAGIC_PENALTY.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: RobePenalty error: " + itemid);
                    }
                }
            }

            propertySplit = customSettings.getProperty("ShieldPenalty", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    SHIELD_PENALTY.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ShieldPenalty error: " + itemid);
                    }
                }
            }

            //
            propertySplit = customSettings.getProperty("ForbiddenBowClasses", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    FORBIDDEN_BOW_CLASSES.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ForbiddenBowClasses error: " + itemid);
                    }
                }
            }
            propertySplit = customSettings.getProperty("ForbiddenBowClassesOly", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    FORBIDDEN_BOW_CLASSES_OLY.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ForbiddenBowClassesOly error: " + itemid);
                    }
                }
            }
            propertySplit = customSettings.getProperty("ForbiddenDualClasses", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    FORBIDDEN_DUAL_CLASSES.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ForbiddenDualClasses error: " + itemid);
                    }
                }
            }
            propertySplit = customSettings.getProperty("ForbiddenFistClasses", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    FORBIDDEN_FIST_CLASSES.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ForbiddenFistClasses error: " + itemid);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("HippyItems", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    HIPPY_ITEMS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: HippyItems error: " + itemid);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("ForbiddenCursedSkills", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    FORB_CURSED_SKILLS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ForbiddenCursedSkills error: " + itemid);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("CustomAugments", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    CUSTOM_AUG_SKILLS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: CustomAugments error: " + itemid);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("ProtectedSubBuffs", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    STOP_SUB_BUFFS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ProtectedSubBuffs error: " + itemid);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("ForbiddenOlySkills", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    FORB_OLY_SKILLS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ForbiddenOlySkills error: " + itemid);
                    }
                }
            }
            propertySplit = customSettings.getProperty("ForbiddenEventSkills", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    FORB_EVENT_SKILLS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: ForbiddenEventSkills error: " + itemid);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("WhiteSkillsList", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    WHITE_SKILLS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: WhiteSkillsList error: " + itemid);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("FixedReuseSkills", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    ALT_FIXED_REUSES.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: FixedReuseSkills error: " + itemid);
                    }
                }
            }
            propertySplit = customSettings.getProperty("FixedCastSkills", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    ALT_FIXED_CASTS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: FixedCastSkills error: " + itemid);
                    }
                }
            }
            //
            propertySplit = customSettings.getProperty("CancelProtectedBuffs", "").trim().split(",");
            for (String itemid : propertySplit) {
                try {
                    PROTECTED_BUFFS.add(Integer.valueOf(itemid));
                } catch (NumberFormatException nfe) {
                    if (!itemid.equals("")) {
                        System.out.println("custom.cfg: CancelProtectedBuffs error: " + itemid);
                    }
                }
            }
            propertySplit = null;
            //
            HERO_ITEMS_PENALTY = Boolean.parseBoolean(customSettings.getProperty("HeroItemsPenalty", "True"));
            //
            ACADEMY_CLASSIC = Boolean.parseBoolean(customSettings.getProperty("ClanAcademyClassic", "True"));
            ACADEMY_BONUS_ON = Boolean.parseBoolean(customSettings.getProperty("RewardAcademyPoints", "True"));
            ACADEMY_POINTS = Integer.parseInt(customSettings.getProperty("ClanAcademyPoints", "400"));
            //
            DISABLE_FORCES = Boolean.parseBoolean(customSettings.getProperty("DisableForces", "False"));
            //
            ALLOW_CURSED_QUESTS = Boolean.parseBoolean(customSettings.getProperty("AllowCursedQuestTalk", "True"));
            //
            MAX_TRADE_ENCHANT = Integer.parseInt(customSettings.getProperty("TradeEnchantLimit", "0"));
            //
            ALT_SIEGE_INTERVAL = Integer.parseInt(customSettings.getProperty("SiegeInterval", "14"));
            //
            SOULSHOT_ANIM = Boolean.parseBoolean(customSettings.getProperty("SoulshotAnimation", "True"));
            //
            MAX_AUGMENTS_BUFFS = Integer.parseInt(customSettings.getProperty("MaxAugmentBuffs", "1"));
            //
            STARTUP_TITLE = customSettings.getProperty("StartUpTitle", "off");
            //
            PICKUP_PENALTY = TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(customSettings.getProperty("PickUpPenalty", "15")));
            //
            SKILLS_CHANCE_MIN = Double.parseDouble(customSettings.getProperty("SkillsChanceMin", "5.0d"));
            SKILLS_CHANCE_MAX = Double.parseDouble(customSettings.getProperty("SkillsChanceMax", "95.0d"));
            SKILLS_CHANCE_MIN_OLY = Double.parseDouble(customSettings.getProperty("SkillsChanceOlyMin", "5.0d"));
            SKILLS_CHANCE_MAX_OLY = Double.parseDouble(customSettings.getProperty("SkillsChanceOlyMax", "95.0d"));
            //
            DISABLE_BOSS_INTRO = Boolean.parseBoolean(customSettings.getProperty("DisableBossIntro", "False"));
            //
            DEATH_REFLECT = Boolean.parseBoolean(customSettings.getProperty("DeathReflect", "False"));
            //
            PROTECT_SAY = Boolean.parseBoolean(customSettings.getProperty("ChatFloodFilter", "False"));
            PROTECT_SAY_COUNT = Integer.parseInt(customSettings.getProperty("ChatFloodFilterCount", "5"));
            PROTECT_SAY_BAN = Integer.parseInt(customSettings.getProperty("ChatFloodFilterBan", "5")) * 60;
            PROTECT_SAY_INTERVAL = TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(customSettings.getProperty("ChatFloodFilterInterval", "10")));
            //
            MIRAGE_CHANCE = Integer.parseInt(customSettings.getProperty("MirageChanse", "50"));
            //
            SUMMON_CP_PROTECT = Boolean.parseBoolean(customSettings.getProperty("ProtectSummonCpFlood", "False"));
            //
            ALLOW_NPC_CHAT = Boolean.parseBoolean(customSettings.getProperty("AllowNpcChat", "False"));
            MNPC_CHAT_CHANCE = Integer.parseInt(customSettings.getProperty("NpcChatChanse", "200"));
            //
            MULTISSELL_PROTECT = Boolean.parseBoolean(customSettings.getProperty("MultisellProtect", "True"));
            //
            MULTISSELL_ERRORS = Boolean.parseBoolean(customSettings.getProperty("MultisellErrors", "True"));
            //
            CHEST_CHANCE = Integer.parseInt(customSettings.getProperty("ChestOpenChance", "80"));
            //
            MOB_DEBUFF_CHANCE = Integer.parseInt(customSettings.getProperty("MonsterDebuffChance", "60"));
            //
            EVERYBODE_HERO = Boolean.parseBoolean(customSettings.getProperty("EverybodyHero", "False"));
            //
            ALLOW_PC_NPC = Boolean.parseBoolean(customSettings.getProperty("AllowPcNpc", "False"));
            //
            ANNOUNCE_CASTLE_OWNER = Boolean.parseBoolean(customSettings.getProperty("AnnounceCastleOwner", "False"));
            //
            CHECK_SIEGE_TELE = Boolean.parseBoolean(customSettings.getProperty("CheckSiegeTele", "True"));
            //
            CAN_DROP_AUGMENTS = Boolean.parseBoolean(customSettings.getProperty("CanDropAugments", "False"));
            //
            propertySplit = customSettings.getProperty("CpRestoreValues", "50,200").split(",");
            CP_RESTORE = new EventReward(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]), 0);
            //
            MIN_HIT_TIME = Integer.parseInt(customSettings.getProperty("MinHitTime", "210"));
            //
            CHAR_CREATE_ENCHANT = Integer.parseInt(customSettings.getProperty("CharCreateEnchant", "0"));
            TVT_CUSTOM_ENCHANT = Integer.parseInt(customSettings.getProperty("tvtenchant", "0"));
            ENABLE_FAKE_ITEMS_MOD = Boolean.parseBoolean(customSettings.getProperty("EnableFakeItemsMod", "False"));
            //
            ARMORSETS_XML = Boolean.parseBoolean(customSettings.getProperty("ArmorSetsXML", "False"));
            //
            NPC_DEWALKED_ZONE = Boolean.parseBoolean(customSettings.getProperty("TeleDewalkedZone", "False"));
            //
            WALK_FAR = Boolean.parseBoolean(customSettings.getProperty("RunFar", "False"));
            //
            propertySplit = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + CUSTOM_CONFIG_FILE + " File.");
        }
    }

    public static void loadPvpCfg() {
        KARMA_LIST_NONDROPPABLE_PET_ITEMS.clear();
        KARMA_LIST_NONDROPPABLE_ITEMS.clear();
        PVPPK_EXP_SP.clear();
        PVPPK_PVPITEMS.clear();
        PVPPK_PKITEMS.clear();
        PVPBONUS_ITEMS.clear();
        PVPBONUS_TITLE.clear();
        PVPBONUS_COLORS.clear();
        PVPBONUS_COLORS_NAME.clear();
        PVPBONUS_COLORS_TITLE.clear();
        //
        PVPBONUS_COLORS_NAME.add(16777215);
        PVPBONUS_COLORS_TITLE.add(16777079);
        //
        try {
            Properties pvpSettings = new Properties();
            InputStream is = new FileInputStream(new File(PVP_CONFIG_FILE));
            pvpSettings.load(is);
            is.close();

            /*
             * KARMA SYSTEM
             */
            KARMA_MIN_KARMA = Integer.parseInt(pvpSettings.getProperty("MinKarma", "240"));
            KARMA_MAX_KARMA = Integer.parseInt(pvpSettings.getProperty("MaxKarma", "10000"));
            KARMA_XP_DIVIDER = Integer.parseInt(pvpSettings.getProperty("XPDivider", "260"));
            KARMA_LOST_BASE = Integer.parseInt(pvpSettings.getProperty("BaseKarmaLost", "0"));

            KARMA_DROP_GM = Boolean.parseBoolean(pvpSettings.getProperty("CanGMDropEquipment", "false"));
            KARMA_AWARD_PK_KILL = Boolean.parseBoolean(pvpSettings.getProperty("AwardPKKillPVPPoint", "true"));

            KARMA_PK_LIMIT = Integer.parseInt(pvpSettings.getProperty("MinimumPKRequiredToDrop", "5"));
            KARMA_PK_NPC_DROP = Boolean.parseBoolean(pvpSettings.getProperty("DropKilledByNpc", "True"));

            KARMA_NONDROPPABLE_PET_ITEMS = pvpSettings.getProperty("ListOfPetItems",
                    "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650");
            KARMA_NONDROPPABLE_ITEMS = pvpSettings.getProperty("ListOfNonDroppableItems",
                    "57,1147,425,1146,461,10,2368,7,6,2370,2369,6842,6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621");

            KARMA_LIST_NONDROPPABLE_PET_ITEMS = new FastList<Integer>();
            for (String id : KARMA_NONDROPPABLE_PET_ITEMS.split(",")) {
                KARMA_LIST_NONDROPPABLE_PET_ITEMS.add(Integer.parseInt(id));
            }

            KARMA_LIST_NONDROPPABLE_ITEMS = new FastList<Integer>();
            for (String id : KARMA_NONDROPPABLE_ITEMS.split(",")) {
                KARMA_LIST_NONDROPPABLE_ITEMS.add(Integer.parseInt(id));
            }

            PVP_NORMAL_TIME = Integer.parseInt(pvpSettings.getProperty("PvPVsNormalTime", "15000"));
            PVP_PVP_TIME = Integer.parseInt(pvpSettings.getProperty("PvPVsPvPTime", "30000"));

            //
            // 1
            ALLOW_PVPPK_REWARD = Boolean.parseBoolean(pvpSettings.getProperty("AllowPvpPkReward", "False"));
            PVPPK_INTERVAL = Integer.parseInt(pvpSettings.getProperty("PvpPkInterval", "30000"));
            PVPPK_IPPENALTY = Boolean.parseBoolean(pvpSettings.getProperty("PvpPkIpPenalty", "False"));
            PVPPK_HWIDPENALTY = Boolean.parseBoolean(pvpSettings.getProperty("PvpPkHwidPenalty", "False"));

            TOWN_PVP_REWARD = Boolean.parseBoolean(pvpSettings.getProperty("TownPvpReward", "False"));

            SHOW_KILLER_INFO = Boolean.parseBoolean(pvpSettings.getProperty("ShowKillerInfo", "False"));
            SHOW_KILLER_INFO_ITEMS = Boolean.parseBoolean(pvpSettings.getProperty("ShowKillerInfoItems", "False"));

            REWARD_PVP_ZONE = Boolean.parseBoolean(pvpSettings.getProperty("RewardPvpZone", "False"));
            REWARD_PVP_ZONE_CLAN = Boolean.parseBoolean(pvpSettings.getProperty("RewardPvpZoneClan", "False"));
            REWARD_PVP_ZONE_PARTY = Boolean.parseBoolean(pvpSettings.getProperty("RewardPvpZoneParty", "False"));
            REWARD_PVP_ZONE_HWID = Boolean.parseBoolean(pvpSettings.getProperty("RewardPvpZoneHwid", "False"));
            REWARD_PVP_ZONE_IP = Boolean.parseBoolean(pvpSettings.getProperty("RewardPvpZoneIp", "False"));
            REWARD_PVP_ZONE_ANNOUNCE = Integer.parseInt(pvpSettings.getProperty("RewardPvpZoneAnnounce", "5"));

            String[] ppp = pvpSettings.getProperty("PvpPkLevelPenalty", "0,0").split(",");
            PVPPK_PENALTY = new PvpColor(Integer.parseInt(ppp[0]), Integer.parseInt(ppp[1]));

            ppp = pvpSettings.getProperty("PvpExpSp", "0,0;0,0").split(";");
            for (String augs : ppp) {
                String[] aug = augs.split(",");
                try {
                    PVPPK_EXP_SP.add(new PvpColor(Integer.parseInt(aug[0]), Integer.parseInt(aug[1])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("pvp.cfg: PvpExpSp error: " + aug[0]);
                    }
                }
            }

            ppp = pvpSettings.getProperty("PvPRewards", "n,n,n;n,n,n").split(";");
            for (String augs : ppp) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    PVPPK_PVPITEMS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("pvp.cfg: PvpExpSp error: " + aug[0]);
                    }
                }
            }
            ppp = pvpSettings.getProperty("PkRewards", "n,n,n;n,n,n").split(";");
            for (String augs : ppp) {
                if (augs.equals("n,n,n")) {
                    break;
                }

                String[] aug = augs.split(",");
                try {
                    PVPPK_PKITEMS.add(new EventReward(Integer.parseInt(aug[0]), Integer.parseInt(aug[1]),
                            Integer.parseInt(aug[2])));
                } catch (NumberFormatException nfe) {
                    if (!aug[0].equals("")) {
                        System.out.println("pvp.cfg: PvpExpSp error: " + aug[0]);
                    }
                }
            }

            // 2
            ALLOW_PVPBONUS_STEPS = Boolean.parseBoolean(pvpSettings.getProperty("AllowPvpBonusSteps", "False"));

            String pvpBonusStepsColors = pvpSettings.getProperty("PvpBonusStepsRewards", "None");
            if (!pvpBonusStepsColors.equals("None")) {
                String[] propertySplit = pvpBonusStepsColors.split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        PVPBONUS_ITEMS.put(Integer.parseInt(aug[0]),
                                new PvpColor(Integer.parseInt(aug[1]), Integer.parseInt(aug[2])));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("pvp.cfg: PvpBonusStepsRewards error: " + aug[0]);
                        }
                    }
                }
                propertySplit = null;
            }
            pvpBonusStepsColors = pvpSettings.getProperty("PvpBonusStepsColors", "None");
            if (!pvpBonusStepsColors.equals("None")) {
                String[] propertySplit = pvpBonusStepsColors.split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        String nick = new TextBuilder(aug[1]).reverse().toString();
                        String title = new TextBuilder(aug[2]).reverse().toString();

                        int nick_hex = 0;
                        int title_hex = 0;

                        if (!nick.equals("llun")) {
                            nick_hex = Integer.decode("0x" + nick);
                        }

                        if (!title.equals("llun")) {
                            title_hex = Integer.decode("0x" + title);
                        }

                        PVPBONUS_COLORS_NAME.add(nick_hex);
                        PVPBONUS_COLORS_TITLE.add(title_hex);
                        PVPBONUS_COLORS.put(Integer.parseInt(aug[0]), new PvpColor(nick_hex, title_hex));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("pvp.cfg: PvpBonusStepsColors error: " + aug[0]);
                        }
                    }
                }
                propertySplit = null;
            }
            //
            PVP_TITLE_SKILL = Boolean.parseBoolean(pvpSettings.getProperty("PvpTitleSkill", "False"));
            pvpBonusStepsColors = pvpSettings.getProperty("PvpBonusTitleSkills", "None");
            if (!pvpBonusStepsColors.equals("None")) {
                String[] propertySplit = pvpBonusStepsColors.split(";");
                for (String augs : propertySplit) {
                    String[] aug = augs.split(",");
                    try {
                        // 10-20,Новичек,8001,1
                        String[] minmax = aug[0].split("-");
                        int min = Integer.parseInt(minmax[0]);
                        int max = Integer.parseInt(minmax[1]);
                        // PVPBONUS_TITLE.add(new PvpColor(aug[1], Integer.parseInt(aug[2]),
                        // Integer.parseInt(aug[3])));
                        PVPBONUS_TITLE.add(new PvpTitleBonus(aug[1], min, max, Integer.parseInt(aug[2]),
                                Integer.parseInt(aug[3])));
                    } catch (NumberFormatException nfe) {
                        if (!aug[0].equals("")) {
                            System.out.println("pvp.cfg: PvpBonusStepsRewards error: " + aug[0]);
                        }
                    }
                }
                propertySplit = null;
            }
            //
            pvpBonusStepsColors = null;
            PVPPK_STEP = Integer.parseInt(pvpSettings.getProperty("PvpPkStep", "0"));
            PVPPK_STEPBAN = TimeUnit.MINUTES.toMillis(Integer.parseInt(pvpSettings.getProperty("PvpPkStepBan", "10")));
            PVPPK_REWARD_ZONE = Boolean.parseBoolean(pvpSettings.getProperty("PvpPkRewardZone", "False"));
            //
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + PVP_CONFIG_FILE + " File.");
        }
    }

    public static void loadAccessLvlCfg() {
        try {
            Properties gmSettings = new Properties();
            InputStream is = new FileInputStream(new File(GM_ACCESS_FILE));
            gmSettings.load(is);
            is.close();

            GM_ACCESSLEVEL = Integer.parseInt(gmSettings.getProperty("GMAccessLevel", "100"));
            GM_MIN = Integer.parseInt(gmSettings.getProperty("GMMinLevel", "100"));
            GM_ALTG_MIN_LEVEL = Integer.parseInt(gmSettings.getProperty("GMCanAltG", "100"));
            GM_ANNOUNCE = Integer.parseInt(gmSettings.getProperty("GMCanAnnounce", "100"));
            GM_BAN = Integer.parseInt(gmSettings.getProperty("GMCanBan", "100"));
            GM_BAN_CHAT = Integer.parseInt(gmSettings.getProperty("GMCanBanChat", "100"));
            GM_CREATE_ITEM = Integer.parseInt(gmSettings.getProperty("GMCanShop", "100"));
            GM_DELETE = Integer.parseInt(gmSettings.getProperty("GMCanDelete", "100"));
            GM_KICK = Integer.parseInt(gmSettings.getProperty("GMCanKick", "100"));
            GM_MENU = Integer.parseInt(gmSettings.getProperty("GMMenu", "100"));
            GM_GODMODE = Integer.parseInt(gmSettings.getProperty("GMGodMode", "100"));
            GM_CHAR_EDIT = Integer.parseInt(gmSettings.getProperty("GMCanEditChar", "100"));
            GM_CHAR_EDIT_OTHER = Integer.parseInt(gmSettings.getProperty("GMCanEditCharOther", "100"));
            GM_CHAR_VIEW = Integer.parseInt(gmSettings.getProperty("GMCanViewChar", "100"));
            GM_NPC_EDIT = Integer.parseInt(gmSettings.getProperty("GMCanEditNPC", "100"));
            GM_NPC_VIEW = Integer.parseInt(gmSettings.getProperty("GMCanViewNPC", "100"));
            GM_TELEPORT = Integer.parseInt(gmSettings.getProperty("GMCanTeleport", "100"));
            GM_TELEPORT_OTHER = Integer.parseInt(gmSettings.getProperty("GMCanTeleportOther", "100"));
            GM_RESTART = Integer.parseInt(gmSettings.getProperty("GMCanRestart", "100"));
            GM_MONSTERRACE = Integer.parseInt(gmSettings.getProperty("GMMonsterRace", "100"));
            GM_RIDER = Integer.parseInt(gmSettings.getProperty("GMRider", "100"));
            GM_ESCAPE = Integer.parseInt(gmSettings.getProperty("GMFastUnstuck", "100"));
            GM_FIXED = Integer.parseInt(gmSettings.getProperty("GMResurectFixed", "100"));
            GM_CREATE_NODES = Integer.parseInt(gmSettings.getProperty("GMCreateNodes", "100"));
            GM_ENCHANT = Integer.parseInt(gmSettings.getProperty("GMEnchant", "100"));
            GM_DOOR = Integer.parseInt(gmSettings.getProperty("GMDoor", "100"));
            GM_RES = Integer.parseInt(gmSettings.getProperty("GMRes", "100"));
            GM_PEACEATTACK = Integer.parseInt(gmSettings.getProperty("GMPeaceAttack", "100"));
            GM_HEAL = Integer.parseInt(gmSettings.getProperty("GMHeal", "100"));
            GM_UNBLOCK = Integer.parseInt(gmSettings.getProperty("GMUnblock", "100"));
            GM_CACHE = Integer.parseInt(gmSettings.getProperty("GMCache", "100"));
            GM_TALK_BLOCK = Integer.parseInt(gmSettings.getProperty("GMTalkBlock", "100"));
            GM_TEST = Integer.parseInt(gmSettings.getProperty("GMTest", "100"));

            String gmTrans = gmSettings.getProperty("GMDisableTransaction", "False");

            if (!gmTrans.equalsIgnoreCase("false")) {
                String[] params = gmTrans.split(",");
                GM_DISABLE_TRANSACTION = true;
                GM_TRANSACTION_MIN = Integer.parseInt(params[0]);
                GM_TRANSACTION_MAX = Integer.parseInt(params[1]);
            } else {
                GM_DISABLE_TRANSACTION = false;
            }
            GM_CAN_GIVE_DAMAGE = Integer.parseInt(gmSettings.getProperty("GMCanGiveDamage", "90"));
            GM_DONT_TAKE_AGGRO = Integer.parseInt(gmSettings.getProperty("GMDontTakeAggro", "90"));
            GM_DONT_TAKE_EXPSP = Integer.parseInt(gmSettings.getProperty("GMDontGiveExpSp", "90"));

        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + GM_ACCESS_FILE + " File.");
        }
    }

    public static void loadCommandsCfg() {
        try {
            Properties cmd = new Properties();
            InputStream is = new FileInputStream(new File(CMD_CONFIG_FILE));
            cmd.load(is);
            is.close();

            // .menu
            CMD_MENU = Boolean.parseBoolean(cmd.getProperty("AllowMenu", "False"));
            VS_NOEXP = Boolean.parseBoolean(cmd.getProperty("NoExp", "False"));
            VS_NOREQ = Boolean.parseBoolean(cmd.getProperty("NoRequests", "False"));
            VS_VREF = Boolean.parseBoolean(cmd.getProperty("VoteRef", "False"));
            VS_AUTOLOOT = Boolean.parseBoolean(cmd.getProperty("Autoloot", "False"));
            VS_TRADERSIGNORE = Boolean.parseBoolean(cmd.getProperty("TradersIgnore", "False"));
            VS_PATHFIND = Boolean.parseBoolean(cmd.getProperty("GeoPathFinding", "False"));
            VS_CHATIGNORE = Boolean.parseBoolean(cmd.getProperty("ChatIgnore", "False"));
            VS_ONLINE = Boolean.parseBoolean(cmd.getProperty("ShowOnline", "False"));
            VS_AUTORESTAT = Boolean.parseBoolean(cmd.getProperty("ShowRestartTime", "False"));
            VS_SKILL_CHANCES = Boolean.parseBoolean(cmd.getProperty("SkillChances", "False"));
            VS_ANIM_SHOTS = Boolean.parseBoolean(cmd.getProperty("ShotsAnimation", "False"));

            VS_AUTOLOOT_VAL = Integer.parseInt(cmd.getProperty("AutolootDefault", "0"));
            VS_PATHFIND_VAL = Integer.parseInt(cmd.getProperty("GeoPathFindingDefault", "0"));
            VS_SKILL_CHANCES_VAL = Integer.parseInt(cmd.getProperty("SkillChancesDefault", "0"));

            // .security
            VS_HWID = Boolean.parseBoolean(cmd.getProperty("HWID", "False"));
            VS_PWD = Boolean.parseBoolean(cmd.getProperty("Password", "False"));
            VS_EMAIL = Boolean.parseBoolean(cmd.getProperty("Email", "False"));

            ALT_ALLOW_OFFLINE_TRADE = Boolean.parseBoolean(cmd.getProperty("AllowOfflineTrade", "False"));
            ALT_RESTORE_OFFLINE_TRADE = Boolean.parseBoolean(cmd.getProperty("RestoreOfflineTraders", "False"));
            ALT_OFFLINE_TRADE_LIMIT = TimeUnit.HOURS.toMillis(Integer.parseInt(cmd.getProperty("OfflineLimit", "96")));
            ALT_OFFLINE_TRADE_ONLINE = Boolean.parseBoolean(cmd.getProperty("AltOfflineTraderStatus", "False"));

            OFFTRADE_COIN = Integer.parseInt(cmd.getProperty("OfflineCoin", "0"));
            OFFTRADE_PRICE = Integer.parseInt(cmd.getProperty("OfflinePrice", "0"));
            OFFTRADE_COIN_NAME = cmd.getProperty("OfflineCoinName", "0");

            CMD_ADENA_COL = Boolean.parseBoolean(cmd.getProperty("CmdAdenaCol", "False"));
            String[] pSplit = cmd.getProperty("AdenaToCol", "4037,1,2000000000").split(",");
            CMD_AC_ADENA = new EventReward(Integer.parseInt(pSplit[0]), Integer.parseInt(pSplit[1]),
                    Integer.parseInt(pSplit[2]));
            pSplit = cmd.getProperty("ColToAdena", "57,1700000000,1").split(",");
            CMD_AC_COL = new EventReward(Integer.parseInt(pSplit[0]), Integer.parseInt(pSplit[1]),
                    Integer.parseInt(pSplit[2]));
            pSplit = null;
            CMD_AC_ADENA_LIMIT = Integer.parseInt(cmd.getProperty("AdenaToColLimit", "0"));
            CMD_AC_COL_LIMIT = Integer.parseInt(cmd.getProperty("ColToAdenaLimit", "0"));

            CMD_EVENTS = Boolean.parseBoolean(cmd.getProperty("CmdEvents", "False"));

            MAX_BAN_CHAT = Integer.parseInt(cmd.getProperty("MaxBanChat", "15"));

            VS_CKEY = Boolean.parseBoolean(cmd.getProperty("CharKey", "False"));
            VS_CKEY_CHARLEVEL = Boolean.parseBoolean(cmd.getProperty("CharKey2ndClass", "False"));
            //
            ENABLE_LOC_COMMAND = Boolean.parseBoolean(cmd.getProperty("EnableLoc", "True"));
            //
            ACP_ENGINE = Boolean.parseBoolean(cmd.getProperty("AcpEngine", "False"));
            ACP_MP = Integer.parseInt(cmd.getProperty("AcpMp", "70"));
            ACP_HP = Integer.parseInt(cmd.getProperty("AcpHp", "70"));
            ACP_CP = Integer.parseInt(cmd.getProperty("AcpCp", "70"));
            ACP_DELAY = Integer.parseInt(cmd.getProperty("AcpDelay", "100"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + CMD_CONFIG_FILE + " File.");
        }
    }

    public static void loadGeoDataCfg() {
        try {
            Properties geo = new Properties();
            InputStream is = new FileInputStream(new File(GEO_FILE));
            geo.load(is);
            is.close();

            GEODATA = Integer.parseInt(geo.getProperty("GeoData", "0"));
            GEO_TYPE = Integer.parseInt(geo.getProperty("GeoDataType", "1"));
            GEO_L2J_PATH = geo.getProperty("PathL2J", "./data/geodata/");
            GEO_OFF_PATH = geo.getProperty("PathOFF", "./data/geodataoff/");

            GEO_SHOW_LOAD = Boolean.parseBoolean(geo.getProperty("GeoDataLog", "True"));

            FORCE_GEODATA = Boolean.parseBoolean(geo.getProperty("ForceGeoData", "True"));
            ACCEPT_GEOEDITOR_CONN = Boolean.parseBoolean(geo.getProperty("AcceptGeoeditorConn", "False"));

            //
            WORLD_X_MIN = Integer.parseInt(geo.getProperty("WorldXMin", "10"));
            WORLD_X_MAX = Integer.parseInt(geo.getProperty("WorldXMax", "26"));
            WORLD_Y_MIN = Integer.parseInt(geo.getProperty("WorldYMin", "10"));
            WORLD_Y_MAX = Integer.parseInt(geo.getProperty("WorldYMax", "26"));
            GEODATA = Integer.parseInt(geo.getProperty("GeoData", "0"));
            GEODATA_CELLFINDING = Boolean.parseBoolean(geo.getProperty("CellPathFinding", "False"));
            PATHFIND_BUFFERS = geo.getProperty("PathFindBuffers", "100x6;128x6;192x6;256x4;320x4;384x4;500x2");
            LOW_WEIGHT = Float.parseFloat(geo.getProperty("LowWeight", "0.5"));
            MEDIUM_WEIGHT = Float.parseFloat(geo.getProperty("MediumWeight", "2"));
            HIGH_WEIGHT = Float.parseFloat(geo.getProperty("HighWeight", "3"));
            ADVANCED_DIAGONAL_STRATEGY = Boolean.parseBoolean(geo.getProperty("AdvancedDiagonalStrategy", "True"));
            DIAGONAL_WEIGHT = Float.parseFloat(geo.getProperty("DiagonalWeight", "0.707"));
            MAX_POSTFILTER_PASSES = Integer.parseInt(geo.getProperty("MaxPostfilterPasses", "3"));
            DEBUG_PATH = Boolean.parseBoolean(geo.getProperty("DebugPath", "False"));
            FORCE_GEODATA = Boolean.parseBoolean(geo.getProperty("ForceGeodata", "True"));
            COORD_SYNCHRONIZE = Integer.parseInt(geo.getProperty("CoordSynchronize", "-1"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + GEO_FILE + " File.");
        }
    }

    public static void loadFakeCfg() {
        try {
            Properties fake = new Properties();
            InputStream is = new FileInputStream(new File(FAKE_FILE));
            fake.load(is);
            is.close();

            ALLOW_FAKE_PLAYERS_PLUS = Boolean.parseBoolean(fake.getProperty("AllowFake", "False"));

            FAKE_PLAYERS_PLUS_COUNT_FIRST = Integer.parseInt(fake.getProperty("FirstCount", "50"));
            FAKE_PLAYERS_PLUS_DELAY_FIRST = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(fake.getProperty("FirstDelay", "5")));
            FAKE_PLAYERS_PLUS_DESPAWN_FIRST = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(fake.getProperty("FirstDespawn", "60")));
            FAKE_PLAYERS_PLUS_DELAY_SPAWN_FIRST = (int) TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(fake.getProperty("FirstDelaySpawn", "1")));
            FAKE_PLAYERS_PLUS_DELAY_DESPAWN_FIRST = (int) TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(fake.getProperty("FirstDelayDespawn", "20")));

            FAKE_PLAYERS_PLUS_COUNT_NEXT = Integer.parseInt(fake.getProperty("NextCount", "50"));
            FAKE_PLAYERS_PLUS_DELAY_NEXT = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(fake.getProperty("NextDelay", "15")));
            FAKE_PLAYERS_PLUS_DESPAWN_NEXT = TimeUnit.MINUTES
                    .toMillis(Integer.parseInt(fake.getProperty("NextDespawn", "90")));
            FAKE_PLAYERS_PLUS_DELAY_SPAWN_NEXT = (int) TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(fake.getProperty("NextDelaySpawn", "20")));
            FAKE_PLAYERS_PLUS_DELAY_DESPAWN_NEXT = (int) TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(fake.getProperty("NextDelayDespawn", "30")));

            String[] ppp = fake.getProperty("FakeEnchant", "0,14").split(",");
            FAKE_PLAYERS_ENCHANT = new PvpColor(Integer.parseInt(ppp[0]), Integer.parseInt(ppp[1]));

            ppp = fake.getProperty("FakeNameColors", "FFFFFF,FFFFFF").split(",");
            for (String ncolor : ppp) {
                String nick = new TextBuilder(ncolor).reverse().toString();
                FAKE_PLAYERS_NAME_CLOLORS.add(Integer.decode("0x" + nick));
            }
            ppp = fake.getProperty("FakeTitleColors", "FFFF77,FFFF77").split(",");
            for (String tcolor : ppp) {
                String title = new TextBuilder(tcolor).reverse().toString();
                FAKE_PLAYERS_TITLE_CLOLORS.add(Integer.decode("0x" + title));
            }

            FAKE_MAX_PATK_BOW = Integer.parseInt(fake.getProperty("MaxPatkBow", "50"));
            FAKE_MAX_MDEF_BOW = Integer.parseInt(fake.getProperty("MaxMdefBow", "50"));
            FAKE_MAX_PSPD_BOW = Integer.parseInt(fake.getProperty("MaxPspdBow", "50"));
            FAKE_MAX_PDEF_BOW = Integer.parseInt(fake.getProperty("MaxPdefBow", "50"));
            FAKE_MAX_MATK_BOW = Integer.parseInt(fake.getProperty("MaxMatkBow", "50"));
            FAKE_MAX_MSPD_BOW = Integer.parseInt(fake.getProperty("MaxMspdBow", "50"));
            FAKE_MAX_HP_BOW = Integer.parseInt(fake.getProperty("MaxHpBow", "50"));

            FAKE_MAX_PATK_MAG = Integer.parseInt(fake.getProperty("MaxPatkMage", "50"));
            FAKE_MAX_MDEF_MAG = Integer.parseInt(fake.getProperty("MaxMdefMage", "50"));
            FAKE_MAX_PSPD_MAG = Integer.parseInt(fake.getProperty("MaxPspdMage", "50"));
            FAKE_MAX_PDEF_MAG = Integer.parseInt(fake.getProperty("MaxPdefkMage", "50"));
            FAKE_MAX_MATK_MAG = Integer.parseInt(fake.getProperty("MaxMatkMage", "50"));
            FAKE_MAX_MSPD_MAG = Integer.parseInt(fake.getProperty("MaxMspdMage", "50"));
            FAKE_MAX_HP_MAG = Integer.parseInt(fake.getProperty("MaxHpMage", "50"));

            FAKE_MAX_PATK_HEAL = Integer.parseInt(fake.getProperty("MaxPatkHeal", "50"));
            FAKE_MAX_MDEF_HEAL = Integer.parseInt(fake.getProperty("MaxMdefHeal", "50"));
            FAKE_MAX_PSPD_HEAL = Integer.parseInt(fake.getProperty("MaxPspdHeal", "50"));
            FAKE_MAX_PDEF_HEAL = Integer.parseInt(fake.getProperty("MaxPdefHeal", "50"));
            FAKE_MAX_MATK_HEAL = Integer.parseInt(fake.getProperty("MaxMatkHeal", "50"));
            FAKE_MAX_MSPD_HEAL = Integer.parseInt(fake.getProperty("MaxMspdHeal", "50"));
            FAKE_MAX_HP_HEAL = Integer.parseInt(fake.getProperty("MaxHpHeal", "50"));

            ppp = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + FAKE_FILE + " File.");
        }
    }

    public static void loadGameGuardCfg() {
        try {
            Properties guard = new Properties();
            InputStream is = new FileInputStream(new File(GAME_GUARD_FILE));
            guard.load(is);
            is.close();

            GAMEGUARD_ENABLED = Boolean.parseBoolean(guard.getProperty("GameGuardEnable", "False"));
            GAMEGUARD_INTERVAL = (int) TimeUnit.SECONDS
                    .toMillis(Integer.parseInt(guard.getProperty("GameGuardInterval", "60")));

            GAMEGUARD_LOG = Boolean.parseBoolean(guard.getProperty("GameGuardLog", "False"));
            GAMEGUARD_PUNISH = Integer.parseInt(guard.getProperty("GameGuardPunish", "1"));

            GAMEGUARD_KEY = Integer.parseInt("FFAAFF", 16) << 8;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load " + FAKE_FILE + " File.");
        }
    }

    public static void loadHexidCfg() {
        InputStream is = null;
        try {
            Properties Settings = new Properties();
            is = new FileInputStream(HEXID_FILE);
            Settings.load(is);
            SERVER_ID = Integer.parseInt(Settings.getProperty("ServerID"));
            HEX_ID = new BigInteger(Settings.getProperty("HexID"), 16).toByteArray();
        } catch (Exception e) {
            _log.warning("Could not load HexID file (" + HEXID_FILE + "). Hopefully login will give us one.");
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ignored) {
                //
            }
        }
    }

    public static void load(boolean reload) {
        if (Server.serverMode == Server.MODE_GAMESERVER) {
            loadServerCfg();
            loadOptionsCfg();
            loadTelnetCfg();
            loadIdFactoryCfg();
            loadOtherCfg();
            loadEnchantCfg();
            loadServicesCfg();
            loadEventsCfg();
            loadRatesCfg();
            loadAltSettingCfg();
            loadSevenSignsCfg();
            loadClanHallCfg();
            loadNpcCfg();
            loadCustomCfg();
            loadPvpCfg();
            loadAccessLvlCfg();
            loadCommandsCfg();
            loadGeoDataCfg();
            loadFakeCfg();
            loadGameGuardCfg();

            loadFiltersConfig();

            if (!reload) {
                loadCatsGuardCfg();
            }
            loadHexidCfg();
            // }
        } else {
            _log.severe("Could not Load Config: server mode was not set");
        }

        if (reload) {
            _log.info(TimeLogger.getLogTime() + "Configs: reloaded.");
        } else {
            _log.info(TimeLogger.getLogTime() + "Configs: loaded.");
        }
    }

    private static void loadChatFilter() {
        CHAT_FILTER_STRINGS.clear();
        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./data/chat_filter.txt");
            if (!Data.exists()) {
                System.out.println("[ERROR] Config, loadChatFilter() '/data/chat_filter.txt' not founded. ");
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                CHAT_FILTER_STRINGS.add(line);
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] Config, loadChatFilter() error: " + e);
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
                if (br != null) {
                    br.close();
                }
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception e1) {
            }
        }
    }

    /**
     * Set a new value to a game parameter from the admin console.
     *
     * @param pName  (String) : name of the parameter to change
     * @param pValue (String) : new value of the parameter
     * @return boolean : true if modification has been made
     * @link useAdminCommand
     */
    public static boolean setParameterValue(String pName, String pValue) {
        return false;
    }

    /**
     * Allow the player to use L2Walker ?
     *
     * @param player (L2PcInstance) : Player trying to use L2Walker
     * @return boolean : true if (L2Walker allowed as a general rule) or
     *         (L2Walker client allowed for GM and player is a GM)
     */
    public static boolean allowL2Walker(L2PcInstance player) {
        return (ALLOW_L2WALKER_CLIENT == L2WalkerAllowed.True
                || (ALLOW_L2WALKER_CLIENT == L2WalkerAllowed.GM && player != null && player.isGM()));
    }

    // it has no instances
    private Config() {
    }

    /**
     * Save hexadecimal ID of the server in the properties file.
     *
     * @param string (String) : hexadecimal ID of the server to store
     * @see HEXID_FILE
     * @see saveHexid(String string, String fileName)
     * @link LoginServerThread
     */
    public static void saveHexid(int serverId, String string) {
        Config.saveHexid(serverId, string, HEXID_FILE);
    }

    /**
     * Save hexadecimal ID of the server in the properties file.
     *
     * @param hexId    (String) : hexadecimal ID of the server to store
     * @param fileName (String) : name of the properties file
     */
    public static void saveHexid(int serverId, String hexId, String fileName) {
        OutputStream out = null;
        try {
            Properties hexSetting = new Properties();
            File file = new File(fileName);
            file.createNewFile();

            out = new FileOutputStream(file);
            hexSetting.setProperty("ServerID", String.valueOf(serverId));
            hexSetting.setProperty("HexID", hexId);
            hexSetting.store(out, "the hexID to auth into login");
        } catch (Exception e) {
            _log.warning("Failed to save hex id to " + fileName + " File.");
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }
}
