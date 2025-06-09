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

//import java.util.logging.Logger;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestGiveNickName extends L2GameClientPacket
{
	//static Logger _log = Logger.getLogger(RequestGiveNickName.class.getName());

	private String _target;
	private String _title;

	@Override
	protected void readImpl()
	{
		_target = readS();
		_title  = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;

		if(System.currentTimeMillis() - player.getRequestGiveNickName() < 200)
		{
			player.logout();
			return;
		}

		player.setRequestGiveNickName();
		//player.clearTitleChngedFail();

		if(_title.length() > 16)
		{
			player.sendPacket(Static.NAMING_CHARNAME_UP_TO_16CHARS);
			return;
		}
	
		// Noblesse can bestow a title to themselves
		if (player.isNoble() && _target.equalsIgnoreCase(player.getName()))//_target.matches(player.getName()))
		{
			player.setTitle(_title);
			player.sendPacket(Static.TITLE_CHANGED);
			player.broadcastTitleInfo();
			return;
		}

		if (player.getClan() == null || player.getClan().getLevel() < 3)
		{
            player.sendPacket(Static.CLAN_LVL_3_NEEDED_TO_ENDOWE_TITLE);
			return;
		}

		//Can the player change/give a title?
		if ((player.getClanPrivileges() & L2Clan.CP_CL_GIVE_TITLE) == L2Clan.CP_CL_GIVE_TITLE)
		{
			L2ClanMember member1 = player.getClan().getClanMember(_target);
            if (member1 == null)
            {
				player.sendPacket(Static.PLAYER_NOT_IN_YOR_CLAN);
				return;
			}

            L2PcInstance member = member1.getPlayerInstance();
            if (member == null)
            {
				player.sendPacket(Static.PLAYER_NOT_IN_GAME);
				return;
			}

        	//is target from the same clan?
    		member.setTitle(_title);
    		member.sendPacket(Static.TITLE_CHANGED);
			member.broadcastTitleInfo();
		}
	}
}
