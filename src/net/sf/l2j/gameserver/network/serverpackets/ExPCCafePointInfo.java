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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class ExPCCafePointInfo extends L2GameServerPacket
{
	private int pcBangPoints, m_AddPoint, m_PeriodType, PointType;
	
	public ExPCCafePointInfo(L2PcInstance player, int modify, boolean add, boolean _double)
	{
		m_AddPoint = modify;
		pcBangPoints = player.getPcPoints();
		m_PeriodType = add ? 1 : 2;
		if(add)
			PointType = _double ? 2 : 1;
		else
			PointType = 0;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x31);
		writeD(pcBangPoints);
		writeD(m_AddPoint);
		writeC(m_PeriodType);
		writeD(0);
		writeC(PointType);
	}
}
