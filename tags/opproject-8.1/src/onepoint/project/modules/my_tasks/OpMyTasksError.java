/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks;

/**
 * My Tasks Errors definition.
 *
 * @author mihai.costin
 */
public interface OpMyTasksError {

   /**
    * Error constants.
    */
   static final int EMPTY_NAME_ERROR_CODE = 1;
   static final int INVALID_DUE_DATE_ERROR_CODE = 2;
   static final int NO_PROJECT_ERROR_CODE = 3;
   static final int NO_RESOURCE_ERROR_CODE = 4;
   static final int EXISTING_WORKSLIP_ERROR_CODE = 5;
   static final int INSUFICIENT_PERMISSIONS_ERROR_CODE = 6;
   static final int INVALID_PRIORITY_ERROR_CODE = 7;
   static final int INVALID_TYPE_ERROR_CODE = 8;
   static final int TASK_NOT_FOUND_ERROR_CODE = 9;

   /**
    * Error names
    */
   static final String EMPTY_NAME_NAME = "EmptyName";
   static final String INVALID_DUE_DATE_NAME = "InvalidDueDate";
   static final String NO_PROJECT_NAME = "NoProjectError";
   static final String NO_RESOURCE_NAME = "NoResourceError";
   static final String EXISTING_WORKSLIP_ERROR_NAME = "ExistingWorkSlipError";
   static final String INSUFICIENT_PERMISSIONS_ERROR_NAME = "InsuficientPermissions";
   static final String INVALID_PRIORITY_ERROR_NAME = "InvalidPriorityErrorMessage";
   static final String INVALID_TYPE_ERROR_NAME = "InvalidTypeError";
   static final String TASK_NOT_FOUND_ERROR_NAME = "TaskNotFoundError";

}
