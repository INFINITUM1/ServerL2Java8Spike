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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author zabbix
 * Lets drink to code!
 */
public final class RequestLinkHtml extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestLinkHtml.class.getName());
	private String _link;

	@Override
	protected void readImpl()
	{
		_link = readS();
	}

	@Override
	public void runImpl()
	{
		L2PcInstance actor = getClient().getActiveChar();
		if(actor == null)
			return;

		if(System.currentTimeMillis() - actor.gCPAG() < 200)
			return;

		actor.sCPAG();

		if(_link.contains("..") || !_link.contains(".htm"))
		{
			_log.warning("[RequestLinkHtml] hack? link contains prohibited characters: '"+_link+"', skipped");
			return;
		}

		NpcHtmlMessage msg = NpcHtmlMessage.id(0);
		msg.setFile(_link);

		sendPacket(msg);
	}
}
