/* This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.geoeditorcon;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import scripts.commands.IVoicedCommandHandler;
import scripts.commands.VoicedCommandHandler;

/**
 * @author Dezmond
 */
public class GeoEditorListener extends Thread {

    private static GeoEditorListener _instance;
    private static final int PORT = 7779;
    private static Logger _log = Logger.getLogger(GeoEditorListener.class.getName());
    private ServerSocket _serverSocket;
    private static GeoEditorThread _geoEditor;

    public static GeoEditorListener getInstance() {
        return _instance;
    }

    public static void init() {
        if (!Config.ACCEPT_GEOEDITOR_CONN) {
            return;
        }
        try {
            _instance = new GeoEditorListener();
            _instance.start();
            _log.info("GeoEditorListener Initialized on port: " + PORT);
        } catch (IOException e) {
            _log.severe("Error creating geoeditor listener! " + e.getMessage());
        }
    }

    private GeoEditorListener() throws IOException {
        _serverSocket = new ServerSocket(PORT);
    }

    public GeoEditorThread getThread() {
        return _geoEditor;
    }

    public String getStatus() {
        if (_geoEditor != null && _geoEditor.isWorking()) {
            return "Geoeditor connected.";
        }
        return "Geoeditor not connected.";
    }

    @Override
    public void run() {
        Socket connection = null;
        try {
            while (true) {
                connection = _serverSocket.accept();
                if (_geoEditor != null && _geoEditor.isWorking()) {
                    _log.warning("Geoeditor already connected!");
                    connection.close();
                    continue;
                }
                _log.info("Received geoeditor connection from: "
                        + connection.getInetAddress().getHostAddress());
                _geoEditor = new GeoEditorThread(connection);
                _geoEditor.start();
            }
        } catch (Exception e) {
            _log.info("GeoEditorListener: " + e.getMessage());
            try {
                connection.close();
            } catch (Exception e2) {
            }
        } finally {
            try {
                _serverSocket.close();
            } catch (IOException io) {
                _log.log(Level.INFO, "", io);
            }
            _log.warning("GeoEditorListener Closed!");
        }
    }
}
