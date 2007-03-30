/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_status;

/**
 * Project Status errors.
 *
 * @author mihai.costin
 */
public abstract class OpProjectStatusError {

   // Error codes
   public final static int PROJECT_STATUS_NAME_NOT_SPECIFIED = 1;
   public final static int PROJECT_STATUS_NAME_NOT_UNIQUE = 2;
   public final static int PROJECT_STATUS_NOT_FOUND = 3;
   public final static int INSUFFICIENT_PRIVILEGES = 4;

   // Error names
   public final static String PROJECT_STATUS_NAME_NOT_SPECIFIED_NAME = "StatusNameNotSpecified";
   public final static String PROJECT_STATUS_NAME_NOT_UNIQUE_NAME = "StatusNameNotUnique";
   public final static String PROJECT_STATUS_NOT_FOUND_NAME = "StatusNotFound";
   public final static String INSUFFICIENT_PRIVILEGES_NAME = "InsufficientPrivileges";

}
