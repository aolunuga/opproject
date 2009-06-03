/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpConnection;
import onepoint.persistence.OpField;
import onepoint.persistence.OpMember;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpType;
import onepoint.persistence.OpTypeManager;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.util.ArrayHelper;

/**
 * This class represents an implementation of a <code>OpConnection</code> for Hibernate persistance.
 */
public class OpHibernateConnection extends OpConnection {

   /**
    * Class logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpHibernateConnection.class);

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
      SchemaExport export = new SchemaExport(source.getConfiguration());
      export.setHaltOnError(false);
//      export.setOutputFile("/usr/users/dfreis/tmp/schema.out");
      export.create(logger.isLoggable(XLog.DEBUG), true);
      // this is required to init hibernates hilo generator with 1 again
      fixHILOTo1(source);
      
//      Collection<OpSource> sources = new HashSet<OpSource>(OpSourceManager.getAllSources());
//      OpHibernateCache cache = OpHibernateCache.getInstance();
//      Iterator<String> iter = cache.getConfigurationNames();
//      while (iter.hasNext()) {
//         String facName = iter.next();
//         Configuration config = cache.getConfiguration(facName);
//         cache.getSessionFactory(facName).close();
//         SessionFactory fact = config.buildSessionFactory();
//         cache.addSessionFactory(facName, fact);
//      }
//      for (OpSource src : sources) {
//         OpHibernateSource hsource = (OpHibernateSource)src;
//         hsource.setSessionFactory(cache.getSessionFactory(hsource.getConfigurationName()));
//      }

      //for hypersonic, we need to switch of the default write delay, or things won't work.
      if (source.getDatabaseType() == OpHibernateSource.HSQLDB) {
         initHsqlDB(source);
      }
   }


   /**
    * @param source
    * @pre
    * @post
    */
   private void fixHILOTo1(OpHibernateSource source) {
      OpHibernateCache cache = OpHibernateCache.getInstance();
      Iterator<String> iter = cache.getConfigurationNames();
      while (iter.hasNext()) {
         String facName = iter.next();
//         Configuration config = cache.getConfiguration(facName);
         SessionFactoryImpl sf = (SessionFactoryImpl) cache.getSessionFactory(facName);
         IdentifierGenerator generator = sf.getIdentifierGenerator(OpObjectIfc.class.getName());
         if (generator instanceof TableHiLoGenerator) {
            TableHiLoGenerator hilo = (TableHiLoGenerator) generator;
            // simply set: lo = maxLo + 1; // so we "clock over" on the first invocation
            Field maxLoField;
            try {
               maxLoField = hilo.getClass().getDeclaredField("maxLo");
               maxLoField.setAccessible(true);
               int maxLo = (Integer) maxLoField.get(hilo);

               Field loField = hilo.getClass().getDeclaredField("lo");
               loField.setAccessible(true);
               loField.set(hilo, maxLo+1);
            }
            catch (SecurityException exc) {
               // TODO Auto-generated catch block
               exc.printStackTrace();
            }
            catch (NoSuchFieldException exc) {
               // TODO Auto-generated catch block
               exc.printStackTrace();
            }
            catch (IllegalArgumentException exc) {
               // TODO Auto-generated catch block
               exc.printStackTrace();
            }
            catch (IllegalAccessException exc) {
               // TODO Auto-generated catch block
               exc.printStackTrace();
            }

         }
      }
   }

   public void updateDBSchema() throws SQLException {
	   OpHibernateSource source = ((OpHibernateSource) getSource());
       Configuration configuration = source.getConfiguration();

	   SchemaUpdate update = new SchemaUpdate(configuration);
	   update.execute(logger.isLoggable(XLog.DEBUG), true);

	   //for hypersonic, we need to switch of the default write delay, or things won't work.
	   if (source.getDatabaseType() == OpHibernateSource.HSQLDB) {
		   initHsqlDB(source);
	   }
	   // set default values (problem on MSSQL, OPP-1079)
      OpBroker broker = OpBroker.getBroker();
      OpTransaction t = broker.newTransaction();
      Iterator<OpPrototype> prototypeIter = OpTypeManager.getPrototypes();
      while (prototypeIter.hasNext()) {
         OpPrototype prototype = prototypeIter.next();
         Iterator<OpMember> iter = prototype.getMembers();
         while (iter.hasNext()) {
            OpMember member = iter.next();
            if (member instanceof OpField) {
               OpField field = (OpField) member;
               String defaultValue = field.getDefaultValue();
               if (defaultValue != null && defaultValue.trim().length() != 0) {
                  if (field.getTypeID() == OpType.BOOLEAN) {
                     defaultValue = OpMappingsGenerator.getDefautBooleanValue(defaultValue, source.getDatabaseType());
                  }
                  if (field.getTypeID() == OpType.STRING || field.getTypeID() == OpType.TEXT) {
                     defaultValue = "'"+defaultValue+"'";
                  }
                  setNullValuesToDefault(broker, prototype.getInstanceClass().getName(), member.getName(), defaultValue);
               }
            }
         }
      }
      t.commit();
//	   source.getSession().flush();
	   // NOTE: oracle throws errors but things seem to work!!!
//	   if (update.getExceptions() != null && !update.getExceptions().isEmpty()) {
//		   SQLException exc = (SQLException) update.getExceptions().get(0);
//		   throw exc;
//	   }
   }

