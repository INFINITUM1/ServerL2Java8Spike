package scripts.ai;

import javolution.util.FastList;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.jython.QuestJython;
import net.sf.l2j.gameserver.network.serverpackets.CharMoveToLocation;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class QueenAntLarva extends L2MonsterInstance
{
	public QueenAntLarva(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
    @Override
	public void onSpawn()
	{ 
    	super.onSpawn();
		setIsImobilised(true);
	} 
	
    @Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
    {
        super.reduceCurrentHp(damage, attacker, awake);
		
		if(attacker.isPlayer() && (attacker.getLevel() > getLevel() + 8))
		{
			if (((L2PcInstance)attacker).isMageClass())
				SkillTable.getInstance().getInfo(4215, 1).getEffects(attacker, attacker);
			else
				SkillTable.getInstance().getInfo(4515, 1).getEffects(attacker, attacker);
		}
	} 
	
    @Override
	public boolean doDie(L2Character killer)
    {
    	super.doDie(killer);
		if (getSpawn() != null)
			getSpawn().setLastKill(System.currentTimeMillis());
		return true;
	}
	
    @Override
	public void deleteMe()
    {
        super.deleteMe();
    }
}
