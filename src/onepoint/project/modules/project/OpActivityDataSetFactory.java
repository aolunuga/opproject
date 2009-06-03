/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import onepoint.express.XComponent;
import onepoint.express.XValidationException;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;
import onepoint.project.util.OpBulkFetchIterator;
import onepoint.project.util.OpCollectionSynchronizationHelper;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.util.OpBulkFetchIterator.LongIdConverter;
import onepoint.project.validators.OpProjectValidator;
import onepoint.service.server.XServiceManager;
import onepoint.util.XCalendar;

public class OpActivityDataSetFactory {

   /**
    * 
    */
   private static final int ACTIVITY_STEP_SIZE = 200;
   /**
    * 
    */
   public static final int MAX_FILTER_SIZE = 999;
   private static final XLog logger = XLogFactory.getLogger(OpActivityDataSetFactory.class);

   private static final String GET_WORK_RECORD_COUNT_FOR_ASSIGNMENT =
        "select count(workRecord.id) from OpWorkRecord workRecord where workRecord.Assignment = (:assignmentId)";
   private static final String GET_COMPLETED_WORK_RECORD_COUNT_FOR_ASSIGNMENT =
      "select count(workRecord.id) from OpWorkRecord workRecord where workRecord.Assignment = (:assignmentId) and workRecord.Completed = true";
   private static final String GET_HOURLY_RATES_PERIOD_COUNT_FOR_PROJECT_ASSIGNMENT =
        "select count(hourlyRates.id) from OpHourlyRatesPeriod hourlyRates where hourlyRates.ProjectNodeAssignment = (:assignmentId)";
   private static final String GET_SUBACTIVITIES_COUNT_FOR_ACTIVITY =
        "select count(activity.id) from OpActivity activity where activity.SuperActivity = (:activityId) and activity.Deleted = false";

   private static OpActivityDataSetFactory instance = new OpActivityDataSetFactory();

   /**
    * Returns an instance of the OpProjectPlanningService
    * 
    * @return an instance of the OpProjectPlanningService
    */
   public static void register(OpActivityDataSetFactory dataSetFactory) {
      instance = dataSetFactory;
   }

   /**
    * Returns an instance of the data set factory
    * 
    * @return an instance of the data set factory
    */
   public static OpActivityDataSetFactory getInstance() {
      return instance;
   }

   public static HashMap resourceMap(OpBroker broker, OpProjectNode projectNode) {
      OpQuery query = broker
           .newQuery("select assignment.Resource from OpProjectNodeAssignment as assignment where assignment.ProjectNode.id = ? order by assignment.Resource.Name asc");
      query.setLong(0, projectNode.getId());
      Iterator resources = broker.iterate(query);
      //LinkedHashMap to maintain the order in which entries are added
      HashMap resourceMap = new LinkedHashMap();
      OpResource resource = null;
      while (resources.hasNext()) {
         resource = (OpResource) (resources.next());
         resourceMap.put(new Long(resource.getId()), resource);
      }
      return resourceMap;
   }

   /**
    * Fills the form data set with the resource hourly rates.
    * Each row has the resource locator as value set on it and a data cell with a map containing
    * the interval start date as key and a list with internal and external rates as value.
    *
    * @param project The current project.
    * @param dataSet Hourly rates data set.
    */
   //<FIXME author="Haizea Florin" description="This is not the proper way to use this method">
   public static synchronized  void fillHourlyRatesDataSet(OpProjectNode project, XComponent dataSet) {
      OpProjectAdministrationService service = (OpProjectAdministrationService) XServiceManager.getService(OpProjectAdministrationService.SERVICE_NAME);
      service.fillHourlyRatesDataSet(project, dataSet);
   }
   //<FIXME>


   public static String [][] RESOURCE_DATASET_DESCRIPTION = {
      {"resourceChoice", ""},
      }; 
   
   public void retrieveResourceDataSet(HashMap resourceMap, XComponent dataSet) {
      Iterator resources = resourceMap.values().iterator();
      OpResource resource = null;
      XComponent dataRow = null;
      XComponent dataCell = null;
      while (resources.hasNext()) {
         resource = (OpResource) (resources.next());
         dataRow = createResourceRow(resource);
         dataSet.addChild(dataRow);
      }
   }
   
   /**
    * @param resource
    * @return
    * @pre
    * @post
    */
   public static XComponent createResourceRow(OpResource resource) {
      XComponent dataRow;
      XComponent dataCell;
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(resource.getAvailable());
      dataRow.addChild(dataCell);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(resource.getHourlyRate());
      dataRow.addChild(dataCell);
      return dataRow;
   }

   public static int BULK_FETCH_SIZE = 100;

   public void retrieveResourceDataSet(OpBroker broker, OpProjectNode projectNode, XComponent dataSet) {
      retrieveResourceDataSet(resourceMap(broker, projectNode), dataSet);
   }

   public static void addWorkPhasesFromWorkPeriods(OpActivityIfc a, Set<? extends OpWorkPeriodIfc> wps, XComponent dataRow) {
      SortedMap<Date, OpWorkPeriodIfc> sortedWPs = getSortedWorkPeriods(wps);
      
      
      Date wphStart = null;
      double wphEffort = 0d;
      
      Date currentDate = a.getStart();
      currentDate = currentDate != null ? currentDate : OpActivityDataSetFactory
            .getFirstWorkdayFromWorkPeriods(sortedWPs);
      Date lastWorkdayFromWorkPeriods = OpActivityDataSetFactory
            .getLastWorkdayFromWorkPeriods(sortedWPs);
      Date endDate = new Date(
            (lastWorkdayFromWorkPeriods != null ? lastWorkdayFromWorkPeriods
                  .getTime() : currentDate.getTime())
                  + XCalendar.MILLIS_PER_DAY);
      // Date endDate = new Date (a.getFinish().getTime() + XCalendar.MILLIS_PER_DAY);
      while (!currentDate.after(endDate)) {
         OpWorkPeriodIfc wp = getWorkPeriodForWorkDay(currentDate, sortedWPs);
         if (wp != null && wphStart == null) {
            // new phase:
            wphStart = currentDate;
         }
         if (wp != null) {
            double dailyEffort = wp.getBaseEffort() / wp.countWorkDays();
            wphEffort += dailyEffort;
         }
         if ((wp == null || !currentDate.before(endDate)) && wphStart != null) {
            // end Of WorkPhase:
            Map wpDesc = OpGanttValidator.createWorkPhaseDesc(wphStart, currentDate, wphEffort);
            OpGanttValidator.addWorkPhase(dataRow, wpDesc);
            wphStart = null;
            wphEffort = 0d;
         }
         currentDate = new Date(currentDate.getTime() + XCalendar.MILLIS_PER_DAY);
      }
   }
   
   public static Date getFirstWorkdayFromWorkPeriods(
         SortedMap<Date, OpWorkPeriodIfc> sortedWPs) {
      return getFringeWorkdayFromWorkPeriods(sortedWPs, true);
   }
   
   public static Date getLastWorkdayFromWorkPeriods(
         SortedMap<Date, OpWorkPeriodIfc> sortedWPs) {
      return getFringeWorkdayFromWorkPeriods(sortedWPs, false);
   }
   
   public static Date getFringeWorkdayFromWorkPeriods(
         SortedMap<Date, OpWorkPeriodIfc> sortedWPs, boolean front) {
      if (sortedWPs == null || sortedWPs.isEmpty()) {
         return null; // no periods, no date...
      }
      SortedSet<Date> wpKeys = new TreeSet<Date>(
            front ? (new Comparator<Date>() {
               public int compare(Date o1, Date o2) {
                  return o1.compareTo(o2); // default, kept for visibility
               }
            }) : (new Comparator<Date>() {
               public int compare(Date o1, Date o2) {
                  return o2.compareTo(o1); // reversed...
               }
            }));
      wpKeys.addAll(sortedWPs.keySet());
      Iterator<Date> dit = wpKeys.iterator();
      while (dit.hasNext()) {
         Date periodKey = dit.next();
         OpWorkPeriodIfc lastPeriod = sortedWPs.get(periodKey);
         long wds = lastPeriod.getWorkingDays();
         int days = front ? 0 : -1;
         while (front ? (wds & 1) == 0 : (wds > 0)) {
            wds = wds >> 1;
            days++;
         }
         if (front ? (wds & 1) == 1 : days != -1) {
            return new Date(lastPeriod.getStart().getTime() + days * XCalendar.MILLIS_PER_DAY);
         }
      }
      return null;
   }
   
   public static Date getFinishDateFromWorkPeriods(
         SortedMap<Date, OpWorkPeriodIfc> sortedWorkPeriods, Date startDate) {
      Date lastWorkdayFromWorkPeriods = OpActivityDataSetFactory
            .getLastWorkdayFromWorkPeriods(sortedWorkPeriods);
      Date finish = (lastWorkdayFromWorkPeriods != null ? lastWorkdayFromWorkPeriods
            : startDate);
      return finish;
   }

   public void retrieveFilteredActivityDataSet(OpProjectSession session,
         OpBroker broker, OpActivityFilter filter, OpObjectOrderCriteria order,
         XComponent dataSet) {
      retrieveFilteredActivityDataSet(session, broker, filter, order, dataSet, 0, 0, false);
   }

