package net.sf.l2j.gameserver.model.entity.olympiad;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.cache.Static;

class CompEndTask implements Runnable {

    @Override
    public void run() {
        if (Olympiad.isOlympiadEnd()) {
            return;
        }

        Olympiad._inCompPeriod = false;

        try {
            OlympiadManager manager = Olympiad._manager;

            // Если остались игры, ждем их завершения еще одну минуту 
            if (manager != null && !manager.getOlympiadGames().isEmpty()) {
                ThreadPoolManager.getInstance().scheduleGeneral(new CompEndTask(), 60000);
                return;
            }

            Announcements.getInstance().announceToAll(Static.THE_OLYMPIAD_GAME_HAS_ENDED);
            Olympiad._log.info("Olympiad System: Olympiad Game Ended");
            OlympiadDatabase.save();
        } catch (Exception e) {
            Olympiad._log.warning("Olympiad System: Failed to save Olympiad configuration:");
            e.printStackTrace();
        }
        Olympiad.init();
    }
}
