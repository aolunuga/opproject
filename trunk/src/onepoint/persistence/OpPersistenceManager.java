/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.persistence.hibernate.OpHibernateSource;

public final class OpPersistenceManager {

   /**
    * Creates a new broker instance.
    *
    * @param sourceName source name to use.
    * @return a <code>OpBroker</code> object.
    */
   public static OpBroker newBroker(String sourceName) {
      return new OpBroker(sourceName);
   }

   /**
    * Returns a broker using the first datasource found. This should be used only for operations that not reguire
    * operations dependent on data source.
    *
    * @return a <code>OpBroker</code> object.
    */
   private static OpBroker newBroker() {
      //TODO - calin.pavel - this line should be changed when multiple databases will be supported.
      OpHibernateSource hibernateSource = (OpHibernateSource) OpSourceManager.getAllSources().iterator().next();

      return newBroker(hibernateSource.getName());
   }

   /**
    * Creates an empty db schema structure.
    */
   public static void createSchema() {
      OpBroker broker = newBroker();
      try {
         broker.getConnection().createSchema();
      } 
      finally {  
         broker.close();
      }
   }

   /**
    * Updates an existing db schema.
    */
   public static void updateSchema() {
      OpBroker broker = newBroker();
      try {
         broker.getConnection().updateSchema();
      }
      finally {
         broker.close();
      }
   }

   /**
    * Deletes an existing database schema.
    */
   public static void dropSchema() {
      OpBroker broker = newBroker();
      try {
         broker.getConnection().dropSchema();
      }
      finally {
         broker.close();
      }
   }

   /**
    * 
    * @pre
    * @post
    */
   public static void updateDBSchema() {
      OpBroker broker = newBroker();
      try {
         broker.getConnection().updateDBSchema();
      }
      finally {
         broker.close();
      }
   }
}
