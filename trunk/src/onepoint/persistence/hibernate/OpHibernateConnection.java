/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;

import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * This class represents an implementation of a <code>OpConnection</code> for Hibernate persistance.
 */
public class OpHibernateConnection extends OpConnection {

   /**
    * Class logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpHibernateConnection.class);

   /**
    * Hibernate session used for data persistance.
    */
   private Session session;

   private static WeakHashMap<Session, OpBroker> sessionToBrokerMap = new WeakHashMap<Session, OpBroker>();

   /**
    * Creates a new instance.
    *
    * @param broker  the broker that creates this connection
    * @param session hibernate session to use
    */
   public OpHibernateConnection(OpBroker broker, Session session) {
      super(broker.getSource());
      this.session = session;
      sessionToBrokerMap.put(session, broker);
      logger.debug("Session to broker map contains now " + sessionToBrokerMap.size() + " entries!");
   }

   protected static OpBroker getBroker(Session session) {
      return sessionToBrokerMap.get(session);
   }

   /**
    * Specify if the connection is still valid.
    *
    * @return <code>true</code> is still valid, and <code>false</code> otherwise.
    */
   public boolean isValid() {
      if (session == null) {
         return false;
      }

      try {
         session.connection().getMetaData();
      }
      catch (Exception e) {
         return false;
      }

      return true;
   }

   /**
    * Checks if the session is still open.
    *
    * @return true if it's open.
    */
   public boolean isOpen() {
      return session.isOpen();
   }
   
   /**
    * Craete a Criteria 
    * @param criteriaClass the cliteria class
    * @param alias the mapping alias
    * @return an hibernate creteria
    */
   public Criteria createCriteria(Class criteriaClass, String alias) {
      return session.createCriteria(criteriaClass, alias);
   }

   /**
    * Executes a DDL script directly via JDBC, commiting everything in 1 large transaction.
    *
    * @param script a <code>String</code> array representing a set of scripts.
    */
   private void executeDDLScript(String[] script) {
      if (script == null || script.length == 0) {
         return;
      }
      Statement statement = null;
      OpTransaction t = newTransaction();
      try {
         Connection connection = session.connection();
         for (int i = 0; i < script.length; i++) {
            statement = connection.createStatement();
            logger.info("Executing SQL: " + script[i]);
            statement.executeUpdate(script[i]);
         }
         t.commit();
      }
      catch (Exception e) {
         t.rollback();
         logger.error("Could not execute DDL script: ", e);
      }
      finally {
         closeStatement(statement);
      }
   }

   /**
    * Executes a DDL script directly via JDBC, commiting each statement in a separate transaction.
    *
    * @param script a <code>String</code> array representing a set of scripts.
    */
   private void softExecuteDDLScript(String[] script) {
      if (script == null || script.length == 0) {
         return;
      }
      // Try each drop extra and issues only warnings if tables cannot be dropped
      Statement statement = null;
      try {
         Connection connection = session.connection();
         OpTransaction t = null;
         for (int i = 0; i < script.length; i++) {
            try {
               t = newTransaction();
               statement = connection.createStatement();
               logger.info("Executing SQL:" + script[i]);
               statement.executeUpdate(script[i]);
               statement.close();
               t.commit();
            }
            catch (SQLException e) {
               logger.warn("Skipping drop statement because: ", e);
               t.rollback();
               this.closeStatement(statement);
            }
            catch (Exception e) {
               logger.error("Could not execute drop statement: ", e);
               t.rollback();
               break;
            }
         }
      }
      finally {
         closeStatement(statement);
      }
   }

   /**
    * Tries to close a possibly open database statement.
    *
    * @param statement a <code>Statement</code> representing a database statement.
    */
   private void closeStatement(Statement statement) {
      if (statement != null) {
         try {
            statement.close();
         }
         catch (SQLException e) {
            logger.error("Cannot close statement:", e);
         }
      }
   }

   public void createSchema() {
      // Create schema creation script
      OpHibernateSource source = ((OpHibernateSource) getSource());
      Configuration configuration = source.getConfiguration();
      String[] script = null;
      try {
         script = configuration.generateSchemaCreationScript(source.newHibernateDialect());
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not generate schema creation script: ", e);
         // *** TODO: Throw OpPersistenceException
      }
      // Execute schema creation script
      executeDDLScript(script);

      //for hypersonic, we need to switch of the default write delay, or things won't work.
      if (source.getDatabaseType() == OpHibernateSource.HSQLDB) {
         initHsqlDB(source);
      }
   }

