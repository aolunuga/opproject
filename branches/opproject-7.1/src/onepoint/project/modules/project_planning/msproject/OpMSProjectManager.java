/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.msproject;

import net.sf.mpxj.*;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXReader;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.mspdi.MSPDIReader;
import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTypeManager;
import onepoint.project.modules.project.*;
import onepoint.project.modules.project.components.OpActivityLoopException;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project.components.OpIncrementalValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.resource.XLocale;
import onepoint.util.XCalendar;

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
   private static final String MPP_FORMAT = "MPP";
   private static final String MPT_FORMAT = "MPT";
   private static final String MPX_FORMAT = "MPX";
   private static final String MSPDI_FORMAT = "XML";

   //utility class
   private OpMSProjectManager() {
   }

   /**
    * Fills the given data set with infor from the previously loaded ms project file.
    * @param fileName 
    *
    * @return a dataset with all the saved activities
    */
   public static XComponent importActivities(OpBroker broker, String fileName, InputStream sourceFile, OpProjectPlan projectPlan, XLocale xlocale)
        throws IOException {

      //read the tasks from the input stream
      List<Task> msTasks = readMsTasks(fileName, sourceFile, xlocale);

      //map: <name>,<locator>
      Map projectResources = new HashMap();
      Map resourceAvailability = new HashMap();
      for (OpProjectNodeAssignment assignment : projectPlan.getProjectNode().getAssignments()) {
         OpResource resource = assignment.getResource();
         String resourceName = resource.getName();
         String resourceLocator = XValidator.choice(resource.locator(), resourceName);
         resourceAvailability.put(resource.locator(), new Double(resource.getAvailable()));
         projectResources.put(resourceName, resourceLocator);
      }

      //create validator for the data-set validation
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpProjectNode projectNode = projectPlan.getProjectNode();
      HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);
      OpIncrementalValidator validator = new OpProjectPlanValidator(projectPlan).createValidator(resources);
      validator.setDataSet(dataSet);

      //populate the activity set
      Map<Integer, XComponent> uniqueIdMap = new HashMap<Integer, XComponent>();
      Map<Integer, XComponent> idMap = new HashMap<Integer, XComponent>();
      populateActivitySet(msTasks, validator, projectResources, dataSet, uniqueIdMap, idMap);
      linkActivities(msTasks, uniqueIdMap, idMap);
      setVisualResources(dataSet, resourceAvailability);

      //validation after import
      if (validator.detectLoops()) {
         throw new OpActivityLoopException(OpGanttValidator.LOOP_EXCEPTION);
      }
      validator.validateEntireDataSet();
      return dataSet;
   }

   /**
    * Sets the visual resources for the imported data-set.
    * @param dataSet a <code>XComponent(DATA_SET)</code> the newly imported data-set.
    * @param resourceAvailability a <code>Map</code> which contains resource availabilities.
    */
   private static void setVisualResources(XComponent dataSet, Map resourceAvailability) {
      //set also the visual resources
      Boolean showHours = Boolean.valueOf(OpSettingsService.getService().get(OpSettings.SHOW_RESOURCES_IN_HOURS));
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         OpGanttValidator.updateVisualResources(dataRow, showHours.booleanValue(), resourceAvailability);
      }
   }

   /**
    * Creates the successor/predecessor links as found in the ms project plan.
    * @param msTasks a <code>List(Task)</code>.
    * @param uniqueIdMap a <code>Map(Integer, XComponent)</code>.
    * @param idMap 
    */
   private static void linkActivities(List<Task> msTasks, Map<Integer, XComponent> uniqueIdMap, Map<Integer, XComponent> idMap) {
      //successors - predecessors links.
      for (Task msTask : msTasks) {
         XComponent activity = (XComponent) uniqueIdMap.get(msTask.getUniqueID());
         List successors = msTask.getSuccessors();
         if (successors != null) {
            for (Iterator iterator = successors.iterator(); iterator.hasNext();) {
               Relation link = (Relation) iterator.next();
               Integer uniqueId = link.getTaskUniqueID();
               Integer id = link.getTaskID();
               
               XComponent succActivity;
               if (uniqueId != null) {
                  succActivity = (XComponent) uniqueIdMap.get(uniqueId);
               }
               else {
                  succActivity = (XComponent) idMap.get(id);
               }
               if (succActivity != null) {
                  OpGanttValidator.addSuccessor(activity, succActivity.getIndex());
                  OpGanttValidator.addPredecessor(succActivity, activity.getIndex());
                  logger.info("Created link between activity \"" + OpGanttValidator.getName(activity) +
                        "\" and \"" + OpGanttValidator.getName(succActivity) + "\"");
               }
               else {
                  logger.error("Could not create link between activity \"" + OpGanttValidator.getName(activity) +
                        "\" and \"" + OpGanttValidator.getName(succActivity) + "\"");
               }
            }
         }

         List predecessors = msTask.getPredecessors();
         if (predecessors != null) {
            for (Iterator iterator = predecessors.iterator(); iterator.hasNext();) {
               Relation link = (Relation) iterator.next();
               
               Integer uniqueId = link.getTaskUniqueID();
               Integer id = link.getTaskID();
               
               XComponent predActivity;
               if (uniqueId != null) {
                  predActivity = (XComponent) uniqueIdMap.get(uniqueId);
               }
               else {
                  predActivity = (XComponent) idMap.get(id);
               }
               if (predActivity != null) {
                  OpGanttValidator.addPredecessor(activity, predActivity.getIndex());
                  OpGanttValidator.addSuccessor(predActivity, activity.getIndex());
                  logger.info("Created link between activity \"" + OpGanttValidator.getName(predActivity) + "\" and \""
                        + OpGanttValidator.getName(activity) + "\"");
               }
               else {
                  logger.error("Could not create link between activity \"" + OpGanttValidator.getName(predActivity) +
                        "\" and \"" + OpGanttValidator.getName(activity) + "\"");
               }
            }
         }
      }
   }

   /**
    * Creates data rows with activities from the given list of tasks.
    * @param msTasks a <code>List(Task)</code>.
    * @param validator a <code>OpGanttValidator</code> used by the validation process.
    * @param projectResources a <code>Map</code> of project resources.
    * @param dataSet a <code>XComponent(DATA_SET)</code>.
    * @return a <code>Map</code> of <code>(Integer, XComponent)</code> values.
    */
   private static void populateActivitySet(List<Task> msTasks,
        OpGanttValidator validator, Map projectResources, XComponent dataSet, 
        Map<Integer, XComponent> uniqueIdMap, Map<Integer, XComponent> idMap) {
      int index = 0;
      int firstOutlineLevel = 0;

      for (Task msTask : msTasks) {
         if (msTask.getOutlineNumber() == null) { // may happen on import of xml
            continue;
         }
         XComponent activityRow = validator.newDataRow();

         OpGanttValidator.setName(activityRow, msTask.getName());
         OpGanttValidator.setType(activityRow, OpGanttValidator.STANDARD);
         //category = null
         if (!validator.getProgressTracked()) {
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
         if (description.length() >  OpTypeManager.MAX_TEXT_LENGTH) {
            description = description.substring(0, OpTypeManager.MAX_TEXT_LENGTH - 1);
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
         uniqueIdMap.put(msTask.getUniqueID(), activityRow);
         idMap.put(msTask.getID(), activityRow);
         if (finish == null) {
            validator.updateDuration(activityRow, OpGanttValidator.getDuration(activityRow));
         }
         index++;
      }
   }

   /**
    * Reads from the given input file the list of tasks as exported in the microsoft project file.
    * @param fileName 
    *
    * @param sourceFile an <code>InputStream</code> for a MS Project file.
    * @return a <code>List(Task)</code>.
    */
   private static List<Task> readMsTasks(String fileName, InputStream sourceFile, XLocale applicationLocale)
        throws IOException {
      ProjectFile msProject = null;
      //for mpp file format the first activity is the name of the project
      String extension = "mpp";
      int suffixPos = fileName.lastIndexOf('.');
      if (suffixPos >= 0) {
         extension = fileName.substring(suffixPos+1).toUpperCase();;
      }
      
      boolean removeFirstActivity = false;
      if ((MPP_FORMAT.equals(extension)) || (MPT_FORMAT.equals(extension))) {
         try {
            msProject = new MPPReader().read(sourceFile);
            removeFirstActivity = true;
         }
         catch (MPXJException e) {
            throw new IOException("Unable to load the file " + fileName);
         }         
      }
      
      else if (MPX_FORMAT.equals(extension)) {
         // try set language
         try {
            MPXReader reader = new MPXReader();
            reader.setLocale(new Locale(applicationLocale.getID()));
            msProject = reader.read(sourceFile);
         }
         catch (MPXJException e1) {
            Locale[] all_locales = {Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH, Locale.ITALIAN,
                 new Locale("pt"), new Locale("sv")};
            boolean read = false;
            for (int count = 0; count < all_locales.length; count++) {
               try {
                  sourceFile.reset();
                  MPXReader reader = new MPXReader();
                  reader.setLocale(all_locales[count]);
                  msProject = reader.read(sourceFile);
                  read = true;
                  break;
               }
               catch (MPXJException e2) {
               }
            }
            if (!read) {
               throw new IOException("Unable to load the file " + fileName);
            }
         }
      }
      else if(MSPDI_FORMAT.equalsIgnoreCase(extension)) {
         try {
            MSPDIReader reader = new MSPDIReader();
            msProject = reader.read(sourceFile);
         }
         catch (MPXJException exc) {
            throw new IOException("Unable to load the file " + fileName);
         }
      }
      else {
         throw new IOException("Unable to load the file " + fileName);         
      }
      
      List<Task> msTasks = new ArrayList<Task>();
      if (msProject != null) {
         msTasks.addAll(msProject.getAllTasks());
         if (removeFirstActivity) {
            Integer integer = msTasks.get(0).getUniqueID();
            if ((integer == null) || (integer.intValue() == 0)) {
               msTasks.remove(0);
            }
         }
      }
      return msTasks;
   }

   public static String exportActivities(String fileName, OutputStream destinationFile, XComponent dataSet,
        XLocale xlocale, OpProjectNode projectNode)
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
      Map resourceMap = new HashMap();
      for (OpProjectNodeAssignment projectAssignment : projectNode.getAssignments()) {
         OpResource projectResource = projectAssignment.getResource();
         String resouceLocator = projectResource.locator();
         String resouceID = XValidator.choiceID(resouceLocator);
         Resource resource = file.addResource();
         resource.setName(projectResource.getName());
         resourceMap.put(resouceID, resource);
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
            Relation rel = succesorTask.addPredecessor(msTask);
            succesorTask.setConstraintType(ConstraintType.AS_SOON_AS_POSSIBLE);            
//            Relation rel = msTask.addSuccessor(succesorTask);
            if (succesorTask.getMilestone()) {
               rel.setType(RelationType.FINISH_START);
            }
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
    * @param resourceMap   the resource map key = locatorID, value = ms resource
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
      task.setConstraintType(ConstraintType.MUST_START_ON); // will be changed if we find a predecessor

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
//      task.setActualStart(start);

      //6) End - Date
      Date end = OpGanttValidator.getEnd(activity);
      task.setFinish(end);

      int workingDays;
      //8) Base Effort - double
      double effort = OpGanttValidator.getBaseEffort(activity);
      task.setBaselineWork(Duration.getInstance(effort, TimeUnit.HOURS));

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
