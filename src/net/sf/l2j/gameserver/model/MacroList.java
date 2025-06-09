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
package net.sf.l2j.gameserver.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2Macro.L2MacroCmd;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SendMacroList;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.2 $ $Date: 2005/03/02 15:38:41 $
 */
public class MacroList
{
    private static Logger _log = AbstractLogger.getLogger(MacroList.class.getName());

    private L2PcInstance _owner;
	private int _revision;
	private int _macroId;
    private Map<Integer, L2Macro> _macroses = new FastMap<Integer, L2Macro>();

    private Map<Integer, L2Macro> _toSave = new ConcurrentHashMap<Integer, L2Macro>();
    private ConcurrentLinkedQueue<Integer> _toRemove = new ConcurrentLinkedQueue<Integer>();

    public MacroList(L2PcInstance owner)
    {
        _owner = owner;
		_revision = 1;
		_macroId = 1000;
    }

	public int getRevision() {
		return _revision;
	}

    public L2Macro[] getAllMacroses()
    {
		return _macroses.values().toArray(new L2Macro[_macroses.size()]);
    }

    public L2Macro getMacro(int id)
    {
        return _macroses.get(id-1);
    }

    public void registerMacro(L2Macro macro)
    {
		if (macro.id == 0) {
			macro.id = _macroId++;
			while (_macroses.get(macro.id) != null)
				macro.id = _macroId++;
			_macroses.put(macro.id, macro);
			registerMacroInDb(macro);
		} else {
			L2Macro old = _macroses.put(macro.id, macro);
			if (old != null)
				deleteMacroFromDb(old);
			registerMacroInDb(macro);
		}
		sendUpdate();
    }

    public void deleteMacro(int id)
    {
        L2Macro macro = _macroses.get(id);
        if(macro != null)
		{
            deleteMacroFromDb(macro);
			_owner.sendMessage("Макрос " + macro.name + " удален");
		}
		_macroses.remove(id);

		/*FastTable <L2ShortCut> allShortCuts = new FastTable<L2ShortCut>();
		allShortCuts.addAll(_owner.getAllShortCuts());
		
		for (int i = 0, n = allShortCuts.size(); i < n; i++)
		{
		    L2ShortCut sc = allShortCuts.get(i);
			if (sc == null)
				continue;
				
			if (sc.getId() == id && sc.getType() == L2ShortCut.TYPE_MACRO)
				_owner.deleteShortCut(sc.getSlot(), sc.getPage());
		}
		allShortCuts.clear();*/
		for (L2ShortCut sc : _owner.getAllShortCuts())
		{
			if (sc == null)
				continue;

			if (sc.getId() == id && sc.getType() == L2ShortCut.TYPE_MACRO)
				_owner.deleteShortCut(sc.getSlot(), sc.getPage());
		}
		sendUpdate();
    }

	public void sendUpdate() {
		_revision++;
		L2Macro[] all = getAllMacroses();
		if (all.length == 0) {
			_owner.sendPacket(new SendMacroList(_revision, all.length, null));
		} else {
			for (L2Macro m : all) {
				_owner.sendPacket(new SendMacroList(_revision, all.length, m));
			}
		}
	}

    private void registerMacroInDb(L2Macro macro)
    {
		_toSave.put(macro.id, macro);
        /*TextBuilder tb = new TextBuilder();
		Connect con = null;
		PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.getConnection();
            st = con.prepareStatement("INSERT INTO character_macroses (char_obj_id,id,icon,name,descr,acronym,commands) values(?,?,?,?,?,?,?)");
			st.setInt(1, _owner.getObjectId());
            st.setInt(2, macro.id);
            st.setInt(3, macro.icon);
            st.setString(4, macro.name);
            st.setString(5, macro.descr);
            st.setString(6, macro.acronym);
			for (L2MacroCmd cmd : macro.commands) 
			{
				tb.append(cmd.type).append(',');
				tb.append(cmd.d1).append(',');
				tb.append(cmd.d2);
				if (cmd.cmd != null && cmd.cmd.length() > 0)
					tb.append(',').append(cmd.cmd);
				tb.append(';');
			}
			String lenta = tb.toString();
			if (lenta.length() > 255)
				lenta = lenta.substring(255);
            st.setString(7, lenta);
            st.execute();
        }
        catch (Exception e)
        {
			_log.log(Level.WARNING, "could not store macro:", e);
        }
        finally
        {
            Close.CS(con, st);
			tb.clear();
			tb = null;
        }*/
    }

