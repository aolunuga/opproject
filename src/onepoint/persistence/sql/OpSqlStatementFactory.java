/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence.sql;

import java.util.Map;
import java.util.HashMap;

/**
 * Factory class that produces different sql statements, based on the type of the db.
 *
 * @author horia.chiorean
 */
public final class OpSqlStatementFactory {

   /**
    * Constants for different db types.
    */
   public static final int UNKNOWN = 0;
   public static final int MYSQL = 1;
   public static final int POSTGRESQL = 2;
   public static final int SQLSERVER = 3;

   /**
    * A cache for the statements.
    */
   private static Map statementCache = new HashMap();

   /**
    * Cannot create instances of this class.
    */
   private OpSqlStatementFactory() {
   }

   /**
    * Returns a <code>OpSqlStatement</code> for the given db type.
    *
    * @param dbType a <code>String</code> representing a db type.
    * @return a <code>OpSqlStatement</code> instance, specific to the given db type.
    */
   public static OpSqlStatement createSqlStatement(Integer dbType) {
      OpSqlStatement statement = (OpSqlStatement) statementCache.get(dbType);
      if (statement == null) {
         switch (dbType.intValue()) {
            case MYSQL: {
               statement = new OpMySqlStatement();
               break;
            }
            case POSTGRESQL: {
               statement = new OpPostgreSqlStatement();
               break;
            }
            case SQLSERVER: {
               statement = new OpSQLServerStatement();
               break;
            }
            case UNKNOWN: {
               statement = new OpUnknownSqlStatement();
               break;
            }
            default: {
               throw new IllegalArgumentException("Unknow db type " + dbType);
            }
         }
         statementCache.put(dbType, statement);
      }
      return statement;
   }
}
