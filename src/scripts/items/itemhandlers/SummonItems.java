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
/**
 *
 * @author FBIagent
 *
 */
package scripts.items.itemhandlers;

import javolution.util.FastTable;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.FakePlayersTablePlus;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SummonItemsData;
import scripts.items.IItemHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.Ride;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class SummonItems implements IItemHandler {

    @Override
    public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl) {
        if (!(playable.isPlayer())) {
            return;
        }

        if (!TvTEvent.onItemSummon(playable.getName())) {
            return;
        }

        L2PcInstance activeChar = (L2PcInstance) playable;

        if (activeChar.isSitting()) {
            activeChar.sendPacket(Static.CANT_MOVE_SITTING);
            return;
        }

        if (activeChar.inObserverMode()) {
            return;
        }

        if (activeChar.isInOlympiadMode()) {
            activeChar.sendPacket(Static.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
            return;
        }

        L2SummonItem sitem = SummonItemsData.getInstance().getSummonItem(item.getItemId());

        if (sitem.getType() != 3 && (activeChar.getPet() != null || activeChar.isMounted()) && sitem.isPetSummon()) {
            activeChar.sendPacket(Static.YOU_ALREADY_HAVE_A_PET);
            return;
        }

        if (activeChar.isAttackingNow()) {
            activeChar.sendPacket(Static.YOU_CANNOT_SUMMON_IN_COMBAT);
            return;
        }

        if (activeChar.isCursedWeaponEquiped() && sitem.isPetSummon()) {
            activeChar.sendPacket(Static.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
            return;
        }

        if (!activeChar.canSummonStrider()) {
            activeChar.sendMessage("Страйдер еще не готов.");
            return;
        }

        int npcID = sitem.getNpcId();

        if (npcID == 0) {
            return;
        }

        L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(npcID);
        if (sitem.getType() != 3 && npcTemplate == null) {
            return;
        }

        switch (sitem.getType()) {
            case 0: // static summons (like christmas tree)
                try {
                    L2Spawn spawn = new L2Spawn(npcTemplate);

                    if (spawn == null) {
                        return;
                    }

                    spawn.setId(IdFactory.getInstance().getNextId());
                    spawn.setLocx(activeChar.getX());
                    spawn.setLocy(activeChar.getY());
                    spawn.setLocz(activeChar.getZ());
                    L2World.getInstance().storeObject(spawn.spawnOne());
                    activeChar.destroyItem("Summon", item.getObjectId(), 1, null, false);
                    //activeChar.sendMessage("Created " + npcTemplate.name + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz());
                } catch (Exception e) {
                    activeChar.sendMessage("Target is not ingame.");
                }

                break;
            case 1: // pet summons
                L2Skill skill = SkillTable.getInstance().getInfo(2046, 1);
                if (skill.checkCondition(activeChar, activeChar, false)) {
                    activeChar.setPetSummon(item);
                    activeChar.useMagic(skill, false, false);
                } else {
                    activeChar.sendActionFailed();
                }
                break;
            case 2: // wyvern
                if (!activeChar.disarmWeapons()) {
                    return;
                }

                if (activeChar.getPvpFlag() != 0 || activeChar.isInPVPArena()) {
                    return;
                }

                Ride mount = new Ride(activeChar.getObjectId(), Ride.ACTION_MOUNT, sitem.getNpcId());
                activeChar.broadcastPacket(mount);
                activeChar.setMountType(mount.getMountType());
                activeChar.setMountObjectID(item.getObjectId());
                break;
            case 3:

                if (activeChar.getPartner() != null) {
                    activeChar.sendPacket(Static.ALREADY_HAVE_PARTNER);
                    return;
                }

                L2PcInstance partner = L2PcInstance.restoreFake(IdFactory.getInstance().getNextId(), npcID, true);
                if (partner == null) {
                    break;
                }

                partner.setName("*" + activeChar.getName() + "*");
                partner.setTitle("");
                partner.setOwner(activeChar);
                partner.setXYZInvisible(activeChar.getX(), activeChar.getY(), activeChar.getZ());
                partner.setFakeLoc(activeChar.getX(), activeChar.getY(), activeChar.getZ());

                switch (npcID) {
                    case 92:
                    case 102:
                    case 109:
                        partner.setPartnerClass(1);
                        FakePlayersTablePlus.getInstance().wearArcher(partner);
                        break;
                    default:
                        FakePlayersTablePlus.getInstance().wearFantom(partner);
                        break;
                }

                //L2PetInstance partner = L2PetInstance.spawnPet(npcTemplate, activeChar, item);

                partner.setTitle(activeChar.getName());

                //if (!partner.isRespawned()) {
                partner.setCurrentHp(partner.getMaxHp());
                partner.setCurrentMp(partner.getMaxMp());
                //partner.getStat().setExp(partner.getExpForThisLevel());
                //partner.setCurrentFed(partner.getMaxFed());
                //}

                partner.setRunning();

                /*
                 * if (!partner.isRespawned()) { partner.store(); }
                 */

                activeChar.setPartner(partner);

                activeChar.sendPacket(new MagicSkillUser(activeChar, 2046, 1, 1000, 600000));
                L2World.getInstance().storeObject(partner);
                partner.spawnMe(activeChar.getX() + 50, activeChar.getY() + 100, activeChar.getZ());
                //partner.startFeed(false);
                item.setEnchantLevel(partner.getLevel());

                ThreadPoolManager.getInstance().scheduleAi(new PartnerSummonFinalizer(activeChar, partner), 900, true);

                /*
                 * if (partner.getCurrentFed() <= 0) {
                 * ThreadPoolManager.getInstance().scheduleAi(new
                 * PetSummonFeedWait(activeChar, partner), 60000, true); } else
                 * { partner.startFeed(false); }
                 */
                break;
        }
    }

    static class PartnerSummonFinalizer implements Runnable {

        private L2PcInstance _activeChar;
        private L2PcInstance _partner;

        private PartnerSummonFinalizer(L2PcInstance activeChar, L2PcInstance partner) {
            _activeChar = activeChar;
            _partner = partner;
        }

        @Override
        public void run() {
            try {
                _activeChar.sendPacket(new MagicSkillLaunched(_activeChar, 2046, 1));
                _partner.setFollowStatus(true);
                _partner.setShowSummonAnimation(false);

                _partner.setCurrentHp(_partner.getMaxHp());
                _partner.setCurrentCp(_partner.getMaxCp());
                _partner.setCurrentMp(_partner.getMaxMp());
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public int[] getItemIds() {
        return SummonItemsData.getInstance().itemIDs();
    }
}
