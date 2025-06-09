/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.l2j.gameserver.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;

/**
 *
 * @author Администратор
 */
public class AntiFarm {

    public static boolean FARM_DELAY;
    public static final FastList<Integer> FARM_DELAY_MOBS = new FastList<>();
    public static int FARM_DELAY_INTERVAL;
    public static int FARM_CHECK_TYPE;
    public static int FARM_TRYES_TELE;
    public static Location FARM_TRYES_LOC;
    public static String bypass = "farmp_pdelay";

    public static void init() {
        try {
            Properties altSettings = new Properties();
            InputStream is = new FileInputStream(new File("./config/npc.cfg"));
            altSettings.load(is);
            is.close();

            String[] propertySplit = null;
            FARM_DELAY = Boolean.parseBoolean(altSettings.getProperty("FarmDelay", "False"));
            if (FARM_DELAY) {
                String farmMobs = altSettings.getProperty("FarmDelayMobs", "All");
                if (!farmMobs.equalsIgnoreCase("All")) {
                    propertySplit = farmMobs.split(",");
                    if (propertySplit.length > 0) {
                        for (String npc_id : propertySplit) {
                            FARM_DELAY_MOBS.add(Integer.parseInt(npc_id));
                        }
                    }
                }
            }

            FARM_DELAY_INTERVAL = Integer.parseInt(altSettings.getProperty("FarmDelayInterval", "50"));
            FARM_CHECK_TYPE = Integer.parseInt(altSettings.getProperty("FarmDelayType", "0"));
            FARM_TRYES_TELE = Integer.parseInt(altSettings.getProperty("FarmTryesTp", "0"));
            propertySplit = altSettings.getProperty("FarmTryesLoc", "0,0,0").split(",");
            FARM_TRYES_LOC = new Location(Integer.parseInt(propertySplit[0]), Integer.parseInt(propertySplit[1]), Integer.parseInt(propertySplit[2]));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to Load ./config/npc.cfg File.");
        }
    }
    private static final Map<Integer, Integer> _farmDelays = new ConcurrentHashMap<>();

    public static class FarmDelay {

        private int _farmDelay = 0;
        private int _farmLesson = 0;
        private int _farmTryes = 0;
        private L2PcInstance player;

        public FarmDelay(L2PcInstance player) {
            this.player = player;
        }

