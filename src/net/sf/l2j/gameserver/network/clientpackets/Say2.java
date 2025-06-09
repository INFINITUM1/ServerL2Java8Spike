/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;
import java.util.StringTokenizer;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.TimeLogger;
import scripts.commands.IVoicedCommandHandler;
import scripts.commands.VoicedCommandHandler;

/**
 * This class ...
 *
 * @version $Revision: 1.16.2.12.2.7 $ $Date: 2005/04/11 10:06:11 $
 */
public final class Say2 extends L2GameClientPacket {

    public final static int ALL = 0;
    public final static int SHOUT = 1; //!
    public final static int TELL = 2;
    public final static int PARTY = 3; //#
    public final static int CLAN = 4;  //@
    public final static int GM = 5;
    public final static int PETITION_PLAYER = 6; // used for petition
    public final static int PETITION_GM = 7; //* used for petition
    public final static int TRADE = 8; //+
    public final static int ALLIANCE = 9; //$
    public final static int ANNOUNCEMENT = 10;
    public final static int PARTYROOM_WAIT = 14;
    public final static int PARTYROOM_COMMANDER = 15; //(Yellow)
    public final static int PARTYROOM_ALL = 16; //(Red)
    public final static int HERO_VOICE = 17;
    private String _text;
    private int _type;
    private String _target;

    @Override
    protected void readImpl() {
        _text = readS();
        try {
            _type = readD();
        } catch (BufferUnderflowException e) {
            _type = 0;
        }
        _target = (_type == TELL) ? readS() : null;
    }

    @Override
    protected void runImpl() {
        if (_type < 0 || _type >= 18) {
            return;
        }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPH() < 300) {
            return;
        }
        player.sCPH();

        if (player.getLevel() < Config.CHAT_LEVEL) {
            player.sendPacket(Static.CHAT_LEVEL_PENALTY);
            return;
        }

        if (player.isChatBanned()) {
            player.sendPacket(Static.CHAT_BLOCKED);
            return;
        }

        if (player.isInJail()) {
            switch (_type) {
                case TELL:
                case SHOUT:
                case TRADE:
                case HERO_VOICE:
                    player.sendPacket(Static.CHAT_BLOCKED);
                    return;
            }
        }

        _text = filter(_text);
        if (_text.isEmpty()) {
            return;
        }

        if (_type == PETITION_PLAYER && player.isGM()) {
            _type = PETITION_GM;
        }

        if (player.isInParty() && player.getParty().isInCommandChannel()) {
            if (_text.startsWith("~~") && player.getParty().getCommandChannel().getChannelLeader().equals(player)) {
                _type = PARTYROOM_COMMANDER;
                _text = _text.replace("~~", "");
            } else if (_text.startsWith("~") && player.getParty().isLeader(player)) {
                _type = PARTYROOM_ALL;
                _text = _text.replace("~", "");
            }
        }

        if (Config.USE_CHAT_FILTER && !_text.startsWith(".")) {
            switch (_type) {
                case ALL:
                case SHOUT:
                case TRADE:
                case HERO_VOICE:
                    String wordn = ""; // текущее слово
                    String wordnf = ""; // текущее слово с заменой похожих букв
                    String newWord = ""; // новое слово (текущее если все ок или замененное Config.CHAT_FILTER_STRINGы)
                    String wordf = ""; // новая фраза

                    String[] tokens = _text.split("[ ]+");
                    for (int i = 0; i < tokens.length; i++) {
                        wordn = tokens[i];
                        wordnf = replaceIdent(wordn);
                        for (String pattern : Config.CHAT_FILTER_STRINGS) {
                            if (wordnf.matches(".*" + pattern + ".*")) {
                                newWord = wordnf.replace(pattern, Config.CHAT_FILTER_STRING);
                                break;
                            } else {
                                newWord = wordn;
                            }
                        }
                        wordf += newWord + " ";
                    }
                    _text = wordf.replace("null", "");
                    break;
            }
        }

        //
        if (Config.PROTECT_SAY && player.identSay(_text)) {
            return;
        }
        player.setLastSay(_text);

