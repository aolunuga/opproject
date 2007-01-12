/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.service.server.XSession;

import java.util.*;

/**
 * Form provider class for adding a new Ad-hoc Task.
 *
 * @author mihai.costin
 */
public class OpAddAdhocTaskFormProvider implements XFormProvider {

   private static final String RESOURCE_SET = "ResourceSet";
   private static final String PROJECT_SET = "ProjectSet";
   private static final String PROJECT_TO_RESOURCE_MAP = "ProjectToResourceMap";


   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      XComponent resourceDataSet = form.findComponent(RESOURCE_SET);
      XComponent projectToResource = form.findComponent(PROJECT_TO_RESOURCE_MAP);
      XComponent projectDataSet = form.findComponent(PROJECT_SET);
      OpProjectSession session = (OpProjectSession) s;

      Map projectsMap = OpProjectDataSetFactory.getProjectToResourceMap(session);

      projectToResource.setValue(projectsMap);

      //fill project chooser data set
      Set projects = projectsMap.keySet();
      List resources = null;
      for (Iterator iterator = projects.iterator(); iterator.hasNext();) {
         String choice = (String) iterator.next();
         XComponent row = new XComponent(XComponent.DATA_ROW);
         row.setStringValue(choice);
         projectDataSet.addChild(row);
         if (resources == null) {
            resources = (List) projectsMap.get(choice);
         }
      }

      //fill resource chooser data set
      if (resources != null) {
         for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
            String choice = (String) iterator.next();
            XComponent row = new XComponent(XComponent.DATA_ROW);
            row.setStringValue(choice);
            resourceDataSet.addChild(row);
         }
      }

   }
}
