/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;

import onepoint.persistence.OpObject;

public class OpWorkPeriod extends OpObject implements OpWorkPeriodIfc {

   public final static String WORK_PERIOD = "OpWorkPeriod";

   public final static String START = "Begin";
   public final static String BASE_EFFORT = "BaseEffort";
   public final static String WORKING_DAYS = "WorkingDays";
   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String ACTIVITY = "Activity";

   private Date start;
   private long workingDays;
   private double baseEffort;
   private OpProjectPlan projectPlan;
   OpActivity activity;

   public void setStart(Date start) {
      this.start = start;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpWorkPeriodIfc#getStart()
    */
   public Date getStart() {
      return start;
   }

   public void setWorkingDays(long workingDays) {
      this.workingDays = workingDays;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpWorkPeriodIfc#getWorkingDays()
    */
   public long getWorkingDays() {
      return workingDays;
   }

   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

   /* (non-Javadoc)
    * @see onepoint.project.modules.project.OpWorkPeriodIfc#getBaseEffort()
    */
   public double getBaseEffort() {
      return baseEffort;
   }

   public void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }

   public OpProjectPlan getProjectPlan() {
      return projectPlan;
   }

   public void setActivity(OpActivity activity) {
      this.activity = activity;
   }

   public OpActivity getActivity() {
      return activity;
   }

}
