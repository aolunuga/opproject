/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

/*
 * (c) 2005 OnePoint Software GmbH (Graz/Austria)
 * All rights reserved
 */
package onepoint.project.configuration;

import onepoint.util.XBase64;

import java.util.HashMap;
import java.util.Map;


/**
 * Holds all the configuration parameters for the onepoint application.
 */
public class OpConfiguration {

   /**
    * Map of database drivers. Contains [database_type,datatabase driver] entries.
    */
   public final static Map DATABASE_DRIVERS = new HashMap();

   static {
      initDatabaseDriversMap();
   }

   /**
    * Object that holds database related configurations.
    */
   private final DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration();

   /**
    * Object that hold application cache related configurations.
    */
   private final CacheConfiguration cacheConfiguration = new CacheConfiguration();

   /**
    * A token to use for encrypted passwords.
    */
   private static final String ENCRYPTION_TOKEN = "praeeminere";

   //Misc. properties
   private String browserApplication;
   private String smtpServer;
   private String logFile;
   private String logLevel;
   private String secureService;
   /**
    * Name of the default backup directory
    */
   private String backupPath = "backup";

   /**
    * Flag that indicates whether source debugging should be used or not for script files.
    */
   private boolean sourceDebugging = false;

   /**
    * Gets the cache configuration object.
    *
    * @return a <code>CacheConfiguration</code> instance.
    */
   public CacheConfiguration getCacheConfiguration() {
      return cacheConfiguration;
   }

   /**
    * Gets the database configuraion object.
    *
    * @return a <code>DatabaseConfiguration</code> instance, that holds properties related to the configuration of the database.
    */
   public DatabaseConfiguration getDatabaseConfiguration() {
      return databaseConfiguration;
   }

   public final void setBrowserApplication(String application) {
      this.browserApplication = application;
   }

   public final String getBrowserApplication() {
      return browserApplication;
   }

   public final void setSMTPServer(String smtpServer) {
      this.smtpServer = smtpServer;
   }

   public final String getSMTPServer() {
      return this.smtpServer;
   }

   public final void setLogFile(String logFile) {
      this.logFile = logFile;
   }

   public final String getLogFile() {
      return this.logFile;
   }

   public final void setLogLevel(String logLevel) {
      this.logLevel = logLevel;
   }

   public final String getLogLevel() {
      return this.logLevel;
   }

   /**
    * Gets the value of the source debugging flag.
    *
    * @return a <code>boolean</code> indicating whether debugging should be used or not for script files.
    */
   public boolean getSourceDebugging() {
      return sourceDebugging;
   }

   /**
    * Sets the value of the source debugging flag.
    *
    * @param sourceDebugging a <code>boolean</code> indicating whether debugging should be used or not for script files.
    */
   public void setSourceDebugging(boolean sourceDebugging) {
      this.sourceDebugging = sourceDebugging;
   }

   /**
    * Gets the value of the secure service attribute that indicates whether the remote comunication should be secure or not.
    *
    * @return a <code>String</code> representing the value of the secure attribute.
    */
   public String getSecureService() {
      return secureService;
   }

   /**
    * Sets the value of the secure service attribute that indicates whether the remote comunication should be secure or not.
    *
    * @param secureService a <code>String</code> representing the value of the secure attribute.
    */
   public void setSecureService(String secureService) {
      this.secureService = secureService;
   }


   /**
    * Gets the backup path, representing either a relative or an absolute directory name.
    *
    * @return a <code>String</code> representing the backup path.
    */
   public String getBackupPath() {
      return backupPath;
   }

   /**
    * Sets the backup path.
    *
    * @param backupPath a <code>String</code> representing a backup directory.
    */
   public void setBackupPath(String backupPath) {
      this.backupPath = backupPath;
   }

   /**
    * Class that holds the application cache configuration settings.
    */
   public class CacheConfiguration {
      /**
       * The size of the cache.
       */
      private String cacheSize;

      /**
       * Resource cache size ( 0 - cache disabled)
       */
      private String resourceCacheSize;

      /**
       * Gets the size of the cache.
       *
       * @return a <code>String</code> representing a number that is the size of the cache.
       */
      public String getCacheSize() {
         return cacheSize;
      }

      /**
       * Sets the size of the cache.
       *
       * @param cacheSize a <code>String</code> representing the size of the cache.
       */
      public void setCacheSize(String cacheSize) {
         this.cacheSize = cacheSize;
      }

      /**
       * Sets the value of the resource cache size
       *
       * @param resourceCacheSize <code>String</code> representing the value of the resourceCacheSize
       */
      public void setResourceCacheSize(String resourceCacheSize) {
         this.resourceCacheSize = resourceCacheSize;
      }

      /**
       * Returns the value of the resource cache size
       *
       * @return <code>String</code> representing the size of the resource cache
       */

      public String getResourceCacheSize() {
         return resourceCacheSize;
      }
   }

   /**
    * Class that hold database configuration parameters.
    */
   public class DatabaseConfiguration {

      /**
       * Database config. parameters.
       */
      private int databaseType = -1;
      private String databaseDriver;
      private String databaseUrl;
      private String databaseLogin;
      private String databasePassword;
      private String databasePath;
      private String connectionPoolMinSize;
      private String connectionPoolMaxSize;

      /**
       * Boolean indicating whether password encryption is needed.
       */
      private boolean needsPasswordEncryption = false;

      /**
       * Sets the database driver.
       *
       * @param databaseDriver a <code>String</code> representing the database driver.
       */
      public void setDatabaseDriver(String databaseDriver) {
         this.databaseDriver = databaseDriver;
      }

      /**
       * Gets the database driver.
       *
       * @return a <code>String</code> representing the database driver.
       */
      public String getDatabaseDriver() {
         return databaseDriver;
      }

