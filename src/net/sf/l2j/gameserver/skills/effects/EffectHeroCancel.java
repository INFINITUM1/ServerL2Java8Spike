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
import net.sf.l2j.util.Rnd;
import javolution.util.FastTable;

public class EffectHeroCancel extends L2Effect {

    public EffectHeroCancel(Env env, EffectTemplate template) {
        super(env, template);
    }

    @Override
    public EffectType getEffectType() {
        return L2Effect.EffectType.FEAR;
    }

    @Override
    public void onStart() {
        if (Rnd.get(100) < 35)
        {
            onExit();
            return;
        }

        int max = Rnd.get(2, 4);
        int canceled = 0;
        int count = 0;
        int finish = getEffected().getBuffCount();

        FastTable<L2Effect> effects = getEffected().getAllEffectsTable();
        for (int i = 0, n = effects.size(); i < n; i++) {
            L2Effect e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (e.getSkill().isCancelProtected()) {
                continue;
            }

            if (e.getSkill().isBuff()) {
                if (Rnd.get(100) < 40) {
                    e.exit();
                    canceled++;
                }

                if (canceled >= max || count >= finish) {
                    onActionTime();
                    break;
                }
                count++;
            }
        }
    }

    @Override
    public boolean onActionTime() {
        return false;
    }

    @Override
    public void onExit() {
    }
}
