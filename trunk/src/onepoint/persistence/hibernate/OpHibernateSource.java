/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.event.*;
import org.hibernate.event.def.DefaultFlushEntityEventListener;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * This is an implementation of a data source based on Hibernate.
 */
public class OpHibernateSource extends OpSource
     implements PostUpdateEventListener, PostDeleteEventListener, PostInsertEventListener {
   /**
    * The logger used in this class.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpHibernateSource.class);

   /**
    * Constant that defines
    */

   // Hibernate source that is always auto-mapped
   public final static int DERBY = 1; // Default database type
   public final static int MYSQL_INNODB = 2;
   public final static int POSTGRESQL = 3;
   public final static int ORACLE = 4;
   public final static int HSQLDB = 5;
   public final static int IBM_DB2 = 6;
   public final static int MSSQL = 7;

   public final static String HSQLDB_TYPE = "file:";
   public final static String HSQLDB_JDBC_CONNECTION_PREFIX = "jdbc:hsqldb:" + HSQLDB_TYPE;
   public final static String DERBY_JDBC_CONNECTION_PREFIX = "jdbc:derby:";
   public final static String DERBY_JDBC_CONNECTION_SUFIX = ";create=true";
   public final static String SCHEMA_TABLE = "op_schema";

   final static String HILO_GENERATOR_TABLE_NAME = " hibernate_unique_key";
   final static String HILO_GENERATOR_COLUMN_NAME = "next_hi";

   private HashMap<Class, Object> listeners;

   /**
    * The latest schema version
    */
   public static final int SCHEMA_VERSION = 50;

   /**
    * Db schema related constants
    */
   private static final String VERSION_COLUMN = "op_version";
   private static final String VERSION_PLACEHOLDER = "#";
   private static final String CREATE_SCHEMA_TABLE_STATEMENT = "create table " + SCHEMA_TABLE + "(" + VERSION_COLUMN + " int)";
   private static final String INSERT_CURENT_VERSION_INTO_SCHEMA_TABLE_STATEMENT = "insert into " + SCHEMA_TABLE + " values(" + VERSION_PLACEHOLDER + ")";
   private static final String UPDATE_SCHEMA_TABLE_STATEMENT = "update " + SCHEMA_TABLE + " set " + VERSION_COLUMN + "=" + VERSION_PLACEHOLDER;
   private static final String GET_SCHEMA_VERSION_STATEMENT = "select * from " + SCHEMA_TABLE;

   // A set of default properties to be used by hibernate.
   private static Properties defaultHibernateConfigProperties = null;

   /**
    * A JDBC connection that will be the only de facto used connection when running in embeded mode.
    */
   private static Connection embededConnection = null;

   // A control-connection could retrieve the correct order of columns etc.
   private Configuration configuration = null;
   private SessionFactory sessionFactory = null;

   /**
    * Configuration settings
    */
   protected String url = null;
   protected String login = ""; // For embedded databases (DERBY)
   protected String password = "";
   protected String driverClassName = null;
   protected String mapping = null;
   protected int databaseType = DERBY;
   protected String connectionPoolMinSize;
   protected String connectionPoolMaxSize;
   protected String cacheCapacity;


   /**
    * Creates a new instance with the provided information. In case that choosen database is HSQLDB, embeded mode will
    * be used.
    *
    * @param name         datasource name
    * @param url          databse connection URL
    * @param driver       JDBC driver to be used
    * @param password     database user password
    * @param login        database username
    * @param databaseType database type
    */
   public OpHibernateSource(String name, String url, String driver, String login, String password, int databaseType) {
      super(name);

      this.url = url;
      this.driverClassName = driver;
      this.password = password;
      this.login = login;
      this.databaseType = databaseType;

      OpBlobUserType.setDatabaseType(databaseType);

      if (databaseType == HSQLDB) {
         this.setEmbeded(true);
      }
   }

   final Configuration getConfiguration() {
      return configuration;
   }

   final SessionFactory getSessionFactory() {
      return sessionFactory;
   }

   public final String getURL() {
      return url;
   }

   public final String getLogin() {
      return login;
   }

   public final String getPassword() {
      return password;
   }

   public final String getDriverClassName() {
      return driverClassName;
   }

   public final int getDatabaseType() {
      return databaseType;
   }

   /**
    * Returns the minimum size of the connection pool.
    */
   public String getConnectionPoolMinSize() {
      return connectionPoolMinSize;
   }

   /**
    * Set the minimum size for the connection pool
    *
    * @param connectionPoolMinSize a <code>String</code> representing the minimum size of the connection pool.
    */
   public void setConnectionPoolMinSize(String connectionPoolMinSize) {
      this.connectionPoolMinSize = connectionPoolMinSize;
   }

   /**
    * Returns the maximum size of the connection pool.
    */
   public String getConnectionPoolMaxSize() {
      return connectionPoolMaxSize;
   }

   /**
    * Set the maximum size for the connection pool
    *
    * @param connectionPoolMaxSize a <code>String</code> representing the maximum size of the connection pool.
    */
   public void setConnectionPoolMaxSize(String connectionPoolMaxSize) {
      this.connectionPoolMaxSize = connectionPoolMaxSize;
   }

   /**
    * Gets the capacity of the hibernate 2nd level cache.
    *
    * @return a <code>Integer</code> representing the maximum number of items that the cache will hold.
    */
   public String getCacheCapacity() {
      return cacheCapacity;
   }

   /**
    * Sets the capacity of the hibernate 2nd level cache.
    *
    * @param cacheCapacity a <code>Integer</code> representing the maximum number of items that the cache will hold.
    */
   public void setCacheCapacity(String cacheCapacity) {
      this.cacheCapacity = cacheCapacity;
   }

   /**
    * Creates a new connection.
    *
    * @return new connection
    */
   public OpConnection newConnection(OpBroker broker) {
      return new OpHibernateConnection(broker, getSession());
   }

   /**
    * Here we create a new instance of Hibernate session with or without an interceptor attached.
    *
    * @return hibernate session or <code>Null</code> if somethig happens and session can't be created.
    */
   protected Session getSession() {
      List<Interceptor> interceptors = getInterceptors();
      OpHibernateInterceptor interceptor = new OpHibernateInterceptor(interceptors);

      Session session = null;
      try {
         if (this.isEmbeded()) {
            if (embededConnection == null) {
               embededConnection = DriverManager.getConnection(this.getURL(), null);
            }
            session = sessionFactory.openSession(embededConnection, interceptor);
         }
         else {
            session = sessionFactory.openSession(interceptor);
            if (session.connection().isClosed()) {
               logger.warn("ERROR: Hibernate supplied closed connection");
            }
         }
      }
      catch (Exception e) {
         logger.error("OpHibernateSource.newConnection(): Could not open session: " + e);
         // *** TODO: Throw OpPersistenceException
      }

      return session;
   }

   /**
    * This method should return a list of Hibernate interceptors. They will be wrapped inside
    * a <code>OpHibernateInterceptor</code> and passed to hibernate.
    *
    * @return a <code>List(Interceptor)</code>.
    */
   protected List<Interceptor> getInterceptors() {
      List<Interceptor> interceptors = new ArrayList<Interceptor>();
      interceptors.add(new OpTimestampInterceptor());
      interceptors.add(new OpTextInterceptor());
      return interceptors;
   }

   /**
    * Based on the database type determine Hibernate dialect to be used.
    *
    * @return Hibernate dialect
    */
   private Class hibernateDialectClass() {
      switch (databaseType) {
         case DERBY:
            return org.hibernate.dialect.DerbyDialect.class;
         case MYSQL_INNODB:
            return org.hibernate.dialect.MySQL5InnoDBDialect.class;
         case POSTGRESQL:
            return org.hibernate.dialect.PostgreSQLDialect.class;
         case ORACLE:
            return org.hibernate.dialect.Oracle9Dialect.class;
         case HSQLDB:
            return org.hibernate.dialect.HSQLDialect.class;
         case IBM_DB2:
            return org.hibernate.dialect.DB2Dialect.class;
         case MSSQL:
            return SQLServerDialect.class;
         default:
            throw new IllegalArgumentException("No dialect for this database type " + databaseType);
      }
   }

   public final Dialect newHibernateDialect() {
      Dialect dialect = null;
      try {
         dialect = (Dialect) (hibernateDialectClass().newInstance());
      }
      catch (Exception e) {
         logger.error("ERROR: Unknown Hibernate dialect: " + hibernateDialectClass().getName());
         System.exit(1);
      }
      return dialect;
   }

   public void open() {
      try {
         // Consider that the configuration is identified uniq by database URL and login/user name.
         String configurationName = url + login;

         // try to retrieve first the configuration from cache.
         OpHibernateCache cache = OpHibernateCache.getInstance();
         configuration = cache.getConfiguration(configurationName);

         if (configuration == null) {
            configuration = new Configuration();
            if (defaultHibernateConfigProperties == null) {
               InputStream input = OpHibernateSource.class.getResourceAsStream("hibernate.properties");
               defaultHibernateConfigProperties = new Properties();
               try {
                  defaultHibernateConfigProperties.load(input);
               }
               catch (IOException e) {
                  logger.error("Cannot load hibernate default properties", e);
               }
            }
            Properties configurationProperties = new Properties();
            configurationProperties.putAll(defaultHibernateConfigProperties);
            configuration.setProperties(configurationProperties);
            // replace default flush listener with ours to enable thread local for sessions
            // FIXME(dfreis Oct 31, 2007 1:05:10 PM) this seems to be the best way, so we should replace
            //       the other (added) listeners within addListeners() if there is time...
            configuration.setListener("flush-entity", new OpFlushEventListener());
            addListeners();
            // Build Hibernate configuration and session factory
            configuration.setProperty("hibernate.connection.driver_class", driverClassName);
            configuration.setProperty("hibernate.connection.url", url);
            configuration.setProperty("hibernate.connection.username", login);
            configuration.setProperty("hibernate.connection.password", password);
            configuration.setProperty("hibernate.dialect", hibernateDialectClass().getName());

            //Important: the following setting is critical for MySQL to store all dates in GMT
            if (databaseType == MYSQL_INNODB) {
               configuration.setProperty("hibernate.connection.useGmtMillisForDatetimes", Boolean.TRUE.toString());
               configuration.setProperty("hibernate.connection.useJDBCCompliantTimezoneShift", Boolean.TRUE.toString());
            }

            //connection pool configuration override
            if (connectionPoolMinSize != null) {
               configuration.setProperty("hibernate.c3p0.min_size", connectionPoolMinSize);
            }
            if (connectionPoolMaxSize != null) {
               configuration.setProperty("hibernate.c3p0.max_size", connectionPoolMaxSize);
            }
            if (cacheCapacity != null) {
               configuration.setProperty("cache.capacity", cacheCapacity);
            }

            Reader reader = new StringReader(mapping);
            ByteArrayOutputStream byte_out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(byte_out, "UTF-8");
            char[] buffer = new char[512];
            int chars_read;
            do {
               chars_read = reader.read(buffer, 0, buffer.length);
               if (chars_read > 0) {
                  writer.write(buffer, 0, chars_read);
               }
            }
            while (chars_read != -1);
            writer.flush();

            // add mappings
            configuration.addInputStream(new ByteArrayInputStream(byte_out.toByteArray()));

            // add this configuration to cache
            cache.addConfiguration(configurationName, configuration);
         }

         // try to retrieve first the session factory from cache.
         sessionFactory = cache.getSessionFactory(configurationName);
         if (sessionFactory == null) {
            sessionFactory = configuration.buildSessionFactory();
            // add this session factory to cache
            cache.addSessionFactory(configurationName, sessionFactory);
         }
      }
      catch (HibernateException e) {
         logger.error("OpHibernateSource.open(): Could not create session factory: " + e);
         throw new OpPersistenceException(e);
      }
      catch (IOException e) {
         logger.error("Cannot write hibernate configuration file", e);
      }
   }

   /**
    * @pre
    * @post
    */
   private void addListeners() {
      EventListeners eventListeners = getConfiguration().getEventListeners();
      PostUpdateEventListener[] postUpdateListeners = eventListeners.getPostCommitUpdateEventListeners();
      PostUpdateEventListener[] newPostUpdateListeners = new PostUpdateEventListener[postUpdateListeners.length + 1];
      System.arraycopy(postUpdateListeners, 0, newPostUpdateListeners, 0, postUpdateListeners.length);
      newPostUpdateListeners[postUpdateListeners.length] = this;
      eventListeners.setPostCommitUpdateEventListeners(newPostUpdateListeners);

      PostDeleteEventListener[] postDeleteListeners = eventListeners.getPostCommitDeleteEventListeners();
      PostDeleteEventListener[] newPostDeleteListeners = new PostDeleteEventListener[postDeleteListeners.length + 1];
      System.arraycopy(postDeleteListeners, 0, newPostDeleteListeners, 0, postDeleteListeners.length);
      newPostDeleteListeners[postDeleteListeners.length] = this;
      eventListeners.setPostCommitDeleteEventListeners(newPostDeleteListeners);

      PostInsertEventListener[] postInsertListeners = eventListeners.getPostCommitInsertEventListeners();
      PostInsertEventListener[] newPostInsertListeners = new PostInsertEventListener[postInsertListeners.length + 1];
      System.arraycopy(postInsertListeners, 0, newPostInsertListeners, 0, postInsertListeners.length);
      newPostInsertListeners[postInsertListeners.length] = this;
      eventListeners.setPostCommitInsertEventListeners(newPostInsertListeners);
   }

   /**
    * @see onepoint.persistence.OpSource#existsTable(String)
    */
   public boolean existsTable(String tableName) {
      Session session = sessionFactory.openSession();
      String queryString = "select * from " + tableName;
      switch (this.databaseType) {
         case MYSQL_INNODB: {
            queryString += " limit 1";
            break;
         }
         case POSTGRESQL: {
            queryString += " limit 1";
            break;
         }
         case IBM_DB2: {
            queryString += " fetch first row only";
            break;
         }
         case ORACLE: {
            queryString += " where ROWNUM < 2";
            break;
         }
         case HSQLDB: {
            queryString = "select top 1 * from " + tableName;
            break;
         }
         case MSSQL: {
            queryString = "select top 1 * from " + tableName;
            break;
         }
      }

      SQLQuery existsQuery = session.createSQLQuery(queryString);
      try {
         existsQuery.list();
         return true;
      }
      catch (HibernateException e) {
         return false;
      }
      finally {
         session.close();
      }
   }

   /**
    * Closes this source, by releasing all resources.
    * <FIXME author="Horia Chiorean" description="According to JLS, this code does assure that we won't get NoClassDefFound if hsqld.jar isn't present in classpath">
    */
   public void close() {
      sessionFactory.close();
      configuration.getProperties().clear();
      if (this.databaseType == HSQLDB) {
         //somewhat dirty, but at least is shuts down properly...
         org.hsqldb.persist.HsqlProperties prop = OpHibernateConnection.cleanupHSQLDBDefaultTableType(this);
         try {
            org.hsqldb.Session localSess = org.hsqldb.DatabaseManager.newSession(OpHibernateSource.HSQLDB_TYPE, OpHibernateConnection.getCleanDBURL(this), getLogin(), getPassword(), prop);
            localSess.sqlExecuteDirectNoPreChecks("SHUTDOWN");
            localSess.commit();
            localSess.close();
            OpHibernateConnection.cleanupHSQLDBDefaultTableType(this); //somehow the shutdown overwrites with the loaded values...
         }
         catch (Exception e) {
            logger.error("Had problems shutting down HSQLDB connection because (will showdown all now): " + e.getMessage(), e);
            org.hsqldb.DatabaseManager.closeDatabases(1);
         }
      }
   }

   // Callbacks invoked by source-manager

   /**
    * This method is called after source registration and should produce Hibernate mappings XML.
    */
   public void onRegister() {
      OpMappingsGenerator gen = getMappingsGenerator();
      gen.init(OpTypeManager.getPrototypes());

      mapping = gen.generateMappings();
   }

   /**
    * Creates and return an instance of <code>OpMappingsGenerator</code> to be used for XML mappings generation
    *
    * @return returns a new instance.
    */
   protected OpMappingsGenerator getMappingsGenerator() {
      return OpMappingsGenerator.getInstance(databaseType);
   }


   /**
    * Updates the schema version number in the db, to the value of the SCHEMA_VERSION constant.
    *
    * @param versionNumber The new schema version number
    */
   public void updateSchemaVersionNumber(Integer versionNumber) {
      Session session = sessionFactory.openSession();
      Connection jdbcConnection = session.connection();
      Statement statement = null;

      try {
         statement = jdbcConnection.createStatement();
         statement.executeUpdate(UPDATE_SCHEMA_TABLE_STATEMENT.replaceAll(VERSION_PLACEHOLDER, String.valueOf(versionNumber)));
         jdbcConnection.commit();
      }
      catch (SQLException e) {
         logger.error("Cannot update db schema number", e);
      }
      finally {
         //the jdbc connection is closed by Hibernate
         OpConnectionManager.closeJDBCObjects(null, statement, null);
         session.close();
      }
   }

   /**
    * Queries the database for the schema version number.
    *
    * @return a <code>int</code> representing the persisted schema version number, or <code>-1</code> if the version number
    *         can't be retrieved.
    */
   public int getExistingSchemaVersionNumber() {
      Session session = sessionFactory.openSession();
      Connection jdbcConnection = session.connection();
      Statement statement = null;
      ResultSet rs = null;
      try {
         createSchemaTable(SCHEMA_VERSION);
         statement = jdbcConnection.createStatement();
         rs = statement.executeQuery(GET_SCHEMA_VERSION_STATEMENT);
         rs.next();
         return rs.getInt(VERSION_COLUMN);
      }
      catch (SQLException e) {
         logger.error("Cannot get version number ", e);
      }
      finally {
         //the connection object is closed by Hibernate
         OpConnectionManager.closeJDBCObjects(null, statement, rs);
         session.close();
      }
      return -1;
   }

//   public void createSchemaTable(int versionNumber) {
//      createSchemaTable(versionNumber, false);
//   }


   /**
    * Creates the schema table and inserts the given version number if the schema table doesn't exist..
    *
    * @param versionNumber a <code>int</code> representing the schema version.
    */
   public void createSchemaTable(int versionNumber)
        throws SQLException {

      if (existsTable(SCHEMA_TABLE)) {
         return;
      }

      Session session = sessionFactory.openSession();
      Connection jdbcConnection = session.connection();
      Statement statement = null;
      try {
         statement = jdbcConnection.createStatement();
         statement.execute(getCreateSchemaTableStatement());
         statement.executeUpdate(INSERT_CURENT_VERSION_INTO_SCHEMA_TABLE_STATEMENT.replaceAll(VERSION_PLACEHOLDER, String.valueOf(versionNumber)));
         jdbcConnection.commit();
         logger.info("Created table op_schema for versioning");
      }
      catch (SQLException e) {
         logger.error("Cannot create schema version or insert version number because:" + e.getMessage(), e);
         throw e;
      }
      finally {
         //the connection object is closed by Hibernate
         OpConnectionManager.closeJDBCObjects(null, statement, null);
         session.close();
      }
   }

   /**
    * Returns a string representing the SQL statement for creating the op_schema table.
    *
    * @return a <code>String</code> representing an SQL "create" statement.
    */
   private String getCreateSchemaTableStatement() {
      if (databaseType == MYSQL_INNODB) {
         return CREATE_SCHEMA_TABLE_STATEMENT + "  ENGINE=InnoDB";
      }
      return CREATE_SCHEMA_TABLE_STATEMENT;
   }

   public void clear() {
      sessionFactory.evictQueries();
      Iterator prototypesIterator = OpTypeManager.getPrototypes();
      while (prototypesIterator.hasNext()) {
         OpPrototype prototype = (OpPrototype) prototypesIterator.next();
         sessionFactory.evict(prototype.getInstanceClass());
      }
   }

   public <O extends OpObject> void addEntityEventListener(Class<O> opclass, OpEntityEventListener listener) {
      if (listeners == null) {
         listeners = new HashMap<Class, Object>();
      }

      Object old = listeners.put(opclass, listener);
      if (old != null) {
         HashSet set;
         if (old instanceof HashSet) {
            set = (HashSet) old;
         }
         else {
            set = new HashSet();
            set.add(old);
         }
         set.add(listener);
         listeners.put(opclass, set);
      }
   }

   public <O extends OpObject> void removeEntityEventListener(Class<O> opclass, OpEntityEventListener listener) {
      if (listeners == null) {
         return;
      }
      Object old = listeners.get(opclass);
      if (old != null) {
         if (old instanceof HashSet) {
            HashSet set = (HashSet) old;
            set.remove(listener);
            if (set.size() == 1) {
               listeners.put(opclass, set.iterator().next());
            }
         }
         else {
            listeners.remove(opclass);
         }
      }
   }

   /* (non-Javadoc)
   * @see org.hibernate.event.PostUpdateEventListener#onPostUpdate(org.hibernate.event.PostUpdateEvent)
   */
   public void onPostUpdate(PostUpdateEvent event) {
      if (listeners == null) {
         return;
      }
      OpObject obj = (OpObject) event.getEntity();
      OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
      String[] propertyNames = event.getPersister().getPropertyNames();
      fireEvent(broker, obj, OpEvent.UPDATE, propertyNames, event.getOldState(), event.getState());
   }


   /* (non-Javadoc)
    * @see org.hibernate.event.PostDeleteEventListener#onPostDelete(org.hibernate.event.PostDeleteEvent)
    */
   public void onPostDelete(PostDeleteEvent event) {
      if (listeners == null) {
         return;
      }
      OpObject obj = (OpObject) event.getEntity();
      OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
      String[] propertyNames = event.getPersister().getPropertyNames();
      fireEvent(broker, obj, OpEvent.DELETE, propertyNames, event.getDeletedState(), null);
   }

   /* (non-Javadoc)
    * @see org.hibernate.event.PostInsertEventListener#onPostInsert(org.hibernate.event.PostInsertEvent)
    */
   public void onPostInsert(PostInsertEvent event) {
      if (listeners == null) {
         return;
      }
      OpObject obj = (OpObject) event.getEntity();
      OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
      String[] propertyNames = event.getPersister().getPropertyNames();
      fireEvent(broker, obj, OpEvent.INSERT, propertyNames, null, event.getState());
   }

   /**
    * @param broker
    * @param obj
    * @param oldState
    * @param state
    * @pre
    * @post
    */
   private OpEvent createEvent(OpBroker broker, OpObject obj, int action,
        String[] propetyNames, Object[] oldState, Object[] state) {
      return new OpEvent(broker, obj, action, propetyNames, oldState, state);
   }

   /**
    * @param broker
    * @param obj
    * @param oldState
    * @param state
    * @param
    * @pre
    * @post
    */
   private void fireEvent(OpBroker broker, OpObject obj, int action, String[] propertyNames, Object[] oldState, Object[] state) {
      if (listeners == null) {
         return;
      }
      Object listener;
      OpEvent event = null;
      Class sourceClass = obj.getClass();
      while (sourceClass != null) {
         listener = listeners.get(sourceClass);
         if (listener != null) {
            if (listener instanceof HashSet) {
               for (Object o : (HashSet) listener) {
                  if (event == null) {
                     event = createEvent(broker, obj, action, propertyNames, oldState, state);
                  }
                  ((OpEntityEventListener) o).entityChangedEvent(event);
               }
            }
            else {
               if (event == null) {
                  event = createEvent(broker, obj, action, propertyNames, oldState, state);
               }
               ((OpEntityEventListener) listener).entityChangedEvent(event);
            }
         }
         if (sourceClass == OpObject.class) {
            break; // stop at OpObject
         }
         sourceClass = sourceClass.getSuperclass();
      }
   }

   private class OpFlushEventListener extends DefaultFlushEntityEventListener {
      public void onFlushEntity(FlushEntityEvent event)
           throws HibernateException {
         if (listeners != null) {
            OpObject obj = (OpObject) event.getEntity();
            OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
            String[] propertyNames = event.getEntityEntry().getPersister().getPropertyNames();
            fireEvent(broker, obj, OpEvent.PRE_FLUSH, propertyNames, event.getDatabaseSnapshot(), event.getPropertyValues());
         }
         super.onFlushEntity(event);
         if (listeners != null) {
            OpObject obj = (OpObject) event.getEntity();
            OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
            String[] propertyNames = event.getEntityEntry().getPersister().getPropertyNames();
            fireEvent(broker, obj, OpEvent.POST_FLUSH, propertyNames, event.getDatabaseSnapshot(), event.getPropertyValues());
         }
      }
   }
}