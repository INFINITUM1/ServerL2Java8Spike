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
package net.sf.l2j.gameserver.model.actor.instance;

import javolution.text.TextBuilder;
import javolution.util.FastTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.network.serverpackets.HennaEquipList;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class L2SymbolMakerInstance extends L2FolkInstance {
    //private static Logger _log = Logger.getLogger(L2SymbolMakerInstance.class.getName());

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.equals("Draw")) {
            player.sendPacket(new HennaEquipList(player));
        } else if (command.equals("RemoveList")) {
            showRemoveChat(player);
        } else if (command.startsWith("Remove ")) {
            int slot = Integer.parseInt(command.substring(7));
            player.removeHenna(slot);
        } else {
            super.onBypassFeedback(player, command);
        }
    }

    public L2SymbolMakerInstance(int objectID, L2NpcTemplate template) {
        super(objectID, template);
    }

    @Override
    public String getHtmlPath(int npcId, int val) {
        return "data/html/symbolmaker/SymbolMaker.htm";
    }

    /*
     * (non-Javadoc) @see net.sf.l2j.gameserver.model.L2Object#isAttackable()
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return false;
    }
}
