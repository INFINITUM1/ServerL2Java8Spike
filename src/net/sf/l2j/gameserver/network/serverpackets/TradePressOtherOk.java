package net.sf.l2j.gameserver.network.serverpackets;

public class TradePressOtherOk extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x7C);
	}

	@Override
	public String getType()
	{
		return "S.TradePressOtherOk";
	}
}