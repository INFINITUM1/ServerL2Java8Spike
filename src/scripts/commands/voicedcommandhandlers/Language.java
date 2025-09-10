
package scripts.commands.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.commands.IVoicedCommandHandler;

public class Language implements IVoicedCommandHandler
{
    private static final String[] VOICED_COMMANDS = { "lang" };

   @Override
public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
{
    if (!Config.MULTILANG_ENABLE)
    {
        activeChar.sendMessage("Switch Language is not allowed");
        return false;
    }

    // Если target пустой – пробуем достать аргумент из command
    if ((target == null || target.isEmpty()) && command.contains(" "))
    {
        target = command.substring(command.indexOf(" ") + 1).trim();
    }

    if (target == null || target.isEmpty())
    {
        activeChar.sendMessage("Allowed Languages " + Config.MULTILANG_ALLOWED);
        return false;
    }

    if (activeChar.setLang(target))
    {
        activeChar.sendMessage("Language: " + target);
        activeChar.storeLangToDB();
    }
    else
    {
        activeChar.sendMessage("Wrong Language. Choose: " + Config.MULTILANG_ALLOWED);
    }
    return true;
}

    @Override
    public String[] getVoicedCommandList()
    {
        return VOICED_COMMANDS;
    }
}