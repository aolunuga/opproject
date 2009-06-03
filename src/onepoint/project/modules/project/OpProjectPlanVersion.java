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

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpObject;
import onepoint.persistence.hibernate.OpPropertyAccessor;
import onepoint.project.modules.calendars.OpHasWorkCalendar;
import onepoint.project.modules.calendars.OpWorkCalendar;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_controlling.OpControllingSheet;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;

public class OpProjectPlanVersion extends OpObject implements OpPermissionable, OpHasWorkCalendar, OpActivityValuesIfc {

   private static final XLog logger = XLogFactory.getLogger(OpProjectPlanVersion.class);

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
   
   private double duration = 0d;
   private double leadTime = 0d;
   private double followUpTime = 0d;
   private double complete = 0d;

   private double baseEffort = 0d; // Person hours
   private double baseTravelCosts = 0d;
   private double basePersonnelCosts = 0d;
   private double baseMaterialCosts = 0d;
   private double baseExternalCosts = 0d;
   private double baseMiscellaneousCosts = 0d;

   private double unassignedEffort = 0d; // Person hours
   
   private double baseProceeds = 0d;

   private OpWorkCalendar workCalendar = null;
 
   transient private Map<Long, OpActivityVersion> allActivityVersionsByActivityId = null;

   // transient...
   private Set<OpActivityValuesIfc> topLevelActivities = null;

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

   public double getBaseEffort() {
      return baseEffort;
   }

   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

   public double getBaseTravelCosts() {
      return baseTravelCosts;
   }

   public void setBaseTravelCosts(double baseTravelCosts) {
      this.baseTravelCosts = baseTravelCosts;
   }

   public double getBasePersonnelCosts() {
      return basePersonnelCosts;
   }

   public void setBasePersonnelCosts(double basePersonnelCosts) {
      this.basePersonnelCosts = basePersonnelCosts;
   }

   public double getBaseMaterialCosts() {
      return baseMaterialCosts;
   }

   public void setBaseMaterialCosts(double baseMaterialCosts) {
      this.baseMaterialCosts = baseMaterialCosts;
   }

   public double getBaseExternalCosts() {
      return baseExternalCosts;
   }

   public void setBaseExternalCosts(double baseExternalCosts) {
      this.baseExternalCosts = baseExternalCosts;
   }

   public double getBaseMiscellaneousCosts() {
      return baseMiscellaneousCosts;
   }

   public void setBaseMiscellaneousCosts(double baseMiscellaneousCosts) {
      this.baseMiscellaneousCosts = baseMiscellaneousCosts;
   }

   public double getUnassignedEffort() {
      return unassignedEffort;
   }

   public void setUnassignedEffort(double unassignedEffort) {
      this.unassignedEffort = unassignedEffort;
   }

   public double getActualEffort() {
      return getProjectPlan().getActualEffort();
   }

   public void setActualEffort(double actualEffort) {
   }

   public double getActualTravelCosts() {
      return getProjectPlan().getActualTravelCosts();
   }

   public void setActualTravelCosts(double actualTravelCosts) {
   }

   public double getRemainingTravelCosts() {
      return getProjectPlan().getRemainingTravelCosts();
   }

   public void setRemainingTravelCosts(double remainingTravelCosts) {
   }

   public double getActualPersonnelCosts() {
      return getProjectPlan().getActualPersonnelCosts();
   }

   public void setActualPersonnelCosts(double actualPersonnelCosts) {
   }

   public double getRemainingPersonnelCosts() {
      return getProjectPlan().getRemainingPersonnelCosts();
   }

   public void setRemainingPersonnelCosts(double remainingPersonnelCosts) {
   }

   public double getActualMaterialCosts() {
      return getProjectPlan().getActualMaterialCosts();
   }

   public void setActualMaterialCosts(double actualMaterialCosts) {
   }

   public double getRemainingEffort() {
      return getProjectPlan().getRemainingEffort();
   }

   public void setRemainingEffort(double remainingEffort) {
   }

   public double getRemainingMaterialCosts() {
      return getProjectPlan().getRemainingMaterialCosts();
   }

