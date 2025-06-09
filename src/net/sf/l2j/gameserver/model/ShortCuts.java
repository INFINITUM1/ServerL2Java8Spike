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
import java.util.Map;
//import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javolution.util.FastTable;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.templates.L2EtcItemType;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:33 $
 */
public class ShortCuts
{
    private static Logger _log = AbstractLogger.getLogger(ShortCuts.class.getName());

    private L2PcInstance _owner;
    private Map<Integer, L2ShortCut> _shortCuts = new ConcurrentHashMap<Integer, L2ShortCut>();

    //private Map<String, L2ShortCut> _toSave = new ConcurrentHashMap<String, L2ShortCut>();
    //private Map<String, L2ShortCut> _toRemove = new ConcurrentHashMap<String, L2ShortCut>();

    public ShortCuts(L2PcInstance owner)
    {
        _owner = owner;
    }

    public FastTable<L2ShortCut> getAllShortCuts()
    {
		FastTable <L2ShortCut> sc = new FastTable <L2ShortCut>();
		for (L2ShortCut shc: _shortCuts.values())
		{
			if (shc == null)
				continue;
			sc.add(shc);
		}
		/*for (FastMap.Entry<Integer, L2ShortCut> e = _shortCuts.head(), end = _shortCuts.tail(); (e = e.getNext()) != end;) 
		{
			//String key = e.getKey(); // No typecast necessary.
			L2ShortCut value = e.getValue(); // No typecast necessary.
			if (value == null)
				continue;
			sc.add(e.getValue());
		}*/
		return sc;//_shortCuts.values().toArray(new L2ShortCut[_shortCuts.values().size()]);
    }

    public L2ShortCut getShortCut(int slot, int page)
    {
		L2ShortCut sc = _shortCuts.get(slot + page * 12);
		if (sc == null)
			return null;
		// verify shortcut
		if (sc.getType() == L2ShortCut.TYPE_ITEM)
        {
			if (_owner.getInventory().getItemByObjectId(sc.getId()) == null)
            {
				deleteShortCut(sc.getSlot(), sc.getPage());
				sc = null;
			}
		}
		return sc;
    }

    public void registerShortCut(L2ShortCut shortcut)
    {
        _shortCuts.put(shortcut.getSlot() + 12 * shortcut.getPage(), shortcut);
        //L2ShortCut oldShortCut = _shortCuts.put(shortcut.getSlot() + 12 * shortcut.getPage(), shortcut);
        //registerShortCutInDb(shortcut, oldShortCut);
    }

   /* private void registerShortCutInDb(L2ShortCut shortcut, L2ShortCut oldShortCut)
    {
        if (oldShortCut != null)
            deleteShortCutFromDb(oldShortCut);

		//_shortCuts.put(shortcut.getSlot() + "_" + shortcut.getPage(), shortcut);
		Connect con = null;
		PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.getConnection();
            st = con.prepareStatement("REPLACE INTO character_shortcuts (char_obj_id,slot,page,type,shortcut_id,level,class_index) values(?,?,?,?,?,?,?)");
            st.setInt(1, _owner.getObjectId());
            st.setInt(2, shortcut.getSlot());
            st.setInt(3, shortcut.getPage());
            st.setInt(4, shortcut.getType());
            st.setInt(5, shortcut.getId());
            st.setInt(6, shortcut.getLevel());
            st.setInt(7, _owner.getClassIndex());
            st.execute();
        }
        catch (Exception e)
        {
			_log.warning("Could not store character shortcut: " + e);
        }
        finally
        {
            Close.CS(con, st);
        }
    }*/

    /**
     * @param slot
     */
    public void deleteShortCut(int slot, int page)
    {
		if (_owner == null)
			return;

        L2ShortCut old = _shortCuts.remove(slot+page*12);
		if (old == null)
			return;

		//deleteShortCutFromDb(old);
		if (old.getType() == L2ShortCut.TYPE_ITEM)
		{
			L2ItemInstance item = _owner.getInventory().getItemByObjectId(old.getId());

	        if ((item != null) && (item.getItemType() == L2EtcItemType.SHOT))
	        {
	        	_owner.removeAutoSoulShot(item.getItemId());
	            _owner.sendPacket(new ExAutoSoulShot(item.getItemId(), 0));
	        }
		}

        _owner.sendPacket(new ShortCutInit(_owner));

        for (int shotId : _owner.getAutoSoulShot().values())
            _owner.sendPacket(new ExAutoSoulShot(shotId, 1));
    }

