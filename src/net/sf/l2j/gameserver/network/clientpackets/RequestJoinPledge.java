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

import java.nio.BufferUnderflowException;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinPledge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestJoinPledge extends L2GameClientPacket
{
	private int _target;
	private int _pledgeType;

	@Override
	protected void readImpl()
	{
		try
		{
			_target  = readD();
			_pledgeType = readD();
		}
		catch (BufferUnderflowException e)
		{
			_target  = 0;
			_pledgeType = 0;
		}
	}

	@Override
	protected void runImpl()
	{
		if (_target == 0)
			return;

		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;

		if(System.currentTimeMillis() - player.gCPAE() < 500)
			return;
		player.sCPAE();
		
		if(player.getClan() == null)
        {
			player.sendPacket(Static.YOU_ARE_NOT_A_CLAN_MEMBER);
            return;
        }
		
		L2Object oTarget = L2World.getInstance().findObject(_target);
        if (oTarget == null || !(oTarget.isPlayer()) || (oTarget.getObjectId() == player.getObjectId()))
		{
			player.sendPacket(Static.TARGET_IS_INCORRECT);
			return;
		}
        L2PcInstance target = oTarget.getPlayer();
		
		if (target.isAlone())
		{
			player.sendPacket(Static.LEAVE_ALONR);
			player.sendActionFailed();
			return;
		}
		
        L2Clan clan = player.getClan();
        if (!clan.checkClanJoinCondition(player, target, _pledgeType))
        	return;
		
		if(target.isTransactionInProgress())
		{
			player.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(target.getName()));
			return;
		}
			
		if(player.isTransactionInProgress())
		{
			player.sendPacket(Static.WAITING_FOR_ANOTHER_REPLY);
			return;
		}

		target.setTransactionRequester(player, System.currentTimeMillis() + 10000);
		target.setTransactionType(TransactionType.CLAN);
		target.setPledgeType(_pledgeType);
		player.setTransactionRequester(target, System.currentTimeMillis() + 10000);
		player.setTransactionType(TransactionType.CLAN);
		
		player.sendMessage("Вы пригласили " + target.getName() + " в клан");
    	target.sendPacket(new AskJoinPledge(player.getObjectId(), player.getClan().getName()));
		target.sendPacket(SystemMessage.id(SystemMessageId.S1_HAS_INVITED_YOU_TO_JOIN_THE_CLAN_S2).addString(player.getName()).addString(player.getClan().getName()));
	}

 	public int getPledgeType()
 	{
 		return _pledgeType;
 	}
}
