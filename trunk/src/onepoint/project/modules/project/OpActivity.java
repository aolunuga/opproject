/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import onepoint.project.modules.custom_attribute.OpCustomAttribute;
import onepoint.project.modules.custom_attribute.OpCustomAttributeManager;
import onepoint.project.modules.custom_attribute.OpCustomizableObject;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_controlling.OpControllingRecord;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpCostRecord;
import onepoint.project.modules.work.OpWorkRecord;

public class OpActivity extends OpCustomizableObject implements OpActivityIfc { //implements OpSubTypable {

   public final static String ACTIVITY = "OpActivity";

   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String TYPE = "Type";
   public final static String ATTRIBUTES = "Attributes";
   public final static String CATEGORY = "Category";
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
   public final static String ACTUAL_EFFORT = "ActualEffort";
   public final static String ACTUAL_PERSONNEL_COSTS = "ActualPersonnelCosts";
   public final static String ACTUAL_TRAVEL_COSTS = "ActualTravelCosts";
   public final static String ACTUAL_MATERIAL_COSTS = "ActualMaterialCosts";
   public final static String ACTUAL_EXTERNAL_COSTS = "ActualExternalCosts";
   public final static String ACTUAL_MISCELLANEOUS_COSTS = "ActualMiscellaneousCosts";
   public final static String REMAINING_EFFORT = "RemainingEffort";
   public final static String DELETED = "Deleted";
   public final static String EXPANDED = "Expanded";
   public final static String TEMPALTE = "Template";
   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String SUPER_ACTIVITY = "SuperActivity";
   public final static String SUB_ACTIVITIES = "SubActivities";
   public final static String ASSIGNMENTS = "Assignments";
   public final static String WORK_PERIODS = "WorkPeriods";
   public final static String SUCCESSOR_DEPENDENCIES = "SuccessorDependencies";
   public final static String PREDECESSOR_DEPENDENCIES = "PredecessorDependencies";
   public final static String ATTACHMENTS = "Attachments";
   public final static String VERSIONS = "Versions";
   public final static String COMMENTS = "Comments";
   public final static String RESPONSIBLE_RESOURCE = "ResponsibleResource";
   public final static String EFFORT_BILLABLE = "EffortBillable";

   //Default priority value
   public final static byte DEFAULT_PRIORITY = OpGanttValidator.DEFAULT_PRIORITY;   
   public final static int START_IS_FIXED = OpGanttValidator.START_IS_FIXED;
   public final static int FINISH_IS_FIXED = OpGanttValidator.FINISH_IS_FIXED;
   public final static int EXPORTED_TO_SUPERPROJECT = OpGanttValidator.EXPORTED_TO_SUPERPROJECT;
   public final static int IMPORTED_FROM_SUBPROJECT = OpGanttValidator.IMPORTED_FROM_SUBPROJECT;
   
   public final static int ATTRIBUTES_INHERITED_FROM_ACTIVITY = OpGanttValidator.HAS_COMMENTS;

   //Start & End date indexes
   public final static int START_DATE_LIST_INDEX = 0;
   public final static int END_DATE_LIST_INDEX = 1;

   public static final double DEFAULT_BILLABLE = 100;

   private String name;
   private String description;
   private byte type = STANDARD;
   private int attributes = 0;
   private OpActivityCategory category;
   private int sequence = -1;
   private byte outlineLevel = -1;
   private Date start;
   private Date finish;
   private double duration = 0d;
   private double leadTime = 0d;
   private double followUpTime = 0d;
   private double complete = 0d;
   private byte priority = -1; // Priority 1-9 (0 means N/A)
   private double baseEffort = 0d; // Person hours
   private double unassignedEffort = 0d; // Person hours
   private double baseTravelCosts = 0d;
   private double basePersonnelCosts = 0d;
   private double baseMaterialCosts = 0d;
   private double baseExternalCosts = 0d;
   private double baseMiscellaneousCosts = 0d;
   private double actualEffort = 0d; // Person hours
   private double actualTravelCosts = 0d;
   private double remainingTravelCosts = 0d;
   private double actualPersonnelCosts = 0d;
   private Double remainingPersonnelCosts = 0d;
   private double actualMaterialCosts = 0d;
   private double remainingMaterialCosts = 0d;
   private double actualExternalCosts = 0d;
   private double remainingExternalCosts = 0d;
   private double actualMiscellaneousCosts = 0d;
   private double remainingMiscellaneousCosts = 0d;
   private double remainingEffort = 0d; // Person hours
   private double baseProceeds = 0d;
   private double actualProceeds = 0d;
   private Double remainingProceeds = 0d;
   private double payment = 0d;
   private boolean deleted = false;
   private boolean expanded = false;
   private boolean template = false;
   private Double effortBillable; // 100% default (PERCENT, you're right...)
   private OpProjectPlan projectPlan;
   private OpActivity superActivity;
   private OpResource responsibleResource;
   private Set<OpActivity> subActivities; // *** Could also be a List (via sequence)
   private Set<OpAssignment> assignments;
   private Set<OpWorkPeriod> workPeriods;
   private Set<OpDependency> successorDependencies;
   private Set<OpDependency> predecessorDependencies;
   private Set<OpAttachment> attachments = new HashSet<OpAttachment>();
   private Set<OpActivityVersion> versions;
   private Set<OpActivityComment> comments;
   private Set<OpControllingRecord> controllingRecords;
   private Set<OpWorkBreak> workBreaks;
   private Set<OpAction> actions;
   
   private OpActivity masterActivity;  // Program Management
   private Set<OpActivity> shallowCopies;
   private Set<OpActivityVersion> shallowVersions;
   private OpProjectNode subProject;

   private String wbsCode = null;

   public OpActivity() {
   }

   public OpActivity(byte type) {
      this();
      setType(type);
   }

   public void setupFromActivity(OpActivity a) {
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

      this.actualEffort = a.getActualEffort();
      this.actualExternalCosts = a.getActualExternalCosts();
      this.actualMaterialCosts = a.getActualMaterialCosts();
      this.actualMiscellaneousCosts = a.getActualMiscellaneousCosts();
      this.actualPersonnelCosts = a.getActualPersonnelCosts();
      this.actualProceeds = a.getActualProceeds();
      this.actualTravelCosts = a.getActualTravelCosts();
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
      if ((type < 0) || (type > ADHOC_TASK)) {
         throw (new IllegalArgumentException("type must be within [0," + ADHOC_TASK + "]!"));
      }
      this.type = type;
   }

