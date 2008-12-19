/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.util.List;
import java.util.Set;

import onepoint.persistence.OpLocatable;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;

public interface OpActivityIfc extends OpLocatable, OpGanttValidator.ProgressTrackableEntityIfc {

   // Activity attributes
   public final static int MANDATORY = OpGanttValidator.MANDATORY;
   public final static int LINKED = OpGanttValidator.LINKED;
   public final static int HAS_ATTACHMENTS = OpGanttValidator.HAS_ATTACHMENTS;
   public final static int HAS_COMMENTS = OpGanttValidator.HAS_COMMENTS;
   public final static int START_IS_FIXED = OpGanttValidator.START_IS_FIXED;
   public final static int FINISH_IS_FIXED = OpGanttValidator.FINISH_IS_FIXED;
   public final static int EXPORTED_TO_SUPERPROJECT = OpGanttValidator.EXPORTED_TO_SUPERPROJECT;
   public final static int IMPORTED_FROM_SUBPROJECT = OpGanttValidator.IMPORTED_FROM_SUBPROJECT;
   
   // Activity types
   public final static byte STANDARD = OpGanttValidator.STANDARD;
   public final static byte MILESTONE = OpGanttValidator.MILESTONE;
   public final static byte COLLECTION = OpGanttValidator.COLLECTION;
   public final static byte TASK = OpGanttValidator.TASK;
   public final static byte COLLECTION_TASK = OpGanttValidator.COLLECTION_TASK;
   public final static byte SCHEDULED_TASK = OpGanttValidator.SCHEDULED_TASK;
   public final static byte ADHOC_TASK = OpGanttValidator.ADHOC_TASK;

   public String locator();

   public void setName(String name);

   public String getName();

   public void setDescription(String description);

   public String getDescription();

   public void setType(byte type);

   public Byte getType();

   public void setAttributes(int attributes);

   /**
    * Gets the attributes of this activity.
    * 
    * @return each bit in the returned number represents the pressence of an
    *         attribute.
    */
   public int getAttributes();

   public boolean hasAttribute(int attribute);
   
   public void setCategory(OpActivityCategory category);

   public OpActivityCategory getCategory();

   public void setSequence(int sequence);

   public int getSequence();

   public void setOutlineLevel(byte outlineLevel);

   public byte getOutlineLevel();

   public void setStart(Date start);

   public Date getStart();

   public void setFinish(Date finish);

   public Date getFinish();

   public void setDuration(double duration);

   public double getDuration();

   public void setComplete(double complete);

   public double getComplete();

   public void setPriority(byte priority);

   public byte getPriority();

   public void setBaseEffort(double baseEffort);

   public double getBaseEffort();

   public void setBaseTravelCosts(double baseTravelCosts);

   public double getBaseTravelCosts();

   public void setBasePersonnelCosts(double basePersonnelCosts);

   public double getBasePersonnelCosts();

   public void setBaseMaterialCosts(double baseMaterialCosts);

   public double getBaseMaterialCosts();

   public void setBaseExternalCosts(double baseExternalCosts);

   public double getBaseExternalCosts();

   public void setBaseMiscellaneousCosts(double baseMiscellaneousCosts);

   public double getBaseMiscellaneousCosts();

   public void setExpanded(boolean expanded);

   public boolean getExpanded();

   public void setTemplate(boolean template);

   public boolean getTemplate();

   // FIXME(dfreis Feb 21, 2008 7:55:53 AM) - should also use a base ifc for
   // OpAssignments
   // public void setProjectPlan(OpProjectPlan projectPlan);
   //
   // public OpProjectPlan getProjectPlan();

   // FIXME(dfreis Feb 21, 2008 7:55:53 AM) - should also use a base ifc for
   // OpAssignments
   // public void setAssignments(Set<OpAssignment> assignments);
   //
   // public Set<OpAssignment> getAssignments();

