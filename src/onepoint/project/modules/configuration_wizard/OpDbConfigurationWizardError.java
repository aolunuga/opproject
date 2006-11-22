/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard;

/**
 * Error codes and names for database configuration wizard
 *
 * @author ovidiu.lupas
 */
public class OpDbConfigurationWizardError {
   // Error codes
   public final static int DATABASE_URL_MISSING = 1;
   public final static int DATABASE_LOGIN_MISSING = 2;
   public final static int INVALID_CREDENTIALS = 3;
   public final static int INVALID_CONNECTION_STRING = 4;
   public final static int GENERAL_CONNECTION_ERROR = 5;
   public final static int JDBC_DRIVER_ERROR = 6;

   //Error names
   public final static String DATABASE_URL_MISSING_NAME = "DatabaseUrlMissing";
   public final static String DATABASE_LOGIN_MISSING_NAME = "DatabaseLoginMissing";
   public final static String INVALID_CREDENTIALS_NAME = "InvalidCredentials";
   public final static String INVALID_CONNECTION_STRING_NAME = "InvalidConnectionString";
   public final static String GENERAL_CONNECTION_ERROR_NAME = "GeneralConnectionError";
   public final static String JDBC_DRIVER_ERROR_NAME = "JDBCDriverError";

}
