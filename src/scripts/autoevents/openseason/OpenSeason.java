package scripts.autoevents.openseason;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExMailArrived;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Rnd;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

public final class OpenSeason
{
	/*
CREATE TABLE `z_openseason_players` (
  `team` decimal(11,0) DEFAULT NULL DEFAULT '0',
  `obj_id` decimal(11,0) NOT NULL DEFAULT '0',
  `points` decimal(11,0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`team`, `obj_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `z_openseason_points` (
  `team` decimal(11,0) DEFAULT NULL DEFAULT '0',
  `points` decimal(11,0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`team`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
	*/
	
	private static final Logger _log = Logger.getLogger(OpenSeason.class.getName());
	private static EventManager _event = EventManager.getInstance();
	//настройки времени
	private static final long NEXTTIME = TimeUnit.HOURS.toMillis(Config.OS_NEXT);
	private static final long RESTART = TimeUnit.MINUTES.toMillis(Config.OS_RESTART);
	private static final long BATTLE_PERIOD = TimeUnit.DAYS.toMillis(Config.OS_BATTLE);
	private static final long FAIL_TIME = TimeUnit.MINUTES.toMillis(Config.OS_FAIL_NEXT);
	private static final long REG_TIME = TimeUnit.MINUTES.toMillis(Config.OS_REGTIME);
	private static final long ANNOUNCE_DELAY = TimeUnit.MINUTES.toMillis(Config.OS_ANNDELAY);
	
	private ScheduledFuture<?> _osTask = null;
	private static EventState _state = EventState.WAIT;

	private static final FastMap<Integer, FastList<Integer>> _teams = new FastMap<Integer, FastList<Integer>>();
	private static FastMap<Integer, Integer> _points = new FastMap<Integer, Integer>();
	private static FastList<String> _teamNames = new FastList<String>();
	private static FastList<EventReward> _rewards = Config.OS_REWARDS;
	
	static enum EventState 
	{
		WAIT,
		REG,
		BATTLE
	}
	
    private static OpenSeason _instance;
    public static OpenSeason getEvent()
    {
        return _instance;
    }
	
	public static void init()
	{
        _instance = new OpenSeason();
        _instance.load();
	}
	
	public void load()
	{
		_teams.clear();
		_teams.put(0, new FastList<Integer>());
		_teams.put(1, new FastList<Integer>());
		
		_points.clear();
		_points.put(0, 0);
		_points.put(1, 0);
		
		_teamNames.clear();
		_teamNames.add("Охотники");
		_teamNames.add("Лесная братва");
		
		checkTimer();
	}

