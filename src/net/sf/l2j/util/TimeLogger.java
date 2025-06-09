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

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeLogger {

    private static SimpleDateFormat _sdf = new SimpleDateFormat("dd (HH:mm:ss)");

    public static String getLogTime() {
        return "#";//_sdf.format(_date)+"#";
    }

    public static String getTime() {
        return _sdf.format(new Date()) + "#";
    }
}
