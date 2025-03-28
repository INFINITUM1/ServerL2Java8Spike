package net.sf.l2j.gameserver.network.serverpackets;

public class TradePressOwnOk extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x75);
	}

	@Override
	public String getType()
	{
		return "S.TradePressOwnOk";
	}
}
