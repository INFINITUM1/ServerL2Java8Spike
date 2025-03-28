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

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;

public class EffectInvisible extends L2Effect {

    public EffectInvisible(Env env, EffectTemplate template) {
        super(env, template);
    }

    @Override
    public EffectType getEffectType() {
        return L2Effect.EffectType.BUFF;
    }

    @Override
    public void onStart() {
        getEffected().setChannel(0);
        getEffected().teleToLocation(getEffected().getX(), getEffected().getY(), getEffected().getZ());
    }

    @Override
    public boolean onActionTime() {
        getEffected().setChannel(1);
        getEffected().teleToLocation(getEffected().getX(), getEffected().getY(), getEffected().getZ());
        return false;
    }

    @Override
    public void onExit() {
        getEffected().setChannel(1);
        getEffected().teleToLocation(getEffected().getX(), getEffected().getY(), getEffected().getZ());
    }
}
