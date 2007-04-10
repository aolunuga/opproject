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
   public static final Byte CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL = new Byte((byte) 0);
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

   /**
    * The code version number.
    */
   public static final String CODE_VERSION_NUMBER = "07";
   public static final String CALENDAR = "calendar";
}
