/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpActivityDataSetFactory;
import onepoint.project.modules.project.OpActivityVersion;
import onepoint.project.modules.project.OpAssignmentVersion;
import onepoint.project.modules.project.OpProjectPlanVersion;
import onepoint.project.modules.project.OpWorkPeriodIfc;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationDataSetFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.util.OpProjectCalendar;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

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
         fillResourceRelatedDetails(session, broker, resource, details, startTime, finishTime);
         OpResourceUtilizationDataSetFactory.getInstance().enhanceResourceDetailsMap(session, broker, resource, details, startTime, finishTime);
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
         XComponent assignedCell = ((XComponent) row.getChild(ASSIGNED_INDEX));
         XComponent effortCell = ((XComponent) row.getChild(EFFORT_INDEX));
         totalAssigned += assignedCell.getValue() != null ? assignedCell.getDoubleValue() : 0d;
         totalEffort += effortCell.getValue() != null ? effortCell.getDoubleValue() : 0d;
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
   private void fillResourceRelatedDetails(OpProjectSession session, OpBroker broker, OpResource resource, Map details, long startTime, long finishTime) {
      Iterator<OpAssignmentVersion> assignments = resource.getAssignmentVersions().iterator();
      XComponent dataRow;
      XComponent dataCell;
      OpAssignmentVersion assignment;
      OpActivityVersion activity;
      // ...
      Map<String, Byte> projectManagerAccessMap = new HashMap<String, Byte>();
      
      // fill data set
      while (assignments.hasNext()) {
         assignment = assignments.next();
         activity = assignment.getActivityVersion();
         
         Byte al = projectManagerAccessMap.get(activity
               .getPlanVersion().getProjectPlan().getProjectNode().locator());
         if (al == null) {
            al = session.effectiveAccessLevel(broker, activity
                  .getPlanVersion().getProjectPlan().getProjectNode().getId());
            projectManagerAccessMap.put(activity
               .getPlanVersion().getProjectPlan().getProjectNode().locator(), al);
         }
         boolean isManager = al.byteValue() >= OpPermission.MANAGER;
         if (al.byteValue() < OpPermission.OBSERVER) {
            continue;
         }
         OpProjectPlanVersion pv = isManager
               && activity.getPlanVersion().getProjectPlan()
                     .getWorkingVersion() != null ? activity.getPlanVersion()
               .getProjectPlan().getWorkingVersion() : activity
               .getPlanVersion().getProjectPlan().getLatestVersion();
         if (pv == null || activity.getPlanVersion().getId() != pv.getId()) {
            continue;
         }
         
         if ((activity.getType() == OpActivity.STANDARD || activity.getType() == OpActivity.TASK) && !activity.getTemplate()) {
            Date actFin = activity.getFinish();
            if (actFin == null) {
               SortedMap<Date,OpWorkPeriodIfc> wps = OpActivityDataSetFactory.getSortedWorkPeriodsForActivity(activity);
               actFin = OpActivityDataSetFactory.getLastWorkdayFromWorkPeriods(wps);
            }
            long activityStartTime = activity.getStart().getTime();
            long activityFinishTime = actFin.getTime() + OpProjectCalendar.MILLIS_PER_DAY;
            if ((activityStartTime >= startTime && activityFinishTime <= finishTime)
                 || (activityStartTime < startTime && activityFinishTime <= finishTime && activityFinishTime > startTime)
                 || (activityStartTime >= startTime && activityFinishTime > finishTime && activityStartTime < finishTime)
                 || (activityStartTime < startTime && activityFinishTime > finishTime)) {

               double effortValue = 0;
               double assignedAccumulated = 0;
               if (details.get(activity.locator()) != null) {
                  XComponent row = (XComponent) details.get(activity.locator());
                  //base effort
                  XComponent cell = (XComponent) row.getChild(EFFORT_INDEX);
                  effortValue = cell.getDoubleValue();
                  //assigned
                  cell = (XComponent) row.getChild(ASSIGNED_INDEX);
                  assignedAccumulated = cell.getDoubleValue();
               }

               String locator = activity.locator();
               String name1 = assignment.getPlanVersion().getProjectPlan().getProjectNode().getName();
               String name2 = activity.getName();
               int sequence = activity.getSequence();
               Date start = activity.getStart();
               Date finish = activity.getFinish();
               effortValue += assignment.getBaseEffort();
               double effort = effortValue;
               assignedAccumulated += assignment.getAssigned();
               double assigned = assignedAccumulated;
               Integer probability = assignment.getPlanVersion().getProjectPlan().getProjectNode().getProbability();
               
               dataRow = OpResourceUtilizationDataSetFactory.getInstance().createUtilizationDetailsDataRow(name1, name2,
                     sequence, start, finish, new Double(effort), new Double(assigned), new Integer(probability));

               details.put(assignment.getActivityVersion().locator(), dataRow);
            }
         }
      }
   }

}
