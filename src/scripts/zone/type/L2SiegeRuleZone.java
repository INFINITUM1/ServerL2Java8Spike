
package scripts.zone.type;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;

public class L2SiegeRuleZone extends L2ZoneType
{
	public L2SiegeRuleZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		character.setInSiegeRuleArea(true);
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		character.setInSiegeRuleArea(false);
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
