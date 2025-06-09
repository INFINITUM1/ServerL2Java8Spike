
package scripts.zone.type;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import scripts.zone.L2ZoneType;


public class L2ElfTreeZone extends L2ZoneType
{
	public L2ElfTreeZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (character.isPlayer())
		{
			L2PcInstance player = (L2PcInstance)character;

			if (player.getRace() != Race.elf) 
				return;

			player.setInsideZone(L2Character.ZONE_MOTHERTREE, true);
			player.setInElfTree(true);
			player.sendPacket(SystemMessage.id(SystemMessageId.ENTER_SHADOW_MOTHER_TREE));
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (character.isPlayer() && character.isInsideZone(L2Character.ZONE_MOTHERTREE))
		{
			L2PcInstance player = (L2PcInstance)character;
			
			character.setInsideZone(L2Character.ZONE_MOTHERTREE, false);
			player.setInElfTree(false);
			player.sendPacket(SystemMessage.id(SystemMessageId.EXIT_SHADOW_MOTHER_TREE));
		}
	}

	@Override
	protected void onDieInside(L2Character character) {}

	@Override
	protected void onReviveInside(L2Character character) {}

}
