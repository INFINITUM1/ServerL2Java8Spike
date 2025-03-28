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
package net.sf.l2j.gameserver.idfactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.7 $ $Date: 2005/04/11 10:06:12 $
 */
public abstract class IdFactory {

    private static final Logger _log = AbstractLogger.getLogger(IdFactory.class.getName());
    protected static final String[] ID_UPDATES = {
        "UPDATE items                 SET owner_id = ?    WHERE owner_id = ?",
        "UPDATE items                 SET object_id = ?   WHERE object_id = ?",
        "UPDATE character_quests      SET char_id = ?     WHERE char_id = ?",
        "UPDATE character_friends     SET char_id = ?     WHERE char_id = ?",
        "UPDATE character_friends     SET friend_id = ?   WHERE friend_id = ?",
        "UPDATE character_hennas      SET char_obj_id = ? WHERE char_obj_id = ?",
        "UPDATE character_recipebook  SET char_id = ?     WHERE char_id = ?",
        "UPDATE character_shortcuts   SET char_obj_id = ? WHERE char_obj_id = ?",
        "UPDATE character_shortcuts   SET shortcut_id = ? WHERE shortcut_id = ? AND type = 1", // items
        "UPDATE character_macroses    SET char_obj_id = ? WHERE char_obj_id = ?",
        "UPDATE character_skills      SET char_obj_id = ? WHERE char_obj_id = ?",
        "UPDATE character_buffs SET char_obj_id = ? WHERE char_obj_id = ?",
        "UPDATE character_subclasses  SET char_obj_id = ? WHERE char_obj_id = ?",
        "UPDATE characters            SET obj_Id = ?      WHERE obj_Id = ?",
        "UPDATE characters            SET clanid = ?      WHERE clanid = ?",
        "UPDATE clan_data             SET clan_id = ?     WHERE clan_id = ?",
        "UPDATE siege_clans           SET clan_id = ?     WHERE clan_id = ?",
        "UPDATE clan_data             SET ally_id = ?     WHERE ally_id = ?",
        "UPDATE clan_data             SET leader_id = ?   WHERE leader_id = ?",
        "UPDATE pets                  SET item_obj_id = ? WHERE item_obj_id = ?",
        "UPDATE character_hennas     SET char_obj_id = ? WHERE char_obj_id = ?",
        "UPDATE itemsonground         SET object_id = ?   WHERE object_id = ?",
        "UPDATE auction_bid          SET bidderId = ?      WHERE bidderId = ?",
        "UPDATE auction_watch        SET charObjId = ?     WHERE charObjId = ?",
        "UPDATE clanhall             SET ownerId = ?       WHERE ownerId = ?"
    };
    protected static final String[] ID_CHECKS = {
        "SELECT owner_id    FROM items                 WHERE object_id >= ?   AND object_id < ?",
        "SELECT object_id   FROM items                 WHERE object_id >= ?   AND object_id < ?",
        "SELECT char_id     FROM character_quests      WHERE char_id >= ?     AND char_id < ?",
        "SELECT char_id     FROM character_friends     WHERE char_id >= ?     AND char_id < ?",
        "SELECT char_id     FROM character_friends     WHERE friend_id >= ?   AND friend_id < ?",
        "SELECT char_obj_id FROM character_hennas      WHERE char_obj_id >= ? AND char_obj_id < ?",
        "SELECT char_id     FROM character_recipebook  WHERE char_id >= ?     AND char_id < ?",
        "SELECT char_obj_id FROM character_shortcuts   WHERE char_obj_id >= ? AND char_obj_id < ?",
        "SELECT char_obj_id FROM character_macroses    WHERE char_obj_id >= ? AND char_obj_id < ?",
        "SELECT char_obj_id FROM character_skills      WHERE char_obj_id >= ? AND char_obj_id < ?",
        "SELECT char_obj_id FROM character_buffs WHERE char_obj_id >= ? AND char_obj_id < ?",
        "SELECT char_obj_id FROM character_subclasses  WHERE char_obj_id >= ? AND char_obj_id < ?",
        "SELECT obj_Id      FROM characters            WHERE obj_Id >= ?      AND obj_Id < ?",
        "SELECT clanid      FROM characters            WHERE clanid >= ?      AND clanid < ?",
        "SELECT clan_id     FROM clan_data             WHERE clan_id >= ?     AND clan_id < ?",
        "SELECT clan_id     FROM siege_clans           WHERE clan_id >= ?     AND clan_id < ?",
        "SELECT ally_id     FROM clan_data             WHERE ally_id >= ?     AND ally_id < ?",
        "SELECT leader_id   FROM clan_data             WHERE leader_id >= ?   AND leader_id < ?",
        "SELECT item_obj_id FROM pets                  WHERE item_obj_id >= ? AND item_obj_id < ?",
        "SELECT object_id   FROM itemsonground        WHERE object_id >= ?   AND object_id < ?"
    };
    protected boolean _initialized;
    public static final int FIRST_OID = 0x10000000;
    public static final int LAST_OID = 0x7FFFFFFF;
    public static final int FREE_OBJECT_ID_SIZE = LAST_OID - FIRST_OID;
    protected static IdFactory _instance = new BitSetIDFactory();

