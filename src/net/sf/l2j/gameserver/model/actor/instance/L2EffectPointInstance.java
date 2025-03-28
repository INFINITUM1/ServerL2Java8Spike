/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.instance;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class L2EffectPointInstance extends L2NpcInstance {

    private L2PcInstance _player;
    private int _effectId;
    private int _skillId;
    private long _delay;
    private int _count = 0;
    private int _maxCount;

    private class EffectCycle implements Runnable {

        public EffectCycle() {
        }

        @Override
        public void run() {
            _count++;
            if (_count > _maxCount) {
                decayMe();
                deleteMe();
                return;
            }

            FastList<L2Character> players = getKnownList().getKnownCharactersInRadius(180);
            if (players == null || players.isEmpty()) {
                if (_delay == 5000) {
                    decayMe();
                    deleteMe();
                } else {
                    ThreadPoolManager.getInstance().scheduleAi(new EffectCycle(), _delay, false);
                }
                return;
            }

            if (_delay == 5000 && !players.contains(_player)) {
                decayMe();
                deleteMe();
                return;
            }

            L2Skill skl = SkillTable.getInstance().getInfo(_effectId, 1);
            L2Character pc = null;
            for (FastList.Node<L2Character> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
                pc = n.getValue();
                if (pc == null || pc.isL2Door()) {
                    continue;
                }

                if (_delay != 5000) {
                    if (pc.isPlayer()) {
                        L2PcInstance pl = pc.getPlayer();
                        if (pl == _player) {
                            continue;
                        }
                        if (_player.getParty() != null && _player.getParty().getPartyMembers().contains(pl)) {
                            continue;
                        }
                    }
                }
                /*else
                {
                if (_player.getParty() != null && !_player.getParty().getPartyMembers().contains(pc))
                continue;
                }*/
                pc.stopSkillEffects(_effectId);
                if (_delay == 5000 || _effectId == 5145 || _effectId == 5134 || _effectId == 5124) {
                    skl.getEffects(pc, pc);
                } else {
                    int damage = (int) Formulas.calcMagicDam(_player, pc, skl, false, true, false);
                    pc.reduceCurrentHp(damage, _player);
                }

                pc.broadcastPacket(new MagicSkillUser(pc, pc, _effectId, 1, 0, 0));
            }
            players.clear();
            players = null;
            pc = null;
            ThreadPoolManager.getInstance().scheduleAi(new EffectCycle(), _delay, false);
        }
    }

    public L2EffectPointInstance(int objectId, L2NpcTemplate template, L2Character owner) {
        super(objectId, template);
    }

    public L2EffectPointInstance(int objectId, L2NpcTemplate template, L2PcInstance player, int effId, int skillId) {
        super(objectId, template);
        _player = player;
        _effectId = effId;
        _skillId = skillId;
        _maxCount = getCount(skillId);
        _delay = getDelay(skillId);
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
        FastList<L2Character> players = getKnownList().getKnownCharactersInRadius(1200);
        if (players == null || players.isEmpty()) {
            return;
        }
        L2Character pc = null;
        for (FastList.Node<L2Character> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null || pc.isL2Door()) {
                continue;
            }
            pc.stopSkillEffects(_effectId);
        }
        players.clear();
        players = null;
        pc = null;
    }

    @Override
    public void onSpawn() {
        ThreadPoolManager.getInstance().scheduleAi(new EffectCycle(), 1000, false);
    }

    private int getDelay(int sklId) {
        switch (sklId) {
            case 454:
            case 456:
            case 457:
            case 458:
            case 459:
            case 460:
                return 5000;
            default:
                return 2000;
        }
    }

    private int getCount(int sklId) {
        switch (sklId) {
            case 454:
            case 456:
            case 457:
            case 458:
            case 459:
            case 460:
                return 24;
            default:
                return 15;
        }
    }

    /**
     * this is called when a player interacts with this NPC
     * @param player
     */
    @Override
    public void onAction(L2PcInstance player) {
        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendActionFailed();
    }

    @Override
    public void onActionShift(L2GameClient client) {
        L2PcInstance player = client.getActiveChar();
        if (player == null) {
            return;
        }
        player.sendActionFailed();
    }

    /*public L2Character getOwner()
    {
    return _owner;
    }*/
    @Override
    public L2PcInstance getPlayer() {
        return _player;
    }
}
