/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/**
 * 
 */
package onepoint.project.modules.project;

import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpCostRecord;

/**
 * @author dfreis
 *
 */
public interface OpAssignmentIfc extends OpGanttValidator.ProgressTrackableEntityIfc {

   public final static String ASSIGNMENT = "OpAssignment";

   public final static String ASSIGNED = "Assigned";

   public final static String COMPLETE = "Complete";

   public final static String BASE_EFFORT = "BaseEffort";

   public final static String ACTUAL_EFFORT = "ActualEffort";

   public final static String BASE_PROCEEDS = "BaseProceeds";

   public final static String ACTUAL_PROCEEDS = "ActualProceeds";

   public final static String REMAINING_EFFORT = "RemainingEffort";

   public final static String BASE_COSTS = "BaseCosts";

   public final static String ACTUAL_COSTS = "ActualCosts";

   public final static String PROJECT_PLAN = "ProjectPlan";

   public final static String RESOURCE = "Resource";

   public final static String ACTIVITY = "Activity";

   public final static String WORK_RECORDS = "WorkRecords";

   public OpActivityIfc getActivity();

   public abstract void setAssigned(double assigned);

   public abstract double getAssigned();

   public abstract void setComplete(double complete);

   public abstract double getComplete();

   public abstract void setBaseEffort(double baseEffort);

   public abstract double getBaseEffort();

   public abstract void setBaseCosts(double baseCosts);

   /**
    * Gets the base personnel costs for this assignment.
    *
    * @return base personnel costs
    */
   public abstract double getBaseCosts();

   /**
    * @return base proceeds for this assignment.
    */
   public abstract double getBaseProceeds();

   public abstract void setBaseProceeds(Double baseProceeds);

   public abstract void setResource(OpResource resource);

   public abstract OpResource getResource();

   public final static Byte COST_TYPE_UNDEFINED = new Byte(
         OpCostRecord.COST_TYPE_UNDEFINED);

   public final static Byte COST_TYPE_TRAVEL = new Byte(
         OpCostRecord.TRAVEL_COST);

   public final static Byte COST_TYPE_MATERIAL = new Byte(
         OpCostRecord.MATERIAL_COST);

   public final static Byte COST_TYPE_EXTERNAL = new Byte(
         OpCostRecord.EXTERNAL_COST);

   public final static Byte COST_TYPE_MISC = new Byte(
         OpCostRecord.MISCELLANEOUS_COST);

   public double getCompleteFromTracking();

   public double getRemainingEffort();
}
