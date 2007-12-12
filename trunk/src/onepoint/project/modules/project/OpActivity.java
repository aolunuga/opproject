/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.persistence.OpSubTypable;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpActivity extends OpObject implements OpSubTypable {

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

   // Activity types
   public final static byte STANDARD = OpGanttValidator.STANDARD;
   public final static byte MILESTONE = OpGanttValidator.MILESTONE;
   public final static byte COLLECTION = OpGanttValidator.COLLECTION;
   public final static byte TASK = OpGanttValidator.TASK;
   public final static byte COLLECTION_TASK = OpGanttValidator.COLLECTION_TASK;
   public final static byte SCHEDULED_TASK = OpGanttValidator.SCHEDULED_TASK;
   public final static byte ADHOC_TASK = OpGanttValidator.ADHOC_TASK;

   //Default priority value
   public final static byte DEFAULT_PRIORITY = OpGanttValidator.DEFAULT_PRIORITY;

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
   private OpActivityCategory category;
   private int sequence;
   private byte outlineLevel;
   private Date start;
   private Date finish;
   private double duration;
   private double complete;
   private byte priority; // Priority 1-9 (0 means N/A)
   private double baseEffort; // Person hours
   private double baseTravelCosts;
   private double basePersonnelCosts;
   private double baseMaterialCosts;
   private double baseExternalCosts;
   private double baseMiscellaneousCosts;
   private double actualEffort; // Person hours
   private double actualTravelCosts;
   private double remainingTravelCosts;
   private double actualPersonnelCosts;
   private Double remainingPersonnelCosts = 0d;
   private double actualMaterialCosts;
   private double remainingMaterialCosts;
   private double actualExternalCosts;
   private double remainingExternalCosts;
   private double actualMiscellaneousCosts;
   private double remainingMiscellaneousCosts;
   private double remainingEffort; // Person hours
   private double baseProceeds;
   private double actualProceeds;
   private Double remainingProceeds = 0d;
   private double payment;
   private boolean deleted;
   private boolean expanded;
   private boolean template;
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
   private boolean usesBaseline;

   public OpActivity() {
   }

   public OpActivity(byte type) {
      this();
      setType(type);
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
      if (usesBaseline) {
         OpActivityVersion baselineVersion = getBaselineVersion();
         if (baselineVersion != null) {
            return baselineVersion.getBaseEffort();
         }
         else {
            return 0;
         }
      }
      return baseEffort;
   }

   public OpActivityVersion getBaselineVersion() {
      OpProjectPlanVersion baselinePlanVersion = getProjectPlan().getBaselineVersion();
      //<FIXME author="Haizea Florin" description="data loading problem: the getVersions().isEmpty() statement will load
      //  all the versions of this activity">
      if (baselinePlanVersion != null && getVersions() != null && !getVersions().isEmpty()) {
         //<FIXME>
         for (OpActivityVersion version : getVersions()) {
            if (version.getPlanVersion().getID() == baselinePlanVersion.getID()) {
               return version;
            }
         }
      }
      return null;
   }

   public boolean isInBaselineVersion() {
      OpProjectPlanVersion baselinePlanVersion = getProjectPlan().getBaselineVersion();
      if (baselinePlanVersion == null) {
         return true;
      }
      for (OpActivityVersion version : baselinePlanVersion.getActivityVersions()) {
         if (version.getActivity().getID() == this.getID()) {
            return true;
         }
      }
      return false;
   }

   public void setBaseTravelCosts(double baseTravelCosts) {
      this.baseTravelCosts = baseTravelCosts;
   }

   public double getBaseTravelCosts() {
      if (usesBaseline) {
         OpActivityVersion baselineVersion = getBaselineVersion();
         if (baselineVersion != null) {
            return baselineVersion.getBaseTravelCosts();
         }
         else {
            return 0;
         }
      }
      return baseTravelCosts;
   }

   public void setBasePersonnelCosts(double basePersonnelCosts) {
      this.basePersonnelCosts = basePersonnelCosts;
   }

   public double getBasePersonnelCosts() {
      if (usesBaseline) {
         OpActivityVersion baselineVersion = getBaselineVersion();
         if (baselineVersion != null) {
            return baselineVersion.getBasePersonnelCosts();
         }
         else {
            return 0;
         }
      }
      return basePersonnelCosts;
   }

   public void setBaseMaterialCosts(double baseMaterialCosts) {
      this.baseMaterialCosts = baseMaterialCosts;
   }

   public double getBaseMaterialCosts() {
      if (usesBaseline) {
         OpActivityVersion baselineVersion = getBaselineVersion();
         if (baselineVersion != null) {
            return baselineVersion.getBaseMaterialCosts();
         }
         else {
            return 0;
         }
      }
      return baseMaterialCosts;
   }

   public void setBaseExternalCosts(double baseExternalCosts) {
      this.baseExternalCosts = baseExternalCosts;
   }

   public double getBaseExternalCosts() {
      if (usesBaseline) {
         OpActivityVersion baselineVersion = getBaselineVersion();
         if (baselineVersion != null) {
            return baselineVersion.getBaseExternalCosts();
         }
         else {
            return 0;
         }
      }
      return baseExternalCosts;
   }

   public void setBaseMiscellaneousCosts(double baseMiscellaneousCosts) {
      this.baseMiscellaneousCosts = baseMiscellaneousCosts;
   }

   public double getBaseMiscellaneousCosts() {
      if (usesBaseline) {
         OpActivityVersion baselineVersion = getBaselineVersion();
         if (baselineVersion != null) {
            return baselineVersion.getBaseMiscellaneousCosts();
         }
         else {
            return 0;
         }
      }
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

   public void setPredecessorDependencies(Set<OpDependency> predecessorDependencies) {
      this.predecessorDependencies = predecessorDependencies;
   }

   public Set<OpDependency> getPredecessorDependencies() {
      return predecessorDependencies;
   }

   public void setAttachments(Set<OpAttachment> attachments) {
      this.attachments = attachments;
   }

   public Set<OpAttachment> getAttachments() {
      return attachments;
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
      if (usesBaseline) {
         OpActivityVersion baselineVersion = getBaselineVersion();
         if (baselineVersion != null) {
            return baselineVersion.getBaseProceeds();
         }
         else {
            return 0;
         }
      }
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
      return payment;
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
    * Calculates the actual total costs of this activity.
    *
    * @return Total actual cost (Personnel + Travel + Material + External + Misc)
    */
   public double calculateActualCost() {
      double actual = this.getActualPersonnelCosts();
      actual += this.getActualTravelCosts();
      actual += this.getActualMaterialCosts();
      actual += this.getActualExternalCosts();
      actual += this.getActualMiscellaneousCosts();
      return actual;
   }

   /**
    * Calculates the base total costs of this activity.
    *
    * @return Total base cost (Personnel + Travel + Material + External + Misc)
    */
   public double calculateBaseCost() {
      double base = this.getBasePersonnelCosts();
      base += this.getBaseTravelCosts();
      base += this.getBaseMaterialCosts();
      base += this.getBaseExternalCosts();
      base += this.getBaseMiscellaneousCosts();
      return base;
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

   public void setIsUsingBaselineValues(boolean useBaseline) {
      this.usesBaseline = useBaseline;
   }

   public boolean isUsingBaselineValues() {
      return usesBaseline;
   }


   public Double getRemainingProceeds() {
      return remainingProceeds;
   }

   public void setRemainingProceeds(Double remainingProceeds) {
      this.remainingProceeds = remainingProceeds != null ? remainingProceeds : 0;
   }

   public Double getRemainingPersonnelCosts() {
      return remainingPersonnelCosts;
   }

   public void setRemainingPersonnelCosts(Double remainingPersonnelCosts) {
      this.remainingPersonnelCosts = remainingPersonnelCosts != null ? remainingPersonnelCosts : 0;
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

}