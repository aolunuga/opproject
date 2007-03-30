/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.user;

import onepoint.error.XErrorMap;

public class OpUserErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "user.error";
   
   OpUserErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpUserError.GROUP_NOT_EMPTY, OpUserError.GROUP_NOT_EMPTY_NAME);
      registerErrorCode(OpUserError.GROUP_NOT_FOUND, OpUserError.GROUP_NOT_FOUND_NAME);
      registerErrorCode(OpUserError.GROUP_NAME_ALREADY_USED, OpUserError.GROUP_NAME_ALREADY_USED_NAME);
      registerErrorCode(OpUserError.EMAIL_INCORRECT, OpUserError.EMAIL_INCORRECT_NAME);
      registerErrorCode(OpUserError.LOGIN_ALREADY_USED, OpUserError.LOGIN_ALREADY_USED_NAME);
      registerErrorCode(OpUserError.FIRST_NAME_MISSING, OpUserError.FIRST_NAME_MISSING_NAME);
      registerErrorCode(OpUserError.GROUP_NAME_MISSING, OpUserError.GROUP_NAME_MISSING_NAME);
      registerErrorCode(OpUserError.LAST_NAME_MISSING, OpUserError.LAST_NAME_MISSING_NAME);
      registerErrorCode(OpUserError.LOGIN_MISSING, OpUserError.LOGIN_MISSING_NAME);
      registerErrorCode(OpUserError.PASSWORD_MISSING, OpUserError.PASSWORD_MISSING_NAME);
      registerErrorCode(OpUserError.PASSWORD_MISMATCH, OpUserError.PASSWORD_MISMATCH_NAME);
      registerErrorCode(OpUserError.USER_NOT_FOUND, OpUserError.USER_NOT_FOUND_NAME);
      registerErrorCode(OpUserError.USER_UNKNOWN, OpUserError.USER_UNKNOWN_NAME);
      registerErrorCode(OpUserError.INSUFFICIENT_PRIVILEGES, OpUserError.INSUFFICIENT_PRIVILEGES_NAME);
      registerErrorCode(OpUserError.SESSION_USER, OpUserError.SESSION_USER_NAME);
      registerErrorCode(OpUserError.EVERYONE_GROUP, OpUserError.EVERYONE_GROUP_NAME);
      registerErrorCode(OpUserError.LOOP_ASSIGNMENT, OpUserError.LOOP_ASSIGNMENT_NAME);
      registerErrorCode(OpUserError.SUPER_GROUP_NOT_FOUND, OpUserError.SUPER_GROUP_NOT_FOUND_NAME);
      registerErrorCode(OpUserError.DEMOTE_USER_ERROR, OpUserError.DEMOTE_USER_ERROR_NAME);
      registerErrorCode(OpUserError.INVALID_USER_LEVEL, OpUserError.INVALID_USER_LEVEL_NAME);
      registerErrorCode(OpUserError.ADMIN_PERMISSION_ERROR, OpUserError.ADMIN_PERMISSION_ERROR_NAME);
      registerErrorCode(OpUserError.PERMISSION_LEVEL_ERROR, OpUserError.PERMISSION_LEVEL_ERROR_NAME);
   }

}