        CreatureSay cs = new CreatureSay(player.getObjectId(), _type, player.getName(), _text);
        switch (_type) {
            case TELL:
                L2PcInstance receiver = L2World.getInstance().getPlayer(_target);
                if (receiver != null && !BlockList.isBlocked(receiver, player)) {
                    if (receiver.isChatBanned() || receiver.isInJail()) {
                        player.sendPacket(Static.PLAYER_BLOCKED);
                        return;
                    }

                    if (receiver.getMessageRefusal() || receiver.getChatIgnore() >= player.getLevel()) {
                        player.sendPacket(Static.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
                        return;
                    }

                    if (receiver.isFantome()) {
                        Log.add(TimeLogger.getTime() + player.getName() + ": " + _text, "pm_bot");
                    }
                    if (receiver.isPartner()) {
                        Log.add(TimeLogger.getTime() + player.getName() + ": " + _text, "partner_bot");
                        player.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_NOT_ONLINE).addString(_target));
                        return;
                    }

                    if (Config.LOG_CHAT) {
                        Log.chat(player.getName() + "->" + receiver.getName() + ":" + _text + "\n", _type);
                    }

                    receiver.sendPacket(cs);
                    player.sendPacket(new CreatureSay(player.getObjectId(), _type, "->" + receiver.getName(), _text));
                } else {
                    player.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_NOT_ONLINE).addString(_target));
                }
                break;
            case SHOUT:
                if (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("on") || (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("gm") && player.isGM())) {
                    int region = MapRegionTable.getInstance().getMapRegion(player.getX(), player.getY());
                    for (L2PcInstance pchar : L2World.getInstance().getAllPlayers()) {
                        if (region == MapRegionTable.getInstance().getMapRegion(pchar.getX(), pchar.getY())) {
                            pchar.sendSayPacket(cs, player.getLevel());
                        }
                    }
                } else if (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("global")) {
                    for (L2PcInstance pchar : L2World.getInstance().getAllPlayers()) {
                        pchar.sendSayPacket(cs, player.getLevel());
                    }
                }
                if (Config.LOG_CHAT) {
                    Log.chat(player.getName() + ":" + _text + "\n", _type);
                }
                break;
            case TRADE:
                if (System.currentTimeMillis() - player.gCPBH() < 5000) {
                    player.sendPacket(Static.HERO_DELAY);
                    return;
                }
                player.sCPBH();
                if (Config.DEFAULT_TRADE_CHAT.equalsIgnoreCase("on")
                        || (Config.DEFAULT_TRADE_CHAT.equalsIgnoreCase("gm") && player.isGM())) {
                    for (L2PcInstance pchar : L2World.getInstance().getAllPlayers()) {
                        pchar.sendSayPacket(cs, player.getLevel());
                    }
                } else if (Config.DEFAULT_TRADE_CHAT.equalsIgnoreCase("limited")) {
                    int region = MapRegionTable.getInstance().getMapRegion(player.getX(), player.getY());
                    for (L2PcInstance pchar : L2World.getInstance().getAllPlayers()) {
                        if (region == MapRegionTable.getInstance().getMapRegion(pchar.getX(), pchar.getY())) {
                            pchar.sendSayPacket(cs, player.getLevel());
                        }
                    }
                }
                if (Config.LOG_CHAT) {
                    Log.chat(player.getName() + ":" + _text + "\n", _type);
                }
                break;
            case ALL:
                if (_text.startsWith(".")) {
                    StringTokenizer st = new StringTokenizer(_text);
                    IVoicedCommandHandler vch;
                    String command = "";
                    String target = "";

                    /*
                     * if (st.countTokens() > 1) { command =
                     * st.nextToken().substring(1); target =
                     * _text.substring(command.length() + 2); vch =
                     * VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);
                     * } else { command = _text.substring(1); //if
                     * (Config.DEBUG) _log.info("Command: "+command); vch =
                     * VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);
                    }
                     */
                    command = _text.substring(1);
                    vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);
                    if (vch != null && !player.isInJail()) {
                        vch.useVoicedCommand(command, player, target);
                        return;
                    }
                    player.sendPacket(cs);
                    FastList<L2PcInstance> players = player.getKnownList().getKnownPlayersInRadius(1250);
                    L2PcInstance pc = null;
                    for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                        pc = n.getValue();
                        if (pc == null) {
                            continue;
                        }

                        pc.sendPacket(cs);
                    }
                    pc = null;
                } else if (_text.startsWith("~") && Config.ALLOW_RUPOR) {
                    if (player.getItemCount(Config.RUPOR_ID) >= 1) {
                        if (player.isInJail()) {
                            return;
                        }

                        L2ItemInstance item = player.getInventory().getItemByItemId(Config.RUPOR_ID);

                        if (item == null) {
                            player.sendMessage("Реплика на весь мир стоит 1 микрофон");
                            return;
                        }

                        player.destroyItemByItemId("Say2", Config.RUPOR_ID, 1, player, false);

                        _text = _text.substring(1);

                        cs = new CreatureSay(player.getObjectId(), 18, player.getName(), player.getName() + ":" + _text);
                        for (L2PcInstance pchar : L2World.getInstance().getAllPlayers()) {
                            if (!pchar.isWorldIgnore()) {
                                pchar.sendPacket(cs);
                            }
                        }
                    } else {
                        player.sendMessage("Реплика на весь мир стоит 1 микрофон");
                        return;
                    }
                } else {
                    player.sendPacket(cs);
                    FastList<L2PcInstance> players = player.getKnownList().getKnownPlayersInRadius(1250);
                    L2PcInstance pc = null;
                    for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                        pc = n.getValue();
                        if (pc == null) {
                            continue;
                        }

                        pc.sendSayPacket(cs, player.getLevel());
                    }
                    pc = null;
                    if (Config.LOG_CHAT) {
                        Log.chat(player.getName() + ":" + _text + "\n", _type);
                    }
                }
                break;
            case CLAN:
                if (player.getClan() != null) {
                    player.getClan().broadcastToOnlineMembers(cs);
                    if (Config.LOG_CHAT) {
                        Log.chat(player.getName() + ":" + _text + "\n", _type);
                    }
                }
                break;
            case ALLIANCE:
                if (player.getClan() != null) {
                    player.getClan().broadcastToOnlineAllyMembers(cs);
                    if (Config.LOG_CHAT) {
                        Log.chat(player.getName() + ":" + _text + "\n", _type);
                    }
                }
                break;
            case PARTY:
                if (player.isInParty()) {
                    player.getParty().broadcastToPartyMembers(cs);
                    if (Config.LOG_CHAT) {
                        Log.chat(player.getName() + ":" + _text + "\n", _type);
                    }
                }
                break;
            case PETITION_PLAYER:
            case PETITION_GM:
                if (!PetitionManager.getInstance().isPlayerInConsultation(player)) {
                    player.sendPacket(Static.YOU_ARE_NOT_IN_PETITION_CHAT);
                    break;
                }

                PetitionManager.getInstance().sendActivePetitionMessage(player, _text);
                break;
            case PARTYROOM_ALL:
                if (player.isInParty()) {
                    if (player.getParty().isInCommandChannel() && player.getParty().isLeader(player)) {
                        player.getParty().getCommandChannel().broadcastToChannelMembers(cs);
                    }
                }
                break;
            case PARTYROOM_COMMANDER:
                if (player.isInParty()) {
                    if (player.getParty().isInCommandChannel() && player.getParty().getCommandChannel().getChannelLeader().equals(player)) {
                        player.getParty().getCommandChannel().broadcastToChannelMembers(cs);
                    }
                }
                break;
            case HERO_VOICE:
                if (player.isHero() || player.isGM() || player.getTradersIgnore()) {
                    if (!player.isGM() && System.currentTimeMillis() - player.gCPBH() < 5000) {
                        player.sendPacket(Static.HERO_DELAY);
                        return;
                    }
                    player.sCPBH();

                    //Log.add(TimeLogger.getTime() + player.getName() + ": " + _text, "hero_chat");

                    for (L2PcInstance pchar : L2World.getInstance().getAllPlayers()) {
                        if (!BlockList.isBlocked(pchar, player)) {
                            pchar.sendPacket(cs);
                        }
                    }
                    if (Config.LOG_CHAT) {
                        Log.chat(player.getName() + ":" + _text + "\n", _type);
                    }
                }
                break;
            case PARTYROOM_WAIT:
                player.sayToPartyRoom(cs);
                break;
        }
        cs = null;
    }

    public static String filter(String source) {
        if (source.length() > Config.MAX_CHAT_LENGTH) {
            source = source.substring(0, Config.MAX_CHAT_LENGTH);
        }

        source = source.replaceAll("\n", "");
        source = source.replace("\n", "");
        source = source.replace("n\\", "");
        source = source.replace("\r", "");
        source = source.replace("r\\", "");

        source = ltrim(source);
        source = rtrim(source);
        source = itrim(source);
        source = lrtrim(source);
        return source;
    }

    public static String replaceIdent(String word) {
        word = word.toLowerCase();
        word = word.replace("a", "а");
        word = word.replace("c", "с");
        word = word.replace("s", "с");
        word = word.replace("e", "е");
        word = word.replace("k", "к");
        word = word.replace("m", "м");
        word = word.replace("o", "о");
        word = word.replace("0", "о");
        word = word.replace("x", "х");
        word = word.replace("uy", "уй");
        word = word.replace("y", "у");
        word = word.replace("u", "у");
        word = word.replace("ё", "е");
        word = word.replace("9", "я");
        word = word.replace("3", "з");
        word = word.replace("z", "з");
        word = word.replace("d", "д");
        word = word.replace("p", "п");
        word = word.replace("i", "и");
        word = word.replace("ya", "я");
        word = word.replace("ja", "я");
        return word;
    }

    public static String ltrim(String source) {
        return source.replaceAll("^\\s+", "");
    }

    public static String rtrim(String source) {
        return source.replaceAll("\\s+$", "");
    }

    public static String itrim(String source) {
        return source.replaceAll("\\b\\s{2,}\\b", " ");
    }

    public static String trim(String source) {
        return itrim(ltrim(rtrim(source)));
    }

    public static String lrtrim(String source) {
        return ltrim(rtrim(source));
    }
}
