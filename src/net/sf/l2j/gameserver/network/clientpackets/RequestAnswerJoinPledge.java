
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.JoinPledge;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAdd;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestAnswerJoinPledge extends L2GameClientPacket
{
	private int _answer;

	@Override
	protected void readImpl()
	{
		_answer  = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;
			
		if(System.currentTimeMillis() - player.gCPAF() < 100)
			return;
		player.sCPAF();

		L2PcInstance requestor = player.getTransactionRequester();
		
		player.setTransactionRequester(null);
		
        if (requestor == null)
        	return;
			
		requestor.setTransactionRequester(null);

		if(requestor.getClan() == null)
			return;

		if(player.getTransactionType() != TransactionType.CLAN || player.getTransactionType() != requestor.getTransactionType())
			return;

		if (_answer == 0)
		{
			player.sendPacket(SystemMessage.id(SystemMessageId.YOU_DID_NOT_RESPOND_TO_S1_CLAN_INVITATION).addString(requestor.getName()));
			requestor.sendPacket(SystemMessage.id(SystemMessageId.S1_DID_NOT_RESPOND_TO_CLAN_INVITATION).addString(player.getName()));
		}
		else
		{
	        L2Clan clan = requestor.getClan();
			// we must double check this cause during response time conditions can be changed, i.e. another player could join clan
			if (clan.checkClanJoinCondition(requestor, player, player.getPledgeType()))
	        {
				player.sendPacket(new JoinPledge(requestor.getClanId()));
				
				player.setPledgeType(player.getPledgeType());
				if(player.getPledgeType() == L2Clan.SUBUNIT_ACADEMY)
				{
					player.setPowerGrade(9); // adademy
					player.setLvlJoinedAcademy(player.getLevel());
				}
				else
				{
					player.setPowerGrade(5); // new member starts at 5, not confirmed
				}
				clan.addClanMember(player);
				player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));

				player.sendPacket(Static.ENTERED_THE_CLAN);
				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(player), player);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(SystemMessage.id(SystemMessageId.S1_HAS_JOINED_CLAN).addString(player.getName()));
				// this activates the clan tab on the new member
				player.sendPacket(new PledgeShowMemberListAll(clan, player));
				player.setClanJoinExpiryTime(0);
				player.broadcastUserInfo();
			}
		}
		
		requestor.setTransactionType(TransactionType.NONE);
		player.setTransactionType(TransactionType.NONE);
	}
}
