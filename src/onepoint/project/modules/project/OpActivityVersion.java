/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.project.modules.project.OpActivity.OpProgressDelta;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpCostRecord;

public class OpActivityVersion extends OpActivityBase implements OpActivityIfc, OpGanttValidator.ProgressTrackableEntityIfc {
   public final static String ACTIVITY_VERSION = "OpActivityVersion";

   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String TYPE = "Type";
   public final static String ATTRIBUTES = "Attributes";
   public final static String SEQUENCE = "Sequence";
   public final static String OUTLINE_LEVEL = "OutlineLevel";
   public final static String START = "Start";
   public final static String FINISH = "Finish";
   public final static String DURATION = "Duration";
   public final static String COMPLETE = "Complete";
   public final static String PRIORITY = "Priority";
   public final static String BASE_EFFORT = "BaseEffort";
   public final static String BASE_TRAVEL_COSTS = "BaseTravelCosts";
   public final static String BASE_PERSONNEL_COSTS = "BasePersonnelCosts";
   public final static String BASE_MATERIAL_COSTS = "BaseMaterialCosts";
   public final static String BASE_EXTERNAL_COSTS = "BaseExternalCosts";
   public final static String BASE_MISCELLANEOUS_COSTS = "BaseMiscellaneousCosts";
   public final static String EXPANDED = "Expanded";
   public final static String TEMPLATE = "Template";
   public final static String CATEGORY = "Category";
   public final static String ACTIVITY = "Activity";
   public final static String SUPER_ACTIVITY_VERSION = "SuperActivityVersion";
   public final static String SUB_ACTIVITIE_VERSIONS = "SubActivitieVersions";
   public final static String PLAN_VERSION = "PlanVersion";
   public final static String ASSIGNMENT_VERSIONS = "AssignmentVersions";
   public final static String WORK_PERIOD_VERSIONS = "WorkPeriodVersions";
   public final static String TASK_VERSIONS = "TaskVersions";
   public final static String SUCCESSOR_VERSIONS = "SuccessorVersions";
   public final static String PREDECESSOR_VERSIONS = "PredecessorVersions";
   public final static String ATTACHMENT_VERSIONS = "AttachmentVersions";

   // Activity attributes
   public final static int MANDATORY = OpGanttValidator.MANDATORY;
   public final static int LINKED = OpGanttValidator.LINKED;
   public final static int HAS_ATTACHMENTS = OpGanttValidator.HAS_ATTACHMENTS;
   public final static int HAS_COMMENTS = OpGanttValidator.HAS_COMMENTS;

   //Start & End date indexes
   public final static int START_DATE_LIST_INDEX = 0;
   public final static int END_DATE_LIST_INDEX = 1;

   private String name;
   private String description;
   private byte type = STANDARD;
   private int attributes;
   private int sequence;
   private byte outlineLevel;
   private Date start;
   private Date finish;
   private double duration;
   private double leadTime;
   private double followUpTime;
   private double complete;
   private byte priority; // Priority 1-9 (zero means undefined)
   private double baseEffort; // Person hours
   private double unassignedEffort; // Person hours
   private double baseTravelCosts;
   private double basePersonnelCosts;
   private double baseMaterialCosts;
   private double baseExternalCosts;
   private double baseMiscellaneousCosts;
   private double baseProceeds;
   private double payment;
   private boolean expanded;
   private boolean template;
   private Double effortBillable;
   private OpActivityCategory category;
   OpActivity activity;
   private OpActivityVersion superActivityVersion;
   private Set subActivityVersions; // *** Could also be a List (via sequence)
   private OpProjectPlanVersion planVersion;
   private Set<OpAssignmentVersion> assignmentVersions;
   private Set workPeriodVersions;
   private Set successorVersions;
   private Set<OpDependencyVersion> predecessorVersions;
   private Set<OpAttachmentVersion> attachmentVersions;
   private OpResource responsibleResource;
   private Set<OpWorkBreak> workBreaks;

   
   private ActualValues actualValues = null;
   private Set<OpActionVersion> actions;

   private OpActivityVersion masterActivityVersion;  // Program Management

   private OpActivity masterActivity;  // Program Management @deprecated
   private OpProjectNode subProject;
   
   private Boolean publicActivity = null;
   private Set<OpActivityVersion> shallowCopies = null;
   /**
    * 
    */
   public OpActivityVersion() {
   }

