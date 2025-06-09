package net.sf.l2j.mysql;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.util.log.AbstractLogger;

public class Connect {

    static final Logger _log = AbstractLogger.getLogger(Connect.class.getName());
    private final Connection _con;
    private int all;

    public Connect(Connection con) {
        _con = con;
        all = 1;
    }

    public void updateCounter() {
        all++;
    }

    public void close() {
        all--;
        if (all == 0) {
            try {
                _con.close();
                L2DatabaseFactory.getAndRemove();
            } catch (Exception e) {
                _log.warning("Couldn't close connection. Cause: " + e.getMessage());
            }
        }
    }

    public Statement createStatement() throws SQLException {
        return _con.createStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return _con.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int a, int b) throws SQLException {
        return _con.prepareStatement(sql, a, b);
    }

    public void setAutoCommit(boolean flag) throws SQLException {
        _con.setAutoCommit(flag);
    }

    public void commit() throws SQLException {
        _con.commit();
    }

    public void rollback() throws SQLException {
        _con.rollback();
    }

    public void setTransactionIsolation(int a) throws SQLException {
        _con.setTransactionIsolation(a);
    }

    public int getTransactionIsolation() throws SQLException {
        return _con.getTransactionIsolation();
    }
}
