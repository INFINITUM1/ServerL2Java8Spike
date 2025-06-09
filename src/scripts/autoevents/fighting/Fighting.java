package scripts.autoevents.fighting;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
//import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.masspvp.massPvp;
import scripts.autoevents.lasthero.LastHero;

public final class Fighting 
{
	protected static final Logger _log = Logger.getLogger(Fighting.class.getName());

	private static Calendar _point = null;
	
	private static final long _regTime = (Config.ELH_REGTIME * 60000);
	private static final long _anTime = (Config.ELH_ANNDELAY * 60000);
	private static final long _tpDelay = (Config.ELH_TPDELAY * 60000);

	private static final int _minLvl = Config.FIGHTING_MINLVL;
	private static final int _minPlayers = Config.FIGHTING_MINP;
	private static final int _maxPlayers = Config.FIGHTING_MAXP;
	
	private static final Location _npcLoc = Config.FIGHTING_NPCLOC;
	private static final int _npcId = Config.FIGHTING_NPCID;
	
	private static final Location _tpLoc = Config.FIGHTING_TPLOC;
	
	private static final int _ticketId = Config.ELH_TICKETID; 
	private static final int _ticketCount = Config.ELH_TICKETCOUNT;

	private static FastList<L2PcInstance> _players = new FastList<L2PcInstance>();
	private static ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<String>();
	
	private static L2NpcInstance _regNpc = null;

	static enum EventState 
	{
		WAIT,
		REG,
		BATTLE
	}
	private static EventState _state = EventState.WAIT;

	private static Fighting _event;
	public static void init()
	{
		_event = new Fighting();
		_event.load();
	}
	
	public static Fighting getEvent()
	{
		return _event;
	}

	public void load() 
	{
		checkTimer();
	}

