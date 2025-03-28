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

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import scripts.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * A mother-trees zone
 *
 * @author  durgus
 */
public class L2MotherTreeZone extends L2ZoneType
{
	public L2MotherTreeZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (character.isPlayer())
		{
			L2PcInstance player = (L2PcInstance)character;

			/*if (player.isInParty())
			{
				for (L2PcInstance member : player.getParty().getPartyMembers())
					if (member.getRace() != Race.elf) return;
			}*/
			if (player.getRace() != Race.elf) 
				return;

			player.setInsideZone(L2Character.ZONE_MOTHERTREE, true);
			player.sendPacket(SystemMessage.id(SystemMessageId.ENTER_SHADOW_MOTHER_TREE));
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (character.isPlayer() && character.isInsideZone(L2Character.ZONE_MOTHERTREE))
		{
			character.setInsideZone(L2Character.ZONE_MOTHERTREE, false);
			((L2PcInstance)character).sendPacket(SystemMessage.id(SystemMessageId.EXIT_SHADOW_MOTHER_TREE));
		}
	}

	@Override
	protected void onDieInside(L2Character character) {}

	@Override
	protected void onReviveInside(L2Character character) {}

}
