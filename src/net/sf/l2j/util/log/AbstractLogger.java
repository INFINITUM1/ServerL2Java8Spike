package net.sf.l2j.util.log;

import java.util.logging.Logger;

import net.sf.l2j.Config;

public class AbstractLogger
{
	private static DefaultLogger log;
	
	public static void init()
	{
		if (Config.CONSOLE_ADVANCED && System.getProperty("os.name").toLowerCase().indexOf("windows") > -1)
			log = PwLogger.init();
		else
			log = new SunLogger("pw2");
	}
	
	/*public AbstractLogger(String name)
	{		
		if (Config.CONSOLE_ADVANCED)
		{
			log = PwLogger.init();
			System.out.println("####1");
		}
		else
		{
			log = new SunLogger(name);
			System.out.println("####2");
		}
	}	*/

	public static void setLoaded()
	{
		PwLogger.setLoaded();
	}
	public static void startRefresTask()
	{
		PwLogger.startRefresTask();
	}

	public static Logger getLogger(String name)
	{
		return log.get(name);
	}
}