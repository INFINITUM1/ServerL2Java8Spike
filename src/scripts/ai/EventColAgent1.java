package scripts.ai;

import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class EventColAgent1 extends L2NpcInstance
{
	private static String htmPath = "data/html/events/";
	
	public EventColAgent1(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
    public void onBypassFeedback(L2PcInstance talker, String command)
    {
		if(command.startsWith("event"))
		{
			if(getItemCount(talker, 6402) >= 1)
				showPage(talker, "event_col_agent1_q0996_05.htm");
			else if(getItemCount(talker, 6401) >= 1)
				showPage(talker, "event_col_agent1_q0996_04.htm");
			else if(getItemCount(talker, 6400) >= 1)
				showPage(talker, "event_col_agent1_q0996_03.htm");
			else if(getItemCount(talker, 6399) >= 1)
				showPage(talker, "event_col_agent1_q0996_02.htm");
			else
				showPage(talker, "event_col_agent1_q0996_01.htm");
		}
		else
			super.onBypassFeedback(talker, command);
        talker.sendActionFailed();
    }
	
	private void showPage(L2PcInstance talker, String page)
	{
		showChatWindow(talker, htmPath + page);
	}
}
