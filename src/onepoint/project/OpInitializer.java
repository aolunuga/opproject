/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project;

import onepoint.express.server.XExpressSession;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpPersistenceManager;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.module.OpLanguageKitFile;
import onepoint.project.module.OpModuleException;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.configuration_wizard.OpConfigurationWizardManager;
import onepoint.project.modules.mail.OpMailer;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.util.OpProjectConstants;
import onepoint.util.XEnvironment;

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
 */
public class OpInitializer {

   /*keys in the initParams map */
   public static String RESOURCE_CACHE_SIZE = "cacheSize";
   public static String SECURE_SERVICE = "secureService";

   /*run level of the application. */
   private static byte runLevel;
   /*init params map that will be returned after initialization is performed */
   private static Map initParams = new HashMap();
   /*class logger */
   private static final XLog logger = XLogFactory.getLogger(OpInitializer.class, true);
   private static boolean emptyDB;

   /*this class should not be instantiated */
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
    * Performs application initilization steps.
    *
    * @param projectPath <code>String</code> the absolute path to the folder which contains the configuration files
    * @return <code>Map</code> of init parameters
    */
   public static Map init(String projectPath) {

      logger.info("Application initialization started");
      runLevel = 0;
      initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

      try {

         XResourceBroker.setResourcePath(OpConfiguration.PROJECT_PACKAGE);
         // Attention: Locale map must be loaded and set before starting up modules
         XLocaleMap locale_map = new XLocaleMapLoader().loadLocaleMap("/locales.olm.xml");
         XLocaleManager.setLocaleMap(locale_map);

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
            XEnvironment.setVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME, projectPath);
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
         OpHibernateSource defaultSource = new OpHibernateSource();
         defaultSource.setDatabaseType(configuration.getDatabaseConfiguration().getDatabaseType());
         defaultSource.setDriverClassName(configuration.getDatabaseConfiguration().getDatabaseDriver());
         defaultSource.setURL(configuration.getDatabaseConfiguration().getDatabaseUrl());
         defaultSource.setLogin(configuration.getDatabaseConfiguration().getDatabaseLogin());
         defaultSource.setPassword(configuration.getDatabaseConfiguration().getDatabasePassword());
         defaultSource.setCacheCapacity(configuration.getCacheConfiguration().getCacheSize());
         defaultSource.setHsqlDatabaseType(onepoint.project.configuration.OpConfiguration.HSQL_DB_TYPE);
         defaultSource.setHsqlDatabasePath(onepoint.project.configuration.OpConfiguration.getHSQLDbPath());
         if (defaultSource.getDatabaseType() == OpHibernateSource.HSQLDB) {
            defaultSource.setEmbeded(true);
         }

         OpSourceManager.registerSource(defaultSource);
         OpSourceManager.setDefaultSource(defaultSource);
         // Must be called before creating the schema (Hibernate)
         defaultSource.open();

         logger.info("Access to database is OK");
         runLevel = 3;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

         //Check repository status

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

         //check if the db is empty
         emptyDB = checkForEmptyDB(broker);
         broker.close();

         //Start registered modules
         OpModuleManager.start();

         logger.info("Registered modules started; Application started");
         runLevel = 6;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));
      }
      catch(OpModuleException e) {
         if (e.getResourceId() != null && e.getResourceMapId() != null) {
            initParams.put(OpProjectConstants.RESOURCE_ID, e.getResourceId());
            initParams.put(OpProjectConstants.RESOURCE_MAP_ID, e.getResourceMapId());
         }
         else {
            logger.error("Cannot load an application module", e);
         }
      }
      catch (Exception e) {
         logger.fatal("Cannot start the application", e);
      }
      return initParams;
   }

   private static boolean checkForEmptyDB(OpBroker broker)
        throws SQLException {

      Connection jdbcConnection = broker.getJDBCConnection();
      Statement statement = jdbcConnection.createStatement();
      ResultSet result = statement.executeQuery("select count(*) from op_projectnode");
      result.next();
      int projects = result.getInt(1);
      statement.close();
      //just one projectNode (the sys object - root portfolio)
      return (projects <= 1);
   }

   private static void updateDBSchema(OpBroker broker, OpHibernateSource defaultSource)
        throws SQLException {

      Statement statement;
      int currentVersion = onepoint.project.configuration.OpConfiguration.SCHEMA_VERSION;

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

   /**
    * Creates an empty db schema.
    *
    */
   private static void createEmptySchema(OpBroker broker) {
      broker.createSchema();
      // Create identification-related system objects (helpers supply their own transactions)
      OpUserService.createAdministrator(broker);
      OpUserService.createEveryone(broker);
      // Setup modules
      OpModuleManager.setup();
   }

   public static boolean isEmptyDB() {
      return emptyDB;
   }
}
