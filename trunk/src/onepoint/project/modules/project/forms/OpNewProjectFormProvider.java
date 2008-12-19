/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectCalendar;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpNewProjectFormProvider implements XFormProvider {


   protected final static String PROJECT_NEW_PROJECT = "project.NewProject";
   protected final static String TEMPLATE_FIELD = "TemplateField";
   protected final static String SUB_TYPE_FIELD = "SubTypeField";
   protected final static String SUB_TYPE_LABEL = "SubTypeLabel";

   private final static String NO_STATUS = "NoStatus";
   private final static String NULL_ID = "null";
   private final static String PORTFOLIO_ID = "PortfolioID";
   private final static String PORTFOLIO_INDEX_FIELD = "PortfolioIndexField";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String PORTFOLIO_INDEX = "portfolio_index";
   private final static String PROJECT_STATUS_DATA_SET = "ProjectStatusDataSet";
   private final static String PERMISSIONS_TAB = "PermissionsTab";
   private final static String READ_ONLY_RESOURCES_SET = "ReadOnlyResourceDataSet";
   private final static String GOALS_TABLE_BOX = "GoalsTableBox";
   private final static String TODAY_DATE_FIELD = "Today";
   private final static String END_OF_YEAR_DATE_FIELD = "EndOfYear";
   private final static String PROJECT_STATUS_CHOICE = "StatusChoice";
   private final static String BUDGET = "Budget";

   private final static String ADJUST_RATES_COLUMN = "AdjustRatesColumn";
   private final static String INTERNAL_RATES_COLUMN = "InternalRatesColumn";
   private final static String EXTENAL_RATES_COLUMN = "ExternalRatesColumn";

   private static final String EFFORT_BASED_PLANNING = "CalculationMode";
   private static final String ENABLE_PROGRESS_TRACKING = "ProgressTracked";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      //disable resource rates / project
      form.findComponent(ADJUST_RATES_COLUMN).setHidden(true);
      form.findComponent(INTERNAL_RATES_COLUMN).setHidden(true);
      form.findComponent(EXTENAL_RATES_COLUMN).setHidden(true);

      //set default values
      form.findComponent(OpProjectNode.PRIORITY).setIntValue(OpProjectNode.DEFAULT_PRIORITY);
      form.findComponent(OpProjectNode.PROBABILITY).setIntValue(OpProjectNode.DEFAULT_PROBABILITY);
      form.findComponent(OpProjectNode.ARCHIVED).setBooleanValue(OpProjectNode.DEFAULT_ARCHIVED);

      String portfolioLocator = (String) (parameters.get(OpProjectAdministrationService.PORTFOLIO_ID));

      OpBroker broker = session.newBroker();
      try {
         if (portfolioLocator == null) {
            portfolioLocator = OpProjectAdministrationService.findRootPortfolio(broker).locator();
         }
         form.findComponent(PORTFOLIO_ID).setStringValue(portfolioLocator);
         Integer portfolioIndex = (Integer) parameters.get(PORTFOLIO_INDEX);
         if (portfolioIndex != null) {
            form.findComponent(PORTFOLIO_INDEX_FIELD).setIntValue(portfolioIndex.intValue());
         }

         OpProjectNode portfolio = (OpProjectNode) broker.getObject(portfolioLocator);
         if (portfolio == null) {
            return; //TODO: Show error on page that portfolio could not be found
         }
         byte portfolioAccesssLevel = session.effectiveAccessLevel(broker, portfolio.getId());

         XComponent statusChoice = form.findComponent(PROJECT_STATUS_CHOICE);
         XComponent budget = form.findComponent(BUDGET);

         if (portfolioAccesssLevel != OpPermission.ADMINISTRATOR) {
            statusChoice.setEnabled(false);
            budget.setEnabled(false);
         }

         XComponent readOnlyResources = form.findComponent(READ_ONLY_RESOURCES_SET);
         OpResourceDataSetFactory.fillReadOnlyResources(broker, session, readOnlyResources);

         //Fill status data set
         XComponent statusDataSet = form.findComponent(PROJECT_STATUS_DATA_SET);
         String nullChoice = XValidator.choice(NULL_ID, session.getLocale().getResourceMap(PROJECT_NEW_PROJECT).getResource(NO_STATUS).getText());
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(nullChoice);
         statusDataSet.addChild(row);
         Iterator statusIterator = OpProjectDataSetFactory.getProjectStatusIterator(broker);
         while (statusIterator.hasNext()) {
            OpProjectStatus status = (OpProjectStatus) statusIterator.next();
            row = new XComponent(XComponent.DATA_ROW);
            row.setStringValue(XValidator.choice(String.valueOf(status.locator()), status.getName()));

            if (status.getActive() == false) {
               row.setEnabled(false);
            }

            statusDataSet.addChild(row);
         }

         // disable templates related stuff
         form.findComponent(TEMPLATE_FIELD).setEnabled(false);
         // disable sub types
         form.findComponent(SUB_TYPE_FIELD).setEnabled(false);

         if (OpEnvironmentManager.isMultiUser()) {
            // Locate permission data set in form
            XComponent permissionSet = form.findComponent(PERMISSION_SET);
            // Retrieve permission set of portfolio -- inheritance of permissions
            OpPermissionDataSetFactory.retrievePermissionSet(session, broker, portfolio.getPermissions(), permissionSet,
                 OpProjectModule.PROJECT_ACCESS_LEVELS, session.getLocale());
            OpPermissionDataSetFactory.administratePermissionTab(form, true, portfolioAccesssLevel);
         }
         else {
            form.findComponent(PERMISSIONS_TAB).setHidden(true);
         }

         //set edit mode to true for goals table
         form.findComponent(GOALS_TABLE_BOX).setEditMode(true);
      }
      finally {
         broker.close();
      }
      //set the date of today and end of year on the form
      form.findComponent(TODAY_DATE_FIELD).setDateValue(OpProjectCalendar.today());
      form.findComponent(END_OF_YEAR_DATE_FIELD).setDateValue(OpProjectCalendar.lastDayOfYear());

      // set the "effort-based planning" and "enable progress tracking" settings
      form.findComponent(EFFORT_BASED_PLANNING).setBooleanValue(Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.EFFORT_BASED_PLANNING)));
      form.findComponent(ENABLE_PROGRESS_TRACKING).setBooleanValue(Boolean.valueOf(OpSettingsService.getService().getStringValue(session, OpSettings.ENABLE_PROGRESS_TRACKING)));
   }
}