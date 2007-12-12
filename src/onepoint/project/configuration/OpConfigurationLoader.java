/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.util.XEnvironmentManager;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class OpConfigurationLoader extends XLoader {

   // Does not use resource loading, because configuration is changed by user

   public final static String CONFIGURATION_FILE_NAME = "configuration.oxc.xml";

   private final static XSchema CONFIGURATION_SCHEMA = new OpConfigurationSchema();

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getClientLogger(OpConfigurationLoader.class);

   public OpConfigurationLoader() {
      super(new XDocumentHandler(CONFIGURATION_SCHEMA));
      setUseResourceLoader(false);
   }

   /**
    * Load configuration settings from the given file.
    *
    * @param fileName file to load.
    * @return loaded configuration
    * @throws OpInvalidDataBaseConfigurationException
    *          In case that any of loaded database configurations is invalid.
    */
   public OpConfiguration loadConfiguration(String fileName)
        throws OpInvalidDataBaseConfigurationException {
      try {
         //check configuration file structure/passwords and update it if necessary
         updateConfigurationFile(fileName);

         OpConfiguration configuration = (OpConfiguration) (loadObject(null, fileName, null));
         if (configuration != null) {
            checkDatabaseConfigurations(configuration.getDatabaseConfigurations());
         }

         return configuration;
      }
      catch (FileNotFoundException e) {
         logger.error("Cannot load configuration file: " + fileName, e);
         throw new OpInvalidDataBaseConfigurationException(OpDatabaseConfiguration.DEFAULT_DB_CONFIGURATION_NAME);
      }
      catch (Exception e) {
         logger.error("OpConfigurationLoader.loadConfiguration ", e);
         String projectPath = OpEnvironmentManager.getOnePointHome();
         File oldConfigFile = new File(projectPath + XEnvironmentManager.PATH_SEPARATOR + OpConfigurationLoader.CONFIGURATION_FILE_NAME);
         File backupConfigFile = new File(projectPath + XEnvironmentManager.PATH_SEPARATOR + OpConfigurationLoader.CONFIGURATION_FILE_NAME+".backup");
         if (oldConfigFile.exists()) {
            oldConfigFile.renameTo(backupConfigFile);
         }

         throw new OpInvalidDataBaseConfigurationException(OpDatabaseConfiguration.DEFAULT_DB_CONFIGURATION_NAME);
      }
   }

   /**
    * Checks whether the databases were configured or it should be configured by using the configuration wizard.
    *
    * @param configurations a <code>Set</code> of <code>OpConfiguration.DatabaseConfiguration</code> isntances to check.
    * @throws OpInvalidDataBaseConfigurationException
    *          In case any of the provided configurations is invalid.
    */
   private void checkDatabaseConfigurations(Set<OpDatabaseConfiguration> configurations)
        throws OpInvalidDataBaseConfigurationException {
      for (OpDatabaseConfiguration dbConfig : configurations) {
         if (dbConfig.getDatabaseType() == -1 || dbConfig.getDatabaseDriver() == null
              || dbConfig.getDatabaseLogin() == null || dbConfig.getDatabasePassword() == null
              || dbConfig.getDatabaseUrl() == null) {
            throw new OpInvalidDataBaseConfigurationException(dbConfig.getName());
         }
      }
   }

   /**
    * Check if the configuration file has the new structure (with support for multiple databases) and if not change it.
    *
    * @param fileName configuration file to check.
    * @throws FileNotFoundException If provided file does not exist.
    * @throws OpInvalidDataBaseConfigurationException if there is an error in the configuration file.
    */
   private void updateConfigurationFile(String fileName)
        throws FileNotFoundException, OpInvalidDataBaseConfigurationException {
      if (fileName != null) {
         InputStream is = new FileInputStream(fileName);
         InputStreamReader inputStreamReader = new InputStreamReader(is);
         try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            documentFactory.setValidating(false);
            DocumentBuilder builder = documentFactory.newDocumentBuilder();
            Document configurationDocument = builder.parse(new InputSource(inputStreamReader));

            updateConfigurationFileStructure(configurationDocument);
            updateDbPasswordEncryption(configurationDocument);

            //write the result back to the file
            writeConfigurationFile(configurationDocument, fileName);
            inputStreamReader.close();
            is.close();
         }
         catch (Exception e) {

            try {
               is.close();
               inputStreamReader.close();
            }
            catch (IOException ex) {
               ex.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            
            logger.error("Cannot update configuration file.", e);
            throw new OpInvalidDataBaseConfigurationException(OpDatabaseConfiguration.DEFAULT_DB_CONFIGURATION_NAME);
         }
      }
   }

   /**
    * Writes configuration into file.
    *
    * @param document configuration DOM element.
    * @param fileName file name where to write configuration
    * @throws TransformerException  If configuration can not be transformed.
    * @throws FileNotFoundException If configuration file can not be found
    */
   public static void writeConfigurationFile(Document document, String fileName)
        throws TransformerException, FileNotFoundException {
      //write the result back to the file
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      try {
         transformerFactory.setAttribute("indent-number", 2);
      }
      catch (IllegalArgumentException exc) {
         // Java 1.6 Bug 6519088
      }
      Transformer t = transformerFactory.newTransformer();
      t.setOutputProperty(OutputKeys.METHOD, "xml");
      try {
         t.setOutputProperty(OutputKeys.INDENT, "yes");
      }
      catch (IllegalArgumentException exc) {
         // Java 1.6 Bug 6519088
      }
      OutputStreamWriter outputStreamWriter;
      try {
         outputStreamWriter = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");
         StreamResult result = new StreamResult(outputStreamWriter);

         // normalize document before writing
         document.normalizeDocument();
         t.transform(new DOMSource(document), result);
         outputStreamWriter.close();
      }
      catch (IOException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
   }

   /**
    * Check if the configuration file has the new structure (with support for multiple databases) and if not change it.
    *
    * @param configurationDocument configuration document to check.
    * @throws FileNotFoundException If provided file does not exist.
    */
   private void updateConfigurationFileStructure(Document configurationDocument)
        throws FileNotFoundException {

      NodeList list = configurationDocument.getElementsByTagName(OpConfigurationValuesHandler.DATABASE_URL);
      if (list.getLength() > 0) {
         Element urlElement = (Element) list.item(0);
         Node parentNode = urlElement.getParentNode();

         // check if database tags are defined as children of <configuration>
         if (OpConfigurationValuesHandler.CONFIGURATION.equals(parentNode.getNodeName())) {
            Element databaseElem = configurationDocument.createElement(OpConfigurationValuesHandler.DATABASE);
            databaseElem.setAttribute(OpConfigurationValuesHandler.NAME_ATTRIBUTE,
                 OpDatabaseConfiguration.DEFAULT_DB_CONFIGURATION_NAME);
            parentNode.appendChild(databaseElem);

            // Now move all database related tags from <configuration> to <database>
            List<String> databaseTags = Arrays.asList(
                 OpConfigurationValuesHandler.CONNECTION_POOL_MAXSIZE,
                 OpConfigurationValuesHandler.CONNECTION_POOL_MINSIZE,
                 OpConfigurationValuesHandler.DATABASE_DRIVER,
                 OpConfigurationValuesHandler.DATABASE_LOGIN,
                 OpConfigurationValuesHandler.DATABASE_PASSWORD,
                 OpConfigurationValuesHandler.DATABASE_PATH,
                 OpConfigurationValuesHandler.DATABASE_TYPE,
                 OpConfigurationValuesHandler.DATABASE_URL);

            NodeList children = parentNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
               Node child = children.item(i);

               if (child.getNodeType() == 3 && child.getNodeValue().trim().length() == 0) {
                  child.setNodeValue("");
               }
               // check if the current child is Database related.
               if (databaseTags.contains(child.getNodeName())) {
                  parentNode.removeChild(child);
                  databaseElem.appendChild(child);
               }
            }
         }
      }
   }

   /**
    * Checks where the db password needs encryption or not. If it needs encryption, it will encrypt it using a base 64 alg.
    *
    * @param configurationDocument to be processed
    * @throws FileNotFoundException If the provided file does not exists.
    */
   private void updateDbPasswordEncryption(Document configurationDocument)
        throws FileNotFoundException {

      // process all Database password elements
      NodeList list = configurationDocument.getElementsByTagName(OpConfigurationValuesHandler.DATABASE_PASSWORD);
      if (list != null && list.getLength() > 0) {
         for (int counter = 0; counter < list.getLength(); counter++) {
            Element passwordElement = (Element) list.item(counter);
            if (passwordElement.getParentNode().getNodeName().equals(OpConfigurationValuesHandler.DATABASE)) {
               // check if the password must be updated/encripted or not.
               boolean isPassEncrypted = Boolean.parseBoolean(passwordElement.getAttribute(OpConfigurationValuesHandler.ENCRYPTED_ATTRIBUTE));
               if (!isPassEncrypted) {
                  NodeList values = passwordElement.getChildNodes();
                  String password = "";
                  for (int i = 0; i < values.getLength(); i++) {
                     Node value = values.item(i);
                     password = value.getNodeValue();
                     passwordElement.removeChild(value);
                  }
                  String encryptedPassword = onepoint.project.configuration.OpConfiguration.getEncryptedDbPassword(password);
                  passwordElement.setAttribute(OpConfigurationValuesHandler.ENCRYPTED_ATTRIBUTE, Boolean.TRUE.toString());
                  Text value = configurationDocument.createTextNode(encryptedPassword);
                  passwordElement.appendChild(value);
               }
            }
         }
      }
   }

}