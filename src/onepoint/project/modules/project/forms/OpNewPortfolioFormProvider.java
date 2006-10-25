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

public class OpNewPortfolioFormProvider implements XFormProvider {

   public final static String SUPER_PORTFOLIO_FIELD_ID = "SuperPortfolioID";
   public final static String SUPER_PORTFOLIO_INDEX_FIELD = "SuperPortfolioIndexField";

   public final static String PERMISSION_SET = "PermissionSet";

   public final static String SUPER_PORTFOLIO_INDEX = "super_portfolio_index";
   public final static String SUPER_PORTFOLIO_ID = "super_portfolio_id";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      String superPortfolioLocator = (String) (parameters.get(SUPER_PORTFOLIO_ID));

      OpBroker broker = session.newBroker();

      if (superPortfolioLocator == null) {
         superPortfolioLocator = OpProjectAdministrationService.findRootPortfolio(broker).locator();
      }
      form.findComponent(SUPER_PORTFOLIO_FIELD_ID).setStringValue(superPortfolioLocator);
      Integer superPortfolioIndex = (Integer) parameters.get(SUPER_PORTFOLIO_INDEX);
      if (superPortfolioIndex != null) {
         form.findComponent(SUPER_PORTFOLIO_INDEX_FIELD).setIntValue(superPortfolioIndex.intValue());
      }

      OpProjectNode superPortfolio = (OpProjectNode) broker.getObject(superPortfolioLocator);
      if (superPortfolio == null) {
         broker.close();
         return; // TODO: Show error on page that portfolio could not be found
      }
      byte superPortfolioAccesssLevel = session.effectiveAccessLevel(broker, superPortfolio.getID());

      // Locate permission data set in form
      XComponent permissionSet = form.findComponent(PERMISSION_SET);

      // Retrieve permission set of portfolio -- inheritance of permissions
      OpPermissionSetFactory.retrievePermissionSet(session, broker, superPortfolio.getPermissions(), permissionSet,
           OpProjectModule.PORTFOLIO_ACCESS_LEVELS, session.getLocale());
      OpPermissionSetFactory.administratePermissionTab(form, true, superPortfolioAccesssLevel);
      broker.close();

   }

}
