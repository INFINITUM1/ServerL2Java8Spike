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

import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.network.serverpackets.ChairSit;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopManageList;
import net.sf.l2j.gameserver.network.serverpackets.Ride;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.PeaceZone;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.7.2.9 $ $Date: 2005/04/06 16:13:48 $
 */
public final class RequestActionUse extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(RequestActionUse.class.getName());
    private int _actionId;
    private boolean _ctrlPressed;
    private boolean _shiftPressed;

    @Override
    protected void readImpl() {
        _actionId = readD();
        _ctrlPressed = (readD() == 1);
        _shiftPressed = (readC() == 1);
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();

        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPAN() < 200) {
            player.sendActionFailed();
            return;
        }

        player.sCPAN();

        //if (Config.DEBUG)
        //_log.finest(player.getName()+" request Action use: id "+_actionId + " 2:" + _ctrlPressed + " 3:"+_shiftPressed);

        // dont do anything if player is dead
        if (player.isAlikeDead()) {
            player.sendActionFailed();
            return;
        }

        // don't do anything if player is confused
        if (player.isOutOfControl() || player.isParalyzed()) {
            player.sendActionFailed();
            return;
        }

        // don't do anything if player is casting 
        if (player.isCastingNow()) {
            player.sendActionFailed();
            return;
        }

        L2Summon pet = player.getPet();
        L2Object target = player.getTarget();

        //if (Config.DEBUG)
        //_log.info("Requested Action ID: " + String.valueOf(_actionId));

        switch (_actionId) {
            case 0:
                if (player.getMountType() != 0) {
                    break;
                }

                if (target != null
                        && !player.isSitting()
                        && target instanceof L2StaticObjectInstance
                        && ((L2StaticObjectInstance) target).getType() == 1
                        && CastleManager.getInstance().getCastle(target) != null
                        && player.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false)) {
                    //ChairSit cs = new ChairSit(player,((L2StaticObjectInstance)target).getStaticObjectId());
                    //player.sendPacket(cs);
                    player.sitDown();
                    player.broadcastPacket(new ChairSit(player, ((L2StaticObjectInstance) target).getStaticObjectId()));
                    break;
                }

                if (player.isSitting()) {
                    player.standUp();
                } else {
                    player.sitDown();
                }

                //if (Config.DEBUG)
                //_log.fine("new wait type: "+(player.isSitting() ? "SITTING" : "STANDING"));

                break;
            case 1:
                if (player.isRunning()) {
                    player.setWalking();
                } else {
                    player.setRunning();
                }

                //if (Config.DEBUG)
                //_log.fine("new move type: "+(player.isRunning() ? "RUNNING" : "WALKIN"));
                break;
            case 15:
            case 21: // pet follow/stop
                if (pet != null && !pet.isMovementDisabled() && !player.isBetrayed()) {
                    pet.setFollowStatus(!pet.getFollowStatus());
                }

                break;
            case 16:
            case 22: // pet attack
                if (target == null || pet == null || pet == target || pet.isDead()) {
                    player.sendActionFailed();
                    return;
                }

                if (player.isBetrayed() || PeaceZone.getInstance().inPeace(player, target)) {
                    //player.sendPacket(SystemMessage.id(SystemMessageId.TARGET_IN_PEACEZONE));
                    player.sendActionFailed();
                    return;
                }

                if (player.isInOlympiadMode() && !player.isOlympiadCompStart()) {
                    player.sendActionFailed();
                    return;
                }

                if (target.isL2Door() && !target.isAutoAttackable(pet)) {
                    player.sendActionFailed();
                    return;
                }

                if (!_ctrlPressed && !target.isAutoAttackable(pet)) {
                    player.sendActionFailed();
                    return;
                }

                pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
                break;
            case 17:
            case 23: // pet - cancel action
                if (pet != null/* && !pet.isMovementDisabled() */ && !player.isBetrayed()) {
                    pet.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
                }

                break;
            case 19: // pet unsummon
                if (pet != null && !player.isBetrayed()) {
                    //returns pet to control item
                    if (pet.isDead()) {
                        player.sendPacket(Static.DEAD_PET_CANNOT_BE_RETURNED);
                    } else if (pet.isAttackingNow() || pet.isRooted()) {
                        player.sendPacket(Static.PET_CANNOT_SENT_BACK_DURING_BATTLE);
                    } else {
                        // if it is a pet and not a summon
                        if (pet.isPet()) {
                            L2PetInstance petInst = (L2PetInstance) pet;

                            // if the pet is more than 40% fed
                            if (petInst.getCurrentFed() > (petInst.getMaxFed() * 0.40)) {
                                pet.unSummon(player);
                            } else {
                                player.sendPacket(Static.YOU_CANNOT_RESTORE_HUNGRY_PETS);
                            }
                        }
                    }
                }
                break;
            case 38: // pet mount
                player.tryMountPet(pet);
                break;
            case 32: // Wild Hog Cannon - Mode Change
                useSkill(4230);
                break;
            case 36: // Soulless - Toxic Smoke
                useSkill(4259);
                break;
            case 37:
                if (player.isAlikeDead()) {
                    player.sendActionFailed();
                    return;
                }
                if (player.getPrivateStoreType() != 0) {
                    player.setPrivateStoreType(L2PcInstance.PS_NONE);
                    player.broadcastUserInfo();
                }
                if (player.isSitting()) {
                    player.standUp();
                }

                if (player.getCreateList() == null) {
                    player.setCreateList(new L2ManufactureList());
                }

                player.sendPacket(new RecipeShopManageList(player, true));
                break;
            case 39: // Soulless - Parasite Burst
                useSkill(4138);
                break;
            case 41: // Wild Hog Cannon - Attack
                useSkill(4230);
                break;
            case 42: // Kai the Cat - Self Damage Shield
                useSkill(4378, player);
                break;
            case 43: // Unicorn Merrow - Hydro Screw
                useSkill(4137);
                break;
            case 44: // Big Boom - Boom Attack
                useSkill(4139);
                break;
            case 45: // Unicorn Boxer - Master Recharge
                useSkill(4025, player);
                break;
            case 46: // Mew the Cat - Mega Storm Strike
                useSkill(4261);
                break;
            case 47: // Silhouette - Steal Blood
                useSkill(4260);
                break;
            case 48: // Mechanic Golem - Mech. Cannon
                useSkill(4068);
                break;
            case 51:
                // Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
                if (player.isAlikeDead()) {
                    player.sendActionFailed();
                    return;
                }
                if (player.getPrivateStoreType() != 0) {
                    player.setPrivateStoreType(L2PcInstance.PS_NONE);
                    player.broadcastUserInfo();
                }
                if (player.isSitting()) {
                    player.standUp();
                }

                if (player.getCreateList() == null) {
                    player.setCreateList(new L2ManufactureList());
                }

                player.sendPacket(new RecipeShopManageList(player, false));
                break;
            case 52: // unsummon
                if (pet != null && pet.isSummon()) {
                    pet.unSummon(player);
                }
                break;
            case 53: // move to target
                if (target != null && pet != null && pet != target && !pet.isMovementDisabled()) {
                    pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), pet.calcHeading(target.getX(), target.getY())));
                }
                break;
            case 54: // move to target hatch/strider
                if (target != null && pet != null && pet != target && !pet.isMovementDisabled()) {
                    pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), pet.calcHeading(target.getX(), target.getY())));
                }
                break;
            case 96: // Quit Party Command Channel
                _log.info("98 Accessed");
                break;
            case 97: // Request Party Command Channel Info
                //if (!PartyCommandManager.getInstance().isPlayerInChannel(player))
                //return;
                _log.info("97 Accessed");
                //PartyCommandManager.getInstance().getActiveChannelInfo(player);
                break;
            case 1000: // Siege Golem - Siege Hammer
                if (target.isL2Door()) {
                    useSkill(4079);
                }
                break;
            case 1001:
                break;
            case 1003: // Wind Hatchling/Strider - Wild Stun
                useSkill(4710); //TODO use correct skill lvl based on pet lvl
                break;
            case 1004: // Wind Hatchling/Strider - Wild Defense
                useSkill(4711, player); //TODO use correct skill lvl based on pet lvl
                break;
            case 1005: // Star Hatchling/Strider - Bright Burst
                useSkill(4712); //TODO use correct skill lvl based on pet lvl
                break;
            case 1006: // Star Hatchling/Strider - Bright Heal
                useSkill(4713, player); //TODO use correct skill lvl based on pet lvl
                break;
            case 1007: // Cat Queen - Blessing of Queen
                useSkill(4699, player);
                break;
            case 1008: // Cat Queen - Gift of Queen
                useSkill(4700, player);
                break;
            case 1009: // Cat Queen - Cure of Queen
                useSkill(4701);
                break;
            case 1010: // Unicorn Seraphim - Blessing of Seraphim
                useSkill(4702, player);
                break;
            case 1011: // Unicorn Seraphim - Gift of Seraphim
                useSkill(4703, player);
                break;
            case 1012: // Unicorn Seraphim - Cure of Seraphim
                useSkill(4704);
                break;
            case 1013: // Nightshade - Curse of Shade
                useSkill(4705);
                break;
            case 1014: // Nightshade - Mass Curse of Shade
                useSkill(4706, player);
                break;
            case 1015: // Nightshade - Shade Sacrifice
                useSkill(4707);
                break;
            case 1016: // Cursed Man - Cursed Blow
                useSkill(4709);
                break;
            case 1017: // Cursed Man - Cursed Strike/Stun
                useSkill(4708);
                break;
            case 1031: // Feline King - Slash
                useSkill(5135);
                break;
            case 1032: // Feline King - Spinning Slash
                useSkill(5136);
                break;
            case 1033: // Feline King - Grip of the Cat
                useSkill(5137);
                break;
            case 1034: // Magnus the Unicorn - Whiplash
                useSkill(5138);
                break;
            case 1035: // Magnus the Unicorn - Tridal Wave
                useSkill(5139);
                break;
            case 1036: // Spectral Lord - Corpse Kaboom
                useSkill(5142);
                break;
            case 1037: // Spectral Lord - Dicing Death
                useSkill(5141);
                break;
            case 1038: // Spectral Lord - Force Curse
                useSkill(5140);
                break;
            case 1039: // Swoop Cannon - Cannon Fodder
                if (!(target.isL2Door())) {
                    useSkill(5110);
                }
                break;
            case 1040: // Swoop Cannon - Big Bang
                if (!(target.isL2Door())) {
                    useSkill(5111);
                }
                break;
            case 61:
                player.sendMessage("Не работает");
                break;
            case 63:
                player.sendMessage("Не работает");
                break;
            case 64:
                player.sendMessage("Не работает");
                break;
            default:
                if (!isPetSkillAction(Config.PETS_ACTION_SKILLS.get(_actionId))) {
                    _log.warning(player.getName() + ": unhandled action type " + _actionId);
                }
                //_log.warning(player.getName() + ": unhandled action type " + _actionId);
                break;
        }
    }

    private boolean isPetSkillAction(Integer skillId) {
        if (skillId == null) {
            return false;
        }

        useSkill(skillId);
        return true;
    }

    /*
     * Cast a skill for active pet/servitor. Target is specified as a parameter
     * but can be overwrited or ignored depending on skill type.
     */
    private void useSkill(int skillId, L2Object target) {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        L2Summon activeSummon = player.getPet();

        if (player.getPrivateStoreType() != 0) {
            player.sendMessage("Cannot use skills while trading");
            return;
        }

        if (activeSummon != null && !player.isBetrayed()) {
            Map<Integer, L2Skill> _skills = activeSummon.getTemplate().getSkills();

            if (_skills == null) {
                return;
            }

            if (_skills.isEmpty()) {
                player.sendPacket(Static.SKILL_NOT_AVAILABLE);
                return;
            }

            L2Skill skill = _skills.get(skillId);

            if (skill == null) {
                //if (Config.DEBUG)
                //_log.warning("Skill " + skillId + " missing from npcskills.sql for a summon id " + activeSummon.getNpcId());
                return;
            }

            activeSummon.setTarget(target);
            activeSummon.useMagic(skill, _ctrlPressed, _shiftPressed);
        }
    }

    /*
     * Cast a skill for active pet/servitor. Target is retrieved from owner'
     * target, then validated by overloaded method useSkill(int, L2Character).
     */
    private void useSkill(int skillId) {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        useSkill(skillId, player.getTarget());
    }
}
