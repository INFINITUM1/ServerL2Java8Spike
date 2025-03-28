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

import java.util.logging.Logger;

import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import javolution.util.FastList;

/**
 *
 * @author  Julian
 */
public class DeluxeKey implements ISkillHandler
{
    private static Logger _log = Logger.getLogger(BeastFeed.class.getName());
    private static final SkillType[] SKILL_IDS = {SkillType.DELUXE_KEY_UNLOCK};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
    {
        if (!(activeChar.isPlayer()))
			return;

		FastList<L2Object> targetList = skill.getTargetList(activeChar);

        if (targetList.isEmpty())
            return;

        _log.fine("Delux key casting succeded.");

        // This is just a dummy skill handler for the golden food and crystal food skills,
        // since the AI responce onSkillUse handles the rest.
		//targets.clear();

    }

    public SkillType[] getSkillIds()
    {
        return SKILL_IDS;
    }
}
