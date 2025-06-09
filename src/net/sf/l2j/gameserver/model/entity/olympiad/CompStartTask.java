package net.sf.l2j.gameserver.model.entity.olympiad;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.cache.Static;

class CompStartTask implements Runnable
{
	@Override
	public void run()
	{
		if(Olympiad.isOlympiadEnd())
			return;

		Olympiad._manager = new OlympiadManager();
		Olympiad._inCompPeriod = true;

		new Thread(Olympiad._manager).start();

		ThreadPoolManager.getInstance().scheduleGeneral(new CompEndTask(), Olympiad.getMillisToCompEnd());

		Announcements.getInstance().announceToAll(Static.THE_OLYMPIAD_GAME_HAS_STARTED);
		Olympiad._log.info("Olympiad System: Olympiad Game Started");
	}
}