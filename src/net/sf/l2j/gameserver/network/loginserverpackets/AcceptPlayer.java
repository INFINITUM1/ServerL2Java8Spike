package net.sf.l2j.gameserver.network.loginserverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.util.WebStat;

public class AcceptPlayer extends LoginServerBasePacket
{
	private String _ip;
	public AcceptPlayer(byte[] decrypt)
	{
		super(decrypt);
		_ip = readS();

		if (Config.WEBSTAT_ENABLE)
			WebStat.getInstance().addLogin(_ip);
	}

	public String getIp()
	{
		return _ip;
	}
}