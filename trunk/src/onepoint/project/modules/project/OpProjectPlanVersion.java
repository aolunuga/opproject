/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.persistence.hibernate.OpPropertyAccessor;
import onepoint.project.modules.calendars.OpHasWorkCalendar;
import onepoint.project.modules.calendars.OpWorkCalendar;
import onepoint.project.modules.project_controlling.OpControllingSheet;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;

public class OpProjectPlanVersion extends OpObject implements OpPermissionable, OpHasWorkCalendar {

   public final static String PROJECT_PLAN_VERSION = "OpProjectPlanVersion";

   public final static String VERSION_NUMBER = "VersionNumber";
   public final static String COMMENT = "Comment";
   public final static String START = "Start";
   public final static String FINISH = "Finish";
   public final static String TEMPLATE = "Template";
   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String ACTIVITY_VERSIONS = "ActivityVersions";
   public final static String ASSIGNMENT_VERSIONS = "AssignmentVersions";
   public final static String WORK_PERIOD_VERSIONS = "WorkPeriodVersions";
   public final static String DEPENDENCY_VERSIONS = "DependencyVersions";
   public final static String CONTROLLING_SHEETS = "ControllingSheets";

   private int versionNumber;
   private String comment;
   private Date start;
   private Date finish;
   private boolean template;
   private Boolean baseline = false;
   private OpProjectPlan projectPlan;
   private String creator;
   private Timestamp recalculated = null;
   private Timestamp checkInTime;
   private Set<OpActivityVersion> activityVersions = new HashSet<OpActivityVersion>();
   private Set assignmentVersions;
   private Set<OpWorkPeriodVersion> workPeriodVersions = null;
   private Set dependencyVersions;
   private Set<OpControllingSheet> controllingSheets;
   private Set<OpWorkBreak> workBreaks;
   private Set<OpPermission> permissions;
   
   private OpWorkCalendar workCalendar = null;
 
   transient private Map<Long, OpActivityVersion> allActivityVersionsByActivityId = null;

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

   public void setActivityVersions(Set<OpActivityVersion> activityVersions) {
      this.activityVersions = activityVersions;
   }

   public Set<OpActivityVersion> getActivityVersions() {
      return activityVersions;
   }
  
   public void setAssignmentVersions(Set assignmentVersions) {
      this.assignmentVersions = assignmentVersions;
   }

   public Set getAssignmentVersions() {
      return assignmentVersions;
   }

   public void addAssignmentVersion(OpAssignmentVersion assignmentVersion) {
      if (getAssignmentVersions() == null) {
         setAssignmentVersions(new HashSet<OpAssignmentVersion>());
      }
      if (getAssignmentVersions().add(assignmentVersion)) {
         assignmentVersion.setPlanVersion(this);
      }
   }

   public void removeAssignmentVersion(OpAssignmentVersion assignmentVersion) {
      if (getAssignmentVersions() == null) {
         return;
      }
      if (getAssignmentVersions().remove(assignmentVersion)) {
         assignmentVersion.setPlanVersion(null);
      }
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
      setBaselineInternal(baseline);
   }

   /**
    * called internally by hibernate
    * @see OpPropertyAccessor
    */
   private void setBaselineInternal(Boolean baseline) {
      this.baseline = (baseline != null) ? baseline : Boolean.FALSE;
   }

   public Set<OpControllingSheet> getControllingSheets() {
      return controllingSheets;
   }

   public void setControllingSheets(Set<OpControllingSheet> controllingSheets) {
      this.controllingSheets = controllingSheets;
   }
   
   // TODO: get rid of this ASAP...
   /**
    * @deprecated
    */
   public Map<Long, OpActivityVersion> getAllActivityVersionsUseOnlyIfRequired() {
      if (allActivityVersionsByActivityId != null) {
         return allActivityVersionsByActivityId;
      }
      if (getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
         // TODO: do something different...
         return null;
      }
      allActivityVersionsByActivityId = new HashMap<Long, OpActivityVersion>();
      for (OpActivityVersion v: getActivityVersions()) {
         if (v.getActivity() != null) {
            allActivityVersionsByActivityId.put(new Long(v.getActivity().getId()), v);
         }
      }
      return allActivityVersionsByActivityId;
   }

