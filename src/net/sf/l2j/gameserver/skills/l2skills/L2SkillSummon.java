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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import javolution.util.FastList;
import net.sf.l2j.Config;

public class L2SkillSummon extends L2Skill {

    private int _npcId;
    private float _expPenalty;
    private boolean _isCubic;

    public L2SkillSummon(StatsSet set) {
        super(set);

        _npcId = set.getInteger("npcId", 0); // default for undescribed skills
        _expPenalty = set.getFloat("expPenalty", 0.f);
        _isCubic = set.getBool("isCubic", false);
    }

    public boolean checkCondition(L2Character activeChar) {
        if (activeChar.isPlayer()) {
            L2PcInstance player = activeChar.getPlayer();

            if (player.noSummon()) {
                player.sendPacket(Static.SUMMON_WRONG_PLACE);
                return false;
            }

            if (_isCubic) {
                if (getTargetType() != L2Skill.SkillTargetType.TARGET_SELF) {
                    return true; //Player is always able to cast mass cubic skill
                }
                int mastery = player.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
                if (mastery < 0) {
                    mastery = 0;
                }
                int count = player.getCubics().size();
                if (count > mastery) {
                    player.sendMessage("Нельзя больше " + count + " кубиков");
                    return false;
                }
            } else {
                if (player.inObserverMode()) {
                    return false;
                }

                if (player.getPet() != null) {
                    player.sendPacket(Static.HAVE_PET);
                    return false;
                }
            }
        }
        return super.checkCondition(activeChar, null, false);
    }

    @Override
    public void useSkill(L2Character caster, FastList<L2Object> targets) {
        if (caster.isAlikeDead() || !(caster.isPlayer())) {
            return;
        }

        L2PcInstance activeChar = caster.getPlayer();
        if (_npcId == 0) {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_S2).addString("Summon skill " + getId() + " not described yet"));
            return;
        }

        if (_isCubic) {
            if (targets.size() > 1) //Mass cubic skill
            {
                for (FastList.Node<L2Object> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;) {
                    L2Object obj = n.getValue();

                    if (!(obj.isPlayer())) {
                        continue;
                    }

                    L2PcInstance player = obj.getPlayer();
                    int mastery = player.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
                    if (mastery < 0) {
                        mastery = 0;
                    }
                    if (mastery == 0 && player.getCubics().size() > 0) {
                        //Player can have only 1 cubic - we shuld replace old cubic with new one
                        for (L2CubicInstance c : player.getCubics().values()) {
                            c.stopAction();
                            c = null;
                        }
                        player.getCubics().clear();
                    }

                    if (player.getCubics().size() > mastery) {
                        continue;
                    }

                    if (player.getCubics().containsKey(_npcId)) {
                        player.sendPacket(Static.HAVE_CUBIC);
                    } else {
                        player.addCubic(_npcId, getLevel());
                        player.broadcastUserInfo();
                    }
                }
            } else //normal cubic skill
            {
                int mastery = activeChar.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
                if (mastery < 0) {
                    mastery = 0;
                }
                if (activeChar.getCubics().size() > mastery) {
                    //if (Config.DEBUG)
                    //	_log.fine("player can't summon any more cubics. ignore summon skill");
                    activeChar.sendPacket(Static.CUBIC_SUMMONING_FAILED);
                    return;
                }
                if (activeChar.getCubics().containsKey(_npcId)) {
                    activeChar.sendPacket(Static.HAVE_CUBIC);
                    return;
                }
                activeChar.addCubic(_npcId, getLevel());
                activeChar.broadcastUserInfo();
            }
            return;
        }

        if (activeChar.getPet() != null || activeChar.isMounted() || activeChar.isInsideDismountZone()) {
            //if (Config.DEBUG)
            //	_log.fine("player has a pet already. ignore summon skill");
            return;
        }

        L2SummonInstance summon;
        L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(_npcId);
        if (summonTemplate.type.equalsIgnoreCase("L2SiegeSummon")) {
            summon = new L2SiegeSummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
        } else {
            summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
        }

        summon.setName(summonTemplate.name);
        summon.setTitle(activeChar.getName());
        summon.setExpPenalty(_expPenalty);

        if (summon.getLevel() >= Experience.LEVEL.length) {
            summon.getStat().setExp(Experience.LEVEL[Experience.LEVEL.length - 1]);
        } else {
            summon.getStat().setExp(Experience.LEVEL[(summon.getLevel() % Experience.LEVEL.length)]);
        }

        summon.setCurrentHp(summon.getMaxHp());
        summon.setCurrentMp(summon.getMaxMp());
        summon.setHeading(activeChar.getHeading());
        summon.setRunning();
        activeChar.setPet(summon);

        L2World.getInstance().storeObject(summon);
        summon.spawnMe(activeChar.getX() + 50, activeChar.getY() + 100, activeChar.getZ());

        summon.setFollowStatus(true);
        summon.setShowSummonAnimation(false); // addVisibleObject created the info packets with summon animation
    }
}
