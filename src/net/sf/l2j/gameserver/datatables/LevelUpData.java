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
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javolution.util.FastMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2LvlupData;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @author NightMarez
 * @version $Revision: 1.3.2.4.2.3 $ $Date: 2005/03/27 15:29:18 $
 */
public class LevelUpData
{
    private static String CLASS_LVL = "class_lvl";
    private static String MP_MOD = "defaultmpmod";
    private static String MP_ADD = "defaultmpadd";
    private static String MP_BASE = "defaultmpbase";
    private static String HP_MOD = "defaulthpmod";
    private static String HP_ADD = "defaulthpadd";
    private static String HP_BASE = "defaulthpbase";
    private static String CP_MOD = "defaultcpmod";
    private static String CP_ADD = "defaultcpadd";
    private static String CP_BASE = "defaultcpbase";
    private static String CLASS_ID = "classid";

    private static Logger _log = AbstractLogger.getLogger(LevelUpData.class.getName());

	private static LevelUpData _instance;

	private static FastMap<Integer, L2LvlupData> _lvlTable = new FastMap<Integer, L2LvlupData>().shared("LevelUpData._lvlTable");

	public static LevelUpData getInstance()
	{
		if (_instance == null)
		{
			_instance = new LevelUpData();
		}
		return _instance;
	}

	private LevelUpData()
	{
		//_lvlTable = new FastMap<Integer, L2LvlupData>().shared();
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT classid, defaulthpbase, defaulthpadd, defaulthpmod, defaultcpbase, defaultcpadd, defaultcpmod, defaultmpbase, defaultmpadd, defaultmpmod, class_lvl FROM lvlupgain");
			rs = st.executeQuery();
			rs.setFetchSize(50);
			L2LvlupData lvlDat;
			while (rs.next())
			{
				lvlDat = new L2LvlupData();
				lvlDat.setClassid(rs.getInt(CLASS_ID));
				lvlDat.setClassLvl(rs.getInt(CLASS_LVL));
				lvlDat.setClassHpBase(rs.getFloat(HP_BASE));
				lvlDat.setClassHpAdd(rs.getFloat(HP_ADD));
				lvlDat.setClassHpModifier(rs.getFloat(HP_MOD));
                lvlDat.setClassCpBase(rs.getFloat(CP_BASE));
                lvlDat.setClassCpAdd(rs.getFloat(CP_ADD));
                lvlDat.setClassCpModifier(rs.getFloat(CP_MOD));
				lvlDat.setClassMpBase(rs.getFloat(MP_BASE));
				lvlDat.setClassMpAdd(rs.getFloat(MP_ADD));
				lvlDat.setClassMpModifier(rs.getFloat(MP_MOD));

				_lvlTable.put(Integer.valueOf(lvlDat.getClassid()), lvlDat);
			}
			_log.config("Loading LevelUpData... total " + _lvlTable.size() + " Templates.");
		}
		catch (Exception e)
		{
			_log.warning("error while creating Lvl up data table "+e);
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
	}

	/**
	 * @param template id
	 * @return
	 */
	public L2LvlupData getTemplate(int classId)
	{
		return _lvlTable.get(classId);
	}
	public L2LvlupData getTemplate(ClassId classId)
	{
		return _lvlTable.get(classId.getId());
	}
}
