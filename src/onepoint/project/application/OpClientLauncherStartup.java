/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.application;

import onepoint.express.XComponent;
import onepoint.express.application.XClientLauncherApplication;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpClientLauncherConfiguration;
import onepoint.project.configuration.OpClientLauncherLoader;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;

import java.awt.*;
import java.net.URL;
import java.util.HashMap;

/**
 * Performs start up for remote client viewer <code>XClientLauncherApplication</code>.
 *
 * @author ovidiu.lupas
 * @see XClientLauncherApplication
 */
public class OpClientLauncherStartup {
   /*logger for this class */
   private static final XLog logger = XLogFactory.getLogger(XClientLauncherApplication.class);


   /**
    * This is not to be instantiated
    */
   private OpClientLauncherStartup() {
   }

   public static void main(String[] args) {
      logger.info("Launcher viewer initialization started...");
      // get ONEPOINT_HOME environment variable
      String projectPath = OpEnvironmentManager.getOnePointHome();
      if (projectPath == null) {
         logger.fatal("Environment variable ONEPOINT_HOME is not set !");
         System.exit(1);
      }
      // load remote configuration file
      OpClientLauncherLoader loader = new OpClientLauncherLoader();
      OpClientLauncherConfiguration configuration = loader.loadConfiguration(projectPath + "/" + OpClientLauncherConfiguration.CONFIGURATION_FILE);

      if (configuration == null) {
         logger.fatal("Launcher viewer configuration was not found !");
         System.exit(1);
      }

      logger.info("Launcher viewer configuration loaded...");
      // create startup application
      XClientLauncherApplication application = createStartupApplication();
      // Register UI-scripting proxies
      XComponent.registerProxy(new OpProjectComponentProxy());

      application.getServer().setSessionClass(OpProjectSession.class);

      // Start must be called *after* overriding session class
      application.start(configuration.getHost(), configuration.getPort(), configuration.getPath(), configuration.getSecure(),
           OpProjectConstants.DEFAULT_START_FORM);

      // Request for run level
      XMessage request = new XMessage();
      request.setAction(OpProjectConstants.GET_RUN_LEVEL_ACTION);
      logger.info("Request for the remote application run level...");
      XMessage response = application.getClient().invokeMethod(request);
      String runLevel = (String) response.getArgument(OpProjectConstants.RUN_LEVEL);
      logger.info("Run level is:" + runLevel);
      // Show GUI
      application.setVisible(true);
      if (Byte.parseByte(runLevel) == OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL.byteValue()) {
         application.getDisplay().showForm(OpProjectConstants.CONFIGURATION_WIZARD_FORM);
      }
      else {
         // the params used for loading the login.oxf.xml form
         HashMap params = new HashMap(1);
         params.put(OpProjectConstants.RUN_LEVEL, runLevel);
         application.getDisplay().showForm(OpProjectConstants.DEFAULT_START_FORM, params);
      }
   }

   /**
    * Creates the start-up remote client application viewer
    *
    * @return <code>XClientLauncherApplication</code>
    */
   private static XClientLauncherApplication createStartupApplication() {
      // Startup application
      XClientLauncherApplication application = new XClientLauncherApplication("OnePoint Project", 1024, 720);
      // set the application-icon
      URL imgURL = Thread.currentThread().getContextClassLoader().getResource("onepoint/project/application/opp_icon16.png");
      if (imgURL != null) {
         Image icon = Toolkit.getDefaultToolkit().createImage(imgURL);
         MediaTracker tracker = new MediaTracker(application);
         tracker.addImage(icon, 0);
         try {
            tracker.waitForID(0);
         }
         catch (InterruptedException e) {
         }
         application.setIconImage(icon);
      }
      return application;
   }

}
