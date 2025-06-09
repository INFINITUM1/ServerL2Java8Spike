/*
 * $Header: MultiSellList.java, 2/08/2005 14:21:01 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 2/08/2005 14:21:01 $
 * $Revision: 1 $
 * $Log: MultiSellList.java,v $
 * Revision 1  2/08/2005 14:21:01  luisantonioa
 * Added copyright notice
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

import java.util.Collection;
import javolution.util.FastList;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellEntry;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellIngredient;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellListContainer;

/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public class MultiSellList extends L2GameServerPacket {

    protected int _listId, _page, _finished;
    private Collection<MultiSellEntry> _possiblelist = new FastList<>();

    public MultiSellList(MultiSellListContainer list, int page, int finished, boolean flag) {
        _possiblelist.addAll(list.getEntries());
        _listId = list.getListId();
        _page = page;
        _finished = finished;
    }

    @Override
    protected void writeImpl() {
        // [ddddd] [dchh] [hdhdh] [hhdh]
        writeC(0xd0);
        writeD(_listId);    // list id
        writeD(_page);		// page
        writeD(_finished);	// finished
        writeD(0x28);	// size of pages
        writeD(_possiblelist == null ? 0 : _possiblelist.size()); //list lenght

        if (_possiblelist == null) {
            return;
        }

        ItemTable it = ItemTable.getInstance();
        for (MultiSellEntry ent : _possiblelist) {
            writeD(ent.getEntryId());
            writeD(0x00); // C6
            writeD(0x00); // C6
            writeC(1);
            writeH(ent.getProducts().size());
            writeH(ent.getIngredients().size());

            for (MultiSellIngredient prod : ent.getProducts()) {
                writeH(prod.getItemId());
                writeD(it.getTemplate(prod.getItemId()).getBodyPart());
                writeH(it.getTemplate(prod.getItemId()).getType2());
                writeD(prod.getItemCount());
                writeH(prod.getEnchantmentLevel()); //enchtant lvl
                writeD(0x00); // C6
                writeD(0x00); // C6
            }

            for (MultiSellIngredient i : ent.getIngredients()) {
                int itemId = i.getItemId();
                switch (itemId) {
                    case 65336:
                    case 65436:
                        writeH(itemId);      //ID
                        writeH(itemId);
                        break;
                    default:
                        writeH(itemId);      //ID
                        writeH(it.getTemplate(i.getItemId()).getType2());
                        break;
                }
                writeD(i.getItemCount());	//Count
                writeH(i.getEnchantmentLevel()); //Enchant Level
                writeD(0x00); // C6
                writeD(0x00); // C6
            }
        }
    }

    @Override
    public void gc() {
        _possiblelist.clear();
        _possiblelist = null;
    }
}
