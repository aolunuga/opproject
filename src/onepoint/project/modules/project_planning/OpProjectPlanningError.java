/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project_planning;

/**
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
   public final static int FILE_READ_ERROR = 7;
   public final static int FILE_WRITE_ERROR = 8;

   // Error names
   public final static String COMMENT_NAME_MISSING_NAME = "CommentNameMissing";
   public final static String COMMENT_NOT_FOUND_NAME = "CommentNotFound";
   public final static String ADMINISTRATOR_ACCESS_DENIED_NAME = "AdministratorAccessDenied";
   public final static String HOURLY_RATES_MODIFIED_WARNING_NAME = "HourlyRatesModifiedWarning";
   public final static String AVAILIBILITY_MODIFIED_WARNING_NAME = "AvailabilityModifiedWarning";
   public final static String AVAILIBILITY_AND_RATES_MODIFIED_WARNING_NAME = "AvailabilityAndRatesModifiedWarning";
   public final static String FILE_READ_ERROR_NAME = "FileReadError";
   public final static String FILE_WRITE_ERROR_NAME = "FileWriteError";
}
