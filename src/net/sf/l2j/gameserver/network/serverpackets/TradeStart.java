package net.sf.l2j.gameserver.network.serverpackets;

import java.util.ArrayList;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class TradeStart extends L2GameServerPacket {

    private ArrayList<L2ItemInstance> _tradelist = new ArrayList<L2ItemInstance>();
    private boolean can_writeImpl = false;
    private int requester_obj_id;

    public TradeStart(L2PcInstance me) {
        if (me == null) {
            return;
        }

        if (me.getActiveTradeList() == null || me.getActiveTradeList().getPartner() == null) {
            return;
        }

        requester_obj_id = me.getActiveTradeList().getPartner().getObjectId();

        L2ItemInstance[] inventory = me.getInventory().getAvailableItems(true);
        for (L2ItemInstance item : inventory) {
            if (!item.isEquipped() && item.getItem().getType2() != 3 && item.isTradeable()) {
                _tradelist.add(item);
            }
        }

        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        writeC(0x1E);
        writeD(requester_obj_id);
        int count = _tradelist.size();
        writeH(count);//count??

        for (L2ItemInstance temp : _tradelist) {
            writeH(temp.getItem().getType1()); // item type1
            writeD(temp.getObjectId());
            writeD(temp.getItemId());
            writeD(temp.getCount());
            writeH(temp.getItem().getType2()); // item type2
            writeH(0x00); // ?

            writeD(temp.getItem().getBodyPart()); // rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
            writeH(temp.getEnchantLevel()); // enchant level
            writeH(0x00); // ?
            writeH(0x00);
        }
    }

    @Override
    public void gc() {
        _tradelist.clear();
        _tradelist = null;
    }
}
