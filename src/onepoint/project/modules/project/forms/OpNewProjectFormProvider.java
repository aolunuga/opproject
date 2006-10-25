/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectModule;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpNewProjectFormProvider implements XFormProvider {

   public final static String PORTFOLIO_ID = "PortfolioID";
   public final static String PORTFOLIO_INDEX_FIELD = "PortfolioIndexField";

   public final static String PERMISSION_SET = "PermissionSet";
   
   public final static String PORTFOLIO_INDEX = "portfolio_index";
   
   public final static String PROJECT_NEW_PROJECT = "project.NewProject";
   public final static String NOT_SELECTED = "NotSelected";
   public final static String TEMPLATE_FIELD = "TemplateField";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      String portfolioLocator = (String) (parameters.get(OpProjectAdministrationService.PORTFOLIO_ID));
      
      OpBroker broker = session.newBroker();

      if (portfolioLocator == null)
         portfolioLocator = OpProjectAdministrationService.findRootPortfolio(broker).locator();
      form.findComponent(PORTFOLIO_ID).setStringValue(portfolioLocator);
      Integer portfolioIndex = (Integer) parameters.get(PORTFOLIO_INDEX);
      if (portfolioIndex != null)
         form.findComponent(PORTFOLIO_INDEX_FIELD).setIntValue(portfolioIndex.intValue());
      
      OpProjectNode portfolio = (OpProjectNode) broker.getObject(portfolioLocator);
      if (portfolio == null) {
         broker.close();
         return; // TODO: Show error on page that portfolio could not be found
      }
      byte portfolioAccesssLevel = session.effectiveAccessLevel(broker, portfolio.getID());

      //disable templates related stuff
      form.findComponent(TEMPLATE_FIELD).setEnabled(false);

      // Locate permission data set in form
      XComponent permissionSet = form.findComponent(PERMISSION_SET);

      // Retrieve permission set of portfolio -- inheritance of permissions
      OpPermissionSetFactory.retrievePermissionSet(session, broker, portfolio.getPermissions(), permissionSet,
            OpProjectModule.PROJECT_ACCESS_LEVELS, session.getLocale());
      OpPermissionSetFactory.administratePermissionTab(form, true, portfolioAccesssLevel);
      
      broker.close();

   }

}
