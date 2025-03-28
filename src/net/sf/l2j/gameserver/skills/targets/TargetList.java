package net.sf.l2j.gameserver.skills.targets;

import javolution.util.FastList;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;

public abstract class TargetList
{
	public static TargetList create(SkillTargetType targetType)
	{
		switch (targetType)
        {
            // The skill can only be used on the L2Character targeted, or on the caster itself
            case TARGET_ONE:
				return new TargetOne();
            case TARGET_SELF:
            case TARGET_GROUND:
				return new TargetSelf();
            case TARGET_HOLY:
				return new TargetHoly();
            case TARGET_PET:
				return new TargetPet();
			case TARGET_OWNER_PET:
				return new TargetPetOwner();
			case TARGET_CORPSE_PET:
				return new TargetPetCorpse();
            case TARGET_AURA:
				return new TargetAura();
            case TARGET_AREA:
				return new TargetArea();
            case TARGET_MULTIFACE:
				return new TargetMultiface();
			case TARGET_PARTY:
				return new TargetParty();
			case TARGET_PARTY_MEMBER:
				return new TargetPartyMember();
			case TARGET_PARTY_OTHER:
				return new TargetPartyOther();
            case TARGET_ALLY:
				return new TargetAlly();
            case TARGET_CORPSE_ALLY:
				return new TargetAllyCorpse();
            case TARGET_CLAN:
				return new TargetClan();
            case TARGET_CORPSE_CLAN:
				return new TargetClanCorpse();
            case TARGET_CORPSE_PLAYER:
				return new TargetCorpsePlayer();
            case TARGET_MOB:
				return new TargetMob();
            case TARGET_CORPSE_MOB:
				return new TargetMobCorpse();
            case TARGET_AREA_CORPSE_MOB:
				return new TargetMobCorpseArea();
            case TARGET_UNLOCKABLE:
				return new TargetUnlockable();
            case TARGET_ITEM:
				return new TargetItem();
            case TARGET_UNDEAD:
				return new TargetUndead();
			case TARGET_TYRANNOSAURUS:
				return new TargetTyrannosaurus();
			case TARGET_AREA_ANGEL:
				return new TargetAngelArea();
            case TARGET_AREA_UNDEAD:
				return new TargetUndeadArea();
            case TARGET_ENEMY_SUMMON:
				return new TargetEnemySummon();
            default:
				return new TargetDefault();
        }
	}
	
	public abstract FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill);
}