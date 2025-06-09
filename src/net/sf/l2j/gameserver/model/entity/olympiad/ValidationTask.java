package net.sf.l2j.gameserver.model.entity.olympiad;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.model.entity.Hero;

public class ValidationTask implements Runnable {

    @Override
    public void run() {
        OlympiadDatabase.sortHerosToBe();
        OlympiadDatabase.saveNobleData();
        if (Hero.getInstance().computeNewHeroes(Olympiad._heroesToBe)) {
            Olympiad._log.warning("Olympiad: Error while computing new heroes!");
        }
        Announcements.getInstance().announceToAll("Olympiad Validation Period has ended");
        Olympiad._period = 0;
        Olympiad._currentCycle++;
        OlympiadDatabase.cleanupNobles();
        OlympiadDatabase.loadNoblesRank();
        OlympiadDatabase.setNewOlympiadEnd();
        Olympiad.init();
        OlympiadDatabase.save();
    }
}