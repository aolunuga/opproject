/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.configuration.OpInvalidDataBaseConfigurationException;
import onepoint.project.modules.configuration_wizard.OpConfigurationWizardService;
import onepoint.project.modules.documents.OpDocumentsService;
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
import onepoint.project.modules.user.OpUserService;
import onepoint.project.modules.work.OpWorkService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.service.server.XServiceManager;

import java.io.File;

/**
 * This is a helper class that helps you to generate strctures necessary into test (E.g to call services) and
 * in the same time to retrieve data from database since there is no  clear mechanism for doing that into product.
 *
 * @author calin.pavel
 */
public abstract class OpTestDataFactory {

   // This is the session that must be used to get data.
   protected OpProjectSession session;

   // Defines the name of the registry file used through tests.
   public static final String RESOURCE_PATH = "onepoint/project";

   // Locales file.
   public static final String LOCALES_OLM_XML = "/locales.olm.xml";

   // Name of the users service.
   public static final String USER_SERVICE_NAME = "UserService";

   // Name of the preferences service.
   public static final String PREFERENCES_SERVICE_NAME = "PreferencesService";

   // Name of the settings service.
   public static final String SETTINGS_SERVICE_NAME = "SettingsService";

   // Name of the project status service.
   public static final String PROJECT_STATUS_SERVICE_NAME = "ProjectStatusService";

   // Name of the project service.
   public static final String PROJECT_SERVICE_NAME = OpProjectAdministrationService.SERVICE_NAME;

   // Name of the resource service.
   public static final String RESOURCE_SERVICE_NAME = OpResourceService.SERVICE_NAME;

   // Name of the repository service.
   public static final String REPOSITORY_SERVICE_NAME = "RepositoryService";

   // Name of the report service.
   public static final String REPORT_SERVICE_NAME = "ReportService";

   // Name of the configuration wizard service.
   public static final String CONFIG_WIZARD_SERVICE_NAME = "DBConfigurationWizard";

   // Name of the planning service.
   public static final String PLANNING_SERVICE_NAME = "PlanningService";

   // Name of the work service.
   public static final String WORK_SERVICE_NAME = "WorkService";

   // Name of the mytasks service.
   public static final String MY_TASKS_SERVICE_NAME = "MyTasksService";

   // Name of the resource utilization service.
   public static final String RESOURCE_UTILIZATION_SERVICE_NAME = "ResourceUtilizationService";

   // Name of the documents service.
   public static final String DOCUMENTS_SERVICE_NAME = "DocumentsService";


   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public OpTestDataFactory(OpProjectSession session) {
      this.session = session;
   }

   /**
    * Helper method used to create a permission set
    *
    * @param level    the permission level (Administrator|Contributer|Observer)
    * @param userId   the locator of the user
    * @param userName the cname of the user
    * @return the permission DATA_SET
    */
   public static XComponent createPermissionSet(byte level, String userId, String userName) {
      XComponent permSet = new XComponent(XComponent.DATA_SET);
      XComponent row = new XComponent(XComponent.DATA_ROW);
      XComponent cell = new XComponent(XComponent.DATA_CELL);
      // first level
      row.setOutlineLevel(0);
      row.addChild(cell);
      cell.setByteValue(level);
      permSet.addChild(row);
      // user level
      row = new XComponent(XComponent.DATA_ROW);
      row.setOutlineLevel(1);
      row.setValue(XValidator.choice(userId, userName));
      permSet.addChild(row);

      return permSet;
   }

   /**
    * Returns configuration used during testing process.
    *
    * @return instance of configuration.
    */
   public static OpConfiguration getTestingConfiguration() {
      OpConfigurationLoader configurationLoader = new OpConfigurationLoader();
      String projectPath = OpEnvironmentManager.getOnePointHome();
      OpConfiguration configuration = null;
      try {
         configuration = configurationLoader.loadConfiguration(projectPath
              + File.separator + OpConfigurationLoader.CONFIGURATION_FILE_NAME);
      }
      catch (OpInvalidDataBaseConfigurationException e) {
         throw new RuntimeException("Could not load configuration file.");
      }

      return configuration;
   }

