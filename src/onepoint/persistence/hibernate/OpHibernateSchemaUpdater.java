/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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

   /**
    * A list of tables that don't have matching prototype entities in the application.
    */
   private static final List TABLES_WITHOUT_PROTOTYPE = new ArrayList();


   static {
      TABLES_WITHOUT_PROTOTYPE.add(OpHibernateSource.SCHEMA_TABLE);
      TABLES_WITHOUT_PROTOTYPE.add("op_object");
   }

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpHibernateSchemaUpdater.class);

   /**
    * Constants used in the various JDBC method calls
    */
   private static final String TABLE_PREFIX = OpHibernateSource.TABLE_NAME_PREFIX + "%";
   private static final String[] TABLE_TYPES = new String[]{"TABLE"};
   private static final String TABLE_NAME_COLUMN = "TABLE_NAME";

   /**
    * The map of db types.
    */
   private static final Map DB_TYPES_MAP = new HashMap();

   /**
    * List of predifined indexes
    */
   private static final List EXCLUDE_INDEX_NAMES = Arrays.asList(new String[]{"PRIMARY"});

   /**
    * The Hibernate source that backs up the updater.
    */
   private OpHibernateSource source = null;

   /**
    * A map of table metadata.
    */
   private Map tableMetaData = new HashMap();

   /**
    * DB dependent sql statement.
    */
   private OpSqlStatement statement = null;

   /**
    * Initialize the db types map.
    */
   static {
      DB_TYPES_MAP.put(new Integer(OpHibernateSource.MYSQL_INNODB), new Integer(OpSqlStatementFactory.MYSQL));
      DB_TYPES_MAP.put(new Integer(OpHibernateSource.POSTGRESQL), new Integer(OpSqlStatementFactory.POSTGRESQL));
      DB_TYPES_MAP.put(new Integer(OpHibernateSource.MSSQL), new Integer(OpSqlStatementFactory.MSSQL));
      DB_TYPES_MAP.put(new Integer(OpHibernateSource.ORACLE), new Integer(OpSqlStatementFactory.ORACLE));
      DB_TYPES_MAP.put(new Integer(OpHibernateSource.IBM_DB2), new Integer(OpSqlStatementFactory.DB2));
   }

   /**
    * Creates a new updater instance.
    *
    * @param source a <code>OpHibernateSource</code> that is used during the update process.
    */
   public OpHibernateSchemaUpdater(OpHibernateSource source) {
      this.source = source;
      Integer dbType = (Integer) DB_TYPES_MAP.get(new Integer(source.getDatabaseType()));
      if (dbType != null) {
         statement = OpSqlStatementFactory.createSqlStatement(dbType);
      }
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
    * Generates a list of statements that drop foreign key contraints and index for the tables of the given database.
    *
    * @param dbMetaData a <code>DatabaseMetaData</code> object representing meta info about a db.
    * @return a <code>List</code> of <code>String</code> representing SQL statements.
    */
   public List generateDropConstraintScripts(DatabaseMetaData dbMetaData) {
      try {
         List dropConstraintScripts = new ArrayList();
         ResultSet rs = dbMetaData.getTables(null, null, TABLE_PREFIX, TABLE_TYPES);
         while (rs.next()) {
            String tableName = rs.getString(TABLE_NAME_COLUMN);
            List tableFKConstraints = generateDropFKConstraints(tableName, dbMetaData);
            dropConstraintScripts.addAll(tableFKConstraints);
            List tableIndexConstraints = generateDropIndexConstraints(tableName, dbMetaData);
            dropConstraintScripts.addAll(tableIndexConstraints);
         }
         rs.close();
         return dropConstraintScripts;
      }
      catch (SQLException e) {
         logger.error("Cannot generate the drop constraints scripts", e);
         return Collections.EMPTY_LIST;
      }
   }

   /**
    * Generates a list of statements that drop predefined tables (tables that are not managed by Hibernate).
    *
    * @return a <code>List</code> of <code>String</code> representing SQL statements.
    */
   public List generateDropPredefinedTablesScripts() {
      List dropTableScripts = new ArrayList();
      if (statement != null) {
         String dropStatement = statement.getDropTableStatement(OpHibernateSource.SCHEMA_TABLE);
         logger.info("Adding drop statement: " + dropStatement);
         dropTableScripts.add(dropStatement);
      }
      return dropTableScripts;
   }

   /**
    * Generates a list of SQL statements for dropping the index constraints of the given table.
    *
    * @param tableName  a <code>String</code> representing a table name.
    * @param dbMetaData a <code>DatabaseMetaData</code> containing information about the underlying db.
    * @return a <code>List</code> of <code>String</code> representing SQL statements.
    */
   private List generateDropIndexConstraints(String tableName, DatabaseMetaData dbMetaData) {
      if (statement != null) {
         try {
            List dropIndexScripts = new ArrayList();
            ResultSet rs = dbMetaData.getIndexInfo(null, null, tableName, false, false);
            while (rs.next()) {
               String indexName = rs.getString("INDEX_NAME");
               //<FIXME author="Horia Chiorean" description="Can we be 100% that this way we only get the hibernate generated indexes ?">
               if (indexName == null || indexName.startsWith(OpHibernateSource.INDEX_NAME_PREFIX) || EXCLUDE_INDEX_NAMES.contains(indexName)) {
                  continue;
               }
               //<FIXME>
               String dropStatement = statement.getDropIndexConstraintStatement(tableName, indexName);
               logger.info("Adding drop index statement: " + dropStatement);
               dropIndexScripts.add(dropStatement);
            }
            rs.close();
            return dropIndexScripts;
         }
         catch (SQLException e) {
            logger.error("Cannot get index constraints for table:" + tableName, e);
         }
      }
      return Collections.EMPTY_LIST;
   }

   /**
    * Generates a list of SQL statements for dropping the FK constraints of the given table.
    *
    * @param tableName  a <code>String</code> representing a table name.
    * @param dbMetaData a <code>DatabaseMetaData</code> containing information about the underlying db.
    * @return a <code>List</code> of <code>String</code> representing SQL statements.
    */
   private List generateDropFKConstraints(String tableName, DatabaseMetaData dbMetaData) {
      if (statement != null) {
         try {
            List dropFkConstraints = new ArrayList();
            ResultSet rs = dbMetaData.getImportedKeys(null, null, tableName);
            while (rs.next()) {
               String fkName = rs.getString("FK_NAME");

               String dropStatement = statement.getDropFKConstraintStatement(tableName, fkName);
               logger.info("Adding drop constraint statement: " + dropStatement);
               dropFkConstraints.add(dropStatement);
            }
            rs.close();
            return dropFkConstraints;
         }
         catch (SQLException e) {
            logger.error("Cannot generate drop fk constraints for table:" + tableName, e);
         }
      }
      return Collections.EMPTY_LIST;
   }

   /**
    * Creates the list of SQL instructions that drop old tables in order to match the current entity structure.
    *
    * @param dbMetaData a <code>DatabaseMetaData</code> object, containing information about the db.
    * @return a <code>List</code> of <code>String</code> representing drop scripts.
    */
   private List generateDropTableScripts(DatabaseMetaData dbMetaData) {
      if (statement != null) {
         try {
            //get current tables
            List currentTables = new ArrayList();
            Iterator it = OpTypeManager.getPrototypes();
            while (it.hasNext()) {
               OpPrototype prototype = (OpPrototype) it.next();
               String tableName = source.newTableName(prototype.getName());
               currentTables.add(tableName);
            }

            ResultSet rs = dbMetaData.getTables(null, null, TABLE_PREFIX, TABLE_TYPES);
            List dropStatements = new ArrayList();
            while (rs.next()) {
               String tableName = rs.getString(TABLE_NAME_COLUMN);
               if (!currentTables.contains(tableName) && !TABLES_WITHOUT_PROTOTYPE.contains(tableName)) {
                  String sqlStatement = statement.getDropTableStatement(tableName);
                  logger.info("Adding drop statement: " + sqlStatement);
                  dropStatements.add(sqlStatement);
               }
            }
            rs.close();
            return dropStatements;
         }
         catch (SQLException e) {
            logger.error("Cannot generate custom update scripts", e);
         }
      }
      return Collections.EMPTY_LIST;
   }

   /**
    * Creates a map of metadata for the underlying database, from the given object.
    *
    * @param dbMetaData a <code>DatabaseMetaData</code> containing meta-information about the database.
    * @throws SQLException if meta-data cannot be read for the database.
    */
   private void populateTableMetaData(DatabaseMetaData dbMetaData)
        throws SQLException {
      ResultSet rs = dbMetaData.getTables(null, null, TABLE_PREFIX, TABLE_TYPES);
      while (rs.next()) {
         String tableName = rs.getString(TABLE_NAME_COLUMN);
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
      if (statement != null) {
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

               String sqlStatement = statement.getAlterColumnTypeStatement(tableName, columnName, hibernateSqlType);
               logger.info("Adding update statement: " + sqlStatement);
               result.add(sqlStatement);

            }
         }
         return result;
      }
      return Collections.EMPTY_LIST;
   }
}
