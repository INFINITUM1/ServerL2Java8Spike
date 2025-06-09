package net.sf.l2j;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javolution.text.TextBuilder;

//import com.mchange.v2.c3p0.ComboPooledDataSource;
//import com.mchange.v2.c3p0.DataSources;
import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.BoneCPConfig;

import net.sf.l2j.mysql.Connect;

public class L2DatabaseFactory {

    static final Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());
    // Data Field
    private static L2DatabaseFactory _cins; // _instance
    private static ProviderType _a;
    //private ComboPooledDataSource _s;
    private static BoneCPDataSource connectionPool;
    private static final Map<String, Connect> cons = new ConcurrentHashMap<String, Connect>();

    public static enum ProviderType {

        MySql,
        MsSql
    }

    @Deprecated
    public static L2DatabaseFactory getInstance() throws SQLException {
        return _cins;
    }

    public static void init() throws SQLException {
        _cins = new L2DatabaseFactory();
        load();
    }

    public static void load() throws SQLException {
        try {
            // setup the connection pool
            BoneCPConfig config = new BoneCPConfig();
            config.setJdbcUrl(Config.DATABASE_URL); // jdbc url specific to your database, eg jdbc:mysql://127.0.0.1/yourdb
            config.setUsername(Config.DATABASE_LOGIN);
            config.setPassword(Config.DATABASE_PASSWORD);
            config.setMinConnectionsPerPartition(Config.MINCONNECTIONSPERPARTITION); // Min no of connections the pool will (initially) create (per partition)
            config.setMaxConnectionsPerPartition(Config.MAXCONNECTIONSPERPARTITION); // Max no of connections the pool will ever create (per partition).
            config.setPartitionCount(Config.PARTITIONCOUNT); // Sets number of partitions to use.

            config.setAcquireIncrement(Config.ACQUIREINCREMENT); // Number of new connections to create in 1 batch whenever we need more connections.
            config.setIdleConnectionTestPeriod(Config.IDLECONNECTIONTESTPERIOD); // This sets the time (in minutes), for a connection to remain idle before sending a test query to the DB.
            config.setIdleMaxAge(Config.IDLEMAXAGE); // Maximum age of an unused connection before it is closed off. In minutes.
            config.setReleaseHelperThreads(Config.RELEASEHELPERTHREADS); // Number of release-connection helper threads to create per partition.
            config.setAcquireRetryDelay(Config.ACQUIRERETRYDELAY); // After attempting to acquire a connection and failing, wait for this value before attempting to acquire a new connection again.
            config.setAcquireRetryAttempts(Config.ACQUIRERETRYATTEMPTS); // After attempting to acquire a connection and failing, try to connect these many times before giving up.
            config.setLazyInit(Config.LAZYINIT); // If set to true, the connection pool will remain empty until the first connection is obtained.
            config.setTransactionRecoveryEnabled(Config.TRANSACTIONRECOVERYENABLED); // If set to true, stores all activity on this connection to allow for replaying it again automatically if it fails. Makes the pool marginally slower.
            config.setQueryExecuteTimeLimit(Config.QUERYEXECUTETIMELIMIT); // Queries taking longer than this limit to execute are logged.
            config.setConnectionTimeout(Config.CONNECTIONTIMEOUT); // Time to wait before a call to get() times out and returns an error.
            connectionPool = new BoneCPDataSource(config); // setup the connection pool
            connectionPool.getConnection().close(); // Test the connection
        } catch (SQLException x) {
            // rethrow the exception
            throw x;
        }
    }

    public L2DatabaseFactory() {
    }

    // =========================================================
    // Method - Public
    public static String prepQuerySelect(String[] fields, String tableName, String whereClause, boolean returnOnlyTopRecord) {
        String msSqlTop1 = "";
        String mySqlTop1 = "";
        if (returnOnlyTopRecord) {
            if (getProviderType() == ProviderType.MsSql) {
                msSqlTop1 = " Top 1 ";
            }
            if (getProviderType() == ProviderType.MySql) {
                mySqlTop1 = " Limit 1 ";
            }
        }
        String query = "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause + mySqlTop1;
        return query;
    }

    public static String safetyString(String[] whatToCheck) {
        // NOTE: Use brace as a safty percaution just incase name is a reserved word
        String braceLeft = "`";
        String braceRight = "`";
        if (getProviderType() == ProviderType.MsSql) {
            braceLeft = "[";
            braceRight = "]";
        }

        TextBuilder result = new TextBuilder("");
        for (String word : whatToCheck) {
            if (!(result.toString().equals(""))) {
                result.append(", ");
            }
            result.append(braceLeft + word + braceRight);
        }
        return result.toString();
    }

    public static Connect get() throws SQLException {
        return findConnect(generateKey(), null);
    }

    @Deprecated
    public Connect getConnection() throws SQLException {
        return findConnect(generateKey(), null);
    }

    private static Connect findConnect(String key, Connect con) throws SQLException {
        con = cons.get(key);
        if (con == null) {
            con = new Connect(connectionPool.getConnection());
        } else {
            con.updateCounter();
        }

        cons.put(key, con);
        return con;
    }

    public static Map<String, Connect> getConnections() {
        return cons;
    }

    public static int getCounts() {
        return cons.size();
    }

    public static void remove(String k) {
        cons.remove(k);
    }

    public static void getAndRemove() {
        cons.remove(generateKey());
    }

    public static void shutdown() {
        try {
            connectionPool.close();
        } catch (Exception e) {
            _log.log(Level.INFO, "", e);
        }
        cons.clear();
    }

    public static String generateKey() {
        return String.valueOf(Thread.currentThread().hashCode());
    }

    public static ProviderType getProviderType() {
        return _a;
    }
}
