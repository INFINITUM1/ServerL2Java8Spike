package net.sf.l2j.gameserver.model.entity.olympiad;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.util.Log;

public class OlympiadTeam {

    private OlympiadGame _game;
    private FastList<TeamMember> _members;
    private String _name = "";
    private int _side;
    private double _damage;

    public OlympiadTeam(OlympiadGame game, int side) {
        _game = game;
        _side = side;
        _members = new FastList<TeamMember>();
    }

    public void addMember(int obj_id) {
        String player_name = "";
        L2PcInstance player = L2World.getInstance().getPlayer(obj_id);
        if (player != null) {
            player_name = player.getName();
        } else {
            StatsSet noble = Olympiad._nobles.get(Integer.valueOf(obj_id));
            if (noble != null) {
                player_name = noble.getString(Olympiad.CHAR_NAME, "");
            }
        }

        _members.add(new TeamMember(obj_id, player_name, _game, _side));

        switch (_game.getType()) {
            case CLASSED:
            case NON_CLASSED:
                _name = player_name;
                break;
        }
    }

    public void addDamage(double damage) {
        _damage += damage;
    }

    public double getDamage() {
        return _damage;
    }

    public String getName() {
        return _name;
    }

    public void portPlayersToArena() {
        for (TeamMember member : _members) {
            member.portPlayerToArena();
            if (Config.ALT_OLY_RELOAD_SKILLS) {
                member.reloadSkills();
            }
        }
    }

    public void portPlayersBack() {
        for (TeamMember member : _members) {
            member.portPlayerBack();
            if (Config.ALT_OLY_RELOAD_SKILLS) {
                member.reloadSkills();
            }
        }
    }

    public void setPvpArena(boolean f) {
        for (TeamMember member : _members) {
            member.setPvpArena(f);
        }
    }

    public void preFightRestore() {
        for (TeamMember member : _members) {
            member.preFightRestore();
        }
    }

    public void preparePlayers() {
        for (TeamMember member : _members) {
            member.preparePlayer();
        }

        if (_members.size() <= 1) {
            return;
        }

        FastList<L2PcInstance> list = new FastList<L2PcInstance>();
        for (TeamMember member : _members) {
            L2PcInstance player = member.getPlayer();
            if (player != null) {
                list.add(player);
                if (player.getParty() != null) {
                    player.getParty().oustPartyMember(player);
                }
            }
        }
    }

    public void takePointsForCrash() {
        for (TeamMember member : _members) {
            member.takePointsForCrash();
        }
    }

