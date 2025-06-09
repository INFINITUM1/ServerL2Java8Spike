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
package net.sf.l2j.gameserver.model;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 *
 * @author Luno
 */
public final class L2ArmorSet {

    private final FastList<Integer> _items = new FastList<>();
    private final FastMap<Integer, FastList<Integer>> _items_special = new FastMap<>();

    //
    private final int _skillId;

    private final int _shield;
    private final int _shieldSkillId;

    private final int _enchant6Skill;

    private final boolean _special;

    public L2ArmorSet(int chest, int legs, int head, int gloves, int feet, int skill_id, int shield, int shield_skill_id, int enchant6skill) {
        _items.add(chest);
        _items.add(legs);
        _items.add(head);
        _items.add(gloves);
        _items.add(feet);

        _skillId = skill_id;

        _shield = shield;
        _shieldSkillId = shield_skill_id;

        _enchant6Skill = enchant6skill;
        _special = false;
    }

    public L2ArmorSet(int chest, FastList<Integer> items, int skill_id, int enchant6skill) {
        _items.add(chest);
        _items.addAll(items);

        _skillId = skill_id;

        _shield = 0;
        _shieldSkillId = 0;

        _enchant6Skill = enchant6skill;
        _special = false;
    }

    public L2ArmorSet(int chest, FastMap<Integer, FastList<Integer>> items, int skill_id, int enchant6skill, boolean special) {
        _items.add(chest);
        _items_special.putAll(items);

        for (FastList<Integer> list : items.values()) {
            if (list == null) {
                continue;
            }

            for (Integer id : list) {
                if (id == null) {
                    continue;
                }
                _items.add(id);
            }
        }

        _skillId = skill_id;

        _shield = 0;
        _shieldSkillId = 0;

        _enchant6Skill = enchant6skill;
        _special = special;
    }

    /**
     * Checks if player have equiped all items from set (not checking shield)
     *
     * @param player whose inventory is being checked
     * @return True if player equips whole set
     */
    public boolean containAll(L2PcInstance player, Inventory inv) {
        if (_special) {
            int found = 0;
            for (FastList<Integer> list : _items_special.values()) {
                if (list == null) {
                    continue;
                }

                for (Integer id : list) {
                    if (found >= 3) {
                        return true;
                    }

                    if (id == null) {
                        continue;
                    }

                    if (inv.isEquippedItem(id, null)) {
                        found++;
                        break;
                    }

                }
            }

            return (found >= 3);
        }

        for (Integer item_id : _items) {
            if (item_id == null
                    || item_id == 0) {
                continue;
            }

            if (!inv.isEquippedItem(item_id, null)) {
                return false;
            }
        }

        return true;
    }

    public boolean containItem(int slot, int itemId) {
        return _items.contains(itemId);
    }

    public int getSkillId() {
        return _skillId;
    }

    public boolean containShield(L2PcInstance player) {
        if (_shield == 0) {
            return false;
        }

        Inventory inv = player.getInventory();

        L2ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
        if (shieldItem != null
                && shieldItem.getItemId() == _shield) {
            return true;
        }

        return false;
    }

    public boolean containShield(int shield_id) {
        if (_shield == 0) {
            return false;
        }

        return _shield == shield_id;
    }

    public int getShieldSkillId() {
        return _shieldSkillId;
    }

    public int getEnchant6skillId() {
        return _enchant6Skill;
    }

    /**
     * Checks if all parts of set are enchanted to +6 or more
     *
     * @param player
     * @return
     */
    public boolean isEnchanted6(L2PcInstance player, Inventory inv) {
        if (_special) {
            int found = 0;
            for (FastList<Integer> list : _items_special.values()) {
                if (list == null) {
                    continue;
                }

                for (Integer id : list) {
                    if (found >= 3) {
                        return true;
                    }

                    if (id == null) {
                        continue;
                    }

                    if (inv.isEquippedItem(id, null, 6)) {
                        found++;
                        break;
                    }

                }
            }

            return (found >= 3);
        }

        for (Integer item_id : _items) {
            if (item_id == null
                    || item_id == 0) {
                continue;
            }

            if (!inv.isEquippedItem(item_id, null, 6)) {
                return false;
            }
        }
        return true;
    }
}