   public Byte getType() {
      return type;
   }

   public void setAttributes(int attributes) {
      this.attributes = attributes;
   }

   /**
    * Gets the attributes of this activity.
    *
    * @return each bit in the returned number represents the pressence of an attribute.
    */
   public int getAttributes() {
      return attributes;
   }

   public void setCategory(OpActivityCategory category) {
      this.category = category;
   }

   public OpActivityCategory getCategory() {
      return category;
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

   public void setActualEffort(double actualEffort) {
      this.actualEffort = actualEffort;
   }

   public double getActualEffort() {
      return actualEffort;
   }

   public void setActualTravelCosts(double actualTravelCosts) {
      this.actualTravelCosts = actualTravelCosts;
   }

   public double getActualTravelCosts() {
      return actualTravelCosts;
   }

   public void setActualPersonnelCosts(double actualPersonnelCosts) {
      this.actualPersonnelCosts = actualPersonnelCosts;
   }

   public double getActualPersonnelCosts() {
      return actualPersonnelCosts;
   }

   public void setActualMaterialCosts(double actualMaterialCosts) {
      this.actualMaterialCosts = actualMaterialCosts;
   }

   public double getActualMaterialCosts() {
      return actualMaterialCosts;
   }

   public void setActualExternalCosts(double actualExternalCosts) {
      this.actualExternalCosts = actualExternalCosts;
   }

   public double getActualExternalCosts() {
      return actualExternalCosts;
   }

   public void setActualMiscellaneousCosts(double actualMiscellaneousCosts) {
      this.actualMiscellaneousCosts = actualMiscellaneousCosts;
   }

   public double getActualMiscellaneousCosts() {
      return actualMiscellaneousCosts;
   }

   public void setRemainingEffort(double remainingEffort) {
      this.remainingEffort = remainingEffort;
   }

   public double getOpenEffort() {
      return getUnassignedEffort() + getRemainingEffort();
   }
   
   public double getRemainingEffort() {
      return remainingEffort;
   }

   public void setDeleted(boolean deleted) {
      this.deleted = deleted;
   }

   public boolean getDeleted() {
      return deleted;
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

   public void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   public OpProjectPlan getProjectPlan() {
      return projectPlan;
   }

   public void setAssignments(Set<OpAssignment> assignments) {
      this.assignments = assignments;
   }

   public Set<OpAssignment> getAssignments() {
      return assignments;
   }

   public Set<OpAssignment> getCheckedInAssignments() {
      return getAssignments();
   }

   public void addAssignment(OpAssignment a) {
      if (getAssignments() == null) {
         setAssignments(new HashSet<OpAssignment>());
      }
      if (getAssignments().add(a)) {
         a.setActivity(this);
      }
  }
   
   public void removeAssignment(OpAssignment a) {
      if (getAssignments() == null) {
         return;
      }
      if (getAssignments().remove(a)) {
         a.setActivity(null);
      }
  }
   
   public void setWorkPeriods(Set<OpWorkPeriod> workPeriods) {
      this.workPeriods = workPeriods;
   }

   public Set<OpWorkPeriod> getWorkPeriods() {
      return workPeriods;
   }

   public void setSuperActivity(OpActivity superActivity) {
      this.superActivity = superActivity;
   }

   public OpActivity getSuperActivity() {
      return superActivity;
   }

   public void setSubActivities(Set<OpActivity> subActivities) {
      this.subActivities = subActivities;
   }

   public Set<OpActivity> getSubActivities() {
      return subActivities;
   }

   public void setSuccessorDependencies(Set<OpDependency> successorDependencies) {
      this.successorDependencies = successorDependencies;
   }

   public Set<OpDependency> getSuccessorDependencies() {
      return successorDependencies;
   }

   public void addSuccessorDependency(OpDependency tgt) {
      if (getSuccessorDependencies() == null) {
         setSuccessorDependencies(new HashSet<OpDependency>());
      }
      if (getSuccessorDependencies().add(tgt)) {
         tgt.setPredecessorActivity(this);
      }
   }

   public void removeSuccessorDependency(OpDependency tgt) {
      if (getSuccessorDependencies() == null) {
         return;
      }
      if (getSuccessorDependencies().remove(tgt)) {
         tgt.setPredecessorActivity(null);
      }
   }

   public void setPredecessorDependencies(Set<OpDependency> predecessorDependencies) {
      this.predecessorDependencies = predecessorDependencies;
   }

   public Set<? extends OpDependencyIfc> getPredecessorDependencies() {
      return predecessorDependencies;
   }

   public void addPredecessorDependency(OpDependency tgt) {
      if (predecessorDependencies == null) {
         predecessorDependencies = new HashSet<OpDependency>();
      }
      if (predecessorDependencies.add(tgt)) {
         tgt.setSuccessorActivity(this);
      }
   }

   public void removePredecessorDependency(OpDependency tgt) {
      if (getPredecessorDependencies() == null) {
         return;
      }
      if (getPredecessorDependencies().remove(tgt)) {
         tgt.setSuccessorActivity(null);
      }
   }

   public void setAttachments(Set<OpAttachment> attachments) {
      this.attachments = attachments;
   }

   public Set<OpAttachment> getAttachments() {
      return attachments;
   }

   public void addAttachment(OpAttachment add) {
      if (getAttachments() == null) {
         setAttachments(new HashSet<OpAttachment>());
      }
      if (getAttachments().add(add)) {
         add.setActivity(this);
      }
   }

   public void removeAttachment(OpAttachment del) {
      if (getAttachments() == null) {
         return;
      }
      if (getAttachments().remove(del)) {
         del.setActivity(null);
      }
   }


   public void setVersions(Set<OpActivityVersion> versions) {
      this.versions = versions;
   }

   public Set<OpActivityVersion> getVersions() {
      return versions;
   }

   public void setComments(Set<OpActivityComment> comments) {
      this.comments = comments;
   }

   public Set<OpActivityComment> getComments() {
      return comments;
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

   public void setBaseProceeds(Double baseProceeds) {
      setBaseProceedsInternal(baseProceeds);
   }

   /**
    * called internally by hibernate
    * @param baseProceeds
    */
   private void setBaseProceedsInternal(Double baseProceeds) {
      this.baseProceeds = (baseProceeds != null) ? baseProceeds : 0;
   }

   public double getActualProceeds() {
      return actualProceeds;
   }

   public void setActualProceeds(Double actualProceeds) {
      setActualProceedsInternal(actualProceeds);
   }

   /**
    * called internally by hibernate
    * @param actualProceeds
    */
   private void setActualProceedsInternal(Double actualProceeds) {
      this.actualProceeds = (actualProceeds != null) ? actualProceeds : 0;
   }

   public double getPayment() {
      if (getType() == OpActivity.MILESTONE)
         return payment;
      else 
         return 0d;
   }

   public void setPayment(Double payment) {
      setPaymentInternal(payment);
   }

   /**
    * called internally by hibernate
    * @param payment
    */

   private void setPaymentInternal(Double payment) {
      this.payment = (payment != null) ? payment : 0;
   }

   public double getRemainingTravelCosts() {
      return remainingTravelCosts;
   }

   public void setRemainingTravelCosts(Double remainingTravelCosts) {
      setRemainingTravelCostsInternal(remainingTravelCosts);
   }
   /**
    * called internally by hibernate
    * @param remainingTravelCosts
    */

   private void setRemainingTravelCostsInternal(Double remainingTravelCosts) {
      this.remainingTravelCosts = remainingTravelCosts != null ? remainingTravelCosts : 0;
   }

   public double getRemainingMaterialCosts() {
      return remainingMaterialCosts;
   }

   public void setRemainingMaterialCosts(Double remainingMaterialCosts) {
      setRemainingMaterialCostsInternal(remainingMaterialCosts);
   }

   /**
    * called internally by hibernate
    * @param remainingMaterialCosts
    */
   private void setRemainingMaterialCostsInternal(Double remainingMaterialCosts) {
      this.remainingMaterialCosts = remainingMaterialCosts != null ? remainingMaterialCosts : 0;
   }

   public double getRemainingExternalCosts() {
      return remainingExternalCosts;
   }

   public void setRemainingExternalCosts(Double remainingExternalCosts) {
      setRemainingExternalCostsInternal(remainingExternalCosts);
   }

   /**
    * called internally by hibernate
    * @param remainingExternalCosts
    */

   private void setRemainingExternalCostsInternal(Double remainingExternalCosts) {
      this.remainingExternalCosts = remainingExternalCosts != null ? remainingExternalCosts : 0;
   }

   public double getRemainingMiscellaneousCosts() {
      return remainingMiscellaneousCosts;
   }

   public void setRemainingMiscellaneousCosts(Double remainingMiscellaneousCosts) {
      setRemainingMiscellaneousCostsInternal(remainingMiscellaneousCosts);
   }

   /**
    * called internally by hibernate
    * @param remainingMiscellaneousCosts
    */

   private void setRemainingMiscellaneousCostsInternal(Double remainingMiscellaneousCosts) {
      this.remainingMiscellaneousCosts = remainingMiscellaneousCosts != null ? remainingMiscellaneousCosts : 0;
   }

   /**
    * Calculates the base total costs of this activity.
    *
    * @return Total base cost (Personnel + Travel + Material + External + Misc)
    */
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
            if (projectPlan.getProjectNode().getFinish() != null) {
               dates.add(END_DATE_LIST_INDEX, projectPlan.getProjectNode().getFinish());
            }
            else {
               dates.add(END_DATE_LIST_INDEX, projectPlan.getFinish());
            }
         }
      }
      if (type == ADHOC_TASK) {
         dates = new ArrayList<Date>();
         if (start != null) {
            dates.add(START_DATE_LIST_INDEX, start);
         }
         else {
            dates.add(START_DATE_LIST_INDEX, projectPlan.getProjectNode().getStart());
         }
         if (finish != null) {
            dates.add(END_DATE_LIST_INDEX, finish);
         }
         else {
            if (projectPlan.getProjectNode().getFinish() != null) {
               dates.add(END_DATE_LIST_INDEX, projectPlan.getProjectNode().getFinish());
            }
            else {
               dates.add(END_DATE_LIST_INDEX, projectPlan.getFinish());
            }
         }
      }

      return dates;
   }

