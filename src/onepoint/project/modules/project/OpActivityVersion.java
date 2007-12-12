/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OpActivityVersion extends OpObject {

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

   // Activity types
   public final static byte STANDARD = OpGanttValidator.STANDARD;
   public final static byte MILESTONE = OpGanttValidator.MILESTONE;
   public final static byte COLLECTION = OpGanttValidator.COLLECTION;
   public final static byte TASK = OpGanttValidator.TASK;
   public final static byte COLLECTION_TASK = OpGanttValidator.COLLECTION_TASK;
   public final static byte SCHEDULED_TASK = OpGanttValidator.SCHEDULED_TASK;

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
   private double complete;
   private byte priority; // Priority 1-9 (zero means undefined)
   private double baseEffort; // Person hours
   private double baseTravelCosts;
   private double basePersonnelCosts;
   private double baseMaterialCosts;
   private double baseExternalCosts;
   private double baseMiscellaneousCosts;
   private double baseProceeds;
   private double payment;
   private boolean expanded;
   private boolean template;
   private OpActivityCategory category;
   private OpActivity activity;
   private OpActivityVersion superActivityVersion;
   private Set subActivityVersions; // *** Could also be a List (via sequence)
   private OpProjectPlanVersion planVersion;
   private Set<OpAssignmentVersion> assignmentVersions;
   private Set workPeriodVersions;
   private Set successorVersions;
   private Set predecessorVersions;
   private Set<OpAttachmentVersion> attachmentVersions;
   private OpResource responsibleResource;

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

   public void setCategory(OpActivityCategory category) {
      this.category = category;
   }

   public OpActivityCategory getCategory() {
      return category;
   }

   public void setActivity(OpActivity activity) {
      this.activity = activity;
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

   public Set getSubActivityVersions() {
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

   public Set<OpAssignmentVersion> getAssignmentVersions() {
      return assignmentVersions;
   }

   public void setWorkPeriodVersions(Set workPeriodVersions) {
      this.workPeriodVersions = workPeriodVersions;
   }

   public Set getWorkPeriodVersions() {
      return workPeriodVersions;
   }

   public void setSuccessorVersions(Set successorVersions) {
      this.successorVersions = successorVersions;
   }

   public Set getSuccessorVersions() {
      return successorVersions;
   }

   public void setPredecessorVersions(Set predecessorVersions) {
      this.predecessorVersions = predecessorVersions;
   }

   public Set getPredecessorVersions() {
      return predecessorVersions;
   }

   public void setAttachmentVersions(Set<OpAttachmentVersion> attachmentVersions) {
      this.attachmentVersions = attachmentVersions;
   }

   public Set<OpAttachmentVersion> getAttachmentVersions() {
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

   public void setBaseProceeds(Double baseProceeds) {
      setBaseProceedsInternal(baseProceeds);
   }

   /**
    * called internally by hibernate
    * @param actualProceeds
    * @see OpPropertyAccessor
    */
   private void setBaseProceedsInternal(Double baseProceeds) {
      this.baseProceeds = (baseProceeds != null) ? baseProceeds : 0;
   }

   public double getPayment() {
      return payment;
   }

   public void setPayment(Double payment) {
      setPaymentInternal(payment);
   }

   /**
    * called internally by hibernate
    * @see OpPropertyAccessor
    */
   private void setPaymentInternal(Double payment) {
      this.payment = (payment != null) ? payment : 0;
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
}
