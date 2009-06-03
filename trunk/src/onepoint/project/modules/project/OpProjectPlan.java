/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpObject;
import onepoint.project.modules.calendars.OpHasWorkCalendar;
import onepoint.project.modules.calendars.OpWorkCalendar;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;

public class OpProjectPlan extends OpObject implements OpPermissionable, OpHasWorkCalendar, OpActivityValuesIfc {

   private static final XLog logger = XLogFactory.getLogger(OpProjectPlan.class);
   
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
   private Timestamp recalculated = null;
   private OpProjectNode projectNode;
   private Set<OpActivity> activities;
   private Set<OpAssignment> activityAssignments;
   private Set<OpWorkPeriod> workPeriods;
   private Set<OpDependency> dependencies;
   private Set<OpProjectPlanVersion> versions;
   public final static int WORKING_VERSION_NUMBER = -1;
   public final static int INITIAL_VERSION_NUMBER = 0;
   private String creator;
   private Integer versionNumber = -1; //default versions
   private Set<OpWorkBreak> workBreaks;
   private Set<OpPermission> permissions;
   
   private OpProjectPlanVersion workingVersion = null;
   private OpProjectPlanVersion baseVersion = null;
   private OpProjectPlanVersion latestVersion = null;
   private boolean implicitBaseline = true;
   
   private Timestamp baseDataChanged = null;
   private OpWorkCalendar workCalendar = null;

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
   
   private double actualEffort = 0d; // Person hours
   private double actualTravelCosts = 0d;
   private double remainingTravelCosts = 0d;
   private double actualPersonnelCosts = 0d;
   private double remainingPersonnelCosts = 0d;
   private double actualMaterialCosts = 0d;
   
   private double remainingEffort = 0d; // Person hours
   private double remainingMaterialCosts = 0d;
   private double actualExternalCosts = 0d;
   private double remainingExternalCosts = 0d;
   private double actualMiscellaneousCosts = 0d;
   private double remainingMiscellaneousCosts = 0d;

   private double baseProceeds = 0d;
   private double actualProceeds = 0d;
   private double remainingProceeds = 0d;
   
   // transient...   
   private Set<OpActivityValuesIfc> topLevelActivities = null;

