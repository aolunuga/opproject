/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.test;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpInitializer;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocaleMap;
import onepoint.resource.XLocaleMapLoader;
import onepoint.resource.XResourceBroker;

import java.sql.*;

/**
 * This class is used to initialize application before executing tests.
 *
 * @author calin.pavel
 */
public class AppInitializer {
   //Class logger.
   private static final XLog logger = XLogFactory.getLogger(AppInitializer.class, true);

   static final String SCHEMA_TABLE = "op_schema";
   private static final int SCHEMA_VERSION = 4;
   private static final String VERSION_COLUMN = "op_version";

   private static final String CREATE_SCHEMA_TABLE_STATEMENT = "create table " + SCHEMA_TABLE + "(" + VERSION_COLUMN + " int)";
   private static final String INSERT_VERSION_INTO_SCHEMA_TABLE_STATEMENT = "insert into " + SCHEMA_TABLE + " values(" + SCHEMA_VERSION + ")";
   private static final String UPDATE_SCHEMA_TABLE_STATEMENT = "update " + SCHEMA_TABLE + " set " + VERSION_COLUMN + "=" + SCHEMA_VERSION;
   private static final String GET_SCHEMA_VERSION_STATEMENT = "select * from " + SCHEMA_TABLE;

   // Specify if the application was initialized or not.
   public static boolean isInitialized;

   /**
    * Initialize application
    */
   public synchronized static void init() {
      if (isInitialized) {
         return;
      }

      try {
         doInitialization();
      }
      catch (Exception e) {
         logger.error("Could not initialize application because: " + e.getMessage(), e);

         // In case an exception occured here the whole testing process will fail.
         System.exit(0);
      }
   }

   /**
    * Here we do the real initialization.
    *
    * @throws Exception If initialization process can not be finished.
    */
   private static void doInitialization()
        throws Exception {
      // Now start application initialization.
      logger.debug("Static set up block for all test cases");
      OpEnvironmentManager.setOnePointHome(Constants.ONEPOINT_HOME);

      XResourceBroker.setResourcePath(Constants.RESOURCE_PATH);
      XLocaleMap locale_map = new XLocaleMapLoader().loadLocaleMap(Constants.LOCALES_OLM_XML);
      XLocaleManager.setLocaleMap(locale_map);

      updateSchemaTable();

      OpInitializer.init(OpProjectConstants.TEAM_EDITION_CODE);

      logger.debug("Application initialization end.");
   }

   /**
    * This method creates a JDBC connection to configured database and
    *
    * @throws SQLException           IF op_schema table can not be updated.
    * @throws ClassNotFoundException If JDBC driver class can not be found.
    */
   private static void updateSchemaTable()
        throws SQLException, ClassNotFoundException {
      OpConfigurationLoader configurationLoader = new OpConfigurationLoader();
      String projectPath = OpEnvironmentManager.getOnePointHome();
      OpConfiguration configuration = configurationLoader.loadConfiguration(projectPath
           + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME);

      if (configuration == null) {
         throw new RuntimeException("Could not load configuration file.");
      }

      //get the db connection parameters
      String databaseUrl = configuration.getDatabaseConfiguration().getDatabaseUrl();
      String databaseDriver = configuration.getDatabaseConfiguration().getDatabaseDriver();
      String databasePassword = configuration.getDatabaseConfiguration().getDatabasePassword();
      String databaseLogin = configuration.getDatabaseConfiguration().getDatabaseLogin();

      Class.forName(databaseDriver);
      Connection conn = DriverManager.getConnection(databaseUrl, databaseLogin, databasePassword);

      // first try to update op_schema table. If not present try to create it.
      try {
         PreparedStatement createStmt = conn.prepareStatement(CREATE_SCHEMA_TABLE_STATEMENT);
         createStmt.execute();
      }
      catch (SQLException e) {
         // expected. This could happen in case table already exists.
         logger.info("OP_SCHEMA table already exists.");
      }

      try {
         // now try to see if the table contains any value or not
         PreparedStatement getVersionStmt = conn.prepareStatement(GET_SCHEMA_VERSION_STATEMENT);
         ResultSet rs = getVersionStmt.executeQuery();
         if (rs.next()) {
            int version = rs.getInt(1);
            if (version != SCHEMA_VERSION) {
               PreparedStatement updateStmt = conn.prepareStatement(UPDATE_SCHEMA_TABLE_STATEMENT);
               updateStmt.execute();
            }
         }
         else {
            PreparedStatement insertStmt = conn.prepareStatement(INSERT_VERSION_INTO_SCHEMA_TABLE_STATEMENT);
            insertStmt.execute();
         }
      }
      catch (SQLException e) {
         logger.error("Could not update schema version.", e);
         throw e;
      }
      finally {
         if (conn != null) {
            conn.close();
         }
      }
   }

}