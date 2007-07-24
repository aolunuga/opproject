/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.xml.XContext;
import onepoint.xml.XDocumentHandler;
import onepoint.xml.XLoader;
import onepoint.xml.XSchema;

/**
 * Class that performs configuration loading for the client launcher application.
 *
 * @author ovidiu.lupas
 * @see OpClientLauncherConfiguration
 */
public class OpClientLauncherLoader extends XLoader {

   /* the client launcher schema */
   private final static XSchema CLIENT_LAUNCHER_CONFIG_SCHEMA = new OpClientLauncherConfigurationSchema();

   /**
    * Creates a configuration loader
    */
   public OpClientLauncherLoader() {
      super(new XDocumentHandler(CLIENT_LAUNCHER_CONFIG_SCHEMA));
      setUseResourceLoader(false);
   }

   /**
    * Loads configuration from the given <code>fileName</code>
    *
    * @param fileName <code>String</code> representing the configuration file path
    * @return <code>OpClientLauncherConfiguration</code>
    */
   public OpClientLauncherConfiguration loadConfiguration(String fileName) {
      return (OpClientLauncherConfiguration) (loadObject(fileName, new XContext()));
   }
}

