/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Form provider for pool utilization details
 *
 * @author mihai.costin
 */
public class OpPoolUtilizationDetailsFormProvider implements XFormProvider {

   // utilization data set
   private static final String UTILIZATION_DATA_SET = "UtilizationDataSet";
   // pool id
   private static final String RESOURCE_ID = "ResourceId";

   // time interval and the index of the start/finish element in the interval list
   private static final String DETAILS_TIME_INTERVAL = "DetailsTimeInterval";
   private static final int START_INDEX = 0;
   private static final int FINISH_INDEX = 1;

   private final static String POOL_UTILIZATION_MAP = "pool_utilization.details";
   private final static String TOTAL = "Total";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession,onepoint.express.XComponent,java.util.HashMap)
    */
   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      String locatorString = (String) parameters.get(RESOURCE_ID);
      // boundaries of the time interval
      List detailsTimeInterval = (List) parameters.get(DETAILS_TIME_INTERVAL);
      long startTime = ((java.sql.Date) detailsTimeInterval.get(START_INDEX)).getTime();
      long finishTime = ((java.sql.Date) detailsTimeInterval.get(FINISH_INDEX)).getTime();

      OpProjectSession session = (OpProjectSession) s;
      XLanguageResourceMap resourceMap = session.getLocale().getResourceMap(POOL_UTILIZATION_MAP);
      XComponent utilizationDataSet = form.findComponent(UTILIZATION_DATA_SET);
      String totalName = resourceMap.getResource(TOTAL).getText();
      OpBroker broker = session.newBroker();
      try {
         OpResourcePool resourcePool = (OpResourcePool) broker.getObject(locatorString);

         Set resources = getAllResources(resourcePool);
         fillPoolDetails(resources, utilizationDataSet, totalName, startTime, finishTime);
      }
      finally {
         broker.close();
      }
   }

   /**
    * Colects all the resources recursively from the given pool and sub-pools.
    *
    * @param resourcePool Pool to get the resources for
    * @return Set with all the found resources
    */
   private Set getAllResources(OpResourcePool resourcePool) {
      Set resources = resourcePool.getResources();
      Set subPools = resourcePool.getSubPools();
      for (Iterator iterator = subPools.iterator(); iterator.hasNext();) {
         OpResourcePool pool = (OpResourcePool) iterator.next();
         resources.addAll(getAllResources(pool));
      }
      return resources;
   }

   /**
    * Fills the utilization detail form with the pool related information.
    *
    * @param resources          Resources found in the pool
    * @param utilizationDataSet Data set that has to be filled up
    * @param totalName          String to use as total name column
    * @param startTime          Time interval start
    * @param finishTime         Time interval end.
    */
   private void fillPoolDetails(Set resources, XComponent utilizationDataSet, String totalName, long startTime, long finishTime) {

      double totalEffort = 0;
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         OpResource resource = (OpResource) iterator.next();
         String name = resource.getName();
         double assigned = 0;
         double effort = 0;
         Set assignments = resource.getActivityAssignments();
         for (Iterator iterator1 = assignments.iterator(); iterator1.hasNext();) {
            OpAssignment assignment = (OpAssignment) iterator1.next();
            OpActivity activity = assignment.getActivity();
            if (activity.getType() == OpActivity.STANDARD && !activity.getTemplate() && !activity.getDeleted()) {
               if (activity.getFinish().getTime() >= startTime && activity.getStart().getTime() < finishTime) {
                  assigned += assignment.getAssigned();
                  effort += assignment.getBaseEffort();
               }
            }
         }

         if (assigned > 0) {
            addUtilizationRow(utilizationDataSet, name, effort, assigned);
            totalEffort += effort;
         }
      }

      //add TOTAL line
      if (resources.size() > 0) {
         addUtilizationRow(utilizationDataSet, totalName, totalEffort, Double.MAX_VALUE);
      }

   }

   /**
    * Adds new information in the utilization set.
    *
    * @param utilizationDataSet Utilization data set.
    * @param name               Name column value
    * @param effort             Effort column value
    * @param assigned           Assigned column value
    */
   private void addUtilizationRow(XComponent utilizationDataSet, String name, double effort, double assigned) {
      XComponent row = new XComponent(XComponent.DATA_ROW);
      XComponent cell;

      //name
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setStringValue(name);
      row.addChild(cell);

      //effort
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(effort);
      row.addChild(cell);

      //sum(% assigned)
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(assigned);
      row.addChild(cell);

      utilizationDataSet.addChild(row);
   }

}
