/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.sql;

/**
 * Null implementation of the <code>OpSqlStatement</code> interface.
 *
 * @author horia.chiorean
 */
public class OpUnknownSqlStatement implements OpSqlStatement {

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getAlterColumnTypeStatement(String, String, int)
    */
   public String getAlterColumnTypeStatement(String tableName, String columnName, int sqlType) {
      return null;
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getDropTableStatement(String)
    */
   public String getDropTableStatement(String tableName) {
      return null;
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getDropFKConstraintStatement(String, String)
    */
   public String getDropFKConstraintStatement(String tableName, String fkConstraintName) {
      return null;
   }

   /**
    * @see onepoint.persistence.sql.OpSqlStatement#getDropIndexConstraintStatement(String, String)  
    */
   public String getDropIndexConstraintStatement(String tableName, String indexName) {
      return null;
   }
}