	public void checkTimer()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), getNextStart());
		String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(_point.getTime());
		System.out.println("EventManager: Fighting, start: " + date + ".");
	}

	private static long getNextStart()
	{
		Calendar tomorrow = new GregorianCalendar();
		//tomorrow.set(Calendar.DAY_OF_WEEK, Config.ANARCHY_DAY);                
		
		int daysToChange = getDaysToPeriodChange(tomorrow);
        if (daysToChange == 7)
            if (tomorrow.get(Calendar.HOUR_OF_DAY) < Config.FIGHTING_HOUR)
                daysToChange = 0;
            else if (tomorrow.get(Calendar.HOUR_OF_DAY) == Config.FIGHTING_HOUR && tomorrow.get(Calendar.MINUTE) < 0)
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
			Config.FIGHTING_HOUR,
			0
		);
		_point = result;
		return (result.getTimeInMillis() - System.currentTimeMillis());
	}	

	private static int getDaysToPeriodChange(Calendar tomorrow)
    {
		int numDays = tomorrow.get(Calendar.DAY_OF_WEEK) - Config.FIGHTING_DAY;

		if (numDays < 0)
			return 0 - numDays;

        return 7 - numDays;
	}
	

	public class StartTask implements Runnable
	{
		public void run()
		{
			if(_state == EventState.WAIT)
				startEvent();
		}
	}

	private void startEvent()
	{
		_state = EventState.REG;
		
		announce(Static.FIGHTING_STARTED);
		announce(Static.FIGHTING_REG_FOR_S1.replace("%a%", String.valueOf(((_regTime/60000) + 1))));
		System.out.println("EventManager: Fighting, registration opened.");
		
		_regNpc = EventManager.getInstance().doSpawn(_npcId, _npcLoc, 0);
		
		_ips.clear();
		_players.clear();
		ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceTask(), _anTime);
	}

	public class AnnounceTask implements Runnable
	{
		public void run()
		{
			if(_state != EventState.REG)
				return;
			
			long regMin = _regTime;
			for (int i = 0; i < _regTime; i += _anTime)
			{
				try
				{
					regMin -= _anTime;
					announce(Static.FIGHTING_REG_REG);
					announce(Static.FIGHTING_REG_LAST_FOR_S1.replace("%a%", String.valueOf(((regMin/60000) + 1))));
					if (_players.isEmpty())
						announce(Static.FIGHTING_NO_PLAYESR_YET);
					else
						announce(Static.FIGHTING_REGGED_PLAYERS.replace("%a%", String.valueOf(_players.size())));
					Thread.sleep(_anTime);
				}
				catch (InterruptedException e)
				{
				}
			}
			
			_regNpc.deleteMe();
			_regNpc = null;
			announce(Static.FIGHTING_REG_CLOSED);
			
			_state = EventState.BATTLE;
			
			if (_players.size() < _minPlayers)
			{
				_state = EventState.WAIT;
				announce(Static.FIGHTING_CANCELED_NO_PLAYERS);
				System.out.println("EventManager: Fighting, canceled: no players.");
				ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), getNextStart());
				String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(_point.getTime());
				System.out.println("EventManager: Fighting, start: " + date + ".");
				return;
			}
			teleportPlayers();
		}
	}
	
	private void teleportPlayers()
	{
		L2PcInstance player = null;
		for (FastList.Node<L2PcInstance> n = _players.head(), endp = _players.tail(); (n = n.getNext()) != endp;)
		{
			player = n.getValue();
			if (player == null)
				continue;
				
			if (player.isDead())
			{
				notifyDeath(player);
				continue;
			}

			if (Config.FORBIDDEN_EVENT_ITMES)
			{
				// снятие переточеных вещей
				for (L2ItemInstance item: player.getInventory().getItems())
				{
					if (item == null)
						continue;

					if (item.notForOly())
						player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
				}
			}
			
			player.setChannel(5);
			player.teleToLocationEvent(_tpLoc.x+Rnd.get(300), _tpLoc.y+Rnd.get(300), _tpLoc.z);
			player.stopAllEffects();
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setEventWait(true);
		}
		announce(Static.FIGHTING_BATTLE_STRT_AFTER.replace("%a%", String.valueOf((_tpDelay/60000))));
		System.out.println("EventManager: Fighting, battle start after " + (_tpDelay/60000) + " min.");
		ThreadPoolManager.getInstance().scheduleGeneral(new StartBattle(), _tpDelay);
	}

	public class StartBattle implements Runnable
	{
		public StartBattle()
		{
		}
		
		public void run()
		{
			L2PcInstance player = null;
			for (FastList.Node<L2PcInstance> n = _players.head(), endp = _players.tail(); (n = n.getNext()) != endp;)
			{
				player = n.getValue();
				if (player == null)
					continue;
				
				if (player.isDead())
					notifyDeath(player);
			}
			
			announce(Static.FIGHTING_BATTLE_STRTED);
			for (FastList.Node<L2PcInstance> n = _players.head(), endp = _players.tail(); (n = n.getNext()) != endp;)
			{
				player = n.getValue();
				if (player == null)
					continue;
				
				player.setEventWait(false);
				player.setPVPArena(true);
				player.setChannel(5);
				player.setTeam(1);
			}
			player = null;
			ThreadPoolManager.getInstance().scheduleGeneral(new WinTask(), 10000);
			System.out.println("EventManager: Fighting, battle started.");
		}
	}

	public class WinTask implements Runnable
	{
		public WinTask()
		{
		}
		
		public void run()
		{
			if (_players.size() == 1)
				announceWinner(_players.getFirst());
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new WinTask(), 10000);
		}
	}

	public void announceWinner(L2PcInstance player)
	{
		_state = EventState.WAIT;
		announce(Static.FIGHTING_FINISHED);
		announce(Static.FIGHTING_WINNER_S1.replace("%a%", player.getName()));
		announce(Static.FIGHTING_NEXT_EVENT_S1);
		System.out.println("EventManager: Fighting, finished; palyer " + player.getName() + " win.");
		
		ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), getNextStart());
		String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(_point.getTime());
		System.out.println("EventManager: Fighting, start: " + date + ".");

		player.setPVPArena(false);
		player.teleToLocationEvent(82737, 148571, -3470);
		
		int skillId = 0;
		switch(player.getClassId().getId())
		{
			case 88:
			case 89:
			case 92:
			case 93:
			case 101:
			case 102:
			case 113:
			case 114:
			case 108:
			case 109:
				skillId = 7077; // файтер
				break;
			case 94:
			case 95:
			case 103:
			case 110:
				skillId = 7078; // маг
				break;
			case 96:
			case 97:
			case 98:
			case 104:
			case 105:
			case 100:
			case 107:
			case 111:
			case 112:
			case 115:
			case 116:
				skillId = 7079; // суппорт
				break;
			case 90:
			case 91:
			case 99:
			case 106:
			case 117:
			case 118:
				skillId = 7080; // танк
				break;
		}
		if (skillId == 0)
			return;
		
		player.setChannel(1);
        player.addDonateSkill(player.getClassId().getId(), skillId, 2, (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)));
	}

	private boolean foundIp(String ip)
	{
		return (_ips.contains(ip));
	}

	public void regPlayer(L2PcInstance player)
	{
		if(_state != EventState.REG)
		{
			player.sendHtmlMessage("-Замес-", "Регистрация еще не обьявлялась<br1> Приходите позже ;).");
			return;
		}
		
		if(!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(player.getName()))
		{
			player.sendHtmlMessage("-Замес-", "Вы уже зарегистрированы на TvT.");
			return;
		}
		
		if (Config.MASS_PVP && massPvp.getEvent().isReg(player))
		{
            player.sendHtmlMessage("-Замес-", "Удачи на евенте -Масс ПВП-");
            return; 
		}
		if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player))
		{
            player.sendHtmlMessage("-Замес-", "Удачи на евенте -Захват базы-");
            return; 
		}
		if(Config.ELH_ENABLE && LastHero.getEvent().isRegged(player))
		{
			player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы в -Последний герой- эвенте.");
			return;
		}
		
		if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode())
		{
			player.sendHtmlMessage("-Замес-", "Вы уже зарегистрированы на олимпиаде.");
			return;
		}
		
		if (player.isCursedWeaponEquiped()) 
		{
			player.sendHtmlMessage("-Замес-", "С проклятым оружием нельзя.");
			return;
		}
		
		if (_players.size() >= _maxPlayers)
		{
			player.sendHtmlMessage("-Замес-", "Достигнут предел игроков.");
			return;
		}
		
		if (_players.contains(player))
		{
			player.sendHtmlMessage("-Замес-", "Вы уже зарегистрированы.");
			return;
		}
		
		if (!Config.EVENTS_SAME_IP && foundIp(player.getIP()))
		{
			player.sendHtmlMessage("-Замес-", "С вашего IP уже есть игрок.");
			return;
		}
		
		if (_ticketId > 0)
		{		
			L2ItemInstance coin = player.getInventory().getItemByItemId(_ticketId);
			if (coin == null || coin.getCount() < _ticketCount)
			{
				player.sendHtmlMessage("-Замес-", "Участив в ивенте платное.");
				return;
			}
			player.destroyItemByItemId("Fighting", _ticketId, _ticketCount, player, true);
		}
		
		_players.add(player);
		if (!Config.EVENTS_SAME_IP)
			_ips.add(player.getIP());
		player.sendHtmlMessage("-Замес-", "Регистрация завершена.");
	}
	
	public void delPlayer(L2PcInstance player)
	{
		if (!(_players.contains(player)))
		{
			player.sendHtmlMessage("-Замес-", "Вы не зарегистрированы.");
			return;
		}
		
		_players.remove(_players.indexOf(player));
		if (!Config.EVENTS_SAME_IP)
			_ips.remove(player.getIP());
		player.sendHtmlMessage("-Замес-", "Регистрация отменена.");
	}
	
	public void notifyFail(L2PcInstance player)
	{
		if (_state == EventState.WAIT)
			return;
		
		if (_players.contains(player))
		{
			_players.remove(_players.indexOf(player));
			if (!Config.EVENTS_SAME_IP)
				_ips.remove(player.getIP());
			player.setXYZ(82737, 148571, -3470);
			player.setPVPArena(false);
			player.setChannel(1);
		}
	}
	
	public void notifyDeath(L2PcInstance player)
	{
		if (_state == EventState.WAIT)
			return;
		
		if (_players.contains(player))
		{
			_players.remove(_players.indexOf(player));
			if (!Config.EVENTS_SAME_IP)
				_ips.remove(player.getIP());
			
			player.sendCritMessage("Вы проиграли...");
			try
			{
				player.teleToLocationEvent(82737, 148571, -3470);
			}
			catch(Exception e){}
			player.doRevive();
			player.setChannel(1);
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			player.setPVPArena(false);
			player.setTeam(0);
		}
	}
	
	public boolean isRegged(L2PcInstance player)
	{
		if (_state == EventState.WAIT)
			return false;
		
		if (_players.contains(player))
			return true;
		
		return false;
	}
	
	private void announce(String text)
	{	
		Announcements.getInstance().announceToAll(text);
	}
	
	public boolean isInBattle()
	{
		return (_state == EventState.BATTLE);
	}
}
