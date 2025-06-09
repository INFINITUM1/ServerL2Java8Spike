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

import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;

/**
 *
 * @author  -Wooden-
 */
public class PledgeReceiveWarList extends L2GameServerPacket
{
	private L2Clan _clan;
	private int _tab;
	private ConcurrentLinkedQueue<PledgeInfo> _list = new ConcurrentLinkedQueue<PledgeInfo>();

	public PledgeReceiveWarList(L2Clan clan, int tab)
	{
		_clan = clan;
		_tab = tab;
		_list.clear();

		L2Clan pledge = null;
		ClanTable ct = ClanTable.getInstance();
		for(int i : _tab == 0 ? _clan.getWarList() : _clan.getAttackerList())
		{
			pledge = ct.getClan(i);
			if (pledge == null) 
				continue;

			_list.add(new PledgeInfo(pledge.getName(), _tab, _tab));
		}
	}

	/**
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x3e);

		writeD(_tab); // type : 0 = Declared, 1 = Under Attack
		writeD(0x00); // page
		writeD(_list.size());

		for(PledgeInfo pledge : _list)
		{
			if (pledge == null) 
				continue;

			writeS(pledge.name);
			writeD(pledge.tab1); //??
			writeD(pledge.tab2); //??
		}
	}

	static class PledgeInfo
	{
		public String name;
		public int tab1, tab2;

		public PledgeInfo(String name, int tab1, int tab2)
		{
			this.name = name;
			this.tab1 = tab1;
			this.tab2 = tab2;
		}
	}
}
