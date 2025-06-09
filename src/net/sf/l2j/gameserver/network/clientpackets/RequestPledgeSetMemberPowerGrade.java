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

/**
 * Format: (ch) Sd
 * @author  -Wooden-
 *
 */
public final class RequestPledgeSetMemberPowerGrade extends L2GameClientPacket
{
    private int _powerGrade;
    private String _member;


    @Override
	protected void readImpl()
    {
        _member = readS();
        _powerGrade = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
	protected void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
        if(player == null)
        	return;
        L2Clan clan = player.getClan();
        if(clan == null)
        	return;
        L2ClanMember member = clan.getClanMember(_member);
        if(member == null)
        	return;
        if(member.getPledgeType() == L2Clan.SUBUNIT_ACADEMY)
        {
        	// also checked from client side
        	player.sendMessage("You cannot change academy member grade");
        	return;
        }
        member.setPowerGrade(_powerGrade);
        clan.broadcastClanStatus();
    }

    /**
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return "C.PledgeSetMemberPowerGrade";
    }

}