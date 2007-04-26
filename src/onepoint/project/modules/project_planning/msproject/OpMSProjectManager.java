/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.msproject;

import net.sf.mpxj.*;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXReader;
import net.sf.mpxj.mpx.MPXWriter;
import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpType;
import onepoint.persistence.OpTypeManager;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.components.OpActivityLoopException;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.util.XCalendar;
import onepoint.resource.XLocale;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.util.*;

/**
 * Class that provides methods in order to transform from MsProject structure in OPProject structure
 *
 * @author : mihai.costin
 */
public class OpMSProjectManager {

   private static final XLog logger = XLogFactory.getServerLogger(OpMSProjectManager.class);

   //utility class
   private OpMSProjectManager() {
   }

   /**
    * Fills the given data set with infor from the previously loaded ms project file.
    *
    * @return a dataset with all the saved activities
    */
   public static XComponent importActivities(InputStream sourceFile, OpProjectPlan projectPlan, XLocale xlocale)
        throws IOException {

      ProjectFile msProject;
      //for mpp file format the first activity is the name of the project
      boolean removeFirstActivity = false;
      try {
         msProject = new MPPReader().read(sourceFile);
         removeFirstActivity = true;
      }
      catch (MPXJException e) {
         try {
            sourceFile.reset();
            MPXReader reader = new MPXReader();
            reader.setLocale(new Locale(xlocale.getID()));
            msProject = reader.read(sourceFile);
         }
         catch (MPXJException e1) {
            throw new IOException("Unable to load the file " + sourceFile);
         }
      }

      boolean progressTracked = projectPlan.getProgressTracked();

      //map: <name>,<locator>
      Map projectResources = new HashMap();
      Map resourceAvailability = new HashMap();
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpGanttValidator validator = new OpGanttValidator();
      validator.setDataSet(dataSet);
      validator.setProjectStart(projectPlan.getProjectNode().getStart());
      validator.setProgressTracked(Boolean.valueOf(progressTracked));
      validator.setProjectTemplate(Boolean.valueOf(projectPlan.getTemplate()));

      XComponent assignmentSet = new XComponent(XComponent.DATA_SET);
      Iterator assignmentsIterator = projectPlan.getProjectNode().getAssignments().iterator();
      while (assignmentsIterator.hasNext()) {
         OpProjectNodeAssignment assignment = (OpProjectNodeAssignment) assignmentsIterator.next();
         OpResource resource = assignment.getResource();
         String resourceName = resource.getName();
         String resourceLocator = XValidator.choice(resource.locator(), resourceName);
         resourceAvailability.put(resource.locator(), new Double(resource.getAvailable()));
         projectResources.put(resourceName, resourceLocator);
         /*populate data set */
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(resourceLocator);
         XComponent dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(resource.getAvailable());
         dataRow.addChild(dataCell);
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setDoubleValue(resource.getHourlyRate());
         dataRow.addChild(dataCell);
         assignmentSet.addChild(dataRow);
      }
      validator.setAssignmentSet(assignmentSet);

      //clean data set
      dataSet.removeAllChildren();

      List msTasks = msProject.getAllTasks();
      Map activityMap = new HashMap();
      int index = 0;
      int firstOutlineLevel = 0;
      int startIndex = 0;
      if (removeFirstActivity) {
         startIndex = 1;
      }
      for (int i = startIndex; i < msTasks.size(); i++) {
         Task msTask = (Task) msTasks.get(i);
         XComponent activityRow = validator.newDataRow();

         OpGanttValidator.setName(activityRow, msTask.getName());
         OpGanttValidator.setType(activityRow, OpGanttValidator.STANDARD);
         //category = null
         if (!progressTracked) {
            OpGanttValidator.setComplete(activityRow, msTask.getPercentageComplete().doubleValue());
         }
         else {
            OpGanttValidator.setComplete(activityRow, 0);
         }
         java.util.Date start = msTask.getStart();
         if (start == null) {
            start = validator.getWorkingProjectStart();
         }
         OpGanttValidator.setStart(activityRow, new Date(start.getTime()));
         java.util.Date finish = msTask.getFinish();
         if (finish != null) {
            OpGanttValidator.setEnd(activityRow, new Date(finish.getTime()));
         }

         Duration msDuration = msTask.getDuration();
         OpGanttValidator.setDuration(activityRow, convertMsDurationToWorkingHours(msDuration));

         msDuration = msTask.getBaselineWork();
         OpGanttValidator.setBaseEffort(activityRow, convertMsDurationToWorkingHours(msDuration));

         //resources
         List assignments = msTask.getResourceAssignments();
         for (Iterator iterator = assignments.iterator(); iterator.hasNext();) {
            ResourceAssignment assignment = (ResourceAssignment) iterator.next();
            Resource resource = assignment.getResource();
            String name = resource.getName();
            if (projectResources.keySet().contains(name)) {
               String locator = (String) projectResources.get(name);
               String id = XValidator.choiceID(locator);
               String oppResource = XValidator.choice(id, name + " " + assignment.getUnits().doubleValue() + "%");
               OpGanttValidator.addResource(activityRow, oppResource);
               //OpGanttValidator.addResourceBaseEffort(activityRow, assignment.getBaseEffort());
               logger.info("Resource \"" + name + "\" has been added to the project on activity " + msTask.getName());
            }
            else {
               logger.warn("Resource \"" + name + "\" won't be added to the project. It is not assigned to the project plan.");
            }
         }

         //personnel costs  -- not imported (will be calculated by the validator)

         //costs are not imported
         //travel costs
         //material costs
         //misc costs

         String description = msTask.getNotes();
         int maxLen = OpTypeManager.getMaxLength(OpType.TEXT);
         if (description.length() > maxLen) {
            description = description.substring(0, maxLen - 1);
         }
         OpGanttValidator.setDescription(activityRow, description);
         //attachments
         //mode
         //work phase begin - validation will take care of this
         //work phase end - validation will take care of this
         //work phase base effort - validation will take care of this
         //resource based efforts
         //priority
         //work records map
         //actual effort

         if (index == 0) {
            firstOutlineLevel = msTask.getOutlineLevel().intValue();
         }
         activityRow.setOutlineLevel(msTask.getOutlineLevel().intValue() - firstOutlineLevel);

         if (msTask.getMilestone()) {
            OpGanttValidator.setDuration(activityRow, 0);
         }

         //msTask.getExpanded() -- not imported

         //add to data set
         dataSet.addChild(activityRow);
         //save activity in map for predecessors-successors links
         activityMap.put(msTask.getUniqueID(), activityRow);

         if (finish == null) {
            validator.updateDuration(activityRow, OpGanttValidator.getDuration(activityRow));
         }
         index++;
      }

      //successors - predecessors links.
      for (int i = 1; i < msTasks.size(); i++) {
         Task msTask = (Task) msTasks.get(i);
         XComponent activity = (XComponent) activityMap.get(msTask.getUniqueID());
         List successors = msTask.getSuccessors();
         if (successors != null) {
            for (Iterator iterator = successors.iterator(); iterator.hasNext();) {
               Relation link = (Relation) iterator.next();
               Integer id = link.getTaskUniqueID();
               if (id == null) {
                  id = link.getTaskID();
               }
               XComponent succActivity = (XComponent) activityMap.get(id);
               OpGanttValidator.addSuccessor(activity, succActivity.getIndex());
               OpGanttValidator.addPredecessor(succActivity, activity.getIndex());
               logger.info("Created link between activity \"" + OpGanttValidator.getName(activity) +
                    "\" and \"" + OpGanttValidator.getName(succActivity) + "\"");
            }
         }

         List predecessors = msTask.getPredecessors();
         if (predecessors != null) {
            for (Iterator iterator = predecessors.iterator(); iterator.hasNext();) {
               Relation link = (Relation) iterator.next();
               Integer id = link.getTaskUniqueID();
               if (id == null) {
                  id = link.getTaskID();
               }
               XComponent predActivity = (XComponent) activityMap.get(id);
               OpGanttValidator.addPredecessor(activity, predActivity.getIndex());
               OpGanttValidator.addSuccessor(predActivity, activity.getIndex());
               logger.info("Created link between activity \"" + OpGanttValidator.getName(predActivity) + "\" and \""
                    + OpGanttValidator.getName(activity) + "\"");
            }
         }
      }

      //set also the visual resources 
      Boolean showHours = Boolean.valueOf(OpSettings.get(OpSettings.SHOW_RESOURCES_IN_HOURS));
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         OpGanttValidator.updateVisualResources(dataRow, showHours.booleanValue(), resourceAvailability);
      }

