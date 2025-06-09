package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2EffectPointInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Location;
import javolution.util.FastList;
import scripts.ai.XmasTree;

public final class L2SkillSummonNpc extends L2Skill {

    public int _effectNpcId;
    public int _effectId;

    public L2SkillSummonNpc(StatsSet set) {
        super(set);
        _effectNpcId = set.getInteger("effectNpcId", -1);
        _effectId = set.getInteger("effectId", -1);
    }

    @Override
    public void useSkill(L2Character caster, FastList<L2Object> targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        if (caster.isPlayer()) {
            L2PcInstance pc = caster.getPlayer();
            L2NpcTemplate template = NpcTable.getInstance().getTemplate(_effectNpcId);
            if (isSpellForceSkill() || isBattleForceSkill()) {
                L2EffectPointInstance effectPoint = new L2EffectPointInstance(IdFactory.getInstance().getNextId(), template, pc, _effectId, getId());
                effectPoint.setCurrentHp(effectPoint.getMaxHp());
                effectPoint.setCurrentMp(effectPoint.getMaxMp());
                L2World.getInstance().storeObject(effectPoint);

                Location loc = pc.getGroundSkillLoc();
                effectPoint.setIsInvul(true);
                if (loc != null) {
                    effectPoint.spawnMe(loc.x, loc.y, loc.z);
                } else {
                    effectPoint.spawnMe(pc.getX(), pc.getY(), pc.getZ());
                }
                pc.setGroundSkillLoc(null);
            } else {
                if (getId() == 2137 || getId() == 2138) {
                    XmasTree effectPoint = new XmasTree(IdFactory.getInstance().getNextId(), template);
                    effectPoint.setCurrentHp(effectPoint.getMaxHp());
                    effectPoint.setCurrentMp(effectPoint.getMaxMp());
                    L2World.getInstance().storeObject(effectPoint);
                    effectPoint.setIsInvul(true);
                    effectPoint.spawnMe(pc.getX(), pc.getY(), pc.getZ());
                }
            }
        }
    }
}
