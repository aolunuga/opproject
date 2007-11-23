/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.hibernate;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.persistence.sql.OpSqlStatement;
import onepoint.persistence.sql.OpSqlStatementFactory;
import org.hibernate.type.NullableType;
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
   private static final List<String> TABLES_WITHOUT_PROTOTYPE = new ArrayList<String>();


   static {
      TABLES_WITHOUT_PROTOTYPE.add(OpHibernateSource.SCHEMA_TABLE);
   }

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getServerLogger(OpHibernateSchemaUpdater.class);

   /**
    * Constants used in the various JDBC method calls
    */
   private static final String TABLE_PREFIX = OpMappingsGenerator.getTableNamePrefix() + "%";
   private static final String[] TABLE_TYPES = new String[]{"TABLE"};
   private static final String TABLE_NAME_COLUMN = "TABLE_NAME";

   /**
    * The map of db types.
    */
   private static final Map<Integer, Integer> DB_TYPES_MAP = new HashMap<Integer, Integer>();

   /**
    * List of predifined indexes
    */
   private static final List EXCLUDE_INDEX_NAMES = Arrays.asList("PRIMARY");

   /**
    * A map of table type related metadata.
    */
   private Map<String, Map> typeMetaData = new HashMap<String, Map>();

   /**
    * A map of table length related metadata
    */
   private Map<String, Integer> lengthMetaData = new HashMap<String, Integer>();

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
    * @param databaseType a <code>Integer</code> a data-base type constant as defined by the hibernate source.
    * @see onepoint.persistence.hibernate.OpHibernateSource
    */
   public OpHibernateSchemaUpdater(Integer databaseType) {
      Integer dbType = (Integer) DB_TYPES_MAP.get(databaseType);
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
   public List<String> generateUpdateSchemaScripts(DatabaseMetaData dbMetaData) {
      //add the hi/lo generator statement
      List<String> updateStatements = new ArrayList<String>();
      try {
         //update column scripts
         this.populateTableMetaData(dbMetaData);

         Iterator it = OpTypeManager.getPrototypes();
         while (it.hasNext()) {
            OpPrototype prototype = (OpPrototype) it.next();
            updateStatements.addAll(generateUpdateTableColumnsScripts(prototype, dbMetaData));
         }

         //drop old tables
         updateStatements.addAll(generateDropTableScripts(dbMetaData));
      }
      catch (SQLException e) {
         logger.error("Cannot generate custom update scripts", e);
      }
      return updateStatements;
   }

   /**
    * Generates a list of statements that drop foreign key contraints and index for the tables of the given database.
    *
    * @param dbMetaData a <code>DatabaseMetaData</code> object representing meta info about a db.
    * @return a <code>List</code> of <code>String</code> representing SQL statements.
    */
   public List<String> generateDropConstraintScripts(DatabaseMetaData dbMetaData) {
      List<String> dropConstraintScripts = new ArrayList<String>();
      try {
         ResultSet rs = dbMetaData.getTables(null, null, TABLE_PREFIX, TABLE_TYPES);
         while (rs.next()) {
            String tableName = rs.getString(TABLE_NAME_COLUMN);
            //<FIXME author="Mihai Costin" description="We should be able to get only op_tables with getTable method">
            if (!tableName.startsWith(OpMappingsGenerator.getTableNamePrefix())) {
               continue;
            }
            //</FIXME>
            List<String> tableFKConstraints = generateDropFKConstraints(tableName, dbMetaData);
            dropConstraintScripts.addAll(tableFKConstraints);
            List<String> tableIndexConstraints = generateDropIndexConstraints(tableName, dbMetaData);
            dropConstraintScripts.addAll(tableIndexConstraints);
         }
         rs.close();
      }
      catch (SQLException e) {
         logger.error("Cannot generate the drop constraints scripts", e);
      }
      return dropConstraintScripts;
   }

   /**
    * Generates a list of statements that drop predefined tables (tables that are not managed by Hibernate).
    *
    * @return a <code>List</code> of <code>String</code> representing SQL statements.
    */
   public List generateDropPredefinedTablesScripts() {
      List<String> dropTableScripts = new ArrayList<String>();
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
   private List<String> generateDropIndexConstraints(String tableName, DatabaseMetaData dbMetaData) {
      List<String> dropIndexScripts = new ArrayList<String>();
      if (statement != null) {
         try {
            ResultSet rs = dbMetaData.getIndexInfo(null, null, tableName, false, false);
            while (rs.next()) {
               String indexName = rs.getString("INDEX_NAME");
               //<FIXME author="Horia Chiorean" description="Can we be 100% that this way we only get the hibernate generated indexes ?">
               if (indexName == null || indexName.startsWith(OpMappingsGenerator.getIndexNamePrefix()) || EXCLUDE_INDEX_NAMES.contains(indexName)) {
                  continue;
               }
               //<FIXME>
               String dropStatement = statement.getDropIndexConstraintStatement(tableName, indexName);
               logger.info("Adding drop index statement: " + dropStatement);
               dropIndexScripts.add(dropStatement);
            }
            rs.close();
         }
         catch (SQLException e) {
            logger.error("Cannot get index constraints for table:" + tableName, e);
         }
      }
      return dropIndexScripts;
   }

   /**
    * Generates a list of SQL statements for dropping the FK constraints of the given table.
    *
    * @param tableName  a <code>String</code> representing a table name.
    * @param dbMetaData a <code>DatabaseMetaData</code> containing information about the underlying db.
    * @return a <code>List</code> of <code>String</code> representing SQL statements.
    */
   private List<String> generateDropFKConstraints(String tableName, DatabaseMetaData dbMetaData) {
      List<String> dropFkConstraints = new ArrayList<String>();
      if (statement != null) {
         try {
            ResultSet rs = dbMetaData.getImportedKeys(null, null, tableName);
            while (rs.next()) {
               String fkName = rs.getString("FK_NAME");

               String dropStatement = statement.getDropFKConstraintStatement(tableName, fkName);
               logger.info("Adding drop constraint statement: " + dropStatement);
               dropFkConstraints.add(dropStatement);
            }
            rs.close();
         }
         catch (SQLException e) {
            logger.error("Cannot generate drop fk constraints for table:" + tableName, e);
         }
      }
      return dropFkConstraints;
   }

   /**
    * Generates a list of SQL statements for dropping the FK constraints of the given table for the given column.
    *
    * @param tableName  a <code>String</code> representing a table name.
    * @param columnName a <code>String</code> representing the column name for which the statements are generated.
    * @param dbMetaData a <code>DatabaseMetaData</code> containing information about the underlying db.
    * @return a <code>List</code> of <code>String</code> representing SQL statements.
    */
   private List<String> generateDropFKConstraints(String tableName, String columnName, DatabaseMetaData dbMetaData) {
      List<String> dropFkConstraints = new ArrayList<String>();
      if (statement != null) {
         try {
            ResultSet rs = dbMetaData.getImportedKeys(null, null, tableName);
            while (rs.next()) {
               String fkName = rs.getString("FK_NAME");
               String fkColumnName = rs.getString("FKCOLUMN_NAME");

               if (fkColumnName.equals(columnName)) {
                  String dropStatement = statement.getDropFKConstraintStatement(tableName, fkName);
                  logger.info("Adding drop constraint statement: " + dropStatement);
                  dropFkConstraints.add(dropStatement);
               }
            }
            rs.close();
         }
         catch (SQLException e) {
            logger.error("Cannot generate drop fk constraints for table:" + tableName, e);
         }
      }
      return dropFkConstraints;
   }

   /**
    * Creates the list of SQL instructions that drop old tables in order to match the current entity structure.
    *
    * @param dbMetaData a <code>DatabaseMetaData</code> object, containing information about the db.
    * @return a <code>List</code> of <code>String</code> representing drop scripts.
    */
   private List<String> generateDropTableScripts(DatabaseMetaData dbMetaData) {
      List<String> dropStatements = new ArrayList<String>();
      if (statement != null) {
         try {
            //get current tables
            List<String> currentTables = new ArrayList<String>();
            Iterator it = OpTypeManager.getPrototypes();
            while (it.hasNext()) {
               OpPrototype prototype = (OpPrototype) it.next();
               String tableName = OpMappingsGenerator.generateTableName(prototype.getName());
               currentTables.add(tableName);
            }

            ResultSet rs = dbMetaData.getTables(null, null, TABLE_PREFIX, TABLE_TYPES);
            while (rs.next()) {
               String tableName = rs.getString(TABLE_NAME_COLUMN);

               //<FIXME author="Mihai Costin" description="We should be able to get only op_tables with getTable method">
               if (!tableName.startsWith(OpMappingsGenerator.getTableNamePrefix())) {
                  continue;
               }
               //</FIXME>
               
               if (!currentTables.contains(tableName) && !TABLES_WITHOUT_PROTOTYPE.contains(tableName.toLowerCase())) {
                  String sqlStatement = statement.getDropTableStatement(tableName);
                  logger.info("Adding drop statement: " + sqlStatement);
                  dropStatements.add(sqlStatement);
               }
            }
            rs.close();
         }
         catch (SQLException e) {
            logger.error("Cannot generate custom update scripts", e);
         }
      }
      return dropStatements;
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
         //<FIXME author="Mihai Costin" description="We should be able to get only op_tables with getTable method">
         if (!tableName.startsWith(OpMappingsGenerator.getTableNamePrefix())) {
            continue;
         }
         //</FIXME>
         ResultSet rs1 = dbMetaData.getColumns(null, null, tableName, OpMappingsGenerator.getColumnNamePrefix() + "%");
         Map<String, Short> tableColumnsMetaData = new HashMap<String, Short>();
         while (rs1.next()) {
            String columnName = rs1.getString("COLUMN_NAME");
            Short columnType = new Short(rs1.getShort("DATA_TYPE"));
            Integer columnLength = rs1.getInt("COLUMN_SIZE");
            this.lengthMetaData.put(columnName,  columnLength);
            tableColumnsMetaData.put(columnName, columnType);
         }
         this.typeMetaData.put(tableName, tableColumnsMetaData);
      }
   }

   /**
    * Generates a list of sql statements that update the column of the table that correspons to the given prototype.
    *
    * @param prototype  a <code>OpPrototype</code> representing a prototype registered by the application.
    * @param dbMetaData a <code>DatabaseMetaData</code> object, containing information about the db.
    * @return a <code>List</code> of <code>String</code> representing a list of SQL statements.
    */
   private List<String> generateUpdateTableColumnsScripts(OpPrototype prototype, DatabaseMetaData dbMetaData) {
      List<String> result = new ArrayList<String>();
      if (statement != null) {

         Iterator membersIt = prototype.getMembers();
         while (membersIt.hasNext()) {
            OpMember member = (OpMember) membersIt.next();
            //relationships are not taken into account and only declared members are taken into account
            if (member instanceof OpRelationship ||  !prototype.containsDeclaredMember(member)) {
               continue;
            }
            result.addAll(checkTypeChanges(prototype.getName(), dbMetaData, member));
            result.addAll(checkLengthChanges(prototype.getName(), member));
         }
      }
      return result;
   }

   /**
    * Checks if there are any type changes between the existent db and the current objects.
    * @param prototypeName a <code>String</code> the name of a prototype
    * @param dbMetaData a <code>DatabaseMetaData</code> object.
    * @param member  a <code>OpMember</code> from the prototype.
    * @return a <code>List(String)</code> list of statements
    */
   private  List<String> checkTypeChanges(String prototypeName, DatabaseMetaData dbMetaData, OpMember member) {
      List<String> result = new ArrayList<String>();
      //find out the sql type for our current registered prototype
      String hibernateTypeName = OpMappingsGenerator.getHibernateTypeName(member.getTypeID());
      if (hibernateTypeName == null) {
         logger.info("Cannot get hibernate type for OpType.id:" + member.getTypeID());
         return result;
      }
      Type hibernateType = TypeFactory.basic(hibernateTypeName);
      //only upgrade for primitive types
      if (!(hibernateType instanceof NullableType)) {
         return result;
      }
      int hibernateSqlType = ((NullableType) hibernateType).sqlType();

      //now find out what we have in the db
      String tableName = OpMappingsGenerator.generateTableName(prototypeName);
      Map columnMetaData = this.typeMetaData.get(tableName);
      if (columnMetaData == null) {
         logger.info("No metadata found for table:" + tableName);
         return result;
      }
      String columnName = OpMappingsGenerator.generateColumnName(member.getName());
      Short columnType = (Short) columnMetaData.get(columnName);
      if (columnType == null) {
         logger.info("No metadata found for column:" + columnName);
         return result;
      }

      //<FIXME author="Mihai Costin" description="The dialect should help us here instead of the manual mapping done in getColumnType">
      if (columnType != statement.getColumnType(hibernateSqlType) && columnType != Types.OTHER) {
      //</FIXME>

         //drop al fk constraints for this column before changing it's type
         List<String> dropFkConstraints = generateDropFKConstraints(tableName, columnName, dbMetaData);
         for (String fkConstraint : dropFkConstraints) {
            logger.info("Adding drop FK statement: " + fkConstraint);
            result.add(fkConstraint);
         }

         List<String> sqlStatement = statement.getAlterColumnTypeStatement(tableName, columnName, hibernateSqlType);
         logger.info("Adding update statement: " + sqlStatement);
         result.addAll(sqlStatement);
      }
      return result;
   }

   /**
    * Checks if there are any type changes between the existent db and the current objects.
    * @param prototypeName a <code>String</code> the name of a prototype
    * @param member  a <code>OpMember</code> from the prototype.
    * @return a <code>List(String)</code> list of statements
    */
   private  List<String> checkLengthChanges(String prototypeName,  OpMember member) {
      List<String> result = new ArrayList<String>();
      if (member.getTypeID() == OpType.TEXT) {
         String tableName = OpMappingsGenerator.generateTableName(prototypeName);
         String columnName = OpMappingsGenerator.generateColumnName(member.getName());
         Integer currentLength = this.lengthMetaData.get(columnName);
         if (currentLength < OpTypeManager.MAX_TEXT_LENGTH) {
            logger.warn("Column " + columnName + " of table " + tableName + " has a length of " + currentLength + ". Upgrading length to " + OpTypeManager.MAX_TEXT_LENGTH);
            result.addAll(this.statement.getAlterTextColumnLengthStatement(tableName, columnName, OpTypeManager.MAX_TEXT_LENGTH));
         }
      }
      return result;
   }

}
