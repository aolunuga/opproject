/**
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.configuration;

/**
 * This exception was introduced to notify cases when loaded database configuration is not correct.
 *
 * @author calin.pavel
 */
public class OpInvalidDataBaseConfigurationException extends Exception {
   private String dataBaseConfigName;


   /**
    * Creates a new instance.
    */
   public OpInvalidDataBaseConfigurationException(String dataBaseConfigName) {
      super("Configuration for database: " + dataBaseConfigName + " is invalid");
      this.dataBaseConfigName = dataBaseConfigName;
   }


   /**
    * Returns the name of the invalid database configuration.
    *
    * @return invalid db configuration
    */
   public String getDataBaseConfigName() {
      return dataBaseConfigName;
   }
}
