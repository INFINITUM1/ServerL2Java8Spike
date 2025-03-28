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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExGetBossRecord;
/**
 * Format: (ch) d
 * @author  -Wooden-
 *
 */
public class RequestGetBossRecord extends L2GameClientPacket
{
    protected static final Logger _log = Logger.getLogger(RequestGetBossRecord.class.getName());
    private int _bossId;

    @Override
	protected void readImpl()
    {
        _bossId = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
	protected void runImpl()
    {
    	L2PcInstance player = getClient().getActiveChar();
    	if(player == null)
    		return;

        int points = RaidBossPointsManager.getPointsByOwnerId(player.getObjectId());
        int ranking = RaidBossPointsManager.calculateRanking(player);
        
        Map<Integer, Integer> list = RaidBossPointsManager.getList(player);

        // trigger packet
       	player.sendPacket(new ExGetBossRecord(ranking, points, list));
    }
}