    public boolean checkPlayers() {
        for (TeamMember member : _members) {
            if (member.checkPlayer()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllDead() {
        for (TeamMember member : _members) {
            if (!member.isDead() && member.checkPlayer()) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(int objId) {
        for (TeamMember member : _members) {
            if (member.getObjId() == objId) {
                return true;
            }
        }
        return false;
    }

    public FastList<L2PcInstance> getPlayers() {
        FastList<L2PcInstance> players = new FastList<L2PcInstance>();
        for (TeamMember member : _members) {
            L2PcInstance player = member.getPlayer();
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    public FastList<TeamMember> getMembers() {
        return _members;
    }

    public void broadcast(L2GameServerPacket p) {
        for (TeamMember member : _members) {
            L2PcInstance player = member.getPlayer();
            if (player != null) {
                player.sendPacket(p);
            }
        }
    }

    public void broadcastInfo() {
        for (TeamMember member : _members) {
            L2PcInstance player = member.getPlayer();
            if (player != null) {
                player.broadcastPacket(new ExOlympiadUserInfo(player));
            }
        }
    }

    public boolean logout(L2PcInstance player) {
        if (player != null) {
            for (TeamMember member : _members) {
                L2PcInstance pl = member.getPlayer();
                if (pl != null && pl == player) {
                    member.logout();
                }
            }
        }
        return checkPlayers();
    }

    public boolean doDie(L2PcInstance player) {
        if (player != null) {
            for (TeamMember member : _members) {
                L2PcInstance pl = member.getPlayer();
                if (pl != null && pl == player) {
                    member.doDie();
                }
            }
        }
        return isAllDead();
    }

    public void winGame(OlympiadTeam looseTeam) {
        int pointDiff = 0;
        for (int i = 0; i < _members.size(); i++) {
            try {
                pointDiff += transferPoints(looseTeam.getMembers().get(i).getStat(), getMembers().get(i).getStat());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (L2PcInstance player : getPlayers()) {
            try {
                rewardPlayer(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        _game.broadcastPacket(SystemMessage.id(SystemMessageId.S1_HAS_WON_THE_GAME).addString(getName()), true, true);
        _game.broadcastPacket(SystemMessage.id(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS).addString(getName()).addNumber(pointDiff), true, false);
        _game.broadcastPacket(SystemMessage.id(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS).addString(looseTeam.getName()).addNumber(pointDiff), true, false);
        Log.add("Olympiad Result: " + getName() + " vs " + looseTeam.getName() + " ... (" + (int) _damage + " vs " + (int) looseTeam.getDamage() + ") " + getName() + " win " + pointDiff + " points", "olympiad");
    }

    private void rewardPlayer(L2PcInstance player) {
        player.getInventory().addItem("Olympiad", 6651, _game.getType().getReward(), player, null);
        player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(6651).addNumber(_game.getType().getReward()));

        if (Config.OLY_ALT_REWARD) {
            switch (Config.OLY_ALT_REWARD_TYPE) {
                case 1:
                    player.addItem("OlyRewardWin", Config.OLY_ALT_REWARD_ITEM.id, Config.OLY_ALT_REWARD_ITEM.count, player, true);
                    break;
                case 2:
                    player.addItem("OlyRewardWinX2", Config.OLY_ALT_REWARD_ITEM.id, Config.OLY_ALT_REWARD_ITEM.count * 2, player, true);
                    break;
            }
        }
    }

    public void tie(OlympiadTeam otherTeam) {
        for (int i = 0; i < _members.size(); i++) {
            try {
                StatsSet stat1 = getMembers().get(i).getStat();
                StatsSet stat2 = otherTeam.getMembers().get(i).getStat();
                stat1.set(Olympiad.POINTS, stat1.getInteger(Olympiad.POINTS) - 2);
                stat2.set(Olympiad.POINTS, stat2.getInteger(Olympiad.POINTS) - 2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        _game.broadcastPacket(Static.THE_GAME_ENDED_IN_A_TIE, true, true);

        Log.add("Olympiad Result: " + getName() + " vs " + otherTeam.getName() + " ... tie", "olympiad");
    }

    private int transferPoints(StatsSet from, StatsSet to) {
        int fromPoints = from.getInteger(Olympiad.POINTS);
        int fromLoose = from.getInteger(Olympiad.COMP_LOOSE);
        int fromPlayed = from.getInteger(Olympiad.COMP_DONE);

        int toPoints = to.getInteger(Olympiad.POINTS);
        int toWin = to.getInteger(Olympiad.COMP_WIN);
        int toPlayed = to.getInteger(Olympiad.COMP_DONE);

        int pointDiff = Math.max(1, Math.min(fromPoints, toPoints) / _game.getType().getLooseMult());
        pointDiff = pointDiff > OlympiadGame.MAX_POINTS_LOOSE ? OlympiadGame.MAX_POINTS_LOOSE : pointDiff;

        from.set(Olympiad.POINTS, fromPoints - pointDiff);
        from.set(Olympiad.COMP_LOOSE, fromLoose + 1);
        from.set(Olympiad.COMP_DONE, fromPlayed + 1);

        to.set(Olympiad.POINTS, toPoints + pointDiff);
        to.set(Olympiad.COMP_WIN, toWin + 1);
        to.set(Olympiad.COMP_DONE, toPlayed + 1);
        return pointDiff;
    }

    public void saveNobleData() {
        for (TeamMember member : _members) {
            member.saveNobleData();
        }
    }
}