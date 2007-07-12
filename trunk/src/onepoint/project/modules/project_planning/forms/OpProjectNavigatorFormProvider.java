/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;

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
      XComponent dataSet = form.findComponent("ProjectNavigatorDataSet");

      ArrayList<String> archivedProjectsLocators = OpProjectDataSetFactory.retrieveArchivedProjects(session, true);
      form.findComponent(ARCHIVED_PROJECTS_FIELD_ID).setListValue(archivedProjectsLocators);
      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy(session, dataSet, OpProjectDataSetFactory.ALL_TYPES, false, archivedProjectsLocators);
   }

}
