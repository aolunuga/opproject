/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectModule;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpNewTemplateFormProvider implements XFormProvider {

   private final static String PORTFOLIO_ID = "PortfolioID";
   private final static String PORTFOLIO_INDEX_FIELD = "PortfolioIndexField";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String PORTFOLIO_INDEX = "portfolio_index";
   private final static String PERMISSIONS_TAB = "PermissionsTab";

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
         return; // TODO: Show error on page that portfolio could not be found
      }
      byte portfolioAccesssLevel = session.effectiveAccessLevel(broker, portfolio.getID());

      if (OpInitializer.isMultiUser()) {
         // Locate permission data set in form
         XComponent permissionSet = form.findComponent(PERMISSION_SET);
         // Retrieve permission set of portfolio -- inheritance of permissions
         OpPermissionSetFactory.retrievePermissionSet(session, broker, portfolio.getPermissions(), permissionSet,
              OpProjectModule.TEMPLATE_ACCESS_LEVELS, session.getLocale());
         OpPermissionSetFactory.administratePermissionTab(form, true, portfolioAccesssLevel);
      }
      else {
         form.findComponent(PERMISSIONS_TAB).setHidden(true);
      }
      broker.close();

   }

}
