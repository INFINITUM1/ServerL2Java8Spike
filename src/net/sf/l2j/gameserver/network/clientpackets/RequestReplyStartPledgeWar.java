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

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestReplyStartPledgeWar extends L2GameClientPacket
{
    private int _answer;

    @Override
	protected void readImpl()
    {
        @SuppressWarnings("unused") String _reqName = readS();
        _answer  = readD();
    }

    @Override
	protected void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null)
            return;
        L2PcInstance requestor = player.getTransactionRequester();
        if (requestor == null)
            return;

        if (_answer == 1)
        {
            ClanTable.getInstance().storeclanswars(requestor.getClanId(), player.getClanId());
        }
        else
        {
            requestor.sendPacket(Static.WAR_PROCLAMATION_HAS_BEEN_REFUSED);
        }
        player.setTransactionRequester(null);
        requestor.onTransactionResponse();
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return "C.ReplyStartPledgeWar";
    }
}