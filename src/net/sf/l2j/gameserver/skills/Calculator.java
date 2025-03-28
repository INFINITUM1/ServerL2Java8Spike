/*
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
package net.sf.l2j.gameserver.skills;

import javolution.util.FastList;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.util.ArrayUtils;

/**
 * A calculator is created to manage and dynamically calculate the effect of a
 * character property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each
 * calculator is a table of Func object in which each Func represents a
 * mathematic function : <BR><BR>
 *
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
 *
 * When the calc method of a calculator is launched, each mathematic function is
 * called according to its priority <B>_order</B>. Indeed, Func with lowest
 * priority order is executed first and Funcs with the same order are executed
 * in unspecified order. The result of the calculation is stored in the value
 * property of an Env class instance.<BR><BR>
 *
 * Method addFunc and removeFunc permit to add and remove a Func object from a
 * Calculator.<BR><BR>
 *
 */
public final class Calculator {

    /**
     * Empty Func table definition
     */
    private static final Func[] _emptyFuncs = new Func[0];

    /**
     * Table of Func object
     */
    private Func[] _functions;

    /**
     * Constructor of Calculator (Init value : emptyFuncs).<BR><BR>
     */
    public Calculator() {
        _functions = _emptyFuncs;
    }

    /**
     * Constructor of Calculator (Init value : Calculator c).<BR><BR>
     */
    public Calculator(Calculator c) {
        _functions = c._functions;
    }

    /**
     * Check if 2 calculators are equals.<BR><BR>
     */
    public static boolean equalsCals(Calculator c1, Calculator c2) {
        if (c1 == c2) {
            return true;
        }

        if (c1 == null || c2 == null) {
            return false;
        }

        Func[] funcs1 = c1._functions;
        Func[] funcs2 = c2._functions;

        if (funcs1 == funcs2) {
            return true;
        }

        if (funcs1.length != funcs2.length) {
            return false;
        }

        if (funcs1.length == 0) {
            return true;
        }

        for (int i = 0; i < funcs1.length; i++) {
            if (funcs1[i] != funcs2[i]) {
                return false;
            }
        }
        return true;

    }

    /**
     * Return the number of Funcs in the Calculator.<BR><BR>
     */
    public int size() {
        return _functions.length;
    }

    /**
     * Add a Func to the Calculator.<BR><BR>
     */
    public void addFunc(Func f) {
        _functions = ArrayUtils.add(_functions, f);
        ArrayUtils.eqSort(_functions);
    }

    /**
     * Remove a Func from the Calculator.<BR><BR>
     */
    public void removeFunc(Func f) {
        _functions = ArrayUtils.remove(_functions, f);
        if (_functions.length == 0) {
            _functions = _emptyFuncs;
        } else {
            ArrayUtils.eqSort(_functions);
        }
    }

    /**
     * Remove each Func with the specified owner of the Calculator.<BR><BR>
     */
    public synchronized FastList<Stats> removeOwner(Object owner) {
        /*Func[] funcs = _functions;
         FastList<Stats> modifiedStats = new FastList<Stats>();

         for (int i = 0; i < funcs.length; i++) {
         if (funcs[i].funcOwner == owner) {
         modifiedStats.add(funcs[i].stat);
         removeFunc(funcs[i]);
         }
         }
         return modifiedStats;*/
        FastList<Stats> modifiedStats = new FastList<>();
        Func[] tmp = _functions;
        for (Func element : tmp) {
            if (element.funcOwner == owner) {
                modifiedStats.add(element.stat);
                removeFunc(element);
            }
        }
        return modifiedStats;
    }

    /**
     * Run each Func of the Calculator.<BR><BR>
     */
    public void calc(Env env) {
        /*Func[] funcs = _functions;

         for (int i = 0; i < funcs.length; i++) {
         funcs[i].calc(env);
         }
         */
        Func[] funcs = _functions;

        for (Func func : funcs) {
            if (func == null) {
                continue;
            }

            func.calc(env);
        }
    }

}
