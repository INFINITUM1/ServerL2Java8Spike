/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.instancemanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
////import net.sf.l2j.gameserver.model.olympiad.OlympiadDiary;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

/**
  * @author Kerberos
  */

public class RaidBossPointsManager
{
	private final static Logger _log = AbstractLogger.getLogger(RaidBossPointsManager.class.getName());
	protected static FastMap<Integer, Map<Integer, Integer>> _points;
	protected static FastMap<Integer, Map<Integer, Integer>> _list;

	public final static void init()
	{
		_list = new FastMap<Integer, Map<Integer, Integer>>();
		FastList<Integer> _chars = new FastList<Integer>();
		Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT * FROM `character_raid_points`");
			rs = st.executeQuery();
			rs.setFetchSize(50);
			while(rs.next())
			{
				_chars.add(rs.getInt("charId"));
			}
			Close.SR(st, rs);
			for(FastList.Node<Integer> n = _chars.head(), end = _chars.tail(); (n = n.getNext()) != end;)
			{
				int charId = n.getValue();
				FastMap<Integer, Integer> values = new FastMap<Integer, Integer>();
				st = con.prepareStatement("SELECT * FROM `character_raid_points` WHERE `charId`=?");
				st.setInt(1, charId);
				rs = st.executeQuery();
				rs.setFetchSize(50);
				while(rs.next())
				{
					values.put(rs.getInt("boss_id"), rs.getInt("points"));
				}
				Close.SR(st, rs);
				_list.put(charId, values);
			}
		}
		catch (SQLException e)
		{
			_log.warning("RaidPointsManager: Couldnt load raid points ");
		}
		catch (Exception e)
		{
			_log.warning(e.getMessage());
		}
		finally
		{
			_chars.clear();
			Close.CSR(con, st, rs);
		}
	}
	public final static void loadPoints(L2PcInstance player)
	{
		if (_points == null)
			_points = new FastMap<Integer, Map<Integer, Integer>>();
		Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
		try
		{
			FastMap<Integer, Integer> tmpScore = new FastMap<Integer, Integer>();
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT boss_id,points FROM character_raid_points WHERE charId=?");
			st.setInt(1, player.getObjectId());
			rs = st.executeQuery();
			rs.setFetchSize(50);
			while (rs.next())
			{
				int raidId = rs.getInt("boss_id");
				int points = rs.getInt("points");
				tmpScore.put(raidId, points);
			}
			Close.SR(st, rs);
			_points.put(player.getObjectId(), tmpScore);
			tmpScore.clear();
		}
		catch (SQLException e)
		{
			_log.warning("RaidPointsManager: Couldnt load raid points for character :" +player.getName());
		}
		catch (Exception e)
		{
			_log.warning(e.getMessage());
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
	}

    public final static void updatePointsInDB(L2PcInstance player, int raidId, int points)
    {
		Connect con = null;
        PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("REPLACE INTO character_raid_points (`charId`,`boss_id`,`points`) VALUES (?,?,?)");
            st.setInt(1, player.getObjectId());
            st.setInt(2, raidId);
            st.setInt(3, points);
			st.executeUpdate();
        } 
		catch (Exception e) 
		{
			_log.log(Level.WARNING, "could not update char raid points:", e);
        } 
		finally
		{
            Close.CS(con, st);
        }
	}

    public final static void addPoints(L2PcInstance player, int bossId, int points)
    {
    	int ownerId = player.getObjectId();
    	Map<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
		if (_points == null)
			_points = new FastMap<Integer, Map<Integer, Integer>>();
    	tmpPoint = _points.get(ownerId);
    	if(tmpPoint == null || tmpPoint.isEmpty())
    	{
    		tmpPoint = new FastMap<Integer, Integer>();
    		tmpPoint.put(bossId, points);
    		updatePointsInDB(player, bossId, points);
    	}
    	else
    	{
    		int currentPoins = tmpPoint.containsKey(bossId) ? tmpPoint.get(bossId).intValue() : 0;
    		tmpPoint.remove(bossId);
    		tmpPoint.put(bossId, currentPoins == 0 ? points : currentPoins + points);
    		updatePointsInDB(player, bossId, currentPoins == 0 ? points : currentPoins + points);
    	}
    	_points.remove(ownerId);
    	_points.put(ownerId, tmpPoint);
    	_list.remove(ownerId);
    	_list.put(ownerId, tmpPoint);
		tmpPoint.clear();
		
		/*// дневник героя
		if (player.isHero())
		{
			String raid_name = "";
			boolean epic = false;
			switch (bossId)
			{
				case 29001:
					raid_name = "Ant Queen";
					epic = true;
					break;
				case 29028:
					raid_name = "Valakas";
					epic = true;
					break;
				case 29020:
					raid_name = "Baium";
					epic = true;
					break;
				case 29019:
					raid_name = "Antharas";
					epic = true;
					break;
				case 29006:
					raid_name = "Core";
					epic = true;
					break;
				case 29014:
					raid_name = "Orfen";
					epic = true;
					break;
				case 29022:
					raid_name = "Zaken";
					epic = true;
					break;
				case 29047:
					raid_name = "Frintezza";
					epic = true;
					break;
			}
			if (epic)
				OlympiadDiary.addRec(player, "Победа в битве с " + raid_name + ".");
		}*/
	}

	public final static int getPointsByOwnerId(int ownerId)
	{
		Map<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
		if (_points == null)
			_points = new FastMap<Integer, Map<Integer, Integer>>();
		tmpPoint = _points.get(ownerId);
		int totalPoints = 0;
		
		if (tmpPoint == null || tmpPoint.isEmpty())
			return 0;
		
		for(int bossId : tmpPoint.keySet())
		{
			totalPoints += tmpPoint.get(bossId);
		}
		return totalPoints;
	}

	public final static Map<Integer, Integer> getList(L2PcInstance player)
	{
		return _list.get(player.getObjectId());
	}

	public final static void cleanUp()
	{
		Connect con = null;
        PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE from character_raid_points WHERE charId > 0");
			st.executeUpdate();
            _points.clear();
            _points = new FastMap<Integer, Map<Integer, Integer>>();
            _list.clear();
            _list = new FastMap<Integer, Map<Integer, Integer>>();
        } catch (Exception e) 
		{
			_log.log(Level.WARNING, "could not clean raid points: ", e);
        } 
		finally
		{
            Close.CS(con, st);
        }
	}

	public final static int calculateRanking(L2PcInstance player)
	{
		Map<Integer, Integer> tmpRanking = new FastMap<Integer, Integer>();
		Map<Integer, Map<Integer, Integer>> tmpPoints = new FastMap<Integer, Map<Integer, Integer>>();
		int totalPoints;
		
		for(int ownerId : _list.keySet())
		{
			totalPoints = getPointsByOwnerId(ownerId);
			if(totalPoints != 0)
			{
				tmpRanking.put(ownerId, totalPoints);
			}
		}
		Vector<Entry<Integer, Integer>> list = new Vector<Map.Entry<Integer, Integer>>(tmpRanking.entrySet());
		tmpRanking.clear();
		
		Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>(){
			public int compare(Map.Entry<Integer, Integer> entry, Map.Entry<Integer, Integer> entry1)
			{
				return entry.getValue().equals(entry1.getValue()) ? 0 : entry.getValue() < entry1.getValue() ? 1 : -1;
			}
		});

		int ranking = 0;
		for(Map.Entry<Integer, Integer> entry : list)
		{
			Map<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
			
			if (tmpPoints.get(entry.getKey()) != null)
				tmpPoint = tmpPoints.get(entry.getKey());
			
			tmpPoint.put(-1, ranking++);
			
			tmpPoints.put(entry.getKey(), tmpPoint);
		}
		Map<Integer, Integer> rank = tmpPoints.get(player.getObjectId());
		if (rank != null)
			return rank.get(-1);
		return 0;
	}
}