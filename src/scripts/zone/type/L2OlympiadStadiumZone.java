
package scripts.zone.type;

import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import scripts.zone.L2ZoneType;

public class L2OlympiadStadiumZone extends L2ZoneType
{
	private int _stadiumId;

	public L2OlympiadStadiumZone(int id)
	{
		super(id);
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("stadiumId"))
		{
			_stadiumId = Integer.parseInt(value);
		}
		else super.setParameter(name, value);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (character.isPlayer())
		{
			L2PcInstance player = (L2PcInstance)character;
			
			//player.sendMessage("id: " + _stadiumId);
			
			if (player.inObserverMode() || player.inFClub() || player.inFightClub() || player.isEventWait())
				return;
			
			if (EventManager.getInstance().isReg(player))
				return;

	        //if (!Olympiad.getInstance().isRegisteredInComp(player) && !player.isGM())
            //{
            //    player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			//	return;
            //}
			
			character.setInsideZone(L2Character.ZONE_NOLANDING, true);
			//character.setInsideZone(L2Character.ZONE_PVP, true);
			player.setInOlumpiadStadium(true);
			//player.sendPacket(SystemMessage.id(SystemMessageId.ENTERED_COMBAT_ZONE));
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		//character.setInsideZone(L2Character.ZONE_PVP, false);
		character.setInsideZone(L2Character.ZONE_NOLANDING, false);

		if (character.isPlayer())
		{	
			L2PcInstance player = (L2PcInstance)character;
			
			// handle removal from olympiad game
			//if (Olympiad.getInstance().isRegistered(player) || player.getOlympiadGameId() != -1) 
				//Olympiad.getInstance().removeDisconnectedCompetitor(player);
				
			//EventManager.getInstance().onExit(player);
			
			//player.sendPacket(SystemMessage.id(SystemMessageId.LEFT_COMBAT_ZONE));
			player.setInOlumpiadStadium(false);
		}
	}
		

	
	@Override
	protected void onDieInside(L2Character character) {}

	@Override
	protected void onReviveInside(L2Character character) {}

	/**
	 * Returns this zones stadium id (if any)
	 * @return
	 */
	public int getStadiumId()
	{
		return _stadiumId;
	}
}
