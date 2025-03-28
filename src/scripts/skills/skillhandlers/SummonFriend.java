package scripts.skills.skillhandlers;

import javolution.util.FastList;
import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;

public class SummonFriend implements ISkillHandler
{
	//private static Logger _log = Logger.getLogger(SummonFriend.class.getName());
	private static final SkillType[] SKILL_IDS = {SkillType.SUMMON_FRIEND};

 	public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
	{
 		if (activeChar == null || !(activeChar.isPlayer())) 
			return;

 		L2PcInstance caster = (L2PcInstance) activeChar;
		if (!caster.canSummon())
			return;

		ConfirmDlg dialog = new ConfirmDlg(1842, caster.getName());
		dialog.addLoc(new Location(caster.getX(), caster.getY(), caster.getZ()));

		for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Object object = n.getValue();
			if (object == null || !(object.isPlayer()))
				continue;

			L2PcInstance target = (L2PcInstance) object;
			if (target == caster) 
				continue;

            if (!target.canBeSummoned(caster))
				continue;

			target.sendSfRequest(caster, dialog);
            //target.teleToLocation(caster.getX()+Rnd.get(50), caster.getY()+Rnd.get(50), caster.getZ(), true);
		}
		dialog.clearPoints();
		dialog = null;
 	}

	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
