package scripts.ai;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.bosses.AntharasManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class Antharas extends L2GrandBossInstance
{
	//заспавнен
	public static long _lastHit = 0;
	//юз скилла
	private static final long _sklChk = 60000;
	//смена таргета
	private static final long _trgChk = 40000;
	
	//скиллы - антарас
	private static final int anJump = 4106;
	private static final int anTail = 4107;
	private static final int anFear = 4108;
	private static final int anDebuff = 4109;
	//private static final int Antaramouth = 4110;
	private static final int anBreath = 4111;
	private static final int anNorm = 4112;
	private static final int anNormEx = 4113;
	
	public Antharas(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
    @Override
	public void onSpawn()
	{ 
    	super.onSpawn();
		StartAnim();
		_lastHit = System.currentTimeMillis();
		
		if (getSpawn() != null)
		{
			getSpawn().setLocx(179596);
            getSpawn().setLocy(114921);
            getSpawn().setLocz(-7708);
		}
	} 
	
    @Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
    {
		_lastHit = System.currentTimeMillis();
        super.reduceCurrentHp(damage, attacker, awake);
	} 
	
    @Override
	public boolean doDie(L2Character killer)
    {
    	super.doDie(killer);
		AntharasManager.getInstance().notifyDie();
		return true;
	}
	
    @Override
	public void deleteMe()
    {
        super.deleteMe();
    }
	
	class ChangeTarget implements Runnable
	{
		ChangeTarget()
		{
		}

		public void run()
		{			
			FastList<L2PcInstance> _players = getKnownList().getKnownPlayersInRadius(1200);
			if (!_players.isEmpty())
			{
				L2PcInstance trg = _players.get(Rnd.get(_players.size()-1));
				if (trg != null)
				{
					setTarget(trg);
					addDamageHate(trg,0,999);
					getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, trg);
				}
			}
			ThreadPoolManager.getInstance().scheduleAi(new ChangeTarget(), _trgChk, false);
		}
	}
	
	class CastSkill implements Runnable
	{
		CastSkill()
		{
		}

		public void run()
		{			
			int rnd = Rnd.get(100);
			if(getCurrentHp()> ((getMaxHp() * 3) / 4))
			{
				if(rnd < 6)
					addUseSkillDesire(anBreath,1);
				else if(rnd < 10)
					addUseSkillDesire(anJump,1);
				else if(rnd < 15)
					addUseSkillDesire(anFear,1);
				else if(rnd < 50)
					addUseSkillDesire(anNorm,1);
				else if(rnd < 80)
					addUseSkillDesire(anTail,1);
				else
					addUseSkillDesire(anNormEx,1);
			}
			else if(getCurrentHp() > ((getMaxHp() * 2) / 4))
			{
				if(rnd < 6)
					addUseSkillDesire(anBreath,1);
				else if(rnd < 10)
					addUseSkillDesire(anJump,1);
				else if(rnd < 15)
					addUseSkillDesire(anFear,1);
				else if(rnd < 40)
					addUseSkillDesire(anDebuff,1);
				else if(rnd < 50)
					addUseSkillDesire(anNorm,1);
				else if(rnd < 80)
					addUseSkillDesire(anTail,1);
				else
					addUseSkillDesire(anNormEx,1);
			}
			else if(getCurrentHp() > ((getMaxHp() * 1) / 4))
			{
				if(rnd < 6)
					addUseSkillDesire(anBreath,1);
				else if(rnd < 10)
					addUseSkillDesire(anJump,1);
				else if(rnd < 15)
					addUseSkillDesire(anFear,1);
				else if(rnd < 40)
					addUseSkillDesire(anDebuff,1);
				else if(rnd < 50)
					addUseSkillDesire(anNorm,1);
				else if(rnd < 80)
					addUseSkillDesire(anTail,1);
				else
					addUseSkillDesire(anNormEx,1);
			}
			else if(rnd < 6 )
				addUseSkillDesire(anBreath,1);
			else if(rnd < 10 )
				addUseSkillDesire(anFear,1);
			else if(rnd < 50 )
				addUseSkillDesire(anNorm,1);
			else if(rnd < 80 )
				addUseSkillDesire(anTail,1);
			else
				addUseSkillDesire(anNormEx,1);
				
			ThreadPoolManager.getInstance().scheduleAi(new CastSkill(), _sklChk, false);
		}
	}
	
	class Anima implements Runnable
	{
		private int _act;
		
		Anima(int act)
		{
			_act = act;
		}

		public void run()
		{	
			L2PcInstance pc = null;
			FastList<L2PcInstance> _players = getKnownList().getKnownPlayersInRadius(2500);
			switch(_act)
			{
				case 1:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.specialCamera(Antharas.this, 700, 13, -19, 0, 10000);
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(2), 3000, false);
					break;
				case 2:
					broadcastPacket(new SocialAction(getObjectId(), 1));
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.specialCamera(Antharas.this, 700, 13, 0, 6000, 10000);
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(3), 10000, false);
					break;
				case 3:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.specialCamera(Antharas.this, 3800, 0, -3, 0, 10000);
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(4), 200, false);
					break;
				case 4:
					broadcastPacket(new SocialAction(getObjectId(), 2));
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.specialCamera(Antharas.this, 1200, 0, -3, 22000, 11000);
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(5), 10800, false);
					break;
				case 5:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.specialCamera(Antharas.this, 1200, 0, -3, 300, 2000);
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(6), 1900, false);
					break;
				case 6:
					ThreadPoolManager.getInstance().scheduleAi(new ChangeTarget(), _trgChk, false);
					ThreadPoolManager.getInstance().scheduleAi(new CastSkill(), _sklChk, false);
					break;
			}
		}
	}
	
	public long getLastHit()
	{
		return _lastHit;
	}
	
	public void StartAnim()
	{
		ThreadPoolManager.getInstance().scheduleAi(new Anima(1), 1000, false);
	}
	
	@Override
	public boolean checkRange()
	{
		return false;
	}
}
