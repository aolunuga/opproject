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

public interface OpActivityIfc extends OpLocatable, OpActivityValuesIfc {

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

   public void addWorkBreak(OpWorkBreak workBreak);

   public double calculateActualCost();

   public double calculateBaseCost();

   public void cloneSimpleMembers(OpActivityIfc src, boolean progressTracked);

   public Set<? extends OpActionIfc> getActions();

   public OpActivity getActivity();

   public OpActivity getActivityForAdditionalObjects();
   
   public Set<? extends OpAssignmentIfc> getAssignments();

   public Set<? extends OpAttachmentIfc> getAttachments();

   /**
    * Gets the attributes of this activity.
    * 
    * @return each bit in the returned number represents the pressence of an
    *         attribute.
    */
   public int getAttributes();

   public OpActivityCategory getCategory();

   public Set<OpAssignment> getCheckedInAssignments();

   public double getComplete();

   public String getDescription();

   public double getEffortBillable();

   public OpActivityValuesIfc getElementForActualValues();

   public boolean getExpanded();

   public String getName();

   public Object getObject(String key);

   public byte getOutlineLevel();

   public double getPayment();

   public Set<? extends OpDependencyIfc> getPredecessorDependencies();

   public byte getPriority();

   public OpProjectPlan getProjectPlan();

   public OpResource getResponsibleResource();

   public int getSequence();

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

   public Set<? extends OpDependencyIfc> getSuccessorDependencies();

   public OpActivityIfc getSuperActivityIfc();

   public boolean getTemplate();

   public byte getType();

   public Set<OpWorkBreak> getWorkBreaks();

   public Set<? extends OpWorkPeriodIfc> getWorkPeriods();

   public boolean hasAggregatedValues();

   public boolean hasAttribute(int attribute);

   /**
    * @return true, if the activity is a collection activity.
    */
   public boolean hasSubActivities();

   public boolean isEnabledForActions();
   
   public boolean isImported();

   public boolean isProgressTracked();
   
   public boolean isTimeTrackable();

   public String locator();

   public void removeWorkBreak(OpWorkBreak workBreak);
   
   public void resetAggregatedValues();
   
   public void setAttributes(int attributes);
   
   public void setCategory(OpActivityCategory category);

   public void setDescription(String description);

   public void setEffortBillable(Double billable);

   public void setExpanded(boolean expanded);

   public void setName(String name);

   public void setObject(String key, Object obj);

   public void setOutlineLevel(byte outlineLevel);

   public void setPayment(double payment);

   public void setPriority(byte priority);

   public void setResponsibleResource(OpResource responsibleResource);
   
   public void setSequence(int sequence);
   
   public void setTemplate(boolean template);

   public void setType(byte type);

   public boolean isSubProjectReference();

   public boolean hasDerivedStartFinish();

   public String getPredecessorDependenciesAsString();
}
