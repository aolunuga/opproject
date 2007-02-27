/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.xml.XSchema;

/**
 * Schema class that handles client launcher configuration file
 *
 * @author ovidiu.lupas
 */
public class OpClientLauncherConfigurationSchema extends XSchema {

   public OpClientLauncherConfigurationSchema() {
      registerNodeHandler(OpClientLauncherHandler.REMOTE_CONFIGURATION, new OpClientLauncherHandler());
      registerNodeHandler(OpClientLauncherHandler.HOST, new OpClientLauncherHandler());
      registerNodeHandler(OpClientLauncherHandler.PORT, new OpClientLauncherHandler());
      registerNodeHandler(OpClientLauncherHandler.PATH, new OpClientLauncherHandler());
      registerNodeHandler(OpClientLauncherHandler.SECURE, new OpClientLauncherHandler());
   }

}
