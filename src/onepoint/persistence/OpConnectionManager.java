/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.hibernate.OpHibernateSource;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that performs various operations on database connections.
 *
 * @author horia.chiorean
 */
public final class OpConnectionManager {

   /**
    * Return codes for the test connection method.
    */
   public static final int SUCCESS = 0;
   public static final int INVALID_CREDENTIALS_EXCEPTION = 1;
   public static final int INVALID_CONNECTION_STRING_EXCEPTION = 2;
   public static final int GENERAL_CONNECTION_EXCEPTION = 3;
   public static final int MISSINING_DRIVER_EXCEPTION = 4;
   public static final int INVALID_MYSQL_ENGINE = 5;

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpConnectionManager.class, true);

   /**
    * Various SQL state values.
    */
   private static final String INVALID_CREDENTIALS_SQLSTATE = "28000";
   private static final String INVALID_CONNECTION_STRING_SQLSTATE = "08001";
   private static final String EXEC_PHASE_ERRORS_SQLSTATE_ORCL = "72000";    // SQL execute phase errors

   private static final Map EXCEPTIONAL_SQLSTATES;

   static {
      Map  oracleSqlstates = new HashMap();
      oracleSqlstates.put(EXEC_PHASE_ERRORS_SQLSTATE_ORCL, new Integer(INVALID_CREDENTIALS_EXCEPTION));
      EXCEPTIONAL_SQLSTATES = new HashMap();
      EXCEPTIONAL_SQLSTATES.put(new Integer(OpHibernateSource.ORACLE), oracleSqlstates);
   }

   /**
    * This is an utility class.
    */
   private OpConnectionManager() {
   }

   /**
    * Tests that a connection can be established to an underlying database, with the given parameters.
    * @param dbType a <code>int</code> constant, representing the type of the database.
    * @param databaseDriver a <code>String</code> representing the database driver class name.
    * @param databaseURL a <code>String</code> representing a connect URL.
    * @param databaseLogin a <code>String</code> representing the user that attempts to connects.
    * @param databasePassword a <code>String</code> representing the password of the db connection.
    * @return a <code>int</code> representing a return code (either success or smth else).
    */
   public static int testConnection(int dbType, String databaseDriver, String databaseURL, String databaseLogin, String databasePassword) {
      Connection conn = null;
      try {
         Class.forName(databaseDriver);
         conn = DriverManager.getConnection(databaseURL, databaseLogin, databasePassword);
         conn.getMetaData();
         if ((dbType == OpHibernateSource.MYSQL || dbType == OpHibernateSource.MYSQL_INNODB) && !isInnoDb(conn)) {
            return INVALID_MYSQL_ENGINE;
         }
         else {
            return SUCCESS;
         }
      }
      catch (SQLException e) {
         logger.error("Invalid db connection parameters (code " + e.getErrorCode() + " )", e);
         String sqlState = e.getSQLState();
         if (sqlState == null) {
            return GENERAL_CONNECTION_EXCEPTION;   
         }
         if (sqlState.equalsIgnoreCase(INVALID_CREDENTIALS_SQLSTATE)) {
            return INVALID_CREDENTIALS_EXCEPTION;
         }
         if (sqlState.equalsIgnoreCase(INVALID_CONNECTION_STRING_SQLSTATE)) {
            return INVALID_CONNECTION_STRING_EXCEPTION;
         }
         if (EXCEPTIONAL_SQLSTATES.containsKey(new Integer(dbType)) &&
              ((Map) EXCEPTIONAL_SQLSTATES.get(new Integer(dbType))).containsKey(sqlState)) {
            return ((Integer) ((Map) EXCEPTIONAL_SQLSTATES.get(new Integer(dbType))).get(sqlState)).intValue();
         }
         return GENERAL_CONNECTION_EXCEPTION;
      }
      catch (ClassNotFoundException e) {
         logger.error("Cannot load jdbc driver " + databaseDriver, e);
         return MISSINING_DRIVER_EXCEPTION;
      }
      finally {
         closeJDBCObjects(conn, null, null);
      }
   }

   /**
    * Closes a series of JDBC objects.
    * @param conn a <code>Connection</code> object.
    * @param st a <code>Statement</code> object.
    * @param rs a <code>ResultSet</code> object.
    */
   private static void closeJDBCObjects(Connection conn, Statement st, ResultSet rs) {
      if (rs != null) {
         try {
            rs.close();
         }
         catch (SQLException e) {
            logger.error("Cannot close Result Set", e);
         }
      }
      if (st != null) {
         try {
            st.close();
         }
         catch (SQLException e) {
            logger.error("Cannot close SQL statement", e);
         }
      }
      if (conn != null) {
         try {
            conn.close();
         }
         catch (SQLException e) {
            logger.error("Cannot close jdbc connection ", e);
         }
      }
   }

   /**
    * Checks the given db connection (when the db type is MySQL) to see whether the storage engine is InnoDB or not.
    *
    * @param connection a <code>Connection</code> object, representing a MySQL connection.
    * @return <code>true</code> if the MySQL storage is InnoDB, false otherwise (including the case when the storage engine can't be determined).
    */
   private static boolean isInnoDb(Connection connection) {
      Statement st = null;
       ResultSet rs = null;
      try {
         st = connection.createStatement();
         st.execute("SHOW TABLE STATUS LIKE '" + OpHibernateSource.TABLE_NAME_PREFIX + "%' ");
         rs = st.getResultSet();
         while (rs.next()) {
            String tableName = rs.getString("Name");
            //for compatibility with clients which already had (before this code) the schema ta
            if (OpHibernateSource.SCHEMA_TABLE.equalsIgnoreCase(tableName)) {
               continue;
            }
            String engine = rs.getString("Engine");
            if (!"InnoDb".equalsIgnoreCase(engine.trim())) {
               return false;
            }
         }
         return true;
      }
      catch (SQLException e) {
         logger.error("Cannot determine engine type for MySQL " , e);
         return false;
      }
      finally {
         closeJDBCObjects(null, st, rs);
      }
   }
}
