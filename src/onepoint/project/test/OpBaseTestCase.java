/**
 * Copyright 2006 CodeBox GmbH. 
 * All Rights Reserved.
 *
 * This software is the proprietary information of CodeBox GmbH.
 * Use is subject to license terms.
 */
package onepoint.project.test;

import junit.framework.TestCase;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.modules.configuration_wizard.OpConfigurationWizardService;
import onepoint.project.modules.my_tasks.OpMyTasksService;
import onepoint.project.modules.preferences.OpPreferencesService;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.project_status.OpProjectStatusService;
import onepoint.project.modules.report.OpReportService;
import onepoint.project.modules.repository.OpRepositoryService;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationService;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.work.OpWorkService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XLocalServer;
import onepoint.service.server.XServer;
import onepoint.service.server.XServiceManager;

/**
 * This is the super class for all test classes used into OnePoint project.
 *
 * @author calin.pavel
 */
public class OpBaseTestCase extends TestCase {
   // This is the session that must be used into all test methods.
   protected OpProjectSession session;

   protected XServer server;

   protected String adminId;

   protected String rootId;

   // Name of the users service.
   private static final String USER_SERVICE_NAME = "UserService";

   // Name of the preferences service.
   private static final String PREFERENCES_SERVICE_NAME = "PreferencesService";

   // Name of the settings service.
   private static final String SETTINGS_SERVICE_NAME = "SettingsService";

   // Name of the project status service.
   private static final String PROJECT_STATUS_SERVICE_NAME = "ProjectStatusService";

   // Name of the project service.
   private static final String PROJECT_SERVICE_NAME = "ProjectService";

   // Name of the resource service.
   private static final String RESOURCE_SERVICE_NAME = "ResourceService";

   // Name of the repository service.
   private static final String REPOSITORY_SERVICE_NAME = "RepositoryService";

   // Name of the report service.
   private static final String REPORT_SERVICE_NAME = "ReportService";

   // Name of the configuration wizard service.
   private static final String CONFIG_WIZARD_SERVICE_NAME = "DBConfigurationWizard";

   // Name of the planning service.
   private static final String PLANNING_SERVICE_NAME = "PlanningService";

   // Name of the work service.
   private static final String WORK_SERVICE_NAME = "WorkService";

   // Name of the mytasks service.
   private static final String MY_TASKS_SERVICE_NAME = "MyTasksService";

   // Name of the resource utilization service.
   private static final String RESOURCE_UTILIZATION_SERVICE_NAME = "ResourceUtilizationService";

   // Dummy error code.
   private static final int DUMMY_ERROR_CODE = -49765;


   // Initialize application
   static {
      AppInitializer.init();
   }

   /**
    * Creates a new instance of test case.
    */
   public OpBaseTestCase() {
   }

   /**
    * Creates a new instance of test case.
    *
    * @param name test case name
    */
   public OpBaseTestCase(String name) {
      super(name);
   }

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      this.server = new XLocalServer();
      this.session = new OpProjectSession();
      this.session.setServer(this.server);

      // Authenticate user to be sure that he has access.
      logIn(OpUser.ADMINISTRATOR_NAME, OpUserService.BLANK_PASSWORD);

