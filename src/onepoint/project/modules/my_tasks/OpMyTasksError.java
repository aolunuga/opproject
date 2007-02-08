/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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
   static final int EMPTY_NAME = 1;
   static final int INVALID_DUE_DATE = 2;
   static final int NO_PROJECT = 3;
   static final int NO_RESOURCE = 4;
   static final int EXISTING_WORKSLIP = 4;
   static final int INSUFICIENT_PERMISSIONS_ERROR_CODE = 5;


   /**
    * Error names
    */
   static final String EMPTY_NAME_NAME = "EmptyName";
   static final String INVALID_DUE_DATE_NAME = "InvalidDueDate";
   static final String NO_PROJECT_NAME = "NoProjectError";
   static final String NO_RESOURCE_NAME = "NoResourceError";
   static final String EXISTING_WORKSLIP_NAME = "ExistingWorkSlipError";
   static final String INSUFICIENT_PERMISSIONS_ERROR_NAME = "InsuficientPermissions";

}
