/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;

import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Data Set factory for WorkSlips .
 *
 * @author mihai.costin
 */
public class OpWorkSlipDataSetFactory {

   public final static long ALL_PROJECTS_ID = -1;

   /**
    * Utility class.
    */
   private OpWorkSlipDataSetFactory() {
   }

   /**
    * Gets the assignments satisfing the given constraints.
    *
    * @param broker        Broker used to qury the db.
    * @param resourceIds   Resources to take into account when searching for the assignments.
    * @param activityTypes Types of activities to include in the search.
    * @param start         Limit date. Will return only those assignments that have activities starting before the
    *                      given date.
    * @param projectNodeId Project Id filter. Will return only those assignments that have activities belonging
    *                      to the given project. If all projects are to be taken into account, ALL_PROJECTS_ID
    *                      should be used.
    * @return Iterator over the found assignments.
    */
   public static Iterator getAssignments(OpBroker broker, List resourceIds, List activityTypes, Date start, long projectNodeId) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("select assignment, activity from OpAssignment as assignment inner join assignment.Activity as activity ");
      buffer.append("where assignment.Resource.ID in (:resourceIds) and assignment.Complete < 100 and activity.Type in (:type)");
      if (start != null) {
         buffer.append(" and activity.Start < :startBefore");
      }
      if (projectNodeId != ALL_PROJECTS_ID) {
         buffer.append(" and assignment.ProjectPlan.ProjectNode.ID = :projectNodeId");
      }
      OpQuery query = broker.newQuery(buffer.toString());
      query.setCollection("resourceIds", resourceIds);
      query.setCollection("type", activityTypes);
      if (start != null) {
         query.setDate("startBefore", start);
      }
      if (projectNodeId != ALL_PROJECTS_ID) {
         query.setLong("projectNodeId", projectNodeId);
      }
      return broker.iterate(query);
   }

   /**
    * Creates a work slip data row using the given information.
    *
    * @param activity        Workslip activity.
    * @param assignment      Workslip assignment.
    * @param progressTracked Flag indicating if progress tarck is on/off.
    * @param resourceMap     Map containg resources: key=resource id, value=resource name.
    * @return a Data Row representing a work slip.
    */
   public static XComponent createWorkSlipDataRow(OpActivity activity, OpAssignment assignment, boolean progressTracked, HashMap resourceMap) {
      XComponent data_row;
      String choice;
      XComponent data_cell;
      double remainingEffort;
      data_row = new XComponent(XComponent.DATA_ROW);
      // Iterate super-activities and "patch" activity name by adding context
      // (Note: This and the assignments can be optimized using bulk-queries)
      String name = activity.getName();
      StringBuffer activityName = (name != null) ? new StringBuffer(name) : new StringBuffer();
      OpActivity superActivity = activity.getSuperActivity();
      if (superActivity != null) {
         while (superActivity != null) {
            if (superActivity.getID() == activity.getSuperActivity().getID()) {
               activityName.append(" (");
            }
            else {
               activityName.append(" - ");
            }
            name = superActivity.getName();
            activityName.append((name != null) ? name : "");
            superActivity = superActivity.getSuperActivity();
         }
         activityName.append(')');
      }

      choice = XValidator.choice(assignment.locator(), activityName.toString());

      //activity name - 0
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setStringValue(choice);
      data_cell.setEnabled(false);
      data_row.addChild(data_cell);

      // New effort - 1
      data_cell = new XComponent(XComponent.DATA_CELL);
      if (activity.getType() == OpActivity.MILESTONE) {
         data_cell.setValue(null);
         data_cell.setEnabled(false);
      }
      else {
         data_cell.setDoubleValue(0.0);
         data_cell.setEnabled(true);
      }
      data_row.addChild(data_cell);

      // Remaining effort -- default value is current effort minus already booked effort - 2
      data_cell = new XComponent(XComponent.DATA_CELL);
      remainingEffort = assignment.getBaseEffort() - assignment.getActualEffort();
      if (remainingEffort < 0.0d) {
         remainingEffort = 0.0d;
      }
      if (progressTracked && activity.getType() != OpActivity.MILESTONE && activity.getType() != OpActivity.ADHOC_TASK) {
         data_cell.setDoubleValue(remainingEffort);
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // Material costs - 3
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(0.0);
      if (activity.getType() != OpActivity.MILESTONE) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // Travel costs - 4
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(0.0);
      if (activity.getType() != OpActivity.MILESTONE) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // External costs - 5
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(0.0);
      if (activity.getType() != OpActivity.MILESTONE) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // Miscellaneous costs - 6
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(0.0);
      if (activity.getType() != OpActivity.MILESTONE) {
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setEnabled(false);
         data_cell.setValue(null);
      }
      data_row.addChild(data_cell);

      // Optional comment - 7
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(true);
      data_row.addChild(data_cell);

      // Resource id - 8
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setEnabled(false);
      data_cell.setStringValue((String) resourceMap.get(new Long(assignment.getResource().getID())));
      data_row.addChild(data_cell);

      // Original remainig effort (can be changed from the client side) - 9
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(remainingEffort);
      data_cell.setEnabled(false);
      data_row.addChild(data_cell);

      // Completed - 10
      data_cell = new XComponent(XComponent.DATA_CELL);
      if (assignment.getProjectPlan().getProgressTracked() || activity.getType() == OpActivity.ADHOC_TASK) {
         data_cell.setBooleanValue(false);
         data_cell.setEnabled(true);
      }
      else {
         data_cell.setValue(null);
         data_cell.setEnabled(false);
      }
      data_row.addChild(data_cell);

      // Activity type - 11
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setIntValue(activity.getType());
      data_row.addChild(data_cell);

      // Activity created status (newly inerted / edit ) - 12
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setBooleanValue(true);
      data_row.addChild(data_cell);

      // Activity's project name - 13
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setStringValue(activity.getProjectPlan().getProjectNode().getName());
      data_row.addChild(data_cell);

      // Assignment base effort - 14
      data_cell = new XComponent(XComponent.DATA_CELL);
      data_cell.setDoubleValue(assignment.getBaseEffort());
      data_row.addChild(data_cell);

      //set the value of the dataRow to the id of the assignment
      data_row.setStringValue(choice);

      return data_row;
   }
}
