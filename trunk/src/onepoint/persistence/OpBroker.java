/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.sql.Blob;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;

public class OpBroker {
   private static final XLog logger = XLogFactory.getLogger(OpBroker.class);

   private OpConnection defaultConnection;
   private OpSource default_source;
   
   // used for debugging only !!
   private static Stack<String> brokerStack = null;

   /**
    * Thread local OpBroker.
    */
   static ThreadLocal brokers = new ThreadLocal();
   
   /**
    * Creates a new broker.
    *
    * @param sourceName source name to use.
    */
   OpBroker(String sourceName) {
      // Constructor is only called by OpSourceManager
      setBroker(this);
      default_source = OpSourceManager.getSource(sourceName);
      if (default_source != null) {
         defaultConnection = default_source.newConnection(this);
      }
      if (logger.isLoggable(XLog.DEBUG)) {
         if (brokerStack == null) {
            brokerStack = new Stack<String>();
         }
         brokerStack.add(new Throwable().getStackTrace()[3].toString());
         if (brokerStack.size() > 1) {
            logger.debug("duplicate brokers at: ");
            for (String st : brokerStack) {
               logger.debug(st);
            }
         }
      }
   }

   public OpConnection getConnection() {
      return defaultConnection;
   }

   public void makePersistent(OpObjectIfc opObject) {
      defaultConnection.persistObject(opObject);
      logger.debug("OpBroker.makePersistent(): id = " + opObject.getId());
   }

   public OpObjectIfc getObject(String s) {
      OpLocator locator = OpLocator.parseLocator(s);
      return locator != null ? getObject(locator) : null;
   }

   /**
    * @param locator
    * @return
    * @pre
    * @post
    */
   public OpObjectIfc getObject(OpLocator locator) {
      return getObject(locator.getPrototype().getInstanceClass(), locator.getID());
   }

   public <C extends OpObjectIfc> C getObject(Class<C> c, long id) {
      // Get object by ID *** attention: Therewhile just default-connection
      logger.debug("getObject(): id = " + id);
      return defaultConnection.getObject(c, id);
   }

   public void updateObject(OpObjectIfc object) {
      logger.debug("OpBroker.updateObject()");
      defaultConnection.updateObject(object);
      logger.debug("/OpBroker.updateObject()");
   }

   public void refreshObject(OpObjectIfc object) {
      logger.debug("OpBroker.refreshObject()");
      defaultConnection.refreshObject(object);
      logger.debug("/OpBroker.refreshObject()");
   }
   public void deleteObject(OpObjectIfc object) {
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
      return forceIterate(query);
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
      removeBroker();
      if (logger.isLoggable(XLog.DEBUG) && (brokerStack != null)) {
         if (brokerStack.size() < 1) {
            logger.debug("closing non opened broker: "+new Throwable().getStackTrace()[3].toString());
         }
         else {
            brokerStack.pop();
         }
      }
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
         defaultConnection.clear();
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

   /**
    * Sets the whole system to read only or read/write mode according the given parameter
    * @param readOnly if true sets read only mode else sets read write mode.
    */
   public void setReadOnlyMode(boolean readOnly) {
      default_source.setReadOnlyMode(readOnly);
   }
   
   /**
    * Gets the read only mode.
    * @return true if the system is within read only mode, false otherwise.
    */
   public boolean isReadOnlyMode() {
      return default_source.isReadOnlyMode();
   }


   /**
    * Used for testing if a given object is of a given type.
    * Throws an <code>UnsupportedOperationException</code> if the broker on witch we call the method is closed. 
    *
    * @param id represents the object id
    * @param objectType represents the type to witch we want to compare the object type
    * @return returns <code>true</code> if the object with the specified id is of type <code>objectType</code> otherwise
    * it returns <code>false</code>
    */
   public boolean isOfType(Long id, String objectType) {
      if (!this.isValid()) {
         throw new UnsupportedOperationException();
      }
      StringBuffer buffer = new StringBuffer("select obj.id from ");
      buffer.append(objectType);
      buffer.append(" obj where obj.id = :objID");
      OpQuery query = this.newQuery(buffer.toString());
      query.setLong("objID", id);

      Iterator it = this.iterate(query);

      if (it != null) {
         return it.hasNext();
      }

      return false;
   }

   /**
    * Gets the OpBroker held as thread local.
    *
    * @return the thread depending OpProjectSession.
    */
   public static OpBroker getBroker() {
      Object existing = brokers.get();
      if (existing == null) {
         return null;
      }
      if (existing instanceof OpBroker) {
         return (OpBroker) existing;
      }
      return ((Stack<OpBroker>) existing).peek();
   }

   /**
    * Sets the given broker as thread local.
    *
    * @param session the session to set
    */
   public static void setBroker(OpBroker broker) {
      Object existing = brokers.get();
      if (existing == null) {
         brokers.set(broker);
         return;
      }
      if (existing instanceof OpBroker) {
         if (broker == null) {
            brokers.set(null);
            return;
         }
         Stack<OpBroker> stack = new Stack<OpBroker>();
         stack.push((OpBroker) existing);
         existing = stack;
         brokers.set(stack);
      }
      Stack<OpBroker> stack = (Stack<OpBroker>) existing;
      if (broker == null) {
         stack.pop();
         if (stack.size() == 1) { // only one element
            brokers.set(stack.pop());
         }
         return;
      }
      stack.push(broker);
   }

   /**
    * Removes the thread local broker
    */
   public static void removeBroker() {
      setBroker(null);
   }

}
