/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.persistence.hibernate.cache.OpOSCache;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;

import java.io.*;
import java.sql.*;
import java.util.*;

public class OpHibernateSource extends OpSource {

   /**
    * The logger used in this class.
    */
   private static final XLog logger = XLogFactory.getLogger(OpHibernateSource.class, true);

   /**
    * Constant that defines
    */

   // Hibernate source that is always auto-mapped
   public final static int DERBY = 1; // Default database type
   public final static int MYSQL = 2;
   public final static int MYSQL_INNODB = 3;
   public final static int POSTGRESQL = 4;
   public final static int ORACLE = 5;
   public final static int HSQLDB = 6;
   public final static int IBM_DB2 = 7;
   public final static int SQLSERVER = 8;

   public final static String INDEX_NAME_PREFIX = "op_";
   public final static String INDEX_NAME_POSTFIX = "_i";
   public final static String COLUMN_NAME_PREFIX = "op_";
   public final static String COLUMN_NAME_POSTFIX = "";
   public final static String TABLE_NAME_PREFIX = "op_";
   public final static String TABLE_NAME_POSTFIX = "";
   public final static String JOIN_NAME_SEPARATOR = "_";

   public final static String HSQLDB_TYPE = "file:";
   public final static String HSQLDB_JDBC_CONNECTION_PREFIX = "jdbc:hsqldb:" + HSQLDB_TYPE;

   /**
    * Strings representing prototype prefixes
    */
   private static final List PROTOTYPE_PREFIXES = Arrays.asList(new String[]{"X", "Op"});

   /**
    * Db schema related constants
    */
   static final String SCHEMA_TABLE = "op_schema";
   private static final int SCHEMA_VERSION = 3;
   private static final String VERSION_COLUMN = "op_version";

   private static final String CREATE_SCHEMA_TABLE_STATEMENT = "create table " + SCHEMA_TABLE + "(" + VERSION_COLUMN + " int)";
   private static final String INSERT_ZERO_INTO_SCHEMA_TABLE_STATEMENT = "insert into " + SCHEMA_TABLE + " values(0)";
   private static final String UPDATE_SCHEMA_TABLE_STATEMENT = "update " + SCHEMA_TABLE + " set " + VERSION_COLUMN + "=" + SCHEMA_VERSION;
   private static final String GET_SCHEMA_VERSION_STATEMENT = "select * from " + SCHEMA_TABLE;

   // A set of default properties to be used by hibernate.
   private static Properties defaultHibernateConfigProperties = null;

   /**
    * A JDBC connection that will be the only de facto used connection when running in embeded mode.
    */
   private static Connection embededConnection = null;

   // A control-connection could retrieve the correct order of columns etc.
   private Configuration _configuration = null;
   private SessionFactory _session_factory = null;

   /**
    * Configuration settings
    */
   private String _url = null;
   private String _login = ""; // For embedded databases (DERBY)
   private String _password = "";
   private String _driver_class_name = null;
   private String _mapping = null;
   private int _database_type = DERBY;
   private String connectionPoolMinSize;
   private String connectionPoolMaxSize;
   private String cacheCapacity;

   /**
    * IBM DB2 index maximum length is 18 (SQLSTATE=42622)
    */
   private static final int IBM_DB2_INDEX_NAME_LENGTH = 18;

   public OpHibernateSource(String _url, String _driver_class_name, String _password, String _login, int _database_type) {
      this._url = _url;
      this._driver_class_name = _driver_class_name;
      this._password = _password;
      this._login = _login;
      this._database_type = _database_type;
      OpBlobUserType.setDatabaseType(_database_type);

      if (_database_type == HSQLDB) {
         this.setEmbeded(true);
      }
   }

   final Configuration getConfiguration() {
      return _configuration;
   }

   final SessionFactory getSessionFactory() {
      return _session_factory;
   }

   public final void setURL(String url) {
      _url = url;
   }

   public final String getURL() {
      return _url;
   }

   public final void setLogin(String login) {
      _login = login;
   }

   public final String getLogin() {
      return _login;
   }

   public final void setPassword(String password) {
      _password = password;
   }

   public final String getPassword() {
      return _password;
   }

   public final void setDriverClassName(String driver_class_name) {
      _driver_class_name = driver_class_name;
   }

   public final String getDriverClassName() {
      return _driver_class_name;
   }

