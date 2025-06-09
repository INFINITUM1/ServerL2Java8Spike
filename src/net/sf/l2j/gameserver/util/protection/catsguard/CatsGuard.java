package net.sf.l2j.gameserver.util.protection.catsguard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.nio.ByteBuffer;
import java.util.Map;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GameClient.IExReader;
import net.sf.l2j.gameserver.network.serverpackets.GameGuardQuery;
import net.sf.l2j.gameserver.network.serverpackets.GameGuardQueryEx;
import net.sf.l2j.util.Log;

public class CatsGuard {

    private static Logger _log = Logger.getLogger("CatsGuard");

    private class CatsGuardReader implements IExReader {

        private RC4 _crypt;
        private L2GameClient _client;
        private int _prevcode = 0;
        private byte[] buffer = new byte[4];
        private int _state;
        private boolean _checkChar;

        private CatsGuardReader(L2GameClient cl) {
            _state = 0;
            _client = cl;
        }

        private void setKey(int data[], String key) {
            //String key = "";
            for (int i = 0; i < 10; i++) {
                key += String.format("%X%X", data[1], _SERVER_KEY);
            }
            _crypt = new RC4(key, false);
            _state = 1;
        }

        @Override
        public int read(ByteBuffer buf, int opcode) {
            //int opcode = 0;
            if (_state == 0) {
                opcode = buf.get() & 0xff;
                if (opcode != 0xca) {
                    illegalAction(_client, _client.getActiveChar(), "Invalid opcode on pre-auth state");
                    return 0;
                }
            } else {
                if (buf.remaining() < 4) {
                    illegalAction(_client, _client.getActiveChar(), "Invalid block size on authed state");
                } else {
                    buf.get(buffer);
                    opcode = decryptPacket(buffer, 0, 1) & 0xff;
                }
            }
            return opcode;
        }

        private int decryptPacket(byte[] packet, int crc, int read_crc) {
            packet = _crypt.rc4(packet);
            crc = CRC16.calc(new byte[]{(byte) (_prevcode & 0xff), packet[1]});
            read_crc = (((packet[3] & 0xff) << 8) & 0xff00) | (packet[2] & 0xff);
            if (crc != read_crc) {
                illegalAction(_client, _client.getActiveChar(), "CRC error");
                return 0;
            }
            _prevcode = packet[1] & 0xff;
            return _prevcode;
        }

        @Override
        public void checkChar(L2PcInstance cha) {
            //System.out.println("###checkChar#1#");
            if (!_checkChar || cha == null) {
                return;
            }
            //System.out.println("###checkChar#2#");

            if (LOG_OPTION.contains("BANNED")) {
                Log.cats("catsguard", "BANNED HWID: " + cha.getFingerPrints());
            }
            cha.kick();
            //System.out.println("###checkChar#3#");
        }
    }
    private static CatsGuard _instance;

