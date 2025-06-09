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
package net.sf.l2j.gameserver.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World; 
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

public class Moderator
{
	private static final Moderator _instance = new Moderator();
	
    private Moderator()
    {
		//
    }

    public static Moderator getInstance()
    {
        return _instance;
    }
	
	public boolean isModer(int ObjId)
	{
		int rank = 0;
		
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.get();
			st = con.prepareStatement("SELECT rank FROM z_moderator WHERE moder = ? LIMIT 1");
			st.setInt(1, ObjId);
			rset = st.executeQuery();
			while (rset.next())
			{
				rank = rset.getInt("rank");
				if (rank > 0)
					return true;
			}
			Close.SR(st,rset);
		}
		catch (Exception e)
		{
			System.out.println("could not check Moder status: "+e);
		}
		finally
		{
			Close.CSR(con,st,rset);
		}
		
		return false;
	}
	
	public int getRank(int ObjId)
	{
		int rank = 0;
		
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.get();
			st = con.prepareStatement("SELECT rank FROM z_moderator WHERE moder = ? LIMIT 1");
			st.setInt(1, ObjId);
			rset = st.executeQuery();
			while (rset.next())
			{
				rank = rset.getInt("rank");
			}
			Close.SR(st,rset);
		}
		catch (Exception e)
		{
			System.out.println("could not get Moder rank: "+e);
		}
		finally
		{
			Close.CSR(con,st,rset);
		}
		
		return rank;
	}
	
	public String getForumName(int ObjId)
	{
		String forumName = "Не указано";
		
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.get();
			st = con.prepareStatement("SELECT * FROM z_moderator WHERE moder=? LIMIT 1");
			st.setInt(1, ObjId);
			rset = st.executeQuery();
			while (rset.next())
			{
				forumName = rset.getString("name");
			}
			Close.SR(st,rset);
		}
		catch (Exception e)
		{
			System.out.println("could not get Moder name: "+e);
		}
		finally
		{
			Close.CSR(con,st,rset);
		}
		
		return forumName;
	}
	
	public void logWrite(String Moderator, String Action)
	{
		Connect con = null;
		PreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.get();
			st = con.prepareStatement("INSERT INTO z_moderator_log (moder,action,date) VALUES (?,?,?)");
			st.setString(1, Moderator);
			st.setString(2, Action);
			st.setString(3, getDate());
			st.execute();
		}
		catch (Exception e) 
		{ 
			System.out.println("Could not set max online"); 
		}
		finally 
		{
			Close.CS(con,st);
		}
	}
	
	public static String getDate()
	{
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd (HH:mm:ss)");
		
		return sdf.format(date);
	}
}
