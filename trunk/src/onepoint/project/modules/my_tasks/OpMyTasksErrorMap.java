/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.my_tasks;

import onepoint.error.XErrorMap;

/**
 * MyTasks Error Map.
 *
 * @author mihai.costin
 */
public class OpMyTasksErrorMap extends XErrorMap {

   /**
    * @see XErrorMap#XErrorMap(String)
    */
   public OpMyTasksErrorMap() {
      super("my_tasks.error");
      registerErrorCode(OpMyTasksError.INVALID_DUE_DATE_ERROR_CODE, OpMyTasksError.INVALID_DUE_DATE_NAME);
      registerErrorCode(OpMyTasksError.EMPTY_NAME_ERROR_CODE, OpMyTasksError.EMPTY_NAME_NAME);
      registerErrorCode(OpMyTasksError.NO_PROJECT_ERROR_CODE, OpMyTasksError.NO_PROJECT_NAME);
      registerErrorCode(OpMyTasksError.NO_RESOURCE_ERROR_CODE, OpMyTasksError.NO_RESOURCE_NAME);
      registerErrorCode(OpMyTasksError.EXISTING_WORKSLIP_ERROR_CODE, OpMyTasksError.EXISTING_WORKSLIP_ERROR_NAME);
      registerErrorCode(OpMyTasksError.INSUFICIENT_PERMISSIONS_ERROR_CODE, OpMyTasksError.INSUFICIENT_PERMISSIONS_ERROR_NAME);
      registerErrorCode(OpMyTasksError.INVALID_PRIORITY_ERROR_CODE, OpMyTasksError.INVALID_PRIORITY_ERROR_NAME);
      registerErrorCode(OpMyTasksError.INVALID_TYPE_ERROR_CODE, OpMyTasksError.INVALID_TYPE_ERROR_NAME);
      registerErrorCode(OpMyTasksError.TASK_NOT_FOUND_ERROR_CODE, OpMyTasksError.TASK_NOT_FOUND_ERROR_NAME);
   }
}