   private void setNullValuesToDefault(OpBroker broker, String prototypeName, String memberName, String defaultValue) {
      String queryString = "update "+prototypeName+" set "+memberName+" = "+defaultValue+" where "+memberName+" is null";
      logger.debug("setting default value: "+queryString);
      OpQuery query = broker.newQuery(queryString);
      broker.execute(query);
   }

   public void deleteOldOpObject() {
	   OpHibernateSource source = ((OpHibernateSource) getSource());
	   Configuration configuration = source.getConfiguration();
 	  String object = "op_object";
	  OpHibernateSchemaUpdater updater = new OpHibernateSchemaUpdater(source.getDatabaseType());
	  Dialect dialect = Dialect.getDialect(configuration.getProperties());
	  Connection connection = session.connection();
	  try {
		  boolean autoCommit = connection.getAutoCommit();
		  connection.setAutoCommit(false);
		  DatabaseMetaData meta = connection.getMetaData();
        List<String> script = updater.generateDropExportedKeysConstraints("op_object", "op_id", meta);
        execute(connection, script);
        meta = connection.getMetaData();
		  script = updater.generateDropExportedFKConstraints("op_object", "op_id", meta);
		  execute(connection, script);
        meta = connection.getMetaData();
		  script = updater.generateDropExportedKeysConstraints("op_object", "op_customvaluepage", meta);
        execute(connection, script);
        meta = connection.getMetaData();
		  script = updater.generateDropExportedFKConstraints("op_object", "op_customvaluepage", meta);
        execute(connection, script);
        meta = connection.getMetaData();
		  script = updater.generateDropExportedKeysConstraints("op_object", "op_object", meta);
        execute(connection, script);
        meta = connection.getMetaData();
        script = updater.generateDropExportedFKConstraints("op_object", "op_object", meta);
        execute(connection, script);
        meta = connection.getMetaData();
		  script = new ArrayList<String>(1);
		  script.add(updater.getStatement().getDropTableStatement("op_object"));
		  execute(connection, script);

		  connection.commit();
		  connection.setAutoCommit(autoCommit);
	  }
	  catch (SQLException exc) {
		  logger.error(exc.getMessage(), exc);
	  }
   }
   /**
    * @param configuration
    * @param string
    * @return
    * @pre
    * @post
    */
   private static String[] createDropConstraintsScript(
         Configuration configuration, String string) {
      ArrayList script = new ArrayList( 50 );
      Dialect dialect = Dialect.getDialect(configuration.getProperties());
      String defaultCatalog = configuration.getProperty( Environment.DEFAULT_CATALOG );
      String defaultSchema = configuration.getProperty( Environment.DEFAULT_SCHEMA );

//      configuration.getTableMappings();
//      Configuration configuration = source.getConfiguration();
      if ( dialect.dropConstraints() ) {
         Iterator iter = configuration.getTableMappings();
         while ( iter.hasNext() ) {
           Table table = (Table) iter.next();
           if ( table.isPhysicalTable() ) {
             Iterator subIter = table.getForeignKeyIterator();
             while ( subIter.hasNext() ) {
               ForeignKey fk = (ForeignKey) subIter.next();
               if ( fk.isPhysicalConstraint() ) { 
                  if (string.equals(fk.getReferencedTable().getName())) {
                     script.add(
                           fk.sqlDropString(
                                 dialect,
                                 defaultCatalog,
                                 defaultSchema
                           )
                     );
                  }
               }
             }
           }
         }
       }
      
//      Iterator iter = configuration.getTableMappings();
//      while ( iter.hasNext() ) {
//         Table table = (Table) iter.next();
//         if (string.equals(table.getName())) {
//            script.add(
//                  table.sqlDropString(
//                        dialect,
//                        defaultCatalog,
//                        defaultSchema
//                  )
//            );
//            break;
//         }
//      }
      return ArrayHelper.toStringArray( script );
   }

