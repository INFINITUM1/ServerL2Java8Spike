
package net.sf.l2j.gameserver.datatables;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

public class PetNameTable
{
	private static Logger _log = AbstractLogger.getLogger(PetNameTable.class.getName());

	private static PetNameTable _instance;

	public static PetNameTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new PetNameTable();
		}
		return _instance;
	}

	public boolean doesPetNameExist(String name, int petNpcId)
	{
		boolean result = true;
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT name FROM pets p, items i WHERE p.item_obj_id = i.object_id AND name=? AND i.item_id IN (?)");
			st.setString(1, name);

			TextBuilder cond = new TextBuilder("");
			for (int it : L2PetDataTable.getPetItemsAsNpc(petNpcId))
			{
				if (!cond.toString().equalsIgnoreCase("")) 
					cond.append(", ");
				cond.append(it);
			}
			st.setString(2, cond.toString());
			rs = st.executeQuery();
			rs.setFetchSize(50);
			result = rs.next();
		}
		catch (SQLException e)
		{
			_log.warning("could not check existing petname:"+e.getMessage());
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
		return result;
	}

    public boolean isValidPetName(String name)
    {
        boolean result = true;

        if (!isAlphaNumeric(name)) return result;

        Pattern pattern;
        try
        {
            pattern = Pattern.compile(Config.PET_NAME_TEMPLATE);
        }
        catch (PatternSyntaxException e) // case of illegal pattern
        {
        	_log.warning("ERROR : Pet name pattern of config is wrong!");
            pattern = Pattern.compile(".*");
        }
        Matcher regexp = pattern.matcher(name);
        if (!regexp.matches())
        {
            result = false;
        }
        return result;
    }

	private boolean isAlphaNumeric(String text)
	{
		boolean result = true;
		char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			if (!Character.isLetterOrDigit(chars[i]))
			{
				result = false;
				break;
			}
		}
		return result;
	}
}
