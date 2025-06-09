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
package scripts.skills.skillhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import scripts.skills.ISkillHandler;
import javolution.util.FastList;
import javolution.util.FastList.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WeddingTP implements ISkillHandler
{
    static final Log _log = LogFactory.getLog(WeddingTP.class);
	private static final SkillType[] SKILL_IDS = {SkillType.WEDDINGTP};

 	public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
	{
		L2PcInstance player = (L2PcInstance)activeChar;
	
		if ((player.getX() >= -150000 && player.getX() <= -60000) && (player.getY() >= -250000 && player.getY() <= -180000))		
			return;
			
        if(!player.isMarried())
        {
			if (!player.getAppearance().getSex())
				player.sendMessage("Вы не женаты");
			else
				player.sendMessage("Вы не замужем");
            return;
        }

        if(player.getPartnerId()==0)
        {
            player.sendMessage("Упс, сообщите об этой ошибке Администратору");
            return;
        }
			
        L2PcInstance partner;
        partner = (L2PcInstance)L2World.getInstance().findObject(player.getPartnerId());
		
		if(partner == null)
        {
			player.sendMessage("Ваша половинка не в игре");
            return;
        }
		
		String Sex = "";
		if (!partner.getAppearance().getSex())
			Sex = "Ваша жена";
		else
			Sex = "Ваш муж";
		
		Castle castleac = CastleManager.getInstance().getCastle(player);
		Castle castlepn = CastleManager.getInstance().getCastle(partner);

        if(player.isInJail())
        {
            player.sendMessage("Вы в тюрьме");
            return;
        }
        else if(player.isInOlympiadMode() || player.getOlympiadSide() > 1)
        {
            player.sendMessage("Вы на олимпиаде");
            return;
        }
        else if(player.atEvent)
        {
            player.sendMessage("Вы на евенте");
            return;
        }
        else  if (player.isInDuel())
        {
            player.sendMessage("Дерись тряпка");
            return;
        }
        else if (player.inObserverMode())
        {
        	player.sendMessage("Смотрите дальше вашу олимпиаду");
            return;
        }
        else if(player.getClan() != null
        		&& CastleManager.getInstance().getCastleByOwner(player.getClan()) != null
        		&& CastleManager.getInstance().getCastleByOwner(player.getClan()).getSiege().getIsInProgress())
        {
        	player.sendMessage("Вы на осаде");
        	return;
        }
		else if (castleac != null && castleac.getSiege().getIsInProgress())
		{
			player.sendMessage("Вы на осаде");
			return;
		}
        else if (player.isFestivalParticipant())
        {
            player.sendMessage("Вы на фестивале");
            return;
        }
        else if (player.isInParty() && player.getParty().isInDimensionalRift())
        {
            player.sendMessage("Вы в рифте");
            return;
        }
        else if (player.isInsideSilenceZone())
        {
            player.sendMessage("Здесь нельзя");
            return;
        }
        // Thanks nbd
        else if (!TvTEvent.onEscapeUse(player.getName()))
        {
        	player.sendPacket(new ActionFailed());
			player.sendMessage("Вы на твт");
        	return;
        }
		if (player.isEventWait() || player.getChannel() > 1)
		{
			player.sendMessage("Здесь нельзя");
			return;
		}
		
        if(partner.isInJail())
        {
			player.sendMessage(""+Sex+" в тюрьме");
            return;
        }
        else if(partner.isDead())
        {
			player.sendMessage(""+Sex+" на том свете");
            return;
        }
        else if(partner.isInOlympiadMode() || partner.getOlympiadSide() > 1)
        {
			player.sendMessage(""+Sex+" на олимпиаде");
            return;
        }
        else if(partner.atEvent)
        {
            player.sendMessage(""+Sex+" на евенте");
            return;
        }
        else  if (partner.isInDuel())
        {
            player.sendMessage(""+Sex+" дуэлится");
            return;
        }
        else if (partner.isFestivalParticipant())
        {
            player.sendMessage(""+Sex+" на фестивале тьмы");
            return;
        }
        else if (partner.isInParty() && partner.getParty().isInDimensionalRift())
        {
            player.sendMessage(""+Sex+" в рифте");
            return;
        }
        else if (partner.inObserverMode())
        {
        	player.sendMessage(""+Sex+" наблюдает за олимпиадой");
            return;
        }
        else if (partner.isInsideSilenceZone())
        {
            player.sendMessage("Здесь нельзя");
            return;
        }
        else if(partner.getClan() != null
        		&& CastleManager.getInstance().getCastleByOwner(partner.getClan()) != null
        		&& CastleManager.getInstance().getCastleByOwner(partner.getClan()).getSiege().getIsInProgress())
        {
        	player.sendMessage(""+Sex+" на осаде");
        	return;
        }
		else if (castlepn != null && castlepn.getSiege().getIsInProgress())
		{
			player.sendMessage(""+Sex+" на осаде");
			return;
		}
		if (partner.isEventWait() || player.getChannel() > 1)
		{
			player.sendMessage("Здесь нельзя");
			return;
		}

		player.teleToLocation(partner.getX(), partner.getY(), partner.getZ());
		//targets.clear();
 	}

	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}