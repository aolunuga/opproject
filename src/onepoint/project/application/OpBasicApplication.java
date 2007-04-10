/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.application;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.application.XExpressApplication;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XResourceCache;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class OpBasicApplication {


   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpBasicApplication.class, true);
   private final String title;
   private static final int ERROR_WIDTH = 400;
   private static final int ERROR_HEIGHT = 100;


   /**
    * This is the main application class.
    *
    * @param title The title of the application
    */
   protected OpBasicApplication(String title) {
      this.title = title;
      String projectHome = OpEnvironmentManager.getOnePointHome();
      //if not found among OS environment variables, set it to the working dir.
      if (projectHome == null) {
         OpEnvironmentManager.setOnePointHome(new File("").getAbsolutePath());
      }
   }

   public static void main(String[] arguments) {
      OpBasicApplication application = new OpBasicApplication("Onepoint Project Basic Edition");
      application.start(arguments);
   }

   protected void start(String[] arguments) {

      // create startup application
      XExpressApplication application = createStartupApplication(title);

      // Register UI-scripting proxies
      XComponent.registerProxy(new OpProjectComponentProxy());

      //perform initialization
      Map initParams = OpInitializer.init(this.getProductCode());
      additionalInitialization();

      /*set up the resource cache max size */
      String cacheSize = OpInitializer.getConfiguration() != null ? OpInitializer.getConfiguration().getCacheConfiguration().getCacheSize()
           : null;
      if (cacheSize != null) {
         int resourceCacheSize = Integer.valueOf(cacheSize).intValue();
         application.getDisplay().setResourceCacheSize(resourceCacheSize);
         XResourceCache.setCacheSize(resourceCacheSize);
      }

      application.getServer().setSessionClass(OpProjectSession.class);

      //Start must be called *after* overriding session class
      application.start();

      // Show GUI
      application.setVisible(true);
      if (OpInitializer.getRunLevel() == OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL.byteValue()) {
         application.getDisplay().showForm(OpProjectConstants.STANDALONE_CONFIGURATION_WIZARD_FORM);
      }
      else if (OpInitializer.getRunLevel() != OpInitializer.getSuccessRunLevel()) {
         Frame mainFrame = application.getDisplay().getViewer().getFrame();
         mainFrame.setResizable(false);
         int centerX = (mainFrame.getBounds().x + mainFrame.getBounds().width - ERROR_WIDTH) / 2;
         int centerY = (mainFrame.getBounds().y + mainFrame.getBounds().height - ERROR_HEIGHT) / 2;
         mainFrame.setBounds(centerX, centerY, ERROR_WIDTH, ERROR_HEIGHT);
         String localeId = application.getSession().getLocale().getID();
         HashMap formParams = new HashMap(initParams);
         String errorText = OpInitializer.checkRunLevel(formParams, localeId, "main.levels");
         formParams.put("errorMessage", errorText);
         application.getDisplay().showForm("/forms/runLevel.oxf.xml", formParams);
      }
      else {
         XMessage request = new XMessage();
         request.setAction("UserService.signOn");
         request.setArgument("login", OpUser.ADMINISTRATOR_NAME);
         request.setArgument("password", OpUser.BLANK_PASSWORD);
         request.setVariable(OpProjectConstants.CLIENT_TIMEZONE, XCalendar.CLIENT_TIMEZONE);
         XMessage response = application.getSession().invokeMethod(request);
         XCalendar calendar = (XCalendar) response.getVariables().get(OpProjectConstants.CALENDAR);
         XDisplay.getDefaultDisplay().setCalendar(calendar);
         application.getDisplay().showForm("/forms/start.oxf.xml", new HashMap(initParams));
      }
   }

   protected void additionalInitialization() {
   }

   /**
    * Creates the start-up application
    *
    * @param title Application title
    * @return <code>XExpressApplication</code>
    */
   protected static XExpressApplication createStartupApplication(String title) {
      // Startup application
      XExpressApplication application = new XExpressApplication(title, 1024, 720);
      // set the application-icon
      URL imgURL = Thread.currentThread().getContextClassLoader().
           getResource("onepoint/project/application/opp_icon16.png");

      if (imgURL != null) {
         Image icon = Toolkit.getDefaultToolkit().createImage(imgURL);
         MediaTracker tracker = new MediaTracker(application);
         tracker.addImage(icon, 0);
         try {
            tracker.waitForID(0);
         }
         catch (InterruptedException e) {
            logger.error(e);
         }
         application.setIconImage(icon);
      }
      return application;
   }

   /**
    * Gets this application's product code.
    * @return a <code>String</code> representing a product code constant.
    */
   protected String getProductCode() {
      return OpProjectConstants.BASIC_EDITION_CODE;
   }
}
