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
package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public final class RequestReplySurrenderPledgeWar extends L2GameClientPacket
{
    private int _answer;
    private String _reqName;

    @Override
	protected void readImpl()
    {
		try
		{
			_reqName = readS();
		}
		catch (BufferUnderflowException e)
		{
			_reqName = "";
		}
        _answer = readD();
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
            requestor.deathPenalty(false);
            ClanTable.getInstance().deleteclanswars(requestor.getClanId(), player.getClanId());
        }
        else
			player.sendMessage("Игрок не согласен");

        player.setTransactionRequester(null);
        requestor.setTransactionRequester(null);
    }
}