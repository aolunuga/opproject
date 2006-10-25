/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class OpAddProjectFormProvider implements XFormProvider {

   public final static String PROJECT_DATA_SET = "ProjectDataSet";
   public final static String ASSIGNED_PROJECT_DATA_SET = "AssignedProjectDataSet";

   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      OpBroker broker = ((OpProjectSession) session).newBroker();

      // TODO: Use project chooser instead (hierarchical, reuse code)
      XComponent project_data_set = form.findComponent(PROJECT_DATA_SET);

      XComponent assigned_project_data_set = (XComponent)parameters.get(ASSIGNED_PROJECT_DATA_SET);
      /*project assignments for selected resource*/
      Set resourceAssignments = new TreeSet();
      for (int i=0 ;i < assigned_project_data_set.getChildCount();i++){
        XComponent assignmentRow = (XComponent)assigned_project_data_set.getChild(i);
        resourceAssignments.add(XValidator.choiceID(assignmentRow.getStringValue()));
      }
      // configure project sort order
      OpObjectOrderCriteria projectOrderCriteria = new OpObjectOrderCriteria(OpProjectNode.PROJECT_NODE, OpProjectNode.NAME, OpObjectOrderCriteria.ASCENDING);
      OpQuery query = broker.newQuery("select project from OpProjectNode as project where project.Type = ?" + projectOrderCriteria.toHibernateQueryString("project"));
      query.setByte(0, OpProjectNode.PROJECT);
      Iterator projects = broker.list(query).iterator();
      OpProjectNode project = null;
      XComponent data_row = null;
      while (projects.hasNext()) {
         project = (OpProjectNode) (projects.next());
         if (!resourceAssignments.contains(project.locator())){
            data_row = new XComponent(XComponent.DATA_ROW);
            data_row.setStringValue(XValidator.choice(project.locator(), project.getName()));
            project_data_set.addChild(data_row);
         }
      }

      broker.close();

   }

}
