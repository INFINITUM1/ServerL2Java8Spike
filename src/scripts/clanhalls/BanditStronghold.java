package scripts.clanhalls;

import java.util.Calendar;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
//import net.sf.l2j.gameserver.model.entity.ClanHallSiege;
//import net.sf.l2j.gameserver.model.entity.ClanHallSiege.ClanInfo;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;

public class BanditStronghold// extends ClanHallSiege
{
    private FastMap<Integer, ClanInfo> _owner = new FastMap<Integer, ClanInfo>();
    private FastMap<Integer, ClanInfo> _clans = new FastMap<Integer, ClanInfo>();
    private FastTable<Location> _locs = new FastTable<Location>();
    private FastTable<Integer> _camps = new FastTable<Integer>();
	
	private static int _ownerClan = 0;
	private static boolean _finalBattle = false;
	private static boolean _inProgress = false;
	
    private static class ClanInfo
    {
        public int _id;
		public int _boss;
		public int _bossObj;
        public FastList<L2PcInstance> _players;
		
        public ClanInfo(int id, int boss, int bossObj, FastList<L2PcInstance> players)
        {
            _id = id;
			_boss = boss;
			_bossObj = bossObj;
            _players = players;
        }
    }
    private static BanditStronghold _ch;
    public static BanditStronghold getCH()
    {
        return _ch;
    }
    public BanditStronghold()
    {
    }
	
	public static void init()
	{
		_ch = new BanditStronghold();
		_ch.load();
	}
	
	private void load()
	{
		_locs.clear();
		_clans.clear();
		_camps.clear();
		_owner.clear();
		
		_ownerClan = 0;
		_finalBattle = false;
		_inProgress = false;
		
		Location loc = new Location(83699,-17468,-1774,19048);
		_locs.add(loc);
		loc = new Location(82053,-17060,-1784,5432);
		_locs.add(loc);
		loc = new Location(82142,-15528,-1799,58792);
		_locs.add(loc);
		loc = new Location(83544,-15266,-1770,44976);
		_locs.add(loc);
		loc = new Location(84609,-16041,-1769,35816);
		_locs.add(loc);
		loc = null;
		
		_camps.add(35423);
		_camps.add(35424);
		_camps.add(35425);
		_camps.add(35426);
		_camps.add(35427);
		if(!ClanHallManager.getInstance().isFree(35))
		{
			ClanHall ch = ClanHallManager.getInstance().getClanHallById(35);
			
			_ownerClan = ch.getOwnerId();
			ClanInfo ci = new ClanInfo(ch.getOwnerId(), 35428, 0, new FastList<L2PcInstance>());
			
			_owner.put(ch.getOwnerId(), ci);
		}
	}
	
	public synchronized void regClan(L2PcInstance player, L2NpcInstance npc)
	{
		int size = _clans.size();
		if (size >= 5)
		{
			npc.showChatWindow(player, "data/html/siege/35437-full.htm");
			return;
		}
			
		FastList<L2PcInstance> pcs = new FastList<L2PcInstance>();
		pcs.add(player);
		
		ClanInfo ci = new ClanInfo(player.getClan().getClanId(), 0, 0, pcs);
		_clans.put(player.getClan().getClanId(), ci);
		//pcs.clear();
		npc.showChatWindow(player, "data/html/siege/35437-reged_"+size+".htm");
	}
	
	public FastTable<String> getClanNames()
	{
		L2Clan clan = null;
		ClanTable ct = ClanTable.getInstance();
		FastTable<String> names = new FastTable<String>();
		for (FastMap.Entry<Integer, ClanInfo> e = _clans.head(), end = _clans.tail(); (e = e.getNext()) != end;) 
		{
			Integer id = e.getKey(); // No typecast necessary.
			if (id == null)
				continue;
			
			clan = ct.getClan(id);
			if (clan == null)
				continue;
				
			names.add(clan.getName());
		}
		return names;
	}
	
