/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
   private static final XLog logger = XLogFactory.getLogger(OpConnectionManager.class, true);

   /**
    * Various SQL state values.
    */
   private static final String INVALID_CREDENTIALS_SQLSTATE = "28000";
   private static final String INVALID_CONNECTION_STRING_SQLSTATE = "08001";

   /**
    * This is an utility class.
    */
   private OpConnectionManager() {
   }

   public static int testConnection(String databaseDriver, String databaseURL, String databaseLogin, String databasePassword) {
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
         if (sqlState.equalsIgnoreCase(INVALID_CREDENTIALS_SQLSTATE)) {
            return INVALID_CREDENTIALS_EXCEPTION;
         }
         if (sqlState.equalsIgnoreCase(INVALID_CONNECTION_STRING_SQLSTATE)) {
            return INVALID_CONNECTION_STRING_EXCEPTION;
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