      //validation after import
      if (validator.detectLoops()) {
         throw new OpActivityLoopException(OpGanttValidator.LOOP_EXCEPTION);
      }
      validator.validateDataSet();
      return dataSet;
   }


   public static String exportActivities(String fileName, OutputStream destinationFile, XComponent dataSet, XLocale xlocale)
        throws IOException {

      //adjust the destination name file
      int endIndex = fileName.lastIndexOf(".");
      if (endIndex == -1) {
         fileName += ".mpx";
      }
      else {
         fileName = fileName.substring(0, endIndex) + ".mpx";
      }

      ProjectFile file = new ProjectFile();
      MPXWriter writer = new MPXWriter();
      writer.setLocale(new Locale(xlocale.getID()));

      OpGanttValidator validator = ((OpGanttValidator) dataSet.validator());

      file.setAutoTaskID(true);
      file.setAutoTaskUniqueID(true);
      file.setAutoResourceID(true);
      file.setAutoResourceUniqueID(true);
      file.setAutoOutlineLevel(true);
      file.setAutoOutlineNumber(true);
      file.setAutoWBS(true);
      file.setAutoCalendarUniqueID(true);

      //holidays
      ProjectCalendar calendar;
      try {
         calendar = file.addDefaultBaseCalendar();
         SortedSet holidays = XCalendar.getDefaultCalendar().getHolidays();
         for (Iterator iterator = holidays.iterator(); iterator.hasNext();) {
            ProjectCalendarException exception = calendar.addCalendarException();
            Date holiday = (Date) iterator.next();
            exception.setFromDate(holiday);
            exception.setToDate(holiday);
            exception.setWorking(false);
         }
      }
      catch (MPXJException e) {
         logger.warn(" Failed to export project plan [add holidays]", e);
         throw new IOException("Unable to write to file ");// + destinationFile);
      }

      ProjectHeader header = file.getProjectHeader();
      header.setStartDate(validator.getProjectStart());

      //add resources on project
      XComponent assignmentSet = validator.getAssignmentSet();
      Map resourceMap = new HashMap();
      for (int j = 0; j < assignmentSet.getChildCount(); j++) {
         XComponent row = (XComponent) assignmentSet.getChild(j);
         String resouceLocator = row.getStringValue();
         String resouceID = XValidator.choiceID(resouceLocator);
         String resourceName = XValidator.choiceCaption(resouceLocator);
         if (!resourceName.equals(OpGanttValidator.NO_RESOURCE_NAME)) {
            Resource resource = file.addResource();
            resource.setName(resourceName);
            resourceMap.put(resouceID, resource);
         }
      }

      //add activities
      int previousOutlineLevel = 0;
      XComponent superActivity = null;
      XComponent previousActivity = null;
      Stack superActivityStack = new Stack();
      Map activityMap = new HashMap();
      for (int i = 0; i < dataSet.getChildCount(); i++) {

         XComponent activity = (XComponent) dataSet.getChild(i);
         if (activity.getOutlineLevel() > previousOutlineLevel) {
            if (superActivity != null) {
               superActivityStack.push(superActivity);
            }
            superActivity = previousActivity;
         }
         else if (activity.getOutlineLevel() < previousOutlineLevel) {
            for (int k = 0; k < previousOutlineLevel - activity.getOutlineLevel(); k++) {
               if (superActivityStack.empty()) {
                  superActivity = null;
                  break;
               }
               else {
                  superActivity = (XComponent) (superActivityStack.pop());
               }
            }
         }

         Task superTask = (Task) activityMap.get(superActivity);
         Task msTask = addMSProjectActivity(file, activity, superTask, resourceMap);

         activityMap.put(activity, msTask);
         previousActivity = activity;
         previousOutlineLevel = previousActivity.getOutlineLevel();
      }

      //9) Predecessors - List
      //10) Successors - List
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent activity = (XComponent) dataSet.getChild(i);
         Task msTask = (Task) activityMap.get(activity);
         List successors = OpGanttValidator.getSuccessors(activity);
         for (Iterator iterator = successors.iterator(); iterator.hasNext();) {
            Integer succesorIndex = (Integer) iterator.next();
            XComponent successorActivity = (XComponent) dataSet.getChild(succesorIndex.intValue());
            Task succesorTask = (Task) activityMap.get(successorActivity);
            msTask.addSuccessor(succesorTask);
         }
      }

      //write to output file
      writer.write(file, destinationFile);
      return fileName;
   }

   /**
    * Add a new task to the project using the given information.
    *
    * @param projectFile   main ms project
    * @param activity      activity data row with activity information
    * @param superActivity the parent ms-task for this activity
    * @param resourceMap   the resource map, key = locatorID, value = ms resource
    * @return the newly created ms task
    */
   private static Task addMSProjectActivity(ProjectFile projectFile, XComponent activity, Task superActivity, Map resourceMap) {

      Task task;
      if (superActivity == null) {
         task = projectFile.addTask();
      }
      else {
         task = superActivity.addTask();
      }

      //1) Name
      task.setName(OpGanttValidator.getName(activity));
      //2) Type - byte
      byte type = OpGanttValidator.getType(activity);
      if (type == OpGanttValidator.MILESTONE) {
         task.setMilestone(true);
      }
      //3) Category - String - not exported
      //4) %Complete - double
      double complete = OpGanttValidator.getComplete(activity);
      task.setPercentageComplete(new Double(complete));
      //5) Start - Date
      Date start = OpGanttValidator.getStart(activity);
      task.setStart(start);
      //6) End - Date
      Date end = OpGanttValidator.getEnd(activity);
      task.setFinish(end);

      int workingDays;
      //8) Base Effort - double
      double effort = OpGanttValidator.getBaseEffort(activity);
      workingDays = (int) (effort / XCalendar.getDefaultCalendar().getWorkHoursPerDay());
      task.setBaselineWork(Duration.getInstance(workingDays, TimeUnit.DAYS));

      //7) Duration - double (wh)
      double duration = OpGanttValidator.getDuration(activity);
      workingDays = (int) (duration / XCalendar.getDefaultCalendar().getWorkHoursPerDay());
      task.setDuration(Duration.getInstance(workingDays, TimeUnit.DAYS));

      //11) Resources - List
      List resources = OpGanttValidator.getResources(activity);
      int i = 0;
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         String locator = (String) iterator.next();
         String id = XValidator.choiceID(locator);
         String caption = XValidator.choiceCaption(locator);
         String resName = OpGanttValidator.getResourceName(caption, "%");
         if (!resName.equals(OpGanttValidator.NO_RESOURCE_NAME)) {
            Resource resource = (Resource) resourceMap.get(id);
            ResourceAssignment assignments = task.addResourceAssignment();
            assignments.setResourceID(resource.getID());
            Double resourceBaseEffort = (Double) OpGanttValidator.getResourceBaseEfforts(activity).get(i);
            Duration workDuration = Duration.getInstance(resourceBaseEffort.doubleValue(), TimeUnit.HOURS);
            assignments.setWork(workDuration);
            if (caption.length() > resName.length()) {
               double assigned = OpGanttValidator.percentageAssigned(locator);
               assignments.setUnits(new Double(assigned));
            }
            i++;
         }
      }

      //Costs are not exported!
      //12) Base personnel costs - double
      //13) Base travel costs - double
      //14) Base material costs - double
      //15) Base external costs - double
      //16) Base misc costs - double
      //17) Description - String
      task.setNotes(OpGanttValidator.getDescription(activity));

      //18) Attachments - List
      //19) Mode - byte
      //20) Work Phase begin - List (Date)
      //21) Work Phase end - List (Date)
      //22) Work Phase base efforts List (double)
      //23) Resource based efforts  List (double)
      //24) Priority (only for tasks)
      //25) Work records map - HashMap
      //26) Actual effort - double

      return task;
   }

   private static double convertMsDurationToWorkingHours(Duration duration) {
      double oppDuration;
      if (duration == null) {
         return 0;
      }
      double msDuration = duration.getDuration();
      TimeUnit timeUnit = duration.getUnits();
      if (timeUnit == TimeUnit.HOURS) {
         oppDuration = msDuration;
      }
      else if (timeUnit == TimeUnit.DAYS) {
         oppDuration = msDuration * XCalendar.getDefaultCalendar().getWorkHoursPerDay();
      }
      else {
         //if (timeUnit == TimeUnit.WEEKS)
         oppDuration = msDuration * XCalendar.getDefaultCalendar().getWorkHoursPerWeek();
      }
      return oppDuration;
   }

}
