/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;
import java.util.Iterator;
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
   public final static String MYSQL_DB_TYPE = "MySQL";
   public final static String MYSQL_INNO_DB_TYPE = "MySQLInnoDB";
   public final static String POSTGRESQL_DB_TYPE = "PostgreSQL";
   public final static String ORACLE_DB_TYPE = "Oracle";
   public final static String IBM_DB2_DB_TYPE = "IBMDB2";
   public final static String HSQL_DB_TYPE = "HSQLDB";
   public final static String MSSQL_DB_TYPE = "MSSQL";

   /**
    * A map from db type name to db type (int constant).
    */
   public final static Map DATABASE_TYPES_MAP;

   /**
    * Db password encrypted attribute.
    */
   final static String ENCRYPTED_ATTRIBUTE = "encrypted";

   static {
      DATABASE_TYPES_MAP = new HashMap();
      DATABASE_TYPES_MAP.put(DERBY_DB_TYPE, new Integer(OpHibernateSource.DERBY));
      DATABASE_TYPES_MAP.put(MYSQL_DB_TYPE, new Integer(OpHibernateSource.MYSQL));
      DATABASE_TYPES_MAP.put(MYSQL_INNO_DB_TYPE, new Integer(OpHibernateSource.MYSQL_INNODB));
      DATABASE_TYPES_MAP.put(POSTGRESQL_DB_TYPE, new Integer(OpHibernateSource.POSTGRESQL));
      DATABASE_TYPES_MAP.put(ORACLE_DB_TYPE, new Integer(OpHibernateSource.ORACLE));
      DATABASE_TYPES_MAP.put(HSQL_DB_TYPE, new Integer(OpHibernateSource.HSQLDB));
      DATABASE_TYPES_MAP.put(IBM_DB2_DB_TYPE, new Integer(OpHibernateSource.IBM_DB2));
      DATABASE_TYPES_MAP.put(MSSQL_DB_TYPE, new Integer(OpHibernateSource.MSSQL));
   }

   public Object newNode(XContext context, String name, HashMap attributes) {
      //see whether we have an encrypted password
      if (name.equals(DATABASE_PASSWORD)) {
         Iterator it = attributes.keySet().iterator();
         while (it.hasNext()) {
            String attributeName = (String) it.next();
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
      if (name == DATABASE_TYPE) {
         String value = ((StringBuffer) node).toString().trim();
         Integer dbType = (Integer) DATABASE_TYPES_MAP.get(value);
         if (dbType != null) {
            ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(dbType.intValue());
         }
         else {
            System.err.println("WARNING: Unknown database type specified in configuration: " + value);
         }
      }
      else if (name == DATABASE_DRIVER) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseDriver(((StringBuffer) node).toString());
      }
      else if (name == DATABASE_URL) {
         OpConfiguration.DatabaseConfiguration configuration = ((OpConfiguration) parent).getDatabaseConfiguration();
         String databaseUrl = ((StringBuffer) node).toString();
         if (configuration.getDatabaseType() == OpHibernateSource.HSQLDB) {
            databaseUrl = databaseUrl.replaceAll("[\\\\]","/");
            OpEnvironmentManager.setDataFolderPathFromDbPath(databaseUrl.replaceFirst(OpHibernateSource.HSQLDB_JDBC_CONNECTION_PREFIX, ""));
         }
         configuration.setDatabaseUrl(databaseUrl);
      }
      else if (name == DATABASE_LOGIN) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseLogin(((StringBuffer) node).toString());
      }
      else if (name == DATABASE_PATH) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabasePath(((StringBuffer) node).toString());
      }
      else if (name == DATABASE_PASSWORD) {
         String encryptedValue = (String) context.getVariable(ENCRYPTED_ATTRIBUTE);
         String databasePassword = ((StringBuffer) node).toString();
         OpConfiguration.DatabaseConfiguration databaseConfiguration = ((OpConfiguration) parent).getDatabaseConfiguration();
         if (encryptedValue == null || !Boolean.valueOf(encryptedValue).booleanValue()) {
            databaseConfiguration.setNeedsPasswordEncryption(true);
            databaseConfiguration.setDatabasePassword(databasePassword);
         }
         else {
            databaseConfiguration.setNeedsPasswordEncryption(false);
            databaseConfiguration.setDatabasePassword(onepoint.project.configuration.OpConfiguration.getUnEncryptedDbPassword(databasePassword));
         }
      }
      else if (name == CONNECTION_POOL_MINSIZE) {
         String value = ((StringBuffer) node).toString();
         ((OpConfiguration) parent).getDatabaseConfiguration().setConnectionPoolMinSize(value);
      }
      else if (name == CONNECTION_POOL_MAXSIZE) {
         String value = ((StringBuffer) node).toString();
         ((OpConfiguration) parent).getDatabaseConfiguration().setConnectionPoolMaxSize(value);
      }
      else if (name == CACHE_SIZE) {
         String value = ((StringBuffer) node).toString();
         ((OpConfiguration) parent).getCacheConfiguration().setCacheSize(value);
      }
      else if (name == BROWSER) {
         ((OpConfiguration) parent).setBrowserApplication(((StringBuffer) node).toString());
      }
      else if (name == SMTP_SERVER) {
         ((OpConfiguration) parent).setSMTPServer(((StringBuffer) node).toString());
      }
      else if (name == LOG_FILE) {
         ((OpConfiguration) parent).setLogFile(((StringBuffer) node).toString());
      }
      else if (name == LOG_LEVEL) {
         ((OpConfiguration) parent).setLogLevel(((StringBuffer) node).toString());
      }
      else if (name == SECURE_SERVICE) {
         ((OpConfiguration) parent).setSecureService(((StringBuffer) node).toString());
      }
      else if (name == JES_DEBUGGING) {
         boolean jessDebugging = Boolean.valueOf(((StringBuffer) node).toString()).booleanValue();
         ((OpConfiguration) parent).setSourceDebugging(jessDebugging);
      }
      else if (name == RESOURCE_CACHE_SIZE) {
         String value = ((StringBuffer) node).toString();
         ((OpConfiguration) parent).getCacheConfiguration().setResourceCacheSize(value);
      }
      else if (name == BACKUP_PATH) {
         String value = ((StringBuffer) node).toString();
         ((OpConfiguration) parent).setBackupPath(value);
      }
   }
}
