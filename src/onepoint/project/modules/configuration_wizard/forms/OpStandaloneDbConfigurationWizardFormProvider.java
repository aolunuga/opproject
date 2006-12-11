/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.configuration.OpConfigurationValuesHandler;
import onepoint.project.modules.user.OpUser;
import onepoint.service.server.XSession;
import onepoint.util.XEnvironmentManager;

import java.util.HashMap;

/**
 * Form provider for the configuration_wizard_basic form provider.
 *
 * @author horia.chiorean
 */
public class OpStandaloneDbConfigurationWizardFormProvider implements XFormProvider {

   /**
    * HSQL parameters
    */
   private static final String HSQL_DB_DIR = "/OnePoint Project Data";
   private static final String HSQL_DB_USER = "sa";
   private static final String HSQL_DB_PASSWORD = "";

   /**
    * Form's component ids
    */
   private static final String HSQL_DB_TYPE_FIELD_ID = "HSQLDbTypeDataField";
   private static final String HSQL_DB_LOGIN_FIELD_ID = "HSQLDDbLoginDataField";
   private static final String HSQL_DB_PASS_FIELD_ID = "HSQLDDbPasswordDataField";
   private static final String HSQL_DB_LOCATION_FIELD_ID = "HSQLDbPathTextField";
   private static final String DEFAULT_LOGIN = "DefaultLogin";
   private static final String DEFAULT_PASSWORD = "DefaultPassword";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      form.findComponent(HSQL_DB_TYPE_FIELD_ID).setStringValue(OpConfigurationValuesHandler.HSQL_DB_TYPE);
      form.findComponent(HSQL_DB_LOGIN_FIELD_ID).setStringValue(HSQL_DB_USER);
      form.findComponent(HSQL_DB_PASS_FIELD_ID).setStringValue(HSQL_DB_PASSWORD);
      String dbDirPath = XEnvironmentManager.convertPathToSlash(System.getProperty("user.home")) + HSQL_DB_DIR;
      form.findComponent(HSQL_DB_LOCATION_FIELD_ID).setStringValue(dbDirPath);

      form.findComponent(DEFAULT_LOGIN).setStringValue(OpUser.ADMINISTRATOR_NAME);
      form.findComponent(DEFAULT_PASSWORD).setStringValue("");
   }
}
