/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.configuration.OpConfigurationValuesHandler;
import onepoint.project.modules.user.OpUser;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.server.XSession;

import java.io.File;
import java.util.HashMap;

/**
 * Form provider for the configuration_wizard_basic form provider.
 *
 * @author horia.chiorean
 */
public class OpStandaloneDbConfigurationWizardFormProvider implements XFormProvider {

   /**
    * This class' logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpStandaloneDbConfigurationWizardFormProvider.class);

   /**
    * Standalone database parameters
    */
   private static final String DB_DEFAULT_USER = "sa";
   private static final String DB_DEFAULT_PASSWORD = "";
   private static final String DB_DEFAULT_FILENAME = "opproject";


   /**
    * Form's component ids
    */
   private static final String DB_TYPE_FIELD_ID = "DbTypeDataField";
   private static final String DB_LOGIN_FIELD_ID = "DbLoginDataField";
   private static final String DB_PASS_FIELD_ID = "DbPasswordDataField";
   private static final String DB_LOCATION_FIELD_ID = "DbPathTextField";
   private static final String DB_DEFAULT_NAME = "DbDefaultName";
   private static final String DEFAULT_LOGIN = "DefaultLogin";
   private static final String DEFAULT_PASSWORD = "DefaultPassword";

   /**
    * @see onepoint.express.server.XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      form.findComponent(DB_TYPE_FIELD_ID).setStringValue(OpConfigurationValuesHandler.DERBY_DB_TYPE);
      form.findComponent(DB_LOGIN_FIELD_ID).setStringValue(DB_DEFAULT_USER);
      form.findComponent(DB_PASS_FIELD_ID).setStringValue(DB_DEFAULT_PASSWORD);
      form.findComponent(DB_DEFAULT_NAME).setStringValue(DB_DEFAULT_FILENAME);
      String dbDirPath = OpEnvironmentManager.getDataFolderPath() + File.separator + OpProjectConstants.DB_DIR_NAME;
      form.findComponent(DB_LOCATION_FIELD_ID).setStringValue(dbDirPath + File.separator + DB_DEFAULT_FILENAME);
      form.findComponent(DEFAULT_LOGIN).setStringValue(OpUser.ADMINISTRATOR_NAME);
      form.findComponent(DEFAULT_PASSWORD).setStringValue("");
   }
}
