/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.components.OpGanttValidator;

import java.sql.Date;
import java.util.HashSet;
import java.util.Iterator;
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
   public final static String ACTIVITY_ASSIGNMENTS = "ActivityAssignments";
   public final static String WORK_PERIODS = "WorkPeriods";
   public final static String DEPENDENCIES = "Dependencies";
   public final static String VERSIONS = "Versions";
   public final static String BASELINE_VERSION = "BaselineVersion";
   public final static String VERSION_NUMBER = "VersionNumber";

   // Calculation modes
   public static final byte EFFORT_BASED = OpGanttValidator.EFFORT_BASED;
   public static final byte INDEPENDENT = OpGanttValidator.INDEPENDENT;

   private Date start;
   private Date finish;
   private byte calculationMode = EFFORT_BASED;
   private boolean progressTracked = true;
   private boolean template;
   private String holidayCalendar = null;
   private OpProjectNode projectNode;
   private Set<OpActivity> activities;
   private Set<OpAssignment> activityAssignments;
   private Set<OpWorkPeriod> workPeriods;
   private Set<OpDependency> dependencies;
   private Set<OpProjectPlanVersion> versions;
   public final static int WORKING_VERSION_NUMBER = -1;
   public final static int ACTUAL_VERSION_NUMBER = 0;
   private String creator;
   private Integer versionNumber = -1; //default versions

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

   /**
    * Gets the id of the holiday calendar.
    *
    * @return a <code>String</code> the id of the holiday calendar, or <code>null</code>
    *         if there isn't any.
    */
   public String getHolidayCalendar() {
      return holidayCalendar;
   }

   /**
    * Sets the id of the holiday calendar.
    *
    * @param holidayCalendar a <code>String</code> the id of the holiday calendar
    *                        used when last modyfing/creating this project plan.
    */
   public void setHolidayCalendar(String holidayCalendar) {
      this.holidayCalendar = holidayCalendar;
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

   /**
    * Returns the baseline version for this project plan, or <code>null</code> if there isn't one.
    *
    * @return a <code>OpProjectPlanVersion</code> object or <code>null</code>.
    */
   public OpProjectPlanVersion getBaselineVersion() {
      for (OpProjectPlanVersion planVersion : versions) {
         if (planVersion.isBaseline()) {
            return planVersion;
         }
      }
      return null;
   }

   public boolean hasWorkingVersion() {
      Set<OpProjectPlanVersion> versions = getVersions();
      for (OpProjectPlanVersion version : versions) {
         if (version.getVersionNumber() == WORKING_VERSION_NUMBER) {
            return true;
         }
      }
      return false;
   }

   public OpProjectPlanVersion getLatestVersion() {
      OpProjectPlanVersion version = null;
      int maxVersion = 0;
      for (OpProjectPlanVersion planVersion : versions) {
         if (planVersion.getVersionNumber() > maxVersion) {
            maxVersion = planVersion.getVersionNumber();
            version = planVersion;
         }
      }
      return version;
   }

   public void setCreator(String creator) {
      this.creator = creator;
   }

   public String getCreator() {
      return creator;
   }

   public void setVersionNumber(Integer versionNumber) {
      this.versionNumber = versionNumber;
   }

   public Integer getVersionNumber() {
      return versionNumber;
   }

   public void incrementVersionNumber() {
      versionNumber++;
   }

   /**
    * @param activity
    * @pre
    * @post
    */
   public void addActivity(OpActivity activity) {
      if (activities == null) {
         activities = new HashSet<OpActivity>();
      }
      activities.add(activity);
   }

   /**
    * @param assignment
    * @pre
    * @post
    */
   public void addAssignment(OpAssignment assignment) {
      if (activityAssignments == null) {
         activityAssignments = new HashSet<OpAssignment>();
      }
      activityAssignments.add(assignment);
   }

   public OpProjectPlanVersion getWorkingVersion() {
      if (getVersions() == null) {
         return null;
      }
      Iterator<OpProjectPlanVersion> versionIt = getVersions().iterator();
      while (versionIt.hasNext()) {
         OpProjectPlanVersion version = versionIt.next();
         if (version.getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
            return version;
         }
      }
      return null;
   }
}