   public double getRemainingProceeds() {
      return remainingProceeds != null ? remainingProceeds.doubleValue() : 0d;
   }

   public void setRemainingProceeds(double remainingProceeds) {
      this.remainingProceeds = new Double(remainingProceeds);
   }

   public double getRemainingPersonnelCosts() {
      return remainingPersonnelCosts != null ? remainingPersonnelCosts.doubleValue() : 0d;
   }

    public void setRemainingPersonnelCosts(double remainingPersonnelCosts) {
       this.remainingPersonnelCosts = new Double(remainingPersonnelCosts);
   }

   public Set<OpControllingRecord> getControllingRecords() {
      return controllingRecords;
   }

   public void setControllingRecords(Set<OpControllingRecord> controllingRecords) {
      this.controllingRecords = controllingRecords;
   }

   public void addControllingRecord(OpControllingRecord cr) {
      if (cr.getActivity() == this) {
         return;
      }
      else if (cr.getActivity() != null) {
         throw new RuntimeException("Controlling records messed up for activity...");
      }
      if (controllingRecords == null) {
         controllingRecords = new HashSet<OpControllingRecord>();
      }
      controllingRecords.add(cr);
      cr.setActivity(this);
   }
   
   public void removeControllingRecord(OpControllingRecord cr) {
      if (controllingRecords != null && controllingRecords.remove(cr)) {
         cr.setActivity(null);
      }
   }
   /**
    * Updates the remaining personnel costs on the activity and its super activities.
    *
    * @param remainingPersonnelCostsChange - the <code>double</code> value with which the remaining personnel costs was
    *                                      changed.
    */
   public void updateRemainingPersonnelCosts(double remainingPersonnelCostsChange) {
      this.remainingPersonnelCosts = this.remainingPersonnelCosts - remainingPersonnelCostsChange;
      if (superActivity != null) {
         superActivity.updateRemainingPersonnelCosts(remainingPersonnelCostsChange);
      }
   }

   /**
    * Updates the remaining proceeds on the activity and its super activities.
    *
    * @param remainingProceedsChange - the <code>double</code> value with which the remaining proceeds was changed.
    */
   public void updateRemainingProceeds(double remainingProceedsChange) {
      this.remainingProceeds = this.remainingProceeds - remainingProceedsChange;
      if (superActivity != null) {
         superActivity.updateRemainingProceeds(remainingProceedsChange);
      }
   }

