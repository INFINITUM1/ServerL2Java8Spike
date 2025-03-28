package scripts.zone.type;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import scripts.zone.L2ZoneType;

public class L2NoblessRbZone extends L2ZoneType
{
	private String _zoneName;
	
	public L2NoblessRbZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("name"))
		{
			_zoneName = value;
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2RaidBossInstance)
		{
			L2RaidBossInstance boss = (L2RaidBossInstance)character;
			
			if (boss.getNpcId() == 25325 && !(boss.isDead() || boss.isAlikeDead()))
			{
				if (boss.getZ() < -2749)
				{
					try 
					{
						boss.deleteMe();
						L2NpcTemplate template = NpcTable.getInstance().getTemplate(25325);
						L2Spawn spawn = new L2Spawn(template);
						spawn.setHeading(30000);
						spawn.setLocx(91171);
						spawn.setLocy(-85971);
						spawn.setLocz(-2714);
						spawn.stopRespawn();
						spawn.spawnOne();
					}
					catch (Exception e1){}
				}
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		//
		
		if (character instanceof L2RaidBossInstance)
		{
			L2RaidBossInstance boss = (L2RaidBossInstance)character;
			
			if (boss.getNpcId() == 25325 && !(boss.isDead() || boss.isAlikeDead()))
			{
				try
				{
					boss.deleteMe();
					L2NpcTemplate template = NpcTable.getInstance().getTemplate(25325);
					L2Spawn spawn = new L2Spawn(template);
					spawn.setHeading(30000);
					spawn.setLocx(91171);
					spawn.setLocy(-85971);
					spawn.setLocz(-2714);
					spawn.stopRespawn();
					spawn.spawnOne();
				}
				catch (Exception e1){}
			}
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
}