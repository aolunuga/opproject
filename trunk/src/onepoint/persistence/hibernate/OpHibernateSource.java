/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import onepoint.error.XLocalizableException;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpConnection;
import onepoint.persistence.OpConnectionManager;
import onepoint.persistence.OpEntityEventListener;
import onepoint.persistence.OpEvent;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpPersistenceException;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpSource;
import onepoint.persistence.OpTypeManager;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserService;
import onepoint.project.util.Triple;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.action.EntityAction;
import org.hibernate.action.EntityDeleteAction;
import org.hibernate.action.EntityInsertAction;
import org.hibernate.action.EntityUpdateAction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.ActionQueue;
import org.hibernate.event.EventListeners;
import org.hibernate.event.FlushEntityEvent;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.def.DefaultFlushEntityEventListener;

/**
 * This is an implementation of a data source based on Hibernate.
 */
public class OpHibernateSource extends OpSource
     implements PostUpdateEventListener, PostDeleteEventListener, PostInsertEventListener,
     PreUpdateEventListener, PreDeleteEventListener, PreInsertEventListener {
   /**
    * 
    */
   private static final String HIBERNATE_PROPERTIES_FILE = "hibernate.properties";

   /**
    * The logger used in this class.
    */
   private static final XLog logger = XLogFactory.getLogger(OpHibernateSource.class);

   static ThreadLocal<Object> deadlockDetect = new ThreadLocal<Object>();
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

   final static String HILO_GENERATOR_TABLE_NAME = "hibernate_unique_key";
   final static String HILO_GENERATOR_COLUMN_NAME = "next_hi";

   private static final String USER_HOME = "user.home";

   // FIXME: (dfreis) sometimes hibernate notifies the wrong Source, so we had to made listeners static
   private static HashMap<String, HashMap<Class, HashMap<Integer, Object>>> listeners;

   /**
    * The latest schema version
    */
   public static final int SCHEMA_VERSION = 86;

   /**
    * Db schema related constants
    */
   private static final String VERSION_COLUMN = "op_version";
   private static final String VERSION_PLACEHOLDER = "#";
   private static final String CREATE_SCHEMA_TABLE_STATEMENT = "create table " + SCHEMA_TABLE + "(" + VERSION_COLUMN + " int)";
   private static final String INSERT_CURENT_VERSION_INTO_SCHEMA_TABLE_STATEMENT = "insert into " + SCHEMA_TABLE + " values(" + VERSION_PLACEHOLDER + ")";
   private static final String UPDATE_SCHEMA_TABLE_STATEMENT = "update " + SCHEMA_TABLE + " set " + VERSION_COLUMN + "=" + VERSION_PLACEHOLDER;
   private static final String GET_SCHEMA_VERSION_STATEMENT = "select * from " + SCHEMA_TABLE;

   private static final Object DEADLOCK_OBJ = new Object();

   // A set of default properties to be used by hibernate.
   private static Properties defaultHibernateConfigProperties = null;

   /**
    * A JDBC connection that will be the only de facto used connection when running in embeded mode.
    */
   private static Connection embededConnection = null;

   // A control-connection could retrieve the correct order of columns etc.
   private Configuration configuration = null;
   private SessionFactory sessionFactory = null;
   // specify if the current source is the one which created attached session factory. 
   private boolean sessionFactoryCreator = false;

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

   private Configuration newConfiguration;

   private static HashMap<ActionQueue, HashMap<Object, Triple<Integer, Integer, Object[]>>> transactionMap = new HashMap<ActionQueue, HashMap<Object,Triple<Integer,Integer,Object[]>>>();

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

   public final Configuration getConfiguration() {
      return configuration;
   }

   /**
    * @param addClass
    * @pre
    * @post
    */
   public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
   }

   final SessionFactory getSessionFactory() {
      return sessionFactory;
   }

   final void setSessionFactory(SessionFactory sessionFactory) {
      this.sessionFactory = sessionFactory;
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
         logger.error("OpHibernateSource.newConnection(): Could not open session: " + e, e);
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
	   return hibernateDialectClass(databaseType);
   }

   public static Class hibernateDialectClass(int databaseType) {
	   switch (databaseType) {
	   case DERBY:
		   return org.hibernate.dialect.DerbyDialect.class;
	   case MYSQL_INNODB:
		   return org.hibernate.dialect.MySQL5InnoDBDialect.class;
	   case POSTGRESQL:
		   return org.hibernate.dialect.PostgreSQLDialect.class;
	   case ORACLE:
		   return org.hibernate.dialect.Oracle10gDialect.class;
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

   public static Dialect getDialect(int dbType) {
	   Class dialectClass = hibernateDialectClass(dbType);
	   Properties props = new Properties();
	   props.put(Environment.DIALECT, dialectClass.getName());
	   Dialect dialect = Dialect.getDialect(props);
	   return dialect;
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
         // Consider that the configuration is identified unique by database URL and login/user name.
         String configurationName = url + login;

         // try to retrieve first the configuration from cache.
         OpHibernateCache cache = OpHibernateCache.getInstance();
         configuration = cache.getConfiguration(configurationName);

         if (configuration == null) {
            configuration = createConfiguration();
        	// replace default flush listener with ours to enable thread local for sessions
        	// FIXME(dfreis Oct 31, 2007 1:05:10 PM) this seems to be the best way, so we should replace
        	//       the other (added) listeners within addListeners() if there is time...
            configuration.setListener("flush-entity", new OpFlushEventListener());
            addListeners();

            cache.addConfiguration(configurationName, configuration);
         }
         // try to retrieve first the session factory from cache.
         sessionFactory = cache.getSessionFactory(configurationName);
         if (sessionFactory == null) {
            sessionFactory = configuration.buildSessionFactory();
            // add this session factory to cache
            cache.addSessionFactory(configurationName, sessionFactory);
            sessionFactoryCreator = true;
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

   public Configuration createConfiguration() throws UnsupportedEncodingException,
		IOException {
	Configuration configuration = new Configuration();
	           
	if (defaultHibernateConfigProperties == null) {
	   InputStream input;
	   File configFile = new File(new File(System.getProperty(USER_HOME)), HIBERNATE_PROPERTIES_FILE);
	   try {
	      input = new FileInputStream(configFile);
	      System.out.println("reading log4j properties from: " + configFile.getAbsolutePath());
	   }
	   catch (FileNotFoundException exc) {
	      //use the class loader so there can be a relative location
	      input = OpHibernateSource.class.getResourceAsStream(HIBERNATE_PROPERTIES_FILE);
	   }

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

	// replace ehcache config with url, otherwise it will not be found be ehcache!
	URL ehcacheConfigUrl = OpHibernateSource.class.getResource(configuration.getProperty("hibernate.cache.provider_configuration_file_resource_path"));
	configuration.setProperty("hibernate.cache.provider_configuration_file_resource_path", ehcacheConfigUrl.toString());
	return configuration;
}

   /**
    * @pre
    * @post
    */
   private void addListeners() {
      EventListeners eventListeners = getConfiguration().getEventListeners();
      PreUpdateEventListener[] preUpdateListeners = eventListeners.getPreUpdateEventListeners();
      PreUpdateEventListener[] newPreUpdateListeners = new PreUpdateEventListener[preUpdateListeners.length + 1];
      System.arraycopy(preUpdateListeners, 0, newPreUpdateListeners, 0, preUpdateListeners.length);
      newPreUpdateListeners[preUpdateListeners.length] = this;
      eventListeners.setPreUpdateEventListeners(newPreUpdateListeners);

      PreInsertEventListener[] preInsertListeners = eventListeners.getPreInsertEventListeners();
      PreInsertEventListener[] newPreInsertListeners = new PreInsertEventListener[preInsertListeners.length + 1];
      System.arraycopy(preInsertListeners, 0, newPreInsertListeners, 0, preInsertListeners.length);
      newPreInsertListeners[preInsertListeners.length] = this;
      eventListeners.setPreInsertEventListeners(newPreInsertListeners);

      PreDeleteEventListener[] preDeleteListeners = eventListeners.getPreDeleteEventListeners();
      PreDeleteEventListener[] newPreDeleteListeners = new PreDeleteEventListener[preDeleteListeners.length + 1];
      System.arraycopy(preDeleteListeners, 0, newPreDeleteListeners, 0, preDeleteListeners.length);
      newPreDeleteListeners[preDeleteListeners.length] = this;
      eventListeners.setPreDeleteEventListeners(newPreDeleteListeners);

      eventListeners = getConfiguration().getEventListeners();
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
   public boolean existsColumn(String tableName, String columnName) {
      Session session = sessionFactory.openSession();
      try {
         ResultSet tables = session.connection().getMetaData().getColumns(null, null, tableName, "%");
         try {
            while (tables.next()) {
                if (tables.getString("COLUMN_NAME").equalsIgnoreCase(columnName)) {
                	return true;
                }
            }
            return false;
         } 
         finally {
            tables.close();
         }
      }
      catch (HibernateException exc) {
         logger.warn(exc.getMessage(), exc);
         return false;
      }
      catch (SQLException exc) {
         logger.warn(exc.getMessage(), exc);
         return false;
      }
      finally {
         session.close();
      }
   }

   /**
    * @see onepoint.persistence.OpSource#existsTable(String)
    */
   public boolean existsTable(String tableName) {
      Session session = sessionFactory.openSession();
      try {
         ResultSet tables = session.connection().getMetaData().getTables(null, null, "%", new String[] { "TABLE" });
         try {
            while (tables.next()) {
               if (tables.getString("TABLE_NAME").equalsIgnoreCase(tableName)) {
                  return true;
               }
            }
            return false;
         } 
         finally {
            tables.close();
         }
      }
      catch (HibernateException exc) {
         logger.warn(exc.getMessage(), exc);
         return false;
      }
      catch (SQLException exc) {
         logger.warn(exc.getMessage(), exc);
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
      if (sessionFactoryCreator) {
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
//         statement.close();
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
//         statement.close();
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
         if (!prototype.isInterface()) {
            sessionFactory.evict(prototype.getInstanceClass());
         }
      }
   }

   public <O extends OpObjectIfc> void addEntityEventListener(Class<O> opclass, OpEntityEventListener listener, int events) {
      for (int event : OpEvent.ALL_EVENTS) {
         if ((events & event) != 0) {
            doAddEntityEventListener(opclass, listener, event);
         }
      }
   }

   private <O extends OpObjectIfc> void doAddEntityEventListener(Class<O> opclass, OpEntityEventListener listener, int events) {
      if (listeners == null) {
         listeners = new HashMap<String, HashMap<Class, HashMap<Integer, Object>>>();
      }
    
      String sourceName = getName();
      HashMap<Class, HashMap<Integer, Object>> namedListeners = listeners.get(sourceName);
      if (namedListeners == null) {
         namedListeners = new HashMap<Class, HashMap<Integer,Object>>();
         listeners.put(sourceName, namedListeners);
      }
      HashMap<Integer, Object> classListeners = namedListeners.get(opclass);
      if (classListeners == null) {
         classListeners = new HashMap<Integer, Object>();
         namedListeners.put(opclass, classListeners);
      }
      Object old = classListeners.put(events, listener);
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
         classListeners.put(events, set);
      }
   }

   public <O extends OpObjectIfc> void removeEntityEventListener(Class<O> opclass, OpEntityEventListener listener, int events) {
      for (int event : OpEvent.ALL_EVENTS) {
         if ((events & event) != 0) {
            doRemoveEntityEventListener(opclass, listener, event);
         }
      }
   }

   public <O extends OpObjectIfc> void doRemoveEntityEventListener(Class<O> opclass, OpEntityEventListener listener, int event) {
      if (listeners == null) {
         return;
      }
      HashMap<Class, HashMap<Integer, Object>> namedListeners = listeners.get(opclass);
      if (namedListeners == null) {
         return;
      }
      HashMap<Integer, Object> classListeners = namedListeners.get(opclass);
      if (classListeners == null) {
         return;
      }
      
      Object old = classListeners.get(event);
      if (old != null) {
         if (old instanceof HashSet) {
            HashSet set = (HashSet) old;
            set.remove(listener);
            if (set.size() == 1) {
               classListeners.put(event, set.iterator().next());
            }
         }
         else {
            classListeners.remove(event);
         }
      }
   }

   /* (non-Javadoc)
   * @see org.hibernate.event.PostUpdateEventListener#onPostUpdate(org.hibernate.event.PostUpdateEvent)
   */
   public void onPostUpdate(PostUpdateEvent event) {
      ActionQueue aq = event.getSession().getActionQueue();
      Triple<Integer, Integer, Object[]> triple;
      if (aq != null) {
         HashMap<Object, Triple<Integer, Integer, Object[]>> map = getTransactionMap(aq);
         triple = map.get(event.getEntity());
         if (triple.getThird() == null) {
            triple.setThird(event.getOldState());
         }
         triple.setSecond(triple.getSecond()+1);
         if (triple.getFirst() == triple.getSecond()) {
            map.remove(event.getEntity());
            if (map.isEmpty()) {
               transactionMap.remove(aq);
            }
         }
      }
      else {
         triple = new Triple<Integer, Integer, Object[]>(0,0, null);
      }
      //check();
      if (listeners == null) {
         return;
      }
      OpObjectIfc obj = (OpObjectIfc) event.getEntity();
      OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
      String[] propertyNames = event.getPersister().getPropertyNames();
      fireEvent(broker, obj, OpEvent.UPDATE, propertyNames, event.getOldState(), event.getState(), 
            triple.getThird(), triple.getFirst() == triple.getSecond());
      //System.err.println("update max: "+triple.getFirst()+", current: "+triple.getSecond()+", obj: "+obj);
   }

   /**
    * @param aq
    * @return
    * @pre
    * @post
    */
   private HashMap<Object, Triple<Integer, Integer, Object[]>> getTransactionMap(
         ActionQueue aq) {
      HashMap<Object, Triple<Integer, Integer, Object[]>> map = transactionMap.get(aq);
      if (map == null) {
         fillUpTransactionMap(aq);
         map = transactionMap.get(aq);
      }
      return map;
   }

   /**
    * @param aq
    * @pre
    * @post
    */
   private void fillUpTransactionMap(ActionQueue aq) {
      try {
         // very hacky way: need to find out which objects are within the ActionQueue 
         // (queue holding all changed objects within a transaction)
         // needs access to private fields and stores the values within a map holding the actionQueue 
         // (identifies the transaction) together with a max ref count (the amount of how often one and the same 
         // object is within the action queue (=dirty elements) (may be more than once eg: if OpActivity.setFinish is called more that 
         // once within one transaction)) and a count that is increased during commit.
         HashMap<Object, Triple<Integer, Integer, Object[]>> map = null;
         Field field = ActionQueue.class.getDeclaredField("executions");
         field.setAccessible(true);
         List list = (List) field.get(aq);
         
         for (Object obj : list) {
            if (map == null){
               map = new HashMap<Object, Triple<Integer,Integer,Object[]>>();
            }
            if (obj instanceof EntityAction) {
               EntityAction action = (EntityAction)obj;
               if (!((action instanceof EntityUpdateAction) || (action instanceof EntityInsertAction) ||
                     (action instanceof EntityDeleteAction))) {
                  continue;
               }
//               Object object = action.getInstance();
               field = EntityAction.class.getDeclaredField("instance");
               field.setAccessible(true);
               Object object = field.get(action);
               Triple<Integer, Integer, Object[]> triple = map.get(object);
               if (triple != null) {
                  triple.setFirst(triple.getFirst()+1);
               }
               else {
                  triple = new Triple<Integer, Integer, Object[]>(1,0,null);
                  map.put(object, triple);
               }
            }
         }
         if (map != null) {
            transactionMap.put(aq, map);
         }
      }
      catch (SecurityException exc) {
         logger.error(exc.getMessage(), exc);
      }
      catch (NoSuchFieldException exc) {
         logger.error(exc.getMessage(), exc);
      }
      catch (IllegalArgumentException exc) {
         logger.error(exc.getMessage(), exc);
      }
      catch (IllegalAccessException exc) {
         logger.error(exc.getMessage(), exc);
      }
      
      
   }

   /* (non-Javadoc)
    * @see org.hibernate.event.PreUpdateEventListener#onPreUpdate(org.hibernate.event.PreUpdateEvent)
    */
   public boolean onPreUpdate(PreUpdateEvent event) {
      if (isReadOnlyMode()) {
         throw new XLocalizableException(OpUserService.ERROR_MAP, OpUserError.SITE_IS_INVALID);
      }
      return false;
   }

   /* (non-Javadoc)
    * @see org.hibernate.event.PreDeleteEventListener#onPreDelete(org.hibernate.event.PreDeleteEvent)
    */
   public boolean onPreDelete(PreDeleteEvent event) {
      if (isReadOnlyMode()) {
         throw new XLocalizableException(OpUserService.ERROR_MAP, OpUserError.SITE_IS_INVALID);
      }
      return false;
   }

   /* (non-Javadoc)
    * @see org.hibernate.event.PreDeleteEventListener#onPreDelete(org.hibernate.event.PreDeleteEvent)
    */
   public boolean onPreInsert(PreInsertEvent event) {
      if (isReadOnlyMode()) {
         throw new XLocalizableException(OpUserService.ERROR_MAP, OpUserError.SITE_IS_INVALID);
      }
      return false;
   }

//   /**
//    * 
//    * @pre
//    * @post
//    */
//   private void check() {
//      System.err.println("XXX: "+transactionMap.size());
//   }

   /* (non-Javadoc)
    * @see org.hibernate.event.PostDeleteEventListener#onPostDelete(org.hibernate.event.PostDeleteEvent)
    */
   public void onPostDelete(PostDeleteEvent event) {
      ActionQueue aq = event.getSession().getActionQueue();
      Triple<Integer, Integer, Object[]> triple;
      if (aq != null) {
         HashMap<Object, Triple<Integer, Integer, Object[]>> map = getTransactionMap(aq);
         triple = map.get(event.getEntity());
         if (triple.getThird() == null) {
            triple.setThird(event.getDeletedState());
         }
         triple.setSecond(triple.getSecond()+1);
         if (triple.getFirst() == triple.getSecond()) {
            map.remove(event.getEntity());
            if (map.isEmpty()) {
               transactionMap.remove(aq);
            }
         }
      }
      else {
         triple = new Triple<Integer, Integer, Object[]>(0,0, null);
      }
      //check();
      if (listeners == null) {
         return;
      }
      OpObjectIfc obj = (OpObjectIfc) event.getEntity();
      OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
      String[] propertyNames = event.getPersister().getPropertyNames();
      fireEvent(broker, obj, OpEvent.DELETE, propertyNames, event.getDeletedState(), null, 
            triple.getThird(), triple.getFirst() == triple.getSecond());
      //System.err.println("delete max: "+triple.getFirst()+", current: "+triple.getSecond()+", obj: "+obj);
   }

   /* (non-Javadoc)
    * @see org.hibernate.event.PostInsertEventListener#onPostInsert(org.hibernate.event.PostInsertEvent)
    */
   public void onPostInsert(PostInsertEvent event) {
      ActionQueue aq = event.getSession().getActionQueue();
      Triple<Integer, Integer, Object[]> triple = null;
      if (aq != null) {
         HashMap<Object, Triple<Integer, Integer, Object[]>> map = getTransactionMap(aq);
         triple = map.get(event.getEntity());
         if (triple != null) {
            triple.setSecond(triple.getSecond()+1);
            if (triple.getFirst() == triple.getSecond()) {
               map.remove(event.getEntity());
               if (map.isEmpty()) {
                  transactionMap.remove(aq);
               }
            }
         }
      }
      if (triple == null) {
         triple = new Triple<Integer, Integer, Object[]>(0,0, null);
      }
      //check();
      if (listeners == null) {
         return;
      }
      OpObjectIfc obj = (OpObjectIfc) event.getEntity();
      OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
      String[] propertyNames = event.getPersister().getPropertyNames();
      fireEvent(broker, obj, OpEvent.INSERT, propertyNames, null, event.getState(),
            triple.getThird(), triple.getFirst() == triple.getSecond());
      //System.err.println("insert max: "+triple.getFirst()+", current: "+triple.getSecond()+", obj: "+obj);
   }


   /**
    * @param broker
    * @param obj
    * @param oldState
    * @param state
    * @param current 
    * @param max 
    * @pre
    * @post
    */
   private OpEvent createEvent(OpBroker broker, OpObjectIfc obj, int action,
        String[] propetyNames, Object[] oldState, Object[] state, Object[] firstState, boolean last) {
      return new OpEvent(broker, obj, action, propetyNames, oldState, state, firstState, last);
   }

   /**
    * @param broker
    * @param obj
    * @param oldState
    * @param state
    * @param current 
    * @param max 
    * @param
    * @pre
    * @post
    */
   private void fireEvent(OpBroker broker, OpObjectIfc obj, int action, String[] propertyNames, Object[] oldState, Object[] state, Object[] initialState, boolean last) {
      if (listeners == null) {
         return;
      }
      // deadlock detection - hibernate sometimes causes recursive call here if a listener calls eg. broker.getObject(..)
      if ((action != OpEvent.PRE_FLUSH) && (deadlockDetect.get() != null)) {
         logger.info("deadlock detected, will not notify listeners again!");
//         return;
      }
      deadlockDetect.set(DEADLOCK_OBJ);
      try {
         HashMap<Class, HashMap<Integer, Object>> namedListeners = listeners.get(obj.getSiteId());
         if (namedListeners != null) {
            HashMap<Integer, Object> listener;
            OpEvent event = null;
            Class sourceClass = obj.getClass();
            while (sourceClass != null) {
               listener = namedListeners.get(sourceClass);
               if (listener != null) {
                  Object actionListener = listener.get(action);
                  if (actionListener != null) {
                     if (actionListener instanceof HashSet) {
                        for (Object o : (HashSet) actionListener) {
                           OpEntityEventListener eventListener = (OpEntityEventListener) o; 
                           if (event == null) {
                              event = createEvent(broker, obj, action, propertyNames, oldState, state, initialState, last);
                           }
                           eventListener.entityChangedEvent(event);
                        }
                     }
                     else {
                        if (event == null) {
                           event = createEvent(broker, obj, action, propertyNames, oldState, state, initialState, last);
                        }
                        ((OpEntityEventListener) actionListener).entityChangedEvent(event);
                     }
                  }
               }
               if (sourceClass == OpObjectIfc.class) {
                  break; // stop at OpObjectIfc
               }
               sourceClass = sourceClass.getSuperclass();
            }
         }
      }
      finally {
         deadlockDetect.set(null);
      }
   }

   private class OpFlushEventListener extends DefaultFlushEntityEventListener {
      public void onFlushEntity(FlushEntityEvent event)
           throws HibernateException {
         if (listeners != null) {
            OpObjectIfc obj = (OpObjectIfc) event.getEntity();
            HashMap<Class, HashMap<Integer, Object>> namedListeners = listeners.get(obj.getSiteId());
            if (namedListeners != null) {
               HashMap<Integer, Object> classListeners = namedListeners.get(obj.getClass());
               if ((classListeners != null) && (classListeners.get(OpEvent.PRE_FLUSH)) != null) {
                  OpBroker broker = OpHibernateConnection.getBroker(event.getSession());
                  String[] propertyNames = event.getEntityEntry().getPersister().getPropertyNames();
                  Object[] oldState = event.getDatabaseSnapshot();
                  fireEvent(broker, obj, OpEvent.PRE_FLUSH, propertyNames, oldState, event.getPropertyValues(), oldState, true);
                  super.onFlushEntity(event);
                  fireEvent(broker, obj, OpEvent.POST_FLUSH, propertyNames, oldState, event.getPropertyValues(), oldState, true);
                  return;
               }
            }
         }
         super.onFlushEntity(event);
      }
   }

   /**
    * @return
    * @pre
    * @post
    */
   public String getConfigurationName() {
      return url+login;
   }

   public void setNewConfiguration(Configuration newConfiguration) {
	   this.newConfiguration = newConfiguration;
   }

   public Configuration getNewConfiguration() {
	   return newConfiguration;
   }
}