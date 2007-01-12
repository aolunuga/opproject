/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.application;

import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.application.XExpressApplication;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XResourceCache;
import onepoint.service.XMessage;
import onepoint.service.server.XService;
import onepoint.service.server.XServiceManager;
import onepoint.util.XCalendar;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class OpBasicApplication {

   /**
    * Command line parameters.
    */
   public final static int DEFAULT_MODE = 0;
   public final static int SETUP_MODE = 1;
   public final static int EXPORT_MODE = 2;
   public final static int IMPORT_MODE = 3;
   public final static int BACKUP_MODE = 4;
   public final static int RESTORE_MODE = 5;

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpBasicApplication.class, true);
   private int mode;
   private final String title;
   private String project_home;
   private final int ERROR_WIDTH = 400;
   private final int ERROR_HEIGHT = 100;


   /**
    * This is the main application class.
    *
    * @param title The title of the application
    */
   protected OpBasicApplication(String title) {
      mode = DEFAULT_MODE;
      this.title = title;
      project_home = OpEnvironmentManager.getEnvironmentVariable(OpEnvironmentManager.ONEPOINT_HOME);
   }

   protected String getProjectHome() {
      return project_home;
   }

   public static void main(String[] arguments) {
      OpBasicApplication application = new OpBasicApplication("Onepoint Project Basic Edition");
      application.start(arguments);
   }

   protected void start(String[] arguments) {

      //parse application arguments
      String exchangeFileName = parseArguments(arguments);

      // create startup application
      XExpressApplication application = createStartupApplication(title);

      // Register UI-scripting proxies
      XComponent.registerProxy(new OpProjectComponentProxy());

      //perform initialization
      Map initParams = OpInitializer.init(project_home, false);
      additionalInitialization();

      /*set up the resource cache max size */
      String cacheSize = (String) initParams.remove(OpInitializer.RESOURCE_CACHE_SIZE);
      if (cacheSize != null) {
         int resourceCacheSize = Integer.valueOf(cacheSize).intValue();
         application.getDisplay().setResourceCacheSize(resourceCacheSize);
         XResourceCache.setCacheSize(resourceCacheSize);
      }

      application.getServer().setSessionClass(OpProjectSession.class);

      //Start must be called *after* overriding session class
      application.start();

      switch (mode) {
         case BACKUP_MODE: {
            OpBackupManager backupManager = OpBackupManager.getBackupManager();
            try {
               if (exchangeFileName == null) {
                  exchangeFileName = application.getDisplay().showFileDialog("Backup respository", Boolean.FALSE, null);
                  application.dispose();
               }
               backupManager.backupRepository((OpProjectSession) application.getSession(), exchangeFileName);
            }
            catch (IOException e) {
               logger.error("ERROR: Could not backup repository: " + e);
            }
            System.exit(0);
         }
         case RESTORE_MODE: {
            try {
               if (exchangeFileName == null) {
                  exchangeFileName = application.getDisplay().showFileDialog("Restore repository", Boolean.TRUE, null);
                  application.dispose();
               }
               OpInitializer.restoreSchemaFromFile(exchangeFileName, (OpProjectSession) application.getSession());
            }
            catch (Exception e) {
               logger.error("ERROR: Could not restore repository: " + e);
            }
            System.exit(0);
         }
         default: {
            // Show GUI
            application.setVisible(true);
            if (OpInitializer.getRunLevel() == OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL.byteValue()) {
               application.getDisplay().showForm(OpProjectConstants.STANDALONE_CONFIGURATION_WIZARD_FORM);
            }
            else if (OpInitializer.getRunLevel() != OpInitializer.getSuccessRunLevel()) {
               Frame mainFrame = application.getDisplay().getViewer().getFrame();
               mainFrame.setResizable(false);
               int centerX = (mainFrame.getBounds().x + mainFrame.getBounds().width - ERROR_WIDTH) / 2;
               int centerY= (mainFrame.getBounds().y + mainFrame.getBounds().height - ERROR_HEIGHT) / 2;
               mainFrame.setBounds(centerX, centerY, ERROR_WIDTH, ERROR_HEIGHT);
               String localeId = application.getSession().getLocale().getID();
               HashMap formParams = new HashMap(initParams);
               String errorText = OpInitializer.checkRunLevel(formParams, localeId, "main.levels");
               formParams.put("errorMessage", errorText);
               application.getDisplay().showForm("/forms/runLevel.oxf.xml", formParams);
            }
            else {
               XService service = XServiceManager.getService("UserService");
               XMessage request = new XMessage();
               request.setArgument("login", OpUser.ADMINISTRATOR_NAME);
               request.setArgument("password", OpUserService.BLANK_PASSWORD);
               XMessage response = service.invokeMethod(application.getSession(), "signOn", request);
               Map settings = (Map) response.getVariables().get(XCalendar.CALENDAR_SETTINGS);
               XDisplay.configureCalendar(settings);
               application.getDisplay().showForm("/forms/start.oxf.xml", new HashMap(initParams));
            }
         }
      }
   }

   protected void additionalInitialization() {
   }


   private String parseArguments(String[] arguments) {
      String exchangeFileName = null;
      if (arguments.length > 0) {
         if ((arguments.length == 1) && (arguments[0].equals("setup"))) {
            mode = SETUP_MODE;
         }
         else if (arguments[0].equals("backup")) {
            if (arguments.length == 2) {
               exchangeFileName = arguments[1];
            }
            mode = BACKUP_MODE;
         }
         else if (arguments[0].equals("restore")) {
            if (arguments.length == 2) {
               exchangeFileName = arguments[1];
            }
            mode = RESTORE_MODE;
         }
         else {
            String[] strings = this.getClass().getName().split("[.]");
            logger.fatal("USAGE: " + strings[strings.length - 1] + " [setup|backup <file-name>|restore <file-name>]");
            System.exit(1);
         }
      }
      return exchangeFileName;
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
}
