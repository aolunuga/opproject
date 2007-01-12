/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project;

import onepoint.express.server.XExpressSession;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpConnectionManager;
import onepoint.persistence.OpPersistenceManager;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.module.OpLanguageKitFile;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.configuration_wizard.OpConfigurationWizardManager;
import onepoint.project.modules.mail.OpMailer;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.*;
import onepoint.util.XEnvironment;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class responsible for performing application initialization steps
 *
 * @author ovidiu.lupas
 *         <FIXME author="Horia Chiorean" description="Not very OO-oriented that this class is static...(same as the 'factories')/>
 */
public class OpInitializer {

   //keys in the initParams map
   public static String RESOURCE_CACHE_SIZE = "cacheSize";
   public static String SECURE_SERVICE = "secureService";

   //hard-coded db schema version number
   private static final int SCHEMA_VERSION = 3;

   //class logger
   private static final XLog logger = XLogFactory.getLogger(OpInitializer.class, true);

   /**
    * Run level of the application.
    */
   private static byte runLevel = 0;

   /**
    * Success Run Level.
    */
   private static byte successRunLevel = 6;

   private static int connectionTestCode = OpConnectionManager.SUCCESS;

   //init params map that will be returned after initialization is performed
   private static Map initParams = new HashMap();

   /**
    * Flag indicating whether the application is multi-user or not.
    */
   private static boolean multiUser = false;

   private static OpConfiguration configuration = null;

   /**
    * This class should not be instantiated
    */
   private OpInitializer() {
   }

   /**
    * Returns the run level of the application
    *
    * @return <code>byte</code> run level
    */
   public static byte getRunLevel() {
      return runLevel;
   }

   /**
    * Returns the run level of the application
    *
    * @return <code>byte</code> run level
    */
   public static byte getSuccessRunLevel() {
      return successRunLevel;
   }

