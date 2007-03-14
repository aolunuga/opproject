/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.modules.project.components.OpGanttValidator;

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
