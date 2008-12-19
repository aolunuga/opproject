/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.calendars.OpProjectCalendarFactory;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.settings.OpSettings;
import onepoint.project.modules.settings.OpSettingsService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpProjectCalendar;
import onepoint.service.server.XServiceException;

/**
 * Class responsible for (re)validating project plans.
 *
 * @author horia.chiorean
 */
public class OpProjectPlanValidator {

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpProjectPlanValidator.class);

   private static final Object MUTEX = new Object();

   private static OpProjectPlanValidator instance;


   /**
    * Creates a new project plan validator instance.
    *
    * @param projectPlan an <code>OpProjectPlan</code> entity.
    */
   protected OpProjectPlanValidator() {
   }
   
   public static OpProjectPlanValidator getInstance() {
      synchronized (MUTEX) {
         if (instance == null) {
            instance = new OpProjectPlanValidator();
         }
         return instance;
      }
   }
   
   public static void register(OpProjectPlanValidator validator) {
      synchronized (MUTEX) {
         instance = validator;
      }
   }
   
   /**
    * Validates this validator's project plan.
    * @param session 
    *
    * @param broker       a <code>OpBroker</code> used for persistence operations. Note that the state of this broker
    *                     (opened/closed) is not maintained by this method and therefore must be handled elsewhere.
    * @param projectPlan  the project plan to validate
    * @param modifier     a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    * @param creatorField Allows to specify a name for the "validation-creator", meaning the creator for the revalidated versions.
    *                     If set to null, will default to session user.
    */
   public void validateProjectPlan(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan, PlanModifier modifier, String creatorField) {
      OpTransaction tx = broker.newTransaction();
      try {
         OpProjectNode projectNode = projectPlan.getProjectNode();
         HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);

         logger.info("Revalidating plan for " + projectNode.getName());

         //obtain the calendar settings from OpSettings
         int firstWorkday = Integer.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_FIRST_WORKDAY));
         int lastWorkday = Integer.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_LAST_WORKDAY));
         double dayWorkTime = Double.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_DAY_WORK_TIME));
         double weekWorkTime = Double.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_WEEK_WORK_TIME));
         Boolean holidaysAreWorkdays = Boolean.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.HOLIDAYS_ARE_WORKDAYS));
         
         OpGanttValidator validator = this.createValidator(session, broker, projectPlan);

         if (creatorField != null) {
            projectPlan.setCreator(creatorField);
         }
         this.validateWorkingVersionPlan(session, broker, projectPlan, validator, modifier, resources);
         this.validatePlan(session, broker, projectPlan, validator, modifier, resources);

         tx.commit();
      }
      catch (RuntimeException exc) {
         tx.rollback();
         throw exc;
      }
   }

   /**
    * Validates the working plan versions for the given project plan.
    * @param session 
    *
    * @param broker           a <code>OpBroker</code> used for persistence operations.
    * @param projectPlan the project plan to validate
    * @param modifier         a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    *                         Can be <code>null</code>.
    * @param startTransaction a <code>boolean</code> whether to start a transaction or not for
    *                         the validation operation.
    */
   public void validateProjectPlanWorkingVersion(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan, PlanModifier modifier, boolean startTransaction) {
      OpTransaction tx = startTransaction ? broker.newTransaction() : null;

      OpProjectNode projectNode = projectPlan.getProjectNode();
      HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);

      logger.info("Revalidating working version plan for " + projectNode.getName());

      //obtain the calendar settings from OpSettings
      int firstWorkday = Integer.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_FIRST_WORKDAY));
      int lastWorkday = Integer.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_LAST_WORKDAY));
      double dayWorkTime = Double.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_DAY_WORK_TIME));
      double weekWorkTime = Double.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.CALENDAR_WEEK_WORK_TIME));
      Boolean holidaysAreWorkdays = Boolean.valueOf(OpSettingsService.getService().getStringValue(broker, OpSettings.HOLIDAYS_ARE_WORKDAYS));

      OpGanttValidator validator = this.createValidator(session, broker, projectPlan);

      this.validateWorkingVersionPlan(session, broker, projectPlan, validator, modifier, resources);

      if (tx != null) {
         tx.commit();
      }
   }

   /**
    * Performs the actual validation on this validator's project plan.
    *
    * @param broker    a <code>OpBroker</code> used for persistence operations.
    * @param projectPlan the project plan to validate
    * @param validator a <code>OpGanttValidator</code> used for gantt validation logic.
    * @param modifier  a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    *                  Can be <code>null</code>.
    * @param resources a <code>HashMap</code> of project resources.
    * 
    */
   private void validatePlan(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan, 
         OpGanttValidator validator, PlanModifier modifier, HashMap resources) 
      throws XServiceException {
      //always update the project plan
      String projectNodeLocator = projectPlan.getProjectNode().locator();
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityVersionDataSetFactory.getInstance().retrieveActivityVersionDataSet(session, broker, projectPlan.getLatestVersion(), dataSet, true);
      validator.setDataSet(dataSet);
      if (modifier != null) {
         modifier.modifyPlan(validator);
      }
      validator.validateEntireDataSet();
      
      OpActivityVersionDataSetFactory.getInstance()
            .storeActivityVersionDataSet(session, broker, dataSet,
                  projectPlan.getLatestVersion(), resources, projectPlan.getLatestVersion(), false);
      OpActivityDataSetFactory.getInstance().checkInProjectPlan(session,
            broker, projectPlan.getLatestVersion());
   }

   /**
    * Validates the working version of a project plan, if one exists.
    *
    * @see OpProjectPlanValidator#validateProjectPlan(onepoint.persistence.OpBroker, onepoint.project.modules.project.OpProjectPlanValidator.PlanModifier, String)
    */
   private void validateWorkingVersionPlan(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan, OpGanttValidator validator, PlanModifier modifier, HashMap resources) {
      //if there is a working plan, validate it
      OpProjectPlanVersion workingPlan = projectPlan.getWorkingVersion();
      if (workingPlan != null) {
         XComponent dataSet = new XComponent(XComponent.DATA_SET);
         //
         OpActivityVersionDataSetFactory.getInstance().retrieveActivityVersionDataSet(session, broker, workingPlan, dataSet, true);
         OpActivityDataSetFactory.getInstance().importSubProjectActivities(session, broker, dataSet);
         validator.setDataSet(dataSet);
         //allow custom modifications
         if (modifier != null) {
            modifier.modifyPlan(validator);
         }
         validator.validateEntireDataSet();
         OpActivityVersionDataSetFactory.getInstance().storeActivityVersionDataSet(session, broker, dataSet, workingPlan, resources, projectPlan.getWorkingVersion(), false);
      }
   }

   /**
    * Creates a Gantt validation object that will update a project plan.
    *
    * @param projectPlan the project plan to validate
    * @param resources a <code>HashMap</code> of resources.
    * @return a <code>OpGanttValidator</code> instance.
    */
   public OpGanttValidator createValidator(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan) {
      return createValidator(session, broker, projectPlan, null);
   }
   public OpGanttValidator createValidator(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan, OpProjectPlanVersion pv) {

      HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectPlan.getProjectNode());
      
      OpGanttValidator validator = newValidator();
      
      Map<String, OpProjectCalendar> resCals = createResourceCalendarMap(
            session, broker, projectPlan);
      Map<String, Set<Date>> absences = new HashMap<String, Set<Date>>();
      if (projectPlan.getProjectNode().getAssignments() != null) {
         absences = createResourceAbsences(projectPlan.getProjectNode().getAssignments());
      }
      OpProjectCalendar pc = OpProjectCalendarFactory.getInstance().getCalendar(session, broker, pv != null ? pv : projectPlan);

      validator.setupCalendars(pc, resCals);
      validator.setupAbsences(absences);
      
      OpProjectNode projectNode = projectPlan.getProjectNode();

      Date startDate = projectPlan.getTemplate() ? (pv != null ? pv.getStart() : projectPlan.getStart()) : projectNode.getStart();
      validator.setProjectStart(startDate);

      Date finish = projectPlan.getTemplate() ? (pv != null ? pv.getFinish() :projectPlan.getFinish()) : projectNode.getFinish();
      validator.setProjectFinish(finish);
      validator.setProjectPlanFinish(finish);

      validator.setProgressTracked(Boolean.valueOf(projectPlan.getProgressTracked()));
      validator.setProjectTemplate(Boolean.valueOf(projectPlan.getTemplate()));
      validator.setCalculationMode(new Byte(projectPlan.getCalculationMode()));

      XComponent resourceDataSet = new XComponent();
      OpActivityDataSetFactory.getInstance().retrieveResourceDataSet(resources, resourceDataSet);
      validator.setAssignmentSet(resourceDataSet);
      XComponent hourlyRates = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.fillHourlyRatesDataSet(projectPlan.getProjectNode(), hourlyRates);
      validator.setHourlyRatesDataSet(hourlyRates);
      return validator;
   }

   public Map<String, OpProjectCalendar> createResourceCalendarMap(
         OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan) {
      return new HashMap<String, OpProjectCalendar>();
   }

   /**
    * @param resourceAssignents
    * @return
    */
   public Map<String, Set<Date>> createResourceAbsences(
         Set<OpProjectNodeAssignment> resourceAssignents) {
      Map<String, Set<Date>> absences = new HashMap<String, Set<Date>>();
      return absences;
   }

   /**
    * @return
    * @pre
    * @post
    */
   protected OpGanttValidator newValidator() {
      OpGanttValidator gv = new OpGanttValidator();
      return gv;
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
         try {
            OpProjectPlan projectPlan = broker.getObject(OpProjectPlan.class, projectPlanId);
            PlanValidatorTask validationTask = new PlanValidatorTask(session, broker, projectPlan);
            if (validationTasks.size() < queSize) {
               validationTasks.add(validationTask);
            }
            else {
               executeValidationTasks(validationTasks, executorService);
               validationTasks.clear();
            }
         }
         finally {
            broker.close();
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
}

/**
 * Inner class, representing a validation task, which will be run from a separate thread.
 */
final class PlanValidatorTask implements Runnable {
   /**
    * A  broker to perform db operations.
    */
   private OpBroker broker = null;
   private OpProjectSession session;
   private OpProjectPlan projectPlan;

   /**
    * Creates a new validation task instance.
    * @param session 
    * @param broker a <code>OpBroker</code> used for db operations.
    */
   PlanValidatorTask(OpProjectSession session, OpBroker broker, OpProjectPlan projectPlan) {
      this.session = session;
      this.broker = broker;
      this.projectPlan = projectPlan;
   }

   /**
    * @see Runnable#run()
    */
   public void run() {
      OpProjectPlanValidator.getInstance().validateProjectPlan(session, broker, projectPlan, null, OpUser.SYSTEM_USER_NAME);
      broker.closeAndEvict();
   }
}
