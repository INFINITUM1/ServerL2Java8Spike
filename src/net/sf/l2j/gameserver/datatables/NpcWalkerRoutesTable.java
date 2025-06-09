
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.util.FastList;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2NpcWalkerNode;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Main Table to Load Npc Walkers Routes and Chat SQL Table.<br>
 * 
 * @author Rayan RPG for L2Emu Project
 * 
 * @since 927 
 *
 */
public class NpcWalkerRoutesTable 
{
	private final static Log _log = LogFactory.getLog(SpawnTable.class.getName());

	private static NpcWalkerRoutesTable  _instance;

	private FastList<L2NpcWalkerNode> _routes = new FastList<L2NpcWalkerNode>();

	public static NpcWalkerRoutesTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new NpcWalkerRoutesTable();
			//_log.info("Initializing Walkers Routes Table.");
		}
		
		return _instance;
	}

	private NpcWalkerRoutesTable()
	{
	}
	//FIXME: NPE while loading. :S
	public void load()
	{
		_routes.clear();
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT route_id, npc_id, move_point, chatText, move_x, move_y, move_z, delay, running FROM walker_routes");
			rs = st.executeQuery();
			rs.setFetchSize(50);
			L2NpcWalkerNode  route;
			while (rs.next())
			{
				route = new L2NpcWalkerNode();
				route.setRouteId(rs.getInt("route_id"));
				route.setNpcId(rs.getInt("npc_id"));
				route.setMovePoint(rs.getString("move_point"));
				route.setChatText(rs.getString("chatText"));
				
				route.setMoveX(rs.getInt("move_x"));
				route.setMoveY(rs.getInt("move_y"));
				route.setMoveZ(rs.getInt("move_z"));
				route.setDelay(rs.getInt("delay"));
				route.setRunning(rs.getBoolean("running"));

				_routes.add(route);
			}
		}
		catch (Exception e) 
		{
			_log.fatal("WalkerRoutesTable: Error while loading Npc Walkers Routes: "+e.getMessage());
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
		_log.info("Loading WalkerRoutesTable... total " + _routes.size() + " Routes.");
	}
	
	public FastList<L2NpcWalkerNode> getRouteForNpc(int id)
	{
		FastList<L2NpcWalkerNode> _return = new FastList<L2NpcWalkerNode>();
		
		for (FastList.Node<L2NpcWalkerNode> n = _routes.head(), end = _routes.tail(); (n = n.getNext()) != end;) 
		{
	        if(n.getValue().getNpcId() == id)
	        	_return.add(n.getValue());
	    }
		
		return _return;
	}
}
