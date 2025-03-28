package net.sf.l2j.gameserver.network.serverpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager.WaitingRoom;
import net.sf.l2j.gameserver.instancemanager.TownManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Format:(ch) d d [dsdddd]
 */
public class ExPartyRoomMembers extends L2GameServerPacket {

    private WaitingRoom _room;
    private boolean _owner;
    private ConcurrentLinkedQueue<L2PcInstance> _players;

    public ExPartyRoomMembers(L2PcInstance player, WaitingRoom room) {
        _room = room;
        _players = room.players;
        _owner = _room.owner.equals(player);
    }

    @Override
    protected final void writeImpl() {
        if (_players == null) {
            return;
        }

        writeC(0xfe);
        writeH(0x0E);

        writeD((_owner ? 0x01 : 0x00));
        //writeD(0x01);
        writeD(_room.players.size());
        for (L2PcInstance player : _players) {
            if (player == null) {
                continue;
            }

            writeD(player.getObjectId());
            writeS(player.getName());
            writeD(player.getActiveClass());
            writeD(player.getLevel());
            writeD(TownManager.getInstance().getClosestLocation(player));
            if (_room.owner.equals(player)) {
                writeD(1);
            } else {
                if (_room.owner.getParty() != null && _room.owner.getParty().getPartyMembers().contains(player)) {
                    writeD(2);
                } else {
                    writeD(0);
                }
            }
        }
        /*writeD(isLeader ? 0x01 : 0x00);
         writeD(members_list.size());
         for(PartyRoomMemberInfo member_info : members_list)
         {
         writeD(member_info.objectId);
         writeS(member_info.name);
         writeD(member_info.classId);
         writeD(member_info.level);
         writeD(member_info.location);
         writeD(member_info.memberType);//1-leader     2-party member    0-not party member
         }*/
    }
    /*
     static class PartyRoomMemberInfo
     {
     public final int objectId, classId, level, location, memberType;
     public final String name;

     public PartyRoomMemberInfo(L2Player member, L2Player leader)
     {
     objectId = member.getObjectId();
     name = member.getName();
     classId = member.getClassId().ordinal();
     level = member.getLevel();
     location = PartyRoomManager.getInstance().getLocation(member);
     memberType = member.equals(leader) ? 0x01 : member.getParty() != null && leader.getParty() == member.getParty() ? 0x02 : 0x00;
     }
     }*/
}
