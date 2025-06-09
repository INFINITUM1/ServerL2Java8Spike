package net.sf.l2j.gameserver.network.serverpackets;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.l2j.gameserver.instancemanager.PartyWaitingRoomManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Format:(ch) d [sdd]
 */
public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	private final ConcurrentLinkedQueue<L2PcInstance> _finders;
	private int _size;

	public ExListPartyMatchingWaitingRoom(L2PcInstance player, int page, int minLvl, int maxLvl)
	{
		/*int first = (page - 1) * 64;
		int firstNot = page * 64;
		int i = 0;

		ArrayList<L2Player> temp = PartyRoomManager.getInstance().getWaitingList(minLevel, maxLevel);
		_fullSize = temp.size();

		waiting_list = new GArray<PartyMatchingWaitingInfo>(_fullSize);
		for(L2Player pc : temp)
		{
			if(i < first || i >= firstNot)
				continue;
			waiting_list.add(new PartyMatchingWaitingInfo(pc));
			i++;
		}*/
		_finders = PartyWaitingRoomManager.getInstance().getFinders(page, minLvl, maxLvl, new ConcurrentLinkedQueue<L2PcInstance>());
		_size = _finders.size();
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x35);

		writeD(_size);
		if(_size == 0)
		{
			writeD(0);
			return;
		}
		writeD(_size);
		
		for(L2PcInstance player : _finders)
		{
			if (player == null)
				continue;
			
			writeS(player.getName());
			writeD(player.getActiveClass());
			writeD(player.getLevel());
		}
	}
}