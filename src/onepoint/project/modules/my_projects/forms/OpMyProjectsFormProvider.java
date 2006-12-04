/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.my_projects.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectStatus;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserAssignment;
import onepoint.service.server.XSession;

import java.util.*;

/**
 * Form provider for my projects tool.
 *
 * @author mihai.costin
 */
public class OpMyProjectsFormProvider implements XFormProvider {

   private final static String PROJECTS_DATA_SET = "ProjectsSet";
   private final static String PROJECT_CHOICE_ID = "project_choice_id";
   private final static String PROJECT_CHOICE_FIELD = "ProjectChooser";
   private final static int DEFAULT_PROJECT_CHOICE_FIELD_INDEX = 0;

   //project choice values
   private final static String ALL = "all";
   private final static String CONTRIB_M = "contribM";
   private final static String MANAGER = "manager";
   private final static String CONTRIB = "contrib";
   private final static String OBSERVER = "observer";
   private final static String DEFAULT_VALUE = CONTRIB_M;
   private final static String QUERY_STRING =
        "select project.ID from OpPermission as permission, OpProjectNode as project " +
             "where permission.Object.ID = project.ID " +
             "and permission.Subject.ID in (:subjectIds) " +
             "and project.Type in (:projectTypes)" +
             "and permission.AccessLevel in (:levels) " +
             "group by project.ID";

   protected Map projectMap;
   protected final int COMPLETE_INDEX = 2;
   protected final int BASE_EFFORT_INDEX = 3;
   protected final int ACTUAL_EFFORT_INDEX = 4;
   protected final int BASE_COST_INDEX = 5;
   protected final int ACTUAL_COST_INDEX = 6;
   protected final int PREDICTED_INDEX = 7;
   protected final int DEVIATION_INDEX = 8;
   protected final int DEVIATION_PERCENT_INDEX = 9;

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      XComponent projectsDataSet;
      projectsDataSet = form.findComponent(PROJECTS_DATA_SET);
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      OpUser user = (OpUser) (broker.getObject(OpUser.class, session.getUserID()));

      String projectChoice = (String) parameters.get(PROJECT_CHOICE_ID);
      if (projectChoice == null) {
         //set default value
         XComponent chooser = form.findComponent(PROJECT_CHOICE_FIELD);
         Map stateMap = session.getComponentStateMap(form.getID());
         int selectedIndex;
         if (stateMap != null) {
            Integer state = (Integer) stateMap.get(PROJECT_CHOICE_FIELD);
            if (state != null) {
               selectedIndex = state.intValue();
               String value = (String) ((XComponent) chooser.getDataSetComponent().getChild(selectedIndex)).getValue();
               projectChoice = XValidator.choiceID(value);
            }
            else {
               projectChoice = DEFAULT_VALUE;
               selectedIndex = DEFAULT_PROJECT_CHOICE_FIELD_INDEX;
            }
         }
         else {
            projectChoice = DEFAULT_VALUE;
            selectedIndex = DEFAULT_PROJECT_CHOICE_FIELD_INDEX;
         }
         chooser.setSelectedIndex(new Integer(selectedIndex));
      }


      OpQuery query = broker.newQuery(QUERY_STRING);
      List levels = new ArrayList();
      List subjectIds = new ArrayList();
      subjectIds.add(new Long(user.getID()));

      //TODO Author="Mihai Costin" Description="Just the first level groups. Is that enough?"
      Iterator assignments = user.getAssignments().iterator();
      OpUserAssignment assignment = null;
      while (assignments.hasNext()) {
         assignment = (OpUserAssignment) assignments.next();
         subjectIds.add(new Long(assignment.getGroup().getID()));
      }


      query.setCollection("subjectIds", subjectIds);
      List types = new ArrayList();
      types.add(new Byte(OpProjectNode.PROJECT));
      query.setCollection("projectTypes", types);


      if (ALL.equals(projectChoice)) {
         //show all projects the user has at least read access to
         levels.add(new Byte(OpPermission.OBSERVER));
         levels.add(new Byte(OpPermission.CONTRIBUTOR));
         levels.add(new Byte(OpPermission.MANAGER));
         levels.add(new Byte(OpPermission.ADMINISTRATOR));
      }
      else if (CONTRIB_M.equals(projectChoice)) {
         //show projects the user has contributor|manager permissions to
         levels.add(new Byte(OpPermission.CONTRIBUTOR));
         levels.add(new Byte(OpPermission.MANAGER));
      }
      else if (CONTRIB.equals(projectChoice)) {
         //show projects the user has contributor permissions to
         levels.add(new Byte(OpPermission.CONTRIBUTOR));
      }
      else if (MANAGER.equals(projectChoice)) {
         //show projects the user has manager permissions to
         levels.add(new Byte(OpPermission.MANAGER));
      }
      else if (OBSERVER.equals(projectChoice)) {
         //show projects the user has observer permissions to
         levels.add(new Byte(OpPermission.OBSERVER));
      }
      query.setCollection("levels", levels);
      List projectNodeIDs = broker.list(query);

