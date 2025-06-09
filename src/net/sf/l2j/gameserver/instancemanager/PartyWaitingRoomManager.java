package net.sf.l2j.gameserver.instancemanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyRoomMembers;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchDetail;
import net.sf.l2j.util.log.AbstractLogger;

public class PartyWaitingRoomManager {

    protected static final Logger _log = AbstractLogger.getLogger(PartyWaitingRoomManager.class.getName());
    private static PartyWaitingRoomManager _instance;
    private static Map<Integer, ConcurrentLinkedQueue<WaitingRoom>> _rooms = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<WaitingRoom>>();
    private static ConcurrentLinkedQueue<L2PcInstance> _finders = new ConcurrentLinkedQueue<L2PcInstance>();

    public static class WaitingRoom {

        public int id;
        public L2PcInstance owner;
        public String leaderName;
        public String title;
        public int maxPlayers;
        public int minLvl;
        public int maxLvl;
        public int location;
        public int loot = 0;
        public ConcurrentLinkedQueue<L2PcInstance> players;

        public WaitingRoom(L2PcInstance player, String title, int maxPlayers, int minLvl, int maxLvl, int location) {
            this.id = IdFactory.getInstance().getNextId();
            this.owner = player;
            this.leaderName = player.getName();
            this.title = title;
            this.maxPlayers = maxPlayers;
            this.minLvl = minLvl;
            this.maxLvl = maxLvl;
            this.location = location;
            this.players = new ConcurrentLinkedQueue<L2PcInstance>();
            this.players.add(player);
        }

        public void addPlayer(L2PcInstance player) {
            this.players.add(player);
        }

        public void delPlayer(L2PcInstance player) {
            this.players.remove(player);
        }

        public boolean contains(L2PcInstance player) {
            return this.players.contains(player);
        }

        public void sayToPartyRoom(CreatureSay cs) {
            for (L2PcInstance member : players) {
                if (member == null) {
                    continue;
                }

                member.sendPacket(cs);
            }
        }
    }

    public static PartyWaitingRoomManager getInstance() {
        return _instance;
    }

    public PartyWaitingRoomManager() {
    }

    public static void init() {
        _instance = new PartyWaitingRoomManager();
        _instance.load();
    }

    private void load() {
        _rooms.put(-1, new ConcurrentLinkedQueue<WaitingRoom>());
        _rooms.put(-2, new ConcurrentLinkedQueue<WaitingRoom>());
        _rooms.put(100, new ConcurrentLinkedQueue<WaitingRoom>());
        for (int i = 1; i <= 15; i++) {
            _rooms.put(i, new ConcurrentLinkedQueue<WaitingRoom>());
        }
        /*
         * рядом со мной -2 все -1 толкин исланд 1 глудио 2 тер. темных эльфов 3
         * св. эльфы 4 дион 5 гиран 6 нейтр. зона 7 шутгарт 9 орен 10 хантер 11
         * инадрил 12 аден 13 руна 14 годдард 15 изменить 100
         */
    }

    public void registerRoom(L2PcInstance player, String title, int maxPlayers, int minLvl, int maxLvl, int location) {
        WaitingRoom new_room = new WaitingRoom(player, title, maxPlayers, minLvl, maxLvl, location);
        _rooms.get(location).add(new_room);
        player.setPartyRoom(new_room);
        player.sendPacket(new PartyMatchDetail(new_room));
        player.sendPacket(new ExPartyRoomMembers(player, new_room));
        _finders.remove(player);
    }

    public void joinRoom(L2PcInstance player, int roomId) {
        WaitingRoom room = getRoom(roomId);
        if (room == null) {
            return;
        }

        if (room.players.size() >= room.maxPlayers) {
            return;
        }

        room.addPlayer(player);
        player.setPartyRoom(room);

        refreshRoom(room);

        _finders.remove(player);
        player.setLFP(false);
        player.broadcastUserInfo();
    }

