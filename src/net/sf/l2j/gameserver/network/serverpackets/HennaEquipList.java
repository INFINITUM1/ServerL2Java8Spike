/*
 * $Header$
 *
 *
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
package net.sf.l2j.gameserver.network.serverpackets;

import javolution.util.FastTable;
import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class HennaEquipList extends L2GameServerPacket
{
	private boolean can_writeImpl = true;
    private L2PcInstance _player;
    private FastTable<L2HennaInstance> _hennaEquipList;

    public HennaEquipList(L2PcInstance player)
    {
        _player = player;
		_hennaEquipList = HennaTreeTable.getInstance().getAvailableHenna(_player.getClassId());
		if (_hennaEquipList == null || _hennaEquipList.isEmpty())
		{
			player.sendMessage("Приходите после 2й профы");
			can_writeImpl = false;
		}
    }

    @Override
	protected final void writeImpl()
    {
		if (!can_writeImpl)
			return;
			
        writeC(0xe2);
        writeD(_player.getAdena());          //activeChar current amount of aden
        writeD(3);     //available equip slot
        //writeD(10);    // total amount of symbol available which depends on difference classes
        writeD(_hennaEquipList.size());

		for (int i = 0, n = _hennaEquipList.size(); i < n; i++)
		{
			L2HennaInstance temp = _hennaEquipList.get(i);
		
            if ((_player.getInventory().getItemByItemId(temp.getItemIdDye())) != null)
            {
                writeD(temp.getSymbolId()); //symbolid
                writeD(temp.getItemIdDye());       //itemid of dye
                writeD(temp.getAmountDyeRequire());    //amount of dye require
                writeD(temp.getPrice());    //amount of aden require
                writeD(1);            //meet the requirement or not
            }
            else
            {
                writeD(0x00);
                writeD(0x00);
                writeD(0x00);
                writeD(0x00);
                writeD(0x00);
            }
        }
    }
	
	/*@Override
	public void gc()
	{
		_hennaEquipList.clear();
		_hennaEquipList = null;
	}*/
}
