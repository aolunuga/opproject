/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

import java.sql.Date;
import java.util.Set;

public class OpProjectPlanVersion extends OpObject {

   public final static String PROJECT_PLAN_VERSION = "OpProjectPlanVersion";

   public final static String VERSION_NUMBER = "VersionNumber";
   public final static String COMMENT = "Comment";
   public final static String START = "Start";
   public final static String FINISH = "Finish";
   public final static String TEMPLATE = "Template";
   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String ACTIVITY_VERSIONS = "ActivityVersions";
   public final static String ATTACHMENT_VERSIONS = "AttachmentVersions";
   public final static String ASSIGNMENT_VERSIONS = "AssignmentVersions";
   public final static String WORK_PERIOD_VERSIONS = "WorkPeriodVersions";
   public final static String DEPENDENCY_VERSIONS = "DependencyVersions";

   private int versionNumber;
   private String comment;
   private Date start;
   private Date finish;
   private boolean template;
   private Boolean baseline = false;
   private OpProjectPlan projectPlan;
   private String creator;
   private String holidayCalendar = null;
   private Set<OpActivityVersion> activityVersions;
   private Set attachmentVersions;
   private Set assignmentVersions;
   private Set workPeriodVersions;
   private Set dependencyVersions;

   public void setVersionNumber(int versionNumber) {
      this.versionNumber = versionNumber;
   }

   public int getVersionNumber() {
      return versionNumber;
   }

   public void setComment(String comment) {
      this.comment = comment;
   }

   public String getComment() {
      return comment;
   }

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

   public void setTemplate(boolean template) {
      this.template = template;
   }
   
   public boolean getTemplate() {
      return template;
   }

   public void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   public OpProjectPlan getProjectPlan() {
      return projectPlan;
   }

   public String getCreator() {
      return creator;
   }

   public void setCreator(String creator) {
      this.creator = creator;
   }

     /**
    * Gets the id of the holiday calendar.
    * @return  a <code>String</code> the id of the holiday calendar, or <code>null</code>
    * if there isn't any.
    */
   public String getHolidayCalendar() {
      return holidayCalendar;
   }

   /**
    * Sets the id of the holiday calendar.
    * @param holidayCalendar a <code>String</code> the id of the holiday calendar
    * used when last modyfing/creating this project plan.
    */
   public void setHolidayCalendar(String holidayCalendar) {
      this.holidayCalendar = holidayCalendar;
   }

   public void setActivityVersions(Set<OpActivityVersion> activityVersions) {
      this.activityVersions = activityVersions;
   }

   public Set<OpActivityVersion> getActivityVersions() {
      return activityVersions;
   }

   public void setAttachmentVersions(Set attachmentVersions) {
      this.attachmentVersions = attachmentVersions;
   }

   public Set getAttachmentVersions() {
      return attachmentVersions;
   }

   public void setAssignmentVersions(Set assignmentVersions) {
      this.assignmentVersions = assignmentVersions;
   }

   public Set getAssignmentVersions() {
      return assignmentVersions;
   }

   public void setWorkPeriodVersions(Set workPeriodVersions) {
      this.workPeriodVersions = workPeriodVersions;
   }

   public Set getWorkPeriodVersions() {
      return workPeriodVersions;
   }

   public void setDependencyVersions(Set dependencyVersions) {
      this.dependencyVersions = dependencyVersions;
   }

   public Set getDependencyVersions() {
      return dependencyVersions;
   }

   public Boolean isBaseline() {
      return baseline;
   }

   public void setBaseline(Boolean baseline) {
      this.baseline = (baseline != null) ? baseline : Boolean.FALSE;
   }
}