    public void exitRoom(L2PcInstance player, WaitingRoom room) {
        if (room == null) {
            return;
        }

        if (room.owner == null) {
            return;
        }

        if (!room.players.contains(player)) {
            return;
        }

        room.delPlayer(player);
        player.setPartyRoom(null);
        if (room.players.isEmpty()) {
            room = null;
            return;
        }
        if (room.owner.equals(player)) {
            room.owner = room.players.peek();
            room.leaderName = room.owner.getName();
        }

        refreshRoom(room);
    }

    public void refreshRoom(WaitingRoom room) {
        if (room == null) {
            return;
        }

        for (L2PcInstance member : room.players) {
            member.sendPacket(new PartyMatchDetail(room));
            member.sendPacket(new ExPartyRoomMembers(member, room));
        }
    }

    public WaitingRoom getRoom(int roomId) {
        for (Map.Entry<Integer, ConcurrentLinkedQueue<WaitingRoom>> entry : _rooms.entrySet()) {
            Integer territoryId = entry.getKey();
            ConcurrentLinkedQueue<WaitingRoom> rooms = entry.getValue();
            if (territoryId == null || rooms == null) {
                continue;
            }

            if (rooms.isEmpty()) {
                continue;
            }

            for (WaitingRoom room : rooms) {
                if (room == null) {
                    continue;
                }

                if (room.id == roomId) {
                    return room;
                }
            }
        }
        return null;
    }

    public ConcurrentLinkedQueue<WaitingRoom> getRooms(int levelType, int territoryId, ConcurrentLinkedQueue<WaitingRoom> rooms) {
        if (territoryId == -1) {
            int minLvl = levelType - 5;
            int maxLvl = levelType + 5;
            for (Map.Entry<Integer, ConcurrentLinkedQueue<WaitingRoom>> entry : _rooms.entrySet()) {
                Integer terrId = entry.getKey();
                ConcurrentLinkedQueue<WaitingRoom> temp = entry.getValue();
                if (terrId == null || temp == null) {
                    continue;
                }

                if (temp.isEmpty()) {
                    continue;
                }

                for (WaitingRoom room : temp) {
                    if (room == null) {
                        continue;
                    }

                    if (levelType == -1) {
                        rooms.add(room);
                    } else if (room.minLvl >= minLvl && room.maxLvl <= maxLvl) {
                        rooms.add(room);
                    }
                }
            }
        } else if (levelType == -1) {
            for (WaitingRoom room : _rooms.get(territoryId)) {
                if (room == null) {
                    continue;
                }

                rooms.add(room);
            }
        } else {
            int minLvl = levelType - 5;
            int maxLvl = levelType + 5;
            for (WaitingRoom room : _rooms.get(territoryId)) {
                if (room == null) {
                    continue;
                }

                if (room.minLvl >= minLvl && room.maxLvl <= maxLvl) {
                    rooms.add(room);
                }
            }
        }
        return rooms;
    }

    public void registerPlayer(L2PcInstance player) {
        _finders.add(player);
    }

    public void delPlayer(L2PcInstance player) {
        _finders.remove(player);
    }

    public ConcurrentLinkedQueue<L2PcInstance> getFinders(int page, int minLvl, int maxLvl, ConcurrentLinkedQueue<L2PcInstance> finders) {
        for (L2PcInstance player : _finders) {
            if (player == null) {
                continue;
            }

            if (player.getLevel() >= minLvl && player.getLevel() <= maxLvl) {
                finders.add(player);
            }
        }
        return finders;
    }
    /*
     * public static String intToLevels(int i) { return ((i >> 8 ) & 0xFF) + "-"
     * + ( i & 0xFF); }
     *
     * public static int levelsToInt(final String levels) { final String[]
     * addressBytes = addr.split("\\-");
     *
     * int ip = 0; for (int i = 0; i < 2; i++) { ip <<= 8; ip |=
     * Integer.parseInt(addressBytes[i]); } return ip; }
     */
}
