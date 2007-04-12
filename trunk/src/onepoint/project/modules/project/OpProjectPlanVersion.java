/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

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
   private OpProjectPlan projectPlan;
   private OpUser creator;
   private Set activityVersions;
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

   public OpUser getCreator() {
      return creator;
   }

   public void setCreator(OpUser creator) {
      this.creator = creator;
   }

   public void setActivityVersions(Set activityVersions) {
      this.activityVersions = activityVersions;
   }

   public Set getActivityVersions() {
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

}