   public boolean hasAttachments() {
      return (attributes & HAS_ATTACHMENTS) == HAS_ATTACHMENTS;
   }

   public boolean hasComments() {
      return (attributes & HAS_COMMENTS) == HAS_COMMENTS;
   }

   public double getEffortBillable() {
      return effortBillable == null ? DEFAULT_BILLABLE : effortBillable.doubleValue();
   }

   public void setEffortBillable(Double billable) {
      this.effortBillable = billable;
   }

   /**
    * @author peter
    * evil little helper for evil structured database ;-)
    */
   public static class OpProgressDelta {
      
      private double unassignedEffort = 0d;

      private double actualEffort = 0d;
      private double remainingEffort = 0d;

      private double basePersonnelCosts = 0d;
      private double actualPersonnelCosts = 0d;
      private double baseProceeds = 0d;
      private double actualProceeds = 0d;
      private double remainingPersonnelCosts = 0d;
      private double remainingProceeds = 0d;

      private double weigthedCompleteDelta = 0d;

      private boolean insert = true;
      
      private Map<Byte, Double> actualCosts = new HashMap<Byte, Double>();
      private Map<Byte, Double> remainingCosts = new HashMap<Byte, Double>();
      
      OpProgressDelta(boolean insert, double unassignedEffort,
            double actualEffort, double basePersonnelCosts,
            double actualPersonnelCosts, double baseProceeds,
            double actualProceeds, double remainingEffort,
            double remainingPersonnelCosts, double remainingProceeds,
            double weigthedCompleteDelta) {
         this.insert = insert;
         this.unassignedEffort = unassignedEffort;
         this.actualEffort = actualEffort;
         this.basePersonnelCosts = basePersonnelCosts;
         this.actualPersonnelCosts = actualPersonnelCosts;
         this.actualProceeds = actualProceeds;
         this.baseProceeds = baseProceeds;
         this.remainingEffort = remainingEffort;
         this.remainingPersonnelCosts = remainingPersonnelCosts;
         this.remainingProceeds = remainingProceeds;
         this.weigthedCompleteDelta = weigthedCompleteDelta;
      }

      public boolean isInsert() {
         return insert;
      }

      public double getUnassignedEffort() {
         return unassignedEffort;
      }

      public double getActualEffort() {
         return actualEffort;
      }

      public double getActualPersonnelCosts() {
         return actualPersonnelCosts;
      }

      public double getActualProceeds() {
         return actualProceeds;
      }
      
      public void setActualCosts(Byte costType, double costs) {
         this.actualCosts.put(costType, new Double(costs));
      }

      public double getActualCosts(Byte type) {
         Double r = actualCosts.get(type);
         return (r != null ? r.doubleValue() : 0d);
      }
      
      public double getRemainingEffort() {
         return remainingEffort;
      }

      public double getRemainingProceeds() {
         return remainingProceeds;
      }

      public double getRemainingPersonnelCosts() {
         return remainingPersonnelCosts;
      }

      public void setRemainingCosts(Byte costType, double costs) {
         this.remainingCosts.put(costType, new Double(costs));
      }

      public double getRemainingCosts(Byte type) {
         Double r = remainingCosts.get(type);
         return (r != null ? r.doubleValue() : 0d);
      }
      
      public void setWeigthedCompleteDelta(double weigthedCompleteDelta) {
         this.weigthedCompleteDelta = weigthedCompleteDelta;
      }

      public double getWeigthedCompleteDelta() {
         return weigthedCompleteDelta;
      }

      public double getBasePersonnelCosts() {
         return basePersonnelCosts;
      }

      public double getBaseProceeds() {
         return baseProceeds;
      }

   }
   
   public Map<Byte, List<OpWorkRecord>> getLatestWorkRecords(OpWorkRecord current, int number, Set<Byte> costTypes) {
      // sort this stuff...
      SortedSet<OpWorkRecord> wrSet= new TreeSet<OpWorkRecord>(new Comparator<OpWorkRecord>() {
         public int compare(OpWorkRecord o1, OpWorkRecord o2) {
            // reverse order:
            int c = o2.getWorkSlip().getDate().compareTo(o1.getWorkSlip().getDate());
            c = c != 0 ? c : Long.signum(o2.getAssignment().getId() - o1.getAssignment().getId());
            return c;
         }});

      // FIXME: make this one less ugly ;-)
      for (OpAssignment a: assignments) {
         Map<Byte, List<OpWorkRecord>> tmp = null;
         if (current != null && current.getAssignment().getId() == a.getId()) {
            tmp = a.getLatestWorkRecords(current, number, costTypes);
         }
         else {
            tmp = a.getLatestWorkRecords(null, number, costTypes);
         }
         for (List<OpWorkRecord> l: tmp.values()) {
            wrSet.addAll(l);
         }
      }
      // /FIXME...
      
      Map<Byte, List<OpWorkRecord>> result = new HashMap<Byte, List<OpWorkRecord>>();
      Set<Byte> completed = new HashSet<Byte>();
      Iterator<OpWorkRecord> i = wrSet.iterator();
      while (costTypes != null && costTypes.size() > completed.size() && i.hasNext()) {
         OpWorkRecord wr = i.next();
         for (Byte ct: costTypes) {
            if ((ct.compareTo(OpAssignmentIfc.COST_TYPE_UNDEFINED) == 0 && !wr.isEmpty())
                  || wr.hasCostRecordForType(ct.byteValue())) {
               List<OpWorkRecord> r = result.get(ct);
               if (r == null) {
                  r = new ArrayList<OpWorkRecord>();
                  result.put(ct, r);
               }
               r.add(wr);
               if (r.size() == number) {
                  completed.add(ct);
               }
            }
         }
      }
      return result;
   }
   
