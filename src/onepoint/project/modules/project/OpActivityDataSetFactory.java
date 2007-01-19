/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.work.OpProgressCalculator;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public abstract class OpActivityDataSetFactory {

   public final static String LINKED_ATTACHMENT_DESCRIPTOR = "u";
   public final static String DOCUMENT_ATTACHMENT_DESCRIPTOR = "d";
   public final static String NO_CONTENT_ID = "0";

   public static HashMap resourceMap(OpBroker broker, OpProjectNode projectNode) {
      OpQuery query = broker
           .newQuery("select assignment.Resource from OpProjectNodeAssignment as assignment where assignment.ProjectNode.ID = ? order by assignment.Resource.Name asc");
      query.setLong(0, projectNode.getID());
      Iterator resources = broker.list(query).iterator();
      //LinkedHashMap to maintain the order in which entries are added
      HashMap resourceMap = new LinkedHashMap();
      OpResource resource = null;
      while (resources.hasNext()) {
         resource = (OpResource) (resources.next());
         resourceMap.put(new Long(resource.getID()), resource);
      }
      return resourceMap;
   }

   public static void retrieveResourceDataSet(HashMap resourceMap, XComponent dataSet) {
      Iterator resources = resourceMap.values().iterator();
      OpResource resource = null;
      XComponent dataRow = null;
      XComponent dataCell = null;
      while (resources.hasNext()) {
         resource = (OpResource) (resources.next());
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice(resource.locator(), resource.getName()));
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(resource.getAvailable());
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(resource.getHourlyRate());
         dataRow.addChild(dataCell);
         dataSet.addChild(dataRow);
      }
   }

   public static void retrieveResourceDataSet(OpBroker broker, OpProjectNode projectNode, XComponent dataSet) {
      retrieveResourceDataSet(resourceMap(broker, projectNode), dataSet);
   }

   public static void retrieveActivityDataSet(OpBroker broker, OpProjectPlan projectPlan, XComponent dataSet,
        boolean editable) {

      // Activities: Fill data set with activity data rows and create activity data row map
      OpQuery query = broker
           .newQuery("select activity from OpActivity as activity where activity.ProjectPlan.ID = ? and activity.Deleted = false and activity.Type != ? order by activity.Sequence");
      query.setLong(0, projectPlan.getID());
      query.setByte(1, OpActivity.ADHOC_TASK);
      Iterator activities = broker.iterate(query);
      OpActivity activity;
      XComponent dataRow;
      while (activities.hasNext()) {
         activity = (OpActivity) activities.next();
         dataRow = new XComponent(XComponent.DATA_ROW);
         retrieveActivityDataRow(activity, dataRow, editable);
         if (activity.getType() == OpActivity.TASK || activity.getType() == OpActivity.COLLECTION_TASK) {
            OpGanttValidator.setStart(dataRow, null);
            OpGanttValidator.setEnd(dataRow, null);
         }
         dataSet.addChild(dataRow);
      }

      // Assignments: Fill resources and resource base efforts columns
      Iterator assignments = projectPlan.getActivityAssignments().iterator();
      OpAssignment assignment = null;
      OpResource resource = null;
      Map resourceAvailability = new HashMap();
      //map of [activitySequence, sum(activity.assignments.baseEffort)]
      Map activityAssignmentsSum = new HashMap();
      while (assignments.hasNext()) {
         assignment = (OpAssignment) assignments.next();

         activity = assignment.getActivity();
         if (activity.getType() != OpActivity.ADHOC_TASK) {
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

            dataRow = (XComponent) dataSet.getChild(assignment.getActivity().getSequence());
            resource = assignment.getResource();
            String caption = resource.getName();
            String assignedString = String.valueOf(assignment.getAssigned());
            caption += " " + assignedString + "%";
            resourceAvailability.put(resource.locator(), new Double(resource.getAvailable()));
            OpGanttValidator.addResource(dataRow, XValidator.choice(resource.locator(), caption));
            OpGanttValidator.addResourceBaseEffort(dataRow, assignment.getBaseEffort());
         }
      }

      //update the resources to take into account invisible resources (independent planning only)
      if (projectPlan.getCalculationMode() == OpGanttValidator.INDEPENDENT) {
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
      Iterator dependencies = projectPlan.getDependencies().iterator();
      OpDependency dependency = null;
      XComponent predecessorDataRow = null;
      XComponent successorDataRow = null;
      while (dependencies.hasNext()) {
         dependency = (OpDependency) dependencies.next();
         predecessorDataRow = (XComponent) dataSet.getChild(dependency.getPredecessorActivity().getSequence());
         successorDataRow = (XComponent) dataSet.getChild(dependency.getSuccessorActivity().getSequence());
         OpGanttValidator.addPredecessor(successorDataRow, predecessorDataRow.getIndex());
         OpGanttValidator.addSuccessor(predecessorDataRow, successorDataRow.getIndex());
      }

      // WorkPhases: Fill work phase starts, finishes and base effort columns
      Iterator workPeriods = projectPlan.getWorkPeriods().iterator();
      OpWorkPeriod workPeriod = null;
      while (workPeriods.hasNext()) {
         workPeriod = (OpWorkPeriod) workPeriods.next();
         dataRow = (XComponent) dataSet.getChild(workPeriod.getActivity().getSequence());
         List workPeriodValues = new ArrayList();
         workPeriodValues.add(new Long(workPeriod.getWorkingDays()));
         workPeriodValues.add(new Double(workPeriod.getBaseEffort()));
         addWorkPhases(dataRow, workPeriod.getStart(), workPeriodValues);
      }
      // Note: Activity comments are not part of the client-side data-set
   }

   /**
    * Transforms from work periods into work phases on a given activity. Stores the resulting work phases on the given
    * data row using the mechanism from the validator.
    *
    * @param dataRow              row to process
    * @param workPeriodStart      start of the work period
    * @param workPeriodListValues the work period values (working days number and base effort)
    */
   public static void addWorkPhases(XComponent dataRow, Date workPeriodStart, List workPeriodListValues) {

      long workingDays = ((Long) workPeriodListValues.get(0)).longValue();
      double periodBaseEffort = ((Double) workPeriodListValues.get(1)).doubleValue();
      Date activityFinish = OpGanttValidator.getEnd(dataRow);
      Calendar calendar = XCalendar.getDefaultCalendar().getCalendar();
      calendar.setTime(workPeriodStart);
      boolean workDay = false;
      int i = 0;
      double baseEffort = 0;
      boolean started = false;
      while (calendar.getTime().getTime() <= activityFinish.getTime() && i < OpWorkPeriod.PERIOD_LENGTH) {
         long mask = 1L << i;
         if ((workingDays & mask) == mask) {
            //working day
            if (!workDay) {
               //if !working day before, start a new work phase
               OpGanttValidator.addWorkPhaseStart(dataRow, new Date(calendar.getTime().getTime()));
               started = true;
            }
            workDay = true;
            baseEffort += periodBaseEffort;
         }
         else {
            //not a working day
            if (workDay) {
               //if working day before, add end work phase (end will be the day before)
               OpGanttValidator.addWorkPhaseFinish(dataRow, new Date(calendar.getTime().getTime() - XCalendar.MILLIS_PER_DAY));
               OpGanttValidator.addWorkPhaseBaseEffort(dataRow, baseEffort);
               started = false;
               baseEffort = 0;
            }
            workDay = false;
         }
         calendar.add(Calendar.DAY_OF_MONTH, 1);
         i++;
      }
      if (started) {
         //add also the last end date
         OpGanttValidator.addWorkPhaseFinish(dataRow, new Date(calendar.getTime().getTime() - XCalendar.MILLIS_PER_DAY));
         OpGanttValidator.addWorkPhaseBaseEffort(dataRow, baseEffort);
      }
   }

   public static void retrieveFilteredActivityDataSet(OpBroker broker, OpActivityFilter filter, OpObjectOrderCriteria order,
        XComponent dataSet) {

      // Note: The filtered activity data set contains an additional column containing the project locator

      // Attention: We have to use an activity-row map, because direct-access using sequence not possible
      // (Potential filtered and reordered activity set across multiple projects)

      ArrayList projectPlanIds = new ArrayList();
      if (filter.getProjectNodeIds().size() > 0) {
         // Pre-fetch project plans and project names (performance: Adding project column)
         OpQuery query = broker.newQuery("select projectPlan, projectNode from OpProjectPlan as projectPlan inner join projectPlan.ProjectNode as projectNode where projectNode.ID in (:projectNodeIds)");
         query.setCollection("projectNodeIds", filter.getProjectNodeIds());
         Iterator result = broker.list(query).iterator();
         Object[] record = null;
         while (result.hasNext()) {
            record = (Object[]) result.next();
            projectPlanIds.add(new Long(((OpProjectPlan) record[0]).getID()));
         }
      }

      // Construct query string and arguments depending on filter and sort order
      StringBuffer fromBuffer = new StringBuffer("OpActivity as activity");
      StringBuffer whereBuffer = new StringBuffer();
      ArrayList argumentNames = new ArrayList();
      ArrayList argumentValues = new ArrayList();

      if (projectPlanIds.size() > 0) {
         whereBuffer.append("activity.ProjectPlan.ID in (:projectPlanIds)");
         argumentNames.add("projectPlanIds");
         argumentValues.add(projectPlanIds);
      }

      if (filter.getResourceIds().size() > 0) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and ");
         }
         fromBuffer.append(" inner join activity.Assignments as assignment");
         whereBuffer.append("assignment.Resource.ID in (:resourceIds)");

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

      if (filter.getTypes().size() > 0) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and ");
         }
         whereBuffer.append("activity.Type in (:types)");
         argumentNames.add("types");
         argumentValues.add(filter.getTypes());
      }

      if (filter.getStartFrom() != null) {
         if (whereBuffer.length() > 0) {
            whereBuffer.append(" and (");
         }
         whereBuffer.append("activity.Start >= :startFrom");
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
      whereBuffer.append("activity.Deleted = false and activity.Template = :template");
      argumentNames.add("template");
      argumentValues.add(Boolean.valueOf(filter.getTemplates()));

      StringBuffer queryBuffer = new StringBuffer("select  activity from ");
      queryBuffer.append(fromBuffer);
      queryBuffer.append(" where ");
      queryBuffer.append(whereBuffer);

      if (order != null) {
         queryBuffer.append(order.toHibernateQueryString("activity"));
      }

      System.err.println("QUERY " + queryBuffer.toString());
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

      ArrayList activityIds = new ArrayList();
      Iterator activities = broker.iterate(query);
      OpActivity activity = null;
      XComponent dataRow = null;
      HashMap activityRowMap = new HashMap();
      StringBuffer nameBuffer = null;
      OpActivity topLevelActivity = null;
      XComponent dataCell = null;
      OpProjectNode projectNode = null;
      while (activities.hasNext()) {
         activity = (OpActivity) activities.next();
         Long activityId = new Long(activity.getID());
         if (activityIds.contains(activityId)) {
            continue;
         }
         activityIds.add(activityId);
         dataRow = new XComponent(XComponent.DATA_ROW);
         retrieveActivityDataRow(activity, dataRow, false);
         // "Flatten" activity hierarchy
         if (dataRow.getOutlineLevel() > 0) {
            // Patch activity name by adding context information
            String activityName = activity.getName();
            nameBuffer = (activityName != null) ? new StringBuffer(activityName) : new StringBuffer();
            nameBuffer.append(" (");
            if (dataRow.getOutlineLevel() == 2) {
               activityName = activity.getSuperActivity().getSuperActivity().getName();
               nameBuffer.append((activityName == null) ? "" : activityName);
               nameBuffer.append(": ");
            }
            else if (dataRow.getOutlineLevel() > 2) {
               topLevelActivity = activity.getSuperActivity();
               while (topLevelActivity.getSuperActivity() != null) {
                  topLevelActivity = topLevelActivity.getSuperActivity();
               }
               activityName = topLevelActivity.getName();
               nameBuffer.append((activityName == null) ? "" : activityName);
               nameBuffer.append("... ");
            }
            activityName = activity.getSuperActivity().getName();
            nameBuffer.append((activityName == null) ? "" : activityName);
            nameBuffer.append(')');
            OpGanttValidator.setName(dataRow, nameBuffer.toString());
         }
         dataRow.setOutlineLevel(0);
         // Add project column
         dataCell = new XComponent(XComponent.DATA_CELL);
         projectNode = activity.getProjectPlan().getProjectNode();
         dataCell.setStringValue(XValidator.choice(projectNode.locator(), projectNode.getName()));
         dataRow.addChild(dataCell);
         dataSet.addChild(dataRow);
         activityRowMap.put(new Long(activity.getID()), dataRow);
      }
      /* no activities found*/
      if (activityIds.isEmpty()) {
         return;
      }
      // Assignments: Fill resources and resource base efforts columns
      if (filter.getResourceIds().size() > 0) {
         query = broker.newQuery("select assignment from OpAssignment as assignment where assignment.Resource.ID in (:resourceIds) and assignment.Activity.ID in (:activityIds)");
         query.setCollection("resourceIds", filter.getResourceIds());
      }
      else {
         query = broker.newQuery("select assignment from OpAssignment as assignment where assignment.Activity.ID in (:activityIds)");
      }
      query.setCollection("activityIds", activityIds);
      Iterator assignments = broker.iterate(query);
      OpAssignment assignment = null;
      OpResource resource = null;
      Map resourceAvailability = new HashMap();
      while (assignments.hasNext()) {
         assignment = (OpAssignment) assignments.next();
         dataRow = (XComponent) activityRowMap.get(new Long(assignment.getActivity().getID()));
         resource = assignment.getResource();
         resourceAvailability.put(resource.locator(), new Double(resource.getAvailable()));
         String caption = resource.getName();
         if (resource.getAvailable() != assignment.getAssigned()) {
            String assignedString = String.valueOf(assignment.getAssigned());
            caption += " " + assignedString + "%";
         }
         OpGanttValidator.addResource(dataRow, XValidator.choice(resource.locator(), caption));
         OpGanttValidator.addResourceBaseEffort(dataRow, assignment.getBaseEffort());
      }

      //set also the visual resources (uses the value of the dataset as a value holder)
      Boolean showHours = (Boolean) dataSet.getValue();
      if (showHours == null) {
         showHours = Boolean.valueOf(OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS));
      }
      Iterator it = activityRowMap.values().iterator();
      while (it.hasNext()) {
         XComponent activityRow = (XComponent) it.next();
         OpGanttValidator.updateVisualResources(activityRow, showHours.booleanValue(), resourceAvailability);
      }

      if (filter.getDependencies()) {
         // Dependencies: Fill predecessor and successor columns
         query = broker
              .newQuery("select dependency from OpDependency as dependency where dependency.PredecessorActivity.ID in (:activityIds) and dependency.SuccessorActivity.ID in (:activityIds)");
         query.setCollection("activityIds", activityIds);
         Iterator dependencies = broker.iterate(query);
         OpDependency dependency = null;
         XComponent predecessorDataRow = null;
         XComponent successorDataRow = null;
         while (dependencies.hasNext()) {
            dependency = (OpDependency) dependencies.next();
            predecessorDataRow = (XComponent) activityRowMap.get(new Long(dependency.getPredecessorActivity().getID()));
            successorDataRow = (XComponent) activityRowMap.get(new Long(dependency.getSuccessorActivity().getID()));
            OpGanttValidator.addPredecessor(successorDataRow, predecessorDataRow.getIndex());
            OpGanttValidator.addSuccessor(predecessorDataRow, successorDataRow.getIndex());
         }
      }

      if (filter.getWorkPhases()) {
         // WorkPhases: Fill work phase starts, finishes and base effort columns
         query = broker.newQuery("select workPeriod from OpWorkPeriod as workPeriod where workPeriod.Activity.ID in (:activityIds)");
         query.setCollection("activityIds", activityIds);
         Iterator workPeriods = broker.iterate(query);
         OpWorkPeriod workPeriod = null;
         while (workPeriods.hasNext()) {
            workPeriod = (OpWorkPeriod) workPeriods.next();
            dataRow = (XComponent) activityRowMap.get(new Long(workPeriod.getActivity().getID()));
            List workPeriodValues = new ArrayList();
            workPeriodValues.add(new Long(workPeriod.getWorkingDays()));
            workPeriodValues.add(new Double(workPeriod.getBaseEffort()));
            addWorkPhases(dataRow, workPeriod.getStart(), workPeriodValues);
         }
      }

      // Note: Activity comments are not part of the client-side data-set

   }

   private static void retrieveAttachments(Set attachments, XComponent dataRow) {
      // TODO: Bulk-fetch like other parts of the project plan
      ArrayList attachmentList = OpGanttValidator.getAttachments(dataRow);
      Iterator i = attachments.iterator();
      OpAttachment attachment = null;
      ArrayList attachmentElement = null;
      while (i.hasNext()) {
         attachment = (OpAttachment) i.next();
         attachmentElement = new ArrayList();
         if (attachment.getLinked()) {
            attachmentElement.add(LINKED_ATTACHMENT_DESCRIPTOR);
         }
         else {
            attachmentElement.add(DOCUMENT_ATTACHMENT_DESCRIPTOR);
         }
         attachmentElement.add(attachment.locator());
         attachmentElement.add(attachment.getName());
         attachmentElement.add(attachment.getLocation());
         if (!attachment.getLinked()) {
            String contentId = OpLocator.locatorString(attachment.getContent());
            attachmentElement.add(contentId);
         }
         else {
            attachmentElement.add(NO_CONTENT_ID);
         }
         attachmentList.add(attachmentElement);
      }
   }

   private static void retrieveActivityDataRow(OpActivity activity, XComponent dataRow, boolean editable) {

      dataRow.setStringValue(activity.locator());

      boolean isCollection = (activity.getType() == OpActivity.COLLECTION || activity.getType() == OpActivity.COLLECTION_TASK);
      boolean isStandard = (activity.getType() == OpActivity.STANDARD);
      boolean isStrictlyTask = (activity.getType() == OpActivity.TASK);
      boolean isTask = isStrictlyTask || (activity.getType() == OpActivity.COLLECTION_TASK);
      boolean isScheduledTask = (activity.getType() == OpActivity.SCHEDULED_TASK);

      dataRow.setOutlineLevel(activity.getOutlineLevel());
      dataRow.setExpanded(activity.getExpanded());

      // Name (0)
      XComponent dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable);
      dataCell.setStringValue(activity.getName());
      dataRow.addChild(dataCell);

      // Type (1)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection);
      dataCell.setByteValue(activity.getType());
      dataRow.addChild(dataCell);

      // Category (2)
      // TODO: For this to work we also need to set color-formatters for activity-table
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection);
      OpActivityCategory category = activity.getCategory();
      if (category != null) {
         dataCell.setStringValue(XValidator.choice(category.locator(), category.getName()));
      }
      dataRow.addChild(dataCell);

      // Complete (3);
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(activity.getComplete());
      dataRow.addChild(dataCell);
      // editable if progress tracking is off
      boolean tracking = activity.getProjectPlan().getProgressTracked();
      dataCell.setEnabled(editable & !tracking && !isCollection);

      // Start (4)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection);
      dataCell.setDateValue(activity.getStart());
      dataRow.addChild(dataCell);

      // Finish (5)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection);
      dataCell.setDateValue(activity.getFinish());
      dataRow.addChild(dataCell);

      // Duration (6)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection && !isTask);
      dataCell.setDoubleValue(activity.getDuration());
      dataRow.addChild(dataCell);

      // BaseEffort (7)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection && !isScheduledTask);
      dataCell.setDoubleValue(activity.getBaseEffort());
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
      dataCell.setDoubleValue(activity.getBasePersonnelCosts());
      dataRow.addChild(dataCell);

      // BaseTravelCosts (12); only editable for standard activities
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && (isStandard || isStrictlyTask));
      dataCell.setDoubleValue(activity.getBaseTravelCosts());
      dataRow.addChild(dataCell);

      // BaseMaterialCosts (13); only editable for standard activities
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && (isStandard || isStrictlyTask));
      dataCell.setDoubleValue(activity.getBaseMaterialCosts());
      dataRow.addChild(dataCell);

      // BaseExternalCosts (14); only editable for standard activities
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && (isStandard || isStrictlyTask));
      dataCell.setDoubleValue(activity.getBaseExternalCosts());
      dataRow.addChild(dataCell);

      // BaseMiscellaneousCosts (15); only editable for standard activities
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && (isStandard || isStrictlyTask));
      dataCell.setDoubleValue(activity.getBaseMiscellaneousCosts());
      dataRow.addChild(dataCell);

      // Description (16); not editable in table
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setStringValue(activity.getDescription());
      dataRow.addChild(dataCell);

      // Attachments (17); not editable in table
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);
      if (activity.getAttachments().size() > 0) {
         retrieveAttachments(activity.getAttachments(), dataRow);
      }

      // Attributes (18); not editable
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setIntValue(activity.getAttributes());
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
      Byte value = (activity.getPriority() == 0) ? null : new Byte(activity.getPriority());
      dataCell.setValue(value);
      dataRow.addChild(dataCell);

      //Workrecords (24): a map of [resourceLocator, hasWorkRecords]
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(false);
      Set assignments = activity.getAssignments();
      Map data = new HashMap(assignments.size());
      Iterator it = assignments.iterator();
      while (it.hasNext()) {
         OpAssignment assignment = (OpAssignment) it.next();
         String resourceLocator = assignment.getResource().locator();
         Boolean hasWorkRecords = (assignment.getWorkRecords() != null) ? Boolean.valueOf(assignment.getWorkRecords().size() > 0) : Boolean.FALSE;
         data.put(resourceLocator, hasWorkRecords);
      }
      dataCell.setValue(data);
      dataRow.addChild(dataCell);

      //Actual effort (25) - needed for %complete
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(false);
      dataCell.setDoubleValue(activity.getActualEffort());
      dataRow.addChild(dataCell);

      //Visual resources (26) - needed for %complete
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && !isCollection && !isScheduledTask);
      dataCell.setListValue(new ArrayList());
      dataRow.addChild(dataCell);

      OpGanttValidator.updateAttachmentAttribute(dataRow);
   }

   public static HashMap activities(OpProjectPlan plan) {
      HashMap activities = new HashMap();
      // Check if this is a new project plan
      if (plan.getActivities() != null) {
         Iterator i = plan.getActivities().iterator();
         OpActivity activity = null;
         while (i.hasNext()) {
            activity = (OpActivity) i.next();
            if (!activity.getDeleted() && activity.getType() != OpActivity.ADHOC_TASK) { // the activity is not marked as deleted
               activities.put(new Long(activity.getID()), activity);
            }
         }
      }
      return activities;
   }

   private static void mapActivityVersionIDs(OpBroker broker, XComponent dataSet, OpProjectPlanVersion workingPlanVersion) {
      // Exchange all activity version IDs contained in data-row values with their respective actual activity IDs

      HashMap activityVersionIdMap = new HashMap();
      OpQuery query = broker
           .newQuery("select activityVersion.ID, activityVersion.Activity.ID from OpActivityVersion as activityVersion where activityVersion.PlanVersion.ID = ?");
      query.setLong(0, workingPlanVersion.getID());
      Iterator result = broker.list(query).iterator();
      Object[] record = null;
      while (result.hasNext()) {
         record = (Object[]) result.next();
         System.err.println("***MAP-ACT_V " + record[0] + " -> " + record[1]);
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

   public static void storeActivityDataSet(OpBroker broker, XComponent dataSet, HashMap resources, OpProjectPlan plan,
        OpProjectPlanVersion workingPlanVersion) {

      // TODO: Maybe use "pure" IDs (Long values) for data-row values instead of locator strings (retrieve)

      // If a working plan version is specified exchange activity IDs in the data-set with activity version IDs
      if (workingPlanVersion != null) {
         mapActivityVersionIDs(broker, dataSet, workingPlanVersion);
      }

      System.err.println("*** WORKING-P-V " + workingPlanVersion);

      // Prefetch activities
      List adhocTasks = getAdHocTasks(plan);

      plan.getActivities().removeAll(adhocTasks);

      HashMap activities = activities(plan);
      Date planStart = plan.getStart();
      Date planFinish = plan.getFinish();

      // Phase 1: Iterate data-rows and store activity versions
      XComponent dataRow;
      OpActivity activity;
      OpActivity previousActivity = null;
      int previousOutlineLevel = 0;
      OpActivity superActivity = null;
      Stack superActivityStack = new Stack();
      ArrayList activityList = new ArrayList();
      int i;
      for (i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         if (dataRow.getOutlineLevel() > previousOutlineLevel) {
            if (superActivity != null) {
               superActivityStack.push(superActivity);
            }
            superActivity = previousActivity;
         }
         else if (dataRow.getOutlineLevel() < previousOutlineLevel) {
            for (int k = 0; k < previousOutlineLevel - dataRow.getOutlineLevel(); k++) {
               if (superActivityStack.empty()) {
                  superActivity = null;
                  break;
               }
               else {
                  superActivity = (OpActivity) (superActivityStack.pop());
               }
            }
         }
         // Remove activity version from activity versions map
         if (dataRow.getStringValue() != null) {
            activity = (OpActivity) activities.remove(new Long(OpLocator.parseLocator(dataRow.getStringValue()).getID()));
         }
         else {
            activity = null;
         }
         activity = insertOrUpdateActivity(broker, dataRow, activity, plan, superActivity);

         // Set locator string value for newly created activity version data rows
         if (dataRow.getStringValue() == null) {
            dataRow.setStringValue(activity.locator());
         }

         // Check project plan start and finish dates
         if (activity.getType() != OpActivity.TASK && activity.getType() != OpActivity.COLLECTION_TASK) {
            if (activity.getStart().getTime() < planStart.getTime()) {
               planStart = activity.getStart();
            }
            System.err.println("AF " + activity.getFinish() + " PF " + planFinish);
            if (activity.getFinish().getTime() < planFinish.getTime()) {
               planFinish = activity.getFinish();
            }
         }

         // Activity version list can be used to efficiently look-up activities by data-row index
         activityList.add(activity);
         previousActivity = activity;
         previousOutlineLevel = previousActivity.getOutlineLevel();
      }

      // Phase 2: Iterate database contents; update and delete existing related activity data
      // (We need to check for existence of sets in case this is a new project plan)
      ArrayList reusableAssignments = null;
      if (plan.getActivityAssignments() != null) {
         reusableAssignments = updateOrDeleteAssignments(broker, dataSet, plan.getActivityAssignments().iterator());
      }
      ArrayList reusableWorkPeriods = null;
      if (plan.getWorkPeriods() != null) {
         reusableWorkPeriods = updateOrDeleteWorkPeriods(broker, dataSet, plan.getWorkPeriods().iterator());
      }
      ArrayList reusableAttachments = null;
      if (plan.getActivityAttachments() != null) {
         reusableAttachments = updateOrDeleteAttachments(broker, dataSet, plan.getActivityAttachments().iterator());
      }
      ArrayList reusableDependencys = null;
      if (plan.getDependencies() != null) {
         reusableDependencys = updateOrDeleteDependencies(broker, dataSet, plan.getDependencies().iterator());
      }

      // Phase 3: Iterate data-rows (second time); insert new related activity data
      for (i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);
         activity = (OpActivity) activityList.get(i);
         // Only standard (work) activities may have assignments and work phases
         //task can also have assignments
         //milestones as well
         if (OpGanttValidator.getType(dataRow) == OpGanttValidator.STANDARD ||
              OpGanttValidator.getType(dataRow) == OpGanttValidator.TASK ||
              OpGanttValidator.getType(dataRow) == OpGanttValidator.MILESTONE) {
            insertActivityAssignments(broker, plan, dataRow, activity, reusableAssignments, resources);
         }
         if (OpGanttValidator.getType(dataRow) == OpGanttValidator.STANDARD) {
            insertActivityWorkPeriods(broker, plan, dataRow, activity, reusableWorkPeriods);
         }
         insertActivityAttachments(broker, plan, dataRow, activity, reusableAttachments);
         insertActivityDependencies(broker, plan, dataSet, dataRow, activity, activityList, reusableDependencys);
      }

      // Phase 4: Delete unused related objects
      if (reusableAssignments != null) {
         for (i = 0; i < reusableAssignments.size(); i++) {
            broker.deleteObject((OpAssignment) reusableAssignments.get(i));
         }
      }
      if (reusableWorkPeriods != null) {
         for (i = 0; i < reusableWorkPeriods.size(); i++) {
            broker.deleteObject((OpWorkPeriod) reusableWorkPeriods.get(i));
         }
      }
      // TOOD: Attention with attachments -- we have to decrement ref-count?
      if (reusableAttachments != null) {
         for (i = 0; i < reusableAttachments.size(); i++) {
            broker.deleteObject((OpAttachment) reusableAttachments.get(i));
         }
      }
      if (reusableDependencys != null) {
         for (i = 0; i < reusableDependencys.size(); i++) {
            broker.deleteObject((OpDependency) reusableDependencys.get(i));
         }
      }

      // Phase 5: Delete unused activity versions
      Iterator unusedActivitys = activities.values().iterator();
      while (unusedActivitys.hasNext()) {
         OpActivity markedActivity = (OpActivity) unusedActivitys.next();
         markedActivity.setDeleted(true);
      }

      // Finally, update project plan version start and finish fields
      plan.setStart(planStart);
      plan.setFinish(planFinish);

      //add the adhoc tasks at the end of the project plan
      Set activitySet = plan.getActivities();
      int maxSeq = 0;
      for (Iterator iterator = activitySet.iterator(); iterator.hasNext();) {
         activity = (OpActivity) iterator.next();
         if (activity.getType() != OpActivity.ADHOC_TASK && activity.getSequence() > maxSeq) {
            maxSeq = activity.getSequence();
         }
      }
      int sequence = maxSeq + 1;
      for (Iterator iterator = adhocTasks.iterator(); iterator.hasNext();) {
         activity = (OpActivity) iterator.next();
         activity.setSequence(sequence);
         activity.setProjectPlan(plan);
         broker.updateObject(activity);
         sequence++;
      }

      broker.updateObject(plan);

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

   private static OpActivity insertOrUpdateActivity(OpBroker broker, XComponent dataRow, OpActivity activity,
        OpProjectPlan projectPlan, OpActivity superActivity) {

      String categoryLocator = null;
      OpActivityCategory category = null;

      OpProjectNode projectNode = projectPlan.getProjectNode();
      if (activity == null) {
         // Insert a new activity
         activity = new OpActivity();
         activity.setProjectPlan(projectPlan);
         activity.setTemplate(projectPlan.getTemplate());

         activity.setName(OpGanttValidator.getName(dataRow));
         activity.setDescription(OpGanttValidator.getDescription(dataRow));
         activity.setSequence(dataRow.getIndex());
         activity.setOutlineLevel((byte) (dataRow.getOutlineLevel()));
         activity.setExpanded(dataRow.getExpanded());
         activity.setSuperActivity(superActivity);
         if (superActivity != null && superActivity.getType() == OpActivity.SCHEDULED_TASK) {
            activity.setStart(superActivity.getStart());
            activity.setFinish(superActivity.getFinish());
         }
         else {
            if (OpGanttValidator.getStart(dataRow) == null) {
               activity.setStart(projectNode.getStart());
            }
            else {
               activity.setStart(OpGanttValidator.getStart(dataRow));
            }
            if (OpGanttValidator.getEnd(dataRow) == null) {
               activity.setFinish(projectNode.getFinish());
            }
            else {
               activity.setFinish(OpGanttValidator.getEnd(dataRow));
            }
         }
         activity.setDuration(OpGanttValidator.getDuration(dataRow));
         activity.setBaseEffort(OpGanttValidator.getBaseEffort(dataRow));
         activity.setType((OpGanttValidator.getType(dataRow)));
         if (OpGanttValidator.getCategory(dataRow) != null) {
            category = (OpActivityCategory) broker.getObject(OpGanttValidator.getCategory(dataRow));
            activity.setCategory(category);
         }
         activity.setBasePersonnelCosts(OpGanttValidator.getBasePersonnelCosts(dataRow));
         activity.setBaseTravelCosts(OpGanttValidator.getBaseTravelCosts(dataRow));
         activity.setBaseMaterialCosts(OpGanttValidator.getBaseMaterialCosts(dataRow));
         activity.setBaseExternalCosts(OpGanttValidator.getBaseExternalCosts(dataRow));
         activity.setBaseMiscellaneousCosts(OpGanttValidator.getBaseMiscellaneousCosts(dataRow));
         activity.setAttributes(OpGanttValidator.getAttributes(dataRow));
         byte priority = OpGanttValidator.getPriority(dataRow) != null ? OpGanttValidator.getPriority(dataRow).byteValue() : 0;
         activity.setPriority(priority);

         activity.setActualEffort(0);

         double complete = OpGanttValidator.getComplete(dataRow);
         activity.setComplete(complete);
         double remaining = OpGanttValidator.calculateRemainingEffort(activity.getBaseEffort(), activity.getActualEffort(),
              complete);
         activity.setRemainingEffort(remaining);
         //actual costs are 0 initially
         activity.setActualPersonnelCosts(0);
         activity.setActualTravelCosts(0);
         activity.setActualMaterialCosts(0);
         activity.setActualExternalCosts(0);
         activity.setActualMiscellaneousCosts(0);

         broker.makePersistent(activity);

      }
      else {

         boolean update = false;

         if (!checkEquality(activity.getName(), OpGanttValidator.getName(dataRow))) {
            update = true;
            activity.setName(OpGanttValidator.getName(dataRow));
         }
         if (!checkEquality(activity.getDescription(), OpGanttValidator.getDescription(dataRow))) {
            update = true;
            activity.setDescription(OpGanttValidator.getDescription(dataRow));
         }
         if (activity.getSequence() != dataRow.getIndex()) {
            update = true;
            activity.setSequence(dataRow.getIndex());
         }
         if (activity.getOutlineLevel() != dataRow.getOutlineLevel()) {
            update = true;
            activity.setOutlineLevel((byte) (dataRow.getOutlineLevel()));
         }
         // TODO: Maybe add a consistency check here (if super-activity is set we need outline-level > zero)
         if (activity.getExpanded() != dataRow.getExpanded()) {
            update = true;
            activity.setExpanded(dataRow.getExpanded());
         }
         if (activity.getSuperActivity() != superActivity) {
            update = true;
            //update actual effort and costs
            updateParentsActualEffort(activity, superActivity, broker);
            activity.setSuperActivity(superActivity);
         }
         if ((activity.getStart() != null && !activity.getStart().equals(OpGanttValidator.getStart(dataRow)))
              || (activity.getStart() == null)) {
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
            if (activity.getStart() == null || !activity.getStart().equals(newStart)) {
               update = true;
               activity.setStart(newStart);
            }
         }
         if ((activity.getFinish() != null && !activity.getFinish().equals(OpGanttValidator.getEnd(dataRow)))
              || (activity.getFinish() == null)) {

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

            if (activity.getFinish() == null || !activity.getFinish().equals(newEnd)) {
               update = true;
               activity.setFinish(newEnd);
            }
         }
         if (activity.getDuration() != OpGanttValidator.getDuration(dataRow)) {
            update = true;
            activity.setDuration(OpGanttValidator.getDuration(dataRow));
         }
         double complete = OpGanttValidator.getComplete(dataRow);
         //if tracking is off, calculate the remaining
         if ((activity.getComplete() != complete)) {
            update = true;
            activity.setComplete(complete);
            double remainingEffort = OpGanttValidator.calculateRemainingEffort(activity.getBaseEffort(), activity.getActualEffort(), complete);
            activity.setRemainingEffort(remainingEffort);
         }

         double baseEffort = OpGanttValidator.getBaseEffort(dataRow);
         if (activity.getBaseEffort() != baseEffort) {
            update = true;
            activity.setBaseEffort(baseEffort);
            double remainingEffort = OpGanttValidator.calculateRemainingEffort(baseEffort, activity.getActualEffort(), activity.getComplete());
            activity.setRemainingEffort(remainingEffort);
         }

         if (activity.getType() != OpGanttValidator.getType(dataRow)) {
            update = true;
            activity.setType((OpGanttValidator.getType(dataRow)));
         }

         //update the category
         categoryLocator = null;
         if (activity.getCategory() != null) {
            categoryLocator = activity.getCategory().locator();
         }

         String newCategory = OpGanttValidator.getCategory(dataRow);
         if (newCategory != null && categoryLocator != newCategory) {
            update = true;
            category = (OpActivityCategory) broker.getObject(newCategory);
            activity.setCategory(category);
         }
         else if (newCategory == null) {
            update = true;
            activity.setCategory(null);
         }

         // Do not update complete from client-data: Calculated from work-slips (RemainingEffort)
         if (activity.getBasePersonnelCosts() != OpGanttValidator.getBasePersonnelCosts(dataRow)) {
            update = true;
            activity.setBasePersonnelCosts(OpGanttValidator.getBasePersonnelCosts(dataRow));
         }
         if (activity.getBaseTravelCosts() != OpGanttValidator.getBaseTravelCosts(dataRow)) {
            update = true;
            activity.setBaseTravelCosts(OpGanttValidator.getBaseTravelCosts(dataRow));
         }
         if (activity.getBaseMaterialCosts() != OpGanttValidator.getBaseMaterialCosts(dataRow)) {
            update = true;
            activity.setBaseMaterialCosts(OpGanttValidator.getBaseMaterialCosts(dataRow));
         }
         if (activity.getBaseExternalCosts() != OpGanttValidator.getBaseExternalCosts(dataRow)) {
            update = true;
            activity.setBaseExternalCosts(OpGanttValidator.getBaseExternalCosts(dataRow));
         }
         if (activity.getBaseMiscellaneousCosts() != OpGanttValidator.getBaseMiscellaneousCosts(dataRow)) {
            update = true;
            activity.setBaseMiscellaneousCosts(OpGanttValidator.getBaseMiscellaneousCosts(dataRow));
         }
         if (activity.getAttributes() != OpGanttValidator.getAttributes(dataRow)) {
            update = true;
            activity.setAttributes(OpGanttValidator.getAttributes(dataRow));
         }
         byte validatorPriority = OpGanttValidator.getPriority(dataRow) == null ? 0 : OpGanttValidator.getPriority(dataRow).byteValue();
         if ((activity.getPriority() == 0 && validatorPriority != 0)
              || !(activity.getPriority() == validatorPriority)) {
            update = true;
            activity.setPriority(validatorPriority);
         }

         if (update) {
            broker.updateObject(activity);
         }

      }

      return activity;

   }


   /**
    * Updates all the fields which depend on the actual effort of an activity, for the new(or old) super activity of
    * that activity.
    *
    * @param activity  a <code>OpActivity</code> which has a new parent.
    * @param newParent a <codE>OpActivity</code> representing the new parent. A value of <code>null</code> means
    *                  that the activity has no current parent.
    * @param broker    a <code>OpBroker</code> used for performing business operations.
    */
   private static void updateParentsActualEffort(OpActivity activity, OpActivity newParent, OpBroker broker) {
      OpActivity originalActivity = activity;
      //update old parents
      while (activity.getSuperActivity() != null) {
         OpActivity oldParent = activity.getSuperActivity();
         oldParent.setActualEffort(oldParent.getActualEffort() - activity.getActualEffort());
         oldParent.setActualExternalCosts(oldParent.getActualExternalCosts() - activity.getActualExternalCosts());
         oldParent.setActualMaterialCosts(oldParent.getActualMaterialCosts() - activity.getActualMaterialCosts());
         oldParent.setActualMiscellaneousCosts(oldParent.getActualMiscellaneousCosts() - activity.getActualMiscellaneousCosts());
         oldParent.setActualPersonnelCosts(oldParent.getActualPersonnelCosts() - activity.getActualPersonnelCosts());
         oldParent.setActualTravelCosts(oldParent.getActualTravelCosts() - activity.getActualTravelCosts());
         broker.updateObject(oldParent);

         activity = oldParent;
      }
      //update the new parents
      activity = originalActivity;
      while (newParent != null) {
         newParent.setActualEffort(newParent.getActualEffort() + activity.getActualEffort());
         newParent.setActualExternalCosts(newParent.getActualExternalCosts() + activity.getActualExternalCosts());
         newParent.setActualMaterialCosts(newParent.getActualMaterialCosts() + activity.getActualMaterialCosts());
         newParent.setActualMiscellaneousCosts(newParent.getActualMiscellaneousCosts() + activity.getActualMiscellaneousCosts());
         newParent.setActualPersonnelCosts(newParent.getActualPersonnelCosts() + activity.getActualPersonnelCosts());
         newParent.setActualTravelCosts(newParent.getActualTravelCosts() + activity.getActualTravelCosts());
         broker.updateObject(newParent);

         activity = newParent;
         newParent = newParent.getSuperActivity();
      }
   }

   private static ArrayList updateOrDeleteAssignments(OpBroker broker, XComponent dataSet, Iterator assignments) {
      OpAssignment assignment = null;
      XComponent dataRow = null;
      ArrayList resourceList = null;
      String resourceChoice = null;
      ArrayList resourceBaseEffortList = null;
      double baseEffort = 0.0d;
      double baseCosts = 0.0d;
      double assigned = 0;
      int i = 0;
      int maxActivitySequence = dataSet.getChildCount();
      int activitySequence = 0;

      ArrayList reusableAssignments = new ArrayList();
      while (assignments.hasNext()) {
         assignment = (OpAssignment) assignments.next();
         activitySequence = assignment.getActivity().getSequence();
         boolean reusable = false;
         if (activitySequence < maxActivitySequence) { // activity was not deleted on the client
            dataRow = (XComponent) dataSet.getChild(activitySequence);
            resourceList = OpGanttValidator.getResources(dataRow);
            resourceBaseEffortList = OpGanttValidator.getResourceBaseEfforts(dataRow);
            // Check whether persistent assignment is present in resource list
            for (i = resourceList.size() - 1; i >= 0; i--) {
               resourceChoice = (String) resourceList.get(i);
               String resourceChoiceId = XValidator.choiceID(resourceChoice);
               //ignore invible resources
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

                  OpActivity activity = assignment.getActivity();
                  boolean tracking = activity.getProjectPlan().getProgressTracked();
                  boolean update = false;

                  // update assignment if tracking is off
                  double complete = activity.getComplete();
                  if (complete != assignment.getComplete() && !tracking) {
                     assignment.setComplete(complete);
                     double remaining = OpGanttValidator.calculateRemainingEffort(assignment.getBaseEffort(), assignment.getActualEffort(), complete);
                     assignment.setRemainingEffort(remaining);
                     update = true;
                  }

                  if (assignment.getAssigned() != assigned) {
                     assignment.setAssigned(assigned);
                     update = true;
                  }

                  if (assignment.getBaseEffort() != baseEffort) {
                     assignment.setBaseEffort(baseEffort);
                     double remaining = OpGanttValidator.calculateRemainingEffort(baseEffort, assignment.getActualEffort(), assignment.getComplete());
                     assignment.setRemainingEffort(remaining);
                     update = true;
                  }

                  if (assignment.getBaseCosts() != baseCosts) {
                     assignment.setBaseCosts(baseCosts);
                     update = true;
                  }

                  if (update) {
                     broker.updateObject(assignment);
                  }
                  break;
               }
            }
            // Assignment does not exist anymore
            if (i == -1) {
               reusable = true;
            }
         }
         else {
            reusable = true;
         }
         if (reusable) {
            if (!(assignment.getActivity() != null && assignment.getActivity().getType() == OpActivity.ADHOC_TASK)) {
               reusableAssignments.add(assignment);
               //break links to activity
               OpActivity activity = assignment.getActivity();
               activity.getAssignments().remove(assignment);
               broker.updateObject(activity);
            }
         }
      }
      return reusableAssignments;
   }

   private static void insertActivityAssignments(OpBroker broker, OpProjectPlan plan, XComponent dataRow,
        OpActivity activity, ArrayList reusableAssignments, HashMap resources) {
      ArrayList resourceList = OpGanttValidator.getResources(dataRow);
      String resourceChoice = null;
      ArrayList resourceBaseEffortList = OpGanttValidator.getResourceBaseEfforts(dataRow);
      double baseEffort = 0.0d;
      OpAssignment assignment = null;
      OpResource resource = null;
      for (int i = 0; i < resourceList.size(); i++) {
         // Insert new assignment version
         resourceChoice = (String) resourceList.get(i);

         //don't persist invisible resources
         String resourceChoiceId = XValidator.choiceID(resourceChoice);
         if (resourceChoiceId.equals(OpGanttValidator.NO_RESOURCE_ID)) {
            continue;
         }

         if (i < resourceBaseEffortList.size()) {
            baseEffort = ((Double) resourceBaseEffortList.get(i)).doubleValue();
         }
         else {
            baseEffort = 0;
         }
         if ((reusableAssignments != null) && (reusableAssignments.size() > 0)) {
            assignment = (OpAssignment) reusableAssignments.remove(reusableAssignments.size() - 1);
         }
         else {
            assignment = new OpAssignment();
         }
         assignment.setProjectPlan(plan);
         assignment.setActivity(activity);
         resource = (OpResource) resources.get(new Long(OpLocator.parseLocator(resourceChoiceId).getID()));
         assignment.setResource(resource);
         double assigned = OpGanttValidator.percentageAssigned(resourceChoice);
         if (assigned == OpGanttValidator.INVALID_ASSIGNMENT) {
            assigned = resource.getAvailable();
         }
         assignment.setAssigned(assigned);
         assignment.setBaseEffort(baseEffort);
         assignment.setBaseCosts(baseEffort * resource.getHourlyRate());
         assignment.setActualEffort(0);
         assignment.setRemainingEffort(baseEffort);
         boolean tracking = activity.getProjectPlan().getProgressTracked();
         //update assignment if tracking is off
         if (!tracking) {
            double complete = activity.getComplete();
            assignment.setComplete(complete);
            OpProgressCalculator.updateAssignmentBasedOnTracking(assignment, false, tracking);
         }

         if (assignment.getID() == 0) {
            broker.makePersistent(assignment);
         }
         else {
            broker.updateObject(assignment);
         }
      }
   }

   private static ArrayList updateOrDeleteWorkPeriods(OpBroker broker, XComponent dataSet, Iterator workPeriodsIt) {
      OpWorkPeriod workPeriod = null;
      XComponent dataRow = null;
      int i = 0;
      double baseEffort = 0.0d;
      ArrayList reusableWorkPeriods = new ArrayList();
      boolean update;
      int maxActivitySequence = dataSet.getChildCount();
      int activitySequence = 0;

      while (workPeriodsIt.hasNext()) {
         workPeriod = (OpWorkPeriod) workPeriodsIt.next();
         activitySequence = workPeriod.getActivity().getSequence();
         Date periodStart = workPeriod.getStart();
         update = false;
         boolean reusable = false;
         if (activitySequence < maxActivitySequence) { // activity was not deleted on client
            dataRow = (XComponent) dataSet.getChild(activitySequence);
            Map workPeriods = getWorkPeriods(dataRow);
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
         else {
            reusable = true;
         }
         if (reusable) {
            reusableWorkPeriods.add(workPeriod);
            //break activity-workPeriods link
            OpActivity activity = workPeriod.getActivity();
            activity.getWorkPeriods().remove(workPeriod);
            broker.updateObject(activity);
         }
      }
      return reusableWorkPeriods;
   }

   private static void insertActivityWorkPeriods(OpBroker broker, OpProjectPlan plan, XComponent dataRow,
        OpActivity activity, ArrayList reusableWorkPeriods) {

      Map workPeriods = getWorkPeriods(dataRow);
      OpWorkPeriod workPeriod = null;
      for (Iterator iterator = workPeriods.entrySet().iterator(); iterator.hasNext();) {
         Map.Entry workPeriodEntry = (Map.Entry) iterator.next();
         Date periodStart = (Date) workPeriodEntry.getKey();
         //check if activity does not already have it
         boolean periodSaved = false;
         if (activity.getWorkPeriods() != null) {
            for (Iterator iterator1 = activity.getWorkPeriods().iterator(); iterator1.hasNext();) {
               OpWorkPeriod opWorkPeriod = (OpWorkPeriod) iterator1.next();
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
         if ((reusableWorkPeriods != null) && (reusableWorkPeriods.size() > 0)) {
            workPeriod = (OpWorkPeriod) reusableWorkPeriods.remove(reusableWorkPeriods.size() - 1);
         }
         else {
            workPeriod = new OpWorkPeriod();
         }
         workPeriod.setProjectPlan(plan);
         workPeriod.setActivity(activity);
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

   /**
    * Transforms from work phases on a given activity into work periods.
    *
    * @param dataRow row to get the work periods for.
    * @return <code>Map</code>0 representing the work period list, where key=period start, value=<code>List</code>
    *         containing working days as <code>Long</code> and base effort per day as <code>Double</code>
    */
   public static Map getWorkPeriods(XComponent dataRow) {
      Map workPeriods = new TreeMap();

      List rowStarts = OpGanttValidator.getWorkPhaseStarts(dataRow);
      List rowEnds = OpGanttValidator.getWorkPhaseFinishes(dataRow);
      if (rowStarts.size() == 0) {
         return workPeriods; //no work phase -> no work periods
      }
      //use a tree map in order to sort work phases from validator by start date.
      TreeMap workPhases = new TreeMap();
      for (int i = 0; i < rowStarts.size(); i++) {
         Date start = (Date) rowStarts.get(i);
         Date end = (Date) rowEnds.get(i);
         workPhases.put(start, end);
      }
      List workPhaseStartsSorted = new ArrayList();
      List workPhaseFinishesSorted = new ArrayList();
      for (Iterator iterator = workPhases.entrySet().iterator(); iterator.hasNext();) {
         Map.Entry entry = (Map.Entry) iterator.next();
         workPhaseStartsSorted.add(entry.getKey());
         workPhaseFinishesSorted.add(entry.getValue());
      }

      XCalendar defaultCalendar = XCalendar.getDefaultCalendar();
      Calendar calendar = defaultCalendar.getCalendar();
      double days = OpGanttValidator.getDuration(dataRow) / defaultCalendar.getWorkHoursPerDay();
      double baseEffortPerDay = OpGanttValidator.getBaseEffort(dataRow) / days;
      Date firstStart = (Date) workPhaseStartsSorted.get(0);
      Date lastEnd = (Date) workPhaseFinishesSorted.get(workPhaseFinishesSorted.size() - 1);
      calendar.setTime(firstStart);
      int index = 0;
      int i = 0;
      long workDay = 0;
      long workingDays = 0L;

      Date periodStart = getPeriodStartForDate(calendar.getTime());
      while (calendar.getTime().getTime() <= lastEnd.getTime()) {
         Date currentStart = (Date) workPhaseStartsSorted.get(index);
         Date currentFinish = (Date) workPhaseFinishesSorted.get(index);

         //begin the continuous work stage
         if (calendar.getTime().equals(currentStart)) {
            workDay = 1;
         }

         //first day of the period
         if (i == 0) {
            periodStart = getPeriodStartForDate(calendar.getTime());
            //adjust the index if current day > period start (this can happen for the fist day of the activity)
            i = (int) ((calendar.getTime().getTime() - periodStart.getTime()) / XCalendar.MILLIS_PER_DAY);
         }

         //set state of day in working days
         workingDays |= (workDay << i);

         //end of continuos working days
         if (calendar.getTime().equals(currentFinish)) {
            index++;
            workDay = 0;
         }

         i++;
         //if i == periodLength, new work period and save current one
         if (i == OpWorkPeriod.PERIOD_LENGTH) {
            //add to workPhases
            List workPeriodValues = new ArrayList();
            workPeriodValues.add(new Long(workingDays));
            workPeriodValues.add(new Double(baseEffortPerDay));
            workPeriods.put(periodStart, workPeriodValues);
            i = 0;
            workingDays = 0L;
         }
         calendar.add(Calendar.DAY_OF_MONTH, 1);
      }
      //add also the last working period
      if (workingDays != 0) {
         List workPeriodValues = new ArrayList();
         workPeriodValues.add(new Long(workingDays));
         workPeriodValues.add(new Double(baseEffortPerDay));
         workPeriods.put(periodStart, workPeriodValues);
      }
      return workPeriods;
   }

   private static Date getPeriodStartForDate(java.util.Date date) {
      long seconds = date.getTime();
      long periodStartTime = seconds - (seconds % (OpWorkPeriod.PERIOD_LENGTH * XCalendar.MILLIS_PER_DAY));
      return new Date(periodStartTime);
   }


   private static ArrayList updateOrDeleteAttachments(OpBroker broker, XComponent dataSet, Iterator attachments) {
      OpAttachment attachment = null;
      XComponent dataRow = null;
      int i = 0;
      ArrayList attachmentList = null;
      ArrayList attachmentElement = null;
      long attachmentId = 0;
      ArrayList reusableAttachments = new ArrayList();

      int maxActivitySequence = dataSet.getChildCount();
      int activitySequence = 0;

      while (attachments.hasNext()) {
         attachment = (OpAttachment) attachments.next();
         activitySequence = attachment.getActivity().getSequence();
         boolean reusable = false;
         if (activitySequence < maxActivitySequence) { // activity was not deleted on client
            dataRow = (XComponent) dataSet.getChild(attachment.getActivity().getSequence());
            attachmentList = OpGanttValidator.getAttachments(dataRow);
            for (i = attachmentList.size() - 1; i >= 0; i--) {
               // Note: We assume that attachments can only be added and removed on the client (no expicit updates)
               attachmentElement = (ArrayList) attachmentList.get(i);
               OpLocator locator = OpLocator.parseLocator(XValidator.choiceID((String) attachmentElement.get(1)));
               if (locator == null) { // new attachment added on client
                  continue;
               }
               attachmentId = locator.getID();
               if (attachment.getID() == attachmentId) {
                  // attachment found in project plan (remove it to avoid double insert)
                  attachmentList.remove(i);
                  break;
               }
            }
            // Attachment was deleted on client: Decrease ref-count of content objects (and delete if it is null)
            if (i == -1) {
               reusable = true;
            }
         }
         else {
            reusable = true;
         }
         if (reusable) {
            OpContent content = attachment.getContent();
            OpContentManager.updateContent(content, broker, false);
            reusableAttachments.add(attachment);
            //break link from activity to attachment
            OpActivity activity = attachment.getActivity();
            activity.getAttachments().remove(attachment);
         }
      }
      return reusableAttachments;
   }

   private static void insertActivityAttachments(OpBroker broker, OpProjectPlan plan, XComponent dataRow,
        OpActivity activity, ArrayList reusableAttachments) {
      ArrayList attachmentList = OpGanttValidator.getAttachments(dataRow);
      ArrayList attachmentElement = null;
      OpAttachment attachment = null;
      for (int i = 0; i < attachmentList.size(); i++) {
         // Insert new attachment version
         attachmentElement = (ArrayList) attachmentList.get(i);
         createAttachment(broker, activity, plan, attachmentElement, reusableAttachments);
      }
   }

   public static void createAttachment(OpBroker broker, OpActivity activity, OpProjectPlan plan, List attachmentElement, ArrayList reusableAttachments) {
      OpAttachment attachment;
      if ((reusableAttachments != null) && (reusableAttachments.size() > 0)) {
         attachment = (OpAttachment) reusableAttachments.remove(reusableAttachments.size() - 1);
      }
      else {
         attachment = new OpAttachment();
      }
      attachment.setProjectPlan(plan);
      attachment.setActivity(activity);
      attachment.setLinked(LINKED_ATTACHMENT_DESCRIPTOR.equals(attachmentElement.get(0)));
      attachment.setName((String) attachmentElement.get(2));
      attachment.setLocation((String) attachmentElement.get(3));
      OpPermissionSetFactory.copyPermissions(broker, plan.getProjectNode(), attachment);
      if (!attachment.getLinked()) {
         String contentId = (String) attachmentElement.get(4);
         if (contentId.equals(OpActivityDataSetFactory.NO_CONTENT_ID)) {
            byte[] bytes = (byte[]) attachmentElement.get(5);
            OpContent content = OpContentManager.newContent(bytes, null);
            broker.makePersistent(content);
            content.getAttachments().add(attachment);
            attachment.setContent(content);
            broker.updateObject(content);
         }
         else {
            OpContent content = (OpContent) broker.getObject(contentId);
            OpContentManager.updateContent(content, broker, true);
            attachment.setContent(content);
            content.getAttachments().add(attachment);
            broker.updateObject(content);
         }
      }
      if (attachment.getID() == 0) {
         broker.makePersistent(attachment);
      }
      else {
         broker.updateObject(attachment);
      }
   }

   private static ArrayList updateOrDeleteDependencies(OpBroker broker, XComponent dataSet, Iterator dependencies) {
      OpDependency dependency = null;
      XComponent predecessorDataRow = null;
      XComponent successorDataRow = null;
      ArrayList predecessorIndexes = null;
      ArrayList successorIndexes = null;
      ArrayList reusableDependencys = new ArrayList();

      int maxActivitySequence = dataSet.getChildCount();
      int successorActivitySequence = 0;

      while (dependencies.hasNext()) {
         dependency = (OpDependency) dependencies.next();
         successorActivitySequence = dependency.getSuccessorActivity().getSequence();
         boolean reusable = false;
         if (successorActivitySequence < maxActivitySequence) { // successor activity was not deleted on client
            successorDataRow = (XComponent) dataSet.getChild(successorActivitySequence);
            predecessorIndexes = OpGanttValidator.getPredecessors(successorDataRow);
            if (predecessorIndexes.remove(new Integer(dependency.getPredecessorActivity().getSequence()))) {
               // Dependency still exists: Remove also other part of bi-directional association
               predecessorDataRow = (XComponent) dataSet.getChild(dependency.getPredecessorActivity().getSequence());
               successorIndexes = OpGanttValidator.getSuccessors(predecessorDataRow);
               successorIndexes.remove(new Integer(dependency.getSuccessorActivity().getSequence()));
            }
            else {
               // Dependency was deleted on client: Add to reusable objects
               reusable = true;
            }
         }
         else {
            reusable = true;
         }
         if (reusable) {
            reusableDependencys.add(dependency);
            //break link activity->dependency
            OpActivity successor = dependency.getSuccessorActivity();
            successor.getPredecessorDependencies().remove(dependency);
            OpActivity predecessor = dependency.getPredecessorActivity();
            predecessor.getSuccessorDependencies().remove(dependency);
            broker.updateObject(successor);
            broker.updateObject(predecessor);
         }
      }
      return reusableDependencys;
   }

   private static void insertActivityDependencies(OpBroker broker, OpProjectPlan plan, XComponent dataSet,
        XComponent dataRow, OpActivity activity, ArrayList activityList, ArrayList reusableDependencys) {
      // Note: We only check for new predecessor indexes
      // (Successors are just the other side of the bi-directional association)
      ArrayList predecessorIndexes = OpGanttValidator.getPredecessors(dataRow);
      OpDependency dependency = null;
      XComponent predecessorDataRow = null;
      OpActivity predecessor = null;
      for (int i = 0; i < predecessorIndexes.size(); i++) {
         // Insert new dependency version
         if ((reusableDependencys != null) && (reusableDependencys.size() > 0)) {
            dependency = (OpDependency) reusableDependencys.remove(reusableDependencys.size() - 1);
         }
         else {
            dependency = new OpDependency();
         }
         dependency.setProjectPlan(plan);
         predecessorDataRow = (XComponent) dataSet.getChild(((Integer) predecessorIndexes.get(i)).intValue());
         predecessor = (OpActivity) activityList.get(predecessorDataRow.getIndex());
         dependency.setPredecessorActivity(predecessor);
         dependency.setSuccessorActivity(activity);
         if (dependency.getID() == 0) {
            broker.makePersistent(dependency);
         }
         else {
            broker.updateObject(dependency);
         }
      }
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
      List categories = broker.list(query);
      OpActivityCategory category = null;
      XComponent dataRow = null;
      XComponent dataCell = null;
      for (int i = 0; i < categories.size(); i++) {
         category = (OpActivityCategory) categories.get(i);
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


}
