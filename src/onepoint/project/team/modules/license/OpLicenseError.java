/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license;

/**
 * License Errors definition.
 *
 * @author mihai.costin
 */
public interface OpLicenseError {

   /**
    * Error constants.
    */
   static final int MANAGER_ERROR = 1;
   static final int STANDARD_ERROR = 2;
   static final int DATE_EXPIRED_ERROR = 3;


   /**
    * Error names
    */
   static final String MANAGER_ERROR_NAME = "LicenseManagerExpired";
   static final String STANDARD_ERROR_NAME = "LicenseStandardExpired";
   static final String DATE_EXPIRED_ERROR_NAME = "LicenseDateExpired";

}
