/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

public final class OpPersistenceManager {

   /**
    * Creates a new broker instance.
    *
    * @return a <code>OpBroker</code> object.
    */
   public static OpBroker newBroker() {
      return new OpBroker();
   }

   /**
    * Creates an empty db schema structure.
    */
   public static void createSchema() {
      OpBroker broker = newBroker();
      broker.getConnection().createSchema();
      broker.close();
   }

   /**
    * Updates an existing db schema.
    */
   public static void updateSchema() {
      OpBroker broker = newBroker();
      broker.getConnection().updateSchema();
      broker.close();
   }

   /**
    * Deletes an existing database schema.
    */
   public static void dropSchema() {
      OpBroker broker = newBroker();
      broker.getConnection().dropSchema();
      broker.close();
   }
}
