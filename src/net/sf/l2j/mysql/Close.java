package net.sf.l2j.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

public class Close {

    public static void C(Connect c) {
        try {
            close(c);
        } catch (SQLException e) {
            // quiet
        }
    }

    public static void S(PreparedStatement s) {
        try {
            close(s);
        } catch (SQLException e) {
            // quiet
        }
    }

    public static void S2(Statement s) {
        try {
            close(s);
        } catch (SQLException e) {
            // quiet
        }
    }

    public static void R(ResultSet r) {
        try {
            close(r);
        } catch (SQLException e) {
            // quiet
        }
    }

    public static void CSR(Connect c, PreparedStatement s, ResultSet r) {
        try {
            R(r);
        } finally {
            try {
                S(s);
            } finally {
                C(c);
            }
        }
    }

    public static void CS(Connect c, PreparedStatement s) {
        try {
            S(s);
        } finally {
            C(c);
        }
    }

    public static void SR(PreparedStatement s, ResultSet r) {
        try {
            S(s);
        } finally {
            R(r);
        }
    }

    //
    private static void close(Connect conn) throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    private static void close(ResultSet rs) throws SQLException {
        if (rs != null) {
            rs.close();
        }
    }

    private static void close(Statement stmt) throws SQLException {
        if (stmt != null) {
            stmt.close();
        }
    }
}
