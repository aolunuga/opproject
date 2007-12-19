/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project.components.OpIncrementalValidator;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendar;
import onepoint.project.modules.settings.holiday_calendar.OpHolidayCalendarManager;
import onepoint.project.modules.user.OpUser;
import onepoint.project.OpProjectSession;
import onepoint.util.XCalendar;

import java.sql.Date;
import java.util.*;
import java.util.concurrent.*;

/**
 * Class responsible for (re)validating project plans.
 *
 * @author horia.chiorean
 */
public class OpProjectPlanValidator {

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpProjectPlanValidator.class);

   /**
    * The project plan that will be validated.
    */
   private OpProjectPlan projectPlan = null;


   /**
    * Creates a new project plan validator instance.
    *
    * @param projectPlan an <code>OpProjectPlan</code> entity.
    */
   public OpProjectPlanValidator(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   /**
    * Validates this validator's project plan.
    *
    * @param broker       a <code>OpBroker</code> used for persistence operations. Note that the state of this broker
    *                     (opened/closed) is not maintained by this method and therefore must be handled elsewhere.
    * @param modifier     a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    * @param creatorField Allows to specify a name for the "validation-creator", meaning the creator for the revalidated versions.
    *                     If set to null, will default to session user.
    */
   public void validateProjectPlan(OpBroker broker, PlanModifier modifier, String creatorField) {
      OpTransaction tx = broker.newTransaction();

      OpProjectNode projectNode = projectPlan.getProjectNode();
      HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);

      logger.info("Revalidating plan for " + projectNode.getName());

      //obtain the holiday calendar settings from the project plan
      OpHolidayCalendar holidayCalendar = null;
      if (OpHolidayCalendarManager.getHolidayCalendarsMap() != null) {
         holidayCalendar = (OpHolidayCalendar) OpHolidayCalendarManager.getHolidayCalendarsMap().get(projectPlan.getHolidayCalendar());
      }

      //obtain the calendar settings from OpSettings
      int firstWorkday = Integer.valueOf(OpSettingsService.getService().get(OpSettings.CALENDAR_FIRST_WORKDAY));
      int lastWorkday = Integer.valueOf(OpSettingsService.getService().get(OpSettings.CALENDAR_LAST_WORKDAY));
      double dayWorkTime = Double.valueOf(OpSettingsService.getService().get(OpSettings.CALENDAR_DAY_WORK_TIME));
      double weekWorkTime = Double.valueOf(OpSettingsService.getService().get(OpSettings.CALENDAR_WEEK_WORK_TIME));

      OpGanttValidator validator = this.createValidator(resources);

      //obtain the original calendar settings from the newly created OpGantValidator
      XCalendar.PlanningSettings originalPlanningSettings = validator.getCalendar().getPlanningSettings();

      if (holidayCalendar != null) {
         //create a new Planning Settings object by combining the sessionPlanningSettings with the holiday calendar
         //from the project plan
         XCalendar.PlanningSettings updatedPlanningSettings = new XCalendar.PlanningSettings(firstWorkday, lastWorkday,
              dayWorkTime, weekWorkTime, new TreeSet(holidayCalendar.getHolidayDates()), projectPlan.getHolidayCalendar());
         //update the validator calendar with the values from the OpSettings and project plan
         validator.getCalendar().setPlanningSettings(updatedPlanningSettings);
      }

      if (creatorField != null) {
         projectPlan.setCreator(creatorField);
      }
      this.validateWorkingVersionPlan(broker, validator, modifier, resources);
      this.validatePlan(broker, validator, modifier, resources);

      //restore the original settings of the validator
      if (holidayCalendar != null) {
         validator.getCalendar().setPlanningSettings(originalPlanningSettings);
      }

      tx.commit();
   }

   /**
    * Validates the working plan versions for the given project plan.
    *
    * @param broker           a <code>OpBroker</code> used for persistence operations.
    * @param modifier         a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    *                         Can be <code>null</code>.
    * @param startTransaction a <code>boolean</code> whether to start a transaction or not for
    *                         the validation operation.
    */
   public void validateProjectPlanWorkingVersion(OpBroker broker, PlanModifier modifier, boolean startTransaction) {
      OpTransaction tx = startTransaction ? broker.newTransaction() : null;

      OpProjectNode projectNode = projectPlan.getProjectNode();
      HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);

      logger.info("Revalidating working version plan for " + projectNode.getName());

      //obtain the holiday calendar settings from the project plan
      OpHolidayCalendar holidayCalendar = null;
      if (OpHolidayCalendarManager.getHolidayCalendarsMap() != null) {
         holidayCalendar = (OpHolidayCalendar) OpHolidayCalendarManager.getHolidayCalendarsMap().get(projectPlan.getHolidayCalendar());
      }

      //obtain the calendar settings from OpSettings
      int firstWorkday = Integer.valueOf(OpSettingsService.getService().get(OpSettings.CALENDAR_FIRST_WORKDAY));
      int lastWorkday = Integer.valueOf(OpSettingsService.getService().get(OpSettings.CALENDAR_LAST_WORKDAY));
      double dayWorkTime = Double.valueOf(OpSettingsService.getService().get(OpSettings.CALENDAR_DAY_WORK_TIME));
      double weekWorkTime = Double.valueOf(OpSettingsService.getService().get(OpSettings.CALENDAR_WEEK_WORK_TIME));

      OpGanttValidator validator = this.createValidator(resources);

      //obtain the original calendar settings from the newly created OpGantValidator
      XCalendar.PlanningSettings originalPlanningSettings = validator.getCalendar().getPlanningSettings();

      if (holidayCalendar != null) {
         //create a new Planning Settings object by combining the sessionPlanningSettings with the holiday calendar
         //from the project plan
         XCalendar.PlanningSettings updatedPlanningSettings = new XCalendar.PlanningSettings(firstWorkday, lastWorkday,
              dayWorkTime, weekWorkTime, new TreeSet(holidayCalendar.getHolidayDates()), projectPlan.getHolidayCalendar());
         //update the validator calendar with the values from the OpSettings and project plan
         validator.getCalendar().setPlanningSettings(updatedPlanningSettings);
      }

      this.validateWorkingVersionPlan(broker, validator, modifier, resources);

      //restore the original settings of the validator
      if (holidayCalendar != null) {
         validator.getCalendar().setPlanningSettings(originalPlanningSettings);
      }

      if (tx != null) {
         tx.commit();
      }
   }

   /**
    * Performs the actual validation on this validator's project plan.
    *
    * @param broker    a <code>OpBroker</code> used for persistence operations.
    * @param validator a <code>OpGanttValidator</code> used for gantt validation logic.
    * @param modifier  a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    *                  Can be <code>null</code>.
    * @param resources a <code>HashMap</code> of project resources.
    */
   private void validatePlan(OpBroker broker, OpGanttValidator validator, PlanModifier modifier, HashMap resources) {
      //always update the project plan
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, projectPlan, dataSet, true);
      validator.setDataSet(dataSet);
      if (modifier != null) {
         modifier.modifyPlan(validator);
      }
      validator.validateEntireDataSet();
      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, resources, projectPlan, null);
   }

   /**
    * Validates the working version of a project plan, if one exists.
    *
    * @see OpProjectPlanValidator#validateProjectPlan(onepoint.persistence.OpBroker, onepoint.project.modules.project.OpProjectPlanValidator.PlanModifier, String)
    */
   private void validateWorkingVersionPlan(OpBroker broker, OpGanttValidator validator, PlanModifier modifier, HashMap resources) {
      //if there is a working plan, validate it
      OpProjectPlanVersion workingPlan = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, projectPlan, OpProjectPlan.WORKING_VERSION_NUMBER);
      if (workingPlan != null) {
         XComponent dataSet = new XComponent(XComponent.DATA_SET);
         OpActivityVersionDataSetFactory.retrieveActivityVersionDataSet(broker, workingPlan, dataSet, true);
         validator.setDataSet(dataSet);
         //allow custom modifications
         if (modifier != null) {
            modifier.modifyPlan(validator);
         }
         validator.validateEntireDataSet();
         OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, workingPlan, resources, false);
      }
   }

   /**
    * Creates a Gantt validation object that will update a project plan.
    *
    * @param resources a <code>HashMap</code> of resources.
    * @return a <code>OpGanttValidator</code> instance.
    */
   public OpIncrementalValidator createValidator(HashMap resources) {
      OpIncrementalValidator validator = new OpIncrementalValidator();

      //copy the calendar (create a new instance)
      XCalendar oldCalendar = validator.getCalendar();
      validator.setCalendar(oldCalendar.copyInstance());
      
      OpProjectNode projectNode = projectPlan.getProjectNode();

      Date startDate = (projectNode.getType() == OpProjectNode.TEMPLATE) ? projectPlan.getStart() : projectNode.getStart();
      validator.setProjectStart(startDate);

      Date finish = (projectNode.getType() == OpProjectNode.TEMPLATE) ? projectPlan.getFinish() : projectNode.getFinish();
      validator.setProjectFinish(finish);
      validator.setProjectPlanFinish(finish);

      validator.setProgressTracked(Boolean.valueOf(projectPlan.getProgressTracked()));
      validator.setProjectTemplate(Boolean.valueOf(projectPlan.getTemplate()));
      validator.setCalculationMode(new Byte(projectPlan.getCalculationMode()));

      XComponent resourceDataSet = new XComponent();
      OpActivityDataSetFactory.retrieveResourceDataSet(resources, resourceDataSet);
      validator.setAssignmentSet(resourceDataSet);
      XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.fillHourlyRatesDataSet(projectPlan.getProjectNode(), hourlyRates);
      validator.setHourlyRatesDataSet(hourlyRates);
      return validator;
   }

   /**
    * Revalidates a list of project plans, given their ids, each project plan in its own separate
    * thread.
    * @param session a <code>OpProjectSession</code> the server session
    * @param projectPlanIds a <code>List(long)</code> the list of project plan ids.
    */
   public static void revalidateProjectPlans(OpProjectSession session, List<Long> projectPlanIds) {
      //this should not exceed the connection pool size !
      int queSize = 5;
      List<PlanValidatorTask> validationTasks = new ArrayList<PlanValidatorTask>(queSize);
      ExecutorService executorService = Executors.newFixedThreadPool(queSize);
      Iterator<Long> it = projectPlanIds.iterator();
      while (it.hasNext()) {
         long projectPlanId = it.next();
         it.remove();
         OpBroker broker = session.newBroker();
         OpProjectPlan projectPlan = broker.getObject(OpProjectPlan.class, projectPlanId);
         PlanValidatorTask validationTask = new OpProjectPlanValidator(projectPlan).new PlanValidatorTask(broker);
         if (validationTasks.size() < queSize) {
            validationTasks.add(validationTask);
         }
         else {
            executeValidationTasks(validationTasks, executorService);
            validationTasks.clear();
         }
      }
      if (!validationTasks.isEmpty()) {
         executeValidationTasks(validationTasks, executorService);
      }
      executorService.shutdown();
   }

   /**
    * Starts a separate thread which will perform a project plan validation, based on the list
    * of tasks to execute.
    * @param validationTasks a <code>List(PlanValidatorTask)</code>.
    * @param executorService  a <code>ExecutorService</code> which will start each thread.
    */
   private static void executeValidationTasks(List<PlanValidatorTask> validationTasks,
        ExecutorService executorService) {
      List<Future<?>> results = new ArrayList<Future<?>>();
      for (PlanValidatorTask newValidationTask : validationTasks) {
         results.add(executorService.submit(newValidationTask));
      }
      for (Future<?> future : results) {
         try {
            future.get(1000 * 60 * 10, TimeUnit.MILLISECONDS);
         }
         catch (Exception e) {
            logger.error("Could not revalidate project because: ", e);
         }
      }
   }

   /**
    * Hook interface that allows a client of <code>ProjectPlanValidator</code> to perform custom operations on a project
    * plan before the project plan is validated an written in the db.
    */
   public interface PlanModifier {

      /**
       * Allows the implementing class (typically an anonymous inner class) to change a project plan before it's validated.
       *
       * @param validator a <code>OpGanttValidator</code> that contains an in-memory representation of the project plan.
       */
      public void modifyPlan(OpGanttValidator validator);
   }

   /**
    * Inner class, representing a validation task, which will be run from a separate thread.
    */
   private class PlanValidatorTask implements Runnable {
      /**
       * A  broker to perform db operations.
       */
      private OpBroker broker = null;

      /**
       * Creates a new validation task instance.
       * @param broker a <code>OpBroker</code> used for db operations.
       */
      private PlanValidatorTask(OpBroker broker) {
         this.broker = broker;
      }

      /**
       * @see Runnable#run()
       */
      public void run() {
         OpProjectPlanValidator.this.validateProjectPlan(broker, null, OpUser.SYSTEM_USER_NAME);
         broker.closeAndEvict();
      }
   }
}
