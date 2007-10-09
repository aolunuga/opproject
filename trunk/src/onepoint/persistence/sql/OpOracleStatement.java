/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence.sql;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
      DB_TYPES.put(Types.VARCHAR, "VARCHAR2(255)");
      DB_TYPES.put(Types.BIGINT, "NUMBER");
      DB_TYPES.put(Types.FLOAT, "FLOAT");
      DB_TYPES.put(Types.INTEGER, "NUMBER");
      DB_TYPES.put(Types.DOUBLE, "FLOAT");
      DB_TYPES.put(Types.BIT, "NUMBER");
      DB_TYPES.put(Types.BOOLEAN, "BOOLEAN");
      DB_TYPES.put(Types.TINYINT, "NUMBER");
      DB_TYPES.put(Types.SMALLINT, "SMALLINT");
      DB_TYPES.put(Types.BLOB, "BLOB");
      DB_TYPES.put(Types.TIMESTAMP, "TIMESTAMP(6)");
      DB_TYPES.put(Types.DATE, "DATE");
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
   public List<String> getAlterColumnTypeStatement(String tableName, String columnName, int sqlType) {
      List<String> resultList = new ArrayList<String>();
      StringBuffer result;

      //previous values must be set to null -- ORA-01439
      result = new StringBuffer();
      result.append("UPDATE ");
      result.append(tableName);
      result.append(" SET ");
      result.append(columnName);
      result.append("=null");
      resultList.add(result.toString());

      //alter table
      result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" MODIFY ");
      result.append(columnName);
      result.append(" ");
      String columnType = (String) DB_TYPES.get(new Integer(sqlType));
      result.append(columnType);
      resultList.add(result.toString());

      return resultList;
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getDropTableStatement(String)
    */
   public String getDropTableStatement(String tableName) {
      StringBuffer result = new StringBuffer();
      result.append("DROP TABLE ");
      result.append(tableName);
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
      return result.toString();
   }

   /**
    * @see OpSqlStatement#getDropIndexConstraintStatement(String, String)
    */
   public String getDropIndexConstraintStatement(String tableName, String indexName) {
      StringBuffer result = new StringBuffer();
      result.append("DROP INDEX ");
      result.append(indexName);
      return result.toString();
   }

   public int getColumnType(int hibernateType) {
      switch(hibernateType) {
         case  Types.DOUBLE :
            return Types.FLOAT;
         case Types.INTEGER :
            return Types.DECIMAL;
         case Types.TINYINT:
            return Types.DECIMAL;
         case Types.BIT:
            return Types.DECIMAL;
         case Types.BIGINT:
            return Types.DECIMAL;
      }
      return hibernateType;
   }
}
