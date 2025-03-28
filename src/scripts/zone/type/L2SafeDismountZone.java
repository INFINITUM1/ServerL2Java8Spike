
package scripts.zone.type;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.Ride;

public class L2SafeDismountZone extends L2ZoneType
{
	public L2SafeDismountZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		character.setInZonePeace(true);
		
		if (character.isPlayer())
		{
			L2PcInstance activeChar = (L2PcInstance)character;
			
			activeChar.setInDismountZone(true);
			
	        if (activeChar.isMounted())
            {
				if (activeChar.setMountType(0))
				{
					if (activeChar.isFlying())
						activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
						
					Ride dismount = new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT, 0);
					activeChar.broadcastPacket(dismount);
					activeChar.setMountObjectID(0);
				}
				return;
            }
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInZonePeace(false);
		
		if (character.isPlayer())
		{
			L2PcInstance activeChar = (L2PcInstance)character;
			activeChar.setInDismountZone(false);
		}
	}

	@Override
	protected void onDieInside(L2Character character) {}

	@Override
	protected void onReviveInside(L2Character character) {}
}
