/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.error.XLocalizableException;
import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpPermissionDataSetFactory;
import onepoint.project.modules.work.OpProgressCalculator;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.server.XServiceManager;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;

public abstract class OpActivityDataSetFactory {

   private static final XLog logger = XLogFactory.getServerLogger(OpActivityDataSetFactory.class);
   private static final String GET_WORK_RECORD_COUNT_FOR_ASSIGNMENT =
        "select count(workRecord.ID) from OpWorkRecord workRecord where workRecord.Assignment = (:assignmentId)";
   private static final String GET_HOURLY_RATES_PERIOD_COUNT_FOR_PROJECT_ASSIGNMENT =
        "select count(hourlyRates.ID) from OpHourlyRatesPeriod hourlyRates where hourlyRates.ProjectNodeAssignment = (:assignmentId)";
   private static final String GET_SUBACTIVITIES_COUNT_FOR_ACTIVITY =
        "select count(activity.ID) from OpActivity activity where activity.SuperActivity = (:activityId)";

   public static HashMap resourceMap(OpBroker broker, OpProjectNode projectNode) {
      OpQuery query = broker
           .newQuery("select assignment.Resource from OpProjectNodeAssignment as assignment where assignment.ProjectNode.ID = ? order by assignment.Resource.Name asc");
      query.setLong(0, projectNode.getID());
      Iterator resources = broker.iterate(query);
      //LinkedHashMap to maintain the order in which entries are added
      HashMap resourceMap = new LinkedHashMap();
      OpResource resource = null;
      while (resources.hasNext()) {
         resource = (OpResource) (resources.next());
         resourceMap.put(new Long(resource.getID()), resource);
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
   public static void fillHourlyRatesDataSet(OpProjectNode project, XComponent dataSet) {
      OpProjectAdministrationService service = (OpProjectAdministrationService) XServiceManager.getService(OpProjectAdministrationService.SERVICE_NAME);
      service.fillHourlyRatesDataSet(project, dataSet);
   }
   //<FIXME>

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
         retrieveActivityDataRow(broker, activity, dataRow, editable);
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
         showHours = Boolean.valueOf(OpSettingsService.getService().get(OpSettings.SHOW_RESOURCES_IN_HOURS));
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
         Iterator result = broker.iterate(query);
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
         retrieveActivityDataRow(broker, activity, dataRow, false);
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
         showHours = Boolean.valueOf(OpSettingsService.getService().get(OpSettings.SHOW_RESOURCES_IN_HOURS));
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

   /**
    * Sets the information regardind the set of attachments on the data row.
    *
    * @param attachments    - the <code>Set</code> of <code>OpAttachment</code> entities
    * @param attachmentList - the <code>list</code> where the information regarding the attachments will be set
    */
   public static void retrieveAttachments(Set attachments, List attachmentList) {
      // TODO: Bulk-fetch like other parts of the project plan
      Iterator i = attachments.iterator();
      OpAttachment attachment = null;
      ArrayList attachmentElement = null;
      while (i.hasNext()) {
         attachment = (OpAttachment) i.next();
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
            String contentId = OpLocator.locatorString(attachment.getContent());
            attachmentElement.add(contentId);
         }
         else {
            attachmentElement.add(OpProjectConstants.NO_CONTENT_ID);
         }
         attachmentList.add(attachmentElement);
      }
   }

   private static void retrieveActivityDataRow(OpBroker broker, OpActivity activity, XComponent dataRow, boolean editable) {

      dataRow.setStringValue(activity.locator());

      boolean isCollection = (activity.getType() == OpActivity.COLLECTION || activity.getType() == OpActivity.COLLECTION_TASK);
      boolean isStandard = (activity.getType() == OpActivity.STANDARD);
      boolean isStrictlyTask = (activity.getType() == OpActivity.TASK);
      boolean isTask = isStrictlyTask || (activity.getType() == OpActivity.COLLECTION_TASK);
      boolean isScheduledTask = (activity.getType() == OpActivity.SCHEDULED_TASK);
      boolean isMilestone = (activity.getType() == OpActivity.MILESTONE);
      boolean isAdHocTask = (activity.getType() == OpActivity.ADHOC_TASK);

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
         dataCell.setStringValue(XValidator.choice(category.locator(), category.getName())); // one select...
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
      dataCell.setEnabled(editable && !isCollection && !isScheduledTask && !isAdHocTask);
      if (!isAdHocTask) {
         dataCell.setDoubleValue(activity.getBaseEffort());
      }
      else {
         dataCell.setValue(null);
      }
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
         List attachmentList = OpGanttValidator.getAttachments(dataRow); // one select...
         retrieveAttachments(activity.getAttachments(), attachmentList);
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
      Set assignments = activity.getAssignments();  //one db-select
      Map data = new HashMap(assignments.size());
      Iterator it = assignments.iterator();
      while (it.hasNext()) {
         OpAssignment assignment = (OpAssignment) it.next();
         String resourceLocator = assignment.getResource().locator();
         Boolean hasWorkRecords = hasWorkRecords(broker, assignment);
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

      //Responsible resource (27)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(false);
      OpResource responsibleResource = activity.getResponsibleResource();
      if (responsibleResource != null) {
         dataCell.setStringValue(XValidator.choice(responsibleResource.locator(), responsibleResource.getName()));
      }
      dataRow.addChild(dataCell);

      // Add project column (28)
      dataCell = new XComponent(XComponent.DATA_CELL);
      OpProjectNode projectNode = activity.getProjectPlan().getProjectNode();
      dataCell.setStringValue(XValidator.choice(projectNode.locator(), projectNode.getName()));
      dataRow.addChild(dataCell);

      // Payment (29)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setEnabled(editable && isMilestone);
      dataCell.setDoubleValue(activity.getPayment());
      dataRow.addChild(dataCell);

      // Base Proceeds (30)
      dataCell = new XComponent(XComponent.DATA_CELL);
      dataCell.setDoubleValue(activity.getBaseProceeds());
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

   public static void storeActivityDataSet(OpBroker broker, XComponent dataSet, HashMap resources, OpProjectPlan plan,
        OpProjectPlanVersion workingPlanVersion) {

      // TODO: Maybe use "pure" IDs (Long values) for data-row values instead of locator strings (retrieve)

      // If a working plan version is specified exchange activity IDs in the data-set with activity version IDs
      if (workingPlanVersion != null) {
         mapActivityVersionIDs(broker, dataSet, workingPlanVersion);
         plan.setHolidayCalendar(workingPlanVersion.getHolidayCalendar());
      }

      System.err.println("*** WORKING-P-V " + workingPlanVersion);

      // Prefetch activities
      List adhocTasks = getAdHocTasks(plan);
      if (plan.getActivities() != null) {
         plan.getActivities().removeAll(adhocTasks);
      }

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
            if (activity.getStart().before(planStart)) {
               planStart = activity.getStart();
            }
            if (activity.getFinish().after(planFinish)) {
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
      List reusableWorkPeriods = null;
      if (plan.getWorkPeriods() != null) {
         reusableWorkPeriods = updateOrDeleteWorkPeriods(broker, dataSet, plan.getWorkPeriods().iterator());
      }
      List reusableAttachments = null;
      if (plan.getActivityAttachments() != null) {
         reusableAttachments = updateOrDeleteAttachments(broker, dataSet, plan.getActivityAttachments().iterator());
      }
      List reusableDependencys = null;
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

      // Phase 5: Delete unused activities
      Iterator unusedActivitys = activities.values().iterator();
      while (unusedActivitys.hasNext()) {
         OpActivity markedActivity = (OpActivity) unusedActivitys.next();
         OpResource responsibleRes = markedActivity.getResponsibleResource();
         if (responsibleRes != null) {
            responsibleRes.getResponsibleActivities().remove(markedActivity);
            broker.updateObject(responsibleRes);
         }
         markedActivity.setDeleted(true);
         markedActivity.setResponsibleResource(null);
         broker.updateObject(markedActivity);
      }

      // Finally, update project plan version start and finish fields
      plan.setStart(planStart);
      plan.setFinish(planFinish);

      //add the adhoc tasks at the end of the project plan
      int dataRowsNr = dataSet.getChildCount();
      updateAdHocTasks(plan, dataRowsNr, adhocTasks, broker);

      broker.updateObject(plan);

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

   private static OpActivity insertOrUpdateActivity(OpBroker broker, XComponent dataRow, OpActivity activity,
        OpProjectPlan projectPlan, OpActivity superActivity) {

      String categoryLocator = null;
      OpActivityCategory category = null;

      OpProjectNode projectNode = projectPlan.getProjectNode();
      String categoryChoice = OpGanttValidator.getCategory(dataRow);
      String responsibleResourceChoice = OpGanttValidator.getResponsibleResource(dataRow);
      if (activity == null) {
         // Insert a new activity
         activity = new OpActivity(OpGanttValidator.getType(dataRow));
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
         if (categoryChoice != null) {
            category = (OpActivityCategory) broker.getObject(XValidator.choiceID(categoryChoice));
            activity.setCategory(category);
         }
         if (responsibleResourceChoice != null) {
            OpResource resource = (OpResource) broker.getObject(XValidator.choiceID(responsibleResourceChoice));
            activity.setResponsibleResource(resource);
         }
         activity.setBasePersonnelCosts(OpGanttValidator.getBasePersonnelCosts(dataRow));
         activity.setBaseProceeds(OpGanttValidator.getBaseProceeds(dataRow));
         activity.setBaseTravelCosts(OpGanttValidator.getBaseTravelCosts(dataRow));
         activity.setBaseMaterialCosts(OpGanttValidator.getBaseMaterialCosts(dataRow));
         activity.setBaseExternalCosts(OpGanttValidator.getBaseExternalCosts(dataRow));
         activity.setBaseMiscellaneousCosts(OpGanttValidator.getBaseMiscellaneousCosts(dataRow));
         activity.setAttributes(OpGanttValidator.getAttributes(dataRow));
         if (activity.getType() == OpGanttValidator.MILESTONE) {
            activity.setPayment(OpGanttValidator.getPayment(dataRow));
         }
         if (OpGanttValidator.getPriority(dataRow) != null) {
            activity.setPriority(OpGanttValidator.getPriority(dataRow).byteValue());
         }
         else {
            activity.setPriority((byte) 0);
         }
         activity.setActualEffort(0);

         double complete = OpGanttValidator.getComplete(dataRow);
         activity.setComplete(complete);
         double remaining = OpGanttValidator.calculateRemainingEffort(activity.getBaseEffort(), activity.getActualEffort(),
              complete);
         activity.setRemainingEffort(remaining);
         //actual costs are 0 initially
         activity.setActualPersonnelCosts(0);
         activity.setActualTravelCosts(0);
         activity.setRemainingTravelCosts(activity.getBaseTravelCosts());
         activity.setActualMaterialCosts(0);
         activity.setRemainingMaterialCosts(activity.getBaseMaterialCosts());
         activity.setActualExternalCosts(0);
         activity.setRemainingExternalCosts(activity.getBaseExternalCosts());
         activity.setActualMiscellaneousCosts(0);
         activity.setRemainingMiscellaneousCosts(activity.getBaseMiscellaneousCosts());
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
            if (activity.getProjectPlan().getProgressTracked()) {
               activity.setRemainingEffort(activity.getBaseEffort());
               complete = OpGanttValidator.calculateCompleteValue(activity.getActualEffort(), baseEffort, activity.getRemainingEffort());
               activity.setComplete(complete);
            }
            else {
               double remainingEffort = OpGanttValidator.calculateRemainingEffort(baseEffort, activity.getActualEffort(), activity.getComplete());
               activity.setRemainingEffort(remainingEffort);
            }
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

         if (categoryChoice != null) {
            String newCategoryLocator = XValidator.choiceID(categoryChoice);
            if (!newCategoryLocator.equals(categoryLocator)) {
               update = true;
               category = (OpActivityCategory) broker.getObject(newCategoryLocator);
               activity.setCategory(category);
            }
         }
         else {
            update = true;
            activity.setCategory(null);
         }

         //update the responsible resource
         String resourceLocator = null;
         if (activity.getResponsibleResource() != null) {
            resourceLocator = activity.getResponsibleResource().locator();
         }

         if (responsibleResourceChoice != null) {
            String newResourceLocator = XValidator.choiceID(responsibleResourceChoice);
            if (!newResourceLocator.equals(resourceLocator)) {
               update = true;
               OpResource resource = (OpResource) broker.getObject(XValidator.choiceID(newResourceLocator));
               activity.setResponsibleResource(resource);
            }
         }
         else {
            update = true;
            activity.setResponsibleResource(null);
         }

         // Do not update complete from client-data: Calculated from work-slips (RemainingEffort)
         if (activity.getBasePersonnelCosts() != OpGanttValidator.getBasePersonnelCosts(dataRow)) {
            update = true;
            activity.setBasePersonnelCosts(OpGanttValidator.getBasePersonnelCosts(dataRow));
         }
         if (activity.getBaseProceeds() != OpGanttValidator.getBaseProceeds(dataRow)) {
            update = true;
            activity.setBaseProceeds(OpGanttValidator.getBaseProceeds(dataRow));
         }

         if (activity.getBaseTravelCosts() != OpGanttValidator.getBaseTravelCosts(dataRow)) {
            update = true;
            activity.setBaseTravelCosts(OpGanttValidator.getBaseTravelCosts(dataRow));
            if (activity.getActualTravelCosts() == 0) {
               double remaining = activity.getBaseTravelCosts() - activity.getActualTravelCosts();
               if (remaining < 0) {
                  remaining = 0;
               }
               activity.setRemainingTravelCosts(remaining);
            }
         }
         if (activity.getBaseMaterialCosts() != OpGanttValidator.getBaseMaterialCosts(dataRow)) {
            update = true;
            activity.setBaseMaterialCosts(OpGanttValidator.getBaseMaterialCosts(dataRow));
            if (activity.getActualMaterialCosts() == 0) {
               double remaining = activity.getBaseMaterialCosts() - activity.getActualMaterialCosts();
               if (remaining < 0) {
                  remaining = 0;
               }
               activity.setRemainingMaterialCosts(remaining);
            }
         }
         if (activity.getBaseExternalCosts() != OpGanttValidator.getBaseExternalCosts(dataRow)) {
            update = true;
            activity.setBaseExternalCosts(OpGanttValidator.getBaseExternalCosts(dataRow));
            if (activity.getActualExternalCosts() == 0) {
               double remaining = activity.getBaseExternalCosts() - activity.getActualExternalCosts();
               if (remaining < 0) {
                  remaining = 0;
               }
               activity.setRemainingExternalCosts(remaining);
            }
         }
         if (activity.getBaseMiscellaneousCosts() != OpGanttValidator.getBaseMiscellaneousCosts(dataRow)) {
            update = true;
            activity.setBaseMiscellaneousCosts(OpGanttValidator.getBaseMiscellaneousCosts(dataRow));
            if (activity.getActualMiscellaneousCosts() == 0) {
               double remaining = activity.getBaseMiscellaneousCosts() - activity.getActualMiscellaneousCosts();
               if (remaining < 0) {
                  remaining = 0;
               }
               activity.setRemainingMiscellaneousCosts(remaining);
            }
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
         if (activity.getPayment() != OpGanttValidator.getPayment(dataRow)) {
            update = true;
            activity.setPayment(OpGanttValidator.getPayment(dataRow));
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

      OpAssignment assignment;
      XComponent dataRow;
      List resourceList;
      String resourceChoice;
      List resourceBaseEffortList;
      double baseEffort;
      double assigned;
      int i;
      int activitySequence;

      ArrayList reusableAssignments = new ArrayList();
      while (assignments.hasNext()) {
         assignment = (OpAssignment) assignments.next();
         activitySequence = assignment.getActivity().getSequence();
         boolean reusable = false;
         if (!deletedActivity(assignment.getActivity(), dataSet)) { // activity was not deleted on the client
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

                  //<FIXME author="Horia Chiorean" description="Caused by double calculations in the OpGanntValidator...">
                  if (Math.abs(assignment.getBaseEffort() - baseEffort) > OpGanttValidator.ERROR_MARGIN) {
                     //<FIXME>
                     assignment.setBaseEffort(baseEffort);
                     if (tracking) {

                        if (assignment.getActualEffort() == 0) {
                           assignment.setRemainingEffort(baseEffort);
                           assignment.setComplete(0);
                        }
                        else {

                           //assignment remaining effort isn't changed becasue we have actual/remaining effort provided by the user
                           complete = OpGanttValidator.calculateCompleteValue(assignment.getActualEffort(), baseEffort, assignment.getRemainingEffort());
                           assignment.setComplete(complete);

                           //update the remaining activity effort
                           double effortToAdd = assignment.getRemainingEffort() - assignment.getBaseEffort();
                           updateRemainingForActivities(broker, activity, effortToAdd);
                        }
                     }
                     else {
                        double remaining = OpGanttValidator.calculateRemainingEffort(baseEffort, assignment.getActualEffort(), assignment.getComplete());
                        assignment.setRemainingEffort(remaining);
                     }
                     update = true;
                  }

                  XCalendar calendar = XCalendar.getDefaultCalendar();
                  if (updateAssignmentCosts(assignment, calendar)) {
                     update = true;
                  }

                  if (update) {
                     broker.updateObject(assignment);
                     updateWorkMonths(broker, assignment, calendar);
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
               //check if the assignemnt still has work records - if so => error
               if (hasWorkRecords(broker, assignment)) {
                  throw new XLocalizableException(OpProjectAdministrationService.ERROR_MAP, OpProjectError.WORKRECORDS_STILL_EXIST_ERROR);
               }
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

   private static void updateRemainingForActivities(OpBroker broker, OpActivity activity, double effortToAdd) {
      if (activity == null) {
         return;
      }

      activity.setRemainingEffort(activity.getRemainingEffort() + effortToAdd);
      double complete = OpGanttValidator.calculateCompleteValue(activity.getActualEffort(), activity.getBaseEffort(), activity.getRemainingEffort());
      activity.setComplete(complete);
      broker.updateObject(activity);

      updateRemainingForActivities(broker, activity.getSuperActivity(), effortToAdd);
   }

   private static void insertActivityAssignments(OpBroker broker, OpProjectPlan plan, XComponent dataRow,
        OpActivity activity, ArrayList reusableAssignments, HashMap resources) {
      List resourceList = OpGanttValidator.getResources(dataRow);
      String resourceChoice = null;
      List resourceBaseEffortList = OpGanttValidator.getResourceBaseEfforts(dataRow);
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

         XCalendar calendar = XCalendar.getDefaultCalendar();
         updateAssignmentCosts(assignment, calendar);

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
         updateWorkMonths(broker, assignment, calendar);
      }
   }

   private static ArrayList updateOrDeleteWorkPeriods(OpBroker broker, XComponent dataSet, Iterator workPeriodsIt) {
      OpWorkPeriod workPeriod = null;
      XComponent dataRow = null;
      int i = 0;
      double baseEffort = 0.0d;
      ArrayList reusableWorkPeriods = new ArrayList();
      boolean update;
      int activitySequence = 0;

      while (workPeriodsIt.hasNext()) {
         workPeriod = (OpWorkPeriod) workPeriodsIt.next();
         activitySequence = workPeriod.getActivity().getSequence();
         Date periodStart = workPeriod.getStart();
         update = false;
         boolean reusable = false;
         if (!deletedActivity(workPeriod.getActivity(), dataSet)) { // activity was not deleted on client
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
        OpActivity activity, List reusableWorkPeriods) {

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
      ArrayList reusableAttachments = new ArrayList();
      int maxActivitySequence = dataSet.getChildCount();
      while (attachments.hasNext()) {
         OpAttachment attachment = (OpAttachment) attachments.next();
         OpActivity activity = attachment.getActivity();
         if (activity.getType() == OpActivity.ADHOC_TASK) {
            continue; // exclude attachments from ADHOC_TASKs
         }
         int activitySequence = activity.getSequence();
         int i;
         boolean reusable = false;
         if (!deletedActivity(activity, dataSet)) { // activity was not deleted on client
            XComponent dataRow = (XComponent) dataSet.getChild(activity.getSequence());
            List attachmentList = OpGanttValidator.getAttachments(dataRow);
            for (i = attachmentList.size() - 1; i >= 0; i--) {
               // Note: We assume that attachments can only be added and removed on the client (no expicit updates)
               ArrayList attachmentElement = (ArrayList) attachmentList.get(i);
               OpLocator locator = OpLocator.parseLocator(XValidator.choiceID((String) attachmentElement.get(1)));
               if (locator == null) { // new attachment added on client
                  continue;
               }
               if (attachment.getID() == locator.getID()) {
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
            OpContentManager.updateContent(content, broker, false, attachment);
            reusableAttachments.add(attachment);
            //break link from activity to attachment
            activity.getAttachments().remove(attachment);
         }
      }
      return reusableAttachments;
   }

   private static void insertActivityAttachments(OpBroker broker, OpProjectPlan plan, XComponent dataRow,
        OpActivity activity, List reusableAttachments) {
      List attachmentList = OpGanttValidator.getAttachments(dataRow);
      ArrayList attachmentElement = null;
      OpAttachment attachment = null;
      for (int i = 0; i < attachmentList.size(); i++) {
         // Insert new attachment version
         attachmentElement = (ArrayList) attachmentList.get(i);
         createAttachment(broker, activity, plan, attachmentElement, reusableAttachments, null);
      }
   }

   /**
    * Creates an <code>OpAttachment</code> entity out o a list of attachment atributes.
    *
    * @param broker
    * @param activity            - the <code>OpActivity</code> entity for which the attachments is created
    *                            (in this case the projectNode parameter is null)
    * @param plan                - the <code>OpProjectPlan</code> entity to which the activity belongs
    * @param attachmentElement   - the <code>List</code> of attachment attributes
    * @param reusableAttachments - the list of already created attachments that need to be updated
    * @param projectNode         - the <code>OpProjectNode</code> entity for which the attachment is created
    *                            (in this case the activity parameter is null)
    * @return - the newly created/updated <code>OpAttachment</code> entity , could be <code>null</code> if the content id is not valid
    */
   public static OpAttachment createAttachment(OpBroker broker, OpActivity activity, OpProjectPlan plan, List attachmentElement,
        List reusableAttachments, OpProjectNode projectNode) {
      OpAttachment attachment;
      if ((reusableAttachments != null) && (reusableAttachments.size() > 0)) {
         attachment = (OpAttachment) reusableAttachments.remove(reusableAttachments.size() - 1);
      }
      else {
         attachment = new OpAttachment();
      }
      attachment.setProjectPlan(plan);
      attachment.setActivity(activity);
      attachment.setLinked(OpProjectConstants.LINKED_ATTACHMENT_DESCRIPTOR.equals(attachmentElement.get(0)));
      attachment.setName((String) attachmentElement.get(2));
      attachment.setLocation((String) attachmentElement.get(3));
      if (plan != null) {
         OpPermissionDataSetFactory.updatePermissions(broker, plan.getProjectNode(), attachment);
      }
      else {
         if (projectNode != null) {
            OpPermissionDataSetFactory.updatePermissions(broker, projectNode, attachment);
         }
      }

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
      if (attachment.getID() == 0) {
         broker.makePersistent(attachment);
      }
      else {
         broker.updateObject(attachment);
      }

      return attachment;
   }

   private static List updateOrDeleteDependencies(OpBroker broker, XComponent dataSet, Iterator dependencies) {
      OpDependency dependency = null;
      XComponent predecessorDataRow = null;
      XComponent successorDataRow = null;
      List predecessorIndexes = null;
      List successorIndexes = null;
      List reusableDependencys = new ArrayList();

      int maxActivitySequence = dataSet.getChildCount();
      int successorActivitySequence = 0;

      while (dependencies.hasNext()) {
         dependency = (OpDependency) dependencies.next();
         successorActivitySequence = dependency.getSuccessorActivity().getSequence();
         boolean reusable = false;
         if (!deletedActivity(dependency.getSuccessorActivity(), dataSet)) { // successor activity was not deleted on client
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
        XComponent dataRow, OpActivity activity, List activityList, List reusableDependencys) {
      // Note: We only check for new predecessor indexes
      // (Successors are just the other side of the bi-directional association)
      List predecessorIndexes = OpGanttValidator.getPredecessors(dataRow);
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
    * Checks if a given activity entity was deletd on the client using the data set received.
    *
    * @param activity
    * @param dataSet
    * @return true if the activity was deleted
    */
   private static boolean deletedActivity(OpActivity activity, XComponent dataSet) {
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         String locator = dataRow.getStringValue();
         long id = OpLocator.parseLocator(locator).getID();
         if (id == activity.getID()) {
            return false;
         }
      }
      return true;
   }

   /**
    * Updates the base personnel costs and base proceeds of the activities that belong to the assignments
    * passed as parameter
    *
    * @param broker             - a <code>OpBroker</code> used for performing business operations.
    * @param updatedAssignments - a <code>List</code> of <code>OpAssignmentVersion</code> that contain the
    *                           <code>OpActivityVersion</code> to be updated
    */
   public static void updateActivityVersionPersonnelCosts(OpBroker broker, List<OpAssignmentVersion> updatedAssignments) {
      OpActivityVersion activityVersion;

      List<OpActivityVersion> updatedActivities = new ArrayList<OpActivityVersion>();

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
               broker.updateObject(activityVersion);
               activityVersion = superActivityVersion;
            }
            broker.updateObject(activityVersion);
         }
      }
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
      List<OpActivity> updatedActivities = new ArrayList<OpActivity>();
      OpActivity activity;

      for (OpAssignment assignment : updatedAssignments) {
         double sumActualCosts = 0;
         double sumActualProceeds = 0;
         activity = assignment.getActivity();
         if (!updatedActivities.contains(activity)) {
            double oldActualCosts = activity.getActualPersonnelCosts();
            double oldActualProceeds = activity.getActualProceeds();

            for (OpAssignment assignmentOfActivity : activity.getAssignments()) {
               sumActualCosts += assignmentOfActivity.getActualCosts();
               sumActualProceeds += assignmentOfActivity.getActualProceeds();
            }
            activity.setActualPersonnelCosts(sumActualCosts);
            activity.setActualProceeds(sumActualProceeds);
            updatedActivities.add(activity);

            //update all super activities
            while (activity.getSuperActivity() != null) {
               OpActivity superActivity = activity.getSuperActivity();
               double actualCostsDifference = activity.getActualPersonnelCosts() - oldActualCosts;
               double actualProceedsDifference = activity.getActualProceeds() - oldActualProceeds;
               oldActualCosts = superActivity.getActualPersonnelCosts();
               oldActualProceeds = superActivity.getActualProceeds();
               superActivity.setActualPersonnelCosts(superActivity.getActualPersonnelCosts() + actualCostsDifference);
               superActivity.setActualProceeds(superActivity.getActualProceeds() + actualProceedsDifference);
               broker.updateObject(activity);
               activity = superActivity;
            }
            broker.updateObject(activity);
         }
      }
   }

   /**
    * Checks if the base costs and the base proceeds have changed for the assignment passed as parameter and
    * returns a <code>boolean</code> value indicating whether the costs have changed or not. If the costs have
    * changed then the noew costs are set on the assignment.
    *
    * @param assignment - the <code>OpAssignment</code> entity on which the new costs are set
    * @param calendar   - the <code>XCalendar</code> needed to handle dates
    * @return - <code>true</code> if the base costs or the base proceeds or both have changed and <code>false</code>
    *         otherwise.
    */
   public static boolean updateAssignmentCosts(OpAssignment assignment, XCalendar calendar) {
      boolean modified = false;
      OpActivity activity = assignment.getActivity();
      OpProjectNodeAssignment projectNodeAssignment = null;
      Double internalSum = 0d;
      Double externalSum = 0d;

      if (activity.getType() != OpActivity.MILESTONE) {
         List<java.sql.Date> startEndList = activity.getStartEndDateByType();
         List<java.sql.Date> workingDays = calendar.getWorkingDaysFromInterval(startEndList.get(OpActivityVersion.START_DATE_LIST_INDEX),
              startEndList.get(OpActivityVersion.END_DATE_LIST_INDEX));
         double workHoursPerDay = calendar.getWorkHoursPerDay();
         if (activity.getType() == OpActivity.TASK) {
            if (workingDays.size() != 0) {
               workHoursPerDay = activity.getBaseEffort() / (double) workingDays.size();
            }
            else {
               workHoursPerDay = 0;
            }
         }

         if (startEndList != null) {
            //get the project node assignment for this assignment's resource
            projectNodeAssignment = assignment.getProjectNodeAssignment();
            List<List<Double>> ratesList = projectNodeAssignment.getRatesForListOfDays(workingDays);
            List<Double> internalRatesList = ratesList.get(OpProjectNodeAssignment.INTERNAL_RATE_LIST_INDEX);
            List<Double> externalRatesList = ratesList.get(OpProjectNodeAssignment.EXTERNAL_RATE_LIST_INDEX);
            for (Double internalRate : internalRatesList) {
               internalSum += internalRate * workHoursPerDay * assignment.getAssigned() / 100;
            }
            for (Double externalRate : externalRatesList) {
               externalSum += externalRate * workHoursPerDay * assignment.getAssigned() / 100;
            }
         }

         if (assignment.getBaseCosts() != internalSum || assignment.getBaseProceeds() != externalSum) {
            assignment.setBaseCosts(internalSum);
            assignment.setBaseProceeds(externalSum);
            modified = true;
         }
      }

      return modified;
   }

   /**
    * Updates the work monts for the given assignment.
    *
    * @param broker     Broker access object.
    * @param assignment Assignment to update the work months for.
    * @param xCalendar  Session calendar.
    */
   public static void updateWorkMonths(OpBroker broker, OpAssignment assignment, XCalendar xCalendar) {

      OpActivity activity = assignment.getActivity();
      if (activity.getType() == OpActivity.MILESTONE) {
         return; // no workmonths form milestones
      }

      OpProjectNodeAssignment projectAssignment = assignment.getProjectNodeAssignment();

      List<OpWorkMonth> reusableWorkMonths = new ArrayList<OpWorkMonth>();
      Set<OpWorkMonth> opWorkMonths = assignment.getWorkMonths();
      if (opWorkMonths != null) {
         reusableWorkMonths.addAll(opWorkMonths);
      }

      double workHoursPerDay = xCalendar.getWorkHoursPerDay();

      //update latest values
      OpWorkMonth workMonth;
      if (!reusableWorkMonths.isEmpty()) {
         workMonth = reusableWorkMonths.remove(reusableWorkMonths.size() - 1);
      }
      else {
         workMonth = new OpWorkMonth();
      }

      Calendar calendar = xCalendar.getCalendar();
      Date start = activity.getStart();
      Date finish = activity.getFinish();

      if (start == null || finish == null) {
         return;
      }

      Date date = new Date(start.getTime());
      calendar.setTime(date);
      int month = calendar.get(Calendar.MONTH);
      int year = calendar.get(Calendar.YEAR);
      double internalSum = 0;
      double externalSum = 0;
      byte workingDays = 0;
      List<OpWorkMonth> newWorkMonths = new ArrayList<OpWorkMonth>();

      while (!date.after(finish)) {
         if (xCalendar.isWorkDay(date)) {
            List<Double> rates = projectAssignment.getRatesForDay(date, true);
            double internalRate = rates.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
            double externalRate = rates.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
            internalSum += internalRate * workHoursPerDay * assignment.getAssigned() / 100;
            externalSum += externalRate * workHoursPerDay * assignment.getAssigned() / 100;
            workingDays++;
         }
         date = new Date(date.getTime() + XCalendar.MILLIS_PER_DAY);

         calendar.setTime(date);
         if ((month != calendar.get(Calendar.MONTH) || year != calendar.get(Calendar.YEAR)) || date.after(finish)) {
            //new workmonth entity... set the values on the previous one.
            double latestEffort = workingDays * workHoursPerDay * assignment.getAssigned() / 100;

            workMonth.setAssignment(assignment);
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

            workMonth.setWorkingDays(workingDays);

            newWorkMonths.add(workMonth);

            //reset counters
            workingDays = 0;
            internalSum = 0;
            externalSum = 0;
            month = calendar.get(Calendar.MONTH);
            year = calendar.get(Calendar.YEAR);
            if (!reusableWorkMonths.isEmpty()) {
               workMonth = reusableWorkMonths.remove(reusableWorkMonths.size() - 1);
            }
            else {
               workMonth = new OpWorkMonth();
            }
         }
      }

      updateWorkMonthBaseValues(broker, assignment, newWorkMonths, reusableWorkMonths);

      updateRemainingValues(broker, xCalendar, assignment);

      broker.updateObject(assignment);
   }

   /**
    * Updates the baseline values of the given workmonth entities.
    *
    * @param assignment         Current assignment
    * @param newWorkMonths      Newly created work months. This list will be updated with baseline values workmonths.
    * @param reusableWorkMonths WorkMonths that can be reused.
    */
   public static void updateWorkMonthBaseValues(OpBroker broker, OpAssignment assignment, List<OpWorkMonth> newWorkMonths, List<OpWorkMonth> reusableWorkMonths) {

      OpActivity activity = assignment.getActivity();
      if (activity.getType() == OpActivity.MILESTONE) {
         return; // no workmonths form milestones
      }

      boolean hasBaselineVersion = assignment.getBaselineVersion() != null;
      if (hasBaselineVersion) {

         Set<OpWorkMonthVersion> baselineMonthVersions = new HashSet<OpWorkMonthVersion>();
         OpAssignmentVersion assignmentBaselineVersion = assignment.getBaselineVersion();
         if (assignmentBaselineVersion != null) {
            baselineMonthVersions = new HashSet<OpWorkMonthVersion>(assignmentBaselineVersion.getWorkMonthVersions());
         }

         //update the newly created workmonths with the values from the baseline
         OpWorkMonth workMonth;
         for (OpWorkMonth newWorkMonth : newWorkMonths) {
            boolean found = false;
            for (Iterator it = baselineMonthVersions.iterator(); it.hasNext();) {
               OpWorkMonthVersion monthVersion = (OpWorkMonthVersion) it.next();
               if (monthVersion.getYear() == newWorkMonth.getYear() && monthVersion.getMonth() == newWorkMonth.getMonth()) {
                  //found the coresponding work month version
                  newWorkMonth.setBaseAssigned(monthVersion.getBaseAssigned());
                  newWorkMonth.setBaseEffort(monthVersion.getBaseEffort());
                  newWorkMonth.setBasePersonnelCosts(monthVersion.getBasePersonnelCosts());
                  newWorkMonth.setBaseProceeds(monthVersion.getBaseProceeds());
                  it.remove();
                  found = true;
               }
            }
            if (!found) {
               OpAssignmentVersion version = assignment.getBaselineVersion();
               if (version != null) {
                  newWorkMonth.setBaseAssigned(version.getAssigned());
               }
            }
         }

         //fill up remaining work months from versions
         for (OpWorkMonthVersion baselineMonthVersion : baselineMonthVersions) {
            if (!reusableWorkMonths.isEmpty()) {
               workMonth = reusableWorkMonths.remove(reusableWorkMonths.size() - 1);
            }
            else {
               workMonth = new OpWorkMonth();
            }

            workMonth.setAssignment(assignment);
            workMonth.setLatestAssigned(assignment.getAssigned());
            workMonth.setLatestEffort(0);
            workMonth.setLatestPersonnelCosts(0);
            workMonth.setLatestProceeds(0);

            workMonth.setYear(baselineMonthVersion.getYear());
            workMonth.setMonth(baselineMonthVersion.getMonth());
            workMonth.setBaseAssigned(baselineMonthVersion.getBaseAssigned());
            workMonth.setBaseEffort(baselineMonthVersion.getBaseEffort());
            workMonth.setBasePersonnelCosts(baselineMonthVersion.getBasePersonnelCosts());
            workMonth.setBaseProceeds(baselineMonthVersion.getBaseProceeds());
            newWorkMonths.add(workMonth);
         }

      }
      else {
         for (OpWorkMonth workMonth : newWorkMonths) {
            workMonth.setBaseAssigned(assignment.getAssigned());
            workMonth.setBaseEffort(workMonth.getLatestEffort());
            workMonth.setBasePersonnelCosts(workMonth.getLatestPersonnelCosts());
            workMonth.setBaseProceeds(workMonth.getLatestProceeds());
         }
      }

      assignment.removeWorkMonths(reusableWorkMonths);
      assignment.setWorkMonths(new HashSet<OpWorkMonth>(newWorkMonths));
      broker.updateObject(assignment);
   }

   /**
    * Updates the remaining personnel cost/proceeds for the given assignment on its workmonths.
    *
    * @param broker
    * @param xCalendar  Session calendar.
    * @param assignment Assignment to make the update for.
    */
   public static void updateRemainingValues(OpBroker broker, XCalendar xCalendar, OpAssignment assignment) {

      OpProjectNodeAssignment projectAssignment = assignment.getProjectNodeAssignment();

      OpActivity activity = assignment.getActivity();
      if (activity.getType() == OpActivity.MILESTONE) {
         return; // no workmonths form milestones
      }

      double actualEffort = assignment.getActualEffort();
      Set<OpWorkMonth> workMonths = assignment.getWorkMonths();

      if (actualEffort == 0) {
         for (OpWorkMonth workMonth : workMonths) {
            workMonth.setRemainingPersonnel(workMonth.getLatestPersonnelCosts());
            workMonth.setRemainingProceeds(workMonth.getLatestProceeds());
            workMonth.setRemainingEffort(workMonth.getLatestEffort());
         }
      }
      else {
         double remainingEffort = assignment.getRemainingEffort();

         Calendar calendar = xCalendar.getCalendar();
         Date start = activity.getStart();
         Date finish = activity.getFinish();
         if (start == null || finish == null) {
            return;
         }

         Date date = new Date(start.getTime());
         calendar.setTime(date);

         //reset the remaining values and calculate the total nr of days
         double workingDays = 0;
         for (OpWorkMonth workMonth : workMonths) {
            workMonth.setRemainingPersonnel(0d);
            workMonth.setRemainingProceeds(0d);
            workMonth.setRemainingEffort(0d);
            workingDays += workMonth.getWorkingDays();
         }

         if (assignment.getBaseEffort() > actualEffort) {
            double workHoursPerDay = xCalendar.getWorkHoursPerDay();
            //find the new start date to distribute the remaining effort
            while (actualEffort > 0) {
               if (xCalendar.isWorkDay(date)) {
                  actualEffort -= workHoursPerDay;
                  workingDays--;
               }
               date = new Date(date.getTime() + XCalendar.MILLIS_PER_DAY);
            }
         }

         //distribute the remaining effort starting from date...
         double remainingEffortPerDay = remainingEffort / workingDays;
         calendar.setTime(date);
         OpWorkMonth workMonth = assignment.getWorkMonth(calendar.get(Calendar.YEAR), (byte) calendar.get(Calendar.MONTH));
         double internalSum = 0;
         double externalSum = 0;
         double workMonthRemainingEffort = 0;
         while (!date.after(finish)) {

            if (xCalendar.isWorkDay(date)) {
               List<Double> rates = projectAssignment.getRatesForDay(date, true);
               double internalRate = rates.get(OpProjectNodeAssignment.INTERNAL_RATE_INDEX);
               double externalRate = rates.get(OpProjectNodeAssignment.EXTERNAL_RATE_INDEX);
               internalSum += internalRate * remainingEffortPerDay;
               externalSum += externalRate * remainingEffortPerDay;
               workMonthRemainingEffort += remainingEffortPerDay;
            }

            date = new Date(date.getTime() + XCalendar.MILLIS_PER_DAY);
            calendar.setTime(date);
            if (workMonth.getMonth() != calendar.get(Calendar.MONTH) ||
                 workMonth.getYear() != calendar.get(Calendar.YEAR) || date.after(finish)) {

               workMonth.setRemainingPersonnel(internalSum);
               workMonth.setRemainingProceeds(externalSum);
               workMonth.setRemainingEffort(workMonthRemainingEffort);

               internalSum = 0;
               externalSum = 0;
               workMonthRemainingEffort = 0;
               workMonth = assignment.getWorkMonth(calendar.get(Calendar.YEAR), (byte) calendar.get(Calendar.MONTH));
            }
         }
      }
      assignment.updateRemainingPersonnelCosts();
      assignment.updateRemainingProceeds();

      broker.updateObject(assignment);
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
      List<Integer> oldPredecessors;
      List<Integer> newPredecessors;
      List<Integer> oldSuccesssors;
      List<Integer> newSuccesssors;
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         dataRow = (XComponent) dataSet.getChild(i);

         //replace the predecessors old indexes with the new ones
         oldPredecessors = OpGanttValidator.getPredecessors(dataRow);
         if (!oldPredecessors.isEmpty()) {
            newPredecessors = new ArrayList<Integer>();
            for (Integer predecessor : oldPredecessors) {
               String predecessorId = oldIndexIdMap.get(predecessor);
               newPredecessors.add(newIdIndexMap.get(predecessorId));
            }
            OpGanttValidator.setPredecessors(dataRow, newPredecessors);
         }

         //replace the successors old indexes with the new ones
         oldSuccesssors = OpGanttValidator.getSuccessors(dataRow);
         if (!oldSuccesssors.isEmpty()) {
            newSuccesssors = new ArrayList<Integer>();
            for (Integer successor : oldSuccesssors) {
               String successorId = oldIndexIdMap.get(successor);
               newSuccesssors.add(newIdIndexMap.get(successorId));
            }
            OpGanttValidator.setSuccessors(dataRow, newSuccesssors);
         }
      }
   }

   /**
    * Returns <code>true</code> if the assignment specified as parameter has any work records or <code>false</code> otherwise.
    *
    * @param broker - the <code>OpBroker</code> object needed to perform DB operations.
    * @param assignment - the <code>OpAssignment</code> object.
    * @return <code>true</code> if the assignment specified as parameter has any work records or <code>false</code> otherwise.
    */
   public static boolean hasWorkRecords(OpBroker broker, OpAssignment assignment) {
      if (assignment.getWorkRecords() != null) {
         OpQuery query = broker.newQuery(GET_WORK_RECORD_COUNT_FOR_ASSIGNMENT);
         query.setLong("assignmentId", assignment.getID());
         Number counter = (Number) broker.iterate(query).next();
         if (counter.intValue() > 0) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns <code>true</code> if the project assignment specified as parameter has any hourly rate periods or
    *    <code>false</code> otherwise.
    *
    * @param broker     - the <code>OpBroker</code> object needed to perform DB operations.
    * @param projectAssignment - the <code>OpProjectNodeAssignment</code> object.
    * @return <code>true</code> if the project assignment specified as parameter has any hourly rate periods or
    *    <code>false</code> otherwise.
    */
   public static boolean hasHourlyRatesPeriods(OpBroker broker, OpProjectNodeAssignment projectAssignment) {
      if (projectAssignment.getHourlyRatesPeriods() != null) {
         OpQuery query = broker.newQuery(GET_HOURLY_RATES_PERIOD_COUNT_FOR_PROJECT_ASSIGNMENT);
         query.setLong("assignmentId", projectAssignment.getID());
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
    * @param broker            - the <code>OpBroker</code> object needed to perform DB operations.
    * @param activity - the <code>OpActivity</code> object.
    * @return the number of subactivities for the activity specified as parameter.
    */
   public static int getSubactivitiesCount(OpBroker broker, OpActivity activity) {
      if (activity.getSubActivities() != null) {
         OpQuery query = broker.newQuery(GET_SUBACTIVITIES_COUNT_FOR_ACTIVITY);
         query.setLong("activityId", activity.getID());
         Number counter = (Number) broker.iterate(query).next();
         return counter.intValue();
      }
      return 0;
   }
}