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

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PledgeReceivePowerInfo;

/**
 * Format: (ch) dS
 * @author  -Wooden-
 *
 */
public final class RequestPledgeMemberPowerInfo extends L2GameClientPacket
{
    @SuppressWarnings("unused")
    private int _unk1;
    private String _player;


    @Override
	protected void readImpl()
    {
        _unk1 = readD();
        _player = readS();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
	protected void runImpl()
    {
        //System.out.println("C5: RequestPledgeMemberPowerInfo d:"+_unk1);
        //System.out.println("C5: RequestPledgeMemberPowerInfo S:"+_player);
        L2PcInstance player = getClient().getActiveChar();
        if(player == null)
        	return;
        //do we need powers to do that??
        L2Clan clan = player.getClan();
        if(clan == null)
        	return;
        L2ClanMember member = clan.getClanMember(_player);
        if(member == null)
        	return;
        player.sendPacket(new PledgeReceivePowerInfo(member));
    }

    /**
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return "C.PledgeMemberPowerInfo";
    }

}