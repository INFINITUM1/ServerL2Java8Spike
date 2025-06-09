/*
 * This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

public final class RequestEvaluate extends L2GameClientPacket
{
	@SuppressWarnings("unused")
    private int _targetId;

	@Override
	protected void readImpl()
	{
		_targetId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;
		
		if (player.getTarget() == null)
		    return;


        if (!(player.getTarget().isPlayer()))
        {
            player.sendPacket(Static.TARGET_IS_INCORRECT);
            return;
        }

        if (player.getLevel() < 10)
        {
            player.sendPacket(Static.ONLY_LEVEL_SUP_10_CAN_RECOMMEND);
            return;
        }

        if (player.getTarget() == player)
        {
            player.sendPacket(Static.YOU_CANNOT_RECOMMEND_YOURSELF);
            return;
        }

        if (player.getRecomLeft() <= 0)
        {
            player.sendPacket(Static.NO_MORE_RECOMMENDATIONS_TO_HAVE);
            return;
        }

        L2PcInstance target = player.getTarget().getPlayer();
        if (target.getRecomHave() >= 255)
        {
            player.sendPacket(Static.YOU_NO_LONGER_RECIVE_A_RECOMMENDATION);
            return;
        }

        if (!player.canRecom(target))
        {
            player.sendPacket(Static.THAT_CHARACTER_IS_RECOMMENDED);
            return;
        }

        player.giveRecom(target);
        player.sendPacket(new UserInfo(player));
		target.broadcastUserInfo();
		player.sendPacket(SystemMessage.id(SystemMessageId.YOU_HAVE_RECOMMENDED).addString(target.getName()).addNumber(player.getRecomLeft()));
		target.sendPacket(SystemMessage.id(SystemMessageId.YOU_HAVE_BEEN_RECOMMENDED).addString(player.getName()));
	}
}
