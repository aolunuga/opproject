/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.license;

import onepoint.error.XErrorMap;

/**
 * License Error Map.
 *
 * @author mihai.costin
 */
public class OpLicenseErrorMap extends XErrorMap {

   /**
    * @see onepoint.error.XErrorMap#XErrorMap(String)
    */
   public OpLicenseErrorMap() {
      super("license.error");
      registerErrorCode(OpLicenseError.MANAGER_ERROR, OpLicenseError.MANAGER_ERROR_NAME);
      registerErrorCode(OpLicenseError.STANDARD_ERROR, OpLicenseError.STANDARD_ERROR_NAME);
      registerErrorCode(OpLicenseError.DATE_EXPIRED_ERROR, OpLicenseError.DATE_EXPIRED_ERROR_NAME);
   }
}