    /**
     * @param shortcut
     */
    private void deleteMacroFromDb(L2Macro macro)
    {
		_toRemove.add(macro.id);
		/*Connect con = null;
		PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.getConnection();
            st = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=? AND id=?");
            st.setInt(1, _owner.getObjectId());
            st.setInt(2, macro.id);
            st.execute();
            st.close();
        }
        catch (Exception e)
        {
			_log.log(Level.WARNING, "could not delete macro:", e);
        }
        finally
        {
            Close.CS(con, st);
        }*/
    }

    public void restore(Connect con)
    {
		_macroses.clear();
		//Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
        try
        {
            //con = L2DatabaseFactory.getConnection();
			//con.setTransactionIsolation(1);		
            st = con.prepareStatement("SELECT char_obj_id, id, icon, name, descr, acronym, commands FROM character_macroses WHERE char_obj_id=?");
            st.setInt(1, _owner.getObjectId());
            rs = st.executeQuery();
            while (rs.next())
            {
				int id = rs.getInt("id");
                int icon = rs.getInt("icon");
                String name = rs.getString("name");
                String descr = rs.getString("descr");
                String acronym = rs.getString("acronym");
				List<L2MacroCmd> commands = new FastList<L2MacroCmd>();
				StringTokenizer st1 = new StringTokenizer(rs.getString("commands"),";");
				while (st1.hasMoreTokens()) 
				{
					StringTokenizer stk = new StringTokenizer(st1.nextToken(),",");
					if(stk.countTokens() < 3)
						continue;
					int type = Integer.parseInt(stk.nextToken());
					int d1 = Integer.parseInt(stk.nextToken());
					int d2 = Integer.parseInt(stk.nextToken());
					String cmd = "";
					if (stk.hasMoreTokens())
						cmd = stk.nextToken();
					//L2MacroCmd mcmd = new L2MacroCmd(commands.size(), type, d1, d2, cmd);
					commands.add(new L2MacroCmd(commands.size(), type, d1, d2, cmd));
				}

				//L2Macro m = new L2Macro(id, icon, name, descr, acronym, commands.toArray(new L2MacroCmd[commands.size()]));
				_macroses.put(id, new L2Macro(id, icon, name, descr, acronym, commands.toArray(new L2MacroCmd[commands.size()])));
            }
        }
        catch (Exception e)
        {
			_log.log(Level.WARNING, "could not restore character_macroses:", e);
        }
        finally
        {
            Close.SR(st, rs);
        }
    }
	
	public void store(Connect con)
	{
		//Connect con = null;
		ResultSet rs = null;
		PreparedStatement st = null;
        TextBuilder tb = new TextBuilder();
        try
        {
            //con = L2DatabaseFactory.getConnection();
			con.setAutoCommit(false);

			st = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=? AND id=?");
			for (Integer macro_id : _toRemove)
			{
				st.setInt(1, _owner.getObjectId());
				st.setInt(2, macro_id);
                st.addBatch(); //
			}
			st.executeBatch();
			Close.S(st);

			st = con.prepareStatement("REPLACE INTO character_macroses (char_obj_id,id,icon,name,descr,acronym,commands) values(?,?,?,?,?,?,?)");
			for (L2Macro macro : _toSave.values())
			{
				st.setInt(1, _owner.getObjectId());
				st.setInt(2, macro.id);
				st.setInt(3, macro.icon);
				st.setString(4, macro.name);
				st.setString(5, macro.descr);
				st.setString(6, macro.acronym);
				for (L2MacroCmd cmd : macro.commands) 
				{
					tb.append(cmd.type).append(',');
					tb.append(cmd.d1).append(',');
					tb.append(cmd.d2);
					if (cmd.cmd != null && cmd.cmd.length() > 0)
						tb.append(',').append(cmd.cmd);
					tb.append(';');
				}
				String lenta = tb.toString();
				if (lenta.length() > 255)
					lenta = lenta.substring(255);
				st.setString(7, lenta);
                st.addBatch(); //
				tb.clear();
			}
			st.executeBatch();
			con.commit();
			con.setAutoCommit(true);
        }
        catch (Exception e)
        {
			_log.log(Level.WARNING, "could not store character_macroses:", e);
        }
        finally
        {
            Close.SR(st, rs);
			tb.clear();
			tb = null;
        }
	}
}
