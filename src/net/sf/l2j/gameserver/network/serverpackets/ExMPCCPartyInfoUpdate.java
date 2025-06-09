package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2Party;

public class ExMPCCPartyInfoUpdate extends L2GameServerPacket
{
	private final String leader_name;
	private final int leader_objId, members_count, _mode;

	/**
	 * @param party
	 * @param mode 0 = Remove, 1 = Add
	 */
	public ExMPCCPartyInfoUpdate(L2Party party, int mode)
	{
		leader_name = party.getLeader().getName();
		leader_objId = party.getLeader().getObjectId();
		members_count = party.getMemberCount();
		_mode = mode;
	}

	@Override
	protected void writeImpl()
	{
		/*writeC(0xFE);
		writeH(0x54);
		writeS(leader_name);
		writeD(leader_objId);
		writeD(members_count);
		writeD(_mode);*/
	}
}