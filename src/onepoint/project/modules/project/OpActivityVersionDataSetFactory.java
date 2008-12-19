/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpBulkFetchIterator;
import onepoint.project.util.OpCollectionSynchronizationHelper;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.util.Pair;
import onepoint.project.util.OpBulkFetchIterator.LocatorIdConverter;
import onepoint.project.util.OpBulkFetchIterator.LongIdConverter;
import onepoint.util.XCalendar;

public class OpActivityVersionDataSetFactory {

   private static final XLog logger = XLogFactory
         .getLogger(OpActivityVersionDataSetFactory.class);

   private static final String GET_SUBACTIVITY_VERSION_COUNT_FOR_ACTIVITY_VERSION = "select count(activityVersion.id) from OpActivityVersion activityVersion where activityVersion.SuperActivityVersion = (:activityVersionId)";

   private static final String GET_ATTACHMENT_VERSIONS_FOR_PLAN_VERSION = "select attachmentVersion from OpAttachmentVersion attachmentVersion where attachmentVersion.ActivityVersion.PlanVersion.id = (:planVersionId)";

   private static OpActivityVersionDataSetFactory instance = new OpActivityVersionDataSetFactory();

   public void retrieveActivityVersionDataSet(OpProjectSession session,
         OpBroker broker, OpProjectPlanVersion planVersion, XComponent dataSet,
         boolean editable) {
      retrieveActivityVersionDataSet(session, broker, planVersion, dataSet, editable, 0, 0);
   }
   
   public void retrieveActivityVersionDataSet(OpProjectSession session,
         OpBroker broker, OpProjectPlanVersion planVersion, XComponent dataSet,
         boolean editable, int attributesToSet, int attributesToRemove) {

      if (planVersion == null) {
         // definitely nothing to do...
         return;
      }

      OpProjectCalendar pCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, planVersion);

      // Activities: Fill data set with activity data rows and create activity
      // data row map
      Map<String, XComponent> dataRowMap = new HashMap<String, XComponent>();

      // tried this one: no good idea to join Activity!
      long now = System.currentTimeMillis();
      OpQuery query = broker
            .newQuery("select activity.id from OpActivityVersion as activity where activity.PlanVersion.id = :planVersionId order by activity.Sequence");
      query.setLong("planVersionId", planVersion.getId());
      Iterator qIt = broker.iterate(query);
      logger.debug("TIMING: retrieveActivityVersionDataSet #00: "
            + (System.currentTimeMillis() - now));
      List<Long> allIds = new ArrayList<Long>();
      while (qIt.hasNext()) {
         allIds.add((Long) qIt.next());
      }
      Iterator<Long> memIt = allIds.iterator();
      
      Map<String, XComponent> importedActivityVersionsMap = new HashMap<String, XComponent>();
      OpBulkFetchIterator<OpActivityVersion, Long> ait = new OpBulkFetchIterator<OpActivityVersion, Long>(
            broker, memIt, broker.newQuery("select actV from "
                  + "OpActivityVersion as actV "
                  + "left join fetch actV.Activity as act "
                  + "where actV.id in (:ids) " + "order by actV.Sequence"),
            new LongIdConverter(), "ids");

      while (ait.hasNext()) {
         OpActivityVersion activity = ait.next();
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         OpActivityDataSetFactory.getInstance().retrieveActivityDataRow(
               planVersion.getProjectPlan(), planVersion, activity,
               dataRow, editable, attributesToSet, attributesToRemove);
         // TODO: HACK as a post-bug fix ???
         if (activity.getType() == OpActivityVersion.TASK
               || activity.getType() == OpActivityVersion.COLLECTION_TASK) {
            OpGanttValidator.setStart(dataRow, null);
            OpGanttValidator.setEnd(dataRow, null);
         }
         dataRowMap.put(activity.locator(), dataRow);
         dataSet.addChild(dataRow);
         if (activity.getMasterActivityVersion() != null) {
            importedActivityVersionsMap.put(activity.getMasterActivityVersion().locator(), dataRow);
         }
      }
      
      logger.debug("TIMING: retrieveActivityVersionDataSet #01: "
            + (System.currentTimeMillis() - now));

      String assignmentQuery = "select assV from OpAssignmentVersion as assV right outer join assV.ActivityVersion as actV where assV.PlanVersion.id = :planVersionId";
      OpQuery assQ = broker.newQuery(assignmentQuery);
      assQ.setLong("planVersionId", planVersion.getId());

      // Assignments: Fill resources and resource base efforts columns
      Iterator assignments = broker.iterate(assQ);
      OpAssignmentVersion assignment = null;
      Map<String, Double> resourceAvailability = new HashMap<String, Double>();
      // map of [activitySequence, sum(activity.assignments.baseEffort)]
      Map<String, Double> activityAssignmentsSum = new HashMap<String, Double>();
      while (assignments.hasNext()) {
         assignment = (OpAssignmentVersion) assignments.next();

         OpActivityVersion activity = assignment.getActivityVersion();
         String activityId = activity.locator();

         Double ass = (Double) activityAssignmentsSum.get(activityId);
         double effortSum = (ass != null ? ass.doubleValue() : 0d) + assignment.getBaseEffort();
         activityAssignmentsSum.put(activityId, new Double(effortSum));

         XComponent dataRow = dataRowMap.get(assignment.getActivityVersion()
               .locator());

         OpGanttValidator.addResource(dataRow, createResourceChoice(assignment, pCal));

         OpResource resource = assignment.getResource();
         OpGanttValidator.setResourceBaseEffort(dataRow, resource.locator(),
               assignment.getBaseEffort());

         resourceAvailability.put(resource.locator(), new Double(resource
               .getAvailable()));
      }

      // update the resources to take into account invisible resources
      // (independent planning only)
      logger.debug("TIMING: retrieveActivityVersionDataSet #02: "
            + (System.currentTimeMillis() - now));
      if (planVersion.getProjectPlan().getCalculationMode() == OpGanttValidator.INDEPENDENT) {
         Iterator<String> it = activityAssignmentsSum.keySet().iterator();
         while (it.hasNext()) {
            String activityId = it.next();
            XComponent dataRow = dataRowMap.get(activityId);
            Double assignmentsEffortSum = (Double) activityAssignmentsSum
                  .get(activityId);
            double unassignedEffort = OpGanttValidator.getBaseEffort(dataRow)
                  - assignmentsEffortSum.doubleValue();
            if (!OpGanttValidator.isZeroWithTolerance(unassignedEffort, assignmentsEffortSum.doubleValue())) {
               OpGanttValidator.addResource(dataRow, buildAssignmentChoice(
                     pCal, unassignedEffort, OpGanttValidator
                           .getDuration(dataRow),
                     OpGanttValidator.NO_RESOURCE_NAME,
                     OpGanttValidator.NO_RESOURCE_ID, unassignedEffort
                           / pCal.getWorkHoursPerDay() * 100d));
            }
         }
      }

      resourceAvailability.put(OpGanttValidator.NO_RESOURCE_ID,
            new Double(Integer.MAX_VALUE));
      logger.debug("TIMING: retrieveActivityVersionDataSet #03: "
            + (System.currentTimeMillis() - now));
      // retrieve assignments for imported resources:
      Iterator<String> iait = importedActivityVersionsMap.keySet().iterator();
      
      OpBulkFetchIterator<OpAssignmentVersion, String> eait = new OpBulkFetchIterator<OpAssignmentVersion, String>(
            broker, iait, broker.newQuery("select ass from "
                  + "OpAssignmentVersion as ass "
                  + "where ass.ActivityVersion.id in (:actIds)"),
            new LocatorIdConverter(), "actIds");
      while (eait.hasNext()) {
         OpAssignmentVersion assV = eait.next();
         if (assV.getActivityVersion() == null) {
            return;
         }
         resourceAvailability.put(assV.getResource().locator(), new Double(assV.getResource().getAvailable()));
         String actLoc = assV.getActivityVersion().locator();
         resourceAvailability.put(assV.getResource().locator(), new Double(assV.getResource().getAvailable()));
         XComponent dataRow = importedActivityVersionsMap.get(actLoc);
         OpGanttValidator.addResource(dataRow, OpActivityVersionDataSetFactory.createResourceChoice(assV, pCal));
      }
      logger.debug("TIMING: retrieveActivityVersionDataSet #04: "
            + (System.currentTimeMillis() - now));
      // set also the visual resources (uses the value of the dataset as a value
      // holder)
      Boolean showHours = (Boolean) dataSet.getValue();
      if (showHours == null) {
         showHours = Boolean.valueOf(OpSettingsService.getService().getStringValue(broker,
               OpSettings.SHOW_RESOURCES_IN_HOURS));
      }
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent activityRow = (XComponent) dataSet.getChild(i);

