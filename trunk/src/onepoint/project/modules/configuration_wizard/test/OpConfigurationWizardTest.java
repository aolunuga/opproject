/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.configuration_wizard.test;

import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.configuration.OpConfiguration;
import onepoint.project.configuration.OpConfigurationValuesHandler;
import onepoint.project.configuration.OpDatabaseConfiguration;
import onepoint.project.modules.configuration_wizard.OpConfigurationWizardManager;
import onepoint.project.modules.configuration_wizard.OpDbConfigurationWizardError;
import onepoint.project.test.OpBaseOpenTestCase;
import onepoint.project.test.OpTestDataFactory;
import onepoint.project.util.OpProjectConstants;
import onepoint.service.XMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This class tests loading of configuration.
 *
 * @author lucian.furtos
 */
public class OpConfigurationWizardTest extends OpBaseOpenTestCase {
   private String dbURL;
   private String dbUserName;
   private String dbPassword;
   private int dbType;

   /**
    * Base set-up.  By default authenticate Administrator user.
    *
    * @throws Exception If setup process can not be successfuly finished
    */
   protected void setUp()
        throws Exception {
      super.setUp();

      // read testing configuration.
      OpConfiguration configuration = OpTestDataFactory.getTestingConfiguration();
      OpDatabaseConfiguration dataBaseConfiguration = configuration.getDatabaseConfigurations().iterator().next();

      dbURL = dataBaseConfiguration.getDatabaseUrl();
      dbPassword = dataBaseConfiguration.getDatabasePassword();
      dbUserName = dataBaseConfiguration.getDatabaseLogin();
      dbType = dataBaseConfiguration.getDatabaseType();

      OpConfigurationWizardManager.loadConfigurationWizardModule();
   }

   public void testDBConfig()
        throws Exception {
      HashMap params = new HashMap();
      params.put("is_standalone", Boolean.FALSE);
      params.put("database_type", getDatabaseString(dbType));
      params.put("database_url", dbURL);
      params.put("database_login", dbUserName);
      params.put("database_password", dbPassword);
      params.put("is_multi_user", Boolean.FALSE);
      params.put("load_demodata", Boolean.FALSE);

      XMessage request = new XMessage();
      request.setArgument("parameters", params);
      XMessage response = OpTestDataFactory.getConfigurationWizardService().writeDatabaseConfigurationFile(session, request);
      assertNoError(response);
      assertNotNull(response.getArgument(OpProjectConstants.INIT_PARAMS));
   }

   public void testDBConfigErrors()
        throws Exception {
      HashMap params = new HashMap();
      params.put("is_standalone", Boolean.FALSE);
      params.put("database_type", getDatabaseString(dbType));

      XMessage request = new XMessage();
      request.setArgument("parameters", new HashMap(params));
      XMessage response = OpTestDataFactory.getConfigurationWizardService().writeDatabaseConfigurationFile(session, request);
      assertError(response, OpDbConfigurationWizardError.DATABASE_URL_MISSING);

      params.put("database_url", dbURL);

      request = new XMessage();
      request.setArgument("parameters", new HashMap(params));
      response = OpTestDataFactory.getConfigurationWizardService().writeDatabaseConfigurationFile(session, request);
      assertError(response, OpDbConfigurationWizardError.DATABASE_LOGIN_MISSING);

      params.put("database_url", "wrong_url");
      params.put("database_login", "a");
      params.put("database_password", "a");

      request = new XMessage();
      request.setArgument("parameters", new HashMap(params));
      response = OpTestDataFactory.getConfigurationWizardService().writeDatabaseConfigurationFile(session, request);
      assertError(response, OpDbConfigurationWizardError.INVALID_CONNECTION_STRING);

      //if client authentication will be needed for Derby the following if can be taken out
      if (dbType != OpHibernateSource.DERBY) {
         params.put("database_url", dbURL);
         params.put("database_login", "a");
         params.put("database_password", "a");

         request = new XMessage();
         request.setArgument("parameters", new HashMap(params));
         response = OpTestDataFactory.getConfigurationWizardService().writeDatabaseConfigurationFile(session, request);
         assertError(response, OpDbConfigurationWizardError.INVALID_CREDENTIALS);
      }
   }

   /**
    * This method determines the STRING representation of the used database type.
    *
    * @param dbTypeToCheck database type.
    * @return database type as string. If this type can not be determined, MySQL type is returned.
    */
   private String getDatabaseString(int dbTypeToCheck) {
      Iterator it = OpConfigurationValuesHandler.DATABASE_TYPES_MAP.entrySet().iterator();
      while (it.hasNext()) {
         Map.Entry entry = (Map.Entry) it.next();
         String key = (String) entry.getKey();
         Integer value = (Integer) entry.getValue();

         if (value.intValue() == dbTypeToCheck) {
            return key;
         }
      }

      return OpConfigurationValuesHandler.MYSQL_INNO_DB_TYPE;
   }
}
