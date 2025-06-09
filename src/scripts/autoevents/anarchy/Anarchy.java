package scripts.autoevents.anarchy;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;

public final class Anarchy
{
	private static Calendar _point = null;
	
	static enum EventState 
	{
		WAIT,
		BATTLE
	}
	private static EventState _state = EventState.WAIT;
	
	private static Anarchy _event = new Anarchy();
	public static void init()
	{
		//_event = new Anarchy();
		_event.load();
	}
	
	public static Anarchy getEvent()
	{
		return _event;
	}

	public void load() 
	{
		//ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), 1000);
		ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), getNextStart());
		String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(_point.getTime());
		System.out.println("EventManager: Anarchy, start: " + date + ".");
	}
	
	private static long getNextStart()
	{
		Calendar tomorrow = new GregorianCalendar();
		//tomorrow.set(Calendar.DAY_OF_WEEK, Config.ANARCHY_DAY);                
		
		int daysToChange = getDaysToPeriodChange(tomorrow);
        if (daysToChange == 7)
            if (tomorrow.get(Calendar.HOUR_OF_DAY) < Config.ANARCHY_HOUR)
                daysToChange = 0;
            else if (tomorrow.get(Calendar.HOUR_OF_DAY) == Config.ANARCHY_HOUR && tomorrow.get(Calendar.MINUTE) < 0)
                daysToChange = 0;

                // Otherwise...
            if (daysToChange > 0)
                tomorrow.add(Calendar.DATE, daysToChange);
		//tomorrow.add(Calendar.DAY_OF_WEEK_IN_MONTH, Config.ANARCHY_DAY);
		Calendar result = new GregorianCalendar(
			tomorrow.get(Calendar.YEAR),
			tomorrow.get(Calendar.MONTH),
			//tomorrow.get(Calendar.DAY_OF_WEEK),
			tomorrow.get(Calendar.DATE),
			Config.ANARCHY_HOUR,
			0
		);
		_point = result;
		return (result.getTimeInMillis() - System.currentTimeMillis());
	}	

	private static int getDaysToPeriodChange(Calendar tomorrow)
    {
		int numDays = tomorrow.get(Calendar.DAY_OF_WEEK) - Config.ANARCHY_DAY;

		if (numDays < 0)
			return 0 - numDays;

        return 7 - numDays;
	}

	public class StartTask implements Runnable
	{
		public void run()
		{
			if(_state == EventState.WAIT)
			{
				_state = EventState.BATTLE;

				announce(Static.ANARCHY_STARTED);
				System.out.println("EventManager: Anarchy, started.");
				ThreadPoolManager.getInstance().scheduleGeneral(new StopTask(), Config.ANARCHY_DELAY);
			}
		}
	}

	public class StopTask implements Runnable
	{
		public void run()
		{
			if(_state == EventState.BATTLE)
			{
				_state = EventState.WAIT;

				announce(Static.ANARCHY_FINISHED);
				System.out.println("EventManager: Anarchy, stopped.");
				ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), getNextStart());
				String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(_point.getTime());
				System.out.println("EventManager: Anarchy, next start: " + date + ".");
			}
		}
	}

	public boolean isInBattle()
	{
		return (_state == EventState.BATTLE);
	}

	public boolean isInBattle(int townId)
	{
		if (_state == EventState.WAIT)
			return false;
		
		return (!Config.ANARCHY_TOWNS.contains(townId));
	}

	private void announce(String text)
	{	
		Announcements.getInstance().announceToAll(text);
	}
}
