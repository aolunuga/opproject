/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.components.OpGanttValidator;

public class OpDependency extends OpObject implements OpDependencyIfc {

   public final static String DEPENDENCY = "OpDependency";

   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String PREDECESSOR_ACTIVITY = "PredecessorActivity";
   public final static String SUCCESSOR_ACTIVITY = "SuccessorActivity";

   public final static int DEPENDENCY_CRITICAL = 65536;
   public final static int DEPENDENCY_CONFLICTING = 65536 * 2;

   public final static int TYPE_MASK = 65536-1;
   
   private OpProjectPlan projectPlan;
   private OpActivity predecessorActivity;
   private OpActivity successorActivity;
   
   private int type = OpGanttValidator.DEP_DEFAULT;

   public void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   public OpProjectPlan getProjectPlan() {
      return projectPlan;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpDependencyIfc#setPredecessorActivity(onepoint.project.modules.project.OpActivity)
    */
   public void setPredecessorActivity(OpActivity predecessorActivity) {
      this.predecessorActivity = predecessorActivity;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpDependencyIfc#getPredecessorActivity()
    */
   public OpActivity getPredecessorActivity() {
      return predecessorActivity;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpDependencyIfc#setSuccessorActivity(onepoint.project.modules.project.OpActivity)
    */
   public void setSuccessorActivity(OpActivity successorActivity) {
      this.successorActivity = successorActivity;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpDependencyIfc#getSuccessorActivity()
    */
   public OpActivity getSuccessorActivity() {
      return successorActivity;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpDependencyIfc#getType()
    */
   public int getDependencyType() {
      return type & TYPE_MASK;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpDependencyIfc#setType(int)
    */
   public void setDependencyType(int type) {
      this.type = (this.type ^ (this.type & TYPE_MASK)) | type;
   }

   private int getAttributes() {
      return (this.type ^ (this.type & TYPE_MASK));
   }

   public void setAttributes(int attibutes) {
      this.type = (this.type & TYPE_MASK) | attibutes;
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
