/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.resource.OpResource;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Form provider for utilization details
 * 
 * @author ovidiu.lupas
 */
public class OpResourceUtilizationDetailsFormProvider implements XFormProvider {

   /* form properties */
   public static final String UTILIZATION_DATA_SET = "UtilizationDataSet";
   /* needed parameters */
   public static final String RESOURCE_ID = "ResourceId";
   public static final String DETAILS_TIME_INTERVAL = "DetailsTimeInterval";
   /* index of the start/finish element in the interval list */
   private static final int START_INDEX = 0;
   private static final int FINISH_INDEX = 1;

   private final static String RESOURCE_MAP = "resource_utilization.details";
   private final static String TOTAL = "Total";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      String resourceId = (String) parameters.get(RESOURCE_ID);
      List detailsTimeInterval = (List) parameters.get(DETAILS_TIME_INTERVAL);
      /* boundaries of the time interval */
      long startTime = ((Date) detailsTimeInterval.get(START_INDEX)).getTime();
      long finishTime = ((Date) detailsTimeInterval.get(FINISH_INDEX)).getTime();

      OpProjectSession session = (OpProjectSession) s;

      XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(RESOURCE_MAP);

      OpBroker broker = session.newBroker();
      // get the resource with resourceId
      OpResource resource = (OpResource) broker.getObject(resourceId);
      Iterator assignments = resource.getActivityAssignments().iterator();
      // the utilization details data set
      XComponent utilizationDataSet = form.findComponent(UTILIZATION_DATA_SET);

      double totalEffort = 0.0d;
      double totalAssigned = 0.0d;

      XComponent dataRow;
      XComponent dataCell;
      OpAssignment assignment;
      OpActivity activity;
      // fill data set
      while (assignments.hasNext()) {
         assignment = (OpAssignment) assignments.next();
         activity = assignment.getActivity();
         if (activity.getType() == OpActivity.STANDARD && !activity.getTemplate()) {
            long activityStartTime = activity.getStart().getTime();
            long activityFinishTime = activity.getFinish().getTime() + XCalendar.MILLIS_PER_DAY;
            if ((activityStartTime >= startTime && activityFinishTime <= finishTime)
                  || (activityStartTime < startTime && activityFinishTime <= finishTime && activityFinishTime > startTime)
                  || (activityStartTime >= startTime && activityFinishTime > finishTime && activityStartTime < finishTime)
                  || (activityStartTime < startTime && activityFinishTime > finishTime)) {

               dataRow = new XComponent(XComponent.DATA_ROW);
               // project name
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setStringValue(assignment.getProjectPlan().getProjectNode().getName());
               dataRow.addChild(dataCell);
               // activity name
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setStringValue(assignment.getActivity().getName());
               dataRow.addChild(dataCell);
               // activity sequence
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setIntValue(assignment.getActivity().getSequence());
               dataRow.addChild(dataCell);
               // activity start date
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setDateValue(assignment.getActivity().getStart());
               dataRow.addChild(dataCell);
               // activity finish date
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setDateValue(assignment.getActivity().getFinish());
               dataRow.addChild(dataCell);
               // assignment base effort
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setDoubleValue(assignment.getBaseEffort());
               dataRow.addChild(dataCell);
               // assignment % assigned
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setDoubleValue(assignment.getAssigned());
               dataRow.addChild(dataCell);
               utilizationDataSet.addChild(dataRow);

               // Add to totals
               totalEffort += assignment.getBaseEffort();
               totalAssigned += assignment.getAssigned();

            }
         }
      }

      // Add total (effort and percentages) if more than a single assignment
      if (utilizationDataSet.getChildCount() > 1) {
         dataRow = new XComponent(XComponent.DATA_ROW);
         // project name
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         dataRow.addChild(dataCell);
         // activity name
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(resourceMap.getResource(TOTAL).getText());
         dataRow.addChild(dataCell);
         // activity sequence
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         dataRow.addChild(dataCell);
         // activity start date
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         dataRow.addChild(dataCell);
         // activity finish date
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         dataRow.addChild(dataCell);
         // assignment base effort
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(totalEffort);
         dataRow.addChild(dataCell);
         // assignment % assigned
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(totalAssigned);
         dataRow.addChild(dataCell);
         utilizationDataSet.addChild(dataRow);
      }

      broker.close();
   }

}
