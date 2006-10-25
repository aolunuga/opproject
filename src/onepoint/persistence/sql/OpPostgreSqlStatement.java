/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.sql;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used for obtaining PostgreSQL statements.
 *
 * @author horia.chiorean
 */
public final class OpPostgreSqlStatement implements OpSqlStatement {
   /**
    * The mapping of MySQL specific types.
    */
   private static final Map DB_TYPES = new HashMap();

   /**
    * Populate the types map.
    */
   static {
      DB_TYPES.put(new Integer(Types.VARCHAR), "VARCHAR(255)");
      DB_TYPES.put(new Integer(Types.BIGINT), "INT8");
      DB_TYPES.put(new Integer(Types.FLOAT), "FLOAT8");
      DB_TYPES.put(new Integer(Types.INTEGER), "INTEGER");
      DB_TYPES.put(new Integer(Types.DOUBLE), "FLOAT8");
      DB_TYPES.put(new Integer(Types.BIT), "BOOLEAN");
      DB_TYPES.put(new Integer(Types.BOOLEAN), "BOOLEAN");
      DB_TYPES.put(new Integer(Types.TINYINT), "SMALLINT");
      DB_TYPES.put(new Integer(Types.SMALLINT), "SMALLINT");
      DB_TYPES.put(new Integer(Types.BLOB), "BYTEA");
      DB_TYPES.put(new Integer(Types.TIMESTAMP), "DATETIME");
      DB_TYPES.put(new Integer(Types.DATE), "DATE");
      //... if needed, continue the list
   }

   /**
    * Instances of this class should only be obtained via the <code>OpSqlStatementFactory</code> class.
    */
   OpPostgreSqlStatement() {
   }

   /**
    * @see OpSqlStatement#getAlterColumnTypeStatement(String, String, int)
    */
   public String getAlterColumnTypeStatement(String tableName, String columnName, int sqlType) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" ALTER COLUMN ");
      result.append(columnName);
      result.append(" TYPE ");
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
}
