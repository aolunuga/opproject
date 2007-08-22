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
import onepoint.project.configuration.OpDatabaseConfiguration;
import onepoint.project.configuration.OpInvalidDataBaseConfigurationException;
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
import java.util.*;

/**
 * Service class responsible for performing application initialization steps
 *
 * @author ovidiu.lupas
 */
public class OpInitializer {

   /**
    * The number of bytes in a MB.
    */
   private static final int MB_TO_BYTE_CONVERSION_UNIT = 1048576;

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

            //check attachment size
            if (this.configuration.getMaxAttachmentSize() > OpConfiguration.DEFAULT_MAX_ATTACHMENT_SIZE) {
               logger.info("Values higher than " + OpConfiguration.DEFAULT_MAX_ATTACHMENT_SIZE + " MB for attachments may lead to OutOfMemoryExceptions and thus cause loss of data or corrupt the whole project");
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
            OpModuleManager.load();

            logger.info("Registered modules loaded");
            runLevel = 2;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

            // Load and register default source
            registerDataSources(databaseUrl, databaseDriver, databaseLogin, databasePassword, databaseType);
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
    * Register datasources.
    *
    * @param databaseUrl      databse URL
    * @param databaseDriver   database driver class
    * @param databaseLogin    database user
    * @param databasePassword database user password
    * @param databaseType     database type
    */
   private void registerDataSources(String databaseUrl, String databaseDriver,
        String databaseLogin, String databasePassword, int databaseType) {

      // close all existing data sources.
      OpSourceManager.closeAllSources();

      for (OpSource dataSource : createSources(databaseUrl, databaseDriver, databaseLogin, databasePassword, databaseType)) {
         OpSourceManager.registerSource(dataSource);
         dataSource.open();
      }
   }

   /**
    * Creates a new instance of <code>OpHibernateSource</code> with the given parameters.
    *
    * @param databaseUrl      connection databse URL
    * @param databaseDriver   database JDBC driver class
    * @param databasePassword connection user password
    * @param databaseLogin    connection user
    * @param databaseType     database type.
    * @return a <code>OpSource</code> instance.
    */
   protected Set<OpSource> createSources(String databaseUrl, String databaseDriver, String databaseLogin,
        String databasePassword, int databaseType) {
      Set<OpSource> sources = new HashSet<OpSource>(1);
      sources.add(new OpHibernateSource(OpSource.DEFAULT_SOURCE_NAME, databaseUrl, databaseDriver,
           databaseLogin, databasePassword, databaseType));

      return sources;
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
      Collection<OpSource> allSources = OpSourceManager.getAllSources();

      for (OpSource source : allSources) {
         OpHibernateSource hibernateSource = (OpHibernateSource) source;

         int existingVersionNr = hibernateSource.getExistingSchemaVersionNumber();
         if (existingVersionNr < OpHibernateSource.SCHEMA_VERSION) {
            logger.info("Updating DB schema from version " + existingVersionNr + "...");
            OpPersistenceManager.updateSchema();
            hibernateSource.updateSchemaVersionNumber();
            OpModuleManager.upgrade(existingVersionNr, OpHibernateSource.SCHEMA_VERSION);
         }
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
      Collection<OpSource> allSources = OpSourceManager.getAllSources();
      for (OpSource source : allSources) {
         if (!source.existsTable("op_object")) {
            OpPersistenceManager.createSchema();
         }

         OpBroker broker = OpPersistenceManager.newBroker(source.getName());
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
    * Returns the maximum size configured for attachments, in bytes.
    *
    * @return a <code>long</code> value representing a number of bytes.
    */
   public long getMaxAttachmentSizeBytes() {
      if (configuration == null) {
         return OpConfiguration.DEFAULT_MAX_ATTACHMENT_SIZE * MB_TO_BYTE_CONVERSION_UNIT;
      }
      return configuration.getMaxAttachmentSize() * MB_TO_BYTE_CONVERSION_UNIT;
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
      OpSourceManager.clearAllSources();

      logger.info("Creating schema...");
      createEmptySchema();

      logger.info("Starting modules");
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
      OpSourceManager.clearAllSources();
      updateDBSchema();
   }
}
