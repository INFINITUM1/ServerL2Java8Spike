package scripts.commands.voicedcommandhandlers;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.commands.IVoicedCommandHandler;

public class ModKick implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "kick"};

	/* (non-Javadoc)
	* @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#useVoicedCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	*/
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{ 
		if (activeChar.isModerator())
		{
			String Moder = activeChar.getForumName();
			
			if (command.equalsIgnoreCase("kick"))
			{  
				L2Object mtarget = activeChar.getTarget();
				if (mtarget != null)
				{
					L2PcInstance targetPlayer = null;
					if (mtarget.isPlayer())
						targetPlayer = (L2PcInstance)mtarget;
					else
					{
						activeChar.sendModerResultMessage("Только игроков х_Х");
						return false;
					}	
					
					if (targetPlayer.getPrivateStoreType() != 0)
					{
						activeChar.sendModerResultMessage(targetPlayer.getName() + " кикнут из игры");
						targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
						targetPlayer.sendMessage("Вас кикнул "+activeChar.getName()+"("+Moder+")");
						targetPlayer.sendMessage("* * * * * * * * * * * * * *"); 
						targetPlayer.kick();
						//
						activeChar.logModerAction(Moder, "Кикнул "+targetPlayer.getName());
					}
				}
			}
		}
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}