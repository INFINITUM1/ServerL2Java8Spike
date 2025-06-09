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

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.util.Rnd;

/**
 * @author littlecrow
 *
 * Implementation of the Fear Effect
 */
final class EffectFear extends L2Effect {

    public static final int FEAR_RANGE = 500;

    public EffectFear(Env env, EffectTemplate template) {
        super(env, template);
    }

    @Override
    public EffectType getEffectType() {
        return EffectType.FEAR;
    }

    /** Notify started */
    @Override
    public void onStart() {
        if (!getEffected().isAfraid()) {
            getEffected().startFear();
            onActionTime();
        }
    }

    /** Notify exited */
    @Override
    public void onExit() {
        getEffected().stopFear(this);
    }

    @Override
    public boolean onActionTime() {
        // чтоб не выбегали за ограду олимпа 
        if (getEffected().isPlayer()) {
            if (getEffected().getPlayer().isInOlympiadMode()) {
                getEffected().getPlayer().startFear();
                return true;
            }
        }

        getEffected().rndWalk();
        return true;
    }
}
