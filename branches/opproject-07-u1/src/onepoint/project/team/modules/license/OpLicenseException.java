/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license;

/**
 * Exception class used for any kind of license problems.
 *
 * @author horia.chiorean
 */
public final class OpLicenseException extends Exception {

   /**
    * @see Exception#Exception(String)
    */
   public OpLicenseException(String message) {
      super(message);
   }
}
