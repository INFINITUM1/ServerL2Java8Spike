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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;
import javolution.text.TextBuilder;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class L2DoormenInstance extends L2FolkInstance
{
    private ClanHall _clanHall;
    private static int COND_ALL_FALSE = 0;
    private static int COND_BUSY_BECAUSE_OF_SIEGE = 1;
    private static int COND_CASTLE_OWNER = 2;
    private static int COND_HALL_OWNER = 3;
	private static DoorTable _dt = DoorTable.getInstance();

    /**
     * @param template
     */
    public L2DoormenInstance(int objectID, L2NpcTemplate template)
    {
        super(objectID, template);
    }

    public final ClanHall getClanHall()
    {
		switch(getTemplate().npcId)
		{
			case 35433:
			case 35434:
			case 35435:
			case 35436:
				_clanHall = ClanHallManager.getInstance().getClanHallById(35);
				break;
			case 35641:
			case 35642:
				_clanHall = ClanHallManager.getInstance().getClanHallById(64);
				break;
		}
        if (_clanHall == null)
            _clanHall = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);
        return _clanHall;
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command)
    {
        int condition = validateCondition(player);
        if (condition <= COND_ALL_FALSE) return;
        if (condition == COND_BUSY_BECAUSE_OF_SIEGE) return;
        else if (condition == COND_CASTLE_OWNER || condition == COND_HALL_OWNER)
        {
            if (command.startsWith("Chat"))
            {
                showMessageWindow(player);
                return;
            }
            else if (command.startsWith("open_doors"))
            {
                if (condition == COND_HALL_OWNER)
                {
					switch(getTemplate().npcId)
					{
						case 35433: // бандит
						case 35434:
							_dt.getDoor(22170003).openMe();
							_dt.getDoor(22170004).openMe(); 
							break;
						case 35435: // бандит
						case 35436:
							_dt.getDoor(22170001).openMe();
							_dt.getDoor(22170002).openMe(); 
							break;
						case 35641: // дед форест
							_dt.getDoor(21170001).openMe(); 
							_dt.getDoor(21170002).openMe();
							break;
						case 35642: // дед форест
							_dt.getDoor(21170003).openMe();
							_dt.getDoor(21170004).openMe(); 
							_dt.getDoor(21170005).openMe(); 
							_dt.getDoor(21170006).openMe(); 
							break;
						default:
							getClanHall().openCloseDoors(true);
					}
                    player.sendPacket(NpcHtmlMessage.id(getObjectId(), "<html><body>Двери открыты.<br>Не забудьте закрыть двери, иначе посторонние смогут войти.<br><center><a action=\"bypass -h npc_" + getObjectId() + "_close_doors\">Закрой двери.</a></center></body></html>"));
                }
                else
                {
                    //DoorTable doorTable = DoorTable.getInstance();
                    StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
                    st.nextToken(); // Bypass first value since its castleid/hallid

                    if (condition == 2)
                    {
                        while (st.hasMoreTokens())
                        {
                            getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
                        }
                        return;
                    }

                }
            }
            else if (command.startsWith("close_doors"))
            {
                if (condition == COND_HALL_OWNER)
                {
					switch(getTemplate().npcId)
					{
						case 35434: // бандит
						case 35433:
							_dt.getDoor(22170003).closeMe();
							_dt.getDoor(22170004).closeMe(); 
							break;
						case 35435:
						case 35436:
							_dt.getDoor(22170001).closeMe();
							_dt.getDoor(22170002).closeMe(); 
							break;
						case 35641: // дед форест
							_dt.getDoor(21170001).closeMe(); 
							_dt.getDoor(21170002).closeMe();
							break;
						case 35642: // дед форест
							_dt.getDoor(21170003).closeMe();
							_dt.getDoor(21170004).closeMe(); 
							_dt.getDoor(21170005).closeMe(); 
							_dt.getDoor(21170006).closeMe(); 
							break;
						default:
							getClanHall().openCloseDoors(false);
					}
                    player.sendPacket(NpcHtmlMessage.id(getObjectId(), "<html><body>Двери закрыты.<br>Всего хорошего!<br><center><a action=\"bypass -h npc_" + getObjectId() + "_open_doors\">Открой двери.</a></center></body></html>"));
                }
                else
                {
                    //DoorTable doorTable = DoorTable.getInstance();
                    StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
                    st.nextToken(); // Bypass first value since its castleid/hallid

                    //L2Clan playersClan = player.getClan();

                    if (condition == 2)
                    {
                        while (st.hasMoreTokens())
                        {
                            getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
                        }
                        return;
                    }
                }
            }
        }

        super.onBypassFeedback(player, command);
    }

	/**
	* this is called when a player interacts with this NPC
	* @param player
	*/
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player)) return;

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendActionFailed();
	}

    public void showMessageWindow(L2PcInstance player)
    {
        player.sendActionFailed();
        String filename = "data/html/doormen/" + getTemplate().npcId + "-no.htm";

        int condition = validateCondition(player);
        if (condition == COND_BUSY_BECAUSE_OF_SIEGE) filename = "data/html/doormen/"
            + getTemplate().npcId + "-busy.htm"; // Busy because of siege
        else if (condition == COND_CASTLE_OWNER) // Clan owns castle
            filename = "data/html/doormen/" + getTemplate().npcId + ".htm"; // Owner message window

        // Prepare doormen for clan hall
        NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
        if (getClanHall() != null)
        {
			TextBuilder tb = new TextBuilder("<html><body>");
            if (condition == COND_HALL_OWNER)
            {
                tb.append("Что я могу сделать для вас?<br><br>");
                tb.append("<center><a action=\"bypass -h npc_%objectId%_open_doors\">Открой двери.</a><br>");
                tb.append("<a action=\"bypass -h npc_%objectId%_close_doors\">Закрой двери.</a></center></body></html>");
            }
            else
            {
                L2Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
                if (owner != null && owner.getLeader() != null)
                {
                    tb.append("Я не должен с вами разговаривать, иначе у меня могут быть неприятности!<br>");
					tb.append("Все, что я могу сказать, это то, что кланхолл пренадлежит клану<br> <font color=\"55FFFF\">" + owner.getName() + "</font>");
                }
                else 
					tb.append("В данный момент у кланхолла <font color=\"LEVEL\">" + getClanHall().getName() + "</font> нет владельца.<br><br></body></html>");
            }
            html.setHtml(tb.toString());
			tb.clear();
			tb = null;
        }
        else 
			html.setFile(filename);

        html.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(html);
    }

    private int validateCondition(L2PcInstance player)
    {
        if (player.getClan() != null)
        {
            // Prepare doormen for clan hall
            if (getClanHall() != null)
            {
                if (player.getClanId() == getClanHall().getOwnerId()) return COND_HALL_OWNER;
                else return COND_ALL_FALSE;
            }
            if (getCastle() != null && getCastle().getCastleId() > 0)
            {
                //		        if (getCastle().getSiege().getIsInProgress())
                //		            return COND_BUSY_BECAUSE_OF_SIEGE;									// Busy because of siege
                //		        else
                if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
                    return COND_CASTLE_OWNER; // Owner
            }
        }

        return COND_ALL_FALSE;
    }
}
