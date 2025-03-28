
package scripts.zone.type;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import scripts.zone.L2ZoneType;
//import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;

public class L2S400Zone extends L2ZoneType
{
	public L2S400Zone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (character.isPlayer())
		{	
			L2PcInstance player = (L2PcInstance)character;
			
			if (player.inObserverMode())
				return;	
				
			if (EventManager.getInstance().isReg(player))
				return;
			
			//if(!Olympiad.getInstance().isRegisteredInComp(player) && !player.isGM())
			//{
			//		
			//	player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			//	return;
			//}

	        //if (player.isMounted() && !player.isGM())
            //{
           //     player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			//	return;
          //  }
		}	
	}
	
	@Override
	protected void onExit(L2Character character)
	{
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