   public void setupFromActivity(OpActivityIfc a) {
      this.attributes = a.getAttributes();

      this.baseEffort = a.getBaseEffort();
      this.baseExternalCosts = a.getBaseExternalCosts();
      this.baseMaterialCosts = a.getBaseMaterialCosts();
      this.baseMiscellaneousCosts = a.getBaseMiscellaneousCosts();
      this.basePersonnelCosts = a.getBasePersonnelCosts();
      this.baseProceeds = a.getBaseProceeds();
      this.baseTravelCosts = a.getBaseTravelCosts();
      
      this.category = a.getCategory();
      this.complete = a.getComplete();
      this.description = a.getDescription();
      this.duration = a.getDuration();
      this.effortBillable = a.getEffortBillable();
      this.expanded = a.getExpanded();
      this.finish = a.getFinish();
      this.followUpTime = a.getFollowUpTime();
      this.leadTime = a.getLeadTime();
      this.name = a.getName();
      this.payment = a.getPayment();
      this.priority = a.getPriority();
      this.start = a.getStart();
      this.type = a.getType();
      
      this.outlineLevel = 0;
      this.sequence = 0;
   }   
   
   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDescription() {
      return description;
   }

   public void setType(byte type) {
      this.type = type;
   }

   public byte getType() {
      return type;
   }

   public void setAttributes(int attributes) {
      this.attributes = attributes;
      setPublicActivity((attributes & OpGanttValidator.EXPORTED_TO_SUPERPROJECT) == OpGanttValidator.EXPORTED_TO_SUPERPROJECT);
   }

   public int getAttributes() {
      return attributes;
   }

   public void setSequence(int sequence) {
      this.sequence = sequence;
   }

   public int getSequence() {
      return sequence;
   }

   public void setOutlineLevel(byte outlineLevel) {
      this.outlineLevel = outlineLevel;
   }

   public byte getOutlineLevel() {
      return outlineLevel;
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

   public void setDuration(double duration) {
      this.duration = duration;
   }

   public double getDuration() {
      return duration;
   }

   public double getLeadTime() {
      return leadTime;
   }

   public void setLeadTime(double leadTime) {
      this.leadTime = leadTime;
   }

   // FIXME: MS_SQL-SERVER and default values...
   public void setLeadTime(Double leadTime) {
      this.leadTime = leadTime != null ? leadTime.doubleValue() : 0d;
   }

   public double getFollowUpTime() {
      return followUpTime;
   }

   public void setFollowUpTime(double followUpTime) {
      this.followUpTime = followUpTime;
   }

   // FIXME: MS_SQL-SERVER and default values...
   public void setFollowUpTime(Double followUpTime) {
      this.followUpTime = followUpTime != null ? followUpTime.doubleValue() : 0d;
   }

   public void setComplete(double complete) {
      this.complete = complete;
   }

   public double getComplete() {
      return complete;
   }

   public void setPriority(byte priority) {
      this.priority = priority;
   }

   public byte getPriority() {
      return priority;
   }

   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

   public double getBaseEffort() {
      return baseEffort;
   }

   public void setBaseTravelCosts(double baseTravelCosts) {
      this.baseTravelCosts = baseTravelCosts;
   }

   public double getBaseTravelCosts() {
      return baseTravelCosts;
   }

   public void setBasePersonnelCosts(double basePersonnelCosts) {
      this.basePersonnelCosts = basePersonnelCosts;
   }

   public double getBasePersonnelCosts() {
      return basePersonnelCosts;
   }

   public void setBaseMaterialCosts(double baseMaterialCosts) {
      this.baseMaterialCosts = baseMaterialCosts;
   }

   public double getBaseMaterialCosts() {
      return baseMaterialCosts;
   }

   public void setBaseExternalCosts(double baseExternalCosts) {
      this.baseExternalCosts = baseExternalCosts;
   }

   public double getBaseExternalCosts() {
      return baseExternalCosts;
   }

   public void setBaseMiscellaneousCosts(double baseMiscellaneousCosts) {
      this.baseMiscellaneousCosts = baseMiscellaneousCosts;
   }

   public double getBaseMiscellaneousCosts() {
      return baseMiscellaneousCosts;
   }

   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
   }

   public boolean getExpanded() {
      return expanded;
   }

   public void setTemplate(boolean template) {
      this.template = template;
   }

   public boolean getTemplate() {
      return template;
   }

   public double getEffortBillable() {
      return effortBillable == null ? OpActivity.DEFAULT_BILLABLE : effortBillable.doubleValue();
   }

   public void setEffortBillable(Double effortBillable) {
      this.effortBillable = effortBillable;
   }

   public void setCategory(OpActivityCategory category) {
      this.category = category;
   }

   public OpActivityCategory getCategory() {
      return category;
   }

