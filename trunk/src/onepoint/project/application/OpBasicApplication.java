/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.application;

import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.XExitHandler;
import onepoint.express.XReflectionProxy;
import onepoint.express.application.XExpressApplication;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpSourceManager;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.components.OpWorkProxy;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpHashProvider;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XResourceCache;
import onepoint.service.XMessage;
import onepoint.util.XCalendar;

public class OpBasicApplication extends XExpressApplication implements XExitHandler {


   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpBasicApplication.class);
   private Rectangle origBounds = null;
   private static final int ERROR_WIDTH = 400;
   private static final int ERROR_HEIGHT = 100;

   private static final String JRE_WARNING = "Please note that you are running Onepoint Project on Java 1.5 Update 6 or " +
        "a previous version.\n For maximum performance and stability we strongly recommend upgrading to a newer version of Java.";


   static {
      // Register UI-scripting proxies
      // XComponent.registerProxy(new OpProjectComponentProxy());
      Class[] classes = {OpGanttValidator.class, OpProjectComponent.class,
           OpHashProvider.class, Set.class, Map.class, List.class, Iterator.class,
           java.io.File.class, TreeMap.class, OpProjectCalendar.class };
      XComponent.registerProxy(new XReflectionProxy(classes));
      XComponent.registerProxy(new OpWorkProxy());
      XCalendar.register(new OpProjectCalendar());
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
      if (arguments.length >= 1) {
         initializer.setConfigurationFileName(arguments[0]);
      }
      Map<String, Object> initParams = initializer.init(this.getProductCode());

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
   protected void postStart(Map<String, Object> initParams) {
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
         if (origBounds == null) {
            origBounds  = new Rectangle(mainFrame.getBounds());
         }
         Map startFormParams = (Map)parameters.get(OpProjectConstants.START_FORM_PARAMETERS);
         mainFrame.setResizable(false);
         int error_width = ERROR_WIDTH;
         int error_height = ERROR_HEIGHT;
         if (startFormParams != null) {
            Integer widthInt = (Integer)startFormParams.get(OpProjectConstants.WIDTH);
            if (widthInt != null) {
               error_width = widthInt.intValue();
            }
            Integer heightInt = (Integer)startFormParams.get(OpProjectConstants.HEIGHT);
            if (heightInt != null) {
               error_height = heightInt.intValue();
            }
         }
         int centerX = (origBounds.x + origBounds.width - error_width) / 2;
         int centerY = (origBounds.y + origBounds.height - error_height) / 2;
         mainFrame.setBounds(centerX, centerY, error_width, error_height);
         HashMap<String, String> formParams = new HashMap<String, String>(parameters);
         if (runLevel == OpProjectConstants.LOCK_FILE_EXISTS_RUN_LEVEL) {
            getDisplay().showForm((String)parameters.get(OpProjectConstants.START_FORM), startFormParams);
         }
         else {
            getDisplay().showForm(OpProjectConstants.RUN_LEVEL_ERROR_FORM, formParams);
         }
      }
      else {
         XMessage request = new XMessage();
         request.setAction("UserService.signOn");
         request.setArgument("login", OpUser.ADMINISTRATOR_NAME);
         request.setArgument("password", OpUser.BLANK_PASSWORD);
         request.setVariable(OpProjectConstants.CLIENT_TIMEZONE, OpProjectCalendar.CLIENT_TIMEZONE);
         XMessage response = this.getClient().invokeMethod(request);
         if (response.getVariables() != null) {
            OpProjectCalendar calendar = (OpProjectCalendar) response.getVariables().get(OpProjectConstants.CALENDAR);
            XDisplay.getDefaultDisplay().setCalendar(calendar);
         }
         if (origBounds != null) {
            int centerX = (origBounds.x + origBounds.width) / 2;
            int centerY = (origBounds.y + origBounds.height) / 2;
            Frame mainFrame = this.getDisplay().getViewer().getFrame();
            mainFrame.setResizable(true);
            mainFrame.setBounds(centerX, centerY, origBounds.width, origBounds.height);
         }
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

   /**
    * Checks the current JRE and displays a warning dialog if the version is less or equal to 1.5.0_06
    */
   public static void checkJREVersion() {
      String version = System.getProperty("java.version");

      // the version string should be in the following format n.n.n(_nn)
      String[] versionElements = version.split("\\.");
      if (versionElements.length == 3) {
         String updateVersion = "00";
         if (versionElements[2].indexOf("_") > -1) {
            String[] updateElements = version.split("_");
            updateVersion = updateElements[1];
            versionElements[2] = versionElements[2].substring(0, versionElements[2].indexOf("_"));
         }

         if (Integer.parseInt(versionElements[0]) <= 1 && (Integer.parseInt(versionElements[1]) < 5 ||
              (Integer.parseInt(versionElements[1]) == 5 && Integer.parseInt(versionElements[2]) == 0)) &&
              Integer.parseInt(updateVersion) <= 6) {
            JOptionPane.showMessageDialog(null, JRE_WARNING);
         }
      }
   }
}
