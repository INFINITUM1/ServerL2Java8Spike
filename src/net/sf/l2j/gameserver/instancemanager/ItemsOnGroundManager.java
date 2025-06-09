/* This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.instancemanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ItemsAutoDestroy;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.templates.L2EtcItemType;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class manage all items on ground
 *
 * @version $Revision: $ $Date: $
 * @author  DiezelMax - original ideea
 * @author  Enforcer  - actual build
 */
public class ItemsOnGroundManager
{
    static final Logger _log = AbstractLogger.getLogger(ItemsOnGroundManager.class.getName());
    private static ItemsOnGroundManager _instance;
    protected List<L2ItemInstance> _items = null;

    private ItemsOnGroundManager()
    {
    	if(!Config.SAVE_DROPPED_ITEM) return;
    		_items = new FastList<L2ItemInstance>();
    	if (Config.SAVE_DROPPED_ITEM_INTERVAL >0)
    			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new storeInDb(), Config.SAVE_DROPPED_ITEM_INTERVAL, Config.SAVE_DROPPED_ITEM_INTERVAL);
    }

    public static final ItemsOnGroundManager getInstance()
    {
        return _instance;
    }

    public static void init()
    {
        _instance = new ItemsOnGroundManager();
        _instance.load();
    }

    private void load()
    {
    	// If SaveDroppedItem is false, may want to delete all items previously stored to avoid add old items on reactivate
        if(!Config.SAVE_DROPPED_ITEM && Config.CLEAR_DROPPED_ITEM_TABLE)
        	emptyTable();


        if(!Config.SAVE_DROPPED_ITEM)
        	return;

        // if DestroyPlayerDroppedItem was previously  false, items curently protected will be added to ItemsAutoDestroy
        if (Config.DESTROY_DROPPED_PLAYER_ITEM)
        {
        	Connect con = null;
			PreparedStatement st = null;
            try
            {
            	String str = null;
				if (!Config.DESTROY_EQUIPABLE_PLAYER_ITEM) //Recycle misc. items only
            		str = "update itemsonground set drop_time=? where drop_time=-1 and equipable=0";
				else if (Config.DESTROY_EQUIPABLE_PLAYER_ITEM) //Recycle all items including equipable
           		 	str = "update itemsonground set drop_time=? where drop_time=-1";
				con = L2DatabaseFactory.get();
				st = con.prepareStatement(str);
				st.setLong(1, System.currentTimeMillis());
				st.execute();
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE,"error while updating table ItemsOnGround " + e);
                e.printStackTrace();
            }
            finally
            {
            	Close.CS(con,st);
            }
        }

        //Add items to world
        Connect con = null;
		Statement s = null;
        ResultSet result = null;
        try
        {
            con = L2DatabaseFactory.get();
            s = con.createStatement();
            int count=0;
            result = s.executeQuery("select object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable from itemsonground");
            while (result.next())
            {
                L2ItemInstance item = new L2ItemInstance(result.getInt(1), result.getInt(2));
                L2World.getInstance().storeObject(item);
                if (item.isStackable() && result.getInt(3) > 1) //this check and..
                	item.setCount(result.getInt(3));
                if (result.getInt(4) > 0)			// this, are really necessary?
                	item.setEnchantLevel(result.getInt(4));
                item.getPosition().setWorldPosition(result.getInt(5), result.getInt(6) ,result.getInt(7));
                item.getPosition().setWorldRegion(L2World.getInstance().getRegion(item.getPosition().getWorldPosition()));
                item.getPosition().getWorldRegion().addVisibleObject(item);
                item.setDropTime(result.getLong(8));
                if (result.getLong(8) == -1)
                	item.setProtected(true);
                else
                	item.setProtected(false);
                item.setIsVisible(true);
                L2World.getInstance().addVisibleObject(item, item.getPosition().getWorldRegion(), null);
                _items.add(item);
                count++;
                // add to ItemsAutoDestroy only items not protected
                if (!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId())){
                	if(result.getLong(8) > -1)
                	{
                		if((Config.AUTODESTROY_ITEM_AFTER > 0 && item.getItemType() != L2EtcItemType.HERB)
                		   ||(Config.HERB_AUTO_DESTROY_TIME > 0 && item.getItemType() == L2EtcItemType.HERB))
                			ItemsAutoDestroy.getInstance().addItem(item);
                	}
                }
            }
            Close.R(result);
            Close.S2(s);
            if (count > 0)
				_log.info("ItemsOnGroundManager: restored " + count + " items.");
            else
            	_log.info("Initializing ItemsOnGroundManager.");
        } 
		catch (Exception e) 
		{
            _log.log(Level.SEVERE,"error while loading ItemsOnGround " + e);
            e.printStackTrace();
        }
		finally 
		{
            Close.S2(s);
            Close.R(result);
            Close.C(con);
        }
        if (Config.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD)
        	emptyTable();
    }

    public void save(L2ItemInstance item)
    {
        if(!Config.SAVE_DROPPED_ITEM) return;
        _items.add(item);
    }

    public void removeObject(L2Object item)
    {
        if(!Config.SAVE_DROPPED_ITEM) return;
        _items.remove(item);
    }

    public void saveInDb()
    {
    	new storeInDb().run();
    }

    public void cleanUp()
    {
    	_items.clear();
    }

    public void emptyTable()
    {
    	Connect con = null;
		PreparedStatement del = null;
        try
		{
			con = L2DatabaseFactory.get();
			del = con.prepareStatement("delete from itemsonground");
            del.execute();
		} 
		catch (Exception e1)
		{
			 _log.log(Level.SEVERE,"error while cleaning table ItemsOnGround " + e1);
			e1.printStackTrace();
		}
		finally 
		{
			Close.CS(con,del);
        }
    }

    protected class storeInDb extends Thread
    {
    	@Override
		public void run()
        {
			if(!Config.SAVE_DROPPED_ITEM) 
				return;

			emptyTable();

			if (_items.isEmpty())
				return;
				
			Connect con = null;
			PreparedStatement st = null;
			try 
			{
				con = L2DatabaseFactory.get();
				for (L2ItemInstance item: _items)
				{
					if (CursedWeaponsManager.getInstance().isCursed(item.getItemId())) 
						continue; // Cursed Items not saved to ground, prevent double save

					st = con.prepareStatement("INSERT INTO itemsonground(object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable) VALUES (?,?,?,?,?,?,?,?,?)");
					st.setInt(1, item.getObjectId());
					st.setInt(2, item.getItemId());
					st.setInt(3, item.getCount());
					st.setInt(4, item.getEnchantLevel());
					st.setInt(5, item.getX());
					st.setInt(6, item.getY());
					st.setInt(7, item.getZ());

					if (item.isProtected())
						st.setLong(8,-1); //item will be protected
					else
						st.setLong(8,item.getDropTime()); //item will be added to ItemsAutoDestroy
					if (item.isEquipable())
						st.setLong(9,1); //set equipable
					else
						st.setLong(9,0);
					st.execute();
					Close.S(st);
				} 
			}
			catch (Exception e) 
			{
				_log.log(Level.SEVERE,"error while inserting into table ItemsOnGround " + e);
				e.printStackTrace();
			}
			finally 
			{
				Close.CS(con,st);
			}
		}
	}
 }