   public void setActivity(OpActivity activity) {
      this.activity = activity;
   }

   public OpActivityValuesIfc getElementForActualValues() {
      if (getMasterActivityVersion() != null) {
         return getMasterActivityVersion().getActivity();
      }
      else if (getSubProject() != null) {
         return getSubProject().getPlan();
      }
      return getActivity();
   }

   public OpActivity getActivityForAdditionalObjects() {
      if (getMasterActivityVersion() != null) {
         return getMasterActivityVersion().getActivity();
      }
      return getActivity();
   }

   public OpActivity getActivity() {
      return activity;
   }

   public void setSuperActivityVersion(OpActivityVersion superActivityVersion) {
      this.superActivityVersion = superActivityVersion;
   }

   public OpActivityVersion getSuperActivityVersion() {
      return superActivityVersion;
   }

   public void setSubActivityVersions(Set subActivityVersions) {
      this.subActivityVersions = subActivityVersions;
   }

   public Set<OpActivityVersion> getSubActivityVersions() {
      return subActivityVersions;
   }

   public void setPlanVersion(OpProjectPlanVersion planVersion) {
      this.planVersion = planVersion;
   }

   public OpProjectPlanVersion getPlanVersion() {
      return planVersion;
   }

   public void setAssignmentVersions(Set<OpAssignmentVersion> assignmentVersions) {
      this.assignmentVersions = assignmentVersions;
   }

   // deprecated
   public Set<OpAssignmentVersion> getAssignmentVersions() {
      return assignmentVersions;
   }

   public Set<? extends OpAssignmentIfc> getAssignments() {
      return assignmentVersions;
   }

   public void setWorkPeriodVersions(Set workPeriodVersions) {
      this.workPeriodVersions = workPeriodVersions;
   }
   
   public void addAssignmentVersion(OpAssignmentVersion assignment) {
      if (getAssignmentVersions() == null) {
         setAssignmentVersions(new HashSet<OpAssignmentVersion>());
      }
      if (getAssignmentVersions().add(assignment)) {
         assignment.setActivityVersion(this);
      }
   }

   public void removeAssignmentVersion(OpAssignmentVersion assignment) {
      if (getAssignmentVersions() == null) {
         return;
      }
      if (getAssignmentVersions().remove(assignment)) {
         assignment.setActivityVersion(null);
      }
   }

   public Set getWorkPeriodVersions() {
      return workPeriodVersions;
   }

   public Set<OpWorkPeriodVersion> getWorkPeriods() {
      return workPeriodVersions;
   }
   
   public void setSuccessorVersions(Set successorVersions) {
      this.successorVersions = successorVersions;
   }

   // deprecated
   public Set getSuccessorVersions() {
      return successorVersions;
   }

   public Set<OpDependencyVersion> getSuccessorDependencies() {
      return successorVersions;
   }

   public void setPredecessorVersions(Set<OpDependencyVersion> predecessorVersions) {
      this.predecessorVersions = predecessorVersions;
   }

   // deprecated
   public Set<OpDependencyVersion> getPredecessorVersions() {
      return predecessorVersions;
   }

   public Set<OpDependencyVersion> getPredecessorDependencies() {
      return predecessorVersions;
   }

   public void setAttachmentVersions(Set<OpAttachmentVersion> attachmentVersions) {
      this.attachmentVersions = attachmentVersions;
   }

   // deprecated
   public Set<OpAttachmentVersion> getAttachmentVersions() {
      return attachmentVersions;
   }

   public Set<? extends OpAttachmentIfc> getAttachments() {
      return attachmentVersions;
   }

   public OpResource getResponsibleResource() {
      return responsibleResource;
   }

   public void setResponsibleResource(OpResource responsibleResource) {
      this.responsibleResource = responsibleResource;
   }

   public double getBaseProceeds() {
      return baseProceeds;
   }

   public void setBaseProceeds(double baseProceeds) {
      this.baseProceeds = baseProceeds;
   }

   public double getPayment() {
      return payment;
   }

   public void setPayment(double payment) {
      this.payment = payment;
   }

