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
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExAskJoinMPCC;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) S
 * @author chris_00
 *
 * D0 0D 00 5A 00 77 00 65 00 72 00 67 00 00 00
 *
 */
public final class RequestExAskJoinMPCC extends L2GameClientPacket
{
	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		L2PcInstance target = L2World.getInstance().getPlayer(_name);
		if(target == null)
		{
    	    player.sendPacket(Static.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
    	    return;
		}
		// invite yourself? ;)
		if(player.isInParty() && target.isInParty() && player.getParty().equals(target.getParty()))
			return;
			
		L2Party activeParty = player.getParty();
		
		// Приглашать в СС может только лидер CC
		if(activeParty == null || (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getChannelLeader().equals(player)))
		{
			player.sendMessage("Вы не имеете прав на приглашение в канал команды");
			return;
		}
		
		// Нельзя приглашать безпартийных и не лидеров партий
		if(!target.isInParty() || !target.getParty().isLeader(target))
		{
			player.sendMessage("Неправильная цель была приглашена");
			return;
		}
		
		if(target.getParty().isInCommandChannel())
		{
			player.sendMessage("Группа "+target.getName()+" уже присоединилась к каналу команды");
			return;
		}
		
		// Чувак уже отвечает на какое-то приглашение
		if(target.isTransactionInProgress())
		{
			player.sendMessage("Персонаж занят, попробуйте позже");
			return;
		}
		
		player.setTransactionType(TransactionType.CHANNEL);
		target.setTransactionRequester(player, System.currentTimeMillis() + 30000);
		target.setTransactionType(TransactionType.CHANNEL);
		target.sendPacket(new ExAskJoinMPCC(player.getName()));
		player.sendMessage("Вы пригласили " + target.getName() + " в канал команды");
		
		//askJoinMPCC(player, target);
	}

	/*private void askJoinMPCC(L2PcInstance requestor, L2PcInstance target)
	{
		boolean hasRight = false;
		if (requestor.getClan() != null && requestor.getClan().getLeaderId() == requestor.getObjectId()) // Clanleader
			hasRight = true;
		else if (requestor.getInventory().getItemByItemId(8871) != null) // 8871 Strategy Guide. Should destroyed after sucessfull invite?
			hasRight = true;
		else
		{
			for (L2Skill skill : requestor.getAllSkills())
			{
				// Skill Clan Imperium
				if (skill.getId() == 391)
				{
					hasRight = true;
					break;
				}
			}
		}
		if (!hasRight && !requestor.getParty().isInCommandChannel())
		{
			requestor.sendMessage("You dont have the rights to open a Command Channel!");
			return;
		}
		if (!target.isProcessingRequest())
		{
			requestor.onTransactionRequest(target);
			target.getParty().getPartyMembers().get(0).sendPacket(new ExAskJoinMPCC(requestor.getName()));
		    requestor.sendMessage("You invited "+target.getName()+" to your Command Channel.");
		}
		else
		{
		    requestor.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_BUSY_TRY_LATER));

		}
	}*/
}
