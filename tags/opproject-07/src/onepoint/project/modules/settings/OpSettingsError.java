/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.settings;

/**
 * Codes and names for errors that can occur in <code>OpSettingsService</code>
 * @author ovidiu.lupas
 */
public class OpSettingsError {
   // Error codes
   public final static int LAST_WORK_DAY_INCORRECT = 1;
   public final static int DAY_WORK_TIME_INCORRECT = 2;
   public final static int WEEK_WORK_TIME_INCORRECT = 3;
   public final static int EMAIL_INCORRECT = 4;
   public final static int REPORT_REMOVE_TIME_PERIOD_INCORRECT = 5;
   public final static int RESOURCE_MAX_AVAILABILITY_INCORRECT = 6;   


   // Error names
   public final static String LAST_WORK_DAY_INCORRECT_NAME = "LastWorkDayIncorrect";
   public final static String DAY_WORK_TIME_INCORRECT_NAME = "DayWorkTimeIncorrect";
   public final static String WEEK_WORK_TIME_INCORRECT_NAME = "WeekWorkTimeIncorrect";
   public final static String EMAIL_INCORRECT_NAME = "EmailIncorrect";
   public final static String REPORT_REMOVE_TIME_PERIOD_INCORRECT_NAME = "ReportsRemoveTimePeriodIncorrect";
   public final static String RESOURCE_MAX_AVAILABILITY_INCORRECT_NAME = "ResourceMaxAvailabilityIncorrect";
}