        public void showPenalty() {
            _farmDelay = Rnd.get(99900, 99921);
            _farmLesson = _farmDelay;
            NpcHtmlMessage html = NpcHtmlMessage.id(0);
            switch (FARM_CHECK_TYPE) {
                case 0:
                    TextBuilder tb = new TextBuilder("<br>");
                    for (int i = (Rnd.get(30)); i > 0; i--) {
                        tb.append("<br>");
                    }
                    html.setHtml("<html><body><font color=\"FF6600\">!Активирован штраф на награду с мобов!</font><br>Нажмите на кнопку для снятия штрафа!<br> <table width=\"" + Rnd.get(40, 300) + "\"><tr><td align=\"right\">" + tb.toString() + "<button value=\"Продолжить\" action=\"bypass -h " + bypass + " " + _farmDelay + "\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></body></html>");
                    tb.clear();
                    tb = null;
                    break;
                case 1:
                    html.setHtml("<html><body><font color=\"FF6600\">!Активирован штраф на награду с мобов!</font><br>Введите этот код,<br>=== <font color=LEVEL>" + _farmDelay + "</font> ===<br> для снятия штрафа: <br><edit var=\"pwd\" width=60 length=\"5\"><br><br><button value=\"Ok\" action=\"bypass -h " + bypass + " $pwd\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></body></html>");
                    break;
                case 2:
                    int a = Rnd.get(100);
                    int b = Rnd.get(100);
                    _farmLesson = a + b;
                    html.setHtml("<html><body><font color=\"FF6600\">!Активирован штраф на награду с мобов!</font><br>Решите пример,<br>=== <font color=LEVEL> " + a + " + " + b + " = ?</font>, ===<br> для снятия штрафа: <br><edit var=\"pwd\" width=60 length=\"5\"><br><br><button value=\"Ok\" action=\"bypass -h " + bypass + " $pwd\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></body></html>");
                    break;
                case 3:
                    int aa = 12;
                    int ab = 12;
                    int ac = 12;
                    int ad = 12;
                    int ae = 12;
                    String aac = "FFFF00";
                    String abc = "CC3300";
                    String acc = "FF33CC";
                    String adc = "0099FF";
                    String aec = "00FF00";
                    String color = "";
                    switch (Rnd.get(4)) {
                        case 1:
                            aa = _farmDelay;
                            color = aac;
                            break;
                        case 2:
                            ab = _farmDelay;
                            color = abc;
                            break;
                        case 3:
                            ac = _farmDelay;
                            color = acc;
                            break;
                        case 4:
                            ad = _farmDelay;
                            color = adc;
                            break;
                        default:
                            ae = _farmDelay;
                            color = aec;
                            break;
                    }

                    html.setHtml("<html><body><font color=\"FF6600\">!Активирован штраф на награду с мобов!</font>"
                            + "<br><font color=" + color + ">Какого цвета этот текст?</font>"
                            + "<br1><font color=" + color + ">Какого цвета этот текст?</font>"
                            + "<br1><font color=" + color + ">Какого цвета этот текст?</font>"
                            + "<br><a action=\"bypass -h " + bypass + " " + aa + "\"><font color=" + aac + ">IIIIIIIIIIII</font></a><br>"
                            + "<br><a action=\"bypass -h " + bypass + " " + ab + "\"><font color=" + abc + ">IIIIIIIIIIII</font></a><br>"
                            + "<br><a action=\"bypass -h " + bypass + " " + ac + "\"><font color=" + acc + ">IIIIIIIIIIII</font></a><br>"
                            + "<br><a action=\"bypass -h " + bypass + " " + ad + "\"><font color=" + adc + ">IIIIIIIIIIII</font></a><br>"
                            + "<br><a action=\"bypass -h " + bypass + " " + ae + "\"><font color=" + aec + ">IIIIIIIIIIII</font></a><br>"
                            + "</body></html>");
                    break;
            }
            player.sendUserPacket(html);

            if (FARM_TRYES_TELE > 0) {
                _farmTryes++;
                if (_farmTryes > FARM_TRYES_TELE) {
                    player.teleToLocation(FARM_TRYES_LOC);
                }
            }
        }

        public void clear() {
            _farmDelay = 0;
            _farmLesson = 0;
            _farmTryes = 0;
            player.sendHtmlMessage("Спасибо", "Запрет снят.");
            clearPlayer(player.getObjectId());
        }

        public boolean hasPenalty() {
            return _farmDelay > 1;
        }

        public int getLesson() {
            return _farmLesson;
        }
    }

    public static void checkPlayer(L2PcInstance player) {
        Integer last = _farmDelays.get(player.getObjectId());
        if (last == null) {
            last = 0;
        }

        if (last >= FARM_DELAY_INTERVAL) {
            player.showFarmPenalty();
            return;
        }
        _farmDelays.put(player.getObjectId(), last + 1);
    }

    public static void clearPlayer(int objId) {
        _farmDelays.put(objId, 1);
    }

    public static FarmDelay create(L2PcInstance player) {
        return new FarmDelay(player);
    }

    public static void check(L2PcInstance player, int npcId) {
        if (player == null) {
            return;
        }

        if (!FARM_DELAY) {
            return;
        }

        if (!FARM_DELAY_MOBS.isEmpty() && !FARM_DELAY_MOBS.contains(npcId)) {
            return;
        }

        checkPlayer(player);
    }

    public static void restore(int objId) {
        //
    }
}
