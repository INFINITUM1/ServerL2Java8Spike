package scripts.ai;

import javolution.util.FastList;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.bosses.QueenAntManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.jython.QuestJython;
import net.sf.l2j.gameserver.network.serverpackets.CharMoveToLocation;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class QueenAntNurse extends L2MonsterInstance
{
	private static QueenAnt aq = null;
	private static QueenAntLarva larva = null;
	
	private boolean process = false;
	
	public QueenAntNurse(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
    @Override
	public void onSpawn()
	{
		setRunning();
		ThreadPoolManager.getInstance().scheduleAi(new Heal(), 10000, false);
	} 
	
	public void setAq(QueenAnt aq)
	{
		this.aq = aq;
	}
	
	public void setLarva(QueenAntLarva larva)
	{
		this.larva = larva;
	}
	
    @Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
    {
        super.reduceCurrentHp(damage, attacker, awake);
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
	
	class Heal implements Runnable
	{
		Heal()
		{
		}

		public void run()
		{
			if (aq == null || aq.isDead())
			{
				deleteMe();
				return;
			}
			if (process)
			{
				ThreadPoolManager.getInstance().scheduleAi(new Heal(), 10000, false);
				return;
			}
			
			if(larva != null && larva.getCurrentHp() < larva.getMaxHp())
			{
				process = true;
				doHeal(larva);
			}
			else if(aq.getCurrentHp() < aq.getMaxHp())
			{
				process = true;
				doHeal(aq);
			}
			ThreadPoolManager.getInstance().scheduleAi(new Heal(), 10000, false);
		}
	}

	private void doHeal(L2Object trg)
	{		
		if (aq == null || aq.isDead())
		{
			deleteMe();
			return;
		}

		if (!Util.checkIfInRange(800, this, trg, false)) 
		{
			moveToLocationm(trg.getX() + Rnd.get(150), trg.getY() + Rnd.get(150), trg.getZ(), 0);
			broadcastPacket(new CharMoveToLocation(this));
		}
		else
		{
			setTarget(trg);
			addUseSkillDesire(4020, 1);
		}
		process = false;
	}
}
