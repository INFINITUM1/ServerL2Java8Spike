package net.sf.l2j.gameserver.model.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadDiary;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.log.AbstractLogger;

public class Hero {

    private static final Logger _log = AbstractLogger.getLogger(Hero.class.getName());
    private static Hero _instance;
    private static final String GET_HEROES = "SELECT * FROM heroes WHERE played = 1";
    private static final String GET_ALL_HEROES = "SELECT * FROM heroes";
    private static Map<Integer, StatsSet> _heroes;
    private static Map<Integer, StatsSet> _completeHeroes;
    public static final String COUNT = "count";
    public static final String PLAYED = "played";
    public static final String CLAN_NAME = "clan_name";
    public static final String CLAN_CREST = "clan_crest";
    public static final String ALLY_NAME = "ally_name";
    public static final String ALLY_CREST = "ally_crest";
    public static final String ACTIVE = "active";

    public static Hero getInstance() {
        if (_instance == null) {
            _instance = new Hero();
        }
        return _instance;
    }

    public Hero() {
        init();
    }

    private static void HeroSetClanAndAlly(int charId, StatsSet hero) {
        L2Clan clan = ClanTable.getInstance().getClanByCharId(charId);
        hero.set(CLAN_CREST, clan == null ? 0 : clan.getCrestId());
        hero.set(CLAN_NAME, clan == null ? "" : clan.getName());
        if (clan != null) {
            hero.set(ALLY_CREST, clan.getAllyCrestId());
            hero.set(ALLY_NAME, clan.getAllyName() == null ? "" : clan.getAllyName());
        } else {
            hero.set(ALLY_CREST, 0);
            hero.set(ALLY_NAME, "");
        }
    }

    private void init() {
        _heroes = new FastMap<Integer, StatsSet>().shared("Hero._heroes");
        _completeHeroes = new FastMap<Integer, StatsSet>().shared("Hero._completeHeroes");

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement(GET_HEROES);
            rset = statement.executeQuery();
            while (rset.next()) {
                StatsSet hero = new StatsSet();
                int charId = rset.getInt(Olympiad.CHAR_ID);
                hero.set(Olympiad.CHAR_NAME, getHeroName(con, charId));
                hero.set(Olympiad.CLASS_ID, getHeroClass(con, charId));
                hero.set(COUNT, rset.getInt(COUNT));
                hero.set(PLAYED, rset.getInt(PLAYED));
                hero.set(ACTIVE, rset.getInt(ACTIVE));
                HeroSetClanAndAlly(charId, hero);
                _heroes.put(charId, hero);
                OlympiadDiary.write(charId);
            }
            Close.SR(statement, rset);

            statement = con.prepareStatement(GET_ALL_HEROES);
            rset = statement.executeQuery();
            while (rset.next()) {
                StatsSet hero = new StatsSet();
                int charId = rset.getInt(Olympiad.CHAR_ID);
                hero.set(Olympiad.CHAR_NAME, getHeroName(con, charId));
                hero.set(Olympiad.CLASS_ID, getHeroClass(con, charId));
                hero.set(COUNT, rset.getInt(COUNT));
                hero.set(PLAYED, rset.getInt(PLAYED));
                hero.set(ACTIVE, rset.getInt(ACTIVE));
                HeroSetClanAndAlly(charId, hero);
                _completeHeroes.put(charId, hero);
            }
        } catch (SQLException e) {
            _log.warning("Hero System: Couldnt load Heroes");
        } finally {
            Close.CSR(con, statement, rset);
        }

