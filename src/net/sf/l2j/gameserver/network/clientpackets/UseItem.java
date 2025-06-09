package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.ShowCalculator;
import net.sf.l2j.gameserver.templates.L2ArmorType;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import scripts.items.ItemHandler;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public final class UseItem extends L2GameClientPacket {

    private int _objectId;
    private int _ctrlClick;

    @Override
    protected void readImpl() {
        _objectId = readD();
        _ctrlClick = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
        if (item == null) {
            return;
        }

        if (item.getItemId() == 57 || player.isParalyzed()) {
            return;
        }
        int itemId = item.getItemId();

        //if((itemId != 5591 && itemId != 5592) && (System.currentTimeMillis() - player.gCPBA() < 200))
        //	return;
        if (!item.isEquipable()) {
            if (System.currentTimeMillis() - player.gCPBA() < 200) {
                return;
            }
            player.sCPBA();
        }

        if (player.isStunned() || player.isSleeping() || player.isAfraid() || player.isFakeDeath()) {
            return;
        }

        if (player.isDead() || player.isAlikeDead()) {
            return;
        }

        if (player.getPrivateStoreType() != 0) {
            player.sendPacket(Static.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
            return;
        }

        if (player.getActiveTradeList() != null) {
            player.cancelActiveTrade();
            return;
        }

        if (player.getActiveWarehouse() != null) {
            player.cancelActiveWarehouse();
            return;
        }

        if (player.getActiveEnchantItem() != null) {
            player.setActiveEnchantItem(null);
            player.sendPacket(new EnchantResult(0, true));
            return;
        }

        synchronized (player.getInventory()) {
            // Items that cannot be used

            if (player.isFishing() && (itemId < 6535 || itemId > 6540)) {
                // You cannot do anything else while fishing
                player.sendPacket(Static.CANNOT_DO_WHILE_FISHING_3);
                player.sendActionFailed();
                return;
            }

            // Char cannot use pet items
            if (item.getItem().isForWolf() || item.getItem().isForHatchling() || item.getItem().isForStrider() || item.getItem().isForBabyPet()) {
                player.sendPacket(Static.CANNOT_EQUIP_PET_ITEM);
                player.sendActionFailed();
                return;
            }



            if (item.getItemType() == L2ArmorType.HEAVY)
            {
                if (Config.HEAVY_PENALTY.contains(player.getClassId().getId()))
                {
                    {
                        player.sendPacket(Static.FORBIDDEN_HEAVY_CLASS);
                        player.sendActionFailed();
                        return;
                    }
                }
            }

            if (item.getItemType() == L2ArmorType.LIGHT)
            {
                if (Config.LIGHT_PENALTY.contains(player.getClassId().getId()))
                {
                    {
                        player.sendPacket(Static.FORBIDDEN_LIGHT_CLASS);
                        player.sendActionFailed();
                        return;
                    }
                }
            }

            if (item.getItemType() == L2ArmorType.MAGIC)
            {
                if (Config.MAGIC_PENALTY.contains(player.getClassId().getId()))
                {
                    {
                        player.sendPacket(Static.FORBIDDEN_MAGIC_CLASS);
                        player.sendActionFailed();
                        return;
                    }
                }
            }

            if (item.getItemType() == L2WeaponType.NONE)
            {
                if (Config.SHIELD_PENALTY.contains(player.getClassId().getId()))
                {
                    {
                        player.sendPacket(Static.FORBIDDEN_SHIELD_CLASS);
                        player.sendActionFailed();
                        return;
                    }
                }
            }

            if (item.isEquipable()) {
                //int bodyPart = item.getItem().getBodyPart();
                //System.out.println(bodyPart);

                if ((player.getChannel() == 8 && Config.TVT_CUSTOM_ITEMS)
                        || (player.getChannel() == 6 && Config.LH_CUSTOM_ITEMS
                        || (player.getChannel() == 4 && Config.EBC_CUSTOM_ITEMS))) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (item.getTimeOfUse() > 0) {
                    player.sendActionFailed();
                    return;
                }

                // Don't allow weapon/shield hero equipment during Olympiads
                if ((player.isInOlympiadMode() || (Config.FORBIDDEN_EVENT_ITMES && player.isInEventChannel())) && (item.isHeroItem() || item.notForOly())) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (player.getChannel() == 67 && item.notForBossZone()) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (item.getItem().isHippy() && player.underAttack()) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (player.isForbidItem(itemId)) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (Config.PREMIUM_ITEMS && item.isPremiumItem() && !player.isPremium()) {
                    player.sendPacket(Static.PREMIUM_ONLY);
                    player.sendActionFailed();
                    return;

                }

                if (item.getItem().isWeapon()) {
                    if (player.isCursedWeaponEquiped() || itemId == 6408) // Don't allow to put formal wear
                    {
                        player.sendActionFailed();
                        return;
                    }
                    player.equipWeapon(item);
                    return;
                }

                player.useEquippableItem(item, true);
                return;
            }

            if (itemId == 4393) {
                player.sendPacket(new ShowCalculator(4393));
                return;
            }

            if (forbidWeapon(player, player.getActiveWeaponItem(), item)) {
                return;
            }

            ItemHandler.getInstance().useItem(player, item, _ctrlClick == 1, null);
            /*IItemHandler handler = ItemHandler.getInstance().getItemHandler(itemId);
            if (handler != null) {
                handler.useItem(player, item);
            }*/
        }
    }

    private boolean forbidWeapon(L2PcInstance player, L2Weapon weaponItem, L2ItemInstance item) {
        if (weaponItem == null) {
            return false;
        }

        if (item.isLure() && weaponItem.getItemType() == L2WeaponType.ROD) {
            player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
            player.sendItems(false);
            player.broadcastUserInfo();
            return true;
        }

        if (item.getItemId() == 8192 && weaponItem.getItemType() != L2WeaponType.BOW) {
            player.sendMessage("Используется с луком");
            return true;
        }
        return false;
    }
}
