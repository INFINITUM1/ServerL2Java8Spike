package net.sf.l2j.gameserver.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.text.SimpleDateFormat;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

public final class WebStat
{
    private static final Logger _log = Logger.getLogger(WebStat.class.getName());

	private static WebStat _instance;
	public static WebStat getInstance()
	{
		return _instance;
	}
	public static void init()
	{
		_instance = new WebStat();
		_instance.load();
	}

	public WebStat()
	{
	}

	//
	private String _date = "";
	
	//аккаунты
	private int _curAccs = 0;
	private int _newAccs = 0;
	private int _totalAccs = 0;

	//коннекты
	private int _totalGame = 0;
	private int _totalLogin = 0;
	private Map<String, FastList<Integer>> _consGame = new ConcurrentHashMap<String, FastList<Integer>>();
	private Map<String, FastList<Integer>> _consLogin = new ConcurrentHashMap<String, FastList<Integer>>();
	// для отложенной записи в бд
	private Map<Integer, FastList<Integer>> _consTemp = new ConcurrentHashMap<Integer, FastList<Integer>>();
	
	//онлайн
	private int _onlineMax = 0;
	private String _onlineToday = "";

	//потребление памяти
	private int _memoryMax = 0;
	private String _memoryToday = "";
	
	//пвп/пк
    private ConcurrentLinkedQueue<Murder> _killsTemp = new ConcurrentLinkedQueue<Murder>();
	
	//заточка
    private ConcurrentLinkedQueue<EnchantResult> _enchantTemp = new ConcurrentLinkedQueue<EnchantResult>();

