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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2FolkInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExEnchantSkillInfo;

/**
 * Format chdd
 * c: (id) 0xD0
 * h: (subid) 0x06
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 *
 */
public final class RequestExEnchantSkillInfo extends L2GameClientPacket
{
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
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

        if (player == null)
            return;

        if (player.getLevel() < 76)
            return;

        L2FolkInstance trainer = player.getLastFolkNPC();

        if ((trainer == null || !player.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false)) && !player.isGM())
            return;

        L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);

        boolean canteach = false;

        if (skill == null || skill.getId() != _skillId)
        {
            //_log.warning("enchant skill id " + _skillID + " level " + _skillLvl
            //    + " is undefined. aquireEnchantSkillInfo failed.");
        	player.sendMessage("This skill doesn't yet have enchant info in Datapack");
            return;
        }

        if (!trainer.getTemplate().canTeach(player.getClassId()))
        	return; // cheater

        L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);

        for (L2EnchantSkillLearn s : skills)
        {
        	if (s.getId() == _skillId && s.getLevel() == _skillLvl)
        	{
        		canteach = true;
        		break;
        	}
        }

        if (!canteach)
        	return; // cheater

        int requiredSp = SkillTreeTable.getInstance().getSkillSpCost(player, skill);
        int requiredExp = SkillTreeTable.getInstance().getSkillExpCost(player, skill);
        byte rate = SkillTreeTable.getInstance().getSkillRate(player, skill);
        ExEnchantSkillInfo asi = new ExEnchantSkillInfo(skill.getId(), skill.getLevel(), requiredSp, requiredExp, rate);

        if (Config.ES_SP_BOOK_NEEDED && (skill.getLevel() == 101 || skill.getLevel() == 141)) // only first lvl requires book
        {
        	int spbId = 6622;
        	asi.addRequirement(4, spbId, 1, 0);
        }
        sendPacket(asi);

	}
}