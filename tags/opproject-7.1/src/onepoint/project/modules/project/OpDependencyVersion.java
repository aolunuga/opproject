/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

public class OpDependencyVersion extends OpObject {

   public final static String DEPENDENCY_VERSION = "OpDependencyVersion";

   public final static String PLAN_VERSION = "PlanVersion";
   public final static String PREDECESSOR_VERSION = "PredecessorVersion";
   public final static String SUCCESSOR_VERSION = "SuccessorVersion";

   private OpProjectPlanVersion planVersion;
   private OpActivityVersion predecessorVersion;
   private OpActivityVersion successorVersion;

   public void setPlanVersion(OpProjectPlanVersion planVersion) {
      this.planVersion = planVersion;
   }

   public OpProjectPlanVersion getPlanVersion() {
      return planVersion;
   }

   public void setPredecessorVersion(OpActivityVersion predecessorVersion) {
      this.predecessorVersion = predecessorVersion;
   }

   public OpActivityVersion getPredecessorVersion() {
      return predecessorVersion;
   }

   public void setSuccessorVersion(OpActivityVersion successorVersion) {
      this.successorVersion = successorVersion;
   }

   public OpActivityVersion getSuccessorVersion() {
      return successorVersion;
   }

}
