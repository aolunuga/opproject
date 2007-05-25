/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

public class OpDependency extends OpObject {

   public final static String DEPENDENCY = "OpDependency";

   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String PREDECESSOR_ACTIVITY = "PredecessorActivity";
   public final static String SUCCESSOR_ACTIVITY = "SuccessorActivity";

   private OpProjectPlan projectPlan;
   private OpActivity predecessorActivity;
   private OpActivity successorActivity;

   public void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   public OpProjectPlan getProjectPlan() {
      return projectPlan;
   }

   public void setPredecessorActivity(OpActivity predecessorActivity) {
      this.predecessorActivity = predecessorActivity;
   }

   public OpActivity getPredecessorActivity() {
      return predecessorActivity;
   }

   public void setSuccessorActivity(OpActivity successorActivity) {
      this.successorActivity = successorActivity;
   }

   public OpActivity getSuccessorActivity() {
      return successorActivity;
   }

}
