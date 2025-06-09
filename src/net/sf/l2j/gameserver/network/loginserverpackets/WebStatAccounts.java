package net.sf.l2j.gameserver.network.loginserverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.util.WebStat;

public class WebStatAccounts extends LoginServerBasePacket
{
	private int _count;
	public WebStatAccounts(byte[] decrypt)
	{
		super(decrypt);
		_count = readD();
	}

	public int getCount()
	{
		return _count;
	}
}