   /**
    * Performs HSQLDB custom initialization.
    *
    * @param source a <code>OpHibernateSource</code> object containing db parameters.
    */
   private void initHsqlDB(OpHibernateSource source) {
      try {
         logger.info("Starting HSQLDB initialization...");

         String url = source.getURL();
         String dbPath = url.replaceAll(OpHibernateSource.HSQLDB_JDBC_CONNECTION_PREFIX, "");

         Class hsqldbDBManagerClass = Class.forName("org.hsqldb.DatabaseManager");
         Class hsqlPropertiesClass = Class.forName("org.hsqldb.persist.HsqlProperties");
         Method newSessionMethod = hsqldbDBManagerClass.getMethod("newSession", String.class, String.class,
              String.class, String.class, hsqlPropertiesClass);

         Class hsqlSessionClass = Class.forName("org.hsqldb.Session");
         Method hsqlExecuteMethod = hsqlSessionClass.getMethod("sqlExecuteDirectNoPreChecks", String.class);
         Method commitMethod = hsqlSessionClass.getMethod("commit");
         Method closeMethod = hsqlSessionClass.getMethod("close");

         Object hsqlDbSession = newSessionMethod.invoke(null, OpHibernateSource.HSQLDB_TYPE, dbPath, source.getLogin(),
              source.getPassword(), null);
         hsqlExecuteMethod.invoke(hsqlDbSession, "SET WRITE_DELAY FALSE");
         commitMethod.invoke(hsqlDbSession);
         closeMethod.invoke(hsqlDbSession);

         logger.info("HSQLDB initialization finished");
      }
      catch (Exception e) {
         logger.error("Cannot initialize HSQLDB connection because: ", e);
      }
   }

   protected static String getCleanDBURL(OpHibernateSource source) {
      String url = source.getURL();
      String dbPath = url.replaceAll(OpHibernateSource.HSQLDB_JDBC_CONNECTION_PREFIX, "");
      //we have to cut off the URL-Parameters
      int paramPos = dbPath.indexOf(';');
      if (paramPos != -1) {
         dbPath = dbPath.substring(0, paramPos);
      }
      return dbPath;
   }

   /**
    * <FIXME author="Horia Chiorean" description="According to JLS, this code does assure that we won't get NoClassDefFound if hsqld.jar isn't present in classpath">
    */
   protected static org.hsqldb.persist.HsqlProperties cleanupHSQLDBDefaultTableType(OpHibernateSource source) {
      try {
         String dbPath = getCleanDBURL(source);
         org.hsqldb.persist.HsqlProperties prop = new org.hsqldb.persist.HsqlProperties(dbPath);
         prop.load();
         prop.setProperty("hsqldb.default_table_type", "cached");
         prop.save();
         return prop;
      }
      catch (Exception e) {
         logger.error("Cannot initialize HSQLDB connection because: ", e);
      }
      return new org.hsqldb.persist.HsqlProperties();
   }

   /**
    * Update database schema.
    */
   @Override
   public void updateSchema() {
      String[] hibernateUpdateScripts;
      List<String> customUpdateScripts;
      List<String> customDropScripts;

      OpHibernateSource source = ((OpHibernateSource) getSource());
      Configuration configuration = source.getConfiguration();
      Connection connection = session.connection();
      OpHibernateSchemaUpdater customSchemaUpdater = new OpHibernateSchemaUpdater(source.getDatabaseType());

      try {
         //first execute any custom drop statements (only for MySQL necessary at the moment)
         if (source.getDatabaseType() == OpHibernateSource.MYSQL_INNODB) {
            customDropScripts = customSchemaUpdater.generateDropConstraintScripts(connection.getMetaData());
            softExecuteDDLScript((String[]) customDropScripts.toArray(new String[]{}));
         }

         //then execute the hibernate update scripts
         Dialect dialect = source.newHibernateDialect();
         DatabaseMetadata meta = new DatabaseMetadata(connection, dialect);
         hibernateUpdateScripts = configuration.generateSchemaUpdateScript(dialect, meta);

         List<String> cleanHibernateUpdateScripts = new ArrayList<String>();
         for (String hibernateUpdateScript : hibernateUpdateScripts) {
            if (hibernateUpdateScript.contains(OpHibernateSource.HILO_GENERATOR_TABLE_NAME)) {
               //statements -create/update- regarding HILO_GENERATOR_TABLE_NAME must be excluded since this table will always be there and up to date
               continue;
            }
            cleanHibernateUpdateScripts.add(hibernateUpdateScript);
         }

         executeDDLScript(cleanHibernateUpdateScripts.toArray(new String[]{}));

         //finally perform the custom update
         customUpdateScripts = customSchemaUpdater.generateUpdateSchemaScripts(connection.getMetaData());
         executeDDLScript(customUpdateScripts.toArray(new String[]{}));
      }
      catch (Exception e) {
         logger.error("Cannot update DB schema because: ", e);
      }
   }

