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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package scripts.items.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import scripts.items.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.0.0.0.0.0 $ $Date: 2005/09/02 19:41:13 $
 */

public class Firework implements IItemHandler
{
    //Modified by Baghak (Prograsso): Added Firework support
    private static final int[] ITEM_IDS = { 6403, 6406, 6407 };

    public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
    {
    	if(!(playable.isPlayer())) 
			return; // prevent Class cast exception
        L2PcInstance pl = (L2PcInstance)playable;
		
		if(System.currentTimeMillis() - pl.gCPBG() < 5500)
			return;
		pl.sCPBG();
		
		int fwId = 0;
		switch(item.getItemId())
		{
			case 6403: // elven_firecracker, xml: 2023
				fwId = 2023;
				break;
			case 6406: // firework, xml: 2024
				fwId = 2024;
				break;
			case 6407: // large_firework, xml: 2025
				fwId = 2025;
				break;
		}
		
        //useFw(pl, 2025, 1);
		playable.broadcastPacket(new MagicSkillUser(playable, playable, fwId, 1, 1, 0));
        playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
    }
    public void useFw(L2PcInstance pl, int magicId,int level)
    {
        L2Skill skill = SkillTable.getInstance().getInfo(magicId,level);
        if (skill != null) {
            pl.useMagic(skill, false, false);
        }
    }
    public int[] getItemIds()
    {
        return ITEM_IDS;
    }
}