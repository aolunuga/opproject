/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

import java.sql.Date;

public class OpWorkPeriod extends OpObject {

   public final static String WORK_PERIOD = "OpWorkPeriod";

   public final static String START = "Begin";
   public final static String BASE_EFFORT = "BaseEffort";
   public final static String WORKING_DAYS = "WorkingDays";
   public final static String PROJECT_PLAN = "ProjectPlan";
   public final static String ACTIVITY = "Activity";

   public final static int PERIOD_LENGTH = 31;

   private Date start;
   private long workingDays;
   private double baseEffort;
   private OpProjectPlan projectPlan;
   private OpActivity activity;

   public void setStart(Date start) {
      this.start = start;
   }

   public Date getStart() {
      return start;
   }

   public void setWorkingDays(long workingDays) {
      this.workingDays = workingDays;
   }

   public long getWorkingDays() {
      return workingDays;
   }

   public void setBaseEffort(double baseEffort) {
      this.baseEffort = baseEffort;
   }

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
