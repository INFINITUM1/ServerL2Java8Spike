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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.0.0.0 $ $Date: 2005/07/11 15:29:30 $
 */
public final class RequestAutoSoulShot extends L2GameClientPacket
{
    private static Logger _log = Logger.getLogger(RequestAutoSoulShot.class.getName());

    // format  cd
    private int _itemId;
    private int _type; // 1 = on : 0 = off;

    @Override
	protected void readImpl()
    {
        _itemId = readD();
        _type = readD();
    }

    @Override
	protected void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();

        if (player == null)
            return;

        if (player.getPrivateStoreType() == 0 && player.getTransactionRequester() == null && !player.isDead())
        {
           // if (Config.DEBUG)
            //    _log.fine("AutoSoulShot:" + _itemId);
//
            L2ItemInstance item = player.getInventory().getItemByItemId(_itemId);

            if (item != null)
            {
                if (_type == 1)
                {
                	//Fishingshots are not automatic on retail
                	if (_itemId < 6535 || _itemId > 6540) 
					{
	                    // Attempt to charge first shot on activation
	                    if (_itemId == 6645 || _itemId == 6646 || _itemId == 6647)
	                    {
	                    	if (player.getPet() != null)
	                    	{
	                    		player.addAutoSoulShot(_itemId);
	                    		player.sendPacket(new ExAutoSoulShot(_itemId, _type));
	                    		//start the auto shot use
	                    		player.sendPacket(SystemMessage.id(SystemMessageId.USE_OF_S1_WILL_BE_AUTO).addString(item.getItemName()));
	                    		player.rechargeAutoSoulShot(true, true, true);
	                    	}
	                    	else
	                    	{
	                    		player.sendPacket(Static.PETS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
	                    		player.sendPacket(Static.NO_SERVITOR_CANNOT_AUTOMATE_USE);
	                    	}
	                    }
	                    else 
						{
							if (_itemId>=3947 && _itemId<=3952 && player.isInOlympiadMode())
							{
								player.sendPacket(Static.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
	                    	}
	                    	else
	                    	{
                   	           	player.addAutoSoulShot(_itemId);
                   	           	player.sendPacket(new ExAutoSoulShot(_itemId, _type));
                   	            // start the auto shot use
                   	            player.sendPacket(SystemMessage.id(SystemMessageId.USE_OF_S1_WILL_BE_AUTO).addString(item.getItemName()));
	                    		player.rechargeAutoSoulShot(true, true, false);
	                    	}
	                    	/*else {
	                    		if ((_itemId >= 2509 && _itemId <= 2514) || (_itemId >= 3947 && _itemId <= 3952) || _itemId == 5790)
	                    			player.sendPacket(SystemMessage.id(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH));
	                    		else
	                    			player.sendPacket(SystemMessage.id(SystemMessageId.SOULSHOTS_GRADE_MISMATCH));
	                    	}*/
	                    }
                    }
                }
                else if (_type == 0)
                {
                    player.removeAutoSoulShot(_itemId);
                    player.sendPacket(new ExAutoSoulShot(_itemId, _type));
                    //cancel the auto soulshot use
                    player.sendPacket(SystemMessage.id(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addString(item.getItemName()));
                }
            }
        }
    }
}