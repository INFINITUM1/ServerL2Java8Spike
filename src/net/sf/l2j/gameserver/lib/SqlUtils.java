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
package net.sf.l2j.gameserver.lib;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.util.log.AbstractLogger;

public class SqlUtils
{
	private static Logger _log = AbstractLogger.getLogger(SqlUtils.class.getName());

    // =========================================================
    // Data Field
	private static SqlUtils _instance;

    // =========================================================
    // Property - Public
	public static SqlUtils getInstance()
	{
        if (_instance == null) _instance = new SqlUtils();
		return _instance;
	}

    // =========================================================
    // Method - Public
	public static Integer getIntValue(String resultField, String tableName, String whereClause)
	{
        String query = "";
		Integer res = null;

		PreparedStatement s = null;
		ResultSet rs = null;

		try
		{
            query = L2DatabaseFactory.prepQuerySelect(new String[] {resultField}, tableName, whereClause, true);

			s = L2DatabaseFactory.get().prepareStatement(query);
			rs = s.executeQuery();

			if(rs.next()) res = rs.getInt(1);
		}
		catch(Exception e)
		{
			_log.warning("Error in query '" + query + "':"+e);
			e.printStackTrace();
		}
		finally
		{
			Close.SR(s,rs);
		}

		return res;
	}

    public static Integer[] getIntArray(String resultField, String tableName, String whereClause)
    {
        String query = "";
        Integer[] res = null;

        PreparedStatement s = null;
        ResultSet rs = null;

        try
        {
            query = L2DatabaseFactory.prepQuerySelect(new String[] {resultField}, tableName, whereClause, false);
            s = L2DatabaseFactory.get().prepareStatement(query);
            rs = s.executeQuery();

            int rows = 0;

            while (rs.next())
                rows++;

            if (rows == 0) return new Integer[0];

            res = new Integer[rows-1];

            rs.first();

            int row = 0;
            while (rs.next())
            {
                res[row] = rs.getInt(1);
            }
        }
        catch(Exception e)
        {
            _log.warning("mSGI: Error in query '" + query + "':"+e);
            e.printStackTrace();
        }
        finally
        {
			Close.SR(s,rs);
        }

        return res;
    }

	public static Integer[][] get2DIntArray(String[] resultFields, String usedTables, String whereClause)
	{
		long start = System.currentTimeMillis();

        String query = "";

		PreparedStatement s = null;
		ResultSet rs = null;

		Integer res[][] = null;

		try
		{
            query = L2DatabaseFactory.prepQuerySelect(resultFields, usedTables, whereClause, false);
            s = L2DatabaseFactory.get().prepareStatement(query);
			rs = s.executeQuery();

			int rows = 0;
			while(rs.next())
				rows++;

			res = new Integer[rows-1][resultFields.length];

			rs.first();

			int row = 0;
			while(rs.next())
			{
				for(int i=0; i<resultFields.length; i++)
			 		res[row][i] = rs.getInt(i+1);
				row++;
			}
		}
		catch(Exception e)
		{
			_log.warning("Error in query '" + query + "':"+e);
			e.printStackTrace();
		}
		finally
		{
			Close.SR(s,rs);
		}

		_log.fine("Get all rows in query '" + query + "' in " + (System.currentTimeMillis()-start) + "ms");
		return res;
	}
}
