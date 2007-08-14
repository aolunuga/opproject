/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.xml.XSchema;

public class OpConfigurationSchema extends XSchema {

   public OpConfigurationSchema() {
      registerNodeHandler(OpConfigurationValuesHandler.CONFIGURATION, new OpConfigurationHandler());

      // Database related node handlers
      registerNodeHandler(OpConfigurationValuesHandler.DATABASE, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.DATABASE_TYPE, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.DATABASE_DRIVER, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.DATABASE_URL, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.DATABASE_LOGIN, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.DATABASE_PASSWORD, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.DATABASE_PATH, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.CONNECTION_POOL_MINSIZE, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.CONNECTION_POOL_MAXSIZE, new OpConfigurationValuesHandler());

      registerNodeHandler(OpConfigurationValuesHandler.BROWSER, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.SMTP_SERVER, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.LOG_FILE, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.LOG_LEVEL, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.CACHE_SIZE, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.JES_DEBUGGING, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.SECURE_SERVICE, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.RESOURCE_CACHE_SIZE, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.BACKUP_PATH, new OpConfigurationValuesHandler());
      registerNodeHandler(OpConfigurationValuesHandler.MAX_ATTACHMENT_SIZE, new OpConfigurationValuesHandler());
   }

}