    ;

	protected IdFactory() {
        //setAllCharacterOffline();
        cleanUpDB();
    }

    /**
     * Sets all character offline
     */
    /*private void setAllCharacterOffline()
    {
    Connect con = null;
    Statement s = null;
    try
    {
    con = L2DatabaseFactory.get();
    s = con.createStatement();
    s.addBatch("UPDATE `characters` SET `online`=0 WHERE `online` <> 0");
    s.executeBatch();
    _log.info("Updated characters online status.");
    }
    catch (SQLException e)
    {
    }
    finally
    {
    Close.S2(s);
    Close.C(con);
    }
    }*/
    /**
     * Cleans up Database
     */
    private void cleanUpDB() {
        Connect con = null;
        Statement stmt = null;
        try {
            long start = System.currentTimeMillis();
            //
            con = L2DatabaseFactory.get();
            con.setAutoCommit(false);

            stmt = con.createStatement();
            stmt.executeUpdate("UPDATE `characters` SET `online`=0");
            _log.info("Updated characters online status.");
            Close.S2(stmt);

            _log.info("");
            _log.info("Starting cleaning database...");
            stmt = con.createStatement();
            //Character related
            //stmt.addBatch("DELETE FROM characters WHERE characters.account_name NOT IN (SELECT login FROM accounts);");
            stmt.addBatch("DELETE FROM character_friends WHERE character_friends.char_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_hennas WHERE character_hennas.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_macroses WHERE character_macroses.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_quests WHERE character_quests.char_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_recipebook WHERE character_recipebook.char_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_shortcuts WHERE character_shortcuts.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_skills WHERE character_skills.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_buffs WHERE character_buffs.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_subclasses WHERE character_subclasses.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM cursed_weapons WHERE cursed_weapons.playerId NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM heroes WHERE heroes.char_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM olympiad_nobles WHERE olympiad_nobles.char_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM pets WHERE pets.item_obj_id NOT IN (SELECT object_id FROM items);");
            stmt.addBatch("DELETE FROM seven_signs WHERE seven_signs.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM character_settings WHERE character_settings.char_obj_id NOT IN (SELECT obj_Id FROM characters);");
            //Auction
            stmt.addBatch("DELETE FROM auction WHERE auction.id IN (SELECT id FROM clanhall WHERE ownerId <> 0);");
            stmt.addBatch("DELETE FROM auction_bid WHERE auctionId IN (SELECT id FROM clanhall WHERE ownerId <> 0)");
            //Clan related
            stmt.addBatch("UPDATE clan_data SET auction_bid_at = 0 WHERE auction_bid_at NOT IN (SELECT auctionId FROM auction_bid);");
            stmt.addBatch("DELETE FROM clan_data WHERE clan_data.leader_id NOT IN (SELECT obj_Id FROM characters);");
            stmt.addBatch("DELETE FROM auction_bid WHERE auction_bid.bidderId NOT IN (SELECT clan_id FROM clan_data);");
            stmt.addBatch("DELETE FROM clanhall_functions WHERE clanhall_functions.hall_id NOT IN (SELECT id FROM clanhall WHERE ownerId <> 0);");
            stmt.addBatch("DELETE FROM clan_privs WHERE clan_privs.clan_id NOT IN (SELECT clan_id FROM clan_data);");
            stmt.addBatch("DELETE FROM clan_skills WHERE clan_skills.clan_id NOT IN (SELECT clan_id FROM clan_data);");
            stmt.addBatch("DELETE FROM clan_subpledges WHERE clan_subpledges.clan_id NOT IN (SELECT clan_id FROM clan_data);");
            stmt.addBatch("DELETE FROM clan_wars WHERE clan_wars.clan1 NOT IN (SELECT clan_id FROM clan_data);");
            stmt.addBatch("DELETE FROM clan_wars WHERE clan_wars.clan2 NOT IN (SELECT clan_id FROM clan_data);");
            stmt.addBatch("DELETE FROM siege_clans WHERE siege_clans.clan_id NOT IN (SELECT clan_id FROM clan_data);");
            stmt.addBatch("UPDATE castle SET taxpercent=0 WHERE castle.id NOT IN (SELECT hasCastle FROM clan_data);");
            //Character & clan related
            stmt.addBatch("DELETE FROM items WHERE items.owner_id NOT IN (SELECT obj_Id FROM characters) AND items.owner_id NOT IN (SELECT clan_id FROM clan_data);");
            stmt.addBatch("UPDATE characters SET clanid=0 WHERE characters.clanid NOT IN (SELECT clan_id FROM clan_data);");
            //Forum related
            stmt.addBatch("DELETE FROM forums WHERE forums.forum_owner_id NOT IN (SELECT clan_id FROM clan_data) AND forums.forum_parent=2;");
            stmt.addBatch("DELETE FROM topic WHERE topic.topic_forum_id NOT IN (SELECT forum_id FROM forums);");
            stmt.addBatch("DELETE FROM posts WHERE posts.post_forum_id NOT IN (SELECT forum_id FROM forums);");
            stmt.executeBatch();
            con.commit();
            stmt.clearBatch();
            //
            _log.info("done; time: " + Util.formatAdena((int) (System.currentTimeMillis() - start)) + " ms.");
            _log.info("");
        } catch (SQLException e) {
            _log.warning("IdFactory [ERROR] cleanUpDB():" + e.getMessage());
        } finally {
            Close.S2(stmt);
            Close.C(con);
        }
    }

