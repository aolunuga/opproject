package onepoint.project.application;

import onepoint.express.XComponent;
import onepoint.express.application.XExpressApplication;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.project_planning.components.OpProjectComponentProxy;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XResourceCache;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class OpProjectApplication {

   private static final XLog logger = XLogFactory.getLogger(OpProjectApplication.class, true);

   // Parse command-line parameters
   public final static int DEFAULT_MODE = 0;
   public final static int SETUP_MODE = 1;
   public final static int EXPORT_MODE = 2;
   public final static int IMPORT_MODE = 3;
   public final static int BACKUP_MODE = 4;
   public final static int RESTORE_MODE = 5;


   public static void main(String[] arguments) {
      int mode = DEFAULT_MODE;

      String exchangeFileName = null;
      String project_home = OpEnvironmentManager.getEnvironmentVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME);
      if (arguments.length > 0) {
         if ((arguments.length == 1) && (arguments[0].equals("setup"))) {
            mode = SETUP_MODE;
         }
         else if (arguments.length == 2 && arguments[0].equals("export")) {
            exchangeFileName = arguments[1];
            mode = EXPORT_MODE;
         }
         else if (arguments.length == 2 && arguments[0].equals("import")) {
            exchangeFileName = arguments[1];
            mode = IMPORT_MODE;
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
            logger
                 .fatal("USAGE: OpProjectApplication [setup|backup <file-name>|restore <file-name>]");
            System.exit(1);
         }
      }
      // create startup application
      XExpressApplication application = createStartupApplication();

      // Register UI-scripting proxies
      XComponent.registerProxy(new OpProjectComponentProxy());

      //perform initialization
      Map initParams = OpInitializer.init(project_home);

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
         case EXPORT_MODE:
            // Export repository contents
            // TODO: Exchange export interface to exportData(OpProjectSession, OutputStream)
            // (Currect method signature using byte[] was only for testing purposes)
            /*
             * try { byte[] data = new OpProjectExporter().export((OpProjectSession) application.getSession());
             * FileOutputStream fileOutput = new FileOutputStream(exchangeFileName, false); fileOutput.write(data);
             * fileOutput.flush(); fileOutput.close(); } catch (IOException e) { System.err.println("ERROR: Could not
             * export repository: " + e); e.printStackTrace(); }
             */
            break;
         case IMPORT_MODE:
            // Import repository contents
            try {
               BufferedInputStream input = new BufferedInputStream(new FileInputStream(exchangeFileName));
               //new OpProjectImporter().importData((OpProjectSession) application.getSession(), input);
               input.close();
            }
            catch (IOException e) {
               System.err.println("ERROR: Could not import repository: " + e);
               e.printStackTrace();
            }
            break;
         case BACKUP_MODE:
         case RESTORE_MODE:
            OpBackupManager backupManager = OpBackupManager.getBackupManager();

            // Invoke backup/restore action
            if (mode == BACKUP_MODE) {
               try {
                  if (exchangeFileName == null) {
                     exchangeFileName = application.getDisplay().showFileDialog("Backup respository", Boolean.FALSE);
                     application.dispose();
                  }
                  backupManager.backupRepository((OpProjectSession) application.getSession(), exchangeFileName);
               }
               catch (IOException e) {
                  logger.error("ERROR: Could not backup repository: " + e);
               }
            }
            else if (mode == RESTORE_MODE) {
               //check for "empty" DB
               if (OpInitializer.isEmptyDB()) {
                  try {
                     if (exchangeFileName == null) {
                        exchangeFileName = application.getDisplay().showFileDialog("Restore repository", Boolean.TRUE);
                        application.dispose();
                     }
                     backupManager.restoreRepository((OpProjectSession) application.getSession(), exchangeFileName);
                  }
                  catch (IOException e) {
                     logger.error("ERROR: Could not restore repository: " + e);
                  }
               }
               else {
                  logger.error("ERROR: Restore needs an empty data base");
               }
            }
            System.exit(0);
            break;
         default:
            // Show GUI
            application.setVisible(true);
            if (OpInitializer.getRunLevel() == OpProjectConstants.CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL.byteValue()) {
               HashMap params = new HashMap();
               params.put("localApplication", Boolean.TRUE);
               application.getDisplay().showForm(OpProjectConstants.CONFIGURATION_FORM, params);
            }
            else {
               application.getDisplay().showForm(OpProjectConstants.DEFAULT_START_FORM, new HashMap(initParams));
            }
      }
   }

   /**
    * Creates the start-up application
    *
    * @return <code>XExpressApplication</code>
    */
   private static XExpressApplication createStartupApplication() {
      // Startup application
      XExpressApplication application = new XExpressApplication("Onepoint Project", 1024, 720);
      // set the application-icon
      URL imgURL = Thread.currentThread().getContextClassLoader().getResource(
           "onepoint/project/application/opp_icon16.png");
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
