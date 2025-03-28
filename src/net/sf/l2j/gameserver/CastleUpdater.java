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
package net.sf.l2j.gameserver;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.ItemContainer;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.util.log.AbstractLogger;

/**
 *
 * Thorgrim - 2005
 * Class managing periodical events with castle
 *
 */
public class CastleUpdater implements Runnable {

    protected static final Logger _log = AbstractLogger.getLogger(CastleUpdater.class.getName());
    private L2Clan _c;
    private int _rc = 0;

    public CastleUpdater(L2Clan c, int rc) {
        _c = c;
        _rc = rc;
    }

    public void run() {
        try {
            // Move current castle treasury to clan warehouse every 2 hour
            ItemContainer warehouse = _c.getWarehouse();
            if ((warehouse != null) && (_c.getHasCastle() > 0)) {
                Castle castle = CastleManager.getInstance().getCastleById(_c.getHasCastle());
                if (!Config.ALT_MANOR_SAVE_ALL_ACTIONS) {
                    if (_rc % Config.ALT_MANOR_SAVE_PERIOD_RATE == 0) {
                        castle.saveSeedData();
                        castle.saveCropData();
                        _log.info("Manor System: all data for " + castle.getName() + " saved");
                    }
                }
                _rc++;
                CastleUpdater cu = new CastleUpdater(_c, _rc);
                ThreadPoolManager.getInstance().scheduleGeneral(cu, 3600000);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