	private void spawnClans()
	{
		DoorTable dt = DoorTable.getInstance();
		dt.getDoor(22170003).closeMe();
		dt.getDoor(22170004).closeMe(); 
		
		L2PcInstance player = null;
		if (!_owner.isEmpty())
		{
			if (_finalBattle)
			{
				dt.getDoor(22170003).openMe();
				dt.getDoor(22170004).openMe(); 
			}
				
			ClanInfo ci = _owner.get(_ownerClan);
			FastList<L2PcInstance> opcs = ci._players;
			for (FastList.Node<L2PcInstance> n = opcs.head(), fend = opcs.tail(); (n = n.getNext()) != fend;) 
			{
				player = n.getValue();
				if(player == null)
					continue;
					
				if (_finalBattle)
					player.sendCritMessage("¬се на выход, на защиту клан холла!");
				else
				{
					player.teleToLocation(80339, -15442, -1804, false);
					player.sendCritMessage("ќжидайте финальную битву внутри клан холла!");
				}
			}
		}
		
		int team = 0;
		int bossId = 0;
		Location loc = null;
		ClanInfo clan = null;
		L2MonsterInstance boss = null;
		ClanTable ct = ClanTable.getInstance();
		GrandBossManager gm = GrandBossManager.getInstance();
		for (FastMap.Entry<Integer, ClanInfo> e = _clans.head(), end = _clans.tail(); (e = e.getNext()) != end;) 
		{
			Integer id = e.getKey(); // No typecast necessary.
			clan = e.getValue();
			if (id == null)
				continue;
			if(clan == null)
				continue;
				
			loc = getSpawns(team);
			
			bossId = clan._boss;
			if (bossId < 10)
				bossId = 35430;
			gm.createOnePrivateEx(getCamp(team), loc.x, loc.y, loc.z, loc.h);
			boss = (L2MonsterInstance)gm.createOnePrivateEx(bossId, loc.x+Rnd.get(110,130), loc.y+Rnd.get(110,130), loc.z, loc.h);
			clan._bossObj = boss.getObjectId();
			boss.setTitle(ct.getClan(id).getName());
			
			if (id == _ownerClan)
				continue;
				
			FastList<L2PcInstance> pcs = clan._players;
			for (FastList.Node<L2PcInstance> n = pcs.head(), fend = pcs.tail(); (n = n.getNext()) != fend;) 
			{
				player = n.getValue();
				if(player == null)
					continue;
				player.teleToLocation(loc.x+Rnd.get(70,110), loc.y+Rnd.get(70,110), loc.z, false);
			}
			team++;
		}
	}
	
	public void startSiege()
	{
		try
		{
			spawnClans();
		}
		catch (Exception e)
		{
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new StartBattle(), 1000);
	}	
	
	public class StartBattle implements Runnable
	{
		public StartBattle()
		{
		}
		
		public void run()
		{	
			_inProgress = true;
			long maxTime = 1200000;
			if (_finalBattle)
			{	
				DoorTable dt = DoorTable.getInstance();
				dt.getDoor(22170003).openMe();
				dt.getDoor(22170004).openMe();
				try
				{
					spawnClans();
				}
				catch (Exception e)
				{
				}
				maxTime = 5600000;
			}
			
			for (int i = 0; i < maxTime; i += 3000)
			{
				try
				{
					Thread.sleep(3000);
					if (haveWinner())
						break;
				}
				catch (InterruptedException e)
				{
				}
			}
			_inProgress = false;

			int winId = 0;
			ClanInfo clan = null;
			for (FastMap.Entry<Integer, ClanInfo> e = _clans.head(), end = _clans.tail(); (e = e.getNext()) != end;) 
			{
				Integer id = e.getKey(); // No typecast necessary.
				clan = e.getValue();
				if (id == null)
					continue;
					
				winId = id;
			}
			
			if (winId == 0)
			{
				CastleManager.getInstance().getCastleById(35).getSiege().endSiege();
				load();
				return;
			}
			
			if (_finalBattle)
				setOwner(winId);
			else
			{
				if(_clans.size() >= 2)
				{
					if (_ownerClan != 0)
						setOwner(_ownerClan);
					else
						CastleManager.getInstance().getCastleById(35).getSiege().endSiege();
		
					load();
					return;
				}
				
				if(_owner.isEmpty())
				{
					setOwner(winId);
					return;
				}
				
				_camps.add(35423);
				_camps.add(35424);
		
				_finalBattle = true;
				_clans.clear();
				
				ClanInfo ci = _owner.get(_ownerClan);
				_clans.put(_ownerClan, ci);
				
				clan._boss = 35430;
				_clans.put(winId, clan);
				
				ThreadPoolManager.getInstance().scheduleGeneral(new StartBattle(), 30000);
			}
		}
	}
	
