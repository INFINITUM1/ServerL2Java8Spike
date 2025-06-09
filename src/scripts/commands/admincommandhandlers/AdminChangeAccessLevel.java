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

import net.sf.l2j.mysql.Connect;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import scripts.commands.IAdminCommandHandler;

/**
 * This class handles following admin commands:
 * - changelvl = change a character's access level
 *  Can be used for character ban (as opposed to regular //ban that affects accounts)
 *  or to grant mod/GM privileges ingame
 * @version $Revision: 1.1.2.2.2.3 $ $Date: 2005/04/11 10:06:00 $
 */
public class AdminChangeAccessLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = { "admin_banchar", "admin_unbanchar", "admin_changelvl" };

	private static final int REQUIRED_LEVEL = Config.GM_ACCESSLEVEL;

	public boolean useAdminCommand(String command, L2PcInstance adm)
	{
		if (!Config.ALT_PRIVILEGES_ADMIN)
			if (!(checkLevel(adm.getAccessLevel()) && adm.isGM()))
				return false;

		String nick = "no-target";
		if (command.startsWith("admin_banchar")) //banchar nick mudazvon
		{
			String cmd = command.substring(14);
			nick = cmd.split(" ")[0];
			String reason = cmd.replace(nick + " ", "");
			banPlayer(adm, nick.trim(), reason, -100);
		}
		else if (command.startsWith("admin_unbanchar"))
		{
			nick = command.substring(16).trim();
			banPlayer(adm, nick, "", 0);
		}
		else if (command.startsWith("admin_changelvl"))
		{
			handleChangeLevel(command, adm);
			nick = (adm.getTarget() != null ? adm.getTarget().getName() : "no-target");
		}
		GMAudit.auditGMAction(adm.getName(), command, nick, "");
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
	 * If no character name is specified, tries to change GM's target access level. Else
	 * if a character name is provided, will try to reach it either from L2World or from
	 * a database connection.
	 *
	 * @param command
	 * @param adm
	 */
	private void handleChangeLevel(String command, L2PcInstance adm)
	{
		String[] parts = command.split(" ");
		if (parts.length == 2)
		{
			try
			{
				int lvl = Integer.parseInt(parts[1]);
				if (adm.getTarget().isPlayer())
					onLineChange(adm, (L2PcInstance)adm.getTarget(), lvl);
				else
					adm.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
			}
			catch (Exception e)
			{
				adm.sendAdmResultMessage("Usage: //changelvl <target_new_level> | <player_name> <new_level>");
			}
		}
		else if (parts.length == 3)
		{
			String name = parts[1];
			int lvl = Integer.parseInt(parts[2]);
			L2PcInstance player = L2World.getInstance().getPlayer(name);
			if (player != null)
				onLineChange(adm, player, lvl);
			else
			{
				Connect con = null;
				try
				{
					con = L2DatabaseFactory.get();
					PreparedStatement statement = con.prepareStatement("UPDATE characters SET accesslevel=? WHERE char_name=?");
					statement.setInt(1, lvl);
					statement.setString(2, name);
					statement.execute();
					int count = statement.getUpdateCount();
					statement.close();
					if (count == 0)
						adm.sendAdmResultMessage("Character not found or access level unaltered.");
					else
						adm.sendAdmResultMessage("Character's access level is now set to "+lvl);
				}
				catch (SQLException se)
				{
					adm.sendAdmResultMessage("SQLException while changing character's access level");
					if (Config.DEBUG)
						se.printStackTrace();
				}
				finally
				{
					try
					{
						con.close();
					}
					catch (Exception e) {}
				}
			}
		}
	}
	
	//ALTER TABLE characters MODIFY BanReason VARCHAR(256) CHARACTER SET utf8 COLLATE utf8_general_ci;
	private void banPlayer(L2PcInstance adm, String nick, String reason, int acs)
	{
		L2PcInstance player = L2World.getInstance().getPlayer(nick);
		if (player != null)
		{
			player.setAccessLevel(acs);
			player.kick();
		}

		Connect con = null;
		PreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.get();
			st = con.prepareStatement("UPDATE `characters` SET `accesslevel`=?, `BanReason`=? WHERE `char_name`=?");
			st.setInt(1, acs);
			st.setString(2, reason);
			st.setString(3, nick);
			st.execute();
			int count = st.getUpdateCount();
			if (count == 0)
				adm.sendAdmResultMessage("Игрок " + nick + " не найден в базе данных.");
			else
			{
				if (acs < 0)
				{
					Announcements.getInstance().announceToAll("Забанен игрок " + nick + ".");
					Announcements.getInstance().announceToAll("(" + reason + ")");
					adm.sendAdmResultMessage("Игрок " + nick + " забанен. (" + reason + ")");
				}
				else
				{
					adm.sendAdmResultMessage("Игрок " + nick + " разбанен.");
					//Announcements.getInstance().announceToAll(command.substring(15));
				}
			}
		}
		catch (SQLException e)
		{
			System.out.println("[ERROR] AdminBan, banPlayer() " + e);
		}
		finally
		{
			Close.CS(con, st);
		}
	}

	/**
	 * @param adm
	 * @param player
	 * @param lvl
	 */
	private void onLineChange(L2PcInstance adm, L2PcInstance player, int lvl)
	{
		player.setAccessLevel(lvl);
		if (lvl>0)
			player.sendAdmResultMessage("Your access level has been changed to "+lvl);
		else
		{
			player.sendAdmResultMessage("Your character has been banned. Bye.");
			player.logout();
		}
		adm.sendAdmResultMessage("Character's access level is now set to "+lvl+". Effects won't be noticeable until next session.");
	}
}
