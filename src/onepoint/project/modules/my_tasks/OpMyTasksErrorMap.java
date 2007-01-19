/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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
      registerErrorCode(OpMyTasksError.INVALID_DUE_DATE, OpMyTasksError.INVALID_DUE_DATE_NAME);
      registerErrorCode(OpMyTasksError.EMPTY_NAME, OpMyTasksError.EMPTY_NAME_NAME);
      registerErrorCode(OpMyTasksError.NO_PROJECT, OpMyTasksError.NO_PROJECT_NAME);
      registerErrorCode(OpMyTasksError.NO_RESOURCE, OpMyTasksError.NO_RESOURCE_NAME);
      registerErrorCode(OpMyTasksError.EXISTING_WORKSLIP, OpMyTasksError.EXISTING_WORKSLIP_NAME);
   }
}
