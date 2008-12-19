/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.sql.SQLException;

import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.OpInitializer;
import onepoint.project.OpInitializerFactory;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XResourceBroker;

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
         // FIXME(dfreis Mar 27, 2008 7:07:11 PM) do not close the broker here since all sessions will
         // be closed within createSchema();
         broker.closeAndEvict();
      }
   }

//   /**
//    * Updates an existing db schema.
//    */
//   public static void updateSchema() {
//      OpBroker broker = newBroker();
//      try {
//         broker.getConnection().updateSchema();
//      }
//      finally {
//         broker.close();
//      }
//   }

   /**
    * Deletes an existing database schema.
    */
   public static void dropSchema() {
      OpBroker broker = newBroker();
      try {
         broker.getConnection().dropSchema();
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * 
    * @pre
    * @post
    */
   public static void updateDBSchema() 
   		throws SQLException {
      OpBroker broker = newBroker();
      try {
         broker.getConnection().updateDBSchema();
      }
      finally {
         broker.close();
      }
   }

   public static void main(String[] args) {
      if (args.length < 1) {
         System.out.println("usage java -cp lib OpPersistenceManager drop|create");
         System.exit(-1);
      }
      boolean drop = false;
      boolean create = false;
      if (args[0].equalsIgnoreCase("drop")) {
         drop = true;
      }
      else if (args[0].equalsIgnoreCase("create")) {
         create = true;
      }
      else {
         System.err.println("ERROR unknown command '" + args[0] + "'");
         System.exit(-1);
      }

      // init op
      OpEnvironmentManager.setOnePointHome(System.getProperty("user.dir"));
      XResourceBroker.setResourcePath("onepoint/project");
      // initialize factory
      OpInitializerFactory factory = OpInitializerFactory.getInstance();
      factory.setInitializer(OpInitializer.class);

      OpInitializer initializer = factory.getInitializer();
      initializer.setUpdateDBSchema(false);
      initializer.init(OpProjectConstants.OPEN_EDITION_CODE);
      
       if (drop) {
          OpPersistenceManager.dropSchema();
       }
       if (create) {
          OpPersistenceManager.createSchema();
       }
       System.err.println("finished");
   }
}