   /**
    * @param connection 
    * @param script
    * @pre
    * @post
    */
   public static void execute(Connection connection, List<String> script) {
      Statement statement;
      try {
         statement = connection.createStatement();
         for (String line : script) {
            try {
               statement.executeUpdate(line);
            }
            catch (SQLException exc) {
               logger.warn( "Unsuccessful: " + line, exc);
            }
         }
         connection.commit();
      }
      catch (SQLException exc1) {
         logger.warn( "Unsuccessful: " + script, exc1);
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
//   @Override
//   public void updateSchema() {
//      String[] hibernateUpdateScripts;
//      List<String> customUpdateScripts;
//      List<String> customDropScripts;
//
//      OpHibernateSource source = ((OpHibernateSource) getSource());
//      Configuration configuration = source.getConfiguration();
//      Connection connection = session.connection();
//      OpHibernateSchemaUpdater customSchemaUpdater = new OpHibernateSchemaUpdater(source.getDatabaseType());
//
//
////      connectionHelper.prepare( true );
////      connection = connectionHelper.getConnection();
////      meta = new DatabaseMetadata( connection, dialect );
////      stmt = connection.createStatement();
//
//      try {
//         //first execute any custom drop statements (only for MySQL necessary at the moment)
//         if (source.getDatabaseType() == OpHibernateSource.MYSQL_INNODB) {
//            customDropScripts = customSchemaUpdater.generateDropConstraintScripts(connection.getMetaData());
//            softExecuteDDLScript((String[]) customDropScripts.toArray(new String[]{}));
//         }
//
//         //then execute the hibernate update scripts
//         Dialect dialect = source.newHibernateDialect();
//         DatabaseMetadata meta = new DatabaseMetadata(connection, dialect);
//         hibernateUpdateScripts = configuration.generateSchemaUpdateScript(dialect, meta);
//
//         List<String> cleanHibernateUpdateScripts = new ArrayList<String>();
//         for (String hibernateUpdateScript : hibernateUpdateScripts) {
//            if (hibernateUpdateScript.contains(OpHibernateSource.HILO_GENERATOR_TABLE_NAME)) {
//               //statements -create/update- regarding HILO_GENERATOR_TABLE_NAME must be excluded since this table will always be there and up to date
//               continue;
//            }
//            cleanHibernateUpdateScripts.add(hibernateUpdateScript);
//         }
//         executeDDLScript(cleanHibernateUpdateScripts.toArray(new String[]{}));
//
//         //finally perform the custom update
//         customUpdateScripts = customSchemaUpdater.generateUpdateSchemaScripts(connection.getMetaData());
//         executeDDLScript(customUpdateScripts.toArray(new String[]{}));
//      }
//      catch (Exception e) {
//         logger.error("Cannot update DB schema because: ", e);
//      }
//   }

   /**
    * Drop database schema.
    */
   public void dropSchema() {
//	   for (int count = 0; count < 5; count++) {
		   if (doDropSchema() == null) {
			   return;
		   }
//	   }
//	   throw new HibernateException("could not drop scheama");
   }
   
   private Exception doDropSchema() {
      OpHibernateSource source = ((OpHibernateSource) getSource());
      Configuration configuration = source.getConfiguration();
      SchemaExport export = new SchemaExport(configuration);
      export.setHaltOnError(false);
      export.drop(logger.isLoggable(XLog.DEBUG), true);
      List excs = export.getExceptions();
      if (excs != null) {
    	 Iterator iter = excs.iterator();
    	 Exception first = null;
    	 while (iter.hasNext()) {
    		 Exception exc = (Exception) iter.next();
    		 if (first == null) {
    			 first = exc;
    		 }
    		 logger.warn(exc);
    	 }
    	 return first;
      }
      return null;
   }


   /**
    * Persist provided object.
    *
    * @param object object to be stored
    */
   public void persistObject(OpObjectIfc object) {
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
   public <C extends OpObjectIfc> C getObject(Class<C> c, long id) {
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
   public void updateObject(OpObjectIfc object) {
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
   public void deleteObject(OpObjectIfc object) {
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

   @Override
   public OpScrollableResults scroll(OpQuery query) throws OpDataException {
      try {
         // Note: Query.iterate() would query each object/row subsequently (n fetches)
         OpHibernateScrollableResults scroller = new OpHibernateScrollableResults(((OpHibernateQuery) query).getQuery().scroll());
         return scroller;
      }
      catch (HibernateException e) {
         throw new OpDataException(e);
      }
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
         return -1;
      }
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

   @Override
   public void clear() {
      session.clear();
   }

   @Override
   public void refreshObject(OpObjectIfc object) {
      session.refresh(object);
   }
}