   public void setRemainingMaterialCosts(double remainingMaterialCosts) {
   }

   public double getActualExternalCosts() {
      return getProjectPlan().getActualExternalCosts();
   }

   public void setActualExternalCosts(double actualExternalCosts) {
   }

   public double getRemainingExternalCosts() {
      return getProjectPlan().getRemainingExternalCosts();
   }

   public void setRemainingExternalCosts(double remainingExternalCosts) {
   }

   public double getActualMiscellaneousCosts() {
      return getProjectPlan().getActualMiscellaneousCosts();
   }

   public void setActualMiscellaneousCosts(double actualMiscellaneousCosts) {
   }

   public double getRemainingMiscellaneousCosts() {
      return getProjectPlan().getRemainingMiscellaneousCosts();
   }

   public void setRemainingMiscellaneousCosts(double remainingMiscellaneousCosts) {
   }

   public double getBaseProceeds() {
      return baseProceeds;
   }

   public void setBaseProceeds(double baseProceeds) {
      this.baseProceeds = baseProceeds;
   }

   public double getActualProceeds() {
      return getProjectPlan().getActualProceeds();
   }

   public void setActualProceeds(double actualProceeds) {
   }

   public double getRemainingProceeds() {
      return getProjectPlan().getRemainingProceeds();
   }

   public void setRemainingProceeds(double remainingProceeds) {
   }

   public Boolean getBaseline() {
      return baseline;
   }

   public double getDuration() {
      return duration;
   }

   public void setDuration(double duration) {
      this.duration = duration;
   }

   public double getLeadTime() {
      return leadTime;
   }

   public void setLeadTime(double leadTime) {
      this.leadTime = leadTime;
   }

   public double getFollowUpTime() {
      return followUpTime;
   }

   public void setFollowUpTime(double followUpTime) {
      this.followUpTime = followUpTime;
   }

   public double getComplete() {
      return complete;
   }

   public void setComplete(double complete) {
      this.complete = complete;
   }

   public double getCompleteFromTracking(boolean progressTracked) {
      return OpGanttValidator.getCompleteFromTracking(this, progressTracked);
   }

   public void addChildComplete(double childComplete, double childBaseEffort) {
      OpActivity.addWeightedComplete(this, childComplete, childBaseEffort);
   }
   
   public OpActivityValuesIfc getParent() {
      return null;
   }

   public byte getType() {
      return OpGanttValidator.PROJECT_PLAN;
   }

   public boolean isImported() {
      return false;
   }

   public void resetActualValues() {
      OpActivity.resetActualValues(this);
   }
   
   public void resetAggregatedValues() {
      OpActivity.resetValues(this);

      setStart(null);
      setFinish(null);
   }

   public void addTopLevelActivity(OpActivityValuesIfc a) {
      if (topLevelActivities == null) {
         topLevelActivities = new HashSet<OpActivityValuesIfc>();
      }
      topLevelActivities.add(a);
   }

   private void updateTopLevelActivities() {
      for(OpActivityVersion a : getActivityVersions()) {
         if (a.getOutlineLevel() == 0) {
            addTopLevelActivity(a);
         }
      }
   }

   public void handleSubActivityProgress(OpActivity.OpProgressDelta delta) {
      updateTopLevelActivities();
      OpActivity.applyDelta(this, delta);
   }

   public double getOpenEffort() {
      return getRemainingEffort() + getUnassignedEffort();
   }

   public Set getTrackedSubElements() {
      return topLevelActivities;
   }

   public boolean isIndivisible() {
      return OpGanttValidator.isIndivisibleElement(this);
   }

   public boolean isTrackingLeaf() {
      return false;
   }

   @Override
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(locator());
      b.append(":");
      if (getProjectPlan() != null && getProjectPlan().getProjectNode() != null) {
         b.append(getProjectPlan().getProjectNode().getName());
      }
      b.append(":");
      b.append(getVersionNumber());
      return b.toString();
   }

   public boolean hasSubActivities() {
      return true;
   }

}
