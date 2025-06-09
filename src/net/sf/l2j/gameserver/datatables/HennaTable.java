
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javolution.util.FastMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.templates.L2Henna;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

public class HennaTable
{
	private static Logger _log = AbstractLogger.getLogger(HennaTable.class.getName());

	private static HennaTable _instance;

	private static FastMap<Integer, L2Henna> _henna = new FastMap<Integer, L2Henna>().shared("HennaTable._henna");
	private boolean _initialized = true;

	public static HennaTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new HennaTable();
		}
		return _instance;
	}

	private HennaTable()
	{
		//_henna = new FastMap<Integer, L2Henna>().shared();
		restoreHennaData();
	}

	/**
	 *
	 */
	private void restoreHennaData()
	{
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			try 
			{
				con = L2DatabaseFactory.get();
				con.setTransactionIsolation(1);
				st = con.prepareStatement("SELECT symbol_id, symbol_name, dye_id, dye_amount, price, stat_INT, stat_STR, stat_CON, stat_MEM, stat_DEX, stat_WIT FROM henna");
				rs = st.executeQuery();
				rs.setFetchSize(50);
				fillHennaTable(rs);
			} 
			catch (Exception e) 
			{
				_log.severe("error while creating henna table " + e);
				e.printStackTrace();
			}

		} 
		finally 
		{
			Close.CSR(con, st, rs);
		}
	}

	private void fillHennaTable(ResultSet HennaData) throws Exception
	{
		while (HennaData.next())
		{
			StatsSet hennaDat = new StatsSet();
			int id = HennaData.getInt("symbol_id");

			hennaDat.set("symbol_id", id);
			//hennaDat.set("symbol_name", HennaData.getString("symbol_name"));
			hennaDat.set("dye", HennaData.getInt("dye_id"));
			hennaDat.set("price", HennaData.getInt("price"));
			//amount of dye required
			hennaDat.set("amount", HennaData.getInt("dye_amount"));
			hennaDat.set("stat_INT", HennaData.getInt("stat_INT"));
			hennaDat.set("stat_STR", HennaData.getInt("stat_STR"));
			hennaDat.set("stat_CON", HennaData.getInt("stat_CON"));
			hennaDat.set("stat_MEM", HennaData.getInt("stat_MEM"));
			hennaDat.set("stat_DEX", HennaData.getInt("stat_DEX"));
			hennaDat.set("stat_WIT", HennaData.getInt("stat_WIT"));


			L2Henna template = new L2Henna(hennaDat);
			_henna.put(id, template);
		}
		_log.config("Loading HennaTable... total " + _henna.size() + " Templates.");
	}


	public boolean isInitialized()
	{
		return _initialized;
	}


	public L2Henna getTemplate(int id)
	{
		return _henna.get(id);
	}
}
