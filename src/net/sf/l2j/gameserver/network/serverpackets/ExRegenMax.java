package net.sf.l2j.gameserver.network.serverpackets;

public class ExRegenMax extends L2GameServerPacket
{
	private double _max;
	private int _count;
	private int _time;

	public ExRegenMax(double max, int count, int time)
	{
		_max = max;
		_count = count;
		_time = time;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x01);
		writeD(1);
		writeD(_count);
		writeD(_time);
		writeF(_max);
	}
}