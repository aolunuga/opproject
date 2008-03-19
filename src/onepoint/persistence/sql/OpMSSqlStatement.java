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
 * Class used for obtaining MSSQLServer2000 specific statements
 *
 * @author horia.chiorean
 */
public final class OpMSSqlStatement extends OpSqlStatement {

   /**
    * The mapping of MsSQLServer specific types.
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
      DB_TYPES.put(new Integer(Types.BOOLEAN), "TINYINT");
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
   public List<String> getAlterColumnTypeStatement(String tableName, String columnName, int sqlType) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" ALTER COLUMN ");
      result.append(columnName);
      result.append(" ");
      String columnType = (String) DB_TYPES.get(new Integer(sqlType));
      result.append(columnType);
      result.append(";");
      List<String> resultList = new ArrayList<String>();
      resultList.add(result.toString());
      return resultList;
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

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getAlterColumnTypeStatement(String, String, int)
    */
   @Override
   public List<String> getAlterTextColumnLengthStatement(String tableName, String columnName, int newLength) {
      StringBuffer result = new StringBuffer();
      result.append("ALTER TABLE ");
      result.append(tableName);
      result.append(" ALTER COLUMN ");
      result.append(columnName);
      String columnType = " VARCHAR(" + newLength + ")";
      result.append(columnType);
      result.append(";");
      List<String> resultList = new ArrayList<String>();
      resultList.add(result.toString());
      return resultList;
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getColumnType(int)
    */
   @Override
   public int getColumnType(int hibernateType) {
      switch (hibernateType) {
         case Types.BIT:
            return Types.TINYINT;
         case Types.BIGINT:
            return Types.NUMERIC;
         case Types.DATE:
            return Types.TIMESTAMP;
      }
      return hibernateType;
   }
}
