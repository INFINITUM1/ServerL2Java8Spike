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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ManufactureItem;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopMsg;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 * cd(dd)
 * @version $Revision: 1.1.2.3.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestRecipeShopListSet extends L2GameClientPacket
{
	private int _count;
	private int[] _items; // count*2


	@Override
	protected void readImpl()
	{
		_count = readD();
		if (_count < 0  || _count * 8 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
            _count = 0;
		_items = new int[_count * 2];
        for (int x = 0; x < _count ; x++)
        {
            int recipeID = readD(); _items[x*2 + 0] = recipeID;
            int cost     = readD(); _items[x*2 + 1] = cost;
        }
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;

		if (player.isInDuel())
		{
			player.sendPacket(Static.CANT_CRAFT_DURING_COMBAT);
			return;
		}

		if (_count == 0 || player.getCreateList() == null)
		{
			player.setPrivateStoreType(L2PcInstance.PS_NONE);
			player.broadcastUserInfo();
			player.standUp();
		}
		else
		{
            L2ManufactureList createList = new L2ManufactureList();

            for (int x = 0; x < _count ; x++)
            {
                int recipeID = _items[x*2 + 0];
                int cost     = _items[x*2 + 1];
                createList.add(new L2ManufactureItem(recipeID, cost));
            }
            createList.setStoreName(player.getCreateList() != null ? player.getCreateList().getStoreName() : "");
            player.setCreateList(createList);

			player.setPrivateStoreType(L2PcInstance.PS_MANUFACTURE);
			player.sitDown();
			player.saveTradeList();
			player.broadcastUserInfo();
			player.sendPacket(new RecipeShopMsg(player));
			player.broadcastPacket(new RecipeShopMsg(player));
		}
	}


	@Override
	public String getType()
	{
		return "C.RecipeShopListSet";
	}


}
