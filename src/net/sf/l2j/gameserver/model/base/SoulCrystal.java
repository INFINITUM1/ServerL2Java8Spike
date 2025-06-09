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
package net.sf.l2j.gameserver.model.base;

/**
 * $ Rewrite 06.12.06 - Yesod
 * */
public class SoulCrystal {

    public static final int[][] HighSoulConvert = {
        {4639, 5577}, //RED 10 - 11
        {5577, 5580}, //RED 11 - 12
        {5580, 5908}, //RED 12 - 13

        {4650, 5578}, //GRN 10 - 11
        {5578, 5581}, //GRN 11 - 12
        {5581, 5911}, //GRN 12 - 13

        {4661, 5579}, //BLU 10 - 11
        {5579, 5582}, //BLU 11 - 12
        {5582, 5914} //BLU 12 - 13
    };
    /**
     * "First line is for Red Soul Crystals, second is Green and third is Blue Soul Crystals,
     *  ordered by ascending level, from 0 to 13..."
     */
    public static final int[] SoulCrystalTable = {
        4629, 4630, 4631, 4632, 4633, 4634, 4635, 4636, 4637, 4638, 4639, 5577, 5580, 5908,
        4640, 4641, 4642, 4643, 4644, 4645, 4646, 4647, 4648, 4649, 4650, 5578, 5581, 5911,
        4651, 4652, 4653, 4654, 4655, 4656, 4657, 4658, 4659, 4660, 4661, 5579, 5582, 5914
    };
    public static final int MAX_CRYSTALS_LEVEL = 13;
    public static final int BREAK_CHANCE = 10;
    public static final int LEVEL_CHANCE = 32;
    public static final int RED_BROKEN_CRYSTAL = 4662;
    public static final int GRN_BROKEN_CYRSTAL = 4663;
    public static final int BLU_BROKEN_CRYSTAL = 4664;
    public static final int RED_NEW_CRYSTAL = 4629;
    public static final int GRN_NEW_CYRSTAL = 4640;
    public static final int BLU_NEW_CRYSTAL = 4651;

    public static int getLevel(int id) {
        if (id >= 4629 && id <= 4639) //Red Soul Crystal
        {
            return id - 4629;
        }
        if (id >= 4640 && id <= 4650) //Green Soul Crystal
        {
            return id - 4640;
        }
        if (id >= 4651 && id <= 4661) //Blue Soul Crystal
        {
            return id - 4651;
        }

        switch (id) {
            case 5577:
            case 5578:
            case 5579:
                return 11;
            case 5580:
            case 5581:
            case 5582:
                return 12;
            case 5908:
            case 5911:
            case 5914:
                return 13;
            default:
                return 99;
        }
    }

    public static int getNextLevel(int id) {
        if (id >= 4629 && id <= 4638) //Red Soul Crystal
        {
            return id + 1;
        }
        if (id >= 4640 && id <= 4649) //Green Soul Crystal
        {
            return id + 1;
        }
        if (id >= 4651 && id <= 4660) //Blue Soul Crystal
        {
            return id + 1;
        }

        switch (id) {
            case 4639:
                return 5577;
            case 4650:
                return 5578;
            case 4661:
                return 5579;
            case 5577:
                return 5580;
            case 5578:
                return 5581;
            case 5579:
                return 5582;
            case 5580:
                return 5908;
            case 5581:
                return 5911;
            case 5582:
                return 5914;
            default:
                return 4629;
        }
    }
}
