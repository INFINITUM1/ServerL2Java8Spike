package scripts.ai;

import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.bosses.AntharasManager;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class HeartOfWarding extends L2NpcInstance {

    private static String htmPath = "data/html/teleporter/";

    public HeartOfWarding(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.startsWith("Chat")) {
            showChatWindow(player, htmPath + getNpcId() + "-" + Integer.parseInt(command.substring(4).trim()) + ".htm");
            return;
        }

        AntharasManager am = AntharasManager.getInstance();
        switch (am.getStatus()) {
            case 1:
                if (GrandBossManager.getInstance().getItem(player, 3865)) {
                    GrandBossManager.getInstance().getZone(179700, 113800, -7676).allowPlayerEntry(player, 9000000);
                    player.teleToLocation((179700 + Rnd.get(700)), (113800 + Rnd.get(2100)), -7709);
                    am.notifyEnter();
                } else {
                    showChatWindow(player, htmPath + getNpcId() + "-4.htm");
                }
                break;
            case 2:
                showChatWindow(player, htmPath + getNpcId() + "-3.htm");
                break;
            default:
                showChatWindow(player, htmPath + getNpcId() + "-2.htm");
                break;
        }
        player.sendActionFailed();
    }
}
