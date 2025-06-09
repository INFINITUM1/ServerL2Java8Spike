package scripts.commands.voicedcommandhandlers;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.commands.IVoicedCommandHandler;

public class ModCommands implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "banchat", "unbanchat", "kick", "recall"};

	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (activeChar.isModerator())
		{
			String[] cmdParams = command.split(" ");
               
			String name = cmdParams[1];
			long banLength = Integer.parseInt(cmdParams[2]);
			
			L2PcInstance targetPlayer = L2World.getInstance().getPlayer(name);
			String Moder = "";
			int obj = activeChar.getObjectId();
			
			switch (obj)
			{
				case 268948838:
					Moder = "EvilsToy";
					break;
				case 268551556:
					Moder = "MYPKA";
					break;
				case 269177626:
					Moder = "I_am_Legend";
					break;
				case 271218061:
					Moder = "d00m";
					break;
				case 270231826:
					Moder = "Zeteo";
					break;
				case 270579677:
					Moder = "ZIKaaaR";
					break;
				case 271162644:
					Moder = "(-=[Sa(.i.)nt]=-)";
					break;
				case 269833471:
					Moder = "Kpayc";
					break;
			}
			
			if (targetPlayer == null)
			{
				activeChar.sendMessage("Игрок не найден или ошибка при наборе ника");
				return false;
			}
			
			if (command.startsWith("banchat"))
			{
				if (targetPlayer.isChatBanned())
				{
					activeChar.sendMessage("Чат "+targetPlayer.getName() + " уже заблокирован кем-то другим");
					return false;
				}
				
				if (targetPlayer.isModerator())
				{
					activeChar.sendMessage("Нельзя");
					return false;
				}
			
				long banLengthMins = banLength / 60;
				
				if (banLengthMins > 1800)
					banLengthMins = 1800;
				
				activeChar.sendMessage("Чат "+targetPlayer.getName() + " заблокирован на " + banLength + " секунд. ("+banLengthMins+" минут)");
				targetPlayer.setChatBanned(true, banLength, "");
				targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
				targetPlayer.sendMessage("Наказал "+activeChar.getName()+"("+Moder+")");
				targetPlayer.sendMessage("Для оспаривания, растените чат и сделайте скрин,");
				targetPlayer.sendMessage("раздел на форуме - Арбитраж.");
				targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
			}
			else if (command.startsWith("unbanchat"))
			{
				targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
				activeChar.sendMessage("Чат "+targetPlayer.getName() + "разблокирован");
				targetPlayer.sendMessage("Амнистировал "+activeChar.getName()+"("+Moder+")");
				targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
				targetPlayer.setChatBanned(false, 0, "");
			}
			else if (command.startsWith("kick"))
			{
				if (activeChar.getPrivateStoreType() != 0)
				{
					activeChar.sendMessage(targetPlayer.getName() + " кикнут из игры");
					targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
					targetPlayer.sendMessage("Вас кикнул "+activeChar.getName()+"("+Moder+")");
					targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
					targetPlayer.logout();
				}
			}
			else if (command.startsWith("recall"))
			{
				activeChar.sendMessage("Вы телепортируете "+targetPlayer.getName());
				targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
				targetPlayer.sendMessage("Вас телепортирует "+activeChar.getName()+"("+Moder+")");
				targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
				targetPlayer.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				targetPlayer.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false);
			}
			return true;
		}
		else
			return false;
	}

	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}