/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.hibernate.OpHibernateSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

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

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpConnectionManager.class);

   /**
    * Various SQL state values.
    */
   private static final String INVALID_CREDENTIALS_SQLSTATE = "28000";
   private static final String INVALID_CONNECTION_STRING_SQLSTATE = "08001";
   private static final String EXEC_PHASE_ERRORS_SQLSTATE_ORCL = "72000";    // SQL execute phase errors

   private static final Map<Integer, Map<String, Integer>> EXCEPTIONAL_SQLSTATES;

   static {
      Map<String, Integer> oracleSqlstates = new HashMap<String, Integer>();
      oracleSqlstates.put(EXEC_PHASE_ERRORS_SQLSTATE_ORCL, INVALID_CREDENTIALS_EXCEPTION);
      EXCEPTIONAL_SQLSTATES = new HashMap<Integer, Map<String, Integer>>();
      EXCEPTIONAL_SQLSTATES.put(OpHibernateSource.ORACLE, oracleSqlstates);
   }

   /**
    * This is an utility class.
    */
   private OpConnectionManager() {
   }

   /**
    * Tests that a connection can be established to an underlying database, with the given parameters.
    *
    * @param databaseDriver   a <code>String</code> representing the database driver class name.
    * @param databaseURL      a <code>String</code> representing a connect URL.
    * @param databaseLogin    a <code>String</code> representing the user that attempts to connects.
    * @param databasePassword a <code>String</code> representing the password of the db connection.
    * @param dbType           an <code>int</code> representing the db type.
    * @return a <code>int</code> representing a return code (either success or smth else).
    */
   public static int testConnection(String databaseDriver, String databaseURL, String databaseLogin, String databasePassword, int dbType) {
      Connection conn = null;
      try {
         Class.forName(databaseDriver);
         conn = DriverManager.getConnection(databaseURL, databaseLogin, databasePassword);
         conn.getMetaData();
         return SUCCESS;
      }
      catch (SQLException e) {
         logger.error("Invalid db connection parameters (code " + e.getErrorCode() + " )");
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
         if (EXCEPTIONAL_SQLSTATES.containsKey(dbType) && EXCEPTIONAL_SQLSTATES.get(dbType).containsKey(sqlState)) {
            return EXCEPTIONAL_SQLSTATES.get(dbType).get(sqlState);
         }
         return GENERAL_CONNECTION_EXCEPTION;
      }
      catch (ClassNotFoundException e) {
         logger.error("Cannot load jdbc driver " + databaseDriver);
         return MISSINING_DRIVER_EXCEPTION;
      }
      finally {
         if (conn != null) {
            try {
               conn.close();
            }
            catch (SQLException e) {
               logger.error("Cannot close jdbc connection ", e);
            }
         }
      }
   }
}