   /**
    * Drop database schema.
    */
   public void dropSchema() {
      // Create drop schema script
      OpHibernateSource source = ((OpHibernateSource) getSource());
      OpHibernateSchemaUpdater customUpdater = new OpHibernateSchemaUpdater(source.getDatabaseType());
      Configuration configuration = source.getConfiguration();
      try {
         String[] hibernateDropScripts = configuration.generateDropSchemaScript(source.newHibernateDialect());
         softExecuteDDLScript(hibernateDropScripts);

         String[] customDropScripts = (String[]) customUpdater.generateDropPredefinedTablesScripts().toArray(new String[]{});
         softExecuteDDLScript(customDropScripts);
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not generate drop schema script: ", e);
      }
   }


   /**
    * Persist provided object.
    *
    * @param object object to be stored
    */
   public void persistObject(OpObject object) {
      try {
         logger.debug("before session.save()");
         session.save(object);
         logger.debug("after session.save()");
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not save object: ", e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   /**
    * Retrieve an object by its identifier.
    *
    * @param c  object class.
    * @param id object identifier
    * @return object with the provided identifier
    */
   public <C extends OpObject> C getObject(Class<C> c, long id) {
      C object = null;
      try {
         object = (C) (session.get(c, new Long(id)));
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not load object: ", e);
         // *** TODO: Throw OpPersistenceException
      }
      return object;
   }

   /**
    * Store/update object information into database.
    *
    * @param object object to be stored.
    */
   public void updateObject(OpObject object) {
      try {
         session.update(object);
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not update object: ", e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   /**
    * Delete provided object.
    *
    * @param object object to be deleted.
    */
   public void deleteObject(OpObject object) {
      try {
         session.delete(object);
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not delete object: ", e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public List list(OpQuery query) {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         return ((OpHibernateQuery) query).getQuery().list();
      }
      catch (HibernateException e) {
         logger.error("Could not execute query:" + ((OpHibernateQuery) query).getQuery().getQueryString());
         logger.error("Exception: ", e);
         // *** TODO: Throw OpPersistenceException
      }
      return null;
   }

   public Iterator iterate(OpQuery query) {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         return ((OpHibernateQuery) query).getQuery().iterate();
      }
      catch (HibernateException e) {
         logger.error("Could not execute query: " + ((OpHibernateQuery) query).getQuery().getQueryString());
         logger.error("Exception: ", e);
         // *** TODO: Throw OpPersistenceException
      }
      return null;
   }

   public int execute(OpQuery query) {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         return ((OpHibernateQuery) query).getQuery().executeUpdate();
      }
      catch (HibernateException e) {
         logger.error("Could not execute query: " + ((OpHibernateQuery) query).getQuery().getQueryString());
         logger.error("Exception: ", e);
         // *** TODO: Throw OpPersistenceException
      }
      return 0;
   }

   public void close() {
      try {
         sessionToBrokerMap.remove(session);
         if (session != null) {
            session.close();
         }
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.close(): Could not close session: ", e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public OpTransaction newTransaction() {
      OpTransaction transaction = null;
      try {
         transaction = new OpHibernateTransaction(session.beginTransaction());
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.newTransaction(): Could not begin transaction: ", e);
         // *** TODO: Throw OpPersistenceException
      }
      return transaction;
   }

   public Blob newBlob(byte[] bytes) {
      return Hibernate.createBlob(bytes);
   }

   public final OpQuery newQuery(String s) {
      return new OpHibernateQuery(session.createQuery(s));
   }

   public final Connection getJDBCConnection() {
      return session.connection();
   }

   /**
    * @see onepoint.persistence.OpConnection#flush()
    */
   @Override
   public void flush() {
      session.flush();
   }

   /**
    * @see onepoint.persistence.OpConnection#setFlushMode(int) ()
    */
   @Override
   public void setFlushMode(int flushMode) {
      switch (flushMode) {
         case FLUSH_MODE_MANUAL: {
            session.setFlushMode(FlushMode.MANUAL);
            break;
         }
         case FLUSH_MODE_COMMIT: {
            session.setFlushMode(FlushMode.COMMIT);
            break;
         }
         case FLUSH_MODE_AUTO: {
            session.setFlushMode(FlushMode.AUTO);
            break;
         }
         case FLUSH_MODE_ALWAYS: {
            session.setFlushMode(FlushMode.ALWAYS);
            break;
         }
         default: {
            throw new IllegalArgumentException("unsupported flush mode: " + flushMode);
         }
      }
   }

   /**
    * @see onepoint.persistence.OpConnection#getFlushMode()
    */
   @Override
   public int getFlushMode() {
      FlushMode mode = session.getFlushMode();
      if (mode.equals(FlushMode.MANUAL)) {
         return FLUSH_MODE_MANUAL;
      }
      if (mode.equals(FlushMode.COMMIT)) {
         return FLUSH_MODE_COMMIT;
      }
      if (mode.equals(FlushMode.AUTO)) {
         return FLUSH_MODE_AUTO;
      }
      if (mode.equals(FlushMode.ALWAYS)) {
         return FLUSH_MODE_ALWAYS;
      }
      return (-1);
   }
}
