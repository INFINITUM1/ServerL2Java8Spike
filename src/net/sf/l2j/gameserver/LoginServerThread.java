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
package net.sf.l2j.gameserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GameClient.GameClientState;
import net.sf.l2j.gameserver.network.gameserverpackets.AuthRequest;
import net.sf.l2j.gameserver.network.gameserverpackets.BlowFishKey;
import net.sf.l2j.gameserver.network.gameserverpackets.ChangeAccessLevel;
import net.sf.l2j.gameserver.network.gameserverpackets.GameServerBasePacket;
import net.sf.l2j.gameserver.network.gameserverpackets.PlayerAuthRequest;
import net.sf.l2j.gameserver.network.gameserverpackets.PlayerInGame;
import net.sf.l2j.gameserver.network.gameserverpackets.PlayerLogout;
import net.sf.l2j.gameserver.network.gameserverpackets.ServerStatus;
import net.sf.l2j.gameserver.network.gameserverpackets.SetHwid;
import net.sf.l2j.gameserver.network.gameserverpackets.SetLastHwid;
import net.sf.l2j.gameserver.network.gameserverpackets.SetNewPassword;
import net.sf.l2j.gameserver.network.gameserverpackets.SetNewEmail;
import net.sf.l2j.gameserver.network.gameserverpackets.WebStatAccountsRequest;
import net.sf.l2j.gameserver.network.loginserverpackets.AcceptPlayer;
import net.sf.l2j.gameserver.network.loginserverpackets.AuthResponse;
import net.sf.l2j.gameserver.network.loginserverpackets.InitLS;
import net.sf.l2j.gameserver.network.loginserverpackets.KickPlayer;
import net.sf.l2j.gameserver.network.loginserverpackets.LoginServerFail;
import net.sf.l2j.gameserver.network.loginserverpackets.PlayerAuthResponse;
import net.sf.l2j.gameserver.network.loginserverpackets.WebStatAccounts;
import net.sf.l2j.gameserver.network.serverpackets.AuthLoginFail;
import net.sf.l2j.gameserver.network.serverpackets.CharSelectInfo;
import net.sf.l2j.util.crypt.NewCrypt;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.TimeLogger;

public class LoginServerThread extends Thread {

    protected static final Logger _log = AbstractLogger.getLogger(LoginServerThread.class.getName());
    /**
     * The LoginServerThread singleton
     */
    private static LoginServerThread _instance;
    /**
     * {
     *
     * @see net.sf.l2j.loginserver.LoginServer#PROTOCOL_REV }
     */
    private static final int REVISION = 0x0102;
    private RSAPublicKey _publicKey;
    private String _hostname;
    private int _port;
    private int[] _gamePorts;
    private Socket _loginSocket;
    private InputStream _in;
    private OutputStream _out;
    /**
     * The BlowFish engine used to encrypt packets<br> It is first initialized
     * with a unified key:<br> "_;v.]05-31!|+-%xT!^[$\00"<br> <br> and then
     * after handshake, with a new key sent by<br> loginserver during the
     * handshake. This new key is stored<br> in {@link #_blowfishKey}
     */
    private NewCrypt _blowfish;
    private byte[] _blowfishKey;
    private byte[] _hexID;
    private boolean _acceptAlternate;
    private int _requestID;
    private int _serverID;
    private boolean _reserveHost;
    private int _maxPlayer;
    private Map<String, WaitingClient> _waitingClients;
    private Map<String, L2GameClient> _accountsInGameServer;
    private int _status;
    private String _serverName;
    private String _gameExternalHost;
    private String _gameInternalHost;

    public LoginServerThread() {
        super("LoginServerThread");
        _port = Config.GAME_SERVER_LOGIN_PORT;
        _gamePorts = Config.PORTS_GAME;
        _hostname = Config.GAME_SERVER_LOGIN_HOST;
        _hexID = Config.HEX_ID;
        if (_hexID == null) {
            _requestID = Config.REQUEST_ID;
            _hexID = generateHex(16);
        } else {
            _requestID = Config.SERVER_ID;
        }

        _acceptAlternate = Config.ACCEPT_ALTERNATE_ID;
        _reserveHost = Config.RESERVE_HOST_ON_LOGIN;
        _gameExternalHost = Config.EXTERNAL_HOSTNAME;
        _gameInternalHost = Config.INTERNAL_HOSTNAME;
        _waitingClients = new ConcurrentHashMap<String, WaitingClient>();
        _accountsInGameServer = new ConcurrentHashMap<String, L2GameClient>();
        _maxPlayer = Config.MAXIMUM_ONLINE_USERS;
    }