      OpBroker broker = session.newBroker();
      // administrator
      adminId = session.administrator(broker).locator();
      // root pool
      rootId = OpResourceService.findRootPool(broker).locator();
      broker.close();
   }

   /**
    * Base teardown
    *
    * @throws Exception If tearDown process can not be successfuly finished
    */
   protected void tearDown()
        throws Exception {
      super.tearDown();

      // Log-off user.
      logOut();

      this.session.close();
   }

   /**
    * Log-in Administrator user.
    */
   protected void logIn() {
      logIn(OpUser.ADMINISTRATOR_NAME, OpUserService.BLANK_PASSWORD);
   }

   /**
    * Tries to login user with the provided data, but only after a log-out.
    *
    * @param userName user name to be used for log-in.
    * @param password user password.
    */
   protected void logIn(String userName, String password) {
      // Authenticate user to be sure that he has access.
      XMessage request = new XMessage();
      request.setArgument(OpUserService.LOGIN, userName);
      request.setArgument(OpUserService.PASSWORD, password);
      XMessage response = getUserService().signOn(session, request);
      assertNoError(response);
   }

   /**
    * Log-out current user.
    */
   protected void logOut() {
      XMessage response = getUserService().signOff(session, new XMessage());
      assertNoError(response);
   }

   /**
    * Asserts that a XMessage reply from a service method contains not error msgs.
    *
    * @param message message to process.
    */
   public static void assertNoError(XMessage message) {
      if (message != null) {
         XError error = message.getError();
         String errorMessage = "No error message should have been returned.";
         if (error != null) {
            errorMessage += "Received error: " + error.getName();
         }
         assertNull(errorMessage, error);
      }
      else {
         //message is null <=> no error (success)
      }
   }

   /**
    * Asserts that a XMessage reply from a service method contains error msgs.
    *
    * @param message message to process.
    */
   public static void assertError(XMessage message) {
      assertError(message, DUMMY_ERROR_CODE);
   }

   /**
    * Asserts that a XMessage reply from a service method contains error msgs.
    *
    * @param message   message to process.
    * @param errorCode expected error code.
    */
   public static void assertError(XMessage message, int errorCode) {
      if (message != null) {
         XError error = message.getError();
         assertNotNull("Error message should have been returned", error);
         int foundErrorCode = error.getCode();
         // do not check error code in case DUMMY code was used.
         assertTrue("Invalid error code.", errorCode == DUMMY_ERROR_CODE || errorCode == foundErrorCode);
         assertNotNull("Error should contain an error name.", error.getName());
         assertNotNull("Error should contain an error message.", error.getName());
      }
      else {
         //message is null <=> no error (success)
      }
   }

   // ----- Helper Methods ------
   /**
    * Returns configuration used during testing process.
    *
    * @return instance of configuration.
    */
   protected static OpConfiguration getTestingConfiguration() {
      OpConfigurationLoader configurationLoader = new OpConfigurationLoader();
      String projectPath = OpEnvironmentManager.getOnePointHome();
      OpConfiguration configuration = configurationLoader.loadConfiguration(projectPath
           + "/" + OpConfigurationLoader.CONFIGURATION_FILE_NAME);

      if (configuration == null) {
         throw new RuntimeException("Could not load configuration file.");
      }

      return configuration;
   }

   /**
    * Return used UserService
    *
    * @return an instance of <code>OpUserService</code>
    */
   protected static OpUserService getUserService() {
      return (OpUserService) XServiceManager.getService(USER_SERVICE_NAME);
   }

   /**
    * Return the Preferences Service instance
    *
    * @return an instance of <code>OpPreferencesService</code>
    */
   protected static OpPreferencesService getPreferencesService() {
      return (OpPreferencesService) XServiceManager.getService(PREFERENCES_SERVICE_NAME);
   }

   /**
    * Return the Settings Service instance
    *
    * @return an instance of <code>OpSettingsService</code>
    */
   protected static OpSettingsService getSettingsService() {
      return (OpSettingsService) XServiceManager.getService(SETTINGS_SERVICE_NAME);
   }

   /**
    * Return the Project Status Service instance
    *
    * @return an instance of <code>OpProjectStatusService</code>
    */
   protected static OpProjectStatusService getProjectStatusService() {
      return (OpProjectStatusService) XServiceManager.getService(PROJECT_STATUS_SERVICE_NAME);
   }

   /**
    * Return the Project Service instance
    *
    * @return an instance of <code>OpProjectAdministrationService</code>
    */
   protected static OpProjectAdministrationService getProjectService() {
      return (OpProjectAdministrationService) XServiceManager.getService(PROJECT_SERVICE_NAME);
   }

   /**
    * Return the Resource Service instance
    *
    * @return an instance of <code>OpResourceService</code>
    */
   protected static OpResourceService getResourceService() {
      return (OpResourceService) XServiceManager.getService(RESOURCE_SERVICE_NAME);
   }

   /**
    * Return the Repository Service instance
    *
    * @return an instance of <code>OpRepositoryService</code>
    */
   protected static OpRepositoryService getRepositoryService() {
      return (OpRepositoryService) XServiceManager.getService(REPOSITORY_SERVICE_NAME);
   }

   /**
    * Return the Configuration Wizard Service instance
    *
    * @return an instance of <code>OpConfigurationWizardService</code>
    */
   protected static OpConfigurationWizardService getConfigurationWizardService() {
      return (OpConfigurationWizardService) XServiceManager.getService(CONFIG_WIZARD_SERVICE_NAME);
   }

   /**
    * Return the Repository Service instance
    *
    * @return an instance of <code>OpReportService</code>
    */
   protected static OpReportService getReportService() {
      return (OpReportService) XServiceManager.getService(REPORT_SERVICE_NAME);
   }

   /**
    * Return the Project Planning Service instance
    *
    * @return an instance of <code>OpProjectPlanningService</code>
    */
   protected static OpProjectPlanningService getProjectPlanningService() {
      return (OpProjectPlanningService) XServiceManager.getService(PLANNING_SERVICE_NAME);
   }

   /**
    * Return the Work Service instance
    *
    * @return an instance of <code>OpWorkService</code>
    */
   protected static OpWorkService getWorkService() {
      return (OpWorkService) XServiceManager.getService(WORK_SERVICE_NAME);
   }

   /**
    * Return the My Tasks Service instance
    *
    * @return an instance of <code>OpMyTasksService</code>
    */
   protected static OpMyTasksService getMyTasksService() {
      return (OpMyTasksService) XServiceManager.getService(MY_TASKS_SERVICE_NAME);
   }

   /**
    * Return the Resource Utilization Service instance
    *
    * @return an instance of <code>OpResourceUtilizationService</code>
    */
   protected static OpResourceUtilizationService getResourceUtilizationService() {
      return (OpResourceUtilizationService) XServiceManager.getService(RESOURCE_UTILIZATION_SERVICE_NAME);
   }

}
