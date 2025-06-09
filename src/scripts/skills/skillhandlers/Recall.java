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
package scripts.skills.skillhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import javolution.util.FastList;
import javolution.util.FastList.Node;

public class Recall implements ISkillHandler
{
	//private static Logger _log = Logger.getLogger(Recall.class.getName());
	private static final SkillType[] SKILL_IDS = {SkillType.RECALL};

 	public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
	{
        if (activeChar.isPlayer())
        {
			L2PcInstance player = (L2PcInstance)activeChar;
        	// Thanks nbd
        	if (!TvTEvent.onEscapeUse(player.getName()))
        	{
        		player.sendActionFailed();
        		return;
        	}

            if (player.isInOlympiadMode())
            {
                player.sendPacket(Static.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
                return;
            }
			if (player.isEventWait() || player.getChannel() > 1)
			{
				player.sendPacket(Static.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
				return;
			}
        }

		try
        {
			for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
			{
				L2Object obj = n.getValue();
				if (obj == null || !(obj.isL2Character()))
					continue;

				L2Character target = (L2Character)obj;

                if (target.isPlayer())
                {
                    L2PcInstance targetChar = (L2PcInstance)target;

                    // Check to see if the current player target is in a festival.
                    if (targetChar.isFestivalParticipant()) {
                        targetChar.sendPacket(SystemMessage.sendString("You may not use an escape skill in a festival."));
                        continue;
                    }

                    // Check to see if player is in jail
                    if (targetChar.isInJail())
                    {
                        targetChar.sendPacket(SystemMessage.sendString("You can not escape from jail."));
                        continue;
                    }

                    // Check to see if player is in a duel
                    if (targetChar.isInDuel())
                    {
                        targetChar.sendPacket(SystemMessage.sendString("You cannot use escape skills during a duel."));
                        continue;
                    }
					if (targetChar.isEventWait() || targetChar.getChannel() > 1)
					{
						targetChar.sendMessage("Нельзя призывать на эвенте");
						return;
					}
                }

                target.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
        } catch (Throwable e) {
 	 	 	if (Config.DEBUG) e.printStackTrace();
 	 	}
		//targets.clear();
 	}

	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}