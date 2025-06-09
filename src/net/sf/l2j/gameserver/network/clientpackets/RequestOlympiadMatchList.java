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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * format ch
 * c: (id) 0xD0
 * h: (subid) 0x13
 * @author -Wooden-
 *
 */
public final class RequestOlympiadMatchList extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
		// trigger packet
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if(!player.inObserverMode())
			return;

		/*String[] matches = L2GrandOlympiad.getInstance().getAllTitles();

		NpcHtmlMessage reply = NpcHtmlMessage.id(0);
		StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<center><br>Grand Olympiad Game View");
		replyMSG.append("<table width=270 border=0 bgcolor=\"000000\">");
		replyMSG.append("<tr><td fixwidth=30>NO.</td><td>Status &nbsp; &nbsp; Player1 vs Player2</td></tr>");

		for(int i = 0; i < matches.length; i++)
			replyMSG.append("<tr><td fixwidth=30><a action=\"bypass -h olympiad_observ_" + i + "\">" + (i + 1) + "</a></td><td>" + matches[i] + "</td></tr>");
		replyMSG.append("</table></center></body></html>");

		reply.setHtml(replyMSG.toString());
		player.sendPacket(reply);*/
	}
}