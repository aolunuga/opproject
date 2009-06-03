/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

import onepoint.error.XErrorMap;

/**
 * @author : mihai.costin
 */
public class OpProjectPlanningErrorMap extends XErrorMap {

   private final static String RESOURCE_MAP_ID = "project_planning.error";

   OpProjectPlanningErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpProjectPlanningError.COMMENT_NAME_MISSING, OpProjectPlanningError.COMMENT_NAME_MISSING_NAME);
      registerErrorCode(OpProjectPlanningError.COMMENT_NOT_FOUND, OpProjectPlanningError.COMMENT_NOT_FOUND_NAME);
      registerErrorCode(OpProjectPlanningError.ADMINISTRATOR_ACCESS_DENIED, OpProjectPlanningError.ADMINISTRATOR_ACCESS_DENIED_NAME);
      registerErrorCode(OpProjectPlanningError.PROJECT_MODIFIED_WARNING, OpProjectPlanningError.PROJECT_MODIFIED_WARNING_NAME);
      registerErrorCode(OpProjectPlanningError.RESOURCES_MODIFIED_WARNING, OpProjectPlanningError.RESOURCES_MODIFIED_WARNING_NAME);
      registerErrorCode(OpProjectPlanningError.PROJECT_AND_RESOURCES_MODIFIED_WARNING, OpProjectPlanningError.PROJECT_AND_RESOURCES_MODIFIED_WARNING_NAME);
      registerErrorCode(OpProjectPlanningError.MSPROJECT_FILE_READ_ERROR, OpProjectPlanningError.MSPROJECT_FILE_READ_ERROR_NAME);
      registerErrorCode(OpProjectPlanningError.MSPROJECT_FILE_WRITE_ERROR, OpProjectPlanningError.MSPROJECT_FILE_WRITE_ERROR_NAME);
      registerErrorCode(OpProjectPlanningError.INSUFICIENT_ATTACHMENT_PERMISSIONS, OpProjectPlanningError.INSUFICIENT_ATTACHMENT_PERMISSIONS_NAME);
      registerErrorCode(OpProjectPlanningError.PROJECT_CHECKED_IN_ERROR, OpProjectPlanningError.PROJECT_CHECKED_IN_ERROR_NAME);

      registerErrorCode(OpProjectPlanningError.PROJECT_CHECK_IN_ERROR, OpProjectPlanningError.PROJECT_CHECK_IN_ERROR_NAME);
      registerErrorCode(OpProjectPlanningError.PROJECT_CHECK_OUT_ERROR, OpProjectPlanningError.PROJECT_CHECK_OUT_ERROR_NAME);
      registerErrorCode(OpProjectPlanningError.PROJECT_REVERT_ERROR, OpProjectPlanningError.PROJECT_REVERT_ERROR_NAME);
      registerErrorCode(OpProjectPlanningError.PROJECT_SAVE_ERROR, OpProjectPlanningError.PROJECT_SAVE_ERROR_NAME);
      registerErrorCode(OpProjectPlanningError.IMPORT_ERROR_WORK_RECORDS_EXIST, OpProjectPlanningError.IMPORT_ERROR_WORK_RECORDS_EXIST_NAME);
      registerErrorCode(OpProjectPlanningError.INVALID_BASE_EFFORT_ERROR, OpProjectPlanningError.INVALID_BASE_EFFORT_ERROR_NAME);
      registerErrorCode(OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_EXPORT, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_EXPORT_NAME);
      registerErrorCode(OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_IMPORT, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_IMPORT_NAME);
      registerErrorCode(OpProjectPlanningError.CALENDARS_MODIFIED_WARNING, OpProjectPlanningError.CALENDARS_MODIFIED_WARNING_NAME);
      registerErrorCode(OpProjectPlanningError.ACTION_NAME_NOT_UNIQUE, OpProjectPlanningError.ACTION_NAME_NOT_UNIQUE_NAME);
      registerErrorCode(OpProjectPlanningError.PROJECT_START_DATE_AFTER_ACTIVITY_DATE, OpProjectPlanningError.PROJECT_START_DATE_AFTER_ACTIVITY_DATE_NAME);
      registerErrorCode(OpProjectPlanningError.SUB_PROJECT_LOOP_ERROR, OpProjectPlanningError.SUB_PROJECT_LOOP_ERROR_NAME);
   }
}
