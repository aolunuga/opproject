/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard;

import onepoint.error.XErrorMap;

/**
 * DB configuration wizard map where error codes are registered
 *
 * @author ovidiu.lupas
 */
public class OpDbConfigurationWizardErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "configuration_wizard.error";

   public OpDbConfigurationWizardErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpDbConfigurationWizardError.DATABASE_URL_MISSING, OpDbConfigurationWizardError.DATABASE_URL_MISSING_NAME);
      registerErrorCode(OpDbConfigurationWizardError.DATABASE_LOGIN_MISSING, OpDbConfigurationWizardError.DATABASE_LOGIN_MISSING_NAME);
      registerErrorCode(OpDbConfigurationWizardError.INVALID_CREDENTIALS, OpDbConfigurationWizardError.INVALID_CREDENTIALS_NAME);
      registerErrorCode(OpDbConfigurationWizardError.INVALID_CONNECTION_STRING, OpDbConfigurationWizardError.INVALID_CONNECTION_STRING_NAME);
      registerErrorCode(OpDbConfigurationWizardError.GENERAL_CONNECTION_ERROR, OpDbConfigurationWizardError.GENERAL_CONNECTION_ERROR_NAME);
      registerErrorCode(OpDbConfigurationWizardError.JDBC_DRIVER_ERROR, OpDbConfigurationWizardError.JDBC_DRIVER_ERROR_NAME);
   }
}
