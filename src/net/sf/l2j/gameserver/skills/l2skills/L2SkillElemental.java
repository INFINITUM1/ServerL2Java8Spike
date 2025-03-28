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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.StatsSet;
import javolution.util.FastList;
import javolution.util.FastList.Node;

public class L2SkillElemental extends L2Skill 
{

	private final int[] _seeds;
	private final boolean _seedAny;

	public L2SkillElemental(StatsSet set) {
		super(set);

		_seeds = new int[3];
		_seeds[0] = set.getInteger("seed1",0);
		_seeds[1] = set.getInteger("seed2",0);
		_seeds[2] = set.getInteger("seed3",0);

		if (set.getInteger("seed_any",0)==1)
			_seedAny = true;
		else
			_seedAny = false;
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Object> targets) 
	{
		if (activeChar.isAlikeDead())
			return;

		boolean ss = false;
		boolean bss = false;

		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

		if (activeChar.isPlayer())
        {
			if (weaponInst == null)
			{
				activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_S2).addString("You must equip one weapon before cast spell."));
				return;
			}
		}

		if (weaponInst != null)
        {
			if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				bss = true;
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
	        else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				ss = true;
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
		}
        // If there is no weapon equipped, check for an active summon.
        else if (activeChar.isL2Summon())
        {
            L2Summon activeSummon = (L2Summon)activeChar;

            if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
            {
                bss = true;
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }
            else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
            {
                ss = true;
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }
        }

		for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = (L2Character) n.getValue();
			if (target.isAlikeDead())
				continue;

			boolean charged = true;
			if (!_seedAny){
				for (int i=0;i<_seeds.length;i++){
					if (_seeds[i]!=0){
						L2Effect e = target.getFirstEffect(_seeds[i]);
						if (e==null || !e.getInUse()){
							charged = false;
							break;
						}
					}
				}
			}
			else {
				charged = false;
				for (int i=0;i<_seeds.length;i++){
					if (_seeds[i]!=0){
						L2Effect e = target.getFirstEffect(_seeds[i]);
						if (e!=null && e.getInUse()){
							charged = true;
							break;
						}
					}
				}
			}
			if (!charged)
			{
				activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_S2).addString("Target is not charged by elements."));
				continue;
			}

			boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this));

			int damage = (int)Formulas.calcMagicDam(
					activeChar, target, this, ss, bss, mcrit);

			if (damage > 0)
			{
				target.reduceCurrentHp(damage, activeChar);

	            // Manage attack or cast break of the target (calculating rate, sending message...)
	            if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
	            {
	                target.breakAttack();
	                target.breakCast();
	            }

            	activeChar.sendDamageMessage(target, damage, false, false, false);

			}

			// activate attacked effects, if any
			target.stopSkillEffects(getId());
            getEffects(activeChar, target);
		}
	}
}
