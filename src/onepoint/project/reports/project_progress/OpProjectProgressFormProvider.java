/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.reports.project_progress;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;

public class OpProjectProgressFormProvider implements XFormProvider {

   public final static String PROJECT_LOCATOR_FIELD = "ProjectLocatorField";

   public final static String PROJECT_NAME_FIELD = "ProjectNameField";

   public final static String RESULT_SET = "ResultSet";

   // Form parameters
   public final static String RUN_QUERY = "RunQuery";

   public final static String PROJECT_LOCATOR = "ProjectLocator";

   public void prepareForm(XSession s, XComponent form,
        HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

      OpBroker broker = ((OpProjectSession) session).newBroker();

      // Execute query and fill result set if RunQuery is true
      if (parameters != null) {
         Boolean runQuery = (Boolean) parameters.get(onepoint.project.reports.project_progress.OpProjectProgressFormProvider.RUN_QUERY);
         if ((runQuery != null) && (runQuery.booleanValue())) {

            String projectLocator = (String) parameters
                 .get(onepoint.project.reports.project_progress.OpProjectProgressFormProvider.PROJECT_LOCATOR);
            if (projectLocator == null) {
               broker.close();
               return; // TODO: Throw exception
            }

            OpProjectNode project = (OpProjectNode) broker
                 .getObject(projectLocator);
            if (project == null) {
               broker.close();
               return; // TODO: Throw exception
            }

            form.findComponent(onepoint.project.reports.project_progress.OpProjectProgressFormProvider.PROJECT_LOCATOR_FIELD).setStringValue(
                 projectLocator);
            form.findComponent(onepoint.project.reports.project_progress.OpProjectProgressFormProvider.PROJECT_NAME_FIELD).setStringValue(
                 project.getName());

            // Execute query
            StringBuffer queryBuffer = new StringBuffer("select activity.Name, activity.Complete, activity.BaseEffort, activity.ActualEffort");
            queryBuffer.append(", activity.BasePersonnelCosts + activity.BaseTravelCosts + activity.BaseMaterialCosts + activity.BaseExternalCosts + activity.BaseMiscellaneousCosts");
            queryBuffer.append(", activity.ActualPersonnelCosts + activity.ActualTravelCosts + activity.ActualMaterialCosts + activity.ActualExternalCosts + activity.ActualMiscellaneousCosts");
            queryBuffer.append(" from OpProjectNode as project inner join project.Plan as projectPlan inner join projectPlan.Activities as activity");
            queryBuffer.append(" where project.ID = ? and activity.OutlineLevel = 0 and activity.Deleted = false");

            OpQuery query = broker.newQuery(queryBuffer.toString());
            query.setLong(0, project.getID());
            Iterator i = broker.iterate(query);
            Object[] record = null;
            XComponent resultSet = form.findComponent("ResultSet");
            XComponent resultRow = null;
            XComponent resultCell = null;
            while (i.hasNext()) {
               record = (Object[]) i.next();
               resultRow = new XComponent(XComponent.DATA_ROW);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setStringValue((String) record[0]);
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[1]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[2]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[3]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[4]).doubleValue());
               resultRow.addChild(resultCell);
               resultCell = new XComponent(XComponent.DATA_CELL);
               resultCell.setDoubleValue(((Double) record[5]).doubleValue());
               resultRow.addChild(resultCell);
               resultSet.addChild(resultRow);
            }

         }
      }
      broker.close();
   }
}
