/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.configuration;

/**
 * Class that hold database configuration parameters.
 */
public class OpDatabaseConfiguration {
   /**
    * Defines the default name for database configuration.
    */
   public static final String DEFAULT_DB_CONFIGURATION_NAME = "Default";

   /**
    * Defines database configuration name
    */
   private String name = DEFAULT_DB_CONFIGURATION_NAME;

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
    * Retrieve database configuration name
    *
    * @return db config name
    */
   public String getName() {
      return name;
   }

   /**
    * Set the name of the database configuration.
    *
    * @param name db config name
    */
   public void setName(String name) {
      this.name = name;
   }

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