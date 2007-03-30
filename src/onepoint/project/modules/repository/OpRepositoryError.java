/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.repository;

/**
 * Interface for the repository's module constants.
 *
 * @author horia.chiorean
 */
public interface OpRepositoryError {
   /**
    * Error codes.
    */
   public static final int BACKUP_ERROR_CODE = 1;
   public static final int RESTORE_ERROR_CODE = 2;
   public static final int RESET_ERROR_CODE = 3;
   public static final int INSUFICIENT_PERMISSIONS_ERROR_CODE = 3;
   public static final int INVALID_ADMIN_PASSWORD = 4;

   /**
    * Error constants.
    */
   public static final String BACKUP_ERROR_NAME = "BackupError";
   public static final String RESTORE_ERROR_NAME = "RestoreError";
   public static final String RESET_ERROR_NAME = "ResetError";
   public static final String INSUFICIENT_PERMISSIONS_ERROR_NAME = "InsuficientPermissions";
   public static final String INVALID_ADMIN_PASSWORD_NAME = "InvalidAdminPassword";

}
