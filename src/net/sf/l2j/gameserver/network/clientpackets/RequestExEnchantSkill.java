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
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

/**
 * Format chdd
 * c: (id) 0xD0
 * h: (subid) 0x06
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 *
 */
public final class RequestExEnchantSkill extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestAquireSkill.class.getName());
	private int _skillId;
	private int _skillLvl;


	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected
	void runImpl()
	{

		L2PcInstance player = getClient().getActiveChar();
        if (player == null)
        	return;

        L2FolkInstance trainer = player.getLastFolkNPC();
        if (trainer == null)
        	return;

        int npcid = trainer.getNpcId();

        if ((trainer == null || !player.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !player.isGM())
            return;

        if (player.getSkillLevel(_skillId) >= _skillLvl)// already knows the skill with this level
            return;

        if (player.getClassId().getId() < 88) // requires to have 3rd class quest completed
    		return;

        if (player.getLevel() < 76) return;

        L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);

        int counts = 0;
        int _requiredSp = 10000000;
        int _requiredExp = 100000;
        byte _rate = 0;
        int _baseLvl = 1;

        L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);

        for (L2EnchantSkillLearn s : skills)
        {
        	L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
        	if (sk == null || sk != skill || !sk.getCanLearn(player.getClassId())
        			|| !sk.canTeachBy(npcid)) continue;
        	counts++;
        	_requiredSp = s.getSpCost();
        	_requiredExp = s.getExp();
        	_rate = s.getRate(player, (Config.PREMIUM_ENABLE && player.isPremium()));
        	_baseLvl = s.getBaseLevel();
        }

        if (counts == 0 && !Config.ALT_GAME_SKILL_LEARN)
        {
        	player.sendPacket(Static.CANT_LEARN_SKILL);
        	//Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
        	return;
        }

        if (player.getSp() >= _requiredSp)
        {
        	if (player.getExp() >= _requiredExp)
        	{
        		if (Config.ES_SP_BOOK_NEEDED && (_skillLvl == 101 || _skillLvl == 141)) // only first lvl requires book
            	{
            		int spbId = 6622;

            		L2ItemInstance spb = player.getInventory().getItemByItemId(spbId);

            		if (spb == null)// Haven't spellbook
            		{
            			player.sendPacket(Static.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
            			return;
            		}
            		// ok
            		player.destroyItem("Consume", spb, trainer, true);
            	}
        	}
        	else
        	{
            	player.sendPacket(Static.YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL);
        		return;
        	}
        }
        else
        {
        	player.sendPacket(Static.YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
        	return;
        }
        if (Rnd.get(100) <= _rate)
        {
        	player.addSkill(skill, true);
        	player.getStat().removeExpAndSp(_requiredExp, _requiredSp);

        	StatusUpdate su = new StatusUpdate(player.getObjectId());
        	su.addAttribute(StatusUpdate.SP, player.getSp());
        	player.sendPacket(su);

            sendPacket(SystemMessage.id(SystemMessageId.EXP_DECREASED_BY_S1).addNumber(_requiredExp));
            sendPacket(SystemMessage.id(SystemMessageId.SP_DECREASED_S1).addNumber(_requiredSp));
        	player.sendPacket(SystemMessage.id(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1).addSkillName(_skillId));
        }
        else
        {
        	if (skill.getLevel() > 100)
        	{
        		_skillLvl = _baseLvl;
        		player.addSkill(SkillTable.getInstance().getInfo(_skillId, _skillLvl), true);
        		player.sendSkillList(); 
        	}
        	player.sendPacket(SystemMessage.id(SystemMessageId.YOU_HAVE_FAILED_TO_ENCHANT_THE_SKILL_S1).addSkillName(_skillId));
        }
        trainer.showEnchantSkillList(player, player.getClassId());

        // update all the shortcuts to this skill
		FastTable <L2ShortCut> allShortCuts = new FastTable<L2ShortCut>();
		allShortCuts.addAll(player.getAllShortCuts());
		for (int i = 0, n = allShortCuts.size(); i < n; i++)
		{
		    L2ShortCut sc = allShortCuts.get(i);
			if (sc == null)
				continue;
				
        	if (sc.getId() == _skillId && sc.getType() == L2ShortCut.TYPE_SKILL)
        	{
        		L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _skillLvl, 1);
        		player.sendPacket(new ShortCutRegister(newsc));
        		player.registerShortCut(newsc);
        	}
        }
		allShortCuts.clear();
		allShortCuts = null;
	}
}