   public final int getDatabaseType() {
      return _database_type;
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

   public OpConnection newConnection() {
      Session session = null;
      try {
         if (this.isEmbeded()) {
            if (embededConnection == null) {
               embededConnection = DriverManager.getConnection(this.getURL(), null);
            }
            session = _session_factory.openSession(embededConnection);
         }
         else {
            session = _session_factory.openSession();
            if (session.connection().isClosed()) {
               logger.warn("ERROR: Hibernate supplied closed connection");
            }
         }
      }
      catch (Exception e) {
         logger.error("OpHibernateSource.newConnection(): Could not open session: " + e);
         // *** TODO: Throw OpPersistenceException
      }
      return new OpHibernateConnection(this, session);
   }

   public Class hibernateDialectClass() {
      switch (_database_type) {
         case DERBY:
            return org.hibernate.dialect.DerbyDialect.class;
         case MYSQL:
            return org.hibernate.dialect.MySQLDialect.class;
         case MYSQL_INNODB:
            return org.hibernate.dialect.MySQLInnoDBDialect.class;
         case POSTGRESQL:
            return org.hibernate.dialect.PostgreSQLDialect.class;
         case ORACLE:
            return org.hibernate.dialect.Oracle9Dialect.class;
         case HSQLDB:
            return org.hibernate.dialect.HSQLDialect.class;
         case IBM_DB2:
            return org.hibernate.dialect.DB2Dialect.class;
         case SQLSERVER:
            return SQLServerDialect.class;
         default:
            throw new IllegalArgumentException("No dialect for this database type " + _database_type);
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

   /**
    * Initializes a set of default properties for the hibernate configuration. Some of these properties can be overriden
    * by using the configuration.oxc.xml file.
    */
   private void initDefaultConfigurationSettings() {
      _configuration = new Configuration();
      if (defaultHibernateConfigProperties == null) {
         InputStream input = this.getClass().getResourceAsStream("hibernate.properties");
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
      _configuration.setProperties(configurationProperties);
   }

   public void open() {
      initDefaultConfigurationSettings();
      // Build Hibernate configuration and session factory
      _configuration.setProperty("hibernate.connection.driver_class", _driver_class_name);
      _configuration.setProperty("hibernate.connection.url", _url);
      _configuration.setProperty("hibernate.connection.username", _login);
      _configuration.setProperty("hibernate.connection.password", _password);
      _configuration.setProperty("hibernate.dialect", hibernateDialectClass().getName());

      //connection pool configuration override
      if (connectionPoolMinSize != null) {
         _configuration.setProperty("hibernate.c3p0.min_size", connectionPoolMinSize);
      }
      if (connectionPoolMaxSize != null) {
         _configuration.setProperty("hibernate.c3p0.max_size", connectionPoolMaxSize);
      }
      if (cacheCapacity != null) {
         _configuration.setProperty(OpOSCache.OSCACHE_CAPACITY, cacheCapacity);
      }

      Reader reader = new StringReader(_mapping);
      ByteArrayOutputStream byte_out = new ByteArrayOutputStream();
      try {
         OutputStreamWriter writer = new OutputStreamWriter(byte_out, "UTF-8");
         char[] buffer = new char[512];
         int chars_read = 0;
         do {
            chars_read = reader.read(buffer, 0, buffer.length);
            if (chars_read > 0) {
               writer.write(buffer, 0, chars_read);
            }
         }
         while (chars_read != -1);
         writer.flush();
         _configuration.addInputStream(new ByteArrayInputStream(byte_out.toByteArray()));
         logger.debug("***before sf");
         _session_factory = _configuration.buildSessionFactory();
         logger.debug("***after sf");
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
    * @see onepoint.persistence.OpSource#existsTable(String)
    */
   public boolean existsTable(String tableName) {
      Session session = _session_factory.openSession();
      String queryString = "select * from " + tableName;
      switch (this._database_type) {
         case MYSQL: {
            queryString += " limit 1";
            break;
         }
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
         case SQLSERVER: {
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
    */
   public void close() {
      _session_factory.close();
      _configuration.getProperties().clear();
   }

   public final String newColumnName(String property_name) {
      StringBuffer buffer = new StringBuffer(COLUMN_NAME_PREFIX);
      buffer.append(property_name.toLowerCase());
      buffer.append(COLUMN_NAME_POSTFIX);
      return buffer.toString();
   }

   public final String newTableName(String prototype_name) {
      StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
      buffer.append(removePrototypeNamePrefixes(prototype_name).toLowerCase());
      buffer.append(TABLE_NAME_POSTFIX);
      return buffer.toString();
   }

   /**
    * Removes prefixes from the name of a prototype, each prefix at most once.
    *
    * @param prototypeName a <code>String</code> representing the name of a prototype.
    * @return a <code>String</code> representing the name of the prototype without the prefixes.
    */
   private String removePrototypeNamePrefixes(String prototypeName) {
      for (int i = 0; i < PROTOTYPE_PREFIXES.size(); i++) {
         String prefix = (String) PROTOTYPE_PREFIXES.get(i);
         if (prototypeName.startsWith(prefix)) {
            prototypeName = prototypeName.replaceAll(prefix, "");
         }
      }
      return prototypeName;
   }

   public final String newJoinTableName(String prototype_name1, String prototype_name2) {
      StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
      buffer.append(prototype_name1.toLowerCase());
      buffer.append(JOIN_NAME_SEPARATOR);
      buffer.append(prototype_name2.toLowerCase());
      buffer.append(TABLE_NAME_POSTFIX);
      return buffer.toString();
   }

   public final String newJoinColumnName(String prototype_name, String property_name) {
      // TODO: Attention -- we should probably remove this (outdated and uses "X" names)
      StringBuffer buffer = new StringBuffer(TABLE_NAME_PREFIX);
      buffer.append(prototype_name.toLowerCase());
      buffer.append(JOIN_NAME_SEPARATOR);
      buffer.append(property_name.toLowerCase());
      buffer.append(TABLE_NAME_POSTFIX);
      return buffer.toString();
   }

   public final String newIndexName(String prototype_name, String property_name) {
      StringBuffer buffer = new StringBuffer(newTableName(prototype_name));
      buffer.append(JOIN_NAME_SEPARATOR);
      buffer.append(property_name.toLowerCase());
      buffer.append(INDEX_NAME_POSTFIX);
      if (_database_type == IBM_DB2 && buffer.length() > IBM_DB2_INDEX_NAME_LENGTH) {
         String indexName = buffer.substring(TABLE_NAME_PREFIX.length());
         int start = 0;
         if (indexName.length() > IBM_DB2_INDEX_NAME_LENGTH) {
            start = indexName.length() - IBM_DB2_INDEX_NAME_LENGTH;
         }
         return indexName.substring(start, indexName.length());
      }
      return buffer.toString();
   }

   // Callbacks invoked by source-manager

   /**
    * <FIXME author="Horia Chiorean" description="Remove HARD CODING config for OpObject !">
    */
   public void onRegister() {
      logger.debug("ON_REG\n\n");
      // Create Hibernate mapping file
      StringBuffer buffer = new StringBuffer();
      buffer.append("<?xml version=\"1.0\"?>\n");
      buffer.append("<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">");
      buffer.append("<hibernate-mapping>\n");
      // TODO: Finalize inheritance (now we assume a simple, fixed 2-level
      // inheritance, i.e., all extend OpObject)
      buffer.append("<class name=\"onepoint.persistence.OpObject\" table=\"op_object\">\n");
      // Add hard-coded primary key property "ID"
      buffer.append("<cache usage=\"read-write\"/>\n");
      buffer.append("      <id name=\"ID\" column=\"op_id\" type=\"long\">\n");
      buffer.append("         <generator class=\"hilo\"/>\n");
      buffer.append("      </id>\n");
      // Add remaining OpObject relationships and fields
      // TODO: When finalizing inheritance, try to hard-code "less" for OpObject
      // (Now, this code maps OpObject in a totally hard-coded way)
      /*
      buffer.append("      <many-to-one name=\"AccessContext\" column=\"");
      buffer.append(newColumnName("AccessContext"));
      buffer.append("\" class=\"onepoint.persistence.OpObject\"/>\n");
      buffer.append("      <set name=\"GuardedObjects\" inverse=\"true\">\n");
      buffer.append("         <key column=\"");
      buffer.append(newColumnName("AccessContext"));
      buffer.append("\"/>\n");
      buffer.append("         <one-to-many class=\"onepoint.persistence.OpObject\"/>\n");
      buffer.append("      </set>\n");
      */
      buffer.append("      <property name=\"Created\" type=\"timestamp\" not-null=\"true\" index=\"object_created_i\"/>\n");
      buffer.append("      <property name=\"Modified\" type=\"timestamp\"/>\n");
      buffer.append("      <set name=\"Permissions\" inverse=\"true\" cascade=\"delete\">\n");
      buffer.append("         <key column=\"");
      buffer.append(newColumnName("Object"));
      buffer.append("\"/>\n");
      buffer.append("         <one-to-many class=\"onepoint.project.modules.user.OpPermission\"/>\n");
      buffer.append("      </set>\n");
      buffer.append("      <set name=\"Locks\" inverse=\"true\" cascade=\"delete\">\n");
      buffer.append("         <key column=\"");
      buffer.append(newColumnName("Target"));
      buffer.append("\"/>\n");
      buffer.append("         <one-to-many class=\"onepoint.project.modules.user.OpLock\"/>\n");
      buffer.append("      </set>\n");
      buffer.append("      <set name=\"DynamicResources\" inverse=\"true\" cascade=\"delete\">\n");
      buffer.append("         <key column=\"");
      buffer.append(newColumnName("Object"));
      buffer.append("\"/>\n");
      buffer.append("         <one-to-many class=\"onepoint.project.modules.documents.OpDynamicResource\"/>\n");
      buffer.append("      </set>\n");

      // Find out which "start" prototypes (only derived from OpObject)
      Iterator prototypes = OpTypeManager.getPrototypes();
      ArrayList extendObjectOnly = new ArrayList();
      OpPrototype prototype = null;
      while (prototypes.hasNext()) {
         prototype = (OpPrototype) prototypes.next();
         if (prototype.getSuperType() == null) {
            extendObjectOnly.add(prototype);
         }
      }
      prototypes = extendObjectOnly.iterator();

      while (prototypes.hasNext()) {
         prototype = (OpPrototype) (prototypes.next());
         // Ignore hard-coded prototype "OpObject"
         if (prototype.getName().equals("OpObject")) {
            continue;
         }
         appendSubTypeMapping(buffer, prototype);
      }
      buffer.append("\n</class>\n");
      buffer.append("</hibernate-mapping>\n");
      // Set mapping
      _mapping = buffer.toString();

      System.err.println("***MAPPING\n---\n");
      System.err.println(_mapping);
      System.err.println("\n");

      logger.debug(_mapping);
   }

   protected void appendSubTypeMapping(StringBuffer buffer, OpPrototype prototype) {
      buffer.append("\n   <joined-subclass name=\"");
      buffer.append(prototype.getInstanceClass().getName());
      buffer.append("\" table=\"");
      String table_name = newTableName(prototype.getName());
      buffer.append(table_name);
      buffer.append("\">\n");
      // Add hard-coded join-key for column of property "ID"
      buffer.append("      <key column=\"op_id\"/>\n");
      // Add declared members (only these of this inheritance level)
      Iterator members = prototype.getDeclaredMembers();
      OpMember member = null;
      OpField field = null;
      OpRelationship relationship = null;
      while (members.hasNext()) {
         member = (OpMember) (members.next());
         if (member instanceof OpField) {
            // Map field
            field = (OpField) member;
            buffer.append("      <property name=\"");
            buffer.append(field.getName());

            buffer.append("\" type=\"");
            buffer.append(OpHibernateSource.getHibernateTypeName(field.getTypeID()));

            if (field.getMandatory()) {
               buffer.append("\" not-null=\"true");
            }

            if (field.getUnique()) {
               buffer.append("\" unique=\"true");
            }

            buffer.append("\"><column name=\"");
            buffer.append(newColumnName(field.getName()));
            buffer.append('"');

            // Exception for MySQL: Use mediumblob (otherwise very limited
            // storage capability)
            if ((field.getTypeID() == OpType.CONTENT)) {
               if ((_database_type == MYSQL) || (_database_type == MYSQL_INNODB)) {
                  buffer.append(" sql-type=\"mediumblob\"");
               }
               if ((_database_type == IBM_DB2)) {
                  buffer.append(" sql-type=\"blob(100M)\"");
               }
            }

            if (field.getMandatory()) {
               buffer.append(" not-null=\"true\"");
            }

            if (field.getUnique() && !(field.getTypeID() == OpType.TEXT)) {
               buffer.append(" unique=\"true\"");
            }

            if (field.getIndexed() && !(field.getTypeID() == OpType.TEXT)) {
               buffer.append(" index=\"");
               buffer.append(newIndexName(prototype.getName(), field.getName()));
               buffer.append('\"');
            }

            if (field.getTypeID() == OpType.TEXT) {
               int maxLen = OpTypeManager.getMaxLength(OpType.TEXT);
               buffer.append(" length=\"").append(maxLen).append("\"");
            }

            buffer.append("/></property>\n");

         }
         else {
            // Map relationship
            relationship = (OpRelationship) member;
            String cascadeMode = relationship.getCascadeMode();
            OpRelationship back_relationship = relationship.getBackRelationship();
            OpPrototype target_prototype = OpTypeManager.getPrototypeByID(relationship.getTypeID());
            if (relationship.getCollectionTypeID() != OpType.SET) {
               // Map one-to-one or many-to-one relationship
               if ((back_relationship != null) && (back_relationship.getCollectionTypeID() != OpType.SET)) {
                  // Map one-to-one relationship
                  if (relationship.getInverse()) {
                     buffer.append("      <one-to-one name=\"");
                     buffer.append(relationship.getName());
                     /*
                     buffer.append("\" column=\"");
                     buffer.append(newColumnName(relationship.getName()));
                     buffer.append("\" class=\"");
                     buffer.append(target_prototype.getInstanceClass().getName());
                     */
                     buffer.append("\" property-ref=\"");
                     buffer.append(back_relationship.getName());
                     if (cascadeMode != null) {
                        buffer.append("\" cascade=\"");
                        buffer.append(cascadeMode);
                     }
                     buffer.append("\"/>\n");
                  }
                  else {
                     buffer.append("      <many-to-one name=\"");
                     buffer.append(relationship.getName());
                     buffer.append("\" column=\"");
                     buffer.append(newColumnName(relationship.getName()));
                     buffer.append("\" class=\"");
                     buffer.append(target_prototype.getInstanceClass().getName());
                     buffer.append("\" unique=\"true\" not-null=\"true");
                     if (cascadeMode != null) {
                        buffer.append("\" cascade=\"");
                        buffer.append(cascadeMode);
                     }
                     buffer.append("\"/>\n");
                  }
               }
               else {
                  // Map many-to-one relationship
                  buffer.append("      <many-to-one name=\"");
                  buffer.append(relationship.getName());
                  buffer.append("\" column=\"");
                  buffer.append(newColumnName(relationship.getName()));
                  buffer.append("\" class=\"");
                  buffer.append(target_prototype.getInstanceClass().getName());
                  if (cascadeMode != null) {
                     buffer.append("\" cascade=\"");
                     buffer.append(cascadeMode);
                  }
                  buffer.append("\"/>\n");
               }
            }
            else if (back_relationship != null) {
               // Map one-to-many or many-to-many relationship
               if (back_relationship.getCollectionTypeID() != OpType.SET) {
                  // Map one-to-many relationship
                  buffer.append("      <set name=\"");
                  buffer.append(relationship.getName());
                  if (relationship.getInverse()) {
                     buffer.append("\" inverse=\"true");
                  }
                  buffer.append("\" lazy=\"true");
                  if (cascadeMode != null) {
                     buffer.append("\" cascade=\"");
                     buffer.append(cascadeMode);
                  }
                  buffer.append("\">\n");
//                  buffer.append("<cache usage=\"read-write\"/>\n");
                  buffer.append("         <key column=\"");
                  buffer.append(newColumnName(back_relationship.getName()));
                  buffer.append("\"/>\n");
                  buffer.append("         <one-to-many class=\"");
                  buffer.append(target_prototype.getInstanceClass().getName());
                  buffer.append("\"/>\n");
                  buffer.append("      </set>\n");
               }
               else {
                  // Map many-to-many relationship
                  buffer.append("      <set name=\"");
                  buffer.append(relationship.getName());
                  String join_table_name = null;
                  String key_column_name = null;
                  String column_name = null;
                  if (relationship.getInverse()) {
                     buffer.append("\" inverse=\"true");
                     join_table_name = newJoinTableName(target_prototype.getName(), back_relationship.getName());
                  }
                  else {
                     join_table_name = newJoinTableName(prototype.getName(), relationship.getName());
                  }
                  key_column_name = newJoinColumnName(prototype.getName(), relationship.getName());
                  column_name = newJoinColumnName(target_prototype.getName(), back_relationship.getName());
                  buffer.append("\" table=\"");
                  buffer.append(join_table_name);
                  buffer.append("\" lazy=\"true");
                  if (cascadeMode != null) {
                     buffer.append("\" cascade=\"");
                     buffer.append(cascadeMode);
                  }
                  buffer.append("\">\n");
//                  buffer.append("<cache usage=\"read-write\"/>\n");
                  buffer.append("         <key column=\"");
                  buffer.append(key_column_name);
                  buffer.append("\"/>\n");
                  buffer.append("         <many-to-many class=\"");
                  buffer.append(target_prototype.getInstanceClass().getName());
                  buffer.append("\" column=\"");
                  buffer.append(column_name);
                  buffer.append("\"/>\n");
                  buffer.append("      </set>\n");
               }
            }
            else {
               logger.warn("Warning: To-many relationships not supported for null back-relationship: " + prototype.getName() + "." + relationship.getName());
            }
         }
      }

      // Recursively map sub-types
      Iterator subTypes = prototype.subTypes();
      OpPrototype subType = null;
      while (subTypes.hasNext()) {
         subType = (OpPrototype) subTypes.next();
         appendSubTypeMapping(buffer, subType);
      }

      buffer.append("   </joined-subclass>\n");
   }

   /**
    * Returns the name of the hibernate type associated with the given id of an <code>OpType</code>.
    *
    * @param xTypeId a <code>int</code> representing the id of an OpType.
    * @return a <code>String</code> representing the name of the equivalent hibernate type, or the name of a custom type.
    */
   static String getHibernateTypeName(int xTypeId) {
      String typeName = null;
      switch (xTypeId) {
         case OpType.BOOLEAN: {
            typeName = "boolean";
            break;
         }
         case OpType.INTEGER: {
            typeName = "integer";
            break;
         }
         case OpType.LONG: {
            typeName = "long";
            break;
         }
         case OpType.STRING: {
            typeName = "string";
            break;
         }
         case OpType.TEXT: {
            typeName = "string";
            break;
         }
         case OpType.DATE: {
            typeName = "java.sql.Date";
            break;
         }
         case OpType.CONTENT: {
            typeName = "onepoint.persistence.hibernate.OpBlobUserType";
            break;
         }
         case OpType.BYTE: {
            typeName = "byte";
            break;
         }
         case OpType.DOUBLE: {
            typeName = "double";
            break;
         }
         case OpType.TIMESTAMP: {
            typeName = "timestamp";
            break;
         }
      }
      return typeName;
   }

   /**
    * Updates the schema version number in the db, to the value of the SCHEMA_VERSION constant.
    */
   public void updateSchemaVersionNumber() {
      Session session = _session_factory.openSession();
      Connection jdbcConnection = session.connection();
      Statement statement = null;

      try {
         statement = jdbcConnection.createStatement();
         statement.executeUpdate(UPDATE_SCHEMA_TABLE_STATEMENT);
         jdbcConnection.commit();
      }
      catch (SQLException e) {
         logger.error("Cannot update db schema number", e);
      }
      finally {
         if (statement != null) {
            try {
               statement.close();
            }
            catch (SQLException e) {
               logger.error("Cannot close statement:" + e.getMessage(), e);
            }
         }
         session.close();
      }
   }

   /**
    * Checks whether the db schema needs upgrading or not.
    *
    * @return <code>true</code> if the db schema needs upgrading.
    */
   public boolean needSchemaUpgrading() {
      int existingVersionNumber = this.getExistingSchemaVersionNumber();
      return existingVersionNumber < SCHEMA_VERSION;
   }

   /**
    * Queries the database for the schema version number.
    *
    * @return a <code>int</code> representing the persisted schema version number, or <code>-1</code> if the version number
    *         can't be retrieved.
    */
   public int getExistingSchemaVersionNumber() {
      Session session = _session_factory.openSession();
      Connection jdbcConnection = session.connection();
      Statement statement = null;
      ResultSet rs = null;
      try {
         statement = jdbcConnection.createStatement();

         if (!existsTable(SCHEMA_TABLE)) {
            statement.execute(CREATE_SCHEMA_TABLE_STATEMENT);
            statement.executeUpdate(INSERT_ZERO_INTO_SCHEMA_TABLE_STATEMENT);
            jdbcConnection.commit();
            logger.info("Created table op_schema for versioning");
         }

         rs = statement.executeQuery(GET_SCHEMA_VERSION_STATEMENT);
         rs.next();
         return rs.getInt(VERSION_COLUMN);
      }
      catch (SQLException e) {
         logger.error("Cannot get version number ", e);
      }
      finally {
         try {
            if (rs != null) {
               rs.close();
            }
            if (statement != null) {
               statement.close();
            }
         }
         catch (SQLException e) {
            logger.error("Cannot close result set and statement", e);
         }
         session.close();
      }
      return -1;
   }

   public void clear() {
      _session_factory.evictQueries();
      Iterator prototypesIterator = OpTypeManager.getPrototypes();
      while (prototypesIterator.hasNext()) {
         OpPrototype prototype = (OpPrototype) prototypesIterator.next();
         _session_factory.evict(prototype.getInstanceClass());
      }
   }
}