   /**
    * Return used UserService
    *
    * @return an instance of <code>OpUserService</code>
    */
   public static OpUserService getUserService() {
      return (OpUserService) XServiceManager.getService(USER_SERVICE_NAME);
   }

   /**
    * Return the Preferences Service instance
    *
    * @return an instance of <code>OpPreferencesService</code>
    */
   public static OpPreferencesService getPreferencesService() {
      return (OpPreferencesService) XServiceManager.getService(PREFERENCES_SERVICE_NAME);
   }

   /**
    * Return the Settings Service instance
    *
    * @return an instance of <code>OpSettingsService</code>
    */
   public static OpSettingsService getSettingsService() {
      return (OpSettingsService) XServiceManager.getService(SETTINGS_SERVICE_NAME);
   }

   /**
    * Return the Project Status Service instance
    *
    * @return an instance of <code>OpProjectStatusService</code>
    */
   public static OpProjectStatusService getProjectStatusService() {
      return (OpProjectStatusService) XServiceManager.getService(PROJECT_STATUS_SERVICE_NAME);
   }

   /**
    * Return the Project Service instance
    *
    * @return an instance of <code>OpProjectAdministrationService</code>
    */
   public static OpProjectAdministrationService getProjectService() {
      return (OpProjectAdministrationService) XServiceManager.getService(PROJECT_SERVICE_NAME);
   }

   /**
    * Return the Resource Service instance
    *
    * @return an instance of <code>OpResourceService</code>
    */
   public static OpResourceService getResourceService() {
      return (OpResourceService) XServiceManager.getService(RESOURCE_SERVICE_NAME);
   }

   /**
    * Return the Repository Service instance
    *
    * @return an instance of <code>OpRepositoryService</code>
    */
   public static OpRepositoryService getRepositoryService() {
      return (OpRepositoryService) XServiceManager.getService(REPOSITORY_SERVICE_NAME);
   }

   /**
    * Return the Configuration Wizard Service instance
    *
    * @return an instance of <code>OpConfigurationWizardService</code>
    */
   public static OpConfigurationWizardService getConfigurationWizardService() {
      return (OpConfigurationWizardService) XServiceManager.getService(CONFIG_WIZARD_SERVICE_NAME);
   }

   /**
    * Return the Repository Service instance
    *
    * @return an instance of <code>OpReportService</code>
    */
   public static OpReportService getReportService() {
      return (OpReportService) XServiceManager.getService(REPORT_SERVICE_NAME);
   }

   /**
    * Return the Project Planning Service instance
    *
    * @return an instance of <code>OpProjectPlanningService</code>
    */
   public static OpProjectPlanningService getProjectPlanningService() {
      return (OpProjectPlanningService) XServiceManager.getService(PLANNING_SERVICE_NAME);
   }

   /**
    * Return the Work Service instance
    *
    * @return an instance of <code>OpWorkService</code>
    */
   public static OpWorkService getWorkService() {
      return (OpWorkService) XServiceManager.getService(WORK_SERVICE_NAME);
   }

   /**
    * Return the My Tasks Service instance
    *
    * @return an instance of <code>OpMyTasksService</code>
    */
   public static OpMyTasksService getMyTasksService() {
      return (OpMyTasksService) XServiceManager.getService(MY_TASKS_SERVICE_NAME);
   }

   /**
    * Return the Resource Utilization Service instance
    *
    * @return an instance of <code>OpResourceUtilizationService</code>
    */
   public static OpResourceUtilizationService getResourceUtilizationService() {
      return (OpResourceUtilizationService) XServiceManager.getService(RESOURCE_UTILIZATION_SERVICE_NAME);
   }

   /**
    * Return the Documents Service instance
    *
    * @return an instance of <code>OpDocumentsService</code>
    */
   public static OpDocumentsService getDocumentsService() {
      return (OpDocumentsService) XServiceManager.getService(DOCUMENTS_SERVICE_NAME);
   }
}
