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
package net.sf.l2j.gameserver.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import javolution.util.FastTable;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

public class PetInventory extends Inventory
{
	private final L2PetInstance _owner;
	
	public FastTable<L2ItemInstance> listItems()
	{
		final FastTable<L2ItemInstance> items = new FastTable<L2ItemInstance>();
		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			String loc ="PET";

			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);		
            statement = con.prepareStatement("SELECT object_id FROM items WHERE owner_id=? AND (loc=?) ORDER BY object_id DESC");
            statement.setInt(1, getOwnerId());
            statement.setString(2, loc);
			rset = statement.executeQuery();

			L2ItemInstance item;
			while(rset.next())
			{
				final int objectId = rset.getInt(1);
				item = L2ItemInstance.restoreFromDb(objectId);
				if(item == null)
					continue;
				items.add(item);
			}
		}
		catch(final Exception e)
		{
			//_log.log(Level.SEVERE, "could not restore warehouse:", e);
		}
		finally
		{
            Close.CSR(con, statement, rset);
		}
		return items;
	}	

	public PetInventory(L2PetInstance owner)
    {
		_owner = owner;
	}

	@Override
	public L2PetInstance getOwner()
    {
        return _owner;
    }

	@Override
	protected ItemLocation getBaseLocation()
    {
        return ItemLocation.PET;
    }

	@Override
	protected ItemLocation getEquipLocation()
    {
        return ItemLocation.PET_EQUIP;
    }
}
