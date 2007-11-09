/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpConnectionManager;
import onepoint.persistence.OpSource;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.configuration.OpConfigurationValuesHandler;
import onepoint.project.configuration.OpDatabaseConfiguration;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;
import onepoint.util.XEnvironmentManager;
import org.w3c.dom.*;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service class for the db configuration wizard.
 *
 * @author horia.chiorean
 */
public class OpConfigurationWizardService extends OpProjectService {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpConfigurationWizardService.class);

   /**
    * Request argument names.
    */
   private static final String PARAMETERS = "parameters";

   /**
    * Error map.
    */
   private static final OpDbConfigurationWizardErrorMap ERROR_MAP = new OpDbConfigurationWizardErrorMap();

   public static final String MYSQL_INNO_DB_DISPLAY = "MySQL InnoDB";
   public static final String ORACLE_DISPLAY = "Oracle";
   public static final String IBM_DB_DISPLAY = "IBM DB/2";
   public static final String MSSQL_DISPLAY = "Microsoft SQL Server";
   public static final String POSTGRE_DISPLAY = "PostgreSQL";

   private static final Map<String, String> DISPLAY_TO_DB_TYPE = new HashMap<String, String>();
   public final static String DEMODATATA_FILE_PATH = "demodata/demodata.opx.xml";

   static {
      DISPLAY_TO_DB_TYPE.put(MYSQL_INNO_DB_DISPLAY, OpConfigurationValuesHandler.MYSQL_INNO_DB_TYPE);
      DISPLAY_TO_DB_TYPE.put(ORACLE_DISPLAY, OpConfigurationValuesHandler.ORACLE_DB_TYPE);
      DISPLAY_TO_DB_TYPE.put(IBM_DB_DISPLAY, OpConfigurationValuesHandler.IBM_DB2_DB_TYPE);
      DISPLAY_TO_DB_TYPE.put(MSSQL_DISPLAY, OpConfigurationValuesHandler.MSSQL_DB_TYPE);
      DISPLAY_TO_DB_TYPE.put(POSTGRE_DISPLAY, OpConfigurationValuesHandler.POSTGRESQL_DB_TYPE);
   }

   /**
    * Writes the db configuration file, with the db settings.
    *
    * @param session a <code>XSession</code> representing the server session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the response.
    */
   public XMessage writeDatabaseConfigurationFile(OpProjectSession session, XMessage request) {

      HashMap parameters = (HashMap) (request.getArgument(PARAMETERS));

      Boolean isStandaloneParameter = (Boolean) parameters.get("is_standalone");
      boolean isStandalone = (isStandaloneParameter != null) && isStandaloneParameter;

      String databaseType = (String) parameters.get("database_type");
      if (DISPLAY_TO_DB_TYPE.get(databaseType) != null) {
         databaseType = DISPLAY_TO_DB_TYPE.get(databaseType);
      }
      String databaseDriver = (String) onepoint.project.configuration.OpConfiguration.DATABASE_DRIVERS.get(databaseType);

      //response message
      XMessage response = new XMessage();

      //perform fields validation
      String databaseURL = (String) parameters.get("database_url");
      if (databaseURL == null) {
         if (!isStandalone) {
            response.setError(session.newError(ERROR_MAP, OpDbConfigurationWizardError.DATABASE_URL_MISSING));
         }
         else {
            response.setError(session.newError(ERROR_MAP, OpDbConfigurationWizardError.DATABASE_PATH_MISSING));
         }
         return response;
      }
      else if (isStandalone) {
         String dataFolder = XEnvironmentManager.convertPathToSlash(databaseURL);
         StringBuffer dbPath = new StringBuffer(dataFolder);
         dbPath.append(File.separator);
         dbPath.append(OpProjectConstants.DB_DIR_NAME);
         dbPath.append(File.separator);
         dbPath.append(OpProjectConstants.DB_FILE_NAME);
         String databasePath = XEnvironmentManager.convertPathToSlash(dbPath.toString());

         OpEnvironmentManager.setDataFolderPathFromDbPath(databasePath);

         StringBuffer dbUrl = new StringBuffer(OpHibernateSource.DERBY_JDBC_CONNECTION_PREFIX);
         dbUrl.append(databasePath);
         dbUrl.append(OpHibernateSource.DERBY_JDBC_CONNECTION_SUFIX);
         databaseURL = dbUrl.toString();
      }

      String databaseLogin = (String) parameters.get("database_login");
      if (databaseLogin == null) {
         response.setError(session.newError(ERROR_MAP, OpDbConfigurationWizardError.DATABASE_LOGIN_MISSING));
         return response;
      }

      //password is not a mandatory field
      String databasePassword = (String) parameters.get("database_password");

      int dbType = OpConfigurationValuesHandler.DATABASE_TYPES_MAP.get(databaseType);

      int errorCode = testConnectionParameters(databaseDriver, databaseURL, databaseLogin, databasePassword, dbType);
      if (errorCode != OpConnectionManager.SUCCESS) {
         Map<String, String> param = new HashMap<String, String>();
         Pattern p = Pattern.compile("^jdbc:(.*):/.*$");
         Matcher m = p.matcher(databaseURL);
         if (m.matches()) {
            param.put("Protocol", m.group(1));
         }
         response.setError(session.newError(ERROR_MAP, errorCode, param));

         return response;
      }

      //the configuration file name
      String configurationFileName = OpEnvironmentManager.getOnePointHome() + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME;
      writeConfigurationFile(configurationFileName, null, databaseType, databaseDriver, databaseURL, databaseLogin, databasePassword);

      OpInitializer initializer = OpInitializerFactory.getInstance().getInitializer();
      Map<String, String> initParams = initializer.init(OpEnvironmentManager.getProductCode());
      response.setArgument(OpProjectConstants.INIT_PARAMS, initParams);

      if (Byte.parseByte(initParams.get(OpProjectConstants.RUN_LEVEL)) == OpProjectConstants.SUCCESS_RUN_LEVEL) {
         //<FIXME author="Horia Chiorean" description="Fix this for multi-site">
         //set the source for the current session
         session.init(OpSource.DEFAULT_SOURCE_NAME);
         //<FIXME>
      }
      else {
         return response;
      }

      //check the load demodata checkbox
      Boolean loadDemodata = (Boolean) parameters.get("load_demodata");
      if (loadDemodata != null && loadDemodata) {
         String demodataFilePath = OpEnvironmentManager.getOnePointHome() + "/" + DEMODATATA_FILE_PATH;
         File demodataFile = new File(demodataFilePath);
         if (!demodataFile.exists()) {
            response.setError(session.newError(ERROR_MAP, OpDbConfigurationWizardError.INVALID_DEMODATA_FILE));
            return response;
         }
         else {
            try {
               initializer.restoreSchemaFromFile(demodataFile.getCanonicalPath(), session);
            }
            catch (Exception e) {
               logger.error("Cannot restore repository because:" + e.getMessage(), e);
               response.setError(session.newError(ERROR_MAP, OpDbConfigurationWizardError.RESTORE_ERROR));
               return response;
            }
         }
      }

      //restore the locale to the system locale (issue OPP-19)
      session.resetLocaleToSystemDefault();

      return response;
   }

   /**
    * Checks whether the given db connection parameters are ok to establish a db connection.
    *
    * @param databaseDriver   a <code>String</code> representing the database driver class.
    * @param databaseURL      a <code>String</code> representing the db connection url.
    * @param databaseLogin    a <code>String</code> representing the user name of the db connection.
    * @param databasePassword a <code>String</code> representing the db password.
    * @param dbType           an <code>int</code> representing the db type.
    * @return an <code>int</code> representing an error code or 0, representing no error.
    */
   private int testConnectionParameters(String databaseDriver, String databaseURL, String databaseLogin, String databasePassword, int dbType) {
      int testResult = OpConnectionManager.testConnection(databaseDriver, databaseURL, databaseLogin, databasePassword, dbType);
      switch (testResult) {
         case OpConnectionManager.GENERAL_CONNECTION_EXCEPTION: {
            return OpDbConfigurationWizardError.GENERAL_CONNECTION_ERROR;
         }
         case OpConnectionManager.INVALID_CONNECTION_STRING_EXCEPTION: {
            return OpDbConfigurationWizardError.INVALID_CONNECTION_STRING;
         }
         case OpConnectionManager.INVALID_CREDENTIALS_EXCEPTION: {
            return OpDbConfigurationWizardError.INVALID_CREDENTIALS;
         }
         case OpConnectionManager.MISSINING_DRIVER_EXCEPTION: {
            return OpDbConfigurationWizardError.JDBC_DRIVER_ERROR;
         }
         case OpConnectionManager.INVALID_MYSQL_ENGINE: {
            return OpDbConfigurationWizardError.INVALID_MYSQL_ENGINE;
         }
      }
      return testResult;
   }

   /**
    * Writes the configuration file for the application, based on the information from the configuration wizard.
    *
    * @param configurationFileName a <code>String</code> representing the name of the application configuration file.
    * @param dataBaseConfigName    a <code>String</code> the name of the db config
    * @param databaseType          a <code>String</code> representing the db type.
    * @param databaseDriver        a <code>String</code> representing the path to the db driver.
    * @param databaseURL           a <code>String</code> representing the db connection string.
    * @param databaseLogin         a <code>String</code> representing the user name in the db config.
    * @param databasePassword      a <code>String</code> representing the user password in the db.
    */
   private void writeConfigurationFile(String configurationFileName, String dataBaseConfigName, String databaseType, String databaseDriver, String databaseURL, String databaseLogin, String databasePassword) {
      try {
         DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
         documentFactory.setValidating(false);
         DocumentBuilder builder = documentFactory.newDocumentBuilder();
         //the document
         Document document;
         //the root element
         Node rootElement;

         // in case that no name was provided for DB configuration use the default one.
         dataBaseConfigName = dataBaseConfigName != null ? dataBaseConfigName : OpDatabaseConfiguration.DEFAULT_DB_CONFIGURATION_NAME;

         File configurationFile = new File(configurationFileName);
         if (configurationFile.exists()) {
            try {
               document = builder.parse(configurationFile);
               //we assume that the root element <configuration> exists
               rootElement = document.getDocumentElement();
            }
            catch (SAXParseException e) {
               logger.error("Configuration file is not parseable...");
               //create the document
               document = builder.newDocument();
               rootElement = document.createElement(OpConfigurationValuesHandler.CONFIGURATION);
               document.appendChild(rootElement);
            }
         }
         else {
            //create the document
            document = builder.newDocument();
            rootElement = document.createElement(OpConfigurationValuesHandler.CONFIGURATION);
            document.appendChild(rootElement);
         }

         // Now try to identify/create node for the specified database configuration.
         Element dataBaseConfig = null;
         NodeList children = rootElement.getChildNodes();
         for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            NamedNodeMap attributes = child.getAttributes();
            Node attr = attributes.getNamedItem(OpConfigurationValuesHandler.NAME_ATTRIBUTE);
            String attributeValue = attr.getNodeValue();

            if (OpConfigurationValuesHandler.DATABASE.equals(child.getNodeName()) && dataBaseConfigName.equals(attributeValue)) {
               dataBaseConfig = (Element) child; // found correct  
            }
         }
         if (dataBaseConfig == null) {
            // database configuration not found; creates a new one.
            dataBaseConfig = document.createElement(OpConfigurationValuesHandler.DATABASE);
            dataBaseConfig.setAttribute(OpConfigurationValuesHandler.NAME_ATTRIBUTE, dataBaseConfigName);

            rootElement.appendChild(dataBaseConfig);
         }

         //create the <database-type> node
         Node dbTypeElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_TYPE, databaseType);
         appendOrReplaceElement(dbTypeElement, dataBaseConfig, document);

         //create the <database-driver> node
         Node dbDriverElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_DRIVER, databaseDriver);
         appendOrReplaceElement(dbDriverElement, dataBaseConfig, document);

         //create the <database-url> node
         Node dbURLElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_URL, databaseURL);
         appendOrReplaceElement(dbURLElement, dataBaseConfig, document);

         //create the <database-login> node
         Node dbLoginElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_LOGIN, databaseLogin);
         appendOrReplaceElement(dbLoginElement, dataBaseConfig, document);

         //create the <database-password> node
         Node dbPasswordElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_PASSWORD, databasePassword);
         appendOrReplaceElement(dbPasswordElement, dataBaseConfig, document);

         //write result to the configuration file
         OpConfigurationLoader.writeConfigurationFile(document, configurationFileName);
      }
      catch (Exception e) {
         logger.error("Error occured while writing configuration file", e);
      }
   }

   /**
    * Creates a tag element for the given <code>document</code>.
    *
    * @param document <code>Document</code> factory to create elements
    * @param name     <code>String<code> the name of the tag element
    * @param value    <code>String<code> the value of the tag element
    * @return <code>Element<code>
    */
   private Node createTagElement(Document document, String name, String value) {
      Node nodeElement = document.createElement(name);
      value = (value != null) ? value : "";
      Node text = document.createTextNode(value);
      nodeElement.appendChild(text);
      return nodeElement;
   }

   /**
    * Appends <code>childElement</code> to <code>parentElement</code> or replaces it if already exists.
    *
    * @param childElement  child <code>Element</code> which will be appended or replaced (if already exists)
    * @param parentElement parent <code>Element</code> node of the <code>childElement</code>
    * @param document      <code>Document</code> where to perform the operation
    */
   private void appendOrReplaceElement(Node childElement, Node parentElement, Document document) {
      NodeList list = document.getElementsByTagName(childElement.getNodeName());
      if (list.getLength() > 0) {
         parentElement.replaceChild(childElement, list.item(0));
      }
      else {
         parentElement.appendChild(childElement);
      }
   }
}
