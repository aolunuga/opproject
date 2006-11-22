/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpConnectionManager;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.configuration.OpConfigurationValuesHandler;
import onepoint.service.XMessage;
import onepoint.service.server.XService;
import onepoint.service.server.XSession;
import onepoint.util.XEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for the db configuration wizard.
 *
 * @author horia.chiorean
 */
public class OpConfigurationWizardService extends XService {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpConfigurationWizardService.class);

   /**
    * Request argument names.
    */
   private static final String PARAMETERS = "parameters";

   /**
    * Writes the db configuration file, with the db settings.
    *
    * @param s a <code>XSession</code> representing the server session.
    * @param request a <code>XMessage</code> representing the client request.
    * @return a <code>XMessage</code> representing the response.
    */
   public XMessage writeDatabaseConfigurationFile(XSession s, XMessage request) {

      OpProjectSession session = (OpProjectSession) s;

      HashMap parameters = (HashMap) (request.getArgument(PARAMETERS));
      //error map
      OpDbConfigurationWizardErrorMap errorMap = new OpDbConfigurationWizardErrorMap();

      String databaseType = (String) parameters.get("database_type");
      String databaseDriver = (String) onepoint.project.configuration.OpConfiguration.DATABASE_DRIVERS.get(databaseType);

      //response message
      XMessage response = new XMessage();

      //perform fields validation
      String databaseURL = (String) parameters.get("database_url");
      if (databaseURL == null) {
         response.setError(session.newError(errorMap, OpDbConfigurationWizardError.DATABASE_URL_MISSING));
         return response;
      }
      String databaseLogin = (String) parameters.get("database_login");
      if (databaseLogin == null) {
         response.setError(session.newError(errorMap, OpDbConfigurationWizardError.DATABASE_LOGIN_MISSING));
         return response;
      }

      //password is not a mandatory field
      String databasePassword = (String) parameters.get("database_password");

      int errorCode = testConnectionParameters(databaseDriver, databaseURL, databaseLogin, databasePassword);
      if (errorCode != OpConnectionManager.SUCCESS) {
         response.setError(session.newError(errorMap, errorCode));
         return response;
      }

      //the configuration file name
      String projectPath = XEnvironment.getVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME);
      String configurationFileName = projectPath + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME;

      writeConfigurationFile(configurationFileName, databaseType, databaseDriver, databaseURL, databaseLogin, databasePassword);

      //re-initialize application
      Map initParams = OpInitializer.init(projectPath);
      response.setArgument("initParams", initParams);
      return response;
   }

   /**
    * Checks whether the given db connection parameters are ok to establish a db connection.
    * @param databaseDriver a <code>String</code> representing the database driver class.
    * @param databaseURL a <code>String</code> representing the db connection url.
    * @param databaseLogin a <code>String</code> representing the user name of the db connection.
    * @param databasePassword a <code>String</code> representing the db password.
    * @return an <code>int</code> representing an error code or 0, representing no error.
    */
   private int testConnectionParameters(String databaseDriver, String databaseURL, String databaseLogin, String databasePassword) {
      int testResult = OpConnectionManager.testConnection(databaseDriver, databaseURL, databaseLogin, databasePassword);
      switch(testResult) {
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
      }
      return testResult;
   }

   /**
    * Writes the configuration file for the application, based on the information from the configuration wizard.
    *
    * @param configurationFileName a <code>String</code> representing the name of the application configuration file.
    * @param databaseType a <code>String</code> representing the db type.
    * @param databaseDriver a <code>String</code> representing the path to the db driver.
    * @param databaseURL a <code>String</code> representing the db connection string.
    * @param databaseLogin a <code>String</code> representing the user name in the db config.
    * @param databasePassword a <code>String</code> representing the user password in the db.
    */
   private void writeConfigurationFile(String configurationFileName, String databaseType, String databaseDriver, String databaseURL, String databaseLogin, String databasePassword) {
      try {
         DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
         documentFactory.setValidating(false);
         DocumentBuilder builder = documentFactory.newDocumentBuilder();
         //the document
         Document document;
         //the root element
         Node rootElement;

         File configurationFile = new File(configurationFileName);
         if (configurationFile.exists()) {
            document = builder.parse(configurationFile);
            //we assume that the root element <configuration> exists
            rootElement = document.getDocumentElement();
         }
         else {
            //create the document
            document = builder.newDocument();
            rootElement = document.createElement(OpConfigurationValuesHandler.CONFIGURATION);
            document.appendChild(rootElement);
         }

         //create the <database-type> node
         Node dbTypeElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_TYPE, databaseType);
         appendOrReplaceElement(dbTypeElement, rootElement, document);

         //create the <database-driver> node
         Node dbDriverElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_DRIVER, databaseDriver);
         appendOrReplaceElement(dbDriverElement, rootElement, document);

         //create the <database-url> node
         Node dbURLElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_URL, databaseURL);
         appendOrReplaceElement(dbURLElement, rootElement, document);

         //create the <database-login> node
         Node dbLoginElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_LOGIN, databaseLogin);
         appendOrReplaceElement(dbLoginElement, rootElement, document);

         //create the <database-password> node
         Node dbPasswordElement = createTagElement(document, OpConfigurationValuesHandler.DATABASE_PASSWORD, databasePassword);
         appendOrReplaceElement(dbPasswordElement, rootElement, document);

         //write result to the configuration file
         Transformer t = TransformerFactory.newInstance().newTransformer();
         t.setOutputProperty(OutputKeys.INDENT, "yes");
         t.transform(new DOMSource(document), new StreamResult(new FileOutputStream(configurationFileName)));

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
