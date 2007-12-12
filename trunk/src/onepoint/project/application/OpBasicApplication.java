/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.application;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.XExitHandler;
import onepoint.express.application.XExpressApplication;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpSourceManager;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.components.OpWorkProxy;
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

public class OpBasicApplication extends XExpressApplication implements XExitHandler {


   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpBasicApplication.class);
   private static final int ERROR_WIDTH = 400;
   private static final int ERROR_HEIGHT = 100;


   static {
      // Register UI-scripting proxies
      XComponent.registerProxy(new OpProjectComponentProxy());
      XComponent.registerProxy(new OpWorkProxy());
   }

   /**
    * This is the main application class.
    *
    * @param title The title of the application
    */
   protected OpBasicApplication(String title) {

      super(title, 1024, 720);

      String projectHome = OpEnvironmentManager.getOnePointHome();
      //if not found among OS environment variables, set it to the working dir.
      if (projectHome == null) {
         OpEnvironmentManager.setOnePointHome(new File("").getAbsolutePath());
      }

      // set the application-icon
      URL imgURL = Thread.currentThread().getContextClassLoader().
           getResource("onepoint/project/application/opp_icon16.png");

      if (imgURL != null) {
         Image icon = Toolkit.getDefaultToolkit().createImage(imgURL);
         MediaTracker tracker = new MediaTracker(this);
         tracker.addImage(icon, 0);
         try {
            tracker.waitForID(0);
         }
         catch (InterruptedException e) {
            logger.error(e);
         }
         setIconImage(icon);
      }

   }

   public static void main(String[] arguments) {
      OpBasicApplication application = new OpBasicApplication("Onepoint Project Basic Edition");
      application.start(arguments);
   }

   protected void start(String[] arguments) {

      //perform initialization
      OpInitializer initializer = getInitializer();
      Map<String, String> initParams = initializer.init(this.getProductCode());

      //set up the resource cache max size
      OpConfiguration config = initializer.getConfiguration();
      String cacheSize = config != null ? config.getCacheConfiguration().getCacheSize() : null;
      if (cacheSize != null) {
         int resourceCacheSize = Integer.valueOf(cacheSize);
         this.getDisplay().setResourceCacheSize(resourceCacheSize);
         XResourceCache.setCacheSize(resourceCacheSize);
      }

      this.getServer().setSessionClass(OpProjectSession.class);

      //Start must be called *after* overriding session class
      super.start();

      //we want do do some cleanup...
      this.registerExitHandler(this);

      postStart(initParams);

      showStartForm(initParams);
   }

   /**
    * Advanced check, method is overriden.
    *
    * @param initParams - a <code>Map</code> of startup parameters.
    */
   protected void postStart(Map<String, String> initParams) {
   }

   protected OpInitializer getInitializer() {
      OpInitializerFactory factory = OpInitializerFactory.getInstance();
      factory.setInitializer(OpInitializer.class);
      return factory.getInitializer();
   }

   /**
    * @param parameters
    */
   public void showStartForm(Map parameters) {
      // Show GUI
      setVisible(true);
      byte runLevel = Byte.valueOf((String) parameters.get(OpProjectConstants.RUN_LEVEL));
      if (runLevel == OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL) {
         getDisplay().showForm(OpProjectConstants.STANDALONE_CONFIGURATION_WIZARD_FORM, new HashMap<String, String>(parameters));
      }
      else if (runLevel != OpProjectConstants.SUCCESS_RUN_LEVEL) {
         Frame mainFrame = this.getDisplay().getViewer().getFrame();
         mainFrame.setResizable(false);
         int centerX = (mainFrame.getBounds().x + mainFrame.getBounds().width - ERROR_WIDTH) / 2;
         int centerY = (mainFrame.getBounds().y + mainFrame.getBounds().height - ERROR_HEIGHT) / 2;
         mainFrame.setBounds(centerX, centerY, ERROR_WIDTH, ERROR_HEIGHT);
         HashMap<String, String> formParams = new HashMap<String, String>(parameters);
         getDisplay().showForm(OpProjectConstants.RUN_LEVEL_ERROR_FORM, formParams);
      }
      else {
         XMessage request = new XMessage();
         request.setAction("UserService.signOn");
         request.setArgument("login", OpUser.ADMINISTRATOR_NAME);
         request.setArgument("password", OpUser.BLANK_PASSWORD);
         request.setVariable(OpProjectConstants.CLIENT_TIMEZONE, XCalendar.CLIENT_TIMEZONE);
         XMessage response = this.getClient().invokeMethod(request);
         XCalendar calendar = (XCalendar) response.getVariables().get(OpProjectConstants.CALENDAR);
         XDisplay.getDefaultDisplay().setCalendar(calendar);
         getDisplay().showForm(OpEnvironmentManager.getStartForm(), new HashMap<String, String>(parameters));
      }
   }

   /**
    * Gets this application's product code.
    *
    * @return a <code>String</code> representing a product code constant.
    */
   protected String getProductCode() {
      return OpProjectConstants.BASIC_EDITION_CODE;
   }

   public void processExitEvent() {
      this.getServer().stop();
      OpSourceManager.closeAllSources();
   }
}
