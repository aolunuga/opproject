/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.configuration_wizard.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.project.configuration.OpConfigurationValuesHandler;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for db_configuration_wizard form
 *
 * @author ovidiu.lupas
 */
public class OpDbConfigurationWizardFormProvider implements XFormProvider {

   /*form's component ids */
   public static final String DB_TYPE_DATA_SET = "DBTypeDataSet";
   public static final String DB_TYPE_CHOICE_FIELD = "DatabaseTypeChoiceField";
   public static final String DEFAULT_DB_URL_DATA_FIELD = "DefaultDbURLDataField";
   public static final String DEFAULT_DB_LOGIN_DATA_FIELD = "DefaultDbLoginDataField";
   public static final String DEFAULT_DB_PASSWORD_DATA_FIELD = "DefaultDbPasswordDataField";

   /*the request params */
   public static final String LOCAL_APPLICATION = "localApplication";

   /**
    * @see XFormProvider#prepareForm(onepoint.service.server.XSession, onepoint.express.XComponent, java.util.HashMap)
    */
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      Boolean localParam = (Boolean)parameters.get(LOCAL_APPLICATION);
      //flag indicating that the choice field should be populated with HSQLDB entry (only for local application)
      boolean isLocal = (localParam != null) && localParam.booleanValue();

      XComponent dataSet = form.findComponent(DB_TYPE_DATA_SET);
      fillDbTypeDataSet(dataSet, isLocal);
      //set the default selected index
      int selectedIndex = 0;
      ((XComponent) dataSet.getChild(0)).setSelected(true);
      XComponent choiceField = form.findComponent(DB_TYPE_CHOICE_FIELD);
      choiceField.setSelectedIndex(new Integer(selectedIndex));

      if (isLocal) {
         form.findComponent(DEFAULT_DB_URL_DATA_FIELD).setStringValue(onepoint.project.configuration.OpConfiguration.getHSQLDbUrl());
         form.findComponent(DEFAULT_DB_LOGIN_DATA_FIELD).setStringValue(onepoint.project.configuration.OpConfiguration.HSQL_DB_USER);
         form.findComponent(DEFAULT_DB_PASSWORD_DATA_FIELD).setStringValue(onepoint.project.configuration.OpConfiguration.HSQL_DB_PASSWORD);
      }
   }

   /**
    * Fills up the form's database type data set with necessary data
    *
    * @param dataSet <code>XComponent.DATA_SET</code>
    * @param isLocal <code>boolean</code> flag indicating that HSQLDB entry should be present in the data set.
    */
   private void fillDbTypeDataSet(XComponent dataSet, boolean isLocal) {
      XComponent dataRow ;
      //MySQL
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice("", OpConfigurationValuesHandler.MYSQL_DB_TYPE));
      dataSet.addDataRow(dataRow);
      //Oracle
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice("", OpConfigurationValuesHandler.ORACLE_DB_TYPE));
      dataSet.addDataRow(dataRow);
      //IBM DB/2
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice("", OpConfigurationValuesHandler.IBM_DB2_DB_TYPE));
      dataSet.addDataRow(dataRow);
      //PostrgeSQL
      dataRow = new XComponent(XComponent.DATA_ROW);
      dataRow.setStringValue(XValidator.choice("", OpConfigurationValuesHandler.POSTGRESQL_DB_TYPE));
      dataSet.addDataRow(dataRow);
      if (isLocal) {
         //HSQLDB
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(XValidator.choice("", OpConfigurationValuesHandler.HSQL_DB_TYPE));
         dataSet.addDataRow(dataRow);
      }
   }
}
