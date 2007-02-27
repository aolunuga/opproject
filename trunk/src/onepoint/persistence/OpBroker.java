/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.util.XCalendar;

import java.sql.Blob;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;

public class OpBroker {
   private static final XLog logger = XLogFactory.getLogger(OpBroker.class, true);

   private OpConnection _default_connection; // Connection to default-source
   // Add object-caching here to broker/cursor instead of connection?!
   // ATTENTION: First implementation ONLY uses default-source
   // ==> Future implementations must add opening of sources and mapping of names/IDs to connections

   OpBroker() {
      // Constructor is only called by OpSourceManager
      OpSource default_source = OpSourceManager.getDefaultSource();
      if (default_source != null) {
         _default_connection = default_source.newConnection();
      }
   }

   public OpConnection getConnection() {
      return _default_connection;
   }

   public void makePersistent(OpObject object) {
      // Persist object into default source and set creation date and time
      object.setCreated(XCalendar.now());
      object.setModified(null);
      _default_connection.persistObject(object);
      logger.debug("OpBroker.makePersistent(): id = " + object.getID());
   }

   // Add mass-calls -- or do we not need these because of "intelligent" caching algorithms?
   // For persist, update, delete?

   public OpObject getObject(String s) {
      OpLocator locator = OpLocator.parseLocator(s);
      return getObject(locator.getPrototype().getInstanceClass(), locator.getID());
   }

   public OpObject getObject(Class c, long id) {
      // Get object by ID *** attention: Therewhile just default-connection
      logger.debug("getObject(): id = " + id);
      return _default_connection.getObject(c, id);
   }

   public void updateObject(OpObject object) {
      logger.debug("OpBroker.updateObject()");
      // Set modification date and time
      object.setModified(XCalendar.now());
      _default_connection.updateObject(object);
      logger.debug("/OpBroker.updateObject()");
   }

   public void deleteObject(OpObject object) {
      logger.debug("OpBroker.deleteObject()");
      _default_connection.deleteObject(object);
      logger.debug("/OpBroker.deleteObject()");
   }

   public List list(OpQuery query) {
      // Find object in sources specified in query (default is default source)
      if (_default_connection != null) {
         return _default_connection.list(query);
      }
      else {
         return null;
      }
   }

   public Iterator iterate(OpQuery query) {
      // Find object in sources specified in query (default is default source)
      if (_default_connection != null) {
         return _default_connection.iterate(query);
      }
      else {
         return null;
      }
   }

   public int execute(OpQuery query) {
      if (_default_connection != null) {
         return _default_connection.execute(query);
      }
      else {
         return 0;
      }
   }

   public Blob newBlob(byte[] bytes) {
      return _default_connection.newBlob(bytes);
   }

   public OpQuery newQuery(String s) {
      return _default_connection.newQuery(s);
   }


   public void close() {
      // Probably rename this function
      if (_default_connection != null) {
         _default_connection.close();
      }
   }

   /**
    * Checks if this broker (and the underlying connection) is still open.
    *
    * @return true if it's open.
    */
   public boolean isOpen() {
      return _default_connection.isOpen();
   }

   public OpTransaction newTransaction() {
      return _default_connection.newTransaction();
   }

   public Connection getJDBCConnection() {
      return _default_connection.getJDBCConnection();
   }

}
