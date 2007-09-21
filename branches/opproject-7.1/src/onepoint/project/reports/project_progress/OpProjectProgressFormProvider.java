/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.reports.project_progress;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OpProjectProgressFormProvider implements XFormProvider {

   /**
    * Form component ids.
    */
   private final static String PROJECT_LOCATOR_FIELD = "ProjectLocatorField";
   private final static String PROJECT_NAME_FIELD = "ProjectNameField";
   private final static String RUN_QUERY = "RunQuery";
   private final static String PROJECT_LOCATOR = "ProjectLocator";
   private final static String PROJECTS_WITH_BASELINE = "ProjectsWithBaseline";

   public void prepareForm(XSession s, XComponent form,
        HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      XComponent projectsWithBaselineDataField = form.findComponent(PROJECTS_WITH_BASELINE);
      //cache the project with baseline map
      if (projectsWithBaselineDataField.getValue() == null) {
         projectsWithBaselineDataField.setValue(OpProjectDataSetFactory.getProjectsWithBaseline(session));
      }
      Map<String, Long> projectsWithBaseline = (Map<String, Long>) projectsWithBaselineDataField.getValue();

      XLocalizer localizer = new XLocalizer();
      localizer.setResourceMap(session.getLocale().getResourceMap(OpPermissionSetFactory.USER_OBJECTS));

      OpBroker broker = ((OpProjectSession) session).newBroker();

      // Execute query and fill result set if RunQuery is true
      if (parameters != null) {
         Boolean runQuery = (Boolean) parameters.get(RUN_QUERY);
         if ((runQuery != null) && (runQuery.booleanValue())) {

            String projectLocator = (String) parameters.get(PROJECT_LOCATOR);
            if (projectLocator == null) {
               broker.close();
               return; // TODO: Throw exception
            }

            OpProjectNode project = (OpProjectNode) broker.getObject(projectLocator);
            if (project == null) {
               broker.close();
               return; // TODO: Throw exception
            }

            form.findComponent(PROJECT_LOCATOR_FIELD).setStringValue(projectLocator);
            form.findComponent(PROJECT_NAME_FIELD).setStringValue(project.getName());

            //Get the correct query (according to baseline)
            OpQuery query = null;
            if (projectsWithBaseline.get(projectLocator) != null) {
               query = this.getQueryWithBaseline(broker, project.getID(), projectsWithBaseline.get(projectLocator));
            }
            else {
               query = this.getQueryWithoutBaseline(broker, project.getID());
            }
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

   /**
    * Returns the report query for a project that has no baseline.
    *
    * @param broker    a <code>OpBroker</code> used for persistence operations.
    * @param projectId a <code>long</code> the id of the project being queried.
    * @return a <code>OpQuery</code> instance.
    */
   private OpQuery getQueryWithoutBaseline(OpBroker broker, long projectId) {
      StringBuffer queryBuffer = new StringBuffer("select activity.Name, activity.Complete, activity.BaseEffort, activity.ActualEffort");
      queryBuffer.append(", activity.BasePersonnelCosts + activity.BaseTravelCosts + activity.BaseMaterialCosts + activity.BaseExternalCosts + activity.BaseMiscellaneousCosts");
      queryBuffer.append(", activity.ActualPersonnelCosts + activity.ActualTravelCosts + activity.ActualMaterialCosts + activity.ActualExternalCosts + activity.ActualMiscellaneousCosts");
      queryBuffer.append(" from OpProjectNode as project inner join project.Plan as projectPlan inner join projectPlan.Activities as activity");
      queryBuffer.append(" where project.Archived=false and project.ID = ? and activity.OutlineLevel = 0 and activity.Deleted = false");

      OpQuery result = broker.newQuery(queryBuffer.toString());
      result.setLong(0, projectId);
      return result;
   }

   /**
    * Returns the report query for a project that has a baseline.
    *
    * @param broker     a <code>OpBroker</code> used for persistence operations.
    * @param projectId  a <code>long</code> the id of the project being queried.
    * @param baselineId a <code>long</code> the id of the project baseline (project plan version).
    * @return a <code>OpQuery</code> instance.
    */
   private OpQuery getQueryWithBaseline(OpBroker broker, long projectId, long baselineId) {
      StringBuffer queryBuffer = new StringBuffer("select activity.Name, activity.Complete, activityVersion.BaseEffort, activity.ActualEffort");
      queryBuffer.append(", activityVersion.BasePersonnelCosts + activityVersion.BaseTravelCosts + activityVersion.BaseMaterialCosts + activityVersion.BaseExternalCosts + activityVersion.BaseMiscellaneousCosts");
      queryBuffer.append(", activity.ActualPersonnelCosts + activity.ActualTravelCosts + activity.ActualMaterialCosts + activity.ActualExternalCosts + activity.ActualMiscellaneousCosts");
      queryBuffer.append(" from OpProjectNode as project inner join project.Plan as projectPlan inner join projectPlan.Activities as activity inner join activity.Versions activityVersion inner join activityVersion.PlanVersion planVersion");
      queryBuffer.append(" where project.Archived=false and project.ID = ? and activity.OutlineLevel = 0 and activity.Deleted = false and planVersion.ID = ?");

      OpQuery result = broker.newQuery(queryBuffer.toString());
      result.setLong(0, projectId);
      result.setLong(1, baselineId);
      return result;

   }
}