   public void retrieveFilteredActivityDataSet(OpProjectSession session,
         OpBroker broker, OpActivityFilter filter, OpObjectOrderCriteria order,
         XComponent dataSet, int attributesToSet,
         int attributesToRemove, boolean editable) {

      // Note: The filtered activity data set contains an additional column containing the project locator

      // Attention: We have to use an activity-row map, because direct-access using sequence not possible
      // (Potential filtered and reordered activity set across multiple projects)

      long now = System.currentTimeMillis();
      ArrayList projectPlanIds = new ArrayList();
      if (filter.getProjectNodeIds().size() > 0) {
         // Pre-fetch project plans and project names (performance: Adding project column)
         OpQuery query = broker.newQuery("" +
               "select planVersion, projectPlan, projectNode " +
               "from OpProjectPlan as projectPlan " +
               "inner join projectPlan.LatestVersion as planVersion " +
               "inner join projectPlan.ProjectNode as projectNode " +
               "where projectNode.id in (:projectNodeIds)");
         query.setCollection("projectNodeIds", filter.getProjectNodeIds());
         Iterator result = broker.iterate(query);
         Object[] record = null;
         while (result.hasNext()) {
            record = (Object[]) result.next();
            projectPlanIds.add(new Long(((OpProjectPlan) record[1]).getId()));
         }
      }

      // Construct query string and arguments depending on filter and sort order
      // Construct query string and arguments depending on filter and sort order
      StringBuffer fromBuffer = new StringBuffer(" OpActivityVersion as activityVersion ");
      StringBuffer whereBuffer = new StringBuffer();
      ArrayList argumentNames = new ArrayList();
      ArrayList argumentValues = new ArrayList();

      whereBuffer.append("activityVersion.PlanVersion = activityVersion.PlanVersion.ProjectPlan.LatestVersion");
      if (projectPlanIds.size() > 0) {
         whereBuffer.append(" and activityVersion.PlanVersion.ProjectPlan.id in (:projectPlanIds)");
         argumentNames.add("projectPlanIds");
         argumentValues.add(projectPlanIds);
      }

      if (filter.getResourceIds().size() > 0) {
//         if (whereBuffer.length() > 0) {
//            whereBuffer.append(" and ");
//         }
         fromBuffer.append(" inner join activityVersion.AssignmentVersions as assignmentVersion");
         whereBuffer.append(" and assignmentVersion.Resource.id in (:resourceIds)");

         if (filter.getAssignmentComplete() != null) {
            if (filter.getAssignmentComplete().booleanValue()) {
               whereBuffer.append(" and assignmentVersion.Assignment.Complete = 100");
            }
            else {
               whereBuffer.append(" and assignmentVersion.Assignment.Complete < 100");
            }
         }

         argumentNames.add("resourceIds");
         argumentValues.add(filter.getResourceIds());
      }

      if (filter.getTypes().size() > 0) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and ");
         }
         whereBuffer.append("activityVersion.Type in (:types)");
         argumentNames.add("types");
         argumentValues.add(filter.getTypes());
      }

      if (filter.getMaxOutlineLevel() != OpActivityFilter.ALL) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and ");
         }
         whereBuffer.append("activityVersion.OutlineLevel <= :outlineLevel");
         argumentNames.add("outlineLevel");
         argumentValues.add(new Integer(filter.getMaxOutlineLevel()));
      }

      if (filter.getStartFrom() != null) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and (");
         }
         whereBuffer.append("activityVersion.Start >= :startFrom");
         argumentNames.add("startFrom");
         argumentValues.add(filter.getStartFrom());
      }

      if (filter.getStartTo() != null) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and ");
         }
         if (filter.getStartFrom() == null) {
            whereBuffer.append('(');
         }
         whereBuffer.append("activityVersion.Start <= :startTo");
         argumentNames.add("startTo");
         argumentValues.add(filter.getStartTo());
      }

      // Ensure that tasks are always returned as well
      if ((filter.getStartFrom() != null) || (filter.getStartTo() != null)) {
         whereBuffer.append(" or activityVersion.Start is null)");
      }

      if (filter.getCompleted() != null) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and ");
         }
         if (filter.getCompleted().booleanValue()) {
            whereBuffer.append("activityVersion.Activity.Complete = 100");
         }
         else {
            whereBuffer.append("activityVersion.Activity.Complete < 100");
         }
      }
      
      // Always only select not-deleted activities
      if (whereBuffer.length() > 0) {
         whereBuffer.append(" and ");
      }
      whereBuffer.append(" activityVersion.Template = :template ");
      argumentNames.add("template");
      argumentValues.add(Boolean.valueOf(filter.getTemplates()));

      String sequenceWhereClause = null;
      if (filter.isExportedOnly()) {
         sequenceWhereClause = whereBuffer.toString();
         whereBuffer.append(" and ");
         whereBuffer.append(" activityVersion.PublicActivity = true ");
      }
      
      StringBuffer queryBuffer = new StringBuffer("select activityVersion.id from ");
      
      queryBuffer.append(fromBuffer);
      queryBuffer.append(" where ");
      queryBuffer.append(whereBuffer);
      
      Map<Integer, Map<Long, Byte>> sequenceMap = new HashMap<Integer, Map<Long, Byte>>();
      if (sequenceWhereClause != null) {
         String sequenceMapQuery = "" +
               "select" +
               " activityVersion.PlanVersion.id, " +
               " activityVersion.Sequence," +
               " activityVersion.OutlineLevel from "
               + fromBuffer + " where " + sequenceWhereClause;
         OpQuery seqQuery = broker.newQuery(sequenceMapQuery);
         setQueryParameters(argumentNames, argumentValues, seqQuery);
         Iterator<Object[]> seqQIt = broker.iterate(seqQuery);
         while (seqQIt.hasNext()) {
            Object[] res = seqQIt.next();
            Long actVId = (Long)(res[0]);
            Integer actVSeq = (Integer)(res[1]);
            Byte outlineLevel = (Byte)(res[2]);
            Map<Long, Byte> actVWithSeq = sequenceMap.get(actVSeq);
            if (actVWithSeq == null) {
               actVWithSeq = new HashMap<Long, Byte>();
               sequenceMap.put(actVSeq, actVWithSeq);
            }
            actVWithSeq.put(actVId, outlineLevel);
         }
      }

      if (order != null) {
         queryBuffer.append(order.toHibernateQueryString("activityVersion"));
      }
      
      OpQuery query = broker.newQuery(queryBuffer.toString());
      // Note: We expect collections, booleans, dates and doubles
      setQueryParameters(argumentNames, argumentValues, query);

      HashSet activityIds = new HashSet();
      Iterator qIt = broker.iterate(query);
      
      List<Long> allIds = new ArrayList<Long>();
      while (qIt.hasNext()) {
         allIds.add((Long) qIt.next());
      }
      
      Map<String, OpProjectPlan> activityVersionLocatorToPlanMap = new HashMap<String, OpProjectPlan>();
      
      Set<OpActivityIfc> activityVersions = new HashSet<OpActivityIfc>();
      Map<String, XComponent> activityRowMap = new HashMap<String, XComponent>();
      StringBuffer nameBuffer = null;
      logger.debug("TIMING: retrieveFilteredActivityDataSet #00: " + (System.currentTimeMillis() - now));
      Iterator<Long> memIt = allIds.iterator();
      Set<Long> idsCollected = new HashSet<Long>();
      SortedSet<OpActivityIfc> data = new TreeSet<OpActivityIfc>(order);
      while (memIt.hasNext() || !idsCollected.isEmpty()) {
         Long actID = null;
         if (memIt.hasNext()) {
            actID = (Long) memIt.next();
            idsCollected.add(actID);
         }
         if (idsCollected.size() == BULK_FETCH_SIZE || actID == null) {
            addActivityVersionDataRows(broker, filter, dataSet, attributesToSet,
                  attributesToRemove, editable, activityIds,
                  activityVersionLocatorToPlanMap, activityVersions,
                  activityRowMap, idsCollected, data);
            idsCollected.clear();
         }
      }
      
      // no adhoc tasks will be exported...
      if (!filter.isExportedOnly()) {
         query = createAdhocTaskQuery(broker, filter, order);
         qIt = broker.iterate(query);
         
         List<Long> adHocIds = new ArrayList<Long>();
         while (qIt.hasNext()) {
            adHocIds.add((Long) qIt.next());
         }
   
         memIt = adHocIds.iterator();
         while (memIt.hasNext() || !idsCollected.isEmpty()) {
            Long actID = null;
            if (memIt.hasNext()) {
               actID = (Long) memIt.next();
               idsCollected.add(actID);
            }
            if (idsCollected.size() == BULK_FETCH_SIZE || actID == null) {
               addActivityDataRows(broker, filter, dataSet, attributesToSet,
                     attributesToRemove, editable, activityIds,
                     activityVersionLocatorToPlanMap, activityVersions,
                     activityRowMap, idsCollected, data);
               idsCollected.clear();
            }
         }
      }
      Stack<Integer> outlineLevels = new Stack<Integer>();
      int sequence = 0;
      int outlineLevel = 0;
      long planVersionId = 0;
      for (OpActivityIfc activity : data) {
         if (filter.doNotFlatten()) {
            long currentPlanVersionId = 0;
            if (activity instanceof OpActivityVersion) {
               currentPlanVersionId = ((OpActivityVersion)activity).getPlanVersion().getId();
            }
            if (planVersionId != currentPlanVersionId) {
               outlineLevels.clear();
               planVersionId = currentPlanVersionId;
            }
            Long pviL = new Long(currentPlanVersionId);
            while (activity.getSequence() >= sequence) {
               Integer seqI = new Integer(sequence);
               Map<Long, Byte> olMap = sequenceMap.get(seqI);
               if (olMap != null) {
                  Byte ol = olMap.get(pviL);
                  outlineLevel = adjustOutlineLevel(outlineLevels,
                        (ol != null ? ol.intValue() : 0), activity
                              .getSequence() == sequence);
               }
               sequence++;
            }
         }

         addDataRow(dataSet, attributesToSet, attributesToRemove, editable,
               activityIds, activityVersionLocatorToPlanMap, activityVersions,
               activityRowMap, outlineLevel, !filter.doNotFlatten(), activity,
               activity.getId());
      }
      
      logger.debug("TIMING: retrieveFilteredActivityDataSet #01: " + (System.currentTimeMillis() - now));
         
      /* no activities found*/
      if (activityIds.isEmpty()) {
         return;
      }
      List activityIdList = new ArrayList(activityIds);      // Assignments: Fill resources and resource base efforts columns
      boolean postFilter = false;
      int endPos = 0;
      Map resourceAvailability = new HashMap();
      while (endPos < activityIdList.size()) {
         if (filter.getResourceIds().size() > 0) {
            if (filter.getResourceIds().size() > MAX_FILTER_SIZE) { // only 1000 elements on oracle
               query = broker.newQuery("select assignment, resource, activity from OpAssignment as assignment" +
                     " join assignment.Resource as resource" +
                     " join assignment.Activity as activity" +
               " where activity.id in (:activityIds)");
               postFilter = true;
            }
            else {
               query = broker.newQuery("select assignment, resource, activity from OpAssignment as assignment" +
                     " join assignment.Resource resource" +
                     " join assignment.Activity activity" +
                     " where resource.id in (:resourceIds)" +
               " and activity.id in (:activityIds)");
               query.setCollection("resourceIds", filter.getResourceIds());
            }
         }
         else {
            query = broker.newQuery("select assignment, resource, activity from OpAssignment as assignment" +
                  " join assignment.Resource as resource" +
                  " join assignment.Activity as activity" +
            " where activity.id in (:activityIds)");
         }
         int startPos = endPos;
         endPos = startPos+Math.min(ACTIVITY_STEP_SIZE, activityIdList.size()-startPos);
         List activityIdsSubset = activityIdList.subList(startPos, endPos);
         query.setCollection("activityIds", activityIdsSubset);
         Iterator assignments = broker.iterate(query);
         OpAssignmentIfc assignment = null;
         OpResource resource = null;
         OpActivity activity = null;
         while (assignments.hasNext()) {
            Object[] result = (Object[]) assignments.next();
            assignment = (OpAssignmentIfc) result[0];
            resource = (OpResource) result[1];
            activity = (OpActivity) result[2];
            if (postFilter) {
               if (!filter.containsResourceID(resource.getId())) {
                  continue;
               }
            }
            XComponent dataRow = (XComponent) activityRowMap.get(activity.locator());
            resourceAvailability.put(resource.locator(), new Double(resource.getAvailable()));
            if (dataRow != null) {
               OpProjectPlan p = activityVersionLocatorToPlanMap.get(dataRow.getStringValue());
               OpProjectCalendar pCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, p);
               OpGanttValidator.addResource(dataRow, OpActivityVersionDataSetFactory.createResourceChoice(assignment, pCal));
               OpGanttValidator.setResourceBaseEffort(dataRow, resource.locator(), assignment.getBaseEffort());
            }
         }

         if (filter.getWorkPhases()) {
            // WorkPhases: Fill work phase starts, finishes and base effort columns
            query = broker
                  .newQuery("" +
                        "select workPeriod " +
                        "from OpWorkPeriod as workPeriod " +
                        "where workPeriod.Activity.id in (:activityIds) " +
                        "order by workPeriod.Activity.id");
            query.setCollection("activityIds", activityIdsSubset);//activityIdList);

            Iterator<OpWorkPeriod> workPeriods = broker.iterate(query);
            addWorkPhasesForActivities(activityRowMap, workPeriods);
         }
      }
      
      logger.debug("TIMING: retrieveFilteredActivityDataSet #02: " + (System.currentTimeMillis() - now));

      if (filter.getDependencies()) {
         // Dependencies: Fill predecessor and successor columns
         XComponent predecessorDataRow = null;
         XComponent successorDataRow = null;
         if (activityVersions != null) {
            for (OpActivityIfc activityVersion : activityVersions) {
               Set<? extends OpDependencyIfc> predecessors = activityVersion.getPredecessorDependencies();
               if (predecessors != null) {
                  successorDataRow = (XComponent) activityRowMap.get(activityVersion.getActivity().locator());
                  for (OpDependencyIfc predecessor: predecessors) {
                     OpActivityIfc predecessorActivity = predecessor.getPredecessorActivity();
                     if (activityVersions.contains(predecessorActivity)) {
                        predecessorDataRow = (XComponent) activityRowMap.get(predecessorActivity.getActivity().locator());
                        OpGanttValidator.addPredecessor(successorDataRow, predecessorDataRow.getIndex(), predecessor.getDependencyType(),
                              predecessor.getAttribute(OpDependency.DEPENDENCY_CRITICAL));
                     }
                  }
               }
               Set<? extends OpDependencyIfc> successors = activityVersion.getSuccessorDependencies();
               if (successors != null) {
                  predecessorDataRow = (XComponent) activityRowMap.get(activityVersion.getActivity().locator());
                  for (OpDependencyIfc successor: successors) {
                     OpActivityIfc successorActivity = successor.getSuccessorActivity();
                     if (activityVersions.contains(successorActivity)) {
                        successorDataRow = (XComponent) activityRowMap.get(successorActivity.getActivity().locator());
                        OpGanttValidator.addSuccessor(predecessorDataRow, successorDataRow.getIndex(), successor.getDependencyType(),
                              successor.getAttribute(OpDependency.DEPENDENCY_CRITICAL));
                     }
                  }
               }
            }
         }
      }
      
