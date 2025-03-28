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

import net.sf.l2j.Config;
import scripts.commands.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *<b> This class handles following admin commands: </b><br><br>
 *
 * <li> admin_banchat = Imposes a chat ban on the specified player. <br>
 * <li> admin_unbanchat = Removes any chat ban on the specified player. <br>
 * <li> admin_unbanchat_all = bans al players chat in server <br>
 * <li> admin_banchat_all = undo mass ban action  <br><br>
 * 
 * <b>Usage:</b><br><br>
 * 
 * <li> //banchat [player_name] [time_in_seconds] <br>
 * <li> //banchat [player_name] [time_in_seconds] [ban_chat_reason] <br>
 * <li> //unbanchat [player_name] <br>
 * <li> //unbanchat_all <br>
 * <li> //banchat_all <br><br>
 * 
 */
public class AdminBanChat implements IAdminCommandHandler
{
	private final static Log _log = LogFactory.getLog(AdminBan.class.getName());

	private static String[] ADMIN_COMMANDS = 
	{
		"admin_banchat",
		"admin_unbanchat",
		"admin_unbanchat_all",
		"admin_banchat_all"
	};
	private static final int REQUIRED_LEVEL = Config.GM_BAN_CHAT;

	public boolean useAdminCommand(String command, L2PcInstance admin)
	{
		if (!Config.ALT_PRIVILEGES_ADMIN)
		{
			if (!(checkLevel(admin.getAccessLevel())))
			{
				_log.info("Not required level for " + admin.getName());
				return false;
			}
		}

		String[] cmdParams = command.split(" ");

		// checking syntax
		if ( cmdParams.length < 3 && command.startsWith("admin_banchat") )
		{
			admin.sendAdmResultMessage("usage:");
			admin.sendAdmResultMessage("  //banchat [<player_name>] [<time_in_seconds>]");
			admin.sendAdmResultMessage("  //banchat [<player_name>] [<time_in_seconds>] [<ban_chat_reason>]");
			return false;
		}
		else if ( cmdParams.length < 2 && command.startsWith("admin_unbanchat") )
		{
			admin.sendAdmResultMessage("UnBanChat Syntax:");
			admin.sendAdmResultMessage("  //unbanchat [<player_name>]");
			return false;
		}
		else if (command.startsWith("admin_banchat_all"))
		{
			try
			{
				for (L2PcInstance player : admin.getKnownList().getKnownPlayers().values())
				{
					if (!player.isGM())
					{
						player.setBanChatTimer(120 * 60000); //setting max 2 min
						player.setChatBannedForAnnounce(true);
					}
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.startsWith("admin_unbanchat_all"))
		{
			try
			{
				for (L2PcInstance player : admin.getKnownList().getKnownPlayers().values())
				{
					player.setChatBannedForAnnounce(false);
				}
			}
			catch (Exception e)
			{
			}
		}
		// void vars		
		long   banLength = -1;
		String banReason = "";
		//L2Object targetObject = null;
		L2PcInstance targetPlayer = null;

		// chat instance
		targetPlayer = L2World.getInstance().getPlayer(cmdParams[1]);

		if (targetPlayer == null)
		{
			admin.sendAdmResultMessage("Incorrect parameter or target.");
			return false;
		}

		// what is our actions?
		if (command.startsWith("admin_banchat"))
		{		
			// ban chat length (seconds)
			try
			{
				banLength = Integer.parseInt(cmdParams[2]);
			}
			catch (NumberFormatException nfe) {}

			// ban chat reason
			if (cmdParams.length > 3)
				banReason = cmdParams[3];

			// apply ban chat
			admin.sendMessage(targetPlayer.getName() + "'s chat is banned for " + banLength + " seconds.");
			targetPlayer.setChatBanned(true, banLength, banReason);
		}
		else

			if (command.startsWith("admin_unbanchat"))
			{
				admin.sendMessage(targetPlayer.getName() + "'s chat ban has now been lifted.");
				targetPlayer.setChatBanned(false, 0, "");
			}
		return true;
	}
	
	public String[] getAdminCommandList() 
	{
		return ADMIN_COMMANDS;
	}
	/**
	 * 
	 * @param level
	 * @return
	 */
	private boolean checkLevel(int level)
	{
		return (level >= REQUIRED_LEVEL);
	}
}