    public static LoginServerThread getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new LoginServerThread();
    }

    private boolean _active;
    private boolean _restartAttempt;

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                if (_restartAttempt) {
                    continue;
                }

                _restartAttempt = true;
            }
            int lengthHi = 0;
            int lengthLo = 0;
            int length = 0;
            boolean checksumOk = false;
            try {
                // Connection
                _log.info(TimeLogger.getLogTime() + "Connecting to login on " + _hostname + ":" + _port);
                _loginSocket = new Socket(_hostname, _port);
                _in = _loginSocket.getInputStream();
                _out = new BufferedOutputStream(_loginSocket.getOutputStream());

                //init Blowfish
                _blowfishKey = generateHex(40);
                _blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
                _active = true;
                _restartAttempt = false;
                while (true) {
                    lengthLo = _in.read();
                    lengthHi = _in.read();
                    length = lengthHi * 256 + lengthLo;

                    if (lengthHi < 0
                            || _loginSocket.isClosed()) {
                        _active = false;
                        _log.finer("LoginServerThread: Login terminated the connection.");
                        break;
                    }

                    byte[] incoming = new byte[length];
                    incoming[0] = (byte) lengthLo;
                    incoming[1] = (byte) lengthHi;

                    int receivedBytes = 0;
                    int newBytes = 0;
                    while (newBytes != -1 && receivedBytes < length - 2) {
                        newBytes = _in.read(incoming, 2, length - 2);
                        receivedBytes = receivedBytes + newBytes;
                    }

                    if (receivedBytes != length - 2) {
                        _log.warning("Incomplete Packet is sent to the server, closing connection.(LS)");
                        break;
                    }

                    byte[] decrypt = new byte[length - 2];
                    System.arraycopy(incoming, 2, decrypt, 0, decrypt.length);
                    // decrypt if we have a key
                    decrypt = _blowfish.decrypt(decrypt);
                    checksumOk = NewCrypt.verifyChecksum(decrypt);

                    if (!checksumOk) {
                        _log.warning("Incorrect packet checksum, ignoring packet (LS)");
                        break;
                    }

                    int packetType = decrypt[0] & 0xff;
                    //System.out.println("##packetType##" + packetType);
                    switch (packetType) {
                        case 00:
                            InitLS init = new InitLS(decrypt);
                            //i/f (Config.DEBUG) _log.info("Init received");
                            if (init.getRevision() != REVISION) {
                                //TODO: revision mismatch
                                _log.warning("/!\\ Revision mismatch between LS and GS /!\\");
                                break;
                            }
                            try {
                                KeyFactory kfac = KeyFactory.getInstance("RSA");
                                BigInteger modulus = new BigInteger(init.getRSAKey());
                                RSAPublicKeySpec kspec1 = new RSAPublicKeySpec(modulus, RSAKeyGenParameterSpec.F4);
                                _publicKey = (RSAPublicKey) kfac.generatePublic(kspec1);
                                //if (Config.DEBUG) _log.info("RSA key set up");
                            } catch (GeneralSecurityException e) {
                                _log.warning("Troubles while init the public key send by login");
                                break;
                            }
                            //send the blowfish key through the rsa encryption
                            sendPacket(new BlowFishKey(_blowfishKey, _publicKey));
                            //if (Config.DEBUG)_log.info("Sent new blowfish key");
                            //now, only accept paket with the new encryption
                            _blowfish = new NewCrypt(_blowfishKey);
                            //if (Config.DEBUG)_log.info("Changed blowfish key");
                            sendPacket(new AuthRequest(_requestID, _acceptAlternate, _hexID, _gameExternalHost, _gameInternalHost, _gamePorts, _reserveHost, _maxPlayer, Config.SERVER_SERIAL_KEY, Config.CATS_GUARD));
                            //if (Config.DEBUG)_log.info("Sent AuthRequest to login");
                            break;
                        case 01:
                            LoginServerFail lsf = new LoginServerFail(decrypt);
                            _log.info(TimeLogger.getLogTime() + "[ERROR] Registeration Failed: " + lsf.getReasonString());
                            if (lsf.getReason() == 8) {
                                System.exit(1);
                            }
                            // login will close the connection here
                            break;
                        case 03:
                            PlayerAuthResponse par = new PlayerAuthResponse(decrypt);
                            String account = par.getAccount();
                            WaitingClient wcToRemove = _waitingClients.get(account);
                            if (wcToRemove != null && wcToRemove.isConnected()) {
                                if (Config.CATS_GUARD) {
                                    if (par.isAuthed()) {
                                        //if (Config.DEBUG)_log.info("Login accepted player "+wcToRemove.account+" waited("+(GameTimeController.getGameTicks()-wcToRemove.timestamp)+"ms)");
                                        wcToRemove.gameClient.setAuthed(true);
                                        wcToRemove.gameClient.startPingTask();

                                        sendPacket(new PlayerInGame(par.getAccount()));
                                        wcToRemove.gameClient.setState(GameClientState.AUTHED);
                                        wcToRemove.gameClient.setSessionId(wcToRemove.session);
                                        CharSelectInfo cl = new CharSelectInfo(wcToRemove.account, wcToRemove.gameClient.getSessionId().playOkID1);
                                        wcToRemove.gameClient.sendPacket(cl);
                                        wcToRemove.gameClient.setCharSelection(cl.getCharInfo());
                                        //
                                        wcToRemove.gameClient.setHasEmail(par.hasEmail());
                                        wcToRemove.gameClient.setMyHWID(par.getHWID());
                                        wcToRemove.gameClient.setLastServerId(par.getServerId());
                                    } else {
                                        _log.warning(TimeLogger.getLogTime() + "session key is not correct. closing connection; account: " + account);
                                        wcToRemove.gameClient.getConnection().sendPacket(new AuthLoginFail(1));
                                        wcToRemove.gameClient.closeNow();
                                        Log.add(TimeLogger.getTime() + "# " + account + " " + par.getHWID(), "wrong_hwid");
                                    }
                                } else if (par.isAuthed() && wcToRemove.gameClient.acceptHWID(par.getHWID(), (Config.GAMEGUARD_ENABLED && Config.VS_HWID))) {
                                    wcToRemove.gameClient.setAuthed(true);
                                    wcToRemove.gameClient.startPingTask();
                                    //if (Config.DEBUG)_log.info("Login accepted player "+wcToRemove.account+" waited("+(GameTimeController.getGameTicks()-wcToRemove.timestamp)+"ms)");
                                    sendPacket(new PlayerInGame(par.getAccount()));
                                    wcToRemove.gameClient.setState(GameClientState.AUTHED);
                                    wcToRemove.gameClient.setSessionId(wcToRemove.session);
                                    CharSelectInfo cl = new CharSelectInfo(wcToRemove.account, wcToRemove.gameClient.getSessionId().playOkID1);
                                    wcToRemove.gameClient.sendPacket(cl);
                                    wcToRemove.gameClient.setCharSelection(cl.getCharInfo());
                                    //
                                    wcToRemove.gameClient.setHasEmail(par.hasEmail());
                                    wcToRemove.gameClient.setMyHWID(par.getHWID());
                                    wcToRemove.gameClient.setLastServerId(par.getServerId());
                                } else {
                                    _log.warning(TimeLogger.getLogTime() + "session key is not correct. closing connection; account: " + account);
                                    wcToRemove.gameClient.getConnection().sendPacket(new AuthLoginFail(1));
                                    wcToRemove.gameClient.closeNow();
                                    Log.add(TimeLogger.getTime() + "# " + account + " " + par.getHWID(), "wrong_hwid");
                                }
                            }
                            _waitingClients.remove(account);
                            break;
                        case 04:
                            KickPlayer kp = new KickPlayer(decrypt);
                            doKickPlayer(_accountsInGameServer.get(kp.getAccount()));
                            break;
                        case 06:
                            AuthResponse aresp = new AuthResponse(decrypt);
                            _serverID = aresp.getServerId();
                            _serverName = aresp.getServerName();
                            Config.saveHexid(_serverID, hexToString(_hexID));
                            //_log.info(TimeLogger.getLogTime()+"Registered on login as Server "+_serverID+" : "+_serverName);
                            for (int port : _gamePorts) {
                                _log.info(TimeLogger.getLogTime() + "#Server " + _serverName + " ready on " + _gameExternalHost + ":" + port);
                            }
                            ServerStatus st = new ServerStatus();
                            if (Config.SERVER_LIST_BRACKET) {
                                st.addAttribute(ServerStatus.SERVER_LIST_SQUARE_BRACKET, ServerStatus.ON);
                            } else {
                                st.addAttribute(ServerStatus.SERVER_LIST_SQUARE_BRACKET, ServerStatus.OFF);
                            }

                            if (Config.SERVER_LIST_CLOCK) {
                                st.addAttribute(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.ON);
                            } else {
                                st.addAttribute(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.OFF);
                            }

                            if (Config.SERVER_LIST_TESTSERVER) {
                                st.addAttribute(ServerStatus.TEST_SERVER, ServerStatus.ON);
                            } else {
                                st.addAttribute(ServerStatus.TEST_SERVER, ServerStatus.OFF);
                            }

                            if (Config.SERVER_GMONLY) {
                                st.addAttribute(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GM_ONLY);
                            } else {
                                st.addAttribute(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_AUTO);
                            }

                            sendPacket(st);
                            if (L2World.getInstance().getAllPlayersCount() > 0) {
                                FastList<String> pl = new FastList<String>();
                                for (L2PcInstance p : L2World.getInstance().getAllPlayers()) {
                                    if (p == null) {
                                        continue;
                                    }

                                    if (p.isFantome()) {
                                        continue;
                                    }

                                    if (p.getAccountName().equalsIgnoreCase("N/A")) {
                                        continue;
                                    }

                                    pl.add(p.getAccountName());
                                }
                                sendPacket(new PlayerInGame(pl));
                                pl.clear();
                                pl = null;
                            }
                            break;
                        case 07:
                            //System.out.println("##acceptPlayer#07#");
                            AcceptPlayer ap = new AcceptPlayer(decrypt);
                            acceptPlayer(ap.getIp());
                            break;
                        case 0xbe:
                            //System.out.println("##acceptPlayer#07#");
                            WebStatAccounts wsa = new WebStatAccounts(decrypt);
                            _totalAccs = wsa.getCount();
                            break;
                    }
                }
            } catch (UnknownHostException e) {
                //if (Config.DEBUG) e.printStackTrace();
            } catch (IOException e) {
                _active = false;
                _log.info(TimeLogger.getLogTime() + "Deconnected from Login, Trying to reconnect:");
                _log.info(e.toString());
            } finally {
                try {
                    _loginSocket.close();
                } catch (Exception e) {
                }
            }

            try {
                Thread.sleep(5000); // 5 seconds tempo.
            } catch (InterruptedException e) {
                //
            }
        }
    }

    private void acceptPlayer(String ip) {
        /*try {
         SelectorThread.getInstance().accept(ip);
         } catch (Exception e) {
         _log.warning("Error acceptPlayer");
         }*/
    }

    public void addWaitingClientAndSendRequest(String acc, L2GameClient client, SessionKey key) {
        _waitingClients.put(acc, new WaitingClient(acc, client, key));
        //PlayerAuthRequest par = new PlayerAuthRequest(acc, key);
        try {
            sendPacket(new PlayerAuthRequest(acc, key));
        } catch (IOException e) {
            _log.warning("Error while sending player auth request");
            //if (Config.DEBUG) e.printStackTrace();
        }
    }

    public WaitingClient addWaitingClient(L2GameClient client) {
        return _waitingClients.put(client.getAccountName(), new WaitingClient(client.getAccountName(), client, client.getSessionId()));
    }

    public void sendLogout(String account) {
        //PlayerLogout pl = new PlayerLogout(account);
        try {
            sendPacket(new PlayerLogout(account));
        } catch (IOException e) {
            _log.warning(TimeLogger.getLogTime() + "Error while sending logout packet to login. " + e);
        }
    }

    public void reConnect() {
        try {
            if (_loginSocket != null) {
                _loginSocket.close();
            }
        } catch (Exception ignored) {
        }
        try {
            _instance.interrupt();
        } catch (Exception ignored) {
        }

        _instance = new LoginServerThread();
        _instance.start();
    }

    public void addGameServerLogin(String account, L2GameClient client) {
        _accountsInGameServer.put(account, client);
    }

    public void sendAccessLevel(String account, int level) {
        //ChangeAccessLevel cal = new ChangeAccessLevel(account, level);
        try {
            sendPacket(new ChangeAccessLevel(account, level));
        } catch (IOException e) {
            //if (Config.DEBUG)
            //e.printStackTrace();
        }
    }

    public void setLastHwid(String account, String hwid) {
        //SetLastHwid cal = new SetLastHwid(account, hwid);
        try {
            sendPacket(new SetLastHwid(account, hwid));
        } catch (IOException e) {
            //if (Config.DEBUG)
            //e.printStackTrace();
        }
    }

    public void updateWebStatAccounts() {
        //WebStatAccountsRequest wsar = new WebStatAccountsRequest();
        try {
            sendPacket(new WebStatAccountsRequest());
        } catch (IOException e) {
            //if (Config.DEBUG)
            //e.printStackTrace();
        }
    }

    public void setHwid(String account, String hwid) {
        //SetHwid cal = new SetHwid(account, hwid);
        try {
            sendPacket(new SetHwid(account, hwid));
        } catch (IOException e) {
            //if (Config.DEBUG)
            //e.printStackTrace();
        }
    }

    public void setNewPassword(String account, String pwd) {
        //SetNewPassword snp = new SetNewPassword(account, pwd);
        try {
            sendPacket(new SetNewPassword(account, pwd));
        } catch (IOException e) {
            //if (Config.DEBUG)
            //e.printStackTrace();
        }
    }

    public void setNewEmail(String account, String email) {
        //SetNewPassword snp = new SetNewPassword(account, pwd);
        try {
            sendPacket(new SetNewEmail(account, email));
        } catch (IOException e) {
            //if (Config.DEBUG)
            //e.printStackTrace();
        }
    }

    private String hexToString(byte[] hex) {
        return new BigInteger(hex).toString(16);
    }

    public void doKickPlayer(L2GameClient client) {
        if (client == null) {
            return;
        }

        if (client.getActiveChar() != null) {
            client.getActiveChar().kick();
        }

        client.closeNow();
        LoginServerThread.getInstance().sendLogout(client.getAccountName());
    }

    public static byte[] generateHex(int size) {
        byte[] array = new byte[size];
        Rnd.nextBytes(array);
        //if (Config.DEBUG)_log.fine("Generated random String:  \""+array+"\"");
        return array;
    }

    public static void knockKnock(String ip) {
        System.out.println("###" + ip);
    }

    /**
     * @param sl
     * @throws IOException
     */
    public void sendPacket(GameServerBasePacket sl) throws IOException {
        if (!_active) {
            System.out.println("sendPacket error");
            return;
        }
        //try
        //{
        byte[] data = sl.getContent();
        NewCrypt.appendChecksum(data);
        //_log.finest("[S]\n"+Util.printData(data));
        //System.out.println("dddddd" + data);
        data = _blowfish.crypt(data);
        int len = data.length + 2;
        synchronized (_out) //avoids tow threads writing in the mean time
        {
            _out.write(len & 0xff);
            _out.write(len >> 8 & 0xff);
            _out.write(data);
            _out.flush();
        }
        //}
        //catch (Exception e)
        //{
        //	e.printStackTrace();
        //}
    }

    /**
     * @param maxPlayer The maxPlayer to set.
     */
    public void setMaxPlayer(int maxPlayer) {
        sendServerStatus(ServerStatus.MAX_PLAYERS, maxPlayer);
        _maxPlayer = maxPlayer;
    }

    /**
     * @return Returns the maxPlayer.
     */
    public int getMaxPlayer() {
        return _maxPlayer;
    }

    /**
     * @param server_gm_only
     */
    public void sendServerStatus(int id, int value) {
        ServerStatus ss = new ServerStatus();
        ss.addAttribute(id, value);
        try {
            sendPacket(ss);
        } catch (IOException e) {
            //if (Config.DEBUG) e.printStackTrace();
        }
    }

    /**
     * @return
     */
    public String getStatusString() {
        return ServerStatus.STATUS_STRING[_status];
    }

    /**
     * @return
     */
    public boolean isClockShown() {
        return Config.SERVER_LIST_CLOCK;
    }

    /**
     * @return
     */
    public boolean isBracketShown() {
        return Config.SERVER_LIST_BRACKET;
    }

    /**
     * @return Returns the serverName.
     */
    public String getServerName() {
        return _serverName;
    }

    public void setServerStatus(int status) {
        switch (status) {
            case ServerStatus.STATUS_AUTO:
                sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_AUTO);
                _status = status;
                break;
            case ServerStatus.STATUS_DOWN:
                sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_DOWN);
                _status = status;
                break;
            case ServerStatus.STATUS_FULL:
                sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_FULL);
                _status = status;
                break;
            case ServerStatus.STATUS_GM_ONLY:
                sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GM_ONLY);
                _status = status;
                break;
            case ServerStatus.STATUS_GOOD:
                sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GOOD);
                _status = status;
                break;
            case ServerStatus.STATUS_NORMAL:
                sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_NORMAL);
                _status = status;
                break;
            default:
                throw new IllegalArgumentException("Status does not exists:" + status);
        }
    }

    public static class SessionKey {

        public int playOkID1;
        public int playOkID2;
        public int loginOkID1;
        public int loginOkID2;
        public int clientKey;

        public SessionKey(int loginOK1, int loginOK2, int playOK1, int playOK2) {
            playOkID1 = playOK1;
            playOkID2 = playOK2;
            loginOkID1 = loginOK1;
            loginOkID2 = loginOK2;
        }

        @Override
        public String toString() {
            return "PlayOk: " + playOkID1 + " " + playOkID2 + " LoginOk:" + loginOkID1 + " " + loginOkID2;
        }
    }

    public static class WaitingClient {

        public int timestamp;
        public String account;
        public L2GameClient gameClient;
        public SessionKey session;

        public WaitingClient(String acc, L2GameClient client, SessionKey key) {
            account = acc;
            timestamp = GameTimeController.getGameTicks();
            gameClient = client;
            session = key;
        }

        private boolean isConnected() {
            if (gameClient == null) {
                return false;
            }
            return gameClient.isConnected();
        }
    }
    private int _totalAccs = 0;

    public int getTotalAccs() {
        return _totalAccs;
    }
    private static Map<String, L2GameClient> _authedClients = new ConcurrentHashMap<String, L2GameClient>();
    private static Map<String, L2GameClient> _logoutRoomClients = new ConcurrentHashMap<String, L2GameClient>();

    public static void checkClient(String cl, L2GameClient client) {
        //System.out.println("##checkClient#1## " + client.getAccountName());
        L2GameClient old = _authedClients.remove(cl);
        if (old != null/* && old.getConnectionStartTime() != client.getConnectionStartTime()*/) {
            //System.out.println("##checkClient#2## " + old.getAccountName());
            old.close();
        }
        //System.out.println("##checkClient#3## " + client.getAccountName());
        _authedClients.put(cl, client);
    }

    public static void putToLogoutRoom(String acc, L2GameClient client) {
        _logoutRoomClients.put(acc, client);
    }

    public static void checkLogoutRoom(String acc, L2GameClient client) {
        L2GameClient old = _logoutRoomClients.remove(acc);
        if (old != null && old.getConnectionStartTime() != client.getConnectionStartTime()) {
            //System.out.println("###2.1## " + old.getAccountName());
            old.close();
        }
        //System.out.println("###3## " + client.getAccountName());
        _logoutRoomClients.put(acc, client);
    }
}
