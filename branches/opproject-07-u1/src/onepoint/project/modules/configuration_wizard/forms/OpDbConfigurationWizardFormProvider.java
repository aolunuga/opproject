/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpConnectionManager;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfigurationValuesHandler;
import onepoint.project.modules.configuration_wizard.OpDbConfigurationWizardError;
import onepoint.resource.XLanguageResourceMap;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for db_configuration_wizard form
 *
 * @author ovidiu.lupas
 */
public class OpDbConfigurationWizardFormProvider implements XFormProvider {

   /*form's component ids */
   private static final String DB_TYPE_DATA_SET = "DBTypeDataSet";
   private static final String DB_TYPE_CHOICE_FIELD = "DatabaseTypeChoiceField";
   private static final String DB_URL_FIELD = "DatabaseUrlTextField";
   private static final String ERROR_LABEL_FIELD = "ErrorLabel";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      XComponent dataSet = form.findComponent(DB_TYPE_DATA_SET);
      fillDbTypeDataSet(dataSet);

      //set the default selected index
      int selectedIndex = 0;
      XComponent defaultSelectedRow = (XComponent) dataSet.getChild(selectedIndex);
      defaultSelectedRow.setSelected(true);
      form.findComponent(DB_URL_FIELD).setStringValue(XValidator.choiceID(defaultSelectedRow.getStringValue()));
      XComponent choiceField = form.findComponent(DB_TYPE_CHOICE_FIELD);
      choiceField.setSelectedIndex(new Integer(selectedIndex));

      //check whether an error code should already be displayed
      int connectionTestCode = OpInitializer.getConnectionTestCode();
      if (connectionTestCode != OpConnectionManager.SUCCESS) {
         XLocalizer localizer = new XLocalizer();
         XLanguageResourceMap resourceMap = XLocaleManager.findResourceMap(((OpProjectSession) session).getLocale().getID(), "configuration_wizard.error");
         localizer.setResourceMap(resourceMap);
         XComponent errorLabel = form.findComponent(ERROR_LABEL_FIELD);
         errorLabel.setVisible(true);
         switch(connectionTestCode) {
            case OpConnectionManager.GENERAL_CONNECTION_EXCEPTION: {
               String text = localizer.localize("{$" + OpDbConfigurationWizardError.GENERAL_CONNECTION_ERROR_NAME + "}");
               errorLabel.setText(text);
               break;
            }
            case OpConnectionManager.INVALID_CONNECTION_STRING_EXCEPTION: {
               String text = localizer.localize("{$" + OpDbConfigurationWizardError.INVALID_CONNECTION_STRING_NAME + "}");
               errorLabel.setText(text);
               break;
            }
            case OpConnectionManager.MISSINING_DRIVER_EXCEPTION: {
               String text = localizer.localize("{$" + OpDbConfigurationWizardError.JDBC_DRIVER_ERROR_NAME + "}");
               errorLabel.setText(text);
               break;
            }
            case OpConnectionManager.INVALID_CREDENTIALS_EXCEPTION: {
               String text = localizer.localize("{$" + OpDbConfigurationWizardError.INVALID_CREDENTIALS_NAME + "}");
               errorLabel.setText(text);
               break;
            }
         }
      }
   }

   /**
    * Fills up the form's database type data set with necessary data
    *
    * @param dataSet <code>XComponent.DATA_SET</code>
    */
   private void fillDbTypeDataSet(XComponent dataSet) {
      XComponent dataRow ;
      //MySQL
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice("jdbc:mysql://localhost:3306/opproject", OpConfigurationValuesHandler.MYSQL_DB_TYPE));
      dataSet.addDataRow(dataRow);
      //Oracle
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice("jdbc:oracle:thin:@localhost:1521", OpConfigurationValuesHandler.ORACLE_DB_TYPE));
      dataSet.addDataRow(dataRow);
      //IBM DB/2
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice("jdbc:db2://localhost:446/opproject", OpConfigurationValuesHandler.IBM_DB2_DB_TYPE));
      dataSet.addDataRow(dataRow);
      //PostrgeSQL
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice("jdbc:postgresql://localhost:5432/opproject", OpConfigurationValuesHandler.POSTGRESQL_DB_TYPE));
      dataSet.addDataRow(dataRow);
   }
}
