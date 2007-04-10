/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpSaveAsTemplateFormProvider implements XFormProvider {

   public final static String PORTFOLIO_SET = "PortfolioSet";
   public final static String PROJECT_ID = "ProjectID";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      String projectLocator = (String) (parameters.get(OpProjectAdministrationService.PROJECT_ID));
      
      form.findComponent(PROJECT_ID).setStringValue(projectLocator);

      OpBroker broker = session.newBroker();
      
      // Populate portfolio data set
      XComponent dataSet = form.findComponent(PORTFOLIO_SET);
      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy(session, dataSet, OpProjectDataSetFactory.PORTFOLIOS, false, null);

      broker.close();

   }

}
