/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.express.XDisplay;
import onepoint.express.server.XExpressSession;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpConnectionManager;
import onepoint.persistence.OpOrigObject;
import onepoint.persistence.OpPersistenceManager;
import onepoint.persistence.OpSource;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.configuration.OpDatabaseConfiguration;
import onepoint.project.configuration.OpInvalidDataBaseConfigurationException;
import onepoint.project.module.OpLanguageKitPath;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.configuration_wizard.OpConfigurationWizardManager;
import onepoint.project.modules.mail.OpMailer;
import onepoint.project.modules.setup.OpSetupModule;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLanguageKit;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocaleMap;
import onepoint.resource.XLocaleMapLoader;
import onepoint.resource.XResourceBroker;
import onepoint.util.XCalendar;

import org.hibernate.cfg.Configuration;

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
   private static final XLog logger = XLogFactory.getLogger(OpInitializer.class);

   /**
    * needed for initialization of multipled servlets
    */
   private static final Object MUTEX = new Object();

   private static final String LOCK_FILE_NAME = ".lock";

   /**
    * Run level of the application.
    */
   protected byte runLevel = 0;

   /**
    * The start form of the application.
    */
   protected String startForm;

   /**
    * The auto login start form of the application.
    */
   protected String autoLoginStartForm;

   /**
    * The code of the db connection test
    */
   private static int connectionTestCode = OpConnectionManager.SUCCESS;

   /**
    * Map containg information about the initialization steps taken by the initializer
    */
   protected Map<String, Object> initParams = new HashMap<String, Object>();

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

   private boolean updateDBSchema = true;

   private String configurationFileName;

   private static final HashMap<String, Object> lockFileExistsParams = new HashMap<String, Object>();
   
   static {
      lockFileExistsParams.put("resourceMap", "main.lockFileExistsDialog");
      lockFileExistsParams.put("title", "${lockFileExistsTitle}");
      lockFileExistsParams.put("message", "${lockFileExists}");
      lockFileExistsParams.put("option", XDisplay.OK_CANCEL_OPTION);
      lockFileExistsParams.put("type", XDisplay.INFORMATION_MESSAGE);
      lockFileExistsParams.put(OpProjectConstants.WIDTH, 550);
      lockFileExistsParams.put(OpProjectConstants.HEIGHT, 150);
   }

   /**
    * This class should not be instantiated randomly. You must get a valid instance from <code>OpInitializerFactory</code>
    */
   public OpInitializer() {
      String projectPath = OpEnvironmentManager.getOnePointHome();
      configurationFileName = projectPath + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME;
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
    * Returns the start form of the application
    *
    * @return <code>String</code> start form
    */
   public String getStartForm() {
      return startForm;
   }

   /**
    * Returns the auto login start form of the application
    *
    * @return <code>String</code> auto login start form
    */
   public String getAutoLoginStartForm() {
      return autoLoginStartForm;
   }

   public Map<String, Object> getInitParams() {
      return initParams;
   }
   
   /**
    * Performs application initilization steps.
    *
    * @param productCode a <code>String</code> representing the program code (the flavour of the application).
    * @return <code>Map</code> of init parameters.
    */
   public Map<String, Object> init(String productCode) {
      synchronized (MUTEX) {
         OpProjectCalendarFactory.register(new OpProjectCalendarFactory());
         XCalendar.register(new OpProjectCalendar());
         
         if (initialized) {
            return initParams;
         }
         //set the product code
         OpEnvironmentManager.setProductCode(productCode);

         logger.info("Application initialization started");
         runLevel = OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL;
         initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

         try {
            //initialize i18n files
            initLanguageResources();

            //perform the pre-initialization (if necessary)
            preInit();

            //load the configuration file
            try {
               loadConfigurationFile();
            }
            catch (OpInvalidDataBaseConfigurationException e) {
               logger.error("Could not load configuration file " + OpEnvironmentManager.getOnePointHome() + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME);
               //prepare the db wizard
               logger.info("Initializing db configuration wizard module...");
               OpConfigurationWizardManager.loadConfigurationWizardModule();

               determineStartForm();
               return initParams; //show db configuration wizard frame
            }

            // check for existing lock file
            OpSetupModule.loadModule();
            if (!OpEnvironmentManager.isMultiUser()) {
               runLevel = OpProjectConstants.LOCK_FILE_EXISTS_RUN_LEVEL;
               initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));
               String projectPath = OpEnvironmentManager.getOnePointHome();
               File lock = new File(new File(projectPath), LOCK_FILE_NAME);
               logger.info("Checking lock file at "+lock.getAbsolutePath());
               if (lock.exists()) {
                  initParams.put(OpProjectConstants.START_FORM, OpProjectConstants.LOCK_FILE_EXISTS_FORM);
                  initParams.put(OpProjectConstants.START_FORM_PARAMETERS, lockFileExistsParams);
                  return initParams;
                  //               lock.delete();
               }
               DateFormat dateFormater = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
               dateFormater.setTimeZone(XCalendar.GMT_TIMEZONE);
               FileWriter lockWriter = new FileWriter(lock);
               lockWriter.write(dateFormater.format(new Date()));
               lockWriter.close();
               lock = new File(new File(projectPath), LOCK_FILE_NAME);
               lock.deleteOnExit();
            }
            //initialize the database connection
            OpDatabaseConfiguration dbConfig = initDatabaseConnection();
            if (dbConfig == null) {
               logger.error("Something is wrong with the db connection parameters. Opening configuration wizard...");
               OpConfigurationWizardManager.loadConfigurationWizardModule();
               determineStartForm();
               return initParams;
            }
            
            logger.info("Configuration loaded; Application is configured");
            runLevel = OpProjectConstants.COULD_NOT_LOAD_REGISTERED_MODULES_RUN_LEVEL;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

            OpModuleManager.load();

            logger.info("Registered modules loaded");
            runLevel = OpProjectConstants.DATABASE_NOT_CONFIGURED_RUN_LEVEL;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

            // Load and register default source
            registerDefaultDataSources(dbConfig.getDatabaseUrl(), dbConfig.getDatabaseDriver(),
                 dbConfig.getDatabaseLogin(), dbConfig.getDatabasePassword(), dbConfig.getDatabaseType());
            logger.info("Access to database is OK");
            runLevel = OpProjectConstants.DATABASE_NOT_SET_UP_RUN_LEVEL;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

            //if db schema doesn't exist, create it
            createEmptySchema();

            //register additional datasources, only after the schema has been created
            registerAdditionalDataSources(dbConfig.getDatabaseUrl(), dbConfig.getDatabaseDriver(),
                 dbConfig.getDatabaseLogin(), dbConfig.getDatabasePassword(), dbConfig.getDatabaseType());

            logger.info("Repository status is OK");
            runLevel = OpProjectConstants.COULD_NOT_UPDATE_SCHEMA_RUN_LEVEL;
            initParams.put(OpProjectConstants.RUN_LEVEL, Byte.toString(runLevel));

            //update db schema
            if (updateDBSchema) {
            	try {
            		updateDBSchema(false);
            	} 
            	catch (SQLException exc) {
                    OpDatabaseConfiguration dbConfig2 = initDatabaseConnection();
                    if (dbConfig2 == null) {
                       logger.error("Something is wrong with the db connection parameters. Opening configuration wizard...");
                       OpConfigurationWizardManager.loadConfigurationWizardModule();
                       determineStartForm();
                       return initParams;
                    }
            	}
               logger.info("Updated database schema is OK");
            }
            runLevel = OpProjectConstants.COULD_NOT_START_REGISTERED_MODULES_RUN_LEVEL;
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
            determineStartForm();
            return initParams;
         }

         initialized = true;
         determineStartForm();
         return initParams;
      }
   }

   public void deleteLockFile() {
      String projectPath = OpEnvironmentManager.getOnePointHome();
      File lock = new File(new File(projectPath), LOCK_FILE_NAME);
      if (lock.exists()) {
         lock.delete();
      }
   }

   /**
    * Pre-initialization steps.
    */
   protected void preInit() {
   }

   /**
    * Determine the auto login start form and the start form of the application based on the run level.
    */
   protected void determineStartForm() {
      startForm = OpEnvironmentManager.getStartForm();
      autoLoginStartForm = OpEnvironmentManager.getAutoLoginStartForm();
      initParams.put(OpProjectConstants.START_FORM_PARAMETERS, null);
      if (runLevel == OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL) {
         startForm = OpProjectConstants.CONFIGURATION_WIZARD_FORM;
      }
      if (runLevel == OpProjectConstants.LOCK_FILE_EXISTS_RUN_LEVEL) {
         startForm = OpProjectConstants.LOCK_FILE_EXISTS_FORM;
         initParams.put(OpProjectConstants.START_FORM_PARAMETERS, lockFileExistsParams);
      }
      initParams.put(OpProjectConstants.START_FORM, startForm);
      initParams.put(OpProjectConstants.AUTO_LOGIN_START_FORM, autoLoginStartForm);
   }

   /**
    * Loads the configuration file which will be used by the initializer.
    *
    * @throws OpInvalidDataBaseConfigurationException
    *          if the configuration file cannot be loaded.
    */
   protected void loadConfigurationFile()
        throws OpInvalidDataBaseConfigurationException {
      // Read configuration file
      OpConfigurationLoader configurationLoader = new OpConfigurationLoader();
      String projectPath = OpEnvironmentManager.getOnePointHome();
      this.configuration = configurationLoader.loadConfiguration(getConfigurationFileName());

      //check attachment size
      if (this.configuration.getMaxAttachmentSize() > OpConfiguration.DEFAULT_MAX_ATTACHMENT_SIZE) {
         logger.warn("Values higher than " + OpConfiguration.DEFAULT_MAX_ATTACHMENT_SIZE + " MB for attachments may need more mamory than available and thus cause loss of data or corrupt the whole project");
      }

      // initialize logging facility
      String logFile = configuration.getLogFile();
      if (logFile != null && !new File(logFile).isAbsolute()) {
         logFile = projectPath + "/" + logFile;
      }
      XLogFactory.initializeLogging(logFile, configuration.getLogLevel());

      // set smtp host for OpMailer
      OpMailer.setSMTPHostName(configuration.getSMTPServer());

      //set the debugging level for scripts
      XExpressSession.setSourceDebugging(configuration.getSourceDebugging());
   }

   public void setConfigurationFileName(String configurationFileName) {
      this.configurationFileName = configurationFileName;
   }
   
   public String getConfigurationFileName() {
      return configurationFileName;
   }

   /**
    * Initializes the database connection, based on the configuration settings.
    *
    * @return a <code>OpDatabaseConfiguration</code> object if the settings are correct
    *         or <code>null</code> if the settings are incorrect and/or a connection to the db
    *         could not be established.
    */
   protected OpDatabaseConfiguration initDatabaseConnection() {
      //get the db connection parameters
      //<FIXME author="Horia Chiorean" description="What happens for multiple DBs ?">
      OpDatabaseConfiguration dbConfig = configuration.getDatabaseConfigurations().iterator().next();
      //<FIXME>
      String databaseUrl = dbConfig.getDatabaseUrl();
      String databaseDriver = dbConfig.getDatabaseDriver();
      String databasePassword = dbConfig.getDatabasePassword();
      String databaseLogin = dbConfig.getDatabaseLogin();
      int databaseType = dbConfig.getDatabaseType();

      //test the db connection
      connectionTestCode = OpConnectionManager.testConnection(databaseDriver, databaseUrl, databaseLogin, databasePassword, databaseType);
      if (connectionTestCode == OpConnectionManager.SUCCESS) {
         return dbConfig;
      }
      else {
         return null;
      }
   }

   /**
    * Registers the default datasources.
    *
    * @param databaseUrl      databse URL
    * @param databaseDriver   database driver class
    * @param databaseLogin    database user
    * @param databasePassword database user password
    * @param databaseType     database type
    */
   private void registerDefaultDataSources(String databaseUrl, String databaseDriver,
        String databaseLogin, String databasePassword, int databaseType) {

      // close all existing data sources.
      OpSourceManager.closeAllSources();

      for (OpSource dataSource : createDefaultSources(databaseUrl, databaseDriver, databaseLogin, databasePassword, databaseType)) {
         registerSource(dataSource);
      }
   }

   /**
    * Registers the default datasources.
    *
    * @param databaseUrl      databse URL
    * @param databaseDriver   database driver class
    * @param databaseLogin    database user
    * @param databasePassword database user password
    * @param databaseType     database type
    */
   private void registerAdditionalDataSources(String databaseUrl, String databaseDriver,
        String databaseLogin, String databasePassword, int databaseType) {
      for (OpSource dataSource : createAdditionalSources(databaseUrl, databaseDriver, databaseLogin, databasePassword, databaseType)) {
         registerSource(dataSource);
      }
   }

   /**
    * Register and open a source
    *
    * @param source sorce to be registered.
    */
   protected void registerSource(OpSource source) {
      OpSourceManager.registerSource(source);
      source.open();
   }

   /**
    * Creates the additonal sources with the given parameters.
    *
    * @param databaseUrl      connection databse URL
    * @param databaseDriver   database JDBC driver class
    * @param databasePassword connection user password
    * @param databaseLogin    connection user
    * @param databaseType     database type.
    * @return a <code>OpSource</code> instance.
    */
   protected Set<OpSource> createAdditionalSources(String databaseUrl, String databaseDriver,
        String databaseLogin, String databasePassword, int databaseType) {
      return Collections.EMPTY_SET;
   }

   /**
    * Creates the default sources with the given parameters.
    *
    * @param databaseUrl      connection databse URL
    * @param databaseDriver   database JDBC driver class
    * @param databasePassword connection user password
    * @param databaseLogin    connection user
    * @param databaseType     database type.
    * @return a <code>OpSource</code> instance.
    */
   protected Set<OpSource> createDefaultSources(String databaseUrl, String databaseDriver, String databaseLogin,
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
    * @param fromFile 
    *
    * @return <code>true</code> if the update was successfull.
    */
   private boolean updateDBSchema(boolean fromFile) 
   	throws SQLException {
      Collection<OpSource> allSources = OpSourceManager.getAllSources();
      boolean updated = false;
      for (OpSource source : allSources) {
         OpHibernateSource hibernateSource = (OpHibernateSource) source;

         int existingVersionNr = hibernateSource.getExistingSchemaVersionNumber();
         if (existingVersionNr < OpHibernateSource.SCHEMA_VERSION) {
            logger.info("Updating DB schema from version " + existingVersionNr + " (actual version "+OpHibernateSource.SCHEMA_VERSION+")...");

            if (existingVersionNr <= 81) {
               // required to add row for customValuePage
                Configuration configuration = hibernateSource.getConfiguration();
                if (configuration.getClassMapping(OpOrigObject.class.getName()) == null) { // not previously added
                   // required to add row for customValuePage
                   try {
                      Configuration oldConfiguration = hibernateSource.createConfiguration();
                      oldConfiguration = oldConfiguration.addClass(OpOrigObject.class);
                      hibernateSource.setConfiguration(oldConfiguration);
                      hibernateSource.setNewConfiguration(configuration);
                   } catch (UnsupportedEncodingException e) {
                      // TODO Auto-generated catch block
                      e.printStackTrace();
                   } catch (IOException e) {
                      // TODO Auto-generated catch block
                      e.printStackTrace();
                   }
                }
            }

            OpPersistenceManager.updateDBSchema();
            OpModuleManager.upgrade(existingVersionNr, OpHibernateSource.SCHEMA_VERSION);
            OpModuleManager.checkModules();
            hibernateSource.updateSchemaVersionNumber(OpHibernateSource.SCHEMA_VERSION);
            updated = true;
         }
      }
      return updated;
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
    * Creates an empty db schema, if necessary. The check to see whether the schema is
    * empty or not is done by searching the global "op_object" table.
    *
    * @throws UnsupportedOperationException if the default application source has not
    *                                       been registered with the <code>OpSourceManager</code>.
    */
   private void createEmptySchema() {
      OpSource defaultSource = OpSourceManager.getDefaultSource();
      if (defaultSource == null) {
         throw new UnsupportedOperationException("The default source has not been registered yet ");
      }

     	if (!defaultSource.existsTable(OpProjectConstants.OP_PERMISSION_TABLE_NAME)) {
         OpPersistenceManager.createSchema();
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
    * Resets the db schema by dropping the existent one and creating a new one.  It's important here to not loose consistency
    * in the hibernate hi-lo generator.
    * <FIXME author="Horia Chiorean" description="Currently this method is used only from tests">
    *
    * @throws SQLException if the db schema cannot be droped or created.
    */
   public void resetDbSchema()
        throws SQLException {
      logger.info("Stopping modules");
      OpModuleManager.stop();
      
//      logger.info("Updating DB schema...");
//      OpPersistenceManager.updateDBSchema();
//      logger.info("Dropping schema...");
//      OpPersistenceManager.dropSchema();

      OpSourceManager.clearAllSources();

//      logger.info("Creating schema...");
//      createEmptySchema();

      logger.info("Updating schema...");
      updateDBSchema(false);

      logger.info("Starting modules");
      OpModuleManager.start();
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
      OpBackupManager.getBackupManager().restoreRepository(projectSession, filePath);
      OpSourceManager.clearAllSources();
      this.updateDBSchema(true);
   }

   public boolean isUpdateDBSchema() {
      return updateDBSchema;
   }

   public void setUpdateDBSchema(boolean updateDBSchema) {
      this.updateDBSchema = updateDBSchema;
   }
}
