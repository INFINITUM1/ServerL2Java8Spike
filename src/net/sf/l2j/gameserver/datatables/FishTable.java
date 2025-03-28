
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.FishData;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * @author -Nemesiss-
 *
 */
public class FishTable
{
	private static Logger _log = AbstractLogger.getLogger(SkillTreeTable.class.getName());
	private static FishTable _instance = new FishTable();

	private static List<FishData> _fishsNormal;
	private static List<FishData> _fishsEasy;
	private static List<FishData> _fishsHard;

	public static FishTable getInstance()
	{
		return _instance;
	}
	private FishTable()
	{
		//Create table that contains all fish datas
		int count   = 0;
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			_fishsEasy = new FastList<FishData>();
			_fishsNormal = new FastList<FishData>();
			_fishsHard = new FastList<FishData>();
			FishData fish;
			st = con.prepareStatement("SELECT id, level, name, hp, hpregen, fish_type, fish_group, fish_guts, guts_check_time, wait_time, combat_time FROM fish ORDER BY id");
			rs = st.executeQuery();
			rs.setFetchSize(50);

			while (rs.next())
			{
				int id = rs.getInt("id");
				int lvl = rs.getInt("level");
				String name = rs.getString("name");
				int hp = rs.getInt("hp");
				int hpreg = rs.getInt("hpregen");
				int type = rs.getInt("fish_type");
				int group = rs.getInt("fish_group");
				int fish_guts = rs.getInt("fish_guts");
				int guts_check_time = rs.getInt("guts_check_time");
				int wait_time = rs.getInt("wait_time");
				int combat_time = rs.getInt("combat_time");
				fish = new FishData(id, lvl, name, hp, hpreg, type, group, fish_guts, guts_check_time, wait_time, combat_time);
				switch (fish.getGroup())
				{
					case 0:
						_fishsEasy.add(fish);
						break;
					case 1:
						_fishsNormal.add(fish);
						break;
					case 2:
						_fishsHard.add(fish);
				}
			}
            count = _fishsEasy.size() + _fishsNormal.size() + _fishsHard.size();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "error while creating fishes table"+ e);
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
        _log.config("Loading FishTable... total " + count + " Fishes.");
	}
	/**
	 * @param Fish - lvl
	 * @param Fish - type
	 * @param Fish - group
	 * @return List of Fish that can be fished
	 */
	public List<FishData> getfish(int lvl, int type, int group)
	{
		List<FishData> result = new FastList<FishData>();
		List<FishData> _Fishs = null;
		switch (group) {
			case 0:
				_Fishs = _fishsEasy;
				break;
			case 1:
				_Fishs = _fishsNormal;
				break;
			case 2:
				_Fishs = _fishsHard;
		}
		if (_Fishs == null)
		{
			// the fish list is empty
			_log.warning("Fish are not defined !");
			return null;
		}
		for (FishData f : _Fishs)
		{
			if (f.getLevel()!= lvl) continue;
			if (f.getType() != type) continue;

			result.add(f);
		}
		if (result.size() == 0)	_log.warning("Cant Find Any Fish!? - Lvl: "+lvl+" Type: " +type);
		return result;
	}

}
