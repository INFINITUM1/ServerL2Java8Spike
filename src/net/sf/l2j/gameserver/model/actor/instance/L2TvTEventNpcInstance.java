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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.lasthero.LastHero;
import scripts.autoevents.masspvp.massPvp;

public class L2TvTEventNpcInstance extends L2NpcInstance
{
	public L2TvTEventNpcInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		TvTEvent.onBypass(command, player);
	}

	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		if (player == null)
			return;
		
		if(player.getKarma() > 0 || player.isCursedWeaponEquiped())
		{
			player.sendHtmlMessage("У вас плохая карма.");
			return;
		}
		if (Config.TVT_NOBL && !player.isNoble()) 
		{
			player.sendHtmlMessage("Только ноблессы могут учавствовать");
			player.sendActionFailed();
			return;
		}
		if (Config.MASS_PVP && massPvp.getEvent().isReg(player))
		{
            player.sendHtmlMessage("Удачи на евенте -Масс ПВП-");
			player.sendActionFailed();
            return; 
		}
		if(Config.ELH_ENABLE && LastHero.getEvent().isRegged(player))
		{
            player.sendHtmlMessage("Вы уже зарегистрированы в евенте -Последний герой-");
			player.sendActionFailed();
            return; 
		}
		if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player))
		{
            player.sendHtmlMessage("Удачи на евенте -Захват базы-");
			player.sendActionFailed();
            return; 
		}
		if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode())
		{
			player.sendHtmlMessage("Вы уже зарегистрированы на олимпиаде.");
			player.sendActionFailed();
			return;
		}
		
		if (TvTEvent.isParticipating())
		{
			String htmFile = "data/html/mods/";

			if (!TvTEvent.isPlayerParticipant(player.getName()))
				htmFile += "TvTEventParticipation";
			else
				htmFile += "TvTEventRemoveParticipation";

			htmFile += ".htm";

			String htmContent = HtmCache.getInstance().getHtm(htmFile);

	    	if (htmContent != null)
	    	{
	    		int[] teamsPlayerCounts = TvTEvent.getTeamsPlayerCounts();
	    		NpcHtmlMessage npcHtmlMessage = NpcHtmlMessage.id(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
	    		npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
	    		player.sendPacket(npcHtmlMessage);
	    	}
		}
		else if (TvTEvent.isStarting() || TvTEvent.isStarted())
		{
			String htmFile = "data/html/mods/TvTEventStatus.htm";
			String htmContent = HtmCache.getInstance().getHtm(htmFile);

	    	if (htmContent != null)
	    	{
	    		int[] teamsPlayerCounts = TvTEvent.getTeamsPlayerCounts();
	    		int[] teamsPointsCounts = TvTEvent.getTeamsPoints();
	    		NpcHtmlMessage npcHtmlMessage = NpcHtmlMessage.id(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
	    		//npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team1points%", String.valueOf(teamsPointsCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%team2points%", String.valueOf(teamsPointsCounts[1])); // <---- array index from 0 to 1 thx DaRkRaGe
	    		player.sendPacket(npcHtmlMessage);
	    	}
		}
		else
		{
			String htmFile = "data/html/mods/TvTShop.htm";
			String htmContent = HtmCache.getInstance().getHtm(htmFile);

	    	if (htmContent != null)
	    	{
	    		NpcHtmlMessage npcHtmlMessage = NpcHtmlMessage.id(getObjectId());
				npcHtmlMessage.setHtml(htmContent);
	    		player.sendPacket(npcHtmlMessage);
	    	}
		}

		player.sendActionFailed();
	}
}
