package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

public class CharNameTable {

    private static Logger _log = AbstractLogger.getLogger(CharNameTable.class.getName());

    private static CharNameTable _instance;

    public static CharNameTable getInstance() {
        if (_instance == null) {
            _instance = new CharNameTable();
        }
        return _instance;
    }

    public boolean doesCharNameExist(String name) {
        if (_createdToday.contains(name)) {
            return true;
        }
        boolean result = true;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
            st.setString(1, name);
            rs = st.executeQuery();
            result = rs.next();
        } catch (SQLException e) {
            _log.warning("could not check existing charname:" + e.getMessage());
        } finally {
            Close.CSR(con, st, rs);
        }
        return result;
    }

    public int accountCharNumber(String account) {
        int number = 0;
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT COUNT(char_name) FROM characters WHERE account_name=?");
            st.setString(1, account);
            rs = st.executeQuery();
            while (rs.next()) {
                number = rs.getInt(1);
            }
        } catch (SQLException e) {
            _log.warning("could not check existing char number:" + e.getMessage());
        } finally {
            Close.CSR(con, st, rs);
        }

        return number;
    }
    private final ConcurrentLinkedQueue<String> _createdToday = new ConcurrentLinkedQueue<>();

    public void storeCharName(String name) {
        _createdToday.add(name);
    }
}
