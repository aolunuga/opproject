/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 *
 */
package onepoint.project.configuration;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.configuration.generated.OpConfig;
import onepoint.project.util.OpEnvironmentManager;

import javax.xml.bind.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

/**
 * @author dfreis
 *         New configuration handler based on jaxb. Capable of validating and parsing an Onepoint configuration file.
 */

public class OpNewConfigurationHandler {

   /**
    * the name of the Onepoint base configuration file.
    */
   public final static String CONFIGURATION_FILE_NAME = "configuration.oxc.xml";

   /**
    * the package containing the jaxb auto generated classes
    */
   private final static String JAXB_CLASS_PACKAGE = "onepoint.project.configuration.generated";

   /**
    * the singleton
    */
   private static OpNewConfigurationHandler instance = null;

   /**
    * mutex for singleton
    */
   private final static Object MUTEX = new Object();

   /**
    * map holding all previously parsed config files.
    */
   private HashMap<String, Object> map;

   /**
    * the logger
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpNewConfigurationHandler.class);

   /**
    * private singleton constructor
    */
   private OpNewConfigurationHandler() {
      map = new HashMap<String, Object>();
   }

   /**
    * Gets a new singleton instance
    *
    * @return the OpNewConfigurationHandler instance
    */
   public final static OpNewConfigurationHandler getInstance() {
      if (instance == null) {
         synchronized (MUTEX) {
            if (instance == null) {
               instance = new OpNewConfigurationHandler();
            }
         }
      }
      return instance;
   }

   /**
    * Gets the Onepoint configuration base class.
    *
    * @return a configuration object representing the base class for the Onepoint configuration.
    */
   public final OpConfig getOpConfiguration() {
      try {
         return (OpConfig) getConfiguration(new File(OpEnvironmentManager.getOnePointHome(), CONFIGURATION_FILE_NAME));
      }
      catch (FileNotFoundException exc) {
         logger.error("Could not load LDAP configuration file at: " + new File(
              OpEnvironmentManager.getOnePointHome(), OpConfigurationLoader.CONFIGURATION_FILE_NAME).getAbsolutePath());
         return null;
      }
      catch (JAXBException exc) {
         logger.error("Could not load LDAP configuration file at: " + new File(
              OpEnvironmentManager.getOnePointHome(), OpConfigurationLoader.CONFIGURATION_FILE_NAME).getAbsolutePath() +
              " Error was: " + exc.getMessage());
         return null;
      }
   }

   /**
    * Gets a configuration base class for the given {@link configFileName}.
    *
    * @param configFileName the name (and path) of the configuration file
    * @return a configuration object representing the given {@link configFileName}
    * @throws FileNotFoundException if no file for the given {@link configFileName} was found.
    * @throws JAXBException         in case of a jaxb error.
    */
   public final Object getConfiguration(String configFileName)
        throws FileNotFoundException, JAXBException {
      return getConfiguration(new File(configFileName));
   }

   /**
    * Gets a configuration base class for the given {@link configFile}.
    *
    * @param configFile the file representing the configuration file
    * @return a configuration object representing the given {@link configFile}
    * @throws FileNotFoundException if no file for the given {@link configFile} was found.
    * @throws JAXBException         in case of a jaxb error.
    */
   public final Object getConfiguration(File configFile)
        throws FileNotFoundException, JAXBException {
      synchronized (map) {
         Object obj = map.get(configFile.getAbsolutePath());
         if (obj == null) {
            obj = readConfiguration(configFile);
            map.put(configFile.getAbsolutePath(), obj);
         }
         return obj;
      }
   }

   /**
    * reads a configuration file.
    *
    * @param configFile the config file to read
    * @return a class representing the config file.
    * @throws JAXBException         in case of an JAXB error.
    * @throws FileNotFoundException if no file was found.
    */
   private Object readConfiguration(File configFile)
        throws JAXBException, FileNotFoundException {
      JAXBContext jc = JAXBContext.newInstance(JAXB_CLASS_PACKAGE);
      Unmarshaller unmarshaller = jc.createUnmarshaller();
      unmarshaller.setEventHandler(new ValidationEventHandler() {
         // allow unmarshalling to continue even if there are errors
         public boolean handleEvent(ValidationEvent ve) {
            // ignore warnings
            if (ve.getSeverity() != ValidationEvent.WARNING) {
               ValidationEventLocator vel = ve.getLocator();
               logger.fatal("xmlfile not valid: Line:Col[" + vel.getLineNumber() +
                    ":" + vel.getColumnNumber() +
                    "]:" + ve.getMessage());
               return false;
            }
            return true;
         }
      });
      FileInputStream is = new FileInputStream(configFile);

      JAXBElement poElement = (JAXBElement) unmarshaller.unmarshal(is);
      return poElement.getValue();
   }
}