   /**
    * Performs application initilization steps.
    *
    * @param projectPath <code>String</code> the absolute path to the folder which contains the configuration files
    * @param isMultiUser True if application is in multi-user mode, false if it is single-user (standalone)
    * @return <code>Map</code> of init parameters
    */
   public static Map init(String projectPath, boolean isMultiUser) {

      logger.info("Application initialization started");
      multiUser = isMultiUser;
      initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

      try {

         XResourceBroker.setResourcePath(OpProjectConstants.PROJECT_PACKAGE);
         // Attention: Locale map must be loaded and set before starting up modules
         if (XLocaleManager.getLocaleMap() == null) {
            XLocaleMap locale_map = new XLocaleMapLoader().loadLocaleMap("/locales.olm.xml");
            XLocaleManager.setLocaleMap(locale_map);
         }

         // load language resources for main application forms (e.g. login.oxf)
         OpLanguageKitFile main_en_file = new OpLanguageKitFile();
         main_en_file.setFileName("/i18n/main_en.olk.xml");
         XLanguageKit main_en = main_en_file.loadLanguageKit();
         XLocaleManager.registerLanguageKit(main_en);
         OpLanguageKitFile main_de_file = new OpLanguageKitFile();
         main_de_file.setFileName("/i18n/main_de.olk.xml");
         XLanguageKit main_de = main_de_file.loadLanguageKit();
         XLocaleManager.registerLanguageKit(main_de);

         // environment setup
         if (projectPath != null) {
            XEnvironment.setVariable(OpEnvironmentManager.ONEPOINT_HOME, projectPath);
         }
         else {
            logger.error("Environment variable ONEPOINT_HOME is not set");
            return initParams;
         }

         // Read configuration file
         OpConfigurationLoader configurationLoader = new OpConfigurationLoader();
         onepoint.project.configuration.OpConfiguration configuration = configurationLoader.loadConfiguration(projectPath + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME);

         if (configuration == null) {
            logger.error("Could not load configuration file " + projectPath + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME);
            //prepare the db wizard
            logger.info("Initializing db configuration wizard module...");
            OpConfigurationWizardManager.loadConfigurationWizardModule();
            return initParams; //show db configuration wizard frame
         }
         else {
            OpInitializer.configuration = configuration;
         }

         //get the db connection parameters
         String databaseUrl = configuration.getDatabaseConfiguration().getDatabaseUrl();
         String databaseDriver = configuration.getDatabaseConfiguration().getDatabaseDriver();
         String databasePassword = configuration.getDatabaseConfiguration().getDatabasePassword();
         String databaseLogin = configuration.getDatabaseConfiguration().getDatabaseLogin();
         int databaseType = configuration.getDatabaseConfiguration().getDatabaseType();

         //test the db connection
         int testResult = OpConnectionManager.testConnection(databaseDriver, databaseUrl, databaseLogin, databasePassword);
         if (testResult != OpConnectionManager.SUCCESS) {
            logger.info("Something is wrong with the db connection parameters. Opening configuration wizard...");
            OpConfigurationWizardManager.loadConfigurationWizardModule();
            connectionTestCode = testResult;
            return initParams;
         }

         // the resource cache max size
         initParams.put(RESOURCE_CACHE_SIZE, configuration.getCacheConfiguration().getResourceCacheSize());

         // the security feature
         initParams.put(SECURE_SERVICE, configuration.getSecureService());

         // set smtp host for OpMailer
         OpMailer.setSMTPHostName(configuration.getSMTPServer());
         // initialize logging facility
         XLog.setLogFile(configuration.getLogFile());
         XLog.setLogLevel(configuration.getLogLevel());
         //set the debugging level for scripts
         XExpressSession.setSourceDebugging(configuration.getSourceDebugging());

         logger.info("Configuration loaded; Application is configured");
         runLevel = 1;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

         // Load modules and register prototypes
         OpModuleManager.load();

         logger.info("Registered modules loaded");
         runLevel = 2;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

         // Load and register default source
         OpHibernateSource defaultSource = (OpHibernateSource) OpSourceManager.getDefaultSource();
         if (defaultSource != null) {
            defaultSource.close();
         }
         defaultSource = new OpHibernateSource(databaseUrl, databaseDriver, databasePassword, databaseLogin, databaseType);
         OpSourceManager.registerSource(defaultSource);
         OpSourceManager.setDefaultSource(defaultSource);
         defaultSource.open();

         logger.info("Access to database is OK");
         runLevel = 3;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

         OpBroker broker = OpPersistenceManager.newBroker();

         //if db schema doesn't exist, create it
         if (!defaultSource.existsTable("op_object")) {
            createEmptySchema(broker);
         }

         logger.info("Repository status is OK");
         runLevel = 4;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

         //update db schema
         updateDBSchema(broker, defaultSource);

         logger.info("Updated database schema is OK");
         runLevel = 5;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

         broker.close();

         //Start registered modules
         OpModuleManager.start();

         logger.info("Registered modules started; Application started");
         runLevel = 6;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));
      }
      catch (Exception e) {
         logger.fatal("Cannot start the application", e);
      }
      return initParams;
   }

   private static void updateDBSchema(OpBroker broker, OpHibernateSource defaultSource)
        throws SQLException {

      Statement statement;
      int currentVersion = SCHEMA_VERSION;

      Connection jdbcConnection = broker.getJDBCConnection();
      int dbVersion;
      if (!defaultSource.existsTable("op_schema")) {
         statement = jdbcConnection.createStatement();
         statement.execute("create table op_schema(op_version int)");
         statement.executeUpdate("insert into op_schema values(0)");
         statement.close();
         jdbcConnection.commit();
         logger.info("Created table op_schema for versioning");
         dbVersion = 0;
      }
      else {
         statement = jdbcConnection.createStatement();
         ResultSet result = statement.executeQuery("select * from op_schema");
         result.next();
         dbVersion = result.getInt("op_version");
         statement.close();
      }

      logger.info("Version in db " + dbVersion + " and current version " + currentVersion);

      if (dbVersion < currentVersion) {
         logger.info("Updating DB schema to version " + currentVersion + "...");
         broker.updateSchema();
         statement = jdbcConnection.createStatement();
         statement.executeUpdate("update op_schema set op_version=" + currentVersion);
         statement.close();
         jdbcConnection.commit();
         logger.info("Updated OK. Current version is " + currentVersion);
         logger.info("Upgrading modules....");
         OpModuleManager.upgrade(dbVersion);
      }
   }

   public static int getConnectionTestCode() {
      return connectionTestCode;
   }

   /**
    * Creates an empty db schema.
    *
    * @param broker Broker used to access db.
    */
   private static void createEmptySchema(OpBroker broker) {
      broker.createSchema();
      // Create identification-related system objects (helpers supply their own transactions)
      OpUserService.createAdministrator(broker);
      OpUserService.createEveryone(broker);
      // Setup modules
      OpModuleManager.setup();
   }

   /**
    * Indicates whether the running mode is multi-user or not.
    *
    * @return a <code>boolean</code> indicating whether the running mode is multi-user or not.
    */
   public static boolean isMultiUser() {
      return multiUser;
   }

   /**
    * Checks the run level found in the parameters, and if necessary displays a message to the user.
    *
    * @param parameters a <code>HashMap</code> of <code>String,Object</code> pairs, representing form parameters.
    * @param localeId   a <code>String</code> representing the id of the current locale.
    * @param mapId      The error resource map ID.
    * @return A string representing an error message, if any. Null otherwise.
    */
   public static String checkRunLevel(HashMap parameters, String localeId, String mapId) {
      String runLevelParameter = (String) parameters.get(OpProjectConstants.RUN_LEVEL);
      if (runLevelParameter != null) {
         XLocalizer localizer = XLocaleManager.createLocalizer(localeId, mapId);

         int runLevel = Integer.valueOf(runLevelParameter).intValue();
         int successRunLevel = getSuccessRunLevel();
         if (runLevel < successRunLevel) {
            String resourceId = "{$" + OpProjectConstants.RUN_LEVEL + runLevelParameter + "}";
            return localizer.localize(resourceId);
         }
      }
      return null;
   }

   /**
    * Gets the application configuration.
    *
    * @return an <code>OpConfiguration</code> object representing the application configuration object.
    */
   public static OpConfiguration getConfiguration() {
      return configuration;
   }

   public static void resetDbSchema(OpBroker broker)
        throws SQLException {
      OpModuleManager.stop();
      broker.dropSchema();
      createEmptySchema(broker);
      updateSchemaVersion(broker);
      OpModuleManager.start();
   }

   private static void updateSchemaVersion(OpBroker broker)
        throws SQLException {
      logger.info("Updating schema version...");
      OpHibernateSource hibernateSource = (OpHibernateSource) OpSourceManager.getDefaultSource();
      updateDBSchema(broker, hibernateSource);
   }

   /**
    * Restores the db schema from the given file, via the backup manager. The restore drops the existent schema
    * and creates a new one.
    * @param filePath a <code>String</code> path to an existent backup file.
    * @param projectSession a <code>OpProjectSession</code> representing an application session.
    * @throws SQLException if the db schema cannot be droped or created.
    * @throws IOException
    */
   public static void restoreSchemaFromFile(String filePath, OpProjectSession projectSession)
        throws SQLException, IOException {
      OpBroker broker = projectSession.newBroker();
      logger.info("Dropping schema...");
      broker.dropSchema();
      logger.info("Creating schema...");
      broker.createSchema();
      broker.close();
      OpBackupManager.getBackupManager().restoreRepository(projectSession, filePath);
      broker = projectSession.newBroker();
      updateSchemaVersion(broker);
      broker.close();
   }
}
