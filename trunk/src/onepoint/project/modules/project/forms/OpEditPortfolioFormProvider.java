/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectModule;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpEditPortfolioFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpEditPortfolioFormProvider.class, true);

   private final static String PORTFOLIO_ID = "PortfolioID";
   private final static String EDIT_MODE = "EditMode";
   private final static String PERMISSION_SET = "PermissionSet";
   private final static String PERMISSIONS_TAB = "PermissionsTab";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Find user in database
      String id_string = (String) (parameters.get(OpProjectAdministrationService.PORTFOLIO_ID));
      Boolean edit_mode = (Boolean) parameters.get(OpProjectAdministrationService.EDIT_MODE);

      logger.debug("OpEditPortfolioFormProvider.prepareForm(): " + id_string);
      OpBroker broker = ((OpProjectSession) session).newBroker();
      OpProjectNode portfolio = (OpProjectNode) (broker.getObject(id_string));

      // Downgrade edit mode to view mode if no manager access
      byte accessLevel = session.effectiveAccessLevel(broker, portfolio.getID());
      if (edit_mode.booleanValue() && (accessLevel < OpPermission.MANAGER)) {
         edit_mode = Boolean.FALSE;
      }

      form.findComponent(PORTFOLIO_ID).setStringValue(id_string);
      form.findComponent(EDIT_MODE).setBooleanValue(edit_mode.booleanValue());

      // Localizer is used to localize name and description of root resource pool
      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpProjectDataSetFactory.PROJECT_OBJECTS));
      XComponent name = form.findComponent(OpProjectNode.NAME);
      name.setStringValue(localizer.localize(portfolio.getName()));
      XComponent desc = form.findComponent(OpProjectNode.DESCRIPTION);
      desc.setStringValue(localizer.localize(portfolio.getDescription()));

      if (!edit_mode.booleanValue()) {
         name.setEnabled(false);
         desc.setEnabled(false);
         form.findComponent("Cancel").setVisible(false);
         String title = session.getLocale().getResourceMap("project.Info").getResource("InfoPortfolio").getText();
         form.setText(title);
      }
      else if (portfolio.getName().equals(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME)) {
         // Root project portfolio name and description are not editable at all
         name.setEnabled(false);
         desc.setEnabled(false);
      }

      if (OpInitializer.isMultiUser()) {
         // Locate permission data set in form
         XComponent permissionSet = form.findComponent(PERMISSION_SET);
         OpPermissionSetFactory.retrievePermissionSet(session, broker, portfolio.getPermissions(), permissionSet,
              OpProjectModule.PORTFOLIO_ACCESS_LEVELS, session.getLocale());
         OpPermissionSetFactory.administratePermissionTab(form, edit_mode.booleanValue(), accessLevel);
      }
      else {
         form.findComponent(PERMISSIONS_TAB).setHidden(true);
      }
      broker.close();
   }
}
