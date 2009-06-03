/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource_utilization.forms;

import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource_utilization.OpResourceUtilizationDataSetFactory;
import onepoint.project.modules.user.OpPermission;
import onepoint.resource.XLanguageResourceMap;
import onepoint.service.server.XSession;

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
         fillPoolDetails(session, broker, resources, utilizationDataSet, totalName, startTime, finishTime);
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
   private void fillPoolDetails(OpProjectSession session, OpBroker broker, Set<OpResource> resources, XComponent utilizationDataSet, String totalName, long startTime, long finishTime) {

      OpQuery assignmentsForDateQuery = broker.newQuery("select ass from OpAssignment as ass" +
      		" where" +
      		" ass.Activity.Start < :finish and ass.Activity.Finish >= :start and" +
      		" ass.Resource.id in (:resourceIds)" +
      		" order by ass.Resource.id");
      
      Date startDate = new Date(startTime);
      assignmentsForDateQuery.setDate("start", startDate);
      Date finishDate = new Date(finishTime);
      assignmentsForDateQuery.setDate("finish", finishDate);
      
      Set<Long> resIDs = new HashSet<Long>(resources.size());
      for (OpResource res: resources) {
         resIDs.add(new Long(res.getId()));
      }
      Set<Long> filteredIds = session.accessibleIds(broker, resIDs, OpPermission.OBSERVER);
      assignmentsForDateQuery.setCollection("resourceIds", filteredIds);
      
      SortedMap<String, XComponent> detailsRows = new TreeMap(new XComponent.ChoiceComparator());
      
      OpResource currentRes = null;
      boolean finished = false;
      double totalEffort = 0d;
      double resEffort = 0d;
      double resAssigned = 0d;
      Iterator<OpAssignment> ait = broker.iterate(assignmentsForDateQuery);
      while (ait.hasNext() || !finished) {
         OpAssignment ass = ait.hasNext() ? ait.next() : null;
         if (ass == null || (currentRes != null && currentRes.getId() != ass.getResource().getId())) {
            if (currentRes != null) {
               XComponent row =  OpResourceUtilizationDataSetFactory.getInstance().newPoolUtilizationRow(currentRes, resEffort, resAssigned);
               detailsRows.put(row.getStringValue(), row);
               totalEffort += resEffort;
            }
            resEffort = 0d;
            resAssigned = 0d;
            finished = ass == null;
         }
         if (ass != null) { 
            resEffort += ass.getBaseEffort();
            resAssigned += ass.getAssigned();
            currentRes = ass.getResource();
         }
      }

      OpResourceUtilizationDataSetFactory.getInstance().enhancePoolDetailsMap(session, broker, resources, detailsRows, startDate, finishDate);
      
      for (Map.Entry<String, XComponent> e: detailsRows.entrySet()) {
         utilizationDataSet.addChild(e.getValue());
      }
   }

}