   private void applyDelta(OpProgressDelta delta) {
      addUnassignedEffort(delta.getUnassignedEffort());
      setBasePersonnelCosts(getBasePersonnelCosts() + delta.getBasePersonnelCosts());
      setBaseProceeds(getBaseProceeds() + delta.getBaseProceeds());
      
      setActualEffort(getActualEffort() + delta.getActualEffort());
      setActualPersonnelCosts(getActualPersonnelCosts() + delta.getActualPersonnelCosts());
      setActualProceeds(getActualProceeds() + delta.getActualProceeds());
      setActualMaterialCosts(getActualMaterialCosts() + delta.getActualCosts(OpAssignment.COST_TYPE_MATERIAL));
      setActualTravelCosts(getActualTravelCosts() + delta.getActualCosts(OpAssignment.COST_TYPE_TRAVEL));
      setActualExternalCosts(getActualExternalCosts() + delta.getActualCosts(OpAssignment.COST_TYPE_EXTERNAL));
      setActualMiscellaneousCosts(getActualMiscellaneousCosts() + delta.getActualCosts(OpAssignment.COST_TYPE_MISC));

      setRemainingEffort(getRemainingEffort() + delta.getRemainingEffort());
      setRemainingPersonnelCosts(getRemainingPersonnelCosts() + delta.getRemainingPersonnelCosts());
      setRemainingProceeds(getRemainingProceeds() + delta.getRemainingProceeds());
      
      setRemainingExternalCosts(getRemainingExternalCosts() + delta.getRemainingCosts(OpAssignmentIfc.COST_TYPE_EXTERNAL));
      setRemainingMaterialCosts(getRemainingMaterialCosts() + delta.getRemainingCosts(OpAssignmentIfc.COST_TYPE_MATERIAL));
      setRemainingMiscellaneousCosts(getRemainingMiscellaneousCosts() + delta.getRemainingCosts(OpAssignmentIfc.COST_TYPE_MISC));
      setRemainingTravelCosts(getRemainingTravelCosts() + delta.getRemainingCosts(OpAssignmentIfc.COST_TYPE_TRAVEL));
   }
   
   /**
    * Propagate progress information from assignments (only leaf-elements has assigments)
    * @param assignment
    * @param workRecord
    * @param delta
    * @param baseWeighting 
    */
   public void handleAssigmentProgress(OpWorkRecord workRecord,
         OpProgressDelta delta, boolean handleWorkingVersion) {
      
      updateProgressCosts(workRecord, delta);
      applyDelta(delta);

      double weigthedCompleteDelta = 0d;
      // now recalculate the dependend things...
      double oldComplete = getComplete();

      double newComplete = OpGanttValidator.getCompleteFromTracking(this, isProgressTracked());
      setComplete(newComplete);
      
      weigthedCompleteDelta = getComplete() - oldComplete;
      // for the next steps (the two recursions) the order
      // is IMPORTANT: first the OpActivity, than the OpActivityVersions (they build upon each other...) 
      
      delta.setWeigthedCompleteDelta(weigthedCompleteDelta);
      if (getSuperActivity() != null) {
         getSuperActivity().handleSubActivityProgress(delta);
      }
      
      if (handleWorkingVersion) {
         // because thi smight be changed during update of the parent activites, restore it here:
         delta.setWeigthedCompleteDelta(weigthedCompleteDelta);
         updateWorkingVersion(getComplete(), delta);
      }
   }

   private void updateProgressCosts(OpWorkRecord workRecord,
         OpProgressDelta delta) {
      if (workRecord == null) {
         return;
      }
      // if we are the latest WR, find the new WR determining remaining costs stuff (otherwise,
      // remaining will not change). This is because the remaining costs are handle COMPLETELY WEIRD!!!
      OpWorkRecord helper = null;
      double sign = delta.isInsert() ? 1.0 : -1.0;
      Map<Byte, List<OpWorkRecord>> latestWRMap = getLatestWorkRecords(
            workRecord, delta.isInsert() ? 1 : 2,
            workRecord != null ? workRecord.getCostTypes() : null);
      Iterator<Byte> ci = latestWRMap.keySet().iterator();
      while(ci.hasNext()) {
         Byte ct = ci.next();
         List<OpWorkRecord> latestWRsOfActivity = latestWRMap.get(ct);
         boolean isLatest = workRecord == null
               || latestWRsOfActivity.get(0).getId() == workRecord.getId();
         if (isLatest) {
            helper = delta.isInsert() ? workRecord
                  : latestWRsOfActivity.size() > 1 ? latestWRsOfActivity.get(1)
                        : null;
            switch (ct.byteValue()) {
            case OpCostRecord.EXTERNAL_COST:
               delta.setRemainingCosts(OpAssignmentIfc.COST_TYPE_EXTERNAL, sign *
                     (helper == null ? getBaseExternalCosts() : helper
                           .getRemExternalCosts())
                           - getRemainingExternalCosts());
               break;
            case OpCostRecord.MATERIAL_COST:
               delta.setRemainingCosts(OpAssignmentIfc.COST_TYPE_MATERIAL, sign *
                     (helper == null ? getBaseMaterialCosts() : helper
                           .getRemMaterialCosts())
                           - getRemainingMaterialCosts());
               break;
            case OpCostRecord.MISCELLANEOUS_COST:
               delta.setRemainingCosts(OpAssignmentIfc.COST_TYPE_MISC, sign *
                     (helper == null ? getBaseMiscellaneousCosts() : helper
                           .getRemMiscCosts())
                           - getRemainingMiscellaneousCosts());
               break;
            case OpCostRecord.TRAVEL_COST:
               delta.setRemainingCosts(OpAssignmentIfc.COST_TYPE_TRAVEL, sign *
                     (helper == null ? getBaseTravelCosts() : helper
                           .getRemTravelCosts())
                           - getRemainingTravelCosts());
               break;
            }
         }
      }
   }

   /**
    * Used to update those collections (only call recursively, cannot be called from outside because
    * this cannot have any assignments and therefore no work records).
    * @param subActivity
    * @param workRecord
    * @param delta
    */
   private void handleSubActivityProgress(OpProgressDelta delta) {
      // update this:
      applyDelta(delta);

      // now recalculate the dependend things...
      if (!isIndivisible()) {
         setComplete(OpGanttValidator.calculateCompleteValue(getActualEffort(), getBaseEffort(), getOpenEffort()));
      }
      else {
         // The Problem: the switch from none-indivisible to indivisible is determined by actual effort and remainig effort.
         // So this might change anytime...
         // step down one level and collect complete values (very ugly...)
         double oldComplete = getComplete();
         
         double newComplete = OpGanttValidator.getCompleteFromTracking(this, isProgressTracked());
         setComplete(newComplete);
         
         delta.setWeigthedCompleteDelta(getComplete() - oldComplete);
      }

      if (getSuperActivity() != null) {
         getSuperActivity().handleSubActivityProgress(delta);
      }
   }
   
   public double getCompleteFromTracking(boolean progressTracked) {
      return OpGanttValidator.getCompleteFromTracking(this, progressTracked);
   }

