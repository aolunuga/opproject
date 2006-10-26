/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.util;

/**
 * Enum class used for holding constants used in more than one place and specific to the application.
 *
 * @author horia.chiorean
 */
public interface OpProjectConstants {

   /**
    * Run level related constants.
    */
   public String RUN_LEVEL = "runLevel";
   public Byte CONFIGURATION_WIZARD_REQUIRED_RUN_LEVEL = new Byte((byte) 0);
   public String CONFIGURATION_FORM = "/modules/configuration_wizard/forms/configuration_wizard.oxf.xml";
   public String RESOURCE_ID = "resourceId";
   public String RESOURCE_MAP_ID = "resourceMapId";
   public String DEFAULT_START_FORM = "/forms/login.oxf.xml";
}
