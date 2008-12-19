/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;

import onepoint.persistence.OpObject;

public class OpWorkPeriodVersion extends OpObject implements OpWorkPeriodIfc {

   public final static String WORK_PERIOD_VERSION = "OpWorkPeriodVersion";

   public final static String START = "Begin";
   public final static String WORKING_DAYS = "WorkingDays";
   public final static String BASE_EFFORT = "BaseEffort";
   public final static String PLAN_VERSION = "PlanVersion";
   public final static String ACTIVITY_VERSION = "ActivityVersion";

   private Date start;
   private Date finish;
   private double baseEffort;
   private OpProjectPlanVersion planVersion;
   private OpActivityVersion activityVersion;
   private long workingDays;

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

   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

   public double getBaseEffort() {
      return baseEffort;
   }

   public void setPlanVersion(OpProjectPlanVersion planVersion) {
      this.planVersion = planVersion;
   }

   public OpProjectPlanVersion getPlanVersion() {
      return planVersion;
   }

   public void setActivityVersion(OpActivityVersion activityVersion) {
      this.activityVersion = activityVersion;
   }

   public OpActivityVersion getActivityVersion() {
      return activityVersion;
   }

   public long getWorkingDays() {
      return workingDays;
   }

   public void setWorkingDays(long workingDays) {
      this.workingDays = workingDays;
   }

   public OpActivityVersion getActivity() {
      return activityVersion;
   }
}
