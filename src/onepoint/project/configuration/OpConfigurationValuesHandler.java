/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.configuration;

import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.xml.XContext;
import onepoint.xml.XNodeHandler;

import java.util.HashMap;
import java.util.Iterator;

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
   /**
    * Database types
    */
   public final static String DERBY_DB_TYPE = "Derby";
   public final static String MYSQL_DB_TYPE = "MySQL";
   public final static String MYSQL_INNO_DB_TYPE = "MySQLInnoDB";
   public final static String POSTGRESQL_DB_TYPE = "PostgreSQL";
   public final static String ORACLE_DB_TYPE = "Oracle";
   public final static String IBM_DB2_DB_TYPE = "IBM DB/2";
   public final static String HSQL_DB_TYPE = "HSQLDB";
   /**
    * Db password encrypted attribute.
    */
   final static String ENCRYPTED_ATTRIBUTE = "encrypted";

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

   public void addChildNode(XContext context, Object node, String child_name, Object child) {}

   public void addNodeContent(XContext context, Object node, String content) {
      ((StringBuffer) node).append(content);
   }

   public void nodeFinished(XContext context, String name, Object node, Object parent) {
      if (name == DATABASE_TYPE) {
         String value = node.toString().trim();
         if (value.equals(DERBY_DB_TYPE)) {
            ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(OpHibernateSource.DERBY);
         }
         else if (value.equals(MYSQL_DB_TYPE)) {
            ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(OpHibernateSource.MYSQL);
         }
         else if (value.equals(MYSQL_INNO_DB_TYPE)) {
            ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(OpHibernateSource.MYSQL_INNODB);
         }
         else if (value.equals(POSTGRESQL_DB_TYPE)) {
            ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(OpHibernateSource.POSTGRESQL);
         }
         else if (value.equals(ORACLE_DB_TYPE)) {
            ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(OpHibernateSource.ORACLE);
         }
         else if (value.equals(HSQL_DB_TYPE)) {
            ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(OpHibernateSource.HSQLDB);
         }
         else if (value.equals(IBM_DB2_DB_TYPE)) {
        	 ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseType(OpHibernateSource.IBM_DB2);
         }
         else {
            System.err.println("WARNING: Unknown database type specified in configuration: " + value);
         }
         // TODO: Better error handling for unknown database type
      }
      else if (name == DATABASE_DRIVER) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseDriver(node.toString());
      }
      else if (name == DATABASE_URL) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseUrl(node.toString());
      }
      else if (name == DATABASE_LOGIN) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabaseLogin(node.toString());
      }
      else if (name == DATABASE_PATH) {
         ((OpConfiguration) parent).getDatabaseConfiguration().setDatabasePath(node.toString());
      }
      else if (name == DATABASE_PASSWORD) {
         String encryptedValue = (String) context.getVariable(ENCRYPTED_ATTRIBUTE);
         String databasePassword = node.toString();
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
         String value = node.toString();
         ((OpConfiguration) parent).getDatabaseConfiguration().setConnectionPoolMinSize(value);
      }
      else if (name == CONNECTION_POOL_MAXSIZE) {
         String value = node.toString();
         ((OpConfiguration) parent).getDatabaseConfiguration().setConnectionPoolMaxSize(value);
      }
      else if (name == CACHE_SIZE) {
         String value = node.toString();
         ((OpConfiguration) parent).getCacheConfiguration().setCacheSize(value);
      }
      else if (name == BROWSER) {
         ((OpConfiguration) parent).setBrowserApplication(node.toString());
      }
      else if (name == SMTP_SERVER) {
         ((OpConfiguration) parent).setSMTPServer(node.toString());
      }
      else if (name == LOG_FILE) {
         ((OpConfiguration) parent).setLogFile(node.toString());
      }
      else if (name == LOG_LEVEL) {
         ((OpConfiguration) parent).setLogLevel(node.toString());
      }
      else if (name == SECURE_SERVICE) {
         ((OpConfiguration) parent).setSecureService(node.toString());
      }
      else if (name == JES_DEBUGGING) {
         boolean jessDebugging = Boolean.valueOf(node.toString()).booleanValue();
         ((OpConfiguration) parent).setSourceDebugging(jessDebugging);
      }
      else if (name == RESOURCE_CACHE_SIZE) {
         String value = node.toString();
         ((OpConfiguration) parent).getCacheConfiguration().setResourceCacheSize(value);
      }
   }
}
