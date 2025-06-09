package net.sf.l2j.gameserver.skills.l2skills;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.StatsSet;

public final class L2SkillDrainSoul extends L2Skill {

    public L2SkillDrainSoul(StatsSet set) {
        super(set);
    }

    @Override
    public void useSkill(L2Character caster, FastList<L2Object> targets) {
        if (caster.isAlikeDead()) {
            return;
        }

        if (!caster.isPlayer()) {
            return;
        }

        addAbsorberTo(caster.getTarget(), caster.getPlayer());
    }

    private void addAbsorberTo(L2Object trg, L2PcInstance player) {
        if (player == null || trg == null) {
            return;
        }

        if (!trg.isL2Attackable()) {
            return;
        }

        trg.addAbsorber(player, player.getCoulCryId());
    }
}
