package net.sf.l2j.gameserver.util.protection;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.L2GameClient;

public class GameGuard
{
	public GameGuard()
	{
	}

	private static GameGuard _instance = null;
	public static void init()
	{
		if (Config.GAMEGUARD_ENABLED)
			_instance = GameGuardMain.load();
		else
			_instance = new GameGuard();
	}

	public static GameGuard getInstance()
	{
		return _instance;
	}

	public void startSession(L2GameClient client)
	{
	}

	public void closeSession(L2GameClient client)
	{
	}

	public boolean checkGameGuardReply(L2GameClient client, int [] reply)
	{
		return true;
	}

	public void startCheckTask()
	{
	}
}
