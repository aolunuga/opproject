/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.msproject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import net.sf.mpxj.ConstraintType;
import net.sf.mpxj.Duration;
import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ProjectHeader;
import net.sf.mpxj.Relation;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXReader;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.mspdi.MSPDIReader;
import net.sf.mpxj.mspdi.MSPDIWriter;
import net.sf.mpxj.writer.AbstractProjectWriter;
import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectNodeAssignment;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanValidator;
import onepoint.project.modules.project.components.OpActivityLoopException;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.OpProjectPlanningError;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectCalendar;
import onepoint.resource.XLocale;
import onepoint.service.server.XServiceException;

/**
 * Class that provides methods in order to transform from MsProject structure in OPProject structure
 *
 * @author : mihai.costin
 */
public class OpMSProjectManager {

   private static final XLog logger = XLogFactory.getLogger(OpMSProjectManager.class);
   private static final String MPP_FORMAT = "MPP";
   private static final String MPT_FORMAT = "MPT";
   private static final String MPX_FORMAT = "MPX";
   private static final String MSPDI_FORMAT = "XML";
   private static final String MS_PROJECT_NO_RESOURCE_NAME = "?";

   //utility class
   private OpMSProjectManager() {
   }

   /**
    * Fills the given data set with infor from the previously loaded ms project file.
    *
    * @param fileName
    * @return a dataset with all the saved activities
    */
   public static XComponent importActivities(OpProjectSession session, OpBroker broker, String fileName, InputStream sourceFile, OpProjectPlan projectPlan, XLocale xlocale)
        throws IOException {

      //read the tasks from the input stream
      ProjectFile msProject = init(fileName, sourceFile, xlocale);
//      java.util.Date start = msProject.getStartDate();
      List<Task> msTasks = readMsTasks(msProject, fileName, sourceFile, xlocale);
      
      // check a tasks is befor project start date
      Date projectStart = projectPlan.getStart();
      if (projectStart != null) {
         for (Task msTask : msTasks) {
            if (msTask.getOutlineNumber() == null) { // may happen on import of xml
               continue;
            }
            if (msTask.getOutlineLevel() == null) {
               continue;
            }
            Date start = new Date(msTask.getStart().getTime());
            if (start != null) {
               if (start.before(projectStart)) {
                  Map<String, Object> parameters = new HashMap<String, Object>();
                  parameters.put("project.start", projectStart);
                  parameters.put("activity.start", start);
                  throw new XServiceException(session.newError(OpProjectPlanningService.PLANNING_ERROR_MAP, OpProjectPlanningError.PROJECT_START_DATE_AFTER_ACTIVITY_DATE, parameters));
               }
            }
         }
      }
      
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
      OpGanttValidator validator = OpProjectPlanValidator.getInstance().createValidator(session, broker, projectPlan);
      validator.setDataSet(dataSet);

      //populate the activity set
      Map<Integer, XComponent> uniqueIdMap = new HashMap<Integer, XComponent>();
      Map<Integer, XComponent> idMap = new HashMap<Integer, XComponent>();
      populateActivitySet(msProject, msTasks, validator, projectResources, dataSet, uniqueIdMap, idMap);
      linkActivities(msProject, msTasks, uniqueIdMap, idMap);
      setVisualResources(broker, dataSet, resourceAvailability, validator);

      //validation after import
      if (validator.detectLoops()) {
         throw new OpActivityLoopException(OpGanttValidator.LOOP_EXCEPTION);
      }
      validator.validateEntireDataSet();
      return dataSet;
   }

