/* This program is free software; you can redistribute it and/or modify
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

import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.funcs.FuncAdd;
import net.sf.l2j.gameserver.skills.funcs.LambdaConst;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

/**
 * Used to store an augmentation and its boni
 *
 * @author durgus
 */
public final class L2Augmentation {

    private static final Logger _log = Logger.getLogger(L2Augmentation.class.getName());
    private L2ItemInstance _item;
    private int _effectsId = 0;
    private AugmentationStatBoni _boni = null;
    private L2Skill _skill = null;

    public L2Augmentation(L2ItemInstance item, int effects, L2Skill skill, boolean save) {
        _item = item;
        _effectsId = effects;
        _boni = new AugmentationStatBoni(_effectsId);
        _skill = skill;

        // write to DB if save is true
        if (save) {
            saveAugmentationData();
        }
    }

    public L2Augmentation(L2ItemInstance item, int effects, int skill, int skillLevel, boolean save) {
        this(item, effects, SkillTable.getInstance().getInfo(skill, skillLevel), save);
    }

    // =========================================================
    // Nested Class
    public static class AugmentationStatBoni {

        private Stats _stats[];
        private float _values[];
        private boolean _active;

        public AugmentationStatBoni(int augmentationId) {
            _active = false;
            FastList<AugmentationData.AugStat> as = AugmentationData.getInstance().getAugStatsById(augmentationId);

            _stats = new Stats[as.size()];
            _values = new float[as.size()];

            int i = 0;
            for (AugmentationData.AugStat aStat : as) {
                _stats[i] = aStat.getStat();
                _values[i] = aStat.getValue();
                i++;
            }
        }

        public void applyBoni(L2PcInstance player) {
            // make sure the boni are not applyed twice..
            if (_active) {
                return;
            }

            for (int i = 0; i < _stats.length; i++) {
                ((L2Character) player).addStatFunc(new FuncAdd(_stats[i], 0x40, this, new LambdaConst(_values[i])));
            }

            _active = true;
        }

        public void removeBoni(L2PcInstance player) {
            // make sure the boni is not removed twice
            if (!_active) {
                return;
            }

            ((L2Character) player).removeStatsOwner(this);

            _active = false;
        }
    }

    private void saveAugmentationData() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement("UPDATE `items` SET `aug_id`=?,`aug_skill`=?,`aug_lvl`=? WHERE `object_id`=?");
            statement.setInt(1, _effectsId);
            if (_skill != null) {
                statement.setInt(2, _skill.getId());
                statement.setInt(3, _skill.getLevel());
            } else {
                statement.setInt(2, -1);
                statement.setInt(3, -1);
            }
            statement.setInt(4, _item.getObjectId());

            statement.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Could not save augmentation for item: " + _item.getObjectId() + " from DB:", e);
        } finally {
            Close.CS(con, statement);
        }
    }

    public void deleteAugmentationData() {
        if (!_item.isAugmented()) {
            return;
        }

        // delete the augmentation from the database
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE `items` SET `aug_id`=?,`aug_skill`=?,`aug_lvl`=? WHERE `object_id`=?");
            statement.setInt(1, -1);
            statement.setInt(2, -1);
            statement.setInt(3, -1);
            statement.setInt(4, _item.getObjectId());
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Could not delete augmentation for item: " + _item.getObjectId() + " from DB:", e);
        } finally {
            Close.CS(con, statement);
        }
    }

    /**
     * Get the augmentation "id" used in serverpackets.
     *
     * @return augmentationId
     */
    public int getAugmentationId() {
        return _effectsId;
    }

    public L2Skill getSkill() {
        return _skill;
    }

    public L2Skill getAugmentSkill() {
        return _skill;
    }

    /**
     * Applys the boni to the player.
     *
     * @param player
     */
    public void applyBoni(L2PcInstance player) {
        _boni.applyBoni(player);
        // add the skill if any
        if (_skill != null) {
            if (Config.ONE_AUGMENT && player.getActiveAug() != 0) {
                player.stopSkillEffects(player.getActiveAug());
                player.setActiveAug(_skill.isSkillTypeOffensive() ? 0 : _skill.getId());
            }
            player.addSkill(_skill, false);
            player.sendSkillList();
        }
    }

    /**
     * Removes the augmentation boni from the player.
     *
     * @param player
     */
    public void removeBoni(L2PcInstance player) {
        _boni.removeBoni(player);
        // remove the skill if any
        if (_skill != null) {
            /*
             * if (Config.ONE_AUGMENT && (_skill.isActive() ||
             * _skill.isChance()))
             player.stopSkillEffects(_skill.getId());
             */
            if (_skill.isActive() || _skill.isChance()) {
                player.abortCast();
                player.abortAttack();
                //player.setActiveAug(_skill.getId());
                if (Config.ONE_AUGMENT
                        && !_skill.isSkillTypeOffensive()) {
                    //player.setActiveAug(0);
                    player.stopSkillEffects(_skill.getId());
                }
            }

            player.removeSkill(_skill, false);
            player.sendSkillList();
        }
    }
}