	public void checkTimer()
	{
		if (_event.GetDBValue("OpenSeason", "nextStart") == 0)
		{
			long nextStart =  _event.GetDBValue("OpenSeason", "nextStart") - System.currentTimeMillis();
			if (nextStart < RESTART)
				nextStart = RESTART;

			_osTask = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), nextStart);
			System.out.println("EventManager: Open Season, start after " + (nextStart / 60000) + " min.");
		}
		else
		{
			_state = EventState.BATTLE;
			_osTask = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), (_event.GetDBValue("OpenSeason", "nextFinish") - System.currentTimeMillis()));
			restorePlayers();
			restorePoints();
		}
	}
	
	public class StartTask implements Runnable
	{
		public void run()
		{
			if(_state == EventState.WAIT)
				startEvent();
		}
	}
	
	private void restorePoints()
	{
		_points.clear();
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT team, points FROM `z_openseason_points`");
			rs = st.executeQuery();
			while(rs.next())
			{
				int team = (rs.getInt("team") - 1);
				int points = rs.getInt("points");
				_points.put(team, points);
			}
		}
		catch(Exception e)
		{
			_log.warning("EventManager: Open Season, could not restore points." + e);
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
	}
	
	private void restorePlayers()
	{
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT team, obj_id FROM `z_openseason_players`");
			rs = st.executeQuery();
			while(rs.next())
			{
				int team = (rs.getInt("team") - 1);
				int obj_id = rs.getInt("obj_id");
				_teams.get(team).add(obj_id);
			}
		}
		catch(Exception e)
		{
			_log.warning("EventManager: Open Season, could not restore players." + e);
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
		System.out.println("EventManager: Open Season, battle period.");
		System.out.println("EventManager: Open Season, restored " + (_teams.get(0).size() + _teams.get(1).size()) + " players.");
	}
	
	private void startEvent()
	{
		_state = EventState.REG;
		
		announce("Стартовал евент -Сезон охоты-!");
		announce("Регистрация в Хантер Вилладж на " + (Config.OS_REGTIME + 1) + " минут.");
		System.out.println("EventManager: Open Season, registration opened.");
		_osTask = ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceTask(), ANNOUNCE_DELAY);
	}
	
	public class AnnounceTask implements Runnable
	{
		public void run()
		{
			if(_state != EventState.REG)
				return;
			
			long regMin = REG_TIME;
			for (int i = 0; i < REG_TIME; i += ANNOUNCE_DELAY)
			{
				try
				{
					regMin -= ANNOUNCE_DELAY;
					announce("Регистрация на евент -Сезон охоты-!");
					announce("В Хантер Вилладж, осталось " + ((regMin/60000) + 1) + " минут.");
					if (_teams.get(0).isEmpty() || _teams.get(1).isEmpty())
						announce("(Еще никто не зарегистрировался на эвент.)");
					else
						announce("(Участники: Охотники " + _teams.get(0).size() +", Лесная братва " + _teams.get(1).size() +")");
					Thread.sleep(ANNOUNCE_DELAY);
				}
				catch (InterruptedException e)
				{
				}
			}
			announce("Закончена регистрация на евент -Сезон охоты-!");
			
			if (_teams.get(0).size() < Config.OS_MINPLAYERS || _teams.get(1).size() < Config.OS_MINPLAYERS)
			{
				announce("Эвент -Сезон охоты- отменен, не хватает участников.");
				announce("Следующий эвент через " + Config.OS_FAIL_NEXT + " минут!");
				System.out.println("EventManager: Open Season, canceled: no players.");
				System.out.println("EventManager: Open Season, next start after " + Config.OS_FAIL_NEXT + " min.");
				_osTask = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), FAIL_TIME);
				return;
			}
			_osTask = ThreadPoolManager.getInstance().scheduleGeneral(new BattleTask(), 10000);
		}
	}
	
	public class BattleTask implements Runnable
	{
		public void run()
		{
			_state = EventState.BATTLE;
			announce("-Сезон охоты- открыт!");
			announce("После " + Config.OS_BATTLE + " дней будет определена победившая команда по количеству фрагов.");
			System.out.println("EventManager: Open Season, battle started after.");
			storePlayers();
			_event.SetDBValue("OpenSeason", "nextFinish", "" + (System.currentTimeMillis() + BATTLE_PERIOD));
			_osTask = ThreadPoolManager.getInstance().scheduleGeneral(new FinishTask(), BATTLE_PERIOD);
		}
	}
	
	public class FinishTask implements Runnable
	{
		public void run()
		{
			announce("-Сезон охоты- завершен!");
			int winTeam = 0;
			if (_points.get(0) < _points.get(1))
				winTeam = 1;
			
			announce("-Сезон охоты-, Охотники: " + _points.get(0) + " фрагов.");
			announce("-Сезон охоты-, Лесная братва: " + _points.get(1) + " фрагов.");
			announce("-Сезон охоты-, победила команда: " + _teamNames.get(winTeam));
			System.out.println("EventManager: Open Season, battle finished.");
			walidateWinner(winTeam);
			//clean();
			manageNextTime();
		}
	}
	
	private void walidateWinner(int winTeam)
	{
		String theme = "!Победа!";
		TextBuilder text = new TextBuilder();
		text.append("Поздравляем, наша команда победила в евенте -Сезон охоты-!<br1>");
		
		Date date = new Date();
		SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat timef = new SimpleDateFormat("HH:mm:ss");
		String dates = datef.format(date).toString();
		String times = timef.format(date).toString();
			
	 	Connect con = null;
		PreparedStatement st = null;
		ResultSet result = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			con.setAutoCommit(false);	
				
			L2PcInstance player = null; 
			FastList<Integer> players = _teams.get(winTeam);			
			for (FastList.Node<Integer> n = players.head(), end = players.tail(); (n = n.getNext()) != end;)
			{
				Integer playerId = n.getValue();
				if (playerId == null)
					continue;
					
				String name = _event.getNameById(playerId);
				if (name.equals(""))
					return;	
					
				player = L2World.getInstance().getPlayer(playerId);

				for (FastList.Node<EventReward> k = _rewards.head(), endk = _rewards.tail(); (k = k.getNext()) != endk;)
				{
					EventReward reward = k.getValue();
					if (reward == null)
						continue;
						
					if (Rnd.get(100) < reward.chance)
					{
						L2Item rewardItem = ItemTable.getInstance().getTemplate(reward.id);
						if (rewardItem == null)
							continue;
						
						st = con.prepareStatement("INSERT INTO `z_post_pos` (`id`,`tema`,`text`,`from`,`to`,`type`,`date`,`time`,`itemName`,`itemId`,`itemCount`,`itemEnch`,`augData`,`augSkill`,`augLvl`) VALUES (NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
						st.setString(1, theme);
						st.setString(2, text.toString());
						st.setString(3, "~" + _teamNames.get(winTeam) + "~");
						st.setString(4, name);
						st.setInt(5, 0);
						st.setString(6, dates);
						st.setString(7, times);
						st.setString(8, rewardItem.getName());
						st.setInt(9, reward.id);
						st.setInt(10, reward.count);
						st.setInt(11, 0);
						st.setInt(12, 0);
						st.setInt(13, 0);
						st.setInt(14, 0);
						st.addBatch();
						
						if (player != null)
							player.sendHtmlMessage("-Сезон охоты-", "Поздравлем с победой, друг! <br> Ваш приз " + rewardItem.getName() + "(" + reward.count + ") отправлен на почту, через некоторое время его доставят. <br1><font color=FF9966>(Массовая рассылка - дело не быстрое)</font><br><br><br>_______<br1>~" + _teamNames.get(winTeam) + "~");
					}
				}
				
				try
				{
					Thread.sleep(50); 
				}
				catch (InterruptedException ex) {}
			}
			st.executeBatch();
			con.commit();
		}
		catch(final Exception e)
		{
			_log.warning("FightClub: sendLetter() error: " + e);
		}
		finally
		{
            Close.CSR(con, st, result);
			text.clear();
		}
	}
	
	private void clean()
	{
		Connect con = null;
		PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("UPDATE z_openseason_points SET points=?");
			st.setInt(1, 0);
			st.executeUpdate();
			Close.S(st);
			
			st = con.prepareStatement("DELETE FROM z_openseason_players WHERE team > ?");
			st.setInt(1, 0);
			st.executeUpdate();
        } 
		catch (Exception e) 
		{
			_log.warning("EventManager: Open Season, could not clean." + e);
        } 
		finally
		{
			Close.CS(con, st);
		}	
	}
	
	private void storePlayers()
	{
		Connect con = null;
		PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
			con.setAutoCommit(false);
			L2PcInstance player = null;
			for (FastMap.Entry<Integer, FastList<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) 
			{
				Integer teamId = (e.getKey() + 1);
				FastList<Integer> players = e.getValue();
				
				for (FastList.Node<Integer> n = players.head(), endp = players.tail(); (n = n.getNext()) != endp;)
				{
					Integer playerId = n.getValue();
					if (playerId == null)
						continue;
					
					st = con.prepareStatement("REPLACE INTO z_openseason_players (team, obj_id) VALUES (?,?)");
					st.setInt(1, teamId);
					st.setInt(2, playerId);
					st.addBatch();
					
					player = L2World.getInstance().getPlayer(playerId);
					if (player != null)
						player.setOsTeam(teamId);
				}
			}
			st.executeBatch();
			con.commit();
        } 
		catch (Exception e) 
		{
			_log.warning("EventManager: Open Season, could not store teams." + e);
        } 
		finally
		{
			Close.CS(con, st);
		}	
	}	
	
	public void regPlayer(L2PcInstance player)
	{
		if(_state != EventState.REG)
		{
			player.sendHtmlMessage("-Сезон охоты-", "Регистрация еще не обьявлялась<br1> Приходите позже ;).");
			return;
		}
		
		if (player.getLevel() < Config.OS_MINLVL)
		{
			player.sendHtmlMessage("-Сезон охоты-", "Минимальный уровень для участия - " + Config.OS_MINLVL + ".");
			return;
		}
		
		if (_teams.get(0).contains(player) || _teams.get(1).contains(player))
		{
			player.sendHtmlMessage("-Сезон охоты-", "Вы уже зарегистрированы.");
			return;
		}
		
		int team = 0;
		if (_teams.get(0).size() == _teams.get(1).size())
			team = Rnd.get(0,1);
		else if (_teams.get(0).size() > _teams.get(1).size())
			team = 1;

		_teams.get(team).add(player.getObjectId());
		player.sendHtmlMessage("-Сезон охоты-", "Регистрация завершена, ваша команда: <br> " + _teamNames.get(team) + ".");
	}
	
	public void increasePoints(int teamId)
	{		
		if(_state != EventState.BATTLE)
			return;

		Lock run = new ReentrantLock(); 
		run.lock();
		try 
		{
			int points = _points.get(teamId);
			_points.put(teamId, (points + 1));
			if (points % 50 == 0)
			{
				storePoints();
				announce("-Сезон охоты-, Охотники: " + _points.get(0) + " фрагов.");
				announce("-Сезон охоты-, Лесная братва: " + _points.get(1) + " фрагов.");
			}
		}
		finally 
		{
			run.unlock();
		}
		//points.put(teamId, _points.get(teamId) + 1);
	}
	
	private void storePoints()
	{
		Connect con = null;
		PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
			con.setAutoCommit(false);
			for (FastMap.Entry<Integer, Integer> e = _points.head(), end = _points.tail(); (e = e.getNext()) != end;) 
			{
				Integer teamId = (e.getKey() + 1);
				Integer points = e.getValue();
				
				st = con.prepareStatement("UPDATE z_openseason_points SET points=? WHERE team=?");
				st.setInt(1, points);
				st.setInt(2, teamId);
				st.addBatch();
			}
			st.executeBatch();
			con.commit();
        } 
		catch (Exception e) 
		{
			_log.warning("EventManager: Open Season, could not store teams." + e);
        } 
		finally
		{
			Close.CS(con, st);
		}	
	}	

	private void manageNextTime()
	{
		_state = EventState.WAIT;
		_osTask = ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), NEXTTIME);
	}
	
	private void announce(String text)
	{	
		_event.announce(text);
	}
	
	public boolean isInBattle()
	{
		return (_state == EventState.BATTLE);
	}
	
	public int getTeam(int playerId)
	{
		if (_teams.get(0).contains(playerId))
			return 1;
		else if (_teams.get(1).contains(playerId))
			return 2;
		
		return 0;
	}
}
