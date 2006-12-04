/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

public class OpProjectError {

   // Error codes
   public final static int PROJECT_NAME_MISSING = 1;
   public final static int START_DATE_MISSING = 2;
   public final static int END_DATE_INCORRECT = 5;
   public final static int PROJECT_NAME_ALREADY_USED = 6;

   public final static int PORTFOLIO_NAME_MISSING = 7;
   public final static int PORTFOLIO_NAME_ALREADY_USED = 8;

   public final static int BUDGET_INCORRECT = 10;
   public final static int PRIORITY_INCORRECT = 11;

   public final static int UPDATE_ACCESS_DENIED = 12;
   
   public final static int TEMPLATE_NAME_MISSING = 13;
   public final static int TEMPLATE_NAME_ALREADY_USED = 14;
   public final static int PROJECT_NOT_SPECIFIED = 15;
   public final static int PROJECT_NOT_FOUND = 16;
   public final static int TARGET_PORTFOLIO_NOT_SELECTED = 17;

   public final static int COMMENT_NAME_MISSING = 18;
   public final static int COMMENT_NOT_FOUND = 19;
   public final static int ADMINISTRATOR_ACCESS_DENIED = 20;
   public final static int MANAGER_ACCESS_DENIED = 21;
   public final static int WORKRECORDS_STILL_EXIST_ERROR = 22;
   public final static int WORKRECORDS_STILL_EXIST_WARNING = 23;
   public final static int HOURLY_RATES_MODIFIED_WARNING = 24;
   public final static int AVAILIBILITY_MODIFIED_WARNING = 25;
   public final static int AVAILIBILITY_AND_RATES_MODIFIED_WARNING = 26;
   public final static int ACTIVITY_ASSIGNMENTS_EXIST_ERROR = 27;
   public final static int FILE_READ_ERROR = 28;
   public final static int FILE_WRITE_ERROR = 29;
   public final static int PERMISSIONS_LEVEL_ERROR = 30;

   public final static int PROJECT_LOCKED_ERROR = 31;
   public final static int PROJECT_CHECKED_IN_ERROR = 32;


   // Error names
   public final static String PROJECT_NAME_MISSING_NAME = "ProjectNameMissing";
   public final static String START_DATE_MISSING_NAME = "StartDateMissing";
   public final static String END_DATE_INCORRECT_NAME = "EndDateIncorrect";
   public final static String PROJECT_NAME_ALREADY_USED_NAME = "ProjectNameAlreadyUsed";

   public final static String PORTFOLIO_NAME_MISSING_NAME = "PortfolioNameMissing";
   public final static String PORTFOLIO_NAME_ALREADY_USED_NAME = "PortfolioNameAlreadyUsed";

   public final static String BUDGET_INCORRECT_NAME = "BudgetIncorrect";
   public final static String PRIORITY_INCORRECT_NAME = "PriorityIncorrect";

   public final static String UPDATE_ACCESS_DENIED_NAME = "UpdateAccessDenied";
   
   public final static String TEMPLATE_NAME_MISSING_NAME = "TemplateNameMissing";
   public final static String TEMPLATE_NAME_ALREADY_USED_NAME = "TemplateNameAlreadyUsed";
   public final static String PROJECT_NOT_SPECIFIED_NAME = "ProjectNotSpecified";
   public final static String PROJECT_NOT_FOUND_NAME = "ProjectNotFound";
   public final static String TARGET_PORTFOLIO_NOT_SELECTED_NAME = "TargetPortfolioNotSelected";

   public final static String COMMENT_NAME_MISSING_NAME = "CommentNameMissing";
   public final static String COMMENT_NOT_FOUND_NAME = "CommentNotFound";
   public final static String ADMINISTRATOR_ACCESS_DENIED_NAME = "AdministratorAccessDenied";
   public final static String MANAGER_ACCESS_DENIED_NAME = "ManagerAccessDenied";
   public final static String WORKRECORDS_STILL_EXIST_ERR0R_NAME = "WorkRecordsStillExistError";
   public final static String WORKRECORDS_STILL_EXIST_WARNING_NAME = "WorkRecordsStillExistWarning";
   public final static String HOURLY_RATES_MODIFIED_WARNING_NAME = "HourlyRatesModifiedWarning";
   public final static String AVAILIBILITY_MODIFIED_WARNING_NAME = "AvailabilityModifiedWarning";
   public final static String AVAILIBILITY_AND_RATES_MODIFIED_WARNING_NAME = "AvailabilityAndRatesModifiedWarning";
   public final static String ACTIVITY_ASSIGNMENTS_EXIST_ERROR_NAME = "ActivityAssignmentsExistError";
   public final static String FILE_READ_ERROR_NAME = "FileReadError";
   public final static String FILE_WRITE_ERROR_NAME = "FileWriteError";
   public final static String PERMISSIONS_LEVEL_ERROR_NAME = "PermissionsLevelError";

   public final static String PROJECT_LOCKED_ERROR_NAME = "ProjectLockedError";
   public final static String PROJECT_CHECKED_IN_ERROR_NAME = "ProjectCheckedInError";
}