      projectMap = new HashMap();
      for (Iterator iterator = projectNodeIDs.iterator(); iterator.hasNext();) {
         Long id = (Long) iterator.next();
         OpProjectNode projectNode = (OpProjectNode) broker.getObject(OpProjectNode.class, id.longValue());
         XComponent row = createProjectRow(projectNode);
         projectsDataSet.addChild(row);
         projectMap.put(new Long(projectNode.getID()), row);
      }

      //set complete (index 2 in the data set)
      List projectIds = new ArrayList();
      projectIds.addAll(projectMap.keySet());

      if (!projectIds.isEmpty()) {
         ArrayList activityTypes = new ArrayList();
         activityTypes.add(new Byte(OpActivity.STANDARD));
         activityTypes.add(new Byte(OpActivity.COLLECTION));

         this.setCompleteValueForProjects(broker, projectIds, types, activityTypes, projectMap, COMPLETE_INDEX);

         //set resource (effort) values
         List resources = this.getProjectResourceValues(broker, projectIds, types, activityTypes);
         Object[] record = null;
         for (int i = 0; i < resources.size(); i++) {
            record = (Object[]) resources.get(i);
            Long projId = (Long) record[0];
            Double actualEffort = (Double) record[1];
            Double baseEffort = (Double) record[2];
            XComponent dataRow = (XComponent) projectMap.get(projId);
            ((XComponent) dataRow.getChild(BASE_EFFORT_INDEX)).setDoubleValue(baseEffort.doubleValue());
            ((XComponent) dataRow.getChild(ACTUAL_EFFORT_INDEX)).setDoubleValue(actualEffort.doubleValue());
         }

         //set cost values
         setProjectCostValues(broker, projectIds, types, activityTypes);
      }
      
