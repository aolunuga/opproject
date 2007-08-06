/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import onepoint.express.server.XExpressSession;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.configuration.OpInvalidDataBaseConfigurationException;
import onepoint.project.configuration.OpDatabaseConfiguration;
import onepoint.project.module.OpLanguageKitPath;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.configuration_wizard.OpConfigurationWizardManager;
import onepoint.project.modules.mail.OpMailer;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class responsible for performing application initialization steps
 *
 * @author ovidiu.lupas
 */
public class OpInitializer {

   /**
    * This class's logger
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpInitializer.class);

   /**
    * needed for initialization of multipled servlets
    */
   private static final Object MUTEX = new Object();

   /**
    * Run level of the application.
    */
   protected byte runLevel = 0;

   /**
    * The code of the db connection test
    */
   private static int connectionTestCode = OpConnectionManager.SUCCESS;

   /**
    * Map containg information about the initialization steps taken by the initializer
    */
   protected Map<String, String> initParams = new HashMap<String, String>();

   /**
    * The configuration object initialized by this class
    */
   private OpConfiguration configuration = null;

   /**
    * Flag indicatig whether the language settings have been initialized or not.
    */
   private boolean languageInitialized = false;

   /**
    * state of initialization
    */
   private boolean initialized = false;

   /**
    * This class should not be instantiated randomly. You must get a valid instance from <code>OpInitializerFactory</code>
    */
   public OpInitializer() {
   }

   /**
    * Returns the run level of the application
    *
    * @return <code>byte</code> run level
    */
   public byte getRunLevel() {
      return runLevel;
   }

   /**
    * Performs application initilization steps.
    *
    * @param productCode a <code>String</code> representing the program code (the flavour of the application).
    * @return <code>Map</code> of init parameters.
    */
   public Map<String, String> init(String productCode) {
      synchronized (MUTEX) {
         if (initialized) {
            return initParams;
         }
         //set the product code
         OpEnvironmentManager.setProductCode(productCode);

         logger.info("Application initialization started");
         runLevel = 0;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

         try {
            initLanguageResources();

            // Read configuration file
            OpConfigurationLoader configurationLoader = new OpConfigurationLoader();
            String projectPath = OpEnvironmentManager.getOnePointHome();

            preInit();

            try {
               this.configuration = configurationLoader.loadConfiguration(projectPath + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME);
            }
            catch (OpInvalidDataBaseConfigurationException e) {
               logger.error("Could not load configuration file " + projectPath + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME);
               //prepare the db wizard
               logger.info("Initializing db configuration wizard module...");
               OpConfigurationWizardManager.loadConfigurationWizardModule();

               return initParams; //show db configuration wizard frame
            }


            // initialize logging facility
            String logFile = configuration.getLogFile();
            if (logFile != null && !new File(logFile).isAbsolute()) {
               logFile = projectPath + "/" + logFile;
            }
            XLogFactory.initializeLogging(logFile, configuration.getLogLevel());

            //get the db connection parameters
            OpDatabaseConfiguration dbConfig = configuration.getDatabaseConfigurations().iterator().next();
            String databaseUrl = dbConfig.getDatabaseUrl();
            String databaseDriver = dbConfig.getDatabaseDriver();
            String databasePassword = dbConfig.getDatabasePassword();
            String databaseLogin = dbConfig.getDatabaseLogin();
            int databaseType = dbConfig.getDatabaseType();

            //test the db connection
            int testResult = OpConnectionManager.testConnection(databaseDriver, databaseUrl, databaseLogin, databasePassword, databaseType);
            connectionTestCode = testResult;
            if (testResult != OpConnectionManager.SUCCESS) {
               logger.info("Something is wrong with the db connection parameters. Opening configuration wizard...");
               OpConfigurationWizardManager.loadConfigurationWizardModule();
               return initParams;
            }

            // set smtp host for OpMailer
            OpMailer.setSMTPHostName(configuration.getSMTPServer());

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
            OpSource defaultSource = OpSourceManager.getDefaultSource();
            if (defaultSource != null) {
               defaultSource.close();
            }
            defaultSource = createSource(databaseUrl, databaseDriver, databasePassword, databaseLogin, databaseType);
            OpSourceManager.registerDefaultSource(defaultSource);
            defaultSource.open();

            logger.info("Access to database is OK");
            runLevel = 3;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

            //if db schema doesn't exist, create it
            createEmptySchema();

            logger.info("Repository status is OK");
            runLevel = 4;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

            //update db schema
            updateDBSchema();

            logger.info("Updated database schema is OK");
            runLevel = 5;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

            //Start registered modules
            OpModuleManager.start();

            logger.info("Registered modules started; Application started");
            runLevel = OpProjectConstants.SUCCESS_RUN_LEVEL;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));
         }
         catch (Exception e) {
            logger.fatal("Cannot start the application", e);
            initialized = false;
            return initParams;
         }