   public OpProjectPlan() {
      // TODO Auto-generated constructor stub
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

   public void addDependency(OpDependency d) {
      if (getDependencies() == null) {
         setDependencies(new HashSet<OpDependency>());
      }
      if (getDependencies().add(d)) {
         d.setProjectPlan(this);
      }
   }

   public void removeDependency(OpDependency d) {
      if (getDependencies() == null) {
         return;
      }
      if (getDependencies().remove(d)) {
         d.setProjectPlan(null);
      }
   }

   private void setVersions(Set<OpProjectPlanVersion> versions) {
      this.versions = versions;
   }

   public void addProjectPlanVersion (OpProjectPlanVersion v) {
      if (getVersions() == null) {
         setVersions(new HashSet<OpProjectPlanVersion>());
      }
      if (getVersions().add(v)) {
         v.setProjectPlan(this);
      }
   }
   
   public void removeProjectPlanVersion (OpProjectPlanVersion v) {
      if (getVersions() == null) {
         return;
      }
      if (getVersions().remove(v)) {
         v.setProjectPlan(null);
      }
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
   @Deprecated
   public OpProjectPlanVersion getBaselineVersionOld() {
      for (OpProjectPlanVersion planVersion : versions) {
         if (planVersion.isBaseline()) {
            return planVersion;
         }
      }
      return null;
   }

   @Deprecated
   public boolean hasWorkingVersion() {
      Set<OpProjectPlanVersion> versions = getVersions();
      for (OpProjectPlanVersion version : versions) {
         if (version.getVersionNumber() == WORKING_VERSION_NUMBER) {
            return true;
         }
      }
      return false;
   }

   @Deprecated
   public OpProjectPlanVersion getLatestVersionOld() {
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

   public int incrementVersionNumber() {
      setVersionNumber(new Integer(getVersionNumber().intValue() + 1));
      return getVersionNumber().intValue();
   }

   
   public void addTopLevelActivity(OpActivityValuesIfc activity) {
      if (topLevelActivities == null) {
         topLevelActivities = new HashSet<OpActivityValuesIfc>();
      }
      topLevelActivities.add(activity);
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
      if (activities.add(activity)) {
         activity.setProjectPlan(this);
      }
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
      if (activityAssignments.add(assignment)) {
         assignment.setProjectPlan(this);
      }
   }

   public void removeAssignment(OpAssignment assignment) {
      if (activityAssignments == null) {
         return;
      }
      if (activityAssignments.remove(assignment)) {
         assignment.setProjectPlan(null);
      }
   }

   @Deprecated
   public OpProjectPlanVersion getWorkingVersionOld() {
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

   public Set<OpWorkBreak> getWorkBreaks() {
      return workBreaks;
   }

   private void setWorkBreaks(Set<OpWorkBreak> workBreaks) {
      this.workBreaks = workBreaks;
   }
   
   public void addWorkBreak(OpActivityWorkBreak workBreak) {
      if (getWorkBreaks() == null) {
         setWorkBreaks(new HashSet<OpWorkBreak>());
      }
      if (getWorkBreaks().add(workBreak)) {
         workBreak.setProjectPlan(this);
      }
   }
   
   public void removeWorkBreak(OpActivityWorkBreak workBreak) {
      if (getWorkBreaks() == null) {
         return;
      }
      if (getWorkBreaks().remove(workBreak)) {
         workBreak.setProjectPlan(null);
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

   public OpProjectPlanVersion getBaseVersion() {
      return baseVersion;
   }

   public void setBaseVersion(OpProjectPlanVersion baseVersion) {
      this.baseVersion = baseVersion;
   }

   public boolean isImplicitBaseline() {
      return implicitBaseline;
   }

   public void setImplicitBaseline(boolean implicitBaseline) {
      this.implicitBaseline = implicitBaseline;
   }

   // FIXME: MS_SQL-SERVER and default values...
   public void setImplicitBaseline(Boolean implicitBaseline) {
      this.implicitBaseline = implicitBaseline != null ? implicitBaseline.booleanValue() : true;
   }
   
   public OpProjectPlanVersion getWorkingVersion() {
      return workingVersion;
   }

   public void setWorkingVersion(OpProjectPlanVersion workingVersion) {
      this.workingVersion = workingVersion;
   }

   public OpProjectPlanVersion getLatestVersion() {
      return latestVersion;
   }
   
   public void setLatestVersion(OpProjectPlanVersion latestVersion) {
      this.latestVersion = latestVersion;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpHasWorkCalendar#getWorkCalendar()
    */
   public OpWorkCalendar getWorkCalendar() {
      return workCalendar;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpHasWorkCalendar#setWorkCalendar(onepoint.project.modules.calendars.OpWorkCalendar)
    */
   public void setWorkCalendar(OpWorkCalendar workCalendar) {
      this.workCalendar = workCalendar;
   }

   public Timestamp getBaseDataChanged() {
      return baseDataChanged;
   }

   private void setBaseDataChanged(Timestamp touched) {
      this.baseDataChanged = touched;
   }
   
   public void touch() {
      setBaseDataChanged(new Timestamp(System.currentTimeMillis()));
   }

   public boolean updateWorkCalendar(OpWorkCalendar newWC) {
      boolean changed = (newWC != null ? newWC.getId() : 0l) != (getWorkCalendar() != null ? getWorkCalendar().getId()
            : 0l);
      if (changed) {
         if (getWorkCalendar() != null) {
            getWorkCalendar().removeProjectPlan(this);
         }
         if (newWC != null) {
            newWC.addProjectPlan(this);
         }
         touch();
      }
      // update working version:
      if (getWorkingVersion() != null) {
         changed = getWorkingVersion().updateWorkCalendar(newWC) || changed;
      }
      
      return changed;
   }

   public void addWorkPeriod(OpWorkPeriod wp) {
      if (getWorkPeriods() == null) {
         setWorkPeriods(new HashSet<OpWorkPeriod>());
      }
      if (getWorkPeriods().add(wp)) {
         wp.setProjectPlan(this);
      }
   }

   public void removeWorkPeriod(OpWorkPeriod wp) {
      if (getWorkPeriods() == null) {
         return;
      }
      if (getWorkPeriods().remove(wp)) {
         wp.setProjectPlan(null);
      }
   }
   
   @Override
	public String toString() {
	   StringBuffer b = new StringBuffer();
	   b.append("OpProjectPlan: {");
	   b.append("S: "+start);
	   b.append("F: "+finish);
		return b.toString();
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
      return actualEffort;
   }

   public void setActualEffort(double actualEffort) {
      this.actualEffort = actualEffort;
   }

   public double getActualTravelCosts() {
      return actualTravelCosts;
   }

   public void setActualTravelCosts(double actualTravelCosts) {
      this.actualTravelCosts = actualTravelCosts;
   }

   public double getRemainingTravelCosts() {
      return remainingTravelCosts;
   }

   public void setRemainingTravelCosts(double remainingTravelCosts) {
      this.remainingTravelCosts = remainingTravelCosts;
   }

   public double getActualPersonnelCosts() {
      return actualPersonnelCosts;
   }

   public void setActualPersonnelCosts(double actualPersonnelCosts) {
      this.actualPersonnelCosts = actualPersonnelCosts;
   }

   public double getRemainingPersonnelCosts() {
      return remainingPersonnelCosts;
   }

   public void setRemainingPersonnelCosts(double remainingPersonnelCosts) {
      this.remainingPersonnelCosts = remainingPersonnelCosts;
   }

   public double getActualMaterialCosts() {
      return actualMaterialCosts;
   }

   public void setActualMaterialCosts(double actualMaterialCosts) {
      this.actualMaterialCosts = actualMaterialCosts;
   }

   public double getRemainingEffort() {
      return remainingEffort;
   }

   public void setRemainingEffort(double remainingEffort) {
      this.remainingEffort = remainingEffort;
   }

   public double getRemainingMaterialCosts() {
      return remainingMaterialCosts;
   }

   public void setRemainingMaterialCosts(double remainingMaterialCosts) {
      this.remainingMaterialCosts = remainingMaterialCosts;
   }

   public double getActualExternalCosts() {
      return actualExternalCosts;
   }

   public void setActualExternalCosts(double actualExternalCosts) {
      this.actualExternalCosts = actualExternalCosts;
   }

   public double getRemainingExternalCosts() {
      return remainingExternalCosts;
   }

   public void setRemainingExternalCosts(double remainingExternalCosts) {
      this.remainingExternalCosts = remainingExternalCosts;
   }

   public double getActualMiscellaneousCosts() {
      return actualMiscellaneousCosts;
   }

   public void setActualMiscellaneousCosts(double actualMiscellaneousCosts) {
      this.actualMiscellaneousCosts = actualMiscellaneousCosts;
   }

   public double getRemainingMiscellaneousCosts() {
      return remainingMiscellaneousCosts;
   }

   public void setRemainingMiscellaneousCosts(double remainingMiscellaneousCosts) {
      this.remainingMiscellaneousCosts = remainingMiscellaneousCosts;
   }

   public double getBaseProceeds() {
      return baseProceeds;
   }

   public void setBaseProceeds(double baseProceeds) {
      this.baseProceeds = baseProceeds;
   }

   public double getActualProceeds() {
      return actualProceeds;
   }

   public void setActualProceeds(double actualProceeds) {
      this.actualProceeds = actualProceeds;
   }

   public double getRemainingProceeds() {
      return remainingProceeds;
   }

   public void setRemainingProceeds(double remainingProceeds) {
      this.remainingProceeds = remainingProceeds;
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

   public void handleSubActivityProgress(OpActivity.OpProgressDelta delta) {
      // this is tricky and not very scalable, butt will not be called to often:
      updateTopLevelActivities();
      OpActivity.applyDelta(this, delta);

      setComplete(getCompleteFromTracking(getProgressTracked()));
      
      // look for all linking programs:
      if (getProjectNode().getProgramActivityVersions() != null) {
         Set<OpActivity> pActs = new HashSet<OpActivity>();
         for (OpActivity pa: getProjectNode().getProgramActivities()) {
            pa.handleSubProjectProgress(this, delta);
         }
      }
   }

   private void updateTopLevelActivities() {
      // topLevelActivities = null;
      for(OpActivity a : getActivities()) {
         if (a.getOutlineLevel() == 0 && !a.getDeleted()) {
            addTopLevelActivity(a);
         }
      }
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

   public boolean hasSubActivities() {
      return true;
   }

}
