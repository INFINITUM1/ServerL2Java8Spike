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
package scripts.skills.skillhandlers;

import java.util.logging.Logger;

import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import javolution.util.FastList;
import javolution.util.FastList.Node;
/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.9 $ $Date: 2005/04/04 19:08:01 $
 */

public class Charge implements ISkillHandler
{
	static Logger _log = Logger.getLogger(Charge.class.getName());

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
	 */
	private static final SkillType[] SKILL_IDS = {/*SkillType.CHARGE*/};

	public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
	{
		for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character ctarget = (L2Character) n.getValue();
        	if (!(ctarget.isPlayer()))
        		continue;
			L2PcInstance target = (L2PcInstance)ctarget;
        	skill.getEffects(activeChar, target);
		}
        // self Effect :]
        L2Effect effect = activeChar.getFirstEffect(skill.getId());
        if (effect != null && effect.isSelfEffect())
        {
        	//Replace old effect with new one.
        	effect.exit();
        }
        skill.getEffectsSelf(activeChar);
		//targets.clear();
	}

	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