   public boolean isIndivisible() {
      return OpGanttValidator.isIndivisibleElemen(this);
   }

   /**
    * @return the working version
    * @deprecated this is temporary code and will be removed - do not use unless you know whwt you do!
    */
   public OpActivityVersion getWorkingVersion() {
//      OpBroker broker = OpBroker.getBroker();
//      OpQuery q = broker.newQuery("from "+OpActivityVersion.class.getName()+" where Activity.id = "+getId()+" and PlanVersion.VersionNumber = "+OpProjectPlan.WORKING_VERSION_NUMBER);
//      List list = broker.list(q);
//      if (list != null && !list.isEmpty()) {
//         return (OpActivityVersion) list.get(0);
//      }
//      return null;
      Set<OpActivityVersion> versions = this.getVersions();
      if (versions == null) {
         return null;
      }
      Iterator<OpActivityVersion> i = versions.iterator();
      while (i.hasNext()) {
         OpActivityVersion av = i.next();
         if (av.getPlanVersion().getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
            return av;
         }
      }
      return null;
   }

   /**
    * 
    * @return
    * @pre
    * @post
    * @deprecated this is temporary code and will be removed - do not use unless you know whwt you do!
    */
   
   public OpActivityVersion getLatestVersion() {
      Set<OpActivityVersion> versions = this.getVersions();
      if (versions == null) {
         return null;
      }
      if (getProjectPlan().getLatestVersion() == null) {
         return null;
      }
      long latestPlanVersion = getProjectPlan().getLatestVersion().getId();
      for (OpActivityVersion av : versions) {
         if (av.getPlanVersion().getId() == latestPlanVersion) {
            return av;
         }
      }
      return null;
      
      
   }
   /**
    * Bridge to the working stuff...
    * @param delta         our allKnowing-Mega-Complete Delta Object
    * @param baseWeighting tell them how we do the calculations.
    */
   public void updateWorkingVersion(double complete, OpProgressDelta delta) {
      if  (getVersions() != null) {
         Iterator<OpActivityVersion> i = getVersions().iterator();
         while (i.hasNext()) {
            OpActivityVersion av = i.next();
            if (av.getPlanVersion().getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
               av.updateComplete(delta);
               break;
            }
         }
      }
   }

   /**
    * @return true, if the activity is a collection activity.
    */
   public boolean hasSubActivities() {
      return getType() == OpActivity.COLLECTION
            || getType() == OpActivity.COLLECTION_TASK
            || getType() == OpActivity.SCHEDULED_TASK;
   }

   public boolean effortCalculatedFromChildren() {
      return getType() == OpActivity.COLLECTION
            || getType() == OpActivity.COLLECTION_TASK
            || getType() == OpActivity.SCHEDULED_TASK;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("OpActivity:{");
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
//      b.append("R: {");
//      for (OpAssignment ass : assignments) {
//    	  b.append(ass.toString());
//      }
//      b.append("}");
      
      b.append("}");
      return b.toString();
   }
   
   public void resetActualValues() {
      setActualEffort(0d);
      setActualExternalCosts(0d);
      setActualMaterialCosts(0d);
      setActualMiscellaneousCosts(0d);
      setActualPersonnelCosts(0d);
      setActualProceeds(0d);
      setActualTravelCosts(0d);
      
      setRemainingEffort(0d);
      setUnassignedEffort(getBaseEffort());

      setRemainingExternalCosts(getBaseExternalCosts());
      setRemainingMaterialCosts(getBaseMaterialCosts());
      setRemainingMiscellaneousCosts(getBaseMiscellaneousCosts());
      setRemainingPersonnelCosts(getBasePersonnelCosts());
      setRemainingProceeds(getBaseProceeds());
      setRemainingTravelCosts(getBaseTravelCosts());
      
      setComplete(0d);
   }
   
   public void resetAggregatedValuesForCollection() {
      setBaseEffort(0d);
      setBaseExternalCosts(0d);
      setBaseMaterialCosts(0d);
      setBaseMiscellaneousCosts(0d);
      setBasePersonnelCosts(0d);
      setBaseProceeds(0d);
      setBaseTravelCosts(0d);
      
      if (!definesStartFinish()) {
         setStart(null);
         setFinish(null);
         setLeadTime(0d);
         setFollowUpTime(0d);
      }
      resetActualValues();
   }
   
   public boolean definesStartFinish() {
      return OpActivity.definesStartFinish(this);
   }
   
   public static boolean definesStartFinish(OpActivityIfc a) {
      return a.getType() == STANDARD || a.getType() == MILESTONE || a.getType() == SCHEDULED_TASK;
   }
   /**
    * WARNING: slow! Uses Hibernate joins to find assignments AND work records!
    * @return
    */
   public boolean hasWorkRecords() {
      if (getAssignments() == null) {
         return false;
      }
      for (OpAssignment a: getAssignments()) {
         if (!a.getWorkRecords().isEmpty()) {
            return true;
         }
      }
      return false;
   }
   
   public boolean isMilestone() {
      return !getDeleted() && getType() == OpActivity.MILESTONE;
   }
   
   public boolean isPlannedActivity() {
      return !getDeleted() && getType() != OpActivity.ADHOC_TASK;
   }
   
   public boolean isProgressTracked() {
      return getProjectPlan().getProgressTracked()
            || getType() == OpActivity.ADHOC_TASK || hasSubActivities();
   }

   public boolean isLeafActivity() {
      return isLeafActivity(this);
   }

   public static boolean isLeafActivity(OpActivityIfc a) {
      return a.getType() == OpActivity.STANDARD
            || a.getType() == OpActivity.MILESTONE
            || a.getType() == OpActivity.ADHOC_TASK
            || a.getType() == OpActivity.TASK; 
   }
   
   public boolean isEnabledForActions() {
      return getType() == OpActivity.STANDARD ||
                  getType() == OpActivity.MILESTONE || 
                  getType() == OpActivity.TASK;
   }
   
   public void addSubActivity(OpActivity a) {
      if (getSubActivities() == null) {
         setSubActivities(new HashSet<OpActivity>());
      }
      if (getSubActivities().add(a)) {
         a.setSuperActivity(this);
      }
   }

   public void removeSubActivity(OpActivity a) {
      if (getSubActivities() == null) {
         return;
      }
      if (getSubActivities().remove(a)) {
         a.setSuperActivity(null);
      }
   }
  
