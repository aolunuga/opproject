/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

/**
 * This represents the superclass of any data source.
 */
public abstract class OpSource {
   // source name.
   private String name;

   /**
    * Inidicates whether this source should be embeded or not.
    */
   private boolean embeded = false;

   /**
    * Defines the name of the source
    *
    * @param name source name.
    */
   public final void setName(String name) {
      this.name = name;
   }

   /**
    * Retrieve source name
    *
    * @return source name.
    */
   public final String getName() {
      return name;
   }

   /**
    * Open source.
    */
   public abstract void open();

   /**
    * Close source.
    */
   public abstract void close();

   /**
    * Clear source.
    */
   public abstract void clear();

   /**
    * Creates a new connection.
    *
    * @return new connection
    */
   public abstract OpConnection newConnection();

   /**
    * Checks whether a table exists or not in the db schema.
    *
    * @param tableName name of the table to be checked.
    */
   public abstract boolean existsTable(String tableName);

   /**
    * On-register callback
    */
   public void onRegister() {
   }

   /**
    * Gets the value of the embeded flag.
    *
    * @return a <code>boolean</code> indicating whether the source is embeded or not.
    */
   public boolean isEmbeded() {
      return this.embeded;
   }

   /**
    * Sets the value of the embeded flag.
    *
    * @param embeded a <code>boolean</code> indicating whether the source is embeded or not.
    */
   public void setEmbeded(boolean embeded) {
      this.embeded = embeded;
   }
}
