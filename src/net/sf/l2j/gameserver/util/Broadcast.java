/*
 * $Header: Broadcast.java, 18/11/2005 15:33:35 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 18/11/2005 15:33:35 $
 * $Revision: 1 $
 * $Log: Broadcast.java,v $
 * Revision 1  18/11/2005 15:33:35  luisantonioa
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
package net.sf.l2j.gameserver.util;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;

/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public final class Broadcast {

    //private static final Logger _log = AbstractLogger.getLogger(Broadcast.class.getName());
    /**
     * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character that have the Character targetted.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
     * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR><BR>
     *
     */
    public static void toPlayersTargettingMyself(L2Character character, L2GameServerPacket mov) {
        sendToPlayersTargettingMyself(character, mov, character.getKnownList().getListKnownPlayers(), null);
    }
    
    private static void sendToPlayersTargettingMyself(final L2Character character, final L2GameServerPacket mov, final FastList<L2PcInstance> players, L2PcInstance pc) {
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            
            if (character.equals(pc.getTarget())) {
                pc.sendPacket(mov);
            }
        }
        
        mov.gcb();
       //pc = null;
        //mov = null;
    }

    /**
     * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
     * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR><BR>
     *
     */
    public static void toKnownPlayers(L2Character character, L2GameServerPacket mov) {
        sendToPlayers(character, mov, character.getKnownList().getListKnownPlayers(), null);
    }
    
    private static void sendToPlayers(final L2Character cha, final L2GameServerPacket mov, final FastList<L2PcInstance> players, L2PcInstance pc) {
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            
            pc.sendPacket(mov);
            if (mov.isCharInfo() && cha.isPlayer()) {
                int relation = cha.getRelation(pc);
                if (cha.getKnownList().updateRelationsFor(pc.getObjectId(), relation)) {
                    pc.sendPacket(new RelationChanged(cha.getPlayer(), relation, pc.isAutoAttackable(cha)));
                }
            }
        }
        
        mov.gcb();
        //pc = null;
        //mov = null;
    }
    
    public static void broadcastSoulShotsPacket(final L2GameServerPacket mov, final FastList<L2PcInstance> players, L2PcInstance pc) {
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            if (pc.showSoulShotsAnim()) {
                pc.sendPacket(mov);
            }
        }
        mov.gcb();
        //pc = null;
        //mov = null;
    }

    /**
     * Send a packet to all L2PcInstance in the _KnownPlayers (in the specified radius) of the L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
     * In order to inform other players of state modification on the L2Character, server just needs to go through _knownPlayers to send Server->Client Packet
     * and check the distance between the targets.<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR><BR>
     *
     */
    public static void toKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, int radius) {
        if (radius < 0) {
            radius = 1500;
        }
        sendToKnownPlayersInRadius(mov, character.getKnownList().getKnownPlayersInRadius(radius), null);
    }
    
    public static void toKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, int radius, boolean f) {
        if (radius < 0) {
            radius = 1500;
        }
        sendToPlayers(character, mov, character.getKnownList().getKnownPlayersInRadius(radius), null);
    }
    
    private static void sendToKnownPlayersInRadius(final L2GameServerPacket mov, final FastList<L2PcInstance> players, L2PcInstance pc) {
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            
            pc.sendPacket(mov);
        }
        
        mov.gcb();
       // pc = null;
        //mov = null;
    }

    /**
     * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character and to the specified character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
     * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet<BR><BR>
     *
     */
    public static void toSelfAndKnownPlayers(L2Character character, L2GameServerPacket mov) {
        if (character.isPlayer()) {
            character.sendPacket(mov);
        }
        
        toKnownPlayers(character, mov);
    }

    // To improve performance we are comparing values of radius^2 instead of calculating sqrt all the time
    public static void toSelfAndKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, long radiusSq) {
        if (radiusSq < 0) {
            radiusSq = 360000;
        }
        
        if (character.isPlayer()) {
            character.sendPacket(mov);
        }
        
        sendToSelfAndKnownPlayersInRadius(character, mov, character.getKnownList().getListKnownPlayers(), null, radiusSq);
    }
    
    private static void sendToSelfAndKnownPlayersInRadius(final L2Character character, final L2GameServerPacket mov, final FastList<L2PcInstance> players, L2PcInstance pc, long radiusSq) {
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }
            
            if (character.getDistanceSq(pc) <= radiusSq) {
                pc.sendPacket(mov);
            }
        }
        
        mov.gcb();
        //pc = null;
        //mov = null;
    }

    /**
     * Send a packet to all L2PcInstance present in the world.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR>
     * In order to inform other players of state modification on the L2Character, server just need to go through _allPlayers to send Server->Client Packet<BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use method toSelfAndKnownPlayers)</B></FONT><BR><BR>
     *
     */
    public static void toAllOnlinePlayers(L2GameServerPacket mov) {
        for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
            if (player == null) {
                continue;
            }
            
            if (player.isInOfflineMode() || player.isFantome()) {
                continue;
            }
            
            player.sendPacket(mov);
        }
    }
}
