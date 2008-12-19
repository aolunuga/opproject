/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.components.OpGanttValidator;

public class OpDependencyVersion extends OpObject implements OpDependencyIfc {

   public final static String DEPENDENCY_VERSION = "OpDependencyVersion";

   public final static String PLAN_VERSION = "PlanVersion";
   public final static String PREDECESSOR_VERSION = "PredecessorVersion";
   public final static String SUCCESSOR_VERSION = "SuccessorVersion";

   private OpProjectPlanVersion planVersion;
   private OpActivityVersion predecessorVersion;
   private OpActivityVersion successorVersion;
   
   private int type = OpGanttValidator.DEP_DEFAULT;
   private int attributes = 0;
   
   public void setPlanVersion(OpProjectPlanVersion planVersion) {
      this.planVersion = planVersion;
   }

   public OpProjectPlanVersion getPlanVersion() {
      return planVersion;
   }

   public void setPredecessorVersion(OpActivityVersion predecessorVersion) {
      this.predecessorVersion = predecessorVersion;
   }

   // deprecated
   public OpActivityVersion getPredecessorVersion() {
      return predecessorVersion;
   }

   public OpActivityVersion getPredecessorActivity() {
      return predecessorVersion;
   }

   public void setSuccessorVersion(OpActivityVersion successorVersion) {
      this.successorVersion = successorVersion;
   }

   // deprecated
   public OpActivityVersion getSuccessorVersion() {
      return successorVersion;
   }

   public OpActivityVersion getSuccessorActivity() {
      return successorVersion;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpDependencyIfc#getType()
    */
   public int getDependencyType() {
      return type & OpDependency.TYPE_MASK;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpDependencyIfc#setType(int)
    */
   public void setDependencyType(int type) {
      this.type = (this.type ^ (this.type & OpDependency.TYPE_MASK)) | type;
   }

   public int getAttributes() {
      return (this.type ^ (this.type & OpDependency.TYPE_MASK));
   }

   private void setAttributes(int attibutes) {
      this.type = (this.type & OpDependency.TYPE_MASK) | attibutes;
   }
   
   public void setAttribute(int key, boolean value) {
      if (value) {
         setAttributes(getAttributes() | key);
      }
      else if (getAttribute(key)) {
         setAttributes(getAttributes() ^ key);
      }
   }

   public boolean getAttribute(int key) {
      return (getAttributes() & key) == key;
   }

   private int getType() {
      return type;
   }

   private void setType(int type) {
      this.type = type;
   }
   
   // FIXME: MS_SQL-SERVER and default values...
   private void setType(Integer type) {
      this.type = type != null ? type.intValue() : OpGanttValidator.DEP_DEFAULT;
   }
}
