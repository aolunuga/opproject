/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

/**
 * This represents the superclass of any data source.
 * <FIXME author="Horia Chiorean" description="This class shouldn't exist. We are only using hibernate...and this leads to confusion and *ugly* code">
 */
public abstract class OpSource {

   /**
    * Defines the name used to register default source
    */
   public static final String DEFAULT_SOURCE_NAME = "admin";

   /**
    * Source name.
    */
   private String name;

   /**
    * Inidicates whether this source should be embeded or not.
    */
   private boolean embeded = false;


   /**
    * Creates a new source
    *
    * @param name source name
    */
   protected OpSource(String name) {
      setName(name);
   }

   /**
    * Defines the name of the source
    *
    * @param name source name.
    */
   private final void setName(String name) {
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
    * @param broker the broker that created this connection
    *
    * @return new connection
    */
   public abstract OpConnection newConnection(OpBroker broker);

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
   
   public <O extends OpObject> void addEntityEventListener(Class<O> opclass, OpEntityEventListener listener) {
   }
   
   public <O extends OpObject> void removeEntityEventListener(Class<O> opclass, OpEntityEventListener listener) {
   }
}