   /**
    * Returns a <code>List</code> containing two dates: a start date and an end date.
    * if the activity is a STANDARD one then the list will contain it's start and end dates,
    * if the activity is a TASK then the list will contain it's start date. If the end date will be chosen
    * from the activity's end date, the project's end date and the project'a plan end date. The first one
    * (in this order) that is found not null will be returned.
    *
    * @return - a <code>List</code> containing two dates: a start date and an end date.
    *         if the activity is a STANDARD one then the list will contain it's start and end dates,
    *         if the activity is a TASK then the list will contain it's start date. If the end date will be chosen
    *         from the activity's end date, the project's end date and the project'a plan end date. The first one
    *         (in this order) that is found not null will be returned.
    */
   public List<Date> getStartEndDateByType() {
      List<Date> dates = null;

      if (type == STANDARD) {
         dates = new ArrayList<Date>();
         dates.add(START_DATE_LIST_INDEX, start);
         dates.add(END_DATE_LIST_INDEX, finish);
      }

      if (type == TASK) {
         dates = new ArrayList<Date>();
         dates.add(START_DATE_LIST_INDEX, start);
         if (finish != null) {
            dates.add(END_DATE_LIST_INDEX, finish);
         }
         else {
            if (planVersion.getProjectPlan().getProjectNode().getFinish() != null) {
               dates.add(END_DATE_LIST_INDEX, planVersion.getProjectPlan().getProjectNode().getFinish());
            }
            else {
               dates.add(END_DATE_LIST_INDEX, planVersion.getProjectPlan().getFinish());
            }
         }
      }

      return dates;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpCustomClassable#getCustomClass()
    */
   public Class getCustomClass() {
      return OpActivity.class;
   }

   /**
    * Called from OpActivity whenever progress needs to be updated for the Working Version
    * (for now, I cannot tell whether this is relevant, by maybe someone knows...)
    * @param delta      delta information collected for activity/assignment...
    * @param baseWeighting method of aggregation for super-ActivityVersions...
    */
   public void updateComplete(OpProgressDelta delta) {
      if (isProgressTracked()) {
         if (isDiskreteActivity()) {
            // The Problem: the switch from none-diskrete to diskret eis determined by actual effort and remainig effort.
            // So this might change anytime...
            // step down one level and collect complete values (very ugly...)
            double complete = 0d;
            double oldComplete = getComplete();
            if (getSubActivityVersions() != null && !getSubActivityVersions().isEmpty()) {
               double factor = getSubActivityVersions().size();
               for (OpActivityVersion a: getSubActivityVersions()) {
                  complete += a.getComplete();
               }
               complete = complete / factor;
            }
            else if (getElementForActualValues() != null) {
               complete = getElementForActualValues().getComplete();
            }
            setComplete(complete);
         }
         else {
            setComplete(OpGanttValidator.calculateCompleteValue(getActualEffort(), getBaseEffort(), getOpenEffort()));
         }
      }
      if (getSuperActivityVersion() != null) {
         getSuperActivityVersion().updateComplete(delta);
      }
   }
   
   public boolean hasSubActivities() {
      return OpActivity.hasSubActivities(this);
   }

   public boolean hasAggregatedValues() {
      return OpActivity.hasSubActivities(this) && !isImported();
   }

   public void resetActualValues() {
      actualValues = null;
   }
   
   public void resetAggregatedValues() {
      OpActivity.resetValues(this);
   }
   
   public boolean definesStartFinish() {
      return OpActivity.definesStartFinish(this);
   }
   
   private void initActualValues() {
      if (actualValues == null) {
         actualValues = new ActualValues();
      }
   }
   
   
   public void setActualEffort(double actualEffort) {
      initActualValues();
      actualValues.setActualEffort(actualEffort);
   }
   
   public void setRemainingEffort(double remainingEffort) {
      initActualValues();
      actualValues.setRemainingEffort(remainingEffort);
   }

   public double getActualEffort() {
      if (actualValues != null) {
         return actualValues.getActualEffort();
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getActualEffort();
      }
      else {
         return 0d;
      }
   }
   
   public double getOpenEffort() {
      if (actualValues != null) {
         return actualValues.getRemainingEffort() + getUnassignedEffort();
      }
      if (getElementForActualValues() != null) {
         return getElementForActualValues().getOpenEffort();
      }
      return getBaseEffort();
   }
   
   public double getRemainingEffort() {
      if (actualValues != null) {
         return actualValues.getRemainingEffort();
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getRemainingEffort();
      }
      else {
         return 0d;
      }
   }
   
   public boolean isZero() {
      return getOpenEffort() == 0d && getActualEffort() == 0d;
   }
   
   public boolean isMilestone() {
      return getType() == OpActivity.MILESTONE;
   }
   
   public boolean isProgressTracked() {
      return getPlanVersion().getProjectPlan().getProgressTracked()
            || getType() == OpActivityVersion.ADHOC_TASK || hasAggregatedValues();
   }
   
   public double getCompleteFromTracking(boolean progressTracked) {
      if (isImported()) {
         return getComplete();
      }
      return OpGanttValidator.getCompleteFromTracking(this, progressTracked);
   }
   
   public void addSubActivityVersion(OpActivityVersion v) {
      if (getSubActivityVersions() == null) {
         setSubActivityVersions(new HashSet<OpActivityVersion>());
      }
      if (getSubActivityVersions().add(v)) {
         v.setSuperActivityVersion(this);
      }
   }
   
   public void removeSubActivityVersion(OpActivityVersion v) {
      if (getSubActivityVersions() == null) {
         return;
      }
      if (getSubActivityVersions().remove(v)) {
         v.setSuperActivityVersion(null);
      }
   }
   
   public boolean isDiskreteActivity() {
      return getType() == OpActivity.ADHOC_TASK
      || getType() == OpActivity.MILESTONE || isZero();
   }
   
   public boolean isEnabledForActions() {
      return getType() == OpActivity.STANDARD ||
                  getType() == OpActivity.MILESTONE || 
                  getType() == OpActivity.TASK;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("OpActivityVersion:{");
      // b.append(super.toString());
      b.append(" N:");
      b.append(getName());
      b.append(" B:");
      b.append(getBaseEffort());
      b.append(" A:");
      b.append(getActualEffort());
      b.append(" R:");
      b.append(getRemainingEffort());
      b.append(" U:");
      b.append(getUnassignedEffort());
      b.append(" C:");
      b.append(getComplete());
      b.append(" S:");
      b.append(getStart());
      b.append(" F:");
      b.append(getFinish());
      b.append("}");
      return b.toString();
   }


   private static class ActualValues {
      private double actualEffort = 0d;
      private double remainingEffort = 0d;
      
      Map<Byte, Double> actualCosts = null;
      Map<Byte, Double> remainingCosts = null;;
      
      public ActualValues() {
         this(0d, 0d);
      }

      public ActualValues(double actualEffort, double remainingEffort) {
         this.actualEffort = actualEffort;
         this.remainingEffort = remainingEffort;
         actualCosts = null;
         remainingCosts = null;
      }
      
      public double getActualEffort() {
         return actualEffort;
      }
      public void setActualEffort(double actualEffort) {
         this.actualEffort = actualEffort;
      }
      public double getRemainingEffort() {
         return remainingEffort;
      }
      public void setRemainingEffort(double remainingEffort) {
         this.remainingEffort = remainingEffort;
      }
      
      public void setActualCosts(byte type, double value) {
         if (actualCosts == null) {
            actualCosts = new HashMap<Byte, Double>();
         }
         actualCosts.put(new Byte(type), new Double(value));
      }
      
      public double getActualCosts(byte type) {
         if (actualCosts == null) {
            return 0d;
         }
         Double v = actualCosts.get(new Byte(type));
         return v != null ? v.doubleValue() : 0d;
      }

      public void setRemainingCosts(byte type, double value) {
         if (remainingCosts == null) {
            remainingCosts = new HashMap<Byte, Double>();
         }
         remainingCosts.put(new Byte(type), new Double(value));
      }
      
      public double getRemainingCosts(byte type) {
         if (remainingCosts == null) {
            return 0d;
         }
         Double v = remainingCosts.get(new Byte(type));
         return v != null ? v.doubleValue() : 0d;
      }
   }

   public OpProjectPlan getProjectPlan() {
      return getPlanVersion() == null ? null : getPlanVersion().getProjectPlan();
   }

   public Set<OpActionVersion> getActions() {
      return actions;
   }

   private void setActions(Set<OpActionVersion> actions) {
      this.actions = actions;
   }
   
   /**
    * @param opAction
    * @pre
    * @post
    */
   public void addAction(OpActionVersion action) {
      if (getActions() == null) {
         setActions(new HashSet<OpActionVersion>());
      }
      getActions().add(action);
      action.setActivity(this);
   }

   public void removeAction(OpActionVersion action) {
      if (getActions() != null) {
         getActions().remove(action);
      }
      action.setActivity(null);
   }

   public Set<OpWorkBreak> getWorkBreaks() {
      return workBreaks;
   }

   private void setWorkBreaks(Set<OpWorkBreak> workBreaks) {
      this.workBreaks = workBreaks;
   }
   
   public void addWorkBreak(OpWorkBreak workBreak) {
      if (getWorkBreaks() == null) {
         setWorkBreaks(new HashSet<OpWorkBreak>());
      }
      Set<OpWorkBreak> wbs = getWorkBreaks();
      if (wbs.add(workBreak)) {
         workBreak.setActivity(this);
         setWorkBreaks(wbs);
      }
   }

   public void removeWorkBreak(OpWorkBreak workBreak) {
      if (getWorkBreaks() == null) {
         return;
      }
      if (getWorkBreaks().remove(workBreak)) {
         workBreak.setActivity(null);
      }
   }

   public OpProjectNode getSubProject() {
      return subProject;
   }

   public void setSubProject(OpProjectNode subProject) {
      this.subProject = subProject;
      updateImported();
   }
   
   public boolean isExported() {
      return (getAttributes() & OpActivity.EXPORTED_TO_SUPERPROJECT) == OpActivity.EXPORTED_TO_SUPERPROJECT;
   }
   
   public void setExported() {
      setAttributes(getAttributes() | OpActivity.EXPORTED_TO_SUPERPROJECT);
   }

   public boolean isImported() {
      return (getAttributes() & OpActivity.IMPORTED_FROM_SUBPROJECT) == OpActivity.IMPORTED_FROM_SUBPROJECT;
      // return isImportedActivity() || isImportedSubProject();
   }
   
   public void updateImported() {
      if (getMasterActivityVersion() != null || getSubProject() != null) {
         setAttributes(getAttributes() | OpActivity.IMPORTED_FROM_SUBPROJECT);
      }
      else {
         setAttributes(getAttributes() - (getAttributes() & OpActivity.IMPORTED_FROM_SUBPROJECT));
      }
   }

   public OpActivityIfc getSuperActivityIfc() {
      return getSuperActivityVersion();
   }

   public Set<OpAssignment> getCheckedInAssignments() {
      return getActivity() != null ? getActivity().getAssignments() : null;
   }
   
   public double getUnassignedEffort() {
      return unassignedEffort;
   }

   public void setUnassignedEffort(double unassignedEffort) {
      this.unassignedEffort = unassignedEffort;
   }

   // FIXME: MSSQL-bug work-around...
   public void setUnassignedEffort(Double uae) {
      this.unassignedEffort = uae != null ? uae.doubleValue() : 0d;
   }

   public void cloneSimpleMembers(OpActivityIfc src, boolean progressTracked) {
      OpActivity.cloneSimpleMembers(src, this, progressTracked);
      if (src instanceof OpActivityVersion) {
         OpActivityVersion srcV = (OpActivityVersion) src;
         OpActivityVersion masterActivity = srcV.getMasterActivityVersion();
         if (masterActivity != null) {
            masterActivity.addShallowCopy(this);
         }
         OpProjectNode subProject = srcV.getSubProject();
         if (subProject != null) {
            subProject.addProgramActivityVersion(this);
         }
      }
      // responsible resource is easy:
      if (src.getResponsibleResource() != null) {
         src.getResponsibleResource().addActivityVersion(this);
      }
   }

   private void applyDelta(OpProgressDelta delta) {
      setUnassignedEffort(getUnassignedEffort() + delta.getUnassignedEffort());
      setBasePersonnelCosts(getBasePersonnelCosts() + delta.getBasePersonnelCosts());
      setBaseProceeds(getBaseProceeds() + delta.getBaseProceeds());
   }
   
   public void handleAssigmentProgress(OpProgressDelta delta) {
      applyDelta(delta);
      OpActivity.propagateProgressToParent(this, delta);
   }

   public void addChildComplete(double childComplete, double childBaseEffort) {
      OpActivity.addWeightedComplete(this, childComplete, childBaseEffort);
   }
   
   
   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActivityIfc#hasAttribute(int)
    */
   public boolean hasAttribute(int attribute) {
      return (getAttributes() & attribute) == attribute;
   }

   public void addAttachmentVersion(OpAttachmentVersion attachment) {
      if (getAttachmentVersions() == null) {
         setAttachmentVersions(new HashSet<OpAttachmentVersion>());
      }
      if (getAttachmentVersions().add(attachment)) {
         attachment.setActivityVersion(this);
      }
   }

   public void removeAttachmentVersion(OpAttachmentVersion attachment) {
      if (getAttachmentVersions() == null) {
         return;
      }
      if (getAttachmentVersions().remove(attachment)) {
         attachment.setActivityVersion(null);
      }
   }

   public void addSuccessorDependency(OpDependencyVersion dependency) {
      if (getSuccessorDependencies() == null) {
         setSuccessorVersions(new HashSet<OpDependencyVersion>());
      }
      if (getSuccessorDependencies().add(dependency)) {
         dependency.setPredecessorVersion(this);
      }
   }

   public void removeSuccessorDependency(OpDependencyVersion dependency) {
      if (getSuccessorDependencies() == null) {
         return;
      }
      if (getSuccessorDependencies().remove(dependency)) {
         dependency.setPredecessorVersion(null);
      }
   }

   public void addPredecessorDependency(OpDependencyVersion dependency) {
      if (getPredecessorDependencies() == null) {
         setPredecessorVersions(new HashSet<OpDependencyVersion>());
      }
      if (getPredecessorDependencies().add(dependency)) {
         dependency.setSuccessorVersion(this);
      }
   }

   public void removePredecessorDependency(OpDependencyVersion dependency) {
      if (getPredecessorDependencies() == null) {
         return;
      }
      if (getPredecessorDependencies().remove(dependency)) {
         dependency.setSuccessorVersion(null);
      }
   }

   public void addWorkPeriodVersion(OpWorkPeriodVersion workPeriod) {
      if (getWorkPeriodVersions() == null) {
         setWorkPeriodVersions(new HashSet<OpWorkPeriodVersion>());
      }
      if (getWorkPeriodVersions().add(workPeriod)) {
         workPeriod.setActivityVersion(this);
      }
   }

   public void removeWorkPeriodVersion(OpWorkPeriodVersion workPeriod) {
      if (getWorkPeriodVersions() == null) {
         return;
      }
      if (getWorkPeriodVersions().remove(workPeriod)) {
         workPeriod.setActivityVersion(null);
      }
   }

   public boolean isLeafActivity() {
      return OpActivity.isLeafActivity(this);
   }

   public Set getTrackedSubElements() {
      return getSubActivityVersions();
   }

   public boolean isTrackingLeaf() {
      return isLeafActivity();
   }

   public boolean isPublicActivity() {
      return publicActivity == null ? false : publicActivity.booleanValue();
   }

   public void setPublicActivity(boolean publicActivity) {
      this.publicActivity = Boolean.valueOf(publicActivity);
   }

   // FIXME: MS_SQL-SERVER and default values...
   public void setPublicActivity(Boolean publicActivity) {
      this.publicActivity = publicActivity;
   }

   public OpActivityVersion getMasterActivityVersion() {
      return masterActivityVersion;
   }

   public void setMasterActivityVersion(OpActivityVersion masterActivityVersion) {
      this.masterActivityVersion = masterActivityVersion;
      updateImported();
   }

   public Set<OpActivityVersion> getShallowCopies() {
      return shallowCopies;
   }

   private void setShallowCopies(Set<OpActivityVersion> shallowCopies) {
      this.shallowCopies = shallowCopies;
   }

   public void addShallowCopy(OpActivityVersion av) {
      if (getShallowCopies() == null) {
         setShallowCopies(new HashSet<OpActivityVersion>());
      }
      if (getShallowCopies().add(av)) {
         av.setMasterActivityVersion(this);
      }
   }

   public void removeShallowCopy(OpActivityVersion av) {
      if (getShallowCopies() == null) {
         return;
      }
      if (getShallowCopies().remove(av)) {
         av.setMasterActivityVersion(null);
      }
   }
   
   public boolean hasBaseEffort() {
      return OpActivity.activityTypeHasBaseEffort(getType());
   }
   
   /**
    * @deprecated
    */
   public OpActivity getMasterActivity() {
      return masterActivity;
   }

   /**
    * @deprecated
    */
   public void setMasterActivity(OpActivity masterActivity) {
      this.masterActivity = masterActivity;
   }
   
   public void setRemainingExternalCosts(double value) {
      initActualValues();
      actualValues.setRemainingCosts(OpCostRecord.EXTERNAL_COST, value);
   }
   public void setRemainingMaterialCosts(double value) {
      initActualValues();
      actualValues.setRemainingCosts(OpCostRecord.MATERIAL_COST, value);
   }
   public void setRemainingMiscellaneousCosts(double value) {
      initActualValues();
      actualValues.setRemainingCosts(OpCostRecord.MISCELLANEOUS_COST, value);
   }
   public void setRemainingPersonnelCosts(double value) {
      initActualValues();
      actualValues.setRemainingCosts(OpCostRecord.PERSONELL_COSTS, value);
   }
   public void setRemainingProceeds(double value) {
      initActualValues();
      actualValues.setRemainingCosts(OpCostRecord.PROCEEDS, value);
   }
   public void setRemainingTravelCosts(double value) {
      initActualValues();
      actualValues.setRemainingCosts(OpCostRecord.TRAVEL_COST, value);
   }

   public void setActualExternalCosts(double value) {
      initActualValues();
      actualValues.setActualCosts(OpCostRecord.EXTERNAL_COST, value);
   }
   public void setActualMaterialCosts(double value) {
      initActualValues();
      actualValues.setActualCosts(OpCostRecord.MATERIAL_COST, value);
   }
   public void setActualMiscellaneousCosts(double value) {
      initActualValues();
      actualValues.setActualCosts(OpCostRecord.MISCELLANEOUS_COST, value);
   }
   public void setActualPersonnelCosts(double value) {
      initActualValues();
      actualValues.setActualCosts(OpCostRecord.PERSONELL_COSTS, value);
   }
   public void setActualProceeds(double value) {
      initActualValues();
      actualValues.setActualCosts(OpCostRecord.PROCEEDS, value);
   }
   public void setActualTravelCosts(double value) {
      initActualValues();
      actualValues.setActualCosts(OpCostRecord.TRAVEL_COST, value);
   }

   public double getRemainingExternalCosts() {
      if (actualValues != null) {
         return actualValues.getRemainingCosts(OpCostRecord.EXTERNAL_COST);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getRemainingExternalCosts();
      }
      return getBaseExternalCosts();
   }
   public double getRemainingMaterialCosts() {
      if (actualValues != null) {
         return actualValues.getRemainingCosts(OpCostRecord.MATERIAL_COST);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getRemainingMaterialCosts();
      }
      return getBaseMaterialCosts();
   }
   public double getRemainingMiscellaneousCosts() {
      if (actualValues != null) {
         return actualValues.getRemainingCosts(OpCostRecord.MISCELLANEOUS_COST);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getRemainingMiscellaneousCosts();
      }
      return getBaseMiscellaneousCosts();
   }
   public double getRemainingPersonnelCosts() {
      if (actualValues != null) {
         return actualValues.getRemainingCosts(OpCostRecord.PERSONELL_COSTS);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getRemainingPersonnelCosts();
      }
      return getBasePersonnelCosts();
   }
   public double getRemainingProceeds() {
      if (actualValues != null) {
         return actualValues.getRemainingCosts(OpCostRecord.PROCEEDS);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getRemainingProceeds();
      }
      return getBaseProceeds();
   }
   public double getRemainingTravelCosts() {
      if (actualValues != null) {
         return actualValues.getRemainingCosts(OpCostRecord.TRAVEL_COST);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getRemainingTravelCosts();
      }
      return getBaseTravelCosts();
   }

   public double getActualExternalCosts() {
      if (actualValues != null) {
         return actualValues.getActualCosts(OpCostRecord.EXTERNAL_COST);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getActualExternalCosts();
      }
      return 0d;
   }
   public double getActualMaterialCosts() {
      if (actualValues != null) {
         return actualValues.getActualCosts(OpCostRecord.MATERIAL_COST);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getActualMaterialCosts();
      }
      return 0d;
   }
   public double getActualMiscellaneousCosts() {
      if (actualValues != null) {
         return actualValues.getActualCosts(OpCostRecord.MISCELLANEOUS_COST);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getActualMiscellaneousCosts();
      }
      return 0d;
   }
   public double getActualPersonnelCosts() {
      if (actualValues != null) {
         return actualValues.getActualCosts(OpCostRecord.PERSONELL_COSTS);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getActualPersonnelCosts();
      }
      return 0d;
   }
   public double getActualProceeds() {
      if (actualValues != null) {
         return actualValues.getActualCosts(OpCostRecord.PROCEEDS);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getActualProceeds();
      }
      return 0d;
   }
   public double getActualTravelCosts() {
      if (actualValues != null) {
         return actualValues.getActualCosts(OpCostRecord.TRAVEL_COST);
      }
      else if (getElementForActualValues() != null) {
         return getElementForActualValues().getActualTravelCosts();
      }
      return 0d;
   }

   public double calculateActualCost() {
      return OpActivity.calculateActualCost(this);
   }

   public double calculateBaseCost() {
      return OpActivity.calculateBaseCost(this);
   }

   public boolean isIndivisible() {
      return OpGanttValidator.isIndivisibleElement(this);
   }

   public boolean isTimeTrackable() {
      return OpActivity.isTimeTrackable(this);
   }
   
   public OpActivityValuesIfc getParent() {
      if (getSuperActivityVersion() != null) {
         return getSuperActivityVersion();
      }
      return getPlanVersion();
   }

   public boolean isSubProjectReference() {
      return getSubProject() != null;
   }

   public boolean hasDerivedStartFinish() {
      return OpActivity.hasDerivedStartFinish(this);
   }

}
