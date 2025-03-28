/* This program is free software; you can redistribute it and/or modify
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
package scripts.skills.skillhandlers;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;

import scripts.skills.ISkillHandler;
import scripts.skills.SkillHandler;

import javolution.util.FastList;

public class BalanceLife implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS = { SkillType.BALANCE_LIFE };

	public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
	{
		// L2Character activeChar = activeChar;
		// check for other effects
		try
		{
			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(SkillType.BUFF);

			if (handler != null)
				handler.useSkill(activeChar, skill, targets);
		} 
		catch (Exception e)
		{
		}

		L2Character target = null;

		L2PcInstance player = null;
		if (activeChar.isPlayer())
			player = (L2PcInstance) activeChar;

		double fullHP = 0;
		double currentHPs = 0;

		for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			target = (L2Character) n.getValue();

			// We should not heal if char is dead
			if (target == null || target.isDead() || target.isAlikeDead() || target.getCurrentHp() <= 0)
				continue;

			// Player holding a cursed weapon can't be healed and can't heal
			if (target != activeChar)
			{
				if (target.isPlayer() && ((L2PcInstance) target).isCursedWeaponEquiped())
					continue;
				else if (player != null && player.isCursedWeaponEquiped())
					continue;
			}

			fullHP += target.getMaxHp();
			currentHPs += target.getCurrentHp();
		}

		double percentHP = currentHPs / fullHP;

		for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			target = (L2Character) n.getValue();
			
			// We should not heal if char is dead
			if (target == null || target.isDead() || target.isAlikeDead() || target.getCurrentHp() <= 0)
				continue;

			double newHP = target.getMaxHp() * percentHP;
			double totalHeal = newHP - target.getCurrentHp();

			target.setCurrentHp(newHP);

			if (totalHeal > 0)
				target.setLastHealAmount((int) totalHeal);

			/*StatusUpdate su = new StatusUpdate(target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);*/
			
			if (target.isPlayer())
				((L2PcInstance)target).sendMessage("HP of the party has been balanced.");
		}
	}

	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
