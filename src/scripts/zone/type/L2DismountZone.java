/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package scripts.zone.type;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.Ride;

public class L2DismountZone extends L2ZoneType
{
	public L2DismountZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
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
