/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.error.XErrorMap;

public class OpProjectErrorMap extends XErrorMap {

   private final static String RESOURCE_MAP_ID = "project.error";

   public OpProjectErrorMap() {

      super(RESOURCE_MAP_ID);
      registerErrorCode(OpProjectError.PROJECT_NAME_MISSING, OpProjectError.PROJECT_NAME_MISSING_NAME);
      registerErrorCode(OpProjectError.PORTFOLIO_NAME_ALREADY_USED, OpProjectError.PORTFOLIO_NAME_ALREADY_USED_NAME);
      registerErrorCode(OpProjectError.START_DATE_MISSING, OpProjectError.START_DATE_MISSING_NAME);
      registerErrorCode(OpProjectError.END_DATE_INCORRECT, OpProjectError.END_DATE_INCORRECT_NAME);
      registerErrorCode(OpProjectError.BUDGET_INCORRECT, OpProjectError.BUDGET_INCORRECT_NAME);
      registerErrorCode(OpProjectError.PORTFOLIO_NAME_MISSING, OpProjectError.PORTFOLIO_NAME_MISSING_NAME);
      registerErrorCode(OpProjectError.PROJECT_NAME_ALREADY_USED, OpProjectError.PROJECT_NAME_ALREADY_USED_NAME);
      registerErrorCode(OpProjectError.UPDATE_ACCESS_DENIED, OpProjectError.UPDATE_ACCESS_DENIED_NAME);
      registerErrorCode(OpProjectError.TEMPLATE_NAME_MISSING, OpProjectError.TEMPLATE_NAME_MISSING_NAME);
      registerErrorCode(OpProjectError.TEMPLATE_NAME_ALREADY_USED, OpProjectError.TEMPLATE_NAME_ALREADY_USED_NAME);
      registerErrorCode(OpProjectError.PROJECT_NOT_SPECIFIED,OpProjectError.PROJECT_NOT_SPECIFIED_NAME);
      registerErrorCode(OpProjectError.PROJECT_NOT_FOUND,OpProjectError.PROJECT_NOT_FOUND_NAME);
      registerErrorCode(OpProjectError.TARGET_PORTFOLIO_NOT_SELECTED,OpProjectError.TARGET_PORTFOLIO_NOT_SELECTED_NAME);
      registerErrorCode(OpProjectError.MANAGER_ACCESS_DENIED,OpProjectError.MANAGER_ACCESS_DENIED_NAME);
      registerErrorCode(OpProjectError.WORKRECORDS_STILL_EXIST_ERROR,OpProjectError.WORKRECORDS_STILL_EXIST_ERR0R_NAME);
      registerErrorCode(OpProjectError.WORKRECORDS_STILL_EXIST_WARNING,OpProjectError.WORKRECORDS_STILL_EXIST_WARNING_NAME);
      registerErrorCode(OpProjectError.ACTIVITY_ASSIGNMENTS_EXIST_ERROR, OpProjectError.ACTIVITY_ASSIGNMENTS_EXIST_ERROR_NAME);
      registerErrorCode(OpProjectError.PROJECT_LOCKED_ERROR, OpProjectError.PROJECT_LOCKED_ERROR_NAME);
      registerErrorCode(OpProjectError.TODO_PRIORITY_ERROR, OpProjectError.TODO_PRIORITY_ERROR_NAME);
      registerErrorCode(OpProjectError.GOAL_PRIORITY_ERROR, OpProjectError.GOAL_PRIORITY_ERROR_NAME);
      registerErrorCode(OpProjectError.LOOP_ASSIGNMENT_ERROR, OpProjectError.LOOP_ASSIGNMENT_ERROR_NAME);
   }
}
