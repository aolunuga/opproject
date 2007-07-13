/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;
import java.util.Map;

public class OpConfigurationValuesHandler implements XNodeHandler {

   public final static String CONFIGURATION = "configuration";
   public final static String DATABASE_TYPE = "database-type";
   public final static String DATABASE_DRIVER = "database-driver";
   public final static String DATABASE_URL = "database-url";
   public final static String DATABASE_LOGIN = "database-login";
   public final static String DATABASE_PASSWORD = "database-password";
   public final static String DATABASE_PATH = "database-path";
   public final static String BROWSER = "browser";
   public final static String SMTP_SERVER = "smtp-server";
   public final static String LOG_FILE = "log-file";
   public final static String LOG_LEVEL = "log-level";
   public final static String CONNECTION_POOL_MINSIZE = "connection-pool-min-size";
   public final static String CONNECTION_POOL_MAXSIZE = "connection-pool-max-size";
   public final static String CACHE_SIZE = "cache-size";
   public final static String JES_DEBUGGING = "jes-debugging";
   public final static String SECURE_SERVICE = "secure-service";
   public final static String RESOURCE_CACHE_SIZE = "resource-cache-size";
   public final static String BACKUP_PATH = "backup-path";
   /**
    * Database types
    */
   public final static String DERBY_DB_TYPE = "Derby";
   public final static String MYSQL_INNO_DB_TYPE = "MySQLInnoDB";
   public final static String POSTGRESQL_DB_TYPE = "PostgreSQL";
   public final static String ORACLE_DB_TYPE = "Oracle";
   public final static String IBM_DB2_DB_TYPE = "IBMDB2";
   public final static String HSQL_DB_TYPE = "HSQLDB";
   public final static String MSSQL_DB_TYPE = "MSSQL";

   /**
    * A map from db type name to db type (int constant).
    */
   public final static Map<String, Integer> DATABASE_TYPES_MAP;

   /**
    * Db password encrypted attribute.
    */
   final static String ENCRYPTED_ATTRIBUTE = "encrypted";

   static {
      DATABASE_TYPES_MAP = new HashMap<String, Integer>();
      DATABASE_TYPES_MAP.put(DERBY_DB_TYPE, OpHibernateSource.DERBY);
      DATABASE_TYPES_MAP.put(MYSQL_INNO_DB_TYPE, OpHibernateSource.MYSQL_INNODB);
      DATABASE_TYPES_MAP.put(POSTGRESQL_DB_TYPE, OpHibernateSource.POSTGRESQL);
      DATABASE_TYPES_MAP.put(ORACLE_DB_TYPE, OpHibernateSource.ORACLE);
      DATABASE_TYPES_MAP.put(HSQL_DB_TYPE, OpHibernateSource.HSQLDB);
      DATABASE_TYPES_MAP.put(IBM_DB2_DB_TYPE, OpHibernateSource.IBM_DB2);
      DATABASE_TYPES_MAP.put(MSSQL_DB_TYPE, OpHibernateSource.MSSQL);
   }

   public Object newNode(XContext context, String name, HashMap attributes) {
      //see whether we have an encrypted password
      if (name.equals(DATABASE_PASSWORD)) {
         for (Object o : attributes.keySet()) {
            String attributeName = (String) o;
            String attributeValue = (String) attributes.get(attributeName);
            if (attributeName.equals(ENCRYPTED_ATTRIBUTE)) {
               context.setVariable(attributeName, attributeValue);
            }
         }
      }
      return new StringBuffer();
   }

   public void addChildNode(XContext context, Object node, String child_name, Object child) {
   }

