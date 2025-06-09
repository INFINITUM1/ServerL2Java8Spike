
package net.sf.l2j.gameserver.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World; 
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.TimeLogger;

public class Online
{
	private static final Online _instance = new Online();
	
	private int _maxOnline = 0; 
	private String _maxOnlineDate = "";
	
    private Online()
    {
		//
    }

    public static Online getInstance()
    {
        return _instance;
    }
		
	class announceOnline implements Runnable
	{
		announceOnline()
		{
		}

		public void run()
		{			
			try
			{
				Announcements an = Announcements.getInstance();
				int currentOnline = getCurrentOnline();
				int offTraders = getOfflineTradersOnline();
				int maxOnline = getMaxOnline();
				
				if (Config.SONLINE_SHOW_OFFLINE)
					an.announceToAll("Игроков онлайн: " + currentOnline + "; Оффлайн торговцев: " + offTraders);
				else
					an.announceToAll("Игроков онлайн: " + currentOnline);
					
				if (Config.SONLINE_SHOW_MAXONLINE)
				{
					an.announceToAll("Максимальный онлайн был: " + maxOnline);
					if (Config.SONLINE_SHOW_MAXONLINE_DATE)
						an.announceToAll("это было: " + getMaxOnlineDate());
				}
				Runtime r = Runtime.getRuntime();
				System.out.println(TimeLogger.getLogTime()+"Online: current online " + currentOnline + "; offline traders: " + offTraders + "; Used memory: " + ((r.totalMemory() - r.freeMemory()) / 1024 / 1024) + "MB.");

				//едем дальше
				ThreadPoolManager.getInstance().scheduleGeneral(new announceOnline(), Config.SONLINE_ANNOUNCE_DELAY);
			}
			catch (Throwable e){}
		}
	}
	
	public int getCurrentOnline()
	{
		return L2World.getInstance().getAllPlayersCount();
	}
	
	public int getOfflineTradersOnline()
	{
		return L2World.getInstance().getAllOfflineCount();
	}
	
	public int getMaxOnline()
	{
		return _maxOnline;
	}
	
	public String getMaxOnlineDate()
	{
		return _maxOnlineDate;
	}
	
	public void loadMaxOnline()
	{	
		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.get();
			statement = con.prepareStatement("SELECT * FROM `z_maxonline` LIMIT 1");
			rset = statement.executeQuery();
			if (rset.next())
			{
				_maxOnline = rset.getInt("online");
				_maxOnlineDate = rset.getString("date");
				System.out.println("Online: Max online was " + _maxOnline);
			}
		}
		catch (Exception e)
		{
			System.out.println("Online: can't load maxOnline");
		}
		finally 
		{ 
			Close.CSR(con, statement, rset);
		}
		
		//кричим через каждые Config.ONLINE_ANNOUNCE_DELAY об онлайне
		if (Config.SONLINE_ANNOUNE)
			ThreadPoolManager.getInstance().scheduleGeneral(new announceOnline(), Config.SONLINE_ANNOUNCE_DELAY);
	}
	
	public void checkMaxOnline()
	{
		if (getCurrentOnline() > _maxOnline)
			updateMaxOnline();
	}
	
	private long _lastUpdate = 0;
	private void updateMaxOnline()
	{			
		int newInline = getCurrentOnline();
		if (newInline < _maxOnline)
			return;
			
		String newDate = getDate();
		
		if(System.currentTimeMillis() - _lastUpdate < 60000)
		{
			_maxOnline = newInline;
			_maxOnlineDate = newDate;
			return;
		}

		_lastUpdate = System.currentTimeMillis();
		Connect con = null;
		PreparedStatement statement = null;
        try
        {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE `z_maxonline` SET `online`=?,`date`=?");
            statement.setInt(1, newInline);
            statement.setString(2, newDate);
            statement.execute();
        }
        catch (Exception e) 
		{
            System.out.println("Online: can't set new maxOnline");
        }
        finally 
		{
            Close.CS(con, statement);
        }
		_maxOnline = newInline;
		_maxOnlineDate = newDate;
		//System.out.println("Online: new max online " + _maxOnline + "!");
	}
	
	private static String getDate()
	{
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy в HH:mm");
		
		return sdf.format(date);
	}
}
