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

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AquireSkillList;
import net.sf.l2j.gameserver.network.serverpackets.BuyList;
import net.sf.l2j.gameserver.network.serverpackets.SellList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class L2FishermanInstance extends L2FolkInstance {

    /**
     * @param objectId
     * @param template
     */
    public L2FishermanInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public String getHtmlPath(int npcId, int val) {
        String pom = "";

        if (val == 0) {
            pom = "" + npcId;
        } else {
            pom = npcId + "-" + val;
        }

        return "data/html/fisherman/" + pom + ".htm";
    }

    private void showBuyWindow(L2PcInstance player, int val) {
        double taxRate = 0;
        if (getIsInTown()) {
            taxRate = getCastle().getTaxRate();
        }
        player.tempInvetoryDisable();
        //if (Config.DEBUG) _log.fine("Showing buylist");
        L2TradeList list = TradeController.getInstance().getBuyList(val);

        if (list != null && list.getNpcId().equals(String.valueOf(getNpcId()))) {
            BuyList bl = new BuyList(list, player.getAdena(), taxRate);
            player.sendPacket(bl);
        } else {
            _log.warning("possible client hacker: " + player.getName() + " attempting to buy from GM shop! < Ban him!");
            _log.warning("buylist id:" + val);
        }

        player.sendActionFailed();
    }

    private void showSellWindow(L2PcInstance player) {
        // if (Config.DEBUG) _log.fine("Showing selllist");

        player.sendPacket(new SellList(player));

        //if (Config.DEBUG) _log.fine("Showing sell window");

        player.sendActionFailed();
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.startsWith("FishSkillList")) {
            player.setSkillLearningClassId(player.getClassId());
            showSkillList(player);
        }

        StringTokenizer st = new StringTokenizer(command, " ");
        String command2 = st.nextToken();

        if (command2.equalsIgnoreCase("Buy")) {
            if (st.countTokens() < 1) {
                return;
            }
            int val = Integer.parseInt(st.nextToken());
            showBuyWindow(player, val);
        } else if (command2.equalsIgnoreCase("Sell")) {
            showSellWindow(player);
        } else {
            super.onBypassFeedback(player, command);
        }
    }

    @Override
    public void showSkillList(L2PcInstance player) {
        SkillTable st = SkillTable.getInstance();
        SkillTreeTable stt = SkillTreeTable.getInstance();
        L2SkillLearn[] skills = stt.getAvailableSkills(player);
        AquireSkillList asl = new AquireSkillList(AquireSkillList.SkillType.Fishing);

        int counts = 0;
        L2Skill sk = null;
        for (L2SkillLearn s : skills) {
            sk = st.getInfo(s.getId(), s.getLevel());
            if (sk == null) {
                continue;
            }

            counts++;
            asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 1);
        }

        if (counts == 0) {
            asl = null;
            int minlevel = stt.getMinLevelForNewSkill(player);
            if (minlevel > 0) {
                // No more skills to learn, come back when you level.
                player.sendPacket(SystemMessage.id(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN).addNumber(minlevel));
            } else {
                player.sendHtmlMessage("You've learned all skills.");
            }
        } else {
            player.sendPacket(asl);
        }

        player.sendActionFailed();
    }

    @Override
    public boolean isL2Fisherman() {
        return true;
    }
}