	private void load()
	{
		_consTemp.put(0, new FastList<Integer>());
		_consTemp.put(1, new FastList<Integer>());
		LoginServerThread.getInstance().updateWebStatAccounts();

		_date = getDate();
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			con.setTransactionIsolation(1);

			_totalAccs = LoginServerThread.getInstance().getTotalAccs();

			String total = "0";
			String value = "";
			st = con.prepareStatement("SELECT value, total FROM `z_stat_values` WHERE `date` = ?");
			st.setString(1, _date);
			rs = st.executeQuery();
			while (rs.next()) 
			{
				total = rs.getString("total");
				value = rs.getString("value");

				if (value.equalsIgnoreCase("accounts"))
					_curAccs = Integer.parseInt(total);
				else if (value.equalsIgnoreCase("game"))
					_totalGame = Integer.parseInt(total);
				else if (value.equalsIgnoreCase("login"))
					_totalLogin = Integer.parseInt(total);
				else if (value.equalsIgnoreCase("online"))
					_onlineToday = total;
				else if (value.equalsIgnoreCase("memory"))
					_memoryToday = total;
			}
			Close.SR(st, rs);

			if (_curAccs > 0)
			{
				_totalAccs -= _curAccs;
				_totalAccs = Math.max(_totalAccs, 0);
			}

			//
			FastList<Integer> login = new FastList<Integer>();
			st = con.prepareStatement("SELECT ip FROM `z_stat_login` WHERE `date` = ?");
			st.setString(1, _date);
			rs = st.executeQuery();
			while (rs.next()) 
			{
				login.add(rs.getInt("ip"));
			}
			_consLogin.put(_date, login);
			Close.SR(st, rs);

			FastList<Integer> game = new FastList<Integer>();
			st = con.prepareStatement("SELECT ip FROM `z_stat_game` WHERE `date` = ?");
			st.setString(1, _date);
			rs = st.executeQuery();
			while (rs.next()) 
			{
				game.add(rs.getInt("ip"));
			}
			_consGame.put(_date, game);
		}
		catch (SQLException e)
		{
			_log.warning("WebStat [ERROR]: load()" + e);
		}
		finally
		{
			Close.CSR(con, st, rs);
		}

		ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask(), Config.WEBSTAT_INTERVAL);
		ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask2(), Config.WEBSTAT_INTERVAL2);

		if (Config.WEBSTAT_INTERVAL3 > 0)
			ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask3(), Config.WEBSTAT_INTERVAL3);
	}

	class UpdateTask implements Runnable
	{
		UpdateTask()
		{
		}

		public void run()
		{
			LoginServerThread.getInstance().updateWebStatAccounts();

			Connect con = null;
			PreparedStatement st = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				con.setAutoCommit(false);

				_newAccs = LoginServerThread.getInstance().getTotalAccs();
				_curAccs = _newAccs - _totalAccs;
				_curAccs = Math.max(_curAccs, 0);
				
				Runtime r = Runtime.getRuntime();
				int onlineCurrent = L2World.getInstance().getAllPlayersCount();
				int memoryCurrrent = (int) ((r.totalMemory() - r.freeMemory()) / 1024 / 1024);

				st = con.prepareStatement("REPLACE INTO z_stat_values (date,value,total) VALUES (?,?,?)");
				//accounts
				st.setString(1, _date);
				st.setString(2, "accounts");
				st.setInt(3, _curAccs);
				st.addBatch();
				//game
				st.setString(1, _date);
				st.setString(2, "game");
				st.setInt(3, _totalGame);
				st.addBatch();
				//login
				st.setString(1, _date);
				st.setString(2, "login");
				st.setInt(3, _totalLogin);
				st.addBatch();
				//online_now
				st.setString(1, _date);
				st.setString(2, "online_now");
				st.setInt(3, onlineCurrent);
				st.addBatch();
				//memory_now
				st.setString(1, _date);
				st.setString(2, "memory_now");
				st.setInt(3, memoryCurrrent);
				st.addBatch();

				st.executeBatch();
				Close.S(st);

				Integer intIp = null;
				FastList<Integer> consTemp = _consTemp.get(0);
				if (!consTemp.isEmpty())
				{
					st = con.prepareStatement("REPLACE INTO z_stat_login (date,ip) VALUES (?,?)");
					for (FastList.Node<Integer> n = consTemp.head(), end = consTemp.tail(); (n = n.getNext()) != end;) 
					{
						intIp = n.getValue(); // No typecast necessary.
						if (intIp == null)
							continue;

						st.setString(1, _date);
						st.setInt(2, intIp);
						st.addBatch();
					}
					st.executeBatch();
					Close.S(st);
					_consTemp.get(0).clear();
				}

				consTemp = _consTemp.get(1);
				if (!consTemp.isEmpty())
				{
					st = con.prepareStatement("REPLACE INTO z_stat_game (date,ip) VALUES (?,?)");
					for (FastList.Node<Integer> n = consTemp.head(), end = consTemp.tail(); (n = n.getNext()) != end;) 
					{
						intIp = n.getValue(); // No typecast necessary.
						if (intIp == null)
							continue;

						st.setString(1, _date);
						st.setInt(2, intIp);
						st.addBatch();
					}
					st.executeBatch();
					Close.S(st);
					_consTemp.get(1).clear();
				}

				con.commit();
			}
			catch (SQLException e)
			{
				_log.warning("WebStat [ERROR]: UpdateTask() " + e);
			}
			finally
			{
				Close.CS(con, st);
			}

			if (!_date.equalsIgnoreCase(getDate()))
			{
				_date = getDate();
				//
				_curAccs = 0;
				_totalGame = 0;
				_totalLogin = 0;
				//
				_consGame.clear();
				_consLogin.clear();
				_consGame.put(_date, new FastList<Integer>());
				_consLogin.put(_date, new FastList<Integer>());
				//
				_consTemp.clear();
				_consTemp.put(0, new FastList<Integer>());
				_consTemp.put(1, new FastList<Integer>());
				//
				_onlineMax = 0;
				_onlineToday = "";
				//
				_memoryMax = 0;
				_memoryToday = "";
			}
			ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask(), Config.WEBSTAT_INTERVAL);
		}
	}

	class UpdateTask2 implements Runnable
	{
		UpdateTask2()
		{
		}

		public void run()
		{
			int onlineCurrent = L2World.getInstance().getAllPlayersCount();
			_onlineMax = Math.max(_onlineMax, onlineCurrent);
			_onlineToday += getTime() + " " + onlineCurrent + ";";
			
			Runtime r = Runtime.getRuntime();
			int memoryCurrrent = (int) ((r.totalMemory() - r.freeMemory()) / 1024 / 1024);
			_memoryMax = Math.max(_memoryMax, memoryCurrrent);
			_memoryToday += getTime() + " " + memoryCurrrent + ";";

			Connect con = null;
			PreparedStatement st = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				con.setAutoCommit(false);

				st = con.prepareStatement("REPLACE INTO z_stat_values (date,value,total) VALUES (?,?,?)");
				//online_max
				st.setString(1, _date);
				st.setString(2, "online_max");
				st.setInt(3, _onlineMax);
				st.addBatch();
				//online
				st.setString(1, _date);
				st.setString(2, "online");
				st.setString(3, _onlineToday);
				st.addBatch();
				//memory_max
				st.setString(1, _date);
				st.setString(2, "memory_max");
				st.setInt(3, _memoryMax);
				st.addBatch();
				//memory
				st.setString(1, _date);
				st.setString(2, "memory");
				st.setString(3, _memoryToday);
				st.addBatch();
				st.executeBatch();
				Close.S(st);
				con.commit();
			}
			catch (SQLException e)
			{
				_log.warning("WebStat [ERROR]: UpdateTask2() " + e);
			}
			finally
			{
				Close.CS(con, st);
			}
			ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask2(), Config.WEBSTAT_INTERVAL2);
		}
	}

	class UpdateTask3 implements Runnable
	{
		UpdateTask3()
		{
		}

		public void run()
		{

			Connect con = null;
			PreparedStatement st = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				con.setAutoCommit(false);

				//pvp/pk
				if (!_killsTemp.isEmpty())
				{
					st = con.prepareStatement("INSERT INTO `z_stat_kills` (`date`, `killer`, `victim`) VALUES (?, ?, ?)");
					for (Murder murd : _killsTemp)
					{
						if (murd == null)
							continue;

						st.setString(1, _date + " " + getTimeS());
						st.setString(2, murd.killer);
						st.setString(3, murd.victim);
						st.addBatch();
					}
					st.executeBatch();
					Close.S(st);
					_killsTemp.clear();
				}

				//enchant
				if (!_enchantTemp.isEmpty())
				{
					st = con.prepareStatement("INSERT INTO `z_stat_enchant` (`date`, `name`, `item`, `enchant`, `success`) VALUES (?, ?, ?, ?, ?)");
					for (EnchantResult enres : _enchantTemp)
					{
						if (enres == null)
							continue;

						st.setString(1, _date + " " + getTimeS());
						st.setString(2, enres.name);
						st.setString(3, enres.item);
						st.setInt(4, enres.ench);
						st.setInt(5, enres.sucess);
						st.addBatch();
					}
					st.executeBatch();
					Close.S(st);
					_enchantTemp.clear();
				}

				con.commit();
			}
			catch (SQLException e)
			{
				_log.warning("WebStat [ERROR]: UpdateTask3() " + e);
			}
			finally
			{
				Close.CS(con, st);
			}
			ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask3(), Config.WEBSTAT_INTERVAL3);
		}
	}

	private static String getDate()
	{
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(date);
	}

	private static String getTime()
	{
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		return sdf.format(date);
	}

	private static String getTimeS()
	{
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		return sdf.format(date);
	}

	public void addLogin(String ip)
	{
		int intIp = Util.ipToInt(ip);
		if (_consLogin.get(_date).contains(intIp))
			return;

		_totalLogin += 1;
		_consLogin.get(_date).add(intIp);

		_consTemp.get(0).add(intIp);
	}

	public void addGame(String ip)
	{
		if (ip.equalsIgnoreCase("Disconnected"))
			return;

		int intIp = Util.ipToInt(ip);
		if (_consGame.get(_date).contains(intIp))
			return;

		_totalGame += 1;
		_consGame.get(_date).add(intIp);

		_consTemp.get(1).add(intIp);
	}

	//
    public static class Murder
    {
        public String killer;
        public String victim;

        Murder(String killer, String victim)
        {
            this.killer = killer;
            this.victim = victim;
        }
    }

	public void addKill(String killer, String victim)
	{
		_killsTemp.add((new Murder(killer, victim)));
	}

	//
    public static class EnchantResult
    {
        public String name;
        public String item;
        public int ench;
        public int sucess;

        EnchantResult(String name, String item, int ench, int sucess)
        {
            this.name = name;
            this.item = item;
            this.ench = ench;
            this.sucess = sucess;
        }
    }

	public void addEnchant(String name, String item, int ench, int sucess)
	{
		_enchantTemp.add(new EnchantResult(name, item, ench, sucess));
	}
}