//      if (filter.isWithAssignments()) {
//         Iterator<XComponent> vLocs = activityRowMap.values().iterator();
//         AssignmentsForActivityLoactorsBulkLoader ah = new AssignmentsForActivityLoactorsBulkLoader(broker, activityRowMap);
//         ah.execute(vLocs, "select ass from OpAssignmentVersion as ass where ass.ActivityVersion.id in (:actIds)");
//      }

      //set also the visual resources (uses the value of the dataset as a value holder)
      Boolean showHours = (Boolean) dataSet.getValue();
      if (showHours == null) {
         showHours = Boolean.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.SHOW_RESOURCES_IN_HOURS));
      }
      Iterator it = activityRowMap.values().iterator();
      while (it.hasNext()) {
         XComponent activityRow = (XComponent) it.next();

         OpProjectPlan plan = activityVersionLocatorToPlanMap.get(activityRow.getStringValue());
         OpProjectCalendar pCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, plan, null);
         Map<String, OpProjectCalendar> resCals = new HashMap<String, OpProjectCalendar>();
         Iterator rit = OpGanttValidator.getResources(activityRow).iterator();
         while (rit.hasNext()) {
            String resId = XValidator.choiceID((String) rit.next());
            resCals.put(resId, OpProjectCalendarFactory.getInstance().getCalendar(session, broker, resId, plan != null ? plan.locator() : null));
         }
         OpGanttValidator.updateResourceVisualization(activityRow, showHours
               .booleanValue(), resourceAvailability, pCal, resCals);
      }
      logger.debug("TIMING: retrieveFilteredActivityDataSet #03: " + (System.currentTimeMillis() - now));
   }

   private void setQueryParameters(ArrayList argumentNames,
         ArrayList argumentValues, OpQuery query) {
      Object value = null;
      for (int i = 0; i < argumentNames.size(); i++) {
         value = argumentValues.get(i);
         if (value instanceof Collection) {
            query.setCollection((String) argumentNames.get(i), (Collection) value);
         }
         else if (value instanceof Boolean) {
            query.setBoolean((String) argumentNames.get(i), ((Boolean) value).booleanValue());
         }
         else if (value instanceof Date) {
            query.setDate((String) argumentNames.get(i), (Date) value);
         }
         else if (value instanceof Double) {
            query.setDouble((String) argumentNames.get(i), ((Double) value).doubleValue());
         }
         else if (value instanceof Integer) {
            query.setInteger((String) argumentNames.get(i), ((Integer) value).intValue());
         }
      }
   }
   
   public static void addWorkPhasesForActivities(
         Map<String, XComponent> activityRowMap,
         Iterator<? extends OpWorkPeriodIfc> workPeriods) {
      OpActivityIfc currentAct = null;
      Set<OpWorkPeriodIfc> actWPs = new HashSet<OpWorkPeriodIfc>();
      boolean finished = !workPeriods.hasNext();
      while (!finished) {
         OpWorkPeriodIfc workPeriod = null;
         finished = true;
         if (workPeriods.hasNext()) {
            workPeriod = (OpWorkPeriodIfc) workPeriods.next();
            finished = false;
         }
         if (currentAct == null || workPeriod == null || currentAct.getId() != workPeriod.getActivity().getId()) {
            // new Act.
            if (currentAct != null && actWPs != null && !actWPs.isEmpty()) {
               XComponent dataRow = (XComponent) activityRowMap.get(currentAct.locator());
               if (dataRow != null) {
                  addWorkPhasesFromWorkPeriods(currentAct, actWPs, dataRow);
               }
            }
            if (workPeriod != null) {
               actWPs = new HashSet<OpWorkPeriodIfc>();
               currentAct = workPeriod.getActivity();
            }
         }
         if (workPeriod != null) {
            actWPs.add(workPeriod);
         }
      }
   }

   /**
    * @param broker
    * @param filter
    * @param dataSet
    * @param fakeParent
    * @param attributesToSet
    * @param attributesToRemove
    * @param editable
    * @param activityIds
    * @param activityVersionLocatorToPlanMap
    * @param activityVersions
    * @param activityRowMap
    * @param outlineLevels
    * @param idsCollected
    * @param data
    * @pre
    * @post
    */
   private void addActivityVersionDataRows(OpBroker broker, OpActivityFilter filter,
         XComponent dataSet, int attributesToSet,
         int attributesToRemove, boolean editable, Set activityIds,
         Map<String, OpProjectPlan> activityVersionLocatorToPlanMap,
         Set<OpActivityIfc> activityVersions, Map<String, XComponent> activityRowMap,
         Set<Long> idsCollected,
         SortedSet<OpActivityIfc> data) {
      OpQuery actQ = broker.newQuery("select actVersion from OpActivityVersion as actVersion left join fetch actVersion.SuperActivityVersion as super left join fetch super.SuperActivityVersion as ssuper where actVersion.id in (:ids) order by actVersion.Sequence");
      actQ.setCollection("ids", idsCollected);
      List<OpActivityVersion> actVs = broker.list(actQ);
      Iterator<OpActivityVersion> it = actVs.iterator();
      while (it.hasNext()) {
         OpActivityVersion activityVersion = it.next();
         Long activityId = new Long(activityVersion.getId());
         activityIds.add(activityVersion.getActivity().getId());
         
         data.add(activityVersion);
      }
   }

   /**
    * @param broker
    * @param filter
    * @param dataSet
    * @param fakeParent
    * @param attributesToSet
    * @param attributesToRemove
    * @param editable
    * @param activityIds
    * @param activityVersionLocatorToPlanMap
    * @param activityVersions
    * @param activityRowMap
    * @param outlineLevels
    * @param idsCollected
    * @param data
    * @pre
    * @post
    */
   private void addActivityDataRows(OpBroker broker, OpActivityFilter filter,
         XComponent dataSet, int attributesToSet,
         int attributesToRemove, boolean editable, Set activityIds,
         Map<String, OpProjectPlan> activityVersionLocatorToPlanMap,
         Set<OpActivityIfc> activityVersions, Map<String, XComponent> activityRowMap,
         Set<Long> idsCollected,
         SortedSet<OpActivityIfc> data) {
      OpQuery actQ = broker.newQuery("select act from OpActivity as act where act.id in (:ids) order by act.Sequence");
      actQ.setCollection("ids", idsCollected);
      List<OpActivity> actVs = broker.list(actQ);
      Iterator<OpActivity> it = actVs.iterator();
      while (it.hasNext()) {
         OpActivity activityVersion = it.next();
         Long activityId = new Long(activityVersion.getId());
         
         data.add(activityVersion);
      }
   }

   /**
    * @param filter
    * @param dataSet
    * @param attributesToSet
    * @param attributesToRemove
    * @param editable
    * @param activityIds
    * @param activityVersionLocatorToPlanMap
    * @param activityVersions
    * @param activityRowMap
    * @param outlineLevels
    * @param activity
    * @param activityId
    * @pre
    * @post
    */
   private void addDataRow(XComponent dataSet, int attributesToSet,
         int attributesToRemove, boolean editable, HashSet activityIds,
         Map<String, OpProjectPlan> activityVersionLocatorToPlanMap,
         Set<OpActivityIfc> activityVersions,
         Map<String, XComponent> activityRowMap, int newOutlineLevel,
         boolean flattenDataSet, OpActivityIfc activity, Long activityId) {
      StringBuffer nameBuffer = activity.getName() != null ? new StringBuffer(activity.getName()) : new StringBuffer();
      int outlineLevel = activity.getOutlineLevel();

      OpActivityVersion actV = null;
      if (activity instanceof OpActivityVersion) {
         actV = (OpActivityVersion) activity;
      }
      if (flattenDataSet && outlineLevel > 0){
         // "Flatten" activity hierarchy
         // Patch activity name by adding context information
         if (actV != null) {
            OpActivityIfc act = actV;
            OpActivityIfc sAct = act.getSuperActivityIfc();
            List<String> actNames = new ArrayList<String>();
            while (sAct != null) {
               actNames.add(sAct.getName());
               sAct = sAct.getSuperActivityIfc();
            }
            int cnt = actNames.size();
            if (cnt > 0) {
               nameBuffer.append(" (");
               nameBuffer.append(actNames.get(0));
               if (cnt > 1) {
                  if (cnt > 2) {
                     nameBuffer.append(" ... ");
                  }
                  else {
                     nameBuffer.append(" : ");
                  }
                  nameBuffer.append(actNames.get(actNames.size() - 1));
               }
               nameBuffer.append(")");
            }
         }
         newOutlineLevel = 0;
      }
      
      XComponent dataRow = new XComponent(XComponent.DATA_ROW);
      setupActivityDataRow(activity.getProjectPlan(), null, activity, dataRow, editable, attributesToSet, attributesToRemove);
      activityVersionLocatorToPlanMap.put(dataRow.getStringValue(), activity.getProjectPlan());
      
      OpGanttValidator.setName(dataRow, nameBuffer.toString());
      dataRow.setOutlineLevel(newOutlineLevel);
      
      activityIds.add(activityId);
      activityVersions.add(activity);
      
      dataSet.addChild(dataRow);
      activityRowMap.put(activity.getActivity().locator(), dataRow);
   }

   private int adjustOutlineLevel(Stack<Integer> outlineLevels, int outlineLevel, boolean contributes) {
      // adjust stack of outline levels currently used:
      while (!outlineLevels.isEmpty() && outlineLevels.peek().intValue() >= outlineLevel) {
         outlineLevels.pop();
      }
      if (!contributes) {
         return outlineLevel;
      }
      int currentOutlineLevel = outlineLevels.isEmpty() ? -1 : outlineLevels.peek().intValue();
      int offset = outlineLevel - currentOutlineLevel;
      if (offset > 0) {
         outlineLevels.push(new Integer(outlineLevel));
      }
      return outlineLevels.size() - 1;
   }

   /**
    * @param broker
    * @param filter
    * @param order
    * @param projectPlanIds
    * @return
    * @pre
    * @post
    */
   private OpQuery createAdhocTaskQuery(OpBroker broker,
         OpActivityFilter filter, OpObjectOrderCriteria order) {
//select activity.id from  onepoint.project.modules.project.OpActivity as activity  inner join activity.AssignmentVersions as assignmentVersion where  activity.ProjectPlan.id in (:projectPlanIds) and  and assignmentVersion.Resource.id in (:resourceIds) and assignmentVersion.Assignment.Complete < 100 and activity.Type in (:types) and (activity.Start <= :startTo or activity.Start is null) and activity.Complete < 100 and activity.Deleted = false and activity.Template = :template order by activity.Sequence asc, activity.Priority asc]

      // Construct query string and arguments depending on filter and sort order
      StringBuffer fromBuffer = new StringBuffer(" OpActivity as activity ");
      StringBuffer whereBuffer = new StringBuffer("activity.Type = 6"); // adhoc task
      ArrayList argumentNames = new ArrayList();
      ArrayList argumentValues = new ArrayList();
      
      if (filter.getProjectNodeIds() != null && filter.getProjectNodeIds().size() > 0) {
         whereBuffer.append(" and activity.ProjectPlan.ProjectNode.id in (:projectIds)");
         argumentNames.add("projectIds");
         argumentValues.add(filter.getProjectNodeIds());
      }

      if (filter.getResourceIds().size() > 0) {
//         if (whereBuffer.length() > 0) {
//            whereBuffer.append(" and ");
//         }
         fromBuffer.append(" inner join activity.Assignments as assignment");
         whereBuffer.append(" and assignment.Resource.id in (:resourceIds)");

         if (filter.getAssignmentComplete() != null) {
            if (filter.getAssignmentComplete().booleanValue()) {
               whereBuffer.append(" and assignment.Complete = 100");
            }
            else {
               whereBuffer.append(" and assignment.Complete < 100");
            }
         }

         argumentNames.add("resourceIds");
         argumentValues.add(filter.getResourceIds());
      }

      Set<Byte> types = filter.getTypes();
      types.add(OpActivity.ADHOC_TASK);
      if (whereBuffer.length() > 0) {
         whereBuffer.append(" and ");
      }
      whereBuffer.append(" activity.Type in (:types)");
      argumentNames.add("types");
      argumentValues.add(types);

      if (filter.getStartFrom() != null) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and (");
         }
         whereBuffer.append(" activity.Start >= :startFrom");
         argumentNames.add("startFrom");
         argumentValues.add(filter.getStartFrom());
      }

      if (filter.getStartTo() != null) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and ");
         }
         if (filter.getStartFrom() == null) {
            whereBuffer.append('(');
         }
         whereBuffer.append("activity.Start <= :startTo");
         argumentNames.add("startTo");
         argumentValues.add(filter.getStartTo());
      }

      // Ensure that tasks are always returned as well
      if ((filter.getStartFrom() != null) || (filter.getStartTo() != null)) {
         whereBuffer.append(" or activity.Start is null)");
      }

      if (filter.getCompleted() != null) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and ");
         }
         if (filter.getCompleted().booleanValue()) {
            whereBuffer.append("activity.Complete = 100");
         }
         else {
            whereBuffer.append("activity.Complete < 100");
         }
      }
      
      // Always only select not-deleted activities
      if (whereBuffer.length() > 0) {
         whereBuffer.append(" and ");
      }
      whereBuffer.append(" activity.Template = :template ");
      argumentNames.add("template");
      argumentValues.add(Boolean.valueOf(filter.getTemplates()));

      StringBuffer queryBuffer = new StringBuffer("select activity.id from ");
      
      queryBuffer.append(fromBuffer);
      queryBuffer.append(" where ");
      queryBuffer.append(whereBuffer);

      if (order != null) {
         queryBuffer.append(order.toHibernateQueryString("activity"));
      }
      
      OpQuery query = broker.newQuery(queryBuffer.toString());
      // Note: We expect collections, booleans, dates and doubles
      Object value = null;
      for (int i = 0; i < argumentNames.size(); i++) {
         value = argumentValues.get(i);
         if (value instanceof Collection) {
            query.setCollection((String) argumentNames.get(i), (Collection) value);
         }
         else if (value instanceof Boolean) {
            query.setBoolean((String) argumentNames.get(i), ((Boolean) value).booleanValue());
         }
         else if (value instanceof Date) {
            query.setDate((String) argumentNames.get(i), (Date) value);
         }
         else if (value instanceof Double) {
            query.setDouble((String) argumentNames.get(i), ((Double) value).doubleValue());
         }
      }
      return query;
   }

   /**
    * Sets the information regardind the set of attachments on the data row.
    *
    * @param attachments    - the <code>Set</code> of <code>OpAttachment</code> entities
    * @param attachmentList - the <code>list</code> where the information regarding the attachments will be set
    */
   public static void retrieveAttachments(Set attachments, List attachmentList) {
      // TODO: Bulk-fetch like other parts of the project plan
      Iterator i = attachments.iterator();
      OpAttachmentIfc attachment = null;
      ArrayList attachmentElement = null;
      while (i.hasNext()) {
         attachment = (OpAttachmentIfc) i.next();
         attachmentElement = new ArrayList();
         if (attachment.getLinked()) {
            attachmentElement.add(OpProjectConstants.LINKED_ATTACHMENT_DESCRIPTOR);
         }
         else {
            attachmentElement.add(OpProjectConstants.DOCUMENT_ATTACHMENT_DESCRIPTOR);
         }
         attachmentElement.add(attachment.locator());
         attachmentElement.add(attachment.getName());
         attachmentElement.add(attachment.getLocation());
         if (!attachment.getLinked()) {
            if (attachment.getContent() != null) {
            String contentId = OpLocator.locatorString(attachment.getContent());
            attachmentElement.add(contentId);
            }
            else {
               logger.error("Linked Attachment without content found: " + attachment.locator());
            }
         }
         else {
            attachmentElement.add(OpProjectConstants.NO_CONTENT_ID);
         }
         attachmentList.add(attachmentElement);
      }
   }
   
   public void setupActivityDataRow(OpProjectPlan plan,
         OpProjectPlanVersion planVersion, OpActivityIfc activity,
         XComponent row, boolean editable, int attributesToSet,
         int attributesToRemove) {

      List attachmentList = new ArrayList();
      if (activity.getAttachments().size() > 0) {
         retrieveAttachments(activity.getAttachments(), attachmentList);
      }
      OpActivityVersion actV = (activity instanceof OpActivityVersion) ? (OpActivityVersion) activity
            : null;
      createDataRow(plan, planVersion, activity, attachmentList,
            row, editable, attributesToSet, attributesToRemove,
            actV != null ? actV.getSubProject() : null, actV != null ? actV
                  .getMasterActivityVersion() : null);
   }

   /*   
    * Mapping information for building the Activity Data Rows.
    */
   public final static String[][] ACTIVITY_ROW_LOCATOR_DESCRIPTION = {
      {"activityLocator", "-1"},
      {"outlineLevel", "-2"},
      {"activity.expanded", "-3"}
   };
   
   // 
   public static String[][] ACTIVITY_ROW_BASE_DESCRIPTION = {
      {"activity.name", "0", null, "out"},
      {"activity.type", "1", null, "out"},
      {"categoryLocator['activity.category.name']", "2"},
      {"activity.elementForActualValues.complete", "3", null, "out"},
      {"activity.start", "4", null, "out"},
      {"activity.finish", "5", null, "out"},
      {"activity.duration", "6", null, "out"},
      {"activity.baseEffort", "7", null, "out"},
      {"predecessors", "8", null, "out"},
      {"successors", "9", null, "out"},
      {"resources", "10", null, "out"},
      {"activity.basePersonnelCosts", "11", null, "out"},
      {"activity.baseTravelCosts", "12", null, "out"},
      {"activity.baseMaterialCosts", "13", null, "out"},
      {"activity.baseExternalCosts", "14", null, "out"},
      {"activity.baseMiscellaneousCosts", "15", null, "out"},
      {"activity.description", "16", null, "out"},
      {"attachments", "17", null, "out"},
      {"attributes", "18", null, "out"},
      {"workPhases", "19", null, "out"},
      {"activity.openEffort", "20", null, "out"},
      {"activity.activity.wbsCode", "21", null, "out"},
      {"resourceBaseEfforts", "22", null, "out"},
      {"activity.priority", "23", null, "out"},
      {"workRecords", "24", null, "out"},
      {"activity.elementForActualValues.actualEffort", "25", null, "out"},
      {"activityResources", "26", null, "out"},
      {"activity.responsibleResource.locator['activity.responsibleResource.name']", "27", null, "out"},
      {"activity.projectPlan.projectNode.locator['activity.projectPlan.projectNode.name']", "28", null, "out"},
      {"activity.payment", "29", null, "out"},
      {"activity.baseProceeds", "30", null, "out"},
      {"activity.effortBillable", "31", null, "out"},
      {"customAttributes", "32", null, "out"},
      {"ownedResources", "33", null, "out"},
      {"activity.leadTime", "34", null, "out"},
      {"activity.followUpTime", "35", null, "out"},
      {"successorVisualization", "36", null, "out"}, // unused here, but required!
      {"predecessorVisualization", "37", null, "out"}, // unused here, but required!
      {"workBreaks", "38", null, "out"},
      {"actions", "40", null, "out"},
      {"masterActivityLocator", "41", null, "out"},
      {"subProjectLocator", "42", null, "out"},
      {"actionsStatus", "44", null, "out"},
      {"error", "45", null, "out"},
   };
   
   public static String[][] NONE_COLLECTION_ROW_DESCRIPTION = {
      {"activity.name", "0"},
      {"activity.start", "4"},
      {"activity.finish", "5"},
      {"activity.duration", "6"},
      {"activity.baseEffort", "7"},

      {"resources", "10"},
      {"activity.baseTravelCosts", "12"},
      {"activity.baseMaterialCosts", "13"},
      {"activity.baseExternalCosts", "14"},
      {"activity.baseMiscellaneousCosts", "15"},

      {"activity.effortBillable", "31"},
      {"activity.priority", "23"},

      {"activityResources", "26"},
   };

   public static String[][] CONNECTED_TASK_ROW_DESCRIPTION = {
      {"predecessors", "8"},
      {"successors", "9"},
      {"successorVisualization", "36"},
      {"predecessorVisualization", "37"},
   };
   public static String[][] COLLECTION_ROW_DESCRIPTION = {
      {"activity.name", "0"},
      {"activity.priority", "23", null, "hidden"},
   };
   public static String[][] MILESTONE_ROW_DESCRIPTION = {
      {"activity.name", "0"},
      {"activity.payment", "29"},
      {"activity.priority", "23", null, "hidden"},
   };
   public static String[][] STANDARD_ROW_DESCRIPTION = {
      {"activity.name", "0"},
      {"activity.leadTime", "34"},
      {"activity.followUpTime", "35"},
   };
   public static String[][] ADHOC_TASK_ROW_DESCRIPTION = {
      {"activity.name", "0"},
      {"activity.complete", "3"},
      {"activity.finish", "5"},
      {"activity.baseEffort", "7"},

      {"resources", "10"},
      {"activity.baseTravelCosts", "12"},
      {"activity.baseMaterialCosts", "13"},
      {"activity.baseExternalCosts", "14"},
      {"activity.baseMiscellaneousCosts", "15"},

      {"activity.effortBillable", "31"},
      {"activity.priority", "23"},

      {"activityResources", "26"},
   };
   public static String[][] NONE_PROGRESS_TRACKED_ROW_DESCRIPTION = {
      {"activity.complete", "3"},
   };
   public static String[][] SCHEDULED_TASK_COLLECTION_ROW_DESCRIPTION = {
      {"activity.name", "0"},
      {"activity.start", "4"},
      {"activity.finish", "5"},

      {"resources", "10"},
      {"activity.baseTravelCosts", "12"},
      {"activity.baseMaterialCosts", "13"},
      {"activity.baseExternalCosts", "14"},
      {"activity.baseMiscellaneousCosts", "15"},
   };
   public static String[][] SUB_PROJECT_EDITABLE_ROW_DESCRIPTION = {
      {"activity.complete", "3"},
   };
   public static String[][] SUB_PROJECT_ROW_DESCRIPTION = {
      {"activity.elementForActualValues.complete", "3"},
   };
   public static String[][] IMPORTED_ROW_DESCRIPTION = {
      {"activity.elementForActualValues.wbsCode", "21"},
      {"activity.elementForActualValues.complete", "3"},
   };

   public void createDataRow(OpProjectPlan plan,
         OpProjectPlanVersion planVersion, OpActivityIfc activity,
         List attachmentList, XComponent row,
         boolean editable, int attributesToSet, int attributesToRemove,
         OpProjectNode subProject, OpActivityVersion masterActivityVersion) {

      int attributes = activity.getAttributes();
      
      // merge in inherited attributes from activity:
      attributes = attributes
            | (activity.getActivity() != null ? (activity.getActivity()
                  .getAttributes() & OpActivity.ATTRIBUTES_INHERITED_FROM_ACTIVITY)
                  : 0);
      
      attributes = attributes | attributesToSet;
      attributes = attributes - (attributes & attributesToRemove);
      
      Map<String, Object> objects = new HashMap<String, Object>();
      objects.put("activityLocator", activity.locator());
      objects.put("categoryLocator", activity.getCategory() != null ? activity.getCategory().locator() : null);
      objects.put("activity", activity);
      
      objects.put("outlineLevel", new Integer(activity.getOutlineLevel()));
      
      objects.put("attachments", attachmentList);
      objects.put("customAttributes", null);
      objects.put("predecessors", new TreeMap());
      objects.put("successors", new TreeMap());

      objects.put("resources", new ArrayList());
      objects.put("workPhases", new TreeMap());
      objects.put("resourceBaseEfforts", new HashMap());
      
      objects.put("workRecords", new HashMap());
      objects.put("activityResources", new ArrayList());
      
      SortedMap<Double, Map<String, Object>> workBreaks = getWorkBreaks(activity);
      objects.put("workBreaks", workBreaks);

      objects.put("attributes", new Integer(attributes));
      objects.put("actionsStatus", 0); // OpActionsDataSetUtil.getActionStatus(activity));
      objects.put("masterActivityLocator",
            masterActivityVersion != null ? masterActivityVersion.locator()
                  : null);
      objects.put("subProjectLocator", subProject != null ? subProject
            .locator() : null);
      boolean isSubProjectActivity = subProject != null;

      objects.put("error", new Integer(0)); // TODO: Idea: initialize with "invalid/not validated"?
      
      List<String[][]> conversionTables = new ArrayList<String[][]>();
      List<String[][]> importedEditableElements = new ArrayList<String[][]>();
      
      boolean isImported = (attributes & OpGanttValidator.IMPORTED_FROM_SUBPROJECT) == OpGanttValidator.IMPORTED_FROM_SUBPROJECT;

      conversionTables.add(ACTIVITY_ROW_LOCATOR_DESCRIPTION);
      conversionTables.add(ACTIVITY_ROW_BASE_DESCRIPTION);

      if (isSubProjectActivity && editable) {
         conversionTables.add(SUB_PROJECT_EDITABLE_ROW_DESCRIPTION);
      }
      else if (isSubProjectActivity) {
         conversionTables.add(SUB_PROJECT_ROW_DESCRIPTION);
      }
      else if (isImported) {
         // only, if no subprojectrow ...
         conversionTables.add(IMPORTED_ROW_DESCRIPTION);
      }
      // logger.debug("Activity type: " + activity.getType());
      switch(activity.getType()) {
      case OpActivity.STANDARD:
         importedEditableElements.add(CONNECTED_TASK_ROW_DESCRIPTION);
         conversionTables.add(NONE_COLLECTION_ROW_DESCRIPTION);
         conversionTables.add(STANDARD_ROW_DESCRIPTION);
         if (!activity.getProjectPlan().getProgressTracked()) {
            conversionTables.add(NONE_PROGRESS_TRACKED_ROW_DESCRIPTION);
         }
         break;
      case OpActivity.MILESTONE:
         importedEditableElements.add(CONNECTED_TASK_ROW_DESCRIPTION);
         conversionTables.add(NONE_COLLECTION_ROW_DESCRIPTION);
         conversionTables.add(MILESTONE_ROW_DESCRIPTION);
         if (!activity.getProjectPlan().getProgressTracked()) {
            conversionTables.add(NONE_PROGRESS_TRACKED_ROW_DESCRIPTION);
         }
         break;
      case OpActivity.TASK:
         importedEditableElements.add(CONNECTED_TASK_ROW_DESCRIPTION);
         conversionTables.add(NONE_COLLECTION_ROW_DESCRIPTION);
         conversionTables.add(STANDARD_ROW_DESCRIPTION);
         if (!activity.getProjectPlan().getProgressTracked()) {
            conversionTables.add(NONE_PROGRESS_TRACKED_ROW_DESCRIPTION);
         }
         break;
      case OpActivity.ADHOC_TASK:
         conversionTables.add(ADHOC_TASK_ROW_DESCRIPTION);
         if (!activity.getProjectPlan().getProgressTracked()) {
            conversionTables.add(NONE_PROGRESS_TRACKED_ROW_DESCRIPTION);
         }
         break;
      case OpActivity.COLLECTION:
         importedEditableElements.add(CONNECTED_TASK_ROW_DESCRIPTION);
         conversionTables.add(COLLECTION_ROW_DESCRIPTION);
         break;
      case OpActivity.COLLECTION_TASK:
         break;
      case OpActivity.SCHEDULED_TASK:
         importedEditableElements.add(CONNECTED_TASK_ROW_DESCRIPTION);
         conversionTables.add(SCHEDULED_TASK_COLLECTION_ROW_DESCRIPTION);
         break;
      }
      
      additionalAddAttributes(activity, objects);
            
      Iterator<String[][]> mapIt = conversionTables.iterator();
      while (mapIt.hasNext()) {
         String[][] map =  mapIt.next();
         if (map != null) {
            OpProjectValidator.populateDataRowFromMap(row, map, objects, editable && !isImported);
         }
      }
      
      mapIt = importedEditableElements.iterator();
      while (mapIt.hasNext()) {
         String[][] map =  mapIt.next();
         if (map != null) {
            OpProjectValidator.populateDataRowFromMap(row, map, objects, editable);
         }
      }
      
      OpGanttValidator.updateAttachmentAttribute(row);
   }

   /**
    * @param activity
    * @param objects
    * @pre
    * @post
    */
   protected void additionalAddAttributes(OpActivityIfc activity,
         Map<String, Object> objects) {
   }

   public static SortedMap<Double, Map<String, Object>> getWorkBreaks(
         OpActivityIfc activity) {
      SortedMap<Double, Map<String, Object>> workBreaks = new TreeMap<Double, Map<String,Object>>();
      if (activity.getWorkBreaks() == null) {
         return workBreaks;
      }
      for (OpWorkBreak wb: activity.getWorkBreaks()) {
         Map<String, Object> wbInfo = new HashMap<String, Object>();
         wbInfo.put(OpGanttValidator.WORK_BREAK_LOCATOR, wb.locator());
         wbInfo.put(OpGanttValidator.WORK_BREAK_START, wb.getStart());
         wbInfo.put(OpGanttValidator.WORK_BREAK_DURATION, wb.getDuration());
         workBreaks.put(wb.getStart(), wbInfo);
      }
      return workBreaks;
   }

   private static void mapActivityVersionIDs(OpBroker broker, XComponent dataSet, OpProjectPlanVersion workingPlanVersion) {
      // Exchange all activity version IDs contained in data-row values with their respective actual activity IDs

      HashMap activityVersionIdMap = new HashMap();
      OpQuery query = broker
           .newQuery("select activityVersion.id, activityVersion.Activity.id from OpActivityVersion as activityVersion where activityVersion.PlanVersion.id = ?");
      query.setLong(0, workingPlanVersion.getId());
      Iterator result = broker.iterate(query);
      Object[] record = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         activityVersionIdMap.put(record[0], record[1]);
      }

      XComponent dataRow = null;
      Long activityId = null;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (dataRow.getStringValue() != null) {
            activityId = (Long) activityVersionIdMap.get(new Long(OpLocator.parseLocator(dataRow.getStringValue())
                 .getID()));
            if (activityId != null) {
               dataRow.setStringValue(OpLocator.locatorString(OpActivity.ACTIVITY, activityId.longValue()));
            }
            else {
               dataRow.setStringValue(null);
            }
         }
      }

   }


   public abstract static class AutoFlushCopyHelper<A, B> extends OpCollectionSynchronizationHelper<A, B> {

      private static final XLog logger = XLogFactory.getLogger(AutoFlushCopyHelper.class);

      OpBroker broker = null;
      int changeCount = 0;
      
      protected AutoFlushCopyHelper(OpBroker broker) {
         this.broker = broker;
         changeCount = 0;
      }
      
      public static final int FLUSH_LIMIT = 100;
      
      protected void touch() {
         changeCount++;
         if (changeCount % FLUSH_LIMIT == 0) {
            // logger.debug("Flushing after " + changeCount + " changes.");
            // broker.getConnection().flush();
         }
      }
      
   }
   
   /**
    * @author peter
    *
    * @param <A> any thing derived from OpActivityIfc
    * @param <B> any thing derived from OpActivityIfc
    */
   public abstract static class ActivityCopyHelper<A, B> extends AutoFlushCopyHelper<A, B> {

      protected ActivityCopyHelper(OpBroker broker) {
         super(broker);
      }

      protected int correspondsIfc(OpActivityIfc tgt, OpActivityIfc src) {
         // for activities and versions, the Object ID is the only criteria.
         if (src.getActivity() == null) {
            return ADD;
         }
         long l1 = tgt.getId();
         long l2 = src.getActivity().getId();
         byte ll1 = tgt.getType();
         byte ll2 = src.getType();
         return cmpLongLong(l1, ll1, l2, ll2);
         // return cmpLong(l1, l2);
      }

      protected int order1Ifc(OpActivityIfc cm1a, OpActivityIfc cm1b) {
         long l1 = cm1a.getId();
         long l2 = cm1b.getId();
         byte ll1 = cm1a.getType();
         byte ll2 = cm1b.getType();
         return cmpLongLong(l1, ll1, l2, ll2);
         // return cmpLong(cm1a.getId(), cm1b.getId());
      }

      protected int order2Ifc(OpActivityIfc cm2a, OpActivityIfc cm2b) {
         long l1 = cm2a.getActivity() == null ? 0 : cm2a.getActivity().getId();
         long l2 = cm2b.getActivity() == null ? 0 : cm2b.getActivity().getId();
         byte ll1 = cm2a.getActivity() == null ? -1 : cm2a.getActivity().getType();
         byte ll2 = cm2b.getActivity() == null ? -1 : cm2b.getActivity().getType();
         return cmpLongLong(l1, ll1, l2, ll2);
      }
   }
   
   public class CheckInActivityCopyHelper extends ActivityCopyHelper<OpActivity, OpActivityVersion> {

      private OpBroker broker = null;
      private OpProjectPlan plan = null;
      private boolean updateDeleted = true;
      
      private Map<Long, OpActivityVersion> superActivityMap = new HashMap<Long, OpActivityVersion>();
      private OpProjectSession session;
      
      public  Map<Long, OpActivityVersion> getSuperActivityMap() {
         return superActivityMap;
      }

      public CheckInActivityCopyHelper(OpProjectSession session, OpBroker broker, OpProjectPlan plan, boolean updateDeleted) {
         super(broker);
         this.session = session;
         this.broker = broker;
         this.plan = plan;
         this.updateDeleted = updateDeleted;
      }

      @Override
      protected int cloneInstance(OpActivity tgt, OpActivityVersion src) {
         if (!checkTypeChange(tgt, src.getType())) {
            throw new XValidationException(OpProjectError.CANNOT_CHANGE_ACTIVITY_TYPE_ERROR_NAME);
         }
         tgt.setDeleted(false); // reanimate, if required...
         if (src.getSuperActivityVersion() != null) {
            superActivityMap.put(tgt.getId(), src.getSuperActivityVersion());
         }
         if (tgt.getSuperActivity() != null) {
            tgt.getSuperActivity().removeSubActivity(tgt);
         }
         OpActivityDataSetFactory.getInstance().clone(session, broker, tgt, src, plan.getProgressTracked());
         // complete is set in this case:
         if (!plan.getProgressTracked()) {
            tgt.setComplete(src.getComplete());
         }
         touch();
         return OK;
      }

      @Override
      protected int corresponds(OpActivity tgt, OpActivityVersion src) {
         return correspondsIfc(tgt, src);
      }

      @Override
      protected void deleteInstance(OpActivity del) {
         if (del.getType() == OpActivity.ADHOC_TASK) {
            return;
         }
         // unlink from it's environment...
         if (del.getSuperActivity() != null) {
            del.getSuperActivity().removeSubActivity(del);
         }
         if (del.getMasterActivity() != null) {
            del.getMasterActivity().removeShallowCopy(del);
         }
         if (del.getSubProject() != null) {
            del.getSubProject().removeProgramActivity(del);
         }
         if (updateDeleted) {
            if (del.getResponsibleResource() != null) {
               del.getResponsibleResource().removeActivity(del);
            }
            del.setOutlineLevel((byte) 0);
            del.setDeleted(true);
         }
      }

      @Override
      protected OpActivity newInstance(OpActivityVersion src) {
         OpActivity a = new OpActivity();
         plan.addActivity(a);
         a.addActivityVersion(src);
         broker.makePersistent(a);
         return a;
      }

      @Override
      protected int targetOrder(OpActivity cm1a, OpActivity cm1b) {
         return order1Ifc(cm1a, cm1b);
      }

      @Override
      protected int sourceOrder(OpActivityVersion cm2a, OpActivityVersion cm2b) {
         return order2Ifc(cm2a, cm2b);
      }

      @Override
      protected String debugSource(OpActivityVersion src) {
         String result = "ID: " + (src.getActivity() != null ? src.getActivity().getId() : 0) + " TYPE:" + src.getType();
         return result;
      }

      @Override
      protected String debugTarget(OpActivity tgt) {
         String result = "ID: " + tgt.getId() + " TYPE:" + tgt.getType();
         return result;
      }

   }
   
   
   private static class CheckInAssignmentsCopyHelper extends AutoFlushCopyHelper<OpAssignment, OpAssignmentVersion> {

      private OpProjectSession session= null;
      private OpBroker broker = null;
      private OpProjectPlan plan = null;
      
      public long td[] = {0,0,0,0,0,0};
      
      private Set<OpAssignment> assignmentsToDelete = new HashSet<OpAssignment>();
      
      public Set<OpAssignment> getAssignmentsToDelete() {
         return assignmentsToDelete;
      }

      public CheckInAssignmentsCopyHelper(OpProjectSession session,
            OpBroker broker, OpProjectPlan plan) {
         super(broker);
         this.session = session;
         this.broker = broker;
         this.plan = plan;
      }
      
      @Override
      protected int cloneInstance(OpAssignment tgt, OpAssignmentVersion src) {
         long nowD = System.currentTimeMillis();
         // unlink, to remove old values from parents:
         // (no parent, if new assignment)
         boolean newAssignment = tgt.getActivity() == null;
         if (!newAssignment) {
            tgt.detachFromActivity(tgt.getActivity());
            // tgt.getActivity().removeAssignment(tgt);
         }
         td[0] += System.currentTimeMillis() - nowD;
         tgt.setAssigned(src.getAssigned());
         tgt.setBaseEffort(src.getBaseEffort());
         tgt.setBaseCosts(src.getBaseCosts());
         tgt.setBaseProceeds(src.getBaseProceeds());
         
         td[1] += System.currentTimeMillis() - nowD;
         if (!tgt.hasWorkRecords()) {
            tgt.setRemainingEffort(tgt.getBaseEffort());
            tgt.setRemainingPersonnelCosts(tgt.getBaseCosts());
            tgt.setRemainingProceeds(tgt.getBaseProceeds());
         }

         td[2] += System.currentTimeMillis() - nowD;
         OpActivity owningActivity = src.getActivityVersion().getActivity();
         if (!plan.getProgressTracked()) {
            // TODO: check...
            double complete = owningActivity.getComplete();
            tgt.setComplete(complete);
            double remainingEffort = OpGanttValidator.calculateRemainingEffort(tgt.getBaseEffort(), tgt.getActualEffort(), tgt.getComplete());
            tgt.setRemainingEffort(remainingEffort);
         }
         
         td[3] += System.currentTimeMillis() - nowD;
         // link to update parent with new values here:
         if (newAssignment) {
            owningActivity.addAssignment(tgt);
            src.getResource().addActivityAssignment(tgt);
         }
         else if (owningActivity.getId() != tgt.getActivity().getId()) {
            logger.error("Assignment linked to wrong activity?: " + tgt.getId());
         }
         else if (src.getResource().getId() != tgt.getResource().getId()) {
            logger.error("Assignment linked to wrong resource?: " + tgt.getId());
         }
         tgt.attachToActivity(src.getActivityVersion().getActivity());
         td[4] += System.currentTimeMillis() - nowD;
         
         // requires the assignment to be attached!
         updateWorkMonths(session, broker, tgt);
         td[5] += System.currentTimeMillis() - nowD;

         touch();
         return OK;
      }

      @Override
      protected int corresponds(OpAssignment tgt, OpAssignmentVersion src) {
         if (src.getActivityVersion() == null || src.getResource() == null) {
            return DO_NOT_COPY;
         }
         if (src.getAssignment() == null) {
            return ADD;
         }
         long l1 = tgt.getActivity().getId();
         long l2 = src.getActivityVersion().getActivity().getId();
         long ll1 = tgt.getResource().getId();
         long ll2 = src.getResource().getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }

      @Override
      protected void deleteInstance(OpAssignment del) {
         if (del.getActivity().getType() == OpActivity.ADHOC_TASK) {
            return;
         }
         del.detachFromActivity(del.getActivity());
         del.getActivity().removeAssignment(del);
            del.getResource().removeActivityAssignment(del);
         // unlink...
         if (del.getAssignmentVersions() == null || del.getAssignmentVersions().isEmpty()) {
            plan.removeAssignment(del);
            broker.deleteObject(del);
         }
         else {
            // deferred
            assignmentsToDelete.add(del);
         }
      }

      @Override
      protected String debugSource(OpAssignmentVersion src) {
         String result = "Act-ID: " + (src.getActivity() != null ? src.getActivityVersion().getActivity().getId() : 0) + " Res:" + src.getResource().getId() + " ActV-Id: " + src.getActivityVersion().getId();
         return result;
      }

      @Override
      protected String debugTarget(OpAssignment tgt) {
         String result = "Act-ID: " + tgt.getActivity().getId() + " Res:" + tgt.getResource().getId();
         return result;
      }

      @Override
      protected OpAssignment newInstance(OpAssignmentVersion src) {
         OpAssignment a = new OpAssignment();
         a.addAssignmentVersion(src);
         plan.addAssignment(a);
         broker.makePersistent(a);
         return a;
      }

      @Override
      protected int targetOrder(OpAssignment cm1a, OpAssignment cm1b) {
         long l1 = cm1a.getActivity().getId();
         long l2 = cm1b.getActivity().getId();
         long ll1 = cm1a.getResource().getId();
         long ll2 = cm1b.getResource().getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }

      @Override
      protected int sourceOrder(OpAssignmentVersion cm2a, OpAssignmentVersion cm2b) {
         long l1 = cm2a.getActivityVersion() == null ? 0 : cm2a.getActivityVersion().getActivity().getId();
         long l2 = cm2b.getActivityVersion() == null ? 0 : cm2b.getActivityVersion().getActivity().getId();
         long ll1 = cm2a.getResource() == null ? 0 : cm2a.getResource().getId();
         long ll2 = cm2b.getResource() == null ? 0 : cm2b.getResource().getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }
      
   }
   
   private static class CheckInDependenciesCopyHelper extends AutoFlushCopyHelper<OpDependency, OpDependencyVersion> {

      private OpBroker broker = null;
      private OpProjectPlan plan = null;
      
      public CheckInDependenciesCopyHelper(OpProjectSession session, OpBroker broker, OpProjectPlan plan) {
         super(broker);
         this.broker = broker;
         this.plan = plan;
      }
      
      @Override
      protected int cloneInstance(OpDependency tgt, OpDependencyVersion src) {
         tgt.setDependencyType(src.getDependencyType());
         tgt.setAttributes(src.getAttributes());
         touch();
         return OK;
      }

      @Override
      protected int corresponds(OpDependency tgt, OpDependencyVersion src) {
         if (src.getPredecessorVersion() == null || src.getPredecessorVersion() == null) {
            return DO_NOT_COPY;
         }
         long l1 = tgt.getPredecessorActivity().getId();
         long l2 = src.getPredecessorVersion().getActivity().getId();
         long ll1 = tgt.getSuccessorActivity().getId();
         long ll2 = src.getSuccessorVersion().getActivity().getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }

      @Override
      protected void deleteInstance(OpDependency del) {
         del.getPredecessorActivity().removeSuccessorDependency(del);
         del.getSuccessorActivity().removePredecessorDependency(del);
         plan.removeDependency(del);
         broker.deleteObject(del);
      }

      @Override
      protected OpDependency newInstance(OpDependencyVersion src) {
         OpActivity pred = src.getPredecessorVersion().getActivity();
         OpActivity succ = src.getSuccessorVersion().getActivity();
         
         OpDependency nd = new OpDependency();
         pred.addSuccessorDependency(nd);
         succ.addPredecessorDependency(nd);
         
         broker.makePersistent(nd);
         plan.addDependency(nd);
         return nd;
      }

      @Override
      protected int targetOrder(OpDependency cm1a, OpDependency cm1b) {
         long l1 = cm1a.getPredecessorActivity().getId();
         long l2 = cm1b.getPredecessorActivity().getId();
         long ll1 = cm1a.getSuccessorActivity().getId();
         long ll2 = cm1b.getSuccessorActivity().getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }

      @Override
      protected int sourceOrder(OpDependencyVersion cm2a, OpDependencyVersion cm2b) {
         long l1 = cm2a.getPredecessorVersion() == null ? 0 : cm2a.getPredecessorVersion().getActivity().getId();
         long l2 = cm2b.getPredecessorVersion() == null ? 0 : cm2b.getPredecessorVersion().getActivity().getId();
         long ll1 = cm2a.getSuccessorVersion() == null ? 0 : cm2a.getSuccessorVersion().getActivity().getId();
         long ll2 = cm2b.getSuccessorVersion() == null ? 0 : cm2b.getSuccessorVersion().getActivity().getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }
      
   }
   
   private static class PermissionCopyHelper extends OpCollectionSynchronizationHelper<OpPermission, OpPermission> {

      private OpBroker broker = null;
      private OpPermissionable newTgt = null;
      private OpProjectSession session;
      
      public PermissionCopyHelper(OpProjectSession session, OpBroker broker, OpPermissionable newTgt) {
         this.session = session;
         this.broker = broker;
         this.newTgt = newTgt;
      }
      
      @Override
      protected int cloneInstance(OpPermission tgt, OpPermission src) {
         tgt.setAccessLevel(src.getAccessLevel());
         src.getSubject().addOwnedPermission(tgt);
         tgt.setSystemManaged(src.getSystemManaged());
         return OK;
      }

      @Override
      protected int corresponds(OpPermission tgt, OpPermission src) {
         return cmpLong(tgt.getSubject().getId(), src.getSubject().getId());
      }

      @Override
      protected void deleteInstance(OpPermission del) {
         del.getSubject().removeOwnedPermission(del);
         del.getObject().removePermission(del);
         broker.deleteObject(del);
      }

      @Override
      protected OpPermission newInstance(OpPermission src) {
         OpPermission np = new OpPermission();
         broker.makePersistent(np);
         newTgt.addPermission(np);
         return np;
      }

      @Override
      protected int targetOrder(OpPermission cm1a, OpPermission cm1b) {
         return cmpLong(cm1a.getSubject().getId(), cm1b.getSubject().getId());
      }

      @Override
      protected int sourceOrder(OpPermission cm2a, OpPermission cm2b) {
         return cmpLong(cm2a.getSubject().getId(), cm2b.getSubject().getId());
      }
      
   }
   
   private static class CheckInAttachmentsCopyHelper extends AutoFlushCopyHelper<OpAttachment, OpAttachmentVersion> {

      private OpBroker broker = null;
      private OpProjectPlan plan= null;
      private OpProjectSession session;
      
      public CheckInAttachmentsCopyHelper(OpProjectSession session, OpBroker broker, OpProjectPlan plan) {
         super(broker);
         this.session = session;
         this.broker = broker;
         this.plan = plan;
      }
      
      @Override
      protected int cloneInstance(OpAttachment tgt, OpAttachmentVersion src) {
         if (tgt.getContent() != null) {
            tgt.getContent().removeAttachment(tgt);
         }
         if (src.getContent() != null) {
            src.getContent().addAttachment(tgt);
         }
         tgt.setLinked(src.getLinked());
         tgt.setLocation(src.getLocation());
         tgt.setName(src.getName());
         
         if (src.getPermissions() != null) {
            PermissionCopyHelper pch = new PermissionCopyHelper(session, broker, tgt);
            if (tgt.getPermissions() == null) {
               tgt.setPermissions(new HashSet<OpPermission>());
            }
            pch.copy(tgt.getPermissions(), src.getPermissions());
         }
         touch();
         return OK;
      }

      @Override
      protected int corresponds(OpAttachment tgt, OpAttachmentVersion src) {
         if (src.getActivityVersion() == null) {
            return DO_NOT_COPY;
         }
         long l1 = tgt.getActivity().getId();
         long l2 = src.getActivityVersion().getActivity().getId();
         long ll1 = tgt.getContent() != null ? tgt.getContent().getId() : tgt.getId();
         long ll2 = src.getContent() != null ? src.getContent().getId() : src.getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }

      @Override
      protected void deleteInstance(OpAttachment del) {
         if (del.getActivity().getType() == OpActivity.ADHOC_TASK) {
            return;
         }
         del.getActivity().removeAttachment(del);
         if (del.getContent() != null) {
            del.getContent().removeAttachment(del);
         }
         broker.deleteObject(del);
      }

      @Override
      protected OpAttachment newInstance(OpAttachmentVersion src) {
         OpAttachment na = new OpAttachment();
         src.getActivityVersion().getActivity().addAttachment(na);
         broker.makePersistent(na);
         return na;
      }

      @Override
      protected int targetOrder(OpAttachment cm1a, OpAttachment cm1b) {
         long l1 = cm1a.getActivity().getId();
         long l2 = cm1b.getActivity().getId();
         long ll1 = cm1a.getContent() != null ? cm1a.getContent().getId() : cm1a.getId();
         long ll2 = cm1b.getContent() != null ? cm1b.getContent().getId() : cm1b.getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }

      @Override
      protected int sourceOrder(OpAttachmentVersion cm2a, OpAttachmentVersion cm2b) {
         long l1 = cm2a.getActivityVersion() == null ? 0 : cm2a.getActivityVersion().getActivity().getId();
         long l2 = cm2b.getActivityVersion() == null ? 0 : cm2b.getActivityVersion().getActivity().getId();
         long ll1 = cm2a.getContent() != null ? cm2a.getContent().getId() : cm2a.getId();
         long ll2 = cm2b.getContent() != null ? cm2b.getContent().getId() : cm2b.getId();
         return cmpLongLong(l1, ll1, l2, ll2);
      }
      
   }
   
   
   private class CheckInWorkBreakCopyHelper extends
   AutoFlushCopyHelper<OpWorkBreak, OpWorkBreak> {

      private OpBroker broker = null;
      private OpProjectPlan plan = null;
      
      public CheckInWorkBreakCopyHelper(OpProjectSession session, OpBroker broker, OpProjectPlan plan) {
         super(broker);
         this.broker = broker;
         this.plan = plan;
      }
      
      @Override
      protected int cloneInstance(OpWorkBreak tgt, OpWorkBreak src) {
         tgt.setStart(src.getStart());
         tgt.setDuration(src.getDuration());
         touch();
         return OK;
      }

      @Override
      protected int corresponds(OpWorkBreak tgt, OpWorkBreak src) {
         if (src.getActivity() == null) {
            return DO_NOT_COPY;
         }
         long l1 = tgt.getActivity().getId();
         long l2 = src.getActivity().getActivity().getId();
         int cmp = cmpLong(l1, l2);
         if (cmp == EQUAL) {
            cmp = tgt.getStart() < src.getStart() ?  BEFORE : tgt.getStart() == src.getStart() ? EQUAL : AFTER;
         }
         return cmp;
      }

      @Override
      protected void deleteInstance(OpWorkBreak del) {
         del.getActivity().removeWorkBreak(del);
         plan.removeWorkBreak((OpActivityWorkBreak) del);
         broker.deleteObject(del);
      }

      @Override
      protected OpWorkBreak newInstance(OpWorkBreak src) {
         OpActivityWorkBreak wb = new OpActivityWorkBreak();
         src.getActivity().getActivity().addWorkBreak(wb);
         plan.addWorkBreak(wb);
         broker.makePersistent(wb);
         return wb;
      }

      @Override
      protected int targetOrder(OpWorkBreak cm1a, OpWorkBreak cm1b) {
         long l1 = cm1a.getActivity().getId();
         long l2 = cm1b.getActivity().getId();
         int cmp = cmpLong(l1, l2);
         if (cmp == EQUAL) {
            cmp = cm1a.getStart() < cm1b.getStart() ?  BEFORE : cm1a.getStart() == cm1b.getStart() ? EQUAL : AFTER;
         }
         return cmp;
      }

      @Override
      protected int sourceOrder(OpWorkBreak cm2a, OpWorkBreak cm2b) {
         long l1 = cm2a.getActivity().getActivity().getId();
         long l2 = cm2b.getActivity().getActivity().getId();
         int cmp = cmpLong(l1, l2);
         if (cmp == EQUAL) {
            cmp = cm2a.getStart() < cm2b.getStart() ?  BEFORE : cm2a.getStart() == cm2b.getStart() ? EQUAL : AFTER;
         }
         return cmp;
      }
      
   }

   private class CheckInWorkPeriodCopyHelper extends
   AutoFlushCopyHelper<OpWorkPeriod, OpWorkPeriodVersion> {

      private OpBroker broker = null;
      private OpProjectPlan plan = null;
      
      public CheckInWorkPeriodCopyHelper(OpProjectSession session, OpBroker broker, OpProjectPlan plan) {
         super(broker);
         this.broker = broker;
         this.plan = plan;
      }
      
      @Override
      protected int cloneInstance(OpWorkPeriod tgt, OpWorkPeriodVersion src) {
         tgt.setStart(src.getStart());
         tgt.setBaseEffort(src.getBaseEffort());
         tgt.setWorkingDays(src.getWorkingDays());
         touch();
         return OK;
      }

      @Override
      protected int corresponds(OpWorkPeriod tgt, OpWorkPeriodVersion src) {
         if (src.getActivityVersion() == null) {
            return DO_NOT_COPY;
         }
         long l1 = tgt.getActivity().getId();
         long l2 = src.getActivityVersion().getActivity().getId();
         int cmp = cmpLong(l1, l2);
         if (cmp == EQUAL) {
            cmp = tgt.getStart().getTime() < src.getStart().getTime() ? BEFORE
                  : tgt.getStart().getTime() == src.getStart().getTime() ? EQUAL
                        : AFTER;
         }
         return cmp;
      }

      @Override
      protected void deleteInstance(OpWorkPeriod del) {
         del.getActivity().removeWorkPeriod(del);
         plan.removeWorkPeriod(del);
         broker.deleteObject(del);
      }

      @Override
      protected OpWorkPeriod newInstance(OpWorkPeriodVersion src) {
         OpWorkPeriod wp = new OpWorkPeriod();
         src.getActivityVersion().getActivity().addWorkPeriod(wp);
         plan.addWorkPeriod(wp);
         broker.makePersistent(wp);
         return wp;
      }

      @Override
      protected int targetOrder(OpWorkPeriod cm1a, OpWorkPeriod cm1b) {
         long l1 = cm1a.getActivity().getId();
         long l2 = cm1b.getActivity().getId();
         int cmp = cmpLong(l1, l2);
         if (cmp == EQUAL) {
            cmp = cm1a.getStart().getTime() < cm1b.getStart().getTime() ? BEFORE
                  : cm1a.getStart().getTime() == cm1b.getStart().getTime() ? EQUAL
                        : AFTER;
         }
         return cmp;
      }

      @Override
      protected int sourceOrder(OpWorkPeriodVersion cm2a, OpWorkPeriodVersion cm2b) {
         long l1 = cm2a.getActivityVersion().getActivity().getId();
         long l2 = cm2b.getActivityVersion().getActivity().getId();
         int cmp = cmpLong(l1, l2);
         if (cmp == EQUAL) {
            cmp = cm2a.getStart().getTime() < cm2b.getStart().getTime() ? BEFORE
                  : cm2a.getStart().getTime() == cm2b.getStart().getTime() ? EQUAL
                        : AFTER;
         }
         return cmp;
      }
      
   }
   
   public final static Pattern WBS_CODE_PATTERN = Pattern.compile("((([0-9]+)\\.)*)([0-9]+)");
   
   private void setNextCode(OpActivity activity, Map<String, Integer> wbsParentIndexMap) {
      OpActivity superActivity = activity.getSuperActivity();
      String wbsParentKey = "";
      if (superActivity != null) {
         wbsParentKey = superActivity.getWbsCode();
      }
      
      Integer wbsParentIndex = wbsParentIndexMap.get(wbsParentKey);
      int ci = 0;
      if (wbsParentIndex != null) {
         ci = wbsParentIndex.intValue();
      }
      ci++;
      wbsParentIndexMap.put(wbsParentKey, new Integer(ci));
      activity
            .setWbsCode((superActivity != null ? (superActivity.getWbsCode() + ".")
                  : "")
                  + Integer.toString(ci));
   }
   
   private boolean updateWbsParentsMap(OpActivity activity, Map<String, Integer> wbsParentIndexMap) {
      String childWBSCode = activity.getWbsCode();
      if (childWBSCode == null || childWBSCode.length() == 0) {
         return false;
      }
      
      Matcher m = WBS_CODE_PATTERN.matcher(childWBSCode);
      if (m.matches()) {
         String wbsParentKey = m.group(1);
         if (wbsParentKey.length() > 0) {
            wbsParentKey = wbsParentKey.substring(0, wbsParentKey.length() - 1);
         }
         String childIndexString = m.group(m.groupCount());
         int ci = Integer.parseInt(childIndexString);
         
         Integer wbsParentIndex = wbsParentIndexMap.get(wbsParentKey);
         if (wbsParentIndex == null || wbsParentIndex.intValue() < ci) {
            wbsParentIndexMap.put(wbsParentKey, new Integer(ci));
         }
         return true;
      }
      return false;
   }
   /**
    * Use the given projectplan version and copy all relevant data to the unversioned objects,
    * Create all required dependend objects.
    * 
    * @param session
    * @param broker
    * @param planVersion
    */
   public void checkInProjectPlan(OpProjectSession session,
         OpBroker broker, OpProjectPlanVersion planVersion) throws XValidationException {
      OpProjectPlan plan = planVersion.getProjectPlan();

      long now = System.currentTimeMillis();
      
      logger.debug("TIMING: checkInProjectPlan #01: "
            + (System.currentTimeMillis() - now) + " plan-ID:" + plan.getId());
      CheckInActivityCopyHelper copyAct = synchronizeActivitiesWithVersions(
            session, broker, planVersion, plan, true);
      
      logger.debug("TIMING: checkInProjectPlan #01: "
            + (System.currentTimeMillis() - now));
      CheckInWorkPeriodCopyHelper copyWp = new CheckInWorkPeriodCopyHelper(session, broker, plan);
      copyWp.copy(plan.getWorkPeriods(), planVersion.getWorkPeriodVersions());

      logger.debug("TIMING: checkInProjectPlan #02: "
            + (System.currentTimeMillis() - now));

      CheckInAssignmentsCopyHelper copyAss = new CheckInAssignmentsCopyHelper(
            session, broker, plan);
      copyAss.copy(plan.getActivityAssignments(), planVersion.getAssignmentVersions());
      OpActivityVersionDataSetFactory.logTimingDetails(logger, "TIMING: checkInProjectPlan #02", copyAss.td);
      
      logger.debug("TIMING: checkInProjectPlan #03: "
            + (System.currentTimeMillis() - now));
      // delete deffered assignment-deletes...
      Iterator<OpAssignment> delAssIt = copyAss.getAssignmentsToDelete().iterator();
      while (delAssIt.hasNext()) {
         OpAssignment del = delAssIt.next();
         // unlink:
         if (del.getAssignmentVersions() != null) {
            Set<OpAssignmentVersion> tmp = new HashSet<OpAssignmentVersion>(del.getAssignmentVersions());
            for (OpAssignmentVersion v : tmp) {
               del.removeAssignmentVersion(v);
            }
         }
         // delete
         plan.removeAssignment(del);
         broker.deleteObject(del);
      }
      
      logger.debug("TIMING: checkInProjectPlan #04: "
            + (System.currentTimeMillis() - now));
      OpProjectCalendar pCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, planVersion);
      updateSuperActivityTree(session, broker, plan, planVersion, pCal, copyAct.getSuperActivityMap());

      logger.debug("TIMING: checkInProjectPlan #05: "
            + (System.currentTimeMillis() - now));
      CheckInDependenciesCopyHelper copyDep = new CheckInDependenciesCopyHelper(session, broker, plan);
      copyDep.copy(plan.getDependencies(), planVersion.getDependencyVersions());
      logger.debug("TIMING: checkInProjectPlan #06: "
            + (System.currentTimeMillis() - now));
      CheckInAttachmentsCopyHelper copyAtt = new CheckInAttachmentsCopyHelper(session, broker, plan);
      copyAtt.copy(getAttachmentsFromProjectPlan(plan),
            OpActivityVersionDataSetFactory
                  .getAttachmentVersionsFromPlanVersion(broker, planVersion));
      
      logger.debug("TIMING: checkInProjectPlan #07: "
            + (System.currentTimeMillis() - now));
      CheckInWorkBreakCopyHelper copyWb = new CheckInWorkBreakCopyHelper(session, broker, plan);
      copyWb.copy(plan.getWorkBreaks(), planVersion.getWorkBreaks());
      
      logger.debug("TIMING: checkInProjectPlan #08: "
            + (System.currentTimeMillis() - now));
      // update WBS codes:
      Map<String, Integer> wbsParentIndexMap = new HashMap<String, Integer>();
      List<OpActivity> updateWBSCode = new ArrayList<OpActivity>();
      OpQuery activitiesbySequenceQuery = broker.newQuery("select act from OpActivity as act where act.ProjectPlan.id = :projectPlanId order by act.Sequence");
      activitiesbySequenceQuery.setLong("projectPlanId", plan.getId());
      Iterator<OpActivity> ait = broker.iterate(activitiesbySequenceQuery);
      while (ait.hasNext()) {
         OpActivity a = ait.next();
         if (!updateWbsParentsMap(a, wbsParentIndexMap)) {
            updateWBSCode.add(a);
         }
      }
      Iterator<OpActivity> awbsit = updateWBSCode.iterator();
      while (awbsit.hasNext()) {
         OpActivity a = awbsit.next();
         if ((a.getAttributes() & OpGanttValidator.IMPORTED_FROM_SUBPROJECT) == OpGanttValidator.IMPORTED_FROM_SUBPROJECT) {
            // no wbs code for imported stuff!
            continue;
         }
         setNextCode(a, wbsParentIndexMap);
      }

      plan.setRecalculated(planVersion.getRecalculated());

      logger.debug("TIMING: checkInProjectPlan #09: "
            + (System.currentTimeMillis() - now));
   }

   public Set<OpAssignmentVersion> bulkFetchAssignmentVersions(OpBroker broker,
         OpProjectPlanVersion planVersion) {
      OpQuery q = broker.newQuery("select ass.id from OpAssignmentVersion as ass where ass.PlanVersion.id = :planVersionId");
      q.setLong("planVersionId", planVersion.getId());
      Set<Long> allIds = new HashSet<Long>();
      
      OpQuery bulkQ = broker.newQuery("select assV from "
            + "OpAssignmentVersion as assV "
            + "left join fetch assV.Assignment as ass "
            + "left join fetch ass.Activity as act "
            + "where assV.id in (:ids)");
      Set<OpAssignmentVersion> asv = bulkFetch(broker, q, bulkQ, "ids");
      return asv;
   }

   public Set<OpAssignment> bulkFetchAssignments(OpBroker broker,
         OpProjectPlan plan) {
      OpQuery q = broker.newQuery("select ass.id from OpAssignment as ass where ass.ProjectPlan.id = :planId");
      q.setLong("planId", plan.getId());
      Set<Long> allIds = new HashSet<Long>();
      
      OpQuery bulkQ = broker.newQuery("select ass from "
            + "OpAssignment as ass "
            + "left join fetch ass.Activity as act "
            + "where ass.id in (:ids)");
      Set<OpAssignment> as = bulkFetch(broker, q, bulkQ, "ids");
      return as;
   }

   public Set<OpDependencyVersion> bulkFetchDependencyVersions(OpBroker broker,
         OpProjectPlanVersion planVersion) {
      OpQuery q = broker.newQuery("select dep.id from OpDependencyVersion as dep where dep.PlanVersion.id = :planVersionId");
      q.setLong("planVersionId", planVersion.getId());
      Set<Long> allIds = new HashSet<Long>();
      
      OpQuery bulkQ = broker.newQuery("select dep from "
            + "OpDependencyVersion as dep "
            + "left join fetch dep.SuccessorVersion as succ "
            + "left join fetch dep.PredecessorVersion as pred "
            + "where dep.id in (:ids)");
      Set<OpDependencyVersion> deps = bulkFetch(broker, q, bulkQ, "ids");
      return deps;
   }

   public Set<OpDependency> bulkFetchDependencies(OpBroker broker,
         OpProjectPlan plan) {
      OpQuery q = broker.newQuery("select dep.id from OpDependency as dep where dep.ProjectPlan.id = :planId");
      q.setLong("planId", plan.getId());
      Set<Long> allIds = new HashSet<Long>();
      
      OpQuery bulkQ = broker.newQuery("select dep from "
            + "OpDependency as dep "
            + "left join fetch dep.SuccessorActivity as succ "
            + "left join fetch dep.PredecessorActivity as pred "
            + "where dep.id in (:ids)");
      Set<OpDependency> deps = bulkFetch(broker, q, bulkQ, "ids");
      return deps;
   }

   public Set bulkFetch(OpBroker broker, OpQuery idQuery, OpQuery bulkQuery, String bulkQueryIdsField) {
      long now = System.currentTimeMillis();
      Set<OpObjectIfc> asv = new HashSet<OpObjectIfc>(1000);
      Set<Long> allIds = new HashSet<Long>();
      Iterator<Long> qit = broker.iterate(idQuery);
      while (qit.hasNext()) {
         allIds.add(qit.next());
      }
      logger.debug("TIMING: bulkFetchVersions #00: "
            + (System.currentTimeMillis() - now));
      Iterator<Long> memIt = allIds.iterator();
      OpBulkFetchIterator<OpObjectIfc, Long> assit = new OpBulkFetchIterator<OpObjectIfc, Long>(
            broker, memIt, bulkQuery, new LongIdConverter(), bulkQueryIdsField);

      while(assit.hasNext()) {
         asv.add(assit.next());
      }
      logger.debug("TIMING: bulkFetchVersions #01: "
            + (System.currentTimeMillis() - now));
      return asv;
   }

   public static void updateSuperActivityTree(OpProjectSession session,
         OpBroker broker, OpProjectPlan plan, OpProjectPlanVersion planVersion, OpProjectCalendar pCal,
         Map<Long, OpActivityVersion> superActivityMap) {
      // setup super activities:
      for (OpActivity a : plan.getActivities()) {
         if (a.getDeleted()) {
            continue;
         }
         // this should never fail, because 
         OpActivityVersion sav = superActivityMap.get(new Long(a.getId()));
         if (a.getSuperActivity() != null) {
            logger.warn("Should not have super anymore... : " + a + " -> " + a.getSuperActivity());
            // we should never get here, but anyway...
            a.getSuperActivity().removeSubActivity(a);
         }
         if (sav != null) {
            sav.getActivity().addSubActivity(a);
         }
      }

      // clear aggregated data...
      plan.resetAggregatedValues();
      for (OpActivity a : plan.getActivities()) {
         if (a.getDeleted()) {
            continue;
         }
         if (!a.hasAggregatedValues()) {
            updateParents(a, pCal, plan.getProgressTracked());
         }
      }
      
      for (OpActivity a : plan.getActivities()) {
         if (a.getDeleted()) {
            continue;
         }
         if (a.hasDerivedStartFinish()) {
            OpActivityVersionDataSetFactory.adjustDurationForCollection(a, pCal);
         }
      }
      
      adjustStartFinishForPlan(planVersion, plan, pCal);
   }

   private static void adjustStartFinishForPlan(OpProjectPlanVersion version, 
         OpProjectPlan plan, OpProjectCalendar pCal) {
      if (plan.getStart() == null) {
         plan.setStart(version.getStart());
      }
      if (plan.getFinish() == null) {
         plan.setFinish(version.getFinish() != null ? version.getFinish()
               : plan.getStart());
      }
      
      OpActivityVersionDataSetFactory.adjustDurationForCollection(plan, pCal);
   }
   public CheckInActivityCopyHelper synchronizeActivitiesWithVersions(
         OpProjectSession session, OpBroker broker,
         OpProjectPlanVersion planVersion, OpProjectPlan plan,
         boolean updateDeleted) {
      long now = System.currentTimeMillis();
      CheckInActivityCopyHelper copyAct = new CheckInActivityCopyHelper(session, broker, plan, updateDeleted);
      copyAct.copy(plan.getActivities(), planVersion.getActivityVersions());
      logger.debug("TIMING: synchronizeActivitiesWithVersions #01: "
            + (System.currentTimeMillis() - now));
      return copyAct;
   }
   
   // return false, if change of activity type is not possible:
   private boolean checkTypeChange(OpActivity a, Byte type) {
      if (type.equals(a.getType())) {
         return true;
      }
      return !a.hasWorkRecords();
   }

   /**
    * Updates the sequence numbers for all ad-hoc tasks in a project plan.
    *
    * @param plan       a <code>OpProjectPlan</code> entity.
    * @param dataRowsNr a <code>int</code> representing the number of activities in the client-side represenation of the
    *                   project plan.
    * @param adhocTasks a <code>List</code> of <code>OpActivity(ADHOC_TASK)</code>.
    * @param broker     a <code>OpBroker</code> used for persistence operations.
    */
   private static void updateAdHocTasks(OpProjectPlan plan, int dataRowsNr, List adhocTasks, OpBroker broker) {
      Set activitySet = plan.getActivities();
      if (activitySet == null || activitySet.isEmpty()) {
         return;
      }
      int maxSeq = 0;
      for (Iterator iterator = activitySet.iterator(); iterator.hasNext();) {
         OpActivity activity = (OpActivity) iterator.next();
         if (activity.getType() != OpActivity.ADHOC_TASK && activity.getSequence() > maxSeq) {
            maxSeq = activity.getSequence();
         }
      }
      if (maxSeq < dataRowsNr) {
         maxSeq = dataRowsNr;
      }

      int sequence = maxSeq + 1;
      for (Iterator iterator = adhocTasks.iterator(); iterator.hasNext();) {
         OpActivity activity = (OpActivity) iterator.next();
         activity.setSequence(sequence);
         activity.setProjectPlan(plan);
         broker.updateObject(activity);
         sequence++;
      }
   }

   private static List getAdHocTasks(OpProjectPlan plan) {
      List adhocTasks = new ArrayList();
      if (plan.getActivities() != null) {
         Iterator i = plan.getActivities().iterator();
         while (i.hasNext()) {
            OpActivity activity = (OpActivity) i.next();
            if (activity.getType() == OpActivity.ADHOC_TASK) {
               adhocTasks.add(activity);
            }
         }
      }
      return adhocTasks;
   }

   public static void updateStartFinish(OpActivityIfc activity, Date start, Date finish, double leadTime, double followUpTime, boolean ignoreType) {
      if (ignoreType) {
         activity.setStart(start);
         activity.setFinish(finish);
         activity.setLeadTime(leadTime);
         activity.setFollowUpTime(followUpTime);
      }
      else {
         switch (activity.getType()) {
         case OpActivity.STANDARD:
         case OpActivity.SCHEDULED_TASK:
            activity.setStart(start);
            activity.setFinish(finish);
            activity.setLeadTime(leadTime);
            activity.setFollowUpTime(followUpTime);
            break;
         case OpActivity.MILESTONE:
            activity.setStart(start);
            activity.setFinish(activity.getStart());
            activity.setDuration(0d);
            break;
         case OpActivity.TASK:
            activity.setStart(activity.getProjectPlan().getProjectNode().getStart());
            activity.setFinish(activity.getProjectPlan().getProjectNode().getFinish());
            activity.setDuration(0d);
            break;
         case OpActivity.COLLECTION:
         case OpActivity.COLLECTION_TASK:
            activity.setStart(null);
            activity.setFinish(null);
            break;
         case OpActivity.ADHOC_TASK:
            activity.setStart(start);
            activity.setFinish(finish);
            break;
         }
      }
   }
   
   private static void updateParents(OpActivity activity,
         OpProjectCalendar projectCalendar, boolean progressTracked) {
      if (!activity.isImported() || activity.isSubProjectReference()) {
         updateVirtualParents(activity, activity.getParent() , projectCalendar, progressTracked);
      }
   }
   
   private static void updateVirtualParents(OpActivityValuesIfc activity,
         OpActivityValuesIfc virtualParent, OpProjectCalendar calendar,
         boolean progressTracked) {
      while (virtualParent != null) {
         OpActivityVersionDataSetFactory.updateParentAggregatedValues(activity, virtualParent, calendar, progressTracked);
         virtualParent = virtualParent.getParent();
      }
   }

   private static double calculateAssignmentRemainingEffort(
         double activityBaseEffort, double activityRemainingEffort,
         double assignmentBaseEffort) {
      double assignmentRemainigEffort = 0;
      if (activityBaseEffort > 0) {
         assignmentRemainigEffort = activityRemainingEffort * assignmentBaseEffort / activityBaseEffort;
      }
      return assignmentRemainigEffort;
   }

   /**
    * Transforms from work phases on a given activity into work periods.
    *
    * @param dataRow row to get the work periods for.
    * @return <code>Map</code>0 representing the work period list, where key=period start, value=<code>List</code>
    *         containing working days as <code>Long</code> and base effort per day as <code>Double</code>
    */
   public static Map getWorkPeriods(XComponent dataRow) {
      Map workPeriods = new TreeMap();
      SortedMap workPhases = OpGanttValidator.getWorkPhases(dataRow);
      if (workPhases == null || workPhases.isEmpty()) {
         return workPeriods; //no work phase -> no work periods
      }
      List<Date> workPhaseBoundariesSorted = new ArrayList();
      for (Iterator iterator = workPhases.entrySet().iterator(); iterator.hasNext();) {
         Map.Entry entry = (Map.Entry) iterator.next();
         workPhaseBoundariesSorted.add((Date) entry.getKey());
         workPhaseBoundariesSorted.add((Date) ((Map)entry.getValue()).get(OpGanttValidator.WORK_PHASE_FINISH_KEY));
      }
      
      Date wphStart = null;
      Date currentDate = workPhaseBoundariesSorted.get(0);
      Date lastDate = new Date(workPhaseBoundariesSorted.get(
            workPhaseBoundariesSorted.size() - 1).getTime()
            + XCalendar.MILLIS_PER_DAY);
      int boundaryIndex = 0;
      boolean inWorkPhase = false;

      long binWorkDays = 0;
      int periodDayIndex = 0;
      double daysPerPeriod = 0d; 

      double overallDays = 0d;
      
      Date currentPeriodStart = null;
      while (currentDate.before(lastDate)) {
         // check for phase shift:
         if (currentDate.equals(workPhaseBoundariesSorted.get(boundaryIndex))) {
            inWorkPhase = !inWorkPhase;
            boundaryIndex++;
         }
         // check for new period:
         Date periodStart = getPeriodStartForDate(currentDate);
         if (currentPeriodStart == null || !currentPeriodStart.equals(periodStart)) {
            // open new period:
            if (currentPeriodStart != null) {
               // save old data:
               List workPeriodValues = new ArrayList();
               workPeriodValues.add(new Long(binWorkDays));
               workPeriodValues.add(new Double(daysPerPeriod));
               workPeriods.put(currentPeriodStart, workPeriodValues);
            }
            else {
               // setup 
            }
            binWorkDays = 0;
            daysPerPeriod = 0d;
            periodDayIndex = (int) ((currentDate.getTime() - periodStart.getTime()) / XCalendar.MILLIS_PER_DAY);
            currentPeriodStart = periodStart;
         }
         binWorkDays = binWorkDays | (inWorkPhase ? (1 << periodDayIndex) : 0);  
         // advance one day...
         periodDayIndex++;
         if (inWorkPhase) {
            daysPerPeriod += 1d;
            overallDays += 1d;
         }
         currentDate = new Date(currentDate.getTime() + XCalendar.MILLIS_PER_DAY);
      }
      
      if (currentPeriodStart != null && binWorkDays != 0) {
         // save old data:
         List workPeriodValues = new ArrayList();
         workPeriodValues.add(new Long(binWorkDays));
         workPeriodValues.add(new Double(daysPerPeriod));
         workPeriods.put(currentPeriodStart, workPeriodValues);
      }

      // this is somewhat ugly, but we need the actual number of days here...
      double baseEffortPerDay = OpGanttValidator.getBaseEffort(dataRow) / overallDays;
      Iterator<List> wpIt = workPeriods.values().iterator();
      while (wpIt.hasNext()) {
         List wp = wpIt.next();
         wp.set(1, new Double(((Double)wp.get(1)).doubleValue() * baseEffortPerDay));
      }
      return workPeriods;
   }

   private static Date getPeriodStartForDate(Date date) {
      Calendar c = Calendar.getInstance();
      c.setTime(date);
      c.set(Calendar.DAY_OF_MONTH, 1);
      date = new Date(c.getTimeInMillis());
      return date;
   }


   /**
    * Creates an <code>OpAttachment</code> entity out o a list of attachment atributes.
    *
    * @param broker - the <code>OpBroker</code> needed to perform the DB operations.
    * @param object - the <code>OpObject</code> entity for which the attachments is created
    *                   (it must be an <code>OpActivity</code>, a <code>OpProjectNode</code> or
    *                   a <code>OpCostRecord</code> object).
    * @param attachmentElement   - the <code>List</code> of attachment attributes
    * @param reusableAttachments - the list of already created attachments that need to be updated
    * @return - the newly created/updated <code>OpAttachment</code> entity , could be <code>null</code> if the content id is not valid
    */
   public static OpAttachment createAttachment(OpBroker broker, OpObjectIfc object, List attachmentElement,
        List reusableAttachments) {
      OpAttachment attachment;
      if ((reusableAttachments != null) && (reusableAttachments.size() > 0)) {
         attachment = (OpAttachment) reusableAttachments.remove(reusableAttachments.size() - 1);
      }
      else {
         attachment = new OpAttachment();
      }
      attachment.setObject(object);

      attachment.setLinked(OpProjectConstants.LINKED_ATTACHMENT_DESCRIPTOR.equals(attachmentElement.get(0)));
      attachment.setName((String) attachmentElement.get(2));
      attachment.setLocation((String) attachmentElement.get(3));

      if (!attachment.getLinked()) {
         String contentId = (String) attachmentElement.get(4);
         if (OpLocator.validate(contentId)) {
            OpContent content = (OpContent) broker.getObject(contentId);
            OpContentManager.updateContent(content, broker, true, attachment);
            attachment.setContent(content);
         }
         else {
            logger.warn("The attachment " + attachment.getName() + " was not persisted because the content was null");
            return null; // the content is not persisted due to some IO errors
         }
      }
      if (attachment.getId() == 0) {
         broker.makePersistent(attachment);
      }
      else {
         broker.updateObject(attachment);
      }

      return attachment;
   }

   // should work for version as well...
   public static void updateWorkBreaks(OpBroker broker, XComponent dataSet, Iterator<OpWorkBreak> workBreaks) {
      Map<String, Map<String, Object>> updatedWBs = new HashMap<String, Map<String,Object>>();
      Map<String, String> wbActivityMap = new HashMap<String, String>();
      Map<String, SortedMap<Double, Map<String, Object>>> newWBsMap = new HashMap<String, SortedMap<Double,Map<String,Object>>>();
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         // ROWs:
         XComponent row = (XComponent) dataSet.getChild(i);
         String activityLocator = row.getStringValue();
         SortedMap<Double, Map<String, Object>> wbs = OpGanttValidator.getWorkBreaks(row);
         Iterator<Double> wbIt = wbs.keySet().iterator();
         while (wbIt.hasNext()) {
            Map<String, Object> wbInfo = wbs.get(wbIt.next());
            String loc = (String) wbInfo.get(OpGanttValidator.WORK_BREAK_LOCATOR);   
            Double start = (Double) wbInfo.get(OpGanttValidator.WORK_BREAK_START);
            Double duration = (Double) wbInfo.get(OpGanttValidator.WORK_BREAK_DURATION);
            
            if (loc != null && OpLocator.parseLocator(loc).getID() != 0) {
               // already present:
               updatedWBs.put(loc, wbInfo);
               wbActivityMap.put(loc, activityLocator);
            }
            else {
               insertWorkBreakIntoActivityMap(newWBsMap, activityLocator,
                     wbInfo);
            }
         }
      }
      // update existing DB-WorkBreaks..
      if (workBreaks != null) {
         while (workBreaks.hasNext()) {
            OpWorkBreak wb = workBreaks.next();
            Map<String, Object> wbInfo = updatedWBs.remove(wb.locator());
            if (wbInfo != null) {
               wb.setStart(((Double) wbInfo.get(OpGanttValidator.WORK_BREAK_START)).doubleValue());
               wb.setDuration(((Double) wbInfo.get(OpGanttValidator.WORK_BREAK_DURATION)).doubleValue());
            }
            else {
               workBreaks.remove();
               broker.deleteObject(wb);
            }
         }
      }
      // check for lost updates (everytime versions are checked in ...)
      Iterator<String> leftUpdates = updatedWBs.keySet().iterator();
      while (leftUpdates.hasNext()) {
         String loc = leftUpdates.next();
         insertWorkBreakIntoActivityMap(newWBsMap, wbActivityMap.get(loc), updatedWBs.get(loc));
      }
      // create new WorkBreaks:
      Iterator<String> newWBsIt = newWBsMap.keySet().iterator();
      while (newWBsIt.hasNext()) {
         String actLocator = newWBsIt.next();
         SortedMap<Double, Map<String, Object>> actWBs = newWBsMap.get(actLocator);
         OpActivityIfc act = (OpActivityIfc) broker.getObject(actLocator);
         Iterator<Double> wbIt = actWBs.keySet().iterator();
         while (wbIt.hasNext()) {
            Map<String, Object> wbInfo = actWBs.get(wbIt.next());
            OpWorkBreak wb = null;
            Double start = (Double) wbInfo.get(OpGanttValidator.WORK_BREAK_START);
            Double duration = (Double) wbInfo.get(OpGanttValidator.WORK_BREAK_DURATION);
            if (act instanceof OpActivity) {
               OpActivity a = (OpActivity) act;
               wb = new OpActivityWorkBreak(start.doubleValue(), duration.doubleValue());
               a.addWorkBreak(wb);
               a.getProjectPlan().addWorkBreak((OpActivityWorkBreak) wb);
            }
            else if (act instanceof OpActivityVersion) {
               OpActivityVersion av = (OpActivityVersion) act;
               wb = new OpActivityVersionWorkBreak(start.doubleValue(), duration.doubleValue());
               av.addWorkBreak(wb);
               av.getPlanVersion().addWorkBreak((OpActivityVersionWorkBreak) wb);
            }
            broker.makePersistent(wb);
         }
      }
   }

   private static void insertWorkBreakIntoActivityMap(
         Map<String, SortedMap<Double, Map<String, Object>>> newWBsMap,
         String activityLocator, Map<String, Object> wbInfo) {
      SortedMap<Double, Map<String, Object>> actNewWBs =  newWBsMap.get(activityLocator);
      if (actNewWBs == null) {
         actNewWBs = new TreeMap<Double, Map<String,Object>>();
      }
      actNewWBs = OpGanttValidator.addWorkBreak(actNewWBs, wbInfo);
      newWBsMap.put(activityLocator, actNewWBs);
   }
   

   /**
    * Performs equality checking for the given args.
    *
    * @param arg1 <code>String</code> first argument
    * @param arg2 <code>String</code> second password
    * @return boolean flag indication passwords equality
    */
   static boolean checkEquality(String arg1, String arg2) {
      if (arg1 != null && arg2 != null) {
         return arg1.equals(arg2);
      }
      if (arg1 != null) {
         return arg1.equals(arg2);
      }
      if (arg2 != null) {
         return arg2.equals(arg1);
      }
      return arg1 == arg2;
   }


   /**
    * Fills the category color data set with the necessary data.
    *
    * @param broker  <code>OpBroker</code> used to query the categories
    * @param dataSet <code>XComponent.DATA_SET</code> to add the categories to
    */
   public static void fillCategoryColorDataSet(OpBroker broker, XComponent dataSet) {
      OpQuery query = broker.newQuery("select category from OpActivityCategory as category");
      Iterator categories = broker.iterate(query);
      OpActivityCategory category = null;
      XComponent dataRow = null;
      XComponent dataCell = null;
      while (categories.hasNext()) {
         category = (OpActivityCategory) categories.next();
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(category.locator());
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setIntValue(category.getColor());
         dataRow.addChild(dataCell);
         dataSet.addChild(dataRow);
      }
   }

   /**
    * Calculates percentage deviation for given base and deviation.
    *
    * @param base      base value
    * @param deviation devation value
    * @return % deviation.
    */
   public static double calculatePercentDeviation(double base, double deviation) {
      if (base != 0) {
         return deviation * 100 / base;
      }
      else {
         if (deviation != 0) {
            return Double.MAX_VALUE;
         }
         else {
            return 0;
         }
      }
   }

   /**
    * Updates the base personnel costs and base proceeds of the activities that belong to the assignments
    * passed as parameter
    *
    * @param broker             - a <code>OpBroker</code> used for performing business operations.
    * @param updatedAssignments - a <code>List</code> of <code>OpAssignmentVersion</code> that contain the
    *                           <code>OpActivityVersion</code> to be updated
    */
   public static boolean updateActivityVersionPersonnelCosts(OpBroker broker, List<OpAssignmentVersion> updatedAssignments) {
      OpActivityVersion activityVersion;
      boolean updated = false;

      Set<OpActivityVersion> updatedActivities = new HashSet<OpActivityVersion>();

      for (OpAssignmentVersion assignmentVersion : updatedAssignments) {
         double sumBaseCosts = 0;
         double sumBaseProceeds = 0;
         activityVersion = assignmentVersion.getActivityVersion();
         //when an activity udates it's costs, the update takes place for all the assignments that belong to the activity
         //thus only the first assignment of the activity "updates" the activity, the rest of the activity assignments do nothing
         if (!updatedActivities.contains(activityVersion)) {
            double oldPersonnelCosts = activityVersion.getBasePersonnelCosts();
            double oldProceedsCosts = activityVersion.getBaseProceeds();

            for (OpAssignmentVersion assignmentVersionOfActivity : activityVersion.getAssignmentVersions()) {
               sumBaseCosts += assignmentVersionOfActivity.getBaseCosts();
               sumBaseProceeds += assignmentVersionOfActivity.getBaseProceeds();
            }
            activityVersion.setBasePersonnelCosts(sumBaseCosts);
            activityVersion.setBaseProceeds(sumBaseProceeds);
            updatedActivities.add(activityVersion);

            //update all super activities
            while (activityVersion.getSuperActivityVersion() != null) {
               OpActivityVersion superActivityVersion = activityVersion.getSuperActivityVersion();
               double personnelCostsDifference = activityVersion.getBasePersonnelCosts() - oldPersonnelCosts;
               double proceedsCostsDifference = activityVersion.getBaseProceeds() - oldProceedsCosts;
               oldPersonnelCosts = superActivityVersion.getBasePersonnelCosts();
               oldProceedsCosts = superActivityVersion.getBaseProceeds();
               superActivityVersion.setBasePersonnelCosts(superActivityVersion.getBasePersonnelCosts() + personnelCostsDifference);
               superActivityVersion.setBaseProceeds(superActivityVersion.getBaseProceeds() + proceedsCostsDifference);
               updated = true;
               broker.updateObject(activityVersion);
               activityVersion = superActivityVersion;
            }
            broker.updateObject(activityVersion);
         }
      }
      return updated;
   }

   /**
    * Updates the actual personnel costs and actual proceeds of the activities that belong to the assignments
    * passed as parameter
    *
    * @param broker             - a <code>OpBroker</code> used for performing business operations.
    * @param updatedAssignments - a <code>List</code> of <code>OpAssignment</code> that contain the
    *                           <code>OpActivity</code> to be updated
    */
   public static void updateActivityActualCosts(OpBroker broker, List<OpAssignment> updatedAssignments) {
      Set<OpActivity> updatedActivities = new HashSet<OpActivity>();
      OpActivity activity;

      for (OpAssignmentIfc assignment : updatedAssignments) {
         double sumActualCosts = 0;
         double sumActualProceeds = 0;
         double sumRemainingPersonnelCosts = 0d;
         double sumRemainingProceeds = 0d;
         activity = (OpActivity) assignment.getActivity();
         if (!updatedActivities.contains(activity)) {
            double oldActualCosts = activity.getActualPersonnelCosts();
            double oldActualProceeds = activity.getActualProceeds();
            double oldRemainingPersonellCosts = activity.getRemainingPersonnelCosts();
            double oldRemainingProceeds = activity.getRemainingProceeds();

            for (OpAssignment assignmentOfActivity : activity.getAssignments()) {
               sumActualCosts += assignmentOfActivity.getActualCosts();
               sumActualProceeds += assignmentOfActivity.getActualProceeds();
               sumRemainingPersonnelCosts += assignmentOfActivity.getRemainingPersonnelCosts();
               sumRemainingProceeds += assignmentOfActivity.getRemainingProceeds();
            }
            activity.setActualPersonnelCosts(sumActualCosts);
            activity.setActualProceeds(sumActualProceeds);
            activity.setRemainingPersonnelCosts(sumRemainingPersonnelCosts);
            activity.setRemainingProceeds(sumRemainingProceeds);
            updatedActivities.add(activity);

            //update all super activities
            while (activity.getSuperActivity() != null) {
               OpActivity superActivity = activity.getSuperActivity();
               double actualCostsDifference = activity.getActualPersonnelCosts() - oldActualCosts;
               double actualProceedsDifference = activity.getActualProceeds() - oldActualProceeds;
               double remainingPersonnelCostsDifference = activity.getRemainingPersonnelCosts() - oldRemainingPersonellCosts;
               double remainingProceedsDifference = activity.getRemainingProceeds() - oldRemainingProceeds;
               
               oldActualCosts = superActivity.getActualPersonnelCosts();
               oldActualProceeds = superActivity.getActualProceeds();
               oldRemainingPersonellCosts = superActivity.getRemainingPersonnelCosts();
               oldRemainingProceeds = superActivity.getRemainingProceeds();
               
               superActivity.setActualPersonnelCosts(oldActualCosts + actualCostsDifference);
               superActivity.setActualProceeds(oldActualProceeds + actualProceedsDifference);
               superActivity.setRemainingPersonnelCosts(oldRemainingPersonellCosts + remainingPersonnelCostsDifference);
               superActivity.setRemainingProceeds(oldRemainingProceeds + remainingProceedsDifference);
               
               broker.updateObject(activity);
               activity = superActivity;
            }
            broker.updateObject(activity);
         }
      }
   }

   public static SortedMap<Date, OpWorkPeriodIfc> getSortedWorkPeriodsForActivity(OpActivityIfc activity) {
      Set<? extends OpWorkPeriodIfc> wps = activity.getWorkPeriods();
      return getSortedWorkPeriods(wps);
   }

   private static SortedMap<Date, OpWorkPeriodIfc> getSortedWorkPeriods(
         Set<? extends OpWorkPeriodIfc> wps) {
      SortedMap<Date, OpWorkPeriodIfc> sortedPeriods = new TreeMap<Date, OpWorkPeriodIfc>();
      if (wps == null || wps.isEmpty()) {
         return sortedPeriods;
      }
      Iterator<? extends OpWorkPeriodIfc> wpit = wps.iterator();
      while (wpit.hasNext()) {
         OpWorkPeriodIfc wp = wpit.next();
         sortedPeriods.put(wp.getStart(), wp);
      }
      return sortedPeriods;
   }
   
   /**
    * return the workperiod, the day belongs to or null, if not a workday.
    * @param day
    * @param sortedPeriods
    * @return
    */
   public static OpWorkPeriodIfc getWorkPeriodForWorkDay(Date day, SortedMap<Date, OpWorkPeriodIfc> sortedPeriods) {
      // first, find matching workperiod:
      OpWorkPeriodIfc matching = null;
      Iterator<Entry<Date, OpWorkPeriodIfc>> wpit = sortedPeriods.entrySet().iterator();
      Date periodStartDate = getPeriodStartForDate(day);
      while (matching == null && wpit.hasNext()) {
         Map.Entry<Date, OpWorkPeriodIfc> entry = wpit.next();
         if (periodStartDate.equals(entry.getValue().getStart())) {
            matching = entry.getValue();
         }
         else if (periodStartDate.before(entry.getValue().getStart())) {
            // will not be part of it...
            break;
         }
      }
      if (matching == null) {
         return null;
      }
      long daysDiff = (day.getTime() - matching.getStart().getTime()) / XCalendar.MILLIS_PER_DAY;
      long test = 1 << daysDiff;
      
      return ((matching.getWorkingDays() & test) == test) ? matching : null;
   }
   
   /**
    * Updates the work monts for the given assignment.
    *
    * @param broker     Broker access object.
    * @param assignment Assignment to update the work months for.
    * @param projectCal  Session calendar.
    */
   public static void updateWorkMonths(OpProjectSession session,
         OpBroker broker, OpAssignment assignment) {

      OpActivity activity = assignment.getActivity();
      if (activity.getType() == OpActivity.MILESTONE) {
         return; // no workmonths form milestones
      }

      SortedMap<Date, OpWorkPeriodIfc> sortedWorkPeriods = getSortedWorkPeriodsForActivity(assignment.getActivity());
      
      OpProjectNodeAssignment projectAssignment = assignment.getProjectNodeAssignment();

      Set<OpWorkMonth> opWorkMonths = assignment.getWorkMonths();
      if (opWorkMonths != null) {
         List<OpWorkMonth> toBeDeleted = new ArrayList<OpWorkMonth>(opWorkMonths);
         for (OpWorkMonth wm : toBeDeleted) {
            assignment.removeWorkMonth(wm);
            broker.deleteObject(wm);
         }
      }

      OpProjectCalendar resCal = OpProjectCalendarFactory.getInstance()
            .getCalendar(session, broker, assignment.getResource(),
                  assignment.getProjectPlan());
      
      double workHoursPerDay = resCal.getWorkHoursPerDay();

      Date start = activity.getStart();
      Date finish = getFinishDateFromWorkPeriods(sortedWorkPeriods, start);

      if (start == null || finish == null) {
         return;
      }

      double internalSum = 0;
      double externalSum = 0;
      byte workingDays = 0;
      long totalWorkingDays = 0;
      
      Date date = new Date(start.getTime());

      Calendar calendar = resCal.cloneCalendarInstance();
      // initaialize this...
      calendar.setTime(date);
      int month = calendar.get(Calendar.MONTH);
      int year = calendar.get(Calendar.YEAR);

      while (!date.after(finish)) {
         boolean workingDay = getWorkPeriodForWorkDay(date, sortedWorkPeriods) != null;
         // logger.debug("Date:" + date + " wd:" + workingDay);

         if (workingDay) {
            List<Double> rates = projectAssignment.getRatesForDay(date, true);
            double internalRate = rates.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
            double externalRate = rates.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
            internalSum += internalRate * workHoursPerDay * assignment.getAssigned() / 100;
            externalSum += externalRate * workHoursPerDay * assignment.getAssigned() / 100;
            workingDays++;
         }
         date = new Date(date.getTime() + OpProjectCalendar.MILLIS_PER_DAY);

         calendar.setTime(date);
         if ((month != calendar.get(Calendar.MONTH) || year != calendar
               .get(Calendar.YEAR))
               || date.after(finish)) {
            //new workmonth entity... set the values on the previous one.
            double latestEffort = workingDays * workHoursPerDay * assignment.getAssigned() / 100;

            OpWorkMonth workMonth = new OpWorkMonth();
            broker.makePersistent(workMonth);
            assignment.addWorkMonth(workMonth);
            workMonth.setMonth((byte) month);
            workMonth.setYear(year);
            workMonth.setLatestAssigned(assignment.getAssigned());
            workMonth.setLatestEffort(latestEffort);
            workMonth.setLatestPersonnelCosts(internalSum);
            workMonth.setLatestProceeds(externalSum);

            workMonth.setBaseAssigned(0);
            workMonth.setBaseEffort(0);
            workMonth.setBasePersonnelCosts(0);
            workMonth.setBaseProceeds(0);

            workMonth.setRemainingEffort(0d);
            workMonth.setRemainingPersonnel(0d);
            workMonth.setRemainingProceeds(0d);

            workMonth.setWorkingDays(workingDays);

            //reset counters
            totalWorkingDays += workingDays;
            workingDays = 0;
            internalSum = 0;
            externalSum = 0;
            month = calendar.get(Calendar.MONTH);
            year = calendar.get(Calendar.YEAR);
         }
      }

      updateRemainingValues(broker, resCal, assignment, sortedWorkPeriods);
   }

   /**
    * Updates the remaining personnel cost/proceeds for the given assignment on its workmonths.
    *
    * @param broker
    * @param projectCalendar  Session calendar.
    * @param assignment Assignment to make the update for.
    * @param sortedWorkPeriods 
    */
   private static void updateRemainingValues(OpBroker broker,
         OpProjectCalendar resourceCalendar, OpAssignment assignment,
         SortedMap<Date, OpWorkPeriodIfc> sortedWorkPeriods) {

      OpProjectNodeAssignment projectAssignment = assignment.getProjectNodeAssignment();

      OpActivity activity = assignment.getActivity();
      if (activity.getType() == OpActivity.MILESTONE) {
         return; // no workmonths form milestones
      }

      double actualEffort = assignment.getActualEffort();
      Set<OpWorkMonth> workMonths = assignment.getWorkMonths();

      double remainingEffort = assignment.getRemainingEffort();

      Calendar calendar = resourceCalendar.cloneCalendarInstance();
      Date start = activity.getStart();
      Date finish = getFinishDateFromWorkPeriods(sortedWorkPeriods, start);
      if (start == null || finish == null) {
         return;
      }

      Date date = new Date(start.getTime());
      calendar.setTime(date);

      //reset the remaining values and calculate the total nr of days
      for (OpWorkMonth workMonth : workMonths) {
         workMonth.setRemainingPersonnel(0d);
         workMonth.setRemainingProceeds(0d);
         workMonth.setRemainingEffort(0d);
      }

      double workHoursPerDay = resourceCalendar.getWorkHoursPerDay() * assignment.getAssigned() / 100;
      //find the new start date to distribute the remaining effort
      if (workHoursPerDay > 0d) {
         while (actualEffort > workHoursPerDay && date.before(finish)) {
            boolean workingDay = (getWorkPeriodForWorkDay(date, sortedWorkPeriods) != null);
            if (workingDay) {
               actualEffort -= workHoursPerDay;
            }
            date = new Date(date.getTime() + OpProjectCalendar.MILLIS_PER_DAY);
         }
      }
      else {
         // zero must not be distributed...
         date = finish;
      }
      //distribute the remaining effort starting from date...
      boolean carry = !date.before(finish);
      int workingDays = 0;
      if (carry) {
         // if there are no days left, the day before finish will get all the remaining costs
         date = new Date(finish.getTime());
         workingDays = 1;
      }
      else {
         Date tmp = date;
         while (!tmp.after(finish)) {
            boolean workingDay = getWorkPeriodForWorkDay(tmp, sortedWorkPeriods) != null;
            if (workingDay) {
               workingDays++;
            }
            tmp = new Date(tmp.getTime() + OpProjectCalendar.MILLIS_PER_DAY);
         }
      }
      
      double remainingEffortPerDay = remainingEffort / workingDays;
      calendar.setTime(date);

      OpWorkMonth workMonth = assignment.getWorkMonth(calendar.get(Calendar.YEAR), (byte) calendar.get(Calendar.MONTH));
      if (workMonth == null) {
         logger.error("NO WORKMONTH? Assignment: " + assignment);
         return;
      }
      double internalSum = 0;
      double externalSum = 0;
      double workMonthRemainingEffort = 0;

      while (!date.after(finish)) {
         boolean workingDay = getWorkPeriodForWorkDay(date, sortedWorkPeriods) != null;
         
         if (workingDay || carry) {
            workingDays--;
            List<Double> rates = projectAssignment.getRatesForDay(date, true);
            double internalRate = rates.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
            double externalRate = rates.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
            internalSum += internalRate * remainingEffortPerDay;
            externalSum += externalRate * remainingEffortPerDay;
            workMonthRemainingEffort += remainingEffortPerDay;
         }

         date = new Date(date.getTime() + OpProjectCalendar.MILLIS_PER_DAY);
         calendar.setTime(date);
         if (workMonth.getMonth() != calendar.get(Calendar.MONTH)
               || workMonth.getYear() != calendar.get(Calendar.YEAR)
               || date.after(finish) || workingDays == 0) {

            workMonth.setRemainingPersonnel(internalSum);
            workMonth.setRemainingProceeds(externalSum);
            workMonth.setRemainingEffort(workMonthRemainingEffort);
            
            OpWorkMonth tmp = assignment.getWorkMonth(calendar.get(Calendar.YEAR), (byte) calendar.get(Calendar.MONTH));
            if (tmp != null) {
               workMonth = tmp;
   
               internalSum = 0;
               externalSum = 0;
               workMonthRemainingEffort = 0;
            }
         }
      }
      assignment.detachFromActivity(assignment.getActivity());
      assignment.updateRemainingPersonnelCosts();
      assignment.updateRemainingProceeds();
      assignment.attachToActivity(assignment.getActivity());
   }

   /**
    * Rebuilds the Predecessor and Successor cell value for each row in the <code>XComponent</code> data set
    * passed as parameter.
    *
    * @param dataSet       - the <code>XComponent</code> data set whose rows are modified.
    * @param oldIndexIdMap - a <code>Map<Integer, String></code> containing the old indexes as keys and the
    *                      String values of the data rows at those indexes as values.
    * @param newIdIndexMap - <code>Map<String, Integer></code> containing the data row's String values as keys
    *                      and the indexes of those data rows as values.
    */
   public static void rebuildPredecessorsSuccessorsIndexes(XComponent dataSet, Map<Integer, String> oldIndexIdMap,
        Map<String, Integer> newIdIndexMap) {
      XComponent dataRow;
      SortedMap<Integer, Object> oldPredecessors;
      SortedMap<Integer, Object> newPredecessors;
      SortedMap<Integer, Object> oldSuccesssors;
      SortedMap<Integer, Object> newSuccesssors;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);

         //replace the predecessors old indexes with the new ones
         oldPredecessors = OpGanttValidator.getPredecessors(dataRow);
         if (!oldPredecessors.isEmpty()) {
            newPredecessors = new TreeMap<Integer, Object>();
            for (Integer predecessor : oldPredecessors.keySet()) {
               String predecessorId = oldIndexIdMap.get(predecessor);
               newPredecessors.put(newIdIndexMap.get(predecessorId), oldPredecessors.get(predecessor));
            }
            OpGanttValidator.setPredecessors(dataRow, newPredecessors);
         }

         //replace the successors old indexes with the new ones
         oldSuccesssors = OpGanttValidator.getSuccessors(dataRow);
         if (!oldSuccesssors.isEmpty()) {
            newSuccesssors = new TreeMap<Integer, Object>();
            for (Integer successor : oldSuccesssors.keySet()) {
               String successorId = oldIndexIdMap.get(successor);
               newSuccesssors.put(newIdIndexMap.get(successorId), oldSuccesssors.get(successor));
            }
            OpGanttValidator.setSuccessors(dataRow, newSuccesssors);
         }
      }
   }

   /**
    * Returns <code>true</code> if the assignment specified as parameter has any work records with completed flag or <code>false</code> otherwise.
    *
    * @param broker     - the <code>OpBroker</code> object needed to perform DB operations.
    * @param assignment - the <code>OpAssignment</code> object.
    * @return <code>true</code> if the assignment specified as parameter has any work records with completed flag set or <code>false</code> otherwise.
    */
   public static boolean hasCompletedWorkRecord(OpBroker broker, OpAssignment assignment) {
      if (assignment.getWorkRecords() != null) {
         OpQuery query = broker.newQuery(GET_COMPLETED_WORK_RECORD_COUNT_FOR_ASSIGNMENT);
         query.setLong("assignmentId", assignment.getId());
         Number counter = (Number) broker.iterate(query).next();
         if (counter.intValue() > 0) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns <code>true</code> if the project assignment specified as parameter has any hourly rate periods or
    * <code>false</code> otherwise.
    *
    * @param broker            - the <code>OpBroker</code> object needed to perform DB operations.
    * @param projectAssignment - the <code>OpProjectNodeAssignment</code> object.
    * @return <code>true</code> if the project assignment specified as parameter has any hourly rate periods or
    *         <code>false</code> otherwise.
    */
   public static boolean hasHourlyRatesPeriods(OpBroker broker, OpProjectNodeAssignment projectAssignment) {
      if (projectAssignment.getHourlyRatesPeriods() != null) {
         OpQuery query = broker.newQuery(GET_HOURLY_RATES_PERIOD_COUNT_FOR_PROJECT_ASSIGNMENT);
         query.setLong("assignmentId", projectAssignment.getId());
         Number counter = (Number) broker.iterate(query).next();
         if (counter.intValue() > 0) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns the number of subactivities for the activity specified as parameter.
    *
    * @param broker   - the <code>OpBroker</code> object needed to perform DB operations.
    * @param activity - the <code>OpActivity</code> object.
    * @return the number of subactivities for the activity specified as parameter.
    */
   public static int getSubactivitiesCount(OpBroker broker, OpActivity activity) {
      if (activity.getSubActivities() != null) {
         OpQuery query = broker.newQuery(GET_SUBACTIVITIES_COUNT_FOR_ACTIVITY);
         query.setLong("activityId", activity.getId());
         Number counter = (Number) broker.iterate(query).next();
         return counter.intValue();
      }
      return 0;
   }

   /**
    * Returns an <code>Iterator</code> over the collection of attachments which are set on the activities belonging
    *    to the <code>OpProjectPlan</code> passed as parameter.
    * (Note: this method loads all the activities belonging to the project plan and all their attachments. Use only
    *    when these objects are already loaded.)
    *
    * @param projectPlan - the <code>OpProjectPlan</code> for which the attachments are returned.
    * @return an <code>Iterator</code> over the collection of attachments which are set on the activities belonging
    *    to the <code>OpProjectPlan</code> passed as parameter.
    */
   public static Collection<OpAttachment> getAttachmentsFromProjectPlan(
         OpProjectPlan projectPlan) {
      Map<String, OpAttachment> attachmentMap = new HashMap<String, OpAttachment>();
      for (OpActivity activity : projectPlan.getActivities()) {
         for (OpAttachment attachment : activity.getAttachments()) {
            if (attachmentMap.get(attachment.locator()) == null) {
               attachmentMap.put(attachment.locator(), attachment);
            }
         }
      }

      return attachmentMap.values();
   }
   
   protected void clone(OpProjectSession session, OpBroker broker, OpActivity tgt, OpActivityVersion src, boolean progressTracked) {
      src.resetActualValues();
      tgt.cloneSimpleMembers(src, progressTracked);
      if (tgt.hasAggregatedValues()) {
         tgt.resetAggregatedValues();
      }
   }
   
   public Map<Integer, Integer> updateSubProjectActivities(OpProjectSession session,
         OpBroker broker, OpProjectNode targetProject, OpProjectNode newSubProject, int offset,
         List<XComponent> oldSubSet, List<XComponent> resultDataSet, boolean editMode) {
      return null;
   }

   public void importSubProjectActivities(OpProjectSession session,
         OpBroker broker, XComponent activityDataSet) {
      // override for extended functionality ;-)      
   }
   
   public void setupSubProjectHeadActivityVersion(OpProjectNode pn,
         OpActivityVersion headActivity) {
   }

   public void copyValuesForSubProjectActivity(OpActivityValuesIfc planValues,
         OpActivityVersion headActivity) {
   }

   public void retrieveSubProjectActivities(OpProjectSession session,
         OpBroker broker, OpProjectCalendar calendar, String projectLocator,
         XComponent tmpDataSet) {
   }
}
