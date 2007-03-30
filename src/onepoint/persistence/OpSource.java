/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

public abstract class OpSource {

   private String _name;
   private int _id;

   /**
    * Inidicates whether this source should be embeded or not.
    */
   private boolean embeded = false;

   public final void setName(String name) {
      _name = name;
   }

   public final String getName() {
      return _name;
   }

   final void setID(int id) {
      // Exclusively called by source-manager
      _id = id;
   }

   public final int getID() {
      return _id;
   }

   public abstract void open();

   public abstract void close();

   public abstract void clear();
   
   public abstract OpConnection newConnection();

   /**
    * Checks whether a table exists or not in the db schema.
    */
   public abstract boolean existsTable(String tableName);

   // On-register callback

   public void onRegister() {
   }

   /**
    * Gets the value of the embeded flag.
    * @return a <code>boolean</code> indicating whether the source is embeded or not.
    */
   public boolean isEmbeded() {
      return this.embeded;
   }

   /**
    * Sets the value of the embeded flag.
    * @param embeded a <code>boolean</code> indicating whether the source is embeded or not.
    */
   public void setEmbeded(boolean embeded) {
      this.embeded = embeded;
   }
}