         Map<String, OpProjectCalendar> resCals = new HashMap<String, OpProjectCalendar>();
         Iterator rit = OpGanttValidator.getResources(activityRow).iterator();
         while (rit.hasNext()) {
            String resId = XValidator.choiceID((String) rit.next());
            resCals.put(resId, OpProjectCalendarFactory.getInstance().getCalendar(session, broker, resId, planVersion.locator()));
         }
         OpGanttValidator.updateResourceVisualization(activityRow, showHours
               .booleanValue(), resourceAvailability, pCal,
               resCals);
      }

      logger.debug("TIMING: retrieveActivityVersionDataSet #05: "
            + (System.currentTimeMillis() - now));
      // Dependencies: Fill predecessor and successor columns
      Iterator dependencies = planVersion.getDependencyVersions().iterator();
      OpDependencyVersion dependency = null;
      XComponent predecessorDataRow = null;
      XComponent successorDataRow = null;
      while (dependencies.hasNext()) {
         dependency = (OpDependencyVersion) dependencies.next();
         predecessorDataRow = (XComponent) dataSet.getChild(dependency.getPredecessorVersion().getSequence());
         successorDataRow = (XComponent) dataSet.getChild(dependency
               .getSuccessorVersion().getSequence());
         OpGanttValidator.addPredecessor(successorDataRow, predecessorDataRow
               .getIndex(), dependency.getDependencyType(), dependency.getAttribute(OpDependency.DEPENDENCY_CRITICAL));
         OpGanttValidator.addSuccessor(predecessorDataRow, successorDataRow
               .getIndex(), dependency.getDependencyType(), dependency.getAttribute(OpDependency.DEPENDENCY_CRITICAL));
      }

      logger.debug("TIMING: retrieveActivityVersionDataSet #06: "
            + (System.currentTimeMillis() - now));
      // WorkPhases: Fill work phase starts, finishes and base effort columns
      Iterator<OpWorkPeriodVersion> workPeriodsIt = planVersion.getWorkPeriodVersions().iterator();
      OpActivityDataSetFactory.addWorkPhasesForActivities(dataRowMap, workPeriodsIt);

      // Note: Comments are only available for activities (not activity
      // versions)
      logger.debug("TIMING: retrieveActivityVersionDataSet #07: "
            + (System.currentTimeMillis() - now));
      
   }

   public static String createResourceChoice(OpAssignmentIfc assignment, OpProjectCalendar pCal) {
      OpActivityIfc act = assignment.getActivity();

      double baseEffort = assignment.getBaseEffort();
      double duration = act.getDuration();
      String resourceName = assignment.getResource().getName();
      String resLoc = assignment.getResource().locator();

      return buildAssignmentChoice(pCal, baseEffort, duration, resourceName,
            resLoc, assignment.getAssigned());
   }

   private static String buildAssignmentChoice(OpProjectCalendar pCal,
         double baseEffort, double duration, String resourceName, String resLoc, double assigned) {
      double durationDays = OpGanttValidator.getDurationDays(duration, pCal);
      assigned = OpGanttValidator.greaterThanZeroWithTolerance(assigned, baseEffort) ? assigned : 100d;
      double assignedHours = durationDays > 0 ? (baseEffort / durationDays)
            : (pCal.getWorkHoursPerDay() * assigned / 100d);
      String assignmentString = resourceName + " " + String.valueOf(assignedHours) + "h";
      String resourceChoice = XValidator.choice(resLoc, assignmentString);
      return resourceChoice;
   }

   private static void retrieveAttachmentVersionsDataCell(Set attachments,
         XComponent dataRow) {
      // TODO: Bulk-fetch like other parts of the project plan
      List attachmentList = OpGanttValidator.getAttachments(dataRow);
      retrieveAttachmentVersions(attachments, attachmentList);
   }

   private static void retrieveAttachmentVersions(Set attachments,
         List attachmentList) {
      Iterator i = attachments.iterator();
      OpAttachmentVersion attachment = null;
      ArrayList attachmentElement = null;
      while (i.hasNext()) {
         attachment = (OpAttachmentVersion) i.next();
         attachmentElement = new ArrayList();
         if (attachment.getLinked()) {
            attachmentElement
                  .add(OpProjectConstants.LINKED_ATTACHMENT_DESCRIPTOR);
         } else {
            attachmentElement
                  .add(OpProjectConstants.DOCUMENT_ATTACHMENT_DESCRIPTOR);
         }
         attachmentElement.add(attachment.locator());
         attachmentElement.add(attachment.getName());
         attachmentElement.add(attachment.getLocation());
         if (!attachment.getLinked()) {
            if (attachment.getContent() == null) {
               logger.error("Found attachment:" + attachment.getName()
                     + " with no content");
               continue;
            }
            String contentId = OpLocator.locatorString(attachment.getContent());
            attachmentElement.add(contentId);
         } else {
            attachmentElement.add(OpProjectConstants.NO_CONTENT_ID);
         }
         attachmentList.add(attachmentElement);
      }
   }

   public static HashMap activityVersions(OpProjectPlanVersion planVersion) {
      HashMap activityVersions = new HashMap();
      // Check if this is a new project plan
      if (planVersion.getActivityVersions() != null) {
         Iterator i = planVersion.getActivityVersions().iterator();
         OpActivityVersion activityVersion = null;
         while (i.hasNext()) {
            activityVersion = (OpActivityVersion) i.next();
            activityVersions.put(new Long(activityVersion.getId()),
                  activityVersion);
         }
      }
      return activityVersions;
   }

   private static boolean mapActivityIDs(OpBroker broker, XComponent dataSet,
         OpProjectPlanVersion workingPlanVersion) {
      // Exchange all activity IDs contained in data-row values with their
      // respective working activity version IDs
      long now = System.currentTimeMillis();
      HashMap activityIdMap = new HashMap();
      OpQuery query = broker
            .newQuery("select activityVersion.Activity.id, activityVersion.id from OpActivityVersion as activityVersion where activityVersion.PlanVersion.id = ?");
      query.setLong(0, workingPlanVersion.getId());
      Iterator result = broker.iterate(query);
      Object[] record = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         logger.debug("MAPPING AIDS: " + record[0] + " -> " + record[1]);
         activityIdMap.put(record[0], record[1]);
      }
      logger.debug("TIMING: mapActivityIDs #00: "
            + (System.currentTimeMillis() - now));

      XComponent dataRow = null;
      Long activityVersionId = null;
      int mappedActivityIds = 0;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (dataRow.getStringValue() != null) {
            activityVersionId = (Long) activityIdMap.get(new Long(OpLocator
                  .parseLocator(dataRow.getStringValue()).getID()));
            if (activityVersionId != null) {
               dataRow.setStringValue(OpLocator.locatorString(
                     OpActivityVersion.ACTIVITY_VERSION, activityVersionId
                           .longValue()));
               mappedActivityIds++;
            }
         }
      }
      logger.debug("TIMING: mapActivityIDs #01: "
            + (System.currentTimeMillis() - now));

      return mappedActivityIds > 0;

   }

   public synchronized void storeActivityVersionDataSet(
         OpProjectSession session, OpBroker broker, XComponent dataSet,
         OpProjectPlanVersion planVersion, HashMap resources,
         OpProjectPlanVersion sourcePlanVersion, boolean independentCopy) {

      long now = System.currentTimeMillis();
      // setup resource and projectcalendar:
      // setup Maps for source locators to source activities and locators to
      // activities:
      Map<String, OpActivityVersion> sourceActivityVersionsMap = createActivityVersionMapForPlanVersion(session, broker, sourcePlanVersion);
      Map<String, OpActivityVersion> activityVersionsMap = createActivityVersionMapForPlanVersion(session, broker, planVersion);
      
      // Map<String, OpActivityVersion> sourceActivityVersionsMap = createActivityVersionMapForPlanVersion(sourcePlanVersion);
      // Map<String, OpActivityVersion> activityVersionsMap = createActivityVersionMapForPlanVersion(planVersion);
      
      OpProjectCalendar pCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, planVersion);

      logger.debug("TIMING: storeActivityVersionDataSet #00: "
            + (System.currentTimeMillis() - now));

      Date planStart = planVersion.getStart();
      Date planFinish = planVersion.getFinish();

      boolean progressTracked = planVersion.getProjectPlan()
      .getProgressTracked();

      long[] tds = {0, 0, 0, 0, 0, 0}; // 5 for now
      long[] iutds = {0,0,0,0,0,0,0,0,0,0,0,0}; // 11 more ;-)
      
      logger.debug("TIMING: storeActivityVersionDataSet #01: "
            + (System.currentTimeMillis() - now));
      // Phase 1: Iterate data-rows and store activity versions
      Stack<OpActivityVersion> superActivityStack = new Stack<OpActivityVersion>();
      ArrayList<OpActivityVersion> activityVersionList = new ArrayList<OpActivityVersion>();
      int i = 0;
      for (i = 0; i < dataSet.getChildCount(); i++) {
         long nowD = System.currentTimeMillis();
         
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         int correctedOutlineLevel = maintainActivityStack((byte) dataRow.getOutlineLevel(), superActivityStack, pCal,
               progressTracked);
         
         if (correctedOutlineLevel != dataRow.getOutlineLevel()) {
            logger.warn("Corrected outlinelevel for " + dataRow.getIndex() + "-" + OpGanttValidator.getName(dataRow) + " from " + dataRow.getOutlineLevel() + " to " +  correctedOutlineLevel);
         }
         tds[0] += System.currentTimeMillis() - nowD;
         // find activity in map for working version:
         OpActivityVersion activityVersion = null;
         OpActivityVersion srcActivityVersion = null;
         if (dataRow.getStringValue() != null) {
            activityVersion = activityVersionsMap.remove(dataRow
                  .getStringValue());
            srcActivityVersion = sourceActivityVersionsMap.remove(dataRow
                  .getStringValue());
         }

         tds[1] += System.currentTimeMillis() - nowD;
         
         OpActivityVersion sav = !superActivityStack.isEmpty() ? superActivityStack.peek() : null;
         // create or alter the Activity:
         
         activityVersion = insertOrUpdateActivityVersion(session, broker, planVersion,
               dataRow, activityVersion, sav,
               srcActivityVersion, pCal, progressTracked, independentCopy, iutds);

         tds[2] += System.currentTimeMillis() - nowD;
         linkToParent(activityVersion, sav);

         tds[3] += System.currentTimeMillis() - nowD;
         // independent copies are not linked to any activity objects...
         if (!independentCopy && srcActivityVersion != null
               && srcActivityVersion.getActivity() != null) {
            srcActivityVersion.getActivity()
                  .addActivityVersion(activityVersion);
         }
         tds[4] += System.currentTimeMillis() - nowD;

         // Set locator string value for newly created activity version data
         // rows (always update because it might have been an activity
         // before...)
         dataRow.setStringValue(activityVersion.locator());

         // Check project plan start and finish dates
         if (activityVersion.getType() == OpActivity.STANDARD
               || activityVersion.getType() == OpActivity.SCHEDULED_TASK) {
            if (activityVersion.getStart().before(planStart)) {
               planStart = activityVersion.getStart();
            }
            if (activityVersion.getFinish().after(planFinish)) {
               planFinish = activityVersion.getFinish();
            }
         }

         tds[5] += System.currentTimeMillis() - nowD;
         // Activity version list can be used to efficiently look-up activities
         // by data-row index
         activityVersionList.add(activityVersion);
         superActivityStack.push(activityVersion);
      }
      // cleanup stack:
      maintainActivityStack((byte) -1, superActivityStack, pCal, progressTracked);
      
      logTimingDetails(logger, "TIMING: storeActivityVersionDataSet #02a", tds);
      logTimingDetails(logger, "TIMING: storeActivityVersionDataSet #02b", iutds);

      logger.debug("TIMING: storeActivityVersionDataSet #03: "
            + (System.currentTimeMillis() - now));
      if (planVersion.getWorkPeriodVersions() != null) {
         updateOrDeleteWorkPeriodVersions(broker, dataSet, planVersion
               .getWorkPeriodVersions().iterator());
      }
      for (i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         OpActivityVersion activityVersion = (OpActivityVersion) activityVersionList
               .get(i);
         // work periods are
         if (OpGanttValidator.getType(dataRow) == OpGanttValidator.STANDARD
               || OpGanttValidator.getType(dataRow) == OpGanttValidator.TASK) {
            insertActivityWorkPeriodVersions(broker, planVersion, dataRow,
                  activityVersion);
         }
      }

      logger.debug("TIMING: storeActivityVersionDataSet #04: "
            + (System.currentTimeMillis() - now));
      List reusableAttachmentVersions = null;
      if (getAttachmentVersionsFromPlanVersion(planVersion).hasNext()) {
         reusableAttachmentVersions = updateOrDeleteAttachmentVersions(broker,
               dataSet, getAttachmentVersionsFromPlanVersion(planVersion));
      }
      logger.debug("TIMING: storeActivityVersionDataSet #05: "
            + (System.currentTimeMillis() - now));
      List reusableDependencyVersions = null;
      if (planVersion.getDependencyVersions() != null) {
         reusableDependencyVersions = updateOrDeleteDependencyVersions(broker,
               dataSet, planVersion.getDependencyVersions().iterator());
      }

      logger.debug("TIMING: storeActivityVersionDataSet #06: "
            + (System.currentTimeMillis() - now));
      if (planVersion.getAssignmentVersions() != null) {
         updateOrDeleteAssignmentVersions(session, broker, dataSet, planVersion, progressTracked);
      }
      long[] tdx = {0,0,0,0,0};
      for (i = 0; i < dataSet.getChildCount(); i++) {
         long nowD = System.currentTimeMillis();
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         OpActivityVersion activityVersion = (OpActivityVersion) activityVersionList
               .get(i);
         if (OpGanttValidator.getType(dataRow) == OpGanttValidator.STANDARD
               || OpGanttValidator.getType(dataRow) == OpGanttValidator.TASK
               || OpGanttValidator.getType(dataRow) == OpGanttValidator.MILESTONE) {
            insertActivityAssignmentVersions(session, broker, planVersion, dataRow,
                  activityVersion, resources, progressTracked);
         }
         tdx[0] += System.currentTimeMillis() - nowD;
         insertActivityAttachmentVersions(broker, planVersion, dataRow,
               activityVersion, reusableAttachmentVersions);
         tdx[1] += System.currentTimeMillis() - nowD;
         insertActivityDependencyVersions(broker, planVersion, dataSet,
               dataRow, activityVersion, activityVersionList,
               reusableDependencyVersions);
         tdx[2] += System.currentTimeMillis() - nowD;

      }
      logTimingDetails(logger, "TIMING: storeActivityVersionDataSet #06a", tdx);
      logger.debug("TIMING: storeActivityVersionDataSet #07: "
            + (System.currentTimeMillis() - now));
      // Phase 4: Delete unused related objects
      // TOOD: Attention with attachments -- we have to decrement ref-count?
      if (reusableAttachmentVersions != null) {
         for (i = 0; i < reusableAttachmentVersions.size(); i++) {
            broker
                  .deleteObject((OpAttachmentVersion) reusableAttachmentVersions
                        .get(i));
         }
      }
      if (reusableDependencyVersions != null) {
         for (i = 0; i < reusableDependencyVersions.size(); i++) {
            broker
                  .deleteObject((OpDependencyVersion) reusableDependencyVersions
                        .get(i));
         }
      }

      // Phase 5: Delete unused activity versions
      Iterator<OpActivityVersion> unusedActivityVersions = activityVersionsMap
            .values().iterator();
      while (unusedActivityVersions.hasNext()) {
         broker.deleteObject(unusedActivityVersions.next());
      }

      logger.debug("TIMING: storeActivityVersionDataSet #08: "
            + (System.currentTimeMillis() - now));
      // Phase n+1: update workbreaks...
      OpActivityDataSetFactory
            .updateWorkBreaks(broker, dataSet, planVersion != null
                  && planVersion.getWorkBreaks() != null ? planVersion
                  .getWorkBreaks().iterator() : null);

      // Finally, update project plan version start and finish fields
      planVersion.setStart(planStart);
      planVersion.setFinish(planFinish);
      broker.updateObject(planVersion);
      logger.debug("TIMING: storeActivityVersionDataSet #09: "
            + (System.currentTimeMillis() - now));
   }

   public static void logTimingDetails(XLog localLogger, String msg, long[] tds) {
      if (localLogger.isLoggable(XLog.DEBUG)) {
         StringBuffer tdString = new StringBuffer();
         for (int i = 0; i < tds.length; i++) {
            tdString.append(" ");
            tdString.append(i);
            tdString.append(":");
            tdString.append(tds[i]);
         }
         localLogger.debug(msg + ": " + tdString);
      }
   }

   private int maintainActivityStack(byte outlineLevel,
         Stack<OpActivityVersion> superActivityStack, OpProjectCalendar pCal,
         boolean progressTracked) {
      if (superActivityStack.isEmpty()) {
         return 0;
      }
      OpActivityVersion parentAct = superActivityStack.peek();
      int maxOutlineLevel = parentAct.hasSubActivities() ? parentAct.getOutlineLevel() + 1
            : parentAct.getOutlineLevel();
      int correctedOutlineLevel = outlineLevel <= maxOutlineLevel ? outlineLevel : maxOutlineLevel;
      while (!superActivityStack.isEmpty() && correctedOutlineLevel <= superActivityStack.peek().getOutlineLevel()) {
         OpActivityVersion sav = superActivityStack.pop();
         if (!superActivityStack.isEmpty()) {
            updateParentAggregatedValues(sav, superActivityStack.peek(), pCal, progressTracked);
         }
      }
      return correctedOutlineLevel;
   }

   private Map<String, OpActivityVersion> createActivityVersionMapForPlanVersion(
         OpProjectPlanVersion sourcePlanVersion) {
      Map<String, OpActivityVersion> sourceActivityVersionsMap = new HashMap<String, OpActivityVersion>();
      if (sourcePlanVersion != null
            && sourcePlanVersion.getActivityVersions() != null) {
         for (OpActivityVersion sa : sourcePlanVersion.getActivityVersions()) {
            sourceActivityVersionsMap.put(sa.locator(), sa);
         }
      }
      return sourceActivityVersionsMap;
   }

   private Map<String, OpActivityVersion> createActivityVersionMapForPlanVersion(OpProjectSession session, OpBroker broker,
         OpProjectPlanVersion sourcePlanVersion) {
      Map<String, OpActivityVersion> sourceActivityVersionsMap = new HashMap<String, OpActivityVersion>();
      if(sourcePlanVersion == null) {
         return sourceActivityVersionsMap;
      }
      OpQuery q = broker.newQuery("select act.id from OpActivityVersion as act where act.PlanVersion.id = :planVersionId");
      q.setLong("planVersionId", sourcePlanVersion.getId());
      Set<Long> allIds = new HashSet<Long>();
      Iterator<Long> qit = broker.iterate(q);
      while (qit.hasNext()) {
         allIds.add(qit.next());
      }
      Iterator<Long> memIt = allIds.iterator();
      OpBulkFetchIterator<OpActivityVersion, Long> ait = new OpBulkFetchIterator<OpActivityVersion, Long>(
            broker, memIt, broker.newQuery("select actV from "
                  + "OpActivityVersion as actV "
                  + "left join fetch actV.Activity as act "
                  + "left join fetch actV.Actions as act "
                  + "left join fetch actV.AssignmentVersions as assV "
                  + "left join fetch actV.PredecessorVersions as predVs "
                  + "left join fetch actV.SuccessorVersions as succVs "
                  + "where actV.id in (:ids)"),
            new LongIdConverter(), "ids");

      while(ait.hasNext()) {
         OpActivityVersion sa = ait.next();
         sourceActivityVersionsMap.put(sa.locator(), sa);
      }
      return sourceActivityVersionsMap;
   }

   private OpActivityVersion insertOrUpdateActivityVersion(
         OpProjectSession session, OpBroker broker,
         OpProjectPlanVersion planVersion, XComponent dataRow,
         OpActivityVersion activityVersion, OpActivityVersion superActivity,
         OpActivityVersion srcActivityVersion, OpProjectCalendar pCal,
         boolean progressTracked, boolean independentCopy, long td[]) {

      long nowD = System.currentTimeMillis();
      OpActivityCategory category = null;
      OpProjectNode projectNode = planVersion.getProjectPlan().getProjectNode();
      String categoryChoice = OpGanttValidator.getCategory(dataRow);
      if (activityVersion == null) {
         // Insert a new activity
         activityVersion = new OpActivityVersion();

         planVersion.addActivityVersion(activityVersion);
         activityVersion.setTemplate(planVersion.getTemplate());
         broker.makePersistent(activityVersion);
      }
      td[0] += System.currentTimeMillis() - nowD;
      activityVersion.setType((OpGanttValidator.getType(dataRow)));

      activityVersion.setName(OpGanttValidator.getName(dataRow));
      activityVersion.setDescription(OpGanttValidator.getDescription(dataRow));
      activityVersion.setSequence(dataRow.getIndex());
      activityVersion.setOutlineLevel((byte) (dataRow.getOutlineLevel()));
      activityVersion.setExpanded(dataRow.getExpanded());
      activityVersion.setDuration(OpGanttValidator.getDuration(dataRow));
      activityVersion.setAttributes(OpGanttValidator.getAttributes(dataRow));

      td[1] += System.currentTimeMillis() - nowD;
      // Pragram Management:
      String subProjectLocator = OpGanttValidator.getSubProject(dataRow);
      updateSubProjectLink(broker, activityVersion, subProjectLocator);
      td[2] += System.currentTimeMillis() - nowD;

      // update the master activity
      String masterActivityLocator = OpGanttValidator
            .getMasterActivity(dataRow);
      updateMasterActivityLink(broker, activityVersion, masterActivityLocator);
      td[3] += System.currentTimeMillis() - nowD;

      OpActivityDataSetFactory.updateStartFinish(activityVersion,
            OpGanttValidator.getStart(dataRow), OpGanttValidator
                  .getEnd(dataRow), OpGanttValidator.getLeadTime(dataRow),
            OpGanttValidator.getFollowUpTime(dataRow), activityVersion.isImported());
      td[4] += System.currentTimeMillis() - nowD;
      // update the category
      String categoryLocator = null;
      if (activityVersion.getCategory() != null) {
         categoryLocator = activityVersion.getCategory().locator();
      }

      if (categoryChoice != null) {
         String newCategory = XValidator.choiceID(categoryChoice);
         category = (OpActivityCategory) broker.getObject(newCategory);
         activityVersion.setCategory(category);
      } else {
         activityVersion.setCategory(null);
      }
      td[5] += System.currentTimeMillis() - nowD;


      byte validatorValue = (OpGanttValidator.getPriority(dataRow) == null) ? 0
            : OpGanttValidator.getPriority(dataRow).byteValue();
      if ((activityVersion.getPriority() == 0 && validatorValue != 0)
            || (activityVersion.getPriority() != 0 && (activityVersion
                  .getPriority() != validatorValue))) {
         activityVersion.setPriority(validatorValue);
      }

      activityVersion.setResponsibleResource(null);

      if (activityVersion.effortCalculatedFromChildren() && !activityVersion.isImported()) {
         activityVersion.resetAggregatedValuesForCollection();
      } else {
         String responsibleResourceLocator = OpGanttValidator
               .getResponsibleResource(dataRow) != null ? XValidator
               .choiceID(OpGanttValidator.getResponsibleResource(dataRow))
               : null;

         updateResponsibleResourceLink(broker, activityVersion,
               responsibleResourceLocator);

         double payment = 0d;
         if (activityVersion.isMilestone()) {
            payment = OpGanttValidator.getPayment(dataRow);
         } else {
            payment = 0d;
         }
         activityVersion.setPayment(OpGanttValidator.getPayment(dataRow));

         double oldBaseEffort = activityVersion.getBaseEffort();
         activityVersion.setBaseEffort(OpGanttValidator.getBaseEffort(dataRow));
         activityVersion.addUnassignedEffort(activityVersion.getBaseEffort()
               - oldBaseEffort);

         // activityVersion.setBasePersonnelCosts(OpGanttValidator
         // .getBasePersonnelCosts(dataRow));
         // activityVersion.setBaseProceeds(OpGanttValidator
         // .getBaseProceeds(dataRow));
         activityVersion.setBaseTravelCosts(OpGanttValidator
               .getBaseTravelCosts(dataRow));
         activityVersion.setBaseMaterialCosts(OpGanttValidator
               .getBaseMaterialCosts(dataRow));
         activityVersion.setBaseExternalCosts(OpGanttValidator
               .getBaseExternalCosts(dataRow));
         activityVersion.setBaseMiscellaneousCosts(OpGanttValidator
               .getBaseMiscellaneousCosts(dataRow));

         activityVersion.setEffortBillable(new Double(OpGanttValidator
               .getEffortBillable(dataRow)));

         td[6] += System.currentTimeMillis() - nowD;
         // update hierarchical stuff:
         double complete = OpGanttValidator.getComplete(dataRow);
         if (!activityVersion.isImported()) {
            if (progressTracked) {
               complete = activityVersion.getCompleteFromTracking(progressTracked);
            }
            activityVersion.setComplete(complete);
         } else {
            activityVersion.setComplete(complete);
         }
         td[7] += System.currentTimeMillis() - nowD;
      }

      
      addAdditionalAttributes(session, broker, activityVersion, dataRow,
            srcActivityVersion, independentCopy, td);
      td[11] += System.currentTimeMillis() - nowD;
      return activityVersion;
   }

   private void updateResponsibleResourceLink(OpBroker broker,
         OpActivityVersion activityVersion, String newLinkLocator) {
      // update the responsible resource
      long oldResourceId = 0;
      if (activityVersion.getResponsibleResource() != null) {
         oldResourceId = activityVersion.getResponsibleResource().getId();
      }
      long newResourceId = 0;
      if (newLinkLocator != null) {
         OpLocator l = OpLocator.parseLocator(newLinkLocator);
         newResourceId = l != null ? l.getID() : 0;
      }
      if (newResourceId != oldResourceId) {
         if (activityVersion.getResponsibleResource() != null) {
            activityVersion.getResponsibleResource().removeActivityVersion(activityVersion);
         }
         if (newResourceId != 0) {
            OpResource newResource = (OpResource) broker
                  .getObject(OpResource.class, newResourceId);
            if (newResource != null) {
               newResource.addActivityVersion(activityVersion);
            }
         }
      }
   }

   private void updateMasterActivityLink(OpBroker broker,
         OpActivityVersion activityVersion, String newLinkLocator) {
      long oldMasterId = 0;
      if (activityVersion.getMasterActivityVersion() != null) {
         oldMasterId = activityVersion.getMasterActivityVersion().getId();
      }
      long newMasterId = 0;
      if (newLinkLocator != null) {
         OpLocator l = OpLocator.parseLocator(newLinkLocator);
         newMasterId = l != null ? l.getID() : 0;
      }

      if (newMasterId != oldMasterId) {
         if (activityVersion.getMasterActivityVersion() != null) {
            activityVersion.getMasterActivityVersion().removeShallowCopy(activityVersion);
         }
         if (newMasterId != 0) {
            OpActivityVersion masterActivityVersion = (OpActivityVersion) broker
                  .getObject(newLinkLocator);
            if (masterActivityVersion != null) {
               masterActivityVersion.addShallowCopy(activityVersion);
            }
         }
      }
   }

   private void updateSubProjectLink(OpBroker broker,
         OpActivityVersion activityVersion, String newLinkLocator) {
      long oldSubProjectId = 0;
      if (activityVersion.getSubProject() != null) {
         oldSubProjectId = activityVersion.getSubProject().getId();
      }
      long newSubProjectId = 0;
      if (newLinkLocator != null) {
         OpLocator l = OpLocator.parseLocator(newLinkLocator);
         newSubProjectId = l != null ? l.getID() : 0;
      }

      if (newSubProjectId != oldSubProjectId) {
         if (activityVersion.getSubProject() != null) {
            activityVersion.getSubProject().removeProgramActivityVersion(activityVersion);
         }
         if (newSubProjectId != 0) {
            OpProjectNode newSubProject = (OpProjectNode) broker
                  .getObject(newLinkLocator);
            if (newSubProject != null) {
               newSubProject.addProgramActivityVersion(activityVersion);
            }
         }
      }
   }

   /**
    * @param session
    * @param broker
    * @param activityVersion
    * @param dataRow
    * @param srcActivityVersion
    * @param independentCopy 
    * @pre
    * @post
    */
   protected void addAdditionalAttributes(OpProjectSession session,
         OpBroker broker, OpActivityVersion activityVersion,
         XComponent dataRow, OpActivityVersion srcActivityVersion, boolean independentCopy, long[] td) {
   }

   private static void linkToParent(OpActivityVersion activity,
         OpActivityVersion newParent) {

      if (activity.getSuperActivityVersion() != null) {
         if (newParent.getId() == activity.getSuperActivityVersion().getId()) {
            return;
         }
         activity.getSuperActivityVersion().removeSubActivityVersion(activity);
      }
      if (newParent != null) {
         newParent.addSubActivityVersion(activity);
         activity.setOutlineLevel((byte)(newParent.getOutlineLevel() + 1));
      }
      else {
         activity.setOutlineLevel((byte) 0);
      }
   }
   

   public static void updateParentAggregatedValues(OpActivityVersion activity,
         OpActivityVersion parent, OpProjectCalendar calendar, boolean progressTracked) {
      
      if (parent.isImported()) {
         return;
      }

      parent.setBaseEffort(parent.getBaseEffort()
            + activity.getBaseEffort());
      parent.addUnassignedEffort(activity.getUnassignedEffort());

      parent.setBaseExternalCosts(parent
            .getBaseExternalCosts()
            + activity.getBaseExternalCosts());
      parent.setBaseMaterialCosts(parent
            .getBaseMaterialCosts()
            + activity.getBaseMaterialCosts());
      parent.setBaseMiscellaneousCosts(parent
            .getBaseMiscellaneousCosts()
            + activity.getBaseMiscellaneousCosts());

      parent.setBasePersonnelCosts(parent
            .getBasePersonnelCosts()
            + activity.getBasePersonnelCosts());
      parent.setBaseProceeds(parent.getBaseProceeds()
            + activity.getBaseProceeds());

      parent.setBaseTravelCosts(parent.getBaseTravelCosts()
            + activity.getBaseTravelCosts());

      parent.addActualEffort(activity.getActualEffort());
      parent.addRemainingEffort(activity.getRemainingEffort());

      parent.setActualExternalCosts(parent.getActualExternalCosts() + activity.getActualExternalCosts());
      parent.setActualMaterialCosts(parent.getActualMaterialCosts() + activity.getActualMaterialCosts());
      parent.setActualMiscellaneousCosts(parent.getActualMiscellaneousCosts() + activity.getActualMiscellaneousCosts());
      parent.setActualPersonnelCosts(parent.getActualPersonnelCosts() + activity.getActualPersonnelCosts());
      parent.setActualProceeds(parent.getActualProceeds() + activity.getActualProceeds());
      parent.setActualTravelCosts(parent.getActualTravelCosts() + activity.getActualTravelCosts());
      
      parent.setRemainingExternalCosts(parent.getRemainingExternalCosts() + activity.getRemainingExternalCosts());
      parent.setRemainingMaterialCosts(parent.getRemainingMaterialCosts() + activity.getRemainingMaterialCosts());
      parent.setRemainingMiscellaneousCosts(parent.getRemainingMiscellaneousCosts() + activity.getRemainingMiscellaneousCosts());
      parent.setRemainingPersonnelCosts(parent.getRemainingPersonnelCosts() + activity.getRemainingPersonnelCosts());
      parent.setRemainingProceeds(parent.getRemainingProceeds() + activity.getRemainingProceeds());
      parent.setRemainingTravelCosts(parent.getRemainingTravelCosts() + activity.getRemainingTravelCosts());
      
      parent.setComplete(parent.getCompleteFromTracking(progressTracked));

      if (parent.getType() == OpGanttValidator.SCHEDULED_TASK) {
         activity.setStart(parent.getStart());
         activity.setFinish(parent.getFinish());
      } else {
         // other direction:
         if (activity.getStart() != null) {
            int startDelta = 0;
            if (parent.getStart() != null) {
               startDelta = calendar.countWorkDaysBetween(activity
                     .getStart(), parent.getStart());
            }
            double leadDelta = activity.getLeadTime()
                  - parent.getLeadTime();
            double leadLowerLimit = parent.getLeadTime()
                  - (startDelta > 0 ? startDelta : 0);
            leadLowerLimit = leadLowerLimit > 0 ? leadLowerLimit : 0;

            if (startDelta >= 0 || parent.getStart() == null) {
               parent.setStart(activity.getStart());
            } else {
               leadDelta += startDelta;
            }
            if (parent.getLeadTime() + leadDelta > leadLowerLimit) {
               parent.setLeadTime(parent.getLeadTime()
                     + leadDelta);
            } else {
               parent.setLeadTime(leadLowerLimit);
            }
         }
         if (activity.getFinish() != null) {
            int finishDelta = 0;
            if (parent.getFinish() != null) {
               finishDelta = calendar.countWorkDaysBetween(new Date(parent.getFinish().getTime() + XCalendar.MILLIS_PER_DAY), new Date(activity
                     .getFinish().getTime() + XCalendar.MILLIS_PER_DAY));
            }
            double followUpDelta = activity.getFollowUpTime()
                  - parent.getFollowUpTime();
            double followUpLowerLimit = parent.getFollowUpTime()
                  - (finishDelta > 0 ? finishDelta : 0);
            followUpLowerLimit = followUpLowerLimit > 0 ? followUpLowerLimit
                  : 0;

            if (finishDelta >= 0 || parent.getFinish() == null) {
               parent.setFinish(activity.getFinish());
            } else {
               followUpDelta += finishDelta;
            }
            if (parent.getFollowUpTime() + followUpDelta > followUpLowerLimit) {
               parent.setFollowUpTime(parent.getFollowUpTime()
                     + followUpDelta);
            } else {
               parent.setFollowUpTime(followUpLowerLimit);
            }
         }
      }
   }

   private static void updateOrDeleteAssignmentVersions(OpProjectSession session, OpBroker broker,
         XComponent dataSet, OpProjectPlanVersion planVersion, boolean progressTracked) {
      List reusableAssignmentVersions = new ArrayList();
      OpAssignmentVersion assignment = null;
      XComponent dataRow = null;
      List resourceList = null;
      String resourceChoice = null;
      double baseEffort = 0.0d;
      double baseCosts = 0.0d;
      Iterator assignments = planVersion.getAssignmentVersions().iterator();
      
      while (assignments.hasNext()) {
         assignment = (OpAssignmentVersion) assignments.next();
         assignment.detachFromActivity(assignment.getActivityVersion());

         dataRow = getDataRowForActivity(assignment.getActivityVersion(),
               dataSet);
         boolean reusable = true;
         if (dataRow != null) {
            SortedMap<Date, OpWorkPeriodIfc> sortedWorkPeriods = OpActivityDataSetFactory
                  .getSortedWorkPeriodsForActivity(assignment.getActivity());

            resourceList = OpGanttValidator.getResources(dataRow);
            Map resourceBaseEffortMap = OpGanttValidator
                  .getResourceBaseEfforts(dataRow);
            // Check whether persistent assignment is present in resource list
            for (int i = resourceList.size() - 1; i >= 0; i--) {
               resourceChoice = (String) resourceList.get(i);
               // ignore invisible resources
               String resourceChoiceId = XValidator.choiceID(resourceChoice);
               if (resourceChoiceId.equals(OpGanttValidator.NO_RESOURCE_ID)) {
                  continue;
               }
               if (OpLocator.parseLocator(resourceChoiceId).getID() == assignment
                     .getResource().getId()) {
                  // Assignment is present: Remove from resource list and check
                  // whether update is required
                  resourceList.remove(i);
                  baseEffort = resourceBaseEffortMap
                        .containsKey(resourceChoiceId) ? ((Double) resourceBaseEffortMap
                        .remove(resourceChoiceId)).doubleValue()
                        : 0d;
                  // FIXME: This is by far the ugliest hack ever, but finding
                  // the root of the problem is delayed for now...
                  if (Double.isNaN(baseEffort))
                     baseEffort = 0;

                  OpProjectCalendar resCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, assignment.getResource(), planVersion);

                  double assignedHoursPerDay = OpGanttValidator.hoursAssigned(resourceChoice);
                  double assigned = assignedHoursPerDay / resCal.getWorkHoursPerDay() * 100;
                  if (assignedHoursPerDay == OpGanttValidator.INVALID_ASSIGNMENT) {
                     assigned = assignment.getResource().getAvailable();
                  }
                  assignment.setAssigned(assigned);
                  assignment.setBaseEffort(baseEffort);
                  // TODO: The original XActivitySetFactory might miss the
                  // updating of assignment-complete (not sure)?
                  // (Maybe reevaluate complete on basis of base-effort and
                  // remaining-effort; note: Not for *version*?)
                  
                  OpProjectNodeAssignment pna = planVersion.getProjectPlan()
                        .getProjectNode().getAssignmentForResource(
                              assignment.getResource());
                  updateAssignmentCosts(session, broker, assignment, pna, resCal.getWorkHoursPerDay(),
                        sortedWorkPeriods);
                  updateAssignmentWorkMonths(session, broker, assignment, pna, resCal.getWorkHoursPerDay(),
                        sortedWorkPeriods);
                  assignment.attachToActivity(assignment.getActivityVersion());
                  reusable = false;
                  break;
               }
            }
         }
         if (reusable) {
            // break links to activity
            assignment.getActivityVersion().removeAssignmentVersion(assignment);
            assignment.getResource()
                  .removeActivityVersionAssignment(assignment);
            assignments.remove();
            broker.deleteObject(assignment);
         }
      }
   }

   private static void updateAssignmentCosts(OpProjectSession session, OpBroker broker, OpAssignmentVersion assignment,
         OpProjectNodeAssignment pna, double workHoursPerDay,
         SortedMap<Date, OpWorkPeriodIfc> sortedWorkPeriods) {
      
      Date date = assignment.getActivityVersion().getStart();
      Date finish = OpActivityDataSetFactory.getFinishDateFromWorkPeriods(sortedWorkPeriods, date);
      finish = new Date(finish.getTime() + XCalendar.MILLIS_PER_DAY);
      
      double dailyEffort = workHoursPerDay * assignment.getAssigned() / 100d;
      double pCostsSum = 0d;
      double proceedsSum = 0d;
      
      while (!date.after(finish)) {
         if (OpActivityDataSetFactory.getWorkPeriodForWorkDay(date, sortedWorkPeriods) != null) {
            List<Double> rates = pna.getRatesForDay(date, true);
            double internalRate = rates.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
            double externalRate = rates.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
            pCostsSum += internalRate * dailyEffort;
            proceedsSum += externalRate * dailyEffort;
         }
         date = new Date(date.getTime() + XCalendar.MILLIS_PER_DAY);
      }
      assignment.setBaseCosts(pCostsSum);
      assignment.setBaseProceeds(proceedsSum);
   }

   private static void updateAssignmentWorkMonths(OpProjectSession session, OpBroker broker, OpAssignmentVersion assignment,
         OpProjectNodeAssignment pna, double workHoursPerDay,
         SortedMap<Date, OpWorkPeriodIfc> sortedWorkPeriods) {
      
      Collection<OpWorkMonthVersion> wmvs = new HashSet<OpWorkMonthVersion>();
      if (assignment.getActivityVersion().hasBaseEffort()) {
         
         Date date = assignment.getActivityVersion().getStart();
         Date assignmentFinish = OpActivityDataSetFactory.getFinishDateFromWorkPeriods(sortedWorkPeriods, date);
         Date finish = new Date(assignmentFinish.getTime() + XCalendar.MILLIS_PER_DAY);
         
         double dailyEffort = workHoursPerDay * assignment.getAssigned() / 100d;
   
         Calendar c = OpProjectCalendarFactory.getInstance().getDefaultCalendar(session).cloneCalendarInstance();
         c.setTime(date);
   
         double wmEffort = 0d;
         double wmCosts = 0d;
         double wmProceeds = 0d;
         int y = c.get(Calendar.YEAR);
         int m = c.get(Calendar.MONTH);
         
         while (!date.after(finish)) {
            c.setTime(date);
            if (c.get(Calendar.YEAR) != y || c.get(Calendar.MONTH) != m
                  || date.after(assignmentFinish)) {
               OpWorkMonthVersion wmv = new OpWorkMonthVersion();
               wmv.setYear(y);
               wmv.setMonth((byte) m);
               wmv.setBaseAssigned(assignment.getAssigned());
               wmv.setBaseEffort(wmEffort);
               wmv.setBasePersonnelCosts(wmCosts);
               wmv.setBaseProceeds(wmProceeds);
               wmvs.add(wmv);
               
               y = c.get(Calendar.YEAR);
               m = c.get(Calendar.MONTH);
               wmEffort = 0d;
               wmCosts = 0d;
               wmProceeds = 0d;
            }
            
            if (OpActivityDataSetFactory.getWorkPeriodForWorkDay(date, sortedWorkPeriods) != null) {
               List<Double> rates = pna.getRatesForDay(date, true);
               double internalRate = rates.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
               double externalRate = rates.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
               wmEffort += dailyEffort;
               wmCosts += internalRate * dailyEffort;
               wmProceeds += externalRate * dailyEffort;
            }
            date = new Date(date.getTime() + XCalendar.MILLIS_PER_DAY);
         }
      }
      WorkMonthCreationSyncHelper wmch = new WorkMonthCreationSyncHelper(assignment, broker);
      wmch.copy(assignment.getWorkMonthVersions(), wmvs);
   }

   /**
    * Retrieves in a safe way a data row from the given data set that is
    * associated with the given activity version
    * 
    * @param activityVersion
    *           actvitiy entitiy
    * @param dataSet
    *           data set to retrieve the data row from
    * @return data row for activity version
    */
   private static XComponent getDataRowForActivity(
         OpActivityVersion activityVersion, XComponent dataSet) {
      XComponent dataRow;
      int sequence = activityVersion.getSequence();
      long id = activityVersion.getId();
      dataRow = null;
      if (sequence < dataSet.getChildCount()) {
         dataRow = (XComponent) dataSet.getChild(sequence);
         long rowId = OpLocator.parseLocator(dataRow.getStringValue()).getID();
         if (rowId != id) {
            dataRow = null;
         }
      }
      return dataRow;
   }

   private static void insertActivityAssignmentVersions(OpProjectSession session, OpBroker broker,
         OpProjectPlanVersion planVersion, XComponent dataRow,
         OpActivityVersion activityVersion, HashMap resources, boolean progressTracked) {
      if (activityVersion.isImported()) {
         return;
      }
      SortedMap<Date, OpWorkPeriodIfc> sortedWorkPeriods = OpActivityDataSetFactory
            .getSortedWorkPeriodsForActivity(activityVersion);

      List resourceList = OpGanttValidator.getResources(dataRow);
      String resourceChoice = null;
      Map resourceBaseEffortMap = OpGanttValidator
            .getResourceBaseEfforts(dataRow);
      double baseEffort = 0.0d;
      OpResource resource = null;
      for (int i = 0; i < resourceList.size(); i++) {
         // Insert new assignment version
         resourceChoice = (String) resourceList.get(i);
         // ignore invisible resources
         String resourceChoiceId = XValidator.choiceID(resourceChoice);
         if (resourceChoiceId.equals(OpGanttValidator.NO_RESOURCE_ID)) {
            continue;
         }
         Double mapBE = ((Double) resourceBaseEffortMap.get(resourceChoiceId));
         baseEffort = mapBE != null ? mapBE.doubleValue() : 0d;

         OpAssignmentVersion assV = new OpAssignmentVersion();
         broker.makePersistent(assV);
         planVersion.addAssignmentVersion(assV);
         activityVersion.addAssignmentVersion(assV);
         resource = (OpResource) resources.get(new Long(OpLocator.parseLocator(
               resourceChoiceId).getID()));
         resource.addActivityVersionAssignment(assV);

         // TODO: The original XActivitySetFactory might miss the updating of
         // assignment-complete (not sure)?
         // (Maybe reevaluate complete on basis of base-effort and
         // remaining-effort; note: Not for *version*?)
         OpProjectCalendar resCal = OpProjectCalendarFactory.getInstance()
               .getCalendar(session, broker, assV.getResource(), planVersion);
   
         double assignedHoursPerDay = OpGanttValidator.hoursAssigned(resourceChoice);
         double assigned = assignedHoursPerDay / resCal.getWorkHoursPerDay() * 100;
         if (assignedHoursPerDay == OpGanttValidator.INVALID_ASSIGNMENT) {
            assigned = resource.getAvailable();
         }
         assV.setAssigned(assigned);
         assV.setBaseEffort(baseEffort);

         activityVersion.addAssignmentVersion(assV);

         OpProjectNodeAssignment pna = planVersion.getProjectPlan()
         .getProjectNode().getAssignmentForResource(
               assV.getResource());
         updateAssignmentCosts(session, broker, assV, pna, resCal.getWorkHoursPerDay(),
               sortedWorkPeriods);
         updateAssignmentWorkMonths(session, broker, assV, pna, resCal
               .getWorkHoursPerDay(), sortedWorkPeriods);

         assV.attachToActivity(activityVersion);

         if (activityVersion.getActivity() != null
               && activityVersion.getActivity().getAssignments() != null) {
            Iterator<OpAssignment> actAssIt = activityVersion.getActivity()
                  .getAssignments().iterator();
            while (actAssIt.hasNext() && assV.getAssignment() == null) {
               OpAssignment ass = actAssIt.next();
               if (ass.getResource().getId() == assV.getResource().getId()) {
                  ass.addAssignmentVersion(assV);
               }
            }
         }


      }
   }

   private static void updateOrDeleteWorkPeriodVersions(OpBroker broker,
         XComponent dataSet, Iterator workPeriodsIt) {
      OpWorkPeriodVersion workPeriod;
      XComponent dataRow;
      double baseEffort;
      boolean update;
      boolean reusable;
      while (workPeriodsIt.hasNext()) {
         reusable = false;
         update = false;
         workPeriod = (OpWorkPeriodVersion) workPeriodsIt.next();
         dataRow = getDataRowForActivity(workPeriod.getActivityVersion(),
               dataSet);
         if (dataRow == null) {
            reusable = true;
         } else {
            Date periodStart = workPeriod.getStart();
            Map workPeriods = OpActivityDataSetFactory.getWorkPeriods(dataRow);
            List activitiPeriodValues = (List) workPeriods.get(periodStart);
            if (activitiPeriodValues != null) {
               long workingDays = ((Long) activitiPeriodValues.get(0))
                     .longValue();
               baseEffort = ((Double) activitiPeriodValues.get(1))
                     .doubleValue();
               if (baseEffort != workPeriod.getBaseEffort()) {
                  workPeriod.setBaseEffort(baseEffort);
                  update = true;
               }
               if (workingDays != workPeriod.getWorkingDays()) {
                  workPeriod.setWorkingDays(workingDays);
                  update = true;
               }
               if (update) {
                  broker.updateObject(workPeriod);
               }
            } else {
               reusable = true;
            }
         }
         // Work phase does not exist anymore: Can be reused
         if (reusable) {
            // break activity-workperiod link
            OpActivityVersion activityVersion = workPeriod.getActivityVersion();
            activityVersion.getWorkPeriodVersions().remove(workPeriod);
            workPeriodsIt.remove();
            broker.deleteObject(workPeriod);
            broker.updateObject(activityVersion);
         }
      }
   }
   
