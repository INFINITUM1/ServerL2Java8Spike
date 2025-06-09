
package scripts.zone.type;

import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import scripts.zone.L2ZoneType;

public class L2OlympiadTexture extends L2ZoneType
{
	private int _stadiumId;

	public L2OlympiadTexture(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		/*character.setInsideZone(L2Character.ZONE_PVP, false);
		if (character.isPlayer())
		{	
			L2PcInstance player = (L2PcInstance)character;
			player.sendMessage("ololo!!");
			
			EventManager.getInstance().onTexture(player);
			// handle removal from olympiad game
			if (Olympiad.getInstance().isRegistered(player) || player.getOlympiadGameId() != -1) 
				Olympiad.getInstance().removeDisconnectedCompetitor(player);
		}*/
	}

	@Override
	protected void onExit(L2Character character)
	{
		//character.setInsideZone(L2Character.ZONE_PVP, false);
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
