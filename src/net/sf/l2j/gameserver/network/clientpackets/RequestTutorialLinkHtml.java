package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;

import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;

public class RequestTutorialLinkHtml extends L2GameClientPacket
{
	// format: cS

	String _bypass;

	@Override
	public void readImpl()
	{
		try
		{
			_bypass = readS();
		}
		catch (BufferUnderflowException e)
		{
			_bypass = "no";
		}
	}

	@Override
	public void runImpl()
	{
		if (_bypass.equalsIgnoreCase("no"))
			return;
		
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		if(System.currentTimeMillis() - player.gCPAR() < 200)
			return;

		player.sCPAR();

		Quest q = QuestManager.getInstance().getQuest(255);
		if(q != null)
			player.processQuestEvent(q.getName(), _bypass);
	}

	@Override
	public String getType()
	{
		return "C.TutorialLinkHtml";
	}
}