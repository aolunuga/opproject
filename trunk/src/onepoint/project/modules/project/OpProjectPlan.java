/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.components.OpGanttValidator;

import java.sql.Date;
import java.util.Set;

public class OpProjectPlan extends OpObject {

   public final static String PROJECT_PLAN = "OpProjectPlan";
   
   public final static String START = "Start";
   public final static String FINISH = "Finish";
   public final static String TEMPLATE = "Template";
   public final static String CALCULATION_MODE = "CalculationMode";
   public final static String PROGRESS_TRACKED = "ProgressTracked";
   public final static String NODE = "ProjectNode";
   public final static String ACTIVITIES = "Activities";
   public final static String ACTIVITY_ATTACHMENTS = "ActivityAttachments";
   public final static String ACTIVITY_ASSIGNMENTS = "ActivityAssignments";
   public final static String WORK_PERIODS = "WorkPeriods";
   public final static String DEPENDENCIES = "Dependencies";
   public final static String VERSIONS = "Versions";
   public final static String BASELINE_VERSION = "BaselineVersion";

   // Calculation modes
   public static final byte EFFORT_BASED = OpGanttValidator.EFFORT_BASED;
   public static final byte INDEPENDENT = OpGanttValidator.INDEPENDENT;

   private Date start;
   private Date finish;
   private byte calculationMode = EFFORT_BASED;
   private boolean progressTracked = true;
   private boolean template;
   private OpProjectNode projectNode;
   private Set<OpActivity> activities;
   private Set<OpAttachment> activityAttachments;
   private Set<OpAssignment> activityAssignments;
   private Set<OpWorkPeriod> workPeriods;
   private Set<OpDependency> dependencies;
   private Set<OpProjectPlanVersion> versions;
   private OpProjectPlanVersion baselineVersion;

   public void setStart(Date start) {
      this.start = start;
   }

   public Date getStart() {
      return start;
   }

   public void setFinish(Date finish) {
      this.finish = finish;
   }

   public Date getFinish() {
      return finish;
   }

   public void setCalculationMode(byte mode) {
      this.calculationMode = mode;
   }

   public byte getCalculationMode() {
      return calculationMode;
   }

   public void setProgressTracked(boolean progressTracked) {
      this.progressTracked = progressTracked;
   }

   public boolean getProgressTracked() {
      return progressTracked;
   }

   public void setTemplate(boolean template) {
      this.template = template;
   }

   public boolean getTemplate() {
      return template;
   }

   public void setProjectNode(OpProjectNode projectNode) {
      this.projectNode = projectNode;
   }

   public OpProjectNode getProjectNode() {
      return projectNode;
   }

   public void setActivities(Set<OpActivity> activities) {
      this.activities = activities;
   }

   public Set<OpActivity> getActivities() {
      return activities;
   }

   public void setActivityAttachments(Set<OpAttachment> activityAttachments) {
      this.activityAttachments = activityAttachments;
   }

   public Set<OpAttachment> getActivityAttachments() {
      return activityAttachments;
   }

   public void setActivityAssignments(Set<OpAssignment> activityAssignments) {
      this.activityAssignments = activityAssignments;
   }

   public Set<OpAssignment> getActivityAssignments() {
      return activityAssignments;
   }

   public void setWorkPeriods(Set<OpWorkPeriod> workPeriods) {
      this.workPeriods = workPeriods;
   }

   public Set<OpWorkPeriod> getWorkPeriods() {
      return workPeriods;
   }

   public void setDependencies(Set<OpDependency> dependencies) {
      this.dependencies = dependencies;
   }

   public Set<OpDependency> getDependencies() {
      return dependencies;
   }

   public void setVersions(Set<OpProjectPlanVersion> versions) {
      this.versions = versions;
   }

   public Set<OpProjectPlanVersion> getVersions() {
      return versions;
   }

   /**
    * Copies the start date and end date from the project plan.
    */
   public void copyDatesFromProject() {
      this.setStart(projectNode.getStart());
      if (projectNode.getFinish() != null) {
         this.setFinish(projectNode.getFinish());
      }
      else {
         this.setFinish(projectNode.getStart());
      }
   }

   public OpProjectPlanVersion getBaselineVersion() {
      return baselineVersion;
   }

   public void setBaselineVersion(OpProjectPlanVersion baselineVersion) {
      this.baselineVersion = baselineVersion;
   }
}
