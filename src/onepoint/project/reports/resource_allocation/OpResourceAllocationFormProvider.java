/*
 * Copyright(c) Onepoint Software GmbH 2008. All Rights Reserved.
 */

package onepoint.project.reports.resource_allocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.service.server.XSession;

/**
 * @author mihai.costin
 */
public class OpResourceAllocationFormProvider implements XFormProvider {
   private final static String PROJECT_SET = "ProjectSet";

   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      // *** Put all project names into project data-set (values are IDs)
      XComponent dataSet = form.findComponent(PROJECT_SET);
      Collection<String> archivedProjectLocators = OpProjectDataSetFactory.retrieveArchivedProjects((OpProjectSession) session, true);

      int types = OpProjectDataSetFactory.PROJECTS + OpProjectDataSetFactory.PORTFOLIOS + OpProjectDataSetFactory.TEMPLATES;
      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy((OpProjectSession) session, dataSet, types, false, archivedProjectLocators);

      Map enableParam = new HashMap();
      enableParam.put(OpProjectDataSetFactory.ENABLE_PORTFOLIOS, false);
      enableParam.put(OpProjectDataSetFactory.ENABLE_TEMPLATES, false);
      enableParam.put(OpProjectDataSetFactory.ENABLE_PROJECTS, true);

      //enable or disable any nodes
      OpProjectDataSetFactory.enableNodes(enableParam, dataSet);

   }

}
