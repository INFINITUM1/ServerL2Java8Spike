package scripts.autoevents.encounter;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.lasthero.LastHero;
import scripts.autoevents.masspvp.massPvp;

public final class Encounter 
{
	protected static final Logger _log = Logger.getLogger(Encounter.class.getName());
	
	private static final long _arRestart = (Config.EENC_ARTIME * 60000);
	private static final long _regTime = (Config.EENC_REGTIME * 60000);
	private static final long _anTime = (Config.EENC_ANNDELAY * 60000);
	private static final long _tpDelay = (Config.EENC_TPDELAY * 60000);
	private static final long _eventFinish = (Config.EENC_FINISH * 60000);
	private static final long _nextTime = (Config.EENC_NEXT * 60000);

	private static final int _minLvl = Config.EENC_MINLVL;
	private static final int _minPlayers = Config.EENC_MINP;
	private static final int _maxPlayers = Config.EENC_MAXP;

	private static Location _tpLoc = Config.EENC_TPLOC;
	private static final int _ticketId = Config.EENC_TICKETID; 
	private static final int _ticketCount = Config.EENC_TICKETCOUNT;

	private static FastList<L2PcInstance> _players = new FastList<L2PcInstance>();
	private static FastList<EventReward> _rewards = Config.EENC_REWARDS;
	private static FastMap<Integer, FastList<Location>> _itemPoints = Config.EENC_POINTS;
	private static ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<String>();
	private static ConcurrentLinkedQueue<L2ItemInstance> _spawnedItems = new ConcurrentLinkedQueue<L2ItemInstance>();
	private static FastMap<L2PcInstance, Integer> _itemFounds = new FastMap<L2PcInstance, Integer>();

	static enum EventState 
	{
		WAIT,
		REG,
		SEARCH
	}
	private static EventState _state = EventState.WAIT;
	
	private static Encounter _event;
	public static void init()
	{
		_event = new Encounter();
		_event.load();
	}
	
	public static Encounter getEvent()
	{
		return _event;
	}

	public void load() 
	{
		checkTimer();
	}
	