   /**
    * Sets the visual resources for the imported data-set.
    *
    * @param dataSet              a <code>XComponent(DATA_SET)</code> the newly imported data-set.
    * @param resourceAvailability a <code>Map</code> which contains resource availabilities.
    */
   private static  void setVisualResources(OpBroker broker, XComponent dataSet, Map resourceAvailability, OpGanttValidator validator) {
      //set also the visual resources
      Boolean showHours = Boolean.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.SHOW_RESOURCES_IN_HOURS));
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         validator.updateResourceVisualization(dataRow);
      }
   }

   /**
    * Creates the successor/predecessor links as found in the ms project plan.
    *
    * @param msProject
    * @param msTasks     a <code>List(Task)</code>.
    * @param uniqueIdMap a <code>Map(Integer, XComponent)</code>.
    * @param idMap
    */
   private static void linkActivities(ProjectFile msProject, List<Task> msTasks, Map<Integer, XComponent> uniqueIdMap, Map<Integer, XComponent> idMap) {
      //successors - predecessors links.
      for (Task msTask : msTasks) {
         XComponent activity = (XComponent) uniqueIdMap.get(msTask.getUniqueID());

         List<Relation> successors = msTask.getSuccessors();
         
         if (successors != null) {
            for (Iterator iterator = successors.iterator(); iterator.hasNext();) {
               Relation link = (Relation) iterator.next();
               Duration lag = link.getDuration();
               Integer uniqueId = link.getTaskUniqueID();
               Integer id = link.getTaskUniqueID();

               XComponent succActivity;
               if (uniqueId != null) {
                  succActivity = (XComponent) uniqueIdMap.get(uniqueId);
               }
               else {
                  succActivity = (XComponent) idMap.get(id);
               }
               if (succActivity != null) {
                  if (RelationType.START_FINISH.equals(link.getType())
                        || RelationType.FINISH_START.equals(link.getType())
                        || RelationType.START_START.equals(link.getType())
                        || RelationType.FINISH_FINISH.equals(link.getType())) {
                     if (!OpEnvironmentManager.isOpenEdition() && lag != null) {
                        double lagVal = lag.convertUnits(TimeUnit.DAYS, msProject.getProjectHeader()).getDuration();
                        XComponent leadActivity = null;
                        XComponent followUpActivity = null;
                        if (RelationType.START_FINISH.equals(link.getType())) {
                           leadActivity = activity;
                        }
                        else if (RelationType.START_START.equals(link.getType())) {
                           leadActivity = succActivity;
                        }
                        else if (RelationType.FINISH_FINISH.equals(link.getType())) {
                           followUpActivity = activity;
                        }
                        else if (RelationType.FINISH_START.equals(link.getType())) {
                           leadActivity = succActivity;
                        }
                        if (leadActivity != null && lagVal != 0d) {
                           if (OpGanttValidator.getLeadTime(leadActivity) != 0) {
                              logger.error("multiple dependencies with different lag times - this cannot be handled in Onepoint Project");
                           }
                           OpGanttValidator.setLeadTime(leadActivity, lagVal);
                        }
                        if (followUpActivity != null && lagVal != 0d) {
                           if (OpGanttValidator.getFollowUpTime(followUpActivity) != 0) {
                              logger.error("multiple dependencies with different lag times - this cannot be handled in Onepoint Project");
                           }
                           OpGanttValidator.setFollowUpTime(followUpActivity, lagVal);
                        }
                     }

                     int relationType = getRelationType(link.getType());
                     if (!OpEnvironmentManager.isOpenEdition() || relationType == OpGanttValidator.DEP_END_START) {
                        OpGanttValidator.addSuccessor(activity, succActivity.getIndex(), relationType);
                        OpGanttValidator.addPredecessor(succActivity, activity.getIndex(), relationType);
                        logger.info("Created link between activity \"" + (activity != null ? OpGanttValidator.getName(activity) : "<null>") +
                              "\" and \"" + OpGanttValidator.getName(succActivity) + "\"");
                     }
                  }
                  else {
                     logger.error("Unsupported Relation Type: " + link.getType());
                  }
               }
               else {
                  logger.error("Could not create link between activity \"" + (activity != null ? OpGanttValidator.getName(activity) : "<null>") +
                       "\" and \"" + (succActivity != null ? OpGanttValidator.getName(succActivity) : "<null>") + "\"");
               }
            }
         }

         List predecessors = msTask.getPredecessors();
         if (predecessors != null) {
            for (Iterator iterator = predecessors.iterator(); iterator.hasNext();) {
               Relation link = (Relation) iterator.next();
//               link.getType() == RelationType.FINISH_START;
               Duration lag = link.getDuration();

               Integer uniqueId = link.getTaskUniqueID();
               Integer id = link.getTaskUniqueID();

               XComponent predActivity;
               if (uniqueId != null) {
                  predActivity = (XComponent) uniqueIdMap.get(uniqueId);
               }
               else {
                  predActivity = (XComponent) idMap.get(id);
               }
               if (predActivity != null) {
                  if (RelationType.START_FINISH.equals(link.getType())
                        || RelationType.FINISH_START.equals(link.getType())
                        || RelationType.START_START.equals(link.getType())
                        || RelationType.FINISH_FINISH.equals(link.getType())) {
                     if (!OpEnvironmentManager.isOpenEdition() && lag != null) {
                        double lagVal = lag.convertUnits(TimeUnit.DAYS, msProject.getProjectHeader()).getDuration();
                        XComponent leadActivity = null;
                        XComponent followUpActivity = null;
                        if (RelationType.START_FINISH.equals(link.getType())) {
                           leadActivity = predActivity;
                        }
                        else if (RelationType.START_START.equals(link.getType())) {
                           leadActivity = activity;
                        }
                        else if (RelationType.FINISH_FINISH.equals(link.getType())) {
                           followUpActivity = predActivity;
                        }
                        else if (RelationType.FINISH_START.equals(link.getType())) {
                           leadActivity = activity;
                        }
                        if (leadActivity != null && lagVal != 0d) {
                           if (OpGanttValidator.getLeadTime(leadActivity) != 0) {
                              logger.error("multiple dependencies with different lag times - this cannot be handled in Onepoint Project");
                           }
                           OpGanttValidator.setLeadTime(leadActivity, lagVal);
                        }
                        if (followUpActivity != null && lagVal != 0d) {
                           if (OpGanttValidator.getFollowUpTime(followUpActivity) != 0) {
                              logger.error("multiple dependencies with different lag times - this cannot be handled in Onepoint Project");
                           }
                           OpGanttValidator.setFollowUpTime(followUpActivity, lagVal);
                        }
                     }
                     int relationType = getRelationType(link.getType());
                     if (!OpEnvironmentManager.isOpenEdition() || relationType == OpGanttValidator.DEP_END_START) {
                        OpGanttValidator.addPredecessor(activity, predActivity.getIndex(), relationType);
                        OpGanttValidator.addSuccessor(predActivity, activity.getIndex(), relationType);
                        logger.info("Created link between activity \"" + OpGanttValidator.getName(predActivity) + "\" and \""
                              + (activity != null ? OpGanttValidator.getName(activity) : "<null>") + "\"");
                     }
                  }
                  else {
                     logger.error("Unsupported Relation Type: " + link.getType());
                  }
               }
               else {
                  logger.error("Could not create link between activity \"" + (predActivity != null ? OpGanttValidator.getName(predActivity) : "<null>") +
                       "\" and \"" + (activity != null ? OpGanttValidator.getName(activity) : "<null>") + "\"");
               }
            }
         }
      }
   }

   /**
    * @param type
    * @return
    * @pre
    * @post
    */
   private static int getRelationType(RelationType type) {
      if (type.equals(RelationType.START_FINISH)) {
         return OpGanttValidator.DEP_START_END;
      }
      if (type.equals(RelationType.START_START)) {
         return OpGanttValidator.DEP_START_START;
      }
      if (type.equals(RelationType.FINISH_FINISH)) {
         return OpGanttValidator.DEP_END_END;
      }
      return OpGanttValidator.DEP_END_START;
   }

   /**
    * @param type
    * @return
    * @pre
    * @post
    */
   private static RelationType getRelationType(int type) {
      if (OpGanttValidator.DEP_START_END == type) {
         return RelationType.START_FINISH;
      }
      if (OpGanttValidator.DEP_START_START == type) {
         return RelationType.START_START;
      }
      if (OpGanttValidator.DEP_END_END == type) {
         return RelationType.FINISH_FINISH;
      }
      return RelationType.FINISH_START;
   }

   /**
    * Creates data rows with activities from the given list of tasks.
    *
    * @param msProject
    * @param msTasks          a <code>List(Task)</code>.
    * @param validator        a <code>OpGanttValidator</code> used by the validation process.
    * @param projectResources a <code>Map</code> of project resources.
    * @param dataSet          a <code>XComponent(DATA_SET)</code>.
    * @return a <code>Map</code> of <code>(Integer, XComponent)</code> values.
    */
   private static void populateActivitySet(ProjectFile msProject, List<Task> msTasks,
        OpGanttValidator validator, Map projectResources, XComponent dataSet,
        Map<Integer, XComponent> uniqueIdMap, Map<Integer, XComponent> idMap) {
      int index = 0;
      int firstOutlineLevel = 0;

      for (Task msTask : msTasks) {
         if (msTask.getOutlineNumber() == null) { // may happen on import of xml
            continue;
         }
         if (msTask.getOutlineLevel() == null) {
            continue;
         }
         XComponent activityRow = validator.newDataRow();

         OpGanttValidator.setName(activityRow, msTask.getName());
         // TODO: feasible???
         validator.updateType(activityRow, OpGanttValidator.STANDARD);
         //category = null
         if (!validator.isProgressTracked()) {
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

         // TODO: check, why not use msTask.getBaselineDuration(); (see getBaselineWork() below)
         Duration msDuration = msTask.getDuration();
         double oppActivityDuration = convertMsDurationToWorkingHours(msDuration, true, validator.getCalendar());
         OpGanttValidator.setDuration(activityRow, oppActivityDuration);

         Duration msBaseLineWork = msTask.getBaselineWork();
         //resources
         double activityEffort = 0d;
         List assignments = msTask.getResourceAssignments();
         if (!assignments.isEmpty()) {
            for (Iterator iterator = assignments.iterator(); iterator.hasNext();) {
               ResourceAssignment assignment = (ResourceAssignment) iterator.next();
               Resource resource = assignment.getResource();
               String name = resource.getName();
               double assignmentWork = convertMsDurationToWorkingHours(assignment.getWork(), false, validator.getCalendar());
               activityEffort += assignmentWork;
               double assignmentActualWork = 0;
               if (!validator.getProgressTracked()) {
                  assignmentActualWork = OpGanttValidator.calculateActualEffort(assignmentWork, 0d, msTask.getPercentageComplete().doubleValue());
               }
//               if (name.equals(MS_PROJECT_NO_RESOURCE_NAME) || projectResources.keySet().contains(name)) {
               if (projectResources.keySet().contains(name)) {
                  String locator = (String) projectResources.get(name);
                  String id = "-1";
                  if (locator != null) {
                     id = XValidator.choiceID(locator);
                  }
                  double percentage = (assignmentWork / oppActivityDuration) * 100d;
                  if (name.equals(MS_PROJECT_NO_RESOURCE_NAME)) {
                     name = "?";
//                     percentage = oppActivityDuration/8
                  }
                  String oppResource = XValidator.choice(id, name + " " + percentage + "%");
                  OpGanttValidator.addResource(activityRow, oppResource);
//                  OpGanttValidator.setResourceBaseEffort(activityRow, locator, assignment.getWork().getDuration());//assignment.getBaseEffort());
                  logger.info("Resource \"" + name + "\" has been added to the project on activity " + msTask.getName());
               }
               else {
                  logger.warn("Resource \"" + name + "\" won't be added to the project. It is not assigned to the project plan.");
               }
            }
         }
         else {
            logger.warn("No Assignment found for  \"" + msTask.getName() + "\"");
         }
         // TODO: check the following... (for now, seems to be a good compromise ;-)
         double activityBaseEffort = convertMsDurationToWorkingHours(msTask.getBaselineWork(), false, validator.getCalendar());
         double actualEffort = 0;

         activityBaseEffort = activityEffort > activityBaseEffort ? activityEffort : activityBaseEffort;
         OpGanttValidator.setBaseEffort(activityRow, activityBaseEffort);

         validator.updateResources(activityRow);
         
         if (!validator.getProgressTracked()) {
            actualEffort = OpGanttValidator.calculateActualEffort(activityBaseEffort, 0d, msTask.getPercentageComplete().doubleValue());
            OpGanttValidator.setActualEffort(activityRow, actualEffort);
         }

         //personnel costs  -- not imported (will be calculated by the validator)

         //costs are not imported
         //travel costs
         //material costs
         //misc costs

         String description = msTask.getNotes();
         if (description.length() > OpTypeManager.MAX_TEXT_LENGTH) {
            description = description.substring(0, OpTypeManager.MAX_TEXT_LENGTH - 1);
         }
         OpGanttValidator.setDescription(activityRow, description);
         //attachments
         //mode

         // splits
         List splits = msTask.getSplits();
         if (splits != null) {
            Iterator iter = splits.iterator();
            Double wbStart = null;
            boolean activeBreak = false;
            SortedMap<Double, Map<String, Object>> workBreaks = new TreeMap<Double, Map<String, Object>>();
            double offset = 0;
            while (iter.hasNext()) {
               Duration splitDuration = (Duration) iter.next();
               Duration days = splitDuration.convertUnits(TimeUnit.DAYS, msProject.getProjectHeader());
               if (!activeBreak) {
                  wbStart = new Double(days.getDuration());
                  activeBreak = true;
               }
               else {
                  Double wbDuration = new Double(days.getDuration() - wbStart);
                  Map<String, Object> wb = new HashMap<String, Object>();
//                  wb.put(OpGanttValidator.WORK_BREAK_LOCATOR, ??);
                  wb.put(OpGanttValidator.WORK_BREAK_START, wbStart-offset);
                  wb.put(OpGanttValidator.WORK_BREAK_DURATION, wbDuration);
                  workBreaks.put(wbStart-offset, wb);
                  offset += wbDuration;
                  activeBreak = false;
               }
               //SortedMap<Double fromBeginDerActivity, Map<String key, Object>> workBreak = OpGanttValidator.getWorkBreaks(activity);
               //keys: duration, start, locator

//               System.err.println("days: "+days);
            }
            OpGanttValidator.setWorkBreaks(activityRow, workBreaks);
         }

         // timephased data
         //work phase begin - validation will take care of this
         //work phase end - validation will take care of this
         //work phase base effort - validation will take care of this
         //resource based efforts
         //priority
         //work records map
         //actual effort

         int outlineLevel = msTask.getOutlineLevel().intValue();
         if (index == 0) {
            firstOutlineLevel = outlineLevel;
         }

         activityRow.setOutlineLevel(outlineLevel - firstOutlineLevel);

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
            validator.updateDuration(activityRow, OpGanttValidator.getDuration(activityRow), true);
         }
         index++;
      }
   }

   /**
    * Reads from the given input file the list of tasks as exported in the microsoft project file.
    *
    * @param msProject
    * @param fileName
    * @param sourceFile an <code>InputStream</code> for a MS Project file.
    * @return a <code>List(Task)</code>.
    */
   private static List<Task> readMsTasks(ProjectFile msProject, String fileName, InputStream sourceFile, XLocale applicationLocale)
        throws IOException {
      boolean removeFirstActivity = false;
      String extension = "mpp";
      int suffixPos = fileName.lastIndexOf('.');
      if (suffixPos >= 0) {
         extension = fileName.substring(suffixPos + 1).toUpperCase();
      }
      if ((MPP_FORMAT.equals(extension)) || (MPT_FORMAT.equals(extension)) || (MSPDI_FORMAT.equals(extension))) {
         removeFirstActivity = true;
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

   /**
    * @param fileName
    * @param sourceFile
    * @param applicationLocale
    * @return
    * @throws IOException
    * @pre
    * @post
    */
   private static ProjectFile init(String fileName, InputStream sourceFile,
        XLocale applicationLocale)
        throws IOException {
      ProjectFile msProject = null;
//      msProject.getProjectHeader()
      //for mpp file format the first activity is the name of the project
      String extension = "mpp";
      int suffixPos = fileName.lastIndexOf('.');
      if (suffixPos >= 0) {
         extension = fileName.substring(suffixPos + 1).toUpperCase();
      }

      if ((MPP_FORMAT.equals(extension)) || (MPT_FORMAT.equals(extension))) {
         try {
            msProject = new MPPReader().read(sourceFile);
         }
         catch (MPXJException e) {
        	 e.printStackTrace();
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
      else if (MSPDI_FORMAT.equalsIgnoreCase(extension)) {
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
      return msProject;
   }

   public static String exportActivities(OpProjectSession session, String fileName, OutputStream destinationFile, XComponent dataSet,
        XLocale xlocale, OpProjectNode projectNode)
        throws IOException {

      // adjust the destination name file
      int endIndex = fileName.lastIndexOf(".");
      if (endIndex == -1) {
         fileName += ".mpx";
      }

      ProjectFile file = new ProjectFile();
      AbstractProjectWriter writer;
      if (fileName.endsWith(".xml")) {
         writer = new MSPDIWriter();
      }
      else {
         writer = new MPXWriter();
         ((MPXWriter) writer).setLocale(xlocale.getLocale());
      }

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
//      try {
      calendar = file.addDefaultBaseCalendar();
      OpProjectCalendar opCal = validator.getCalendar();
      SortedMap holidays = opCal.getHolidays();
      for (Iterator iterator = holidays.keySet().iterator(); iterator.hasNext();) {
         ProjectCalendarException exception = calendar.addCalendarException();
         Date holiday = (Date) iterator.next();
         exception.setFromDate(holiday);
         exception.setToDate(holiday);
         exception.setWorking(false);
      }
//      }
//      catch (MPXJException e) {
//         logger.warn(" Failed to export project plan [add holidays]", e);
//         throw new IOException("Unable to write to file ");// + destinationFile);
//      }

      ProjectHeader header = file.getProjectHeader();
      header.setStartDate(validator.getProjectStart());

      //add resources on project
      
      Map resourceMap = new HashMap();
      Resource resource = file.addResource();
      resource.setName(MS_PROJECT_NO_RESOURCE_NAME);
      resourceMap.put("-1", resource);
      for (OpProjectNodeAssignment projectAssignment : projectNode.getAssignments()) {
         OpResource projectResource = projectAssignment.getResource();
         String resouceLocator = projectResource.locator();
         String resouceID = XValidator.choiceID(resouceLocator);
         String resName = projectResource.getName();
//         if (resouceID.equals(OpGanttValidator.NO_RESOURCE_NAME)) {
//            resName = MS_PROJECT_NO_RESOURCE_NAME;
//         }
         resource = file.addResource();
         resource.setName(resName);
         resourceMap.put(resouceID, resource);
      }
//      resou
      
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
         Task msTask = addMSProjectActivity(file, activity, superTask, resourceMap, ((OpGanttValidator)dataSet.validator()).getCalendar());

         activityMap.put(activity, msTask);
         previousActivity = activity;
         previousOutlineLevel = previousActivity.getOutlineLevel();
      }

      //9) Predecessors - List

      //10) Successors - List
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent activity = (XComponent) dataSet.getChild(i);
         Task msTask = (Task) activityMap.get(activity);
         SortedMap successors = OpGanttValidator.getSuccessors(activity);
         for (Iterator iterator = successors.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Integer succesorIndex = (Integer) entry.getKey();
            Map value = (Map) entry.getValue();
            int type = ((Integer) value.get(OpGanttValidator.DEP_TYPE)).intValue();

            XComponent successorActivity = (XComponent) dataSet.getChild(succesorIndex.intValue());
            Task succesorTask = (Task) activityMap.get(successorActivity);
            Relation rel = succesorTask.addPredecessor(msTask);
            succesorTask.setConstraintType(ConstraintType.AS_SOON_AS_POSSIBLE);
            RelationType relType;
            if (succesorTask.getMilestone()) {
               relType = RelationType.FINISH_START;
            }
            else {
               relType = getRelationType(type);
            }
            rel.setType(relType);

            // MS Project only knows of one (lag) time, no follow up times
            double lag = 0;
            if (RelationType.START_FINISH.equals(relType)) {
               lag += OpGanttValidator.getLeadTime(activity);
               lag += OpGanttValidator.getFollowUpTime(successorActivity);
            }
            else if (RelationType.START_START.equals(relType)){
               lag -= OpGanttValidator.getLeadTime(activity);
               lag += OpGanttValidator.getLeadTime(successorActivity);
            }
            else if (RelationType.FINISH_FINISH.equals(relType)) {
               lag += OpGanttValidator.getFollowUpTime(activity);
               lag -= OpGanttValidator.getFollowUpTime(successorActivity);
            }
            else if (RelationType.FINISH_START.equals(relType)) {
               lag += OpGanttValidator.getFollowUpTime(activity);
               lag += OpGanttValidator.getLeadTime(successorActivity);
            }
            
            if (lag != 0d) {
               rel.setDuration(Duration.getInstance(lag, TimeUnit.DAYS));
            }
            //SortedMap<Double fromBeginDerActivity, Map<String key, Object>> workBreak = OpGanttValidator.getWorkBreaks(activity);
            //keys: duration, start, locator
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
   private static Task addMSProjectActivity(ProjectFile projectFile, XComponent activity, Task superActivity, Map resourceMap, OpProjectCalendar calendar) {

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
      workingDays = (int) (duration / calendar.getWorkHoursPerDay());
      task.setDuration(Duration.getInstance(workingDays, TimeUnit.DAYS));

      //11) Resources - List
      List resources = OpGanttValidator.getResources(activity);
      int i = 0;
      for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
         String locator = (String) iterator.next();
         String id = XValidator.choiceID(locator);
         String caption = XValidator.choiceCaption(locator);
         String resName = OpGanttValidator.getResourceName(caption);
         //         if (!resName.equals(OpGanttValidator.NO_RESOURCE_NAME)) {
         Resource resource = (Resource) resourceMap.get(id);
         ResourceAssignment assignments = task.addResourceAssignment();
         assignments.setResourceUniqueID(resource.getID());
         Double resourceBaseEffort = (Double) OpGanttValidator.getResourceBaseEfforts(activity).get(id);
         double hours = Double.MAX_VALUE;
         if (caption.length() > resName.length()) {
            double hoursPerDay = OpGanttValidator.hoursAssigned(locator);
            if (calendar.getWorkHoursPerDay() != 0) {
               hours = hoursPerDay / calendar.getWorkHoursPerDay();
            }
//            Duration assignedHours = Duration.getInstance(assigned, TimeUnit.HOURS);
            assignments.setUnits(new Double(100 * hours));
         }
         
         if (resourceBaseEffort == null) {
            resourceBaseEffort = new Double(hours * OpGanttValidator.getDuration(activity));
         }
         Duration workDuration = Duration.getInstance(resourceBaseEffort == null ? 0 : resourceBaseEffort.doubleValue(), TimeUnit.HOURS);
         assignments.setWork(workDuration);
         
         i++;
         //         }
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
      //
      SortedMap wbs = OpGanttValidator.getWorkBreaks(activity);
      if (wbs != null && !wbs.isEmpty()) {
         double offset = 0;
         LinkedList splits = new LinkedList();
         Iterator iter = wbs.values().iterator();
         double durationInDays = calendar.convertDurationToUnit(OpGanttValidator.getDuration(activity), OpProjectCalendar.DAYS).doubleValue();
         while (iter.hasNext()) {
            Map<String, Object> wb = (Map<String, Object>) iter.next();
            double wbStart = ((Double) wb.get(OpGanttValidator.WORK_BREAK_START)).doubleValue();
            double wbDuration = ((Double) wb.get(OpGanttValidator.WORK_BREAK_DURATION)).doubleValue();
            offset += wbStart;
            splits.add(Duration.getInstance(8 * offset, TimeUnit.HOURS));
            offset += wbDuration;
            durationInDays += wbDuration;
            splits.add(Duration.getInstance(8 * offset, TimeUnit.HOURS));
         }
//         Date startDate = OpGanttValidator.getStart(activity);
//         Date stopDate = OpGanttValidator.getEnd(activity);
//         GregorianCalendar startCal = new GregorianCalendar();
//         startCal.setTime(startDate);
//         GregorianCalendar stopCal = new GregorianCalendar();
//         stopCal.setTime(stopDate);
//         int days = 1;
//         while (startCal.before(stopCal)) {
//            startCal.add(Calendar.DAY_OF_YEAR, 1);
//            days++;
//         }
//         splits.add(Duration.getInstance(days, TimeUnit.DAYS));
         splits.add(Duration.getInstance(8 * durationInDays, TimeUnit.HOURS));

         task.setSplits(splits);
         task.setSplitCompleteDuration(Duration.getInstance(((Duration) splits.getLast()).getDuration(), TimeUnit.HOURS));
      }

      //20) Work Phase begin - List (Date)
      //21) Work Phase end - List (Date)
      //22) Work Phase base efforts List (double)
      //23) Resource based efforts  List (double)
      //24) Priority (only for tasks)
      //25) Work records map - HashMap
      //26) Actual effort - double

      return task;
   }

   private static double convertMsDurationToWorkingHours(Duration duration, boolean roundHoursToDays, OpProjectCalendar calendar) {
      double oppDuration;
      if (duration == null) {
         return 0;
      }
      double msDuration = duration.getDuration();
      TimeUnit timeUnit = duration.getUnits();
      if (timeUnit == TimeUnit.HOURS) {
         // since we only accept complete days, we should round things up here, don't we?
         oppDuration = msDuration;
      }
      else if (timeUnit == TimeUnit.DAYS) {
         oppDuration = msDuration * calendar.getWorkHoursPerDay();
      }
      else {
         //if (timeUnit == TimeUnit.WEEKS)
         oppDuration = msDuration * calendar.getWorkHoursPerWeek();
      }
      if (roundHoursToDays && oppDuration % calendar.getWorkHoursPerDay() != 0) {
         oppDuration = oppDuration
              + calendar.getWorkHoursPerDay()
              - (oppDuration
              % calendar.getWorkHoursPerDay());
      }
      return oppDuration;
   }
}
