/*
 *@author Julian
 *
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
package net.sf.l2j.gameserver.model.actor.instance;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

/**
 * This class manages all chest.
 */
public final class L2ChestInstance extends L2MonsterInstance {

    private volatile boolean _isInteracted;
    private volatile boolean _specialDrop;

    public L2ChestInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
        _isInteracted = false;
        _specialDrop = false;
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        _isInteracted = false;
        _specialDrop = false;
        setMustRewardExpSp(true);
    }

    public synchronized boolean isInteracted() {
        return _isInteracted;
    }

    public synchronized void setInteracted() {
        _isInteracted = true;
    }

    public synchronized boolean isSpecialDrop() {
        return _specialDrop;
    }

    public synchronized void setSpecialDrop() {
        _specialDrop = true;
    }

    @Override
    public void doItemDrop(L2NpcTemplate npcTemplate, L2Character lastAttacker) {
        int id = getTemplate().npcId;

        if (!_specialDrop) {
            if (id >= 18265 && id <= 18286) {
                id += 3536;
            } else if (id == 18287 || id == 18288) {
                id = 21671;
            } else if (id == 18289 || id == 18290) {
                id = 21694;
            } else if (id == 18291 || id == 18292) {
                id = 21717;
            } else if (id == 18293 || id == 18294) {
                id = 21740;
            } else if (id == 18295 || id == 18296) {
                id = 21763;
            } else if (id == 18297 || id == 18298) {
                id = 21786;
            }
        }

        super.doItemDrop(NpcTable.getInstance().getTemplate(id), lastAttacker);
    }
    //cast - trap chest

    public void chestTrap(L2Character player) {
        int trapSkillId = 0;
        int rnd = Rnd.get(120);

        if (getTemplate().level >= 61) {
            if (rnd >= 90) {
                trapSkillId = 4139;//explosion
            } else if (rnd >= 50) {
                trapSkillId = 4118;//area paralysys
            } else if (rnd >= 20) {
                trapSkillId = 1167;//poison cloud
            } else {
                trapSkillId = 223;//sting
            }
        } else if (getTemplate().level >= 41) {
            if (rnd >= 90) {
                trapSkillId = 4139;//explosion
            } else if (rnd >= 60) {
                trapSkillId = 96;//bleed
            } else if (rnd >= 20) {
                trapSkillId = 1167;//poison cloud
            } else {
                trapSkillId = 4118;//area paralysys
            }
        } else if (getTemplate().level >= 21) {
            if (rnd >= 80) {
                trapSkillId = 4139;//explosion
            } else if (rnd >= 50) {
                trapSkillId = 96;//bleed
            } else if (rnd >= 20) {
                trapSkillId = 1167;//poison cloud
            } else {
                trapSkillId = 129;//poison
            }
        } else {
            if (rnd >= 80) {
                trapSkillId = 4139;//explosion
            } else if (rnd >= 50) {
                trapSkillId = 96;//bleed
            } else {
                trapSkillId = 129;//poison
            }
        }

        player.sendPacket(SystemMessage.sendString("There was a trap!"));
        handleCast(player, trapSkillId);
    }
    //<--
    //cast casse
    //<--

    private boolean handleCast(L2Character player, int skillId) {
        int skillLevel = 1;
        byte lvl = getTemplate().level;
        if (lvl > 20 && lvl <= 40) {
            skillLevel = 3;
        } else if (lvl > 40 && lvl <= 60) {
            skillLevel = 5;
        } else if (lvl > 60) {
            skillLevel = 6;
        }

        if (player.isDead()
                || !player.isVisible()
                || !player.isInsideRadius(this, getDistanceToWatchObject(player), false, false)) {
            return false;
        }

        L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);

        if (player.getFirstEffect(skill) == null) {
            skill.getEffects(this, player);
            broadcastPacket(new MagicSkillUser(this, player, skill.getId(), skillLevel,
                    skill.getHitTime(), 0));
            return true;
        }
        return false;
    }

    @Override
    public boolean isMovementDisabled() {
        if (super.isMovementDisabled()) {
            return true;
        }
        if (isInteracted()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean hasRandomAnimation() {
        return false;
    }

    @Override
    protected void calculateRewards(L2Character lastAttacker) {
        //
    }

    @Override
    public void notifySkillUse(L2PcInstance caster, L2Skill skill) {
        if (skill == null) {
            reduceCurrentHp(1, caster, true);
            return;
        }

        int open_chance = 0;
        int chest_lvl = getLevel();

        int temp = 0;
        int lvl_diff = 0;
        int skill_id = skill.getId();
        int skill_lvl = skill.getLevel();
        switch (skill_id) {
            case 27:
                switch (skill_lvl) {
                    case 1:
                        temp = 98;
                        break;
                    case 2:
                    case 4:
                        temp = 84;
                        break;
                    case 3:
                        temp = 99;
                        break;
                    case 5:
                    case 8:
                        temp = 88;
                        break;
                    case 6:
                    case 10:
                        temp = 90;
                        break;
                    case 7:
                    case 12:
                    case 13:
                    case 14:
                        temp = 89;
                        break;
                    case 9:
                        temp = 86;
                        break;
                    case 11:
                        temp = 87;
                        break;
                    default:
                        temp = 89;
                        break;
                }
                open_chance = (temp - (chest_lvl - skill_lvl * 4 - 16) * 6);
                open_chance = Math.min(open_chance, temp);
                break;
            case 2065:
                open_chance = (int) ((60 - (chest_lvl - (skill_lvl - 1) * 10) * 1.500000));
                open_chance = Math.min(open_chance, 60);
                break;
            case 2229:
                switch (skill_lvl) {
                    case 1:
                        lvl_diff = (chest_lvl - 19);
                        if (lvl_diff > 0) {
                            open_chance = 100;
                        } else {
                            open_chance = (int) ((0.000200 * lvl_diff * lvl_diff - 0.026400 * lvl_diff + 0.769500) * 100);
                        }
                        break;
                    case 2:
                        lvl_diff = (chest_lvl - 29);
                        if (lvl_diff > 0) {
                            open_chance = 100;
                        } else {
                            open_chance = (int) ((0.000300 * lvl_diff * lvl_diff - 0.027900 * lvl_diff + 0.756800) * 100);
                        }
                        break;
                    case 3:
                        lvl_diff = (chest_lvl - 39);
                        if (lvl_diff > 0) {
                            open_chance = 100;
                        } else {
                            open_chance = (int) ((0.000300 * lvl_diff * lvl_diff - 0.026900 * lvl_diff + 0.733400) * 100);
                        }
                        break;
                    case 4:
                        lvl_diff = (chest_lvl - 49);
                        if (lvl_diff > 0) {
                            open_chance = 100;
                        } else {
                            open_chance = (int) ((0.000300 * lvl_diff * lvl_diff - 0.028400 * lvl_diff + 0.803400) * 100);
                        }
                        break;
                    case 5:
                        lvl_diff = (chest_lvl - 59);
                        if (lvl_diff > 0) {
                            open_chance = 100;
                        } else {
                            open_chance = (int) ((0.000500 * lvl_diff * lvl_diff - 0.035600 * lvl_diff + 0.906500) * 100);
                        }
                        break;
                    case 6:
                        lvl_diff = (chest_lvl - 69);
                        if (lvl_diff > 0) {
                            open_chance = 100;
                        } else {
                            open_chance = (int) ((0.000900 * lvl_diff * lvl_diff - 0.037300 * lvl_diff + 0.857200) * 100);
                        }
                        break;
                    case 7:
                        lvl_diff = (chest_lvl - 79);
                        if (lvl_diff > 0) {
                            open_chance = 100;
                        } else {
                            open_chance = (int) ((0.004300 * lvl_diff * lvl_diff - 0.067100 * lvl_diff + 0.959300) * 100);
                        }
                        break;
                    default:
                        open_chance = 100;
                        break;
                }
                break;
        }

        open_chance = Math.min(open_chance, Config.CHEST_CHANCE);
        if (Config.CUSTOM_CHEST_DROP && CustomServerData.getInstance().isSpecChest(getNpcId())) {
            open_chance = 100;
        }

        if (Rnd.get(100) < open_chance) {
            suicide(caster, getNpcId() + "" + skill_id + "" + skill_lvl);
        } else {
            soundEffect(caster, "ItemSound2.broken_key");
            doDie(null);
            setCurrentHp(0);
        }
    }

    public void suicide(L2PcInstance attacker) {
        suicide(attacker, null);
    }

    private void suicide(L2PcInstance attacker, String skillid_lvl) {
        //doDie(null);
        //setCurrentHp(0);
        super.reduceCurrentHp(99999999, attacker);

        // 21-41
        // 42-51
        // 52-63
        // 64-72
        // 72-80
        int drop_lvl = 0;
        int chest_lvl = getLevel();
        if (chest_lvl >= 21 && chest_lvl <= 41) {
            drop_lvl = 1;
        } else if (chest_lvl >= 42 && chest_lvl <= 51) {
            drop_lvl = 2;
        } else if (chest_lvl >= 52 && chest_lvl <= 63) {
            drop_lvl = 3;
        } else if (chest_lvl >= 64 && chest_lvl <= 72) {
            drop_lvl = 4;
        } else if (chest_lvl >= 72) {
            drop_lvl = 5;
        }

        if (Config.CUSTOM_CHEST_DROP && skillid_lvl != null) {
            CustomServerData.getInstance().checkChestReward(attacker, skillid_lvl);
        }

        FastList<EventReward> drop = CustomServerData.getInstance().getChestDrop(drop_lvl);
        if (drop == null) {
            return;
        }

        for (FastList.Node<EventReward> n = drop.head(), end = drop.tail(); (n = n.getNext()) != end;) {
            EventReward reward = n.getValue(); // No typecast necessary.
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                dropItem(reward.id, reward.count, attacker);
                break;
            }
        }
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        int skill_lvl = (int) (getLevel() / 10);
        if (!isSkillDisabled(4143)) {
            setTarget(attacker);
            addUseSkillDesire(4143, skill_lvl);
        }

        super.reduceCurrentHp(damage, attacker, awake);
    }

    @Override
    public boolean doDie(L2Character killer) {
        // killing is only possible one time
        synchronized (this) {
            if (isKilledAlready()) {
                return false;
            }
            setIsKilledAlready(true);
        }
        // Set target to null and cancel Attack or Cast
        setTarget(null);

        // Stop movement
        stopMove(null);

        // Stop HP/MP/CP Regeneration task
        getStatus().stopHpMpRegeneration();

        broadcastStatusUpdate();
        getAI().notifyEvent(CtrlEvent.EVT_DEAD, null);

        if (getWorldRegion() != null) {
            getWorldRegion().onDeath(this);
        }

        getNotifyQuestOfDeath().clear();
        getAttackByList().clear();
        getKnownList().gc();
        DecayTaskManager.getInstance().addDecayTask(this);
        return true;
    }

    @Override
    public void addDamageHate(L2Character attacker, int damage, int aggro) {
        //
    }

    @Override
    public boolean isL2Chest() {
        return true;
    }
}
