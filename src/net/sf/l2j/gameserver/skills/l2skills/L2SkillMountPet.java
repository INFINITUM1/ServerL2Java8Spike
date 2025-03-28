package net.sf.l2j.gameserver.skills.l2skills;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.Ride;
import net.sf.l2j.gameserver.templates.StatsSet;

public final class L2SkillMountPet extends L2Skill {

    public L2SkillMountPet(StatsSet set) {
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

        L2PcInstance player = caster.getPlayer();
        L2Summon pet = player.getPet();
        if (pet == null) {
            return;
        }

        player.stopSkillEffects(player.getActiveAug());
        player.broadcastUserInfo();
        Ride mount = new Ride(player.getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().npcId);
        player.broadcastPacket(mount);
        player.setMountType(mount.getMountType());
        player.setMountObjectID(pet.getControlItemId());
        pet.unSummon(player);
        player.startUnmountPet(Config.MOUNT_EXPIRE);
    }
}
