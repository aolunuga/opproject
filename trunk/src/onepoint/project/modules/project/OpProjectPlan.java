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

   // Calculation modes
   public static final byte EFFORT_BASED = OpGanttValidator.EFFORT_BASED;
   public static final byte INDEPENDENT = OpGanttValidator.INDEPENDENT;

   private Date start;
   private Date finish;
   private byte calculationMode = EFFORT_BASED;
   private boolean progressTracked = true;
   private boolean template;
   private OpProjectNode projectNode;
   private Set activities;
   private Set activityAttachments;
   private Set activityAssignments;
   private Set workPeriods;
   private Set dependencies;
   private Set versions;

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

   public void setActivities(Set activities) {
      this.activities = activities;
   }

   public Set getActivities() {
      return activities;
   }

   public void setActivityAttachments(Set activityAttachments) {
      this.activityAttachments = activityAttachments;
   }

   public Set getActivityAttachments() {
      return activityAttachments;
   }

   public void setActivityAssignments(Set activityAssignments) {
      this.activityAssignments = activityAssignments;
   }

   public Set getActivityAssignments() {
      return activityAssignments;
   }

   public void setWorkPeriods(Set workPeriods) {
      this.workPeriods = workPeriods;
   }

   public Set getWorkPeriods() {
      return workPeriods;
   }

   public void setDependencies(Set dependencies) {
      this.dependencies = dependencies;
   }

   public Set getDependencies() {
      return dependencies;
   }

   public void setVersions(Set versions) {
      this.versions = versions;
   }

   public Set getVersions() {
      return versions;
   }

}