   /**
    * @param planVersion
    * @pre
    * @post
    */
   public void removeActivityVersion(OpActivityVersion activityVersion) {
      if (getActivityVersions().remove(activityVersion)) {
         activityVersion.setPlanVersion(null);
      }
   }

   /**
    * @param planVersion
    * @pre
    * @post
    */
   public void addActivityVersion(OpActivityVersion activityVersion) {
      if (getActivityVersions().add(activityVersion)) {
         activityVersion.setPlanVersion(this);         
      }
   }
  
   public Set<OpWorkBreak> getWorkBreaks() {
      return workBreaks;
   }

   private void setWorkBreaks(Set<OpWorkBreak> workBreaks) {
      this.workBreaks = workBreaks;
   }
   
   public void addWorkBreak(OpActivityVersionWorkBreak workBreak) {
      if (getWorkBreaks() == null) {
         setWorkBreaks(new HashSet<OpWorkBreak>());
      }
      if (getWorkBreaks().add(workBreak)) {
         workBreak.setPlanVersion(this);
      }
   }
   
   public void removeWorkBreak(OpActivityVersionWorkBreak workBreak) {
      if (getWorkBreaks() == null) {
         return;
      }
      if (getWorkBreaks().remove(workBreak)) {
         workBreak.setPlanVersion(null);
      }
   }
   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   public Set<OpPermission> getPermissions() {
      return permissions;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#setPermissions(java.util.Set)
    */
   public void setPermissions(Set<OpPermission> permissions) {
      this.permissions = permissions;
   }
   
   public void addPermission(OpPermission permission) {
      Set<OpPermission> perm = getPermissions();
      if (perm == null) {
         perm = new HashSet<OpPermission>();
         setPermissions(perm);
      }
      perm.add(permission);
      permission.setObject(this);
   }

   /**
    * @param opPermission
    * @pre
    * @post
    */
   public void removePermission(OpPermission opPermission) {
      Set<OpPermission> perm = getPermissions();
      if (perm != null) {
         perm.remove(opPermission);
      }
      opPermission.setObject(null);
   }

   public Timestamp getRecalculated() {
      return recalculated;
   }

   public void setRecalculated(Timestamp recalculated) {
      this.recalculated = recalculated;
   }

   public Timestamp getCheckInTime() {
      return checkInTime;
   }

   public void setCheckInTime(Timestamp checkInTime) {
      this.checkInTime = checkInTime;
   }

   public OpWorkCalendar getWorkCalendar() {
      return workCalendar;
   }

   public void setWorkCalendar(OpWorkCalendar workCalendar) {
      this.workCalendar = workCalendar;
   }

   public boolean updateWorkCalendar(OpWorkCalendar newWC) {
      boolean changed = (newWC != null ? newWC.getId() : 0l) != (getWorkCalendar() != null ? getWorkCalendar().getId()
            : 0l);
      if (changed) {
         if (getWorkCalendar() != null) {
            getWorkCalendar().removeProjectPlanVersion(this);
         }
         if (newWC != null) {
            newWC.addProjectPlanVersion(this);
         }
      }
      return changed;
   }

   public void addDependencyVersion(OpDependencyVersion dependency) {
      if (getDependencyVersions() == null) {
         setDependencyVersions(new HashSet<OpDependencyVersion>());
      }
      if (getDependencyVersions().add(dependency)) {
         dependency.setPlanVersion(this);
      }
   }

   public void removeDependencyVersion(OpDependencyVersion dependency) {
      if (getDependencyVersions() == null) {
         return;
      }
      if (getDependencyVersions().remove(dependency)) {
         dependency.setPlanVersion(null);
      }
   }

   public void addWorkPeriodVersion(OpWorkPeriodVersion workPeriod) {
      if (getWorkPeriodVersions() == null) {
         setWorkPeriodVersions(new HashSet<OpWorkPeriodVersion>());
      }
      if (getWorkPeriodVersions().add(workPeriod)) {
         workPeriod.setPlanVersion(this);
      }
   }

   public void removeWorkPeriodVersion(OpWorkPeriodVersion workPeriod) {
      if (getWorkPeriodVersions() == null) {
         return;
      }
      if (getWorkPeriodVersions().remove(workPeriod)) {
         workPeriod.setPlanVersion(null);
      }
   }
}
