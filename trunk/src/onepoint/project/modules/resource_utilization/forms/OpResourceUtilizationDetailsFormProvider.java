/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.resource.OpResource;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

/**
 * Form provider for utilization details
 *
 * @author ovidiu.lupas
 */
public class OpResourceUtilizationDetailsFormProvider implements XFormProvider {

   // form properties
   private static final String UTILIZATION_DATA_SET = "UtilizationDataSet";

   // needed parameters
   private static final String RESOURCE_ID = "ResourceId";

   // time interval and the index of the start/finish element in the interval list 
   private static final String DETAILS_TIME_INTERVAL = "DetailsTimeInterval";
   private static final int START_INDEX = 0;
   private static final int FINISH_INDEX = 1;

   private final static String RESOURCE_MAP = "resource_utilization.details";
   private final static String TOTAL = "Total";
   private final static int EFFORT_INDEX = 5;
   private final static int ASSIGNED_INDEX = 6;

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {

      String locatorString = (String) parameters.get(RESOURCE_ID);
      List detailsTimeInterval = (List) parameters.get(DETAILS_TIME_INTERVAL);

      // boundaries of the time interval
      long startTime = ((Date) detailsTimeInterval.get(START_INDEX)).getTime();
      long finishTime = ((Date) detailsTimeInterval.get(FINISH_INDEX)).getTime();

      OpProjectSession session = (OpProjectSession) s;
      XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(RESOURCE_MAP);
      // the utilization details data set
      XComponent utilizationDataSet = form.findComponent(UTILIZATION_DATA_SET);
      String totalName = resourceMap.getResource(TOTAL).getText();
      OpBroker broker = session.newBroker();
      try {
         // get the resource with locatorString
         OpLocator locator = OpLocator.parseLocator(locatorString);
         Class instanceClass = locator.getPrototype().getInstanceClass();
         Map details;

         OpResource resource = (OpResource) broker.getObject(locatorString);
         details = new HashMap();
         fillResourceRelatedDetails(resource, details, startTime, finishTime);
         fillDataSet(utilizationDataSet, details, totalName);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Fill the given data set with the detail rows taken from the given details map. Calculates also the total effort/assign
    *
    * @param utilizationDataSet data set to be filled up with the details
    * @param details            map containing the utilization details
    * @param totalName          display text for total row
    */
   private void fillDataSet(XComponent utilizationDataSet, Map details, String totalName) {
      Set keys = details.keySet();
      double totalEffort = 0;
      double totalAssigned = 0;

      for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
         String name = (String) iterator.next();
         XComponent row = (XComponent) details.get(name);
         utilizationDataSet.addChild(row);
         totalAssigned += ((XComponent) row.getChild(ASSIGNED_INDEX)).getDoubleValue();
         totalEffort += ((XComponent) row.getChild(EFFORT_INDEX)).getDoubleValue();
      }

      if (keys.size() > 1) {
         XComponent totalRow = new XComponent(XComponent.DATA_ROW);
         // project name
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         totalRow.addChild(dataCell);
         // activity name
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(totalName);
         totalRow.addChild(dataCell);
         // activity sequence
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         totalRow.addChild(dataCell);
         // activity start date
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         totalRow.addChild(dataCell);
         // activity finish date
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         totalRow.addChild(dataCell);
         // assignment base effort
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(totalEffort);
         totalRow.addChild(dataCell);
         // assignment % assigned
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(totalAssigned);
         totalRow.addChild(dataCell);
         // probability %
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setEnabled(false);
         totalRow.addChild(dataCell);
         details.put(totalName, totalRow);
         utilizationDataSet.addChild((XComponent) details.get(totalName));
      }
   }

   /**
    * Fills the given map with resource details. The effort/assigned will be added up for the same activity locator
    * (same entry in the details map)
    *
    * @param resource   resource to get the details for
    * @param details    Map that will hold the detail rows (key:activity.locator, value:detail row)
    * @param startTime  start interval for details
    * @param finishTime end interval for details
    */
   private void fillResourceRelatedDetails(OpResource resource, Map details, long startTime, long finishTime) {
      Iterator assignments = resource.getActivityAssignments().iterator();
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

               double effortValue = 0;
               double assigned = 0;
               if (details.get(activity.locator()) != null) {
                  XComponent row = (XComponent) details.get(activity.locator());
                  //base effort
                  XComponent cell = (XComponent) row.getChild(EFFORT_INDEX);
                  effortValue = cell.getDoubleValue();
                  //assigned
                  cell = (XComponent) row.getChild(ASSIGNED_INDEX);
                  assigned = cell.getDoubleValue();
               }

               dataRow = new XComponent(XComponent.DATA_ROW);

               // project name 0
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setStringValue(assignment.getProjectPlan().getProjectNode().getName());
               dataRow.addChild(dataCell);

               // activity name 1
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setStringValue(assignment.getActivity().getName());
               dataRow.addChild(dataCell);

               // activity sequence 2
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setIntValue(assignment.getActivity().getSequence());
               dataRow.addChild(dataCell);

               // activity start date 3
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setDateValue(assignment.getActivity().getStart());
               dataRow.addChild(dataCell);

               // activity finish date 4
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setDateValue(assignment.getActivity().getFinish());
               dataRow.addChild(dataCell);

               // assignment base effort 5
               dataCell = new XComponent(XComponent.DATA_CELL);
               effortValue += assignment.getBaseEffort();
               dataCell.setDoubleValue(effortValue);
               dataRow.addChild(dataCell);

               // assignment % assigned 6
               dataCell = new XComponent(XComponent.DATA_CELL);
               assigned += assignment.getAssigned();
               dataCell.setDoubleValue(assigned);
               dataRow.addChild(dataCell);

               // project probability % 7
               dataCell = new XComponent(XComponent.DATA_CELL);
               dataCell.setDoubleValue(assignment.getProjectPlan().getProjectNode().getProbability());
               dataRow.addChild(dataCell);

               details.put(activity.locator(), dataRow);
            }
         }
      }
   }
}
