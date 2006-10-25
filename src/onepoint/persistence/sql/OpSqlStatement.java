/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.sql;

/**
 * Interface representing an sql statement which is db dependent.
 *
 * @author horia.chiorean
 */
public interface OpSqlStatement {

   /**
    * Returns an sql statement that will change the type of a db column.
    *
    * @param tableName  a <code>String</code> representing the name of the table for which the statement is executed.
    * @param columnName a <code>String</code> representing the name of the column for which the statement is executed.
    * @param sqlType    an <code>int</code> representing new SQL type of the column.
    * @return a <code>String</code> representing a statement used to change the type of a column type.
    *
    * @see java.sql.Types                                                                                                       
    */
   public String getAlterColumnTypeStatement(String tableName, String columnName, int sqlType);

   /**
    * Returns an sql statement that will drop the given table.
    *
    * @param tableName table to be dropped
    * @return a <code>String</code> representing a statement used to drop a table
    */
   public String getDropTableStatement(String tableName);
}
