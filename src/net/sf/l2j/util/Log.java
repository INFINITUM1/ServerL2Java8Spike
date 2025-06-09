package net.sf.l2j.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

public class Log {

    private static final Logger _log = AbstractLogger.getLogger(Log.class.getName());

    public static void add(String text, String cat) {
        add(text, cat, "log/");
    }

    public static void add(String text, String cat, String path) {
        Lock print = new ReentrantLock();
        print.lock();
        try {
            new File(path).mkdirs();

            File file = new File(path + "" + (cat != null ? cat : "_all") + ".txt");

            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    _log.warning("saving " + (cat != null ? cat : "all") + " log failed, can't create file: " + e);
                    //return;
                }
            }

            FileWriter save = null;
            TextBuilder msgb = new TextBuilder();
            try {
                save = new FileWriter(file, true);

                msgb.append(text + "\n");
                save.write(msgb.toString());
            } catch (IOException e) {
                _log.warning("saving " + (cat != null ? cat : "all") + " log failed: " + e);
                e.printStackTrace();
            } finally {
                try {
                    if (save != null) {
                        save.close();
                    }
                    msgb.clear();
                    msgb = null;
                    save = null;
                } catch (Exception e1) {
                }
            }
        } catch (Exception e) {
            //
        } finally {
            print.unlock();
        }
    }

    // лог доната
    public static void addDonate(L2PcInstance player, String action, int price) {
        Date date = new Date();
        SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timef = new SimpleDateFormat("HH:mm:ss");

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("INSERT INTO `zz_donate_log` (`id`,`date`,`time`,`login`,`name`,`action`,`payment`) VALUES (NULL,?,?,?,?,?,?)");
            statement.setString(1, datef.format(date).toString());
            statement.setString(2, timef.format(date).toString());
            statement.setString(3, player.getAccountName());
            statement.setString(4, player.getName());
            statement.setString(5, action);
            statement.setInt(6, price);
            statement.execute();
        } catch (final Exception e) {
            _log.warning("Donate: logAction() error: " + e);
        } finally {
            Close.CS(con, statement);
        }
    }
    public static final int TRADE = 1;
    public static final int WAREHOUSE = 2;
    public static final int MULTISELL = 3;
    public static final int PRIVATE_STORE = 4;
    public static final int SHOP = 5;
    public static final int PICKUP = 6;
    public static final int DIEDROP = 7;
    private static File item_trade = null;
    private static File item_wh = null;
    private static File item_ms = null;
    private static File item_ps = null;
    private static File item_shop = null;
    private static File item_pickup = null;
    private static File item_didrop = null;
    private static File item_all = null;
    private static File chat_whisper = null;
    private static File chat_shout = null;
    private static File chat_clan = null;
    private static File chat_ally = null;
    private static File chat_trade = null;
    private static File chat_party = null;
    private static File chat_hero = null;
    private static File chat_all = null;

    public static void init() {
        initChat();
        initItems();
    }

    private static void initChat() {
        if (!Config.LOG_CHAT) {
            return;
        }
        new File("log/chat/").mkdirs();
        Date date = new Date();
        SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd_HH-mm_");
        String time = datef.format(date).toString();
        chat_whisper = new File("log/chat/" + time + "whisper.txt");
        if (!chat_whisper.exists()) {
            try {
                chat_whisper.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create chat_whisper: " + e);
            }
        }
        chat_shout = new File("log/chat/" + time + "shout.txt");
        if (!chat_shout.exists()) {
            try {
                chat_shout.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create chat_shout: " + e);
            }
        }
        chat_clan = new File("log/chat/" + time + "clan.txt");
        if (!chat_clan.exists()) {
            try {
                chat_clan.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create chat_clan: " + e);
            }
        }
        chat_ally = new File("log/chat/" + time + "ally.txt");
        if (!chat_ally.exists()) {
            try {
                chat_ally.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create chat_ally: " + e);
            }
        }
        chat_trade = new File("log/chat/" + time + "trade.txt");
        if (!chat_trade.exists()) {
            try {
                chat_trade.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create chat_trade: " + e);
            }
        }
        chat_party = new File("log/chat/" + time + "party.txt");
        if (!chat_party.exists()) {
            try {
                chat_party.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create chat_party: " + e);
            }
        }
        chat_hero = new File("log/chat/" + time + "hero.txt");
        if (!chat_hero.exists()) {
            try {
                chat_hero.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create chat_hero: " + e);
            }
        }
        chat_all = new File("log/chat/" + time + "all.txt");
        if (!chat_all.exists()) {
            try {
                chat_all.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create chat_all: " + e);
            }
        }
    }

    private static void initItems() {
        if (!Config.LOG_ITEMS) {
            return;
        }
        new File("log/items/").mkdirs();
        Date date = new Date();
        SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd_HH-mm_");
        String time = datef.format(date).toString();
        item_trade = new File("log/items/" + time + "trade.txt");
        if (!item_trade.exists()) {
            try {
                item_trade.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create item_trade: " + e);
            }
        }
        item_wh = new File("log/items/" + time + "warehouse.txt");
        if (!item_wh.exists()) {
            try {
                item_wh.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create item_wh: " + e);
            }
        }
        item_ms = new File("log/items/" + time + "multisell.txt");
        if (!item_ms.exists()) {
            try {
                item_ms.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create item_ms: " + e);
            }
        }
        item_ps = new File("log/items/" + time + "private_store.txt");
        if (!item_ps.exists()) {
            try {
                item_ps.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create item_ps: " + e);
            }
        }
        item_shop = new File("log/items/" + time + "shop.txt");
        if (!item_shop.exists()) {
            try {
                item_shop.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create item_shop: " + e);
            }
        }
        item_pickup = new File("log/items/" + time + "pickup.txt");
        if (!item_pickup.exists()) {
            try {
                item_pickup.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create item_pickup: " + e);
            }
        }
        item_didrop = new File("log/items/" + time + "diedrop.txt");
        if (!item_didrop.exists()) {
            try {
                item_didrop.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create item_didrop: " + e);
            }
        }
        item_all = new File("log/items/" + time + "all.txt");
        if (!item_all.exists()) {
            try {
                item_all.createNewFile();
            } catch (IOException e) {
                _log.warning("Log [ERROR], can't create item_all: " + e);
            }
        }
    }
    private static Lock itemLock = new ReentrantLock();

    public static void item(String data, int type) {
        if (!Config.LOG_ITEMS) {
            return;
        }

        itemLock.lock();
        try {
            File file = null;
            FileWriter save = null;
            //TextBuilder msgb = new TextBuilder();
            try {
                switch (type) {
                    case TRADE:
                        file = item_trade;
                        break;
                    case WAREHOUSE:
                        file = item_wh;
                        break;
                    case MULTISELL:
                        file = item_ms;
                        break;
                    case PRIVATE_STORE:
                        file = item_ps;
                        break;
                    case SHOP:
                        file = item_shop;
                        break;
                    case PICKUP:
                        file = item_pickup;
                        break;
                    case DIEDROP:
                        file = item_didrop;
                        break;
                }
                save = new FileWriter(file, true);
                //msgb.append(data + "\n");
                save.write(data);

                if (save != null) {
                    save.close();
                }

                file = item_all;
                save = new FileWriter(file, true);
                //msgb.append(data + "\n");
                save.write(data);
            } catch (IOException e) {
                _log.warning("Log [ERROR], saving " + file.getName() + " log failed: " + e);
                e.printStackTrace();
            } finally {
                try {
                    if (save != null) {
                        save.close();
                    }
                    //msgb.clear();
                    //msgb = null;
                    save = null;
                } catch (Exception e1) {
                }
            }
        } catch (Exception e) {
            //
        } finally {
            itemLock.unlock();
        }
    }
    private static Lock chatLock = new ReentrantLock();

    public static void chat(String data, int type) {
        if (!Config.LOG_CHAT) {
            return;
        }

        chatLock.lock();
        try {
            File file = null;
            FileWriter save = null;
            String ctype = "[All]";
            try {
                switch (type) {
                    case 1:
                        file = chat_shout;
                        ctype = "[Shout]";
                        break;
                    case 2:
                        file = chat_whisper;
                        ctype = "[Whisper]";
                        break;
                    case 3:
                        file = chat_party;
                        ctype = "[Party]";
                        break;
                    case 4:
                        file = chat_clan;
                        ctype = "[Clan]";
                        break;
                    case 8:
                        file = chat_trade;
                        ctype = "[Trade]";
                        break;
                    case 9:
                        file = chat_ally;
                        ctype = "[Alliance]";
                        break;
                    case 17:
                        file = chat_hero;
                        ctype = "[Hero]";
                        break;
                }
                if (file != null) {
                    save = new FileWriter(file, true);
                    //msgb.append(data + "\n");
                    save.write(getTime() + data);
                    if (save != null) {
                        save.close();
                    }
                }

                file = chat_all;
                save = new FileWriter(file, true);
                save.write(getTime() + ctype + " " + data);
            } catch (IOException e) {
                _log.warning("Log [ERROR], saving " + file.getName() + " log failed: " + e);
                e.printStackTrace();
            } finally {
                try {
                    if (save != null) {
                        save.close();
                    }
                    //msgb.clear();
                    //msgb = null;
                    save = null;
                } catch (Exception e1) {
                }
            }
        } catch (Exception e) {
            //
        } finally {
            chatLock.unlock();
        }
    }

    public static void banHWID(String hwid, String ip, String account) {
        banHWID(hwid, ip, account, "lameguard/banned_hwid.txt");
    }

    public static void banHWID(String hwid, String ip, String account, String destfile) {
        //105a7e70825ed03f8bf64a40ce1c1196	# added : 11.10.23 21:41:12 ip : 178.46.86.32 account : medvedd reason: IG bot
        Lock print = new ReentrantLock();
        print.lock();
        try {
            File file = new File(destfile);
            FileWriter save = null;
            try {
                save = new FileWriter(file, true);
                save.write(hwid + "	#added : " + getTime() + " ip: " + ip + "; account: " + account + "; reason: admin_hwidban\n");
            } catch (IOException e) {
                _log.warning("saving banHWID " + hwid + " failed: " + e);
                e.printStackTrace();
            } finally {
                try {
                    if (save != null) {
                        save.close();
                    }
                    save = null;
                } catch (Exception e1) {
                }
            }
        } catch (Exception e) {
            //
        } finally {
            print.unlock();
        }
    }

    public static void addToPath(File file, String text) {
        Lock print = new ReentrantLock();
        print.lock();
        try {
            FileWriter save = null;
            TextBuilder msgb = new TextBuilder();
            try {
                save = new FileWriter(file, true);

                msgb.append(text + "\n");
                save.write(msgb.toString());
            } catch (IOException e) {
                _log.warning("saving " + file.getName() + " failed: " + e);
                e.printStackTrace();
            } finally {
                try {
                    if (save != null) {
                        save.close();
                    }
                    msgb.clear();
                    msgb = null;
                    save = null;
                } catch (Exception e1) {
                }
            }
        } catch (Exception e) {
            //
        } finally {
            print.unlock();
        }
    }

    public static String getTime() {
        Date date = new Date();
        SimpleDateFormat datef = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SS, ");
        return datef.format(date).toString();
    }

    public static void donate(String type, String text) {
        add(text, "donate/" + type);
    }

    public static void cats(String type, String text) {
        add(text, type, "catsguard/");
    }
}
