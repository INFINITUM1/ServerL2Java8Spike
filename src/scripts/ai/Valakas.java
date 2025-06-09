package scripts.ai;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.bosses.ValakasManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.State;
import net.sf.l2j.gameserver.model.quest.jython.QuestJython;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class Valakas extends L2GrandBossInstance
{
	//заспавнен
	public static long _lastHit = 0;
	//юз скилла
	private static final long _sklChk = 60000;
	//смена таргета
	private static final long _trgChk = 40000;
	
	//скиллы - валакас
	private static int NorMalAttackLeft = 4681;
	private static int NorMalAttackRight = 4682;
	private static int RearStrike = 4685;
	private static int RearThrow = 4688;
	private static int Meteor = 4690;
	private static int BreathHigh = 4684;
	private static int BreathLow = 4683;
	private static int Fear = 4689;
	//private int Valakaregeneration = 4691;
	private static int PowerUp = 4680;
	//private int ReadyRearAttack = 4687;

	public Valakas(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

    @Override
	public void onSpawn()
	{ 
		startAnim();
		_lastHit = System.currentTimeMillis();
    	super.onSpawn();
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
		deathAnim();
		ValakasManager.getInstance().notifyDie();
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
			if( getCurrentHp() > ( ( getMaxHp() * 3.000000 ) / 4.000000 ) )
			{
				if(rnd < 6)
				{
					addUseSkillDesire(Meteor,1);
					//broadcastPacket(new SpecialCamera(getObjectId(),1600,13,1,1,8000));
				}
				else if(rnd < 15)
					addUseSkillDesire(Fear,1);
				else if(rnd < 60)
				{
					int rndEx = Rnd.get(100);
					if(rndEx < 7)
						addUseSkillDesire(RearStrike,1);
					else if(rndEx < 10)
						addUseSkillDesire(RearThrow,1);
					else if(rndEx < 15)
						addUseSkillDesire(BreathLow,1);
					else
					{
						if(Rnd.get(100) < 50)
							addUseSkillDesire(NorMalAttackRight,1);
						else
							addUseSkillDesire(NorMalAttackLeft,1);
					}
				}
				else
					addUseSkillDesire(BreathHigh,1);
			}
			else if( getCurrentHp() > ( ( getMaxHp() * 2.000000 ) / 4.000000 ) )
			{
				if(rnd < 6)
				{
					addUseSkillDesire(Meteor,1);
					//broadcastPacket(new SpecialCamera(getObjectId(),1600,13,1,1,8000));
				}
				else if(rnd < 10)
					addUseSkillDesire(Fear,1);
				else if(rnd < 60)
				{
					int rndEx = Rnd.get(100);
					if(rndEx < 15)
						addUseSkillDesire(RearStrike,1);
					else if(rndEx < 17)
						addUseSkillDesire(RearThrow,1);
					else if(rndEx < 20)
						addUseSkillDesire(BreathLow,1);
					else
					{
						if( Rnd.get(100) < 50)
							addUseSkillDesire(NorMalAttackRight,1);
						else
							addUseSkillDesire(NorMalAttackLeft,1);
					}
				}
				else
					addUseSkillDesire(BreathHigh,1);
			}
			else if( getCurrentHp() > ( ( getMaxHp() * 1.000000 ) / 4.000000 ) )
			{
				if(rnd < 20)
				{
					addUseSkillDesire(Meteor,1);
					//broadcastPacket(new SpecialCamera(getObjectId(),1600,13,1,1,8000));
				}
				else if(rnd < 25)
					addUseSkillDesire(Fear,1);
				else if(rnd < 60)
				{
					int rndEx = Rnd.get(100);
					if(rndEx < 17)
						addUseSkillDesire(RearStrike,1);
					else if(rndEx < 19)
						addUseSkillDesire(RearThrow,1);
					else if(rndEx < 35)
						addUseSkillDesire(BreathLow,1);
					else
					{
						if(Rnd.get(100) < 50)
							addUseSkillDesire(NorMalAttackRight,1);
						else
							addUseSkillDesire(NorMalAttackLeft,1);
					}
				}
				else
					addUseSkillDesire(BreathHigh,1);
			}
			else 
			{
				if(rnd < 6)
				{
					addUseSkillDesire(Meteor,1);
					//broadcastPacket(new SpecialCamera(getObjectId(),1600,13,1,1,8000));
				}
				else if(rnd < 10)
					addUseSkillDesire(Fear,1);
				else if(rnd < 60)
				{
					int rndEx = Rnd.get(100);
					if(rndEx < 5)
						addUseSkillDesire(RearStrike,1);
					else if(rndEx < 7)
						addUseSkillDesire(RearThrow,1);
					else if(rndEx < 15)
						addUseSkillDesire(BreathLow,1);
					else
					{
						if(Rnd.get(100) < 50)
							addUseSkillDesire(NorMalAttackRight,1);
						else
							addUseSkillDesire(NorMalAttackLeft,1);
					}
				}
				else
					addUseSkillDesire(BreathHigh,1);
			}
				
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
			int oobjId = getObjectId();
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
						pc.sendPacket(new SpecialCamera(oobjId, 1800, 180, -1, 1500, 15000, 0, 0, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(2), 1500, false);
					break;
				case 2:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 1300, 180, -5, 3000, 15000, 0, -5, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(3), 3300, false);
					break;
				case 3:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 500, 180, -8, 600, 15000, 0, 60, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(4), 2900, false);
					break;
				case 4:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 800, 180, -8, 2700, 15000, 0, 30, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(5), 2700, false);
					break;
				case 5:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 200, 250, 70, 0, 15000, 30, 80, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(6), 1, false);
					break;
				case 6:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 1100, 250, 70, 2500, 15000, 30, 80, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(7), 3200, false);
					break;
				case 7:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 700, 150, 30, 0, 15000, -10, 60, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(8), 1400, false);
					break;
				case 8:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 1200, 150, 20, 2900, 15000, -10, 30, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(9), 6700, false);
					break;
				case 9:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 750, 170, -10, 3400, 15000, 10, -15, 1, 0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(10), 5700, false);
					break;
				case 10:
					/*for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId, 750, 170, -10, 3400, 15000, 10, -15, 1, 0));
					}*/
					addUseSkillDesire(4691, 1);
					//ThreadPoolManager.getInstance().scheduleAi(new Anima(10), 5700, false);
					break;
				case 11:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId,1100,210,-5,3000,15000,-13,0,1,0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(12),3500, false);
					break;
				case 12:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId,1300,200,-8,3000,15000,0,15,1,0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(13),4500, false);
					break;
				case 13:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId,1000,190,0,500,15000,0,10,1,0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(14),500, false);
					break;
				case 14:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId,1700,120,0,2500,15000,12,40,1,0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(15),4600, false);
					break;
				case 15:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId,1700,20,0,700,15000,10,10,1,0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(16),750, false);
					break;
				case 16:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId,1700,10,0,1000,15000,20,70,1,0));
					}
					ThreadPoolManager.getInstance().scheduleAi(new Anima(17),2500, false);
					break;
				case 17:
					for (FastList.Node<L2PcInstance> n = _players.head(), end = _players.tail(); (n = n.getNext()) != end;)
					{
						pc = n.getValue();
						if (pc == null)
							continue;
						pc.sendPacket(new SpecialCamera(oobjId,1700,10,0,300,15000,20,-20,1,0));
					}
					break;
			}
		}
	}
	
	public void startAnim()
	{
		if (Config.DISABLE_BOSS_INTRO)
			addUseSkillDesire(4691, 1);
		else
			ThreadPoolManager.getInstance().scheduleAi(new Anima(1), 2000, false);
	}
	
	public void deathAnim()
	{
		ThreadPoolManager.getInstance().scheduleAi(new Anima(11), 500, false);
	}
	
	public long getLastHit()
	{
		return _lastHit;
	}
	
	@Override
	public boolean checkRange()
	{
		return false;
	}
}