      /**
       * Sets the database url.
       *
       * @param databaseUrl a <code>String</code> representing the database url.
       */
      public void setDatabaseUrl(String databaseUrl) {
         this.databaseUrl = databaseUrl;
      }

      /**
       * Gets the database url.
       *
       * @return a <code>String</code> representing the database url.
       */
      public String getDatabaseUrl() {
         return databaseUrl;
      }

      /**
       * Sets the database login name.
       *
       * @param databaseLogin a <code>String</code> representing the database login name.
       */
      public void setDatabaseLogin(String databaseLogin) {
         this.databaseLogin = databaseLogin;
      }

      /**
       * Gets the database login name.
       *
       * @return a <code>String</code> representing the database login name.
       */
      public String getDatabaseLogin() {
         return databaseLogin;
      }

      /**
       * Sets the database password.
       *
       * @param databasePassword a <code>String</code> representing the database password.
       */
      public void setDatabasePassword(String databasePassword) {
         this.databasePassword = databasePassword;
      }

      /**
       * Gets the database password.
       *
       * @return a <code>String</code> representing the database password.
       */
      public String getDatabasePassword() {
         return databasePassword;
      }

      /**
       * Sets the type of database used.
       *
       * @param databaseType a <code>int</code> representing the type of the database.
       */
      public void setDatabaseType(int databaseType) {
         this.databaseType = databaseType;
      }

      /**
       * Gets the type of database used.
       *
       * @return a <code>int</code> representing the type of the database.
       */
      public int getDatabaseType() {
         return databaseType;
      }

      /**
       * Gets the minimum size of the connection pool.
       *
       * @return a <code>String</code> representing the minimum size of the connection pool.
       */
      public String getConnectionPoolMinSize() {
         return connectionPoolMinSize;
      }

      /**
       * Sets the minimum size of the connection pool.
       *
       * @param connectionPoolMinSize a <code>String</code> representing the minimum size of the connection pool.
       */
      public void setConnectionPoolMinSize(String connectionPoolMinSize) {
         this.connectionPoolMinSize = connectionPoolMinSize;
      }

      /**
       * Gets the maximum size of the connection pool.
       *
       * @return a <code>String</code> representing the maximum size of the connection pool.
       */
      public String getConnectionPoolMaxSize() {
         return connectionPoolMaxSize;
      }

      /**
       * Sets the maximum size of the connection pool.
       *
       * @param connectionPoolMaxSize a <code>String</code> representing the maximum size of the connection pool.
       */
      public void setConnectionPoolMaxSize(String connectionPoolMaxSize) {
         this.connectionPoolMaxSize = connectionPoolMaxSize;
      }

      /**
       * Indicates whether password encrytion is needed or not.
       *
       * @return <code>true</code> if password encryption is needed.
       */
      public boolean needsPasswordEncryption() {
         return needsPasswordEncryption;
      }

      /**
       * Sets the value of the password encryption parameter.
       *
       * @param needsPasswordEncryption a <code>boolean</code> indicating whether password encryption is needed or not.
       */
      public void setNeedsPasswordEncryption(boolean needsPasswordEncryption) {
         this.needsPasswordEncryption = needsPasswordEncryption;
      }

      public String getDatabasePath() {
         return databasePath;
      }

      public void setDatabasePath(String databasePath) {
         this.databasePath = databasePath;
      }
   }

   /**
    * Gets an encrypted db password.
    *
    * @param unencryptedPasswd a <code>String</code> representing the unencrypted password.
    * @return a <code>String</code> representing the encrypted base 64 password.
    */
   static String getEncryptedDbPassword(String unencryptedPasswd) {
      StringBuffer buffer = new StringBuffer(ENCRYPTION_TOKEN + unencryptedPasswd);
      buffer = buffer.reverse();
      return XBase64.encodeString(buffer.toString());
   }

   /**
    * Gets an unencrypted db password.
    *
    * @param encryptedPasswd a <code>String</code> representing the bae 64 encrypted password.
    * @return a <code>String</code> representing the unencrypted password.
    */
   static String getUnEncryptedDbPassword(String encryptedPasswd) {
      String decodedPasswd = XBase64.decodeToString(encryptedPasswd);
      int startIndex = decodedPasswd.length() - ENCRYPTION_TOKEN.length();
      if (startIndex < decodedPasswd.length()) {
         StringBuffer buffer = new StringBuffer(decodedPasswd);
         buffer = buffer.delete(startIndex, decodedPasswd.length());
         decodedPasswd = buffer.reverse().toString();
      }
      return decodedPasswd;
   }

   /**
    * Performs static initialization of the database drivers map
    */
   private static void initDatabaseDriversMap() {
      DATABASE_DRIVERS.put(OpConfigurationValuesHandler.MYSQL_INNO_DB_TYPE, "com.mysql.jdbc.Driver");
      DATABASE_DRIVERS.put(OpConfigurationValuesHandler.ORACLE_DB_TYPE, "oracle.jdbc.driver.OracleDriver");
      DATABASE_DRIVERS.put(OpConfigurationValuesHandler.IBM_DB2_DB_TYPE, "com.ibm.db2.jcc.DB2Driver");
      DATABASE_DRIVERS.put(OpConfigurationValuesHandler.POSTGRESQL_DB_TYPE, "org.postgresql.Driver");
      DATABASE_DRIVERS.put(OpConfigurationValuesHandler.HSQL_DB_TYPE, "org.hsqldb.jdbcDriver");
      DATABASE_DRIVERS.put(OpConfigurationValuesHandler.MSSQL_DB_TYPE, "net.sourceforge.jtds.jdbc.Driver");
   }
}
