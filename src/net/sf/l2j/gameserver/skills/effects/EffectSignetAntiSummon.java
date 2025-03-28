package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;

final class EffectSignetAntiSummon extends L2Effect
{
	public EffectSignetAntiSummon(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Effect#getEffectType()
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SIGNET_GROUND;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
	 */
	@Override
	public void onStart()
	{
		if (!getEffected().isPlayer())
			return;
			
		L2PcInstance pc = (L2PcInstance) getEffected();	
		pc.setNoSummon(true);
		if (pc.getPet() != null)
			pc.getPet().unSummon(pc);
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
	 */
	@Override
	public boolean onActionTime()
	{
		if (!getEffected().isPlayer())
			return false;
			
		((L2PcInstance) getEffected()).setNoSummon(false);
		return false;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
	 */
	@Override
	public void onExit()
	{
	}
}
