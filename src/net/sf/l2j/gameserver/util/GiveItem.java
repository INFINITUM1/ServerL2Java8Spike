package net.sf.l2j.gameserver.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

public class GiveItem {

    public static boolean insertOffline(Connect con, int char_id, int item_id, int item_count, int item_ench, int aug_id, int aug_lvl, String inv_loc) {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            if (item_count > 1) {
                st = con.prepareStatement("SELECT object_id,count FROM items WHERE `owner_id`=? AND `item_id`=? AND `loc`=? LIMIT 1");
                st.setInt(1, char_id);
                st.setInt(2, item_id);
                st.setString(3, inv_loc);
                rs = st.executeQuery();
                if (rs.next()) {
                    int obj = rs.getInt("object_id");
                    int count = rs.getInt("count");
                    Close.SR(st, rs);

                    st = con.prepareStatement("UPDATE items SET `count`=? WHERE `object_id`=? AND `loc`=?");
                    st.setInt(1, (count + item_count));
                    st.setInt(2, obj);
                    st.setString(3, inv_loc);
                    st.execute();
                    return true;
                } else {
                    st = con.prepareStatement("INSERT INTO items (owner_id,object_id,item_id,count,enchant_level,loc,loc_data,price_sell,price_buy,custom_type1,custom_type2,mana_left) VALUES (?,?,?,?,0,?,0,0,0,0,0,-1)");
                    st.setInt(1, char_id);
                    st.setInt(2, IdFactory.getInstance().getNextId());
                    st.setInt(3, item_id);
                    st.setInt(4, item_count);
                    st.setString(5, inv_loc);
                    st.execute();
                    return true;
                }
            } else {
                st = con.prepareStatement("INSERT INTO items (owner_id,object_id,item_id,count,enchant_level,loc,loc_data,price_sell,price_buy,custom_type1,custom_type2,mana_left) VALUES (?,?,?,?,?,?,0,0,0,0,0,-1)");
                st.setInt(1, char_id);
                st.setInt(2, IdFactory.getInstance().getNextId());
                st.setInt(3, item_id);
                st.setInt(4, item_count);
                st.setInt(5, item_ench);
                st.setString(6, inv_loc);
                st.execute();
                return true;
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] GiveItem, insertOffline() error: " + e);
        } finally {
            Close.SR(st, rs);
        }
        return false;
    }
}
