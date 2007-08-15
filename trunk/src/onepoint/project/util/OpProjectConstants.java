/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.util;

/**
 * Class used for holding constants used in more than one place and specific to the application.
 *
 * @author horia.chiorean
 */
public interface OpProjectConstants {

   public static final String RUN_LEVEL = "runLevel";
   public static final String GET_RUN_LEVEL_ACTION = "GetRunLevel";
   public static final byte CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL = 0;
   public static final String CONFIGURATION_WIZARD_FORM = "/modules/configuration_wizard/forms/configuration_wizard.oxf.xml";
   public static final String STANDALONE_CONFIGURATION_WIZARD_FORM = "/modules/configuration_wizard/forms/standalone_configuration_wizard.oxf.xml";
   public static final String DEFAULT_START_FORM = "/forms/login.oxf.xml";
   public static final String START_FORM = "/forms/start.oxf.xml";
   public static final String CHILDREN = "children";
   public static final String DUMMY_ROW_ID = "DummyChildId";
   public static final String REFRESH_PARAM = "refresh";
   public static final String PROJECT_PACKAGE = "onepoint/project";
   public static final String SIGNON_ACTION = "UserService.signOn";
   public static final String REMEMBER_PARAM = "remember";
   public static final String LOGIN_PARAM = "login";
   public static final String PASSWORD_PARAM = "password";
   public static final String CLIENT_TIMEZONE = "clientTimeZone";

   /**
    * General application flavour codes.
    */
   public static final String BASIC_EDITION_CODE = "OPPBE";
   public static final String PROFESSIONAL_EDITION_CODE = "OPPPE";
   public static final String OPEN_EDITION_CODE = "OPPOE";
   public static final String TEAM_EDITION_CODE = "OPPTE";
   public static final String ON_DEMAND_EDITION_CODE = "OPPOD";

   /**
    * The code version number.
    */
   public static final String CODE_VERSION_NUMBER = "07";
   public static final String CALENDAR = "calendar";

   /**
    * HSQL BD folder name.
    */
   public static final String DB_DIR_NAME = "repository";

   /**
    * HSQL BD file name.
    */
   public static final String DB_FILE_NAME = "onepoint";

   /**
    * Backup/restore folder name.
    */
   public static final String BACKUP_DIR_NAME = "backup";

   /**
    * Constants needed for attachment management
    */
   public final static String LINKED_ATTACHMENT_DESCRIPTOR = "u";
   public final static String DOCUMENT_ATTACHMENT_DESCRIPTOR = "d";
   public final static String NO_CONTENT_ID = "0";

   /**
    * Success Run Level.
    */
   public static final byte SUCCESS_RUN_LEVEL = 6;

   public static final String RUN_LEVEL_ERROR_FORM = "/forms/runLevel.oxf.xml";

   public static final String INIT_PARAMS = "initParams";
   String PROJECT_ID = "project_id";

   /**
    * User level types
    */
   public static final byte OBSERVER_CUSTOMER_USER_LEVEL = -1;
   public static final byte OBSERVER_USER_LEVEL = 0;
   public static final byte CONTRIBUTOR_USER_LEVEL = 1;
   public static final byte MANAGER_USER_LEVEL = 2;

   /**
    * License service name
    */
   public static final String LICENSE_SERVICE_NAME = "LicenseService";  
}
