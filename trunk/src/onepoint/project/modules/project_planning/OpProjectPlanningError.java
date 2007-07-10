/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

/**
 * Project Planning Error 
 *
 * @author : mihai.costin
 */
public class OpProjectPlanningError {

   // Error codes
   public final static int COMMENT_NAME_MISSING = 1;
   public final static int COMMENT_NOT_FOUND = 2;
   public final static int ADMINISTRATOR_ACCESS_DENIED = 3;
   public final static int HOURLY_RATES_MODIFIED_WARNING = 4;
   public final static int AVAILIBILITY_MODIFIED_WARNING = 5;
   public final static int AVAILIBILITY_AND_RATES_MODIFIED_WARNING = 6;
   public final static int MSPROJECT_FILE_READ_ERROR = 7;
   public final static int MSPROJECT_FILE_WRITE_ERROR = 8;
   public final static int INSUFICIENT_ATTACHMENT_PERMISSIONS = 9;
   public final static int PROJECT_CHECK_OUT_ERROR = 10;
   public final static int PROJECT_CHECK_IN_ERROR = 11;
   public final static int PROJECT_REVERT_ERROR = 12;
   public final static int PROJECT_SAVE_ERROR = 13;
   public final static int PROJECT_CHECKED_IN_ERROR = 14;
   public final static int IMPORT_ERROR_WORK_RECORDS_EXIST = 15;
   public final static int INVALID_BASE_EFFORT_ERROR = 16;
   public final static int INVALID_PROJECT_NODE_TYPE_FOR_IMPORT = 17;
   public final static int INVALID_PROJECT_NODE_TYPE_FOR_EXPORT = 18;

   // Error names
   public final static String COMMENT_NAME_MISSING_NAME = "CommentNameMissing";
   public final static String COMMENT_NOT_FOUND_NAME = "CommentNotFound";
   public final static String ADMINISTRATOR_ACCESS_DENIED_NAME = "AdministratorAccessDenied";
   public final static String HOURLY_RATES_MODIFIED_WARNING_NAME = "HourlyRatesModifiedWarning";
   public final static String AVAILIBILITY_MODIFIED_WARNING_NAME = "AvailabilityModifiedWarning";
   public final static String AVAILIBILITY_AND_RATES_MODIFIED_WARNING_NAME = "AvailabilityAndRatesModifiedWarning";
   public final static String MSPROJECT_FILE_READ_ERROR_NAME = "MsProjectFileReadError";
   public final static String MSPROJECT_FILE_WRITE_ERROR_NAME = "MsProjectFileWriteError";
   public final static String INSUFICIENT_ATTACHMENT_PERMISSIONS_NAME = "InsuficientAttachmentPermissions";
   public final static String PROJECT_CHECK_OUT_ERROR_NAME = "ProjectCheckOutError";
   public final static String PROJECT_CHECK_IN_ERROR_NAME = "ProjectCheckInError";
   public final static String PROJECT_REVERT_ERROR_NAME = "ProjectRevertError";
   public final static String PROJECT_SAVE_ERROR_NAME = "ProjectSaveError";
   public final static String PROJECT_CHECKED_IN_ERROR_NAME = "ProjectCheckedInError";
   public final static String IMPORT_ERROR_WORK_RECORDS_EXIST_NAME = "ImportErrorWorkRecordsExist";
   public final static String INVALID_BASE_EFFORT_ERROR_NAME = "InvalidBaseEffortError";
   public final static String INVALID_PROJECT_NODE_TYPE_FOR_IMPORT_NAME = "InvalidProjectNodeTypeForImport";
   public final static String INVALID_PROJECT_NODE_TYPE_FOR_EXPORT_NAME = "InvalidProjectNodeTypeForExport";   

}
