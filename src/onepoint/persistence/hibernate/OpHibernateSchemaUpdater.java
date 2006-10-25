/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpMember;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpRelationship;
import onepoint.persistence.OpTypeManager;
import onepoint.persistence.sql.OpSqlStatement;
import onepoint.persistence.sql.OpSqlStatementFactory;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * Class that is responsible for updating the db schema (beyond of what the default hibernate capabilities are), after
 * something has changed in one of the entities of the application.
 *
 * @author horia.chiorean
 */
public final class OpHibernateSchemaUpdater {

   public static List nonPrototypeTables = new ArrayList();

   static {
      nonPrototypeTables.add("op_schema");
      nonPrototypeTables.add("op_object");
   }

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpHibernateSchemaUpdater.class, true);

   /**
    * The map of db types.
    */
   private static final Map DB_TYPES_MAP = new HashMap();

   /**
    * The only class instance.
    */
   private static OpHibernateSchemaUpdater instance = null;

   /**
    * The Hibernate source that backs up the updater.
    */
   private OpHibernateSource source = null;

   /**
    * A map of table metadata.
    */
   private Map tableMetaData = new HashMap();

   /**
    * Initialize the db types map.
    */
   static {
      DB_TYPES_MAP.put(new Integer(OpHibernateSource.MYSQL), new Integer(OpSqlStatementFactory.MYSQL));
      DB_TYPES_MAP.put(new Integer(OpHibernateSource.MYSQL_INNODB), new Integer(OpSqlStatementFactory.MYSQL));
      DB_TYPES_MAP.put(new Integer(OpHibernateSource.POSTGRESQL), new Integer(OpSqlStatementFactory.POSTGRESQL));
   }

   /**
    * Creates a new updater instance.
    */
   private OpHibernateSchemaUpdater() {
   }

   /**
    * Returns an updater instance that uses the given source object for performing the
    *
    * @param source a <code>OpHibernateSource</code> that is used during the update process.
    * @return a <code>OpHibernateSchemaUpdater</code> instance.
    */
   public static synchronized OpHibernateSchemaUpdater getInstance(OpHibernateSource source) {
      if (instance == null) {
         instance = new OpHibernateSchemaUpdater();
      }
      instance.source = source;
      return instance;
   }

   /**
    * Creates the list of SQL instructions that update the existing database, to match the current entity structure.
    *
    * @param dbMetaData a <code>DatabaseMetaData</code> object, containing information about the db.
    * @return a <code>List</code> of <code>String</code> representing db information.
    */
   public List generateUpdateSchemaScripts(DatabaseMetaData dbMetaData) {
      try {

         //update column scripts
         this.populateTableMetaData(dbMetaData);
         List updateStatements = new ArrayList();
         Iterator it = OpTypeManager.getPrototypes();
         while (it.hasNext()) {
            OpPrototype prototype = (OpPrototype) it.next();
            updateStatements.addAll(generateUpdateTableColumnsScripts(prototype));
         }

         //drop old tables
         updateStatements.addAll(generateDropTableScripts(dbMetaData));

         return updateStatements;
      }
      catch (SQLException e) {
         logger.error("Cannot generate custom update scripts", e);
         return Collections.EMPTY_LIST;
      }
   }

   /**
    * Creates the list of SQL instructions that drop old tables in order to match the current entity structure.
    *
    * @param dbMetaData a <code>DatabaseMetaData</code> object, containing information about the db.
    * @return a <code>List</code> of <code>String</code> representing drop scripts.
    */
   private List generateDropTableScripts(DatabaseMetaData dbMetaData) {

      try {
         //get current tables
         List currentTables = new ArrayList();
         Iterator it = OpTypeManager.getPrototypes();
         while (it.hasNext()) {
            OpPrototype prototype = (OpPrototype) it.next();
            String tableName = source.newTableName(prototype.getName());
            currentTables.add(tableName);
         }


         ResultSet rs = dbMetaData.getTables(null, null, OpHibernateSource.TABLE_NAME_PREFIX + "%", new String[]{"TABLE"});
         List dropStatements = new ArrayList();
         while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            if (!currentTables.contains(tableName) && !nonPrototypeTables.contains(tableName)) {
               Integer dbType = (Integer) DB_TYPES_MAP.get(new Integer(source.getDatabaseType()));
               OpSqlStatement statement = OpSqlStatementFactory.createSqlStatement(dbType);
               String sqlStatement = statement.getDropTableStatement(tableName);
               dropStatements.add(sqlStatement);
            }
         }
         return dropStatements;
      }
      catch (SQLException e) {
         logger.error("Cannot generate custom update scripts", e);
         return Collections.EMPTY_LIST;
      }
   }

   /**
    * Creates a map of metadata for the underlying database, from the given object.
    *
    * @param dbMetaData a <code>DatabaseMetaData</code> containing meta-information about the database.
    * @throws SQLException if meta-data cannot be read for the database.
    */
   private void populateTableMetaData(DatabaseMetaData dbMetaData)
        throws SQLException {
      ResultSet rs = dbMetaData.getTables(null, null, OpHibernateSource.TABLE_NAME_PREFIX + "%", new String[]{"TABLE"});
      while (rs.next()) {
         String tableName = rs.getString("TABLE_NAME");
         ResultSet rs1 = dbMetaData.getColumns(null, null, tableName, OpHibernateSource.COLUMN_NAME_PREFIX + "%");
         Map tableColumnsMetaData = new HashMap();
         while (rs1.next()) {
            String columnName = rs1.getString("COLUMN_NAME");
            Short columnType = new Short(rs1.getShort("DATA_TYPE"));
            tableColumnsMetaData.put(columnName, columnType);
         }
         this.tableMetaData.put(tableName, tableColumnsMetaData);
      }
   }

   /**
    * Generates a list of sql statements that update the column of the table that correspons to the given prototype.
    *
    * @param prototype a <code>OpPrototype</code> representing a prototype registered by the application.
    * @return a <code>List</code> of <code>String</code> representing a list of SQL statements.
    */
   private List generateUpdateTableColumnsScripts(OpPrototype prototype) {
      List result = new ArrayList();
      Iterator membersIt = prototype.getMembers();
      while (membersIt.hasNext()) {
         OpMember member = (OpMember) membersIt.next();
         //relationships are not taken into account
         if (member instanceof OpRelationship) {
            continue;
         }
         //find out the sql type for our current registered prototype
         String hibernateTypeName = OpHibernateSource.getHibernateTypeName(member.getTypeID());
         if (hibernateTypeName == null) {
            logger.info("Cannot get hibernate type for OpType.id:" + member.getTypeID());
            continue;
         }
         Type hibernateType = TypeFactory.basic(hibernateTypeName);
         //only upgrade for primitive types
         if (!(hibernateType instanceof PrimitiveType)) {
            continue;
         }
         int hibernateSqlType = ((PrimitiveType) hibernateType).sqlType();

         //now find out what we have in the db
         String tableName = source.newTableName(prototype.getName());
         Map columnMetaData = (Map) this.tableMetaData.get(tableName);
         if (columnMetaData == null) {
            logger.info("No metadata found for table:" + tableName);
            continue;
         }
         String columnName = source.newColumnName(member.getName());
         Short columnType = (Short) columnMetaData.get(columnName);
         if (columnType == null) {
            logger.info("No metadata found for column:" + columnName);
            continue;
         }
         if (hibernateSqlType != columnType.shortValue() && columnType.shortValue() != Types.OTHER) {
            Integer dbType = (Integer) DB_TYPES_MAP.get(new Integer(source.getDatabaseType()));
            OpSqlStatement statement = OpSqlStatementFactory.createSqlStatement(dbType);
            String sqlStatement = statement.getAlterColumnTypeStatement(tableName, columnName, hibernateSqlType);
            logger.info("XSchemaUpdater: adding update statement: " + sqlStatement);
            result.add(sqlStatement);
         }
      }
      return result;
   }
}
