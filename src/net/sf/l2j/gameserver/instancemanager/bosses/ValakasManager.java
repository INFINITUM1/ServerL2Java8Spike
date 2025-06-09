package net.sf.l2j.gameserver.instancemanager.bosses;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.TimeLogger;
import scripts.ai.Valakas;

public class ValakasManager extends GrandBossManager
{
	private static final Logger _log = AbstractLogger.getLogger(ValakasManager.class.getName());

	private static final int BOSS = 29028;
	private Valakas self = null;
	private boolean _enter = false;

    private static ValakasManager _instance;
    public static final ValakasManager getInstance()
    {
        return _instance;
    }

	static class Status
	{
		public int status;
		public long respawn;
		public boolean wait = true;
		
		public Status(int status, long respawn)
		{
			this.status = status;
			this.respawn = respawn;
		}
	}
	private static Status _status;

	class ManageBoss implements Runnable
	{
		ManageBoss()
		{
		}

		public void run()
		{	
			long delay = 0;
			if (_status.respawn > 0)
				delay = _status.respawn - System.currentTimeMillis();

			if(delay <= 0)
				prepareBoss();
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new PrepareBoss(), delay);
			_status.wait = false;
		}
	}

	class PrepareBoss implements Runnable
	{
		PrepareBoss()
		{
		}

		public void run()
		{	
			prepareBoss();
		}
	}

	class CloseGate implements Runnable
	{
		CloseGate()
		{
		}

		public void run()
		{	
			_enter = false;
			setState(2, 0);
		}
	}

	class SpawnBoss implements Runnable
	{
		SpawnBoss()
		{
		}

		public void run()
		{	
			spawnBoss();
		}
	}

	class Sleep implements Runnable
	{
		Sleep()
		{
		}

		public void run()
		{			
			if (self == null || self.isDead())
				return;
			
			if(System.currentTimeMillis() - self.getLastHit() > Config.VALAKAS_UPDATE_LAIR)
			{
				self.deleteMe();
				self = null;
				_enter = false;
				setState(1, 0);
				GrandBossManager.getInstance().getZone(213004, -114890, -1635).oustAllPlayers();
				return;
			}
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new Sleep(), Config.VALAKAS_UPDATE_LAIR);
					
			if(self.getCurrentHp() > ((self.getMaxHp() * 1.000000) / 4.000000))
				if(self.getFirstEffect(4241) == null) 
					self.addUseSkillDesire(4241,1);
			else if(self.getCurrentHp() > ((self.getMaxHp() * 2.000000) / 4.000000))
				if(self.getFirstEffect(4240) == null) 
					self.addUseSkillDesire(4240,1);
			else if(self.getCurrentHp() > ((self.getMaxHp() * 3.000000) / 4.000000))
				if(self.getFirstEffect(4239) == null) 
					self.addUseSkillDesire(4239,1);
			else if(self.getFirstEffect(4125) == null) 
				self.addUseSkillDesire(4125,1);
		}
	}

	public static void init()
	{
        _instance = new ValakasManager();
        _instance.load();
	}

	public void load()
	{
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT spawn_date, status FROM grandboss_data WHERE boss_id=?");
			st.setInt(1, BOSS);
			rs = st.executeQuery();
			if (rs.next())
			{
				int status = rs.getInt("status");
				long respawn = rs.getLong("spawn_date");
				
				if (status > 1)
					status = 1;
				
				if (respawn > 0)
					status = 0;
				
				_status = new Status(status, respawn);
			}
		}
		catch (SQLException e)
		{
			_log.warning("ValakasManager, failed to load: " + e);
			e.getMessage();
		}
		finally
		{
			Close.CSR(con, st, rs);
		}

		switch(_status.status)
		{
			case 0:
				String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(_status.respawn));
				_log.info("Valakas: dead; spawn date: " + date);
				break;
			case 1:
				_log.info("Valakas: live; farm delay: " + (Config.VALAKAS_RESTART_DELAY / 60000) + "min.");
				break;
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new ManageBoss(), Config.VALAKAS_RESTART_DELAY);
	}

	public void prepareBoss()
	{
		setState(1, 0);
	}

	public void spawnBoss()
	{
		self = (Valakas) createOnePrivateEx(BOSS, 213004, -114890, -1635, 30000);
		self.setRunning();

		ThreadPoolManager.getInstance().scheduleGeneral(new Sleep(), Config.VALAKAS_UPDATE_LAIR);
	}

	public void setState(int status, long respawn)
	{
		_status.status = status;
		_status.respawn = respawn;
		
		Connect con = null;
		PreparedStatement statement = null;
        try
        {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE `grandboss_data` SET `status`=?, `spawn_date`=? WHERE `boss_id`=?");
            statement.setInt(1, status);
            statement.setLong(2, respawn);
            statement.setInt(3, BOSS);
            statement.executeUpdate();
        } 
		catch (SQLException e) 
		{
			_log.warning("ValakasManager, could not set Valakas status" + e);
			e.getMessage();
        } 
		finally
		{
			Close.CS(con, statement);
		}

		switch(status)
		{
			case 0:
				ThreadPoolManager.getInstance().scheduleGeneral(new PrepareBoss(), (respawn - System.currentTimeMillis()));
				String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(respawn));
				_log.info("ValakasManager, Valakas status: 0; killed, respawn date: " + date);
				break;
			case 1:
				_log.info("ValakasManager, Valakas status: 1; ready for farm.");
				if (Config.ANNOUNCE_EPIC_STATES)
					EventManager.getInstance().announce(Static.VALAKAS_SPAWNED);
				break;
			case 2:
				_log.info("ValakasManager, Valakas status: 2; under attack.");
				break;
			case 5:
				_log.info("ValakasManager, Valakas status: 5; gate opened.");
				break;
		}
	}

	public int getStatus()
	{
		if (_status.wait)
			return 0;

		return _status.status;
	}

	public void notifyEnter()
	{
		if (_enter)
			return;

		_enter = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new CloseGate(), Config.VALAKAS_CLOSE_PORT);
		ThreadPoolManager.getInstance().scheduleGeneral(new SpawnBoss(), Config.VALAKAS_SPAWN_DELAY);
	}

	public void notifyDie()
	{
		if (self == null)
			return;
		
		self = null;
		SpawnTable.getInstance().getTerritory(5025).spawn(1000);
		
		long offset = (Config.VALAKAS_MIN_RESPAWN + Config.VALAKAS_MAX_RESPAWN) / 2;
		setState(0, (System.currentTimeMillis() + offset));
		
		if (Config.ANNOUNCE_EPIC_STATES)
			EventManager.getInstance().announce(Static.VALAKAS_DIED);
	}
}