   // FIXME(dfreis Feb 21, 2008 7:55:53 AM) - should also use a base ifc for
   // OpAssignments
   // public void setWorkPeriods(Set<OpWorkPeriod> workPeriods);
   //
   // public Set<OpWorkPeriod> getWorkPeriods();

   // FIXME(dfreis Feb 21, 2008 7:55:53 AM) - should also use a base ifc for
   // OpAssignments
   // public void setAttachments(Set<OpAttachment> attachments);
   //
   // public Set<OpAttachment> getAttachments();

   public OpResource getResponsibleResource();

   public void setResponsibleResource(OpResource responsibleResource);

   public double getBaseProceeds();

   public void setBaseProceeds(Double baseProceeds);

   public double getPayment();

   public void setPayment(Double payment);

   /**
    * Returns a <code>List</code> containing two dates: a start date and an
    * end date. if the activity is a STANDARD one then the list will contain
    * it's start and end dates, if the activity is a TASK then the list will
    * contain it's start date. If the end date will be chosen from the
    * activity's end date, the project's end date and the project'a plan end
    * date. The first one (in this order) that is found not null will be
    * returned.
    * 
    * @return - a <code>List</code> containing two dates: a start date and an
    *         end date. if the activity is a STANDARD one then the list will
    *         contain it's start and end dates, if the activity is a TASK then
    *         the list will contain it's start date. If the end date will be
    *         chosen from the activity's end date, the project's end date and
    *         the project'a plan end date. The first one (in this order) that is
    *         found not null will be returned.
    */
   public List<Date> getStartEndDateByType();

   public double getEffortBillable();

   public void setEffortBillable(Double billable);

   public OpProjectPlan getProjectPlan();

   public Set<? extends OpActionIfc> getActions();

   public double getLeadTime();

   public void setLeadTime(double leadTime);

   public double getFollowUpTime();

   public void setFollowUpTime(double followUpTime);
    
    public Set<? extends OpAssignmentIfc> getAssignments();

   public Object getObject(String key);

   public void setObject(String key, Object obj);

   public Set<OpWorkBreak> getWorkBreaks();

   public void addWorkBreak(OpWorkBreak workBreak);

   public void removeWorkBreak(OpWorkBreak workBreak);

   public OpActivityIfc getSuperActivityIfc();

   public Set<? extends OpAttachmentIfc> getAttachments();
   
   public Set<OpAssignment> getCheckedInAssignments();

   public OpActivity getActivityForActualValues();

   public void cloneSimpleMembers(OpActivityIfc src, boolean progressTracked);
   
   public double getUnassignedEffort();

   public void setUnassignedEffort(double unassignedEffort);
   
   public void addUnassignedEffort(double effort);
   
   public boolean effortCalculatedFromChildren();
   
   public void resetAggregatedValuesForCollection();

   public Set<? extends OpDependencyIfc> getPredecessorDependencies();

   public Set<? extends OpDependencyIfc> getSuccessorDependencies();

   /**
    * @return true, if the activity is a collection activity.
    */
   public boolean hasSubActivities();

   public Set<? extends OpWorkPeriodIfc> getWorkPeriods();

   public double getOpenEffort();

   public double getActualEffort();

   public OpActivity getActivity();

   public double getActualProceeds();

   public double getActualMiscellaneousCosts();

   public double getActualExternalCosts();

   public double getActualTravelCosts();

   public double getActualMaterialCosts();

   public double getActualPersonnelCosts();

   public double getRemainingPersonnelCosts();

   public double getRemainingMaterialCosts();

   public double getRemainingTravelCosts();

   public double getRemainingExternalCosts();

   public double getRemainingMiscellaneousCosts();

   public double getRemainingProceeds();

   public void setRemainingMaterialCosts(Double costs);

   public void setRemainingTravelCosts(Double costs);

   public void setRemainingExternalCosts(Double costs);

   public void setRemainingMiscellaneousCosts(Double costs);

   public boolean isEnabledForActions();

   public double calculateBaseCost();

   public double calculateActualCost();

   public boolean isTimeTrackable();
}
