/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.repository;

import onepoint.error.XErrorMap;

/**
 * Error map for the repository module's service errors.
 *
 * @author Horia Chiorean
 */
public class OpRepositoryErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "repository.error";

   protected OpRepositoryErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpRepositoryError.BACKUP_ERROR_CODE, OpRepositoryError.BACKUP_ERROR_NAME);
      registerErrorCode(OpRepositoryError.RESTORE_ERROR_CODE, OpRepositoryError.RESTORE_ERROR_NAME);
      registerErrorCode(OpRepositoryError.RESET_ERROR_CODE, OpRepositoryError.RESET_ERROR_NAME);
      registerErrorCode(OpRepositoryError.INSUFICIENT_PERMISSIONS_ERROR_CODE, OpRepositoryError.INSUFICIENT_PERMISSIONS_ERROR_NAME);
      registerErrorCode(OpRepositoryError.INVALID_ADMIN_PASSWORD, OpRepositoryError.INVALID_ADMIN_PASSWORD_NAME);
   }
}