	public void checkTimer()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _arRestart);
		System.out.println("EventManager: Encounter, start after " + (_arRestart/60000) + " min.");
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
		
		announce(Static.EENC_STARTED);
		announce(Static.EENC_REG_FOR_S1.replace("%a%", String.valueOf(((_regTime/60000) + 1))));
		System.out.println("EventManager: Encounter, registration opened.");
		
		_ips.clear();
		_players.clear();
		_itemFounds.clear();
		_spawnedItems.clear();
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
					announce(Static.EENC_REG_REG);
					announce(Static.EENC_REG_LOST_S1.replace("%a%", String.valueOf(((regMin/60000) + 1))));
					if (_players.isEmpty())
						announce(Static.EENC_NO_PLAYESR_YET);
					else
						announce(Static.EENC_PLAYER_TEAMS.replace("%a%", String.valueOf(_players.size())));
					Thread.sleep(_anTime);
				}
				catch (InterruptedException e)
				{
				}
			}
			announce(Static.EENC_REG_CLOSED);
			_state = EventState.SEARCH;
			
			if (_players.size() < _minPlayers)
			{
				_state = EventState.WAIT;
				announce(Static.EENC_NO_PLAYERS);
				announce(Static.EENC_NEXT_AFTER.replace("%a%", String.valueOf((_nextTime/60000))));
				System.out.println("EventManager: Encounter, canceled: no players.");
				System.out.println("EventManager: Encounter, next start after " + (_nextTime/60000) + " min.");
				ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
				return;
			}
			announce(Static.EENC_SEARCH_AFTER.replace("%a%", String.valueOf((_tpDelay/60000))));
			announce(Static.EENC_GO_STRIDER);
			System.out.println("EventManager: Encounter, search start after " + (_tpDelay/60000) + " min.");
			ThreadPoolManager.getInstance().scheduleGeneral(new StartBattle(), _tpDelay);
		}
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
				
				if (!player.isMounted())
				{
					player.sendHtmlMessage("-Дозор-", "Ошибка!<br>Вы были удалены с ивента, т.к. не сидели на страйдере!");
					notifyDeath(player);
				}
			}
			
			spawnItems();
			
			for (FastList.Node<L2PcInstance> n = _players.head(), endp = _players.tail(); (n = n.getNext()) != endp;)
			{
				player = n.getValue();
				if (player == null)
					continue;
				
				player.setInEncounterEvent(true);
				
				if(player.getParty() != null)
					player.getParty().oustPartyMember(player);
			
				player.teleToLocationEvent(_tpLoc.x+Rnd.get(200), _tpLoc.y+Rnd.get(200), _tpLoc.z);
				player.stopAllEffects();
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setTeam(1);
			}
			player = null;
			ThreadPoolManager.getInstance().scheduleGeneral(new FinishTask(), _eventFinish);
			System.out.println("EventManager: Encounter, search started.");
		}
	}
	
	private void spawnItems()
	{
		for (FastMap.Entry<Integer, FastList<Location>> e = _itemPoints.head(), end = _itemPoints.tail(); (e = e.getNext()) != end;) 
		{
			Integer key = e.getKey();
			FastList<Location> value = e.getValue();
			if (key == null || value == null)
				continue;
			
			Location loc = value.get(Rnd.get((value.size() - 1)));
			if (loc == null)
				continue;
			
			L2ItemInstance ditem = ItemTable.getInstance().createItem("Encounter", key, 1, null, null);
			ditem.dropMe(null, loc.x, loc.y, loc.z);
			
			_spawnedItems.add(ditem);
		}
	}
	
	public class FinishTask implements Runnable
	{
		public FinishTask()
		{
		}
		
		public void run()
		{
			if (!isInBattle())
				return;
			
			announce(Static.EENC_FINISHED);
			
			L2PcInstance player = null;
			for (FastList.Node<L2PcInstance> n = _players.head(), endp = _players.tail(); (n = n.getNext()) != endp;)
			{
				player = n.getValue();
				if (player == null)
					continue;
				
				player.setInEncounterEvent(false);

				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setTeam(0);
			}

			for (L2ItemInstance ditem : _spawnedItems)
			{
				if (ditem == null)
					continue;
				player = null;

				if (ditem.getOwnerId() != 0)
					player = L2World.getInstance().getPlayer(ditem.getOwnerId());

				ditem.deleteMe();
				if (player != null)
				{
					if (_itemFounds.containsKey(player))
					{
						int current = _itemFounds.get(player);
						current += 1;
						_itemFounds.put(player, current);
					}
					else
						_itemFounds.put(player, 1);
						
					player.sendItems(false);
				}
			}
			
			int max = 0;
			player = null;
			L2PcInstance winner = null;
			for (FastMap.Entry<L2PcInstance, Integer> e = _itemFounds.head(), end = _itemFounds.tail(); (e = e.getNext()) != end;) 
			{
				player = e.getKey();
				Integer value = e.getValue();
				if (player == null || value == null)
					continue;
				
				if (value > max)
				{
					max = value;
					winner = player;
				}
			}
			
			if (winner != null)
				announceWinner(winner, max);
			
			announce(Static.EENC_NEXT_AFTER.replace("%a%", String.valueOf((_nextTime/60000))));
			System.out.println("EventManager: Encounter, finished; palyer " + player.getName() + " win.");
			System.out.println("EventManager: Encounter, next start after " + (_nextTime/60000) + " min.");
			ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
			
			_ips.clear();
			_players.clear();
			_itemFounds.clear();
			_spawnedItems.clear();
		}
	}
	
	public void announceWinner(L2PcInstance player, int max)
	{
		if (!isInBattle())
			return;
			
		_state = EventState.WAIT;
		
		String anns = Static.EENC_WINNER.replace("%a%", player.getName());
		anns = anns.replace("%b%", String.valueOf(max));
		announce(anns);
		
		for (FastList.Node<EventReward> k = _rewards.head(), endk = _rewards.tail(); (k = k.getNext()) != endk;)
		{
			EventReward reward = k.getValue();
			if (reward == null)
				continue;
			
			if (Rnd.get(100) < reward.chance)
				player.addItem("Encounter", reward.id, reward.count, player, true);
		}
	}
	
	private boolean foundIp(String ip)
	{
		return (_ips.contains(ip));
	}
	
	public void regPlayer(L2PcInstance player)
	{
		if(_state != EventState.REG)
		{
			player.sendHtmlMessage("-Дозор-", "Регистрация еще не обьявлялась<br1> Приходите позже ;).");
			return;
		}
		
		if (!player.isMounted())
		{
			player.sendHtmlMessage("-Дозор-", "Ошибка!<br>Вы должны сидеть на страйдере!");
			return;
		}

		if(!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(player.getName()))
		{
			player.sendHtmlMessage("-Дозор-", "Вы уже зарегистрированы на TvT.");
			return;
		}
		
		if (Config.MASS_PVP && massPvp.getEvent().isReg(player))
		{
            player.sendHtmlMessage("-Дозор-", "Удачи на евенте -Масс ПВП-");
            return; 
		}
		if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player))
		{
            player.sendHtmlMessage("-Дозор-", "Удачи на евенте -Захват базы-");
            return; 
		}
		if(Config.ELH_ENABLE && LastHero.getEvent().isRegged(player))
		{
			player.sendHtmlMessage("-Дозор-", "Вы уже зарегистрированы в -Последний герой- эвенте.");
			return;
		}

		if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode())
		{
			player.sendHtmlMessage("-Дозор-", "Вы уже зарегистрированы на олимпиаде.");
			return;
		}

		if (player.isCursedWeaponEquiped()) 
		{
			player.sendHtmlMessage("-Дозор-", "С проклятым оружием нельзя.");
			return;
		}

		if (_players.size() >= _maxPlayers)
		{
			player.sendHtmlMessage("-Дозор-", "Достигнут предел игроков.");
			return;
		}

		if (_players.contains(player))
		{
			player.sendHtmlMessage("-Дозор-", "Вы уже зарегистрированы.");
			return;
		}

		if (!Config.EVENTS_SAME_IP && foundIp(player.getIP()))
		{
			player.sendHtmlMessage("-Дозор-", "С вашего IP уже есть игрок.");
			return;
		}

		if (_ticketId > 0)
		{		
			L2ItemInstance coin = player.getInventory().getItemByItemId(_ticketId);
			if (coin == null || coin.getCount() < _ticketCount)
			{
				player.sendHtmlMessage("-Дозор-", "Участив в ивенте платное.");
				return;
			}
			player.destroyItemByItemId("lasthero", _ticketId, _ticketCount, player, true);
		}

		_players.add(player);
		if (!Config.EVENTS_SAME_IP)
			_ips.add(player.getIP());
		player.sendHtmlMessage("-Дозор-", "Регистрация завершена.");
	}
	
	public void delPlayer(L2PcInstance player)
	{
		if (!(_players.contains(player)))
		{
			player.sendHtmlMessage("-Дозор-", "Вы не зарегистрированы.");
			return;
		}
		
		_players.remove(_players.indexOf(player));
		if (!Config.EVENTS_SAME_IP)
			_ips.remove(player.getIP());
		player.sendHtmlMessage("-Дозор-", "Регистрация отменена.");
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
			player.setInEncounterEvent(false);
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
			player.setInEncounterEvent(false);
			try
			{
				player.teleToLocationEvent(82737, 148571, -3470);
			}
			catch(Exception e){}
			player.doRevive();
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
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
		return (_state == EventState.SEARCH);
	}
}
