package net.sf.l2j.gameserver.model.actor.instance;

import java.util.logging.Logger;
import javolution.util.FastList;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.entity.olympiad.CompType;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadDatabase;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadGame;
import net.sf.l2j.gameserver.model.entity.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * Olympiad Npc's Instance
 */
public class L2OlympiadManagerInstance extends L2NpcInstance {

    private static Logger _log = AbstractLogger.getLogger(L2OlympiadManagerInstance.class.getName());
    private static final short noblesseGatePass = 6651;

    public L2OlympiadManagerInstance(final int objectId, final L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(final L2PcInstance player, final String command) {
        if (command.startsWith("OlympiadDesc")) {
            final int val = Integer.parseInt(command.substring(13, 14));
            final String suffix = command.substring(14);
            showChatWindow(player, val, suffix);
        } else if (command.startsWith("OlympiadNoble")) {
            final int classId = player.getClassId().getId();
            if (!player.isNoble() || classId < 88 || classId > 118 && classId < 131 || classId > 134) {
                player.sendActionFailed();
                return;
            }

            if (player.isInOlympiadMode() || player.getOlympiadGameId() > -1) {
                player.sendActionFailed();
                return;
            }

            final int val = Integer.parseInt(command.substring(14));
            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            StringBuffer replyMSG;

            switch (val) {
                case 1:
                    Olympiad.unRegisterNoble(player);
                    break;
                case 2:
                    int classed = 0;
                    int nonClassed = 0;
                    int[] array = Olympiad.getWaitingList();

                    if (array != null) {
                        classed = array[0];
                        nonClassed = array[1];
                    }

                    reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
                    reply.replace("%listClassed%", String.valueOf(classed));
                    reply.replace("%listNonClassed%", String.valueOf(nonClassed));
                    reply.replace("%objectId%", String.valueOf(getObjectId()));

                    player.sendPacket(reply);
                    break;
                case 3:
                    int points = Olympiad.getNoblePoints(player.getObjectId());
                    reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
                    reply.replace("%points%", String.valueOf(points));
                    reply.replace("%objectId%", String.valueOf(getObjectId()));
                    player.sendPacket(reply);
                    break;
                case 4:
                    Olympiad.registerNoble(player, CompType.NON_CLASSED);
                    break;
                case 5:
                    Olympiad.registerNoble(player, CompType.CLASSED);
                    break;
                case 6:
                    final int passes = Olympiad.getNoblessePasses(player);
                    if (passes > 0) {
                        final L2ItemInstance item = player.getInventory().addItem("Olympiad", noblesseGatePass, passes, player, null);
                        player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addNumber(passes).addItemName(item.getItemId()));
                    } else {
                        reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_nopoints.htm");
                        reply.replace("%objectId%", String.valueOf(getObjectId()));
                        player.sendPacket(reply);
                    }
                    break;
                case 7:
                    L2Multisell.getInstance().SeparateAndSend(102, player, false, getCastle().getTaxRate());
                    break;
                case 8:
                    int point = Olympiad.getNoblePointsPast(player.getObjectId());
                    reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_points2.htm");
                    reply.replace("%points%", String.valueOf(point));
                    reply.replace("%objectId%", String.valueOf(getObjectId()));
                    player.sendPacket(reply);
                    break;
                case 9:
                    L2Multisell.getInstance().SeparateAndSend(103, player, false, getCastle().getTaxRate());
                    break;
                default:
                    _log.warning("Olympiad System: Couldnt send packet for request " + val);
                    break;
            }
        } else if (command.startsWith("Olympiad")) {
            if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode() || player.getOlympiadGameId() > -1) {
                player.sendActionFailed();
                return;
            }

            final int val = Integer.parseInt(command.substring(9, 10));

            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            StringBuffer replyMSG;

            switch (val) {
                case 1:
                    StringBuilder replace = new StringBuilder("");
                    OlympiadManager manager = Olympiad._manager;
                    if (manager != null) {
                        for (int i = 0; i < Olympiad.STADIUMS.length; i++) {
                            OlympiadGame game = manager.getOlympiadInstance(i);
                            if (game != null && game.getState() > 0) {
                                replace.append("<br1>Arena " + (i + 1) + ":&nbsp;<a action=\"bypass -h npc_" + getObjectId() + "_Olympiad 3_" + i + "\">" + manager.getOlympiadInstance(i).getTitle() + "</a>");
                                replace.append("<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>");
                            }
                        }
                    }

                    reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_observe.htm");
                    reply.replace("%arenas%", replace.toString());
                    reply.replace("%objectId%", String.valueOf(getObjectId()));
                    player.sendPacket(reply);
                    break;
                case 2:
                    // for example >> Olympiad 2_88
                    int classId = Integer.parseInt(command.substring(11));
                    if (classId >= 88) {
                        reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_ranking.htm");

                        FastList<String> names = OlympiadDatabase.getClassLeaderBoard(classId);

                        int index = 1;
                        for (String name : names) {
                            if (name == null) {
                                continue;
                            }
                            reply.replace("%place" + index + "%", String.valueOf(index));
                            reply.replace("%rank" + index + "%", Util.htmlSpecialChars(name));
                            index++;
                            if (index > 10) {
                                break;
                            }
                        }
                        for (; index <= 10; index++) {
                            reply.replace("%place" + index + "%", "");
                            reply.replace("%rank" + index + "%", "");
                        }
                        reply.replace("%objectId%", String.valueOf(getObjectId()));
                        player.sendPacket(reply);
                    }
                    // TODO Send player each class rank
                    break;
                case 3:
                    if (!player.canSeeBroadcast()) {
                        player.sendActionFailed();
                        return;
                    }

                    Olympiad.addSpectator(Integer.parseInt(command.substring(11)), player);
                    break;
                default:
                    _log.warning("Olympiad System: Couldnt send packet for request " + val);
                    break;
            }
        } else {
            super.onBypassFeedback(player, command);
        }
    }

    private void showChatWindow(final L2PcInstance player, final int val, final String suffix) {
        String filename = Olympiad.OLYMPIAD_HTML_PATH;
        filename += "noble_desc" + val;
        filename += suffix != null ? suffix + ".htm" : ".htm";
        if (filename.equals(Olympiad.OLYMPIAD_HTML_PATH + "noble_desc0.htm")) {
            filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
        }

        showChatWindow(player, filename);
    }
}
