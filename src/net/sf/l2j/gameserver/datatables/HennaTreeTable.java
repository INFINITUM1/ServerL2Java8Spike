
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javolution.util.FastMap;
import java.util.logging.Logger;
import javolution.util.FastTable;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.templates.L2Henna;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

public class HennaTreeTable
{
	private static Logger _log = AbstractLogger.getLogger(HennaTreeTable.class.getName());
	private static HennaTreeTable _instance = new HennaTreeTable();
	private static FastMap<ClassId, FastTable<L2HennaInstance>> _hennaTrees;//= new FastMap<ClassId, FastTable<L2HennaInstance>>().shared();;
	private boolean _initialized = true;

	public static HennaTreeTable getInstance()
	{
		return _instance;
	}

	private HennaTreeTable()
	{
		_hennaTrees = new FastMap<ClassId, FastTable<L2HennaInstance>>().shared("HennaTreeTable._hennaTrees");
		int classId = 0;
        int count   = 0;
		Connect con = null;
		PreparedStatement st = null;
		PreparedStatement st2 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT class_name, id, parent_id FROM class_list ORDER BY id");
			rs = st.executeQuery();
			rs.setFetchSize(50);
			FastTable<L2HennaInstance> table = new FastTable<L2HennaInstance>();
			//int parentClassId;
			//L2Henna henna;
			while (rs.next())
			{
				table = new FastTable<L2HennaInstance>();
				classId = rs.getInt("id");
				st2 = con.prepareStatement("SELECT class_id, symbol_id FROM henna_trees where class_id=? ORDER BY symbol_id");
				st2.setInt(1, classId);
				rs2 = st2.executeQuery();

				while (rs2.next())
				{
					int id = rs2.getInt("symbol_id");
					//String name = rs2.getString("name");
					L2Henna template = HennaTable.getInstance().getTemplate(id);
                    if(template == null)
                    {
						Close.SR(st2, rs2);
						Close.SR(st, rs);
                        return;
                    }
			    	L2HennaInstance temp = new L2HennaInstance(template);
					temp.setSymbolId(id);
					temp.setItemIdDye(template.getDyeId());
					temp.setAmountDyeRequire(template.getAmountDyeRequire());
					temp.setPrice(template.getPrice());
					temp.setStatINT(template.getStatINT());
					temp.setStatSTR(template.getStatSTR());
					temp.setStatCON(template.getStatCON());
					temp.setStatMEM(template.getStatMEM());
					temp.setStatDEX(template.getStatDEX());
					temp.setStatWIT(template.getStatWIT());

					table.add(temp);
				}
				_hennaTrees.put(ClassId.values()[classId], table);
				Close.SR(st2, rs2);
                count += table.size();
				_log.fine("Henna Tree for Class: " + classId + " has " + table.size() + " Henna Templates.");
			}
			//table.clear();
			//table = null;
		}
		catch (Exception e)
		{
			_log.warning("error while creating henna tree for classId "+classId + "  "+e);
			e.printStackTrace();
		}
		finally
		{
			Close.SR(st2, rs2);
			Close.CSR(con, st, rs);
		}

        _log.config("Loading HennaTreeTable... total " + count + " Templates.");
	}

	public FastTable<L2HennaInstance> getAvailableHenna(ClassId classId)
	{
		/*FastTable<L2HennaInstance> henna = new FastTable<L2HennaInstance>();// = _hennaTrees.get(classId);
		henna.addAll(_hennaTrees.get(classId));
		if (henna.isEmpty())
		{
			// the rs2 for this class is undefined, so we give an empty list
			_log.warning("Hennatree for class " + classId + " is not defined !");
			return henna;
		}
		FastTable<L2HennaInstance> result = new FastTable<L2HennaInstance>();

		for (int i = 0; i < henna.size(); i++)
		{
			L2HennaInstance temp = henna.get(i);
			result.add(temp);
		}
		henna.clear();
		henna = null;
		return result;//.toArray(new L2HennaInstance[result.size()]);*/
		//System.out.println("##getAvailableHenna##" + classId + "####");
		return _hennaTrees.get(classId);
	}

	public boolean isInitialized()
	{
		return _initialized;
	}
}
