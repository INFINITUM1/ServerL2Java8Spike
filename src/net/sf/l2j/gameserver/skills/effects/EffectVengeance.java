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
package net.sf.l2j.gameserver.skills.effects;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.util.Rnd;

/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
final class EffectVengeance extends L2Effect {

    public EffectVengeance(Env env, EffectTemplate template) {
        super(env, template);
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.BUFF;
    }

    /**
     * Notify started
     */
    @Override
    public void onStart() {
        getEffected().setIsImobilised(true);
        FastList<L2Character> chars = getEffected().getKnownList().getKnownCharactersInRadius(200);
        if (!chars.isEmpty()) {
            boolean srcInArena = (getEffected().isInsideZone(L2Character.ZONE_PVP) && !getEffected().isInsideZone(L2Character.ZONE_SIEGE));

            L2PcInstance src = getEffected().getPlayer();

            L2Character obj = null;
            for (FastList.Node<L2Character> n = chars.head(), end = chars.tail(); (n = n.getNext()) != end;) {
                obj = n.getValue();
                if (obj == null) {
                    continue;
                }

                if (!(obj.isL2Attackable() || obj.isL2Playable())) {
                    continue;
                }

                if (obj == getEffected() || obj.isDead()) {
                    continue;
                }

                if (obj.isPlayer() || obj.isL2Summon()) {
                    L2PcInstance trg = obj.getPlayer();

                    if (!src.checkPvpSkill(trg, SkillTable.getInstance().getInfo(1344, 1))) {
                        continue;
                    }

                    if (trg.isInZonePeace()) {
                        continue;
                    }

                    if ((src.getParty() != null && src.getParty().getPartyMembers().contains(trg))) {
                        continue;
                    }

                    if (!srcInArena && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE))) {
                        if (src.getClanId() != 0 && src.getClanId() == trg.getClanId()) {
                            continue;
                        }

                        if (src.getAllyId() != 0 && src.getAllyId() == trg.getAllyId()) {
                            continue;
                        }
                    }
                }

                if (!getEffected().canSeeTarget(obj)) {
                    continue;
                }

                //if (Rnd.get(100) < 00)
                //{
                obj.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getEffected(), (int) ((150 * 3994) / (obj.getLevel() + 7)));
                obj.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getEffected());
                obj.setTarget(getEffected());
                //}				
            }
            chars.clear();
            chars = null;
            obj = null;
        }
    }

    /**
     * Notify exited
     */
    @Override
    public void onExit() {
        getEffected().setIsImobilised(false);
    }

    @Override
    public boolean onActionTime() {
        // just stop this effect
        getEffected().setIsImobilised(false);
        return false;
    }
}
