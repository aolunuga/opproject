/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermission;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpProjectsFormProvider implements XFormProvider {

   /**
    * Form component ids.
    */
   private final static String PROJECT_DATA_SET = "ProjectDataSet";

   private static final String NEW_PROJECT_BUTTON = "NewProjectButton";
   private static final String NEW_PORTFOLIO_BUTTON = "NewPortfolioButton";
   private static final String INFO_BUTTON = "InfoButton";
   private static final String MOVE_BUTTON = "MoveButton";
   private static final String DELETE_BUTTON = "DeleteButton";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = ((OpProjectSession) session).newBroker();

      //set the value of the manager permission
      form.findComponent("ManagerPermission").setByteValue(OpPermission.MANAGER);

      //set the permission for the root portfolio
      OpProjectNode rootPortfolio = OpProjectAdministrationService.findRootPortfolio(broker);
      byte rootPortfolioPermission = session.effectiveAccessLevel(broker, rootPortfolio.getID());
      broker.close();
      form.findComponent("RootPortfolioPermission").setByteValue(rootPortfolioPermission);

      //see whether newXXX buttons should be enabled or disabled
      if (rootPortfolioPermission < OpPermission.MANAGER) {
         form.findComponent(NEW_PORTFOLIO_BUTTON).setEnabled(false);
         form.findComponent(NEW_PROJECT_BUTTON).setEnabled(false);
      }

      //disable buttons that required selection to work
      disableSelectionButtons(form);

      //retrieve project data set
      XComponent dataSet = form.findComponent(PROJECT_DATA_SET);
      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy(session, dataSet, OpProjectDataSetFactory.ALL_TYPES, true, null);
   }

   /**
    * Disables buttons that require a selection in order to be enabled.
    *
    * @param form a <code>XComponent</code> representing the project form.
    */
   protected void disableSelectionButtons(XComponent form) {
      form.findComponent(INFO_BUTTON).setEnabled(false);
      form.findComponent(MOVE_BUTTON).setEnabled(false);
      form.findComponent(DELETE_BUTTON).setEnabled(false);
   }
}
