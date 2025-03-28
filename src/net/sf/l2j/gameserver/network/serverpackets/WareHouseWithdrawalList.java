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
package net.sf.l2j.gameserver.network.serverpackets;

//import java.util.logging.Logger;
import java.util.NoSuchElementException;
import javolution.util.FastTable;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.ItemContainer;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * 0x42 WarehouseWithdrawalList  dh (h dddhh dhhh d)
 *
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/29 23:15:10 $
 */
public class WareHouseWithdrawalList extends L2GameServerPacket {

    public static final int PRIVATE = 1;
    public static final int CLAN = 2;
    public static final int CASTLE = 3; //not sure
    public static final int FREIGHT = 4; //not sure
    //private static Logger _log = Logger.getLogger(WareHouseWithdrawalList.class.getName());
    private L2PcInstance _activeChar;
    private int _playerAdena;
    private FastTable<L2ItemInstance> _items;
    private int _whType;
    private boolean can_writeImpl = false;

    public WareHouseWithdrawalList(L2PcInstance player, int type) {
        _activeChar = player;
        _whType = type;
        ItemContainer warehouse = player.getActiveWarehouse();
        _items = new FastTable<L2ItemInstance>();
        switch (_whType) {
            case PRIVATE:
                _items.addAll(warehouse.listItems(1));
                break;
            case CLAN:
            case CASTLE:
                _items.addAll(warehouse.listItems(2));
                break;
            case FREIGHT:
                _items.addAll(warehouse.listItems(4));
                break;
            default:
                throw new NoSuchElementException("Invalid value of 'type' argument");
        }

        _playerAdena = _activeChar.getAdena();
        /*if (_activeChar.getActiveWarehouse() == null)
        {
        // Something went wrong!
        //_log.warning("error while sending withdraw request to: " + _activeChar.getName());
        return;
        }
        else _items = _activeChar.getActiveWarehouse().getItems();*/

        if (_items.size() == 0) {
            player.sendPacket(Static.NO_ITEM_DEPOSITED_IN_WH);
            return;
        }

        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        writeC(0x42);
        /* 0x01-Private Warehouse
         * 0x02-Clan Warehouse
         * 0x03-Castle Warehouse
         * 0x04-Warehouse */
        writeH(_whType);
        writeD(_playerAdena);
        writeH(_items.size());

        for (int i = 0, n = _items.size(); i < n; i++) {
            L2ItemInstance item = _items.get(i);
            if (item == null || item.getItem() == null) {
                continue;
            }

            writeH(item.getItem().getType1()); // item type1 //unconfirmed, works
            writeD(0x00); //unconfirmed, works
            writeD(item.getItemId()); //unconfirmed, works
            writeD(item.getCount()); //unconfirmed, works
            writeH(item.getItem().getType2());	// item type2 //unconfirmed, works
            writeH(0x00);	// ?
            writeD(item.getItem().getBodyPart());	// ?
            writeH(item.getEnchantLevel());	// enchant level -confirmed
            writeH(0x00);	// ?
            writeH(0x00);	// ?
            writeD(item.getObjectId()); // item id - confimed
            if (item.isAugmented()) {
                //System.out.println("213" + item.getAugmentation());
                //writeD(0x0000FFFF&item.getAugmentation().getAugmentationId());
                //writeD(item.getAugmentation().getAugmentationId()>>16);
                writeD(item.getAugmentation().getAugmentationId() & 0x0000FFFF);
                writeD(item.getAugmentation().getAugmentationId() >> 16);
            } else {
                writeQ(0x00);
                //System.out.println("00");
            }
        }
    }

    @Override
    public void gc() {
        _items.clear();
        _items = null;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
    public String getType() {
        return "S.WareHouseWithdrawalList";
    }
}
