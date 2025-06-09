package scripts.commands.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.commands.IVoicedCommandHandler;

public class ModBanChat implements IVoicedCommandHandler {

    private static final String[] VOICED_COMMANDS = {"banchat", "unbanchat"};

    @Override
    public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target) {
        if (activeChar.isModerator()) {
            String name = "";
            String[] cmdParams = command.split(" ");
            try {
                name = cmdParams[1];
            } catch (Exception e) {
                name = "_npe";
            }

            if (name.equalsIgnoreCase("_npe")) {
                activeChar.sendModerResultMessage("Ошибка при наборе ника");
                return false;
            }

            L2PcInstance targetPlayer = L2World.getInstance().getPlayer(name);
            if (targetPlayer == null) {
                activeChar.sendModerResultMessage("Игрок не найден или ошибка при наборе ника");
                return false;
            }

            //int obj = activeChar.getObjectId();
            String Moder = activeChar.getForumName();

            if (command.startsWith("banchat")) {
                long banLengthMins = Integer.parseInt(cmdParams[2]);

                if (targetPlayer.isChatBanned()) {
                    activeChar.sendModerResultMessage("Чат " + targetPlayer.getName() + " уже заблокирован кем-то другим");
                    return false;
                }

                if (targetPlayer.isModerator() && activeChar.getModerRank() > 1) {
                    return false;
                }

                if (banLengthMins > Config.MAX_BAN_CHAT) {
                    banLengthMins = Config.MAX_BAN_CHAT;
                }

                long banLength = banLengthMins * 60;

                activeChar.sendModerResultMessage("Чат " + targetPlayer.getName() + " заблокирован на " + banLength + " секунд. (" + banLengthMins + " минут)");
                targetPlayer.sendMessage("* * * * * * * * * * * * * *");
                targetPlayer.setChatBanned(true, banLength, "");
                targetPlayer.sendMessage("Наказал " + activeChar.getName() + "(" + Moder + ")");
                targetPlayer.sendMessage("Для оспаривания, растените чат и сделайте скрин,");
                targetPlayer.sendMessage("раздел на форуме - Арбитраж.");
                targetPlayer.sendMessage("* * * * * * * * * * * * * *");
                //
                activeChar.logModerAction(Moder, "Бан чата " + targetPlayer.getName() + " на " + banLengthMins + " минут");
            } else if (command.startsWith("unbanchat")) {
                activeChar.sendModerResultMessage("Чат " + targetPlayer.getName() + " разблокирован");
                targetPlayer.sendMessage("* * * * * * * * * * * * * *");
                targetPlayer.setChatBanned(false, 0, "");
                targetPlayer.sendMessage("Амнистировал " + activeChar.getName() + "(" + Moder + ")");
                targetPlayer.sendMessage("* * * * * * * * * * * * * *");
                //
                activeChar.logModerAction(Moder, "Снял бан чата " + targetPlayer.getName());
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String[] getVoicedCommandList() {
        return VOICED_COMMANDS;
    }
}