package net.sf.l2j.gameserver.instancemanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

public class ServerVariables
{
	private static StatsSet server_vars = null;

	private static StatsSet getVars()
	{
		if(server_vars == null)
		{
			server_vars = new StatsSet();
			loadFromDB();
		}
		return server_vars;
	}

	private static void loadFromDB()
	{
		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			statement = con.prepareStatement("SELECT * FROM server_variables");
			rs = statement.executeQuery();
			while(rs.next())
				server_vars.set(rs.getString("name"), rs.getString("value"));
		}
		catch(final Exception e)
		{}
		finally
		{
			Close.CSR(con, statement, rs);
		}
	}

	private static void SaveToDB(final String name)
	{
		Connect con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.get();
			final String value = server_vars.getString(name, "");
			if(value.isEmpty())
			{
				statement = con.prepareStatement("DELETE FROM server_variables WHERE name = ?");
				statement.setString(1, name);
				statement.execute();
			}
			else
			{
				statement = con.prepareStatement("REPLACE INTO server_variables (name, value) VALUES (?,?)");
				statement.setString(1, name);
				statement.setString(2, value);
				statement.execute();
			}
		}
		catch(final Exception e)
		{}
		finally
		{
			Close.CS(con, statement);
		}
	}

	public static boolean getBool(final String name)
	{
		return getVars().getBool(name);
	}

	public static boolean getBool(final String name, final boolean _defult)
	{
		return getVars().getBool(name, _defult);
	}

	public static int getInt(final String name)
	{
		return getVars().getInteger(name);
	}

	public static int getInt(final String name, final int _defult)
	{
		return getVars().getInteger(name, _defult);
	}

	public static long getLong(final String name)
	{
		return getVars().getLong(name);
	}

	public static long getLong(final String name, final long _defult)
	{
		return getVars().getLong(name, _defult);
	}

	public static float getFloat(final String name)
	{
		return getVars().getFloat(name);
	}

	public static float getFloat(final String name, final float _defult)
	{
		return getVars().getFloat(name, _defult);
	}

	public static String getString(final String name)
	{
		return getVars().getString(name);
	}

	public static String getString(final String name, final String _defult)
	{
		return getVars().getString(name, _defult);
	}

	public static void set(final String name, final boolean value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void set(final String name, final int value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void set(final String name, final long value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void set(final String name, final double value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void set(final String name, final String value)
	{
		getVars().set(name, value);
		SaveToDB(name);
	}

	public static void unset(final String name)
	{
		getVars().unset(name);
		SaveToDB(name);
	}
}