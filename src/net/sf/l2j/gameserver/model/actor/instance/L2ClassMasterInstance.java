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
package net.sf.l2j.gameserver.model.actor.instance;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.ClassLevel;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Log;

import java.util.logging.Logger;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.7 $ $Date: 2005/03/27 15:29:32 $
 */
public final class L2ClassMasterInstance extends L2FolkInstance {
    private static Logger _log = Logger.getLogger(L2ClassMasterInstance.class.getName());

    public static final int[] SECONDN_CLASS_IDS = {2, 3, 5, 6, 9, 8, 12, 13, 14, 16, 17, 20, 21, 23, 24, 27,
        28, 30, 33, 34, 36, 37, 40, 41, 43, 46, 48, 51, 52, 55, 57};
    private final int CLAN_COIN = Config.MCLAN_COIN;
    private final String CLAN_COIN_NAME = Config.MCLAN_COIN_NAME; // название итема, за перенос аугментации
    private final int CLAN_LVL6 = Config.CLAN_LVL6;
    private final int CLAN_LVL7 = Config.CLAN_LVL7;
    private final int CLAN_LVL8 = Config.CLAN_LVL8;

    /**
     * @param template
     */
    public L2ClassMasterInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    private static L2ClassMasterInstance _classmaster;

    public static L2ClassMasterInstance getInstance() {
        return _classmaster;
    }

    @Override
    public void onAction(L2PcInstance player) {
        if (!canTarget(player)) {
            return;
        }

        if (player.isCursedWeaponEquiped()) {
            player.sendActionFailed();
            return;
        }

        // Check if the L2PcInstance already target the L2NpcInstance
        if (getObjectId() != player.getTargetId()) {
            // Set the target of the L2PcInstance player
            player.setTarget(this);

            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            player.sendPacket(new MyTargetSelected(getObjectId(), 0));

            // Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
        } else {
            if (!canInteract(player)) {
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
                return;
            }

            NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
            html.setFile("data/html/classmaster/index.htm");
            html.replace("%objectId%", String.valueOf(getObjectId()));
            html.replace("%config_master_npcname%", Config.MASTER_NPCNAME);
            player.sendPacket(html);
        }
        player.sendActionFailed();
    }

