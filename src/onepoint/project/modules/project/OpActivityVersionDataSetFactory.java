/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;

import java.sql.Date;
import java.util.*;

public abstract class OpActivityVersionDataSetFactory {

   private static final XLog logger = XLogFactory.getLogger(OpActivityVersionDataSetFactory.class, true);

   public static void retrieveActivityVersionDataSet(OpBroker broker, OpProjectPlanVersion planVersion,
        XComponent dataSet, boolean editable) {

      // Activities: Fill data set with activity data rows and create activity data row map
      OpQuery query = broker
           .newQuery("select activity from OpActivityVersion as activity where activity.PlanVersion.ID = ? order by activity.Sequence");
      query.setLong(0, planVersion.getID());
      Iterator activities = broker.iterate(query);
      OpActivityVersion activity = null;
      XComponent dataRow = null;
      while (activities.hasNext()) {
         activity = (OpActivityVersion) activities.next();
         dataRow = new XComponent(XComponent.DATA_ROW);
         retrieveActivityVersionDataRow(activity, dataRow, editable);
         if (activity.getType() == OpActivityVersion.TASK || activity.getType() == OpActivityVersion.COLLECTION_TASK) {
            OpGanttValidator.setStart(dataRow, null);
            OpGanttValidator.setEnd(dataRow, null);
         }
         dataSet.addChild(dataRow);
      }

      // Assignments: Fill resources and resource base efforts columns
      Iterator assignments = planVersion.getAssignmentVersions().iterator();
      OpAssignmentVersion assignment = null;
      OpResource resource = null;
      Map resourceAvailability = new HashMap();
      //map of [activitySequence, sum(activity.assignments.baseEffort)]
      Map activityAssignmentsSum = new HashMap();
      while (assignments.hasNext()) {
         assignment = (OpAssignmentVersion) assignments.next();

         activity = assignment.getActivityVersion();
         Integer activitySequence = new Integer(activity.getSequence());
         if (activityAssignmentsSum.get(activitySequence) == null && assignment.getBaseEffort() < activity.getBaseEffort()) {
            activityAssignmentsSum.put(activitySequence, new Double(assignment.getBaseEffort()));
         }
         else if (activityAssignmentsSum.get(activitySequence) != null) {
            double effortSum = ((Double) activityAssignmentsSum.get(activitySequence)).doubleValue();
            effortSum += assignment.getBaseEffort();
            if (effortSum < activity.getBaseEffort()) {
               activityAssignmentsSum.put(activitySequence, new Double(effortSum));
            }
            else {
               activityAssignmentsSum.remove(activitySequence);
            }
         }

         dataRow = (XComponent) dataSet.getChild(assignment.getActivityVersion().getSequence());
         resource = assignment.getResource();
         String caption = resource.getName();
         resourceAvailability.put(resource.locator(), new Double(resource.getAvailable()));
         String assignedString = String.valueOf(assignment.getAssigned());
         caption += " " + assignedString + "%";
         OpGanttValidator.addResource(dataRow, XValidator.choice(resource.locator(), caption));
         OpGanttValidator.addResourceBaseEffort(dataRow, assignment.getBaseEffort());
      }

      //update the resources to take into account invisible resources (independent planning only)
      if (planVersion.getProjectPlan().getCalculationMode() == OpGanttValidator.INDEPENDENT) {
         Iterator it = activityAssignmentsSum.keySet().iterator();
         while (it.hasNext()) {
            Integer sequence = (Integer) it.next();
            dataRow = (XComponent) dataSet.getChild(sequence.intValue());
            Double assignmentsEffortSum = (Double) activityAssignmentsSum.get(sequence);
            double noResourceHours = OpGanttValidator.getBaseEffort(dataRow) - assignmentsEffortSum.doubleValue();
            if (noResourceHours > OpGanttValidator.ERROR_MARGIN) {
               resourceAvailability.put(OpGanttValidator.NO_RESOURCE_ID, new Double(Integer.MAX_VALUE));
               double percent = noResourceHours * 100.0 / OpGanttValidator.getDuration(dataRow);
               String caption = OpGanttValidator.NO_RESOURCE_NAME + " " + String.valueOf(percent) + "%";
               String noResource = XValidator.choice(OpGanttValidator.NO_RESOURCE_ID, caption);
               OpGanttValidator.addResource(dataRow, noResource);
            }
         }
      }

      //set also the visual resources (uses the value of the dataset as a value holder)
      Boolean showHours = (Boolean) dataSet.getValue();
      if (showHours == null) {
         showHours = Boolean.valueOf(OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS));
      }
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         OpGanttValidator.updateVisualResources(dataRow, showHours.booleanValue(), resourceAvailability);
      }

      // Dependencies: Fill predecessor and successor columns
      Iterator dependencies = planVersion.getDependencyVersions().iterator();
      OpDependencyVersion dependency = null;
      XComponent predecessorDataRow = null;
      XComponent successorDataRow = null;
      while (dependencies.hasNext()) {
         dependency = (OpDependencyVersion) dependencies.next();
         predecessorDataRow = (XComponent) dataSet.getChild(dependency.getPredecessorVersion().getSequence());
         successorDataRow = (XComponent) dataSet.getChild(dependency.getSuccessorVersion().getSequence());
         OpGanttValidator.addPredecessor(successorDataRow, predecessorDataRow.getIndex());
         OpGanttValidator.addSuccessor(predecessorDataRow, successorDataRow.getIndex());
      }

      // WorkPhases: Fill work phase starts, finishes and base effort columns
      Iterator workPeriodsIt = planVersion.getWorkPeriodVersions().iterator();
      OpWorkPeriodVersion workPeriod = null;
      while (workPeriodsIt.hasNext()) {
         workPeriod = (OpWorkPeriodVersion) workPeriodsIt.next();
         dataRow = (XComponent) dataSet.getChild(workPeriod.getActivityVersion().getSequence());
         List workPeriodValues = new ArrayList();
         workPeriodValues.add(new Long(workPeriod.getWorkingDays()));
         workPeriodValues.add(new Double(workPeriod.getBaseEffort()));
         OpActivityDataSetFactory.addWorkPhases(dataRow, workPeriod.getStart(), workPeriodValues);
      }

      // Note: Comments are only available for activities (not activity versions)

   }

   private static void retrieveAttachmentVersionsDataCell(Set attachments, XComponent dataRow) {
      // TODO: Bulk-fetch like other parts of the project plan
      ArrayList attachmentList = OpGanttValidator.getAttachments(dataRow);
      Iterator i = attachments.iterator();
      OpAttachmentVersion attachment = null;
      ArrayList attachmentElement = null;
      while (i.hasNext()) {
         attachment = (OpAttachmentVersion) i.next();
         attachmentElement = new ArrayList();
         if (attachment.getLinked()) {
            attachmentElement.add(OpActivityDataSetFactory.LINKED_ATTACHMENT_DESCRIPTOR);
         }
         else {
            attachmentElement.add(OpActivityDataSetFactory.DOCUMENT_ATTACHMENT_DESCRIPTOR);
         }
         attachmentElement.add(attachment.locator());
         attachmentElement.add(attachment.getName());
         attachmentElement.add(attachment.getLocation());
         if (!attachment.getLinked()) {
            if (attachment.getContent() == null) {
               logger.error("Found attachment:" + attachment.getName() + " with no content");
               continue;
            }
            String contentId = OpLocator.locatorString(attachment.getContent());
            attachmentElement.add(contentId);
         }
         else {
            attachmentElement.add(OpActivityDataSetFactory.NO_CONTENT_ID);
         }
         attachmentList.add(attachmentElement);
      }
   }

   private static void retrieveActivityVersionDataRow(OpActivityVersion activityVersion, XComponent dataRow,
        boolean editable) {

      dataRow.setStringValue(activityVersion.locator());

      boolean isCollection = (activityVersion.getType() == OpActivityVersion.COLLECTION || activityVersion.getType() == OpActivityVersion.COLLECTION_TASK);
      boolean isStandard = (activityVersion.getType() == OpActivityVersion.STANDARD);
      boolean isStrictlyTask = (activityVersion.getType() == OpActivity.TASK);
      boolean isTask = (isStrictlyTask || activityVersion.getType() == OpActivityVersion.COLLECTION_TASK);
      boolean isScheduledTask = (activityVersion.getType() == OpActivityVersion.SCHEDULED_TASK);

      dataRow.setOutlineLevel(activityVersion.getOutlineLevel());
      dataRow.setExpanded(activityVersion.getExpanded());

      // Name (0)
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable);
      dataCell.setStringValue(activityVersion.getName());
      dataRow.addChild(dataCell);

      // Type (1)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection);
      dataCell.setByteValue(activityVersion.getType());
      dataRow.addChild(dataCell);

      // Category (2)
      // TODO: For this to work we also need to set color-formatters for activityVersion-table
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection);
      OpActivityCategory category = activityVersion.getCategory();
      if (category != null) {
         dataCell.setStringValue(XValidator.choice(category.locator(), category.getName()));
      }
      dataRow.addChild(dataCell);

      // Complete (3);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(activityVersion.getComplete());
      dataRow.addChild(dataCell);
      OpProjectPlanVersion planVersion = activityVersion.getPlanVersion();
      OpProjectPlan projectPlan = planVersion.getProjectPlan();
      boolean tracking = projectPlan.getProgressTracked();
      dataCell.setEnabled(editable & !tracking && !isCollection);

      // Start (4)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection);
      dataCell.setDateValue(activityVersion.getStart());
      dataRow.addChild(dataCell);

      // Finish (5)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection);
      dataCell.setDateValue(activityVersion.getFinish());
      dataRow.addChild(dataCell);

      // Duration (6)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection && !isTask);
      dataCell.setDoubleValue(activityVersion.getDuration());
      dataRow.addChild(dataCell);

      // BaseEffort (7)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection && !isScheduledTask);
      dataCell.setDoubleValue(activityVersion.getBaseEffort());
      dataRow.addChild(dataCell);

      // Predecessors (8)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      // Successors (9)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      // Resources (10)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection && !isScheduledTask);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      // PersonnelCosts (11); not editable (calculated from resource assignments and hourly rates)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(activityVersion.getBasePersonnelCosts());
      dataRow.addChild(dataCell);

      // BaseTravelCosts (12); only editable for standard activities
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && (isStandard || isStrictlyTask));
      dataCell.setDoubleValue(activityVersion.getBaseTravelCosts());
      dataRow.addChild(dataCell);

      // BaseMaterialCosts (13); only editable for standard activities
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && (isStandard || isStrictlyTask));
      dataCell.setDoubleValue(activityVersion.getBaseMaterialCosts());
      dataRow.addChild(dataCell);

      // BaseExternalCosts (14); only editable for standard activities
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && (isStandard || isStrictlyTask));
      dataCell.setDoubleValue(activityVersion.getBaseExternalCosts());
      dataRow.addChild(dataCell);

      // BaseMiscellaneousCosts (15); only editable for standard activities
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && (isStandard || isStrictlyTask));
      dataCell.setDoubleValue(activityVersion.getBaseMiscellaneousCosts());
      dataRow.addChild(dataCell);

      // Description (16); not editable in table
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(activityVersion.getDescription());
      dataRow.addChild(dataCell);

      // Attachments (17); not editable in table
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);
      if (activityVersion.getAttachmentVersions().size() > 0) {
         retrieveAttachmentVersionsDataCell(activityVersion.getAttachmentVersions(), dataRow);
      }

      // Attributes (18); not editable
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(activityVersion.getAttributes());
      dataRow.addChild(dataCell);

      // WorkPhaseStarts (19); not editable
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      // WorkPhaseFinishes (20); not editable
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      // WorkPhaseBaseEfforts (21); not editable
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      // ResourceBaseEfforts (22); not editable
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      // Priority (23); editable for tasks
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && isTask);
      Byte value = (activityVersion.getPriority() == 0) ? null : new Byte(activityVersion.getPriority());
      dataCell.setValue(value);
      dataRow.addChild(dataCell);

      //Workrecords (24): a map of [resourceLocator, hasWorkRecords]
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(false);
      Map data = null;
      if (activityVersion.getActivity() != null) {
         Set assignments = activityVersion.getActivity().getAssignments();
         data = new HashMap(assignments.size());
         Iterator it = assignments.iterator();
         while (it.hasNext()) {
            OpAssignment assignment = (OpAssignment) it.next();
            String resourceLocator = assignment.getResource().locator();
            Boolean hasWorkRecords = (assignment.getWorkRecords() != null) ? Boolean.valueOf(assignment.getWorkRecords().size() > 0) : Boolean.FALSE;
            data.put(resourceLocator, hasWorkRecords);
         }
      }
      else {
         data = new HashMap();
      }
      dataCell.setValue(data);
      dataRow.addChild(dataCell);

      //actual effort (25)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(false);
      double completeValue = (activityVersion.getActivity() != null) ? activityVersion.getActivity().getActualEffort() : 0;
      dataCell.setDoubleValue(completeValue);
      dataRow.addChild(dataCell);

      //Visual resources (26) - needed for %complete
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection && !isScheduledTask);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      //Responsible resource (27)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(false);
      OpResource responsibleResource = activityVersion.getResponsibleResource();
      if (responsibleResource != null) {
         dataCell.setStringValue(XValidator.choice(responsibleResource.locator(), responsibleResource.getName()));
      }
      dataRow.addChild(dataCell);

      // Add project column (28)
      dataCell = new XComponent(XComponent.DATA_CELL);
      OpProjectNode projectNode = activityVersion.getPlanVersion().getProjectPlan().getProjectNode();
      dataCell.setStringValue(XValidator.choice(projectNode.locator(), projectNode.getName()));
      dataRow.addChild(dataCell);

      OpGanttValidator.updateAttachmentAttribute(dataRow);
   }

   public static HashMap activityVersions(OpProjectPlanVersion planVersion) {
      HashMap activityVersions = new HashMap();
      // Check if this is a new project plan
      if (planVersion.getActivityVersions() != null) {
         Iterator i = planVersion.getActivityVersions().iterator();
         OpActivityVersion activityVersion = null;
         while (i.hasNext()) {
            activityVersion = (OpActivityVersion) i.next();
            activityVersions.put(new Long(activityVersion.getID()), activityVersion);
         }
      }
      return activityVersions;
   }

   private static boolean mapActivityIDs(OpBroker broker, XComponent dataSet, OpProjectPlanVersion workingPlanVersion) {
      // Exchange all activity IDs contained in data-row values with their respective working activity version IDs

      HashMap activityIdMap = new HashMap();
      OpQuery query = broker.newQuery("select activityVersion.Activity.ID, activityVersion.ID from OpActivityVersion as activityVersion where activityVersion.PlanVersion.ID = ?");
      query.setLong(0, workingPlanVersion.getID());
      Iterator result = broker.list(query).iterator();
      Object[] record = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         System.err.println("   ***MAPPING AIDS " + record[0] + " -> " + record[1]);
         activityIdMap.put(record[0], record[1]);
      }

      XComponent dataRow = null;
      Long activityVersionId = null;
      int mappedActivityIds = 0;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (dataRow.getStringValue() != null) {
            activityVersionId = (Long) activityIdMap.get(new Long(OpLocator.parseLocator(dataRow.getStringValue()).getID()));
            if (activityVersionId != null) {
               dataRow.setStringValue(OpLocator.locatorString(OpActivityVersion.ACTIVITY_VERSION, activityVersionId.longValue()));
               mappedActivityIds++;
            }
         }
      }

      return mappedActivityIds > 0;

   }

   public static void storeActivityVersionDataSet(OpBroker broker, XComponent dataSet, OpProjectPlanVersion planVersion,
        HashMap resources, boolean fromProjectPlan) {

      System.err.println("STORE-ACTIVITY-VERSION-DATA-SET " + dataSet.getChildCount());

      // TODO: Maybe use "pure" IDs (Long values) for data-row values instead of locator strings (retrieve)

      // If the data-set was constructed from a project plan exchange activity ID row values with activity version IDs
      HashMap activities = null;
      if (fromProjectPlan) {
         // If activities could not be mapped then prefetch activities for inserting new activity versions
         if (!mapActivityIDs(broker, dataSet, planVersion)) {
            activities = OpActivityDataSetFactory.activities(planVersion.getProjectPlan());
         }
      }

      System.err.println("   ACTIVITIES " + activities);

      // Prefetch activity versions
      HashMap activityVersions = activityVersions(planVersion);
      Date planStart = planVersion.getStart();
      Date planFinish = planVersion.getFinish();

      // Phase 1: Iterate data-rows and store activity versions
      XComponent dataRow = null;
      OpActivityVersion activityVersion = null;
      OpActivityVersion previousActivityVersion = null;
      int previousOutlineLevel = 0;
      OpActivityVersion superActivityVersion = null;
      Stack superActivityStack = new Stack();
      ArrayList activityVersionList = new ArrayList();
      int i = 0;
      for (i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (dataRow.getOutlineLevel() > previousOutlineLevel) {
            if (superActivityVersion != null) {
               superActivityStack.push(superActivityVersion);
            }
            superActivityVersion = previousActivityVersion;
         }
         else if (dataRow.getOutlineLevel() < previousOutlineLevel) {
            for (int k = 0; k < previousOutlineLevel - dataRow.getOutlineLevel(); k++) {
               if (superActivityStack.empty()) {
                  superActivityVersion = null;
                  break;
               }
               else {
                  superActivityVersion = (OpActivityVersion) (superActivityStack.pop());
               }
            }
         }
         // Remove activity version from activity versions map
         if (dataRow.getStringValue() != null) {
            activityVersion = (OpActivityVersion) activityVersions.remove(new Long(OpLocator.parseLocator(
                 dataRow.getStringValue()).getID()));
         }
         else {
            activityVersion = null;
         }
         activityVersion = insertOrUpdateActivityVersion(broker, dataRow, activityVersion, planVersion, superActivityVersion, activities);

         // Set locator string value for newly created activity version data rows
         if (dataRow.getStringValue() == null) {
            dataRow.setStringValue(activityVersion.locator());
         }

         // Check project plan start and finish dates
         if (activityVersion.getType() != OpActivity.TASK && activityVersion.getType() != OpActivity.COLLECTION_TASK) {
            if (activityVersion.getStart().getTime() < planStart.getTime()) {
               planStart = activityVersion.getStart();
            }
            if (activityVersion.getFinish().getTime() < planFinish.getTime()) {
               planFinish = activityVersion.getFinish();
            }
         }

         // Activity version list can be used to efficiently look-up activities by data-row index
         activityVersionList.add(activityVersion);
         previousActivityVersion = activityVersion;
         previousOutlineLevel = previousActivityVersion.getOutlineLevel();
      }

      // Phase 2: Iterate database contents; update and delete existing related activity data
      // (We need to check for existence of sets in case this is a new project plan)
      ArrayList reusableAssignmentVersions = null;
      if (planVersion.getAssignmentVersions() != null) {
         reusableAssignmentVersions = updateOrDeleteAssignmentVersions(broker, dataSet, planVersion
              .getAssignmentVersions().iterator());
      }
      ArrayList reusableWorkPeriodVersions = null;
      if (planVersion.getWorkPeriodVersions() != null) {
         reusableWorkPeriodVersions = updateOrDeleteWorkPeriodVersions(broker, dataSet, planVersion
              .getWorkPeriodVersions().iterator());
      }
      ArrayList reusableAttachmentVersions = null;
      if (planVersion.getAttachmentVersions() != null) {
         reusableAttachmentVersions = updateOrDeleteAttachmentVersions(broker, dataSet, planVersion
              .getAttachmentVersions().iterator());
      }
      ArrayList reusableDependencyVersions = null;
      if (planVersion.getDependencyVersions() != null) {
         reusableDependencyVersions = updateOrDeleteDependencyVersions(broker, dataSet, planVersion
              .getDependencyVersions().iterator());
      }

      // Phase 3: Iterate data-rows (second time); insert new related activity data
      for (i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         activityVersion = (OpActivityVersion) activityVersionList.get(i);
         // Only standard (work) activities may have assignments and work phases
         if (OpGanttValidator.getType(dataRow) == OpGanttValidator.STANDARD ||
              OpGanttValidator.getType(dataRow) == OpGanttValidator.TASK ||
              OpGanttValidator.getType(dataRow) == OpGanttValidator.MILESTONE) {
            insertActivityAssignmentVersions(broker, planVersion, dataRow, activityVersion, reusableAssignmentVersions,
                 resources);
         }
         if (OpGanttValidator.getType(dataRow) == OpGanttValidator.STANDARD) {
            insertActivityWorkPeriodVersions(broker, planVersion, dataRow, activityVersion, reusableWorkPeriodVersions);
         }
         insertActivityAttachmentVersions(broker, planVersion, dataRow, activityVersion, reusableAttachmentVersions);
         insertActivityDependencyVersions(broker, planVersion, dataSet, dataRow, activityVersion, activityVersionList,
              reusableDependencyVersions);
      }

      // Phase 4: Delete unused related objects
      if (reusableAssignmentVersions != null) {
         for (i = 0; i < reusableAssignmentVersions.size(); i++) {
            broker.deleteObject((OpAssignmentVersion) reusableAssignmentVersions.get(i));
         }
      }
      if (reusableWorkPeriodVersions != null) {
         for (i = 0; i < reusableWorkPeriodVersions.size(); i++) {
            broker.deleteObject((OpWorkPeriodVersion) reusableWorkPeriodVersions.get(i));
         }
      }
      // TOOD: Attention with attachments -- we have to decrement ref-count?
      if (reusableAttachmentVersions != null) {
         for (i = 0; i < reusableAttachmentVersions.size(); i++) {
            broker.deleteObject((OpAttachmentVersion) reusableAttachmentVersions.get(i));
         }
      }
      if (reusableDependencyVersions != null) {
         for (i = 0; i < reusableDependencyVersions.size(); i++) {
            broker.deleteObject((OpDependencyVersion) reusableDependencyVersions.get(i));
         }
      }

      // Phase 5: Delete unused activity versions
      Iterator unusedActivityVersions = activityVersions.values().iterator();
      while (unusedActivityVersions.hasNext()) {
         broker.deleteObject((OpActivityVersion) unusedActivityVersions.next());
      }

      // Finally, update project plan version start and finish fields
      planVersion.setStart(planStart);
      planVersion.setFinish(planFinish);
      broker.updateObject(planVersion);

   }

   private static OpActivityVersion insertOrUpdateActivityVersion(OpBroker broker, XComponent dataRow,
        OpActivityVersion activityVersion, OpProjectPlanVersion planVersion, OpActivityVersion superActivity, HashMap activities) {

      OpActivityCategory category = null;
      OpProjectNode projectNode = planVersion.getProjectPlan().getProjectNode();
      String categoryChoice = OpGanttValidator.getCategory(dataRow);
      String responsibleResource = OpGanttValidator.getResponsibleResource(dataRow);
      if (activityVersion == null) {
         // Insert a new activity
         activityVersion = new OpActivityVersion();
         activityVersion.setPlanVersion(planVersion);
         activityVersion.setTemplate(planVersion.getTemplate());

         // If activities is set this means that activity version does not yet exist
         // Therefore, set activity from activity locator in data-row
         if (activities != null) {
            String activityLocator = dataRow.getStringValue();
            if (activityLocator != null) {
               activityVersion.setActivity((OpActivity) activities.get(new Long(OpLocator.parseLocator(activityLocator).getID())));
               dataRow.setStringValue(null);
            }
         }

         activityVersion.setName(OpGanttValidator.getName(dataRow));
         activityVersion.setDescription(OpGanttValidator.getDescription(dataRow));
         activityVersion.setSequence(dataRow.getIndex());
         activityVersion.setOutlineLevel((byte) (dataRow.getOutlineLevel()));
         activityVersion.setExpanded(dataRow.getExpanded());
         activityVersion.setSuperActivityVersion(superActivity);
         if (superActivity != null && superActivity.getType() == OpActivity.SCHEDULED_TASK) {
            activityVersion.setStart(superActivity.getStart());
            activityVersion.setFinish(superActivity.getFinish());
         }
         else {
            if (OpGanttValidator.getStart(dataRow) == null) {
               activityVersion.setStart(projectNode.getStart());
            }
            else {
               activityVersion.setStart(OpGanttValidator.getStart(dataRow));
            }
            if (OpGanttValidator.getEnd(dataRow) == null) {
               activityVersion.setFinish(projectNode.getFinish());
            }
            else {
               activityVersion.setFinish(OpGanttValidator.getEnd(dataRow));
            }
         }
         activityVersion.setDuration(OpGanttValidator.getDuration(dataRow));
         activityVersion.setBaseEffort(OpGanttValidator.getBaseEffort(dataRow));
         activityVersion.setType((OpGanttValidator.getType(dataRow)));
         if (categoryChoice != null) {
            String categoryLocator = XValidator.choiceID(categoryChoice);
            category = (OpActivityCategory) broker.getObject(categoryLocator);
            activityVersion.setCategory(category);
         }
         if (responsibleResource != null) {
            String resourceLocator = XValidator.choiceID(responsibleResource);
            OpResource resource = (OpResource) broker.getObject(resourceLocator);
            activityVersion.setResponsibleResource(resource);
         }
         // Set complete once (we assume that client-side complete value is correct)
         activityVersion.setComplete(OpGanttValidator.getComplete(dataRow));
         activityVersion.setBasePersonnelCosts(OpGanttValidator.getBasePersonnelCosts(dataRow));
         activityVersion.setBaseTravelCosts(OpGanttValidator.getBaseTravelCosts(dataRow));
         activityVersion.setBaseMaterialCosts(OpGanttValidator.getBaseMaterialCosts(dataRow));
         activityVersion.setBaseExternalCosts(OpGanttValidator.getBaseExternalCosts(dataRow));
         activityVersion.setBaseMiscellaneousCosts(OpGanttValidator.getBaseMiscellaneousCosts(dataRow));
         activityVersion.setAttributes(OpGanttValidator.getAttributes(dataRow));
         byte priority = OpGanttValidator.getPriority(dataRow) != null ? OpGanttValidator.getPriority(dataRow).byteValue() : 0;
         activityVersion.setPriority(priority);

         broker.makePersistent(activityVersion);

      }
      else {

         boolean update = false;

         if (!OpActivityDataSetFactory.checkEquality(activityVersion.getName(), OpGanttValidator.getName(dataRow))) {
            update = true;
            activityVersion.setName(OpGanttValidator.getName(dataRow));
         }
         if (!OpActivityDataSetFactory.checkEquality(activityVersion.getDescription(), OpGanttValidator.getDescription(dataRow))) {
            update = true;
            activityVersion.setDescription(OpGanttValidator.getDescription(dataRow));
         }
         if (activityVersion.getSequence() != dataRow.getIndex()) {
            update = true;
            activityVersion.setSequence(dataRow.getIndex());
         }
         if (activityVersion.getOutlineLevel() != dataRow.getOutlineLevel()) {
            update = true;
            activityVersion.setOutlineLevel((byte) (dataRow.getOutlineLevel()));
         }
         // TODO: Maybe add a consistency check here (if super-activity is set we need outline-level > zero)
         if (activityVersion.getExpanded() != dataRow.getExpanded()) {
            update = true;
            activityVersion.setExpanded(dataRow.getExpanded());
         }
         if (activityVersion.getSuperActivityVersion() != superActivity) {
            update = true;
            activityVersion.setSuperActivityVersion(superActivity);
         }
         if ((activityVersion.getStart() != null && !activityVersion.getStart().equals(OpGanttValidator.getStart(dataRow)))
              || (activityVersion.getStart() == null)) {
            Date newStart;
            Date start = OpGanttValidator.getStart(dataRow);
            if (start == null) {
               if (superActivity != null && superActivity.getType() == OpActivity.SCHEDULED_TASK) {
                  newStart = superActivity.getStart();
               }
               else {
                  newStart = projectNode.getStart();
               }
            }
            else {
               newStart = start;
            }
            if (activityVersion.getStart() == null || !activityVersion.getStart().equals(newStart)) {
               update = true;
               activityVersion.setStart(newStart);
            }
         }
         if ((activityVersion.getFinish() != null && !activityVersion.getFinish().equals(OpGanttValidator.getEnd(dataRow)))
              || (activityVersion.getFinish() == null)) {

            Date end = OpGanttValidator.getEnd(dataRow);
            Date newEnd;
            if (end == null) {
               if (superActivity != null && superActivity.getType() == OpActivity.SCHEDULED_TASK) {
                  newEnd = superActivity.getFinish();
               }
               else {
                  newEnd = projectNode.getFinish();
               }
            }
            else {
               newEnd = end;
            }
            if (activityVersion.getFinish() == null || !activityVersion.getFinish().equals(newEnd)) {
               update = true;
               activityVersion.setFinish(newEnd);
            }
         }
         if (activityVersion.getDuration() != OpGanttValidator.getDuration(dataRow)) {
            update = true;
            activityVersion.setDuration(OpGanttValidator.getDuration(dataRow));
         }
         if (activityVersion.getBaseEffort() != OpGanttValidator.getBaseEffort(dataRow)) {
            update = true;
            activityVersion.setBaseEffort(OpGanttValidator.getBaseEffort(dataRow));
         }
         if (activityVersion.getType() != OpGanttValidator.getType(dataRow)) {
            update = true;
            activityVersion.setType((OpGanttValidator.getType(dataRow)));
         }

         //update the category
         String categoryLocator = null;
         if (activityVersion.getCategory() != null) {
            categoryLocator = activityVersion.getCategory().locator();
         }

         if (categoryChoice != null) {
            String newCategory = XValidator.choiceID(categoryChoice);
            if (!newCategory.equals(categoryLocator)) {
               update = true;
               category = (OpActivityCategory) broker.getObject(categoryChoice);
               activityVersion.setCategory(category);
            }
         }
         else {
            update = true;
            activityVersion.setCategory(null);
         }

         //update the responsible resource
         String resourceLocator = null;
         if (activityVersion.getResponsibleResource() != null) {
            resourceLocator = activityVersion.getResponsibleResource().locator();
         }
         
         if (responsibleResource != null) {
            String newResourceLocator = XValidator.choiceID(responsibleResource);
            if (!newResourceLocator.equals(resourceLocator)) {
               update = true;
               OpResource resource = (OpResource) broker.getObject(newResourceLocator);
               activityVersion.setResponsibleResource(resource);
            }
         }
         else {
            update = true;
            activityVersion.setResponsibleResource(null);
         }

         // Do not update complete from client-data: Calculated from work-slips (RemainingEffort)
         //update complete if progress tracking is off
         OpProjectPlan projectPlan = planVersion.getProjectPlan();
         boolean tracking = projectPlan.getProgressTracked();
         double complete = OpGanttValidator.getComplete(dataRow);
         if ((activityVersion.getComplete() != complete) && !tracking) {
            update = true;
            activityVersion.setComplete(complete);
         }

         if (activityVersion.getBasePersonnelCosts() != OpGanttValidator.getBasePersonnelCosts(dataRow)) {
            update = true;
            activityVersion.setBasePersonnelCosts(OpGanttValidator.getBasePersonnelCosts(dataRow));
         }
         if (activityVersion.getBaseTravelCosts() != OpGanttValidator.getBaseTravelCosts(dataRow)) {
            update = true;
            activityVersion.setBaseTravelCosts(OpGanttValidator.getBaseTravelCosts(dataRow));
         }
         if (activityVersion.getBaseMaterialCosts() != OpGanttValidator.getBaseMaterialCosts(dataRow)) {
            update = true;
            activityVersion.setBaseMaterialCosts(OpGanttValidator.getBaseMaterialCosts(dataRow));
         }
         if (activityVersion.getBaseExternalCosts() != OpGanttValidator.getBaseExternalCosts(dataRow)) {
            update = true;
            activityVersion.setBaseExternalCosts(OpGanttValidator.getBaseExternalCosts(dataRow));
         }
         if (activityVersion.getBaseMiscellaneousCosts() != OpGanttValidator.getBaseMiscellaneousCosts(dataRow)) {
            update = true;
            activityVersion.setBaseMiscellaneousCosts(OpGanttValidator.getBaseMiscellaneousCosts(dataRow));
         }
         if (activityVersion.getAttributes() != OpGanttValidator.getAttributes(dataRow)) {
            update = true;
            activityVersion.setAttributes(OpGanttValidator.getAttributes(dataRow));
         }
         byte validatorValue = (OpGanttValidator.getPriority(dataRow) == null) ? 0 : OpGanttValidator.getPriority(dataRow).byteValue();
         if ((activityVersion.getPriority() == 0 && validatorValue != 0)
              || (activityVersion.getPriority() != 0 && !(activityVersion.getPriority() == validatorValue))) {
            update = true;
            activityVersion.setPriority(validatorValue);
         }

         if (update) {
            broker.updateObject(activityVersion);
         }

      }

      return activityVersion;

   }

   private static ArrayList updateOrDeleteAssignmentVersions(OpBroker broker, XComponent dataSet, Iterator assignments) {
      ArrayList reusableAssignmentVersions = new ArrayList();
      OpAssignmentVersion assignment = null;
      XComponent dataRow = null;
      ArrayList resourceList = null;
      String resourceChoice = null;
      ArrayList resourceBaseEffortList = null;
      double baseEffort = 0.0d;
      double baseCosts = 0.0d;
      double assigned = 0;
      int i;
      boolean reusable;
      while (assignments.hasNext()) {
         assignment = (OpAssignmentVersion) assignments.next();
         dataRow = getDataRowForActivity(assignment.getActivityVersion(), dataSet);
         reusable = false;
         if (dataRow == null) {
            reusable = true;
         }
         else {
            resourceList = OpGanttValidator.getResources(dataRow);
            resourceBaseEffortList = OpGanttValidator.getResourceBaseEfforts(dataRow);
            // Check whether persistent assignment is present in resource list
            for (i = resourceList.size() - 1; i >= 0; i--) {
               resourceChoice = (String) resourceList.get(i);
               //ignore invisible resources
               String resourceChoiceId = XValidator.choiceID(resourceChoice);
               if (resourceChoiceId.equals(OpGanttValidator.NO_RESOURCE_ID)) {
                  continue;
               }
               if (OpLocator.parseLocator(resourceChoiceId).getID() == assignment.getResource()
                    .getID()) {
                  // Assignment is present: Remove from resource list and check whether update is required
                  resourceList.remove(i);
                  baseEffort = ((Double) resourceBaseEffortList.remove(i)).doubleValue();
                  baseCosts = baseEffort * assignment.getResource().getHourlyRate();
                  assigned = OpGanttValidator.percentageAssigned(resourceChoice);
                  if (assigned == OpGanttValidator.INVALID_ASSIGNMENT) {
                     assigned = assignment.getResource().getAvailable();
                  }
                  // TODO: The original XActivitySetFactory might miss the updating of assignment-complete (not sure)?
                  // (Maybe reevaluate complete on basis of base-effort and remaining-effort; note: Not for *version*?)
                  if ((assignment.getAssigned() != assigned) || (assignment.getBaseEffort() != baseEffort)
                       || (assignment.getBaseCosts() != baseCosts)) {
                     assignment.setAssigned(assigned);
                     assignment.setBaseEffort(baseEffort);
                     assignment.setBaseCosts(baseCosts);
                     broker.updateObject(assignment);
                  }
                  break;
               }
            }

            if (i == -1) {
               // Assignment does not exist anymore
               reusable = true;
            }

         }
         if (reusable) {
            reusableAssignmentVersions.add(assignment);
            //break links to activity
            OpActivityVersion activityVersion = assignment.getActivityVersion();
            activityVersion.getAssignmentVersions().remove(assignment);
            broker.updateObject(activityVersion);
         }
      }
      return reusableAssignmentVersions;
   }

   /**
    * Retrieves in a safe way a data row from the given data set that is associated with the given activity version
    *
    * @param activityVersion actvitiy entitiy
    * @param dataSet         data set to retrieve the data row from
    * @return data row for activity version
    */
   private static XComponent getDataRowForActivity(OpActivityVersion activityVersion, XComponent dataSet) {
      XComponent dataRow;
      int sequence = activityVersion.getSequence();
      long id = activityVersion.getID();
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

   private static void insertActivityAssignmentVersions(OpBroker broker, OpProjectPlanVersion planVersion,
        XComponent dataRow, OpActivityVersion activityVersion, ArrayList reusableAssignmentVersions, HashMap resources) {
      ArrayList resourceList = OpGanttValidator.getResources(dataRow);
      String resourceChoice = null;
      ArrayList resourceBaseEffortList = OpGanttValidator.getResourceBaseEfforts(dataRow);
      double baseEffort = 0.0d;
      OpAssignmentVersion assignment = null;
      OpResource resource = null;
      for (int i = 0; i < resourceBaseEffortList.size(); i++) {
         // Insert new assignment version
         resourceChoice = (String) resourceList.get(i);
         //ignore invisible resources
         String resourceChoiceId = XValidator.choiceID(resourceChoice);
         if (resourceChoiceId.equals(OpGanttValidator.NO_RESOURCE_ID)) {
            continue;
         }
         baseEffort = ((Double) resourceBaseEffortList.get(i)).doubleValue();
         if ((reusableAssignmentVersions != null) && (reusableAssignmentVersions.size() > 0)) {
            assignment = (OpAssignmentVersion) reusableAssignmentVersions.remove(reusableAssignmentVersions.size() - 1);
         }
         else {
            assignment = new OpAssignmentVersion();
         }
         assignment.setPlanVersion(planVersion);
         assignment.setActivityVersion(activityVersion);
         resource = (OpResource) resources.get(new Long(OpLocator.parseLocator(resourceChoiceId)
              .getID()));
         assignment.setResource(resource);
         // TODO: The original XActivitySetFactory might miss the updating of assignment-complete (not sure)?
         // (Maybe reevaluate complete on basis of base-effort and remaining-effort; note: Not for *version*?)
         double assigned = OpGanttValidator.percentageAssigned(resourceChoice);
         if (assigned == OpGanttValidator.INVALID_ASSIGNMENT) {
            assigned = resource.getAvailable();
         }
         assignment.setAssigned(assigned);
         assignment.setBaseEffort(baseEffort);
         assignment.setBaseCosts(baseEffort * resource.getHourlyRate());
         if (assignment.getID() == 0) {
            broker.makePersistent(assignment);
         }
         else {
            broker.updateObject(assignment);
         }
      }
   }

   private static ArrayList updateOrDeleteWorkPeriodVersions(OpBroker broker, XComponent dataSet, Iterator workPeriodsIt) {
      OpWorkPeriodVersion workPeriod;
      XComponent dataRow;
      double baseEffort;
      ArrayList reusableWorkPeriodVersions = new ArrayList();
      boolean update;
      boolean reusable;
      while (workPeriodsIt.hasNext()) {
         reusable = false;
         update = false;
         workPeriod = (OpWorkPeriodVersion) workPeriodsIt.next();
         dataRow = getDataRowForActivity(workPeriod.getActivityVersion(), dataSet);
         if (dataRow == null) {
            reusable = true;
         }
         else {
            Date periodStart = workPeriod.getStart();
            Map workPeriods = OpActivityDataSetFactory.getWorkPeriods(dataRow);
            List activitiPeriodValues = (List) workPeriods.get(periodStart);
            if (activitiPeriodValues != null) {
               long workingDays = ((Long) activitiPeriodValues.get(0)).longValue();
               baseEffort = ((Double) activitiPeriodValues.get(1)).doubleValue();
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
            }
            else {
               reusable = true;
            }
         }
         // Work phase does not exist anymore: Can be reused
         if (reusable) {
            reusableWorkPeriodVersions.add(workPeriod);
            //break activity-workperiod link
            OpActivityVersion activityVersion = workPeriod.getActivityVersion();
            activityVersion.getWorkPeriodVersions().remove(workPeriod);
            broker.updateObject(activityVersion);
         }
      }
      return reusableWorkPeriodVersions;
   }

   private static void insertActivityWorkPeriodVersions(OpBroker broker, OpProjectPlanVersion planVersion,
        XComponent dataRow, OpActivityVersion activityVersion, ArrayList reusableWorkPeriodVersions) {

      Map workPeriods = OpActivityDataSetFactory.getWorkPeriods(dataRow);
      OpWorkPeriodVersion workPeriod = null;
      for (Iterator iterator = workPeriods.entrySet().iterator(); iterator.hasNext();) {
         Map.Entry workPeriodEntry = (Map.Entry) iterator.next();
         Date periodStart = (Date) workPeriodEntry.getKey();
         //check if activity does not already have it
         boolean periodSaved = false;
         if (activityVersion.getWorkPeriodVersions() != null) {
            for (Iterator iterator1 = activityVersion.getWorkPeriodVersions().iterator(); iterator1.hasNext();) {
               OpWorkPeriodVersion opWorkPeriod = (OpWorkPeriodVersion) iterator1.next();
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
         double baseEffortPerDay = ((Double) workPeriodValues.get(1)).doubleValue();
         if ((reusableWorkPeriodVersions != null) && (reusableWorkPeriodVersions.size() > 0)) {
            workPeriod = (OpWorkPeriodVersion) reusableWorkPeriodVersions.remove(reusableWorkPeriodVersions.size() - 1);
         }
         else {
            workPeriod = new OpWorkPeriodVersion();
         }
         workPeriod.setPlanVersion(planVersion);
         workPeriod.setActivityVersion(activityVersion);
         workPeriod.setStart(periodStart);
         workPeriod.setWorkingDays(workingDays);
         workPeriod.setBaseEffort(baseEffortPerDay);
         if (workPeriod.getID() == 0) {
            broker.makePersistent(workPeriod);
         }
         else {
            broker.updateObject(workPeriod);
         }
      }
   }

   private static ArrayList updateOrDeleteAttachmentVersions(OpBroker broker, XComponent dataSet, Iterator attachments) {
      OpAttachmentVersion attachment = null;
      XComponent dataRow = null;
      int i = 0;
      ArrayList attachmentList = null;
      ArrayList attachmentElement = null;
      long attachmentId = 0;
      ArrayList reusableAttachmentVersions = new ArrayList();
      boolean reusable;
      while (attachments.hasNext()) {
         reusable = false;
         attachment = (OpAttachmentVersion) attachments.next();
         dataRow = getDataRowForActivity(attachment.getActivityVersion(), dataSet);
         if (dataRow == null) {
            reusable = true;
         }
         else {
            attachmentList = OpGanttValidator.getAttachments(dataRow);
            for (i = attachmentList.size() - 1; i >= 0; i--) {
               // Note: We assume that attachments can only be added and removed on the client (no expicit updates)
               attachmentElement = (ArrayList) attachmentList.get(i);
               String choiceId = XValidator.choiceID((String) attachmentElement.get(1));
               OpLocator attLocator = OpLocator.parseLocator(choiceId);
               if (attLocator == null) {
                  //newly added
                  continue;
               }
               attachmentId = attLocator.getID();
               if (attachment.getID() == attachmentId) {
                  attachmentList.remove(i);
                  break;
               }
            }
            if (i == -1) {
               reusable = true;
            }
         }
         // Attachment was deleted on client: Decrease ref-count of content objects (and delete if it is null)
         if (reusable) {
            OpContent content = attachment.getContent();
            OpContentManager.updateContent(content, broker, false);
            reusableAttachmentVersions.add(attachment);
            //break link from activity to attachment
            OpActivityVersion activityVersion = attachment.getActivityVersion();
            activityVersion.getAttachmentVersions().remove(attachment);
         }
      }
      return reusableAttachmentVersions;
   }

   private static void insertActivityAttachmentVersions(OpBroker broker, OpProjectPlanVersion planVersion,
        XComponent dataRow, OpActivityVersion activityVersion, ArrayList reusableAttachmentVersions) {
      ArrayList attachmentList = OpGanttValidator.getAttachments(dataRow);
      ArrayList attachmentElement = null;
      OpAttachmentVersion attachment = null;
      for (int i = 0; i < attachmentList.size(); i++) {
         // Insert new attachment version
         attachmentElement = (ArrayList) attachmentList.get(i);
         if ((reusableAttachmentVersions != null) && (reusableAttachmentVersions.size() > 0)) {
            attachment = (OpAttachmentVersion) reusableAttachmentVersions.remove(reusableAttachmentVersions.size() - 1);
         }
         else {
            attachment = new OpAttachmentVersion();
         }
         attachment.setPlanVersion(planVersion);
         attachment.setActivityVersion(activityVersion);
         attachment.setLinked(OpActivityDataSetFactory.LINKED_ATTACHMENT_DESCRIPTOR.equals(attachmentElement.get(0)));
         attachment.setName((String) attachmentElement.get(2));
         attachment.setLocation((String) attachmentElement.get(3));
         OpPermissionSetFactory.copyPermissions(broker, planVersion.getProjectPlan().getProjectNode(), attachment);
         OpContent content = null;
         if (!attachment.getLinked()) {

            String contentId = (String) attachmentElement.get(4);
            if (contentId.equals(OpActivityDataSetFactory.NO_CONTENT_ID)) {
               // Insert new content object and set ref-count to one (1)
               byte[] bytes = (byte[]) attachmentElement.get(5);
               content = OpContentManager.newContent(bytes, null);
               broker.makePersistent(content);
               attachment.setContent(content);
            }
            else {
               content = (OpContent) broker.getObject(contentId);
               OpContentManager.updateContent(content, broker, true);
               attachment.setContent(content);
            }
         }

         if (attachment.getID() == 0) {
            broker.makePersistent(attachment);
         }
         else {
            broker.updateObject(attachment);
         }

         if (content != null) {
            content.getAttachmentVersions().add(attachment);
            broker.updateObject(content);
         }
      }
   }

   private static ArrayList updateOrDeleteDependencyVersions(OpBroker broker, XComponent dataSet, Iterator dependencies) {
      OpDependencyVersion dependency = null;
      XComponent predecessorDataRow = null;
      XComponent successorDataRow = null;
      ArrayList predecessorIndexes = null;
      ArrayList successorIndexes = null;
      ArrayList reusableDependencyVersions = new ArrayList();
      boolean reusable;
      while (dependencies.hasNext()) {
         reusable = false;
         dependency = (OpDependencyVersion) dependencies.next();
         successorDataRow = getDataRowForActivity(dependency.getSuccessorVersion(), dataSet);
         if (successorDataRow == null) {
            reusable = true;
         }
         else {
            predecessorIndexes = OpGanttValidator.getPredecessors(successorDataRow);
            if (predecessorIndexes.remove(new Integer(dependency.getPredecessorVersion().getSequence()))) {
               // Dependency still exists: Remove also other part of bi-directional association
               predecessorDataRow = (XComponent) dataSet.getChild(dependency.getPredecessorVersion().getSequence());
               successorIndexes = OpGanttValidator.getSuccessors(predecessorDataRow);
               successorIndexes.remove(new Integer(dependency.getSuccessorVersion().getSequence()));
            }
            else {
               reusable = true;
            }
         }
         if (reusable) {
            reusableDependencyVersions.add(dependency);
            //break link activiyt->dependency
            OpActivityVersion successorVersion = dependency.getSuccessorVersion();
            successorVersion.getPredecessorVersions().remove(dependency);
            OpActivityVersion predecessorVersion = dependency.getPredecessorVersion();
            predecessorVersion.getSuccessorVersions().remove(dependency);
            broker.updateObject(successorVersion);
            broker.updateObject(predecessorVersion);
         }
      }
      return reusableDependencyVersions;
   }

   private static void insertActivityDependencyVersions(OpBroker broker, OpProjectPlanVersion planVersion,
        XComponent dataSet, XComponent dataRow, OpActivityVersion activityVersion, ArrayList activityVersionList,
        ArrayList reusableDependencyVersions) {
      // Note: We only check for new predecessor indexes
      // (Successors are just the other side of the bi-directional association)
      ArrayList predecessorIndexes = OpGanttValidator.getPredecessors(dataRow);
      OpDependencyVersion dependency = null;
      XComponent predecessorDataRow = null;
      OpActivityVersion predecessor = null;
      for (int i = 0; i < predecessorIndexes.size(); i++) {
         // Insert new dependency version
         if ((reusableDependencyVersions != null) && (reusableDependencyVersions.size() > 0)) {
            dependency = (OpDependencyVersion) reusableDependencyVersions.remove(reusableDependencyVersions.size() - 1);
         }
         else {
            dependency = new OpDependencyVersion();
         }
         dependency.setPlanVersion(planVersion);
         predecessorDataRow = (XComponent) dataSet.getChild(((Integer) predecessorIndexes.get(i)).intValue());
         predecessor = (OpActivityVersion) activityVersionList.get(predecessorDataRow.getIndex());
         dependency.setPredecessorVersion(predecessor);
         dependency.setSuccessorVersion(activityVersion);
         if (dependency.getID() == 0) {
            broker.makePersistent(dependency);
         }
         else {
            broker.updateObject(dependency);
         }
      }
   }

   public static OpProjectPlanVersion newProjectPlanVersion(OpBroker broker, OpProjectPlan projectPlan, OpUser creator,
        int versionNumber, boolean copyActivities) {

      // Create plan version w/version numnber and simply "copy" activity rows to activity-version rows
      OpProjectPlanVersion planVersion = new OpProjectPlanVersion();
      planVersion.setCreator(creator);
      planVersion.setProjectPlan(projectPlan);
      planVersion.setVersionNumber(versionNumber);
      planVersion.setStart(projectPlan.getStart());
      planVersion.setFinish(projectPlan.getFinish());
      planVersion.setTemplate(projectPlan.getTemplate());
      broker.makePersistent(planVersion);

      // Copy activities (check for null activities is necessary for newly created project plans)
      if (copyActivities && (projectPlan.getActivities() != null)) {

         System.err.println("NEW-PROJECT-PLAN-VERSION: Copy activities");

         HashMap activityVersionMap = new HashMap();
         Iterator activities = projectPlan.getActivities().iterator();
         OpActivity activity = null;
         OpActivityVersion activityVersion = null;
         while (activities.hasNext()) {
            activity = (OpActivity) activities.next();
            if (activity.getType() == OpActivity.ADHOC_TASK) {
               continue;
            }
            activityVersion = new OpActivityVersion();
            activityVersion.setPlanVersion(planVersion);
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
            activityVersion.setBasePersonnelCosts(activity.getBasePersonnelCosts());
            activityVersion.setBaseMaterialCosts(activity.getBaseMaterialCosts());
            activityVersion.setBaseExternalCosts(activity.getBaseExternalCosts());
            activityVersion.setBaseMiscellaneousCosts(activity.getBaseMiscellaneousCosts());
            broker.makePersistent(activityVersion);
            // Add new activity version to activity version map
            activityVersionMap.put(new Long(activity.getID()), activityVersion);
         }

         // Copy attachments and increment ref-count of reused content objects
         Iterator attachments = projectPlan.getActivityAttachments().iterator();
         OpAttachment attachment = null;
         OpAttachmentVersion attachmentVersion = null;
         OpContent content = null;
         while (attachments.hasNext()) {
            attachment = (OpAttachment) attachments.next();
            if (attachment.getActivity().getType() == OpActivity.ADHOC_TASK) {
               continue;
            }
            attachmentVersion = new OpAttachmentVersion();
            attachmentVersion.setPlanVersion(planVersion);
            Long activityId = new Long(attachment.getActivity().getID());
            attachmentVersion.setActivityVersion((OpActivityVersion) activityVersionMap.get(activityId));
            attachmentVersion.setName(attachment.getName());
            attachmentVersion.setLinked(attachment.getLinked());
            attachmentVersion.setLocation(attachment.getLocation());
            if (attachment.getContent() != null) {
               // Increase ref-count of reused content object
               content = attachment.getContent();
               OpContentManager.updateContent(content, broker, true);
               attachmentVersion.setContent(content);
            }
            broker.makePersistent(attachmentVersion);
         }

         // Copy assignments
         HashMap assignmentVersionMap = new HashMap();
         Iterator assignments = projectPlan.getActivityAssignments().iterator();
         OpAssignment assignment = null;
         OpAssignmentVersion assignmentVersion = null;
         while (assignments.hasNext()) {
            assignment = (OpAssignment) assignments.next();
            if (assignment.getActivity().getType() == OpActivity.ADHOC_TASK) {
               continue;
            }
            assignmentVersion = new OpAssignmentVersion();
            assignmentVersion.setPlanVersion(planVersion);
            Long activityId = new Long(assignment.getActivity().getID());
            assignmentVersion.setActivityVersion((OpActivityVersion) activityVersionMap.get(activityId));
            assignmentVersion.setResource(assignment.getResource());
            assignmentVersion.setAssigned(assignment.getAssigned());
            assignmentVersion.setComplete(assignment.getComplete());
            assignmentVersion.setBaseEffort(assignment.getBaseEffort());
            assignmentVersion.setBaseCosts(assignment.getBaseCosts());
            broker.makePersistent(assignmentVersion);
            // Add new assignment version to assignment version map
            assignmentVersionMap.put(new Long(assignment.getID()), assignmentVersion);
         }

         // Copy work phases
         Iterator workPeriods = projectPlan.getWorkPeriods().iterator();
         OpWorkPeriod workPeriod = null;
         OpWorkPeriodVersion workPeriodVersion = null;
         while (workPeriods.hasNext()) {
            workPeriod = (OpWorkPeriod) workPeriods.next();
            if (workPeriod.getActivity().getType() == OpActivity.ADHOC_TASK) {
               continue;
            }
            workPeriodVersion = new OpWorkPeriodVersion();
            workPeriodVersion.setPlanVersion(planVersion);
            Long activityId = new Long(workPeriod.getActivity().getID());
            workPeriodVersion.setActivityVersion((OpActivityVersion) activityVersionMap.get(activityId));
            workPeriodVersion.setStart(workPeriod.getStart());
            workPeriodVersion.setWorkingDays(workPeriod.getWorkingDays());
            workPeriodVersion.setBaseEffort(workPeriod.getBaseEffort());
            broker.makePersistent(workPeriodVersion);
         }

         // Copy dependencies
         Iterator dependencies = projectPlan.getDependencies().iterator();
         OpDependency dependency = null;
         OpDependencyVersion dependencyVersion = null;
         while (dependencies.hasNext()) {
            dependency = (OpDependency) dependencies.next();
            dependencyVersion = new OpDependencyVersion();
            dependencyVersion.setPlanVersion(planVersion);
            dependencyVersion.setPredecessorVersion((OpActivityVersion) activityVersionMap.get(new Long(dependency
                 .getPredecessorActivity().getID())));
            dependencyVersion.setSuccessorVersion((OpActivityVersion) activityVersionMap.get(new Long(dependency
                 .getSuccessorActivity().getID())));
            broker.makePersistent(dependencyVersion);
         }

      }

      return planVersion;

   }

   public static OpProjectPlanVersion findProjectPlanVersion(OpBroker broker, OpProjectPlan projectPlan, int versionNumber) {
      // Find project plan version by version number (returns null if no such version exists)
      OpQuery query = broker
           .newQuery("select planVersion from OpProjectPlanVersion as planVersion where planVersion.ProjectPlan.ID = ? and planVersion.VersionNumber = ?");
      query.setLong(0, projectPlan.getID());
      query.setInteger(1, versionNumber);
      Iterator result = broker.iterate(query);
      if (!result.hasNext()) {
         return null;
      }
      return (OpProjectPlanVersion) result.next();
   }

   public static void deleteProjectPlanVersion(OpBroker broker, OpProjectPlanVersion planVersion) {

      // Decrease ref-count of reused content objects (attachments) and delete project plan (uses cascading)
      // Get all attachments, decrease ref-counts and delete content is ref-count is zero
      Iterator attachments = planVersion.getAttachmentVersions().iterator();
      OpAttachmentVersion attachment = null;
      OpContent content = null;
      while (attachments.hasNext()) {
         attachment = (OpAttachmentVersion) attachments.next();
         if (attachment.getContent() != null) {
            // Decrease ref-count of reused content object; delete if ref-count is zero
            content = attachment.getContent();
            OpContentManager.updateContent(content, broker, false);
         }
      }

      // Finally, delete project plan version object
      broker.deleteObject(planVersion);

   }
}