   public void addNodeContent(XContext context, Object node, String content) {
      ((StringBuffer) node).append(content);
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      if (DATABASE_TYPE.equals(name)) {
         String value = node.toString().trim();
         Integer dbType = DATABASE_TYPES_MAP.get(value);
         if (dbType != null) {
            ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(dbType);
         }
         else {
            System.err.println("WARNING: Unknown database type specified in configuration: " + value);
         }
      }
      else if (DATABASE_DRIVER.equals(name)) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseDriver(node.toString());
      }
      else if (DATABASE_URL.equals(name)) {
         OpConfiguration.DatabaseConfiguration configuration = ((OpConfiguration) parent).getDatabaseConfiguration();
         String databaseUrl = node.toString();
         if (configuration.getDatabaseType() == OpHibernateSource.HSQLDB) {
            databaseUrl = databaseUrl.replaceAll("[\\\\]", "/");
            OpEnvironmentManager.setDataFolderPathFromDbPath(databaseUrl.replaceFirst(OpHibernateSource.HSQLDB_JDBC_CONNECTION_PREFIX, ""));
         }
         else if (configuration.getDatabaseType() == OpHibernateSource.MYSQL_INNODB) {
            // set the connection params to support large blobs
            databaseUrl = setJDBCBoolParam(databaseUrl, "useServerPrepStmts", true);
            databaseUrl = setJDBCBoolParam(databaseUrl, "emulateLocators", true);
         }
         else if (configuration.getDatabaseType() == OpHibernateSource.MSSQL) {
            // set the timezone
            StringBuffer sb = new StringBuffer(databaseUrl);
            if (!databaseUrl.endsWith(";")) {
               sb.append(';');
            }
            sb.append("useTimeZone=GMT");
            databaseUrl = sb.toString();
         }
         configuration.setDatabaseUrl(databaseUrl);
      }
      else if (DATABASE_LOGIN.equals(name)) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseLogin(node.toString());
      }
      else if (DATABASE_PATH.equals(name)) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabasePath(node.toString());
      }
      else if (DATABASE_PASSWORD.equals(name)) {
         String encryptedValue = (String) context.getVariable(ENCRYPTED_ATTRIBUTE);
         String databasePassword = node.toString();
         OpConfiguration.DatabaseConfiguration databaseConfiguration = ((OpConfiguration) parent).getDatabaseConfiguration();
         if (encryptedValue == null || !Boolean.valueOf(encryptedValue)) {
            databaseConfiguration.setNeedsPasswordEncryption(true);
            databaseConfiguration.setDatabasePassword(databasePassword);
         }
         else {
            databaseConfiguration.setNeedsPasswordEncryption(false);
            databaseConfiguration.setDatabasePassword(onepoint.project.configuration.OpConfiguration.getUnEncryptedDbPassword(databasePassword));
         }
      }
      else if (CONNECTION_POOL_MINSIZE.equals(name)) {
         String value = node.toString();
         ((OpConfiguration) parent).getDatabaseConfiguration().setConnectionPoolMinSize(value);
      }
      else if (CONNECTION_POOL_MAXSIZE.equals(name)) {
         String value = node.toString();
         ((OpConfiguration) parent).getDatabaseConfiguration().setConnectionPoolMaxSize(value);
      }
      else if (CACHE_SIZE.equals(name)) {
         String value = node.toString();
         ((OpConfiguration) parent).getCacheConfiguration().setCacheSize(value);
      }
      else if (BROWSER.equals(name)) {
         ((OpConfiguration) parent).setBrowserApplication(node.toString());
      }
      else if (SMTP_SERVER.equals(name)) {
         ((OpConfiguration) parent).setSMTPServer(node.toString());
      }
      else if (LOG_FILE.equals(name)) {
         ((OpConfiguration) parent).setLogFile(node.toString());
      }
      else if (LOG_LEVEL.equals(name)) {
         ((OpConfiguration) parent).setLogLevel(node.toString());
      }
      else if (SECURE_SERVICE.equals(name)) {
         ((OpConfiguration) parent).setSecureService(node.toString());
      }
      else if (JES_DEBUGGING.equals(name)) {
         boolean jessDebugging = Boolean.valueOf(node.toString());
         ((OpConfiguration) parent).setSourceDebugging(jessDebugging);
      }
      else if (RESOURCE_CACHE_SIZE.equals(name)) {
         String value = node.toString();
         ((OpConfiguration) parent).getCacheConfiguration().setResourceCacheSize(value);
      }
      else if (BACKUP_PATH.equals(name)) {
         String value = node.toString();
         ((OpConfiguration) parent).setBackupPath(value);
      }
   }

   /**
    * Set a boolean parameter for a JDBC connection URL
    *
    * @param databaseUrl the initial JDBC connection URL
    * @param param       the name of the parameter to set
    * @param value       the <code>boolean</code> value of the parameter to be set
    * @return the new JDBC connection URL string
    */
   private static String setJDBCBoolParam(String databaseUrl, String param, boolean value) {
      boolean hasParams = databaseUrl.indexOf('?') > -1;
      if (hasParams) {
         boolean hasThisParam = databaseUrl.contains(param);
         if (hasThisParam) {
            boolean isParamSet = databaseUrl.contains(param + '=' + value);
            if (!isParamSet) {
               databaseUrl = databaseUrl.replace(param + '=' + !value, param + '=' + value);
            }
         }
         else {
            databaseUrl = databaseUrl.concat('&' + param + '=' + value);
         }
      }
      else {
         databaseUrl = databaseUrl.concat('?' + param + '=' + value);
      }
      return databaseUrl;
   }
}
