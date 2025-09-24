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
package net.sf.l2j.gameserver.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * @author Layane
 *
 */
public class HtmCache {

    private static final Logger _log = AbstractLogger.getLogger(HtmCache.class.getName());
    private static HtmCache _instance;
    private FastMap<Integer, String> _cache;
    private int _loadedFiles;
    private long _bytesBuffLen;

    public static HtmCache getInstance() {
        if (_instance == null) {
            _instance = new HtmCache();
        }

        return _instance;
    }

    public HtmCache() {
        _cache = new FastMap<Integer, String>().shared("HtmCache._cache");
        reload();
    }

    public void reload() {
        reload(Config.DATAPACK_ROOT);
    }

    public void reload(File f) {
        if (!Config.LAZY_CACHE) {
            _log.info("HtmCache: cache start...");
            parseDir(f);
            _log.info("HtmCache: " + String.format("%.3f", getMemoryUsage()) + " megabytes on " + getLoadedFiles()
                    + " files loaded");
        } else {
            _cache.clear();
            _cache.setName("HtmCache._cache");
            _loadedFiles = 0;
            _bytesBuffLen = 0;
            _log.info("HtmCache: Running lazy cache");
        }
    }

    public void reloadPath(File f) {
        parseDir(f);
        _log.info("HtmCache: Reloaded specified path.");
    }

    public double getMemoryUsage() {
        return ((float) _bytesBuffLen / 1048576);
    }

    public int getLoadedFiles() {
        return _loadedFiles;
    }

    static class HtmFilter implements FileFilter {

        public boolean accept(File file) {
            if (!file.isDirectory()) {
                return (file.getName().endsWith(".htm") || file.getName().endsWith(".html"));
            }
            return true;
        }
    }

    private void parseDir(File dir) {
        FileFilter filter = new HtmFilter();
        File[] files = dir.listFiles(filter);

        for (File file : files) {
            if (!file.isDirectory()) {
                loadFile(file);
            } else {
                parseDir(file);
            }
        }
    }

    public String loadFile(File file) {
        HtmFilter filter = new HtmFilter();

        if (file.exists() && filter.accept(file) && !file.isDirectory()) {
            String content;
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                int bytes = bis.available();
                byte[] raw = new byte[bytes];

                bis.read(raw);
                content = new String(raw, "UTF-8");
                content = content.replaceAll("\r\n", "\n");

                String relpath = Util.getRelativePath(Config.DATAPACK_ROOT, file);
                int hashcode = relpath.hashCode();

                String oldContent = _cache.get(hashcode);

                if (oldContent == null) {
                    _bytesBuffLen += bytes;
                    _loadedFiles++;
                } else {
                    _bytesBuffLen = _bytesBuffLen - oldContent.length() + bytes;
                }

                _cache.put(hashcode, content);

                return content;
            } catch (Exception e) {
                _log.warning("problem with htm file " + e);
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (Exception e1) {
                    _log.warning("HtmlCache: fis.close() " + e1);
                }
                try {
                    if (bis != null) {
                        bis.close();
                    }
                } catch (Exception e1) {
                    _log.warning("HtmlCache: bis.close() " + e1);
                }
            }
        }

        return null;
    }

    public String getHtmForce(String path) {
        String content = getHtm(path);

        if (content == null) {
            content = "<html><body>My text is missing:<br>" + path + "</body></html>";
            _log.warning("HtmCache: Missing HTML page: " + path);
        }

        return content;
    }

    public String getHtm(String path) {
        String content = _cache.get(path.hashCode());

        if (Config.LAZY_CACHE && content == null) {
            content = loadFile(new File(Config.DATAPACK_ROOT, path));
        }

        return content;
    }

    // public String getHtm(L2PcInstance player, String path) {
    //     String content = null;
    //     String newPath = path;

    //     if (player != null && Config.MULTILANG_ENABLE) {
    //         String lang = player.getLang();
    //         if (lang != null && !lang.isEmpty() && !lang.equals(Config.MULTILANG_DEFAULT)) {
    //             // заменяем только "data/html/" в начале
    //             if (path.startsWith("data/html/")) {
    //                 newPath = path.replaceFirst("data/html", "data/html-" + lang);
    //             }

    //             content = _cache.get(newPath.hashCode());

    //             if (Config.LAZY_CACHE && content == null) {
    //                 content = loadFile(new File(Config.DATAPACK_ROOT, newPath));
    //             }
    //         }
    //     }

    //     // если для языка не нашли → пробуем дефолт
    //     if (content == null) {
    //         content = _cache.get(path.hashCode());

    //         if (Config.LAZY_CACHE && content == null) {
    //             content = loadFile(new File(Config.DATAPACK_ROOT, path));
    //         }
    //     }

    //     return content;
    // }

    public boolean contains(String path) {
        return _cache.containsKey(path.hashCode());
    }

    /**
     * Check if an HTM exists and can be loaded
     * 
     * @param
     * path        The path to the HTM
     */
    public boolean isLoadable(String path) {
        File file = new File(path);
        HtmFilter filter = new HtmFilter();

        if (file.exists() && filter.accept(file) && !file.isDirectory()) {
            return true;
        }

        return false;
    }
}
