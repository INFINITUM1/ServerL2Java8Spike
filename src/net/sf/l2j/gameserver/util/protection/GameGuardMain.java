package net.sf.l2j.gameserver.util.protection;

import java.util.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.GameGuardQuery;
import net.sf.l2j.gameserver.network.serverpackets.GameGuardQueryEx;
import net.sf.l2j.util.TimeLogger;
import net.sf.l2j.util.Util;
import net.sf.l2j.util.log.AbstractLogger;

public class GameGuardMain extends GameGuard {

    private final static Logger _log = AbstractLogger.getLogger(GameGuardMain.class.getName());
    private static Map<L2GameClient, L2PcInstance> _clients = new ConcurrentHashMap<L2GameClient, L2PcInstance>();

    public GameGuardMain() {
    }
    private static GameGuardMain _instance = null;

    public static GameGuardMain load() {
        _instance = new GameGuardMain();
        return _instance;
    }

    @Override
    public void startSession(L2GameClient client) {
        _clients.put(client, client.getActiveChar());
    }

    @Override
    public void closeSession(L2GameClient client) {
        _clients.remove(client);
    }

    @Override
    public boolean checkGameGuardReply(L2GameClient client, int[] reply) {
        try {
            if ((reply[3] & 0x4) == 4) {
                client.punishClient();
                return false;
            }

            if (!acceptHwId(client, getHwid(reply[1]))) {
                client.punishClient();
                return false;
            }

            reply[3] = reply[3] & 0xFFFFFF00;
            client.getSessionId().clientKey = reply[0];

            if (Config.GAMEGUARD_KEY != reply[3]) {
                client.punishClient();
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean acceptHwId(L2GameClient client, String hwid) {
        if (client.getMyHWID().equalsIgnoreCase("none") || client.getMyHWID().equalsIgnoreCase(hwid)) {
            client.setHWID(hwid);
            return true;
        }

        return false;
    }

    private String getHwid(int hwid) {
        if (Config.VS_HWID) {
            return Util.md5(String.format("%X", hwid));
        }

        return "none";
    }

    @Override
    public void startCheckTask() {
        _log.info(TimeLogger.getLogTime() + "Game Guard: loaded.");
        if (Config.GAMEGUARD_INTERVAL > 0) {
            ThreadPoolManager.getInstance().scheduleGeneral(new GameGuardCheckTask(), Config.GAMEGUARD_INTERVAL);
            _log.info(TimeLogger.getLogTime() + "Game Guard Cron Task: initialized.");
        }
    }

    static class GameGuardCheckTask implements Runnable {

        GameGuardCheckTask() {
        }

        @Override
        public void run() {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        L2GameClient client = null;
                        L2PcInstance player = null;
                        for (Map.Entry<L2GameClient, L2PcInstance> entry : _clients.entrySet()) {
                            client = entry.getKey();
                            player = entry.getValue();
                            if (client == null || player == null) {
                                continue;
                            }

                            if (player.isDeleting() || player.isInOfflineMode()) {
                                continue;
                            }

                            if (!client.isAuthedGG()) {
                                client.punishClient();
                                continue;
                            }

                            if (Config.GAMEGUARD_ENABLED) {
                                client.sendPacket(new GameGuardQueryEx());
                            } else {
                                client.sendPacket(new GameGuardQuery());
                            }
                        }
                    } catch (Exception e) {
                    }
                    ThreadPoolManager.getInstance().scheduleGeneral(new GameGuardCheckTask(), Config.GAMEGUARD_INTERVAL);
                }
            }).start();
        }
    }
}