         initialized = true;
         return initParams;
      }
   }

   /**
    * Pre-initialization steps.
    */
   protected void preInit() {
   }

   /**
    * Creates a new instance of <code>OpHibernateSource</code> with the given parameters.
    *
    * @param databaseUrl      connection databse URL
    * @param databaseDriver   database JDBC driver class
    * @param databasePassword connection user password
    * @param databaseLogin    connection user
    * @param databaseType     database type.
    * @return
    */
   protected OpSource createSource(String databaseUrl, String databaseDriver, String databasePassword,
        String databaseLogin, int databaseType) {
      return new OpHibernateSource(databaseUrl, databaseDriver, databasePassword, databaseLogin, databaseType);
   }

   /**
    * Initializes the default language settings.
    */
   private void initLanguageResources() {
      if (languageInitialized) {
         return;
      }

      XResourceBroker.setResourcePath(OpProjectConstants.PROJECT_PACKAGE);
      // Attention: Locale map must be loaded and set before starting up modules
      XLocaleMap localeMap = new XLocaleMapLoader().loadLocaleMap("/locales.olm.xml");
      if (localeMap != null) {
         XLocaleManager.setLocaleMap(localeMap);
      }

      // load language resources for main application forms (e.g. login.oxf)
      OpLanguageKitPath mainPath = new OpLanguageKitPath("/i18n");
      List kits = mainPath.loadLanguageKits();
      for (Object kit1 : kits) {
         XLanguageKit kit = (XLanguageKit) kit1;
         XLocaleManager.registerLanguageKit(kit);
      }

      languageInitialized = true;
   }

   /**
    * Updates the db schema if necessary.
    */
   private void updateDBSchema() {
      OpHibernateSource defaultSource = (OpHibernateSource) OpSourceManager.getDefaultSource();
      int existingVersionNr = defaultSource.getExistingSchemaVersionNumber();
      if (existingVersionNr < OpHibernateSource.SCHEMA_VERSION) {
         logger.info("Updating DB schema from version " + existingVersionNr + "...");
         OpPersistenceManager.updateSchema();
         defaultSource.updateSchemaVersionNumber();
         OpModuleManager.upgrade(existingVersionNr, OpHibernateSource.SCHEMA_VERSION);
      }
   }

   /**
    * Returns connection test code.
    *
    * @return connection test code
    */
   public int getConnectionTestCode() {
      return connectionTestCode;
   }

   /**
    * Creates an empty db schema, if necessary.
    */
   private void createEmptySchema() {
      OpHibernateSource hibernateSource = (OpHibernateSource) OpSourceManager.getDefaultSource();
      if (!hibernateSource.existsTable("op_object")) {
         OpPersistenceManager.createSchema();
         OpBroker broker = OpPersistenceManager.newBroker();
         // Create identification-related system objects (helpers supply their own transactions)
         OpUserService.createAdministrator(broker);
         OpUserService.createEveryone(broker);
         broker.close();
      }
   }

   /**
    * Gets the application configuration.
    *
    * @return an <code>OpConfiguration</code> object representing the application configuration object.
    */
   public OpConfiguration getConfiguration() {
      return configuration;
   }

   /**
    * Resets the db schema by dropping the existent one and creating a new one.
    *
    * @throws SQLException if the db schema cannot be droped or created.
    */
   public void resetDbSchema()
        throws SQLException {
      logger.info("Stopping modules");
      OpModuleManager.stop();

      logger.info("Dropping schema...");
      OpPersistenceManager.dropSchema();

      logger.info("Creating schema...");
      createEmptySchema();

      logger.info("Starting modules");
      OpSourceManager.getDefaultSource().clear();
      OpModuleManager.start();

      logger.info("Updating schema...");
      updateDBSchema();
   }

   /**
    * Restores the db schema from the given file, via the backup manager. The restore drops the existent schema
    * and creates a new one.
    *
    * @param filePath       a <code>String</code> path to an existent backup file.
    * @param projectSession a <code>OpProjectSession</code> representing an application session.
    * @throws SQLException if the db schema cannot be droped or created.
    * @throws IOException  if the repository cannot be restored from the given file.
    */
   public void restoreSchemaFromFile(String filePath, OpProjectSession projectSession)
        throws SQLException, IOException {
      logger.info("Dropping schema...");
      OpPersistenceManager.dropSchema();

      logger.info("Creating schema...");
      OpPersistenceManager.createSchema();
      OpBackupManager.getBackupManager().restoreRepository(projectSession, filePath);
      OpSourceManager.getDefaultSource().clear();
      updateDBSchema();
   }
}
