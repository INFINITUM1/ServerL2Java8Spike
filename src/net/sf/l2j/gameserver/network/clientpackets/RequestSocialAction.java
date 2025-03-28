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
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.6.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestSocialAction extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestSocialAction.class.getName());

	// format  cd
	private int _actionId;


	@Override
	protected void readImpl()
	{
		_actionId  = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;
			
		if(System.currentTimeMillis() - player.gCPP() < 500)
			return;

		player.sCPP();

        // You cannot do anything else while fishing
        if (player.isFishing())
        {
            player.sendPacket(Static.CANNOT_DO_WHILE_FISHING_3);
            return;
        }

        // check if its the actionId is allowed
        if (_actionId < 2 || _actionId > 13)
        {
        	//Util.handleIllegalPlayerAction(player, "Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" requested an internal Social Action.", Config.DEFAULT_PUNISH);
        	return;
        }

		if (	player.getPrivateStoreType()==0 &&
				player.getTransactionRequester()==null &&
				!player.isAlikeDead() &&
				(!player.isAllSkillsDisabled() || player.isInDuel()) &&
				player.getAI().getIntention()==CtrlIntention.AI_INTENTION_IDLE)
		{
			//if (Config.DEBUG) _log.fine("Social Action:" + _actionId);

			player.broadcastPacket(new SocialAction(player.getObjectId(), _actionId));
			/*
			// Schedule a social task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new SocialTask(this), 2600);
			player.setIsParalyzed(true);
			*/
		}
	}
	/*
	class SocialTask implements Runnable
	{
		L2PcInstance _player;
		SocialTask(RequestSocialAction action)
		{
			_player = getClient().getActiveChar();
		}
		public void run()
		{
			_player.setIsParalyzed(false);
		}
	}
	*/

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "C.SocialAction";
	}
}
