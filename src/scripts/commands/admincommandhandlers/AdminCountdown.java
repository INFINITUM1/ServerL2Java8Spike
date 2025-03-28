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
package scripts.commands.admincommandhandlers;

import java.util.Collection;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
//import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import scripts.commands.IAdminCommandHandler;

/**
 * This class handles following admin commands:
 * - gmchat text = sends text to all online GM's
 * - gmchat_menu text = same as gmchat, displays the admin panel after chat
 *
 * @version $Revision: 1.2.4.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminCountdown implements IAdminCommandHandler 
{

	private static final String[] ADMIN_COMMANDS = {"admin_countdown"};
	private static final int REQUIRED_LEVEL = Config.GM_MIN;

	public boolean useAdminCommand(String command, L2PcInstance activeChar) 
	{
		if (!Config.ALT_PRIVILEGES_ADMIN)
			if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) return false;

		if (command.startsWith("admin_countdown"))
			handleGmChat(command, activeChar);
		return true;
	}


	public String[] getAdminCommandList() 
	{
		return ADMIN_COMMANDS;
	}

	private boolean checkLevel(int level) 
	{
		return (level >= REQUIRED_LEVEL);
	}

	/**
	 * @param command
	 * @param activeChar
	 */
	private void handleGmChat(String command, L2PcInstance activeChar)
	{
		try
		{
			/*
			CreatureSay cs = new CreatureSay(activeChar.getObjectId(), 15, activeChar.getName(), text);*/
			String pre_countdown = command.substring(16);
			int countdown = Integer.parseInt(pre_countdown);
			
			for (int i = countdown; i >= 0; i--)
			{
				SystemMessage sm = null;
				if (i > 0)
					sm = SystemMessage.id(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS).addNumber(i);
				else 
					sm = SystemMessage.id(SystemMessageId.LET_THE_DUEL_BEGIN);
				
				sendMessageToPlayers(activeChar, sm);
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					return;
				}
			}
		}
		catch (StringIndexOutOfBoundsException e)
		{
			// empty message.. ignore
		}
	}
	
	public void sendMessageToPlayers(L2PcInstance activeChar, L2GameServerPacket packet)
	{
		Collection<L2PcInstance> players = activeChar.getKnownList().getKnownPlayersInRadius(1250);
		for (L2PcInstance player : players)
		{	
			player.sendPacket(packet);
		}
		activeChar.sendPacket(packet);
	}
}
