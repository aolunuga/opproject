/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceManager;

import java.util.HashMap;

/**
 * Class responsible for (re)validating project plans.
 *
 * @author horia.chiorean
 */
public class OpProjectPlanValidator {

   /**
    * This class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpProjectPlanValidator.class, true);

   /**
    * The project plan that will be validated.
    */
   private OpProjectPlan projectPlan = null;


   /**
    * Creates a new project plan validator instance.
    * @param projectPlan an <code>OpProjectPlan</code> entity.
    */
   public OpProjectPlanValidator(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   /**
    * Validates this validator's project plan.
    * 
    * @param broker a <code>OpBroker</code> used for persistence operations. Note that the state of this broker
    * (opened/closed) is not maintained by this method and therefore must be handled elsewhere.
    * @param modifier a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    * Can be <code>null</code>.
    */
   public void validateProjectPlan(OpBroker broker, PlanModifier modifier) {
      OpTransaction tx = broker.newTransaction();

      OpProjectNode projectNode = projectPlan.getProjectNode();
      HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);

      logger.info("Revalidating plan for " + projectNode.getName());
      OpGanttValidator validator = this.createValidator(resources);
      this.validateWorkingVersionPlan(broker, validator, modifier, resources);
      this.validatePlan(broker, validator, modifier, resources);

      tx.commit();
   }

   /**
    * Validates this validator's project plan, by creating a new plan version and checking it in.
    *
    * @param session a <code>OpProjectSession</code> representing the server session.
    * @param modifier a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    * Can be <code>null</code>.
    * @return a <code>XMessage</code> that represents a possible error response or <code>null</code> if the operation was
    * successfull.
    *<FIXME author="Horia Chiorean" description="Possible problem: this method is not atomic">
    */
   public XMessage validateProjectPlanIntoNewVersion(OpProjectSession session, PlanModifier modifier) {
      OpProjectPlanningService planningService = (OpProjectPlanningService) XServiceManager.getService(OpProjectPlanningService.SERVICE_NAME);
      if (planningService == null) {
         throw new UnsupportedOperationException("Cannot retrieve the registered project planning service !");
      }

      //check out the current project plan
      String projectId = projectPlan.getProjectNode().locator();
      XMessage editActivitiesRequest = new XMessage();
      editActivitiesRequest.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      XMessage reply = planningService.editActivities(session, editActivitiesRequest);
      if (reply != null && reply.getError() != null) {
         return reply;
      }

      //create and validated a working version
      OpBroker broker = session.newBroker();
      OpTransaction tx = broker.newTransaction();

      OpProjectPlanVersion workingVersion = OpActivityVersionDataSetFactory.newProjectPlanVersion(broker, projectPlan, session.user(broker),
         OpProjectAdministrationService.WORKING_VERSION_NUMBER, false);
      String workingPlanLocator = workingVersion.locator();
      OpProjectNode projectNode = projectPlan.getProjectNode();
      HashMap resources = OpActivityDataSetFactory.resourceMap(broker, projectNode);
      logger.info("Revalidating working plan for " + projectNode.getName());
      OpGanttValidator validator = this.createValidator(resources);
      this.validatePlanVersion(broker, validator, modifier, resources, workingVersion);

      tx.commit();
      broker.close();

      //check-in the working version
      XMessage checkInRequest = new XMessage();
      checkInRequest.setArgument(OpProjectPlanningService.ACTIVITY_SET, validator.getDataSet());
      checkInRequest.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      checkInRequest.setArgument(OpProjectPlanningService.WORKING_PLAN_VERSION_ID, workingPlanLocator);
      reply = planningService.checkInActivities(session, checkInRequest);

      if (reply != null && reply.getError() != null) {
         return reply;
      }
      return null;
   }

   /**
    * Performs the actual validation on this validator's project plan.
    * @param broker a <code>OpBroker</code> used for persistence operations.
    * @param validator a <code>OpGanttValidator</code> used for gantt validation logic.
    * @param modifier a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    * Can be <code>null</code>.
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
      validator.validateDataSet();
      OpActivityDataSetFactory.storeActivityDataSet(broker, dataSet, resources, projectPlan, null);
   }

   /**
    * Validates this validator's project plan into a new plan version.
    * 
    * @param broker a <code>OpBroker</code> used for persistence operations.
    * @param validator a <code>OpGanttValidator</code> used for gantt validation logic.
    * @param modifier a <code>PlanModifier</code> instance, that allows a hook into the validation mechanism.
    * Can be <code>null</code>.
    * @param resources a <code>HashMap</code> of project resources.
    * @param planVersion a <code>OpProjectPlanVersion</code> instance.
    */
   private void validatePlanVersion(OpBroker broker, OpGanttValidator validator, PlanModifier modifier, HashMap resources,
        OpProjectPlanVersion planVersion) {
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpActivityDataSetFactory.retrieveActivityDataSet(broker, projectPlan, dataSet, false);
      validator.setDataSet(dataSet);
      if (modifier != null) {
         modifier.modifyPlan(validator);
      }
      validator.validateDataSet();
      OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, planVersion, resources, false);
   }

   /**
    * Validates the working version of a project plan, if one exists.
    *
    * @see onepoint.project.modules.project.OpProjectPlanValidator#validateProjectPlan(onepoint.persistence.OpBroker, onepoint.project.modules.project.OpProjectPlanValidator.PlanModifier)
    *
    */
   private void validateWorkingVersionPlan(OpBroker broker, OpGanttValidator validator, PlanModifier modifier, HashMap resources) {
      //if there is a working plan, validate it
      OpProjectPlanVersion workingPlan = OpActivityVersionDataSetFactory.findProjectPlanVersion(broker, projectPlan, OpProjectAdministrationService.WORKING_VERSION_NUMBER);
      if (workingPlan != null) {
         XComponent dataSet = new XComponent(XComponent.DATA_SET);
         OpActivityVersionDataSetFactory.retrieveActivityVersionDataSet(broker, workingPlan, dataSet, true);
         validator.setDataSet(dataSet);
         //allow custom modifications
         if (modifier != null) {
            modifier.modifyPlan(validator);
         }
         validator.validateDataSet();
         OpActivityVersionDataSetFactory.storeActivityVersionDataSet(broker, dataSet, workingPlan, resources, false);
      }
   }

   /**
    * Creates a Gantt validation object that will update a project plan.
    * @param resources a <code>HashMap</code> of resources.
    * @return a <code>OpGanttValidator</code> instance.
    */
   private OpGanttValidator createValidator(HashMap resources) {
      OpGanttValidator validator = new OpGanttValidator();
      validator.setProjectStart(projectPlan.getProjectNode().getStart());
      validator.setProgressTracked(Boolean.valueOf(projectPlan.getProgressTracked()));
      validator.setProjectTemplate(Boolean.valueOf(projectPlan.getTemplate()));
      validator.setCalculationMode(new Byte(projectPlan.getCalculationMode()));

      XComponent resourceDataSet = new XComponent();
      OpActivityDataSetFactory.retrieveResourceDataSet(resources, resourceDataSet);
      validator.setAssignmentSet(resourceDataSet);
      return validator;
   }

   /**
    * Hook interface that allows a client of <code>ProjectPlanValidator</code> to perform custom operations on a project
    * plan before the project plan is validated an written in the db.
    */
   public interface PlanModifier {

      /**
       * Allows the implementing class (typically an anonymous inner class) to change a project plan before it's validated.
       * @param validator a <code>OpGanttValidator</code> that contains an in-memory representation of the project plan.
       */
      public void modifyPlan(OpGanttValidator validator);
   }
}
