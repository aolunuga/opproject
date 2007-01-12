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


   /**
    * Error names
    */
   static final String EMPTY_NAME_NAME = "EmptyName";
   static final String INVALID_DUE_DATE_NAME = "InvalidDueDate";
   static final String NO_PROJECT_NAME = "NoProjectError";
   static final String NO_RESOURCE_NAME = "NoResourceError";

}
