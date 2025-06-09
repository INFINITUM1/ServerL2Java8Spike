package scripts.commands.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadDatabase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import scripts.commands.IAdminCommandHandler;

public class AdminHero implements IAdminCommandHandler {

    private static String[] _adminCommands = {"admin_sethero", "admin_manualhero"};
    private final static Log _log = LogFactory.getLog(AdminHero.class.getName());
    private static final int REQUIRED_LEVEL = Config.GM_MENU;

    public boolean useAdminCommand(String command, L2PcInstance admin) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!(checkLevel(admin.getAccessLevel()) && admin.isGM())) {
                return false;
            }
        }
        if (command.startsWith("admin_sethero")) {
            L2Object obj = admin.getTarget();
            if (!obj.isPlayer()) {
                return false;
            }

            L2PcInstance target = (L2PcInstance) obj;
            if (target.isHero()) {
                target.setHero(false);
                target.setHeroExpire(0);
                target.broadcastUserInfo();
                admin.sendAdmResultMessage("Забрали стутас героя у игрока " + target.getName());
                /*
                 * Connect con = null; PreparedStatement st = null; try { con =
                 * L2DatabaseFactory.getConnection(); st =
                 * con.prepareStatement("UPDATE characters SET hero=0 WHERE
                 * obj_id=?"); st.setInt(1, target.getObjectId()); st.execute();
                 * } catch (SQLException e) { _log.warn("could not unset Hero
                 * stats of char:", e); } finally { Close.CS(con, st); }
                 */
            } else {
                int days = 30;
                try {
                    days = Integer.parseInt(command.substring(14).trim());
                } catch (Exception e) {
                    days = 30;
                }
                admin.sendAdmResultMessage("Выдан стутас героя игроку " + target.getName() + " на " + days + " дней.");
                target.setHero(days);
            }
        } else if (command.startsWith("admin_manualhero")) {
            admin.sendAdmResultMessage("Активирована выдача геройства... ждите!");
            OlympiadDatabase.sortHerosToBe(true);
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
        return false;
    }

    public String[] getAdminCommandList() {
        return _adminCommands;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }
}