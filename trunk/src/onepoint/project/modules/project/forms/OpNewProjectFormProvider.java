/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpNewProjectFormProvider implements XFormProvider {


   protected final static String PROJECT_NEW_PROJECT = "project.NewProject";
   protected final static String TEMPLATE_FIELD = "TemplateField";

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
   private final static String TODOS_TABLE_BOX = "ToDosTableBox";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      String portfolioLocator = (String) (parameters.get(OpProjectAdministrationService.PORTFOLIO_ID));

      OpBroker broker = session.newBroker();

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
         broker.close();
         return; //TODO: Show error on page that portfolio could not be found
      }
      byte portfolioAccesssLevel = session.effectiveAccessLevel(broker, portfolio.getID());

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
         statusDataSet.addChild(row);
      }

      //disable templates related stuff
      form.findComponent(TEMPLATE_FIELD).setEnabled(false);

      if (OpInitializer.isMultiUser()) {
         // Locate permission data set in form
         XComponent permissionSet = form.findComponent(PERMISSION_SET);
         // Retrieve permission set of portfolio -- inheritance of permissions
         OpPermissionSetFactory.retrievePermissionSet(session, broker, portfolio.getPermissions(), permissionSet,
              OpProjectModule.PROJECT_ACCESS_LEVELS, session.getLocale());
         OpPermissionSetFactory.administratePermissionTab(form, true, portfolioAccesssLevel);
      }
      else {
         form.findComponent(PERMISSIONS_TAB).setHidden(true);
      }

      //set edit mode to true for goals table and todos table
      form.findComponent(GOALS_TABLE_BOX).setEditMode(true);
      form.findComponent(TODOS_TABLE_BOX).setEditMode(true);
      broker.close();

   }

}
