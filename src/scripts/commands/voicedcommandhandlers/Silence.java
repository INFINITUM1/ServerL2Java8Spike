package scripts.commands.voicedcommandhandlers;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.commands.IVoicedCommandHandler;

public class Silence implements IVoicedCommandHandler {

    private static final String[] VOICED_COMMANDS = {"silence", "unpartner"};

    @Override
    public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target) {
        if (command.equalsIgnoreCase("silence")) {
            if (activeChar.isWorldIgnore()) {
                activeChar.setWorldIgnore(false);
                activeChar.sendMessage("Игнор мирового чата выключен");
            } else {
                activeChar.setWorldIgnore(true);
                activeChar.sendMessage("Игнор мирового чата включен");
            }
        } else if (command.equalsIgnoreCase("unpartner")) {
            L2PcInstance _partner = activeChar.getPartner();
            if (_partner != null) {
                try {
                    _partner.despawnMe();
                } catch (Exception t) {
                }// returns pet to control item
            }
        } 
        return true;
    }

    @Override
    public String[] getVoicedCommandList() {
        return VOICED_COMMANDS;
    }
}