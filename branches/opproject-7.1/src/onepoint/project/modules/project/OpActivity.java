/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.work.OpWorkRecord;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class OpActivity extends OpObject {

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
   private Set<OpAttachment> attachments;
   private Set<OpActivityVersion> versions;
   private Set<OpActivityComment> comments;
   private boolean usesBaseline;

   private Boolean progressTrackedCache;

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

   public byte getType() {
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

   public void addAssignment(OpAssignment a) {
      if (getAssignments() == null) {
         setAssignments(new HashSet<OpAssignment>());
      }
      getAssignments().add(a);
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
      this.baseProceeds = (baseProceeds != null) ? baseProceeds : 0;
   }

   public double getActualProceeds() {
      return actualProceeds;
   }

   public void setActualProceeds(Double actualProceeds) {
      this.actualProceeds = (actualProceeds != null) ? actualProceeds : 0;
   }

   public double getPayment() {
      return payment;
   }

   public void setPayment(Double payment) {
      this.payment = (payment != null) ? payment : 0;
   }

   public double getRemainingTravelCosts() {
      return remainingTravelCosts;
   }

   public void setRemainingTravelCosts(Double remainingTravelCosts) {
      this.remainingTravelCosts = remainingTravelCosts != null ? remainingTravelCosts : 0;
   }

   public double getRemainingMaterialCosts() {
      return remainingMaterialCosts;
   }

   public void setRemainingMaterialCosts(Double remainingMaterialCosts) {
      this.remainingMaterialCosts = remainingMaterialCosts != null ? remainingMaterialCosts : 0;
   }

   public double getRemainingExternalCosts() {
      return remainingExternalCosts;
   }

   public void setRemainingExternalCosts(Double remainingExternalCosts) {
      this.remainingExternalCosts = remainingExternalCosts != null ? remainingExternalCosts : 0;
   }

   public double getRemainingMiscellaneousCosts() {
      return remainingMiscellaneousCosts;
   }

   public void setRemainingMiscellaneousCosts(Double remainingMiscellaneousCosts) {
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


   /**
    * @author peter
    * evil little helper for evil structured database ;-)
    */
   public static class OpProgressDelta {
      private boolean progressTracked = false;
      
      private double remainingEffort = 0.0;
      private boolean insert = true;
      private boolean completedChanged = false;
      private double weigthedCompleteDelta = 0.0;
      private boolean latest = false;
      
      private double remainingMaterialCosts = 0.0;
      private double remainingTravelCosts = 0.0;
      private double remainingExternalCosts = 0.0;
      private double remainingMiscCosts = 0.0;

      OpProgressDelta(boolean progressTracked, double remainingEffort,
            boolean insert, boolean completedChanged, double weigthedCompleteDelta,
            boolean latest) {
         this.progressTracked = progressTracked;
         this.remainingEffort = remainingEffort;
         this.insert = insert;
         this.completedChanged = completedChanged;
         this.weigthedCompleteDelta = weigthedCompleteDelta;
         this.latest = latest;
      }

      public void setProgressTracked(boolean progressTracked) {
         this.progressTracked = progressTracked;
      }

      public void setWeigthedCompleteDelta(double weigthedCompleteDelta) {
         this.weigthedCompleteDelta = weigthedCompleteDelta;
      }

      public void setRemainingMaterialCosts(double remainingMaterialCosts) {
         this.remainingMaterialCosts = remainingMaterialCosts;
      }

      public void setRemainingTravelCosts(double remainingTravelCosts) {
         this.remainingTravelCosts = remainingTravelCosts;
      }

      public void setRemainingExternalCosts(double remainingExternalCosts) {
         this.remainingExternalCosts = remainingExternalCosts;
      }

      public void setRemainingMiscCosts(double remainingMiscCosts) {
         this.remainingMiscCosts = remainingMiscCosts;
      }

      public double getRemainingEffort() {
         return remainingEffort;
      }

      public boolean isProgressTracked() {
         return progressTracked;
      }

      public boolean isInsert() {
         return insert;
      }

      public boolean isCompletedChanged() {
         return completedChanged;
      }

      public double getWeigthedCompleteDelta() {
         return weigthedCompleteDelta;
      }

      public boolean isLatest() {
         return latest;
      }

      public double getRemainingMaterialCosts() {
         return remainingMaterialCosts;
      }

      public double getRemainingTravelCosts() {
         return remainingTravelCosts;
      }

      public double getRemainingExternalCosts() {
         return remainingExternalCosts;
      }

      public double getRemainingMiscCosts() {
         return remainingMiscCosts;
      }
   }
   
   /**
    * TODO: optimize for performance, maybe even query?
    * @param number       number of records in the past
    * @param acceptEmpty  Do we accept empty records
    * @return             The list of maxmum number records.
    */
   public List<OpWorkRecord> getLatestWorkRecords(OpWorkRecord current, int number, boolean acceptEmpty) {
      // sort this stuff...
      SortedSet<OpWorkRecord> wrSet= new TreeSet<OpWorkRecord>(new Comparator<OpWorkRecord>() {
         public int compare(OpWorkRecord o1, OpWorkRecord o2) {
            // reverse order:
            int c = o2.getWorkSlip().getDate().compareTo(o1.getWorkSlip().getDate());
            c = c != 0 ? c : Long.signum(o2.getID() - o1.getID());
            return o2.getWorkSlip().getDate().compareTo(o1.getWorkSlip().getDate());
         }});

      for (OpAssignment a: assignments) {
         if (current.getAssignment().getID() == a.getID()) {
            wrSet.addAll(a.getLatestWorkRecords(current, number, acceptEmpty));
         }
         else {
            wrSet.addAll(a.getLatestWorkRecords(null, number, acceptEmpty));
         }
      }
      
      List<OpWorkRecord> result = new ArrayList<OpWorkRecord>();
      Iterator<OpWorkRecord> i = wrSet.iterator();
      while (result.size() < number && i.hasNext()) {
         OpWorkRecord wr = i.next();
         if (!wr.isEmpty() || acceptEmpty) {
            result.add(wr);
         }
      }
      return result;
   }
   
   private void updateActualStuff(OpWorkRecord workRecord, double factor) {
      setActualEffort(getActualEffort() + factor * workRecord.getActualEffort());
      setActualPersonnelCosts(getActualPersonnelCosts() + factor * workRecord.getPersonnelCosts());
      setActualProceeds(getActualProceeds() + factor * workRecord.getActualProceeds());
      setActualMaterialCosts(getActualMaterialCosts() + factor * workRecord.getMaterialCosts());
      setActualTravelCosts(getActualTravelCosts() + factor * workRecord.getTravelCosts());
      setActualExternalCosts(getActualExternalCosts() + factor * workRecord.getExternalCosts());
      setActualMiscellaneousCosts(getActualMiscellaneousCosts() + factor * workRecord.getMiscellaneousCosts());
   }
   
   private void applyDelta(OpProgressDelta delta) {
      setRemainingEffort(getRemainingEffort() + delta.getRemainingEffort());
      
      setRemainingExternalCosts(getRemainingExternalCosts() + delta.getRemainingExternalCosts());
      setRemainingMaterialCosts(getRemainingMaterialCosts() + delta.getRemainingMaterialCosts());
      setRemainingMiscellaneousCosts(getRemainingMiscellaneousCosts() + delta.getRemainingMiscCosts());
      setRemainingTravelCosts(getRemainingTravelCosts() + delta.getRemainingTravelCosts());
   }
   
   /**
    * Propagate progress information from assignments (only leaf-elements has assigments)
    * @param assigment
    * @param workRecord
    * @param delta
    * @param baseWeighting 
    */
   public void handleAssigmentProgress(OpAssignment assigment,
         OpWorkRecord workRecord, OpProgressDelta delta) {
      
      double sign = delta.isInsert() ? 1.0 : -1.0;
      
      // update this:
      updateActualStuff(workRecord, sign);
      
      // if we are the latest WR, find the new WR determining remaining costs stuff (otherwise,
      // remaining will not change). This is because the remaining costs are handle COMPLETELY WEIRD!!!
      OpWorkRecord helper = null;
      if (delta.isLatest()) {
         List<OpWorkRecord> latestWRsOfActivity = getLatestWorkRecords(workRecord, delta.isInsert() ? 1 : 2, false);
         boolean latest = latestWRsOfActivity.get(0).getID() == workRecord.getID(); // latest record always exists!
         if (latest) {
            helper = delta.isInsert() ? workRecord
                  : latestWRsOfActivity.size() > 1 ? latestWRsOfActivity.get(1)
                        : null;
         }
         
         if (latest) {
         // calculate the deltas of the remaining stuff (only, if we are latest for the assignment:
            delta.setRemainingExternalCosts(getRemainingExternalCosts() - (helper == null ? getBaseExternalCosts() : sign * helper.getRemExternalCosts()));
            delta.setRemainingMaterialCosts(getRemainingMaterialCosts() - (helper == null ? getBaseMaterialCosts() : sign * helper.getRemMaterialCosts()));
            delta.setRemainingMaterialCosts(getRemainingMiscellaneousCosts() - (helper == null ? getBaseMiscellaneousCosts() : sign * helper.getRemMiscCosts()));
            delta.setRemainingMaterialCosts(getRemainingTravelCosts() - (helper == null ? getBaseTravelCosts() : sign * helper.getRemTravelCosts()));
         }
      }
      
      applyDelta(delta);
      
      // now recalculate the dependend things...
      if (delta.isProgressTracked()) {
         switch (getType()) {
         case OpActivity.ADHOC_TASK:
         case OpActivity.TASK:
         case OpActivity.MILESTONE:
            // multiple assignments possible...
            // NOTE: the same code could be used for TASKS as well,
            // but we devided those cases for the sake of clearity.
            if (delta.isCompletedChanged() && delta.isLatest()) {
               if (workRecord.getCompleted() && !delta.isInsert()) {
                  setComplete(0d);
               }
               else {
                  boolean completed = !getAssignments().isEmpty();
                  for (OpAssignment a : getAssignments()) {
                     if (a.getComplete() != 100d) {
                        completed = false;
                        break;
                     }
                  }
                  setComplete(completed ? 100 : 0);
               }
            }
            break;
         default:
            setComplete(OpGanttValidator.calculateCompleteValue(
                  getActualEffort(), getBaseEffort(), getRemainingEffort()));
            break;
         }
      }
      // for the next steps (the two recursions) the order
      // is IMPORTANT: first the OpActivity, than the OpActivityVersions (they build upon each other...) 
      if (getSuperActivity() != null) {
         getSuperActivity().handleSubActivityProgress(this, workRecord, delta);
      }
      
      updateWorkingVersion(getComplete(), delta);
   }

   /**
    * Used to update those collections (only call recursively, cannot be called from outside because
    * this cannot have any assignments and therefore no work records).
    * @param subActivity
    * @param workRecord
    * @param delta
    */
   private void handleSubActivityProgress(OpActivity subActivity, OpWorkRecord workRecord, OpProgressDelta delta) {
      // update this:
      updateActualStuff(workRecord, delta.isInsert() ? 1.0 : -1.0);
      
      applyDelta(delta);

      // now recalculate the dependend things...
      setComplete(OpGanttValidator.calculateCompleteValue(getActualEffort(), getBaseEffort(), getRemainingEffort()));

      if (getSuperActivity() != null) {
         getSuperActivity().handleSubActivityProgress(this, workRecord, delta);
      }
   }
   
   /**
    * In case of progress tracking, we need to honour different types of activities because
    * calculation of progress is significantly different here...
    * @return
    */
   public double getCompleteFromTracking() {
      if (getProjectPlan().getProgressTracked()) {
         switch (getType()) {
         case OpActivity.ADHOC_TASK:
         case OpActivity.TASK:
         case OpActivity.MILESTONE:
            boolean completed = !getAssignments().isEmpty();
            for (OpAssignment a : getAssignments()) {
               if (a.getCompleteFromTracking() != 100d) { // ugly!!!
                  completed = false;
                  break;
               }
            }
            return (completed ? 100 : 0);
         default:
            return OpGanttValidator.calculateCompleteValue(getActualEffort(),
                  getBaseEffort(), getRemainingEffort());
         }
      }
      else {
         return OpGanttValidator.calculateCompleteValue(getActualEffort(),
               getBaseEffort(), getRemainingEffort());
      }
   }

   /**
    * Bridge to the working stuff...
    * @param delta         our allKnowing-Mega-Complete Delta Object
    * @param baseWeighting tell them how we do the calculations.
    */
   public void updateWorkingVersion(double complete, OpProgressDelta delta) {
      Iterator<OpActivityVersion> i = getVersions().iterator();
      while (i.hasNext()) {
         OpActivityVersion av = i.next();
         if (av.getPlanVersion().getVersionNumber() == OpProjectPlan.WORKING_VERSION_NUMBER) {
            av.updateComplete(delta);
            break;
         }
      }
   }

   /**
    * @return true, if the activity is a collection activity.
    */
   public boolean isCollection() {
      return getType() == OpActivity.COLLECTION || getType() == OpActivity.COLLECTION_TASK;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("OpActivity:{");
      b.append(super.toString());
      b.append(" N:");
      b.append(getName());
      b.append(" B:");
      b.append(getBaseEffort());
      b.append(" A:");
      b.append(getActualEffort());
      b.append(" R:");
      b.append(getRemainingEffort());
      b.append(" C:");
      b.append(getComplete());
      b.append("}");
      return b.toString();
   }
   
   public void resetValues() {
      setBaseEffort(0d);
      setBaseExternalCosts(0d);
      setBaseMaterialCosts(0d);
      setBaseMiscellaneousCosts(0d);
      setBasePersonnelCosts(0d);
      setBasePersonnelCosts(0d);
      setBaseProceeds(0d);
      setBaseTravelCosts(0d);
      
      setActualEffort(0d);
      setActualExternalCosts(0d);
      setActualMaterialCosts(0d);
      setActualMiscellaneousCosts(0d);
      setActualPersonnelCosts(0d);
      setActualProceeds(0d);
      setActualTravelCosts(0d);
      
      setRemainingEffort(0d);
      setRemainingExternalCosts(0d);
      setRemainingMaterialCosts(0d);
      setRemainingMiscellaneousCosts(0d);
      setRemainingPersonnelCosts(0d);
      setRemainingProceeds(0d);
      setRemainingTravelCosts(0d);
    }
   
   
}