/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

public class OpProjectError {

   // Error codes
   public final static int PROJECT_NAME_MISSING = 1;
   public final static int START_DATE_MISSING = 2;
   public final static int END_DATE_INCORRECT = 3;
   public final static int PROJECT_NAME_ALREADY_USED = 4;
   public final static int PORTFOLIO_NAME_MISSING = 5;
   public final static int PORTFOLIO_NAME_ALREADY_USED = 6;
   public final static int BUDGET_INCORRECT = 7;
   public final static int UPDATE_ACCESS_DENIED = 8;

   public final static int TEMPLATE_NAME_MISSING = 9;
   public final static int TEMPLATE_NAME_ALREADY_USED = 10;
   public final static int PROJECT_NOT_SPECIFIED = 11;
   public final static int PROJECT_NOT_FOUND = 12;
   public final static int TARGET_PORTFOLIO_NOT_SELECTED = 13;

   public final static int MANAGER_ACCESS_DENIED = 14;
   public final static int WORKRECORDS_STILL_EXIST_ERROR = 15;
   public final static int WORKRECORDS_STILL_EXIST_WARNING = 16;
   public final static int ACTIVITY_ASSIGNMENTS_EXIST_ERROR = 17;

   public final static int PROJECT_LOCKED_ERROR = 18;
   public final static int GOAL_PRIORITY_ERROR = 19;
   public final static int TODO_PRIORITY_ERROR = 20;

   // Error names
   public final static String PROJECT_NAME_MISSING_NAME = "ProjectNameMissing";
   public final static String START_DATE_MISSING_NAME = "StartDateMissing";
   public final static String END_DATE_INCORRECT_NAME = "EndDateIncorrect";
   public final static String PROJECT_NAME_ALREADY_USED_NAME = "ProjectNameAlreadyUsed";
   public final static String PORTFOLIO_NAME_MISSING_NAME = "PortfolioNameMissing";
   public final static String PORTFOLIO_NAME_ALREADY_USED_NAME = "PortfolioNameAlreadyUsed";
   public final static String BUDGET_INCORRECT_NAME = "BudgetIncorrect";
   public final static String UPDATE_ACCESS_DENIED_NAME = "UpdateAccessDenied";
   public final static String TEMPLATE_NAME_MISSING_NAME = "TemplateNameMissing";
   public final static String TEMPLATE_NAME_ALREADY_USED_NAME = "TemplateNameAlreadyUsed";
   public final static String PROJECT_NOT_SPECIFIED_NAME = "ProjectNotSpecified";
   public final static String PROJECT_NOT_FOUND_NAME = "ProjectNotFound";
   public final static String TARGET_PORTFOLIO_NOT_SELECTED_NAME = "TargetPortfolioNotSelected";
   public final static String MANAGER_ACCESS_DENIED_NAME = "ManagerAccessDenied";
   public final static String WORKRECORDS_STILL_EXIST_ERR0R_NAME = "WorkRecordsStillExistError";
   public final static String WORKRECORDS_STILL_EXIST_WARNING_NAME = "WorkRecordsStillExistWarning";
   public final static String ACTIVITY_ASSIGNMENTS_EXIST_ERROR_NAME = "ActivityAssignmentsExistError";
   public final static String PROJECT_LOCKED_ERROR_NAME = "ProjectLockedError";
   public final static String GOAL_PRIORITY_ERROR_NAME = "GoalPriorityError";
   public final static String TODO_PRIORITY_ERROR_NAME = "TodoPriorityError";
}
