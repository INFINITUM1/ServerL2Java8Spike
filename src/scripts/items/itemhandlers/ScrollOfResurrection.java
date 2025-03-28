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
package scripts.items.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import scripts.items.IItemHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.7 $ $Date: 2005/04/05 19:41:13 $
 */
public class ScrollOfResurrection implements IItemHandler {
    // all the items ids that this handler knows

    private static final int[] ITEM_IDS = {737, 3936, 3959, 6387};

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    @Override
    public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl) {
        if (!(playable.isPlayer())) {
            return;
        }

        L2PcInstance activeChar = (L2PcInstance) playable;
        if (activeChar.isSitting()) {
            activeChar.sendPacket(Static.CANT_MOVE_SITTING);
            return;
        }
        if (activeChar.isHippy() || activeChar.isMovementDisabled()) {
            return;
        }
        if (Config.PVP_ZONE_REWARDS && ZoneManager.getInstance().isResDisabled(activeChar)) {
            activeChar.sendPacket(Static.RES_DISABLED);
            return;
        }

        L2Object trg = activeChar.getTarget();
        // SoR Animation section
        if (trg != null && trg.isL2Character()) {
            L2Character target = (L2Character) trg;
            if (!target.isDead()) {
                activeChar.sendPacket(Static.TARGET_IS_INCORRECT);
                return;
            }

            if (!activeChar.isInsideRadius(target, 600, false, false)) {
                activeChar.sendPacket(Static.TARGET_TOO_FAR);
                return;
            }

            int itemId = item.getItemId();
            //boolean blessedScroll = (itemId != 737);
            boolean humanScroll = (itemId == 3936 || itemId == 3959 || itemId == 737);
            boolean petScroll = (itemId == 6387 || itemId == 737);

            L2PcInstance targetPlayer = null;
            if (target.isPlayer()) {
                targetPlayer = (L2PcInstance) target;
            }

            L2PetInstance targetPet = null;
            if (target.isPet()) {
                targetPet = (L2PetInstance) target;
            }

            if (targetPlayer != null || targetPet != null) {
                //check target is not in a active siege zone
                Castle castle = null;
                if (targetPlayer != null) {
                    castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                } else {
                    castle = CastleManager.getInstance().getCastle(targetPet.getX(), targetPet.getY(), targetPet.getZ());
                }

                if (castle != null && castle.getSiege().getIsInProgress()) {
                    activeChar.sendPacket(Static.CANNOT_BE_RESURRECTED_DURING_SIEGE);
                    return;
                }

                if (targetPet != null) {
                    if (targetPet.getOwner() != activeChar) {
                        if (targetPet.getOwner().isReviveRequested()) {
                            if (targetPet.getOwner().isRevivingPet()) {
                                activeChar.sendPacket(Static.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
                            } else {
                                activeChar.sendPacket(Static.PET_CANNOT_RES); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
                            }
                            return;
                        }
                    } else if (!petScroll) {
                        activeChar.sendMessage("You do not have the correct scroll");
                        return;
                    }
                } else if (targetPlayer != null) {
                    if (targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a festival.
                    {
                        activeChar.sendPacket(SystemMessage.sendString("You may not resurrect participants in a festival."));
                        return;
                    }
                    if (targetPlayer.isReviveRequested()) {
                        if (targetPlayer.isRevivingPet()) {
                            activeChar.sendPacket(Static.MASTER_CANNOT_RES); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
                        } else {
                            activeChar.sendPacket(Static.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
                        }
                        return;
                    } else if (!humanScroll) {
                        activeChar.sendMessage("You do not have the correct scroll");
                        return;
                    }
                }

                if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false)) {
                    return;
                }

                int skillId = 0;
                // int skillLevel = 1;

                switch (itemId) {
                    case 737:
                        skillId = 2014;
                        break; // Scroll of Resurrection
                    case 3936:
                        skillId = 2049;
                        break; // Blessed Scroll of Resurrection
                    case 3959:
                        skillId = 2062;
                        break; // L2Day - Blessed Scroll of Resurrection
                    case 6387:
                        skillId = 2179;
                        break; // Blessed Scroll of Resurrection: For Pets
                }

                if (skillId != 0) {
                    activeChar.doCast(SkillTable.getInstance().getInfo(skillId, 1));
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_DISAPPEARED).addItemName(itemId));
                }
            }
        } else {
            activeChar.sendPacket(Static.TARGET_IS_INCORRECT);
        }
    }

    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
