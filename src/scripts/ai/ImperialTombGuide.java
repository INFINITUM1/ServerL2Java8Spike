package scripts.ai;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.bosses.FrintezzaManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class ImperialTombGuide extends L2NpcInstance {

    private static String htmPath = "data/html/teleporter/";

    public ImperialTombGuide(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        FrintezzaManager fm = FrintezzaManager.getInstance();
        switch (fm.getStatus()) {
            case 1:
                if (canEnter(player, player.getParty(), fm)) {
                    fm.doEvent(1);
                    //player.getParty().teleToEpic(173240, -76950, -5104, 174239, -89805, -5020);
                    for (L2Party rparty : player.getParty().getCommandChannel().getPartys()) {
                        if (rparty == null) {
                            continue;
                        }

                        if (fm.getPartyCount() == 1 || fm.getPartyCount() == 3) {
                            rparty.teleToEpic(173240, -76950, -5104, 174239, -89805, -5020);
                        } else {
                            rparty.teleToEpic(174108, -76197, -5104, 174239, -89805, -5020);
                        }

                        fm.addParty(rparty);
                    }
                }
                break;
            case 2:
                showChatWindow(player, htmPath + getNpcId() + "-08.htm");
                //return "<html><head><body>[Whispering]<br><font color=\"LEVEL\">Busy, please wait.</font></body></html>";
                break;
            default:
                showChatWindow(player, htmPath + getNpcId() + "-03.htm");
                //return "<html><head><body>[Whispering]<br><font color=\"LEVEL\">Now you can not fight with him. Go away!</font></body></html>";
                break;
        }
    }

    private boolean canEnter(L2PcInstance player, L2Party party, FrintezzaManager fm) {
        if (fm.getPartyCount() >= 5) {
            showChatWindow(player, htmPath + getNpcId() + "-04.htm");
            //return "<html><head><body>[Whispering]<br><font color=\"LEVEL\">Already included 5 groups, can no longer be.</font></body></html>";
            return false;
        }

        if (party == null || party.getMemberCount() < Config.FRINTA_MMIN_PLAYERS) {
            showChatWindow(player, htmPath + getNpcId() + "-02.htm");
            //return "<html><head><body>[Whispering]<br><font color=\"LEVEL\">Only complete group can sign.</font></body></html>";
            return false;
        }

        if (!party.isInCommandChannel() || !party.getCommandChannel().getChannelLeader().equals(player)) {
            showChatWindow(player, htmPath + getNpcId() + "-07.htm");
            //return "<html><head><body>[Whispering]<br><font color=\"LEVEL\">Only Command Chanell leader can sign.</font></body></html>";
            return false;
        }

        if (party.getCommandChannel().getPartys().size() < Config.FRINTA_MMIN_PARTIES) {
            showChatWindow(player, htmPath + getNpcId() + "-06.htm");
            //return "<html><head><body>[Whispering]<br><font color=\"LEVEL\">Minimum 2 partys in Command Chanell can sign.</font></body></html>";
            return false;
        }

        if (!haveTicket(player)) {
            showChatWindow(player, htmPath + getNpcId() + "-05.htm");
            //return "<html><head><body>[Whispering]<br><font color=\"LEVEL\">Your leader of the group must Force Field Removal Scroll to sign.</font></body></html>";
            return false;
        }
        return true;
    }

    private boolean haveTicket(L2PcInstance talker) {
        L2ItemInstance coin = talker.getInventory().getItemByItemId(8073);
        if (coin == null || coin.getCount() < 1) {
            return false;
        }

        if (!talker.destroyItemByItemId("RaidBossTele", 8073, 1, talker, true)) {
            return false;
        }

        return true;
    }
}