//   private static class WorkPeriodsSynchHelper extends OpCollectionSynchronizationHelper<OpWorkPeriodVersion, Map.Entry<Date, List>> {
//
//      private OpBroker broker = null;
//      private OpActivityVersion actVersion = null;
//      private OpProjectPlanVersion planVersion = null;
//      
//      public WorkPeriodsSynchHelper(OpBroker broker, OpActivityVersion actVersion, OpProjectPlanVersion planVersion) {
//         this.broker = broker;
//         this.actVersion = actVersion;
//         this.planVersion = planVersion;
//      }
//      
//      @Override
//      protected int cloneInstance(OpWorkPeriodVersion tgt,
//            Map.Entry<Date, List> src) {
//         tgt.setStart(src.getStart());
//         tgt.setFinish(src.getFinish());
//         tgt.setBaseEffort(src.getBaseEffort());
//         tgt.setWorkingDays(src.getWorkingDays());
//         return OK;
//      }
//
//      @Override
//      protected int corresponds(OpWorkPeriodVersion tgt, Map.Entry<Date, List> src) {
//         return tgt.getStart().compareTo(src.getStart());
//      }
//
//      @Override
//      protected void deleteInstance(OpWorkPeriodVersion del) {
//         if (del.getActivityVersion() != null) {
//            del.getActivityVersion().removeWorkPeriodVersion(del);
//         }
//         if (del.getPlanVersion() != null) {
//            del.getPlanVersion().removeWorkPeriodVersion(del);
//         }
//         broker.deleteObject(del);
//      }
//
//      @Override
//      protected OpWorkPeriodVersion newInstance(Map.Entry<Date, List> src) {
//         OpWorkPeriodVersion wp = new OpWorkPeriodVersion();
//         planVersion.addWorkPeriodVersion(wp);
//         actVersion.addWorkPeriodVersion(wp);
//         return wp;
//      }
//
//      @Override
//      protected int sourceOrder(Map.Entry<Date, List> cm2a,
//            Map.Entry<Date, List> cm2b) {
//         return cm2a.getStart().compareTo(cm2b.getStart());
//      }
//
//      @Override
//      protected int targetOrder(OpWorkPeriodVersion cm1a,
//            OpWorkPeriodVersion cm1b) {
//         return cm1a.getStart().compareTo(cm1b.getStart());
//      }
//      
//      
//      private long getWorkingDays(Map.Entry<Date, List> )
//   }

   private static void insertActivityWorkPeriodVersions(OpBroker broker,
         OpProjectPlanVersion planVersion, XComponent dataRow,
         OpActivityVersion activityVersion) {

      Map workPeriods = OpActivityDataSetFactory.getWorkPeriods(dataRow);
      OpWorkPeriodVersion workPeriod = null;
      for (Iterator iterator = workPeriods.entrySet().iterator(); iterator
            .hasNext();) {
         Map.Entry workPeriodEntry = (Map.Entry) iterator.next();

         Date periodStart = (Date) workPeriodEntry.getKey();
         // check if activity does not already have it
         boolean periodSaved = false;
         if (activityVersion.getWorkPeriodVersions() != null) {
            for (Iterator iterator1 = activityVersion.getWorkPeriodVersions()
                  .iterator(); iterator1.hasNext();) {
               OpWorkPeriodVersion opWorkPeriod = (OpWorkPeriodVersion) iterator1
                     .next();
               if (opWorkPeriod.getStart().equals(periodStart)) {
                  periodSaved = true;
                  break;
               }
            }
         }
         if (periodSaved) {
            continue;
         }
         List workPeriodValues = (List) workPeriodEntry.getValue();
         long workingDays = ((Long) workPeriodValues.get(0)).longValue();
         double baseEffortPerDay = ((Double) workPeriodValues.get(1))
               .doubleValue();

         workPeriod = new OpWorkPeriodVersion();
         planVersion.addWorkPeriodVersion(workPeriod);
         activityVersion.addWorkPeriodVersion(workPeriod);
         workPeriod.setStart(periodStart);
         workPeriod.setWorkingDays(workingDays);
         workPeriod.setBaseEffort(baseEffortPerDay);

         broker.makePersistent(workPeriod);
      }
   }

   private static ArrayList updateOrDeleteAttachmentVersions(OpBroker broker,
         XComponent dataSet, Iterator attachments) {
      OpAttachmentVersion attachment = null;
      XComponent dataRow = null;
      int i = 0;
      List attachmentList = null;
      ArrayList attachmentElement = null;
      long attachmentId = 0;
      ArrayList reusableAttachmentVersions = new ArrayList();
      boolean reusable;
      while (attachments.hasNext()) {
         reusable = false;
         attachment = (OpAttachmentVersion) attachments.next();
         dataRow = getDataRowForActivity(attachment.getActivityVersion(),
               dataSet);
         if (dataRow == null) {
            reusable = true;
         } else {
            attachmentList = OpGanttValidator.getAttachments(dataRow);
            for (i = attachmentList.size() - 1; i >= 0; i--) {
               attachmentElement = (ArrayList) attachmentList.get(i);
               String choiceId = XValidator.choiceID((String) attachmentElement
                     .get(1));
               OpLocator attLocator = OpLocator.parseLocator(choiceId);
               if (attLocator == null) {
                  // newly added
                  continue;
               }
               attachmentId = attLocator.getID();
               if (attachment.getId() == attachmentId) {
                  // check if the attachment had it's name updated
                  String caption = (String) attachmentElement.get(2);
                  if (!attachment.getName().equals(caption)) {
                     attachment.setName(caption);
                     broker.updateObject(attachment);
                  }
                  attachmentList.remove(i);
                  break;
               }
            }
            if (i == -1) {
               reusable = true;
            }
         }
         // Attachment was deleted on client: Decrease ref-count of content
         // objects (and delete if it is null)
         if (reusable) {
            OpContent content = attachment.getContent();
            OpContentManager.updateContent(content, broker, false, attachment);
            reusableAttachmentVersions.add(attachment);
            // break link from activity to attachment
            OpActivityVersion activityVersion = attachment.getActivityVersion();
            activityVersion.getAttachmentVersions().remove(attachment);
         }
      }
      return reusableAttachmentVersions;
   }

   private static void insertActivityAttachmentVersions(OpBroker broker,
         OpProjectPlanVersion planVersion, XComponent dataRow,
         OpActivityVersion activityVersion, List reusableAttachmentVersions) {
      if (activityVersion.isImported()) {
         return;
      }
      List attachmentList = OpGanttValidator.getAttachments(dataRow);
      List attachmentElement;
      OpAttachmentVersion attachment;
      for (int i = 0; i < attachmentList.size(); i++) {
         // Insert new attachment version
         attachmentElement = (List) attachmentList.get(i);
         if ((reusableAttachmentVersions != null)
               && (reusableAttachmentVersions.size() > 0)) {
            attachment = (OpAttachmentVersion) reusableAttachmentVersions
                  .remove(reusableAttachmentVersions.size() - 1);
         } else {
            attachment = new OpAttachmentVersion();
         }
         activityVersion.addAttachmentVersion(attachment);
         attachment.setLinked(OpProjectConstants.LINKED_ATTACHMENT_DESCRIPTOR
               .equals(attachmentElement.get(0)));
         attachment.setName((String) attachmentElement.get(2));
         attachment.setLocation((String) attachmentElement.get(3));
         OpPermissionDataSetFactory.updatePermissions(broker, planVersion
               .getProjectPlan().getProjectNode(), attachment);

         if (!attachment.getLinked()) {
            String contentId = (String) attachmentElement.get(4);
            if (OpLocator.validate(contentId)) {
               OpContent content = (OpContent) broker.getObject(contentId);
               OpContentManager
                     .updateContent(content, broker, true, attachment);
               attachment.setContent(content);
            } else {
               logger.warn("The attachment " + attachment.getName()
                     + " was not persisted because the content was null");
               continue; // the content is not persisted due to some IO errors
            }
         }

         if (attachment.getId() == 0) {
            broker.makePersistent(attachment);
         } else {
            broker.updateObject(attachment);
         }
      }
   }

   private static List updateOrDeleteDependencyVersions(OpBroker broker,
         XComponent dataSet, Iterator dependencies) {
      OpDependencyVersion dependency = null;
      XComponent predecessorDataRow = null;
      XComponent successorDataRow = null;
      List reusableDependencyVersions = new ArrayList();
      while (dependencies.hasNext()) {
         boolean reusable = false;
         dependency = (OpDependencyVersion) dependencies.next();
         successorDataRow = getDataRowForActivity(dependency
               .getSuccessorVersion(), dataSet);
         if (successorDataRow == null) {
            reusable = true;
         } else {
            SortedMap predecessors = OpGanttValidator
                  .getPredecessors(successorDataRow);
            // update dependency type
            Map values = (Map) predecessors.get(new Integer(dependency
                  .getPredecessorVersion().getSequence()));
            if (values != null) {
               dependency.setDependencyType(((Integer) values
                     .get(OpGanttValidator.DEP_TYPE)).intValue());
               dependency.setAttribute(OpDependency.DEPENDENCY_CRITICAL,
                     values.get(OpGanttValidator.DEP_CRITICAL) != null
                           && ((Boolean) values.get(OpGanttValidator.DEP_CRITICAL))
                                 .booleanValue());
            }
            if (predecessors.remove(new Integer(dependency
                  .getPredecessorVersion().getSequence())) != null) {
               // Dependency still exists: Remove also other part of
               // bi-directional association
               predecessorDataRow = (XComponent) dataSet.getChild(dependency
                     .getPredecessorVersion().getSequence());
               SortedMap successors = OpGanttValidator
                     .getSuccessors(predecessorDataRow);
               successors.remove(new Integer(dependency.getSuccessorVersion()
                     .getSequence()));
            } else {
               reusable = true;
            }
         }
         if (reusable) {
            reusableDependencyVersions.add(dependency);
            // break link activiyt->dependency
            OpActivityVersion successorVersion = dependency
                  .getSuccessorVersion();
            successorVersion.getPredecessorVersions().remove(dependency);
            OpActivityVersion predecessorVersion = dependency
                  .getPredecessorVersion();
            predecessorVersion.getSuccessorVersions().remove(dependency);
            broker.updateObject(successorVersion);
            broker.updateObject(predecessorVersion);
         }
      }
      return reusableDependencyVersions;
   }

   private static void insertActivityDependencyVersions(OpBroker broker,
         OpProjectPlanVersion planVersion, XComponent dataSet,
         XComponent dataRow, OpActivityVersion successor,
         List activityVersionList, List reusableDependencyVersions) {
      // Note: We only check for new predecessor indexes
      // (Successors are just the other side of the bi-directional association)
      SortedMap predecessors = OpGanttValidator.getPredecessors(dataRow);
      OpDependencyVersion dependency = null;
      XComponent predecessorDataRow = null;
      OpActivityVersion predecessor = null;
      Iterator pit = predecessors.keySet().iterator();
      while (pit.hasNext()) {
         Integer key = (Integer) pit.next();
         Map values = (Map) predecessors.get(key);
         // Insert new dependency version
         if ((reusableDependencyVersions != null)
               && (reusableDependencyVersions.size() > 0)) {
            dependency = (OpDependencyVersion) reusableDependencyVersions
                  .remove(reusableDependencyVersions.size() - 1);
         } else {
            dependency = new OpDependencyVersion();
         }
         planVersion.addDependencyVersion(dependency);
         dependency.setDependencyType(((Integer) values.get(OpGanttValidator.DEP_TYPE))
               .intValue());
         dependency.setAttribute(OpDependency.DEPENDENCY_CRITICAL,
               values.get(OpGanttValidator.DEP_CRITICAL) != null
                     && ((Boolean) values.get(OpGanttValidator.DEP_CRITICAL))
                           .booleanValue());
         predecessorDataRow = (XComponent) dataSet.getChild(key.intValue());
         predecessor = (OpActivityVersion) activityVersionList
               .get(predecessorDataRow.getIndex());
         predecessor.addSuccessorDependency(dependency);
         successor.addPredecessorDependency(dependency);
         if (dependency.getId() == 0) {
            broker.makePersistent(dependency);
         } else {
            broker.updateObject(dependency);
         }
      }
   }

   @Deprecated
   // kept for upgrade only
   public OpProjectPlanVersion newProjectPlanVersion(OpProjectSession session,
         OpBroker broker, OpProjectPlan projectPlan, OpUser creator,
         int versionNumber, boolean copyActivities) {

      long now = System.currentTimeMillis();
      OpProjectPlanVersion planVersion = createProjectPlanVersionObject(
            session, broker, projectPlan, creator, versionNumber);

      boolean progressTracked = projectPlan.getProgressTracked();
      OpProjectCalendar pCal = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, planVersion);
      // Copy activities (check for null activities is necessary for newly
      // created project plans)
      if (copyActivities && (projectPlan.getActivities() != null)) {

         logger.debug("TIMING: newProjectPlanVersion #01: "
               + (System.currentTimeMillis() - now));

         HashMap<Long, Pair<OpActivity, OpActivityVersion>> activityVersionMap = new HashMap<Long, Pair<OpActivity, OpActivityVersion>>();
         Iterator<OpActivity> activities = projectPlan.getActivities().iterator();
         OpActivity activity = null;
         OpActivityVersion activityVersion = null;
         while (activities.hasNext()) {
            activity = activities.next();
            if (activity.getDeleted() || activity.getType() == OpActivity.ADHOC_TASK) {
//            if (activity.getType() == OpActivity.ADHOC_TASK) {
               continue;
            }
            activityVersion = new OpActivityVersion();
            planVersion.addActivityVersion(activityVersion);
            activityVersion.setActivity(activity);
            activityVersion.setName(activity.getName());
            activityVersion.setDescription(activity.getDescription());
            activityVersion.setType(activity.getType());
            activityVersion.setAttributes(activity.getAttributes());
            activityVersion.setSequence(activity.getSequence());
            activityVersion.setOutlineLevel(activity.getOutlineLevel());
            activityVersion.setStart(activity.getStart());
            activityVersion.setFinish(activity.getFinish());
            activityVersion.setDuration(activity.getDuration());
            activityVersion.setComplete(activity.getComplete());
            activityVersion.setBaseEffort(activity.getBaseEffort());
            activityVersion.setBaseTravelCosts(activity.getBaseTravelCosts());
            activityVersion.setBasePersonnelCosts(activity
                  .getBasePersonnelCosts());
            activityVersion.setBaseProceeds(activity.getBaseProceeds());
            activityVersion.setBaseMaterialCosts(activity
                  .getBaseMaterialCosts());
            activityVersion.setBaseExternalCosts(activity
                  .getBaseExternalCosts());
            activityVersion.setBaseMiscellaneousCosts(activity
                  .getBaseMiscellaneousCosts());

            activityVersion.setLeadTime(activity.getLeadTime());
            activityVersion.setFollowUpTime(activity.getFollowUpTime());
            OpActivity masterActivity = activity.getMasterActivity();
            if (masterActivity != null) {
               masterActivity.addShallowVersion(activityVersion);
            }
            OpProjectNode subProject = activity.getSubProject();
            if (subProject != null) {
               subProject.addProgramActivityVersion(activityVersion);
            }
            clone(session, broker, activityVersion, activity, progressTracked);
            broker.makePersistent(activityVersion);
            // Add new activity version to activity version map
            activityVersionMap.put(activity.getId(),
                  new Pair<OpActivity, OpActivityVersion>(activity,
                        activityVersion));
         }

         logger.debug("TIMING: newProjectPlanVersion #01a: "
               + (System.currentTimeMillis() - now));
         // setup superactivities:
         activities = projectPlan.getActivities().iterator();
         while (activities.hasNext()) {
            OpActivity act = activities.next();
            Pair<OpActivity, OpActivityVersion> pair = activityVersionMap.get(act.getId());
            if (pair != null) {
               OpActivityVersion v = pair.getSecond();
               if (act.getSuperActivity() != null) {
                  Pair<OpActivity, OpActivityVersion> superPair = activityVersionMap.get(
                        act.getSuperActivity().getId());
                  if (superPair != null) {
                     OpActivityVersion sv = superPair.getSecond();
                     sv.addSubActivityVersion(v);
                     linkToParent(v, sv);
                     updateParentAggregatedValues(v, sv, pCal, projectPlan.getProgressTracked());
                  }
               }

               if (act.getWorkBreaks() != null && !act.getWorkBreaks().isEmpty()) {
                  Iterator<OpWorkBreak> wbit = act.getWorkBreaks().iterator();
                  while (wbit.hasNext()) {
                     OpWorkBreak wb = wbit.next();
                     OpActivityVersionWorkBreak nwb = new OpActivityVersionWorkBreak(
                           wb.getStart(), wb.getDuration());
                     v.addWorkBreak(nwb);
                     broker.makePersistent(nwb);
                  }
               }
            }
         }

         logger.debug("TIMING: newProjectPlanVersion #02: "
               + (System.currentTimeMillis() - now));
         // Copy attachments and increment ref-count of reused content objects
         Iterator attachments = OpActivityDataSetFactory
               .getAttachmentsFromProjectPlan(projectPlan).iterator();
         OpAttachment attachment = null;
         OpAttachmentVersion attachmentVersion = null;
         OpContent content = null;
         while (attachments.hasNext()) {
            attachment = (OpAttachment) attachments.next();
            if (((OpActivityIfc) attachment.getObject()).getType() == OpActivity.ADHOC_TASK) {
               continue;
            }
            Long activityId = ((OpActivity) attachment.getObject()).getId();
            Pair<OpActivity, OpActivityVersion> pair = activityVersionMap
                  .get(activityId);
            if (pair != null) {
               attachmentVersion = new OpAttachmentVersion();
               attachmentVersion.setActivityVersion((OpActivityVersion) pair.getSecond());
               attachmentVersion.setName(attachment.getName());
               attachmentVersion.setLinked(attachment.getLinked());
               attachmentVersion.setLocation(attachment.getLocation());
               if (attachment.getContent() != null) {
               // Increase ref-count of reused content object
                  content = attachment.getContent();
                  OpContentManager.updateContent(content, broker, true,
                        attachmentVersion);
                  attachmentVersion.setContent(content);
               }
               broker.makePersistent(attachmentVersion);
            }
         }

         logger.debug("TIMING: newProjectPlanVersion #03: "
               + (System.currentTimeMillis() - now));
         // Copy assignments
         Iterator<OpAssignment> assignments = projectPlan
               .getActivityAssignments().iterator();
         OpAssignment assignment = null;
         OpAssignmentVersion assignmentVersion = null;
         assignments = projectPlan.getActivityAssignments().iterator();
         while (assignments.hasNext()) {
            assignment = (OpAssignment) assignments.next();
            if (assignment.getActivity().getType() == OpActivity.ADHOC_TASK) {
               continue;
            }
            Long activityId = assignment.getActivity().getId();
            Pair<OpActivity, OpActivityVersion> pair = activityVersionMap.get(activityId);
            if (pair != null) {
               assignmentVersion = new OpAssignmentVersion();
               assignment.addAssignmentVersion(assignmentVersion);
               ((OpActivityVersion) pair.getSecond()).addAssignmentVersion(assignmentVersion);
               assignmentVersion.setPlanVersion(planVersion);
               assignmentVersion.setResource(assignment.getResource());
               assignmentVersion.setAssigned(assignment.getAssigned());
               assignmentVersion.setComplete(assignment.getComplete());
               assignmentVersion.setBaseEffort(assignment.getBaseEffort());
               assignmentVersion.setBaseCosts(assignment.getBaseCosts());
               assignmentVersion.setBaseProceeds(assignment.getBaseProceeds());
               broker.makePersistent(assignmentVersion);
            }

            // create workmonth versions
            for (OpWorkMonth workMonth : assignment.getWorkMonths()) {
               OpWorkMonthVersion version = createWorkMonthVersion(workMonth);
               if (version != null) {
                  version.setAssignmentVersion(assignmentVersion);
                  broker.makePersistent(version);
               }
            }
         }
         logger.debug("TIMING: newProjectPlanVersion #04: "
               + (System.currentTimeMillis() - now));
         // Copy work phases
         Iterator workPeriods = projectPlan.getWorkPeriods().iterator();
         OpWorkPeriod workPeriod = null;
         OpWorkPeriodVersion workPeriodVersion = null;
         while (workPeriods.hasNext()) {
            workPeriod = (OpWorkPeriod) workPeriods.next();
            if (workPeriod.getActivity().getType() == OpActivity.ADHOC_TASK) {
               continue;
            }
            Long activityId = new Long(workPeriod.getActivity().getId());
            Pair<OpActivity, OpActivityVersion> pair = activityVersionMap.get(activityId);
            if (pair != null) {
               workPeriodVersion = new OpWorkPeriodVersion();
               workPeriodVersion.setPlanVersion(planVersion);
               workPeriodVersion.setActivityVersion((OpActivityVersion) pair.getSecond());
               workPeriodVersion.setStart(workPeriod.getStart());
               workPeriodVersion.setWorkingDays(workPeriod.getWorkingDays());
               workPeriodVersion.setBaseEffort(workPeriod.getBaseEffort());
               broker.makePersistent(workPeriodVersion);
            }
         }

         logger.debug("TIMING: newProjectPlanVersion #05: "
               + (System.currentTimeMillis() - now));
         // Copy dependencies
         Iterator dependencies = projectPlan.getDependencies().iterator();
         OpDependency dependency = null;
         OpDependencyVersion dependencyVersion = null;
         while (dependencies.hasNext()) {
            dependency = (OpDependency) dependencies.next();
            Pair<OpActivity, OpActivityVersion> pair = activityVersionMap.get(new Long(dependency.getPredecessorActivity().getId()));
            Pair<OpActivity, OpActivityVersion> pair2 = activityVersionMap.get(new Long(dependency.getSuccessorActivity().getId()));
            if ((pair != null) && (pair2 != null)) {
               dependencyVersion = new OpDependencyVersion();
               dependencyVersion.setPlanVersion(planVersion);
               dependencyVersion.setPredecessorVersion((OpActivityVersion) pair.getSecond());
               dependencyVersion.setSuccessorVersion((OpActivityVersion) pair2.getSecond());
               dependencyVersion.setDependencyType(dependency.getDependencyType());
               broker.makePersistent(dependencyVersion);
            }
         }

         logger.debug("TIMING: newProjectPlanVersion #06: "
               + (System.currentTimeMillis() - now));
      }

      return planVersion;

   }

   /**
    * @param session
    * @param broker
    * @param activityVersion
    * @param actIfc
    * @pre
    * @post
    */
   protected void clone(OpProjectSession session, OpBroker broker,
         OpActivityVersion activityVersion, OpActivity act, boolean progressTracked) {
      activityVersion.cloneSimpleMembers(act, progressTracked);
      if (activityVersion.effortCalculatedFromChildren()) {
         activityVersion.resetAggregatedValuesForCollection();
      }
   }

   public OpProjectPlanVersion createProjectPlanVersionObject(
         OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan,
         OpUser creator, int versionNumber) {
      // to activity-version rows
      OpProjectPlanVersion planVersion = new OpProjectPlanVersion();
      planVersion.setStart(projectPlan.getStart());
      planVersion.setFinish(projectPlan.getFinish());

      broker.makePersistent(planVersion);
      planVersion.setCreator(creator != null ? creator.getDisplayName()
            : projectPlan.getCreator());
      projectPlan.addProjectPlanVersion(planVersion);

      if (versionNumber != OpProjectPlan.WORKING_VERSION_NUMBER) {
         projectPlan.incrementVersionNumber();
         broker.updateObject(projectPlan);
         planVersion.setVersionNumber(projectPlan.getVersionNumber());
      } else {
         planVersion.setVersionNumber(versionNumber);
      }

      planVersion.setTemplate(projectPlan.getTemplate());
      if (projectPlan.getWorkCalendar() != null) {
         projectPlan.getWorkCalendar().addProjectPlanVersion(planVersion);
      }
      planVersion.setRecalculated(new Timestamp(System.currentTimeMillis()));
      return planVersion;
   }

   /**
    * Creates a workmonth version form the given workmonth entity.
    * 
    * @param workMonth
    *           Workmonth entity to create the version upon
    * @return a workmonth version
    */
   private static OpWorkMonthVersion createWorkMonthVersion(
         OpWorkMonth workMonth) {
      OpWorkMonthVersion workMonthVersion = new OpWorkMonthVersion();
      double effort = workMonth.getLatestEffort();
      double costs = workMonth.getLatestPersonnelCosts();
      double proceeds = workMonth.getLatestProceeds();
      if (effort == 0 && costs == 0 && proceeds == 0) {
         return null;
      }
      workMonthVersion.setBaseAssigned(workMonth.getLatestAssigned());
      workMonthVersion.setBaseEffort(workMonth.getBaseEffort());
      workMonthVersion.setBasePersonnelCosts(workMonth.getBasePersonnelCosts());
      workMonthVersion.setBaseProceeds(workMonth.getBaseProceeds());
      workMonthVersion.setMonth(workMonth.getMonth());
      workMonthVersion.setYear(workMonth.getYear());
      return workMonthVersion;
   }

   private final static String DELETE_WORK_MONTH_VERSIONS_FOR_PLAN = "delete OpWorkMonthVersion as workVersion where workVersion.AssignmentVersion.id in (select version.id from OpAssignmentVersion as version where version.PlanVersion.id = :planVersion)";

   private final static String DELETE_ASSIGNMENT_VERSIONS_FOR_PLAN = "delete OpAssignmentVersion as version where version.PlanVersion.id = :planVersion";

   private final static String DELETE_DEPENDENCY_VERSIONS_FOR_PLAN = "delete OpDependencyVersion as version where version.PlanVersion.id = :planVersion";

   private final static String DELETE_WORK_PERIOD_VERSIONS_FOR_PLAN = "delete OpWorkPeriodVersion as version where version.PlanVersion.id = :planVersion";

   private final static String UNLINK_CUSTOM_VALUE_PAGE = "update OpActivityVersion as version set version.CustomValuePage = null where version.PlanVersion.id = :planVersion";

   private final static String DELETE_CUSTOM_ATTRIBUTES_FOR_PLAN = "delete OpCustomValuePage as custom where custom.Object.id in (select version.id from OpActivityVersion as version where version.PlanVersion.id = :planVersion)";

   private final static String DELETE_ACTIONS_FOR_PLAN = "delete OpActionVersion as action where action.Activity.id in (select version.id from OpActivityVersion as version where version.PlanVersion.id = :planVersion)";

   private final static String DELETE_ACTIVITY_VERSIONS_FOR_PLAN_LEVEL = "delete OpActivityVersion as version where version.PlanVersion.id = :planVersion and OutlineLevel = :level";

   private final static String DELETE_ACTIVITY_VERSIONS_FOR_PLAN = "delete OpActivityVersion as version where version.PlanVersion = :planVersion";

   private final static String GET_OUTLINE_LEVEL_FOR_PLAN = "select max(act.OutlineLevel) from OpActivityVersion as act where act.PlanVersion.id = :planVersion";

   private final static String DELETE_ATTACHMENT_VERSIONS_FOR_PLAN = "delete OpAttachmentVersion as attachmentVersion where attachmentVersion.ActivityVersion.id in (select version.id from OpActivityVersion as version where version.PlanVersion.id = :planVersion)";

   private final static String DELETE_WORK_BREAKS_VERSIONS_FOR_PLAN = "delete OpActivityVersionWorkBreak as wbVersion where wbVersion.PlanVersion.id = :planVersion";

   public static void deleteProjectPlanVersion(OpBroker broker,
         OpProjectPlanVersion planVersion) {
      long start = System.currentTimeMillis();
      // Decrease ref-count of reused content objects (attachments) and delete
      // project plan (uses cascading)
      // Get all attachments, decrease ref-counts and delete content is
      // ref-count is zero
      List<OpAttachmentVersion> attachmentList = getAttachmentVersionsFromPlanVersion(
            broker, planVersion);
      logger.debug("TIMING: deleteProjectPlanVersion #00: "
            + (System.currentTimeMillis() - start));
      OpContent content = null;
      for (OpAttachmentVersion attachment : attachmentList) {
         if (attachment.getContent() != null) {
            // Decrease ref-count of reused content object; delete if ref-count
            // is zero
            content = attachment.getContent();
            OpContentManager.updateContent(content, broker, false, attachment);
            attachment.setContent(null);
         }
         broker.deleteObject(attachment);
      }
      logger.debug("TIMING: deleteProjectPlanVersion #01: "
            + (System.currentTimeMillis() - start));
      boolean iterative = false;

      // delete work month versions prior to assignment versions
      OpQuery deleteWorkMonthVersions = broker
            .newQuery(DELETE_WORK_MONTH_VERSIONS_FOR_PLAN);
      deleteWorkMonthVersions.setLong("planVersion", planVersion.getId());
      broker.execute(deleteWorkMonthVersions);

      logger.debug("TIMING: deleteProjectPlanVersion #02: "
            + (System.currentTimeMillis() - start));
      OpQuery deleteAssignmentVersions = broker
            .newQuery(DELETE_ASSIGNMENT_VERSIONS_FOR_PLAN);
      deleteAssignmentVersions.setLong("planVersion", planVersion.getId());
      broker.execute(deleteAssignmentVersions);

      logger.debug("TIMING: deleteProjectPlanVersion #03: "
            + (System.currentTimeMillis() - start));
      OpQuery deleteWorkPeriodVersions = broker
            .newQuery(DELETE_WORK_PERIOD_VERSIONS_FOR_PLAN);
      deleteWorkPeriodVersions.setLong("planVersion", planVersion.getId());
      broker.execute(deleteWorkPeriodVersions);

      logger.debug("TIMING: deleteProjectPlanVersion #04: "
            + (System.currentTimeMillis() - start));
      OpQuery deleteDependencyVersions = broker
            .newQuery(DELETE_DEPENDENCY_VERSIONS_FOR_PLAN);
      deleteDependencyVersions.setLong("planVersion", planVersion.getId());
      broker.execute(deleteDependencyVersions);

      logger.debug("TIMING: deleteProjectPlanVersion #05: "
            + (System.currentTimeMillis() - start));
      OpQuery unlinkCVPs = broker.newQuery(UNLINK_CUSTOM_VALUE_PAGE);
      unlinkCVPs.setLong("planVersion", planVersion.getId());
      broker.execute(unlinkCVPs);

      logger.debug("TIMING: deleteProjectPlanVersion #06: "
            + (System.currentTimeMillis() - start));
      OpQuery deleteWorkBreaksForVersions = broker
            .newQuery(DELETE_WORK_BREAKS_VERSIONS_FOR_PLAN);
      deleteWorkBreaksForVersions.setLong("planVersion", planVersion.getId());
      broker.execute(deleteWorkBreaksForVersions);

      logger.debug("TIMING: deleteProjectPlanVersion #06a: "
            + (System.currentTimeMillis() - start));
      OpQuery deleteCustomValuePagesForVersions = broker
            .newQuery(DELETE_CUSTOM_ATTRIBUTES_FOR_PLAN);
      deleteCustomValuePagesForVersions.setLong("planVersion", planVersion
            .getId());
      broker.execute(deleteCustomValuePagesForVersions);

      logger.debug("TIMING: deleteProjectPlanVersion #06b: "
            + (System.currentTimeMillis() - start));
      OpQuery deleteActionsForVersions = broker
            .newQuery(DELETE_ACTIONS_FOR_PLAN);
      deleteActionsForVersions.setLong("planVersion", planVersion.getId());
      broker.execute(deleteActionsForVersions);

      // Attachments are processed above ;-) (hopefully not so many ...)
      // FIXME(dfreis 14.08.2008 18:55:38) did not work so added code again!!
      OpQuery deleteAttachmentVersions = broker
            .newQuery(DELETE_ATTACHMENT_VERSIONS_FOR_PLAN);
      deleteAttachmentVersions.setLong("planVersion", planVersion.getId());
      broker.execute(deleteAttachmentVersions);

      logger.debug("TIMING: deleteProjectPlanVersion #07: "
            + (System.currentTimeMillis() - start));
      if (iterative) {
         // get max Outline Level:
         OpQuery maxOutlineLevelQuery = broker
               .newQuery(GET_OUTLINE_LEVEL_FOR_PLAN);
         maxOutlineLevelQuery.setLong("planVersion", planVersion.getId());
         List<Byte> maxOutlineLevels = broker.list(maxOutlineLevelQuery);
         byte maxLevel = maxOutlineLevels.size() == 1
               && maxOutlineLevels.get(0) != null ? maxOutlineLevels.get(0)
               .byteValue() : 0;
         // delete activities, starting with highest outline-level
         while (maxLevel >= 0) {
            OpQuery deleteActivityVersions = broker
                  .newQuery(DELETE_ACTIVITY_VERSIONS_FOR_PLAN_LEVEL);
            deleteActivityVersions.setLong("planVersion", planVersion.getId());
            deleteActivityVersions.setInteger("level", maxLevel);
            broker.execute(deleteActivityVersions);
            maxLevel--;
            logger.debug("TIMING: deleteProjectPlanVersion #08i: "
                  + (System.currentTimeMillis() - start));
         }
      } else {
         OpQuery deleteActivityVersions = broker
               .newQuery(DELETE_ACTIVITY_VERSIONS_FOR_PLAN);
         deleteActivityVersions.setLong("planVersion", planVersion.getId());
         broker.execute(deleteActivityVersions);
         logger.debug("TIMING: deleteProjectPlanVersion #08: "
               + (System.currentTimeMillis() - start));
      }
      OpProjectPlan projectPlan = planVersion.getProjectPlan();
      projectPlan.removeProjectPlanVersion(planVersion);
      if (projectPlan.getWorkingVersion() != null && projectPlan.getWorkingVersion().getId() == planVersion.getId()) {
         projectPlan.setWorkingVersion(null);
      }

      broker.refreshObject(planVersion);
      broker.deleteObject(planVersion);
      logger.debug("TIMING: deleteProjectPlanVersion #09: "
            + (System.currentTimeMillis() - start));
   }

   /**
    * Returns the number of subactivity versions for the activity version
    * specified as parameter.
    * 
    * @param broker -
    *           the <code>OpBroker</code> object needed to perform DB
    *           operations.
    * @param activityVersion -
    *           the <code>OpActivityVersion</code> object.
    * @return the number of subactivity versions for the activity version
    *         specified as parameter.
    */
   public static int getSubactivityVersionsCount(OpBroker broker,
         OpActivityVersion activityVersion) {
      if (activityVersion.getSubActivityVersions() != null) {
         OpQuery query = broker
               .newQuery(GET_SUBACTIVITY_VERSION_COUNT_FOR_ACTIVITY_VERSION);
         query.setLong("activityVersionId", activityVersion.getId());
         Number counter = (Number) broker.iterate(query).next();
         return counter.intValue();
      }
      return 0;
   }

   /**
    * Returns a <code>List</code> of attachment versions which are set on the
    * activity versions belonging to the <code>OpProjectPlanVersion</code>
    * passed as parameter.
    * 
    * @param planVersion -
    *           the <code>OpProjectPlanVersion</code> for which the attachment
    *           versions are returned.
    * @return a <code>List</code> of attachment versions which are set on the
    *         activity versions belonging to the
    *         <code>OpProjectPlanVersion</code> passed as parameter.
    */
   public static List<OpAttachmentVersion> getAttachmentVersionsFromPlanVersion(
         OpBroker broker, OpProjectPlanVersion planVersion) {
      OpQuery query = broker.newQuery(GET_ATTACHMENT_VERSIONS_FOR_PLAN_VERSION);
      query.setLong("planVersionId", planVersion.getId());
      return broker.list(query);
   }

   /**
    * Returns an <code>Iterator</code> over the collection of attachment
    * versions which are set on the activity versions belonging to the
    * <code>OpProjectPlanVersion</code> passed as parameter. (Note: this
    * method loads all the activity versions belonging to the project plan
    * version and all their attachment versions. Use only when these objects are
    * already loaded.)
    * 
    * @param planVersion -
    *           the <code>OpProjectPlanVersion</code> for which the attachment
    *           versions are returned.
    * @return an <code>Iterator</code> over the collection of attachment
    *         versions which are set on the activity versions belonging to the
    *         <code>OpProjectPlanVersion</code> passed as parameter.
    */
   private static Iterator<OpAttachmentIfc> getAttachmentVersionsFromPlanVersion(
         OpProjectPlanVersion planVersion) {
      Map<String, OpAttachmentIfc> attachmentMap = new HashMap<String, OpAttachmentIfc>();
      for (OpActivityVersion activityVersion : planVersion
            .getActivityVersions()) {
         if (activityVersion.getAttachmentVersions() != null) {
            for (OpAttachmentIfc attachmentVersion : activityVersion
                  .getAttachmentVersions()) {
               if (attachmentMap.get(attachmentVersion.locator()) == null) {
                  attachmentMap.put(attachmentVersion.locator(),
                        attachmentVersion);
               }
            }
         }
      }

      return attachmentMap.values().iterator();
   }

   /**
    * Returns an instance of the OpProjectPlanningService
    * 
    * @return an instance of the OpProjectPlanningService
    */
   public static void register(OpActivityVersionDataSetFactory dataSetFactory) {
      instance = dataSetFactory;
   }

   /**
    * Returns an instance of the data set factory
    * 
    * @return an instance of the data set factory
    */
   public static OpActivityVersionDataSetFactory getInstance() {
      return instance;
   }

   private static class WorkMonthCreationSyncHelper extends OpCollectionSynchronizationHelper<OpWorkMonthVersion, OpWorkMonthVersion> {

      private OpAssignmentVersion assV = null;
      private OpBroker broker = null;
      
      public WorkMonthCreationSyncHelper(OpAssignmentVersion assV, OpBroker broker) {
         this.assV = assV;
         this.broker = broker;
      }
      
      @Override
      protected int cloneInstance(OpWorkMonthVersion tgt, OpWorkMonthVersion src) {
         tgt.setBaseAssigned(src.getBaseAssigned());
         tgt.setBaseEffort(src.getBaseEffort());
         tgt.setBasePersonnelCosts(src.getBasePersonnelCosts());
         tgt.setBaseProceeds(src.getBaseProceeds());
         
         tgt.setMonth(src.getMonth());
         tgt.setYear(src.getYear());
         return OK;
      }

      @Override
      protected int corresponds(OpWorkMonthVersion tgt, OpWorkMonthVersion src) {
         return cmpLongLong(tgt.getYear(), tgt.getMonth(), src.getYear(), src.getMonth());
      }

      @Override
      protected void deleteInstance(OpWorkMonthVersion del) {
         if (del.getAssignmentVersion() != null) {
            del.getAssignmentVersion().removeWorkMonthVersion(del);
         }
         broker.deleteObject(del);
      }

      @Override
      protected OpWorkMonthVersion newInstance(OpWorkMonthVersion src) {
         OpWorkMonthVersion wmv = new OpWorkMonthVersion();
         assV.addWorkMonthVersion(wmv);
         broker.makePersistent(wmv);
         return wmv;
      }

      @Override
      protected int sourceOrder(OpWorkMonthVersion cm2a, OpWorkMonthVersion cm2b) {
         return cmpLongLong(cm2a.getYear(), cm2a.getMonth(), cm2b.getYear(), cm2b.getMonth());
      }

      @Override
      protected int targetOrder(OpWorkMonthVersion cm1a, OpWorkMonthVersion cm1b) {
         return cmpLongLong(cm1a.getYear(), cm1a.getMonth(), cm1b.getYear(), cm1b.getMonth());
      }
      
   }
   
   public void createWorkMonthVersions(OpProjectSession session, OpBroker broker, OpAssignmentVersion assV) {
      List<OpWorkMonthVersion> newWMVs = new ArrayList<OpWorkMonthVersion>();
   }
}