      broker.close();
   }

   private void setProjectCostValues(OpBroker broker, List projectIds, List types, ArrayList activityTypes) {
      Object[] record;
      List costs = this.getProjectCostsValues(broker, projectIds, types, activityTypes);
      record = null;
      for (int i = 0; i < costs.size(); i++) {
         record = (Object[]) costs.get(i);
         Long projId = (Long) record[0];
         Double actualCost = (Double) record[1];
         Double baseCost = (Double) record[2];
         XComponent dataRow = (XComponent) projectMap.get(projId);

         double base = baseCost.doubleValue();
         ((XComponent) dataRow.getChild(BASE_COST_INDEX)).setDoubleValue(base);

         double actual = actualCost.doubleValue();
         ((XComponent) dataRow.getChild(ACTUAL_COST_INDEX)).setDoubleValue(actual);

         double complete = ((XComponent) dataRow.getChild(COMPLETE_INDEX)).getDoubleValue();
         double predicted;
         if (actual > 0) {
            if (complete > 0) {
               predicted = actual * 100 / complete;
            }
            else {
               predicted = Math.max(actual, base);
            }
         }
         else {
            predicted = base - (base * complete / 100);
         }
         ((XComponent) dataRow.getChild(PREDICTED_INDEX)).setDoubleValue(predicted);

         double deviation = predicted - base;
         ((XComponent) dataRow.getChild(DEVIATION_INDEX)).setDoubleValue(deviation);

         double percentage;
         if (base != 0) {
            percentage = deviation * 100 / base;
         }
         else {
            if (deviation != 0){
               percentage = Double.MAX_VALUE;
            }
            else {
               percentage = 0;
            }
         }
         ((XComponent) dataRow.getChild(DEVIATION_PERCENT_INDEX)).setDoubleValue(percentage);
      }
   }

   /**
    * Retrieves from the DB the list of costs values (Actual & Base effort) for the each project ID from the ones given in the projectIds list.
    *
    * @param broker        Broket object to use for db query
    * @param projectIds    Projects to get the costs values for (will take only those projects that also satisfy the type constraint)
    * @param projectTypes  Types of projects to include in search
    * @param activityTypes Types of activities to take into account when calculating the costs values.
    * @return A List of records arrays. Each array contains the project ID, actual costs sum and base costs sum [in that order !]
    */
   private List getProjectCostsValues(OpBroker broker, List projectIds, List projectTypes, List activityTypes) {
      StringBuffer queryBuffer;
      OpQuery query;
      queryBuffer = new StringBuffer("select project.ID");
      queryBuffer
           .append(", sum(activity.ActualPersonnelCosts + activity.ActualTravelCosts + activity.ActualMaterialCosts + activity.ActualExternalCosts + activity.ActualMiscellaneousCosts)");
      queryBuffer
           .append(" , sum(activity.BasePersonnelCosts + activity.BaseTravelCosts + activity.BaseMaterialCosts + activity.BaseExternalCosts + activity.BaseMiscellaneousCosts)");
      queryBuffer
           .append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer
           .append(" where project.ID in (:projectIds) and project.Type in (:projectTypes) and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) group by project.ID");
      query = broker.newQuery(queryBuffer.toString());
      query.setCollection("projectIds", projectIds);
      query.setCollection("activityTypes", activityTypes);
      query.setCollection("projectTypes", projectTypes);
      return  broker.list(query);
   }

   /**
    * Retrieves from the DB the list of resource values (Actual & Base effort) for the each project ID from the ones given in the projectIds list.
    *
    * @param broker        Broket object to use for db query
    * @param projectIds    Projects to get the resource values for (will take only those projects that also satisfy the type constraint)
    * @param projectTypes  Types of projects to include in search
    * @param activityTypes Types of activities to take into account when calculating the resource values.
    * @return A List of records arrays. Each array contains the project ID, actual effort sum and base effort sum [in that order !]
    */
   private List getProjectResourceValues(OpBroker broker, List projectIds, List projectTypes, List activityTypes) {
      StringBuffer queryBuffer;
      OpQuery query;
      queryBuffer = new StringBuffer("select project.ID, sum(activity.ActualEffort), sum(activity.BaseEffort)");
      queryBuffer
           .append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer
           .append(" where project.ID in (:projectIds) and project.Type in (:projectTypes) and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) group by project.ID");
      query = broker.newQuery(queryBuffer.toString());
      query.setCollection("projectIds", projectIds);
      query.setCollection("projectTypes", projectTypes);
      query.setCollection("activityTypes", activityTypes);
      return broker.list(query);
   }

   private void setCompleteValueForProjects(OpBroker broker, List projectIds, List projectTypes, List activityTypes, Map dataRowMap, int completeIndex) {
      int i;
      XComponent dataRow;
      StringBuffer queryBuffer = new StringBuffer(
           "select project.ID, sum(activity.Complete * activity.Duration),  sum(activity.Duration)");
      queryBuffer
           .append(" from OpProjectNode as project inner join project.Plan as plan inner join plan.Activities as activity");
      queryBuffer
           .append(" where project.ID in (:projectIds) and project.Type in (:projectTypes) and activity.OutlineLevel = 0 and activity.Type in (:activityTypes) group by project.ID");
      OpQuery query = broker.newQuery(queryBuffer.toString());
      query.setCollection("projectIds", projectIds);
      query.setCollection("projectTypes", projectTypes);
      query.setCollection("activityTypes", activityTypes);
      List completes = broker.list(query);
      Object[] record = null;
      for (i = 0; i < completes.size(); i++) {
         record = (Object[]) completes.get(i);
         Long projId = (Long) record[0];
         Double sum1 = (Double) record[1];
         Double sum2 = (Double) record[2];
         dataRow = (XComponent) dataRowMap.get(projId);
         if (sum1 != null && sum2 != null) {
            double value = Double.MAX_VALUE;
            if (sum1.doubleValue() == 0 && sum2.doubleValue() == 0) {
               value = 0;
            }
            else if (sum2.doubleValue() != 0) {
               value = sum1.doubleValue() / sum2.doubleValue();
            }
            ((XComponent) dataRow.getChild(completeIndex)).setDoubleValue(value);
         }
      }
   }

   private XComponent createProjectRow(OpProjectNode projectNode) {
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      XComponent dataCell;

      //project locator
      dataRow.setStringValue(projectNode.locator());

      //project name  0
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(projectNode.getName());
      dataRow.addChild(dataCell);
      //project status  1
      dataCell = new XComponent(XComponent.DATA_CELL);
      OpProjectStatus projectStatus = projectNode.getStatus();
      if (projectStatus != null) {
         dataCell.setStringValue(projectStatus.getName());
      }
      else {
         dataCell.setStringValue(null);
      }
      dataRow.addChild(dataCell);
      //% complete   2
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);
      //base effort  3
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);
      //acctual effort 4
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);
      //base costs  5
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);
      //acctual costs  6
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);
      //predicted
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);
      //deviation
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);
      //deviation %
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(0);
      dataRow.addChild(dataCell);

      return dataRow;
   }


}
