/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource;

import onepoint.error.XErrorMap;

public class OpResourceErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "resource.error";

   OpResourceErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpResourceError.RESOURCE_NAME_NOT_SPECIFIED, OpResourceError.RESOURCE_NAME_NOT_SPECIFIED_NAME);
      registerErrorCode(OpResourceError.RESOURCE_NAME_NOT_UNIQUE, OpResourceError.RESOURCE_NAME_NOT_UNIQUE_NAME);
      registerErrorCode(OpResourceError.POOL_NAME_NOT_SPECIFIED, OpResourceError.POOL_NAME_NOT_SPECIFIED_NAME);
      registerErrorCode(OpResourceError.POOL_NAME_NOT_UNIQUE, OpResourceError.POOL_NAME_NOT_UNIQUE_NAME);
      registerErrorCode(OpResourceError.AVAILABILITY_NOT_VALID, OpResourceError.AVAILABILITY_NOT_VALID_NAME);
      registerErrorCode(OpResourceError.HOURLY_RATE_NOT_VALID, OpResourceError.HOURLY_RATE_NOT_VALID_NAME);
      registerErrorCode(OpResourceError.USER_ID_NOT_SPECIFIED, OpResourceError.USER_ID_NOT_SPECIFIED_NAME);
      registerErrorCode(OpResourceError.NAME_FORMAT_NOT_SPECIFIED, OpResourceError.NAME_FORMAT_NOT_SPECIFIED_NAME);
      registerErrorCode(OpResourceError.NAME_FORMAT_NOT_VALID, OpResourceError.NAME_FORMAT_NOT_VALID_NAME);
      registerErrorCode(OpResourceError.RESOURCE_NOT_FOUND, OpResourceError.RESOURCE_NOT_FOUND_NAME);
      registerErrorCode(OpResourceError.POOL_NOT_FOUND, OpResourceError.POOL_NOT_FOUND_NAME);
      registerErrorCode(OpResourceError.UPDATE_ACCESS_DENIED,OpResourceError.UPDATE_ACCESS_DENIED_NAME);
      registerErrorCode(OpResourceError.RESOURCE_NAME_NOT_VALID,OpResourceError.RESOURCE_NAME_NOT_VALID_NAME);
      registerErrorCode(OpResourceError.ASSIGN_ACCESS_DENIED,OpResourceError.ASSIGN_ACCESS_DENIED_NAME);
      registerErrorCode(OpResourceError.RESOURCE_ASSIGNMENT_WARNING,OpResourceError.RESOURCE_ASSIGNMENT_WARNING_NAME);
      registerErrorCode(OpResourceError.MANAGER_ACCESS_DENIED,OpResourceError.MANAGER_ACCESS_DENIED_NAME);
      registerErrorCode(OpResourceError.DELETE_RESOURCE_ASSIGNMENTS_DENIED,OpResourceError.DELETE_RESOURCE_ASSIGNMENTS_DENIED_NAME);
      registerErrorCode(OpResourceError.DELETE_POOL_RESOURCE_ASSIGNMENTS_DENIED,OpResourceError.DELETE_POOL_RESOURCE_ASSIGNMENTS_DENIED_NAME);
      registerErrorCode(OpResourceError.ACTIVITY_ASSIGNMENTS_EXIST_ERROR, OpResourceError.ACTIVITY_ASSIGNMENTS_EXIST_ERROR_NAME);
      registerErrorCode(OpResourceError.LOOP_ASSIGNMENT_ERROR, OpResourceError.LOOP_ASSIGNMENT_ERROR_NAME);
   }

}
