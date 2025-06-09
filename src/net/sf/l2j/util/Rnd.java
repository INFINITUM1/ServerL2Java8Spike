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
package net.sf.l2j.util;

import java.util.Random;

import net.sf.l2j.Config;

public class Rnd {

    private static MTRandom _rnd = new MTRandom();

    public static float get() // get random number from 0 to 1
    {
        return _rnd.nextFloat();
    }

    /**
     * Gets a random number from 0(inclusive) to n(exclusive)
     *
     * @param n The superior limit (exclusive)
     * @return A number from 0 to n-1
     */
    public static int get(int n) {
        return (int) Math.floor(_rnd.nextDouble() * n);
    }

    public static int get(int min, int max) // get random number from min to max (not max-1 !)
    {
        return min + (int) Math.floor(_rnd.nextDouble() * (max - min + 1));
    }

    public static boolean chance(int chance) {
        return _rnd.nextInt(99) + 1 <= chance;
    }

    public static boolean calcEnchant(double chance, boolean premium) {
        if (premium) {
            chance += Config.PREMIUM_ENCH_ITEM;
        }

        if (Config.ENCHANT_ALT_FORMULA) {
            return poker(chance) < chance;
        }

        return _rnd.nextDouble() <= chance / 100.;
    }

    public static int nextInt(int n) {
        return (int) Math.floor(_rnd.nextDouble() * n);
    }

    public static int nextInt() {
        return _rnd.nextInt();
    }

    public static double nextDouble() {
        return _rnd.nextDouble();
    }

    public static double nextGaussian() {
        return _rnd.nextGaussian();
    }

    public static boolean nextBoolean() {
        return _rnd.nextBoolean();
    }

    public static void nextBytes(byte[] array) {
        _rnd.nextBytes(array);
    }
    public static Random skillRnd = new Random(System.nanoTime());

    public static double chance(final double pMin, final double pMax) {
        skillRnd.setSeed(System.nanoTime());
        return pMin + skillRnd.nextDouble() * (pMax - pMin);
    }

    public static double poker(final double chance) {
        return chance(chance * 0.75, 100);
    }

    public static double poker(final double chance, final double pMax) {
        return chance(chance * 0.75, pMax);
    }

    public static long get(long n) {
        return (long) (_rnd.nextDouble() * n);
    }
}
