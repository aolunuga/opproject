/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.sql;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used for obtaining MySQL statements.
 *
 * @author horia.chiorean
 */
public final class OpMySqlStatement implements OpSqlStatement {

   /**
    * The mapping of MySQL specific types.
    */
   private static final Map DB_TYPES = new HashMap();

   /**
    * Populate the types map.
    */
   static {
      DB_TYPES.put(new Integer(Types.VARCHAR), "VARCHAR(255)");
      DB_TYPES.put(new Integer(Types.BIGINT), "BIGINT");
      DB_TYPES.put(new Integer(Types.FLOAT), "FLOAT");
      DB_TYPES.put(new Integer(Types.INTEGER), "INTEGER");
      DB_TYPES.put(new Integer(Types.DOUBLE), "DOUBLE");
      DB_TYPES.put(new Integer(Types.BIT), "BIT");
      DB_TYPES.put(new Integer(Types.BOOLEAN), "BIT");
      DB_TYPES.put(new Integer(Types.TINYINT), "TINYINT");
      DB_TYPES.put(new Integer(Types.SMALLINT), "INTEGER");
      DB_TYPES.put(new Integer(Types.BLOB), "BLOB");
      DB_TYPES.put(new Integer(Types.TIMESTAMP), "DATETIME");
      DB_TYPES.put(new Integer(Types.DATE), "DATE");
      //... if needed, continue the list
   }

   /**
    * Instances of this class should only be obtained via the <code>OpSqlStatementFactory</code> class.
    */
   OpMySqlStatement() {
   }

   /**
    * @see OpSqlStatement#getAlterColumnTypeStatement(String, String, int)
    */
   public String getAlterColumnTypeStatement(String tableName, String columnName, int sqlType) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" MODIFY COLUMN ");
      result.append(columnName);
      result.append(" ");
      String columnType = (String) DB_TYPES.get(new Integer(sqlType));
      result.append(columnType);
      result.append(";");
      return result.toString();
   }

   /**
    * @see OpSqlStatement#getDropTableStatement(String)
    */
   public String getDropTableStatement(String tableName) {
      StringBuffer result = new StringBuffer();
      result.append("DROP TABLE ");
      result.append(tableName);
      result.append(";");
      return result.toString();
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getDropFKConstraintStatement(String, String)
    */
   public String getDropFKConstraintStatement(String tableName, String fkConstraintName) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" DROP FOREIGN KEY ");
      result.append(fkConstraintName);
      result.append(";");
      return result.toString();
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getDropIndexConstraintStatement(String, String)
    */
   public String getDropIndexConstraintStatement(String tableName, String indexName) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" DROP INDEX ");
      result.append(indexName);
      result.append(";");
      return result.toString();
   }
}
