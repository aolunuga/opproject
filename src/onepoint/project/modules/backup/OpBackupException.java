/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.backup;

/**
 * Exception class for the logical backup/restore of the application.
 *
 * @author horia.chiorean
 */
public class OpBackupException extends RuntimeException {
   /**
    * @see RuntimeException#RuntimeException(String)
    */
   public OpBackupException(String message) {
      super(message);
   }
}
