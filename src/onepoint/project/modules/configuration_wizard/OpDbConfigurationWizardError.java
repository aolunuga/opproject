/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
   public final static int DATABASE_PATH_MISSING = 7;
   public final static int INVALID_MYSQL_ENGINE = 8;
   public final static int INVALID_DEMODATA_FILE = 9;
   public final static int RESTORE_ERROR = 10;

   //Error names
   public final static String DATABASE_URL_MISSING_NAME = "DatabaseUrlMissing";
   public final static String DATABASE_LOGIN_MISSING_NAME = "DatabaseLoginMissing";
   public final static String INVALID_CREDENTIALS_NAME = "InvalidCredentials";
   public final static String INVALID_CONNECTION_STRING_NAME = "InvalidConnectionString";
   public final static String GENERAL_CONNECTION_ERROR_NAME = "GeneralConnectionError";
   public final static String JDBC_DRIVER_ERROR_NAME = "JDBCDriverError";
   public final static String DATABASE_PATH_MISSING_NAME = "DatabasePathMissing";
   public final static String INVALID_MYSQL_ENGINE_NAME = "InvalidMySqlEngine";
   public final static String INVALID_DEMODATA_FILE_NAME = "InvalidDemodataFile";
   public final static String RESTORE_ERROR_NAME = "RestoreError";
}
