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

import java.util.logging.Logger;
import javolution.util.FastTable;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.SkillSpellbookTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2PledgeSkillLearn;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.actor.instance.L2FishermanInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2VillageMasterInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestAquireSkill extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(RequestAquireSkill.class.getName());
    private int _id;
    private int _level;
    private int _skillType;

    @Override
    protected void readImpl() {
        _id = readD();
        _level = readD();
        _skillType = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPAQ() < 300) {
            return;
        }

        player.sCPAQ();

        if (player.getAquFlag() != _id) {
            return;
        }

        player.setAquFlag(0);

        L2FolkInstance trainer = player.getLastFolkNPC();
        if (trainer == null) {
            return;
        }

        int npcid = trainer.getNpcId();
        if (!player.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false) && !player.isGM()) {
            return;
        }

        if (!Config.ALT_GAME_SKILL_LEARN) {
            player.setSkillLearningClassId(player.getClassId());
        }

        if (player.getSkillLevel(_id) >= _level) {
            return;
        }

        SkillTable st = SkillTable.getInstance();
        SkillTreeTable stt = SkillTreeTable.getInstance();

        int counts = 0;
        int _requiredSp = 10000000;
        L2Skill skill = st.getInfo(_id, _level);

        switch (_skillType) {
            case 0:
                for (L2SkillLearn s : stt.getAvailableSkills(player, player.getSkillLearningClassId())) {
                    L2Skill sk = st.getInfo(s.getId(), s.getLevel());
                    if (sk == null || sk != skill || !sk.getCanLearn(player.getSkillLearningClassId()) || !sk.canTeachBy(npcid)) {
                        continue;
                    }
                    counts++;
                    _requiredSp = stt.getSkillCost(player, skill);
                }

                if (counts == 0 && !Config.ALT_GAME_SKILL_LEARN) {
                    //player.sendMessage("You are trying to learn skill that u can't..");
                    //Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
                    return;
                }

                if (player.getSp() >= _requiredSp) {
                    if (Config.SP_BOOK_NEEDED) {
                        int spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill);

                        if (skill.getLevel() == 1 && spbId > -1) {
                            L2ItemInstance spb = player.getInventory().getItemByItemId(spbId);

                            if (spb == null) {
                                // Haven't spellbook
                                player.sendPacket(Static.ITEM_MISSING_TO_LEARN_SKILL);
                                return;
                            }

                            // ok
                            player.destroyItem("Consume", spb, trainer, true);
                        }
                    }
                } else {
                    player.sendPacket(Static.NOT_ENOUGH_SP_TO_LEARN_SKILL);
                    return;
                }
                break;
            case 1:
                int costid = 0;
                int costcount = 0;
                // Skill Learn bug Fix
                L2Skill sk = null;
                for (L2SkillLearn s : stt.getAvailableSkills(player)) {
                    sk = st.getInfo(s.getId(), s.getLevel());
                    if (sk == null || sk != skill) {
                        continue;
                    }

                    counts++;
                    costid = s.getIdCost();
                    costcount = s.getCostCount();
                    _requiredSp = s.getSpCost();
                }

                if (counts == 0) {
                    player.sendPacket(Static.CANT_LEARN_SKILL);
                    //Util.handleIllegalPlayerAction(player, "Player "+ player.getName()+ " tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
                    return;
                }

                if (player.getSp() >= _requiredSp) {
                    if (!player.destroyItemByItemId("Consume", costid, costcount, trainer, false)) {
                        // Haven't spellbook
                        player.sendPacket(Static.ITEM_MISSING_TO_LEARN_SKILL);
                        return;
                    }

                    sendPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addNumber(costcount).addItemName(costid));
                } else {
                    player.sendPacket(Static.NOT_ENOUGH_SP_TO_LEARN_SKILL);
                    return;
                }
                break;
            case 2:
                if (!player.isClanLeader()) {
                    player.sendPacket(Static.ONLY_FOR_CLANLEADER);
                    return;
                }

                int itemId = 0;
                int repCost = 100000000;
                // Skill Learn bug Fix
                L2Skill skl = null;
                for (L2PledgeSkillLearn s : stt.getAvailablePledgeSkills(player)) {
                    skl = st.getInfo(s.getId(), s.getLevel());
                    if (skl == null || skl != skill) {
                        continue;
                    }

                    counts++;
                    itemId = s.getItemId();
                    repCost = s.getRepCost();
                }

                if (player.isPremium()) {
                    repCost *= Config.PREMIUM_AQURE_SKILL_MUL;
                }

                if (counts == 0) {
                    //player.sendMessage("You are trying to learn skill that u can't..");
                    //Util.handleIllegalPlayerAction(player, "Player " + player.getName()+ " tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
                    return;
                }

                if (player.getClan().getReputationScore() >= repCost) {
                    if (Config.LIFE_CRYSTAL_NEEDED) {
                        if (!player.destroyItemByItemId("Consume", itemId, 1, trainer, false)) {
                            // Haven't spellbook
                            player.sendPacket(Static.ITEM_MISSING_TO_LEARN_SKILL);
                            return;
                        }

                        sendPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addItemName(itemId).addNumber(1));
                    }
                } else {
                    player.sendPacket(Static.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE);
                    return;
                }
                player.getClan().setReputationScore(player.getClan().getReputationScore() - repCost, true);
                player.getClan().addNewSkill(skill);

                player.sendPacket(SystemMessage.id(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(repCost));
                player.sendPacket(SystemMessage.id(SystemMessageId.CLAN_SKILL_S1_ADDED).addSkillName(_id));

                player.getClan().broadcastToOnlineMembers(new PledgeSkillList(player.getClan()));

                for (L2PcInstance member : player.getClan().getOnlineMembers("")) {
                    if (member == null) {
                        continue;
                    }

                    member.sendSkillList();
                }

                trainer.showPledgeSkillList(player); //Maybe we shoud add a check here...
                return;
            default:
                _log.warning("Recived Wrong Packet Data in Aquired Skill - unk1:" + _skillType);
                return;
        }

        player.stopSkillEffects(_id);
        player.addSkill(skill, true);

        player.setSp(player.getSp() - _requiredSp);

        player.updateStats();
        player.sendChanges();

        sendPacket(SystemMessage.id(SystemMessageId.SP_DECREASED_S1).addNumber(_requiredSp));
        player.sendPacket(SystemMessage.id(SystemMessageId.LEARNED_SKILL_S1).addSkillName(_id));

        // update all the shortcuts to this skill
        if (_level > 1) {
            FastTable<L2ShortCut> allShortCuts = new FastTable<L2ShortCut>();
            allShortCuts.addAll(player.getAllShortCuts());

            for (int i = 0, n = allShortCuts.size(); i < n; i++) {
                L2ShortCut sc = allShortCuts.get(i);
                if (sc == null) {
                    continue;
                }

                if (sc.getId() == _id && sc.getType() == L2ShortCut.TYPE_SKILL) {
                    L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _level, 1);
                    player.sendPacket(new ShortCutRegister(newsc));
                    player.registerShortCut(newsc);
                }
            }
            allShortCuts.clear();
            allShortCuts = null;
        }

        if (trainer.isL2Fisherman()) {
            trainer.showSkillList(player);
        } else {
            trainer.showSkillList(player, player.getSkillLearningClassId());
        }

        if (_id >= 1368 && _id <= 1372) // if skill is expand sendpacket :)
        {
            player.sendPacket(new ExStorageMaxCount(player));
        }
    }
}
