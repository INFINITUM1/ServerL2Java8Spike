package scripts.ai;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager.BossInfo;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.jython.QuestJython;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class EventColAgent2 extends L2NpcInstance
{
	private static String htmPath = "data/html/events/";
	
	public EventColAgent2(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
    public void onBypassFeedback(L2PcInstance talker, String command)
    {
		if(command.startsWith("event"))
		{
			int reply = Integer.parseInt(command.substring(5).trim());
			switch(reply)
			{
				case 1:
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
					break;
				case 2:	
					if( getItemCount(talker,6401) >= 1)
					{
						if( getItemCount(talker,6393) >= 40 )
						{
							showPage(talker,"event_col_agent2_q0996_11.htm");
							talker.setEventColNumber(Rnd.get(1,2));
						}
						else
							showPage(talker,"event_col_agent2_q0996_12.htm");
					}
					else if( getItemCount(talker,6400) >= 1)
					{
						if( getItemCount(talker,6393) >= 20 )
						{
							showPage(talker,"event_col_agent2_q0996_11.htm");
							talker.setEventColNumber(Rnd.get(1,2));
						}
						else
							showPage(talker,"event_col_agent2_q0996_12.htm");
					}
					else if( getItemCount(talker,6399) >= 1)
					{
						if( getItemCount(talker,6393) >= 10 )
						{
							showPage(talker,"event_col_agent2_q0996_11.htm");
							talker.setEventColNumber(Rnd.get(1,2));
						}
						else
							showPage(talker,"event_col_agent2_q0996_12.htm");
					}
					else if( getItemCount(talker,6393) >= 5 )
					{
						showPage(talker,"event_col_agent2_q0996_11.htm");
						talker.setEventColNumber(Rnd.get(1,2));
					}
					else
						showPage(talker,"event_col_agent2_q0996_12.htm");
					break;
				case 3:	
					if( getItemCount(talker,6401) >= 1)
					{
						if( getItemCount(talker,6393) >= 40 )
						{
							if( talker.getEventColNumber() == 1 )
							{
								if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
								{
									talker.setQuestLastReward();
									giveItem(talker,6402,1);
									deleteItem(talker,6401,1);
									deleteItem(talker,6393,40);
									showPage(talker,"event_col_agent2_q0996_24.htm");
								}
							}
							else if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
							{
								talker.getQuestLastReward();
								deleteItem(talker,6393,40);
								showPage(talker,"event_col_agent2_q0996_25.htm");
							}
						}
						else
							showPage(talker,"event_col_agent2_q0996_26.htm");
						talker.setEventColNumber(0);
					}
					else if( getItemCount(talker,6400) >= 1)
					{
						if( getItemCount(talker,6393) >= 20 )
						{
							if( talker.getEventColNumber() == 1 )
							{
								if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
								{
									talker.setQuestLastReward();
									giveItem(talker,6401,1);
									deleteItem(talker,6400,1);
									deleteItem(talker,6393,20);
									showPage(talker,"event_col_agent2_q0996_23.htm");
								}
							}
							else if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
							{
								talker.setQuestLastReward();
								deleteItem(talker,6393,20);
								showPage(talker,"event_col_agent2_q0996_25.htm");
							}
						}
						else
						{
							showPage(talker,"event_col_agent2_q0996_26.htm");
						}
						talker.setEventColNumber(0);
					}
					else if( getItemCount(talker,6399) >= 1)
					{
						if( getItemCount(talker,6393) >= 10 )
						{
							if( talker.getEventColNumber() == 1 )
							{
								if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
								{
									talker.setQuestLastReward();
									giveItem(talker,6400,1);
									deleteItem(talker,6399,1);
									deleteItem(talker,6393,10);
									showPage(talker,"event_col_agent2_q0996_22.htm");
								}
							}
							else if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
							{
								talker.setQuestLastReward();
								deleteItem(talker,6393,10);
								showPage(talker,"event_col_agent2_q0996_25.htm");
							}
						}
						else
							showPage(talker,"event_col_agent2_q0996_26.htm");
						talker.setEventColNumber(0);
					}
					else if( getItemCount(talker,6393) >= 5 )
					{
						if( talker.getEventColNumber() == 1 )
						{
							if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
							{
								talker.setQuestLastReward();
								giveItem(talker,6399,1);
								deleteItem(talker,6393,5);
								showPage(talker,"event_col_agent2_q0996_21.htm");
							}
						}
						else if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
						{
							talker.setQuestLastReward();
							deleteItem(talker,6393,5);
							showPage(talker,"event_col_agent2_q0996_25.htm");
						}
					}
					else
						showPage(talker,"event_col_agent2_q0996_26.htm");
					talker.setEventColNumber(0);
					break;
				case 4:
					if( getItemCount(talker,6401) >= 1)
					{
						if( getItemCount(talker,6393) >= 40 )
						{
							if( talker.getEventColNumber() == 2 )
							{
								if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
								{
									talker.setQuestLastReward();
									giveItem(talker,6402,1);
									deleteItem(talker,6401,1);
									deleteItem(talker,6393,40);
									showPage(talker,"event_col_agent2_q0996_34.htm");
								}
							}
							else if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
							{
								talker.setQuestLastReward();
								deleteItem(talker,6393,40);
								showPage(talker,"event_col_agent2_q0996_35.htm");
							}
						}
						else
						{
							showPage(talker,"event_col_agent2_q0996_26.htm");
						}
						talker.setEventColNumber(0);
					}
					else if( getItemCount(talker,6400) >= 1)
					{
						if( getItemCount(talker,6393) >= 20 )
						{
							if( talker.getEventColNumber() == 2 )
							{
								if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
								{
									talker.setQuestLastReward();
									giveItem(talker,6401,1);
									deleteItem(talker,6400,1);
									deleteItem(talker,6393,20);
									showPage(talker,"event_col_agent2_q0996_33.htm");
								}
							}
							else if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
							{
								talker.setQuestLastReward();
								deleteItem(talker,6393,20);
								showPage(talker,"event_col_agent2_q0996_35.htm");
							}
						}
						else
						{
							showPage(talker,"event_col_agent2_q0996_26.htm");
						}
						talker.setEventColNumber(0);
					}
					else if( getItemCount(talker,6399) >= 1)
					{
						if( getItemCount(talker,6393) >= 10 )
						{
							if( talker.getEventColNumber() == 2 )
							{
								if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
								{
									talker.setQuestLastReward();
									giveItem(talker,6400,1);
									deleteItem(talker,6399,1);
									deleteItem(talker,6393,10);
									showPage(talker,"event_col_agent2_q0996_32.htm");
								}
							}
							else if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
							{
								talker.setQuestLastReward();
								deleteItem(talker,6393,10);
								showPage(talker,"event_col_agent2_q0996_35.htm");
							}
						}
						else
						{
							showPage(talker,"event_col_agent2_q0996_26.htm");
						}
						talker.setEventColNumber(0);
					}
					else if( getItemCount(talker,6393) >= 5 )
					{
						if( talker.getEventColNumber() == 2 )
						{
							if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
							{
								talker.setQuestLastReward();
								giveItem(talker,6399,1);
								deleteItem(talker,6393,5);
								showPage(talker,"event_col_agent2_q0996_31.htm");
							}
						}
						else if( ( System.currentTimeMillis() - talker.getQuestLastReward() ) > 1000 )
						{
							talker.setQuestLastReward();
							deleteItem(talker,6393,5);
							showPage(talker,"event_col_agent2_q0996_35.htm");
						}
					}
					else
						showPage(talker,"event_col_agent2_q0996_26.htm");
					talker.setEventColNumber(0);
					break;
			}
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
