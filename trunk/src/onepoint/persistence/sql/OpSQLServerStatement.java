/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.sql;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used for obtaining SQLServer2000 specific statements
 *
 * @author horia.chiorean
 */
public final class OpSQLServerStatement implements OpSqlStatement {

   /**
    * The mapping of SQLServer specific types.
    */
   private static final Map DB_TYPES = new HashMap();

   /**
    * Populate the types map.
    */
   static {
      DB_TYPES.put(new Integer(Types.VARCHAR), "VARCHAR(255)");
      DB_TYPES.put(new Integer(Types.BIGINT), "BIGINT");
      DB_TYPES.put(new Integer(Types.FLOAT), "FLOAT");
      DB_TYPES.put(new Integer(Types.INTEGER), "INT");
      DB_TYPES.put(new Integer(Types.DOUBLE), "FLOAT");
      DB_TYPES.put(new Integer(Types.BIT), "BIT");
      DB_TYPES.put(new Integer(Types.BOOLEAN), "BIT");
      DB_TYPES.put(new Integer(Types.TINYINT), "TINYINT");
      DB_TYPES.put(new Integer(Types.SMALLINT), "TINYINT");
      DB_TYPES.put(new Integer(Types.BLOB), "IMAGE");
      DB_TYPES.put(new Integer(Types.TIMESTAMP), "DATETIME");
      DB_TYPES.put(new Integer(Types.DATE), "DATETIME");
      //... if needed, continue the list
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getAlterColumnTypeStatement(String,String,int)
    */
   public String getAlterColumnTypeStatement(String tableName, String columnName, int sqlType) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" ALTER COLUMN ");
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
    * @see onepoint.persistence.sql.OpSqlStatement#getDropFKConstraintStatement(String,String)
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
    * @see OpSqlStatement#getDropIndexConstraintStatement(String,String)
    */
   public String getDropIndexConstraintStatement(String tableName, String indexName) {
      StringBuffer result = new StringBuffer();
      result.append("DROP INDEX ");
      result.append(tableName);
      result.append(".");
      result.append(indexName);
      result.append(";");
      return result.toString();
   }
}
