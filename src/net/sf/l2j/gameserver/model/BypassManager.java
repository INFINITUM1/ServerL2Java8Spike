package net.sf.l2j.gameserver.model;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Log;
import scripts.communitybbs.Manager.BaseBBSManager;

public class BypassManager {

    private static final Pattern p = Pattern.compile("\"(bypass +-h +)(.+?)\"");
    private static final Pattern pbbs = Pattern.compile("\"(bypass +)(.+?)\"");

    public static enum BypassType {

        ENCODED,
        ENCODED_BBS,
        SIMPLE,
        SIMPLE_BBS,
        SIMPLE_DIRECT
    }

    public static BypassType getBypassType(String bypass) {
        switch (bypass.charAt(0)) {
            case '0':
                return BypassType.ENCODED;
            case '1':
                return BypassType.ENCODED_BBS;
            default:
                if (matches(bypass, "^(_mrsl|_diary|_match|manor_menu_select|_match|_olympiad).*", Pattern.DOTALL)) {
                    return BypassType.SIMPLE;
                }
                return BypassType.SIMPLE_DIRECT;
        }
    }

    private static boolean matches(String str, String regex, int flags) {
        return Pattern.compile(regex, flags).matcher(str).matches();
    }

    public static String encode(String html, List<String> bypassStorage, boolean bbs) {
        Matcher m = p.matcher(html);
        if (bbs) {
            m = pbbs.matcher(html);
        }
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String bypass = m.group(2);
            //System.out.println("##!!01##" + bypass);
            String code = bypass;
            String params = "";
            int i = bypass.indexOf(" $");
            boolean use_params = i >= 0;
            if (use_params) {
                code = bypass.substring(0, i);
                params = bypass.substring(i).replace("$", "\\$");
            }

            //System.out.println("##!!11##" + params);
            if (bbs) {
                //System.out.println("##!!if (bbs)##");
                // m.appendReplacement(sb, Matcher.quoteReplacement("\"bypass -h 1" + Integer.toHexString(bypassStorage.size()) + params + "\""));
                m.appendReplacement(sb, Matcher.quoteReplacement("\"bypass 1" + Integer.toHexString(bypassStorage.size()) + params + "\""));
                //m.appendReplacement(sb, "\"bypass 1" + Integer.toHexString(bypassStorage.size()) + params + "\"");
            } else {
                //System.out.println("##!!else##");
                m.appendReplacement(sb, "\"bypass -h 0" + Integer.toHexString(bypassStorage.size()) + params + "\"");
            }

            bypassStorage.add(code);
        }

        m.appendTail(sb);
        return sb.toString();
    }

    public static DecodedBypass decode(String bypass, List<String> bypassStorage, boolean bbs, L2PcInstance player) {
        synchronized (bypassStorage) {
            String[] bypass_parsed = bypass.split(" ");
            int idx = Integer.parseInt(bypass_parsed[0].substring(1), 16);
            String bp;

            try {
                bp = bypassStorage.get(idx);
            } catch (Exception e) {
                bp = null;
            }

            if (bp == null) {
                //Log.add("Can't decode bypass (bypass not exists): " + (bbs ? "[bbs] " : "") + bypass + " / Player: " + player.getName(), "debug_bypass");
                return null;
            }

            DecodedBypass result = new DecodedBypass(bp, bbs);
            for (int i = 1; i < bypass_parsed.length; i++) {
                result.bypass += " " + bypass_parsed[i];
            }
            result.trim();

            return result;
        }
    }

    public static class DecodedBypass {

        public String bypass;
        public boolean bbs;
        public BaseBBSManager handler;

        public DecodedBypass(String _bypass, boolean _bbs) {
            bypass = _bypass;
            bbs = _bbs;
        }

        public DecodedBypass(String _bypass, BaseBBSManager _handler) {
            bypass = _bypass;
            handler = _handler;
        }

        public DecodedBypass trim() {
            bypass = bypass.trim();
            return this;
        }
    }
}
