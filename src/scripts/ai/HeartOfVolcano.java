package scripts.ai;

import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.bosses.ValakasManager;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class HeartOfVolcano extends L2NpcInstance {

    private static String htmPath = "data/html/teleporter/";

    public HeartOfVolcano(int objectId, L2NpcTemplate template) {
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

        ValakasManager vm = ValakasManager.getInstance();
        switch (vm.getStatus()) {
            case 1:
                showChatWindow(player, htmPath + getNpcId() + "-5.htm");
                break;
            case 2:
                showChatWindow(player, htmPath + getNpcId() + "-3.htm");
                break;
            case 5:
                if (GrandBossManager.getInstance().getItem(player, 7267)) {
                    if (GrandBossManager.getInstance().getZone(213004, -114890, -1635).getPlayersCount() > 200) {
                        showChatWindow(player, htmPath + getNpcId() + "-6.htm");
                        break;
                    }
                    GrandBossManager.getInstance().getZone(213004, -114890, -1635).allowPlayerEntry(player, 9000000);
                    player.teleToLocation((204527 + Rnd.get(200)), (-112026 + Rnd.get(200)), 61);
                    vm.notifyEnter();
                } else {
                    showChatWindow(player, htmPath + getNpcId() + "-4.htm");
                }
                break;
            default:
                showChatWindow(player, htmPath + getNpcId() + "-2.htm");
                break;
        }
        player.sendActionFailed();
    }
}
