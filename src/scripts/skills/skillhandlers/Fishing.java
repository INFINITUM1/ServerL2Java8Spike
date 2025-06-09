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
package scripts.skills.skillhandlers;

import net.sf.l2j.Config;
import scripts.skills.ISkillHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;
import scripts.zone.type.L2WaterZone;
import javolution.util.FastList;
import net.sf.l2j.gameserver.GeoData;

public class Fishing implements ISkillHandler {
    //private static Logger _log = Logger.getLogger(SiegeFlag.class.getName());
    //protected SkillType[] _skillIds = {SkillType.FISHING};

    private static final SkillType[] SKILL_IDS = {SkillType.FISHING};

    @Override
    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (activeChar == null || !(activeChar.isPlayer())) {
            return;
        }

        L2PcInstance player = (L2PcInstance) activeChar;

        /* If fishing is disabled, there isn't much point in doing anything else, unless you are GM.
         * so this got moved up here, before anything else.
         */
        if (!Config.ALLOWFISHING && !player.isGM()) {
            player.sendMessage("Рыбалка отключена на сервере");
            return;
        }

        if (player.isFishing()) {
            if (player.GetFishCombat() != null) {
                player.GetFishCombat().doDie(false);
            } else {
                player.EndFishing(false);
            }
            //Cancels fishing
            player.sendPacket(Static.FISHING_ATTEMPT_CANCELLED);
            return;
        }
        L2Weapon weaponItem = player.getActiveWeaponItem();
        if ((weaponItem == null || weaponItem.getItemType() != L2WeaponType.ROD)) {
            //Fishing poles are not installed
            player.sendPacket(Static.FISHING_POLE_NOT_EQUIPPED);
            return;
        }
        L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
        if (lure == null) {
            //Bait not equiped.
            player.sendPacket(Static.BAIT_ON_HOOK_BEFORE_FISHING);
            return;
        }
        player.SetLure(lure);
        L2ItemInstance lure2 = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

        if (lure2 == null || lure2.getCount() < 1) //Not enough bait.
        {
            player.sendPacket(Static.NOT_ENOUGH_BAIT);
        }

        if (player.isInBoat()) {
            //You can't fish while you are on boat
            player.sendPacket(Static.CANNOT_FISH_ON_BOAT);
            if (!player.isGM()) {
                return;
            }
        }
        if (player.isInCraftMode() || player.isInStoreMode()) {
            player.sendPacket(Static.CANNOT_FISH_WHILE_USING_RECIPE_BOOK);
            if (!player.isGM()) {
                return;
            }
        }

        if (!player.isInFishZone()) {
            player.sendPacket(Static.CANNOT_FISH_HERE);
            return;
        }

        /*
         * If fishing is enabled, here is the code that was striped from
         * startFishing() in L2PcInstance. Decide now where will the hook be
         * cast...
         */
        int rnd = Rnd.get(150) + 50;
        double angle = Util.convertHeadingToDegree(player.getHeading());
        double radian = Math.toRadians(angle);
        double sin = Math.sin(radian);
        double cos = Math.cos(radian);
        int x = player.getX() + (int) (cos * rnd);
        int y = player.getY() + (int) (sin * rnd);
        int z = player.getZ() - 30;

        L2WaterZone water = ZoneManager.getInstance().getWaterZone(x, y, z, null);
        if (water == null) {
            player.sendPacket(Static.CANNOT_FISH_BEHIND);
            return;
        }
        // if water zone exist using it, if not - using fishing zone
        z = water.getWaterZ() + 10;
        // Has enough bait, consume 1 and update inventory. Start fishing
        // follows.
        lure2 = player.getInventory().destroyItem("Consume", player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, player, null);
        InventoryUpdate iu = new InventoryUpdate();
        iu.addModifiedItem(lure2);
        player.sendPacket(iu);
        // If everything else checks out, actually cast the hook and start
        // fishing... :P
        player.startFishing(x, y, z);
        player.sendSkillList();
    }

    @Override
    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