    public void deleteShortCutByObjectId(int objectId)
    {
        L2ShortCut toRemove = null;
        for (L2ShortCut shortcut : _shortCuts.values())
        {
            if (shortcut.getType() == L2ShortCut.TYPE_ITEM && shortcut.getId() == objectId)
            {
                toRemove = shortcut;
                break;
            }
        }

        if (toRemove != null)
            deleteShortCut(toRemove.getSlot(), toRemove.getPage());
    }

    /**
     * @param shortcut
     */
    /*private void deleteShortCutFromDb(L2ShortCut shortcut)
    {
		_toRemove.put(shortcut.getSlot() + "_" + shortcut.getPage(), shortcut);
		Connect con = null;
		PreparedStatement st = null;
        try
        {
            con = L2DatabaseFactory.getConnection();
            st = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND slot=? AND page=? AND class_index=?");
            st.setInt(1, _owner.getObjectId());
            st.setInt(2, shortcut.getSlot());
            st.setInt(3, shortcut.getPage());
            st.setInt(4, _owner.getClassIndex());
            st.execute();
        }
        catch (Exception e)
        {
			_log.warning("Could not delete character shortcut: " + e);
        }
        finally
        {
            Close.CS(con, st);
        }
    }*/

    public void restore(Connect con)
    {
        _shortCuts.clear();
		//Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
        try
        {
            //con = L2DatabaseFactory.getConnection();
			//con.setTransactionIsolation(1);		
            st = con.prepareStatement("SELECT char_obj_id, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE char_obj_id=? AND class_index=?");
            st.setInt(1, _owner.getObjectId());
            st.setInt(2, _owner.getClassIndex());
            rs = st.executeQuery();
            while (rs.next())
            {
                int slot = rs.getInt("slot");
                int page = rs.getInt("page");
                int type = rs.getInt("type");
                int id = rs.getInt("shortcut_id");
                int level = rs.getInt("level");

                //L2ShortCut sc = new L2ShortCut(slot, page, type, id, level, 1);
                _shortCuts.put(slot+page*12, new L2ShortCut(slot, page, type, id, level, 1));
            }
        }
        catch (Exception e)
        {
			_log.warning("Could not restore character shortcuts: " + e);
        }
        finally
        {
            Close.SR(st, rs);
        }

		// verify shortcuts
		for (L2ShortCut sc : _shortCuts.values())
        {
			if (sc == null)
				continue;

			if (sc.getType() == L2ShortCut.TYPE_ITEM)
            {
				if (_owner.getInventory().getItemByObjectId(sc.getId()) == null)
					deleteShortCut(sc.getSlot(), sc.getPage());
            }
		}
    }

	public void store(Connect con)
	{
		//Connect con = null;
		ResultSet rs = null;
		PreparedStatement st = null;
        try
        {
            //con = L2DatabaseFactory.getConnection();
			con.setAutoCommit(false);

			/*st = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND slot=? AND page=? AND class_index=?");
			for (L2ShortCut sc : _toRemove.values())
			{
				st.setInt(1, _owner.getObjectId());
				st.setInt(2, sc.getSlot());
				st.setInt(3, sc.getPage());
				st.setInt(4, _owner.getClassIndex());
                st.addBatch(); //
			}
			st.executeBatch();
			Close.S(st);*/
			st = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?");
			st.setInt(1, _owner.getObjectId());
			st.setInt(2, _owner.getClassIndex());
			st.execute();
			Close.S(st);

			st = con.prepareStatement("REPLACE INTO character_shortcuts (char_obj_id,slot,page,type,shortcut_id,level,class_index) values(?,?,?,?,?,?,?)");
			for (L2ShortCut sc : _shortCuts.values())
			{
				st.setInt(1, _owner.getObjectId());
				st.setInt(2, sc.getSlot());
				st.setInt(3, sc.getPage());
				st.setInt(4, sc.getType());
				st.setInt(5, sc.getId());
				st.setInt(6, sc.getLevel());
				st.setInt(7, _owner.getClassIndex());
                st.addBatch(); //
			}
			st.executeBatch();
			con.commit();
			con.setAutoCommit(true);
        }
        catch (Exception e)
        {
			_log.warning("Could not store character shortcuts: " + e);
        }
        finally
        {
            Close.SR(st, rs);
        }
	}
}
