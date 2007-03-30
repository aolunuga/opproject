/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
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

public class OpHibernateConnection extends OpConnection {

   private static final XLog logger = XLogFactory.getLogger(OpHibernateConnection.class, true);

   private Session session;

   public OpHibernateConnection(OpSource source, Session session) {
      super(source);
      this.session = session;
   }

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
         logger.error("Could not execute DDL script: " + e);
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
               logger.warn("Skipping drop statement because: " + e.getMessage());
               t.rollback();
               this.closeStatement(statement);
            }
            catch (Exception e) {
               logger.error("Could not execute drop statement: " + e.getMessage(), e);
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
         logger.error("OpHibernateConnection.persistObject(): Could not generate schema creation script: " + e);
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
    * @param source a <code>OpHibernateSource</code> object containing db parameters.
    */
   private void initHsqlDB(OpHibernateSource source) {
      try {
         logger.info("Starting HSQLDB initialization...");

         String url = source.getURL();
         String dbPath = url.replaceAll(OpHibernateSource.HSQLDB_JDBC_CONNECTION_PREFIX, "");

         Class hsqldbDBManagerClass = Class.forName("org.hsqldb.DatabaseManager");
         Class hsqlPropertiesClass = Class.forName("org.hsqldb.persist.HsqlProperties");
         Method newSessionMethod = hsqldbDBManagerClass.getMethod("newSession", new Class[] {String.class, String.class,
            String.class, String.class, hsqlPropertiesClass});

         Class hsqlSessionClass = Class.forName("org.hsqldb.Session");
         Method hsqlExecuteMethod = hsqlSessionClass.getMethod("sqlExecuteDirectNoPreChecks", new Class[] {String.class});
         Method commitMethod = hsqlSessionClass.getMethod("commit", null);
         Method closeMethod = hsqlSessionClass.getMethod("close", null);

         Object hsqlDbSession = newSessionMethod.invoke(null, new Object[] {OpHibernateSource.HSQLDB_TYPE, dbPath, source.getLogin(), source.getPassword(), null});
         hsqlExecuteMethod.invoke(hsqlDbSession, new Object[]{"SET WRITE_DELAY FALSE"});
         commitMethod.invoke(hsqlDbSession, null);
         closeMethod.invoke(hsqlDbSession, null);

         logger.info("HSQLDB initialization finished");
      }
      catch (Exception e) {
         logger.error("Cannot initialize HSQLDB connection because: " + e.getMessage(), e);
      }
   }

   public void updateSchema() {
      String[] hibernateUpdateScripts = null;
      List customUpdateScripts = new ArrayList();
      List customDropScripts = new ArrayList();

      OpHibernateSource source = ((OpHibernateSource) getSource());
      Configuration configuration = source.getConfiguration();
      Connection connection = session.connection();
      OpHibernateSchemaUpdater customSchemaUpdater = new OpHibernateSchemaUpdater(source);

      try {
         //first execute any custom drop statements (only for MySQL necessary at the moment)
         if (source.getDatabaseType() == OpHibernateSource.MYSQL || source.getDatabaseType() == OpHibernateSource.MYSQL_INNODB) {
            customDropScripts = customSchemaUpdater.generateDropConstraintScripts(connection.getMetaData());
            softExecuteDDLScript((String[]) customDropScripts.toArray(new String[]{}));
         }

         //then execute the hibernate update scripts
         Dialect dialect = source.newHibernateDialect();
         DatabaseMetadata meta = new DatabaseMetadata(connection, dialect);
         hibernateUpdateScripts = configuration.generateSchemaUpdateScript(dialect, meta);
         executeDDLScript(hibernateUpdateScripts);

         //finally perform the custom update
         customUpdateScripts = customSchemaUpdater.generateUpdateSchemaScripts(connection.getMetaData());
         executeDDLScript((String[]) customUpdateScripts.toArray(new String[]{}));
      }
      catch (Exception e) {
         logger.error("Cannot update DB schema because: " + e.getMessage(), e);
      }
   }

   public void dropSchema() {
      // Create drop schema script
      OpHibernateSource source = ((OpHibernateSource) getSource());
      OpHibernateSchemaUpdater customUpdater = new OpHibernateSchemaUpdater(source);
      Configuration configuration = source.getConfiguration();
      try {
         String[] hibernateDropScripts = configuration.generateDropSchemaScript(source.newHibernateDialect());
         softExecuteDDLScript(hibernateDropScripts);

         String[] customDropScripts = (String[]) customUpdater.generateDropPredefinedTablesScripts().toArray(new String[]{});
         softExecuteDDLScript(customDropScripts);
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not generate drop schema script: " + e);
      }
   }

   // *** alterSchema could use generateUpdateSchemaScript()

   public void persistObject(OpObject object) {
      try {
         logger.debug("before session.save()");
         session.save(object);
         logger.debug("after session.save()");
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not save object: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

//   public boolean contains(Object obj) {
//     return(_session.contains(obj));
//   }

   public OpObject getObject(Class c, long id) {
      OpObject object = null;
      try {
         object = (OpObject) (session.get(c, new Long(id)));
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not load object: " + e);
         // *** TODO: Throw OpPersistenceException
      }
      return object;
   }

   public void updateObject(OpObject object) {
      try {
         session.update(object);
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not update object: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public void deleteObject(OpObject object) {
      try {
         session.delete(object);
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.persistObject(): Could not delete object: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public List list(OpQuery query) {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         return ((OpHibernateQuery) query).getQuery().list();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.find(): Could not execute query: " + e);
         e.printStackTrace(System.err);
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
         logger.error("OpHibernateConnection.find(): Could not execute query: " + e);
         e.printStackTrace(System.err);
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
         logger.error("OpHibernateConnection.find(): Could not execute query: " + e);
         e.printStackTrace(System.err);
         // *** TODO: Throw OpPersistenceException
      }
      return 0;
   }

   public void close() {
      try {
         session.close();
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.close(): Could not close session: " + e);
         // *** TODO: Throw OpPersistenceException
      }
   }

   public OpTransaction newTransaction() {
      OpTransaction transaction = null;
      try {
         transaction = new OpHibernateTransaction(session.beginTransaction());
      }
      catch (HibernateException e) {
         logger.error("OpHibernateConnection.newTransaction(): Could not begin transaction: " + e);
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

  /* (non-Javadoc)
   * @see onepoint.persistence.OpConnection#flush()
   */
  @Override
  public void flush() {
    session.flush();
  }
}
