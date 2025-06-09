package scripts.ai;

import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class SantaTrainee extends L2NpcInstance {

    private static String htmPath = "data/html/events/";

    public SantaTrainee(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance talker, String command) {
        if (command.startsWith("santa")) {
            int reply = Integer.parseInt(command.substring(5).trim());
            switch (reply) {
                case 1:
                    showPage(talker, "event_wannabe_santa1_q0998_01.htm");
                    break;
                case 2:
                    if (getItemCount(talker, 57) >= 1000) {
                        showPage(talker, "event_wannabe_santa1_q0998_01a.htm");
                        giveItem(talker, 5555, 1);
                        deleteItem(talker, 57, 1000);
                    } else {
                        showPage(talker, "event_wannabe_santa1_q0998_01b.htm");
                    }
                    break;
                case 3:
                    showPage(talker, "event_wannabe_santa1_q0998_02.htm");
                    break;
                case 4:
                    if (getItemCount(talker, 5556) >= 4 && getItemCount(talker, 5557) >= 4 && getItemCount(talker, 5558) >= 10 && getItemCount(talker, 5559) >= 1) {
                        showPage(talker, "event_wannabe_santa1_q0998_03.htm");
                        deleteItem(talker, 5556, 4);
                        deleteItem(talker, 5557, 4);
                        deleteItem(talker, 5558, 10);
                        deleteItem(talker, 5559, 1);
                        giveItem(talker, 5560, 1);
                    } else {
                        showPage(talker, "event_wannabe_santa1_q0998_03a.htm");
                    }
                    break;
                case 5:
                    if (getItemCount(talker, 5560) >= 10) {
                        deleteItem(talker, 5560, 10);
                        giveItem(talker, 5561, 1);
                        showPage(talker, "event_wannabe_santa1_q0998_04.htm");
                    } else {
                        showPage(talker, "event_wannabe_santa1_q0998_04a.htm");
                    }
                    break;
                case 6:
                    showPage(talker, "event_wannabe_santa1_q0998_05.htm");
                    break;
                case 11:
                    addBuff(talker, 4342, 2);
                    showPage(talker, "event_wannabe_santa1_q0998_06.htm");
                    break;
                case 12:
                    addBuff(talker, 1086, 2);
                    showPage(talker, "event_wannabe_santa1_q0998_07.htm");
                    break;
                case 13:
                    addBuff(talker, 1059, 3);
                    showPage(talker, "event_wannabe_santa1_q0998_08.htm");
                    break;
                case 14:
                    addBuff(talker, 1068, 3);
                    showPage(talker, "event_wannabe_santa1_q0998_09.htm");
                    break;
                case 15:
                    addBuff(talker, 1040, 3);
                    showPage(talker, "event_wannabe_santa1_q0998_10.htm");
                    break;
                case 21:
                    showPage(talker, "event_wannabe_santa1_q0998_11.htm");
                    break;
                case 22:
                    if (getItemCount(talker, 5560) >= 10) {
                        deleteItem(talker, 5560, 10);
                        giveItem(talker, 7836, 1);
                        showPage(talker, "event_wannabe_santa1_q0998_12.htm");
                    } else {
                        showPage(talker, "event_wannabe_santa1_q0998_13.htm");
                    }
                    break;
            }
        } else {
            super.onBypassFeedback(talker, command);
        }
        talker.sendActionFailed();
    }

    private void showPage(L2PcInstance talker, String page) {
        showChatWindow(talker, htmPath + page);
    }
}