	protected boolean haveWinner()
	{
		if(_clans.size() == 1)
			return true;
		
		return false;
	}
	
	private void setOwner(int clanId)
	{
		if(!ClanHallManager.getInstance().isFree(35))
			ClanHallManager.getInstance().setFree(35);
						
		ClanHallManager.getInstance().setOwner(35, ClanTable.getInstance().getClan(clanId));
		CastleManager.getInstance().getCastleById(35).getSiege().endSiege();
		load();
	}
	
	public void cancel()
	{
		load();
	}
	
	public void notifyDeath(int bossObj)
	{
		int clanId = 0;
		ClanInfo clan = null;
		L2PcInstance player = null;
		for (FastMap.Entry<Integer, ClanInfo> e = _clans.head(), end = _clans.tail(); (e = e.getNext()) != end;) 
		{
			Integer id = e.getKey(); // No typecast necessary.
			clan = e.getValue();
			if (id == null)
				continue;
			if(clan == null)
				continue;
			if(clan._bossObj != bossObj)
				continue;
				
			clanId = id;
			FastList<L2PcInstance> pcs = clan._players;
			for (FastList.Node<L2PcInstance> n = pcs.head(), fend = pcs.tail(); (n = n.getNext()) != fend;) 
			{
				player = n.getValue();
				if(player == null)
					continue;
				player.teleToLocation(77310, 50317, -3160, false);
			}
		}
		if (clanId > 1)
			_clans.remove(clanId);
	}
	
	public synchronized void acceptNpc(int id, L2PcInstance player, L2NpcInstance npc)
	{
		if (_camps.isEmpty())
		{
			npc.showChatWindow(player, "data/html/siege/35437-closed.htm");
			return;
		}
		
		int bossId = 0;
		switch(id)
		{
			case 0:
				bossId = 35428;
				break;
			case 1:
				bossId = 35429;
				break;
			case 2:
				bossId = 35430;
				break;
			case 3:
				bossId = 35431;
				break;
			case 4:
				bossId = 35432;
				break;
		}
		
		ClanInfo ci = null;
		if (player.getClan().getClanId() == _ownerClan)
			ci = _owner.get(_ownerClan);
		else
			ci = _clans.get(player.getClan().getClanId());
		ci._boss = bossId;
		//_camps.delete(id);
		npc.showChatWindow(player, "data/html/siege/35437-accept.htm");
		
		/*ClanInfo ci = null;
		for (FastMap.Entry<Integer, ClanInfo> e = _clans.head(), end = _clans.tail(); (e = e.getNext()) != end;) 
		{
			Integer id = e.getKey(); // No typecast necessary.
			ci = n.getValue();
			if (id == null)
				continue;
			
			if (id == clan.getClanId())
			{
				ci._boss = id;
				_camps.remove(_camps.indexOf(id));
			}
		}*/
	}
	
	public synchronized void regPlayer(L2PcInstance player, L2NpcInstance npc)
	{
		ClanInfo ci = null;
		if (player.getClan().getClanId() == _ownerClan)
			ci = _owner.get(_ownerClan);
		else
			ci = _clans.get(player.getClan().getClanId());
			
		if (ci._players.size() >= 18)
		{
			npc.showChatWindow(player, "data/html/siege/35437-max.htm");
			return;
		}
		if (!ci._players.contains(player))
			ci._players.add(player);
		npc.showChatWindow(player, "data/html/siege/35437-accept.htm");
	}
	
	private Location getSpawns(int team)
	{
		if (_finalBattle)
		{
			Location f = new Location(81981, -15708, -1858, 60392);
			if (team == 1)
				f = new Location(84375, -17060, -1860, 27712);
			return f;
		}
		return _locs.get(team);
	}
	
	public FastMap<Integer, ClanInfo> getAttackers()
	{
		return _clans;
	}
	
	private int getCamp(int team)
	{
		return _camps.get(team);
	}
	
	public boolean isRegistered(L2Clan clan)
	{
		if (_clans.containsKey(clan.getClanId()))
			return true;
			
		return false;	
	}
	
	public boolean isRegTime()
	{
		long siege = CastleManager.getInstance().getCastleById(35).getSiegeDate().getTimeInMillis();
		long time = Calendar.getInstance().getTimeInMillis();
		if ((siege - time) < 3600 && (siege - time) > 0)
			return true;
			
		return false;
	}
	
	public boolean inProgress()
	{
		return _inProgress;
	}
}