    @Override
    public String getHtmlPath(int npcId, int val) {
        return "data/html/classmaster/" + val + ".htm";
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.equalsIgnoreCase("class_master")) {
            if (!Config.ALLOW_CLASS_MASTERS) {
                player.sendPacket(Static.DISABLED);
                return;
            }

            ClassId classId = player.getClassId();
            int jobLevel = 0;
            int level = player.getLevel();
            ClassLevel lvl = PlayerClass.values()[classId.getId()].getLevel();
            switch (lvl) {
                case First:
                    jobLevel = 1;
                    break;
                case Second:
                    jobLevel = 2;
                    break;
                default:
                    jobLevel = 3;
            }

            if (((level >= 20 && jobLevel == 1) || (level >= 40 && jobLevel == 2)) && Config.ALLOW_CLASS_MASTERS) {
                showChatWindow(player, classId.getId());
            } else if (level >= 76 && Config.ALLOW_CLASS_MASTERS && classId.getId() < 88) {
                for (int i = 0; i < SECONDN_CLASS_IDS.length; i++) {
                    if (classId.getId() == SECONDN_CLASS_IDS[i]) {
                        NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
                        TextBuilder sb = new TextBuilder();
                        sb.append("<html><body><table width=200>");
                        sb.append("<tr><td><br></td></tr>");
                        sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class " + (88 + i) + "\">Продолжить за " + CharTemplateTable.getClassNameById(88 + i) + "</a></td></tr>");
                        sb.append("<tr><td><br></td></tr>");
                        sb.append("</table></body></html>");
                        html.setHtml(sb.toString());
                        sb.clear();
                        sb = null;
                        player.sendPacket(html);
                        break;
                    }
                }
            } else {
                NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
                TextBuilder sb = new TextBuilder();
                sb.append("<html><body>");
                switch (jobLevel) {
                    case 1:
                        sb.append("Приходите, когда получите 20 уровень.<br>");
                        break;
                    case 2:
                        sb.append("Приходите, когда получите 40 уровень.<br>");
                        break;
                    case 3:
                        sb.append("Нет доступных профессий.<br>");
                        break;
                }

                //for (Quest q : Quest.findAllEvents())
                //	sb.append("Event: <a action=\"bypass -h Quest "+q.getName()+"\">"+q.getDescr()+"</a><br>");
                sb.append("</body></html>");
                html.setHtml(sb.toString());
                sb.clear();
                sb = null;
                player.sendPacket(html);
            }
        } else if (command.equalsIgnoreCase("clan_level")) {
            if (!Config.ALLOW_CLAN_LEVEL) {
                player.sendPacket(Static.DISABLED);
                return;
            }

            if (!player.isClanLeader()) {
                player.sendPacket(Static.WAR_NOT_LEADER);
                return;
            }

            if (player.getClan().getLevel() < 5) {
                player.sendPacket(Static.CLAN_5LVL_HIGHER);
                return;
            }

            L2Clan clan = player.getClan();
            int level = clan.getLevel();
            NpcHtmlMessage reply = NpcHtmlMessage.id(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");
            replyMSG.append("Повышение уровня клана:<br1>");
            if (level < 8) {
                switch (level) {
                    case 5:
                        replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanLevel_6\">6 уровень</a> (" + CLAN_LVL6 + " " + CLAN_COIN_NAME + ")<br>");
                        //replyMSG.append("<font color=999999>[7 уровень]</font> (" + CLAN_LVL7 + " " + CLAN_COIN_NAME + ")<br>");
                        //replyMSG.append("<font color=999999>[8 уровень]</font> (" + CLAN_LVL8 + " " + CLAN_COIN_NAME + ")<br>");
                        break;
                    case 6:
                        replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanLevel_7\">7 уровень.</a> (" + CLAN_LVL7 + " " + CLAN_COIN_NAME + ")<br>");
                        //replyMSG.append("<font color=999999>[8 уровень]</font> (" + CLAN_LVL8 + " " + CLAN_COIN_NAME + ")<br>");
                        break;
                    case 7:
                        replyMSG.append("<a action=\"bypass -h npc_" + getObjectId() + "_clanLevel_8\">8 уровень.</a> (" + CLAN_LVL8 + " " + CLAN_COIN_NAME + ")<br>");
                        break;
                }
            } else {
                replyMSG.append("<font color=66CC00>Уже максимальный! Already MAX</font><br>");
            }
            replyMSG.append("</body></html>");
            reply.setHtml(replyMSG.toString());
            player.sendPacket(reply);
        } else if (command.startsWith("change_class")) {
            int val = Integer.parseInt(command.substring(13));

            // Exploit prevention
            ClassId classId = player.getClassId();
            int level = player.getLevel();
            int jobLevel = 0;
            int newJobLevel = 0;

            ClassLevel lvlnow = PlayerClass.values()[classId.getId()].getLevel();

            switch (lvlnow) {
                case First:
                    jobLevel = 1;
                    break;
                case Second:
                    jobLevel = 2;
                    break;
                case Third:
                    jobLevel = 3;
                    break;
                default:
                    jobLevel = 4;
            }

            if (jobLevel == 4) {
                return; // no more job changes
            }
            ClassLevel lvlnext = PlayerClass.values()[val].getLevel();
            switch (lvlnext) {
                case First:
                    newJobLevel = 1;
                    break;
                case Second:
                    newJobLevel = 2;
                    break;
                case Third:
                    newJobLevel = 3;
                    break;
                default:
                    newJobLevel = 4;
            }

            // prevents changing between same level jobs
            if (newJobLevel != jobLevel + 1) {
                return;
            }

            if (level < 20 && newJobLevel > 1) {
                return;
            }
            if (level < 40 && newJobLevel > 2) {
                return;
            }
            if (level < 75 && newJobLevel > 3) {
                return;
            }
            // -- prevention ends

            EventReward pay = Config.CLASS_MASTERS_PRICES.get(newJobLevel);
            if (pay != null) {
                if (player.getItemCount(pay.id) < pay.count) {
                    player.sendHtmlMessage("Class Master", "Cтоимость получения профы " + pay.count + " " + ItemTable.getInstance().getTemplate(pay.id).getName() + "!");
                    return;
                }
                player.destroyItemByItemId("clasmaster", pay.id, pay.count, player, true);
            }

            changeClass(player, val);
            player.checkAllowedSkills();

            if (val >= 88) {
                player.sendPacket(Static.THIRD_CLASS_TRANSFER); // system sound 3rd occupation
            } else {
                player.sendPacket(Static.CLASS_TRANSFER);    // system sound for 1st and 2nd occupation
            }
            NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
            TextBuilder sb = new TextBuilder();
            sb.append("<html><body>");
            sb.append("Получена профессия <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getClassId().getId()) + "</font>.");
            if (Config.REWARD_SHADOW) {
                player.setShadeItems(true);
                if (newJobLevel == 3 && level >= 40) {
                    sb.append("<br>Выбери желаемый сет:<br>");
                    sb.append("<table width=300><tr><td><a action=\"bypass -h npc_" + getObjectId() + "_getArmor 1\">Avadon Robe Set</a><br1><font color=666666>//P. Def. +5.26% and Casting Spd. +15%.</font></td></tr>");
                    sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_getArmor 2\">Leather Armor of Doom</a><br1><font color=666666>//P. Atk. +2.7%, MP recovery rate +2.5%, STR -1, CON -2, DEX +3.</font></td></tr>");
                    sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_getArmor 3\">Doom Plate Armor</a><br1><font color=666666>//Maximum HP +320, Breath Gauge increase, STR-3, and CON+3.</font></td></tr>");
                    sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_getArmor 4\">Blue Wolf Breastplate</a><br1><font color=666666>//Speed +7, and HP recovery rate +5.24%, STR+3, CON-1, and DEX-2.</font></td></tr></table><br>");
                }

                if (val >= 88) {
                    sb.append("<table width=300><tr><td><a action=\"bypass -h npc_" + getObjectId() + "_getArmor 5\">Robe Flame Armor</a><br1><font color=666666>//CP + 177, MP + 400, C.Spd 15%, M.Atk 15%, M.Def/P.Def 4%.</font></td></tr>");
                    sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_getArmor 6\">Light Flame Armor</a><br1><font color=666666>//CP + 195, HP/MP + 200, Crit.Dmg 25%, Atk.Spd 10% M.Def/P.Def 8%.</font></td></tr>");
                    sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_getArmor 7\">Heavy Flame Armor</a><br1><font color=666666>//CP + 232, HP + 400, Atk.Dmg 15%, Atk.Spd 15%, M.Def/P.Def 12%.</font></td></tr></table><br>");
                }
            }
            sb.append("</body></html>");
            html.setHtml(sb.toString());
            sb.clear();
            sb = null;
            player.sendPacket(html);

            if (Config.VS_CKEY_CHARLEVEL) {
                player.setUserKeyOnLevel();
            }
        } else if (command.startsWith("clanLevel_")) {
            if (!Config.ALLOW_CLAN_LEVEL) {
                player.sendPacket(Static.DISABLED);
                return;
            }
            int level = Integer.parseInt(command.substring(10).trim());
            clanSetLevel(player, level);
        } else if (command.startsWith("getArmor")) {
            int val = Integer.parseInt(command.substring(9));

            if (player.getShadeItems()) {
                return;
            }

            player.setShadeItems(false);

            Inventory inventory = player.getInventory();
            int[] shadowSet = CustomServerData.getInstance().getShadeItems(val);
            for (int i = 0; i < shadowSet.length; i++) {
                L2ItemInstance item = ItemTable.getInstance().createItem("China3", shadowSet[i], 1, player, null);
                if (val < 5) {
                    item.setEnchantLevel(30);
                } else {
                    item.setEnchantLevel(37);
                    item.setMana(180);
                }
                inventory.addItem("China3", item, player, null);
                inventory.equipItemAndRecord(item);
                item.decreaseMana(true);
            }
            if (val >= 5) {
                player.addItem("China3", 50009, 1, player, true);
            }

            player.sendItems(true);
        } else {
            super.onBypassFeedback(player, command);
        }
    }

    private void clanSetLevel(L2PcInstance player, int level) {
        if (CLAN_COIN > 0) {
            int price = 99999;
            switch (level) {
                case 6:
                    price = CLAN_LVL6;
                    break;
                case 7:
                    price = CLAN_LVL7;
                    break;
                case 8:
                    price = CLAN_LVL8;
                    break;
            }

            L2ItemInstance coin = player.getInventory().getItemByItemId(CLAN_COIN);
            if (coin == null || coin.getCount() < price) {
                player.sendMessage("Проверьте стоимость");
                return;
            }

            if (!player.destroyItemByItemId("DS clanSetLevel", CLAN_COIN, price, player, true)) {
                player.sendMessage("Проверьте стоимость");
                return;
            }
            Log.addDonate(player, "Clan Level: " + level, price);
        }

        player.getClan().changeLevel(level);
        player.sendMessage("Уровень клана увеличен до " + level + "\nClan level raised up to " + level);
    }

    @Override
    public void changeClass(L2PcInstance player, int val) {
        player.abortAttack();
        player.abortCast();
        player.setIsParalyzed(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        player.setClassId(val);
        if (player.isSubClassActive()) {
            player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
        } else {
            player.setBaseClass(player.getActiveClass());
        }

        player.rewardSkills();
        player.store();
        player.broadcastUserInfo();
        player.setIsParalyzed(false);
    }
}