    /**
     * @param con
     * @return
     * @throws SQLException
     */
    protected int[] extractUsedObjectIDTable() throws SQLException {
        Connect con = null;
        Statement s = null;
        ResultSet result = null;
        try {
            con = L2DatabaseFactory.get();
            //create a temporary table
            s = con.createStatement();
            s.addBatch("drop table if exists temporaryObjectTable");

            s.addBatch("delete from itemsonground where object_id in (select object_id from items)");
            s.addBatch("create table IF NOT EXISTS temporaryObjectTable (object_id int NOT NULL PRIMARY KEY) ENGINE = MyISAM");
            //s.addBatch("ALTER TABLE temporaryObjectTable MAX_ROWS=1000000000 AVG_ROW_LENGTH=nnn;");

            s.addBatch("replace into temporaryObjectTable (object_id) select obj_id from characters");
            s.addBatch("replace into temporaryObjectTable (object_id) select object_id from items");
            s.addBatch("replace into temporaryObjectTable (object_id) select clan_id from clan_data");
            s.addBatch("replace into temporaryObjectTable (object_id) select object_id from itemsonground");
            s.executeBatch();

            result = s.executeQuery("select count(object_id) from temporaryObjectTable");

            result.next();
            int size = result.getInt(1);
            int[] tmp_obj_ids = new int[size];
            // System.out.println("tmp table size: " + tmp_obj_ids.length);
            Close.R(result);

            result = s.executeQuery("select object_id from temporaryObjectTable ORDER BY object_id");

            int idx = 0;
            while (result.next()) {
                tmp_obj_ids[idx++] = result.getInt(1);
            }

            return tmp_obj_ids;
        } catch (SQLException e) {
            _log.warning("IdFactory [ERROR] extractUsedObjectIDTable():" + e.getMessage());
            return null;
        } finally {
            Close.R(result);
            Close.S2(s);
            Close.C(con);
        }
    }

    public boolean isInitialized() {
        return _initialized;
    }

    public static IdFactory getInstance() {
        return _instance;
    }

    public abstract int getNextId();

    /**
     * return a used Object ID back to the pool
     * @param object ID
     */
    public abstract void releaseId(int id);

    public abstract int size();
}
