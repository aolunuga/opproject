/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

public class OpConfigurationLoader extends XLoader {

   // Does not use resource loading, because configuration is changed by user

   public final static String CONFIGURATION_FILE_NAME = "configuration.oxc.xml";

   private final static XSchema CONFIGURATION_SCHEMA = new OpConfigurationSchema();

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpConfigurationLoader.class);

   public OpConfigurationLoader() {
      super(new XDocumentHandler(CONFIGURATION_SCHEMA));
      setUseResourceLoader(false);
   }

   public OpConfiguration loadConfiguration(String filename) {
      OpConfiguration configuration =  (OpConfiguration) (loadObject(filename, null));
      if (configuration != null) {
         if (checkDatabaseConfiguration(configuration)){
            return null; // database should be configured using db configuration wizard
         }
         try {
            InputStream is = new FileInputStream(filename);
            checkDbPasswordEncrytion(configuration.getDatabaseConfiguration(), is, filename);
         }
         catch (FileNotFoundException e) {
            logger.error("Cannot check for db password encryption", e);
         }
      }
      return configuration;
   }

   /**
    * Checks whether the database was configured or it should be configured by using the configuration wizard.
    * @param configuration a <code>OpConfiguration</code> object.
    * @return <code>boolean</code> flag indicating that the database is properly configured or not
    */
   private boolean checkDatabaseConfiguration(OpConfiguration configuration) {
      OpConfiguration.DatabaseConfiguration dbConfig = configuration.getDatabaseConfiguration();
      return (dbConfig.getDatabaseType() == -1) || (dbConfig.getDatabaseDriver() == null) || (dbConfig.getDatabaseLogin() == null)
           || (dbConfig.getDatabasePassword() == null) || (dbConfig.getDatabaseUrl() == null);
   }

   /**
    * Checks where the db password needs encryption or not. If it needs encryption, it will encrypt it using a base 64 alg.
    * @param dbConfig  a <code>OpConfiguration.DatabaseConfiguration</code> object representing the db configuration.
    * @param is a <code>InputStream</code> to the configuration file.
    * @param  fileName a <code>String</code> file where the resulted configuration file will be written (if necessary)
    */
   private void checkDbPasswordEncrytion(OpConfiguration.DatabaseConfiguration dbConfig, InputStream is, String fileName) {
      if (fileName != null && dbConfig.needsPasswordEncryption()) {
         try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            documentFactory.setValidating(false);
            DocumentBuilder builder = documentFactory.newDocumentBuilder();
            Document configurationDocument = builder.parse(is);
            NodeList list = configurationDocument.getElementsByTagName(OpConfigurationValuesHandler.DATABASE_PASSWORD);
            if (list.getLength() > 0) {
               Element passwordElement = (Element) list.item(0);
               NodeList values = passwordElement.getChildNodes();
               for (int i = 0; i < values.getLength(); i++) {
                  passwordElement.removeChild(values.item(i));
               }
               String encryptedPassword = onepoint.project.configuration.OpConfiguration.getEncryptedDbPassword(dbConfig.getDatabasePassword());
               passwordElement.setAttribute(OpConfigurationValuesHandler.ENCRYPTED_ATTRIBUTE, "true");
               Text value = configurationDocument.createTextNode(encryptedPassword);
               passwordElement.appendChild(value);

               //write the result back to the file
               Transformer t = TransformerFactory.newInstance().newTransformer();
               t.transform(new DOMSource(configurationDocument), new StreamResult(new FileOutputStream(fileName)));
            }
         }
         catch (Exception e) {
            logger.error("Cannot encrypt db password in configuration", e);
         }
      }
   }

}