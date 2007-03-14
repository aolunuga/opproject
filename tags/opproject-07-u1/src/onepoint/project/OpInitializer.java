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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class responsible for performing application initialization steps
 *
 * @author ovidiu.lupas
 *         <FIXME author="Horia Chiorean" description="Not very OO-oriented that this class is static...(same as the 'factories')/>
 */
public final class OpInitializer {
   /**
    * Success Run Level.
    */
   public static final byte SUCCESS_RUN_LEVEL = 6;

   /**
    * This class's logger
    */
   private static final XLog logger = XLogFactory.getLogger(OpInitializer.class, true);

   /**
    * A map of [productCode, boolean] pairs, indicating which application is multi user and which is not.
    */
   private static final Map PRODUCT_CODES_MAP = new HashMap();

   /**
    * Run level of the application.
    */
   private static byte runLevel = 0;

   /**
    * The code of the db connection test
    */
   private static int connectionTestCode = OpConnectionManager.SUCCESS;

   /**
    * Map containg information about the initialization steps taken by the initializer
    */
   private static Map initParams = new HashMap();

   /**
    * The configuration object initialized by this class
    */
   private static OpConfiguration configuration = null;

   /**
    * The product code used in the initialization process
    */
   private static String productCode = null;

   /**
    * Initialize the product codes map
    */
   static {
      PRODUCT_CODES_MAP.put(OpProjectConstants.BASIC_EDITION_CODE, Boolean.FALSE);
      PRODUCT_CODES_MAP.put(OpProjectConstants.PROFESSIONAL_EDITION_CODE, Boolean.FALSE);
      PRODUCT_CODES_MAP.put(OpProjectConstants.OPEN_EDITION_CODE, Boolean.TRUE);
      PRODUCT_CODES_MAP.put(OpProjectConstants.TEAM_EDITION_CODE, Boolean.TRUE);
   }

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
      return SUCCESS_RUN_LEVEL;
   }

   /**
    * Performs application initilization steps.
    *
    * @param productCode a <code>String</code> representing the program code (the flavour of the application).
    * @return <code>Map</code> of init parameters.
    */
   public static Map init(String productCode) {
      //set the product code
      OpInitializer.productCode = productCode;

      logger.info("Application initialization started");
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

         // Read configuration file
         OpConfigurationLoader configurationLoader = new OpConfigurationLoader();
         String projectPath = OpEnvironmentManager.getOnePointHome();
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

         // initialize logging facility

         String logFile = configuration.getLogFile();
         if (logFile != null && !new File(logFile).isAbsolute()) {
            logFile = projectPath + "/" + logFile;  
         }
         XLogFactory.initializeLogging(logFile, configuration.getLogLevel());

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
         runLevel = 6;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));
      }
      catch (Exception e) {
         logger.fatal("Cannot start the application", e);
      }
      return initParams;
   }

   /**
    * Updates the db schema if necessary.
    */
   private static void updateDBSchema() {
      OpHibernateSource defaultSource = (OpHibernateSource) OpSourceManager.getDefaultSource();
      if (defaultSource.needSchemaUpgrading()) {
         int existingVersionNr = defaultSource.getExistingSchemaVersionNumber();
         logger.info("Updating DB schema from version " + existingVersionNr + "...");
         OpPersistenceManager.updateSchema();
         defaultSource.updateSchemaVersionNumber();
         OpModuleManager.upgrade(existingVersionNr);
      }
   }

   public static int getConnectionTestCode() {
      return connectionTestCode;
   }

   /**
    * Creates an empty db schema, if necessary.
    */
   private static void createEmptySchema() {
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
    * Indicates whether the running mode is multi-user or not.
    *
    * @return a <code>boolean</code> indicating whether the running mode is multi-user or not.
    */
   public static boolean isMultiUser() {
      Boolean isMultiUser = (Boolean) PRODUCT_CODES_MAP.get(productCode);
      if (isMultiUser == null) {
         throw new UnsupportedOperationException("Cannot determine whether application is multi user or not");
      }
      return isMultiUser.booleanValue();
   }


   /**
    * Gets the product code registered with the initializer class.
    * @return a <code>String</code> representing the product code, which indicates the flavour of the application.
    */
   public static String getProductCode() {
      return productCode;
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

   /**
    * Resets the db schema by dropping the existent one and creating a new one.
    *
    * @throws SQLException if the db schema cannot be droped or created.
    */
   public static void resetDbSchema()
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
    * @param filePath a <code>String</code> path to an existent backup file.
    * @param projectSession a <code>OpProjectSession</code> representing an application session.
    * @throws SQLException if the db schema cannot be droped or created.
    * @throws IOException if the repository cannot be restored from the given file.
    */
   public static void restoreSchemaFromFile(String filePath, OpProjectSession projectSession)
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
