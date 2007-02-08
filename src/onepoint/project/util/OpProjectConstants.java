/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used for holding constants used in more than one place and specific to the application.
 *
 * @author horia.chiorean
 */
public abstract class OpProjectConstants {

   public static String RUN_LEVEL = "runLevel";
   public static String GET_RUN_LEVEL_ACTION = "GetRunLevel";
   public static Byte CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL = new Byte((byte) 0);
   public static String CONFIGURATION_WIZARD_FORM = "/modules/configuration_wizard/forms/configuration_wizard.oxf.xml";
   public static String STANDALONE_CONFIGURATION_WIZARD_FORM = "/modules/configuration_wizard/forms/standalone_configuration_wizard.oxf.xml";
   public static String DEFAULT_START_FORM = "/forms/login.oxf.xml";
   public static String CHILDREN = "children";
   public static String DUMMY_ROW_ID = "DummyChildId";
   public static String REFRESH_PARAM = "refresh";
   public static String PROJECT_PACKAGE = "onepoint/project";

   public static String BASIC_EDITION_CODE = "OPPBE";
   public static String PROFESSIONAL_EDITION_CODE = "OPPPE";
   public static String OPEN_EDITION_CODE = "OPPOE";
   public static String TEAM_EDITION_CODE = "OPPTE";
   public static Map PRODUCT_CODES = new HashMap();
   
   /**
    * Initializer for the map of product codes.
    * <FIXME author="Horia Chiorean" description="Check if the product names should come from an i18n file">
    */
   static {
      PRODUCT_CODES.put(BASIC_EDITION_CODE, "Onepoint Project Basic Edition");
      PRODUCT_CODES.put(PROFESSIONAL_EDITION_CODE, "Onepoint Project Professional Edition");
      PRODUCT_CODES.put(OPEN_EDITION_CODE, "Onepoint Project Open Edition");
      PRODUCT_CODES.put(TEAM_EDITION_CODE, "Onepoint Project Team Edition");
   }
}