   public Set<OpAction> getActions() {
      return actions;
   }

   private void setActions(Set<OpAction> actions) {
      this.actions = actions;
   }

   /**
    * @param opAction
    * @pre
    * @post
    */
   public void addAction(OpAction action) {
      if (getActions() == null) {
         setActions(new HashSet<OpAction>());
      }
      getActions().add(action);
      action.setActivity(this);
   }
   
   public void removeAction(OpAction action) {
      if (getActions() != null) {
         getActions().remove(action);
      }
      action.setActivity(null);
   }

   public Set<OpWorkBreak> getWorkBreaks() {
      return workBreaks;
   }

   void setWorkBreaks(Set<OpWorkBreak> workBreaks) {
      this.workBreaks = workBreaks;
   }
   
   public void addWorkBreak(OpWorkBreak workBreak) {
      if (getWorkBreaks() == null) {
         setWorkBreaks(new HashSet<OpWorkBreak>());
      }
      if (getWorkBreaks().add(workBreak)) {
         workBreak.setActivity(this);
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

   /**
    * @deprecated
    */
   public Set<OpActivity> getShallowCopies() {
      return shallowCopies;
   }

   /**
    * @deprecated
    */
   private void setShallowCopies(Set<OpActivity> shallowCopies) {
      this.shallowCopies = shallowCopies;
   }
   
   public void addShallowCopy(OpActivity shallowCopy) {
      // TODO: loop detection...
      if (getShallowCopies() == null) {
         setShallowCopies(new HashSet<OpActivity>());
      }
      if (getShallowCopies().add(shallowCopy)) {
         shallowCopy.setMasterActivity(this);
      }
   }
   
   public void removeShallowCopy(OpActivity shallowCopy) {
      if (getShallowCopies() == null) {
         return;
      }
      if (getShallowCopies().remove(shallowCopy)) {
         shallowCopy.setMasterActivity(null);
      }
   }

   /**
    * @deprecated
    */
   public Set<OpActivityVersion> getShallowVersions() {
      return shallowVersions;
   }

   /**
    * @deprecated
    */
   private void setShallowVersions(Set<OpActivityVersion> shallowVersions) {
      this.shallowVersions = shallowVersions;
   }
   
   public void addShallowVersion(OpActivityVersion shallowVersion) {
      // TODO: loop detection...
      if (getShallowVersions() == null) {
         setShallowVersions(new HashSet<OpActivityVersion>());
      }
      if (getShallowVersions().add(shallowVersion)) {
         shallowVersion.setMasterActivity(this);
      }
   }
   
   public void removeShallowVersion(OpActivityVersion shallowVersion) {
      if (getShallowVersions() == null) {
         return;
      }
      if (getShallowVersions().remove(shallowVersion)) {
         shallowVersion.setMasterActivity(null);
      }
   }

   /**
    * @deprecated
    */
   public OpProjectNode getSubProject() {
      return subProject;
   }

   /**
    * @deprecated
    */
   public void setSubProject(OpProjectNode subProject) {
      this.subProject = subProject;
   }

   public OpActivityIfc getSuperActivityIfc() {
      return getSuperActivity();
   }

   public OpActivity getActivityForActualValues() {
      return this;
   }

   public void addActivityVersion(OpActivityVersion av) {
      Set<OpActivityVersion> actVersions = getVersions();
      if (actVersions == null) {
         setVersions(new HashSet<OpActivityVersion>());
         actVersions = getVersions();
      }
      if (actVersions.add(av)) {
         av.setActivity(this);
      }
   }
  
   public void removeActivityVersion(OpActivityVersion av) {
      if (getVersions() == null) {
         return;
      }
      if (getVersions().remove(av)) {
         av.setActivity(null);
      }
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

   public void addUnassignedEffort(double effort) {
      setUnassignedEffort(getUnassignedEffort() + effort);
   }
   
   public void cloneSimpleMembers(OpActivityIfc src, boolean progressTracked) {
      // TODO all new Progress delta!      
      cloneSimpleMembers(src, this, progressTracked);
      
      // copy all custom attributes
      OpCustomAttributeManager caManager = OpCustomAttributeManager.getInstance();
      if (caManager != null) {
         Map<String, OpCustomAttribute> map = caManager.getCustomAttributesMap(OpActivity.class, null);
         for (String key : map.keySet()) {
            this.setObject(key, src.getObject(key));
         }
      }

      // responsible resource is easy:
      if (src.getResponsibleResource() != null) {
         src.getResponsibleResource().addActivity(this);
      }
   }
   
   public static void cloneSimpleMembers(OpActivityIfc src, OpActivityIfc tgt, boolean progressTracked) {

      // all things solely maintained by the activity itself (unassigned effort, remaining costs)
      // must be maintained here in a delta process...
      double baseEffortDelta = src.getBaseEffort() - tgt.getBaseEffort();
      tgt.addUnassignedEffort(baseEffortDelta);

      // TODO: Check assumption: if actual costs exist, the remainings are defined by the costs records. None-progress Tracked stuff???
      double baseExternalCostsDelta = tgt.getActualExternalCosts() == 0 ? src.getBaseExternalCosts() - tgt.getBaseExternalCosts() : 0d;
      double baseMaterialCostsDelta = tgt.getActualMaterialCosts() == 0 ? src.getBaseMaterialCosts() - tgt.getBaseMaterialCosts() : 0d;
      double baseMiscellaneousCostsDelta = tgt.getActualMiscellaneousCosts() == 0 ? src.getBaseMiscellaneousCosts() - tgt.getBaseMiscellaneousCosts() : 0d;
      double baseTravelCostsDelta = tgt.getActualTravelCosts() == 0 ? src.getBaseTravelCosts() - tgt.getBaseTravelCosts() : 0d;
      
      tgt.setBaseEffort(0d);
      tgt.setBaseExternalCosts(0d);
      tgt.setBaseMaterialCosts(0d);
      tgt.setBaseMiscellaneousCosts(0d);
      tgt.setBaseTravelCosts(0d);
      
      tgt.setFinish(null);
      tgt.setLeadTime(0d);
      tgt.setStart(null);
      tgt.setFollowUpTime(0d);

      tgt.setDuration(0d);
      tgt.setEffortBillable(null);
      tgt.setExpanded(false);

      tgt.setAttributes(0);
      tgt.setCategory(null);
      tgt.setName(null);
      tgt.setDescription(null);

      tgt.setOutlineLevel((byte) 0);

      tgt.setPayment(0d);
      tgt.setPriority(OpActivity.DEFAULT_PRIORITY);
      tgt.setSequence(0);
      tgt.setTemplate(false);
      tgt.setType(OpActivity.STANDARD);
      
      // a little ugly maybe, 
      switch (src.getType()) {
      case OpActivity.STANDARD:
      case OpActivity.MILESTONE:
      case OpActivity.TASK:

         tgt.setBaseEffort(src.getBaseEffort());

         tgt.setBaseExternalCosts(src.getBaseExternalCosts());
         tgt.setBaseMaterialCosts(src.getBaseMaterialCosts());
         tgt.setBaseMiscellaneousCosts(src.getBaseMiscellaneousCosts());
         tgt.setBaseTravelCosts(src.getBaseTravelCosts());

         // ATTN: currently, remaining costs are always estimated by project contributor 
         if (true || progressTracked) { 
            tgt.setRemainingExternalCosts(tgt.getRemainingExternalCosts() + baseExternalCostsDelta);
            tgt.setRemainingMaterialCosts(tgt.getRemainingMaterialCosts() + baseMaterialCostsDelta);
            tgt.setRemainingMiscellaneousCosts(tgt.getRemainingMiscellaneousCosts() + baseMiscellaneousCostsDelta);
            tgt.setRemainingTravelCosts(tgt.getRemainingTravelCosts() + baseTravelCostsDelta);
         }
         else {
            tgt.setComplete(src.getComplete());
            tgt.setRemainingExternalCosts(OpGanttValidator.calculateRemainingEffort(tgt.getBaseExternalCosts(), tgt.getActualExternalCosts(), tgt.getComplete()));
            tgt.setRemainingMaterialCosts(OpGanttValidator.calculateRemainingEffort(tgt.getBaseMaterialCosts(), tgt.getActualMaterialCosts(), tgt.getComplete()));
            tgt.setRemainingMiscellaneousCosts(OpGanttValidator.calculateRemainingEffort(tgt.getBaseMiscellaneousCosts(), tgt.getActualMiscellaneousCosts(), tgt.getComplete()));
            tgt.setRemainingTravelCosts(OpGanttValidator.calculateRemainingEffort(tgt.getBaseTravelCosts(), tgt.getActualTravelCosts(), tgt.getComplete()));
         }
      case OpActivity.COLLECTION_TASK:
      case OpActivity.SCHEDULED_TASK:
         tgt.setFinish(src.getFinish());
         tgt.setLeadTime(src.getLeadTime());
         tgt.setStart(src.getStart());
         tgt.setFollowUpTime(src.getFollowUpTime());

      case OpActivity.COLLECTION:
         tgt.setDuration(src.getDuration());
         tgt.setEffortBillable(src.getEffortBillable());
         tgt.setExpanded(src.getExpanded());

         tgt.setAttributes(src.getAttributes());
         tgt.setCategory(src.getCategory());
         tgt.setName(src.getName());
         tgt.setDescription(src.getDescription());

         tgt.setOutlineLevel(src.getOutlineLevel());

         tgt.setPayment(src.getPayment());
         tgt.setPriority(src.getPriority());
         tgt.setSequence(src.getSequence());
         tgt.setTemplate(src.getTemplate());
         tgt.setType(src.getType());
         
         break;
      }
   }

   public void removeWorkPeriod(OpWorkPeriod del) {
      if (getWorkPeriods() == null) {
         return;
      }
      if (getWorkPeriods().remove(del)) {
         del.setActivity(null);
      }
   }

   public void addWorkPeriod(OpWorkPeriod wp) {
      if (getWorkPeriods() == null) {
         setWorkPeriods(new HashSet<OpWorkPeriod>());
      }
      if (getWorkPeriods().add(wp)) {
         wp.setActivity(this);
      }
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpActivityIfc#hasAttribute(int)
    */
   public boolean hasAttribute(int attribute) {
      return (getAttributes() & attribute) == attribute;
   }

   public Set getTrackedSubElements() {
      if (isLeafActivity()) {
         return getAssignments();
      }
      else {
         Set subElements = new HashSet();
         if (getSubActivities() == null) {
            return subElements;
         }
         Iterator i = getSubActivities().iterator();
         while (i.hasNext()) {
            OpActivity a = (OpActivity) i.next();
            if (!a.getDeleted()) {
               subElements.add(a);
            }
         }
         return subElements;
      }
   }

   public boolean isTrackingLeaf() {
      return isLeafActivity();
   }

   public OpActivity getActivity() {
      return this;
   }
   
   public boolean hasBaseEffort() {
      return activityTypeHasBaseEffort(getType());
   }
   
   public static boolean activityTypeHasBaseEffort(byte type) {
      boolean result = false;
      switch (type) {
      case STANDARD:
      case TASK:
         result = true;
         break;
      case COLLECTION:
      case COLLECTION_TASK:
      case MILESTONE:
      case ADHOC_TASK:
         result = false;
         break;
      }
      return result;
   }

   public String getWbsCode() {
      return wbsCode;
   }

   public void setWbsCode(String wbsCode) {
      this.wbsCode = wbsCode;
   }

   public double calculateActualCost() {
      return OpActivity.calculateActualCost(this);
   }

   public double calculateBaseCost() {
      return OpActivity.calculateBaseCost(this);
   }

   public static double calculateBaseCost(OpActivityIfc act) {
      double base = act.getBasePersonnelCosts();
      base += act.getBaseTravelCosts();
      base += act.getBaseMaterialCosts();
      base += act.getBaseExternalCosts();
      base += act.getBaseMiscellaneousCosts();
      return base;
   }

   public static double calculateActualCost(OpActivityIfc act) {
      double actual = act.getActualPersonnelCosts();
      actual += act.getActualTravelCosts();
      actual += act.getActualMaterialCosts();
      actual += act.getActualExternalCosts();
      actual += act.getActualMiscellaneousCosts();
      return actual;
   }

   public boolean isTimeTrackable() {
      return OpActivity.isTimeTrackable(this);
   }
   
   public static boolean isTimeTrackable(OpActivityIfc act) {
      return act.getType() == OpGanttValidator.STANDARD
            || act.getType() == OpGanttValidator.TASK
            || act.getType() == OpGanttValidator.ADHOC_TASK;
   }


}