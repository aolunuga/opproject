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

public interface OpActivityIfc {

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
    * @return each bit in the returned number represents the pressence of an attribute.
    */
   public int getAttributes();

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

// FIXME(dfreis Feb 21, 2008 7:55:53 AM) - should also use a base ifc for OpAssignments
//   public void setProjectPlan(OpProjectPlan projectPlan);
//
//   public OpProjectPlan getProjectPlan();

// FIXME(dfreis Feb 21, 2008 7:55:53 AM) - should also use a base ifc for OpAssignments
//   public void setAssignments(Set<OpAssignment> assignments);
//
//   public Set<OpAssignment> getAssignments();

// FIXME(dfreis Feb 21, 2008 7:55:53 AM) - should also use a base ifc for OpAssignments
//   public void setWorkPeriods(Set<OpWorkPeriod> workPeriods);
//
//   public Set<OpWorkPeriod> getWorkPeriods();

// FIXME(dfreis Feb 21, 2008 7:55:53 AM) - should also use a base ifc for OpAssignments
//   public void setAttachments(Set<OpAttachment> attachments);
//
//   public Set<OpAttachment> getAttachments();

   public OpResource getResponsibleResource();

   public void setResponsibleResource(OpResource responsibleResource);

   public double getBaseProceeds();

   public void setBaseProceeds(Double baseProceeds);

		public double getPayment();

		public void setPayment(Double payment);

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
		public List<Date> getStartEndDateByType();

		public double getEffortBillable();

		public void setEffortBillable(Double billable);
}
