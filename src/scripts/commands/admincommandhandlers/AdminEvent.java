
package scripts.commands.admincommandhandlers;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import scripts.autoevents.masspvp.massPvp;
import scripts.autoevents.schuttgart.Schuttgart;
import scripts.commands.IAdminCommandHandler;

/**
 * This class handles following admin commands:
 * - gmshop = shows menu
 * - buy id = shows shop with respective id
 * @version $Revision: 1.2.4.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminEvent implements IAdminCommandHandler {
	private static Logger _log = Logger.getLogger(AdminEvent.class.getName());

	private static final String[] ADMIN_COMMANDS = {"admin_masspvp", "admin_schuttgart"};

	private static final int REQUIRED_LEVEL = Config.GM_CREATE_ITEM;

	public boolean useAdminCommand(String command, L2PcInstance pc)
	{
		if (!Config.ALT_PRIVILEGES_ADMIN)
			if (!(checkLevel(pc.getAccessLevel()) && pc.isGM()))
				return false;

		if (command.startsWith("admin_masspvp"))
		{			
			if (!Config.MASS_PVP)
			{
				pc.sendHtmlMessage("Эвент отключен.");
				return true;
			}
			int action = Integer.valueOf(command.substring(14));
			
			if (action == 1)
				massPvp.getEvent().startScript();
			else
				massPvp.getEvent().stopScript(pc);
		}
		else if (command.startsWith("admin_schuttgart"))
		{	
			if (!Config.ALLOW_SCH)
			{
				pc.sendHtmlMessage("Эвент отключен.");
				return true;
			}
			int action = Integer.valueOf(command.substring(17));
			
			if (action == 1)
				Schuttgart.getEvent().startScript(pc);
			else
				Schuttgart.getEvent().stopScript(pc);
		}
		else if (command.startsWith("admin_tvt"))
		{	
			if (!Config.TVT_EVENT_ENABLED)
			{
				pc.sendHtmlMessage("Эвент отключен.");
				return true;
			}
			int action = Integer.valueOf(command.substring(10));

			if (action == 1)
				TvTEvent.startParticipation();
			else
				Schuttgart.getEvent().stopScript(pc);
		}
		return true;
	}

	public String[] getAdminCommandList() 
	{
		return ADMIN_COMMANDS;
	}

	private boolean checkLevel(int level) 
	{
		return (level >= REQUIRED_LEVEL);
	}
}
