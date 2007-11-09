/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

import java.sql.Blob;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;

public class OpBroker {
   private static final XLog logger = XLogFactory.getServerLogger(OpBroker.class);

   private OpConnection defaultConnection;
   private OpSource default_source;

   /**
    * Creates a new broker.
    *
    * @param sourceName source name to use.
    */
   OpBroker(String sourceName) {
      // Constructor is only called by OpSourceManager
      default_source = OpSourceManager.getSource(sourceName);
      if (default_source != null) {
         defaultConnection = default_source.newConnection(this);
      }
   }

   public OpConnection getConnection() {
      return defaultConnection;
   }

   public void makePersistent(OpObject object) {
      defaultConnection.persistObject(object);
      logger.debug("OpBroker.makePersistent(): id = " + object.getID());
   }

   public OpObject getObject(String s) {
      OpLocator locator = OpLocator.parseLocator(s);
      return getObject(locator.getPrototype().getInstanceClass(), locator.getID());
   }

   public <C extends OpObject> C getObject(Class<C> c, long id) {
      // Get object by ID *** attention: Therewhile just default-connection
      logger.debug("getObject(): id = " + id);
      return defaultConnection.getObject(c, id);
   }

   public void updateObject(OpObject object) {
      logger.debug("OpBroker.updateObject()");
      defaultConnection.updateObject(object);
      logger.debug("/OpBroker.updateObject()");
   }

   public void deleteObject(OpObject object) {
      logger.debug("OpBroker.deleteObject()");
      defaultConnection.deleteObject(object);
      logger.debug("/OpBroker.deleteObject()");
   }

   public List list(OpQuery query) {
      // Find object in sources specified in query (default is default source)
      if (defaultConnection != null) {
         return defaultConnection.list(query);
      }
      else {
         return null;
      }
   }

   /**
    * @param query
    * @param name
    * @return
    * @pre
    * @post
    */
   @SuppressWarnings("unchecked")
   public <C> List<C> list(OpQuery query, Class<C> type) {
      // Find object in sources specified in query (default is default source)
      if (defaultConnection != null) {
         return (List<C>)defaultConnection.list(query);
      }
      else {
         return null;
      }
   }

   
   public Iterator iterate(OpQuery query) {
      //<FIXME author="Mihai Costin" description="Use iterate when iterate related issues have been solved">
      // Find object in sources specified in query (default is default source)
      if (defaultConnection != null) {

         // This check was introduced because sometime on MS-SQL list(query) returns NULL.
         List list = list(query);
         if (list == null) {
            return defaultConnection.iterate(query);
         }
         else {
            return list.iterator();
         }
      }
      else {
         return null;
      }
      //</FIXME>
   }

   public Iterator forceIterate(OpQuery query) {
      // Find object in sources specified in query (default is default source)
      if (defaultConnection != null) {
         return defaultConnection.iterate(query);
      }
      else {
         return null;
      }
   }


   public int execute(OpQuery query) {
      if (defaultConnection != null) {
         return defaultConnection.execute(query);
      }
      else {
         return 0;
      }
   }

   public Blob newBlob(byte[] bytes) {
      return defaultConnection.newBlob(bytes);
   }

   public OpQuery newQuery(String s) {
      return defaultConnection.newQuery(s);
   }

   public void close() {
      // Probably rename this function
      if (defaultConnection != null) {
         defaultConnection.close();
         defaultConnection = null;
      }
   }

   /**
    * Closes this broker and evicts the cache.
    */
   public void closeAndEvict() {
      clear();
      close();
   }

   /**
    * Evicts the cache for this broker.
    */
   public void clear() {
      if (defaultConnection != null) {
         default_source.clear();
      }
   }

   /**
    * Checks if this broker (and the underlying connection) is still open.
    *
    * @return true if it's open.
    */
   public boolean isOpen() {
      return defaultConnection != null && defaultConnection.isOpen();
   }

   public boolean isValid() {
      if (defaultConnection == null) {
         return (false);
      }
      return (defaultConnection.isValid());
   }

   public OpTransaction newTransaction() {
      return defaultConnection.newTransaction();
   }

   public Connection getJDBCConnection() {
      return defaultConnection.getJDBCConnection();
   }

   /**
    * @return
    * @pre
    * @post
    */
   public OpSource getSource() {
      return default_source;
   }

}
