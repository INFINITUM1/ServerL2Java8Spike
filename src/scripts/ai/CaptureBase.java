package scripts.ai;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import scripts.autoevents.basecapture.BaseCapture;

public class CaptureBase extends L2MonsterInstance
{
	public CaptureBase(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
    @Override
	public void onSpawn()
	{
		super.onSpawn();
		setIsImobilised(true);
		setIsParalyzed(true);
	}
	
	
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTargetBase(player))
		{
			player.sendActionFailed();
			return;
		}
		super.onAction(player);
	}
	
    @Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
    {
		L2PcInstance pc = null;
        if (attacker instanceof L2PcInstance) 
			pc = (L2PcInstance)attacker;
        else if (attacker instanceof L2Summon) 
			pc = ((L2Summon)attacker).getOwner();
		
		if (pc == null)
			return;
					
		if (canTargetBase(pc))
			super.reduceCurrentHp(damage, attacker, awake);
    }
	
    @Override
    public boolean doDie(L2Character killer)
    {
    	if (!super.doDie(killer))
    		return false;
			
    	BaseCapture.getEvent().notifyBaseDestroy(getTemplate().npcId);
        return true;
    }
	
	private boolean canTargetBase(L2PcInstance player)
	{
		if (getTemplate().npcId == Config.EBC_BASE1ID && BaseCapture.getEvent().isInTeam2(player))
			return true;
		else if (getTemplate().npcId == Config.EBC_BASE2ID && BaseCapture.getEvent().isInTeam1(player))
			return true;
		
		return false;
	}
	
    @Override
	public boolean isRaid()
    {
        return true;
    }
}
