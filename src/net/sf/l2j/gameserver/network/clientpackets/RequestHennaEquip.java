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

import java.nio.BufferUnderflowException;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2Henna;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public final class RequestHennaEquip extends L2GameClientPacket {

    private int _symbolId;
    // format  cd

    /**
     * packet type id 0xbb
     * format:		cd
     * @param decrypt
     */
    @Override
    protected void readImpl() {
        try
        {
            _symbolId = readD();
        } catch (BufferUnderflowException e) {
            _symbolId = -1;
        }
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);
        if (template == null) {
            return;
        }

        L2HennaInstance dye = new L2HennaInstance(template);
        int _count = 0;

        /* Prevents henna drawing exploit: 
        1) talk to L2SymbolMakerInstance 
        2) RequestHennaList
        3) Don't close the window and go to a GrandMaster and change your subclass
        4) Get SymbolMaker range again and press draw
        You could draw any kind of henna just having the required subclass...
         */
        boolean c = true;
        for (L2HennaInstance h : HennaTreeTable.getInstance().getAvailableHenna(player.getClassId())) {
            if (h.getSymbolId() == dye.getSymbolId()) {
                c = false;
                break;
            }
        }

        if (c) {
            player.sendPacket(Static.CANT_DRAW_SYMBOL);
            player.sendActionFailed();
            return;
        }

        try {
            _count = player.getInventory().getItemByItemId(dye.getItemIdDye()).getCount();
        } catch (Exception e) {
        }

        if (player.getAdena() < dye.getPrice() || _count < dye.getAmountDyeRequire()) {
            player.sendMessage("ѕроверь стоимость и количество красок");
            player.sendActionFailed();
            return;
        }

        if (player.addHenna(dye)) {
            player.getInventory().reduceAdena("Henna", dye.getPrice(), player, player.getLastFolkNPC());
            player.destroyItemByItemId("HennaEquip", dye.getItemIdDye(), dye.getAmountDyeRequire(), player.getLastFolkNPC(), true);

            player.sendPacket(Static.SYMBOL_ADDED);
            player.sendItems(false);
            player.sendChanges();
        }
    }
}
