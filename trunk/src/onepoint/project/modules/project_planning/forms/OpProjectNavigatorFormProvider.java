/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import java.util.Collection;
import java.util.HashMap;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.service.server.XSession;

public class OpProjectNavigatorFormProvider implements XFormProvider {

   /**
    * Form component ids.
    */
   private static final String ARCHIVED_PROJECTS_FIELD_ID = "ArchivedProjects";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      String selectLocator = (String)parameters.get(XFormProvider.SELECT);

      XComponent dataSet = form.findComponent("ProjectNavigatorDataSet");

      Collection<String> archivedProjectsLocators = OpProjectDataSetFactory.retrieveArchivedProjects(session, true);
      form.findComponent(ARCHIVED_PROJECTS_FIELD_ID).setValue(archivedProjectsLocators);
      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy(session, dataSet, OpProjectDataSetFactory.ALL_TYPES, false, archivedProjectsLocators, selectLocator);
      
      // *** GM: For URL-based navigation: Check parameters for "project" parameter
      // ==> PROBLEM: We need to locate the project inside the hierarchy
      // ==> If it is not yet in we need to retrieve it via a broker
      // ==> Then, we need to traverse the super-project-node hierarchy
      // ==> Finally, we need to add and "open" each part of the project-node hierarchy
      
   }

}
