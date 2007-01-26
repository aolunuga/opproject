/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.sql;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used for obtaining Oracle statements.
 *
 * @author horia.chiorean
 */
public final class OpOracleStatement implements OpSqlStatement {

   /**
    * The mapping of Oracle specific types.
    */
   private static final Map DB_TYPES = new HashMap();

   /**
    * Populate the types map.
    */
   static {
      DB_TYPES.put(new Integer(Types.VARCHAR), "VARCHAR2");
      DB_TYPES.put(new Integer(Types.BIGINT), "NUMBER");
      DB_TYPES.put(new Integer(Types.FLOAT), "FLOAT");
      DB_TYPES.put(new Integer(Types.INTEGER), "NUMBER");
      DB_TYPES.put(new Integer(Types.DOUBLE), "FLOAT");
      DB_TYPES.put(new Integer(Types.BIT), "NUMBER");
      DB_TYPES.put(new Integer(Types.BOOLEAN), "BOOLEAN");
      DB_TYPES.put(new Integer(Types.TINYINT), "NUMBER");
      DB_TYPES.put(new Integer(Types.SMALLINT), "SMALLINT");
      DB_TYPES.put(new Integer(Types.BLOB), "BLOB");
      DB_TYPES.put(new Integer(Types.TIMESTAMP), "TIMESTAMP(6)");
      DB_TYPES.put(new Integer(Types.DATE), "DATE");
      //... if needed, continue the list
   }

   /**
    * Instances of this class should only be obtained via the <code>OpSqlStatementFactory</code> class.
    */
   OpOracleStatement() {
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getAlterColumnTypeStatement(String, String, int)
    */
   public String getAlterColumnTypeStatement(String tableName, String columnName, int sqlType) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" MODIFY ");
      result.append(columnName);
      result.append(" ");
      String columnType = (String) DB_TYPES.get(new Integer(sqlType));
      result.append(columnType);
      result.append(";");
      return result.toString();
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getDropTableStatement(String)
    */
   public String getDropTableStatement(String tableName) {
      StringBuffer result = new StringBuffer();
      result.append("DROP TABLE ");
      result.append(tableName);
      result.append(";");
      return result.toString();
   }

   /**
    * @see OpSqlStatement#getDropFKConstraintStatement(String, String)
    */
   public String getDropFKConstraintStatement(String tableName, String fkConstraintName) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" DROP CONSTRAINT ");
      result.append(fkConstraintName);
      result.append(";");
      return result.toString();
   }

   /**
    * @see OpSqlStatement#getDropIndexConstraintStatement(String, String)
    */
   public String getDropIndexConstraintStatement(String tableName, String indexName) {
      StringBuffer result = new StringBuffer();
      result.append("DROP INDEX ");
      result.append(indexName);
      result.append(";");
      return result.toString();
   }
}
