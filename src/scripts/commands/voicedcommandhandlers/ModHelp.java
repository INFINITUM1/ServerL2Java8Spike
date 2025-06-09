package scripts.commands.voicedcommandhandlers;

import javolution.text.TextBuilder;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import scripts.commands.IVoicedCommandHandler;

public class ModHelp implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "moderhelp"};

	/* (non-Javadoc)
	* @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#useVoicedCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	*/
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{ 
		if (activeChar.isModerator())
		{
			String Moder = activeChar.getForumName();
			int rank = activeChar.getModerRank();
			
			if (command.equalsIgnoreCase("moderhelp"))
			{  
				NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
				TextBuilder build = new TextBuilder("<html><body>");
				build.append("<center><font color=\"LEVEL\">Помощь</font></center><br><br>");
				build.append("Модератор <font color=\"LEVEL\">" + Moder + "</font>; rank " + rank + "<br><br>");
				build.append("Cписок команд:<br>");
				build.append("<font color=66CC00>.синтаксис</font> (как работает); описание<br>");
				build.append("<font color=66CC00>.banchat</font> (.banchat Hope 120); Бан чата в минутах<br>");
				build.append("<font color=66CC00>.unbanchat</font> (.unbanchat Hope); Снять бан чата<br>");
				build.append("<font color=66CC00>.kick</font> (взять чара в таргет); Кикнуть торговца<br>");
				build.append("<font color=66CC00>.cleartitle</font> (взять чара в таргет); Удалить титул<br>");
				switch (rank)
				{
					case 2:
						build.append("<font color=CC9900>.showstat</font> (.showstat Nick); Статы чара<br>");
						//build.append("<font color=66CC00>.showinv<font> (.showinv Nick); Шмотки чара<br>");
						break;
				}
				build.append("<br><br>");
				build.append("</body></html>");
				nhm.setHtml(build.toString());
				activeChar.sendPacket(nhm);
			}
		}
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}