    public static CatsGuard getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new CatsGuard();
        _instance.load();
    }
    private Map<String, Integer> _connections = new ConcurrentHashMap<String, Integer>();
    private ConcurrentLinkedQueue<String> _bannedhwid = new ConcurrentLinkedQueue<String>();
    private static boolean ENABLED = false;
    private static int _SERVER_KEY = 7958915;
    private int MAX_SESSIONS;
    private String LOG_OPTION;
    private boolean ANNOUNCE_HACK;
    private String ON_HACK_ATTEMP;
    private boolean LOG_SESSIONS;
    private long _lastBanUpdate = 0;

    private void load() {
        _log.info("CatsGuard");
        try {
            if (_SERVER_KEY == 0) {
                return;
            }
            Properties p = new Properties();
            InputStream is = new FileInputStream(new File("./catsguard/catsguard.cfg"));
            p.load(is);
            is.close();

            ENABLED = Config.CATS_GUARD;//Boolean.parseBoolean(p.getProperty("Enabled", "False"));
            if (!ENABLED) {
                _log.info("CatsGuard: disabled");
                return;
            }

            LOG_OPTION = p.getProperty("LogOption", "NOSPS HACK");
            MAX_SESSIONS = Integer.parseInt(p.getProperty("MaxSessionsFromHWID", "-1"));
            ANNOUNCE_HACK = Boolean.parseBoolean(p.getProperty("AnnounceHackAttempt", "true"));
            ON_HACK_ATTEMP = p.getProperty("OnHackAttempt", "kick");
            LOG_SESSIONS = Boolean.parseBoolean(p.getProperty("LogSessions", "false"));

            File file = new File("./catsguard/catsguard.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                }
            }
            file = new File("./catsguard/audit.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                }
            }
            file = new File("./catsguard/banned_hwid.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                }
            }
            file = new File("./catsguard/wrong_hwid.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                }
            }

            loadBannedHwids();
            _log.info("CatsGuard: Ready");
        } catch (Exception e) {
            _log.warning("CatsGuard: Error while loading ./catsguard/catsguard.cfg");
            ENABLED = false;
        }
    }

    private void loadBannedHwids() {
        ConcurrentLinkedQueue<String> banned = new ConcurrentLinkedQueue<String>();
        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File bans = new File("./catsguard/banned_hwid.txt");
            if (!bans.exists()) {
                return;
            }
            _lastBanUpdate = bans.lastModified();

            fr = new FileReader(bans);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                banned.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        _bannedhwid.clear();
        _bannedhwid.addAll(banned);
        _log.info("CatsGuard: Loaded " + _bannedhwid.size() + " banned hwid(s)");
    }

    private void checkBansUpdate() {
        try {
            File bans = new File("./catsguard/banned_hwid.txt");
            if (!bans.exists()) {
                return;
            }
            if (bans.lastModified() != _lastBanUpdate) {
                loadBannedHwids();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isEnabled() {
        return ENABLED;
    }

    public void ban(L2PcInstance player) {
        //ban(player.getClient().getHWid());
        ban(player.getHWID(), player.getIP(), player.getAccountName());
    }

    public void ban(String hwid, String ip, String acc) {
        if (_bannedhwid.contains(hwid)) {
            return;
        }

        _bannedhwid.add(hwid);
        Log.banHWID(hwid, ip, acc, "catsguard/banned_hwid.txt");
        /*try {
         Connect con = L2DatabaseFactory.getInstance().getConnection();
         PreparedStatement stm = con.prepareStatement("insert into banned_hwid values(?)");
         stm.setString(1, hwid);
         stm.execute();
         stm.close();
         con.close();
         } catch (SQLException e) {
         _log.warning("CatsGuard: Unable to store banned hwid");
         }*/
    }

    private void illegalAction(L2GameClient cl, L2PcInstance player, String reason) {
        if (cl.getActiveChar() != null && ANNOUNCE_HACK) {
            Announcements.getInstance().announceToAll("Игрок " + player.getName() + " использует недопустимое ПО!");
        }
        if (ON_HACK_ATTEMP.equals("hwidban") && cl.getHWid() != null) {
            ban(cl.getHWid(), cl.getIpAddr(), cl.getAccountName());
            player.setAccountAccesslevel(-100);
        } else if (player != null) {
            if (ON_HACK_ATTEMP.equals("jail")) {
                player.setInJail(true, 99999);
            } else if (ON_HACK_ATTEMP.equals("ban")) {
                player.setAccountAccesslevel(-100);
            }
        }
        //_log.info("CatsGuard: Client " + cl + " use illegal software and will " + ON_HACK_ATTEMP + "ed. Reason: " + reason);
        Log.cats("audit", "ILLEGAL SOFTWARE: (" + reason + ")" + (player == null ? cl.getFingerPrints() : cl.getActiveChar().getFingerPrints()));
        cl.close(true);
    }

    public void unban(String hwid) {
        _bannedhwid.remove(hwid);
    }

    public void initSession(L2GameClient cl) {
        checkBansUpdate();
        //cl.sendPacket(new GameGuardQuery());
        if (Config.GAMEGUARD_ENABLED) {
            cl.sendPacket(new GameGuardQueryEx());
        } else {
            cl.sendPacket(new GameGuardQuery());
        }
        cl._reader = new CatsGuardReader(cl);
    }

    public void doneSession(L2GameClient cl) {
        if (MAX_SESSIONS > 0 && cl.getHWid() != null) {
            if (_connections.containsKey(cl.getHWid())) {
                int nwnd = _connections.get(cl.getHWid());
                if (nwnd == 0) {
                    _connections.remove(cl.getHWid());
                } else {
                    _connections.put(cl.getHWid(), --nwnd);
                }
            }
        }

        cl._reader = null;
    }

    public void initSession(L2GameClient cl, int[] data) {
        //System.out.println("##initSession(L2GameClient cl, int[] data)#1#");
        if (data[0] != _SERVER_KEY) {
            if (LOG_OPTION.contains("NOPROTECT")) {
                Log.cats("audit", "Client " + cl.getFingerPrints() + " try to log with no CatsGuard");
            }
            cl.close(true);
            return;
        }
        //System.out.println("##initSession(L2GameClient cl, int[] data)#2#");
        String hwid = String.format("%x", data[3]);
        if (cl._reader == null) {
            if (LOG_OPTION.contains("HACK")) {
                Log.cats("audit", "Client " + cl.getFingerPrints() + " has no pre-authed state");
            }
            cl.close(true);
            return;
        }
        cl.setHWID(hwid);

        if (Config.VS_HWID && !cl.acceptHWID(cl.getMyHWID(), false)) {
            Log.cats("wrong_hwid", "# " + cl.getFingerPrints() + " " + cl.getMyHWID());
            cl.close(true);
            return;
        }

        //System.out.println("##initSession(L2GameClient cl, int[] data)#3#");
        if (_bannedhwid.contains(hwid)) {
            ((CatsGuardReader) cl._reader)._checkChar = true;
        }

        if (MAX_SESSIONS > 0) {
            if (!_connections.containsKey(hwid)) {
                _connections.put(hwid, 0);
            }
            int nwindow = _connections.get(hwid);
            //System.out.println("##initSession(L2GameClient cl, int[] data)#4#");
            if (MAX_SESSIONS > 0 && ++nwindow > MAX_SESSIONS) {
                if (LOG_OPTION.contains("SESSIONS")) {
                    Log.cats("audit", "To many sessions from hwid " + hwid);
                }
                cl.close(true);
                return;
            }
            _connections.put(hwid, nwindow);
        }

        ((CatsGuardReader) cl._reader).setKey(data, "");
        //System.out.println("##initSession(L2GameClient cl, int[] data)#8#");
        if (LOG_SESSIONS) {
            Log.cats("catsguard", "Client " + cl.getFingerPrints() + " connected.");
        }

    }
}