        _log.info("Hero System: Loaded " + _heroes.size() + " Heroes.");
        _log.info("Hero System: Loaded " + _completeHeroes.size() + " all time Heroes.");
        OlympiadDiary.close();
    }

    private String getHeroName(Connect con, int objId) {
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            st = con.prepareStatement("SELECT char_name FROM `characters` WHERE `obj_Id` = ? LIMIT 0,1");
            st.setInt(1, objId);
            rset = st.executeQuery();
            if (rset.next()) {
                return rset.getString("char_name");
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] Hero System: , getHeroName() error: " + e);
        } finally {
            Close.SR(st, rset);
        }
        return "???";
    }

    private int getHeroClass(Connect con, int objId) {
        PreparedStatement st = null;
        ResultSet rset = null;
        try {
            st = con.prepareStatement("SELECT base_class FROM `characters` WHERE `obj_Id` = ? LIMIT 0,1");
            st.setInt(1, objId);
            rset = st.executeQuery();
            if (rset.next()) {
                return rset.getInt("base_class");
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] Hero System: , getHeroClass() error: " + e);
        } finally {
            Close.SR(st, rset);
        }
        return 0;
    }

    public Map<Integer, StatsSet> getHeroes() {
        return _heroes;
    }

    public synchronized void clearHeroes() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE heroes SET played = ?, active = ?");
            statement.setInt(1, 0);
            statement.setInt(2, 0);
            statement.execute();
        } catch (SQLException e) {
            _log.warning("Hero System: Couldnt clearHeroes");
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }

        if (!_heroes.isEmpty()) {
            for (StatsSet hero : _heroes.values()) {
                if (hero.getInteger(ACTIVE) == 0) {
                    continue;
                }

                String name = hero.getString(Olympiad.CHAR_NAME);

                L2PcInstance player = L2World.getInstance().getPlayer(name);

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

        _heroes.clear();
        OlympiadDiary.clear();
    }

    public synchronized boolean computeNewHeroes(FastList<StatsSet> newHeroes) {
        if (newHeroes.size() == 0) {
            return true;
        }

        Map<Integer, StatsSet> heroes = new FastMap<Integer, StatsSet>();//.setShared(true);
        boolean error = false;

        for (StatsSet hero : newHeroes) {
            int charId = hero.getInteger(Olympiad.CHAR_ID);

            if (_completeHeroes != null && _completeHeroes.containsKey(charId)) {
                StatsSet oldHero = _completeHeroes.get(charId);
                int count = oldHero.getInteger(COUNT);
                oldHero.set(COUNT, count + 1);
                oldHero.set(PLAYED, 1);
                oldHero.set(ACTIVE, 0);

                heroes.put(charId, oldHero);
            } else {
                StatsSet newHero = new StatsSet();
                newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
                newHero.set(Olympiad.CLASS_ID, hero.getInteger(Olympiad.CLASS_ID));
                newHero.set(COUNT, 1);
                newHero.set(PLAYED, 1);
                newHero.set(ACTIVE, 0);

                heroes.put(charId, newHero);
            }
        }

        _heroes.putAll(heroes);
        heroes.clear();

        updateHeroes(0);

        return error;
    }

    public void updateHeroes(int id) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("REPLACE INTO heroes VALUES (?,?,?,?)");

            for (Integer heroId : _heroes.keySet()) {
                if (id > 0 && heroId != id) {
                    continue;
                }
                StatsSet hero = _heroes.get(heroId);
                try {
                    statement.setInt(1, heroId);
                    statement.setInt(2, hero.getInteger(COUNT));
                    statement.setInt(3, hero.getInteger(PLAYED));
                    statement.setInt(4, hero.getInteger(ACTIVE));
                    statement.execute();
                    if (_completeHeroes != null && !_completeHeroes.containsKey(heroId)) {
                        HeroSetClanAndAlly(heroId, hero);
                        _completeHeroes.put(heroId, hero);
                    }
                } catch (SQLException e) {
                    _log.warning("Hero System: Couldnt update Hero: " + heroId);
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            _log.warning("Hero System: Couldnt update Heroes");
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    public boolean isHero(int id) {
        if (_heroes == null || _heroes.isEmpty()) {
            return false;
        }
        if (_heroes.containsKey(id) && _heroes.get(id).getInteger(ACTIVE) == 1) {
            return true;
        }
        return false;
    }

    public boolean isInactiveHero(int id) {
        if (_heroes == null || _heroes.isEmpty()) {
            return false;
        }
        if (_heroes.containsKey(id) && _heroes.get(id).getInteger(ACTIVE) == 0) {
            return true;
        }
        return false;
    }

    public void activateHero(L2PcInstance player) {
        StatsSet hero = _heroes.get(player.getObjectId());
        hero.set(ACTIVE, 1);
        _heroes.remove(player.getObjectId());
        _heroes.put(player.getObjectId(), hero);

        player.setHero(true);
        player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
        //player.updatePledgeClass();

        String heroclass = CharTemplateTable.getClassNameById(player.getBaseClass());
        if (player.getClan() != null) {
            Announcements.getInstance().announceToAll(player.getName() + " из клана " + player.getClan().getName() + " стал героем в классе " + heroclass + ". Поздравляем!");
            if (player.getClan().getLevel() >= 5) {
                player.getClan().addPoints(1000);
                player.getClan().broadcastMessageToOnlineMembers("Члена клана " + player.getName() + " стал героем. " + (Config.ALT_CLAN_REP_MUL > 1 ? ((int) 1000 * Config.ALT_CLAN_REP_MUL) : 1000) + " очков было добавлено к счету репутации Вашего клана.");
            }
        } else {
            Announcements.getInstance().announceToAll(player.getName() + " стал героем в классе " + heroclass + ". Поздравляем!");
        }

        player.broadcastUserInfo();
        updateHeroes(player.getObjectId());
        giveRewards(player, Config.ALT_HERO_REWARDS);
        OlympiadDiary.addRecord(player, "Получение геройства.");
    }

    private void giveRewards(L2PcInstance player, FastList<Config.EventReward> _rewards) {
        if (_rewards.isEmpty()) {
            return;
        }

        for (FastList.Node<Config.EventReward> k = _rewards.head(), endk = _rewards.tail(); (k = k.getNext()) != endk;) {
            Config.EventReward reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                player.addItem("Hero.reward", reward.id, reward.count, player, true);
            }
        }
    }

    public static void addSkills(L2PcInstance player) {
        for (L2Skill s : HeroSkillTable.getHeroSkills()) {
            player.addSkill(s, false);
        }
    }

    public static void removeSkills(L2PcInstance player) {
        for (L2Skill s : HeroSkillTable.getHeroSkills()) {
            player.removeSkill(s); //Just Remove skills from nonHero characters
        }
    }
}