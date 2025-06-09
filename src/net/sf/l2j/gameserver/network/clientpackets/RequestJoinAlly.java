
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinAlly;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestJoinAlly extends L2GameClientPacket
{
	private int _id;

	@Override
	protected void readImpl()
	{
		_id = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null || player.getAllyId() == 0)
			return;
			
		if(player.getClan() == null)
        {
			player.sendPacket(Static.YOU_ARE_NOT_A_CLAN_MEMBER);
            return;
        }
		
		L2Object oTarget = L2World.getInstance().findObject(_id);
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
		
        if (!clan.checkAllyJoinCondition(player, target))
        	return;
			
		if(player.isTransactionInProgress())
		{
			player.sendPacket(Static.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		if(target.isTransactionInProgress())
		{
			player.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(target.getName()));
			return;
		}

		target.setTransactionRequester(player, System.currentTimeMillis() + 10000);
		target.setTransactionType(TransactionType.ALLY);
		player.setTransactionRequester(target, System.currentTimeMillis() + 10000);
		player.setTransactionType(TransactionType.ALLY);
		//leader of alliance request an alliance.
		target.sendPacket(new AskJoinAlly(player.getObjectId(), player.getClan().getAllyName()));
		target.sendPacket(SystemMessage.id(SystemMessageId.S2_ALLIANCE_LEADER_OF_S1_REQUESTED_ALLIANCE).addString(player.getClan().getAllyName()).addString(player.getName()));
	    return;
	}
}

